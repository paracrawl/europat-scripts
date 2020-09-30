package patentdata.opstools;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Methods for writing out text and PDF files.
 *
 * File names are relative to the given working directory and based on
 * the country code, year, and language of the data.
 *
 * Author: Elaine Farrow
 */
public class PatentResultWriter {

    public static final String ABSTRACT_FILE = "abstract";
    public static final String CLAIM_FILE = "claim";
    public static final String DESCRIPTION_FILE = "desc";
    public static final String IMAGE_FILE = "image";
    public static final String INFO_FILE = "info";
    public static final String MISSING_CLAIM_FILE = "missing-claims";
    public static final String MISSING_DESCRIPTION_FILE = "missing-descriptions";
    public static final String MISSING_IMAGE_FILE = "missing-images";
    public static final String TITLE_FILE = "title";

    private static final Logger LOGGER = LogManager.getLogger();

    private final String countryCode;
    private final String year;
    private final File workingDir;
    private final File resultDir;
    private File checkpointDir;

    public PatentResultWriter(String workingDirName, String countryCode, Integer year) {
        this(new File(workingDirName), null, countryCode, String.valueOf(year));
    }

    private PatentResultWriter(File workingDir, File resultDir, String countryCode, String year) {
        if (resultDir == null) {
            resultDir = new File(workingDir, String.join("-", countryCode, year));
        }
        this.countryCode = countryCode;
        this.year = year;
        this.workingDir = workingDir;
        this.resultDir = resultDir;
        setCheckpointDir("checkpoints");
    }

    public PatentResultWriter getCheckpointWriter() {
        return new PatentResultWriter(workingDir, checkpointDir, countryCode, year);
    }

    public void setCheckpointDir(String dirName) {
        checkpointDir = new File(workingDir, dirName);
    }

    public PatentResultWriter getSampleWriter() {
        // write sample results to a subdirectory of the main results
        PatentResultWriter w = new PatentResultWriter(workingDir, new File(resultDir, "sample"), countryCode, year);
        w.checkpointDir = this.checkpointDir;
        return w;
    }

    // -------------------------------------------------------------------------------

    /**
     * Does the information file exist?
     */
    public boolean infoFileExists() {
        return getInfoFile().exists();
    }

    /**
     * Read patent information from a file in a standard location.
     */
    public List<PatentInfo> readInfo() throws Exception {
        return readInfoFile(getInfoFile());
    }

    /**
     * Write the given patent information into a file in a standard
     * location.
     */
    public void writeInfo(List<PatentInfo> info) throws Exception {
        writeLines(getInfoFile(), info);
    }

    /**
     * Write the given lines into a file in a standard location.
     */
    public void writeIds(List<String> lines) throws Exception {
        writeLines(getIdsFile(), lines);
    }

    /**
     * Read lines from a file in a standard location based on the
     * given file type.
     */
    public List<String> readIds(String fileType) throws Exception {
        return readLines(getIdsFile(fileType));
    }

    /**
     * Write the given lines into a file in a standard location based
     * on the given file type.
     */
    public void writeIds(List<String> lines, String fileType) throws Exception {
        writeLines(getIdsFile(fileType), lines);
    }

    /**
     * Read missing records from a file in a standard location based
     * on the given file type.
     */
    public List<String> readMissingIds(String fileType) throws Exception {
        return readLines(getMissingIdsFile(fileType));
    }

    /**
     * Write missing records into a file in a standard location based
     * on the given file type.
     */
    public void writeMissingIds(List<String> lines, String fileType) throws Exception {
        if (! lines.isEmpty()) {
            writeLines(getMissingIdsFile(fileType), lines);
        }
    }

    /**
     * Read the content from a file in a standard location, named
     * using the given language and content type.
     */
    public List<List<String>> readContent(String language, String fileType) throws Exception {
        return parseFile(getLanguageFile(language, fileType), fileType);
    }

    /**
     * Write the given content into a set of files in a standard
     * location, named using the language and the given content type.
     */
    public void writeContent(List<PatentContent> contents, String language, String fileType) throws Exception {
        if (! contents.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (PatentContent content : contents) {
                lines.add(formatPatentContent(content, fileType));
            }
            writeLines(getLanguageFile(language, fileType), lines);
        }
    }

    /**
     * Write the given stream into a PDF files in a standard location,
     * named using the given patent information and page number.
     */
    public void writePdfFile(PatentInfo p, int pageId, InputStream s) throws Exception {
        writeStream(getPdfFile(p, pageId), s);
    }

    /**
     * Do files of the given type already exist for all the given languages?
     */
    public boolean allFilesExist(Collection<String> languages, String fileType) {
        for (String language : languages) {
            File f = getLanguageFile(language, fileType);
            if (! f.exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * How many entries of the given type have been downloaded?
     */
    public long countEntries(String language, String fileType) {
        long total = 0;
        File f = getLanguageFile(language, fileType);
        if (f.exists()) {
            try (Stream<String> stream = Files.lines(f.toPath())) {
                total = stream.count();
            } catch (IOException e) {
                // ignore
            }
        }
        return total;
    }

    /**
     * Does the file relating to the given patent information and page
     * number already exist?
     */
    public boolean pdfFileExists(PatentInfo p, int pageId) {
        return getPdfFile(p, pageId).exists();
    }

    /**
     * Have all the PDF files for the given patent already been downloaded?
     */
    public boolean allPdfFilesExist(PatentInfo p, Map<String, List<String>> downloadedPdfs) {
        String docId = p.getDocdbId();
        if (downloadedPdfs.containsKey(docId)) {
            List<String> patentPdfs = getPdfFileNames(p);
            return downloadedPdfs.get(docId).containsAll(patentPdfs);
        }
        return false;
    }

    /**
     * How many of the PDF files for the given patent have been downloaded?
     */
    public int countPdfFiles(PatentInfo p, Map<String, List<String>> downloadedPdfs) {
        String docId = p.getDocdbId();
        if (downloadedPdfs.containsKey(docId)) {
            List<String> patentPdfs = getPdfFileNames(p);
            patentPdfs.retainAll(downloadedPdfs.get(docId));
            return patentPdfs.size();
        }
        return 0;
    }

    /**
     * Get a list of the PDF files that have already been downloaded for each patent.
     */
    public Map<String, List<String>> findPdfFiles() {
        Map<String, List<String>> result = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resultDir.toPath(), "*.pdf")) {
            for (Path path: stream) {
                String name = path.toFile().getName();
                String docId = parseDocId(name.replaceFirst("-\\d+-\\d+\\.pdf$", ""));
                if (! result.containsKey(docId)) {
                    result.put(docId, new ArrayList<>());
                }
                result.get(docId).add(name);
            }
        } catch (Exception ex) {
            LOGGER.warn("problem finding PDF files", ex);
        }
        return result;
    }

    // -------------------------------------------------------------------------------

    private File getInfoFile() {
        String fileName = String.join("-", countryCode, year, "info") + ".txt";
        return new File(resultDir, fileName);
    }

    private File getIdsFile() {
        String fileName = String.join("-", "ids", countryCode, year) + ".txt";
        return new File(resultDir, fileName);
    }

    private File getIdsFile(String fileType) {
        String fileName = String.join("-", "ids", countryCode, year, fileType) + ".txt";
        return new File(resultDir, fileName);
    }

    private File getMissingIdsFile(String fileType) {
        return getIdsFile(getMissingFileType(fileType));
    }

    private File getLanguageFile(String language, String fileType) {
        String fileName =  String.join("-", countryCode, language, year, fileType) + ".tab";
        return new File(resultDir, fileName);
    }

    private File getPdfFile(PatentInfo p, int pageId) {
        String fileName = getPdfFileName(p, pageId);
        return new File(resultDir, fileName);
    }

    private String getPdfFileName(PatentInfo p, int pageId) {
        StringBuilder buf = new StringBuilder();
        buf.append(formatDocId(p.getDocdbId()));
        buf.append("-").append(p.getNPages());
        buf.append("-").append(pageId).append(".pdf");
        return buf.toString();
    }

    private List<String> getPdfFileNames(PatentInfo p) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < p.getNPages(); i++) {
            result.add(getPdfFileName(p, i+1));
        }
        return result;
    }

    // -------------------------------------------------------------------------------

    private static String getMissingFileType(String fileType) {
        switch (fileType) {
        case CLAIM_FILE:
            return MISSING_CLAIM_FILE;
        case DESCRIPTION_FILE:
            return MISSING_DESCRIPTION_FILE;
        case IMAGE_FILE:
            return MISSING_IMAGE_FILE;
        default:
            return fileType;
        }
    }

    private static List<PatentInfo> readInfoFile(File file) throws Exception {
        List<PatentInfo> info = new ArrayList<>();
        if (file.exists()) {
            for (String line : readLines(file)) {
                info.add(PatentInfo.fromString(line));
            }
        }
        return info;
    }

    private static void writeStream(File file, InputStream stream) throws Exception {
        FileUtils.copyInputStreamToFile(stream, file);
    }

    private static void writeString(File file, String content) throws Exception {
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

    private static void writeLines(File file, List<?> lines) throws Exception {
        FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, "\n");
    }

    private static List<String> readLines(File file) throws Exception {
        List<String> result = new ArrayList<>();
        if (file.exists()) {
            result.addAll(FileUtils.readLines(file, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String formatPatentContent(PatentContent value, String fileType) {
        PatentInfo p = value.getInfo();
        StringBuilder buf = new StringBuilder();
        buf.append(formatDocId(p.getDocdbId())).append("\t");
        buf.append(formatDate(p.getDate())).append("\t");
        buf.append(formatContent(value.getValue(), fileType));
        return buf.toString();
    }

    private static List<List<String>> parseFile(File file, String fileType) throws Exception {
        List<List<String>> values = new ArrayList<>();
        if (file.exists()) {
            for (String line : readLines(file)) {
                String[] parts = line.split("\t", 3);
                if (parts.length < 3) {
                    LOGGER.trace(String.format("  bad line: %d parts", parts.length));
                    LOGGER.trace(line.substring(0, Math.min(40, line.length())));
                    continue;
                }
                String docId = parseDocId(parts[0]);
                // ignore the middle part (the date)
                String value = parseContent(parts[2], fileType);
                values.add(Arrays.asList(docId, value));
            }
        }
        return values;
    }

    private static String formatDate(String date) {
        return date == null ? "" : date;
    }

    private static String formatDocId(String docId) {
        return docId.replaceAll(PatentInfo.DOCDB_SEPARATOR_PATTERN, "-");
    }

    private static String parseDocId(String docId) {
        return docId.trim().replaceAll("-", PatentInfo.DOCDB_SEPARATOR_PATTERN);
    }

    private static String formatContent(String content, String fileType) {
        return content.trim().replaceAll("\n", "<br>");
    }

    private static String parseContent(String content, String fileType) {
        return content.trim().replaceAll("<br>", "\n");
    }
}
