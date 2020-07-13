package patentdata.opstools;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Methods for reporting stats about patents.
 *
 * This class uses the information directly from the info file.
 *
 * Author: Elaine Farrow
 */
public class ReportPatentStats {

    public static List<PatentInfo> run(String countryCode, Integer year, List<PatentInfo> info) {
        Map<String, Integer> titles = new TreeMap<>();
        Map<String, Integer> abstracts = new TreeMap<>();
        Map<String, Integer> claims = new TreeMap<>();
        Map<String, Integer> descriptions = new TreeMap<>();
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
        int missingInfo = 0;
        for (PatentInfo p : info) {
            totalPatents++;
            if (p.checkedTitle() && p.checkedAbstract() && p.checkedClaims() && p.checkedDescription() && p.checkedImages()) {
                patentsProcessed++;
            }
            if (p.hasTitle()) {
                withTitle++;
            }
            if (p.hasAbstract()) {
                withAbstract++;
            }
            if (p.hasClaims()) {
                withClaims++;
            }
            if (p.hasDescription()) {
                withDescription++;
            }
            for (String lang : p.getTitles()) {
                increment(titles, lang);
            }
            for (String lang : p.getAbstracts()) {
                increment(abstracts, lang);
            }
            for (String lang : p.getClaims()) {
                increment(claims, lang);
            }
            for (String lang : p.getDescriptions()) {
                increment(descriptions, lang);
            }
            if (p.hasImages()) {
                withImages++;
            }
            if (p.hasTitle() && p.hasAbstract() && p.hasClaims() && p.hasDescription()) {
                withAllText++;
            } else if (p.hasImages()) {
                withNeededImages++;
                totalNeededImages += p.getNPages();
            } else {
                missingInfo++;
            }
        }
        double averageImages = withNeededImages == 0 ? 0 : totalNeededImages/ withNeededImages;

        System.out.println(String.format("Results for %s %d", countryCode, year));
        System.out.println(String.format("%8d patents found", totalPatents));
        System.out.println(String.format("%8d patents fully processed", patentsProcessed));
        System.out.println(String.format("%8d with any title", withTitle));
        for (String lang : titles.keySet()) {
            System.out.println(String.format("%8d with title in language \"%s\"", titles.get(lang), lang));
        }
        System.out.println(String.format("%8d with any abstract", withAbstract));
        for (String lang : abstracts.keySet()) {
            System.out.println(String.format("%8d with abstract in language \"%s\"", abstracts.get(lang), lang));
        }
        System.out.println(String.format("%8d with any claims", withClaims));
        for (String lang : claims.keySet()) {
            System.out.println(String.format("%8d with claims in language \"%s\"", claims.get(lang), lang));
        }
        System.out.println(String.format("%8d with any description", withDescription));
        for (String lang : descriptions.keySet()) {
            System.out.println(String.format("%8d with description in language \"%s\"", descriptions.get(lang), lang));
        }
        System.out.println(String.format("%8d with all text parts in any language", withAllText));
        System.out.println(String.format("%8d with any PDFs", withImages));
        System.out.println(String.format("%8d with PDFs we want (%d pages total, mean %.1f per patent)", withNeededImages, totalNeededImages, averageImages));
        System.out.println(String.format("%8d with missing information", missingInfo));
        return info;
    }

    private static void increment(Map<String, Integer> map, String key) {
        int count = map.containsKey(key) ? map.get(key) : 0;
        map.put(key, count + 1);
    }
}
