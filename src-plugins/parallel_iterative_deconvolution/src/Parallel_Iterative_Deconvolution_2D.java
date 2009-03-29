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
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import cern.colt.Arrays;
import cern.colt.Timer;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoubleCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoublePCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatPCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBROptions;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBR_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBROptions;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBR_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.InSolvType;
import edu.emory.mathcs.restoretools.iterative.method.hybr.RegMethodType;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoubleMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoublePMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatPMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.wpl.DoubleWPLOptions;
import edu.emory.mathcs.restoretools.iterative.method.wpl.DoubleWPL_2D;
import edu.emory.mathcs.restoretools.iterative.method.wpl.FloatWPLOptions;
import edu.emory.mathcs.restoretools.iterative.method.wpl.FloatWPL_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.PreconditionerType;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Parallel Iterative Deconvolution 2D GUI
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class Parallel_Iterative_Deconvolution_2D implements PlugIn, ImageListener {

	/**
	 * Method used to deblur with WPL from a macro.
	 * 
	 * @param pathToBlurredImage
	 * @param pathToPsf
	 * @param pathToDeblurredImage
	 * @param boundaryStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param thresholdStr
	 * @param nOfItersStr
	 * @param nOfThreadsStr
	 * @param showIterStr
	 * @param gammaStr
	 * @param filterXYStr
	 * @param normalizeStr
	 * @param logMeanStr
	 * @param antiRingStr
	 * @param changeThreshPercentStr
	 * @param dbStr
	 * @param detectDivergenceStr
	 * @return path to deblurred image or error message
	 */
	public static String deconvolveWPL(String pathToBlurredImage, String pathToPsf, String pathToDeblurredImage, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String thresholdStr, String nOfItersStr, String nOfThreadsStr, String showIterStr, String gammaStr, String filterXYStr, String normalizeStr,
			String logMeanStr, String antiRingStr, String changeThreshPercentStr, String dbStr, String detectDivergenceStr) {
		boolean showIteration, normalize, logMean, antiRing, db, detectDivergence;
		double threshold, gamma, filterXY, changeThreshPercent;
		int nOfIters;
		int nOfThreads;
		BoundaryType boundary = null;
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
			return "RGB images are not currently supported";
		}
		if (imB.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D";
		}
		ImageProcessor ipPSF = imPSF.getProcessor();
		if (ipPSF instanceof ColorProcessor) {
			return "RGB images are not currently supported";
		}
		if (imPSF.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D";
		}
		try {
			nOfIters = Integer.parseInt(nOfItersStr);
		} catch (Exception ex) {
			return "nOfIters must be a positive integer";
		}
		if (nOfIters < 1) {
			return "nOfIters must be a positive integer";
		}
		for (BoundaryType elem : BoundaryType.values()) {
			if (elem.toString().equals(boundaryStr)) {
				boundary = elem;
				break;
			}
		}
		if (boundary == null) {
			return "boundary must be in " + Arrays.toString(BoundaryType.values());
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
			showIteration = Boolean.parseBoolean(showIterStr);
		} catch (Exception ex) {
			return "showItration must be a boolean value (true or false)";
		}
		try {
			gamma = Double.parseDouble(gammaStr);
		} catch (Exception ex) {
			return "gamma must be a nonnegative value";
		}
		if (gamma < 0.0) {
			return "gamma must be a nonnegative value";
		}

		try {
			filterXY = Double.parseDouble(filterXYStr);
		} catch (Exception ex) {
			return "filterXY must be a nonnegative value";
		}
		if (filterXY < 0.0) {
			return "filterXY must be a nonnegative value";
		}
		try {
			normalize = Boolean.parseBoolean(normalizeStr);
		} catch (Exception ex) {
			return "normalize must be a boolean value (true or false)";
		}
		try {
			logMean = Boolean.parseBoolean(logMeanStr);
		} catch (Exception ex) {
			return "logMean must be a boolean value (true or false)";
		}
		try {
			antiRing = Boolean.parseBoolean(antiRingStr);
		} catch (Exception ex) {
			return "antiRing must be a boolean value (true or false)";
		}
		try {
			db = Boolean.parseBoolean(dbStr);
		} catch (Exception ex) {
			return "db must be a boolean value (true or false)";
		}
		try {
			detectDivergence = Boolean.parseBoolean(detectDivergenceStr);
		} catch (Exception ex) {
			return "detectDivergence must be a boolean value (true or false)";
		}
		try {
			changeThreshPercent = Double.parseDouble(changeThreshPercentStr);
		} catch (Exception ex) {
			return "changeThreshPercent must be a nonnegative value";
		}
		if (changeThreshPercent < 0.0) {
			IJ.error("changeThreshPercent must be a nonnegative value");
		}
		ConcurrencyUtils.setNumberOfProcessors(nOfThreads);
		switch (precision) {
		case DOUBLE:
			DoubleWPLOptions doptions = new DoubleWPLOptions(gamma, filterXY, filterXY, filterXY, normalize, logMean, antiRing, changeThreshPercent, db, detectDivergence);
			DoubleWPL_2D dwpl = new DoubleWPL_2D(imB, imPSF, boundary, resizing, output, nOfIters, doptions, showIteration);
			imX = dwpl.deblur(threshold);
			break;
		case SINGLE:
			FloatWPLOptions foptions = new FloatWPLOptions((float) gamma, (float) filterXY, (float) filterXY, (float) filterXY, normalize, logMean, antiRing, (float) changeThreshPercent, db, detectDivergence);
			FloatWPL_2D fwpl = new FloatWPL_2D(imB, imPSF, boundary, resizing, output, nOfIters, foptions, showIteration);
			imX = fwpl.deblur((float) threshold);
			break;
		}
		IJ.save(imX, pathToDeblurredImage);
		return pathToDeblurredImage;
	}

	/**
	 * Method used to deblur with MRNSD from a macro.
	 * 
	 * @param pathToBlurredImage
	 * @param pathToPsf
	 * @param pathToDeblurredImage
	 * @param preconditionerStr
	 * @param preconditionerTolStr
	 * @param boundaryStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param stoppingTolStr
	 * @param thresholdStr
	 * @param nOfItersStr
	 * @param nOfThreadsStr
	 * @param showIterStr
	 * @return path to deblurred image or error message
	 */
	public static String deconvolveMRNSD(String pathToBlurredImage, String pathToPsf, String pathToDeblurredImage, String preconditionerStr, String preconditionerTolStr, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String stoppingTolStr, String thresholdStr, String nOfItersStr, String nOfThreadsStr,
			String showIterStr) {
		double stoppingTol;
		double preconditionerTol;
		boolean showIteration;
		double threshold;
		int nOfIters;
		int nOfThreads;
		PreconditionerType preconditioner = null;
		BoundaryType boundary = null;
		ResizingType resizing = null;
		OutputType output = null;		
		Precision precision = null;
		ImagePlus imX = null;
		ImagePlus imB = IJ.openImage(pathToBlurredImage);
		if (imB == null) {
			return "Cannot open image " + pathToBlurredImage;
		}
		ImagePlus[][] imPSF = new ImagePlus[1][1];
		imPSF[0][0] = IJ.openImage(pathToPsf);
		if (imPSF[0][0] == null) {
			return "Cannot open image " + pathToPsf;
		}
		ImageProcessor ipB = imB.getProcessor();
		if (ipB instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imB.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D";
		}
		ImageProcessor ipPSF = imPSF[0][0].getProcessor();
		if (ipPSF instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imPSF[0][0].getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D.";
		}
		try {
			nOfIters = Integer.parseInt(nOfItersStr);
		} catch (Exception ex) {
			return "nOfIters must be a positive integer";
		}
		if (nOfIters < 1) {
			return "nOfIters must be a positive integer";
		}
		for (PreconditionerType elem : PreconditionerType.values()) {
			if (elem.toString().equals(preconditionerStr)) {
				preconditioner = elem;
				break;
			}
		}
		if (preconditioner == null) {
			return "preconditioner must be in " + Arrays.toString(PreconditionerType.values());
		}
		for (BoundaryType elem : BoundaryType.values()) {
			if (elem.toString().equals(boundaryStr)) {
				boundary = elem;
				break;
			}
		}
		if (boundary == null) {
			return "boundary must be in " + Arrays.toString(BoundaryType.values());
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
			preconditionerTol = Double.parseDouble(preconditionerTolStr);
		} catch (Exception ex) {
			return "preconditionerTol must be a number between 0 and 1 or -1 for auto";
		}
		if ((preconditionerTol != -1) && ((preconditionerTol < 0) || (preconditionerTol > 1))) {
			return "preconditionerTol must be a number between 0 and 1 or -1 for auto";
		}
		try {
			stoppingTol = Double.parseDouble(stoppingTolStr);
		} catch (Exception ex) {
			return "stoppingTol must be a number between 0 and 1 or -1 for auto";
		}
		if ((stoppingTol != -1) && ((stoppingTol < 0) || (stoppingTol > 1))) {
			return "stoppingTol must be a number between 0 and 1 or -1 for auto";
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
			showIteration = Boolean.parseBoolean(showIterStr);
		} catch (Exception ex) {
			return "showItration must be a boolean value (true or false)";
		}
		ConcurrencyUtils.setNumberOfProcessors(nOfThreads);
		switch (precision) {
		case DOUBLE:
			if (preconditioner == PreconditionerType.NONE) {
				DoubleMRNSD_2D dmrnsd = new DoubleMRNSD_2D(imB, imPSF, boundary, resizing, output, nOfIters, stoppingTol, false, showIteration);
				imX = dmrnsd.deblur(threshold);
			} else {
				DoublePMRNSD_2D dpmrnsd = new DoublePMRNSD_2D(imB, imPSF, boundary, resizing, output, preconditioner, preconditionerTol, nOfIters, stoppingTol, false, showIteration);
				imX = dpmrnsd.deblur(threshold);
			}
			break;
		case SINGLE:
			if (preconditioner == PreconditionerType.NONE) {
				FloatMRNSD_2D fmrnsd = new FloatMRNSD_2D(imB, imPSF, boundary, resizing, output, nOfIters, (float) stoppingTol, false, showIteration);
				imX = fmrnsd.deblur((float) threshold);
			} else {
				FloatPMRNSD_2D fpmrnsd = new FloatPMRNSD_2D(imB, imPSF, boundary, resizing, output, preconditioner, (float) preconditionerTol, nOfIters, (float) stoppingTol, false, showIteration);
				imX = fpmrnsd.deblur((float) threshold);
			}
			break;
		}
		IJ.save(imX, pathToDeblurredImage);
		return pathToDeblurredImage;
	}

	/**
	 * Method used to deblur with HyBR from a macro.
	 * @param pathToBlurredImage
	 * @param pathToPsf
	 * @param pathToDeblurredImage
	 * @param preconditionerStr
	 * @param preconditionerTolStr
	 * @param boundaryStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param thresholdStr
	 * @param nOfItersStr
	 * @param nOfThreadsStr
	 * @param showIterStr
	 * @param inSolvStr
	 * @param regMethodStr
	 * @param regParamStr
	 * @param omegaStr
	 * @param reorthStr
	 * @param begRegStr
	 * @param flatTolStr
	 * @return path to deblurred image or error message
	 */
	public static String deconvolveHyBR(String pathToBlurredImage, String pathToPsf, String pathToDeblurredImage, String preconditionerStr, String preconditionerTolStr, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String thresholdStr, String nOfItersStr, String nOfThreadsStr, String showIterStr,
			String inSolvStr, String regMethodStr, String regParamStr, String omegaStr, String reorthStr, String begRegStr, String flatTolStr) {
		double preconditionerTol, regParam, omega, flatTol;
		boolean showIteration, reorth;
		double threshold;
		int nOfIters;
		int nOfThreads;
		int begReg;
		PreconditionerType preconditioner = null;
		InSolvType inSolv = null;
		RegMethodType regMethod = null;
		BoundaryType boundary = null;
		ResizingType resizing = null;
		OutputType output = null;
		Precision precision = null;
		ImagePlus imX = null;
		ImagePlus imB = IJ.openImage(pathToBlurredImage);
		if (imB == null) {
			return "Cannot open image " + pathToBlurredImage;
		}
		ImagePlus[][] imPSF = new ImagePlus[1][1];
		imPSF[0][0] = IJ.openImage(pathToPsf);
		if (imPSF[0][0] == null) {
			return "Cannot open image " + pathToPsf;
		}
		ImageProcessor ipB = imB.getProcessor();
		if (ipB instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imB.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D";
		}
		ImageProcessor ipPSF = imPSF[0][0].getProcessor();
		if (ipPSF instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imPSF[0][0].getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D.";
		}
		try {
			nOfIters = Integer.parseInt(nOfItersStr);
		} catch (Exception ex) {
			return "nOfIters must be a positive integer";
		}
		if (nOfIters < 1) {
			return "nOfIters must be a positive integer";
		}
		for (PreconditionerType elem : PreconditionerType.values()) {
			if (elem.toString().equals(preconditionerStr)) {
				preconditioner = elem;
				break;
			}
		}
		if (preconditioner == null) {
			return "preconditioner must be in " + Arrays.toString(PreconditionerType.values());
		}
		for (BoundaryType elem : BoundaryType.values()) {
			if (elem.toString().equals(boundaryStr)) {
				boundary = elem;
				break;
			}
		}
		if (boundary == null) {
			return "boundary must be in " + Arrays.toString(BoundaryType.values());
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
			preconditionerTol = Double.parseDouble(preconditionerTolStr);
		} catch (Exception ex) {
			return "preconditionerTol must be a number between 0 and 1 or -1 for auto";
		}
		if ((preconditionerTol != -1) && ((preconditionerTol < 0) || (preconditionerTol > 1))) {
			return "preconditionerTol must be a number between 0 and 1 or -1 for auto";
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
			showIteration = Boolean.parseBoolean(showIterStr);
		} catch (Exception ex) {
			return "showItration must be a boolean value (true or false)";
		}
		for (InSolvType elem : InSolvType.values()) {
			if (elem.toString().equals(inSolvStr)) {
				inSolv = elem;
				break;
			}
		}
		if (inSolv == null) {
			return "inSolv must be in " + Arrays.toString(InSolvType.values());
		}
		for (RegMethodType elem : RegMethodType.values()) {
			if (elem.toString().equals(regMethodStr)) {
				regMethod = elem;
				break;
			}
		}
		if (regMethod == null) {
			return "regMethod method must be in " + Arrays.toString(RegMethodType.values());
		}

		try {
			regParam = Double.parseDouble(regParamStr);
		} catch (Exception ex) {
			return "regParam must be a floating-point number between 0 and 1 or -1 for auto";
		}
		if ((regParam != -1) && ((regParam < 0.0) || (regParam > 1.0))) {
			return "regParam must be a floating-point number between 0 and 1 or -1 for auto";
		}

		try {
			omega = Double.parseDouble(omegaStr);
		} catch (Exception ex) {
			return "omega must be a nonnegative floating-point number";
		}
		if (omega < 0.0) {
			return "omega must be a nonnegative floating-point number";
		}
		try {
			reorth = Boolean.parseBoolean(reorthStr);
		} catch (Exception ex) {
			return "reorth must be a boolean value (true or false)";
		}
		try {
			begReg = Integer.parseInt(begRegStr);
		} catch (Exception ex) {
			return "begReg must be an integer number greater than 1";
		}
		if (begReg <= 1) {
			return "begReg must be an integer number greater than 1";
		}

		try {
			flatTol = Double.parseDouble(flatTolStr);
		} catch (Exception ex) {
			return "flatTol must be a nonnegative floating-point number";
		}
		if (flatTol < 0.0) {
			return "flatTol must be a nonnegative floating-point number";
		}

		ConcurrencyUtils.setNumberOfProcessors(nOfThreads);
		switch (precision) {
		case DOUBLE:
			DoubleHyBROptions doptions = new DoubleHyBROptions(inSolv, regMethod, regParam, omega, reorth, begReg, flatTol);
			DoubleHyBR_2D dhybr = new DoubleHyBR_2D(imB, imPSF, boundary, resizing, output, preconditioner, preconditionerTol, nOfIters, doptions, showIteration);
			imX = dhybr.deblur(threshold);
			break;
		case SINGLE:
			FloatHyBROptions foptions = new FloatHyBROptions(inSolv, regMethod, (float) regParam, (float) omega, reorth, begReg, (float) flatTol);
			FloatHyBR_2D fhybr = new FloatHyBR_2D(imB, imPSF, boundary, resizing, output, preconditioner, (float) preconditionerTol, nOfIters, foptions, showIteration);
			imX = fhybr.deblur((float) threshold);
			break;
		}
		IJ.save(imX, pathToDeblurredImage);
		return pathToDeblurredImage;
	}

	/**
	 * Mathod used to deblur with CGLS from a macro.
	 * @param pathToBlurredImage
	 * @param pathToPsf
	 * @param pathToDeblurredImage
	 * @param preconditionerStr
	 * @param preconditionerTolStr
	 * @param boundaryStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param stoppingTolStr
	 * @param thresholdStr
	 * @param nOfItersStr
	 * @param nOfThreadsStr
	 * @param showIterStr
	 * @return path to deblurred image or error message
	 */
	public static String deconvolveCGLS(String pathToBlurredImage, String pathToPsf, String pathToDeblurredImage, String preconditionerStr, String preconditionerTolStr, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String stoppingTolStr, String thresholdStr, String nOfItersStr, String nOfThreadsStr,
			String showIterStr) {
		double stoppingTol;
		double preconditionerTol;
		boolean showIteration;
		double threshold;
		int nOfIters;
		int nOfThreads;
		PreconditionerType preconditioner = null;
		BoundaryType boundary = null;
		ResizingType resizing = null;
		OutputType output = null;
		Precision precision = null;
		ImagePlus imX = null;
		ImagePlus imB = IJ.openImage(pathToBlurredImage);
		if (imB == null) {
			return "Cannot open image " + pathToBlurredImage;
		}
		ImagePlus[][] imPSF = new ImagePlus[1][1];
		imPSF[0][0] = IJ.openImage(pathToPsf);
		if (imPSF[0][0] == null) {
			return "Cannot open image " + pathToPsf;
		}
		ImageProcessor ipB = imB.getProcessor();
		if (ipB instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imB.getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D.";
		}
		ImageProcessor ipPSF = imPSF[0][0].getProcessor();
		if (ipPSF instanceof ColorProcessor) {
			return "RGB images are not currently supported.";
		}
		if (imPSF[0][0].getStackSize() > 1) {
			return "For 3D images use Parallel Iterative Deconvolution 3D.";
		}
		try {
			nOfIters = Integer.parseInt(nOfItersStr);
		} catch (Exception ex) {
			return "nOfIters must be a positive integer";
		}
		if (nOfIters < 1) {
			return "nOfIters must be a positive integer";
		}
		for (PreconditionerType elem : PreconditionerType.values()) {
			if (elem.toString().equals(preconditionerStr)) {
				preconditioner = elem;
				break;
			}
		}
		if (preconditioner == null) {
			return "preconditioner must be in " + Arrays.toString(PreconditionerType.values());
		}
		for (BoundaryType elem : BoundaryType.values()) {
			if (elem.toString().equals(boundaryStr)) {
				boundary = elem;
				break;
			}
		}
		if (boundary == null) {
			return "boundary must be in " + Arrays.toString(BoundaryType.values());
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
			preconditionerTol = Double.parseDouble(preconditionerTolStr);
		} catch (Exception ex) {
			return "preconditionerTol must be a number between 0 and 1 or -1 for auto";
		}
		if ((preconditionerTol != -1) && ((preconditionerTol < 0) || (preconditionerTol > 1))) {
			return "preconditionerTol must be a number between 0 and 1 or -1 for auto";
		}
		try {
			stoppingTol = Double.parseDouble(stoppingTolStr);
		} catch (Exception ex) {
			return "stoppingTol must be a number between 0 and 1 or -1 for auto";
		}
		if ((stoppingTol != -1) && ((stoppingTol < 0) || (stoppingTol > 1))) {
			return "stoppingTol must be a number between 0 and 1 or -1 for auto";
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
			showIteration = Boolean.parseBoolean(showIterStr);
		} catch (Exception ex) {
			return "showItration must be a boolean value (true or false)";
		}
		ConcurrencyUtils.setNumberOfProcessors(nOfThreads);
		switch (precision) {
		case DOUBLE:
			if (preconditioner == PreconditionerType.NONE) {
				DoubleCGLS_2D dcgls = new DoubleCGLS_2D(imB, imPSF, boundary, resizing, output, nOfIters, stoppingTol, false, showIteration);
				imX = dcgls.deblur(threshold);
			} else {
				DoublePCGLS_2D dpcgls = new DoublePCGLS_2D(imB, imPSF, boundary, resizing, output, preconditioner, preconditionerTol, nOfIters, stoppingTol, false, showIteration);
				imX = dpcgls.deblur(threshold);
			}
			break;
		case SINGLE:
			if (preconditioner == PreconditionerType.NONE) {
				FloatCGLS_2D fcgls = new FloatCGLS_2D(imB, imPSF, boundary, resizing, output, nOfIters, (float) stoppingTol, false, showIteration);
				imX = fcgls.deblur((float) threshold);
			} else {
				FloatPCGLS_2D fpcgls = new FloatPCGLS_2D(imB, imPSF, boundary, resizing, output, preconditioner, (float) preconditionerTol, nOfIters, (float) stoppingTol, false, showIteration);
				imX = fpcgls.deblur((float) threshold);
			}
			break;
		}
		IJ.save(imX, pathToDeblurredImage);
		return pathToDeblurredImage;
	}

	private final static String version = "1.8";

	private enum Method {
		MRNSD, WPL, CGLS, HyBR
	};

	private enum Precision {
		SINGLE, DOUBLE
	};

	private static final String[] methodNames = { "MRNSD", "WPL", "CGLS", "HyBR" };

	private static final String[] shortMethodNames = { "mrnsd", "wpl", "cgls", "hybr" };

	private static final String[] precisionNames = { "Single", "Double" };

	private static final String[] precondNames = { "FFT preconditioner", "None" };

	private static final String[] shortPrecondNames = { "fft", "none" };

	private static final String[] boundaryNames = { "Reflexive", "Periodic", "Zero" };

	private static final String[] resizingNames = { "Auto", "Minimal", "Next power of two" };
	
	private static final String[] outputNames = { "Same as source", "Byte (8-bit)", "Short (16-bit)", "Float (32-bit)" };
	
	private static final String[] shortBoundaryNames = { "ref", "per", "zero" };

	private static final String[] innerSolverNames = { "Tikhonov", "None" };

	private static final String[] regMethodNames = { "GCV", "WGCV", "Adaptive WGCV", "None" };

	private JFrame mainPanelFrame, PSFCreateFrame, PSFEditFrame, HyBROptionsFrame, MRNSDOptionsFrame, CGLSOptionsFrame, WPLOptionsFrame;

	private DoubleCGLS_2D dcgls;

	private DoublePCGLS_2D dpcgls;

	private DoubleHyBR_2D dhybr;

	private DoubleMRNSD_2D dmrnsd;

	private DoublePMRNSD_2D dpmrnsd;

	private DoubleWPL_2D dwpl;

	private FloatCGLS_2D fcgls;

	private FloatPCGLS_2D fpcgls;

	private FloatHyBR_2D fhybr;

	private FloatMRNSD_2D fmrnsd;

	private FloatPMRNSD_2D fpmrnsd;

	private FloatWPL_2D fwpl;

	private ImagePlus imB, imX;

	private ImagePlus[][] imPSF;

	private int[] windowIDs;

	private String[] imageTitles;

	private JComboBox blurChoice, psfChoice, methodChoice, precondChoice, boundaryChoice, resizingChoice, outputChoice, precisionChoice;

	private JTextField itersField, threadsField, precondField;

	private JCheckBox variantPSFCheck, itersCheck, precondCheck;

	private JButton definePSFButton, editPSFButton, optionsButton, deconvolveButton, cancelButton;

	private int psfRows, psfColumns;

	private DoubleHyBROptions dHyBROptions;

	private FloatHyBROptions fHyBROptions;

	private DoubleWPLOptions dWPLOptions;

	private FloatWPLOptions fWPLOptions;

	private double cglsThreshold = 0;

	private double mrnsdThreshold = -1;

	private double wplThreshold = -1;

	private double hybrThreshold = 0;

	private double cglsStoppingTol = -1;

	private double mrnsdStoppingTol = -1;

	private boolean hybrOptionsSet = false;

	private boolean mrnsdOptionsSet = false;

	private boolean cglsOptionsSet = false;

	private boolean wplOptionsSet = false;

	private int iters, threads;

	private double precTol;

	private Thread deconvolveThread;

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
		mainPanelFrame = new JFrame("Parallel Iterative Deconvolution 2D " + version + " ");
		mainPanelFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainPanelFrame.add(panel);
		mainPanelFrame.setResizable(false);
		mainPanelFrame.pack();
		mainPanelFrame.setLocationRelativeTo(null);
		mainPanelFrame.setVisible(true);
	}

	public void imageClosed(ImagePlus imp) {
		blurChoice.removeItem(imp.getTitle());
		blurChoice.revalidate();
		psfChoice.removeItem(imp.getTitle());
		psfChoice.revalidate();
		if (imX != null) {
			if (imp.getTitle().equals(imX.getTitle())) {
				clean_old_data();
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

	private void clean_old_data() {
		dcgls = null;
		dpcgls = null;
		dmrnsd = null;
		dpmrnsd = null;
		dhybr = null;
		dwpl = null;
		fcgls = null;
		fpcgls = null;
		fmrnsd = null;
		fpmrnsd = null;
		fhybr = null;
		fwpl = null;

	}

	private void clean_all() {
		dcgls = null;
		dpcgls = null;
		dmrnsd = null;
		dpmrnsd = null;
		dhybr = null;
		dwpl = null;
		fcgls = null;
		fpcgls = null;
		fmrnsd = null;
		fpmrnsd = null;
		fhybr = null;
		fwpl = null;
		imB = null;
		imPSF = null;
		imX = null;
		windowIDs = null;
		imageTitles = null;
	}

	private class MRNSDOptionsPanel extends JPanel {

		private static final long serialVersionUID = -8230645552596239730L;

		private JTextField stoppingTolField, thresholdField;

		private JCheckBox stoppingTolCheck, thresholdCheck;

		public MRNSDOptionsPanel() {
		}

		private void init() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			JPanel stoppingTolPanel = new JPanel();
			stoppingTolPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel stoppingTolLabel = new JLabel("Stopping tolerance:");
			stoppingTolPanel.add(stoppingTolLabel);
			if (Precision.values()[precisionChoice.getSelectedIndex()] == Precision.DOUBLE) {
				stoppingTolField = new JTextField("1E-6", 5);
			} else {
				stoppingTolField = new JTextField("1E-4", 5);
			}
			stoppingTolField.addActionListener(new StoppingTolFieldActionListener());
			stoppingTolField.setEnabled(false);
			stoppingTolPanel.add(stoppingTolField);
			stoppingTolCheck = new JCheckBox("Auto");
			stoppingTolCheck.addItemListener(new StoppingTolCheckItemListener());
			stoppingTolCheck.setSelected(true);
			stoppingTolPanel.add(stoppingTolCheck);
			add(stoppingTolPanel);

			JPanel thresholdPanel = new JPanel();
			thresholdPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			thresholdCheck = new JCheckBox("Threshold:  ");
			thresholdCheck.setSelected(false);
			thresholdCheck.addItemListener(new ThresholdCheckItemListener());
			thresholdPanel.add(thresholdCheck);
			thresholdField = new JTextField("0.0", 6);
			thresholdField.setEnabled(false);
			thresholdField.addActionListener(new ThresholdFieldActionListener());
			thresholdPanel.add(thresholdField);
			add(thresholdPanel);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new OkButtonActionListener());
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel);
			validate();
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

		private class StoppingTolCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (stoppingTolCheck.isSelected()) {
					stoppingTolField.setEnabled(false);
				} else {
					stoppingTolField.setEnabled(true);
				}
			}
		}

		private class StoppingTolFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				checkStoppingTolFieldText();
			}
		}

		private class ThresholdFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				checkThresholdFieldText();
			}
		}

		private boolean checkThresholdFieldText() {
			try {
				mrnsdThreshold = Double.parseDouble(thresholdField.getText());
			} catch (Exception ex) {
				IJ.error("Threshold must be a nonnegative value.");
				return false;
			}
			if (mrnsdThreshold < 0.0) {
				IJ.error("Threshold must be a nonnegative value.");
				return false;
			}
			return true;
		}

		private boolean checkStoppingTolFieldText() {
			try {
				mrnsdStoppingTol = Double.parseDouble(stoppingTolField.getText());
			} catch (Exception ex) {
				IJ.error("Stopping tolerance must be a nonnegative floating-point number.");
				return false;
			}
			if (mrnsdStoppingTol < 0.0) {
				IJ.error("Omega must be a nonnegative floating-point number.");
				return false;
			}
			return true;
		}

		private class OkButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (!stoppingTolCheck.isSelected()) {
					if (!checkStoppingTolFieldText())
						return;
				} else {
					mrnsdStoppingTol = -1;
				}
				if (thresholdCheck.isSelected()) {
					if (!checkThresholdFieldText())
						return;
				} else {
					mrnsdThreshold = -1;
				}
				MRNSDOptionsFrame.setVisible(false);
				mrnsdOptionsSet = true;
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				MRNSDOptionsFrame.dispose();
				mrnsdOptionsSet = false;
			}
		}

	}

	private class CGLSOptionsPanel extends JPanel {

		private static final long serialVersionUID = 7409538380768081324L;

		private JTextField stoppingTolField, thresholdField;

		private JCheckBox stoppingTolCheck, thresholdCheck;

		public CGLSOptionsPanel() {
		}

		private void init() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			JPanel stoppingTolPanel = new JPanel();
			stoppingTolPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel stoppingTolLabel = new JLabel("Stopping tolerance:");
			stoppingTolPanel.add(stoppingTolLabel);
			if (Precision.values()[precisionChoice.getSelectedIndex()] == Precision.DOUBLE) {
				stoppingTolField = new JTextField("1E-6", 5);
			} else {
				stoppingTolField = new JTextField("1E-4", 5);
			}
			stoppingTolField.addActionListener(new StoppingTolFieldActionListener());
			stoppingTolField.setEnabled(false);
			stoppingTolPanel.add(stoppingTolField);
			stoppingTolCheck = new JCheckBox("Auto");
			stoppingTolCheck.addItemListener(new StoppingTolCheckItemListener());
			stoppingTolCheck.setSelected(true);
			stoppingTolPanel.add(stoppingTolCheck);
			add(stoppingTolPanel);

			JPanel thresholdPanel = new JPanel();
			thresholdPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			thresholdCheck = new JCheckBox("Threshold:  ");
			thresholdCheck.setSelected(true);
			thresholdCheck.addItemListener(new ThresholdCheckItemListener());
			thresholdPanel.add(thresholdCheck);
			thresholdField = new JTextField("0.0", 6);
			thresholdField.setEnabled(true);
			thresholdField.addActionListener(new ThresholdFieldActionListener());
			thresholdPanel.add(thresholdField);
			add(thresholdPanel);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new OkButtonActionListener());
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel);
			validate();
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

		private class StoppingTolCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (stoppingTolCheck.isSelected()) {
					stoppingTolField.setEnabled(false);
				} else {
					stoppingTolField.setEnabled(true);
				}
			}
		}

		private class StoppingTolFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				checkStoppingTolFieldText();
			}
		}

		private class ThresholdFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				checkThresholdFieldText();
			}
		}

		private boolean checkThresholdFieldText() {
			try {
				cglsThreshold = Double.parseDouble(thresholdField.getText());
			} catch (Exception ex) {
				IJ.error("Threshold must be a nonnegative value.");
				return false;
			}
			if (cglsThreshold < 0.0) {
				IJ.error("Threshold must be a nonnegative value.");
				return false;
			}
			return true;
		}

		private boolean checkStoppingTolFieldText() {
			try {
				cglsStoppingTol = Double.parseDouble(stoppingTolField.getText());
			} catch (Exception ex) {
				IJ.error("Stopping tolerance must be a nonnegative floating-point number.");
				return false;
			}
			if (cglsStoppingTol < 0.0) {
				IJ.error("Omega must be a nonnegative floating-point number.");
				return false;
			}
			return true;
		}

		private class OkButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (!stoppingTolCheck.isSelected()) {
					if (!checkStoppingTolFieldText())
						return;
				} else {
					cglsStoppingTol = -1;
				}
				if (thresholdCheck.isSelected()) {
					if (!checkThresholdFieldText())
						return;
				} else {
					cglsThreshold = -1;
				}
				CGLSOptionsFrame.setVisible(false);
				cglsOptionsSet = true;
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				CGLSOptionsFrame.dispose();
				cglsOptionsSet = false;
			}
		}

	}

	private class HyBROptionsPanel extends JPanel {

		private static final long serialVersionUID = -1408974856904413342L;

		private JComboBox solverChoice, regChoice;

		private JTextField regField, begRegField, omegaField, flatTolField, thresholdField;

		private JCheckBox reorthCheck, thresholdCheck;

		private double regParam, omega, flatGCVTol;

		private int begReg;

		public HyBROptionsPanel() {
		}

		private void init() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			JPanel regPanel = new JPanel();
			regPanel.setLayout(new BoxLayout(regPanel, BoxLayout.Y_AXIS));
			Border border = new TitledBorder(null, "Regularization options", TitledBorder.LEFT, TitledBorder.TOP);
			regPanel.setBorder(border);

			JPanel regPanelFirstLine = new JPanel();
			regPanelFirstLine.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel regMethodLabel = new JLabel("Method:");
			regPanelFirstLine.add(regMethodLabel);
			regChoice = new JComboBox(regMethodNames);
			regChoice.setSelectedIndex(2);
			regChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			regChoice.addActionListener(new RegMethodChoiceActionListener());
			regChoice
					.setToolTipText("<html>Choose a method for computing a regularization parameter:<br><ul><li>GCV - Generalized Cross-Validation</li><li>WGCV - Weighted Generalized Cross-Validation</li><li>Adaptive WGCV - Adaptive Weighted Generalized Cross-Validation</li><li>None - no method for choosing regularization parameter.<br> You have to enter your own value.</li></ul></html>");
			regPanelFirstLine.add(regChoice);
			JLabel regParamLabel = new JLabel("Parameter:");
			regPanelFirstLine.add(regParamLabel);
			regField = new JTextField("0.0", 5);
			regField.setToolTipText("<html>Regularization parameter</html>");
			regField.setEnabled(false);
			regField.addActionListener(new RegFieldActionListener());
			regPanelFirstLine.add(regField);
			regPanel.add(regPanelFirstLine);

			JPanel regPanelThirdLine = new JPanel();
			regPanelThirdLine.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel omegaLabel = new JLabel("Omega:");
			regPanelThirdLine.add(omegaLabel);
			omegaField = new JTextField("0.00001", 5);
			omegaField.setToolTipText("<html>Omega parameter</html>");
			omegaField.setEnabled(false);
			omegaField.addActionListener(new OmegaFieldActionListener());
			regPanelThirdLine.add(omegaField);
			regPanel.add(regPanelThirdLine);

			JPanel regPanelForthLine = new JPanel();
			regPanelForthLine.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel begRegLabel = new JLabel("Begin regularization after this iteration:");
			regPanelForthLine.add(begRegLabel);
			begRegField = new JTextField("2", 2);
			begRegField.addActionListener(new BegRegFieldActionListener());
			regPanelForthLine.add(begRegField);
			regPanel.add(regPanelForthLine);

			add(regPanel);

			JPanel solverPanel = new JPanel();
			solverPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel solverLabel = new JLabel("Inner solver:");
			solverPanel.add(solverLabel);
			solverChoice = new JComboBox(innerSolverNames);
			solverChoice.setSelectedIndex(0);
			solverChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			solverPanel.add(solverChoice);
			add(solverPanel);

			JPanel flatTolPanel = new JPanel();
			flatTolPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel flatTolLabel = new JLabel("Stopping tolerance:");
			flatTolPanel.add(flatTolLabel);
			if (Precision.values()[precisionChoice.getSelectedIndex()] == Precision.DOUBLE) {
				flatTolField = new JTextField("1E-6", 5);
			} else {
				flatTolField = new JTextField("1E-4", 5);
			}
			flatTolField.setToolTipText("<html>Tolerance for detecting flatness in <br>the GCV curve as a stopping criteria</html>");
			flatTolField.addActionListener(new FlatTolFieldActionListener());
			flatTolPanel.add(flatTolField);
			add(flatTolPanel);

			JPanel reorthPanel = new JPanel();
			reorthPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			reorthCheck = new JCheckBox("Reorthogonalize Lanczos subspaces");
			reorthCheck.setSelected(false);
			reorthPanel.add(reorthCheck);
			add(reorthPanel);

			JPanel thresholdPanel = new JPanel();
			thresholdPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			thresholdCheck = new JCheckBox("Threshold:  ");
			thresholdCheck.setSelected(true);
			thresholdCheck.addItemListener(new ThresholdCheckItemListener());
			thresholdPanel.add(thresholdCheck);
			thresholdField = new JTextField("0.0", 6);
			thresholdField.setEnabled(true);
			thresholdField.addActionListener(new ThresholdFieldActionListener());
			thresholdPanel.add(thresholdField);
			add(thresholdPanel);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new OkButtonActionListener());
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel);
			validate();
		}

		private class ThresholdFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					hybrThreshold = Double.parseDouble(thresholdField.getText());
				} catch (Exception ex) {
					IJ.error("Threshold must be a nonnegative value.");
				}
				if (hybrThreshold < 0.0) {
					IJ.error("Threshold must be a nonnegative value.");
				}
			}
		}

		private class ThresholdCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (thresholdCheck.isSelected()) {
					thresholdField.setEnabled(true);
				} else {
					thresholdField.setEnabled(false);
					hybrThreshold = -1;
				}
			}
		}

		private class OmegaFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					omega = Double.parseDouble(omegaField.getText());
				} catch (Exception ex) {
					IJ.error("Omega must be a nonnegative floating-point number.");
				}
				if (omega < 0.0) {
					IJ.error("Omega must be a nonnegative floating-point number.");
				}
			}
		}

		private class FlatTolFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					flatGCVTol = Double.parseDouble(flatTolField.getText());
				} catch (Exception ex) {
					IJ.error("GCV tolerance must be a positive floating-point number.");
				}
				if (flatGCVTol <= 0.0) {
					IJ.error("Omega must be a positive floating-point number.");
				}
			}
		}

		private class RegMethodChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (regChoice.getSelectedIndex() == 1) {// WGCV
					omegaField.setEnabled(true);
				} else {
					omegaField.setEnabled(false);
				}
				if (regChoice.getSelectedIndex() == 3) {// None
					regField.setEnabled(true);
					flatTolField.setEnabled(false);
				} else {
					regField.setEnabled(false);
					flatTolField.setEnabled(true);
				}

			}
		}

		private class RegFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					regParam = Double.parseDouble(regField.getText());
				} catch (Exception ex) {
					IJ.error("Regularization parameter must be a floating-point number between 0 and 1.");
				}
				if ((regParam < 0.0) || (regParam > 1.0)) {
					IJ.error("Regularization parameter must be a floating-point number between 0 and 1.");
				}
			}
		}

		private class BegRegFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					begReg = Integer.parseInt(begRegField.getText());
				} catch (Exception ex) {
					IJ.error("Iteration number must be an integer number greater than 1.");
				}
				if (begReg <= 1) {
					IJ.error("Iteration number must be an integer number greater than 1.");
				}
			}
		}

		private class OkButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (checkTextFields() == true) {
					InSolvType selInSolv = InSolvType.values()[solverChoice.getSelectedIndex()];
					RegMethodType selRegMethod = RegMethodType.values()[methodChoice.getSelectedIndex()];
					dHyBROptions = new DoubleHyBROptions(selInSolv, selRegMethod, regParam, omega, reorthCheck.isSelected(), begReg, flatGCVTol);
					fHyBROptions = new FloatHyBROptions(selInSolv, selRegMethod, (float) regParam, (float) omega, reorthCheck.isSelected(), begReg, (float) flatGCVTol);
					HyBROptionsFrame.setVisible(false);
					hybrOptionsSet = true;
				}
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				HyBROptionsFrame.dispose();
				hybrOptionsSet = false;
			}
		}

		private boolean checkTextFields() {
			try {
				regParam = Double.parseDouble(regField.getText());
			} catch (Exception ex) {
				IJ.error("Regularization parameter must be a floating-point number between 0 and 1.");
				return false;
			}
			if ((regParam < 0.0) || (regParam > 1.0)) {
				IJ.error("Regularization parameter must be a floating-point number between 0 and 1.");
				return false;
			}

			try {
				flatGCVTol = Double.parseDouble(flatTolField.getText());
			} catch (Exception ex) {
				IJ.error("GCV tolerance must be a nonnegative floating-point number.");
				return false;
			}
			if (flatGCVTol < 0.0) {
				IJ.error("GCV tolerance must be a nonnegative floating-point number.");
				return false;
			}

			try {
				omega = Double.parseDouble(omegaField.getText());
			} catch (Exception ex) {
				IJ.error("Omega must be a nonnegative floating-point number.");
				return false;
			}
			if (omega < 0.0) {
				IJ.error("Omega must be a nonnegative floating-point number.");
				return false;
			}

			try {
				begReg = Integer.parseInt(begRegField.getText());
			} catch (Exception ex) {
				IJ.error("Iteration number must be an integer number greater than 1.");
				return false;
			}
			if (begReg <= 1) {
				IJ.error("Iteration number must be an integer number greater than 1.");
				return false;
			}
			if (thresholdCheck.isSelected()) {
				try {
					hybrThreshold = Double.parseDouble(thresholdField.getText());
				} catch (Exception ex) {
					IJ.error("Threshold must be a nonnegative value.");
					return false;
				}
				if (hybrThreshold < 0.0) {
					IJ.error("Threshold must be a nonnegative value.");
					return true;
				}
			} else {
				hybrThreshold = -1;
			}
			return true;
		}
	}

	private class WPLOptionsPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private JTextField gammaField, filterXYfield, changeThreshPercentField, thresholdField;

		private JCheckBox normalizeCheck, logMeanCheck, antiRingCheck, dbCheck, detectDivergenceCheck, thresholdCheck;

		private double filterXY, gamma, changeThreshPercent;

		public WPLOptionsPanel() {
		}

		private void init() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			JPanel normalizePanel = new JPanel();
			normalizePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			normalizeCheck = new JCheckBox("Narmalize PSF");
			normalizeCheck.setSelected(true);
			normalizePanel.add(normalizeCheck);
			add(normalizePanel);

			JPanel logMeanPanel = new JPanel();
			logMeanPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			logMeanCheck = new JCheckBox("Log mean pixel value to track convergence");
			logMeanCheck.setSelected(false);
			logMeanPanel.add(logMeanCheck);
			add(logMeanPanel);

			JPanel antiRingPanel = new JPanel();
			antiRingPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			antiRingCheck = new JCheckBox("Perform anti-ringing step");
			antiRingCheck.setSelected(true);
			antiRingPanel.add(antiRingCheck);
			add(antiRingPanel);

			JPanel detectDivergencePanel = new JPanel();
			detectDivergencePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			detectDivergenceCheck = new JCheckBox("Detect divergence");
			detectDivergenceCheck.setSelected(true);
			detectDivergencePanel.add(detectDivergenceCheck);
			add(detectDivergencePanel);

			JPanel dbPanel = new JPanel();
			dbPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			dbCheck = new JCheckBox("Data (image, psf and result) in dB");
			dbCheck.setSelected(false);
			dbPanel.add(dbCheck);
			add(dbPanel);

			JPanel thresholdPanel = new JPanel();
			thresholdPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			thresholdCheck = new JCheckBox("Threshold:  ");
			thresholdCheck.setSelected(false);
			thresholdCheck.addItemListener(new ThresholdCheckItemListener());
			thresholdPanel.add(thresholdCheck);
			thresholdField = new JTextField("0.0", 6);
			thresholdField.setEnabled(false);
			thresholdField.addActionListener(new ThresholdFieldActionListener());
			thresholdPanel.add(thresholdField);
			add(thresholdPanel);

			JPanel gammaPanel = new JPanel();
			gammaPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel gammaLabel = new JLabel("Wiener filter gamma (suggest 0 [<.0001] to turn off, 0.0001-0.1 as tests)");
			gammaPanel.add(gammaLabel);
			gammaField = new JTextField("0.0", 6);
			gammaField.addActionListener(new GammaFieldActionListener());
			gammaPanel.add(gammaField);
			add(gammaPanel);

			JPanel filterXYPanel = new JPanel();
			filterXYPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel filterXYlabel = new JLabel("Low pass filter x and y, pixels (suggest 1, 0 to turn off)");
			filterXYlabel.setPreferredSize(gammaLabel.getPreferredSize());
			filterXYPanel.add(filterXYlabel);
			filterXYfield = new JTextField("1", 6);
			filterXYfield.addActionListener(new FilterXYFieldActionListener());
			filterXYPanel.add(filterXYfield);
			add(filterXYPanel);

			JPanel changeThreshPercentPanel = new JPanel();
			changeThreshPercentPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel changeThreshPercentLabel = new JLabel("Terminate iteration if mean delta < x% (suggest 0.01, 0 to turn off)");
			changeThreshPercentLabel.setPreferredSize(gammaLabel.getPreferredSize());
			changeThreshPercentPanel.add(changeThreshPercentLabel);
			changeThreshPercentField = new JTextField("0.01", 6);
			changeThreshPercentField.addActionListener(new ChangeThreshPercentFieldActionListener());
			changeThreshPercentPanel.add(changeThreshPercentField);
			add(changeThreshPercentPanel);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new OkButtonActionListener());
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel);
			validate();
		}

		private class ThresholdFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					wplThreshold = Double.parseDouble(thresholdField.getText());
				} catch (Exception ex) {
					IJ.error("Threshold must be a nonnegative value.");
				}
				if (wplThreshold < 0.0) {
					IJ.error("Threshold must be a nonnegative value.");
				}
			}
		}

		private class GammaFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					gamma = Double.parseDouble(gammaField.getText());
				} catch (Exception ex) {
					IJ.error("Gamma must be a nonnegative value.");
				}
				if (gamma < 0.0) {
					IJ.error("Gamma must be a nonnegative value.");
				}
			}
		}

		private class FilterXYFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					filterXY = Double.parseDouble(filterXYfield.getText());
				} catch (Exception ex) {
					IJ.error("Filter x and y must be a nonnegative value.");
				}
				if (filterXY < 0.0) {
					IJ.error("Filter x and y must be a nonnegative value.");
				}
			}
		}

		private class ChangeThreshPercentFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					changeThreshPercent = Double.parseDouble(changeThreshPercentField.getText());
				} catch (Exception ex) {
					IJ.error("Mean delta must be a nonnegative value.");
				}
				if (changeThreshPercent < 0.0) {
					IJ.error("Mean delta must be a nonnegative value.");
				}
			}
		}

		private class ThresholdCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (thresholdCheck.isSelected()) {
					thresholdField.setEnabled(true);
				} else {
					thresholdField.setEnabled(false);
					wplThreshold = -1;
				}
			}
		}

		private class OkButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (checkTextFields() == true) {
					dWPLOptions = new DoubleWPLOptions(gamma, filterXY, filterXY, filterXY, normalizeCheck.isSelected(), logMeanCheck.isSelected(), antiRingCheck.isSelected(), changeThreshPercent, dbCheck.isSelected(), detectDivergenceCheck.isSelected());
					fWPLOptions = new FloatWPLOptions((float) gamma, (float) filterXY, (float) filterXY, (float) filterXY, normalizeCheck.isSelected(), logMeanCheck.isSelected(), antiRingCheck.isSelected(), (float) changeThreshPercent, dbCheck.isSelected(), detectDivergenceCheck.isSelected());
					WPLOptionsFrame.setVisible(false);
					wplOptionsSet = true;
				}
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				WPLOptionsFrame.dispose();
				wplOptionsSet = false;
			}
		}

		private boolean checkTextFields() {
			try {
				gamma = Double.parseDouble(gammaField.getText());
			} catch (Exception ex) {
				IJ.error("Gamma must be a nonnegative value.");
			}
			if (gamma < 0.0) {
				IJ.error("Gamma must be a nonnegative value.");
			}

			try {
				filterXY = Double.parseDouble(filterXYfield.getText());
			} catch (Exception ex) {
				IJ.error("Filter x and y must be a nonnegative value.");
			}
			if (filterXY < 0.0) {
				IJ.error("Filter x and y must be a nonnegative value.");
			}

			try {
				changeThreshPercent = Double.parseDouble(changeThreshPercentField.getText());
			} catch (Exception ex) {
				IJ.error("Mean delta must be a nonnegative value.");
			}
			if (changeThreshPercent < 0.0) {
				IJ.error("Mean delta must be a nonnegative value.");
			}

			if (thresholdCheck.isSelected()) {
				try {
					wplThreshold = Double.parseDouble(thresholdField.getText());
				} catch (Exception ex) {
					IJ.error("Threshold must be a nonnegative value.");
					return false;
				}
				if (wplThreshold < 0.0) {
					IJ.error("Threshold must be a nonnegative value.");
					return true;
				}
			} else {
				wplThreshold = -1;
			}
			return true;
		}
	}

	private class PSFCreatePanel extends JPanel {

		private static final long serialVersionUID = -4018857998511946590L;

		public JTextField rowsField, columnsField;

		public PSFCreatePanel() {
		}

		private void init() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel label = new JLabel("Enter the number of PSFs in the form of 2D matrix (rows x columns)");
			labelPanel.add(label);
			add(labelPanel);
			JPanel textPanel = new JPanel();
			textPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			rowsField = new JTextField("2", 3);
			JLabel times = new JLabel(" x ");
			columnsField = new JTextField("2", 3);
			textPanel.add(rowsField);
			textPanel.add(times);
			textPanel.add(columnsField);
			add(textPanel);
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new OkButtonActionListener());
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel);
			validate();
		}

		private class OkButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (checkTextFields() == true) {
					PSFCreateFrame.dispose();
					PSFEditPanel panel = new PSFEditPanel();
					panel.init();
					PSFEditFrame = new JFrame("Edit Spatially Variant PSF");
					PSFEditFrame.addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							PSFEditFrame.dispose();
						}
					});
					PSFEditFrame.add(panel);
					PSFEditFrame.setResizable(true);
					PSFEditFrame.setSize(panel.totalSize);
					// PSFEditFrame.pack();
					PSFEditFrame.setLocationRelativeTo(null);
					PSFEditFrame.setVisible(true);
				}
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				PSFCreateFrame.dispose();
			}
		}

		private boolean checkTextFields() {
			try {
				psfRows = Integer.parseInt(rowsField.getText());
			} catch (Exception ex) {
				IJ.error("Number of rows must be a positive integer number.");
				return false;
			}
			if (psfRows < 1) {
				IJ.error("Number of rows must be a positive integer number.");
				return false;
			}
			try {
				psfColumns = Integer.parseInt(columnsField.getText());
			} catch (Exception ex) {
				IJ.error("Number of columns must be a positive integer number.");
				return false;
			}
			if (psfColumns < 1) {
				IJ.error("Number of columns must be a positive integer number.");
				return false;
			}
			return true;
		}
	}

	private class PSFEditPanel extends JPanel {

		private static final long serialVersionUID = 11865121724333240L;

		private final Dimension totalSize = new Dimension();

		JButton[][] buttons;

		JButton okButton, cancelButton;

		int counter;

		public PSFEditPanel() {
			totalSize.height = 200;
			totalSize.width = 350;
		}

		private void init() {
			setLayout(new BorderLayout());
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel label = new JLabel("Define all PSFs");
			labelPanel.add(label);
			add(labelPanel, BorderLayout.PAGE_START);
			JPanel tablePanel = new JPanel();
			counter = psfRows * psfColumns;
			tablePanel.setLayout(new GridLayout(psfRows, psfColumns));
			buttons = new JButton[psfRows][psfColumns];
			for (int r = 0; r < psfRows; r++) {
				for (int c = 0; c < psfColumns; c++) {
					buttons[r][c] = new JButton("Null");
					buttons[r][c].setToolTipText("Null");
					buttons[r][c].addActionListener(new PSFButtonsActionListener());
					buttons[r][c].setHorizontalAlignment(SwingConstants.LEFT);
					tablePanel.add(buttons[r][c]);
				}
			}
			add(tablePanel, BorderLayout.CENTER);
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			okButton = new JButton("OK");
			okButton.setEnabled(false);
			okButton.addActionListener(new OkButtonActionListener());
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel, BorderLayout.PAGE_END);
			validate();
		}

		private class PSFButtonsActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				OpenDialog od = new OpenDialog("Open file", "");
				String directory = od.getDirectory();
				String name = od.getFileName();
				if (name != null) {
					String path = directory + name;
					((JButton) e.getSource()).setText(path);
					((JButton) e.getSource()).setToolTipText(path);
					((JButton) e.getSource()).setIcon(new ImageIcon(path));
					counter--;
					if (counter == 0) {
						okButton.setEnabled(true);
					}
				}
			}
		}

		private class OkButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				imPSF = new ImagePlus[psfRows][psfColumns];
				Opener o = new Opener();
				for (int r = 0; r < psfRows; r++) {
					for (int c = 0; c < psfColumns; c++) {
						imPSF[r][c] = o.openImage(buttons[r][c].getText());
						ImageProcessor ipPSF = imPSF[r][c].getProcessor();
						if (ipPSF instanceof ColorProcessor) {
							IJ.showMessage("RGB images are not currently supported.");
							return;
						}
						if (imPSF[r][c].getStackSize() > 1) {
							IJ.showMessage("For 3D images use Parallel Iterative Deconvolution 3D");
							return;
						}
					}
				}
				PSFEditFrame.setVisible(false);
				editPSFButton.setEnabled(true);
			}
		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				PSFEditFrame.setVisible(false);
			}
		}
	}

	private class MainPanel extends JPanel {

		private static final long serialVersionUID = 3975356344081858245L;

		private final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

		private final Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);

		public MainPanel() {
		}

		private void init() {
			ToolTipManager.sharedInstance().setDismissDelay(20000);
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
			blurLabel.setPreferredSize(new Dimension(90, blurLabel.getPreferredSize().height));
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
			blurChoice.setToolTipText("<html>Choose a blurred image.</html>");
			blurPanel.add(blurChoice);
			// --------------------------------------------------------------
			JPanel psfPanel = new JPanel();
			psfPanel.setLayout(new GridLayout(2, 1));
			Border border = new TitledBorder(null, null, TitledBorder.LEFT, TitledBorder.TOP);
			psfPanel.setBorder(border);
			JPanel psfChoicePanel = new JPanel();
			psfChoicePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel psfLabel = new JLabel("PSF:");
			psfLabel.setPreferredSize(new Dimension(85, psfLabel.getPreferredSize().height));
			psfChoicePanel.add(psfLabel);
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
			psfChoice.setToolTipText("<html>Choose a PSF image.</html>");
			psfChoicePanel.add(psfChoice);
			psfPanel.add(psfChoicePanel);
			JPanel psfVariantPanel = new JPanel();
			psfVariantPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			variantPSFCheck = new JCheckBox("Spatially variant PSF");
			variantPSFCheck.setSelected(false);
			variantPSFCheck.addItemListener(new PSFCheckItemListener());
			psfVariantPanel.add(variantPSFCheck);
			definePSFButton = new JButton("Define");
			definePSFButton.addActionListener(new DefinePSFButtonActionListener());
			definePSFButton.setEnabled(false);
			psfVariantPanel.add(definePSFButton);
			editPSFButton = new JButton("Edit");
			editPSFButton.setEnabled(false);
			editPSFButton.addActionListener(new EditPSFButtonActionListener());
			psfVariantPanel.add(editPSFButton);
			psfPanel.add(psfVariantPanel);
			// --------------------------------------------------------------
			JPanel methodPanel = new JPanel();
			methodPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel methodLabel = new JLabel("Method:");
			methodLabel.setPreferredSize(new Dimension(90, methodLabel.getPreferredSize().height));
			methodPanel.add(methodLabel);
			methodChoice = new JComboBox(methodNames);
			methodChoice
					.setToolTipText("<html>Choose a method:<br><ul><li>MRNSD - Modified Residual Norm Steepest Descent.<br>This is a nonnegatively constrained algorithm.</li><li>WPL - Wiener Filter Preconditioned Landweber.<br>This is a nonnegatively constrained algorithm.</li><li>CGLS - Conjugate Gradient for Least Squares.</li><li>HyBR - Hybrid Bidiagonalization Regularization.</li></ul></html>");
			methodChoice.setSelectedIndex(0);
			methodChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			methodChoice.addActionListener(new MethodChoiceActionListener());
			methodPanel.add(methodChoice);
			optionsButton = new JButton("Options");
			optionsButton.addActionListener(new OptionsButtonActionListener());
			methodPanel.add(optionsButton);
			// --------------------------------------------------------------
			JPanel precondPanel = new JPanel();
			precondPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel precondLabel1 = new JLabel("Preconditioner:");
			precondLabel1.setPreferredSize(new Dimension(90, precondLabel1.getPreferredSize().height));
			precondPanel.add(precondLabel1);
			precondChoice = new JComboBox(precondNames);
			precondChoice.setSelectedIndex(0);
			precondChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			precondChoice.addActionListener(new PrecondChoiceActionListener());
			precondChoice.setToolTipText("<html>Choose a preconditioner:<br><ul><li>FFT preconditioner - based on the Fast Fourier Transform.</li><li>None - no preconditioner.</li></ul></html>");
			precondPanel.add(precondChoice);
			JLabel precondLabel2 = new JLabel("Tolerance:");
			precondPanel.add(precondLabel2);
			precondField = new JTextField("0.0", 5);
			precondField.addActionListener(new PrecondFieldActionListener());
			precondField.setToolTipText("<html>A tolerance to \"regularize\" the preconditioner.</html>");
			precondField.setEnabled(false);
			precondPanel.add(precondField);
			precondCheck = new JCheckBox("Auto");
			precondCheck.setToolTipText("<html>Automatic choice of a tolerance for preconditioner<br>(based on the Generalized Cross Validation).</html>");
			precondCheck.setSelected(true);
			precondCheck.addItemListener(new PrecondCheckItemListener());
			precondPanel.add(precondCheck);

			// --------------------------------------------------------------
			JPanel boundaryPanel = new JPanel();
			boundaryPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel boundaryLabel = new JLabel("Boundary:");
			boundaryLabel.setPreferredSize(new Dimension(90, boundaryLabel.getPreferredSize().height));
			boundaryPanel.add(boundaryLabel);
			boundaryChoice = new JComboBox(boundaryNames);
			boundaryChoice.setSelectedIndex(0);
			boundaryChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			boundaryChoice.setToolTipText("<html>Choose boundary conditions.</html>");
			boundaryPanel.add(boundaryChoice);
			// --------------------------------------------------------------
			JPanel resizingPanel = new JPanel();
			resizingPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel resizingLabel = new JLabel("Resizing:");
			resizingLabel.setPreferredSize(new Dimension(90, resizingLabel.getPreferredSize().height));
			resizingPanel.add(resizingLabel);
			resizingChoice = new JComboBox(resizingNames);
			resizingChoice.setSelectedIndex(0);
			resizingChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			resizingChoice.setToolTipText("<html>Choose resizing.</html>");
			resizingPanel.add(resizingChoice);
			// --------------------------------------------------------------
			JPanel outputPanel = new JPanel();
			outputPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel outputLabel = new JLabel("Output:");
			outputLabel.setPreferredSize(new Dimension(90, outputLabel.getPreferredSize().height));
			outputPanel.add(outputLabel);
			outputChoice = new JComboBox(outputNames);
			outputChoice.setSelectedIndex(0);
			outputChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			outputChoice.setToolTipText("<html>Choose a type of deblurred image.</html>");
			outputPanel.add(outputChoice);
			// --------------------------------------------------------------
			JPanel precisionPanel = new JPanel();
			precisionPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel precisionLabel = new JLabel("Precision:");
			precisionLabel.setPreferredSize(new Dimension(90, precisionLabel.getPreferredSize().height));
			precisionPanel.add(precisionLabel);
			precisionChoice = new JComboBox(precisionNames);
			precisionChoice.setSelectedIndex(0);
			precisionChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			precisionChoice.setToolTipText("<html>Choose precision.</html>");
			precisionPanel.add(precisionChoice);

			// --------------------------------------------------------------
			JPanel itersPanel = new JPanel();
			itersPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel itersLabel = new JLabel("Max number of iterations:");
			itersPanel.add(itersLabel);
			itersField = new JTextField("5", 3);
			itersField.addActionListener(new ItersFieldActionListener());
			itersField.setToolTipText("<html>The maximum number of iterations.</html>");
			itersPanel.add(itersField);
			itersCheck = new JCheckBox("Show iterations");
			itersCheck.setToolTipText("<html>Show restored image after each iteration.</html>");
			itersCheck.setSelected(false);
			itersPanel.add(itersCheck);

			// --------------------------------------------------------------
			JPanel threadsPanel = new JPanel();
			threadsPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
			JLabel threadsLabel = new JLabel("Number of threads (power of 2):  ");
			threadsPanel.add(threadsLabel);
			ConcurrencyUtils.setNumberOfProcessors(ConcurrencyUtils.concurrency());
			threadsField = new JTextField(Integer.toString(ConcurrencyUtils.getNumberOfProcessors()), 3);
			threadsField.addActionListener(new ThreadsFieldActionListener());
			threadsPanel.add(threadsField);
			// --------------------------------------------------------------
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
			deconvolveButton = new JButton("Deconvolve");
			deconvolveButton.addActionListener(new DeconvolveButtonActionListener());
			if (windowIDs == null) {
				deconvolveButton.setEnabled(false);
			}
			buttonPanel.add(deconvolveButton);
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new CancelButtonActionListener());
			buttonPanel.add(cancelButton);
			// --------------------------------------------------------------
			add(blurPanel);
			add(psfPanel);
			add(methodPanel);
			add(precondPanel);
			add(boundaryPanel);
			add(resizingPanel);			
			add(outputPanel);	
			add(precisionPanel);
			add(itersPanel);
			add(threadsPanel);
			add(buttonPanel);
			validate();
		}

		private class BlurChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				windowIDs = WindowManager.getIDList();
				if (windowIDs != null) {
					deconvolveButton.setEnabled(true);
				} else {
					deconvolveButton.setEnabled(false);
				}
			}
		}

		private class PsfChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				windowIDs = WindowManager.getIDList();
				if (windowIDs != null) {
					deconvolveButton.setEnabled(true);
				} else {
					deconvolveButton.setEnabled(false);
				}
			}
		}

		private class MethodChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				Method selMethod = Method.values()[methodChoice.getSelectedIndex()];
				if (selMethod == Method.WPL) {
					precondChoice.setEnabled(false);
					precondCheck.setEnabled(false);
					precondField.setEnabled(false);
					variantPSFCheck.setSelected(false);
					variantPSFCheck.setEnabled(false);
					definePSFButton.setEnabled(false);
					editPSFButton.setEnabled(false);
				} else {
					precondChoice.setEnabled(true);
					precondCheck.setEnabled(true);
					if (!precondCheck.isSelected()) {
						precondField.setEnabled(true);
					}
					variantPSFCheck.setEnabled(true);

				}
			}
		}

		private class PrecondChoiceActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				PreconditionerType selPrecond = PreconditionerType.values()[precondChoice.getSelectedIndex()];
				switch (selPrecond) {
				case FFT:
					precondCheck.setEnabled(true);
					if (precondCheck.isSelected() == false) {
						precondField.setEnabled(true);
					} else {
						precondField.setEnabled(false);
					}
					break;
				case NONE:
					precondField.setEnabled(false);
					precondCheck.setEnabled(false);
					break;
				}
			}
		}

		private class ItersFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					iters = Integer.parseInt(itersField.getText());
				} catch (Exception ex) {
					IJ.error("Number of iterations must be a positive integer.");
				}
				if (iters < 1) {
					IJ.error("Number of iterations must be a positive integer.");
				}
			}
		}

		private class ThreadsFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					threads = Integer.parseInt(threadsField.getText());
				} catch (Exception ex) {
					IJ.error("Number of threads must be power of 2.");
					return;
				}
				if (threads < 1) {
					IJ.error("Number of threads must be power of 2.");
					return;
				}
				if (!ConcurrencyUtils.isPowerOf2(threads)) {
					IJ.error("Number of threads must be power of 2.");
					return;
				}
				ConcurrencyUtils.setNumberOfProcessors(threads);
			}
		}

		private class PSFCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (variantPSFCheck.isSelected() == true) {
					definePSFButton.setEnabled(true);
					editPSFButton.setEnabled(false);
					psfChoice.setEnabled(false);
				} else {
					definePSFButton.setEnabled(false);
					editPSFButton.setEnabled(false);
					psfChoice.setEnabled(true);
				}
			}
		}

		private class PrecondCheckItemListener implements ItemListener {
			public void itemStateChanged(ItemEvent e) {
				if (precondCheck.isSelected() == true) {
					precondField.setEnabled(false);
				} else {
					precondField.setEnabled(true);
				}
			}
		}

		private class OptionsButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				final Method selMethod = Method.values()[methodChoice.getSelectedIndex()];
				switch (selMethod) {
				case CGLS:
					if (cglsOptionsSet == true) {
						CGLSOptionsFrame.setVisible(true);
					} else {
						CGLSOptionsPanel panel = new CGLSOptionsPanel();
						panel.init();
						CGLSOptionsFrame = new JFrame("CGLS options");
						CGLSOptionsFrame.addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent e) {
								CGLSOptionsFrame.dispose();
							}
						});
						CGLSOptionsFrame.add(panel);
						CGLSOptionsFrame.setResizable(false);
						CGLSOptionsFrame.pack();
						CGLSOptionsFrame.setLocationRelativeTo(null);
						CGLSOptionsFrame.setVisible(true);
					}
					break;
				case MRNSD:
					if (mrnsdOptionsSet == true) {
						MRNSDOptionsFrame.setVisible(true);
					} else {
						MRNSDOptionsPanel panel = new MRNSDOptionsPanel();
						panel.init();
						MRNSDOptionsFrame = new JFrame("MRNSD options");
						MRNSDOptionsFrame.addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent e) {
								MRNSDOptionsFrame.dispose();
							}
						});
						MRNSDOptionsFrame.add(panel);
						MRNSDOptionsFrame.setResizable(false);
						MRNSDOptionsFrame.pack();
						MRNSDOptionsFrame.setLocationRelativeTo(null);
						MRNSDOptionsFrame.setVisible(true);
					}
					break;
				case HyBR:
					if (hybrOptionsSet == true) {
						HyBROptionsFrame.setVisible(true);
					} else {
						HyBROptionsPanel panel = new HyBROptionsPanel();
						panel.init();
						HyBROptionsFrame = new JFrame("HyBR options");
						HyBROptionsFrame.addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent e) {
								HyBROptionsFrame.dispose();
							}
						});
						HyBROptionsFrame.add(panel);
						HyBROptionsFrame.setResizable(false);
						HyBROptionsFrame.pack();
						HyBROptionsFrame.setLocationRelativeTo(null);
						HyBROptionsFrame.setVisible(true);
					}
					break;
				case WPL:
					if (wplOptionsSet == true) {
						WPLOptionsFrame.setVisible(true);
					} else {
						WPLOptionsPanel panel = new WPLOptionsPanel();
						panel.init();
						WPLOptionsFrame = new JFrame("WPL options");
						WPLOptionsFrame.addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent e) {
								WPLOptionsFrame.dispose();
							}
						});
						WPLOptionsFrame.add(panel);
						WPLOptionsFrame.setResizable(false);
						WPLOptionsFrame.pack();
						WPLOptionsFrame.setLocationRelativeTo(null);
						WPLOptionsFrame.setVisible(true);
					}
					break;
				}

			}
		}

		private class PrecondFieldActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (precondCheck.isSelected() == true) {
					precTol = -1;
				} else {
					try {
						precTol = Double.parseDouble(precondField.getText());
					} catch (Exception ex) {
						IJ.error("Tolerance for preconditioner must be a number between 0 and 1.");
					}
					if ((precTol < 0) || (precTol > 1)) {
						IJ.error("Tolerance for preconditioner must be a number between 0 and 1.");
					}
				}
			}
		}

		private class DefinePSFButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				PSFCreatePanel panel = new PSFCreatePanel();
				panel.init();
				PSFCreateFrame = new JFrame("Create Spatially Variant PSF");
				PSFCreateFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						PSFCreateFrame.dispose();
					}
				});
				PSFCreateFrame.add(panel);
				PSFCreateFrame.setResizable(false);
				PSFCreateFrame.pack();
				PSFCreateFrame.setLocationRelativeTo(null);
				PSFCreateFrame.setVisible(true);
			}
		}

		private class EditPSFButtonActionListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				PSFEditFrame.setVisible(true);
			}
		}

		private class DeconvolveButtonActionListener implements ActionListener {
			private final Timer timer = new Timer();

			public void actionPerformed(ActionEvent e) {
				deconvolveThread = new Thread(new Runnable() {
					public void run() {
						imB = WindowManager.getImage((String) blurChoice.getSelectedItem());
						ImageProcessor ipB = imB.getProcessor();
						if (ipB instanceof ColorProcessor) {
							IJ.showMessage("RGB images are not currently supported.");
							return;
						}
						if (imB.getStackSize() > 1) {
							IJ.showMessage("For 3D images use Parallel Iterative Deconvolution 3D");
							return;
						}
						if (variantPSFCheck.isSelected() == false) {
							imPSF = new ImagePlus[1][1];
							imPSF[0][0] = WindowManager.getImage((String) psfChoice.getSelectedItem());
							ImageProcessor ipPSF = imPSF[0][0].getProcessor();
							if (ipPSF instanceof ColorProcessor) {
								IJ.showMessage("RGB images are not currently supported.");
								return;
							}
							if (imPSF[0][0].getStackSize() > 1) {
								IJ.showMessage("For 3D images use Parallel Iterative Deconvolution 3D.");
								return;
							}
						}
						if (!checkTextFields())
							return;
						setCursor(waitCursor);
						deconvolveButton.setEnabled(false);
						cancelButton.setEnabled(false);
						final Method selMethod = Method.values()[methodChoice.getSelectedIndex()];
						final PreconditionerType selPrecond = PreconditionerType.values()[precondChoice.getSelectedIndex()];
						final BoundaryType selBoundary = BoundaryType.values()[boundaryChoice.getSelectedIndex()];
						final ResizingType selResizing = ResizingType.values()[resizingChoice.getSelectedIndex()];
						final OutputType selOutput = OutputType.values()[outputChoice.getSelectedIndex()];
						final Precision selPrecision = Precision.values()[precisionChoice.getSelectedIndex()];
						clean_old_data();
						timer.reset().start();
						switch (selPrecision) {
						case DOUBLE:
							switch (selMethod) {
							case CGLS:
								if (selPrecond == PreconditionerType.NONE) {
									dcgls = new DoubleCGLS_2D(imB, imPSF, selBoundary, selResizing, selOutput, iters, cglsStoppingTol, false, itersCheck.isSelected());
									imX = dcgls.deblur(cglsThreshold);
								} else {
									dpcgls = new DoublePCGLS_2D(imB, imPSF, selBoundary, selResizing, selOutput, selPrecond, precTol, iters, cglsStoppingTol, false, itersCheck.isSelected());
									precondField.setText(String.format("%.4f", dpcgls.getPreconditionerTolerance()));
									imX = dpcgls.deblur(cglsThreshold);
								}
								timer.stop();
								break;
							case MRNSD:
								if (selPrecond == PreconditionerType.NONE) {
									dmrnsd = new DoubleMRNSD_2D(imB, imPSF, selBoundary, selResizing, selOutput, iters, mrnsdStoppingTol, false, itersCheck.isSelected());
									imX = dmrnsd.deblur(mrnsdThreshold);
								} else {
									dpmrnsd = new DoublePMRNSD_2D(imB, imPSF, selBoundary, selResizing, selOutput, selPrecond, precTol, iters, mrnsdStoppingTol, false, itersCheck.isSelected());
									precondField.setText(String.format("%.4f", dpmrnsd.getPreconditionerTolerance()));
									imX = dpmrnsd.deblur(mrnsdThreshold);
								}
								timer.stop();
								break;
							case HyBR:
								if (selPrecond == PreconditionerType.NONE) {
									dhybr = new DoubleHyBR_2D(imB, imPSF, selBoundary, selResizing, selOutput, PreconditionerType.NONE, -1, iters, dHyBROptions, itersCheck.isSelected());
									imX = dhybr.deblur(hybrThreshold);
								} else {
									dhybr = new DoubleHyBR_2D(imB, imPSF, selBoundary, selResizing, selOutput, selPrecond, precTol, iters, dHyBROptions, itersCheck.isSelected());
									precondField.setText(String.format("%.4f", dhybr.getPreconditionerTolerance()));
									imX = dhybr.deblur(hybrThreshold);
								}
								timer.stop();
								break;
							case WPL:
								dwpl = new DoubleWPL_2D(imB, imPSF[0][0], selBoundary, selResizing, selOutput, iters, dWPLOptions, itersCheck.isSelected());
								imX = dwpl.deblur(wplThreshold);
								timer.stop();
								break;
							}
							break;
						case SINGLE:
							switch (selMethod) {
							case CGLS:
								if (selPrecond == PreconditionerType.NONE) {
									fcgls = new FloatCGLS_2D(imB, imPSF, selBoundary, selResizing, selOutput, iters, (float) cglsStoppingTol, false, itersCheck.isSelected());
									imX = fcgls.deblur((float) cglsThreshold);
								} else {
									fpcgls = new FloatPCGLS_2D(imB, imPSF, selBoundary, selResizing, selOutput, selPrecond, (float) precTol, iters, (float) cglsStoppingTol, false, itersCheck.isSelected());
									precondField.setText(String.format("%.4f", fpcgls.getPreconditionerTolerance()));
									imX = fpcgls.deblur((float) cglsThreshold);
								}
								timer.stop();
								break;
							case MRNSD:
								if (selPrecond == PreconditionerType.NONE) {
									fmrnsd = new FloatMRNSD_2D(imB, imPSF, selBoundary, selResizing, selOutput, iters, (float) mrnsdStoppingTol, false, itersCheck.isSelected());
									imX = fmrnsd.deblur((float) mrnsdThreshold);
								} else {
									fpmrnsd = new FloatPMRNSD_2D(imB, imPSF, selBoundary, selResizing, selOutput, selPrecond, (float) precTol, iters, (float) mrnsdStoppingTol, false, itersCheck.isSelected());
									precondField.setText(String.format("%.4f", fpmrnsd.getPreconditionerTolerance()));
									imX = fpmrnsd.deblur((float) mrnsdThreshold);
								}
								timer.stop();
								break;
							case HyBR:
								if (selPrecond == PreconditionerType.NONE) {
									fhybr = new FloatHyBR_2D(imB, imPSF, selBoundary, selResizing, selOutput, PreconditionerType.NONE, -1, iters, fHyBROptions, itersCheck.isSelected());
									imX = fhybr.deblur((float) hybrThreshold);
								} else {
									fhybr = new FloatHyBR_2D(imB, imPSF, selBoundary, selResizing, selOutput, selPrecond, (float) precTol, iters, fHyBROptions, itersCheck.isSelected());
									precondField.setText(String.format("%.4f", fhybr.getPreconditionerTolerance()));
									imX = fhybr.deblur((float) hybrThreshold);
								}
								timer.stop();
								break;
							case WPL:
								fwpl = new FloatWPL_2D(imB, imPSF[0][0], selBoundary, selResizing, selOutput, iters, fWPLOptions, itersCheck.isSelected());
								imX = fwpl.deblur((float) wplThreshold);
								timer.stop();
								break;
							}
							break;
						}
						if (selPrecond != PreconditionerType.NONE) {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + shortMethodNames[methodChoice.getSelectedIndex()] + "_" + shortPrecondNames[precondChoice.getSelectedIndex()] + precondField.getText() + "_"
									+ shortBoundaryNames[boundaryChoice.getSelectedIndex()]));
						} else {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + shortMethodNames[methodChoice.getSelectedIndex()] + "_none_" + shortBoundaryNames[boundaryChoice.getSelectedIndex()]));
						}
						if (selMethod == Method.WPL) {
							imX.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + "_deblurred_" + shortMethodNames[methodChoice.getSelectedIndex()] + "_" + shortBoundaryNames[boundaryChoice.getSelectedIndex()]));
						}
						if (itersCheck.isSelected() == false) {
							imX.show();
						} else {
							blurChoice.removeItem("(deblurred)");
							psfChoice.removeItem("(deblurred)");
							blurChoice.addItem(imX.getTitle());
							psfChoice.addItem(imX.getTitle());
							blurChoice.revalidate();
							psfChoice.revalidate();
						}
						IJ.showStatus(timer.toString());
						setCursor(defaultCursor);
						deconvolveButton.setEnabled(true);
						cancelButton.setEnabled(true);
					}

				});
				deconvolveThread.setUncaughtExceptionHandler(new DefaultExceptionHandler());
				deconvolveThread.start();
			}
		}

		private class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

			public void uncaughtException(Thread t, Throwable e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				pw.flush();
				sw.flush();
				IJ.log(sw.toString());
				mainPanelFrame.dispose();
				ImagePlus.removeImageListener(getImageListener());
				clean_all();
			}

		}

		private class CancelButtonActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				mainPanelFrame.dispose();
				ImagePlus.removeImageListener(getImageListener());
				clean_all();
			}
		}

		private boolean checkTextFields() {
			try {
				iters = Integer.parseInt(itersField.getText());
			} catch (Exception ex) {
				IJ.error("Number of iterations must be a positive integer.");
				return false;
			}
			if (iters < 1) {
				IJ.error("Number of iterations must be a positive integer.");
				return false;
			}
			if (precondCheck.isSelected() == true) {
				precTol = -1;
			} else {
				try {
					precTol = Double.parseDouble(precondField.getText());
				} catch (Exception ex) {
					IJ.error("Tolerance for preconditioner must be a number between 0 and 1.");
					return false;
				}
				if ((precTol < 0) || (precTol > 1)) {
					IJ.error("Tolerance for preconditioner must be a number between 0 and 1.");
					return false;
				}
			}
			try {
				threads = Integer.parseInt(threadsField.getText());
			} catch (Exception ex) {
				IJ.error("Number of threads must be power of 2.");
				return false;
			}
			if (threads < 1) {
				IJ.error("Number of threads must be power of 2.");
				return false;
			}
			if (!ConcurrencyUtils.isPowerOf2(threads)) {
				IJ.error("Number of threads must be power of 2.");
				return false;
			}
			ConcurrencyUtils.setNumberOfProcessors(threads);
			return true;
		}
	}

	public static void main(String args[]) {

		new ImageJ();
		IJ.open("D:\\Research\\Images\\grain-blur.png");
		IJ.open("D:\\Research\\Images\\grain-psf.png");
		IJ.runPlugIn("Parallel_Iterative_Deconvolution_2D", null);
	}

}
