"""
(Mostly) swing based GUI for Joao's Data Display
"""

import os
import sys
import time
from threading import Thread
from java.lang.System import getProperty
from javax.swing import JPanel, JList, JLabel, JFrame, JButton, ListSelectionModel, DefaultListModel, JTabbedPane, JScrollPane, BorderFactory
from java.awt import BorderLayout, GridLayout, Dimension, ScrollPane
sys.path.append( os.path.join( getProperty("fiji.dir") + "/src-plugins/Joao_Data_Display" ) )
from helpers import log, exit
from ij import IJ, ImagePlus
from ij.gui import ImageCanvas

# A GUI that supports multiple screens
class SelectionGUI:
	def __init__(self, controler):
		self.controler = controler
		self.project = controler.project
		self.frame = None
		self.selectionPanel = None
		self.continueButton = None
		self.experimentModel = DefaultListModel()
		self.experimentList = None
		self.lists = {}
		self.init()

	def init(self):
		frame = JFrame( "Data Display for " + self.project.name )
		#frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout( BorderLayout() )
		# The selection panel covers everything for protein/marker selection
		selectionPanel = JPanel( BorderLayout() )
		proteinPanel = JPanel( GridLayout( 0, len( self.project.proteins ) + 1 ) )
		# add a JList for all the proteins
		for p in self.project.proteins:
			panel = JPanel( BorderLayout() )
			panel.add( JLabel(p.name), BorderLayout.NORTH )
			markerList = JList(p.markers)
			markerList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
			self.lists[ markerList ] = p.name
			markerList.valueChanged = self.select
			panel.add( markerList, BorderLayout.CENTER );
			proteinPanel.add( panel )
		# Add experiment list box
		panel = JPanel( BorderLayout() )
		panel.add( JLabel( "Experiments" ), BorderLayout.NORTH )
		self.experimentList = JList(self.experimentModel, valueChanged=self.selectExperiment)
		panel.add( self.experimentList, BorderLayout.CENTER );
		proteinPanel.add( panel )
		#frame.getContentPane().add(JScrollPane(all))
		self.continueButton = JButton("Show data", actionPerformed=self.showData)
		self.continueButton.setEnabled( False )
		selectionPanel.add( JLabel( "Please select a combination" ), BorderLayout.NORTH )
		selectionPanel.add( proteinPanel, BorderLayout.CENTER )
		selectionPanel.add( self.continueButton, BorderLayout.SOUTH )
		frame.add( selectionPanel, BorderLayout.CENTER )
		frame.pack()
		frame.setSize( 400, 300 )
		frame.setVisible( False )
		# store as fields
		self.frame = frame
		self.selectionPanel = selectionPanel

	def show(self):
		self.frame.setVisible( True )

	def doNothing(self, event):
		return

	def select(self, event):
		# react only to final selection event
		if event.getValueIsAdjusting():
			return
		# remove all event handlers
		for l in self.lists.keys():
			l.valueChanged = self.doNothing
		# get current selection and update lists
		srcList = event.source
		marker = srcList.selectedValue
		for l in self.lists.keys():
			if l is not srcList:
				l.clearSelection()
		# add all event handlers
		for l in self.lists.keys():
			l.valueChanged = self.select
		# try to find experiments for that combination
		self.experimentModel.clear()
		self.continueButton.setEnabled( False )
		protein = self.lists[srcList]
		for e in self.project.experiments:
			if protein == e.protein and marker == e.marker:
				self.experimentModel.addElement(e.name)

	def selectExperiment(self, event):
		# react only to final selection event
		if event.getValueIsAdjusting():
			return
		if event.source.selectedValue is None:
			return
		self.continueButton.setEnabled( True )
		log( "Selected experiment " + event.source.selectedValue )

	def showData(self, event):
		# Find the actual experiment selected
		selectedExperiment = self.experimentList.getSelectedValue()
		experiment = None
		for e in self.project.experiments:
			if e.name == selectedExperiment:
				experiment = e
				break
		# Make sure we got a project
		if experiment is None:
			log("Could not find a valid project with name " + selectedExperiment)
			return
		# Go on to the data display
		log( "Showing data for experiment " + experiment.name )
		self.controler.showDataDialog( experiment )

# A function that repeatedly forwards to the next frame
# of the current movie
def animate( gui ):
	while gui.moviePlaying:
		gui.showNextMovieFrame()
		time.sleep( 1.0 / 7.0 )

# A GUI that displays information for one particular experiment
class DataGUI:
	def __init__(self, experiment):
		self.experiment = experiment
		self.frame = None
		self.sliceLabel = None
		self.stopButton = None
		self.movies = []
		self.canvases = []
		self.currentMovie = None
		self.currentCanvas = None
		self.moviePlaying = False
		self.animationThread = None
		self.init()

	def init(self):
		frame = JFrame( "Data Display for experiment " + self.experiment.name )
		frame.setSize( 500, 400 )
		frame.setLayout( BorderLayout() )
		frame.setVisible(False)
		self.frame = frame
		# Check if the data folder for the experiment is available
		if not os.path.exists(self.experiment.path):
			frame.add( JLabel( "Could not find path: " + self.experiment.path), BorderLayout.NORTH )
			return
		frame.add( JLabel( "Data path: " + self.experiment.path ), BorderLayout.NORTH )
		# Create and populate the data panel
		dataPanel = JPanel( GridLayout( 2, 2 ) )
		# First, the movie panel
		moviePanel = JPanel( BorderLayout() )
		moviePanel.setBorder( BorderFactory.createTitledBorder("Movies") )
		tabbedPane = JTabbedPane( stateChanged=self.handleMovieTabChange )
		for ( counter, mp ) in enumerate( self.experiment.moviePaths ):
			# Load it into an image
			imp = IJ.openImage(mp)
			if imp is None:
				log( "Could not load image: " + mp )
				continue
			# Set current movie to first one loaded
			if counter == 0:
				currentMovie = imp
			self.movies.append( imp )
			ic = ImageCanvas(imp)
			self.canvases.append( ic )
			ic.setPreferredSize( Dimension( ic.getWidth(), ic.getHeight() ) )
			# Unfortunately, the AWT.ScrollPane has to be used with AWT.Canvas
			scroll = ScrollPane()
			scroll.add(ic)
			tabbedPane.addTab("Movie #" + str(counter), None, scroll, mp)
		moviePanel.add( tabbedPane, BorderLayout.CENTER )
		controlPanel = JPanel()
		controlPanel.add( JButton("Play", actionPerformed=self.playMovie) )
		self.stopButton = JButton("Pause", actionPerformed=self.stopMovie, enabled=False)
		controlPanel.add( self.stopButton )
		controlPanel.add( JButton("Prev", actionPerformed=self.prevFrameButtonHandler) )
		controlPanel.add( JButton("Next", actionPerformed=self.nextFrameButtonHandler) )
		self.sliceLabel = JLabel()
		self.updateFrameInfo()
		controlPanel.add( self.sliceLabel )
		moviePanel.add( controlPanel, BorderLayout.SOUTH )
		dataPanel.add( moviePanel )
		# Second, the matlab figure
		figurePanel = JPanel( BorderLayout() )
		figurePanel.setBorder( BorderFactory.createTitledBorder("Matlab figure") )
		dataPanel.add( figurePanel )
		# Third, the excel sheet graph
		tablePanel = JPanel( BorderLayout() )
		tablePanel.setBorder( BorderFactory.createTitledBorder("Spreadsheet data") )
		dataPanel.add( tablePanel )
		# Last, the lsm file meta data
		metadataPanel = JPanel( BorderLayout() )
		metadataPanel.setBorder( BorderFactory.createTitledBorder("Meta data") )
		dataPanel.add( metadataPanel )
		# Add all to the frame
		frame.add( dataPanel, BorderLayout.CENTER )
		frame.add( JButton("Close", actionPerformed=self.close), BorderLayout.SOUTH)
		frame.pack()

	def show(self):
		self.frame.setVisible( True )

	def close(self, event):
		self.frame.setVisible( False )

	# Updates the image data and the canvas component
	def updateImage(self):
		self.currentCanvas.setImageUpdated()
		self.currentMovie.draw()
		self.currentCanvas.repaint()

	# Reacts to changes of the movie tab component
	def handleMovieTabChange(self, event):
		# Update reference to current movie and current canvas
		self.currentMovie = self.movies[ event.source.getSelectedIndex() ]
		self.currentCanvas = self.canvases[ event.source.getSelectedIndex() ]
		self.updateFrameInfo()

	def updateFrameInfo(self):
		if self.sliceLabel is None:
			return
		mov = self.currentMovie
		info = "Frame " + str( mov.getCurrentSlice() ) + "/" + str( mov.getNSlices() )
		self.sliceLabel.setText( info )

	# Handler for the "next" button
	def nextFrameButtonHandler(self, event):
		self.showNextMovieFrame()

	# Forwars one frame of the current movie
	def showNextMovieFrame( self ):
		imp = self.currentMovie
		imp.setSlice( imp.getCurrentSlice() + 1 )
		self.updateImage()
		self.updateFrameInfo()

	# Handler for the "prev" button
	def prevFrameButtonHandler(self, event):
		self.showPreviousMovieFrame()

	# Rewinds one frame of the current movie
	def showPreviousMovieFrame(self):
		imp = self.currentMovie
		imp.setSlice( imp.getCurrentSlice() - 1 )
		self.updateImage()
		self.updateFrameInfo()

	# Plays the current movie with the help pf a new thread
	def playMovie(self, event):
		# Don't start another thread if we are already running one
		if self.moviePlaying:
			return
		self.stopButton.setEnabled( True )
		# Create and start a new thread that forwards the movie
		self.moviePlaying = True
		self.animationThread = Thread( target=lambda: animate( self ) )
		self.animationThread.start()
		self.stopButton.setText( "Pause" )

	# Stops the current movie playback
	def stopMovie(self, event):
		if not self.moviePlaying:
			self.currentMovie.setSlice( 1 )
			self.stopButton.setEnabled( False )
			self.stopButton.setText( "Pause" )
			self.updateImage()
			self.updateFrameInfo()
			return
		self.stopButton.setText( "Stop" )
		self.moviePlaying = False
		self.animationThread.join()
