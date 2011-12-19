"""
Small script to display various experiment data sets with
the help of Fiji.

Tom Kazimiers, December 2011
"""

import sys

from javax.swing import JPanel, JList, JLabel, JFrame, JButton, ListSelectionModel, DefaultListModel
from java.awt import BorderLayout, GridLayout

# Log a method
def log(msg):
	IJ.log(msg)

# Log a method and exit with return value 1
def exit(msg):
	log(msg)
	sys.exit(1)

# A named protein with a list of markers
class Protein:
	def __init__(self, name):
		self.name = name
		self.markers = []
	def __init__(self, name, markers):
		self.name = name
		self.markers = markers

# An experiment uses a marker to identify a protein
class Experiment:
	def __init__(self, name, protein, marker):
		self.name = name
		self.protein = protein
		self.marker = marker
	@classmethod
	def implicit(cls, protein, marker):
		name = protein + "-" + marker
		return Experiment(name, protein, marker)

# A project has a name and list of proteins
class Project:
	def __init__(self, name, proteins, experiments):
		self.name = name
		self.proteins = proteins
		self.experiments = experiments

# A GUI that supports multiple screens
class GUI:
	def __init__(self, project):
		self.project = project
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
		log( "Showing data for " )

# Creates projects based on a name
def loadProject(name):
	project = None
	if name == "Joao":
		proteins = []
		proteins.append( Protein( "WT", ["SpiderGFP", "EcadGFP", "SASVenus", "LachesinGFP"] ) )
		proteins.append( Protein( "crb11A22", ["SpiderGFP", "EcadGFP", "SASVenus"] ) )
		experiments = []
		experiments.append( Experiment.implicit( "WT", "SpiderGFP" ) )
		experiments.append( Experiment.implicit( "crb11A22", "SASVenus" ) )
		project = Project( name, proteins, experiments )
	else:
		log("Could not find definition for project with name " + name)
	return project
	
# Main entry
def main():
	log("Loading project")
	project = loadProject("Joao")
	if project is None:
		exit("Loading failed, exiting")
	gui = GUI(project)
	gui.show()

# call main
if __name__ == "__main__":
	main()
