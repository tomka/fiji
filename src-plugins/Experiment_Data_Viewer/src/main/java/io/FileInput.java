package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import tools.Helpers;
import model.Condition;
import model.Project;
import model.Experiment;
import model.View;

import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

/**
 * Reads a project data from an input file. This file
 * has to be a YAML file.
 */
public class FileInput {

	public static Project read( String path ) {
		try {
			InputStream is = new FileInputStream( path );
			return read( is );
		}
		catch (FileNotFoundException e) {
			Helpers.log("The file " + path + " was not found.");
			return null;
		}
	}

	public static Project read( InputStream is ) {
		// read the file as YAML data
		Yaml yaml = new Yaml();
		Map<String,Object> projectData = null;
		projectData = (Map<String,Object>)yaml.load( is );

		// make sure we have data
		if (projectData == null) {
			Helpers.log("Could not read input.");
			return null;
		}

		// Iterate over documents and try to find projects and experiments
		for (String s : projectData.keySet()) {
			Helpers.logVerbose( s );
			Helpers.logVerbose( projectData.get(s).toString() );
			Helpers.logVerbose( "\n" );
		}

		// construct a new project
		Project p = new Project();

		// get the name
		if (projectData.containsKey("name")) {
			p.setName( (String) projectData.get("name"));
			Helpers.logVerbose("Found project: " + p.getName());
		} else {
			Helpers.log("Could not find a project name.");
			return null;
		}

		// get base directory
		if (projectData.containsKey("basedirectory")) {
			p.setBaseDirectory( (String) projectData.get("basedirectory"));
		}

		// set up filters and conditions
		List<Condition> exConditions = new ArrayList<Condition>();
		List<Condition> inConditions = new ArrayList<Condition>();
		if (projectData.containsKey("filters")) {
			Helpers.logVerbose("Using predefined filters");
			Map<String,Object> fd = (Map<String,Object>) projectData.get("filters");
			// get exclusive filters
			if (fd.containsKey("exclusive")) {
				Map<String,Object> ef = (Map<String,Object>) fd.get("exclusive");
				for (String key : ef.keySet()) {
					exConditions.add(new Condition(key, (List<String>) ef.get(key)));
				}
			}
			// get inclusive filters
			if (fd.containsKey("inclusive")) {
				Map<String,Object> ef = (Map<String,Object>) fd.get("inclusive");
				for (String key : ef.keySet()) {
					inConditions.add(new Condition(key, (List<String>) ef.get(key)));
				}
			}
		} else {
			Helpers.log("Could not find any definiton of the filters.");
			return null;
		}
		p.setExclusiveConditions( exConditions );
		p.setInclusiveConditions( inConditions );

		// set up view settings -- a map of view names to setting maps
		Map<String,Map<String,Object>> viewSettings;
		if (projectData.containsKey("views")) {
			viewSettings = (Map<String,Map<String,Object>>) projectData.get("views");
		} else {
			viewSettings = new HashMap<String,Map<String,Object>>();
		}

		// set up experiments
		List<Experiment> experiments = new ArrayList<Experiment>();
		if (projectData.containsKey("experiments")) {
			List<Map<String,Object>> edata =
				(List<Map<String,Object>>) projectData.get("experiments");
			for ( Map<String,Object> em : edata ) {
				// experiment name
				String name = (String) em.get("name");
				if (name == null) {
					Helpers.log( "Found experiment without name. Aborting." );
					return null;
				}

				// experiment conditions
				List<Condition> conditions = new ArrayList<Condition>();
				Map<String,List<String>> cdata =
					(Map<String,List<String>>) em.get("conditions");
				for (String key : cdata.keySet()) {
					conditions.add( new Condition( key, cdata.get(key) ));
				}

				// look for views and directories, prefer views
				if (em.containsKey("views")) {
					Map<String,Map<String,Object>> viewData =
						(Map<String,Map<String,Object>>) em.get("views");
					List<View> views = parseExperimentViews( viewData,
						viewSettings, p.getBaseDirectory() );
					experiments.add( new Experiment(name, conditions, views) );
				} else if (em.containsKey("directory")) {
					String relPath = (String)em.get("directory");
					String path = Helpers.joinPath(p.getBaseDirectory(), relPath);
					Map<String,String> namedPaths =
						parseExperimentDirectory( path, name );
					for ( String namedPath : namedPaths.keySet() ) {
						String experimentName = namedPaths.get( namedPath );
						experiments.add( new Experiment( experimentName,
							conditions, namedPath, viewSettings ) );
					}
				} else {
					Helpers.log("The experiment named " + name +
						" doesn't contain a views or a directory" +
						" definition. Aborting as this is needed.");
					return null;
				}
			}
		} else {
			Helpers.log("No experiments have been been found!");
		}
		p.setExperiments(experiments);

		Helpers.logVerbose("Finished parsing project description file.");
		return p;
	}

	public static List<View> parseExperimentViews(
			Map<String,Map<String,Object>> vd,
			Map<String,Map<String,Object>> viewSettings, String baseDirectory ) {
		List<View> views = new ArrayList<View>();
		for (String key : vd.keySet()) {
			Map<String,Object> currentView = vd.get(key);
			List<String> paths = new ArrayList<String>();
			// iterate files for this view
			for (String viewFile : (List<String>) currentView.get("files")) {
				paths.add( Helpers.joinPath( baseDirectory, viewFile ) );
			}
			// override global with local settings
			Map<String,Object> settings;
			if (viewSettings.containsKey(key)) {
				settings = new HashMap( viewSettings.get(key) );
				for (String s : currentView.keySet()) {
					settings.put(s, currentView.get(s));
				}
			} else {
				settings = currentView;
			}
			// create new view
			views.add( new View( key, paths, settings ) );
		}
		return views;
	}

	/**
	 * Looks into the given directory and assumes sub-foldes to be
	 * experiment folders for single experiments. If thera are no
	 * sub-folders and files presest on the given path, it will use
	 * the given path as experiment root.
	 */
	public static Map<String,String> parseExperimentDirectory(
			String path, String defaultName ) {
		Map<String,String> namedPaths = new HashMap<String,String>();

		// Make sure the directory exists
		File directory = new File(path);
		if (!directory.exists()) {
			Helpers.log("Could not access path \"" + path +
				"\". It does not exist. Continueing with next experiment.");
			return namedPaths;
		}
		if (!directory.isDirectory()) {
			Helpers.log("Expected \"" + path + "\" to be a directory, " +
				"but it isn't. Continueing with next experiment.");
			return namedPaths;
		}
		/* Check if the path contains subfolders. If so, try
		 * to create separate experiments out of it and use
		 * the folder names as experiment names.
		 */
		List<File> expFiles = new ArrayList<File>();
		List<File> expFolders = new ArrayList<File>();
		File[] files = new File(path).listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				expFolders.add(f);
			} else {
				expFiles.add(f);
			}
		}
		int nFiles = expFiles.size();
		int nFolders = expFolders.size();

		// Make sure we've get files XOR folders
		if (nFiles > 0 && nFolders > 0) {
			Helpers.log("The experiment data folder \"" + path +
				"\" contains both, files (" + nFiles + ") and folders (" +
				nFolders + "). Please have only files OR (experiment-)folders in there.");
		}
		// Prefer folders
		if (nFolders > 0) {
			/* Add named paths based on the sub-folders.
			 * Use the sub-folders's name as experiment name
			 */
			for (File ep : expFolders) {
				Helpers.logVerbose( "Using sub-folder: " + ep.getPath() );
				boolean filesPresent = false;
				for ( File f : ep.listFiles() ) {
					filesPresent = filesPresent || !f.isDirectory();
					if ( filesPresent ) {
						break;
					}
				}
				if ( filesPresent ) {
					namedPaths.put( ep.getPath(), ep.getName() );
				} else {
					Helpers.log( "There are no files in this sub-folder. Ignoring it." );
				}
			}
		} else if (nFiles > 0) {
				namedPaths.put( path, defaultName );
		} else {
			Helpers.log("The experiment data folder \"" + path +
				"\" contains no files and no folders. Please correct that.");
			return namedPaths;
		}

		return namedPaths;
	}
}
