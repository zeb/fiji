package volumeCalculator;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Wand;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import java.awt.event.MouseEvent;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.Node;
import javax.media.j3d.SceneGraphPath;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import skeleton_analysis.Edge;
import skeleton_analysis.Point;
import skeleton_analysis.Vertex;

/**
 * <p>
 * CustomVolumeBehavior implements the special Picking behavior needed by the
 * Volume_Calculator. We are only concerned with the MOUSE_CLICKED event, any
 * others are passed up.
 * </p>
 * <p>
 * The special behavior consists of waiting for two clicks of the mouse button
 * and then the identification of the Java 3D nodes that are in a path 
 * connected between the two click points. This path is traversed calculating
 * the volume. If there is no path then nothing is done.
 * </p>
 * <p>
 * The InteractiveBehavior of the 3D Viewer is extended so as to catch the
 * events first.
 *</p>
 * @author pcmarks
 */
public class CustomVolumeBehavior extends InteractiveBehavior {

    /**
     * Where the branch node of a tree for a scene graph path is located.
     * NB: This could change!!
     */
    private static int BRANCH_NODE_INDEX = 3;
    private static final String NO_PATH_MSG = "No path between those two points.";
    
    private final PickCanvas pickCanvas;
    private boolean firstPickPicked;
    private SceneGraphPath firstClickSGP;
    private SceneGraphPath secondClickSGP;
    private Volumes volumes;        // Model
    private VolumesPanel volumesPanel;    // View
    private ImagePlus imagePlus;

    public CustomVolumeBehavior(
            Image3DUniverse universe,
            Content content,
            Volumes volumes,
            VolumesPanel volumesPanel,
            ImagePlus imagePlus) {
        super(universe);

        this.volumes = volumes;
        this.volumesPanel = volumesPanel;
        this.imagePlus = imagePlus;

        pickCanvas = new PickCanvas(universe.getCanvas(), content);
        pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);

    }

    /**
     *
     * Special consideration must be made for the fact that the AnalyzedGraph
     * BranchGroup is now a child of the ij3D viewers BranchGraphs.
     *
     */
    @Override
    public void doProcess(MouseEvent e) {
        int iD = e.getID();
        if (iD == MouseEvent.MOUSE_CLICKED) {
            // Get the point on the geometry where the mouse
            // press occurred
            pickCanvas.setShapeLocation(e.getX(), e.getY());
            PickResult pickResult = pickCanvas.pickClosest();
            if (pickResult != null) {
                if (firstPickPicked) {
                    volumesPanel.showStatus("Second Click");
                    secondClickSGP = pickResult.getSceneGraphPath();

                    // Determine if the two paths share  any nodes. Because each
                    // Network tree is a different branch graph, if the branch nodes
                    // for each node must be equal for there to be a connection.
                    // BRANCH_NODE_INDEX may be arbitrary!!
                    if (firstClickSGP.getNode(BRANCH_NODE_INDEX)
                            != secondClickSGP.getNode(BRANCH_NODE_INDEX)) {
                        IJ.showMessage(NO_PATH_MSG);
                        firstPickPicked = false;
                        volumesPanel.showStatus("");
                        return;
                    }
                    int lenSGP1 = firstClickSGP.nodeCount();
                    int lenSGP2 = secondClickSGP.nodeCount();
                    int commonCount = 0;
                    while ((commonCount < lenSGP1) && (commonCount < lenSGP2)
                            && (firstClickSGP.getNode(commonCount)
                            == secondClickSGP.getNode(commonCount))) {
                        commonCount++;
                    }
                    if (commonCount > 0) {
                        /**
                         * Backup one segment to the common place
                         */
                        commonCount--;
                        for (int c = commonCount; c < lenSGP1; c++) {
                            Node node = firstClickSGP.getNode(c);
                            if (node instanceof Group) {
                                for (int n = 0; n < ((Group) node).numChildren(); n++) {
                                    Node childNode = ((Group) node).getChild(n);
                                    if (childNode instanceof Shape3D) {
                                        highlightEdge((Shape3D) childNode);

                                    }
                                }
                            }
                        }
                        for (int c = commonCount; c < lenSGP2; c++) {
                            Node node = secondClickSGP.getNode(c);
                            if (node instanceof Group) {
                                for (int n = 0; n < ((Group) node).numChildren(); n++) {
                                    Node childNode = ((Group) node).getChild(n);
                                    if (childNode instanceof Shape3D) {
                                        highlightEdge((Shape3D) childNode);
                                    }
                                }
                            }
                        }
                    }
                    firstPickPicked = false;
                    volumesPanel.showStatus("");
                } else {
                    firstPickPicked = true;
                    firstClickSGP = pickResult.getSceneGraphPath();
                    volumesPanel.showStatus("First Click");
                }
            } else {
                firstPickPicked = false;        // Turn off - bail out
            }
        } else {
            super.doProcess(e);
        }
        return;
    }

    /**
     * Use the currently selected color to paint an edge and
     * caluculate the edge's volume and add it to that color's total.
     * @param shape Shape3D
     */
    void highlightEdge(Shape3D shape) {

        Color3f highlightColor = volumes.getSelectedColor();
        int ia = volumes.getCurrentColorIndex();
        if (shape.getGeometry() instanceof LineArray) {
            Appearance appearance = shape.getAppearance();
            ColoringAttributes ca = appearance.getColoringAttributes();
            Color3f before = new Color3f();
            Color3f after = new Color3f();
            appearance.setColoringAttributes(ca);
            ca.getColor(before);
            LineArray la = (LineArray) shape.getGeometry();


            Point3f p1 = new Point3f();
            Point3f p2 = new Point3f();
            la.getCoordinate(1, p1);
            la.getCoordinate(0, p2);
            ca.setColor(highlightColor);
            UserData ud = (UserData) (shape.getUserData());
            int oldColorIndex = ud.getColorIndex();
            int currentColorIndex = volumes.getCurrentColorIndex();
            int voxelCount = getVoxelCount(ud);
            volumes.updateVoxelCount(oldColorIndex, -voxelCount);
            volumes.updateVoxelCount(currentColorIndex, +voxelCount);
            // Make corresponding changes to the VolumePanel
            volumesPanel.updateVoxelCount(oldColorIndex);
            volumesPanel.updateVoxelCount(currentColorIndex);
            ud.setColorIndex(currentColorIndex);
        }
    }

    /**
     * Use the points available for this edge to compute the number of
     * voxels at each slice. If the slice number doesn't change then do not
     * compute the voxel count because it has already been done.
     * 
     * @param userData Information about an edge
     * @return the total number of voxels
     */
    int getVoxelCount(UserData userData) {
        Object graphData = userData.getGraphInfo();
        int totalVoxels = 0;
        if (graphData instanceof Edge) {
            Edge edge = (Edge) graphData;
            Vertex v1 = edge.getV1();
            Point p1 = v1.getPoints().get(0);
            Vertex v2 = edge.getV2();
            Point p2 = v2.getPoints().get(0);
            // For all the points along this edge:
            // Move to the slice number z + 1 and run the Wand at the x, y
            // coordinates of the original (thresholded image).
            // NB: Adding 1 to slice number because this z is 0-based.
            ImageProcessor sliceProcessor;
            Wand wand;
            sliceProcessor = imagePlus.getStack().getProcessor(p1.z + 1);
            wand = new Wand(sliceProcessor);
            wand.autoOutline(p1.x, p1.y);
            totalVoxels += wand.npoints;
            // Need to watch for a change in the value of the z coordinate
            int lastZpoint = p1.z + 1;
            for (Point point : edge.getSlabs()) {
                // If the z position has not changed skip this point because
                // the previous pixel is connected to this pixel.
                if (point.z + 1 != lastZpoint) {
                    sliceProcessor = imagePlus.getStack().getProcessor(point.z + 1);
                    wand = new Wand(sliceProcessor);
                    wand.autoOutline(point.x, point.y);
                    totalVoxels += wand.npoints;
                    lastZpoint = point.z + 1;
                }
            }
            if (lastZpoint != (p2.z + 1)) {
                // get the last point and count the surrounding pixels
                sliceProcessor = imagePlus.getStack().getProcessor(p2.z + 1);
                wand = new Wand(sliceProcessor);
                wand.autoOutline(p2.x, p2.y);
                totalVoxels += wand.npoints;
            }
        }
        return totalVoxels;
    }
}
