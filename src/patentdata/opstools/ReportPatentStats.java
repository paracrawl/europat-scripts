package patentdata.opstools;

import java.util.List;

/**
 * Methods for reporting stats about patents.
 *
 * This class uses the information directly from the info file.
 *
 * Author: Elaine Farrow
 */
public class ReportPatentStats {

    public static List<PatentInfo> run(String countryCode, Integer year, List<PatentInfo> info) {
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
        System.out.println(String.format("%8d with title", withTitle));
        System.out.println(String.format("%8d with abstract", withAbstract));
        System.out.println(String.format("%8d with claims", withClaims));
        System.out.println(String.format("%8d with description", withDescription));
        System.out.println(String.format("%8d with PDFs", withImages));
        System.out.println(String.format("%8d with all text parts", withAllText));
        System.out.println(String.format("%8d with needed PDF files", withNeededImages));
        System.out.println(String.format("%8.1f PDF pages on average (mean)", averageImages));
        System.out.println(String.format("%8d with missing information", missingInfo));
        return info;
    }
}
