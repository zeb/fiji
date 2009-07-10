# Take a snapshot of the desktop every X miliseconds,
# and then make a stack out of it.
# Limited by RAM for speed, this plugin is intended
# for short recordings.

import thread
import time

from java.awt import Robot, Rectangle
from java.io import File, FilenameFilter
from java.util import Arrays
from java.util.concurrent import Executors

class Saver(Runnable):
	def __init__(self, i, dir, bounds, img):
		self.i = i
		self.dir = dir
		self.bounds = bounds
		self.img = img

	def run(self):
		# zero-pad up to 10 digits
		bi = None
		try:
			title = '%010d' % self.i
			FileSaver(ImagePlus(title, ColorProcessor(self.img))).saveAsTiff(self.dir + title + '.tif')
		except Exception, e:
			print e
			e.printStackTrace()
		if bi is not None: bi.flush()
		self.img.flush()

class TifFilter(FilenameFilter):
	def accept(self, dir, name):
		return name.endswith('.tif')

def run(title):
	gd = GenericDialog('Record Desktop')
	gd.addNumericField('Max. frames:', 50, 0)
	gd.addNumericField('Milisecond interval:', 300, 0)
	gd.addSlider('Start in (seconds):', 0, 20, 5)
	gd.addCheckbox("To file", False)
	gd.showDialog()
	if gd.wasCanceled():
		return
	n_frames = int(gd.getNextNumber())
	interval = gd.getNextNumber() / 1000.0 # in seconds
	delay = int(gd.getNextNumber())
	tofile = gd.getNextBoolean()

	dir = None
	executors = None
	if tofile:
		dc = DirectoryChooser("Directory to store image frames")
		dir = dc.getDirectory()
		if dir is None:
			return # dialog canceled
		executors = Executors.newFixedThreadPool(1)
	
	snaps = []

	try:
		while delay > 0:
			IJ.showStatus('Starting in ' + str(delay) + 's.')
			time.sleep(1) # one second
			delay -= 1
		IJ.showStatus('')
		System.out.println("Starting...")
		# start capturing
		robot = Robot()
		box = Rectangle(IJ.getScreenSize())
		start = System.currentTimeMillis() / 1000.0 # in seconds
		last = start
		intervals = []
		real_interval = 0
		# Initial shot
		i = 1
		fus = []
		img = robot.createScreenCapture(box)
		if tofile:
			fus.append(executors.submit(Saver(i, dir, box, img))) # will flush img
			i += 1
		else:
			snaps.append(img)

		while len(snaps) < n_frames and last - start < n_frames * interval:
			now = System.currentTimeMillis() / 1000.0 # in seconds
			real_interval = now - last
			if real_interval >= interval:
				last = now
				img = robot.createScreenCapture(box)
				if tofile:
					fus.append(executors.submit(Saver(i, dir, box, img))) # will flush img
					i += 1
				else:
					snaps.append(img)
				intervals.append(real_interval)
			else:
				time.sleep(interval / 5) # time in seconds
		# Create stack
		System.out.println("End")
		stack = None;
		if tofile:
			for fu in fus: fu.get() # wait on all
			stack = VirtualStack(box.width, box.height, None, dir)
			files = File(dir).list(TifFilter())
			Arrays.sort(files)
			for f in files:
				stack.addSlice(f)
		else:
			awt = snaps[0]
			stack = ImageStack(awt.getWidth(None), awt.getHeight(None), None)
			t = 0
			for snap,real_interval in zip(snaps,intervals):
				stack.addSlice(str(IJ.d2s(t, 3)), ImagePlus('', snap).getProcessor())
				snap.flush()
				t += real_interval

		ImagePlus("Desktop recording", stack).show()
	except Exception, e:
		print "Some error ocurred:"
		print e
		for snap in snaps: snap.flush()

thread.start_new_thread(run, ("Do it",))
