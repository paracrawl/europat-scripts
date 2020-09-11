package patentdata.opstools;

import java.io.File;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
 * However, if a sample is requested, this will include PDFs where ALL
 * the parts are available as text.
 *
 * Author: Elaine Farrow
 */
public class PDFPatentPipeline {

    public static final int DEFAULT_SAMPLE_SIZE = 20;

    public static final String STAGE_ALL = "all";
    public static final String STAGE_BIBLIO = "biblio";
    public static final String STAGE_CLAIMS = "claims";
    public static final String STAGE_DESCRIPTION = "description";
    public static final String STAGE_FULLTEXT = "fulltext";
    public static final String STAGE_IMAGES = "images";
    public static final String STAGE_PDF = "pdf";
    public static final String STAGE_PREPDF = "prepdf";
    public static final String STAGE_REPORT = "report";
    public static final String STAGE_SAMPLE = "sample";
    public static final String STAGE_SEARCH = "search";

    private static final Logger LOGGER = LogManager.getLogger();

    // configure the application
    private final OpsConfigHelper config;

    // use the same helper to manage all the API calls
    private final OpsApiHelper api;

    // zero indicates we are not using sampling
    private final int sampleSize;

    public PDFPatentPipeline(String configFilePath, int sampleSize) throws Exception {
        config = new OpsConfigHelper(configFilePath);
        api = new OpsApiHelper(config);
        this.sampleSize = sampleSize;
    }

    public static void main(String[] args) throws Exception {
        List<String> params = new ArrayList<>(Arrays.asList(args));
        int index = Math.max(params.indexOf("-h"), params.indexOf("--help"));
        if (index >= 0 || params.isEmpty()) {
            printHelp();
            return;
        }
        String configFilePath = null;
        index = Math.max(params.indexOf("-c"), params.indexOf("-C"));
        if (index >= 0) {
            configFilePath = params.remove(index+1);
            params.remove(index);
        }
        if (configFilePath != null) {
            LOGGER.warn(String.format("Using config file: %s", configFilePath));
        }
        int sampleSize = DEFAULT_SAMPLE_SIZE;
        index = Math.max(params.indexOf("-s"), params.indexOf("--sample"));
        if (index >= 0) {
            sampleSize = Integer.parseInt(params.remove(index+1));
            params.remove(index);
            LOGGER.warn(String.format("Using a sample size of %d", sampleSize));
        }
        index = Math.max(params.indexOf("-t"), params.indexOf("--test"));
        if (index >= 0 || params.isEmpty()) {
            new PDFPatentPipeline(configFilePath, sampleSize).runTests();
            return;
        }
        String inputFilePath = null;
        index = Math.max(params.indexOf("-f"), params.indexOf("-F"));
        if (index >= 0) {
            inputFilePath = params.remove(index+1);
            params.remove(index);
        }
        if (inputFilePath != null) {
            LOGGER.warn(String.format("Reading input from file: %s", inputFilePath));
            warnExtraParams(params);
        }
        PDFPatentPipeline p = new PDFPatentPipeline(configFilePath, sampleSize);
        try {
            if (inputFilePath == null) {
                runPipeline(p, params);
            } else {
                runCommandsFromFile(p, inputFilePath);
            }
        } finally {
            LogManager.shutdown();
        }
        if (p.weeklyQuotaExceeded()) {
            // report this condition to the caller
            System.exit(2);
        }
    }

    // -------------------------------------------------------------------------------

    private static void runPipeline(PDFPatentPipeline p, List<String> params) throws Exception {
        int nArgs = params.size();
        if (nArgs < 2) {
            LOGGER.error(String.format("ERROR: Too few parameters: %s", String.join(" ", params)));
            printUsage();
        } else {
            if (nArgs > 3) {
                warnExtraParams(params.subList(3, nArgs));
            }
            String countryCode = params.get(0);
            Integer year = Integer.valueOf(params.get(1));
            String stage = nArgs > 2 ? params.get(2) : STAGE_REPORT;
            p.runPipeline(countryCode, year, stage);
        }
    }

    private static void runCommandsFromFile(PDFPatentPipeline p, String path) throws Exception {
        for (String line : FileUtils.readLines(new File(path), StandardCharsets.UTF_8)) {
            if (! line.isEmpty()) {
                runPipeline(p, Arrays.asList(line.split("\\s")));
                if (p.weeklyQuotaExceeded()) {
                    break;
                }
            }
        }
    }

    private static void warnExtraParams(List<String> params) {
        if (! params.isEmpty()) {
            LOGGER.warn(String.format("WARNING: Ignoring extra parameters: %s", String.join(" ", params)));
        }
    }

    private static void printHelp() {
        StringBuilder buf = new StringBuilder();
        buf.append("------------------------").append("\n");
        buf.append("PDFPatentPipeline").append("\n");
        buf.append("------------------------").append("\n");
        buf.append("This is a tool for running a pipeline of steps for downloading patents from the OPS API. ");
        buf.append("It manages the calls to the OPS API, obeying the throttling limits.").append("\n");
        buf.append("\n");
        buf.append("PDF patents will NOT be downloaded if the title, abstract, claims, and description are all available as text. ");
        buf.append("This tool does not currently check which languages are available as text before making this decision.").append("\n");
        System.err.print(buf);
        printUsage();
    }

    private static void printUsage() {
        StringBuilder buf = new StringBuilder();
        buf.append("------------------------").append("\n");
        buf.append("Parameters").append("\n");
        buf.append("------------------------").append("\n");
        buf.append("-c <ConfigPath> (Optional): Config path (JSON only)").append("\n");
        buf.append("-f <InputPath>  (Optional): Path to file with inputs").append("\n");
        buf.append("  OR a single input with 2 or 3 parts:").append("\n");
        buf.append("<CountryCode>   (Required): 2 letter country code").append("\n");
        buf.append("<Year>          (Required): 4 digit year").append("\n");
        buf.append("<Stage>         (Optional): target stage").append("\n");
        buf.append("\n");
        buf.append("Stage is one of the following:").append("\n");
        buf.append("  report        : report patent statistics (default)").append("\n");
        buf.append("  all           : download PDFs, claims, and descriptions").append("\n");
        buf.append("  pdf           : download PDFs").append("\n");
        buf.append("  claims        : download claims").append("\n");
        buf.append("  description   : download descriptions").append("\n");
        buf.append("  biblio        : download titles and abstracts").append("\n");
        buf.append("  search        : identify patents").append("\n");
        buf.append("  fulltext      : identify patents with full text").append("\n");
        buf.append("  images        : identify patents with PDFs available").append("\n");
        buf.append("  prepdf        : identify PDFs to download").append("\n");
        buf.append("  sample        : download sample PDFs").append("\n");
        buf.append("------------------------").append("\n");
        buf.append("Examples").append("\n");
        buf.append("------------------------").append("\n");
        buf.append("java -jar pdfpatents.jar -c \"config/path/patent.json\" NO 1994").append("\n");
        buf.append("java -jar pdfpatents.jar -f \"input/path/inputs.txt\"").append("\n");
        buf.append("java -jar pdfpatents.jar NO 1994 pdf").append("\n");
        buf.append("------------------------").append("\n");
        System.err.print(buf);
    }

    // -------------------------------------------------------------------------------

    /**
     * Reports whether the weekly quota for API calls has been
     * exceeded.
     */
    public boolean weeklyQuotaExceeded() {
        return api.weeklyQuotaExceeded();
    }

    /**
     * Runs the needed pipeline of API calls to complete the given
     * stage for the given country code and year.
     */
    public void runPipeline(String countryCode, Integer year, String stage) throws Exception {
        try {
            List<String> stages = stagesNeeded(stage);
            if (! stages.isEmpty()) {
                LOGGER.info(String.format("Processing %s %d through stages: ", countryCode, year) + stages);
                List<PatentInfo> results = runPipelineStages(countryCode, year, stages);
                LOGGER.info(String.valueOf(results.subList(0, Math.min(3, results.size()))));
                String operation = STAGE_SEARCH.equals(stage) ? "Found" : "Processed";
                LOGGER.info(String.format("%s %d records for %s %d", operation, results.size(), countryCode, year));
            }
        } finally {
            // always report the stats at the end, even in case of errors
            runPipelineStages(countryCode, year, Arrays.asList(STAGE_REPORT));
        }
    }

    private List<PatentInfo> runPipelineStages(String countryCode, Integer year, List<String> stages) throws Exception {
        PatentResultWriter writer = new PatentResultWriter(config.getWorkingDirName(), countryCode, year);
        // initialise values from the master copy
        List<PatentInfo> info = writer.readInfo();
        if (stages.contains(STAGE_SAMPLE)) {
            writer = writer.getSampleWriter();
        }
        for (String stage : stages) {
            if (! STAGE_REPORT.equals(stage)) {
                LOGGER.info("** Starting " + stage + " stage **");
            }
            writer.setCheckpointDir("ops_" + stage);
            try {
                boolean success = true;
                switch(stage) {
                case STAGE_BIBLIO:
                    success = RetrieveBiblio.run(api, writer, info);
                    break;
                case STAGE_CLAIMS:
                    success = RetrieveClaims.run(api, writer, info);
                    break;
                case STAGE_DESCRIPTION:
                    success = RetrieveDescriptions.run(api, writer, info);
                    break;
                case STAGE_FULLTEXT:
                    success = FindFullText.run(api, writer, info);
                    break;
                case STAGE_IMAGES:
                    success = FindImages.run(api, writer, info);
                    break;
                case STAGE_PDF:
                    success = DownloadPdfPatents.run(api, writer, info);
                    break;
                case STAGE_SAMPLE:
                    success = DownloadPdfPatents.downloadSample(api, writer, info, sampleSize);
                    break;
                case STAGE_SEARCH:
                    success = FindPatentIds.run(api, writer, countryCode, year, info);
                    break;
                case STAGE_REPORT:
                    success = ReportPatentStats.run(writer, countryCode, year, info);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown stage " + stage);
                }
                if (! success) {
                    LOGGER.error(stage + " stage failed");
                    break;
                }
            } catch (Exception e) {
                LOGGER.error(stage + " stage failed", e);
                throw(e);
            } finally {
                switch(stage) {
                case STAGE_REPORT:
                case STAGE_SAMPLE:
                    // metadata unchanged, no need to update
                    break;
                default:
                    // update the master copy after each stage, even in case of errors
                    writer.writeInfo(info);
                    break;
                }
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
            case STAGE_REPORT:
                // don't add anything
                break;
            case STAGE_SAMPLE:
            case STAGE_SEARCH:
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
            case STAGE_PREPDF:
                addStage(STAGE_BIBLIO, stages);
                addStage(STAGE_FULLTEXT, stages);
                addStage(STAGE_IMAGES, stages);
                // prepdf is not a real stage - don't add
                break;
            case STAGE_PDF:
                addStage(STAGE_PREPDF, stages);
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

    // -------------------------------------------------------------------------------

    private void runTests() {
        api.runTests();
    }
}
