/*
 * Copyright 2012,2013 Robert Huitema robert@42.co.nz
 * 
 * This file is part of FreeBoard. (http://www.42.co.nz/freeboard)
 * 
 * FreeBoard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FreeBoard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with FreeBoard. If not, see <http://www.gnu.org/licenses/>.
 */

package nz.co.fortytwo.freeboard.installer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;
import purejavacomm.CommPortIdentifier;

/** @see http://stackoverflow.com/questions/4053090 */
public class ChartManager extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String title = "Select a file";
	private ProcessingPanel processingPanel = new ProcessingPanel();
	private LoadingPanel uploadingPanel = new LoadingPanel();
	private CalibratePanel calibrationPanel = new CalibratePanel();
	private JLabel result = new JLabel(title, JLabel.CENTER);
	private ChartFileChooser chartFileChooser = new ChartFileChooser();
	private HexFileChooser hexFileChooser = new HexFileChooser();
	
	private StringBuffer rawBuffer= new StringBuffer();
	File toolsDir;
	JComboBox<String> deviceComboBox;
	HashMap<String, String> deviceMap = new HashMap<String, String>();
	JComboBox<String> portComboBox;
	JComboBox<String> portComboBox1;

	public ChartManager(String name) {
		super(name);

		String[] devices = new String[] { "ArduIMU v3", "Arduino Mega 1280", "Arduino Mega 2560" };
		deviceMap.put(devices[0], "atmega328p");
		deviceMap.put(devices[1], "atmega1280");
		deviceMap.put(devices[2], "atmega2560");
		deviceComboBox = new JComboBox<String>(devices);

		toolsDir = new File("./src/main/resources/tools");
		if (!toolsDir.exists()) {
			System.out.println("Cannot locate avrdude");
		}
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> commPorts = CommPortIdentifier.getPortIdentifiers();
		Vector<String> commModel = new Vector<String>();
		while (commPorts.hasMoreElements()) {
			CommPortIdentifier commPort = commPorts.nextElement();
			if (commPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (commPort.getName().startsWith("tty")) {
					// linux
					commModel.add("/dev/" + commPort.getName());
				} else {
					// windoze
					commModel.add(commPort.getName());
				}
			}
		}
		portComboBox = new JComboBox<String>( commModel.toArray(new String[0]));
		portComboBox1 = new JComboBox<String>( commModel.toArray(new String[0]));

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWidgets();
		this.pack();
		this.setVisible(true);
	}

	private void addWidgets() {
		JTabbedPane tabPane = new JTabbedPane();
		this.add(tabPane, BorderLayout.CENTER);
		// upload to arduinos
		JPanel uploadPanel = new JPanel();
		uploadPanel.setLayout(new BorderLayout());
		uploadPanel.add(uploadingPanel, BorderLayout.CENTER);
		JPanel westPanel = new JPanel(new MigLayout());

		westPanel.add(new JLabel("Select comm port:"));

		westPanel.add(portComboBox, "wrap");

		westPanel.add(new JLabel("Select device:"),"gap unrelated");

		westPanel.add(deviceComboBox, "wrap");

		hexFileChooser.setApproveButtonText("Upload");
		hexFileChooser.setAcceptAllFileFilterUsed(false);
		hexFileChooser.addChoosableFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "*.hex - Hex file";
			}

			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				if (f.getName().toUpperCase().endsWith(".HEX"))
					return true;
				return false;
			}
		});
		westPanel.add(hexFileChooser, "span, wrap");

		uploadPanel.add(westPanel, BorderLayout.WEST);
		tabPane.addTab("Upload", uploadPanel);

		// charts
		JPanel chartPanel = new JPanel();
		chartPanel.setLayout(new BorderLayout());
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Charts", "tiff", "kap", "KAP", "TIFF", "tif", "TIF");
		chartFileChooser.setFileFilter(filter);
		chartPanel.add(chartFileChooser, BorderLayout.WEST);
		chartPanel.add(processingPanel, BorderLayout.CENTER);
		chartPanel.add(result, BorderLayout.SOUTH);
		tabPane.addTab("Charts", chartPanel);

		// IMU calibration
		JPanel calPanel = new JPanel();
		calPanel.setLayout(new BorderLayout());
		JPanel westCalPanel = new JPanel(new MigLayout());

		westCalPanel.add(new JLabel("Select comm port:"));

		westCalPanel.add(portComboBox1, "wrap");
		JButton startCal = new JButton("Start");
		startCal.addActionListener(new ActionListener() {
			 
            public void actionPerformed(ActionEvent e)
            {
                calibrationPanel.process((String)portComboBox.getSelectedItem());
            }
        });      
		westCalPanel.add(startCal);
		JButton stopCal = new JButton("Stop");
		stopCal.addActionListener(new ActionListener() {
			 
            public void actionPerformed(ActionEvent e)
            {
                calibrationPanel.stopProcess();
            }
        }); 
		westCalPanel.add(stopCal);
		
		calPanel.add(westCalPanel, BorderLayout.WEST);
		calPanel.add(calibrationPanel, BorderLayout.CENTER);
		calPanel.add(result, BorderLayout.SOUTH);
		tabPane.addTab("Calibration", calPanel);

	}

	class HexFileChooser extends JFileChooser {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void approveSelection() {
			final File f = hexFileChooser.getSelectedFile();

			new Thread() {

				@Override
				public void run() {
					String device = deviceMap.get(deviceComboBox.getSelectedItem());
					uploadingPanel.process(f, (String) portComboBox.getSelectedItem(), device);
				}

			}.start();
		}

		@Override
		public void cancelSelection() {
			uploadingPanel.clear();
			result.setText(title);
		}
	}
	
	
	class ChartFileChooser extends JFileChooser {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void approveSelection() {
			final File f = chartFileChooser.getSelectedFile();

			new Thread() {

				@Override
				public void run() {
					processingPanel.process(f);
				}

			}.start();
		}

		@Override
		public void cancelSelection() {
			processingPanel.clear();
			result.setText(title);
		}
	}

	class ProcessingPanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JTextArea textArea = new JTextArea();
		private JScrollPane scrollPane;

		public ProcessingPanel() {
			this.setPreferredSize(new Dimension(500, 700));
			scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(480, 680));
			textArea.setBorder(BorderFactory.createLineBorder(Color.black, 1));
			this.add(scrollPane);
		}

		public boolean process(File f) {
			// one at a time
			chartFileChooser.setEnabled(false);
			System.out.println("Processing " + f.getAbsolutePath());
			try {
				ChartProcessor processor = new ChartProcessor(true, textArea);
				redirectSystemStreams();
				processor.processChart(f, true);

			} catch (Exception e) {
				System.out.print(e.getMessage() + "\n");
				e.printStackTrace();
				return false;
			} finally {
				chartFileChooser.setEnabled(true);
			}
			return true;
		}

		public void clear() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					textArea.setText("");
				}
			});

		}

		private void updateTextArea(final String text) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					textArea.append(text);
				}
			});
		}

		private void redirectSystemStreams() {
			OutputStream out = new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					updateTextArea(String.valueOf((char) b));
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					updateTextArea(new String(b, off, len));
				}

				@Override
				public void write(byte[] b) throws IOException {
					write(b, 0, b.length);
				}
			};

			System.setOut(new PrintStream(out, true));
			System.setErr(new PrintStream(out, true));
		}

	}

	class LoadingPanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JTextArea textArea = new JTextArea();
		private JScrollPane scrollPane;

		public LoadingPanel() {
			this.setPreferredSize(new Dimension(500, 700));
			scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(480, 680));
			textArea.setBorder(BorderFactory.createLineBorder(Color.black, 1));
			this.add(scrollPane);
		}

		public boolean process(File f, String commPort, String device) {
			// one at a time
			chartFileChooser.setEnabled(false);
			System.out.println("Processing " + f.getAbsolutePath());
			try {
				UploadProcessor processor = new UploadProcessor(true, textArea);
				redirectSystemStreams();
				System.out.println("Uploading "+f.getAbsolutePath()+" to "+device+" on "+commPort +", tools at"+toolsDir);
				processor.processUpload(f, commPort, device, toolsDir.getAbsolutePath());

			} catch (Exception e) {
				System.out.print(e.getMessage() + "\n");
				e.printStackTrace();
				return false;
			} finally {
				chartFileChooser.setEnabled(true);
			}
			return true;
		}

		public void clear() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					textArea.setText("");
				}
			});

		}

		private void updateTextArea(final String text) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					textArea.append(text);
				}
			});
		}

		private void redirectSystemStreams() {
			OutputStream out = new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					updateTextArea(String.valueOf((char) b));
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					updateTextArea(new String(b, off, len));
				}

				@Override
				public void write(byte[] b) throws IOException {
					write(b, 0, b.length);
				}
			};

			System.setOut(new PrintStream(out, true));
			System.setErr(new PrintStream(out, true));
		}

	}

	class CalibratePanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JTextArea textArea = new JTextArea();
		private JScrollPane scrollPane;
		private CalibrateProcessor processor;
		

		public CalibratePanel() {
			this.setPreferredSize(new Dimension(500, 700));
			scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(480, 680));
			textArea.setBorder(BorderFactory.createLineBorder(Color.black, 1));
			this.add(scrollPane);
		}

		public void stopProcess() {
			if(processor!=null){
				try {
					System.out.print("Attempting to stop..\n");
					processor.stopRawData();
					//processor=null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(rawBuffer.toString().length()<10){
				System.out.println("Did not collect enough data, try again..\n"+rawBuffer.toString());
				return;
			}

			int[] offsets = new int[6];
			float[] scale = new float[6];
			double [][] fullCompensation = processor.calculate(rawBuffer.toString());
			offsets[0]=(int) fullCompensation[0][0];
			offsets[1]=(int) fullCompensation[1][0];
			offsets[2]=(int) fullCompensation[2][0];
			offsets[3]=(int) fullCompensation[3][0];
			offsets[4]=(int) fullCompensation[4][0];
			offsets[5]=(int) fullCompensation[5][0];
			scale[0]=(float) fullCompensation[0][1];
			scale[1]=(float) fullCompensation[1][1];
			scale[2]=(float) fullCompensation[2][1];
			scale[3]=(float) fullCompensation[3][1];
			scale[4]=(float) fullCompensation[4][1];
			scale[5]=(float) fullCompensation[5][1];
			//format for IMU
			// (int16_t) acc_off_x, acc_off_y, acc_off_z, magn_off_x, magn_off_y, magn_off_z;
			// (float) acc_scale_x, acc_scale_y, acc_scale_z, magn_scale_x, magn_scale_y, magn_scale_z;
			processor.saveToDevice(offsets, scale);
			
		}

		public boolean process(String commPortStr) {
			// one at a time
			//System.out.println("Processing " + f.getAbsolutePath());
			try {
				processor = new CalibrateProcessor(true, textArea);
				redirectSystemStreams();
				rawBuffer=new StringBuffer();
				processor.connect(CommPortIdentifier.getPortIdentifier(commPortStr),rawBuffer);

			} catch (Exception e) {
				System.out.print(e.getMessage() + "\n");
				e.printStackTrace();
				return false;
			} 
			return true;
		}

		public void clear() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					textArea.setText("");
				}
			});

		}

		private void updateTextArea(final String text) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					textArea.append(text);
				}
			});
		}

		private void redirectSystemStreams() {
			OutputStream out = new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					updateTextArea(String.valueOf((char) b));
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					updateTextArea(new String(b, off, len));
				}

				@Override
				public void write(byte[] b) throws IOException {
					write(b, 0, b.length);
				}
			};

			System.setOut(new PrintStream(out, true));
			System.setErr(new PrintStream(out, true));
		}

	}

	public static void main(String[] args) {
		System.out.println("Current dir:" + new File(".").getAbsolutePath());
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				new ChartManager("Chart Manager").setVisible(true);
			}
		});
	}
}
