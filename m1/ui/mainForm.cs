using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Globalization;
using System.Drawing.Imaging;
using System.IO;
using System.Text.RegularExpressions;
using System.Threading;
using System.Xml.Linq;
using Microsoft.DirectX.DirectSound;
using DSDevice = Microsoft.DirectX.DirectSound.Device;
using mame;

namespace ui
{
    public partial class mainForm : Form
    {
        public mame.Timer.emu_timer ui_timer;
        public Atime ui_time;
        private int iTime, nTime;
        private bool bWaiting;
        public int iInt1, iInt5;
        private ToolStripMenuItem[] itemSize;
        private loadForm loadform;
        public string sSelect;
        public bool bLoading;
        private DSDevice dev;
        private BufferDescription desc1;
        public static Thread t1;
        public string handle1;
        public mainForm()
        {
            InitializeComponent();
        }
        public void ui_init()
        {
            this.Text = "M1.NET: " + Machine.sDescription + " [" + Machine.sName + "]";
            bLoading = true;
            LoadTrack();
            ui_time = new Atime(0, (long)(1e18 / 10));
            ui_timer = mame.Timer.timer_alloc_common(ui_callback, "timer1_tick", false);
            mame.Timer.timer_adjust_periodic(ui_timer, ui_time, Attotime.ATTOTIME_NEVER);
        }
        private string SecondToTime(int i1)
        {
            string sTime;
            int iMinute, iSecond;
            iMinute = i1 / 60;
            iSecond = i1 % 60;
            sTime = iMinute.ToString("00") + ":" + iSecond.ToString("00");
            return sTime;
        }
        private void btnRun_Click(object sender, EventArgs e)
        {
            if (btnRun.Text == "run")
            {
                OnRunning(true);
            }
            else if (btnRun.Text == "pause")
            {
                OnRunning(false);
            }
        }
        public void run()
        {
            Mame.soft_reset();
            while (!Mame.exit_pending)
            {
                if (!Mame.paused)
                {
                    Cpuexec.cpuexec_timeslice();
                }
                else
                {
                    Video.video_frame_update();
                }
            }
        }
        private void Form1_Load(object sender, EventArgs e)
        {
            StreamReader sr1 = new StreamReader("m1.ini");
            sr1.ReadLine();
            sSelect = sr1.ReadLine();
            sr1.Close();
            this.Text = Version.build_version;
            lbTrack.MouseWheel += new MouseEventHandler(lbTrack_MouseWheel);
            Mame.handle1 = this.Handle;
            RomInfo.Rom=new RomInfo();
            dev = new DSDevice();
            dev.SetCooperativeLevel(this, CooperativeLevel.Normal);
            desc1 = new BufferDescription();
            desc1.Format = CreateWaveFormat();
            desc1.BufferBytes = 0x9400;
            desc1.ControlVolume = true;
            desc1.GlobalFocus = true;
            Keyboard.InitializeInput(this);
            Sound.buf2 = new SecondaryBuffer(desc1, dev);
            iInt1 = 0;            
            InitLoadForm();
            OnLoaded(false);
        }
        private void lbTrack_MouseWheel(object sender, MouseEventArgs e)
        {
            HandledMouseEventArgs ee = (HandledMouseEventArgs)e;
            ee.Handled = true;
        }
        public void LoadRom()
        {
            mame.Timer.lt = new List<mame.Timer.emu_timer>();
            sSelect = RomInfo.Rom.Name;
            Machine.FORM = this;
            Machine.rom = RomInfo.Rom;
            Machine.sName = Machine.rom.Name;
            Machine.sParent = Machine.rom.Parent;
            Machine.sBoard = Machine.rom.Board;
            Machine.sDirection = Machine.rom.Direction;
            Machine.sDescription = Machine.rom.Description;
            Machine.sManufacturer = Machine.rom.Manufacturer;
            Machine.lsParents = RomInfo.GetParents(Machine.sName);
            RomInfo.IStop = ushort.Parse(RomInfo.Rom.M1Stop);
            int i;
            iInt5 = 5;
            switch (Machine.sBoard)
            {
                case "CPS-1":
                case "CPS-1(QSound)":
                case "CPS2":
                    Video.nMode = 3;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "512x512";
                    itemSize[1].Text = "512x256";
                    itemSize[2].Text = "384x224";                 
                    itemSelect();
                    CPS.CPSInit();
                    //CPS.GDIInit();
                    break;
                case "Data East":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "256x224";
                    Video.iMode = 0;
                    itemSelect();
                    Dataeast.DataeastInit();
                    //Dataeast.GDIInit();
                    switch (Machine.sName)
                    {
                        case "pcktgal":
                            iInt5 = 15;
                            break;
                    }
                    break;
                case "Tehkan":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "256x224";
                    Video.iMode = 0;
                    itemSelect();
                    Tehkan.PbactionInit();
                    //Tehkan.GDIInit();
                    break;
                case "Neo Geo":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "320x224";                  
                    Video.iMode = 0;
                    itemSelect();
                    Neogeo.NeogeoInit();
                    //Neogeo.GDIInit();
                    break;
                case "Namco System 1":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "288x224";
                    Video.iMode = 0;
                    itemSelect();
                    Namcos1.Namcos1Init();
                    //Namcos1.GDIInit();
                    break;
                case "M72":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "384x256";
                    Video.iMode = 0;
                    itemSelect();
                    M72.M72Init();
                    //M72.GDIInit();
                    break;
                case "M92":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "320x240";
                    Video.iMode = 0;
                    itemSelect();
                    M92.M92Init();
                    //M92.GDIInit();
                    switch (Machine.sName)
                    {
                        case "rtypeleo":
                            iInt5 = 10;
                            break;
                    }
                    break;
                case "Taito":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "256x224";
                    Video.iMode = 0;
                    itemSelect();
                    Taito.TaitoInit();
                    //Taito.GDIInit();
                    switch (Machine.sName)
                    {
                        case "bublbobl":
                            iInt5 = 10;
                            break;
                    }
                    break;
                case "Taito B":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "320x224";
                    Video.iMode = 0;
                    itemSelect();
                    Taitob.TaitobInit();
                    //Taitob.GDIInit();
                    break;
                case "Konami 68000":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "288*224";
                    Video.iMode = 0;
                    itemSelect();
                    Konami68000.Konami68000Init();
                    //Konami68000.GDIInit();
                    break;
                case "Capcom":
                    Video.nMode = 1;
                    itemSize = new ToolStripMenuItem[Video.nMode];
                    for (i = 0; i < Video.nMode; i++)
                    {
                        itemSize[i] = new ToolStripMenuItem();
                        itemSize[i].Size = new Size(152, 22);
                        itemSize[i].Click += new EventHandler(itemsizeToolStripMenuItem_Click);
                    }
                    itemSize[0].Text = "384*224";
                    Video.iMode = 0;
                    itemSelect();
                    Capcom.CapcomInit();
                    //Capcom.GDIInit();
                    break;
            }
            if (Machine.bRom)
            {
                Mame.init_machine();
                //Generic.nvram_load();
                OnLoaded(true);
            }
            else
            {
                OnLoaded(false);
                MessageBox.Show("error rom");
            }
        }
        private void InitLoadForm()
        {
            loadform = new loadForm(this);

            ColumnHeader columnheader;
            columnheader = new ColumnHeader();
            columnheader.Text = "Title";
            columnheader.Width = 350;
            loadform.listView1.Columns.Add(columnheader);
            columnheader = new ColumnHeader();
            columnheader.Text = "Year";
            columnheader.Width = 60;
            loadform.listView1.Columns.Add(columnheader);
            columnheader = new ColumnHeader();
            columnheader.Text = "ROM";
            columnheader.Width = 80;
            loadform.listView1.Columns.Add(columnheader);
            columnheader = new ColumnHeader();
            columnheader.Text = "Parent";
            columnheader.Width = 60;
            loadform.listView1.Columns.Add(columnheader);
            /*columnheader = new ColumnHeader();
            columnheader.Text = "Direction";
            columnheader.Width = 70;
            loadform.listView1.Columns.Add(columnheader);*/
            columnheader = new ColumnHeader();
            columnheader.Text = "Manufacturer";
            columnheader.Width = 120;
            loadform.listView1.Columns.Add(columnheader);
            columnheader = new ColumnHeader();
            columnheader.Text = "Board";
            columnheader.Width = 120;
            loadform.listView1.Columns.Add(columnheader);
            IEnumerable<XElement> elements = XElement.Parse(mame.Properties.Resources.m1).Elements("game");
            showInfoByElements(elements);
        }
        private void showInfoByElements(IEnumerable<XElement> elements)
        {
            RomInfo.romList = new List<RomInfo>();
            //StreamWriter sw1 = new StreamWriter("1.txt", false);
            foreach (var ele in elements)
            {
                RomInfo rom = new RomInfo();
                rom.Name = ele.Attribute("name").Value;
                rom.Board = ele.Attribute("board").Value;
                rom.Parent = ele.Element("parent").Value;
                rom.Direction = ele.Element("direction").Value;
                rom.Description = ele.Element("description").Value;
                rom.Year = ele.Element("year").Value;
                rom.Manufacturer = ele.Element("manufacturer").Value;
                rom.M1Default = ele.Element("m1data").Attribute("default").Value;
                rom.M1Stop = ele.Element("m1data").Attribute("stop").Value;
                rom.M1Min = ele.Element("m1data").Attribute("min").Value;
                rom.M1Max = ele.Element("m1data").Attribute("max").Value;
                try
                {
                    rom.M1Subtype = ele.Element("m1data").Attribute("subtype").Value;
                }
                catch
                {
                    rom.M1Subtype = "";
                }
                RomInfo.romList.Add(rom);
                loadform.listView1.Items.Add(new ListViewItem(new string[] { rom.Description, rom.Year, rom.Name, rom.Parent, rom.Manufacturer, rom.Board }));
                //sw1.WriteLine(rom.Name + "\t" + rom.Board + "\t" + rom.Parent + "\t" + rom.Direction + "\t" + rom.Description + "\t" + rom.Year + "\t" + rom.Manufacturer);
                //sw1.WriteLine(rom.Description + " [" + rom.Name + "]");
                //sw1.WriteLine(rom.Name);
            }
            //sw1.Close();
        }
        private WaveFormat CreateWaveFormat()
        {
            WaveFormat format = new Microsoft.DirectX.DirectSound.WaveFormat();
            format.AverageBytesPerSecond = 192000;
            format.BitsPerSample = 16;
            format.BlockAlign = 4;
            format.Channels = 2;
            format.FormatTag = WaveFormatTag.Pcm;
            format.SamplesPerSecond = 48000;
            return format;
        }
        private void Form1_FormClosing(object sender, FormClosingEventArgs e)
        {
            if (Machine.bRom)
            {
                UI.cpurun();
            }
            Mame.exit_pending = true;
            Thread.Sleep(100);
            //Generic.nvram_save();
            StreamWriter sw1 = new StreamWriter("m1.ini", false);
            sw1.WriteLine("[select]");
            sw1.WriteLine(sSelect);
            sw1.Close();
        }
        private void OnPlaying(Mame.PlayState state)
        {
            switch (state)
            {
                case Mame.PlayState.PLAY_STOPPED:
                case Mame.PlayState.RECORD_STOPPED:
                    btnPlay.Text = "play";
                    btnRecord.Text = "record";
                    btnPlay.Enabled = true;
                    btnRun.Enabled = true;
                    lbTrack.Enabled = true;
                    btnRecord.Enabled = true;
                    break;
                case Mame.PlayState.PLAY_STOPPING:
                    btnPlay.Text = "play";
                    btnPlay.Enabled = false;
                    break;
                case Mame.PlayState.PLAY_START:
                case Mame.PlayState.PLAY_RUNNING:
                case Mame.PlayState.PLAY_CONTINUOUSSTART:
                case Mame.PlayState.PLAY_CONTINUOUSSTART2:
                case Mame.PlayState.PLAY_CONTINUOUS:
                case Mame.PlayState.PLAY_NEXT:
                case Mame.PlayState.PLAY_NEXT2:
                    btnPlay.Text = "stop";
                    btnRecord.Enabled = false;
                    break;
                case Mame.PlayState.RECORD_START:
                case Mame.PlayState.RECORD_RECORDING:
                    btnRecord.Text = "stop";
                    btnPlay.Enabled = false;
                    btnRun.Enabled = false;
                    lbTrack.Enabled = false;
                    break;
            }
        }
        private void OnRunning(bool b1)
        {
            if (b1)
            {
                Mame.mame_pause(false);
                btnRun.Text = "pause";
            }
            else
            {
                Mame.mame_pause(true);
                btnRun.Text = "run";
            }
        }
        public void ui_callback()
        {
            if (bWaiting == true)
            {
                return;
            }
            if (Mame.playState == Mame.PlayState.PLAY_STOPPING)
            {
                iTime++;
                if (Sound.nZero >= iInt5)
                {
                    tbMsg1.Text = "00:00";
                    Mame.playState = Mame.PlayState.PLAY_STOPPED;
                    OnPlaying(Mame.playState);
                }
                else
                {
                    if (iTime % 10 == 0)
                    {
                        tbMsg1.Text = SecondToTime(iTime / 10);
                    }
                }
            }
            else if (Mame.playState == Mame.PlayState.PLAY_START)
            {
                iTime++;
                if (Sound.nZero == 0)
                {
                    Mame.playState = Mame.PlayState.PLAY_RUNNING;
                }
            }
            else if (Mame.playState == Mame.PlayState.RECORD_START)
            {
                iTime++;
                if (Sound.nZero == 0)
                {
                    Mame.playState = Mame.PlayState.RECORD_RECORDING;
                }
            }
            else if (Mame.playState == Mame.PlayState.PLAY_CONTINUOUSSTART)
            {
                iTime++;
                if (Sound.nZero == 0)
                {
                    Mame.playState = Mame.PlayState.PLAY_CONTINUOUS;
                }
            }
            else if (Mame.playState == Mame.PlayState.PLAY_RUNNING || Mame.playState == Mame.PlayState.RECORD_RECORDING)
            {
                if ((Sound.nZero >= iInt5 && nTime == 10000) || iTime == nTime + iInt1)
                {
                    Sound.nZero = 0;
                    Sound.stoptrack(TrackInfo.StopTrack);
                    if (Mame.playState == Mame.PlayState.RECORD_RECORDING)
                    {
                        WavWrite.CloseSoundFile();
                    }
                    tbMsg1.Text = "00:00";
                    Mame.playState = Mame.PlayState.PLAY_STOPPED;
                    OnPlaying(Mame.playState);
                }
                else
                {
                    iTime++;
                    if (iTime % 10 == 0)
                    {
                        tbMsg1.Text = SecondToTime(iTime / 10);
                    }
                }
            }
            else if (Mame.playState == Mame.PlayState.PLAY_CONTINUOUSSTART2)
            {
                bWaiting = true;
                TrackInfo ti = TrackInfo.CurTrack;
                iTime = 0;
                nTime = (int)(ti.Second * 10);
                lbTrack.SelectedIndex = ti.IndexList;
                tbMsg1.Text = "00:00";
                tbMsg2.Text = ti.Message;
                if (Inptport.lsFA.Count == 0)
                {
                    Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART;
                }
                bWaiting = false;
            }
            else if (Mame.playState == Mame.PlayState.PLAY_NEXT)
            {
                TrackInfo.GetNext();
                Mame.playState = Mame.PlayState.PLAY_NEXT2;
            }
            else if (Mame.playState == Mame.PlayState.PLAY_NEXT2)
            {
                TrackInfo ti = null;
                ti = TrackInfo.CurTrack;
                if (ti != null)
                {
                    TrackInfo.CurTrack = ti;
                    iTime = 0;
                    nTime = (int)(ti.Second * 10);
                    lbTrack.SelectedIndex = ti.IndexList;
                    tbMsg1.Text = "00:00";
                    tbMsg2.Text = ti.Message;
                    Sound.playtrack(ti);
                    Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART;
                }
                else
                {
                    tbMsg1.Text = "00:00";
                    Mame.playState = Mame.PlayState.PLAY_STOPPED;
                    Sound.stoptrack(TrackInfo.StopTrack);
                }
            }
            else if (Mame.playState == Mame.PlayState.PLAY_CONTINUOUS)
            {
                if ((Sound.nZero >= iInt5 && nTime == 10000) || iTime == nTime + iInt1)
                {
                    Sound.stoptrack(TrackInfo.StopTrack);
                    Mame.playState = Mame.PlayState.PLAY_NEXT;
                }
                else
                {
                    iTime++;
                    if (iTime % 10 == 0)
                    {
                        tbMsg1.Text = SecondToTime(iTime / 10);
                    }
                }
            }
            if (mame.Timer.global_basetime.attoseconds == 0)
            {
                tbSecond.Text = mame.Timer.global_basetime.seconds.ToString();
            }
            mame.Timer.timer_adjust_periodic(ui_timer, ui_time, Attotime.ATTOTIME_NEVER);
        }
        public void LoadTrack()
        {
            tbMsg2.Clear();
            lbTrack.Items.Clear();
            TrackInfo.lTI = new List<TrackInfo>();
            string s1;
            StreamReader sr1 = new StreamReader("lists\\en\\" + Machine.sName + ".lst", Encoding.UTF8);
            while (true)
            {
                s1 = sr1.ReadLine();
                if (s1 == "$main")
                {
                    break;
                }
            }
            while (true)
            {
                s1 = sr1.ReadLine();
                if (s1 == "$end")
                {
                    break;
                }
                else if (Regex.IsMatch(s1, "^(.+)&lt;(.+)&gt;$"))
                {
                    string s3;
                    Match m1;
                    m1 = Regex.Match(s1, "^(.+)&lt;(.+)&gt;$");
                    s3 = m1.Groups[1].ToString() + "<" + m1.Groups[2].ToString() + ">";
                    s1 = s3;
                }
                if (Regex.IsMatch(s1, "^#"))
                {
                    ushort u1;
                    int i1;
                    string s2, s3 = "", s4 = "";
                    Match m11, m12;
                    TrackInfo ti1 = null;
                    m11 = Regex.Match(s1, "^#([0-9]{2,4}) (.*) <time=\"([^\"]+)\".*>$");
                    m12 = Regex.Match(s1, "^#([0-9]{2,4}) (.*)$");
                    if (m11.Groups.Count == 4)
                    {
                        u1 = ushort.Parse(m11.Groups[1].ToString());
                        s2 = m11.Groups[2].ToString();
                        s3 = m11.Groups[3].ToString();
                        s4 = m11.Groups[1].ToString() + " " + m11.Groups[2].ToString();
                        i1 = lbTrack.Items.Count;
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                    else if (m12.Groups.Count == 3)
                    {
                        u1 = ushort.Parse(m12.Groups[1].ToString());
                        s2 = m12.Groups[2].ToString();
                        s3 = "";
                        s4 = m12.Groups[1].ToString() + " " + m12.Groups[2].ToString();
                        i1 = lbTrack.Items.Count;
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                    TrackInfo.lTI.Add(ti1);
                    lbTrack.Items.Add(s4);
                }
                else if (Regex.IsMatch(s1, "^\\$"))
                {
                    ushort u1;
                    int i1;
                    string s2, s3 = "", s4 = "";
                    Match m21, m22;
                    TrackInfo ti1 = null;
                    m21 = Regex.Match(s1, "^\\$([0-9a-fA-F]{2,3}) (.*) <time=\"([^\"]+)\".*>$");
                    m22 = Regex.Match(s1, "^\\$([0-9a-fA-F]{2,3}) (.*)$");
                    if (m21.Groups.Count == 4)
                    {
                        u1 = ushort.Parse(m21.Groups[1].ToString(), NumberStyles.HexNumber);
                        s2 = m21.Groups[2].ToString();
                        s3 = m21.Groups[3].ToString();
                        s4 = m21.Groups[1].ToString() + " " + m21.Groups[2].ToString();
                        i1 = lbTrack.Items.Count;
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                    else if (m22.Groups.Count == 3)
                    {
                        u1 = ushort.Parse(m22.Groups[1].ToString(), NumberStyles.HexNumber);
                        s2 = m22.Groups[2].ToString();
                        s3 = "";
                        s4 = m22.Groups[1].ToString() + " " + m22.Groups[2].ToString();
                        i1 = lbTrack.Items.Count;
                        ti1 = new TrackInfo(u1, s2, s3, s4, i1);
                    }
                    TrackInfo.lTI.Add(ti1);
                    lbTrack.Items.Add(s4);
                }
                else if (Regex.IsMatch(s1, "^//"))
                {

                }
                else
                {
                    lbTrack.Items.Add(s1);
                }
            }
            TrackInfo.StopTrack = new TrackInfo(RomInfo.IStop, "stop", "", "", lbTrack.Items.Count);
            sr1.Close();
        }
        public void OnLoaded(bool b1)
        {
            if (b1)
            {
                btnPlay.Enabled = true;
                btnRun.Enabled = true;
                btnRecord.Enabled = true;
                lbTrack.Enabled = true;
                if (bLoading)
                {
                    bLoading = false;
                    OnRunning(true);
                    t1 = new Thread(run);
                    t1.Start();
                }
            }
            else
            {
                btnPlay.Enabled = false;
                btnRun.Enabled = false;
                btnRecord.Enabled = false;
                lbTrack.Enabled = false;
            }
        }
        private void lbTrack_DoubleClick(object sender, EventArgs e)
        {
            TrackInfo ti;
            ti = TrackInfo.GetTrackByIndexList(lbTrack.SelectedIndex);
            if (ti != null)
            {
                bWaiting = true;
                if (Mame.playState == Mame.PlayState.PLAY_STOPPED)
                {
                    Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART2;
                    OnPlaying(Mame.playState);
                    Sound.playtrack(ti);
                }
                else
                {
                    Mame.playState = Mame.PlayState.PLAY_CONTINUOUSSTART2;
                    Sound.stopandplaytrack(ti);
                }
                TrackInfo.CurTrack = ti;
                bWaiting = false;
            }
        }        
        private void exitToolStripMenuItem_Click(object sender, EventArgs e)
        {
            this.Close();
        }
        protected override void WndProc(ref Message msg)
        {
            if (msg.Msg == 0x0112)
            {
                if (msg.WParam.ToString("X4") == "F100")
                {
                    if (Keyboard.bF10)
                    {
                        Keyboard.bF10 = false;
                        return;
                    }
                }
            }
            // Pass message to default handler.
            base.WndProc(ref msg);
        }
        private void aboutToolStripMenuItem_Click(object sender, EventArgs e)
        {
            aboutForm about1 = new aboutForm();
            about1.ShowDialog();
        }
        private void itemsizeToolStripMenuItem_Click(object sender, EventArgs e)
        {
            int i, n;
            n = itemSize.Length;
            for (i = 0; i < n; i++)
            {
                itemSize[i].Checked = false;
            }
            for (i = 0; i < n; i++)
            {
                if (itemSize[i] == (ToolStripItem)sender)
                {
                    Video.iMode = i;
                    itemSelect();
                    break;
                }
            }
        }
        private void itemSelect()
        {
            itemSize[Video.iMode].Checked = true;
            switch (Machine.sBoard)
            {
                case "CPS-1":
                case "CPS-1(QSound)":
                case "CPS2":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 0;
                        Video.width = 512;
                        Video.height = 512;
                    }
                    else if (Video.iMode == 1)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 256;
                        Video.width = 512;
                        Video.height = 256;
                    }
                    else if (Video.iMode == 2)
                    {
                        Video.offsetx = 64;
                        Video.offsety = 272;
                        Video.width = 384;
                        Video.height = 224;
                    }
                    break;
                case "Data East":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 16;
                        Video.width = 256;
                        Video.height = 224;
                    }
                    break;
                case "Tehkan":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 16;
                        Video.width = 256;
                        Video.height = 224;
                    }
                    break;
                case "Neo Geo":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 30;
                        Video.offsety = 16;
                        Video.width = 320;
                        Video.height = 224;
                    }
                    break;
                case "SunA8":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 16;
                        Video.width = 256;
                        Video.height = 224;
                    }
                    break;
                case "Namco System 1":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 73;
                        Video.offsety = 16;
                        Video.width = 288;
                        Video.height = 224;
                    }
                    break;
                case "IGS011":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 0;
                        Video.width = 512;
                        Video.height = 240;
                    }
                    break;
                case "PGM":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 0;
                        Video.width = 448;
                        Video.height = 224;
                    }
                    break;
                case "M72":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 64;
                        Video.offsety = 0;
                        Video.width = 384;
                        Video.height = 256;
                    }
                    break;
                case "M92":
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 80;
                        Video.offsety = 8;
                        Video.width = 320;
                        Video.height = 240;
                    }
                    break;
                case "Taito":
                    if (Video.iMode == 0)
                    {
                        switch (Machine.sName)
                        {
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
                    if (Video.iMode == 0)
                    {
                        Video.offsetx = 0;
                        Video.offsety = 16;
                        Video.width = 320;
                        Video.height = 224;
                    }
                    break;
                case "Konami 68000":
                    if (Video.iMode == 0)
                    {
                        switch (Machine.sName)
                        {
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
                    if (Video.iMode == 0)
                    {
                        switch (Machine.sName)
                        {
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
        private void btnPlay_Click(object sender, EventArgs e)
        {
            if (btnPlay.Text == "play")
            {
                TrackInfo ti;
                if (lbTrack.SelectedItem != null)
                {
                    ti = TrackInfo.GetTrackByIndexList(lbTrack.SelectedIndex);
                    if (ti != null)
                    {
                        bWaiting = true;
                        Mame.playState = Mame.PlayState.PLAY_START;
                        tbMsg1.Text = "00:00";
                        tbMsg2.Text = ti.Message;
                        iTime = 0;
                        nTime = (int)(ti.Second * 10);
                        TrackInfo.CurTrack = ti;
                        Sound.nZero = 0;
                        Sound.playtrack(ti);
                        OnPlaying(Mame.playState);
                        bWaiting = false;
                    }
                }
            }
            else if (btnPlay.Text == "stop")
            {
                Mame.playState = Mame.PlayState.PLAY_STOPPING;
                Sound.stoptrack(TrackInfo.StopTrack);
                OnPlaying(Mame.playState);
                return;
            }
        }
        private void btnRecord_Click(object sender, EventArgs e)
        {
            if (btnRecord.Text == "record")
            {
                TrackInfo ti;
                if (lbTrack.SelectedItem != null)
                {
                    ti = TrackInfo.GetTrackByIndexList(lbTrack.SelectedIndex);
                    if (ti != null)
                    {
                        bWaiting = true;
                        WavWrite.CreateSoundFile("waves\\" + RomInfo.Rom.Name + "-" + ti.TrackID.ToString("000") + ".wav");
                        Mame.playState = Mame.PlayState.RECORD_START;
                        tbMsg1.Text = "00:00";
                        tbMsg2.Text = ti.Message;
                        iTime = 0;
                        nTime = (int)(ti.Second * 10);
                        TrackInfo.CurTrack = ti;
                        Sound.nZero = 0;
                        Sound.playtrack(ti);
                        OnPlaying(Mame.playState);
                        bWaiting = false;
                    }
                }
            }
            else if (btnRecord.Text == "stop")
            {
                Sound.stoptrack(TrackInfo.StopTrack);
                WavWrite.CloseSoundFile();
                Mame.playState = Mame.PlayState.PLAY_STOPPED;
                OnPlaying(Mame.playState);
            }
        }
        private void loadToolStripMenuItem_Click(object sender, EventArgs e)
        {
            Mame.exit_pending = true;
            if (TrackInfo.CurTrack != null)
            {
                Sound.stoptrack(TrackInfo.StopTrack);
            }
            if (Machine.bRom)
            {
                UI.cpurun();
                Mame.mame_pause(true);
            }
            Mame.playState = Mame.PlayState.PLAY_STOPPED;
            OnPlaying(Mame.playState);
            OnRunning(false);
            foreach (ListViewItem lvi in loadform.listView1.Items)
            {
                if (sSelect == lvi.SubItems[2].Text)
                {
                    loadform.listView1.FocusedItem = lvi;
                    lvi.Selected = true;
                    loadform.listView1.TopItem = lvi;
                    break;
                }
            }
            loadform.bLoad = false;
            loadform.ShowDialog();
        }
    }
}