package fiji.recorder.template;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fiji.recorder.util.XMLFileErrorHandler;

public class XMLTemplateReader extends DefaultHandler  {

	/*
	 * FIELDS
	 */

	// Currently parsed
	private String main_body;
	private String element_body;
	private Template template;
	private String current_element;

	/*
	 * CONSTRUCTOR
	 */

	public XMLTemplateReader(String path) throws ParserConfigurationException,
	IOException, SAXException {
		initialize(new InputSource(path));
	}

	/*
	 * METHODS
	 */

	private void initialize(InputSource inputSource)
	throws ParserConfigurationException, SAXException,
	IOException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		SAXParser parser = factory.newSAXParser();
		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(inputSource);
	}

	public void startDocument () {
		template = new Template();
		main_body = "";
	}

	public void endDocument () { }

	public void startElement(String uri, String name, String qName,	Attributes atts) {

		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;

		element_body = "";
		current_element = tagName;

		if (tagName.equalsIgnoreCase("CommandTemplate")) {
			template.setName( atts.getValue( "name"));
		}

	}

	public void endElement(String uri, String name, String qName) { 
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;

		if (tagName.equalsIgnoreCase("CommandTemplate")) {
			template.setDescription(main_body);
		} else if (tagName.equalsIgnoreCase("Python")) {
			template.setPythonString(element_body);
		}
	}

	public void characters(char ch[], int start, int length) {
		if (current_element.equalsIgnoreCase("CommandTemplate")) {
			main_body += new String(ch, start, length); 
		} else {
			element_body += new String(ch, start, length);
		}

	}

	public Template getTemplate() {
		return template;
	}

}


