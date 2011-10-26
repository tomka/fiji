from ij.io import DirectoryChooser
from ij import ImageStack
from ij import CompositeImage
import os
import tempfile
import shutil
import re
import xml.dom.minidom
import time
from javax.swing import JButton, JScrollPane, JPanel, JComboBox, JLabel, JFrame, JTextField, JCheckBox
from java.awt import Color, GridLayout
from java.awt.event import ActionListener
from java.util import ArrayList
from loci.plugins import LociImporter
from loci.plugins import BF
from loci.plugins.in import ImporterOptions
import Stitch_Image_Collection
import stitching
import stitching.model

# Global variables
useSystemTmpFolder = True
deleteTempFolder = True
saveStitchedChannels = False
showReference = False
showResult = True
tmpDir = ""
srcDir = ""
sourceFiles = []
referenceFiles = {}
referenceFilesMetaData = {}
referenceFilesCalibration = {}
sourceFileInfos = {}
tilingInfoFile = "MATL_Mosaic.log"
tilingInfoSuffix = "_01"
numChannels = -1
referenceChannel = -1
tilingCongigFile = "TilingConfiguration.txt"
xTiles = 0
yTiles = 0
dim = 3
rgbOrder = "rgb"
alpha = 1.5
thresholdR = 0.6
thresholdDisplacementRelative = 2.5
thresholdDisplacementAbsolute = 3.5
fusionMethod = "Linear Blending"
#fusionMethod = "None"
handleRGB = "Red, Green and Blue"
invertXOffset = False
invertYOffset = False

# Choose a directory with lots of stacks
dc = DirectoryChooser("Choose directory with stacks")
srcDir = dc.getDirectory()

def log(message):
	print message
	time.sleep(0.001)

# A closs for storing image information
class ImageInfo():
	def __init__(self, filename, xpos, ypos):
		self.filename = filename
		self.xpos = xpos
		self.ypos = ypos
		self.xOffset = 0.0
		self.yOffset = 0.0
	def __str__(self):
		return "ImageInfo: " + self.filename + " is at positon (" + str(self.xpos) + ", " + str(self.ypos) + ")"

# Get source files
def getSourceFiles(extFilter):
	global sourceFiles
	log("looking gor source files, using regular expression: " + extFilter)
	pattern = re.compile(extFilter)
	for filename in os.listdir(srcDir):
		# don't allow folders
		if os.path.isdir(srcDir + filename):
			continue
		# check agains regular expression
		if (pattern.match(filename) is None):
			continue
		sourceFiles.append(filename)

# Prepends channel information to a file name and appends .tiff
def getChannelFileName(filename, channel):
	return "C-" + str(channel) + "_" + filename + ".tiff"

# Creates a name for a channel configuration file based on a
# reference file name.
def getChannelConfigName(referenceFile, channel):
	return referenceFile + "_C-" + str(channel)

# Load the stacks and extract the different channels into
# separate files
def extractChannels(channel):
	global referenceFiles
	global numChannels
	log("extracting channels from files")
	# check out all files in source folder
	for filename in sourceFiles:
		log("\tloading " + filename)
		# Import the image
		path = srcDir + filename
		options = ImporterOptions()
		options.setId(path)
		options.setSplitChannels(True)
		options.setWindowless(True)
		imps = BF.openImagePlus(options)
		numChannels = len(imps)
		log("\t\tsplitting files into " + str(numChannels) + " channel(s)")
		# Get the requested channel, save it and close everything again
		if numChannels <= channel:
			log("\t\terror: requested channel number (" + str(channel) + ") is larger than available channels")
			raise StandardError("Requested Channel number too large")
		for i in range(0, numChannels):
			name = getChannelFileName(filename, i)
			if i == channel:
				# Remember the reference file name and the original file name
				referenceFiles[name] = filename
				referenceFilesMetaData[name] = imps[i].getProperty("Info")
				referenceFilesCalibration[name] = imps[i].getCalibration()
			IJ.saveAs(imps[i], "tif", tmpDir + "/" + name)
			#imps[i].close()

# Iterates the xml document and looks for image information
# for a specified filename
def findImageInfo(doc, filename):
	origFilename = filename
	dotIdx = filename.rfind(tilingInfoSuffix)
	if (dotIdx != -1):
		tailPos = dotIdx + len(tilingInfoSuffix)
		filename = filename[0:dotIdx] + filename[tailPos:len(filename)]
	imageElement = None
	for node in doc.getElementsByTagName('ImageInfo'):
		for e in node.childNodes:
			if e.nodeType == e.ELEMENT_NODE and e.localName == "Filename":
				for f in e.childNodes:
					if f.nodeType == f.TEXT_NODE:
						currentFileName = f.nodeValue.strip()
						if currentFileName == filename:
							 # Found correct node
							 log("\tfound XML node for file " + filename + " (corrected from: " + origFilename + ")")
							 imageElement = node
	if imageElement == None:
		log("\tfount no position information information in XML document for file " + origFilename)
		return None
	# Read out position information
	xPos = 0.0
	yPos = 0.0
	xPosFound = False
	yPosFound = False
	for e in imageElement.childNodes:
		if e.nodeType == e.ELEMENT_NODE and e.localName == "XPos":
			for f in e.childNodes:
				if f.nodeType == f.TEXT_NODE:
					xPos = float(f.nodeValue.strip())
					xPosFound = True
		if e.nodeType == e.ELEMENT_NODE and e.localName == "YPos":
			for f in e.childNodes:
				if f.nodeType == f.TEXT_NODE:
					yPos = float(f.nodeValue.strip())
					yPosFound = True
	if xPosFound and yPosFound:
		return ImageInfo(origFilename, xPos, yPos)
	else:
		return None

def translateImageInfo():
	global sourceFileInfos
	firstFound = False
	bbMinXInfo = None
	bbMinYInfo = None
	bbMinX = 0.0
	bbMinY = 0.0
	# Find min and max of available image info
	for sf in sourceFileInfos:
		i = sourceFileInfos[sf]
		if i == None:
			continue
		if not firstFound:
			bbMinX = i.xpos
			bbMinY = i.ypos
			firstFound = True
			continue
		if i.xpos < bbMinX:
			bbMinX = i.xpos
			bbMinXInfo = i
		if i.ypos < bbMinY:
			bbMinY = i.ypos
			bbMinYInfo = i
	# Relate all other images to the one with the lowest X
	if bbMinXInfo is None:
		return
	useCalibration = True
	heightConvValue = 1.0
	widthConvValue = 1.0
	if useCalibration:
		calibration = referenceFilesCalibration.values()[0]
		heightConvValue = calibration.pixelWidth
		widthConvValue = calibration.pixelHeight
	else:
		# For now, expect all images to come from the same experiment
		# and use the conversion factors in the meta data from the first
		# reference file.
		metadata = referenceFilesMetaData.values()[0]
		heightConvValueString = "[Reference Image Parameter] HeightConvertValue = "
		heightConvValueIndex = metadata.find(heightConvValueString)
		if heightConvValueIndex == -1:
			raise StandardError("Did not find height conversion information in meta data")
		heightConvValueLineEnd = metadata.find("\n", heightConvValueIndex)
		heightConvValue = float(metadata[heightConvValueIndex+len(heightConvValueString):heightConvValueLineEnd])
		
		widthConvValueString = "[Reference Image Parameter] WidthConvertValue = "
		widthConvValueIndex = metadata.find(widthConvValueString)
		if widthConvValueIndex == -1:
			raise StandardError("Did not find width conversion information in meta data")
		widthConvValueLineEnd = metadata.find("\n", widthConvValueIndex)
		widthConvValue = float(metadata[widthConvValueIndex+len(widthConvValueString):widthConvValueLineEnd])
	
	log("\tfound width convetsion value \"" + str(widthConvValue) + "\" and height conversion value \"" + str(heightConvValue) + "\"")
		
	# Go through the other file infos and update offsets
	for sf in sourceFileInfos:
		i = sourceFileInfos[sf]
		if i is None:
			continue
		if i is bbMinXInfo:
			i.xOffset = 0.0
			i.yOffset = 0.0
		else:
			i.xOffset = (bbMinXInfo.xpos - i.xpos ) / heightConvValue
			i.yOffset = (bbMinXInfo.ypos - i.ypos ) / widthConvValue
			if invertXOffset:
				i.xOffset = -i.xOffset;
			if invertYOffset:
				i.yOffset = -i.yOffset;
		log("\t\toffset of " + sf + ": (" + str(i.xOffset) + ", " + str(i.yOffset) + ")")

# Tries to create tiling information
def createTilingInfo():
	global sourceFileInfos
	log("creating tiling information")
	tilingFilePath = srcDir + tilingInfoFile
	if os.path.exists(tilingFilePath):
		log("\tfound tiling info XML (" + tilingFilePath + "), going to read it")
		doc = xml.dom.minidom.parse(tilingFilePath)
		for f in sourceFiles:
			imageInfo = findImageInfo(doc, f)
			sourceFileInfos[f] = imageInfo
			if imageInfo != None:
				log("\t\t" + str(imageInfo))
	else:
		for f in sourceFiles:
			sourceFileInfos[f] = None
		log("\tdid not find tiling info XML (" + tilingFilePath + "), making wild guesses")

# A method to create one tiling configuration file per
# channel, based on a configuration file for the reference
# channel. 
def createChannelTilingConfig(referenceFile):
	log("\tcreating tile configuration files")
	# Open reference file and read in its contents
	referenceConfig = open(referenceFile)
	referenceLines = []
	for line in referenceConfig:
		referenceLines.append(line)
	referenceContent = ''.join(referenceLines)
	# Create a new config file for each channel
	for c in range(0, numChannels):
		# Open the output file
		chConfigName = getChannelConfigName(referenceFile, c)
		chConfig = open(chConfigName, 'w')
		chContent = referenceContent
		# Look for mentioned reference files in the reference
		# files conent. If found, replace it with the current
		# channel file name.
		for ref in referenceFiles:
			origFileName = referenceFiles[ref]
			chFileName = getChannelFileName(origFileName, c)
			chContent = chContent.replace(ref, chFileName)
		# Write the (possibly altered) content to the channel file
		chConfig.write(chContent)
		# We are done with that channel, close it
		chConfig.close()
	referenceConfig.close()

def stitch():
	log("starting stitching")
	stitcher = Stitch_Image_Collection()
	# General stitching properties
	createPreview = False
	computeOverlap = True
	gridLayout = stitching.GridLayout()
	outputFileName	= tmpDir + "/" + tilingCongigFile
	# Add information about the stitching process
	gridLayout.rgbOrder = rgbOrder
	gridLayout.alpha = alpha
	gridLayout.thresholdR = thresholdR
	gridLayout.thresholdDisplacementRelative = thresholdDisplacementRelative
	gridLayout.thresholdDisplacementAbsolute =	thresholdDisplacementAbsolute
	gridLayout.sizeX = xTiles
	gridLayout.sizeY = yTiles
	gridLayout.fusionMethod = fusionMethod
	gridLayout.handleRGB = handleRGB
	gridLayout.dim = dim
	gridLayout.imageInformationList = ArrayList()
	# Stitch the reference files
	i = 0
	for f in referenceFiles:
		log("\tPreparing image info for file " + f)
		iI = None
		if dim == 3:
			iI = stitching.ImageInformation(3, i, stitching.model.TranslationModel3D())
		else:
			iI = stitching.ImageInformation(2, i, stitching.model.TranslationModel2D())
		iI.imageName = tmpDir + "/" + f
		iI.imp = None
		sourceFileInfo = sourceFileInfos[referenceFiles[f]]
		if sourceFileInfo == None:
			log("\t\tno position information available")
			iI.offset[0] = 0
			iI.offset[1] = 0
			iI.position[0] = 0
			iI.position[1] = 0
			if dim == 3:
				iI.offset[2] = 0
				iI.position[2] = 0
		else:
			log("\t\tthere is position information available: (" + str(sourceFileInfo.xpos) + ", " + str(sourceFileInfo.ypos) + ")")
			iI.offset[0] = sourceFileInfo.xOffset
			iI.offset[1] = sourceFileInfo.yOffset
			iI.position[0] = sourceFileInfo.xOffset
			iI.position[1] = sourceFileInfo.yOffset
			if dim == 3:
				iI.offset[2] = 0
				iI.position[2] = 0
		
		gridLayout.imageInformationList.add(iI)
		i = i + 1
	log("\tcomputing overlap and tiling configuration")
	stitcher.work(gridLayout, createPreview, computeOverlap, outputFileName, showReference)
	log("\tbuilding ap stitched images for all " + str(numChannels) + " channels")
	tilingConfigFile = outputFileName + ".registered"
	if not os.path.exists(tilingConfigFile):
		log("\tthe tiling configuration file (" + tilingConfigFile + ") could not be found, an error might have happened")
		raise StandardError("Tiling configuration file not found")
	# Create tile configuration files for all other channels
	log("\tcreating tiling configurations for all channels")
	createChannelTilingConfig(tilingConfigFile)
	# Stitch all channels separately
	log("\tstitching all channels")
	stitchedChannels = []
	for c in range(0, numChannels):
		chConfigName = getChannelConfigName(tilingConfigFile, c)
		stitchedCh = stitcher.work(chConfigName, False, False, fusionMethod, handleRGB, False)
		stitchedChannels.append(stitchedCh)
	# Save channels if wanted
	if saveStitchedChannels:
		log("\tsaving single channel images to folder " + tmpDir)
		for i in range(0, numChannels):
			name = "C-" + str(i) + "_stitched.tiff"
			IJ.saveAs(stitchedChannels[i], "tif", tmpDir + "/" + name)
	# Combine channels into one file
	width = stitchedChannels[0].getWidth()
	height = stitchedChannels[0].getHeight()
	numSlices = stitchedChannels[0].getStackSize()
	stack = ImageStack( width, height )
	log("\tcombining all " + str(numChannels) + " channels into one composite image with dimensions " + str(width) + "x" + str(height) + " and " + str(numSlices) + " slices")
	for z in range(0, numSlices):
		for c in range(0, numChannels):
			channel = stitchedChannels[c]
			cp = channel.getStack().getProcessor(z + 1).duplicate()
			stack.addSlice( "", cp );
	combined = ImagePlus( "rendered", stack)
	combined.setDimensions(numChannels, numSlices, 1)
	composite = CompositeImage( combined )
	IJ.saveAs(composite, "tif", tmpDir + "/" + "stitched_composite.tiff")
	if showResult:
		composite.show()

# Main method
def doWork(extFilter):
	global tmpDir
	tmpDir = srcDir + "tmp"
	try:
		startTime = time.time()
		# create temporary directory if not present
		if useSystemTmpFolder:
			tmpDir = tempfile.mkdtemp()
		else:
			if not os.path.exists(tmpDir):
				os.makedirs(tmpDir)
		log("\tusing temporary directory: " + tmpDir)
		getSourceFiles(extFilter)
		extractChannels(referenceChannel)
		createTilingInfo()
		translateImageInfo()
		prepareTime = time.time() - startTime
		log('timing: took %0.3f min for preparation' % (prepareTime/60.0))
		stitch()
		stitchTime = time.time() - startTime - prepareTime
		log('timing: took %0.3f min for stitching' % (stitchTime/60.0))
	finally:
		log("done")
		# delete temporary directory again
		try:
			if deleteTempFolder:
				log("removing temporary directory")
				shutil.rmtree(tmpDir) # delete directory
		except OSError, e:
			if e.errno != 2: # code 2 - no such file or directory
				raise

# Create the GUI and start it up
frame = JFrame("Options")
all = JPanel()
layout = GridLayout(13, 2)
all.setLayout(layout)

extTf = JTextField()
chTf = JTextField()
xTilesTf = JTextField(str(xTiles))
yTilesTf = JTextField(str(yTiles))
tilingInfoTf = JTextField(tilingInfoFile)
invertXCb = JCheckBox("Invert X offset", invertXOffset)
invertYCb = JCheckBox("Invert Y offset", invertYOffset)
tmpCb = JCheckBox("Use System temp folder", useSystemTmpFolder)
saveChCb = JCheckBox("Save stitched channel files", saveStitchedChannels)
delTmpCb = JCheckBox("Delete temp folder at end", deleteTempFolder)
showRefStitchCb = JCheckBox("Show reference stitching image", showReference)
showResultCb = JCheckBox("Show result composite image", showResult)

class Listener(ActionListener):
	def actionPerformed(self, event):
		global useSystemTmpFolder
		global deleteTempFolder
		global tilingInfoFile
		global referenceChannel
		global saveStitchedChannels
		global showResult
		global showReference
		global invertXOffset
		global invertYOffset
		print "Starting stitching"
		frame.setVisible(False)
		invertXOffset = invertXCb.isSelected()
		invertYOffset = invertYCb.isSelected()
		useSystemTmpFolder = tmpCb.isSelected()
		deleteTempFolder = delTmpCb.isSelected()
		referenceChannel = int(chTf.getText())
		tilingInfoFile = tilingInfoTf.getText()
		xTiles = int(xTilesTf.getText())
		yTiles = int(yTilesTf.getText())
		saveStitchedChannels = saveChCb.isSelected()
		showReference = showRefStitchCb.isSelected()
		showResult = showResultCb.isSelected()
		doWork(extTf.getText()) 

# Add GUI input
all.add(JLabel("file reg. ex."))
extTf.setText(".*\.oif")
all.add(extTf)
all.add(JLabel("Stitching channel"))
chTf.setText("1")
all.add(chTf)
all.add(JLabel("X tiles"))
all.add(xTilesTf)
all.add(JLabel("Y tiles"))
all.add(yTilesTf)
all.add(JLabel("Tiling info XML"))
all.add(tilingInfoTf)
all.add(tmpCb)
all.add(JLabel(""))
all.add(invertXCb)
all.add(JLabel(""))
all.add(invertYCb)
all.add(JLabel(""))
all.add(saveChCb)
all.add(JLabel(""))
all.add(delTmpCb)
all.add(JLabel(""))
all.add(showRefStitchCb)
all.add(JLabel(""))
all.add(showResultCb)
# Add start button and pane to frame
all.add(JLabel(""))
btn = JButton("Start")
btn.addActionListener(Listener())
all.add(btn)
frame.getContentPane().add(JScrollPane(all))
frame.pack()
# Show GUI
frame.setVisible(True)