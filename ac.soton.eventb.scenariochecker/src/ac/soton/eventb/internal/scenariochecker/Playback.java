/*******************************************************************************
 * Copyright (c) 2011, 2020 University of Southampton.
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

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;

import ac.soton.eventb.emf.oracle.Entry;
import ac.soton.eventb.emf.oracle.Snapshot;
import ac.soton.eventb.emf.oracle.Step;
import ac.soton.eventb.probsupport.data.Operation_;

/**
 * A Playback consisting of a list of Oracle Entries
 * 
 * consume steps to progress through the playback.
 * 
 * The current snapshot (initially null) reflects the state after the last step
 * The next Step (finally null) reflects the operation that the playback should execute next.
 * 
 * @author cfsnook
 *
 */
public class Playback {

	// The list of play-back steps.
	private EList<Entry> playbackEntries;

	// The iterator of the play-back steps.
	private Iterator<Entry> playbackIter;

	// The next playback step.
	private Step nextStep;

	// The next playback snapshot.
	private Snapshot currentSnapshot;
	
	
	/**
	 * Constructs a new playback using the given list of entries
	 * 
	 * @param run
	 * 
	 */
	public Playback(EList<Entry> playbackEntries) {
		this.playbackEntries = playbackEntries;
		reset();
	}
	
	
	/**
	 * Returns the next snapshot.
	 * 
	 * @return
	 */
	public Snapshot getCurrentSnapshot() {
		return currentSnapshot;
	}
	
	/**
	 * Returns the next step.
	 * This does not consume the step
	 * 
	 * @return
	 */
	public Step getNextStep() {
		return nextStep;
	}
	
	/**
	 * Checks whether currently in playback mode 
	 * @return
	 */
	public boolean isPlayback(){
		return nextStep!=null || currentSnapshot!=null;
	}
	
	/**
	 * moves to the next step
	 */
	public void consumeStep() {
		if (playbackIter.hasNext()) {
			currentSnapshot = (Snapshot) playbackIter.next();
			if (playbackIter.hasNext()) {
				nextStep = (Step) playbackIter.next();
			}else {
				nextStep = null;
			}
		} else {
			nextStep = null;
			currentSnapshot = null;
		}
	}

	/**
	 * restarts the playback using its current entries
	 */
	public void reset() {
		playbackIter = this.playbackEntries.iterator();
		currentSnapshot = null;
		if (playbackIter.hasNext()) {
			nextStep = (Step) playbackIter.next();
		}
	}

	/**
	 * gets the next operation from the playback if there is one
	 * returns null if not.
	 * 
	 * @return
	 */
	public Operation_ getNextOperation() {
		if (nextStep!=null) {
			return new Operation_(nextStep.getName(), nextStep.getArgs());
		}
		return null;
	}

}
