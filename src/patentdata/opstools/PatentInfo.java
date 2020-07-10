package patentdata.opstools;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for gathering information about patents from the OPS
 * API. Values are written to file after each stage in the pipeline to
 * track progress.
 *
 * Author: Elaine Farrow
 */
public class PatentInfo {

    public static final String DOCDB_SEPARATOR_PATTERN = "\\.";
    private static final String ID_KEY = "ID";
    private static final String FAMILY_KEY = "FAMILY";
    private static final String DATE_KEY = "DATE";
    private static final String TITLE_KEY = "TITLE";
    private static final String ABSTRACT_KEY = "ABSTRACT";
    private static final String CLAIMS_KEY = "CLAIMS";
    private static final String DESCRIPTION_KEY = "DESCRIPTION";
    private static final String IMAGES_KEY = "IMAGES";
    private static final String PAGES_KEY = "PAGES";

    private static final String[] KEYS = {
        ID_KEY,
        FAMILY_KEY,
        DATE_KEY,
        TITLE_KEY,
        ABSTRACT_KEY,
        CLAIMS_KEY,
        DESCRIPTION_KEY,
        IMAGES_KEY,
        PAGES_KEY};

    private final Map<String, String> info = new HashMap<>();

    public PatentInfo(String docdbId) {
        info.put(ID_KEY, docdbId);
        String[] parts = docdbId.split(DOCDB_SEPARATOR_PATTERN);
        if (parts.length > 3) {
            info.put(DATE_KEY, parts[3]);
        }
    }

    public static String getCountry(String docId) {
        return docId.split(DOCDB_SEPARATOR_PATTERN)[0];
    }

    public static String getNumber(String docId) {
        return docId.split(DOCDB_SEPARATOR_PATTERN)[1];
    }

    public static String getKind(String docId) {
        return docId.split(DOCDB_SEPARATOR_PATTERN)[2];
    }

    public static PatentInfo fromString(String valueString) {
        String[] values = valueString.split("\t");
        PatentInfo result = new PatentInfo(values[0]);
        for (int i = 1; i < values.length; i++) {
            String value = values[i];
            result.info.put(KEYS[i], "null".equals(value) ? null : value);
        }
        return result;
    }

    @Override
    public String toString() {
        List<String> values = new ArrayList<>();
        for (String key : KEYS) {
            values.add(info.get(key));
        }
        return String.join("\t", values);
    }

    public String getDocdbId() {
        return info.get(ID_KEY);
    }

    public String getCountry() {
        return getCountry(getDocdbId());
    }

    public String getNumber() {
        return getNumber(getDocdbId());
    }

    public String getKind() {
        return getKind(getDocdbId());
    }

    public String getFamily() {
        return info.get(FAMILY_KEY);
    }

    public void setFamily(String family) {
        info.put(FAMILY_KEY, family);
    }

    public String getDate() {
        return info.get(DATE_KEY);
    }

    public void setDate(String date) {
        info.put(DATE_KEY, date);
    }

    public boolean checkedTitle() {
        return info.get(TITLE_KEY) != null;
    }

    public boolean hasTitle() {
        return ! getTitles().isEmpty();
    }

    public List<String> getTitles() {
        return splitValues(info.get(TITLE_KEY));
    }

    public void setTitles(List<String> languages) {
        info.put(TITLE_KEY, combineValues(languages));
    }

    public boolean checkedAbstract() {
        return info.get(ABSTRACT_KEY) != null;
    }

    public boolean hasAbstract() {
        return ! getAbstracts().isEmpty();
    }

    public List<String> getAbstracts() {
        return splitValues(info.get(ABSTRACT_KEY));
    }

    public void setAbstracts(List<String> languages) {
        info.put(ABSTRACT_KEY, combineValues(languages));
    }

    public boolean checkedClaims() {
        return info.get(CLAIMS_KEY) != null;
    }

    public boolean hasClaims() {
        return ! getClaims().isEmpty();
    }

    public List<String> getClaims() {
        return splitValues(info.get(CLAIMS_KEY));
    }

    public void setClaims(List<String> languages) {
        info.put(CLAIMS_KEY, combineValues(languages));
    }

    public boolean checkedDescription() {
        return info.get(DESCRIPTION_KEY) != null;
    }

    public boolean hasDescription() {
        return ! getDescriptions().isEmpty();
    }

    public List<String> getDescriptions() {
        return splitValues(info.get(DESCRIPTION_KEY));
    }

    public void setDescriptions(List<String> languages) {
        info.put(DESCRIPTION_KEY, combineValues(languages));
    }

    public boolean checkedImages() {
        return info.get(PAGES_KEY) != null;
    }

    public boolean hasImages() {
        return getNPages() > 0;
    }

    public String getImages() {
        return info.get(IMAGES_KEY);
    }

    public void setImages(String images) {
        info.put(IMAGES_KEY, images);
    }

    public int getNPages() {
        String pages = info.get(PAGES_KEY);
        return pages == null ? 0 : Integer.parseUnsignedInt(pages);
    }

    public void setPages(int nPages) {
        info.put(PAGES_KEY, Integer.toString(nPages));
    }

    private static String combineValues(List<String> values) {
        return String.join("-", values);
    }

    private static List<String> splitValues(String values) {
        return values == null || values.length() == 0
            ? Collections.emptyList()
            : Arrays.asList(values.split("-"));
    }
}
