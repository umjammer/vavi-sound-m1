using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Microsoft.DirectX.DirectInput;
using ui;

namespace mame
{
    public struct FrameArray
    {
        public long frame_number_obj;
        public int index;
        public byte id;
        public FrameArray(long l, int i, byte b)
        {
            frame_number_obj = l;
            index = i;
            id = b;
        }
    }
    public class analog_field_state
    {
        //public byte shift;
        public int adjdefvalue;
        public int adjmin;
        public int adjmax;

        public int sensitivity;
        public bool reverse;
        public int delta;
        //public int centerdelta;

        public int accum;
        public int previous;
        //public int previousanalog;

        public int minimum;
        public int maximum;
        //public int center;
        public int reverse_val;

        public long scalepos;
        public long scaleneg;
        public long keyscalepos;
        public long keyscaleneg;
        //public long positionalscale;

        public bool absolute;
        public bool wraps;
        //public byte autocenter;
        public byte single_scale;
        public bool interpolate;
        public byte lastdigital;
    }
    public class input_port_private
    {
        public Atime last_frame_time;
        public long last_delta_nsec;
    }
    public partial class Inptport
    {        
        public static bool bReplayRead;
        public static List<FrameArray> lsFA;
        public delegate void loop_delegate();
        public static loop_delegate loop_inputports_callback, record_port_callback, replay_port_callback;
        public static analog_field_state analog_p0, analog_p1,analog_p1x,analog_p1y;
        public static input_port_private portdata;
        public static void input_port_init()
        {
            lsFA = new List<FrameArray>();
            portdata = new input_port_private();
            switch (Machine.sBoard)
            {
                case "CPS-1":
                    loop_inputports_callback = loop_inputports_c;
                    //loop_inputports_callback = CPS.loop_inputports_cps1_6b;
                    record_port_callback = CPS.record_portC;
                    replay_port_callback = CPS.replay_portC;
                    analog_p0 = new analog_field_state();
                    analog_p1 = new analog_field_state();
                    analog_p0.adjdefvalue = 0;
                    analog_p1.adjdefvalue = 0;
                    analog_p0.sensitivity = 100;
                    analog_p1.sensitivity = 100;
                    analog_p0.reverse = false;
                    analog_p1.reverse = false;
                    analog_p0.delta = 20;
                    analog_p1.delta = 20;
                    analog_p0.minimum = 0;
                    analog_p1.minimum = 0;
                    analog_p0.maximum = 0x1ffe00;
                    analog_p1.maximum = 0x1ffe00;
                    analog_p0.reverse_val = 0x200000;
                    analog_p1.reverse_val = 0x200000;
                    analog_p0.scalepos = 0x8000;
                    analog_p1.scalepos = 0x8000;
                    analog_p0.scaleneg = 0x8000;
                    analog_p1.scaleneg = 0x8000;
                    analog_p0.absolute = false;
                    analog_p0.wraps = true;
                    analog_p0.interpolate = true;
                    analog_p1.absolute = false;
                    analog_p1.wraps = true;
                    analog_p1.interpolate = true;
                    break;
                case "CPS-1(QSound)":
                    loop_inputports_callback = loop_inputports_q;
                    //loop_inputports_callback = CPS.loop_inputports_cps1_6b;
                    record_port_callback = CPS.record_portC;
                    replay_port_callback = CPS.replay_portC;
                    analog_p0 = new analog_field_state();
                    analog_p1 = new analog_field_state();
                    analog_p0.adjdefvalue = 0;
                    analog_p1.adjdefvalue = 0;
                    analog_p0.sensitivity = 100;
                    analog_p1.sensitivity = 100;
                    analog_p0.reverse = false;
                    analog_p1.reverse = false;
                    analog_p0.delta = 20;
                    analog_p1.delta = 20;
                    analog_p0.minimum = 0;
                    analog_p1.minimum = 0;
                    analog_p0.maximum = 0x1ffe00;
                    analog_p1.maximum = 0x1ffe00;
                    analog_p0.reverse_val = 0x200000;
                    analog_p1.reverse_val = 0x200000;
                    analog_p0.scalepos = 0x8000;
                    analog_p1.scalepos = 0x8000;
                    analog_p0.scaleneg = 0x8000;
                    analog_p1.scaleneg = 0x8000;
                    analog_p0.absolute = false;
                    analog_p0.wraps = true;
                    analog_p0.interpolate = true;
                    analog_p1.absolute = false;
                    analog_p1.wraps = true;
                    analog_p1.interpolate = true;
                    break;
                case "CPS2":
                    loop_inputports_callback = loop_inputports_c2;
                    //loop_inputports_callback = CPS.loop_inputports_cps2_2p6b;
                    record_port_callback = CPS.record_portC2;
                    replay_port_callback = CPS.replay_portC2;
                    analog_p0 = new analog_field_state();
                    analog_p1 = new analog_field_state();
                    analog_p0.adjdefvalue = 0;
                    analog_p1.adjdefvalue = 0;
                    analog_p0.sensitivity = 100;
                    analog_p1.sensitivity = 100;
                    analog_p0.reverse = false;
                    analog_p1.reverse = false;
                    analog_p0.delta = 20;
                    analog_p1.delta = 20;
                    analog_p0.minimum = 0;
                    analog_p1.minimum = 0;
                    analog_p0.maximum = 0x1ffe00;
                    analog_p1.maximum = 0x1ffe00;
                    analog_p0.reverse_val = 0x200000;
                    analog_p1.reverse_val = 0x200000;
                    analog_p0.scalepos = 0x8000;
                    analog_p1.scalepos = 0x8000;
                    analog_p0.scaleneg = 0x8000;
                    analog_p1.scaleneg = 0x8000;
                    break;
                case "Neo Geo":
                    loop_inputports_callback = loop_inputports_neogeo;
                    //loop_inputports_callback = Neogeo.loop_inputports_neogeo_standard;
                    record_port_callback = Neogeo.record_port;
                    replay_port_callback = Neogeo.replay_port;
                    analog_p0 = new analog_field_state();
                    analog_p1 = new analog_field_state();
                    analog_p0.adjdefvalue = 0;
                    analog_p1.adjdefvalue = 0;
                    analog_p0.sensitivity = 10;
                    analog_p1.sensitivity = 10;
                    analog_p0.reverse = true;
                    analog_p1.reverse = true;
                    analog_p0.delta = 20;
                    analog_p1.delta = 20;
                    analog_p0.minimum = 0;
                    analog_p1.minimum = 0;
                    analog_p0.maximum = 0x1fe00;
                    analog_p1.maximum = 0x1fe00;
                    analog_p0.reverse_val = 0x20000;
                    analog_p1.reverse_val = 0x20000;
                    analog_p0.scalepos = 0x8000;
                    analog_p1.scalepos = 0x8000;
                    analog_p0.scaleneg = 0x8000;
                    analog_p1.scaleneg = 0x8000;
                    analog_p0.absolute = false;
                    analog_p0.wraps = true;
                    analog_p0.interpolate = true;
                    analog_p1.absolute = false;
                    analog_p1.wraps = true;
                    analog_p1.interpolate = true;
                    break;
                case "Namco System 1":
                    loop_inputports_callback = loop_inputports_namcos1;
                    //loop_inputports_callback = Namcos1.loop_inputports_ns1_3b;
                    record_port_callback = Namcos1.record_port;
                    replay_port_callback = Namcos1.replay_port;
                    analog_p0 = new analog_field_state();
                    analog_p1 = new analog_field_state();
                    analog_p0.adjdefvalue = 0;
                    analog_p1.adjdefvalue = 0;
                    analog_p0.sensitivity = 30;
                    analog_p1.sensitivity = 30;
                    analog_p0.reverse = false;
                    analog_p1.reverse = false;
                    analog_p0.delta = 15;
                    analog_p1.delta = 15;
                    analog_p0.minimum = 0;
                    analog_p1.minimum = 0;
                    analog_p0.maximum = 0x1fe00;
                    analog_p1.maximum = 0x1fe00;
                    analog_p0.reverse_val = 0x20000;
                    analog_p1.reverse_val = 0x20000;
                    analog_p0.scalepos = 0x8000;
                    analog_p1.scalepos = 0x8000;
                    analog_p0.scaleneg = 0x8000;
                    analog_p1.scaleneg = 0x8000;
                    analog_p0.absolute = false;
                    analog_p0.wraps = true;
                    analog_p0.interpolate = true;
                    analog_p1.absolute = false;
                    analog_p1.wraps = true;
                    analog_p1.interpolate = true;
                    break;
                case "IGS011":
                    loop_inputports_callback = IGS011.loop_inputports_igs011_drgnwrld;
                    //record_port_callback = IGS011.record_port;
                    //replay_port_callback = IGS011.replay_port;
                    break;
                case "PGM":
                    loop_inputports_callback = PGM.loop_inputports_pgm_standard;
                    record_port_callback = PGM.record_port;
                    replay_port_callback = PGM.replay_port;
                    break;
                case "M72":
                    loop_inputports_callback = loop_inputports_m72;
                    //loop_inputports_callback = M72.loop_inputports_m72_common;
                    record_port_callback = M72.record_port;
                    replay_port_callback = M72.replay_port;
                    break;
                case "M92":
                    loop_inputports_callback = loop_inputports_m92;
                    //loop_inputports_callback = M92.loop_inputports_m92_common;
                    record_port_callback = M92.record_port;
                    replay_port_callback = M92.replay_port;
                    break;
                case "Taito":
                    //loop_inputports_callback = loop_inputports_taito;
                    record_port_callback = Taito.record_port_bublbobl;
                    replay_port_callback = Taito.replay_port_bublbobl;
                    analog_p1x = new analog_field_state();
                    analog_p1x.adjdefvalue = 0x80;
                    analog_p1x.adjmin = 0;
                    analog_p1x.adjmax = 0xff;
                    analog_p1x.sensitivity = 25;
                    analog_p1x.reverse = false;
                    analog_p1x.delta = 15;
                    analog_p1x.minimum = -0x10000;
                    analog_p1x.maximum = 0x10000;
                    analog_p1x.absolute = true;
                    analog_p1x.wraps = false;
                    analog_p1x.interpolate = false;
                    analog_p1x.single_scale = 0;
                    analog_p1x.scalepos = 0x7f00;
                    analog_p1x.scaleneg = 0x8000;
                    analog_p1x.reverse_val = 0x200000;
                    analog_p1x.keyscalepos = 0x0000000204081020;
                    analog_p1x.keyscaleneg = 0x0000000200000000;
                    analog_p1y = new analog_field_state();
                    analog_p1y.adjdefvalue = 0x80;
                    analog_p1y.adjmin = 0;
                    analog_p1y.adjmax = 0xff;
                    analog_p1y.sensitivity = 25;
                    analog_p1y.reverse = false;
                    analog_p1y.delta = 15;
                    analog_p1y.minimum = -0x10000;
                    analog_p1y.maximum = 0x10000;
                    analog_p1y.absolute = true;
                    analog_p1y.wraps = false;
                    analog_p1y.interpolate = false;
                    analog_p1y.single_scale = 0;
                    analog_p1y.scalepos = 0x7f00;
                    analog_p1y.scaleneg = 0x8000;
                    analog_p1y.reverse_val = 0x200000;
                    analog_p1y.keyscalepos = 0x0000000204081020;
                    analog_p1y.keyscaleneg = 0x0000000200000000;
                    break;
                case "Taito B":
                    //loop_inputports_callback = Taitob.loop_inputports_taitob_pbobble;
                    record_port_callback = Taitob.record_port;
                    replay_port_callback = Taitob.replay_port;
                    break;
                case "Konami 68000":
                    //loop_inputports_callback = Konami68000.loop_inputports_konami68000_ssriders;
                    record_port_callback = Konami68000.record_port;
                    replay_port_callback = Konami68000.replay_port;
                    break;
                case "Capcom":
                    //loop_inputports_callback = Capcom.loop_inputports_sfus;
                    record_port_callback = Capcom.record_port_sf;
                    replay_port_callback = Capcom.replay_port_sf;
                    break;
            }
            switch (Machine.sName)
            {
                case "pcktgal":
                    loop_inputports_callback = loop_inputports_dataeast_pcktgal;
                    break;
                case "pbaction":
                    loop_inputports_callback = loop_inputports_tehkan_pbaction;
                    break;
                case "tokio":
                case "tokioo":
                case "tokiou":
                case "tokiob":
                    loop_inputports_callback = loop_inputports_taito;
                    break;
                case "bublbobl":
                case "bublbobl1":
                case "bublboblr":
                case "bublboblr1":
                case "bub68705":
                case "bublcave":
                case "bublcave11":
                case "bublcave10":
                    loop_inputports_callback = loop_inputports_taito;
                    break;
                case "boblbobl":
                case "sboblbobl":
                case "sboblbobla":
                case "sboblboblb":
                case "sboblbobld":
                case "sboblboblc":
                case "dland":
                case "bbredux":
                case "bublboblb":
                case "boblcave":                
                    loop_inputports_callback = Taito.loop_inputports_taito_boblbobl;
                    break;
                case "pbobble":
                    loop_inputports_callback = loop_inputports_taitob;
                    break;
                case "silentd":
                case "silentdj":
                case "silentdu":
                    loop_inputports_callback = loop_inputports_taitob;
                    break;                
                case "mia":
                case "tmnt":
                    loop_inputports_callback = loop_inputports_konami68000_tmnt;
                    break;
                case "punkshot":
                case "lgtnfght":
                case "blswhstl":
                case "tmnt2":
                case "qgakumon":
                case "ssriders":
                case "thndrx2":
                    loop_inputports_callback = loop_inputports_konami68000_punkshot;
                    break;
                case "gng":
                    loop_inputports_callback = loop_inputports_capcom_gng;
                    break;
                case "sf":
                    loop_inputports_callback = loop_inputports_capcom_sf;
                    break;
                case "sfua":
                case "sfj":
                    loop_inputports_callback = Capcom.loop_inputports_sfjp;
                    break;
                case "sfjan":
                case "sfan":
                case "sfp":
                    loop_inputports_callback = Capcom.loop_inputports_sfan;
                    break;
            }
        }
        public static int apply_analog_min_max(analog_field_state analog, int value)
        {
            int adjmin = (analog.minimum * 100) / analog.sensitivity;
            int adjmax = (analog.maximum * 100) / analog.sensitivity;
            if (!analog.wraps)
            {
                if (value > adjmax)
                    value = adjmax;
                else if (value < adjmin)
                    value = adjmin;
            }
            else
            {
                int adj1 = (512 * 100) / analog.sensitivity;
                int adjdif = adjmax - adjmin + adj1;
                if (analog.reverse)
                {
                    while (value <= adjmin - adj1)
                        value += adjdif;
                    while (value > adjmax)
                        value -= adjdif;
                }
                else
                {
                    while (value >= adjmax + adj1)
                        value -= adjdif;
                    while (value < adjmin)
                        value += adjdif;
                }
            }
            return value;
        }
        public static uint input_port_read_direct(analog_field_state analog)
        {
            uint result;
            int value;
            long nsec_since_last;
            value = analog.accum;
            if (analog.interpolate && portdata.last_delta_nsec != 0)
            {
                nsec_since_last = Attotime.attotime_to_attoseconds(Attotime.attotime_sub(Timer.get_current_time(), portdata.last_frame_time)) / Attotime.ATTOSECONDS_PER_NANOSECOND;
                value = (int)(analog.previous + ((long)(analog.accum - analog.previous) * nsec_since_last / portdata.last_delta_nsec));
            }
            result = (uint)apply_analog_settings(value, analog);
            return result;
        }
        public static int apply_analog_settings(int value,analog_field_state analog)
        {
            value = apply_analog_min_max(analog, value);
            value = (int)((long)value * analog.sensitivity / 100);            
            if (analog.reverse)
            {
                value = analog.reverse_val - value;
            }
            if (value >= 0)
            {
                value = (int)((long)(value * analog.scalepos)>>24);
            }
            else
            {
                value = (int)((long)(value * analog.scaleneg)>>24);
            }
            value += analog.adjdefvalue;
            return value;
        }
        public static void frame_update_callback()
        {
            if (Mame.mame_is_paused())
            {
                return;
            }
            frame_update();
            Video.screenstate.frame_number++;
        }
        public static void loop_inputports_c()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Sound.latched_value[lsFA[i].index] = lsFA[i].id;
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_q()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    CPS.qsound_sharedram1_w(lsFA[i].index, lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_c2()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    CPS.qsound_sharedram1_w(lsFA[i].index, lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_neogeo()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    if (lsFA[i].index == 0)
                    {
                        Neogeo.audio_command_w(lsFA[i].id);
                    }
                    else if (lsFA[i].index == 1)
                    {
                        Neogeo.audio_cpu_nmi_enabled = true;
                        Neogeo.audio_command_w(lsFA[i].id);
                        //Neogeo.audio_result_w(lsFA[i].id);
                    }
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_namcos1()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Namcos1.soundram_w(lsFA[i].index, lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_m72()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    M72.m72_sound_command_w(lsFA[i].index, lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_m92()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    M92.m92_soundlatch_w(lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_taito()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Taito.bublbobl_sound_command_w(lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_taitob()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    if (lsFA[i].index == 0)
                    {
                        Taitosnd.taitosound_port_w(0, 0);
                        Taitosnd.taitosound_comm_w(0, lsFA[i].id);
                        Taitosnd.taitosound_comm_w(0, (byte)(lsFA[i].id >> 4));
                    }
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_konami68000_tmnt()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Sound.latched_value[0] = lsFA[i].id;
                    Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_konami68000_punkshot()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    K053260.k053260_write(0, 0, lsFA[i].id);
                    Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_capcom_gng()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Sound.soundlatch_w(lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_capcom_sf()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Capcom.soundcmd_w(lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_dataeast_pcktgal()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Dataeast.pcktgal_sound_w(lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        public static void loop_inputports_tehkan_pbaction()
        {
            int i;
            for (i = lsFA.Count - 1; i >= 0; i--)
            {
                if (Video.screenstate.frame_number == lsFA[i].frame_number_obj)
                {
                    Tehkan.pbaction_sh_command_w(lsFA[i].id);
                    lsFA.Remove(lsFA[i]);
                }
            }
        }
        private static void frame_update()
        {
            Atime curtime = Timer.get_current_time();
            portdata.last_delta_nsec = Attotime.attotime_to_attoseconds(Attotime.attotime_sub(curtime, portdata.last_frame_time)) / Attotime.ATTOSECONDS_PER_NANOSECOND;
            portdata.last_frame_time = curtime;
            if (Mame.playState != Mame.PlayState.PLAY_REPLAYRUNNING)
            {
                //if (Mame.is_foreground)
                {
                    loop_inputports_callback();
                }
            }
            if (Mame.playState == Mame.PlayState.PLAY_RECORDRUNNING)
            {
                record_port_callback();
            }
            else if (Mame.playState == Mame.PlayState.PLAY_REPLAYRUNNING)
            {
                replay_port_callback();
            }
        }
        public static void frame_update_analog_field_forgottn_p0(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            if (analog.accum != value2)
            {
                int i1 = 1;
            }
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.K))
            {
                keypressed = true;
                delta -= analog_p0.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.L))
            {
                keypressed = true;
                delta += analog_p0.delta * 0x200;
                analog.lastdigital = 2;
            }
            if (Mouse.deltaY < 0)
            {
                keypressed = true;
                delta += Mouse.deltaY * 0x200;
                analog.lastdigital = 1;
            }
            if (Mouse.deltaY > 0)
            {
                keypressed = true;
                delta += Mouse.deltaY * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_forgottn_p1(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.NumPad2))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.NumPad3))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_ecofghtr_p0(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.U))
            {
                keypressed = true;
                delta -= analog_p0.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.I))
            {
                keypressed = true;
                delta += analog_p0.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_ecofghtr_p1(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.NumPad4))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.NumPad5))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_irrmaze_p0(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.A))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.D))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_irrmaze_p1(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.S))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.W))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_quester_p0(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.A))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.D))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_quester_p1(analog_field_state analog)
        {
            bool keypressed = false;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            if (Keyboard.IsPressed(Key.Left))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.Right))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
                analog.lastdigital = 0;
        }
        public static void frame_update_analog_field_opwolf_p1x(analog_field_state analog)
        {
            bool keypressed = false;
            long keyscale;
            int rawvalue;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            rawvalue = Mouse.deltaX;
            if (rawvalue != 0)
            {
                delta = rawvalue;
                analog.lastdigital = 0;
            }
            if (Keyboard.IsPressed(Key.A))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.D))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            if (Mouse.deltaX < 0)
            {
                keypressed = true;
                delta += Mouse.deltaX * 0x200;
                analog.lastdigital = 1;
            }
            if (Mouse.deltaX > 0)
            {
                keypressed = true;
                delta += Mouse.deltaX * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
            {
                analog.lastdigital = 0;
            }
        }
        public static void frame_update_analog_field_opwolf_p1y(analog_field_state analog)
        {
            bool keypressed = false;
            long keyscale;
            int rawvalue;
            int delta = 0;
            int value2;
            value2 = apply_analog_min_max(analog, analog.accum);
            analog.previous = analog.accum = value2;
            rawvalue = Mouse.deltaY;
            if (rawvalue != 0)
            {
                delta = rawvalue;
                analog.lastdigital = 0;
            }
            if (Keyboard.IsPressed(Key.W))
            {
                keypressed = true;
                delta -= analog.delta * 0x200;
                analog.lastdigital = 1;
            }
            if (Keyboard.IsPressed(Key.S))
            {
                keypressed = true;
                delta += analog.delta * 0x200;
                analog.lastdigital = 2;
            }
            if (Mouse.deltaY < 0)
            {
                keypressed = true;
                delta += Mouse.deltaY * 0x200;
                analog.lastdigital = 1;
            }
            if (Mouse.deltaY > 0)
            {
                keypressed = true;
                delta += Mouse.deltaY * 0x200;
                analog.lastdigital = 2;
            }
            analog.accum += delta;
            if (!keypressed)
            {
                analog.lastdigital = 0;
            }
        }
    }
}