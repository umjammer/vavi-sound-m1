/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.cps;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m68000.MC68000;
import m1.cpu.z80.Z80A;
import m1.emu.Attotime;
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
import m1.emu.Tilemap.trans_t;
import m1.emu.Timer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.OKI6295;
import m1.sound.QSound;
import m1.sound.Sound;
import m1.sound.YM2151;


public class CPS {

    public static short[] cps_a_regs, cps_b_regs, cps2_objram1, cps2_objram2, cps2_output;
    public static byte[] mainromop, gfxrom, gfx1rom, audioromop, starsrom, user1rom;
    public static byte[] gfxram;
    public static byte[] qsound_sharedram1, qsound_sharedram2;
    public static byte[] mainram2, mainram3;
    public static byte dswa, dswb, dswc;
    public static int cps_version;
    public static int basebanksnd;
    public static int sf2ceblp_prot;
    public static int dial0, dial1;
    public static int scrollxoff, scrollyoff;
    public static int cps2networkpresent, cps2_objram_bank;
    public static int scancount, cps1_scanline1, cps1_scanline2, cps1_scancalls;
    public static List<gfx_range> lsRange0, lsRange1, lsRange2, lsRangeS;

    public static class gfx_range {

        public final int start;
        public final int end;
        public final int add;

        public gfx_range(int i1, int i2, int i3) {
            start = i1;
            end = i2;
            add = i3;
        }
    }

    public static byte[] ByteToSbyte(byte[] bb1) {
        byte[] bb2 = null;
        int n1;
        if (bb1 != null) {
            n1 = bb1.length;
            bb2 = new byte[n1];
            System.arraycopy(bb1, 0, bb2, 0, n1);
        }
        return bb2;
    }

    public static void CPSInit() {
        int i, n;
        cps_a_regs = new short[0x20];
        cps_b_regs = new short[0x20];
        gfxram = new byte[0x3_0000];
        Memory.mainram = new byte[0x1_0000];
        Memory.audioram = new byte[0x800];
        Machine.bRom = true;
        Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
        gfxrom = new byte[0x1_0000]; // Machine.GetRom("gfx.rom");
        n = gfxrom.length;
        gfx1rom = new byte[n * 2];
        for (i = 0; i < n; i++) {
            gfx1rom[i * 2] = (byte) (gfxrom[i] & 0x0f);
            gfx1rom[i * 2 + 1] = (byte) (gfxrom[i] >> 4);
        }
        Memory.audiorom = Machine.GetRom("audiocpu.rom");
        switch (Machine.sBoard) {
            case "CPS-1":
                cps_version = 1;
                starsrom = new byte[0x1_0000]; // Machine.GetRom("stars.rom");
                OKI6295.okirom = Machine.GetRom("oki.rom");
                if (Memory.mainrom == null || gfxrom == null || Memory.audiorom == null || OKI6295.okirom == null) {
                    Machine.bRom = false;
                }
                break;
            case "CPS-1(QSound)":
                cps_version = 1;
                qsound_sharedram1 = new byte[0x1000];
                qsound_sharedram2 = new byte[0x1000];
                audioromop = Machine.GetRom("audiocpuop.rom");
                user1rom = null; // Machine.GetRom("user1.rom");
                QSound.qsoundrom = ByteToSbyte(Machine.GetRom("qsound.rom"));
                if (Memory.mainrom == null || audioromop == null || gfxrom == null || Memory.audiorom == null || QSound.qsoundrom == null) {
                    Machine.bRom = false;
                }
                break;
            case "CPS2":
                cps_version = 2;
                cps2_objram1 = new short[0x1000];
                cps2_objram2 = new short[0x1000];
                cps2_output = new short[0x06];
                cps2networkpresent = 0;
                cps2_objram_bank = 0;
                scancount = 0;
                cps1_scanline1 = 262;
                cps1_scanline2 = 262;
                cps1_scancalls = 0;
                qsound_sharedram1 = new byte[0x1000];
                qsound_sharedram2 = new byte[0x1000];
                if (!Machine.sManufacturer.equals("bootleg")) {
                    mainromop = new byte[0x1_0000]; // Machine.GetRom("maincpuop.rom");
                }
                audioromop = Machine.GetRom("audiocpu.rom");
                QSound.qsoundrom = ByteToSbyte(Machine.GetRom("qsound.rom"));
                if (Memory.mainrom == null || (!Machine.sManufacturer.equals("bootleg") && mainromop == null) || audioromop == null || gfxrom == null || Memory.audiorom == null || QSound.qsoundrom == null) {
                    Machine.bRom = false;
                }
                break;
        }
        if (Machine.bRom) {
            Machine.FORM.setTitle("MAME.NET: " + Machine.sDescription + " [" + Machine.sName + "]");
            scrollxoff = 0x00;
            scrollyoff = 0x100;
            switch (Machine.sName) {
                case "forgottn":
                case "forgottna":
                case "forgottnu":
                case "forgottnue":
                case "forgottnuc":
                case "forgottnua":
                case "forgottnuaa":
                case "lostwrld":
                case "lostwrldo":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange0.add(new gfx_range(0x8000, 0xffff, -0x8000));
                    lsRange0.add(new gfx_range(0x1_0000, 0x1_7fff, -0x1_0000));
                    lsRange0.add(new gfx_range(0x1_8000, 0x1_ffff, -0x1_8000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x3fff, 0x4000));
                    lsRange1.add(new gfx_range(0x4000, 0x7fff, 0));
                    lsRange1.add(new gfx_range(0x8000, 0xbfff, -0x4000));
                    lsRange1.add(new gfx_range(0xc000, 0xffff, -0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0x1000));
                    lsRange2.add(new gfx_range(0x1000, 0x1fff, 0));
                    lsRange2.add(new gfx_range(0x2000, 0x2fff, -0x1000));
                    lsRange2.add(new gfx_range(0x3000, 0x3fff, -0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x3fff, 0));
                    break;
                case "ghouls":
                case "ghoulsu":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange0.add(new gfx_range(0x8000, 0xffff, -0x8000));
                    lsRange0.add(new gfx_range(0x1_0000, 0x1_7fff, -0x1_0000));
                    lsRange0.add(new gfx_range(0x1_8000, 0x1_ffff, -0x1_8000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x3fff, 0));
                    lsRange1.add(new gfx_range(0x4000, 0x7fff, -0x4000));
                    lsRange1.add(new gfx_range(0x8000, 0xbfff, -0x8000));
                    lsRange1.add(new gfx_range(0xc000, 0xffff, -0xc000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x13ff, 0));
                    lsRange2.add(new gfx_range(0x1400, 0x17ff, -0x400));
                    lsRange2.add(new gfx_range(0x1800, 0x1fff, -0x1000));
                    lsRange2.add(new gfx_range(0x2000, 0x2fff, -0x2000));
                    lsRange2.add(new gfx_range(0x3000, 0x3fff, -0x3000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRangeS.add(new gfx_range(0x1000, 0x1fff, 0x4000));
                    lsRangeS.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRangeS.add(new gfx_range(0x4000, 0x7fff, -0x4000));
                    lsRangeS.add(new gfx_range(0x8000, 0xbfff, -0x8000));
                    lsRangeS.add(new gfx_range(0xc000, 0xffff, -0xc000));
                    break;
                case "daimakai":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x03ff, 0x1000));
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0xc00));
                    lsRange2.add(new gfx_range(0x0800, 0x0bff, 0x800));
                    lsRange2.add(new gfx_range(0x0c00, 0x0fff, 0x400));
                    lsRange2.add(new gfx_range(0x1000, 0x13ff, 0));
                    lsRange2.add(new gfx_range(0x1400, 0x17ff, -0x400));
                    lsRange2.add(new gfx_range(0x1800, 0x1bff, -0x800));
                    lsRange2.add(new gfx_range(0x1c00, 0x1fff, -0xc00));
                    lsRange2.add(new gfx_range(0x2000, 0x23ff, -0x1000));
                    lsRange2.add(new gfx_range(0x2400, 0x27ff, -0x1400));
                    lsRange2.add(new gfx_range(0x2800, 0x2bff, -0x1800));
                    lsRange2.add(new gfx_range(0x2c00, 0x2fff, -0x1c00));
                    lsRange2.add(new gfx_range(0x3000, 0x33ff, -0x2000));
                    lsRange2.add(new gfx_range(0x3400, 0x37ff, -0x2400));
                    lsRange2.add(new gfx_range(0x3800, 0x3bff, -0x2800));
                    lsRange2.add(new gfx_range(0x3c00, 0x3fff, -0x2c00));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRangeS.add(new gfx_range(0x1000, 0x1fff, 0x4000));
                    break;
                case "daimakair":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x2000, 0x2fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0x1000));
                    lsRange2.add(new gfx_range(0x1000, 0x1fff, 0));
                    lsRange2.add(new gfx_range(0x2000, 0x2fff, -0x1000));
                    lsRange2.add(new gfx_range(0x3000, 0x3fff, -0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRangeS.add(new gfx_range(0x1000, 0x1fff, 0x4000));
                    break;
                case "strider":
                case "striderua":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xbf;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7000, 0x7fff, 0x8000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0x1000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x27ff, 0));
                    break;
                case "strideruc":
                    cpsb_addr = 0x08;
                    cpsb_value = 0x0407;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x08, 0x10, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xbf;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7000, 0x7fff, 0x8000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0x1000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x27ff, 0));
                    break;
                case "striderj":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xbf;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7000, 0x7fff, 0x8000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0x1000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x27ff, 0));
                    break;
                case "striderjr":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xbf;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7000, 0x7fff, 0x8000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0x1000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x27ff, 0));
                    break;
                case "dynwar":
                case "dynwara":
                case "dynwarj":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0002;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2c;
                    priority = new int[] {
                            0x2a, 0x28, 0x26, 0x24
                    };
                    palette_control = 0x22;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x6000, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0x4000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x07ff, 0x1000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x2fff, 0));
                    break;
                case "dynwarjr":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x6000, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x3fff, 0x4000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x07ff, 0x1000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x2fff, 0));
                    break;
                case "willow":
                case "willowu":
                case "willowuo":
                case "willowj":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x30;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x26;
                    layer_enable_mask = new int[] {
                            0x20, 0x10, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7000, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x1fff, 0x4000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0a00, 0x0dff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x27ff, 0));
                    break;
                case "unsquad":
                case "area88":
                    cpsb_addr = 0x32;
                    cpsb_value = 0x0401;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x08, 0x10, 0x20, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x2fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0c00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x17ff, 0));
                    break;
                case "area88r":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x2fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0c00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x17ff, 0));
                    break;
                case "ffight":
                case "ffighta":
                case "ffightu":
                case "ffightu1":
                case "ffightj":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0004;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2e;
                    priority = new int[] {
                            0x26, 0x30, 0x28, 0x32
                    };
                    palette_control = 0x2a;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4400, 0x4bff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0980, 0x0bff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x21ff, 0));
                    break;
                case "ffightua":
                case "ffightj1":
                case "ffightjh":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4400, 0x4bff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0980, 0x0bff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x21ff, 0));
                    break;
                case "ffightub":
                    cpsb_addr = -1;
                    cpsb_value = 0x0000;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x30;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x26;
                    layer_enable_mask = new int[] {
                            0x20, 0x10, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4400, 0x4bff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0980, 0x0bff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x21ff, 0));
                    break;
                case "ffightuc":
                case "ffightj3":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0005;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x2a, 0x2c, 0x2e, 0x30
                    };
                    palette_control = 0x32;
                    layer_enable_mask = new int[] {
                            0x02, 0x08, 0x20, 0x14, 0x14
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4400, 0x4bff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0980, 0x0bff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x21ff, 0));
                    break;
                case "ffightj2":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0002;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2c;
                    priority = new int[] {
                            0x2a, 0x28, 0x26, 0x24
                    };
                    palette_control = 0x22;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4400, 0x4bff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0980, 0x0bff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x21ff, 0));
                    break;
                case "1941":
                case "1941r1":
                case "1941u":
                case "1941j":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0005;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x2a, 0x2c, 0x2e, 0x30
                    };
                    palette_control = 0x32;
                    layer_enable_mask = new int[] {
                            0x02, 0x08, 0x20, 0x14, 0x14
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x47ff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2400, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x0fff, 0));
                    break;
                case "mercs":
                case "mercsu":
                case "mercsur1":
                case "mercsj":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0402;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2c;
                    priority = new int[] {
                            0x2a, 0x28, 0x26, 0x24
                    };
                    palette_control = 0x22;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0bff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0600, 0x1dff, 0));
                    lsRange1.add(new gfx_range(0x5400, 0x5bff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0780, 0x097f, 0));
                    lsRange2.add(new gfx_range(0x1700, 0x17ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x2600, 0x53ff, 0));
                    break;
                case "mtwins":
                case "chikij":
                    cpsb_addr = 0x1e;
                    cpsb_value = 0x0404;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x12;
                    priority = new int[] {
                            0x14, 0x16, 0x18, 0x1a
                    };
                    palette_control = 0x1c;
                    layer_enable_mask = new int[] {
                            0x08, 0x20, 0x10, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xdc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x3000, 0x3fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x37ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0e00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x17ff, 0));
                    break;
                case "msword":
                case "mswordr1":
                case "mswordu":
                case "mswordj":
                    cpsb_addr = 0x2e;
                    cpsb_value = 0x0403;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x22;
                    priority = new int[] {
                            0x24, 0x26, 0x28, 0x2a
                    };
                    palette_control = 0x2c;
                    layer_enable_mask = new int[] {
                            0x20, 0x02, 0x04, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xbc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x37ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0e00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x1fff, 0));
                    break;
                case "cawing":
                case "cawingr1":
                case "cawingu":
                case "cawingj":
                    cpsb_addr = 0x00;
                    cpsb_value = 0x0406;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x0c;
                    priority = new int[] {
                            0x0a, 0x08, 0x06, 0x04
                    };
                    palette_control = 0x02;
                    layer_enable_mask = new int[] {
                            0x10, 0x0a, 0x0a, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x5000, 0x57ff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x17ff, 0));
                    lsRange1.add(new gfx_range(0x2c00, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0600, 0x09ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x17ff, 0));
                    lsRangeS.add(new gfx_range(0x2c00, 0x3fff, 0));
                    break;
                case "nemo":
                case "nemor1":
                case "nemoj":
                    cpsb_addr = 0x0e;
                    cpsb_value = 0x0405;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x02;
                    priority = new int[] {
                            0x04, 0x06, 0x08, 0x0a
                    };
                    palette_control = 0x0c;
                    layer_enable_mask = new int[] {
                            0x04, 0x02, 0x20, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x47ff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRange1.add(new gfx_range(0x2400, 0x33ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0d00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRangeS.add(new gfx_range(0x2400, 0x3dff, 0));
                    break;
                case "sf2":
                case "sf2ug":
                    cpsb_addr = 0x32;
                    cpsb_value = 0x0401;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x08, 0x10, 0x20, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2eb":
                case "sf2ua":
                case "sf2ub":
                case "sf2uk":
                case "sf2qp1":
                case "sf2thndr":
                    cpsb_addr = 0x08;
                    cpsb_value = 0x0407;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x08, 0x10, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2ed":
                case "sf2ud":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0005;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x2a, 0x2c, 0x2e, 0x30
                    };
                    palette_control = 0x32;
                    layer_enable_mask = new int[] {
                            0x02, 0x08, 0x20, 0x14, 0x14
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2ee":
                case "sf2ue":
                    cpsb_addr = 0x10;
                    cpsb_value = 0x0408;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x1c;
                    priority = new int[] {
                            0x1a, 0x18, 0x16, 0x14
                    };
                    palette_control = 0x12;
                    layer_enable_mask = new int[] {
                            0x10, 0x08, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x3c;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2uc":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0402;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2c;
                    priority = new int[] {
                            0x2a, 0x28, 0x26, 0x24
                    };
                    palette_control = 0x22;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2uf":
                    cpsb_addr = 0x0e;
                    cpsb_value = 0x0405;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x02;
                    priority = new int[] {
                            0x04, 0x06, 0x08, 0x0a
                    };
                    palette_control = 0x0c;
                    layer_enable_mask = new int[] {
                            0x04, 0x02, 0x20, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2ui":
                    cpsb_addr = 0x1e;
                    cpsb_value = 0x0404;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x12;
                    priority = new int[] {
                            0x14, 0x16, 0x18, 0x1a
                    };
                    palette_control = 0x1c;
                    layer_enable_mask = new int[] {
                            0x08, 0x20, 0x10, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2j":
                case "sf2jh":
                    cpsb_addr = 0x2e;
                    cpsb_value = 0x0403;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x22;
                    priority = new int[] {
                            0x24, 0x26, 0x28, 0x2a
                    };
                    palette_control = 0x2c;
                    layer_enable_mask = new int[] {
                            0x20, 0x02, 0x04, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2ja":
                case "sf2jl":
                    cpsb_addr = 0x08;
                    cpsb_value = 0x0407;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x08, 0x10, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2jc":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0402;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2c;
                    priority = new int[] {
                            0x2a, 0x28, 0x26, 0x24
                    };
                    palette_control = 0x22;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2jf":
                    cpsb_addr = 0x0e;
                    cpsb_value = 0x0405;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x02;
                    priority = new int[] {
                            0x04, 0x06, 0x08, 0x0a
                    };
                    palette_control = 0x0c;
                    layer_enable_mask = new int[] {
                            0x04, 0x02, 0x20, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2ebbl":
                case "sf2ebbl2":
                case "sf2ebbl3":
                    cpsb_addr = 0x08;
                    cpsb_value = 0x0407;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x08, 0x10, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 1;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "3wonders":
                case "3wondersr1":
                case "3wondersu":
                case "wonder3":
                    cpsb_addr = 0x32;
                    cpsb_value = 0x0800;
                    mult_factor1 = 0x0e;
                    mult_factor2 = 0x0c;
                    mult_result_lo = 0x0a;
                    mult_result_hi = 0x08;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x26, 0x24, 0x22, 0x20
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x04, 0x08, 0x12, 0x12
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0x9a;
                    dswc = (byte) 0x99;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x5400, 0x6fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x1400, 0x3fff, 0x4000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x07ff, 0x1000));
                    lsRange2.add(new gfx_range(0x0e00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x29ff, 0));
                    lsRangeS.add(new gfx_range(0x2a00, 0x3fff, 0x4000));
                    break;
                case "3wondersb":
                    cpsb_addr = 0x32;
                    cpsb_value = 0x0800;
                    mult_factor1 = 0x0e;
                    mult_factor2 = 0x0c;
                    mult_result_lo = 0x0a;
                    mult_result_hi = 0x08;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x26, 0x24, 0x22, 0x20
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x04, 0x08, 0x12, 0x12
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0x88;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0x9a;
                    dswc = (byte) 0x99;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x5400, 0x6fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x1400, 0x3fff, 0x4000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x07ff, 0x1000));
                    lsRange2.add(new gfx_range(0x0e00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x29ff, 0));
                    lsRangeS.add(new gfx_range(0x2a00, 0x3fff, 0x4000));
                    break;
                case "3wondersh":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = 0x0e;
                    mult_factor2 = 0x0c;
                    mult_result_lo = 0x0a;
                    mult_result_hi = 0x08;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x26, 0x24, 0x22, 0x20
                    };
                    palette_control = 0x22;
                    layer_enable_mask = new int[] {
                            0x20, 0x04, 0x08, 0x12, 0x12
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0x9a;
                    dswc = (byte) 0x99;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x5400, 0x6fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x1400, 0x3fff, 0x4000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x07ff, 0x1000));
                    lsRange2.add(new gfx_range(0x0e00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x29ff, 0));
                    lsRangeS.add(new gfx_range(0x2a00, 0x3fff, 0x4000));
                    break;
                case "kod":
                case "kodr1":
                case "kodu":
                case "kodj":
                case "kodja":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = 0x1e;
                    mult_factor2 = 0x1c;
                    mult_result_lo = 0x1a;
                    mult_result_hi = 0x18;
                    layer_control = 0x20;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x30, 0x08, 0x30, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0xc000, 0xd7ff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x4800, 0x5fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1b00, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x47ff, 0));
                    break;
                case "captcomm":
                case "captcommr1":
                case "captcommu":
                case "captcommj":
                case "captcommjr1":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = 0x06;
                    mult_factor2 = 0x04;
                    mult_result_lo = 0x02;
                    mult_result_hi = 0x00;
                    layer_control = 0x20;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x12, 0x12, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x38;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x8000, 0xffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1000, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x7fff, 0));
                    break;
                case "captcommb":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = 0x06;
                    mult_factor2 = 0x04;
                    mult_result_lo = 0x02;
                    mult_result_hi = 0x00;
                    layer_control = 0x20;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x12, 0x12, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x38;
                    out2_addr = 0x34;
                    bootleg_kludge = 3;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x8000, 0xffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1000, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x7fff, 0));
                    break;
                case "knights":
                case "knightsu":
                case "knightsj":
                case "knightsja":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = 0x06;
                    mult_factor2 = 0x04;
                    mult_result_lo = 0x02;
                    mult_result_hi = 0x00;
                    layer_control = 0x28;
                    priority = new int[] {
                            0x26, 0x24, 0x22, 0x20
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x10, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x8000, 0x9fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x67ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1a00, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x67ff, 0));
                    break;
                case "sf2ce":
                case "sf2ceea":
                case "sf2ceua":
                case "sf2ceub":
                case "sf2ceuc":
                case "sf2ceja":
                case "sf2cejb":
                case "sf2cejc":
                case "sf2bhh":
                case "sf2rb":
                case "sf2rb2":
                case "sf2rb3":
                case "sf2red":
                case "sf2v004":
                case "sf2acc":
                case "sf2acca":
                case "sf2accp2":
                case "sf2ceblp":
                case "sf2cebltw":
                case "sf2dkot2":
                case "sf2dongb":
                case "sf2hf":
                case "sf2hfu":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2amf2":
                case "sf2m5":
                case "sf2m6":
                case "sf2m7":
                case "sf2yyc":
                case "sf2koryu":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 1;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2m2":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 1;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xec;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2m3":
                case "sf2m8":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x0e, 0x0e, 0x0e, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 2;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2m4":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x0e, 0x0e, 0x0e, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 1;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "sf2m10":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x14;
                    priority = new int[] {
                            0x12, 0x10, 0x0e, 0x0c
                    };
                    palette_control = 0x0a;
                    layer_enable_mask = new int[] {
                            0x0e, 0x0e, 0x0e, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 1;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "cworld2j":
                case "cworld2jb":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x20;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x14, 0x14, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfe;
                    dswc = (byte) 0xdf;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7800, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x37ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0e00, 0x0eff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x37ff, 0));
                    break;
                case "cworld2ja":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfe;
                    dswc = (byte) 0xdf;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7800, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x37ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0e00, 0x0eff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x37ff, 0));
                    break;
                case "varth":
                case "varthr1":
                case "varthu":
                    cpsb_addr = 0x20;
                    cpsb_value = 0x0004;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2e;
                    priority = new int[] {
                            0x26, 0x30, 0x28, 0x32
                    };
                    palette_control = 0x2a;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x3fff, 0));
                    break;
                case "varthj":
                case "varthjr":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x0e;
                    mult_factor2 = 0x0c;
                    mult_result_lo = 0x0a;
                    mult_result_hi = 0x08;
                    layer_control = 0x20;
                    priority = new int[] {
                            0x2e, 0x2c, 0x2a, 0x28
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x20, 0x04, 0x02, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x3fff, 0));
                    break;
                case "qad":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2c;
                    priority = new int[] {
                            -1, -1, -1, -1
                    };
                    palette_control = 0x12;
                    layer_enable_mask = new int[] {
                            0x14, 0x02, 0x14, 0x00, 0x00
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x3fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x07ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x1fff, 0));
                    break;
                case "qadjr":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x38;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xdf;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x07ff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x1000, 0x3fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0100, 0x03ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x1000, 0x3fff, 0));
                    break;
                case "wof":
                case "wofu":
                case "wofj":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x22;
                    priority = new int[] {
                            0x24, 0x26, 0x28, 0x2a
                    };
                    palette_control = 0x2c;
                    layer_enable_mask = new int[] {
                            0x10, 0x08, 0x04, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0xffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0xffff, 0));
                    break;
                case "wofr1":
                case "wofa":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0xffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0xffff, 0));
                    break;
                case "wofhfh":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xec;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0xffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0xffff, 0));
                    break;
                case "sf2hfj":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xf4;
                    dswc = (byte) 0x9f;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x4000, 0x4fff, 0x1_0000));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2800, 0x3fff, 0x8000));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0400, 0x07ff, 0x2000));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x8fff, 0));
                    break;
                case "dino":
                case "dinou":
                case "dinoj":
                    cpsb_addr = -1;
                    cpsb_value = -1;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x0a;
                    priority = new int[] {
                            0x0c, 0x0e, 0x00, 0x02
                    };
                    palette_control = 0x04;
                    layer_enable_mask = new int[] {
                            0x16, 0x16, 0x16, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x4000, 0x6fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1c00, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0x6fff, 0));
                    break;
                case "dinohunt":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xdc;
                    dswc = (byte) 0x9e;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x4000, 0x6fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1c00, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0x6fff, 0));
                    break;
                case "punisher":
                case "punisheru":
                case "punisherh":
                case "punisherj":
                    cpsb_addr = 0x0e;
                    cpsb_value = 0x0c00;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x12;
                    priority = new int[] {
                            0x14, 0x16, 0x08, 0x0a
                    };
                    palette_control = 0x0c;
                    layer_enable_mask = new int[] {
                            0x04, 0x02, 0x20, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x4000, 0x6dff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1b80, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0x6dff, 0));
                    break;
                case "punisherbz":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xef;
                    dswb = (byte) 0x94;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x4000, 0x6dff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1b80, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0x6dff, 0));
                    break;
                case "slammast":
                case "slammastu":
                case "mbomberj":
                    cpsb_addr = 0x2e;
                    cpsb_value = 0x0c01;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x16;
                    priority = new int[] {
                            0x00, 0x02, 0x28, 0x2a
                    };
                    palette_control = 0x2c;
                    layer_enable_mask = new int[] {
                            0x04, 0x08, 0x10, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0800, 0xb3ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x2d00, 0x2fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0xb3ff, 0));
                    break;
                case "mbombrd":
                case "mbombrdj":
                    cpsb_addr = 0x1e;
                    cpsb_value = 0x0c02;
                    mult_factor1 = -1;
                    mult_factor2 = -1;
                    mult_result_lo = -1;
                    mult_result_hi = -1;
                    layer_control = 0x2a;
                    priority = new int[] {
                            0x2c, 0x2e, 0x30, 0x32
                    };
                    palette_control = 0x1c;
                    layer_enable_mask = new int[] {
                            0x04, 0x08, 0x10, 0x00, 0x00
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0800, 0xb3ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x2d00, 0x2fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0xb3ff, 0));
                    break;
                case "pnickj":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0xdf;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0800, 0x2fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0c00, 0x0fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0x2fff, 0));
                    break;
                case "qtono2j":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x38;
                    out2_addr = 0x34;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfc;
                    dswc = (byte) 0xdf;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x0fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0200, 0x07ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0800, 0x2fff, 0));
                    break;
                case "megaman":
                case "megamana":
                case "rockmanj":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xfe;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x1_ffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0xffff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x3fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0xffff, 0));
                    break;
                case "pang3":
                case "pang3r1":
                case "pang3j":
                case "pang3b":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0xa000, 0xbfff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x4fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x1800, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x4fff, 0));
                    break;
                case "pokonyan":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x36;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xbe;
                    dswb = (byte) 0xfb;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x7000, 0x7fff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x2000, 0x37ff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0600, 0x07ff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x17ff, 0));
                    break;
                case "wofch":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0xffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0x7fff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x1fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0x7fff, 0));
                    break;
                case "sfzch":
                case "sfach":
                case "sfzbch":
                    cpsb_addr = 0x32;
                    cpsb_value = -1;
                    mult_factor1 = 0x00;
                    mult_factor2 = 0x02;
                    mult_result_lo = 0x04;
                    mult_result_hi = 0x06;
                    layer_control = 0x26;
                    priority = new int[] {
                            0x28, 0x2a, 0x2c, 0x2e
                    };
                    palette_control = 0x30;
                    layer_enable_mask = new int[] {
                            0x02, 0x04, 0x08, 0x30, 0x30
                    };
                    in2_addr = 0x00;
                    in3_addr = 0x00;
                    out2_addr = 0x00;
                    bootleg_kludge = 0;
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                    dswc = (byte) 0xff;
                    lsRange0 = new ArrayList<>();
                    lsRange0.add(new gfx_range(0x0000, 0x1_ffff, 0));
                    lsRange1 = new ArrayList<>();
                    lsRange1.add(new gfx_range(0x0000, 0xffff, 0));
                    lsRange2 = new ArrayList<>();
                    lsRange2.add(new gfx_range(0x0000, 0x3fff, 0));
                    lsRangeS = new ArrayList<>();
                    lsRangeS.add(new gfx_range(0x0000, 0xffff, 0));
                    break;
            }
            if (cps_version == 2) {
                cpsb_addr = 0x32;
                cpsb_value = -1;
                mult_factor1 = 0x00;
                mult_factor2 = 0x02;
                mult_result_lo = 0x04;
                mult_result_hi = 0x06;
                layer_control = 0x26;
                priority = new int[] {
                        0x28, 0x2a, 0x2c, 0x2e
                };
                palette_control = 0x30;
                layer_enable_mask = new int[] {
                        0x02, 0x04, 0x08, 0x30, 0x30
                };
                in2_addr = 0x00;
                in3_addr = 0x00;
                out2_addr = 0x00;
                bootleg_kludge = 0;
                lsRange0 = new ArrayList<>();
                lsRange0.add(new gfx_range(0x0000, 0x1_ffff, 0x2_0000));
                lsRange1 = new ArrayList<>();
                lsRange1.add(new gfx_range(0x0000, 0xffff, 0x1_0000));
                lsRange2 = new ArrayList<>();
                lsRange2.add(new gfx_range(0x0000, 0x3fff, 0x4000));
                lsRangeS = new ArrayList<>();
                lsRangeS.add(new gfx_range(0x0000, 0xffff, 0));
            }
        }
    }

    public static byte cps1_dsw_r(int offset) {
        String[] dswname = {"IN0", "DSWA", "DSWB", "DSWC"};
        int in0;
        if (offset == 0) {
            in0 = sbyte0;
        } else if (offset == 1) {
            in0 = dswa;
        } else if (offset == 2) {
            in0 = dswb;
        } else if (offset == 3) {
            in0 = dswc;
        } else {
            in0 = 0;
        }
        return (byte) in0;
    }

    public static void cps1_snd_bankswitch_w(byte data) {
        int bankaddr;
        bankaddr = ((data & 1) * 0x4000);
        basebanksnd = 0x1_0000 + bankaddr;
    }

    public static void cps1_oki_pin7_w(byte data) {
        OKI6295.okim6295_set_pin7(data & 1);
    }

    public static void cps1_coinctrl_w(short data) {
        Generic.coin_counter_w(0, data & 0x0100);
        Generic.coin_counter_w(1, data & 0x0200);
        Generic.coin_lockout_w(0, ~data & 0x0400);
        Generic.coin_lockout_w(1, ~data & 0x0800);
    }

    public static void qsound_banksw_w(byte data) {
        basebanksnd = 0x1_0000 + ((data & 0x0f) * 0x4000);
    }

    public static short qsound_rom_r(int offset) {
        if (user1rom != null) {
            return (short) (user1rom[offset] | 0xff00);
        } else {
            return 0;
        }
    }

    public static short qsound_sharedram1_r(int offset) {
        return (short) (qsound_sharedram1[offset] | 0xff00);
    }

    public static void qsound_sharedram1_w(int offset, byte data) {
        qsound_sharedram1[offset] = data;
    }

    public static short qsound_sharedram2_r(int offset) {
        return (short) (qsound_sharedram2[offset] | 0xff00);
    }

    public static void qsound_sharedram2_w(int offset, byte data) {
        qsound_sharedram2[offset] = data;
    }

    public static void cps1_interrupt() {
        Cpuint.cpunum_set_input_line(0, 2, LineState.HOLD_LINE);
    }

    public static void cpsq_coinctrl2_w(short data) {
        Generic.coin_counter_w(2, data & 0x01);
        Generic.coin_lockout_w(2, ~data & 0x02);
        Generic.coin_counter_w(3, data & 0x04);
        Generic.coin_lockout_w(3, ~data & 0x08);
    }

    public static int cps1_eeprom_port_r() {
        return Eeprom.eeprom_read_bit();
    }

    public static void cps1_eeprom_port_w(int data) {
        // bit 0 = data
        // bit 6 = clock
        // bit 7 = cs
        Eeprom.eeprom_write_bit(data & 0x01);
        Eeprom.eeprom_set_cs_line(((data & 0x80) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Eeprom.eeprom_set_clock_line(((data & 0x40) != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void sf2m3_layer_w(short data) {
        cps1_cps_b_w(0x0a, data);
    }

    public static short cps2_objram2_r(int offset) {
        if ((cps2_objram_bank & 1) != 0) {
            return cps2_objram1[offset];
        } else {
            return cps2_objram2[offset];
        }
    }

    public static void cps2_objram1_w(int offset, short data) {
        if ((cps2_objram_bank & 1) != 0) {
            cps2_objram2[offset] = data;
        } else {
            cps2_objram1[offset] = data;
        }
    }

    public static void cps2_objram2_w(int offset, short data) {
        if ((cps2_objram_bank & 1) != 0) {
            cps2_objram1[offset] = data;
        } else {
            cps2_objram2[offset] = data;
        }
    }

    public static short cps2_qsound_volume_r() {
        if (cps2networkpresent != 0) {
            return (short) 0x2021;
        } else {
            return (short) (0xe021);
        }
    }

    public static short kludge_r() {
        return -1;
    }

    public static void cps2_eeprom_port_bh(int data) {
        data = (data & 0xff) << 8;
        Eeprom.eeprom_write_bit(data & 0x1000);
        Eeprom.eeprom_set_clock_line(((data & 0x2000) != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        Eeprom.eeprom_set_cs_line(((data & 0x4000) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
    }

    public static void cps2_eeprom_port_bl(int data) {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), ((data & 0x0008) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Generic.coin_counter_w(0, data & 0x0001);
        Generic.coin_counter_w(1, data & 0x0002);
        Generic.coin_lockout_w(0, ~data & 0x0010);
        Generic.coin_lockout_w(1, ~data & 0x0020);
        Generic.coin_lockout_w(2, ~data & 0x0040);
        Generic.coin_lockout_w(3, ~data & 0x0080);
    }

    public static void cps2_eeprom_port_w(int data) {
        // high 8 bits
        {
            // bit 0 - Unused
            // bit 1 - Unused
            // bit 2 - Unused
            // bit 3 - Unused?
            // bit 4 - Eeprom data
            // bit 5 - Eeprom clock
            // bit 6 -
            // bit 7 -

            // EEPROM
            Eeprom.eeprom_write_bit(data & 0x1000);
            Eeprom.eeprom_set_clock_line(((data & 0x2000) != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            Eeprom.eeprom_set_cs_line(((data & 0x4000) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        }
        // low 8 bits
        {
            // bit 0 - coin counter 1
            // bit 0 - coin counter 2
            // bit 2 - Unused
            // bit 3 - Allows access to Z80 address space (Z80 reset)
            // bit 4 - lock 1
            // bit 5 - lock 2
            // bit 6 -
            // bit 7 -

            // Z80 Reset
            Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), ((data & 0x0008) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);

            Generic.coin_counter_w(0, data & 0x0001);
            Generic.coin_counter_w(1, data & 0x0002);

            Generic.coin_lockout_w(0, ~data & 0x0010);
            Generic.coin_lockout_w(1, ~data & 0x0020);
            Generic.coin_lockout_w(2, ~data & 0x0040);
            Generic.coin_lockout_w(3, ~data & 0x0080);

//            set_led_status(0, data & 0x01);
//            set_led_status(1, data & 0x10);
//            set_led_status(2, data & 0x20);
        }
    }

    public static void cps2_objram_bank_w(int data) {
        cps2_objram_bank = data & 1;
    }

    public static void cps2_interrupt() {
        // 2 is vblank, 4 is some sort of scanline interrupt, 6 is both at the same time.
        if (scancount >= 261) {
            scancount = -1;
            cps1_scancalls = 0;
        }
        scancount++;
        if ((cps_b_regs[0x10 / 2] & 0x8000) != 0) {
            cps_b_regs[0x10 / 2] = (short) (cps_b_regs[0x10 / 2] & 0x1ff);
        }
        if ((cps_b_regs[0x12 / 2] & 0x8000) != 0) {
            cps_b_regs[0x12 / 2] = (short) (cps_b_regs[0x12 / 2] & 0x1ff);
        }
//        if (cps1_scanline1 == scancount || (cps1_scanline1 < scancount && (cps1_scancalls != 0))) {
//            CPS1.cps1_cps_b_regs[0x10 / 2] = 0;
//
//            cpunum_set_input_line(machine, 0, 4, HOLD_LINE);
//            cps2_set_sprite_priorities();
//            video_screen_update_partial(machine -> primary_screen, 16 - 10 + scancount);
//            cps1_scancalls++;
//        }
//        if (cps1_scanline2 == scancount || (cps1_scanline2 < scancount && !cps1_scancalls)) {
//            cps1_cps_b_regs[0x12 / 2] = 0;
//            cpunum_set_input_line(machine, 0, 4, HOLD_LINE);
//            cps2_set_sprite_priorities();
//            video_screen_update_partial(machine -> primary_screen, 16 - 10 + scancount);
//            cps1_scancalls++;
//        }
        if (scancount == 256)  /* VBlank */ {
            cps_b_regs[0x10 / 2] = (short) cps1_scanline1;
            cps_b_regs[0x12 / 2] = (short) cps1_scanline2;
            Cpuint.cpunum_set_input_line(0, 2, LineState.HOLD_LINE);
            if (cps1_scancalls != 0) {
                cps2_set_sprite_priorities();
//                video_screen_update_partial(machine->primary_screen, 256);
            }
            cps2_objram_latch();
        }
    }

    public static void machine_reset_cps() {
        basebanksnd = 0;
    }

    public static void play_cps_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_cps_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_cps_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void play_cps_megaman(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte) 0xff));
    }

    public static void stop_cps_megaman(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte) 0xff));
    }

    public static void stopandplay_cps_megaman(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte) 0xff));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 1, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 3, 0, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 3, 1, (byte) 0xff));
    }

    public static void play_cps_dinohunt(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte) 0x33));
    }

    public static void stopandplay_cps_dinohunt(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 1, (byte) 0x33));
    }

    public static void stop_cps_strider(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0xf7));
    }

    public static void stopandplay_cps_strider(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0xf7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
    }

    public static void play_qsound_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x00, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x01, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x0f, (byte) 0x00));
    }

    public static void stop_qsound_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x00, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x01, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x0f, (byte) 0x00));
    }

    public static void stopandplay_qsound_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x00, (byte) (RomInfo.iStop >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x01, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x0f, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x00, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x01, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x0f, (byte) 0x00));
    }

    public static void play_cps2_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x00, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x01, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x0f, (byte) 0x00));
    }

    public static void stop_cps2_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x00, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x01, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x0f, (byte) 0x00));
    }

    public static void stopandplay_cps2_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x00, (byte) (RomInfo.iStop >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x01, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x0f, (byte) 0x00));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x00, (byte) (ti.TrackID >> 8)));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x01, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x0f, (byte) 0x00));
    }

//#region Video

    private static int iXAll, iYAll, nBitmap;
    private static BufferedImage bmAll = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    private static final List<String> lBitmapHash = new ArrayList<>();
    private static int cpsb_addr, cpsb_value, mult_factor1, mult_factor2, mult_result_lo, mult_result_hi;
    public static int layercontrol, layer_control, palette_control, in2_addr, in3_addr, out2_addr, bootleg_kludge;
    public static int[] priority, layer_enable_mask;
    public static int[] primasks;
    /** Base address of objects */
    private static final int CPS1_OBJ_BASE = 0;
    /** Base address of scroll 1 */
    private static final int CPS1_SCROLL1_BASE = (0x02 / 2);
    /** Base address of scroll 2 */
    private static final int CPS1_SCROLL2_BASE = (0x04 / 2);
    /** Base address of scroll 3 */
    private static final int CPS1_SCROLL3_BASE = (0x06 / 2);
    /** Base address of other video */
    private static final int CPS1_OTHER_BASE = (0x08 / 2);
    /** Base address of palette */
    private static final int CPS1_PALETTE_BASE = (0x0a / 2);
    /** Scroll 1 X */
    public static final int CPS1_SCROLL1_SCROLLX = (0x0c / 2);
    /** Scroll 1 Y */
    public static final int CPS1_SCROLL1_SCROLLY = (0x0e / 2);
    /** Scroll 2 X */
    public static final int CPS1_SCROLL2_SCROLLX = (0x10 / 2);
    /** Scroll 2 Y */
    public static final int CPS1_SCROLL2_SCROLLY = (0x12 / 2);
    /** Scroll 3 X */
    public static final int CPS1_SCROLL3_SCROLLX = (0x14 / 2);
    /** Scroll 3 Y */
    public static final int CPS1_SCROLL3_SCROLLY = (0x16 / 2);
    /** Stars 1 X */
    private static final int CPS1_STARS1_SCROLLX = (0x18 / 2);
    /** Stars 1 Y */
    private static final int CPS1_STARS1_SCROLLY = (0x1a / 2);
    /** Stars 2 X */
    private static final int CPS1_STARS2_SCROLLX = (0x1c / 2);
    /** Stars 2 Y */
    private static final int CPS1_STARS2_SCROLLY = (0x1e / 2);
    /** base of row scroll offsets in other RAM */
    public static final int CPS1_ROWSCROLL_OFFS = (0x20 / 2);
    /** flip screen, rowscroll enable */
    private static final int CPS1_VIDEOCONTROL = (0x22 / 2);
    public static int scroll1, scroll2, scroll3;
    public static int scroll1xoff = 0, scroll2xoff = 0, scroll3xoff = 0;
    public static int obj, other;
    private static short[] cps1_buffered_obj, cps2_buffered_obj, uuBFF;
    private static int cps1_last_sprite_offset;
    private static int[] cps1_stars_enabled;
    /** transparent if in none of the layers below */
    private static final byte TILEMAP_PIXEL_TRANSPARENT = 0x00;
    /** pixel is opaque in layer 0 */
    private static final byte TILEMAP_PIXEL_LAYER0 = 0x10;
    /** pixel is opaque in layer 1 */
    private static final byte TILEMAP_PIXEL_LAYER1 = 0x20;
    /** pixel is opaque in layer 2 */
    private static final byte TILEMAP_PIXEL_LAYER2 = 0x40;
    public static int scroll1x, scroll1y;
    public static int scroll2x, scroll2y;
    public static int scroll3x, scroll3y;
    private static int stars1x, stars1y, stars2x, stars2y;
    public static int pri_ctrl;

    public static boolean bRecord;

    private static int cps1_base(int offset, int boundary) {
        int base1 = cps_a_regs[offset] * 256;
        int poffset;
        base1 &= ~(boundary - 1);
        poffset = base1 & 0x3_ffff;
        return poffset / 2;
    }

    private static void cps1_cps_a_w(int add, byte data) {
        int offset = add / 2;
        if (add % 2 == 0) {
            cps_a_regs[offset] = (short) ((data << 8) | (cps_a_regs[offset] & 0xff));
        } else {
            cps_a_regs[offset] = (short) ((cps_a_regs[offset] & 0xff00) | data);
        }
        if (offset == CPS1_PALETTE_BASE) {
            cps1_build_palette(cps1_base(CPS1_PALETTE_BASE, 0x0400));
        }
    }

    private static void cps1_cps_a_w(int offset, short data) {
        cps_a_regs[offset] = data;
        if (offset == CPS1_PALETTE_BASE) {
            cps1_build_palette(cps1_base(CPS1_PALETTE_BASE, 0x0400));
        }
    }

    private static short cps1_cps_b_r(int offset) {
        if (offset == cpsb_addr / 2)
            return (short) cpsb_value;
        if (offset == mult_result_lo / 2)
            return (short) ((cps_b_regs[mult_factor1 / 2] * cps_b_regs[mult_factor2 / 2]) & 0xffff);
        if (offset == mult_result_hi / 2)
            return (short) ((cps_b_regs[mult_factor1 / 2] * cps_b_regs[mult_factor2 / 2]) >> 16);
        if (offset == in2_addr / 2) { // Extra input ports (on C-board)
            return short2;
        }
        if (offset == in3_addr / 2) { // Player 4 controls (on C-board) ("Captain Commando")
            return sbyte3;
        }
        if (cps_version == 2) {
            if (offset == 0x10 / 2) {
                return cps_b_regs[0x10 / 2];
            }
            if (offset == 0x12 / 2) {
                return cps_b_regs[0x12 / 2];
            }
        }
        return (short) 0xffff;
    }

    private static void cps1_cps_b_w(int offset, short data) {
        cps_b_regs[offset] = data;
        if (cps_version == 2) {
            if (offset == 0x0e / 2) {
                return;
            }
            if (offset == 0x10 / 2) {
                cps1_scanline1 = (data & 0x1ff);
                return;
            }
            if (offset == 0x12 / 2) {
                cps1_scanline2 = (data & 0x1ff);
                return;
            }
        }
        if (offset == out2_addr / 2) {
            Generic.coin_lockout_w(2, ~data & 0x02);
            Generic.coin_lockout_w(3, ~data & 0x08);
        }
    }

    private static void cps1_get_video_base() {
        int videocontrol;
        if (scroll1 != cps1_base(CPS1_SCROLL1_BASE, 0x4000)) {
            scroll1 = cps1_base(CPS1_SCROLL1_BASE, 0x4000);
            ttmap[0].all_tiles_dirty = true;
        }
        if (scroll2 != cps1_base(CPS1_SCROLL2_BASE, 0x4000)) {
            scroll2 = cps1_base(CPS1_SCROLL2_BASE, 0x4000);
            ttmap[1].all_tiles_dirty = true;
        }
        if (scroll3 != cps1_base(CPS1_SCROLL3_BASE, 0x4000)) {
            scroll3 = cps1_base(CPS1_SCROLL3_BASE, 0x4000);
            ttmap[2].all_tiles_dirty = true;
        }
        if (bootleg_kludge == 1) {
            cps_a_regs[CPS1_OBJ_BASE] = (short) 0x9100;
        } else if (bootleg_kludge == 2) {
            cps_a_regs[CPS1_OBJ_BASE] = (short) 0x9100;
        } else if (bootleg_kludge == 0x88) // 3wondersb
        {
            cps_b_regs[0x30 / 2] = 0x3f;
            cps_a_regs[CPS1_VIDEOCONTROL] = 0x3e;
            cps_a_regs[CPS1_SCROLL2_BASE] = (short) 0x90c0;
            cps_a_regs[CPS1_SCROLL3_BASE] = (short) 0x9100;
            cps_a_regs[CPS1_PALETTE_BASE] = (short) 0x9140;
        }
        obj = cps1_base(CPS1_OBJ_BASE, 0x800);
        other = cps1_base(CPS1_OTHER_BASE, 0x800);
        scroll1x = cps_a_regs[CPS1_SCROLL1_SCROLLX] + scroll1xoff;
        scroll1y = cps_a_regs[CPS1_SCROLL1_SCROLLY];
        scroll2x = cps_a_regs[CPS1_SCROLL2_SCROLLX] + scroll2xoff;
        scroll2y = cps_a_regs[CPS1_SCROLL2_SCROLLY];
        scroll3x = cps_a_regs[CPS1_SCROLL3_SCROLLX] + scroll3xoff;
        scroll3y = cps_a_regs[CPS1_SCROLL3_SCROLLY];
        ttmap[0].rowscroll[0] = cps_a_regs[CPS1_SCROLL1_SCROLLX] + scroll1xoff;
        ttmap[0].colscroll[0] = cps_a_regs[CPS1_SCROLL1_SCROLLX];
        ttmap[1].rowscroll[0] = cps_a_regs[CPS1_SCROLL2_SCROLLX] + scroll1xoff;
        ttmap[1].colscroll[0] = cps_a_regs[CPS1_SCROLL2_SCROLLX];
        ttmap[2].rowscroll[0] = cps_a_regs[CPS1_SCROLL3_SCROLLX] + scroll1xoff;
        ttmap[2].colscroll[0] = cps_a_regs[CPS1_SCROLL3_SCROLLX];
        stars1x = cps_a_regs[CPS1_STARS1_SCROLLX];
        stars1y = cps_a_regs[CPS1_STARS1_SCROLLY];
        stars2x = cps_a_regs[CPS1_STARS2_SCROLLX];
        stars2y = cps_a_regs[CPS1_STARS2_SCROLLY];
        layercontrol = cps_b_regs[layer_control / 2];
        videocontrol = cps_a_regs[CPS1_VIDEOCONTROL];
        ttmap[0].enable = ((layercontrol & layer_enable_mask[0]) != 0);
        ttmap[1].enable = ((layercontrol & layer_enable_mask[1]) != 0 && (videocontrol & 4) != 0);
        ttmap[2].enable = ((layercontrol & layer_enable_mask[2]) != 0 && (videocontrol & 8) != 0);
        cps1_stars_enabled[0] = layercontrol & layer_enable_mask[3];
        cps1_stars_enabled[1] = layercontrol & layer_enable_mask[4];
    }

    private static void cps1_gfxram_w(int offset) {
        int row, col;
        int page = (offset >> 7) & 0x3c0;
        int memindex;
        if (page == (cps_a_regs[CPS1_SCROLL1_BASE] & 0x3c0)) {
            memindex = offset / 2 & 0x0fff;
            row = memindex / 0x800 * 0x20;
            memindex %= 0x800;
            row += memindex % 0x20;
            col = memindex / 0x20;
            ttmap[0].tilemap_mark_tile_dirty(row, col);
        }
        if (page == (cps_a_regs[CPS1_SCROLL2_BASE] & 0x3c0)) {
            memindex = offset / 2 & 0x0fff;
            row = memindex / 0x400 * 0x10;
            memindex %= 0x400;
            row += memindex % 0x10;
            col = memindex / 0x10;
            ttmap[1].tilemap_mark_tile_dirty(row, col);
        }
        if (page == (cps_a_regs[CPS1_SCROLL3_BASE] & 0x3c0)) {
            memindex = offset / 2 & 0x0fff;
            row = memindex / 0x200 * 0x08;
            memindex %= 0x200;
            row += memindex % 0x08;
            col = memindex / 0x08;
            ttmap[2].tilemap_mark_tile_dirty(row, col);
        }
    }

    private static void cps1_update_transmasks() {
        int group, pen, mask;
        for (group = 0; group < 4; group++) {
            if (priority[group] != -1) {
                mask = cps_b_regs[priority[group] / 2] ^ 0xffff;
            } else {
                if ((layercontrol & (1 << group)) != 0) {
                    mask = 0x8000;
                } else {
                    mask = 0xffff;
                }
            }
            for (pen = 0; pen < 16; pen++) {
                byte fgbits = (((mask >> pen) & 1) != 0) ? TILEMAP_PIXEL_TRANSPARENT : TILEMAP_PIXEL_LAYER0;
                byte bgbits = (((0x8000 >> pen) & 1) != 0) ? TILEMAP_PIXEL_TRANSPARENT : TILEMAP_PIXEL_LAYER1;
                byte layermask = (byte) (fgbits | bgbits);
                if (ttmap[0].pen_to_flags[group][pen] != layermask) {
                    ttmap[0].pen_to_flags[group][pen] = layermask;
                    ttmap[0].all_tiles_dirty = true;
                }
                if (ttmap[1].pen_to_flags[group][pen] != layermask) {
                    ttmap[1].pen_to_flags[group][pen] = layermask;
                    ttmap[1].all_tiles_dirty = true;
                }
                if (ttmap[2].pen_to_flags[group][pen] != layermask) {
                    ttmap[2].pen_to_flags[group][pen] = layermask;
                    ttmap[2].all_tiles_dirty = true;
                }
            }
        }
    }

    public static void video_start_cps() {
        bmAll = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bmAll.createGraphics();
        g.setColor(Color.magenta);
        g.fillRect(0, 0, bmAll.getWidth(), bmAll.getHeight());
        g.dispose();
        int i;
        ttmap[0].enable = true;
        ttmap[1].enable = true;
        ttmap[2].enable = true;
        ttmap[0].all_tiles_dirty = true;
        ttmap[1].all_tiles_dirty = true;
        ttmap[2].all_tiles_dirty = true;
//        Arrays.fill(ttmap[0].pen_to_flags, 0, 0x40, null);
//        Arrays.fill(ttmap[1].pen_to_flags, 0, 0x40, null);
//        Arrays.fill(ttmap[2].pen_to_flags, 0, 0x40, null);
        cps1_update_transmasks();
        for (i = 0; i < 0xc00; i++) {
            Palette.palette_entry_set_color1(i, Palette.make_rgb(0, 0, 0));
        }
        primasks = new int[8];
        cps1_stars_enabled = new int[2];
        cps1_buffered_obj = new short[0x400];
        cps2_buffered_obj = new short[0x1000];
        cps2_objram1 = new short[0x1000];
        cps2_objram2 = new short[0x1000];

        uuBFF = new short[0x200 * 0x200];
        for (i = 0; i < 0x4_0000; i++) {
            uuBFF[i] = 0xbff;
        }
        Arrays.fill(cps1_buffered_obj, 0, 0x400, (short) 0);
        Arrays.fill(cps2_buffered_obj, 0, 0x1000, (short) 0);

        Arrays.fill(gfxram, 0, 0x3_0000, (byte) 0);
        Arrays.fill(cps_a_regs, 0, 0x20, (short) 0);
        Arrays.fill(cps_b_regs, 0, 0x20, (short) 0);
        Arrays.fill(cps2_objram1, 0, 0x1000, (short) 0);
        Arrays.fill(cps2_objram2, 0, 0x1000, (short) 0);

        cps_a_regs[CPS1_OBJ_BASE] = (short) 0x9200;
        cps_a_regs[CPS1_SCROLL1_BASE] = (short) 0x9000;
        cps_a_regs[CPS1_SCROLL2_BASE] = (short) 0x9040;
        cps_a_regs[CPS1_SCROLL3_BASE] = (short) 0x9080;
        cps_a_regs[CPS1_OTHER_BASE] = (short) 0x9100;
        if (bootleg_kludge == 0) {
            scroll1xoff = 0;
            scroll2xoff = 0;
            scroll3xoff = 0;
        } else if (bootleg_kludge == 1) {
            scroll1xoff = -0x0c;
            scroll2xoff = -0x0e;
            scroll3xoff = -0x10;
        } else if (bootleg_kludge == 2) {
            scroll1xoff = -0x0c;
            scroll2xoff = -0x10;
            scroll3xoff = -0x10;
        } else if (bootleg_kludge == 3) {
            scroll1xoff = -0x08;
            scroll2xoff = -0x0b;
            scroll3xoff = -0x0c;
        } else if (bootleg_kludge == 0x88) {
            scroll1xoff = 0x4;
            scroll2xoff = 0x6;
            scroll3xoff = 0xa;
        }
        cps1_get_video_base();   /* Calculate base pointers */
        cps1_get_video_base();   /* Calculate old base pointers */
    }

    private static void cps1_build_palette(int palette_offset) {
        int offset, page;
        int pallete_offset1 = palette_offset;
        int ctrl = cps_b_regs[palette_control / 2];
        for (page = 0; page < 6; ++page) {
            if (((ctrl >> page) & 1) != 0) {
                for (offset = 0; offset < 0x200; ++offset) {
                    int palette = gfxram[pallete_offset1 * 2] * 0x100 + gfxram[pallete_offset1 * 2 + 1];
                    pallete_offset1++;
                    int r, g, b, bright;
                    bright = 0x0f + ((palette >> 12) << 1);
                    r = ((palette >> 8) & 0x0f) * 0x11 * bright / 0x2d;
                    g = ((palette >> 4) & 0x0f) * 0x11 * bright / 0x2d;
                    b = ((palette >> 0) & 0x0f) * 0x11 * bright / 0x2d;
                    Palette.palette_entry_set_color1(0x200 * page + offset, Palette.make_rgb(r, g, b));
                }
            } else {
                if (pallete_offset1 != palette_offset)
                    pallete_offset1 += 0x200;
            }
        }
    }

    /** Find the offset of last sprite */
    private static void cps1_find_last_sprite() {
        int offset = 0;
        // Locate the end of table marker
        while (offset < 0x400) {
            if (bootleg_kludge == 3) {
                // captcommb - same end of sprite marker as CPS-2
                int colour = cps1_buffered_obj[offset + 1] & 0xffff;
                if (colour >= 0x8000) {
                    // Marker found. This is the last sprite.
                    cps1_last_sprite_offset = offset - 4;
                    return;
                }
            } else {
                int colour = cps1_buffered_obj[offset + 3];
                if ((colour & 0xff00) == 0xff00) {
                    // Marker found. This is the last sprite.
                    cps1_last_sprite_offset = offset - 4;
                    return;
                }
            }
            offset += 4;
        }
        // Sprites must use full sprite RAM
        cps1_last_sprite_offset = 0x400 - 4;
    }

    private static void cps1_render_sprites() {
        int i, match;
        String[] ss1;
        int[] iiRender;
        int i9, n1;
        int baseoffset, baseadd;
        Color c1 = new Color(0);
        int iSprite = 0, nSprite, iOffset, iRatio = 1, iFlip, iXCount, iYCount;
        int width1 = 512, height1 = 512;
        int[] iiX = new int[256], iiY = new int[256], iiCode = new int[256], iiColor = new int[256], iiIndex = new int[256];
        List<Integer> lX = new ArrayList<>(), lY = new ArrayList<>(), lY2 = new ArrayList<>(), lCode = new ArrayList<>(), lColor = new ArrayList<>();
        List<Integer> lFlip = new ArrayList<>();
        List<Integer> lIndex = new ArrayList<>(), lXCount = new ArrayList<>(), lYCount = new ArrayList<>();
//        ss1 = CPS1.c1.FORM.tbInput.Text.Split(sde2, StringSplitOptions.RemoveEmptyEntries);
//        n1 = ss1.Length;
//        iiRender = new int[n1];
//        for (i9 = 0; i9 < n1; i9++) {
//            iiRender[i9] = int.Parse(ss1[i9]);
//        }
        // some sf2 hacks draw the sprites in reverse order
        if ((bootleg_kludge == 1) || (bootleg_kludge == 2) || (bootleg_kludge == 3)) {
            baseoffset = cps1_last_sprite_offset;
            baseadd = -4;
        } else {
            baseoffset = 0;
            baseadd = 4;
        }
        for (i = 0; i <= cps1_last_sprite_offset; i += 4) {
            int x, y, code, colour, col;
            x = cps1_buffered_obj[baseoffset];
            y = cps1_buffered_obj[baseoffset + 1];
            code = cps1_buffered_obj[baseoffset + 2];
            colour = cps1_buffered_obj[baseoffset + 3];
            col = colour & 0x1f;
//            if ((colour & 0xff00) == 0xff00) {
//                break;
//            }
            iiX[iSprite] = x;
            iiY[iSprite] = y;
            iiCode[iSprite] = code;
            iiColor[iSprite] = colour;
//            iiIndex[iSprite] = Array.IndexOf(iiRender, iiColor[iSprite] % 32);
//            if (iiIndex[iSprite] >= 0) {
//                lX.add(iiX[iSprite]);
//                lY.add(iiY[iSprite]);
//                lCode.add(iiCode[iSprite]);
//                lColor.add(iiColor[iSprite]);
//                lIndex.add(iiIndex[iSprite]);
//                iFlip = ((iiColor[iSprite] & 0x60) >> 5) & 3;
//                iXCount = iiColor[iSprite] / 256 % 16 + 1;
//                iYCount = iiColor[iSprite] / 256 / 16 + 1;
//                lXCount.add(iXCount);
//                lYCount.add(iYCount);
//                lY2.add(iiY[iSprite] + iYCount * 16);
//                lFlip.add(iFlip);
//                iSprite++;
//            }
            if (x == 0 && y == 0 && code == 0 && colour == 0) {
                baseoffset += baseadd;
                continue;
            }
            match = 0;
            for (gfx_range r : lsRangeS) {
                if (code >= r.start && code <= r.end) {
                    code += r.add;
                    match = 1;
                    break;
                }
            }
            if (match == 0) {
                baseoffset += baseadd;
                continue;
            }
            y += 0x100;
            if ((colour & 0xff00) != 0) {
                // handle blocked sprites
                int nx = (colour & 0x0f00) >> 8;
                int ny = (colour & 0xf000) >> 12;
                int nxs, nys, sx, sy;
                nx++;
                ny++;
                if ((colour & 0x40) != 0) {
                    // Y flip
                    if ((colour & 0x20) != 0) {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16) & 0x1ff;
                                sy = (y + nys * 16) & 0x1ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, (code & ~0xf) + ((code + (nx - 1) - nxs) & 0xf) + 0x10 * (ny - 1 - nys), col, 1, 1, sx, sy, 0x8000_0002, Video.screenstate.visarea);
                            }
                        }
                    } else {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16) & 0x1ff;
                                sy = (y + nys * 16) & 0x1ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, (code & ~0xf) + ((code + nxs) & 0xf) + 0x10 * (ny - 1 - nys), col, 0, 1, sx, sy, 0x8000_0002, Video.screenstate.visarea);
                            }
                        }
                    }
                } else {
                    if ((colour & 0x20) != 0) {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16) & 0x1ff;
                                sy = (y + nys * 16) & 0x1ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, (code & ~0xf) + ((code + (nx - 1) - nxs) & 0xf) + 0x10 * nys, col, 1, 0, sx, sy, 0x8000_0002, Video.screenstate.visarea);
                            }
                        }
                    } else {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16) & 0x1ff;
                                sy = (y + nys * 16) & 0x1ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, (code & ~0xf) + ((code + nxs) & 0xf) + 0x10 * nys, col, 0, 0, sx, sy, 0x8000_0002, Video.screenstate.visarea);
                            }
                        }
                    }
                }
            } else {
                Drawgfx.common_drawgfx_c(CPS.gfx1rom, code, col, colour & 0x20, colour & 0x40, x & 0x1ff, y & 0x1ff, 0x8000_0002, Video.screenstate.visarea);
            }
            baseoffset += baseadd;
        }
    }

    private static void cps2_render_sprites() {
        int i, x, y, priority, code, colour, col, cps2_last_sprite_offset;
        int xoffs = 64 - cps2_port(0x08);
        int yoffs = 16 - cps2_port(0x0a);
        cps2_last_sprite_offset = 0x3ff;
        for (i = 0; i < 0x400; i++) {
            y = cps2_buffered_obj[i * 4 + 1] & 0xffff;
            colour = cps2_buffered_obj[i * 4 + 3] & 0xffff;
            if (y >= 0x8000 || colour >= 0xff00) {
                cps2_last_sprite_offset = i - 1;
                break;
            }
        }
        for (i = cps2_last_sprite_offset; i >= 0; i--) {
            x = cps2_buffered_obj[i * 4];
            y = cps2_buffered_obj[i * 4 + 1];
            priority = (x >> 13) & 0x07;
            code = cps2_buffered_obj[i * 4 + 2] + ((y & 0x6000) << 3);
            colour = cps2_buffered_obj[i * 4 + 3];
            col = colour & 0x1f;
            if ((colour & 0x80) != 0) {
                x += cps2_port(0x08); // fix the offset of some games
                y += cps2_port(0x0a); // like Marvel vs. Capcom ending credits
            }
            y += 0x100;
            if ((colour & 0xff00) != 0) {
                // handle blocked sprites
                int nx = (colour & 0x0f00) >> 8;
                int ny = (colour & 0xf000) >> 12;
                int nxs, nys, sx, sy;
                nx++;
                ny++;
                if ((colour & 0x40) != 0) {
                    // Y flip
                    if ((colour & 0x20) != 0) {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16 + xoffs) & 0x3ff;
                                sy = (y + nys * 16 + yoffs) & 0x3ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, code + (nx - 1) - nxs + 0x10 * (ny - 1 - nys), (col & 0x1f), 1, 1, sx, sy, primasks[priority] | 0x8000_0000, Video.screenstate.visarea);
                            }
                        }
                    } else {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16 + xoffs) & 0x3ff;
                                sy = (y + nys * 16 + yoffs) & 0x3ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, code + nxs + 0x10 * (ny - 1 - nys), (col & 0x1f), 0, 1, sx, sy, primasks[priority] | 0x8000_0000, Video.screenstate.visarea);
                            }
                        }
                    }
                } else {
                    if ((colour & 0x20) != 0) {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16 + xoffs) & 0x3ff;
                                sy = (y + nys * 16 + yoffs) & 0x3ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, code + (nx - 1) - nxs + 0x10 * nys, (col & 0x1f), 1, 0, sx, sy, primasks[priority] | 0x8000_0000, Video.screenstate.visarea);
                            }
                        }
                    } else {
                        for (nys = 0; nys < ny; nys++) {
                            for (nxs = 0; nxs < nx; nxs++) {
                                sx = (x + nxs * 16 + xoffs) & 0x3ff;
                                sy = (y + nys * 16 + yoffs) & 0x3ff;
                                Drawgfx.common_drawgfx_c(CPS.gfx1rom, (code & ~0xf) + ((code + nxs) & 0xf) + 0x10 * nys, (col & 0x1f), 0, 0, sx, sy, primasks[priority] | 0x8000_0000, Video.screenstate.visarea);
                            }
                        }
                    }
                }
            } else {
                // Simple case... 1 sprite
                Drawgfx.common_drawgfx_c(CPS.gfx1rom, code, (col & 0x1f), colour & 0x20, colour & 0x40, (x + xoffs) & 0x3ff, (y + yoffs) & 0x3ff, primasks[priority] | 0x8000_0000, Video.screenstate.visarea);
            }
        }
    }

    private static void cps1_render_stars() {
        int offs;
        if (starsrom == null && (cps1_stars_enabled[0] != 0 || cps1_stars_enabled[1] != 0)) {
            return; // stars enabled but no stars ROM
        }
        if (cps1_stars_enabled[0] != 0) {
            for (offs = 0; offs < 0x2000 / 2; offs++) {
                int col = starsrom[8 * offs + 4];
                if (col != 0x0f) {
                    int sx = (offs / 256) * 32;
                    int sy = (offs % 256);
                    sx = (sx - stars2x + (col & 0x1f)) & 0x1ff;
                    sy = ((sy - stars2y) & 0xff) + 0x100;
                    col = (int) (((col & 0xe0) >> 1) + (Video.screenstate.frame_number / 16 & 0x0f));
                    if (sx >= Video.screenstate.visarea.min_x && sx <= Video.screenstate.visarea.max_x && sy >= Video.screenstate.visarea.min_y && sy <= Video.screenstate.visarea.max_y)
                        Video.bitmapbase[Video.curbitmap][sy * 0x200 + sx] = (short) (0xa00 + col);
                }
            }
        }
        if (cps1_stars_enabled[1] != 0) {
            for (offs = 0; offs < 0x2000 / 2; offs++) {
                int col = starsrom[8 * offs];
                if (col != 0x0f) {
                    int sx = (offs / 256) * 32;
                    int sy = (offs % 256);
                    sx = (sx - stars1x + (col & 0x1f)) & 0x1ff;
                    sy = ((sy - stars1y) & 0xff) + 0x100;
                    col = (int) (((col & 0xe0) >> 1) + (Video.screenstate.frame_number / 16 & 0x0f));
                    if (sx >= Video.screenstate.visarea.min_x && sx <= Video.screenstate.visarea.max_x && sy >= Video.screenstate.visarea.min_y && sy <= Video.screenstate.visarea.max_y)
                        Video.bitmapbase[Video.curbitmap][sy * 0x200 + sx] = (short) (0x800 + col);
                }
            }
        }
    }

    private static void cps1_render_layer(int layer, byte primask) {
        switch (layer) {
            case 0:
                cps1_render_sprites();
                break;
            case 1:
                ttmap[0].tilemap_draw_primask(Video.screenstate.visarea, 0x20, primask);
                break;
            case 2:
                ttmap[1].tilemap_draw_primask(Video.screenstate.visarea, 0x20, primask);
                break;
            case 3:
                ttmap[2].tilemap_draw_primask(Video.screenstate.visarea, 0x20, primask);
                break;
        }
    }

    private static void cps1_render_high_layer(int layer) {
        switch (layer) {
            case 0:
                break;
            case 1:
                ttmap[0].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 1);
                break;
            case 2:
                ttmap[1].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 1);
                break;
            case 3:
                ttmap[2].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 1);
                break;
        }
    }

    public static void video_update_cps1() {
        int i;
        int l0, l1, l2, l3;
        int videocontrol = cps_a_regs[CPS1_VIDEOCONTROL];
        layercontrol = cps_b_regs[layer_control / 2];
        cps1_get_video_base();
        cps1_find_last_sprite();
        cps1_update_transmasks();
        ttmap[0].tilemap_set_scrollx(0, scroll1x);
        ttmap[0].tilemap_set_scrolly(0, scroll1y);
        if ((videocontrol & 0x01) != 0) { // linescroll enable
            int scrly = -scroll2y;
            int otheroffs;
            ttmap[1].scrollrows = 1024;
            otheroffs = cps_a_regs[CPS1_ROWSCROLL_OFFS];
            for (i = 0; i < 0x400; i++) { // 0x100
                ttmap[1].tilemap_set_scrollx((i - scrly) & 0x3ff, scroll2x + gfxram[(other + ((otheroffs + i) & 0x3ff)) * 2] * 0x100 + gfxram[(other + ((otheroffs + i) & 0x3ff)) * 2 + 1]);
            }
        } else {
            ttmap[1].scrollrows = 1;
            ttmap[1].tilemap_set_scrollx(0, scroll2x);
        }
        ttmap[1].tilemap_set_scrolly(0, scroll2y);
        ttmap[2].tilemap_set_scrollx(0, scroll3x);
        ttmap[2].tilemap_set_scrolly(0, scroll3y);
        System.arraycopy(uuBFF, 0, Video.bitmapbase[Video.curbitmap], 0, 0x4_0000);
        cps1_render_stars();
        l0 = (layercontrol >> 0x06) & 0x3;
        l1 = (layercontrol >> 0x08) & 0x3;
        l2 = (layercontrol >> 0x0a) & 0x3;
        l3 = (layercontrol >> 0x0c) & 0x3;
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x4_0000, null);
        if (cps_version == 1) {
            if ((bootleg_kludge & 0x80) != 0) {
                cps1_build_palette(cps1_base(CPS1_PALETTE_BASE, 0x0400));
            }
            cps1_render_layer(l0, (byte) 0);
            if (l1 == 0)
                cps1_render_high_layer(l0); // prepare mask for sprites
            cps1_render_layer(l1, (byte) 0);
            if (l2 == 0)
                cps1_render_high_layer(l1); // prepare mask for sprites
            cps1_render_layer(l2, (byte) 0);
            if (l3 == 0)
                cps1_render_high_layer(l2); // prepare mask for sprites
            cps1_render_layer(l3, (byte) 0);
        } else {
            int l0pri, l1pri, l2pri, l3pri;
            l0pri = (pri_ctrl >> 4 * l0) & 0x0f;
            l1pri = (pri_ctrl >> 4 * l1) & 0x0f;
            l2pri = (pri_ctrl >> 4 * l2) & 0x0f;
            l3pri = (pri_ctrl >> 4 * l3) & 0x0f;
            // take out the CPS1 sprites layer
            if (l0 == 0) {
                l0 = l1;
                l1 = 0;
                l0pri = l1pri;
            }
            if (l1 == 0) {
                l1 = l2;
                l2 = 0;
                l1pri = l2pri;
            }
            if (l2 == 0) {
                l2 = l3;
                l3 = 0;
                l2pri = l3pri;
            }
            {
                int mask0 = 0xaa;
                int mask1 = 0xcc;
                if (l0pri > l1pri) {
                    mask0 &= ~0x88;
                }
                if (l0pri > l2pri) {
                    mask0 &= ~0xa0;
                }
                if (l1pri > l2pri) {
                    mask1 &= ~0xc0;
                }
                primasks[0] = 0xff;
                for (i = 1; i < 8; i++) {
                    if (i <= l0pri && i <= l1pri && i <= l2pri) {
                        primasks[i] = 0xfe;
                        continue;
                    }
                    primasks[i] = 0;
                    if (i <= l0pri) {
                        primasks[i] |= mask0;
                    }
                    if (i <= l1pri) {
                        primasks[i] |= mask1;
                    }
                    if (i <= l2pri) {
                        primasks[i] |= 0xf0;
                    }
                }
            }
            cps1_render_layer(l0, (byte) 1);
            cps1_render_layer(l1, (byte) 2);
            cps1_render_layer(l2, (byte) 4);
            cps2_render_sprites(); // screen->machine, bitmap, cliprect, primasks);
        }
    }

    public static void video_eof_cps1() {
        int i;
        cps1_get_video_base();
        if (cps_version == 1) {
            for (i = 0; i < 0x400; i++) {
                cps1_buffered_obj[i] = (short) (gfxram[obj * 2 + i * 2] * 0x100 + gfxram[obj * 2 + i * 2 + 1]);
            }
        }
    }

    public static int cps2_port(int offset) {
        return cps2_output[offset / 2];
    }

    public static void cps2_set_sprite_priorities() {
        pri_ctrl = cps2_port(0x04);
    }

    public static void cps2_objram_latch() {
        cps2_set_sprite_priorities();
        //memcpy(cps2_buffered_obj, cps2_objbase(), cps2_obj_size);
        int baseptr;
        baseptr = 0x7000;
        if ((cps2_objram_bank & 1) != 0) {
            baseptr ^= 0x0080;
        }
        if (baseptr == 0x7000) {
            System.arraycopy(cps2_objram1, 0, cps2_buffered_obj, 0, 0x1000);
        } else {
            System.arraycopy(cps2_objram2, 0, cps2_buffered_obj, 0, 0x1000);
        }
    }

//#endregion Tilemap

    public static Tmap[] ttmap;

    public static void tilemap_init() {
        int i;
        ttmap = new Tmap[3];
        ttmap[0] = new Tmap();
        ttmap[0].tilewidth = 8;
        ttmap[0].tileheight = 8;
        ttmap[0].width = 0x200;
        ttmap[0].height = 0x200;
        ttmap[0].scrollrows = 1;
        ttmap[0].pixmap = new short[0x200 * 0x200];
        ttmap[0].flagsmap = new byte[0x200][0x200];
        ttmap[0].tileflags = new byte[0x40][0x40];
        ttmap[0].pen_to_flags = new byte[4][16];
        ttmap[0].pen_data = new byte[0x40];
        ttmap[1] = new Tmap();
        ttmap[1].tilewidth = 0x10;
        ttmap[1].tileheight = 0x10;
        ttmap[1].width = 0x400;
        ttmap[1].height = 0x400;
        ttmap[1].scrollrows = 0x400;
        ttmap[1].pixmap = new short[0x400 * 0x400];
        ttmap[1].flagsmap = new byte[0x400][0x400];
        ttmap[1].tileflags = new byte[0x40][0x40];
        ttmap[1].pen_to_flags = new byte[4][16];
        ttmap[1].pen_data = new byte[0x100];
        ttmap[2] = new Tmap();
        ttmap[2].tilewidth = 0x20;
        ttmap[2].tileheight = 0x20;
        ttmap[2].width = 0x800;
        ttmap[2].height = 0x800;
        ttmap[2].scrollrows = 1;
        ttmap[2].pixmap = new short[0x800 * 0x800];
        ttmap[2].flagsmap = new byte[0x800][0x800];
        ttmap[2].tileflags = new byte[0x40][0x40];
        ttmap[2].pen_to_flags = new byte[4][16];
        ttmap[2].pen_data = new byte[0x400];
        for (i = 0; i < 3; i++) {
            ttmap[i].rows = 0x40;
            ttmap[i].cols = 0x40;
            ttmap[i].enable = true;
            ttmap[i].all_tiles_dirty = true;
            ttmap[i].scrollcols = 1;
            ttmap[i].rowscroll = new int[ttmap[i].scrollrows];
            ttmap[i].colscroll = new int[ttmap[i].scrollcols];
            ttmap[i].tilemap_draw_instance3 = ttmap[i]::tilemap_draw_instanceC;
            ttmap[i].tilemap_set_scrolldx(0, 0);
            ttmap[i].tilemap_set_scrolldy(0x100, 0);
        }
        ttmap[0].tile_update3 = ttmap[0]::tile_updateC0;
        ttmap[1].tile_update3 = ttmap[1]::tile_updateC1;
        ttmap[2].tile_update3 = ttmap[2]::tile_updateC2;
        ttmap[0].total_elements = CPS.gfxrom.length / 0x40;
        ttmap[1].total_elements = CPS.gfxrom.length / 0x80;
        ttmap[2].total_elements = CPS.gfxrom.length / 0x200;
    }

    public static class Tmap extends Tilemap.Tmap {

        public void tile_updateC0(int col, int row) {
            byte group0, flags0;
            int x0 = 0x08 * col;
            int y0 = 0x08 * row;
            int palette_base0;
            int code, attr;
            int memindex;
            int gfxset;
            int match;
            int i, j;
            memindex = (row & 0x1f) + ((col & 0x3f) << 5) + ((row & 0x20) << 6);
            {
                code = CPS.gfxram[(CPS.scroll1 + 2 * memindex) * 2] * 0x100 + CPS.gfxram[(CPS.scroll1 + 2 * memindex) * 2 + 1];
                match = 0;
                for (CPS.gfx_range r : CPS.lsRange0) {
                    if (code >= r.start && code <= r.end) {
                        code += r.add;
                        match = 1;
                        break;
                    }
                }
                code %= CPS.ttmap[0].total_elements;
                gfxset = (memindex & 0x20) >> 5;
                attr = CPS.gfxram[(CPS.scroll1 + 2 * memindex + 1) * 2] * 0x100 + CPS.gfxram[(CPS.scroll1 + 2 * memindex + 1) * 2 + 1];
                {
                    if (match == 0) {
                        System.arraycopy(Tilemap.bb0F, 0, pen_data, 0, 0x40);
                    } else {
                        for (j = 0; j < 0x08; j++) {
                            System.arraycopy(CPS.gfx1rom, code * 0x80 + gfxset * 8 + j * 0x10, pen_data, j * 8, 8);
                        }
                    }
                    palette_base0 = 0x10 * ((attr & 0x1f) + 0x20);
                    flags0 = (byte) (((attr & 0x60) >> 5) & 3);
                }
                group0 = (byte) ((attr & 0x0180) >> 7);
            }
            {
                int offset = 0;
                byte andmask = (byte) 0xff, ormask = 0;
                int dx0 = 1, dy0 = 1;
                int tx, ty;
                if ((flags0 & Tilemap.TILE_FLIPY) != 0) {
                    y0 += 0x07;
                    dy0 = -1;
                }
                if ((flags0 & Tilemap.TILE_FLIPX) != 0) {
                    x0 += 0x07;
                    dx0 = -1;
                }
                for (ty = 0; ty < 0x08; ty++) {
                    int offsetx1 = x0;
                    int offsety1 = y0;
                    int xoffs = 0;
                    y0 += dy0;
                    for (tx = 0; tx < 0x08; tx++) {
                        byte pen, map;
                        pen = pen_data[offset];
                        map = pen_to_flags[group0][pen];
                        pixmap[offsety1 * 0x200 + offsetx1 + xoffs] = (short) (palette_base0 + pen);
                        flagsmap[offsety1][offsetx1 + xoffs] = map;
                        andmask &= map;
                        ormask |= map;
                        xoffs += dx0;
                        offset++;
                    }
                }
                tileflags[row][col] = (byte) (andmask ^ ormask);
            }
        }

        public void tile_updateC1(int col, int row) {
            byte group1, flags1;
            int x0 = 0x10 * col;
            int y0 = 0x10 * row;
            int palette_base1;
            int code, attr;
            int memindex;
            int match;
            memindex = (row & 0x0f) + ((col & 0x3f) << 4) + ((row & 0x30) << 6);
            {
                code = CPS.gfxram[(CPS.scroll2 + 2 * memindex) * 2] * 0x100 + CPS.gfxram[(CPS.scroll2 + 2 * memindex) * 2 + 1];
                match = 0;
                for (CPS.gfx_range r : CPS.lsRange1) {
                    if (code >= r.start && code <= r.end) {
                        code += r.add;
                        match = 1;
                        break;
                    }
                }
                code %= CPS.ttmap[1].total_elements;
                attr = CPS.gfxram[(CPS.scroll2 + 2 * memindex + 1) * 2] * 0x100 + CPS.gfxram[(CPS.scroll2 + 2 * memindex + 1) * 2 + 1];
                if (match == 0) {
                    System.arraycopy(Tilemap.bb0F, 0, pen_data, 0, 0x100);
                } else {
                    System.arraycopy(CPS.gfx1rom, code * 0x100, pen_data, 0, 0x100);
                }
                palette_base1 = 0x10 * ((attr & 0x1f) + 0x40);
                flags1 = (byte) (((attr & 0x60) >> 5) & 3);
                group1 = (byte) ((attr & 0x0180) >> 7);
            }
            {
                int offset = 0;
                byte andmask = (byte) 0xff, ormask = 0;
                int dx0 = 1, dy0 = 1;
                int tx, ty;
                if ((flags1 & Tilemap.TILE_FLIPY) != 0) {
                    y0 += 0x0f;
                    dy0 = -1;
                }
                if ((flags1 & Tilemap.TILE_FLIPX) != 0) {
                    x0 += 0x0f;
                    dx0 = -1;
                }
                for (ty = 0; ty < 0x10; ty++) {
                    int offsetx1 = x0;
                    int offsety1 = y0;
                    int xoffs = 0;
                    y0 += dy0;
                    for (tx = 0; tx < 0x10; tx++) {
                        byte pen, map;
                        pen = pen_data[offset];
                        map = pen_to_flags[group1][pen];
                        pixmap[offsety1 * 0x400 + offsetx1 + xoffs] = (short) (palette_base1 + pen);
                        flagsmap[offsety1][offsetx1 + xoffs] = map;
                        andmask &= map;
                        ormask |= map;
                        xoffs += dx0;
                        offset++;
                    }
                }
                tileflags[row][col] = (byte) (andmask ^ ormask);
            }
        }

        public void tile_updateC2(int col, int row) {
            byte group2, flags2;
            int x0 = 0x20 * col;
            int y0 = 0x20 * row;
            int palette_base2;
            int code, attr;
            int memindex;
            int match;
            memindex = (row & 0x07) + ((col & 0x3f) << 3) + ((row & 0x38) << 6);
            {
                code = (CPS.gfxram[(CPS.scroll3 + 2 * memindex) * 2] * 0x100 + CPS.gfxram[(CPS.scroll3 + 2 * memindex) * 2 + 1]) & 0x3fff;
                match = 0;
                for (CPS.gfx_range r : CPS.lsRange2) {
                    if (code >= r.start && code <= r.end) {
                        code += r.add;
                        match = 1;
                        break;
                    }
                }
                code %= CPS.ttmap[2].total_elements;
                attr = CPS.gfxram[(CPS.scroll3 + 2 * memindex + 1) * 2] * 0x100 + CPS.gfxram[(CPS.scroll3 + 2 * memindex + 1) * 2 + 1];
                if (match == 0) {
                    System.arraycopy(Tilemap.bb0F, 0, pen_data, 0, 0x400);
                } else {
                    System.arraycopy(CPS.gfx1rom, code * 0x400, pen_data, 0, 0x400);
                }
                palette_base2 = 0x10 * ((attr & 0x1f) + 0x60);
                flags2 = (byte) (((attr & 0x60) >> 5) & 3);
                group2 = (byte) ((attr & 0x0180) >> 7);
            }
            {
                int offset = 0;
                byte andmask = (byte) 0xff, ormask = 0;
                int dx0 = 1, dy0 = 1;
                int tx, ty;
                if ((flags2 & Tilemap.TILE_FLIPY) != 0) {
                    y0 += 0x1f;
                    dy0 = -1;
                }
                if ((flags2 & Tilemap.TILE_FLIPX) != 0) {
                    x0 += 0x1f;
                    dx0 = -1;
                }
                for (ty = 0; ty < 0x20; ty++) {
                    int offsetx1 = x0;
                    int offsety1 = y0;
                    int xoffs = 0;
                    y0 += dy0;
                    for (tx = 0; tx < 0x20; tx++) {
                        byte pen, map;
                        pen = pen_data[offset];
                        map = pen_to_flags[group2][pen];
                        pixmap[offsety1 * 0x800 + offsetx1 + xoffs] = (short) (palette_base2 + pen);
                        flagsmap[offsety1][offsetx1 + xoffs] = map;
                        andmask &= map;
                        ormask |= map;
                        xoffs += dx0;
                        offset++;
                    }
                }
                tileflags[row][col] = (byte) (andmask ^ ormask);
            }
        }

        public void tilemap_draw_instanceC(Tilemap.RECT cliprect, int xpos, int ypos) {
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
    }

//#region Input

    public static void loop_inputports_cps1_6b() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
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
            short1 &= ~0x01;
        } else {
            short1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short1 &= ~0x02;
        } else {
            short1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short1 &= ~0x04;
        } else {
            short1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short1 &= ~0x08;
        } else {
            short1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x10;
        } else {
            short1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short1 &= ~0x20;
        } else {
            short1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short1 &= ~0x40;
        } else {
            short1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short2 &= ~0x01;
        } else {
            short2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short2 &= ~0x02;
        } else {
            short2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            short2 &= ~0x04;
        } else {
            short2 |= 0x04;
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
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short2 &= ~0x10;
        } else {
            short2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            short2 &= ~0x20;
        } else {
            short2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {
            short2 &= ~0x40;
        } else {
            short2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
    }

    public static void loop_inputports_cps1_forgottn() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
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
            short1 &= ~0x01;
        } else {
            short1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short1 &= ~0x02;
        } else {
            short1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short1 &= ~0x04;
        } else {
            short1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short1 &= ~0x08;
        } else {
            short1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x10;
        } else {
            short1 |= 0x10;
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
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
        Inptport.frame_update_analog_field_forgottn_p0(Inptport.analog_p0);
        Inptport.frame_update_analog_field_forgottn_p1(Inptport.analog_p1);
    }

    public static void loop_inputports_cps1_sf2hack() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
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
            short1 &= ~0x01;
        } else {
            short1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short1 &= ~0x02;
        } else {
            short1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short1 &= ~0x04;
        } else {
            short1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short1 &= ~0x08;
        } else {
            short1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x10;
        } else {
            short1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short1 &= ~0x20;
        } else {
            short1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short1 &= ~0x40;
        } else {
            short1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short2 &= ~0x0200;
        } else {
            short2 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            short2 &= ~0x0400;
        } else {
            short2 |= 0x0400;
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
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
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
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
    }

    public static void loop_inputports_cps1_cworld2j() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
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
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x10;
        } else {
            short1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short1 &= ~0x20;
        } else {
            short1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short1 &= ~0x40;
        } else {
            short1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short1 &= ~0x80;
        } else {
            short1 |= 0x80;
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
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short1 &= (short) ~0x8000;
        } else {
            short1 |= (short) 0x8000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
    }

    public static void loop_inputports_cps2_2p6b() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short2 &= ~0x1000;
        } else {
            short2 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short2 &= ~0x2000;
        } else {
            short2 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short2 &= ~0x0200;
        } else {
            short2 |= 0x0200;
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
            short0 &= ~0x0010;
        } else {
            short0 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short0 &= ~0x0020;
        } else {
            short0 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short0 &= ~0x0040;
        } else {
            short0 |= 0x0040;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short1 &= ~0x0001;
        } else {
            short1 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short1 &= ~0x0002;
        } else {
            short1 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            short1 &= ~0x0004;
        } else {
            short1 |= 0x0004;
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
            short0 &= ~0x1000;
        } else {
            short0 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short0 &= ~0x2000;
        } else {
            short0 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            short0 &= ~0x4000;
        } else {
            short0 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short1 &= ~0x0010;
        } else {
            short1 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            short1 &= ~0x0020;
        } else {
            short1 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {
            short2 &= ~0x4000;
        } else {
            short2 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            short2 &= ~0x0004;
        } else {
            short2 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            short2 &= ~0x0002;
        } else {
            short2 |= 0x0002;
        }
    }

    public static void loop_inputports_cps2_ecofghtr() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short2 &= ~0x1000;
        } else {
            short2 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short2 &= ~0x2000;
        } else {
            short2 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short2 &= ~0x0200;
        } else {
            short2 |= 0x0200;
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
            short0 &= ~0x0010;
        } else {
            short0 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short0 &= ~0x0020;
        } else {
            short0 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short0 &= ~0x0040;
        } else {
            short0 |= 0x0040;
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
            short0 &= ~0x1000;
        } else {
            short0 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short0 &= ~0x2000;
        } else {
            short0 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            short0 &= ~0x4000;
        } else {
            short0 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            short2 &= ~0x0004;
        } else {
            short2 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            short2 &= ~0x0002;
        } else {
            short2 |= 0x0002;
        }
        Inptport.frame_update_analog_field_ecofghtr_p0(Inptport.analog_p0);
        Inptport.frame_update_analog_field_ecofghtr_p1(Inptport.analog_p1);
    }

    public static void loop_inputports_cps2_qndream() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short2 &= ~0x1000;
        } else {
            short2 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short2 &= ~0x2000;
        } else {
            short2 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short2 &= ~0x0200;
        } else {
            short2 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short0 &= ~0x0008;
        } else {
            short0 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short0 &= ~0x0004;
        } else {
            short0 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            short0 &= ~0x0002;
        } else {
            short0 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short0 &= ~0x0001;
        } else {
            short0 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short0 &= ~0x0800;
        } else {
            short0 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short0 &= ~0x0400;
        } else {
            short0 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            short0 &= ~0x0200;
        } else {
            short0 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short0 &= ~0x0100;
        } else {
            short0 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            short2 &= ~0x0004;
        } else {
            short2 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            short2 &= ~0x0002;
        } else {
            short2 |= 0x0002;
        }
    }

    public static void record_portC() {
        if (sbyte0 != sbyte0_old || short1 != short1_old || short2 != short2_old || sbyte3 != sbyte3_old) {
            sbyte0_old = sbyte0;
            short1_old = short1;
            short2_old = short2;
            sbyte3_old = sbyte3;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(short1);
            Mame.bwRecord.write(short2);
            Mame.bwRecord.write(sbyte3);
        }
    }

    public static void record_portC2() {
        if (short0 != short0_old || short1 != short1_old || short2 != short2_old) {
            short0_old = short0;
            short1_old = short1;
            short2_old = short2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(short0);
            Mame.bwRecord.write(short1);
            Mame.bwRecord.write(short2);
        }
    }

    public static void replay_portC() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                short1_old = Mame.brRecord.readInt16();
                short2_old = Mame.brRecord.readInt16();
                sbyte3_old = Mame.brRecord.readSByte();
            } catch (Exception e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            short1 = short1_old;
            short2 = short2_old;
            sbyte3 = sbyte3_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void replay_portC2() {
        if (Inptport.bReplayRead) {
            try {
                Video.screenstate.frame_number = Mame.brRecord.readInt64();
                short0_old = Mame.brRecord.readInt16();
                short1_old = Mame.brRecord.readInt16();
                short2_old = Mame.brRecord.readInt16();
            } catch (Exception e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Attotime.attotime_compare(Timer.global_basetime, Timer.global_basetime_obj) == 0) {
            short0 = short0_old;
            short1 = short1_old;
            short2 = short2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Memory

    public static short short0, short1, short2;
    public static byte sbyte0, sbyte3;
    public static short short0_old, short1_old, short2_old;
    public static byte sbyte0_old, sbyte3_old;

    public static byte MCReadOpByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
//        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
//            result = (byte) gfxram[(address & 0x3_ffff)];
//        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
//            result = (byte) Memory.mainram[address & 0xffff];
        }
        return result;
    }

    public static byte MCReadByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0007) {
            if (address == 0x80_0000) {
                result = (byte) (short1 >> 8);
            } else if (address == 0x80_0001) {
                result = (byte) (short1);
            } else {
                result = -1;
            }
        } else if (address >= 0x80_0018 && address <= 0x80_001f) {
            int offset = (address - 0x80_0018) / 2;
            result = cps1_dsw_r(offset);
        } else if (address >= 0x80_0020 && address <= 0x80_0021) {
            result = 0;
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            if (address % 2 == 0) {
                result = (byte) (cps1_cps_b_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) cps1_cps_b_r(offset);
            }
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            result = Memory.mainram[address & 0xffff];
        }
        return result;
    }

    public static short MCReadOpWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static short MCReadWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0007) {
            result = short1;// input_port_4_word_r
        } else if (address >= 0x80_0018 && address + 1 <= 0x80_001f) {
            int offset = (address - 0x80_0018) / 2;
            result = (short) ((cps1_dsw_r(offset) << 8) | cps1_dsw_r(offset));
        } else if (address >= 0x80_0020 && address + 1 <= 0x80_0021) {
            result = 0;
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static int MCReadOpLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static int MCReadLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_0007) {
            result = 0;
        } else if (address >= 0x80_0018 && address + 3 <= 0x80_001f) {
            result = 0;
        } else if (address >= 0x80_0020 && address + 3 <= 0x80_0021) {
            result = 0;
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset) * 0x1_0000 + cps1_cps_b_r(offset + 1);
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static void MCWriteByte(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address <= 0x80_0037) {
            if (address % 2 == 0) {
                cps1_coinctrl_w((short) (value * 0x100));
            } else {
            }
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
        } else if (address >= 0x80_0180 && address <= 0x80_0187) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x80_0188 && address <= 0x80_018f) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = value;
        } else {
            int i1 = 1;
        }
    }

    public static void MCWriteWord(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 1 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 1 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0180 && address + 1 <= 0x80_0187) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x80_0188 && address + 1 <= 0x80_018f) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 1] = (byte) value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value);
        } else {
            int i1 = 1;
        }
    }

    public static void MCWriteLong(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 3 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 3 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_a_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_b_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0180 && address + 3 <= 0x80_0187) {
        } else if (address >= 0x80_0188 && address + 3 <= 0x80_018f) {
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 24);
            gfxram[(address & 0x3_ffff) + 1] = (byte) (value >> 16);
            gfxram[(address & 0x3_ffff) + 2] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 3] = (byte) (value);
            cps1_gfxram_w((address & 0x3_ffff) / 2);
            cps1_gfxram_w(((address + 2) & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) (value);
        } else {
            int i1 = 1;
        }
    }

    public static byte ZCReadOp(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address & 0x7fff];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZCReadMemory(short address) {
        byte result = 0;
        if (address < 0x8000) {
            result = Memory.audiorom[address & 0x7fff];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            result = Memory.audiorom[basebanksnd + (address & 0x3fff)];
        } else if (address >= 0xd000 && address <= 0xd7ff) {
            result = Memory.audioram[address & 0x7ff];
        } else if (address == 0xf001) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address == 0xf002) {
            result = OKI6295.okim6295_status_0_r();
        } else if (address == 0xf008) {
            result = (byte) Sound.soundlatch_r();
        } else if (address == 0xf00a) {
            result = (byte) Sound.soundlatch2_r();
        } else {
            result = 0;
        }
        return result;
    }

    public static void ZCWriteMemory(short address, byte value) {
        if (address >= 0xd000 && address <= 0xd7ff) {
            Memory.audioram[address & 0x7ff] = value;
        } else if (address == 0xf000) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xf001) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address == 0xf002) {
            OKI6295.okim6295_data_0_w(value);
        } else if (address == 0xf004) {
            cps1_snd_bankswitch_w(value);
        } else if (address == 0xf006) {
            cps1_oki_pin7_w(value);
        } else {

        }
    }

    public static byte MQReadOpByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        }
        return result;
    }

    public static byte MQReadByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0007) {
            if (address == 0x80_0000) // IN1
            {
                result = (byte) (short1 >> 8);
            } else if (address == 0x80_0001) {
                result = (byte) (short1);
            } else {
                result = -1;
            }
        } else if (address >= 0x80_0018 && address <= 0x80_001f) {
            int offset = (address - 0x80_0018) / 2;
            result = cps1_dsw_r(offset);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        } else if (address >= 0xf0_0000 && address <= 0xf0_ffff) {
            int offset = (address - 0xf0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (qsound_rom_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) qsound_rom_r(offset);
            }
        } else if (address >= 0xf1_8000 && address <= 0xf1_9fff) {
            int offset = (address - 0xf1_8000) / 2;
            result = (byte) qsound_sharedram1_r(offset);
        } else if (address >= 0xf1_c000 && address <= 0xf1_c001) {
            result = (byte) short2;
        } else if (address >= 0xf1_c002 && address <= 0xf1_c003) {
            result = sbyte3;
        } else if (address >= 0xf1_c006 && address <= 0xf1_c007) {
            result = (byte) cps1_eeprom_port_r();
        } else if (address >= 0xf1_e000 && address <= 0xf1_ffff) {
            int offset = (address - 0xf1_e000) / 2;
            result = (byte) qsound_sharedram2_r(offset);
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            result = Memory.mainram[address & 0xffff];
        }
        return result;
    }

    public static short MQReadOpWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static short MQReadWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0007) {
            result = short1; // input_port_4_word_r
        } else if (address >= 0x80_0018 && address + 1 <= 0x80_001f) {
            int offset = (address - 0x80_0018) / 2;
            result = cps1_dsw_r(offset);
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xf0_0000 && address + 1 <= 0xf0_ffff) {
            int offset = (address - 0xf0_0000) / 2;
            result = qsound_rom_r(offset);
        } else if (address >= 0xf1_8000 && address + 1 <= 0xf1_9fff) {
            int offset = (address - 0xf1_8000) / 2;
            result = qsound_sharedram1_r(offset);
        } else if (address >= 0xf1_c000 && address + 1 <= 0xf1_c001) {
            result = (short) ((int) short2 & 0xff);
        } else if (address >= 0xf1_c002 && address + 1 <= 0xf1_c003) {
            result = (short) ((int) sbyte3 & 0xff);
        } else if (address >= 0xf1_c006 && address + 1 <= 0xf1_c007) {
            result = (short) cps1_eeprom_port_r();
        } else if (address >= 0xf1_e000 && address + 1 <= 0xf1_ffff) {
            int offset = (address - 0xf1_e000) / 2;
            result = qsound_sharedram2_r(offset);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static int MQReadOpLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static int MQReadLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_0007) {
            result = -1; // short1
        } else if (address >= 0x80_0018 && address + 3 <= 0x80_001f) {
            result = 0; // cps1_dsw_r
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            result = 0; // cps1_cps_b_r
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xf0_0000 && address + 3 <= 0xf0_ffff) {
            result = 0; // qsound_rom_r
        } else if (address >= 0xf1_8000 && address + 3 <= 0xf1_9fff) {
            result = 0; // qsound_sharedram1_r
        } else if (address >= 0xf1_c000 && address + 3 <= 0xf1_c001) {
            result = (int) short2 & 0xff;
        } else if (address >= 0xf1_c002 && address + 3 <= 0xf1_c003) {
            result = (int) sbyte3 & 0xff;
        } else if (address >= 0xf1_c006 && address + 3 <= 0xf1_c007) {
            result = 0; // cps1_eeprom_port_r();
        } else if (address >= 0xf1_e000 && address + 3 <= 0xf1_ffff) {
            result = 0; // qsound_sharedram2_r
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static void MQWriteByte(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address <= 0x80_0037) {
            if (address % 2 == 0) {
                cps1_coinctrl_w((short) (value * 0x100));
            } else {
                int i11 = 1;
            }
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            int i11 = 1; // cps1_cps_a_w
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int i11 = 1; // cps1_cps_b_w
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xf1_8000 && address <= 0xf1_9fff) {
            int offset = (address - 0xf1_8000) / 2;
            if ((address & 1) == 1) {
                qsound_sharedram1_w(offset, value);
            } else {
                int i1 = 1;
            }
        } else if (address >= 0xf1_c004 && address <= 0xf1_c005) {
            int i11 = 1; // cpsq_coinctrl2_w
        } else if (address >= 0xf1_c006 && address <= 0xf1_c007) {
            if ((address & 1) == 1) {
                cps1_eeprom_port_w(value);
            }
        } else if (address >= 0xf1_e000 && address <= 0xf1_ffff) {
            int offset = (address - 0xf1_e000) / 2;
            if ((address & 1) == 1) {
                qsound_sharedram2_w(offset, value);
            } else {
                int i1 = 1;
            }
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = value;
        } else {
            int i11 = 1;
        }
    }

    public static void MQWriteWord(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 1 <= 0x80_0037) {
            if (address % 2 == 0) {
                cps1_coinctrl_w((short) (value * 0x100));
            } else {
                int i11 = 1;
            }
        } else if (address >= 0x80_0100 && address + 1 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 1] = (byte) value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xf1_8000 && address + 1 <= 0xf1_9fff) {
            qsound_sharedram1_w((address - 0xf1_8000) >> 1, (byte) value);
        } else if (address >= 0xf1_c004 && address + 1 <= 0xf1_c005) {
            cpsq_coinctrl2_w(value);
        } else if (address >= 0xf1_c006 && address + 1 <= 0xf1_c007) {
            cps1_eeprom_port_w(value);
        } else if (address >= 0xf1_e000 && address + 1 <= 0xf1_ffff) {
            qsound_sharedram2_w((address - 0xf1_e000) >> 1, (byte) value);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value);
        } else {
            int i11 = 1;
        }
    }

    public static void MQWriteLong(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 3 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 3 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_a_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            //cps1_cps_b_w
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            if (address == 0x90_4000) {
                int i11 = 1;
            }
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 24);
            gfxram[(address & 0x3_ffff) + 1] = (byte) (value >> 16);
            gfxram[(address & 0x3_ffff) + 2] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 3] = (byte) (value);
            cps1_gfxram_w((address & 0x3_ffff) / 2);
            cps1_gfxram_w(((address + 2) & 0x3_ffff) / 2);
        } else if (address >= 0xf1_8000 && address + 3 <= 0xf1_9fff) {
            int i11 = 1; // qsound_sharedram1_w
        } else if (address >= 0xf1_c004 && address + 3 <= 0xf1_c005) {
            int i11 = 1; // cpsq_coinctrl2_w
        } else if (address >= 0xf1_c006 && address + 3 <= 0xf1_c007) {
            int i11 = 1; // cps1_eeprom_port_w
        } else if (address >= 0xf1_e000 && address + 3 <= 0xf1_ffff) {
            int i11 = 1; // qsound_sharedram2_w
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) (value);
        } else {
            int i11 = 1;
        }
    }

    public static byte MC2ReadOpByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < mainromop.length) {
                result = mainromop[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MC2ReadPcrelByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < mainromop.length) {
                result = mainromop[address];
            } else {
                result = 0;

            }
        } else {
            result = MC2ReadByte(address);
        }
        return result;
    }

    public static byte MC2ReadByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            result = (byte) cps2_output[offset];
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            result = (byte) qsound_sharedram1_r(offset);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            result = 0;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            result = 0;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            result = 0;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            result = 0;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            result = 0;
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = (byte) short0;
        } else if (address >= 0x80_4010 && address <= 0x80_4011) {
            if (address == 0x80_4010) {
                result = (byte) (short1 >> 8);
            } else if (address == 0x80_4011) {
                result = (byte) short1;
            }
        } else if (address >= 0x80_4020 && address <= 0x80_4021) {
            if (address == 0x80_4020) {
                result = (byte) (short2 >> 8);
            } else if (address == 0x80_4021) {
                result = (byte) (short2 & (Eeprom.eeprom_bit_r() - 2));
            }
        } else if (address >= 0x80_4030 && address <= 0x80_4031) {
            if (address == 0x80_4030) {
                result = (byte) (cps2_qsound_volume_r() >> 8);
            } else {
                result = (byte) cps2_qsound_volume_r();
            }
        } else if (address >= 0x80_40b0 && address <= 0x80_40b3) {
            result = (byte) kludge_r();
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int offset = (address - 0x80_4140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            result = Memory.mainram[address & 0xffff];
        }
        return result;
    }

    public static short MC2ReadOpWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < mainromop.length) {
                result = (short) (mainromop[address] * 0x100 + mainromop[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MC2ReadPcrelWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < mainromop.length) {
                result = (short) (mainromop[address] * 0x100 + mainromop[address + 1]);
            } else {
                result = 0;
            }
        } else {
            result = MC2ReadWord(address);
        }
        return result;
    }

    public static short MC2ReadWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            result = cps2_output[offset];
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            result = qsound_sharedram1_r(offset);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            result = 0;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            result = 0;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            result = 0;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            result = 0;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            result = 0;
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = short0;
        } else if (address >= 0x80_4010 && address <= 0x80_4011) {
            result = short1;
        } else if (address >= 0x80_4020 && address <= 0x80_4021) {
            result = (short) (short2 & (Eeprom.eeprom_bit_r() - 2));
        } else if (address >= 0x80_4030 && address <= 0x80_4031) {
            result = cps2_qsound_volume_r();
        } else if (address >= 0x80_40b0 && address <= 0x80_40b3) {
            result = kludge_r();
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int offset = (address - 0x80_4140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static int MC2ReadOpLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < mainromop.length) {
                result = mainromop[address] * 0x100_0000 + mainromop[address + 1] * 0x1_0000 + mainromop[address + 2] * 0x100 + mainromop[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MC2ReadPcrelLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < mainromop.length) {
                result = mainromop[address] * 0x100_0000 + mainromop[address + 1] * 0x1_0000 + mainromop[address + 2] * 0x100 + mainromop[address + 3];
            } else {
                result = 0;
            }
        } else {
            result = MC2ReadLong(address);
        }
        return result;
    }

    public static int MC2ReadLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_000b) {
            result = 0;
//            int offset = (add - 0x40_0000) / 2;
//            return (short) CPS1.cps2_output[offset];
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            result = 0;
//            int offset = (add - 0x61_8000) / 2;
//            return CPS1.qsound_sharedram1_r(offset);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            result = 0;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            result = 0;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            result = 0;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            result = 0;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            result = 0;
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            result = 0;
//            int offset = (add - 0x80_0140) / 2;
//            return (short) cps1_cps_b_r(offset);
        } else if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = 0;
            //return (int)sbyte0 & 0xff;
        } else if (address >= 0x80_4010 && address <= 0x80_4011) {
            result = -1;
            //return short1;
        } else if (address >= 0x80_4020 && address <= 0x80_4021) {
            result = 0;
            //return (int)sbyte2 & 0xff;
        } else if (address >= 0x80_4030 && address <= 0x80_4031) {
            result = 0;
            //return CPS1.cps2_qsound_volume_r();
        } else if (address >= 0x80_40b0 && address <= 0x80_40b3) {
            result = kludge_r();
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            result = 0;
            //int offset = (add - 0x80_4140) / 2;
            //return (short)CPS1.cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static void MC2WriteByte(int address, byte value) {
        address &= 0xff_ffff;
        if (address <= 0x3f_ffff) {
            int i11 = 1;
        }
        if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            cps2_output[offset] = value;
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            qsound_sharedram1_w(offset, value);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            int i11 = 1;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            int i11 = 1;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            int i11 = 1;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            int i11 = 1;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            int i11 = 1;
        } else if (address >= 0x70_0000 && address <= 0x70_1fff) {
            int offset = (address - 0x70_0000) / 2;
            cps2_objram1_w(offset, value);
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int i1 = 1;
            //int offset = (add - 0x70_a000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int i1 = 1;
            //int offset = (add - 0x70_c000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int i1 = 1;
            //int offset = (add - 0x70_e000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            int i11 = 1; // cps1_cps_a_w
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int i11 = 1; // cps1_cps_b_w
        } else if (address >= 0x80_4040 && address <= 0x80_4041) {
            if (address == 0x80_4040) {
                cps2_eeprom_port_bh(value);
            } else if (address == 0x80_4041) {
                cps2_eeprom_port_bl(value);
            }
        } else if (address >= 0x80_40a0 && address <= 0x80_40a1) {
            int i11 = 1; // nop
        } else if (address >= 0x80_40e0 && address <= 0x80_40e1) {
            cps2_objram_bank_w(value);
        } else if (address >= 0x80_4100 && address <= 0x80_413f) {
            int i11 = 1; // cps1_cps_a_w
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int i11 = 1; // cps1_cps_b_w
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = value;
        } else {
            int i11 = 1;
        }
    }

    public static void MC2WriteWord(int address, short value) {
        address &= 0xff_ffff;
        if (address <= 0x3f_ffff) {
            int i11 = 1;
        }
        if (address >= 0x40_0000 && address + 1 <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            cps2_output[offset] = value;
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            qsound_sharedram1_w(offset, (byte) value);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            int i11 = 1;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            int i11 = 1;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            int i11 = 1;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            int i11 = 1;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            int i11 = 1;
        } else if (address >= 0x70_0000 && address <= 0x70_1fff) {
            int offset = (address - 0x70_0000) / 2;
            cps2_objram1_w(offset, value);
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_4040 && address <= 0x80_4041) {
            cps2_eeprom_port_w(value);
        } else if (address >= 0x80_40a0 && address <= 0x80_40a1) {
            int i11 = 1; // nop
        } else if (address >= 0x80_40e0 && address <= 0x80_40e1) {
            cps2_objram_bank_w(value);
        } else if (address >= 0x80_4100 && address + 1 <= 0x80_413f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            gfxram[address & 0x3_ffff] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 1] = (byte) value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            Memory.mainram[address & 0xffff] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) value;
        } else {
            int i11 = 1;
        }
    }

    public static void MC2WriteLong(int address, int value) {
        address &= 0xff_ffff;
        if (address <= 0x3f_ffff) {
            int i11 = 1;
        }
        if (address >= 0x40_0000 && address + 3 <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            cps2_output[offset] = (short) (value >> 16);
            cps2_output[offset + 1] = (short) value;
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            qsound_sharedram1_w(offset, (byte) (value >> 16));
            qsound_sharedram1_w(offset + 1, (byte) value);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            int i11 = 1;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            int i11 = 1;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            int i11 = 1;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            int i11 = 1;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            int i11 = 1;
        } else if (address >= 0x70_0000 && address <= 0x70_1fff) {
            int offset = (address - 0x70_0000) / 2;
            cps2_objram1_w(offset, (short) (value >> 16));
            cps2_objram1_w(offset + 1, (short) value);
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x80_0100 && address + 3 <= 0x80_013f) {
            int offset = (address & 0x3f) / 2;
            cps1_cps_a_w(offset, (short) (value >> 16));
            cps1_cps_a_w(offset + 1, (short) value);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int i11 = 1;
            //cps1_cps_b_w((add & 0x3f) / 2, (ushort)value);
        } else if (address >= 0x80_4040 && address <= 0x80_4041) {
            int i11 = 1;
            //cps2_eeprom_port_w(value);
        } else if (address >= 0x80_40a0 && address <= 0x80_40a1) {
            int i11 = 1; // nop
        } else if (address >= 0x80_40e0 && address <= 0x80_40e1) {
            int i11 = 1;
            //cps2_objram_bank_w(value);
        } else if (address >= 0x80_4100 && address <= 0x80_413f) {
            int offset = (address & 0x3f) / 2;
            cps1_cps_a_w(offset, (short) (value >> 16));
            cps1_cps_a_w(offset + 1, (short) value);
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int i11 = 1;
            //cps1_cps_b_w((add & 0x3f) / 2, (ushort)value);
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 24);
            gfxram[(address & 0x3_ffff) + 1] = (byte) (value >> 16);
            gfxram[(address & 0x3_ffff) + 2] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 3] = (byte) (value);
            cps1_gfxram_w((address & 0x3_ffff) / 2);
            cps1_gfxram_w(((address + 2) & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) (value);
        } else {
            int i11 = 1;
        }
    }

    public static byte ZQReadOp(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = audioromop[address & 0x7fff];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZQReadMemory(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address & 0x7fff];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            result = Memory.audiorom[basebanksnd + (address & 0x3fff)];
        } else if (address >= 0xc000 && address <= 0xcfff) {
            result = qsound_sharedram1[address & 0xfff];
        } else if (address == 0xd007) {
            result = QSound.qsound_status_r();
        } else if (address >= 0xf000 && address <= 0xffff) {
            result = qsound_sharedram2[address & 0xfff];
        } else {
            result = 0;
        }
        return result;
    }

    public static void ZQWriteMemory(short address, byte value) {
        if (address >= 0xc000 && address <= 0xcfff) {
            qsound_sharedram1[address & 0xfff] = value;
        } else if (address == 0xd000) {
            QSound.qsound_data_h_w(value);
        } else if (address == 0xd001) {
            QSound.qsound_data_l_w(value);
        } else if (address == 0xd002) {
            QSound.qsound_cmd_w(value);
        } else if (address == 0xd003) {
            qsound_banksw_w(value);
        } else if (address >= 0xf000 && address <= 0xffff) {
            qsound_sharedram2[address & 0xfff] = value;
        } else {

        }
    }

    public static byte ZCReadHardware(short address) {
        return 0;
    }

    public static void ZCWriteHardware(short address, byte value) {

    }

    public static int ZIRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[0].cpunum, 0);
    }

//#endregion

//#region Memory2

    public static byte MCReadByte_forgottn(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x80_0052 && address <= 0x80_0055) {
            int offset = (address - 0x80_0052) / 2;
            result = (byte) ((Inptport.input_port_read_direct(Inptport.analog_p0) - dial0) >> (8 * offset));
        } else if (address >= 0x80_005a && address <= 0x80_005d) {
            int offset = (address - 0x80_005a) / 2;
            result = (byte) ((Inptport.input_port_read_direct(Inptport.analog_p1) - dial1) >> (8 * offset));
        } else {
            result = MCReadByte(address);
        }
        return result;
    }

    public static short MCReadWord_forgottn(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x80_0052 && address <= 0x80_0055) {
            int offset = (address - 0x80_0052) / 2;
            result = (short) (((Inptport.input_port_read_direct(Inptport.analog_p0) - dial0) >> (8 * offset)) & 0xff);
        } else if (address >= 0x80_005a && address <= 0x80_005d) {
            int offset = (address - 0x80_005a) / 2;
            result = (short) (((Inptport.input_port_read_direct(Inptport.analog_p1) - dial1) >> (8 * offset)) & 0xff);
        } else {
            result = MCReadWord(address);
        }
        return result;
    }

    public static int MCReadLong_forgottn(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x80_0052 && address + 3 <= 0x80_0055) {
            result = ((Inptport.input_port_read_direct(Inptport.analog_p0) - dial0) & 0xff) * 0x1_0000 + (((Inptport.input_port_read_direct(Inptport.analog_p0) - dial0) >> 8) & 0xff);
        } else if (address >= 0x80_005a && address + 3 <= 0x80_005d) {
            result = ((Inptport.input_port_read_direct(Inptport.analog_p1) - dial1) & 0xff) * 0x1_0000 + (((Inptport.input_port_read_direct(Inptport.analog_p1) - dial1) >> 8) & 0xff);
        } else {
            result = MCReadLong(address);
        }
        return result;
    }

    public static void MCWriteByte_forgottn(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0040 && address <= 0x80_0041) {
            dial0 = Inptport.input_port_read_direct(Inptport.analog_p0);
        } else if (address >= 0x80_0048 && address <= 0x80_0049) {
            dial1 = Inptport.input_port_read_direct(Inptport.analog_p1);
        } else {
            MCWriteByte(address, value);
        }
    }

    public static void MCWriteWord_forgottn(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0040 && address <= 0x80_0041) {
            dial0 = Inptport.input_port_read_direct(Inptport.analog_p0);
        } else if (address >= 0x80_0048 && address <= 0x80_0049) {
            dial1 = Inptport.input_port_read_direct(Inptport.analog_p1);
        } else {
            MCWriteWord(address, value);
        }
    }

    public static void MCWriteLong_forgottn(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0040 && address <= 0x80_0041) {
            int i1 = 1;
        } else if (address >= 0x80_0048 && address <= 0x80_0049) {
            int i1 = 1;
        } else {
            MCWriteLong(address, value);
        }
    }

    public static byte MCReadByte_sf2thndr(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x80_01c0 && address <= 0x80_01ff) {
            int offset = (address - 0x80_01c0) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else {
            result = MCReadByte(address);
        }
        return result;
    }

    public static short MCReadWord_sf2thndr(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x80_01c0 && address + 1 <= 0x80_01ff) {
            int offset = (address - 0x80_01c0) / 2;
            result = cps1_cps_b_r(offset);
        } else {
            result = MCReadWord(address);
        }
        return result;
    }

    public static void MCWriteWord_sf2thndr(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_01c0 && address + 1 <= 0x80_01ff) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else {
            MCWriteWord(address, value);
        }
    }

    public static short MCReadWord_sf2ceblp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x57_a2b0 && address + 1 <= 0x57_a2b1) {
            if (sf2ceblp_prot == 0x0) {
                result = 0x1992;
            } else if (sf2ceblp_prot == 0x04) {
                result = 0x0408;
            } else {
                result = -1;
            }
        } else {
            result = MCReadWord(address);
        }
        return result;
    }

    public static void MCWriteWord_sf2ceblp(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x57_62b0 && address + 1 <= 0x57_62b1) {
            sf2ceblp_prot = value;
        } else {
            MCWriteWord(address, value);
        }
    }

    public static byte MCReadByte_sf2m3(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0010 && address <= 0x80_0011) {
            if (address == 0x80_0010) {
                result = (byte) (short1 >> 8);
            } else if (address == 0x80_0011) {
                result = (byte) (short1);
            }
        } else if (address >= 0x80_0028 && address <= 0x80_002f) {
            int offset = (address - 0x80_0028) / 2;
            result = cps1_dsw_r(offset);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x80_0186 && address <= 0x80_0187) {
            result = (byte) short2;
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            result = Memory.mainram[address & 0xffff];
        }
        return result;
    }

    public static short MCReadWord_sf2m3(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0010 && address + 1 <= 0x80_0011) {
            result = short1;
        } else if (address >= 0x80_0028 && address + 1 <= 0x80_002f) {
            int offset = (address - 0x80_0028) / 2;
            result = (short) ((cps1_dsw_r(offset) << 8) | cps1_dsw_r(offset));
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x80_0186 && address + 1 <= 0x80_0187) {
            result = (short) ((short2 << 8) | (byte) short2);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static int MCReadLong_sf2m3(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0010 && address + 3 <= 0x80_0011) {
            result = 0;
        } else if (address >= 0x80_0028 && address + 3 <= 0x80_002f) {
            result = 0;
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset) * 0x1_0000 + cps1_cps_b_r(offset + 1);
        } else if (address >= 0x80_0186 && address + 3 <= 0x80_0187) {
            result = 0;
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static void MCWriteByte_sf2m3(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address <= 0x80_0037) {
            if (address % 2 == 0) {
                cps1_coinctrl_w((short) (value * 0x100));
            } else {
            }
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
        } else if (address >= 0x80_0190 && address <= 0x80_0197) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x80_0198 && address <= 0x80_019f) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0x80_01a0 && address <= 0x80_01c3) {
        } else if (address >= 0x80_01c4 && address <= 0x80_01c5) {
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = value;
        } else {
            int i1 = 1;
        }
    }

    public static void MCWriteWord_sf2m3(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 1 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 1 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0190 && address + 1 <= 0x80_0197) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x80_0198 && address + 1 <= 0x80_019f) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0x80_01a0 && address + 1 <= 0x80_01c3) {
            cps1_cps_a_w((address - 0x80_01a0) / 2, value);
        } else if (address >= 0x80_01c4 && address + 1 <= 0x80_01c5) {
            sf2m3_layer_w(value);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 1] = (byte) value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value);
        } else {
            int i1 = 1;
        }
    }

    public static void MCWriteLong_sf2m3(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 3 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 3 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_a_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_b_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0190 && address + 3 <= 0x80_0197) {
        } else if (address >= 0x80_0198 && address + 3 <= 0x80_019f) {
        } else if (address >= 0x80_01a0 && address + 3 <= 0x80_01c3) {
            cps1_cps_a_w((address - 0x80_01a0) / 2, (short) (value >> 16));
            cps1_cps_a_w((address + 2 - 0x80_01a0) / 2, (short) value);
        } else if (address >= 0x80_01c4 && address + 3 <= 0x80_01c5) {
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 24);
            gfxram[(address & 0x3_ffff) + 1] = (byte) (value >> 16);
            gfxram[(address & 0x3_ffff) + 2] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 3] = (byte) (value);
            cps1_gfxram_w((address & 0x3_ffff) / 2);
            cps1_gfxram_w(((address + 2) & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) (value);
        } else {
            int i1 = 1;
        }
    }

    public static byte MCReadByte_sf2m10(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0007) {
            if (address == 0x80_0000) {
                result = (byte) (short1 >> 8);
            } else if (address == 0x80_0001) {
                result = (byte) (short1);
            } else {
                result = -1;
            }
        } else if (address >= 0x80_0018 && address <= 0x80_001f) {
            int offset = (address - 0x80_0018) / 2;
            result = cps1_dsw_r(offset);
        } else if (address >= 0x80_0020 && address <= 0x80_0021) {
            result = 0;
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        } else if (address >= 0xe0_0000 && address <= 0xef_ffff) {
            result = mainram2[address & 0xf_ffff];
        } else if (address >= 0xf1_c000 && address <= 0xf1_c001) {
            result = (byte) short2;
        } else if (address >= 0xfe_ff00 && address <= 0xfe_ffff) {
            result = mainram3[address & 0xff];
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            result = Memory.mainram[address & 0xffff];
        }
        return result;
    }

    public static short MCReadWord_sf2m10(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0007) {
            result = short1;
        } else if (address >= 0x80_0018 && address + 1 <= 0x80_001f) {
            int offset = (address - 0x80_0018) / 2;
            result = (short) (((short) (cps1_dsw_r(offset)) << 8) | (short) cps1_dsw_r(offset));
        } else if (address >= 0x80_0020 && address + 1 <= 0x80_0021) {
            result = 0;
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xe0_0000 && address + 1 <= 0xef_ffff) {
            result = (short) (mainram2[(address & 0xf_ffff)] * 0x100 + mainram2[(address & 0xf_ffff) + 1]);
        } else if (address >= 0xf1_c000 && address + 1 <= 0xf1_c001) {
            result = (short) ((short2 << 8) | (byte) short2);
        } else if (address >= 0xfe_ff00 && address + 1 <= 0xfe_ffff) {
            result = (short) (mainram3[(address & 0xff)] * 0x100 + mainram3[(address & 0xff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            result = (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        }
        return result;
    }

    public static int MCReadLong_sf2m10(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_0007) {
            result = 0;
        } else if (address >= 0x80_0018 && address + 3 <= 0x80_001f) {
            result = 0;
        } else if (address >= 0x80_0020 && address + 3 <= 0x80_0021) {
            result = 0;
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset) * 0x1_0000 + cps1_cps_b_r(offset + 1);
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xe0_0000 && address + 3 <= 0xef_ffff) {
            result = mainram2[(address & 0xf_ffff)] * 0x100_0000 + mainram2[(address & 0xf_ffff) + 1] * 0x1_0000 + mainram2[(address & 0xf_ffff) + 2] * 0x100 + mainram2[(address & 0xf_ffff) + 3];
        } else if (address >= 0xf1_c000 && address + 3 <= 0xf1_c001) {
            result = 0;
        } else if (address >= 0xfe_ff00 && address + 3 <= 0xfe_ffff) {
            result = mainram3[(address & 0xff)] * 0x100_0000 + mainram3[(address & 0xff) + 1] * 0x1_0000 + mainram3[(address & 0xff) + 2] * 0x100 + mainram3[(address & 0xff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        }
        return result;
    }

    public static void MCWriteByte_sf2m10(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address <= 0x80_0037) {
            if (address % 2 == 0) {
                cps1_coinctrl_w((short) (value * 0x100));
            } else {
            }
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            cps1_cps_a_w(address & 0x3f, value);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
        } else if (address >= 0x80_0180 && address <= 0x80_0187) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x80_0188 && address <= 0x80_018f) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0x80_01a2 && address <= 0x80_01b3) {
            cps1_cps_a_w(address - 0x80_01a2, value);
        } else if (address >= 0x80_01fe && address <= 0x80_01ff) {
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xe0_0000 && address <= 0xef_ffff) {
            mainram2[(address & 0xf_ffff)] = value;
        } else if (address >= 0xfe_ff00 && address <= 0xfe_ffff) {
            mainram3[(address & 0xff)] = value;
        } else if (address >= 0xff_0000 && address <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = value;
        } else {
            int i1 = 1;
        }
    }

    public static void MCWriteWord_sf2m10(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 1 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 1 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0140 && address + 1 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0180 && address + 1 <= 0x80_0187) {
            Sound.soundlatch_w(value);
        } else if (address >= 0x80_0188 && address + 1 <= 0x80_018f) {
            Sound.soundlatch2_w(value);
        } else if (address >= 0x80_01a2 && address + 1 <= 0x80_01b3) {
            cps1_cps_a_w((address - 0x80_01a2) / 2, value);
        } else if (address >= 0x80_01fe && address + 1 <= 0x80_01ff) {
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 1] = (byte) value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xe0_0000 && address + 1 <= 0xef_ffff) {
            mainram2[(address & 0xf_ffff)] = (byte) (value >> 8);
            mainram2[(address & 0xf_ffff) + 1] = (byte) (value);
        } else if (address >= 0xfe_ff00 && address + 1 <= 0xfe_ffff) {
            mainram3[(address & 0xff)] = (byte) (value >> 8);
            mainram3[(address & 0xff) + 1] = (byte) (value);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value);
        } else {
            int i1 = 1;
        }
    }

    public static void MCWriteLong_sf2m10(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x80_0030 && address + 3 <= 0x80_0037) {
        } else if (address >= 0x80_0100 && address + 3 <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_a_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0140 && address + 3 <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, (short) (value >> 16));
            cps1_cps_b_w(((address + 2) & 0x3f) / 2, (short) value);
        } else if (address >= 0x80_0180 && address + 3 <= 0x80_0187) {
        } else if (address >= 0x80_0188 && address + 3 <= 0x80_018f) {
        } else if (address >= 0x80_01a2 && address + 3 <= 0x80_01b3) {
            cps1_cps_a_w((address - 0x80_01a2) / 2, (short) (value >> 16));
            cps1_cps_a_w((address + 2 - 0x80_01a2) / 2, (short) value);
        } else if (address >= 0x80_01fe && address + 3 <= 0x80_01ff) {
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 24);
            gfxram[(address & 0x3_ffff) + 1] = (byte) (value >> 16);
            gfxram[(address & 0x3_ffff) + 2] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 3] = (byte) (value);
            cps1_gfxram_w((address & 0x3_ffff) / 2);
            cps1_gfxram_w(((address + 2) & 0x3_ffff) / 2);
        } else if (address >= 0xe0_0000 && address + 3 <= 0xef_ffff) {
            mainram2[(address & 0xf_ffff)] = (byte) (value >> 24);
            mainram2[(address & 0xf_ffff) + 1] = (byte) (value >> 16);
            mainram2[(address & 0xf_ffff) + 2] = (byte) (value >> 8);
            mainram2[(address & 0xf_ffff) + 3] = (byte) (value);
        } else if (address >= 0xfe_ff00 && address + 3 <= 0xfe_ffff) {
            mainram3[(address & 0xff)] = (byte) (value >> 24);
            mainram3[(address & 0xff) + 1] = (byte) (value >> 16);
            mainram3[(address & 0xff) + 2] = (byte) (value >> 8);
            mainram3[(address & 0xff) + 3] = (byte) (value);
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffff) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) (value);
        } else {
            int i1 = 1;
        }
    }

    public static short MCReadWord_sf2dongb(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address == 0x18_0000 || address == 0x1f_7040) {
            result = 0x0210;
        } else {
            result = MCReadWord(address);
        }
        return result;
    }

    public static byte MCReadByte_dinohunt(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0xf1_8000 && address <= 0xf1_9fff) {
            result = -1;
        } else if (address >= 0xfc_0000 && address <= 0xfc_0001) {
            result = (byte) short2;
        } else {
            result = MCReadByte(address);
        }
        return result;
    }

    public static byte MCReadByte_pang3(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x80_017a && address <= 0x80_017b) {
            result = (byte) cps1_eeprom_port_r();
        } else {
            result = MCReadByte(address);
        }
        return result;
    }

    public static short MCReadWord_pang3(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x80_017a && address + 1 <= 0x80_017b) {
            result = (short) cps1_eeprom_port_r();
        } else {
            result = MCReadWord(address);
        }
        return result;
    }

    public static void MCWriteByte_pang3(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x80_017a && address <= 0x80_017b) {
            if ((address & 1) == 1) {
                cps1_eeprom_port_w(value);
            }
        } else {
            MCWriteByte(address, value);
        }
    }

    public static void MCWriteWord_pang3(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x80_017a && address + 1 <= 0x80_017b) {
            cps1_eeprom_port_w(value);
        } else {
            MCWriteWord(address, value);
        }
    }

    public static byte MC2ReadByte_ecofghtr(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = 0;
        } else {
            result = MC2ReadByte(address);
        }
        return result;
    }

    public static short MC2ReadWord_ecofghtr(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x80_4000 && address + 1 <= 0x80_4001) {
            result = 0;
        } else {
            result = MC2ReadWord(address);
        }
        return result;
    }

    public static byte MC2ReadOpByte_dead(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MC2ReadByte_dead(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x3f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            result = (byte) cps2_output[offset];
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            result = (byte) qsound_sharedram1_r(offset);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            result = 0;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            result = 0;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            result = 0;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            result = 0;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            result = 0;
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            result = (byte) cps2_objram2_r(offset);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = (byte) short0;
        } else if (address >= 0x80_4010 && address <= 0x80_4011) {
            if (address == 0x80_4010) {
                result = (byte) (short1 >> 8);
            } else if (address == 0x80_4011) {
                result = (byte) short1;
            }
        } else if (address >= 0x80_4020 && address <= 0x80_4021) {
            if (address == 0x80_4020) {
                result = (byte) (short2 >> 8);
            } else if (address == 0x80_4021) {
                result = (byte) (short2 & (Eeprom.eeprom_bit_r() - 2));
            }
        } else if (address >= 0x80_4030 && address <= 0x80_4031) {
            if (address == 0x80_4030) {
                result = (byte) (cps2_qsound_volume_r() >> 8);
            } else {
                result = (byte) cps2_qsound_volume_r();
            }
        } else if (address >= 0x80_40b0 && address <= 0x80_40b3) {
            result = (byte) kludge_r();
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int offset = (address - 0x80_4140) / 2;
            result = (byte) cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)];
        } else if (address >= 0xff_0000 && address <= 0xff_ffef) {
            result = Memory.mainram[address & 0xffff];
        } else if (address >= 0xff_fff0 && address <= 0xff_fffb) {
            int offset = (address - 0xff_fff0) / 2;
            result = (byte) cps2_output[offset];
        } else if (address >= 0xff_fffc && address <= 0xff_ffff) {
            result = 0;
        }
        return result;
    }

    public static short MC2ReadOpWord_dead(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MC2ReadWord_dead(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            result = cps2_output[offset];
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            result = qsound_sharedram1_r(offset);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            result = 0;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            result = 0;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            result = 0;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            result = 0;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            result = 0;
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            result = cps2_objram2_r(offset);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int offset = (address - 0x80_0140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = short0;
        } else if (address >= 0x80_4010 && address <= 0x80_4011) {
            result = short1;
        } else if (address >= 0x80_4020 && address <= 0x80_4021) {
            result = (short) (short2 & (Eeprom.eeprom_bit_r() - 2));
        } else if (address >= 0x80_4030 && address <= 0x80_4031) {
            result = cps2_qsound_volume_r();
        } else if (address >= 0x80_40b0 && address <= 0x80_40b3) {
            result = kludge_r();
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int offset = (address - 0x80_4140) / 2;
            result = cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            result = (short) (gfxram[(address & 0x3_ffff)] * 0x100 + gfxram[(address & 0x3_ffff) + 1]);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffef) {
            return (short) (Memory.mainram[(address & 0xffff)] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        } else if (address >= 0xff_fff0 && address + 1 <= 0xff_fffb) {
            int offset = (address - 0xff_fff0) / 2;
            result = cps2_output[offset];
        } else if (address >= 0xff_fffc && address + 1 <= 0xff_ffff) {
            result = 0;
        }
        return result;
    }

    public static int MC2ReadOpLong_dead(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MC2ReadLong_dead(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x3f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_000b) {
            result = 0;
            //int offset = (add - 0x40_0000) / 2;
            //return (short)CPS1.cps2_output[offset];
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            result = 0;
            //int offset = (add - 0x61_8000) / 2;
            //return CPS1.qsound_sharedram1_r(offset);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            result = 0;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            result = 0;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            result = 0;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            result = 0;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            result = 0;
        } else if (address >= 0x70_8000 && address + 3 <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            result = cps2_objram2_r(offset) * 0x1_0000 + cps2_objram2_r(offset + 1);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            result = 0;
            //int offset = (add - 0x80_0140) / 2;
            //return (short)cps1_cps_b_r(offset);
        } else if (address >= 0x80_4000 && address <= 0x80_4001) {
            result = 0;
            //return (int)sbyte0 & 0xff;
        } else if (address >= 0x80_4010 && address <= 0x80_4011) {
            result = -1;
            //return short1;
        } else if (address >= 0x80_4020 && address <= 0x80_4021) {
            result = 0;
            //return (int)sbyte2 & 0xff;
        } else if (address >= 0x80_4030 && address <= 0x80_4031) {
            result = 0;
            //return CPS1.cps2_qsound_volume_r();
        } else if (address >= 0x80_40b0 && address <= 0x80_40b3) {
            result = 0;
            //return CPS1.kludge_r();
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            result = 0;
            //int offset = (add - 0x80_4140) / 2;
            //return (short)CPS1.cps1_cps_b_r(offset);
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            result = gfxram[(address & 0x3_ffff)] * 0x100_0000 + gfxram[(address & 0x3_ffff) + 1] * 0x1_0000 + gfxram[(address & 0x3_ffff) + 2] * 0x100 + gfxram[(address & 0x3_ffff) + 3];
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffef) {
            result = Memory.mainram[(address & 0xffff)] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        } else if (address >= 0xff_fff0 && address + 3 <= 0xff_fffb) {
            result = 0;
            //int offset = (address - 0xff_fff0) / 2;
            //return (sbyte)cps2_output[offset];
        } else if (address >= 0xff_fffc && address + 3 <= 0xff_ffff) {
            result = 0;
        }
        return result;
    }

    public static void MC2WriteByte_dead(int address, byte value) {
        address &= 0xff_ffff;
        if (address <= 0x3f_ffff) {
            int i11 = 1;
        }
        if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            cps2_output[offset] = value;
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            qsound_sharedram1_w(offset, value);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            int i11 = 1;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            int i11 = 1;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            int i11 = 1;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            int i11 = 1;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            int i11 = 1;
        } else if (address >= 0x70_0000 && address <= 0x70_1fff) {
            int offset = (address - 0x70_0000) / 2;
            cps2_objram1_w(offset, value);
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int i1 = 1;
            //int offset = (add - 0x70_a000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int i1 = 1;
            //int offset = (add - 0x70_c000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int i1 = 1;
            //int offset = (add - 0x70_e000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            int i11 = 1; // cps1_cps_a_w
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int i11 = 1; // cps1_cps_b_w
        } else if (address >= 0x80_4040 && address <= 0x80_4041) {
            if (address == 0x80_4040) {
                cps2_eeprom_port_bh(value);
            } else if (address == 0x80_4041) {
                cps2_eeprom_port_bl(value);
            }
        } else if (address >= 0x80_40a0 && address <= 0x80_40a1) {
            int i11 = 1; // nop
        } else if (address >= 0x80_40e0 && address <= 0x80_40e1) {
            cps2_objram_bank_w(value);
        } else if (address >= 0x80_4100 && address <= 0x80_413f) {
            int i11 = 1; // cps1_cps_a_w
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int i11 = 1; // cps1_cps_b_w
        } else if (address >= 0x90_0000 && address <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address <= 0xff_ffef) {
            Memory.mainram[(address & 0xffff)] = value;
        } else if (address >= 0xff_fff0 && address <= 0xff_fffb) {
            int offset = (address - 0xff_fff0) / 2;
            cps2_output[offset] = value;
        } else if (address >= 0xff_fffc && address <= 0xff_ffff) {
            int i11 = 1;
        } else {
            int i11 = 1;
        }
    }

    public static void MC2WriteWord_dead(int address, short value) {
        address &= 0xff_ffff;
        if (address <= 0x3f_ffff) {
            int i11 = 1;
        }
        if (address >= 0x40_0000 && address <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            cps2_output[offset] = value;
        } else if (address >= 0x61_8000 && address <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            qsound_sharedram1_w(offset, (byte) value);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            int i11 = 1;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            int i11 = 1;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            int i11 = 1;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            int i11 = 1;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            int i11 = 1;
        } else if (address >= 0x70_0000 && address <= 0x70_1fff) {
            int offset = (address - 0x70_0000) / 2;
            cps2_objram1_w(offset, value);
        } else if (address >= 0x70_8000 && address <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_a000 && address <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            cps2_objram2_w(offset, value);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int i11 = 1;
            //int offset = (add - 0x70_c000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int i11 = 1;
            //int offset = (add - 0x70_e000) / 2;
            //cps2_objram2_w(offset, (ushort)value);
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_4040 && address <= 0x80_4041) {
            cps2_eeprom_port_w(value);
        } else if (address >= 0x80_40a0 && address <= 0x80_40a1) {
            int i11 = 1; // nop
        } else if (address >= 0x80_40e0 && address <= 0x80_40e1) {
            cps2_objram_bank_w(value);
        } else if (address >= 0x80_4100 && address <= 0x80_413f) {
            cps1_cps_a_w((address & 0x3f) / 2, value);
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            cps1_cps_b_w((address & 0x3f) / 2, value);
        } else if (address >= 0x90_0000 && address + 1 <= 0x92_ffff) {
            gfxram[address & 0x3_ffff] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 1] = (byte) value;
            cps1_gfxram_w((address & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 1 <= 0xff_ffef) {
            Memory.mainram[address & 0xffff] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) value;
        } else if (address >= 0xff_fff0 && address + 1 <= 0xff_fffb) {
            int offset = (address - 0xff_fff0) / 2;
            cps2_output[offset] = value;
        } else if (address >= 0xff_fffc && address + 1 <= 0xff_ffff) {
            int i11 = 1;
        } else {
            int i11 = 1;
        }
    }

    public static void MC2WriteLong_dead(int address, int value) {
        address &= 0xff_ffff;
        if (address <= 0x3f_ffff) {
            int i11 = 1;
        }
        if (address >= 0x40_0000 && address + 3 <= 0x40_000b) {
            int offset = (address - 0x40_0000) / 2;
            cps2_output[offset] = (short) (value >> 16);
            cps2_output[offset + 1] = (short) value;
        } else if (address >= 0x61_8000 && address + 3 <= 0x61_9fff) {
            int offset = (address - 0x61_8000) / 2;
            qsound_sharedram1_w(offset, (byte) (value >> 16));
            qsound_sharedram1_w(offset + 1, (byte) value);
        } else if (address >= 0x66_2000 && address <= 0x66_2001) {
            int i11 = 1;
        } else if (address >= 0x66_2008 && address <= 0x66_2009) {
            int i11 = 1;
        } else if (address >= 0x66_2020 && address <= 0x66_2021) {
            int i11 = 1;
        } else if (address >= 0x66_0000 && address <= 0x66_3fff) {
            int i11 = 1;
        } else if (address >= 0x66_4000 && address <= 0x66_4001) {
            int i11 = 1;
        } else if (address >= 0x70_0000 && address <= 0x70_1fff) {
            int offset = (address - 0x70_0000) / 2;
            cps2_objram1_w(offset, (short) (value >> 16));
            cps2_objram1_w(offset + 1, (short) value);
        } else if (address >= 0x70_8000 && address + 3 <= 0x70_9fff) {
            int offset = (address - 0x70_8000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x70_a000 && address + 3 <= 0x70_bfff) {
            int offset = (address - 0x70_a000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x70_c000 && address <= 0x70_dfff) {
            int offset = (address - 0x70_c000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x70_e000 && address <= 0x70_ffff) {
            int offset = (address - 0x70_e000) / 2;
            cps2_objram2_w(offset, (short) (value >> 16));
            cps2_objram2_w(offset + 1, (short) value);
        } else if (address >= 0x80_0100 && address <= 0x80_013f) {
            int offset = (address & 0x3f) / 2;
            cps1_cps_a_w(offset, (short) (value >> 16));
            cps1_cps_a_w(offset + 1, (short) value);
        } else if (address >= 0x80_0140 && address <= 0x80_017f) {
            int i11 = 1;
            //cps1_cps_b_w((add & 0x3f) / 2, (ushort)value);
        } else if (address >= 0x80_4040 && address <= 0x80_4041) {
            int i11 = 1;
            //cps2_eeprom_port_w(value);
        } else if (address >= 0x80_40a0 && address <= 0x80_40a1) {
            int i11 = 1; // nop
        } else if (address >= 0x80_40e0 && address <= 0x80_40e1) {
            int i11 = 1;
            //cps2_objram_bank_w(value);
        } else if (address >= 0x80_4100 && address <= 0x80_413f) {
            int offset = (address & 0x3f) / 2;
            cps1_cps_a_w(offset, (short) (value >> 16));
            cps1_cps_a_w(offset + 1, (short) value);
        } else if (address >= 0x80_4140 && address <= 0x80_417f) {
            int i11 = 1;
            //cps1_cps_b_w((add & 0x3f) / 2, (ushort)value);
        } else if (address >= 0x90_0000 && address + 3 <= 0x92_ffff) {
            gfxram[(address & 0x3_ffff)] = (byte) (value >> 24);
            gfxram[(address & 0x3_ffff) + 1] = (byte) (value >> 16);
            gfxram[(address & 0x3_ffff) + 2] = (byte) (value >> 8);
            gfxram[(address & 0x3_ffff) + 3] = (byte) (value);
            cps1_gfxram_w((address & 0x3_ffff) / 2);
            cps1_gfxram_w(((address + 2) & 0x3_ffff) / 2);
        } else if (address >= 0xff_0000 && address + 3 <= 0xff_ffef) {
            Memory.mainram[(address & 0xffff)] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) (value);
        } else if (address >= 0xff_fff0 && address + 3 <= 0xff_fffb) {
            int offset = (address - 0xff_fff0) / 2;
            cps2_output[offset] = (short) (value >> 16);
            cps2_output[offset + 1] = (short) value;
        } else if (address >= 0xff_fffc && address + 3 <= 0xff_ffff) {
            int i11 = 1;
        } else {
            int i11 = 1;
        }
    }

//#endregion

//#region State

    public static void SaveStateBinaryC(BinaryWriter writer) {
        int i;
        writer.write(dswa);
        writer.write(dswb);
        writer.write(dswc);
        writer.write(basebanksnd);
        for (i = 0; i < 0x20; i++) {
            writer.write(cps_a_regs[i]);
        }
        for (i = 0; i < 0x20; i++) {
            writer.write(cps_b_regs[i]);
        }
        for (i = 0; i < 0xc00; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1_0000);
        writer.write(gfxram, 0, 0x3_0000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        writer.write(Video.screenstate.frame_number);
        writer.write(Sound.last_update_second);
        for (i = 0; i < 2; i++) {
            writer.write(Cpuexec.cpu[i].suspend);
            writer.write(Cpuexec.cpu[i].nextsuspend);
            writer.write(Cpuexec.cpu[i].eatcycles);
            writer.write(Cpuexec.cpu[i].nexteatcycles);
            writer.write(Cpuexec.cpu[i].localtime.seconds);
            writer.write(Cpuexec.cpu[i].localtime.attoseconds);
        }
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        OKI6295.SaveStateBinary(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.okistream.output_sampindex);
        writer.write(Sound.okistream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        switch (RomInfo.rom.name) {
            case "forgottn":
            case "forgottna":
            case "forgottnu":
            case "forgottnue":
            case "forgottnuc":
            case "forgottnua":
            case "forgottnuaa":
            case "lostwrld":
            case "lostwrldo":
                writer.write(Inptport.portdata.last_delta_nsec);
                break;
        }
    }

    public static void SaveStateBinaryQ(BinaryWriter writer) {
        int i;
        writer.write(dswa);
        writer.write(dswb);
        writer.write(dswc);
        writer.write(basebanksnd);
        for (i = 0; i < 0x20; i++) {
            writer.write(cps_a_regs[i]);
        }
        for (i = 0; i < 0x20; i++) {
            writer.write(cps_b_regs[i]);
        }
        for (i = 0; i < 0xc00; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1_0000);
        writer.write(gfxram, 0, 0x3_0000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        writer.write(Video.screenstate.frame_number);
        writer.write(Sound.last_update_second);
        for (i = 0; i < 2; i++) {
            writer.write(Cpuexec.cpu[i].suspend);
            writer.write(Cpuexec.cpu[i].nextsuspend);
            writer.write(Cpuexec.cpu[i].eatcycles);
            writer.write(Cpuexec.cpu[i].nexteatcycles);
            writer.write(Cpuexec.cpu[i].localtime.seconds);
            writer.write(Cpuexec.cpu[i].localtime.attoseconds);
        }
        Timer.saveStateBinary(writer);
        writer.write(qsound_sharedram1);
        writer.write(qsound_sharedram2);
        QSound.SaveStateBinary(writer);
        writer.write(Sound.qsoundstream.output_sampindex);
        writer.write(Sound.qsoundstream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void SaveStateBinaryC2(BinaryWriter writer) {
        int i;
        writer.write(basebanksnd);
        for (i = 0; i < 0x20; i++) {
            writer.write(cps_a_regs[i]);
        }
        for (i = 0; i < 0x20; i++) {
            writer.write(cps_b_regs[i]);
        }
        for (i = 0; i < 0x1000; i++) {
            writer.write(cps2_objram1[i]);
        }
        for (i = 0; i < 0x1000; i++) {
            writer.write(cps2_objram2[i]);
        }
        for (i = 0; i < 6; i++) {
            writer.write(cps2_output[i]);
        }
        writer.write(cps2networkpresent);
        writer.write(cps2_objram_bank);
        writer.write(scancount);
        writer.write(cps1_scanline1);
        writer.write(cps1_scanline2);
        writer.write(cps1_scancalls);
        for (i = 0; i < 0xc00; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1_0000);
        writer.write(gfxram, 0, 0x3_0000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        writer.write(Video.screenstate.frame_number);
        writer.write(Sound.last_update_second);
        for (i = 0; i < 2; i++) {
            writer.write(Cpuexec.cpu[i].suspend);
            writer.write(Cpuexec.cpu[i].nextsuspend);
            writer.write(Cpuexec.cpu[i].eatcycles);
            writer.write(Cpuexec.cpu[i].nexteatcycles);
            writer.write(Cpuexec.cpu[i].localtime.seconds);
            writer.write(Cpuexec.cpu[i].localtime.attoseconds);
        }
        Timer.saveStateBinary(writer);
        writer.write(qsound_sharedram1);
        writer.write(qsound_sharedram2);
        QSound.SaveStateBinary(writer);
        writer.write(Sound.qsoundstream.output_sampindex);
        writer.write(Sound.qsoundstream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void LoadStateBinaryC(BinaryReader reader) {
        try {
            int i;
            dswa = reader.readByte();
            dswb = reader.readByte();
            dswc = reader.readByte();
            basebanksnd = reader.readInt32();
            for (i = 0; i < 0x20; i++) {
                cps_a_regs[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x20; i++) {
                cps_b_regs[i] = reader.readUInt16();
            }
            for (i = 0; i < 0xc00; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1_0000);
            gfxram = reader.readBytes(0x3_0000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.screenstate.frame_number = reader.readInt64();
            Sound.last_update_second = reader.readInt32();
            for (i = 0; i < 2; i++) {
                Cpuexec.cpu[i].suspend = reader.readByte();
                Cpuexec.cpu[i].nextsuspend = reader.readByte();
                Cpuexec.cpu[i].eatcycles = reader.readByte();
                Cpuexec.cpu[i].nexteatcycles = reader.readByte();
                Cpuexec.cpu[i].localtime.seconds = reader.readInt32();
                Cpuexec.cpu[i].localtime.attoseconds = reader.readInt64();
            }
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            OKI6295.LoadStateBinary(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.okistream.output_sampindex = reader.readInt32();
            Sound.okistream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            switch (RomInfo.rom.name) {
                case "forgottn":
                case "forgottna":
                case "forgottnu":
                case "forgottnue":
                case "forgottnuc":
                case "forgottnua":
                case "forgottnuaa":
                case "lostwrld":
                case "lostwrldo":
                    Inptport.portdata.last_delta_nsec = reader.readInt64();
                    break;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void LoadStateBinaryQ(BinaryReader reader) {
        try {
            int i;
            dswa = reader.readByte();
            dswb = reader.readByte();
            dswc = reader.readByte();
            basebanksnd = reader.readInt32();
            for (i = 0; i < 0x20; i++) {
                cps_a_regs[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x20; i++) {
                cps_b_regs[i] = reader.readUInt16();
            }
            for (i = 0; i < 0xc00; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1_0000);
            gfxram = reader.readBytes(0x3_0000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.screenstate.frame_number = reader.readInt64();
            Sound.last_update_second = reader.readInt32();
            for (i = 0; i < 2; i++) {
                Cpuexec.cpu[i].suspend = reader.readByte();
                Cpuexec.cpu[i].nextsuspend = reader.readByte();
                Cpuexec.cpu[i].eatcycles = reader.readByte();
                Cpuexec.cpu[i].nexteatcycles = reader.readByte();
                Cpuexec.cpu[i].localtime.seconds = reader.readInt32();
                Cpuexec.cpu[i].localtime.attoseconds = reader.readInt64();
            }
            Timer.loadStateBinary(reader);
            qsound_sharedram1 = reader.readBytes(0x1000);
            qsound_sharedram2 = reader.readBytes(0x1000);
            QSound.LoadStateBinary(reader);
            Sound.qsoundstream.output_sampindex = reader.readInt32();
            Sound.qsoundstream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void LoadStateBinaryC2(BinaryReader reader) {
        try {
            int i;
            basebanksnd = reader.readInt32();
            for (i = 0; i < 0x20; i++) {
                cps_a_regs[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x20; i++) {
                cps_b_regs[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x1000; i++) {
                cps2_objram1[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x1000; i++) {
                cps2_objram2[i] = reader.readUInt16();
            }
            for (i = 0; i < 6; i++) {
                cps2_output[i] = reader.readUInt16();
            }
            cps2networkpresent = reader.readInt32();
            cps2_objram_bank = reader.readInt32();
            scancount = reader.readInt32();
            cps1_scanline1 = reader.readInt32();
            cps1_scanline2 = reader.readInt32();
            cps1_scancalls = reader.readInt32();
            for (i = 0; i < 0xc00; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1_0000);
            gfxram = reader.readBytes(0x3_0000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.screenstate.frame_number = reader.readInt64();
            Sound.last_update_second = reader.readInt32();
            for (i = 0; i < 2; i++) {
                Cpuexec.cpu[i].suspend = reader.readByte();
                Cpuexec.cpu[i].nextsuspend = reader.readByte();
                Cpuexec.cpu[i].eatcycles = reader.readByte();
                Cpuexec.cpu[i].nexteatcycles = reader.readByte();
                Cpuexec.cpu[i].localtime.seconds = reader.readInt32();
                Cpuexec.cpu[i].localtime.attoseconds = reader.readInt64();
            }
            Timer.loadStateBinary(reader);
            qsound_sharedram1 = reader.readBytes(0x1000);
            qsound_sharedram2 = reader.readBytes(0x1000);
            QSound.LoadStateBinary(reader);
            Sound.qsoundstream.output_sampindex = reader.readInt32();
            Sound.qsoundstream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion
}
