import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import model.Condition;
import model.Experiment;
import model.View;

import org.junit.Assert;
import org.junit.Test;

public class ExperimentTest {

	@Test
	public void EmptyExperimentConsistensyTest() {
		String name = "TestName";
		List<Condition> conditions = new ArrayList<Condition>();
		List<View> views = new ArrayList<View>();

		Experiment e = new Experiment( name, conditions, views );
		Assert.assertEquals( name, e.getName() );
		Assert.assertEquals( 0, e.getConditions().size() );
		Assert.assertEquals( 0, e.getViews().size() );
	}

	/**
	 * Generate a small Example instance, with the following
	 * properties:
	 *
	 * name: TestName
	 * conditions:
	 *   condition1: []
	 *   condition2: []
	 * views:
	 *   test:
	 *	   element
	 */
	public Experiment generateSampleExperiment() {
		String name = "TestName";
		List<Condition> conditions = new ArrayList<Condition>();
		conditions.add( new Condition( "condition1" ) );
		conditions.add( new Condition( "condition2" ) );

		List<View> views = new ArrayList<View>();
		Map<String,Object> viewSettings =
			new HashMap<String,Object>();
		viewSettings.put( "test", "element" );
		views.add( new View( "view1", new ArrayList<String>(),
			viewSettings ) );

		return new Experiment( name, conditions, views );
	}

	@Test
	public void ExperimentConsistensyTest() {
		Experiment e = generateSampleExperiment();
		Assert.assertEquals( "TestName", e.getName() );
		Assert.assertEquals( 2, e.getConditions().size() );
		Assert.assertEquals( 1, e.getViews().size() );
	}

	@Test
	public void construction() {
		Experiment e1 = new Experiment( null, null, null );
		Assert.assertNotNull( e1.getName() );
		Assert.assertNotNull( e1.getViews() );
		Assert.assertNotNull( e1.getConditions() );

		Experiment e2 = new Experiment( null, null, null, null );
		Assert.assertNotNull( e2.getName() );
		Assert.assertNotNull( e2.getViews() );
		Assert.assertNotNull( e2.getConditions() );
	}

	@Test
	public void setName() {
		Experiment e = generateSampleExperiment();
		e.setName( null );
		Assert.assertEquals( "", e.getName() );
		String name = "e8/7-trG2 @2i";
		e.setName( name );
		Assert.assertEquals( name, e.getName() );
	}

	@Test
	public void ExperimentMatchingTest() {
		String name = "TestName";
		// create conditions
		List<String> options1 = new ArrayList<String>();
		options1.add( "a" );
		options1.add( "b" );
		List<String> options2 = new ArrayList<String>();
		options2.add( "c" );
		List<String> options3 = new ArrayList<String>();
		options3.add( "a" );
		options3.add( "c" );
		// Conditions with options {a,b} and {c}
		List<Condition> conditions1 = new ArrayList<Condition>();
		conditions1.add( new Condition( "condition", options1 ) );
		conditions1.add( new Condition( "condition", options2 ) );
		// Condition with options {a,c}
		List<Condition> conditions2 = new ArrayList<Condition>();
		conditions2.add( new Condition( "condition", options3 ) );
		// create views
		List<View> views = new ArrayList<View>();
		Map<String,Object> viewSettings =
			new HashMap<String,Object>();
		viewSettings.put( "test", "element" );
		views.add( new View( "view1", new ArrayList<String>(),
			viewSettings ) );
		// create experiments
		Experiment e1 = new Experiment( name, conditions1, views );
		Experiment e2 = new Experiment( name, conditions2, views );
		/* e1 should not match e2, because e1 has conditions that e2
		 * can't meet. e2 should't match e1 as well as it's
		 * conditions form only a sub-set of e1's.
		 */
		Assert.assertFalse( e1.matches( e2.getConditions() ) );
		Assert.assertFalse( e2.matches( e1.getConditions() ) );

		// Condition with options {c}
		List<Condition> conditions3 = new ArrayList<Condition>();
		conditions3.add( new Condition( "condition", options2 ) );
		Experiment e3 = new Experiment( name, conditions3, views );
		/* e2 should match e3, bacause the options of e2's condition
		 * form a super set of e3's.
		 */
		Assert.assertTrue( e2.matches( e3.getConditions() ) );
		Assert.assertFalse( e3.matches( e2.getConditions() ) );
	}
}
