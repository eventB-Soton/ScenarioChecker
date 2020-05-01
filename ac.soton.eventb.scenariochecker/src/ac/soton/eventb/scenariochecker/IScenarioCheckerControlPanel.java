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
package ac.soton.eventb.scenariochecker;

import java.util.List;

import ac.soton.eventb.internal.scenariochecker.Triplet;

/**
 * An interface for control panels for the scenario checker
 * 
 * @author cfsnook
 *
 */
public interface IScenarioCheckerControlPanel {

	void updateModeIndicator(Mode mode);
	
	void updateEnabledOperations(List<String> enabledOperations, int selected);
	
	void updateDirtyStatus(boolean dirty);
	
	void updateFailures(List<Triplet<String, String, String>> result);

	boolean isReady();

	void stop();

	void start();
	
}
