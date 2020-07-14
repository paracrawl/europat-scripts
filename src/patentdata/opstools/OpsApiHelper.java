package patentdata.opstools;

import java.io.InputStream;
import java.io.StringWriter;

import java.net.SocketTimeoutException;
import java.net.URI;

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

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.json.JSONObject;

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

    private final Logger logger;
    private final Map<String, Map<Integer, Date>> allowedRates = new HashMap<>();
    private final Map<String, List<Date>> recentCalls = new HashMap<>();
    private final String authUrl;
    private final String authString;
    private final String serviceUrl;
    private String accessToken = "";

    public OpsApiHelper(OpsConfigHelper config) throws Exception {
        logger = config.getLogger();
        authUrl = config.getAuthUrl();
        authString = config.getAuthString();
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
        try (CloseableHttpClient client = initClient()) {
            while (g.hasNext()) {
                String service = g.getService();
                // abide by service allowed rate
                manageAllowedRate(service);
                // construct API call
                String urlString = makeUrlString(service, g.getNextQuery());
                if (handleCall(client, urlString, p)) {
                    logCall(service);
                    p.writeCheckpointResults(cw);
                    continue;
                }
                // something wrong - stop
                logger.logErr(String.format("API call failed"));
                return false;
            }
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

    public static String streamToString(InputStream inputStream) throws Exception {
        try {
            return inputStream == null ? "" : IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
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

    private boolean updateRates(String rateInfo) {
        // logger.log(String.format("Update: %s", rateInfo));
        boolean isOverloaded = rateInfo.contains("overloaded");
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
        return isOverloaded;
    }

    // -------------------------------------------------------------------------------

    private String makeUrlString(String service, String query) {
        StringBuilder buf = new StringBuilder();
        buf.append(serviceUrl);
        buf.append(service).append("/");
        buf.append(query);
        return buf.toString();
    }

    private boolean handleCall(CloseableHttpClient client, String urlString, OpsResultProcessor p) throws Exception {
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
                return processResponse(client, urlString, p, false);
            } catch (ConnectionClosedException|SSLException|ConnectTimeoutException|SocketTimeoutException e) {
                retries++;
            }
        }
        return false;
    }

    private boolean processResponse(CloseableHttpClient client, String urlString, OpsResultProcessor p, boolean renewCredentials) throws Exception {
        if (renewCredentials) {
            renewCredentials(client);
        }
        boolean retry;
        long msDelay = 0;
        String delayMessage = "";
        CloseableHttpResponse response = getUrlResponse(client, urlString);
        try {
            Header retryAfter = response.getFirstHeader("Retry-After");
            Header rejection = response.getFirstHeader("X-Rejection-Reason");
            Header throttling = response.getFirstHeader("X-Throttling-Control");
            int statusCode = response.getStatusLine().getStatusCode();
            if (throttling != null) {
                logger.log(String.valueOf(throttling));
                boolean isOverloaded = updateRates(throttling.getValue());
                if (isOverloaded) {
                    // add an arbitrary delay before the next call
                    int delay = 5;
                    msDelay = TimeUnit.MILLISECONDS.convert(delay, TimeUnit.MINUTES);
                    delayMessage = String.format("Server overloaded. Waiting %d minutes...", delay);
                }
            }
            if (HttpStatus.SC_OK == statusCode) {
                // success - log call and process result
                return p.processServerResponse(response);
            } else if (retryAfter != null) {
                msDelay = Integer.parseInt(retryAfter.getValue());
                delayMessage = String.format("Retry after %d milliseconds...", msDelay);
                retry = true;
            } else if ("IndividualQuotaPerHour".equals(rejection)) {
                int delay = 60;
                msDelay = TimeUnit.MILLISECONDS.convert(delay, TimeUnit.MINUTES);
                delayMessage = String.format("Hourly quota exceeded: retry after %d minutes...", delay);
                retry = true;
            } else {
                String output = streamToString(response.getEntity().getContent());
                if (output.contains("<message>No results found</message>")) {
                    // no results - log call and inform the application
                    return p.processNoResults();
                } else if (output.contains("<message>invalid_access_token</message>")) {
                    // renew credentials then retry once
                    retry = ! renewCredentials;
                    renewCredentials = true;
                } else if (output.contains("<code>CLIENT.RobotDetected</code>")) {
                    // retry after an arbitrary delay
                    int delay = 3;
                    msDelay = TimeUnit.MILLISECONDS.convert(delay, TimeUnit.SECONDS);
                    delayMessage = String.format("CLIENT.RobotDetected. Wait %d seconds to reconnect...", delay);
                    retry = true;
                } else {
                    // some other issue - log for human inspection
                    logger.log(String.format("Headers: %s", Arrays.asList(response.getAllHeaders())));
                    logger.log(String.format("Output: %s", output));
                    retry = false;
                }
            }
        } finally {
            // consume and close the reponse
            EntityUtils.consumeQuietly(response.getEntity());
            response.close();
            if (msDelay > 0) {
                logger.log(delayMessage);
                try {
                    TimeUnit.MILLISECONDS.sleep(msDelay);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        if (retry) {
            return processResponse(client, urlString, p, renewCredentials);
        }
        return false;
    }

    private CloseableHttpResponse getUrlResponse(CloseableHttpClient client, String urlString) throws Exception {
        if (accessToken.isEmpty()) {
            renewCredentials(client);
        }
        HttpGet request = new HttpGet(new URI(urlString));
        request.addHeader("Authorization", "Bearer " + accessToken);
        CloseableHttpResponse response = client.execute(request);
        logger.log(String.format("%s : %s", response.getStatusLine(), urlString));
        return response;
    }

    private void renewCredentials(CloseableHttpClient client) throws Exception {
        logger.log(String.format("Renewing credentials..."));
        HttpPost request = new HttpPost(new URI(authUrl));
        request.addHeader("Authorization", "Basic " + authString);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        List<BasicNameValuePair> params = Arrays.asList(new BasicNameValuePair("grant_type", "client_credentials"));
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = client.execute(request)) {
            String output = streamToString(response.getEntity().getContent());
            accessToken = getJSONValue(new JSONObject(output), "access_token");
        }
        logger.log(String.format(" ... done"));
    }

    private static String getJSONValue(JSONObject json, String name) {
        return (json != null && json.has(name)) ? json.get(name).toString() : null;
    }

    private static CloseableHttpClient initClient() {
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000).build();
        return HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
    }
}
