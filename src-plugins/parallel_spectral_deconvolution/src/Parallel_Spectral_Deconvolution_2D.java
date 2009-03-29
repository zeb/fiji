/*
 *  Copyright 2008 Piotr Wendykier
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import ij.IJ;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cern.colt.Arrays;
import cern.colt.Timer;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import edu.emory.mathcs.restoretools.spectral.DoubleGTikDCT_2D;
import edu.emory.mathcs.restoretools.spectral.DoubleGTikFFT_2D;
import edu.emory.mathcs.restoretools.spectral.DoubleTikDCT_2D;
import edu.emory.mathcs.restoretools.spectral.DoubleTikFFT_2D;
import edu.emory.mathcs.restoretools.spectral.DoubleTsvdDCT_2D;
import edu.emory.mathcs.restoretools.spectral.DoubleTsvdFFT_2D;
import edu.emory.mathcs.restoretools.spectral.FloatGTikDCT_2D;
import edu.emory.mathcs.restoretools.spectral.FloatGTikFFT_2D;
import edu.emory.mathcs.restoretools.spectral.FloatTikDCT_2D;
import edu.emory.mathcs.restoretools.spectral.FloatTikFFT_2D;
import edu.emory.mathcs.restoretools.spectral.FloatTsvdDCT_2D;
import edu.emory.mathcs.restoretools.spectral.FloatTsvdFFT_2D;
import edu.emory.mathcs.restoretools.spectral.OutputType;
import edu.emory.mathcs.restoretools.spectral.ResizingType;
import edu.emory.mathcs.restoretools.utils.DoubleSlider;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Parallel Spectral Deconvolution 2D GUI
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class Parallel_Spectral_Deconvolution_2D implements PlugIn, ImageListener {
	
	/**
	 * Method used to call the plugin from a macro.
	 * 
	 * @param pathToBlurredImage
	 * @param pathToPsf
	 * @param pathToDeblurredImage
	 * @param methodStr
	 * @param stencilStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param thresholdStr
	 * @param regParamStr
	 * @param nOfThreadsStr
	 * @param showPaddedStr
	 * @return path to deblurred image or error message
	 */
	public static String deconvolve(String pathToBlurredImage, String pathToPsf, String pathToDeblurredImage, String methodStr, String stencilStr, String resizingStr, String outputStr, String precisionStr, String thresholdStr, String regParamStr, String nOfThreadsStr, String showPaddedStr) {
		boolean showPadded;
		double threshold, regParam;
		int nOfThreads;
		Method method = null;
		String stencil = null;
		ResizingType resizing = null;
		OutputType output = null;
		Precision precision = null;
		ImagePlus imX = null;
		ImagePlus imB = IJ.openImage(pathToBlurredImage);
		if (imB == null) {
			return "Cannot open image " + pathToBlurredImage;
		}
		ImagePlus imPSF = IJ.openImage(pathToPsf);
		if (imPSF == null) {
			return "Cannot open image " + pathToPsf;
		}
		ImageProcessor ipB = imB.getProcessor();
		if (ipB instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imB.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D";
		}
		ImageProcessor ipPSF = imPSF.getProcessor();
		if (ipPSF instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imPSF.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D.";
		}
		for (Method elem : Method.values()) {
			if (elem.toString().equals(methodStr)) {
				method = elem;
				break;
			}
		}
		if (method == null) {
			return "method must be in " + Arrays.toString(Method.values());
		}
		for (ResizingType elem : ResizingType.values()) {
			if (elem.toString().equals(resizingStr)) {
				resizing = elem;
				break;
			}
		}
		if (resizing == null) {
			return "resizing must be in " + Arrays.toString(ResizingType.values());
		}
		for (OutputType elem : OutputType.values()) {
			if (elem.toString().equals(outputStr)) {
				output = elem;
				break;
			}
		}
		if (output == null) {
			return "output must be in " + Arrays.toString(OutputType.values());
		}
		for (Stencil elem : Stencil.values()) {
			if (elem.toString().equals(stencilStr)) {
				stencil = elem.toString();
				break;
			}
		}
		if (stencil == null) {
			return "stencil must be in " + Arrays.toString(Stencil.values());
		}
		for (Precision elem : Precision.values()) {
			if (elem.toString().equals(precisionStr)) {
				precision = elem;
				break;
			}
		}
		if (precision == null) {
			return "precision must be in " + Arrays.toString(Precision.values());
		}
		try {
			threshold = Double.parseDouble(thresholdStr);
		} catch (Exception ex) {
			return "threshold must be a nonnegative number or -1 to disable";
		}
		if ((threshold != -1) && (threshold < 0)) {
			return "threshold must be a nonnegative number or -1 to disable";
		}
		try {
			regParam = Double.parseDouble(regParamStr);
		} catch (Exception ex) {
			return "regParam must be a nonnegative number or -1 for auto";
		}
		if ((regParam != -1) && (regParam < 0)) {
			return "regParam must be a nonnegative number or -1 for auto";
		}
		try {
			nOfThreads = Integer.parseInt(nOfThreadsStr);
		} catch (Exception ex) {
			return "nOfThreads must be power of 2";
		}
		if (nOfThreads < 1) {
			return "nOfThreads must be power of 2";
		}
		if (!ConcurrencyUtils.isPowerOf2(nOfThreads)) {
			return "nOfThreads must be power of 2";
		}
		try {
			showPadded = Boolean.parseBoolean(showPaddedStr);
		} catch (Exception ex) {
			return "showPadded must be a boolean value (true or false)";
		}
		ConcurrencyUtils.setNumberOfProcessors(nOfThreads);
		switch (precision) {
		case DOUBLE:
			switch (method) {
			case GTIK_REFLEXIVE:
				DoubleGTikDCT_2D dgtik_dct = new DoubleGTikDCT_2D(imB, imPSF, DoubleStencil.valueOf(stencil).stencil, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dgtik_dct.deblur(threshold);
				} else {
					imX = dgtik_dct.deblur(regParam, threshold);
				}
				break;
			case GTIK_PERIODIC:
				DoubleGTikFFT_2D dgtik_fft = new DoubleGTikFFT_2D(imB, imPSF, DoubleStencil.valueOf(stencil).stencil, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dgtik_fft.deblur(threshold);
				} else {
					imX = dgtik_fft.deblur(regParam, threshold);
				}
				break;
			case TIK_REFLEXIVE:
				DoubleTikDCT_2D dtik_dct = new DoubleTikDCT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtik_dct.deblur(threshold);
				} else {
					imX = dtik_dct.deblur(regParam, threshold);
				}
				break;
			case TIK_PERIODIC:
				DoubleTikFFT_2D dtik_fft = new DoubleTikFFT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtik_fft.deblur(threshold);
				} else {
					imX = dtik_fft.deblur(regParam, threshold);
				}
				break;
			case TSVD_REFLEXIVE:
				DoubleTsvdDCT_2D dtsvd_dct = new DoubleTsvdDCT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtsvd_dct.deblur(threshold);
				} else {
					imX = dtsvd_dct.deblur(regParam, threshold);
				}
				break;
			case TSVD_PERIODIC:
				DoubleTsvdFFT_2D dtsvd_fft = new DoubleTsvdFFT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtsvd_fft.deblur(threshold);
				} else {
					imX = dtsvd_fft.deblur(regParam, threshold);
				}
				break;
			}
			break;
		case SINGLE:
			switch (method) {
			case GTIK_REFLEXIVE:
				FloatGTikDCT_2D dgtik_dct = new FloatGTikDCT_2D(imB, imPSF, FloatStencil.valueOf(stencil).stencil, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dgtik_dct.deblur((float) threshold);
				} else {
					imX = dgtik_dct.deblur((float) regParam, (float) threshold);
				}
				break;
			case GTIK_PERIODIC:
				FloatGTikFFT_2D dgtik_fft = new FloatGTikFFT_2D(imB, imPSF, FloatStencil.valueOf(stencil).stencil, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dgtik_fft.deblur((float) threshold);
				} else {
					imX = dgtik_fft.deblur((float) regParam, (float) threshold);
				}
				break;
			case TIK_REFLEXIVE:
				FloatTikDCT_2D dtik_dct = new FloatTikDCT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtik_dct.deblur((float) threshold);
				} else {
					imX = dtik_dct.deblur((float) regParam, (float) threshold);
				}
				break;
			case TIK_PERIODIC:
				FloatTikFFT_2D dtik_fft = new FloatTikFFT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtik_fft.deblur((float) threshold);
				} else {
					imX = dtik_fft.deblur((float) regParam, (float) threshold);
				}
				break;
			case TSVD_REFLEXIVE:
				FloatTsvdDCT_2D dtsvd_dct = new FloatTsvdDCT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtsvd_dct.deblur((float) threshold);
				} else {
					imX = dtsvd_dct.deblur((float) regParam, (float) threshold);
				}
				break;
			case TSVD_PERIODIC:
				FloatTsvdFFT_2D dtsvd_fft = new FloatTsvdFFT_2D(imB, imPSF, resizing, output, showPadded);
				if (regParam == -1) {
					imX = dtsvd_fft.deblur((float) threshold);
				} else {
					imX = dtsvd_fft.deblur((float) regParam, (float) threshold);
				}
				break;
			}
			break;
		}
		IJ.save(imX, pathToDeblurredImage);
		return pathToDeblurredImage;
	}

	private final static String version = "1.8";

	private enum Method {
		GTIK_REFLEXIVE, GTIK_PERIODIC, TIK_REFLEXIVE, TIK_PERIODIC, TSVD_REFLEXIVE, TSVD_PERIODIC
	};

	private final String[] methodNames = { "Generalized Tikhonov (reflexive)", "Generalized Tikhonov (periodic)", "Tikhonov (reflexive)", "Tikhonov (periodic)", "Truncated SVD (reflexive)", "Truncated SVD (periodic)" };

	private final String[] methodShortNames = { "gtik_ref", "gtik_per", "tik_ref", "tik_per", "tsvd_tef", "tsvd_per" };

	private enum Precision {
		SINGLE, DOUBLE
	};

	private enum Stencil {
		IDENTITY, FIRST_DERIVATIVE_COLUMNS, SECOND_DERIVATIVE_COLUMNS, FIRST_DERIVATIVE_ROWS, SECOND_DERIVATIVE_ROWS, LAPLACIAN
	};

	private enum DoubleStencil {
		IDENTITY(new DenseDoubleMatrix2D(3, 3).assign(new double[] { 0, 0, 0, 0, 1, 0, 0, 0, 0 })), FIRST_DERIVATIVE_COLUMNS(new DenseDoubleMatrix2D(3, 3).assign(new double[] { 0, 1, 0, 0, -1, 0, 0, 0, 0 })), SECOND_DERIVATIVE_COLUMNS(new DenseDoubleMatrix2D(3, 3).assign(new double[] { 0, 1, 0, 0,
				-2, 0, 0, 1, 0 })), FIRST_DERIVATIVE_ROWS(new DenseDoubleMatrix2D(3, 3).assign(new double[] { 0, 0, 0, 1, -1, 0, 0, 0, 0 })), SECOND_DERIVATIVE_ROWS(new DenseDoubleMatrix2D(3, 3).assign(new double[] { 0, 0, 0, 1, -2, 1, 0, 0, 0 })), LAPLACIAN(new DenseDoubleMatrix2D(3, 3)
				.assign(new double[] { 0, 1, 0, 1, -4, 1, 0, 1, 0 }));

		private final DoubleMatrix2D stencil;

		private DoubleStencil(DoubleMatrix2D stencil) {
			this.stencil = stencil;
		}
	};

	private enum FloatStencil {
		IDENTITY(new DenseFloatMatrix2D(3, 3).assign(new float[] { 0, 0, 0, 0, 1, 0, 0, 0, 0 })), FIRST_DERIVATIVE_COLUMN(new DenseFloatMatrix2D(3, 3).assign(new float[] { 0, 1, 0, 0, -1, 0, 0, 0, 0 })), SECOND_DERIVATIVE_COLUMNS(new DenseFloatMatrix2D(3, 3).assign(new float[] { 0, 1, 0, 0, -2, 0,
				0, 1, 0 })), FIRST_DERIVATIVE_ROWS(new DenseFloatMatrix2D(3, 3).assign(new float[] { 0, 0, 0, 1, -1, 0, 0, 0, 0 })), SECOND_DERIVATIVE_ROWS(new DenseFloatMatrix2D(3, 3).assign(new float[] { 0, 0, 0, 1, -2, 1, 0, 0, 0 })), LAPLACIAN(new DenseFloatMatrix2D(3, 3).assign(new float[] {
				0, 1, 0, 1, -4, 1, 0, 1, 0 }));

		private final FloatMatrix2D stencil;

		private FloatStencil(FloatMatrix2D stencil) {
			this.stencil = stencil;
		}
	};

	private final String[] stencilNames = { "Identity", "First derivative (columns)", "Second derivative (columns)", "First derivative (rows)", "Second Derivative (rows)", "Laplacian" };

	private static final String[] resizingNames = { "None", "Next power of two" };

	private static final String[] outputNames = { "Same as source", "Byte (8-bit)", "Short (16-bit)", "Float (32-bit)" };

	private final String[] stencilShortNames = { "identity", "first_deriv_cols", "second_deriv_cols", "first_deriv_rows", "second_deriv_rows", "laplacian" };

	private final String[] precisionNames = { "Single", "Double" };

	private JFrame mainPanelFrame;

	private DoubleGTikDCT_2D dgtik_dct;

	private DoubleGTikFFT_2D dgtik_fft;

	private DoubleTikFFT_2D dtik_fft;

	private DoubleTikDCT_2D dtik_dct;

	private DoubleTsvdFFT_2D dtsvd_fft;

	private DoubleTsvdDCT_2D dtsvd_dct;

	private FloatGTikDCT_2D fgtik_dct;

	private FloatGTikFFT_2D fgtik_fft;

	private FloatTikFFT_2D ftik_fft;

	private FloatTikDCT_2D ftik_dct;

	private FloatTsvdFFT_2D ftsvd_fft;

	private FloatTsvdDCT_2D ftsvd_dct;

	private ImagePlus imB, imPSF, imX;

	private int[] windowIDs;

	private String[] imageTitles;

	private String oldImageTitle = "";

	private String oldPSFTitle = "";

	private int oldMethodIndex = -1;

	private int oldResizingIndex = -1;

	private int oldOutputIndex = -1;

	private int oldStencilIndex = -1;

	private int oldPrecisionIndex = -1;

	private int oldNumberThreads = -1;

	private JComboBox blurChoice, psfChoice, methodChoice, stencilChoice, resizingChoice, outputChoice, precisionChoice;

	private DoubleSlider regSlider;

	private JTextField regField, threadsField, thresholdField;

	private JCheckBox regCheck, paddedCheck, thresholdCheck;

	private JButton deconvolveButton, updateButton, cancelButton;

	private ImageListener getImageListener() {
		return this;
	}

	public void run(String arg) {
		if (IJ.versionLessThan("1.35l")) {
			IJ.showMessage("This plugin requires ImageJ 1.35l+");
			return;
		}

		if (!IJ.isJava15()) {
			IJ.showMessage("This plugin requires Sun Java 1.5+");
			return;
		}
		WindowManager.checkForDuplicateName = true;
		ImagePlus.addImageListener(this);
		final MainPanel panel = new MainPanel();
		panel.init();
		mainPanelFrame = new JFrame("Parallel Spectral Deconvolution 2D " + version + " ");
		mainPanelFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainPanelFrame.add(panel);
		mainPanelFrame.setResizable(false);
		mainPanelFrame.setSize(panel.totalSize);
		mainPanelFrame.pack();
		mainPanelFrame.setLocationRelativeTo(null);
		mainPanelFrame.setVisible(true);

	}

	public void imageClosed(ImagePlus imp) {
		blurChoice.removeItem(imp.getTitle());
		blurChoice.revalidate();
		psfChoice.removeItem(imp.getTitle());
		psfChoice.revalidate();
		if ((imX != null) && (updateButton.isEnabled())) {
			if (imp.getTitle().equals(imX.getTitle())) {
				cleanOldData();
				updateButton.setEnabled(false);
				regCheck.setSelected(true);

			}
		}
	}

	public void imageOpened(ImagePlus imp) {
		blurChoice.addItem(imp.getTitle());
		blurChoice.revalidate();
		psfChoice.addItem(imp.getTitle());
		psfChoice.revalidate();

	}

	public void imageUpdated(ImagePlus imp) {
	}

	private void cleanOldData() {
		dgtik_dct = null;
		dgtik_fft = null;
		dtik_dct = null;
		dtik_fft = null;
		dtsvd_dct = null;
		dtsvd_fft = null;
		fgtik_dct = null;
		fgtik_fft = null;
		ftik_dct = null;
		ftik_fft = null;
		ftsvd_dct = null;
		ftsvd_fft = null;
		System.gc();
	}

	private void cleanAll() {
		cleanOldData();
		imB = null;
		imPSF = null;
		imX = null;
		windowIDs = null;
		imageTitles = null;
		System.gc();
	}

	private class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

		public void uncaughtException(Thread t, Throwable e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			pw.flush();
			sw.flush();
			IJ.log(sw.toString());
		}

	}

	private class MainPanel extends JPanel {

		private static final long serialVersionUID = 3975356344081858245L;

		private final Dimension totalSize = new Dimension();

		private final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

		private final Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);

		public MainPanel() {
			totalSize.height = 280;
			totalSize.width = 450;
		}

		private void init() {
			Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
			windowIDs = WindowManager.getIDList();
			if (windowIDs != null) {
				imageTitles = new String[windowIDs.length];
				for (int i = 0; i < windowIDs.length; i++) {
					ImagePlus im = WindowManager.getImage(windowIDs[i]);
					if (im != null)
						imageTitles[i] = im.getTitle();
					else
						imageTitles[i] = "";
				}
			}
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			// --------------------------------------------------------------
			JPanel blurPanel = new JPanel();
			blurPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel blurLabel = new JLabel("Image:");
			blurLabel.setPreferredSize(new Dimension(60, blurLabel.getPreferredSize().height));
			blurPanel.add(blurLabel);
			if (windowIDs != null) {
				blurChoice = new JComboBox(imageTitles);
				blurChoice.setSelectedIndex(0);
			} else {
				blurChoice = new JComboBox();
			}
			blurChoice.setPreferredSize(new Dimension(378, blurChoice.getPreferredSize().height));
			blurChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			blurChoice.addActionListener(new BlurChoiceActionListener());
			blurPanel.add(blurChoice);
			// --------------------------------------------------------------
			JPanel psfPanel = new JPanel();
			psfPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel psfLabel = new JLabel("PSF:");
			psfLabel.setPreferredSize(new Dimension(60, psfLabel.getPreferredSize().height));
			psfPanel.add(psfLabel);
			if (windowIDs != null) {
				psfChoice = new JComboBox(imageTitles);
			} else {
				psfChoice = new JComboBox();
			}
			psfChoice.setPreferredSize(new Dimension(378, psfChoice.getPreferredSize().height));
			if (windowIDs != null) {
				if (windowIDs.length > 1) {
					psfChoice.setSelectedIndex(1);
				} else {
					psfChoice.setSelectedIndex(0);
				}
			}
			psfChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			psfChoice.addActionListener(new PsfChoiceActionListener());
			psfPanel.add(psfChoice);
			// --------------------------------------------------------------
			JPanel methodPanel = new JPanel();
			methodPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel methodLabel = new JLabel("Method:");
			methodLabel.setPreferredSize(new Dimension(60, methodLabel.getPreferredSize().height));
			methodPanel.add(methodLabel);
			methodChoice = new JComboBox(methodNames);
			methodChoice.setSelectedIndex(0);
			methodChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			methodChoice.addActionListener(new MethodChoiceActionListener());
			methodPanel.add(methodChoice);
			// --------------------------------------------------------------
			JPanel stencilPanel = new JPanel();
			stencilPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel stencilLabel = new JLabel("Stencil:");
			stencilLabel.setPreferredSize(new Dimension(60, stencilLabel.getPreferredSize().height));
			stencilPanel.add(stencilLabel);
			stencilChoice = new JComboBox(stencilNames);
			stencilChoice.setSelectedIndex(0);
			stencilChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			stencilChoice.addActionListener(new StencilChoiceActionListener());
			stencilPanel.add(stencilChoice);
			// --------------------------------------------------------------
			JPanel resizingPanel = new JPanel();
			resizingPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel resizingLabel = new JLabel("Resizing:");
			resizingLabel.setPreferredSize(new Dimension(60, resizingLabel.getPreferredSize().height));
			resizingPanel.add(resizingLabel);
			resizingChoice = new JComboBox(resizingNames);
			resizingChoice.setSelectedIndex(0);
			resizingChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			resizingChoice.setToolTipText("<html>Choose resizing.</html>");
			resizingChoice.addActionListener(new ResizingChoiceActionListener());
			resizingPanel.add(resizingChoice);
			// --------------------------------------------------------------
			JPanel outputPanel = new JPanel();
			outputPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel outputLabel = new JLabel("Output:");
			outputLabel.setPreferredSize(new Dimension(60, outputLabel.getPreferredSize().height));
			outputPanel.add(outputLabel);
			outputChoice = new JComboBox(outputNames);
			outputChoice.setSelectedIndex(0);
			outputChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			outputChoice.setToolTipText("<html>Choose format of deblurred image.</html>");
			outputChoice.addActionListener(new OutputChoiceActionListener());
			outputPanel.add(outputChoice);
			// --------------------------------------------------------------
			JPanel precisionPanel = new JPanel();
			precisionPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel precisionLabel = new JLabel("Precision:");
			precisionLabel.setPreferredSize(new Dimension(60, precisionLabel.getPreferredSize().height));
			precisionPanel.add(precisionLabel);
			precisionChoice = new JComboBox(precisionNames);
			precisionChoice.setSelectedIndex(0);
			precisionChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			precisionChoice.addActionListener(new PrecisionChoiceActionListener());
			precisionPanel.add(precisionChoice);
			thresholdCheck = new JCheckBox("Threshold:  ");
			thresholdCheck.setSelected(true);
			thresholdCheck.addItemListener(new ThresholdCheckItemListener());
			precisionPanel.add(thresholdCheck);
			thresholdField = new JTextField("0.0", 6);
			thresholdField.setEnabled(true);
			thresholdField.addActionListener(new ThresholdFieldActionListener());
			precisionPanel.add(thresholdField);

			// --------------------------------------------------------------
			JPanel regPanel = new JPanel();
			regPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel regLabel = new JLabel("Regularization parameter:  ");
			regPanel.add(regLabel);
			regField = new JTextField("0.0", 6);
			regField.setEnabled(false);
			regField.addActionListener(new RegFieldActionListener());
			regPanel.add(regField);
			regSlider = new DoubleSlider(0, 0, 0, 1, 6);
			regSlider.setPaintLabels(false);
			regSlider.setEnabled(false);
			regSlider.addChangeListener(new RegSliderChangeListener());
			regPanel.add(regSlider);
			// --------------------------------------------------------------
			JPanel threadsPanel = new JPanel();
			threadsPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel threadsLabel = new JLabel("Number of threads (power of 2):  ");
			threadsPanel.add(threadsLabel);
			ConcurrencyUtils.setNumberOfProcessors(ConcurrencyUtils.concurrency());
			threadsField = new JTextField(Integer.toString(ConcurrencyUtils.getNumberOfProcessors()), 3);
			oldNumberThreads = ConcurrencyUtils.getNumberOfProcessors();
			threadsField.addActionListener(new ThreadsFieldActionListener());
			threadsPanel.add(threadsField);
			// --------------------------------------------------------------
			JPanel checkPanel = new JPanel();
			checkPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			regCheck = new JCheckBox("Auto regularization parameter");
			regCheck.setSelected(true);
			regCheck.addItemListener(new RegCheckItemListener());
			checkPanel.add(regCheck);
			paddedCheck = new JCheckBox("Show padded image");
			paddedCheck.setSelected(false);
			checkPanel.add(paddedCheck);
			// --------------------------------------------------------------
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			deconvolveButton = new JButton("Deconvolve");
			deconvolveButton.addActionListener(new DeconvolveButtonActionListener());
			if (windowIDs == null) {
				deconvolveButton.setEnabled(false);
			}
			buttonPanel.add(deconvolveButton);
			updateButton = new JButton("Update");
			updateButton.setEnabled(false);
			updateButton.addActionListener(new UpdateButtonActionListener());
			buttonPanel.add(updateButton);
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(cancelButton);
			// --------------------------------------------------------------
			add(blurPanel);
			add(psfPanel);
			add(methodPanel);
			add(stencilPanel);
			add(resizingPanel);
			add(outputPanel);
			add(precisionPanel);
			add(regPanel);
			add(threadsPanel);
			add(checkPanel);
			add(buttonPanel);
			validate();
		}

		private class BlurChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				windowIDs = WindowManager.getIDList();
				if (windowIDs != null) {
					deconvolveButton.setEnabled(true);
					if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
							&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!imX.getWindow().isClosed()))) {
						updateButton.setEnabled(true);
						regCheck.setSelected(false);
					} else {
						updateButton.setEnabled(false);
						regCheck.setSelected(true);
					}
				} else {
					deconvolveButton.setEnabled(false);
				}
			}
		}

		private class PsfChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				windowIDs = WindowManager.getIDList();
				if (windowIDs != null) {
					if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
							&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!imX.getWindow().isClosed()))) {
						updateButton.setEnabled(true);
						regCheck.setSelected(false);
					} else {
						updateButton.setEnabled(false);
						regCheck.setSelected(true);
					}
				} else {
					deconvolveButton.setEnabled(false);
				}
			}
		}

		private class MethodChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				Method selMethod = Method.values()[methodChoice.getSelectedIndex()];
				switch (selMethod) {
				case GTIK_REFLEXIVE:
					stencilChoice.setEnabled(true);
					break;
				case GTIK_PERIODIC:
					stencilChoice.setEnabled(true);
					break;
				case TIK_REFLEXIVE:
					stencilChoice.setEnabled(false);
					break;
				case TIK_PERIODIC:
					stencilChoice.setEnabled(false);
					break;
				case TSVD_REFLEXIVE:
					stencilChoice.setEnabled(false);
					break;
				case TSVD_PERIODIC:
					stencilChoice.setEnabled(false);
					break;
				}
				if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
						&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!imX.getWindow().isClosed()))) {
					updateButton.setEnabled(true);
					regCheck.setSelected(false);
				} else {
					updateButton.setEnabled(false);
					regCheck.setSelected(true);
				}
			}
		}

		private class StencilChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
						&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (!imX.getWindow().isClosed()))) {
					updateButton.setEnabled(true);
					regCheck.setSelected(false);
				} else {
					updateButton.setEnabled(false);
					regCheck.setSelected(true);
				}

			}
		}

		private class ResizingChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
						&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!imX.getWindow().isClosed()))) {
					updateButton.setEnabled(true);
					regCheck.setSelected(false);
				} else {
					updateButton.setEnabled(false);
					regCheck.setSelected(true);
				}

			}
		}

		private class OutputChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
						&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!imX.getWindow().isClosed()))) {
					updateButton.setEnabled(true);
					regCheck.setSelected(false);
				} else {
					updateButton.setEnabled(false);
					regCheck.setSelected(true);
				}

			}
		}

		private class PrecisionChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
						&& (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!imX.getWindow().isClosed()))) {
					updateButton.setEnabled(true);
					regCheck.setSelected(false);
				} else {
					updateButton.setEnabled(false);
					regCheck.setSelected(true);
				}

			}
		}

		private class ThresholdFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				checkThresholdFieldText();
			}
		}

		private class RegFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (checkRegFieldText()) {
					regSlider.setValue(Double.parseDouble(regField.getText()));
				}
			}
		}

		private class ThreadsFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (checkThreadsFieldText()) {
					int val = Integer.parseInt(threadsField.getText());
					ConcurrencyUtils.setNumberOfProcessors(val);
					if (oldNumberThreads != val) {
						if (updateButton.isEnabled()) {
							updateButton.setEnabled(false);
						}
					} else {
						if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex())
								&& (oldOutputIndex == outputChoice.getSelectedIndex()) && (oldStencilIndex == stencilChoice.getSelectedIndex() && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (imX.getWindow() != null) && (!imX.getWindow().isClosed()))) {
							updateButton.setEnabled(true);
						}
					}
				}
			}
		}

		private class RegSliderChangeListener implements ChangeListener {
			public void stateChanged(ChangeEvent e) {
				double val = regSlider.getDoubleValue();
				regField.setText(Double.toString(val));
			}
		}

		private class ThresholdCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (thresholdCheck.isSelected()) {
					thresholdField.setEnabled(true);
				} else {
					thresholdField.setEnabled(false);
				}
			}
		}

		private class RegCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (regCheck.isSelected()) {
					regField.setEnabled(false);
					regSlider.setEnabled(false);
					updateButton.setEnabled(false);

				} else {
					regField.setEnabled(true);
					regSlider.setEnabled(true);
					if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex()) && (oldOutputIndex == outputChoice.getSelectedIndex())
							&& (oldStencilIndex == stencilChoice.getSelectedIndex()) && (oldPrecisionIndex == precisionChoice.getSelectedIndex())) {
						updateButton.setEnabled(true);
					}
				}
			}
		}

		private class DeconvolveButtonActionListener implements ActionListener {
			private final Timer timer = new Timer();

			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread(new Runnable() {
					public void run() {
						imB = WindowManager.getImage((String) blurChoice.getSelectedItem());
						ImageProcessor ipB = imB.getProcessor();
						if (ipB instanceof ColorProcessor) {
							IJ.showMessage("RGB images are not currently supported.");
							return;
						}
						if (imB.getStackSize() > 1) {
							IJ.showMessage("For 3D images use Parallel Spectral Deconvolve 3D.");
							return;
						}
						imPSF = WindowManager.getImage((String) psfChoice.getSelectedItem());
						ImageProcessor ipPSF = imPSF.getProcessor();
						if (ipPSF instanceof ColorProcessor) {
							IJ.showMessage("RGB images are not currently supported.");
							return;
						}
						if (imPSF.getStackSize() > 1) {
							IJ.showMessage("For 3D images use Parallel Spectral Deconvolve 3D.");
							return;
						}
						if (!checkRegFieldText())
							return;
						if (!checkThresholdFieldText())
							return;
						if (!checkThreadsFieldText())
							return;

						oldNumberThreads = Integer.parseInt(threadsField.getText());
						oldImageTitle = (String) blurChoice.getSelectedItem();
						oldPSFTitle = (String) psfChoice.getSelectedItem();
						oldMethodIndex = methodChoice.getSelectedIndex();
						oldResizingIndex = resizingChoice.getSelectedIndex();
						oldOutputIndex = outputChoice.getSelectedIndex();
						oldStencilIndex = stencilChoice.getSelectedIndex();
						oldPrecisionIndex = precisionChoice.getSelectedIndex();
						setCursor(waitCursor);
						deconvolveButton.setEnabled(false);
						updateButton.setEnabled(false);
						cancelButton.setEnabled(false);
						ConcurrencyUtils.setNumberOfProcessors(oldNumberThreads);
						Method selMethod = Method.values()[methodChoice.getSelectedIndex()];
						Precision selPrecision = Precision.values()[precisionChoice.getSelectedIndex()];
						double threshold = -1.0;
						if (thresholdCheck.isSelected()) {
							threshold = Double.parseDouble(thresholdField.getText());
						}
						cleanOldData();
						timer.reset().start();
						switch (selPrecision) {
						case DOUBLE:
							switch (selMethod) {
							case GTIK_REFLEXIVE:
								dgtik_dct = new DoubleGTikDCT_2D(imB, imPSF, DoubleStencil.values()[stencilChoice.getSelectedIndex()].stencil, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = dgtik_dct.deblur(threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Double) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = dgtik_dct.deblur(Double.parseDouble(regField.getText()), threshold);
									timer.stop();
								}
								break;
							case GTIK_PERIODIC:
								dgtik_fft = new DoubleGTikFFT_2D(imB, imPSF, DoubleStencil.values()[stencilChoice.getSelectedIndex()].stencil, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = dgtik_fft.deblur(threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Double) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = dgtik_fft.deblur(Double.parseDouble(regField.getText()), threshold);
									timer.stop();
								}
								break;
							case TIK_REFLEXIVE:
								dtik_dct = new DoubleTikDCT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = dtik_dct.deblur(threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Double) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = dtik_dct.deblur(Double.parseDouble(regField.getText()), threshold);
									timer.stop();
								}
								break;
							case TIK_PERIODIC:
								dtik_fft = new DoubleTikFFT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = dtik_fft.deblur(threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Double) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = dtik_fft.deblur(Double.parseDouble(regField.getText()), threshold);
									timer.stop();
								}
								break;
							case TSVD_REFLEXIVE:
								dtsvd_dct = new DoubleTsvdDCT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = dtsvd_dct.deblur(threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Double) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = dtsvd_dct.deblur(Double.parseDouble(regField.getText()), threshold);
									timer.stop();
								}
								break;
							case TSVD_PERIODIC:
								dtsvd_fft = new DoubleTsvdFFT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = dtsvd_fft.deblur(threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Double) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = dtsvd_fft.deblur(Double.parseDouble(regField.getText()), threshold);
									timer.stop();
								}
								break;
							}
							break;
						case SINGLE:
							switch (selMethod) {
							case GTIK_REFLEXIVE:
								fgtik_dct = new FloatGTikDCT_2D(imB, imPSF, FloatStencil.values()[stencilChoice.getSelectedIndex()].stencil, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = fgtik_dct.deblur((float) threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Float) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = fgtik_dct.deblur(Float.parseFloat(regField.getText()), (float) threshold);
									timer.stop();
								}
								break;
							case GTIK_PERIODIC:
								fgtik_fft = new FloatGTikFFT_2D(imB, imPSF, FloatStencil.values()[stencilChoice.getSelectedIndex()].stencil, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = fgtik_fft.deblur((float) threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Float) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = fgtik_fft.deblur(Float.parseFloat(regField.getText()), (float) threshold);
									timer.stop();
								}
								break;
							case TIK_REFLEXIVE:
								ftik_dct = new FloatTikDCT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = ftik_dct.deblur((float) threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Float) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = ftik_dct.deblur(Float.parseFloat(regField.getText()), (float) threshold);
									timer.stop();
								}
								break;
							case TIK_PERIODIC:
								ftik_fft = new FloatTikFFT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = ftik_fft.deblur((float) threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Float) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = ftik_fft.deblur(Float.parseFloat(regField.getText()), (float) threshold);
									timer.stop();
								}
								break;
							case TSVD_REFLEXIVE:
								ftsvd_dct = new FloatTsvdDCT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = ftsvd_dct.deblur((float) threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Float) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = ftsvd_dct.deblur(Float.parseFloat(regField.getText()), (float) threshold);
									timer.stop();
								}
								break;
							case TSVD_PERIODIC:
								ftsvd_fft = new FloatTsvdFFT_2D(imB, imPSF, ResizingType.values()[resizingChoice.getSelectedIndex()], OutputType.values()[outputChoice.getSelectedIndex()], paddedCheck.isSelected());
								if (regCheck.isSelected()) {
									imX = ftsvd_fft.deblur((float) threshold);
									timer.stop();
									regField.setText(String.format("%.6f", imX.getProperty("alpha")));
									regSlider.setValue((Float) imX.getProperty("alpha"));
									regCheck.setSelected(false);
								} else {
									imX = ftsvd_fft.deblur(Float.parseFloat(regField.getText()), (float) threshold);
									timer.stop();
								}
								break;
							}
							break;
						}
						if (stencilChoice.isEnabled() == true) {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + methodShortNames[methodChoice.getSelectedIndex()] + "_" + stencilShortNames[stencilChoice.getSelectedIndex()] + "_" + regField.getText()));
						} else {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + methodShortNames[methodChoice.getSelectedIndex()] + "_" + regField.getText()));
						}
						imX.show();
						if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex())
								&& (oldOutputIndex == outputChoice.getSelectedIndex()) && (oldStencilIndex == stencilChoice.getSelectedIndex()) && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!regCheck.isSelected())) {
							updateButton.setEnabled(true);
						} else {
							updateButton.setEnabled(false);
						}
						IJ.showStatus(timer.toString());
						setCursor(defaultCursor);
						deconvolveButton.setEnabled(true);
						cancelButton.setEnabled(true);
					}
				});
				thread.setUncaughtExceptionHandler(new DefaultExceptionHandler());
				thread.start();
			}
		}

		private class UpdateButtonActionListener implements ActionListener {
			private final Timer timer = new Timer();

			public void actionPerformed(ActionEvent e) {
				if (!checkRegFieldText())
					return;
				if (!checkThresholdFieldText())
					return;
				Thread thread = new Thread(new Runnable() {
					public void run() {
						setCursor(waitCursor);
						deconvolveButton.setEnabled(false);
						updateButton.setEnabled(false);
						cancelButton.setEnabled(false);
						Method selMethod = Method.values()[methodChoice.getSelectedIndex()];
						Precision selPrecision = Precision.values()[precisionChoice.getSelectedIndex()];
						double threshold = -1.0;
						if (thresholdCheck.isSelected()) {
							threshold = Double.parseDouble(thresholdField.getText());
						}
						timer.reset().start();
						switch (selPrecision) {
						case DOUBLE:
							switch (selMethod) {
							case GTIK_REFLEXIVE:
								dgtik_dct.update(Double.parseDouble(regField.getText()), threshold, imX);
								break;
							case GTIK_PERIODIC:
								dgtik_fft.update(Double.parseDouble(regField.getText()), threshold, imX);
								break;
							case TIK_REFLEXIVE:
								dtik_dct.update(Double.parseDouble(regField.getText()), threshold, imX);
								break;
							case TIK_PERIODIC:
								dtik_fft.update(Double.parseDouble(regField.getText()), threshold, imX);
								break;
							case TSVD_REFLEXIVE:
								dtsvd_dct.update(Double.parseDouble(regField.getText()), threshold, imX);
								break;
							case TSVD_PERIODIC:
								dtsvd_fft.update(Double.parseDouble(regField.getText()), threshold, imX);
								break;
							}
							break;
						case SINGLE:
							switch (selMethod) {
							case GTIK_REFLEXIVE:
								fgtik_dct.update(Float.parseFloat(regField.getText()), (float) threshold, imX);
								break;
							case GTIK_PERIODIC:
								fgtik_fft.update(Float.parseFloat(regField.getText()), (float) threshold, imX);
								break;
							case TIK_REFLEXIVE:
								ftik_dct.update(Float.parseFloat(regField.getText()), (float) threshold, imX);
								break;
							case TIK_PERIODIC:
								ftik_fft.update(Float.parseFloat(regField.getText()), (float) threshold, imX);
								break;
							case TSVD_REFLEXIVE:
								ftsvd_dct.update(Float.parseFloat(regField.getText()), (float) threshold, imX);
								break;
							case TSVD_PERIODIC:
								ftsvd_fft.update(Float.parseFloat(regField.getText()), (float) threshold, imX);
								break;
							}
							break;
						}
						timer.stop();
						if (stencilChoice.isEnabled() == true) {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + methodShortNames[methodChoice.getSelectedIndex()] + "_" + stencilShortNames[stencilChoice.getSelectedIndex()] + "_" + regField.getText()));
						} else {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + methodShortNames[methodChoice.getSelectedIndex()] + "_" + regField.getText()));
						}
						imX.updateAndDraw();
						blurChoice.removeItemAt(blurChoice.getItemCount() - 1);
						psfChoice.removeItemAt(psfChoice.getItemCount() - 1);
						blurChoice.addItem(imX.getTitle());
						psfChoice.addItem(imX.getTitle());
						blurChoice.revalidate();
						psfChoice.revalidate();
						IJ.showStatus(timer.toString());
						setCursor(defaultCursor);
						if ((oldImageTitle.equals(blurChoice.getSelectedItem())) && (oldPSFTitle.equals(psfChoice.getSelectedItem())) && (oldMethodIndex == methodChoice.getSelectedIndex()) && (oldResizingIndex == resizingChoice.getSelectedIndex())
								&& (oldOutputIndex == outputChoice.getSelectedIndex()) && (oldStencilIndex == stencilChoice.getSelectedIndex()) && (oldPrecisionIndex == precisionChoice.getSelectedIndex()) && (!regCheck.isSelected())) {
							updateButton.setEnabled(true);
						} else {
							updateButton.setEnabled(false);
						}
						deconvolveButton.setEnabled(true);
						cancelButton.setEnabled(true);
					}
				});
				thread.setUncaughtExceptionHandler(new DefaultExceptionHandler());
				thread.start();
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				mainPanelFrame.dispose();
				ImagePlus.removeImageListener(getImageListener());
				cleanAll();
			}
		}

		private boolean checkRegFieldText() {
			double val = 0.0;
			try {
				val = Double.parseDouble(regField.getText());
			} catch (Exception ex) {
				IJ.error("Regularization parameter must be between 0 and 1.");
				return false;
			}
			if ((val < 0.0) || (val > 1.0)) {
				IJ.error("Regularization parameter must be between 0 and 1.");
				return false;
			}
			return true;
		}

		private boolean checkThresholdFieldText() {
			double val = 0.0;
			try {
				val = Double.parseDouble(thresholdField.getText());
			} catch (Exception ex) {
				IJ.error("Threshold must be a nonnegative value.");
				return false;
			}
			if (val < 0.0) {
				IJ.error("Threshold must be a nonnegative value.");
				return false;
			}
			return true;
		}

		private boolean checkThreadsFieldText() {
			int val = 0;
			try {
				val = Integer.parseInt(threadsField.getText());
			} catch (Exception ex) {
				IJ.error("Number of threads must be power of 2.");
				return false;
			}
			if (val < 1) {
				IJ.error("Number of threads must be power of 2.");
				return false;
			}
			if (!ConcurrencyUtils.isPowerOf2(val)) {
				IJ.error("Number of threads must be power of 2.");
				return false;
			}
			return true;
		}

	}

	public static void main(String args[]) {

		

		new ImageJ();
		IJ.open("D:\\Research\\Images\\io513-blur.png");
		IJ.open("D:\\Research\\Images\\io513-psf.png");
		IJ.runPlugIn("Parallel_Spectral_Deconvolution_2D", null);
	}
}
