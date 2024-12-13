/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.capcom;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m68000.MC68000;
import m1.cpu.m6809.M6809;
import m1.cpu.z80.Z80A;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
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
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.AY8910;
import m1.sound.MSM5205;
import m1.sound.Sound;
import m1.sound.YM2151;
import m1.sound.YM2203;


public class Capcom {

    public static byte[] audiorom2;
    public static int basebankmain, basebanksnd1;
    public static byte[] gfx1rom, gfx2rom, gfx3rom, gfx4rom, gfx5rom, gfx12rom, gfx22rom, gfx32rom, gfx42rom;
    public static short dsw1, dsw2;
    public static byte bytedsw1, bytedsw2;
    public static short[] sf_objectram, sf_videoram;
    public static final int[] scale = new int[] {
            0x00, 0x40, 0xe0, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe
    };

    public static void CapcomInit() {
        int i, n;
        Machine.bRom = true;
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
                Generic.spriteram = new byte[0x200];
                Generic.buffered_spriteram = new byte[0x200];
                Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx12rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                gfx22rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
                n = gfx22rom.length;
                gfx2rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx2rom[i * 2] = (byte) (gfx22rom[i] >> 4);
                    gfx2rom[i * 2 + 1] = (byte) (gfx22rom[i] & 0x0f);
                }
                gfx32rom = new byte[0x1_0000]; // Machine.GetRom("gfx3.rom");
                n = gfx32rom.length;
                gfx3rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx3rom[i * 2] = (byte) (gfx32rom[i] >> 4);
                    gfx3rom[i * 2 + 1] = (byte) (gfx32rom[i] & 0x0f);
                }
                Memory.mainram = new byte[0x1e00];
                Memory.audioram = new byte[0x800];
                Generic.paletteram = new byte[0x100];
                Generic.paletteram_2 = new byte[0x100];
                if (Memory.mainrom == null || Memory.audiorom == null || gfx12rom == null || gfx22rom == null || gfx32rom == null) {
                    Machine.bRom = false;
                }
                break;
            case "sf":
            case "sfua":
            case "sfj":
            case "sfjan":
            case "sfan":
            case "sfp":
                sf_objectram = new short[0x1000];
                sf_videoram = new short[0x800];
                Generic.paletteram16 = new short[0x400];
                Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                audiorom2 = Machine.GetRom("audio2.rom");
                gfx12rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                gfx22rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
                n = gfx22rom.length;
                gfx2rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx2rom[i * 2] = (byte) (gfx22rom[i] >> 4);
                    gfx2rom[i * 2 + 1] = (byte) (gfx22rom[i] & 0x0f);
                }
                gfx32rom = new byte[0x1_0000]; // Machine.GetRom("gfx3.rom");
                n = gfx32rom.length;
                gfx3rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx3rom[i * 2] = (byte) (gfx32rom[i] >> 4);
                    gfx3rom[i * 2 + 1] = (byte) (gfx32rom[i] & 0x0f);
                }
                gfx42rom = new byte[0x1_0000]; // Machine.GetRom("gfx4.rom");
                n = gfx42rom.length;
                gfx4rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx4rom[i * 2] = (byte) (gfx42rom[i] >> 4);
                    gfx4rom[i * 2 + 1] = (byte) (gfx42rom[i] & 0x0f);
                }
                gfx5rom = new byte[0x4_0000]; // Machine.GetRom("gfx5.rom");
                Memory.mainram = new byte[0x6000];
                Memory.audioram = new byte[0x800];
                if (Memory.mainrom == null || gfx12rom == null || Memory.audiorom == null) {
                    Machine.bRom = false;
                }
                break;
        }
        if (Machine.bRom) {
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
                    bytedsw1 = (byte) 0xdf;
                    bytedsw2 = (byte) 0xfb;
                    break;
                case "diamond":
                    bytedsw1 = (byte) 0x81;
                    bytedsw2 = 0x07;
                    break;
                case "sf":
                case "sfua":
                case "sfj":
                    dsw1 = (short) 0xdfff;
                    dsw2 = (short) 0xfbff;
                    shorts = (short) 0xff7f;
                    break;
                case "sfjan":
                case "sfan":
                case "sfp":
                    dsw1 = (short) 0xdfff;
                    dsw2 = (short) 0xffff;
                    shorts = (short) 0xff7f;
                    break;
            }
        }
    }

    public static short dummy_r() {
        return (short) 0xffff;
    }

    public static void sf_coin_w() {
//        if (ACCESSING_BITS_0_7) {
//            coin_counter_w(0, data & 0x01);
//            coin_counter_w(1, data & 0x02);
//            coin_lockout_w(0, ~data & 0x10);
//            coin_lockout_w(1, ~data & 0x20);
//            coin_lockout_w(2, ~data & 0x40);
//        }
    }

    public static void sf_coin_w2() {

    }

    public static void soundcmd_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            Sound.soundlatch_w((short) (data & 0xff));
            Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
        }
    }

    public static void soundcmd_w2(byte data) {
        Sound.soundlatch_w((short) (data & 0xff));
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
    }

    public static void write_dword(int offset, int data) {
        MC68000.m1.WriteWord.accept(offset, (short) (data >> 16));
        MC68000.m1.WriteWord.accept(offset + 2, (short) data);
    }

    public static void protection_w(short data) {
        int[][] maplist = new int[][] {
                {1, 0, 3, 2, 4, 5, 6, 7, 8, 9},
                {4, 5, 6, 7, 1, 0, 3, 2, 8, 9},
                {3, 2, 1, 0, 6, 7, 4, 5, 8, 9},
                {6, 7, 4, 5, 3, 2, 1, 0, 8, 9}
        };
        int map;
        map = maplist[MC68000.m1.ReadByte.apply(0xff_c006)][(MC68000.m1.ReadByte.apply(0xff_c003) << 1) + (MC68000.m1.ReadWord.apply(0xff_c004) >> 8)];
        switch (MC68000.m1.ReadByte.apply(0xff_c684)) {
            case 1: {
                int base1;
                base1 = 0x1_b6e8 + 0x300e * map;
                write_dword(0xff_c01c, 0x1_6bfc + 0x270 * map);
                write_dword(0xff_c020, base1 + 0x80);
                write_dword(0xff_c024, base1);
                write_dword(0xff_c028, base1 + 0x86);
                write_dword(0xff_c02c, base1 + 0x8e);
                write_dword(0xff_c030, base1 + 0x20e);
                write_dword(0xff_c034, base1 + 0x30e);
                write_dword(0xff_c038, base1 + 0x38e);
                write_dword(0xff_c03c, base1 + 0x40e);
                write_dword(0xff_c040, base1 + 0x80e);
                write_dword(0xff_c044, base1 + 0xc0e);
                write_dword(0xff_c048, base1 + 0x180e);
                write_dword(0xff_c04c, base1 + 0x240e);
                write_dword(0xff_c050, 0x1_9548 + 0x60 * map);
                write_dword(0xff_c054, 0x1_9578 + 0x60 * map);
                break;
            }
            case 2: {
                int[] delta1 = new int[] {
                        0x1f80, 0x1c80, 0x2700, 0x2400, 0x2b80, 0x2e80, 0x3300, 0x3600, 0x3a80, 0x3d80
                };
                int[] delta2 = new int[] {
                        0x2180, 0x1800, 0x3480, 0x2b00, 0x3e00, 0x4780, 0x5100, 0x5a80, 0x6400, 0x6d80
                };
                int d1 = delta1[map] + 0xc0;
                int d2 = delta2[map];
                MC68000.m1.WriteWord.accept(0xff_c680, (short) d1);
                MC68000.m1.WriteWord.accept(0xff_c682, (short) d2);
                MC68000.m1.WriteWord.accept(0xff_c00c, (short) 0xc0);
                MC68000.m1.WriteWord.accept(0xff_c00e, (short) 0);
                sf_fg_scroll_w((short) d1);
                sf_bg_scroll_w((short) d2);
                break;
            }
            case 4: {
                int pos = MC68000.m1.ReadByte.apply(0xff_c010);
                pos = (pos + 1) & 3;
                MC68000.m1.WriteByte.accept(0xff_c010, (byte) pos);
                if (pos == 0) {
                    int d1 = MC68000.m1.ReadWord.apply(0xff_c682);
                    int off = MC68000.m1.ReadWord.apply(0xff_c00e);
                    if (off != 512) {
                        off++;
                        d1++;
                    } else {
                        off = 0;
                        d1 -= 512;
                    }
                    MC68000.m1.WriteWord.accept(0xff_c682, (short) d1);
                    MC68000.m1.WriteWord.accept(0xff_c00e, (short) off);
                    sf_bg_scroll_w((short) d1);
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    public static void protection_w1(byte data) {
        int[][] maplist = new int[][] {
                {1, 0, 3, 2, 4, 5, 6, 7, 8, 9},
                {4, 5, 6, 7, 1, 0, 3, 2, 8, 9},
                {3, 2, 1, 0, 6, 7, 4, 5, 8, 9},
                {6, 7, 4, 5, 3, 2, 1, 0, 8, 9}
        };
        int map;
        map = maplist[MC68000.m1.ReadByte.apply(0xff_c006)][(MC68000.m1.ReadByte.apply(0xff_c003) << 1) + (MC68000.m1.ReadWord.apply(0xff_c004) >> 8)];
        switch (MC68000.m1.ReadByte.apply(0xff_c684)) {
            case 1: {
                int base1;
                base1 = 0x1_b6e8 + 0x300e * map;
                write_dword(0xff_c01c, 0x1_6bfc + 0x270 * map);
                write_dword(0xff_c020, base1 + 0x80);
                write_dword(0xff_c024, base1);
                write_dword(0xff_c028, base1 + 0x86);
                write_dword(0xff_c02c, base1 + 0x8e);
                write_dword(0xff_c030, base1 + 0x20e);
                write_dword(0xff_c034, base1 + 0x30e);
                write_dword(0xff_c038, base1 + 0x38e);
                write_dword(0xff_c03c, base1 + 0x40e);
                write_dword(0xff_c040, base1 + 0x80e);
                write_dword(0xff_c044, base1 + 0xc0e);
                write_dword(0xff_c048, base1 + 0x180e);
                write_dword(0xff_c04c, base1 + 0x240e);
                write_dword(0xff_c050, 0x1_9548 + 0x60 * map);
                write_dword(0xff_c054, 0x1_9578 + 0x60 * map);
                break;
            }
            case 2: {
                int[] delta1 = new int[] {
                        0x1f80, 0x1c80, 0x2700, 0x2400, 0x2b80, 0x2e80, 0x3300, 0x3600, 0x3a80, 0x3d80
                };
                int[] delta2 = new int[] {
                        0x2180, 0x1800, 0x3480, 0x2b00, 0x3e00, 0x4780, 0x5100, 0x5a80, 0x6400, 0x6d80
                };
                int d1 = delta1[map] + 0xc0;
                int d2 = delta2[map];
                MC68000.m1.WriteWord.accept(0xff_c680, (short) d1);
                MC68000.m1.WriteWord.accept(0xff_c682, (short) d2);
                MC68000.m1.WriteWord.accept(0xff_c00c, (short) 0xc0);
                MC68000.m1.WriteWord.accept(0xff_c00e, (short) 0);
                sf_fg_scroll_w1((byte) (d1 >> 8));
                sf_bg_scroll_w((byte) (d2 >> 8));
                break;
            }
            case 4: {
                int pos = MC68000.m1.ReadByte.apply(0xff_c010);
                pos = (pos + 1) & 3;
                MC68000.m1.WriteByte.accept(0xff_c010, (byte) pos);
                if (pos == 0) {
                    int d1 = MC68000.m1.ReadWord.apply(0xff_c682);
                    int off = MC68000.m1.ReadWord.apply(0xff_c00e);
                    if (off != 512) {
                        off++;
                        d1++;
                    } else {
                        off = 0;
                        d1 -= 512;
                    }
                    MC68000.m1.WriteWord.accept(0xff_c682, (short) d1);
                    MC68000.m1.WriteWord.accept(0xff_c00e, (short) off);
                    sf_bg_scroll_w((byte) (d1 >> 8));
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    public static void protection_w2(byte data) {
        int[][] maplist = new int[][] {
                {
                        1, 0, 3, 2, 4, 5, 6, 7, 8, 9
                },
                {
                        4, 5, 6, 7, 1, 0, 3, 2, 8, 9
                },
                {
                        3, 2, 1, 0, 6, 7, 4, 5, 8, 9
                },
                {
                        6, 7, 4, 5, 3, 2, 1, 0, 8, 9
                }
        };
        int map;
        map = maplist[MC68000.m1.ReadByte.apply(0xff_c006)][(MC68000.m1.ReadByte.apply(0xff_c003) << 1) + (MC68000.m1.ReadWord.apply(0xff_c004) >> 8)];
        switch (MC68000.m1.ReadByte.apply(0xff_c684)) {
            case 1: {
                int base1;
                base1 = 0x1_b6e8 + 0x300e * map;
                write_dword(0xff_c01c, 0x1_6bfc + 0x270 * map);
                write_dword(0xff_c020, base1 + 0x80);
                write_dword(0xff_c024, base1);
                write_dword(0xff_c028, base1 + 0x86);
                write_dword(0xff_c02c, base1 + 0x8e);
                write_dword(0xff_c030, base1 + 0x20e);
                write_dword(0xff_c034, base1 + 0x30e);
                write_dword(0xff_c038, base1 + 0x38e);
                write_dword(0xff_c03c, base1 + 0x40e);
                write_dword(0xff_c040, base1 + 0x80e);
                write_dword(0xff_c044, base1 + 0xc0e);
                write_dword(0xff_c048, base1 + 0x180e);
                write_dword(0xff_c04c, base1 + 0x240e);
                write_dword(0xff_c050, 0x1_9548 + 0x60 * map);
                write_dword(0xff_c054, 0x1_9578 + 0x60 * map);
                break;
            }
            case 2: {
                int[] delta1 = new int[] {
                        0x1f80, 0x1c80, 0x2700, 0x2400, 0x2b80, 0x2e80, 0x3300, 0x3600, 0x3a80, 0x3d80
                };
                int[] delta2 = new int[] {
                        0x2180, 0x1800, 0x3480, 0x2b00, 0x3e00, 0x4780, 0x5100, 0x5a80, 0x6400, 0x6d80
                };
                int d1 = delta1[map] + 0xc0;
                int d2 = delta2[map];
                MC68000.m1.WriteWord.accept(0xff_c680, (short) d1);
                MC68000.m1.WriteWord.accept(0xff_c682, (short) d2);
                MC68000.m1.WriteWord.accept(0xff_c00c, (short) 0xc0);
                MC68000.m1.WriteWord.accept(0xff_c00e, (short) 0);
                sf_fg_scroll_w((byte) d1);
                sf_bg_scroll_w((byte) d2);
                break;
            }
            case 4: {
                int pos = MC68000.m1.ReadByte.apply(0xff_c010);
                pos = (pos + 1) & 3;
                MC68000.m1.WriteByte.accept(0xff_c010, (byte) pos);
                if (pos == 0) {
                    int d1 = MC68000.m1.ReadWord.apply(0xff_c682);
                    int off = MC68000.m1.ReadWord.apply(0xff_c00e);
                    if (off != 512) {
                        off++;
                        d1++;
                    } else {
                        off = 0;
                        d1 -= 512;
                    }
                    MC68000.m1.WriteWord.accept(0xff_c682, (short) d1);
                    MC68000.m1.WriteWord.accept(0xff_c00e, (short) off);
                    sf_bg_scroll_w((byte) d1);
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    public static short button1_r() {
        return (short) ((scale[sbyte3] << 8) | scale[sbyte1]);
    }

    public static short button2_r() {
        return (short) ((scale[sbyte4] << 8) | scale[sbyte2]);
    }

    public static void msm5205_w(int offset, byte data) {
        MSM5205.msm5205_reset_w(offset, (data >> 7) & 1);
        /* ?? bit 6?? */
        MSM5205.msm5205_data_w(offset, data);
        MSM5205.msm5205_vclk_w(offset, 1);
        MSM5205.msm5205_vclk_w(offset, 0);
    }

    public static void irq_handler(int irq) {
        Cpuint.cpunum_set_input_line(1, 0, (irq != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void machine_reset_capcom() {

    }

    public static void play_capcom_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_capcom_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_capcom_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//#region Gng

    public static byte[] gng_fgvideoram, gng_bgvideoram;
    public static byte[] scrollx, scrolly;

    public static void gng_bankswitch_w(byte data) {
        if (data == 4) {
            basebankmain = 0x4000;
        } else {
            basebankmain = 0x1_0000 + 0x2000 * (data & 0x03);
        }
    }

    public static void gng_coin_counter_w(int offset, byte data) {
        Generic.coin_counter_w(offset, data);
    }

    public static void video_start_gng() {
        gng_fgvideoram = new byte[0x800];
        gng_bgvideoram = new byte[0x800];
        scrollx = new byte[2];
        scrolly = new byte[2];
    }

    public static void gng_fgvideoram_w(int offset, byte data) {
        gng_fgvideoram[offset] = data;
        int row, col;
        row = (offset & 0x3ff) / 0x20;
        col = (offset & 0x3ff) % 0x20;
        fg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void gng_bgvideoram_w(int offset, byte data) {
        gng_bgvideoram[offset] = data;
        int row, col;
        row = (offset & 0x3ff) % 0x20;
        col = (offset & 0x3ff) / 0x20;
        bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void gng_bgscrollx_w(int offset, byte data) {
        scrollx[offset] = data;
        bg_tilemap.tilemap_set_scrollx(0, scrollx[0] + 256 * scrollx[1]);
    }

    public static void gng_bgscrolly_w(int offset, byte data) {
        scrolly[offset] = data;
        bg_tilemap.tilemap_set_scrolly(0, scrolly[0] + 256 * scrolly[1]);
    }

    public static void gng_flipscreen_w(byte data) {
        Generic.flip_screen_set(~data & 1);
    }

    public static void draw_sprites_gng(RECT cliprect) {
        int offs;
        for (offs = 0x200 - 4; offs >= 0; offs -= 4) {
            byte attributes = Generic.buffered_spriteram[offs + 1];
            int sx = Generic.buffered_spriteram[offs + 3] - 0x100 * (attributes & 0x01);
            int sy = Generic.buffered_spriteram[offs + 2];
            int flipx = attributes & 0x04;
            int flipy = attributes & 0x08;
            if (Generic.flip_screen_get() != 0) {
                sx = 240 - sx;
                sy = 240 - sy;
                flipx = (flipx == 0 ? 1 : 0);
                flipy = (flipy == 0 ? 1 : 0);
            }
            Drawgfx.common_drawgfx_gng(gfx3rom, Generic.buffered_spriteram[offs] + ((attributes << 2) & 0x300), (attributes >> 4) & 3, flipx, flipy, sx, sy, cliprect);
        }
    }

    public static void video_update_gng() {
        bg_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x20, (byte) 0);
        draw_sprites_gng(Video.screenstate.visarea);
        bg_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
        fg_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
    }

    public static void video_eof_gng() {
        Generic.buffer_spriteram_w();
    }

//#endregion

//#region Input

    public static void loop_inputports_gng() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            bytes &= (byte) ~0x40;
        } else {
            bytes |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            bytes &= (byte) ~0x80;
        } else {
            bytes |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            bytes &= (byte) ~0x01;
        } else {
            bytes |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            bytes &= (byte) ~0x02;
        } else {
            bytes |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte1 &= (byte) ~0x01;
        } else {
            byte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte1 &= (byte) ~0x02;
        } else {
            byte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte1 &= (byte) ~0x04;
        } else {
            byte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte1 &= (byte) ~0x08;
        } else {
            byte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte1 &= (byte) ~0x10;
        } else {
            byte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte1 &= (byte) ~0x20;
        } else {
            byte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            byte2 &= (byte) ~0x01;
        } else {
            byte2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte2 &= (byte) ~0x02;
        } else {
            byte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte2 &= (byte) ~0x04;
        } else {
            byte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            byte2 &= (byte) ~0x20;
        } else {
            byte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            bytes &= (byte) ~0x20;
        } else {
            bytes |= 0x20;
        }
    }

    public static void loop_inputports_diamond() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            bytes &= (byte) ~0x40;
        } else {
            bytes |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            bytes &= (byte) ~0x80;
        } else {
            bytes |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            bytes &= (byte) ~0x01;
        } else {
            bytes |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            bytes &= (byte) ~0x02;
        } else {
            bytes |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte1 &= (byte) ~0x01;
        } else {
            byte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte1 &= (byte) ~0x02;
        } else {
            byte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte1 &= (byte) ~0x04;
        } else {
            byte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte1 &= (byte) ~0x08;
        } else {
            byte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte1 &= (byte) ~0x10;
        } else {
            byte1 |= 0x10;
        }
    }

    public static void loop_inputports_sfus() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short0 &= ~0x0001;
        } else {
            short0 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short0 &= ~0x0002;
        } else {
            short0 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            shorts &= ~0x0001;
        } else {
            shorts |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            shorts &= ~0x0002;
        } else {
            shorts |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            short1 &= ~0x0001;
        } else {
            short1 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short1 &= ~0x0002;
        } else {
            short1 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short1 &= ~0x0004;
        } else {
            short1 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short1 &= ~0x0008;
        } else {
            short1 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x0010;
        } else {
            short1 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short1 &= ~0x0020;
        } else {
            short1 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short0 &= ~0x0200;
        } else {
            short0 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short1 &= ~0x0040;
        } else {
            short1 |= 0x0040;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short1 &= ~0x0080;
        } else {
            short1 |= 0x0080;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            short0 &= ~0x0004;
        } else {
            short0 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            short1 &= ~0x0100;
        } else {
            short1 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            short1 &= ~0x0200;
        } else {
            short1 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            short1 &= ~0x0400;
        } else {
            short1 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            short1 &= ~0x0800;
        } else {
            short1 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short1 &= ~0x1000;
        } else {
            short1 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short1 &= ~0x2000;
        } else {
            short1 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            short0 &= ~0x0400;
        } else {
            short0 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            short1 &= (short) ~0x8000;
        } else {
            short1 |= (short) 0x8000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {
            short0 &= ~0x0100;
        } else {
            short0 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            shorts &= ~0x0004;
        } else {
            shorts |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
//            sbyte0 &= ~0x40;
        } else {
//            sbyte0 |= 0x40;
        }
    }

    public static void loop_inputports_sfjp() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            shortc &= ~0x0001;
        } else {
            shortc |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            shortc &= ~0x0002;
        } else {
            shortc |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            shorts &= ~0x0001;
        } else {
            shorts |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            shorts &= ~0x0002;
        } else {
            shorts |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            short1 &= ~0x0001;
        } else {
            short1 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short1 &= ~0x0002;
        } else {
            short1 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short1 &= ~0x0004;
        } else {
            short1 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short1 &= ~0x0008;
        } else {
            short1 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x0100;
        } else {
            short1 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short1 &= ~0x0200;
        } else {
            short1 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short1 &= ~0x0400;
        } else {
            short1 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short1 &= ~0x1000;
        } else {
            short1 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short1 &= ~0x2000;
        } else {
            short1 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            short2 &= ~0x0001;
        } else {
            short2 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            short2 &= ~0x0002;
        } else {
            short2 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            short2 &= ~0x0004;
        } else {
            short2 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            short2 &= ~0x0008;
        } else {
            short2 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short2 &= ~0x0200;
        } else {
            short2 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            short2 &= ~0x0400;
        } else {
            short2 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short2 &= ~0x1000;
        } else {
            short2 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            short2 &= ~0x2000;
        } else {
            short2 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {
            short2 &= ~0x4000;
        } else {
            short2 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            shorts &= ~0x0004;
        } else {
            shorts |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
//            sbyte0 &= ~0x40;
        } else {
//            sbyte0 |= 0x40;
        }
    }

    public static void loop_inputports_sfan() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            shortc &= ~0x0001;
        } else {
            shortc |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            shortc &= ~0x0002;
        } else {
            shortc |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            shorts &= ~0x0001;
        } else {
            shorts |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            shorts &= ~0x0002;
        } else {
            shorts |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            short0 &= ~0x0001;
        } else {
            short0 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short0 &= ~0x0002;
        } else {
            short0 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short0 &= ~0x0004;
        } else {
            short0 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short0 &= ~0x0008;
        } else {
            short0 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 |= 0x01;
        } else {
            sbyte1 &= ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 |= 0x02;
        } else {
            sbyte1 &= ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            sbyte1 |= 0x04;
        } else {
            sbyte1 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            sbyte2 |= 0x01;
        } else {
            sbyte2 &= ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            sbyte2 |= 0x02;
        } else {
            sbyte2 &= ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            sbyte2 |= 0x04;
        } else {
            sbyte2 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            short0 &= ~0x0100;
        } else {
            short0 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            short0 &= ~0x0200;
        } else {
            short0 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            short0 &= ~0x0400;
        } else {
            short0 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            short0 &= ~0x0800;
        } else {
            short0 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte3 |= 0x01;
        } else {
            sbyte3 &= ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte3 |= 0x02;
        } else {
            sbyte3 &= ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            sbyte3 |= 0x04;
        } else {
            sbyte3 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            sbyte4 |= 0x01;
        } else {
            sbyte4 &= ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            sbyte4 &= ~0x02;
        } else {
            sbyte4 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {
            sbyte4 |= 0x04;
        } else {
            sbyte4 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            shorts &= ~0x0004;
        } else {
            shorts |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            //sbyte0 &= ~0x40;
        } else {
            //sbyte0 |= 0x40;
        }
    }

    public static void record_port_gng() {
        if (bytes != bytes_old || byte1 != byte1_old || byte2 != byte2_old) {
            bytes_old = bytes;
            byte1_old = byte1;
            byte2_old = byte2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(bytes);
            Mame.bwRecord.write(byte1);
            Mame.bwRecord.write(byte2);
        }
    }

    public static void replay_port_gng() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                bytes_old = Mame.brRecord.readByte();
                byte1_old = Mame.brRecord.readByte();
                byte2_old = Mame.brRecord.readByte();
            } catch (Exception e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            bytes = bytes_old;
            byte1 = byte1_old;
            byte2 = byte2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void record_port_sf() {
        if (short0 != short0_old || short1 != short1_old || short2 != short2_old || shorts != shorts_old || shortc != shortc_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbyte3 != sbyte3_old || sbyte4 != sbyte4_old) {
            short0_old = short0;
            short1_old = short1;
            short2_old = short2;
            shorts_old = shorts;
            shortc_old = shortc;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbyte3_old = sbyte3;
            sbyte4_old = sbyte4;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(short0);
            Mame.bwRecord.write(short1);
            Mame.bwRecord.write(short2);
            Mame.bwRecord.write(shorts);
            Mame.bwRecord.write(shortc);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbyte3);
            Mame.bwRecord.write(sbyte4);
        }
    }

    public static void replay_port_sf() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                short0_old = Mame.brRecord.readInt16();
                short1_old = Mame.brRecord.readInt16();
                short2_old = Mame.brRecord.readInt16();
                shorts_old = Mame.brRecord.readInt16();
                shortc_old = Mame.brRecord.readInt16();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
                sbyte3_old = Mame.brRecord.readSByte();
                sbyte4_old = Mame.brRecord.readSByte();
            } catch (Exception e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            short0 = short0_old;
            short1 = short1_old;
            short2 = short2_old;
            shorts = shorts_old;
            shortc = shortc_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbyte3 = sbyte3_old;
            sbyte4 = sbyte4_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Memory

    public static byte sbyte1, sbyte2, sbyte3, sbyte4;
    public static byte sbyte1_old, sbyte2_old, sbyte3_old, sbyte4_old;
    public static short short0, short1, short2, shorts, shortc;
    public static short short0_old, short1_old, short2_old, shorts_old, shortc_old;
    public static byte bytes, byte1, byte2;
    public static byte bytes_old, byte1_old, byte2_old;

    public static byte MReadOpByte_gng(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x1dff) {
            result = Memory.mainram[address];
        } else if ((address & 0xffff) >= 0x4000 && (address & 0xffff) <= 0x5fff) {
            int offset = address - 0x4000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) >= 0x6000 && (address & 0xffff) <= 0xffff) {
            result = Memory.mainrom[address];
        } else {
            int i1 = 1;
        }
        return result;
    }

    public static byte MReadByte_gng(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x1dff) {
            result = Memory.mainram[(address & 0xffff)];
        } else if ((address & 0xffff) >= 0x1e00 && (address & 0xffff) <= 0x1fff) {
            int offset = (address & 0xffff) - 0x1e00;
            result = Generic.spriteram[offset];
        } else if ((address & 0xffff) >= 0x2000 && (address & 0xffff) <= 0x27ff) {
            int offset = (address & 0xffff) - 0x2000;
            result = gng_fgvideoram[offset];
        } else if ((address & 0xffff) >= 0x2800 && (address & 0xffff) <= 0x2fff) {
            int offset = (address & 0xffff) - 0x2800;
            result = gng_bgvideoram[offset];
        } else if ((address & 0xffff) == 0x3000) {
            result = bytes;
        } else if ((address & 0xffff) == 0x3001) {
            int offset = ((address & 0xffff) - 0x3001) / 2;
            result = byte1;
        } else if ((address & 0xffff) == 0x3002) {
            int offset = ((address & 0xffff) - 0x3002) / 2;
            result = byte2;
        } else if ((address & 0xffff) == 0x3003) {
            int offset = ((address & 0xffff) - 0x3003) / 2;
            result = bytedsw1;
        } else if ((address & 0xffff) == 0x3004) {
            int offset = ((address & 0xffff) - 0x3004) / 2;
            result = bytedsw2;
        } else if ((address & 0xffff) == 0x3c00) {
            result = 0;
        } else if ((address & 0xffff) >= 0x4000 && (address & 0xffff) <= 0x5fff) {
            int offset = address - 0x4000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) >= 0x6000 && (address & 0xffff) <= 0xffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte MReadByte_diamond(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x1dff) {
            result = Memory.mainram[(address & 0xffff)];
        } else if ((address & 0xffff) >= 0x1e00 && (address & 0xffff) <= 0x1fff) {
            int offset = (address & 0xffff) - 0x1e00;
            result = Generic.spriteram[offset];
        } else if ((address & 0xffff) >= 0x2000 && (address & 0xffff) <= 0x27ff) {
            int offset = (address & 0xffff) - 0x2000;
            result = gng_fgvideoram[offset];
        } else if ((address & 0xffff) >= 0x2800 && (address & 0xffff) <= 0x2fff) {
            int offset = (address & 0xffff) - 0x2800;
            result = gng_bgvideoram[offset];
        } else if ((address & 0xffff) == 0x3000) {
            result = bytes;
        } else if ((address & 0xffff) == 0x3001) {
            int offset = ((address & 0xffff) - 0x3001) / 2;
            result = byte1;
        } else if ((address & 0xffff) == 0x3002) {
            int offset = ((address & 0xffff) - 0x3002) / 2;
            result = byte2;
        } else if ((address & 0xffff) == 0x3003) {
            int offset = ((address & 0xffff) - 0x3003) / 2;
            result = bytedsw1;
        } else if ((address & 0xffff) == 0x3004) {
            int offset = ((address & 0xffff) - 0x3004) / 2;
            result = bytedsw2;
        } else if ((address & 0xffff) == 0x3c00) {
            result = 0;
        } else if ((address & 0xffff) >= 0x4000 && (address & 0xffff) <= 0x5fff) {
            int offset = (address & 0xffff) - 0x4000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) == 0x6000) {
            result = 0;
        } else if ((address & 0xffff) >= 0x6001 && (address & 0xffff) <= 0xffff) {
            result = Memory.mainrom[(address & 0xffff)];
        }
        return result;
    }

    public static void MWriteByte_gng(short address, byte value) {
        if (address <= 0x1dff) {
            Memory.mainram[address] = value;
        } else if (address >= 0x1e00 && address <= 0x1fff) {
            int offset = address - 0x1e00;
            Generic.spriteram[offset] = value;
        } else if (address >= 0x2000 && address <= 0x27ff) {
            int offset = address - 0x2000;
            gng_fgvideoram_w(offset, value);
        } else if (address >= 0x2800 && address <= 0x2fff) {
            int offset = address - 0x2800;
            gng_bgvideoram_w(offset, value);
        } else if (address >= 0x3800 && address <= 0x38ff) {
            int offset = address - 0x3800;
            Generic.paletteram_RRRRGGGGBBBBxxxx_split2_w(offset, value);
        } else if (address >= 0x3900 && address <= 0x39ff) {
            int offset = address - 0x3900;
            Generic.paletteram_RRRRGGGGBBBBxxxx_split1_w(offset, value);
        } else if (address == 0x3a00) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x3b08 && address <= 0x3b09) {
            int offset = address - 0x3b08;
            gng_bgscrollx_w(offset, value);
        } else if (address >= 0x3b0a && address <= 0x3b0b) {
            int offset = address - 0x3b0a;
            gng_bgscrolly_w(offset, value);
        } else if (address == 0x3c00) {
            int i1 = 1;
        } else if (address == 0x3d00) {
            gng_flipscreen_w(value);
        } else if (address >= 0x3d02 && address <= 0x3d03) {
            int offset = address - 0x3d02;
            gng_coin_counter_w(offset, value);
        } else if (address == 0x3e00) {
            gng_bankswitch_w(value);
        } else if (address >= 0x4000 && address <= 0xffff) {
            Memory.mainrom[address] = value;
        }
    }

    public static byte ZReadOp_gng(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[(address & 0xffff)];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xc7ff) {
            int offset = (address & 0xffff) - 0xc000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_gng(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xc7ff) {
            int offset = (address & 0xffff) - 0xc000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) == 0xc800) {
            result = (byte) Sound.soundlatch_r();
        } else {
            result = 0;
        }
        return result;
    }

    public static void ZWriteMemory_gng(short address, byte value) {
        if ((address & 0xffff) >= 0x0000 && (address & 0xffff) <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xc7ff) {
            int offset = (address & 0xffff) - 0xc000;
            if (value != 0) {
                int i1 = 1;
            }
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) == 0xe000) {
            YM2203.ym2203_control_port_0_w(value);
        } else if ((address & 0xffff) == 0xe001) {
            YM2203.ym2203_write_port_0_w(value);
        } else if ((address & 0xffff) == 0xe002) {
            YM2203.ym2203_control_port_1_w(value);
        } else if ((address & 0xffff) == 0xe003) {
            YM2203.ym2203_write_port_1_w(value);
        }
    }

    public static byte MReadOpByte_sfus(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x04_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_sf(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x04_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (sf_videoram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) sf_videoram[offset];
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_0001) {
            if (address % 2 == 0) {
                result = (byte) (shortc >> 8);
            } else if (address % 2 == 1) {
                result = (byte) shortc;
            }
        } else if (address >= 0xc0_0002 && address <= 0xc0_0003) {
            if (address % 2 == 0) {
                result = (byte) (short0 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) short0;
            }
        } else if (address >= 0xc0_0004 && address <= 0xc0_0005) {
            if (address % 2 == 0) {
                result = (byte) (button1_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) button1_r();
            }
        } else if (address >= 0xc0_0006 && address <= 0xc0_0007) {
            if (address % 2 == 0) {
                result = (byte) (button2_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) button2_r();
            }
        } else if (address >= 0xc0_0008 && address <= 0xc0_0009) {
            if (address % 2 == 0) {
                result = (byte) (dsw1 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dsw1;
            }
        } else if (address >= 0xc0_000a && address <= 0xc0_000b) {
            if (address % 2 == 0) {
                result = (byte) (dsw2 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dsw2;
            }
        } else if (address >= 0xc0_000c && address <= 0xc0_000d) {
            if (address % 2 == 0) {
                result = (byte) (shorts >> 8);
            } else if (address % 2 == 1) {
                result = (byte) shorts;
            }
        } else if (address >= 0xc0_000e && address <= 0xc0_000f) {
            if (address % 2 == 0) {
                result = (byte) (dummy_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dummy_r();
            }
        } else if (address >= 0xff_8000 && address <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = Memory.mainram[offset];
        } else if (address >= 0xff_e000 && address <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            if (address % 2 == 0) {
                result = (byte) (sf_objectram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) sf_objectram[offset];
            }
        }
        return result;
    }

    public static byte MReadByte_sfus(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x04_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (sf_videoram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) sf_videoram[offset];
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_0001) {
            if (address % 2 == 0) {
                result = (byte) (short0 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) short0;
            }
        } else if (address >= 0xc0_0002 && address <= 0xc0_0003) {
            if (address % 2 == 0) {
                result = (byte) (short1 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) short1;
            }
        } else if (address >= 0xc0_0004 && address <= 0xc0_0005) {
            if (address % 2 == 0) {
                result = (byte) (dummy_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dummy_r();
            }
        } else if (address >= 0xc0_0006 && address <= 0xc0_0007) {
            if (address % 2 == 0) {
                result = (byte) (dummy_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dummy_r();
            }
        } else if (address >= 0xc0_0008 && address <= 0xc0_0009) {
            if (address % 2 == 0) {
                result = (byte) (dsw1 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dsw1;
            }
        } else if (address >= 0xc0_000a && address <= 0xc0_000b) {
            if (address % 2 == 0) {
                result = (byte) (dsw2 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dsw2;
            }
        } else if (address >= 0xc0_000c && address <= 0xc0_000d) {
            if (address % 2 == 0) {
                result = (byte) (shorts >> 8);
            } else if (address % 2 == 1) {
                result = (byte) shorts;
            }
        } else if (address >= 0xc0_000e && address <= 0xc0_000f) {
            if (address % 2 == 0) {
                result = (byte) (dummy_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dummy_r();
            }
        } else if (address >= 0xff_8000 && address <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = Memory.mainram[offset];
        } else if (address >= 0xff_e000 && address <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            if (address % 2 == 0) {
                result = (byte) (sf_objectram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) sf_objectram[offset];
            }
        }
        return result;
    }

    public static byte MReadByte_sfjp(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x04_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (sf_videoram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) sf_videoram[offset];
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_0001) {
            if (address % 2 == 0) {
                result = (byte) (shortc >> 8);
            } else if (address % 2 == 1) {
                result = (byte) shortc;
            }
        } else if (address >= 0xc0_0002 && address <= 0xc0_0003) {
            if (address % 2 == 0) {
                result = (byte) (short1 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) short1;
            }
        } else if (address >= 0xc0_0004 && address <= 0xc0_0005) {
            if (address % 2 == 0) {
                result = (byte) (short2 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) short2;
            }
        } else if (address >= 0xc0_0006 && address <= 0xc0_0007) {
            if (address % 2 == 0) {
                result = (byte) (dummy_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dummy_r();
            }
        } else if (address >= 0xc0_0008 && address <= 0xc0_0009) {
            if (address % 2 == 0) {
                result = (byte) (dsw1 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dsw1;
            }
        } else if (address >= 0xc0_000a && address <= 0xc0_000b) {
            if (address % 2 == 0) {
                result = (byte) (dsw2 >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dsw2;
            }
        } else if (address >= 0xc0_000c && address <= 0xc0_000d) {
            if (address % 2 == 0) {
                result = (byte) (shorts >> 8);
            } else if (address % 2 == 1) {
                result = (byte) shorts;
            }
        } else if (address >= 0xc0_000e && address <= 0xc0_000f) {
            if (address % 2 == 0) {
                result = (byte) (dummy_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) dummy_r();
            }
        } else if (address >= 0xff_8000 && address <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = Memory.mainram[offset];
        } else if (address >= 0xff_e000 && address <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            if (address % 2 == 0) {
                result = (byte) (sf_objectram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) sf_objectram[offset];
            }
        }
        return result;
    }

    public static short MReadOpWord_sfus(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x04_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_sf(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x04_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            result = sf_videoram[offset];
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_0001) {
            result = shortc;
        } else if (address >= 0xc0_0002 && address + 1 <= 0xc0_0003) {
            result = short0;
        } else if (address >= 0xc0_0004 && address + 1 <= 0xc0_0005) {
            result = button1_r();
        } else if (address >= 0xc0_0006 && address + 1 <= 0xc0_0007) {
            result = button2_r();
        } else if (address >= 0xc0_0008 && address + 1 <= 0xc0_0009) {
            result = dsw1;
        } else if (address >= 0xc0_000a && address + 1 <= 0xc0_000b) {
            result = dsw2;
        } else if (address >= 0xc0_000c && address + 1 <= 0xc0_000d) {
            result = shorts;
        } else if (address >= 0xc0_000e && address + 1 <= 0xc0_000f) {
            result = dummy_r();
        } else if (address >= 0xff_8000 && address + 1 <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0xff_e000 && address + 1 <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            result = sf_objectram[offset];
        }
        return result;
    }

    public static short MReadWord_sfus(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x04_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            result = sf_videoram[offset];
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_0001) {
            result = short0;
        } else if (address >= 0xc0_0002 && address + 1 <= 0xc0_0003) {
            result = short1;
        } else if (address >= 0xc0_0004 && address + 1 <= 0xc0_0005) {
            result = dummy_r();
        } else if (address >= 0xc0_0006 && address + 1 <= 0xc0_0007) {
            result = dummy_r();
        } else if (address >= 0xc0_0008 && address + 1 <= 0xc0_0009) {
            result = dsw1;
        } else if (address >= 0xc0_000a && address + 1 <= 0xc0_000b) {
            result = dsw2;
        } else if (address >= 0xc0_000c && address + 1 <= 0xc0_000d) {
            result = shorts;
        } else if (address >= 0xc0_000e && address + 1 <= 0xc0_000f) {
            result = dummy_r();
        } else if (address >= 0xff_8000 && address + 1 <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0xff_e000 && address + 1 <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            result = sf_objectram[offset];
        }
        return result;
    }

    public static short MReadWord_sfjp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x04_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            result = sf_videoram[offset];
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_0001) {
            result = shortc;
        } else if (address >= 0xc0_0002 && address + 1 <= 0xc0_0003) {
            result = short1;
        } else if (address >= 0xc0_0004 && address + 1 <= 0xc0_0005) {
            result = short2;
        } else if (address >= 0xc0_0006 && address + 1 <= 0xc0_0007) {
            result = dummy_r();
        } else if (address >= 0xc0_0008 && address + 1 <= 0xc0_0009) {
            result = dsw1;
        } else if (address >= 0xc0_000a && address + 1 <= 0xc0_000b) {
            result = dsw2;
        } else if (address >= 0xc0_000c && address + 1 <= 0xc0_000d) {
            result = shorts;
        } else if (address >= 0xc0_000e && address + 1 <= 0xc0_000f) {
            result = dummy_r();
        } else if (address >= 0xff_8000 && address + 1 <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0xff_e000 && address + 1 <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            result = sf_objectram[offset];
        }
        return result;
    }

    public static int MReadOpLong_sfus(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x04_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_sfus(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x04_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            result = sf_videoram[offset] * 0x1_0000 + sf_videoram[offset + 1];
        } else if (address >= 0xff_8000 && address + 3 <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0xff_e000 && address + 3 <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            result = sf_objectram[offset] * 0x1_0000 + sf_objectram[offset + 1];
        }
        return result;
    }

    public static void MWriteByte_sf(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address <= 0x04_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = value;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                sf_videoram_w1(offset, value);
            } else if (address % 2 == 1) {
                sf_videoram_w2(offset, value);
            }
        } else if (address >= 0xb0_0000 && address <= 0xb0_07ff) {
            int offset = (address - 0xb0_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w2(offset, value);
            }
        } else if (address >= 0xc0_0010 && address <= 0xc0_0011) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                sf_coin_w2();
            }
        } else if (address >= 0xc0_0014 && address <= 0xc0_0015) {
            if (address % 2 == 0) {
                sf_fg_scroll_w1(value);
            } else if (address % 2 == 1) {
                sf_fg_scroll_w2(value);
            }
        } else if (address >= 0xc0_0018 && address <= 0xc0_0019) {
            if (address % 2 == 0) {
                sf_bg_scroll_w1(value);
            } else if (address % 2 == 1) {
                sf_bg_scroll_w2(value);
            }
        } else if (address >= 0xc0_001a && address <= 0xc0_001b) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                sf_gfxctrl_w2(value);
            }
        } else if (address >= 0xc0_001c && address <= 0xc0_001d) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                soundcmd_w2(value);
            }
        } else if (address >= 0xc0_001e && address <= 0xc0_001f) {
            if (address % 2 == 0) {
                protection_w1(value);
            } else if (address % 2 == 1) {
                protection_w2(value);
            }
        } else if (address >= 0xff_8000 && address <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            Memory.mainram[offset] = value;
        } else if (address >= 0xff_e000 && address <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            if (address % 2 == 0) {
                sf_objectram[offset] = (short) ((value << 8) | (sf_objectram[offset] & 0xff));
            } else if (address % 2 == 1) {
                sf_objectram[offset] = (short) ((sf_objectram[offset] & 0xff00) | value);
            }
        }
    }

    public static void MWriteWord_sf(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 1 <= 0x04_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 8);
                Memory.mainrom[address + 1] = (byte) value;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            sf_videoram_w(offset, value);
        } else if (address >= 0xb0_0000 && address + 1 <= 0xb0_07ff) {
            int offset = (address - 0xb0_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, value);
        } else if (address >= 0xc0_0010 && address + 1 <= 0xc0_0011) {
            sf_coin_w();
        } else if (address >= 0xc0_0014 && address + 1 <= 0xc0_0015) {
            sf_fg_scroll_w(value);
        } else if (address >= 0xc0_0018 && address + 1 <= 0xc0_0019) {
            sf_bg_scroll_w(value);
        } else if (address >= 0xc0_001a && address + 1 <= 0xc0_001b) {
            sf_gfxctrl_w(value);
        } else if (address >= 0xc0_001c && address + 1 <= 0xc0_001d) {
            soundcmd_w(value);
        } else if (address >= 0xc0_001e && address + 1 <= 0xc0_001f) {
            protection_w(value);
        } else if (address >= 0xff_8000 && address + 1 <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0xff_e000 && address + 1 <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            sf_objectram[offset] = value;
        }
    }

    public static void MWriteLong_sf(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 3 <= 0x04_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 24);
                Memory.mainrom[address + 1] = (byte) (value >> 16);
                Memory.mainrom[address + 2] = (byte) (value >> 8);
                Memory.mainrom[address + 3] = (byte) value;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_0fff) {
            int offset = (address - 0x80_0000) / 2;
            sf_videoram_w(offset, (short) (value >> 16));
            sf_videoram_w(offset + 1, (short) value);
        } else if (address >= 0xb0_0000 && address + 3 <= 0xb0_07ff) {
            int offset = (address - 0xb0_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset + 1, (short) value);
        } else if (address >= 0xff_8000 && address + 3 <= 0xff_dfff) {
            int offset = address - 0xff_8000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0xff_e000 && address + 3 <= 0xff_ffff) {
            int offset = (address - 0xff_e000) / 2;
            sf_objectram[offset] = (short) (value >> 16);
            sf_objectram[offset + 1] = (short) value;
        }
    }

    public static byte Z0ReadOp_sf(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            int offset = address - 0xc000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte Z0ReadMemory_sf(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            int offset = address - 0xc000;
            result = Memory.audioram[offset];
        } else if (address == 0xc800) {
            result = (byte) Sound.soundlatch_r();
        } else if (address == 0xe001) {
            result = YM2151.ym2151_status_port_0_r();
        } else {
            result = 0;
        }
        return result;
    }

    public static void Z0WriteMemory_sf(short address, byte value) {
        if (address >= 0x0000 && address <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            int offset = address - 0xc000;
            Memory.audioram[offset] = value;
        } else if (address == 0xe000) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xe001) {
            YM2151.ym2151_data_port_0_w(value);
        }
    }

    public static byte Z0ReadHardware(short address) {
        return 0;
    }

    public static void Z0WriteHardware(short address, byte value) {

    }

    public static int Z0IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[0].cpunum, 0);
    }

    public static byte Z1ReadOp_sf(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = audiorom2[address];
        } else if (address >= 0x8000 && address <= 0xffff) {
            int offset = address - 0x8000;
            result = audiorom2[basebanksnd1 + offset];
        }
        return result;
    }

    public static byte Z1ReadMemory_sf(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = audiorom2[address];
        } else if (address >= 0x8000 && address <= 0xffff) {
            int offset = address - 0x8000;
            result = audiorom2[basebanksnd1 + offset];
        }
        return result;
    }

    public static void Z1WriteMemory_sf(short address, byte value) {
        if (address >= 0x0000 && address <= 0xffff) {

        }
    }

    public static byte Z1ReadHardware(short address) {
        byte result = 0;
        address &= 0xff;
        if (address == 0x01) {
            result = (byte) Sound.soundlatch_r();
        }
        return result;
    }

    public static void Z1WriteHardware(short address, byte value) {
        address &= 0xff;
        if (address >= 0x00 && address <= 0x01) {
            msm5205_w(address, value);
        } else if (address == 0x02) {
            basebanksnd1 = 0x8000 * (value + 1);
        }
    }

    public static int Z1IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[1].cpunum, 0);
    }

//#endregion

//#region State

    public static void SaveStateBinary_gng(BinaryWriter writer) {
        int i;
        writer.write(bytedsw1);
        writer.write(bytedsw2);
        writer.write(basebankmain);
        writer.write(gng_fgvideoram, 0, 0x800);
        writer.write(gng_bgvideoram, 0, 0x800);
        writer.write(scrollx, 0, 2);
        writer.write(scrolly, 0, 2);
        writer.write(Generic.paletteram, 0, 0x100);
        writer.write(Generic.paletteram_2, 0, 0x100);
        writer.write(Generic.spriteram, 0, 0x200);
        writer.write(Generic.buffered_spriteram, 0, 0x200);
        for (i = 0; i < 0x100; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1e00);
        M6809.mm1[0].SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        AY8910.AA8910[0].SaveStateBinary(writer);
        AY8910.AA8910[1].SaveStateBinary(writer);
        YM2203.FF2203[0].SaveStateBinary(writer);
        YM2203.FF2203[1].SaveStateBinary(writer);
        writer.write(Sound.latched_value[0]);
        writer.write(Sound.utempdata[0]);
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(AY8910.AA8910[1].stream.output_sampindex);
        writer.write(AY8910.AA8910[1].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[1].stream.output_sampindex);
        writer.write(YM2203.FF2203[1].stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_gng(BinaryReader reader) {
        try {
            int i;
            bytedsw1 = reader.readByte();
            bytedsw2 = reader.readByte();
            basebankmain = reader.readInt32();
            gng_fgvideoram = reader.readBytes(0x800);
            gng_bgvideoram = reader.readBytes(0x800);
            scrollx = reader.readBytes(2);
            scrolly = reader.readBytes(2);
            Generic.paletteram = reader.readBytes(0x100);
            Generic.paletteram_2 = reader.readBytes(0x100);
            Generic.spriteram = reader.readBytes(0x200);
            Generic.buffered_spriteram = reader.readBytes(0x200);
            for (i = 0; i < 0x100; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1e00);
            M6809.mm1[0].LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            AY8910.AA8910[0].LoadStateBinary(reader);
            AY8910.AA8910[1].LoadStateBinary(reader);
            YM2203.FF2203[0].LoadStateBinary(reader);
            YM2203.FF2203[1].LoadStateBinary(reader);
            Sound.latched_value[0] = reader.readUInt16();
            Sound.utempdata[0] = reader.readUInt16();
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            AY8910.AA8910[1].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[1].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[1].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[1].stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_sf(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(basebanksnd1);
        for (i = 0; i < 0x1000; i++) {
            writer.write(sf_objectram[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(sf_videoram[i]);
        }
        for (i = 0; i < 0x400; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        writer.write(bg_scrollx);
        writer.write(fg_scrollx);
        writer.write(sf_active);
        for (i = 0; i < 0x400; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x6000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Z80A.zz1[1].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        MSM5205.mm1[0].SaveStateBinary(writer);
        MSM5205.mm1[1].SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(MSM5205.mm1[0].voice.stream.output_sampindex);
        writer.write(MSM5205.mm1[0].voice.stream.output_base_sampindex);
        writer.write(MSM5205.mm1[1].voice.stream.output_sampindex);
        writer.write(MSM5205.mm1[1].voice.stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_sf(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readUInt16();
            dsw2 = reader.readUInt16();
            basebanksnd1 = reader.readInt32();
            for (i = 0; i < 0x1000; i++) {
                sf_objectram[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                sf_videoram[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x400; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            bg_scrollx = reader.readInt32();
            fg_scrollx = reader.readInt32();
            sf_active = reader.readInt32();
            for (i = 0; i < 0x400; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x6000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Z80A.zz1[1].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            MSM5205.mm1[0].LoadStateBinary(reader);
            MSM5205.mm1[1].LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            MSM5205.mm1[0].voice.stream.output_sampindex = reader.readInt32();
            MSM5205.mm1[0].voice.stream.output_base_sampindex = reader.readInt32();
            MSM5205.mm1[1].voice.stream.output_sampindex = reader.readInt32();
            MSM5205.mm1[1].voice.stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Tilemap

    public static void tilemap_init() {
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
                tilemap_init_gng();
                break;
            case "sf":
            case "sfua":
            case "sfj":
            case "sfjan":
            case "sfan":
            case "sfp":
                tilemap_init_sf();
                break;
        }
    }

    public static void tilemap_init_gng() {
        int i;
        fg_tilemap = new Tmap();
        fg_tilemap.cols = 0x20;
        fg_tilemap.rows = 0x20;
        fg_tilemap.tilewidth = 8;
        fg_tilemap.tileheight = 8;
        fg_tilemap.width = 0x100;
        fg_tilemap.height = 0x100;
        fg_tilemap.enable = true;
        fg_tilemap.all_tiles_dirty = true;
        fg_tilemap.total_elements = gfx1rom.length / 0x40;
        fg_tilemap.pixmap = new short[0x100 * 0x100];
        fg_tilemap.flagsmap = new byte[0x100][0x100];
        fg_tilemap.tileflags = new byte[0x20][0x20];
        fg_tilemap.pen_data = new byte[0x40];
        fg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 16; i++) {
            fg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        fg_tilemap.pen_to_flags[0][3] = 0;
        fg_tilemap.scrollrows = 1;
        fg_tilemap.scrollcols = 1;
        fg_tilemap.rowscroll = new int[fg_tilemap.scrollrows];
        fg_tilemap.colscroll = new int[fg_tilemap.scrollcols];
        fg_tilemap.tilemap_draw_instance3 = fg_tilemap::tilemap_draw_instanceCapcom_gng;
        fg_tilemap.tile_update3 = fg_tilemap::tile_updateCapcomfg_gng;
        bg_tilemap = new Tmap();
        bg_tilemap = new Tmap();
        bg_tilemap.cols = 0x20;
        bg_tilemap.rows = 0x20;
        bg_tilemap.tilewidth = 0x10;
        bg_tilemap.tileheight = 0x10;
        bg_tilemap.width = 0x200;
        bg_tilemap.height = 0x200;
        bg_tilemap.enable = true;
        bg_tilemap.all_tiles_dirty = true;
        bg_tilemap.total_elements = gfx2rom.length / 0x100;
        bg_tilemap.pixmap = new short[0x200 * 0x200];
        bg_tilemap.flagsmap = new byte[0x200][0x200];
        bg_tilemap.tileflags = new byte[0x20][0x20];
        bg_tilemap.pen_data = new byte[0x100];
        bg_tilemap.pen_to_flags = new byte[2][16];
        for (i = 0; i < 8; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x20;
        }
        for (i = 8; i < 0x10; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x30;
        }
        bg_tilemap.pen_to_flags[1][0] = 0x20;
        bg_tilemap.pen_to_flags[1][1] = 0x10;
        bg_tilemap.pen_to_flags[1][2] = 0x10;
        bg_tilemap.pen_to_flags[1][3] = 0x10;
        bg_tilemap.pen_to_flags[1][4] = 0x10;
        bg_tilemap.pen_to_flags[1][5] = 0x10;
        bg_tilemap.pen_to_flags[1][6] = 0x20;
        bg_tilemap.pen_to_flags[1][7] = 0x10;
        for (i = 8; i < 0x10; i++) {
            bg_tilemap.pen_to_flags[1][i] = 0x30;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceCapcom_gng;
        bg_tilemap.tile_update3 = bg_tilemap::tile_updateCapcombg_gng;
    }

    public static void tilemap_init_sf() {
        int i;
        bg_tilemap = new Tmap();
        bg_tilemap.cols = 0x800;
        bg_tilemap.rows = 0x10;
        bg_tilemap.tilewidth = 0x10;
        bg_tilemap.tileheight = 0x10;
        bg_tilemap.width = 0x8000;
        bg_tilemap.height = 0x100;
        bg_tilemap.enable = true;
        bg_tilemap.all_tiles_dirty = true;
        bg_tilemap.total_elements = gfx1rom.length / 0x100;
        bg_tilemap.pixmap = new short[0x100 * 0x8000];
        bg_tilemap.flagsmap = new byte[0x100][0x8000];
        bg_tilemap.tileflags = new byte[0x10][0x800];
        bg_tilemap.pen_data = new byte[0x100];
        bg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceCapcom_sf;
        bg_tilemap.tile_update3 = bg_tilemap::tile_updateCapcombg;

        fg_tilemap = new Tmap();
        fg_tilemap.cols = 0x800;
        fg_tilemap.rows = 0x10;
        fg_tilemap.tilewidth = 0x10;
        fg_tilemap.tileheight = 0x10;
        fg_tilemap.width = 0x8000;
        fg_tilemap.height = 0x100;
        fg_tilemap.enable = true;
        fg_tilemap.all_tiles_dirty = true;
        fg_tilemap.total_elements = gfx2rom.length / 0x100;
        fg_tilemap.pixmap = new short[0x100 * 0x8000];
        fg_tilemap.flagsmap = new byte[0x100][0x8000];
        fg_tilemap.tileflags = new byte[0x10][0x800];
        fg_tilemap.pen_data = new byte[0x100];
        fg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 15; i++) {
            fg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        fg_tilemap.pen_to_flags[0][3] = 0;
        fg_tilemap.scrollrows = 1;
        fg_tilemap.scrollcols = 1;
        fg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        fg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        fg_tilemap.tilemap_draw_instance3 = fg_tilemap::tilemap_draw_instanceCapcom_sf;
        fg_tilemap.tile_update3 = fg_tilemap::tile_updateCapcomfg;

        tx_tilemap = new Tmap();
        tx_tilemap.cols = 0x40;
        tx_tilemap.rows = 0x20;
        tx_tilemap.tilewidth = 8;
        tx_tilemap.tileheight = 8;
        tx_tilemap.width = 0x200;
        tx_tilemap.height = 0x100;
        tx_tilemap.enable = true;
        tx_tilemap.all_tiles_dirty = true;
        tx_tilemap.total_elements = gfx4rom.length / 0x40;
        tx_tilemap.pixmap = new short[0x100 * 0x200];
        tx_tilemap.flagsmap = new byte[0x100][0x200];
        tx_tilemap.tileflags = new byte[0x20][0x40];
        tx_tilemap.pen_data = new byte[0x40];
        tx_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 16; i++) {
            tx_tilemap.pen_to_flags[0][i] = 0x10;
        }
        tx_tilemap.pen_to_flags[0][3] = 0;
        tx_tilemap.scrollrows = 1;
        tx_tilemap.scrollcols = 1;
        tx_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        tx_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        tx_tilemap.tilemap_draw_instance3 = tx_tilemap::tilemap_draw_instanceCapcom_sf;
        tx_tilemap.tile_update3 = tx_tilemap::tile_updateCapcomtx;

        Tilemap.lsTmap = new ArrayList<>();
        Tilemap.lsTmap.add(bg_tilemap);
        Tilemap.lsTmap.add(fg_tilemap);
        Tilemap.lsTmap.add(tx_tilemap);
    }

    public static class Tmap extends Tilemap.Tmap {

        public void tilemap_draw_instanceCapcom_gng(RECT cliprect, int xpos, int ypos) {
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
                    if (cur_trans == prev_trans) {
                        continue;
                    }
                    x_end = column * tilewidth;
                    x_end = Math.max(x_end, x1);
                    x_end = Math.min(x_end, x2);
                    if (prev_trans != trans_t.WHOLLY_TRANSPARENT) {
                        int cury;
                        offsety2 = offsety1;
                        if (prev_trans == trans_t.WHOLLY_OPAQUE) {
                            for (cury = y; cury < nexty; cury++) {
                                System.arraycopy(pixmap, offsety2 * width + x_start, Video.bitmapbase[Video.curbitmap], (offsety2 + ypos) * 0x100 + xpos + x_start, x_end - x_start);
                                offsety2++;
                            }
                        } else if (prev_trans == trans_t.MASKED) {
                            for (cury = y; cury < nexty; cury++) {
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                        Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x100 + i] = pixmap[offsety2 * width + i - xpos];
                                    }
                                }
                                offsety2++;
                            }
                        }
                    }
                    x_start = x_end;
                    prev_trans = cur_trans;
                }
                if (nexty == y2) {
                    break;
                }
                offsety1 += (nexty - y);
                y = nexty;
                nexty += tileheight;
                nexty = Math.min(nexty, y2);
            }
        }

        public void tilemap_draw_instanceCapcom_sf(RECT cliprect, int xpos, int ypos) {
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
                    if (cur_trans == prev_trans) {
                        continue;
                    }
                    x_end = column * tilewidth;
                    x_end = Math.max(x_end, x1);
                    x_end = Math.min(x_end, x2);
                    if (prev_trans != trans_t.WHOLLY_TRANSPARENT) {
                        int cury;
                        offsety2 = offsety1;
                        if (prev_trans == trans_t.WHOLLY_OPAQUE) {
                            for (cury = y; cury < nexty; cury++) {
                                System.arraycopy(pixmap, offsety2 * width + x_start, Video.bitmapbase[Video.curbitmap], (offsety2 + ypos) * 0x200 + xpos + x_start, x_end - x_start);
                                offsety2++;
                            }
                        } else if (prev_trans == trans_t.MASKED) {
                            for (cury = y; cury < nexty; cury++) {
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                        Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x200 + i] = pixmap[offsety2 * width + i - xpos];
                                    }
                                }
                                offsety2++;
                            }
                        }
                    }
                    x_start = x_end;
                    prev_trans = cur_trans;
                }
                if (nexty == y2) {
                    break;
                }
                offsety1 += (nexty - y);
                y = nexty;
                nexty += tileheight;
                nexty = Math.min(nexty, y2);
            }
        }

        public void tile_updateCapcombg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, color;
            int pen_data_offset, palette_base, group;
            tile_index = col * rows + row;
            int base_offset = 2 * tile_index;
            int attr = Capcom.gfx5rom[base_offset + 0x1_0000];
            color = Capcom.gfx5rom[base_offset];
            code = (Capcom.gfx5rom[base_offset + 0x1_0000 + 1] << 8) | Capcom.gfx5rom[base_offset + 1];
            code = code % total_elements;
            pen_data_offset = code * 0x100;
            palette_base = color * 0x10;
            group = 0;
            flags = (attr & 0x03) ^ (attributes & 0x03);
            tileflags[row][col] = tile_drawCapcom(Capcom.gfx1rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updateCapcomfg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, color;
            int pen_data_offset, palette_base, group;
            tile_index = col * rows + row;
            int base_offset = 0x2_0000 + 2 * tile_index;
            int attr = Capcom.gfx5rom[base_offset + 0x1_0000];
            color = Capcom.gfx5rom[base_offset];
            code = (Capcom.gfx5rom[base_offset + 0x1_0000 + 1] << 8) | Capcom.gfx5rom[base_offset + 1];
            code = code % total_elements;
            pen_data_offset = code * 0x100;
            palette_base = 0x100 + (color * 0x10);
            group = 0;
            flags = (attr & 0x03) ^ (attributes & 0x03);
            tileflags[row][col] = tile_drawCapcom(Capcom.gfx2rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updateCapcomtx(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;
            code = Capcom.sf_videoram[tile_index];
            color = code >> 12;
            flags = (((code & 0xc00) >> 10) & 0x03) ^ (attributes & 0x03);
            code = (code & 0x3ff) % total_elements;
            pen_data_offset = code * 0x40;
            palette_base = 0x300 + (color * 0x4);
            group = 0;
            tileflags[row][col] = tile_drawCapcomtx(Capcom.gfx4rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updateCapcombg_gng(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, color;
            int pen_data_offset, palette_base, group;
            tile_index = col * rows + row;
            int base_offset = 2 * tile_index;
            int attr = Capcom.gng_bgvideoram[tile_index + 0x400];
            color = attr & 0x07;
            code = Capcom.gng_bgvideoram[tile_index] + ((attr & 0xc0) << 2);
            code = code % total_elements;
            pen_data_offset = code * 0x100;
            palette_base = color * 8;
            flags = (((attr & 0x30) >> 4) & 0x03) ^ (attributes & 0x03);
            group = (attr & 0x08) >> 3;
            tileflags[row][col] = tile_drawCapcom(Capcom.gfx2rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updateCapcomfg_gng(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;
            int base_offset = 2 * tile_index;
            int attr = Capcom.gng_fgvideoram[tile_index + 0x400];
            color = attr & 0x0f;
            code = Capcom.gng_fgvideoram[tile_index] + ((attr & 0xc0) << 2);
            code = code % total_elements;
            pen_data_offset = code * 0x40;
            palette_base = 0x80 + color * 4;
            flags = (((attr & 0x30) >> 4) & 0x03) ^ (attributes & 0x03);
            group = 0;
            tileflags[row][col] = tile_drawCapcomfg_gng(Capcom.gfx1rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public byte tile_drawCapcomfg_gng(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
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

        public byte tile_drawCapcom(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
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

        public byte tile_drawCapcomtx(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
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

//#endregion

//#region Video

    public static Tmap bg_tilemap, fg_tilemap, tx_tilemap;
    public static int bg_scrollx, fg_scrollx;
    public static int sf_active;
    public static short[] uuB0000;

    public static void video_start_sf() {
        int i;
        sf_active = 0;
        uuB0000 = new short[0x200 * 0x100];
        for (i = 0; i < 0x2_0000; i++) {
            uuB0000[i] = 0x0;
        }
    }

    public static void sf_videoram_w(int offset, short data) {
        int row, col;
        sf_videoram[offset] = data;
        row = offset / 64;
        col = offset % 64;
        tx_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void sf_videoram_w1(int offset, byte data) {
        int row, col;
        sf_videoram[offset] = (short) ((data << 8) | (sf_videoram[offset] & 0xff));
        row = offset / 64;
        col = offset % 64;
        tx_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void sf_videoram_w2(int offset, byte data) {
        int row, col;
        sf_videoram[offset] = (short) ((sf_videoram[offset] & 0xff00) | data);
        row = offset / 64;
        col = offset % 64;
        tx_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void sf_bg_scroll_w(short data) {
        bg_scrollx = data;
        bg_tilemap.tilemap_set_scrollx(0, bg_scrollx);
    }

    public static void sf_bg_scroll_w1(byte data) {
        bg_scrollx = (data << 8) | (bg_scrollx & 0xff);
        bg_tilemap.tilemap_set_scrollx(0, bg_scrollx);
    }

    public static void sf_bg_scroll_w2(byte data) {
        bg_scrollx = (bg_scrollx & 0xff00) | data;
        bg_tilemap.tilemap_set_scrollx(0, bg_scrollx);
    }

    public static void sf_fg_scroll_w(short data) {
        fg_scrollx = data;
        fg_tilemap.tilemap_set_scrollx(0, fg_scrollx);
    }

    public static void sf_fg_scroll_w1(byte data) {
        fg_scrollx = (data << 8) | (fg_scrollx & 0xff);
        fg_tilemap.tilemap_set_scrollx(0, fg_scrollx);
    }

    public static void sf_fg_scroll_w2(byte data) {
        fg_scrollx = (fg_scrollx & 0xff00) | data;
        fg_tilemap.tilemap_set_scrollx(0, fg_scrollx);
    }

    public static void sf_gfxctrl_w(short data) {
        sf_active = data & 0xff;
        Generic.flip_screen_set(data & 0x04);
        tx_tilemap.enable = (data & 0x08) != 0;
        bg_tilemap.enable = (data & 0x20) != 0;
        fg_tilemap.enable = (data & 0x40) != 0;
    }

    public static void sf_gfxctrl_w2(byte data) {
        sf_active = data & 0xff;
        Generic.flip_screen_set(data & 0x04);
        tx_tilemap.enable = (data & 0x08) != 0;
        bg_tilemap.enable = (data & 0x20) != 0;
        fg_tilemap.enable = (data & 0x40) != 0;
    }

    public static int sf_invert(int nb) {
        int[] delta = new int[] {
            0x00, 0x18, 0x18, 0x00
        };
        return nb ^ delta[(nb >> 3) & 3];
    }

    public static void draw_sprites(RECT cliprect) {
        int offs;
        for (offs = 0x1000 - 0x20; offs >= 0; offs -= 0x20) {
            int c = sf_objectram[offs];
            int attr = sf_objectram[offs + 1];
            int sy = sf_objectram[offs + 2];
            int sx = sf_objectram[offs + 3];
            int color = attr & 0x000f;
            int flipx = attr & 0x0100;
            int flipy = attr & 0x0200;
            if ((attr & 0x400) != 0) {
                int c1, c2, c3, c4, t;
                if (Generic.flip_screen_get() != 0) {
                    sx = 480 - sx;
                    sy = 224 - sy;
                    flipx = (flipx == 0) ? 1 : 0;
                    flipy = (flipy == 0) ? 1 : 0;
                }
                c1 = c;
                c2 = c + 1;
                c3 = c + 16;
                c4 = c + 17;
                if (flipx != 0) {
                    t = c1;
                    c1 = c2;
                    c2 = t;
                    t = c3;
                    c3 = c4;
                    c4 = t;
                }
                if (flipy != 0) {
                    t = c1;
                    c1 = c3;
                    c3 = t;
                    t = c2;
                    c2 = c4;
                    c4 = t;
                }
                Drawgfx.common_drawgfx_sf(gfx3rom, sf_invert(c1), color, flipx, flipy, sx, sy, cliprect);
                Drawgfx.common_drawgfx_sf(gfx3rom, sf_invert(c2), color, flipx, flipy, sx + 16, sy, cliprect);
                Drawgfx.common_drawgfx_sf(gfx3rom, sf_invert(c3), color, flipx, flipy, sx, sy + 16, cliprect);
                Drawgfx.common_drawgfx_sf(gfx3rom, sf_invert(c4), color, flipx, flipy, sx + 16, sy + 16, cliprect);
            } else {
                if (Generic.flip_screen_get() != 0) {
                    sx = 496 - sx;
                    sy = 240 - sy;
                    flipx = (flipx == 0) ? 1 : 0;
                    flipy = (flipy == 0) ? 1 : 0;
                }
                Drawgfx.common_drawgfx_sf(gfx3rom, sf_invert(c), color, flipx, flipy, sx, sy, cliprect);
            }
        }
    }

    public static void video_update_sf() {
        if ((sf_active & 0x20) != 0) {
            bg_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
        } else {
            System.arraycopy(uuB0000, 0, Video.bitmapbase[Video.curbitmap], 0, 0x2_0000);
        }
        fg_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
        if ((sf_active & 0x80) != 0) {
            draw_sprites(Video.screenstate.visarea);
        }
        tx_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
    }

    public static void video_eof() {
    }

//#endregion
}
