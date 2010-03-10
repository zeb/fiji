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
import time
 
global foldername
global syn_radius
foldername=None
syn_radius=5
small_radius=2
subset_size = 300

class Filter(FilenameFilter):
    'Pull out list entries ending in .tif'
    def accept(self, dir, name):
        reg = re.compile("\.tif$")
        m = reg.search(name)
        if m:
            return 1
        else:
            return 0

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
    i1=open(filename, "r")
    p=re.compile('^[0-9]')
    for line in i1.readlines():         
        line=(line.strip().strip('\n'))
        if p.match(line):
            s=Pivot()          
            line_args = (line.split('\t'))           
            s.index=int(line_args[0])#index 
           
            s.size=float(line_args[1])#size
            s.brightness=float(line_args[2])#brightness

            #x,y,z
            s.position=(float(line_args[3]),float(line_args[4]),float(line_args[5]))

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
    

def print_pivots(pivots, filename):
    i1=open(filename, "w")
    i1.write('Index 	Volume (pixel^3)    brightness    X	Y	Z\n')
    for p in pivots:
        i1.write(str(p.index)+'\t'+str(p.size)+'\t'+str(p.brightness)+'\t'+str(p.position[0])+'\t'+str(p.position[1])+'\t'+str(p.position[2])+'\n')

def process_reference_channel(image):
    'normalize the given image, then find its local maxima and return a list of Pivots'
    IJ.run("Subtract Background...", "rolling=10 stack")
    IJ.run("Enhance Contrast", "saturated=0 normalize normalize_all")
    IJ.run("Maximum (3D) Point")

    i=IJ.getImage()
    pivots=scan_pivots(i)
    print_pivots(pivots,foldername+image.getTitle()+".pivots.xls")   
    IJ.run("Close All Without Saving")
    return pivots
    

def extract_features(pivot, image):
    brightness=0
    prox=0

    im_type=image.getType()
    pixels = None
    w = image.getWidth();
    h = image.getHeight();
    d = image.getStackSize();
    cx,cy,cz = pivot.position
    dx,dy,dz=0,0,0
    cenx,ceny,cenz=0.0,0.0,0.0
    momi=0.0
    maxloc=[-1,-1,-1]
    maxpix=-1

    bitdepth = image.getBitDepth() 

    for z in xrange(int(cz-syn_radius),int(cz+syn_radius)+1):
        if z <= 0 or z > d: continue
        dz = (z-cz)*(z-cz)
        if z>cz: sz = z-cz 
        else: sz = cz-z
        pixels = image.getStack().getPixels(z)
        for y in xrange(int(cy-syn_radius),int(cy+syn_radius)+1):
            if y < 0 or y >= h: continue
            dy = (y-cy)*(y-cy)
            if y> cy: sy= y-cy
            else: sy = cy-y
            for x in xrange(int(cx-syn_radius),int(cx+syn_radius)+1):
                if x < 0 or x >= w: continue
                #Innermost loop, perform computation
                dx = (x-cx)*(x-cx)
                if x > cx: sx = x - cx 
                else: sx = cx - x
                    

                #get pixel value
                p=pixels[y*w + x]
                if bitdepth==8:
                    p = (p & 0xff)
                elif bitdepth==16:
                    p = (p & 0xffff)
                
                if dx+dy+dz > syn_radius*syn_radius: continue        

                #integrated brightness
                brightness +=  p

                #center of mass
                cenx += p*sx                
                ceny += p*sy    
                cenz += p*sz    

                #moment of inertia
                momi += p*(dx+dy+dz)

                #proximity brightness: brightness/dist^2
                prox += p/(1+dx+dy+dz)

                #max brightness?
                if p > maxpix:
                    maxpix=p
                    maxloc = [x,y,z]
          
    #now do a small search around the location of the maximum intensity
    brightness2=0
    dist=sqrt( (maxloc[2]-cz)*(maxloc[2]-cz) + (maxloc[1]-cy)*(maxloc[1]-cy) + (maxloc[0]-cx)*(maxloc[0]-cx))
    for z in xrange(int(maxloc[2]-small_radius),int(maxloc[2]+small_radius)+1):
        if z <= 0 or z > d: continue
        dz = (z-maxloc[2])*(z-maxloc[2])
        if z>maxloc[2]: sz = z-maxloc[2] 
        else: sz = maxloc[2]-z
        pixels = image.getStack().getPixels(z)
        for y in xrange(int(maxloc[1]-small_radius),int(maxloc[1]+small_radius)+1):
            if y < 0 or y >= h: continue
            dy = (y-maxloc[1])*(y-maxloc[1])
            if y> maxloc[1]: sy= y-maxloc[1]
            else: sy = maxloc[1]-y
            for x in xrange(int(maxloc[0]-small_radius),int(maxloc[0]+small_radius)+1):
                if x < 0 or x >= w: continue
                #Innermost loop, perform computation
                dx = (x-maxloc[0])*(x-maxloc[0])
                if x > maxloc[0]: sx = x - maxloc[0] 
                else: sx = maxloc[0] - x    

                #get pixel value
                p=pixels[y*w + x]
                if bitdepth==8:
                    p = (p & 0xff)
                elif bitdepth==16:
                    p = (p & 0xffff)
                
                if dx+dy+dz > small_radius*small_radius: continue        

                #integrated brightness
                brightness2 +=  p                    

    if brightness:
        cenmass=1.0*sqrt(cenx*cenx+ceny*ceny+cenz*cenz)/brightness
        momi /= 1.0*brightness
    else:
        cenmass=sqrt(cenx*cenx+ceny*ceny+cenz*cenz)
    
    return [brightness,cenmass,momi,brightness2,dist]
    #return [prox]

def extract_feature_loop(pivots, images):
    featurelist=[]
    for p in pivots:
        featurelist.append([])
    for ei,i in enumerate(images):
        print "Extracting features from",i.name,",",ei,"of",len(images)
        im=Opener().openImage(foldername, i.name)
        #normalize the image
        im.show()
        IJ.run("Subtract Background...", "rolling=10 stack")
        IJ.run("Enhance Contrast", "saturated=0 normalize normalize_all")
        for ep,p in enumerate(pivots):            
            if ep % 10000 == 0:
                print "     Punctum",ep,"of",len(pivots)
            featurelist[ep].extend(extract_features(p,im))
        im.close()
    return featurelist

def print_features(pivots, features, filename):
    'prints a training set in libSVM format, complete with default class identity'
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
    subset=[]
    maxlen=len(pivots)
    for i in range(0,subset_size):
        subset.append(pivots[random.randint(0,maxlen-1)])
    return subset
    
def printSimpleFeatures(filename, features):
    'Prints a simple comma-separated list of features'
    f=open(filename,'w')
    for r in features:
        for ec,c in enumerate(r):
            if ec > 0: f.write(', ')
            f.write(str(c))
        f.write('\n')
    f.close()     

##############################
''' Start of actual script '''
##############################

fd = OpenDialog("Choose Reference Image or Pivot List",None)
file_name = fd.getFileName()
foldername=fd.getDirectory()
pivots=[]
Interp = ij.macro.Interpreter()
Interp.batchMode = True

if None != file_name:
    im=Opener().openImage(fd.getDirectory(), file_name)
    if im != None:
        im.show()
        pivots=process_reference_channel(im)
    else:        
        pivots=load_pivots(fd.getDirectory()+file_name)       
print str(len(pivots))+" total pivots"

tifs= [f for f in File(fd.getDirectory()).listFiles(Filter())]
tifs.sort(lambda a,b:cmp(a.name,b.name))
print "Images to process: ",tifs

features = extract_feature_loop(pivots,tifs)
printSimpleFeatures(foldername+file_name+'Features.txt',features)

#Uncomment the following to extract and print out a subset_size list of random pivots
#subset=make_subset(pivots)
#subset_features = extract_feature_loop(subset,tifs)
#print_pivots(subset,foldername+file_name+'SubsetObjects.xls')
#printSimpleFeatures(foldername+file_name+'SubsetFeatures.txt',subset_features)
#make_ImageJ_ROI(subset, tifs)

Interp.batchMode = False
print "Done"


