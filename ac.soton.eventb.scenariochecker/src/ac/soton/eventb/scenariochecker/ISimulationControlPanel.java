package ac.soton.eventb.scenariochecker;

import java.util.List;

public interface ISimulationControlPanel {

	void updateModeIndicator(Mode mode);
	
	void updateEnabledOperations(List<String> names, int initialSelection);

	boolean isReady();

	void stop();

	void start();
	
}
