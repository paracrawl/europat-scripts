package patentdata.opstools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Methods for reporting stats about patents.
 *
 * This class uses the information directly from the info file.
 *
 * It also looks inside the configured output directories to see how
 * many files have been downloaded so far.
 *
 * Author: Elaine Farrow
 */
public class ReportPatentStats {

    public static boolean run(PatentResultWriter writer, String countryCode, Integer year, List<PatentInfo> info) {
        if (writer.infoFileExists()) {
            System.out.println(String.format("Results for %s %d", countryCode, year));
            printStats(writer, info);
        } else {
            System.out.println(String.format("No results for %s %d", countryCode, year));
        }
        System.out.println();
        return true;
    }

    // -------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker REPORT_MARKER = MarkerManager.getMarker("REPORT");

    private static void printStats(PatentResultWriter writer, List<PatentInfo> info) {
        Map<String, List<String>> downloadedPdfs = writer.findPdfFiles();
        Map<String, List<String>> downloadedSamplePdfs = writer.getSampleWriter().findPdfFiles();
        Map<String, Integer> titles = new TreeMap<>();
        Map<String, Integer> abstracts = new TreeMap<>();
        Map<String, Integer> claims = new TreeMap<>();
        Map<String, Integer> descriptions = new TreeMap<>();
        Map<String, Integer> allText = new TreeMap<>();
        Set<String> anyText = new HashSet<>();
        int totalPatents = 0;
        int patentsProcessed = 0;
        int withTitle = 0;
        int withAbstract = 0;
        int withClaims = 0;
        int withDescription = 0;
        int withPages = 0;
        int withAllText = 0;
        int withTooManyPages = 0;
        int withUnreadPages = 0;
        int withNeededPages = 0;
        int totalUnreadPages = 0;
        int totalNeededPages = 0;
        int totalUnreadDownloadedPages = 0;
        int totalNeededDownloadedPages = 0;
        int withDownloadedSamplePages = 0;
        int totalDownloadedSamplePages = 0;
        int missingInfo = 0;
        for (PatentInfo p : info) {
            totalPatents++;
            if (p.hasTitle()) {
                withTitle++;
                increment(titles, p.getTitles());
            }
            if (p.hasAbstract()) {
                withAbstract++;
                increment(abstracts, p.getAbstracts());
            }
            if (p.hasClaims()) {
                withClaims++;
                increment(claims, p.getClaims());
            }
            if (p.hasDescription()) {
                withDescription++;
                increment(descriptions, p.getDescriptions());
            }
            Map<String, Integer> languages = p.getLanguages();
            for (String lang : languages.keySet()) {
                anyText.add(lang);
                if (languages.get(lang) == 4) {
                    PatentInfo.increment(allText, lang);
                }
            }
            if (p.hasImages()) {
                withPages++;
            }
            if (p.checkedTitle() && p.checkedAbstract() && p.checkedClaims() && p.checkedDescription() && p.checkedImages()) {
                patentsProcessed++;
                if (p.hasTitle() && p.hasAbstract() && p.hasClaims() && p.hasDescription()) {
                    withAllText++;
                    LOGGER.trace(REPORT_MARKER, p.getDocdbId() + " all text available");
                    if (p.hasImages()) {
                        int nDownloadedSamplePages = writer.countPdfFiles(p, downloadedSamplePdfs);
                        if (nDownloadedSamplePages > 0) {
                            withDownloadedSamplePages++;
                            totalDownloadedSamplePages += nDownloadedSamplePages;
                        }
                    }
                } else if (p.hasImages()) {
                    int nPages = p.getNPages();
                    int nDownloadedPages = writer.countPdfFiles(p, downloadedPdfs);
                    withUnreadPages++;
                    totalUnreadPages += nPages;
                    totalUnreadDownloadedPages += nDownloadedPages;
                    if (nPages > DownloadPdfPatents.MAX_PAGES) {
                        withTooManyPages++;
                        LOGGER.trace(REPORT_MARKER, p.getDocdbId() + " too many pages");
                    } else {
                        withNeededPages++;
                        totalNeededPages += nPages;
                        totalNeededDownloadedPages += nDownloadedPages;
                        LOGGER.trace(REPORT_MARKER, p.getDocdbId() + " okay");
                    }
                } else {
                    missingInfo++;
                    LOGGER.trace(REPORT_MARKER, p.getDocdbId() + " information missing");
                }
            }
        }

        System.out.println(String.format("%8d patents found", totalPatents));
        if (totalPatents > 0) {
            if (titles.isEmpty() || titles.size() > 1) {
                System.out.println(String.format("%8d with any title", withTitle));
            }
            if (abstracts.isEmpty() || abstracts.size() > 1) {
                System.out.println(String.format("%8d with any abstract", withAbstract));
            }
            if (claims.isEmpty() || claims.size() > 1) {
                System.out.println(String.format("%8d with any claims", withClaims));
            }
            if (descriptions.isEmpty() || descriptions.size() > 1) {
                System.out.println(String.format("%8d with any description", withDescription));
            }
            if (allText.isEmpty() || allText.size() > 1) {
                System.out.println(String.format("%8d with all text parts in any language", withAllText));
            }
            if (missingInfo > 0) {
                System.out.println(String.format("%8d with missing information", missingInfo));
            }
            System.out.println(String.format("%8d with any PDFs", withPages));
            if (withUnreadPages > 0) {
                System.out.println(String.format("%8d with PDFs we might want", withUnreadPages));
                System.out.println(String.format("%8d PDF pages found", totalUnreadPages));
                System.out.println(String.format("%8d PDF pages downloaded", totalUnreadDownloadedPages));
                if (withTooManyPages > 0) {
                    System.out.println(String.format("%8d with too many pages (limit %d)", withTooManyPages, DownloadPdfPatents.MAX_PAGES));
                }
                System.out.println(String.format("%8d with PDFs we need", withNeededPages));
            }
            StringBuilder allDownloads = new StringBuilder();
            for (String lang : OpsResultProcessor.sort(anyText)) {
                StringBuilder downloads = new StringBuilder();
                System.out.println(String.format("  text in language \"%s\":", lang));
                if (titles.containsKey(lang)) {
                    System.out.println(String.format("%8d with title", titles.get(lang)));
                    long count = writer.countEntries(lang, PatentResultWriter.TITLE_FILE);
                    if (count > 0) {
                        downloads.append(String.format("%8d titles\n", count));
                    }
                }
                if (abstracts.containsKey(lang)) {
                    System.out.println(String.format("%8d with abstract", abstracts.get(lang)));
                    long count = writer.countEntries(lang, PatentResultWriter.ABSTRACT_FILE);
                    if (count > 0) {
                        downloads.append(String.format("%8d abstracts\n", count));
                    }
                }
                if (claims.containsKey(lang)) {
                    System.out.println(String.format("%8d with claims", claims.get(lang)));
                    long count = writer.countEntries(lang, PatentResultWriter.CLAIM_FILE);
                    if (count > 0) {
                        downloads.append(String.format("%8d claims\n", count));
                    }
                }
                if (descriptions.containsKey(lang)) {
                    System.out.println(String.format("%8d with description", descriptions.get(lang)));
                    long count = writer.countEntries(lang, PatentResultWriter.DESCRIPTION_FILE);
                    if (count > 0) {
                        downloads.append(String.format("%8d descriptions\n", count));
                    }
                }
                if (downloads.length() > 0) {
                    allDownloads.append(String.format("  downloaded in language \"%s\":\n", lang));
                    allDownloads.append(downloads);
                }
                if (allText.containsKey(lang)) {
                    System.out.println(String.format("%8d with all text parts", allText.get(lang)));
                }
            }
            System.out.print(allDownloads);
            if (withNeededPages > 0) {
                System.out.println(String.format("  PDF pages:"));
                double averagePages = totalNeededPages/ (double) withNeededPages;
                System.out.println(String.format("%8d PDF pages needed (mean %.1f per patent)", totalNeededPages, averagePages));
                System.out.println(String.format("%8d PDF pages downloaded already", totalNeededDownloadedPages));
                System.out.println(String.format("%8d PDF pages still to download", totalNeededPages - totalNeededDownloadedPages));
            }
            if (withDownloadedSamplePages > 0) {
                System.out.println(String.format("  additional sample PDF pages:"));
                System.out.println(String.format("%8d PDF pages downloaded from %d sample patents", totalDownloadedSamplePages, withDownloadedSamplePages));
            }
            if (totalPatents > patentsProcessed) {
                System.out.println(String.format("  to do:"));
                System.out.println(String.format("%8d patents still to check", totalPatents - patentsProcessed));
            }
        }
    }

    private static void increment(Map<String, Integer> map, List<String> languages) {
        for (String lang : languages) {
            PatentInfo.increment(map, lang);
        }
    }
}
