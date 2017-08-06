/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.serviceprovider.parsers;

import static com.lyncode.xml.matchers.QNameMatchers.localPart;
import static com.lyncode.xml.matchers.XmlEventMatchers.aStartElement;
import static com.lyncode.xml.matchers.XmlEventMatchers.anEndElement;
import static com.lyncode.xml.matchers.XmlEventMatchers.elementName;
import static com.lyncode.xml.matchers.XmlEventMatchers.text;
import static com.lyncode.xml.matchers.XmlEventMatchers.theEndOfDocument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;
import org.dspace.xoai.model.xoai.Element;
import org.dspace.xoai.model.xoai.Field;
import org.dspace.xoai.model.xoai.XOAIMetadata;
import org.hamcrest.Matcher;

import com.lyncode.xml.XmlReader;
import com.lyncode.xml.exceptions.XmlReaderException;

public class MetadataParser {
    public XOAIMetadata parse(InputStream input) throws XmlReaderException {
        XOAIMetadata metadata = new XOAIMetadata();
        XmlReader reader = new XmlReader(input);
        reader.next(elementName(localPart(equalTo("metadata"))));

        List<Element> elementsWithFields = new ArrayList<Element>();
        List<String> elementName = new ArrayList<String>();
        while (moreToGo(reader))
        {
            //System.err.println("loop, now on " + reader.getName());
            if (reader.current(startElement()))
            {
                elementName.add(reader.getAttributeValue(name()));
                //System.err.println(">> ."+reader.getAttributeValue(name()));
                reader.next(startElement(), endElement(), startField(), endMetadata());
            }
            if (reader.current(startField())) {
                Element element = new Element(StringUtils.join(elementName, "."));
                while (reader.current(startField())) {
                    Field field = new Field()
                            .withName(reader.getAttributeValue(name()));

                    if (reader.next(anEndElement(), text()).current(text()))
                        field.withValue(reader.getText());

                    element.withField(field);
                    //System.err.println("just added field " + field.getName() + " in " + element.getName());
                    reader.next(startField(), startElement(), endElement());
                }
                elementsWithFields.add(element);
                //System.err.println("found e=" + element.getName());
                //System.err.println("now at " + reader.getName() + " " + reader.current(startElement())  + " "  + reader.current(endElement()));
            }
            if (reader.current(endElement()) && elementName.size() > 0) {
                elementName.remove(elementName.size() -1);
            }
        }
        for(Element e : elementsWithFields)
        {
             metadata.withElement(e);
        }

        return metadata;
    }
    
    private boolean moreToGo(XmlReader reader) throws XmlReaderException 
    {
        if (reader.current(endMetadata()))
            return false;
        if (reader.current(startElement()))
            return true;
        return ! reader.next(startElement(), endElement(), startField(), endMetadata()).current(endMetadata());
    }

    private Matcher<XMLEvent> endOfMetadata() {
        return allOf(anEndElement(), elementName(localPart(equalTo("metadata"))));
    }

    private Matcher<QName> name() {
        return localPart(equalTo("name"));
    }

    private Matcher<XMLEvent> startElement() {
        return allOf(aStartElement(), elementName(localPart(equalTo("element"))));
    }
    private Matcher<XMLEvent> endElement() {
        return allOf(anEndElement(), elementName(localPart(equalTo("element"))));
    }
    private Matcher<XMLEvent> endMetadata() {
        return allOf(anEndElement(), elementName(localPart(equalTo("metadata"))));
    }
}
