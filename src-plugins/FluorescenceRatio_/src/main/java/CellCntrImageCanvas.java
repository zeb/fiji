/*
 * CellCntrImageCanvas.java
 *
 * Created on November 22, 2005, 5:58 PM
 *
 */
/*
 *
 * @author Kurt De Vos � 2005
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.filter.RGBStackSplitter;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ListIterator;
import java.util.Vector;
import java.io.*;

/**
 *
 * @author Kurt De Vos
 */
public class CellCntrImageCanvas extends ImageCanvas {
	private Vector typeVector;
	private CellCntrMarkerVector currentMarkerVector;
	private FlurorescenceRatio cc;
	private ImagePlus img;
	private boolean delmode = false;
	private boolean showNumbers = false;
	private boolean showAll = false;
	private Font font = new Font("SansSerif", Font.PLAIN, 10);
	private int radius = 4;

	/** Creates a new instance of CellCntrImageCanvas */
	public CellCntrImageCanvas(ImagePlus img, Vector typeVector,
			FlurorescenceRatio cc, Vector displayList) {
		super(img);
		this.img = img;
		this.typeVector = typeVector;
		this.cc = cc;
		if (displayList != null)
			this.setDisplayList(displayList);
	}

	public void largerRadius() {
		radius++;
	}

	public void smallerRadius() {
		if (radius > 2)
			radius--;
	}

	public void mousePressed(MouseEvent e) {
		if (IJ.spaceBarDown() || Toolbar.getToolId() == Toolbar.MAGNIFIER
				|| Toolbar.getToolId() == Toolbar.HAND) {
			super.mousePressed(e);
			return;
		}

		if (currentMarkerVector == null) {
			IJ.error("Select a counter type first!");
			return;
		}

		int x = super.offScreenX(e.getX());
		int y = super.offScreenY(e.getY());
		if (!delmode) {
			CellCntrMarker m = new CellCntrMarker(x, y, img.getCurrentSlice());
			currentMarkerVector.addMarker(m);
		} else {
			CellCntrMarker m = currentMarkerVector.getMarkerFromPosition(
					new Point(x, y), img.getCurrentSlice());
			currentMarkerVector.remove(m);
		}
		repaint();
		cc.populateTxtFields();
	}

	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
	}

	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
	}

	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
	}

	public void mouseEntered(MouseEvent e) {
		super.mouseEntered(e);
		if (!IJ.spaceBarDown() | Toolbar.getToolId() != Toolbar.MAGNIFIER
				| Toolbar.getToolId() != Toolbar.HAND)
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
	}

	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
	}

	private Point point;
	private Rectangle srcRect = new Rectangle(0, 0, 0, 0);

	public void paint(Graphics g) {
		super.paint(g);
		srcRect = getSrcRect();
		Roi roi = img.getRoi();
		double xM = 0;
		double yM = 0;

		/*
		 * double magnification = super.getMagnification();
		 *
		 * try { if (imageUpdated) { imageUpdated = false; img.updateImage(); }
		 * Image image = img.getImage(); if (image!=null) g.drawImage(image, 0,
		 * 0, (int)(srcRect.width*magnification),
		 * (int)(srcRect.height*magnification), srcRect.x, srcRect.y,
		 * srcRect.x+srcRect.width, srcRect.y+srcRect.height, null); if (roi !=
		 * null) roi.draw(g); } catch(OutOfMemoryError e) {
		 * IJ.outOfMemory("Paint "+e.getMessage()); }
		 */

		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(1f));
		g2.setFont(font);

		ListIterator it = typeVector.listIterator();
		while (it.hasNext()) {
			CellCntrMarkerVector mv = (CellCntrMarkerVector) it.next();
			int typeID = mv.getType();
			g2.setColor(mv.getColor());
			ListIterator mit = mv.listIterator();
			while (mit.hasNext()) {
				CellCntrMarker m = (CellCntrMarker) mit.next();
				boolean sameSlice = m.getZ() == img.getCurrentSlice();
				if (sameSlice || showAll) {
					xM = ((m.getX() - srcRect.x) * magnification);
					yM = ((m.getY() - srcRect.y) * magnification);
					g2.drawOval((int) (xM - radius * magnification),
							(int) (yM - radius * magnification),
							(int) (2 * radius * magnification),
							(int) (2 * radius * magnification));
					if (showNumbers)
						g2.drawString(Integer.toString(typeID), (int) xM + 3,
								(int) yM - 3);
				}
			}
		}
	}

	public void removeLastMarker() {
		currentMarkerVector.removeLastMarker();
		repaint();
		cc.populateTxtFields();
	}

	public void measure() {
		try {
			// Produce raw data
			IJ.setColumnHeadings("Type\tMean\tStddev");
			ImageProcessor ip = img.getProcessor();
			Calibration cal = img.getCalibration();
			int W = ip.getWidth();
			int H = ip.getHeight();

			byte[] pixels = null;
			if (img.getNChannels() == 3) {
				RGBStackSplitter splitter = new RGBStackSplitter();
				splitter.split(img.getStack(), true);
				ImagePlus green = new ImagePlus("Green", splitter.green);
				green.setCalibration(cal);
				byte[] G = (byte[]) green.getProcessor().convertToByte(false)
						.getPixels();
				ImagePlus blue = new ImagePlus("Blue", splitter.blue);
				blue.setCalibration(cal);
				byte[] B = (byte[]) blue.getProcessor().convertToByte(false)
						.getPixels();
				ImagePlus red = new ImagePlus("Red", splitter.red);
				red.setCalibration(cal);
				byte[] R = (byte[]) red.getProcessor().convertToByte(false)
						.getPixels();
				double sumR = 0;
				double sumG = 0;
				double sumB = 0;
				for (int i = 0; i < H; i++) {
					for (int j = 0; j < W; j++) {
						int r = 0xff & R[i * W + j];
						int g = 0xff & G[i * W + j];
						int b = 0xff & B[i * W + j];
						sumR += r;
						sumG += g;
						sumB += b;
					}
				}
				if (sumR > sumG && sumR > sumB)
					pixels = R; // R
				else if (sumG > sumR && sumG > sumB)
					pixels = G; // G
				else
					pixels = B; // B
			}
			else
			{
				pixels = (byte[]) img.getProcessor().convertToByte(false).getPixels();
			}

			int radius2 = radius * radius;
			FileOutputStream rawData = new FileOutputStream(
					"FluorescenceRatioRawData.txt",
					(new File("FluorescenceRatioRawData.txt")).exists());
			PrintStream pRawData = new PrintStream(rawData);
			ListIterator it = typeVector.listIterator();
			double[] muobs = new double[3];
			double[] sigma2obs = new double[3];
			double A = 0;
			while (it.hasNext()) {
				CellCntrMarkerVector mv = (CellCntrMarkerVector) it.next();
				int typeID = mv.getType();
				String typeLabel = "";
				switch (typeID) {
				case 1:
					typeLabel = "Bg";
					break;
				case 2:
					typeLabel = "R1";
					break;
				case 3:
					typeLabel = "R2";
					break;
				}
				ListIterator mit = mv.listIterator();
				double muSum = 0, sigma2Sum = 0;
				double muN = 0;
				int ii = 0;
				while (mit.hasNext()) {
					CellCntrMarker m = (CellCntrMarker) mit.next();
					int xM = m.getX();
					int yM = m.getY();
					double value = 0;
					A = 0;
					for (int sx = -radius; sx <= radius; sx++) {
						int sx2 = sx * sx;
						for (int sy = -radius; sy <= radius; sy++) {
							int sy2 = sy * sy;
							if (sx2 + sy2 < radius2 && xM + sx >= 0
									&& xM + sx < W && yM + sy >= 0
									&& yM + sy < H) {
								int pixelValue = 0xff & pixels[(yM + sy) * W
										+ xM + sx];
								value += pixelValue;
								A++;
							}
						}
					}
					if (A > 0)
						value /= A;
					m.setValue(value);
					muSum += value;
					sigma2Sum += value * value;
					muN++;
					IJ.write(typeLabel + "\t" + value);
					pRawData.println(typeLabel + "\t" + value);
					ii++;
				}
				muobs[typeID - 1] = (muSum / muN);
				sigma2obs[typeID - 1] = (sigma2Sum / muN) - muobs[typeID - 1]
						* muobs[typeID - 1];
			}
			pRawData.println(" ");
			rawData.close();

			double[] mu = new double[3];
			mu[0] = muobs[0];
			mu[1] = muobs[1] - muobs[2];
			mu[2] = muobs[2] - muobs[0];

			double[] sigma2 = new double[3];
			sigma2[0] = sigma2obs[0];
			sigma2[1] = 2 * (sigma2obs[0] + sigma2obs[1]);
			sigma2[2] = 2 * (sigma2obs[0] + sigma2obs[2]);

			double ratio = (mu[1] + mu[2]) / mu[2];

			IJ.write(" ");
			IJ.write("CircleRadius\t" + radius);
			IJ.write("CircleArea\t" + A);
			IJ.write("Bg\t" + mu[0] + "\t" + Math.sqrt(sigma2[0]));
			IJ.write("R1\t" + (mu[1] + mu[2]) + "\t"
					+ Math.sqrt(sigma2[1] + sigma2[2]));
			IJ.write("R2\t" + mu[2] + "\t" + Math.sqrt(sigma2[2]));
			IJ.write(" ");
			IJ.write("Ratio\t" + ratio);

			FileOutputStream ratioData = new FileOutputStream(
					"FluorescenceRatio.txt",
					(new File("FluorescenceRatio.txt")).exists());
			PrintStream pRatioData = new PrintStream(ratioData);
			pRatioData.println(ratio);
			ratioData.close();
		} catch (FileNotFoundException e) {
			IJ.write("Cannot open raw data file for output");
			return;
		} catch (IOException e) {
			// Do nothing
		}
	}

	public Vector getTypeVector() {
		return typeVector;
	}

	public void setTypeVector(Vector typeVector) {
		this.typeVector = typeVector;
	}

	public CellCntrMarkerVector getCurrentMarkerVector() {
		return currentMarkerVector;
	}

	public void setCurrentMarkerVector(CellCntrMarkerVector currentMarkerVector) {
		this.currentMarkerVector = currentMarkerVector;
	}

	public boolean isDelmode() {
		return delmode;
	}

	public void setDelmode(boolean delmode) {
		this.delmode = delmode;
	}

	public boolean isShowNumbers() {
		return showNumbers;
	}

	public void setShowNumbers(boolean showNumbers) {
		this.showNumbers = showNumbers;
	}

	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

}
