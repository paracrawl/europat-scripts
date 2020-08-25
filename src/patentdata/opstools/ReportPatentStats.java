package patentdata.opstools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Methods for reporting stats about patents.
 *
 * This class uses the information directly from the info file.
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

    private static void printStats(PatentResultWriter writer, List<PatentInfo> info) {
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
        int withImages = 0;
        int withAllText = 0;
        int withNeededImages = 0;
        int totalNeededImages = 0;
        int totalDownloadedImages = 0;
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
                withImages++;
            }
            if (p.checkedTitle() && p.checkedAbstract() && p.checkedClaims() && p.checkedDescription() && p.checkedImages()) {
                patentsProcessed++;
                if (p.hasTitle() && p.hasAbstract() && p.hasClaims() && p.hasDescription()) {
                    withAllText++;
                } else if (p.hasImages()) {
                    withNeededImages++;
                    totalNeededImages += p.getNPages();
                    totalDownloadedImages += writer.countPdfFiles(p);
                } else {
                    missingInfo++;
                }
            }
        }
        double averageImages = withNeededImages == 0 ? 0 : totalNeededImages/ withNeededImages;

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
            System.out.println(String.format("%8d with any PDFs", withImages));
            if (withImages > 0) {
                System.out.println(String.format("%8d with PDFs we want", withNeededImages));
            }
            if (missingInfo > 0) {
                System.out.println(String.format("%8d with missing information", missingInfo));
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
            if (withNeededImages > 0) {
                System.out.println(String.format("  PDF pages:"));
                System.out.println(String.format("%8d PDF pages needed (mean %.1f per patent)", totalNeededImages, averageImages));
                System.out.println(String.format("%8d PDF pages downloaded already", totalDownloadedImages));
                System.out.println(String.format("%8d PDF pages still to download", totalNeededImages - totalDownloadedImages));
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
