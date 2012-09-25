package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ScrollPane;

import java.util.List;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import model.View;
import tools.Helpers;

/**
 * A common panel for displaying views.
 */
public abstract class ViewPanel<T> extends JPanel {
	// the view this panel is displaying
	View view;
	// title of the panel
	String title;
	// a mapping from tab indices to file indices
	List<Integer> tabFileMapping;
	// content of the giles in this view
	List<T> files;
	// tha currently looked at file
	T currentFile;

	public ViewPanel( View view, String title) {
		this( view, title, true );
	}

	public ViewPanel( View view, String title, boolean scrollable ) {
		super( new BorderLayout() );
		this.view = view;
		this.title = title;
		this.tabFileMapping = new ArrayList<Integer>();
		this.files = new ArrayList<T>();

		init( scrollable );
	}

	protected void init(boolean scrollable) {
		this.setBorder( BorderFactory.createTitledBorder( title ) );
		JTabbedPane tabbedPane = new JTabbedPane();

		int counter=0;
		for ( String p : view.getPaths() ) {
			// load data of file
			T data = loadData( p );
			if ( data == null ) {
				Helpers.log( "Could not load file: " + p );
				continue;
			}
			// set first file to the current one
			if (counter == 0) {
				currentFile = data;
			}
			files.add( data );

			List<Component> components = getContent( data );

			int n = 0;
			for ( Component c : components ) {
				tabFileMapping.add( counter );
				/* Unfortunately, the AWT.ScrollPane has to be
				 * used with AWT.Canvas.
				 */
				if ( scrollable ) {
					ScrollPane scroll = new ScrollPane();
					scroll.add( c );
					c = scroll;
				}
				tabbedPane.addTab( getTabText( counter + 1, n ), null, c, p );
				// increase component counter
				n++;
			}
			// increase file counter
			counter++;
		}

		add( tabbedPane, BorderLayout.CENTER );

		// only add listener when there are tabs
		if ( tabbedPane.getTabCount() > 0) {
			/* add the change listener last to avoid events during
			 * initialization.
			 */
			tabbedPane.addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged( ChangeEvent e ) {
						JTabbedPane source = (JTabbedPane) e.getSource();
						int newTabIndex = source.getSelectedIndex();
						int fileIndex = tabFileMapping.get( newTabIndex );
						currentFile = files.get( fileIndex );
						handleTabChange( newTabIndex );
					}
				} );
		}
	}

	public String getTitle() {
		return title;
	}

	protected void handleTabChange( int newTabIndex ) {
		// nothing to do by default
	}

	/**
	 * Loads the data from the given path.
	 */
	protected abstract T loadData( String path );

	/**
	 * Creates a Swing component to be used in the UI, based
	 * on the given file data.
	 */
	protected abstract List<Component> getContent( T data );

	/**
	 * Generates a title text for tab page for file with index
	 * "counter" and subcomponent index "subcomponent" of this
	 * file.
	 */
	protected abstract String getTabText( int counter, int subcomponent );
}
