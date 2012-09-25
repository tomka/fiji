package control;

import java.util.ArrayList;
import java.util.List;

import model.Experiment;
import model.Project;
import tools.Helpers;

import ui.UI;
import ui.SelectionGUI;
import ui.DataGUI;

public class Controler {
	// the model to control
	Project project;
	// A list of currently active UIs
	List<UI> uis;

	public Controler( Project project ) {
		this.project = project;
		this.uis = new ArrayList<UI>();
	}

	public void showSelectionDialog() {
		UI ui = new SelectionGUI(this);
		uis.add( ui );
		ui.show();
	}

	public void showDataDisplay(Experiment experiment) {
		UI ui = new DataGUI( experiment );
		uis.add( ui );
		ui.show();
	}

	public void showOmeroExportDialog() {

	}

	/**
	 * Will close all open UIs before exiting the plugin.
	 */
	public void exit() {
		// kill all available UIs
		for (UI ui : uis) {
			ui.close();
		Helpers.log("Good bye");
		}
	}

	public Project getProject() {
		return project;
	}
}
