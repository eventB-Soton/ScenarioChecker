/*******************************************************************************
 *  Copyright (c) 2019-2021 University of Southampton.
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
import java.util.Collections;
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
import org.eventb.emf.core.machine.Variable;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.eventb.emf.oracle.OracleFactory;
import ac.soton.eventb.emf.oracle.Run;
import ac.soton.eventb.emf.oracle.Snapshot;
import ac.soton.eventb.emf.oracle.Step;
import ac.soton.eventb.internal.scenariochecker.Clock;
import ac.soton.eventb.internal.scenariochecker.OracleHandler;
import ac.soton.eventb.internal.scenariochecker.Playback;
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
	
	private static final String CAN_NOT_RUN_UNTIL_CONTEXT_SET_UP = "Can not run until context set up";
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
	
	private ScenarioCheckerManager() {	//make constructor private
	}
	
	private IMachineRoot mchRoot = null;
	private Machine machine = null;
	private List<String> publicVariables= new ArrayList<String>();
	private Clock clock = new Clock();	
	private Operation_ manuallySelectedOp = null;
	private List<Operation_> enabledOperations = null;
	private boolean dirty = false;
	private Playback playback = null;
	private List<IScenarioCheckerView> scenarioCheckerViews = new ArrayList<IScenarioCheckerView>();
	
	/**
	 * add a scenario checker view to those registered to receive notifications from the Scenario Checker Manager
	 * 
	 * @param scenarioCheckerView
	 */
	public void addSimulationView(IScenarioCheckerView scenarioCheckerView) {
		scenarioCheckerViews.add(scenarioCheckerView);
	}
	/**
	 * remove a scenario checker view from those registered to receive notifications from the Scenario Checker Manager
	 * @param scenarioCheckerView
	 */
	public void removeSimulationView(IScenarioCheckerView scenarioCheckerView) {
		scenarioCheckerViews.remove(scenarioCheckerView);
	}
	
	/**
	 * Initialise the Scenario Checker Manager with a particular machine root
	 * @param mchRoot
	 */
	public void initialise(IMachineRoot mchRoot) {
		this.mchRoot = mchRoot;
		EMFRodinDB emfRodinDB = new EMFRodinDB();
		machine = (Machine) emfRodinDB.loadEventBComponent(mchRoot);
		publicVariables.clear();
		for (Variable v : machine.getVariables()) {
			if(!v.getComment().startsWith("<PRIVATE>")) {
				publicVariables.add(v.getName());
			}
		}
		//initialise oracle file handler
		OracleHandler.getOracle().initialise(recordingName, machine);
		//start the scenario checker views
		for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
			scenarioCheckerView.start(machine.getName());
		}
		displayMessage("Checking "+machine.getName());
		restart(mchRoot);
	}
	
	/**
	 * This (re)starts the scenario without affecting any playback settings etc.
	 * @param mchRoot
	 */
	public void restart(IMachineRoot mchRoot) {
		if (mchRoot.getCorrespondingResource() != this.mchRoot.getCorrespondingResource()) return;
		clock.reset();
		updateModeIndicator();
		setDirty(false);
		//execute setup operation automatically
		if (inSetup()) {
			runSetup();
		}
	}

	/**
	 * stops the current simulation
	 * 
	 * @param mchRoot
	 */
	public void stop(IMachineRoot mchRoot) {
		//if (this.mchRoot==null || mchRoot.getCorrespondingResource() != this.mchRoot.getCorrespondingResource()) return;
		playback=null;
		//stop the scenario checker views
		for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
			scenarioCheckerView.stop();
		}
		displayMessage("Stopped");
		// clear state
		this.mchRoot = null;
		machine = null;
		clock.reset();
		manuallySelectedOp = null;
		setDirty(false);
	}
	
	/**
	 * Tests whether the Scenario Checker is open and ready to start... 
	 * .. i.e. whether there is an open scenario checker view  attached
	 * @return
	 */
	public boolean isOpen() {
		for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
			if (scenarioCheckerView.isReady()) return true;
		}
		return false;
	}
	
	
	////////// interface for scenario checker views to implement user commands/////////
	
	/**
	 * control panel selection changed.
	 * This is called by the scenarioCheckerViews when the user has selected an operation.
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
	
	/**
	 * check whether running in playback mode or not
	 * 
	 * @return
	 */
	public boolean isPlayback() {
		return playback!=null && playback.isPlayback();
	}
	
	/**
	 * 	implements the big step behaviour where we fire the next operation and then run to completion of all internal operations
	 */
	public boolean bigStep() {	
		String message = "BigStep - " ;
		boolean progress = false;
		if (inSetup()) {
			message = CAN_NOT_RUN_UNTIL_CONTEXT_SET_UP;	
		}else {
			Operation_ op = pickNextOperation();
			//execute at least one
			progress = executeOperation(op, false);
			message = message+
					(progress? "fired " : "FAILED ")+
					(isExternal(op)?"[ext] ":"[int] ")+
					op.inStringFormat();
			//continue executing any internal operations
			List<Operation_> loop = new ArrayList<Operation_>(); //prevent infinite looping in case doesn't converge
			while (	progress && 
					(op = pickNextOperation())!=null &&
					!isExternal(op) &&
					Collections.frequency(loop, op)<10// !loop.contains(op)
					) {
				progress = executeOperation(op, false);
				
				message = message+ "\n  - "+
						(progress? "fired " : "FAILED ")+
						"[int] "+
						op.inStringFormat();
				loop.add(op);
			}
			if (progress) {
				if (op==null) {
					message = message+ "\n  - Big step aborted due to deadlock ";
				}else if (isExternal(op)){
					message = message+ "\n  - Big step ran to completion";
				}else if (Collections.frequency(loop, op)>=10) {  //loop.contains(op)) {
					message = message+ "\n  - Big step aborted due to loop on "+op.inStringFormat();
				}else {
					message = message+ "\n  - Big step aborted for a mysterious reason";
				}
			}
			updateModeIndicator();
		}
		displayMessage(message);
		return progress;
	}
	
	/**
	 *  implements the small step behaviour where we fire one enabled external or internal operation
	 */
	public void singleStep(){
		Operation_ op = pickNextOperation();
		if (op==null) {
			displayMessage("Small step aborted - nothing enabled");
		}else {
			executeOperation(op, false);
			updateModeIndicator();
			String type = isExternal(op)? "[ext] ":"[int] ";
			displayMessage("Small step - "+type+op.inStringFormat());
		}
	}
	
	/**
	 *  implements the run behaviour where we take the selected number of big steps
	 * @param ticks
	 * @return
	 */
	// (when not in playback mode we stop when a non-deterministic choice is available)  <<<<<<<<<< DISABLED FOR NOW - WHICH IS BETTER?
	public String runForTicks(Integer ticks) {
		String msg;
		if (inSetup()) {
			msg = CAN_NOT_RUN_UNTIL_CONTEXT_SET_UP;
		}else{
			final int endTime = clock.getValueInt()+ticks;
			boolean progress = true;
			while (clock.getValueInt() < endTime && progress) {
					progress = bigStep();
			}
			int done= ticks-endTime + clock.getValueInt();
			if (!progress) {
				msg = "Run terminated after "+done+" of\n  "+ticks+" ticks due to lack of progress";
			}else {
				msg = "Completed all "+done+" ticks";
			}
		}
		displayMessage("Run For - " + msg);
		return msg;
	}

	/**
	 * restarts the animation.
	 * if in playback, the current scenario is replayed.
	 * if not, the animation will be reset in recording mode
	 * 
	 */
	public void restartPressed() {
		if (isPlayback()){
			playback.reset();
		}
		AnimationManager.restartAnimation(mchRoot);
		displayMessage("Restarted");
	}
	
	/**
	 * saves the current scenario
	 */
	public void savePressed() {
		Run run = makeRun(recordingName, AnimationManager.getHistory(mchRoot));
		try {
			OracleHandler.getOracle().save(run);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		setDirty(false);
		displayMessage("saved "+recordingName);
	}
	
	/**
	 *
	 * if playing back, stops and switches to recording mode
	 * 		(without restarting - so a new scenario can continue from the played back one)
	 * if recording, starts playing back a scenario selected by the user
	 */
	public void modeButtonPressed() {
		if (isPlayback()){
			playback=null;
		}else {// if recording, switch to playback
			playback = new Playback(OracleHandler.getOracle().getRun().getEntries());
			AnimationManager.restartAnimation(mchRoot);
		}
		updateModeIndicator();
		displayMessage("mode changed to "+(playback==null? Mode.RECORDING : Mode.PLAYBACK));
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

		if (mchRoot==null) {
			for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
				scenarioCheckerView.stop();
			}
			displayMessage(
					"The Scenario Checker aborted because ProB\n"+
					"   appears to have stopped animating the machine (i.e. sent null)");
			return;
		}
		if (mchRoot!=this.mchRoot) {
			for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
				scenarioCheckerView.stop();
			}
			displayMessage(
					"The Scenario Checker aborted because ProB\n"+
					"   appears to be animating a different machine:\n"+
					"   "+mchRoot.getCorrespondingResource().getName()
					);
			return;
		}

		{//update state views
			List<Triplet <String, String, String>> result = new ArrayList<Triplet<String,String,String>>();
			Map<String, String> currentState = AnimationManager.getCurrentState(mchRoot).getAllValues();
			// if in playback check state matches oracle
			
			if (isPlayback() && playback.getCurrentSnapshot()!=null) {
				
				for (Map.Entry<String, String> value : playback.getCurrentSnapshot().getValues()){
					if(publicVariables.contains(value.getKey())){
						result.add(Triplet.of(value.getKey(), currentState.get(value.getKey()), value.getValue()));
					}
				}
			}else {
				for (Map.Entry<String, String> value : currentState.entrySet()){
					if(publicVariables.contains(value.getKey())){
						result.add(Triplet.of(value.getKey(), value.getValue(), ""));
					}
				}
			}
			for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
				scenarioCheckerView.updateState(result);
			}
		}

		{//update the enabled external events views
			enabledOperations = AnimationManager.getEnabledOperations(mchRoot);
			List<String> operationSignatures = new ArrayList<String>();
			for(Operation_ op: enabledOperations){
				if (isExternal(op)) {
					operationSignatures.add(op.inStringFormat());
				}
			}

			// find index of the next op in playback
			int selectedOp = -1;
			if (isPlayback() && playback.getNextOperation()!=null) {
				selectedOp = enabledOperations.indexOf(playback.getNextOperation());
			}

			// update operations tables in the scenario checker views
			for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
				scenarioCheckerView.updateEnabledOperations(operationSignatures, selectedOp);
			}
		}
	}		

	///////////////// private utilities to help with execution ///////////////////////////

	/**
	 * update the mode indicator on scenario checker views
	 * according to whether the scenario is in playback or recording
	 */
	private void updateModeIndicator() {
		if (isPlayback()){
			for (IScenarioCheckerView controlPanel : scenarioCheckerViews) {
				controlPanel.updateModeIndicator(Mode.PLAYBACK);
			}
		}else{
			for (IScenarioCheckerView controlPanel : scenarioCheckerViews) {
				controlPanel.updateModeIndicator(Mode.RECORDING);
			}
		}
	}
	
	/**
	 * sets the dirty flag and tells the scenario checker views to show as dirty or not
	 * @param dirty
	 */
	private void setDirty(boolean dirty) {
		this.dirty=dirty;
		for (IScenarioCheckerView controlPanel : scenarioCheckerViews) {
			controlPanel.updateDirtyStatus(dirty);;
		}
	}
	
	/**
	 * make a new Run from the given animation History_
	 * @param history
	 * @param run
	 */
	private Run makeRun(String name, History_ history) {
		Run run = OracleFactory.eINSTANCE.createRun();
		run.setName(name);
		Clock runclock = new Clock();
		Snapshot currentSnapshot = null;
		for (History_.HistoryItem_ hi : history.getAllItems() ) { 
			Operation_ op = hi.operation;
			//we only record external events
			if (op!=null) {
				if (isExternal(op) || SETUP.equals(op.getName())) {
					//create a new step entry in the recording
					Step step = makeStep(op, runclock.getValue());
					run.getEntries().add(step);
					//create a new empty snapshot in the recording
					currentSnapshot = makeSnapshot(runclock.getValue());
					run.getEntries().add(currentSnapshot);
					//update clock
					runclock.inc();
				}
				//add any changed state to the current snapshot (for internal as well as external events)
				//later state changes overwrite earlier ones so that we end up with the state at the end of the run
				//(if a variable changes and then reverts this is still counted as a change)
				State_ state = hi.state;
				for (Entry<String, String> entry : state.getAllValues().entrySet()) {
					if (!Utils.isPrivate(entry.getKey(), machine)){
						if (hasChanged(entry.getKey(),entry.getValue(), run)){
							if (currentSnapshot!=null) {
								currentSnapshot.getValues().put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			}
		}
		return run;
	}
	
	/**
	 * make an empty Snapshot
	 * 
	 * @return
	 */
	private Snapshot makeSnapshot(String tick) {
		Snapshot currentSnapshot;
		currentSnapshot = OracleFactory.eINSTANCE.createSnapshot();
		currentSnapshot.setClock(tick);
		currentSnapshot.setMachine(machine.getName());
		currentSnapshot.setResult(true);	//for now, all snapshots are result = true
		return currentSnapshot;
	}
	
	/**
	 * make a new Step from the given Operation_
	 * 
	 * @param op
	 * @return
	 */
	private Step makeStep(Operation_ op, String tick) {
		Step step = OracleFactory.eINSTANCE.createStep();
		step.setName(op.getName());
		step.getArgs().addAll(op.getArguments());
		step.setMachine(machine.getName());
		step.setClock(tick);
		return step;
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
	 * run the context set-up operation if enabled
	 **/
	private boolean runSetup(){
		boolean ret = false;
		for (Operation_ op : AnimationManager.getEnabledOperations(mchRoot)){
			if (SETUP.equals(op.getName())){
				if (isPlayback() && SETUP.equals(playback.getNextStep().getName())){
					playback.consumeStep();
				}
				executeOperation(op,false);
				ret=true;
			}
		}
		return ret;
	}
		
	/**
	 * selects the next operation to be executed.
	 * 
	 * First an operation is randomly selected from those that are enabled (prioritising internal ones and using the priority annotations if any).
	 * 
	 * If the selected operation is external (i.e. no internal ones are enabled):
	 * when in playback mode, the next external operation is taken from the recording;
	 * when in recording mode, if the user has selected an operation, that is used, but if not, the one from the random selection is used.
	 * 
	 * whatever the outcome, any manual selection is cleared.
	 * 
	 * @return the operation to be executed
	 */
	private Operation_ pickNextOperation() {
		
		// pick an operation randomly subject to priorities (internal ones get priority and may be annotated with priorities)
		Operation_  nextOp = pickFrom(prioritise(AnimationManager.getEnabledOperations(mchRoot)));

		//if no internal operations are available, we might use a different method to get an external one
		if (nextOp!=null && isExternal(nextOp)){
			//in playback all externals are from the recording
			if (isPlayback()) {	
				nextOp = playback.getNextOperation();
			//in recording mode the user may have selected the next external
			}else if (manuallySelectedOp!=null && isEnabled(manuallySelectedOp)) {
				nextOp = manuallySelectedOp;
			}
		}
		//the manual selection only gets one chance to fire - it would be confusing to remember it for later
		manuallySelectedOp=null;
		
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
		if (ops.isEmpty()) return null;
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
	 * Execute the given operation.
	 * If recording and the operation is not setup nor initialisation, the scenario becomes dirty.
	 * If in playback and the operation is the next step, the playback progresses
	 * If the operation is external, the clock is incremented
	 * 
	 * 
	 * @param animator
	 * @param operation
	 * @param silent
	 * @return
	 */
	private boolean executeOperation(Operation_ operation, boolean silent){
		if (operation==null) return false;
		System.out.println("executing operation : "+operation.getName()+" "+operation.getArguments() );
		
		if (!isPlayback() && 
				!SETUP.equals(operation.getName()) && 
				!INITIALISATION.equals(operation.getName()) ) {
			setDirty(true);
		}
		
		if (isPlayback() && 
				operation.equals(playback.getNextOperation())) {
			playback.consumeStep();;
		}
		
		if (isExternal(operation)) {
			clock.inc();
		}
		
		//must make sure everything else is updated BEFORE executing,
		// as ProB will immediately call the listeners to update the views
		try {
			AnimationManager.executeOperation(mchRoot, operation, silent);
		}catch (Exception e) {			
			displayMessage("Exception in ProB while excecuting operation \n"+ e.toString());	
			return false;
		}
		
		return true;
	}
	
	/**
	 * checks whether the given operation is external
	 * @return
	 */
	private boolean isExternal(Operation_ operation) {
		return Utils.isExternal(Utils.findEvent(operation.getName(), machine));
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
	
	/**
	 * 
	 * Sends a message to the scenario checker views so they can display it
	 * 
	 * @param message
	 */
	private void displayMessage(String message) {
		for (IScenarioCheckerView scenarioCheckerView : scenarioCheckerViews) {
			scenarioCheckerView.displayMessage(message);
		}
	}

}
