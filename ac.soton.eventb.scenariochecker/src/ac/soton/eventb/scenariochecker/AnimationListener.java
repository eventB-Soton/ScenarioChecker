package ac.soton.eventb.scenariochecker;

import de.prob.core.IAnimationListener;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;

/**
 * a listener that ProB can instantiate.
 * It just defers to the singleton SimulationManager
 * @author cfs
 *
 */
public class AnimationListener implements IAnimationListener {
	@Override
	public void currentStateChanged(State activeState, Operation operation) {
		SimulationManager.getDefault().currentStateChanged(activeState, operation);
	}
}