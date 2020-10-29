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
 * An interface for views for the scenario checker
 * 
 * @author cfsnook
 *
 */
public interface IScenarioCheckerView {

	/**
	 * Update the playback/recording mode indicator to the given mode
	 * 
	 * @param mode
	 */
	void updateModeIndicator(Mode mode);
	
	/**
	 * Update the list of enabled operations to the given list
	 * and highlight the operation given by the index, selected (-1 = none selected)
	 * 
	 * @param enabledOperations
	 * @param selected
	 */
	void updateEnabledOperations(List<String> enabledOperations, int selected);
	
	/**
	 * Update the dirty status to the given value
	 * 
	 * @param dirty
	 */
	void updateDirtyStatus(boolean dirty);
	
	/**
	 * Update the state to the given list of values
	 * Each entry is a triplet - identifier, actual value, expected value
	 * 
	 * @param state
	 */
	void updateState(List<Triplet<String, String, String>> state);

	/**
	 * check whether the view is ready to be updated
	 * @return
	 */
	boolean isReady();

	/**
	 * start - initialise the view, ready for updates.
	 * The machine name is passed in case the view wants to display it
	 * 
	 * @param machineName
	 * @since 1.0
	 */
	void start(String machineName);
	
	/**
	 * stop - clear any information from the view
	 */
	void stop();

	/**
	 * start - initialise the view, ready for updates
	 */
	void start();
	
}
