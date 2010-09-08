
package volumeCalculator;

import ij.measure.Calibration;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Color3f;


/**
 * <p>An instance of Volumes is used to keep track of the volume values accumulated by
 * the Volume_Calculator plugin. There is one set of volume values for each
 * edge "color". There can be an arbitrary number of "colors";  only one color is
 * active at one time. When the user selects an edge the display color of the
 * edge is changed to the current color and the number of voxels is calculated
 * for this edge.
 * </p>
 * <p>
 * MVC: An instance of Volumes (the Model) is shared by CustomValueBehavior (the
 * Controller) and VolumePanel (the View).
 * </p>
 * 
 * @author pcmarks
 */
public class Volumes {

    private final String calibrationUnits;
    private final double volumeMultiplier;

    private int                     currentColorIndex = 0;

    private List<Double>             volumes = new ArrayList<Double>();
    private List<Integer>           voxelCounts = new ArrayList<Integer>();
    private List<Color3f>           colors = new ArrayList<Color3f>();

    public Volumes (Calibration calibration) {
        this.calibrationUnits = calibration.getUnits();
        this.volumeMultiplier = calibration.pixelDepth *
                                calibration.pixelHeight *
                                calibration.pixelWidth;
    }

    /**
     * The user has created another Volume Color. Make a place for its color and
     * its volume.
     * 
     * @param chosenColor
     * @return
     */
    public boolean addVolumeColor(Color chosenColor) {
        volumes.add(new Double(0));
        voxelCounts.add(new Integer(0));
        colors.add(new Color3f(chosenColor));
        currentColorIndex = volumes.size()-1;
        return true;
    }

    Color3f getSelectedColor() {
        return colors.get(currentColorIndex);
    }

    int getCurrentColorIndex() {
        return currentColorIndex;
    }

    void setCurrentColorIndex(int index) {
        currentColorIndex = index;
    }

    void updateVoxelCount(int colorIndex, int count) {
        if (colorIndex == UserData.INITIAL_COLOR_INDEX) return;
        int voxelCount = voxelCounts.get(colorIndex) + count;
        voxelCounts.set(colorIndex, voxelCount);
        Double volume = volumes.get(colorIndex);
        volumes.set(colorIndex, voxelCount * volumeMultiplier);
    }

    Double getVolumeAt(int colorIndex) {
        return volumes.get(colorIndex);
    }

    public String getCalibrationUnits() {
        return calibrationUnits;
    }

    Color3f getColorAt(int colorIndex) {
        return colors.get(colorIndex);
    }

    void clearVoxelCount(int colorIndex) {
        updateVoxelCount(colorIndex, -voxelCounts.get(colorIndex));
    }
    
}
