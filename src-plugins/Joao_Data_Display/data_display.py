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
from structures import Condition, Experiment, Project

# Creates projects based on a name
def loadSampleProject( name ):
	project = None
	if name == "Joao":
		conditions = []
		conditions.append( Condition( "WT", ["SpiderGFP", "EcadGFP", "SASVenus", "LachesinGFP"] ) )
		conditions.append( Condition( "crb11A22", ["SpiderGFP", "EcadGFP", "SASVenus"] ) )
		experiments = []
		experiments.append( Experiment.fromConditionImplicit( \
			Condition.fromNameAndOption("WT", "EcadGFP"), \
			"/Volumes/knustlab/Tom/EcadGFP_heterozygous" ) )
		project = Project( name, conditions, experiments )
	else:
		log("Could not find definition for project with name " + name)
	return project

# A controler class that is responsible for showing the views
# or manipulating data
class Controler:
	def __init__(self, project):
		self.project = project
		self.uis = []

	def showSelectionDialog(self):
		gui = SelectionGUI(self)
		self.uis.append(gui)
		gui.show()

	def showDataDialog(self, experiment):
		gui = DataGUI(experiment)
		self.uis.append(gui)
		gui.show()

	def exitProgram(self):
		# kill available GUIs
		for ui in self.uis:
			ui.close()
		log( "Good bye" )

# Main entry
def main():
	log("Loading project")
	project = loadSampleProject("Joao")
	if project is None:
		exit("Loading failed, exiting")
	ctrl = Controler( project )
	ctrl.showSelectionDialog()

# call main
if __name__ == "__main__":
	main()
