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
import random
import os
import string
 
global foldername
global syn_radius
syn_radius = 5
foldername=None
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

def copy_data(image,newimage,position):
    brightness=0

    im_type=image.getType()
    pixels = None
    w = image.getWidth();
    h = image.getHeight();
    d = image.getStackSize();
    cx,cy,cz = pivot.position
    #cx,cy,cz = cx-1,cy-1,cz
    dx,dy,dz=0,0,0
    cenx,ceny,cenz=0.0,0.0,0.0
    momi=0.0

    bitdepth = image.getBitDepth() 

    for z in xrange(int(position[2]),int(position[2]+newimage.getImageStackSize())+1):
        if z <= 0 or z > d: continue
        dz = (z-cz)*(z-cz)
        pixels = image.getStack().getPixels(z)
        for y in xrange(int(cy-syn_radius),int(cy+syn_radius)+1):
            if y < 0 or y >= h: continue
            dy = (y-cy)*(y-cy)
            for x in xrange(int(cx-syn_radius),int(cx+syn_radius)+1):
                if x < 0 or x >= w: continue
                #Innermost loop, perform computation
                dx = (x-cx)*(x-cx)

                #get pixel value
                p=pixels[y*w + x]
                if bitdepth==8:
                    p = (p & 0xff)
                elif bitdepth==16:
                    p = (p & 0xffff)
                elif bitdepth==24:
                    p = (p & 0xffffff)
                elif bitdepth==32:
                    p = (p & 0xffffffff)
                
                if dx+dy+dz > syn_radius*syn_radius: continue        

                #integrated brightness
                brightness +=  p

                #center of mass
                cenx += p*sqrt(dx)                
                ceny += p*sqrt(dy)    
                cenz += p*sqrt(dz)    

                #moment of inertia
                momi += p*(dx+dy+dz)

    if brightness:
        cenmass=1.0*sqrt(cenx*cenx+ceny*ceny+cenz*cenz)/brightness
        momi /= 1.0*brightness
    else:
        cenmass=sqrt(cenx*cenx+ceny*ceny+cenz*cenz)
    return [brightness,cenmass,momi]


##############################
''' Start of actual script '''
##############################

Interp = ij.macro.Interpreter()
Interp.batchMode = True

fd = FileDialog(IJ.getInstance(), "Choose Image Folder or ROI Gen Macro", FileDialog.LOAD)
fd.show() #have the user pick a folder and something to gen pivots from
file_name = fd.getFile()
foldername=fd.getDirectory()
pivots=[]
if None != file_name:
    pass
    #im=Opener().openImage(fd.getDirectory(), file_name)
    #if im != None:
    #    pass
    #else:   
        #TODO: read in ROI     
    #    pass
else:
    pass #error
    

tifs= [f for f in File(fd.getDirectory()).listFiles(Filter())]
tifs.sort()
print "Images to process: ",tifs

#make a folder to put our temp files in
try:
    os.mkdir(foldername+"tmp/")
except:
    pass #it probably already exists

#make a folder to put our results in
try:
    os.mkdir(foldername+"montage/")
except:
    pass #it probably already exists

manager = ij.plugin.frame.RoiManager.getInstance() 
if not manager:
    #The ROI manager isn't open, but it should be, so quit
    quit()
ROIs=manager.getRoisAsArray() 
ROInum=manager.getCount()
IJ.showProgress(0.0);

for et,t in enumerate(tifs):
    IJ.showProgress(0.5*et/len(tifs));
    i = Opener().openImage(foldername, t.name)
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
    for er,r in enumerate(ROIs):
        #get the next ROI
        roiname=r.getName()
        slicenum=manager.getSliceNumber(roiname)       
        manager.select(er)
        i.setSlice(slicenum+syn_radius)

        #move the ROI over to account for the padding we added earlier
        newr=r.clone()
        newr.setLocation(int(newr.getBounds().getX())+syn_radius,int(newr.getBounds().getY())+syn_radius)

        #Make a substack with the shifted ROI
        dupe=ij.plugin.filter.Duplicater()
        i.setRoi(newr)
        smalli = dupe.duplicateSubstack(i,i.getTitle()+" "+r.getName(),slicenum,slicenum+2*syn_radius)
        IJ.saveAs(smalli,"Tiff",foldername+"tmp/"+smalli.getTitle())
        smalli.close()
    i.close()
    while ij.WindowManager.getCurrentImage():
        ij.WindowManager.getCurrentImage().close()

#all tifs have made subvolumes, so let's make some montage

roiFiles=[f for f in File(foldername+"tmp/").listFiles(Filter())]
import re    
for er,r in enumerate(ROIs): #for each pivot
    roiname=r.getName() #the "name" (location) of the ROI
    #if the ROI manager added a -1 at the end of the ROI, remove the -1
    roiname=re.split("-.$",roiname)[0]
    #roiCurr - all the channels from /tmp for this roi
    roiCurr = [roi for roi in roiFiles if string.find(roi.name,roiname) != -1]
    #sometimes, the previous section generates multiple files, which IJ denotes by 
    #appending -1.tif, -2.tif, etc.  Filter them out as well.    
    roiCurr = [roi for roi in roiCurr if re.search("-.\.tif$",roi.name)==None]
    #sort them in alphabetical order (instead of whichever is first on the disk)
    roiCurr.sort(lambda a,b:cmp(a.name,b.name))
    #whew

    while ij.WindowManager.getCurrentImage(): #close everything
        ij.WindowManager.getCurrentImage().close()
    
    firstImg=None#firstImg = the one in red on all the other channels
    for img in roiCurr:
        i = Opener().openImage(foldername+"tmp/", img.name)
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
    montage=ij.plugin.MontageMaker()
    tmp=IJ.getImage()
    montage.makeMontage(tmp, 1, len(roiCurr), 1, 1, len(roiCurr), 1, 0, True)    
    IJ.selectWindow("Montage")
    mont=IJ.getImage()
    IJ.saveAs(mont,"Tiff",foldername+"montage/"+str(er)+".tif")
    mont.close()
    while ij.WindowManager.getCurrentImage():
        ij.WindowManager.getCurrentImage().close()
IJ.showProgress(1.0);

Interp.batchMode = False

print "Done"


