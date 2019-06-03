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

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.workspace.AbstractEMFOperation;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eventb.emf.core.CorePackage;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBObject;

import ac.soton.eventb.emf.oracle.Entry;
import ac.soton.eventb.emf.oracle.Oracle;
import ac.soton.eventb.emf.oracle.OracleFactory;
import ac.soton.eventb.emf.oracle.OraclePackage;
import ac.soton.eventb.emf.oracle.Run;
import ac.soton.eventb.emf.oracle.Snapshot;
import ac.soton.eventb.emf.oracle.Step;
import ac.soton.umlb.internal.simulator.views.SimulatorView;
import ac.soton.umlb.simulator.Activator;
import de.prob.core.Animator;
import de.prob.core.domainobjects.Operation;



public class OracleHandler {
	
	private final static String oracleExtension = "oracle";
	private final static String goldOracleExtension = "gold_"+oracleExtension;
	private boolean debug = true;


	
	/**
	 * Singleton
	 */
	private static OracleHandler instance = null;
	
	public static OracleHandler getOracle(){
		if (instance == null){
			instance = new OracleHandler();
		}
		return instance;
	}
	
	private OracleHandler(){
		editingDomain = TransactionalEditingDomain.Factory.INSTANCE.createEditingDomain();
	}

	
	/**
	 * SET UP
	 */
	
	public void initialise(EventBObject eventBObject){
		modelName = ((EventBNamed)eventBObject.getContaining(CorePackage.Literals.EVENT_BNAMED_COMMENTED_COMPONENT_ELEMENT)).getName();
		try {
			folder = getOracleFolder(eventBObject);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if (debug) System.out.println("Oracle initialisation FAILED");
		}
		playback = false;
	}
	
	public boolean restart(Shell shell, String name, EventBObject eventBObject){
		if (eventBObject==null){
			if (debug)System.out.println("Oracle initialisation FAILED due to no machine");
			return false;
		}else{
			modelName = ((EventBNamed)eventBObject.getContaining(CorePackage.Literals.EVENT_BNAMED_COMMENTED_COMPONENT_ELEMENT)).getName();
			try {
				folder = getOracleFolder(eventBObject);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (debug) System.out.println("Oracle initialisation FAILED");
			}
			this.shell = shell;
			oracleName = name;
			for (Resource resource : editingDomain.getResourceSet().getResources()){
				resource.unload();
			}		
			if (playback==true) doStartPlayback();
			else  doStartRecording();
			return true;
		}
	}
	
	/**
	 * RECORDING
	 */
	
	/**
	 * 
	 * @return
	 */
	public void startRecording(){
		//SimulatorView.getSimulator().restartAnimator();
		
	}
	
	private void doStartRecording(){
		if (debug) System.out.println("Oracle start recording");
		currentRecordRun = makeRun();
		currentSnapshot = null;
		return;
	}

	private Run makeRun() {
		Run run = OracleFactory.eINSTANCE.createRun();		
		run.setName(oracleName);
		return run;
	}

	public void startSnapshot(String clockValue) {
		startSnapshot(modelName == null? "<unknown>" : modelName, clockValue);
	}
		
	public void startSnapshot(String machineName, String clockValue) {
		assert (currentSnapshot == null);
		if (debug) System.out.println("Oracle startSnapshot");
		currentSnapshot = OracleFactory.eINSTANCE.createSnapshot();
		currentSnapshot.setClock(clockValue);
		currentSnapshot.setMachine(machineName);
	}

	public void addValueToSnapshot(String name, String value, String timeStamp){
		addValueToSnapshot(name, value, timeStamp, false);
	}
	
	public void addValueToSnapshot(String name, String value, String timeStamp, boolean verbose) {
		assert(currentSnapshot != null);
		assert(currentSnapshot.getClock().equals(timeStamp));
		if (verbose || hasChanged(name,value)){
			currentSnapshot.getValues().put(name, value);
			if (debug) System.out.println("Oracle addtoSnapshot: "+name+" -> "+value);
		}
	}

	private boolean hasChanged(String name, String value) {
		if (currentRecordRun == null) return true;
		EList<Entry> entries = currentRecordRun.getEntries();		
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

	public void stopSnapshot(String value) {
		assert(currentSnapshot != null);
		assert(currentSnapshot.getClock().equals(value));
		assert(currentRecordRun!= null);
		if (debug) System.out.println("Oracle stopSnapshot");
		if (!currentSnapshot.getValues().isEmpty()) {
			if (playback == true){
				Snapshot goldSnapshot = getNextGoldSnapshot();
				if (goldSnapshot!=null)
					currentSnapshot.setResult(compareSnapshots(goldSnapshot, currentSnapshot));
				else{
					currentSnapshot.setResult(false);
				}
			}else{
				currentSnapshot.setResult(true);	//for gold run all snapshots are result = true
			}
			currentRecordRun.getEntries().add(currentSnapshot);
		}
		currentSnapshot = null;
		
	}
	
	private boolean compareSnapshots(Snapshot goldSnapshot, Snapshot newSnapshot) {
		EMap<String, String> newValues = newSnapshot.getValues();
		for (Map.Entry<String, String> goldValue : goldSnapshot.getValues()){
			if (!(newValues.containsKey(goldValue.getKey()) && newValues.get(goldValue.getKey()).equals(goldValue.getValue()))){
				
				//TODO: This used to stop and warn the user when a discrepancy was found
				// Now we record the discrepancy but carry on.
//				//User message
//				MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
//				mbox.setText("Playback Discrepancy");				
//				if (newValues.containsKey(goldValue.getKey())){
//					mbox.setMessage("Different value in playback for variable "+goldValue.getKey()+"\n"+
//							"Original Value = "+goldValue.getValue() +"\n"+
//							"Playback Value ="+newValues.get(goldValue.getKey()));
//				}else{
//					mbox.setMessage("No new value in playback for variable "+goldValue.getKey()+"\n"+
//							"(Original Value = "+goldValue.getValue() +")");
//				}
//				mbox.open();

				return false;
			}
		}
		return true;
	}


	private Snapshot getNextGoldSnapshot() {
		if (currentPlaybackRun == null){return null;}
		EList<Entry> oracleEntries = currentPlaybackRun.getEntries();
		if (snapShotPointer >= oracleEntries.size() || oracleEntries.get(snapShotPointer) instanceof Step){return null;}
		snapShotPointer++;
		return (Snapshot) oracleEntries.get(snapShotPointer-1);
	}

	public Step addStepToTrace(String machineName, Operation operation, String clockValue){
		if (debug) System.out.println("Oracle addStepToTrace: "+operation.getName());
		Step step = OracleFactory.eINSTANCE.createStep();
		step.setName(operation.getName());
		step.getArgs().addAll(operation.getArguments());
		step.setMachine(machineName);
		step.setClock(clockValue);
		currentRecordRun.getEntries().add(step);
		snapShotPointer = currentRecordRun.getEntries().size();	//this keeps the snapshot pointer in synch with the steps
		return step;
	}
	
	public boolean saveRecording(){
		if (debug) System.out.println("Oracle saveRecording ");
		if (currentSnapshot!=null) stopSnapshot(currentSnapshot.getClock());
		try {
			save(currentRecordRun);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean stopRecording(boolean save){
		if (debug) System.out.println("Oracle stopRecording (save = "+save+")");
		if (save) saveRecording();
		return true;
	}

	
	/**
	 * PLAYBACK
	 */
	
	public boolean isPlayback(){
		return playback;
	}
	
	public void startPlayback(boolean repeat){
		playback = true;
		this.repeat = repeat;
		//SimulatorView.getSimulator().restartAnimator();
	}
	
	private void doStartPlayback(){
		assert(playback == false);
		playback = true;
		if (debug) System.out.println("Oracle startPlayback");
		if (currentPlaybackRun == null || repeat==false){
			currentPlaybackRun = getGoldRun();
		}
		//if acquiring gold run failed, revert to recording mode
		if (currentPlaybackRun == null) playback = false;
		
		stepPointer = -1;
		snapShotPointer = -1;
		nextStep = null;
		doStartRecording();
		return ;
	}

//	/**
//	 * returns the index in ops of the next operation according to the Oracle being replayed
//	 * 
//	 * @param animator
//	 * @return index of operation in currently enabled operations
//	 * @deprecated - use findNextOperation and look up its index yourself.
//	 */
//	public int selectNextOperation(Animator animator) {
//		List<Operation> ops = animator.getCurrentState().getEnabledOperations();
//		return ops.indexOf(findNextOperation(animator));
//	}
	
	/**
	 * This consumes the next step 
	 * so that the next find will return a different operation.
	 * 
	 * @param animator
	 * @return
	 */
	public void consumeNextStep() {
		nextStep = null;
	}
	
	/** 
	 * returns the next operation according to the Oracle being replayed
	 * this does not change the Oracle state so can be called repeatedly
	 * 
	 * 
	 * 
	 * @param animator
	 * @return operation
	 */
	public Operation findNextOperation(Animator animator) {
		
		List<Operation> ops = animator.getCurrentState().getEnabledOperations();
		if (!hasNextStep() && isPlayback()){
			stopPlayback(false);
			MessageBox mbox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
			mbox.setText("Playback Finished");
			mbox.setMessage("Playback has completed the trace");
			mbox.open();
			return null;
		}
		
		if (nextStep!=null){
			for (Operation op : ops) {
				if (op.getName().equals(nextStep.getName())) {
					boolean thisOne;
					if (nextStep.getArgs().size() == op.getArguments().size()){
						thisOne = true;
						Iterator<String> it = nextStep.getArgs().iterator();
						for (String arg : op.getArguments()){
							if (!(it.hasNext() && arg.equals(it.next()))){
								thisOne = false;
								break;
							}
						}						
					}else{
						thisOne = false;
					}
					if (thisOne) {
						if (debug) System.out.println("Oracle selected next operation = "+op.getName());
						return op;
					}
				}
			}
		}
		MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		mbox.setText("Playback Discrepancy");
		String message = "?";
		if (nextStep ==null){
			message = "Original operation is null";
		}else{
			message = "Original operation is not enabled in playback. \n Operation = "+nextStep.getName();
			if (nextStep.getArgs().size()>0){
				message = message+"\n"+"Arguments = "+nextStep.getArgs() ;
			}
		}
		mbox.setMessage(message);
		mbox.open();

		if (debug) System.out.println("Oracle select next operation FAILED");
		return null; 
	}
	
	private boolean hasNextStep(){
		assert(playback==true);
		if (nextStep == null) {
			EList<Entry> oracleEntries = currentPlaybackRun.getEntries();
			int oldStepPointer = stepPointer;
			do{
				stepPointer = stepPointer+1;
			}while (stepPointer < oracleEntries.size() && (!(oracleEntries.get(stepPointer) instanceof Step) ||
					SimulatorView.getSimulator().isInternal(((Step)oracleEntries.get(stepPointer)).getName()))) ;
			if (stepPointer < oracleEntries.size()){
				nextStep = (Step)oracleEntries.get(stepPointer);
				return true;
			} else {
				stepPointer = oldStepPointer;
				return false;
			}
		}
		return true;
	}
	
	public boolean stopPlayback(boolean save){
		assert(playback==true);
		if (debug) System.out.println("Oracle stopPlayback (save = "+save+")");
		if (save) saveRecording();
		playback = false;
		return true;
	}
	
	////////////////////////////////
	/////////////internal//////////
	////////////////////////////////
	private Shell shell = null;

	private String modelName = null;
	private IFolder folder = null;
	private TransactionalEditingDomain editingDomain;
	private String oracleName = null;
	
	//RECORDING
	// the run being recorded - this should not be added to the oracle runs until save
	private Run currentRecordRun = null;
	// the snapshot being recorded - add to the currentRecordRun when the snapshot is stopped
	private Snapshot currentSnapshot = null;
	
	//PLAYBACK
	//flag indicating that same gold should be played back again
	private boolean repeat = false;
	// flag indicating playback in progress
	private boolean playback = false;
	// the Run being played back - this should not be modified
	private Run currentPlaybackRun = null;
	// this is a cache of the next step to be taken. getNextStep returns this if it is not null.
	// It should be cleared to null when it has successfully been used so that getnextStep is forced to retrieve another step.
	private Step nextStep= null;	
	// pointer into the collection of playback Entries - used to find the next step to be taken
	private Integer stepPointer = -1; 
	
	// pointer into the collection of Entries - used to find the next Snapshot 
	// (when a step is recorded, this is moved to the entry after that step).
	private Integer snapShotPointer = -1;

	private IFolder oracleFolder = null;

	private String getTimeStamp() {
		String timestamp = "";
		Calendar calendar =Calendar.getInstance();
		timestamp = timestamp + twoDigits(calendar.get(Calendar.YEAR));
		timestamp = timestamp + twoDigits(calendar.get(Calendar.MONTH)+1);
		timestamp = timestamp + twoDigits(calendar.get(Calendar.DAY_OF_MONTH));
		timestamp = timestamp + twoDigits(calendar.get(Calendar.HOUR_OF_DAY));
		timestamp = timestamp + twoDigits(calendar.get(Calendar.MINUTE));
		timestamp = timestamp + twoDigits(calendar.get(Calendar.SECOND));
		return timestamp;
	}

	private String twoDigits(int integer) {
		String ret = Integer.toString(integer);
		if (ret.length()<2) ret = "0"+ret;
		return ret;
	}
	
	private Run getGoldRun() {
	try {
	   FileDialog dialog = new FileDialog(shell, SWT.OPEN);
	   dialog.setFilterExtensions(new String [] {"*."+goldOracleExtension, "*."+oracleExtension,"*.*"});
	   dialog.setFilterPath(oracleFolder.getRawLocation().toString());
	   dialog.setText("Select Gold Oracle File to Replay");
	   String rawLocation = dialog.open();
	   if (rawLocation==null) return null;
	   IPath rawPath = new Path(rawLocation);
	   
//	   IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getRawLocation();
//	   IPath workspaceRelativePath = rawPath.makeRelativeTo(workspacePath);   
//	   IPath path = new Path("platform:/resource");
//	   path = path.append(workspaceRelativePath);
//	   URI uri = URI.createURI(path.toString()); 
	   
	   URI uri = URI.createFileURI(rawPath.toString());
	   ResourceSet rset = editingDomain.getResourceSet();
	   Resource resource = rset.getResource(uri, true);	   
	   Run run = loadRun(resource);
	   return run;
	}catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	}
	}
	

	private Run loadRun(Resource resource) throws CoreException{
		try {
			resource.load(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		for (EObject content : resource.getContents()){
			if (content instanceof Oracle){
				for (Run run : ((Oracle)content).getRuns()){
					return run;
				}
			}
		}
		return null;
	}
	
	private void save(Run run) throws CoreException{	
		final Resource resource = getResource(oracleName, getTimeStamp(), !playback);
		final SaveRunCommand saveRunCommand = new SaveRunCommand(editingDomain, resource, shell, run);
		if (saveRunCommand.canExecute()) {	
			// run with progress
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
			try {
				dialog.run(true, true, new IRunnableWithProgress(){
				     public void run(IProgressMonitor monitor) { 
				    	 monitor.beginTask("Saving Oracle", IProgressMonitor.UNKNOWN);
				         try {
				        	 saveRunCommand.execute(monitor, null);
				         } catch (ExecutionException e) {
								e.printStackTrace();			        	 
				         }
				         monitor.done();
				     }
				 });
				//disconnect run from this resource so that we can add more to it
				editingDomain.getResourceSet().getResources().remove(resource);
				TransactionUtil.disconnectFromEditingDomain(resource);
				EcoreUtil.remove(run);	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}

	private Resource getResource(String name, String timestamp, boolean gold) throws CoreException{
		IPath filePath = folder.getFullPath();
		filePath = filePath.append("/"+modelName);
		filePath = filePath.addFileExtension(name);
		filePath = filePath.addFileExtension(timestamp);
		if (gold) filePath = filePath.addFileExtension(goldOracleExtension);
		else filePath = filePath.addFileExtension(oracleExtension);
		IPath path = new Path("platform:/resource");
		path = path.append(filePath);
		URI uri = URI.createURI(path.toString(),true);
		ResourceSet rset = editingDomain.getResourceSet();
		Resource resource = rset.getResource(uri, false);
		if (resource == null){
			resource = editingDomain.getResourceSet().createResource(uri);
		}
		return resource;
	}
	
	/**
	 * This locates (and creates if necessary) a folder called "Oracle" in the same 
	 * container as the model.
	 * The folder is also added to the editing domain's resource set 
	 * 
	 * @return IFolder Oracle
	 * @throws CoreException
	 */

	private IFolder getOracleFolder(EventBObject model) throws CoreException{
		assert (model != null);
		URI uri = EcoreUtil.getURI(model);
		uri = uri.trimFileExtension();
		uri = uri.trimSegments(1);
		uri = uri.appendSegment("Oracle");
		editingDomain.getResourceSet().createResource(uri);
		IPath folderPath = new Path(uri.toPlatformString(true));
		folderPath = folderPath.makeAbsolute();
		oracleFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(folderPath);
		if (!oracleFolder.exists()){
			oracleFolder.create(false, true, null);
		}
		return oracleFolder;
	}
	
	//////////////save command////////////
	
	private static class SaveRunCommand extends AbstractEMFOperation {
		
		Resource resource;
		Run run;

		public SaveRunCommand(TransactionalEditingDomain editingDomain, Resource resource, Shell shell, Run run) {
			super(editingDomain, "what can I say?");
			//setOptions(Collections.singletonMap(Transaction.OPTION_UNPROTECTED, Boolean.TRUE));
			this.resource = resource;
			this.run = run;
		}
		
		@Override
		public boolean canRedo(){
			return false;
		}

		@Override
		public boolean canUndo(){
			return false;
		}

		@Override
		protected IStatus doExecute(IProgressMonitor monitor, IAdaptable info) 	throws ExecutionException {			
			if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof Oracle)){
				resource.getContents().clear();
				Oracle oracle = OracleFactory.eINSTANCE.createOracle();
				resource.getContents().add(oracle);
			}
			Oracle oracle = (Oracle) resource.getContents().get(0);
			//add current recorded run to the oracle
			if (run != null){
				Command addCommand = AddCommand.create(getEditingDomain(), oracle, OraclePackage.Literals.ORACLE__RUNS, run);
				if (addCommand.canExecute()) addCommand.execute();
			}
			final Map<Object, Object> saveOptions = new HashMap<Object, Object>();
			//saveOptions.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
			try {
				resource.save(saveOptions);
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new Status(Status.ERROR, Activator.PLUGIN_ID, "Saving Oracle Failed" , e);
			}
			return new Status(Status.OK, Activator.PLUGIN_ID, "Saving Oracle Succeeded");
		}
	}

}
