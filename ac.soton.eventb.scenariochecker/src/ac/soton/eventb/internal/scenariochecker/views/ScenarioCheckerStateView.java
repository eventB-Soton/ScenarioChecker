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
package ac.soton.eventb.internal.scenariochecker.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import ac.soton.eventb.internal.scenariochecker.Triplet;
import ac.soton.eventb.scenariochecker.IScenarioCheckerView;

/**
 * This is the State view for the Scenario Checker
 * 
 * @author cfsnook
 *
 */
public class ScenarioCheckerStateView extends AbstractScenarioCheckerView implements IScenarioCheckerView{
	
	public static final String ID = "ac.soton.eventb.internal.scenariochecker.views.ScenarioCheckerStateView"; //$NON-NLS-1$
	
	private Table stateTable;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void doCreatePartControl() {
							
			stateTable = new Table(container, SWT.BORDER	| SWT.FULL_SELECTION);
			{
				FormData fd = new FormData();
				fd.left = new FormAttachment(0, 5);
				fd.right = new FormAttachment(100, -5);
				fd.top = new FormAttachment(0, 5);
				fd.bottom = new FormAttachment(100, -5);
				stateTable.setLayoutData(fd);
				
				stateTable.setToolTipText("Displays any differences from the recorded state during playback");
				toolkit.adapt(stateTable);
				toolkit.paintBordersFor(stateTable);
				stateTable.setHeaderVisible(true);
				stateTable.setLinesVisible(true);
				
				String[] titles = {"Variable", "Actual Value", "Expected Value"};
			    for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
			      TableColumn column = new TableColumn(stateTable, SWT.NULL);
			      column.setText(titles[loopIndex]);
			    }
		        for (int i = 0; i < 1; i++) {
		            TableItem item = new TableItem(stateTable, SWT.NULL);
		            item.setText(0, "test0");
		            item.setText(1, "test1");
		            item.setText(2, "test2");
		         }
			    for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
			        stateTable.getColumn(loopIndex).pack();
			     }
				stateTable.pack();
				stateTable.redraw();
			}
			stop();	//initialise as stopped
	}
	
	/**
	 * When the part is focused, pass the focus to the stateTable
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		stateTable.setFocus();
	}

	/////////////  interface IScenarioCheckerView - API for Simulation Manager //////////////
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#stop()
	 */
	@Override
	public void stop() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
				stateTable.removeAll();
		    }
		});
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#start()
	 */
	@Override
	public void start() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
				stateTable.removeAll();
		    }
		});
	}

	/* (non-Javadoc)
	 * @see ac.soton.eventb.internal.scenariochecker.views.AbstractScenarioCheckerView#updateFailures(java.util.List)
	 */
	@Override
	public void updateState(List<Triplet<String, String, String>> result) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				stateTable.clearAll();
				stateTable.removeAll();
				TableItem item;
		        for (int i = 0; i < result.size(); i++) {
		            item = new TableItem(stateTable, SWT.NULL);
		            item.setText(result.get(i).first);
		            item.setText(0, result.get(i).first);
		            item.setText(1, result.get(i).second);
		            item.setText(2, result.get(i).third);
		            if ("".equals(result.get(i).third)){
		            	item.setForeground(1,null);		            	
		            }else if (isEqual(result.get(i).second.trim(), result.get(i).third.trim())) {
		            	item.setForeground(1,green);
		            	item.setForeground(2,null);
		            }else {
		            	item.setForeground(1,red);
		            	item.setForeground(2,null);	            	
		            }
		        }
		        for (int loopIndex = 0; loopIndex < 3; loopIndex++) {
	        	 	stateTable.getColumn(loopIndex).pack();
		        }
		        stateTable.redraw();
		    }
		});
	}

}
