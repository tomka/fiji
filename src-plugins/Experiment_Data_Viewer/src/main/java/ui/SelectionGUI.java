package ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import control.Controler;
import model.Experiment;
import model.Condition;
import model.Project;
import tools.Helpers;

public class SelectionGUI extends UI {
	// know your controler
	Controler controler;
	// the model we are working with
	Project project;
	// our window frame
	JFrame frame;
	// a panel which contains the selection controls
	JPanel selectionPanel;
	// a list where all experiments get shown in
	JList experimentList;
	// the model for the experiment list
	DefaultListModel experimentModel;
	// a button to confirm experiment selection
	JButton continueButton;
	/* a map from GUI components to list names for
	 * exclusive conditions
	 */
	Map<JList, String> exclusiveLists;
	/* a map from GUI components to list names for
	 * inclusive conditions
	 */
	Map<JList, String> inclusiveLists;
	// listeners for exclusive and inclusive lists
	ListSelectionListener exclusiveValueChanged;
	ListSelectionListener inclusiveValueChanged;
	ListSelectionListener selectExperiment;
	// currently active exclusive condition
	Condition exclusiveCondition;
	// currently active inclusive conditions
	List<Condition> inclusiveConditions;

	public SelectionGUI( Controler controler ) {
		this.controler = controler;
		this.project = controler.getProject();
		this.exclusiveLists = new HashMap<JList,String>();
		this.inclusiveLists = new HashMap<JList,String>();
		init();
	}

	protected void initListListeners() {
		exclusiveValueChanged =
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent event) {
						// react only to the final selection event
        				if (event.getValueIsAdjusting()) {
							return;
						}
						// remove all event handlers
						for (JList l : exclusiveLists.keySet()) {
							l.removeListSelectionListener( exclusiveValueChanged );
						}
						// get current selection and update lists
						JList srcList = (JList) event.getSource();
						String option =  (String) srcList.getSelectedValues()[0];
						// clear all other exclusive list selections
						for (JList l : exclusiveLists.keySet()) {
							if (l != srcList)
								l.clearSelection();
						}
						// add all event handlers again
						for (JList l : exclusiveLists.keySet()) {
							l.addListSelectionListener( exclusiveValueChanged );
						}
						// create condition for selection
						exclusiveCondition = new Condition( exclusiveLists.get(srcList), option);
						// try to find valid experiments
						showValidExperiments();
          			}
				};
		inclusiveValueChanged =
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent event) {
						// react only to the final selection event
        				if (event.getValueIsAdjusting()) {
							return;
						}
						// walk over all inclusive lists and get conditions
						inclusiveConditions = new ArrayList<Condition>();
						for (JList l : inclusiveLists.keySet()) {
							if (l.getSelectedValues().length > 0) {
								// get current selection and update lists
								String option =  (String) l.getSelectedValues()[0];
								Condition c = new Condition( inclusiveLists.get(l), option );
								inclusiveConditions.add( c );
							}
						}
						// try to find valid experiments
						showValidExperiments();
          			}
				};
		selectExperiment =
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent event) {
						// react only to the final selection event
        				if (event.getValueIsAdjusting()) {
							return;
						}
						// test whether there is actually something selected
						JList srcList = (JList) event.getSource();
						if (srcList.getSelectedValues().length == 0) {
							return;
						}
						continueButton.setEnabled( true );
						Helpers.logVerbose( "Selected Experiment: "
							+ (String) srcList.getSelectedValues()[0] );
          			}
				};
	}

	protected void init() {
		initListListeners();
		frame = new JFrame( "Data display for " + project.getName() );
		frame.setLayout( new BorderLayout() );
		selectionPanel = new JPanel( new BorderLayout() );
		int numLists = project.getExclusiveConditions().size() +
			project.getInclusiveConditions().size() + 1;
		JPanel conditionsPanel = new JPanel( new GridLayout( 0, numLists ) );
		// add a JList for each exclusive condition
		for (Condition c : project.getExclusiveConditions()) {
			JPanel panel = new JPanel( new BorderLayout() );
			panel.add( new JLabel( c.getName() ), BorderLayout.NORTH );
			JList optionList = new JList( c.getOptions().toArray() );
			optionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			exclusiveLists.put( optionList, c.getName() );
			// add a listener
			optionList.addListSelectionListener( exclusiveValueChanged );
			// put the JList into a scrollpane
			JScrollPane scrollPane = new JScrollPane( optionList );
			panel.add( scrollPane, BorderLayout.CENTER );
			conditionsPanel.add( panel );
		}
		// add a JList for each inclusive condition
		for (Condition c : project.getInclusiveConditions()) {
			JPanel panel = new JPanel( new BorderLayout() );
			panel.add( new JLabel( c.getName() ), BorderLayout.NORTH );
			JList optionList = new JList( c.getOptions().toArray() );
			optionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			inclusiveLists.put( optionList, c.getName() );
			// add a listener
			optionList.addListSelectionListener( inclusiveValueChanged );
			// put the JList into a scrollpane
			JScrollPane scrollPane = new JScrollPane( optionList );
			panel.add( scrollPane, BorderLayout.CENTER );
			conditionsPanel.add( panel );
		}
		// add experiment list box
		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( new JLabel( "Experiments" ), BorderLayout.NORTH );
		experimentModel = new DefaultListModel();
		experimentList = new JList( experimentModel );
		experimentList.addListSelectionListener( selectExperiment );
		// put the JList into a scrollpane
		JScrollPane scrollPane = new JScrollPane( experimentList );
		panel.add( scrollPane, BorderLayout.CENTER );
		conditionsPanel.add( panel );
		// add continue button
		continueButton = new JButton( "Show data" );
		continueButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					showData();
  				}
			}
		);
		continueButton.setEnabled( false );
		selectionPanel.add( new JLabel( "Please select a combination" ), BorderLayout.NORTH );
		selectionPanel.add( conditionsPanel, BorderLayout.CENTER );
		// button panel
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					controler.exit();
  				}
			}
		);
		JButton omeroButton = new JButton("Export to OMERO");
		omeroButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					controler.showOmeroExportDialog();
  				}
			}
		);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(closeButton);
		buttonPanel.add(omeroButton);
		buttonPanel.add(continueButton);
		selectionPanel.add( buttonPanel, BorderLayout.SOUTH );
		frame.add( selectionPanel, BorderLayout.CENTER );
		frame.pack();
		frame.setSize(400,300);
		frame.setVisible( false );
	}

	public void showData() {
		// make sure we have a selected experiment
		if (experimentList.getSelectedValues().length == 0) {
			return;
		}
		// find actual experiment
		String selectedExperiment =
			(String) experimentList.getSelectedValues()[0];
		Experiment experiment = null;
		for (Experiment e : project.getExperiments()) {
			if (e.getName().equals( selectedExperiment )) {
				experiment = e;
				break;
			}
		}
		// make sure we've got an experiment
		if (experiment == null) {
			Helpers.log("Could not find a valid experiment with name " + selectedExperiment);
			return;
		}
		// go on, display the data
		Helpers.log("Showing data for experiment " + experiment.getName());
		controler.showDataDisplay( experiment );
	}

	public void showValidExperiments() {
		experimentModel.clear();
		continueButton.setEnabled( false );
		for (Experiment e : project.getExperiments()) {
			// check for matching experiments
			boolean exclusiveMatches =
				exclusiveCondition == null? true : e.matches( exclusiveCondition );
			boolean inclusiveMatches =
				inclusiveConditions == null? true : e.matches( inclusiveConditions );
			// only add experiment if it matches conditions
			if (exclusiveMatches && inclusiveMatches) {
				experimentModel.addElement(e.getName());
			}
		}
	}

	@Override
	public void close() {
		frame.setVisible( false );
		frame.dispose();
	}

	@Override
	public void show() {
		frame.setVisible( true );
	}
}
