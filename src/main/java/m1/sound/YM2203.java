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
import m1.mame.dataeast.Dataeast;
import m1.mame.taito.Taito;
import m1.sound.AY8910.ay8910_interface;
import m1.sound.FM.FM_OPN;


public class YM2203 {

    public byte[] REGS;
    public FM.FM_OPN OPN;
    public Streams stream;
    public static final YM2203[] FF2203 = new YM2203[2];
    public EmuTimer[] timer;

    public static void timer_callback_2203_0_0() {
        FF2203[0].ym2203_timer_over(0);
    }

    public static void timer_callback_2203_0_1() {
        FF2203[0].ym2203_timer_over(1);
    }

    public static void timer_callback_2203_1_0() {
        FF2203[1].ym2203_timer_over(0);
    }

    public static void timer_callback_2203_1_1() {
        FF2203[1].ym2203_timer_over(1);
    }

    public void timer_handler(int c, int count, int clock) {
        if (count == 0) {
            Timer.enable(timer[c], false);
        } else {
            Atime period = Attotime.attotime_mul(new Atime(0, Attotime.ATTOSECONDS_PER_SECOND / clock), count);
            if (!Timer.enable(timer[c], true)) {
                Timer.adjustPeriodic(timer[c], period, Attotime.ATTOTIME_NEVER);
            }
        }
    }

    public static void ym2203_init(int sndindex, int clock, int rate) {
        FM.FM_init();
        FF2203[sndindex].OPN = new FM_OPN();
        FF2203[sndindex].REGS = new byte[256];
        FF2203[sndindex].OPN.type = FM.TYPE_YM2203;
        FF2203[sndindex].OPN.ST.clock = clock;
        FF2203[sndindex].OPN.ST.rate = rate;
        FF2203[sndindex].OPN.ST.timer_handler = FF2203[sndindex]::timer_handler;
        switch (Machine.sBoard) {
            case "Data East":
                switch (Machine.sName) {
                    case "pcktgal":
                    case "pcktgalb":
                    case "pcktgal2":
                    case "pcktgal2j":
                    case "spool3":
                    case "spool3i":
                        FF2203[sndindex].OPN.ST.IRQ_Handler = Dataeast::irqhandler;
                        FF2203[sndindex].OPN.ST.SSG.set_clock = AY8910.AA8910[sndindex]::ay8910_set_clock_ym;
                        FF2203[sndindex].OPN.ST.SSG.write = AY8910.AA8910[sndindex]::ay8910_write_ym;
                        FF2203[sndindex].OPN.ST.SSG.read = AY8910.AA8910[sndindex]::ay8910_read_ym;
                        FF2203[sndindex].OPN.ST.SSG.reset = AY8910.AA8910[sndindex]::ay8910_reset_ym;
                        break;
                }
                break;
            case "Taito":
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
                        FF2203[sndindex].OPN.ST.IRQ_Handler = Taito::irqhandler;
                        FF2203[sndindex].OPN.ST.SSG.set_clock = AY8910.AA8910[sndindex]::ay8910_set_clock_ym;
                        FF2203[sndindex].OPN.ST.SSG.write = AY8910.AA8910[sndindex]::ay8910_write_ym;
                        FF2203[sndindex].OPN.ST.SSG.read = AY8910.AA8910[sndindex]::ay8910_read_ym;
                        FF2203[sndindex].OPN.ST.SSG.reset = AY8910.AA8910[sndindex]::ay8910_reset_ym;
                        break;
                }
                break;
            case "Capcom":
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
                        FF2203[sndindex].OPN.ST.IRQ_Handler = null;
                        FF2203[sndindex].OPN.ST.SSG.set_clock = AY8910.AA8910[sndindex]::ay8910_set_clock_ym;
                        FF2203[sndindex].OPN.ST.SSG.write = AY8910.AA8910[sndindex]::ay8910_write_ym;
                        FF2203[sndindex].OPN.ST.SSG.read = AY8910.AA8910[sndindex]::ay8910_read_ym;
                        FF2203[sndindex].OPN.ST.SSG.reset = AY8910.AA8910[sndindex]::ay8910_reset_ym;
                        break;
                }
                break;
        }
        YM2203.FF2203[sndindex].ym2203_reset_chip();
    }

    public static void ym2203_start(int sndindex, int clock) {
        FF2203[sndindex] = new YM2203();
        AY8910.ay8910_interface generic_2203 = new ay8910_interface();
        generic_2203.flags = 1;
        generic_2203.res_load = new int[] {
                1000, 1000, 1000
        };
        generic_2203.portAread = null;
        generic_2203.portBread = null;
        generic_2203.portAwrite = null;
        generic_2203.portBwrite = null;
        FMOpl.tl_tab = new int[0x1800];
        FMOpl.sin_tab = new int[0x1000];
        int rate = clock / 72;
        AY8910.ay8910_start_ym(14, sndindex, clock, generic_2203);
        FF2203[sndindex].timer = new EmuTimer[2];
        if (sndindex == 0) {
            FF2203[sndindex].timer[0] = Timer.allocCommon(YM2203::timer_callback_2203_0_0, "timer_callback_2203_0_0", false);
            FF2203[sndindex].timer[1] = Timer.allocCommon(YM2203::timer_callback_2203_0_1, "timer_callback_2203_0_1", false);
        } else if (sndindex == 1) {
            FF2203[sndindex].timer[0] = Timer.allocCommon(YM2203::timer_callback_2203_1_0, "timer_callback_2203_1_0", false);
            FF2203[sndindex].timer[1] = Timer.allocCommon(YM2203::timer_callback_2203_1_1, "timer_callback_2203_1_1", false);
        }
        FF2203[sndindex].stream = new Streams(rate, 0, 1, FF2203[sndindex]::ym2203_update_one);
        ym2203_init(sndindex, clock, rate);
    }

    public static byte ym2203_status_port_0_r() {
        return FF2203[0].ym2203_read(0, 0);
    }

    public static byte ym2203_read_port_0_r() {
        return FF2203[0].ym2203_read(0, 1);
    }

    public static void ym2203_control_port_0_w(byte data) {
        FF2203[0].ym2203_write(0, data);
    }

    public static void ym2203_control_port_1_w(byte data) {
        FF2203[1].ym2203_write(0, data);
    }

    public static void ym2203_write_port_0_w(byte data) {
        FF2203[0].ym2203_write(1, data);
    }

    public static void ym2203_write_port_1_w(byte data) {
        FF2203[1].ym2203_write(1, data);
    }

    public void ym2203_write(int a, byte v) {
        if ((a & 1) == 0) {
            OPN.ST.address = (v &= (byte) 0xff);
            if (v < 16) {
                OPN.ST.SSG.write.accept(0, v);
            }
            if (v >= 0x2d && v <= 0x2f) {
                OPN.OPNPrescaler_w(v, 1);
            }
        } else {
            int addr = OPN.ST.address;
            REGS[addr] = v;
            switch (addr & 0xf0) {
                case 0x00:
                    OPN.ST.SSG.write.accept(a, v);
                    break;
                case 0x20:
                    stream.stream_update();
                    OPN.OPNWriteMode(addr, v);
                    break;
                default:
                    stream.stream_update();
                    OPN.OPNWriteReg(addr, v);
                    break;
            }
            OPN.FM_BUSY_SET(1);
        }
    }

    public byte ym2203_read(int chip, int a) {
        int addr = OPN.ST.address;
        byte ret = 0;
        if ((a & 1) == 0) {
            ret = OPN.FM_STATUS_FLAG();
        } else {
            if (addr < 16) {
                ret = OPN.ST.SSG.read.get();
            }
        }
        return ret;
    }

    public void ym2203_update_one(int offset, int length) {
        int i;
        OPN.refresh_fc_eg_chan(OPN.type, 0);
        OPN.refresh_fc_eg_chan(OPN.type, 1);
        if ((OPN.ST.mode & 0xc0) != 0) {
            if (OPN.CH[2].SLOT[0].Incr == -1) {
                OPN.refresh_fc_eg_slot(OPN.type, 2, 0, OPN.SL3.fc[1], OPN.SL3.kcode[1]);
                OPN.refresh_fc_eg_slot(OPN.type, 2, 2, OPN.SL3.fc[2], OPN.SL3.kcode[2]);
                OPN.refresh_fc_eg_slot(OPN.type, 2, 1, OPN.SL3.fc[0], OPN.SL3.kcode[0]);
                OPN.refresh_fc_eg_slot(OPN.type, 2, 3, OPN.CH[2].fc, OPN.CH[2].kcode);
            }
        } else {
            OPN.refresh_fc_eg_chan(OPN.type, 2);
        }
        FM.LFO_AM = 0;
        FM.LFO_PM = 0;
        for (i = 0; i < length; i++) {
            FM.out_fm[0] = 0;
            FM.out_fm[1] = 0;
            FM.out_fm[2] = 0;
            OPN.eg_timer += OPN.eg_timer_add;
            while (OPN.eg_timer >= OPN.eg_timer_overflow) {
                OPN.eg_timer -= OPN.eg_timer_overflow;
                OPN.eg_cnt++;
                OPN.advance_eg_channel(0);
                OPN.advance_eg_channel(1);
                OPN.advance_eg_channel(2);
            }
            OPN.chan_calc(0, 0);
            OPN.chan_calc(1, 1);
            OPN.chan_calc(2, 2);
            {
                int lt;
                lt = FM.out_fm[0] + FM.out_fm[1] + FM.out_fm[2];
                lt >>= 0;
                lt = FM.Limit(lt, 32767, -32768);
                stream.streamoutput[0][offset + i] = lt;
            }
        }
    }

    public int ym2203_timer_over(int c) {
        if (c != 0) {
            OPN.TimerBOver();
        } else {
            stream.stream_update();
            OPN.TimerAOver();
            if ((OPN.ST.mode & 0x80) != 0) {
                OPN.CSMKeyControll();
            }
        }
        return OPN.ST.irq;
    }

    public void ym2203_reset_chip() {
        int i;
        OPN.OPNPrescaler_w(0, 1);
        OPN.ST.SSG.reset.run();
        OPN.FM_IRQMASK_SET((byte) 0x03);
        OPN.FM_BUSY_CLEAR();
        OPN.OPNWriteMode(0x27, (byte) 0x30);
        OPN.eg_timer = 0;
        OPN.eg_cnt = 0;
        OPN.FM_STATUS_RESET((byte) 0xff);
        OPN.reset_channels(3);
        for (i = 0xb2; i >= 0x30; i--) {
            OPN.OPNWriteReg(i, (byte) 0);
        }
        for (i = 0x26; i >= 0x20; i--) {
            OPN.OPNWriteReg(i, (byte) 0);
        }
    }

    public void ym2203_postload() {
        int r;
        OPN.OPNPrescaler_w(1, 1);
        for (r = 0; r < 16; r++) {
            OPN.ST.SSG.write.accept(0, (byte) r);
            OPN.ST.SSG.write.accept(1, REGS[r]);
        }
        for (r = 0x30; r < 0x9e; r++) {
            if ((r & 3) != 3) {
                OPN.OPNWriteReg(r, REGS[r]);
            }
        }
        for (r = 0xb0; r < 0xb6; r++) {
            if ((r & 3) != 3) {
                OPN.OPNWriteReg(r, REGS[r]);
            }
        }
    }

    public void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        writer.write(REGS, 0, 256);
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
        for (i = 0; i < 3; i++) {
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
        REGS = reader.readBytes(256);
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
        for (i = 0; i < 3; i++) {
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
