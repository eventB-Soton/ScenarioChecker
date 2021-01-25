/*******************************************************************************
 * Copyright (c) 2011, 2020 University of Southampton.
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IMachineRoot;

import ac.soton.eventb.internal.scenariochecker.perspectives.ScenarioCheckerPerspective;
import ac.soton.eventb.probsupport.AnimationManager;


/**
 * This handler is the same as the ProB support handlers but will switch to the scenario checker perspective.
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

		// If a machine is selected, start the animations for it
		// This starts all animation participants that have open views/editors..
		// ... including the scenario checker if it is open
		if (mchRoot != null) {
			AnimationManager.startAnimation(mchRoot);
		}
		
		// Switch to Scenario Checker perspective.
		IWorkbench workbench = PlatformUI.getWorkbench();
		try {
			workbench.showPerspective(ScenarioCheckerPerspective.PERSPECTIVE_ID, HandlerUtil.getActiveWorkbenchWindow(event));   //activeWorkbenchWindow);
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
 
		return null;
	}

}
