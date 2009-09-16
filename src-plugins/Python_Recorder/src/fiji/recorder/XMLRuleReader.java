package fiji.recorder;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class XMLRuleReader extends DefaultHandler {

	/*
	 * FIELDS
	 */
	
	// Currently parsed
	private String body;
	private CommandTranslatorRule rule;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public XMLRuleReader(String path) throws ParserConfigurationException,
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
		rule = new CommandTranslatorRule();
		body = "";
	}

	public void endDocument () { }

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;
		
		if (tagName.equalsIgnoreCase("CommandTranslatorRule")) {
			rule.setPriority( Integer.parseInt(atts.getValue("priority")) );
			rule.setName( atts.getValue( "name"));
		}
		else if (tagName.equalsIgnoreCase("Matcher")) {
			rule.setCommand( atts.getValue("command") );
			rule.setClassName( atts.getValue("class_name") );
			rule.setArguments( atts.getValue("arguments"));
			rule.setModifiers( Integer.parseInt(atts.getValue( "modifiers")) );
		} 
		else if (tagName.equalsIgnoreCase("PythonTranslator")) {
			rule.setPythonTranslator( atts.getValue("target"));
		}

	}

	public void endElement(String uri, String name, String qName) { 
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;
		
		if (tagName.equalsIgnoreCase("CommandTranslatorRule")) {
			rule.setDescription(body);
		}
	}

	public void characters(char ch[], int start, int length) {
		body += new String(ch, start, length);
	}

	public CommandTranslatorRule getRule() {
		return rule;
	}

}
