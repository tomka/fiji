package ui;

import ij.IJ;
import ij.gui.ImageCanvas;
import ij.ImagePlus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.Runnable;
import java.lang.Thread;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.plugins.LociImporter;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import model.View;
import tools.Helpers;

/**
 * Provides a panel that allows the display and control
 * of movie playback.
 */
public class MovieViewPanel extends ViewPanel<ImagePlus> {
	// available FPS settings
	Integer[] availableFPS = { 2, 5, 10, 20, 30, 50 };
	// actual FPS setting used
	int moviePanelFPS = 10;
	// indicates if there is currently a movie playing
	boolean moviePlaying;
	// the currently displayed image canvas
	ImageCanvas currentCanvas;
	// a collection of canvases to display all images
	List<ImageCanvas> canvases;
	// a label to show the current slice/frame number
	JLabel sliceLabel;
	// a stop button to stop the movie playback
	JButton stopButton;
	// slider to move through the slices/frames
	JSlider sliceSlider;
	// thread that deals with movie playback
	Thread animationThread;

	// listeners for different UI compononts
	ActionListener comboBoxListener;
	ActionListener playMovieListener;
	ActionListener stopMovieListener;
	ActionListener prevFrameListener;
	ActionListener nextFrameListener;
	ChangeListener sliderListener;

	/**
	 * An animation Runnable to drive the forwarding of frames
	 * during movie playback.
	 */
	protected class Animator implements Runnable {
		public void run() {
			while ( moviePlaying ) {
				moveToNextFrame();
				try {
					Thread.sleep( (long) (1000.0 / moviePanelFPS) );
				} catch (InterruptedException e) {
					Helpers.log( "Movie playing thread got interrupted." );
				}
				// stop playing at last frame
				if ( isAtLastFrame() ) {
					stopMovie();
					break;
				}
			}
		}
	}

	public MovieViewPanel( View view ) {
		super( view, "Movies" );
		initListeners();
		init();
	}

	protected void initListeners() {
		sliderListener = new ChangeListener() {
			@Override
			public void stateChanged( ChangeEvent e ) {
				int val = sliceSlider.getValue();
				setCurrentFrame( val );
			}
		};

		comboBoxListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				JComboBox source = (JComboBox) e.getSource();
				moviePanelFPS = availableFPS[ source.getSelectedIndex() ];
			}
		};

		playMovieListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				playMovie();
			}
		};

		stopMovieListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				stopMovie();
			}
		};

		prevFrameListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				moveToPrevFrame();
			}
		};

		nextFrameListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				moveToNextFrame();
			}
		};
	}

	protected void init() {
		if ( view.getPaths().size() == 0 ) {
			add( new JLabel( "Did not find any files." ), BorderLayout.CENTER );
		} else {
			JPanel controlPanel = new JPanel();
			controlPanel.add( new JLabel( "FPS:" ) );
			// init slice label
			sliceLabel = new JLabel();
			// init slice slider
			sliceSlider = new JSlider();
			sliceSlider.addChangeListener( sliderListener );
			// FPS commbo box
			JComboBox fpsComboBox = new JComboBox( availableFPS );
			fpsComboBox.setSelectedIndex( 2 );
			fpsComboBox.addActionListener( comboBoxListener );
			// play button
			JButton playButton = new JButton( "Play" );
			playButton.addActionListener( playMovieListener );
			// stop button
			stopButton = new JButton( "Pause" );
			stopButton.addActionListener( stopMovieListener );
			// previous frame button
			JButton prevButton = new JButton( "Prev" );
			prevButton.addActionListener( prevFrameListener );
			// next frame button
			JButton nextButton = new JButton( "Next" );
			nextButton.addActionListener( nextFrameListener );
			// add all components to the control panel
			controlPanel.add( fpsComboBox );
			controlPanel.add( playButton );
			controlPanel.add( stopButton );
			controlPanel.add( prevButton );
			controlPanel.add( nextButton );
			controlPanel.add( sliceSlider );
			controlPanel.add( sliceLabel );
			// add control panel to ourself
			add( controlPanel, BorderLayout.SOUTH );

			/* Trigger tab change event handler to select the
			 * first tab, if possible.
			 */
			handleTabChange( 0 ); 
		}
	}

	@Override
	protected ImagePlus loadData( String path ) {
		try {
			ImporterOptions options = new ImporterOptions();
			options.setId( path );
			options.setSplitChannels( false );
			options.setWindowless( true );
			options.setVirtual( false );
			ImagePlus[] imps = BF.openImagePlus( options );
			if ( imps.length == 0 ) {
				Helpers.log( "Could not load image" );
				return null;
			}
			return imps[0];
		} catch ( Exception e ) {
			Helpers.log( "Error while loading file: " + path + " -- Mesage: " + e.getMessage() );
			System.out.println( "Trace:" );
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected List<Component> getContent( ImagePlus data ) {
		ImageCanvas ic = new ImageCanvas( data );
		/* this method is called while construction, so we need
		 * to initialize the canvases list.
		 */
		if ( canvases == null ) {
			canvases = new ArrayList<ImageCanvas>();
		}
		canvases.add( ic );
		ic.setPreferredSize( new Dimension( ic.getWidth(), ic.getHeight() ) );
		return Arrays.asList( (Component) ic );
	}

	@Override
	protected String getTabText( int counter, int subcomponent ) {
		return "Movie #" + counter;
	}

	protected void updateImage() {
		currentCanvas.setImageUpdated();
		currentFile.draw();
		currentCanvas.repaint();
	}

	protected void updateFrameInfo() {
		String info = currentFile.getT() + "/" + currentFile.getNFrames();
		sliceSlider.setMinimum( 1 );
		sliceSlider.setMaximum( currentFile.getNFrames() );
		sliceSlider.setValue( currentFile.getT() );
		sliceSlider.setToolTipText( "Frame " + info );
		sliceLabel.setText( info );
	}

	protected void handleTabChange( int newTabIndex ) {
		if ( newTabIndex < canvases.size() ) {
			currentCanvas = canvases.get( newTabIndex );
			updateFrameInfo();
		}
	}

	protected void playMovie() {
		if ( moviePlaying ) {
			return;
		}
		stopButton.setEnabled( true );
		moviePlaying = true;
		// Create and stard a new thread that forwards the movie
		animationThread = new Thread( new Animator() );
		animationThread.start();
		stopButton.setText( "Pause" );
		sliceSlider.setEnabled( false );
	}

	protected void stopMovie() {
		sliceSlider.setEnabled( true );
		if (moviePlaying) {
			stopButton.setText( "Stop" );
			moviePlaying = false;
			try {
				animationThread.join();
			} catch (InterruptedException e) {
				// don't bother, we wanted to stop it anyway
			}
		} else {
			currentFile.setT( 1 );
			stopButton.setEnabled( false );
			stopButton.setText( "Pause" );
			updateImage();
			updateFrameInfo();
		}
	}

	protected boolean isAtLastFrame() {
		return currentFile.getT() == currentFile.getNFrames();
	}

	/**
	 * Move the current movie one frame back.
	 */
	protected void moveToPrevFrame() {
		setCurrentFrame( currentFile.getT() - 1 );
	}

	/**
	 * Move the current movie one frame forward.
	 */
	protected void moveToNextFrame() {
		setCurrentFrame( currentFile.getT() + 1 );
	}

	/**
	 * Sets the current movie to the given frame and
	 * update the image as well as the image info.
	 */
	protected void setCurrentFrame( int frame ) {
		currentFile.setT( frame );
		updateImage();
		updateFrameInfo();
	}
}
