package patentdata.opstools;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import patentdata.utils.Config;
import patentdata.utils.Connector;

/**
 * Helper class for calling the OPS API with a set of queries.
 *
 * This class manages the throttling behaviour.
 *
 * Author: Elaine Farrow
 */
public class OpsApiHelper {

    public static final String OPS_NS = "http://ops.epo.org";
    public static final String OPS_FULLTEXT_NS = "http://www.epo.org/fulltext";

    public static final String CONSTITUENT_ABSTRACT = "abstract";
    public static final String CONSTITUENT_BIBLIO = "biblio";
    public static final String CONSTITUENT_FULLCYCLE = "full-cycle";

    public static final String ENDPOINT_ABSTRACT = "abstract";
    public static final String ENDPOINT_BIBLIO = "biblio";
    public static final String ENDPOINT_CLAIMS = "claims";
    public static final String ENDPOINT_DESCRIPTION = "description";
    public static final String ENDPOINT_EQUIVALENTS = "equivalents";
    public static final String ENDPOINT_FULLTEXT = "fulltext";
    public static final String ENDPOINT_IMAGES = "images";

    public static final String INPUT_FORMAT_DOCDB = "docdb";
    public static final String INPUT_FORMAT_EPODOC = "epodoc";

    public static final String REF_TYPE_APPLICATION = "application";
    public static final String REF_TYPE_PRIORITY = "priority";
    public static final String REF_TYPE_PUBLICATION = "publication";

    public static final String SERVICE_FAMILY = "family";
    public static final String SERVICE_IMAGES = "published-data/images";
    public static final String SERVICE_LEGAL = "legal";
    public static final String SERVICE_PUBLISHED_DATA = "published-data";
    public static final String SERVICE_SEARCH = "published-data/search";

    public static final String THROTTLE_IMAGES = "images";
    public static final String THROTTLE_INPADOC = "inpadoc";
    public static final String THROTTLE_OTHER = "other";
    public static final String THROTTLE_RETRIEVAL = "retrieval";
    public static final String THROTTLE_SEARCH = "search";

    private final Connector connector;
    private final Logger logger;
    private final Map<String, Map<Integer, Date>> allowedRates = new HashMap<>();
    private final Map<String, List<Date>> recentCalls = new HashMap<>();
    private final String serviceUrl;

    public OpsApiHelper(OpsConfigHelper config) throws Exception {
        connector = config.getConnector();
        logger = config.getLogger();
        serviceUrl = config.getServiceUrl();
    }

    // -------------------------------------------------------------------------------

    /**
     * Calls the API with all the queries from the given generator,
     * processes the results with the given processor, and writes
     * incremental results using the given writer.
     */
    public boolean callApi(OpsQueryGenerator g, OpsResultProcessor p, PatentResultWriter w) throws Exception {
        PatentResultWriter cw = w.getCheckpointWriter();
        p.readCheckpointResults(cw);
        while (g.hasNext()) {
            String service = g.getService();
            // abide by service allowed rate
            manageAllowedRate(service);
            // construct API call
            String urlString = makeUrlString(service, g.getNextQuery());
            // logger.log(String.format("URL: %s", urlString));
            if (processResponse(urlString, p)) {
                logCall(service);
                p.writeCheckpointResults(cw);
                continue;
            }
            // something wrong - stop
            logger.logErr(String.format("API call failed"));
            return false;
        }
        // no more queries - all done
        p.writeResults(w);
        return true;
    }

    // -------------------------------------------------------------------------------

    /**
     * Gets the name of the throttle for the given service.
     */
    public static String getThrottle(String service) {
        if (SERVICE_FAMILY.equals(service)) {
            return THROTTLE_INPADOC;
        }
        if (SERVICE_IMAGES.equals(service)) {
            return THROTTLE_IMAGES;
        }
        if (SERVICE_LEGAL.equals(service)) {
            return THROTTLE_INPADOC;
        }
        if (SERVICE_PUBLISHED_DATA.equals(service)) {
            return THROTTLE_RETRIEVAL;
        }
        if (SERVICE_SEARCH.equals(service)) {
            return THROTTLE_SEARCH;
        }
        return THROTTLE_OTHER;
    }

    // -------------------------------------------------------------------------------

    private void manageAllowedRate(String service) {
        // logger.log(String.format("Rates: %s", allowedRates));
        Instant tooOld = Instant.now().minus(60, ChronoUnit.SECONDS);
        String throttle = getThrottle(service);
        Integer allowed = null;
        synchronized (allowedRates) {
            if (allowedRates.containsKey(throttle)) {
                Map<Integer, Date> map = allowedRates.get(throttle);
                for (Iterator <Map.Entry<Integer, Date>> it = map.entrySet().iterator();
                     it.hasNext();) {
                    Map.Entry<Integer, Date> e = it.next();
                    if (e.getValue().toInstant().isBefore(tooOld)) {
                        it.remove();
                    } else {
                        allowed = e.getKey();
                    }
                }
            }
        }
        synchronized (recentCalls) {
            if (recentCalls.containsKey(throttle)) {
                List<Date> calls = recentCalls.get(throttle);
                Collections.sort(calls);
                Instant oldest = null;
                for (Iterator <Date> it = calls.iterator();
                     it.hasNext();) {
                    oldest = it.next().toInstant();
                    if (oldest.isBefore(tooOld)) {
                        it.remove();
                    } else {
                        break;
                    }
                }
                if (allowed != null && calls.size() >= allowed.intValue()) {
                    long wait = Duration.between(tooOld, oldest).toMillis();
                    logger.log(String.format("Self-throttle: waiting %d ms...", wait));
                    try {
                        TimeUnit.MILLISECONDS.sleep(wait);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            } else {
                recentCalls.put(throttle, new ArrayList<Date>()); 
            }
        }
    }

    private void logCall(String service) {
        String throttle = getThrottle(service);
        synchronized (recentCalls) {
            if (! recentCalls.containsKey(throttle)) {
                recentCalls.put(throttle, new ArrayList<Date>()); 
            }
            recentCalls.get(throttle).add(new Date());
        }
    }

    private void updateRates(String rateInfo) {
        // logger.log(String.format("Update: %s", rateInfo));
        if (rateInfo.contains("overloaded")) {
            // arbitrary choice of delay
            int delay = 10;
            logger.log(String.format("Server overloaded. Waiting %d seconds...", delay));
            try {
                TimeUnit.SECONDS.sleep(delay);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        Pattern p = Pattern.compile("\\b(\\w+)=\\w+:(\\d+)\\b");
        Matcher m = p.matcher(rateInfo);
        while (m.find()) {
            String service = m.group(1);
            Integer limit = Integer.valueOf(m.group(2));
            synchronized (allowedRates) {
                if (! allowedRates.containsKey(service)) {
                    allowedRates.put(service, new TreeMap<Integer, Date>()); 
                }
                allowedRates.get(service).put(limit, new Date());
            }
        }
    }

    // -------------------------------------------------------------------------------

    private String makeUrlString(String service, String query) {
        StringBuilder buf = new StringBuilder();
        buf.append(serviceUrl);
        buf.append(service).append("/");
        buf.append(query);
        return buf.toString();
    }

    private boolean processResponse(String urlString, OpsResultProcessor p) throws Exception {
        int retries = 0;
        while (retries < 3) {
            if (retries > 0) {
                // if the connection drops, retry after arbitrary delay
                int delay = 10;
                logger.log(String.format("Connection dropped. Retry after %d seconds...", delay));
                try {
                    TimeUnit.SECONDS.sleep(delay);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            try {
                return processResponse(urlString, p, false);
            } catch (SSLException e) {
                retries++;
            }
        }
        return false;
    }

    private boolean processResponse(String urlString, OpsResultProcessor p, boolean renewCredentials) throws Exception {
        if (renewCredentials) {
            renewCredentials();
        }
        HttpResponse response = getUrlResponse(urlString);
        Header throttling = response.getFirstHeader("X-Throttling-Control");
        if (throttling != null) {
            logger.log(String.valueOf(throttling));
            updateRates(throttling.getValue());
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK == statusCode) {
            // success - log call and process result
            return p.processServerResponse(response);
        }
        Header retry = response.getFirstHeader("Retry-After");
        if (retry != null) {
            int millis = Integer.parseInt(retry.getValue());
            logger.log(String.format("Retry after %d milliseconds...", millis));
            try {
                TimeUnit.MILLISECONDS.sleep(millis);
            } catch (InterruptedException e) {
                // ignore
            }
            return processResponse(urlString, p);
        }
        String output = streamToString(response.getEntity().getContent());
        if (output.contains("<message>invalid_access_token</message>")) {
            // renew credentials then retry once
            if (! renewCredentials) {
                return processResponse(urlString, p, true);
            }
        } else if (output.contains("<code>CLIENT.RobotDetected</code>")) {
            // arbitrary sleep then retry
            Integer n = 3;
            logger.log(String.format("CLIENT.RobotDetected. Wait %d seconds to reconnect...", n));
            try {
                TimeUnit.SECONDS.sleep(n);
            } catch (InterruptedException e) {
                // ignore
            }
            return processResponse(urlString, p);
        } else if (output.contains("<message>No results found</message>")) {
            // no results - log call and inform the application
            return p.processNoResults();
        }
        logger.log(String.format("Headers: %s", Arrays.asList(response.getAllHeaders())));
        logger.log(String.format("Output: %s", output));
        return false;
    }

    private String streamToString(InputStream inputStream) throws Exception {
        return inputStream == null ? null : IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    private HttpResponse getUrlResponse(String urlString) throws Exception {
        return connector.goTo(urlString);
    }

    private void renewCredentials() throws Exception {
        logger.log(String.format("Renewing credentials..."));
        connector.getToken();
        logger.log(String.format(" ... done"));
    }
}
