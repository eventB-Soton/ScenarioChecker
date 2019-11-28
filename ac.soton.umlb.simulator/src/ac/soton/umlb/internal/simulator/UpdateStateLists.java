/*******************************************************************************
 *  Copyright (c) 2019-2019 University of Southampton.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *   
 *  Contributors:
 *  University of Southampton - Initial implementation
 *******************************************************************************/
package ac.soton.umlb.internal.simulator;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eventb.emf.core.machine.Machine;

import ac.soton.umlb.internal.simulator.views.SimulatorView;
import de.prob.core.domainobjects.Variable;

public class UpdateStateLists  {

	private UpdateStateLists(){}
	
	// Update the state list, taking into account filtering
	public static void execute(SimulatorView simulatorView, Clock clock, Map<String, Variable> stateMap) {
		//Group classGroup;
		Group timeGroup;
		//Group associationGroup;
		FormToolkit toolkit;
		
		// Do nothing if the simulator view is not active
		if (simulatorView == null) return;
		Machine machine = simulatorView.getMachine();
		if(machine == null) return;
		
		//classGroup = simulatorView.getClassGroup();
		//associationGroup = simulatorView.getAssociationGroup();
		timeGroup = simulatorView.getTimeGroup();
		
		Composite container = simulatorView.getContainer();
		// we do not really want to dispose of all of the class group
		// this is a temporary measure.
//		if (classGroup == null) return;
//		classGroup.dispose();
//		if (associationGroup != null) timeGroup.dispose();
		if (timeGroup != null) timeGroup.dispose();
		// create new groups and re-assign to the fields
		simulatorView.createNewGroups();
//		classGroup = simulatorView.getClassGroup();
		timeGroup = simulatorView.getTimeGroup();
//		associationGroup = simulatorView.getAssociationGroup();

		// Keep track of this machine's class data in this list
//		List<Class> classList = new ArrayList<Class>();
//		List<Association> associationList = new ArrayList<Association>();

		toolkit = new FormToolkit(Display.getCurrent());
		
		// add the current time
		String[] timeRow = { "clock = ", clock.getValue() };
		Table timeTable = new Table(timeGroup, SWT.BORDER
				| SWT.FULL_SELECTION);
		toolkit.adapt(timeTable);
		toolkit.paintBordersFor(timeTable);
		timeTable.setHeaderVisible(true);
		timeTable.setLinesVisible(true);
		int lastColumnIndex = timeRow.length;
		for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
			TableColumn col = new TableColumn(timeTable, SWT.NULL);
			col.setText(timeRow[loopIndex]);
		}
		// ... and pack
		for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
			timeTable.getColumn(loopIndex).pack();
		}

		timeGroup.pack();
		timeGroup.layout();

		//===================================
		simulatorView.updateStatusTable();
		//===================================
		
//		// go through each extension and process classes and associations
//		for (AbstractExtension ext : machine.getExtensions()) {
//			if (ext instanceof Classdiagram) {
//				Classdiagram classDiagram = (Classdiagram) ext;
//				classList.addAll(classDiagram.getClasses());
//				associationList.addAll(classDiagram.getAssociations());
//			}
//		}
		
//		for (Class class_ : classList) {
//			processClass(class_, stateMap, classGroup, toolkit); //, oracle);
//		}

//		for (Association ass : associationList) {
//			processAssociation(ass, stateMap, associationGroup, toolkit); //, oracle);
//		}
		
		// re-lay out the container
		container.layout();

	}

//	private static void processAssociation(Association association, Map<String, Variable> stateMap, Group associationGroup, FormToolkit toolkit) { //, OracleHandler oracle) {
//		Variable assocValueRaw = stateMap.get(association.getName());
//		if (assocValueRaw != null) {
//			Set<String> assocValueParsed = new StateResultStringParser(assocValueRaw.getValue()).parseToSet();
//
//			Table associationTable = new Table(associationGroup, SWT.BORDER
//					| SWT.FULL_SELECTION);
//			toolkit.adapt(associationTable);
//			toolkit.paintBordersFor(associationTable);
//			associationTable.setHeaderVisible(true);
//			associationTable.setLinesVisible(true);
//						
//			// set up the table, with a header
//			String[] title = { association.getName(), association.getSource().getName()+" - "+ association.getTarget().getName()}; //""};
//						
//			int lastColumnIndex = title.length;
//			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
//				TableColumn col = new TableColumn(associationTable, SWT.NULL);
//				col.setText(title[loopIndex]);
//			}
//			// iterate through the tableContent strings adding them as
//			// tableItems
//			for (String entry : assocValueParsed) { //Entry<String, String> entry : assocValueParsed.entrySet()) {		//String[] rowContent : tableContent) {
//				TableItem ti = new TableItem(associationTable, SWT.NULL);
//				String[] row = entry.split("\u21a6");// {entry.getKey(), entry.getValue()};
//				ti.setText(row);
//			}
//			// ... and pack
//			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
//				associationTable.getColumn(loopIndex).pack();
//			}
//			associationGroup.pack();
//		}
//	}

//	private static void processClass(Class class_, Map<String, Variable> stateMap, Group classGroup, FormToolkit toolkit) { //, OracleHandler oracle) {
//		// The following code creates a group of tables.
//		// we need to obtain the statemachines for this class, and report their current states
//		EList<EObject> statemachineList = class_.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true);
//		EList<ClassAttribute> attributeList = class_.getClassAttributes();
//
//		
//		String className = class_.getName();
//		Variable classVariable = stateMap.get(className);
//		
//		String[] instanceNameArray = {};
//		String instances = null;
//		if (classVariable==null) {
//			//look for instantiation in context
//			instances = findInstantiation(class_);
//		}else {
//			//variable instances
//			if (!"\u2205".equals(classVariable.getValue())) {
//				instances =  classVariable.getValue();
//			}
//		}
//		if (instances != null) {
//			instanceNameArray =  instances.replace("{","").split("\\W+"); //split to isolate the identifiers
//		}
//
//		List<String[]> tableContent = new ArrayList<String[]>();
//
//		// get the state-machine state
//		for (EObject eObject : statemachineList) {
//			if (eObject instanceof Statemachine){
//				Statemachine statemachine = (Statemachine)eObject;
//				if (TranslationKind.SINGLEVAR.equals(statemachine.getTranslation())){
//					Variable variable = stateMap.get(statemachine.getName());
//					if (variable != null) {
//						String value = variable.getValue();
//						// add to the list tableContent
//						String[] tempString = { statemachine.getName(), value };
//						tableContent.add(tempString);
//						
////						//record the statemachine state in the oracle
////						if (oracle != null ) oracle.addValueToSnapshot(statemachine.getName(), value, clock.getValue());
//
//					}
//				}else if(TranslationKind.MULTIVAR.equals(statemachine.getTranslation())){
//					EList<AbstractNode> smNodes = statemachine.getNodes();
//					for (AbstractNode node : smNodes) {
//						if (node instanceof State){
//							String currentStateName = ((State)node).getName();
//							// we have found a state-machine state
//							// now get the value from the stateMap
//							Variable variable = stateMap.get(currentStateName);
//							if (variable != null) {
//								String value = variable.getValue();
//								if (value.equals("TRUE")) {
//									// add to the list tableContent
//									String[] tempString = { statemachine.getName(), currentStateName };
//									tableContent.add(tempString);
////									//record the statemachine state in the oracle
////									if (oracle != null ) oracle.addValueToSnapshot(statemachine.getName(), currentStateName, clock.getValue());
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//
//		Table classesTable = new Table(classGroup, SWT.BORDER
//				| SWT.FULL_SELECTION);
//		toolkit.adapt(classesTable);
//		toolkit.paintBordersFor(classesTable);
//		classesTable.setHeaderVisible(true);
//		classesTable.setLinesVisible(true);
//		
//		//Titles and columns
//		List<String> titleList = new ArrayList<String>();
//		titleList.add(className);
//		titleList.add("State");
//		for (ClassAttribute ca : attributeList){
//			titleList.add(ca.getName());
//		}
//		for (String title : titleList){
//			TableColumn col = new TableColumn(classesTable, SWT.NULL);
//			col.setText(title);
//		}
//		
//		String[] rowContent = new String[titleList.size()];
//		
//		for (String instance : instanceNameArray) {
//			rowContent[0] = instance;
//			rowContent[1] = "";	//TODO this is the state-machine state - should be many
//			int r=2;
//			for (ClassAttribute ca : attributeList) {
//				rowContent[r] = getAttributeMapping(stateMap.get(ca.getName()),instance);
//			}
//
//			TableItem ti = new TableItem(classesTable, SWT.NULL);
//			ti.setText(rowContent);
//		}
//		
//		// ... and pack
//		for (TableColumn column : classesTable.getColumns()){
//			column.pack();
//		}
//		classGroup.pack();
//	}  //end of processClass

	
//	private static String findInstantiation(Class class_) {
//		if (class_.getInstances()!= null && class_.getInstances().startsWith("{")) {
//			return class_.getInstances();
//		}
//		for (EventBSuperType sc : class_.getSupertypes()) {
//			String i = findInstantiation(sc.toSuperClass());
//			if (i!=null && i.startsWith("{")) return i;
//		}
//		return null;
//	}
//
//	private static String getAttributeMapping(Variable atVar, String instance) {
//		if (atVar == null || instance ==null) return "";
//		Set<String> attribValueParsed = new StateResultStringParser(atVar.getValue()).parseToSet();
//		for (String maplet : attribValueParsed) {
//			String[] pair = maplet.split("\u21a6");
//			if (instance.equals(pair[0])) {
//				return pair[1];
//			}
//		}
//		return "";
//	}

}
