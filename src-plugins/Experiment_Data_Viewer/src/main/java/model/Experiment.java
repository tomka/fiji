package model;

import java.io.File;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

import model.Condition;
import model.View;
import tools.Helpers;

/**
 * An experiment with a certain set of conditions.
 *
 */
public class Experiment {
	// The name of the experiment
	String name;
	// Conditions under which the experiment was made
	List<Condition> conditions;
	// Views associated with this experiment
	List<View> views;

	public Experiment(String name, List<Condition> conditions,
			List<View> views) {
		init( name, conditions, views );
	}

	/**
	 * Creates a new Experiment by looking at the files in a path to
	 * build the needed views. To decide which view to build, it is looked
	 * at the file's extension.
	 */
	public Experiment(String name, List<Condition> conditions,
			String path, Map<String,Map<String,Object>> viewSettings) {
		init( name, conditions, null );
		if ( path == null ) {
			Helpers.log( "The specified path for this experiment is <null>," +
				" aborting further initalization of the experiment." );
			return;
		}
		File dir = new File( path );
		/* Return, if the directory is not present or if it
		 * is a file.
		 */
		if ( ! (dir.exists() && dir.isDirectory()) ) {
			Helpers.log("Could not create views for experiment " + name +
				", because the given path (" + path + ") does not exist or is a file.");
			return;
		}
		/* Iterate over all the files and map identified
		 * files to view types.
		 */
		Map< View.ViewType, List<String> > viewPaths =
			new HashMap< View.ViewType, List<String> >();
		for ( File f : dir.listFiles() ) {
			View.ViewType type = View.ViewType.UNKNOWN;
			String fileName = f.getName();
			/* use the file's extension to let the View class decide
			 * about its type.
			 */
			int lastDotIdx = fileName.lastIndexOf( "." );
			if ( lastDotIdx >= 0 && lastDotIdx < (fileName.length() - 1) ) {
				String ext = fileName.substring( lastDotIdx + 1,
					fileName.length() );
				type = View.getTypeForExtension( ext );
				if ( !viewPaths.containsKey( type ) ) {
					// we haven't seen this type before
					viewPaths.put( type, new ArrayList<String>() );
				}
				// add the current path to the type's path list
				viewPaths.get(type).add( f.getPath() );
			}
		}
		// Build a view for every type found.
		for ( View.ViewType type : viewPaths.keySet() ) {
			String viewName = View.typeToString( type );
			Map<String,Object> settings = null;
			if ( viewSettings != null &&
					viewSettings.containsKey( viewName ) ) {
				// we have additional settings available, use it
				settings = viewSettings.get( viewName );
			}
			views.add( new View( type, viewPaths.get( type ), settings ) );
		}
		Helpers.logVerbose( "Scanned directory " + path + " and created " +
			views.size() + " views." );
	}

	private void init(String name, List<Condition> conditions,
			List<View> views ) {
		// make sure we have a valid name
		if (name == null) {
			this.name = "";
		} else {
			this.name = name;
		}
		// make sure we have a valid conditions list
		if (conditions == null) {
			this.conditions = new ArrayList<Condition>();
		} else {
			this.conditions = conditions;
		}
		// make sure we have a valid views list
		if (views == null) {
			this.views = new ArrayList<View>();
		} else {
			this.views = views;
		}
	}

	public boolean matches(Iterable<Condition> testConditions) {
		for (Condition tc : testConditions) {
			if (! matches(tc) ) {
				return false;
			}
		}
		return true;
	}

	public boolean matches(Condition testCondition) {
		boolean condMatches = false;
		for (Condition c : conditions) {
			condMatches = condMatches || c.matches(testCondition);
		}
		return condMatches;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public List<View> getViews() {
		return views;
	}

	@Override
	public String toString() {
		String viewInfo = views.size() == 0 ? "(No views)" : Arrays.toString(views.toArray());
		return "Experiment " + name + "\n\t" +
			Arrays.toString(conditions.toArray()) + "\n\t" + viewInfo;
	}
}
