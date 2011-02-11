package reconstructreader;

import org.w3c.dom.*;

import java.util.Comparator;
import java.util.StringTokenizer;

public final class Utils {

    public final static String DELIM = " \t\n\r\f";

    private Utils(){}

    public static class ReconstructSectionIndexComparator implements Comparator<Document>
    {
        public int compare(Document o1, Document o2) {
            Integer index1 = Integer.valueOf(o1.getDocumentElement().getAttribute("index"));
            Integer index2 = Integer.valueOf(o2.getDocumentElement().getAttribute("index"));
            return index1.compareTo(index2);
        }
    }


    public static Element findElementByAttributeRegex(NodeList list, String name, String regex)
    {
        for (int i = 0; i < list.getLength(); ++i)
        {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                final Element e = (Element) list.item(i);

                if (e.hasAttribute(name) &&
                        e.getAttribute(name).matches(regex))
                {
                    return e;
                }
            }
        }
        return null;
    }

    public static Element findElementByAttribute(NodeList list, String name, String value)
    {
        for (int i = 0; i < list.getLength(); ++i)
        {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                final Element e = (Element) list.item(i);
                if (e.hasAttribute(name) && e.getAttribute(name).equals(value))
                {
                    return e;
                }
            }
        }
        return null;
    }

    public static float[] createNodeValueVector(final String val)
    {
        final StringTokenizer tokr = new StringTokenizer(val, DELIM + ",\"");
        return new float[tokr.countTokens()];
    }

    public static int nodeValueToVector(String val, float[] matrix)
    {
        StringTokenizer t;
        int rCount = 0, i = 0;

        if (val.startsWith("\""))
        {
            val = val.substring(1);
        }
        if (val.endsWith("\""))
        {
            val = val.substring(0, val.length());
        }

        t = new StringTokenizer(val, DELIM);

        while (t.hasMoreElements() && !t.nextToken().contains(","))
        {
            ++rCount;
        }

        t = new StringTokenizer(val, DELIM + ",\"");

        while (t.hasMoreElements())
        {
            matrix[i++] = Float.valueOf(t.nextToken());
        }

        return rCount;
    }

    public static float[] getReconstructImageWH(Node image)
    {
        return getReconstructImageWH(image, null);
    }

    public static float[] getReconstructImageWH(Node image, float[] wh)
    {
        NodeList imageContourList =
                ((Element)image.getParentNode()).getElementsByTagName("Contour");
        Element imageDomainContour =
                Utils.findElementByAttributeRegex(imageContourList, "name", "^domain.*");
        String pointsString = imageDomainContour.getAttribute("points");
        float[] points = Utils.createNodeValueVector(pointsString);
        Utils.nodeValueToVector(pointsString, points);

        if (null == wh)
        {
            wh = new float[2];
        }

        wh[0] = points[2];
        wh[1] = points[5];

        return wh;
    }



}
