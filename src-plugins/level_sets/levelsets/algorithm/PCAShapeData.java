package levelsets.algorithm;

import levelsets.ij.ImageContainer;
import mpicbg.models.RigidModel2D;

public class PCAShapeData {
	
	public final int VERSION = 1;
	public int w, h, d, no_pix;
	public int no;
	public double [] meanSignedDistance;
	public double [][] pcaImage;
	public double [] pcaDev;
	
	RigidModel2D rigid = new RigidModel2D();

	
	// Constructors make sure that arrays are never empty
	public PCAShapeData(int w, int h, int d, int no) {
		init(w, h, d, no);
	}
	
	public PCAShapeData(String serialized) {
		// TODO
	}
	
	protected void init(int w, int h, int d, int no) {
		this.w = w;
		this.h = h;
		this.no_pix = w * h * d;

		meanSignedDistance = new double[no_pix];
		pcaImage = new double[no_pix][no];
		pcaDev = new double[no_pix];
	
	}
	
	// sqrt of Eigenvalues is the std.dev. of the PCA
	public void setPCAdev(double [] eigenvals) {
		for ( int i=0; i<eigenvals.length; i++ ) {
			pcaDev[i] = Math.sqrt(eigenvals[i]);
		}
	}
	
	// makes the image PCA singular -> std dev = 1
	public void makeSingular() {
		for ( int i=0; i<pcaDev.length; i++) {
			for ( int j=0; j<no; j++ ) {
				pcaImage[i][j] *= pcaDev[i];
			}
		}
	}
	
	public void serialize() {
		
	}

	// Encapsule all arrangements in here to ensure consistent coordinate system
	protected double [][] addPxVector(double [][] A, int pos, ImageContainer img) {
		int px = 0;
		
		if ( no_pix != img.getWidth() * img.getHeight() * img.getImageCount() ) {
			throw new ArithmeticException("Training images are not of identical size");
		}
		
		for (int z=0; z < img.getWidth(); z++ ) {
			for (int y=0; y < img.getHeight(); y++) {
				for (int x=0; x < img.getImageCount(); x++) {
					A[px++][pos] = img.getPixel(x, y, z);
				}
				
			}
			
		}
		
		return A;
	}

	public int getPointInModel(int x, int y, int z, float [] pose) {
		
		float [] coord = new float[2];
		int coord_sx, coord_sy, coord_sz, pt;
		
		coord[0] = x;
		coord[1] = y;
		// TODO z
		
		rigid.applyInverseInPlace(coord);

		coord_sx = Math.min(w, Math.round(coord[0]));
		coord_sy = Math.min(h, Math.round(coord[1]));
		coord_sz = 1;

		pt = coord_sx + coord_sy * w + coord_sz * (d - 1);
		
		return pt;
	}
	
	
	
}
