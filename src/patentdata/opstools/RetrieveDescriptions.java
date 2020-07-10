package patentdata.opstools;

import java.util.List;

/**
 * Retrieve a set of patent descriptions from the OPS API.
 *
 * Author: Elaine Farrow
 */
public class RetrieveDescriptions {

    public static List<PatentInfo> run(OpsApiHelper api, PatentResultWriter writer, Logger logger, List<PatentInfo> info) throws Exception {
        DescriptionProcessor p = new DescriptionProcessor(logger, info);
        if (api.callApi(p, p, writer)) {
            info = p.getInfo();
        }
        return info;
    }

    // -------------------------------------------------------------------------------

    protected static class DescriptionProcessor extends ValueProcessor {

        protected DescriptionProcessor(Logger logger, List<PatentInfo> inputInfo) {
            super(logger, inputInfo, OpsApiHelper.ENDPOINT_DESCRIPTION, PatentResultWriter.DESCRIPTION_FILE);
        }

        @Override
        protected boolean shouldProcess(PatentInfo p) {
            if (! p.checkedDescription()) {
                throw new IllegalStateException("Check for full text first.");
            }
            return p.hasDescription();
        }

        @Override
        protected List<String> getLanguages(PatentInfo p) {
            return p.getDescriptions();
        }
    }
}
