package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import model.Experiment;
import model.View;
import tools.Helpers;

public class DataGUI extends UI {
	// the experiment this instance works with
	Experiment experiment;
	// frame of the data gui
	JFrame frame;
	// panel containing all views
	JPanel dataPanel;

	public DataGUI( Experiment experiment ) {
		this.experiment = experiment;
		init();
	}

	protected void init() {
		// initialize our frame
		frame = new JFrame( "Data display for experiment "
			+ experiment.getName() );
		frame.setSize( 500, 400 );
		frame.setLayout( new BorderLayout() );
		frame.setVisible( false );

		/* find out how many views need to be created and
		 * do a layout for them.
		 */
		int numViews = experiment.getViews().size();
		boolean searchingLayout = true;
		int xDim = 1;
		int yDim = 1;
		while ( searchingLayout ) {
			searchingLayout = xDim * yDim < numViews;
			if (!searchingLayout) {
				break;
			}
			if ( xDim < yDim ) {
				yDim++;
			} else {
				xDim++;
			}
		}
		Helpers.logVerbose( "Creating space for " + xDim + "x" + yDim +
			" views (" + numViews + " views are available)" );

		// create and populate the data panel
		dataPanel = new JPanel( new GridLayout( yDim, xDim ) );
		for ( View v : experiment.getViews() ) {
			if ( v.getType() == View.ViewType.MOVIE ) {
				dataPanel.add( new MovieViewPanel( v ) );
			} else if ( v.getType() == View.ViewType.FIGURE ) {
				dataPanel.add( new FigureViewPanel( v ) );
			} else if ( v.getType() == View.ViewType.METADATA ) {
				dataPanel.add( new MetadataViewPanel( v ) );
			} else if ( v.getType() == View.ViewType.TABLE ) {

			} else if ( v.getType() == View.ViewType.UNKNOWN ) {
				// do nothing for unknown files
			}
		}
		// complete the frame
		frame.add( dataPanel, BorderLayout.CENTER );
		JButton closeButton = new JButton( "Close" );
		closeButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					close();
  				}
			}
		);
		frame.add( closeButton, BorderLayout.SOUTH );
		frame.pack();
	}

	@Override
	public void show() {
		frame.setVisible( true );
	}

	@Override
	public void close() {
		frame.setVisible( false );
		frame.dispose();
	}
}
