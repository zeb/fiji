import ij.ImagePlus;
import ij.process.ImageProcessor;
import mpicbg.ij.InverseTransformMapping;
import janelia.dmesh.DMesh;

dMesh = new DMesh(
	"<affinesTextPath>",
	"<mapTextPath>" );
itm = new InverseTransformMapping( dMesh );
ip = imp.getProcessor().createProcessor( dMesh.getWidth(), dMesh.getHeight() );
itm.mapInterpolated( imp.getProcessor(), ip );
imp.setProcessor( imp.getTitle(), ip );
