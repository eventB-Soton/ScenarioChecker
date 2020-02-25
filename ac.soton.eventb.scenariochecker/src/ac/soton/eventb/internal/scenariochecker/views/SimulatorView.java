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
package ac.soton.eventb.internal.scenariochecker.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
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
import de.prob.core.command.GetCurrentStateIdCommand;
import de.prob.core.command.GetEnabledOperationsCommand;
import de.prob.core.domainobjects.History;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.core.domainobjects.Variable;
import de.prob.exceptions.ProBException;
import de.prob.ui.StateBasedViewPart;

public class SimulatorView extends StateBasedViewPart {
	
	public static final String ID = "ac.soton.eventb.internal.scenariochecker.views.SimulatorView"; //$NON-NLS-1$
	
	Display display = Display.getCurrent();
	Color red = display.getSystemColor(SWT.COLOR_RED);
	Color green = display.getSystemColor(SWT.COLOR_GREEN);
	Color blue = display.getSystemColor(SWT.COLOR_BLUE);
	
	private static SimulatorView simulatorView = null;
	public SimulatorView() {
		simulatorView = this; //may be called on restarting Rodin to re-create the views
	}
	public static SimulatorView getSimulatorView() {
		if (simulatorView==null){
			simulatorView = new  SimulatorView();
		}
		return simulatorView;
	}
	
	private String statusText;
	private String oldStatusText;
	private String getStatusText() {
		if (statusText == null) statusText = "NULL";
		return statusText;
	}
	
	private Machine machine;
	private IMachineRoot mchRoot;

	public void initialise(IMachineRoot mchRoot) {
		this.mchRoot = mchRoot;
		EMFRodinDB emfRodinDB = new EMFRodinDB();
		machine = (Machine) emfRodinDB.loadEventBComponent(mchRoot);
		historyPosition=0;
		clock.reset();
		
		//initialise oracle in record mode
		OracleHandler.getOracle().initialise(machine);
		
		//umlbPerspective();
	}
	
// DISABLED FOR NOW AS SEEMS TO CLASH WITH BMS PERSPECTIVE
//	/**
//	 * Switch to UML-B perspective
//	 * 
//	 */
//	private void umlbPerspective() {
//		// Switch to umlb simulation perspective.
//		final IWorkbench workbench = PlatformUI.getWorkbench();
//		try {
//			workbench.showPerspective(SimPerspective.PERSPECTIVE_ID, workbench.getActiveWorkbenchWindow());
//		} catch (WorkbenchException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	
	///////////////////////////////////
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Button btnTickN;
	private Button btnSave;
	private Button btnContinue;
	private Text count;
	private Button btnStop;
	private Button btnReplay;
	private Button btnRestart;
	private Button btnStep;
	private Button btnInd;
	private Table methodsTable;
	private Composite container;
	private String countField = "5";
	private Clock clock = Clock.getInstance();
	private Operation manuallySelectedOp = null;
	private int historyPosition=0;
	
	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 * @return 
	 */
	@Override
	public Control createStatePartControl(Composite parent) {			
		OracleHandler.getOracle().restart(getSite().getShell(), "UML-B", machine);
		
		if (OracleHandler.getOracle().isPlayback()) statusText = "Playback";
		else statusText = "Recording";
		
		container = toolkit.createComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE );
		toolkit.paintBordersFor(container);
		container.setLayout(new FormLayout());
		
		Group buttonGroup = createButtonGroup();
		
		{
			methodsTable = new Table(container, SWT.BORDER	| SWT.FULL_SELECTION);
			methodsTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					// Select operation for later execution
					
					// Manual selection of events is disabled during playback
					if (OracleHandler.getOracle()!=null && OracleHandler.getOracle().isPlayback()){
						MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setText("Error - Cannot Execute Event");
						mbox.setMessage("Cannot select events manually while playback is in progress.");
						mbox.open();
						return;
					}
					
					TableItem selected = methodsTable.getItem(methodsTable.getSelectionIndex());
					manuallySelectedOp = Utils.findOperation(selected.getText(0));
				}
				
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// Execute the selected operation (as a big step)
					
					// Manual selection of events is disabled during playback
					if (OracleHandler.getOracle()!=null && OracleHandler.getOracle().isPlayback()){
						MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setText("Error - Cannot Execute Event");
						mbox.setMessage("Cannot select events manually while playback is in progress.");
						mbox.open();
						return;
					}
					
					TableItem selected = methodsTable.getItem(methodsTable.getSelectionIndex());
					manuallySelectedOp = Utils.findOperation(selected.getText(0));
					try {
						bigStep();
					} catch (ProBException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
			FormData fd_methodsTable = new FormData();
			//fd_methodsTable.width=500;
			fd_methodsTable.left = new FormAttachment(buttonGroup, 10);
			fd_methodsTable.right = new FormAttachment(100, -5);
			fd_methodsTable.top = new FormAttachment(0, 5);
			fd_methodsTable.bottom = new FormAttachment(100, -5);
			methodsTable.setLayoutData(fd_methodsTable);

			String[] title = {"Enabled External Operations"};
			methodsTable.setToolTipText(title[0]);
			
			toolkit.adapt(methodsTable);
			toolkit.paintBordersFor(methodsTable);
			methodsTable.setHeaderVisible(true);
			methodsTable.setLinesVisible(true);
			
			methodsTable.pack();

		}
		initializeToolBar();
		initializeMenu();
		setup(); 

		return container;
	}
	
	/**
	 * creates new button group - note this is called to update state as well as initial setup
	 * @return 
	 */
	private Group createButtonGroup() {
		Group buttonGroup =  new Group(container, SWT.BORDER);
		FormData fd_buttonGroup = new FormData();
		fd_buttonGroup.top = new FormAttachment(0, 5);
		//fd_buttonGroup.right = new FormAttachment(100, -700);
		buttonGroup.setLayoutData(fd_buttonGroup);
		toolkit.adapt(buttonGroup);
		toolkit.paintBordersFor(buttonGroup);
		buttonGroup.setLayout(null);
		{	//INDICATOR - not a button
			btnInd = new Button(buttonGroup, SWT.NONE);
			btnInd.setBounds(10, 10, 110, 25);
			toolkit.adapt(btnInd, true, true);
			btnInd.setText("Recording");
			btnInd.setBackground(red);
		}
		{	//BIG STEP
			btnTickN = new Button(buttonGroup, SWT.NONE);
			btnTickN.setBounds(10, 40, 85, 25);
			btnTickN.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					try {
						bigStep();
					} catch (ProBException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
			toolkit.adapt(btnTickN, true, true);
			btnTickN.setText("Big Step");
		}
		{	//SML STEP
			btnStep = new Button(buttonGroup, SWT.NONE);
			btnStep.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					singleStep();
				}
			});
			btnStep.setBounds(10, 70, 85, 25);
			toolkit.adapt(btnStep, true, true);
			btnStep.setText("Sml Step");
		}
		{	//RUN FOR
			btnContinue = new Button(buttonGroup, SWT.NONE);
			btnContinue.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					try {
						runForTicks();
					} catch (ProBException e1) {
						e1.printStackTrace();
					}
				}
			});
			btnContinue.setBounds(10, 100, 80, 25);
			toolkit.adapt(btnContinue, true, true);
			btnContinue.setText("Run For");
		}
		{	//Tick Count
			count = new Text(buttonGroup, SWT.BORDER);
			count.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					if (e.widget instanceof Text) {
						String newText = ((Text) e.widget).getText();
						count.setText(newText);
						countField = newText;
						count.pack();
					}
				}
			});
			count.setBounds(80, 100, 30, 20);
			count.setText(countField);
			toolkit.adapt(count, true, true);
		}

		{	//RESTART
			btnRestart = new Button(buttonGroup, SWT.NONE);
			btnRestart.setBounds(10, 130, 85, 25); 	//92, 10, 70, 25);
			btnRestart.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {	
						clock.reset();
						historyPosition=0;
						if (OracleHandler.getOracle().isPlayback()){
							OracleHandler.getOracle().stopPlayback(false);
							OracleHandler.getOracle().startPlayback(true);
							statusText = "PlayBack";
						}else{
							OracleHandler.getOracle().stopRecording(false);
							OracleHandler.getOracle().startRecording();	
							statusText = "Recording";
						}
						updateModeIndicator();
						AnimationManager.restartAnimation(mchRoot);
				}
			});
			toolkit.adapt(btnRestart, true, true);
			btnRestart.setText("Restart");
		}
		{	//SAVE
			btnSave = new Button(buttonGroup, SWT.NONE);
			btnSave.setBounds(10, 160, 85, 25); //160, 10, 70, 25);
			btnSave.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					OracleHandler.getOracle().saveRecording();
					if (!"Saved".equals(statusText)) oldStatusText = statusText;
					statusText = "Saved";
					updateModeIndicator();
				}
			});
			toolkit.adapt(btnSave, true, true);
			btnSave.setText("Save");
		}
		{	//REPLAY
			btnReplay = new Button(buttonGroup, SWT.NONE);
			btnReplay.setBounds(10, 190, 85, 25); //160, 41, 70, 25);
			btnReplay.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					clock.reset();
					historyPosition=0;
					if (!OracleHandler.getOracle().isPlayback()){
						OracleHandler.getOracle().stopRecording(false);
						OracleHandler.getOracle().startPlayback(false);
					}
					statusText = "Playback";
					updateModeIndicator();
					AnimationManager.restartAnimation(mchRoot);
				}
			});
			btnReplay.setText("Replay");
			toolkit.adapt(btnReplay, true, true);

		}
		{	//STOP
			btnStop = new Button(buttonGroup, SWT.NONE);
			btnStop.setBounds(10, 220, 85, 25); //160, 72, 70, 25);
			btnStop.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {				
					if (OracleHandler.getOracle().isPlayback()){
						OracleHandler.getOracle().stopPlayback(false);
						statusText = "Recording";
					}
					updateModeIndicator();
				}
			});

			toolkit.adapt(btnStop, true, true);
			btnStop.setText("Stop");
		}

		updateModeIndicator();
		return buttonGroup;
	}

	private void updateModeIndicator() {
		if (!OracleHandler.getOracle().isPlayback()) {
			btnInd.setText(getStatusText());
			btnInd.setBackground(red);
			btnInd.setForeground(red);
		}else {
			btnInd.setText(getStatusText());
			btnInd.setBackground(blue);
			btnInd.setForeground(blue);
		}
	}
	
	
	////////////////////////////////////////////
	
	// implements the big step behaviour where we 
	// fire the next operation and then run to completion of all internal operations
	private boolean bigStep() throws ProBException {	
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
	private void singleStep(){
		Operation op = findNextOperation();
		executeOperation(op, false);
	}
	
	// implements the run behaviour where we take the selected number of big steps
	// (when not in playback mode we stop when a non-deterministic choice is available)  <<<<<<<<<< DISABLED FOR NOW - WHICH IS BETTER?
	private boolean runForTicks() throws ProBException {
		if (inSetup()) return false;	
		//Animator animator = Animator.getAnimator();
		final int endTime = clock.getValueInt()+Integer.valueOf(count.getText());
		boolean progress = true;
		while (clock.getValueInt() < endTime && progress) {
//			if (!OracleHandler.getOracle().isPlayback() && nonDeterministicChoice()) { //InClass()){
//				adviseUser("Run terminated after reaching non-deterministic choice");
//				return false;
//			}else{
				progress = bigStep();
//			}
		}
		if (!progress) {
			messageUser("Run terminated due to lack of progress.");
		}
		return progress;
	}

	/*
	 * display message to the user
	 */
	private void messageUser(String message) {
		MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_INFORMATION | SWT.OK);
		mbox.setText("Continue Terminated Message");
		mbox.setMessage(message);
		mbox.open();
	}
	
	/*
	 * check whether the context needs to be set up
	 */
	private boolean inSetup(){
		List<Operation> enabledOperations = Animator.getAnimator().getCurrentState().getEnabledOperations();
		for (Operation op : enabledOperations){
			if ("SETUP_CONTEXT".equals(op.getName()) ){
				messageUser("Use Step button to execute SETUP_CONTEXT");
				return true;
			}
		}
		return false;
	}
	
	/*
	 * run the context set-up operation if enabled
	 */
	private boolean setup(){
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
//	 EXECUTING INITIALISATION AUTOMATICALLY DOES NOT WORK WITH REPLAY 
//			enabledOperations = animator.getCurrentState().getEnabledOperations();	
//			for (Operation op : enabledOperations){
//				if ("INITIALISATION".equals(op.getName()) ){
//					try {
//						bigStep(op);
//						ret=true;
//					} catch (ProBException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
		}
		return ret;
	}

/**
 * called by ProB when the state has changed
 * Since the order of notifications of state changes can vary we use the history 
 * and only update when the history contains information past the trace point of the last update
 * The oracle is only updated for external operations.
 * 
 */
	@Override
	protected void stateChanged(final State activeState, final Operation operation) {
		if (machine==null) 
			return;
		History history = Animator.getAnimator().getHistory();
		if (historyPosition ==0 || history.getCurrentPosition()>historyPosition) {
			Map<String, Variable> stateMap = Animator.getAnimator().getCurrentState().getValues();
			createButtonGroup();
			
			{	//update the enabled ops table
				if (methodsTable == null) return;
				State currentState = Animator.getAnimator().getCurrentState();
				List<Operation> enabledOps = currentState.getEnabledOperations();
				methodsTable.removeAll();
				// for each enabled operation in the ProB model
				int select = -1;
				int j=0;
				for(Operation op: enabledOps){
					if (Utils.isExternal(Utils.findEvent(op.getName(), machine))) {
						TableItem tableItem = new TableItem(methodsTable, SWT.NULL);
						String[] rowString = {Utils.operationInStringFormat(op)}; 
						tableItem.setText(rowString);
						if (op==manuallySelectedOp || Utils.isNextOp(op)) {
							select = j;
						}
						j++;
					}
				}
				if (select>-1) methodsTable.select(select);
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
	
	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		@SuppressWarnings("unused")
		IMenuManager manager = getViewSite().getActionBars().getMenuManager();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			//waitingForOperation=null;
			return false;
		}
		if ("Saved".equals(statusText)) statusText = oldStatusText;
		return true;
	}
	

}
