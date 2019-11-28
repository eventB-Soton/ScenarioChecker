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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.classdiagrams.Class;
import ac.soton.eventb.classdiagrams.Classdiagram;
import ac.soton.umlb.internal.simulator.views.SimulatorView;
import de.prob.core.Animator;
import de.prob.core.command.GetCurrentStateIdCommand;
import de.prob.core.command.GetEnabledOperationsCommand;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.exceptions.ProBException;


public class UpdateEnabledOpsList {
	
	private UpdateEnabledOpsList(){ //prevent instantiation
	}
	
	/**
	 * 
	 */
	public static void execute(SimulatorView simulatorView){
		// return if there is no simulator view
		if(simulatorView == null){
			return;
		}

		Machine mch = simulatorView.getMachine();
		if(mch == null) return;

		List<Class> classList = new ArrayList<Class>();
		EList<AbstractExtension> exts = simulatorView.getMachine().getExtensions();
		// go through each extension and process components
		for (AbstractExtension ext : exts) {
			if (ext instanceof Classdiagram) {
				Classdiagram rootComponent = (Classdiagram) ext;
				classList.addAll(rootComponent.getClasses());
			}
		}
		Animator animator = Animator.getAnimator();
		State currentState = animator.getCurrentState();
		List<Operation> enabledOps = currentState.getEnabledOperations();
		Table methodsTable = simulatorView.getMethodsTable();
		if (methodsTable == null) return;
		methodsTable.removeAll();
		
//		String className = new String("");
//		
		// for each enabled operation in the ProB model
		int select = -1;
		int j=0;
		for(Operation op: enabledOps){
//			for(Class class_: classList){		//CURRENTLY NOT ADDING CLASS NAME TO TABLE
//				for(ClassMethod method: class_.getMethods()){
//					EList<Event> elaboratesList = method.getElaborates();
//					for(Event event: elaboratesList){
//						// if the elaboration event is the same as the proB operation name
//						if(event.getName().equals(op.getName())){
//							// then the class is linked to the enabled ProB op
//							className = class_.getName();
//							break;
//						}
//					}
//				}
//			}
			if (simulatorView.isExternal(op)) {
				TableItem tableItem = new TableItem(methodsTable, SWT.NULL);
				String[] rowString = {operationInStringFormat(op)}; 
	//			String[] rowString = {className, operationInStringFormat(op)};
				tableItem.setText(rowString);
				if (simulatorView.isNextOp(op)) {
					select = j;
				}
				//className = "";
				j++;
			}
		}
		
		for(int i = 0; i < methodsTable.getColumnCount(); i++){
			methodsTable.getColumn(i).pack();
		}
		if (select>-1) methodsTable.select(select);
	}

	/**
	 * 
	 * @param opSignature
	 * @return
	 */
	public static Operation findOperation(Animator animator, String opSignature) {
		List<Operation> enabledOpsList = null;
		try {
			enabledOpsList = GetEnabledOperationsCommand.getOperations(
					animator,
					GetCurrentStateIdCommand.getID(animator));
		} catch (ProBException e1) {
			e1.printStackTrace();
		}
		if (enabledOpsList != null) {
			for (Operation op : enabledOpsList) {
				if (opSignature.equals(operationInStringFormat(op))){
					return op;
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param op
	 * @return
	 */
	private static String operationInStringFormat(Operation op) {
		return op.toString().replaceFirst("\\(", " (");
	}
}
