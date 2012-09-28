package ui;

import ij.ImagePlus;

import java.awt.Component;

import java.lang.System;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Properties;

import javax.swing.JComponent;

import model.View;
import tools.Helpers;

import org.icepdf.ri.util.PropertiesManager;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.core.views.DocumentViewController;

/**
 * A panel to view PDF files.
 */
public class FigureViewPanel extends ViewPanel<JComponent> {

	public FigureViewPanel( View view ) {
		super( view, "Figures and documents" );
	}

	@Override
	protected JComponent loadData( String path ) {
		// Make sure we have path data
		if ( path == null ) {
			return null;
		}
		// A controller is needed
		SwingController controller = new SwingController();
		// Create some default settings
		Properties props = new Properties();
		props.setProperty(
			PropertiesManager.PROPERTY_SHOW_UTILITY_SAVE, "false" );
		props.setProperty(
			PropertiesManager.PROPERTY_SHOW_UTILITY_PRINT, "false" );
		props.setProperty(
			PropertiesManager.PROPERTY_SHOW_UTILITY_SEARCH, "false" );
		props.setProperty(
			PropertiesManager.PROPERTY_SHOW_UTILITY_UPANE, "false" );
		props.setProperty(
			PropertiesManager.PROPERTY_SHOW_TOOLBAR_ANNOTATION, "false" );
		props.setProperty(
			PropertiesManager.PROPERTY_SHOW_STATUSBAR, "false" );
		props.setProperty(
			PropertiesManager.PROPERTY_DEFAULT_PAGEFIT,
			new Integer(DocumentViewController.PAGE_FIT_WINDOW_HEIGHT).toString() );
		props.setProperty(
			"application.showLocalStorageDialogs", "false" );
		ResourceBundle res = ResourceBundle.getBundle(
			PropertiesManager.DEFAULT_MESSAGE_BUNDLE );
		PropertiesManager pm =
			new PropertiesManager( System.getProperties(), props, res );
		// Build a SwingViewFactory configured with the controller
		SwingViewBuilder factory = new SwingViewBuilder( controller, pm );
		/* Use the factory to build a JPanel that is pre-configured
		 * with a complete, active Viewer UI.
		 */
		JComponent viewerComponentPanel = factory.buildViewerPanel();
		// Open a PDF document to view
		controller.openDocument( path );
		// Make sure the file has been loaded
		if ( controller.getDocument() == null ) {
			return null;
		} else {	
			return viewerComponentPanel;
		}
	}

	@Override
	protected List<Component> getContent( JComponent data ) {
		return Arrays.asList( (Component) data );
	}

	@Override
	protected String getTabText( int counter, int subcomponent ) {
		return "Figure #" + counter;
	}
}
