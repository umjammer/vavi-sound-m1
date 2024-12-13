/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;


import java.io.IOException;
import java.util.function.Consumer;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Cpuint.LineState;
import m1.emu.Machine;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;


public class YM3812 {

    public static EmuTimer[] timer;
    // ym3812_delegate
    public static Consumer<LineState> ym3812handler;

    public static void IRQHandler_3812(int irq) {
        if (ym3812handler != null) {
            ym3812handler.accept(irq != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void timer_callback_3812_0() {
        FMOpl.ym3812_timer_over(0);
    }

    public static void timer_callback_3812_1() {
        FMOpl.ym3812_timer_over(1);
    }

    private static void TimerHandler_3812(int c, Atime period) {
        if (Attotime.attotime_compare(period, Attotime.ATTOTIME_ZERO) == 0) {
            Timer.enable(timer[c], false);
        } else {
            Timer.adjustPeriodic(timer[c], period, Attotime.ATTOTIME_NEVER);
        }
    }

    public static void _stream_update_3812(int interval) {
        Sound.ym3812stream.stream_update();
    }

    public static void ym3812_start(int clock) {
        FMOpl.tl_tab = new int[0x1800];
        FMOpl.sin_tab = new int[0x1000];
        timer = new EmuTimer[2];
        int rate = clock / 72;
        switch (Machine.sName) {
            case "pcktgal":
            case "pcktgalb":
            case "pcktgal2":
            case "pcktgal2j":
            case "spool3":
            case "spool3i":
                ym3812handler = null;
                break;
            case "starfigh":
                ym3812handler = null;
                break;
            case "drgnwrld":
            case "drgnwrldv30":
            case "drgnwrldv21":
            case "drgnwrldv21j":
            case "drgnwrldv20j":
            case "drgnwrldv10c":
            case "drgnwrldv11h":
            case "drgnwrldv40k":
                ym3812handler = null;
                break;
            default:
                ym3812handler = null;
                break;
        }
        FMOpl.ym3812_init(0, clock, rate);
        FMOpl.ym3812_set_timer_handler(YM3812::TimerHandler_3812);
        FMOpl.ym3812_set_irq_handler(YM3812::IRQHandler_3812);
        FMOpl.ym3812_set_update_handler(YM3812::_stream_update_3812);
        timer[0] = Timer.allocCommon(YM3812::timer_callback_3812_0, "timer_callback_3812_0", false);
        timer[1] = Timer.allocCommon(YM3812::timer_callback_3812_1, "timer_callback_3812_1", false);
    }

    public static void ym3812_control_port_0_w(byte data) {
        FMOpl.ym3812_write(0, data);
    }

    public static void ym3812_write_port_0_w(byte data) {
        FMOpl.ym3812_write(1, data);
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        for (i = 0; i < 9; i++) {
            writer.write(FMOpl.YM3812.P_CH[i].block_fnum);
            writer.write(FMOpl.YM3812.P_CH[i].kcode);
            for (j = 0; j < 2; j++) {
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].ar);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].dr);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].rr);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].KSR);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].ksl);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].ksr);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].mul);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].Cnt);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].FB);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].op1_out[0]);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].op1_out[1]);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].CON);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].eg_type);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].state);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].TL);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].volume);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].sl);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].key);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].AMmask);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].vib);
                writer.write(FMOpl.YM3812.P_CH[i].SLOT[j].wavetable);
            }
        }
        writer.write(FMOpl.YM3812.eg_cnt);
        writer.write(FMOpl.YM3812.eg_timer);
        writer.write(FMOpl.YM3812.rhythm);
        writer.write(FMOpl.YM3812.lfo_am_depth);
        writer.write(FMOpl.YM3812.lfo_pm_depth_range);
        writer.write(FMOpl.YM3812.lfo_am_cnt);
        writer.write(FMOpl.YM3812.lfo_pm_cnt);
        writer.write(FMOpl.YM3812.noise_rng);
        writer.write(FMOpl.YM3812.noise_p);
        writer.write(FMOpl.YM3812.wavesel);
        for (i = 0; i < 2; i++) {
            writer.write(FMOpl.YM3812.T[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(FMOpl.YM3812.st[i]);
        }
        writer.write(FMOpl.YM3812.address);
        writer.write(FMOpl.YM3812.status);
        writer.write(FMOpl.YM3812.statusmask);
        writer.write(FMOpl.YM3812.mode);
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i, j;
        for (i = 0; i < 9; i++) {
            FMOpl.YM3812.P_CH[i].block_fnum = reader.readUInt32();
            FMOpl.YM3812.P_CH[i].kcode = reader.readByte();
            for (j = 0; j < 2; j++) {
                FMOpl.YM3812.P_CH[i].SLOT[j].ar = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].dr = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].rr = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].KSR = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].ksl = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].ksr = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].mul = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].Cnt = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].FB = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].op1_out[0] = reader.readInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].op1_out[1] = reader.readInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].CON = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].eg_type = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].state = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].TL = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].volume = reader.readInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].sl = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].key = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].AMmask = reader.readUInt32();
                FMOpl.YM3812.P_CH[i].SLOT[j].vib = reader.readByte();
                FMOpl.YM3812.P_CH[i].SLOT[j].wavetable = reader.readUInt16();
            }
        }
        FMOpl.YM3812.eg_cnt = reader.readUInt32();
        FMOpl.YM3812.eg_timer = reader.readUInt32();
        FMOpl.YM3812.rhythm = reader.readByte();
        FMOpl.YM3812.lfo_am_depth = reader.readByte();
        FMOpl.YM3812.lfo_pm_depth_range = reader.readByte();
        FMOpl.YM3812.lfo_am_cnt = reader.readUInt32();
        FMOpl.YM3812.lfo_pm_cnt = reader.readUInt32();
        FMOpl.YM3812.noise_rng = reader.readUInt32();
        FMOpl.YM3812.noise_p = reader.readUInt32();
        FMOpl.YM3812.wavesel = reader.readByte();
        for (i = 0; i < 2; i++) {
            FMOpl.YM3812.T[i] = reader.readUInt32();
        }
        for (i = 0; i < 2; i++) {
            FMOpl.YM3812.st[i] = reader.readByte();
        }
        FMOpl.YM3812.address = reader.readByte();
        FMOpl.YM3812.status = reader.readByte();
        FMOpl.YM3812.statusmask = reader.readByte();
        FMOpl.YM3812.mode = reader.readByte();
    }

    public static void IRQHandler_3526(int irq) {
        if (ym3812handler != null) {
            ym3812handler.accept(irq != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void timer_callback_3526_0() {
        FMOpl.ym3526_timer_over(0);
    }

    public static void timer_callback_3526_1() {
        FMOpl.ym3812_timer_over(1);
    }

    public static void TimerHandler_3526(int c, Atime period) {
        if (Attotime.attotime_compare(period, Attotime.ATTOTIME_ZERO) == 0) {
            Timer.enable(timer[c], false);
        } else {
            Timer.adjustPeriodic(timer[c], period, Attotime.ATTOTIME_NEVER);
        }
    }

    public static void _stream_update_3526(int interval) {
        Sound.ym3526stream.stream_update();
    }

    public static void ym3526_start(int clock) {
        int rate = clock / 72;
        FMOpl.YM3526 = FMOpl.ym3526_init(0, clock, rate);
        timer = new EmuTimer[2];
        FMOpl.ym3526_set_timer_handler(YM3812::TimerHandler_3526);
        FMOpl.ym3526_set_irq_handler(YM3812::IRQHandler_3526);
        FMOpl.ym3526_set_update_handler(YM3812::_stream_update_3526);
        timer[0] = Timer.allocCommon(YM3812::timer_callback_3526_0, "timer_callback_3526_0", false);
        timer[1] = Timer.allocCommon(YM3812::timer_callback_3526_1, "timer_callback_3526_1", false);
    }

    public static void ym3526_control_port_0_w(byte data) {
        FMOpl.ym3526_write(0, data);
    }

    public static void ym3526_write_port_0_w(byte data) {
        FMOpl.ym3526_write(1, data);
    }

    public static byte ym3526_status_port_0_r() {
        return FMOpl.ym3526_read(0);
    }

    public static byte ym3526_read_port_0_r() {
        return FMOpl.ym3526_read(1);
    }

    public static void ym3526_control_port_1_w(byte data) {
        FMOpl.ym3526_write(0, data);
    }

    public static void ym3526_write_port_1_w(byte data) {
        FMOpl.ym3526_write(1, data);
    }

    public static byte ym3526_status_port_1_r() {
        return FMOpl.ym3526_read(0);
    }

    public static byte ym3526_read_port_1_r() {
        return FMOpl.ym3526_read(1);
    }

    public static void SaveStateBinary_YM3526(BinaryWriter writer) {
        int i, j;
        for (i = 0; i < 9; i++) {
            writer.write(FMOpl.YM3526.P_CH[i].block_fnum);
            writer.write(FMOpl.YM3526.P_CH[i].kcode);
            for (j = 0; j < 2; j++) {
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].ar);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].dr);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].rr);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].KSR);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].ksl);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].ksr);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].mul);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].Cnt);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].FB);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].op1_out[0]);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].op1_out[1]);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].CON);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].eg_type);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].state);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].TL);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].volume);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].sl);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].key);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].AMmask);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].vib);
                writer.write(FMOpl.YM3526.P_CH[i].SLOT[j].wavetable);
            }
        }
        writer.write(FMOpl.YM3526.eg_cnt);
        writer.write(FMOpl.YM3526.eg_timer);
        writer.write(FMOpl.YM3526.rhythm);
        writer.write(FMOpl.YM3526.lfo_am_depth);
        writer.write(FMOpl.YM3526.lfo_pm_depth_range);
        writer.write(FMOpl.YM3526.lfo_am_cnt);
        writer.write(FMOpl.YM3526.lfo_pm_cnt);
        writer.write(FMOpl.YM3526.noise_rng);
        writer.write(FMOpl.YM3526.noise_p);
        writer.write(FMOpl.YM3526.wavesel);
        for (i = 0; i < 2; i++) {
            writer.write(FMOpl.YM3526.T[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(FMOpl.YM3526.st[i]);
        }
        writer.write(FMOpl.YM3526.address);
        writer.write(FMOpl.YM3526.status);
        writer.write(FMOpl.YM3526.statusmask);
        writer.write(FMOpl.YM3526.mode);
    }

    public static void LoadStateBinary_YM3526(BinaryReader reader) throws IOException {
        int i, j;
        for (i = 0; i < 9; i++) {
            FMOpl.YM3526.P_CH[i].block_fnum = reader.readUInt32();
            FMOpl.YM3526.P_CH[i].kcode = reader.readByte();
            for (j = 0; j < 2; j++) {
                FMOpl.YM3526.P_CH[i].SLOT[j].ar = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].dr = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].rr = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].KSR = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].ksl = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].ksr = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].mul = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].Cnt = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].FB = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].op1_out[0] = reader.readInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].op1_out[1] = reader.readInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].CON = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].eg_type = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].state = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].TL = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].volume = reader.readInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].sl = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].key = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].AMmask = reader.readUInt32();
                FMOpl.YM3526.P_CH[i].SLOT[j].vib = reader.readByte();
                FMOpl.YM3526.P_CH[i].SLOT[j].wavetable = reader.readUInt16();
            }
        }
        FMOpl.YM3526.eg_cnt = reader.readUInt32();
        FMOpl.YM3526.eg_timer = reader.readUInt32();
        FMOpl.YM3526.rhythm = reader.readByte();
        FMOpl.YM3526.lfo_am_depth = reader.readByte();
        FMOpl.YM3526.lfo_pm_depth_range = reader.readByte();
        FMOpl.YM3526.lfo_am_cnt = reader.readUInt32();
        FMOpl.YM3526.lfo_pm_cnt = reader.readUInt32();
        FMOpl.YM3526.noise_rng = reader.readUInt32();
        FMOpl.YM3526.noise_p = reader.readUInt32();
        FMOpl.YM3526.wavesel = reader.readByte();
        for (i = 0; i < 2; i++) {
            FMOpl.YM3526.T[i] = reader.readUInt32();
        }
        for (i = 0; i < 2; i++) {
            FMOpl.YM3526.st[i] = reader.readByte();
        }
        FMOpl.YM3526.address = reader.readByte();
        FMOpl.YM3526.status = reader.readByte();
        FMOpl.YM3526.statusmask = reader.readByte();
        FMOpl.YM3526.mode = reader.readByte();
    }
}
