from java.awt import Color
from java.awt.event import TextListener
from java.awt import FileDialog
from java.io import File, FilenameFilter
from jarray import zeros
import re
import ij
import sys
import copy
from math import sqrt
from math import log
from math import ceil
import random
import os
import string
from fiji.util.gui import GenericDialogPlus

global syn_radius
syn_radius = 5
subset_size = 300

class Filter(FilenameFilter):
    def accept(self, dir, name):
        reg = re.compile("\.tif$")
        m = reg.search(name)
        if m:
            return 1
        else:
            return 0
    
def str_from_type(image_type):
    #"8-bit", "16-bit", "32-bit" or "RGB"
    if image_type == ij.ImagePlus.GRAY8:
        return "8-bit"
    elif image_type == ij.ImagePlus.GRAY16:
        return "16-bit"
    elif image_type == ij.ImagePlus.GRAY32:
        return "32-bit"    
    elif image_type == ij.ImagePlus.COLOR_256:
        return "RGB"
    return ""

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
                line_args = (line.split(','))         
                s.index=len(new_pivots)
                s.size=1
                s.brightness=0
                s.position=(float(line_args[0]),float(line_args[1]),float(line_args[2]))              

            new_pivots.append(s)
    return new_pivots

def scan_pivots(image):
    '''Takes as input a mostly black image with solitary local maxima, returns
    a list of pivots representing those maxima'''
    new_pivots=[]
    w = image.getWidth();
    h = image.getHeight();
    d = image.getStackSize();
    bitdepth = image.getBitDepth() 
    for z in xrange(1,d+1):        
        pixels = image.getStack().getPixels(z)
        for y in xrange(0,h):
            for x in xrange(0,w):
                #get pixel value
                p=pixels[y*w + x]
                #check if the pixel is above a low threshold
                if (bitdepth == 8 or bitdepth == 32) and p > 30:
                    scan=True
                elif bitdepth == 16 and p > 7000:
                    scan=True
                else:
                    scan=False
                if scan:
                    s=Pivot()   
                    s.index=len(new_pivots)
                    s.size=1
                    s.brightness=p
                    s.position=(x,y,z)
                    new_pivots.append(s)
    return new_pivots

def process_reference_channel(image):
    'normalize the given image, then find its local maxima and return a list of Pivots'
    IJ.run("Subtract Background...", "rolling=10 stack")
    IJ.run("Enhance Contrast", "saturated=0 normalize normalize_all")
    IJ.run("Maximum (3D) Point")

    i=IJ.getImage()
    pivots=scan_pivots(i)
    IJ.run("Close All Without Saving")
    return pivots

##############################
''' Start of actual script '''
##############################

referenceImage=""
featuresFolder=IJ.getDirectory('current')

gd = GenericDialogPlus("Montage from Pivot List")
gd.addFileField("Pivot List/Reference Channel:",referenceImage,20)
gd.addDirectoryField("Stacks Folder:",IJ.getDirectory('current'),20)
gd.showDialog()
if gd.wasCanceled():
    print "nevermind"

referenceImage=gd.getNextString()
featuresFolder=gd.getNextString()


pivots=[]
#turn off image showing - speeds things up a lot
Interp = ij.macro.Interpreter()
Interp.batchMode = True

if None != referenceImage:
    im=Opener().openImage(referenceImage)
    if im != None:
        im.show() #it was an image - find pivots within it
        pivots=process_reference_channel(im)
    else:        
        pivots=load_pivots(referenceImage) #load the pivot file we were given
print str(len(pivots))+" total pivots"
    

tifs= [f for f in File(featuresFolder).listFiles(Filter())]
tifs.sort()
print "Images to process: ",tifs

#make a folder to put our temp files in
try:
    os.mkdir(featuresFolder+"/tmp/")
except:
    pass #it probably already exists

#make a folder to put our results in
try:
    os.mkdir(featuresFolder+"/montage/")
except:
    pass #it probably already exists

#manager = ij.plugin.frame.RoiManager.getInstance() 
#if not manager:
    #The ROI manager isn't open, but it should be, so quit
#    IJ.error("ROI manager must be open w/ ROIs!")
#    quit()
#ROIs=manager.getRoisAsArray() 
#ROInum=manager.getCount()
IJ.showProgress(0.0);

#This next section iterates through everything and makes a small subvolume
#for each combination of image and pivot

for et,t in enumerate(tifs):
    IJ.showProgress(0.5*et/len(tifs));
    i = Opener().openImage(featuresFolder, t.name)
    print t.name
    #normalize the image
    i.show()
    IJ.run("Subtract Background...", "rolling=10 stack")
    IJ.run("Enhance Contrast", "saturated=0 normalize normalize_all")
    #we have to pad the image around the edges to avoid errors
    #first, top and bottom
    padi = IJ.createImage("Padding", str_from_type(i.getType())+" black", i.width, i.height, syn_radius)
    padi.show()
    concat=ij.plugin.Concatenator()
    i=concat.concatenate(padi,i,True)
    i=concat.concatenate(i,padi,False)

    #now, around the edges
    resizer=ij.plugin.CanvasResizer()
    newstack = resizer.expandStack(i.getImageStack(),i.width+2*syn_radius,i.height+2*syn_radius,syn_radius,syn_radius)
    i.setStack(t.name,newstack)

    #Okay, start processing ROIs
    for ep,p in enumerate(pivots):#ROIs):
        #get the next ROI
        roiname=str(p.position)#r.getName()
        slicenum=int(p.position[2])#manager.getSliceNumber(roiname)       
        #manager.select(er)
        i.setSlice(slicenum+syn_radius)

        #if isinstance(r,ij.gui.PointRoi): #if the ROIs are points, convert to centered rects
        #rb=r.getBounds()
        r=ij.gui.Roi(int(p.position[0]-syn_radius),int(p.position[1]-syn_radius),1+2*syn_radius,1+2*syn_radius)
        #move the ROI over to account for the padding we added earlier
        newr=r.clone()
        newr.setLocation(int(newr.getBounds().getX())+syn_radius,int(newr.getBounds().getY())+syn_radius)

        #Make a substack with the shifted ROI
        dupe=ij.plugin.filter.Duplicater()
        i.setRoi(newr)
        #print roiname, slicenum, newr.getBounds().getX(), newr.getBounds().getY(), i.width, i.height
        smalli = ij.plugin.Duplicator().run(i,slicenum,slicenum+2*syn_radius);
        #smalli = dupe.duplicateSubstack(i,i.getTitle()+" "+roiname,slicenum,slicenum+2*syn_radius)
        #IJ.saveAs(smalli,"Tiff",featuresFolder+"/tmp/"+str(ep).zfill(int(round(log(len(pivots))/log(10))))+i.getTitle()+" "+roiname)
        IJ.saveAs(smalli,"Tiff",featuresFolder+"/tmp/"+str(ep+1).zfill(int(ceil(log(len(pivots)+1)/log(10))))+i.getTitle()+" "+roiname)
        smalli.close()
    i.close()
    while ij.WindowManager.getCurrentImage():
        ij.WindowManager.getCurrentImage().close()

#all tifs have made subvolumes, so let's make some montage

roiFiles=[f for f in File(featuresFolder+"/tmp/").listFiles(Filter())]
import re    
print len(pivots)
for ep,p in enumerate(pivots):#for er,r in enumerate(ROIs): #for each pivot
    IJ.showProgress(0.5*ep/len(pivots));
    roiname=str(p.position)#r.getName() #the "name" (location) of the ROI
    #if the ROI manager added a -1 at the end of the ROI, remove the -1
    #roiname=re.split("-.$",roiname)[0]
    #roiCurr - all the channels from /tmp for this roi
    roiCurr = [roi for roi in roiFiles if string.find(roi.name,roiname) != -1]
    #sometimes, the previous section generates multiple files, which IJ denotes by 
    #appending -1.tif, -2.tif, etc.  Filter them out as well.    
    roiCurr = [roi for roi in roiCurr if re.search("-.\.tif$",roi.name)==None]
    #sort them in alphabetical order (instead of whichever is first on the disk)
    roiCurr.sort(lambda a,b:cmp(a.name,b.name))
    #whew

    #we want the montage text in white
    IJ.setForegroundColor(255,255,255)

    while ij.WindowManager.getCurrentImage(): #close everything
        ij.WindowManager.getCurrentImage().close()
    
    firstImg=None#firstImg = the one in red on all the other channels
    for img in roiCurr:
        i = Opener().openImage(featuresFolder+"/tmp/", img.name)
        i.show()
        i=IJ.getImage()
        if i.getBitDepth() == 32:
            i.getProcessor().setMinAndMax(0,255) #un-normalize the levels IJ sets automatically
        IJ.run("8-bit");#convert to 8bit
        if not firstImg: firstImg=ij.plugin.filter.Duplicater().duplicateStack(i, 'firstImg') #just make it gray
        if firstImg: 
            ij.plugin.ImageCalculator().calculate('Max create stack',i,firstImg) #make a red channel where red = first+current images
            comb=IJ.getImage()
            #merge them together again
            i.setStack(i.getTitle(),ij.plugin.RGBStackMerge().mergeStacks(i.getWidth(), i.getHeight(), i.getImageStackSize(), comb.getImageStack() , i.getImageStack() , i.getImageStack() , False))
        montage=ij.plugin.MontageMaker()
        montage.makeMontage(i, 1+2*syn_radius, 1, 4, 1, 1+2*syn_radius, 1, 2, False)
        
        i.close()   
        comb.close()    
        mont=ij.WindowManager.getImage("Montage")
        mont.setTitle(img.name.split(".tif")[0])      
    if firstImg: firstImg.close()
    IJ.run("Images to Stack")
    IJ.run("Make Montage...", "columns=1 rows="+str(len(roiCurr))+" scale=1 first=1 last="+str(len(roiCurr))+" increment=1 border=0 font=12 label use"); 
    IJ.selectWindow("Montage")
    mont=IJ.getImage()
    #IJ.saveAs(mont,"BMP",featuresFolder+"/montage/"+str(ep).zfill(int(round(log(len(pivots))/log(10))))+".bmp")
    IJ.saveAs(mont,"Tiff",featuresFolder+"/montage/"+str(ep+1).zfill(int(ceil(log(len(pivots)+1)/log(10))))+".tif")
    mont.close()
    while ij.WindowManager.getCurrentImage():
        ij.WindowManager.getCurrentImage().close()
IJ.showProgress(1.0);

Interp.batchMode = False

print "Done"


