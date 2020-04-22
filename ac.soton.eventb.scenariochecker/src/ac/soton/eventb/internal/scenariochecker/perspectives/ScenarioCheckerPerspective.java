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

package ac.soton.eventb.internal.scenariochecker.perspectives;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * <p>
 * The perspective for Scenario Checker. This is the default
 * implementation. All views and actions are added via Eclipse extension
 * mechanism.
 * </p>
 * 
 * @author cfsnook
 * 
 */
public class ScenarioCheckerPerspective implements IPerspectiveFactory {

	// The Perspective ID.
	public static final String PERSPECTIVE_ID = "ac.soton.eventb.scenariochecker.scenarioCheckerPerspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		// Everything is done via extension mechanism.
	}

}
