package reconstructreader;

import org.w3c.dom.*;

import java.awt.geom.AffineTransform;
import java.util.Comparator;
import java.util.StringTokenizer;

public final class Utils {

    public final static String DELIM = " \t\n\r\f";

    private Utils(){}




    public static class ReconstructSectionIndexComparator implements Comparator<Document>
    {
        public int compare(final Document o1, final Document o2) {
            Integer index1 = Integer.valueOf(o1.getDocumentElement().getAttribute("index"));
            Integer index2 = Integer.valueOf(o2.getDocumentElement().getAttribute("index"));
            return index1.compareTo(index2);
        }
    }

    public static AffineTransform reconstructTransform(final Element trans)
    {
        int dim = Integer.valueOf(trans.getAttribute("dim"));
        double[] matrix = new double[6];
        double[] xcoef = createNodeValueVector(trans.getAttribute("xcoef"));
        double[] ycoef = createNodeValueVector(trans.getAttribute("ycoef"));

        Utils.nodeValueToVector(trans.getAttribute("xcoef"), xcoef);
        Utils.nodeValueToVector(trans.getAttribute("ycoef"), ycoef);

        for (int i = 0; i < 6; ++i)
        {
            matrix[i] = 0.0f;
        }

        switch (dim)
        {
            case 1:
                matrix[0] = 1;
                matrix[3] = 1;
                matrix[4] = xcoef[0];
                matrix[5] = ycoef[0];
                break;
            case 2:
                matrix[0] = xcoef[1];
                matrix[3] = ycoef[1];
                matrix[4] = xcoef[0];
                matrix[5] = ycoef[0];
                break;
            case 3:
                matrix[0] = xcoef[1];
                matrix[1] = ycoef[1];
                matrix[2] = xcoef[2];
                matrix[3] = ycoef[2];
                matrix[4] = xcoef[0];
                matrix[5] = ycoef[0];
                break;
            default:
                int index = Integer.valueOf(
                        trans.getOwnerDocument().getDocumentElement().getAttribute("index"));
                boolean weird = false;
                matrix[0] = xcoef[1];
                matrix[1] = ycoef[1];
                matrix[2] = xcoef[2];
                matrix[3] = ycoef[2];
                matrix[4] = xcoef[0];
                matrix[5] = ycoef[0];
                for (int i = 3; i < 6; ++i)
                {
                    weird |= xcoef[i] != 0 || ycoef[i] != 0;
                }
                if (weird)
                {
                    System.err.println("Non affine tranforms are unsupported." +
                            " Expect weirdness at index " + index);
                }
                break;
        }

        return new AffineTransform(matrix);
    }

    public static String transformToString(AffineTransform trans)
    {
        double[] mat = new double[6];
        trans.getMatrix(mat);
        return transformToString(mat);
    }

    public static String transformToString(double[] matrix)
    {
        StringBuilder transSB = new StringBuilder();
        transSB.append("matrix(");
        for (int i = 0; i < matrix.length - 1; ++i)
        {
            transSB.append(matrix[i]);
            transSB.append(",");
        }
        transSB.append(matrix[matrix.length - 1]);
        transSB.append(")");
        return transSB.toString();
    }

    public static double[] getReconstructBoundingBox(double[] wh, AffineTransform trans) {
        double x = wh[0], y = wh[1];
        double[] xy = new double[]{0, 0, x, 0, x, y, 0, y};
        trans.transform(xy, 0, xy, 0, 4);
        return xy;
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

    public static double[] createNodeValueVector(final String val)
    {
        final StringTokenizer tokr = new StringTokenizer(val, DELIM + ",\"");
        return new double[tokr.countTokens()];
    }

    public static int nodeValueToVector(String val, double[] matrix)
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

    public static double[] getReconstructImageWH(Node image)
    {
        return getReconstructImageWH(image, null);
    }

    public static double[] getReconstructImageWH(Node image, double[] wh)
    {
        NodeList imageContourList =
                ((Element)image.getParentNode()).getElementsByTagName("Contour");
        Element imageDomainContour =
                Utils.findElementByAttributeRegex(imageContourList, "name", "^domain.*");
        String pointsString = imageDomainContour.getAttribute("points");
        double[] points = Utils.createNodeValueVector(pointsString);
        Utils.nodeValueToVector(pointsString, points);

        if (null == wh)
        {
            wh = new double[2];
        }

        wh[0] = points[2];
        wh[1] = points[5];

        return wh;
    }



}
