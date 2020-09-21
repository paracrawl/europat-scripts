package patentdata.opstools;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Tool for downloading PDFs of patents from the OPS API.
 *
 * PDF patents will not normally be downloaded if the title, abstract,
 * claims, and description are all available as text. This tool does
 * not currently check which languages are available as text before
 * making this decision.
 *
 * The downloadSample method allows a sample of PDF patents to be
 * downloaded that do have all four parts available as text. The
 * patents chosen are simply the first to be found. They are not
 * selected at random.
 *
 * Author: Elaine Farrow
 */
public class DownloadPdfPatents {

    // For now, we won't download very long PDFs
    public static final int MAX_PAGES = 25;

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, List<PatentInfo> info) throws Exception {
        PdfDownloader p = new PdfDownloader(info, writer);
        return api.callApi(p, p, writer);
    }

    public static boolean downloadSample(OpsApiHelper api, PatentResultWriter writer, List<PatentInfo> info, int sampleSize) throws Exception {
        PdfDownloader p = new PdfDownloader(info, writer, sampleSize);
        return api.callApi(p, p, writer);
    }

    // -------------------------------------------------------------------------------

    private static class PdfDownloader
        extends OpsResultProcessor
        implements OpsQueryGenerator {

        private static final Logger LOGGER = LogManager.getLogger();

        private final List<PatentInfo> docInfo = new ArrayList<>();
        private final List<String> missingPdfs = new ArrayList<>();
        private final PatentResultWriter writer;
        private int index = 0;
        private int pageId = 0;

        public PdfDownloader(List<PatentInfo> inputInfo, PatentResultWriter writer) throws Exception {
            this(inputInfo, writer, 0);
        }

        public PdfDownloader(List<PatentInfo> inputInfo, PatentResultWriter writer, int sampleSize) throws Exception {
            super(inputInfo);
            this.writer = writer;
            // initialise the inputs
            int count = 0;
            Map<String, List<String>> downloadedPdfs = writer.findPdfFiles();
            for (PatentInfo p : inputInfo) {
                if (shouldProcess(p, sampleSize > 0)) {
                    if (p.getNPages() > MAX_PAGES) {
                        LOGGER.debug("  skipping " + p.getDocdbId() + " - too many pages");
                        continue;
                    }
                    // if any pages are missing, add this patent to the list
                    if (! writer.allPdfFilesExist(p, downloadedPdfs)) {
                        docInfo.add(p);
                    }
                    count++;
                    if (sampleSize > 0 && count >= sampleSize) {
                        LOGGER.debug("  sampling complete");
                        break;
                    }
                } else {
                    LOGGER.debug("  skipping " + p.getDocdbId());
                }
            }
            if (sampleSize > 0 && count < sampleSize) {
                LOGGER.warn("  sample size not reached: found " + count);
            }
        }

        @Override
        public String getService() {
            return OpsApiHelper.SERVICE_IMAGES;
        }

        @Override
        public boolean hasNext() {
            return pageId < getPageCount();
        }

        @Override
        public String getNextQuery() {
            if (pageId < getPageCount()) {
                pageId++;
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
            writer.writeIds(missingPdfs, PatentResultWriter.MISSING_PDF_FILE);
        }

        @Override
        public void readCheckpointResults(PatentResultWriter writer) throws Exception {
            missingPdfs.addAll(writer.readIds(PatentResultWriter.MISSING_PDF_FILE));
            skipDownloadedPages();
        }

        @Override
        public boolean processContentStream(InputStream inputStream) throws Exception {
            PatentInfo p = getDocInfo();
            writer.writePdfFile(p, pageId, inputStream);
            LOGGER.info(String.format("Saved page %d of %d for %s", pageId, p.getNPages(), p.getDocdbId()));
            skipDownloadedPages();
            return true;
        }

        @Override
        public boolean processNoResults() throws Exception {
            PatentInfo p = getDocInfo();
            missingPdfs.add(getPageInfoString(p, pageId));
            LOGGER.error(String.format("*** PDF page %d of %d not found for %s", pageId, p.getNPages(), p.getDocdbId()));
            skipDownloadedPages();
            return true;
        }

        private int getPageCount() {
            return index < docInfo.size() ? getDocInfo().getNPages() : 0;
        }

        private PatentInfo getDocInfo() {
            return docInfo.get(index);
        }

        private void skipDownloadedPages() {
            for (; index < docInfo.size(); pageId = 0, index++) {
                PatentInfo p = getDocInfo();
                for (; pageId < getPageCount(); pageId++) {
                    int nextPageId = pageId + 1;
                    if (missingPdfs.contains(getPageInfoString(p, nextPageId))) {
                        // this one is unavailable
                        LOGGER.trace(String.format("  skipping page %d of %d for %s (unavailable)", nextPageId, p.getNPages(), p.getDocdbId()));
                    } else if (missingPdfs.remove(getPageInfoString(p.getDocdbId(), nextPageId))) {
                        // this one is unavailable - rewrite entry in new format
                        missingPdfs.add(getPageInfoString(p, nextPageId));
                        LOGGER.trace(String.format("  Skipping page %d of %d for %s (unavailable)", nextPageId, p.getNPages(), p.getDocdbId()));
                    } else if (! writer.pdfFileExists(p, nextPageId)) {
                        // found one we need
                        return;
                    }
                }
            }
        }

        private static String getPageInfoString(PatentInfo p, int pageId) {
            StringBuilder buf = new StringBuilder();
            buf.append(p.getDocdbId());
            buf.append(" page ").append(pageId);
            buf.append(" of ").append(p.getNPages());
            buf.append(" is unavailable");
            return buf.toString();
        }

        private static String getPageInfoString(String docId, int pageId) {
            StringBuilder buf = new StringBuilder();
            buf.append("patent ").append(docId).append(" page ").append(pageId);
            return buf.toString();
        }

        private static boolean shouldProcess(PatentInfo p, boolean sampling) {
            if (sampling) {
                return shouldProcessForSample(p);
            }
            return shouldProcess(p);
        }

        private static boolean shouldProcess(PatentInfo p) {
            List<String> missing = new ArrayList<>();
            if (! p.checkedTitle()) {
                missing.add("title");
            }
            if (! p.checkedAbstract()) {
                missing.add("abstract");
            }
            if (! p.checkedClaims()) {
                missing.add("claims");
            }
            if (! p.checkedDescription()) {
                missing.add("description");
            }
            if (! p.checkedImages()) {
                missing.add("images");
            }
            if (! missing.isEmpty()) {
                String error = String.format("Check for %s first (%s).", String.join(", ", missing), p.getDocdbId());
                throw new IllegalStateException(error);
            }
            if (p.hasTitle() && p.hasAbstract() && p.hasClaims() && p.hasDescription()) {
                return false;
            }
            return p.hasImages();
        }

        private static boolean shouldProcessForSample(PatentInfo p) {
            List<String> missing = new ArrayList<>();
            if (! p.checkedTitle()) {
                missing.add("title");
            }
            if (! p.checkedAbstract()) {
                missing.add("abstract");
            }
            if (! p.checkedClaims()) {
                missing.add("claims");
            }
            if (! p.checkedDescription()) {
                missing.add("description");
            }
            if (! p.checkedImages()) {
                missing.add("images");
            }
            if (! missing.isEmpty()) {
                String error = String.format("Check for %s first (%s).", String.join(", ", missing), p.getDocdbId());
                throw new IllegalStateException(error);
            }
            if (p.getLanguages().get(p.getCountry()) < 4) {
                // skip if any part is not available as text in the main language
                return false;
            }
            return p.hasImages();
        }
    }
}
