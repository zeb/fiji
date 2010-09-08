package volumeCalculator;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import java.awt.BorderLayout;

/**
 * <p>This is the Volume_Calculator ImageJ/Fiji plugin. It is a plugin that is
 * capable of measuring the volume of a selected path in the 3D skeletonized
 * representation of
 * a vasculature. The volume is measured in voxels or other units,
 * e.g. cm^3, depending on the meta information associated with the source image
 * file.
 * </p><p>
 * Internally it runs the Skeletonize and then the AnalyzeSkeleton plugins. The
 * output of the analysis is used to draw a Java 3D image. The user can then
 * pick points in a path and get the volume based on the original image.
 * </p>
 *
 * @author Peter C Marks - Maine Medical Center Research Institute (MMCRI.org)
 */
public class Volume_Calculator implements PlugInFilter {

    private AnalyzedGraph vasculature;
    private Image3DUniverse universe;
    private ImagePlus imagePlus;
    private ImageProcessor ip;
    private Content content;
    private VolumesPanel volumesPanel;

    /////
    // Implementation of the PlugInFilter interface
    /////
    @Override
    public int setup(String string, ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        return DOES_ALL + STACK_REQUIRED;
    }

    /**
     * Start the Volume_Calculator plugin. This means invoking the
     * Fiji 3D Viewer in which the Java 3D image is displayed. A 3D Viewer
     * Content object is created to encapsulate the Java 3D scene graph.
     * 
     * @param ip ImageProcessor
     */
    @Override
    public void run(ImageProcessor ip) {

        this.ip = ip;

        vasculature = new AnalyzedGraph();
        vasculature.init(imagePlus);
        vasculature.getSceneGraph().compile();

        // Create a universe and show it
        universe = new Image3DUniverse();
        universe.setShowBoundingBoxUponSelection(true);     // Default is on.
        universe.show();

        // We need this image's Calibration info from the image in order to
        // calculate the volumes accurately. Given to Volumes.
        Calibration calibration = imagePlus.getCalibration();

        // Create the volumes data structure. Its gui is: VolumePanel
        // VolumePanel is placed to the SOUTH of what's in 3D Viewer
        Volumes volumes = new Volumes(calibration);
        volumesPanel = new VolumesPanel(volumes, vasculature, universe);
        universe.getWindow().add(volumesPanel,BorderLayout.SOUTH);
        universe.getWindow().pack();

        // Ask AnalyzedGraph for a Java 3D version of the network -
        GraphContentNode contentNode = vasculature.getSceneGraph();
        content = new Content("Vessel Network");
        content.display(contentNode);

        // Create the picking behavior (Controller) for the graphic view of
        // the vasculature. This controller also needs a Volumes instance for storing
        // the selected volumes.
        universe.setInteractiveBehavior(
                new CustomVolumeBehavior(universe, content, volumes, volumesPanel, imagePlus));
        universe.addContent(content);

    }

    /////
    //
    /////
    /**
     * main() is available in case you want to test this plugin directly.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ImageJ imageJ = null;
        try {
            imageJ = new ij.ImageJ();
            IJ.run("Open...");
            Volume_Calculator volume_calculator = new Volume_Calculator();
            volume_calculator.setup("", IJ.getImage());
            volume_calculator.run(IJ.getImage().getProcessor());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            IJ.showMessage(e.getLocalizedMessage());
            System.out.println(""+e.getLocalizedMessage());
            imageJ.quit();
        }
    }
}
