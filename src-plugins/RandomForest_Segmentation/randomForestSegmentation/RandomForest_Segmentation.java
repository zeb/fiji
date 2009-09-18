package randomForestSegmentation;
/* This is a small Plugin that should perform better in segmentation than thresholding
 * The idea is to train a random forest classifier on given manual labels
 * and then classify the whole image 
 * I try to keep parameters hidden from the user to make usage of the plugin
 * intuitive and easy. I decided that it is better to need more manual annotations
 * for training and do feature selection instead of having the user manually tune 
 * all filters.
 * 
 * ToDos:
 * - work on whole Stack and distinguish between train and test images
 *     - Another option is to divide the image into train and test area
 * - give option to specify annotations as test data and give confusion matrix
 * - give option to delete annotations
 * - save classifier and load classifier
 * - apply classifier to other images
 * - make button to show color overlay
 * - look for bug with color jpg
 * - put thread solution to wiki http://pacific.mpi-cbg.de/wiki/index.php/Developing_Fiji#Writing_plugins
 * 
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Author: Verena Kaynig (verena.kaynig@inf.ethz.ch)
 */


import ij.IJ;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.ImagePlus;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import hr.irb.fastRandomForest.FastRandomForest;

public class RandomForest_Segmentation implements PlugIn {
	
	private List<Roi> positiveExamples = new ArrayList< Roi>(); 
	private List<Roi> negativeExamples = new ArrayList< Roi>();
	private ImagePlus trainingImage;
	private ImagePlus displayImage;
	private ImagePlus classifiedImage = null;
	private FeatureStack featureStack;
   	final Button posExampleButton;
  	final Button negExampleButton;
  	final Button trainButton;
  	final Button overlayButton;
  	
 
  	
  	public RandomForest_Segmentation() {
	    	posExampleButton = new Button("positiveExample");
  	      	negExampleButton = new Button("negativeExample");
  	      	trainButton = new Button("train Classifier");
  	      	overlayButton = new Button("create Overlay");
	}
	
  	ExecutorService exec = Executors.newFixedThreadPool(1);
  	
  	private ActionListener listener = new ActionListener() {
  		public void actionPerformed(final ActionEvent e) {
  			exec.submit(new Runnable() {
  				public void run() {
  					if(e.getSource() == posExampleButton){
  		  				addPositiveExamples();
  		  			}
  		  			if(e.getSource() == negExampleButton){
  		  				addNegativeExamples();
  		  			}
  		  			if(e.getSource() == trainButton){
  		  				trainClassifier();
  		  			}
  		  			if(e.getSource() == overlayButton){
  		  				showOverlay();
  		  			}
  				}
  			});
  		}
  	};
  	
 
  	
  	private class CustomWindow extends ImageWindow {
  		CustomWindow(ImagePlus imp) {
  			super(imp);
  			Panel all = new Panel();
  			BoxLayout box = new BoxLayout(all, BoxLayout.Y_AXIS);
  			all.setLayout(box);
  			
  			Panel piw = new Panel();
  			piw.setLayout(super.getLayout());
  			setTitle("Playground");
  			for (Component c : getComponents()) {
  				piw.add(c);
  			}
  			
  			
  			Panel buttons = new Panel();
  			buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
  			
  	      	posExampleButton.addActionListener(listener);
  	      	negExampleButton.addActionListener(listener);
  	      	trainButton.addActionListener(listener);
  	      	overlayButton.addActionListener(listener);
  	      	buttons.add(posExampleButton);
  	      	buttons.add(negExampleButton);
  	      	buttons.add(trainButton);
  	      	buttons.add(overlayButton);
  	      	
  	      	for (Component c : new Component[]{posExampleButton, negExampleButton, trainButton, overlayButton}) {
  	      		c.setMaximumSize(new Dimension(230, 50));
  	      		c.setPreferredSize(new Dimension(130, 30));
  	      	}
  	      	
  	      	all.add(piw);
  	      	all.add(buttons);
  	      	
  	      	removeAll();
  	      	
  	      	add(all);
  	      	
  	      	pack();
  	      	
  	      	// Propagate all listeners
  	      	for (Panel p : new Panel[]{all, buttons, piw}) {
  	      		for (KeyListener kl : getKeyListeners()) {
  	      			p.addKeyListener(kl);
  	      		}
  	      	}
  	      	
  	      	addWindowListener(new WindowAdapter() {
  	      		public void windowClosing(WindowEvent e) {
  	      			// cleanup
  	      			exec.shutdownNow();
  	      		}
  	      	});
  		}
  	}
  	
	public void run(String arg) {
//		trainingImage = IJ.openImage("testImages/i00000-1.tif");
		//get current image
		trainingImage = ij.WindowManager.getCurrentImage();
		if (trainingImage==null) {
			IJ.error("No image open.");
			return;
		}
		
		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
													 "is larger than 1024 pixels. \n" +
													 "Feature stack creation and classifier training \n" +
													 "might take some time depending on your computer.\n" +
													 "Proceed?"))
				return;

		
		trainingImage.setProcessor("training image", trainingImage.getProcessor().convertToByte(true));
		createFeatureStack(trainingImage);
		displayImage = new ImagePlus();
		displayImage.setProcessor("training image", trainingImage.getProcessor().convertToRGB());
		
		
		//Build GUI
		ImageWindow win = new CustomWindow(displayImage);
		trainingImage.getWindow().setVisible(false);
		}
	
	private void addPositiveExamples(){
		//get selected pixels
		Roi r = displayImage.getRoi();
		displayImage.killRoi();
		positiveExamples.add(r);
		drawExamples();
	}
	
	private void addNegativeExamples(){
		//get selected pixels
		Roi r = displayImage.getRoi();
		displayImage.killRoi();
		negativeExamples.add(r);
		drawExamples();
	}
	
	private void drawExamples(){
		displayImage.setColor(Color.GREEN);
		for (Roi r : positiveExamples){
			r.drawPixels(displayImage.getProcessor());
		}
		
		displayImage.setColor(Color.RED);
		for (Roi r : negativeExamples){
			r.drawPixels(displayImage.getProcessor());
		}
		
		displayImage.updateAndDraw();
	}
	
	public void createFeatureStack(ImagePlus img){
		IJ.log("creating feature stack");
		featureStack = new FeatureStack(img);
		int counter = 1;
		for (float i=2.0f; i<featureStack.getWidth()/5.0f; i*=2){
			IJ.showStatus("creating feature stack   " + counter);
			featureStack.addGaussianBlur(i); counter++;
			IJ.showStatus("creating feature stack   " + counter);			
			featureStack.addGradient(i); counter++;
			IJ.showStatus("creating feature stack   " + counter);			
			featureStack.addHessian(i); counter++;
			for (float j=2.0f; j<i; j*=2){
				IJ.showStatus("creating feature stack   " + counter);				
				featureStack.addDoG(i, j); counter++;
			}
		}
	}
	
	
	public void writeDataToARFF(Instances data, String filename){
		try{
			BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(
				new FileOutputStream( filename) ) );
			try{	
					out.write(data.toString());
					
			}
			catch(IOException e){IJ.showMessage("IOException");}
	}
	catch(FileNotFoundException e){IJ.showMessage("File not found!");}
		
	}

	public Instances createTrainingInstances(){
		FastVector attributes = new FastVector();
		for (int i=1; i<=featureStack.getSize(); i++){
			String attString = featureStack.getSliceLabel(i) + " numeric";
			attributes.addElement(new Attribute(attString));
		}
		FastVector classes = new FastVector();
		classes.addElement("foreground");
		classes.addElement("background");
		attributes.addElement(new Attribute("class", classes));
		
		Instances trainingData =  new Instances("segment", attributes, positiveExamples.size()+negativeExamples.size());
		
		for(int j=0; j<positiveExamples.size(); j++){
			Roi r = positiveExamples.get(j);
			int[] x = r.getPolygon().xpoints;
			int[] y = r.getPolygon().ypoints;
			int n = r.getPolygon().npoints;
			
			for (int i=0; i<n; i++){
				double[] values = new double[featureStack.getSize()+1];
				for (int z=1; z<=featureStack.getSize(); z++){
					values[z-1] = featureStack.getProcessor(z).getPixelValue(x[i], y[i]);
				}
				values[featureStack.getSize()] = 1.0;
				trainingData.add(new Instance(1.0, values));
			}
		}
		
		for(int j=0; j<negativeExamples.size(); j++){
			Roi r = negativeExamples.get(j);
			int[] x = r.getPolygon().xpoints;
			int[] y = r.getPolygon().ypoints;
			int n = r.getPolygon().npoints;
			
			for (int i=0; i<n; i++){
				double[] values = new double[featureStack.getSize()+1];
				for (int z=1; z<=featureStack.getSize(); z++){
					values[z-1] = featureStack.getProcessor(z).getPixelValue(x[i], y[i]);
				}
				values[featureStack.getSize()] = 0.0;
				trainingData.add(new Instance(1.0, values));
			}
		}
		return trainingData;
	}
	
	public void trainClassifier(){
		 IJ.showStatus("training classifier");
		 long start = System.currentTimeMillis();
		 Instances data = createTrainingInstances();
		 long end = System.currentTimeMillis();
		 IJ.log("creating training data took: " + (end-start));
		 data.setClassIndex(data.numAttributes() - 1);
//		 writeDataToARFF(data, "trainingDataFromInstances.arff");
		 
		 FastRandomForest rf = new FastRandomForest();
		 //FIXME: should depend on image size?? Or labels??
		 rf.setNumTrees(100);
		 //this is the default that Breiman suggests
		 rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		 //rf.setNumFeatures(2);
		 rf.setSeed(123);
		 
		 IJ.log("training classifier");
		 try{rf.buildClassifier(data);}
		 catch(Exception e){IJ.showMessage("Could not train Classifier!");}
		 
		 IJ.log("reading whole image data");
		 start = System.currentTimeMillis();
		 data = featureStack.createInstances();
		 end = System.currentTimeMillis();
		 IJ.log("creating whole image data took: " + (end-start));
		 data.setClassIndex(data.numAttributes() - 1);
		 
//		 writeDataToARFF(data, "testWholeImageData.arff");
		 
		 IJ.log("classifying image");
		 double[] classificationResult = new double[data.numInstances()];
		 for (int i=0; i<data.numInstances(); i++){
			 try{
			 classificationResult[i] = rf.classifyInstance(data.instance(i));
			 }catch(Exception e){IJ.showMessage("Could not apply Classifier!");}
		 }
		 
		 IJ.log("showing result");
		 ImageProcessor classifiedImageProcessor = new FloatProcessor(trainingImage.getWidth(), trainingImage.getHeight(), classificationResult);
		 classifiedImageProcessor.convertToByte(true);
		 classifiedImage = new ImagePlus("classification result", classifiedImageProcessor);
		 classifiedImage.show();
	}

	void showOverlay(){
		int width = trainingImage.getWidth();
		int height = trainingImage.getHeight();
		
		ImageStack redStack = new ImageStack(width, height);
		redStack.addSlice("red", classifiedImage.getProcessor().duplicate());
		ImageStack greenStack = new ImageStack(width, height);
		trainingImage.show();
		greenStack.addSlice("green", trainingImage.getProcessor().duplicate());
		ImageStack blueStack = new ImageStack(width, height);
		blueStack.addSlice("blue", trainingImage.getProcessor().duplicate());
		
		RGBStackMerge merger = new RGBStackMerge();
		ImageStack overlayStack = merger.mergeStacks(trainingImage.getWidth(), trainingImage.getHeight(), 
						   1, redStack, greenStack, blueStack, true);
	
		ImagePlus overlayImage = new ImagePlus("overlay image", overlayStack);
		overlayImage.show();
	}
	
}
