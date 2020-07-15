package patentdata.opstools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tool for fetching information about images available from the OPS
 * API.
 *
 * This tool does not fetch the actual images, just the information
 * about which images are available.
 *
 * Author: Elaine Farrow
 */
public class FindImages {

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        ImageResultProcessor p = new ImageResultProcessor(logger, info);
        if (api.callApi(p, p, writer)) {
            info.clear();
            info.addAll(p.getInfo());
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------------

    private static class ImageResultProcessor
        extends OpsResultProcessor
        implements OpsQueryGenerator {

        private static final String IMAGE_PREFIX = OpsApiHelper.SERVICE_IMAGES + "/";

        private final List<String> docIds = new ArrayList<>();
        private final List<String> idsWithImages = new ArrayList<>();
        private final String query;
        private int index = -1;

        public ImageResultProcessor(Logger logger, List<PatentInfo> inputInfo) {
            super(logger, inputInfo);
            StringBuilder buf = new StringBuilder();
            buf.append(OpsApiHelper.REF_TYPE_PUBLICATION).append("/");
            buf.append(OpsApiHelper.INPUT_FORMAT_DOCDB).append("/");
            query = buf.toString();
            // initialise the inputs and outputs
            for (PatentInfo p : inputInfo) {
                String docId = p.getDocdbId();
                if (p.checkedImages()) {
                    log("  skipping " + docId);
                } else {
                    docIds.add(docId);
                }
                updateInfo(p);
            }
        }

        public List<String> getIdsWithImages() {
            return sort(idsWithImages);
        }

        @Override
        public String getService() {
            return OpsApiHelper.SERVICE_PUBLISHED_DATA;
        }

        @Override
        public boolean hasNext() {
            return (index + 1) < docIds.size();
        }

        @Override
        public String getNextQuery() {
            index++;
            StringBuilder buf = new StringBuilder(query);
            buf.append(docIds.get(index)).append("/");
            buf.append(OpsApiHelper.ENDPOINT_IMAGES);
            return buf.toString();
        }

        @Override
        public void writeResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
            writer.writeInfo(getInfo());
            writer.writeIds(getIdsWithImages(), PatentResultWriter.IMAGE_FILE);
        }

        @Override
        public void readCheckpointResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public boolean processContentString(String xmlString) throws Exception {
            processResult(xmlString);
            return true;
        }

        @Override
        public boolean processNoResults() {
            // accept no results as a valid response
            getInfo(docIds.get(index)).setPages(0);
            return true;
        }

        private void updateInfo(PatentInfo p) {
            String docId = p.getDocdbId();
            if (p.hasImages()) {
                idsWithImages.add(docId);
            }
        }

        private void processResult(String xmlString) throws Exception {
            // log("*** Processing images");
            // log(xmlString);
            Element docEl = OpsXmlHelper.parseResults(xmlString);
            String docId = OpsXmlHelper.getDocNumber(docEl, OpsApiHelper.INPUT_FORMAT_DOCDB);
            PatentInfo p = getInfo(docId);
            int nPages = 0;
            NodeList nodes = docEl.getElementsByTagNameNS(OpsApiHelper.OPS_NS, "document-instance");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String desc = el.getAttribute("desc");
                if ("FullDocument".equals(desc)) {
                    String link = el.getAttribute("link");
                    if (link.startsWith(IMAGE_PREFIX)) {
                        link = link.substring(IMAGE_PREFIX.length());
                    }
                    nPages = Integer.parseUnsignedInt(el.getAttribute("number-of-pages"));
                    p.setImages(link);
                    break;
                }
            }
            p.setPages(nPages);
            updateInfo(p);
        }
    }
}
