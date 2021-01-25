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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;

import ac.soton.eventb.scenariochecker.IScenarioCheckerView;

/**
 * This is the Console view for the Scenario Checker
 * 
 * @author cfsnook
 *
 */
public class ScenarioCheckerConsoleView extends AbstractScenarioCheckerView implements IScenarioCheckerView{
	
	public static final String ID = "ac.soton.eventb.internal.scenariochecker.views.ScenarioCheckerConsoleView"; //$NON-NLS-1$
	
	private List messageArea;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void doCreatePartControl() {

		{	//Status Message Area
			messageArea = new List(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);			
			FormData fd = new FormData();
			fd.left = new FormAttachment(0, 5);
			fd.right = new FormAttachment(100, -5);
			fd.top = new FormAttachment(0, 5);
			fd.bottom = new FormAttachment(100, -5);
			messageArea.setLayoutData(fd);
			messageArea.setToolTipText("Scenario Checker Console Messages");
			messageArea.setVisible(true);
			messageArea.redraw();
		}

	}

	///////////// Control panel interface IScenarioCheckerView - API for Simulation Manager //////////////
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#start()
	 */
	@Override
	public void start(String machineName) {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
				setPartName(getPartName()+" - "+machineName);	//add machine name to tab
				messageArea.removeAll();						//reset the message list
		    }
		});
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#stop()
	 */
	@Override
	public void stop() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (getPartName().contains(" - ")) {
		    		setPartName(getPartName().substring(0, getPartName().indexOf(" - "))); //remove machine name from tab
		    	}
		    }
		});
	}	
	
	/**
	 * Displays the message on the console.
	 * The newest message is displayed at the top of the console.
	 * The message can be multi-line (i.e. contain \n)
	 * 
	 * @param message the multi-line string to be displayed
	 */
	@Override
	public void displayMessage(String message) {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	messageArea.deselectAll();
		    	
		    	String[] lines = message.split("\n");
		    	
		    	for (int i=lines.length-1; i>-1; i-- ) {
		    		String line = lines[i];
		    		messageArea.add(line,0); 
		    	}

		    }
		});
	}

}
