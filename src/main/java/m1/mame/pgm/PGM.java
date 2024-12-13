/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.pgm;


import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoField;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import m1.cpu.m68000.MC68000;
import m1.cpu.z80.Z80A;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Generic;
import m1.emu.Inptport;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.Memory;
import m1.emu.Palette;
import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Tilemap.trans_t;
import m1.emu.Timer;
import m1.emu.Video;
import m1.sound.ICS2115;
import m1.sound.Sound;


public class PGM {

    public static byte[] mainbiosrom, videobios, audiobios;
    public static byte[] pgm_bg_videoram, pgm_tx_videoram, pgm_rowscrollram, pgm_videoregs, sprmaskrom, sprcolrom, tilesrom, tiles1rom, tiles2rom, pgm_sprite_a_region;
    public static byte CalVal, CalMask, CalCom = 0, CalCnt = 0;
    public static int[] arm7_shareram;
    public static int arm7_latch;
    public static int pgm_sprite_a_region_allocate;

    public static void PGMInit() {
        Machine.bRom = true;
        mainbiosrom = null; // Properties.Resources.pgmmainbios;
        videobios = null; // Properties.Resources.pgmvideobios;
        audiobios = null; // Properties.Resources.pgmaudiobios;
        ICS2115.icsrom = audiobios;
        byte[] bb1, bb2;
        int i3, n1, n2, n3;
        bb1 = Machine.GetRom("ics.rom");
        bb2 = Machine.GetRom("tiles.rom");
        if (bb1 == null) {
            bb1 = new byte[0];
        }
        n1 = bb1.length;
        n2 = bb2.length;
        ICS2115.icsrom = new byte[0x40_0000 + n1];
        System.arraycopy(audiobios, 0, ICS2115.icsrom, 0, 0x20_0000);
        System.arraycopy(bb1, 0, ICS2115.icsrom, 0x40_0000, n1);
        tilesrom = new byte[0x40_0000 + n2];
        System.arraycopy(videobios, 0, tilesrom, 0, 0x20_0000);
        System.arraycopy(bb2, 0, tilesrom, 0x40_0000, n2);
        n3 = tilesrom.length;
        tiles1rom = new byte[n3 * 2];
        for (i3 = 0; i3 < n3; i3++) {
            tiles1rom[i3 * 2] = (byte) (tilesrom[i3] & 0x0f);
            tiles1rom[i3 * 2 + 1] = (byte) (tilesrom[i3] >> 4);
        }
        Memory.mainrom = Machine.GetRom("maincpu.rom");
        sprmaskrom = Machine.GetRom("sprmask.rom");
        sprcolrom = Machine.GetRom("sprcol.rom");
        expand_32x32x5bpp();
        expand_colourdata();
        Memory.mainram = new byte[0x2_0000];
        pgm_bg_videoram = new byte[0x4000];
        pgm_tx_videoram = new byte[0x2000];
        pgm_rowscrollram = new byte[0x800];
        Generic.paletteram16 = new short[0x900];
        pgm_videoregs = new byte[0x1_0000];
        Memory.audioram = new byte[0x1_0000];
        if (Memory.mainrom == null || sprmaskrom == null || pgm_sprite_a_region == null) {
            Machine.bRom = false;
        }
    }

    private static void expand_32x32x5bpp() {
        int n2 = tilesrom.length / 5 * 8;
        tiles2rom = new byte[n2];
        int cnt;
        byte pix;
        for (cnt = 0; cnt < tilesrom.length / 5; cnt++) {
            pix = (byte) ((tilesrom[0 + 5 * cnt] >> 0) & 0x1f);
            tiles2rom[0 + 8 * cnt] = pix;
            pix = (byte) (((tilesrom[0 + 5 * cnt] >> 5) & 0x07) | ((tilesrom[1 + 5 * cnt] << 3) & 0x18));
            tiles2rom[1 + 8 * cnt] = pix;
            pix = (byte) ((tilesrom[1 + 5 * cnt] >> 2) & 0x1f);
            tiles2rom[2 + 8 * cnt] = pix;
            pix = (byte) (((tilesrom[1 + 5 * cnt] >> 7) & 0x01) | ((tilesrom[2 + 5 * cnt] << 1) & 0x1e));
            tiles2rom[3 + 8 * cnt] = pix;
            pix = (byte) (((tilesrom[2 + 5 * cnt] >> 4) & 0x0f) | ((tilesrom[3 + 5 * cnt] << 4) & 0x10));
            tiles2rom[4 + 8 * cnt] = pix;
            pix = (byte) ((tilesrom[3 + 5 * cnt] >> 1) & 0x1f);
            tiles2rom[5 + 8 * cnt] = pix;
            pix = (byte) (((tilesrom[3 + 5 * cnt] >> 6) & 0x03) | ((tilesrom[4 + 5 * cnt] << 2) & 0x1c));
            tiles2rom[6 + 8 * cnt] = pix;
            pix = (byte) ((tilesrom[4 + 5 * cnt] >> 3) & 0x1f);
            tiles2rom[7 + 8 * cnt] = pix;
        }
    }

    private static void expand_colourdata() {
        int srcsize = sprcolrom.length;
        int cnt;
        int needed = srcsize / 2 * 3;
        int pgm_sprite_a_region_allocate = 1;
        int colpack;
        while (pgm_sprite_a_region_allocate < needed) {
            pgm_sprite_a_region_allocate = pgm_sprite_a_region_allocate << 1;
        }
        pgm_sprite_a_region = new byte[pgm_sprite_a_region_allocate];
        for (cnt = 0; cnt < srcsize / 2; cnt++) {
            colpack = sprcolrom[cnt * 2] | (sprcolrom[cnt * 2 + 1] << 8);
            pgm_sprite_a_region[cnt * 3 + 0] = (byte) ((colpack >> 0) & 0x1f);
            pgm_sprite_a_region[cnt * 3 + 1] = (byte) ((colpack >> 5) & 0x1f);
            pgm_sprite_a_region[cnt * 3 + 2] = (byte) ((colpack >> 10) & 0x1f);
        }
    }

    public static void machine_reset_pgm() {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_HALT.ordinal(), LineState.ASSERT_LINE);
        device_reset();
    }

    public static byte z80_ram_r(int offset) {
        return Memory.audioram[offset];
    }

    public static void z80_ram_w(int offset, byte data) {
        int pc = MC68000.m1.PC;
        Memory.audioram[offset] = data;
        if (pc != 0xf12 && pc != 0xde2 && pc != 0x10_0c50 && pc != 0x10_0b20) {
            //error
        }
    }

    public static void z80_reset_w(short data) {
        if (data == 0x5050) {
            ICS2115.ics2115_reset();
            Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_HALT.ordinal(), LineState.CLEAR_LINE);
            Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), LineState.PULSE_LINE);
        } else {
            Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_HALT.ordinal(), LineState.ASSERT_LINE);
        }
    }

    public static void z80_ctrl_w() {

    }

    public static void m68k_l1_w(byte data) {
        //if(ACCESSING_BITS_0_7)
        Sound.soundlatch_w(data);
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
    }

    public static void m68k_l1_w(short data) {
        Sound.soundlatch_w(data);
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
    }

    public static void z80_l3_w(byte data) {
        Sound.soundlatch3_w(data);
    }

    public static void sound_irq(int level) {
        Cpuint.cpunum_set_input_line(1, 0, LineState.values()[level]);
    }

    public static byte bcd(byte data) {
        return (byte) (((data / 10) << 4) | (data % 10));
    }

    public static byte pgm_calendar_r() {
        byte calr;
        calr = (byte) (((CalVal & CalMask) != 0) ? 1 : 0);
        CalMask <<= 1;
        return calr;
    }

    public static void pgm_calendar_w(short data) {
        //DateTime time = DateTime.Now;
        Instant time = Instant.parse("1970-01-01 08:00:00");
        CalCom <<= 1;
        CalCom |= (byte) (data & 1);
        ++CalCnt;
        if (CalCnt == 4) {
            CalMask = 1;
            CalVal = 1;
            CalCnt = 0;
            switch (CalCom & 0xf) {
                case 1:
                case 3:
                case 5:
                case 7:
                case 9:
                case 0xb:
                case 0xd:
                    CalVal++;
                    break;
                case 0:
                    CalVal = bcd((byte) time.get(ChronoField.DAY_OF_WEEK)); //??
                    break;
                case 2:  //Hours
                    CalVal = bcd((byte) time.get(ChronoField.HOUR_OF_DAY));
                    break;
                case 4:  //Seconds
                    CalVal = bcd((byte) time.get(ChronoField.SECOND_OF_MINUTE));
                    break;
                case 6:  //Month
                    CalVal = bcd((byte) (time.get(ChronoField.MONTH_OF_YEAR))); //?? not bcd in MVS
                    break;
                case 8:
                    CalVal = 0; //Controls blinking speed, maybe milliseconds
                    break;
                case 0xa: //Day
                    CalVal = bcd((byte) time.get(ChronoField.DAY_OF_MONTH));
                    break;
                case 0xc: //Minute
                    CalVal = bcd((byte) time.get(ChronoField.MINUTE_OF_HOUR));
                    break;
                case 0xe:  //year
                    CalVal = bcd((byte) (time.get(ChronoField.YEAR) % 100));
                    break;
                case 0xf:  //Load Date
                    //mame_get_base_datetime(machine, &systime);
                    break;
            }
        }
    }

    public static void nvram_handler_load_pgm() {
        if (Files.exists(Path.of("nvram", Machine.sName + ".nv"))) {
            FileStream fs1 = new FileStream("nvram\\" + Machine.sName + ".nv", FileMode.Open);
            int n = 0x2_0000;
            fs1.read(Memory.mainram, 0, n);
            fs1.close();
        }
    }

    public static void nvram_handler_save_pgm() {
        FileStream fs1 = new FileStream("nvram\\" + Machine.sName + ".nv", FileMode.Create);
        fs1.write(Memory.mainram, 0, 0x2_0000);
        fs1.close();
    }

//#region Machine

    public static int kb_game_id, kb_region;
    public static short kb_prot_hold, kb_prot_hilo, kb_prot_hilo_select;
    public static int kb_cmd, kb_reg, kb_ptr;
    public static byte kb_swap;
    public static short olds_bs, kb_cmd3;
    public static byte[][] kb_source_data;
    public static final byte[][] drgw2_source_data = new byte[][] { //[0xec]

            new byte[] {0,}, // Region 0, not used
            new byte[] {  // Region 1, $13A886
                    0x67, 0x51, (byte) 0xF3, 0x19, (byte) 0xA0, 0x11, (byte) 0xB1, 0x11, (byte) 0xB0, (byte) 0xEE, (byte) 0xE3, (byte) 0xF6, (byte) 0xBE, (byte) 0x81, 0x35, (byte) 0xE3,
                    (byte) 0xFB, (byte) 0xE6, (byte) 0xEF, (byte) 0xDF, 0x61, 0x01, (byte) 0xFA, 0x22, 0x5D, 0x43, 0x01, (byte) 0xA5, 0x3B, 0x17, (byte) 0xD4, 0x74,
                    (byte) 0xF0, (byte) 0xF4, (byte) 0xF3, 0x43, (byte) 0xB5, 0x19, 0x04, (byte) 0xD5, (byte) 0x84, (byte) 0xCE, (byte) 0x87, (byte) 0xFE, 0x35, 0x3E, (byte) 0xC4, 0x3C,
                    (byte) 0xC7, (byte) 0x85, 0x2A, 0x33, 0x00, (byte) 0x86, (byte) 0xD0, 0x4D, 0x65, 0x4B, (byte) 0xF9, (byte) 0xE9, (byte) 0xC0, (byte) 0xBA, (byte) 0xAA, 0x77,
                    (byte) 0x9E, 0x66, (byte) 0xF6, 0x0F, 0x4F, 0x3A, (byte) 0xB6, (byte) 0xF1, 0x64, (byte) 0x9A, (byte) 0xE9, 0x25, 0x1A, 0x5F, 0x22, (byte) 0xA3,
                    (byte) 0xA2, (byte) 0xBF, 0x4B, 0x77, 0x3F, 0x34, (byte) 0xC9, 0x6E, (byte) 0xDB, 0x12, 0x5C, 0x33, (byte) 0xA5, (byte) 0x8B, 0x6C, (byte) 0xB1,
                    0x74, (byte) 0xC8, 0x40, 0x4E, 0x2F, (byte) 0xE7, 0x46, (byte) 0xAE, (byte) 0x99, (byte) 0xFC, (byte) 0xB0, 0x55, 0x54, (byte) 0xDF, (byte) 0xA7, (byte) 0xA1,
                    0x0F, 0x5E, 0x49, (byte) 0xCF, 0x56, 0x3C, (byte) 0x90, 0x2B, (byte) 0xAC, 0x65, 0x6E, (byte) 0xDB, 0x58, 0x3E, (byte) 0xC9, 0x00,
                    (byte) 0xAE, 0x53, 0x4D, (byte) 0x92, (byte) 0xFA, 0x40, (byte) 0xB2, 0x6B, 0x65, 0x4B, (byte) 0x90, (byte) 0x8A, 0x0C, (byte) 0xE2, (byte) 0xA5, (byte) 0x9A,
                    (byte) 0xD0, 0x20, 0x29, 0x55, (byte) 0xA4, 0x44, (byte) 0xAC, 0x51, (byte) 0x87, 0x54, 0x53, 0x34, 0x24, 0x4B, (byte) 0x81, 0x67,
                    0x34, 0x4C, 0x5F, 0x31, 0x4E, (byte) 0xF2, (byte) 0xF1, 0x19, 0x18, 0x1C, 0x34, 0x38, (byte) 0xE1, (byte) 0x81, 0x17, (byte) 0xCF,
                    0x24, (byte) 0xB9, (byte) 0x9A, (byte) 0xCB, 0x34, 0x51, 0x50, 0x59, 0x44, (byte) 0xB1, 0x0B, 0x50, (byte) 0x95, 0x6C, 0x48, 0x7E,
                    0x14, (byte) 0xA4, (byte) 0xC6, (byte) 0xD9, (byte) 0xD3, (byte) 0xA5, (byte) 0xD6, (byte) 0xD0, (byte) 0xC5, (byte) 0x97, (byte) 0xF0, 0x45, (byte) 0xD0, (byte) 0x98, 0x51, (byte) 0x91,
                    (byte) 0x9F, (byte) 0xA3, 0x43, 0x51, 0x05, (byte) 0x90, (byte) 0xEE, (byte) 0xCA, 0x7E, 0x5F, 0x72, 0x53, (byte) 0xB1, (byte) 0xD3, (byte) 0xAF, 0x36,
                    0x08, 0x75, (byte) 0xB0, (byte) 0x9B, (byte) 0xE0, 0x0D, 0x43, (byte) 0x88, (byte) 0xAA, 0x27, 0x44, 0x11
            },
            new byte[] {0,}, // Region 2, not used
            new byte[] {0,}, // Region 3, not used
            new byte[] {0,}, // Region 4, not used
            new byte[] {  // Region 5, $13ab42 (drgw2c)
                    0x7F, 0x41, (byte) 0xF3, 0x39, (byte) 0xA0, 0x11, (byte) 0xA1, 0x11, (byte) 0xB0, (byte) 0xA2, 0x4C, 0x23, 0x13, (byte) 0xE9, 0x25, 0x3D,
                    0x0F, 0x72, 0x3A, (byte) 0x9D, (byte) 0xB5, (byte) 0x96, (byte) 0xD1, (byte) 0xDA, 0x07, 0x29, 0x41, (byte) 0x9A, (byte) 0xAD, 0x70, (byte) 0xBA, 0x46,
                    0x63, 0x2B, 0x7F, 0x3D, (byte) 0xBE, 0x40, (byte) 0xAD, (byte) 0xD4, 0x4C, 0x73, 0x27, 0x58, (byte) 0xA7, 0x65, (byte) 0xDC, (byte) 0xD6,
                    (byte) 0xFD, (byte) 0xDE, (byte) 0xB5, 0x6E, (byte) 0xD6, 0x6C, 0x75, 0x1A, 0x32, 0x45, (byte) 0xD5, (byte) 0xE3, 0x6A, 0x14, 0x6D, (byte) 0x80,
                    (byte) 0x84, 0x15, (byte) 0xAF, (byte) 0xCC, 0x7B, 0x61, 0x51, (byte) 0x82, 0x40, 0x53, 0x7F, 0x38, (byte) 0xA0, (byte) 0xD6, (byte) 0x8F, 0x61,
                    0x79, 0x19, (byte) 0xE5, (byte) 0x99, (byte) 0x84, (byte) 0xD8, 0x78, 0x27, 0x3F, 0x16, (byte) 0x97, 0x78, 0x4F, 0x7B, 0x0C, (byte) 0xA6,
                    0x37, (byte) 0xDB, (byte) 0xC6, 0x0C, 0x24, (byte) 0xB4, (byte) 0xC7, (byte) 0x94, (byte) 0x9D, (byte) 0x92, (byte) 0xD2, 0x3B, (byte) 0xD5, 0x11, 0x6F, 0x0A,
                    (byte) 0xDB, 0x76, 0x66, (byte) 0xE7, (byte) 0xCD, 0x18, 0x2B, 0x66, (byte) 0xD8, 0x41, 0x40, 0x58, (byte) 0xA2, 0x01, 0x1E, 0x6D,
                    0x44, 0x75, (byte) 0xE7, 0x19, 0x4F, (byte) 0xB2, (byte) 0xE8, (byte) 0xC4, (byte) 0x96, 0x77, 0x62, 0x02, (byte) 0xC9, (byte) 0xDC, 0x59, (byte) 0xF3,
                    0x43, (byte) 0x8D, (byte) 0xC8, (byte) 0xFE, (byte) 0x9E, 0x2A, (byte) 0xBA, 0x32, 0x3B, 0x62, (byte) 0xE3, (byte) 0x92, 0x6E, (byte) 0xC2, 0x08, 0x4D,
                    0x51, (byte) 0xCD, (byte) 0xF9, 0x3A, 0x3E, (byte) 0xC9, 0x50, 0x27, 0x21, 0x25, (byte) 0x97, (byte) 0xD7, 0x0E, (byte) 0xF8, 0x39, 0x38,
                    (byte) 0xF5, (byte) 0x86, (byte) 0x94, (byte) 0x93, (byte) 0xBF, (byte) 0xEB, 0x18, (byte) 0xA8, (byte) 0xFC, 0x24, (byte) 0xF5, (byte) 0xF9, (byte) 0x99, 0x20, 0x3D, (byte) 0xCD,
                    0x2C, (byte) 0x94, 0x25, 0x79, 0x28, 0x77, (byte) 0x8F, 0x2F, 0x10, 0x69, (byte) 0x86, 0x30, 0x43, 0x01, (byte) 0xD7, (byte) 0x9A,
                    0x17, (byte) 0xE3, 0x47, 0x37, (byte) 0xBD, 0x62, 0x75, 0x42, 0x78, (byte) 0xF4, 0x2B, 0x57, 0x4C, 0x0A, (byte) 0xDB, 0x53,
                    0x4D, (byte) 0xA1, 0x0A, (byte) 0xD6, 0x3A, 0x16, 0x15, (byte) 0xAA, 0x2C, 0x6C, 0x39, 0x42
            },
            new byte[] {  // Region 6, $13ab42 (drgw2), $13ab2e (dw2v100x)
                    0x12, 0x09, (byte) 0xF3, 0x29, (byte) 0xA0, 0x11, (byte) 0xA0, 0x11, (byte) 0xB0, (byte) 0xD5, 0x66, (byte) 0xA1, 0x28, 0x4A, 0x21, (byte) 0xC0,
                    (byte) 0xD3, (byte) 0x9B, (byte) 0x86, (byte) 0x80, 0x57, 0x6F, 0x41, (byte) 0xC2, (byte) 0xE4, 0x2F, 0x0B, (byte) 0x91, (byte) 0xBD, 0x3A, 0x7A, (byte) 0xBA,
                    0x00, (byte) 0xE5, 0x35, 0x02, 0x74, 0x7D, (byte) 0x8B, 0x21, 0x57, 0x10, 0x0F, (byte) 0xAE, 0x44, (byte) 0xBB, (byte) 0xE2, 0x37,
                    0x18, 0x7B, 0x52, 0x3D, (byte) 0x8C, 0x59, (byte) 0x9E, 0x20, 0x1F, 0x0A, (byte) 0xCC, 0x1C, (byte) 0x8E, 0x6A, (byte) 0xD7, (byte) 0x95,
                    0x2B, 0x34, (byte) 0xB0, (byte) 0x82, 0x6D, (byte) 0xFD, 0x25, 0x33, (byte) 0xAA, 0x3B, 0x2B, 0x70, 0x15, (byte) 0x87, 0x31, 0x5D,
                    (byte) 0xBB, 0x29, 0x19, (byte) 0x95, (byte) 0xD5, (byte) 0x8E, 0x24, 0x28, 0x5E, (byte) 0xD0, 0x20, (byte) 0x83, 0x46, 0x4A, 0x21, 0x70,
                    0x5B, (byte) 0xCD, (byte) 0xAE, 0x7B, 0x61, (byte) 0xA1, (byte) 0xFA, (byte) 0xF4, 0x2B, (byte) 0x84, 0x15, 0x6E, 0x36, 0x5D, 0x1B, 0x24,
                    0x0F, 0x09, 0x3A, 0x61, 0x38, 0x0F, 0x18, 0x35, 0x11, 0x38, (byte) 0xB4, (byte) 0xBD, (byte) 0xEE, (byte) 0xF7, (byte) 0xEC, 0x0F,
                    0x1D, (byte) 0xB7, 0x48, 0x01, (byte) 0xAA, 0x09, (byte) 0x8F, 0x61, (byte) 0xB5, 0x0F, 0x1D, 0x26, 0x39, 0x2E, (byte) 0x8C, (byte) 0xD6,
                    0x26, 0x5C, 0x3D, 0x23, 0x63, (byte) 0xE9, 0x6B, (byte) 0x97, (byte) 0xB4, (byte) 0x9F, 0x7B, (byte) 0xB6, (byte) 0xBA, (byte) 0xA0, 0x7C, (byte) 0xC6,
                    0x25, (byte) 0xA1, 0x73, 0x36, 0x67, 0x7F, 0x74, 0x1E, 0x1D, (byte) 0xDA, 0x70, (byte) 0xBF, (byte) 0xA5, 0x63, 0x35, 0x39,
                    0x24, (byte) 0x8C, (byte) 0x9F, (byte) 0x85, 0x16, (byte) 0xD8, 0x50, (byte) 0x95, 0x71, (byte) 0xC0, (byte) 0xF6, 0x1E, 0x6D, (byte) 0x80, (byte) 0xED, 0x15,
                    (byte) 0xEB, 0x63, (byte) 0xE9, 0x1B, (byte) 0xF6, 0x78, 0x31, (byte) 0xC6, 0x5C, (byte) 0xDD, 0x19, (byte) 0xBD, (byte) 0xDF, (byte) 0xA7, (byte) 0xEC, 0x50,
                    0x22, (byte) 0xAD, (byte) 0xBB, (byte) 0xF6, (byte) 0xEB, (byte) 0xD6, (byte) 0xA3, 0x20, (byte) 0xC9, (byte) 0xE6, (byte) 0x9F, (byte) 0xCB, (byte) 0xF2, (byte) 0x97, (byte) 0xB9, 0x54,
                    0x12, 0x66, (byte) 0xA6, (byte) 0xBE, 0x4A, 0x12, 0x43, (byte) 0xEC, 0x00, (byte) 0xEA, 0x49, 0x02
            },
            new byte[] {0,}  // Region 7, not used
    };
    public static final byte[][] killbld_source_data = new byte[][] { //[(byte) 0xec]
            new byte[] { // region 16, $178772
                    0x5e, 0x09, (byte) 0xb3, 0x39, 0x60, 0x71, 0x71, 0x53, 0x11, (byte) 0xe5, 0x26, 0x34, 0x4c, (byte) 0x8c, (byte) 0x90, (byte) 0xee,
                    (byte) 0xed, (byte) 0xb5, 0x05, (byte) 0x95, (byte) 0x9e, 0x6b, (byte) 0xdd, (byte) 0x87, 0x0e, 0x7b, (byte) 0xed, 0x33, (byte) 0xaf, (byte) 0xc2, 0x62, (byte) 0x98,
                    (byte) 0xec, (byte) 0xc8, 0x2c, 0x2b, 0x57, 0x3d, 0x00, (byte) 0xbd, 0x12, (byte) 0xac, (byte) 0xba, 0x64, (byte) 0x81, (byte) 0x99, 0x16, 0x29,
                    (byte) 0xb4, 0x63, (byte) 0xa8, (byte) 0xd9, (byte) 0xc9, 0x5f, (byte) 0xfe, 0x21, (byte) 0xbb, (byte) 0xbf, (byte) 0x9b, (byte) 0xd1, 0x7b, (byte) 0x93, (byte) 0xc4, (byte) 0x82,
                    (byte) 0xef, 0x2b, (byte) 0xe8, (byte) 0xa6, (byte) 0xdc, 0x68, 0x3a, (byte) 0xd9, (byte) 0xc9, 0x23, (byte) 0xc7, 0x7b, (byte) 0x98, 0x5b, (byte) 0xe1, (byte) 0xc7,
                    (byte) 0xa3, (byte) 0xd4, 0x51, 0x0a, (byte) 0x86, 0x30, 0x20, 0x51, 0x6e, 0x04, 0x1c, (byte) 0xd4, (byte) 0xfb, (byte) 0xf5, 0x22, (byte) 0x8f,
                    0x16, 0x6f, (byte) 0xb9, 0x59, 0x30, (byte) 0xcf, (byte) 0xab, 0x32, 0x1d, 0x6c, (byte) 0x84, (byte) 0xab, 0x23, (byte) 0x90, (byte) 0x94, (byte) 0xb1,
                    (byte) 0xe7, 0x4b, 0x6d, (byte) 0xc1, (byte) 0x84, (byte) 0xba, 0x32, 0x68, (byte) 0xa3, (byte) 0xf2, 0x47, 0x28, (byte) 0xe5, (byte) 0xcb, (byte) 0xbb, 0x47,
                    0x14, 0x2c, (byte) 0xad, 0x4d, (byte) 0xa1, (byte) 0xd7, 0x18, 0x53, (byte) 0xf7, 0x6f, 0x05, (byte) 0x81, (byte) 0x8f, (byte) 0xbb, 0x29, (byte) 0xdc,
                    (byte) 0xbd, 0x17, 0x61, (byte) 0x92, (byte) 0x9b, 0x1d, 0x4e, 0x7a, (byte) 0x83, 0x14, (byte) 0x9f, 0x7b, 0x7a, 0x6a, (byte) 0xe1, 0x27,
                    0x62, 0x52, 0x7e, (byte) 0x82, 0x45, (byte) 0xda, (byte) 0xed, (byte) 0xf1, 0x0a, 0x3b, 0x6c, 0x02, 0x5b, 0x6e, 0x45, 0x4e,
                    (byte) 0xf2, 0x65, (byte) 0x87, 0x1d, (byte) 0x80, (byte) 0xed, 0x6a, (byte) 0xc3, 0x77, (byte) 0xcb, (byte) 0xe8, (byte) 0x8d, 0x5a, (byte) 0xb8, (byte) 0xda, (byte) 0x89,
                    (byte) 0x88, 0x4b, 0x27, (byte) 0xd5, 0x57, 0x29, (byte) 0x91, (byte) 0x86, 0x12, (byte) 0xbb, (byte) 0xd3, (byte) 0x8c, (byte) 0xc7, 0x49, (byte) 0x84, (byte) 0x9c,
                    (byte) 0x96, 0x59, 0x30, (byte) 0x93, (byte) 0x92, (byte) 0xeb, 0x59, 0x2b, (byte) 0x93, 0x5b, 0x5f, (byte) 0xf9, 0x67, (byte) 0xac, (byte) 0x97, (byte) 0x8c,
                    0x04, (byte) 0xda, 0x1b, 0x65, (byte) 0xd7, (byte) 0xef, 0x44, (byte) 0xca, (byte) 0xc4, (byte) 0x87, 0x18, 0x2b
            },
            new byte[] { // region 17, $178a36
                    (byte) 0xd7, 0x49, (byte) 0xb3, 0x39, 0x60, 0x71, 0x70, 0x53, 0x11, 0x00, 0x27, (byte) 0xb2, 0x61, (byte) 0xd3, (byte) 0x8c, (byte) 0x8b,
                    (byte) 0xb2, (byte) 0xde, 0x6a, 0x78, 0x40, 0x5d, 0x4d, (byte) 0x88, (byte) 0xeb, (byte) 0x81, (byte) 0xd0, 0x2a, (byte) 0xbf, (byte) 0x8c, 0x22, 0x0d,
                    (byte) 0x89, (byte) 0x83, (byte) 0xc8, (byte) 0xef, 0x0d, 0x7a, (byte) 0xf6, (byte) 0xf0, 0x1d, 0x49, (byte) 0xa2, (byte) 0xd3, 0x1e, (byte) 0xef, 0x1c, (byte) 0xa2,
                    (byte) 0xce, 0x00, 0x5e, (byte) 0xa8, 0x7f, 0x4c, 0x41, 0x27, (byte) 0xa8, 0x6b, (byte) 0x92, 0x0a, (byte) 0xb8, 0x03, 0x2f, 0x7e,
                    (byte) 0xaf, 0x4a, (byte) 0xd0, 0x5c, (byte) 0xce, (byte) 0xeb, 0x0e, (byte) 0x8a, 0x4d, 0x0b, 0x73, (byte) 0xb3, (byte) 0xf3, 0x0c, (byte) 0x83, (byte) 0xaa,
                    (byte) 0xe5, (byte) 0xe4, (byte) 0x84, 0x06, (byte) 0xd7, (byte) 0xcc, (byte) 0xcb, 0x52, (byte) 0x8d, (byte) 0xbe, (byte) 0xa4, (byte) 0xdf, (byte) 0xd9, (byte) 0xab, 0x50, 0x59,
                    0x53, 0x61, (byte) 0xa1, (byte) 0xc8, 0x6d, (byte) 0xbc, (byte) 0xde, (byte) 0xab, (byte) 0xaa, 0x5e, (byte) 0xc6, (byte) 0xf7, (byte) 0x83, (byte) 0xdc, 0x40, (byte) 0xcb,
                    0x1b, (byte) 0xdd, 0x28, 0x3b, (byte) 0xee, (byte) 0xb1, 0x1f, 0x37, (byte) 0xdb, (byte) 0xe9, (byte) 0xbb, 0x74, 0x4b, (byte) 0xc2, (byte) 0x8a, (byte) 0xe8,
                    (byte) 0xec, 0x6e, 0x0e, 0x35, (byte) 0xe3, 0x2e, (byte) 0xbe, (byte) 0xef, (byte) 0xfd, 0x07, (byte) 0xbf, (byte) 0x8c, (byte) 0xfe, (byte) 0xf3, 0x5c, (byte) 0xbf,
                    (byte) 0x87, (byte) 0xe5, (byte) 0xbc, (byte) 0xcf, 0x60, (byte) 0xdc, 0x18, (byte) 0xf8, (byte) 0xfc, 0x51, 0x50, (byte) 0x86, (byte) 0xc6, 0x48, 0x3d, (byte) 0xb9,
                    0x1d, 0x26, (byte) 0xf7, 0x7e, (byte) 0x87, (byte) 0x90, 0x12, (byte) 0xe8, 0x06, 0x0a, 0x45, (byte) 0xe9, (byte) 0xd9, (byte) 0xd8, 0x41, 0x68,
                    0x21, 0x52, (byte) 0x92, 0x0f, (byte) 0xd6, (byte) 0xda, (byte) 0xa2, (byte) 0x97, (byte) 0xeb, 0x68, (byte) 0xd0, (byte) 0xb1, 0x15, 0x19, (byte) 0x8b, (byte) 0xd0,
                    0x48, 0x1a, (byte) 0xeb, (byte) 0x90, 0x3f, 0x2a, 0x33, 0x1e, 0x5e, 0x30, 0x66, 0x01, 0x64, (byte) 0xef, (byte) 0x99, 0x52,
                    (byte) 0xba, 0x23, (byte) 0xbd, 0x53, (byte) 0xc0, 0x60, (byte) 0x87, 0x09, (byte) 0xcb, 0x4d, (byte) 0xd3, (byte) 0x87, 0x0e, 0x3a, 0x5c, (byte) 0x8d,
                    (byte) 0xc8, (byte) 0xb8, (byte) 0xb7, 0x34, 0x01, (byte) 0xeb, 0x72, 0x0d, (byte) 0xb1, 0x1f, 0x0f, (byte) 0xea
            },
            new byte[] { // region 18, $17dac4
                    0x6a, 0x13, (byte) 0xb3, 0x09, 0x60, 0x79, 0x61, 0x53, 0x11, 0x33, 0x41, 0x31, 0x76, 0x34, (byte) 0x88, 0x0f,
                    0x77, 0x08, (byte) 0xb6, 0x74, (byte) 0xc8, 0x36, (byte) 0xbc, 0x70, (byte) 0xe2, (byte) 0x87, (byte) 0x9a, 0x21, (byte) 0xe8, 0x56, (byte) 0xe1, (byte) 0x9a,
                    0x26, 0x57, 0x7e, (byte) 0x9b, (byte) 0xdb, (byte) 0xb7, (byte) 0xd4, 0x3d, 0x0f, (byte) 0xfe, (byte) 0x8a, 0x2a, (byte) 0xba, 0x2d, 0x22, 0x03,
                    (byte) 0xcf, (byte) 0x9c, (byte) 0xfa, 0x77, 0x35, 0x39, 0x6a, 0x14, (byte) 0xae, 0x30, (byte) 0x89, 0x42, (byte) 0xdc, 0x59, (byte) 0xb2, (byte) 0x93,
                    0x6f, (byte) 0x82, (byte) 0xd1, 0x12, (byte) 0xd9, (byte) 0x88, (byte) 0xfa, 0x3b, (byte) 0xb7, 0x0c, 0x1f, 0x05, 0x68, (byte) 0xa3, 0x0c, (byte) 0xa6,
                    0x0f, (byte) 0xf4, (byte) 0x9e, 0x1b, 0x29, (byte) 0x82, 0x77, 0x3a, (byte) 0xac, (byte) 0x92, 0x2d, 0x04, (byte) 0xd0, 0x61, 0x65, 0x0a,
                    0x77, 0x6c, (byte) 0x89, 0x38, (byte) 0xaa, (byte) 0xa9, (byte) 0xf8, 0x0c, 0x1f, 0x37, 0x09, 0x2b, (byte) 0xca, 0x29, 0x05, (byte) 0xe5,
                    0x4e, 0x57, (byte) 0xfb, (byte) 0xcd, 0x40, (byte) 0xa8, 0x0c, 0x06, 0x2d, (byte) 0xe0, 0x30, (byte) 0xd9, (byte) 0x97, (byte) 0xb9, 0x59, (byte) 0x8a,
                    (byte) 0xde, (byte) 0xc9, (byte) 0x87, 0x1d, 0x3f, (byte) 0x84, 0x4c, 0x73, 0x04, (byte) 0x85, 0x61, (byte) 0xb0, 0x6e, 0x2c, (byte) 0x8f, (byte) 0xa2,
                    0x6a, (byte) 0xcd, 0x31, (byte) 0xf3, 0x25, (byte) 0x83, (byte) 0xe1, 0x5e, 0x5d, (byte) 0xa7, (byte) 0xe7, (byte) 0xaa, 0x13, 0x26, (byte) 0xb1, 0x33,
                    (byte) 0xf0, 0x13, 0x58, 0x7a, (byte) 0xb0, 0x46, 0x1d, (byte) 0xdf, 0x02, (byte) 0xbf, 0x1e, (byte) 0xd1, 0x71, 0x43, 0x56, (byte) 0x82,
                    0x4f, 0x58, (byte) 0x9d, 0x01, 0x2d, (byte) 0xc7, (byte) 0xda, 0x6b, 0x47, 0x05, (byte) 0xd1, (byte) 0xd5, (byte) 0xe8, (byte) 0x92, 0x3c, 0x18,
                    0x21, (byte) 0xcf, (byte) 0xc9, 0x32, 0x0e, 0x12, (byte) 0xed, (byte) 0xb5, (byte) 0xaa, (byte) 0xa4, 0x12, 0x75, 0x01, 0x7d, (byte) 0xc7, 0x21,
                    (byte) 0xde, (byte) 0xec, 0x32, 0x13, (byte) 0xee, (byte) 0xd4, (byte) 0x9c, (byte) 0xe6, 0x04, 0x3f, 0x48, (byte) 0xfb, (byte) 0xb4, (byte) 0xc7, 0x21, (byte) 0x8e,
                    (byte) 0x8d, 0x7d, 0x54, 0x03, 0x11, (byte) 0xe7, (byte) 0xb9, 0x4f, (byte) 0x85, (byte) 0xb6, 0x1f, (byte) 0xaa
            },
            new byte[] { // region 19, $178eee
                    (byte) 0xe3, 0x53, (byte) 0xb3, 0x09, 0x60, 0x79, 0x60, 0x53, 0x11, 0x66, 0x5b, (byte) 0xc8, (byte) 0x8b, (byte) 0x94, (byte) 0x84, (byte) 0xab,
                    0x3c, 0x18, 0x03, 0x57, 0x6a, 0x0f, 0x45, 0x58, (byte) 0xc0, 0x74, 0x64, 0x18, (byte) 0xf8, 0x39, (byte) 0xa1, 0x0f,
                    (byte) 0xc2, 0x2b, 0x1b, 0x60, (byte) 0xaa, 0x0e, (byte) 0xb2, (byte) 0x89, 0x01, (byte) 0x9b, 0x72, (byte) 0x80, 0x57, (byte) 0x83, 0x28, 0x63,
                    (byte) 0xe9, 0x39, (byte) 0x97, 0x46, (byte) 0xea, 0x3f, (byte) 0x93, 0x01, (byte) 0x9b, (byte) 0xf4, (byte) 0x80, (byte) 0x93, 0x01, (byte) 0xaf, 0x1d, (byte) 0x8f,
                    0x16, (byte) 0xa1, (byte) 0xb9, (byte) 0xc7, (byte) 0xe4, 0x0c, (byte) 0xe7, (byte) 0xd2, 0x3b, (byte) 0xf3, (byte) 0xca, 0x3d, (byte) 0xc3, 0x54, (byte) 0xad, (byte) 0x89,
                    0x51, 0x1e, (byte) 0xd1, 0x17, 0x7a, 0x1f, 0x23, 0x22, (byte) 0xcb, 0x4d, (byte) 0xce, 0x0f, (byte) 0xae, 0x30, (byte) 0x93, (byte) 0xd3,
                    (byte) 0x9b, 0x77, 0x71, (byte) 0xa7, (byte) 0xe7, (byte) 0x96, 0x2c, (byte) 0x85, (byte) 0xac, 0x29, 0x4b, 0x5e, 0x2b, 0x75, (byte) 0xb0, 0x00,
                    (byte) 0x81, (byte) 0xe9, (byte) 0xb6, 0x47, (byte) 0xaa, (byte) 0x9f, (byte) 0xdf, (byte) 0xd4, 0x7e, (byte) 0xd7, (byte) 0xa4, 0x3f, (byte) 0xe3, (byte) 0xb0, 0x41, 0x2c,
                    (byte) 0xb7, 0x0c, (byte) 0xe7, (byte) 0xeb, (byte) 0x9a, (byte) 0xda, (byte) 0xd9, 0x10, 0x23, 0x1d, 0x1c, (byte) 0xd4, (byte) 0xdd, 0x7d, (byte) 0xc2, 0x6c,
                    0x4d, (byte) 0x9c, (byte) 0xa5, 0x18, (byte) 0xd0, 0x43, (byte) 0xab, (byte) 0xdc, (byte) 0xbd, (byte) 0xe4, 0x7f, (byte) 0xb5, 0x5f, 0x04, 0x0d, (byte) 0xac,
                    (byte) 0xab, (byte) 0xe6, (byte) 0xb8, 0x76, (byte) 0xf2, 0x15, 0x41, (byte) 0xef, 0x17, (byte) 0x8e, (byte) 0xf6, (byte) 0xb9, (byte) 0xef, (byte) 0x94, 0x52, (byte) 0x83,
                    (byte) 0x96, 0x45, (byte) 0x8f, (byte) 0xf2, (byte) 0x9c, (byte) 0xb4, 0x13, 0x3f, (byte) 0xbb, (byte) 0xa1, (byte) 0xd2, (byte) 0xf9, (byte) 0xa3, (byte) 0xf2, 0x06, 0x78,
                    (byte) 0xe0, (byte) 0x9e, (byte) 0xa7, (byte) 0xd3, (byte) 0xdc, 0x13, (byte) 0x8f, 0x4d, (byte) 0xf6, 0x19, (byte) 0xbd, 0x03, (byte) 0x9d, 0x24, (byte) 0xdc, (byte) 0xd6,
                    (byte) 0xe9, (byte) 0xcf, (byte) 0xa6, (byte) 0xd2, 0x1d, 0x49, (byte) 0xca, (byte) 0xc4, 0x55, 0x18, (byte) 0xbc, 0x70, 0x5b, 0x55, (byte) 0xfe, (byte) 0x8f,
                    0x6b, 0x42, (byte) 0xf0, (byte) 0xd1, 0x21, (byte) 0xe3, (byte) 0xe7, (byte) 0x91, 0x59, 0x4e, 0x16, (byte) 0x83
            },
            new byte[] {0,}, // unused region 1a
            new byte[] {0,}, // unused region 1b
            new byte[] {0,}, // unused region 1c
            new byte[] {0,}, // unused region 1d
            new byte[] {0,}, // unused region 1e
            new byte[] {0,}, // unused region 1f
            new byte[] { // region 20, $17a322
                    (byte) 0xb3, 0x10, (byte) 0xf3, 0x0b, (byte) 0xe0, 0x71, 0x60, 0x53, 0x11, (byte) 0x9a, 0x12, 0x70, 0x1f, 0x1e, (byte) 0x81, (byte) 0xda,
                    (byte) 0x9d, 0x1f, 0x4b, (byte) 0xd6, 0x71, 0x48, (byte) 0x83, (byte) 0xe1, 0x04, 0x6c, 0x1b, (byte) 0xf1, (byte) 0xcd, 0x09, (byte) 0xdf, 0x3e,
                    0x0b, (byte) 0xaa, (byte) 0x95, (byte) 0xc1, 0x07, (byte) 0xec, 0x0f, 0x54, (byte) 0xd0, 0x16, (byte) 0xb0, (byte) 0xdc, (byte) 0x86, 0x7b, 0x52, 0x38,
                    0x3c, 0x68, 0x2b, (byte) 0xed, (byte) 0xe2, (byte) 0xeb, (byte) 0xb3, (byte) 0xc6, 0x48, 0x24, 0x41, 0x36, 0x17, 0x25, 0x1f, (byte) 0xa5,
                    0x22, (byte) 0xc6, 0x5c, (byte) 0xa6, 0x19, (byte) 0xef, 0x17, 0x5c, 0x56, 0x4b, 0x4a, 0x2b, 0x75, (byte) 0xab, (byte) 0xe6, 0x22,
                    (byte) 0xd5, (byte) 0xc0, (byte) 0xd3, 0x46, (byte) 0xcc, (byte) 0xe4, (byte) 0xd4, (byte) 0xc4, (byte) 0x8c, (byte) 0x9a, (byte) 0x8a, 0x75, 0x24, 0x73, (byte) 0xa4, 0x26,
                    (byte) 0xca, 0x79, (byte) 0xaf, (byte) 0xb3, (byte) 0x94, 0x2a, 0x15, (byte) 0xbe, 0x40, 0x7b, 0x4d, (byte) 0xf6, (byte) 0xb4, (byte) 0xa4, 0x7b, (byte) 0xcf,
                    (byte) 0xce, (byte) 0xa0, 0x1d, (byte) 0xcb, 0x2f, 0x60, 0x28, 0x63, (byte) 0x85, (byte) 0x98, (byte) 0xd3, (byte) 0xd2, 0x45, 0x3f, 0x02, 0x65,
                    (byte) 0xd7, (byte) 0xf4, (byte) 0xbc, 0x2a, (byte) 0xe7, 0x50, (byte) 0xd1, 0x3f, 0x7f, (byte) 0xf6, 0x05, (byte) 0xb8, (byte) 0xe9, 0x39, 0x10, 0x6e,
                    0x68, (byte) 0xa8, (byte) 0x89, 0x60, 0x00, 0x68, (byte) 0xfd, 0x20, (byte) 0xc4, (byte) 0xdc, (byte) 0xef, 0x67, 0x75, (byte) 0xfb, (byte) 0xbe, (byte) 0xfe,
                    0x2b, 0x16, (byte) 0xa6, 0x5a, 0x77, 0x0d, 0x0c, (byte) 0xe2, 0x2d, (byte) 0xd1, (byte) 0xe4, 0x11, (byte) 0xc9, 0x4b, (byte) 0x81, 0x3a,
                    0x0c, 0x24, (byte) 0xaa, 0x77, 0x2b, 0x2f, (byte) 0x83, 0x23, (byte) 0xd1, (byte) 0xe9, (byte) 0xa7, 0x29, 0x0a, (byte) 0xf9, 0x26, (byte) 0x9d,
                    0x51, (byte) 0xc8, 0x6d, 0x71, (byte) 0x9d, (byte) 0xce, 0x46, 0x72, 0x26, 0x48, 0x3d, 0x64, (byte) 0xe5, 0x67, (byte) 0xbb, 0x1a,
                    (byte) 0xb4, 0x6d, 0x21, 0x11, 0x79, 0x78, (byte) 0xc2, (byte) 0xd5, 0x11, 0x6a, (byte) 0xd2, (byte) 0xea, 0x03, 0x4d, (byte) 0x92, (byte) 0xaf,
                    0x18, (byte) 0xd5, 0x07, 0x79, (byte) 0xaa, (byte) 0xf9, 0x44, (byte) 0x93, 0x6f, 0x41, 0x22, 0x0d
            },
            new byte[] { // region 21, $17b3b4
                    0x2d, 0x50, (byte) 0xf3, 0x0b, (byte) 0xe0, 0x71, 0x61, 0x53, 0x11, (byte) 0xb4, 0x2c, (byte) 0xee, 0x34, 0x7e, 0x7d, 0x5e,
                    0x62, 0x48, (byte) 0x97, (byte) 0xd2, (byte) 0xf9, 0x3a, (byte) 0xf2, (byte) 0xc9, (byte) 0xfa, 0x59, (byte) 0xe4, (byte) 0xe8, (byte) 0xf6, (byte) 0xd2, (byte) 0x9f, (byte) 0xb2,
                    (byte) 0xa7, 0x7e, 0x32, (byte) 0x86, (byte) 0xbc, 0x43, (byte) 0xec, (byte) 0xa0, (byte) 0xc2, (byte) 0xcb, (byte) 0x98, 0x33, 0x23, (byte) 0xd1, 0x58, (byte) 0x98,
                    0x56, 0x05, (byte) 0xc7, (byte) 0xbc, (byte) 0x98, (byte) 0xd8, (byte) 0xdc, (byte) 0xb3, 0x35, (byte) 0xe8, 0x51, 0x6e, 0x3b, 0x7b, (byte) 0x89, (byte) 0xba,
                    (byte) 0xe1, (byte) 0xe5, 0x44, 0x5c, 0x24, 0x73, 0x04, 0x0d, (byte) 0xd9, 0x33, (byte) 0xf5, 0x63, (byte) 0xe9, 0x5c, (byte) 0x88, 0x05,
                    0x18, (byte) 0xd0, 0x07, 0x5b, 0x1e, (byte) 0x81, (byte) 0x80, (byte) 0xac, (byte) 0x92, 0x6e, 0x13, (byte) 0x80, 0x1b, 0x29, (byte) 0xd2, (byte) 0xef,
                    0x08, (byte) 0x84, (byte) 0x97, 0x23, (byte) 0xd1, 0x17, 0x2f, 0x38, (byte) 0xb4, 0x6d, (byte) 0x8f, 0x2a, 0x15, (byte) 0xf0, 0x40, (byte) 0xe9,
                    0x02, 0x33, (byte) 0xd7, 0x5e, (byte) 0x99, 0x57, 0x15, 0x32, (byte) 0xbd, (byte) 0x8f, 0x48, 0x38, (byte) 0x91, 0x36, (byte) 0xe9, 0x07,
                    (byte) 0xc9, 0x37, 0x1d, 0x12, 0x2a, (byte) 0xbf, 0x5f, (byte) 0xdb, (byte) 0x85, 0x75, (byte) 0xbf, (byte) 0xdc, 0x59, (byte) 0x8a, 0x43, 0x51,
                    0x4b, 0x77, (byte) 0xfd, (byte) 0x84, (byte) 0xc4, 0x28, (byte) 0xc7, (byte) 0x85, 0x25, 0x1a, (byte) 0x87, (byte) 0x8b, (byte) 0xc1, (byte) 0xd9, 0x1a, 0x78,
                    (byte) 0xe5, 0x03, 0x20, 0x56, (byte) 0xa0, (byte) 0xc2, 0x17, (byte) 0xf2, 0x29, (byte) 0xa0, (byte) 0xbd, (byte) 0xf8, 0x61, (byte) 0x9c, 0x7d, 0x54,
                    0x3a, 0x11, (byte) 0xb5, 0x69, (byte) 0x9a, 0x1c, (byte) 0xbb, (byte) 0xf6, 0x2d, (byte) 0x86, (byte) 0xa8, 0x4d, (byte) 0xdd, 0x5a, (byte) 0xd6, (byte) 0xe4,
                    0x11, 0x7e, 0x4b, 0x13, 0x6c, (byte) 0xb6, 0x01, 0x0a, 0x72, (byte) 0xbc, (byte) 0xe8, (byte) 0xf1, (byte) 0x82, 0x0e, (byte) 0xd0, (byte) 0xcf,
                    (byte) 0xbf, 0x50, (byte) 0x95, (byte) 0xb7, (byte) 0xa7, (byte) 0xec, (byte) 0xd7, (byte) 0xb3, 0x49, 0x5c, 0x47, 0x5f, (byte) 0xa9, (byte) 0xda, 0x70, (byte) 0xb0,
                    (byte) 0xdc, (byte) 0x9a, (byte) 0xa3, 0x48, (byte) 0xd3, (byte) 0xf5, 0x72, (byte) 0xd5, 0x43, (byte) 0xd8, 0x19, (byte) 0xcc
            }
    };

    public static void drgw_interrupt() {
        if (Cpuexec.iloops == 0) {
            Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
        } else {
            Cpuint.cpunum_set_input_line(0, 4, LineState.HOLD_LINE);
        }
    }

    public static void device_init() {
        kb_prot_hold = 0;
        kb_prot_hilo = 0;
        kb_prot_hilo_select = 0;
        kb_cmd = 0;
        kb_reg = 0;
        kb_ptr = 0;
        kb_swap = 0;
        olds_bs = 0;
        kb_cmd3 = 0;
        switch (Machine.sName) {
            case "drgw2":
                kb_source_data = drgw2_source_data;
                int region = 0x06;
                kb_region = region;
                kb_game_id = region | (region << 8) | (region << 16) | (region << 24);
                break;
            case "killbld":
                kb_source_data = killbld_source_data;

                break;
        }
    }

    public static void device_reset() {
        kb_prot_hold = 0;
        kb_prot_hilo = 0;
        kb_prot_hilo_select = 0;
        kb_cmd = 0;
        kb_reg = 0;
        kb_ptr = 0;
        kb_swap = 0;
        olds_bs = 0;
        kb_cmd3 = 0;
    }

    public static int BIT(int x, int n) {
        return (x >> n) & 1;
    }

    public static int BITSWAP8(int val, int B7, int B6, int B5, int B4, int B3, int B2, int B1, int B0) {
        return ((BIT(val, B7) << 7) |
                (BIT(val, B6) << 6) |
                (BIT(val, B5) << 5) |
                (BIT(val, B4) << 4) |
                (BIT(val, B3) << 3) |
                (BIT(val, B2) << 2) |
                (BIT(val, B1) << 1) |
                (BIT(val, B0) << 0));
    }

    public static short killbld_igs025_prot_r(int offset) {
        if (offset != 0) {
            switch (kb_cmd) {
                case 0x00:
                    return (short) BITSWAP8((kb_swap + 1) & 0x7f, 0, 1, 2, 3, 4, 5, 6, 7);
                case 0x01:
                    return (short) (kb_reg & 0x7f);
                case 0x02:
                    return (short) (olds_bs | 0x80);
                case 0x03:
                    return kb_cmd3;
                case 0x05: {
                    switch (kb_ptr) {
                        case 1:
                            return (short) (0x3f00 | ((kb_game_id >> 0) & 0xff));
                        case 2:
                            return (short) (0x3f00 | ((kb_game_id >> 8) & 0xff));
                        case 3:
                            return (short) (0x3f00 | ((kb_game_id >> 16) & 0xff));
                        case 4:
                            return (short) (0x3f00 | ((kb_game_id >> 24) & 0xff));
                        default:
                            return (short) (0x3f00 | BITSWAP8(kb_prot_hold, 5, 2, 9, 7, 10, 13, 12, 15));
                    }
                }
                case 0x40:
                    killbld_protection_calculate_hilo();
                    return 0;
            }
        }
        return 0;
    }

    public static void drgw2_d80000_protection_w(int offset, short data) {
        if (offset == 0) {
            kb_cmd = data;
            return;
        }
        switch (kb_cmd) {
            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x25:
            case 0x26:
            case 0x27:
                kb_ptr++;
                killbld_protection_calculate_hold(kb_cmd & 0x0f, data & 0xff);
                break;
        }
    }

    public static void killbld_protection_calculate_hold(int y, int z) {
        short old = kb_prot_hold;
        kb_prot_hold = (short) ((old << 1) | (old >> 15));
        kb_prot_hold ^= (short) 0x2bad;
        kb_prot_hold ^= (short) BIT(z, y);
        kb_prot_hold ^= (short) (BIT(old, 7) << 0);
        kb_prot_hold ^= (short) (BIT(~old, 13) << 4);
        kb_prot_hold ^= (short) (BIT(old, 3) << 11);
        kb_prot_hold ^= (short) ((kb_prot_hilo & ~0x0408) << 1);
    }

    public static void killbld_protection_calculate_hilo() {
        byte source;
        kb_prot_hilo_select++;
        if (kb_prot_hilo_select > 0xeb) {
            kb_prot_hilo_select = 0;
        }
        source = kb_source_data[kb_region][kb_prot_hilo_select];
        if ((kb_prot_hilo_select & 1) != 0) {
            kb_prot_hilo = (short) ((kb_prot_hilo & 0x00ff) | (source << 8));
        } else {
            kb_prot_hilo = (short) ((kb_prot_hilo & 0xff00) | (source << 0));
        }
    }

//#endregion

//#region Memory

    public static short short0, short1, short2, short3, short4, short5, short6;
    public static short short0_old, short1_old, short2_old, short3_old, short4_old, short5_old, short6_old;

    public static byte MReadOpByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x1_ffff) {
            result = mainbiosrom[address];
        } else if (address >= 0x10_0000 && address <= 0x3f_ffff) {
            if (address < 0x10_0000 + Memory.mainrom.length) {
                result = Memory.mainrom[address - 0x10_0000];
            } else {
                result = 0;
            }
        }
            /*else if (address >= 0x80_0000 && address <= 0x81_ffff)
            {
                result = (sbyte)Memory.mainram[address - 0x80_0000];
            }*/
        return result;
    }

    public static byte MReadByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x1_ffff) {
            result = mainbiosrom[address];
        } else if (address >= 0x10_0000 && address <= 0x3f_ffff) {
            if (address < 0x10_0000 + Memory.mainrom.length) {
                result = Memory.mainrom[address - 0x10_0000];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x81_ffff) {
            result = Memory.mainram[address - 0x80_0000];
        } else if (address >= 0x90_0000 && address <= 0x90_3fff) {
            result = pgm_bg_videoram[address - 0x90_0000];
        } else if (address >= 0x90_4000 && address <= 0x90_5fff) {
            result = pgm_tx_videoram[address - 0x90_4000];
        } else if (address >= 0x90_7000 && address <= 0x90_77ff) {
            result = pgm_rowscrollram[address - 0x90_7000];
        } else if (address >= 0xa0_0000 && address <= 0xa0_11ff) {
            int offset = (address - 0xa0_0000) / 2;
            if ((address % 2) == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if ((address % 2) == 1) {
                result = (byte) (Generic.paletteram16[offset]);
            }
        } else if (address >= 0xb0_0000 && address <= 0xb0_ffff) {
            result = pgm_videoregs[address - 0xb0_0000];
        } else if (address >= 0xc0_0002 && address <= 0xc0_0003) {
            result = (byte) Sound.latched_value[0];
        } else if (address >= 0xc0_0004 && address <= 0xc0_0005) {
            result = (byte) Sound.latched_value[1];
        } else if (address >= 0xc0_0006 && address <= 0xc0_0007) {
            result = PGM.pgm_calendar_r();
        } else if (address >= 0xc0_000c && address <= 0xc0_000d) {
            result = (byte) Sound.latched_value[2];
        } else if (address >= 0xc0_8000 && address <= 0xc0_8001) {
            result = (byte) short0;
        } else if (address >= 0xc0_8002 && address <= 0xc0_8003) {
            result = (byte) short1;
        } else if (address >= 0xc0_8004 && address <= 0xc0_8005) {
            result = (byte) short2;
        } else if (address >= 0xc0_8006 && address <= 0xc0_8007) {
            result = (byte) short3;
        } else if (address >= 0xc1_0000 && address <= 0xc1_ffff) {
            result = z80_ram_r(address - 0xc1_0000);
        }
        return result;
    }

    public static short MReadOpWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x1_ffff) {
            result = (short) (mainbiosrom[address] * 0x100 + mainbiosrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x3f_ffff) {
            if (address + 1 < 0x10_0000 + Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address - 0x10_0000] * 0x100 + Memory.mainrom[address - 0x10_0000 + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x81_ffff) {
            result = (short) (Memory.mainram[address - 0x80_0000] * 0x100 + Memory.mainram[address - 0x80_0000 + 1]);
        }
        return result;
    }

    public static short MReadWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x1_ffff) {
            result = (short) (mainbiosrom[address] * 0x100 + mainbiosrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x3f_ffff) {
            if (address + 1 < 0x10_0000 + Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address - 0x10_0000] * 0x100 + Memory.mainrom[address - 0x10_0000 + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x81_ffff) {
            result = (short) (Memory.mainram[address - 0x80_0000] * 0x100 + Memory.mainram[address - 0x80_0000 + 1]);
        } else if (address >= 0x90_0000 && address + 1 <= 0x90_3fff) {
            result = (short) (pgm_bg_videoram[address - 0x90_0000] * 0x100 + pgm_bg_videoram[address - 0x90_0000 + 1]);
        } else if (address >= 0x90_4000 && address + 1 <= 0x90_5fff) {
            result = (short) (pgm_tx_videoram[address - 0x90_4000] * 0x100 + pgm_tx_videoram[address - 0x90_4000 + 1]);
        } else if (address >= 0x90_7000 && address + 1 <= 0x90_77ff) {
            result = (short) (pgm_rowscrollram[address - 0x90_7000] * 0x100 + pgm_rowscrollram[address - 0x90_7000 + 1]);
        } else if (address >= 0xa0_0000 && address + 1 <= 0xa0_11ff) {
            int offset = (address - 0xa_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0xb0_0000 && address + 1 <= 0xb0_ffff) {
            result = (short) (pgm_videoregs[address - 0xb0_0000] * 0x100 + pgm_videoregs[address - 0xb0_0000 + 1]);
        } else if (address >= 0xc0_0002 && address + 1 <= 0xc0_0003) {
            result = Sound.latched_value[0];
        } else if (address >= 0xc0_0004 && address + 1 <= 0xc0_0005) {
            result = Sound.latched_value[1];
        } else if (address >= 0xc0_0006 && address + 1 <= 0xc0_0007) {
            result = PGM.pgm_calendar_r();
        } else if (address >= 0xc0_000c && address + 1 <= 0xc0_000d) {
            result = Sound.latched_value[2];
        } else if (address >= 0xc0_8000 && address + 1 <= 0xc0_8001) {
            result = short0;
        } else if (address >= 0xc0_8002 && address + 1 <= 0xc0_8003) {
            result = short1;
        } else if (address >= 0xc0_8004 && address + 1 <= 0xc0_8005) {
            result = short2;
        } else if (address >= 0xc0_8006 && address + 1 <= 0xc0_8007) {
            result = short3;
        } else if (address >= 0xc1_0000 && address + 1 <= 0xc1_ffff) {
            result = (short) (z80_ram_r(address - 0xc1_0000) * 0x100 + z80_ram_r(address - 0xc1_0000 + 1));
        }
        return result;
    }

    public static int MReadOpLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0 && address + 3 <= 0x1_ffff) {
            result = mainbiosrom[address] * 0x100_0000 + mainbiosrom[address + 1] * 0x1_0000 + mainbiosrom[address + 2] * 0x100 + mainbiosrom[address + 3];
        } else if (address >= 0x10_0000 && address + 3 <= 0x3f_ffff) {
            if (address + 3 < 0x10_0000 + Memory.mainrom.length) {
                result = Memory.mainrom[address - 0x10_0000] * 0x100_0000 + Memory.mainrom[address - 0x10_0000 + 1] * 0x1_0000 + Memory.mainrom[address - 0x10_0000 + 2] * 0x100 + Memory.mainrom[address - 0x10_0000 + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x81_ffff) {
            result = Memory.mainram[address - 0x80_0000] * 0x100_0000 + Memory.mainram[address - 0x80_0000 + 1] * 0x1_0000 + Memory.mainram[address - 0x80_0000 + 2] * 0x100 + Memory.mainram[address - 0x80_0000 + 3];
        }
        return result;
    }

    public static int MReadLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0 && address + 1 <= 0x1_ffff) {
            result = mainbiosrom[address] * 0x100_0000 + mainbiosrom[address + 1] * 0x1_0000 + mainbiosrom[address + 2] * 0x100 + mainbiosrom[address + 3];
        } else if (address >= 0x10_0000 && address + 3 <= 0x3f_ffff) {
            if (address + 3 < 0x10_0000 + Memory.mainrom.length) {
                result = Memory.mainrom[address - 0x10_0000] * 0x100_0000 + Memory.mainrom[address - 0x10_0000 + 1] * 0x1_0000 + Memory.mainrom[address - 0x10_0000 + 2] * 0x100 + Memory.mainrom[address - 0x10_0000 + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x81_ffff) {
            result = Memory.mainram[address - 0x80_0000] * 0x100_0000 + Memory.mainram[address - 0x80_0000 + 1] * 0x1_0000 + Memory.mainram[address - 0x80_0000 + 2] * 0x100 + Memory.mainram[address - 0x80_0000 + 3];
        } else if (address >= 0x90_0000 && address + 3 <= 0x90_3fff) {
            result = pgm_bg_videoram[address - 0x90_0000] * 0x100_0000 + pgm_bg_videoram[address - 0x90_0000 + 1] * 0x1_0000 + pgm_bg_videoram[address - 0x90_0000 + 2] * 0x100 + pgm_bg_videoram[address - 0x90_0000 + 3];
        } else if (address >= 0x90_4000 && address + 3 <= 0x90_5fff) {
            result = pgm_tx_videoram[address - 0x90_4000] * 0x100_0000 + pgm_tx_videoram[address - 0x90_4000 + 1] * 0x1_0000 + pgm_tx_videoram[address - 0x90_4000 + 2] * 0x100 + pgm_tx_videoram[address - 0x90_4000 + 3];
        } else if (address >= 0x90_7000 && address + 3 <= 0x90_77ff) {
            result = pgm_rowscrollram[address - 0x90_7000] * 0x100_0000 + pgm_rowscrollram[address - 0x90_7000 + 1] * 0x1_0000 + pgm_rowscrollram[address - 0x90_7000 + 2] * 0x100 + pgm_rowscrollram[address - 0x90_7000 + 3];
        } else if (address >= 0xa0_0000 && address + 3 <= 0xa0_11ff) {
            int offset = (address - 0xa0_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0xb0_0000 && address + 3 <= 0xb0_ffff) {
            result = pgm_videoregs[address - 0xb0_0000] * 0x100_0000 + pgm_videoregs[address - 0xb0_0000 + 1] * 0x1_0000 + pgm_videoregs[address - 0xb0_0000 + 2] * 0x100 + pgm_videoregs[address - 0xb0_0000 + 3];
        }
            /*else if (address >= 0xc0_0002 && address + 3 <= 0xc0_0003)
            {
                result = (short)Sound.ulatched_value[0];
            }
            else if (address >= 0xc0_0004 && address + 3 <= 0xc0_0005)
            {
                result = (short)Sound.ulatched_value[1];
            }
            else if (address >= 0xc0_0006 && address + 3 <= 0xc0_0007)
            {
                result = (short)Pgm.pgm_calendar_r();
            }
            else if (address >= 0xc0_000c && address + 3 <= 0xc0_000d)
            {
                result = (short)Sound.ulatched_value[2];
            }*/
        else if (address >= 0xc0_8000 && address + 3 <= 0xc0_8003) {
            result = (short0 << 16) | short1;
        } else if (address >= 0xc0_8002 && address + 3 <= 0xc0_8005) {
            result = short1;
        } else if (address >= 0xc0_8004 && address + 3 <= 0xc0_8007) {
            result = short2;
        } else if (address >= 0xc0_8006 && address + 3 <= 0xc0_8009) {
            result = short3;
        } else if (address >= 0xc1_0000 && address + 3 <= 0xc1_ffff) {
            result = z80_ram_r(address - 0xc1_0000) * 0x100_0000 + z80_ram_r(address - 0xc1_0000 + 1) * 0x1_0000 + z80_ram_r(address - 0xc1_0000 + 2) * 0x100 + z80_ram_r(address - 0xc1_0000 + 3);
        }
        return result;
    }

    public static void MWriteByte(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x70_0006 && address <= 0x70_0007) {
            //NOP;
        } else if (address >= 0x80_0000 && address <= 0x81_ffff) {
            int offset = address - 0x80_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x90_0000 && address <= 0x90_3fff) {
            int offset = address - 0x90_0000;
            pgm_bg_videoram_w(offset, value);
        } else if (address >= 0x90_4000 && address <= 0x90_5fff) {
            int offset = address - 0x90_4000;
            pgm_tx_videoram_w(offset, value);
        } else if (address >= 0x90_7000 && address <= 0x90_77ff) {
            int offset = address - 0x90_7000;
            pgm_rowscrollram[offset] = value;
        } else if (address >= 0xa0_0000 && address <= 0xa0_11ff) {
            int offset = (address - 0xa0_0000) / 2;
            if ((address % 2) == 0) {
                Generic.paletteram16[offset] = (short) ((value << 8) | (Generic.paletteram16[offset] & 0xff));
            } else if ((address % 2) == 1) {
                Generic.paletteram16[offset] = (short) ((Generic.paletteram16[offset] & 0xff00) | value);
            }
            Generic.paletteram16_xRRRRRGGGGGBBBBB_word_w(offset);
        } else if (address >= 0xb0_0000 && address <= 0xb0_ffff) {
            int offset = address - 0xb0_0000;
            pgm_videoregs[offset] = value;
        } else if (address >= 0xc0_0002 && address <= 0xc0_0003) {
            if (address == 0xc0_0003) {
                m68k_l1_w(value);
            }
        } else if (address >= 0xc0_0004 && address <= 0xc0_0005) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0xc0_0006 && address <= 0xc0_0007) {
            if (address == 0xc0_0006) {
                int i1 = 1;
            } else if (address == 0xc0_0007) {
                pgm_calendar_w(value);
            }
        } else if (address >= 0xc0_0008 && address <= 0xc0_0009) {
            z80_reset_w(value);
        } else if (address >= 0xc0_000a && address <= 0xc0_000b) {
            z80_ctrl_w();
        } else if (address >= 0xc0_000c && address <= 0xc0_000d) {
            Sound.soundlatch3_w(value);
        } else if (address >= 0xc1_0000 && address <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            z80_ram_w(offset, value);
        } else {
            int i1 = 1;
        }
    }

    public static void MWriteWord(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x70_0006 && address + 1 <= 0x70_0007) {
            //NOP;
        } else if (address >= 0x80_0000 && address + 1 <= 0x81_ffff) {
            int offset = address - 0x80_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x90_0000 && address + 1 <= 0x90_3fff) {
            int offset = (address - 0x90_0000) / 2;
            pgm_bg_videoram_w(offset, value);
        } else if (address >= 0x90_4000 && address + 1 <= 0x90_5fff) {
            int offset = (address - 0x90_4000) / 2;
            pgm_tx_videoram_w(offset, value);
        } else if (address >= 0x90_7000 && address + 1 <= 0x90_77ff) {
            int offset = (address - 0x90_7000) / 2;
            pgm_rowscrollram[offset * 2] = (byte) (value >> 8);
            pgm_rowscrollram[offset * 2 + 1] = (byte) value;
        } else if (address >= 0xa0_0000 && address + 1 <= 0xa0_11ff) {
            int offset = (address - 0xa0_0000) / 2;
            Generic.paletteram16[offset] = value;
            Generic.paletteram16_xRRRRRGGGGGBBBBB_word_w(offset);
        } else if (address >= 0xb0_0000 && address + 1 <= 0xb0_ffff) {
            int offset = (address - 0xb0_0000) / 2;
            pgm_videoregs[offset * 2] = (byte) (value >> 8);
            pgm_videoregs[offset * 2 + 1] = (byte) value;
        } else if (address >= 0xc0_0002 && address + 1 <= 0xc0_0003) {
            m68k_l1_w(value);
        } else if (address >= 0xc0_0004 && address + 1 <= 0xc0_0005) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0xc0_0006 && address + 1 <= 0xc0_0007) {
            pgm_calendar_w(value);
        } else if (address >= 0xc0_0008 && address + 1 <= 0xc0_0009) {
            z80_reset_w(value);
        } else if (address >= 0xc0_000a && address + 1 <= 0xc0_000b) {
            z80_ctrl_w();
        } else if (address >= 0xc0_000c && address + 1 <= 0xc0_000d) {
            Sound.soundlatch3_w(value);
        } else if (address >= 0xc1_0000 && address + 1 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            z80_ram_w(offset, (byte) (value >> 8));
            z80_ram_w(offset + 1, (byte) value);
        } else {
            int i1 = 1;
        }
    }

    public static void MWriteLong(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x70_0006 && address + 3 <= 0x70_0007) {
            //NOP;
        } else if (address >= 0x80_0000 && address + 3 <= 0x81_ffff) {
            int offset = address - 0x80_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x90_0000 && address + 3 <= 0x90_3fff) {
            int offset = (address - 0x90_0000) / 2;
            pgm_bg_videoram_w(offset, (short) (value >> 16));
            pgm_bg_videoram_w(offset + 1, (short) value);
        } else if (address >= 0x90_4000 && address + 3 <= 0x90_5fff) {
            int offset = (address - 0x90_4000) / 2;
            pgm_tx_videoram_w(offset, (short) (value >> 16));
            pgm_tx_videoram_w(offset + 1, (short) value);
        } else if (address >= 0x90_7000 && address + 3 <= 0x90_77ff) {
            int offset = (address - 0x90_7000) / 2;
            pgm_rowscrollram[offset * 2] = (byte) (value >> 24);
            pgm_rowscrollram[offset * 2 + 1] = (byte) (value >> 16);
            pgm_rowscrollram[offset * 2 + 2] = (byte) (value >> 8);
            pgm_rowscrollram[offset * 2 + 3] = (byte) value;
        } else if (address >= 0xa0_0000 && address + 3 <= 0xa0_11ff) {
            int offset = (address - 0xa0_0000) / 2;
            Generic.paletteram16[offset] = (short) (value >> 16);
            Generic.paletteram16[offset + 1] = (short) value;
            Generic.paletteram16_xRRRRRGGGGGBBBBB_word_w(offset);
            Generic.paletteram16_xRRRRRGGGGGBBBBB_word_w(offset + 1);
        } else if (address >= 0xb0_0000 && address + 3 <= 0xb0_ffff) {
            int offset = (address - 0xb0_0000) / 2;
            pgm_videoregs[offset * 2] = (byte) (value >> 24);
            pgm_videoregs[offset * 2 + 1] = (byte) (value >> 16);
            pgm_videoregs[offset * 2 + 2] = (byte) (value >> 8);
            pgm_videoregs[offset * 2 + 3] = (byte) value;
        }
            /*else if (address >= 0xc0_0002 && address + 3 <= 0xc0_0003)
            {
                m68k_l1_w((ushort)value);
            }
            else if (address >= 0xc0_0004 && address + 3 <= 0xc0_0005)
            {
                Sound.soundlatch2_w((ushort)value);
            }
            else if (address >= 0xc0_0006 && address + 3 <= 0xc0_0007)
            {
                pgm_calendar_w((ushort)value);
            }
            else if (address >= 0xc0_0008 && address + 3 <= 0xc0_0009)
            {
                z80_reset_w((ushort)value);
            }
            else if (address >= 0xc0_000a && address + 3 <= 0xc0_000b)
            {
                z80_ctrl_w();
            }
            else if (address >= 0xc0_000c && address + 3 <= 0xc0_000d)
            {
                Sound.soundlatch3_w((ushort)value);
            }*/
        else if (address >= 0xc1_0000 && address + 3 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            z80_ram_w(offset, (byte) (value >> 24));
            z80_ram_w(offset + 1, (byte) (value >> 16));
            z80_ram_w(offset + 2, (byte) (value >> 8));
            z80_ram_w(offset + 3, (byte) value);
        } else {
            int i1 = 1;
        }
    }

    public static byte ZReadMemory(short address) {
        byte result = Memory.audioram[address];
        return result;
    }

    public static void ZWriteMemory(short address, byte value) {
        Memory.audioram[address] = value;
    }

    public static byte ZReadHardware(short address) {
        byte result = 0;
        if (address >= 0x8000 && address <= 0x8003) {
            int offset = address - 0x8000;
            result = ICS2115.ics2115_r(offset);
        } else if (address >= 0x8100 && address <= 0x81ff) {
            result = (byte) Sound.soundlatch3_r();
        } else if (address >= 0x8200 && address <= 0x82ff) {
            result = (byte) Sound.soundlatch_r();
        } else if (address >= 0x8400 && address <= 0x84ff) {
            result = (byte) Sound.soundlatch2_r();
        }
        return result;
    }

    public static void ZWriteHardware(short address, byte value) {
        if (address >= 0x8000 && address <= 0x8003) {
            int offset = address - 0x8000;
            ICS2115.ics2115_w(offset, value);
        } else if (address >= 0x8100 && address <= 0x81ff) {
            z80_l3_w(value);
        } else if (address >= 0x8200 && address <= 0x82ff) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x8400 && address <= 0x84ff) {
            Sound.soundlatch2_w(value);
        }
    }

    public static int ZIRQCallback() {
        return 0;
    }

//#endregion

//#region Memory2

    public static byte MPReadByte_orlegend(int address) {
        byte result;
        address &= 0xff_ffff;
        if (address == 0xc0_400f) {
            result = (byte) pgm_asic3_r();
        } else {
            result = MReadByte(address);
        }
        return result;
    }

    public static short MPReadWord_orlegend(int address) {
        short result;
        address &= 0xff_ffff;
        if (address == 0xc0_400e) {
            result = pgm_asic3_r();
        } else {
            result = MReadWord(address);
        }
        return result;
    }

    public static void MPWriteByte_orlegend(int address, byte value) {
        address &= 0xff_ffff;
        if (address == 0xc0_4001) {
            pgm_asic3_reg_w(value);
        } else if (address == 0xc0_400f) {
            pgm_asic3_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MPWriteWord_orlegend(int address, short value) {
        address &= 0xff_ffff;
        if (address == 0xc0_4000) {
            pgm_asic3_reg_w(value);
        } else if (address == 0xc0_400e) {
            pgm_asic3_w(value);
        } else {
            MWriteWord(address, value);
        }
    }

    public static byte MPReadByte_drgw2(int address) {
        byte result;
        address &= 0xff_ffff;
        if (address >= 0xd8_0000 && address <= 0xd8_0003) {
            result = 0;
        } else {
            result = MReadByte(address);
        }
        return result;
    }

    public static short MPReadWord_drgw2(int address) {
        short result;
        address &= 0xff_ffff;
        if (address == 0xd8_0000 && address + 1 <= 0xd8_0003) {
            int offset = (address - 0xd8_0000) / 2;
            result = killbld_igs025_prot_r(offset);
        } else {
            result = MReadWord(address);
        }
        return result;
    }

    public static int MPReadLong_drgw2(int address) {
        int result;
        address &= 0xff_ffff;
        if (address == 0xd8_0000 && address + 3 <= 0xd8_0003) {
            result = 0;
        } else {
            result = MReadLong(address);
        }
        return result;
    }

    public static void MPWriteByte_drgw2(int address, byte value) {
        address &= 0xff_ffff;
        if (address == 0xd8_0000 && address <= 0xd8_0003) {
            int offset = (address - 0xd8_0000) / 2;
            //drgw2_d80000_protection_w(offset, (ushort)value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MPWriteWord_drgw2(int address, short value) {
        address &= 0xff_ffff;
        if (address == 0xd8_0000 && address + 1 <= 0xd8_0003) {
            int offset = (address - 0xd8_0000) / 2;
            drgw2_d80000_protection_w(offset, value);
        } else {
            MWriteWord(address, value);
        }
    }

    public static void MPWriteLong_drgw2(int address, int value) {
        address &= 0xff_ffff;
        if (address == 0xd8_0000 && address + 3 <= 0xd8_0003) {
            int offset = (address - 0xd8_0000) / 2;
            //drgw2_d80000_protection_w(offset, (short) value);
        } else {
            MWriteLong(address, value);
        }
    }

//#endregion

//#region State

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        writer.write(pgm_tx_videoram, 0, 0x2000);
        writer.write(pgm_bg_videoram, 0, 0x4000);
        writer.write(pgm_rowscrollram, 0, 0x800);
        writer.write(pgm_videoregs, 0, 0x1_0000);
        writer.write(CalVal);
        writer.write(CalMask);
        writer.write(CalCom);
        writer.write(CalCnt);
        writer.write(asic3_reg);
        writer.write(asic3_x);
        for (i = 0; i < 3; i++) {
            writer.write(asic3_latch[i]);
        }
        writer.write(asic3_hold);
        writer.write(asic3_hilo);
        for (i = 0; i < 0x900; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x901; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x2_0000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x1_0000);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        ICS2115.SaveStateBinary(writer);
        for (i = 0; i < 3; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 3; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(Sound.ics2115stream.output_sampindex);
        writer.write(Sound.ics2115stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i, j;
            pgm_tx_videoram = reader.readBytes(0x2000);
            pgm_bg_videoram = reader.readBytes(0x4000);
            pgm_rowscrollram = reader.readBytes(0x800);
            pgm_videoregs = reader.readBytes(0x1_0000);
            CalVal = reader.readByte();
            CalMask = reader.readByte();
            CalCom = reader.readByte();
            CalCnt = reader.readByte();
            asic3_reg = reader.readByte();
            asic3_x = reader.readByte();
            for (i = 0; i < 3; i++) {
                asic3_latch[i] = reader.readByte();
            }
            asic3_hold = reader.readUInt16();
            asic3_hilo = reader.readUInt16();
            for (i = 0; i < 0x900; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x901; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x2_0000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x1_0000);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            ICS2115.LoadStateBinary(reader);
            for (i = 0; i < 3; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 3; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            Sound.ics2115stream.output_sampindex = reader.readInt32();
            Sound.ics2115stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Pgmprot

    public static byte asic3_reg, asic3_x;
    public static final byte[] asic3_latch = new byte[3];
    public static short asic3_hold, asic3_hilo;

    public static int bt(int v, int bit) {
        if ((v & (1 << bit)) != 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public static void asic3_compute_hold(int y, int z) {
        short old = asic3_hold;
        asic3_hold = (short) ((old << 1) | (old >> 15));
        asic3_hold ^= 0x2bad;
        asic3_hold ^= (short) BIT(z, y);
        asic3_hold ^= (short) (BIT(asic3_x, 2) << 10);
        asic3_hold ^= (short) BIT(old, 5);
        switch (short4) {
            case 0:
            case 1:
                asic3_hold ^= (short) (BIT(old, 10) ^ BIT(old, 8) ^ (BIT(asic3_x, 0) << 1) ^ (BIT(asic3_x, 1) << 6) ^ (BIT(asic3_x, 3) << 14));
                break;
            case 2:
                asic3_hold ^= (short) (BIT(old, 10) ^ BIT(old, 8) ^ (BIT(asic3_x, 0) << 4) ^ (BIT(asic3_x, 1) << 6) ^ (BIT(asic3_x, 3) << 12));
                break;
            case 3:
                asic3_hold ^= (short) (BIT(old, 7) ^ BIT(old, 6) ^ (BIT(asic3_x, 0) << 4) ^ (BIT(asic3_x, 1) << 6) ^ (BIT(asic3_x, 3) << 12));
                break;
            case 4:
                asic3_hold ^= (short) (BIT(old, 7) ^ BIT(old, 6) ^ (BIT(asic3_x, 0) << 3) ^ (BIT(asic3_x, 1) << 8) ^ (BIT(asic3_x, 3) << 14));
                break;
        }
    }

    public static short pgm_asic3_r() {
        byte res = 0;
        switch (asic3_reg) {
            case 0x00:
                res = (byte) ((asic3_latch[0] & 0xf7) | ((short4 << 3) & 0x08));
                break;
            case 0x01:
                res = asic3_latch[1];
                break;
            case 0x02:
                res = (byte) ((asic3_latch[2] & 0x7f) | ((short4 << 6) & 0x80));
                break;
            case 0x03:
                res = (byte) BITSWAP8(asic3_hold, 5, 2, 9, 7, 10, 13, 12, 15);
                break;
            case 0x20:
                res = 0x49;
                break;
            case 0x21:
                res = 0x47;
                break;
            case 0x22:
                res = 0x53;
                break;
            case 0x24:
                res = 0x41;
                break;
            case 0x25:
                res = 0x41;
                break;
            case 0x26:
                res = 0x7f;
                break;
            case 0x27:
                res = 0x41;
                break;
            case 0x28:
                res = 0x41;
                break;
            case 0x2a:
                res = 0x3e;
                break;
            case 0x2b:
                res = 0x41;
                break;
            case 0x2c:
                res = 0x49;
                break;
            case 0x2d:
                res = (byte) 0xf9;
                break;
            case 0x2e:
                res = 0x0a;
                break;
            case 0x30:
                res = 0x26;
                break;
            case 0x31:
                res = 0x49;
                break;
            case 0x32:
                res = 0x49;
                break;
            case 0x33:
                res = 0x49;
                break;
            case 0x34:
                res = 0x32;
                break;
        }
        return res;
    }

    public static void pgm_asic3_w(short data) {
        //if(ACCESSING_BITS_0_7)
        {
            if (asic3_reg < 3)
                asic3_latch[asic3_reg] = (byte) (data << 1);
            else if (asic3_reg == 0x40) {
                asic3_hilo = (short) ((asic3_hilo << 8) | data);
            } else if (asic3_reg == 0x48) {
                asic3_x = 0;
                if ((asic3_hilo & 0x0090) == 0)
                    asic3_x |= 0x01;
                if ((asic3_hilo & 0x0006) == 0)
                    asic3_x |= 0x02;
                if ((asic3_hilo & 0x9000) == 0)
                    asic3_x |= 0x04;
                if ((asic3_hilo & 0x0a00) == 0)
                    asic3_x |= 0x08;
            } else if (asic3_reg >= 0x80 && asic3_reg <= 0x87) {
                asic3_compute_hold(asic3_reg & 0x07, data);
            } else if (asic3_reg == 0xa0) {
                asic3_hold = 0;
            }
        }
    }

    public static void pgm_asic3_reg_w(short data) {
        //if(ACCESSING_BITS_0_7)
        asic3_reg = (byte) (data & 0xff);
    }

//#endregion

//#region Inpput

    public static void loop_inputports_pgm_standard() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short2 &= ~0x0001;
        } else {
            short2 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short2 &= ~0x0002;
        } else {
            short2 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short0 &= ~0x0001;
        } else {
            short0 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short0 &= ~0x0100;
        } else {
            short0 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            short0 &= ~0x0010;
        } else {
            short0 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short0 &= ~0x0008;
        } else {
            short0 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short0 &= ~0x0004;
        } else {
            short0 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short0 &= ~0x0002;
        } else {
            short0 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short0 &= ~0x0020;
        } else {
            short0 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short0 &= ~0x0040;
        } else {
            short0 |= 0x0040;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short0 &= ~0x0080;
        } else {
            short0 |= 0x0080;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            short0 &= ~0x1000;
        } else {
            short0 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            short0 &= ~0x0800;
        } else {
            short0 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            short0 &= ~0x0400;
        } else {
            short0 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            short0 &= ~0x0200;
        } else {
            short0 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short0 &= ~0x2000;
        } else {
            short0 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short0 &= ~0x4000;
        } else {
            short0 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            short0 &= (short) ~0x8000;
        } else {
            short0 |= (short) 0x8000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short2 &= ~0x0200;
        } else {
            short2 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            short2 &= ~0x0020;
        } else {
            short2 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            short2 &= ~0x0080;
        } else {
            short2 |= 0x0080;
        }
    }

    public static void record_port() {
        if (short0 != short0_old || short1 != short1_old || short2 != short2_old || short3 != short3_old || short4 != short4_old) {
            short0_old = short0;
            short1_old = short1;
            short2_old = short2;
            short3_old = short3;
            short4_old = short4;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(short0);
            Mame.bwRecord.write(short1);
            Mame.bwRecord.write(short2);
            Mame.bwRecord.write(short3);
            Mame.bwRecord.write(short4);
        }
    }

    public static void replay_port() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                short0_old = Mame.brRecord.readInt16();
                short1_old = Mame.brRecord.readInt16();
                short2_old = Mame.brRecord.readInt16();
                short3_old = Mame.brRecord.readInt16();
                short4_old = Mame.brRecord.readInt16();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            short0 = short0_old;
            short1 = short1_old;
            short2 = short2_old;
            short3 = short3_old;
            short4 = short4_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Tilemap

    public static Tmap pgm_tx_tilemap, pgm_bg_tilemap;

    public static void tilemap_init() {
        int i;
        pgm_tx_tilemap = new Tmap();
        pgm_tx_tilemap.rows = 32;
        pgm_tx_tilemap.cols = 64;
        pgm_tx_tilemap.tilewidth = 8;
        pgm_tx_tilemap.tileheight = 8;
        pgm_tx_tilemap.width = 0x200;
        pgm_tx_tilemap.height = 0x100;
        pgm_tx_tilemap.enable = true;
        pgm_tx_tilemap.all_tiles_dirty = true;
        pgm_tx_tilemap.pixmap = new short[0x100 * 0x200];
        pgm_tx_tilemap.flagsmap = new byte[0x100][0x200];
        pgm_tx_tilemap.tileflags = new byte[0x20][0x40];
        pgm_tx_tilemap.tile_update3 = pgm_tx_tilemap::tile_updatePgmtx;
        pgm_tx_tilemap.tilemap_draw_instance3 = pgm_tx_tilemap::tilemap_draw_instancePgm;
        pgm_tx_tilemap.total_elements = 0x80_0000 / 0x20;
        pgm_tx_tilemap.pen_data = new byte[0x40];
        pgm_tx_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 15; i++) {
            pgm_tx_tilemap.pen_to_flags[0][i] = 0x10;
        }
        pgm_tx_tilemap.pen_to_flags[0][15] = 0;
        pgm_tx_tilemap.scrollrows = 1;
        pgm_tx_tilemap.scrollcols = 1;
        pgm_tx_tilemap.rowscroll = new int[pgm_tx_tilemap.scrollrows];
        pgm_tx_tilemap.colscroll = new int[pgm_tx_tilemap.scrollcols];
        pgm_bg_tilemap = new Tmap();
        pgm_bg_tilemap.cols = 64;
        pgm_bg_tilemap.rows = 64;
        pgm_bg_tilemap.tilewidth = 32;
        pgm_bg_tilemap.tileheight = 32;
        pgm_bg_tilemap.width = 0x800;
        pgm_bg_tilemap.height = 0x800;
        pgm_bg_tilemap.enable = true;
        pgm_bg_tilemap.all_tiles_dirty = true;
        pgm_bg_tilemap.pixmap = new short[0x800 * 0x800];
        pgm_bg_tilemap.flagsmap = new byte[0x800][0x800];
        pgm_bg_tilemap.tileflags = new byte[0x40][0x40];
        pgm_bg_tilemap.tile_update3 = pgm_bg_tilemap::tile_updatePgmbg;
        pgm_bg_tilemap.tilemap_draw_instance3 = pgm_bg_tilemap::tilemap_draw_instancePgm;
        pgm_bg_tilemap.total_elements = 0x3333;
        pgm_bg_tilemap.pen_data = new byte[0x400];
        pgm_bg_tilemap.pen_to_flags = new byte[1][32];
        for (i = 0; i < 31; i++) {
            pgm_bg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        pgm_bg_tilemap.pen_to_flags[0][31] = 0;
        pgm_bg_tilemap.scrollrows = 64 * 32;
        pgm_bg_tilemap.scrollcols = 1;
        pgm_bg_tilemap.rowscroll = new int[pgm_bg_tilemap.scrollrows];
        pgm_bg_tilemap.colscroll = new int[pgm_bg_tilemap.scrollcols];
    }

    public static class Tmap extends Tilemap.Tmap {

        public void tile_updatePgmtx(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int tileno, colour, flipyx;
            int code, tile_index, palette_base;
            byte flags;
            tile_index = row * PGM.pgm_tx_tilemap.cols + col;
            tileno = (PGM.pgm_tx_videoram[tile_index * 4] * 0x100 + PGM.pgm_tx_videoram[tile_index * 4 + 1]) & 0xffff;
            colour = (PGM.pgm_tx_videoram[tile_index * 4 + 3] & 0x3e) >> 1;
            flipyx = (PGM.pgm_tx_videoram[tile_index * 4 + 3] & 0xc0) >> 6;
            if (tileno > 0xbfff) {
                tileno -= 0xc000;
                tileno += 0x2_0000;
            }
            code = tileno % PGM.pgm_tx_tilemap.total_elements;
            flags = (byte) (flipyx & 3);
            palette_base = 0x800 + 0x10 * colour;
            tileflags[row][col] = tile_drawPgmtx(code * 0x40, x0, y0, palette_base, flags);
            //tileflags[row, col] = tile_apply_bitmaskPgmtx(code << 3, x0, y0, flags);
            //tileinfo_set( tileinfo, 0,tileno,colour,flipyx&3)
        }

        public byte tile_drawPgmtx(int pendata_offset, int x0, int y0, int palette_base, byte flags) {
            int height = tileheight;
            int width = tilewidth;
            int dx0 = 1, dy0 = 1;
            int tx, ty;
            int offset1 = 0;
            int offsety1;
            int xoffs;
            byte andmask = (byte) 0xff, ormask = 0;
            byte pen, map;
            System.arraycopy(PGM.tiles1rom, pendata_offset, pen_data, 0, 0x40);
            if ((flags & Tilemap.TILE_FLIPY) != 0) {
                y0 += height - 1;
                dy0 = -1;
            }
            if ((flags & Tilemap.TILE_FLIPX) != 0) {
                x0 += width - 1;
                dx0 = -1;
            }
            for (ty = 0; ty < height; ty++) {
                xoffs = 0;
                offsety1 = y0;
                y0 += dy0;
                for (tx = 0; tx < width; tx++) {
                    pen = pen_data[offset1];
                    map = pen_to_flags[0][pen];
                    offset1++;
                    pixmap[offsety1 * 0x200 + x0 + xoffs] = (short) (palette_base + pen);
                    flagsmap[offsety1][x0 + xoffs] = map;
                    andmask &= map;
                    ormask |= map;
                    xoffs += dx0;
                }
            }
            return (byte) (andmask ^ ormask);
        }

        public void tilemap_draw_instancePgm(RECT cliprect, int xpos, int ypos) {
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
                    if (column == maxcol)
                        cur_trans = trans_t.WHOLLY_TRANSPARENT;
                    else {
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
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    Tilemap.priority_bitmap[offsety2 + ypos][i] = priority;
                                }
                                offsety2++;
                            }
                        } else if (prev_trans == trans_t.MASKED) {
                            for (cury = y; cury < nexty; cury++) {
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                        Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x200 + i] = (short) (pixmap[offsety2 * width + i - xpos] + palette_offset);
                                        Tilemap.priority_bitmap[offsety2 + ypos][i] = priority;
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

        public void tile_updatePgmbg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int tileno, colour, flipyx;
            int code, tile_index, palette_base;
            byte flags;
            tile_index = row * PGM.pgm_bg_tilemap.cols + col;
            tileno = (PGM.pgm_bg_videoram[tile_index * 4] * 0x100 + PGM.pgm_bg_videoram[tile_index * 4 + 1]) & 0xffff;
            if (tileno > 0x7ff) {
                tileno += 0x1000;
            }
            colour = (PGM.pgm_bg_videoram[tile_index * 4 + 3] & 0x3e) >> 1;
            flipyx = (PGM.pgm_bg_videoram[tile_index * 4 + 3] & 0xc0) >> 6;
            code = tileno % PGM.pgm_bg_tilemap.total_elements;
            flags = (byte) (flipyx & 3);
            palette_base = 0x400 + 0x20 * colour;
            tileflags[row][col] = tile_drawPgmbg(code * 0x400, x0, y0, palette_base, flags);
        }

        public byte tile_drawPgmbg(int pendata_offset, int x0, int y0, int palette_base, byte flags) {
            int height = tileheight;
            int width = tilewidth;
            int dx0 = 1, dy0 = 1;
            int tx, ty;
            int offset1 = 0;
            int offsety1;
            int xoffs;
            byte andmask = (byte) 0xff, ormask = 0;
            byte pen, map;
            System.arraycopy(PGM.tiles2rom, pendata_offset, pen_data, 0, 0x400);
            if ((flags & Tilemap.TILE_FLIPY) != 0) {
                y0 += height - 1;
                dy0 = -1;
            }
            if ((flags & Tilemap.TILE_FLIPX) != 0) {
                x0 += width - 1;
                dx0 = -1;
            }
            for (ty = 0; ty < height; ty++) {
                xoffs = 0;
                offsety1 = y0;
                y0 += dy0;
                for (tx = 0; tx < width; tx++) {
                    pen = pen_data[offset1];
                    map = pen_to_flags[0][pen];
                    offset1++;
                    pixmap[offsety1 * 0x800 + x0 + xoffs] = (short) (palette_base + pen);
                    flagsmap[offsety1][x0 + xoffs] = map;
                    andmask &= map;
                    ormask |= map;
                    xoffs += dx0;
                }
            }
            return (byte) (andmask ^ ormask);
        }
    }

//#endregion

//#region Video

    public static short[] pgm_spritebufferram; // buffered spriteram
    public static short[] sprite_temp_render;
    private static short[] uu900;
    private static int pgm_sprite_source_offset;

    private static void pgm_prepare_sprite(int wide, int high, int palt, int boffset) {
        int bdatasize = sprmaskrom.length - 1;
        int adatasize = pgm_sprite_a_region.length - 1;
        int xcnt, ycnt, aoffset, x;
        short msk;
        aoffset = (sprmaskrom[(boffset + 3) & bdatasize] << 24) | (sprmaskrom[(boffset + 2) & bdatasize] << 16) | (sprmaskrom[(boffset + 1) & bdatasize] << 8) | (sprmaskrom[(boffset + 0) & bdatasize] << 0);
        aoffset = aoffset >> 2;
        aoffset *= 3;
        boffset += 4;
        for (ycnt = 0; ycnt < high; ycnt++) {
            for (xcnt = 0; xcnt < wide; xcnt++) {
                msk = (short) ((sprmaskrom[(boffset + 1) & bdatasize] << 8) | (sprmaskrom[(boffset + 0) & bdatasize] << 0));
                for (x = 0; x < 16; x++) {
                    if ((msk & 0x0001) == 0) {
                        sprite_temp_render[(ycnt * (wide * 16)) + (xcnt * 16 + x)] = (short) (pgm_sprite_a_region[aoffset & adatasize] + palt * 32);
                        aoffset++;
                    } else {
                        sprite_temp_render[(ycnt * (wide * 16)) + (xcnt * 16 + x)] = (short) 0x8000;
                    }
                    msk >>= 1;
                }
                boffset += 2;
            }
        }
    }

    private static void pgm_draw_pix(int xdrawpos, int ydrawpos, int pri, short srcdat) {
        if ((xdrawpos >= 0) && (xdrawpos < 448)) {
            if ((Tilemap.priority_bitmap[ydrawpos][xdrawpos] & 1) == 0) {
                if (pri == 0) {
                    Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
                } else {
                    if ((Tilemap.priority_bitmap[ydrawpos][xdrawpos] & 2) == 0) {
                        Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
                    }
                }
            }
            Tilemap.priority_bitmap[ydrawpos][xdrawpos] |= 1;
        }
    }

    private static void pgm_draw_pix_nopri(int xdrawpos, int ydrawpos, short srcdat) {
        if ((xdrawpos >= 0) && (xdrawpos < 448)) {
            if ((Tilemap.priority_bitmap[ydrawpos][xdrawpos] & 1) == 0) {
                Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
            }
            Tilemap.priority_bitmap[ydrawpos][xdrawpos] |= 1;
        }
    }

    private static void pgm_draw_pix_pri(int xdrawpos, int ydrawpos, short srcdat) {
        if ((xdrawpos >= 0) && (xdrawpos < 448)) {
            if ((Tilemap.priority_bitmap[ydrawpos][xdrawpos] & 1) == 0) {
                if ((Tilemap.priority_bitmap[ydrawpos][xdrawpos] & 2) == 0) {
                    Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
                }
            }
            Tilemap.priority_bitmap[ydrawpos][xdrawpos] |= 1;
        }
    }

    private static void draw_sprite_line(int wide, int ydrawpos, int xzoom, int xgrow, int yoffset, int flip, int xpos) {
        int xcnt, xcntdraw;
        int xzoombit;
        int xoffset;
        int xdrawpos = 0;
        short srcdat;
        xcnt = 0;
        xcntdraw = 0;
        while (xcnt < wide * 16) {
            if ((flip & 0x01) == 0)
                xoffset = xcnt;
            else
                xoffset = (wide * 16) - xcnt - 1;
            srcdat = sprite_temp_render[yoffset + xoffset];
            xzoombit = (xzoom >> (xcnt & 0x1f)) & 1;
            if (xzoombit == 1 && xgrow == 1) {
                xdrawpos = xpos + xcntdraw;
                if ((srcdat & 0x8000) == 0) {
                    if ((xdrawpos >= 0) && (xdrawpos < 448))
                        Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
                }
                xcntdraw++;
                xdrawpos = xpos + xcntdraw;
                if ((srcdat & 0x8000) == 0) {
                    if ((xdrawpos >= 0) && (xdrawpos < 448))
                        Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
                }
                xcntdraw++;
            } else if (xzoombit == 1 && xgrow == 0) {

            } else {
                xdrawpos = xpos + xcntdraw;
                if ((srcdat & 0x8000) == 0) {
                    if ((xdrawpos >= 0) && (xdrawpos < 448))
                        Video.bitmapbase[Video.curbitmap][ydrawpos * 0x200 + xdrawpos] = srcdat;
                }
                xcntdraw++;
            }
            xcnt++;
            if (xdrawpos == 448)
                xcnt = wide * 16;
        }
    }

    private static void draw_sprite_new_zoomed(int wide, int high, int xpos, int ypos, int palt, int boffset, int flip, int xzoom, int xgrow, int yzoom, int ygrow) {
        int ycnt;
        int ydrawpos;
        int yoffset;
        int ycntdraw;
        int yzoombit;
        pgm_prepare_sprite(wide, high, palt, boffset);
        ycnt = 0;
        ycntdraw = 0;
        while (ycnt < high) {
            yzoombit = (yzoom >> (ycnt & 0x1f)) & 1;
            if (yzoombit == 1 && ygrow == 1) {
                ydrawpos = ypos + ycntdraw;
                if ((flip & 0x02) == 0)
                    yoffset = (ycnt * (wide * 16));
                else
                    yoffset = ((high - ycnt - 1) * (wide * 16));
                if ((ydrawpos >= 0) && (ydrawpos < 224)) {
                    draw_sprite_line(wide, ydrawpos, xzoom, xgrow, yoffset, flip, xpos);
                }
                ycntdraw++;
                ydrawpos = ypos + ycntdraw;
                if ((flip & 0x02) == 0)
                    yoffset = (ycnt * (wide * 16));
                else
                    yoffset = ((high - ycnt - 1) * (wide * 16));
                if ((ydrawpos >= 0) && (ydrawpos < 224)) {
                    draw_sprite_line(wide, ydrawpos, xzoom, xgrow, yoffset, flip, xpos);
                }
                ycntdraw++;
                if (ydrawpos == 224)
                    ycnt = high;
            } else if (yzoombit == 1 && ygrow == 0) {

            } else {
                ydrawpos = ypos + ycntdraw;
                if ((flip & 0x02) == 0)
                    yoffset = (ycnt * (wide * 16));
                else
                    yoffset = ((high - ycnt - 1) * (wide * 16));
                if ((ydrawpos >= 0) && (ydrawpos < 224)) {
                    draw_sprite_line(wide, ydrawpos, xzoom, xgrow, yoffset, flip, xpos);
                }
                ycntdraw++;
                if (ydrawpos == 224)
                    ycnt = high;
            }
            ycnt++;
        }
    }

    private static void draw_sprites(int priority) {
        while (pgm_sprite_source_offset < 0x500) {
            int xpos = pgm_spritebufferram[pgm_sprite_source_offset + 0] & 0x07ff;
            int ypos = pgm_spritebufferram[pgm_sprite_source_offset + 1] & 0x03ff;
            int xzom = (pgm_spritebufferram[pgm_sprite_source_offset + 0] & 0x7800) >> 11;
            int xgrow = (pgm_spritebufferram[pgm_sprite_source_offset + 0] & 0x8000) >> 15;
            int yzom = (pgm_spritebufferram[pgm_sprite_source_offset + 1] & 0x7800) >> 11;
            int ygrow = (pgm_spritebufferram[pgm_sprite_source_offset + 1] & 0x8000) >> 15;
            int palt = (pgm_spritebufferram[pgm_sprite_source_offset + 2] & 0x1f00) >> 8;
            int flip = (pgm_spritebufferram[pgm_sprite_source_offset + 2] & 0x6000) >> 13;
            int boff = ((pgm_spritebufferram[pgm_sprite_source_offset + 2] & 0x007f) << 16) | (pgm_spritebufferram[pgm_sprite_source_offset + 3] & 0xffff);
            int wide = (pgm_spritebufferram[pgm_sprite_source_offset + 4] & 0x7e00) >> 9;
            int high = pgm_spritebufferram[pgm_sprite_source_offset + 4] & 0x01ff;
            int pri = (pgm_spritebufferram[pgm_sprite_source_offset + 2] & 0x0080) >> 7;
            int xzoom, yzoom;
            int pgm_sprite_zoomtable_offset = 0x1000;
            if (xgrow != 0) {
                xzom = 0x10 - xzom;
            }
            if (ygrow != 0) {
                yzom = 0x10 - yzom;
            }
            xzoom = ((pgm_videoregs[pgm_sprite_zoomtable_offset + xzom * 4] * 0x100 + pgm_videoregs[pgm_sprite_zoomtable_offset + xzom * 4 + 1]) << 16) | (pgm_videoregs[pgm_sprite_zoomtable_offset + xzom * 4 + 2] * 0x100 + pgm_videoregs[pgm_sprite_zoomtable_offset + xzom * 4 + 3]);
            yzoom = ((pgm_videoregs[pgm_sprite_zoomtable_offset + yzom * 4] * 0x100 + pgm_videoregs[pgm_sprite_zoomtable_offset + yzom * 4 + 1]) << 16) | (pgm_videoregs[pgm_sprite_zoomtable_offset + yzom * 4 + 2] * 0x100 + pgm_videoregs[pgm_sprite_zoomtable_offset + yzom * 4 + 3]);
            boff *= 2;
            if (xpos > 0x3ff)
                xpos -= 0x800;
            if (ypos > 0x1ff)
                ypos -= 0x400;
            if (high == 0)
                break;
            if ((priority == 1) && (pri == 0))
                break;
            draw_sprite_new_zoomed(wide, high, xpos, ypos, palt, boff, flip, xzoom, xgrow, yzoom, ygrow);
            pgm_sprite_source_offset += 5;
        }
    }

    private static void pgm_tx_videoram_w(int offset, byte data) {
        int col, row;
        pgm_tx_videoram[offset] = data;
        col = (offset / 4) % 64;
        row = (offset / 4) / 64;
        pgm_tx_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    private static void pgm_tx_videoram_w(int offset, short data) {
        int col, row;
        pgm_tx_videoram[offset * 2] = (byte) (data >> 8);
        pgm_tx_videoram[offset * 2 + 1] = (byte) data;
        col = (offset / 2) % 64;
        row = (offset / 2) / 64;
        pgm_tx_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    private static void pgm_bg_videoram_w(int offset, byte data) {
        int col, row;
        pgm_bg_videoram[offset] = data;
        col = (offset / 4) % 64;
        row = (offset / 4) / 64;
        pgm_bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    private static void pgm_bg_videoram_w(int offset, short data) {
        int col, row;
        pgm_bg_videoram[offset * 2] = (byte) (data >> 8);
        pgm_bg_videoram[offset * 2 + 1] = (byte) data;
        col = (offset / 2) % 64;
        row = (offset / 2) / 64;
        pgm_bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void video_start_pgm() {
        int i;
        uu900 = new short[0x200 * 0x200];
        for (i = 0; i < 0x4_0000; i++) {
            uu900[i] = 0x900;
        }
        pgm_spritebufferram = new short[0xa00 / 2];
        sprite_temp_render = new short[0x400 * 0x200];
    }

    public static void video_update_pgm() {
        int y;
        RECT new_clip = new RECT();
        new_clip.min_x = 0x00;
        new_clip.max_x = 0x1bf;
        new_clip.min_y = 0x00;
        new_clip.max_y = 0xdf;
        System.arraycopy(uu900, 0, Video.bitmapbase[Video.curbitmap], 0, 0x4_0000);
        pgm_sprite_source_offset = 0;
        draw_sprites(1);
        pgm_bg_tilemap.tilemap_set_scrolly(0, pgm_videoregs[0x2000] * 0x100 + pgm_videoregs[0x2000 + 1]);
        for (y = 0; y < 224; y++) {
            pgm_bg_tilemap.tilemap_set_scrollx((y + pgm_videoregs[0x2000] * 0x100 + pgm_videoregs[0x2000 + 1]) & 0x7ff, pgm_videoregs[0x3000] * 0x100 + pgm_videoregs[0x3000 + 1] + pgm_rowscrollram[y * 2] * 0x100 + pgm_rowscrollram[y * 2 + 1]);
        }
        pgm_bg_tilemap.tilemap_draw_primask(new_clip, 0x10, (byte) 0);
        draw_sprites(0);
        //draw_sprites();
        pgm_tx_tilemap.tilemap_set_scrolly(0, pgm_videoregs[0x5000] * 0x100 + pgm_videoregs[0x5000 + 1]);
        pgm_tx_tilemap.tilemap_set_scrollx(0, pgm_videoregs[0x6000] * 0x100 + pgm_videoregs[0x6000 + 1]);
        pgm_tx_tilemap.tilemap_draw_primask(new_clip, 0x10, (byte) 0);
    }

    public static void video_eof_pgm() {
        int i;
        for (i = 0; i < 0x500; i++) {
            pgm_spritebufferram[i] = (short) (Memory.mainram[i * 2] * 0x100 + Memory.mainram[i * 2 + 1]);
        }
    }

//#endregion
}
