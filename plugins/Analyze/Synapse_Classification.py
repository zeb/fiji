from java.awt import Color
from java.awt.event import TextListener
from java.awt import FileDialog
from java.io import File, FilenameFilter
import re
import ij
import sys
import copy
from math import sqrt
import random
 
global foldername
global syn_radius
foldername=None
syn_radius=5
subset_size = 300

class Filter(FilenameFilter):
    def accept(self, dir, name):
        reg = re.compile("\.tif$")
        m = reg.search(name)
        if m:
            return 1
        else:
            return 0

class Pivot(Object):
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
    i1=open(filename, "r")
    p=re.compile('^[0-9]')
    for line in i1.readlines():         
        line=(line.strip().strip('\n'))
        if p.match(line):
            s=Pivot()          
            line_args = (line.split('\t'))           
            s.index=int(line_args[0])#index 
           
            s.size=float(line_args[1])#size
            s.brightness=float(line_args[5])#brightness

            #x,y,z
            s.position=(float(line_args[11]),float(line_args[12]),float(line_args[13]))

            new_pivots.append(s)
    return new_pivots

def scan_pivots(image):
    new_pivots=[]
    w = image.getWidth();
    h = image.getHeight();
    d = image.getStackSize();
    for z in xrange(1,d+1):        
        pixels = image.getStack().getPixels(z)
        for y in xrange(0,h):
            for x in xrange(0,w):
                #get pixel value
                p=pixels[y*w + x]
                if p > 5000:
                    s=Pivot()   
                    s.index=len(new_pivots)
                    s.size=1
                    s.brightness=p
                    s.position=(x,y,z)
                    new_pivots.append(s)
    return new_pivots
    

def print_pivots(pivots, filename):
    i1=open(filename, "w")
    print >>i1, ' 	Volume (pixel^3)	Surface (pixel^2)	Nb of obj. voxels	Nb of surf. voxels	IntDen	Mean	StdDev	Median	Min	Max	X	Y	Z	Mean dist. to surf. (pixel)	SD dist. to surf. (pixel)	Median dist. to surf. (pixel)	XM	YM	ZM	BX	BY	BZ	B-width	B-height	B-depth'
    for p in pivots:
        print >>i1,str(p.index),'\t',str(p.size),'\t1\t1\t1\t',str(p.brightness),'\t1\t1\t1\t1\t1\t',str(p.position[0]),'\t',str(p.position[1]),'\t',str(p.position[2]),'\t0\t0\t0\t',str(p.position[0]),'\t',str(p.position[1]),'\t',str(p.position[2]),'\t',str(int(p.position[0])),'\t',str(int(p.position[1])),'\t',str(int(p.position[2])),'\t1\t1\t1'

def process_reference_channel(image):
    IJ.run("Maximum (3D) Point")
    i=IJ.getImage()
    pivots=scan_pivots(i)
    print_pivots(pivots,foldername+image.getTitle()+".pivots.xls")
    #IJ.run("Close All Without Saving")
    return pivots
    

def extract_features(pivot, image):
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

    for z in xrange(int(cz-syn_radius),int(cz+syn_radius)+1):
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

def extract_feature_loop(pivots, images):
    featurelist=[]
    for p in pivots:
        featurelist.append([])
    for ei,i in enumerate(images):
        im=Opener().openImage(foldername, i.name)
        for ep,p in enumerate(pivots):            
            featurelist[ep].extend(extract_features(p,im))
        im.close()
    return featurelist

def print_features(pivots, features, filename):
    fp=open(filename, "w")
    for ep,p in enumerate(pivots):
        fp.write('0')
        for ef,f in enumerate(features[ep]):
            fp.write(' '+str(ef+1)+':'+str(f))
        fp.write('\n')
    fp.close()

def make_ImageJ_ROI(pivots, images):
    macro = open(foldername+'imagejROI.txt','w')

    for p in pivots:
        print >>macro, 'setSlice('+str(int(p.position[2]))+');'
        print >>macro, 'makeRectangle('+str(int(p.position[0]-syn_radius))+','+str(int(p.position[1]-syn_radius))+','+str(int(2*syn_radius+1))+','+str(int(2*syn_radius+1))+');'
        print >>macro, 'roiManager("Add");'
 
def make_subset(pivots):
    subset = copy.copy(pivots)
    while len(subset) > subset_size:
        subset.pop(random.randint(0,len(subset)-1))
    return subset
       

##############################
''' Start of actual script '''
##############################

fd = FileDialog(IJ.getInstance(), "Choose Reference Channel or Pivot List", FileDialog.LOAD)
fd.show() #have the user pick a folder and something to gen pivots from
file_name = fd.getFile()
foldername=fd.getDirectory()
pivots=[]
if None != file_name:
    im=Opener().openImage(fd.getDirectory(), file_name)
    if im != None:
        im.show()
        pivots=process_reference_channel(im)
    else:        
        pivots=load_pivots(fd.getDirectory()+file_name)       

tifs= [f for f in File(fd.getDirectory()).listFiles(Filter())]

#tifimgs = [Opener().openImage(fd.getDirectory(), file.name) for file in File(fd.getDirectory()).listFiles(Filter())]
#tifimgs.sort(lambda a,b:cmp(a.getTitle(),b.getTitle()))
tifs.sort()
print "Images to process: ",tifs

subset=make_subset(pivots)
features = extract_feature_loop(pivots,tifs)
subset_features = extract_feature_loop(subset,tifs)
print_features(pivots,features,foldername+file_name+'Features.txt')
print_pivots(subset,foldername+file_name+'SubsetObjects.xls')
print_features(subset,subset_features,foldername+file_name+'SubsetFeatures.txt')
make_ImageJ_ROI(subset, tifs)
print "Done"


