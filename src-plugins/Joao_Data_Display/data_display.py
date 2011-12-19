import sys

from javax.swing import JScrollPane, JPanel, JComboBox, JLabel, JFrame

# A named protein with a list of markers
class Protein:
	def __init__(self, name):
		self.name = name
		self.markers = []
	def __init__(self, name, markers):
		self.name = name
		self.markers = markers

# A project has a name and list of proteins
class Project:
	def __init__(self, name):
		self.name = name
		self.proteins = []
	def __init__(self, name, proteins):
		self.name = name
		self.proteins = proteins

def log(msg):
	IJ.log(msg)

def exit(msg):
	log(msg)
	sys.exit(1)

# GUI
def initGUI(project):
	frame = JFrame("Data Display for " + project.name)
	#frame.getContentPane().add(JScrollPane(all))
	frame.pack()
	frame.setVisible(True)

# Creates projects based on a name
def loadProject(name):
	project = None
	if name == "Joao":
		prot1 = Protein("WT", ["SpiderGFP", "EcadGFP", "SASVenus", "LachesinGFP"])
		prot2 = Protein("crb11A22", ["SpiderGFP, EcadGFP, SASVenus"])
		project = Project(name, [prot1, prot2])
	else:
		log("Could not find definition for project with name " + name)
	return project
	
# Main entry
def main():
	log("Loading project")
	project = loadProject("Joao")
	if project is None:
		exit("Loading failed, exiting")
	initGUI(project)

# call main
if __name__ == "__main__":
	main()