import os
import sys
from java.lang.System import getProperty
sys.path.append( os.path.join( getProperty("fiji.dir") + "/src-plugins/Joao_Data_Display" ) )
from helpers import log, exit

# A named experiment condition with a list of options. This could
# for instance be a wildtype experiment with a set of markers:
# 
#   Condition( "Wildtype", ["SpiderGFP", "EcadGFP", "SASVenus"] )
#
class Condition:
	def __init__(self, name, options):
		self.name = name
		self.options = options

	@classmethod
	def fromNameOnly(self, name):
		return Condition( name, [] )

	@classmethod
	def fromNameAndOption(self, name, option):
		return Condition( name, [option] )

	# Checks whether a condition is a subset of another one.
	# That is true if the names are equal and the options
	# of the condition to test against (the parameter) are
	# contained in the instances options.
	def matches( self, condition ):
		if self.name != condition.name:
			return False
		for o in condition.options:
			try:
				i = self.options.index( o )
			except ValueError:
				return False
		return True

# A views is a named container for a set of file paths.
class View:
	movieName = "movie"
	figureName = "matlab"
	metadataName = "metadata"
	tableName = "spreadsheet"

	def __init__(self, name, paths):
		self.name = name
		self.paths = paths

# An experiment which was made under certain conditions.
class Experiment:
	def __init__(self, name, conditions, views):
		self.name = name
		self.conditions = conditions
		self.views = views

	# Creates a new experiment based on looking at all the files
	# in one folder. It expects one Condition and creates the
	# name implicitely out of the conditions name and its options.
	@classmethod
	def baseOnPathImplicit(cls, condition, path):
		name = condition.name + "-" + "-".join( condition.options )
		return Experiment.baseOnPath(name, [condition], path)

	# Creates a new experiment based on looking at all the files
	# in one folder. Expects a list of conditions.
	@classmethod
	def baseOnPath(self, name, conditions, path):
		# Check if the path is actually valid
		if not os.path.exists(path):
			log( "Could not find path: " + path)
			raise Exception, "Could not find experiment base path: " + path
		# Init lists of known views
		movies = []
		figures = []
		metafiles = []
		tables = []
		# Look at the files
		for filename in os.listdir(path):
			filePath = os.path.join( path, filename )
			log( filePath )
			# don't allow folders
			if os.path.isdir( filePath ):
				continue
			# look for movies (*.avi)
			if filename.endswith(".avi"):
				movies.append( filePath )
			# look for matlab figures (*.fig)
			elif filename.endswith(".fig"):
				figures.append( filePath )
			# look for raw data files (*.lsm)
			elif filename.endswith(".lsm"):
				metafiles.append( filePath )
			# look for spreadsheed data (*.xls)
			elif filename.endswith(".xls"):
				tables.append( filePath )
		# Check what we've got and create views
		views = []
		if len(movies) > 0:
			views.append( View( View.movieName, movies ) )
		if len(figures) > 0:
			views.append( View( View.figureName, figures ) )
		if len(metafiles) > 0:
			views.append( View( View.metadataName, metafiles ) )
		if len(tables) > 0:
			views.append( View( View.tableName, tables ) )
		# create new experiment
		return Experiment(name, conditions, views)

	def matches(self, testConditions):
		for tc in testConditions:
			condMatches = False
			for c in self.conditions:
				condMatches = condMatches or c.matches(tc)
			if not condMatches:
				return False
		return True


# A project has a name, a list of conditions and a list of
# experiments.
class Project:
	def __init__(self, name, exclusiveConditions, inclusiveConditions, experiments):
		self.name = name
		self.exclusiveConditions = exclusiveConditions
		self.inclusiveConditions = inclusiveConditions
		self.experiments = experiments
