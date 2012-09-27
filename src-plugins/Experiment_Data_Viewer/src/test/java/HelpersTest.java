import org.junit.Assert;
import org.junit.Test;

import tools.Helpers;

public class HelpersTest {

	@Test
	public void joinPath() {
		String testPath1 = Helpers.joinPath( "", "" );
		Assert.assertEquals( "", testPath1 );
		String testPath2 = Helpers.joinPath( "a", "" );
		Assert.assertEquals( "a", testPath2 );
		String testPath3 = Helpers.joinPath( "", "b" );
		Assert.assertEquals( "b", testPath3 );
		String testPath4 = Helpers.joinPath( "a", "b" );
		Assert.assertEquals( "a/b", testPath4 );
	}

	@Test
	public void randomString() {
		int length = 6;
		String ranString1 = Helpers.randomAlphaNumericString( length );
		Assert.assertEquals( length, ranString1.length() );
	}
}
