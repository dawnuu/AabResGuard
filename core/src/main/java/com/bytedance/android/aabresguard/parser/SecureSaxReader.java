package com.bytedance.android.aabresguard.parser;

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

/**
 * Creates SAXReaders that reject DOCTYPE declarations and never resolve
 * external entities, which protects config parsing against XXE.
 */
final class SecureSaxReader {

    private SecureSaxReader() {
    }

    static SAXReader create() throws DocumentException {
        SAXReader reader = new SAXReader();
        try {
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (SAXException e) {
            throw new DocumentException("Unable to configure a secure XML parser: " + e.getMessage(), e);
        }
        return reader;
    }
}
