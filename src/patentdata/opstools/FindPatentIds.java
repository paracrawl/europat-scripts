package patentdata.opstools;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

import java.time.YearMonth;

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

    // IPC: International Patent Classification
    private static final String[] IPC_SECTIONS = {"A", "B", "C", "D", "E", "F", "G", "H"};
    private static final int NUM_IPC_CLASSES = 100;

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
                queries.addAll(generateQueries(basicQuery, year.intValue(), allQueries));
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
            writer.writeQueries(queries.subList(hasMore ? queryId : queryId+1, queries.size()));
            writer.writeInfo(getInfo());
            writer.writeIds(getDocIds());
        }

        @Override
        public void readCheckpointResults(PatentResultWriter writer) throws Exception {
            // only run the search once
            if (writer.infoFileExists()) {
                queries.clear();
                // look for remaining queries from previous run
                if (queries.addAll(writer.readQueries())) {
                    LOGGER.debug("  continue search");
                    addInfo(writer.readInfo());
                } else {
                    LOGGER.debug("  search done");
                }
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
            LOGGER.trace(XML_MARKER, "*** Processing patent IDs");
            LOGGER.trace(XML_MARKER, xmlString);
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
            LOGGER.trace(XML_MARKER, String.format("  total results: %d, range end: %d", totalResults, rangeEnd));
            if (totalResults > MAX_RESULTS && isFirstBatch()) {
                LOGGER.debug(XML_MARKER, String.format("  total results %d is over limit of %d", totalResults, MAX_RESULTS));
                if (expandCurrentQuery()) {
                    // ignore these results and use a smaller granularity instead
                    return;
                } else {
                    LOGGER.error(XML_MARKER, String.format("%d results will not be retrieved", totalResults - MAX_RESULTS));
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
                LOGGER.trace(XML_MARKER, String.format("No more results for this query"));
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

        // -------------------------------------------------------------------------------

        static List<String> generateQueries(String basicQuery, int year, Map<String, List<String>> queries) throws Exception {
            // query the whole year
            StringBuilder buf = new StringBuilder(basicQuery);
            buf.append(" pd=").append(year);
            String yearQuery = makeEncodedQuery(buf.toString());
            queries.put(yearQuery, generateQueriesByMonth(basicQuery, year, queries));
            return Collections.singletonList(yearQuery);
        }

        static List<String> generateQueriesByMonth(String basicQuery, int year, Map<String, List<String>> queries) throws Exception {
            // query one month at a time
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (int month = 1; month <= 12; month++) {
                StringBuilder buf = new StringBuilder(basicQuery);
                buf.append(" pd=").append(String.format("%d%02d", year, month));
                String monthQuery = makeEncodedQuery(buf.toString());
                map.put(monthQuery, generateQueriesByDayBlocks(basicQuery, year, month, queries));
            }
            queries.putAll(map);
            return new ArrayList<>(map.keySet());
        }

        static List<String> generateQueriesByDayBlocks(String basicQuery, int year, int month, Map<String, List<String>> queries) throws Exception {
            // query in ~6-day chunks
            int[] days = new int[]{1, 7, 13, 19, 25};
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (int i = 0; i < days.length; i++) {
                int startDay = days[i];
                int endDay = -1;
                StringBuilder buf = new StringBuilder(basicQuery);
                String start = String.format("%d%02d%02d", year, month, startDay);
                if (i < days.length - 1) {
                    endDay += days[i+1];
                    String end = String.format("%d%02d%02d", year, month, endDay);
                    buf.append(" pd within ");
                    buf.append("\"").append(start).append(" ").append(end).append("\"");
                } else {
                    buf.append(" pd=").append(String.format("%d%02d", year, month));
                    buf.append(" and pd>=").append(start);
                }
                String daysQuery = makeEncodedQuery(buf.toString());
                map.put(daysQuery, generateQueriesByDays(basicQuery, year, month, startDay, endDay, queries));
            }
            queries.putAll(map);
            return new ArrayList<>(map.keySet());
        }

        static List<String> generateQueriesByDays(String basicQuery, int year, int month, int startDay, int endDay, Map<String, List<String>> queries) throws Exception {
            // query one day at a time
            if (endDay < 0) {
                endDay = YearMonth.of(year, month).lengthOfMonth();
            }
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (int day = startDay; day <= endDay; day++) {
                StringBuilder buf = new StringBuilder(basicQuery);
                buf.append(" pd=").append(String.format("%d%02d%02d", year, month, day));
                String dayQuery = makeEncodedQuery(buf.toString());
                map.put(dayQuery, generateQueriesByIpcSection(buf.toString(), queries));
            }
            queries.putAll(map);
            return new ArrayList<>(map.keySet());
        }

        static List<String> generateQueriesByIpcSection(String basicQuery, Map<String, List<String>> queries) throws Exception {
            // query one IPC section at a time
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (String ipcSection : IPC_SECTIONS) {
                StringBuilder buf = new StringBuilder(basicQuery);
                buf.append(" ipc=").append(String.format("%s", ipcSection));
                String query = makeEncodedQuery(buf.toString());
                map.put(query, generateQueriesByIpcClass(basicQuery, ipcSection, queries));
            }
            queries.putAll(map);
            return new ArrayList<>(map.keySet());
        }

        static List<String> generateQueriesByIpcClass(String basicQuery, String ipcSection, Map<String, List<String>> queries) throws Exception {
            // query one IPC class at a time
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (int ipcClass = 1; ipcClass < NUM_IPC_CLASSES; ipcClass++) {
                StringBuilder buf = new StringBuilder(basicQuery);
                buf.append(" ipc=").append(String.format("%s%02d", ipcSection, ipcClass));
                String query = makeEncodedQuery(buf.toString());
                map.put(query, null);
            }
            return new ArrayList<>(map.keySet());
        }

        static String makeEncodedQuery(String query) throws Exception {
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
