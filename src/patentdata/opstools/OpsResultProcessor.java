package patentdata.opstools;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Methods for collecting and processing OPS API results.
 *
 * Author: Elaine Farrow
 */
public abstract class OpsResultProcessor {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Marker for logging information about XML processing.
     */
    protected static final Marker XML_MARKER = MarkerManager.getMarker("OPS_XML");

    /**
     * Information about patents, indexed by document ID.
     */
    private final Map<String, PatentInfo> info = new TreeMap<>();

    // -------------------------------------------------------------------------------

    /**
     * Initialise with the given patent information.
     */
    public OpsResultProcessor(Collection<PatentInfo> inputInfo) {
        for (PatentInfo p : inputInfo) {
            info.put(p.getDocdbId(), p);
        }
    }
        
    // -------------------------------------------------------------------------------

    /**
     * Writes out the final results.
     */
    public abstract void writeResults(PatentResultWriter writer) throws Exception;

    /**
     * Writes out partial results to a checkpoint.
     */
    public abstract void writeCheckpointResults(PatentResultWriter writer) throws Exception;

    /**
     * Reads partial results from a checkpoint to support restart.
     */
    public abstract void readCheckpointResults(PatentResultWriter writer) throws Exception;

    /**
     * Gets the list of document IDs.
     */
    public List<String> getDocIds() {
        return new ArrayList<>(info.keySet());
    }

    /**
     * Gets the information about patents.
     */
    public List<PatentInfo> getInfo() {
        return new ArrayList<>(info.values());
    }

    /**
     * Processes the response from the OPS server.
     */
    public boolean processServerResponse(HttpResponse response) throws Exception {
        if (response != null) {
            InputStream inputStream = response.getEntity().getContent();
            return processContentStream(inputStream);
        }
        return false;
    }

    /**
     * Processes the content stream returned from the OPS server.
     */
    public boolean processContentStream(InputStream stream) throws Exception {
        return stream == null ? null : processContentString(OpsApiHelper.streamToString(stream));
    }

    /**
     * Processes the content returned from the OPS server as a string.
     */
    public boolean processContentString(String contentString) throws Exception {
        return true;
    }

    /**
     * Processes the response from the OPS server when there are no results.
     */
    public boolean processNoResults() throws Exception {
        return false;
    }

    // -------------------------------------------------------------------------------

    /**
     * Is there information about the given patent?
     */
    protected boolean hasInfo(String docId) {
        return info.containsKey(docId);
    }

    /**
     * Gets the information about the given patent, creating an entry if needed.
     */
    protected PatentInfo getInfo(String docId) {
        if (! info.containsKey(docId)) {
            info.put(docId, new PatentInfo(docId));
        }
        return info.get(docId);
    }

    /**
     * Stores the given patent information.
     */
    protected void addInfo(Collection<PatentInfo> inputInfo) {
        for (PatentInfo p : inputInfo) {
            String docId = p.getDocdbId();
            if (info.containsKey(docId)) {
                LOGGER.error(String.format("Duplicate entry for %s - skipped", docId));
            } else {
                info.put(docId, p);
            }
        }
    }

    /**
     * Makes a new sorted list from the input.
     */
    protected static <T extends Comparable<? super T>> List<T> sort(Collection<T> values) {
        List<T> result = new ArrayList<T>(values);
        Collections.sort(result);
        return result;
    }
}
