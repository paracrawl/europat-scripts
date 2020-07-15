package patentdata.opstools;

import java.util.List;

/**
 * Retrieve a set of patent claims from the OPS API.
 *
 * Author: Elaine Farrow
 */
public class RetrieveClaims {

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        ClaimsProcessor p = new ClaimsProcessor(logger, info);
        if (api.callApi(p, p, writer)) {
            info.clear();
            info.addAll(p.getInfo());
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------------

    protected static class ClaimsProcessor extends ValueProcessor {

        protected ClaimsProcessor(Logger logger, List<PatentInfo> inputInfo) {
            super(logger, inputInfo, OpsApiHelper.ENDPOINT_CLAIMS, PatentResultWriter.CLAIM_FILE);
        }

        @Override
        protected boolean shouldProcess(PatentInfo p) {
            if (! p.checkedClaims()) {
                throw new IllegalStateException(String.format("Check for full text first (%s).", p.getDocdbId()));
            }
            return p.hasClaims();
        }

        @Override
        protected List<String> getLanguages(PatentInfo p) {
            return p.getClaims();
        }
    }
}
