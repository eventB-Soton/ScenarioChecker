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

import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.Attribute;
import org.eventb.emf.core.AttributeType;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.probsupport.AnimationManager;
import ac.soton.eventb.probsupport.data.Operation_;

/**
 * Some utils used in the scenario checker
 * 
 * @author cfsnook
 *
 */

public class Utils {

	/**
	 * Find an operation with the given signature in the animation of the given machine root
	 * 
	 * 
	 * @param opSignature - a simple string representation of the operation signature
	 * @return	Operation_ - the operation in the format of the Soton ProBSupport plugin
	 */
	public static Operation_ findOperation(IMachineRoot mchRoot, String opSignature) {
		for (Operation_ op : AnimationManager.getEnabledOperations(mchRoot)) {
			if (opSignature.equals(op.inStringFormat())){
				return op;
			}
		}
		return null;
	}
	

	
	/**
	 * returns true if the event is not null and is not annotated as internal
	 * 
	 * @param ev
	 * @return
	 */
	public static boolean isExternal(Event ev) {
		if (ev == null) return false;
		return !isInternal(ev);
	}
		
	/**
	 * return true if the given event is annotated as internal 
	 * 
	 * The annotation may either be in a comment or in an attribute with key "INTERNAL"
	 * 
	 * @param event
	 * @return
	 */
	public static boolean isInternal(Event ev) {
		String comment = ev.getComment();
		return (comment!=null && comment.contains("<INTERNAL>")) || getBooleanAttribute(ev, "INTERNAL");
	}
	

	
	/**
	 * return true if the given machine has a variable with the given name
	 * and that variable is annotated as private 
	 * 
	 * The annotation may either be in a comment or in an attribute with key "PRIVATE"
	 * 
	 * @param name
	 * @return
	 */
	public static boolean isPrivate(String name, Machine machine) {
		org.eventb.emf.core.machine.Variable var = findVariable(name, machine);
		if (var == null) {
			return false;
		}else {
			String comment = var.getComment();
			return (comment!=null && comment.contains("<PRIVATE>")) || getBooleanAttribute(var, "PRIVATE");
		}
	}
	
	/**
	 * utility method - returns true if the element has a boolean attribute with the given key, 
	 * that is set to value true
	 * 
	 * @param element
	 * @param key
	 * @return
	 */
	private static boolean getBooleanAttribute(EventBElement element, String key) {
		Attribute value = element.getAttributes().get(key);
		if (value!= null) {
			if (value.getType().equals(AttributeType.BOOLEAN) &&
					value.getValue().equals(Boolean.TRUE)) {
				return true;
			}
		}
		return false;
	}
	
	/** 
	 * gets the priority of an event where a low number is high priority 
	 * (-1 indicates null event)
	 * external are always the lowest priority (highest integer returned) 
	 * internal events may be prioritised by a comment 
	 * 
	 * @param ev
	 * @return
	 */
	public static Integer getPriority(Event ev) {
		if (ev == null) return -1;
		String priString = ev.getComment();
		Integer pri = Integer.MAX_VALUE;
		if (isInternal(ev)) {
			pri = pri-1;	//internal events default to slightly higher priority than external
			Integer priAtt = getIntegerAttribute(ev, "PRIORITY");
			if (priAtt != null) {
				pri = priAtt;
			}else if (priString!=null && priString.contains("<PRIORITY=")) {
				priString = priString.substring(priString.indexOf("<PRIORITY=")+10);
				int i = priString.indexOf(">"); 
				if (i>0)  priString =  priString.substring(0,i);
				pri = Integer.valueOf(priString);
			}
		}
		return pri;
	}
	
	/**
	 * utility method - returns the value of the integer attribute with the given key,
	 * or null if the element has no such attribute 
	 * 
	 * @param element
	 * @param key
	 * @return
	 */
	private static Integer getIntegerAttribute(EventBElement element, String key) {
		Attribute value = element.getAttributes().get(key);
		if (value!= null) {
			if (value.getType().equals(AttributeType.INTEGER) ) {
					return (int) value.getValue();
			}
		}
		return null;
	}
	
	/**
	 * find an event in the machine with the given name
	 * 
	 * @param event name
	 * @return
	 */
	public static Event findEvent(String name, Machine machine) { 
		for (Event ev : machine.getEvents()) {
			if (name.equals(ev.getName())) {
				return ev;
			}
		}
		return null;
	}
	
	/**
	 * returns the variable in the machine with the given name, or null
	 */
	public static org.eventb.emf.core.machine.Variable findVariable(String name, Machine machine) {
		for (org.eventb.emf.core.machine.Variable var : machine.getVariables()) {
			if (name.equals(var.getName())) {
				return var;
			}
		}
		return null;
	}

	
}
