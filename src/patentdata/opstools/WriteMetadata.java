package patentdata.opstools;

import java.util.List;

/**
 * Write out patent metadata to file.
 *
 * Author: Elaine Farrow
 */
public class WriteMetadata {

    public static boolean run(OpsApiHelper api, PatentResultWriter writer, List<PatentInfo> info) throws Exception {
        writer.writeMetadata(info);
        return true;
    }
}
