package model;

import model.Condition;
import model.Experiment;

import java.util.Arrays;
import java.util.List;

/**
 * A project contains a set of experiments.
 */
public class Project {
	// Name of the project
	String name;
	// An optional base directory
	String baseDirectory;
	// All exclusive conditions wrt. the experiments
	List<Condition> exclusiveConditions;
	// All inclusive conditions wrt. the experiments
	List<Condition> inclusiveConditions;
	// The experiments
	List<Experiment> experiments;

	public Project() {

	}

	public Project(String name, List<Condition> exclConditions,
			List<Condition> inclConditions, List<Experiment> experiments) {
		this.name = name;
		this.exclusiveConditions = exclConditions;
		this.inclusiveConditions = inclConditions;
		this.experiments = experiments;
	}

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBaseDirectory() {
		return baseDirectory;
	}

	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public List<Condition> getExclusiveConditions() {
		return exclusiveConditions;
	}

	public void setExclusiveConditions(List<Condition> exclusiveConditions) {
		this.exclusiveConditions = exclusiveConditions;
	}

	public List<Condition> getInclusiveConditions() {
		return inclusiveConditions;
	}

	public void setInclusiveConditions(List<Condition> inclusiveConditions) {
		this.inclusiveConditions = inclusiveConditions;
	}

	@Override
	public String toString() {
		return "Project " + name + ":" + "\n\t" +
			"Exclusive conditions: " + Arrays.toString( exclusiveConditions.toArray() ) + "\n\t" +
			"Inclusive conditions: " + Arrays.toString( inclusiveConditions.toArray() ) + "\n\t" +
			"Experiments: " + Arrays.toString( experiments.toArray() );
	}
}
