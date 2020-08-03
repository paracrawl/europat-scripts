package patentdata.opstools;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class FindPatentIds {

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, String countryCode, Integer year, List<PatentInfo> info) throws Exception {
        PatentIdProcessor p = new PatentIdProcessor(countryCode, year, info);
        if (api.callApi(p, p, writer)) {
            info.clear();
            info.addAll(p.getInfo());
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------------

    private static class PatentIdProcessor
        extends OpsResultProcessor
        implements OpsQueryGenerator {

        // OPS API won't allow us to access more than this many results from a single query
        private static final int MAX_RESULTS = 2000;
        private static final Logger LOGGER = LogManager.getLogger();

        private final int batchSize = 100;
        private final Map<String, List<String>> allQueries = new HashMap<>();
        private final List<String> queries = new ArrayList<>();
        private int start = 1;
        private int queryId = -1;
        private boolean hasMore = false;

        public PatentIdProcessor(String countryCode, Integer year, List<PatentInfo> info) throws Exception {
            super(info);
            StringBuilder buf = new StringBuilder();
            if (countryCode != null) {
                buf.append(" pn=").append(countryCode);
            }
            String basicQuery = buf.toString();
            if (year == null) {
                queries.add(makeEncodedQuery(basicQuery));
            } else {
                allQueries.putAll(generateQueries(basicQuery, year.intValue()));
                queries.addAll(allQueries.get(null));
            }
        }

        public boolean addPatent(String docId) {
            // skip patent applications for now
            String kind = PatentInfo.getKind(docId);
            if (kind.startsWith("A")) {
                LOGGER.debug("  skipping " + docId);
                return false;
            }
            return true;
        }

        @Override
        public String getService() {
            return OpsApiHelper.SERVICE_SEARCH;
        }

        @Override
        public boolean hasNext() {
            return hasMore || (queryId + 1) < queries.size();
        }

        @Override
        public String getNextQuery() {
            if (hasMore) {
                start += batchSize;
            } else {
                // go on to the next query
                start = 1;
                hasMore = true;
                queryId++;
            }
            StringBuilder buf = new StringBuilder();
            int end  = start + batchSize - 1;
            buf.append("?Range=").append(start).append("-").append(end);
            buf.append(queries.get(queryId));
            return buf.toString();
        }

        @Override
        public void writeResults(PatentResultWriter writer) throws Exception {
            // nothing to do
        }

        @Override
        public void writeCheckpointResults(PatentResultWriter writer) throws Exception {
            writer.writeInfo(getInfo());
            writer.writeIds(getDocIds());
        }

        @Override
        public void readCheckpointResults(PatentResultWriter writer) throws Exception {
            // only run the search once
            if (writer.infoFileExists()) {
                queries.clear();
            }
        }

        @Override
        public boolean processContentString(String xmlString) throws Exception {
            processResult(xmlString);
            return true;
        }

        @Override
        public boolean processNoResults() throws Exception {
            // accept no results as a valid response
            hasMore = false;
            return true;
        }

        private void processResult(String xmlString) throws Exception {
            LOGGER.trace("*** Processing patent IDs");
            LOGGER.trace(xmlString);
            int rangeEnd = 0;
            int totalResults = 0;
            Element docEl = OpsXmlHelper.parseResults(xmlString);
            NodeList biblio = docEl.getElementsByTagNameNS(OpsApiHelper.OPS_NS, "biblio-search");
            if (biblio.getLength() > 0) {
                Element el = (Element) biblio.item(0);
                String count = el.getAttribute("total-result-count");
                totalResults = Integer.parseUnsignedInt(count);
                NodeList nodes = el.getElementsByTagNameNS(OpsApiHelper.OPS_NS, "range");
                String end = ((Element) nodes.item(0)).getAttribute("end");
                rangeEnd = Integer.parseUnsignedInt(end);
            }
            LOGGER.trace(String.format("  total results: %d, range end: %d", totalResults, rangeEnd));
            if (totalResults > MAX_RESULTS && isFirstBatch()) {
                LOGGER.debug(String.format("  total results %d is over limit of %d", totalResults, MAX_RESULTS));
                if (expandCurrentQuery()) {
                    // ignore these results and use a smaller granularity instead
                    return;
                } else {
                    LOGGER.warn(String.format("%d results will not be retrieved", totalResults - MAX_RESULTS));
                }
            }

            NodeList nodes = docEl.getElementsByTagNameNS(OpsApiHelper.OPS_NS, "publication-reference");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String familyId = el.getAttribute("family-id");
                String docId = OpsXmlHelper.getDocNumber(el, OpsApiHelper.INPUT_FORMAT_DOCDB);
                if (addPatent(docId)) {
                    // add a new entry for this patent
                    getInfo(docId).setFamily(familyId);
                }
            }
            if (rangeEnd >= totalResults || rangeEnd >= MAX_RESULTS || nodes.getLength() == 0) {
                hasMore = false;
                LOGGER.trace(String.format("No more results for this query"));
            }
        }

        private boolean isFirstBatch() {
            return start == 1;
        }

        private boolean expandCurrentQuery() {
            String currentQuery = queries.get(queryId);
            if (allQueries.containsKey(currentQuery)) {
                List<String> childQueries = allQueries.get(currentQuery);
                LOGGER.trace(String.format("  -> got %d new queries", childQueries.size()));
                queries.addAll(queryId+1, childQueries);
                hasMore = false;
                return true;
            }
            return false;
        }

        private Map<String, List<String>> generateQueries(String basicQuery, int year) throws Exception {
            // query the whole year
            Map<String, List<String>> map = new LinkedHashMap<>();
            StringBuilder buf = new StringBuilder(basicQuery);
            buf.append(" pd=").append(year);
            String yearQuery = makeEncodedQuery(buf.toString());
            map.put(null, Collections.singletonList(yearQuery));
            Map<String, List<String>> monthQueries = generateQueriesByMonth(basicQuery, year);
            map.put(yearQuery, new ArrayList<>(monthQueries.keySet()));
            map.putAll(monthQueries);
            return map;
        }

        private Map<String, List<String>> generateQueriesByMonth(String basicQuery, int year) throws Exception {
            // query one month at a time
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (int month = 1; month < 13; month++) {
                StringBuilder buf = new StringBuilder(basicQuery);
                buf.append(" pd=").append(String.format("%d%02d", year, month));
                String monthQuery = makeEncodedQuery(buf.toString());
                map.put(monthQuery, generateQueriesByDays(basicQuery, year, month));
            }
            return map;
        }

        private List<String> generateQueriesByDays(String basicQuery, int year, int month) throws Exception {
            // query in ~6-day chunks to avoid end-of-month date calculations
            List<String> list = new ArrayList<>();
            int[] days = new int[]{1, 7, 13, 19, 25};
            for (int i = 0; i < days.length; i++) {
                StringBuilder buf = new StringBuilder(basicQuery);
                String start = String.format("%d%02d%02d", year, month, days[i]);
                if (i < days.length - 1) {
                    String end = String.format("%d%02d%02d", year, month, days[i+1]-1);
                    buf.append(" pd within ");
                    buf.append("\"").append(start).append(" ").append(end).append("\"");
                } else {
                    buf.append(" pd=").append(String.format("%d%02d", year, month));
                    buf.append(" and pd>=").append(start);
                }
                String daysQuery = makeEncodedQuery(buf.toString());
                list.add(daysQuery);
            }
            return list;
        }

        private String makeEncodedQuery(String query) throws Exception {
            LOGGER.trace(String.format("query: %s", query));
            StringBuilder buf = new StringBuilder();
            if (! query.isEmpty()) {
                buf.append("&q=");
                buf.append(URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
            }
            return buf.toString();
        }
    }
}
