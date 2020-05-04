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
package ac.soton.eventb.internal.scenariochecker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eventb.emf.core.CorePackage;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.emf.oracle.Entry;
import ac.soton.eventb.emf.oracle.Oracle;
import ac.soton.eventb.emf.oracle.OracleFactory;
import ac.soton.eventb.emf.oracle.OraclePackage;
import ac.soton.eventb.emf.oracle.Run;
import ac.soton.eventb.emf.oracle.Snapshot;
import ac.soton.eventb.emf.oracle.Step;
import ac.soton.eventb.probsupport.data.Operation_;
import ac.soton.eventb.scenariochecker.Activator;


/**
 * Manages reading and writing to the oracle files
 * 
 * @author cfsnook
 *
 */
public class OracleHandler {
	
	private final static String oracleExtension = "oracle";
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
		shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		machine = (Machine) ((EventBNamed)eventBObject.getContaining(CorePackage.Literals.EVENT_BNAMED_COMMENTED_COMPONENT_ELEMENT));
		try {
			folder = getOracleFolder(eventBObject);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if (debug) System.out.println("Oracle initialisation FAILED");
		}
		playback = false;
	}
	
	public boolean restart(String name, EventBObject eventBObject){
		if (eventBObject==null){
			if (debug)System.out.println("Oracle initialisation FAILED due to no machine");
			return false;
		}else{
			machine = (Machine) ((EventBNamed)eventBObject.getContaining(CorePackage.Literals.EVENT_BNAMED_COMMENTED_COMPONENT_ELEMENT));
			try {
				folder = getOracleFolder(eventBObject);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (debug) System.out.println("Oracle initialisation FAILED");
			}
			oracleName = name;
			for (Resource resource : editingDomain.getResourceSet().getResources()){
				resource.unload();
			}		
			if (playback==true) doStartPlayback();
//			else  doStartRecording();
			return true;
		}
	}
	
	
//	/**
//	 * @param goldSnapshot
//	 * @param newSnapshot
//	 * @return
//	 */
//	private boolean compareSnapshots(Snapshot goldSnapshot, Snapshot newSnapshot) {
//		EMap<String, String> newValues = newSnapshot.getValues();
//		for (Map.Entry<String, String> goldValue : goldSnapshot.getValues()){
//			if (!(newValues.containsKey(goldValue.getKey()) && newValues.get(goldValue.getKey()).equals(goldValue.getValue()))){
//				return false;
//			}
//		}
//		return true;
//	}


	/**
	 * When in playback, returns the next snapshot.
	 * 
	 * If not in playback or a step is next, returns null;
	 * 
	 * @return
	 */
	//	 * This assumes that the stepPointer is pointing to the last executed step
	public Snapshot getNextGoldSnapshot() {
		if (!isPlayback() || currentPlaybackRun == null){return null;}
		EList<Entry> oracleEntries = currentPlaybackRun.getEntries();
		if (stepPointer <0 || stepPointer+1 >= oracleEntries.size() || !(oracleEntries.get(stepPointer+1) instanceof Snapshot))
			{return null;}
		return (Snapshot) oracleEntries.get(stepPointer+1);
	}
	
	/**
	 * PLAYBACK
	 */
	
	/**
	 * Checks whether currently in playback mode 
	 * @return
	 */
	public boolean isPlayback(){
		return playback;
	}
	
	/**
	 * Switch to playback mode
	 * If repeat is true, use the existin oracle file, otherwise a new one will be loaded
	 * 
	 * @param repeat
	 */
	public void startPlayback(boolean repeat){
		playback = true;
		this.repeat = repeat;
	}
	
	/**
	 * 
	 */
	private void doStartPlayback(){
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
//		doStartRecording();
		return ;
	}
	
	/**
	 * This consumes the next step so that the next find will return a different operation.
	 * 
	 * @param animator
	 * @return
	 */
	public void consumeNextStep() {
		nextStep = null;
		snapShotPointer = stepPointer+1;
	}
	
	/** 
	 * Returns the next operation according to the Oracle being replayed
	 * this does not change the Oracle state so can be called repeatedly
	 * 
	 * @return operation
	 */
	public Operation_ findNextOperation() {
		if (!hasNextStep() && isPlayback()){
			stopPlayback();
			return null;
		}
		
		if (nextStep!=null){
			List<String> args = new ArrayList<String>();
			args.addAll(nextStep.getArgs());
			if (debug) System.out.println("Oracle selected next operation = "+nextStep.getName());
			return (new Operation_(nextStep.getName(),args));
			
//			for (Operation_ op : ops) {
//				if (op.getName().equals(nextStep.getName())) {
//					boolean thisOne;
//					if (nextStep.getArgs().size() == op.getArguments().size()){
//						thisOne = true;
//						Iterator<String> it = nextStep.getArgs().iterator();
//						for (String arg : op.getArguments()){
//							if (!(it.hasNext() && arg.equals(it.next()))){
//								thisOne = false;
//								break;
//							}
//						}						
//					}else{
//						thisOne = false;
//					}
//					if (thisOne) {
//						if (debug) System.out.println("Oracle selected next operation = "+op.getName());
//						return op;
//					}
//				}
//			}
		}
//		MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
//		mbox.setText("Playback Discrepancy");
//		String message = "?";
//		if (nextStep ==null){
//			message = "Original operation is null";
//		}else{
//			message = "Original operation is not enabled in playback. \n Operation_ = "+nextStep.getName();
//			if (nextStep.getArgs().size()>0){
//				message = message+"\n"+"Arguments = "+nextStep.getArgs() ;
//			}
//		}
//		mbox.setMessage(message);
//		mbox.open();

		if (debug) System.out.println("Oracle select next operation FAILED");
		return null; 
	}
	
	/**
	 * @return
	 */
	private boolean hasNextStep(){
		assert(playback==true);
		if (nextStep == null) {
			EList<Entry> oracleEntries = currentPlaybackRun.getEntries();
			int oldStepPointer = stepPointer;
			Event event;
			do{
				stepPointer = stepPointer+1;
			}while (stepPointer < oracleEntries.size() && (!(oracleEntries.get(stepPointer) instanceof Step) ||
					//TODO: do we need this.. i don't think we store internal events any more
					(event = Utils.findEvent(((Step)oracleEntries.get(stepPointer)).getName(), machine))!=null &&
					Utils.isInternal(event))) ;
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
	
	/**
	 * Stop playback from the current oracle file
	 * @param save
	 * @return
	 */
	public boolean stopPlayback() {
		assert(playback==true);
		if (debug) System.out.println("Oracle stopPlayback" ); //(save = "+save+")");
		playback = false;
		return true;
	}
	
	////////////////////////////////
	/////////////internal//////////
	////////////////////////////////
	private Shell shell = null;
	private Machine machine = null;
	private IFolder folder = null;
	private TransactionalEditingDomain editingDomain;
	private String oracleName = null;
		
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

	/**
	 * @return
	 */
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

	/**
	 * @param integer
	 * @return
	 */
	private String twoDigits(int integer) {
		String ret = Integer.toString(integer);
		if (ret.length()<2) ret = "0"+ret;
		return ret;
	}
	
	/**
	 * @return
	 */
	private Run getGoldRun() {
		try {
		   FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		   dialog.setFilterExtensions(new String [] {"*."+oracleExtension,"*.*"});
		   dialog.setFilterPath(oracleFolder.getRawLocation().toString());
		   dialog.setText("Select Oracle File to Replay");
		   String rawLocation = dialog.open();
		   if (rawLocation==null) return null;
		   IPath rawPath = new Path(rawLocation);
		   URI uri = URI.createFileURI(rawPath.toString());
		   ResourceSet rset = editingDomain.getResourceSet();
		   Resource resource = rset.getResource(uri, true);	   
		   Run run = loadRun(resource);
		   return run;
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	

	/**
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	private Run loadRun(Resource resource) throws CoreException{
		try {
			resource.load(null);
		} catch (IOException e) {
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
	
	/**
	 * @param run
	 * @throws CoreException
	 */
	public void save(Run run) throws CoreException{	
		final Resource resource = getResource(oracleName, getTimeStamp());
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
				e.printStackTrace();
			} 
		}
	}

	/**
	 * @param name
	 * @param timestamp
	 * @return
	 * @throws CoreException
	 */
	private Resource getResource(String name, String timestamp) throws CoreException{
		IPath filePath = folder.getFullPath();
		filePath = filePath.append("/"+machine.getName());
		filePath = filePath.addFileExtension(name);
		filePath = filePath.addFileExtension(timestamp); 
		filePath = filePath.addFileExtension(oracleExtension);
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
	
	/**
	 * A command to save the oracle run
	 * 
	 * @author cfsnook
	 *
	 */
	private static class SaveRunCommand extends AbstractEMFOperation {
		
		Resource resource;
		Run run;

		public SaveRunCommand(TransactionalEditingDomain editingDomain, Resource resource, Shell shell, Run run) {
			super(editingDomain, "what can I say?");
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
			try {
				resource.save(saveOptions);
			}
			catch (Exception e) {
				e.printStackTrace();
				return new Status(Status.ERROR, Activator.PLUGIN_ID, "Saving Oracle Failed" , e);
			}
			return new Status(Status.OK, Activator.PLUGIN_ID, "Saving Oracle Succeeded");
		}
	}


}
