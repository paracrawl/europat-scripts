package patentdata.opstools;

import java.util.Arrays;
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
 * Helper class for retrieving full-text values from the OPS API.
 *
 * Author: Elaine Farrow
 */
abstract class ValueProcessor extends OpsResultProcessor
    implements OpsQueryGenerator {

    private static final int CHECKPOINT_RATE = 20;
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<String> docIds = new ArrayList<>();
    private final Map<String, List<PatentContent>> contentMap = new HashMap<>();
    private final List<String> missingValues = new ArrayList<>();
    private final String query;
    private final String endPoint;
    private final String fileType;
    private int index = -1;

    protected ValueProcessor(List<PatentInfo> inputInfo, String endPoint, String fileType) {
        super(inputInfo);
        StringBuilder buf = new StringBuilder();
        buf.append(OpsApiHelper.REF_TYPE_PUBLICATION).append("/");
        buf.append(OpsApiHelper.INPUT_FORMAT_DOCDB).append("/");
        query = buf.toString();
        this.endPoint = endPoint;
        this.fileType = fileType;
        // initialise the inputs
        for (PatentInfo p : inputInfo) {
            String docId = p.getDocdbId();
            if (shouldProcess(p)) {
                docIds.add(docId);
            } else {
                LOGGER.debug("  skipping " + docId);
            }
        }
    }

    protected abstract boolean shouldProcess(PatentInfo p);

    protected abstract List<String> getLanguages(PatentInfo p);

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
        buf.append(docIds.get(index)).append("/").append(endPoint);
        return buf.toString();
    }

    @Override
    public void writeResults(PatentResultWriter writer) throws Exception {
        writer.writeMissingIds(missingValues, fileType);
        for (String language : contentMap.keySet()) {
            writer.writeContent(contentMap.get(language), language, fileType);
        }
    }

    @Override
    public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
        // don't rewrite the checkpoint files every time
        if ((docIds.size() - index - 1) % CHECKPOINT_RATE == 0) {
            writeResults(writer);
        }
    }

    @Override
    public void readCheckpointResults(PatentResultWriter writer) throws Exception {
        Set<String> languages = new HashSet<>();
        for (PatentInfo p : getInfo()) {
            languages.addAll(getLanguages(p));
        }
        for (String language : languages) {
            LOGGER.trace(String.format("  reading %s checkpoint file for %s", endPoint, language));
            List<PatentContent> content = new ArrayList<>();
            for (List<String> pair : writer.readContent(language, fileType)) {
                String docId = pair.get(0);
                content.add(new PatentContent(getInfo(docId), pair.get(1)));
                // don't run again on the patents where we already have the result
                docIds.remove(docId);
                LOGGER.debug("  already got " + docId);
            }
            if (! content.isEmpty()) {
                contentMap.put(language, content);
            }
        }
        missingValues.addAll(writer.readMissingIds(fileType));
        // don't run again on the patents where we know the result is unavailable
        docIds.removeAll(missingValues);
    }

    @Override
    public boolean processContentString(String xmlString) throws Exception {
        processResult(xmlString);
        return true;
    }

    @Override
    public boolean processNoResults() throws Exception {
        String docId = docIds.get(index);
        missingValues.add(docId);
        LOGGER.error(XML_MARKER, String.format("*** No %s result found for %s", endPoint, docId));
        return true;
    }

    private void processResult(String xmlString) throws Exception {
        String docId = docIds.get(index);
        LOGGER.trace(XML_MARKER, String.format("*** Processing %s result for %s", endPoint, docId));
        LOGGER.trace(XML_MARKER, xmlString);
        PatentInfo p = getInfo(docId);
        Element docEl = OpsXmlHelper.parseResults(xmlString);
        // result will have either claims or description, not both
        NodeList claims = docEl.getElementsByTagName("claims");
        List<String> clangs = processResults(p, claims, contentMap);
        if (clangs.isEmpty()) {
            if (OpsApiHelper.ENDPOINT_CLAIMS.equals(endPoint)) {
                LOGGER.trace(XML_MARKER, "  no claims found");
                processNoResults();
                LOGGER.error(xmlString);
            }
        } else {
            LOGGER.trace(XML_MARKER, String.format("  claims found for %s", String.join(" ", clangs)));
        }
        NodeList descriptions = docEl.getElementsByTagName("description");
        List<String> dlangs = processResults(p, descriptions, contentMap);
        if (dlangs.isEmpty()) {
            if (OpsApiHelper.ENDPOINT_DESCRIPTION.equals(endPoint)) {
                LOGGER.trace(XML_MARKER, "  no descriptions found");
                processNoResults();
                LOGGER.error(xmlString);
            }
        } else {
            LOGGER.trace(XML_MARKER, String.format("  descriptions found for %s", String.join(" ", dlangs)));
        }
    }

    /**
     * Process the given node list to find the language-value pairs
     * for the given patent and add them to the given map. Return the
     * list of languages.
     */
    protected static List<String> processResults(PatentInfo p, NodeList nodes, Map<String, List<PatentContent>> results) throws Exception {
        List<List<String>> pairs = processContent(nodes);
        List<String> languages = new ArrayList<>();
        for (List<String> pair : pairs) {
            String language = pair.get(0);
            if (language.isEmpty() || "ol".equals(language)) {
                // use the country code instead
                language = p.getCountry();
            }
            if (! results.containsKey(language)) {
                results.put(language, new ArrayList<>());
            }
            results.get(language).add(new PatentContent(p, pair.get(1)));
            languages.add(language);
        }
        return languages;
    }

    /**
     * Process the given XML node list to find the language-value pairs
     * for a single patent.
     */
    private static List<List<String>> processContent(NodeList nodes) throws Exception {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            // look for separate claims
            StringBuilder buf = new StringBuilder();
            NodeList childNodes = el.getElementsByTagName("claim-text");
            if (childNodes.getLength() == 0) {
                buf.append(el.getTextContent());
            } else {
                for (int j = 0; j < childNodes.getLength(); j++) {
                    buf.append(childNodes.item(j).getTextContent().trim());
                    buf.append("\n");
                }
            }
            String content = buf.toString().trim();
            if (content.isEmpty()) {
                continue;
            }
            String lang = el.getAttribute("lang");
            LOGGER.trace(String.format("  found %s: %s", lang, content.substring(0, Math.min(40, content.length()))));
            result.add(Arrays.asList(lang, content));
        }
        return result;
    }
}
