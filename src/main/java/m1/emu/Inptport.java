/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import m1.emu.Attotime.Atime;
import m1.emu.Cpuint.LineState;
import m1.mame.capcom.Capcom;
import m1.mame.cps.CPS;
import m1.mame.dataeast.Dataeast;
import m1.mame.igs011.IGS011;
import m1.mame.konami68000.Konami68000;
import m1.mame.m72.M72;
import m1.mame.m92.M92;
import m1.mame.namcos1.Namcos1;
import m1.mame.neogeo.Neogeo;
import m1.mame.pgm.PGM;
import m1.mame.taito.Taito;
import m1.mame.taitob.Taitob;
import m1.mame.tehkan.Tehkan;
import m1.sound.K053260;
import m1.sound.Sound;
import m1.sound.Taitosnd;

import static m1.emu.Attotime.ATTOTIME_ZERO;


public class Inptport {

    public static class FrameArray {

        public final long frame_number_obj;
        public final int index;
        public final byte id;

        public FrameArray(long l, int i, byte b) {
            frame_number_obj = l;
            index = i;
            id = b;
        }
    }

    public static class analog_field_state {

        //public byte shift;
        public int adjdefvalue;
        public int adjmin;
        public int adjmax;

        public int sensitivity;
        public boolean reverse;
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

        public boolean absolute;
        public boolean wraps;
        //public byte autocenter;
        public byte single_scale;
        public boolean interpolate;
        public byte lastdigital;
    }

    public static class input_port_private {

        public Atime last_frame_time = ATTOTIME_ZERO;
        public long last_delta_nsec;
    }

    public static boolean bReplayRead;
    public static List<FrameArray> lsFA;

    public interface loop_delegate extends Runnable {}

    public static loop_delegate loop_inputports_callback, record_port_callback, replay_port_callback;
    public static analog_field_state analog_p0, analog_p1, analog_p1x, analog_p1y;
    public static input_port_private portdata;

    public static void input_port_init() {
        lsFA = new ArrayList<>();
        portdata = new input_port_private();
        switch (Machine.sBoard) {
            case "CPS-1":
                loop_inputports_callback = Inptport::loop_inputports_c;
                //loop_inputports_callback = CPS.loop_inputports_cps1_6b;
                record_port_callback = CPS::record_portC;
                replay_port_callback = CPS::replay_portC;
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
                analog_p0.maximum = 0x1f_fe00;
                analog_p1.maximum = 0x1f_fe00;
                analog_p0.reverse_val = 0x20_0000;
                analog_p1.reverse_val = 0x20_0000;
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
                loop_inputports_callback = Inptport::loop_inputports_q;
                //loop_inputports_callback = CPS.loop_inputports_cps1_6b;
                record_port_callback = CPS::record_portC;
                replay_port_callback = CPS::replay_portC;
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
                analog_p0.maximum = 0x1f_fe00;
                analog_p1.maximum = 0x1f_fe00;
                analog_p0.reverse_val = 0x20_0000;
                analog_p1.reverse_val = 0x20_0000;
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
                loop_inputports_callback = Inptport::loop_inputports_c2;
                //loop_inputports_callback = CPS.loop_inputports_cps2_2p6b;
                record_port_callback = CPS::record_portC2;
                replay_port_callback = CPS::replay_portC2;
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
                analog_p0.maximum = 0x1f_fe00;
                analog_p1.maximum = 0x1f_fe00;
                analog_p0.reverse_val = 0x20_0000;
                analog_p1.reverse_val = 0x20_0000;
                analog_p0.scalepos = 0x8000;
                analog_p1.scalepos = 0x8000;
                analog_p0.scaleneg = 0x8000;
                analog_p1.scaleneg = 0x8000;
                break;
            case "Neo Geo":
                loop_inputports_callback = Inptport::loop_inputports_neogeo;
                //loop_inputports_callback = Neogeo.loop_inputports_neogeo_standard;
                record_port_callback = Neogeo::record_port;
                replay_port_callback = Neogeo::replay_port;
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
                analog_p0.maximum = 0x1_fe00;
                analog_p1.maximum = 0x1_fe00;
                analog_p0.reverse_val = 0x2_0000;
                analog_p1.reverse_val = 0x2_0000;
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
                loop_inputports_callback = Inptport::loop_inputports_namcos1;
                //loop_inputports_callback = Namcos1.loop_inputports_ns1_3b;
                record_port_callback = Namcos1::record_port;
                replay_port_callback = Namcos1::replay_port;
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
                analog_p0.maximum = 0x1_fe00;
                analog_p1.maximum = 0x1_fe00;
                analog_p0.reverse_val = 0x2_0000;
                analog_p1.reverse_val = 0x2_0000;
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
                loop_inputports_callback = IGS011::loop_inputports_igs011_drgnwrld;
                //record_port_callback = IGS011.record_port;
                //replay_port_callback = IGS011.replay_port;
                break;
            case "PGM":
                loop_inputports_callback = PGM::loop_inputports_pgm_standard;
                record_port_callback = PGM::record_port;
                replay_port_callback = PGM::replay_port;
                break;
            case "M72":
                loop_inputports_callback = Inptport::loop_inputports_m72;
                //loop_inputports_callback = M72.loop_inputports_m72_common;
                record_port_callback = M72::record_port;
                replay_port_callback = M72::replay_port;
                break;
            case "M92":
                loop_inputports_callback = Inptport::loop_inputports_m92;
                //loop_inputports_callback = M92.loop_inputports_m92_common;
                record_port_callback = M92::record_port;
                replay_port_callback = M92::replay_port;
                break;
            case "Taito":
                //loop_inputports_callback = loop_inputports_taito;
                record_port_callback = Taito::record_port_bublbobl;
                replay_port_callback = Taito::replay_port_bublbobl;
                analog_p1x = new analog_field_state();
                analog_p1x.adjdefvalue = 0x80;
                analog_p1x.adjmin = 0;
                analog_p1x.adjmax = 0xff;
                analog_p1x.sensitivity = 25;
                analog_p1x.reverse = false;
                analog_p1x.delta = 15;
                analog_p1x.minimum = -0x1_0000;
                analog_p1x.maximum = 0x1_0000;
                analog_p1x.absolute = true;
                analog_p1x.wraps = false;
                analog_p1x.interpolate = false;
                analog_p1x.single_scale = 0;
                analog_p1x.scalepos = 0x7f00;
                analog_p1x.scaleneg = 0x8000;
                analog_p1x.reverse_val = 0x20_0000;
                analog_p1x.keyscalepos = 0x0000_0002_0408_1020L;
                analog_p1x.keyscaleneg = 0x0000_0002_0000_0000L;
                analog_p1y = new analog_field_state();
                analog_p1y.adjdefvalue = 0x80;
                analog_p1y.adjmin = 0;
                analog_p1y.adjmax = 0xff;
                analog_p1y.sensitivity = 25;
                analog_p1y.reverse = false;
                analog_p1y.delta = 15;
                analog_p1y.minimum = -0x1_0000;
                analog_p1y.maximum = 0x1_0000;
                analog_p1y.absolute = true;
                analog_p1y.wraps = false;
                analog_p1y.interpolate = false;
                analog_p1y.single_scale = 0;
                analog_p1y.scalepos = 0x7f00;
                analog_p1y.scaleneg = 0x8000;
                analog_p1y.reverse_val = 0x20_0000;
                analog_p1y.keyscalepos = 0x0000_0002_0408_1020L;
                analog_p1y.keyscaleneg = 0x0000_0002_0000_0000L;
                break;
            case "Taito B":
                //loop_inputports_callback = Taitob.loop_inputports_taitob_pbobble;
                record_port_callback = Taitob::record_port;
                replay_port_callback = Taitob::replay_port;
                break;
            case "Konami 68000":
                //loop_inputports_callback = Konami68000.loop_inputports_konami68000_ssriders;
                record_port_callback = Konami68000::record_port;
                replay_port_callback = Konami68000::replay_port;
                break;
            case "Capcom":
                //loop_inputports_callback = Capcom.loop_inputports_sfus;
                record_port_callback = Capcom::record_port_sf;
                replay_port_callback = Capcom::replay_port_sf;
                break;
        }
        switch (Machine.sName) {
            case "pcktgal":
                loop_inputports_callback = Inptport::loop_inputports_dataeast_pcktgal;
                break;
            case "pbaction":
                loop_inputports_callback = Inptport::loop_inputports_tehkan_pbaction;
                break;
            case "tokio":
            case "tokioo":
            case "tokiou":
            case "tokiob":
                loop_inputports_callback = Inptport::loop_inputports_taito;
                break;
            case "bublbobl":
            case "bublbobl1":
            case "bublboblr":
            case "bublboblr1":
            case "bub68705":
            case "bublcave":
            case "bublcave11":
            case "bublcave10":
                loop_inputports_callback = Inptport::loop_inputports_taito;
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
                loop_inputports_callback = Taito::loop_inputports_taito_boblbobl;
                break;
            case "pbobble":
                loop_inputports_callback = Inptport::loop_inputports_taitob;
                break;
            case "silentd":
            case "silentdj":
            case "silentdu":
                loop_inputports_callback = Inptport::loop_inputports_taitob;
                break;
            case "mia":
            case "tmnt":
                loop_inputports_callback = Inptport::loop_inputports_konami68000_tmnt;
                break;
            case "punkshot":
            case "lgtnfght":
            case "blswhstl":
            case "tmnt2":
            case "qgakumon":
            case "ssriders":
            case "thndrx2":
                loop_inputports_callback = Inptport::loop_inputports_konami68000_punkshot;
                break;
            case "gng":
                loop_inputports_callback = Inptport::loop_inputports_capcom_gng;
                break;
            case "sf":
                loop_inputports_callback = Inptport::loop_inputports_capcom_sf;
                break;
            case "sfua":
            case "sfj":
                loop_inputports_callback = Capcom::loop_inputports_sfjp;
                break;
            case "sfjan":
            case "sfan":
            case "sfp":
                loop_inputports_callback = Capcom::loop_inputports_sfan;
                break;
        }
    }

    public static int apply_analog_min_max(analog_field_state analog, int value) {
        int adjmin = (analog.minimum * 100) / analog.sensitivity;
        int adjmax = (analog.maximum * 100) / analog.sensitivity;
        if (!analog.wraps) {
            if (value > adjmax)
                value = adjmax;
            else if (value < adjmin)
                value = adjmin;
        } else {
            int adj1 = (512 * 100) / analog.sensitivity;
            int adjdif = adjmax - adjmin + adj1;
            if (analog.reverse) {
                while (value <= adjmin - adj1)
                    value += adjdif;
                while (value > adjmax)
                    value -= adjdif;
            } else {
                while (value >= adjmax + adj1)
                    value -= adjdif;
                while (value < adjmin)
                    value += adjdif;
            }
        }
        return value;
    }

    public static int input_port_read_direct(analog_field_state analog) {
        int result;
        int value;
        long nsec_since_last;
        value = analog.accum;
        if (analog.interpolate && portdata.last_delta_nsec != 0) {
            nsec_since_last = Attotime.attotime_to_attoseconds(Attotime.attotime_sub(Timer.getCurrentTime(), portdata.last_frame_time)) / Attotime.ATTOSECONDS_PER_NANOSECOND;
            value = (int) (analog.previous + ((long) (analog.accum - analog.previous) * nsec_since_last / portdata.last_delta_nsec));
        }
        result = apply_analog_settings(value, analog);
        return result;
    }

    public static int apply_analog_settings(int value, analog_field_state analog) {
        value = apply_analog_min_max(analog, value);
        value = (int) ((long) value * analog.sensitivity / 100);
        if (analog.reverse) {
            value = analog.reverse_val - value;
        }
        if (value >= 0) {
            value = (int) ((value * analog.scalepos) >> 24);
        } else {
            value = (int) ((value * analog.scaleneg) >> 24);
        }
        value += analog.adjdefvalue;
        return value;
    }

    public static void frame_update_callback() {
        if (Mame.mame_is_paused()) {
            return;
        }
        frame_update();
        Video.screenstate.frame_number++;
    }

    public static void loop_inputports_c() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Sound.latched_value[lsFA.get(i).index] = lsFA.get(i).id;
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_q() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                CPS.qsound_sharedram1_w(lsFA.get(i).index, lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_c2() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                CPS.qsound_sharedram1_w(lsFA.get(i).index, lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_neogeo() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                if (lsFA.get(i).index == 0) {
                    Neogeo.audio_command_w(lsFA.get(i).id);
                } else if (lsFA.get(i).index == 1) {
                    Neogeo.audio_cpu_nmi_enabled = true;
                    Neogeo.audio_command_w(lsFA.get(i).id);
                    //Neogeo.audio_result_w(lsFA.get(i).id);
                }
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_namcos1() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Namcos1.soundram_w(lsFA.get(i).index, lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_m72() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                M72.m72_sound_command_w(lsFA.get(i).index, lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_m92() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                M92.m92_soundlatch_w(lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_taito() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Taito.bublbobl_sound_command_w(lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_taitob() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                if (lsFA.get(i).index == 0) {
                    Taitosnd.taitosound_port_w(0, (byte) 0);
                    Taitosnd.taitosound_comm_w(0, lsFA.get(i).id);
                    Taitosnd.taitosound_comm_w(0, (byte) (lsFA.get(i).id >> 4));
                }
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_konami68000_tmnt() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Sound.latched_value[0] = lsFA.get(i).id;
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_konami68000_punkshot() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                K053260.k053260_write(0, 0, lsFA.get(i).id);
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_capcom_gng() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Sound.soundlatch_w(lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_capcom_sf() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Capcom.soundcmd_w(lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_dataeast_pcktgal() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Dataeast.pcktgal_sound_w(lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    public static void loop_inputports_tehkan_pbaction() {
        for (int i = lsFA.size() - 1; i >= 0; i--) {
            if (Video.screenstate.frame_number == lsFA.get(i).frame_number_obj) {
                Tehkan.pbaction_sh_command_w(lsFA.get(i).id);
                lsFA.remove(lsFA.get(i));
            }
        }
    }

    private static void frame_update() {
        Atime curtime = Timer.getCurrentTime();
        portdata.last_delta_nsec = Attotime.attotime_to_attoseconds(Attotime.attotime_sub(curtime, portdata.last_frame_time)) / Attotime.ATTOSECONDS_PER_NANOSECOND;
        portdata.last_frame_time = curtime;
        if (Mame.playState != Mame.PlayState.PLAY_REPLAYRUNNING) {
            /* if (Mame.is_foreground) */ {
                loop_inputports_callback.run();
            }
        }
        if (Mame.playState == Mame.PlayState.PLAY_RECORDRUNNING) {
            record_port_callback.run();
        } else if (Mame.playState == Mame.PlayState.PLAY_REPLAYRUNNING) {
            replay_port_callback.run();
        }
    }

    public static void frame_update_analog_field_forgottn_p0(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        if (analog.accum != value2) {
            int i1 = 1;
        }
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            keypressed = true;
            delta -= analog_p0.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            keypressed = true;
            delta += analog_p0.delta * 0x200;
            analog.lastdigital = 2;
        }
        if (Mouse.deltaY < 0) {
            keypressed = true;
            delta += Mouse.deltaY * 0x200;
            analog.lastdigital = 1;
        }
        if (Mouse.deltaY > 0) {
            keypressed = true;
            delta += Mouse.deltaY * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_forgottn_p1(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_ecofghtr_p0(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            keypressed = true;
            delta -= analog_p0.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            keypressed = true;
            delta += analog_p0.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_ecofghtr_p1(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_irrmaze_p0(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_irrmaze_p1(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_quester_p0(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_quester_p1(analog_field_state analog) {
        boolean keypressed = false;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed)
            analog.lastdigital = 0;
    }

    public static void frame_update_analog_field_opwolf_p1x(analog_field_state analog) {
        boolean keypressed = false;
        long keyscale;
        int rawvalue;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        rawvalue = Mouse.deltaX;
        if (rawvalue != 0) {
            delta = rawvalue;
            analog.lastdigital = 0;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        if (Mouse.deltaX < 0) {
            keypressed = true;
            delta += Mouse.deltaX * 0x200;
            analog.lastdigital = 1;
        }
        if (Mouse.deltaX > 0) {
            keypressed = true;
            delta += Mouse.deltaX * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed) {
            analog.lastdigital = 0;
        }
    }

    public static void frame_update_analog_field_opwolf_p1y(analog_field_state analog) {
        boolean keypressed = false;
        long keyscale;
        int rawvalue;
        int delta = 0;
        int value2;
        value2 = apply_analog_min_max(analog, analog.accum);
        analog.previous = analog.accum = value2;
        rawvalue = Mouse.deltaY;
        if (rawvalue != 0) {
            delta = rawvalue;
            analog.lastdigital = 0;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            keypressed = true;
            delta -= analog.delta * 0x200;
            analog.lastdigital = 1;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            keypressed = true;
            delta += analog.delta * 0x200;
            analog.lastdigital = 2;
        }
        if (Mouse.deltaY < 0) {
            keypressed = true;
            delta += Mouse.deltaY * 0x200;
            analog.lastdigital = 1;
        }
        if (Mouse.deltaY > 0) {
            keypressed = true;
            delta += Mouse.deltaY * 0x200;
            analog.lastdigital = 2;
        }
        analog.accum += delta;
        if (!keypressed) {
            analog.lastdigital = 0;
        }
    }

//#region KeyStruct

    private static List<KeyStruct> lks;
    public static List<Integer> lk;

    public static class KeyStruct {

        public final int key;
        public final char c;

        public KeyStruct(int _key, char _c) {
            key = _key;
            c = _c;
        }
    }

    public static char getcharbykey(int key1) {
        char c1 = ' ';
        for (KeyStruct ks : lks) {
            if (ks.key == key1) {
                c1 = ks.c;
                break;
            }
        }
        return c1;
    }

    public static void input_init() {
        lk = new ArrayList<>();
        lk.add(KeyEvent.VK_1);
        lk.add(KeyEvent.VK_2);
        lk.add(KeyEvent.VK_3);
        lk.add(KeyEvent.VK_4);
        lk.add(KeyEvent.VK_5);
        lk.add(KeyEvent.VK_6);
        lk.add(KeyEvent.VK_7);
        lk.add(KeyEvent.VK_8);
        lk.add(KeyEvent.VK_9);
        lk.add(KeyEvent.VK_0);
        lk.add(KeyEvent.VK_A);
        lk.add(KeyEvent.VK_B);
        lk.add(KeyEvent.VK_C);
        lk.add(KeyEvent.VK_D);
        lk.add(KeyEvent.VK_E);
        lk.add(KeyEvent.VK_F);
        lk.add(KeyEvent.VK_G);
        lk.add(KeyEvent.VK_H);
        lk.add(KeyEvent.VK_I);
        lk.add(KeyEvent.VK_J);
        lk.add(KeyEvent.VK_K);
        lk.add(KeyEvent.VK_L);
        lk.add(KeyEvent.VK_M);
        lk.add(KeyEvent.VK_N);
        lk.add(KeyEvent.VK_O);
        lk.add(KeyEvent.VK_P);
        lk.add(KeyEvent.VK_Q);
        lk.add(KeyEvent.VK_R);
        lk.add(KeyEvent.VK_S);
        lk.add(KeyEvent.VK_T);
        lk.add(KeyEvent.VK_U);
        lk.add(KeyEvent.VK_V);
        lk.add(KeyEvent.VK_W);
        lk.add(KeyEvent.VK_X);
        lk.add(KeyEvent.VK_Y);
        lk.add(KeyEvent.VK_Z);
        lks = new ArrayList<>();
        lks.add(new KeyStruct(KeyEvent.VK_1, '1'));
        lks.add(new KeyStruct(KeyEvent.VK_2, '2'));
        lks.add(new KeyStruct(KeyEvent.VK_3, '3'));
        lks.add(new KeyStruct(KeyEvent.VK_4, '4'));
        lks.add(new KeyStruct(KeyEvent.VK_5, '5'));
        lks.add(new KeyStruct(KeyEvent.VK_6, '6'));
        lks.add(new KeyStruct(KeyEvent.VK_7, '7'));
        lks.add(new KeyStruct(KeyEvent.VK_8, '8'));
        lks.add(new KeyStruct(KeyEvent.VK_9, '9'));
        lks.add(new KeyStruct(KeyEvent.VK_0, '0'));
        lks.add(new KeyStruct(KeyEvent.VK_A, 'a'));
        lks.add(new KeyStruct(KeyEvent.VK_B, 'b'));
        lks.add(new KeyStruct(KeyEvent.VK_C, 'c'));
        lks.add(new KeyStruct(KeyEvent.VK_D, 'd'));
        lks.add(new KeyStruct(KeyEvent.VK_E, 'e'));
        lks.add(new KeyStruct(KeyEvent.VK_F, 'f'));
        lks.add(new KeyStruct(KeyEvent.VK_G, 'g'));
        lks.add(new KeyStruct(KeyEvent.VK_H, 'h'));
        lks.add(new KeyStruct(KeyEvent.VK_I, 'i'));
        lks.add(new KeyStruct(KeyEvent.VK_J, 'j'));
        lks.add(new KeyStruct(KeyEvent.VK_K, 'k'));
        lks.add(new KeyStruct(KeyEvent.VK_L, 'l'));
        lks.add(new KeyStruct(KeyEvent.VK_M, 'm'));
        lks.add(new KeyStruct(KeyEvent.VK_N, 'n'));
        lks.add(new KeyStruct(KeyEvent.VK_O, 'o'));
        lks.add(new KeyStruct(KeyEvent.VK_P, 'p'));
        lks.add(new KeyStruct(KeyEvent.VK_Q, 'q'));
        lks.add(new KeyStruct(KeyEvent.VK_R, 'r'));
        lks.add(new KeyStruct(KeyEvent.VK_S, 's'));
        lks.add(new KeyStruct(KeyEvent.VK_T, 't'));
        lks.add(new KeyStruct(KeyEvent.VK_U, 'u'));
        lks.add(new KeyStruct(KeyEvent.VK_V, 'v'));
        lks.add(new KeyStruct(KeyEvent.VK_W, 'w'));
        lks.add(new KeyStruct(KeyEvent.VK_X, 'x'));
        lks.add(new KeyStruct(KeyEvent.VK_Y, 'y'));
        lks.add(new KeyStruct(KeyEvent.VK_Z, 'z'));
    }

//#endregion
}
