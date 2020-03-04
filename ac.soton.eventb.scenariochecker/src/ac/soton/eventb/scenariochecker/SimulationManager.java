/*******************************************************************************
 *  Copyright (c) 2019-2020 University of Southampton.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *   
 *  Contributors:
 *  University of Southampton - Initial implementation
 *******************************************************************************/

package ac.soton.eventb.scenariochecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.eventb.internal.scenariochecker.Clock;
import ac.soton.eventb.internal.scenariochecker.OracleHandler;
import ac.soton.eventb.internal.scenariochecker.Utils;
import ac.soton.eventb.probsupport.AnimationManager;
import de.prob.core.Animator;
import de.prob.core.command.ExecuteOperationCommand;
import de.prob.core.domainobjects.History;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.core.domainobjects.Variable;
import de.prob.exceptions.ProBException;

public class SimulationManager  {
	
	//the Singleton SimulationManager instance
	private static SimulationManager instance = null;

	public static SimulationManager getDefault() {
		if (instance==null){
			instance = new  SimulationManager();
		}
		return instance;
	}
	
	private IMachineRoot mchRoot;
	private Machine machine;
	private Clock clock = Clock.getInstance();	
	private Operation manuallySelectedOp = null;
	private int historyPosition=0;
	
	//classes that provide a control panel UI view for the simulation can register here
	private static List<ISimulationControlPanel> simulationControlPanels = new ArrayList<ISimulationControlPanel>();
	public void addSimulationControlPanel(ISimulationControlPanel operationSelector) {
		simulationControlPanels.add(operationSelector);
	}
	public void removeSimulationControlPanel(ISimulationControlPanel simulationControlPanel) {
		simulationControlPanels.remove(simulationControlPanel);
	}
	


	public void initialise(IMachineRoot mchRoot) {
		this.mchRoot = mchRoot;
		EMFRodinDB emfRodinDB = new EMFRodinDB();
		machine = (Machine) emfRodinDB.loadEventBComponent(mchRoot);
		historyPosition=0;
		clock.reset();
		//initialise oracle in record mode
		OracleHandler.getOracle().initialise(machine);
		OracleHandler.getOracle().restart("UML-B", machine);
		//start the control panels
		for (ISimulationControlPanel simulationControlPanel : simulationControlPanels) {
			simulationControlPanel.start();
		}
		//execute setup operation automatically
		if (inSetup()) {
			setup();
		}
	}

	/**
	 * stops the current simulation
	 * 
	 * @param mchRoot
	 */
	public void stop(IMachineRoot mchRoot) {
		if (mchRoot.getCorrespondingResource()!= this.mchRoot.getCorrespondingResource()) return;
		//stop the control panels
		for (ISimulationControlPanel simulationControlPanel : simulationControlPanels) {
			simulationControlPanel.stop();
		}
		// clear state
		this.mchRoot = null;
		machine = null;
		clock.reset();
		manuallySelectedOp = null;
		historyPosition = 0;

	}
	
	/**
	 * Tests whether the Scenario Checker is open and ready to start... 
	 * .. i.e. whether there is an open simulation control panel view attached
	 * @return
	 */
	public boolean isOpen() {
		for (ISimulationControlPanel scp : simulationControlPanels) {
			if (scp.isReady()) return true;
		}
		return false;
	}
	
	
	////////// interface for control panel to implement user commands/////////
	
	/**
	 * control panel selection changed.
	 * This is called by the simulationControlPanels when the user has selected an operation.
	 * If 'fire' is true, the new selection will be executed as a big step now
	 * 
	 * @param opName
	 * @param fireNow
	 */
	public void selectionChanged(String opName, boolean fireNow) {
		manuallySelectedOp = Utils.findOperation(opName);
		if (fireNow) {
			bigStep();
		}
	}
	
	
	public boolean isPlayback() {
		return OracleHandler.getOracle().isPlayback();
	}
	
	// implements the big step behaviour where we 
	// fire the next operation and then run to completion of all internal operations
	public boolean bigStep() {	
		if (inSetup()) return false;	
		Operation op = findNextOperation();
		//Animator animator = Animator.getAnimator();
		boolean progress = true;
		//execute at least one
		progress = executeOperation(op, false);
		//continue executing any internal operations
		List<Operation> loop = new ArrayList<Operation>(); //prevent infinite looping in case doesn't converge
		while (progress && (op = findNextOperation())!=null && Utils.isInternal(Utils.findEvent(op.getName(), machine)) && !loop.contains(op)) {
			progress = executeOperation(op, false);
			loop.add(op);
		}
		return progress;
	}
	
	// implements the small step behaviour where we fire one enabled external or internal operation
	public void singleStep(){
		Operation op = findNextOperation();
		executeOperation(op, false);
	}
	
	// implements the run behaviour where we take the selected number of big steps
	// (when not in playback mode we stop when a non-deterministic choice is available)  <<<<<<<<<< DISABLED FOR NOW - WHICH IS BETTER?
	public String runForTicks(Integer ticks) {
		if (inSetup()) return "In Setup";	
		final int endTime = clock.getValueInt()+ticks;
		boolean progress = true;
		while (clock.getValueInt() < endTime && progress) {
				progress = bigStep();
		}
		if (!progress) {
			return "Run terminated due to lack of progress.";
		}
		return "ok";
	}
	
	/*
	 * run the context set-up operation if enabled
	 */
	public boolean setup(){
		boolean ret = false;
		if (Animator.getAnimator().getCurrentState()==null) {
			ret= false;
		}else {
			List<Operation> enabledOperations = Animator.getAnimator().getCurrentState().getEnabledOperations();
			for (Operation op : enabledOperations){
				if ("SETUP_CONTEXT".equals(op.getName())){
					if (OracleHandler.getOracle().isPlayback()) {
						Operation nextop = OracleHandler.getOracle().findNextOperation();
						if (nextop!=null && "SETUP_CONTEXT".equals(nextop.getName())){
							OracleHandler.getOracle().consumeNextStep();
						}
					}
					executeOperation(op,false);
					ret=true;
				}
			}
		}
		return ret;
	}

	public void restartPressed() {
		clock.reset();
		historyPosition=0;
		if (OracleHandler.getOracle().isPlayback()){
			OracleHandler.getOracle().stopPlayback(false);
			OracleHandler.getOracle().startPlayback(true);
			for (ISimulationControlPanel controlPanel : simulationControlPanels) {
				controlPanel.updateModeIndicator(Mode.PLAYBACK);
			}
		}else{
			OracleHandler.getOracle().stopRecording(false);
			OracleHandler.getOracle().startRecording();	
			for (ISimulationControlPanel controlPanel : simulationControlPanels) {
				controlPanel.updateModeIndicator(Mode.RECORDING);
			}
		}
		AnimationManager.restartAnimation(mchRoot);
	}

	public void savePressed() {
		OracleHandler.getOracle().saveRecording();
		for (ISimulationControlPanel controlPanel : simulationControlPanels) {
			controlPanel.updateModeIndicator(Mode.SAVED);
		}
	}

	public void replayPressed() {
		clock.reset();
		historyPosition=0;
		if (!OracleHandler.getOracle().isPlayback()){
			OracleHandler.getOracle().stopRecording(false);
			OracleHandler.getOracle().startPlayback(false);
		}
		for (ISimulationControlPanel controlPanel : simulationControlPanels) {
			controlPanel.updateModeIndicator(Mode.PLAYBACK);
		}
		AnimationManager.restartAnimation(mchRoot);
	}

	public void stopPressed() {
		if (OracleHandler.getOracle().isPlayback()){
			OracleHandler.getOracle().stopPlayback(false);
		}
		for (ISimulationControlPanel controlPanel : simulationControlPanels) {
			controlPanel.updateModeIndicator(Mode.RECORDING);
		}
	}
	
	//////////////////// ProB listener interface ////////////
	/**
	 * called by the Animation Listener for ProB when the state has changed
	 * Since the order of notifications of state changes can vary we use the history 
	 * and only update when the history contains information past the trace point of the last update
	 * The oracle is only updated for external operations.
	 * 
	 */
		public void currentStateChanged(State activeState, Operation operation) {
			if (machine==null)  return;
			History history = Animator.getAnimator().getHistory();
			if (historyPosition ==0 || history.getCurrentPosition()>historyPosition) {
				Map<String, Variable> stateMap = Animator.getAnimator().getCurrentState().getValues();
				//createButtonGroup();
				
				{	//update the enabled ops table
					//if (methodsTable == null) return;
					State currentState = Animator.getAnimator().getCurrentState();
					List<Operation> enabledOps = currentState.getEnabledOperations();
					List<String> opnames = new ArrayList<String>();
					// for each enabled operation in the ProB model
					int select = -1;
					int j=0;
					for(Operation op: enabledOps){
						if (Utils.isExternal(Utils.findEvent(op.getName(), machine))) {
							opnames.add(Utils.operationInStringFormat(op));
							
//							TableItem tableItem = new TableItem(methodsTable, SWT.NULL);
//							String[] rowString = {Utils.operationInStringFormat(op)}; 
//							tableItem.setText(rowString);
							
							if (op==manuallySelectedOp || Utils.isNextOp(op)) {
								select = j;
							}
							j++;
						}
					}
					for (ISimulationControlPanel simulationControlPanel : simulationControlPanels) {
						simulationControlPanel.updateEnabledOperations(opnames,select);
					}

				}
				
				if (OracleHandler.getOracle()!=null) {
					for (int i=historyPosition; i<history.getCurrentPosition(); i++) {
						//n.b. history is indexed backwards from the current state.. i.e 0 get current, -1 gets previous etc.
						//(the last operation is in the previous position; current pos never has an operation, it is just the post-state)
						int pos = i-history.getCurrentPosition();
						Operation op = history.getHistoryItem(pos).getOperation();
						
						//we only record external events
						if (op!=null && (Utils.isExternal(Utils.findEvent(op.getName(), machine)) || op.getName().equals("SETUP_CONTEXT"))) {
							OracleHandler.getOracle().addStepToTrace(machine.getName(), op, clock.getValue());	
							OracleHandler.getOracle().startSnapshot(clock.getValue());
							//the post state of an operation is in the next history item. 
							stateMap = history.getHistoryItem(pos+1).getState().getValues();
							for (Entry<String, Variable> entry : stateMap.entrySet()) {
								if (!Utils.isPrivate(entry.getKey(), machine)){
									OracleHandler.getOracle().addValueToSnapshot(entry.getKey(), entry.getValue().getValue(), clock.getValue());
								}
							}
							OracleHandler.getOracle().stopSnapshot(clock.getValue());
						}
					}
				}
				historyPosition = history.getCurrentPosition();
			}
		}	
	
		
		
		
		
	///////////////// private utilities to help with execution ///////////////////////////
	
		/*
		 * check whether the context needs to be set up
		 */
		private boolean inSetup(){
			List<Operation> enabledOperations = Animator.getAnimator().getCurrentState().getEnabledOperations();
			for (Operation op : enabledOperations){
				if ("SETUP_CONTEXT".equals(op.getName()) ){
					return true;
				}
			}
			return false;
		}
		
		
	/**
	 * finds the next operation to be executed.
	 * when not in playback mode, it is manually (or randomly) selected from those that are enabled according to priority (internal first)
	 * when in playback mode, external events are given by the next operation in the OracleHandler.getOracle() being replayed,
	 * 
	 * @param animator
	 * @return
	 */
	private Operation findNextOperation() {	
		Operation nextOp = null;
		State currentState = Animator.getAnimator().getCurrentState();	
		List<Operation> ops = prioritise(currentState.getEnabledOperations());
		nextOp = 	ops.isEmpty()? null: 
					ops.contains(manuallySelectedOp) ? manuallySelectedOp :
					pickFrom(ops);

		if (OracleHandler.getOracle().isPlayback() && Utils.isExternal(Utils.findEvent(nextOp.getName(), machine))){
			nextOp = OracleHandler.getOracle().findNextOperation();
		}
		return nextOp;
	}

	private static final Random random = new Random();
	private Operation pickFrom(List<Operation> ops) {
		Operation op = ops.get(random.nextInt(ops.size()));
		return op;
	}
	
	/**
	 * filter the given list of operations so that it contains the subset with the highest eventPriorities
	 * 
	 * 
	 * @param enabledOperations
	 * @return
	 */
	private List<Operation> prioritise(List<Operation> enabledOperations) {
		List<Operation> filtered = new ArrayList<Operation>();
		Integer current = Integer.MAX_VALUE;
		
		for (Operation op : enabledOperations) {
			Integer priority;
			Event ev = Utils.findEvent(op.getName(), machine);
			priority = ev==null? -1 : Utils.getPriority(ev);
			if (priority>current) continue; 	//ignore lesser (i.e. higher int) eventPriorities
			if (priority<current) {				//found a better eventPriorities
				filtered.clear();
				current=priority;
			}
			filtered.add(op);
		}
		return filtered;
	}

	/**
	 * execute the given operation while maintaining the clock
	 * 
	 * If the operation execution succeeds, and the operation is not internal,
	 * the clock is incremented
	 * 
	 * @param animator
	 * @param operation
	 * @param silent
	 * @return
	 */
	private boolean executeOperation(Operation operation, boolean silent){
		if (operation==null) return false;
		try {
			if (OracleHandler.getOracle().isPlayback() && Utils.isExternal(Utils.findEvent(operation.getName(), machine))) {
				OracleHandler.getOracle().consumeNextStep();
			}
			ExecuteOperationCommand.executeOperation(Animator.getAnimator(), operation, silent);
			//waitingForOperation = operation;
			if (Utils.isExternal(Utils.findEvent(operation.getName(), machine))) clock.inc();
		} catch (ProBException e) {
			e.printStackTrace();
			//waitingForOperation=null;
			return false;
		}
		return true;
	}



}
