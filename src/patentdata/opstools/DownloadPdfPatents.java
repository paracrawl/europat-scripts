package patentdata.opstools;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool for downloading PDFs of patents from the OPS API.
 *
 * PDF patents will NOT be downloaded if the title, abstract, claims,
 * and description are all available as text. This tool does not
 * currently check which languages are available as text before making
 * this decision.
 *
 * Author: Elaine Farrow
 */
public class DownloadPdfPatents {

    public static List<PatentInfo> run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        PdfDownloader g = new PdfDownloader(logger, info, writer);
        api.callApi(g, g, writer);
        // return the input info unchanged
        return info;
    }

    // -------------------------------------------------------------------------------

    private static class PdfDownloader
        extends OpsResultProcessor
        implements OpsQueryGenerator {

        private final List<PatentInfo> docInfo = new ArrayList<>();
        private final PatentResultWriter writer;
        private int index = -1;
        private int pageId = 1;

        public PdfDownloader(Logger logger, List<PatentInfo> inputInfo, PatentResultWriter writer) throws Exception {
            super(logger, inputInfo);
            this.writer = writer;
            // initialise the inputs
            for (PatentInfo p : inputInfo) {
                // if any pages are missing, download this patent again
                if (shouldProcess(p) && ! writer.allPdfFilesExist(p)) {
                    docInfo.add(p);
                } else {
                    log("  skipping " + p.getDocdbId());
                }
            }
        }

        @Override
        public String getService() {
            return OpsApiHelper.SERVICE_IMAGES;
        }

        @Override
        public boolean hasNext() {
            return pageId < getPageCount() || (index + 1) < docInfo.size();
        }

        @Override
        public String getNextQuery() {
            if (pageId < getPageCount()) {
                pageId++;
            } else {
                pageId = 1;
                index++;
            }
            StringBuilder buf = new StringBuilder();
            buf.append(getDocInfo().getImages()).append(".pdf");
            buf.append("?Range=").append(pageId);
            return buf.toString();
        }

        @Override
        public void writeResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public void readCheckpointResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public boolean processContentStream(InputStream inputStream) throws Exception {
            PatentInfo p = getDocInfo();
            writer.writePdfFile(p, pageId, inputStream);
            log(String.format("Saved page %d of %d for %s", pageId, p.getNPages(), p.getDocdbId()));
            return true;
        }

        private int getPageCount() {
            return (index >= 0 && index < docInfo.size()) ? getDocInfo().getNPages() : 0;
        }

        private PatentInfo getDocInfo() {
            return docInfo.get(index);
        }

        private static boolean shouldProcess(PatentInfo p) {
            if (! p.checkedTitle()) {
                throw new IllegalStateException("Check for title first.");
            }
            if (! p.checkedAbstract()) {
                throw new IllegalStateException("Check for abstract first.");
            }
            if (! p.checkedClaims() || ! p.checkedDescription()) {
                throw new IllegalStateException("Check for full text first.");
            }
            if (! p.checkedImages()) {
                throw new IllegalStateException("Check for images first.");
            }
            if (p.hasTitle() && p.hasAbstract() && p.hasClaims() && p.hasDescription()) {
                return false;
            }
            return p.hasImages();
        }
    }
}
