from java.awt import Color
from java.awt.event import TextListener
from java.awt import Dialog, FileDialog
from java.io import File, FilenameFilter
import re
import ij
import sys
import copy
from math import sqrt
import random
import time
from fiji.util.gui import GenericDialogPlus
 
class Pivot(Object):
    '''A Pivot is a putative synapse, a location from which a short-range 
    analysis can be based'''
    def __init__(self, index=0,size=0,brightness=0,position=(0,0,0)):
        self.index=index
        self.size=size
        self.brightness=brightness
        self.position=position
        self.classification=None
        self.params=None
    
    def change_params(self,params):
        self.params=params
    
    def change_class(self,new_class):
        self.classification=new_class

def load_pivots(filename):
    new_pivots=[]
    use_excel = False
    if '.xls' in filename:
        use_excel = True
    i1=open(filename, "r")
    p=re.compile('^[0-9]')
    for line in i1.readlines():         
        line=(line.strip().strip('\n'))
        if p.match(line):
            s=Pivot()    
            if use_excel:      
                #excel values - 'index  size    brightness  x   y   z'
                line_args = (line.split('\t'))           
                s.index=int(line_args[0])#index 
               
                s.size=float(line_args[1])#size
                s.brightness=float(line_args[2])#brightness

                #x,y,z
                s.position=(float(line_args[3]),float(line_args[4]),float(line_args[5]))
            else:
                #plain csv values - 'x,y,z'
                #print line
                line_args = (line.split(','))         
                #print line_args
                s.index=len(new_pivots)
                s.size=1
                s.brightness=0
                s.position=(float(line_args[0]),float(line_args[1]),float(line_args[2]))              

            new_pivots.append(s)
    return new_pivots
    
def draw_single_pivot(pivot, image, newimage, radius):
    #find image bounds
    w = image.getWidth();
    h = image.getHeight();
    d = image.getStackSize();

    cx,cy,cz = pivot.position
    dx,dy,dz=0,0,0
    cenx,ceny,cenz=0.0,0.0,0.0
    bitdepth = image.getBitDepth() 

    for z in xrange(int(cz-radius),int(cz+radius)+1):
        if z <= 0 or z > d: continue
        dz = (z-cz)*(z-cz)
        pixels = image.getStack().getPixels(z)
        newpixels = newimage.getStack().getPixels(z)
        for y in xrange(int(cy-radius),int(cy+radius)+1):
            if y < 0 or y >= h: continue
            dy = (y-cy)*(y-cy)
            for x in xrange(int(cx-radius),int(cx+radius)+1):
                if x < 0 or x >= w: continue
                #Innermost loop, perform computation
                dx = (x-cx)*(x-cx)                  

                if dx+dy+dz > radius*radius: continue   

                newpixels[y*w + x]=pixels[y*w + x]
    return newimage

def draw_pivots(pivots,image,radius):
    'create a black copy of image, and copy over the bits within radius of pivots'
    #tmp=image.createEmptyStack()
    #imp=image.createImagePlus()
	
    if image.getBitDepth() == 8:
        imp=IJ.createImage("My new image", "8-bit black", image.getWidth(),image.getHeight(),image.getImageStackSize())
    if image.getBitDepth() == 16:
        imp=IJ.createImage("My new image", "16-bit black", image.getWidth(),image.getHeight(),image.getImageStackSize())
    if image.getBitDepth() == 32:
        imp=IJ.createImage("My new image", "32-bit black", image.getWidth(),image.getHeight(),image.getImageStackSize())
    if image.getBitDepth() == 24:
        imp=IJ.createImage("My new image", "RGB black", image.getWidth(),image.getHeight(),image.getImageStackSize());
    
	
            
    for ep,p in enumerate(pivots):            
        if ep % 10000 == 0:
            print "     Punctum",ep,"of",len(pivots)
        imp=draw_single_pivot(p,image,imp,radius)
    return imp
    
    


##############################
''' Start of actual script '''
##############################

refFile=IJ.getDirectory('current') 
objectFile=IJ.getDirectory('current') 

gd = GenericDialogPlus("Plot a Synaptic Subset")
gd.addFileField("Reference Channel:",refFile,20)
gd.addFileField("Object File:",objectFile,20)
gd.addNumericField("Keep Pixel Radius",5,4);
gd.showDialog()

refFile=gd.getNextString()
objectFile=gd.getNextString()
radius=gd.getNextNumber()

pivots=load_pivots(objectFile) 

Interp = ij.macro.Interpreter()
Interp.batchMode = True

image=Opener().openImage(refFile)
image.show()
IJ.run("Subtract Background...", "rolling=10 stack")
IJ.run("Enhance Contrast", "saturated=0 normalize normalize_all")

newimage=draw_pivots(pivots,image,radius)

IJ.saveAs(newimage,"Tiff",refFile+"subset.tif")


Interp.batchMode = False
print "Done"


