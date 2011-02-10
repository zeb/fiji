package reconstructreader.reconstruct;


import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ReconstructReader implements PlugIn
{
    private int oid;
    private String projectName;
    Document serDoc;
    final ArrayList<Document> sections;

    public ReconstructReader()
    {
        sections = new ArrayList<Document>();
    }

    public void run(final String arg) {
        String fname;
        if (arg.equals(""))
        {
            OpenDialog od = new OpenDialog("Select Reconstruct ser File", "");
            fname = od.getDirectory() + od.getFileName();
        }
        else
        {
            fname = arg;
        }

        convertReconstruct(fname, new StringBuilder());
    }

    public StringBuilder convertReconstruct(final String fname, final StringBuilder xmlBuilder) {
        final File file = new File(fname);
        File[] list;

        sections.clear();
        oid = -1;
        projectName = (fname.endsWith(".ser") || fname.endsWith(".SER")) ?
                fname.substring(0, fname.length() - 4) : fname;

        list = file.getParentFile().listFiles(
                        new FilenameFilter()
                        {
                            public boolean accept(File dir, String name)
                            {
                                return Pattern.matches(projectName + "*\\d$", name);
                            }
                        }
                );


        try {
            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            serDoc = builder.parse(file);

            for (File f : list)
            {
                sections.add(builder.parse(f));
            }

            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            appendDTD(xmlBuilder);
            appendProject(xmlBuilder);
            appendLayerSet(xmlBuilder);
            appendDisplay(xmlBuilder);

            NodeList nodes = serDoc.getElementsByTagName("topic");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);

                NodeList title = element.getElementsByTagName("title");
                Element line = (Element) title.item(0);

                //System.out.println("Title: " + getCharacterDataFromElement(line));

                NodeList url = element.getElementsByTagName("url");
                line = (Element) url.item(0);
                //System.out.println("Url: " + getCharacterDataFromElement(line));

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return xmlBuilder;
    }

    /*
    TODO: Replace these methods with methods that actually do things.
     */
    protected String getUNUID()
    {
        return "1280781963399.1877850213.102743204";
    }

    protected String getMipMapFolder(final String unuid)
    {
        return "trakem2." + unuid + "/trakem2.mipmaps";
    }

    protected void appendDTD(final StringBuilder sb)
    {
        //TODO: something
    }

    protected void appendProject(StringBuilder sb)
    {
        sb.append("<project\n");
        sb.append("id=\"0\"");
        sb.append("title=\"").append(projectName).append("\"\n");
		sb.append("unuid=\"").append(getUNUID()).append("\"\n");
		sb.append("mipmaps_folder=\"").append(getMipMapFolder(getUNUID())).append("\"\n");
		sb.append("storage_folder=\"\"\n");
		sb.append("mipmaps_format=\"0\"\n");
        sb.append("</project>\n");
    }

    protected void appendLayerSet(StringBuilder sb)
    {
        float layerw, layerh;



        sb.append("<t2_layer_set\n");
        sb.append("oid=\"").append(nextOID()).append("\"\n");
        sb.append("width=\"20.0\"\n");
        sb.append("height=\"20.0\"\n");
        sb.append("transform=\"matrix(1.0,0.0,0.0,1.0,0.0,0.0)\"\n");
        sb.append("title=\"Top Level\"\n");
        sb.append("links=\"\"");
        sb.append("layer_width=\"").append(layerw).append("\"\n");
		sb.append("layer_height=\"").append(layerh).append("\"\n");
        sb.append("rot_x=\"0.0\"\n");
        sb.append("rot_y=\"0.0\"\n");
        sb.append("rot_z=\"0.0\"\n");
        sb.append("snapshots_quality=\"true\"\n");
        sb.append("snapshots_mode=\"Full\"\n");
        sb.append("color_cues=\"true\"\n");
        sb.append("n_layers_color_cue=\"-1\"\n");
        sb.append("paint_arrows=\"true\"\n");
        sb.append("paint_edge_confidence_boxes=\"true\"\n");
        sb.append("prepaint=\"true\"\n");
        sb.append(">\n");
    }

    protected void appendDisplay(StringBuilder sb)
    {

    }

    protected int nextOID()
    {
        return ++oid;
    }
}