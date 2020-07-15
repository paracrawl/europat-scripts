package patentdata.opstools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Retrieve a set of patent titles and abstracts from the OPS API.
 *
 * Author: Elaine Farrow
 */
public class RetrieveBiblio {

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, List<PatentInfo> info) throws Exception {
        BiblioProcessor p = new BiblioProcessor(info);
        if (api.callApi(p, p, writer)) {
            info.clear();
            info.addAll(p.getInfo());
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------------

    protected static class BiblioProcessor
        extends OpsResultProcessor
        implements OpsQueryGenerator {

        private static final Logger LOGGER = LogManager.getLogger();

        private final List<String> docIds = new ArrayList<>();
        private final Map<String, List<PatentContent>> titleMap = new HashMap<>();
        private final Map<String, List<PatentContent>> abstractMap = new HashMap<>();
        private final String query;
        private final int batchSize = 100;
        private int start = -batchSize;

        protected BiblioProcessor(List<PatentInfo> inputInfo) {
            super(inputInfo);
            StringBuilder buf = new StringBuilder();
            buf.append(OpsApiHelper.REF_TYPE_PUBLICATION).append("/");
            buf.append(OpsApiHelper.INPUT_FORMAT_DOCDB).append("/");
            query = buf.toString();
            // initialise the inputs
            for (PatentInfo p : inputInfo) {
                String docId = p.getDocdbId();
                if (p.checkedTitle() && p.checkedAbstract() && ! (p.hasTitle() || p.hasAbstract())) {
                    // we know there's no title or abstract -- skip
                    LOGGER.debug("  skipping " + docId);
                } else {
                    docIds.add(docId);
                }
            }
        }

        @Override
        public String getService() {
            return OpsApiHelper.SERVICE_PUBLISHED_DATA;
        }
        
        @Override
        public boolean hasNext() {
            return (start + batchSize) < docIds.size();
        }
        
        @Override
        public String getNextQuery() {
            start += batchSize;
            int end = Math.min(docIds.size(), start + batchSize);
            String ids = String.join(",", docIds.subList(start, end));
            StringBuilder buf = new StringBuilder(query);
            buf.append(ids).append("/").append(OpsApiHelper.ENDPOINT_BIBLIO);
            return buf.toString();
        }

        @Override
        public void writeResults(PatentResultWriter writer) throws Exception {
            for (String language : titleMap.keySet()) {
                writer.writeContent(titleMap.get(language), language, PatentResultWriter.TITLE_FILE);
            }
            for (String language : abstractMap.keySet()) {
                writer.writeContent(abstractMap.get(language), language, PatentResultWriter.ABSTRACT_FILE);
            }
        }
        
        @Override
        public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
            writer.writeInfo(getInfo());
            writeResults(writer);
        }

        @Override
        public void readCheckpointResults(PatentResultWriter writer) throws Exception {
            Set<String> titleLanguages = new HashSet<>();
            Set<String> abstractLanguages = new HashSet<>();
            for (PatentInfo p : getInfo()) {
                if (! p.checkedTitle() || ! p.checkedAbstract()) {
                    // more to do - ignore checkpoint data
                    return;
                }
                titleLanguages.addAll(p.getTitles());
                abstractLanguages.addAll(p.getAbstracts());
            }
            // if all patents have been checked, and all expected
            // checkpoint files exist, assume we are finished
            if (writer.allFilesExist(titleLanguages, PatentResultWriter.TITLE_FILE)
                && writer.allFilesExist(abstractLanguages, PatentResultWriter.ABSTRACT_FILE)) {
                docIds.clear();
            }
        }

        @Override
        public boolean processContentString(String xmlString) throws Exception {
            processResult(xmlString);
            return true;
        }

        private void processResult(String xmlString) throws Exception {
            LOGGER.trace("*** Processing result");
            LOGGER.trace(xmlString);
            Element docEl = OpsXmlHelper.parseResults(xmlString);
            NodeList docNodes = docEl.getElementsByTagName("exchange-document");
            for (int i = 0; i < docNodes.getLength(); i++) {
                Element docNode = (Element) docNodes.item(i);
                String docId = OpsXmlHelper.getDocNumber(docNode, OpsApiHelper.INPUT_FORMAT_DOCDB);
                String date = OpsXmlHelper.getDocDate(docNode, OpsApiHelper.INPUT_FORMAT_DOCDB);
                PatentInfo p = getInfo(docId);
                if (date != null) {
                    p.setDate(date);
                }
                NodeList titles = docNode.getElementsByTagName("invention-title");
                p.setTitles(ValueProcessor.processResults(p, titles, titleMap));
                NodeList abstracts = docNode.getElementsByTagName("abstract");
                p.setAbstracts(ValueProcessor.processResults(p, abstracts, abstractMap));
            }
        }
    }
}
