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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.eventb.emf.oracle.OracleFactory;
import ac.soton.eventb.emf.oracle.Run;
import ac.soton.eventb.emf.oracle.Snapshot;
import ac.soton.eventb.emf.oracle.Step;
import ac.soton.eventb.internal.scenariochecker.Clock;
import ac.soton.eventb.internal.scenariochecker.OracleHandler;
import ac.soton.eventb.internal.scenariochecker.Triplet;
import ac.soton.eventb.internal.scenariochecker.Utils;
import ac.soton.eventb.probsupport.AnimationManager;
import ac.soton.eventb.probsupport.data.History_;
import ac.soton.eventb.probsupport.data.Operation_;
import ac.soton.eventb.probsupport.data.State_;

/**
 * 
 * This is the Scenario Checker manager
 * 
 * @author cfsnook
 *
 */
public class ScenarioCheckerManager  {
	
	private static final String recordingName = "Scenario";
	private static final String SETUP = "SETUP_CONTEXT";
	private static final String INITIALISATION = "INITIALISATION";
	
	//the Singleton ScenarioCheckerManager instance
	private static ScenarioCheckerManager instance = null;

	public static ScenarioCheckerManager getDefault() {
		if (instance==null){
			instance = new  ScenarioCheckerManager();
		}
		return instance;
	}
	
	private IMachineRoot mchRoot;
	private Machine machine;
	//TODO: We could delete clock - it is not used for much now that we only save externals
	private Clock clock = Clock.getInstance();	
	private Operation_ manuallySelectedOp = null;
	private List<Operation_> enabledOperations = null;
	private boolean dirty = false;
	
	//classes that provide a control panel UI view for the simulation can register here
	private static List<IScenarioCheckerControlPanel> scenarioCheckerControlPanels = new ArrayList<IScenarioCheckerControlPanel>();
	public void addSimulationControlPanel(IScenarioCheckerControlPanel operationSelector) {
		scenarioCheckerControlPanels.add(operationSelector);
	}
	public void removeSimulationControlPanel(IScenarioCheckerControlPanel scenarioCheckerControlPanel) {
		scenarioCheckerControlPanels.remove(scenarioCheckerControlPanel);
	}
	
	
	
	public void initialise(IMachineRoot mchRoot) {
		this.mchRoot = mchRoot;
		EMFRodinDB emfRodinDB = new EMFRodinDB();
		machine = (Machine) emfRodinDB.loadEventBComponent(mchRoot);
		clock.reset();
		//initialise oracle in record mode
		OracleHandler.getOracle().initialise(machine);
		OracleHandler.getOracle().restart(recordingName, machine);
		//start the control panels
		for (IScenarioCheckerControlPanel scenarioCheckerControlPanel : scenarioCheckerControlPanels) {
			scenarioCheckerControlPanel.start();
		}
		//execute setup operation automatically
		if (inSetup()) {
			setup();
		}
		dirty = false;
	}

	/**
	 * stops the current simulation
	 * 
	 * @param mchRoot
	 */
	public void stop(IMachineRoot mchRoot) {
		if (this.mchRoot==null) return;
		if (mchRoot.getCorrespondingResource() != this.mchRoot.getCorrespondingResource()) return;
		//stop the control panels
		for (IScenarioCheckerControlPanel scenarioCheckerControlPanel : scenarioCheckerControlPanels) {
			scenarioCheckerControlPanel.stop();
		}
		
		// clear state
		this.mchRoot = null;
		machine = null;
		clock.reset();
		manuallySelectedOp = null;
		dirty=false;
	}
	
	/**
	 * Tests whether the Scenario Checker is open and ready to start... 
	 * .. i.e. whether there is an open simulation control panel view attached
	 * @return
	 */
	public boolean isOpen() {
		for (IScenarioCheckerControlPanel scp : scenarioCheckerControlPanels) {
			if (scp.isReady()) return true;
		}
		return false;
	}
	
	
	////////// interface for control panel to implement user commands/////////
	
	/**
	 * control panel selection changed.
	 * This is called by the scenarioCheckerControlPanels when the user has selected an operation.
	 * If 'fire' is true, the new selection will be executed as a big step now
	 * 
	 * @param opName
	 * @param fireNow
	 */
	public void selectionChanged(String operationSignature, boolean fireNow) {
		for (Operation_ operation : AnimationManager.getEnabledOperations(mchRoot)) {
			if (operationSignature.equals(operation.inStringFormat())){
				manuallySelectedOp = operation; 
				if (fireNow) {
					bigStep();
				}
				break;
			}
		}
	}
	
	public boolean isPlayback() {
		return OracleHandler.getOracle().isPlayback();
	}
	
	/**
	 * 	implements the big step behaviour where we fire the next operation and then run to completion of all internal operations
	 */

	public boolean bigStep() {	
		if (inSetup()) return false;	
		Operation_ op = findNextOperation();
		//Animator animator = Animator.getAnimator();
		boolean progress = true;
		//execute at least one
		progress = executeOperation(op, false);
		//continue executing any internal operations
		List<Operation_> loop = new ArrayList<Operation_>(); //prevent infinite looping in case doesn't converge
		while (progress && (op = findNextOperation())!=null &&
				Utils.isInternal(Utils.findEvent(op.getName(), machine)) &&
				!loop.contains(op)) {
			progress = executeOperation(op, false);
			loop.add(op);
		}
		return progress;
	}
	
	/**
	 *  implements the small step behaviour where we fire one enabled external or internal operation
	 */
	public void singleStep(){
		Operation_ op = findNextOperation();
		executeOperation(op, false);
	}
	
	/**
	 *  implements the run behaviour where we take the selected number of big steps
	 * @param ticks
	 * @return
	 */
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
	
	/**
	 * run the context set-up operation if enabled
	 **/
	public boolean setup(){
		boolean ret = false;
		for (Operation_ op : AnimationManager.getEnabledOperations(mchRoot)){
			if (SETUP.equals(op.getName())){
				if (OracleHandler.getOracle().isPlayback()) {
					Operation_ nextop = OracleHandler.getOracle().findNextOperation();
					if (nextop!=null && SETUP.equals(nextop.getName())){
						OracleHandler.getOracle().consumeNextStep();
					}
				}
				executeOperation(op,false);
				ret=true;
			}
		}
		return ret;
	}

	/**
	 * restarts the current scenario
	 */
	public void restartPressed() {
		clock.reset();
		AnimationManager.restartAnimation(mchRoot);
		if (OracleHandler.getOracle().isPlayback()){
			OracleHandler.getOracle().stopPlayback();
			OracleHandler.getOracle().startPlayback(true);
			for (IScenarioCheckerControlPanel controlPanel : scenarioCheckerControlPanels) {
				controlPanel.updateModeIndicator(Mode.PLAYBACK);
			}
		}else{
			for (IScenarioCheckerControlPanel controlPanel : scenarioCheckerControlPanels) {
				controlPanel.updateModeIndicator(Mode.RECORDING);
			}
		}
		OracleHandler.getOracle().restart(recordingName, machine);
	}
	
	/**
	 * saves the current scenario
	 */
	public void savePressed() {
		saveToOracle();
		setDirty(false);
	}
	
	/**
	 * starts playing back the selected scenario
	 */
	public void replayPressed() {
		clock.reset();
		AnimationManager.restartAnimation(mchRoot);
		if (!OracleHandler.getOracle().isPlayback()){
			OracleHandler.getOracle().startPlayback(false);
			for (IScenarioCheckerControlPanel controlPanel : scenarioCheckerControlPanels) {
				controlPanel.updateModeIndicator(Mode.PLAYBACK);
			}
		}
		OracleHandler.getOracle().restart(recordingName, machine);
	}

	/**
	 * stops the current playback and switches to recording mode
	 * (without restarting - so a new scenario can continue from the played back one)
	 */
	public void stopPressed() {
		if (OracleHandler.getOracle().isPlayback()){
			OracleHandler.getOracle().stopPlayback();
		}
		for (IScenarioCheckerControlPanel controlPanel : scenarioCheckerControlPanels) {
			controlPanel.updateModeIndicator(Mode.RECORDING);
		}
	}
	
	/**
	 * checks whether the scenario is dirty
	 * (i.e. a scenario has been manually played beyond initialisation, but not yet saved)
	 * @return
	 */
	public boolean isDirty() {
		return dirty;
	}
	
	
	//////////////////// ProB listener interface ////////////
	/**
	 * called by the Animation Listener for ProB when the state has changed
	 * Since the order of notifications of state changes can vary we use the history 
	 * and only update when the history contains information past the trace point of the last update
	 * The oracle is only updated for external operations.
	 * 
	 */
	
	public void currentStateChanged(IMachineRoot mchRoot) {
		//check state matches oracle (if in playback)
		List<Triplet <String, String, String>> result = new ArrayList<Triplet<String,String,String>>();
		Map<String, String> currentState = AnimationManager.getCurrentState(mchRoot).getAllValues();
		Snapshot goldSnapshot = OracleHandler.getOracle().getNextGoldSnapshot(); //returns null if not in playback
		if (goldSnapshot!=null) {
			for (Map.Entry<String, String> value : goldSnapshot.getValues()){
				//if (!(currentState.containsKey(goldValue.getKey()) && currentState.get(goldValue.getKey()).equals(goldValue.getValue()))){
					result.add(Triplet.of(value.getKey(), currentState.get(value.getKey()), value.getValue()));
				//}
			}
		}else {
			for (Map.Entry<String, String> value : currentState.entrySet()){
				result.add(Triplet.of(value.getKey(), value.getValue(), ""));
			}
		}
		for (IScenarioCheckerControlPanel scenarioCheckerControlPanel : scenarioCheckerControlPanels) {
			scenarioCheckerControlPanel.updateFailures(result);
		}
		//update the enabled ops table
		enabledOperations = AnimationManager.getEnabledOperations(mchRoot);
		List<String> operationSignatures = new ArrayList<String>();
		Operation_ selectedOp = null;
		for(Operation_ op: enabledOperations){
			if (Utils.isExternal(Utils.findEvent(op.getName(), machine))) {
				operationSignatures.add(op.inStringFormat());
			}
		}
		// find the selected op in playback
		if (OracleHandler.getOracle().isPlayback()) {
			selectedOp = OracleHandler.getOracle().findNextOperation();
			// if we drop out of playback show switch to recording
			if (!OracleHandler.getOracle().isPlayback()) {		
				for (IScenarioCheckerControlPanel scenarioCheckerControlPanel : scenarioCheckerControlPanels) {
					scenarioCheckerControlPanel.updateModeIndicator(Mode.RECORDING);
				}	
			}
		}
		//if no playback op selected, use manually selected op
		if (selectedOp==null) {	
			selectedOp = manuallySelectedOp;
		}
		// find index of selected op so that it can be highlighted
		int select = selectedOp==null? -1 : operationSignatures.indexOf(selectedOp.inStringFormat());
		// update operations tables in the control panels
		for (IScenarioCheckerControlPanel scenarioCheckerControlPanel : scenarioCheckerControlPanels) {
			scenarioCheckerControlPanel.updateEnabledOperations(operationSignatures, select);
		}
	}		
		
	///////////////// private utilities to help with execution ///////////////////////////

	/**
	 * sets the dirty flag and tells the control panels to show as dirty
	 * @param dirty
	 */
	private void setDirty(boolean dirty) {
		for (IScenarioCheckerControlPanel controlPanel : scenarioCheckerControlPanels) {
			controlPanel.updateDirtyStatus(dirty);;
		}
		this.dirty=dirty;
	}
	
	/**
	 * saves the history as a scenario
	 * (the history is obtained from the animation manager)
	 * 
	 */
	private void saveToOracle() {
		if (OracleHandler.getOracle()!=null) {
			History_ history = AnimationManager.getHistory(mchRoot);
			Run recording = OracleFactory.eINSTANCE.createRun();
			recording.setName(recordingName);
			for (History_.HistoryItem_ hi : history.getAllItems() ) { 
				Operation_ op = hi.operation;
				//we only record external events
				if (op!=null && (Utils.isExternal(Utils.findEvent(op.getName(), machine)) || SETUP.equals(op.getName()))) {
					Step step = OracleFactory.eINSTANCE.createStep();
					step.setName(op.getName());
					step.getArgs().addAll(op.getArguments());
					step.setMachine(machine.getName());
					step.setClock(clock.getValue());
					recording.getEntries().add(step);	
					Snapshot currentSnapshot = OracleFactory.eINSTANCE.createSnapshot();
					currentSnapshot.setClock(clock.getValue());
					currentSnapshot.setMachine(machine.getName());
					State_ state = hi.state;
					for (Entry<String, String> entry : state.getAllValues().entrySet()) {
						if (!Utils.isPrivate(entry.getKey(), machine)){
							if (hasChanged(entry.getKey(),entry.getValue(), recording)){
								currentSnapshot.getValues().put(entry.getKey(), entry.getValue());
							}
						}
					}
					if (!currentSnapshot.getValues().isEmpty()) {
						currentSnapshot.setResult(true);	//for now, all snapshots are result = true
						recording.getEntries().add(currentSnapshot);
					}
				}
			}
			try {
				OracleHandler.getOracle().save(recording);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
		/**
		 * check whether the context needs to be set up
		 */
		private boolean inSetup(){
			List<Operation_> enabledOperations = AnimationManager.getEnabledOperations(mchRoot);
			for (Operation_ op : enabledOperations){
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
	private Operation_ findNextOperation() {	
		Operation_ nextOp = null;
		nextOp = 	manuallySelectedOp!=null && 
				isEnabled(manuallySelectedOp) ? manuallySelectedOp :
					pickFrom(prioritise(AnimationManager.getEnabledOperations(mchRoot)));
		if (OracleHandler.getOracle().isPlayback() && Utils.isExternal(Utils.findEvent(nextOp.getName(), machine))){
			Operation_ playbackOp = OracleHandler.getOracle().findNextOperation();
			if ("INITIALISATION".equals(nextOp.getName()) &&
					"SETUP_CONTEXT".equals(playbackOp.getName())) {
				OracleHandler.getOracle().consumeNextStep();
				playbackOp = OracleHandler.getOracle().findNextOperation();
			}
			nextOp = playbackOp;
			//it may come out of playback here.. in which case update control panel
			if (!OracleHandler.getOracle().isPlayback()) {
				for (IScenarioCheckerControlPanel controlPanel : scenarioCheckerControlPanels) {
					controlPanel.updateModeIndicator(Mode.RECORDING);
				}
			}
		}
		return nextOp;
	}

	/**
	 * checks whether a particular operation is currently enabled
	 * @param op
	 * @return
	 */
	private boolean isEnabled(Operation_ op) {
		List<Operation_> enabled = AnimationManager.getEnabledOperations(mchRoot);
		for (Operation_ eop : enabled) {
			if (eop!=null && op.inStringFormat().equals(eop.inStringFormat())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * selects an operation from the given list at random
	 */
	private static final Random random = new Random();
	private Operation_ pickFrom(List<Operation_> ops) {
		Operation_ op = ops.get(random.nextInt(ops.size()));
		return op;
	}
	
	/**
	 * filter the given list of operations so that it contains the subset with the highest eventPriorities
	 * 
	 * @param enabledOperations
	 * @return
	 */
	private List<Operation_> prioritise(List<Operation_> enabledOperations) {
		List<Operation_> filtered = new ArrayList<Operation_>();
		Integer current = Integer.MAX_VALUE;
		
		for (Operation_ op : enabledOperations) {
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
	 * If the operation execution succeeds, and the operation is not internal,
	 * the clock is incremented
	 * 
	 * @param animator
	 * @param operation
	 * @param silent
	 * @return
	 */
	private boolean executeOperation(Operation_ operation, boolean silent){
		if (operation==null) return false;
		boolean playback = OracleHandler.getOracle().isPlayback();
		if (playback && Utils.isExternal(Utils.findEvent(operation.getName(), machine))) {
			OracleHandler.getOracle().consumeNextStep();
		}
		System.out.println("executing operation : "+operation.getName()+" "+operation.getArguments() );
		AnimationManager.executeOperation(mchRoot, operation, silent);
		Event ev =Utils.findEvent(operation.getName(), machine);
		if (ev!=null && Utils.isExternal(ev)) clock.inc();
		if (!playback && !SETUP.equals(operation.getName()) && !INITIALISATION.equals(operation.getName()) ) {
			setDirty(true);
		}
		return true;
	}
	
	/**
	 * Checks whether the value of the named identifier has changed since the last time it was recorded in the recording.
	 * 
	 * @param name
	 * @param value
	 * @param run 
	 * @return
	 */
	private boolean hasChanged(String name, String value, Run run) {
		if (run == null) return true;
		EList<ac.soton.eventb.emf.oracle.Entry> entries = run.getEntries();		
		for (int i = entries.size()-1; i>=0 ; i = i-1){
			if (entries.get(i) instanceof Snapshot){
				EMap<String, String> snapshotValues = ((Snapshot) entries.get(i)).getValues();
				if (snapshotValues.containsKey(name)){
					if (value.equals(snapshotValues.get(name))){
						return false;
					}else{
						return true;
					}	
				}
			}
		}
		return true;
	}

}
