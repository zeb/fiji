package fiji.plugin.constrainedshapes;

import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseWheelEvent;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.StackWindow;

/**
 * This class extends the ImageJ {@link StackWindow}, and only add the support for
 * multiple ROI, as in "one ROI per slice". When the user navigate from one slice
 * to another, the roi for the target slice is recalled from a list and displayed.
 * As he navigates away from the slice, its modifications are stored in the roi list.
 *  <p>
 *  This work is based on a much more complete extension: CustomStackWindow in
 *  the VIB package. See http://pacific.mpi-cbg.de/cgi-bin/gitweb.cgi?p=VIB.git;a=blob_plain;f=vib/segment/CustomStackWindow.java;hb=refs/heads/VIB
 *
 * @author Jean-Yves Tinevez
 *
 */
public class RoiListStackWindow extends StackWindow {

	protected static final long serialVersionUID = 1L;

	protected Roi[] roiList;
	protected int oldSlice;

	public RoiListStackWindow(ImagePlus imp) {
		super(imp);
		roiList = new Roi[imp.getStack().getSize() + 1];
	}

	public RoiListStackWindow(ImagePlus imp, ImageCanvas ic) {
		super(imp, ic);
		roiList = new Roi[imp.getStack().getSize() + 1];
	}

	/*
	 * AdjustmentListener interface
	 */
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		super.adjustmentValueChanged(e);
		updateRois();
	}

	public synchronized void updateRois() {
		updateRois(sliceSelector.getValue());
	}

	public synchronized void updateRois(int newSlice) {
		roiList[oldSlice] = imp.getRoi();
		oldSlice = newSlice;
		if (roiList[oldSlice] == null)
			imp.killRoi();
		else
			imp.setRoi(roiList[oldSlice]);
		repaint();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		super.mouseWheelMoved(e);
		updateRois();
	}


	/*
	 * SETTERS AND GETTERS
	 */

	/**
	 * Return the roi list stored by this window.
	 */
	public Roi[] getRoiList() {
		return roiList;
	}

	/**
	 * Set the <code>roi</code> to be stored on <code>slice</code>. If the slice number
	 * given in argument is larger than the numver of slice in the stack displayed by this
	 * Window, nothing is done.
	 * @param roi  The roi to store
	 * @param slice  The slice to store it in
	 */
	public void setRoi(Roi roi, int slice) {
		if ( (slice >= roiList.length) || (slice<0) ) return;
		roiList[slice] = roi;
	}
}