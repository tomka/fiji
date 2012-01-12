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
from structures import Condition, Experiment, Project, View

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
		self.exclusiveLists = {}
		self.inclusiveLists = {}
		self.exclusiveCondition = None
		self.inclusiveConditions = {}
		self.init()

	def init(self):
		frame = JFrame( "Data Display for " + self.project.name )
		#frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout( BorderLayout() )
		# The selection panel covers everything for protein/marker selection
		selectionPanel = JPanel( BorderLayout() )
		numLists = len( self.project.exclusiveConditions ) + len( self.project.inclusiveConditions ) + 1
		conditionsPanel = JPanel( GridLayout( 0, numLists ) )
		# add a JList for each exclusive condition
		for c in self.project.exclusiveConditions:
			panel = JPanel( BorderLayout() )
			panel.add( JLabel(c.name), BorderLayout.NORTH )
			optionList = JList(c.options)
			optionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
			self.exclusiveLists[ optionList ] = c.name
			optionList.valueChanged = self.selectExclusive
			panel.add( optionList, BorderLayout.CENTER );
			conditionsPanel.add( panel )
		# add a JList for each inclusive condition
		for c in self.project.inclusiveConditions:
			panel = JPanel( BorderLayout() )
			panel.add( JLabel(c.name), BorderLayout.NORTH )
			optionList = JList(c.options)
			optionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
			self.inclusiveLists[ optionList ] = c.name
			optionList.valueChanged = self.selectInclusive
			panel.add( optionList, BorderLayout.CENTER );
			conditionsPanel.add( panel )
		# Add experiment list box
		panel = JPanel( BorderLayout() )
		panel.add( JLabel( "Experiments" ), BorderLayout.NORTH )
		self.experimentList = JList(self.experimentModel, valueChanged=self.selectExperiment)
		panel.add( self.experimentList, BorderLayout.CENTER );
		conditionsPanel.add( panel )
		#frame.getContentPane().add(JScrollPane(all))
		self.continueButton = JButton("Show data", actionPerformed=self.showData)
		self.continueButton.setEnabled( False )
		selectionPanel.add( JLabel( "Please select a combination" ), BorderLayout.NORTH )
		selectionPanel.add( conditionsPanel, BorderLayout.CENTER )
		# button panel
		buttonPanel = JPanel()
		buttonPanel.add( JButton("Close", actionPerformed=lambda x: self.controler.exitProgram() ) )
		buttonPanel.add( self.continueButton )
		selectionPanel.add( buttonPanel, BorderLayout.SOUTH)
		frame.add( selectionPanel, BorderLayout.CENTER )
		frame.pack()
		frame.setSize( 400, 300 )
		frame.setVisible( False )
		# store as fields
		self.frame = frame
		self.selectionPanel = selectionPanel

	def close(self):
		self.frame.setVisible( False )
		self.frame.dispose()

	def show(self):
		self.frame.setVisible( True )

	def doNothing(self, event):
		return

	def selectExclusive(self, event):
		# react only to final selection event
		if event.getValueIsAdjusting():
			return
		# remove all event handlers
		for l in self.exclusiveLists.keys():
			l.valueChanged = self.doNothing
		# get current selection and update lists
		srcList = event.source
		option = srcList.selectedValue
		for l in self.exclusiveLists.keys():
			if l is not srcList:
				l.clearSelection()
		# add all event handlers
		for l in self.exclusiveLists.keys():
			l.valueChanged = self.selectExclusive
		# create condition for selection
		self.exclusiveCondition = Condition.fromNameAndOption(self.exclusiveLists[srcList], option)
		# try to find a valid experiment
		self.showValidExperiments()

	def selectInclusive(self, event):
		# react only to final selection event
		if event.getValueIsAdjusting():
			return
		# get current selection and option
		srcList = event.source
		option = srcList.selectedValue
		# walk over all inclusive lists and get conditions
		self.inclusiveConditions = []
		for l in self.inclusiveLists.keys():
			option = l.selectedValue
			condition = Condition.fromNameAndOption(self.inclusiveLists[l], option)
			self.inclusiveConditions.append( condition )
		# try to find a valid experiment
		self.showValidExperiments()

	def showValidExperiments(self):
		self.experimentModel.clear()
		self.continueButton.setEnabled( False )
		for e in self.project.experiments:
			exclusiveMatches = e.matches( [ self.exclusiveCondition ] )
			inclusiveMatches = e.matches( self.inclusiveConditions )
			if exclusiveMatches and inclusiveMatches:
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

# A GUI that displays information for one particular experiment
class DataGUI:
	def __init__(self, experiment):
		self.experiment = experiment
		self.init()

	def init(self):
		frame = JFrame( "Data Display for experiment " + self.experiment.name )
		frame.setSize( 500, 400 )
		frame.setLayout( BorderLayout() )
		frame.setVisible(False)
		self.frame = frame
		# Find out how many views there are and create a layout for this batch
		numViews = len( self.experiment.views )
		searchingLayout = True
		xDim = 1
		yDim = 1
		while searchingLayout:
			searchingLayout = xDim * yDim < numViews
			if not searchingLayout:
				break
			if yDim < xDim:
				yDim = yDim + 1
			else:
				xDim = xDim + 1
		log( "Creating space for " + str(xDim) + "x" + str(yDim) + " views ("  + str(numViews) + " views are available)")
		# Create and populate the data panel
		dataPanel = JPanel( GridLayout( yDim, xDim ) )
		# Iterate over all views and create panels
		for v in self.experiment.views:
			if v.name == View.movieName:
				dataPanel.add( MovieViewPanel(v) )
			elif v.name == View.figureName:
				dataPanel.add( FigureViewPanel(v) )
			elif v.name == View.metadataName:
				dataPanel.add( SpreadsheetViewPanel(v) )
			elif v.name == View.tableName:
				dataPanel.add( MetaDataViewPanel(v) )
			else:
				log( "Don't know view: " + v.name )
		# Add all to the frame
		frame.add( dataPanel, BorderLayout.CENTER )
		frame.add( JButton("Close", actionPerformed=self.closeButtonHandler), BorderLayout.SOUTH)
		frame.pack()

	def close(self):
		self.frame.setVisible( False )
		self.frame.dispose()

	def show(self):
		self.frame.setVisible( True )

	def closeButtonHandler(self, event):
		self.close()

class ViewPanel( JPanel ):
	"""A common panel for displaying views"""
	def __init__( self, view, title ):
		super( ViewPanel, self ).__init__( BorderLayout() )
		self.currentFile = None
		self.files = []
		self.view = view
		self.title = title

		self.setBorder( BorderFactory.createTitledBorder( title ) )
		tabbedPane = JTabbedPane( stateChanged=self.handleTabChangeHook )
		for ( counter, p ) in enumerate( view.paths ):
			# Load it into an image
			data = self.loadData( p )
			if data is None:
				log( "Could not load file: " + p )
				continue
			# Set current movie to first one loaded
			if counter == 0:
				self.currentFile = data
			self.files.append( data )
			component = self.getContent( data )
			# Unfortunately, the AWT.ScrollPane has to be used with AWT.Canvas
			scroll = ScrollPane()
			scroll.add( component )
			tabbedPane.addTab( self.getTabText( counter ), None, scroll, p )
		self.add( tabbedPane, BorderLayout.CENTER )

	def loadData( self, filepath ):
		pass

	def handleTabChangeHook( self, event ):
		self.currentFile = self.files[ event.source.getSelectedIndex() ]
		self.handleTabChange( event )

	def handleTabChange( self, event ):
		pass

	def getContent( self, data ):
		pass

	def getTabText( self, counter ):
		pass

# A function that repeatedly forwards to the next frame
# of the current movie
def animate( gui ):
	while gui.moviePlaying:
		gui.showNextMovieFrame()
		time.sleep( 1.0 / 7.0 )

class MovieViewPanel( ViewPanel ):
	"""A panel to view movie files"""
	def __init__( self, view ):
		# Set up members
		self.canvases = []
		self.frame = None
		self.sliceLabel = None
		self.stopButton = None
		self.canvases = []
		self.currentCanvas = None
		self.moviePlaying = False
		self.animationThread = None
		# Init base class
		super( MovieViewPanel, self ).__init__( view, "Movies" )
		# Add control components
		controlPanel = JPanel()
		controlPanel.add( JButton("Play", actionPerformed=self.playMovie) )
		self.stopButton = JButton("Pause", actionPerformed=self.stopMovie, enabled=False)
		controlPanel.add( self.stopButton )
		controlPanel.add( JButton("Prev", actionPerformed=self.prevFrameButtonHandler) )
		controlPanel.add( JButton("Next", actionPerformed=self.nextFrameButtonHandler) )
		self.sliceLabel = JLabel()
		self.updateFrameInfo()
		controlPanel.add( self.sliceLabel )
		self.add( controlPanel, BorderLayout.SOUTH )

	def loadData( self, filepath ):
		return IJ.openImage( filepath )

	def getContent( self, data ):
		ic = ImageCanvas( data )
		self.canvases.append( ic )
		ic.setPreferredSize( Dimension( ic.getWidth(), ic.getHeight() ) )
		return ic

	def getTabText( self, counter ):
		return "Movie #" + str(counter)

	# Updates the image data and the canvas component
	def updateImage(self):
		self.currentCanvas.setImageUpdated()
		self.currentFile.draw()
		self.currentCanvas.repaint()

	# Reacts to changes of the movie tab component
	def handleTabChange(self, event):
		# Update reference to current canvas
		self.currentCanvas = self.canvases[ event.source.getSelectedIndex() ]
		self.updateFrameInfo()

	def updateFrameInfo(self):
		if self.sliceLabel is None:
			return
		mov = self.currentFile
		info = "Frame " + str( mov.getCurrentSlice() ) + "/" + str( mov.getNSlices() )
		self.sliceLabel.setText( info )

	# Handler for the "next" button
	def nextFrameButtonHandler(self, event):
		self.showNextMovieFrame()

	# Forwars one frame of the current movie
	def showNextMovieFrame( self ):
		imp = self.currentFile
		imp.setSlice( imp.getCurrentSlice() + 1 )
		self.updateImage()
		self.updateFrameInfo()

	# Handler for the "prev" button
	def prevFrameButtonHandler(self, event):
		self.showPreviousMovieFrame()

	# Rewinds one frame of the current movie
	def showPreviousMovieFrame(self):
		imp = self.currentFile
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
			self.currentFile.setSlice( 1 )
			self.stopButton.setEnabled( False )
			self.stopButton.setText( "Pause" )
			self.updateImage()
			self.updateFrameInfo()
			return
		self.stopButton.setText( "Stop" )
		self.moviePlaying = False
		self.animationThread.join()

class MetaDataViewPanel( ViewPanel ):
	"""A panel to view meta data files"""
	def __init__( self, view ):
		super( MetaDataViewPanel, self ).__init__( view, "Meta data" )
		# Use LOCI to read meta data
		pass

class FigureViewPanel( ViewPanel ):
	"""A panel to view Matlab figure files"""
	def __init__( self, view ):
		super( FigureViewPanel, self ).__init__( view, "Matlab figures" )
		pass

class SpreadsheetViewPanel( ViewPanel ):
	"""A panel to view spreadsheet data files"""
	def __init__( self, view ):
		super( SpreadsheetViewPanel, self ).__init__( view, "Spreadsheet data" )
		pass
