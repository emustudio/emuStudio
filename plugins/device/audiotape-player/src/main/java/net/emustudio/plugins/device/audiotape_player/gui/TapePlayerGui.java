package net.emustudio.plugins.device.audiotape_player.gui;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.border.*;

import net.emustudio.emulib.runtime.interaction.BrowseButton;
import net.emustudio.emulib.runtime.interaction.CachedComboBoxModel;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.ShortenedString;
import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;
import net.miginfocom.swing.*;

public class TapePlayerGui extends JDialog {
    private final static String FOLDER_OPEN_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/folder-open.png";
    private final static String PLAY_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-start.png";
    private final static String STOP_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-stop.png";
    private final static String EJECT_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-eject.png";
    private final static String REFRESH_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/view-refresh.png";
    private final static String LOAD_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/applications-multimedia.png";

    private final JPanel panelTapeInfo = new JPanel();
    private final JButton btnBrowse;
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnLoad = new JButton("Load");
    private final CachedComboBoxModel<ShortenedString<Path>> cmbDirsModel = new CachedComboBoxModel<>();
    private final JComboBox<ShortenedString<Path>> cmbDirs = new JComboBox<>(cmbDirsModel);
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final JList<String> lstTapes = new JList<>(lstTapesModel);
    private final JScrollPane scrollTapes = new JScrollPane(lstTapes);

    private final AtomicReference<ShortenedString<Path>> loadedFileName = new AtomicReference<>();

    private final JButton btnPlay = new JButton("Play");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnEject = new JButton("Eject");

    private final JTextArea txtFileName = new JTextArea("N/A");
    private final JLabel lblStatus = new JLabel("Stopped");

    private final JTextArea txtEvents = new JTextArea();

    private final TapePlaybackController controller;

	public TapePlayerGui(JFrame parent, Dialogs dialogs, TapePlaybackController controller) {
		super(parent, "Audio Tape Player");
        Objects.requireNonNull(dialogs);
        this.controller = Objects.requireNonNull(controller);

        btnBrowse = new BrowseButton(dialogs, "Select Directory", "Select", p -> {
            ShortenedString<Path> ps = new ShortenedString<>(p, Path::toString);
            ps.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth());
            cmbDirsModel.add(ps);
            cmbDirs.setSelectedIndex(0);
        });
        btnBrowse.setIcon(new ImageIcon(getClass().getResource(FOLDER_OPEN_ICON)));
        btnBrowse.setText("");
        btnBrowse.setToolTipText("Select directory");
        btnBrowse.setFocusPainted(false);

		initComponents();
        setupListeners();
        setCassetteState(controller.getState());
	}

    public void addProgramDetail(String program, String detail) {
        txtEvents.append("\n" + program + ": " + detail);
    }

    public void addPulseInfo(String pulse) {
        txtEvents.append("\n" + pulse);
    }

    public void setCassetteState(TapePlaybackController.CassetteState state) {
        this.lblStatus.setText(state.name());
        switch (state) {
            case CLOSED:
                txtEvents.setText("");
                btnLoad.setEnabled(false);
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnEject.setEnabled(false);
                break;

            case PLAYING:
                txtEvents.setText("");
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(false);
                btnEject.setEnabled(true);
                btnStop.setEnabled(true);
                break;

            case STOPPED:
                btnStop.setEnabled(false);
                btnLoad.setEnabled(true);
                btnEject.setEnabled(true);
                btnPlay.setEnabled(true);
                break;

            case UNLOADED:
                txtEvents.setText("");
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(true);
                btnEject.setEnabled(false);
                loadedFileName.set(null);
                txtFileName.setToolTipText("");
                txtFileName.setText("N/A");
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void setupListeners() {
        cmbDirs.addActionListener(e -> {
            ShortenedString<Path> path = (ShortenedString<Path>) cmbDirs.getSelectedItem();
            if (path != null) {
                lstTapesModel.reset(path.getValue());
            }
        });
        cmbDirs.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ShortenedString<Path> path = (ShortenedString<Path>) cmbDirs.getSelectedItem();

                if (path != null) {
                    // ComboBox has 2 components: text area and drop-down button. We need to eliminate button width.
                    // From observation, the drop-down button width is 36 pixels.
                    path.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth() - 36);
                    String dirName = path.getValue().toString();
                    if (dirName.length() > path.getMaxStringLength()) {
                        cmbDirs.setToolTipText(dirName);
                    } else {
                        cmbDirs.setToolTipText(null);
                    }
                    revalidate();
                }
            }
        });

        panelTapeInfo.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ShortenedString<Path> ps = loadedFileName.get();
                if (ps != null) {
                    ps.deriveMaxStringLength(panelTapeInfo);
                    String shortened = ps.getShortenedString();
                    if (shortened.length() < ps.getValue().toString().length()) {
                        txtFileName.setToolTipText(ps.getValue().toString());
                    }
                    txtFileName.setText(shortened);
                }
            }
        });

        lstTapes.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = lstTapes.locationToIndex(e.getPoint());
                if (index > -1) {
                    String fileName = lstTapesModel.getFilePath(index).getFileName().toString();
                    if (!fileName.equals(lstTapesModel.elementAt(index))) {
                        lstTapes.setToolTipText(fileName);
                    } else {
                        lstTapes.setToolTipText(null);
                    }
                }
            }
        });
        lstTapes.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                lstTapesModel.resize(e.getComponent(), scrollTapes.getViewportBorderBounds().width);
            }
        });

        btnRefresh.addActionListener(e -> lstTapesModel.refresh());
        btnLoad.addActionListener(e -> {
            int index = lstTapes.getSelectedIndex();
            if (index != -1) {
                Path path = lstTapesModel.getFilePath(index);
                controller.load(path);

                ShortenedString<Path> ps = new ShortenedString<>(path, p -> p.getFileName().toString());
                loadedFileName.set(ps);
                ps.deriveMaxStringLength(panelTapeInfo);
                txtFileName.setText(ps.getShortenedString());
            }
        });
        btnPlay.addActionListener(e -> controller.play());
        btnStop.addActionListener(e -> controller.stop(false));
        btnEject.addActionListener(e -> controller.stop(true));
    }

	private void initComponents() {
		JPanel panelAvailableTapes = new JPanel();
		JPanel panelDirs = new JPanel();
		JToolBar toolbarAvailableTapes = new JToolBar();
		JPanel panelTape = new JPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, panelAvailableTapes, panelTape);

		JLabel lblFileNameLabel = new JLabel("File name:");
		JLabel lblStatusLabel = new JLabel("Status:");
		JScrollPane scrollEvents = new JScrollPane(txtEvents);
		JToolBar toolbarTape = new JToolBar();
		JPanel hSpacer1 = new JPanel(null);
        JPanel hSpacer2 = new JPanel(null);

		Container contentPane = getContentPane();
		contentPane.setLayout(new MigLayout("insets dialog, fill", "[fill]", "[fill]"));

        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(0.3);

        panelAvailableTapes.setBorder(new TitledBorder("Available tapes"));
        panelAvailableTapes.setLayout(new MigLayout("fill,insets 2", "[fill, grow]"));

        panelDirs.setLayout(new MigLayout("fillx, hidemode 3", "[grow, fill][]", "[]"));
        panelDirs.add(cmbDirs);
        JToolBar btnBrowseToolBar = new JToolBar();
        btnBrowseToolBar.add(btnBrowse);
        btnBrowseToolBar.setFloatable(false);
        panelDirs.add(btnBrowseToolBar);

        btnRefresh.setIcon(new ImageIcon(getClass().getResource(REFRESH_ICON)));
        btnLoad.setIcon(new ImageIcon(getClass().getResource(LOAD_ICON)));

        toolbarAvailableTapes.add(btnRefresh);
        toolbarAvailableTapes.add(hSpacer2);
        toolbarAvailableTapes.add(btnLoad);
        toolbarAvailableTapes.setFloatable(false);

        lstTapes.setCellRenderer(new TapesListRenderer());

        panelAvailableTapes.add(panelDirs, "cell 0 0, growx, pushx");
        panelAvailableTapes.add(scrollTapes, "cell 0 1, growy, pushy");
        panelAvailableTapes.add(toolbarAvailableTapes, "cell 0 2");
        splitPane.setLeftComponent(panelAvailableTapes);

        panelTape.setBorder(new TitledBorder("Audio Tape"));
        panelTape.setLayout(new MigLayout("fill,insets 2,debug", "[fill]", "[][][]"));

        btnPlay.setIcon(new ImageIcon(getClass().getResource(PLAY_ICON)));
        btnStop.setIcon(new ImageIcon(getClass().getResource(STOP_ICON)));
        btnEject.setIcon(new ImageIcon(getClass().getResource(EJECT_ICON)));

        toolbarTape.add(btnPlay);
        toolbarTape.add(btnStop);
        toolbarTape.addSeparator();
        toolbarTape.add(hSpacer1);
        toolbarTape.add(btnEject);
        toolbarTape.setFloatable(false);

        lblStatus.setFont(lblStatus.getFont().deriveFont(lblStatus.getFont().getStyle() | Font.BOLD));

        txtFileName.setEditable(false);
        txtFileName.setLineWrap(true);
        txtFileName.setBackground(UIManager.getColor("Panel.background"));

        panelTapeInfo.setLayout(new MigLayout("fillx, debug","[][fill]", "[][]"));
        panelTapeInfo.add(lblFileNameLabel, "cell 0 0,alignx right,growx 0");
        panelTapeInfo.add(txtFileName, "cell 1 0, growx");
        panelTapeInfo.add(lblStatusLabel, "cell 0 1,alignx right,growx 0");
        panelTapeInfo.add(lblStatus, "cell 1 1,growx");

        panelTape.add(panelTapeInfo, "cell 0 0");
        panelTape.add(scrollEvents, "cell 0 1, growy, pushy");
        panelTape.add(toolbarTape, "cell 0 2");
        splitPane.setRightComponent(panelTape);

		contentPane.add(splitPane, "cell 0 0, push, grow");

		pack();
		setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}
}
