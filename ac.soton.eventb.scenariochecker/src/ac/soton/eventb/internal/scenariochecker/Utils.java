package ac.soton.eventb.internal.scenariochecker;

import java.util.List;

import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import de.prob.core.Animator;
import de.prob.core.command.GetCurrentStateIdCommand;
import de.prob.core.command.GetEnabledOperationsCommand;
import de.prob.core.domainobjects.Operation;
import de.prob.exceptions.ProBException;

public class Utils {

	////////////////////////////// utils /////////////
	
	/**
	 * 
	 * @param op
	 * @return
	 */
	public static boolean isNextOp(Operation op) {
		return (OracleHandler.getOracle().isPlayback() && op==OracleHandler.getOracle().findNextOperation());
	}

	/**
	 * 
	 * @param opSignature
	 * @return
	 */
	public static Operation findOperation(String opSignature) {
		List<Operation> enabledOpsList = null;
		try {
			enabledOpsList = GetEnabledOperationsCommand.getOperations(
					Animator.getAnimator(),
					GetCurrentStateIdCommand.getID(Animator.getAnimator()));
		} catch (ProBException e1) {
			e1.printStackTrace();
		}
		if (enabledOpsList != null) {
			for (Operation op : enabledOpsList) {
				if (opSignature.equals(operationInStringFormat(op))){
					return op;
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param op
	 * @return
	 */
	public static String operationInStringFormat(Operation op) {
		return op.toString().replaceFirst("\\(", " (");
	}
	
	public static boolean isExternal(Event ev) {
		if (ev == null) return false;
		return !isInternal(ev);
	}
		
	/**
	 * return true if the given event is internal
	 * 
	 * @param event
	 * @return
	 */
	public static boolean isInternal(Event ev) {
		String comment = ev.getComment();
		return comment!=null && comment.contains("<INTERNAL>");
	}
	
	/**
	 * return true if the given variable is private 
	 * @param name
	 * @return
	 */
	public static boolean isPrivate(String name, Machine machine) {
		org.eventb.emf.core.machine.Variable var = findVariable(name, machine);
		if (var == null) {
			return false;
		}else {
			String comment = var.getComment();
			return comment!=null && comment.contains("<PRIVATE>");
		}
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
			if (priString!=null && priString.contains("<PRIORITY=")) {
				priString = priString.substring(priString.indexOf("<PRIORITY=")+10);
				int i = priString.indexOf(">"); 
				if (i>0)  priString =  priString.substring(0,i);
				pri = Integer.valueOf(priString);
			}
		}
		return pri;
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
