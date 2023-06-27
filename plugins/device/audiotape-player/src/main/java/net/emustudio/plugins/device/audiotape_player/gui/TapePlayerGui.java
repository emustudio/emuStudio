package net.emustudio.plugins.device.audiotape_player.gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.border.*;

import net.emustudio.emulib.runtime.interaction.BrowseButton;
import net.emustudio.emulib.runtime.interaction.CachedComboBoxModel;
import net.emustudio.emulib.runtime.interaction.Dialogs;
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
    private final CachedComboBoxModel<PathString> cmbDirsModel = new CachedComboBoxModel<>();
    private final JComboBox<PathString> cmbDirs = new JComboBox<>(cmbDirsModel);
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final JList<String> lstTapes = new JList<>(lstTapesModel);
    private final JScrollPane scrollTapes = new JScrollPane(lstTapes);

    private final AtomicReference<PathString> loadedFileName = new AtomicReference<>();

    private final JButton btnPlay = new JButton("Play");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnEject = new JButton("Eject");

    private final JLabel lblFileName = new JLabel("N/A");
    private final JLabel lblStatus = new JLabel("Stopped");

    private final JTextArea txtEvents = new JTextArea();

    private final TapePlaybackController controller;

	public TapePlayerGui(JFrame parent, Dialogs dialogs, TapePlaybackController controller) {
		super(parent, "Audio Tape Player");
        Objects.requireNonNull(dialogs);
        this.controller = Objects.requireNonNull(controller);

        btnBrowse = new BrowseButton(dialogs, "Select Directory", "Select", p -> {
            PathString ps = new PathString(p, true);
            ps.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth());
            cmbDirsModel.add(ps);
            cmbDirs.setSelectedIndex(0);
        });
        btnBrowse.setIcon(new ImageIcon(getClass().getResource(FOLDER_OPEN_ICON)));
        btnBrowse.setText("");
        btnBrowse.setToolTipText("Select directory");

		initComponents();
        setupListeners();
        setCassetteState(controller.getState());
	}

    public void addProgramDetail(String program, String detail) {
        txtEvents.setText(txtEvents.getText() + "\n" + program + ": " + detail);
    }

    public void addPulseInfo(String pulse) {
        JLabel pulseLabel = new JLabel(pulse);
        pulseLabel.setFont(pulseLabel.getFont().deriveFont(Font.ITALIC));
        txtEvents.setText(txtEvents.getText() + "\n" + pulseLabel);
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
                lblFileName.setToolTipText("");
                lblFileName.setText("N/A");
                break;
        }
    }

    private void setupListeners() {
        cmbDirs.addActionListener(e -> {
            PathString path = (PathString) cmbDirs.getSelectedItem();
            if (path != null) {
                lstTapesModel.reset(path.getPath());
            }
        });
        cmbDirs.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                PathString path = (PathString) cmbDirs.getSelectedItem();

                if (path != null) {
                    path.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth());
                    String dirName = path.getPath().toString();
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
                PathString ps = loadedFileName.get();
                if (ps != null) {
                    ps.deriveMaxStringLength(panelTapeInfo, panelTapeInfo.getWidth());
                    String shortened = ps.getPathShortened();
                    if (shortened.length() < ps.getPath().toString().length()) {
                        lblFileName.setToolTipText(ps.getPath().toString());
                    }
                    lblFileName.setText(shortened);
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

                PathString ps = new PathString(path);
                loadedFileName.set(ps);
                ps.deriveMaxStringLength(panelTapeInfo, panelTapeInfo.getWidth());
                lblFileName.setText(ps.getPathShortened());
            }
        });
        btnPlay.addActionListener(e -> controller.play());
        btnStop.addActionListener(e -> controller.stop(false));
        btnEject.addActionListener(e -> controller.stop(true));
    }

	private void initComponents() {
		JPanel panelAvailableTapes = new JPanel();
		JToolBar toolbarDirs = new JToolBar();
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

        toolbarDirs.add(cmbDirs);
        toolbarDirs.add(btnBrowse);
        toolbarDirs.setFloatable(false);

        btnRefresh.setIcon(new ImageIcon(getClass().getResource(REFRESH_ICON)));
        btnLoad.setIcon(new ImageIcon(getClass().getResource(LOAD_ICON)));

        toolbarAvailableTapes.add(btnRefresh);
        toolbarAvailableTapes.add(hSpacer2);
        toolbarAvailableTapes.add(btnLoad);
        toolbarAvailableTapes.setFloatable(false);

        lstTapes.setCellRenderer(new TapesListRenderer());

        panelAvailableTapes.add(toolbarDirs, "cell 0 0, growx, pushx");
        panelAvailableTapes.add(scrollTapes, "cell 0 1, growy, pushy");
        panelAvailableTapes.add(toolbarAvailableTapes, "cell 0 2");
        splitPane.setLeftComponent(panelAvailableTapes);

        panelTape.setBorder(new TitledBorder("Audio Tape"));
        panelTape.setLayout(new MigLayout("fill,insets 2,hidemode 3", "[fill]", "[][][]"));

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

        panelTapeInfo.setLayout(new MigLayout("fillx,hidemode 3","[fill][fill]", "[][]"));
        panelTapeInfo.add(lblFileNameLabel, "cell 0 0,alignx right,growx 0");
        panelTapeInfo.add(lblFileName, "cell 1 0,push, grow");
        panelTapeInfo.add(lblStatusLabel, "cell 0 1,alignx right,growx 0");
        panelTapeInfo.add(lblStatus, "cell 1 1,pushx,growx");

        panelTape.add(panelTapeInfo, "cell 0 0");
        panelTape.add(scrollEvents, "cell 0 1, growy, pushy");
        panelTape.add(toolbarTape, "cell 0 2");
        splitPane.setRightComponent(panelTape);

		contentPane.add(splitPane, "cell 0 0");

		pack();
		setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
}
