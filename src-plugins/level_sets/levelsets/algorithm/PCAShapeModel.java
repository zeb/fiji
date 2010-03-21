package levelsets.algorithm;

import java.util.Vector;

import levelsets.ij.ImageContainer;
import mpicbg.models.RigidModel2D;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.EigenDecomposition;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;

public class PCAShapeModel {
	
	PCAShapeData model;
	RigidModel2D rigid = new RigidModel2D();

	
	public PCAShapeModel() {
		
	}
	
	
	public PCAShapeModel(String fn) {
		
	}
	
	
	public double getDistanceValue(int x, int y, int z, double [] p, float [] pose) {

		int point = model.getPointInModel(x, y, z, pose);
		
		return (model.meanSignedDistance[point]);
		
	}
	
	
	public int getNoCurveParams() {
		return model.no;
	}
	
	
	public int getNoPoseParams() {
		if ( model.d == 1 ) {
			return 3;
		} else {
			throw new ArithmeticException("3D shape models not implemented yet");
		}
	}
	
	
	// returns equivalent of u*
	public double getCurveValue(int x, int y, int z, double [] p) {
		double u = 0;
		float [] pose = new float[p.length - model.no];
		
		for (int i=model.no; i < p.length; i++ ) {
			pose[i-model.no] = (float) p[i];
		}
		
		int point = model.getPointInModel(x, y, z, pose);
		
		u += model.meanSignedDistance[point];
		for ( int i=0; i < p.length; i++ ) {
			u += p[i] * model.pcaDev[i] * model.pcaImage[point][i];
		}
		
		return u;
	}
	
	
	public void learnTrainingImages(Vector<ImageContainer> images) {
		int img_no;
		int img_px;
		ImageContainer img, img_dist;
		double [][] a;
		RealMatrix A, A_for_pca;


		img_no = images.size();
		img = images.elementAt(0);
		img_px = img.getWidth() * img.getHeight();
		a = new double[img_px][img_no];
		model = new PCAShapeData(img.getWidth(), img.getHeight(), img.getImageCount(), img_no);
		
		// Arrange images into array pixels x training images
		img_dist = null; // TODO should be mean signed distance function to model
		a = model.addPxVector(a, 0, img_dist);
		for (int i=1; i<img_no; i++) {
			img = images.elementAt(i);
			a = model.addPxVector(a, i, img);
		}
		
		// Eigenvalue calculations
		A = new Array2DRowRealMatrix(a);
		A_for_pca = A.transpose().multiply(A); // A' * A
		EigenDecomposition A_eig = new EigenDecompositionImpl(A_for_pca, 0);
		RealMatrix img_eigv = A_eig.getV();
		model.setPCAdev(A_eig.getRealEigenvalues());
		
		// Create PCA images
		for ( int t=0; t < img_no; t++ ) {
			for ( int p=0; p < img_px; p++ ) {
				double val_px = a[p][t];
				for ( int v=0; v < img_no; v++ ) {
					model.pcaImage[p][v] = val_px * img_eigv.getEntry(t, v);
				}
			}
		}		
	}
	
	
}
