package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.TwoCircleRoi.ClickLocation;

import fiji.tool.AbstractTool;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.geom.Point2D;

public class TwoCircleTool extends AbstractTool implements MouseListener, MouseMotionListener {

	protected InteractionStatus status;
	protected Point2D startDrag;
	protected TwoCircleRoi roi;
	protected TwoCircleShape shape;
	protected InteractionStatus previousStatus;

	/**
	 * Enum type to specify the current user interaction status.
	 */
	public static enum InteractionStatus { FREE, MOVING_ROI, MOVING_C1, MOVING_C2, RESIZING_C1, RESIZING_C2, CREATING_C1, CREATING_C2 };

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {

			if (arg.equalsIgnoreCase("test")) {
				final int w = imp.getWidth();
				final int h = imp.getHeight();
				final int r = Math.min(w, h)/4;
				final int y = h/2;
				final int x1 = 3*w/8;
				final int x2 = 5*w/8;
				shape = new TwoCircleShape(x1,y,r,x2,y,r);
				roi = new TwoCircleRoi(shape);
				status = InteractionStatus.FREE;
				imp.setRoi(roi);
			} else {
				Roi currentRoi = imp.getRoi();
				if ( (currentRoi != null) && (currentRoi instanceof TwoCircleRoi) ) {
					roi = (TwoCircleRoi) currentRoi;
					shape = roi.getShape();
					status = InteractionStatus.FREE;
				} else {
					shape = new TwoCircleShape();
					roi = new TwoCircleRoi(shape);
					status = InteractionStatus.CREATING_C1;
				}
			}
		}
		super.run(arg);
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public String getToolIcon() {
		return "C000D38D39D3dD53D62D63D7dD8cD9cDc2Dc3Dc9DcaDd3Dd9" +
				"C000D29D2aD2cD2dD37D3aD3cD3eD43D44D45D47D48D4dD4e" +
				"D54D55D61D6dD6eD71D72D7cD7eD81D82D8dD9bDa1Da2Daa" +
				"DabDb1Db2DbaDbbDc1DcbDd4Dd5Dd7Dd8De3De4De5De7De8De9" +
				"C000C111C222C333C444D1aD1bD1cD28D2bD2eD35D36D3bD46" +
				"D49D4fD52D56D57D5dD5eD5fD6fD80D8bD8eD90D91D92D9aDa0" +
				"DacDd2Dd6DdaDe6Df5Df6Df7C444C555C666C777C888D19D1dD34" +
				"D3fD42D51D58D64D70D73D7bD7fD8aD9dDb0Db3Db9DbcDc4Dc8Dd1" +
				"DdbDe2DeaDf4Df8C888C999CaaaCbbbD18D1eD27D2fD33D41D4aD4c" +
				"D59D60D65D6cD7aD83D8fD9eDa3Da9Dc0Dc5Dc7DccDe1DebDf3Df9" +
				"CbbbCcccCdddCeeeCfff";
	}

	@Override
	public String getToolName() {
		return "Two circle shape";
	}

	/*
	 * MOUSE INTERACTIONS
	 */

	protected TwoCircleRoi getRoi(ImagePlus imp, boolean createIfNotExists) {
		Roi roi = imp.getRoi();
		if ((roi == null) || !(roi instanceof TwoCircleRoi)) {
			if (!createIfNotExists)
				return null;
			status = InteractionStatus.CREATING_C1;
			TwoCircleRoi newRoi = new TwoCircleRoi();
			imp.setRoi(newRoi);
			return newRoi;
		} else {
			return (TwoCircleRoi)roi;
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		ImagePlus imp = getImagePlus(e);
		final double x = getOffscreenXDouble(e);
		final double y = getOffscreenYDouble(e);
		final Point2D p = new Point2D.Double(x, y);

		TwoCircleRoi roi = getRoi(imp, true);
		ClickLocation cl = roi.getClickLocation(new Point2D.Double(e.getX(), e.getY()));
		shape = roi.getShape();

		switch (cl) {
		case OUTSIDE:
			switch (status) {
			case CREATING_C1:
				shape.setC1(p);
				break;
			case CREATING_C2:
				shape.setC2(p);
				break;
			default:
				// If drag is small, then we will kill this roi
			}
			break;
		default:
			previousStatus = status;
			status = cl.getInteractionStatus();
		}
		startDrag = p;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		final double x = getOffscreenXDouble(e);
		final double y = getOffscreenYDouble(e);
		final Point2D p = new Point2D.Double(x, y);
		final double[] params = shape.getParameters();

		switch (status) {
		case MOVING_ROI:
			params[0] += x-startDrag.getX();
			params[3] += x-startDrag.getX();
			params[1] += y-startDrag.getY();
			params[4] += y-startDrag.getY();
			break;
		case MOVING_C1:
			params[0] += x-startDrag.getX();
			params[1] += y-startDrag.getY();
			break;
		case MOVING_C2:
			params[3] += x-startDrag.getX();
			params[4] += y-startDrag.getY();
			break;
		case RESIZING_C1:
		case CREATING_C1:
			params[2] = shape.getC1().distance(p);
			break;
		case RESIZING_C2:
		case CREATING_C2:
			params[5] = shape.getC2().distance(p);
			break;
		}
		startDrag = p;
		roi = new TwoCircleRoi(shape);
		ImagePlus imp = getImagePlus(e);
		imp.setRoi(roi);
		IJ.showStatus(shape.toString());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		switch (status) {
		case CREATING_C1:
			status = InteractionStatus.CREATING_C2;
			previousStatus = InteractionStatus.CREATING_C1;
			break;
		case CREATING_C2:
			previousStatus = InteractionStatus.CREATING_C2;
			status = InteractionStatus.FREE;
			break;
		default:
			status = previousStatus;
			break;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) { }
	@Override
	public void mouseMoved(MouseEvent e) { }
	@Override
	public void mouseEntered(MouseEvent e) { }
	@Override
	public void mouseExited(MouseEvent e) { }
}