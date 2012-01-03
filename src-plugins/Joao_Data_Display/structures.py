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

# An experiment which was made under certain conditions.
class Experiment:
	def __init__(self, name, condition, path):
		self.name = name
		self.condition = condition
		self.path = path
		self.moviePaths = []
		self.figurePath = None
		self.rawPath = None
		self.tablePath = None
		self.init()

	# Creates a new experiment based on looking at all the files
	# in one folder.
	@classmethod
	def fromConditionImplicit(cls, condition, path):
		name = condition.name + "-" + "-".join( condition.options )
		return Experiment(name, condition, path)

	def init(self):
		# Check if the path is actually valid
		if not os.path.exists(self.path):
			log( "Could not find path: " + self.path)
			raise Exception, "Could not find experiment base path: " + self.path
		# Look at the files
		for filename in os.listdir(self.path):
			filePath = os.path.join( self.path, filename )
			log( filePath )
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

# A project has a name, a list of conditions and a list of
# experiments.
class Project:
	def __init__(self, name, exclusiveConditions, inclusiveConditions, experiments):
		self.name = name
		self.exclusiveConditions = exclusiveConditions
		self.inclusiveConditions = inclusiveConditions
		self.experiments = experiments
