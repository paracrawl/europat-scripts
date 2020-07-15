package patentdata.opstools;

import java.util.List;

/**
 * Retrieve a set of patent descriptions from the OPS API.
 *
 * Author: Elaine Farrow
 */
public class RetrieveDescriptions {

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        DescriptionProcessor p = new DescriptionProcessor(logger, info);
        if (api.callApi(p, p, writer)) {
            info.clear();
            info.addAll(p.getInfo());
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------------

    protected static class DescriptionProcessor extends ValueProcessor {

        protected DescriptionProcessor(Logger logger, List<PatentInfo> inputInfo) {
            super(logger, inputInfo, OpsApiHelper.ENDPOINT_DESCRIPTION, PatentResultWriter.DESCRIPTION_FILE);
        }

        @Override
        protected boolean shouldProcess(PatentInfo p) {
            if (! p.checkedDescription()) {
                throw new IllegalStateException(String.format("Check for full text first (%s).", p.getDocdbId()));
            }
            return p.hasDescription();
        }

        @Override
        protected List<String> getLanguages(PatentInfo p) {
            return p.getDescriptions();
        }
    }
}
