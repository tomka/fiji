"""
(Mostly) swing based GUI for Joao's Data Display
"""

import os
import sys
import time
from threading import Thread
from java.lang.System import getProperty
from javax.swing import JPanel, JList, JLabel, JFrame, JButton, ListSelectionModel, DefaultListModel, JTabbedPane, JScrollPane, BorderFactory, JTable
from javax.swing.table import DefaultTableModel
from java.awt import BorderLayout, GridLayout, Dimension, ScrollPane
sys.path.append( os.path.join( getProperty("fiji.dir") + "/src-plugins/Joao_Data_Display" ) )
from helpers import log, exit
from ij import IJ, ImagePlus
from ij.gui import ImageCanvas
from ij.plugin.filter import Info
from loci.plugins import LociImporter
from loci.plugins import BF
from loci.plugins.in import ImporterOptions
from structures import Condition, Experiment, Project, View
from org.apache.poi.hssf.usermodel import HSSFCell, HSSFRichTextString, HSSFRow, HSSFSheet, HSSFWorkbook
from org.apache.poi.hssf import OldExcelFormatException
from org.apache.poi.poifs.filesystem import POIFSFileSystem
from jxl import Workbook as JXLWorkbook
from java.io import File, FileInputStream, IOException

#Charts
from info.monitorenter.gui.chart import Chart2D, ITrace2D
from info.monitorenter.gui.chart.traces import Trace2DSimple

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
				dataPanel.add( MetaDataViewPanel(v) )
			elif v.name == View.tableName:
				dataPanel.add( SpreadsheetViewPanel(v) )
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
		self.tabFileMapping = []

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
			
			components = self.getContent( data )
			if not isinstance( components, list ):
				components = [ components ]
				
			for n, c in enumerate( components ):
				self.tabFileMapping.append(counter)
				# Unfortunately, the AWT.ScrollPane has to be used with AWT.Canvas
				scroll = ScrollPane()
				scroll.add( c )
				tabbedPane.addTab( self.getTabText( counter + 1, n ), None, scroll, p )

		self.add( tabbedPane, BorderLayout.CENTER )

	def loadData( self, filepath ):
		pass

	def handleTabChangeHook( self, event ):
		fileIndex = self.tabFileMapping[ event.source.getSelectedIndex() ]
		self.currentFile = self.files[ fileIndex ]
		self.handleTabChange( event )

	def handleTabChange( self, event ):
		pass

	def getContent( self, data ):
		pass

	def getTabText( self, counter, subcomponent=0 ):
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

	def getTabText( self, counter, subcomponent=0 ):
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
		pass

	def loadData( self, filepath ):
		"""Override super class method and use LOCI to read meta data in."""
		options = ImporterOptions()
		options.setId( filepath )
		options.setSplitChannels( False )
		options.setWindowless( True )
		options.setVirtual( True )
		imps = BF.openImagePlus( options )
		if len(imps) == 0:
			log("\t\tCould not load image")
			return
		return imps[0]

	def getContent( self, data ):
		""" We get an ImagePlus object here and put its info string
		into a label."""
		imgInfo = Info();
		info = imgInfo.getImageInfo( data, data.getChannelProcessor() )
		htmlInfo = "<html>" + info.replace( "\n", "<br>" ) + "</html>"
		label = JLabel( htmlInfo )
		return label

	def getTabText( self, counter, subcomponent=0 ):
		return "File #" + str(counter)

class FigureViewPanel( ViewPanel ):
	"""A panel to view Matlab figure files"""
	def __init__( self, view ):
		super( FigureViewPanel, self ).__init__( view, "Matlab figures" )
		pass

class SpreadsheetViewPanel( ViewPanel ):
	"""A panel to view spreadsheet data files. The first row is taken
	as table headings.
	"""
	def __init__( self, view ):
		self.usingPoi = True
		self.columnFilter = []
		self.plots = []
		log( "Spreadsheet view settings: " + str(view.settings) )
		if view.settings is not None:
			if "display" in view.settings:
				for c in view.settings.get("display"):
					self.columnFilter.append( c )
			if "plot" in view.settings:
				for p in view.settings.get("plot"):
					self.plots.append( p )
		super( SpreadsheetViewPanel, self ).__init__( view, "Spreadsheet data" )

	def loadData( self, filepath ):
		"""Override super class method and use xlrd to read excel data in."""
		if not filepath.endswith( '.xls' ):
			return None
		workbook = None
		try:
			inputStream = FileInputStream( filepath )
			fileSystem = POIFSFileSystem( inputStream )
			workbook = HSSFWorkbook( fileSystem )
		except OldExcelFormatException, e:
			# Problems could happen with old (< Excel 97)
			log( "A very old Excel file version has been detected, switching to JXL" )
			workbook = JXLWorkbook.getWorkbook( File( filepath ) )
			self.usingPoi = False
		except:
			log( "An error occured while trying to access the file: " + filepath )
			workbook = None
		return workbook

	def getColumnHeader( self, nColumn ):
		""" Expects the columns to start with 1
		"""
		colHeading = ""
		colCounter = nColumn
		while colCounter > 0:
			colCounter = colCounter - 1
			colHeading = chr( ( colCounter % 26 ) + ord('A') ) + colHeading
			colCounter = int(colCounter / 26)
		return colHeading

	def getJXLContent( self, data ):
		sheet = data.getSheet(0)
		nRows = sheet.getRows()
		model = DefaultTableModel()
		# Add rows
		for i in range(0, nRows-1):
			 model.addRow( [] )
		# Create a small translation table
		
		# Get Column name from the spreedsheet and set table's column
		addedCols = 0
		availCols = {}
		for i in range(0, sheet.getColumns() ):
			column = sheet.getColumn( i );
			if len(column) > 0:
				commonName = self.getColumnHeader(i+1)
				if self.columnFilter and commonName not in self.columnFilter:
					continue
				if not column[0].getContents() == "":
					columnName = column[0].getContents()
					model.addColumn( columnName )
					availCols[addedCols] = commonName
					addedCols = addedCols + 1
		# Get cell contents and put them in the table
		plotData = {}
		for i in range(0, model.getColumnCount()):
			# check if this column is part of a plot
			plotCoords = None
			for p in self.plots:
				for c in p:
					if availCols[i] == c:
						# Create a new structure for the data, if not present yet
						if p not in plotData:
							plotData[p] = []
						# get all the values in this column
						plotCoords = []
						plotData[p].append( plotCoords )
			if plotCoords is not None:
				# update the model
				for j in range(1, model.getRowCount()):
					cell = sheet.getCell(i, j)
					model.setValueAt( cell.getContents(), j-1, i)
					plotCoords.append( cell.getValue() )
			else:
				# update the model
				for j in range(1, model.getRowCount()):
					cell = sheet.getCell(i, j)
					model.setValueAt( cell.getContents(), j-1, i)
		# Create plots
		charts = []
		for k in plotData:
			chart = Chart2D()
			trace = Trace2DSimple()
			chart.addTrace(trace)
			chartData = plotData[k]
			for i in range(0, len(chartData[0])):
				x = float(chartData[0][i])
				y = float(chartData[1][i])
				trace.addPoint(x, y)
			charts.append( chart )
				
		# close the workbook and free memory
		data.close()
		# create the table
		table = JTable()
		table.setModel(model)
		# Make sure the header is displayed
		panel = JPanel()
		panel.setLayout(BorderLayout());
		panel.add(table.getTableHeader(), BorderLayout.NORTH);
		panel.add(table, BorderLayout.CENTER);
		# all the components together
		components = [ panel ]
		components.extend( charts )
		return components

	def getPOIContent( self, data ):
		sheet = data.getSheetAt( 0 )
		rows = sheet.rowIterator()
		while rows.hasNext():
			row = rows.next()
			log( "Row No.: " + str( row.getRowNum() ) )
			cells = row.cellIterator()
			while cells.hasNext():
				cell = cells.next()
				c = cell.getCellNum()
				commonName = self.getColumnHeader(c+1)
				if self.columnFilter and commonName not in self.columnFilter:
					continue
				log( "Cell No.: " + str( c ) + " Column name: " + commonName )
				cellType = cell.getCellType()
				if cellType == HSSFCell.CELL_TYPE_NUMERIC:
					# cell type numeric
					log( "Numeric value: " + str( cell.getNumericCellValue() ) )
				elif cellType == HSSFCell.CELL_TYPE_STRING:
					# cell type string.
					richTextString = cell.getRichStringCellValue()
					log( "String value: " + richTextString.getString() )
				else:
					# types other than String and Numeric.
					log( "Type not supported." )
		return None


	def getContent( self, data ):
		table = None
		if self.usingPoi:
			table = self.getPOIContent( data )
			table = JPanel()
		else:
			table = self.getJXLContent( data )
		return table

	def getTabText( self, counter, subcomponent=0 ):
		if subcomponent == 0:
			return "Table #" + str(counter)
		else:
			return "Table #" + str(counter) + " Plot #" + str(subcomponent)