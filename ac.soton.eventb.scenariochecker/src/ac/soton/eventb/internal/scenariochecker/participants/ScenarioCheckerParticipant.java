package ac.soton.eventb.internal.scenariochecker.participants;

import org.eventb.core.IMachineRoot;

import ac.soton.eventb.internal.scenariochecker.views.SimulatorView;
import ac.soton.eventb.probsupport.IAnimationParticipant;

public class ScenarioCheckerParticipant implements IAnimationParticipant {
	
	@Override
	public void startAnimating(IMachineRoot mchRoot) {
		
		
		SimulatorView.getSimulatorView().initialise(mchRoot);
	}
	
	@Override
	public void stopAnimating(IMachineRoot mchRoot) {
		//SimulationManager.getSimulatorView().dispose();
	}

}
