package reconstructreader.reconstruct;

import ij.IJ;
import ij.gui.MessageDialog;
import ini.trakem2.Project;
import reconstructreader.Utils;

import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

public class ReconstructReader implements PlugIn
{
    private int oid;
    private String projectName;
    private Document serDoc;
    private final ArrayList<Document> sections;
    private int firstLayerOID;
    private boolean lastStatus;

    public ReconstructReader()
    {
        sections = new ArrayList<Document>();
    }

    public void run(final String arg) {
        String fname;
        String trakEMFile;
        StringBuilder sb = new StringBuilder();

	lastStatus = false;
        
        if (arg.equals(""))
        {
        	//fname = "/home/larry/project-files/Volumejosef.ser";
            OpenDialog od = new OpenDialog("Select Reconstruct ser File", "");
            fname = od.getDirectory() + od.getFileName();
        }
        else
        {
            fname = arg;
        }

        convertReconstruct(fname, sb);

        trakEMFile = fname.toLowerCase().endsWith(".ser") ?
                fname.substring(0, fname.length() - 4) + ".xml" :
                fname + ".xml";

        if (lastStatus)
        {
            try
            {
                FileWriter fw = new FileWriter(
                        new File(trakEMFile));
                fw.write(sb.toString());
                fw.close();
                Project.openFSProject(trakEMFile);
            }
            catch (IOException ioe)
            {
                System.err.println("Well, that didn't work.  Tried to write to file " + trakEMFile);
            }
        }
        else
        {
            new MessageDialog(IJ.getInstance(), "Error", "Encountered an Error while translating");
        }

    }

    public StringBuilder convertReconstruct(final String fname, final StringBuilder xmlBuilder) {
        final File file = new File(fname);
        final File seriesDTD = new File(file.getParent() + "/series.dtd");
        final File sectionDTD = new File(file.getParent() + "/section.dtd");
        final String localFile = file.getName();
        File[] list;

        sections.clear();
        oid = -1;
        firstLayerOID = -1;

        projectName = (localFile.endsWith(".ser") || localFile.endsWith(".SER")) ?
                localFile.substring(0, localFile.length() - 4) : localFile;

        try
        {
            if (!seriesDTD.exists())
            {
                FileWriter fw = new FileWriter(seriesDTD);
                fw.write("");
                fw.close();
                seriesDTD.deleteOnExit();
            }

            if (!sectionDTD.exists())
            {
                FileWriter fw = new FileWriter(sectionDTD);
                fw.write("");
                fw.close();
                sectionDTD.deleteOnExit();
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            xmlBuilder.append(ioe.getStackTrace());
            return xmlBuilder;
        }

        list = file.getParentFile().listFiles(
                        new FilenameFilter()
                        {
                            public boolean accept(File dir, String name)
                            {
                                return Pattern.matches(projectName + ".*[0-9]$", name);
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

            Collections.sort(sections, new Utils.ReconstructSectionIndexComparator());

            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
            appendDTD(xmlBuilder);
            xmlBuilder.append("<trakem2>\n");
            appendProject(xmlBuilder);
            appendLayerSet(xmlBuilder);
            appendDisplay(xmlBuilder);
            xmlBuilder.append("</trakem2>\n");
            lastStatus = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return xmlBuilder;
    }

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
        //TODO: something smarter than this.
        sb.append("<!DOCTYPE trakem2_anything [\n" +
                "\t<!ELEMENT trakem2 (project,t2_layer_set,t2_display)>\n" +
                "\t<!ELEMENT project (anything)>\n" +
                "\t<!ATTLIST project id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project unuid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project preprocessor NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project mipmaps_folder NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST project storage_folder NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT anything EMPTY>\n" +
                "\t<!ATTLIST anything id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST anything expanded NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_layer (t2_patch,t2_label,t2_layer_set,t2_profile)>\n" +
                "\t<!ATTLIST t2_layer oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer thickness NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer z NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_layer_set (t2_prop,t2_linked_prop,t2_annot,t2_layer,t2_pipe,t2_ball,t2_area_list,t2_calibration,t2_stack,t2_treeline)>\n" +
                "\t<!ATTLIST t2_layer_set oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set layer_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set layer_height NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set rot_x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set rot_y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set rot_z NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set snapshots_quality NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set color_cues NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set n_layers_color_cue NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set paint_arrows NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_layer_set paint_edge_confidence_boxes NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_calibration EMPTY>\n" +
                "\t<!ATTLIST t2_calibration pixelWidth NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration pixelHeight NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration pixelDepth NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration xOrigin NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration yOrigin NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration zOrigin NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration info NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration valueUnit NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration timeUnit NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_calibration unit NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_ball (t2_prop,t2_linked_prop,t2_annot,t2_ball_ob)>\n" +
                "\t<!ATTLIST t2_ball oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball fill NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_ball_ob EMPTY>\n" +
                "\t<!ATTLIST t2_ball_ob x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball_ob y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball_ob r NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_ball_ob layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_label (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_label oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_label composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_patch (t2_prop,t2_linked_prop,t2_annot,ict_transform,ict_transform_list)>\n" +
                "\t<!ATTLIST t2_patch oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch file_path NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch original_path NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch type NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch ct NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch o_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch o_height NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_patch pps NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_pipe (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_pipe oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe d NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe p_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_pipe layer_ids NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_polyline (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_polyline oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_polyline d NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_profile (t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_profile oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_profile d NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_area_list (t2_prop,t2_linked_prop,t2_annot,t2_area)>\n" +
                "\t<!ATTLIST t2_area_list oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_area_list fill_paint NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_area (t2_path)>\n" +
                "\t<!ATTLIST t2_area layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_path EMPTY>\n" +
                "\t<!ATTLIST t2_path d NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_dissector (t2_prop,t2_linked_prop,t2_annot,t2_dd_item)>\n" +
                "\t<!ATTLIST t2_dissector oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dissector composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_dd_item EMPTY>\n" +
                "\t<!ATTLIST t2_dd_item radius NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dd_item tag NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_dd_item points NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_stack (t2_prop,t2_linked_prop,t2_annot,(iict_transform|iict_transform_list)?)>\n" +
                "\t<!ATTLIST t2_stack oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack composite NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack file_path CDATA #REQUIRED>\n" +
                "\t<!ATTLIST t2_stack depth CDATA #REQUIRED>\n" +
                "\t<!ELEMENT t2_tag EMPTY>\n" +
                "\t<!ATTLIST t2_tag name NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_tag key NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_node (t2_area*,t2_tag*)>\n" +
                "\t<!ATTLIST t2_node x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node lid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node c NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_node r NMTOKEN #IMPLIED>\n" +
                "\t<!ELEMENT t2_treeline (t2_node*,t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_treeline oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_treeline composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_areatree (t2_node*,t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_areatree oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_areatree composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_connector (t2_node*,t2_prop,t2_linked_prop,t2_annot)>\n" +
                "\t<!ATTLIST t2_connector oid NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector transform NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector style NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector locked NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector visible NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector title NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector links NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_connector composite NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_prop EMPTY>\n" +
                "\t<!ATTLIST t2_prop key NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_prop value NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_linked_prop EMPTY>\n" +
                "\t<!ATTLIST t2_linked_prop target_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_linked_prop key NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_linked_prop value NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT t2_annot EMPTY>\n" +
                "\t<!ELEMENT t2_display EMPTY>\n" +
                "\t<!ATTLIST t2_display id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display layer_id NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display magnification NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_x NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_y NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_width NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display srcrect_height NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display scroll_step NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display c_alphas NMTOKEN #REQUIRED>\n" +
                "\t<!ATTLIST t2_display c_alphas_state NMTOKEN #REQUIRED>\n" +
                "\t<!ELEMENT ict_transform EMPTY>\n" +
                "\t<!ATTLIST ict_transform class CDATA #REQUIRED>\n" +
                "\t<!ATTLIST ict_transform data CDATA #REQUIRED>\n" +
                "\t<!ELEMENT iict_transform EMPTY>\n" +
                "\t<!ATTLIST iict_transform class CDATA #REQUIRED>\n" +
                "\t<!ATTLIST iict_transform data CDATA #REQUIRED>\n" +
                "\t<!ELEMENT ict_transform_list (ict_transform|iict_transform)*>\n" +
                "\t<!ELEMENT iict_transform_list (iict_transform*)>\n" +
                "] >\n\n");
    }

    protected void appendProject(final StringBuilder sb)
    {
        sb.append("<project\n");
        sb.append("id=\"0\"\n");
        sb.append("title=\"").append(projectName).append("\"\n");
        sb.append("unuid=\"").append(getUNUID()).append("\"\n");
        sb.append("mipmaps_folder=\"").append(getMipMapFolder(getUNUID())).append("\"\n");
        sb.append("storage_folder=\"\"\n");
        sb.append("mipmaps_format=\"0\"\n");
        sb.append(">\n");
        sb.append("</project>\n");
    }

    protected void appendLayerSet(final StringBuilder sb)
    {
        Node image = sections.get(0).getElementsByTagName("Image").item(0);
        double[] layerwh = Utils.getReconstructImageWH(image);

        sb.append("<t2_layer_set\n");
        sb.append("oid=\"").append(nextOID()).append("\"\n");
        sb.append("width=\"20.0\"\n");
        sb.append("height=\"20.0\"\n");
        sb.append("transform=\"matrix(1.0,0.0,0.0,1.0,0.0,0.0)\"\n");
        sb.append("title=\"Top Level\"\n");
        sb.append("links=\"\"\n");
        sb.append("layer_width=\"").append(layerwh[0]).append("\"\n");
        sb.append("layer_height=\"").append(layerwh[1]).append("\"\n");
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

        appendCalibration(sb);

        for (Document doc : sections)
        {
            appendLayer(sb, doc);
        }

        sb.append("</t2_layer_set>\n");

    }

    protected void appendLayer(final StringBuilder sb, final Document doc)
    {
        String thickness = doc.getDocumentElement().getAttribute("thickness");
        int index = Integer.valueOf(doc.getDocumentElement().getAttribute("index"));
        int oid = nextOID();
        float thicknessFloat = Float.valueOf(thickness);
        NodeList imageList = doc.getElementsByTagName("Image");

        if (firstLayerOID < 0)
        {
            firstLayerOID = oid;
        }

        sb.append("<t2_layer oid=\"")
                .append(oid).append("\"\n" +
                "thickness=\"").append(thickness).append("\"\n" +
                "z=\"").append(thicknessFloat * (float)index).append("\"\n" +
                "title=\"\"\n" +
                ">\n");

        for (int i = 0; i < imageList.getLength(); ++i)
        {
            appendPatch(sb, (Element)imageList.item(0));
        }

        sb.append("</t2_layer>\n");
    }

    protected void appendPatch(final StringBuilder sb,final Element image)
    {
        Element rTransform = (Element)image.getParentNode();
        AffineTransform trans = Utils.reconstructTransform(rTransform);
        String src = image.getAttribute("src");
        String transString = Utils.transformToString(trans);
        double[] wh = Utils.getReconstructImageWH(image);
        double[] twh = Utils.getReconstructBoundingBox(wh, trans);
        double minX = twh[0], maxX = twh[0], minY = twh[1], maxY = twh[1];
        double boundHeight = twh[1];
        for (int i = 1; i < 4; ++i)
        {
            double x = twh[2 * i];
            double y = twh[2 * i + 1];
            if (minX > x)
                minX = x;
            if (maxX < x)
                maxX = x;
            if (minY > y)
                minY = y;
            if (maxY < y)
                maxY = y;
        }

        sb.append("<t2_patch\n" +
                "oid=\"").append(nextOID()).append("\"\n" +
                "width=\"").append(maxX - minX).append("\"\n" +
                "height=\"").append(maxY - minY).append("\"\n" +
                "transform=\"").append(transString).append("\"\n" +
                "title=\"").append(src).append("\"\n" +
                "links=\"\"\n" +
                "type=\"0\"\n" +
                "file_path=\"").append(src).append("\"\n" +
                "style=\"fill-opacity:1.0;stroke:#ffff00;\"\n" +
                "o_width=\"").append((int)wh[0]).append("\"\n" +
                "o_height=\"").append((int)wh[1]).append("\"\n" +
                "min=\"0.0\"\n" +
                "max=\"255.0\"\n" +
                ">\n" +
                "</t2_patch>\n");
    }

    protected void appendCalibration(final StringBuilder sb)
    {
        Element image = (Element)sections.get(0).getElementsByTagName("Image").item(0);
        String mag = image.getAttribute("mag");
        String thickness = sections.get(0).getDocumentElement().getAttribute("thickness");

        sb.append("<t2_calibration\n" +
                "pixelWidth=\"").append(mag).append("\"\n" +
                "pixelHeight=\"").append(mag).append("\"\n" +
                "pixelDepth=\"").append(thickness).append("\"\n" +
                "xOrigin=\"0.0\"\n" +
                "yOrigin=\"0.0\"\n" +
                "tzOrigin=\"0.0\"\n" +
                "info=\"null\"\n" +
                "valueUnit=\"Gray Value\"\n" +
                "timeUnit=\"sec\"\n" +
                "unit=\"pixel\"\n" +
                "/>\n");
    }

    protected void appendDisplay(final StringBuilder sb)
    {
        sb.append("<t2_display id=\"7\"\n" +
                "layer_id=\"").append(firstLayerOID).append("\"\n" +
                "c_alphas=\"-1\"\n" +
                "c_alphas_state=\"-1\"\n" +
                "x=\"1276\"\n" +
                "y=\"-3\"\n" +
                "magnification=\"0.39615630662788986\"\n" +
                "srcrect_x=\"0\"\n" +
                "srcrect_y=\"0\"\n" +
                "srcrect_width=\"4096\"\n" +
                "srcrect_height=\"2974\"\n" +
                "scroll_step=\"1\"\n" +
                "/>\n");
    }

    protected int nextOID()
    {
        return ++oid;
    }

}