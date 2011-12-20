"""
Swing based GUI for Joao's Data Display
"""

import os
import sys
from java.lang.System import getProperty
from javax.swing import JPanel, JList, JLabel, JFrame, JButton, ListSelectionModel, DefaultListModel
from java.awt import BorderLayout, GridLayout
sys.path.append( os.path.join( getProperty("fiji.dir") + "/src-plugins/Joao_Data_Display" ) )
from helpers import log, exit

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

# A GUI that displays information for one particular experiment
class DataGUI:
	def __init__(self, experiment):
		self.experiment = experiment
		self.frame = None
		self.init()

	def init(self):
		frame = JFrame( "Data Display for experiment " + self.experiment.name )
		frame.setSize( 400, 300 )
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
		moviePanel.add( JLabel( "Movies" ), BorderLayout.NORTH )
		#for mp in self.experiment.moviePaths:

		dataPanel.add( moviePanel )
		# Second, the matlab figure
		figurePanel = JPanel( BorderLayout() )
		figurePanel.add( JLabel( "Matlab figure" ), BorderLayout.NORTH )
		dataPanel.add( figurePanel )
		# Third, the excel sheet graph
		tablePanel = JPanel( BorderLayout() )
		tablePanel.add( JLabel( "Spreadsheet data" ), BorderLayout.NORTH )
		dataPanel.add( tablePanel )
		# Last, the lsm file meta data
		metadataPanel = JPanel( BorderLayout() )
		metadataPanel.add( JLabel( "Meta data" ), BorderLayout.NORTH )
		dataPanel.add( metadataPanel )
		# Add all to the frame
		frame.add( dataPanel, BorderLayout.CENTER )
		frame.add( JButton("Close", actionPerformed=self.close), BorderLayout.SOUTH)

	def show(self):
		self.frame.setVisible( True )

	def close(self, event):
		self.frame.setVisible( False )