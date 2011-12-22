import sys
from ij import IJ

# Log a method
def log(msg):
	IJ.log(msg)

# Log a method and exit with return value 1
def exit(msg):
	log(msg)
	IJ.error( "Data display: good bye" )
