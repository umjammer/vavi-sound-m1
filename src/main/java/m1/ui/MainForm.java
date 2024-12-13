/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.StreamReader;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Cpuexec;
import m1.emu.Inptport;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.RomInfo;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.UI;
import m1.emu.Video;
import m1.mame.capcom.Capcom;
import m1.mame.cps.CPS;
import m1.mame.dataeast.Dataeast;
import m1.mame.konami68000.Konami68000;
import m1.mame.m72.M72;
import m1.mame.m92.M92;
import m1.mame.namcos1.Namcos1;
import m1.mame.neogeo.Neogeo;
import m1.mame.taito.Taito;
import m1.mame.taitob.Taitob;
import m1.mame.tehkan.Tehkan;
import m1.sound.Sound;
import m1.sound.WavWrite;
import vavi.swing.binding.table.TableModel;
import vavix.util.screenscrape.annotation.WebScraper;

import static java.lang.System.getLogger;


/**
 * @author shunninghuang@gmail.com
 */
public class MainForm extends JFrame {

    private static final Logger logger = getLogger(MainForm.class.getName());

    public EmuTimer ui_timer;
    public Atime ui_time;
    private int iTime, nTime;
    private boolean bWaiting;
    public int iInt1, iInt5;
    private JMenuItem[] itemSize;
    private LoadForm loadform;
    public String sSelect;
    public boolean bLoading;
    public static Thread t1;
    private final Preferences prefs = Preferences.userNodeForPackage(MainForm.class);

    public MainForm() {
        initializeComponent();
    }

    public void ui_init() throws IOException {
        this.setTitle("JavaM1: " + Machine.sDescription + " [" + Machine.sName + "]");
        bLoading = true;
        LoadTrack();
        ui_time = new Atime(0, (long) (1e18 / 10));
        ui_timer = Timer.allocCommon(this::ui_callback, "timer1_tick", false);
        Timer.adjustPeriodic(ui_timer, ui_time, Attotime.ATTOTIME_NEVER);
    }

    private String SecondToTime(int i1) {
        String sTime;
        int iMinute, iSecond;
        iMinute = i1 / 60;
        iSecond = i1 % 60;
        sTime = "%02d".formatted(iMinute) + ":" + "%02d".formatted(iSecond);
        return sTime;
    }

    private void btnRun_Click(ActionEvent e) {
        if (btnRun.getText().equals("run")) {
            OnRunning(true);
        } else if (btnRun.getText().equals("pause")) {
            OnRunning(false);
        }
    }

    public void run() {
try {
logger.log(Level.INFO, "thread start");
        Mame.soft_reset();
logger.log(Level.INFO, "Mame.exit_pending: " + Mame.exit_pending);
        while (!Mame.exit_pending) {
logger.log(Level.INFO, "thread loop: " + Mame.paused);
            if (!Mame.paused) {
logger.log(Level.INFO, "cpuexec_timeslice");
                Cpuexec.cpuexec_timeslice();
            } else {
logger.log(Level.INFO, "video_frame_update");
                Video.video_frame_update();
            }
        }
} catch (Throwable t) {
 logger.log(Level.ERROR, t.getMessage(), t);
}
logger.log(Level.INFO, "exit run");
    }

    private final WindowListener Form1_Load = new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
            sSelect = prefs.get("sSelect", null);

            setTitle(Version.build_version);
            RomInfo.rom = new RomInfo();
            Keyboard.InitializeInput(MainForm.this);
            iInt1 = 0;
            InitLoadForm();
            loadform.model.update();
            OnLoaded(false);
        }
    };

//    final MouseWheelListener lbTrack_MouseWheel = new MouseAdapter() {
//        @Override
//        public void mouseWheelMoved(MouseWheelEvent e) {
//        }
//    };

    public void LoadRom() throws IOException {
        Timer.lt = new ArrayList<>();
        sSelect = RomInfo.rom.name;
        Machine.FORM = this;
        Machine.rom = RomInfo.rom;
        Machine.sName = Machine.rom.name;
        Machine.sParent = Machine.rom.parent;
        Machine.sBoard = Machine.rom.board;
        Machine.sDirection = Machine.rom.direction;
        Machine.sDescription = Machine.rom.description;
        Machine.sManufacturer = Machine.rom.manufacturer;
        Machine.lsParents = RomInfo.getParents(Machine.sName);
        RomInfo.iStop = Short.parseShort(RomInfo.rom.m1Stop);
        int i;
        iInt5 = 5;
        switch (Machine.sBoard) {
            case "CPS-1":
            case "CPS-1(QSound)":
            case "CPS2":
                Video.nMode = 3;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("512x512");
                itemSize[1].setText("512x256");
                itemSize[2].setText("384x224");
                itemSelect();
                CPS.CPSInit();
                //CPS.GDIInit();
                break;
            case "Data East":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("256x224");
                Video.iMode = 0;
                itemSelect();
                Dataeast.DataeastInit();
                //Dataeast.GDIInit();
                switch (Machine.sName) {
                    case "pcktgal":
                        iInt5 = 15;
                        break;
                }
                break;
            case "Tehkan":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("256x224");
                Video.iMode = 0;
                itemSelect();
                Tehkan.PbactionInit();
                //Tehkan.GDIInit();
                break;
            case "Neo Geo":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("320x224");
                Video.iMode = 0;
                itemSelect();
                Neogeo.NeogeoInit();
                //Neogeo.GDIInit();
                break;
            case "Namco System 1":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("288x224");
                Video.iMode = 0;
                itemSelect();
                Namcos1.Namcos1Init();
                //Namcos1.GDIInit();
                break;
            case "M72":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("384x256");
                Video.iMode = 0;
                itemSelect();
                M72.M72Init();
                //M72.GDIInit();
                break;
            case "M92":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("320x240");
                Video.iMode = 0;
                itemSelect();
                M92.M92Init();
                //M92.GDIInit();
                switch (Machine.sName) {
                    case "rtypeleo":
                        iInt5 = 10;
                        break;
                }
                break;
            case "Taito":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("256x224");
                Video.iMode = 0;
                itemSelect();
                Taito.TaitoInit();
                //Taito.GDIInit();
                switch (Machine.sName) {
                    case "bublbobl":
                        iInt5 = 10;
                        break;
                }
                break;
            case "Taito B":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("320x224");
                Video.iMode = 0;
                itemSelect();
                Taitob.TaitobInit();
                //Taitob.GDIInit();
                break;
            case "Konami 68000":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("288*224");
                Video.iMode = 0;
                itemSelect();
                Konami68000.Konami68000Init();
                //Konami68000.GDIInit();
                break;
            case "Capcom":
                Video.nMode = 1;
                itemSize = new JMenuItem[Video.nMode];
                for (i = 0; i < Video.nMode; i++) {
                    itemSize[i] = new JMenuItem();
                    itemSize[i].addActionListener(this::itemsizeJMenuItem_Click);
                }
                itemSize[0].setText("384*224");
                Video.iMode = 0;
                itemSelect();
                Capcom.CapcomInit();
                //Capcom.GDIInit();
                break;
        }
        if (Machine.bRom) {
            Mame.init_machine();
            //Generic.nvram_load();
            OnLoaded(true);
        } else {
            OnLoaded(false);
logger.log(Level.INFO, "error rom");
            JOptionPane.showMessageDialog(null, "error rom", "error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void InitLoadForm() {
        loadform = new LoadForm(this);

        loadform.model = new TableModel<>(RomInfo.rom);
        loadform.model.bind(loadform.table);

        try {
            WebScraper.Util.foreach(RomInfo.class, RomInfo.romList::add);
logger.log(Level.INFO, "m1.xml: " + RomInfo.romList.size());
//RomInfo.romList.forEach(System.err::println);
//            loadform.model.addTableModelListener(e -> {
//logger.log(Level.INFO, "table: " + loadform.model.getRowCount() + ", " + loadform.table.getRowCount());
//            });
            loadform.model.update();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean isWindowForeground;

    public static boolean isWindowForeground() {
        return isWindowForeground;
    }

    private WindowListener Form1_FormClosing = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent event) {
            try {
                if (Machine.bRom) {
                    UI.cpurun();
                }
                Mame.exit_pending = true;
                Thread.sleep(100);
                //Generic.nvram_save();

                prefs.put("sSelect", sSelect);
            } catch (Exception e) {
                logger.log(Level.TRACE, e.getMessage(), e);
            }
        }
        @Override
        public void windowActivated(WindowEvent e) {
            isWindowForeground = true;
        }
        @Override
        public void windowDeactivated(WindowEvent e) {
            isWindowForeground = false;
        }
    };

    private void OnPlaying(Mame.PlayState state) {
        switch (state) {
            case PLAY_STOPPED:
            case RECORD_STOPPED:
                btnPlay.setText("play");
                btnRecord.setText("record");
                btnPlay.setEnabled(true);
                btnRun.setEnabled(true);
                lbTrack.setEnabled(true);
                btnRecord.setEnabled(true);
                break;
            case PLAY_STOPPING:
                btnPlay.setText("play");
                btnPlay.setEnabled(false);
                break;
            case PLAY_START:
            case PLAY_RUNNING:
            case PLAY_CONTINUOUSSTART:
            case PLAY_CONTINUOUSSTART2:
            case PLAY_CONTINUOUS:
            case PLAY_NEXT:
            case PLAY_NEXT2:
                btnPlay.setText("stop");
                btnRecord.setEnabled(false);
                break;
            case RECORD_START:
            case RECORD_RECORDING:
                btnRecord.setText("stop");
                btnPlay.setEnabled(false);
                btnRun.setEnabled(false);
                lbTrack.setEnabled(false);
                break;
        }
    }

    private void OnRunning(boolean b1) {
        if (b1) {
            Mame.mame_pause(false);
            btnRun.setText("pause");
        } else {
            Mame.mame_pause(true);
            btnRun.setText("run");
        }
    }

    public void ui_callback() {
        if (bWaiting) {
            return;
        }
        if (Mame.playState == Mame.PlayState.PLAY_STOPPING) {
            iTime++;
            if (Sound.nZero >= iInt5) {
                tbMsg1.setText("00:00");
                Mame.playState = Mame.PlayState.PLAY_STOPPED;
                OnPlaying(Mame.playState);
            } else {
                if (iTime % 10 == 0) {
                    tbMsg1.setText(SecondToTime(iTime / 10));
                }
            }
        } else if (Mame.playState == Mame.PlayState.PLAY_START) {
            iTime++;
            if (Sound.nZero == 0) {
                Mame.playState = Mame.PlayState.PLAY_RUNNING;
            }
        } else if (Mame.playState == Mame.PlayState.RECORD_START) {
            iTime++;
            if (Sound.nZero == 0) {
                Mame.playState = Mame.PlayState.RECORD_RECORDING;
            }
        } else if (Mame.playState == Mame.PlayState.PLAY_CONTINUOUSSTART) {
            iTime++;
            if (Sound.nZero == 0) {
                Mame.playState = Mame.PlayState.PLAY_CONTINUOUS;
            }
        } else if (Mame.playState == Mame.PlayState.PLAY_RUNNING || Mame.playState == Mame.PlayState.RECORD_RECORDING) {
            if ((Sound.nZero >= iInt5 && nTime == 10000) || iTime == nTime + iInt1) {
                Sound.nZero = 0;
                Sound.stoptrack.accept(TrackInfo.StopTrack);
                if (Mame.playState == Mame.PlayState.RECORD_RECORDING) {
                    try {
                        WavWrite.closeSoundFile();
                    } catch (IOException e) {
                        logger.log(Level.ERROR, e.getMessage(), e);
                    }
                }
                tbMsg1.setText("00:00");
                Mame.playState = Mame.PlayState.PLAY_STOPPED;
                OnPlaying(Mame.playState);
            } else {
                iTime++;
                if (iTime % 10 == 0) {
                    tbMsg1.setText(SecondToTime(iTime / 10));
                }
            }
        } else if (Mame.playState == Mame.PlayState.PLAY_CONTINUOUSSTART2) {
            bWaiting = true;
            TrackInfo ti = TrackInfo.CurTrack;
            iTime = 0;
            nTime = (int) (ti.Second * 10);
            lbTrack.setSelectedIndex(ti.IndexList);
            tbMsg1.setText("00:00");
            tbMsg2.setText(ti.Message);
            if (Inptport.lsFA.isEmpty()) {
                Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART;
            }
            bWaiting = false;
        } else if (Mame.playState == Mame.PlayState.PLAY_NEXT) {
            TrackInfo.GetNext();
            Mame.playState = Mame.PlayState.PLAY_NEXT2;
        } else if (Mame.playState == Mame.PlayState.PLAY_NEXT2) {
            TrackInfo ti;
            ti = TrackInfo.CurTrack;
            if (ti != null) {
                TrackInfo.CurTrack = ti;
                iTime = 0;
                nTime = (int) (ti.Second * 10);
                lbTrack.setSelectedIndex(ti.IndexList);
                tbMsg1.setText("00:00");
                tbMsg2.setText(ti.Message);
                Sound.playtrack.accept(ti);
                Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART;
            } else {
                tbMsg1.setText("00:00");
                Mame.playState = Mame.PlayState.PLAY_STOPPED;
                Sound.stoptrack.accept(TrackInfo.StopTrack);
            }
        } else if (Mame.playState == Mame.PlayState.PLAY_CONTINUOUS) {
            if ((Sound.nZero >= iInt5 && nTime == 10000) || iTime == nTime + iInt1) {
                Sound.stoptrack.accept(TrackInfo.StopTrack);
                Mame.playState = Mame.PlayState.PLAY_NEXT;
            } else {
                iTime++;
                if (iTime % 10 == 0) {
                    tbMsg1.setText(SecondToTime(iTime / 10));
                }
            }
        }
        if (Timer.global_basetime.attoseconds == 0) {
            tbSecond.setText(String.valueOf(Timer.global_basetime.seconds));
        }
        Timer.adjustPeriodic(ui_timer, ui_time, Attotime.ATTOTIME_NEVER);
    }

    public void LoadTrack() throws IOException {
        tbMsg2.setText(null);
        model.clear();
        TrackInfo.lTI = new ArrayList<>();
        String s1;
        StreamReader sr1 = new StreamReader(new FileStream(Path.of("lists", "en", Machine.sName + ".lst").toString(), FileMode.Open), StandardCharsets.UTF_8);
        do {
            s1 = sr1.readLine();
        } while (!s1.equals("$main"));
        while (true) {
            s1 = sr1.readLine();
//logger.log(Level.DEBUG, s1);
            Pattern p = Pattern.compile("^(.+)&lt;(.+)&gt;$");
            if (s1.equals("$end")) {
                break;
            } else {
                Matcher m1 = p.matcher(s1);
                if (m1.matches()) {
                    String s3;
//logger.log(Level.DEBUG, m1.groupCount());
                    s3 = m1.group(1) + "<" + m1.group(2) + ">";
                    s1 = s3;
                }
            }
//logger.log(Level.DEBUG, s1);
            if (s1.matches("^#.*")) {
                short u1;
                int i1;
                String s2, s3 = "", s4 = "";
                Matcher m11, m12;
                TrackInfo ti1 = null;
                m11 = Pattern.compile("^#([0-9]{2,4}) (.*) <time=\"([^\"]+)\".*>$").matcher(s1);
                m12 = Pattern.compile("^#([0-9]{2,4}) (.*)$").matcher(s1);
                if (m11.matches()) {
                    if (m11.groupCount() == 3) {
                        u1 = Short.parseShort(m11.group(1));
                        s2 = m11.group(2);
                        s3 = m11.group(3);
                        s4 = m11.group(1) + " " + m11.group(2);
                        i1 = model.size();
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                } else if (m12.matches()) {
                    if (m12.groupCount() == 2) {
                        u1 = Short.parseShort(m12.group(1));
                        s2 = m12.group(2);
                        s3 = "";
                        s4 = m12.group(1) + " " + m12.group(2);
                        i1 = model.size();
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                }
                TrackInfo.lTI.add(ti1);
                model.addElement(s4);
            } else if (s1.matches("^\\$.*")) {
                short u1;
                int i1;
                String s2, s3 = "", s4 = "";
                Matcher m21, m22;
                TrackInfo ti1 = null;
                m21 = Pattern.compile("^\\$([0-9a-fA-F]{2,3}) (.*) <time=\"([^\"]+)\".*>$").matcher(s1);
                m22 = Pattern.compile("^\\$([0-9a-fA-F]{2,3}) (.*)$").matcher(s1);
                if (m21.matches()) {
                    if (m21.groupCount() == 4) {
                        u1 = Short.parseShort(m21.group(1), 16);
                        s2 = m21.group(2);
                        s3 = m21.group(3);
                        s4 = m21.group(1) + " " + m21.group(2);
                        i1 = model.size();
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                } else if (m22.matches()) {
                    if (m22.groupCount() == 3) {
                        u1 = Short.parseShort(m22.group(1), 16);
                        s2 = m22.group(2);
                        s3 = "";
                        s4 = m22.group(1) + " " + m22.group(2);
                        i1 = model.size();
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                }
                TrackInfo.lTI.add(ti1);
                model.addElement(s4);
            } else if (s1.matches("^//.*")) {

            } else {
                model.addElement(s1); // TODO " " works, but "" will not be inserted
            }
        }
        TrackInfo.StopTrack = new TrackInfo(RomInfo.iStop, "stop", "", "", model.size());
        sr1.close();
    }

    public void OnLoaded(boolean b1) {
//logger.log(Level.INFO, "OnLoaded: " + b1);
        if (b1) {
            btnPlay.setEnabled(true);
            btnRun.setEnabled(true);
            btnRecord.setEnabled(true);
            lbTrack.setEnabled(true);
            if (bLoading) {
                bLoading = false;
                OnRunning(true);
logger.log(Level.INFO, "thread run");
                t1 = new Thread(this::run);
                t1.start();
            }
        } else {
            btnPlay.setEnabled(false);
            btnRun.setEnabled(false);
            btnRecord.setEnabled(false);
            lbTrack.setEnabled(false);
        }
    }

    private final MouseListener lbTrack_DoubleClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {
                TrackInfo ti;
                ti = TrackInfo.GetTrackByIndexList(lbTrack.getSelectedIndex());
logger.log(Level.DEBUG, "track info: " + ti);
                if (ti != null) {
                    bWaiting = true;
                    if (Mame.playState == Mame.PlayState.PLAY_STOPPED) {
                        Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART2;
                        OnPlaying(Mame.playState);
                        Sound.playtrack.accept(ti);
                    } else {
                        Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART2;
                        Sound.stopandplaytrack.accept(ti);
                    }
                    TrackInfo.CurTrack = ti;
                    bWaiting = false;
logger.log(Level.DEBUG, "Mame.playState: " + Mame.playState);
                }
            }
        }
    };

    private void exitJMenuItem_Click(ActionEvent e) {
        this.setVisible(false);
    }

//    protected void WndProc(/* ref */ Message msg) {
//        if (msg.Msg == 0x0112) {
//            if (msg.WParam.toString("X4") == "F100") {
//                if (Keyboard.bF10) {
//                    Keyboard.bF10 = false;
//                    return;
//                }
//            }
//        }
//        // Pass message to default handler.
//        base.WndProc(/* ref */ msg);
//    }

    private void aboutJMenuItem_Click(ActionEvent e) {
        AboutForm about1 = new AboutForm();
        about1.setVisible(true);
    }

    private void itemsizeJMenuItem_Click(ActionEvent e) {
        int i, n;
        n = itemSize.length;
        for (i = 0; i < n; i++) {
            itemSize[i].setSelected(false);
        }
        for (i = 0; i < n; i++) {
            if (itemSize[i] == e.getSource()) {
                Video.iMode = i;
                itemSelect();
                break;
            }
        }
    }

    private void itemSelect() {
        itemSize[Video.iMode].setSelected(true);
        switch (Machine.sBoard) {
            case "CPS-1":
            case "CPS-1(QSound)":
            case "CPS2":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 0;
                    Video.width = 512;
                    Video.height = 512;
                } else if (Video.iMode == 1) {
                    Video.offsetx = 0;
                    Video.offsety = 256;
                    Video.width = 512;
                    Video.height = 256;
                } else if (Video.iMode == 2) {
                    Video.offsetx = 64;
                    Video.offsety = 272;
                    Video.width = 384;
                    Video.height = 224;
                }
                break;
            case "Data East":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 16;
                    Video.width = 256;
                    Video.height = 224;
                }
                break;
            case "Tehkan":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 16;
                    Video.width = 256;
                    Video.height = 224;
                }
                break;
            case "Neo Geo":
                if (Video.iMode == 0) {
                    Video.offsetx = 30;
                    Video.offsety = 16;
                    Video.width = 320;
                    Video.height = 224;
                }
                break;
            case "SunA8":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 16;
                    Video.width = 256;
                    Video.height = 224;
                }
                break;
            case "Namco System 1":
                if (Video.iMode == 0) {
                    Video.offsetx = 73;
                    Video.offsety = 16;
                    Video.width = 288;
                    Video.height = 224;
                }
                break;
            case "IGS011":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 0;
                    Video.width = 512;
                    Video.height = 240;
                }
                break;
            case "PGM":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 0;
                    Video.width = 448;
                    Video.height = 224;
                }
                break;
            case "M72":
                if (Video.iMode == 0) {
                    Video.offsetx = 64;
                    Video.offsety = 0;
                    Video.width = 384;
                    Video.height = 256;
                }
                break;
            case "M92":
                if (Video.iMode == 0) {
                    Video.offsetx = 80;
                    Video.offsety = 8;
                    Video.width = 320;
                    Video.height = 240;
                }
                break;
            case "Taito":
                if (Video.iMode == 0) {
                    switch (Machine.sName) {
                        case "tokio":
                        case "tokioo":
                        case "tokiou":
                        case "tokiob":
                        case "bublbobl":
                        case "bublbobl1":
                        case "bublboblr":
                        case "bublboblr1":
                        case "boblbobl":
                        case "sboblbobl":
                        case "sboblbobla":
                        case "sboblboblb":
                        case "sboblbobld":
                        case "sboblboblc":
                        case "bub68705":
                        case "dland":
                        case "bbredux":
                        case "bublboblb":
                        case "bublcave":
                        case "boblcave":
                        case "bublcave11":
                        case "bublcave10":
                            Video.offsetx = 0;
                            Video.offsety = 16;
                            Video.width = 256;
                            Video.height = 224;
                            break;
                        case "opwolf":
                        case "opwolfa":
                        case "opwolfj":
                        case "opwolfu":
                        case "opwolfb":
                        case "opwolfp":
                            Video.offsetx = 0;
                            Video.offsety = 8;
                            Video.width = 320;
                            Video.height = 240;
                            break;
                    }
                }
                break;
            case "Taito B":
                if (Video.iMode == 0) {
                    Video.offsetx = 0;
                    Video.offsety = 16;
                    Video.width = 320;
                    Video.height = 224;
                }
                break;
            case "Konami 68000":
                if (Video.iMode == 0) {
                    switch (Machine.sName) {
                        case "cuebrick":
                        case "mia":
                        case "mia2":
                        case "tmnt2":
                        case "tmnt2a":
                        case "tmht22pe":
                        case "tmht24pe":
                        case "tmnt22pu":
                        case "qgakumon":
                            Video.offsetx = 104;
                            Video.offsety = 16;
                            Video.width = 304;
                            Video.height = 224;
                            break;
                        case "tmnt":
                        case "tmntu":
                        case "tmntua":
                        case "tmntub":
                        case "tmht":
                        case "tmhta":
                        case "tmhtb":
                        case "tmntj":
                        case "tmnta":
                        case "tmht2p":
                        case "tmht2pa":
                        case "tmnt2pj":
                        case "tmnt2po":
                        case "lgtnfght":
                        case "lgtnfghta":
                        case "lgtnfghtu":
                        case "trigon":
                        case "blswhstl":
                        case "blswhstla":
                        case "detatwin":
                            Video.offsetx = 96;
                            Video.offsety = 16;
                            Video.width = 320;
                            Video.height = 224;
                            break;
                        case "punkshot":
                        case "punkshot2":
                        case "punkshotj":
                        case "glfgreat":
                        case "glfgreatj":
                        case "ssriders":
                        case "ssriderseaa":
                        case "ssridersebd":
                        case "ssridersebc":
                        case "ssridersuda":
                        case "ssridersuac":
                        case "ssridersuab":
                        case "ssridersubc":
                        case "ssridersadd":
                        case "ssridersabd":
                        case "ssridersjad":
                        case "ssridersjac":
                        case "ssridersjbd":
                        case "thndrx2":
                        case "thndrx2a":
                        case "thndrx2j":
                        case "prmrsocr":
                        case "prmrsocrj":
                            Video.offsetx = 112;
                            Video.offsety = 16;
                            Video.width = 288;
                            Video.height = 224;
                            break;
                    }
                }
                break;
            case "Capcom":
                if (Video.iMode == 0) {
                    switch (Machine.sName) {
                        case "gng":
                        case "gnga":
                        case "gngbl":
                        case "gngprot":
                        case "gngblita":
                        case "gngc":
                        case "gngt":
                        case "makaimur":
                        case "makaimurc":
                        case "makaimurg":
                        case "diamond":
                            Video.offsetx = 0;
                            Video.offsety = 16;
                            Video.width = 256;
                            Video.height = 224;
                            break;
                        case "sf":
                        case "sfua":
                        case "sfj":
                        case "sfjan":
                        case "sfan":
                        case "sfp":
                            Video.offsetx = 64;
                            Video.offsety = 16;
                            Video.width = 384;
                            Video.height = 224;
                            break;
                    }
                }
                break;
        }
        //ResizeMain();
    }

    private void btnPlay_Click(ActionEvent e) {
        if (btnPlay.getText().equals("play")) {
            TrackInfo ti;
            if (lbTrack.getSelectedValue() != null) {
                ti = TrackInfo.GetTrackByIndexList(lbTrack.getSelectedIndex());
                if (ti != null) {
                    bWaiting = true;
                    Mame.playState = Mame.PlayState.PLAY_START;
                    tbMsg1.setText("00:00");
                    tbMsg2.setText(ti.Message);
                    iTime = 0;
                    nTime = (int) (ti.Second * 10);
                    TrackInfo.CurTrack = ti;
                    Sound.nZero = 0;
                    Sound.playtrack.accept(ti);
                    OnPlaying(Mame.playState);
                    bWaiting = false;
                }
            }
        } else if (btnPlay.getText().equals("stop")) {
            Mame.playState = Mame.PlayState.PLAY_STOPPING;
            Sound.stoptrack.accept(TrackInfo.StopTrack);
            OnPlaying(Mame.playState);
        }
    }

    private void btnRecord_Click(ActionEvent e) {
        if (btnRecord.getText().equals("record")) {
            TrackInfo ti;
            if (lbTrack.getSelectedValue() != null) {
                ti = TrackInfo.GetTrackByIndexList(lbTrack.getSelectedIndex());
                if (ti != null) {
                    bWaiting = true;
                    WavWrite.createSoundFile("waves\\" + RomInfo.rom.name + "-" + "%03d".formatted(ti.TrackID) + ".wav");
                    Mame.playState = Mame.PlayState.RECORD_START;
                    tbMsg1.setText("00:00");
                    tbMsg2.setText(ti.Message);
                    iTime = 0;
                    nTime = (int) (ti.Second * 10);
                    TrackInfo.CurTrack = ti;
                    Sound.nZero = 0;
                    Sound.playtrack.accept(ti);
                    OnPlaying(Mame.playState);
                    bWaiting = false;
                }
            }
        } else if (btnRecord.getText().equals("stop")) {
            try {
                Sound.stoptrack.accept(TrackInfo.StopTrack);
                WavWrite.closeSoundFile();
                Mame.playState = Mame.PlayState.PLAY_STOPPED;
                OnPlaying(Mame.playState);
            } catch (IOException ex) {
                logger.log(Level.DEBUG, ex.getMessage(), ex);
            }
        }
    }

    private void loadJMenuItem_Click(ActionEvent e) {
        Mame.exit_pending = true;
        if (TrackInfo.CurTrack != null) {
            Sound.stoptrack.accept(TrackInfo.StopTrack);
        }
        if (Machine.bRom) {
            UI.cpurun();
            Mame.mame_pause(true);
        }
        Mame.playState = Mame.PlayState.PLAY_STOPPED;
        OnPlaying(Mame.playState);
        OnRunning(false);
logger.log(Level.DEBUG, "sSelect: " + sSelect);
        for (int i = 0; i < loadform.model.getRowCount(); i++) {
//logger.log(Level.TRACE, "sSelect: " + sSelect + ", " + loadform.model.getValueAt(i, 2));
            if (sSelect.equals(loadform.model.getValueAt(i, 2))) {
                loadform.table.getSelectionModel().setSelectionInterval(i, i);
                Rectangle cellRectangle = loadform.table.getCellRect(loadform.table.getSelectedRow(), 0, true);
                loadform.table.scrollRectToVisible(cellRectangle);
                break;
            }
        }

        loadform.bLoad = false;
        loadform.setVisible(true);
    }

//#region Windows Form Designer generated code

    /**
     * Required method for Designer support - do not modify
     * the contents of this method with the code editor.
     */
    private void initializeComponent() {
        this.menuStrip1 = new JMenuBar();
        this.fileJMenu = new JMenu();
        this.loadJMenuItem = new JMenuItem();
        this.exitJMenuItem = new JMenuItem();
        this.helpJMenu = new JMenu();
        this.aboutJMenuItem = new JMenuItem();

        this.btnRecord = new JButton();
        this.tbMsg1 = new JTextField();
        this.tbMsg2 = new JTextField();
        this.tbSecond = new JTextArea();
        this.lbTrack = new JList<>();
        this.btnPlay = new JButton();
        this.btnRun = new JButton();
        //
        // menuStrip1
        //
        this.menuStrip1.add(this.fileJMenu);
        this.menuStrip1.add(this.helpJMenu);
        this.menuStrip1.setName("menuStrip1");
        //
        // fileJMenu
        //
        this.fileJMenu.add(this.loadJMenuItem);
        this.fileJMenu.add(this.exitJMenuItem);
        this.fileJMenu.setText("File");
        //
        // loadJMenuItem
        //
        this.loadJMenuItem.setText("Load");
        this.loadJMenuItem.addActionListener(this::loadJMenuItem_Click);
        //
        // exitJMenuItem
        //
        this.exitJMenuItem.setText("Exit");
        this.exitJMenuItem.addActionListener(this::exitJMenuItem_Click);
        //
        // helpJMenu
        //
        this.helpJMenu.add(this.aboutJMenuItem);
        this.helpJMenu.add(this.aboutJMenuItem);
        this.helpJMenu.setText("Help");
        //
        // aboutJMenuItem
        //
        this.aboutJMenuItem.setText("About");
        this.aboutJMenuItem.addActionListener(this::aboutJMenuItem_Click);
        //
        // btnRecord
        //
        this.btnRecord.setText("record");
        this.btnRecord.addActionListener(this::btnRecord_Click);
        //
        // tbMsg1
        //
        this.tbMsg1.setColumns(20);
        //
        // tbMsg2
        //
        this.tbMsg2.setColumns(20);
        //
        // tbSecond
        //
        this.tbSecond.setColumns(20);
        this.tbSecond.setRows(10);
        //
        // lbTrack
        //
        model = new DefaultListModel<>();
        this.lbTrack.setModel(model);
        this.lbTrack.setName("lbTrack");
        this.lbTrack.addMouseListener(this.lbTrack_DoubleClick);
        //
        // btnPlay
        //
        this.btnPlay.setText("play");
        this.btnPlay.addActionListener(this::btnPlay_Click);
        //
        // btnRun
        //
        this.btnRun.setName("btnRun");
        this.btnRun.setText("run");
        this.btnRun.addActionListener(this::btnRun_Click);
        //
        // MainForm
        //
        JPanel left = new JPanel();
        left.setLayout(new FlowLayout());
        left.add(tbMsg1);
        left.add(tbMsg2);
        left.add(tbSecond);
        left.add(btnPlay);
        left.add(btnRun);
        left.add(btnRecord);

        JScrollPane right = new JScrollPane();
        right.setViewportView(this.lbTrack);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(left);
        splitPane.setRightComponent(right);
        splitPane.setPreferredSize(new Dimension(624, 662));

        this.setContentPane(splitPane);
        this.setJMenuBar(this.menuStrip1);
        this.setName("MainForm");
        this.setTitle("...");
        this.addWindowListener(this.Form1_Load);
        this.addWindowListener(this.Form1_FormClosing);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.pack();

        splitPane.setDividerLocation(0.4);
    }

//#endregion

    DefaultListModel<String> model;
    private JMenuBar menuStrip1;
    private JMenu fileJMenu;
    private JMenuItem loadJMenuItem;
    private JMenuItem exitJMenuItem;
    private JMenu helpJMenu;
    private JMenuItem aboutJMenuItem;
    private JButton btnRecord;
    public JTextField tbMsg1;
    public JTextField tbMsg2;
    private JTextArea tbSecond;
    public JList<String> lbTrack;
    private JButton btnPlay;
    private JButton btnRun;
}
