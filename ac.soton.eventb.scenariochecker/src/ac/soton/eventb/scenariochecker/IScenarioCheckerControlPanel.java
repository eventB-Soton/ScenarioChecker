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

public interface IScenarioCheckerControlPanel {

	void updateModeIndicator(Mode mode);
	
	void updateEnabledOperations(List<String> enabledOperations, int selected);

	boolean isReady();

	void stop();

	void start();

}
