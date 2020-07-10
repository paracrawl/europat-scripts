package patentdata.opstools;

/**
 * Helper class for collecting full-text content from the OPS API.
 *
 * Author: Elaine Farrow
 */
public class PatentContent {

    public final PatentInfo info;
    public final String value;

    public PatentContent(PatentInfo info, String value) {
        this.info = info;
        this.value = value;
    }

    public PatentInfo getInfo() {
        return info;
    }

    public String getValue() {
        return value;
    }
}
