package volumeCalculator;

import ij3d.ContentNode;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3d;

/**
 * GraphContentNode is a 3D Viewer ContentNode designed to be the structure returned
 * by the AnalyzedGraph class as a Java 3D representation of the vascular tree
 * representation created by the AnalyzeSkeleton plugin.
 *
 * In essence it is a Java 3D scene graph (BranchGroup).
 *
 * @author pcmarks
 */
public class GraphContentNode extends ContentNode {
    private final Point3f min;
    private final Point3f max;
    private final Point3f center;

    public GraphContentNode() {
        super();
        min = new Point3f(0,0,0);
        max = new Point3f(1,1,1);
        center = new Point3f(1f,1f,1f);
    }

    @Override
    public float getVolume() {
        return 0;
    }

    @Override
    public void getMin(Tuple3d tpld) {
        tpld.set(this.min);
    }

    @Override
    public void getMax(Tuple3d tpld) {
        tpld.set(this.max);
    }

    @Override
    public void getCenter(Tuple3d tpld) {
        tpld.set(this.center);
    }

    @Override
    public void channelsUpdated(boolean[] blns) {
        ;
    }

    @Override
    public void thresholdUpdated(int i) {
        ;
    }

    @Override
    public void colorUpdated(Color3f clrf) {
        ;
    }

    @Override
    public void transparencyUpdated(float f) {
        ;
    }

    @Override
    public void shadeUpdated(boolean bln) {
        ;
    }

    @Override
    public void eyePtChanged(View view) {
        ;
    }

}
