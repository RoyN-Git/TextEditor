package editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditor extends JFrame {
	private JTextField SearchField;
	private JCheckBox regexp;
	private JFileChooser fileChooser;
	private String openedFile;
	private JPanel upperPanel;
	private JTextArea textArea;
	private boolean useRegexp;
	private long begin;
	private static final int WIDTH = 410;
	private static final int HEIGHT = 410;
	private static final int FRAME_PADDING = 20;
	public TextEditor() {
		super("Text Editor");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		//fileChooser
		fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		fileChooser.setName("FileChooser");
		add(fileChooser);

		setSize(WIDTH, HEIGHT);
		useRegexp = false;
		begin = 0;
		openedFile = "";
		initComponents();

		setVisible(true);
	}
	private BiFunction createRegexpSearchFunc() {
		BiFunction <String, String, Integer[]> regexpFunc = (String text, String goal) -> {
			Pattern pattern = Pattern.compile(goal);
			Matcher m = pattern.matcher(text);
			Integer[] beginEnd = new Integer[2];
			for (int i = 0; i <= begin; i++) {
				if (!m.find()) {
					if (begin == 0) {
						beginEnd[0] = -2;
						beginEnd[1] = -2;
					}
					else {
						beginEnd[0] = -1;
						beginEnd[1] = -1;
					}
					return beginEnd;
				}
			}
			beginEnd[0] = m.start();
			beginEnd[1] = m.end();
			return beginEnd;
		};
		return regexpFunc;
	}
	private BiFunction createSearchFunc() {
		BiFunction <String, String, Integer[]> searchFunc = (String text, String goal) -> {
			int tempBegin = -1;
			Integer [] beginEnd = new Integer[2];
			for (int i = 0; i <= begin; i++) {
				if (!text.substring(tempBegin + 1).contains(goal)) {
					if (begin == 0) {
						beginEnd[0] = -2;
						beginEnd[1] = -2;
					}
					else {
						beginEnd[0] = -1;
						beginEnd[1] = -1;
					}
					return beginEnd;
				}
				tempBegin += text.substring(tempBegin + 1).indexOf(goal);
				tempBegin += 1;
			}
			if (tempBegin != -1 )
				beginEnd[0] = tempBegin;
			else
				beginEnd[0] = 0;
			beginEnd[1] = tempBegin + goal.length();
			return beginEnd;
		};
		return searchFunc;
	}
	private void initSearchIcons(BiFunction <String, String, Integer[]> searchFunc, BiFunction <String, String, Integer[]> regexpFunc) {
		JButton search = new JButton(new ImageIcon("icons/search.png"));
		search.setName("StartSearchButton");
		initIcons (search, "icons/search.png", "icons/searchLight.png");
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				begin = 0;
				if (!useRegexp)
					searchInText(searchFunc);
				else
					searchInText(regexpFunc);
			}
		});
		upperPanel.add(search);

		JButton leftArrow = new JButton(new ImageIcon("icons/arrowLeft.png"));
		leftArrow.setName("PreviousMatchButton");
		initIcons(leftArrow, "icons/arrowLeftDark.png", "icons/arrowLeftLight.png");
		leftArrow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					begin--;
					if (!useRegexp)
						searchInText(searchFunc);
					else
						searchInText(regexpFunc);
			}
		});
		upperPanel.add(leftArrow);

		JButton rightArrow = new JButton(new ImageIcon("icons/arrowRight.png"));
		rightArrow.setName("NextMatchButton");
		initIcons(rightArrow, "icons/arrowRightDark.png", "icons/arrowRightLight.png");
		rightArrow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				begin++;
				if (!useRegexp)
					searchInText(searchFunc);
				else
					searchInText(regexpFunc);

			}
		});
		upperPanel.add(rightArrow);

		regexp = new JCheckBox("Use regexp");
		regexp.setName("UseRegExCheckbox");
		regexp.setContentAreaFilled(false);

		rightArrow.setName("NextMatchButton");
		upperPanel.add(regexp);
		regexp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				useRegexp = !useRegexp;
			}
		});
	}
	private void initUpperPanel() {
		BiFunction <String, String, Integer[]> searchFunc = createSearchFunc();
		BiFunction <String, String, Integer[]> regexpFunc = createRegexpSearchFunc();
		//panel
		upperPanel = new JPanel();
		upperPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		upperPanel.setComponentOrientation(
				ComponentOrientation.LEFT_TO_RIGHT);
		upperPanel.setBackground(new Color(204, 220, 253));
		upperPanel.setBorder(new EmptyBorder(FRAME_PADDING, FRAME_PADDING, 0, FRAME_PADDING));

		//button save
		JButton save = new JButton(new ImageIcon("icons/diskette.png"));
		save.setName("SaveButton");
		initIcons(save, "icons/disketteDark.png", "icons/disketteLight.png");
		ActionListener listenSave = e -> saveFile();
		save.addActionListener(listenSave);
		upperPanel.add(save);

		//button load
		JButton load = new JButton(new ImageIcon("icons/loadBlue.png"));
		load.setName("OpenButton");
		initIcons(load, "icons/loadDark.png", "icons/loadLight.png");
		ActionListener listenLoad = e -> loadFile();
		load.addActionListener(listenLoad);
		upperPanel.add(load);

		//SearchField
		SearchField = new JTextField();
		SearchField.setName("SearchField");
		SearchField.setColumns(10);
		upperPanel.add(SearchField);
		initSearchIcons(createSearchFunc(), createRegexpSearchFunc());
		initMenu(createSearchFunc(), createRegexpSearchFunc());
		add(upperPanel, BorderLayout.NORTH);
	}
	private void searchInText( BiFunction <String, String, Integer[]> search) {
		Thread searching = new Thread() {
			@Override
			public void run() {
				String goal = SearchField.getText();
				String text = textArea.getText();
				Integer [] endBegin = new Integer[2];
				if (begin == -1) {
					endBegin[0] = 0;
					while (endBegin[0] != -1) {
						begin++;
						endBegin = search.apply(text, goal);
						if (endBegin[0] == -2) {
							return;
						}
					}
					begin--;
					endBegin = search.apply(text, goal);
				}
				else
				{
					endBegin = search.apply(text, goal);
					if (endBegin[0] == -2) {
						return ;
					}
					if (endBegin[0] == -1) {
						begin = 0;
						endBegin = search.apply(text, goal);
					}
				}
				textArea.setCaretPosition(0);
				textArea.select(endBegin[0], endBegin[1]);
				textArea.grabFocus();
			}
		};
		searching.start();
	}
	private void initIcons(JButton button, String darkIcon, String lightIcon) {
		button.setPreferredSize(new Dimension(24, 24));
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);
		button.setPressedIcon(new ImageIcon(darkIcon));
		button.setRolloverIcon (new ImageIcon(lightIcon));
	}
	private void initMenu(BiFunction <String, String, Integer[]> searchFunc, BiFunction <String, String, Integer[]> regexpFunc) {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu searchMenu = new JMenu("Search");

		fileMenu.setName("MenuFile");
		searchMenu.setName("MenuSearch");

		menuBar.add(fileMenu);
		menuBar.add(searchMenu);

		JMenuItem loadMenuItem = new JMenuItem("Open");
		loadMenuItem.setName("MenuOpen");
		JMenuItem saveMenuItem = new JMenuItem("Save");
		saveMenuItem.setName("MenuSave");
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setName("MenuExit");
		loadMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadFile();
			}
		});
		saveMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveFile();
			}
		});
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		fileMenu.add(loadMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(exitMenuItem);


		JMenuItem searchMenuItem = new JMenuItem("Start search");
		searchMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				begin = 0;
				if (!useRegexp)
					searchInText(searchFunc);
				else
					searchInText(regexpFunc);
			}
		});

		searchMenuItem.setName("MenuStartSearch");
		JMenuItem prevMenuItem = new JMenuItem("Previous match");
		prevMenuItem.setName("MenuPreviousMatch");
		prevMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				begin--;
				if (!useRegexp)
					searchInText(searchFunc);
				else
					searchInText(regexpFunc);
			}
		});
		JMenuItem nextMenuItem = new JMenuItem("Next match");
		nextMenuItem.setName("MenuNextMatch");

		nextMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				begin++;
				if (!useRegexp)
					searchInText(searchFunc);
				else
					searchInText(regexpFunc);
			}
		});

		JMenuItem regexpMenuItem = new JMenuItem("Use regular expressions");
		regexpMenuItem.setName("MenuUseRegExp");
		regexpMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				useRegexp = !useRegexp;
				regexp.doClick();
			}
		});
		searchMenu.add(regexpMenuItem);
		searchMenu.add(nextMenuItem);
		searchMenu.add(prevMenuItem);
		searchMenu.add(searchMenuItem);

		setJMenuBar(menuBar);
	}
	private void initComponents() {
		// panel for main text
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BorderLayout());
		textPanel.setBackground(new Color(204, 220, 253));
		textPanel.setBorder(new EmptyBorder(FRAME_PADDING, FRAME_PADDING, FRAME_PADDING, FRAME_PADDING));
		add(textPanel, BorderLayout.CENTER); //BorderLayout позволяет разместить компоненты в контейнере в соответствии со сторонами света

		//main text area
		textArea = new JTextArea(10, 20);
		textArea.setName("TextArea");

		//scroll pane
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setName("ScrollPane");
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		textPanel.add(scroll);

		initUpperPanel();
	}
	private void loadFile() {
		int returnValue = fileChooser.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			openedFile = selectedFile.getAbsolutePath();
		}
		byte[] fileBytes = null;
		try {
			fileBytes = Files.readAllBytes(Paths.get(openedFile));
		}
		catch(IOException e) {
			textArea.setText(null);
			openedFile = "";
			return ;
		}
		textArea.setText(null);
		textArea.setText(new String(fileBytes));
	}
	private void saveFile() {
		int returnValue = fileChooser.showSaveDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			openedFile = selectedFile.getAbsolutePath();
		}
		byte[] text = textArea.getText().getBytes(StandardCharsets.UTF_8);
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(Path.of(openedFile)))) {
			out.write(text, 0, text.length);
		} catch (IOException e) {
		}
	}
}

