from ij.io import DirectoryChooser
from ij import ImageStack
from ij import CompositeImage
from ij import IJ
import os
import tempfile
import shutil
import re
import xml.dom.minidom
import time
import sys
from jarray import array
import math
from javax.swing import JButton, JScrollPane, JPanel, JComboBox, JLabel, JFrame, JTextField, JCheckBox
from java.awt import Color, GridLayout
from java.awt.event import ActionListener
from java.util import ArrayList
from java.io import File
from loci.plugins import LociImporter
from loci.plugins import BF
from loci.plugins.in import ImporterOptions
from mpicbg.stitching import StitchingParameters
from mpicbg.stitching import ImageCollectionElement
from mpicbg.models import TranslationModel2D
from mpicbg.models import TranslationModel3D
from mpicbg.stitching import CollectionStitchingImgLib
from mpicbg.stitching.fusion import Fusion
from mpicbg.imglib.type.numeric.integer import UnsignedByteType
from mpicbg.imglib.type.numeric.integer import UnsignedShortType
from mpicbg.imglib.type.numeric.real import FloatType

# Global variables
useSystemTmpFolder = True
deleteTempFolder = True
saveStitchedChannels = False
showReference = False
showResult = True
tmpDir = ""
srcDir = ""
outputDir = "stitched"
sourceFiles = []
referenceFileList = []
referenceFiles = {}
referenceFilesMetaData = {}
referenceFilesCalibration = {}
sourceFileInfos = {}
sourceFilesCalibration = {}
tilingInfoFile = "MATL_Mosaic.log"
tilingInfoSuffix = "_01"
numChannels = -1
referenceChannel = 1
conversionFactor = 1.0
tilingCongigFile = "TilingConfiguration.txt"
xTiles = 0
yTiles = 0
dim = 3
rgbOrder = "rgb"
alpha = 1.5
thresholdR = 0.6
thresholdDisplacementRelative = 200.5
thresholdDisplacementAbsolute = 300.5
fusionMethod = "Linear Blending"
preCombineZStacks = True
# Information about the current image
resolution = ""
dimension = ""
tilingDesc = ""
#fusionMethod = "None"
handleRGB = "Red, Green and Blue"
invertXOffset = False
invertYOffset = False
logMessages = []

# Choose a directory with lots of stacks
dc = DirectoryChooser("Choose directory with stacks")
srcDir = dc.getDirectory()
if srcDir is None:
	sys.exit()
outputDir = srcDir + "stitched"

def log(message):
	logMessages.append(message)
	print message
	time.sleep(0.001)

def saveWrapperLog(path):
	try:
		f = open(path, 'w')
		for item in logMessages:
			f.write("%s\n" % item)
	except:
		print "ERR: Could not save log file!"
	finally:
		if f is not None:
			f.close()

def saveStitcherLog(path):
	try:
		f = open(path, 'w')
		logText = IJ.getLog()
		if logText is not None:
			f.write(logText)
	except:
		print "ERR: Could not save log file!"
	finally:
		if f is not None:
			f.close()

def saveInfoFile(path):
	try:
		f = open(path, 'w')
		f.write("dimension: " + dimension + "\n")
		f.write("resolution: " + resolution + "\n")
		f.write("name: " + tilingDesc)
	except:
		print "ERR: Could not save info file!"
	finally:
		if f is not None:
			f.close()

# A closs for storing image information
class ImageInfo():
	def __init__(self, path, filename):
		self.path = path
		self.filename = filename
		self.xpos = 0.0
		self.ypos = 0.0
		self.xOffset = 0.0
		self.yOffset = 0.0
		self.zOffset = 0.0
		self.numSlices = 0
	def __str__(self):
		return "ImageInfo: " + self.filename + " is at positon (" + str(self.xpos) + ", " + str(self.ypos) + ")"

# Get source files
def getSourceFiles(extFilter):
	global sourceFiles
	log("looking for source files, using regular expression: " + extFilter)
	pattern = re.compile(extFilter)
	for filename in os.listdir(srcDir):
		# don't allow folders
		if os.path.isdir(srcDir + filename):
			continue
		# check agains regular expression
		if (pattern.match(filename) is None):
			continue
		sourceFiles.append(filename)
	# Sort source files
	log("\tsorting source files")
	sourceFiles.sort()

# Load the stacks and extract the metadata
def extractMetadata():
	global referenceFiles
	global numChannels
	log("extracting meta data from files")
	# check out all files in source folder
	for filename in sourceFiles:
		log("\tloading " + filename)
		# Import the image
		path = srcDir + filename
		options = ImporterOptions()
		options.setId(path)
		options.setSplitChannels(False)
		options.setWindowless(True)
		options.setVirtual(True)
		imps = BF.openImagePlus(options)
		if len(imps) == 0:
			log("\t\tCould not load image")
			continue
		width = imps[0].getWidth()
		height = imps[0].getHeight()
		numSlices = imps[0].getStackSize()
		log("\t\tOpened " + str(len(imps)) + " image(s) with dimensions " + str(width) + "x" + str(height) + " and " + str(numSlices) + " slices")
		# Check if there are enought channels
		#if numChannels <= channel:
		#	log("\t\terror: requested channel number (" + str(channel) + ") is larger than available channels")
		#	raise StandardError("Requested Channel number too large")
		sourceFilesCalibration[filename] = imps[0].getCalibration()
		sourceFileInfos[filename] = ImageInfo(srcDir, filename)

# Iterates the xml document and looks for image information
# for a specified filename
def findImageInfo(doc, filename, imageInfo):
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
		log("\tfound no position information information in XML document for file " + origFilename)
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
		imageInfo.xpos = xPos
		imageInfo.ypos = yPos

def combineZStacks(stackInfo):
	global srcDir
	global sourceFiles
	global sourceFileInfos
	log("\tcombining Z stacks")
	# make sure the tmpDir exists
	if not os.path.exists(tmpDir):
		log("\t\tError: could not find temp. directory")
	newSourceFiles = []
	newSourceFileInfos = {}
	#newSourceFilesCalibration = {}
	updateSourceDir = False
	for posHash in stackInfo:
		stacks = stackInfo[posHash]
		log("\t\tLooking at offset hash: " + posHash)
		if len(stacks) < 2:
			# continue with next pos if there is nothing to concatenate
			log("\t\t\tNot enough stacks to concatenate, using single original file")
			name = stacks[0].filename
			newSourceFiles.append(name)
			newSourceFileInfos[name] = stacks[0]
			continue
		# combine the stacks
		macro = "setBatchMode(true);\n"
		file1 = None
		file2 = None
		file1Name = None
		file2Name = None
		concatImgName = "concatenated"
		outputName = concatImgName
		topImageInfo = None
		for s in stacks:
			log("\t\t\tstack: " + str(s))
			# get top stack
			if file1 is None:
				file1 = os.path.join(s.path, s.filename)
				file1Name = s.filename
				outputName += file1Name + ".tiff"
				topImageInfo = s
				macro += "run(\"Bio-Formats\", \"open=[" + file1 + "] view=Hyperstack stack_order=XYCZT display_metadata=false\");\n"
				continue
			# get next stack
			if file2 is None:
				file2 = os.path.join(s.path, s.filename)
				file2Name = s.filename
				macro += "run(\"Bio-Formats\", \"open=[" + file2 + "] view=Hyperstack stack_order=XYCZT display_metadata=false\");\n"
			# concatenate
			macro += "run(\"Concatenate...\", \"stack1=[" + file1Name + "] stack2=[" + file2Name + "] title=[" + concatImgName + "]\");\n"
			file1Name = concatImgName
			file2 = None
		# save result in same place as source files
		outputDir = os.path.dirname(file1)
		outputPath = os.path.join(tmpDir, outputName)
		macro += "saveAs(\"Tiff\", \"" + outputPath + "\");\n"
		macro += "close();\n"
		macroResult = IJ.runMacro(macro)
		if macroResult is None:
			log("\t\t\tmacro returned: None")
		else:
			log("\t\t\tmacro returned: " + macroResult)
		# print macro
		# IJ.log(macro)
		#Update the source image information
		newSourceFiles.append(outputName)
		ii = ImageInfo(tmpDir, outputName)
		ii.xpos = topImageInfo.xpos
		ii.ypos = topImageInfo.ypos
		ii.xOffset = topImageInfo.xOffset
		ii.yOffset = topImageInfo.yOffset
		newSourceFileInfos[outputName] = ii
		updateSourceDir = True
	# read the new concatenated images from the tmpDir
	if updateSourceDir:
		srcDir = tmpDir
		sourceFiles = newSourceFiles
		sourceFileInfos = newSourceFileInfos

def translateImageInfo():
	global sourceFileInfos
	global resolution
	firstFound = False
	# Take the first file as reference file
	fi = sourceFileInfos[sourceFiles[0]]
	bbMinXInfo = fi
	bbMinYInfo = fi
	bbMinX = fi.xpos
	bbMinY = fi.ypos
	# Relate all other images to the one with the lowest X
	if bbMinXInfo is None:
		log("\tdid not find bounding box info")
		return
	useCalibration = True
	heightConvValue = 1.0
	widthConvValue = 1.0
	calibration = sourceFilesCalibration.values()[0]
	heightConvValue = conversionFactor * calibration.pixelWidth
	widthConvValue = conversionFactor * calibration.pixelHeight
	unit = calibration.getUnit()
	
	log("\tfound width conversion value \"" + str(widthConvValue) + "\" and height conversion value \"" + str(heightConvValue) + "\" -- unit: " + unit + "/px")
	# The resolution infomation is expected to be nm/px
	resolutionInfo = widthConvValue
	if unit == "um" or unit == "micron":
		resolutionInfo = resolutionInfo * 1000
	elif unit != "nm":
		log("\t\tthe unit used is not yet recognized to be converted for use with CatMaid")	
	resolution = "(" + str(resolutionInfo) + "," + str(resolutionInfo) + "," + str(resolutionInfo) + ")"
	
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
			# make sure we got no "negative zeros"
			if math.fabs(i.xOffset) < 0.00001:
				i.xOffset = 0.0
			if math.fabs(i.yOffset) < 0.00001:
				i.yOffset = 0.0
	# Look for duplicates in the offset. If found interpret these as
	# z-offset information. The z-offset of the duplicate is set to
	# be just below the original one
	coordMap = {}
	stacksToCombineInZ = {}
	for sf in sourceFiles:
		i = sourceFileInfos[sf]
		if i is None:
			continue
		coordHash = str(i.xOffset) + "," + str(i.yOffset)
		zIndex = 0.0
		# use already present value, if any
		if coordHash in coordMap:
			zIndex = coordMap[coordHash]
			stacksToCombineInZ[coordHash].append(i)
		else:
			stacksToCombineInZ[coordHash] = [i]
		i.zOffset = zIndex
		zIndex += i.numSlices + 1 # slices are 1-based
		coordMap[coordHash] = zIndex
	# Manage the pre-composition of different z stacks if requested
	if preCombineZStacks:
		combineZStacks(stacksToCombineInZ)
	# One more iteration for log output
	for sf in sourceFiles:
		i = sourceFileInfos[sf]
		if i is None:
			continue
		log("\t\toffset of " + sf + ": (" + str(i.xOffset) + ", " + str(i.yOffset) + ", " + str(i.zOffset) + ")")

# Tries to create tiling information
def createTilingInfo():
	global sourceFileInfos
	useXML = True
	log("creating tiling information")
	tilingFilePath = srcDir + tilingInfoFile
	if useXML and os.path.exists(tilingFilePath):
		log("\tfound tiling info XML (" + tilingFilePath + "), going to read it")
		doc = xml.dom.minidom.parse(tilingFilePath)
		for f in sourceFiles:
			imageInfo = sourceFileInfos[f]
			findImageInfo(doc, f, imageInfo)
			if imageInfo != None:
				log("\t\t" + str(imageInfo))
	else:
		for f in sourceFiles:
			calibration = sourceFilesCalibration[f]
			imageInfo = sourceFileInfos[f]
			imageInfo.xOffset = calibration.xOrigin
			imageInfo.yOffset = calibration.yOrigin
			log("\tUsing calibration data: " + str(imageInfo))

def stitch():
	global dimension
	log("starting stitching")
	params = StitchingParameters()
	params.fusionMethod = 0
	params.regThreshold = thresholdR
	params.relativeThreshold = thresholdDisplacementRelative
	params.absoluteThreshold = thresholdDisplacementAbsolute
	params.computeOverlap = True
	params.subpixelAccuracy = True
	params.cpuMemChoice = 0
	params.channel1 = 0
	params.channel2 = 0
	params.timeSelect = 0
	params.checkPeaks = 5
	params.dimensionality = dim;
	
	elements = ArrayList()
	numTimePoints = 1
	index = 0
	for f in sourceFiles:
		fi = sourceFileInfos[f]
		element = ImageCollectionElement( File( fi.path, fi.filename ), index )
		log("\tadding file to pool: " + fi.path + "/" + fi.filename)
		index = index + 1
		element.setDimensionality( dim )
		if dim == 3:
			element.setModel( TranslationModel3D() )
			element.setOffset( array([fi.xOffset, fi.yOffset, fi.zOffset], "f") )
		else:
			element.setModel( TranslationModel2D() )
			element.setOffset( array([fi.xOffset, fi.yOffset], "f") )
		elements.add(element)
	

	optimized = CollectionStitchingImgLib.stitchCollection( elements, params )
	imp = None
	closeElements = True
	if optimized is None:
		log("\tcould not find any overlapping tiles")
		if elements.size() == 1:
			# if we got only one image, treat it as the result
			log("\tusing the single available image as result")
			imp = elements.get(0).open()
			closeElements = False
		else:
			# cancel if we have more than one source files
			log("\tcanceling the stitching")
			return
	else:
		#for ( final ImagePlusTimePoint imt : optimized )
		#	IJ.log( imt.getImagePlus().getTitle() + ": " + imt.getModel() );

		log("\tfusing images")
		models = ArrayList()
		images = ArrayList()
		is32bit = False
		is16bit = False
		is8bit = False

		for i in range(0, optimized.size()):
			imt = optimized.get(i)
			imp = imt.getImagePlus()
			if imp.getType() == ImagePlus.GRAY32:
				is32bit = True;
			elif imp.getType() == ImagePlus.GRAY16:
				is16bit = True;
			elif imp.getType() == ImagePlus.GRAY8:
				is8bit = True;
			images.add( imp );

		for f in range(1, numTimePoints + 1):
			for i in range(0, optimized.size()):
				imt = optimized.get(i)
				models.add( imt.getModel() )

		if is32bit:
			imp = Fusion.fuse( FloatType(), images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod )
		elif is16bit:
			imp = Fusion.fuse( UnsignedShortType(), images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod )
		elif is8bit:
			imp = Fusion.fuse( UnsignedByteType(), images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod )
		else:
			log( "unknown image type for fusion." )

	# create dimension information for info.yml
	width = imp.getWidth()
	height = imp.getHeight()
	numSlices = imp.getNSlices()
	log("\tcreated result image with dimensions " + str(width) + "x" + str(height) + " and " + str(numSlices) + " slices")
	dimension = "(" + str(width) + "," + str(height) + "," + str(numSlices) + ")"

	try:
		resultPath = outputDir + "/stitched_composite.tiff"
		log("Saving result to: " + resultPath)
		IJ.saveAs(imp, "tif", resultPath)
	except:
		log("ERR: Could not save file")

	if showResult:
		imp.show()

	# close all images
	if closeElements:
		for element in elements:
			element.close()

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
		# create output directory if not present
		if not os.path.exists(outputDir):
			os.makedirs(outputDir)
		log("\tusing output directory: " + outputDir)
		getSourceFiles(extFilter)
		extractMetadata()
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
	saveWrapperLog(outputDir + "/wrapper.log")
	saveStitcherLog(outputDir + "/stitcher.log")
	saveInfoFile(outputDir + "/info.yml")
	exit()

# Create the GUI and start it up
frame = JFrame("Options")
all = JPanel()
layout = GridLayout(18, 2)
all.setLayout(layout)

extTf = JTextField(".*\.oif")
chTf = JTextField(str(referenceChannel))
xTilesTf = JTextField(str(xTiles))
yTilesTf = JTextField(str(yTiles))
thresholdTf = JTextField(str(thresholdR))
convFactorTf = JTextField(str(conversionFactor))
outputTf = JTextField(outputDir)
tilingInfoTf = JTextField(tilingInfoFile)
tilingDescTf = JTextField(tilingDesc)
invertXCb = JCheckBox("Invert X offset", invertXOffset)
invertYCb = JCheckBox("Invert Y offset", invertYOffset)
precombineCb = JCheckBox("Precombine adjacent Z tiles (if any)", preCombineZStacks)
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
		global outputDir
		global thresholdR
		global tilingDesc
		global preCombineZStacks
		global conversionFactor
		print "Starting stitching"
		frame.setVisible(False)
		invertXOffset = invertXCb.isSelected()
		invertYOffset = invertYCb.isSelected()
		useSystemTmpFolder = tmpCb.isSelected()
		deleteTempFolder = delTmpCb.isSelected()
		referenceChannel = int(chTf.getText())
		conversionFactor = float(convFactorTf.getText())
		outputDir = outputTf.getText()
		tilingInfoFile = tilingInfoTf.getText()
		tilingDesc = tilingDescTf.getText()
		xTiles = int(xTilesTf.getText())
		yTiles = int(yTilesTf.getText())
		preCombineZStacks = precombineCb.isSelected()
		thresholdR = float(thresholdTf.getText())
		saveStitchedChannels = saveChCb.isSelected()
		showReference = showRefStitchCb.isSelected()
		showResult = showResultCb.isSelected()
		doWork(extTf.getText()) 

# Add GUI input
all.add(JLabel("file reg. ex."))
all.add(extTf)
all.add(JLabel("Stitching channel"))
all.add(chTf)
all.add(JLabel("X tiles"))
all.add(xTilesTf)
all.add(JLabel("Y tiles"))
all.add(yTilesTf)
all.add(JLabel("Threshold"))
all.add(thresholdTf)
all.add(JLabel("Factor for conv. values"))
all.add(convFactorTf)
all.add(JLabel("Output folder"))
all.add(outputTf)
all.add(JLabel("Tiling info XML"))
all.add(tilingInfoTf)
all.add(JLabel("Tiling description"))
all.add(tilingDescTf)
all.add(tmpCb)
all.add(JLabel(""))
all.add(invertXCb)
all.add(JLabel(""))
all.add(invertYCb)
all.add(JLabel(""))
all.add(precombineCb)
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
