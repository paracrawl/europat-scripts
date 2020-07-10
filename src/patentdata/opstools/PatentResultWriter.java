package patentdata.opstools;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

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
    public static final String TITLE_FILE = "title";

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

    // -------------------------------------------------------------------------------

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
     * Write the given lines into a file in a standard location based
     * on the given file type.
     */
    public void writeIds(List<String> lines, String fileType) throws Exception {
        writeLines(getIdsFile(fileType), lines);
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
            StringBuilder buf = new StringBuilder();
            for (PatentContent content : contents) {
                formatPatentContent(buf, content, fileType).append("\n");
            }
            writeString(getLanguageFile(language, fileType), buf.toString());
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
     * Do all the PDF files for the given patent already exist?
     */
    public boolean allPdfFilesExist(PatentInfo p) {
        for (int i = 0; i < p.getNPages(); i++) {
            File f = getPdfFile(p, i+1);
            if (! f.exists()) {
                return false;
            }
        }
        return true;
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

    private File getLanguageFile(String language, String fileType) {
        String fileName =  String.join("-", countryCode, language, year, fileType) + ".tab";
        return new File(resultDir, fileName);
    }

    private File getPdfFile(PatentInfo p, int pageId) {
        StringBuilder buf = new StringBuilder();
        buf.append(formatDocId(p.getDocdbId()));
        buf.append("-").append(p.getNPages());
        buf.append("-").append(pageId).append(".pdf");
        return new File(resultDir, buf.toString());
    }

    // -------------------------------------------------------------------------------

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
        return FileUtils.readLines(file, StandardCharsets.UTF_8);
    }

    private static StringBuilder formatPatentContent(StringBuilder buf, PatentContent value, String fileType) {
        PatentInfo p = value.getInfo();
        buf.append(formatDocId(p.getDocdbId())).append("\t");
        buf.append(formatDate(p.getDate())).append("\t");
        buf.append(formatContent(value.getValue(), fileType));
        return buf;
    }

    private static List<List<String>> parseFile(File file, String fileType) throws Exception {
        List<List<String>> values = new ArrayList<>();
        if (file.exists()) {
            for (String line : readLines(file)) {
                String[] parts = line.split("\t", 2);
                String docId = parseDocId(parts[0]);
                // ignore the middle part (date)
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
