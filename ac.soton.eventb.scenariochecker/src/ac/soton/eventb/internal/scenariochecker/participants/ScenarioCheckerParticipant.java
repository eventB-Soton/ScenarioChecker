package ac.soton.eventb.internal.scenariochecker.participants;

import org.eventb.core.IMachineRoot;

import ac.soton.eventb.probsupport.IAnimationParticipant;
import ac.soton.eventb.scenariochecker.SimulationManager;

public class ScenarioCheckerParticipant implements IAnimationParticipant {
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#startAnimating(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void startAnimating(IMachineRoot mchRoot) {
		if (SimulationManager.getDefault().isOpen()) {
			SimulationManager.getDefault().initialise(mchRoot);
		}
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#stopAnimating(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void stopAnimating(IMachineRoot mchRoot) {
		SimulationManager.getDefault().stop(mchRoot);
	}

}
