package patentdata.opstools;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

/**
 * Helper class for parsing the OPS API results.
 *
 * Author: Elaine Farrow
 */
public class OpsXmlHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Parse the XML string and return the document element.
     */
    public static Element parseResults(String xmlString) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            return doc.getDocumentElement();
        } catch (Exception ex) {
            // capture the XML that failed to parse
            LOGGER.error(xmlString);
            throw ex;
        }
    }

    /**
     * Return the patent number in the given format, or <code>null</code>.
     */
    public static String getDocNumber(Element reference, String format) {
        if (reference != null) {
            NodeList nodes = reference.getElementsByTagName("document-id");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element docId = (Element) nodes.item(i);
                String docType = docId.getAttribute("document-id-type");
                if (format.equals(docType)) {
                    if (OpsApiHelper.INPUT_FORMAT_EPODOC.equals(docType)) {
                        return getFirstChildText(docId, "doc-number");
                    } else if (OpsApiHelper.INPUT_FORMAT_DOCDB.equals(docType)) {
                        return String.join(".",
                                           getFirstChildText(docId, "country"),
                                           getFirstChildText(docId, "doc-number"),
                                           getFirstChildText(docId, "kind"));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the date from the first document using the given format,
     * or <code>null</code>.
     */
    public static String getDocDate(Element reference, String format) {
        if (reference != null) {
            NodeList nodes = reference.getElementsByTagName("document-id");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element docId = (Element) nodes.item(i);
                String docType = docId.getAttribute("document-id-type");
                if (format.equals(docType)) {
                    return getFirstChildText(docId, "date");
                }
            }
        }
        return null;
    }

    /**
     * Return the first child element with the given name, or
     * <code>null</code>.
     */
    public static Element getFirstChildElementByName(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }

    /**
     * Return the text content of the first child element with the
     * given name, or <code>null</code>.
     */
    public static String getFirstChildText(Element parent, String tagName) {
        Element el = getFirstChildElementByName(parent, tagName);
        return el == null ? null : el.getTextContent();
    }
}
