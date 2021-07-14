/*******************************************************************************
 * Copyright (c) 2019, 2020 University of Southampton.
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

package ac.soton.eventb.internal.scenariochecker;

/**
 * Simple clock for keeping track of the simulator ticks
 * 
 * @author cfsnook
 *
 */
public class Clock {

	public Clock() {}
	
	private Integer clock = -1;
	
	public String getValue() {
		return clock.toString();
	}
	
	public int getValueInt() {
		return clock;
	}
	
	public void inc() {
		clock++;
	}
	
	public void set(int value) {
		clock = value;
	}
	
	public void reset() {
		clock = -1;
	}
		
}
