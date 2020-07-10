package patentdata.opstools;

import patentdata.utils.Log;

/**
 * Logger class for this package.
 *
 * Author: Elaine Farrow
 */
public class Logger {

    private final Log log;

    public Logger(String logDirName) throws Exception {
        log = new Log(logDirName);
    }

    /**
     * Writes the given string to the log.
     */
    public Exception log(String s) {
        try {
            log.print(s);
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    /**
     * Writes the given error string to the log.
     */
    public Exception logErr(String s) {
        try {
            log.printErr(s);
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    /**
     * Writes the given exception and string to the log.
     */
    public Exception logErr(Exception e, String s) {
        try {
            log.printErr(e, s);
        } catch (Exception e2) {
            return e2;
        }
        return null;
    }
}
