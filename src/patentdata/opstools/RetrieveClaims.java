package patentdata.opstools;

import java.util.List;

/**
 * Retrieve a set of patent claims from the OPS API.
 *
 * Author: Elaine Farrow
 */
public class RetrieveClaims {

    public static List<PatentInfo> run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        ClaimsProcessor p = new ClaimsProcessor(logger, info);
        if (api.callApi(p, p, writer)) {
            info = p.getInfo();
        }
        return info;
    }

    // -------------------------------------------------------------------------------

    protected static class ClaimsProcessor extends ValueProcessor {

        protected ClaimsProcessor(Logger logger, List<PatentInfo> inputInfo) {
            super(logger, inputInfo, OpsApiHelper.ENDPOINT_CLAIMS, PatentResultWriter.CLAIM_FILE);
        }

        @Override
        protected boolean shouldProcess(PatentInfo p) {
            if (! p.checkedClaims()) {
                throw new IllegalStateException("Check for full text first.");
            }
            return p.hasClaims();
        }

        @Override
        protected List<String> getLanguages(PatentInfo p) {
            return p.getClaims();
        }
    }
}
