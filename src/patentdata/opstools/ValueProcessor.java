package patentdata.opstools;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Helper class for retrieving full-text values from the OPS API.
 *
 * Author: Elaine Farrow
 */
abstract class ValueProcessor extends OpsResultProcessor
    implements OpsQueryGenerator {

    private final List<String> docIds = new ArrayList<>();
    private final Map<String, List<PatentContent>> contentMap = new HashMap<>();
    private final String query;
    private final String endPoint;
    private final String fileType;
    private int index = -1;

    protected ValueProcessor(Logger logger, List<PatentInfo> inputInfo, String endPoint, String fileType) {
        super(logger, inputInfo);
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
                log("  skipping " + docId);
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
        for (String language : contentMap.keySet()) {
            writer.writeContent(contentMap.get(language), language, fileType);
        }
    }

    @Override
    public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
        writeResults(writer);
    }

    @Override
    public void readCheckpointResults(PatentResultWriter writer) throws Exception {
        Set<String> languages = new HashSet<>();
        for (PatentInfo p : getInfo()) {
            languages.addAll(getLanguages(p));
        }
        for (String language : languages) {
            List<PatentContent> content = new ArrayList<>();
            for (List<String> pair : writer.readContent(language, fileType)) {
                String docId = pair.get(0);
                content.add(new PatentContent(getInfo(docId), pair.get(1)));
                // don't run again on the patents where we already have the result
                docIds.remove(docId);
            }
            if (! content.isEmpty()) {
                contentMap.put(language, content);
            }
        }
    }

    @Override
    public boolean processContentString(String xmlString) throws Exception {
        processResult(xmlString);
        return true;
    }

    private void processResult(String xmlString) throws Exception {
        // log("*** Processing result");
        // log(xmlString);
        PatentInfo p = getInfo(docIds.get(index));
        Element docEl = OpsXmlHelper.parseResults(xmlString);
        // result will have either claims or description, not both
        NodeList claims = docEl.getElementsByTagName("claims");
        processResults(p, claims, contentMap);
        NodeList descriptions = docEl.getElementsByTagName("description");
        processResults(p, descriptions, contentMap);
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
            // log(String.format("  found %s: %s", lang, content.substring(0, Math.min(40, content.length()))));
            result.add(Arrays.asList(lang, content));
        }
        return result;
    }
}
