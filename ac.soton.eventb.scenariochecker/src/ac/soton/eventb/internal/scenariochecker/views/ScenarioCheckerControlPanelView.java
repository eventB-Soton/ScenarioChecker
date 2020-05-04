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

import java.util.Collections;
//import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import ac.soton.eventb.scenariochecker.IScenarioCheckerControlPanel;
import ac.soton.eventb.scenariochecker.Mode;
import ac.soton.eventb.scenariochecker.ScenarioCheckerManager;

/**
 * This is the Control Panel view for the Scenario Checker
 * 
 * @author cfsnook
 *
 */
public class ScenarioCheckerControlPanelView extends AbstractScenarioCheckerView implements IScenarioCheckerControlPanel{
	
	public static final String ID = "ac.soton.eventb.internal.scenariochecker.views.ScenarioCheckerControlPanelView"; //$NON-NLS-1$
	
	private Button modeIndicator; //not really a button.. used to display the record/playback mode
	
	private Button bigStepButton;
	private Button smallStepButton;
	private Button severalStepsButton;
	private Button restartButton;
	private Button saveButton;
	private Button replayButton;
	private Button stopButton;

	private Text stepCount;
	private String defaultStepCount = "5";
	
	private List operations;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void doCreatePartControl() {
		//Group buttonGroup = createButtonGroup();
		Group buttonGroup =  new Group(container, SWT.BORDER); 
		{ // button group
			FormData fd_buttonGroup = new FormData();
			fd_buttonGroup.top = new FormAttachment(0, 5);
			//fd_buttonGroup.right = new FormAttachment(100, -700);
			buttonGroup.setLayoutData(fd_buttonGroup);
			toolkit.adapt(buttonGroup);
			toolkit.paintBordersFor(buttonGroup);
			buttonGroup.setLayout(null);
			{	//INDICATOR - not a button
				modeIndicator = new Button(buttonGroup, SWT.NONE);
				modeIndicator.setBounds(10, 10, 110, 25);
				toolkit.adapt(modeIndicator, true, true);
				updateModeIndicator(Mode.RECORDING);	//start off in recording mode
			}
			{	//BIG STEP
				bigStepButton = new Button(buttonGroup, SWT.NONE);
				bigStepButton.setBounds(10, 40, 85, 25);
				bigStepButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						ScenarioCheckerManager.getDefault().bigStep();
					}
				});
				toolkit.adapt(bigStepButton, true, true);
				bigStepButton.setText("Big Step");
				bigStepButton.setToolTipText("Execute next external event and then execute internal events until none are unabled");
			}
			{	//SML STEP
				smallStepButton = new Button(buttonGroup, SWT.NONE);
				smallStepButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						ScenarioCheckerManager.getDefault().singleStep();
					}
				});
				smallStepButton.setBounds(10, 70, 85, 25);
				toolkit.adapt(smallStepButton, true, true);
				smallStepButton.setText("Sml Step");
				smallStepButton.setToolTipText("Execute next external or internal event");
			}
			{	//RUN FOR
				severalStepsButton = new Button(buttonGroup, SWT.NONE);
				severalStepsButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						ScenarioCheckerManager.getDefault().runForTicks(Integer.valueOf(stepCount.getText()));
					}
				});
				severalStepsButton.setBounds(10, 100, 80, 25);
				toolkit.adapt(severalStepsButton, true, true);
				severalStepsButton.setText("Run For");
				severalStepsButton.setToolTipText("Execute next n events");
			}
			{	//Tick Count
				stepCount = new Text(buttonGroup, SWT.BORDER);
				stepCount.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						if (e.widget instanceof Text) {
							String newText = ((Text) e.widget).getText();
							stepCount.setText(newText);
							defaultStepCount = newText;
							stepCount.pack();
						}
					}
				});
				stepCount.setBounds(80, 100, 30, 20);
				stepCount.setText(defaultStepCount);
				toolkit.adapt(stepCount, true, true);
			}

			{	//RESTART
				restartButton = new Button(buttonGroup, SWT.NONE);
				restartButton.setBounds(10, 130, 85, 25); 	//92, 10, 70, 25);
				restartButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {	
						ScenarioCheckerManager.getDefault().restartPressed();
					}
				});
				toolkit.adapt(restartButton, true, true);
				restartButton.setText("Restart");
				restartButton.setToolTipText("Start the current scenario again from INITIALISATION");
			}
			{	//SAVE
				saveButton = new Button(buttonGroup, SWT.NONE);
				saveButton.setBounds(10, 160, 85, 25); //160, 10, 70, 25);
				saveButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						ScenarioCheckerManager.getDefault().savePressed();
					}
				});
				toolkit.adapt(saveButton, true, true);
				saveButton.setText("Save");
				saveButton.setToolTipText("Save the current scenario");

			}
			{	//REPLAY
				replayButton = new Button(buttonGroup, SWT.NONE);
				replayButton.setBounds(10, 190, 85, 25); //160, 41, 70, 25);
				replayButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						if (!ScenarioCheckerManager.getDefault().isDirty() ||
								SWT.OK == messageUser("Question", "Do you really want to discard the scenario we have just recorded?", SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL)) {
							ScenarioCheckerManager.getDefault().replayPressed();
						}
					}

				});
				toolkit.adapt(replayButton, true, true);
				replayButton.setText("Replay");
				replayButton.setToolTipText("Select a previously recorded scenario to play back");
			}
			{	//STOP
				stopButton = new Button(buttonGroup, SWT.NONE);
				stopButton.setBounds(10, 220, 85, 25); //160, 72, 70, 25);
				stopButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						ScenarioCheckerManager.getDefault().stopPressed();					
					}
				});

				toolkit.adapt(stopButton, true, true);
				stopButton.setText("Stop");
				stopButton.setToolTipText("Stop the current scenario play back and continue in recording mode");
			}
		}
		
		operations = new List(container, SWT.BORDER	| SWT.FULL_SELECTION) ;// Table(container, SWT.BORDER	| SWT.FULL_SELECTION);
		{
			operations.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					// Select operation for later execution
					int index = operations.getSelectionIndex();
					int count = operations.getItemCount();
					if (index<0 || index>=count) return;
					String selected = operations.getItem(index);
					if (ScenarioCheckerManager.getDefault().isPlayback()) {
						messageUser("Error", "Cannot select events manually while playback is in progress.", SWT.ICON_ERROR | SWT.OK);
					}else {
						//tell the manager about the new selection but not to fire it yet
						ScenarioCheckerManager.getDefault().selectionChanged(selected, false);
					}
				}
				
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// Execute the selected operation (as a big step)		
					String selected = operations.getItem(operations.getSelectionIndex());
					if (ScenarioCheckerManager.getDefault().isPlayback()) {
						messageUser("Error", "Cannot select events manually while playback is in progress.", SWT.ICON_ERROR | SWT.OK);
					}else {
						//tell the manager about the new selection and to fire it
						ScenarioCheckerManager.getDefault().selectionChanged(selected, true);
					}
				}
			});
			FormData fd = new FormData();
			fd.left = new FormAttachment(buttonGroup, 10);
			fd.right = new FormAttachment(100, -5);
			fd.top = new FormAttachment(0, 5);
			fd.bottom = new FormAttachment(100, -5);
			operations.setLayoutData(fd);
		    operations.setToolTipText("Enabled External Operations");
		    operations.setVisible(true);
		    operations.add("");	//it seems to need some dummy data to get it started
			//operations.pack();
			operations.redraw();
		}
	}
	
	/**
	 * When the part is focused, pass the focus to the step button.
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		bigStepButton.setFocus();
	}
	

	///////////// Control panel interface IScenarioCheckerControlPanel - API for Simulation Manager //////////////
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerControlPanel#start()
	 */
	@Override
	public void start() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (!modeIndicator.isDisposed()) {
					bigStepButton.setEnabled(true);
					saveButton.setEnabled(true);
					severalStepsButton.setEnabled(true);
					stopButton.setEnabled(true);
					replayButton.setEnabled(true);
					restartButton.setEnabled(true);
					smallStepButton.setEnabled(true);
					modeIndicator.setEnabled(true);
		    	}
		    }
		});
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerControlPanel#stop()
	 */
	@Override
	public void stop() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (!modeIndicator.isDisposed()) {
					updateEnabledOperations(Collections.emptyList(),-1);
					bigStepButton.setEnabled(false);
					saveButton.setEnabled(false);
					severalStepsButton.setEnabled(false);
					stopButton.setEnabled(false);
					replayButton.setEnabled(false);
					restartButton.setEnabled(false);
					smallStepButton.setEnabled(false);
					modeIndicator.setEnabled(false);
		    	}
		    }
		});
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerControlPanel#updateEnabledOperations(java.util.List, int)
	 */
	@Override
	public void updateEnabledOperations (java.util.List<String> enabledOperations, int selected) {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (!operations.isDisposed()) {
					operations.removeAll();
					for (String opString : enabledOperations) {
						operations.add(opString);
					}
					if (selected>-1) {
						operations.select(selected);
					}
					operations.redraw();
			    }
		    }
		});
	}

	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerControlPanel#updateModeIndicator(ac.soton.eventb.scenariochecker.Mode)
	 */
	@Override
	public void updateModeIndicator(Mode mode) {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (!modeIndicator.isDisposed()) {
					if (mode == Mode.RECORDING) {
						modeIndicator.setText("Recording");
						modeIndicator.setBackground(red);
						modeIndicator.setForeground(red);
					}else if (mode == Mode.PLAYBACK) {
						modeIndicator.setText("Playback");
						modeIndicator.setBackground(blue);
						modeIndicator.setForeground(blue);
					}
		    	}
		    }
		});
	}	

	@Override
	public void updateDirtyStatus(boolean dirty) {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
				String title = getPartName();
				if (dirty) {
					if (title.indexOf("*")!=0) {
						setPartName("*"+title);
					}
				}else {
					if (title.indexOf("*")==0) {
						setPartName(title.substring(1));
					}
				}
		    }
		});
	}

}
