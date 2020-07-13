package patentdata.opstools;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool for running a pipeline of steps for downloading patents from
 * the OPS API.
 *
 * The tool manages the calls to the OPS API, obeying the throttling
 * limits.
 *
 * PDF patents will NOT be downloaded if the title, abstract, claims,
 * and description are all available as text. This tool does not
 * currently check which languages are available as text before making
 * this decision.
 *
 * Author: Elaine Farrow
 */
public class PDFPatentPipeline {

    public static final String STAGE_ALL = "all";
    public static final String STAGE_BIBLIO = "biblio";
    public static final String STAGE_CLAIMS = "claims";
    public static final String STAGE_DESCRIPTION = "description";
    public static final String STAGE_FULLTEXT = "fulltext";
    public static final String STAGE_IMAGES = "images";
    public static final String STAGE_PDF = "pdf";
    public static final String STAGE_SEARCH = "search";
    public static final String STAGE_STATS = "stats";

    // configure the application
    private final OpsConfigHelper config;

    // use the same helper to manage all the API calls
    private final OpsApiHelper api;

    // use the same logger
    private final Logger logger;

    public PDFPatentPipeline(String path) throws Exception {
        config = new OpsConfigHelper(path);
        api = new OpsApiHelper(config);
        logger = config.getLogger();
    }

    public static void main(String... args) throws Exception {
        List<PatentInfo> results = new ArrayList<>();
        PDFPatentPipeline p = new PDFPatentPipeline(args[0]);
        int nArgs = args.length;
        String countryCode = args[1];
        Integer year = Integer.valueOf(args[2]);
        String stage = nArgs > 3 ? args[3] : STAGE_PDF;
        List<String> stages = stagesNeeded(stage);
        System.err.println("Stages: " + stages);
        results = p.runPipeline(countryCode, year, stages);
        if (! STAGE_STATS.equals(stage)) {
            String operation = STAGE_SEARCH.equals(stage) ? "Found" : "Processed";
            System.err.println(results.subList(0, Math.min(10, results.size())));
            System.err.println(operation + " " + results.size() + " records");
        }
    }

    // -------------------------------------------------------------------------------

    public List<PatentInfo> runPipeline(String countryCode, Integer year, List<String> stages) throws Exception {
        PatentResultWriter writer = new PatentResultWriter(config.getWorkingDirName(), countryCode, year);
        // initialise values from the master copy
        List<PatentInfo> info = writer.readInfo();
        for (String stage : stages) {
            logger.log("** Starting " + stage + " stage **");
            writer.setCheckpointDir("ops_" + stage);
            try {
                switch(stage) {
                case STAGE_BIBLIO:
                    info = RetrieveBiblio.run(api, writer, logger, info);
                    break;
                case STAGE_CLAIMS:
                    info = RetrieveClaims.run(api, writer, logger, info);
                    break;
                case STAGE_DESCRIPTION:
                    info = RetrieveDescriptions.run(api, writer, logger, info);
                    break;
                case STAGE_FULLTEXT:
                    info = FindFullText.run(api, writer, logger, info);
                    break;
                case STAGE_IMAGES:
                    info = FindImages.run(api, writer, logger, info);
                    break;
                case STAGE_PDF:
                    info = DownloadPdfPatents.run(api, writer, logger, info);
                    break;
                case STAGE_SEARCH:
                    info = FindPatentIds.run(api, writer, logger, countryCode, year, info);
                    break;
                case STAGE_STATS:
                    info = ReportPatentStats.run(countryCode, year, info);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown stage " + stage);
                }
            } catch (Exception e) {
                // log the stage that failed
                logger.logErr(e, stage + " stage failed");
                throw(e);
            } finally {
                // update the master copy after each stage, even in case of errors
                writer.writeInfo(info);
            }
        }
        return info;
    }

    private static List<String> stagesNeeded(String stage) {
        return addStage(stage, new ArrayList<>());
    }

    private static List<String> addStage(String stage, List<String> stages) {
        if (! stages.contains(stage)) {
            switch(stage) {
            case STAGE_SEARCH:
            case STAGE_STATS:
                stages.add(stage);
                break;
            case STAGE_BIBLIO:
            case STAGE_FULLTEXT:
            case STAGE_IMAGES:
                addStage(STAGE_SEARCH, stages);
                stages.add(stage);
                break;
            case STAGE_CLAIMS:
            case STAGE_DESCRIPTION:
                addStage(STAGE_BIBLIO, stages);
                addStage(STAGE_FULLTEXT, stages);
                stages.add(stage);
                break;
            case STAGE_PDF:
                addStage(STAGE_BIBLIO, stages);
                addStage(STAGE_FULLTEXT, stages);
                addStage(STAGE_IMAGES, stages);
                stages.add(stage);
                break;
            case STAGE_ALL:
                addStage(STAGE_PDF, stages);
                addStage(STAGE_CLAIMS, stages);
                addStage(STAGE_DESCRIPTION, stages);
                break;
            default:
                throw new IllegalArgumentException("Unknown stage " + stage);
            }
        }
        return stages;
    }
}
