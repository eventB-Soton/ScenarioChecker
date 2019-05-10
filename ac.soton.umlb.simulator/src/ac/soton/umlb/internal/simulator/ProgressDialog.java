/*******************************************************************************
 *  Copyright (c) 2019-2019 University of Southampton.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *   
 *  Contributors:
 *  University of Southampton - Initial implementation
 *******************************************************************************/
package ac.soton.umlb.internal.simulator;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.beans.*;
import java.util.Random;

public class ProgressDialog extends JPanel implements ActionListener,
		PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ProgressMonitor progressMonitor;
	private JTextArea taskOutput;
	private Task monitorRun;

	class Task extends SwingWorker<Void, Void> {
		@Override
		public Void doInBackground() {
			Random random = new Random();
			int progress = 0;
			setProgress(0);
			try {
				Thread.sleep(1000);
				while (progress < 100 && !isCancelled()) {
					// Sleep for up to one second.
					Thread.sleep(random.nextInt(1000));
					// Make random progress.
					progress += random.nextInt(10);
					setProgress(Math.min(progress, 100));
					System.out.println("visited task");
				}
			} catch (InterruptedException ignore) {
			}
			return null;
		}

		@Override
		public void done() {
			Toolkit.getDefaultToolkit().beep();
			progressMonitor.setProgress(0);
		}
	}

	public ProgressDialog() {
		super(new BorderLayout());

		taskOutput = new JTextArea(5, 20);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);
		add(new JScrollPane(taskOutput), BorderLayout.CENTER);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

	}


	/**
	 * Invoked when task's progress property changes.
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			progressMonitor.setProgress(progress);
			String message = String.format(".", progress);
			progressMonitor.setNote(message);
			taskOutput.append(message);
			if (progressMonitor.isCanceled() || monitorRun.isDone()) {
				Toolkit.getDefaultToolkit().beep();
				if (progressMonitor.isCanceled()) {
					monitorRun.cancel(true);
					taskOutput.append("Task canceled.\n");
				} else {
					taskOutput.append("Task completed.\n");
				}
			}
		}

	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	public void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("ProgressMonitorDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		JComponent newContentPane = new ProgressDialog();
		newContentPane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(newContentPane);

		// Display the window.
		frame.pack();
		frame.setVisible(false);
		
		progressMonitor = new ProgressMonitor(ProgressDialog.this,
				"Running Simulation", "", 0, 100);
		progressMonitor.setProgress(0);
		monitorRun = new Task();
		monitorRun.addPropertyChangeListener(this);
		monitorRun.execute();

	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}