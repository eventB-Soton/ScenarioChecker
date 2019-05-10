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
package ac.soton.umlb.internal.simulator.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CBanner;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.persistence.EMFRodinDB;

import ac.soton.eventb.classdiagrams.Class;
import ac.soton.eventb.classdiagrams.ClassMethod;
import ac.soton.eventb.classdiagrams.Classdiagram;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditor;
import ac.soton.umlb.internal.simulator.BMSStarter;
import ac.soton.umlb.internal.simulator.Clock;
import ac.soton.umlb.internal.simulator.OracleHandler;
import ac.soton.umlb.internal.simulator.UpdateEnabledOpsList;
import ac.soton.umlb.internal.simulator.UpdateStateLists;
import ac.soton.umlb.internal.simulator.perspectives.SimPerspective;
import de.bmotionstudio.gef.editor.BMotionStudioEditor;
import de.prob.core.Animator;
import de.prob.core.LanguageDependendAnimationPart;
import de.prob.core.command.ExecuteOperationCommand;
import de.prob.core.command.LoadEventBModelCommand;
import de.prob.core.domainobjects.History;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.core.domainobjects.Variable;
import de.prob.exceptions.ProBException;
import de.prob.ui.StateBasedViewPart;
import swing2swt.layout.FlowLayout;


public class SimulatorView extends StateBasedViewPart {
	
	public static final String ID = "ac.soton.umlb.internal.simulator.views.SimulatorView"; //$NON-NLS-1$
	private static final String BMOTION_STUDIO_EXT = "bmso";
	
	private static SimulatorView simulator = null;
	public SimulatorView() {
		simulator = this;
	}

	public static SimulatorView getSimulator() {
		if (simulator==null)
			simulator = new SimulatorView();
		return simulator;
	}

	private Animator animator;
	public Animator getAnimator() {
		return animator;
	}
	public void restartAnimator(){
		try {
			project.refreshLocal(IResource.DEPTH_ONE, null);
			//restart animator
			LanguageDependendAnimationPart ldp = animator.getLanguageDependendPart();
			if (ldp!= null)
					ldp.reload(Animator.getAnimator());
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ProBException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	private String statusText;
	private String oldStatusText;

	public String getStatusText() {
		if (statusText == null) statusText = "NULL";
		return statusText;
	}

	private OracleHandler oracle = null;
	public OracleHandler getOracle(){
		if (oracle == null) {
			oracle = OracleHandler.getOracle();
		}
		return oracle;
	}
	
	private IProject project;
	private Machine machine;
	public Machine getMachine(){
		return machine;
	}
	List<Statemachine> stateMachines = new ArrayList<Statemachine>(); 
	List<IFile> bmsFiles = new ArrayList<IFile>();


	
	public void initialise(IMachineRoot mchRoot) {
		//load machine as EMF
		EMFRodinDB emfRodinDB = new EMFRodinDB();
		machine = (Machine) emfRodinDB.loadEventBComponent(mchRoot);		
		project = mchRoot.getRodinProject().getProject();
		historyPosition=0;
		eventPriorities.clear();
		eventInternal.clear();
		eventMap.clear();

		// Find all the statemachines of the machine
		// (these must come from the editors as each editor has a different
		// local copy)
		for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages()) {   //   activeWorkbenchWindow.getPages()) {
			for (IEditorReference editorRef : page.getEditorReferences()) {
				IEditorPart editor = editorRef.getEditor(true);
				if (editor instanceof StatemachinesDiagramEditor) {
					Statemachine statemachine = (Statemachine) ((StatemachinesDiagramEditor) editor).getDiagram().getElement();
					if (machine.equals(statemachine.getContaining(MachinePackage.Literals.MACHINE))){

						if (editor.isDirty()) {
							editor.doSave(new NullProgressMonitor());
						}
						stateMachines.add(statemachine);

						// let the editor know that we are animating so that it
						// doesn't try to save animation artifacts
						((StatemachinesDiagramEditor) editor).startAnimating();
					}
				}
				
	    		//also look for BMotionStudio editors on the same machine
	    		if (editor instanceof BMotionStudioEditor) {
	    			BMotionStudioEditor bmsEditor = (BMotionStudioEditor) editor;
	    			Object bmspf = bmsEditor.getVisualization().getProjectFile();
	    			if (bmspf instanceof IFile && BMOTION_STUDIO_EXT.equals(((IFile)bmspf).getFileExtension())){
		    			String bmsMachineName = bmsEditor.getVisualization().getMachineName();
		    			IProject bmsproject = ((IFile)bmspf).getProject();
	    				if (bmsMachineName.startsWith(machine.getName()) && project.equals(bmsproject)){
	    					if (!bmsFiles.contains(bmspf)) bmsFiles.add(((IFile)bmspf));
	    				}
	    			}
		    	}

			}
		}
		
		// start ProB animator
		System.out.println("Starting ProB for " + machine);
		animator = Animator.getAnimator();
		try {
			project.refreshLocal(IResource.DEPTH_ONE, null); // ensure files seen in workspace
			LoadEventBModelCommand.load(animator, mchRoot);
		} catch (ProBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//start any BMS visualisations
		BMSStarter.restartBMS(bmsFiles, animator);
		//switch to UML-B simulator perspective
		umlbPerspective();
		//initialise oracle in record mode
		getOracle().initialise(machine);
	}
	
	
	/**
	 * Switch to UML-B perspective
	 * 
	 */
	private void umlbPerspective() {
		// Switch to umlb simulation perspective.
		final IWorkbench workbench = PlatformUI.getWorkbench(); //activeWorkbenchWindow.getWorkbench(); //
		try {
			workbench.showPerspective(SimPerspective.PERSPECTIVE_ID, workbench.getActiveWorkbenchWindow());   //activeWorkbenchWindow);
		} catch (WorkbenchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Button btnTickN;
	private Button btnSave;
	private Button btnContinue;
	private Text count;
	private Button btnStop;
	private Button btnReplay;
	private Button btnRestart;
	private Button btnStep;
	private Table methodsTable;
	private Composite container;
	private Composite parent;

	private String countField = "5";

	public Composite getParent() {
		return parent;
	}

	public Composite getContainer() {
		return container;
	}

	private Group buttonGroup;
	private Group timeGroup;
	private Table statusTable;
	private Group classGroup;
	private Group associationGroup;
	private FormData fd_classGroup;
	private FormData fd_associationGroup;
	private FormData fd_timeGroup;
	private Clock clock = Clock.getInstance();

	public Group getAssociationGroup() {
		return associationGroup;
	}

	public Group getTimeGroup() {
		return timeGroup;
	}
	
	public void updateStatusTable() {
		if (statusTable != null) statusTable.dispose();
		statusTable = new Table(timeGroup, SWT.BORDER | SWT.FULL_SELECTION);
		toolkit.adapt(statusTable);
		toolkit.paintBordersFor(statusTable);
		statusTable.setHeaderVisible(true);
		statusTable.setLinesVisible(false);
		TableColumn col = new TableColumn(statusTable, SWT.NULL);
		col.setText(getStatusText());
		col.pack();
		timeGroup.layout();
//		clockText.setText(clock.getValue()); trying to get a text field in the button group to update but it won't
//		clockText.redraw();
//		clockText.update();
//		buttonGroup.update();
	}

	public Group getClassGroup() {
		return classGroup;
	}

	public Table getMethodsTable() {
		return methodsTable;
	}


	private Operation manuallySelectedOp = null;
//	private Operation waitingForOperation;
//	private List<Operation> queue = new ArrayList<Operation>();
	private int historyPosition=0;
	
	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 * @return 
	 */
	@Override
	public Control createStatePartControl(Composite parent) {			
		this.parent = parent;
		getOracle().restart(getSite().getShell(), "UML-B", machine);
		
		if (oracle.isPlayback()) statusText = "Playback";
		else statusText = "Recording";
		
		container = toolkit.createComposite(parent, SWT.V_SCROLL);
		toolkit.paintBordersFor(container);
		container.setLayout(new FormLayout());
		{
			methodsTable = new Table(container, SWT.BORDER
					| SWT.FULL_SELECTION);
			methodsTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					// Note the selected operation for 
					
					// Manual selection of events is disabled during playback
					if (oracle!=null && oracle.isPlayback()){
						MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setText("Error - Cannot Execute Event");
						mbox.setMessage("Cannot select events manually while playback is in progress.");
						mbox.open();
						return;
					}
					
					TableItem selected = methodsTable.getItem(methodsTable.getSelectionIndex());
					manuallySelectedOp = UpdateEnabledOpsList.getInstance().findOperation(selected.getText(0));

				}
				
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// Execute the selected operation
					
					// Manual selection of events is disabled during playback
					if (oracle!=null && oracle.isPlayback()){
						MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setText("Error - Cannot Execute Event");
						mbox.setMessage("Cannot select events manually while playback is in progress.");
						mbox.open();
						return;
					}
					
					TableItem selected = methodsTable.getItem(methodsTable.getSelectionIndex());
					executeOperation(UpdateEnabledOpsList.getInstance().findOperation(selected.getText(0)), false);
					
				}
			});
			fd_methodsTable = new FormData();
			fd_methodsTable.width=3000;
			methodsTable.setLayoutData(fd_methodsTable);


			String[] title = {"         Enabled Operation                      "};

			int lastColumnIndex = title.length;
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				TableColumn col = new TableColumn(methodsTable, SWT.NULL);
				col.setText(title[loopIndex]);
			}
			
			toolkit.adapt(methodsTable);
			toolkit.paintBordersFor(methodsTable);
			methodsTable.setHeaderVisible(true);
			methodsTable.setLinesVisible(true);
			
			for(int i = 0; i < lastColumnIndex; i++){
				methodsTable.getColumn(i).pack();
			}
//			methodsTable.getColumn(0).pack();
//			methodsTable.getColumn(1).pack(); //setWidth(1000);
//			methodsTable.computeSize(50, 1000);

			createNewGroups();
		}
		initializeToolBar();
		initializeMenu();
		return container;
	}

	public void createNewGroups() {
		{
			buttonGroup = new Group(container, SWT.BORDER);
			fd_methodsTable.left = new FormAttachment(buttonGroup, 29);
			//fd_methodsTable.right = new FormAttachment(buttonGroup, 263, SWT.RIGHT);
			fd_methodsTable.top = new FormAttachment(0, 10);
			fd_buttonGroup = new FormData();
			fd_buttonGroup.top = new FormAttachment(0, 100);
			fd_buttonGroup.right = new FormAttachment(100, -700);
			buttonGroup.setLayoutData(fd_buttonGroup);
			toolkit.adapt(buttonGroup);
			toolkit.paintBordersFor(buttonGroup);
			buttonGroup.setLayout(null);
			{	//BBIG STEP
				btnTickN = new Button(buttonGroup, SWT.NONE);
				btnTickN.setBounds(10, 10, 85, 25);
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
				btnStep.setBounds(10, 41, 85, 25);
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
				btnContinue.setBounds(10, 72, 85, 25);
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
				count.setBounds(90, 72, 30, 20);
				count.setText(countField);
				toolkit.adapt(count, true, true);
			}

			{	//RESTART
				btnRestart = new Button(buttonGroup, SWT.NONE);
				btnRestart.setBounds(92, 10, 70, 25);
				btnRestart.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {	
							clock.reset();
							historyPosition=0;
							if (oracle.isPlayback()){
								oracle.stopPlayback(false);
								oracle.startPlayback(true);
								statusText = "PlayBack";
							}else{
								oracle.stopRecording(false);
								oracle.startRecording();	
								statusText = "Recording";
							}
							updateStatusTable();
							BMSStarter.restartBMS(bmsFiles, animator);
							umlbPerspective();
					}
				});
				toolkit.adapt(btnRestart, true, true);
				btnRestart.setText("Restart");
			}
			{	//SAVE
				btnSave = new Button(buttonGroup, SWT.NONE);
				btnSave.setBounds(160, 10, 70, 25);
				btnSave.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						oracle.saveRecording();
						if (!"Saved".equals(statusText)) oldStatusText = statusText;
						statusText = "Saved";
						updateStatusTable();
					}
				});
				toolkit.adapt(btnSave, true, true);
				btnSave.setText("Save");
			}
			{	//REPLAY
				btnReplay = new Button(buttonGroup, SWT.NONE);
				btnReplay.setBounds(160, 41, 70, 25);
				btnReplay.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						clock.reset();
						historyPosition=0;
						if (!oracle.isPlayback()){
							oracle.stopRecording(false);
							oracle.startPlayback(false);
						}
						statusText = "Playback";
						updateStatusTable();
						BMSStarter.restartBMS(bmsFiles, animator);
						umlbPerspective();
					}
				});
				btnReplay.setText("Replay");
				toolkit.adapt(btnReplay, true, true);

			}
			{	//STOP
				btnStop = new Button(buttonGroup, SWT.NONE);
				btnStop.setBounds(160, 72, 70, 25);
				btnStop.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {				
						if (oracle.isPlayback()){
							oracle.stopPlayback(false);
							statusText = "Recording";
						}
						updateStatusTable();
					}
				});

				toolkit.adapt(btnStop, true, true);
				btnStop.setText("Stop");
			}
		}

		CBanner banner = new CBanner(container, SWT.NONE);
		fd_methodsTable.bottom = new FormAttachment(100, -10);
		FormData fd_banner = new FormData();
		fd_banner.top = new FormAttachment(0, 290);
		fd_banner.left = new FormAttachment(0, 430);
		banner.setLayoutData(fd_banner);
		toolkit.adapt(banner);
		toolkit.paintBordersFor(banner);
		{
			timeGroup = new Group(container, SWT.BORDER);
			timeGroup.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			fd_timeGroup = new FormData();
			fd_timeGroup.right = new FormAttachment(buttonGroup, 0, SWT.RIGHT);
			fd_timeGroup.top = new FormAttachment(methodsTable, 0, SWT.TOP);
			fd_timeGroup.left = new FormAttachment(buttonGroup, 0, SWT.LEFT);
			timeGroup.setLayoutData(fd_timeGroup);
			toolkit.adapt(timeGroup);
			toolkit.paintBordersFor(timeGroup);
		}
		{
			classGroup = new Group(container, SWT.BORDER);
			classGroup.setText("Classes");
			classGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
			fd_classGroup = new FormData();
			fd_classGroup.top = new FormAttachment(0, 10);
			fd_classGroup.left = new FormAttachment(0, 69);
			fd_classGroup.right = new FormAttachment(buttonGroup, -12);
			classGroup.setLayoutData(fd_classGroup);
			toolkit.adapt(classGroup);
			toolkit.paintBordersFor(classGroup);
		}
		{
			associationGroup = new Group(container, SWT.BORDER);
			associationGroup.setText("Associations");
			associationGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
			fd_associationGroup = new FormData();
			fd_associationGroup.top = new FormAttachment(classGroup, 12);
			fd_associationGroup.left = new FormAttachment(0, 67);
			fd_associationGroup.right = new FormAttachment(buttonGroup, -12);
			associationGroup.setLayoutData(fd_associationGroup);
			toolkit.adapt(associationGroup);
			toolkit.paintBordersFor(associationGroup);
		}
	}

	////////////////////////////////////////////
	
	// implements the big step behaviour where we 
	// fire the next operations and then run to completion of all internal operations
	private boolean bigStep() throws ProBException {	
		if (inSetup()) return false;	
		//Animator animator = Animator.getAnimator();
		boolean progress = true;
		//execute at least one
		Operation op = findNextOperation();
		progress = executeOperation(op, false);
		//continue executing any internal operations
		while (progress && (op = findNextOperation())!=null &&
				isInternal(findEvent(op.getName()))) {
			//queue.add(op);
			progress = executeOperation(op, false);
		}
		return progress;
	}
	
	// implements the small step behaviour where we fire one enabled external or internal operation
	private void singleStep(){
		Operation op = findNextOperation();
		executeOperation(op, false);
	}
	
	// implements the run behaviour where we take the selected number of big steps
	// (when not in playback mode we stop when a non-deterministic choice is available)
	private boolean runForTicks() throws ProBException {
		if (inSetup()) return false;	
		//Animator animator = Animator.getAnimator();
		final int endTime = clock.getValueInt()+Integer.valueOf(count.getText());
		boolean progress = true;
		while (clock.getValueInt() < endTime && progress) {
			if (!oracle.isPlayback() && nonDeterministicChoiceInClass()){
				adviseUser("Run terminated after reaching non-deterministic choice");
				return false;
			}else{
				progress = bigStep();
			}
		}
		if (!progress) {
			adviseUser("Run terminated due to lack of progress.");
		}
		return progress;
	}

	/*
	 * display message to the user
	 */
	private void adviseUser(String message) {
		MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_INFORMATION | SWT.OK);
		mbox.setText("Continue Terminated Message");
		mbox.setMessage(message);
		mbox.open();
	}
	
	private boolean inSetup(){
		List<Operation> enabledOperations = animator.getCurrentState().getEnabledOperations();
		for (Operation op : enabledOperations){
			if ("SETUP_CONTEXT".equals(op.getName()) ){
				adviseUser("Use Step button to execute SETUP_CONTEXT");
				return true;
			}
		}
		return false;
	}
	
	private boolean nonDeterministicChoiceInClass() {
		int foundComponentOpEnabled;
		// if there is a choice of operations then stop the animation
		List<Operation> enabledOps = animator.getCurrentState().getEnabledOperations();
		List<String> enabledOpNames = new ArrayList<String>();
		for (Operation op : enabledOps) {
			enabledOpNames.add(op.getName());
		}
		EList<AbstractExtension> exts = machine.getExtensions();
		// go through each extension and find classdiagrams and map to their eventNames.
		for (AbstractExtension ext : exts) {
			if (ext instanceof Classdiagram) {
				Classdiagram classdiagram = (Classdiagram) ext;
				EList<Class> classes = classdiagram.getClasses();
				// iterate through classes
				for (Class class_ : classes) {
					List<String> evtNames = new ArrayList<String>();					
					foundComponentOpEnabled = 0;
					EList<ClassMethod> classMethods = class_.getMethods();
					for (ClassMethod m : classMethods) {
						//if (!(m instanceof External)) {
							EList<Event> elaborates = m.getElaborates();
							for (Event evt : elaborates) {
								evtNames.add(evt.getName());
							}
						//}
					}
					// we now have a list of enabled event names for this class
					for (String evtName : evtNames) {
						if (enabledOpNames.contains(evtName)) {
							foundComponentOpEnabled++;
							// if we have more than one enabled method then advise user and return
							if(foundComponentOpEnabled>1){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
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
		History history = animator.getHistory();
		if (historyPosition ==0 || history.getCurrentPosition()>historyPosition) {
			Map<String, Variable> stateMap = animator.getCurrentState().getValues();
			UpdateStateLists.getInstance().execute(stateMap);
			UpdateEnabledOpsList.getInstance().execute();
			
			OracleHandler oracle = getOracle();
			if (oracle!=null) {
				for (int i=historyPosition; i<history.getCurrentPosition(); i++) {
					//n.b. history is indexed backwards from the current state.. i.e 0 get current, -1 gets previous etc.
					//(the last operation is in the previous position; current pos never has an operation, it is just the post-state)
					int pos = i-history.getCurrentPosition();
					Operation op = history.getHistoryItem(pos).getOperation();
					
					//TODO: should we only record external events? If so need to change findNextOperation
					if (op!=null) { //&& !isExternal(findEvent(op.getName()))) {
						oracle.addStepToTrace(machine.getName(), op, clock.getValue());	
						oracle.startSnapshot(clock.getValue());
						//the post state of an operation is in the next history item. 
						stateMap = history.getHistoryItem(pos+1).getState().getValues();
						for (Entry<String, Variable> entry : stateMap.entrySet()) {
							oracle.addValueToSnapshot(entry.getKey(), entry.getValue().getValue(), clock.getValue());
						}
						oracle.stopSnapshot(clock.getValue());
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

	
	private static final Random random = new Random();
	private FormData fd_methodsTable;
	private FormData fd_buttonGroup;
	private Map<Event, Integer> eventPriorities = new HashMap<Event,Integer>();
	private Map<Event, Boolean> eventInternal = new HashMap<Event,Boolean>();
	private Map<String, Event> eventMap = new HashMap<String, Event>();
	
	/**
	 * finds the next operation to be executed.
	 * when in playback mode, it is the next operation in the oracle being replayed,
	 * when not in playback mode, it is randomly selected from those that are enabled.
	 * 
	 * @param animator
	 * @return
	 */
	private Operation findNextOperation() {	
		Operation nextOp = null;
		if (oracle.isPlayback()){
			
			nextOp = oracle.findNextOperation(animator);
		}else{
			State currentState = animator.getCurrentState();	
			List<Operation> ops = prioritise(currentState.getEnabledOperations());
			nextOp = 	ops.isEmpty()? null: 
						ops.contains(manuallySelectedOp) ? manuallySelectedOp :
						ops.get(random.nextInt(ops.size()));
		}
		return nextOp;
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
			Event ev = findEvent(op.getName());
			priority = ev==null? -1 : getPriority(ev);

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
			if (oracle.isPlayback()) {
				oracle.consumeNextStep();
			}
			ExecuteOperationCommand.executeOperation(animator, operation, silent);
			//waitingForOperation = operation;
			if (isExternal(findEvent(operation.getName()))) clock.inc();
		} catch (ProBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//waitingForOperation=null;
			return false;
		}
		if ("Saved".equals(statusText)) statusText = oldStatusText;
		return true;
	}

	private boolean isExternal(Event ev) {
		if (ev == null) return false;
		return !isInternal(ev);
	}
	
	/**
	 * return true if the given event is internal
	 * 
	 * @param event
	 * @return
	 */
	private boolean isInternal(Event ev) {
		if (ev == null) return false;
		if (!eventInternal.containsKey(ev)) {
			String comment = ev.getComment();
			eventInternal.put(ev,comment!=null && comment.contains("<INTERNAL>"));
		}
		return eventInternal.get(ev);
	}
	
	private Integer getPriority(Event ev) {
		if (ev == null) return -1;
		if (!eventPriorities.containsKey(ev)) {
			String priString = ev.getComment();
			Integer pri = Integer.MAX_VALUE;
			if (priString!=null && priString.contains("<PRIORITY=")) {
				priString = priString.substring(priString.indexOf("<PRIORITY=")+10);
				int i = priString.indexOf(">"); 
				if (i>0)  priString =  priString.substring(0,i);
				pri = Integer.valueOf(priString);
			}
			eventPriorities.put(ev, pri);
		}
		return eventPriorities.get(ev);
	}
	
	
	/**
	 * find an event in the machine with the given name
	 * 
	 * @param event name
	 * @return
	 */
	private Event findEvent(String name) { 
		if (!eventMap.containsKey(name)) {
			Event found = null;
			for (Event ev : machine.getEvents()) {
				if (name.equals(ev.getName())) {
					found = ev;
				}
			}
			eventMap.put(name, found);
		}
		return eventMap.get(name);
	}

}
