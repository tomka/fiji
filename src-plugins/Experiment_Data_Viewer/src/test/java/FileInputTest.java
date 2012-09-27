import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import io.FileInput;
import model.Condition;
import model.Experiment;
import model.Project;
import model.View;
import tools.Helpers;

public class FileInputTest {
	/* tests can create temporary directories. This list
	 * is looked at at the end of each test and cleaned up
	 * if necessary.
	 */
	protected List<File> createdDirectories = new ArrayList<File>();

	@Test
	public void ParseSimpleFile() {
		InputStream is = getClass().getResourceAsStream( "/simple-project.yaml" );
		Project p = FileInput.read( is );
		Assert.assertNotNull( p );
		Assert.assertEquals( "TestProject", p.getName() );
		Assert.assertEquals( "/path/to/test/data", p.getBaseDirectory() );

		// test exclusive conditions
		Assert.assertEquals( 2, p.getExclusiveConditions().size() );
		Condition ec1 = p.getExclusiveConditions().get(0);
		Assert.assertEquals( "Filter1", ec1.getName() );
		Assert.assertEquals( 3, ec1.getOptions().size() );
		Assert.assertEquals( "a", ec1.getOptions().get(0) );
		Assert.assertEquals( "b", ec1.getOptions().get(1) );
		Assert.assertEquals( "c", ec1.getOptions().get(2) );
		Condition ec2 = p.getExclusiveConditions().get(1);
		Assert.assertEquals( "Filter1A", ec2.getName() );
		Assert.assertEquals( 3, ec2.getOptions().size() );
		Assert.assertEquals( "a2", ec2.getOptions().get(0) );
		Assert.assertEquals( "b2", ec2.getOptions().get(1) );
		Assert.assertEquals( "c2", ec2.getOptions().get(2) );

		// test inclusive conditions
		Assert.assertEquals( 2, p.getInclusiveConditions().size() );
		Condition ic1 = p.getInclusiveConditions().get(0);
		Assert.assertEquals( "Filter2", ic1.getName() );
		Assert.assertEquals( 2, ic1.getOptions().size() );
		Assert.assertEquals( "d", ic1.getOptions().get(0) );
		Assert.assertEquals( "e", ic1.getOptions().get(1) );
		Condition ic2 = p.getInclusiveConditions().get(1);
		Assert.assertEquals( "Filter3", ic2.getName() );
		Assert.assertEquals( 3, ic2.getOptions().size() );
		Assert.assertEquals( "f", ic2.getOptions().get(0) );
		Assert.assertEquals( "g", ic2.getOptions().get(1) );
		Assert.assertEquals( "h", ic2.getOptions().get(2) );

		// test experiment
		Assert.assertNotNull( p.getExperiments() );
		Assert.assertEquals( 1, p.getExperiments().size() );
		Experiment e = p.getExperiments().get(0);
		Assert.assertEquals( "ExperimentOne", e.getName() );
		// test experiment conditions
		Assert.assertEquals( 1, e.getConditions().size() );
		Condition c = e.getConditions().get(0);
		Assert.assertEquals( "Filter1", c.getName() );
		Assert.assertEquals( 1, c.getOptions().size() );
		Assert.assertEquals( "a", c.getOptions().get(0) );
		// test experiment views
		Assert.assertNotNull( e.getViews() );
		Assert.assertEquals( 2, e.getViews().size() );
		View v1 = e.getViews().get(0);
		Assert.assertEquals( View.ViewType.MOVIE, v1.getType() );
		Assert.assertNotNull( v1.getPaths() );
		Assert.assertEquals( 2, v1.getPaths().size() );
		Assert.assertEquals( "/path/to/test/data/experimentOneData1", v1.getPaths().get(0) );
		Assert.assertEquals( "/path/to/test/data/experimentOneData2", v1.getPaths().get(1) );
		View v2 = e.getViews().get(1);
		Assert.assertEquals( View.ViewType.METADATA, v2.getType() );
		Assert.assertNotNull( v2.getPaths() );
		Assert.assertEquals( 2, v2.getPaths().size() );
		Assert.assertEquals( "/path/to/test/data/experimentOneData3", v2.getPaths().get(0) );
		Assert.assertEquals( "/path/to/test/data/experimentOneData4", v2.getPaths().get(1) );
	}

	/**
	 * Generates a sample view data structure as it comes out of
	 * the snakeyaml YAML parser. It is structured like this:
	 *
	 * movie:
	 *   files:
	 *     - a
	 *     - b
	 *     - c
	 * metadata:
	 *   files:
	 *     - d
	 *     - e
	 *     - f
	 */
	public Map<String,Map<String,Object>> generateSampleExperimentViewData() {
		// create a first set of files
		List<String> files1 = Arrays.asList( "a", "b", "c" );
		Map<String,Object> fileData1 =
			new HashMap<String, Object>();
		fileData1.put( "files", files1 );
		// create a second set of files
		List<String> files2 = Arrays.asList( "d", "e", "f" );
		Map<String,Object> fileData2 =
			new HashMap<String,Object>();
		fileData2.put( "files", files2 );
		// combine both in one view structure
		Map<String,Map<String,Object>> viewData =
			new HashMap<String,Map<String,Object>>();
		viewData.put( "movie", fileData1 );
		viewData.put( "metadata", fileData2 );

		return viewData;
	}

	@Test
	public void experimentViewParsing() {
		// create a test view structure
		Map<String,Map<String,Object>> viewData =
			generateSampleExperimentViewData();
		// have an empty view settings structure available
		Map<String,Map<String,Object>> viewSettings =
			new HashMap<String,Map<String,Object>>();

		// parse the views
		List<View> views = FileInput.parseExperimentViews(
			viewData, viewSettings, "" );
		// test the generated set of views
		Assert.assertEquals( 2, views.size() );
		View v1 = views.get(0);
		Assert.assertEquals( v1.getType(), View.ViewType.MOVIE );
		Assert.assertEquals( 3, v1.getPaths().size() );
		Assert.assertEquals( "a", v1.getPaths().get(0) );
		Assert.assertEquals( "b", v1.getPaths().get(1) );
		Assert.assertEquals( "c", v1.getPaths().get(2) );
		View v2 = views.get(1);
		Assert.assertEquals( v2.getType(), View.ViewType.METADATA );
		Assert.assertEquals( 3, v2.getPaths().size() );
		Assert.assertEquals( "d", v2.getPaths().get(0) );
		Assert.assertEquals( "e", v2.getPaths().get(1) );
		Assert.assertEquals( "f", v2.getPaths().get(2) );
	}

	@Test
	public void directoryParsing() throws IOException {
		// Create a single directory
		File temp = File.createTempFile( "edv-test-", "");
		temp.delete();
		temp.mkdir();
		createdDirectories.add( temp );

		// test directory parsing with empty directory
		String name = "TestName";
		Map<String,String> namedPaths;
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 0, namedPaths.keySet().size() );

		// create a file in that directory
		String fileName = "file1.avi";
		File temp2 = new File( temp.getPath(), fileName );
		temp2.createNewFile();
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 1, namedPaths.keySet().size() );
		String pathFound = (String) namedPaths.keySet().toArray()[0];
		Assert.assertEquals( temp.getPath(), pathFound );
		Assert.assertEquals( name, namedPaths.get( pathFound ) );

		// create another file in that directory
		fileName = "file2.pdf";
		File temp3 = new File( temp.getPath(), fileName );
		temp3.createNewFile();
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 1, namedPaths.keySet().size() );
		pathFound = (String) namedPaths.keySet().toArray()[0];
		Assert.assertEquals( temp.getPath(), pathFound );
		Assert.assertEquals( name, namedPaths.get( pathFound ) );

		// have a mapping from path to sub-dir ready
		HashMap<String,String> subDirMapping =
			new HashMap<String,String>();

		// create a sub-folder
		String subName = "sub1";
		File temp4 = new File( temp.getPath(), subName );
		temp4.mkdir();
		subDirMapping.put( temp4.getPath(), subName );
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 0, namedPaths.keySet().size() );

		// create a new file within the sub-directory
		fileName = "file1.avi";
		File temp5 = new File( temp4.getPath(), fileName );
		temp5.createNewFile();
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 1, namedPaths.keySet().size() );
		pathFound = (String) namedPaths.keySet().toArray()[0];
		Assert.assertEquals( temp4.getPath(), pathFound );
		Assert.assertEquals( subName, namedPaths.get( pathFound ) );

		// create another file in the sub-directory
		fileName = "file2.pdf";
		File temp6 = new File( temp4.getPath(), fileName );
		temp6.createNewFile();
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 1, namedPaths.keySet().size() );
		pathFound = (String) namedPaths.keySet().toArray()[0];
		Assert.assertEquals( temp4.getPath(), pathFound );
		Assert.assertEquals( subName, namedPaths.get( pathFound ) );

		// create another sub-folder
		String sub2Name = "sub2";
		File temp7 = new File( temp.getPath(), sub2Name );
		temp7.mkdir();
		subDirMapping.put( temp7.getPath(), sub2Name );
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 1, namedPaths.keySet().size() );
		pathFound = (String) namedPaths.keySet().toArray()[0];
		Assert.assertEquals( temp4.getPath(), pathFound );
		Assert.assertEquals( subName, namedPaths.get( pathFound ) );

		// create a new file in the second sub-folder
		fileName = "file3.pdf";
		File temp8 = new File( temp7.getPath(), fileName );
		temp8.createNewFile();
		namedPaths = FileInput.parseExperimentDirectory(
			temp.getPath(), name );
		Assert.assertNotNull( namedPaths );
		Assert.assertEquals( 2, namedPaths.keySet().size() );
		// test independently of order in set
		List<String> tested = new ArrayList<String>();
		for ( String foundPath : namedPaths.keySet() ) {
			// don't expect any path to be present twice
			Assert.assertFalse( tested.contains( foundPath ) );
			tested.add( foundPath );
			Assert.assertEquals( subDirMapping.get( foundPath ),
				new File( foundPath ).getName() );
		}
		Assert.assertEquals( 2, tested.size() );
		Assert.assertTrue( tested.contains ( temp4.getPath() ) );
		Assert.assertTrue( tested.contains ( temp7.getPath() ) );
	}

	@After
	public void cleanup() {
		// remove all temporary directories
		for ( File f : createdDirectories ) {
			f.delete();
		}
	}
}
