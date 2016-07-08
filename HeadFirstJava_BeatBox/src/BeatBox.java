import java.awt.*;
import java.io.*;
import java.nio.file.DirectoryStream;

import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;

public class BeatBox {
	JPanel mainPanel;
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	JFrame theFrame;

	String[] instrumentNames = { "Bass Drum", "Snare Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
			"Crash Cymbal", "Hand Clap", "Hi Tom", "Hi Bongo", "Marakas", "Whistle", "Low Conga", "Cowbell",
			"Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga" };
	int[] instruments = { 36, 38, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };

	public static void main(String[] args) {
		File saveDir = new File("Beats");
		if (saveDir.exists())
			;
		else {
			try {
				saveDir.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		new BeatBox().buildGUI();
	}

	public void buildGUI() {
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		checkboxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);

		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);

		JButton serializeIt = new JButton("SerializeIt");
		serializeIt.addActionListener(new serializeListener());
		buttonBox.add(serializeIt);

		JButton restore = new JButton("restore");
		restore.addActionListener(new MyReadInListener());
		buttonBox.add(restore);

		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < 17; i++) {
			nameBox.add(new Label(instrumentNames[i]));
		}

		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);

		theFrame.getContentPane().add(background);

		GridLayout grid = new GridLayout(17, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		for (int i = 0; i < 272; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		setUpMidi();

		theFrame.setBounds(50, 50, 300, 300);
		theFrame.pack();
		theFrame.setVisible(true);
	}

	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void buildTrackAndStart() {
		int[] trackList = null;

		sequence.deleteTrack(track);
		track = sequence.createTrack();

		for (int i = 0; i < 17; i++) {
			trackList = new int[16];

			int key = instruments[i];

			for (int j = 0; j < 16; j++) {
				JCheckBox jc = checkboxList.get(j + 16 * i);
				if (jc.isSelected()) {
					trackList[j] = key;
				} else {
					trackList[j] = 0;
				}
			}

			makeTracks(trackList);
			track.add(makeEvent(176, 1, 127, 0, 16));
		}

		track.add(makeEvent(192, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class MyStartListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			buildTrackAndStart();
		}
	}

	public class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
		}
	}

	public class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 1.03));
		}
	}

	public class MyDownTempoListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 0.97));
		}

	}

	public void makeTracks(int[] list) {
		for (int i = 0; i < 16; i++) {
			int key = list[i];

			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}

	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return event;

	}

	public class serializeListener implements ActionListener {

		JFrame frame;
		JTextField nameField;
		Label label;
		JButton button;

		public void actionPerformed(ActionEvent a) {

			boolean[] checkboxState = new boolean[272];

			frame = new JFrame();
			nameField = new JTextField(10);
			label = new Label("저장할 이름을 쓰시오");
			button = new JButton("저장");
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					try {
						FileOutputStream fileStream = new FileOutputStream(
								new File("Beats" + File.separator + nameField.getText() + ".bbx"));
						ObjectOutputStream os = new ObjectOutputStream(fileStream);
						os.writeObject(checkboxState);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					frame.dispose();

				}

			});

			frame.setLayout(new FlowLayout());
			frame.getContentPane().add(label);
			frame.getContentPane().add(nameField);
			frame.getContentPane().add(button);

			frame.setLocation(330, 50);
			frame.pack();
			frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);

			for (int i = 0; i < 272; i++) {
				JCheckBox check = checkboxList.get(i);
				checkboxState[i] = check.isSelected();
			}

		}
	}

	public class MyReadInListener implements ActionListener {

		JFileChooser fc;

		public void actionPerformed(ActionEvent a) {
			boolean[] checkboxState = null;

			fc = new JFileChooser("Beats");
			int val = fc.showOpenDialog(null);

			if (val == fc.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				try {
					FileInputStream fileIn = new FileInputStream(file);
					ObjectInputStream is = new ObjectInputStream(fileIn);
					checkboxState = (boolean[]) is.readObject();
				} catch (Exception e) {
					e.printStackTrace();
				}
				for (int i = 0; i < 272; i++) {
					JCheckBox check = (JCheckBox) checkboxList.get(i);
					check.setSelected(checkboxState[i]);
				}
				sequencer.stop();
				buildTrackAndStart();
			}

		}

	}
}
