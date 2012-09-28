package ui;

import ij.ImagePlus;
import ij.plugin.filter.Info;

import java.awt.Component;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import loci.plugins.LociImporter;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import model.View;
import tools.Helpers;

/**
 * A panel to view the meta data of of files.
 */
public class MetadataViewPanel extends ViewPanel<ImagePlus> {

	public MetadataViewPanel( View view ) {
		super( view, "Meta data" );
	}

	@Override
	protected ImagePlus loadData( String path ) {
		// use LOCI BioFormats to read in meta data
		try {
			ImporterOptions options = new ImporterOptions();
			options.setId( path );
			options.setSplitChannels( false );
			options.setWindowless( true );
			options.setVirtual( true );
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
		/* we are dealing with ImagePlus instances, so
		 * just use the image info.
		 */
		Info imgInfo = new Info();
		String info = imgInfo.getImageInfo( data, data.getChannelProcessor() );
		String htmlInfo = "<html>" + info.replace( "\n", "<br>" ) + "</html>";
		JLabel label = new JLabel( htmlInfo );
		return Arrays.asList( (Component) label );
	}

	@Override
	protected String getTabText( int counter, int subcomponent ) {
		return "File #" + counter;
	}
}
