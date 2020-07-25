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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ac.soton.eventb.internal.scenariochecker.Triplet;
import ac.soton.eventb.scenariochecker.IScenarioCheckerView;
import ac.soton.eventb.scenariochecker.Mode;
import ac.soton.eventb.scenariochecker.ScenarioCheckerManager;

/**
 * This is an abstract basis for the Scenario Checker Views
 * 
 * update methods that do nothing
 * 
 * @author cfsnook
 *
 */
public abstract class AbstractScenarioCheckerView extends ViewPart implements IScenarioCheckerView{
		
	protected static final Color red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
	protected static final Color blue = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
	protected static final Color green = Display.getCurrent().getSystemColor(SWT.COLOR_GREEN);	
	
	/**
	 * Creates a Scenario Checker control panel view and registers it with the Simulation Manager
	 */
	public AbstractScenarioCheckerView() {
		super();
		//register with the manager as a Simulation Control Panel
		ScenarioCheckerManager.getDefault().addSimulationView(this);
	}
	
	protected final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	protected Composite container;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public final void createPartControl(Composite parent) {
		Display.getDefault().syncExec(new Runnable() {
		    public void run() {
			container = toolkit.createComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE );
			toolkit.paintBordersFor(container);
			container.setLayout(new FormLayout());
							
			doCreatePartControl();
			
			stop();	//initialise as stopped
		    }


		});
	}
	
	/**
	 * clients should override to specify the main body of the creation of the part
	 * adding to the composite, container
	 * 
	 */
	protected abstract void doCreatePartControl();
	
	/**
	 * When the part is focused, pass the focus to the container
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		container.setFocus();
	}
	
	/*
	 * display message to the user
	 */
	protected int messageUser(String title, String message, int style) {
		MessageBox mbox = new MessageBox(getSite().getShell(), style);
		mbox.setText(title);
		mbox.setMessage(message);
		return mbox.open();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		ScenarioCheckerManager.getDefault().removeSimulationView(this);
		super.dispose();
	}

	/////////////  interface IScenarioCheckerView - API for Scenario Checker Manager //////////////
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#isReady()
	 */
	@Override
	public boolean isReady() {
		if (!this.container.isDisposed()) return true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#start()
	 */
	@Override
	public void start() {
		//do nothing
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#stop()
	 */
	@Override
	public void stop() {
		//do nothing
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#updateEnabledOperations(java.util.List, int)
	 */
	@Override
	public void updateEnabledOperations (List<String> enabledOperations, int selected) {
		//do nothing
	}

	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#updateModeIndicator(ac.soton.eventb.scenariochecker.Mode)
	 */
	@Override
	public void updateModeIndicator(Mode mode) {
		//do nothing
	}	

	@Override
	public void updateDirtyStatus(boolean dirty) {
		//do nothing
	}

	@Override
	public void updateState(List<Triplet<String, String, String>> result) {
		//do nothing
	}

}
