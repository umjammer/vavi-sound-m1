/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.taito;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m6800.M6800;
import m1.cpu.m68000.MC68000;
import m1.cpu.m6805.M6805;
import m1.cpu.z80.Z80A;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
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
import m1.emu.Mouse;
import m1.emu.Palette;
import m1.emu.RomInfo;
import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.emu.Watchdog;
import m1.sound.AY8910;
import m1.sound.MSM5205;
import m1.sound.Sound;
import m1.sound.Taitosnd;
import m1.sound.YM2151;
import m1.sound.YM2203;
import m1.sound.YM3812;


public class Taito {

    public static int basebankmain, basebanksnd;
    public static byte[] bb1, bublbobl_mcu_sharedram, videoram, bublbobl_objectram, slaverom, mcurom, mcuram, mainram2, mainram3, subrom;

    public static void TaitoInit() {
        int i, n;
        Machine.bRom = true;
        switch (Machine.sName) {
            case "tokio":
            case "tokioo":
            case "tokiou":
            case "tokiob":
                videoram = new byte[0x1d00];
                bublbobl_objectram = new byte[0x300];
                Memory.mainram = new byte[0x1800];
                Memory.audioram = new byte[0x1000];
                Generic.paletteram = new byte[0x200];
                Memory.mainrom = new byte[0x1_0000]; //Machine.GetRom("maincpu.rom");
                slaverom = new byte[0x1_0000]; //Machine.GetRom("slave.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx12rom = Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                prom = new byte[0x100]; //Machine.GetRom("proms.rom");
                bublbobl_video_enable = 1;
                if (Memory.mainrom == null || slaverom == null || Memory.audiorom == null || gfx1rom == null || prom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dsw0 = (byte) 0xfe;
                    dsw1 = 0x7e;
                }
                break;
            case "bublbobl":
            case "bublbobl1":
            case "bublboblr":
            case "bublboblr1":
            case "bub68705":
            case "bublcave":
            case "bublcave11":
            case "bublcave10":
                videoram = new byte[0x1d00];
                bublbobl_objectram = new byte[0x300];
                Memory.mainram = new byte[0x1800];
                Memory.audioram = new byte[0x1000];
                mcuram = new byte[0xc0];
                Generic.paletteram = new byte[0x200];
                bublbobl_mcu_sharedram = new byte[0x400];
                Memory.mainrom = new byte[0x1_0000]; //Machine.GetRom("maincpu.rom");
                slaverom = new byte[0x1_0000]; //Machine.GetRom("slave.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                mcurom = new byte[0x1_0000]; //Machine.GetRom("mcu.rom");
                gfx12rom = new byte[0x1_0000]; //Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                prom = new byte[0x100]; //Machine.GetRom("proms.rom");
                bublbobl_video_enable = 0;
                if (Memory.mainrom == null || slaverom == null || Memory.audiorom == null || mcurom == null || gfx1rom == null || prom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dsw0 = (byte) 0xfe;
                    dsw1 = (byte) 0xff;
                }
                break;
            case "boblbobl":
            case "sboblbobl":
            case "sboblbobla":
            case "sboblboblb":
            case "sboblbobld":
            case "sboblboblc":
            case "dland":
            case "bbredux":
                mainram2 = new byte[0x100];
                mainram3 = new byte[0x100];
                videoram = new byte[0x1d00];
                bublbobl_objectram = new byte[0x300];
                Memory.mainram = new byte[0x1800];
                Memory.audioram = new byte[0x1000];
                Generic.paletteram = new byte[0x200];
                Memory.mainrom = new byte[0x1_0000]; //Machine.GetRom("maincpu.rom");
                slaverom = new byte[0x1_0000]; //Machine.GetRom("slave.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx12rom = new byte[0x1_0000]; //Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                prom = Machine.GetRom("proms.rom");
                bublbobl_video_enable = 0;
                if (Memory.mainrom == null || slaverom == null || Memory.audiorom == null || gfx1rom == null || prom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dsw0 = (byte) 0xfe;
                    dsw1 = 0x3f;
                }
                break;
            case "bublboblb":
            case "boblcave":
                mainram2 = new byte[0x100];
                mainram3 = new byte[0x100];
                videoram = new byte[0x1d00];
                bublbobl_objectram = new byte[0x300];
                Memory.mainram = new byte[0x1800];
                Memory.audioram = new byte[0x1000];
                Generic.paletteram = new byte[0x200];
                Memory.mainrom = new byte[0x1_0000]; //Machine.GetRom("maincpu.rom");
                slaverom = new byte[0x1_0000]; //Machine.GetRom("slave.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx12rom = Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                prom = Machine.GetRom("proms.rom");
                bublbobl_video_enable = 0;
                if (Memory.mainrom == null || slaverom == null || Memory.audiorom == null || gfx1rom == null || prom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dsw0 = (byte) 0xfe;
                    dsw1 = (byte) 0xc0;
                }
                break;
            case "opwolf":
            case "opwolfa":
            case "opwolfj":
            case "opwolfu":
                mainram2 = new byte[0x1_0000];
                cchip_ram = new byte[0x2000];
                Generic.paletteram16 = new short[0x800];
                Memory.mainram = new byte[0x8000];
                Memory.audioram = new byte[0x1000];
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                bb1 = Machine.GetRom("audiocpu.rom");
                Memory.audiorom = new byte[0x2_0000];
                System.arraycopy(bb1, 0, Memory.audiorom, 0, 0x1_0000);
                gfx12rom = Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                gfx22rom = Machine.GetRom("gfx2.rom");
                n = gfx22rom.length;
                gfx2rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx2rom[i * 2] = (byte) (gfx22rom[i] >> 4);
                    gfx2rom[i * 2 + 1] = (byte) (gfx22rom[i] & 0x0f);
                }
                adpcmrom = Machine.GetRom("adpcm.rom");
                Taitosnd.taitosnd_start();
                if (Memory.mainrom == null || Memory.audiorom == null || gfx1rom == null || gfx2rom == null || adpcmrom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dswa = (byte) 0xff;
                    dswb = 0x7f;
                }
                break;
            case "opwolfb":
                mainram2 = new byte[0x1_0000];
                cchip_ram = new byte[0x2000];
                Generic.paletteram16 = new short[0x800];
                Memory.mainram = new byte[0x8000];
                Memory.audioram = new byte[0x1000];
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                bb1 = Machine.GetRom("audiocpu.rom");
                Memory.audiorom = new byte[0x2_0000];
                System.arraycopy(bb1, 0, Memory.audiorom, 0, 0x1_0000);
                subrom = Machine.GetRom("sub.rom");
                gfx12rom = Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                gfx22rom = Machine.GetRom("gfx2.rom");
                n = gfx22rom.length;
                gfx2rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx2rom[i * 2] = (byte) (gfx22rom[i] >> 4);
                    gfx2rom[i * 2 + 1] = (byte) (gfx22rom[i] & 0x0f);
                }
                adpcmrom = Machine.GetRom("adpcm.rom");
                Taitosnd.taitosnd_start();
                if (Memory.mainrom == null || Memory.audiorom == null || subrom == null || gfx1rom == null || gfx2rom == null || adpcmrom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                }
                break;
            case "opwolfp":
                mainram2 = new byte[0x1_0000];
                cchip_ram = new byte[0x2000];
                Generic.paletteram16 = new short[0x800];
                Memory.mainram = new byte[0x8000];
                Memory.audioram = new byte[0x1000];
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                bb1 = Machine.GetRom("audiocpu.rom");
                Memory.audiorom = new byte[0x2_0000];
                System.arraycopy(bb1, 0, Memory.audiorom, 0, 0x1_0000);
                gfx12rom = Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                gfx22rom = Machine.GetRom("gfx2.rom");
                n = gfx22rom.length;
                gfx2rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx2rom[i * 2] = (byte) (gfx22rom[i] >> 4);
                    gfx2rom[i * 2 + 1] = (byte) (gfx22rom[i] & 0x0f);
                }
                adpcmrom = Machine.GetRom("adpcm.rom");
                Taitosnd.taitosnd_start();
                if (Memory.mainrom == null || Memory.audiorom == null || gfx1rom == null || gfx2rom == null || adpcmrom == null) {
                    Machine.bRom = false;
                }
                if (Machine.bRom) {
                    dswa = (byte) 0xff;
                    dswb = (byte) 0xff;
                }
                break;
        }
    }

    public static void machine_reset_null() {

    }

    public static void irqhandler(int irq) {
        Cpuint.cpunum_set_input_line(2, 0, irq != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void driver_init_opwolf() {
        opwolf_region = Memory.mainrom[0x03_ffff];
        opwolf_cchip_init();
        opwolf_gun_xoffs = 0xec - Memory.mainrom[0x03_ffb1];
        opwolf_gun_yoffs = 0x1c - Memory.mainrom[0x03_ffaf];
        basebanksnd = 0x1_0000;
    }

    public static void driver_init_opwolfb() {
        opwolf_region = Memory.mainrom[0x03_ffff];
        opwolf_gun_xoffs = -2;
        opwolf_gun_yoffs = 17;
        basebanksnd = 0x1_0000;
    }

    public static void driver_init_opwolfp() {
        opwolf_region = Memory.mainrom[0x03_ffff];
        opwolf_gun_xoffs = 5;
        opwolf_gun_yoffs = 30;
        basebanksnd = 0x1_0000;
    }

    public static void play_taito_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_taito_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_taito_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 5, 0, (byte) ti.TrackID));
    }

//#region Memory

    public static byte sbyte0, sbyte1, sbyte2, sbyte3, sbyte4, sbyte5;
    public static byte sbyte0_old, sbyte1_old, sbyte2_old, sbyte3_old, sbyte4_old, sbyte5_old;
    public static int p1x_accum_old, p1x_previous_old, p1y_accum_old, p1y_previous_old;

    public static byte Z0ReadMemory_tokio(short address) {
        byte result;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = (address & 0xffff) - 0xc000;
            result = videoram[offset];
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xdd00;
            result = bublbobl_objectram[offset];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = Memory.mainram[offset];
        } else if ((address & 0xffff) == 0xfa03) {
            result = dsw0;
        } else if ((address & 0xffff) == 0xfa04) {
            result = dsw1;
        } else if ((address & 0xffff) == 0xfa05) {
            result = sbyte0;
        } else if ((address & 0xffff) == 0xfa06) {
            result = sbyte1;
        } else if ((address & 0xffff) == 0xfa07) {
            result = sbyte2;
        } else if ((address & 0xffff) == 0xfc00) {
            result = 0;
        } else if ((address & 0xffff) == 0xfe00) {
            result = tokio_mcu_r();
        } else {
            result = 0;
        }
        return result;
    }

    public static byte Z0ReadMemory_tokiob(short address) {
        byte result;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = (address & 0xffff) - 0xc000;
            result = videoram[offset];
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xdd00;
            result = bublbobl_objectram[offset];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = Memory.mainram[offset];
        } else if ((address & 0xffff) == 0xfa03) {
            result = dsw0;
        } else if ((address & 0xffff) == 0xfa04) {
            result = dsw1;
        } else if ((address & 0xffff) == 0xfa05) {
            result = sbyte0;
        } else if ((address & 0xffff) == 0xfa06) {
            result = sbyte1;
        } else if ((address & 0xffff) == 0xfa07) {
            result = sbyte2;
        } else if ((address & 0xffff) == 0xfc00) {
            result = 0;
        } else if ((address & 0xffff) == 0xfe00) {
            result = tokiob_mcu_r();
        } else {
            result = 0;
        }
        return result;
    }

    public static byte Z0ReadMemory_bootleg(short address) {
        byte result;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = (address & 0xffff) - 0xc000;
            result = videoram[offset];
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xdd00;
            result = bublbobl_objectram[offset];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = Memory.mainram[offset];
        } else if ((address & 0xffff) >= 0xfc00 && (address & 0xffff) <= 0xfcff) {
            int offset = (address & 0xffff) - 0xfc00;
            result = mainram2[offset];
        } else if ((address & 0xffff) >= 0xfd00 && (address & 0xffff) <= 0xfdff) {
            int offset = (address & 0xffff) - 0xfd00;
            result = mainram3[offset];
        } else if ((address & 0xffff) >= 0xfe00 && (address & 0xffff) <= 0xfe03) {
            int offset = address - 0xfe00;
            result = boblbobl_ic43_a_r(offset);
        } else if ((address & 0xffff) >= 0xfe80 && (address & 0xffff) <= 0xfe83) {
            int offset = address - 0xfe80;
            result = boblbobl_ic43_b_r(offset);
        } else if ((address & 0xffff) == 0xff00) {
            result = dsw0;
        } else if ((address & 0xffff) == 0xff01) {
            result = dsw1;
        } else if ((address & 0xffff) == 0xff02) {
            result = sbyte0;
        } else if ((address & 0xffff) == 0xff03) {
            result = sbyte1;
        } else {
            result = 0;
        }
        return result;
    }

    public static void Z0WriteMemory_tokio(short address, byte value) {
        if ((address & 0xffff) >= 0x0000 && (address & 0xffff) <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            Memory.mainrom[basebankmain + offset] = value;
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = address - 0xc000;
            videoram[offset] = value;
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = address - 0xdd00;
            bublbobl_objectram[offset] = value;
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = address - 0xe000;
            Memory.mainram[offset] = value;
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xf9ff) {
            int offset = address - 0xf800;
            Generic.paletteram_RRRRGGGGBBBBxxxx_be_w(offset, value);
        } else if ((address & 0xffff) == 0xfa00) {
            Generic.watchdog_reset_w();
        } else if ((address & 0xffff) == 0xfa80) {
            tokio_bankswitch_w(value);
        } else if ((address & 0xffff) == 0xfb00) {
            tokio_videoctrl_w(value);
        } else if ((address & 0xffff) == 0xfb80) {
            bublbobl_nmitrigger_w();
        } else if ((address & 0xffff) == 0xfc00) {
            bublbobl_sound_command_w(value);
        } else if ((address & 0xffff) == 0xfe00) {

        }
    }

    public static void Z0WriteMemory_bootleg(short address, byte value) {
        if ((address & 0xffff) >= 0x0000 && (address & 0xffff) <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            Memory.mainrom[basebankmain + offset] = value;
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = (address & 0xffff) - 0xc000;
            videoram[offset] = value;
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xdd00;
            bublbobl_objectram[offset] = value;
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            Memory.mainram[offset] = value;
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xf9ff) {
            int offset = address - 0xf800;
            Generic.paletteram_RRRRGGGGBBBBxxxx_be_w(offset, value);
        } else if ((address & 0xffff) == 0xfa00) {
            bublbobl_sound_command_w(value);
        } else if ((address & 0xffff) == 0xfa03) {

        } else if ((address & 0xffff) == 0xfa80) {

        } else if ((address & 0xffff) == 0xfb40) {
            bublbobl_bankswitch_w(value);
        } else if ((address & 0xffff) >= 0xfc00 && (address & 0xffff) <= 0xfcff) {
            int offset = address - 0xfc00;
            mainram2[offset] = value;
        } else if ((address & 0xffff) >= 0xfd00 && (address & 0xffff) <= 0xfdff) {
            int offset = address - 0xfd00;
            mainram3[offset] = value;
        } else if ((address & 0xffff) >= 0xfe00 && (address & 0xffff) <= 0xfe03) {
            int offset = address - 0xfe00;
            boblbobl_ic43_a_w(offset);
        } else if (address >= 0xfe80 && address <= 0xfe83) {
            int offset = address - 0xfe80;
            boblbobl_ic43_b_w(offset, value);
        } else if (address == 0xff94) {

        } else if (address == 0xff98) {

        }
    }

    public static byte Z1ReadOp_tokio(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = slaverom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x97ff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte Z1ReadMemory_tokio(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = slaverom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x97ff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static void Z1WriteMemory_tokio(short address, byte value) {
        if ((address & 0xffff) <= 0x7fff) {
            slaverom[address] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x97ff) {
            int offset = (address & 0xffff) - 0x8000;
            Memory.mainram[offset] = value;
        }
    }

    public static byte Z2ReadMemory_tokio(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) == 0x9000) {
            result = (byte) Sound.soundlatch_r();
        } else if ((address & 0xffff) == 0x9800) {
            result = 0;
        } else if ((address & 0xffff) == 0xb000) {
            result = YM2203.ym2203_status_port_0_r();
        } else if ((address & 0xffff) == 0xb001) {
            result = YM2203.ym2203_read_port_0_r();
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xffff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static void Z2WriteMemory_tokio(short address, byte value) {
        if ((address & 0xffff) <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) == 0x9000) {

        } else if ((address & 0xffff) == 0xa000) {
            bublbobl_sh_nmi_disable_w();
        } else if ((address & 0xffff) == 0xa800) {
            bublbobl_sh_nmi_enable_w();
        } else if ((address & 0xffff) == 0xb000) {
            YM2203.ym2203_control_port_0_w(value);
        } else if ((address & 0xffff) == 0xb001) {
            YM2203.ym2203_write_port_0_w(value);
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xffff) {
            Memory.audiorom[address] = value;
        }
    }

    public static byte Z0ReadOp_bublbobl(short address) {
        byte result;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte Z0ReadMemory_bublbobl(short address) {
        byte result;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = (address & 0xffff) - 0xc000;
            result = videoram[offset];
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xdd00;
            result = bublbobl_objectram[offset];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = Memory.mainram[offset];
        } else if ((address & 0xffff) >= 0xfc00 && (address & 0xffff) <= 0xffff) {
            int offset = (address & 0xffff) - 0xfc00;
            result = bublbobl_mcu_sharedram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static void Z0WriteMemory_bublbobl(short address, byte value) {
        if ((address & 0xffff) >= 0x0000 && (address & 0xffff) <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = (address & 0xffff) - 0x8000;
            Memory.mainrom[basebankmain + offset] = value;
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdcff) {
            int offset = (address & 0xffff) - 0xc000;
            videoram[offset] = value;
        } else if ((address & 0xffff) >= 0xdd00 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xdd00;
            bublbobl_objectram[offset] = value;
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            Memory.mainram[offset] = value;
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xf9ff) {
            int offset = address - 0xf800;
            Generic.paletteram_RRRRGGGGBBBBxxxx_be_w(offset, value);
        } else if ((address & 0xffff) == 0xfa00) {
            bublbobl_sound_command_w(value);
        } else if ((address & 0xffff) == 0xfa80) {
            Watchdog.watchdog_reset();
        } else if ((address & 0xffff) == 0xfb40) {
            bublbobl_bankswitch_w(value);
        } else if ((address & 0xffff) >= 0xfc00 && (address & 0xffff) <= 0xffff) {
            int offset = (address & 0xffff) - 0xfc00;
            bublbobl_mcu_sharedram[offset] = value;
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

    public static byte Z1ReadOp_bublbobl(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = slaverom[address];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte Z1ReadMemory_bublbobl(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = slaverom[address];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static void Z1WriteMemory_bublbobl(short address, byte value) {
        if ((address & 0xffff) <= 0x7fff) {
            slaverom[(address & 0xffff)] = value;
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xe000;
            Memory.mainram[offset] = value;
        }
    }

    public static byte Z1ReadHardware(short address) {
        byte result = 0;
        address &= 0xff;
        return result;
    }

    public static void Z1WriteHardware(short address, byte value) {
        address &= 0xff;

    }

    public static int Z1IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[1].cpunum, 0);
    }

    public static byte Z2ReadOp_bublbobl(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.audioram[offset];
        }
        return result;
    }

    public static byte Z2ReadMemory_bublbobl(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) == 0x9000) {
            result = YM2203.ym2203_status_port_0_r();
        } else if ((address & 0xffff) == 0x9001) {
            result = YM2203.ym2203_read_port_0_r();
        } else if ((address & 0xffff) == 0xa000) {
            result = YM3812.ym3526_status_port_0_r();
        } else if ((address & 0xffff) == 0xb000) {
            result = (byte) Sound.soundlatch_r();
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xffff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static void Z2WriteMemory_bublbobl(short address, byte value) {
        if ((address & 0xffff) <= 0x7fff) {
            Memory.audiorom[(address & 0xffff)] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) == 0x9000) {
            YM2203.ym2203_control_port_0_w(value);
        } else if ((address & 0xffff) == 0x9001) {
            YM2203.ym2203_write_port_0_w(value);
        } else if ((address & 0xffff) == 0xa000) {
            YM3812.ym3526_control_port_0_w(value);
        } else if ((address & 0xffff) == 0xa001) {
            YM3812.ym3526_write_port_0_w(value);
        } else if ((address & 0xffff) == 0xb001) {
            bublbobl_sh_nmi_enable_w();
        } else if ((address & 0xffff) == 0xb002) {
            bublbobl_sh_nmi_disable_w();
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xffff) {
            Memory.audiorom[(address & 0xffff)] = value;
        }
    }

    public static byte Z2ReadHardware(short address) {
        byte result = 0;
        address &= 0xff;
        return result;
    }

    public static void Z2WriteHardware(short address, byte value) {
        address &= 0xff;
    }

    public static int Z2IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[2].cpunum, 0);
    }

    public static byte MReadOp_bublbobl(short address) {
        byte result = 0;
        if ((address & 0xffff) >= 0x0040 && (address & 0xffff) <= 0x00ff) {
            int offset = address - 0x0040;
            result = mcuram[offset];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xffff) {
            result = mcurom[address];
        }
        return result;
    }

    public static byte MReadMemory_bublbobl(short address) {
        byte result = 0;
        if (address == 0x0000) {
            result = bublbobl_mcu_ddr1_r();
        } else if (address == 0x0001) {
            result = bublbobl_mcu_ddr2_r();
        } else if (address == 0x0002) {
            result = bublbobl_mcu_port1_r();
        } else if (address == 0x0003) {
            result = bublbobl_mcu_port2_r();
        } else if (address == 0x0004) {
            result = bublbobl_mcu_ddr3_r();
        } else if (address == 0x0005) {
            result = bublbobl_mcu_ddr4_r();
        } else if (address == 0x0006) {
            result = bublbobl_mcu_port3_r();
        } else if (address == 0x0007) {
            result = bublbobl_mcu_port4_r();
        } else if (address >= 0x0040 && address <= 0x00ff) {
            int offset = address - 0x0040;
            result = mcuram[offset];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xffff) {
            result = mcurom[(address & 0xffff)];
        }
        return result;
    }

    public static void MWriteMemory_bublbobl(short address, byte value) {
        if (address == 0x0000) {
            bublbobl_mcu_ddr1_w(value);
        } else if (address == 0x0001) {
            bublbobl_mcu_ddr2_w(value);
        } else if (address == 0x0002) {
            bublbobl_mcu_port1_w(value);
        } else if (address == 0x0003) {
            bublbobl_mcu_port2_w(value);
        } else if (address == 0x0004) {
            bublbobl_mcu_ddr3_w(value);
        } else if (address == 0x0005) {
            bublbobl_mcu_ddr4_w(value);
        } else if (address == 0x0006) {
            bublbobl_mcu_port3_w(value);
        } else if (address == 0x0007) {
            bublbobl_mcu_port4_w(value);
        } else if (address >= 0x0040 && address <= 0x00ff) {
            int offset = address - 0x0040;
            mcuram[offset] = value;
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xffff) {
            mcurom[(address & 0xffff)] = value;
        }
    }

    public static byte MReadOp_bootleg(short address) {
        byte result = 0;
        address &= 0x7ff;
        if (address >= 0x010 && address <= 0x07f) {
            result = mcuram[address];
        } else if (address >= 0x080 && address <= 0x7ff) {
            result = mcurom[address];
        }
        return result;
    }

    public static byte MReadMemory_bootleg(short address) {
        byte result = 0;
        address &= 0x7ff;
        if (address == 0x000) {
            result = bublbobl_68705_portA_r();
        } else if (address == 0x001) {
            result = bublbobl_68705_portB_r();
        } else if (address == 0x002) {
            result = sbyte0;
        } else if (address >= 0x010 && address <= 0x07f) {
            result = mcuram[address];
        } else if (address >= 0x080 && address <= 0x7ff) {
            result = mcurom[address];
        }
        return result;
    }

    public static void MWriteMemory_bootleg(short address, byte value) {
        address &= 0x7ff;
        if (address == 0x000) {
            bublbobl_68705_portA_w(value);
        } else if (address == 0x001) {
            bublbobl_68705_portB_w(value);
        } else if (address == 0x004) {
            bublbobl_68705_ddrA_w(value);
        } else if (address == 0x005) {
            bublbobl_68705_ddrB_w(value);
        } else if (address == 0x006) {

        } else if (address >= 0x010 && address <= 0x07f) {
            mcuram[address] = value;
        } else if (address >= 0x080 && address <= 0x7ff) {
            mcurom[address] = value;
        }
    }

    public static byte MReadOpByte_opwolf(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_opwolf(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        int add1;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x0f_0000 && address <= 0x0f_ffff) {
            add1 = address & 0xfff;
            if (add1 >= 0 && add1 <= 0x7ff) {
                int offset = add1 / 2;
                if (add1 % 2 == 0) {
                    result = (byte) (opwolf_cchip_data_r(offset) >> 8);
                } else if (add1 % 2 == 1) {
                    result = (byte) opwolf_cchip_data_r(offset);
                }
            } else if (add1 >= 0x802 && add1 <= 0x803) {
                if (add1 == 0x802) {
                    result = 0;
                } else if (add1 == 0x803) {
                    result = (byte) opwolf_cchip_status_r();
                }
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x38_0000 && address <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = opwolf_dsw_r(offset);
            }
        } else if (address >= 0x3a_0000 && address <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (opwolf_lightgun_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) opwolf_lightgun_r(offset);
            }
        } else if (address >= 0x3e_0000 && address <= 0x3e_0001) {
            result = 0;
        } else if (address >= 0x3e_0002 && address <= 0x3e_0003) {
            if (address % 2 == 0) {
                result = (byte) (Taitosnd.taitosound_comm16_msb_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Taitosnd.taitosound_comm16_msb_r();
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (PC080SN_word_0_r(offset) >> 8);
            } else {
                result = (byte) PC080SN_word_0_r(offset);
            }
        } else if (address >= 0xd0_0000 && address <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (PC090OJ_word_0_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) PC090OJ_word_0_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_opwolf(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_opwolf(int address) {
        address &= 0xff_ffff;
        short result = 0;
        int add1;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x0f_0000 && address <= 0x0f_ffff) {
            add1 = address & 0xfff;
            if (add1 >= 0 && add1 <= 0x7ff) {
                int offset = add1 / 2;
                result = opwolf_cchip_data_r(offset);
            } else if (add1 >= 0x802 && add1 <= 0x803) {
                result = opwolf_cchip_status_r();
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x38_0000 && address + 1 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            result = opwolf_dsw_r(offset);
        } else if (address >= 0x3a_0000 && address + 1 <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            result = opwolf_lightgun_r(offset);
        } else if (address >= 0x3e_0000 && address + 1 <= 0x3e_0001) {
            result = 0;
        } else if (address >= 0x3e_0002 && address + 1 <= 0x3e_0003) {
            result = Taitosnd.taitosound_comm16_msb_r();
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            result = PC080SN_word_0_r(offset);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            result = PC090OJ_word_0_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_opwolf(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_opwolf(int address) {
        address &= 0xff_ffff;
        int result = 0;
        int add1;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x0f_0000 && address <= 0x0f_ffff) {
            add1 = address & 0xfff;
            if (add1 >= 0 && add1 <= 0x7ff) {
                int offset = add1 / 2;
                result = opwolf_cchip_data_r(offset) * 0x1_0000 + opwolf_cchip_data_r(offset + 1);
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x10_00000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x38_0000 && address + 3 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            result = opwolf_dsw_r(offset) * 0x1_0000 + opwolf_dsw_r(offset + 1);
        } else if (address >= 0x3a_0000 && address + 3 <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            result = opwolf_lightgun_r(offset) * 0x1_0000 + opwolf_lightgun_r(offset + 1);
        } else if (address >= 0xc0_0000 && address + 3 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            result = PC080SN_word_0_r(offset) * 0x1_0000 + PC080SN_word_0_r(offset + 1);
        } else if (address >= 0xd0_0000 && address + 3 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            result = PC090OJ_word_0_r(offset) * 0x1_0000 + PC090OJ_word_0_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_opwolf(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = value;
            }
        } else if (address >= 0x0f_f000 && address <= 0x0f_f7ff) {
            int offset = (address - 0x0f_f000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                opwolf_cchip_data_w2(offset, value);
            }
        } else if (address >= 0x0f_f802 && address <= 0x0f_f803) {
            opwolf_cchip_status_w();
        } else if (address >= 0x0f_fc00 && address <= 0x0f_fc01) {
            if (address == 0x0f_fc01) {
                opwolf_cchip_bank_w(value);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w2(offset, value);
            }
        } else if (address >= 0x38_0000 && address <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            if (address % 2 == 1) {
                opwolf_spritectrl_w2(offset, value);
            }
        } else if (address >= 0x3c_0000 && address <= 0x3c_0001) {
            int i1 = 1;
        } else if (address >= 0x3e_0000 && address <= 0x3e_0001) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_port16_msb_w1(value);
            }
        } else if (address >= 0x3e_0002 && address <= 0x3e_0003) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_comm16_msb_w1(value);
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_word_0_w2(offset, value);
            }
        } else if (address >= 0xc1_0000 && address <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = value;
        } else if (address >= 0xc2_0000 && address <= 0xc2_0003) {
            int offset = (address - 0xc2_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_yscroll_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_yscroll_word_0_w2(offset, value);
            }
        } else if (address >= 0xc4_0000 && address <= 0xc4_0003) {
            int offset = (address - 0xc4_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_xscroll_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_xscroll_word_0_w2(offset, value);
            }
        } else if (address >= 0xc5_0000 && address <= 0xc5_0003) {
            int offset = (address - 0xc5_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_ctrl_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_ctrl_word_0_w2(offset, value);
            }
        } else if (address >= 0xd0_0000 && address <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            if (address % 2 == 0) {
                PC090OJ_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC090OJ_word_0_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_opwolf(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 1 <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 8);
                Memory.mainrom[address + 1] = (byte) value;
            }
        } else if (address >= 0x0f_f000 && address + 1 <= 0x0f_f7ff) {
            int offset = (address - 0x0f_f000) / 2;
            opwolf_cchip_data_w(offset, value);
        } else if (address >= 0x0f_f802 && address + 1 <= 0x0f_f803) {
            opwolf_cchip_status_w();
        } else if (address >= 0x0f_fc00 && address + 1 <= 0x0f_fc01) {
            opwolf_cchip_bank_w((byte) value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, value);
        } else if (address >= 0x38_0000 && address + 1 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            opwolf_spritectrl_w(offset, value);
        } else if (address >= 0x3c_0000 && address + 1 <= 0x3c_0001) {
            int i1 = 1;
        } else if (address >= 0x3e_0000 && address + 1 <= 0x3e_0001) {
            Taitosnd.taitosound_port16_msb_w(value);
        } else if (address >= 0x3e_0002 && address + 1 <= 0x3e_0003) {
            Taitosnd.taitosound_comm16_msb_w(value);
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            PC080SN_word_0_w(offset, value);
        } else if (address >= 0xc1_0000 && address + 1 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0xc2_0000 && address + 1 <= 0xc2_0003) {
            int offset = (address - 0xc2_0000) / 2;
            PC080SN_yscroll_word_0_w(offset, value);
        } else if (address >= 0xc4_0000 && address + 1 <= 0xc4_0003) {
            int offset = (address - 0xc4_0000) / 2;
            PC080SN_xscroll_word_0_w(offset, value);
        } else if (address >= 0xc5_0000 && address + 1 <= 0xc5_0003) {
            int offset = (address - 0xc5_0000) / 2;
            PC080SN_ctrl_word_0_w(offset, value);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            PC090OJ_word_0_w(offset, value);
        }
    }

    public static void MWriteLong_opwolf(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 3 <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 24);
                Memory.mainrom[address + 1] = (byte) (value >> 16);
                Memory.mainrom[address + 2] = (byte) (value >> 8);
                Memory.mainrom[address + 3] = (byte) value;
            }
        } else if (address >= 0x0f_f000 && address + 3 <= 0x0f_f7ff) {
            int offset = (address - 0x0f_f000) / 2;
            opwolf_cchip_data_w(offset, (short) (value >> 16));
            opwolf_cchip_data_w(offset + 1, (short) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset + 1, (short) value);
        } else if (address >= 0x38_0000 && address + 3 <= 0x38_0003) {
            int i1 = 1;
        } else if (address >= 0xc0_0000 && address + 3 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            PC080SN_word_0_w(offset, (short) (value >> 16));
            PC080SN_word_0_w(offset + 1, (short) value);
        } else if (address >= 0xc1_0000 && address + 3 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0xd0_0000 && address + 3 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            PC090OJ_word_0_w(offset, (short) (value >> 16));
            PC090OJ_word_0_w(offset + 1, (short) value);
        }
    }

    public static byte MReadByte_opwolfb(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x0f_0008 && address <= 0x0f_000b) {
            int offset = (address - 0x0f_0008) / 2;
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = opwolf_in_r(offset);
            }
        } else if (address >= 0x0f_f000 && address <= 0x0f_ffff) {
            int offset = (address - 0x0f_f000) / 2;
            if (address % 2 == 0) {
                result = (byte) (cchip_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = cchip_r(offset);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x38_0000 && address <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = opwolf_dsw_r(offset);
            }
        } else if (address >= 0x3a_0000 && address <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (opwolf_lightgun_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) opwolf_lightgun_r(offset);
            }
        } else if (address >= 0x3e_0000 && address <= 0x3e_0001) {
            result = 0;
        } else if (address >= 0x3e_0002 && address <= 0x3e_0003) {
            if (address % 2 == 0) {
                result = (byte) (Taitosnd.taitosound_comm16_msb_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Taitosnd.taitosound_comm16_msb_r();
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (PC080SN_word_0_r(offset) >> 8);
            } else {
                result = (byte) PC080SN_word_0_r(offset);
            }
        } else if (address >= 0xd0_0000 && address <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (PC090OJ_word_0_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) PC090OJ_word_0_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_opwolfb(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_opwolfb(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x0f_0008 && address + 1 <= 0x0f_000b) {
            int offset = (address - 0x0f_0008) / 2;
            result = opwolf_in_r(offset);
        } else if (address >= 0x0f_f000 && address + 1 <= 0x0f_ffff) {
            int offset = (address - 0x0f_f000) / 2;
            result = cchip_r(offset);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x38_0000 && address + 1 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            result = opwolf_dsw_r(offset);
        } else if (address >= 0x3a_0000 && address + 1 <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            result = opwolf_lightgun_r(offset);
        } else if (address >= 0x3e_0000 && address + 1 <= 0x3e_0001) {
            result = 0;
        } else if (address >= 0x3e_0002 && address + 1 <= 0x3e_0003) {
            result = Taitosnd.taitosound_comm16_msb_r();
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            result = PC080SN_word_0_r(offset);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            result = PC090OJ_word_0_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_opwolfb(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_opwolfb(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x0f_0008 && address + 3 <= 0x0f_000b) {
            int offset = (address - 0x0f_0008) / 2;
            result = opwolf_in_r(offset) * 0x1_0000 + opwolf_in_r(offset + 1);
        } else if (address >= 0x0f_f000 && address + 3 <= 0x0f_ffff) {
            int offset = (address - 0x0f_f000) / 2;
            result = cchip_r(offset) * 0x1_0000 + cchip_r(offset + 1);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x38_0000 && address + 3 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            result = opwolf_dsw_r(offset) * 0x1_0000 + opwolf_dsw_r(offset + 1);
        } else if (address >= 0x3a_0000 && address + 3 <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            result = opwolf_lightgun_r(offset) * 0x1_0000 + opwolf_lightgun_r(offset + 1);
        } else if (address >= 0xc0_0000 && address + 3 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            result = PC080SN_word_0_r(offset) * 0x1_0000 + PC080SN_word_0_r(offset + 1);
        } else if (address >= 0xd0_0000 && address + 3 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            result = PC090OJ_word_0_r(offset) * 0x1_0000 + PC090OJ_word_0_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_opwolfb(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = value;
            }
        } else if (address >= 0x0f_f000 && address <= 0x0f_ffff) {
            int offset = (address - 0x0f_f000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                cchip_w(offset, value);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w2(offset, value);
            }
        } else if (address >= 0x38_0000 && address <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            if (address % 2 == 1) {
                opwolf_spritectrl_w2(offset, value);
            }
        } else if (address >= 0x3c_0000 && address <= 0x3c_0001) {
            int i1 = 1;
        } else if (address >= 0x3e_0000 && address <= 0x3e_0001) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_port16_msb_w1(value);
            }
        } else if (address >= 0x3e_0002 && address <= 0x3e_0003) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_comm16_msb_w1(value);
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_word_0_w2(offset, value);
            }
        } else if (address >= 0xc1_0000 && address <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = value;
        } else if (address >= 0xc2_0000 && address <= 0xc2_0003) {
            int offset = (address - 0xc2_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_yscroll_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_yscroll_word_0_w2(offset, value);
            }
        } else if (address >= 0xc4_0000 && address <= 0xc4_0003) {
            int offset = (address - 0xc4_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_xscroll_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_xscroll_word_0_w2(offset, value);
            }
        } else if (address >= 0xc5_0000 && address <= 0xc5_0003) {
            int offset = (address - 0xc5_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_ctrl_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_ctrl_word_0_w2(offset, value);
            }
        } else if (address >= 0xd0_0000 && address <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            if (address % 2 == 0) {
                PC090OJ_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC090OJ_word_0_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_opwolfb(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 1 <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 8);
                Memory.mainrom[address + 1] = (byte) value;
            }
        } else if (address >= 0x0f_f000 && address + 1 <= 0x0f_ffff) {
            int offset = (address - 0x0f_f000) / 2;
            cchip_w(offset, (byte) value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, value);
        } else if (address >= 0x38_0000 && address + 1 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            opwolf_spritectrl_w(offset, value);
        } else if (address >= 0x3c_0000 && address + 1 <= 0x3c_0001) {
            int i1 = 1;
        } else if (address >= 0x3e_0000 && address + 1 <= 0x3e_0001) {
            Taitosnd.taitosound_port16_msb_w(value);
        } else if (address >= 0x3e_0002 && address + 1 <= 0x3e_0003) {
            Taitosnd.taitosound_comm16_msb_w(value);
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            PC080SN_word_0_w(offset, value);
        } else if (address >= 0xc1_0000 && address + 1 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0xc2_0000 && address + 1 <= 0xc2_0003) {
            int offset = (address - 0xc2_0000) / 2;
            PC080SN_yscroll_word_0_w(offset, value);
        } else if (address >= 0xc4_0000 && address + 1 <= 0xc4_0003) {
            int offset = (address - 0xc4_0000) / 2;
            PC080SN_xscroll_word_0_w(offset, value);
        } else if (address >= 0xc5_0000 && address + 1 <= 0xc5_0003) {
            int offset = (address - 0xc5_0000) / 2;
            PC080SN_ctrl_word_0_w(offset, value);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            PC090OJ_word_0_w(offset, value);
        }
    }

    public static void MWriteLong_opwolfb(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 3 <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 24);
                Memory.mainrom[address + 1] = (byte) (value >> 16);
                Memory.mainrom[address + 2] = (byte) (value >> 8);
                Memory.mainrom[address + 3] = (byte) value;
            }
        } else if (address >= 0x0f_f000 && address + 3 <= 0x0f_ffff) {
            int offset = (address - 0x0f_f000) / 2;
            cchip_w(offset, (byte) (value >> 16));
            cchip_w(offset + 1, (byte) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset + 1, (short) value);
        } else if (address >= 0x38_0000 && address + 3 <= 0x38_0003) {
            int i1 = 1;
        } else if (address >= 0xc0_0000 && address + 3 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            PC080SN_word_0_w(offset, (short) (value >> 16));
            PC080SN_word_0_w(offset + 1, (short) value);
        } else if (address >= 0xc1_0000 && address + 3 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0xd0_0000 && address + 3 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            PC090OJ_word_0_w(offset, (short) (value >> 16));
            PC090OJ_word_0_w(offset + 1, (short) value);
        }
    }

    public static byte MReadByte_opwolfp(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x38_0000 && address <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = opwolf_dsw_r(offset);
            }
        } else if (address >= 0x3a_0000 && address <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (opwolf_lightgun_r_p(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) opwolf_lightgun_r_p(offset);
            }
        } else if (address >= 0x3e_0000 && address <= 0x3e_0001) {
            result = 0;
        } else if (address >= 0x3e_0002 && address <= 0x3e_0003) {
            if (address % 2 == 0) {
                result = (byte) (Taitosnd.taitosound_comm16_msb_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Taitosnd.taitosound_comm16_msb_r();
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (PC080SN_word_0_r(offset) >> 8);
            } else {
                result = (byte) PC080SN_word_0_r(offset);
            }
        } else if (address >= 0xd0_0000 && address <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (PC090OJ_word_0_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) PC090OJ_word_0_r(offset);
            }
        }
        return result;
    }

    public static short MReadWord_opwolfp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x38_0000 && address + 1 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            result = opwolf_dsw_r(offset);
        } else if (address >= 0x3a_0000 && address + 1 <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            result = opwolf_lightgun_r_p(offset);
        } else if (address >= 0x3e_0000 && address + 1 <= 0x3e_0001) {
            result = 0;
        } else if (address >= 0x3e_0002 && address + 1 <= 0x3e_0003) {
            result = Taitosnd.taitosound_comm16_msb_r();
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            result = PC080SN_word_0_r(offset);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            result = PC090OJ_word_0_r(offset);
        }
        return result;
    }

    public static int MReadLong_opwolfp(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x38_0000 && address + 3 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            result = opwolf_dsw_r(offset) * 0x1_0000 + opwolf_dsw_r(offset + 1);
        } else if (address >= 0x3a_0000 && address + 3 <= 0x3a_0003) {
            int offset = (address - 0x3a_0000) / 2;
            result = opwolf_lightgun_r(offset) * 0x1_0000 + opwolf_lightgun_r(offset + 1);
        } else if (address >= 0xc0_0000 && address + 3 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            result = PC080SN_word_0_r(offset) * 0x1_0000 + PC080SN_word_0_r(offset + 1);
        } else if (address >= 0xd0_0000 && address + 3 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            result = PC090OJ_word_0_r(offset) * 0x1_0000 + PC090OJ_word_0_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_opwolfp(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = value;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w2(offset, value);
            }
        } else if (address >= 0x38_0000 && address <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            if (address % 2 == 1) {
                opwolf_spritectrl_w2(offset, value);
            }
        } else if (address >= 0x3c_0000 && address <= 0x3c_0001) {
            int i1 = 1;
        } else if (address >= 0x3e_0000 && address <= 0x3e_0001) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_port16_msb_w1(value);
            }
        } else if (address >= 0x3e_0002 && address <= 0x3e_0003) {
            if (address % 2 == 0) {
                Taitosnd.taitosound_comm16_msb_w1(value);
            }
        } else if (address >= 0xc0_0000 && address <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_word_0_w2(offset, value);
            }
        } else if (address >= 0xc1_0000 && address <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = value;
        } else if (address >= 0xc2_0000 && address <= 0xc2_0003) {
            int offset = (address - 0xc2_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_yscroll_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_yscroll_word_0_w2(offset, value);
            }
        } else if (address >= 0xc4_0000 && address <= 0xc4_0003) {
            int offset = (address - 0xc4_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_xscroll_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_xscroll_word_0_w2(offset, value);
            }
        } else if (address >= 0xc5_0000 && address <= 0xc5_0003) {
            int offset = (address - 0xc5_0000) / 2;
            if (address % 2 == 0) {
                PC080SN_ctrl_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC080SN_ctrl_word_0_w2(offset, value);
            }
        } else if (address >= 0xd0_0000 && address <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            if (address % 2 == 0) {
                PC090OJ_word_0_w1(offset, value);
            } else if (address % 2 == 1) {
                PC090OJ_word_0_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_opwolfp(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 1 <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 8);
                Memory.mainrom[address + 1] = (byte) value;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, value);
        } else if (address >= 0x38_0000 && address + 1 <= 0x38_0003) {
            int offset = (address - 0x38_0000) / 2;
            opwolf_spritectrl_w(offset, value);
        } else if (address >= 0x3c_0000 && address + 1 <= 0x3c_0001) {
            int i1 = 1;
        } else if (address >= 0x3e_0000 && address + 1 <= 0x3e_0001) {
            Taitosnd.taitosound_port16_msb_w(value);
        } else if (address >= 0x3e_0002 && address + 1 <= 0x3e_0003) {
            Taitosnd.taitosound_comm16_msb_w(value);
        } else if (address >= 0xc0_0000 && address + 1 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            PC080SN_word_0_w(offset, value);
        } else if (address >= 0xc1_0000 && address + 1 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0xc2_0000 && address + 1 <= 0xc2_0003) {
            int offset = (address - 0xc2_0000) / 2;
            PC080SN_yscroll_word_0_w(offset, value);
        } else if (address >= 0xc4_0000 && address + 1 <= 0xc4_0003) {
            int offset = (address - 0xc4_0000) / 2;
            PC080SN_xscroll_word_0_w(offset, value);
        } else if (address >= 0xc5_0000 && address + 1 <= 0xc5_0003) {
            int offset = (address - 0xc5_0000) / 2;
            PC080SN_ctrl_word_0_w(offset, value);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            PC090OJ_word_0_w(offset, value);
        }
    }

    public static void MWriteLong_opwolfp(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x00_0000 && address + 3 <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                Memory.mainrom[address] = (byte) (value >> 24);
                Memory.mainrom[address + 1] = (byte) (value >> 16);
                Memory.mainrom[address + 2] = (byte) (value >> 8);
                Memory.mainrom[address + 3] = (byte) value;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xxxxRRRRGGGGBBBB_word_w(offset + 1, (short) value);
        } else if (address >= 0x38_0000 && address + 3 <= 0x38_0003) {
            int i1 = 1;
        } else if (address >= 0xc0_0000 && address + 3 <= 0xc0_ffff) {
            int offset = (address - 0xc0_0000) / 2;
            PC080SN_word_0_w(offset, (short) (value >> 16));
            PC080SN_word_0_w(offset + 1, (short) value);
        } else if (address >= 0xc1_0000 && address + 3 <= 0xc1_ffff) {
            int offset = address - 0xc1_0000;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0xd0_0000 && address + 3 <= 0xd0_3fff) {
            int offset = (address - 0xd0_0000) / 2;
            PC090OJ_word_0_w(offset, (short) (value >> 16));
            PC090OJ_word_0_w(offset + 1, (short) value);
        }
    }

    public static byte ZReadOp_opwolf(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x3fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x4000 && (address & 0xffff) <= 0x7fff) {
            int offset = address - 0x4000;
            result = Memory.audiorom[basebanksnd + offset];
        }
        return result;
    }

    public static byte ZReadMemory_opwolf(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x3fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x4000 && (address & 0xffff) <= 0x7fff) {
            int offset = (address & 0xffff) - 0x4000;
            result = Memory.audiorom[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) == 0x9001) {
            result = YM2151.ym2151_status_port_0_r();
        } else if ((address & 0xffff) >= 0x9002 && (address & 0xffff) <= 0x9100) {
            int offset = (address & 0xffff) - 0x9002;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) == 0xa001) {
            result = Taitosnd.taitosound_slave_comm_r();
        }
        return result;
    }

    public static void ZWriteMemory_opwolf(short address, byte value) {
        if ((address & 0xffff) <= 0x3fff) {
            Memory.audiorom[address] = value;
        } else if ((address & 0xffff) >= 0x4000 && (address & 0xffff) <= 0x7fff) {
            int offset = (address & 0xffff) - 0x4000;
            Memory.audiorom[basebanksnd + offset] = value;
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0x8fff) {
            int offset = (address & 0xffff) - 0x8000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) == 0x9000) {
            YM2151.ym2151_register_port_0_w(value);
        } else if ((address & 0xffff) == 0x9001) {
            YM2151.ym2151_data_port_0_w(value);
        } else if ((address & 0xffff) == 0xa000) {
            Taitosnd.taitosound_slave_port_w(value);
        } else if ((address & 0xffff) == 0xa001) {
            Taitosnd.taitosound_slave_comm_w(value);
        } else if ((address & 0xffff) >= 0xb000 && (address & 0xffff) <= 0xb006) {
            int offset = address - 0xb000;
            opwolf_adpcm_b_w(offset, value);
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xc006) {
            int offset = address - 0xc000;
            opwolf_adpcm_c_w(offset, value);
        } else if ((address & 0xffff) == 0xd000) {
            opwolf_adpcm_d_w();
        } else if ((address & 0xffff) == 0xe000) {
            opwolf_adpcm_e_w();
        }
    }

    public static byte ZReadOp_opwolf_sub(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = subrom[address];
        }
        return result;
    }

    public static byte ZReadMemory_opwolf_sub(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = subrom[address];
        } else if ((address & 0xffff) == 0x8800) {
            result = z80_input1_r();
        } else if ((address & 0xffff) == 0x9800) {
            result = z80_input2_r();
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xc7ff) {
            int offset = (address & 0xffff) - 0xc000;
            result = cchip_ram[offset];
        }
        return result;
    }

    public static void ZWriteMemory_opwolf_sub(short address, byte value) {
        if ((address & 0xffff) <= 0x7fff) {
            subrom[address] = value;
        } else if ((address & 0xffff) == 0x8000) {
            int offset = (address & 0xffff) - 0x8000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) == 0x9000) {
            int i1 = 1;
        } else if ((address & 0xffff) == 0xa000) {
            int i1 = 1;
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xc7ff) {
            int offset = (address & 0xffff) - 0xc000;
            cchip_ram[offset] = value;
        }
    }

    public static byte MReadHardware(short address) {
        byte result = 0;
        address &= 0xff;
        if (address == 0x01) {
            result = (byte) Sound.soundlatch_r();
        }
        return result;
    }

    public static void MWriteHardware(short address, byte value) {
        address &= 0xff;
    }

    public static int MIRQCallback() {
        return 0;
    }

//#endregion

//#region Opwolf

    public static int opwolf_region;
    public static byte[] cchip_ram, adpcmrom;
    public static byte[] adpcm_b = new byte[0x08];
    public static byte[] adpcm_c = new byte[0x08];
    public static int opwolf_gun_xoffs, opwolf_gun_yoffs;
    public static byte dswa, dswb, p1x, p1y;
    public static final int[] adpcm_pos = new int[2];
    public static final int[] adpcm_end = new int[2];
    public static final int[] adpcm_data = new int[2];
    public static short m_sprite_ctrl;
    public static short m_sprites_flipscreen;
    public static byte current_bank = 0;
    public static byte current_cmd = 0;
    public static byte cchip_last_7a = 0;
    public static byte cchip_last_04 = 0;
    public static byte cchip_last_05 = 0;
    public static byte[] cchip_coins_for_credit = new byte[2];
    public static byte[] cchip_credits_for_coin = new byte[2];
    public static byte[] cchip_coins = new byte[2];
    public static byte c588 = 0, c589 = 0, c58a = 0;
    public static byte m_triggeredLevel1b; // These variables derived from comparison to unprotection version
    public static byte m_triggeredLevel2;
    public static byte m_triggeredLevel2b;
    public static byte m_triggeredLevel2c;
    public static byte m_triggeredLevel3b;
    public static byte m_triggeredLevel13b;
    public static byte m_triggeredLevel4;
    public static byte m_triggeredLevel5;
    public static byte m_triggeredLevel7;
    public static byte m_triggeredLevel8;
    public static byte m_triggeredLevel9;
    public static final short[] level_data_00 = new short[] {
            0x0480, 0x1008, 0x0300, 0x5701, 0x0001, 0x0010,
            0x0480, 0x1008, 0x0300, 0x5701, 0x0001, 0x002b,
            0x0780, 0x0009, 0x0300, 0x4a01, 0x0004, 0x0020,
            0x0780, 0x1208, 0x0300, 0x5d01, 0x0004, 0x0030,
            0x0780, 0x0209, 0x0300, 0x4c01, 0x0004, 0x0038,
            0x0780, 0x0309, 0x0300, 0x4d01, 0x0004, 0x0048,
            0x0980, 0x1108, 0x0300, 0x5a01, (short) 0xc005, 0x0018,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0xc005, 0x0028,
            0x0b80, 0x020a, 0x0000, 0x6401, (short) 0x8006, 0x0004,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, (short) 0x8006, (short) 0x8002,
            0x0b80, 0x020a, 0x0000, 0x6401, (short) 0x8006, 0x0017,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, (short) 0x8006, (short) 0x8015,
            0x0b80, 0x020a, 0x0000, 0x6401, 0x0007, 0x0034,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, 0x0007, (short) 0x8032,
            0x0b80, 0x020a, 0x0000, 0x6401, (short) 0x8006, (short) 0x803e,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, (short) 0x8006, (short) 0x803d,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x0008,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x000b,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x001b,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x001e,
            0x0b80, 0x100a, 0x0000, 0x6001, (short) 0x8007, 0x0038,
            0x0b80, 0x100a, 0x0000, 0x6001, (short) 0x8007, 0x003b,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, (short) 0x8042,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, (short) 0x8045,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, (short) 0x800b, (short) 0x8007,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, (short) 0x800b, (short) 0x801a,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, 0x000c, (short) 0x8037,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, (short) 0x800b, 0x0042,
            0x0c80, (short) 0xd04b, 0x0000, (short) 0xf301, (short) 0x8006, (short) 0x8009,
            0x0c80, (short) 0xd04b, 0x0000, (short) 0xf301, (short) 0x8006, (short) 0x801c,
            0x0c80, (short) 0xd04b, 0x0000, (short) 0xf301, (short) 0x8006, 0x0044,
            0x0c80, 0x030b, 0x0000, (short) 0xf401, 0x0008, 0x0028,
            0x0c80, 0x030b, 0x0000, (short) 0xf401, 0x0008, (short) 0x804b,
            0x0c00, 0x040b, 0x0000, (short) 0xf501, 0x0008, (short) 0x8026,
            (short) 0xffff
    };
    public static final short[] level_data_01 = new short[] {
            0x0780, 0x0209, 0x0300, 0x4c01, 0x0004, 0x0010,
            0x0780, 0x0209, 0x0300, 0x4c01, 0x4004, 0x0020,
            0x0780, 0x0309, 0x0300, 0x4d01, (short) 0xe003, 0x0030,
            0x0780, 0x0309, 0x0300, 0x4d01, (short) 0x8003, 0x0040,
            0x0780, 0x0209, 0x0300, 0x4c01, (short) 0x8004, 0x0018,
            0x0780, 0x0309, 0x0300, 0x4d01, (short) 0xc003, 0x0028,
            0x0b80, 0x000b, 0x0000, 0x0b02, (short) 0x8009, 0x0029,
            0x0b80, 0x0409, 0x0000, 0x0f02, (short) 0x8008, (short) 0x8028,
            0x0b80, 0x040a, 0x0000, 0x3502, 0x000a, (short) 0x8028,
            0x0b80, 0x050a, 0x0000, 0x1002, (short) 0x8006, (short) 0x8028,
            0x0b80, 0x120a, 0x0000, 0x3602, 0x0008, 0x004d,
            0x0b80, 0x120a, 0x0000, 0x3602, 0x0008, 0x004f,
            0x0b80, 0x120a, 0x0000, 0x3602, 0x0008, 0x0001,
            0x0b80, 0x120a, 0x0000, 0x3602, 0x0008, 0x0003,
            0x0b80, 0x130a, 0x0000, 0x3a02, 0x0007, 0x0023,
            0x0b80, 0x130a, 0x0000, 0x3a02, 0x0007, (short) 0x8025,
            0x0b80, 0x130a, 0x0000, 0x3a02, (short) 0x8009, 0x0023,
            0x0b80, 0x130a, 0x0000, 0x3a02, (short) 0x8009, (short) 0x8025,
            0x0b80, 0x140a, 0x0000, 0x3e02, 0x0007, 0x000d,
            0x0b80, 0x140a, 0x0000, 0x3e02, 0x0007, (short) 0x800f,
            0x0b80, 0x000b, 0x0000, 0x0102, 0x0007, (short) 0x804e,
            0x0b80, (short) 0xd24b, 0x0000, 0x0302, 0x0007, 0x000e,
            0x0b80, 0x000b, 0x0000, 0x0402, (short) 0x8006, 0x0020,
            0x0b80, (short) 0xd34b, 0x0000, 0x0502, (short) 0x8006, 0x0024,
            0x0b80, 0x000b, 0x0000, 0x0602, (short) 0x8009, 0x0001,
            0x0b80, (short) 0xd44b, 0x0000, 0x0702, (short) 0x800b, (short) 0x800b,
            0x0b80, (short) 0xd54b, 0x0000, 0x0802, (short) 0x800b, 0x000e,
            0x0b80, 0x000b, 0x0000, 0x0902, (short) 0x800b, 0x0010,
            0x0b80, 0x000b, 0x0000, 0x0a02, 0x0009, 0x0024,
            0x0b80, (short) 0xd64b, 0x0000, 0x0c02, 0x000c, (short) 0x8021,
            0x0b80, 0x000b, 0x0000, 0x0d02, 0x000c, 0x0025,
            0x0b80, 0x000b, 0x0000, 0x0e02, (short) 0x8009, 0x004e,
            0x0b80, 0x000b, 0x0300, 0x4e01, (short) 0x8006, (short) 0x8012,
            0x0b80, 0x000b, 0x0300, 0x4e01, 0x0007, (short) 0x8007,
            (short) 0xffff
    };
    public static final short[] level_data_02 = new short[] {
            0x0480, 0x000b, 0x0300, 0x4501, 0x0001, 0x0018,
            0x0480, 0x000b, 0x0300, 0x4501, 0x2001, 0x0030,
            0x0780, 0x1208, 0x0300, 0x5d01, 0x0004, 0x0010,
            0x0780, 0x1208, 0x0300, 0x5d01, 0x2004, 0x001c,
            0x0780, 0x1208, 0x0300, 0x5d01, (short) 0xe003, 0x0026,
            0x0780, 0x1208, 0x0300, 0x5d01, (short) 0x8003, 0x0034,
            0x0780, 0x1208, 0x0300, 0x5d01, 0x3004, 0x0040,
            0x0780, 0x010c, 0x0300, 0x4601, 0x4004, 0x0022,
            0x0780, 0x010c, 0x0300, 0x4601, 0x6004, 0x0042,
            0x0780, 0x000c, 0x0500, 0x7b01, (short) 0x800b, 0x0008,
            0x0780, 0x010c, 0x0300, 0x4601, 0x2004, 0x0008,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0b80, 0x000b, 0x0000, 0x1902, 0x000b, 0x0004,
            0x0b80, 0x000b, 0x0000, 0x1a02, 0x0009, (short) 0x8003,
            0x0b80, 0x000b, 0x0000, 0x1902, 0x000b, 0x000c,
            0x0b80, 0x000b, 0x0000, 0x1a02, 0x0009, (short) 0x800b,
            0x0b80, 0x000b, 0x0000, 0x1902, 0x000b, 0x001c,
            0x0b80, 0x000b, 0x0000, 0x1a02, 0x0009, (short) 0x801b,
            0x0b80, 0x000b, 0x0000, 0x1902, 0x000b, 0x002c,
            0x0b80, 0x000b, 0x0000, 0x1a02, 0x0009, (short) 0x802b,
            0x0b80, 0x000b, 0x0000, 0x1902, 0x000b, 0x0044,
            0x0b80, 0x000b, 0x0000, 0x1a02, 0x0009, (short) 0x8043,
            0x0b80, 0x000b, 0x0000, 0x1902, 0x000b, 0x004c,
            0x0b80, 0x000b, 0x0000, 0x1a02, 0x0009, (short) 0x804b,
            0x0b80, 0x020c, 0x0300, 0x4801, (short) 0xa009, 0x0010,
            0x0b80, 0x020c, 0x0300, 0x4801, (short) 0xa009, 0x0028,
            0x0b80, 0x020c, 0x0300, 0x4801, (short) 0xa009, 0x0036,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            (short) 0xffff
    };
    public static final short[] level_data_03 = new short[] {
            0x0480, 0x000b, 0x0300, 0x4501, 0x0001, 0x0018,
            0x0480, 0x000b, 0x0300, 0x4501, 0x2001, 0x002b,
            0x0780, 0x010c, 0x0300, 0x4601, 0x0004, 0x000d,
            0x0780, 0x000c, 0x0500, 0x7b01, (short) 0x800b, 0x0020,
            0x0780, 0x010c, 0x0300, 0x4601, 0x2004, 0x0020,
            0x0780, 0x010c, 0x0300, 0x4601, (short) 0x8003, 0x0033,
            0x0780, 0x010c, 0x0300, 0x4601, 0x0004, 0x003c,
            0x0780, 0x010c, 0x0300, 0x4601, (short) 0xd003, 0x0045,
            0x0780, 0x000c, 0x0500, 0x7b01, (short) 0x900b, 0x0041,
            0x0780, 0x010c, 0x0300, 0x4601, 0x3004, 0x0041,
            0x0b80, 0x020c, 0x0300, 0x4801, 0x0007, 0x0000,
            0x0b80, 0x410a, 0x0000, 0x2b02, (short) 0xe006, 0x4049,
            0x0b80, 0x020c, 0x0300, 0x4801, (short) 0x8007, 0x000b,
            0x0b80, 0x000b, 0x0000, 0x2702, (short) 0x800a, (short) 0x8005,
            0x0b80, 0x000b, 0x0000, 0x1e02, 0x0008, (short) 0x800e,
            0x0b80, 0x000b, 0x0000, 0x1f02, (short) 0x8007, 0x0011,
            0x0b80, 0x000b, 0x0000, 0x2802, 0x000b, 0x0012,
            0x0b80, 0x000b, 0x0000, 0x2002, 0x0007, (short) 0x8015,
            0x0b80, 0x000b, 0x0000, 0x2102, 0x0007, (short) 0x801b,
            0x0b80, 0x000b, 0x0000, 0x2902, (short) 0x800a, 0x001a,
            0x0b80, 0x000b, 0x0000, 0x2202, (short) 0x8007, 0x001e,
            0x0b80, 0x000b, 0x0000, 0x1e02, 0x0008, 0x0025,
            0x0b80, 0x000b, 0x0000, 0x2302, (short) 0x8007, (short) 0x802c,
            0x0b80, 0x000b, 0x0000, 0x2802, 0x000b, (short) 0x8028,
            0x0b80, 0x020c, 0x0300, 0x4801, 0x0007, 0x0030,
            0x0b80, 0x400a, 0x0000, 0x2e02, 0x4007, 0x002d,
            0x0b80, 0x000b, 0x0000, 0x2702, (short) 0x800a, (short) 0x8035,
            0x0b80, 0x020c, 0x0300, 0x4801, (short) 0x8007, 0x0022,
            0x0b80, 0x000b, 0x0000, 0x2402, (short) 0x8007, 0x0047,
            0x0b80, 0x000b, 0x0000, 0x2a02, (short) 0x800a, 0x004b,
            0x0b80, 0x000b, 0x0000, 0x2502, 0x0007, (short) 0x804b,
            0x0b80, 0x000b, 0x0000, 0x2602, 0x0007, 0x004e,
            0x0b80, 0x020c, 0x0300, 0x4801, 0x0007, (short) 0x8043,
            0x0b80, 0x020c, 0x0300, 0x4801, (short) 0x8007, (short) 0x803d,
            (short) 0xffff
    };
    public static final short[] level_data_04 = new short[] {
            0x0780, 0x0209, 0x0300, 0x4c01, 0x0004, 0x0010,
            0x0780, 0x0209, 0x0300, 0x4c01, 0x4004, 0x0020,
            0x0780, 0x0309, 0x0300, 0x4d01, (short) 0xe003, 0x0030,
            0x0780, 0x0309, 0x0300, 0x4d01, (short) 0x8003, 0x0040,
            0x0780, 0x0209, 0x0300, 0x4c01, (short) 0x8004, 0x0018,
            0x0780, 0x0309, 0x0300, 0x4d01, (short) 0xc003, 0x0028,
            0x0780, 0x000b, 0x0300, 0x5601, (short) 0x8004, 0x0008,
            0x0780, 0x000b, 0x0300, 0x5601, (short) 0x8004, 0x0038,
            0x0780, 0x000b, 0x0300, 0x5501, (short) 0x8004, 0x0048,
            0x0980, 0x0509, 0x0f00, 0x0f01, 0x4005, 0x4007,
            0x0980, 0x0509, 0x0f00, 0x0f01, 0x4005, 0x4037,
            0x0b80, 0x030a, 0x0000, 0x1302, (short) 0x8006, 0x0040,
            0x0b80, 0x110a, 0x0000, 0x1502, (short) 0x8008, (short) 0x8048,
            0x0b80, 0x110a, 0x0000, 0x1502, (short) 0x8008, (short) 0x8049,
            0x0b80, 0x000b, 0x0000, (short) 0xf601, 0x0007, (short) 0x8003,
            0x0b80, 0x000b, 0x0000, (short) 0xf701, 0x0007, 0x0005,
            0x0b80, 0x000b, 0x0000, (short) 0xf901, 0x0007, (short) 0x8008,
            0x0b80, 0x000b, 0x0000, (short) 0xf901, 0x0007, 0x0010,
            0x0b80, 0x000b, 0x0000, (short) 0xfa01, 0x0007, (short) 0x8013,
            0x0b80, 0x000b, 0x0000, (short) 0xf801, (short) 0x800b, (short) 0x800b,
            0x0b80, 0x000b, 0x0000, 0x0002, (short) 0x800b, (short) 0x801a,
            0x0b80, 0x000b, 0x0000, (short) 0xf901, 0x0007, (short) 0x8017,
            0x0b80, 0x000b, 0x0000, (short) 0xfa01, 0x0007, 0x001b,
            0x0b80, 0x000b, 0x0000, (short) 0xf801, (short) 0x800b, 0x0013,
            0x0b80, 0x000b, 0x0000, 0x4202, (short) 0x800b, 0x0016,
            0x0b80, 0x000b, 0x0000, (short) 0xfb01, (short) 0x8007, (short) 0x8020,
            0x0b80, 0x000b, 0x0000, (short) 0xf601, 0x0007, (short) 0x8023,
            0x0b80, 0x000b, 0x0000, 0x4202, (short) 0x800b, (short) 0x800e,
            0x0b80, 0x000b, 0x0000, 0x4302, (short) 0x800b, (short) 0x801d,
            0x0b80, 0x000b, 0x0000, (short) 0xf701, 0x0007, 0x0025,
            0x0b80, 0x000b, 0x0000, (short) 0xfd01, (short) 0x8006, 0x003f,
            0x0b80, 0x000b, 0x0000, (short) 0xfe01, 0x0007, 0x0046,
            0x0b80, 0x000b, 0x0000, (short) 0xff01, (short) 0x8007, (short) 0x8049,
            0x0b80, 0x000b, 0x0000, (short) 0xfc01, (short) 0x8009, 0x0042,
            (short) 0xffff
    };
    public static final short[] level_data_05 = new short[] {
            0x0480, 0x1008, 0x0300, 0x5701, 0x0001, 0x0010,
            0x0480, 0x1008, 0x0300, 0x5701, 0x0001, 0x002b,
            0x0780, 0x0009, 0x0300, 0x4a01, 0x0004, 0x0020,
            0x0780, 0x1208, 0x0300, 0x5d01, 0x0004, 0x0030,
            0x0780, 0x0209, 0x0300, 0x4c01, 0x0004, 0x0038,
            0x0780, 0x0309, 0x0300, 0x4d01, 0x0004, 0x0048,
            0x0980, 0x1108, 0x0300, 0x5a01, (short) 0xc005, 0x0018,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0xc005, 0x0028,
            0x0b80, 0x020a, 0x0000, 0x6401, (short) 0x8006, 0x0004,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, (short) 0x8006, (short) 0x8002,
            0x0b80, 0x020a, 0x0000, 0x6401, (short) 0x8006, 0x0017,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, (short) 0x8006, (short) 0x8015,
            0x0b80, 0x020a, 0x0000, 0x6401, 0x0007, 0x0034,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, 0x0007, (short) 0x8032,
            0x0b80, 0x020a, 0x0000, 0x6401, (short) 0x8006, (short) 0x803e,
            0x0c80, 0x010b, 0x0000, (short) 0xf201, (short) 0x8006, (short) 0x803d,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x0008,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x000b,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x001b,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, 0x001e,
            0x0b80, 0x100a, 0x0000, 0x6001, (short) 0x8007, 0x0038,
            0x0b80, 0x100a, 0x0000, 0x6001, (short) 0x8007, 0x003b,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, (short) 0x8042,
            0x0b80, 0x100a, 0x0000, 0x6001, 0x0007, (short) 0x8045,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, (short) 0x800b, (short) 0x8007,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, (short) 0x800b, (short) 0x801a,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, 0x000c, (short) 0x8037,
            0x0c80, 0x000b, 0x0000, (short) 0xf101, (short) 0x800b, 0x0042,
            0x0c80, (short) 0xd04b, 0x0000, (short) 0xf301, (short) 0x8006, (short) 0x8009,
            0x0c80, (short) 0xd04b, 0x0000, (short) 0xf301, (short) 0x8006, (short) 0x801c,
            0x0c80, (short) 0xd04b, 0x0000, (short) 0xf301, (short) 0x8006, 0x0044,
            0x0c80, 0x030b, 0x0000, (short) 0xf401, 0x0008, 0x0028,
            0x0c80, 0x030b, 0x0000, (short) 0xf401, 0x0008, (short) 0x804b,
            0x0c00, 0x040b, 0x0000, (short) 0xf501, 0x0008, (short) 0x8026,
            (short) 0xffff
    };
    public static final short[] level_data_06 = new short[] {
            0x0000, 0x1008, 0x0300, 0x5701, 0x0001, 0x0010,
            0x0000, 0x1008, 0x0300, 0x5701, 0x0001, 0x002b,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0700, 0x0009, 0x0300, 0x4a01, 0x0004, 0x0020,
            0x0700, 0x1208, 0x0300, 0x5d01, 0x0004, 0x0030,
            0x0700, 0x0209, 0x0300, 0x4c01, 0x0004, 0x0038,
            0x0700, 0x0309, 0x0300, 0x4d01, 0x0004, 0x0048,
            0x0900, 0x1108, 0x0300, 0x5a01, (short) 0xc005, 0x0018,
            0x0900, 0x0109, 0x0300, 0x4b01, (short) 0xc005, 0x0028,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0980, (short) 0xdb4c, 0x0000, 0x3202, 0x0006, 0x0004,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            0x0000, 0x000b, 0x0000, 0x0000, 0x0018, 0x0000,
            (short) 0xffff
    };
    public static final short[] level_data_07 = new short[] {
            0x0480, 0x000b, 0x0300, 0x4501, 0x0001, 0x0001,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0780, 0x0109, 0x0300, 0x4a01, 0x0004, 0x0004,
            0x0780, 0x0009, 0x0300, 0x4a01, 0x0004, 0x000d,
            0x0780, 0x000c, 0x0500, 0x7b01, 0x000c, 0x0005,
            0x0780, 0x000c, 0x0540, 0x7b01, 0x000c, 0x0005,
            0x0780, 0x010c, 0x0300, 0x4601, 0x0005, 0x0005,
            0x0780, 0x000c, 0x0500, 0x7b01, (short) 0x800b, (short) 0xc00d,
            0x0780, 0x000c, 0x0540, 0x7b01, (short) 0x800b, (short) 0xc00d,
            0x0780, 0x010c, 0x0300, 0x4601, (short) 0x8004, (short) 0xc00d,
            0x0900, 0x0109, 0x0340, 0x4b01, 0x2006, 0x400c,
            0x0780, 0x020c, 0x0300, 0x4801, (short) 0x8007, 0x0008,
            0x0780, 0x020c, 0x0300, 0x4801, 0x4007, (short) 0xc00b,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0xc006, (short) 0x8007,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0x8007, (short) 0x8008,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0xc006, (short) 0x800c,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            (short) 0xffff
    };
    public static final short[] level_data_08 = new short[] {
            (short) 0xffff
    };
    public static final short[] level_data_09 = new short[] {
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0780, 0x0109, 0x0300, 0x4a01, (short) 0x8003, (short) 0x8003,
            0x0780, 0x0009, 0x0300, 0x4a01, 0x0004, (short) 0x800e,
            0x0780, 0x000c, 0x0500, 0x7b01, 0x000c, 0x0005,
            0x0780, 0x000c, 0x0540, 0x7b01, 0x000c, 0x0005,
            0x0780, 0x010c, 0x0300, 0x4601, 0x0005, 0x0005,
            0x0780, 0x000c, 0x0500, 0x7b01, (short) 0x800b, (short) 0xc00d,
            0x0780, 0x000c, 0x0540, 0x7b01, (short) 0x800b, (short) 0xc00d,
            0x0780, 0x010c, 0x0300, 0x4601, (short) 0x8004, (short) 0xc00d,
            0x0900, 0x0109, 0x0340, 0x4b01, 0x2006, 0x400c,
            0x0780, 0x020c, 0x0300, 0x4801, (short) 0x8007, 0x0008,
            0x0780, 0x020c, 0x0300, 0x4801, 0x4007, (short) 0xc00b,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0xc006, (short) 0x8007,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0x8007, (short) 0x8008,
            0x0980, 0x0109, 0x0300, 0x4b01, (short) 0xc006, (short) 0x800c,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, (short) 0xf001, 0x0000, 0x0000,
            (short) 0xffff
    };
    public static final short[][] level_data_lookup = new short[][] {
            level_data_00,
            level_data_01,
            level_data_02,
            level_data_03,
            level_data_04,
            level_data_05,
            level_data_06,
            level_data_07,
            level_data_08,
            level_data_09
    };

    public static byte cchip_r(int offset) {
        return cchip_ram[offset];
    }

    public static void cchip_w(int offset, byte data) {
        cchip_ram[offset] = data;
    }

    public static byte opwolf_in_r(int offset) {
        byte result = 0;
        if (offset == 0) {
            result = sbyte0;
        } else if (offset == 1) {
            result = sbyte1;
        }
        return result;
    }

    public static byte opwolf_dsw_r(int offset) {
        byte result = 0;
        if (offset == 0) {
            result = dswa;
        } else if (offset == 1) {
            result = dswb;
        }
        return result;
    }

    public static short opwolf_lightgun_r(int offset) {
        short result = 0;
        if (offset == 0) {
            result = (short) (0xfe00 | opwolf_gun_x_r());
        } else if (offset == 1) {
            result = (short) (0xfe00 | opwolf_gun_y_r());
        }
        return result;
    }

    public static short opwolf_lightgun_r_p(int offset) {
        short result = 0;
        if (offset == 0) {
            result = (short) ((sbyte2 << 8) | opwolf_gun_x_r());
        } else if (offset == 1) {
            result = (short) ((sbyte3 << 8) | opwolf_gun_y_r());
        }
        return result;
    }

    public static byte z80_input1_r() {
        byte result;
        result = sbyte0;
        return result;
    }

    public static byte z80_input2_r() {
        byte result;
        result = sbyte0;
        return result;
    }

    public static void sound_bankswitch_w(int offset, byte data) {
        basebanksnd = 0x1_0000 + 0x4000 * ((data - 1) & 0x03);
    }

    public static void machine_reset_opwolf() {
        adpcm_b[0] = adpcm_b[1] = 0;
        adpcm_c[0] = adpcm_c[1] = 0;
        adpcm_pos[0] = adpcm_pos[1] = 0;
        adpcm_end[0] = adpcm_end[1] = 0;
        adpcm_data[0] = adpcm_data[1] = -1;
        m_sprite_ctrl = 0;
        m_sprites_flipscreen = 0;
        MSM5205.msm5205_reset_w(0, 1);
        MSM5205.msm5205_reset_w(1, 1);
    }

    public static void opwolf_msm5205_vck(int chip) {
        if (adpcm_data[chip] != -1) {
            MSM5205.msm5205_data_w(chip, adpcm_data[chip] & 0x0f);
            adpcm_data[chip] = -1;
            if (adpcm_pos[chip] == adpcm_end[chip]) {
                MSM5205.msm5205_reset_w(chip, 1);
            }
        } else {
            adpcm_data[chip] = adpcmrom[adpcm_pos[chip]];
            adpcm_pos[chip] = (adpcm_pos[chip] + 1) & 0x7_ffff;
            MSM5205.msm5205_data_w(chip, adpcm_data[chip] >> 4);
        }
    }

    public static void opwolf_adpcm_b_w(int offset, byte data) {
        int start;
        int end;
        adpcm_b[offset] = data;
        if (offset == 0x04) { //trigger ?
            start = adpcm_b[0] + adpcm_b[1] * 256;
            end = adpcm_b[2] + adpcm_b[3] * 256;
            start *= 16;
            end *= 16;
            adpcm_pos[0] = start;
            adpcm_end[0] = end;
            MSM5205.msm5205_reset_w(0, 0);
        }
    }

    public static void opwolf_adpcm_c_w(int offset, byte data) {
        int start;
        int end;
        adpcm_c[offset] = data;
        if (offset == 0x04) { //trigger ?
            start = adpcm_c[0] + adpcm_c[1] * 256;
            end = adpcm_c[2] + adpcm_c[3] * 256;
            start *= 16;
            end *= 16;
            adpcm_pos[1] = start;
            adpcm_end[1] = end;
            MSM5205.msm5205_reset_w(1, 0);
        }
    }

    public static void opwolf_adpcm_d_w() {

    }

    public static void opwolf_adpcm_e_w() {

    }

    public static int opwolf_gun_x_r() {
        p1x = (byte) Inptport.input_port_read_direct(Inptport.analog_p1x);
        int scaled = (p1x * 320) / 256;
        return (scaled + 0x15 + opwolf_gun_xoffs);
    }

    public static int opwolf_gun_y_r() {
        p1y = (byte) Inptport.input_port_read_direct(Inptport.analog_p1y);
        return (p1y - 0x24 + opwolf_gun_yoffs);
    }

    public static void irq_handler(int irq) {
        Cpuint.cpunum_set_input_line(1, 0, irq != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void opwolf_timer_callback() {
        if (current_cmd == (byte) 0xf5) {
            int level = cchip_ram[0x1b] % 10;
            int i = 0;
            Arrays.fill(cchip_ram, 0x200, 0x200 + 0x200, (byte) 0);
            for (i = 0; (i < 0x200) && (level_data_lookup[level][i] != (short) 0xffff); i += 3) {
                cchip_ram[0x200 + i * 2 + 0] = (byte) (level_data_lookup[level][i] >> 8);
                cchip_ram[0x200 + i * 2 + 1] = (byte) (level_data_lookup[level][i] & 0xff);
                cchip_ram[0x200 + i * 2 + 2] = (byte) (level_data_lookup[level][i + 1] >> 8);
                cchip_ram[0x200 + i * 2 + 3] = (byte) (level_data_lookup[level][i + 1] & 0xff);
                cchip_ram[0x200 + i * 2 + 4] = (byte) (level_data_lookup[level][i + 2] >> 8);
                cchip_ram[0x200 + i * 2 + 5] = (byte) (level_data_lookup[level][i + 2] & 0xff);
            }
            // The bootleg cchip writes 0 to these locations - hard to tell what the real one writes
            cchip_ram[0x0] = 0;
            cchip_ram[0x76] = 0;
            cchip_ram[0x75] = 0;
            cchip_ram[0x74] = 0;
            cchip_ram[0x72] = 0;
            cchip_ram[0x71] = 0;
//            cchip_ram[0x70] = 0;
            cchip_ram[0x66] = 0;
            cchip_ram[0x2b] = 0;
            cchip_ram[0x30] = 0;
            cchip_ram[0x31] = 0;
            cchip_ram[0x32] = 0;
            cchip_ram[0x27] = 0;
            c588 = 0;
            c589 = 0;
            c58a = 0;
            m_triggeredLevel1b = 0;
            m_triggeredLevel13b = 0;
            m_triggeredLevel2 = 0;
            m_triggeredLevel2b = 0;
            m_triggeredLevel2c = 0;
            m_triggeredLevel3b = 0;
            m_triggeredLevel4 = 0;
            m_triggeredLevel5 = 0;
            m_triggeredLevel7 = 0;
            m_triggeredLevel8 = 0;
            m_triggeredLevel9 = 0;
            cchip_ram[0x1a] = 0;
            cchip_ram[0x7a] = 1; // Signal command complete
        }
        current_cmd = 0;
    }

    public static void updateDifficulty(int mode) {
        // The game is made up of 6 rounds, when you complete the
        // sixth you return to the start but with harder difficulty.
        if (mode == 0) {
            switch (cchip_ram[0x15] & 3) { // Dipswitch B
                case 3:
                    cchip_ram[0x2c] = 0x31;
                    cchip_ram[0x77] = 0x05;
                    cchip_ram[0x25] = 0x0f;
                    cchip_ram[0x26] = 0x0b;
                    break;
                case 0:
                    cchip_ram[0x2c] = 0x20;
                    cchip_ram[0x77] = 0x06;
                    cchip_ram[0x25] = 0x07;
                    cchip_ram[0x26] = 0x03;
                    break;
                case 1:
                    cchip_ram[0x2c] = 0x31;
                    cchip_ram[0x77] = 0x05;
                    cchip_ram[0x25] = 0x0f;
                    cchip_ram[0x26] = 0x0b;
                    break;
                case 2:
                    cchip_ram[0x2c] = 0x3c;
                    cchip_ram[0x77] = 0x04;
                    cchip_ram[0x25] = 0x13;
                    cchip_ram[0x26] = 0x0f;
                    break;
            }
        } else {
            switch (cchip_ram[0x15] & 3) { // Dipswitch B
                case 3:
                    cchip_ram[0x2c] = 0x46;
                    cchip_ram[0x77] = 0x05;
                    cchip_ram[0x25] = 0x11;
                    cchip_ram[0x26] = 0x0e;
                    break;
                case 0:
                    cchip_ram[0x2c] = 0x30;
                    cchip_ram[0x77] = 0x06;
                    cchip_ram[0x25] = 0x0b;
                    cchip_ram[0x26] = 0x03;
                    break;
                case 1:
                    cchip_ram[0x2c] = 0x3a;
                    cchip_ram[0x77] = 0x05;
                    cchip_ram[0x25] = 0x0f;
                    cchip_ram[0x26] = 0x09;
                    break;
                case 2:
                    cchip_ram[0x2c] = 0x4c;
                    cchip_ram[0x77] = 0x04;
                    cchip_ram[0x25] = 0x19;
                    cchip_ram[0x26] = 0x11;
                    break;
            }
        }
    }

    public static void opwolf_cchip_status_w() {
        cchip_ram[0x3d] = 1;
        cchip_ram[0x7a] = 1;
        updateDifficulty(0);
    }

    public static void opwolf_cchip_bank_w(byte data) {
        current_bank = (byte) (data & 7);
    }

    public static void opwolf_cchip_data_w(int offset, short data) {
        cchip_ram[(current_bank * 0x400) + offset] = (byte) (data & 0xff);
        if (current_bank == 0) {
            // Dip switch A is written here by the 68k - precalculate the coinage values
            // Shouldn't we directly read the values from the ROM area ?
            if (offset == 0x14) {
                int[] coin_table = new int[] {0, 0};
                byte[] coin_offset = new byte[2];
                int slot;

                if ((opwolf_region == 1) || (opwolf_region == 2)) {
                    coin_table[0] = 0x03_ffce;
                    coin_table[1] = 0x03_ffce;
                }
                if ((opwolf_region == 3) || (opwolf_region == 4)) {
                    coin_table[0] = 0x03_ffde;
                    coin_table[1] = 0x03_ffee;
                }
                coin_offset[0] = (byte) (12 - (4 * ((data & 0x30) >> 4)));
                coin_offset[1] = (byte) (12 - (4 * ((data & 0xc0) >> 6)));
                for (slot = 0; slot < 2; slot++) {
                    if (coin_table[slot] != 0) {
                        cchip_coins_for_credit[slot] = (byte) ((Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 0) / 2 * 2] * 0x100 + Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 0) / 2 * 2 + 1]) & 0xff);
                        cchip_credits_for_coin[slot] = (byte) ((Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 2) / 2 * 2] * 0x100 + Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 2) / 2 * 2 + 1]) & 0xff);
                    }
                }
            }
            // Dip switch B
            if (offset == 0x15) {
                updateDifficulty(0);
            }
        }
    }

    public static void opwolf_cchip_data_w2(int offset, byte data) {
        cchip_ram[(current_bank * 0x400) + offset] = (byte) (data & 0xff);
        if (current_bank == 0) {
            // Dip switch A is written here by the 68k - precalculate the coinage values
            // Shouldn't we directly read the values from the ROM area ?
            if (offset == 0x14) {
                int[] coin_table = new int[] {0, 0};
                byte[] coin_offset = new byte[2];
                int slot;

                if ((opwolf_region == 1) || (opwolf_region == 2)) {
                    coin_table[0] = 0x03_ffce;
                    coin_table[1] = 0x03_ffce;
                }
                if ((opwolf_region == 3) || (opwolf_region == 4)) {
                    coin_table[0] = 0x03_ffde;
                    coin_table[1] = 0x03_ffee;
                }
                coin_offset[0] = (byte) (12 - (4 * ((data & 0x30) >> 4)));
                coin_offset[1] = (byte) (12 - (4 * ((data & 0xc0) >> 6)));
                for (slot = 0; slot < 2; slot++) {
                    if (coin_table[slot] != 0) {
                        cchip_coins_for_credit[slot] = (byte) ((Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 0) / 2 * 2] * 0x100 + Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 0) / 2 * 2 + 1]) & 0xff);
                        cchip_credits_for_coin[slot] = (byte) ((Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 2) / 2 * 2] * 0x100 + Memory.mainrom[(coin_table[slot] + coin_offset[slot] + 2) / 2 * 2 + 1]) & 0xff);
                    }
                }
            }
            // Dip switch B
            if (offset == 0x15) {
                updateDifficulty(0);
            }
        }
    }

    public static short opwolf_cchip_status_r() {
        return 0x1;
    }

    public static short opwolf_cchip_data_r(int offset) {
        return cchip_ram[(current_bank * 0x400) + offset];
    }

    public static void cchip_timer() {
        cchip_ram[0x4] = sbyte0;
        cchip_ram[0x5] = sbyte1;
        if (cchip_ram[0x4] != cchip_last_04) {
            int slot = -1;
            if ((cchip_ram[0x4] & 1) != 0) {
                slot = 0;
            }
            if ((cchip_ram[0x4] & 2) != 0) {
                slot = 1;
            }
            if (slot != -1) {
                cchip_coins[slot]++;
                if (cchip_coins[slot] >= cchip_coins_for_credit[slot]) {
                    cchip_ram[0x53] += cchip_credits_for_coin[slot];
                    cchip_ram[0x51] = 0x55;
                    cchip_ram[0x52] = 0x55;
                    cchip_coins[slot] -= cchip_coins_for_credit[slot];
                }
                Generic.coin_counter_w(slot, 1);
            }
            if (cchip_ram[0x53] > 9) {
                cchip_ram[0x53] = 9;
            }
        }
        cchip_last_04 = cchip_ram[0x4];
        if (cchip_ram[0x5] != cchip_last_05) {
            if ((cchip_ram[0x5] & 4) == 0) {
                cchip_ram[0x53]++;
                cchip_ram[0x51] = 0x55;
                cchip_ram[0x52] = 0x55;
            }
        }
        cchip_last_05 = cchip_ram[0x5];
        Generic.coin_lockout_w(1, cchip_ram[0x53] == 9 ? 1 : 0);
        Generic.coin_lockout_w(0, cchip_ram[0x53] == 9 ? 1 : 0);
        Generic.coin_counter_w(0, 0);
        Generic.coin_counter_w(1, 0);
        if (cchip_ram[0x34] < 2) {
            updateDifficulty(0);
            cchip_ram[0x76] = 0;
            cchip_ram[0x75] = 0;
            cchip_ram[0x74] = 0;
            cchip_ram[0x72] = 0;
            cchip_ram[0x71] = 0;
            cchip_ram[0x70] = 0;
            cchip_ram[0x66] = 0;
            cchip_ram[0x2b] = 0;
            cchip_ram[0x30] = 0;
            cchip_ram[0x31] = 0;
            cchip_ram[0x32] = 0;
            cchip_ram[0x27] = 0;
            c588 = 0;
            c589 = 0;
            c58a = 0;
        }
        if (cchip_ram[0x1c] == 0 && cchip_ram[0x1d] == 0 && cchip_ram[0x1e] == 0 && cchip_ram[0x1f] == 0 && cchip_ram[0x20] == 0) {
            if (cchip_ram[0x1b] == 0x6) {
                if (cchip_ram[0x27] == 0x1)
                    cchip_ram[0x32] = 1;
            } else if (cchip_ram[0x1b] == 0x2) {
                if (m_triggeredLevel2 == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f)
                {
                    cchip_ram[0x5f] = 4; // 0xBE at 68K side
                    m_triggeredLevel2 = 1;
                }
                if (m_triggeredLevel2 != 0 && cchip_ram[0x5d] != 0) {
                    cchip_ram[0x32] = 1;
                    cchip_ram[0x5d] = 0; // acknowledge 68K command
                }
            } else if (cchip_ram[0x1b] == 0x4) {
                cchip_ram[0x32] = 1;
                if (m_triggeredLevel4 == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f))
                {
                    cchip_ram[0x5f] = 10;
                    m_triggeredLevel4 = 1;
                }
            } else {
                cchip_ram[0x32] = 1;
            }
        }
        if (cchip_ram[0x1c] == 0 && cchip_ram[0x1d] == 0) {
            if (cchip_ram[0x1b] == 0x1 && m_triggeredLevel1b == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f))
            {
                cchip_ram[0x5f] = 7;
                m_triggeredLevel1b = 1;
            }
            if (cchip_ram[0x1b] == 0x3 && m_triggeredLevel3b == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f))
            {
                cchip_ram[0x5f] = 8;
                m_triggeredLevel3b = 1;
            }
            if ((cchip_ram[0x1b] != 0x1 && cchip_ram[0x1b] != 0x3) && m_triggeredLevel13b == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f))
            {
                cchip_ram[0x5f] = 9;
                m_triggeredLevel13b = 1;
            }
        }
        if (cchip_ram[0x1b] == 0x2) {
            int numMen = (cchip_ram[0x1d] << 8) + cchip_ram[0x1c];
            if (numMen < 0x25 && m_triggeredLevel2b == 1 && m_triggeredLevel2c == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f))
            {
                cchip_ram[0x5f] = 6;
                m_triggeredLevel2c = 1;
            }
            if (numMen < 0x45 && m_triggeredLevel2b == 0 && cchip_ram[0x5f] == 0) // Don't write unless 68K is ready (0 at 0x5f))
            {
                cchip_ram[0x5f] = 5;
                m_triggeredLevel2b = 1;
            }
        }
        if (cchip_ram[0x1b] == 0x5) {
            if (cchip_ram[0x1c] == 0 && cchip_ram[0x1d] == 0 && m_triggeredLevel5 == 0) {
                cchip_ram[0x2f] = 1;
                m_triggeredLevel5 = 1;
            }
        }
        if (cchip_ram[0x1b] == 0x6) {
            if (c58a == 0) {
                if ((cchip_ram[0x72] & 0x7f) >= 8 && cchip_ram[0x74] == 0 && cchip_ram[0x1c] == 0 && cchip_ram[0x1d] == 0 && cchip_ram[0x1f] == 0) {
                    cchip_ram[0x30] = 1;
                    cchip_ram[0x74] = 1;
                    c58a = 1;
                }
            }
            if (cchip_ram[0x1a] == (byte) 0x90) {
                cchip_ram[0x74] = 0;
            }
            if (c58a != 0) {
                if (c589 == 0 && cchip_ram[0x27] == 0 && cchip_ram[0x75] == 0 && cchip_ram[0x1c] == 0 && cchip_ram[0x1d] == 0 && cchip_ram[0x1e] == 0 && cchip_ram[0x1f] == 0) {
                    cchip_ram[0x31] = 1;
                    cchip_ram[0x75] = 1;
                    c589 = 1;
                }
            }
            if (cchip_ram[0x2b] == 0x1) {
                cchip_ram[0x2b] = 0;
                if (cchip_ram[0x30] == 0x1) {
                    if (cchip_ram[0x1a] != (byte) 0x90) {
                        cchip_ram[0x1a]--;
                    }
                }
                if (cchip_ram[0x72] == 0x9) {
                    if (cchip_ram[0x76] != 0x4) {
                        cchip_ram[0x76] = 3;
                    }
                } else {
                    c588 |= (byte) 0x80;
                    cchip_ram[0x72] = c588;
                    c588++;
                    cchip_ram[0x1a]--;
                    cchip_ram[0x1a]--;
                    cchip_ram[0x1a]--;
                }
            }
            if (cchip_ram[0x76] == 0) {
                cchip_ram[0x76] = 1;
                updateDifficulty(1);
            }
        }
        if (cchip_ram[0x1b] == 0x7 && m_triggeredLevel7 == 0 && cchip_ram[0x5f] == 0) { // Don't write unless 68K is ready (0 at 0x5f))
            m_triggeredLevel7 = 1;
            cchip_ram[0x5f] = 1;
        }
        if (cchip_ram[0x1b] == 0x8 && m_triggeredLevel8 == 0 && cchip_ram[0x5f] == 0) { // Don't write unless 68K is ready (0 at 0x5f))
            m_triggeredLevel8 = 1;
            cchip_ram[0x5f] = 2;
        }
        if (cchip_ram[0x1b] == 0x9 && m_triggeredLevel9 == 0 && cchip_ram[0x5f] == 0) { // Don't write unless 68K is ready (0 at 0x5f))
            m_triggeredLevel9 = 1;
            cchip_ram[0x5f] = 3;
        }
        if (cchip_ram[0xe] == 1) {
            cchip_ram[0xe] = (byte) 0xfd;
            cchip_ram[0x61] = 0x04;
        }
        if (cchip_ram[0x7a] == 0 && cchip_last_7a != 0 && current_cmd != (byte) 0xf5) {
            current_cmd = (byte) 0xf5;
            EmuTimer timer = Timer.allocCommon(Taito::opwolf_timer_callback, "opwolf_timer_callback", true);
            Timer.adjustPeriodic(timer, new Atime(0, 80000 * Cpuexec.cpu[0].attoseconds_per_cycle), Attotime.ATTOTIME_NEVER);
        }
        cchip_last_7a = cchip_ram[0x7a];
        if (cchip_ram[0x7f] == 0xa) {
            cchip_ram[0xfe] = (byte) 0xf7;
            cchip_ram[0xff] = 0x6e;
        }
        cchip_ram[0x64] = 0;
        cchip_ram[0x66] = 0;
    }

    public static void opwolf_cchip_init() {
        m_triggeredLevel1b = 0;
        m_triggeredLevel2 = 0;
        m_triggeredLevel2b = 0;
        m_triggeredLevel2c = 0;
        m_triggeredLevel3b = 0;
        m_triggeredLevel13b = 0;
        m_triggeredLevel4 = 0;
        m_triggeredLevel5 = 0;
        m_triggeredLevel7 = 0;
        m_triggeredLevel8 = 0;
        m_triggeredLevel9 = 0;
        current_bank = 0;
        current_cmd = 0;
        cchip_last_7a = 0;
        cchip_last_04 = (byte) 0xfc;
        cchip_last_05 = (byte) 0xff;
        c588 = 0;
        c589 = 0;
        c58a = 0;
        cchip_coins[0] = 0;
        cchip_coins[1] = 0;
        cchip_coins_for_credit[0] = 1;
        cchip_credits_for_coin[0] = 1;
        cchip_coins_for_credit[1] = 1;
        cchip_credits_for_coin[1] = 1;
        Timer.pulseInternal(new Atime(0, (long) (1e18 / 60)), Taito::cchip_timer, "cchip_timer");
    }

//#endregion

//#region State

    public static void SaveStateBinary_tokio(BinaryWriter writer) {
        int i;
        writer.write(dsw0);
        writer.write(dsw1);
        writer.write(basebankmain);
        writer.write(videoram, 0, 0x1d00);
        writer.write(bublbobl_objectram, 0, 0x300);
        writer.write(Generic.paletteram, 0, 0x200);
        writer.write(bublbobl_video_enable);
        writer.write(tokio_prot_count);
        writer.write(sound_nmi_enable);
        writer.write(pending_nmi);
        for (i = 0; i < 0x100; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1800);
        writer.write(Memory.audioram, 0, 0x1000);
        for (i = 0; i < 3; i++) {
            Z80A.zz1[i].SaveStateBinary(writer);
        }
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
        YM2203.FF2203[0].SaveStateBinary(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_tokio(BinaryReader reader) {
        try {
            int i;
            dsw0 = reader.readByte();
            dsw1 = reader.readByte();
            basebankmain = reader.readInt32();
            videoram = reader.readBytes(0x1d00);
            bublbobl_objectram = reader.readBytes(0x300);
            Generic.paletteram = reader.readBytes(0x200);
            bublbobl_video_enable = reader.readInt32();
            tokio_prot_count = reader.readInt32();
            sound_nmi_enable = reader.readInt32();
            pending_nmi = reader.readInt32();
            for (i = 0; i < 0x100; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1800);
            Memory.audioram = reader.readBytes(0x1000);
            for (i = 0; i < 3; i++) {
                Z80A.zz1[i].LoadStateBinary(reader);
            }
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
            YM2203.FF2203[0].LoadStateBinary(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_bublbobl(BinaryWriter writer) {
        int i;
        writer.write(dsw0);
        writer.write(dsw1);
        writer.write(basebankmain);
        writer.write(videoram, 0, 0x1d00);
        writer.write(bublbobl_objectram, 0, 0x300);
        writer.write(Generic.paletteram, 0, 0x200);
        writer.write(bublbobl_mcu_sharedram, 0, 0x400);
        writer.write(bublbobl_video_enable);
        writer.write(ddr1);
        writer.write(ddr2);
        writer.write(ddr3);
        writer.write(ddr4);
        writer.write(port1_in);
        writer.write(port2_in);
        writer.write(port3_in);
        writer.write(port4_in);
        writer.write(port1_out);
        writer.write(port2_out);
        writer.write(port3_out);
        writer.write(port4_out);
        for (i = 0; i < 0x100; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1800);
        writer.write(Memory.audioram, 0, 0x1000);
        writer.write(mcuram, 0, 0xc0);
        for (i = 0; i < 3; i++) {
            Z80A.zz1[i].SaveStateBinary(writer);
        }
        M6800.m1.SaveStateBinary(writer);
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
        YM2203.FF2203[0].SaveStateBinary(writer);
        YM3812.SaveStateBinary_YM3526(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_base_sampindex);
        writer.write(Sound.ym3526stream.output_sampindex);
        writer.write(Sound.ym3526stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_bublbobl(BinaryReader reader) {
        try {
            int i;
            dsw0 = reader.readByte();
            dsw1 = reader.readByte();
            basebankmain = reader.readInt32();
            videoram = reader.readBytes(0x1d00);
            bublbobl_objectram = reader.readBytes(0x300);
            Generic.paletteram = reader.readBytes(0x200);
            bublbobl_mcu_sharedram = reader.readBytes(0x400);
            bublbobl_video_enable = reader.readInt32();
            ddr1 = reader.readByte();
            ddr2 = reader.readByte();
            ddr3 = reader.readByte();
            ddr4 = reader.readByte();
            port1_in = reader.readByte();
            port2_in = reader.readByte();
            port3_in = reader.readByte();
            port4_in = reader.readByte();
            port1_out = reader.readByte();
            port2_out = reader.readByte();
            port3_out = reader.readByte();
            port4_out = reader.readByte();
            for (i = 0; i < 0x100; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1800);
            Memory.audioram = reader.readBytes(0x1000);
            mcuram = reader.readBytes(0xc0);
            for (i = 0; i < 3; i++) {
                Z80A.zz1[i].LoadStateBinary(reader);
            }
            M6800.m1.LoadStateBinary(reader);
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
            YM2203.FF2203[0].LoadStateBinary(reader);
            YM3812.LoadStateBinary_YM3526(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_base_sampindex = reader.readInt32();
            Sound.ym3526stream.output_sampindex = reader.readInt32();
            Sound.ym3526stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_boblbobl(BinaryWriter writer) {
        int i;
        writer.write(dsw0);
        writer.write(dsw1);
        writer.write(basebankmain);
        writer.write(videoram, 0, 0x1d00);
        writer.write(bublbobl_objectram, 0, 0x300);
        writer.write(Generic.paletteram, 0, 0x200);
        writer.write(bublbobl_video_enable);
        writer.write(ic43_a);
        writer.write(ic43_b);
        for (i = 0; i < 0x100; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1800);
        writer.write(mainram2, 0, 0x100);
        writer.write(mainram3, 0, 0x100);
        writer.write(Memory.audioram, 0, 0x1000);
        writer.write(mcuram, 0, 0xc0);
        for (i = 0; i < 3; i++) {
            Z80A.zz1[i].SaveStateBinary(writer);
        }
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
        YM2203.FF2203[0].SaveStateBinary(writer);
        YM3812.SaveStateBinary_YM3526(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_base_sampindex);
        writer.write(Sound.ym3526stream.output_sampindex);
        writer.write(Sound.ym3526stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_boblbobl(BinaryReader reader) {
        try {
            int i;
            dsw0 = reader.readByte();
            dsw1 = reader.readByte();
            basebankmain = reader.readInt32();
            videoram = reader.readBytes(0x1d00);
            bublbobl_objectram = reader.readBytes(0x300);
            Generic.paletteram = reader.readBytes(0x200);
            bublbobl_video_enable = reader.readInt32();
            ic43_a = reader.readInt32();
            ic43_b = reader.readInt32();
            for (i = 0; i < 0x100; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1800);
            mainram2 = reader.readBytes(0x100);
            mainram3 = reader.readBytes(0x100);
            Memory.audioram = reader.readBytes(0x1000);
            for (i = 0; i < 3; i++) {
                Z80A.zz1[i].LoadStateBinary(reader);
            }
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
            YM2203.FF2203[0].LoadStateBinary(reader);
            YM3812.LoadStateBinary_YM3526(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_base_sampindex = reader.readInt32();
            Sound.ym3526stream.output_sampindex = reader.readInt32();
            Sound.ym3526stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_bub68705(BinaryWriter writer) {
        int i;
        writer.write(dsw0);
        writer.write(dsw1);
        writer.write(basebankmain);
        writer.write(videoram, 0, 0x1d00);
        writer.write(bublbobl_objectram, 0, 0x300);
        writer.write(Generic.paletteram, 0, 0x200);
        writer.write(bublbobl_mcu_sharedram, 0, 0x400);
        writer.write(bublbobl_video_enable);
        writer.write(portA_in);
        writer.write(portA_out);
        writer.write(ddrA);
        writer.write(portB_in);
        writer.write(portB_out);
        writer.write(ddrB);
        for (i = 0; i < 0x100; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1800);
        writer.write(Memory.audioram, 0, 0x1000);
        writer.write(mcuram, 0, 0xc0);
        for (i = 0; i < 3; i++) {
            Z80A.zz1[i].SaveStateBinary(writer);
        }
        M6805.m1.SaveStateBinary(writer);
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
        YM2203.FF2203[0].SaveStateBinary(writer);
        YM3812.SaveStateBinary_YM3526(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_base_sampindex);
        writer.write(Sound.ym3526stream.output_sampindex);
        writer.write(Sound.ym3526stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_bub68705(BinaryReader reader) {
        try {
            int i;
            dsw0 = reader.readByte();
            dsw1 = reader.readByte();
            basebankmain = reader.readInt32();
            videoram = reader.readBytes(0x1d00);
            bublbobl_objectram = reader.readBytes(0x300);
            Generic.paletteram = reader.readBytes(0x200);
            bublbobl_mcu_sharedram = reader.readBytes(0x400);
            bublbobl_video_enable = reader.readInt32();
            portA_in = reader.readByte();
            portA_out = reader.readByte();
            ddrA = reader.readByte();
            portB_in = reader.readByte();
            portB_out = reader.readByte();
            ddrB = reader.readByte();
            for (i = 0; i < 0x100; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1800);
            Memory.audioram = reader.readBytes(0x1000);
            mcuram = reader.readBytes(0xc0);
            for (i = 0; i < 3; i++) {
                Z80A.zz1[i].LoadStateBinary(reader);
            }
            M6805.m1.LoadStateBinary(reader);
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
            YM2203.FF2203[0].LoadStateBinary(reader);
            YM3812.LoadStateBinary_YM3526(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_base_sampindex = reader.readInt32();
            Sound.ym3526stream.output_sampindex = reader.readInt32();
            Sound.ym3526stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_opwolf(BinaryWriter writer) {
        int i;
        writer.write(dswa);
        writer.write(dswb);
        writer.write(Inptport.analog_p1x.accum);
        writer.write(Inptport.analog_p1x.previous);
        writer.write(Inptport.analog_p1x.lastdigital);
        writer.write(Inptport.analog_p1y.accum);
        writer.write(Inptport.analog_p1y.previous);
        writer.write(Inptport.analog_p1y.lastdigital);
        writer.write(Inptport.portdata.last_frame_time.seconds);
        writer.write(Inptport.portdata.last_frame_time.attoseconds);
        writer.write(Inptport.portdata.last_delta_nsec);
        writer.write(basebanksnd);
        writer.write(PC080SN_chips);
        for (i = 0; i < 8; i++) {
            writer.write(PC080SN_ctrl[0][i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(PC080SN_bg_ram_offset[0][i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(PC080SN_bgscroll_ram_offset[0][i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(PC080SN_bgscrollx[0][i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(PC080SN_bgscrolly[0][i]);
        }
        for (i = 0; i < 0x8000; i++) {
            writer.write(PC080SN_ram[0][i]);
        }
        writer.write(PC080SN_xoffs);
        writer.write(PC080SN_yoffs);
        writer.write(PC080SN_yinvert);
        writer.write(PC080SN_dblwidth);
        writer.write(PC090OJ_ctrl);
        writer.write(PC090OJ_buffer);
        writer.write(PC090OJ_gfxnum);
        writer.write(PC090OJ_sprite_ctrl);
        for (i = 0; i < 0x2000; i++) {
            writer.write(PC090OJ_ram[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(PC090OJ_ram_buffered[i]);
        }
        writer.write(PC090OJ_xoffs);
        writer.write(PC090OJ_yoffs);
        writer.write(opwolf_region);
        writer.write(cchip_ram, 0, 0x2000);
        writer.write(adpcm_b, 0, 8);
        writer.write(adpcm_c, 0, 8);
        writer.write(opwolf_gun_xoffs);
        writer.write(opwolf_gun_yoffs);
        for (i = 0; i < 2; i++) {
            writer.write(adpcm_pos[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(adpcm_end[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(adpcm_data[i]);
        }
        writer.write(m_sprite_ctrl);
        writer.write(m_sprites_flipscreen);
        writer.write(current_bank);
        writer.write(current_cmd);
        writer.write(cchip_last_7a);
        writer.write(cchip_last_04);
        writer.write(cchip_last_05);
        writer.write(cchip_coins_for_credit, 0, 2);
        writer.write(cchip_credits_for_coin, 0, 2);
        writer.write(cchip_coins, 0, 2);
        writer.write(c588);
        writer.write(c589);
        writer.write(c58a);
        writer.write(m_triggeredLevel1b);
        writer.write(m_triggeredLevel2);
        writer.write(m_triggeredLevel2b);
        writer.write(m_triggeredLevel2c);
        writer.write(m_triggeredLevel3b);
        writer.write(m_triggeredLevel13b);
        writer.write(m_triggeredLevel4);
        writer.write(m_triggeredLevel5);
        writer.write(m_triggeredLevel7);
        writer.write(m_triggeredLevel8);
        writer.write(m_triggeredLevel9);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x8000);
        writer.write(mainram2, 0, 0x1_0000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x1000);
        for (i = 0; i < Z80A.nZ80; i++) {
            Z80A.zz1[i].SaveStateBinary(writer);
        }
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
        writer.write(Sound.latched_value[0]);
        writer.write(Sound.utempdata[0]);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(MSM5205.mm1[0].voice.stream.output_sampindex);
        writer.write(MSM5205.mm1[0].voice.stream.output_base_sampindex);
        writer.write(MSM5205.mm1[1].voice.stream.output_sampindex);
        writer.write(MSM5205.mm1[1].voice.stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_opwolf(BinaryReader reader) {
        try {
            int i;
            dswa = reader.readByte();
            dswb = reader.readByte();
            Inptport.analog_p1x.accum = reader.readInt32();
            Inptport.analog_p1x.previous = reader.readInt32();
            Inptport.analog_p1x.lastdigital = reader.readByte();
            Inptport.analog_p1y.accum = reader.readInt32();
            Inptport.analog_p1y.previous = reader.readInt32();
            Inptport.analog_p1y.lastdigital = reader.readByte();
            Inptport.portdata.last_frame_time.seconds = reader.readInt32();
            Inptport.portdata.last_frame_time.attoseconds = reader.readInt64();
            Inptport.portdata.last_delta_nsec = reader.readInt64();
            basebanksnd = reader.readInt32();
            PC080SN_chips = reader.readInt32();
            for (i = 0; i < 8; i++) {
                PC080SN_ctrl[0][i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                PC080SN_bg_ram_offset[0][i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                PC080SN_bgscroll_ram_offset[0][i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                PC080SN_bgscrollx[0][i] = reader.readInt32();
            }
            for (i = 0; i < 2; i++) {
                PC080SN_bgscrolly[0][i] = reader.readInt32();
            }
            for (i = 0; i < 0x8000; i++) {
                PC080SN_ram[0][i] = reader.readUInt16();
            }
            PC080SN_xoffs = reader.readInt32();
            PC080SN_yoffs = reader.readInt32();
            PC080SN_yinvert = reader.readInt32();
            PC080SN_dblwidth = reader.readInt32();
            PC090OJ_ctrl = reader.readUInt16();
            PC090OJ_buffer = reader.readUInt16();
            PC090OJ_gfxnum = reader.readUInt16();
            PC090OJ_sprite_ctrl = reader.readUInt16();
            for (i = 0; i < 0x2000; i++) {
                PC090OJ_ram[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                PC090OJ_ram_buffered[i] = reader.readUInt16();
            }
            PC090OJ_xoffs = reader.readInt32();
            PC090OJ_yoffs = reader.readInt32();
            opwolf_region = reader.readInt32();
            cchip_ram = reader.readBytes(0x2000);
            adpcm_b = reader.readBytes(8);
            adpcm_c = reader.readBytes(8);
            opwolf_gun_xoffs = reader.readInt32();
            opwolf_gun_yoffs = reader.readInt32();
            for (i = 0; i < 2; i++) {
                adpcm_pos[i] = reader.readInt32();
            }
            for (i = 0; i < 2; i++) {
                adpcm_end[i] = reader.readInt32();
            }
            for (i = 0; i < 2; i++) {
                adpcm_data[i] = reader.readInt32();
            }
            m_sprite_ctrl = reader.readUInt16();
            m_sprites_flipscreen = reader.readUInt16();
            current_bank = reader.readByte();
            current_cmd = reader.readByte();
            cchip_last_7a = reader.readByte();
            cchip_last_04 = reader.readByte();
            cchip_last_05 = reader.readByte();
            cchip_coins_for_credit = reader.readBytes(2);
            cchip_credits_for_coin = reader.readBytes(2);
            cchip_coins = reader.readBytes(2);
            c588 = reader.readByte();
            c589 = reader.readByte();
            c58a = reader.readByte();
            m_triggeredLevel1b = reader.readByte();
            m_triggeredLevel2 = reader.readByte();
            m_triggeredLevel2b = reader.readByte();
            m_triggeredLevel2c = reader.readByte();
            m_triggeredLevel3b = reader.readByte();
            m_triggeredLevel13b = reader.readByte();
            m_triggeredLevel4 = reader.readByte();
            m_triggeredLevel5 = reader.readByte();
            m_triggeredLevel7 = reader.readByte();
            m_triggeredLevel8 = reader.readByte();
            m_triggeredLevel9 = reader.readByte();
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x8000);
            mainram2 = reader.readBytes(0x1_0000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x1000);
            for (i = 0; i < Z80A.nZ80; i++) {
                Z80A.zz1[i].LoadStateBinary(reader);
            }
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
            Sound.latched_value[0] = reader.readUInt16();
            Sound.utempdata[0] = reader.readUInt16();
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

//#region Taitoic

    public static int PC080SN_chips;
    public static short[][] PC080SN_ctrl;
    public static short[][] PC080SN_ram;
    public static short[][] PC080SN_bg_ram_offset, PC080SN_bgscroll_ram_offset;
    public static int[][] PC080SN_bgscrollx, PC080SN_bgscrolly;
    public static int PC080SN_xoffs, PC080SN_yoffs;
    public static Tmap[][] PC080SN_tilemap;
    public static int PC080SN_yinvert, PC080SN_dblwidth;
    public static short PC090OJ_ctrl, PC090OJ_buffer, PC090OJ_gfxnum;
    public static short PC090OJ_sprite_ctrl;
    public static short[] PC090OJ_ram, PC090OJ_ram_buffered;
    public static int PC090OJ_xoffs, PC090OJ_yoffs;

    public static void taitoic_init() {
        int i;
        PC080SN_ctrl = new short[2][];
        PC080SN_ram = new short[2][];
        PC080SN_bg_ram_offset = new short[2][];
        PC080SN_bgscroll_ram_offset = new short[2][];
        PC080SN_bgscrollx = new int[2][];
        PC080SN_bgscrolly = new int[2][];
        PC080SN_tilemap = new Tmap[2][];
        for (i = 0; i < 2; i++) {
            PC080SN_ctrl[i] = new short[8];
            PC080SN_bg_ram_offset[i] = new short[2];
            PC080SN_bgscroll_ram_offset[i] = new short[2];
            PC080SN_bgscrollx[i] = new int[2];
            PC080SN_bgscrolly[i] = new int[2];
            PC080SN_tilemap[i] = new Tmap[2];
        }
    }

    public static void tilemap_init() {
        switch (Machine.sName) {
            case "opwolf":
            case "opwolfa":
            case "opwolfj":
            case "opwolfu":
            case "opwolfb":
            case "opwolfp":
                taitoic_init();
                break;
        }
    }

    public static void PC080SN_vh_start(int chips, int gfxnum, int x_offset, int y_offset, int y_invert, int opaque, int dblwidth) {
        int i, j, k;
        PC080SN_chips = chips;
        PC080SN_yinvert = y_invert;
        PC080SN_dblwidth = dblwidth;
        PC080SN_xoffs = x_offset;
        PC080SN_yoffs = y_offset;
        for (i = 0; i < chips; i++) {
            int xd, yd;
            for (j = 0; j < 2; j++) {
                PC080SN_tilemap[i][j] = new Tmap();
                if (PC080SN_dblwidth == 0) { // standard tilemaps
                    PC080SN_tilemap[i][j].cols = 64;
                    PC080SN_tilemap[i][j].rows = 64;
                    PC080SN_tilemap[i][j].tilewidth = 8;
                    PC080SN_tilemap[i][j].tileheight = 8;
                    PC080SN_tilemap[i][j].width = 0x200;
                    PC080SN_tilemap[i][j].height = 0x200;
                    PC080SN_tilemap[i][j].enable = true;
                    PC080SN_tilemap[i][j].all_tiles_dirty = true;
                    PC080SN_tilemap[i][j].total_elements = gfx1rom.length / 0x40;
                    PC080SN_tilemap[i][j].pixmap = new short[0x200 * 0x200];
                    PC080SN_tilemap[i][j].flagsmap = new byte[0x200][0x200];
                    PC080SN_tilemap[i][j].tileflags = new byte[64][64];
                    PC080SN_tilemap[i][j].pen_data = new byte[0x40];
                    PC080SN_tilemap[i][j].pen_to_flags = new byte[1][16];
                    PC080SN_tilemap[i][j].pen_to_flags[0][0] = 0;
                    for (k = 1; k < 16; k++) {
                        PC080SN_tilemap[i][j].pen_to_flags[0][k] = 0x10;
                    }
                    PC080SN_tilemap[i][j].scrollrows = 1;
                    PC080SN_tilemap[i][j].scrollcols = 1;
                    PC080SN_tilemap[i][j].rowscroll = new int[PC080SN_tilemap[i][j].scrollrows];
                    PC080SN_tilemap[i][j].colscroll = new int[PC080SN_tilemap[i][j].scrollcols];
                    PC080SN_tilemap[i][j].tilemap_draw_instance3 = PC080SN_tilemap[i][j]::tilemap_draw_instanceTaito_opwolf;
                } else { // double width tilemaps
                    PC080SN_tilemap[i][j].cols = 128;
                    PC080SN_tilemap[i][j].rows = 64;
                    PC080SN_tilemap[i][j].tilewidth = 8;
                    PC080SN_tilemap[i][j].tileheight = 8;
                    PC080SN_tilemap[i][j].width = 0x400;
                    PC080SN_tilemap[i][j].height = 0x200;
                    PC080SN_tilemap[i][j].enable = true;
                    PC080SN_tilemap[i][j].all_tiles_dirty = true;
                    PC080SN_tilemap[i][j].total_elements = gfx1rom.length / 0x40;
                    PC080SN_tilemap[i][j].pixmap = new short[0x200 * 0x400];
                    PC080SN_tilemap[i][j].flagsmap = new byte[0x200][0x400];
                    PC080SN_tilemap[i][j].tileflags = new byte[128][64];
                    PC080SN_tilemap[i][j].pen_data = new byte[0x40];
                    PC080SN_tilemap[i][j].pen_to_flags = new byte[1][16];
                    PC080SN_tilemap[i][j].pen_to_flags[0][0] = 0;
                    for (k = 1; k < 16; k++) {
                        PC080SN_tilemap[i][j].pen_to_flags[0][k] = 0x10;
                    }
                    PC080SN_tilemap[i][j].scrollrows = 1;
                    PC080SN_tilemap[i][j].scrollcols = 1;
                    PC080SN_tilemap[i][j].rowscroll = new int[PC080SN_tilemap[i][j].scrollrows];
                    PC080SN_tilemap[i][j].colscroll = new int[PC080SN_tilemap[i][j].scrollcols];
                    PC080SN_tilemap[i][j].tilemap_draw_instance3 = PC080SN_tilemap[i][j]::tilemap_draw_instanceTaito_opwolf;
                }
            }
            PC080SN_tilemap[i][0].tile_update3 = PC080SN_tilemap[i][0]::tile_updateTaitobg_opwolf;
            PC080SN_tilemap[i][1].tile_update3 = PC080SN_tilemap[i][1]::tile_updateTaitofg_opwolf;
            PC080SN_ram[i] = new short[0x8000];
            PC080SN_bg_ram_offset[i][0] = 0x0000 / 2;
            PC080SN_bg_ram_offset[i][1] = 0x8000 / 2;
            PC080SN_bgscroll_ram_offset[i][0] = 0x4000 / 2;
            PC080SN_bgscroll_ram_offset[i][1] = 0xc000 / 2;
            Arrays.fill(PC080SN_ram[i], 0, 0x8000, (short) 0);
//            state_save_register_item_pointer("PC080SN", i, PC080SN_ram[i], PC080SN_RAM_SIZE / 2);
//            state_save_register_item_array("PC080SN", i, PC080SN_ctrl[i]);
//            state_save_register_postload(machine, PC080SN_restore_scroll, (void *) (FPTR) i);

//            tilemap_set_transparent_pen(PC080SN_tilemap[i][0], 0);
//            tilemap_set_transparent_pen(PC080SN_tilemap[i][1], 0);

            // I'm setting optional chip #2 with the same offsets (Topspeed)
            xd = (i == 0) ? -x_offset : -x_offset;
            yd = (i == 0) ? y_offset : y_offset;
            PC080SN_tilemap[i][0].tilemap_set_scrolldx(-16 + xd, -16 - xd);
            PC080SN_tilemap[i][0].tilemap_set_scrolldy(yd, -yd);
            PC080SN_tilemap[i][1].tilemap_set_scrolldx(-16 + xd, -16 - xd);
            PC080SN_tilemap[i][1].tilemap_set_scrolldy(yd, -yd);
            if (PC080SN_dblwidth == 0) {
                PC080SN_tilemap[i][0].scrollrows = 512;
                PC080SN_tilemap[i][0].rowscroll = new int[PC080SN_tilemap[i][0].scrollrows];
                PC080SN_tilemap[i][1].scrollrows = 512;
                PC080SN_tilemap[i][1].rowscroll = new int[PC080SN_tilemap[i][1].scrollrows];
            }
        }
    }

    public static short PC080SN_word_0_r(int offset) {
        return PC080SN_ram[0][offset];
    }

    public static short PC080SN_word_1_r(int offset) {
        return PC080SN_ram[1][offset];
    }

    public static void PC080SN_word_w(int chip, int offset, short data) {
        int row, col, memindex;
        PC080SN_ram[chip][offset] = data;
        if (PC080SN_dblwidth == 0) {
            if (offset < 0x2000) {
                memindex = offset / 2;
                row = memindex / PC080SN_tilemap[chip][0].cols;
                col = memindex % PC080SN_tilemap[chip][0].cols;
                PC080SN_tilemap[chip][0].tilemap_mark_tile_dirty(row, col);
            } else if (offset >= 0x4000 && offset < 0x6000) {
                memindex = (offset & 0x1fff) / 2;
                row = memindex / PC080SN_tilemap[chip][1].cols;
                col = memindex % PC080SN_tilemap[chip][1].cols;
                PC080SN_tilemap[chip][1].tilemap_mark_tile_dirty(row, col);
            }
        } else {
            if (offset < 0x4000) {
                memindex = offset & 0x1fff;
                row = memindex / PC080SN_tilemap[chip][0].cols;
                col = memindex % PC080SN_tilemap[chip][0].cols;
                PC080SN_tilemap[chip][0].tilemap_mark_tile_dirty(row, col);
            } else if (offset >= 0x4000 && offset < 0x8000) {
                memindex = offset & 0x1fff;
                row = memindex / PC080SN_tilemap[chip][1].cols;
                col = memindex % PC080SN_tilemap[chip][1].cols;
                PC080SN_tilemap[chip][1].tilemap_mark_tile_dirty(row, col);
            }
        }
    }

    public static void PC080SN_word_w1(int chip, int offset, byte data) {
        int row, col, memindex;
        PC080SN_ram[chip][offset] = (short) ((data << 8) | (PC080SN_ram[chip][offset] & 0xff));
        if (PC080SN_dblwidth == 0) {
            if (offset < 0x2000) {
                memindex = offset / 2;
                row = memindex / PC080SN_tilemap[chip][0].cols;
                col = memindex % PC080SN_tilemap[chip][0].cols;
                PC080SN_tilemap[chip][0].tilemap_mark_tile_dirty(row, col);
            } else if (offset >= 0x4000 && offset < 0x6000) {
                memindex = (offset & 0x1fff) / 2;
                row = memindex / PC080SN_tilemap[chip][1].cols;
                col = memindex % PC080SN_tilemap[chip][1].cols;
                PC080SN_tilemap[chip][1].tilemap_mark_tile_dirty(row, col);
            }
        } else {
            if (offset < 0x4000) {
                memindex = offset & 0x1fff;
                row = memindex / PC080SN_tilemap[chip][0].cols;
                col = memindex % PC080SN_tilemap[chip][0].cols;
                PC080SN_tilemap[chip][0].tilemap_mark_tile_dirty(row, col);
            } else if (offset >= 0x4000 && offset < 0x8000) {
                memindex = offset & 0x1fff;
                row = memindex / PC080SN_tilemap[chip][1].cols;
                col = memindex % PC080SN_tilemap[chip][1].cols;
                PC080SN_tilemap[chip][1].tilemap_mark_tile_dirty(row, col);
            }
        }
    }

    public static void PC080SN_word_w2(int chip, int offset, byte data) {
        int row, col, memindex;
        PC080SN_ram[chip][offset] = (short) ((PC080SN_ram[chip][offset] & 0xff00) | data);
        if (PC080SN_dblwidth == 0) {
            if (offset < 0x2000) {
                memindex = offset / 2;
                row = memindex / PC080SN_tilemap[chip][0].cols;
                col = memindex % PC080SN_tilemap[chip][0].cols;
                PC080SN_tilemap[chip][0].tilemap_mark_tile_dirty(row, col);
            } else if (offset >= 0x4000 && offset < 0x6000) {
                memindex = (offset & 0x1fff) / 2;
                row = memindex / PC080SN_tilemap[chip][1].cols;
                col = memindex % PC080SN_tilemap[chip][1].cols;
                PC080SN_tilemap[chip][1].tilemap_mark_tile_dirty(row, col);
            }
        } else {
            if (offset < 0x4000) {
                memindex = offset & 0x1fff;
                row = memindex / PC080SN_tilemap[chip][0].cols;
                col = memindex % PC080SN_tilemap[chip][0].cols;
                PC080SN_tilemap[chip][0].tilemap_mark_tile_dirty(row, col);
            } else if (offset >= 0x4000 && offset < 0x8000) {
                memindex = offset & 0x1fff;
                row = memindex / PC080SN_tilemap[chip][1].cols;
                col = memindex % PC080SN_tilemap[chip][1].cols;
                PC080SN_tilemap[chip][1].tilemap_mark_tile_dirty(row, col);
            }
        }
    }

    public static void PC080SN_word_0_w(int offset, short data) {
        PC080SN_word_w(0, offset, data);
    }

    public static void PC080SN_word_0_w1(int offset, byte data) {
        PC080SN_word_w1(0, offset, data);
    }

    public static void PC080SN_word_0_w2(int offset, byte data) {
        PC080SN_word_w2(0, offset, data);
    }

    public static void PC080SN_word_1_w(int offset, short data) {
        PC080SN_word_w(1, offset, data);
    }

    public static void PC080SN_word_1_w1(int offset, byte data) {
        PC080SN_word_w1(1, offset, data);
    }

    public static void PC080SN_word_1_w2(int offset, byte data) {
        PC080SN_word_w2(1, offset, data);
    }

    public static void PC080SN_xscroll_word_w(int chip, int offset, short data) {
        short data1;
        PC080SN_ctrl[chip][offset] = data;
        data1 = PC080SN_ctrl[chip][offset];
        switch (offset) {
            case 0x00:
                PC080SN_bgscrollx[chip][0] = -data1;
                break;
            case 0x01:
                PC080SN_bgscrollx[chip][1] = -data1;
                break;
        }
    }

    public static void PC080SN_xscroll_word_w1(int chip, int offset, byte data) {
        short data1;
        PC080SN_ctrl[chip][offset] = (short) ((data << 8) | (PC080SN_ctrl[chip][offset] & 0xff));
        data1 = PC080SN_ctrl[chip][offset];
        switch (offset) {
            case 0x00:
                PC080SN_bgscrollx[chip][0] = -data1;
                break;
            case 0x01:
                PC080SN_bgscrollx[chip][1] = -data1;
                break;
        }
    }

    public static void PC080SN_xscroll_word_w2(int chip, int offset, byte data) {
        short data1;
        PC080SN_ctrl[chip][offset] = (short) ((PC080SN_ctrl[chip][offset] & 0xff00) | data);
        data1 = PC080SN_ctrl[chip][offset];
        switch (offset) {
            case 0x00:
                PC080SN_bgscrollx[chip][0] = -data1;
                break;
            case 0x01:
                PC080SN_bgscrollx[chip][1] = -data1;
                break;
        }
    }

    public static void PC080SN_yscroll_word_w(int chip, int offset, short data) {
        int data1;
        PC080SN_ctrl[chip][offset + 2] = data;
        data1 = PC080SN_ctrl[chip][offset + 2];
        if (PC080SN_yinvert != 0) {
            data1 = -data1;
        }
        switch (offset) {
            case 0x00:
                PC080SN_bgscrolly[chip][0] = -data1;
                break;
            case 0x01:
                PC080SN_bgscrolly[chip][1] = -data1;
                break;
        }
    }

    public static void PC080SN_yscroll_word_w1(int chip, int offset, byte data) {
        int data1;
        PC080SN_ctrl[chip][offset + 2] = (short) ((data << 8) | (PC080SN_ctrl[chip][offset + 2] & 0xff));
        data1 = PC080SN_ctrl[chip][offset + 2];
        if (PC080SN_yinvert != 0) {
            data1 = -data1;
        }
        switch (offset) {
            case 0x00:
                PC080SN_bgscrolly[chip][0] = -data1;
                break;
            case 0x01:
                PC080SN_bgscrolly[chip][1] = -data1;
                break;
        }
    }

    public static void PC080SN_yscroll_word_w2(int chip, int offset, byte data) {
        int data1;
        PC080SN_ctrl[chip][offset + 2] = (short) ((PC080SN_ctrl[chip][offset + 2] & 0xff00) | data);
        data1 = PC080SN_ctrl[chip][offset + 2];
        if (PC080SN_yinvert != 0) {
            data1 = -data1;
        }
        switch (offset) {
            case 0x00:
                PC080SN_bgscrolly[chip][0] = -data1;
                break;
            case 0x01:
                PC080SN_bgscrolly[chip][1] = -data1;
                break;
        }
    }

    public static void PC080SN_ctrl_word_w(int chip, int offset, short data) {
        short data1;
        PC080SN_ctrl[chip][offset + 4] = data;
        data1 = PC080SN_ctrl[chip][offset + 4];
        switch (offset) {
            case 0x00: {
                int flip = (data1 & 0x01) != 0 ? (Tilemap.TILEMAP_FLIPX | Tilemap.TILEMAP_FLIPY) : 0;
//                tilemap_set_flip(PC080SN_tilemap[chip][0], flip);
//                tilemap_set_flip(PC080SN_tilemap[chip][1], flip);
                break;
            }
        }
    }

    public static void PC080SN_ctrl_word_w1(int chip, int offset, byte data) {
        short data1;
        PC080SN_ctrl[chip][offset + 4] = (short) ((data << 8) | (PC080SN_ctrl[chip][offset + 4] & 0xff));
        data1 = PC080SN_ctrl[chip][offset + 4];
        switch (offset) {
            case 0x00: {
                int flip = (data1 & 0x01) != 0 ? (Tilemap.TILEMAP_FLIPX | Tilemap.TILEMAP_FLIPY) : 0;
                break;
            }
        }
    }

    public static void PC080SN_ctrl_word_w2(int chip, int offset, byte data) {
        short data1;
        PC080SN_ctrl[chip][offset + 4] = (short) ((PC080SN_ctrl[chip][offset + 4] & 0xff00) | data);
        data1 = PC080SN_ctrl[chip][offset + 4];
        switch (offset) {
            case 0x00: {
                int flip = (data1 & 0x01) != 0 ? (Tilemap.TILEMAP_FLIPX | Tilemap.TILEMAP_FLIPY) : 0;
                break;
            }
        }
    }

    public static void PC080SN_xscroll_word_0_w(int offset, short data) {
        PC080SN_xscroll_word_w(0, offset, data);
    }

    public static void PC080SN_xscroll_word_0_w1(int offset, byte data) {
        PC080SN_xscroll_word_w1(0, offset, data);
    }

    public static void PC080SN_xscroll_word_0_w2(int offset, byte data) {
        PC080SN_xscroll_word_w2(0, offset, data);
    }

    public static void PC080SN_xscroll_word_1_w(int offset, short data) {
        PC080SN_xscroll_word_w(1, offset, data);
    }

    public static void PC080SN_xscroll_word_1_w1(int offset, byte data) {
        PC080SN_xscroll_word_w1(1, offset, data);
    }

    public static void PC080SN_xscroll_word_1_w2(int offset, byte data) {
        PC080SN_xscroll_word_w2(1, offset, data);
    }

    public static void PC080SN_yscroll_word_0_w(int offset, short data) {
        PC080SN_yscroll_word_w(0, offset, data);
    }

    public static void PC080SN_yscroll_word_0_w1(int offset, byte data) {
        PC080SN_yscroll_word_w1(0, offset, data);
    }

    public static void PC080SN_yscroll_word_0_w2(int offset, byte data) {
        PC080SN_yscroll_word_w2(0, offset, data);
    }

    public static void PC080SN_yscroll_word_1_w(int offset, short data) {
        PC080SN_yscroll_word_w(1, offset, data);
    }

    public static void PC080SN_yscroll_word_1_w1(int offset, byte data) {
        PC080SN_yscroll_word_w1(1, offset, data);
    }

    public static void PC080SN_yscroll_word_1_w2(int offset, byte data) {
        PC080SN_yscroll_word_w2(1, offset, data);
    }

    public static void PC080SN_ctrl_word_0_w(int offset, short data) {
        PC080SN_ctrl_word_w(0, offset, data);
    }

    public static void PC080SN_ctrl_word_0_w1(int offset, byte data) {
        PC080SN_ctrl_word_w1(0, offset, data);
    }

    public static void PC080SN_ctrl_word_0_w2(int offset, byte data) {
        PC080SN_ctrl_word_w2(0, offset, data);
    }

    public static void PC080SN_ctrl_word_1_w(int offset, short data) {
        PC080SN_ctrl_word_w(1, offset, data);
    }

    public static void PC080SN_ctrl_word_1_w1(int offset, byte data) {
        PC080SN_ctrl_word_w1(1, offset, data);
    }

    public static void PC080SN_ctrl_word_1_w2(int offset, byte data) {
        PC080SN_ctrl_word_w2(1, offset, data);
    }

    public static void PC080SN_tilemap_update() {
        int chip, j;
        for (chip = 0; chip < PC080SN_chips; chip++) {
            PC080SN_tilemap[chip][0].tilemap_set_scrolly(0, PC080SN_bgscrolly[chip][0]);
            PC080SN_tilemap[chip][1].tilemap_set_scrolly(0, PC080SN_bgscrolly[chip][1]);
            if (PC080SN_dblwidth == 0) {
                for (j = 0; j < 256; j++) {
                    PC080SN_tilemap[chip][0].tilemap_set_scrollx((j + PC080SN_bgscrolly[chip][0]) & 0x1ff, PC080SN_bgscrollx[chip][0] - PC080SN_ram[chip][PC080SN_bgscroll_ram_offset[chip][0] + j]);
//                    PC080SN_tilemap[chip][0].tilemap_set_scrollx(j, PC080SN_bgscrollx[chip][0]);
                }
                for (j = 0; j < 256; j++) {
                    PC080SN_tilemap[chip][1].tilemap_set_scrollx((j + PC080SN_bgscrolly[chip][1]) & 0x1ff, PC080SN_bgscrollx[chip][1] - PC080SN_ram[chip][PC080SN_bgscroll_ram_offset[chip][1] + j]);
//                    PC080SN_tilemap[chip][1].tilemap_set_scrollx(j, PC080SN_bgscrollx[chip][1]);
                }
            } else {
                PC080SN_tilemap[chip][0].tilemap_set_scrollx(0, PC080SN_bgscrollx[chip][0]);
                PC080SN_tilemap[chip][1].tilemap_set_scrollx(0, PC080SN_bgscrollx[chip][1]);
            }
        }
    }

    public static void PC080SN_tilemap_draw(int chip, int layer, int flags, byte priority) {
        PC080SN_tilemap[chip][layer].tilemap_draw_primask(Video.screenstate.visarea, flags, priority);
    }

    public static void PC090OJ_vh_start(int gfxnum, int x_offset, int y_offset, int use_buffer) {
        PC090OJ_gfxnum = (short) gfxnum;
        PC090OJ_xoffs = x_offset;
        PC090OJ_yoffs = y_offset;
        PC090OJ_buffer = (short) use_buffer;
        PC090OJ_ram = new short[0x2000];
        PC090OJ_ram_buffered = new short[0x2000];
        Arrays.fill(PC090OJ_ram, 0, 0x2000, (short) 0);
        Arrays.fill(PC090OJ_ram_buffered, 0, 0x2000, (short) 0);
    }

    public static short PC090OJ_word_0_r(int offset) {
        return PC090OJ_ram[offset];
    }

    public static void PC090OJ_word_w(int offset, short data) {
        PC090OJ_ram[offset] = data;
        if (PC090OJ_buffer == 0) {
            PC090OJ_ram_buffered[offset] = PC090OJ_ram[offset];
        }
        if (offset == 0xdff) {
            PC090OJ_ctrl = data;
        }
    }

    public static void PC090OJ_word_w1(int offset, byte data) {
        PC090OJ_ram[offset] = (short) ((data << 8) | (PC090OJ_ram[offset] & 0xff));
        if (PC090OJ_buffer == 0) {
            PC090OJ_ram_buffered[offset] = PC090OJ_ram[offset];
        }
        if (offset == 0xdff) {
            PC090OJ_ctrl = (short) ((data << 8) | (PC090OJ_ctrl & 0xff));
        }
    }

    public static void PC090OJ_word_w2(int offset, byte data) {
        PC090OJ_ram[offset] = (short) ((PC090OJ_ram[offset] & 0xff00) | data);
        if (PC090OJ_buffer == 0) {
            PC090OJ_ram_buffered[offset] = PC090OJ_ram[offset];
        }
        if (offset == 0xdff) {
            PC090OJ_ctrl = (short) ((PC090OJ_ctrl & 0xff00) | data);
        }
    }

    public static void PC090OJ_word_0_w(int offset, short data) {
        PC090OJ_word_w(offset, data);
    }

    public static void PC090OJ_word_0_w1(int offset, byte data) {
        PC090OJ_word_w1(offset, data);
    }

    public static void PC090OJ_word_0_w2(int offset, byte data) {
        PC090OJ_word_w2(offset, data);
    }

    public static void PC090OJ_draw_sprites(int pri_type) {
        int offs, priority = 0;
        int sprite_colbank = (PC090OJ_sprite_ctrl & 0xf) << 4; // top nibble
        switch (pri_type) {
            case 0x00:
                priority = 0; // sprites over top bg layer
                break;
            case 0x01:
                priority = 1; // sprites under top bg layer
                break;
            case 0x02:
                priority = PC090OJ_sprite_ctrl >> 15; // variable sprite/tile priority
                break;
        }
        for (offs = 0; offs < 0x800 / 2; offs += 4) {
            int flipx, flipy;
            int x, y;
            int data, code, color;
            data = PC090OJ_ram_buffered[offs + 0];
            flipy = (data & 0x8000) >> 15;
            flipx = (data & 0x4000) >> 14;
            color = (data & 0x000f) | sprite_colbank;
            code = PC090OJ_ram_buffered[offs + 2] & 0x1fff;
            x = PC090OJ_ram_buffered[offs + 3] & 0x1ff;   /* mask verified with Rainbowe board */
            y = PC090OJ_ram_buffered[offs + 1] & 0x1ff;   /* mask verified with Rainbowe board */
            if (x > 0x140) {
                x -= 0x200;
            }
            if (y > 0x140) {
                y -= 0x200;
            }
            if ((PC090OJ_ctrl & 1) == 0) {
                x = 320 - x - 16;
                y = 256 - y - 16;
                flipx = (flipx == 0 ? 1 : 0);
                flipy = (flipy == 0 ? 1 : 0);
            }
            x += PC090OJ_xoffs;
            y += PC090OJ_yoffs;
            Drawgfx.common_drawgfx_opwolf(gfx2rom, code, color, flipx, flipy, x, y, cliprect, (priority != 0 ? 0xfc : 0xf0) | (1 << 31));
        }
    }

//#endregion

//#region Video

    public static byte[] gfx1rom, gfx2rom, gfx12rom, gfx22rom, prom;
    public static final int bublbobl_objectram_size = 0x300;
    public static RECT cliprect;
    public static short[] uuFF;

    public static void video_start_bublbobl() {
        int i;
        uuFF = new short[0x100 * 0x100];
        for (i = 0; i < 0x1_0000; i++) {
            uuFF[i] = 0xff;
        }
        cliprect = new RECT();
        cliprect.min_x = 0;
        cliprect.max_x = 255;
        cliprect.min_y = 16;
        cliprect.max_y = 239;
    }

    public static void video_update_bublbobl() {
        int offs;
        int sx, sy, xc, yc;
        int gfx_num, gfx_attr, gfx_offs;
        int prom_line_offset;
        System.arraycopy(uuFF, 0, Video.bitmapbase[Video.curbitmap], 0, 0x1_0000);
        if (bublbobl_video_enable == 0) {
            return;
        }
        sx = 0;
        if (videoram[0xe86] == 0x7b) {
            int i1 = 1;
        }
        for (offs = 0; offs < bublbobl_objectram_size; offs += 4) {
            if (bublbobl_objectram[offs] == 0 && bublbobl_objectram[offs + 1] == 0 && bublbobl_objectram[offs + 2] == 0 && bublbobl_objectram[offs + 3] == 0) {
                continue;
            }
            gfx_num = bublbobl_objectram[offs + 1];
            gfx_attr = bublbobl_objectram[offs + 3];
            prom_line_offset = 0x80 + ((gfx_num & 0xe0) >> 1);
            gfx_offs = ((gfx_num & 0x1f) * 0x80);
            if ((gfx_num & 0xa0) == 0xa0) {
                gfx_offs |= 0x1000;
            }
            sy = -bublbobl_objectram[offs + 0];
            for (yc = 0; yc < 32; yc++) {
                if ((prom[prom_line_offset + yc / 2] & 0x08) != 0) {
                    continue;
                }
                if ((prom[prom_line_offset + yc / 2] & 0x04) == 0) {
                    sx = bublbobl_objectram[offs + 2];
                    if ((gfx_attr & 0x40) != 0) {
                        sx -= 256;
                    }
                }
                for (xc = 0; xc < 2; xc++) {
                    int goffs, code, color, flipx, flipy, x, y;
                    goffs = gfx_offs + xc * 0x40 + (yc & 7) * 0x02 + (prom[prom_line_offset + yc / 2] & 0x03) * 0x10;
                    code = videoram[goffs] + 256 * (videoram[goffs + 1] & 0x03) + 1024 * (gfx_attr & 0x0f);
                    color = (videoram[goffs + 1] & 0x3c) >> 2;
                    flipx = videoram[goffs + 1] & 0x40;
                    flipy = videoram[goffs + 1] & 0x80;
                    x = sx + xc * 8;
                    y = (sy + yc * 8) & 0xff;
                    if (Generic.flip_screen_get() != 0) {
                        x = 248 - x;
                        y = 248 - y;
                        flipx = (flipx == 0) ? 1 : 0;
                        flipy = (flipy == 0) ? 1 : 0;
                    }
                    Drawgfx.common_drawgfx_bublbobl(gfx1rom, code, color, flipx, flipy, x, y, cliprect);
                }
            }
            sx += 16;
        }
    }

    public static void video_eof_taito() {
    }

    public static void video_start_opwolf() {
        cliprect = new RECT();
        cliprect.min_x = 0;
        cliprect.max_x = 319;
        cliprect.min_y = 8;
        cliprect.max_y = 247;
        PC080SN_vh_start(1, 1, 0, 0, 0, 0, 0);
        PC090OJ_vh_start(0, 0, 0, 0);
    }

    public static void video_update_opwolf() {
        int[] layer = new int[2];
        PC080SN_tilemap_update();
        layer[0] = 0;
        layer[1] = 1;
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x1_4000, null);
        PC080SN_tilemap_draw(0, layer[0], 0, (byte) 1);
        PC080SN_tilemap_draw(0, layer[1], 0x10, (byte) 2);
        PC090OJ_draw_sprites(1);
    }

    public static void opwolf_spritectrl_w(int offset, short data) {
        if (offset == 0) {
            PC090OJ_sprite_ctrl = (short) ((data & 0xe0) >> 5);
        }
    }

    public static void opwolf_spritectrl_w2(int offset, byte data) {
        if (offset == 0) {
            PC090OJ_sprite_ctrl = (short) ((data & 0xe0) >> 5);
        }
    }

//#endregion

//#region Input

    public static void loop_inputports_taito_common() {
    }

    public static void loop_inputports_taito_bublbobl() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 |= 0x04;
        } else {
            sbyte0 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 |= 0x08;
        } else {
            sbyte0 &= ~0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte1 &= ~0x40;
        } else {
            sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            sbyte1 &= ~0x01;
        } else {
            sbyte1 |= 0x01;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_S)) {
//            sbyte2 &= ~0x02;
//        } else {
//            sbyte2 |= 0x02;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_W)) {
//            sbyte2 &= ~0x01;
//        } else {
//            sbyte2 |= 0x01;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_L)) {
//            sbyte1 &= ~0x04;
//        } else {
//            sbyte1 |= 0x04;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            sbyte2 &= ~0x02;
        } else {
            sbyte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            sbyte2 &= ~0x01;
        } else {
            sbyte2 |= 0x01;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_DOWN)) {
//            sbyte2 &= ~0x20;
//        } else {
//            sbyte2 |= 0x20;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_UP)) {
//            sbyte2 &= ~0x10;
//        } else {
//            sbyte2 |= 0x10;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD3)) {
//            sbyte1 &= ~0x40;
//        } else {
//            sbyte1 |= 0x40;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_T)) {
//            sbyte0 &= ~0x02;
//        } else {
//            sbyte0 |= 0x02;
//        }
    }

    public static void loop_inputports_taito_tokio() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 |= 0x04;
        } else {
            sbyte0 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 |= 0x08;
        } else {
            sbyte0 &= ~0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte1 &= ~0x40;
        } else {
            sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            sbyte1 &= ~0x01;
        } else {
            sbyte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_L)) {
//            sbyte1 &= ~0x04;
//        } else {
//            sbyte1 |= 0x04;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            sbyte2 &= ~0x02;
        } else {
            sbyte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            sbyte2 &= ~0x01;
        } else {
            sbyte2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD3)) {
//            sbyte1 &= ~0x40;
//        } else {
//            sbyte1 |= 0x40;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_T)) {
//            sbyte0 &= ~0x02;
//        } else {
//            sbyte0 |= 0x02;
//        }
    }

    public static void loop_inputports_taito_boblbobl() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 |= 0x08;
        } else {
            sbyte0 &= ~0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 |= 0x04;
        } else {
            sbyte0 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte1 &= ~0x40;
        } else {
            sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            sbyte0 &= ~0x02;
        } else {
            sbyte0 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_S)) {
//            sbyte1 &= ~0x04;
//        } else {
//            sbyte1 |= 0x04;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_W)) {
//            sbyte1 &= ~0x08;
//        } else {
//            sbyte1 |= 0x08;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte0 &= ~0x20;
        } else {
            sbyte0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_L)) {
//            sbyte1 &= ~0x04;
//        } else {
//            sbyte1 |= 0x04;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            sbyte1 &= ~0x01;
        } else {
            sbyte1 |= 0x01;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_DOWN)) {
//            sbyte2 &= ~0x04;
//        } else {
//            sbyte2 |= 0x04;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_UP)) {
//            sbyte2 &= ~0x08;
//        } else {
//            sbyte2 |= 0x08;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD3)) {
//            sbyte1 &= ~0x40;
//        } else {
//            sbyte1 |= 0x40;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_T)) {
//            sbyte0 &= ~0x02;
//        } else {
//            sbyte0 |= 0x02;
//        }
    }

    public static void loop_inputports_taito_opwolf() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 |= 0x01;
        } else {
            sbyte0 &= ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte0 |= 0x02;
        } else {
            sbyte0 &= ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_2)) {
//            sbyte2 &= ~0x40;
//        } else {
//            sbyte2 |= 0x40;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_D)) {
//            sbyte1 &= ~0x02;
//        } else {
//            sbyte1 |= 0x02;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_A)) {
//            sbyte1 &= ~0x01;
//        } else {
//            sbyte1 |= 0x01;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_S)) {
//            sbyte2 &= ~0x02;
//        } else {
//            sbyte2 |= 0x02;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_W)) {
//            sbyte2 &= ~0x01;
//        } else {
//            sbyte2 |= 0x01;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_J) || Mouse.buttons[0] != 0) {
            sbyte1 &= ~0x01;
        } else {
            sbyte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K) || Mouse.buttons[1] != 0) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_L)) {
//            sbyte1 &= ~0x04;
//        } else {
//            sbyte1 |= 0x04;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_RIGHT)) {
//            sbyte2 &= ~0x02;
//        } else {
//            sbyte2 |= 0x02;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_LEFT)) {
//            sbyte2 &= ~0x01;
//        } else {
//            sbyte2 |= 0x01;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_DOWN)) {
//            sbyte2 &= ~0x20;
//        } else {
//            sbyte2 |= 0x20;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_UP)) {
//            sbyte2 &= ~0x10;
//        } else {
//            sbyte2 |= 0x10;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD1)) {
//            sbyte2 &= ~0x20;
//        } else {
//            sbyte2 |= 0x20;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD2)) {
//            sbyte2 &= ~0x10;
//        } else {
//            sbyte2 |= 0x10;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD3)) {
//            sbyte1 &= ~0x40;
//        } else {
//            sbyte1 |= 0x40;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        Inptport.frame_update_analog_field_opwolf_p1x(Inptport.analog_p1x);
        Inptport.frame_update_analog_field_opwolf_p1y(Inptport.analog_p1y);
        p1x = (byte) Inptport.input_port_read_direct(Inptport.analog_p1x);
        p1y = (byte) Inptport.input_port_read_direct(Inptport.analog_p1y);
    }

    public static void loop_inputports_taito_opwolfp() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte3 |= 0x02;
        } else {
            sbyte3 &= ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbyte3 |= 0x04;
        } else {
            sbyte3 &= ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J) || Mouse.buttons[0] != 0) {
            sbyte2 &= ~0x02;
        } else {
            sbyte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K) || Mouse.buttons[1] != 0) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        Inptport.frame_update_analog_field_opwolf_p1x(Inptport.analog_p1x);
        Inptport.frame_update_analog_field_opwolf_p1y(Inptport.analog_p1y);
        p1x = (byte) Inptport.input_port_read_direct(Inptport.analog_p1x);
        p1y = (byte) Inptport.input_port_read_direct(Inptport.analog_p1y);
    }

    public static void record_port_bublbobl() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
        }
    }

    public static void replay_port_bublbobl() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
                //Mame.mame_pause(true);
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void record_port_opwolf() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || Inptport.analog_p1x.accum != p1x_accum_old || Inptport.analog_p1x.previous != p1x_previous_old || Inptport.analog_p1y.accum != p1y_accum_old || Inptport.analog_p1y.previous != p1y_previous_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            p1x_accum_old = Inptport.analog_p1x.accum;
            p1x_previous_old = Inptport.analog_p1x.previous;
            p1y_accum_old = Inptport.analog_p1y.accum;
            p1y_previous_old = Inptport.analog_p1y.previous;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(Inptport.analog_p1x.accum);
            Mame.bwRecord.write(Inptport.analog_p1x.previous);
            Mame.bwRecord.write(Inptport.analog_p1y.accum);
            Mame.bwRecord.write(Inptport.analog_p1y.previous);
        }
    }

    public static void replay_port_opwolf() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                p1x_accum_old = Mame.brRecord.readInt32();
                p1x_previous_old = Mame.brRecord.readInt32();
                p1y_accum_old = Mame.brRecord.readInt32();
                p1y_previous_old = Mame.brRecord.readInt32();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            Inptport.analog_p1x.accum = p1x_accum_old;
            Inptport.analog_p1x.previous = p1x_previous_old;
            Inptport.analog_p1y.accum = p1y_accum_old;
            Inptport.analog_p1y.previous = p1y_previous_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void record_port_opwolfp() {
        if (sbyte2 != sbyte2_old || sbyte3 != sbyte3_old || Inptport.analog_p1x.accum != p1x_accum_old || Inptport.analog_p1x.previous != p1x_previous_old || Inptport.analog_p1y.accum != p1y_accum_old || Inptport.analog_p1y.previous != p1y_previous_old) {
            sbyte2_old = sbyte2;
            sbyte3_old = sbyte3;
            p1x_accum_old = Inptport.analog_p1x.accum;
            p1x_previous_old = Inptport.analog_p1x.previous;
            p1y_accum_old = Inptport.analog_p1y.accum;
            p1y_previous_old = Inptport.analog_p1y.previous;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbyte3);
            Mame.bwRecord.write(Inptport.analog_p1x.accum);
            Mame.bwRecord.write(Inptport.analog_p1x.previous);
            Mame.bwRecord.write(Inptport.analog_p1y.accum);
            Mame.bwRecord.write(Inptport.analog_p1y.previous);
        }
    }

    public static void replay_port_opwolfp() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte2_old = Mame.brRecord.readSByte();
                sbyte3_old = Mame.brRecord.readSByte();
                p1x_accum_old = Mame.brRecord.readInt32();
                p1x_previous_old = Mame.brRecord.readInt32();
                p1y_accum_old = Mame.brRecord.readInt32();
                p1y_previous_old = Mame.brRecord.readInt32();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte2 = sbyte2_old;
            sbyte3 = sbyte3_old;
            Inptport.analog_p1x.accum = p1x_accum_old;
            Inptport.analog_p1x.previous = p1x_previous_old;
            Inptport.analog_p1y.accum = p1y_accum_old;
            Inptport.analog_p1y.previous = p1y_previous_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Bublbobl

    public static byte dsw0, dsw1;
    public static int bublbobl_video_enable, tokio_prot_count;
    public static int address, latch;
    public static int sound_nmi_enable, pending_nmi;
    public static byte ddr1, ddr2, ddr3, ddr4;
    public static byte port1_in, port2_in, port3_in, port4_in;
    public static byte port1_out, port2_out, port3_out, port4_out;
    public static byte portA_in, portA_out, ddrA, portB_in, portB_out, ddrB;
    public static int ic43_a, ic43_b;
    public static final byte[] tokio_prot_data = new byte[] {
            0x6c,
            0x7f, 0x5f, 0x7f, 0x6f, 0x5f, 0x77, 0x5f, 0x7f, 0x5f, 0x7f, 0x5f, 0x7f, 0x5b, 0x7f, 0x5f, 0x7f,
            0x5f, 0x77, 0x59, 0x7f, 0x5e, 0x7e, 0x5f, 0x6d, 0x57, 0x7f, 0x5d, 0x7d, 0x5f, 0x7e, 0x5f, 0x7f,
            0x5d, 0x7d, 0x5f, 0x7e, 0x5e, 0x79, 0x5f, 0x7f, 0x5f, 0x7f, 0x5d, 0x7f, 0x5f, 0x7b, 0x5d, 0x7e,
            0x5f, 0x7f, 0x5d, 0x7d, 0x5f, 0x7e, 0x5e, 0x7e, 0x5f, 0x7d, 0x5f, 0x7f, 0x5f, 0x7e, 0x7f, 0x5f,
            0x01, 0x00, 0x02, 0x01, 0x01, 0x01, 0x03, 0x00, 0x05, 0x02, 0x04, 0x01, 0x03, 0x00, 0x05, 0x01,
            0x02, 0x03, 0x00, 0x04, 0x04, 0x01, 0x02, 0x00, 0x05, 0x03, 0x02, 0x01, 0x04, 0x05, 0x00, 0x03,
            0x00, 0x05, 0x02, 0x01, 0x03, 0x04, 0x05, 0x00, 0x01, 0x04, 0x04, 0x02, 0x01, 0x04, 0x01, 0x00,
            0x03, 0x01, 0x02, 0x05, 0x00, 0x03, 0x00, 0x01, 0x02, 0x00, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x00, 0x00, 0x00,
            0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0x02, 0x00, 0x01, 0x01, 0x00, 0x00, 0x02, 0x01, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x01
    };

    public static void bublbobl_bankswitch_w(byte data) {
        basebankmain = 0x1_0000 + 0x4000 * ((data ^ 4) & 7);
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), (data & 0x10) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        if (Cpuexec.ncpu == 4) {
            Cpuint.cpunum_set_input_line(3, LineState.INPUT_LINE_RESET.ordinal(), (data & 0x20) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        }
        bublbobl_video_enable = data & 0x40;
        Generic.flip_screen_set(data & 0x80);
    }

    public static void tokio_bankswitch_w(byte data) {
        basebankmain = 0x1_0000 + 0x4000 * (data & 7);
    }

    public static void tokio_videoctrl_w(byte data) {
        Generic.flip_screen_set(data & 0x80);
    }

    public static void bublbobl_nmitrigger_w() {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
    }

    public static byte tokio_mcu_r() {
        tokio_prot_count %= tokio_prot_data.length;
        return tokio_prot_data[tokio_prot_count++];
    }

    public static byte tokiob_mcu_r() {
        return (byte) 0xbf; // ad-hoc value set to pass initial testing
    }

    public static void nmi_callback() {
        if (sound_nmi_enable != 0) {
            Cpuint.cpunum_set_input_line(2, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
        } else {
            pending_nmi = 1;
        }
    }

    public static void bublbobl_sound_command_w(byte data) {
        Sound.soundlatch_w(data);
        Timer.setInternal(Taito::nmi_callback, "nmi_callback");
    }

    public static void bublbobl_sh_nmi_disable_w() {
        sound_nmi_enable = 0;
    }

    public static void bublbobl_sh_nmi_enable_w() {
        sound_nmi_enable = 1;
        if (pending_nmi != 0) {
            Cpuint.cpunum_set_input_line(2, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
            pending_nmi = 0;
        }
    }

    public static byte bublbobl_mcu_ddr1_r() {
        return ddr1;
    }

    public static void bublbobl_mcu_ddr1_w(byte data) {
        ddr1 = data;
    }

    public static byte bublbobl_mcu_ddr2_r() {
        return ddr2;
    }

    public static void bublbobl_mcu_ddr2_w(byte data) {
        ddr2 = data;
    }

    public static byte bublbobl_mcu_ddr3_r() {
        return ddr3;
    }

    public static void bublbobl_mcu_ddr3_w(byte data) {
        ddr3 = data;
    }

    public static byte bublbobl_mcu_ddr4_r() {
        return ddr4;
    }

    public static void bublbobl_mcu_ddr4_w(byte data) {
        ddr4 = data;
    }

    public static byte bublbobl_mcu_port1_r() {
        port1_in = sbyte0;
        return (byte) ((port1_out & ddr1) | (port1_in & ~ddr1));
    }

    public static void bublbobl_mcu_port1_w(byte data) {
//        coin_lockout_global_w(~data & 0x10);
        if ((port1_out & 0x40) != 0 && (~data & 0x40) != 0) {
            Cpuint.cpunum_set_input_line_vector(0, 0, bublbobl_mcu_sharedram[0]);
            Cpuint.cpunum_set_input_line(0, 0, LineState.HOLD_LINE);
        }
        port1_out = data;
    }

    public static byte bublbobl_mcu_port2_r() {
        return (byte) ((port2_out & ddr2) | (port2_in & ~ddr2));
    }

    public static void bublbobl_mcu_port2_w(byte data) {
        byte[] ports = new byte[] {dsw0, dsw1, sbyte1, sbyte2};
        if ((~port2_out & 0x10) != 0 && (data & 0x10) != 0) {
            int address = port4_out | ((data & 0x0f) << 8);
            if ((port1_out & 0x80) != 0) {
                if ((address & 0x0800) == 0x0000) {
                    port3_in = ports[address & 3];
                } else if ((address & 0x0c00) == 0x0c00) {
                    port3_in = bublbobl_mcu_sharedram[address & 0x03ff];
                }
            } else {
                if ((address & 0x0c00) == 0x0c00) {
                    bublbobl_mcu_sharedram[address & 0x03ff] = port3_out;
                }
            }
        }
        port2_out = data;
    }

    public static byte bublbobl_mcu_port3_r() {
        return (byte) ((port3_out & ddr3) | (port3_in & ~ddr3));
    }

    public static void bublbobl_mcu_port3_w(byte data) {
        port3_out = data;
    }

    public static byte bublbobl_mcu_port4_r() {
        return (byte) ((port4_out & ddr4) | (port4_in & ~ddr4));
    }

    public static void bublbobl_mcu_port4_w(byte data) {
        port4_out = data;
    }

    public static byte boblbobl_ic43_a_r(int offset) {
        if (offset == 0) {
            return (byte) (ic43_a << 4);
        } else {
            return 0;
        }
    }

    public static void boblbobl_ic43_a_w(int offset) {
        int res = 0;
        switch (offset) {
            case 0:
                if ((~ic43_a & 8) != 0) {
                    res ^= 1;
                }
                if ((~ic43_a & 1) != 0) {
                    res ^= 2;
                }
                if ((~ic43_a & 1) != 0) {
                    res ^= 4;
                }
                if ((~ic43_a & 2) != 0) {
                    res ^= 4;
                }
                if ((~ic43_a & 4) != 0) {
                    res ^= 8;
                }
                break;
            case 1:
                if ((~ic43_a & 8) != 0) {
                    res ^= 1;
                }
                if ((~ic43_a & 2) != 0) {
                    res ^= 1;
                }
                if ((~ic43_a & 8) != 0) {
                    res ^= 2;
                }
                if ((~ic43_a & 1) != 0) {
                    res ^= 4;
                }
                if ((~ic43_a & 4) != 0) {
                    res ^= 8;
                }
                break;
            case 2:
                if ((~ic43_a & 4) != 0) {
                    res ^= 1;
                }
                if ((~ic43_a & 8) != 0) {
                    res ^= 2;
                }
                if ((~ic43_a & 2) != 0) {
                    res ^= 4;
                }
                if ((~ic43_a & 1) != 0) {
                    res ^= 8;
                }
                if ((~ic43_a & 4) != 0) {
                    res ^= 8;
                }
                break;
            case 3:
                if ((~ic43_a & 2) != 0) {
                    res ^= 1;
                }
                if ((~ic43_a & 4) != 0) {
                    res ^= 2;
                }
                if ((~ic43_a & 8) != 0) {
                    res ^= 2;
                }
                if ((~ic43_a & 8) != 0) {
                    res ^= 4;
                }
                if ((~ic43_a & 1) != 0) {
                    res ^= 8;
                }
                break;
        }
        ic43_a = res;
    }

    public static void boblbobl_ic43_b_w(int offset, byte data) {
        int[] xor = new int[] {4, 1, 8, 2};
        ic43_b = (data >> 4) ^ xor[offset];
    }

    public static byte boblbobl_ic43_b_r(int offset) {
        if (offset == 0) {
            return (byte) (ic43_b << 4);
        } else {
            return (byte) 0xff;
        }
    }

    public static void bublbobl_m68705_interrupt() {
        if ((Cpuexec.iloops & 1) != 0) {
            Cpuint.cpunum_set_input_line(3, 0, LineState.CLEAR_LINE);
        } else {
            Cpuint.cpunum_set_input_line(3, 0, LineState.ASSERT_LINE);
        }
    }

    public static byte bublbobl_68705_portA_r() {
        return (byte) ((portA_out & ddrA) | (portA_in & ~ddrA));
    }

    public static void bublbobl_68705_portA_w(byte data) {
        portA_out = data;
    }

    public static void bublbobl_68705_ddrA_w(byte data) {
        ddrA = data;
    }

    public static byte bublbobl_68705_portB_r() {
        return (byte) ((portB_out & ddrB) | (portB_in & ~ddrB));
    }

    public static void bublbobl_68705_portB_w(byte data) {
        byte[] ports = new byte[] {dsw0, dsw1, sbyte1, sbyte2};
        if (((ddrB & 0x01) != 0) && ((~data & 0x01) != 0) && ((portB_out & 0x01) != 0)) {
            portA_in = (byte) latch;
        }
        if (((ddrB & 0x02) != 0) && ((data & 0x02) != 0) && ((~portB_out & 0x02) != 0)) {
            address = (address & 0xff00) | portA_out;
        }
        if (((ddrB & 0x04) != 0) && ((data & 0x04) != 0) && ((~portB_out & 0x04) != 0)) {
            address = (address & 0x00ff) | ((portA_out & 0x0f) << 8);
        }
        if (((ddrB & 0x10) != 0) && ((~data & 0x10) != 0) && ((portB_out & 0x10) != 0)) {
            if ((data & 0x08) != 0) {
                if ((address & 0x0800) == 0x0000) {
                    latch = ports[address & 3];
                } else if ((address & 0x0c00) == 0x0c00) {
                    latch = bublbobl_mcu_sharedram[address & 0x03ff];
                } else {
                }
            } else {
                if ((address & 0x0c00) == 0x0c00) {
                    bublbobl_mcu_sharedram[address & 0x03ff] = portA_out;
                } else {
                }
            }
        }
        if (((ddrB & 0x20) != 0) && ((~data & 0x20) != 0) && ((portB_out & 0x20) != 0)) {
            bublbobl_mcu_sharedram[0x7c] = 0;
            Cpuint.cpunum_set_input_line_vector(0, 0, bublbobl_mcu_sharedram[0]);
            Cpuint.cpunum_set_input_line(0, 0, LineState.HOLD_LINE);
        }
        if (((ddrB & 0x40) != 0) && ((~data & 0x40) != 0) && ((portB_out & 0x40) != 0)) {
        }
        if (((ddrB & 0x80) != 0) && ((~data & 0x80) != 0) && ((portB_out & 0x80) != 0)) {
        }
        portB_out = data;
    }

    public static void bublbobl_68705_ddrB_w(byte data) {
        ddrB = data;
    }

//#endregion
}
