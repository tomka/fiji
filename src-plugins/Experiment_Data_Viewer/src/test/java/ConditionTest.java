import java.util.ArrayList;
import java.util.List;

import model.Condition;

import org.junit.Assert;
import org.junit.Test;

public class ConditionTest {

	@Test
	public void EmptyConditionConsistensyTest() {
		String testName = "TestName";
		Condition c = new Condition( testName );
		Assert.assertEquals( testName, c.getName() );
		Assert.assertEquals( 0, c.getOptions().size() );
	}

	@Test
	public void SingleOptionConditionConsistensyTest() {
		String testName = "TestName";
		String testOption = "TestOption";
		Condition c = new Condition( testName, testOption );
		Assert.assertEquals( testName, c.getName() );
		Assert.assertEquals( 1, c.getOptions().size() );
		Assert.assertEquals( testOption, c.getOptions().get(0) );
	}

	@Test
	public void MultiOptionConditionConsistensyTest() {
		String testName = "TestName";
		List<String> options = new ArrayList<String>();
		options.add("a");
		options.add("b");
		options.add("c");
		Condition c = new Condition( testName, options );
		Assert.assertEquals( testName, c.getName() );
		Assert.assertEquals( 3, c.getOptions().size() );

		for (int i=0; i<options.size(); i++) {
			Assert.assertEquals( options.get(i), c.getOptions().get(i) );
		}
	}

	@Test
	public void SingleOptionConditionMatchingTest() {
		String testOption = "TestOption";
		Condition c1 = new Condition( "Test", testOption );
		Condition c2 = new Condition( "Test", testOption );
		Condition c3 = new Condition( "Test2", testOption );
		// only conditions with the same name should match
		Assert.assertTrue( c1.matches( c2 ) );
		Assert.assertTrue( c2.matches( c1 ) );
		Assert.assertFalse( c1.matches( c3 ) );
		Assert.assertFalse( c3.matches( c2 ) );
	}

	@Test
	public void MultiOptionConditionMatchingTest() {
		List<String> options1 = new ArrayList<String>();
		options1.add("a");
		options1.add("b");
		options1.add("c");
		List<String> options2 = new ArrayList<String>();
		options2.add("a");
		options2.add("b");
		List<String> options3 = new ArrayList<String>();
		options3.add("d");

		/* Conditions match only when the second one contains
		 * the same options as the first one or a sub-set of it.
		 */

		Condition c1 = new Condition( "Test", options1 );
		Condition c2 = new Condition( "Test", options2 );
		Assert.assertTrue( c1.matches( c2 ) );
		Assert.assertFalse( c2.matches( c1 ) );

		Condition c3 = new Condition( "Test", options1 );
		Assert.assertTrue( c1.matches( c3 ) );
		Assert.assertTrue( c3.matches( c1 ) );

		Condition c4 = new Condition( "Test", options3 );
		Assert.assertFalse( c1.matches( c4 ) );
		Assert.assertFalse( c4.matches( c1 ) );
	}
}
