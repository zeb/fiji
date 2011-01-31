import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.PlugInFilter;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;
import java.text.*;

/*
 * NewFrame.java
 *
 * Created on January 17, 2006, 10:11 AM
 */

/**
 *
 * @author  Thomas Kuo
 */
public class ITCN_ extends PlugInFrame {

    /** Creates new form NewFrame */
    public ITCN_() {
		super("ITCN: Image-based Tool for Counting Nuclei");
		IJ.register(ITCN_.class);

        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        imagePanel = new java.awt.Panel();
        ccImageLabel = new java.awt.Label();
        filenameLabel = new java.awt.Label();
        varsPanel = new java.awt.Panel();
        widthLabel = new java.awt.Label();
        widthTextField = new java.awt.TextField();
        minDistUnitsLabel = new java.awt.Label();
        minDistLabel = new java.awt.Label();
        minDistTextField = new java.awt.TextField();
        widthUnitsLabel = new java.awt.Label();
        recomendLabel = new java.awt.Label();
        darkPeaksCheckbox = new java.awt.Checkbox();
        maskPanel = new java.awt.Panel();
        maskLabel = new java.awt.Label();
        maskChoice = new java.awt.Choice();
        openMaskButton = new java.awt.Button();
        buttonPanel = new java.awt.Panel();
        okButton = new java.awt.Button();
        exitButton = new java.awt.Button();
        widthButton = new java.awt.Button();
        minDistButton = new java.awt.Button();
        thresLabel = new java.awt.Label();
        thresTextField = new java.awt.TextField();
        thresScroll = new java.awt.Scrollbar();
        midPanel = new java.awt.Panel();

		java.awt.GridBagConstraints gridBagConstraints;
		int ipadx = 50;

        setLayout(new java.awt.GridLayout(5, 1));

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });


		// window manager stuff.....

		currImp = WindowManager.getCurrentImage();
		if (currImp==null) {
			IJ.beep();
			IJ.showStatus("No image");
			return;
		}
		if (!currImp.lock())
			return;

		int[] WinList = WindowManager.getIDList();
		if (WinList==null) {
			IJ.error("No windows are open.");
			return;
		}

		winIDList = new ArrayList(WinList.length+1);
		winIDList.add(new Integer(0));
		for (int i=0; i<WinList.length; i++) {
			winIDList.add(new Integer(WinList[i]));
		}

		String[] WinTitles = new String[WinList.length + 1];
		WinTitles[0] = strNONE;

		// Window Manager stuff...

        imagePanel.setLayout(new java.awt.GridLayout());

        ccImageLabel.setAlignment(java.awt.Label.RIGHT);
        ccImageLabel.setText("Image Name:");
        imagePanel.add(ccImageLabel);

        filenameLabel.setText(currImp.getTitle());
        imagePanel.add(filenameLabel);

        add(imagePanel);

        //varsPanel.setLayout(new java.awt.GridLayout(3, 4));
        varsPanel.setLayout(new java.awt.GridBagLayout());

        widthLabel.setAlignment(java.awt.Label.RIGHT);
        widthLabel.setText("Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        varsPanel.add(widthLabel, gridBagConstraints);

//        widthLabel.setAlignment(java.awt.Label.RIGHT);
//        widthLabel.setText("Width");
//        varsPanel.add(widthLabel);

        widthTextField.setText(Integer.toString(widthDefault));
        widthTextField.addTextListener(new java.awt.event.TextListener() {
            public void textValueChanged(java.awt.event.TextEvent evt) {
                widthTextFieldTextValueChanged(evt);
            }
        });
        //varsPanel.add(widthTextField);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = ipadx;
		varsPanel.add(widthTextField, gridBagConstraints);

        minDistUnitsLabel.setText("pixels");
        varsPanel.add(minDistUnitsLabel);

        widthButton.setLabel("Measure Line Length");
        widthButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                widthButtonActionPerformed(evt);
            }
        });
		//varsPanel.add(widthButton);
		gridBagConstraints = new java.awt.GridBagConstraints();
        varsPanel.add(widthButton,gridBagConstraints);

        minDistLabel.setAlignment(java.awt.Label.RIGHT);
        minDistLabel.setText("Minimum Distance");
        varsPanel.add(minDistLabel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        varsPanel.add(minDistLabel, gridBagConstraints);

        minDistTextField.setText(Double.toString(min_distDefault));
        varsPanel.add(minDistTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = ipadx;
        varsPanel.add(minDistTextField, gridBagConstraints);

        widthUnitsLabel.setText("pixels");
        varsPanel.add(widthUnitsLabel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        varsPanel.add(widthUnitsLabel, gridBagConstraints);

        minDistButton.setLabel("Measure Line Length");
        minDistButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minDistButtonActionPerformed(evt);
            }
        });
		varsPanel.add(minDistButton);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        varsPanel.add(minDistButton, gridBagConstraints);

		thresLabel.setAlignment(java.awt.Label.RIGHT);
		thresLabel.setText("Threshold");
		varsPanel.add(thresLabel);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        varsPanel.add(thresLabel, gridBagConstraints);

		thresTextField.setText(Double.toString(thresDefault));
        thresTextField.addTextListener(new java.awt.event.TextListener() {
            public void textValueChanged(java.awt.event.TextEvent evt) {
                thresTextFieldTextValueChanged(evt);
            }
        });
		varsPanel.add(thresTextField);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = ipadx;
        varsPanel.add(thresTextField, gridBagConstraints);

		//varsPanel.add(thresScroll);
		thresScroll.setValues((int)(thresPrecision*thresDefault),1,0,10*(int)thresPrecision+1);
        thresScroll.setOrientation(java.awt.Scrollbar.HORIZONTAL);
        thresScroll.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                thresScrollAdjustmentValueChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        varsPanel.add(thresScroll, gridBagConstraints);

        add(varsPanel);

		midPanel.setLayout(new java.awt.GridLayout(2,1));

        recomendLabel.setAlignment(java.awt.Label.CENTER);
        recomendLabel.setText("(Recommended: Minimum Distance = Width/2)");
        midPanel.add(recomendLabel);

        darkPeaksCheckbox.setLabel("Detect Dark Peaks");
        midPanel.add(darkPeaksCheckbox);

		add(midPanel);

        maskLabel.setText("Mask Image");
        maskPanel.add(maskLabel);

        maskPanel.add(maskChoice);
        maskChoice.add(WinTitles[0]);
		for (int i=0; i<WinList.length; i++) {
			ImagePlus imp = WindowManager.getImage(WinList[i]);
			if (imp != null) {
				WinTitles[i+1] = imp.getTitle();
			} else {
				WinTitles[i+1] = "";
			}
			maskChoice.add(WinTitles[i+1]);
		}

        openMaskButton.setLabel("Open...");
        openMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMaskButtonActionPerformed(evt);
            }
        });

        maskPanel.add(openMaskButton);

        add(maskPanel);

        buttonPanel.setLayout(new java.awt.GridLayout());

        okButton.setLabel("Count");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(okButton);

        exitButton.setLabel("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(exitButton);

        add(buttonPanel);

        pack();
        setSize(400, 375);
		GUI.center(this);
        show();
    }

    private void widthTextFieldTextValueChanged(TextEvent evt) {
		minDistTextField.setText(Double.toString(Double.parseDouble(widthTextField.getText())/2));
    }

	private void widthButtonActionPerformed(ActionEvent evt) {
		Roi roi = currImp.getRoi();

		if (roi.isLine()) {
			Line line = (Line) roi;
			widthTextField.setText(Integer.toString((int)Math.ceil(line.getRawLength())));
		}
	}

	private void minDistButtonActionPerformed(ActionEvent evt) {
		Roi roi = currImp.getRoi();

		if (roi.isLine()) {
			Line line = (Line) roi;
			minDistTextField.setText(Integer.toString((int)Math.ceil(line.getRawLength())));
		}
	}

	private void thresTextFieldTextValueChanged(TextEvent evt) {
		double threshold = Double.parseDouble(thresTextField.getText());
		if (thresPrecision*threshold != Math.round(thresPrecision*threshold)) {
			threshold = Math.round(thresPrecision*threshold)/thresPrecision;
			thresTextField.setText(Double.toString(threshold));
		}
		if ((double)thresScroll.getValue() != thresPrecision*threshold)
			thresScroll.setValue((int)(thresPrecision*threshold));
	}

	private void thresScrollAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
		double threshold = (double)thresScroll.getValue();
		thresTextField.setText(Double.toString(threshold/thresPrecision));
	}

	private void exitButtonActionPerformed(ActionEvent evt) {
		close();
	}

    private void okButtonActionPerformed(ActionEvent evt) {
		//close();

		widthDefault = Integer.parseInt(widthTextField.getText());
		min_distDefault = Double.parseDouble(minDistTextField.getText());
		thresDefault = Double.parseDouble(thresTextField.getText());

		int maskIndex = maskChoice.getSelectedIndex();
		String maskString = maskChoice.getSelectedItem();

		Integer maskID = (Integer)winIDList.get(maskIndex);

		ImagePlus maskImp = WindowManager.getImage(maskID.intValue());

		new ITCN_Runner(currImp, Integer.parseInt(widthTextField.getText()),
			Double.parseDouble(minDistTextField.getText()), Double.parseDouble(thresTextField.getText()),
			darkPeaksCheckbox.getState(), maskImp);
    }

    private void openMaskButtonActionPerformed(ActionEvent evt) {
		OpenDialog od = new OpenDialog("Open Mask...","");
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
            return;

		Opener opener = new Opener();
		ImagePlus imp2= opener.openImage(directory, name);

		winIDList.add(new Integer(imp2.getID()));

		maskChoice.add(name);
		maskChoice.select(maskChoice.getItemCount()-1);

		imp2.show();
	}

    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
		currImp.unlock();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Panel buttonPanel;
    private java.awt.Button exitButton;
    private java.awt.Label ccImageLabel;
    private java.awt.Checkbox darkPeaksCheckbox;
    private java.awt.Label filenameLabel;
    private java.awt.Panel imagePanel;
    private java.awt.Choice maskChoice;
    private java.awt.Label maskLabel;
    private java.awt.Panel maskPanel;
    private java.awt.Label minDistLabel;
    private java.awt.TextField minDistTextField;
    private java.awt.Label minDistUnitsLabel;
    private java.awt.Button okButton;
    private java.awt.Button openMaskButton;
    private java.awt.Label recomendLabel;
    private java.awt.Panel varsPanel;
    private java.awt.Label widthLabel;
    private java.awt.TextField widthTextField;
    private java.awt.Label widthUnitsLabel;
    private java.awt.Button widthButton;
    private java.awt.Button minDistButton;
    private java.awt.Label thresLabel;
    private java.awt.TextField thresTextField;
    private java.awt.Scrollbar thresScroll;
    private java.awt.Panel midPanel;
	private static final String strNONE = "Use selected ROI";
	private ImagePlus currImp;
	private ArrayList winIDList;
	private static int widthDefault = 10;				// Filter width
	private static double min_distDefault = 5.0;		// Min distance
	private static double thresDefault = 0.0;			// Threshold
	private static double thresPrecision = 10;
    // End of variables declaration//GEN-END:variables
}


class ITCN_Runner extends Thread {
	// Input paramaters with default values
	private int width;					// Filter width
	private double min_dist;			// Min distance
	private double threshold;			// Threshold
	private boolean darkPeaks;			// Select dark or light peaks
	private double sigma;				// Standard Deviation
	private double variance;			// Variance
	private ImagePlus impImage;				// ImagePlus for current image
	private ImageProcessor ip;				// ImageProcessor for current image
	private ImagePlus maskImp;				// ImagePlus for mask image (null if does not exist)
	private int maskID;						// ID for mask image
	private static String maskName = null;	// name for mask image

	private static final String strNONE = "Use selected ROI";

	double[] kernel;

	public ITCN_Runner(ImagePlus imp, int width, double min_dist, double threshold, boolean darkPeaks, ImagePlus maskImp) {
		this.impImage = imp;
		this.ip = imp.getProcessor();
		this.maskImp = maskImp;

		this.width = width;
		this.min_dist = min_dist;
		this.threshold = threshold;
		this.darkPeaks = darkPeaks;

		sigma = ((double)width-1.0)/3.0;
		variance = sigma*sigma;

		maskID = 0;
		maskName = null;

		start();
	}

/*
	public int setup(String arg, ImagePlus imp) {
		impImage = imp;
		return 1; //DOES_8G+SUPPORTS_MASKING;
	}
	*/

	public void run() {
		//ImagePlus impl;
		boolean inputs;
		double image[][];

		// Set ROI
		if (maskImp != null) {
			ip.resetRoi();
		}
		Rectangle r = ip.getRoi();

		// 2 Compute kernel
		IJ.showStatus("Finding Kernel");
		kernel=findKernal();

		// 3 Convolution
		IJ.showStatus("Convolution");
/*
		double[][] image1 = new double[4][4];
		int k=0;
		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				image1[i][j] = k++;
			}
		}

		image = filter2(image1,4,4, kernel, width, width);

		String kStr = new String();
		for (int j=0; j<width; j++) {
			for (int i=0; i<width; i++) {
				kStr += kernel[i+width*j]+" ";
			}
			kStr += "\n";
		}
		IJ.showMessage(kStr);

		String im1Str = new String();
		for (int j=0; j<4; j++) {
			for (int i=0; i<4; i++) {
				im1Str += image1[i][j]+" ";
			}
			im1Str += "\n";
		}
		IJ.showMessage(im1Str);

		String imStr = new String();
		for (int j=0; j<4; j++) {
			for (int i=0; i<4; i++) {
				imStr += image[i][j]+" ";
			}
			imStr += "\n";
		}
		IJ.showMessage(imStr);
*/

		image = filter2(ip,kernel,width,width);

		for (int i=0; i<ip.getWidth(); i++) {
			for (int j=0; j<ip.getHeight(); j++) {
				if (image[i][j]<threshold) image[i][j]=threshold;
				image[i][j] -= threshold;
			}
		}

		// 4 Find Maximum
		IJ.showStatus("Finding Maximums");

		// Create Mask
		ImageProcessor ipMask = null;
		int border = 1;
		boolean[][] mask = new boolean[r.width][r.height];

		if (maskImp != null) {
			ImageProcessor ipMask2 = maskImp.getProcessor();
			ipMask = ipMask2.duplicate();
		} else {
			if (impImage.getMask()!=null) {
				ipMask = (impImage.getMask()).duplicate();
			}
		}

		//IJ.showMessage("ok");
		//IJ.showMessage(ipMask.getPixelValue(5,5)+" ");

		// Get area
		int numPixels=0;
		if (maskImp==null) {
			// Process selected ROI
			for (int i=0; i<r.width; i++) {
				for (int j=0; j<r.height; j++) {
					if(!(ipMask!=null && ipMask.getPixelValue(i,j)==0)) {
						numPixels++;
					}
				}
			}
		} else {
			for (int i=0; i<r.width; i++) {
				for (int j=0; j<r.height; j++) {
					if(!(ipMask!=null && ipMask.getPixelValue(i,j)==0)) {
						numPixels++;
					}
				}
			}
		}

		// Get area if ROI selected.

		if (ipMask != null) {
			ipMask.dilate();
		}

		for (int i=0; i<r.width; i++) {
			for (int j=0; j<r.height; j++) {
				if((ipMask!=null && ipMask.getPixelValue(i,j)==0) || i<border || i>=r.width-border || j<border || j>=r.height-border) {
					mask[i][j]=false;
				} else {
					mask[i][j]=true;
				}
			}
		}

		// Local Maximum
		ArrayList peaks;
		peaks=find_local_max(image, r, Math.floor((double)width/3.0), min_dist, mask);

		// 5 Display results
		ImageProcessor ipCopy = (ip.duplicate()).convertToRGB();
		ImagePlus imp2 = new ImagePlus("Results "+impImage.getTitle(), ipCopy);

		ipCopy.setColor(java.awt.Color.red);
		ipCopy.setLineWidth(1);

		for(int i=0; i<peaks.size(); i++) {
			Point pt = (Point)peaks.get(i);

			ipCopy.drawDot(pt.x+r.x,pt.y+r.y);

			//IJ.write("Peak at: "+(pt.x+r.x)+" "+(pt.y+r.y)+" "+image[pt.x+r.x][pt.y+r.y]);
		}

		IJ.write("Image: " +impImage.getTitle());

		// Read units
		Calibration cali = impImage.getCalibration();

		DecimalFormat densityForm = new DecimalFormat("###0.00");

		if (cali == null) {
			IJ.write("Number of Cells: "+peaks.size());
		} else {
			IJ.write("Number of Cells: "+peaks.size()+" in "+densityForm.format((double)numPixels*cali.pixelHeight*cali.pixelWidth)+" square "+cali.getUnits());
			IJ.write("Density: "+densityForm.format((double)peaks.size()/((double)numPixels*cali.pixelHeight*cali.pixelWidth))+" cells per square "+cali.getUnit());
		}

		IJ.write(".........................................................................................");

		ipCopy.setColor(java.awt.Color.yellow);
		ipCopy.drawRect(r.x, r.y, r.width, r.height);

		imp2.show();

		return;

	}

	private double[] findKernal() {
		double[] hg = new double[width*width];
		double[] h = new double[width*width];
		double hgSum=0, hSum=0;
		double kSum=0, kProd=1;
		double bounds = ((double)width-1.0)/2.0;
		int index;

		index=0;
		for (double n1=-bounds; n1<=bounds; n1++) {
			for (double n2=-bounds; n2<=bounds; n2++) {
				hg[index]=Math.exp(-(n1*n1+n2*n2)/(2*variance));
				hgSum += hg[index];
				h[index]=(n1*n1+n2*n2-2*variance)*hg[index]/(variance*variance);  // v2 added
				hSum += h[index];
				index++;
			}
		}

		index=0;
		for (double n1=-bounds; n1<=bounds; n1++) {
			for (double n2=-bounds; n2<=bounds; n2++) {
				h[index] = (h[index] - hSum/(double)(width*width))/hgSum;
				index++;
			}
		}

		for (int i=0; i<width*width; i++) {
			kSum += h[i];
		}

		double kOffset = kSum/(width*width);
		for (int i=0; i<width*width; i++) {
			h[i] -= kOffset;
		}


		return h;
	}

	private double[][] filter2(double image[][], int width, int height, double[] kern, int kh, int kw) {
		double[][] dr = new double[width][height];

		for(int x=0; x<width; x++) {
			for (int y=0; y<height; y++)
			{
				dr[x][y] = 0;

				for (int i=0; i<kw; i++) {
					for (int j=0; j<kh ; j++) {

						try {
							if ((x+i-(kw-1)/2)>=0 && (x+i-(kw-1)/2)<width &&
								(y+j-(kw-1)/2)>=0 && (y+j-(kw-1)/2)<height) {
								dr[x][y] += kern[i+kw*j]*image[(x+i-(kw-1)/2)][(y+j-(kw-1)/2)];
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							IJ.showMessage("Array out of Bounds: ("+x+", "+y+") ("+i+", "+j+") ");
						}
					}
				}

			}
		}

		return dr;
	}

	private double[][] filter2(ImageProcessor ip, double[] kern, int kh, int kw) {
		int imgW = ip.getWidth();
		int imgH = ip.getHeight();

		Rectangle r = ip.getRoi();

		byte[] pixels = (byte[])ip.getPixels();
		double pix;
		double[][] dr = new double[imgW][imgH];

		for(int x=0; x<imgW; x++) {
			for (int y=0; y<imgH; y++)
			{
				dr[x][y] = 0;

				for (int i=0; i<kw; i++) {
					for (int j=0; j<kh ; j++) {

						try {
							if ((x+i-(kw-1)/2)>=0 && (x+i-(kw-1)/2)<imgW &&
								(y+j-(kw-1)/2)>=0 && (y+j-(kw-1)/2)<imgH) {

								if(darkPeaks)
									pix = (double)(0xff & pixels[(x+i-(kw-1)/2)+imgW*(y+j-(kw-1)/2)]);
								else
									pix = 255.0-(double)(0xff & pixels[(x+i-(kw-1)/2)+imgW*(y+j-(kw-1)/2)]);

								dr[x][y] += kern[i+kw*j]*pix;
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							IJ.showMessage("Array out of Bounds: ("+x+", "+y+") ("+i+", "+j+") ");
						}
					}
				}
				//IJ.write(dr[x][y]+" ");
			}
		}

		return dr;
	}

	private ArrayList find_local_max(double[][] image, Rectangle r, double epsilon, double min_dist, boolean[][] mask) {
		ArrayList ind_n = new ArrayList();
		ArrayList ind_n_ext = new ArrayList();

		// prepare neighborhood indices
		double n_dim = epsilon;

		for (double i=-n_dim; i<=n_dim; i++) {
			for (double j=-n_dim; j<=n_dim; j++) {
				if (i!=0 && j!=0 && ((i*i+j*j)<=epsilon*epsilon)) {
					ind_n.add(new Point((int)i,(int)j));
				}
			}
		}
		//int N_n = ind_n.size();

		// prepare extended neighborhood indices
		n_dim = min_dist;

		for (int i=(int)(-n_dim); i<=n_dim; i++) {
			for (int j=(int)(-n_dim); j<=n_dim; j++) {
				if ((i*i+j*j)<=min_dist*min_dist) {
					ind_n_ext.add(new Point((int)i,(int)j));
				}
			}
		}
		//int N_n_ext = ind_n_ext.size();

		ArrayList peaks = new ArrayList();


		double minimum = 0;
		while(true) {
			double maximum=minimum;
			int x=0,y=0;
			//int mx=0, my=0;

			for (int i=0; i<r.width; i++) {
				for (int j=0; j<r.height; j++) {
					if ((image[i+r.x][j+r.y]>maximum) && mask[i][j]) {
						maximum=image[i+r.x][j+r.y];
						x=i;  y=j;
					}
				}
			}

			if (maximum==minimum)
				break;

			// Verify it is a maximum
			boolean flag=true;
			for (int i=0; i<ind_n.size(); i++) {

				if(!flag) break;

				Point ind_nPt = (Point)ind_n.get(i);
				int nx = x+ind_nPt.x;
				int ny = y+ind_nPt.y;

				try {
					flag = flag && (maximum >= image[nx+r.x][ny+r.y]);
				} catch (ArrayIndexOutOfBoundsException e) {
				}
			}

			if (flag) {
				peaks.add(new Point(x,y));
			} else {
				mask[x][y]=false;
			}

			for (int i=0; i<ind_n_ext.size(); i++) {
				Point ind_n_extPt = (Point)ind_n_ext.get(i);
				int nx = x+ind_n_extPt.x;
				int ny = y+ind_n_extPt.y;

				if (nx >= 0 && nx < r.width && ny >= 0 && ny < r.height) {
					mask[nx][ny]=false;
				}
			}

		}

		return peaks;
	}
}