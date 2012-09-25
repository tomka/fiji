import model.Project;

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
public abstract class Experiment_Data_Viewer {
	// the storage key for Fiji preferences
	protected final static String PREF_KEY = "ExperimentDataViewer.";

	protected void displayExperimentSelector(Project p) {

	}
}
