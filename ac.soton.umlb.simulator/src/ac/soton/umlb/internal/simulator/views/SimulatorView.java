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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
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
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.persistence.EMFRodinDB;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import ac.soton.eventb.classdiagrams.Class;
import ac.soton.eventb.classdiagrams.ClassMethod;
import ac.soton.eventb.classdiagrams.Classdiagram;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;
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
		if (animator==null) {
			animator = Animator.getAnimator();
		}
		return animator;
	}
	
	
	public void restartAnimator(){
		try {
			project.refreshLocal(IResource.DEPTH_ONE, null);
			//restart animator
			LanguageDependendAnimationPart ldp = getAnimator().getLanguageDependendPart();
			if (ldp!= null)
					ldp.reload(getAnimator());
			
			BMSStarter.restartBMS(bmsFiles, getAnimator());
			umlbPerspective();
			
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
		project = mchRoot.getRodinProject().getProject();
		String machineName = mchRoot.getComponentName();
		historyPosition=0;
		clock.reset();
		eventPriorities.clear();
		eventInternal.clear();
		eventMap.clear();
		machine = null;
		stateMachines.clear();
		bmsFiles.clear();

		// start ProB animator
		System.out.println("Starting ProB for " + machine);
		try {
			project.refreshLocal(IResource.DEPTH_ONE, null); // ensure files seen in workspace
			LoadEventBModelCommand.load(getAnimator(), mchRoot);
		} catch (ProBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Find all the state-machines and bmsFiles of the machine
		// (these must come from the editors as each editor has a different local copy)
		for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages()) {   //   activeWorkbenchWindow.getPages()) {
			for (IEditorReference editorRef : page.getEditorReferences()) {
				IEditorPart editor = editorRef.getEditor(true);
				if (editor instanceof StatemachinesDiagramEditor) {
					Statemachine statemachine = (Statemachine) ((StatemachinesDiagramEditor) editor).getDiagram().getElement();
					if (mchRoot.equals(getEventBRoot(statemachine))) {
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
	    				if (bmsMachineName.startsWith(machineName) && project.equals(bmsproject)){
	    					if (!bmsFiles.contains(bmspf)) bmsFiles.add(((IFile)bmspf));
	    				}
	    			}
		    	}

			}
		}

		if (stateMachines.size() != 0) {
			machine = (Machine) stateMachines.get(0).getContaining(MachinePackage.Literals.MACHINE);
		}else {
			EMFRodinDB emfRodinDB = new EMFRodinDB();
			machine = (Machine) emfRodinDB.loadEventBComponent(mchRoot);
		}
		DiagramAnimator diagramAnimator = DiagramAnimator.getAnimator();
		try {
			diagramAnimator.start(machine, stateMachines, mchRoot, bmsFiles);
		} catch (ProBException e) {
			e.printStackTrace();
		}
		
		restartAnimator();

		//initialise oracle in record mode
		getOracle().initialise(machine);
	}
	
	
	
	/**
	 * Switch to UML-B perspective
	 * 
	 */
	private void umlbPerspective() {
		// Switch to umlb simulation perspective.
		final IWorkbench workbench = PlatformUI.getWorkbench();
		try {
			workbench.showPerspective(SimPerspective.PERSPECTIVE_ID, workbench.getActiveWorkbenchWindow());
		} catch (WorkbenchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static IEventBRoot getEventBRoot(EventBElement element) {
		Resource resource = element.eResource();
		if (resource != null && resource.isLoaded()) {
			IFile file = WorkspaceSynchronizer.getFile(resource);
			IRodinProject rodinProject = RodinCore.getRodinDB()
					.getRodinProject(file.getProject().getName());
			IEventBRoot root = (IEventBRoot) rodinProject.getRodinFile(
					file.getName()).getRoot();
			return root;
		}
		return null;
	}
	
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
	}

	public Group getClassGroup() {
		return classGroup;
	}

	public Table getMethodsTable() {
		return methodsTable;
	}
	
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
					// Select operation for later execution
					
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
					// Execute the selected operation (as a big step)
					
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
					try {
						bigStep();
					} catch (ProBException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
			fd_methodsTable = new FormData();
			fd_methodsTable.width=2000;
			methodsTable.setLayoutData(fd_methodsTable);

			String[] title = {"         Enabled External Operations                 "};

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

			createNewGroups();
		}
		initializeToolBar();
		initializeMenu();
		setup(); 
		return container;
	}

	/**
	 * creates new groups - note this is called to update state as well as initial setup
	 */
	public void createNewGroups() {
		{
			buttonGroup = new Group(container, SWT.BORDER);
			fd_methodsTable.left = new FormAttachment(buttonGroup, 29);
			fd_methodsTable.top = new FormAttachment(0, 10);
			fd_buttonGroup = new FormData();
			fd_buttonGroup.top = new FormAttachment(0, 100);
			fd_buttonGroup.right = new FormAttachment(100, -700);
			buttonGroup.setLayoutData(fd_buttonGroup);
			toolkit.adapt(buttonGroup);
			toolkit.paintBordersFor(buttonGroup);
			buttonGroup.setLayout(null);
			{	//BIG STEP
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
							restartAnimator();
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
						restartAnimator();
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
	// fire the next operation and then run to completion of all internal operations
	private boolean bigStep() throws ProBException {	
		if (inSetup()) return false;	
		Operation op = findNextOperation();
		//Animator animator = Animator.getAnimator();
		boolean progress = true;
		//execute at least one
		progress = executeOperation(op, false);
		//continue executing any internal operations
		while (progress && (op = findNextOperation())!=null && isInternal(op)) {
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
	
	/*
	 * check whether the context needs to be set up
	 */
	private boolean inSetup(){
		List<Operation> enabledOperations = getAnimator().getCurrentState().getEnabledOperations();
		for (Operation op : enabledOperations){
			if ("SETUP_CONTEXT".equals(op.getName()) ){
				adviseUser("Use Step button to execute SETUP_CONTEXT");
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
		if (getAnimator().getCurrentState()==null) {
			ret= false;
		}else {
			List<Operation> enabledOperations = getAnimator().getCurrentState().getEnabledOperations();
			for (Operation op : enabledOperations){
				if ("SETUP_CONTEXT".equals(op.getName())){
					if (oracle.isPlayback() && "SETUP_CONTEXT".equals(oracle.findNextOperation(animator).getName())){
						oracle.consumeNextStep();
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
	
	private boolean nonDeterministicChoiceInClass() {
		int foundComponentOpEnabled;
		// if there is a choice of operations then stop the animation
		List<Operation> enabledOps = getAnimator().getCurrentState().getEnabledOperations();
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
		History history = getAnimator().getHistory();
		if (historyPosition ==0 || history.getCurrentPosition()>historyPosition) {
			Map<String, Variable> stateMap = getAnimator().getCurrentState().getValues();
			UpdateStateLists.getInstance().execute(stateMap);
			UpdateEnabledOpsList.getInstance().execute();
			
			OracleHandler oracle = getOracle();
			if (oracle!=null) {
				for (int i=historyPosition; i<history.getCurrentPosition(); i++) {
					//n.b. history is indexed backwards from the current state.. i.e 0 get current, -1 gets previous etc.
					//(the last operation is in the previous position; current pos never has an operation, it is just the post-state)
					int pos = i-history.getCurrentPosition();
					Operation op = history.getHistoryItem(pos).getOperation();
					
					//we only record external events
					if (op!=null && (isExternal(op) || op.getName().equals("SETUP_CONTEXT"))) {
						oracle.addStepToTrace(machine.getName(), op, clock.getValue());	
						oracle.startSnapshot(clock.getValue());
						//the post state of an operation is in the next history item. 
						stateMap = history.getHistoryItem(pos+1).getState().getValues();
						for (Entry<String, Variable> entry : stateMap.entrySet()) {
							if (!isPrivate(entry.getKey())){
								oracle.addValueToSnapshot(entry.getKey(), entry.getValue().getValue(), clock.getValue());
							}
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
	private Map<String, org.eventb.emf.core.machine.Variable> variableMap = new HashMap<String, org.eventb.emf.core.machine.Variable>();
	private Map<org.eventb.emf.core.machine.Variable, Boolean> privateVariables = new HashMap<org.eventb.emf.core.machine.Variable,Boolean>();	
	
	
	/**
	 * finds the next operation to be executed.
	 * when not in playback mode, it is manually (or randomly) selected from those that are enabled according to priority (internal first)
	 * when in playback mode, external events are given by the next operation in the oracle being replayed,
	 * 
	 * @param animator
	 * @return
	 */
	private Operation findNextOperation() {	
		Operation nextOp = null;
		State currentState = getAnimator().getCurrentState();	
		List<Operation> ops = prioritise(currentState.getEnabledOperations());
		nextOp = 	ops.isEmpty()? null: 
					ops.contains(manuallySelectedOp) ? manuallySelectedOp :
					pickFrom(ops);

		if (oracle.isPlayback() && isExternal(nextOp)){
			nextOp = oracle.findNextOperation(getAnimator());
		}
		return nextOp;
	}

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
			if (oracle.isPlayback() && isExternal(operation)) {
				oracle.consumeNextStep();
			}
			ExecuteOperationCommand.executeOperation(getAnimator(), operation, silent);
			//waitingForOperation = operation;
			if (isExternal(operation)) clock.inc();
		} catch (ProBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//waitingForOperation=null;
			return false;
		}
		if ("Saved".equals(statusText)) statusText = oldStatusText;
		return true;
	}

	public boolean isExternal (Operation op) {
		return isExternal(op.getName());
	}
	
	public boolean isExternal(String name) {
		return isExternal(findEvent(name));
	}
	
	private boolean isExternal(Event ev) {
		if (ev == null) return false;
		return !isInternal(ev);
	}
	
	
	public boolean isInternal(Operation op) {
		return isInternal(op.getName());
	}
	
	public boolean isInternal(String name) {
		return isInternal(findEvent(name));
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
	
	/**
	 * return true if the given variable is private 
	 * @param name
	 * @return
	 */
	private boolean isPrivate(String name) {
		org.eventb.emf.core.machine.Variable var = findVariable(name);
		if (var == null) return false;
		if (!privateVariables.containsKey(var)) {
			String comment = var.getComment();
			privateVariables.put(var,comment!=null && comment.contains("<PRIVATE>"));
		}
		return privateVariables.get(var);
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
	
	
	private org.eventb.emf.core.machine.Variable findVariable(String name) {
		if (!variableMap.containsKey(name)) {
			org.eventb.emf.core.machine.Variable found = null;
			for (org.eventb.emf.core.machine.Variable var : machine.getVariables()) {
				if (name.equals(var.getName())) {
					found = var;
				}
			}
			variableMap.put(name, found);
		}
		return variableMap.get(name);
	}

	public boolean isNextOp(Operation op) {
		return op==manuallySelectedOp || 
				(oracle.isPlayback() && op==oracle.findNextOperation(getAnimator()));
	}



}
