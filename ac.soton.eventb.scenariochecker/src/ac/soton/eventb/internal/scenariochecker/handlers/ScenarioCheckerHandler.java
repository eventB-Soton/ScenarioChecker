/*******************************************************************************
 * Copyright (c) 2019, 2021 University of Southampton.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    University of Southampton - initial API and implementation
 *******************************************************************************/
package ac.soton.eventb.internal.scenariochecker.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IMachineRoot;

import ac.soton.eventb.internal.scenariochecker.perspectives.ScenarioCheckerPerspective;
import ac.soton.eventb.probsupport.AnimationManager;


/**
 * This handler is the same as the ProB support handlers but will make sure the scenario checker is open and 
 * will switch to the scenario checker perspective.
 * 
 * @author cfsnook
 *
 */

public class ScenarioCheckerHandler extends AbstractHandler implements IHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

		// Get the selected Event-B machine
		IMachineRoot mchRoot = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				Object obj = ssel.getFirstElement();
				if (obj instanceof IMachineRoot) {
					mchRoot = (IMachineRoot) obj;
				}
			}
		}
		// Return if the current selection is not a machine root.
		if (mchRoot == null) return null;

		// a runnable to switch to the scenario checker perspective
		Runnable scenarioPerspectiveswitcher = new Runnable() {
			@Override
			public void run() {
				try {
					PlatformUI.getWorkbench().showPerspective(ScenarioCheckerPerspective.PERSPECTIVE_ID, HandlerUtil.getActiveWorkbenchWindow(event));
				} catch (WorkbenchException e) {
					e.printStackTrace();
				}
			}
		};

		// Switch to Scenario Checker perspective.
		// .. this ensures the scenario checker views are open  
		Display.getDefault().syncExec(scenarioPerspectiveswitcher);
		
		
		// If a machine is selected, initialise the animations for it
		// This initialises all animation participants that have open views/editors..
		// ... including the scenario checker which we opened above
		if (mchRoot != null) {
			AnimationManager.startAnimation(mchRoot);
		}
		
		// we need to do a restart to actually get started
		AnimationManager.restartAnimation(mchRoot);
		
		// Switch to Scenario Checker perspective.
		// .. BMotion studio tries to use its perspective.. but we can try to get it back to our perspective
		Display.getDefault().asyncExec(scenarioPerspectiveswitcher);
					
		return null;
	}

	
}
