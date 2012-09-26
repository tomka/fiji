import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import model.View;

import org.junit.Assert;
import org.junit.Test;

public class ViewTest {

	@Test
	public void EmptyViewConsistensyTest() {
		String name = "TestName";
		List<String> paths = new ArrayList<String>();
		Map<String,Object> settings = new HashMap<String,Object>();
		View v = new View( name, paths, settings);

		Assert.assertEquals( View.ViewType.UNKNOWN, v.getType() );
		Assert.assertEquals( 0, v.getPaths().size() );
	}

	@Test
	public void ViewConsistensyTest() {
		String name = "TestName";
		List<String> paths = new ArrayList<String>();
		String p1 = "a";
		String p2 = "b";
		String p3 = "c";
		paths.add( p1 );
		paths.add( p2 );
		paths.add( p3 );
		Map<String,Object> settings = new HashMap<String,Object>();
		View v = new View( name, paths, settings);

		Assert.assertEquals( View.ViewType.UNKNOWN, v.getType() );
		Assert.assertEquals( paths.size(), v.getPaths().size() );
		for (int i=0; i<v.getPaths().size(); i++) {
			Assert.assertEquals( paths.get(i), v.getPaths().get(i) );
		}
	}

	@Test
	public void fileExtensionMapping() {
		Assert.assertEquals( View.ViewType.MOVIE,
			View.getTypeForExtension( "avi" ) );
		Assert.assertEquals( View.ViewType.METADATA,
			View.getTypeForExtension( "lsm" ) );
		Assert.assertEquals( View.ViewType.TABLE,
			View.getTypeForExtension( "xls" ) );
		Assert.assertEquals( View.ViewType.FIGURE,
			View.getTypeForExtension( "fig" ) );
		Assert.assertEquals( View.ViewType.FIGURE,
			View.getTypeForExtension( "pdf" ) );
	}
}
