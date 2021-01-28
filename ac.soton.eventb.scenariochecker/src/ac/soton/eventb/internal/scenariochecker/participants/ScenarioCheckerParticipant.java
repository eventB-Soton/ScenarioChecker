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
package ac.soton.eventb.internal.scenariochecker.participants;

import org.eventb.core.IMachineRoot;

import ac.soton.eventb.probsupport.IAnimationParticipant;
import ac.soton.eventb.scenariochecker.ScenarioCheckerManager;

/**
 * The Animation Participant for the Scenario Checker
 * 
 * @author cfsnook
 *
 */
public class ScenarioCheckerParticipant implements IAnimationParticipant {
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#startAnimating(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void startAnimation(IMachineRoot mchRoot) {
		if (ScenarioCheckerManager.getDefault().isOpen()) {
			ScenarioCheckerManager.getDefault().initialise(mchRoot);
		}
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#stopAnimating(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void stopAnimation(IMachineRoot mchRoot) {
		ScenarioCheckerManager.getDefault().stop(mchRoot);
	}

	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#updateAnimation(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void updateAnimation(IMachineRoot mchRoot) {
		ScenarioCheckerManager.getDefault().currentStateChanged(mchRoot);
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#restartAnimation(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void restartAnimation(IMachineRoot mchRoot) {
		if (ScenarioCheckerManager.getDefault().isOpen()) {
			ScenarioCheckerManager.getDefault().restart(mchRoot);
		}
	}
	
}
