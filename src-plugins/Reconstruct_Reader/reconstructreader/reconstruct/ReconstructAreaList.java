package reconstructreader.reconstruct;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import reconstructreader.Utils;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


public class ReconstructAreaList implements ContourSet {

    private final String name;
    private final int oid;
    private final int recContourID, areaListID;
    private final ArrayList<Element> contourList;
    private final ArrayList<Integer> indexList;
    private final Translator translator;

    public ReconstructAreaList(final Element e, final Translator t)
    {
        translator = t;
        name = e.getAttribute("name");
        oid = translator.nextOID();
        recContourID = translator.nextOID();
        areaListID = translator.nextOID();
        contourList = new ArrayList<Element>();
        indexList = new ArrayList<Integer>();
        contourList.add(e);
        indexList.add(Utils.sectionIndex(e));
    }

    public boolean equals(final Object o)
    {
        if (o instanceof ReconstructAreaList)
        {
            ReconstructAreaList ral = (ReconstructAreaList)o;
            return name.equals(ral.name);
        }
        else if (o instanceof Element)
        {
            Element e = (Element)o;
            return name.equals(e.getAttribute("name"));
        }
        else
        {
            return false;
        }
    }

    public void addContour(final Element e)
    {
        contourList.add(e);
        indexList.add(Utils.sectionIndex(e));
    }

    public void appendProjectXML(final StringBuilder sb)
    {
        sb.append("<reconstruct_contour id=\"").append(recContourID).append("\" title=\"")
                .append(name).append("\" expanded=\"true\">\n" +
                "<area_list id=\"").append(areaListID).append("\" oid=\"").append(oid)
                .append("\"/>\n</reconstruct_contour>\n");
    }

    public void appendLayerSetXML(final StringBuilder sb, List<ReconstructSection> sectionList)
    {
        final ArrayList<Element> selectionList = new ArrayList<Element>();

        sb.append("<t2_area_list\n" +
                "oid=\"").append(oid).append("\"\n" +
                "width=\"").append(translator.getStackWidth()).append("\"\n" +
                "height=\"").append(translator.getStackHeight()).append("\"\n" +
                "transform=\"matrix(1.0,0.0,0.0,1.0,0,0)\"\n" +
                "title=\"area_list\"\n" +
                "links=\"\"\n" +
                "layer_set_id=\"0\"\n" +
                "fill_paint=\"true\"\n" +
                "style=\"stroke:none;fill-opacity:0.4;fill:#ffff00;\"\n" +
                ">\n");

        System.out.println("Appending area lists. Got " + sectionList.size() + " sections.");
        System.out.println("I have " + contourList.size() + " contours.");

        for (ReconstructSection sec : sectionList)
        {
            System.out.println("Adding contours for section " + sec.getIndex());
            int index = sec.getIndex();
            int layerOID = sec.getOID();
            Document doc = sec.getDocument();
            NodeList imageList = doc.getElementsByTagName("Image");
            double mag = Double.valueOf(((Element)imageList.item(0)).getAttribute("mag"));

            Utils.selectElementsByIndex(contourList, indexList, selectionList, index);

            System.out.println("Selection List has " + selectionList.size() + " Elements");

            sb.append("<t2_area layer_id=\"").append(layerOID).append("\">\n");

            for (Element contour : selectionList)
            {
                boolean domainContour = name.startsWith("domain");
                double zoom = domainContour ? 1.0 : 1.0 / mag;
                double useMag = domainContour ? mag : 1.0;
                AffineTransform trans = Utils.reconstructTransform(
                        (Element)contour.getParentNode(),
                        useMag, translator.getStackHeight(), zoom, domainContour);
                double[] pts = Utils.createNodeValueVector(contour.getAttribute("points"));
                int nrows = Utils.nodeValueToVector(contour.getAttribute("points"), pts);
                if (nrows != 2)
                {
                    System.err.println("Nrows should have been 2, instead it was " + nrows
                            + ", therefore, we're boned");
                    System.err.println("Points text: " + contour.getAttribute("points"));
                }

                trans.transform(pts, 0, pts, 0, pts.length / 2);

                if (!domainContour)
                {
                    for (int i = 1; i < pts.length; i+=2)
                    {
                        pts[i] = translator.getStackHeight() - pts[i];
                    }
                }

                /*for (int i = 0; i < pts.length; ++i)
                {
                    pts[i] /= mag;
                }*/

                sb.append("<t2_path d=\"");
                Utils.append2DPointXML(sb, pts);
                sb.append("\" />\n");
                sb.append("<!-- zoom = ").append(zoom).append(" -->\n");

            }

            sb.append("</t2_area>\n");

            selectionList.clear();
        }
        sb.append("</t2_area_list>\n");
    }
}
