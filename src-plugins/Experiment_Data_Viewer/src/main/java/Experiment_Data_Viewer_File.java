import ij.IJ;
import ij.Prefs;
import ij.plugin.PlugIn;

import fiji.util.gui.GenericDialogPlus;

import control.Controler;
import io.FileInput;
import model.Project;
import tools.Helpers;

/**
   Copyright 2012 Tom Kazimiers and the Fiji project. Fiji is just
   imageJ - batteries included.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see http://www.gnu.org/licenses/ .
 */

/**
 * A plugin which provides convenient access to a set of experiments.
 * It displays different files belonging to one experiment in a single
 * user interface. The set of experiments can currently be defined in a
 * file or retrieved from an OMERO server.
 *
 */
public class Experiment_Data_Viewer_File extends Experiment_Data_Viewer implements PlugIn {
	// Path of project file
	String projectFilePath;
	// A project, containing all the experiments
	Project project;

	public void run(String arg0) {
		if (showDialog()) {
			Helpers.logVerbose("Loading project from file");
			project = FileInput.read( projectFilePath );
			// make sure we got a project
			if (project == null) {
				IJ.error("Could not read file");
				return;
			} else {
				Helpers.logVerbose("Showing selection GUI");
				Controler ctrl = new Controler( project );
				Helpers.log( project.toString() );
				ctrl.showSelectionDialog();
			}
		}
	}

	public boolean showDialog() {
		/* create a new generic dialog for the
		 * display of various options.
		 */
		final GenericDialogPlus gd
			= new GenericDialogPlus("Experiment data viewer (file based)");

		// set up the users preferences
		projectFilePath = Prefs.get(PREF_KEY+"descriptionFile", "");

		// add user-interface elements
		gd.addMessage("Please, select a dataset descripton file, please.");
		gd.addFileField("Description file", projectFilePath);

		// show the dialog, finally
		gd.showDialog();
		// do nothing if dialog has been canceled
		if (gd.wasCanceled())
			return false;

		// read out GUI data
		projectFilePath = gd.getNextString();

		// save user preferences
		Prefs.set(PREF_KEY+"descriptionFile", projectFilePath);

		return true;
	}
}
