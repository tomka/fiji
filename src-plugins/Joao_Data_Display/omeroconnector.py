from omero import client
from omero import rtypes
from omero.model import Dataset, Project, DatasetAnnotationLink, TagAnnotationI, ProjectAnnotationLinkI, DatasetAnnotationLinkI, ProjectDatasetLinkI, ProjectI, DatasetI, OriginalFileI, FileAnnotationI, ImageAnnotationLinkI, ImageI
from omero.sys import ParametersI
from pojos import ProjectData, DatasetData

from jarray import zeros
from java.util import ArrayList, Formatter, Arrays
from java.io import File, FileInputStream, BufferedInputStream
from java.nio import ByteBuffer
from java.security import MessageDigest, DigestInputStream
from java.lang import StringBuffer, Long

import subprocess as sub

def byteArray2Hex( byteHash):
	""" Converts a byte array to a hex array. """
	formatter = Formatter()
	for b in byteHash:
		formatter.format("%02x", b)
	return formatter.toString()

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
		self.tagCache = {}

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
		# Return the cached tag, if possible
		if text in self.tagCache:
			return self.tagCache[ text ]
		# Create a new tag
		tag = TagAnnotationI()
		tag.setTextValue( rtypes.rstring( text ) )
		tag.setDescription( rtypes.rstring( description ) )
		tag.setNs( rtypes.rstring( namespace ) )
		# Save the tag on the server
		tag = self.save( tag )
		# Put the new tag in the tag cache
		self.tagCache[ text ] = tag

		return tag

	def create_new_file_annotation( self, originalFile, description="", namespace="" ):
		""" Creates a new FileAnnotation instance. Expects that the
		original File in DB and raw data uploaded."""
		fa = FileAnnotationI()
		fa.setFile(originalFile)
		fa.setDescription( rtypes.rstring( description) )
		fa.setNs( rtypes.rstring( namespace ) )

		return fa

	def save_new_file( self, fileObj, mimeType="application/octet-stream" ):
		""" Creates a new OriginalFile instance. It won't create the SHA-1 sum."""
		name = fileObj.getName()
		absolutePath = fileObj.getAbsolutePath()
		path = absolutePath[ 0 : len(absolutePath) - len(name) ]
		# create the original file object.
		originalFile = OriginalFileI()
		originalFile.setName( rtypes.rstring( name ) )
		originalFile.setPath( rtypes.rstring( path ) )
		originalFile.setSize( rtypes.rlong( fileObj.length() ) )
		originalFile.setMimetype( rtypes.rstring(mimeType) )

		# The SHA-1 calculator
		"""
		algorithm = MessageDigest.getInstance("SHA1")
		fis = FileInputStream( fileObj )
		bis = BufferedInputStream(fis)
		dis = DigestInputStream(bis, algorithm)
		# read the file and update the hash calculation
		while dis.read() != -1:
			continue
		# get the hash value as byte array
		fileBytheHash = algorithm.digest()
		fileHexHash = byteArray2Hex( fileBytheHash )
		"""
		# For now, use the sha1sum tool
		p = sub.Popen('/usr/bin/sha1sum \'' + absolutePath + '\'',shell=True,stdout=sub.PIPE,stderr=sub.PIPE)
		output, errors = p.communicate()
		fileHexHash = output[0:output.find(" ")]

		# Assign the SHA-1 string to the OriginalFile instance
		originalFile.setSha1( rtypes.rstring( fileHexHash ) )

		# Save the original file
		originalFile = self.save( originalFile )

		## Read in the file's content
		INC = 262144
		# Initialize the service to load the raw data
		rawFileStore = self.entry.createRawFileStore()
		rawFileStore.setFileId( originalFile.getId().getValue() )

		stream = FileInputStream( fileObj )
		pos = 0
		buf = zeros(INC, 'b')
		rlen = stream.read(buf)
		while rlen > 0:
			rawFileStore.write(buf, pos, rlen)
			pos += rlen
			bbuf = ByteBuffer.wrap(buf)
			bbuf.limit(rlen)
			rlen = stream.read(buf)
		stream.close()

		originalFile = rawFileStore.save()
		# Important to close the service
		rawFileStore.close()

		return (originalFile, fileHexHash)


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

def exportProjectToOmero( project, host, port, username, password, importerPath, namespace="data" ):
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
		# Add the experiment conditions as tags
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
		# Add the related files as file annotations
		imagePaths = []
		fileannotations = []
		for nv,v in enumerate(e.views):
			print( "\tView: " + v.name )
			for path in v.paths:
				# import meta data view files as images
				if v.name == v.metadataName:
					print( "\t\tImage: " + path )
					imagePaths.append( path )
				else:
					print( "\t\tAnnotation: " + path )
					fileObj = File(path)
					if not fileObj.exists():
						print( "\t\t\tCouldn't find file, ignoring it." )
						continue
					# save the originalFile object, upload it
					originalFile, fileHash = exporter.save_new_file( fileObj )
					print( "\t\t\tSHA1 hash: " + fileHash )
					# Create and save the new file annotation
					fDesc = "A file belonging to experiment/dataset " + e.name + "."
					fa = exporter.create_new_file_annotation( originalFile, fDesc, namespace )
					fa = exporter.save(fa)
					fileannotations.append( fa )

		description += "."
		datasetData.setDescription(description);
		# Add the dataset to OMERO
		d = exporter.save( datasetData.asDataset() )

		# Link dataset to project
		link = ProjectDatasetLinkI()
		link.setChild( DatasetI( d.id.getValue(), False ) )
		link.setParent( ProjectI( p.id.getValue(), False ) )
		r = exporter.save( link )

		# Import images
		# At the moment it is easier to just call the CLI importer
		arguments = "-s " + host + " -u " + username + " -w " + password + " -d " + str(d.getId().getValue())
		for ip in imagePaths:
			iFile = File(ip)
			iName = "\"" + iFile.getName() + "\""
			iDesc = "\"An image of experiment/dataset " + e.name + ".\""
			arguments += " -n " + iName + " -x " + iDesc
			print( ">>>>>>>>>>>>>> IMPORTING " + ip + " <<<<<<<<<<<<<<<<<" )
			call = importerPath + " " + arguments + ' \'' + ip + '\''
			print( "Call: " + call )
			pcall = sub.Popen(call, shell=True, stdout=sub.PIPE, stderr=sub.PIPE)
			output, errors = pcall.communicate()
			print(output)
			print( ">>>>>>>>>>>>>> DONE <<<<<<<<<<<<<<<<<" )

		# Link annotations to datasets
		for a in annotations:
			# Link to datasets
			link = DatasetAnnotationLinkI()
			link.setChild( a )
			link.setParent( DatasetI( d.id.getValue(), False ))
			r = exporter.save( link )

		# Link  file annotations to datasets
		for a in fileannotations:
			# Link to datasets
			link = DatasetAnnotationLinkI()
			link.setChild( a )
			link.setParent( DatasetI( d.id.getValue(), False ))
			r = exporter.save( link )

		# Link annotations to images
		param = ParametersI()
		param.exp( rtypes.rlong(exporter.userId) )
		param.leaves() # load images
		print("\tAnnotating images")
		uid = Long(d.getId().getValue())
		results = exporter.containerProxy.loadContainerHierarchy(Dataset.canonicalName, [uid], param)
		if results is not None:
			for r in results:
				dataset = DatasetData( r )
				images = dataset.getImages()
				for img in images:
					print( "\tAnnotating image (id " + str(img.getId()) + "): " + img.getName() )
					for a in annotations:
						link = ImageAnnotationLinkI()
						link.setChild( a )
						link.setParent( ImageI( img.getId(), False ) )
						r = exporter.save( link )
						print("\tannotated")
		else:
			print( "\tNo images found." )

		print( "Created data set: " + description )

	exporter.closeConnection()
	print( "Done" )