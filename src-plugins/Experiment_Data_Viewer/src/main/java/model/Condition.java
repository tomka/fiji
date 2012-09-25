package model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * A named experiment condition with a list of options. This could
 * for instance be a wildtype experiment with a set of markers:
 *
 * Condition("Wildtype", ["SpiderGFP", "EcadGFP", "SASVenus"])
 *
 */
public class Condition implements Iterable<String> {
	// Name of the condition
	String name;
	// Options of the condition
	List<String> options;

	/**
	 * Creates a new named Condition without any options.
	 */
	public Condition(String name) {
		this.name = name;
		this.options = new ArrayList<String>();
	}

	/**
	 * Creates a new named Condition with a set of options.
	 */
	public Condition(String name, List<String> options) {
		this.name = name;
		this.options = options;
	}

	/**
	 * Creates a new named Condition with one option.
	 */
	public Condition(String name, String option) {
		this.name = name;
		this.options = new ArrayList<String>(1);
		this.options.add( option );
	}

	/**
	 * Checks whether this condition matches another one.
	 */
	public boolean matches(Condition other) {
		// Conditions are not the same if their names differ
		if (!this.name.equals(other.name)) {
			return false;
		}

		// Also, the options of the other condition need to be present
		for(String o : other.getOptions()) {
			if (!this.options.contains(o)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Return the name of the experiment.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Provides accsss to the options of the experiment.
	 */
	public Iterator<String> iterator() {
		return options.iterator();
	}

	public List<String> getOptions() {
		return options;
	}

	@Override
	public String toString() {
		return "Condition " + name + " with options: " + Arrays.toString(options.toArray());
	}
}
