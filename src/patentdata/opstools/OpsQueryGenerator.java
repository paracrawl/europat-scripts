package patentdata.opstools;

/**
 * Methods for iterating over a set of OPS API queries.
 *
 * Author: Elaine Farrow
 */
public interface OpsQueryGenerator {

    /**
     * Gets the name of the OPS service to call.
     */
    String getService();

    /**
     * Is there another OPS query to submit?
     */
    boolean hasNext();

    /**
     * Gets the next OPS query to submit.
     */
    String getNextQuery();
}
