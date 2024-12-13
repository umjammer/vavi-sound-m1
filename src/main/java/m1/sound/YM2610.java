/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Machine;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.mame.neogeo.Neogeo;
import m1.mame.taitob.Taitob;
import m1.sound.AY8910.ay8910_interface;
import m1.sound.FM.FM_OPN;


public class YM2610 {

    public byte[] REGS;
    public FM.FM_OPN OPN;
    public byte addr_A1;
    public byte[] pcmbuf;
    public int pcm_size;
    public byte adpcmTL;
    public FM.ADPCM_CH[] adpcm;
    public byte[] adpcmreg;
    public byte adpcm_arrivedEndAddress;
    public static YM2610 F2610 = new YM2610();
    public static EmuTimer[] timer;

    public void timer_callback_0() {
        ym2610_timer_over(0);
    }

    public void timer_callback_1() {
        ym2610_timer_over(1);
    }

    public static void timer_handler(int c, int count, int clock) {
        if (count == 0) {
            Timer.enable(timer[c], false);
        } else {
            Atime period = Attotime.attotime_mul(new Atime(0, Attotime.ATTOSECONDS_PER_SECOND / clock), count); // 8000000
            if (!Timer.enable(timer[c], true)) {
                Timer.adjustPeriodic(timer[c], period, Attotime.ATTOTIME_NEVER);
            }
        }
    }

    public static void ym2610_update_request() {
        Sound.ym2610stream.stream_update();
    }

    public void ADPCMA_calc_chan(int c) {
        int step;
        byte data;
        int i;
        adpcm[c].now_step += adpcm[c].step;
        if (adpcm[c].now_step >= (1 << 16)) {
            step = adpcm[c].now_step >> 16;
            adpcm[c].now_step &= (1 << 16) - 1;
            for (i = 0; i < step; i++) {
                if ((adpcm[c].now_addr & ((1 << 21) - 1)) == ((adpcm[c].end << 1) & ((1 << 21) - 1))) {
                    adpcm[c].flag = 0;
                    adpcm_arrivedEndAddress |= adpcm[c].flagMask;
                    return;
                }
                if ((adpcm[c].now_addr & 1) != 0) {
                    data = (byte) (adpcm[c].now_data & 0x0f);
                } else {
                    adpcm[c].now_data = FM.ymsndrom[adpcm[c].now_addr >> 1];
                    data = (byte) ((adpcm[c].now_data >> 4) & 0x0f);
                }
                adpcm[c].now_addr++;
                adpcm[c].adpcm_acc += FM.jedi_table[adpcm[c].adpcm_step + data];
                if ((adpcm[c].adpcm_acc & ~0x7ff) != 0) {
                    adpcm[c].adpcm_acc |= ~0xfff;
                } else {
                    adpcm[c].adpcm_acc &= 0xfff;
                }
                adpcm[c].adpcm_step += FM.step_inc[data & 7];
                adpcm[c].adpcm_step = FM.Limit(adpcm[c].adpcm_step, 48 * 16, 0);
            }
            adpcm[c].adpcm_out = ((adpcm[c].adpcm_acc * adpcm[c].vol_mul) >> adpcm[c].vol_shift) & ~3;
        }
        FM.out_adpcm[FM.ipan[c]] += adpcm[c].adpcm_out;
    }

    public static void ym2610_start(int clock) {
        F2610 = new YM2610();
        F2610.OPN = new FM_OPN();
        AY8910.ay8910_interface generic_ay8910 = new ay8910_interface();
        generic_ay8910.flags = 3;
        generic_ay8910.res_load = new int[] {
            1000, 1000, 1000
        };
        generic_ay8910.portAread = null;
        generic_ay8910.portBread = null;
        generic_ay8910.portAwrite = null;
        generic_ay8910.portBwrite = null;
        int rate = clock / 72;
        AY8910.ay8910_start_ym(17, 0, clock, generic_ay8910);
        timer = new EmuTimer[2];
        timer[0] = Timer.allocCommon(F2610::timer_callback_0, "timer_callback_0", false);
        timer[1] = Timer.allocCommon(F2610::timer_callback_1, "timer_callback_1", false);
        ym2610_init(clock, rate);
    }

    public static void ym2610_init(int clock, int rate) {
        FM.FM_init();
        F2610.REGS = new byte[512];
        F2610.adpcmreg = new byte[0x30];
        F2610.adpcm = new FM.ADPCM_CH[6];
        F2610.OPN = new FM_OPN();
        F2610.OPN.type = FM.TYPE_YM2610;
        F2610.OPN.ST.clock = clock;
        F2610.OPN.ST.rate = rate;
        F2610.OPN.ST.timer_handler = YM2610::timer_handler;
        switch (Machine.sBoard) {
            case "Taito B":
                switch (Machine.sName) {
                    case "pbobble":
                    case "silentd":
                    case "silentdj":
                    case "silentdu":
                        F2610.OPN.ST.IRQ_Handler = Taitob::irqhandler;
                        F2610.OPN.ST.SSG.set_clock = AY8910.AA8910[0]::ay8910_set_clock_ym;
                        F2610.OPN.ST.SSG.write = AY8910.AA8910[0]::ay8910_write_ym;
                        F2610.OPN.ST.SSG.read = AY8910.AA8910[0]::ay8910_read_ym;
                        F2610.OPN.ST.SSG.reset = AY8910.AA8910[0]::ay8910_reset_ym;
                        break;
                }
                break;
            case "Neo Geo":
                F2610.OPN.ST.IRQ_Handler = Neogeo::audio_cpu_irq;
                F2610.OPN.ST.SSG.set_clock = AY8910.AA8910[0]::ay8910_set_clock_ym;
                F2610.OPN.ST.SSG.write = AY8910.AA8910[0]::ay8910_write_ym;
                F2610.OPN.ST.SSG.read = AY8910.AA8910[0]::ay8910_read_ym;
                F2610.OPN.ST.SSG.reset = AY8910.AA8910[0]::ay8910_reset_ym;
                break;
        }
        F2610.pcmbuf = FM.ymsndrom;
        if (F2610.pcmbuf == null) {
            F2610.pcm_size = 0;
        } else {
            F2610.pcm_size = F2610.pcmbuf.length;
        }
        YMDeltat.DELTAT.reg = new byte[16];
        if (YMDeltat.ymsnddeltatrom == null) {
            YMDeltat.ymsnddeltatrom = FM.ymsndrom;
        }
        if (YMDeltat.ymsnddeltatrom == null) {
            YMDeltat.DELTAT.memory_size = 0;
        } else {
            YMDeltat.DELTAT.memory_size = YMDeltat.ymsnddeltatrom.length;
        }
        YMDeltat.DELTAT.status_set_handler = F2610::YM2610_deltat_status_set;
        YMDeltat.DELTAT.status_reset_handler = F2610::YM2610_deltat_status_reset;
        YMDeltat.DELTAT.status_change_EOS_bit = (byte) 0x80;
        F2610.ym2610_reset_chip();
        FM.Init_ADPCMATable();
    }

    public void ym2610_reset_chip() {
        int i;
        OPN.OPNSetPres(6 * 24, 6 * 24, 4 * 2);
        OPN.ST.SSG.reset.run();
        OPN.FM_IRQMASK_SET((byte) 0x03);
        OPN.FM_BUSY_CLEAR();
        OPN.OPNWriteMode(0x27, (byte) 0x30);
        OPN.eg_timer = 0;
        OPN.eg_cnt = 0;
        OPN.FM_STATUS_RESET((byte) 0xff);
        OPN.reset_channels(6);
        for (i = 0xb6; i >= 0xb4; i--) {
            OPN.OPNWriteReg(i, (byte) 0xc0);
            OPN.OPNWriteReg(i | 0x100, (byte) 0xc0);
        }
        for (i = 0xb2; i >= 0x30; i--) {
            OPN.OPNWriteReg(i, (byte) 0);
            OPN.OPNWriteReg(i | 0x100, (byte) 0);
        }
        for (i = 0x26; i >= 0x20; i--) {
            OPN.OPNWriteReg(i, (byte) 0);
        }
        for (i = 0; i < 6; i++) {
            adpcm[i].step = (int) ((float) (1 << 16) * ((float) F2610.OPN.ST.freqbase) / 3.0);
            adpcm[i].now_addr = 0;
            adpcm[i].now_step = 0;
            adpcm[i].start = 0;
            adpcm[i].end = 0;
            adpcm[i].vol_mul = 0;
            FM.ipan[i] = 3;
            adpcm[i].flagMask = (byte) (1 << i);
            adpcm[i].flag = 0;
            adpcm[i].adpcm_acc = 0;
            adpcm[i].adpcm_step = 0;
            adpcm[i].adpcm_out = 0;
        }
        adpcmTL = 0x3f;
        adpcm_arrivedEndAddress = 0;
        YMDeltat.DELTAT.freqbase = F2610.OPN.ST.freqbase;
        YMDeltat.DELTAT.output_pointer = 0;
        YMDeltat.DELTAT.portshift = 8;
        YMDeltat.DELTAT.output_range = 1 << 23;
        YMDeltat.YM_DELTAT_ADPCM_Reset(3, 1);
    }

    public void ym2610_write(int a, byte v) {
        int addr;
        int ch;
        v &= (byte) 0xff;
        switch (a & 3) {
            case 0:
                OPN.ST.address = v;
                addr_A1 = 0;
                if (v < 16) {
                    OPN.ST.SSG.write.accept(0, v);
                }
                break;
            case 1:
                if (addr_A1 != 0)
                    break;
                addr = OPN.ST.address;
                REGS[addr] = v;
                switch (addr & 0xf0) {
                    case 0x00:
                        OPN.ST.SSG.write.accept(a, v);
                        break;
                    case 0x10:
                        ym2610_update_request();
                        switch (addr) {
                            case 0x10:
                            case 0x11:
                            case 0x12:
                            case 0x13:
                            case 0x14:
                            case 0x15:
                            case 0x19:
                            case 0x1a:
                            case 0x1b:
                                YMDeltat.YM_DELTAT_ADPCM_Write(addr - 0x10, v);
                                break;
                            case 0x1c: {
                                byte statusmask = (byte) ~v;
                                for (ch = 0; ch < 6; ch++) {
                                    adpcm[ch].flagMask = (byte) (statusmask & (1 << ch));
                                }
                                YMDeltat.DELTAT.status_change_EOS_bit = (byte) (statusmask & 0x80);
                                adpcm_arrivedEndAddress &= statusmask;
                            }
                            break;
                            default:
                                break;
                        }
                        break;
                    case 0x20:
                        ym2610_update_request();
                        OPN.OPNWriteMode(addr, v);
                        break;
                    default:
                        ym2610_update_request();
                        OPN.OPNWriteReg(addr, v);
                        break;
                }
                break;
            case 2:
                OPN.ST.address = v;
                addr_A1 = 1;
                break;
            case 3:
                if (addr_A1 != 1)
                    break;
                ym2610_update_request();
                addr = OPN.ST.address;
                REGS[addr | 0x100] = v;
                if (addr < 0x30) {
                    FM_ADPCMAWrite(addr, v);
                } else {
                    OPN.OPNWriteReg(addr | 0x100, v);
                }
                break;
        }
    }

    public byte ym2610_read(int a) {
        int addr = OPN.ST.address;
        byte ret = 0;
        switch (a & 3) {
            case 0:
                ret = (byte) (OPN.FM_STATUS_FLAG() & 0x83);
                break;
            case 1:
                if (addr < 16) {
                    ret = OPN.ST.SSG.read.get();
                }
                if (addr == 0xff) {
                    ret = 0x01;
                }
                break;
            case 2:
                ret = adpcm_arrivedEndAddress;
                break;
            case 3:
                ret = 0;
                break;
        }
        return ret;
    }

    public int ym2610_timer_over(int c) {
        if (c != 0) {
            OPN.TimerBOver();
        } else {
            ym2610_update_request();
            OPN.TimerAOver();
            if ((OPN.ST.mode & 0x80) != 0) {
                OPN.CSMKeyControll();
            }
        }
        return OPN.ST.irq;
    }

    private void FM_ADPCMAWrite(int r, byte v) {
        byte c = (byte) (r & 0x07);
        adpcmreg[r] = (byte) (v & 0xff);
        switch (r) {
            case 0x00:
                if ((v & 0x80) == 0) {
                    for (c = 0; c < 6; c++) {
                        if (((v >> c) & 1) != 0) {
                            adpcm[c].step = (int) ((float) (1 << 16) * ((float) F2610.OPN.ST.freqbase) / 3.0);
                            adpcm[c].now_addr = adpcm[c].start << 1;
                            adpcm[c].now_step = 0;
                            adpcm[c].adpcm_acc = 0;
                            adpcm[c].adpcm_step = 0;
                            adpcm[c].adpcm_out = 0;
                            adpcm[c].flag = 1;
                            if (pcmbuf == null) {
                                adpcm[c].flag = 0;
                            } else {
                                if (adpcm[c].end >= pcm_size) {

                                }
                                if (adpcm[c].start >= pcm_size) {
                                    adpcm[c].flag = 0;
                                }
                            }
                        }
                    }
                } else {
                    for (c = 0; c < 6; c++) {
                        if (((v >> c) & 1) != 0) {
                            adpcm[c].flag = 0;
                        }
                    }
                }
                break;
            case 0x01:
                adpcmTL = (byte) ((v & 0x3f) ^ 0x3f);
                for (c = 0; c < 6; c++) {
                    int volume = F2610.adpcmTL + adpcm[c].IL;
                    if (volume >= 63) {
                        adpcm[c].vol_mul = 0;
                        adpcm[c].vol_shift = 0;
                    } else {
                        adpcm[c].vol_mul = (byte) (15 - (volume & 7));
                        adpcm[c].vol_shift = (byte) (1 + (volume >> 3));
                    }
                    adpcm[c].adpcm_out = ((adpcm[c].adpcm_acc * adpcm[c].vol_mul) >> adpcm[c].vol_shift) & ~3;
                }
                break;
            default:
                c = (byte) (r & 0x07);
                if (c >= 0x06)
                    return;
                switch (r & 0x38) {
                    case 0x08: {
                        int volume;
                        adpcm[c].IL = (byte) ((v & 0x1f) ^ 0x1f);
                        volume = adpcmTL + adpcm[c].IL;
                        if (volume >= 63)    /* This is correct, 63 = quiet */ {
                            adpcm[c].vol_mul = 0;
                            adpcm[c].vol_shift = 0;
                        } else {
                            adpcm[c].vol_mul = (byte) (15 - (volume & 7));
                            adpcm[c].vol_shift = (byte) (1 + (volume >> 3));
                        }
                        FM.ipan[c] = (v >> 6) & 0x03;
                        adpcm[c].adpcm_out = ((adpcm[c].adpcm_acc * adpcm[c].vol_mul) >> adpcm[c].vol_shift) & ~3;
                    }
                    break;
                    case 0x10:
                    case 0x18:
                        adpcm[c].start = (F2610.adpcmreg[0x18 + c] * 0x0100 | adpcmreg[0x10 + c]) << 8;
                        break;
                    case 0x20:
                    case 0x28:
                        adpcm[c].end = (F2610.adpcmreg[0x28 + c] * 0x0100 | adpcmreg[0x20 + c]) << 8;
                        adpcm[c].end += (1 << 8) - 1;
                        break;
                }
                break;
        }
    }

    public void ym2610_update_one(int offset, int length) {
        int i, j;
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 1);
        if ((OPN.ST.mode & 0xc0) != 0) {
            if (OPN.CH[2].SLOT[0].Incr == -1) {
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 0, OPN.SL3.fc[1], F2610.OPN.SL3.kcode[1]);
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 2, OPN.SL3.fc[2], F2610.OPN.SL3.kcode[2]);
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 1, OPN.SL3.fc[0], F2610.OPN.SL3.kcode[0]);
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 3, OPN.CH[2].fc, F2610.OPN.CH[2].kcode);
            }
        } else {
            OPN.refresh_fc_eg_chan(F2610.OPN.type, 2);
        }
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 4);
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 5);
        for (i = 0; i < length; i++) {
            OPN.advance_lfo();
            FM.out_adpcm[2] = FM.out_adpcm[1] = FM.out_adpcm[3] = 0;
            FM.out_delta[2] = FM.out_delta[1] = FM.out_delta[3] = 0;
            FM.out_fm[1] = 0;
            FM.out_fm[2] = 0;
            FM.out_fm[4] = 0;
            FM.out_fm[5] = 0;
            OPN.eg_timer += OPN.eg_timer_add;
            while (OPN.eg_timer >= OPN.eg_timer_overflow) {
                OPN.eg_timer -= OPN.eg_timer_overflow;
                OPN.eg_cnt++;
                OPN.advance_eg_channel(1);
                OPN.advance_eg_channel(2);
                OPN.advance_eg_channel(4);
                OPN.advance_eg_channel(5);
            }
            OPN.chan_calc(1, 1);
            OPN.chan_calc(2, 2);
            OPN.chan_calc(4, 4);
            OPN.chan_calc(5, 5);
            if ((YMDeltat.DELTAT.portstate & 0x80) != 0) {
                YMDeltat.YM_DELTAT_ADPCM_CALC();
            }
            for (j = 0; j < 6; j++) {
                if (adpcm[j].flag != 0) {
                    ADPCMA_calc_chan(j);
                }
            }
            int lt, rt;
            lt = FM.out_adpcm[2] + FM.out_adpcm[3];
            rt = FM.out_adpcm[1] + FM.out_adpcm[3];
            lt += (FM.out_delta[2] + FM.out_delta[3]) >> 9;
            rt += (FM.out_delta[1] + FM.out_delta[3]) >> 9;
            lt += (FM.out_fm[1] >> 1) & OPN.pan[2];
            rt += (FM.out_fm[1] >> 1) & OPN.pan[3];
            lt += (FM.out_fm[2] >> 1) & OPN.pan[4];
            rt += (FM.out_fm[2] >> 1) & OPN.pan[5];
            lt += (FM.out_fm[4] >> 1) & OPN.pan[8];
            rt += (FM.out_fm[4] >> 1) & OPN.pan[9];
            lt += (FM.out_fm[5] >> 1) & OPN.pan[10];
            rt += (FM.out_fm[5] >> 1) & OPN.pan[11];
            lt = FM.Limit(lt, 32767, -32768);
            rt = FM.Limit(rt, 32767, -32768);
            Sound.ym2610stream.streamoutput[0][offset + i] = lt;
            Sound.ym2610stream.streamoutput[1][offset + i] = rt;
        }
    }

    public void ym2610b_update_one(int offset, int length) {
        int i, j;
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 0);
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 1);
        if ((OPN.ST.mode & 0xc0) != 0) {
            if (OPN.CH[2].SLOT[0].Incr == -1) {
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 0, OPN.SL3.fc[1], OPN.SL3.kcode[1]);
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 2, OPN.SL3.fc[2], OPN.SL3.kcode[2]);
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 1, OPN.SL3.fc[0], OPN.SL3.kcode[0]);
                OPN.refresh_fc_eg_slot(F2610.OPN.type, 2, 3, OPN.CH[2].fc, OPN.CH[2].kcode);
            }
        } else {
            OPN.refresh_fc_eg_chan(F2610.OPN.type, 2);
        }
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 3);
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 4);
        OPN.refresh_fc_eg_chan(F2610.OPN.type, 5);
        for (i = 0; i < length; i++) {
            OPN.advance_lfo();
            FM.out_adpcm[2] = FM.out_adpcm[1] = FM.out_adpcm[3] = 0;
            FM.out_delta[2] = FM.out_delta[1] = FM.out_delta[3] = 0;
            FM.out_fm[0] = 0;
            FM.out_fm[1] = 0;
            FM.out_fm[2] = 0;
            FM.out_fm[3] = 0;
            FM.out_fm[4] = 0;
            FM.out_fm[5] = 0;
            OPN.eg_timer += OPN.eg_timer_add;
            while (OPN.eg_timer >= OPN.eg_timer_overflow) {
                OPN.eg_timer -= OPN.eg_timer_overflow;
                OPN.eg_cnt++;
                OPN.advance_eg_channel(0);
                OPN.advance_eg_channel(1);
                OPN.advance_eg_channel(2);
                OPN.advance_eg_channel(3);
                OPN.advance_eg_channel(4);
                OPN.advance_eg_channel(5);
            }
            OPN.chan_calc(0, 0);
            OPN.chan_calc(1, 1);
            OPN.chan_calc(2, 2);
            OPN.chan_calc(3, 3);
            OPN.chan_calc(4, 4);
            OPN.chan_calc(5, 5);
            if ((YMDeltat.DELTAT.portstate & 0x80) != 0) {
                YMDeltat.YM_DELTAT_ADPCM_CALC();
            }
            for (j = 0; j < 6; j++) {
                if (adpcm[j].flag != 0) {
                    ADPCMA_calc_chan(j);
                }
            }
            int lt, rt;
            lt = FM.out_adpcm[2] + FM.out_adpcm[3];
            rt = FM.out_adpcm[1] + FM.out_adpcm[3];
            lt += (FM.out_delta[2] + FM.out_delta[3]) >> 9;
            rt += (FM.out_delta[1] + FM.out_delta[3]) >> 9;
            lt += (FM.out_fm[0] >> 1) & OPN.pan[0];
            rt += (FM.out_fm[0] >> 1) & OPN.pan[1];
            lt += (FM.out_fm[1] >> 1) & OPN.pan[2];
            rt += (FM.out_fm[1] >> 1) & OPN.pan[3];
            lt += (FM.out_fm[2] >> 1) & OPN.pan[4];
            rt += (FM.out_fm[2] >> 1) & OPN.pan[5];
            lt += (FM.out_fm[3] >> 1) & OPN.pan[6];
            rt += (FM.out_fm[3] >> 1) & OPN.pan[7];
            lt += (FM.out_fm[4] >> 1) & OPN.pan[8];
            rt += (FM.out_fm[4] >> 1) & OPN.pan[9];
            lt += (FM.out_fm[5] >> 1) & OPN.pan[10];
            rt += (FM.out_fm[5] >> 1) & OPN.pan[11];
            lt = FM.Limit(lt, 32767, -32768);
            rt = FM.Limit(rt, 32767, -32768);
            Sound.ym2610stream.streamoutput[0][offset + i] = lt;
            Sound.ym2610stream.streamoutput[1][offset + i] = rt;
        }
    }

    public void ym2610_postload() {
        int r;
        for (r = 0; r < 16; r++) {
            OPN.ST.SSG.write.accept(0, (byte) r);
            OPN.ST.SSG.write.accept(1, REGS[r]);
        }
        for (r = 0x30; r < 0x9e; r++) {
            if ((r & 3) != 3) {
                OPN.OPNWriteReg(r, F2610.REGS[r]);
                OPN.OPNWriteReg(r | 0x100, F2610.REGS[r | 0x100]);
            }
        }
        for (r = 0xb0; r < 0xb6; r++) {
            if ((r & 3) != 3) {
                OPN.OPNWriteReg(r, F2610.REGS[r]);
                OPN.OPNWriteReg(r | 0x100, F2610.REGS[r | 0x100]);
            }
        }
        FM_ADPCMAWrite(1, F2610.REGS[0x101]);
        for (r = 0; r < 6; r++) {
            FM_ADPCMAWrite(r + 0x08, REGS[r + 0x108]);
            FM_ADPCMAWrite(r + 0x10, REGS[r + 0x110]);
            FM_ADPCMAWrite(r + 0x18, REGS[r + 0x118]);
            FM_ADPCMAWrite(r + 0x20, REGS[r + 0x120]);
            FM_ADPCMAWrite(r + 0x28, REGS[r + 0x128]);
        }
        YMDeltat.YM_DELTAT_postload(REGS, 0x010);
    }

    private void YM2610_deltat_status_set(byte changebits) {
        adpcm_arrivedEndAddress |= changebits;
    }

    private void YM2610_deltat_status_reset(byte changebits) {
        adpcm_arrivedEndAddress &= (byte) (~changebits);
    }

    public void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        writer.write(REGS, 0, 512);
        writer.write(addr_A1);
        writer.write(adpcmTL);
        writer.write(adpcmreg, 0, 0x30);
        writer.write(adpcm_arrivedEndAddress);
        writer.write(OPN.ST.freqbase);
        writer.write(OPN.ST.timer_prescaler);
        writer.write(OPN.ST.busy_expiry_time.seconds);
        writer.write(OPN.ST.busy_expiry_time.attoseconds);
        writer.write(OPN.ST.address);
        writer.write(OPN.ST.irq);
        writer.write(OPN.ST.irqmask);
        writer.write(OPN.ST.status);
        writer.write(OPN.ST.mode);
        writer.write(OPN.ST.prescaler_sel);
        writer.write(OPN.ST.fn_h);
        writer.write(OPN.ST.TA);
        writer.write(OPN.ST.TAC);
        writer.write(OPN.ST.TB);
        writer.write(OPN.ST.TBC);
        for (i = 0; i < 12; i++) {
            writer.write(OPN.pan[i]);
        }
        writer.write(OPN.eg_cnt);
        writer.write(OPN.eg_timer);
        writer.write(OPN.eg_timer_add);
        writer.write(OPN.eg_timer_overflow);
        writer.write(OPN.lfo_cnt);
        writer.write(OPN.lfo_inc);
        for (i = 0; i < 8; i++) {
            writer.write(OPN.lfo_freq[i]);
        }
        for (i = 0; i < 6; i++) {
            for (j = 0; j < 4; j++) {
                writer.write(OPN.CH[i].SLOT[j].KSR);
                writer.write(OPN.CH[i].SLOT[j].ar);
                writer.write(OPN.CH[i].SLOT[j].d1r);
                writer.write(OPN.CH[i].SLOT[j].d2r);
                writer.write(OPN.CH[i].SLOT[j].rr);
                writer.write(OPN.CH[i].SLOT[j].ksr);
                writer.write(OPN.CH[i].SLOT[j].mul);
                writer.write(OPN.CH[i].SLOT[j].phase);
                writer.write(OPN.CH[i].SLOT[j].Incr);
                writer.write(OPN.CH[i].SLOT[j].state);
                writer.write(OPN.CH[i].SLOT[j].tl);
                writer.write(OPN.CH[i].SLOT[j].volume);
                writer.write(OPN.CH[i].SLOT[j].sl);
                writer.write(OPN.CH[i].SLOT[j].vol_out);
                writer.write(OPN.CH[i].SLOT[j].eg_sh_ar);
                writer.write(OPN.CH[i].SLOT[j].eg_sel_ar);
                writer.write(OPN.CH[i].SLOT[j].eg_sh_d1r);
                writer.write(OPN.CH[i].SLOT[j].eg_sel_d1r);
                writer.write(OPN.CH[i].SLOT[j].eg_sh_d2r);
                writer.write(OPN.CH[i].SLOT[j].eg_sel_d2r);
                writer.write(OPN.CH[i].SLOT[j].eg_sh_rr);
                writer.write(OPN.CH[i].SLOT[j].eg_sel_rr);
                writer.write(OPN.CH[i].SLOT[j].ssg);
                writer.write(OPN.CH[i].SLOT[j].ssgn);
                writer.write(OPN.CH[i].SLOT[j].key);
                writer.write(OPN.CH[i].SLOT[j].AMmask);
            }
        }
        for (i = 0; i < 6; i++) {
            writer.write(adpcm[i].flag);
            writer.write(adpcm[i].flagMask);
            writer.write(adpcm[i].now_data);
            writer.write(adpcm[i].now_addr);
            writer.write(adpcm[i].now_step);
            writer.write(adpcm[i].step);
            writer.write(adpcm[i].start);
            writer.write(adpcm[i].end);
            writer.write(adpcm[i].IL);
            writer.write(adpcm[i].adpcm_acc);
            writer.write(adpcm[i].adpcm_step);
            writer.write(adpcm[i].adpcm_out);
            writer.write(adpcm[i].vol_mul);
            writer.write(adpcm[i].vol_shift);
        }
        for (i = 0; i < 6; i++) {
            writer.write(OPN.CH[i].ALGO);
            writer.write(OPN.CH[i].FB);
            writer.write(OPN.CH[i].op1_out0);
            writer.write(OPN.CH[i].op1_out1);
            writer.write(OPN.CH[i].mem_value);
            writer.write(OPN.CH[i].pms);
            writer.write(OPN.CH[i].ams);
            writer.write(OPN.CH[i].fc);
            writer.write(OPN.CH[i].kcode);
            writer.write(OPN.CH[i].block_fnum);
        }
        for (i = 0; i < 3; i++) {
            writer.write(OPN.SL3.fc[i]);
        }
        writer.write(OPN.SL3.fn_h);
        writer.write(OPN.SL3.kcode, 0, 3);
        for (i = 0; i < 3; i++) {
            writer.write(OPN.SL3.block_fnum[i]);
        }
        writer.write(YMDeltat.DELTAT.portstate);
        writer.write(YMDeltat.DELTAT.now_addr);
        writer.write(YMDeltat.DELTAT.now_step);
        writer.write(YMDeltat.DELTAT.acc);
        writer.write(YMDeltat.DELTAT.prev_acc);
        writer.write(YMDeltat.DELTAT.adpcmd);
        writer.write(YMDeltat.DELTAT.adpcml);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        int i, j;
        REGS = reader.readBytes(512);
        addr_A1 = reader.readByte();
        adpcmTL = reader.readByte();
        adpcmreg = reader.readBytes(0x30);
        adpcm_arrivedEndAddress = reader.readByte();
        OPN.ST.freqbase = reader.readDouble();
        OPN.ST.timer_prescaler = reader.readInt32();
        OPN.ST.busy_expiry_time.seconds = reader.readInt32();
        OPN.ST.busy_expiry_time.attoseconds = reader.readInt64();
        OPN.ST.address = reader.readByte();
        OPN.ST.irq = reader.readByte();
        OPN.ST.irqmask = reader.readByte();
        OPN.ST.status = reader.readByte();
        OPN.ST.mode = reader.readByte();
        OPN.ST.prescaler_sel = reader.readByte();
        OPN.ST.fn_h = reader.readByte();
        OPN.ST.TA = reader.readInt32();
        OPN.ST.TAC = reader.readInt32();
        OPN.ST.TB = reader.readByte();
        OPN.ST.TBC = reader.readInt32();
        for (i = 0; i < 12; i++) {
            OPN.pan[i] = reader.readUInt32();
        }
        OPN.eg_cnt = reader.readUInt32();
        OPN.eg_timer = reader.readUInt32();
        OPN.eg_timer_add = reader.readUInt32();
        OPN.eg_timer_overflow = reader.readUInt32();
        OPN.lfo_cnt = reader.readInt32();
        OPN.lfo_inc = reader.readInt32();
        for (i = 0; i < 8; i++) {
            OPN.lfo_freq[i] = reader.readInt32();
        }
        for (i = 0; i < 6; i++) {
            for (j = 0; j < 4; j++) {
                OPN.CH[i].SLOT[j].KSR = reader.readByte();
                OPN.CH[i].SLOT[j].ar = reader.readInt32();
                OPN.CH[i].SLOT[j].d1r = reader.readInt32();
                OPN.CH[i].SLOT[j].d2r = reader.readInt32();
                OPN.CH[i].SLOT[j].rr = reader.readInt32();
                OPN.CH[i].SLOT[j].ksr = reader.readByte();
                OPN.CH[i].SLOT[j].mul = reader.readInt32();
                OPN.CH[i].SLOT[j].phase = reader.readUInt32();
                OPN.CH[i].SLOT[j].Incr = reader.readInt32();
                OPN.CH[i].SLOT[j].state = reader.readByte();
                OPN.CH[i].SLOT[j].tl = reader.readInt32();
                OPN.CH[i].SLOT[j].volume = reader.readInt32();
                OPN.CH[i].SLOT[j].sl = reader.readInt32();
                OPN.CH[i].SLOT[j].vol_out = reader.readUInt32();
                OPN.CH[i].SLOT[j].eg_sh_ar = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sel_ar = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sh_d1r = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sel_d1r = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sh_d2r = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sel_d2r = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sh_rr = reader.readByte();
                OPN.CH[i].SLOT[j].eg_sel_rr = reader.readByte();
                OPN.CH[i].SLOT[j].ssg = reader.readByte();
                OPN.CH[i].SLOT[j].ssgn = reader.readByte();
                OPN.CH[i].SLOT[j].key = reader.readUInt32();
                OPN.CH[i].SLOT[j].AMmask = reader.readUInt32();
            }
        }
        for (i = 0; i < 6; i++) {
            adpcm[i].flag = reader.readByte();
            adpcm[i].flagMask = reader.readByte();
            adpcm[i].now_data = reader.readByte();
            adpcm[i].now_addr = reader.readUInt32();
            adpcm[i].now_step = reader.readUInt32();
            adpcm[i].step = reader.readUInt32();
            adpcm[i].start = reader.readUInt32();
            adpcm[i].end = reader.readUInt32();
            adpcm[i].IL = reader.readByte();
            adpcm[i].adpcm_acc = reader.readInt32();
            adpcm[i].adpcm_step = reader.readInt32();
            adpcm[i].adpcm_out = reader.readInt32();
            adpcm[i].vol_mul = reader.readSByte();
            adpcm[i].vol_shift = reader.readByte();
        }
        for (i = 0; i < 6; i++) {
            OPN.CH[i].ALGO = reader.readByte();
            OPN.CH[i].FB = reader.readByte();
            OPN.CH[i].op1_out0 = reader.readInt32();
            OPN.CH[i].op1_out1 = reader.readInt32();
            OPN.CH[i].mem_value = reader.readInt32();
            OPN.CH[i].pms = reader.readInt32();
            OPN.CH[i].ams = reader.readByte();
            OPN.CH[i].fc = reader.readUInt32();
            OPN.CH[i].kcode = reader.readByte();
            OPN.CH[i].block_fnum = reader.readUInt32();
        }
        for (i = 0; i < 3; i++) {
            OPN.SL3.fc[i] = reader.readUInt32();
        }
        OPN.SL3.fn_h = reader.readByte();
        OPN.SL3.kcode = reader.readBytes(3);
        for (i = 0; i < 3; i++) {
            OPN.SL3.block_fnum[i] = reader.readUInt32();
        }
        YMDeltat.DELTAT.portstate = reader.readByte();
        YMDeltat.DELTAT.now_addr = reader.readInt32();
        YMDeltat.DELTAT.now_step = reader.readInt32();
        YMDeltat.DELTAT.acc = reader.readInt32();
        YMDeltat.DELTAT.prev_acc = reader.readInt32();
        YMDeltat.DELTAT.adpcmd = reader.readInt32();
        YMDeltat.DELTAT.adpcml = reader.readInt32();
    }
}
