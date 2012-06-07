from omero import client
from omero import rtypes
from omero.model import Project, DatasetAnnotationLink, TagAnnotationI, ProjectAnnotationLinkI, DatasetAnnotationLinkI, ProjectDatasetLinkI, ProjectI, DatasetI
from omero.sys import ParametersI
from pojos import ProjectData, DatasetData

from java.util import ArrayList

class OmeroExporter:
	def __init__(self, host, port, username, password):
		self.host = host
		self.port = port
		self.username = username
		self.password = password
		self.client = None
		self.entry = None
		self.containerProxy = None
		self.metadataProxy = None
		self.userId = None
		self.groupId = None

	def connect(self):
		cln = client(self.host, self.port)
		encryptedEntry = cln.createSession(self.username, self.password) # ServiceFactoryPrx
		self.client = cln.createClient(False)
		self.entry = self.client.getSession()
		self.containerProxy = self.entry.getContainerService()
		self.metadataProxy = self.entry.getMetadataService()

		# Retrieve the user and group id.
		adminService = self.entry.getAdminService()
		eventContext = adminService.getEventContext()
		self.userId = eventContext.userId
		self.groupId = eventContext.groupId

	def get_project_info(self):
		info = "UserID: " + str(self.userId) + " GroupID: " + str(self.groupId)
		# Get project and dataset data
		param = ParametersI()
		param.exp( rtypes.rlong(self.userId) )
		# param.leaves() # indicate to load the images
		# param.noLeaves() # no images loaded, this is the default value.

		results = self.containerProxy.loadContainerHierarchy( Project.canonicalName, ArrayList(), param)

		for r in results:
			project = ProjectData( r )
			datasets = project.getDatasets()
			info += "Project name: " + project.getName() + ", description: " + project.getDescription()
			for ds in datasets:
				info += "\tDataset: " + ds.getName()
				annotations =ds.getAnnotations()
				if annotations is None:
					info += "\t\t(No annotations)"
				else:
					for a in annotations:
						info += a

	def print_annotations(self, namespace):
		nsToInclude = ArrayList()
		nsToInclude.add( namespace )
		nsToExclude = ArrayList()

		annotations = self.metadataProxy.loadSpecifiedAnnotations(TagAnnotationI.canonicalName, nsToInclude, nsToExclude, param)
		if len(annotations) == 0:
			print( "(No annotations found)" )
		else:
			for a in annotations:
				print("---")
				print(a.getTextValue().getValue())

	def create_new_tag( self, text, description="", namespace="" ):
		""" Creates a new IObject TagAnnotationI object. """
		tag = TagAnnotationI()
		tag.setTextValue( rtypes.rstring( text ) )
		tag.setDescription( rtypes.rstring( description ) )
		tag.setNs( rtypes.rstring( namespace ) )
		return tag

	def test_tagging(self):
		print( "[Annotation test]" )

		ns = "test_namespace"

		# Add tags
		#Using the IObject.
		tagP = self.create_new_tag( "new project tag", "new project tag", ns )
		tagD = self.create_new_tag( "new dataset tag", "new dataset tag", ns )

		# link project and annotation
		linkP = ProjectAnnotationLinkI()
		linkP.setChild(tagP)
		linkD = DatasetAnnotationLinkI()
		linkD.setChild(tagD)

		# Add an annotation
		# Get project and dataset data
		param = ParametersI()
		param.exp( rtypes.rlong(self.userId) ) # load the annotation for a given user.
		#param.leaves() # indicate to load the images
		# param.noLeaves() # no images loaded, this is the default value.

		results = proxy.loadContainerHierarchy( Project.canonicalName, ArrayList(), param)

		print( "Annotations before:" )
		self.print_annotations( ns )

		addedP = False
		addedD = False
		for r in results:
			if not r.annotationLinksLoaded:
				#r.reloadAnnotationLinks(r)
				print("Annotations are not loadod")
			#for a in r.linkedAnnotationList():
			#	print(a)

			project = ProjectData( r )
			if not addedP:
				linkP.setParent( r );
				addedP = True
			datasets = project.getDatasets()
			for ds in datasets:
				if not addedD:
					linkD.setParent( ds.asDataset() );
					addedD = True

		#rP = entryUnencrypted.getUpdateService().saveAndReturnObject(linkP);
		#rD = entryUnencrypted.getUpdateService().saveAndReturnObject(linkD);

		print( "Annotations after:" )
		self.print_annotations( ns )

	def save(self, obj):
		return self.entry.getUpdateService().saveAndReturnObject(obj)

	def closeConnection(self):
		self.client.closeSession()

def exportProjectToOmero( project, host, port, username, password, namespace="data" ):
	# Create a new exporter
	exporter = OmeroExporter(host, port, username, password)
	exporter.connect()

	# Create a new OMERO project
	pData = ProjectData()
	pData.setName( project.name )
	p = exporter.save( pData.asProject() )

	# Create the experiment datasets
	for e in project.experiments:
		# Each experiment becomes a data set in OMERO, using pojo object
		datasetData = DatasetData()
		datasetData.setName( e.name );
		description = "This is the data of the experiment " + e.name + ". It has the following conditions: "
		annotations = []
		for nc,c in enumerate(e.conditions):
			# Add a condition separator if needed
			if nc > 0:
				description += ", "
			description += c.name
			if len(c.options) > 0:
				description += ": "
				for n,o in enumerate(c.options):
					# Add an option separator if needed
					if n > 0:
						description += ", "
					# Remember this option in the description and create a tag
					description += o
					tName = c.name + "-" + o
					tDesc = "A tag for a single experiment condition: " + tName
					optionTag = exporter.create_new_tag( tName, tDesc, namespace )
					annotations.append( optionTag )
		description += "."
		datasetData.setDescription(description);
		# Add the dataset to OMERO
		d = exporter.save( datasetData.asDataset() )

		# Link dataset to project
		link = ProjectDatasetLinkI()
		link.setChild( DatasetI( d.id.getValue(), False ) )
		link.setParent( ProjectI( p.id.getValue(), False ) )
		r = exporter.save( link )

		# Link annotations
		for a in annotations:
			link = DatasetAnnotationLinkI()
			link.setChild( a )
			link.setParent( DatasetI( d.id.getValue(), False ))
			r = exporter.save( link )
		print( "Created data set: " + description )

	exporter.closeConnection()