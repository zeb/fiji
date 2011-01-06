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
    
def draw_single_pivot(pivot, image, newimage, radius, rgb=None, colorpivot=None):
    #find image bounds
    w = image.getWidth();
    h = image.getHeight();
    d = image.getStackSize();

    cx,cy,cz = pivot.position
    dx,dy,dz=0,0,0
    cenx,ceny,cenz=0.0,0.0,0.0
    bitdepth = image.getBitDepth() 
    
    (r,g,b)=(random.random(),random.random(),random.random())
    m=min(r,g,b)
    (r,g,b)=(r-m,g-m,b-m)
    m=max(r,g,b)
    (r,g,b)=(r/m,g/m,b/m)
    
    if rgb == 'Red':
        color= r if not colorpivot else colorpivot.position[0]/255.0
    elif rgb == 'Green':
        color= g if not colorpivot else colorpivot.position[1]/255.0
    elif rgb == 'Blue':
        color= b if not colorpivot else colorpivot.position[2]/255.0
    else:
        color=1
    p=0
    
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
                
                newp=pixels[y*w + x]
                newd=newpixels[y*w + x]
                #if newp < 0 : p += 5
                #if p > 0: 
                #    print newp
                #    p -= 1
                if image.getType()==ImagePlus.GRAY8:
                    if newp<0: newp += 256
                    if newd<0: newd += 256
                    newp = int(newp*color)
                    if newp > 127: newp -= 256
                else:
                    if newp<0: newp += 65536
                    if newd<0: newd += 65536
                    newp = int(newp*color)
                    if newp > 32767: newp -= 65536
                #print color, pixels[y*w + x], newp
                #if  int(color*pixels[y*w + x]) < 0:
                #    print pixels[y*w + x],int(color*pixels[y*w + x])
                newpixels[y*w + x]=newp#int(color*pixels[y*w + x])
    return newimage

def draw_pivots(pivots,image,radius,rgb=None,colorpivots=[]):
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
        colorpivot=None
        if colorpivots != []:
            colorpivot=colorpivots[ep]           
        imp=draw_single_pivot(p,image,imp,radius,rgb,colorpivot)
    return imp
    
    


##############################
''' Start of actual script '''
##############################

random.seed(0)

refFile=IJ.getDirectory('current') 
objectFile=IJ.getDirectory('current') 
colorFile=IJ.getDirectory('current')

gd = GenericDialogPlus("Plot a Synaptic Subset")
gd.addFileField("Reference Channel:",refFile,20)
gd.addFileField("Object File:",objectFile,20)
gd.addNumericField("Keep Pixel Radius",4,3);
gd.addCheckbox("Make RGB channels?",True)
gd.addCheckbox("Randomize colors?",False)
gd.addFileField("Color File:",colorFile,20)
gd.showDialog()

refFile=gd.getNextString()
objectFile=gd.getNextString()
radius=gd.getNextNumber()

use_rgb=gd.getNextBoolean()
rand_colors=gd.getNextBoolean()
colorFile=gd.getNextString()

Interp = ij.macro.Interpreter()
Interp.batchMode = True

pivots=load_pivots(objectFile) 

if use_rgb:
    if rand_colors:
        color_pivots=[]
    else:
        color_pivots=load_pivots(colorFile) #color_pivots: pivots whose x,y,z info are really color information for another set of pivots
        if len(pivots) != len (color_pivots):
            IJ.error('Length of pivot list differs from length of pivot color.\nThis is not likely to end well.')


image=Opener().openImage(refFile)
image.show()

if not use_rgb:
    newimage=draw_pivots(pivots,image,radius)
    IJ.saveAs(newimage,"Tiff",refFile+"Obj2Img.tif")
else:
    for c in ['Red','Green','Blue']:
        newimage=draw_pivots(pivots,image,radius,c,color_pivots)
        IJ.saveAs(newimage,"Tiff",refFile+c+"Obj2Img.tif")    
            
    

#IJ.saveAs(newimage,"Tiff",refFile+"Redsubset.tif")


Interp.batchMode = False
print "Done"


