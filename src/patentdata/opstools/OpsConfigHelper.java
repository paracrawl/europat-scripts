package patentdata.opstools;

import patentdata.utils.Config;
import patentdata.utils.Connector;

import org.apache.commons.codec.binary.Base64;

/**
 * Helper class for configuring our application.
 *
 * Author: Elaine Farrow
 */
public class OpsConfigHelper {

    private final Connector connector;
    private final Logger logger;
    private final String authUrl;
    private final String authString;
    private final String serviceUrl;
    private final String workingDirName;

    public OpsConfigHelper(String path) throws Exception {
        Config config = new Config(path);
        connector = new Connector(config);
        workingDirName = config._config.WorkingDir;
        logger = new Logger(workingDirName);
        authString = Base64.encodeBase64String((config._config.ConsumerKey + ":" + config._config.ConsumerSecret).getBytes());
        authUrl = config._config.AuthenURL;
        serviceUrl = config._config.ServiceURL.replaceAll("(?i)(?<!(http:|https:))/+", "/");
    }

    /**
     * Gets the connector.
     */
    public Connector getConnector() {
        return connector;
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
}
