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
import java.util.Calendar;
import java.util.HashMap;
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
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.emf.oracle.Oracle;
import ac.soton.eventb.emf.oracle.OracleFactory;
import ac.soton.eventb.emf.oracle.OraclePackage;
import ac.soton.eventb.emf.oracle.Run;
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

	private Shell shell = null;
	private Machine machine = null;
	private IFolder folder = null;
	private TransactionalEditingDomain editingDomain;
	private String oracleName = null;
	private IFolder oracleFolder = null;
	
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
	
	public void initialise(String name, EventBObject eventBObject){
		shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		machine = (Machine) ((EventBNamed)eventBObject.getContaining(CorePackage.Literals.EVENT_BNAMED_COMMENTED_COMPONENT_ELEMENT));
		try {
			folder = getOracleFolder(eventBObject);
		} catch (CoreException e) {
			e.printStackTrace();
			if (debug) System.out.println("Oracle initialisation FAILED");
		}
		oracleName = name;
		for (Resource resource : editingDomain.getResourceSet().getResources()){
			resource.unload();
		}		
	}

		

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
	 * loads an oracle run by asking the user to select an oracle file from the workspace
	 * @return Run - the run contained inside the file selected by the user
	 */
	public Run getRun() {
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
	 * save the given run as an oracle file.
	 * The filename is automatically constructed from the current time stamp
	 * The run is then disconnected from the resource so that further changes 
	 * can be made to it without affecting the saved snapshot
	 * 
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
