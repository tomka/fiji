package model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A view is a named container for a set of file paths.
 */
public class View {
	// A set of available types of views
	public enum ViewType { MOVIE, FIGURE, METADATA, TABLE, UNKNOWN };
	// The type of this view
	protected ViewType type;
	// All file paths associated with this view
	protected List<String> paths;

	/**
	 * Create a new named view associated to some paths.
	 */
	public View(String name, List<String> paths, Map<String,Object> settings) {
		ViewType viewType = stringToType( name );
		init( viewType, paths, settings );
	}

	/**
	 * Create a new named view associated to some paths.
	 */
	public View(ViewType type, List<String> paths, Map<String,Object> settings) {
		init( type, paths, settings );
	}

	protected void init(ViewType type, List<String> paths, Map<String,Object> settings) {
		this.type = type;
		this.paths = paths;
	}

	public static String typeToString( ViewType type ) {
		if (type == ViewType.MOVIE) {
			return "movie";
		} else if (type == ViewType.FIGURE) {
			return "figure";
		} else if (type == ViewType.METADATA) {
			return "metadata";
		} else if (type == ViewType.TABLE) {
			return "spreadsheet";
		} else {
			return "unknown";
		}
	}

	public static ViewType stringToType( String type ) {
		if ( type.equals( "movie" ) ) {
			return ViewType.MOVIE;
		} else if ( type.equals( "figure" ) ) {
			return ViewType.FIGURE;
		} else if ( type.equals( "metadata" ) ) {
			return ViewType.METADATA;
		} else if ( type.equals( "spreadsheet" ) ) {
			return ViewType.TABLE;
		} else {
			return ViewType.UNKNOWN;
		}
	}

	/**
	 * Returs a view type for a given extension.
	 */
	public static ViewType getTypeForExtension( String ext ) {
		if (ext.equals("avi")) {
			return ViewType.MOVIE;
		} else if (ext.equals("fig")) {
			return ViewType.FIGURE;
		} else if (ext.equals("pdf")) {
			return ViewType.FIGURE;
		} else if (ext.equals("lsm")) {
			return ViewType.METADATA;
		} else if (ext.equals("xls")) {
			return ViewType.TABLE;
		} else {
			return ViewType.UNKNOWN;
		}
	}

	/**
	 * Return the name of this view.
	 */
	public ViewType getType() {
		return type;
	}

	/**
	 * Return the associated paths.
	 */
	public List<String> getPaths() {
		return paths;
	}

	@Override
	public String toString() {
		return "View (" + type + ") with files: " + Arrays.toString(paths.toArray());
	}
}
