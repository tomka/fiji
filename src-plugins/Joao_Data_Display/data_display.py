"""
Systems biology data display based on Fiji

Tom Kazimiers, December 2011
"""

import os
import sys
from org.yaml.snakeyaml import Yaml
from java.io import File, FileInputStream
from java.util import List, Map
from java.lang.System import getProperty
sys.path.append( os.path.join( getProperty("fiji.dir") + "/src-plugins/Joao_Data_Display" ) )
from swing_gui import SelectionGUI, DataGUI
from helpers import log, exit
from structures import Condition, Experiment, Project, View

# Creates projects based on a name
def loadSampleProject( name ):
	project = None
	if name == "Joao":
		exConditions = []
		exConditions.append( Condition( "Wildtype", ["SpiderGFP", "EcadGFP", "SASVenus", "LachesinGFP"] ) )
		exConditions.append( Condition( "crb11A22", ["SpiderGFP", "EcadGFP", "SASVenus"] ) )
		inConditions = []
		inConditions.append( Condition( "Position", ["anterior", "posterior"] ) )
		experiments = []
		experiments.append( Experiment.baseOnPathImplicit( \
			Condition.fromNameAndOption("Wildtype", "EcadGFP"), \
			"/Volumes/knustlab/Tom/EcadGFP_heterozygous" ) )
		project = Project( name, exConditions, inConditions, experiments )
	else:
		log("Could not find definition for project with name " + name)
	return project

def loadYAMLProject( path ):
	if not os.path.exists( path ):
		log( "Could not find project file: " + path )
		return None
	else:
		log( "Parsing project file" )
	yaml = Yaml()
	stream = FileInputStream( File( path ) )
	project_data = yaml.loadAll( stream )
	# iterate over documents and try to find project and experiments
	project = None
	experiments_data = []
	for e in project_data:
		print e.toString() + "\n"
		# get the project
		if e.containsKey( "project" ):
			project = e.get( "project" )
		if e.containsKey( "experiment" ):
			experiments_data.append( e.get( "experiment" ) )
	if project is None:
		log( "Could not find project declaration" )
		return None
	projectName = project.get( "name" )
	log( "Found project: " + projectName )
	# Set up filters/conditions of project
	exConditions = []
	inConditions = []
	if project.containsKey( "filters" ):
		log( "Using predefined filters" )
		filter_data = project.get( "filters" )
		# mutual exclusive filters
		if filter_data.containsKey( "exclusive" ):
			exlusive_filters = filter_data.get( "exclusive" )
			for c in exlusive_filters:
				options = []
				for o in exlusive_filters.get( c ):
					options.append( o )
				exConditions.append( Condition( c, options ) )
		# inclusive filters
		if filter_data.containsKey( "inclusive" ):
			inclusive_filters = filter_data.get( "inclusive" )
			for c in inclusive_filters:
				options = []
				for o in inclusive_filters.get( c ):
					options.append( o )
				inConditions.append( Condition( c, options ) )
	else:
		log( "Creating filter conditions from available experiments" )
		log( "--> Not yey supported!" )
		return None
	# Set up base directory
	basedirectory = ""
	if project.containsKey( "basedirectory" ):
		basedirectory = project.get( "basedirectory" )
	viewSettings = {}
	if project.containsKey( "views" ):
		viewSettings = project.get( "views" )
		log( "Using predefined view properties: " + str( viewSettings ) )
	# Setup experiments of project
	experiments = []
	log( "Found " + str( len( experiments_data ) ) + " experiment(s)" )
	for e in experiments_data:
		name = e.get( "name" )
		exp_conditions = []
		conditions_data = e.get( "conditions" )
		for c in conditions_data:
			options = []
			for o in conditions_data.get( c ):
				options.append( o )
			exp_conditions.append( Condition( c, options ) )
		# Look for views or directory, prefer views.
		if e.containsKey( "views" ):
			views = []
			views_data = e.get( "views" )
			for v in views_data:
				try:
					current_view = views_data.get( v )
					paths = []
					for p in current_view.get( "files" ):
						paths.append( os.path.join( basedirectory, p ) )
					# v are the local settings.
					settings = None
					if v in viewSettings:
						settings = viewSettings.get(v).clone()
						for s in current_view:
							settings[s] = current_view.get(s)
					else:
						settings = current_view
					views.append( View( v, paths, settings ) )
				except AttributeError, e:
					log( "No files definition found for view \"" + str(v) + "\" of experiment \"" + name + "\": " + str(e) )
					continue
			experiments.append( Experiment( name, exp_conditions, views ) )
		elif e.containsKey( "directory" ):
			path = os.path.join( basedirectory, e.get( "directory" ) )
			# Check if the path contains subfolders. If so, try to create
			# seperate experiments out of it and use the folder names as
			# experiment names.
			expFiles = []
			expFolders = []
			for p in os.listdir( path ):
				fullpath = os.path.join( path, p )
				if os.path.isdir( fullpath ):
					expFolders.append( p )
				else:
					expFiles.append( p )
			nFiles = len( expFiles )
			nFolders = len( expFolders )
			# Make sure we got files OR folders
			if nFiles > 0 and nFolders > 0:
				log("The experiment date folder \"" + path + "\" contains both, files (" + str(nFiles) + ") and folders (" + str(nFolders) + "). Please have only files OR (experiment-)folders in there.")
				#return None
			if nFolders > 0:
				# Add experiments based on the sub-folders. Use the sub-folder name
				# as experiment names.
				for p in expFolders:
					fullpath = os.path.join( path, p )
					log("  Using subfolder: " + fullpath)
					experiments.append( Experiment.baseOnPath( p, exp_conditions, fullpath, viewSettings ) )
			elif nFiles > 0:
				# Add experiment based on a folder with files only
				experiments.append( Experiment.baseOnPath( name, exp_conditions, path, viewSettings ) )
			else:
				log("The experiment date folder \"" + path + "\" contains no files and no folders. Please correct that.")
				return None

	return Project( projectName, exConditions, inConditions, experiments )

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
	project = loadYAMLProject( "/home/tom/tmp/joao-experiments.yml" )
	#project = loadSampleProject( "Joao" )
	if project is None:
		exit("Loading failed, exiting")
	ctrl = Controler( project )
	ctrl.showSelectionDialog()

# call main
if __name__ == "__main__":
	main()
