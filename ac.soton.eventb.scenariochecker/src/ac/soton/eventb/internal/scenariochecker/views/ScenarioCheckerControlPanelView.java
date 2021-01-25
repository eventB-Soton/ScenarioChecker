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

import ac.soton.eventb.scenariochecker.IScenarioCheckerView;
import ac.soton.eventb.scenariochecker.Mode;
import ac.soton.eventb.scenariochecker.ScenarioCheckerManager;

/**
 * This is the Control Panel view for the Scenario Checker
 * 
 * @author cfsnook
 *
 */
public class ScenarioCheckerControlPanelView extends AbstractScenarioCheckerView implements IScenarioCheckerView{
	
	public static final String ID = "ac.soton.eventb.internal.scenariochecker.views.ScenarioCheckerControlPanelView"; //$NON-NLS-1$
	
	private Button modeButton;
	private Button restartButton;
	private Button saveButton;
	private Button bigStepButton;
	private Button smallStepButton;
	private Button severalStepsButton;

	private Text stepCount;
	private String defaultStepCount = "5";
	
	private List operations;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void doCreatePartControl() {
		Group buttonGroup =  new Group(container, SWT.BORDER); 
		{ // button group
			FormData fd_buttonGroup = new FormData();
			fd_buttonGroup.top = new FormAttachment(0, 5);
			buttonGroup.setLayoutData(fd_buttonGroup);
			toolkit.adapt(buttonGroup);
			toolkit.paintBordersFor(buttonGroup);
			buttonGroup.setLayout(null);
			{	//MODE - indicates mode but also a button to change mode
				modeButton = new Button(buttonGroup, SWT.NONE);
				modeButton.setBounds(10, 10, 110, 30);
				modeButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						if (!ScenarioCheckerManager.getDefault().isDirty() ||
								SWT.OK == messageUser("Question", "Do you really want to discard this scenario?", SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL)) {
							ScenarioCheckerManager.getDefault().modeButtonPressed();
						}

					}
				});
				toolkit.adapt(modeButton, true, true);
				updateModeIndicator(Mode.RECORDING);	//start off in recording mode
			}
			{	//RESTART
				restartButton = new Button(buttonGroup, SWT.NONE);
				restartButton.setBounds(10, 45, 85, 30);
				restartButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {	
						if (!ScenarioCheckerManager.getDefault().isDirty() ||
								SWT.OK == messageUser("Question", "Do you really want to discard this scenario?", SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL)) {
							ScenarioCheckerManager.getDefault().restartPressed();
						}
					}
				});
				toolkit.adapt(restartButton, true, true);
				restartButton.setText("Restart");
				restartButton.setToolTipText("Start the current scenario again from INITIALISATION");
			}
			{	//SAVE
				saveButton = new Button(buttonGroup, SWT.NONE);
				saveButton.setBounds(10, 80, 85, 30);
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
			
			{	//BIG STEP
				bigStepButton = new Button(buttonGroup, SWT.NONE);
				bigStepButton.setBounds(10, 115, 85, 30);
				bigStepButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						if (!ScenarioCheckerManager.getDefault().bigStep()) {
							if (ScenarioCheckerManager.getDefault().isPlayback()) {
								messageUser("Information", "Playback has finished - press mode button to continue in recording mode", SWT.ICON_INFORMATION | SWT.OK);
							}else {
								messageUser("Information", "Deadlock - nothing is enabled", SWT.ICON_INFORMATION | SWT.OK);
							}
						}
						
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
				smallStepButton.setBounds(10, 150, 85, 30);
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
				severalStepsButton.setBounds(10, 185, 85, 30);
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
				stepCount.setBounds(95, 190, 20, 20);
				stepCount.setText(defaultStepCount);
				toolkit.adapt(stepCount, true, true);
			}

		}
		
		operations = new List(container, SWT.BORDER	| SWT.H_SCROLL | SWT.V_SCROLL) ;
		{
			operations.addMouseListener(new MouseAdapter() {
				
				@Override
				public void mouseUp(MouseEvent e) {
					//operations widget should only be enabled in recording mode
					// Select operation for later execution
					int index = operations.getSelectionIndex();
					int count = operations.getItemCount();
					if (index<0 || index>=count) return;
					String selected = operations.getItem(index);
					//tell the manager about the new selection but not to fire it yet
					ScenarioCheckerManager.getDefault().selectionChanged(selected, false);
				}
				
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					//operations widget should only be enabled in recording mode
					// Select operation for later execution
					int index = operations.getSelectionIndex();
					int count = operations.getItemCount();
					if (index<0 || index>=count) return;
					String selected = operations.getItem(index);
					//tell the manager about the new selection and to fire it
					ScenarioCheckerManager.getDefault().selectionChanged(selected, true);
				}
			});
			FormData fd = new FormData();
			fd.left = new FormAttachment(buttonGroup, 5);
			fd.right = new FormAttachment(100, -5);
			fd.top = new FormAttachment(0, 5);
			fd.bottom = new FormAttachment(100, -5);
			operations.setLayoutData(fd);
		    operations.setToolTipText("Enabled External Operations");
		    operations.setVisible(true);
		    operations.add("");	//it seems to need some dummy data to get it started
			operations.redraw();
		}
	}
	
	/**
	 * When the part is focused, pass the focus to the big step button.
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	/* (non-Javadoc)
	 * @see ac.soton.eventb.internal.scenariochecker.views.AbstractScenarioCheckerView#setFocus()
	 */
	@Override
	public void setFocus() {
		super.setFocus();
		bigStepButton.setFocus();
	}
	

	///////////// Control panel interface IScenarioCheckerView - API for Simulation Manager //////////////
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#start()
	 */
	@Override
	public void start(String machineName) {
		Display.getDefault().syncExec(new Runnable() {		//sync to ensure start and stop in correct order
		    public void run() {
				setPartName(getPartName()+" - "+machineName);	//add machine name to tab
		    	if (!modeButton.isDisposed()) {
					modeButton.setEnabled(true);
					restartButton.setEnabled(true);
					saveButton.setEnabled(true);
					bigStepButton.setEnabled(true);
					smallStepButton.setEnabled(true);
					severalStepsButton.setEnabled(true);
		    	}
		    }
		});
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#stop()
	 */
	@Override
	public void stop() {
		Display.getDefault().syncExec(new Runnable() {		//sync to ensure start and stop in correct order
		    public void run() {
		    	if (getPartName().contains(" - ")) {
		    		setPartName(getPartName().substring(0, getPartName().indexOf(" - "))); //remove machine name from tab
		    	}
		    	if (!modeButton.isDisposed()) {
					modeButton.setEnabled(false);
					restartButton.setEnabled(false);
					saveButton.setEnabled(false);
					bigStepButton.setEnabled(false);
					smallStepButton.setEnabled(false);
					severalStepsButton.setEnabled(false);
					updateEnabledOperations(Collections.emptyList(),-1);
		    	}
		    }
		});
	}
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#updateEnabledOperations(java.util.List, int)
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
	 * @see ac.soton.eventb.scenariochecker.IScenarioCheckerView#updateModeIndicator(ac.soton.eventb.scenariochecker.Mode)
	 */
	@Override
	public void updateModeIndicator(Mode mode) {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (!modeButton.isDisposed()) {
					if (mode == Mode.RECORDING) {
						modeButton.setText("Recording");
						modeButton.setBackground(white);
						modeButton.setForeground(red);
						modeButton.setToolTipText("Change mode to "+Mode.PLAYBACK);
						operations.setEnabled(true);
					}else if (mode == Mode.PLAYBACK) {
						modeButton.setText("Playback");
						modeButton.setBackground(white);
						modeButton.setForeground(blue);
						modeButton.setToolTipText("Change mode to "+Mode.RECORDING);
						operations.setEnabled(false);
					}
		    	}
		    }
		});
	}	

	/* (non-Javadoc)
	 * @see ac.soton.eventb.internal.scenariochecker.views.AbstractScenarioCheckerView#updateDirtyStatus(boolean)
	 */
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
