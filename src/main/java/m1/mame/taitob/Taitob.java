/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.taitob;

import java.awt.event.KeyEvent;
import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m68000.MC68000;
import m1.cpu.z80.Z80A;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Eeprom;
import m1.emu.Generic;
import m1.emu.Inptport;
import m1.emu.Inptport.FrameArray;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.Memory;
import m1.emu.Palette;
import m1.emu.RomInfo;
import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Tilemap.trans_t;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.emu.Watchdog;
import m1.sound.AY8910;
import m1.sound.FM;
import m1.sound.Sound;
import m1.sound.Taitosnd;
import m1.sound.YM2610;
import m1.sound.YMDeltat;


public class Taitob {

    public static byte[] gfxrom, gfx0rom, gfx1rom, mainram2, mainram3;
    public static short eep_latch;
    public static short coin_word;
    public static int basebanksnd;
    public static byte dswa, dswb, dswb_old;

    public static void TaitobInit() {
        int i, n;
        Generic.paletteram16 = new short[0x1000];
        TC0180VCU_ram = new short[0x8000];
        TC0180VCU_ctrl = new short[0x10];
        TC0220IOC_regs = new byte[8];
        TC0220IOC_port = 0;
        TC0640FIO_regs = new byte[8];
        taitob_scroll = new short[0x400];
        Memory.mainram = new byte[0x1_0000];
        mainram2 = new byte[0x1e80];
        mainram3 = new byte[0x2000];
        Memory.audioram = new byte[0x2000];
        bg_rambank = new short[2];
        fg_rambank = new short[2];
        pixel_scroll = new short[2];
        taitob_spriteram = new short[0xcc0];
        TC0640FIO_regs = new byte[8];
        Machine.bRom = true;
        Taitosnd.taitosnd_start();
        basebanksnd = 0x1_0000;
        eep_latch = 0;
        video_control = 0;
        coin_word = 0;
        for (i = 0; i < 0x10; i++) {
            TC0180VCU_ctrl[i] = 0;
        }
        Machine.bRom = true;
        Memory.mainrom = new byte[0x1_0000]; //Machine.GetRom("maincpu.rom");
        Memory.audiorom = Machine.GetRom("audiocpu.rom");
        gfxrom = new byte[0x1_0000]; //Machine.GetRom("gfx1.rom");
        n = gfxrom.length;
        gfx0rom = new byte[n * 2];
        gfx1rom = new byte[n * 2];
        for (i = 0; i < n; i++) {
            gfx1rom[i * 2] = (byte) (gfxrom[i] >> 4);
            gfx1rom[i * 2 + 1] = (byte) (gfxrom[i] & 0x0f);
        }
        for (i = 0; i < n; i++) {
            gfx0rom[((i / 0x10) % 8 + (i / 0x80 * 0x10) + ((i / 8) % 2) * 8) * 8 + (i % 8)] = gfx1rom[i];
        }
        FM.ymsndrom = Machine.GetRom("ymsnd.rom");
        YMDeltat.ymsnddeltatrom = Machine.GetRom("ymsnddeltat.rom");
        if (Memory.mainrom == null || gfxrom == null || Memory.audiorom == null || FM.ymsndrom == null) {
            Machine.bRom = false;
        }
        if (Machine.bRom) {
            switch (Machine.sName) {
                case "pbobble":
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    break;
                case "silentd":
                case "silentdj":
                case "silentdu":
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xbf;
                    break;
            }
        }
    }

    public static void irqhandler(int irq) {
        Cpuint.cpunum_set_input_line(1, 0, irq != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void bankswitch_w(byte data) {
        basebanksnd = 0x1_0000 + 0x4000 * ((data - 1) & 3);
    }

    public static void rsaga2_interrupt2() {
        Cpuint.cpunum_set_input_line(0, 2, LineState.HOLD_LINE);
    }

    public static void rastansaga2_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::rsaga2_interrupt2, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void crimec_interrupt3() {
        Cpuint.cpunum_set_input_line(0, 3, LineState.HOLD_LINE);
    }

    public static void crimec_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::crimec_interrupt3, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
    }

    public static void hitice_interrupt6() {
        Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
    }

    public static void hitice_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::hitice_interrupt6, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void rambo3_interrupt1() {
        Cpuint.cpunum_set_input_line(0, 1, LineState.HOLD_LINE);
    }

    public static void rambo3_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::rambo3_interrupt1, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
    }

    public static void pbobble_interrupt5() {
        Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
    }

    public static void pbobble_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::pbobble_interrupt5, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 3, LineState.HOLD_LINE);
    }

    public static void viofight_interrupt1() {
        Cpuint.cpunum_set_input_line(0, 1, LineState.HOLD_LINE);
    }

    public static void viofight_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::viofight_interrupt1, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void masterw_interrupt4() {
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void masterw_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::masterw_interrupt4, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
    }

    public static void silentd_interrupt4() {
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void silentd_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::silentd_interrupt4, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
    }

    public static void selfeena_interrupt4() {
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void selfeena_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::selfeena_interrupt4, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 5000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
    }

    public static void sbm_interrupt5() {
        Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
    }

    public static void sbm_interrupt() {
        EmuTimer timer = Timer.allocCommon(Taitob::sbm_interrupt5, "vblank_interrupt2", true);
        Timer.adjustPeriodic(timer, new Atime(0, 10000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
    }

    public static void mb87078_gain_changed(int channel, int percent) {
        if (channel == 1) {
            AY8910.AA8910[0].stream.gain = (int) (0x100 * (percent / 100.0));
//            sound_type type = Machine.config.sound[0].type;
//            sndti_set_output_gain(type, 0, 0, percent / 100.0);
//            sndti_set_output_gain(type, 1, 0, percent / 100.0);
//            sndti_set_output_gain(type, 2, 0, percent / 100.0);
        }
    }

    public static void machine_reset_mb87078() {
        MB87078_start(0);
    }

    public static void gain_control_w1(int offset, byte data) {
        if (offset == 0) {
            MB87078_data_w(0, data, 0);
        } else {
            MB87078_data_w(0, data, 1);
        }
    }

    public static void gain_control_w(int offset, short data) {
        if (offset == 0) {
            MB87078_data_w(0, data >> 8, 0);
        } else {
            MB87078_data_w(0, data >> 8, 1);
        }
    }

    public static void nvram_handler_load_taitob() {
    }

    public static void nvram_handler_save_taitob() {
    }

    public static short eeprom_r() {
        short res;
        res = (short) (Eeprom.eeprom_read_bit() & 0x01);
        res |= (short) (dswb & 0xfe);
        return res;
    }

    public static short eep_latch_r() {
        return eep_latch;
    }

    public static void eeprom_w1(byte data) {
        eep_latch = (short) ((data << 8) | (eep_latch & 0xff));
        Eeprom.eeprom_write_bit(data & 0x04);
        Eeprom.eeprom_set_clock_line(((data & 0x08) != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        Eeprom.eeprom_set_cs_line(((data & 0x10) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
    }

    public static void eeprom_w2(byte data) {
        eep_latch = (short) ((eep_latch & 0xff00) | data);
    }

    public static void eeprom_w(short data) {
        eep_latch = data;
        data >>= 8;
        Eeprom.eeprom_write_bit(data & 0x04);
        Eeprom.eeprom_set_clock_line(((data & 0x08) != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        Eeprom.eeprom_set_cs_line(((data & 0x10) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
    }

    public static void player_34_coin_ctrl_w(short data) {
        coin_word = data;
//        coin_lockout_w(2, ~data & 0x0100);
//        coin_lockout_w(3, ~data & 0x0200);
//        coin_counter_w(2, data & 0x0400);
//        coin_counter_w(3, data & 0x0800);
    }

    public static short pbobble_input_bypass_r(int offset) {
        short result = 0;
        switch (offset) {
            case 0x01:
                result = (short) (eeprom_r() << 8);
                break;
            default:
                result = (short) (TC0640FIO_r(offset) << 8);
                break;
        }
        return result;
    }

    public static void play_taitob_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_taitob_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_taitob_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//#region Video

    public static short[][] framebuffer;
    public static short[] taitob_scroll, TC0180VCU_ram, taitob_spriteram, taitob_pixelram;
    public static short[] bg_rambank, fg_rambank, pixel_scroll, TC0180VCU_ctrl;
    public static short tx_rambank;
    public static byte framebuffer_page, video_control;
    public static int b_bg_color_base = 0, b_fg_color_base = 0, b_sp_color_base = 0, b_tx_color_base = 0;
    public static byte[] TC0220IOC_regs, TC0640FIO_regs;
    public static byte TC0220IOC_port;
    public static RECT cliprect;
    public static short[] uuB0000;

    public static void taitob_video_control(byte data) {
        video_control = data;
        if ((video_control & 0x80) != 0) {
            framebuffer_page = (byte) ((~video_control & 0x40) >> 6);
        }
        //tilemap_set_flip(ALL_TILEMAPS, (video_control & 0x10) ? (TILEMAP_FLIPX | TILEMAP_FLIPY) : 0 );
    }

    public static short TC0180VCU_word_r(int offset) {
        return TC0180VCU_ram[offset];
    }

    public static void TC0180VCU_word_w1(int offset, byte data) {
        int row, col;
        TC0180VCU_ram[offset] = (short) ((data << 8) | (byte) TC0180VCU_ram[offset]);
        if ((offset & 0x7000) == fg_rambank[0] || (offset & 0x7000) == fg_rambank[1]) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            fg_tilemap.tilemap_mark_tile_dirty(row, col);
        }
        if ((offset & 0x7000) == bg_rambank[0] || (offset & 0x7000) == bg_rambank[1]) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            bg_tilemap.tilemap_mark_tile_dirty(row, col);
        }
        if ((offset & 0x7800) == tx_rambank) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            tx_tilemap.tilemap_mark_tile_dirty(row, col);
        }
    }

    public static void TC0180VCU_word_w2(int offset, byte data) {
        int row, col;
        TC0180VCU_ram[offset] = (short) ((TC0180VCU_ram[offset] & 0xff00) | data);
        if ((offset & 0x7000) == fg_rambank[0] || (offset & 0x7000) == fg_rambank[1]) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            fg_tilemap.tilemap_mark_tile_dirty(row, col);
            //tilemap_mark_tile_dirty(fg_tilemap, offset & 0x0fff);
        }
        if ((offset & 0x7000) == bg_rambank[0] || (offset & 0x7000) == bg_rambank[1]) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            bg_tilemap.tilemap_mark_tile_dirty(row, col);
            //tilemap_mark_tile_dirty(bg_tilemap, offset2 & 0x0fff);
        }
        if ((offset & 0x7800) == tx_rambank) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            tx_tilemap.tilemap_mark_tile_dirty(row, col);
            //tilemap_mark_tile_dirty(tx_tilemap, offset2 & 0x7ff);
        }
    }

    public static void TC0180VCU_word_w(int offset, short data) {
        int row, col;
        TC0180VCU_ram[offset] = data;
        if ((offset & 0x7000) == fg_rambank[0] || (offset & 0x7000) == fg_rambank[1]) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            fg_tilemap.tilemap_mark_tile_dirty(row, col);
        }
        if ((offset & 0x7000) == bg_rambank[0] || (offset & 0x7000) == bg_rambank[1]) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            bg_tilemap.tilemap_mark_tile_dirty(row, col);
        }
        if ((offset & 0x7800) == tx_rambank) {
            row = (offset & 0x0fff) / 64;
            col = (offset & 0x0fff) % 64;
            tx_tilemap.tilemap_mark_tile_dirty(row, col);
        }
    }

    public static void video_start_taitob_core() {
        int i;
        uuB0000 = new short[0x200 * 0x100];
        for (i = 0; i < 0x2_0000; i++) {
            uuB0000[i] = 0x0;
        }
        cliprect = new RECT();
        cliprect.min_x = 0;
        cliprect.max_x = 319;
        cliprect.min_y = 16;
        cliprect.max_y = 239;
//        framebuffer[0] = auto_bitmap_alloc(512, 256, video_screen_get_format(machine.primary_screen));
//        framebuffer[1] = auto_bitmap_alloc(512, 256, video_screen_get_format(machine.primary_screen));
//        pixel_bitmap = null;  /* only hitice needs this */

//        tilemap_set_transparent_pen(fg_tilemap, 0);
//        tilemap_set_transparent_pen(tx_tilemap, 0);
        bg_tilemap.tilemap_set_scrolldx(0, 24 * 8);
        fg_tilemap.tilemap_set_scrolldx(0, 24 * 8);
        tx_tilemap.tilemap_set_scrolldx(0, 24 * 8);
    }

    public static void video_start_taitob_color_order1() {
        b_bg_color_base = 0x00;
        b_fg_color_base = 0x40;
        b_sp_color_base = 0x80 * 16;
        b_tx_color_base = 0xc0;
        video_start_taitob_core();
    }

    public static void video_start_taitob_color_order2() {
        b_bg_color_base = 0x30;
        b_fg_color_base = 0x20;
        b_sp_color_base = 0x10 * 16;
        b_tx_color_base = 0x00;
        video_start_taitob_core();
    }

    public static short TC0180VCU_framebuffer_word_r(int offset) {
        int sy = offset >> 8;
        int sx = 2 * (offset & 0xff);
        return framebuffer[sy >> 8][(sy & 0xff) + sx];
    }

    public static void TC0180VCU_framebuffer_word_w1(int offset, byte data) {
        int sy = offset >> 8;
        int sx = 2 * (offset & 0xff);
        framebuffer[sy >> 8][(sy & 0xff) * 0x200 + sx] = (short) ((short) (data << 8) | (framebuffer[sy >> 8][(sy & 0xff) * 0x200 + sx] & 0xff));
    }

    public static void TC0180VCU_framebuffer_word_w2(int offset, byte data) {
        int sy = offset >> 8;
        int sx = 2 * (offset & 0xff);
        framebuffer[sy >> 8][(sy & 0xff) * 0x200 + sx] = (short) ((framebuffer[sy >> 8][(sy & 0xff) * 0x200 + sx] & 0xff00) | data);
    }

    public static void TC0180VCU_framebuffer_word_w(int offset, short data) {
        int sy = offset >> 8;
        int sx = 2 * (offset & 0xff);
        framebuffer[sy >> 8][(sy & 0xff) * 0x200 + sx] = data;
    }

    public static byte TC0220IOC_r(int offset) {
        byte result = 0;
        switch (offset) {
            case 0x00:    /* IN00-07 (DSA) */
                result = dswa;
                break;
            case 0x01:    /* IN08-15 (DSB) */
                result = dswb;
                break;
            case 0x02:    /* IN16-23 (1P) */
                result = sbyte0;
                break;
            case 0x03:    /* IN24-31 (2P) */
                result = sbyte1;
                break;
            case 0x04:    /* coin counters and lockout */
                result = TC0220IOC_regs[4];
                break;
            case 0x07:    /* INB0-7 (coin) */
                result = sbyte2;
                break;
            default:
                result = (byte) 0xff;
                break;
        }
        return result;
    }

    public static void TC0220IOC_w(int offset, byte data) {
        TC0220IOC_regs[offset] = data;
        switch (offset) {
            case 0x00:
                Watchdog.watchdog_reset();
                break;

            case 0x04:
//                coin_lockout_w(0, ~data & 0x01);
//                coin_lockout_w(1, ~data & 0x02);
//                coin_counter_w(0, data & 0x04);
//                coin_counter_w(1, data & 0x08);
                break;
            default:
                break;
        }
    }

    public static short TC0220IOC_halfword_r(int offset) {
        return TC0220IOC_r(offset);
    }

    public static void TC0220IOC_halfword_w1(int offset, byte data) {
        TC0220IOC_w(offset, data);
    }

    public static void TC0220IOC_halfword_w(int offset, short data) {
        TC0220IOC_w(offset, (byte) data);
    }

    public static short taitob_v_control_r(int offset) {
        return TC0180VCU_ctrl[offset];
    }

    public static void taitob_v_control_w1(int offset, byte data) {
        short oldword = TC0180VCU_ctrl[offset];
        TC0180VCU_ctrl[offset] = (short) ((data << 8) | (TC0180VCU_ctrl[offset] & 0xff));
        switch (offset) {
            case 0:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    fg_tilemap.all_tiles_dirty = true;
                    fg_rambank[0] = (short) (((TC0180VCU_ctrl[offset] >> 8) & 0x0f) << 12);
                    fg_rambank[1] = (short) (((TC0180VCU_ctrl[offset] >> 12) & 0x0f) << 12);
                }
                break;
            case 1:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    bg_tilemap.all_tiles_dirty = true;
                    bg_rambank[0] = (short) (((TC0180VCU_ctrl[offset] >> 8) & 0x0f) << 12);
                    bg_rambank[1] = (short) (((TC0180VCU_ctrl[offset] >> 12) & 0x0f) << 12);
                }
                break;
            case 4:
            case 5:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    tx_tilemap.all_tiles_dirty = true;
                }
                break;
            case 6:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    tx_tilemap.all_tiles_dirty = true;
                    tx_rambank = (short) (((TC0180VCU_ctrl[offset] >> 8) & 0x0f) << 11);
                }
                break;
            case 7:
                taitob_video_control((byte) ((TC0180VCU_ctrl[offset] >> 8) & 0xff));
                break;
            default:
                break;
        }
    }

    public static void taitob_v_control_w2(int offset, byte data) {
        TC0180VCU_ctrl[offset] = (short) ((TC0180VCU_ctrl[offset] & 0xff00) | data);
    }

    public static void taitob_v_control_w(int offset, short data) {
        short oldword = TC0180VCU_ctrl[offset];
        TC0180VCU_ctrl[offset] = data;
        switch (offset) {
            case 0:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    fg_tilemap.all_tiles_dirty = true;
                    fg_rambank[0] = (short) (((TC0180VCU_ctrl[offset] >> 8) & 0x0f) << 12);
                    fg_rambank[1] = (short) (((TC0180VCU_ctrl[offset] >> 12) & 0x0f) << 12);
                }
                break;
            case 1:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    bg_tilemap.all_tiles_dirty = true;
                    bg_rambank[0] = (short) (((TC0180VCU_ctrl[offset] >> 8) & 0x0f) << 12);
                    bg_rambank[1] = (short) (((TC0180VCU_ctrl[offset] >> 12) & 0x0f) << 12);
                }
                break;
            case 4:
            case 5:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    tx_tilemap.all_tiles_dirty = true;
                }
                break;
            case 6:
                if (oldword != TC0180VCU_ctrl[offset]) {
                    tx_tilemap.all_tiles_dirty = true;
                    tx_rambank = (short) (((TC0180VCU_ctrl[offset] >> 8) & 0x0f) << 11);
                }
                break;
            case 7:
                taitob_video_control((byte) ((TC0180VCU_ctrl[offset] >> 8) & 0xff));
                break;
            default:
                break;
        }
    }

    public static void hitice_pixelram_w(int offset, short data) {
        int sy = offset >> 9;
        int sx = offset & 0x1ff;
        taitob_pixelram[offset] = data;
    }

    public static byte TC0640FIO_r(int offset) {
        byte result = 0;
        switch (offset) {
            case 0x00:    /* DSA */
                result = dswa;
                break;
            case 0x01:    /* DSB */
                result = dswb;
                break;
            case 0x02:    /* 1P */
                result = sbyte0;
                break;
            case 0x03:    /* 2P */
                result = sbyte1;
                break;
            case 0x04:    /* coin counters and lockout */
                result = TC0640FIO_regs[4];
                break;
            case 0x07:    /* coin */
                result = sbyte2;
                break;
            default:
                result = (byte) 0xff;
                break;
        }
        return result;
    }

    public static void TC0640FIO_w(int offset, byte data) {
        TC0640FIO_regs[offset] = data;
        switch (offset) {
            case 0x00:
                Watchdog.watchdog_reset();
                break;
            case 0x04:
//                coin_lockout_w(0, ~data & 0x01);
//                coin_lockout_w(1, ~data & 0x02);
//                coin_counter_w(0, data & 0x04);
//                coin_counter_w(1, data & 0x08);
                break;
            default:
                break;
        }
    }

    public static short TC0640FIO_halfword_r(int offset) {
        return TC0640FIO_r(offset);
    }

    public static void TC0640FIO_halfword_byteswap_w1(int offset, byte data) {
        TC0640FIO_w(offset, data);
    }

    public static void TC0640FIO_halfword_byteswap_w(int offset, short data) {
        TC0640FIO_w(offset, (byte) ((data >> 8) & 0xff));
    }

    public static RECT sect_rect(RECT dst, RECT src) {
        RECT dst2 = dst;
        if (src.min_x > dst.min_x) dst2.min_x = src.min_x;
        if (src.max_x < dst.max_x) dst2.max_x = src.max_x;
        if (src.min_y > dst.min_y) dst2.min_y = src.min_y;
        if (src.max_y < dst.max_y) dst2.max_y = src.max_y;
        return dst2;
    }

    public static void draw_sprites(RECT cliprect) {
        int x, y, xlatch = 0, ylatch = 0, x_no = 0, y_no = 0, x_num = 0, y_num = 0, big_sprite = 0;
        int offs, code, color, flipx, flipy;
        int data, zoomx, zoomy, zx, zy, zoomxlatch = 0, zoomylatch = 0;
        for (offs = (0x1980 - 16) / 2; offs >= 0; offs -= 8) {
            code = taitob_spriteram[offs];
            color = taitob_spriteram[offs + 1];
            flipx = color & 0x4000;
            flipy = color & 0x8000;
            color = (color & 0x3f) * 16;
            x = taitob_spriteram[offs + 2] & 0x3ff;
            y = taitob_spriteram[offs + 3] & 0x3ff;
            if (x >= 0x200) x -= 0x400;
            if (y >= 0x200) y -= 0x400;
            data = taitob_spriteram[offs + 5];
            if (data != 0) {
                if (big_sprite == 0) {
                    x_num = (data >> 8) & 0xff;
                    y_num = (data) & 0xff;
                    x_no = 0;
                    y_no = 0;
                    xlatch = x;
                    ylatch = y;
                    data = taitob_spriteram[offs + 4];
                    zoomxlatch = (data >> 8) & 0xff;
                    zoomylatch = (data) & 0xff;
                    big_sprite = 1;
                }
            }
            data = taitob_spriteram[offs + 4];
            zoomx = (data >> 8) & 0xff;
            zoomy = (data) & 0xff;
            zx = (0x100 - zoomx) / 16;
            zy = (0x100 - zoomy) / 16;
            if (big_sprite != 0) {
                zoomx = zoomxlatch;
                zoomy = zoomylatch;
                x = xlatch + x_no * (0x100 - zoomx) / 16;
                y = ylatch + y_no * (0x100 - zoomy) / 16;
                zx = xlatch + (x_no + 1) * (0x100 - zoomx) / 16 - x;
                zy = ylatch + (y_no + 1) * (0x100 - zoomy) / 16 - y;
                y_no++;
                if (y_no > y_num) {
                    y_no = 0;
                    x_no++;
                    if (x_no > x_num)
                        big_sprite = 0;
                }
            }
            if ((zoomx != 0) || (zoomy != 0)) {
                Drawgfx.common_drawgfxzoom_taitob(gfx1rom, code, color, flipx, flipy, x, y, cliprect, 0, (zx << 16) / 16, (zy << 16) / 16);
            } else {
                Drawgfx.common_drawgfx_taitob(gfx1rom, code, color, flipx, flipy, x, y, cliprect);
            }
        }
    }

    public static void TC0180VCU_tilemap_draw(RECT cliprect, Tmap tmap, int plane) {
        RECT my_clip = new RECT();
        int i;
        int scrollx, scrolly;
        int lines_per_block;    /* number of lines scrolled by the same amount (per one scroll value) */
        int number_of_blocks;    /* number of such blocks per _screen_ (256 lines) */
        lines_per_block = 256 - (TC0180VCU_ctrl[2 + plane] >> 8);
        number_of_blocks = 256 / lines_per_block;
        my_clip.min_x = cliprect.min_x;
        my_clip.max_x = cliprect.max_x;
        for (i = 0; i < number_of_blocks; i++) {
            scrollx = taitob_scroll[plane * 0x200 + i * 2 * lines_per_block];
            scrolly = taitob_scroll[plane * 0x200 + i * 2 * lines_per_block + 1];
            my_clip.min_y = i * lines_per_block;
            my_clip.max_y = (i + 1) * lines_per_block - 1;
            if ((video_control & 0x10) != 0)   /*flip screen*/ {
                my_clip.min_y = 0x100 - 1 - (i + 1) * lines_per_block - 1;
                my_clip.max_y = 0x100 - 1 - i * lines_per_block;
            }
            my_clip = sect_rect(my_clip, cliprect);
            if (my_clip.min_y <= my_clip.max_y) {
                tmap.tilemap_set_scrollx(0, -scrollx);
                tmap.tilemap_set_scrolly(0, -scrolly);
                tmap.tilemap_draw_primask(my_clip, 0x10, (byte) 0);
            }
        }
    }

    public static void draw_framebuffer(RECT cliprect, int priority) {
        RECT myclip = cliprect;
        int x, y;
        priority <<= 4;
        if ((video_control & 0x08) != 0) {
            if (priority != 0) {
                return;
            }
            if ((video_control & 0x10) != 0)   /*flip screen*/ {
                for (y = myclip.min_y; y <= myclip.max_y; y++) {
                    for (x = myclip.min_x; x <= myclip.max_x; x++) {
                        short c = framebuffer[framebuffer_page][y * 512 + x];
                        if (c != 0) {
                            Video.bitmapbase[Video.curbitmap][(255 - y) * 512 + 319 - x] = (short) (b_sp_color_base + c);
                        }
                    }
                }
            } else {
                for (y = myclip.min_y; y <= myclip.max_y; y++) {
                    for (x = myclip.min_x; x <= myclip.max_x; x++) {
                        short c = framebuffer[framebuffer_page][y * 512 + x];
                        if (c != 0) {
                            Video.bitmapbase[Video.curbitmap][y * 512 + x] = (short) (b_sp_color_base + c);
                        }
                    }
                }
            }
        } else {
            if ((video_control & 0x10) != 0)   /*flip screen*/ {
                for (y = myclip.min_y; y <= myclip.max_y; y++) {
                    for (x = myclip.min_x; x <= myclip.max_x; x++) {
                        short c = framebuffer[framebuffer_page][y * 512 + x];
                        if ((c != 0) && ((c & 0x10) == priority)) {
                            Video.bitmapbase[Video.curbitmap][(255 - y) * 512 + 319 - x] = (short) (b_sp_color_base + c);
                        }
                    }
                }
            } else {
                for (y = myclip.min_y; y <= myclip.max_y; y++) {
                    for (x = myclip.min_x; x <= myclip.max_x; x++) {
                        short c = framebuffer[framebuffer_page][y * 512 + x];
                        if ((c != 0) && ((c & 0x10) == priority)) {
                            Video.bitmapbase[Video.curbitmap][y * 512 + x] = (short) (b_sp_color_base + c);
                        }
                    }
                }
            }
        }
    }

    public static void video_update_taitob() {
        if ((video_control & 0x20) == 0) {
            System.arraycopy(uuB0000, 0, Video.bitmapbase[Video.curbitmap], 0, 0x2_0000);
            return;
        }
        /* Draw playfields */
        TC0180VCU_tilemap_draw(cliprect, bg_tilemap, 1);
        draw_framebuffer(cliprect, 1);
        TC0180VCU_tilemap_draw(cliprect, fg_tilemap, 0);
            /*if (pixel_bitmap)  // hitice only
            {
                int scrollx = -2 * pixel_scroll[0]; //+320;
                int scrolly = -pixel_scroll[1]; //+240;
                copyscrollbitmap_trans(bitmap, pixel_bitmap, 1, &scrollx, 1, &scrolly, cliprect, b_fg_color_base * 16);
            }*/
        draw_framebuffer(cliprect, 0);
        tx_tilemap.tilemap_draw_primask(cliprect, 0x10, (byte) 0);
    }

    public static void video_eof_taitob() {
        if ((~video_control & 0x01) != 0) {
            System.arraycopy(uuB0000, 0, framebuffer[framebuffer_page], 0, 0x2_0000);
        }
        if ((~video_control & 0x80) != 0) {
            framebuffer_page ^= 1;
        }
        draw_sprites(cliprect);
    }

//#endregion Tilemap

    public static Tmap bg_tilemap, fg_tilemap, tx_tilemap;

    public static void tilemap_init() {
        int i;
        framebuffer = new short[2][];
        for (i = 0; i < 2; i++) {
            framebuffer[i] = new short[0x200 * 0x100];
        }
        bg_tilemap = new Tmap();
        bg_tilemap.cols = 64;
        bg_tilemap.rows = 64;
        bg_tilemap.tilewidth = 16;
        bg_tilemap.tileheight = 16;
        bg_tilemap.width = 0x400;
        bg_tilemap.height = 0x400;
        bg_tilemap.enable = true;
        bg_tilemap.all_tiles_dirty = true;
        bg_tilemap.total_elements = gfx1rom.length / 0x100;
        bg_tilemap.pixmap = new short[0x400 * 0x400];
        bg_tilemap.flagsmap = new byte[0x400][0x400];
        bg_tilemap.tileflags = new byte[64][64];
        bg_tilemap.pen_data = new byte[0x100];
        bg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceTaitob;
        bg_tilemap.tile_update3 = bg_tilemap::tile_updateTaitobbg;

        fg_tilemap = new Tmap();
        fg_tilemap.cols = 64;
        fg_tilemap.rows = 64;
        fg_tilemap.tilewidth = 16;
        fg_tilemap.tileheight = 16;
        fg_tilemap.width = 0x400;
        fg_tilemap.height = 0x400;
        fg_tilemap.enable = true;
        fg_tilemap.all_tiles_dirty = true;
        fg_tilemap.total_elements = gfx1rom.length / 0x100;
        fg_tilemap.pixmap = new short[0x400 * 0x400];
        fg_tilemap.flagsmap = new byte[0x400][0x400];
        fg_tilemap.tileflags = new byte[64][64];
        fg_tilemap.pen_data = new byte[0x100];
        fg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 1; i < 16; i++) {
            fg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        fg_tilemap.pen_to_flags[0][0] = 0;
        fg_tilemap.scrollrows = 1;
        fg_tilemap.scrollcols = 1;
        fg_tilemap.rowscroll = new int[fg_tilemap.scrollrows];
        fg_tilemap.colscroll = new int[fg_tilemap.scrollcols];
        fg_tilemap.tilemap_draw_instance3 = fg_tilemap::tilemap_draw_instanceTaitob;
        fg_tilemap.tile_update3 = fg_tilemap::tile_updateTaitobfg;


        tx_tilemap = new Tmap();
        tx_tilemap.cols = 64;
        tx_tilemap.rows = 32;
        tx_tilemap.tilewidth = 8;
        tx_tilemap.tileheight = 8;
        tx_tilemap.width = 0x200;
        tx_tilemap.height = 0x100;
        tx_tilemap.enable = true;
        tx_tilemap.all_tiles_dirty = true;
        tx_tilemap.total_elements = gfx1rom.length / 0x40;
        tx_tilemap.pixmap = new short[0x100 * 0x200];
        tx_tilemap.flagsmap = new byte[0x100][0x200];
        tx_tilemap.tileflags = new byte[32][64];
        tx_tilemap.pen_data = new byte[0x40];
        tx_tilemap.pen_to_flags = new byte[1][16];
        for (i = 1; i < 16; i++) {
            tx_tilemap.pen_to_flags[0][i] = 0x10;
        }
        tx_tilemap.pen_to_flags[0][0] = 0;
        tx_tilemap.scrollrows = 1;
        tx_tilemap.scrollcols = 1;
        tx_tilemap.rowscroll = new int[tx_tilemap.scrollrows];
        tx_tilemap.colscroll = new int[tx_tilemap.scrollcols];
        tx_tilemap.tilemap_draw_instance3 = tx_tilemap::tilemap_draw_instanceTaitob;
        tx_tilemap.tile_update3 = tx_tilemap::tile_updateTaitobtx;
    }

    public static class Tmap extends Tilemap.Tmap {

        public void tilemap_draw_instanceTaitob(RECT cliprect, int xpos, int ypos) {
            int mincol, maxcol;
            int x1, y1, x2, y2;
            int y, nexty;
            int offsety1, offsety2;
            int i;
            x1 = Math.max(xpos, cliprect.min_x);
            x2 = Math.min(xpos + width, cliprect.max_x + 1);
            y1 = Math.max(ypos, cliprect.min_y);
            y2 = Math.min(ypos + height, cliprect.max_y + 1);
            if (x1 >= x2 || y1 >= y2)
                return;
            x1 -= xpos;
            y1 -= ypos;
            x2 -= xpos;
            y2 -= ypos;
            offsety1 = y1;
            mincol = x1 / tilewidth;
            maxcol = (x2 + tilewidth - 1) / tilewidth;
            y = y1;
            nexty = tileheight * (y1 / tileheight) + tileheight;
            nexty = Math.min(nexty, y2);
            for (; ; ) {
                int row = y / tileheight;
                trans_t prev_trans = trans_t.WHOLLY_TRANSPARENT;
                trans_t cur_trans;
                int x_start = x1;
                int column;
                for (column = mincol; column <= maxcol; column++) {
                    int x_end;
                    if (column == maxcol) {
                        cur_trans = trans_t.WHOLLY_TRANSPARENT;
                    } else {
                        if (tileflags[row][column] == Tilemap.TILE_FLAG_DIRTY) {
                            tile_update3.accept(column, row);
                        }
                        if ((tileflags[row][column] & mask) != 0) {
                            cur_trans = trans_t.MASKED;
                        } else {
                            cur_trans = ((flagsmap[offsety1][column * tilewidth] & mask) == value) ? trans_t.WHOLLY_OPAQUE :
                                    trans_t.WHOLLY_TRANSPARENT;
                        }
                    }
                    if (cur_trans == prev_trans)
                        continue;
                    x_end = column * tilewidth;
                    x_end = Math.max(x_end, x1);
                    x_end = Math.min(x_end, x2);
                    if (prev_trans != trans_t.WHOLLY_TRANSPARENT) {
                        int cury;
                        offsety2 = offsety1;
                        if (prev_trans == trans_t.WHOLLY_OPAQUE) {
                            for (cury = y; cury < nexty; cury++) {
                                System.arraycopy(pixmap, offsety2 * width + x_start, Video.bitmapbase[Video.curbitmap], (offsety2 + ypos) * 0x200 + xpos + x_start, x_end - x_start);
                                if (priority != 0) {
                                    for (i = xpos + x_start; i < xpos + x_end; i++) {
                                        Tilemap.priority_bitmap[offsety2 + ypos][i] = (byte) (Tilemap.priority_bitmap[offsety2 + ypos][i] | priority);
                                    }
                                }
                                offsety2++;
                            }
                        } else if (prev_trans == trans_t.MASKED) {
                            for (cury = y; cury < nexty; cury++) {
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                        Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x200 + i] = pixmap[offsety2 * width + i - xpos];
                                        Tilemap.priority_bitmap[offsety2 + ypos][i] = (byte) (Tilemap.priority_bitmap[offsety2 + ypos][i] | priority);
                                    }
                                }
                                offsety2++;
                            }
                        }
                    }
                    x_start = x_end;
                    prev_trans = cur_trans;
                }
                if (nexty == y2)
                    break;
                offsety1 += (nexty - y);
                y = nexty;
                nexty += tileheight;
                nexty = Math.min(nexty, y2);
            }
        }

        public void tile_updateTaitobbg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int tile, code, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;
            tile = Taitob.TC0180VCU_ram[tile_index + Taitob.bg_rambank[0]];
            code = tile % total_elements;
            color = Taitob.TC0180VCU_ram[tile_index + Taitob.bg_rambank[1]];
            pen_data_offset = code * 0x100;
            palette_base = 0x10 * (Taitob.b_bg_color_base + (color & 0x3f));
            group = 0;
            flags = (((color & 0x00c0) >> 6) & 0x03) ^ (attributes & 0x03);
            tileflags[row][col] = tile_drawTaitob(Taitob.gfx1rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updateTaitobfg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int tile, code, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;
            tile = Taitob.TC0180VCU_ram[tile_index + Taitob.fg_rambank[0]];
            code = tile % total_elements;
            color = Taitob.TC0180VCU_ram[tile_index + Taitob.fg_rambank[1]];
            pen_data_offset = code * 0x100;
            palette_base = 0x10 * (Taitob.b_fg_color_base + (color & 0x3f));
            group = 0;
            flags = (((color & 0x00c0) >> 6) & 0x03) ^ (attributes & 0x03);
            tileflags[row][col] = tile_drawTaitob(Taitob.gfx1rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updateTaitobtx(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int tile, code, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;
            tile = Taitob.TC0180VCU_ram[tile_index + Taitob.tx_rambank];
            code = ((tile & 0x07ff) | ((Taitob.TC0180VCU_ctrl[4 + ((tile & 0x800) >> 11)] >> 8) << 11)) % total_elements;
            color = Taitob.b_tx_color_base + ((tile >> 12) & 0x0f);
            pen_data_offset = code * 0x40;
            palette_base = 0x10 * color;
            group = 0;
            flags = attributes & 0x03;
            tileflags[row][col] = tile_drawTaitobtx(Taitob.gfx0rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public byte tile_drawTaitob(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
            byte andmask = (byte) 0xff, ormask = 0;
            int dx0 = 1, dy0 = 1;
            int tx, ty;
            byte pen, map;
            int offset1 = 0;
            int offsety1;
            int xoffs;
            System.arraycopy(bb1, pen_data_offset, pen_data, 0, 0x100);
            if ((flags & Tilemap.TILE_FLIPY) != 0) {
                y0 += tileheight - 1;
                dy0 = -1;
            }
            if ((flags & Tilemap.TILE_FLIPX) != 0) {
                x0 += tilewidth - 1;
                dx0 = -1;
            }
            for (ty = 0; ty < tileheight; ty++) {
                xoffs = 0;
                offsety1 = y0;
                y0 += dy0;
                for (tx = 0; tx < tilewidth; tx++) {
                    pen = pen_data[offset1];
                    map = pen_to_flags[group][pen];
                    offset1++;
                    pixmap[(offsety1 % width) * width + x0 + xoffs] = (short) (palette_base + pen);
                    flagsmap[offsety1 % width][x0 + xoffs] = map;
                    andmask &= map;
                    ormask |= map;
                    xoffs += dx0;
                }
            }
            return (byte) (andmask ^ ormask);
        }

        public byte tile_drawTaitobtx(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
            byte andmask = (byte) 0xff, ormask = 0;
            int dx0 = 1, dy0 = 1;
            int tx, ty;
            byte pen, map;
            int offset1 = 0;
            int offsety1;
            int xoffs;
            System.arraycopy(bb1, pen_data_offset, pen_data, 0, 0x40);
            if ((flags & Tilemap.TILE_FLIPY) != 0) {
                y0 += tileheight - 1;
                dy0 = -1;
            }
            if ((flags & Tilemap.TILE_FLIPX) != 0) {
                x0 += tilewidth - 1;
                dx0 = -1;
            }
            for (ty = 0; ty < tileheight; ty++) {
                xoffs = 0;
                offsety1 = y0;
                y0 += dy0;
                for (tx = 0; tx < tilewidth; tx++) {
                    pen = pen_data[offset1];
                    map = pen_to_flags[group][pen];
                    offset1++;
                    pixmap[(offsety1 % width) * width + x0 + xoffs] = (short) (palette_base + pen);
                    flagsmap[offsety1 % width][x0 + xoffs] = map;
                    andmask &= map;
                    ormask |= map;
                    xoffs += dx0;
                }
            }
            return (byte) (andmask ^ ormask);
        }
    }

//#region Input

    public static void loop_inputports_taitob_common() {

    }

    public static void loop_inputports_taitob_pbobble() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            dswb &= (byte) ~0x10;
        } else {
            dswb |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            dswb &= (byte) ~0x20;
        } else {
            dswb |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte0 &= ~0x20;
        } else {
            sbyte0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            sbyte2 &= ~0x02;
        } else {
            sbyte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte2 &= ~0x01;
        } else {
            sbyte2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x01;
        } else {
            sbyte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            sbyte1 &= ~0x40;
        } else {
            sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
        }
    }

    public static void loop_inputports_taitob_silentd() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            sbyte2 &= ~0x02;
        } else {
            sbyte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte2 &= ~0x01;
        } else {
            sbyte2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte0 &= ~0x08;
        } else {
            sbyte0 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            sbyte0 &= ~0x20;
        } else {
            sbyte0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte1 &= ~0x01;
        } else {
            sbyte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
    }

    public static void record_port() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbyte3 != sbyte3_old || sbyte4 != sbyte4_old || sbyte5 != sbyte5_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbyte3_old = sbyte3;
            sbyte4_old = sbyte4;
            sbyte5_old = sbyte5;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbyte3);
            Mame.bwRecord.write(sbyte4);
            Mame.bwRecord.write(sbyte5);
        }
    }

    public static void replay_port() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
                sbyte3_old = Mame.brRecord.readSByte();
                sbyte4_old = Mame.brRecord.readSByte();
                sbyte5_old = Mame.brRecord.readSByte();
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
                //Mame.mame_pause(true);
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbyte3 = sbyte3_old;
            sbyte4 = sbyte4_old;
            sbyte5 = sbyte5_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void record_port_pbobble() {
        if (dswb != dswb_old || sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbyte3 != sbyte3_old || sbyte4 != sbyte4_old || sbyte5 != sbyte5_old) {
            dswb_old = dswb;
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbyte3_old = sbyte3;
            sbyte4_old = sbyte4;
            sbyte5_old = sbyte5;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(dswb);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbyte3);
            Mame.bwRecord.write(sbyte4);
            Mame.bwRecord.write(sbyte5);
        }
    }

    public static void replay_port_pbobble() throws IOException {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                dswb_old = Mame.brRecord.readByte();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
                sbyte3_old = Mame.brRecord.readSByte();
                sbyte4_old = Mame.brRecord.readSByte();
                sbyte5_old = Mame.brRecord.readSByte();
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
                //Mame.mame_pause(true);
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            dswb = dswb_old;
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbyte3 = sbyte3_old;
            sbyte4 = sbyte4_old;
            sbyte5 = sbyte5_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Memory

    public static byte sbyte0, sbyte1, sbyte2, sbyte3, sbyte4, sbyte5;
    public static byte sbyte0_old, sbyte1_old, sbyte2_old, sbyte3_old, sbyte4_old, sbyte5_old;

    public static byte MReadOpByte_pbobble(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x80_0000 && address <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x90_0000 && address <= 0x90_ffff) {
            result = Memory.mainram[address - 0x90_0000];
        }
        return result;
    }

    public static byte MReadByte_pbobble(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_ffff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (TC0180VCU_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) TC0180VCU_word_r(offset);
            }
        } else if (address >= 0x41_0000 && address <= 0x41_197f) {
            int offset = (address - 0x41_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (taitob_spriteram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) taitob_spriteram[offset];
            }
        } else if (address >= 0x41_1980 && address <= 0x41_37ff) {
            result = mainram2[address - 0x41_1980];
        } else if (address >= 0x41_3800 && address <= 0x41_3fff) {
            int offset = (address - 0x41_3800) / 2;
            if (address % 2 == 0) {
                result = (byte) (taitob_scroll[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) taitob_scroll[offset];
            }
        } else if (address >= 0x41_8000 && address <= 0x41_801f) {
            int offset = (address - 0x41_8000) / 2;
            if (address % 2 == 0) {
                result = (byte) (taitob_v_control_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) taitob_v_control_r(offset);
            }
        } else if (address >= 0x44_0000 && address <= 0x47_ffff) {
            int offset = (address - 0x44_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (TC0180VCU_framebuffer_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) TC0180VCU_framebuffer_word_r(offset);
            }
        } else if (address >= 0x50_0000 && address <= 0x50_000f) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (pbobble_input_bypass_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = 0;
            }
        } else if (address >= 0x50_0024 && address <= 0x50_0025) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x50_0026 && address <= 0x50_0027) {
            if (address % 2 == 0) {
                result = (byte) (eep_latch_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) eep_latch_r();
            }
        } else if (address >= 0x50_002e && address <= 0x50_002f) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte4;
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0001) {
            result = 0; // NOP
        } else if (address >= 0x70_0002 && address <= 0x70_0003) {
            if (address % 2 == 0) {
                result = (byte) (Taitosnd.taitosound_comm16_msb_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Taitosnd.taitosound_comm16_msb_r();
            }
        } else if (address >= 0x80_0000 && address <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x90_0000 && address <= 0x90_ffff) {
            result = Memory.mainram[address - 0x90_0000];
        }
        return result;
    }

    public static short MReadOpWord_pbobble(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x90_0000 && address + 1 <= 0x90_ffff) {
            result = (short) (Memory.mainram[address - 0x90_0000] * 0x100 + Memory.mainram[address - 0x90_0000 + 1]);
        }
        return result;
    }

    public static short MReadWord_pbobble(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_ffff) {
            int offset = (address - 0x40_0000) / 2;
            result = TC0180VCU_word_r(offset);
        } else if (address >= 0x41_0000 && address + 1 <= 0x41_197f) {
            int offset = (address - 0x41_0000) / 2;
            result = taitob_spriteram[offset];
        } else if (address >= 0x41_1980 && address + 1 <= 0x41_37ff) {
            int offset = address - 0x41_0000;
            result = (short) (mainram2[offset] * 0x100 + mainram2[offset + 1]);
        } else if (address >= 0x41_3800 && address <= 0x41_3fff) {
            int offset = (address - 0x41_3800) / 2;
            result = taitob_scroll[offset];
        } else if (address >= 0x41_8000 && address + 1 <= 0x41_801f) {
            int offset = (address - 0x41_8000) / 2;
            result = taitob_v_control_r(offset);
        } else if (address >= 0x44_0000 && address + 1 <= 0x47_ffff) {
            int offset = (address - 0x44_0000) / 2;
            result = TC0180VCU_framebuffer_word_r(offset);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_000f) {
            int offset = (address - 0x50_0000) / 2;
            result = pbobble_input_bypass_r(offset);
        } else if (address >= 0x50_0024 && address + 1 <= 0x50_0025) {
            result = sbyte3;
        } else if (address >= 0x50_0026 && address + 1 <= 0x50_0027) {
            result = eep_latch_r();
        } else if (address >= 0x50_002e && address + 1 <= 0x50_002f) {
            result = sbyte4;
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            result = 0; // NOP
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0003) {
            result = Taitosnd.taitosound_comm16_msb_r();
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x90_0000 && address + 1 <= 0x90_ffff) {
            result = (short) (Memory.mainram[address - 0x90_0000] * 0x100 + Memory.mainram[address - 0x90_0000 + 1]);
        }
        return result;
    }

    public static int MReadOpLong_pbobble(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x90_0000 && address + 3 <= 0x90_ffff) {
            result = Memory.mainram[address - 0x90_0000] * 0x100_0000 + Memory.mainram[address - 0x90_0000 + 1] * 0x1_0000 + Memory.mainram[address - 0x90_0000 + 2] * 0x100 + Memory.mainram[address - 0x90_0000 + 3];
        }
        return result;
    }

    public static int MReadLong_pbobble(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_ffff) {
            int offset = (address - 0x40_0000) / 2;
            result = TC0180VCU_word_r(offset) * 0x1_0000 + TC0180VCU_word_r(offset + 1);
        } else if (address >= 0x41_0000 && address + 1 <= 0x41_197f) {
            int offset = (address - 0x41_0000) / 2;
            result = taitob_spriteram[offset] * 0x1_0000 + taitob_spriteram[offset + 1];
        } else if (address >= 0x41_1980 && address <= 0x41_37ff) {
            int offset = address - 0x41_1980;
            result = mainram2[offset] * 0x100_0000 + mainram2[offset + 1] * 0x1_0000 + mainram2[offset + 2] * 0x100 + mainram2[offset + 3];
        } else if (address >= 0x41_3800 && address <= 0x41_3fff) {
            int offset = (address - 0x41_3800) / 2;
            result = taitob_scroll[offset] * 0x1_0000 + taitob_scroll[offset + 1];
        } else if (address >= 0x41_8000 && address + 1 <= 0x41_801f) {
            int offset = (address - 0x41_8000) / 2;
            result = taitob_v_control_r(offset) * 0x1_0000 + taitob_v_control_r(offset + 1);
        } else if (address >= 0x44_0000 && address + 1 <= 0x47_ffff) {
            int offset = (address - 0x44_0000) / 2;
            result = TC0180VCU_framebuffer_word_r(offset) * 0x1_0000 + TC0180VCU_framebuffer_word_r(offset + 1);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_000f) {
            int offset = (address - 0x50_0000) / 2;
            result = pbobble_input_bypass_r(offset) * 0x1_0000 + pbobble_input_bypass_r(offset + 1);
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x90_0000 && address + 3 <= 0x90_ffff) {
            result = Memory.mainram[address - 0x90_0000] * 0x100_0000 + Memory.mainram[address - 0x90_0000 + 1] * 0x1_0000 + Memory.mainram[address - 0x90_0000 + 2] * 0x100 + Memory.mainram[address - 0x90_0000 + 3];
        }
        return result;
    }

    public static void MWriteByte_pbobble(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = value;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_ffff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                TC0180VCU_word_w1(offset, value);
            } else if (address % 2 == 1) {
                TC0180VCU_word_w2(offset, value);
            }
        } else if (address >= 0x41_0000 && address <= 0x41_197f) {
            int offset = (address - 0x41_0000) / 2;
            if (address % 2 == 0) {
                taitob_spriteram[offset] = (short) ((value << 8) | (taitob_spriteram[offset] & 0xff));
            } else if (address % 2 == 1) {
                taitob_spriteram[offset] = (short) ((taitob_spriteram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x41_1980 && address <= 0x41_37ff) {
            int offset = address - 0x41_1980;
            mainram2[offset] = value;
        } else if (address >= 0x41_3800 && address <= 0x41_3fff) {
            int offset = (address - 0x41_3800) / 2;
            if (address % 2 == 0) {
                taitob_scroll[offset] = (short) ((value << 8) | (taitob_scroll[offset] & 0xff));
            } else if (address % 2 == 1) {
                taitob_scroll[offset] = (short) ((taitob_scroll[offset] & 0xff00) | value);
            }
        } else if (address >= 0x41_8000 && address <= 0x41_801f) {
            int offset = (address - 0x41_8000) / 2;
            if (address % 2 == 0) {
                taitob_v_control_w1(offset, value);
            } else if (address % 2 == 1) {
                taitob_v_control_w2(offset, value);
            }
        } else if (address >= 0x44_0000 && address <= 0x47_ffff) {
            int offset = (address - 0x44_0000) / 2;
            if (address % 2 == 0) {
                TC0180VCU_framebuffer_word_w1(offset, value);
            } else if (address % 2 == 1) {
                TC0180VCU_framebuffer_word_w2(offset, value);
            }
        } else if (address >= 0x50_0000 && address <= 0x50_000f) {
            int offset = (address - 0x50_0000) / 2;
            TC0640FIO_halfword_byteswap_w1(offset, value);
        } else if (address >= 0x50_0026 && address <= 0x50_0027) {
            if (address % 2 == 0) {
                eeprom_w1(value);
            } else if (address % 2 == 1) {
                eeprom_w2(value);
            }
        } else if (address >= 0x50_0028 && address <= 0x50_0029) {
            player_34_coin_ctrl_w(value);
        } else if (address >= 0x60_0000 && address <= 0x60_0003) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                gain_control_w1(offset, value);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0001) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_port16_msb_w1(value);
            }
        } else if (address >= 0x70_0002 && address <= 0x70_0003) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_comm16_msb_w1(value);
            }
        } else if (address >= 0x80_0000 && address <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w2(offset, value);
            }
        } else if (address >= 0x90_0000 && address <= 0x90_ffff) {
            int offset = address - 0x90_0000;
            Memory.mainram[offset] = value;
        }
    }

    public static void MWriteWord_pbobble(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 8);
                Memory.mainrom[address + 1] = (byte) value;
            }
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_ffff) {
            int offset = (address - 0x40_0000) / 2;
            TC0180VCU_word_w(offset, value);
        } else if (address >= 0x41_0000 && address + 1 <= 0x41_197f) {
            int offset = (address - 0x41_0000) / 2;
            taitob_spriteram[offset] = value;
        } else if (address >= 0x41_1980 && address + 1 <= 0x41_37ff) {
            int offset = address - 0x41_1980;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0x41_3800 && address + 1 <= 0x41_3fff) {
            int offset = (address - 0x41_3800) / 2;
            taitob_scroll[offset] = value;
        } else if (address >= 0x41_8000 && address + 1 <= 0x41_801f) {
            int offset = (address - 0x41_8000) / 2;
            taitob_v_control_w(offset, value);
        } else if (address >= 0x44_0000 && address + 1 <= 0x47_ffff) {
            int offset = (address - 0x44_0000) / 2;
            TC0180VCU_framebuffer_word_w(offset, value);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_000f) {
            int offset = (address - 0x50_0000) / 2;
            TC0640FIO_halfword_byteswap_w(offset, value);
        } else if (address >= 0x50_0026 && address + 1 <= 0x50_0027) {
            eeprom_w(value);
        } else if (address >= 0x50_0028 && address + 1 <= 0x50_0029) {
            player_34_coin_ctrl_w(value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0003) {
            int offset = (address - 0x60_0000) / 2;
            gain_control_w(offset, value);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            Taitosnd.taitosound_port16_msb_w(value);
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0003) {
            Taitosnd.taitosound_comm16_msb_w(value);
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w(offset, value);
        } else if (address >= 0x90_0000 && address + 1 <= 0x90_ffff) {
            int offset = address - 0x90_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        }
    }

    public static void MWriteLong_pbobble(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 24);
                Memory.mainrom[address + 1] = (byte) (value >> 16);
                Memory.mainrom[address + 2] = (byte) (value >> 8);
                Memory.mainrom[address + 3] = (byte) value;
            }
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_ffff) {
            int offset = (address - 0x40_0000) / 2;
            TC0180VCU_word_w(offset, (short) (value >> 16));
            TC0180VCU_word_w(offset + 1, (short) value);
        } else if (address >= 0x41_0000 && address + 3 <= 0x41_197f) {
            int offset = (address - 0x41_0000) / 2;
            taitob_spriteram[offset] = (short) (value >> 16);
            taitob_spriteram[offset + 1] = (short) value;
        } else if (address >= 0x41_1980 && address + 3 <= 0x41_37ff) {
            int offset = address - 0x41_1980;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0x41_3800 && address + 3 <= 0x41_3fff) {
            int offset = (address - 0x41_3800) / 2;
            taitob_scroll[offset] = (short) (value >> 16);
            taitob_scroll[offset + 1] = (short) value;
        } else if (address >= 0x41_8000 && address + 3 <= 0x41_801f) {
            int offset = (address - 0x41_8000) / 2;
            taitob_v_control_w(offset, (short) (value >> 16));
            taitob_v_control_w(offset + 1, (short) value);
        } else if (address >= 0x44_0000 && address + 3 <= 0x47_ffff) {
            int offset = (address - 0x44_0000) / 2;
            TC0180VCU_framebuffer_word_w(offset, (short) (value >> 16));
            TC0180VCU_framebuffer_word_w(offset + 1, (short) value);
        } else if (address >= 0x50_0000 && address + 3 <= 0x50_000f) {
            int offset = (address - 0x50_0000) / 2;
            TC0640FIO_halfword_byteswap_w(offset, (short) (value >> 16));
            TC0640FIO_halfword_byteswap_w(offset + 1, (short) value);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_0003) {
            int offset = (address - 0x60_0000) / 2;
            gain_control_w(offset, (short) (value >> 16));
            gain_control_w(offset + 1, (short) value);
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_1fff) {
            int offset = (address - 0x80_0000) / 2;
            Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w(offset + 1, (short) value);
        } else if (address >= 0x90_0000 && address + 3 <= 0x90_ffff) {
            int offset = address - 0x90_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        }
    }

    public static byte ZReadOp(short address) {
        byte result = 0;
        if (address <= 0x3fff) {
            result = Memory.audiorom[address & 0x7fff];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory(short address) {
        byte result = 0;
        if (address <= 0x3fff) {
            result = Memory.audiorom[address & 0x7fff];
        } else if (address >= 0x4000 && address <= 0x7fff) {
            result = Memory.audiorom[basebanksnd + (address & 0x3fff)];
        } else if (address >= 0xc000 && address <= 0xdfff) {
            result = Memory.audioram[address - 0xc000];
        } else if (address >= 0xe000 && address <= 0xe000) {
            result = YM2610.F2610.ym2610_read(0);
        } else if (address >= 0xe001 && address <= 0xe001) {
            result = YM2610.F2610.ym2610_read(1);
        } else if (address >= 0xe002 && address <= 0xe002) {
            result = YM2610.F2610.ym2610_read(2);
        } else if (address >= 0xe200 && address <= 0xe200) {

        } else if (address >= 0xe201 && address <= 0xe201) {
            result = Taitosnd.taitosound_slave_comm_r();
        } else if (address >= 0xea00 && address <= 0xea00) {

        }
        return result;
    }

    public static void ZWriteMemory(short address, byte value) {
        if (address <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if (address >= 0xc000 && address <= 0xdfff) {
            Memory.audioram[address - 0xc000] = value;
        } else if (address >= 0xe000 && address <= 0xe000) {
            YM2610.F2610.ym2610_write(0, value);
        } else if (address >= 0xe001 && address <= 0xe001) {
            YM2610.F2610.ym2610_write(1, value);
        } else if (address >= 0xe002 && address <= 0xe002) {
            YM2610.F2610.ym2610_write(2, value);
        } else if (address >= 0xe003 && address <= 0xe003) {
            YM2610.F2610.ym2610_write(3, value);
        } else if (address >= 0xe200 && address <= 0xe200) {
            Taitosnd.taitosound_slave_port_w(value);
        } else if (address >= 0xe201 && address <= 0xe201) {
            Taitosnd.taitosound_slave_comm_w(value);
        } else if (address >= 0xe400 && address <= 0xe403) {

        } else if (address >= 0xe600 && address <= 0xe600) {

        } else if (address >= 0xee00 && address <= 0xee00) {

        } else if (address >= 0xf000 && address <= 0xf000) {

        } else if (address >= 0xf200 && address <= 0xf200) {
            bankswitch_w(value);
        }
    }

    public static byte ZReadHardware(short address) {
        byte result = 0;
        return result;
    }

    public static void ZWriteHardware(short address, byte value) {

    }

    public static int ZIRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[0].cpunum, 0);
    }

//#endregion

//#region Memory2

    public static byte MReadOpByte_silentd(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x30_0000 && address <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x40_0000 && address <= 0x40_3fff) {
            result = Memory.mainram[address - 0x40_0000];
        }
        return result;
    }

    public static byte MReadByte_silentd(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_0001) {
            result = 0;
        } else if (address >= 0x10_0002 && address <= 0x10_0003) {
            if (address % 2 == 0) {
                result = (byte) (Taitosnd.taitosound_comm16_msb_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Taitosnd.taitosound_comm16_msb_r();
            }
        } else if (address >= 0x20_0000 && address <= 0x20_000f) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = (byte) TC0220IOC_halfword_r(offset);
            }
        } else if (address >= 0x21_0000 && address <= 0x21_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x22_0000 && address <= 0x22_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte4;
            }
        } else if (address >= 0x23_0000 && address <= 0x23_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte5;
            }
        } else if (address >= 0x30_0000 && address <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x40_0000 && address <= 0x40_3fff) {
            result = Memory.mainram[address - 0x40_0000];
        } else if (address >= 0x50_0000 && address <= 0x50_ffff) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (TC0180VCU_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) TC0180VCU_word_r(offset);
            }
        } else if (address >= 0x51_0000 && address <= 0x51_197f) {
            int offset = (address - 0x51_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (taitob_spriteram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) taitob_spriteram[offset];
            }
        } else if (address >= 0x51_1980 && address <= 0x51_37ff) {
            result = mainram2[address - 0x51_1980];
        } else if (address >= 0x51_3800 && address <= 0x51_3fff) {
            int offset = (address - 0x51_3800) / 2;
            if (address % 2 == 0) {
                result = (byte) (taitob_scroll[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) taitob_scroll[offset];
            }
        } else if (address >= 0x51_8000 && address <= 0x51_801f) {
            int offset = (address - 0x51_8000) / 2;
            if (address % 2 == 0) {
                result = (byte) (taitob_v_control_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) taitob_v_control_r(offset);
            }
        } else if (address >= 0x54_0000 && address <= 0x57_ffff) {
            int offset = (address - 0x54_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (TC0180VCU_framebuffer_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) TC0180VCU_framebuffer_word_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_silentd(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x30_0000 && address + 1 <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_3fff) {
            result = (short) (Memory.mainram[address - 0x40_0000] * 0x100 + Memory.mainram[address - 0x40_0000 + 1]);
        }
        return result;
    }

    public static short MReadWord_silentd(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_0001) {
            result = 0;
        } else if (address >= 0x10_0002 && address + 1 <= 0x10_0003) {
            result = Taitosnd.taitosound_comm16_msb_r();
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_000f) {
            int offset = (address - 0x20_0000) / 2;
            result = TC0220IOC_halfword_r(offset);
        } else if (address >= 0x21_0000 && address + 1 <= 0x21_0001) {
            result = sbyte3;
        } else if (address >= 0x22_0000 && address + 1 <= 0x22_0001) {
            result = sbyte4;
        } else if (address >= 0x23_0000 && address + 1 <= 0x23_0001) {
            result = sbyte5;
        } else if (address >= 0x30_0000 && address + 1 <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_3fff) {
            result = (short) (Memory.mainram[address - 0x40_0000] * 0x100 + Memory.mainram[address - 0x40_0000 + 1]);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_ffff) {
            int offset = (address - 0x50_0000) / 2;
            result = TC0180VCU_word_r(offset);
        } else if (address >= 0x51_0000 && address + 1 <= 0x51_197f) {
            int offset = (address - 0x51_0000) / 2;
            result = taitob_spriteram[offset];
        } else if (address >= 0x51_1980 && address + 1 <= 0x51_37ff) {
            result = (short) (mainram2[address - 0x51_1980] * 0x100 + mainram2[address - 0x51_1980 + 1]);
        } else if (address >= 0x51_3800 && address + 1 <= 0x51_3fff) {
            int offset = (address - 0x51_3800) / 2;
            result = taitob_scroll[offset];
        } else if (address >= 0x51_8000 && address + 1 <= 0x51_801f) {
            int offset = (address - 0x51_8000) / 2;
            result = taitob_v_control_r(offset);
        } else if (address >= 0x54_0000 && address + 1 <= 0x57_ffff) {
            int offset = (address - 0x54_0000) / 2;
            result = TC0180VCU_framebuffer_word_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_silentd(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x30_0000 && address + 3 <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_3fff) {
            result = Memory.mainram[address - 0x40_0000] * 0x100_0000 + Memory.mainram[address - 0x40_0000 + 1] * 0x1_0000 + Memory.mainram[address - 0x40_0000 + 2] * 0x100 + Memory.mainram[address - 0x40_0000 + 3];
        }
        return result;
    }

    public static int MReadLong_silentd(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_000f) {
            int offset = (address - 0x20_0000) / 2;
            result = TC0220IOC_halfword_r(offset) * 0x1_0000 + TC0220IOC_halfword_r(offset + 1);
        } else if (address >= 0x30_0000 && address + 1 <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_3fff) {
            result = Memory.mainram[address - 0x40_0000] * 0x100_0000 + Memory.mainram[address - 0x40_0000 + 1] * 0x1_0000 + Memory.mainram[address - 0x40_0000 + 2] * 0x100 + Memory.mainram[address - 0x40_0000 + 3];
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_ffff) {
            int offset = (address - 0x50_0000) / 2;
            result = TC0180VCU_word_r(offset) * 0x1_0000 + TC0180VCU_word_r(offset + 1);
        } else if (address >= 0x51_0000 && address + 1 <= 0x51_197f) {
            int offset = (address - 0x51_0000) / 2;
            result = taitob_spriteram[offset] * 0x1_0000 + taitob_spriteram[offset + 1];
        } else if (address >= 0x51_1980 && address + 1 <= 0x51_37ff) {
            result = mainram2[address - 0x51_1980] * 0x100_0000 + mainram2[address - 0x51_1980 + 1] * 0x1_0000 + mainram2[address - 0x51_1980 + 2] * 0x100 + mainram2[address - 0x51_1980 + 3];
        } else if (address >= 0x51_3800 && address + 1 <= 0x51_3fff) {
            int offset = (address - 0x51_3800) / 2;
            result = taitob_scroll[offset] * 0x1_0000 + taitob_scroll[offset + 1];
        } else if (address >= 0x51_8000 && address + 1 <= 0x51_801f) {
            int offset = (address - 0x51_8000) / 2;
            result = taitob_v_control_r(offset) * 0x1_0000 + taitob_v_control_r(offset + 1);
        } else if (address >= 0x54_0000 && address + 1 <= 0x57_ffff) {
            int offset = (address - 0x54_0000) / 2;
            result = TC0180VCU_framebuffer_word_r(offset) * 0x1_0000 + TC0180VCU_framebuffer_word_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_silentd(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = value;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_0001) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_port16_msb_w1(value);
            }
        } else if (address >= 0x10_0002 && address <= 0x10_0003) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_comm16_msb_w1(value);
            }
        } else if (address >= 0x20_0000 && address <= 0x20_000f) {
            int offset = (address - 0x20_0000) / 2;
            TC0220IOC_halfword_w1(offset, value);
        } else if (address >= 0x24_0000 && address <= 0x24_0001) {

        } else if (address >= 0x30_0000 && address <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w2(offset, value);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_3fff) {
            int offset = address - 0x40_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x50_0000 && address <= 0x50_ffff) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {
                TC0180VCU_word_w1(offset, value);
            } else if (address % 2 == 1) {
                TC0180VCU_word_w2(offset, value);
            }
        } else if (address >= 0x51_0000 && address <= 0x51_197f) {
            int offset = (address - 0x51_0000) / 2;
            if (address % 2 == 0) {
                taitob_spriteram[offset] = (short) ((value << 8) | (taitob_spriteram[offset] & 0xff));
            } else if (address % 2 == 1) {
                taitob_spriteram[offset] = (short) ((taitob_spriteram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x51_1980 && address <= 0x51_37ff) {
            int offset = address - 0x51_1980;
            mainram2[offset] = value;
        } else if (address >= 0x51_3800 && address <= 0x51_3fff) {
            int offset = (address - 0x51_3800) / 2;
            if (address % 2 == 0) {
                taitob_scroll[offset] = (short) ((value << 8) | (taitob_scroll[offset] & 0xff));
            } else if (address % 2 == 1) {
                taitob_scroll[offset] = (short) ((taitob_scroll[offset] & 0xff00) | value);
            }
        } else if (address >= 0x51_8000 && address <= 0x51_801f) {
            int offset = (address - 0x51_8000) / 2;
            if (address % 2 == 0) {
                taitob_v_control_w1(offset, value);
            } else if (address % 2 == 1) {
                taitob_v_control_w2(offset, value);
            }
        } else if (address >= 0x54_0000 && address <= 0x57_ffff) {
            int offset = (address - 0x54_0000) / 2;
            if (address % 2 == 0) {
                TC0180VCU_framebuffer_word_w1(offset, value);
            } else if (address % 2 == 1) {
                TC0180VCU_framebuffer_word_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_silentd(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 8);
                Memory.mainrom[address + 1] = (byte) value;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_0001) {
            Taitosnd.taitosound_port16_msb_w((byte) value);
        } else if (address >= 0x10_0002 && address + 1 <= 0x10_0003) {
            Taitosnd.taitosound_comm16_msb_w(value);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_000f) {
            int offset = (address - 0x20_0000) / 2;
            TC0220IOC_halfword_w(offset, value);
        } else if (address >= 0x24_0000 && address + 1 <= 0x24_0001) {

        } else if (address >= 0x30_0000 && address + 1 <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_3fff) {
            int offset = address - 0x40_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_ffff) {
            int offset = (address - 0x50_0000) / 2;
            TC0180VCU_word_w(offset, value);
        } else if (address >= 0x51_0000 && address + 1 <= 0x51_197f) {
            int offset = (address - 0x51_0000) / 2;
            taitob_spriteram[offset] = value;
        } else if (address >= 0x51_1980 && address + 1 <= 0x51_37ff) {
            int offset = address - 0x51_1980;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0x51_3800 && address + 1 <= 0x51_3fff) {
            int offset = (address - 0x51_3800) / 2;
            taitob_scroll[offset] = value;
        } else if (address >= 0x51_8000 && address + 1 <= 0x51_801f) {
            int offset = (address - 0x51_8000) / 2;
            taitob_v_control_w(offset, value);
        } else if (address >= 0x54_0000 && address + 1 <= 0x57_ffff) {
            int offset = (address - 0x54_0000) / 2;
            TC0180VCU_framebuffer_word_w(offset, value);
        }
    }

    public static void MWriteLong_silentd(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 24);
                Memory.mainrom[address + 1] = (byte) (value >> 16);
                Memory.mainrom[address + 2] = (byte) (value >> 8);
                Memory.mainrom[address + 3] = (byte) value;
            }
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_000f) {
            int offset = (address - 0x20_0000) / 2;
            TC0220IOC_halfword_w(offset, (short) (value >> 16));
            TC0220IOC_halfword_w(offset + 1, (short) value);
        } else if (address >= 0x30_0000 && address + 3 <= 0x30_1fff) {
            int offset = (address - 0x30_0000) / 2;
            Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_RRRRGGGGBBBBRGBx_word_w(offset + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_3fff) {
            int offset = address - 0x40_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x50_0000 && address + 3 <= 0x50_ffff) {
            int offset = (address - 0x50_0000) / 2;
            TC0180VCU_word_w(offset, (short) (value >> 16));
            TC0180VCU_word_w(offset + 1, (short) value);
        } else if (address >= 0x51_0000 && address + 3 <= 0x51_197f) {
            int offset = (address - 0x51_0000) / 2;
            taitob_spriteram[offset] = (short) (value >> 16);
            taitob_spriteram[offset + 1] = (short) value;
        } else if (address >= 0x51_1980 && address + 3 <= 0x51_37ff) {
            int offset = address - 0x51_1980;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0x51_3800 && address + 3 <= 0x51_3fff) {
            int offset = (address - 0x51_3800) / 2;
            taitob_scroll[offset] = (short) (value >> 16);
            taitob_scroll[offset + 1] = (short) value;
        } else if (address >= 0x51_8000 && address + 3 <= 0x51_801f) {
            int offset = (address - 0x51_8000) / 2;
            taitob_v_control_w(offset, (short) (value >> 16));
            taitob_v_control_w(offset + 1, (short) value);
        } else if (address >= 0x54_0000 && address + 3 <= 0x57_ffff) {
            int offset = (address - 0x54_0000) / 2;
            TC0180VCU_framebuffer_word_w(offset, (short) (value >> 16));
            TC0180VCU_framebuffer_word_w(offset + 1, (short) value);
        }
    }

//#endregion

//#region Mb87078

    public static class MB87078 {

        public int[] gain;        /* gain index 0-63,64,65 */
        public int channel_latch;    /* current channel */
        public byte[] latch;    /* 6bit+3bit 4 data latches */
        public byte reset_comp;
    }

    public static MB87078[] c;
    public static final int[] MB87078_gain_percent = new int[] {
            100, 94, 89, 84, 79, 74, 70, 66,
            63, 59, 56, 53, 50, 47, 44, 42,
            39, 37, 35, 33, 31, 29, 28, 26,
            25, 23, 22, 21, 19, 18, 17, 16,
            15, 14, 14, 13, 12, 11, 11, 10,
            10, 9, 8, 8, 7, 7, 7, 6,
            6, 5, 5, 5, 5, 4, 4, 4,
            3, 3, 3, 3, 3, 2, 2, 2,
            2, 0
    };

    public static int calc_gain_index(int data0, int data1) {
        if ((data1 & 4) == 0) {
            return 65;
        } else {
            if ((data1 & 16) != 0) {
                return 64;
            } else {
                if ((data1 & 8) != 0) {
                    return 0;
                } else {
                    return (data0 ^ 0x3f);
                }
            }
        }
    }

    public static void gain_recalc(int which) {
        int i;
        for (i = 0; i < 4; i++) {
            int old_index = c[which].gain[i];
            c[which].gain[i] = calc_gain_index(c[which].latch[i], c[which].latch[4 + i]);
            if (old_index != c[which].gain[i]) {
                mb87078_gain_changed(i, MB87078_gain_percent[c[which].gain[i]]);
            }
        }
    }

    public static void MB87078_start(int which) {
        c = new MB87078[1];
        c[0] = new MB87078();
        c[0].gain = new int[4];
        c[0].latch = new byte[8];
        if (which >= 4) {
            return;
        }
        MB87078_reset_comp_w(which, 0);
        MB87078_reset_comp_w(which, 1);
    }

    public static void MB87078_reset_comp_w(int which, int level) {
        c[which].reset_comp = (byte) level;
        if (level == 0) {
            c[which].latch[0] = 0x3f;
            c[which].latch[1] = 0x3f;
            c[which].latch[2] = 0x3f;
            c[which].latch[3] = 0x3f;
            c[which].latch[4] = 0x0 | 0x4;
            c[which].latch[5] = 0x1 | 0x4;
            c[which].latch[6] = 0x2 | 0x4;
            c[which].latch[7] = 0x3 | 0x4;
        }
        gain_recalc(which);
    }

    public static void MB87078_data_w(int which, int data, int dsel) {
        if (c[which].reset_comp == 0) {
            return;
        }
        if (dsel == 0) {
            c[which].latch[0 + c[which].channel_latch] = (byte) (data & 0x3f);
        } else {
            c[which].channel_latch = data & 3;
            c[which].latch[4 + c[which].channel_latch] = (byte) (data & 0x1f);
        }
        gain_recalc(which);
    }

//#endregion

//#region State

    public static void SaveStateBinary(BinaryWriter writer) {
        //pixel_scroll
        int i;
        writer.write(dswa);
        writer.write(dswb);
        writer.write(basebanksnd);
        writer.write(eep_latch);
        writer.write(coin_word);
        for (i = 0; i < 0x1000; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x400; i++) {
            writer.write(taitob_scroll[i]);
        }
        for (i = 0; i < 0x8000; i++) {
            writer.write(TC0180VCU_ram[i]);
        }
        for (i = 0; i < 0x10; i++) {
            writer.write(TC0180VCU_ctrl[i]);
        }
        writer.write(TC0220IOC_regs, 0, 8);
        writer.write(TC0220IOC_port);
        writer.write(TC0640FIO_regs, 0, 8);
        for (i = 0; i < 0xcc0; i++) {
            writer.write(taitob_spriteram[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(bg_rambank[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(fg_rambank[i]);
        }
        writer.write(tx_rambank);
        writer.write(video_control);
        for (i = 0; i < 0x1000; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1_0000);
        writer.write(mainram2, 0, 0x1e80);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x2000);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        writer.write(Video.screenstate.vblank_start_time.seconds);
        writer.write(Video.screenstate.vblank_start_time.attoseconds);
        writer.write(Video.screenstate.frame_number);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        AY8910.AA8910[0].SaveStateBinary(writer);
        YM2610.F2610.SaveStateBinary(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(AY8910.AA8910[0].stream.gain);
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(Sound.ym2610stream.output_sampindex);
        writer.write(Sound.ym2610stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        for (i = 0; i < 4; i++) {
            writer.write(c[0].gain[i]);
        }
        writer.write(c[0].channel_latch);
        for (i = 0; i < 8; i++) {
            writer.write(c[0].latch[i]);
        }
        writer.write(c[0].reset_comp);
        Eeprom.SaveStateBinary(writer);
        Taitosnd.SaveStateBinary(writer);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i;
            dswa = reader.readByte();
            dswb = reader.readByte();
            basebanksnd = reader.readInt32();
            eep_latch = reader.readUInt16();
            coin_word = reader.readUInt16();
            for (i = 0; i < 0x1000; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x400; i++) {
                taitob_scroll[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x8000; i++) {
                TC0180VCU_ram[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x10; i++) {
                TC0180VCU_ctrl[i] = reader.readUInt16();
            }
            TC0220IOC_regs = reader.readBytes(8);
            TC0220IOC_port = reader.readByte();
            TC0640FIO_regs = reader.readBytes(8);
            for (i = 0; i < 0xcc0; i++) {
                taitob_spriteram[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                bg_rambank[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                fg_rambank[i] = reader.readUInt16();
            }
            tx_rambank = reader.readUInt16();
            video_control = reader.readByte();
            for (i = 0; i < 0x1000; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1_0000);
            mainram2 = reader.readBytes(0x1e80);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x2000);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.screenstate.vblank_start_time.seconds = reader.readInt32();
            Video.screenstate.vblank_start_time.attoseconds = reader.readInt64();
            Video.screenstate.frame_number = reader.readInt64();
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            AY8910.AA8910[0].LoadStateBinary(reader);
            YM2610.F2610.LoadStateBinary(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            AY8910.AA8910[0].stream.gain = reader.readInt32();
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            Sound.ym2610stream.output_sampindex = reader.readInt32();
            Sound.ym2610stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            for (i = 0; i < 4; i++) {
                c[0].gain[i] = reader.readInt32();
            }
            c[0].channel_latch = reader.readInt32();
            c[0].latch = reader.readBytes(8);
            c[0].reset_comp = reader.readByte();
            Eeprom.LoadStateBinary(reader);
            Taitosnd.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion
}
