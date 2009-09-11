package randomForestSegmentation;

/*
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
 */

import ij.IJ;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.WindowManager;
import ij.ImagePlus;
import ij.io.Opener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.image.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;

import javax.swing.*;

import ij.IJ;
import ij.gui.GenericDialog;
import java.awt.event.KeyListener;

public class SegmentationGUI implements KeyListener{
	
	private List<Roi> positiveExamples = new ArrayList< Roi>(); 
	private List<Roi> negativeExamples = new ArrayList< Roi>();
	private ImagePlus trainingImage;
	
	public void keyPressed(KeyEvent e){
		System.out.println("pressed" + e.getKeyChar());
	}
	public void keyReleased(KeyEvent e){
		System.out.println("released" + e.getKeyChar());
	}
	public void keyTyped(KeyEvent e){
		System.out.println("typed" + e.getKeyChar());
	}
	
	public SegmentationGUI(ImagePlus img){
		trainingImage = img;
		trainingImage.setProcessor("training image", trainingImage.getProcessor().convertToRGB());
		
		
		//IJ.showMessage("Please select a set of pixels and then type p for positive examples or n for negative examples");
		
	}
	
	private void addPositiveExamples(){
		//get selected pixels
		Roi r = trainingImage.getRoi();
		trainingImage.setColor(Color.GREEN);
		trainingImage.killRoi();
		r.drawPixels(trainingImage.getProcessor());
		trainingImage.updateAndDraw();
		positiveExamples.add(r);
	}
	
	private void addNegativeExamples(){
		//get selected pixels
		Roi r = trainingImage.getRoi();
		trainingImage.setColor(Color.RED);
		trainingImage.killRoi();
		r.drawPixels(trainingImage.getProcessor());
		trainingImage.updateAndDraw();
		negativeExamples.add(r);
	}
}
