package patentdata.opstools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tool for fetching information about full text available from the
 * OPS API.
 *
 * This tool does not fetch the actual values, just the information
 * about which full text values are available.
 *
 * Author: Elaine Farrow
 */
public class FindFullText {

    public static List<PatentInfo> run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        FullTextProcessor p = new FullTextProcessor(logger, info);
        if (api.callApi(p, p, writer)) {
            info = p.getInfo();
        }
        return info;
    }

    // -------------------------------------------------------------------------------

    private static class FullTextProcessor
        extends OpsResultProcessor
        implements OpsQueryGenerator {

        private final List<String> docIds = new ArrayList<>();
        private final List<String> idsWithClaims = new ArrayList<>();
        private final List<String> idsWithDescription = new ArrayList<>();
        private final String query;
        private int index = -1;

        public FullTextProcessor(Logger logger, List<PatentInfo> inputInfo) {
            super(logger, inputInfo);
            StringBuilder buf = new StringBuilder();
            buf.append(OpsApiHelper.REF_TYPE_PUBLICATION).append("/");
            buf.append(OpsApiHelper.INPUT_FORMAT_DOCDB).append("/");
            query = buf.toString();
            // initialise the inputs and outputs
            for (PatentInfo p : inputInfo) {
                String docId = p.getDocdbId();
                if (p.checkedClaims() || p.checkedDescription()) {
                    log("  skipping " + docId);
                } else {
                    docIds.add(docId);
                }
                updateInfo(p);
            }
        }

        public List<String> getIdsWithClaims() {
            return sort(idsWithClaims);
        }

        public List<String> getIdsWithDescriptions() {
            return sort(idsWithDescription);
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
            buf.append(OpsApiHelper.ENDPOINT_FULLTEXT);
            return buf.toString();
        }

        @Override
        public void writeResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
            writer.writeInfo(getInfo());
            writer.writeIds(getIdsWithClaims(), PatentResultWriter.CLAIM_FILE);
            writer.writeIds(getIdsWithDescriptions(), PatentResultWriter.DESCRIPTION_FILE);
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
            PatentInfo p = getInfo(docIds.get(index));
            p.setClaims(Collections.emptyList());
            p.setDescriptions(Collections.emptyList());
            return true;
        }

        private void updateInfo(PatentInfo p) {
            String docId = p.getDocdbId();
            if (p.hasClaims()) {
                idsWithClaims.add(docId);
            }
            if (p.hasDescription()) {
                idsWithDescription.add(docId);
            }
        }

        private void processResult(String xmlString) throws Exception {
            // log("*** Processing full text");
            // log(xmlString);
            Element docEl = OpsXmlHelper.parseResults(xmlString);
            String docId = OpsXmlHelper.getDocNumber(docEl, OpsApiHelper.INPUT_FORMAT_DOCDB);
            List<String> claimsLanguages = new ArrayList<>();
            List<String> descriptionLanguages = new ArrayList<>();
            NodeList nodes = docEl.getElementsByTagNameNS(OpsApiHelper.OPS_NS, "fulltext-instance");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String lang = el.getAttribute("lang");
                if (lang.isEmpty()) {
                    lang = PatentInfo.getCountry(docId);
                }
                String desc = el.getAttribute("desc");
                if ("claims".equals(desc)) {
                    claimsLanguages.add(lang);
                } else if ("description".equals(desc)) {
                    descriptionLanguages.add(lang);
                }
            }
            PatentInfo p = getInfo(docId);
            p.setClaims(claimsLanguages);
            p.setDescriptions(descriptionLanguages);
            updateInfo(p);
        }
    }
}
