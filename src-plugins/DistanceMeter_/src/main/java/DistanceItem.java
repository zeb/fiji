
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Juanjo Vega
 */
public class DistanceItem {

    private final static int SIZE = 50;
    private Image thumbnail;
    private Roi roi;
    private double distance;
    private String unit;

    public DistanceItem(ImagePlus imp, Roi roi, Point pointA, Point pointB) {
        thumbnail = createThumbnail(imp);
        this.roi = roi;

        Calibration cal = imp.getCalibration();
        unit = cal.getUnit();
        distance = calculateDistance(pointA, pointB, cal.pixelWidth, cal.pixelHeight);
    }

    private double calculateDistance(Point a, Point b, double scaleX, double scaleY) {
        double Ax = (a.x - b.x) * scaleX;
        double Ay = (a.y - b.y) * scaleY;

        return Math.sqrt(Ax * Ax + Ay * Ay);
    }

    private static Image createThumbnail(ImagePlus imp) {
        // Scales image.
        int W = imp.getWidth(), H = imp.getHeight();
        int w = W >= H ? SIZE : -1;
        int h = W < H ? SIZE : -1;

        Image i = imp.getImage();
        if (W > SIZE || H > SIZE) {
            i = i.getScaledInstance(w, h, Image.SCALE_FAST);
        }

        // Draws it centered in a transparent image.
        BufferedImage bimage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        w = i.getWidth(null);
        h = i.getHeight(null);
        int x = SIZE / 2 - w / 2;
        int y = SIZE / 2 - h / 2;
        bimage.getGraphics().drawImage(i, x, y, null);

        return bimage;
    }

    public Image getThumbnail() {
        return thumbnail;
    }

    public Roi getRoi() {
        return roi;
    }

    public double getDistance() {
        return distance;
    }

    public String getUnit() {
        return unit;
    }
}
