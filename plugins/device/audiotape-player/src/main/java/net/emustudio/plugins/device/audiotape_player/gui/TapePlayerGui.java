package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.emulib.runtime.interaction.BrowseButton;
import net.emustudio.emulib.runtime.interaction.CachedComboBoxModel;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.ShortenedString;
import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

// https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
public class TapePlayerGui extends JDialog {
    private final static String FOLDER_OPEN_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/folder-open.png";
    private final static String PLAY_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-start.png";
    private final static String STOP_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-stop.png";
    private final static String EJECT_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-eject.png";
    private final static String REFRESH_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/view-refresh.png";
    private final static String LOAD_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/applications-multimedia.png";

    private final JPanel panelTapeInfo = new JPanel();
    private final JButton btnBrowse;
    private final JButton btnRefresh = new JButton("Refresh", new ImageIcon(ClassLoader.getSystemResource(REFRESH_ICON)));
    private final JButton btnLoad = new JButton("Load", new ImageIcon(ClassLoader.getSystemResource(LOAD_ICON)));
    private final CachedComboBoxModel<ShortenedString<Path>> cmbDirsModel = new CachedComboBoxModel<>();
    private final JComboBox<ShortenedString<Path>> cmbDirs = new JComboBox<>(cmbDirsModel);
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final JList<String> lstTapes = new JList<>(lstTapesModel);
    private final JScrollPane scrollTapes = new JScrollPane(lstTapes);

    private final AtomicReference<ShortenedString<Path>> loadedFileName = new AtomicReference<>();

    private final JButton btnPlay = new JButton("Play", new ImageIcon(ClassLoader.getSystemResource(PLAY_ICON)));
    private final JButton btnStop = new JButton("Stop", new ImageIcon(ClassLoader.getSystemResource(STOP_ICON)));
    private final JButton btnEject = new JButton("Eject", new ImageIcon(ClassLoader.getSystemResource(EJECT_ICON)));

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
            ps.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth() - 36);
            cmbDirsModel.add(ps);
            cmbDirs.setSelectedIndex(0);
            cmbDirs.setMinimumSize(new Dimension(0, 0));
        });
        btnBrowse.setIcon(new ImageIcon(ClassLoader.getSystemResource(FOLDER_OPEN_ICON)));
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

        cmbDirs.setMinimumSize(new Dimension(0, 0));
        panelDirs.setLayout(new MigLayout("fillx", "[fill, grow][]", "[]"));
        panelDirs.add(cmbDirs, "cell 0 0");
        JToolBar btnBrowseToolBar = new JToolBar();
        btnBrowseToolBar.add(btnBrowse);
        btnBrowseToolBar.setFloatable(false);
        panelDirs.add(btnBrowseToolBar, "cell 1 0");

        toolbarAvailableTapes.add(btnRefresh);
        toolbarAvailableTapes.add(hSpacer2);
        toolbarAvailableTapes.add(btnLoad);
        toolbarAvailableTapes.setFloatable(false);

        lstTapes.setCellRenderer(new TapesListRenderer());

        panelAvailableTapes.setBorder(new TitledBorder("Available tapes"));
        panelAvailableTapes.setLayout(new MigLayout("insets 2", "[grow]", "[][grow][]"));
        panelAvailableTapes.add(panelDirs, "cell 0 0, growx");
        panelAvailableTapes.add(scrollTapes, "cell 0 1, grow");
        panelAvailableTapes.add(toolbarAvailableTapes, "cell 0 2, growx");
        splitPane.setLeftComponent(panelAvailableTapes);

        toolbarTape.add(btnPlay);
        toolbarTape.add(btnStop);
        toolbarTape.addSeparator();
        toolbarTape.add(hSpacer1);
        toolbarTape.add(btnEject);
        toolbarTape.setFloatable(false);

        lblStatus.setFont(lblStatus.getFont().deriveFont(lblStatus.getFont().getStyle() | Font.BOLD));

        txtFileName.setEditable(false);
        txtFileName.setLineWrap(true);
        txtFileName.setMinimumSize(new Dimension(0, 0));
        txtFileName.setBackground(UIManager.getColor("Panel.background"));
        lblStatus.setMinimumSize(new Dimension(0, 0));

        panelTapeInfo.setLayout(new MigLayout("", "[][grow]", "[][]"));
        panelTapeInfo.add(lblFileNameLabel, "cell 0 0, alignx right");
        panelTapeInfo.add(txtFileName, "cell 1 0, growx");
        panelTapeInfo.add(lblStatusLabel, "cell 0 1, alignx right");
        panelTapeInfo.add(lblStatus, "cell 1 1, growx");

        panelTape.setBorder(new TitledBorder("Audio Tape"));
        panelTape.setLayout(new MigLayout("insets 2", "[grow]", "[][grow][]"));
        panelTape.add(panelTapeInfo, "cell 0 0, growx");
        panelTape.add(scrollEvents, "cell 0 1, grow");
        panelTape.add(toolbarTape, "cell 0 2, growx");
        splitPane.setRightComponent(panelTape);

        contentPane.add(splitPane, "push, grow");

        pack();
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
