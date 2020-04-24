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

package ac.soton.eventb.internal.scenariochecker;

/**
 * Simple centralised clock for keeping track of the simulator ticks
 * 
 * A tick is defined as the run to completion of all internal events
 * 
 * 
 * 
 * @author cfsnook
 *
 */
public class Clock {
	
	//singleton
	private static Clock instance = null;
	private Clock() {}
	public static Clock getInstance() {
		if (instance==null) {
			instance = new Clock();
		}
		return instance;
	}
	
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
