from java.awt import Color
from java.awt.event import TextListener
from java.awt import Dialog, FileDialog
from java.io import File, FilenameFilter
import re
import ij
import sys
import os
import copy
from fiji.util.gui import GenericDialogPlus
#import register_virtual_stack
 
##############################
''' Start of actual script '''
##############################

unalignedFile=IJ.getDirectory('current') 
transformFile=IJ.getDirectory('current') 
gd = GenericDialogPlus("Mass Transform Virtual Stack Sections")
gd.addDirectoryField("Unaligned Sections Folder:",unalignedFile,20)
gd.addDirectoryField("Transform Folder:",transformFile,20)
gd.showDialog()
if gd.wasCanceled():
    print "nevermind"
else:
    unalignedFile=gd.getNextString()
    transformFile=gd.getNextString()

    Interp = ij.macro.Interpreter() 
    Interp.batchMode = True

	#Folder structure
	#root
	#   unaligned
	#       channel 1
	#       channel2 etc
	#root
	#   aligned
	#       channel 1
	#       channel2 etc

    unalignedfolder=File(unalignedFile)
    rootfolder=unalignedfolder.getParentFile()

    tifs= [f for f in unalignedfolder.listFiles() if f.isDirectory()]
    tifs.sort(lambda a,b:cmp(a.name,b.name))

    if len(tifs) > 0:    

		#make a folder for aligned stacks
        try:
            os.mkdir(rootfolder.getPath()+"/aligned sections/")
        except:
            pass #it probably already exists

        #make a folder to put aligned stacks in
        try:
            os.mkdir(rootfolder.getPath()+"/aligned stacks/")
        except:
            pass #it probably already exists

        print "Images to process: ",tifs
		
		
        for i in tifs:
            name=i.name
			
            #make a folder to put aligned sections in
            try:
                os.mkdir(rootfolder.getPath()+"/aligned sections/"+name+"/")
            except:
                pass #it probably already exists

            #write the files
            try:
                #for debug use: IJ.error("source=["+unalignedFile+'/'+name+"] output=["+rootfolder.getPath()+"/aligned sections/"+name+"/] transforms=["+transformFile+"]")		
                if transformFile != '':	
                    IJ.run("Transform Virtual Stack Slices", "source=["+unalignedFile+'/'+name+"] output=["+rootfolder.getPath()+"/aligned sections/"+name+"/] transforms=["+transformFile+"]")
                else:
                    print unalignedFile+'/'+name
                    IJ.run("Image Sequence...", "open=["+unalignedFile+'/'+name+'/'+name+"_001.tif] number=999 starting=1 increment=1 scale=100 file=[] or=[] sort")                    
            except:
                IJ.error("Skipping "+name+" for now.  Rerun to try again")

            stack=ij.WindowManager.getCurrentImage()
            IJ.saveAs(stack,"Tiff",rootfolder.getPath()+"/aligned stacks/"+name)
			
			#close everything
            while ij.WindowManager.getCurrentImage():
                ij.WindowManager.getCurrentImage().close()
			


			
    Interp.batchMode = False
    print "Done"
	


