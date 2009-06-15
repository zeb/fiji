import janelia.dmesh.DMesh;

import java.awt.geom.Area;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.in.MinimalTiffReader;
import mpicbg.ij.InverseTransformMapping;
import mpicbg.models.AffineModel2D;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.InverseCoordinateTransformMap2D;

import ij.IJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ini.trakem2.Project;
import ini.trakem2.display.AreaList;
import ini.trakem2.display.Display;
import ini.trakem2.display.Layer;
import ini.trakem2.display.Patch;
import ini.trakem2.io.AmiraImporter;
import ini.trakem2.persistence.FSLoader;
import ini.trakem2.tree.ProjectThing;
import ini.trakem2.tree.ProjectTree;
import ini.trakem2.tree.TemplateThing;
import ini.trakem2.tree.TemplateTree;
import ini.trakem2.utils.Utils;


/**
 * @author saalfeld
 *
 */
public class DMesh_Visualization implements PlugIn
{
	protected File dir = null;
	
	public boolean setup()
	{
		File d = null;
		while ( d == null || !d.isDirectory() )
		{
			final DirectoryChooser dc = new DirectoryChooser( "Choose DMesh directory" );
			if ( dc.getDirectory() == null )
				return false;
			
			IJ.log( "opening " + dc.getDirectory() );
		
			d = new File( dc.getDirectory() );
		}
		
		dir = d;
		return true;
	}
	
	static protected ImagePlus openImageFile( final String path )
	{
		IFormatReader fr = null;
		try {
			fr = new MinimalTiffReader();
			fr.setId( path );
			return new ImagePlus( path, fr.openImage( 0 ) );
		} catch (FormatException fe) {
			IJ.error("Error in reading image file at " + path + "\n" + fe);
		} catch (Exception e) {
			IJ.error( e.toString() );
		} finally {
			if (null != fr) try {
				fr.close();
			} catch (IOException ioe) { IJ.log("Could not close IFormatReader: " + ioe); }
		}
		return null;
	}
	
	public void test()
	{
		if ( setup() )
		{
			try
			{
				final DMesh dMesh = new DMesh(
						dir.getAbsolutePath() + "/t.txt",
						dir.getAbsolutePath() + "/map.tif" );
				final InverseTransformMapping< InverseCoordinateTransform > iMapping = new InverseTransformMapping< InverseCoordinateTransform >( dMesh );
			
				final ImagePlus imp1 = openImageFile( dir.getAbsolutePath() + "/1.tif" );
				final ImagePlus imp2 = openImageFile( dir.getAbsolutePath() + "/2.tif" );
				final ImageProcessor ip2Mapped = imp1.getProcessor().createProcessor( imp1.getWidth(), imp1.getHeight() );
				iMapping.mapInterpolated( imp2.getProcessor(), ip2Mapped );
				
				new ImagePlus( "2 mapped", ip2Mapped ).show();
			
			
				final File[] files = dir.listFiles();
				for ( final File f : files )
				{
					if ( f.getName().endsWith( ".tif"  ) )
					{
						IJ.log( "found image " + f.getName() );
						
						final ImagePlus imp = openImageFile( f.getAbsolutePath() );
						imp.show();
						//FileInputStream fis = new FileInputStream( od.)
					}
				}
			}
			catch ( Exception e ) { IJ.error( e.getMessage() +"\n" + e.getStackTrace() ); }
		}
	}
	
	//@Override
	public void run( final String arg )
	{
		if ( setup() )
		{
			try
			{
				InputStream is = getClass().getResourceAsStream( "dmeshmapping.bsh.tpl" );
				byte[] bytes = new byte[ is.available() ];
				is.read( bytes );
				String script = new String( bytes );
				script = script.replaceAll( "<affinesTextPath>", dir.getAbsolutePath() + "/t.txt" );
				script = script.replaceAll( "<mapTextPath>", dir.getAbsolutePath() + "/map.tif" );
				
				final PrintStream ps = new PrintStream( dir.getAbsolutePath() + "/dmeshmapping.bsh" );
				ps.print( script );
				ps.close();
				
				IJ.log( "saved file " + dir.getAbsolutePath() + "/dmeshmapping.bsh" );
				
				final Project project = Project.newFSProject( "blank", null, dir.getAbsolutePath() );
				final Layer layer1 = project.getRootLayerSet().getLayer( 0 );
				final Layer layer2 = project.getRootLayerSet().getLayer( 40, 40, true );
				
				project.setProperty( "no_shutdown_hook", "true" );
				
				/* translating compressed tiff files into something ImageJ can eat */
				IJ.save( openImageFile( dir.getAbsolutePath() + "/1.tif" ), dir.getAbsolutePath() + "/1.ij.tif" );
				IJ.save( openImageFile( dir.getAbsolutePath() + "/2.tif" ), dir.getAbsolutePath() + "/2.ij.tif" );
				final Patch p1 = project.getLoader().importImage( project, 0, 0, dir.getAbsolutePath() + "/1.ij.tif", false );
				final Patch p2 = project.getLoader().importImage( project, 0, 0, dir.getAbsolutePath() + "/2.ij.tif", false );
				p2.setPreprocessorScriptPath( dir.getAbsolutePath() + "/dmeshmapping.bsh" );
				layer1.add( p1 );
				layer2.add( p2 );
				
				final Map< Float, AreaList > triangles = AmiraImporter.extractAreaLists( openImageFile( dir.getAbsolutePath() + "/map.tif" ), layer1, 0, 0, 0.2f, false );
				
				final TemplateTree ttree = project.getTemplateTree();  // these are DNDTree which extend JTree
				final ProjectTree ptree = project.getProjectTree();


				TemplateThing troot = ( TemplateThing ) ( ( DefaultMutableTreeNode ) ttree.getModel().getRoot() ).getUserObject();

				TemplateThing tt1 = ttree.addNewChildType( troot, "dmesh" );
				TemplateThing tt2 = ttree.addNewChildType( tt1, "triangle" );
				TemplateThing tt3 = ttree.addNewChildType( tt2, "area_list" );
				
				ProjectThing proot = project.getRootProjectThing();
				DefaultTreeModel pmodel = ( DefaultTreeModel ) ptree.getModel();
				DefaultMutableTreeNode proot_node = ( DefaultMutableTreeNode ) ptree.getRoot();
				// proot == root_node.getUserObject()

				ProjectThing pt0 = proot.createChild( "anything" );
				DefaultMutableTreeNode node0 = new DefaultMutableTreeNode( pt0 );
				proot_node.add( node0 );
				
				// Insert a new "transformations" node instance into the root
				// node of the ProjectTree
				ProjectThing pt1 = pt0.createChild( "dmesh" );
				DefaultMutableTreeNode node1 = new DefaultMutableTreeNode( pt1 );
				
				for ( final Map.Entry<  Float, AreaList > e : triangles.entrySet() )
				{
					/* Insert a new "triangle" node instance into the pt1 node; */
					final ProjectThing pt2 = pt1.createChild( "triangle" );
					DefaultMutableTreeNode node2 = new DefaultMutableTreeNode( pt2 );
					node1.add( node2 ); // at index 0
	
					// Insert a new "arealist" node instance into the pt2 node
					final AreaList al1 = e.getValue();
					al1.setArea( layer2.getId(), new Area( al1.getArea( layer1 ) ) );
					ProjectThing pt3 = new ProjectThing( tt3, project, e.getValue() );
					pt2.addChild( pt3 );
					e.getValue().setTitle( "" + ( int )( e.getKey() + 0.5f ) );
					
					DefaultMutableTreeNode node3 = new DefaultMutableTreeNode( pt3 );
					node2.add( node3 );
				}
				
				/* finally adding the root nopde into the tree */
				node0.add( node1 );
				
				// repaint
				ttree.updateUILater();
				ptree.updateUILater();
				
				Display.repaint();
								
				IJ.log( script );
			}
			catch ( Exception e ) { IJ.error( e.getMessage() ); }
		}
	}
}
