package patentdata.opstools;

import java.io.File;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import org.json.JSONObject;

/**
 * Helper class for configuring our application.
 *
 * Configuration values are read from a JSON file.
 *
 * Author: Elaine Farrow
 */
public class OpsConfigHelper {

    private final String authUrl;
    private final String authString;
    private final String serviceUrl;
    private final String workingDirName;
    private final Logger logger;

    public OpsConfigHelper(String path) throws Exception {
        JSONObject json = readConfigFile(path);
        authString = makeAuthString(json);
        authUrl = makeUrlString(json, "AuthenURL");
        serviceUrl = makeUrlString(json, "ServiceURL");
        workingDirName = json.getString("WorkingDir");
        logger = new Logger(workingDirName);
    }

    /**
     * Gets the logger.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the authorisation URL as a String.
     */
    public String getAuthUrl() {
        return authUrl;
    }

    /**
     * Gets the authorisation String.
     */
    public String getAuthString() {
        return authString;
    }

    /**
     * Gets the service URL as a String.
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Gets the name of the configured working directory.
     */
    public String getWorkingDirName() {
        return workingDirName;
    }

    // -------------------------------------------------------------------------------

    private static String makeAuthString(JSONObject json) throws Exception {
        String consumerKey = json.getString("ConsumerKey");
        String consumerSecret = json.getString("ConsumerSecretKey");
        return Base64.encodeBase64String((consumerKey + ":" + consumerSecret).getBytes());
    }

    private static String makeUrlString(JSONObject json, String name) throws Exception {
        StringBuilder buf = new StringBuilder();
        buf.append(json.getString("Protocol")).append("://");
        buf.append(json.getString("Host"));
        String endPoint = json.getString(name);
        if (! endPoint.startsWith("/")) {
            buf.append("/");
        }
        buf.append(endPoint);
        return buf.toString();
    }

    private static JSONObject readConfigFile(String path) throws Exception {
        return new JSONObject(FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8));
    }
}
