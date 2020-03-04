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
 * @author cfs
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
	//Variable currTimeVar = null;
	
	public String getValue() {
		//currTimeVar = stateMap.get("current_time");
//		if (currTimeVar==null) { 
			return clock.toString();
//		}else {
//			return currTimeVar.getValue();
//		}
	}
	
	public int getValueInt() {
		//currTimeVar = stateMap.get("current_time");
//		if (currTimeVar==null) { 
			return clock;
//		}else {
//			return currTimeVar.getValue();
//		}
	}

	
	public void inc() {
		//currTimeVar = stateMap.get("current_time");
		//TODO: we would like to increment the clock only when a non-internal event is enabled.
		clock++;
	}
	
	public void set(int value) {
		clock = value;
	}
	
	public void reset() {
		clock = -1;
	}
		
}
