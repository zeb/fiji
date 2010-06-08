from java.awt import Color
from java.awt.event import TextListener
from java.awt import FileDialog
from java.io import File, FilenameFilter
import re
import ij
import sys
import os
import copy
 
global foldername
foldername=None

class Filter(FilenameFilter):
    'Pull out list entries ending in .tif'
    def accept(self, dir, name):
        reg = re.compile("\.tif$")
        m = reg.search(name)
        if m:
            return 1
        else:
            return 0

 

##############################
''' Start of actual script '''
##############################

fd = OpenDialog("Choose an Image in the Folder to Decompose",None)
file_name = fd.getFileName()
foldername=fd.getDirectory()
pivots=[]
Interp = ij.macro.Interpreter()
Interp.batchMode = True

tifs= [f for f in File(fd.getDirectory()).listFiles(Filter())]
tifs.sort(lambda a,b:cmp(a.name,b.name))
if len(tifs) > 0:    

    #make a folder to put our unalinged files in
    try:
        os.mkdir(foldername+"decomposed stacks/")
    except:
        pass #it probably already exists

    print "Images to process: ",tifs
    
    
    for i in tifs:
        im=Opener().openImage(foldername, i.name)
        im.show()
        name=i.name.rstrip('./tif')
        #make a folder to put our temp files in
        try:
            os.mkdir(foldername+"decomposed stacks/"+name+"/")
        except:
            pass #it probably already exists

        #write the files
        try:
            print "Saving "+foldername+'decomposed stacks/'+name+'/'+name+"0000.tif"
            IJ.run("Image Sequence... ", "format=TIFF name=["+name+"] start=0 digits=4 save=["+foldername+'decomposed stacks/'+name+'/'+name+"0000.tif] save=["+foldername+'decomposed stacks/'+name+'/'+name+"0000.tif]");
        except:
            IJ.error("Skipping "+i.name+" for now.  Rerun to try again")
        im.close()
        #print "save=["+foldername+'decomposed stacks/'+name+'/'+name+"0000.tif]"
        #sIJ.run("Image Sequence... ", "format=TIFF name=["+name+"] start=0 digits=4 save=["+foldername+'decomposed stacks/'+name+'/'+name+"0000.tif] save=["+foldername+'decomposed stacks/'+name+'/'+name+"0000.tif]");

    #make a folder to put our temp aligned files in
    try:
        os.mkdir(foldername+"temp/")
    except:
        pass #it probably already exists

    #make a folder to put our transforms in
    try:
        os.mkdir(foldername+"transform/")
    except:
        pass #it probably already exists

Interp.batchMode = False
print "Done"


