/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;


import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime;
import m1.emu.Cpuint.LineState;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.mame.pgm.PGM;


public class ICS2115 {

    public static final byte V_ON = 1;
    public static final byte V_DONE = 2;
    public static voice_struct[] voice2;
    public static timer_struct[] timer;
    public static byte[] icsrom;
    public static short[] ulaw = new short[256];
    public static byte osc_select;
    public static byte reg_select;
    public static byte irq_enabled, irq_pending;
    public static boolean irq_on;
//    public static byte active_osc;
//    public static byte vmode;
//    public static short[] volume = new ushort[4096];
//    public static ics2115_voice[] voice;
//    public struct osc_struct {
//        public int left;
//        public int acc, start, end;
//        public short fc;
//        public byte ctl, saddr;
//    }
//
//    public struct vol_struct {
//        public int left;
//        public int add;
//        public int start, end;
//        public int acc;
//        public short regacc;
//        public byte incr;
//        public byte pan, mode;
//    }
//
//    public class osc_conf_struct {
//
//        public boolean ulaw;
//        public boolean stop;
//        public boolean eightbit;
//        public boolean loop;
//        public boolean loop_bidir;
//        public boolean irq;
//        public boolean invert;
//        public boolean irq_pending;
//        private byte value {
//        public byte getValue() {
//            byte value = 0;
//            if (ulaw) value |= 0x01;
//            if (stop) value |= 0x02;
//            if (eightbit) value |= 0x04;
//            if (loop) value |= 0x08;
//            if (loop_bidir) value |= 0x10;
//            if (irq) value |= 0x20;
//            if (invert) value |= 0x40;
//            if (irq_pending) value |= 0x80;
//            return value;
//        }
//        public void setValue(byte value) {
//            ulaw = (value & 0x01) != 0;
//            stop = (value & 0x02) != 0;
//            eightbit = (value & 0x04) != 0;
//            loop = (value & 0x08) != 0;
//            loop_bidir = (value & 0x10) != 0;
//            irq = (value & 0x20) != 0;
//            invert = (value & 0x40) != 0;
//            irq_pending = (value & 0x80) != 0;
//        }
//    }
//
//    public class vol_ctrl_struct {
//
//        public boolean done;
//        public boolean stop;
//        public boolean rollover;
//        public boolean loop;
//        public boolean loop_bidir;
//        public boolean irq;
//        public boolean invert;
//        public boolean irq_pending;
//        private byte value;
//        public byte getValue() {
//            byte value = 0;
//            if (done) value |= 0x01;
//            if (stop) value |= 0x02;
//            if (rollover) value |= 0x04;
//            if (loop) value |= 0x08;
//            if (loop_bidir) value |= 0x10;
//            if (irq) value |= 0x20;
//            if (invert) value |= 0x40;
//            if (irq_pending) value |= 0x80;
//            return value;
//        }
//        public void setValue(byte value) {
//            done = (value & 0x01) != 0;
//            stop = (value & 0x02) != 0;
//            rollover = (value & 0x04) != 0;
//            loop = (value & 0x08) != 0;
//            loop_bidir = (value & 0x10) != 0;
//            irq = (value & 0x20) != 0;
//            invert = (value & 0x40) != 0;
//            irq_pending = (value & 0x80) != 0;
//        }
//    }
//
//    public class state_struct {
//
//        public bool on;
//        public byte ramp;
//        private byte value {
//        public byte getValue() {
//            if (on) value |= 0x80;
//            value |= (byte) (ramp & 0x4f);
//            return value;
//        }
//        public void setValue(byte value) {
//            on = (value & 0x80) != 0;
//            ramp = (byte) (value & 0x4f);
//        }
//    }
//
//    public class ics2115_voice {
//
//        public osc_struct osc;
//        public vol_struct vol;
//        public osc_conf_struct osc_conf;
//        public vol_ctrl_struct vol_ctrl;
//        public state_struct state;
//
//        public bool playing() {
//            return state.on && !((vol_ctrl.done || vol_ctrl.stop) && osc_conf.stop);
//        }
//
//        public void update_ramp() {
//            if (state.on && !osc_conf.stop) {
//                if (state.ramp < 0x40)
//                    state.ramp += 0x1;
//                else
//                    state.ramp = 0x40;
//            } else {
//                if (state.ramp != 0)
//                    state.ramp -= 0x1;
//            }
//        }
//
//        public bool update_volume_envelope() {
//            bool ret = false;
//            if (vol_ctrl.done || vol_ctrl.stop)
//                return ret;
//            if (vol_ctrl.invert) {
//                vol.acc -= vol.add;
//                vol.left = (int) (vol.acc - vol.start);
//            } else {
//                vol.acc += vol.add;
//                vol.left = (int) (vol.end - vol.acc);
//            }
//            if (vol.left > 0)
//                return ret;
//            if (vol_ctrl.irq) {
//                vol_ctrl.irq_pending = true;
//                ret = true;
//            }
//            if (osc_conf.eightbit)
//                return ret;
//            if (vol_ctrl.loop) {
//                if (vol_ctrl.loop_bidir)
//                    vol_ctrl.invert = !vol_ctrl.invert;
//                if (vol_ctrl.invert)
//                    vol.acc = (int) (vol.end + vol.left);
//                else
//                    vol.acc = (int) (vol.start - vol.left);
//            } else {
//                state.on = false;
//                vol_ctrl.done = true;
//                if (vol_ctrl.invert)
//                    vol.acc = vol.end;
//                else
//                    vol.acc = vol.start;
//            }
//            return ret;
//        }
//
//        public bool update_oscillator() {
//            bool ret = false;
//            if (osc_conf.stop)
//                return ret;
//            if (osc_conf.invert) {
//                osc.acc -= (uint) (osc.fc << 2);
//                osc.left = (int) (osc.acc - osc.start);
//            } else {
//                osc.acc += (uint) (osc.fc << 2);
//                osc.left = (int) (osc.end - osc.acc);
//            }
//            if (osc.left > 0)
//                return ret;
//            if (osc_conf.irq) {
//                osc_conf.irq_pending = true;
//                ret = true;
//            }
//            if (osc_conf.loop) {
//                if (osc_conf.loop_bidir)
//                    osc_conf.invert = !osc_conf.invert;
//                if (osc_conf.invert) {
//                    osc.acc = (int) (osc.end + osc.left);
//                    osc.left = (int) (osc.acc - osc.start);
//                } else {
//                    osc.acc = (int) (osc.start - osc.left);
//                    osc.left = (int) (osc.end - osc.acc);
//                }
//            } else {
//                state.on = false;
//                osc_conf.stop = true;
//                if (!osc_conf.invert)
//                    osc.acc = osc.end;
//                else
//                    osc.acc = osc.start;
//            }
//            return ret;
//        }
//    }
//
//    public static int get_sample(int osc) {
//        int curaddr = (uint) (((voice[osc].osc.saddr << 20) & 0xff_ffff) | (voice[osc].osc.acc >> 12));
//        int nextaddr;
//        if (voice[osc].state.on && voice[osc].osc_conf.loop && !voice[osc].osc_conf.loop_bidir && (voice[osc].osc.left < (voice[osc].osc.fc << 2))) {
//            nextaddr = (uint) (((voice[osc].osc.saddr << 20) & 0xff_ffff) | (voice[osc].osc.start >> 12));
//        } else
//            nextaddr = curaddr + 2;
//        short sample1, sample2;
//        if (voice[osc].osc_conf.eightbit) {
//            sample1 = (short) (((sbyte) icsrom[curaddr]) << 8);
//            sample2 = (short) (((sbyte) icsrom[curaddr + 1]) << 8);
//        } else {
//            sample1 = (short) (icsrom[curaddr + 0] | (((sbyte) icsrom[curaddr + 1]) << 8));
//            sample2 = (short) (icsrom[nextaddr + 0] | (((sbyte) icsrom[nextaddr + 1]) << 8));
//        }
//        int sample, diff;
//        short fract;
//        diff = sample2 - sample1;
//        fract = (ushort) ((voice[osc].osc.acc >> 3) & 0x1ff);
//        sample = (((int) sample1 << 9) + diff * fract) >> 9;
//        return sample;
//    }
//
//    public static boolean fill_output(int osc, int offset, int samples) {
//        int i;
//        boolean irq_invalid = false;
//        short fine = (short) (1 << (3 * (voice[osc].vol.incr >> 6)));
//        voice[osc].vol.add = (uint) ((voice[osc].vol.incr & 0x3f) << (10 - fine));
//        for (i = 0; i < samples; i++) {
//            int volacc = (voice[osc].vol.acc >> 10) & 0xffff;
//            int volume2 = (int) ((volume[(int) (volacc >> 4)] * voice[osc].state.ramp) >> 6);
//            short vleft = (short) volume2;
//            short vright = (short) volume2;
//            int sample;
//            if (voice[osc].osc_conf.ulaw) {
//                uint curaddr = (uint) (((voice[osc].osc.saddr << 20) & 0xff_ffff) | (voice[osc].osc.acc >> 12));
//                sample = ulaw[icsrom[curaddr]];
//            } else {
//                sample = get_sample(osc);
//            }
//            if (vmode == 0 || voice[osc].playing()) {
//                Sound.ics2115stream.streamoutput[0][offset + i] += (sample * vleft) >> 4;
//                Sound.ics2115stream.streamoutput[1][offset + i] += (sample * vright) >> 4;
//            }
//            voice[osc].update_ramp();
//            if (voice[osc].playing()) {
//                if (voice[osc].update_oscillator())
//                    irq_invalid = true;
//                if (voice[osc].update_volume_envelope())
//                    irq_invalid = true;
//            }
//        }
//        return irq_invalid;
//    }
//
//    public static void reg_write(byte data, bool msb) {
//        switch (reg_select) {
//            case 0x00: // [osc] Oscillator Configuration
//                if (msb) {
//                    voice[osc_select].osc_conf.value &= 0x80;
//                    voice[osc_select].osc_conf.value |= (byte) (data & 0x7f);
//                }
//                break;
//            case 0x01: // [osc] Wavesample frequency
//                // freq = fc*33075/1024 in 32 voices mode, fc*44100/1024 in 24 voices mode
//                if (msb)
//                    voice[osc_select].osc.fc = (ushort) ((voice[osc_select].osc.fc & 0x00ff) | (data << 8));
//                else
//                    //last bit not used!
//                    voice[osc_select].osc.fc = (ushort) ((voice[osc_select].osc.fc & 0xff00) | (data & 0xfe));
//                break;
//            case 0x02: // [osc] Wavesample loop start high
//                if (msb)
//                    voice[osc_select].osc.start = (uint) ((voice[osc_select].osc.start & 0x00ff_ffff) | (data << 24));
//                else
//                    voice[osc_select].osc.start = (uint) ((voice[osc_select].osc.start & 0xff00_ffff) | (data << 16));
//                break;
//            case 0x03: // [osc] Wavesample loop start low
//                if (msb)
//                    voice[osc_select].osc.start = (uint) ((voice[osc_select].osc.start & 0xffff_00ff) | (data << 8));
//                break;
//            case 0x04: // [osc] Wavesample loop end high
//                if (msb)
//                    voice[osc_select].osc.end = (uint) ((voice[osc_select].osc.end & 0x00ff_ffff) | (data << 24));
//                else
//                    voice[osc_select].osc.end = (uint) ((voice[osc_select].osc.end & 0xff00_ffff) | (data << 16));
//                break;
//            case 0x05: // [osc] Wavesample loop end low
//                if (msb)
//                    voice[osc_select].osc.end = (uint) ((voice[osc_select].osc.end & 0xffff_00ff) | (data << 8));
//                break;
//            case 0x06: // [osc] Volume Increment
//                if (msb)
//                    voice[osc_select].vol.incr = data;
//                break;
//            case 0x07: // [osc] Volume Start
//                if (!msb)
//                    voice[osc_select].vol.start = (uint) (data << (10 + 8));
//                break;
//            case 0x08: // [osc] Volume End
//                if (!msb)
//                    voice[osc_select].vol.end = (uint) (data << (10 + 8));
//                break;
//            case 0x09: // [osc] Volume accumulator
//                if (msb)
//                    voice[osc_select].vol.regacc = (ushort) ((voice[osc_select].vol.regacc & 0x00ff) | (data << 8));
//                else
//                    voice[osc_select].vol.regacc = (ushort) ((voice[osc_select].vol.regacc & 0xff00) | data);
//                voice[osc_select].vol.acc = (uint) (voice[osc_select].vol.regacc << 10);
//                break;
//            case 0x0A: // [osc] Wavesample address high
//
//                if (msb)
//                    voice[osc_select].osc.acc = (uint) ((voice[osc_select].osc.acc & 0x00ff_ffff) | (data << 24));
//                else
//                    voice[osc_select].osc.acc = (uint) ((voice[osc_select].osc.acc & 0xff00_ffff) | (data << 16));
//                break;
//            case 0x0B: // [osc] Wavesample address low
//                if (msb)
//                    voice[osc_select].osc.acc = (uint) ((voice[osc_select].osc.acc & 0xffff_00ff) | (data << 8));
//                else
//                    voice[osc_select].osc.acc = (uint) ((voice[osc_select].osc.acc & 0xffff_ff00) | (data & 0xf8));
//                break;
//            case 0x0C: // [osc] Pan
//                if (msb)
//                    voice[osc_select].vol.pan = data;
//                break;
//            case 0x0D: // [osc] Volume Envelope Control
//                if (msb) {
//                    voice[osc_select].vol_ctrl.value &= 0x80;
//                    voice[osc_select].vol_ctrl.value |= (byte) (data & 0x7f);
//                }
//                break;
//            case 0x0E: // Active Voices
//                // Does this value get added to 1? Not sure. Could trace for writes of 32.
//                if (msb) {
//                    active_osc = (byte) (data & 0x1F); // & 0x1F ? (Guessing)
//                }
//                break;
//            //2X8 ?
//            case 0x10: // [osc] Oscillator Control
//                // Could this be 2X9?
//                // [7 R | 6 M2 | 5 M1 | 4-2 Reserve | 1 - Timer 2 Strt | 0 - Timer 1 Strt]
//                if (msb) {
//                    voice[osc_select].osc.ctl = data;
//                    if (data == 0)
//                        keyon();
//                        //guessing here
//                    else if (data == 0xf) {
//                        if (vmode == 0) {
//                            voice[osc_select].osc_conf.stop = true;
//                            voice[osc_select].vol_ctrl.stop = true;
//                            //try to key it off as well!
//                            voice[osc_select].state.on = false;
//                        }
//                    }
//                }
//                break;
//            case 0x11: // [osc] Wavesample static address 27-20
//                if (msb)
//                    //v->Osc.SAddr = data;
//                    voice[osc_select].osc.saddr = data;
//                break;
//            case 0x12:
//                //Could be per voice! -- investigate.
//                if (msb)
//                    vmode = data;
//                break;
//            case 0x40: // Timer 1 Preset
//            case 0x41: // Timer 2 Preset
//                if (!msb) {
//                    timer[reg_select & 0x1].preset = data;
//                    recalc_timer(reg_select & 0x1);
//                }
//                break;
//            case 0x42: // Timer 1 Prescale
//            case 0x43: // Timer 2 Prescale
//                if (!msb) {
//                    timer[reg_select & 0x1].scale = data;
//                    recalc_timer(reg_select & 0x1);
//                }
//                break;
//            case 0x4A: // IRQ Enable
//                if (!msb) {
//                    irq_enabled = data;
//                    recalc_irq();
//                }
//                break;
//            case 0x4F: // Oscillator Address being Programmed
//                if (!msb) {
//                    osc_select = (byte) (data % (1 + active_osc));
//                }
//                break;
//            default:
//                break;
//        }
//    }
//
//    public static short reg_read() {
//        short ret;
//        switch (reg_select) {
//            case 0x00: // [osc] Oscillator Configuration
//                ret = voice[osc_select].osc_conf.value;
//                ret <<= 8;
//                break;
//            case 0x01: // [osc] Wavesample frequency
//                // freq = fc*33075/1024 in 32 voices mode, fc*44100/1024 in 24 voices mode
//                //ret = v.Osc.FC;
//                ret = voice[osc_select].osc.fc;
//                break;
//            case 0x02: // [osc] Wavesample loop start high
//                // TODO are these returns valid? might be 0x00ff for this one...
//                ret = (short) ((voice[osc_select].osc.start >> 16) & 0xffff);
//                break;
//            case 0x03: // [osc] Wavesample loop start low
//                ret = (short) ((voice[osc_select].osc.start >> 0) & 0xff00);
//                break;
//            case 0x04: // [osc] Wavesample loop end high
//                ret = (short) ((voice[osc_select].osc.end >> 16) & 0xffff);
//                break;
//            case 0x05: // [osc] Wavesample loop end low
//                ret = (short) ((voice[osc_select].osc.end >> 0) & 0xff00);
//                break;
//            case 0x06: // [osc] Volume Increment
//                ret = voice[osc_select].vol.incr;
//                break;
//            case 0x07: // [osc] Volume Start
//                ret = (short) (voice[osc_select].vol.start >> (10 + 8));
//                break;
//            case 0x08: // [osc] Volume End
//                ret = (short) (voice[osc_select].vol.end >> (10 + 8));
//                break;
//            case 0x09: // [osc] Volume accumulator
//                //ret = v.Vol.Acc;
//                ret = (short) (voice[osc_select].vol.acc >> (10));
//                break;
//            case 0x0A: // [osc] Wavesample address
//                ret = (short) ((voice[osc_select].osc.acc >> 16) & 0xffff);
//                break;
//            case 0x0B: // [osc] Wavesample address
//                ret = (short) ((voice[osc_select].osc.acc >> 0) & 0xfff8);
//                break;
//            case 0x0C: // [osc] Pan
//                ret = (short) (voice[osc_select].vol.pan << 8);
//                break;
//            case 0x0D: // [osc] Volume Envelope Control
//                if (vmode == 0)
//                    ret = (short) (voice[osc_select].vol_ctrl.irq ? 0x81 : 0x01);
//                else
//                    ret = 0x01;
//                ret <<= 8;
//                break;
//            case 0x0E: // Active Voices
//                ret = active_osc;
//                break;
//            case 0x0F: { // [osc] Interrupt source/oscillator
//                ret = 0xff;
//                for (int i = 0; i <= active_osc; i++) {
//                    if (voice[i].osc_conf.irq_pending || voice[i].vol_ctrl.irq_pending) {
//                        ret = (short) (i | 0xe0);
//                        ret &= (short) (voice[i].vol_ctrl.irq_pending ? (~0x40) : 0xff);
//                        ret &= (short) (voice[i].osc_conf.irq_pending ? (~0x80) : 0xff);
//                        recalc_irq();
//                        if (voice[i].osc_conf.irq_pending) {
//                            voice[i].osc_conf.irq_pending = false;
//                            ret &= (short) (~0x80);
//                        }
//                        if (voice[i].vol_ctrl.irq_pending) {
//                            voice[i].vol_ctrl.irq_pending = false;
//                            ret &= (short) (~0x40);
//                        }
//                        break;
//                    }
//                }
//                ret <<= 8;
//                break;
//            }
//            case 0x10: // [osc] Oscillator Control
//                ret = (short) (voice[osc_select].osc.ctl << 8);
//                break;
//            case 0x11: // [osc] Wavesample static address 27-20
//                ret = (short) (voice[osc_select].osc.saddr << 8);
//                break;
//            case 0x40: // Timer 0 clear irq
//            case 0x41: // Timer 1 clear irq
//                // TODO examine this suspect code
//                ret = timer[reg_select & 0x1].preset;
//                irq_pending &= (byte) (~(1 << (reg_select & 0x1)));
//                recalc_irq();
//                break;
//            case 0x43: // Timer status
//                ret = (short) (irq_pending & 3);
//                break;
//            case 0x4a: // IRQ Pending
//                ret = irq_pending;
//                break;
//            case 0x4b: // Address of Interrupting Oscillator
//                ret = 0x80;
//                break;
//            case 0x4c: // Chip Revision
//                ret = 0x01;
//                break;
//            default:
//                ret = 0;
//                break;
//        }
//        return ret;
//    }
//
//    public static void recalc_irq() {
//        int i;
//        bool irq = (irq_pending & irq_enabled) != 0;
//        for (i = 0; (!irq) && (i < 32); i++)
//            irq |= voice[i].vol_ctrl.irq_pending && voice[i].osc_conf.irq_pending;
//        irq_on = irq;
//        Pgm.sound_irq(irq ? (int) LineState.ASSERT_LINE : (int) LineState.CLEAR_LINE);
//    }
//
//    public static void ics2115_update(int offset, int length) {
//        int osc, i;
//        bool irq_invalid = false;
//        for (i = 0; i < length; i++) {
//            Sound.ics2115stream.streamoutput[0][offset + i] = 0;
//            Sound.ics2115stream.streamoutput[1][offset + i] = 0;
//        }
//        for (osc = 0; osc <= active_osc; osc++) {
//            if (fill_output(osc, offset, length))
//                irq_invalid = true;
//        }
//        for (i = 0; i < length; i++) {
//            Sound.ics2115stream.streamoutput[0][offset + i] >>= 16;
//            Sound.ics2115stream.streamoutput[1][offset + i] >>= 16;
//        }
//        if (irq_invalid)
//            recalc_irq();
//    }
//
//    public static void keyon() {
//        voice[osc_select].state.on = true;
//        voice[osc_select].state.ramp = 0x40;
//    }
//
//    public static void recalc_timer(int i) {
//        long period = ((timer[i].scale & 0x1f) + 1) * (timer[i].preset + 1);
//        period = (period << (4 + (timer[i].scale >> 5))) * 78125 / 2646;
//        if (timer[i].period != period) {
//            timer[i].period = period;
//            if (period != 0)
//                Timer.timer_adjust_periodic(timer[i].timer, Attotime.ATTOTIME_IN_NSEC(period), Attotime.ATTOTIME_IN_NSEC(period));
//            else
//                Timer.timer_adjust_periodic(timer[i].timer, Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
//        }
//    }
//
//    public static void ics2115_start() {
//        int i;
//        voice = new ics2115_voice[32];
//        timer = new timer_struct[2];
//        timer[0].timer = Timer.timer_alloc_common(timer_cb_0, "timer_cb_0", false);
//        timer[1].timer = Timer.timer_alloc_common(timer_cb_1, "timer_cb_1", false);
//        for (i = 0; i < 32; i++) {
//            voice[i] = new ics2115_voice();
//            voice[i].osc = new osc_struct();
//            voice[i].vol = new vol_struct();
//            voice[i].osc_conf = new osc_conf_struct();
//            voice[i].vol_ctrl = new vol_ctrl_struct();
//            voice[i].state = new state_struct();
//        }
//        for (i = 0; i < 4096; i++) {
//            volume[i] = (short) (((0x100 | (i & 0xff)) << 6) >> (15 - (i >> 8)));
//        }
//        short[] lut = new short[8];
//        short lut_initial = 33 << 2;
//        for (i = 0; i < 8; i++)
//            lut[i] = (ushort) ((lut_initial << i) - lut_initial);
//        for (i = 0; i < 256; i++) {
//            byte exponent = (byte) ((~i >> 4) & 0x07);
//            byte mantissa = (byte) (~i & 0x0f);
//            byte value = (byte) (lut[exponent] + (mantissa << (exponent + 3)));
//            ulaw[i] = (short) (((i & 0x80) != 0) ? -value : value);
//        }
//    }
//
//    public static byte ics2115_r(int offset) {
//        byte ret = 0;
//        switch (offset) {
//            case 0:
//                if (irq_on) {
//                    int i;
//                    ret |= 0x80;
//                    if (irq_enabled != 0 && ((irq_pending & 3) != 0))
//                        ret |= 1;
//                    for (i = 0; i <= active_osc; i++) {
//                        if (voice[i].osc_conf.irq_pending) {
//                            ret |= 2;
//                            break;
//                        }
//                    }
//                }
//                break;
//            case 1:
//                ret = reg_select;
//                break;
//            case 2:
//                ret = (byte) (reg_read());
//                break;
//            case 3:
//                ret = (byte) (reg_read() >> 8);
//                break;
//            default:
//                break;
//        }
//        return ret;
//    }
//
//    public static void ics2115_reset() {
//        int i;
//        irq_enabled = 0;
//        irq_pending = 0;
//        active_osc = 31;
//        osc_select = 0;
//        reg_select = 0;
//        vmode = 0;
//        irq_on = false;
//        for (i = 0; i < 32; i++) {
//            voice[i].osc.left = 0;
//            voice[i].osc.acc = 0;
//            voice[i].osc.start = 0;
//            voice[i].osc.end = 0;
//            voice[i].osc.fc = 0;
//            voice[i].osc.ctl = 0;
//            voice[i].osc.saddr = 0;
//            //voice[i].vol.left=0;
//            //voice[i].vol.add=0;
//            voice[i].vol.start = 0;
//            voice[i].vol.end = 0;
//            voice[i].vol.acc = 0;
//            //voice[i].vol.regacc=0;
//            voice[i].vol.incr = 0;
//            voice[i].vol.pan = 0x7f;
//            voice[i].vol.mode = 0;
//            voice[i].osc_conf.value = 2;
//            voice[i].vol_ctrl.value = 1;
//            voice[i].state.value = 0;
//        }
//        for (i = 0; i < 2; i++) {
//            Timer.timer_adjust_periodic(timer[i].timer, Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
//            timer[i].period = 0;
//            timer[i].scale = 0;
//            timer[i].preset = 0;
//        }
//    }

    public static class timer_struct {

        public byte scale, preset;
        public EmuTimer timer;
        public long period;
    }

    public static void ics2115_w(int offset, byte data) {
        switch (offset) {
            case 1:
                reg_select = data;
                break;
            case 2:
                reg_write(data, false);
                break;
            case 3:
                reg_write(data, true);
                break;
            default:
                break;
        }
    }

    public static void timer_cb_0() {
        irq_pending |= 1 << 0;
        recalc_irq();
    }

    public static void timer_cb_1() {
        irq_pending |= 1 << 1;
        recalc_irq();
    }

    public static class voice_struct {

        public short fc, addrh, addrl, strth, endh, volacc;
        public byte strtl, endl, saddr, pan, conf, ctl;
        public byte vstart, vend, vctl;
        public byte state;
    }

    public static void recalc_irq() {
        int i;
        boolean irq = false;
        if ((irq_enabled & irq_pending) != 0)
            irq = true;
        for (i = 0; !irq && i < 32; i++)
            if ((voice2[i].state & V_DONE) != 0)
                irq = true;
        if (irq != irq_on) {
            irq_on = irq;
            PGM.sound_irq(irq ? LineState.ASSERT_LINE.ordinal() : LineState.CLEAR_LINE.ordinal());
        }
    }

    public static void ics2115_update(int offset, int length) {
        int osc, i;
        boolean irq_invalid = false;
        for (i = 0; i < length; i++) {
            Sound.ics2115stream.streamoutput[0][offset + i] = 0;
            Sound.ics2115stream.streamoutput[1][offset + i] = 0;
        }
        for (osc = 0; osc < 32; osc++) {
            if ((voice2[osc].state & V_ON) != 0) {
                int adr = (voice2[osc].addrh << 16) | voice2[osc].addrl;
                int end = (voice2[osc].endh << 16) | (voice2[osc].endl << 8);
                int loop = (voice2[osc].strth << 16) | (voice2[osc].strtl << 8);
                int badr = (voice2[osc].saddr << 20) & 0xff_ffff;
                int delta = voice2[osc].fc << 2;
                byte conf = voice2[osc].conf;
                int vol = voice2[osc].volacc;
                vol = (((vol & 0xff0) | 0x1000) << (vol >> 12)) >> 12;
                for (i = 0; i < length; i++) {
                    int v;
                    if ((badr | adr >> 12) >= icsrom.length) {
                        v = 0;
                    } else {
                        v = icsrom[badr | (adr >> 12)];
                    }
                    if ((conf & 1) != 0)
                        v = ulaw[v];
                    else
                        v = ((byte) v) << 6;
                    v = (v * vol) >> (16 + 5);
                    Sound.ics2115stream.streamoutput[0][offset + i] += v;
                    Sound.ics2115stream.streamoutput[1][offset + i] += v;
                    adr += delta;
                    if (adr >= end) {
                        adr -= (end - loop);
                        voice2[osc].state &= (byte) (~V_ON);
                        voice2[osc].state |= V_DONE;
                        irq_invalid = true;
                        break;
                    }
                }
                voice2[osc].addrh = (short) (adr >> 16);
                voice2[osc].addrl = (short) (adr);
            }
        }
        if (irq_invalid) {
            recalc_irq();
        }
    }

    public static void keyon() {
        voice2[osc_select].state |= V_ON;
    }

    public static void recalc_timer(int i) {
        long period = 1_000_000_000L * timer[i].scale * timer[i].preset / 33868800;
        if (period != 0)
            period = (long) (1_000_000_000 / 62.8206);
        if (timer[i].period != period) {
            timer[i].period = period;
            if (period != 0)
                Timer.adjustPeriodic(timer[i].timer, Attotime.ATTOTIME_IN_NSEC(period), Attotime.ATTOTIME_IN_NSEC(period));
            else
                Timer.adjustPeriodic(timer[i].timer, Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
        }
    }

    static void reg_write(byte data, boolean msb) {
        switch (reg_select) {
            case 0x00: // [osc] Oscillator Configuration
                if (msb) {
                    voice2[osc_select].conf = data;
                }
                break;
            case 0x01: // [osc] Wavesample frequency
                // freq = fc*33075/1024 in 32 voices mode, fc*44100/1024 in 24 voices mode
                if (msb)
                    voice2[osc_select].fc = (short) ((voice2[osc_select].fc & 0xff) | (data << 8));
                else
                    voice2[osc_select].fc = (short) ((voice2[osc_select].fc & 0xff00) | data);
                break;
            case 0x02: // [osc] Wavesample loop start address 19-4
                if (msb)
                    voice2[osc_select].strth = (short) ((voice2[osc_select].strth & 0xff) | (data << 8));
                else
                    voice2[osc_select].strth = (short) ((voice2[osc_select].strth & 0xff00) | data);
                break;
            case 0x03: // [osc] Wavesample loop start address 3-0.3-0
                if (msb) {
                    voice2[osc_select].strtl = data;
                }
                break;
            case 0x04: // [osc] Wavesample loop end address 19-4
                if (msb)
                    voice2[osc_select].endh = (short) ((voice2[osc_select].endh & 0xff) | (data << 8));
                else
                    voice2[osc_select].endh = (short) ((voice2[osc_select].endh & 0xff00) | data);
                break;
            case 0x05: // [osc] Wavesample loop end address 3-0.3-0
                if (msb) {
                    voice2[osc_select].endl = data;
                }
                break;
            case 0x07: // [osc] Volume Start
                if (msb) {
                    voice2[osc_select].vstart = data;
                }
                break;
            case 0x08: // [osc] Volume End
                if (msb) {
                    voice2[osc_select].vend = data;
                }
                break;
            case 0x09: // [osc] Volume accumulator
                if (msb)
                    voice2[osc_select].volacc = (short) ((voice2[osc_select].volacc & 0xff) | (data << 8));
                else
                    voice2[osc_select].volacc = (short) ((voice2[osc_select].volacc & 0xff00) | data);
                break;
            case 0x0a: // [osc] Wavesample address 19-4
                if (msb)
                    voice2[osc_select].addrh = (short) ((voice2[osc_select].addrh & 0xff) | (data << 8));
                else
                    voice2[osc_select].addrh = (short) ((voice2[osc_select].addrh & 0xff00) | data);
                break;
            case 0x0b: // [osc] Wavesample address 3-0.8-0
                if (msb)
                    voice2[osc_select].addrl = (short) ((voice2[osc_select].addrl & 0xff) | (data << 8));
                else
                    voice2[osc_select].addrl = (short) ((voice2[osc_select].addrl & 0xff00) | data);
                break;
            case 0x0c: // [osc] Pan
                if (msb) {
                    voice2[osc_select].pan = data;
                }
                break;
            case 0x0d: // [osc] Volume Enveloppe Control
                if (msb) {
                    voice2[osc_select].vctl = data;
                }
                break;
            case 0x10: // [osc] Oscillator Control
                if (msb) {
                    voice2[osc_select].ctl = data;
                    if (data == 0)
                        keyon();
                }
                break;
            case 0x11: // [osc] Wavesample static address 27-20
                if (msb) {
                    voice2[osc_select].saddr = data;
                }
                break;

            case 0x40: // Timer 1 Preset
                if (!msb) {
                    timer[0].preset = data;
                    recalc_timer(0);
                }
                break;
            case 0x41: // Timer 2 Preset
                if (!msb) {
                    timer[1].preset = data;
                    recalc_timer(1);
                }
                break;
            case 0x42: // Timer 1 Prescaler
                if (!msb) {
                    timer[0].scale = data;
                    recalc_timer(0);
                }
                break;
            case 0x43: // Timer 2 Prescaler
                if (!msb) {
                    timer[1].scale = data;
                    recalc_timer(1);
                }
                break;
            case 0x4a: // IRQ Enable
                if (!msb) {
                    irq_enabled = data;
                    recalc_irq();
                }
                break;

            case 0x4f: // Oscillator Address being Programmed
                if (!msb) {
                    osc_select = (byte) (data & 31);
                }
                break;
            default:
                break;
        }
    }

    public static short reg_read() {
        switch (reg_select) {
            case 0x0d:
                return 0x100;
            case 0x0f: {
                int osc;
                byte res = (byte) 0xff;
                for (osc = 0; osc < 32; osc++)
                    if ((voice2[osc].state & V_DONE) != 0) {
                        voice2[osc].state &= (byte) (~V_DONE);
                        recalc_irq();
                        res = (byte) (0x40 | osc);
                        break;
                    }
                return (short) (res << 8);
            }
            case 0x40:
                irq_pending &= (byte) (~(1 << 0));
                recalc_irq();
                return timer[0].preset;
            case 0x41:
                irq_pending &= (byte) (~(1 << 1));
                recalc_irq();
                return timer[1].preset;
            case 0x43:
                return (short) (irq_pending & 3);
            case 0x4a:
                return irq_pending;
            case 0x4b:
                return 0x80;
            case 0x4c:
                return 0x01;
            default:
                return 0;
        }
    }

    public static void ics2115_start() {
        int i;
        voice2 = new voice_struct[32];
        timer = new timer_struct[2];
        timer[0].timer = Timer.allocCommon(ICS2115::timer_cb_0, "timer_cb_0", false);
        timer[1].timer = Timer.allocCommon(ICS2115::timer_cb_1, "timer_cb_1", false);
        ulaw = new short[256];
        for (i = 0; i < 256; i++) {
            byte c = (byte) (~i);
            int v;
            v = ((c & 15) << 1) + 33;
            v <<= ((c & 0x70) >> 4);
            if ((c & 0x80) != 0)
                v = 33 - v;
            else
                v = v - 33;
            ulaw[i] = (short) v;
        }
    }

    public static byte ics2115_r(int offset) {
        byte ret = 0;
        switch (offset) {
            case 0:
                if (irq_on) {
                    int i;
                    ret |= (byte) 0x80;
                    if ((irq_enabled & irq_pending & 3) != 0)
                        ret |= 1;
                    for (i = 0; i < 32; i++)
                        if ((voice2[i].state & V_DONE) != 0) {
                            ret |= 2;
                            break;
                        }
                }
                break;
            case 1:
                ret = reg_select;
                break;
            case 2:
                ret = (byte) (reg_read());
                break;
            case 3:
                ret = (byte) (reg_read() >> 8);
                break;
            default:
                break;
        }
        return ret;
    }

    public static void ics2115_reset() {
        int i;
        irq_enabled = 0;
        irq_pending = 0;
        for (i = 0; i < 32; i++) {
            voice2[i].fc = 0;
            voice2[i].addrh = 0;
            voice2[i].addrl = 0;
            voice2[i].strth = 0;
            voice2[i].endh = 0;
            voice2[i].volacc = 0;
            voice2[i].strtl = 0;
            voice2[i].endl = 0;
            voice2[i].saddr = 0;
            voice2[i].pan = 0;
            voice2[i].conf = 0;
            voice2[i].ctl = 0;
            voice2[i].vstart = 0;
            voice2[i].vend = 0;
            voice2[i].vctl = 0;
            voice2[i].state = 0;
        }
        for (i = 0; i < 2; i++) {
            Timer.adjustPeriodic(timer[i].timer, Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
            timer[i].period = 0;
        }
        recalc_irq();
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i;
        for (i = 0; i < 32; i++) {
            writer.write(voice2[i].fc);
            writer.write(voice2[i].addrh);
            writer.write(voice2[i].addrl);
            writer.write(voice2[i].strth);
            writer.write(voice2[i].endh);
            writer.write(voice2[i].volacc);
            writer.write(voice2[i].strtl);
            writer.write(voice2[i].endl);
            writer.write(voice2[i].saddr);
            writer.write(voice2[i].pan);
            writer.write(voice2[i].conf);
            writer.write(voice2[i].ctl);
            writer.write(voice2[i].vstart);
            writer.write(voice2[i].vend);
            writer.write(voice2[i].vctl);
            writer.write(voice2[i].state);
        }
        for (i = 0; i < 2; i++) {
            writer.write(timer[i].scale);
            writer.write(timer[i].preset);
            writer.write(timer[i].period);
        }
        writer.write(osc_select);
        writer.write(reg_select);
        writer.write(irq_enabled);
        writer.write(irq_pending);
        writer.write(irq_on);
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i;
        for (i = 0; i < 32; i++) {
            voice2[i].fc = reader.readUInt16();
            voice2[i].addrh = reader.readUInt16();
            voice2[i].addrl = reader.readUInt16();
            voice2[i].strth = reader.readUInt16();
            voice2[i].endh = reader.readUInt16();
            voice2[i].volacc = reader.readUInt16();
            voice2[i].strtl = reader.readByte();
            voice2[i].endl = reader.readByte();
            voice2[i].saddr = reader.readByte();
            voice2[i].pan = reader.readByte();
            voice2[i].conf = reader.readByte();
            voice2[i].ctl = reader.readByte();
            voice2[i].vstart = reader.readByte();
            voice2[i].vend = reader.readByte();
            voice2[i].vctl = reader.readByte();
            voice2[i].state = reader.readByte();
        }
        for (i = 0; i < 2; i++) {
            timer[i].scale = reader.readByte();
            timer[i].preset = reader.readByte();
            timer[i].period = reader.readInt64();
        }
        osc_select = reader.readByte();
        reg_select = reader.readByte();
        irq_enabled = reader.readByte();
        irq_pending = reader.readByte();
        irq_on = reader.readBoolean();
    }
}
