"""
Small script to display various experiment data sets with
the help of Fiji.

Tom Kazimiers, December 2011
"""

import os
import sys
from java.lang.System import getProperty
sys.path.append( os.path.join( getProperty("fiji.dir") + "/src-plugins/Joao_Data_Display" ) )
from swing_gui import SelectionGUI, DataGUI
from helpers import log, exit

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
	def __init__(self, name, protein, marker, path):
		self.name = name
		self.protein = protein
		self.marker = marker
		self.path = path
		self.moviePaths = []
		self.figurePath = None
		self.rawPath = None
		self.tablePath = None
		self.init()

	@classmethod
	def implicit(cls, protein, marker, path):
		name = protein + "-" + marker
		return Experiment(name, protein, marker, path)

	def init(self):
		# Check if the path is actually valid
		if not os.path.exists(self.path):
			log( "Could not find path: " + self.path)
			raise Exception, "Could not find experiment base path: " + self.path
		# Look at the files
		for filename in os.listdir(self.path):
			filePath = os.path.join( self.path, filename )
			# don't allow folders
			if os.path.isdir( filePath ):
				continue
			# look for movies (*.avi)
			if filename.endswith(".avi"):
				self.moviePaths.append( filePath )
			# look for matlab figures (*.fig)
			elif filename.endswith(".fig"):
				self.figurePath = filePath
			# look for raw data files (*.lsm)
			elif filename.endswith(".lsm"):
				self.rawPath = filePath
			# look for spreadsheed data (*.xls)
			elif filename.endswith(".xls"):
				self.tablePath = filePath

# A project has a name and list of proteins
class Project:
	def __init__(self, name, proteins, experiments):
		self.name = name
		self.proteins = proteins
		self.experiments = experiments

# Creates projects based on a name
def loadProject(name):
	project = None
	if name == "Joao":
		proteins = []
		proteins.append( Protein( "WT", ["SpiderGFP", "EcadGFP", "SASVenus", "LachesinGFP"] ) )
		proteins.append( Protein( "crb11A22", ["SpiderGFP", "EcadGFP", "SASVenus"] ) )
		experiments = []
		#experiments.append( Experiment.implicit( "WT", "SpiderGFP", "") )
		#experiments.append( Experiment.implicit( "crb11A22", "SASVenus", "" ) )
		experiments.append( Experiment.implicit( "WT", "EcadGFP", "/Volumes/knustlab/Tom/EcadGFP_heterozygous" ) )
		project = Project( name, proteins, experiments )
	else:
		log("Could not find definition for project with name " + name)
	return project

# A controler class that is responsible for showing the views
# or manipulating data
class Controler:
	def __init__(self, project):
		self.project = project

	def showSelectionDialog(self):
		gui = SelectionGUI(self)
		gui.show()

	def showDataDialog(self, experiment):
		gui = DataGUI(experiment)
		gui.show()
	
# Main entry
def main():
	log("Loading project")
	project = loadProject("Joao")
	if project is None:
		exit("Loading failed, exiting")
	ctrl = Controler( project )
	ctrl.showSelectionDialog()

# call main
if __name__ == "__main__":
	main()
