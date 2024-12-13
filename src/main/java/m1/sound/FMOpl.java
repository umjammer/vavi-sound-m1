/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import m1.emu.Attotime;
import m1.emu.Attotime.Atime;


public class FMOpl {

    public static class OPL_SLOT {

        public int ar;
        public int dr;
        public int rr;
        public byte KSR;
        public byte ksl;
        public byte ksr;
        public byte mul;

        public int Cnt;
        public int Incr;
        public byte FB;
        public int iconnect;
        public int[] op1_out;
        public byte CON;

        public byte eg_type;
        public byte state;
        public int TL;
        public int TLL;
        public int volume;
        public int sl;
        public byte eg_sh_ar;
        public byte eg_sel_ar;
        public byte eg_sh_dr;
        public byte eg_sel_dr;
        public byte eg_sh_rr;
        public byte eg_sel_rr;
        public int key;

        public int AMmask;
        public byte vib;

        public short wavetable;
    }

    public static class OPL_CH {

        public OPL_SLOT[] SLOT;
        public int block_fnum;
        public int fc;
        public int ksl_base;
        public byte kcode;
    }

    public static class FM_OPL {

        public OPL_CH[] P_CH;

        public int eg_cnt;
        public int eg_timer;
        public int eg_timer_add;
        public int eg_timer_overflow;

        public byte rhythm;

        public int[] fn_tab;

        public byte lfo_am_depth;
        public byte lfo_pm_depth_range;
        public int lfo_am_cnt;
        public int lfo_am_inc;
        public int lfo_pm_cnt;
        public int lfo_pm_inc;

        public int noise_rng;
        public int noise_p;
        public int noise_f;

        public byte wavesel;

        public int[] T;
        public byte[] st;

        public YMDeltat.YM_DELTAT deltat;

        public byte portDirection;
        public byte portLatch;
        // OPL_PORTHANDLER_R
        public Supplier<Byte> porthandler_r;
        // OPL_PORTHANDLER_W
        public Consumer<Byte> porthandler_w;
        // OPL_PORTHANDLER_R
        public Supplier<Byte> keyboardhandler_r;
        // OPL_PORTHANDLER_W
        public Consumer<Byte> keyboardhandler_w;

        // OPL_TIMERHANDLER
        public BiConsumer<Integer, Atime> timer_handler;
        // OPL_IRQHANDLER
        public Consumer<Integer> IRQHandler;
        // OPL_UPDATEHANDLER
        public Consumer<Integer> UpdateHandler;

        public byte type;
        public byte address;
        public byte status;
        public byte statusmask;
        public byte mode;

        public int clock;
        public int rate;
        public double freqbase;
        public Atime TimerBase;

        public void OPLResetChip() {
            int c, s;
            int i;
            eg_timer = 0;
            eg_cnt = 0;
            noise_rng = 1;
            mode = 0;
            OPL_STATUS_RESET(0x7f);
            OPLWriteReg(0x01, 0);
            OPLWriteReg(0x02, 0);
            OPLWriteReg(0x03, 0);
            OPLWriteReg(0x04, 0);
            for (i = 0xff; i >= 0x20; i--) {
                OPLWriteReg(i, 0);
            }
            for (c = 0; c < 9; c++) {
                for (s = 0; s < 2; s++) {
                    P_CH[c].SLOT[s].wavetable = 0;
                    P_CH[c].SLOT[s].state = 0;
                    P_CH[c].SLOT[s].volume = 0x1ff;
                }
            }
        }

        public void OPL_initalize() {
            int i;
            freqbase = (rate != 0) ? ((double) clock / 72.0) / rate : 0;
            TimerBase = Attotime.attotime_mul(Attotime.ATTOTIME_IN_HZ(clock), 72);
            for (i = 0; i < 1024; i++) {
                fn_tab[i] = (int) ((double) i * 64 * freqbase * (1 << (16 - 10)));
            }
            lfo_am_inc = (int) (0x4_0000 * freqbase);
            lfo_pm_inc = (int) (0x4000 * freqbase);
            noise_f = (int) (0x1_0000 * freqbase);
            eg_timer_add = (int) (0x1_0000 * freqbase);
            eg_timer_overflow = 0x1_0000;
        }

        private void FM_KEYON(int chan, int slot, int key_set) {
            if (P_CH[chan].SLOT[slot].key == 0) {
                P_CH[chan].SLOT[slot].Cnt = 0;
                P_CH[chan].SLOT[slot].state = 4;
            }
            P_CH[chan].SLOT[slot].key |= key_set;
        }

        private void FM_KEYOFF(int chan, int slot, int key_clr) {
            if (P_CH[chan].SLOT[slot].key != 0) {
                P_CH[chan].SLOT[slot].key &= key_clr;
                if (P_CH[chan].SLOT[slot].key == 0) {
                    if (P_CH[chan].SLOT[slot].state > 1)
                        P_CH[chan].SLOT[slot].state = 1;
                }
            }
        }

        private void CALC_FCSLOT(int chan, int slot) {
            int ksr;
            P_CH[chan].SLOT[slot].Incr = P_CH[chan].fc * P_CH[chan].SLOT[slot].mul;
            ksr = P_CH[chan].kcode >> P_CH[chan].SLOT[slot].KSR;
            if (P_CH[chan].SLOT[slot].ksr != ksr) {
                P_CH[chan].SLOT[slot].ksr = (byte) ksr;
                if ((P_CH[chan].SLOT[slot].ar + P_CH[chan].SLOT[slot].ksr) < 78) {
                    P_CH[chan].SLOT[slot].eg_sh_ar = eg_rate_shift[P_CH[chan].SLOT[slot].ar + P_CH[chan].SLOT[slot].ksr];
                    P_CH[chan].SLOT[slot].eg_sel_ar = eg_rate_select[P_CH[chan].SLOT[slot].ar + P_CH[chan].SLOT[slot].ksr];
                } else {
                    P_CH[chan].SLOT[slot].eg_sh_ar = 0;
                    P_CH[chan].SLOT[slot].eg_sel_ar = 13 * 8;
                }
                P_CH[chan].SLOT[slot].eg_sh_dr = eg_rate_shift[P_CH[chan].SLOT[slot].dr + P_CH[chan].SLOT[slot].ksr];
                P_CH[chan].SLOT[slot].eg_sel_dr = eg_rate_select[P_CH[chan].SLOT[slot].dr + P_CH[chan].SLOT[slot].ksr];
                P_CH[chan].SLOT[slot].eg_sh_rr = eg_rate_shift[P_CH[chan].SLOT[slot].rr + P_CH[chan].SLOT[slot].ksr];
                P_CH[chan].SLOT[slot].eg_sel_rr = eg_rate_select[P_CH[chan].SLOT[slot].rr + P_CH[chan].SLOT[slot].ksr];
            }
        }

        private void set_mul(int slot, int v) {
            P_CH[slot / 2].SLOT[slot & 1].mul = mul_tab[v & 0x0f];
            P_CH[slot / 2].SLOT[slot & 1].KSR = (byte) ((v & 0x10) != 0 ? 0 : 2);
            P_CH[slot / 2].SLOT[slot & 1].eg_type = (byte) (v & 0x20);
            P_CH[slot / 2].SLOT[slot & 1].vib = (byte) (v & 0x40);
            P_CH[slot / 2].SLOT[slot & 1].AMmask = (v & 0x80) != 0 ? ~0 : 0;
            CALC_FCSLOT(slot / 2, slot & 1);
        }

        private void set_ksl_tl(int slot, int v) {
            int ksl = v >> 6;
            P_CH[slot / 2].SLOT[slot & 1].ksl = (byte) (ksl != 0 ? 3 - ksl : 31);
            P_CH[slot / 2].SLOT[slot & 1].TL = (v & 0x3f) << 2;
            P_CH[slot / 2].SLOT[slot & 1].TLL = P_CH[slot / 2].SLOT[slot & 1].TL + (P_CH[slot / 2].ksl_base >> P_CH[slot / 2].SLOT[slot & 1].ksl);
        }

        private void set_ar_dr(int slot, int v) {
            P_CH[slot / 2].SLOT[slot & 1].ar = (v >> 4) != 0 ? 16 + ((v >> 4) << 2) : 0;
            if ((P_CH[slot / 2].SLOT[slot & 1].ar + P_CH[slot / 2].SLOT[slot & 1].ksr) < 16 + 62) {
                P_CH[slot / 2].SLOT[slot & 1].eg_sh_ar = eg_rate_shift[P_CH[slot / 2].SLOT[slot & 1].ar + P_CH[slot / 2].SLOT[slot & 1].ksr];
                P_CH[slot / 2].SLOT[slot & 1].eg_sel_ar = eg_rate_select[P_CH[slot / 2].SLOT[slot & 1].ar + P_CH[slot / 2].SLOT[slot & 1].ksr];
            } else {
                P_CH[slot / 2].SLOT[slot & 1].eg_sh_ar = 0;
                P_CH[slot / 2].SLOT[slot & 1].eg_sel_ar = 13 * 8;
            }
            P_CH[slot / 2].SLOT[slot & 1].dr = (v & 0x0f) != 0 ? 16 + ((v & 0x0f) << 2) : 0;
            P_CH[slot / 2].SLOT[slot & 1].eg_sh_dr = eg_rate_shift[P_CH[slot / 2].SLOT[slot & 1].dr + P_CH[slot / 2].SLOT[slot & 1].ksr];
            P_CH[slot / 2].SLOT[slot & 1].eg_sel_dr = eg_rate_select[P_CH[slot / 2].SLOT[slot & 1].dr + P_CH[slot / 2].SLOT[slot & 1].ksr];
        }

        private void set_sl_rr(int slot, int v) {
            P_CH[slot / 2].SLOT[slot & 1].sl = sl_tab[v >> 4];
            P_CH[slot / 2].SLOT[slot & 1].rr = (v & 0x0f) != 0 ? 16 + ((v & 0x0f) << 2) : 0;
            P_CH[slot / 2].SLOT[slot & 1].eg_sh_rr = eg_rate_shift[P_CH[slot / 2].SLOT[slot & 1].rr + P_CH[slot / 2].SLOT[slot & 1].ksr];
            P_CH[slot / 2].SLOT[slot & 1].eg_sel_rr = eg_rate_select[P_CH[slot / 2].SLOT[slot & 1].rr + P_CH[slot / 2].SLOT[slot & 1].ksr];
        }

        public void OPLWriteReg(int r, int v) {
            int slot;
            int block_fnum;
            r &= 0xff;
            v &= 0xff;
            switch (r & 0xe0) {
                case 0x00:
                    switch (r & 0x1f) {
                        case 0x01:
                            if ((type & 0x01) != 0) {
                                wavesel = (byte) (v & 0x20);
                            }
                            break;
                        case 0x02:
                            T[0] = (256 - v) * 4;
                            break;
                        case 0x03:
                            T[1] = (256 - v) * 16;
                            break;
                        case 0x04:
                            if ((v & 0x80) != 0) {
                                OPL_STATUS_RESET(0x7f - 0x08);
                            } else {
                                byte st1 = (byte) (v & 1);
                                byte st2 = (byte) ((v >> 1) & 1);
                                OPL_STATUS_RESET(v & (0x78 - 0x08));
                                OPL_STATUSMASK_SET((~v) & 0x78);
                                if (st[1] != st2) {
                                    Atime period = st2 != 0 ? Attotime.attotime_mul(TimerBase, T[1]) : Attotime.ATTOTIME_ZERO;
                                    st[1] = st2;
                                    if (timer_handler != null) {
                                        timer_handler.accept(1, period);
                                    }
                                }
                                if (st[0] != st1) {
                                    Atime period = st1 != 0 ? Attotime.attotime_mul(TimerBase, T[0]) : Attotime.ATTOTIME_ZERO;
                                    st[0] = st1;
                                    if (timer_handler != null) {
                                        timer_handler.accept(0, period);
                                    }
                                }
                            }
                            break;
                        case 0x08:
                            mode = (byte) v;
                            break;
                        default:
                            break;
                    }
                    break;
                case 0x20:
                    slot = slot_array[r & 0x1f];
                    if (slot < 0) {
                        return;
                    }
                    set_mul(slot, v);
                    break;
                case 0x40:
                    slot = slot_array[r & 0x1f];
                    if (slot < 0) {
                        return;
                    }
                    set_ksl_tl(slot, v);
                    break;
                case 0x60:
                    slot = slot_array[r & 0x1f];
                    if (slot < 0) {
                        return;
                    }
                    set_ar_dr(slot, v);
                    break;
                case 0x80:
                    slot = slot_array[r & 0x1f];
                    if (slot < 0) {
                        return;
                    }
                    set_sl_rr(slot, v);
                    break;
                case 0xa0:
                    if (r == 0xbd) {
                        lfo_am_depth = (byte) (v & 0x80);
                        lfo_pm_depth_range = (byte) ((v & 0x40) != 0 ? 8 : 0);
                        rhythm = (byte) (v & 0x3f);
                        if ((rhythm & 0x20) != 0) {
                            if ((v & 0x10) != 0) {
                                FM_KEYON(6, 0, 2);
                                FM_KEYON(6, 1, 2);
                            } else {
                                FM_KEYOFF(6, 0, ~2);
                                FM_KEYOFF(6, 1, ~2);
                            }
                            if ((v & 0x01) != 0) {
                                FM_KEYON(7, 0, 2);
                            } else {
                                FM_KEYOFF(7, 0, ~2);
                            }
                            if ((v & 0x08) != 0) {
                                FM_KEYON(7, 1, 2);
                            } else {
                                FM_KEYOFF(7, 1, ~2);
                            }
                            if ((v & 0x04) != 0) {
                                FM_KEYON(8, 0, 2);
                            } else {
                                FM_KEYOFF(8, 0, ~2);
                            }
                            if ((v & 0x02) != 0) {
                                FM_KEYON(8, 1, 2);
                            } else {
                                FM_KEYOFF(8, 1, ~2);
                            }
                        } else {
                            FM_KEYOFF(6, 0, ~2);
                            FM_KEYOFF(6, 1, ~2);
                            FM_KEYOFF(7, 0, ~2);
                            FM_KEYOFF(7, 1, ~2);
                            FM_KEYOFF(8, 0, ~2);
                            FM_KEYOFF(8, 1, ~2);
                        }
                        return;
                    }
                    if ((r & 0x0f) > 8) {
                        return;
                    }
                    if ((r & 0x10) == 0) {
                        block_fnum = (P_CH[r & 0x0f].block_fnum & 0x1f00) | v;
                    } else {
                        block_fnum = ((v & 0x1f) << 8) | (P_CH[r & 0x0f].block_fnum & 0xff);
                        if ((v & 0x20) != 0) {
                            FM_KEYON(r & 0x0f, 0, 1);
                            FM_KEYON(r & 0x0f, 1, 1);
                        } else {
                            FM_KEYOFF(r & 0x0f, 0, ~1);
                            FM_KEYOFF(r & 0x0f, 1, ~1);
                        }
                    }
                    if (P_CH[r & 0x0f].block_fnum != block_fnum) {
                        byte block = (byte) (block_fnum >> 10);
                        P_CH[r & 0x0f].block_fnum = block_fnum;
                        P_CH[r & 0x0f].ksl_base = ksl_tab[block_fnum >> 6];
                        P_CH[r & 0x0f].fc = fn_tab[block_fnum & 0x03ff] >> (7 - block);
                        P_CH[r & 0x0f].kcode = (byte) ((P_CH[r & 0x0f].block_fnum & 0x1c00) >> 9);
                        if ((mode & 0x40) != 0) {
                            P_CH[r & 0x0f].kcode |= (byte) ((P_CH[r & 0x0f].block_fnum & 0x100) >> 8);
                        } else {
                            P_CH[r & 0x0f].kcode |= (byte) ((P_CH[r & 0x0f].block_fnum & 0x200) >> 9);
                        }
                        P_CH[r & 0x0f].SLOT[0].TLL = P_CH[r & 0x0f].SLOT[0].TL + (P_CH[r & 0x0f].ksl_base >> P_CH[r & 0x0f].SLOT[0].ksl);
                        P_CH[r & 0x0f].SLOT[1].TLL = P_CH[r & 0x0f].SLOT[1].TL + (P_CH[r & 0x0f].ksl_base >> P_CH[r & 0x0f].SLOT[1].ksl);
                        CALC_FCSLOT(r & 0x0f, 0);
                        CALC_FCSLOT(r & 0x0f, 1);
                    }
                    break;
                case 0xc0:
                    if ((r & 0x0f) > 8) {
                        return;
                    }
                    P_CH[r & 0x0f].SLOT[0].FB = (byte) (((v >> 1) & 7) != 0 ? ((v >> 1) & 7) + 7 : 0);
                    P_CH[r & 0x0f].SLOT[0].CON = (byte) (v & 1);
                    P_CH[r & 0x0f].SLOT[0].iconnect = P_CH[r & 0x0f].SLOT[0].CON != 0 ? 1 : 2;
                    break;
                case 0xe0:
                    if (wavesel != 0) {
                        slot = slot_array[r & 0x1f];
                        if (slot < 0) {
                            return;
                        }
                        P_CH[slot / 2].SLOT[slot & 1].wavetable = (short) ((v & 0x03) * 0x400);
                    }
                    break;
            }
        }

        public int OPLWrite(int a, int v) {
            if ((a & 1) == 0) {
                address = (byte) (v & 0xff);
            } else {
                if (UpdateHandler != null) {
                    UpdateHandler.accept(0);
                }
                OPLWriteReg(address, v);
            }
            return status >> 7;
        }

        public byte OPLRead(int a) {
            if ((a & 1) == 0) {
                return (byte) (status & (statusmask | 0x80));
            }
            return (byte) 0xff;
        }

        public int op_calc1(int phase, int env, int pm, int wave_tab) {
            int p;
            p = (env << 4) + sin_tab[wave_tab + ((((phase & ~0xffff) + pm) >> 16) & 0x3ff)];
            if (p >= 0x1800) {
                return 0;
            }
            return tl_tab[p];
        }

        public int volume_calc(int chan, int slot) {
            int i1;
            i1 = P_CH[chan].SLOT[slot].TLL + P_CH[chan].SLOT[slot].volume + (LFO_AM & P_CH[chan].SLOT[slot].AMmask);
            return i1;
        }

        public void OPL_CALC_CH(int chan) {
            int env;
            int out1;
            phase_modulation = 0;
            env = volume_calc(chan, 0);
            out1 = P_CH[chan].SLOT[0].op1_out[0] + P_CH[chan].SLOT[0].op1_out[1];
            P_CH[chan].SLOT[0].op1_out[0] = P_CH[chan].SLOT[0].op1_out[1];
            if (P_CH[chan].SLOT[0].iconnect == 1) {
                output0 += P_CH[chan].SLOT[0].op1_out[0];
            } else if (P_CH[chan].SLOT[0].iconnect == 2) {
                phase_modulation += P_CH[chan].SLOT[0].op1_out[0];
            } else {

            }
            P_CH[chan].SLOT[0].op1_out[1] = 0;
            if (env < 0x180) {
                if (P_CH[chan].SLOT[0].FB == 0) {
                    out1 = 0;
                }
                P_CH[chan].SLOT[0].op1_out[1] = op_calc1(P_CH[chan].SLOT[0].Cnt, env, (out1 << P_CH[chan].SLOT[0].FB), P_CH[chan].SLOT[0].wavetable);
            }
            env = volume_calc(chan, 1);
            if (env < 0x180) {
                output0 += op_calc(P_CH[chan].SLOT[1].Cnt, env, phase_modulation, P_CH[chan].SLOT[1].wavetable);
            }
        }

        public void OPL_CALC_RH(int noise) {
            int out1;
            int env;
            phase_modulation = 0;
            env = volume_calc(6, 0);
            out1 = P_CH[6].SLOT[0].op1_out[0] + P_CH[6].SLOT[0].op1_out[1];
            P_CH[6].SLOT[0].op1_out[0] = P_CH[6].SLOT[0].op1_out[1];
            if (P_CH[6].SLOT[0].CON == 0) {
                phase_modulation = P_CH[6].SLOT[0].op1_out[0];
            }
            P_CH[6].SLOT[0].op1_out[1] = 0;
            if (env < 0x180) {
                if (P_CH[6].SLOT[0].FB == 0) {
                    out1 = 0;
                }
                P_CH[6].SLOT[0].op1_out[1] = op_calc1(P_CH[6].SLOT[0].Cnt, env, (out1 << P_CH[6].SLOT[0].FB), P_CH[6].SLOT[0].wavetable);
            }
            env = volume_calc(6, 1);
            if (env < 0x180) {
                output0 += op_calc(P_CH[6].SLOT[1].Cnt, env, phase_modulation, P_CH[6].SLOT[1].wavetable) * 2;
            }
            env = volume_calc(7, 0);
            if (env < 0x180) {
                byte bit7 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 7) & 1);
                byte bit3 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 3) & 1);
                byte bit2 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 2) & 1);
                byte res1 = (byte) ((bit2 ^ bit7) | bit3);
                int phase = res1 != 0 ? (0x200 | (0xd0 >> 2)) : 0xd0;
                byte bit5e = (byte) (((P_CH[8].SLOT[1].Cnt >> 16) >> 5) & 1);
                byte bit3e = (byte) (((P_CH[8].SLOT[1].Cnt >> 16) >> 3) & 1);
                byte res2 = (byte) (bit3e ^ bit5e);
                if (res2 != 0) {
                    phase = (0x200 | (0xd0 >> 2));
                }
                if ((phase & 0x200) != 0) {
                    if (noise != 0) {
                        phase = 0x200 | 0xd0;
                    }
                } else {
                    if (noise != 0) {
                        phase = 0xd0 >> 2;
                    }
                }
                output0 += op_calc(phase << 16, env, 0, P_CH[7].SLOT[0].wavetable) * 2;
            }
            env = volume_calc(7, 1);
            if (env < 0x180) {
                byte bit8 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 8) & 1);
                int phase = bit8 != 0 ? 0x200 : 0x100;
                if (noise != 0) {
                    phase ^= 0x100;
                }
                output0 += op_calc(phase << 16, env, 0, P_CH[7].SLOT[1].wavetable) * 2;
            }
            env = volume_calc(8, 0);
            if (env < 0x180) {
                output0 += op_calc(P_CH[8].SLOT[0].Cnt, env, 0, P_CH[8].SLOT[0].wavetable) * 2;
            }
            env = volume_calc(8, 1);
            if (env < 0x180) {
                byte bit7 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 7) & 1);
                byte bit3 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 3) & 1);
                byte bit2 = (byte) (((P_CH[7].SLOT[0].Cnt >> 16) >> 2) & 1);
                byte res1 = (byte) ((bit2 ^ bit7) | bit3);
                int phase = res1 != 0 ? 0x300 : 0x100;
                byte bit5e = (byte) (((P_CH[8].SLOT[1].Cnt >> 16) >> 5) & 1);
                byte bit3e = (byte) (((P_CH[8].SLOT[1].Cnt >> 16) >> 3) & 1);
                byte res2 = (byte) (bit3e ^ bit5e);
                if (res2 != 0) {
                    phase = 0x300;
                }
                output0 += op_calc(phase << 16, env, 0, P_CH[8].SLOT[1].wavetable) * 2;
            }
        }

        public void OPL_STATUS_SET(int flag) {
            status |= (byte) flag;
            if ((status & 0x80) == 0) {
                if ((status & statusmask) != 0) {
                    status |= (byte) 0x80;
                    if (IRQHandler != null) {
                        IRQHandler.accept(1);
                    }
                }
            }
        }

        public void OPL_STATUS_RESET(int flag) {
            status &= (byte) (~flag);
            if ((status & 0x80) != 0) {
                if ((status & statusmask) == 0) {
                    status &= 0x7f;
                    if (IRQHandler != null) {
                        IRQHandler.accept(0);
                    }
                }
            }
        }

        public void OPL_STATUSMASK_SET(int flag) {
            statusmask = (byte) flag;
            OPL_STATUS_SET(0);
            OPL_STATUS_RESET(0);
        }

        public void advance_lfo() {
            byte tmp;
            lfo_am_cnt += lfo_am_inc;
            if (lfo_am_cnt >= (210 << 24)) {
                lfo_am_cnt -= (210 << 24);
            }
            tmp = lfo_am_table[lfo_am_cnt >> 24];
            if (lfo_am_depth != 0) {
                LFO_AM = tmp;
            } else {
                LFO_AM = tmp >> 2;
            }
            lfo_pm_cnt += lfo_pm_inc;
            LFO_PM = ((lfo_pm_cnt >> 24) & 7) | lfo_pm_depth_range;
        }

        public void advance() {
            int i;
            eg_timer += eg_timer_add;
            while (eg_timer >= eg_timer_overflow) {
                eg_timer -= eg_timer_overflow;
                eg_cnt++;
                for (i = 0; i < 9 * 2; i++) {
                    switch (P_CH[i / 2].SLOT[i & 1].state) {
                        case 4:
                            if ((eg_cnt & ((1 << P_CH[i / 2].SLOT[i & 1].eg_sh_ar) - 1)) == 0) {
                                P_CH[i / 2].SLOT[i & 1].volume += (~P_CH[i / 2].SLOT[i & 1].volume * (eg_inc[P_CH[i / 2].SLOT[i & 1].eg_sel_ar + ((eg_cnt >> P_CH[i / 2].SLOT[i & 1].eg_sh_ar) & 7)])) >> 3;
                                if (P_CH[i / 2].SLOT[i & 1].volume <= 0) {
                                    P_CH[i / 2].SLOT[i & 1].volume = 0;
                                    P_CH[i / 2].SLOT[i & 1].state = 3;
                                }
                            }
                            break;
                        case 3:
                            if ((eg_cnt & ((1 << P_CH[i / 2].SLOT[i & 1].eg_sh_dr) - 1)) == 0) {
                                P_CH[i / 2].SLOT[i & 1].volume += eg_inc[P_CH[i / 2].SLOT[i & 1].eg_sel_dr + ((eg_cnt >> P_CH[i / 2].SLOT[i & 1].eg_sh_dr) & 7)];
                                if (P_CH[i / 2].SLOT[i & 1].volume >= P_CH[i / 2].SLOT[i & 1].sl) {
                                    P_CH[i / 2].SLOT[i & 1].state = 2;
                                }
                            }
                            break;
                        case 2:
                            if (P_CH[i / 2].SLOT[i & 1].eg_type != 0) {

                            } else {
                                if ((eg_cnt & ((1 << P_CH[i / 2].SLOT[i & 1].eg_sh_rr) - 1)) == 0) {
                                    P_CH[i / 2].SLOT[i & 1].volume += eg_inc[P_CH[i / 2].SLOT[i & 1].eg_sel_rr + ((eg_cnt >> P_CH[i / 2].SLOT[i & 1].eg_sh_rr) & 7)];
                                    if (P_CH[i / 2].SLOT[i & 1].volume >= 0x1ff) {
                                        P_CH[i / 2].SLOT[i & 1].volume = 0x1ff;
                                    }
                                }
                            }
                            break;
                        case 1:
                            if ((eg_cnt & ((1 << P_CH[i / 2].SLOT[i & 1].eg_sh_rr) - 1)) == 0) {
                                P_CH[i / 2].SLOT[i & 1].volume += eg_inc[P_CH[i / 2].SLOT[i & 1].eg_sel_rr + ((eg_cnt >> P_CH[i / 2].SLOT[i & 1].eg_sh_rr) & 7)];
                                if (P_CH[i / 2].SLOT[i & 1].volume >= 0x1ff) {
                                    P_CH[i / 2].SLOT[i & 1].volume = 0x1ff;
                                    P_CH[i / 2].SLOT[i & 1].state = 0;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            for (i = 0; i < 9 * 2; i++) {
                if (P_CH[i / 2].SLOT[i & 1].vib != 0) {
                    byte block;
                    int block_fnum = P_CH[i / 2].block_fnum;
                    int fnum_lfo = (block_fnum & 0x0380) >> 7;
                    int lfo_fn_table_index_offset = lfo_pm_table[LFO_PM + 16 * fnum_lfo];
                    if (lfo_fn_table_index_offset != 0) {
                        block_fnum += lfo_fn_table_index_offset;
                        block = (byte) ((block_fnum & 0x1c00) >> 10);
                        P_CH[i / 2].SLOT[i & 1].Cnt += (fn_tab[block_fnum & 0x03ff] >> (7 - block)) * P_CH[i / 2].SLOT[i & 1].mul;
                    } else {
                        P_CH[i / 2].SLOT[i & 1].Cnt += P_CH[i / 2].SLOT[i & 1].Incr;
                    }
                } else {
                    P_CH[i / 2].SLOT[i & 1].Cnt += P_CH[i / 2].SLOT[i & 1].Incr;
                }
            }
            noise_p += noise_f;
            i = noise_p >> 16;
            noise_p &= 0xffff;
            while (i != 0) {
                if ((noise_rng & 1) != 0) {
                    noise_rng ^= 0x80_0302;
                }
                noise_rng >>= 1;
                i--;
            }
        }

        private void CSMKeyControll(int chan) {
            FM_KEYON(chan, 0, 4);
            FM_KEYON(chan, 1, 4);
            FM_KEYOFF(chan, 0, 4);
            FM_KEYOFF(chan, 1, 4);
        }

        public int OPLTimerOver(int c) {
            if (c != 0) {
                OPL_STATUS_SET(0x20);
            } else {
                OPL_STATUS_SET(0x40);
                if ((mode & 0x80) != 0) {
                    int ch;
                    if (UpdateHandler != null) {
                        UpdateHandler.accept(0);
                    }
                    for (ch = 0; ch < 9; ch++) {
                        CSMKeyControll(ch);
                    }
                }
            }
            if (timer_handler != null) {
                timer_handler.accept(c, Attotime.attotime_mul(TimerBase, T[c]));
            }
            return status >> 7;
        }

        // OPL_TIMERHANDLER
        public void OPLSetTimerHandler(BiConsumer<Integer, Atime> _timer_handler) {
            timer_handler = _timer_handler;
        }

        // OPL_IRQHANDLER
        public void OPLSetIRQHandler(Consumer<Integer> _IRQHandler) {
            IRQHandler = _IRQHandler;
        }

        // OPL_UPDATEHANDLER
        public void OPLSetUpdateHandler(Consumer<Integer> _UpdateHandler) {
            UpdateHandler = _UpdateHandler;
        }
    }

    public static FM_OPL YM3812, YM3526;
    public static final int[] slot_array = {
            0, 2, 4, 1, 3, 5, -1, -1,
            6, 8, 10, 7, 9, 11, -1, -1,
            12, 14, 16, 13, 15, 17, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1
    };
    public static final int[] ksl_tab = {
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,

            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 8, 12, 16,
            20, 24, 28, 32,

            0, 0, 0, 0,
            0, 12, 20, 28,
            32, 40, 44, 48,
            52, 56, 60, 64,

            0, 0, 0, 20,
            32, 44, 52, 60,
            64, 72, 76, 80,
            84, 88, 92, 96,

            0, 0, 32, 52,
            64, 76, 84, 92,
            96, 104, 108, 112,
            116, 120, 124, 128,

            0, 32, 64, 84,
            96, 108, 116, 124,
            128, 136, 140, 144,
            148, 152, 156, 160,

            0, 64, 96, 116,
            128, 140, 148, 156,
            160, 168, 172, 176,
            180, 184, 188, 192,

            0, 96, 128, 148,
            160, 172, 180, 188,
            192, 200, 204, 208,
            212, 216, 220, 224
    };
    public static final int[] sl_tab = {
            0 * 16, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16,
            8 * 16, 9 * 16, 10 * 16, 11 * 16, 12 * 16, 13 * 16, 14 * 16, 31 * 16
    };
    public static final byte[] eg_inc = {
            0, 1, 0, 1, 0, 1, 0, 1,
            0, 1, 0, 1, 1, 1, 0, 1,
            0, 1, 1, 1, 0, 1, 1, 1,
            0, 1, 1, 1, 1, 1, 1, 1,

            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 2, 1, 1, 1, 2,
            1, 2, 1, 2, 1, 2, 1, 2,
            1, 2, 2, 2, 1, 2, 2, 2,

            2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 4, 2, 2, 2, 4,
            2, 4, 2, 4, 2, 4, 2, 4,
            2, 4, 4, 4, 2, 4, 4, 4,

            4, 4, 4, 4, 4, 4, 4, 4,
            8, 8, 8, 8, 8, 8, 8, 8,
            0, 0, 0, 0, 0, 0, 0, 0,
    };
    public static final byte[] eg_rate_select = {
            14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8,
            14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8, 14 * 8,

            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,
            0, 1 * 8, 2 * 8, 3 * 8,

            4 * 8, 5 * 8, 6 * 8, 7 * 8,

            8 * 8, 9 * 8, 10 * 8, 11 * 8,

            12 * 8, 12 * 8, 12 * 8, 12 * 8,

            12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8,
            12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8, 12 * 8,
    };
    public static final byte[] eg_rate_shift = {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,

            12, 12, 12, 12,
            11, 11, 11, 11,
            10, 10, 10, 10,
            9, 9, 9, 9,
            8, 8, 8, 8,
            7, 7, 7, 7,
            6, 6, 6, 6,
            5, 5, 5, 5,
            4, 4, 4, 4,
            3, 3, 3, 3,
            2, 2, 2, 2,
            1, 1, 1, 1,
            0, 0, 0, 0,

            0, 0, 0, 0,

            0, 0, 0, 0,

            0, 0, 0, 0,

            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
    };
    public static final byte[] mul_tab = {
            1, 2, 4, 6, 8, 10, 12, 14,
            16, 18, 20, 20, 24, 24, 30, 30
    };
    public static int[] tl_tab;
    public static int[] sin_tab;
    public static final byte[] lfo_am_table = {
            0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1,
            2, 2, 2, 2,
            3, 3, 3, 3,
            4, 4, 4, 4,
            5, 5, 5, 5,
            6, 6, 6, 6,
            7, 7, 7, 7,
            8, 8, 8, 8,
            9, 9, 9, 9,
            10, 10, 10, 10,
            11, 11, 11, 11,
            12, 12, 12, 12,
            13, 13, 13, 13,
            14, 14, 14, 14,
            15, 15, 15, 15,
            16, 16, 16, 16,
            17, 17, 17, 17,
            18, 18, 18, 18,
            19, 19, 19, 19,
            20, 20, 20, 20,
            21, 21, 21, 21,
            22, 22, 22, 22,
            23, 23, 23, 23,
            24, 24, 24, 24,
            25, 25, 25, 25,
            26, 26, 26,
            25, 25, 25, 25,
            24, 24, 24, 24,
            23, 23, 23, 23,
            22, 22, 22, 22,
            21, 21, 21, 21,
            20, 20, 20, 20,
            19, 19, 19, 19,
            18, 18, 18, 18,
            17, 17, 17, 17,
            16, 16, 16, 16,
            15, 15, 15, 15,
            14, 14, 14, 14,
            13, 13, 13, 13,
            12, 12, 12, 12,
            11, 11, 11, 11,
            10, 10, 10, 10,
            9, 9, 9, 9,
            8, 8, 8, 8,
            7, 7, 7, 7,
            6, 6, 6, 6,
            5, 5, 5, 5,
            4, 4, 4, 4,
            3, 3, 3, 3,
            2, 2, 2, 2,
            1, 1, 1, 1
    };
    public static final byte[] lfo_pm_table = {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,

            0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0, 0, -1, 0, 0, 0,

            1, 0, 0, 0, -1, 0, 0, 0,
            2, 1, 0, -1, -2, -1, 0, 1,

            1, 0, 0, 0, -1, 0, 0, 0,
            3, 1, 0, -1, -3, -1, 0, 1,

            2, 1, 0, -1, -2, -1, 0, 1,
            4, 2, 0, -2, -4, -2, 0, 2,

            2, 1, 0, -1, -2, -1, 0, 1,
            5, 2, 0, -2, -5, -2, 0, 2,

            3, 1, 0, -1, -3, -1, 0, 1,
            6, 3, 0, -3, -6, -3, 0, 3,

            3, 1, 0, -1, -3, -1, 0, 1,
            7, 3, 0, -3, -7, -3, 0, 3
    };
    public static int num_lock = 0;
    public static int phase_modulation;
    public static int output0;
    public static int LFO_AM;
    public static int LFO_PM;

    private static int limit(int val, int max, int min) {
        if (val > max) {
            return max;
        } else if (val < min) {
            return min;
        } else {
            return val;
        }
    }

    private static int op_calc(int phase, int env, int pm, int wave_tab) {
        int p;
        p = (env << 4) + sin_tab[wave_tab + ((((phase & ~0xffff) + (pm << 16)) >> 16) & 0x3ff)];
        if (p >= 0x1800) {
            return 0;
        }
        return tl_tab[p];
    }

    private static int init_tables() {
        int i, x;
        int n;
        double o, m;
        for (x = 0; x < 0x100; x++) {
            m = (1 << 16) / Math.pow(2, (x + 1) * (1.0 / 32) / 8.0);
            m = Math.floor(m);
            n = (int) m;
            n >>= 4;
            if ((n & 1) != 0) {
                n = (n >> 1) + 1;
            } else {
                n = n >> 1;
            }
            n <<= 1;
            tl_tab[x * 2 + 0] = n;
            tl_tab[x * 2 + 1] = -tl_tab[x * 2 + 0];
            for (i = 1; i < 12; i++) {
                tl_tab[x * 2 + 0 + i * 2 * 0x100] = tl_tab[x * 2 + 0] >> i;
                tl_tab[x * 2 + 1 + i * 2 * 0x100] = -tl_tab[x * 2 + 0 + i * 2 * 0x100];
            }
        }
        for (i = 0; i < 0x400; i++) {
            m = Math.sin(((i * 2) + 1) * Math.PI / 0x400);
            if (m > 0.0) {
                o = 8 * Math.log(1.0 / m) / Math.log(2);
            } else {
                o = 8 * Math.log(-1.0 / m) / Math.log(2);
            }
            o = o * 32;
            n = (int) (2.0 * o);
            if ((n & 1) != 0) {
                n = (n >> 1) + 1;
            } else {
                n = n >> 1;
            }
            sin_tab[i] = n * 2 + (m >= 0.0 ? 0 : 1);
        }
        for (i = 0; i < 0x400; i++) {
            if ((i & 0x200) != 0) {
                sin_tab[0x400 + i] = 0x1800;
            } else {
                sin_tab[0x400 + i] = sin_tab[i];
            }
            sin_tab[2 * 0x400 + i] = sin_tab[i & (0x3ff >> 1)];
            if ((i & 0x100) != 0) {
                sin_tab[3 * 0x400 + i] = 0x1800;
            } else {
                sin_tab[3 * 0x400 + i] = sin_tab[i & (0x3ff >> 2)];
            }
        }
        return 1;
    }

    private static int OPL_LockTable() {
        num_lock++;
        if (num_lock > 1) {
            return 0;
        }
        if (init_tables() == 0) {
            num_lock--;
            return -1;
        }
        return 0;
    }

    private static void OPL_UnLockTable() {
        if (num_lock != 0) {
            num_lock--;
        }
    }

    private static FM_OPL OPLCreate(int type, int clock, int rate) {
        int i;
        FM_OPL OPL = new FM_OPL();
        OPL_LockTable();
        OPL = new FM_OPL();
        OPL.P_CH = new OPL_CH[9];
        OPL.fn_tab = new int[1024];
        OPL.T = new int[2];
        OPL.st = new byte[2];
        for (i = 0; i < 9; i++) {
            OPL.P_CH[i].SLOT = new OPL_SLOT[2];
            OPL.P_CH[i].SLOT[0].op1_out = new int[2];
            OPL.P_CH[i].SLOT[1].op1_out = new int[2];
        }
        OPL.type = (byte) type;
        OPL.clock = clock;
        OPL.rate = rate;
        OPL.OPL_initalize();
        return OPL;
    }

    private static void OPLDestroy() {
        OPL_UnLockTable();
    }

    public static void ym3812_init(int sndindex, int clock, int rate) {
        YM3812 = OPLCreate(1, clock, rate);
        ym3812_reset_chip();
    }

    private static void ym3812_shutdown() {
        OPLDestroy();
    }

    public static void ym3812_reset_chip() {
        YM3812.OPLResetChip();
        num_lock = 0;
    }

    public static int ym3812_write(int a, int v) {
        return YM3812.OPLWrite(a, v);
    }

    public static byte ym3812_read(int a) {
        return (byte) (YM3812.OPLRead(a) | 0x06);
    }

    public static int ym3812_timer_over(int c) {
        return YM3812.OPLTimerOver(c);
    }

    // OPL_TIMERHANDLER
    public static void ym3812_set_timer_handler(BiConsumer<Integer, Atime> timer_handler) {
        YM3812.OPLSetTimerHandler(timer_handler);
    }

    // OPL_IRQHANDLER
    public static void ym3812_set_irq_handler(Consumer<Integer> IRQHandler) {
        YM3812.OPLSetIRQHandler(IRQHandler);
    }

    // OPL_UPDATEHANDLER
    public static void ym3812_set_update_handler(Consumer<Integer> UpdateHandler) {
        YM3812.OPLSetUpdateHandler(UpdateHandler);
    }

    public static void ym3812_update_one(int offset, int length) {
        byte rhythm = (byte) (YM3812.rhythm & 0x20);
        int i;
        for (i = 0; i < length; i++) {
            int lt;
            output0 = 0;
            YM3812.advance_lfo();
            YM3812.OPL_CALC_CH(0);
            YM3812.OPL_CALC_CH(1);
            YM3812.OPL_CALC_CH(2);
            YM3812.OPL_CALC_CH(3);
            YM3812.OPL_CALC_CH(4);
            YM3812.OPL_CALC_CH(5);
            if (rhythm == 0) {
                YM3812.OPL_CALC_CH(6);
                YM3812.OPL_CALC_CH(7);
                YM3812.OPL_CALC_CH(8);
            } else {
                YM3812.OPL_CALC_RH((YM3812.noise_rng >> 0) & 1);
            }
            lt = output0;
            lt = limit(lt, 32767, -32768);
            Sound.ym3812stream.streamoutput[0][offset + i] = lt;
            YM3812.advance();
        }
    }

    public static FM_OPL ym3526_init(int sndindex, int clock, int rate) {
        YM3526 = OPLCreate(0, clock, rate);
        ym3526_reset_chip();
        return YM3526;
    }

    private static void ym3526_shutdown() {
        OPLDestroy();
    }

    public static void ym3526_reset_chip() {
        YM3526.OPLResetChip();
        num_lock = 0;
    }

    public static int ym3526_write(int a, int v) {
        return YM3526.OPLWrite(a, v);
    }

    public static byte ym3526_read(int a) {
        return (byte) (YM3526.OPLRead(a) | 0x06);
    }

    // OPL_TIMERHANDLER
    public static void ym3526_set_timer_handler(BiConsumer<Integer, Atime> timer_handler) {
        YM3526.OPLSetTimerHandler(timer_handler);
    }

    // OPL_IRQHANDLER
    public static void ym3526_set_irq_handler(Consumer<Integer> IRQHandler) {
        YM3526.OPLSetIRQHandler(IRQHandler);
    }

    // OPL_UPDATEHANDLER
    public static void ym3526_set_update_handler(Consumer<Integer> UpdateHandler) {
        YM3526.OPLSetUpdateHandler(UpdateHandler);
    }

    public static int ym3526_timer_over(int c) {
        return YM3526.OPLTimerOver(c);
    }

    public static void ym3526_update_one(int offset, int length) {
        byte rhythm = (byte) (YM3526.rhythm & 0x20);
        int i;
        for (i = 0; i < length; i++) {
            int lt;
            output0 = 0;
            YM3526.advance_lfo();
            YM3526.OPL_CALC_CH(0);
            YM3526.OPL_CALC_CH(1);
            YM3526.OPL_CALC_CH(2);
            YM3526.OPL_CALC_CH(3);
            YM3526.OPL_CALC_CH(4);
            YM3526.OPL_CALC_CH(5);
            if (rhythm == 0) {
                YM3526.OPL_CALC_CH(6);
                YM3526.OPL_CALC_CH(7);
                YM3526.OPL_CALC_CH(8);
            } else {
                YM3526.OPL_CALC_RH((YM3526.noise_rng >> 0) & 1);
            }
            lt = output0;
            lt = limit(lt, 32767, -32768);
            Sound.ym3526stream.streamoutput[0][offset + i] = lt;
            YM3526.advance();
        }
    }
}
