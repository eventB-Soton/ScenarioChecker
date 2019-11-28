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
package ac.soton.umlb.internal.simulator.handlers;

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

import ac.soton.umlb.internal.simulator.perspectives.SimPerspective;
import ac.soton.umlb.internal.simulator.views.SimulatorView;


/**
 * @author cfsnook
 *
 */

public class SimulateHandler extends AbstractHandler implements IHandler {
//	private static final String BMOTION_STUDIO_EXT = "bmso";

	
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

		// Switch to umlb simulation perspective.
		IWorkbench workbench = PlatformUI.getWorkbench();
		try {
			workbench.showPerspective(SimPerspective.PERSPECTIVE_ID, HandlerUtil.getActiveWorkbenchWindow(event));   //activeWorkbenchWindow);
		} catch (WorkbenchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SimulatorView.getSimulatorView().initialise(mchRoot);

		return null;
	}

}
