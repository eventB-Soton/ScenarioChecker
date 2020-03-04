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

import de.prob.core.IAnimationListener;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;

/**
 * a listener that ProB can instantiate.
 * It just defers to the singleton ScenarioCheckerManager
 * @author cfs
 *
 */
public class AnimationListener implements IAnimationListener {
	@Override
	public void currentStateChanged(State activeState, Operation operation) {
		ScenarioCheckerManager.getDefault().currentStateChanged(activeState, operation);
	}
}