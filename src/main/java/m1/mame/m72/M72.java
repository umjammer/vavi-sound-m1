/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.m72;


import java.awt.event.KeyEvent;
import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.nec.Nec;
import m1.cpu.z80.Z80A;
import m1.emu.Attotime;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Cpuint.vec;
import m1.emu.Generic;
import m1.emu.Inptport;
import m1.emu.Inptport.FrameArray;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.Memory;
import m1.emu.Palette;
import m1.emu.RomInfo;
import m1.emu.Tilemap.RECT;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.DAC;
import m1.sound.Sound;
import m1.sound.YM2151;


public class M72 {

    public static byte[] protection_ram;
    public static EmuTimer scanline_timer;
    public static byte m72_irq_base;
    public static int m72_scanline_param;

    public static byte[] spritesrom, sprites1rom, samplesrom, gfx2rom, gfx21rom, gfx3rom, gfx31rom;
    public static final byte[] airduelm72_code = new byte[] {
            0x68, 0x00, (byte) 0xd0, 0x1f, (byte) 0xc6, 0x06, (byte) 0xc0, 0x1c, 0x57, (byte) 0xea, 0x69, 0x0b, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    public static final byte[] gunforce_decryption_table = new byte[] {
            (byte) 0xff, (byte) 0x90, (byte) 0x90, 0x2c, (byte) 0x90, (byte) 0x90, 0x43, (byte) 0x88, (byte) 0x90, 0x13, 0x0a, (byte) 0xbd, (byte) 0xba, 0x60, (byte) 0xea, (byte) 0x90, /* 00 */
            (byte) 0x90, (byte) 0x90, (byte) 0xf2, 0x29, (byte) 0xb3, 0x22, (byte) 0x90, 0x0c, (byte) 0xa9, 0x5f, (byte) 0x9d, 0x07, (byte) 0x90, (byte) 0x90, 0x0b, (byte) 0xbb, /* 10 */
            (byte) 0x8a, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x3a, 0x3c, 0x5a, 0x38, (byte) 0x99, (byte) 0x90, (byte) 0xf8, (byte) 0x89, (byte) 0x90, (byte) 0x91, (byte) 0x90, 0x55, /* 20 */
            (byte) 0xac, 0x40, 0x73, (byte) 0x90, 0x59, (byte) 0x90, (byte) 0xfc, (byte) 0x90, 0x50, (byte) 0xfa, (byte) 0x90, 0x25, (byte) 0x90, 0x34, 0x47, (byte) 0xb7, /* 30 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x49, (byte) 0x90, 0x0f, (byte) 0x8b, 0x05, (byte) 0xc3, (byte) 0xa5, (byte) 0xbf, (byte) 0x83, (byte) 0x86, (byte) 0xc5, (byte) 0x90, (byte) 0x90, /* 40 */
            0x28, 0x77, 0x24, (byte) 0xb4, (byte) 0x90, (byte) 0x92, (byte) 0x90, 0x3b, 0x5e, (byte) 0xb6, (byte) 0x80, 0x0d, 0x2e, (byte) 0xab, (byte) 0xe7, (byte) 0x90, /* 50 */
            0x48, (byte) 0x90, (byte) 0xad, (byte) 0xc0, (byte) 0x90, 0x1b, (byte) 0xc6, (byte) 0xa3, 0x04, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x16, (byte) 0xb0, 0x7d, (byte) 0x98, /* 60 */
            (byte) 0x87, 0x46, (byte) 0x8c, (byte) 0x90, (byte) 0x90, (byte) 0xfe, (byte) 0x90, (byte) 0xcf, (byte) 0x90, 0x68, (byte) 0x84, (byte) 0x90, (byte) 0xd2, (byte) 0x90, 0x18, 0x51, /* 70 */
            0x76, (byte) 0xa4, 0x36, 0x52, (byte) 0xfb, (byte) 0x90, (byte) 0xb9, (byte) 0x90, (byte) 0x90, (byte) 0xb1, 0x1c, 0x21, (byte) 0xe6, (byte) 0xb5, 0x17, 0x27, /* 80 */
            0x3d, 0x45, (byte) 0xbe, (byte) 0xae, (byte) 0x90, 0x4a, 0x0e, (byte) 0xe5, (byte) 0x90, 0x58, 0x1f, 0x61, (byte) 0xf3, 0x02, (byte) 0x90, (byte) 0xe8, /* 90 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xf7, 0x56, (byte) 0x96, (byte) 0x90, (byte) 0xbc, 0x4f, (byte) 0x90, (byte) 0x90, 0x79, (byte) 0xd0, (byte) 0x90, 0x2a, 0x12, /* A0 */
            0x4e, (byte) 0xb8, (byte) 0x90, 0x41, (byte) 0x90, (byte) 0x90, (byte) 0xd3, (byte) 0x90, 0x2d, 0x33, (byte) 0xf6, (byte) 0x90, (byte) 0x90, 0x14, (byte) 0x90, 0x32, /* B0 */
            0x5d, (byte) 0xa8, 0x53, 0x26, 0x2b, 0x20, (byte) 0x81, 0x75, 0x7f, 0x3e, (byte) 0x90, (byte) 0x90, 0x00, (byte) 0x93, (byte) 0x90, (byte) 0xb2, /* C0 */
            0x57, (byte) 0x90, (byte) 0xa0, (byte) 0x90, 0x39, (byte) 0x90, (byte) 0x90, 0x72, (byte) 0x90, 0x01, 0x42, 0x74, (byte) 0x9c, 0x1e, (byte) 0x90, 0x5b, /* D0 */
            (byte) 0x90, (byte) 0xf9, (byte) 0x90, 0x2f, (byte) 0x85, (byte) 0x90, (byte) 0xeb, (byte) 0xa2, (byte) 0x90, (byte) 0xe2, 0x11, (byte) 0x90, 0x4b, 0x7e, (byte) 0x90, 0x78, /* E0 */
            (byte) 0x90, (byte) 0x90, 0x09, (byte) 0xa1, 0x03, (byte) 0x90, 0x23, (byte) 0xc1, (byte) 0x8e, (byte) 0xe9, (byte) 0xd1, 0x7c, (byte) 0x90, (byte) 0x90, (byte) 0xc7, 0x06, /* F0 */
    };
    public static final byte[] airduelm72_crc = new byte[] {0x72, (byte) 0x9c, (byte) 0xca, (byte) 0x85, (byte) 0xc9, 0x12, (byte) 0xcc, (byte) 0xea, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public static void M72Init() {
        int i1, i2, i3, n1, n2, n3;
        Generic.paletteram16 = new short[0x600];
        Generic.paletteram16_2 = new short[0x600];
        Generic.spriteram16 = new short[0x200];
        Machine.bRom = true;
        Timer.setvector = M72::setvector_callback;
        protection_ram = new byte[0x1000];
        Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
        Memory.audiorom = Machine.GetRom("soundcpu.rom");
        spritesrom = new byte[0x8_0000];// Machine.GetRom("sprites.rom");
        n1 = spritesrom.length;
        sprites1rom = new byte[n1 * 2];
        for (i1 = 0; i1 < n1; i1++) {
            sprites1rom[i1 * 2] = (byte) (spritesrom[i1] >> 4);
            sprites1rom[i1 * 2 + 1] = (byte) (spritesrom[i1] & 0x0f);
        }
        gfx2rom = new byte[0x1_0000];// Machine.GetRom("gfx2.rom");
        n2 = gfx2rom.length;
        gfx21rom = new byte[n2 * 2];
        for (i2 = 0; i2 < n2; i2++) {
            gfx21rom[i2 * 2] = (byte) (gfx2rom[i2] >> 4);
            gfx21rom[i2 * 2 + 1] = (byte) (gfx2rom[i2] & 0x0f);
        }
        gfx3rom = new byte[0x1_0000];// Machine.GetRom("gfx3.rom");
        if (gfx3rom != null) {
            n3 = gfx3rom.length;
            gfx31rom = new byte[n3 * 2];
            for (i3 = 0; i3 < n3; i3++) {
                gfx31rom[i3 * 2] = (byte) (gfx3rom[i3] >> 4);
                gfx31rom[i3 * 2 + 1] = (byte) (gfx3rom[i3] & 0x0f);
            }
        }
        samplesrom = Machine.GetRom("samples.rom");
        Memory.mainram = new byte[0x4000];
        switch (Machine.sName) {
            case "airduelm72":
                Memory.audioram = Machine.GetRom("audioram.rom");
                break;
            case "ltswords":
                Memory.audioram = new byte[0x1_0000];
                break;
        }
        dsw = (short) 0xffbf;
        if (Memory.mainrom == null || Memory.audiorom == null || sprites1rom == null || gfx21rom == null || samplesrom == null) {
            Machine.bRom = false;
        }
    }

    public static byte protection_r(byte[] protection_code, int offset) {
        System.arraycopy(protection_code, 0, protection_ram, 0, 96);
        return protection_ram[0xffa + offset];
    }

    public static short protection_r2(byte[] protection_code, int offset) {
        System.arraycopy(protection_code, 0, protection_ram, 0, 96);
        return (short) (protection_ram[0xffa + offset] + protection_ram[0xffa + 1 + offset] * 0x100);
    }

    public static void protection_w(byte[] protection_crc, int offset, byte data) {
        data ^= (byte) 0xff;
        protection_ram[offset] = data;
        data ^= (byte) 0xff;
        if (offset == 0xfff && data == 0) {
            System.arraycopy(protection_crc, 0, protection_ram, 0xfe0, 18);
        }
    }

    public static void protection_w(byte[] protection_crc, int offset, short data) {
        data ^= (short) 0xffff;
        protection_ram[offset * 2] = (byte) data;
        protection_ram[offset * 2 + 1] = (byte) (data >> 8);
        data ^= (short) 0xffff;
        if (offset == 0x0fff / 2 && (data >> 8) == 0) {
            System.arraycopy(protection_crc, 0, protection_ram, 0xfe0, 18);
        }
    }

    public static void fake_nmi() {
        byte sample = m72_sample_r();
        if (sample != 0) {
            m72_sample_w(sample);
        }
    }

    public static void airduelm72_sample_trigger_w(byte data) {
        int[] a = {
                0x0_0000, 0x0_0020, 0x0_3ec0, 0x0_5640, 0x0_6dc0, 0x0_83a0, 0x0_c000, 0x0_eb60,
                0x1_12e0, 0x1_3dc0, 0x1_6520, 0x1_6d60, 0x1_8ae0, 0x1_a5a0, 0x1_bf00, 0x1_c340
        };
        if ((data & 0xff) < 16) {
            m72_set_sample_start(a[data & 0xff]);
        }
    }

    public static byte soundram_r(int offset) {
        return Memory.audioram[offset];
    }

    public static short soundram_r2(int offset) {
        return (short) (Memory.audioram[offset * 2 + 0] | (Memory.audioram[offset * 2 + 1] << 8));
    }

    public static void soundram_w(int offset, byte data) {
        Memory.audioram[offset] = data;
    }

    public static void soundram_w(int offset, short data) {
        Memory.audioram[offset * 2] = (byte) data;
        Memory.audioram[offset * 2 + 1] = (byte) (data >> 8);
    }

    public static void machine_start_m72() {
        scanline_timer = Timer.allocCommon(M72::m72_scanline_interrupt, "m72_scanline_interrupt", false);
    }

    public static void machine_reset_m72() {
        m72_irq_base = 0x20;
        machine_reset_m72_sound();
        Timer.adjustPeriodic(scanline_timer, Video.video_screen_get_time_until_pos(0, 0), Attotime.ATTOTIME_NEVER);
    }

    public static void machine_reset_kengo() {
        m72_irq_base = 0x18;
        machine_reset_m72_sound();
        Timer.adjustPeriodic(scanline_timer, Video.video_screen_get_time_until_pos(0, 0), Attotime.ATTOTIME_NEVER);
    }

    public static void m72_scanline_interrupt() {
        int scanline = m72_scanline_param;
        if (scanline < 256 && scanline == m72_raster_irq_position - 128) {
            Video.video_screen_update_partial(scanline);
            Cpuexec.cpu[0].cpunum_set_input_line_and_vector(0, 0, LineState.HOLD_LINE, m72_irq_base + 2);
        } else if (scanline == 256) {
            Video.video_screen_update_partial(scanline);
            Cpuexec.cpu[0].cpunum_set_input_line_and_vector(0, 0, LineState.HOLD_LINE, m72_irq_base + 0);
        }
        if (++scanline >= Video.screenstate.height) {
            scanline = 0;
        }
        m72_scanline_param = scanline;
        Timer.adjustPeriodic(scanline_timer, Video.video_screen_get_time_until_pos(scanline, 0), Attotime.ATTOTIME_NEVER);
    }

    public static void play_m72_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_m72_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_m72_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//#region Memory

    public static short short0, short1, dsw;
    public static short short0_old, short1_old;

    public static byte NReadOpByte(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0xf_ffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte NReadByte_m72(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x7_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0xa_0000 && address <= 0xa_3fff) {
            result = Memory.mainram[address - 0xa_0000];
        } else if (address >= 0xc_0000 && address <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            result = (byte) Generic.spriteram16[offset];
        } else if (address >= 0xc_8000 && address <= 0xc_8bff) {
            int offset = (address - 0xc_8000) / 2;
            result = (byte) m72_palette1_r(offset);
        } else if (address >= 0xc_c000 && address <= 0xc_cbff) {
            int offset = (address - 0xc_c000) / 2;
            result = (byte) m72_palette2_r(offset);
        } else if (address >= 0xd_0000 && address <= 0xd_3fff) {
            int offset = address - 0xd_0000;
            result = m72_videoram1[offset];
        } else if (address >= 0xd_8000 && address <= 0xd_bfff) {
            int offset = address - 0xd_8000;
            result = m72_videoram2[offset];
        } else if (address >= 0xe_0000 && address <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            result = soundram_r(offset);
        } else if (address >= 0xf_fff0 && address <= 0xf_ffff) {
            result = Memory.mainrom[address & 0xf_ffff];
        }
        return result;
    }

    public static short NReadWord_m72(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x7_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        } else if (address >= 0xa_0000 && address + 1 <= 0xa_3fff) {
            result = (short) (Memory.mainram[address - 0xa_0000] + Memory.mainram[address - 0xa_0000 + 1] * 0x100);
        } else if (address >= 0xc_0000 && address + 1 <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            result = Generic.spriteram16[offset];
        } else if (address >= 0xc_8000 && address + 1 <= 0xc_8bff) {
            int offset = (address - 0xc_8000) / 2;
            result = m72_palette1_r(offset);
        } else if (address >= 0xc_c000 && address + 1 <= 0xc_cbff) {
            int offset = (address - 0xc_c000) / 2;
            result = m72_palette2_r(offset);
        } else if (address >= 0xd_0000 && address + 1 <= 0xd_3fff) {
            int offset = address - 0xd_0000;
            result = (short) (m72_videoram1[offset] + m72_videoram1[offset + 1] * 0x100);
        } else if (address >= 0xd_8000 && address + 1 <= 0xd_bfff) {
            int offset = address - 0xd_8000;
            result = (short) (m72_videoram2[offset] + m72_videoram2[offset + 1] * 0x100);
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_ffff) {
            int offset = (address - 0xe_0000) / 2;
            result = soundram_r2(offset);
        } else if (address >= 0xf_fff0 && address + 1 <= 0xf_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        }
        return result;
    }

    public static void NWriteByte_m72(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0xa_0000 && address <= 0xa_3fff) {
            Memory.mainram[address - 0xa_0000] = value;
        } else if (address >= 0xc_0000 && address <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xc_8000 && address <= 0xc_8bff) {
            int offset = (address - 0xc_8000) / 2;
            m72_palette1_w(offset, value);
        } else if (address >= 0xc_c000 && address <= 0xc_cbff) {
            int offset = (address - 0xc_c000) / 2;
            m72_palette2_w(offset, value);
        } else if (address >= 0xd_0000 && address <= 0xd_3fff) {
            int offset = address - 0xd_0000;
            m72_videoram1_w(offset, value);
        } else if (address >= 0xd_8000 && address <= 0xd_bfff) {
            int offset = address - 0xd_8000;
            m72_videoram2_w(offset, value);
        } else if (address >= 0xe_0000 && address <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            soundram_w(offset, value);
        }
    }

    public static void NWriteWord_m72(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0xa_0000 && address + 1 <= 0xa_3fff) {
            Memory.mainram[address - 0xa_0000] = (byte) value;
            Memory.mainram[address - 0xa_0000 + 1] = (byte) (value >> 8);
        } else if (address >= 0xc_0000 && address + 1 <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xc_8000 && address + 1 <= 0xc_8bff) {
            int offset = (address - 0xc_8000) / 2;
            m72_palette1_w(offset, value);
        } else if (address >= 0xc_c000 && address + 1 <= 0xc_cbff) {
            int offset = (address - 0xc_c000) / 2;
            m72_palette2_w(offset, value);
        } else if (address >= 0xd_0000 && address + 1 <= 0xd_3fff) {
            int offset = (address - 0xd_0000) / 2;
            m72_videoram1_w(offset, value);
        } else if (address >= 0xd_8000 && address + 1 <= 0xd_bfff) {
            int offset = (address - 0xd_8000) / 2;
            m72_videoram2_w(offset, value);
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_ffff) {
            int offset = (address - 0xe_0000) / 2;
            soundram_w(offset, value);
        }
    }

    public static byte NReadIOByte(int address) {
        byte result = 0;
        if (address >= 0x00 && address <= 0x01) {
            result = (byte) short0;
        } else if (address >= 0x02 && address <= 0x03) {
            result = (byte) short1;
        } else if (address >= 0x04 && address <= 0x05) {
            result = (byte) dsw;
        }
        return result;
    }

    public static short NReadIOWord(int address) {
        short result = 0;
        if (address >= 0x00 && address + 1 <= 0x01) {
            result = short0;
        } else if (address >= 0x02 && address + 1 <= 0x03) {
            result = short1;
        } else if (address >= 0x04 && address + 1 <= 0x05) {
            result = dsw;
        }
        return result;
    }

    public static void NWriteIOByte_m72(int address, byte value) {
        if (address >= 0x00 && address <= 0x01) {
            m72_sound_command_w(0, value);
        } else if (address >= 0x02 && address <= 0x03) {
            m72_port02_w(value);
        } else if (address >= 0x04 && address <= 0x05) {
            m72_dmaon_w(value);
        } else if (address >= 0x06 && address <= 0x07) {
            m72_irq_line_w(value);
        } else if (address >= 0x40 && address <= 0x43) {

        } else if (address >= 0x80 && address <= 0x81) {
            m72_scrolly1_w(value);
        } else if (address >= 0x82 && address <= 0x83) {
            m72_scrollx1_w(value);
        } else if (address >= 0x84 && address <= 0x85) {
            m72_scrolly2_w(value);
        } else if (address >= 0x86 && address <= 0x87) {
            m72_scrollx2_w(value);
        }
    }

    public static void NWriteIOWord_m72(int address, short value) {
        if (address >= 0x00 && address + 1 <= 0x01) {
            m72_sound_command_w(0, value);
        } else if (address >= 0x02 && address + 1 <= 0x03) {
            m72_port02_w(value);
        } else if (address >= 0x04 && address + 1 <= 0x05) {
            m72_dmaon_w(value);
        } else if (address >= 0x06 && address + 1 <= 0x07) {
            m72_irq_line_w(value);
        } else if (address >= 0x40 && address + 1 <= 0x43) {

        } else if (address >= 0x80 && address + 1 <= 0x81) {
            m72_scrolly1_w(value);
        } else if (address >= 0x82 && address + 1 <= 0x83) {
            m72_scrollx1_w(value);
        } else if (address >= 0x84 && address + 1 <= 0x85) {
            m72_scrolly2_w(value);
        } else if (address >= 0x86 && address + 1 <= 0x87) {
            m72_scrollx2_w(value);
        }
    }

    public static byte NReadByte_kengo(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x7_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x8_0000 && address <= 0x8_3fff) {
            int offset = address - 0x8_0000;
            result = m72_videoram1[offset];
        } else if (address >= 0x8_4000 && address <= 0x8_7fff) {
            int offset = address - 0x8_4000;
            result = m72_videoram2[offset];
        } else if (address >= 0xa_0000 && address <= 0xa_0bff) {
            int offset = (address - 0xa_0000) / 2;
            result = (byte) m72_palette1_r(offset);
        } else if (address >= 0xa_8000 && address <= 0xa_8bff) {
            int offset = (address - 0xa_8000) / 2;
            result = (byte) m72_palette2_r(offset);
        } else if (address >= 0xc_0000 && address <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            result = (byte) Generic.spriteram16[offset];
        } else if (address >= 0xe_0000 && address <= 0xe_3fff) {
            result = Memory.mainram[address - 0xe_0000];
        } else if (address >= 0xf_fff0 && address <= 0xf_ffff) {
            result = Memory.mainrom[address & 0xf_ffff];
        }
        return result;
    }

    public static short NReadWord_kengo(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x7_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        } else if (address >= 0x8_0000 && address + 1 <= 0x8_3fff) {
            int offset = address - 0x8_0000;
            result = (short) (m72_videoram1[offset] + m72_videoram1[offset + 1] * 0x100);
        } else if (address >= 0x8_4000 && address + 1 <= 0x8_7fff) {
            int offset = address - 0x8_4000;
            result = (short) (m72_videoram2[offset] + m72_videoram2[offset + 1] * 0x100);
        } else if (address >= 0xa_0000 && address + 1 <= 0xa_0bff) {
            int offset = (address - 0xa_0000) / 2;
            result = m72_palette1_r(offset);
        } else if (address >= 0xa_8000 && address + 1 <= 0xa_8bff) {
            int offset = (address - 0xa_8000) / 2;
            result = m72_palette2_r(offset);
        } else if (address >= 0xc_0000 && address + 1 <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            result = Generic.spriteram16[offset];
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_3fff) {
            result = (short) (Memory.mainram[address - 0xe_0000] + Memory.mainram[address - 0xe_0000 + 1] * 0x100);
        } else if (address >= 0xf_fff0 && address + 1 <= 0xf_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        }
        return result;
    }

    public static void NWriteByte_kengo(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0x8_0000 && address <= 0x8_3fff) {
            int offset = address - 0x8_0000;
            m72_videoram1_w(offset, value);
        } else if (address >= 0x8_4000 && address <= 0x8_7fff) {
            int offset = address - 0x8_4000;
            m72_videoram2_w(offset, value);
        } else if (address >= 0xa_0000 && address <= 0xa_0bff) {
            int offset = (address - 0xa_0000) / 2;
            m72_palette1_w(offset, value);
        } else if (address >= 0xa_8000 && address <= 0xa_8bff) {
            int offset = (address - 0xa_8000) / 2;
            m72_palette2_w(offset, value);
        } else if (address >= 0xb_0000 && address <= 0xb_0001) {
            m72_irq_line_w(value);
        } else if (address >= 0xb_c000 && address <= 0xb_c001) {
            m72_dmaon_w(value);
        } else if (address >= 0xc_0000 && address <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xe_0000 && address <= 0xe_3fff) {
            Memory.mainram[address - 0xe_0000] = value;
        }
    }

    public static void NWriteWord_kengo(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0x8_0000 && address + 1 <= 0x8_3fff) {
            int offset = (address - 0x8_0000) / 2;
            m72_videoram1_w(offset, value);
        } else if (address >= 0x8_4000 && address + 1 <= 0x8_7fff) {
            int offset = (address - 0x8_4000) / 2;
            m72_videoram2_w(offset, value);
        } else if (address >= 0xa_0000 && address + 1 <= 0xa_0bff) {
            int offset = (address - 0xa_0000) / 2;
            m72_palette1_w(offset, value);
        } else if (address >= 0xa_8000 && address + 1 <= 0xa_8bff) {
            int offset = (address - 0xa_8000) / 2;
            m72_palette2_w(offset, value);
        } else if (address >= 0xb_0000 && address + 1 <= 0xb_0001) {
            m72_irq_line_w(value);
        } else if (address >= 0xb_c000 && address + 1 <= 0xb_c001) {
            m72_dmaon_w(value);
        } else if (address >= 0xc_0000 && address + 1 <= 0xc_03ff) {
            int offset = (address - 0xc_0000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_3fff) {
            Memory.mainram[address - 0xe_0000] = (byte) value;
            Memory.mainram[address - 0xe_0000 + 1] = (byte) (value >> 8);
        }
    }

    public static void NWriteIOByte_kengo(int address, byte value) {
        if (address >= 0x00 && address <= 0x01) {
            m72_sound_command_w(0, value);
        } else if (address >= 0x02 && address <= 0x03) {
            rtype2_port02_w(value);
        } else if (address >= 0x80 && address <= 0x81) {
            m72_scrolly1_w(value);
        } else if (address >= 0x82 && address <= 0x83) {
            m72_scrollx1_w(value);
        } else if (address >= 0x84 && address <= 0x85) {
            m72_scrolly2_w(value);
        } else if (address >= 0x86 && address <= 0x87) {
            m72_scrollx2_w(value);
        } else {
            int i1 = 1;
        }
    }

    public static void NWriteIOWord_kengo(int address, short value) {
        if (address >= 0x00 && address + 1 <= 0x01) {
            m72_sound_command_w(0, value);
        } else if (address >= 0x02 && address + 1 <= 0x03) {
            rtype2_port02_w(value);
        } else if (address >= 0x80 && address + 1 <= 0x81) {
            m72_scrolly1_w(value);
        } else if (address >= 0x82 && address + 1 <= 0x83) {
            m72_scrollx1_w(value);
        } else if (address >= 0x84 && address + 1 <= 0x85) {
            m72_scrolly2_w(value);
        } else if (address >= 0x86 && address + 1 <= 0x87) {
            m72_scrollx2_w(value);
        } else {
            int i1 = 1;
        }
    }

    public static byte ZReadOp(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0xffff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static byte ZReadMemory_ram(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0xffff) {
            result = Memory.audioram[address];
        }
        return result;
    }

    public static byte ZReadMemory_rom(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0xefff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf000 && address <= 0xffff) {
            result = Memory.audioram[address - 0xf000];
        }
        return result;
    }

    public static void ZWriteMemory_ram(short address, byte value) {
        if (address >= 0x0000 && address <= 0xffff) {
            Memory.audioram[address] = value;
        }
    }

    public static void ZWriteMemory_rom(short address, byte value) {
        if (address >= 0xf000 && address <= 0xffff) {
            Memory.audioram[address - 0xf000] = value;
        }
    }

    public static byte ZReadHardware(short address) {
        byte result = 0;
        address &= 0xff;
        if (address == 0x01) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address == 0x02) {
            result = (byte) Sound.soundlatch_r();
        } else if (address == 0x84) {
            result = m72_sample_r();
        }

        return result;
    }

    public static byte ZReadHardware_rtype2(short address) {
        byte result = 0;
        address &= 0xff;
        if (address == 0x01) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address == 0x80) {
            result = (byte) Sound.soundlatch_r();
        } else if (address == 0x84) {
            result = m72_sample_r();
        }

        return result;
    }

    public static void ZWriteHardware(short address, byte value) {
        address &= 0xff;
        if (address == 0x00) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0x01) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address == 0x06) {
            m72_sound_irq_ack_w(0, value);
        } else if (address == 0x82) {
            m72_sample_w(value);
        }
    }

    public static void ZWriteHardware_rtype2(short address, byte value) {
        address &= 0xff;
        if (address == 0x00) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0x01) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address >= 0x80 && address <= 0x81) {
            int offset = address - 0x80;
            rtype2_sample_addr_w(offset, value);
        } else if (address == 0x82) {
            m72_sample_w(value);
        } else if (address == 0x83) {
            m72_sound_irq_ack_w(0, value);
        }
    }

    public static int ZIRQCallback() {
        return Cpuint.cpu_irq_callback(1, 0);
    }

//#endregion

//#region Memory2

    public static byte NReadOpByte_airduel(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0xb_0000 && address <= 0xb_0fff) {
            int offset = address - 0xb_0000;
            result = protection_ram[offset];
        } else {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte NReadByte_m72_airduel(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0xb_0ffa && address <= 0xb_0ffb) {
            int offset = address - 0xb_0ffa;
            System.arraycopy(airduelm72_code, 0, protection_ram, 0, 96);
            result = protection_ram[0xffa + offset];
        } else if (address >= 0xb_0000 && address <= 0xb_0fff) {
            int offset = address - 0xb_0000;
            result = protection_ram[offset];
        } else {
            result = NReadByte_m72(address);
        }
        return result;
    }

    public static short NReadWord_m72_airduel(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0xb_0ffa && address + 1 <= 0xb_0ffb) {
            int offset = address - 0xb_0ffa;
            System.arraycopy(airduelm72_code, 0, protection_ram, 0, 96);
            result = (short) (protection_ram[0xffa + offset] + protection_ram[0xffa + offset + 1] * 0x100);
        } else if (address >= 0xb_0000 && address + 1 <= 0xb_0fff) {
            int offset = address - 0xb_0000;
            result = (short) (protection_ram[offset] + protection_ram[offset + 1] * 0x100);
        } else {
            result = NReadWord_m72(address);
        }
        return result;
    }

    public static void NWriteByte_m72_airduel(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0xb_0000 && address <= 0xb_0fff) {
            int offset = address - 0xb_0000;
            protection_w(airduelm72_crc, offset, value);
        } else {
            NWriteByte_m72(address, value);
        }
    }

    public static void NWriteWord_m72_airduel(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0xb_0000 && address + 1 <= 0xb_0fff) {
            int offset = (address - 0xb_0000) / 2;
            protection_w(airduelm72_crc, offset, value);
        } else {
            NWriteWord_m72(address, value);
        }
    }

    public static void NWriteIOByte_m72_airduel(int address, byte data) {
        if (address >= 0xc0 && address <= 0xc1) {
            airduelm72_sample_trigger_w(data);
        } else {
            NWriteIOByte_m72(address, data);
        }
    }

    public static void NWriteIOWord_m72_airduel(int address, short data) {
        if (address >= 0xc0 && address + 1 <= 0xc1) {
            airduelm72_sample_trigger_w((byte) data);
        } else {
            NWriteIOWord_m72(address, data);
        }
    }

//#endregion

//#region State

    public static void SaveStateBinary(BinaryWriter writer) {
        int i;
        writer.write(dsw);
        writer.write(setvector_param);
        writer.write(irqvector);
        writer.write(sample_addr);
        writer.write(protection_ram, 0, 0x1000);
        writer.write(m72_irq_base);
        writer.write(m72_scanline_param);
        for (i = 0; i < 0x600; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x600; i++) {
            writer.write(Generic.paletteram16_2[i]);
        }
        for (i = 0; i < 0x200; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        writer.write(m72_videoram1, 0, 0x4000);
        writer.write(m72_videoram2, 0, 0x4000);
        writer.write(m72_raster_irq_position);
        writer.write(video_off);
        writer.write(scrollx1);
        writer.write(scrolly1);
        writer.write(scrollx2);
        writer.write(scrolly2);
        for (i = 0; i < 0x200; i++) {
            writer.write(m72_spriteram[i]);
        }
        //majtitle_rowscrollram spriteram_size majtitle_rowscroll
        for (i = 0; i < 0x201; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        Nec.nn1[0].SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x1_0000);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        Cpuint.SaveStateBinary_v(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        writer.write(DAC.dac1.output);
        writer.write(Sound.latched_value[0]);
        writer.write(Sound.utempdata[0]);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.dacstream.output_sampindex);
        writer.write(Sound.dacstream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i;
            dsw = reader.readUInt16();
            setvector_param = reader.readInt32();
            irqvector = reader.readByte();
            sample_addr = reader.readInt32();
            protection_ram = reader.readBytes(0x1000);
            m72_irq_base = reader.readByte();
            m72_scanline_param = reader.readInt32();
            for (i = 0; i < 0x600; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x600; i++) {
                Generic.paletteram16_2[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x200; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            m72_videoram1 = reader.readBytes(0x4000);
            m72_videoram2 = reader.readBytes(0x4000);
            m72_raster_irq_position = reader.readInt32();
            video_off = reader.readInt32();
            scrollx1 = reader.readInt32();
            scrolly1 = reader.readInt32();
            scrollx2 = reader.readInt32();
            scrolly2 = reader.readInt32();
            for (i = 0; i < 0x200; i++) {
                m72_spriteram[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x201; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            Nec.nn1[0].LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x1_0000);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Cpuint.LoadStateBinary_v(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            DAC.dac1.output = reader.readInt16();
            Sound.latched_value[0] = reader.readUInt16();
            Sound.utempdata[0] = reader.readUInt16();
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.dacstream.output_sampindex = reader.readInt32();
            Sound.dacstream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Input

    public static void loop_inputports_m72_common() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short1 &= (short) ~0x0004;
        } else {
            short1 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short1 &= (short) ~0x0008;
        } else {
            short1 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short1 &= (short) ~0x0001;
        } else {
            short1 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short1 &= (short) ~0x0002;
        } else {
            short1 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            short0 &= (short) ~0x0001;
        } else {
            short0 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short0 &= (short) ~0x0002;
        } else {
            short0 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short0 &= (short) ~0x0004;
        } else {
            short0 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short0 &= (short) ~0x0008;
        } else {
            short0 |= 0x0008;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short0 &= (short) ~0x0080;
        } else {
            short0 |= 0x0080;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short0 &= (short) ~0x0040;
        } else {
            short0 |= 0x0040;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short0 &= (short) ~0x0020;
        } else {
            short0 |= 0x0020;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short0 &= (short) ~0x0010;
        } else {
            short0 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            short0 &= (short) ~0x0100;
        } else {
            short0 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            short0 &= (short) ~0x0200;
        } else {
            short0 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            short0 &= (short) ~0x0400;
        } else {
            short0 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            short0 &= (short) ~0x0800;
        } else {
            short0 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short0 &= (short) ~0x8000;
        } else {
            short0 |= (short) 0x8000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short0 &= (short) ~0x4000;
        } else {
            short0 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short0 &= (short) ~0x2000;
        } else {
            short0 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            short0 &= (short) ~0x1000;
        } else {
            short0 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            short1 &= (short) ~0x0010;
        } else {
            short1 |= 0x0010;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            short1 &= (short) ~0x0020;
        } else {
            short1 |= 0x0020;
        }
    }

    public static void record_port() {
        if (short0 != short0_old || short1 != short1_old) {
            short0_old = short0;
            short1_old = short1;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(short0);
            Mame.bwRecord.write(short1);
        }
    }

    public static void replay_port() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                short0_old = Mame.brRecord.readUInt16();
                short1_old = Mame.brRecord.readUInt16();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            short0 = short0_old;
            short1 = short1_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Audio

    public static int setvector_param;
    public static byte irqvector;
    public static int sample_addr;

    public static void setvector_callback() {
        switch (setvector_param) {
            case 0:
                irqvector = (byte) 0xff;
                break;
            case 1:
                irqvector &= (byte) 0xef;
                break;
            case 2:
                irqvector |= 0x10;
                break;
            case 3:
                irqvector &= (byte) 0xdf;
                break;
            case 4:
                irqvector |= 0x20;
                break;
        }
        Cpuint.interrupt_vector[1][0] = irqvector;
        if (irqvector == (byte) 0xff) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.CLEAR_LINE);
        } else {
            Cpuint.cpunum_set_input_line(1, 0, LineState.ASSERT_LINE);
        }
    }

    public static void machine_reset_m72_sound() {
        setvector_param = 0;
        setvector_callback();
    }

    public static void m72_ym2151_irq_handler(int irq) {
        if (irq != 0) {
            Cpuint.lvec.add(new vec(1, Timer.getCurrentTime()));
            setvector_param = 1;
            EmuTimer timer = Timer.allocCommon(M72::setvector_callback, "setvector_callback", true);
            Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        } else {
            Cpuint.lvec.add(new vec(2, Timer.getCurrentTime()));
            setvector_param = 2;
            EmuTimer timer = Timer.allocCommon(M72::setvector_callback, "setvector_callback", true);
            Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        }
    }

    public static void m72_sound_command_w(int offset, short data) {
        //if (ACCESSING_BITS_0_7)
        {
            Sound.soundlatch_w(data);
            Cpuint.lvec.add(new vec(3, Timer.getCurrentTime()));
            setvector_param = 3;
            EmuTimer timer = Timer.allocCommon(M72::setvector_callback, "setvector_callback", true);
            Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        }
    }

    public static void m72_sound_command_byte_w(int offset, byte data) {
        Sound.soundlatch_w(data);
        Cpuint.lvec.add(new vec(3, Timer.getCurrentTime()));
        setvector_param = 3;
        EmuTimer timer = Timer.allocCommon(M72::setvector_callback, "setvector_callback", true);
        Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
    }

    public static void m72_sound_irq_ack_w(int offset, byte data) {
        Cpuint.lvec.add(new vec(4, Timer.getCurrentTime()));
        setvector_param = 4;
        EmuTimer timer = Timer.allocCommon(M72::setvector_callback, "setvector_callback", true);
        Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
    }

    public static void m72_set_sample_start(int start) {
        sample_addr = start;
    }

    public static void rtype2_sample_addr_w(int offset, byte data) {
        sample_addr >>= 5;
        if (offset == 1) {
            sample_addr = (sample_addr & 0x00ff) | ((data << 8) & 0xff00);
        } else {
            sample_addr = (sample_addr & 0xff00) | ((data << 0) & 0x00ff);
        }
        sample_addr <<= 5;
    }

    public static byte m72_sample_r() {
        return samplesrom[sample_addr];
    }

    public static void m72_sample_w(byte data) {
        DAC.dac_signed_data_w(0, data);
        sample_addr = (sample_addr + 1) & (samplesrom.length - 1);
    }

//#endregion

//#region Video

    public static byte[] m72_videoram1, m72_videoram2;
    public static short[] majtitle_rowscrollram;
    public static int m72_raster_irq_position;
    public static short[] m72_spriteram;
    private static short[] uuB200;
    public static int scrollx1, scrolly1, scrollx2, scrolly2;
    public static int video_off, spriteram_size, majtitle_rowscroll;

    public static short m72_palette1_r(int offset) {
        offset &= ~0x100;
        return (short) (Generic.paletteram16[offset] | 0xffe0);
    }

    public static short m72_palette2_r(int offset) {
        offset &= ~0x100;
        return (short) (Generic.paletteram16_2[offset] | 0xffe0);
    }

    public static void changecolor(int color, int r, int g, int b) {
        Palette.palette_entry_set_color1(color, Palette.make_rgb(Palette.pal5bit((byte) r), Palette.pal5bit((byte) g), Palette.pal5bit((byte) b)));
    }

    public static void m72_palette1_w(int offset, short data) {
        offset &= ~0x100;
        Generic.paletteram16[offset] = data;
        offset &= 0x0ff;
        changecolor(offset, Generic.paletteram16[offset + 0x000], Generic.paletteram16[offset + 0x200], Generic.paletteram16[offset + 0x400]);
    }

    public static void m72_palette2_w(int offset, short data) {
        offset &= ~0x100;
        Generic.paletteram16_2[offset] = data;
        offset &= 0x0ff;
        changecolor(offset + 256, Generic.paletteram16_2[offset + 0x000], Generic.paletteram16_2[offset + 0x200], Generic.paletteram16_2[offset + 0x400]);
    }

    public static void m72_videoram1_w(int offset, byte data) {
        int row, col;
        m72_videoram1[offset] = data;
        row = (offset / 4) / 0x40;
        col = (offset / 4) % 0x40;
        fg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void m72_videoram2_w(int offset, byte data) {
        int row, col;
        m72_videoram2[offset] = data;
        row = (offset / 4) / 0x40;
        col = (offset / 4) % 0x40;
        bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void m72_videoram1_w(int offset, short data) {
        int row, col;
        m72_videoram1[offset * 2] = (byte) data;
        m72_videoram1[offset * 2 + 1] = (byte) (data >> 8);
        row = (offset / 2) / 0x40;
        col = (offset / 2) % 0x40;
        fg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void m72_videoram2_w(int offset, short data) {
        int row, col;
        m72_videoram2[offset * 2] = (byte) data;
        m72_videoram2[offset * 2 + 1] = (byte) (data >> 8);
        row = (offset / 2) / 0x40;
        col = (offset / 2) % 0x40;
        bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void m72_irq_line_w(short data) {
        m72_raster_irq_position = data;
    }

    public static void m72_scrollx1_w(short data) {
        scrollx1 = data;
    }

    public static void m72_scrollx2_w(short data) {
        scrollx2 = data;
    }

    public static void m72_scrolly1_w(short data) {
        scrolly1 = data;
    }

    public static void m72_scrolly2_w(short data) {
        scrolly2 = data;
    }

    public static void m72_dmaon_w(short data) {
        //if (ACCESSING_BITS_0_7)
        System.arraycopy(Generic.spriteram16, 0, m72_spriteram, 0, spriteram_size / 2);
    }

    public static void m72_port02_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            //flip_screen_set(((data & 0x04) >> 2) ^ ((~input_port_read(machine, "DSW") >> 8) & 1));
            video_off = data & 0x08;
            if ((data & 0x10) != 0) {
                Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), LineState.CLEAR_LINE);
            } else {
                Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), LineState.ASSERT_LINE);
            }
        }
    }

    public static void rtype2_port02_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            //flip_screen_set(((data & 0x04) >> 2) ^ ((~input_port_read(machine, "DSW") >> 8) & 1));
            video_off = data & 0x08;
        }
    }

    public static void majtitle_gfx_ctrl_w(short data) {
        //if (ACCESSING_BITS_8_15)
        {
            if ((data & 0xff00) != 0) {
                majtitle_rowscroll = 1;
            } else {
                majtitle_rowscroll = 0;
            }
        }
    }

    public static void m72_draw_sprites(RECT rect) {
        int offs;
        offs = 0;
        while (offs < spriteram_size / 2) {
            int code, color, sx, sy, flipx, flipy, w, h, x, y;
            code = m72_spriteram[offs + 1];
            color = m72_spriteram[offs + 2] & 0x0f;
            sx = -256 + (m72_spriteram[offs + 3] & 0x3ff);
            sy = 384 - (m72_spriteram[offs + 0] & 0x1ff);
            flipx = m72_spriteram[offs + 2] & 0x0800;
            flipy = m72_spriteram[offs + 2] & 0x0400;
            w = 1 << ((m72_spriteram[offs + 2] & 0xc000) >> 14);
            h = 1 << ((m72_spriteram[offs + 2] & 0x3000) >> 12);
            sy -= 16 * h;
//            if (flip_screen_get()) {
//                sx = 512 - 16 * w - sx;
//                sy = 284 - 16 * h - sy;
//                flipx = !flipx;
//                flipy = !flipy;
//            }
            for (x = 0; x < w; x++) {
                for (y = 0; y < h; y++) {
                    int c = code;
                    if (flipx != 0) {
                        c += 8 * (w - 1 - x);
                    } else {
                        c += 8 * x;
                    }
                    if (flipy != 0) {
                        c += h - 1 - y;
                    } else {
                        c += y;
                    }
                    Drawgfx.common_drawgfx_m72(M72.sprites1rom, c, color, flipx, flipy, sx + 16 * x, sy + 16 * y, rect);
                }
            }
            offs += w * 4;
        }
    }

    public static void video_start_m72() {
        int i;
        uuB200 = new short[0x200 * 0x200];
        Video.new_clip = new RECT();
        spriteram_size = 0x400;
        for (i = 0; i < 0x4_0000; i++) {
            uuB200[i] = 0x200;
        }
        m72_spriteram = new short[0x200];
        m72_videoram1 = new byte[0x4000];
        m72_videoram2 = new byte[0x4000];
        fg_tilemap.tilemap_set_scrolldx(0, 0);
        fg_tilemap.tilemap_set_scrolldy(-128, 16);
        bg_tilemap.tilemap_set_scrolldx(0, 0);
        bg_tilemap.tilemap_set_scrolldy(-128, 16);
        switch (Machine.sName) {
            case "ltswords":
            case "kengo":
            case "kengoa":
                fg_tilemap.tilemap_set_scrolldx(6, 0);
                bg_tilemap.tilemap_set_scrolldx(6, 0);
                break;
        }
    }

    public static void video_update_m72() {
        if (video_off != 0) {
            System.arraycopy(uuB200, 0, Video.bitmapbase[Video.curbitmap], 0, 0x4_0000);
            return;
        }
        fg_tilemap.tilemap_set_scrollx(0, scrollx1);
        fg_tilemap.tilemap_set_scrolly(0, scrolly1);
        bg_tilemap.tilemap_set_scrollx(0, scrollx2);
        bg_tilemap.tilemap_set_scrolly(0, scrolly2);
        bg_tilemap.tilemap_draw_primask(Video.new_clip, 0x20, (byte) 0);
        fg_tilemap.tilemap_draw_primask(Video.new_clip, 0x20, (byte) 0);
        m72_draw_sprites(Video.new_clip);
        bg_tilemap.tilemap_draw_primask(Video.new_clip, 0x10, (byte) 0);
        fg_tilemap.tilemap_draw_primask(Video.new_clip, 0x10, (byte) 0);
    }

    public static void video_eof_m72() {

    }

    public static void video_start_m82() {
        int i;
        uuB200 = new short[0x400 * 0x200];
        Video.new_clip = new RECT();
        spriteram_size = 0x400;
        for (i = 0; i < 0x8_0000; i++) {
            uuB200[i] = 0x200;
        }
    }

//#endregion

//#region Tilemap

    public static Tmap bg_tilemap, fg_tilemap, bg_tilemap_large;

    public static void tilemap_init() {
        int i;
        bg_tilemap = new Tmap();
        bg_tilemap.rows = 64;
        bg_tilemap.cols = 64;
        bg_tilemap.tilewidth = 8;
        bg_tilemap.tileheight = 8;
        bg_tilemap.width = 0x200;
        bg_tilemap.height = 0x200;
        bg_tilemap.enable = true;
        bg_tilemap.all_tiles_dirty = true;
        bg_tilemap.pixmap = new short[0x200 * 0x200];
        bg_tilemap.flagsmap = new byte[0x200][0x200];
        bg_tilemap.tileflags = new byte[0x40][0x40];
        bg_tilemap.total_elements = M72.gfx21rom.length / 0x40;
        bg_tilemap.pen_data = new byte[0x40];
        bg_tilemap.pen_to_flags = new byte[3][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x20;
        }
        for (i = 0; i < 8; i++) {
            bg_tilemap.pen_to_flags[1][i] = 0x20;
        }
        for (i = 8; i < 16; i++) {
            bg_tilemap.pen_to_flags[1][i] = 0x10;
        }
        bg_tilemap.pen_to_flags[2][0] = 0x20;
        for (i = 1; i < 16; i++) {
            bg_tilemap.pen_to_flags[2][i] = 0x10;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceM72;
        fg_tilemap = new Tmap();
        fg_tilemap.cols = 64;
        fg_tilemap.rows = 64;
        fg_tilemap.tilewidth = 8;
        fg_tilemap.tileheight = 8;
        fg_tilemap.width = 0x200;
        fg_tilemap.height = 0x200;
        fg_tilemap.enable = true;
        fg_tilemap.all_tiles_dirty = true;
        fg_tilemap.pixmap = new short[0x200 * 0x200];
        fg_tilemap.flagsmap = new byte[0x200][0x200];
        fg_tilemap.tileflags = new byte[0x40][0x40];
        fg_tilemap.total_elements = M72.gfx21rom.length / 0x40;
        fg_tilemap.pen_data = new byte[0x400];
        fg_tilemap.pen_to_flags = new byte[3][32];
        fg_tilemap.pen_to_flags[0][0] = 0;
        for (i = 1; i < 16; i++) {
            fg_tilemap.pen_to_flags[0][i] = 0x20;
        }
        fg_tilemap.pen_to_flags[1][0] = 0;
        for (i = 1; i < 8; i++) {
            fg_tilemap.pen_to_flags[1][i] = 0x20;
        }
        for (i = 8; i < 16; i++) {
            fg_tilemap.pen_to_flags[1][i] = 0x10;
        }
        fg_tilemap.pen_to_flags[2][0] = 0;
        for (i = 1; i < 16; i++) {
            fg_tilemap.pen_to_flags[2][i] = 0x10;
        }
        fg_tilemap.scrollrows = 1;
        fg_tilemap.scrollcols = 1;
        fg_tilemap.rowscroll = new int[fg_tilemap.scrollrows];
        fg_tilemap.colscroll = new int[fg_tilemap.scrollcols];
        fg_tilemap.tilemap_draw_instance3 = fg_tilemap::tilemap_draw_instanceM72;
        switch (Machine.sName) {
            case "airduel":
            case "airduelm72":
                bg_tilemap.tile_update3 = bg_tilemap::tile_updateM72_bg_m72;
                fg_tilemap.tile_update3 = fg_tilemap::tile_updateM72_fg_m72;
                break;
            case "ltswords":
            case "kengo":
            case "kengoa":
                bg_tilemap.tile_update3 = bg_tilemap::tile_updateM72_bg_rtype2;
                fg_tilemap.tile_update3 = fg_tilemap::tile_updateM72_fg_rtype2;
                break;
        }
    }

    public static void tilemap_init_m82() {
        int i;
        bg_tilemap = new Tmap();
        bg_tilemap.rows = 64;
        bg_tilemap.cols = 64;
        bg_tilemap.tilewidth = 8;
        bg_tilemap.tileheight = 8;
        bg_tilemap.width = 0x200;
        bg_tilemap.height = 0x200;
        bg_tilemap.enable = true;
        bg_tilemap.all_tiles_dirty = true;
        bg_tilemap.pixmap = new short[0x200 * 0x200];
        bg_tilemap.flagsmap = new byte[0x200][0x200];
        bg_tilemap.tileflags = new byte[0x40][0x40];
        bg_tilemap.total_elements = M72.gfx21rom.length / 0x40;
        bg_tilemap.pen_data = new byte[0x40];
        bg_tilemap.pen_to_flags = new byte[3][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x20;
        }
        for (i = 0; i < 8; i++) {
            bg_tilemap.pen_to_flags[1][i] = 0x20;
        }
        for (i = 8; i < 16; i++) {
            bg_tilemap.pen_to_flags[1][i] = 0x10;
        }
        bg_tilemap.pen_to_flags[2][0] = 0x20;
        for (i = 1; i < 16; i++) {
            bg_tilemap.pen_to_flags[2][i] = 0x10;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceM72;
        fg_tilemap = new Tmap();
        fg_tilemap.cols = 64;
        fg_tilemap.rows = 64;
        fg_tilemap.tilewidth = 8;
        fg_tilemap.tileheight = 8;
        fg_tilemap.width = 0x200;
        fg_tilemap.height = 0x200;
        fg_tilemap.enable = true;
        fg_tilemap.all_tiles_dirty = true;
        fg_tilemap.pixmap = new short[0x200 * 0x200];
        fg_tilemap.flagsmap = new byte[0x200][0x200];
        fg_tilemap.tileflags = new byte[0x40][0x40];
        fg_tilemap.total_elements = M72.gfx21rom.length / 0x40;
        fg_tilemap.pen_data = new byte[0x400];
        fg_tilemap.pen_to_flags = new byte[3][32];
        fg_tilemap.pen_to_flags[0][0] = 0;
        for (i = 1; i < 16; i++) {
            fg_tilemap.pen_to_flags[0][i] = 0x20;
        }
        fg_tilemap.pen_to_flags[1][0] = 0;
        for (i = 1; i < 8; i++) {
            fg_tilemap.pen_to_flags[1][i] = 0x20;
        }
        for (i = 8; i < 16; i++) {
            fg_tilemap.pen_to_flags[1][i] = 0x10;
        }
        fg_tilemap.pen_to_flags[2][0] = 0;
        for (i = 1; i < 16; i++) {
            fg_tilemap.pen_to_flags[2][i] = 0x10;
        }
        fg_tilemap.scrollrows = 1;
        fg_tilemap.scrollcols = 1;
        fg_tilemap.rowscroll = new int[fg_tilemap.scrollrows];
        fg_tilemap.colscroll = new int[fg_tilemap.scrollcols];
        fg_tilemap.tilemap_draw_instance3 = fg_tilemap::tilemap_draw_instanceM72;
        bg_tilemap_large = new Tmap();
        bg_tilemap_large.rows = 0x40;
        bg_tilemap_large.cols = 0x80;
        bg_tilemap_large.tilewidth = 8;
        bg_tilemap_large.tileheight = 8;
        bg_tilemap_large.width = 0x400;
        bg_tilemap_large.height = 0x200;
        bg_tilemap_large.enable = true;
        bg_tilemap_large.all_tiles_dirty = true;
        bg_tilemap_large.pixmap = new short[0x400 * 0x200];
        bg_tilemap_large.flagsmap = new byte[0x200][0x400];
        bg_tilemap_large.tileflags = new byte[0x40][0x80];
        bg_tilemap_large.total_elements = M72.gfx21rom.length / 0x40;
        bg_tilemap_large.pen_data = new byte[0x40];
        bg_tilemap_large.pen_to_flags = new byte[3][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap_large.pen_to_flags[0][i] = 0x20;
        }
        for (i = 0; i < 8; i++) {
            bg_tilemap_large.pen_to_flags[1][i] = 0x20;
        }
        for (i = 8; i < 16; i++) {
            bg_tilemap_large.pen_to_flags[1][i] = 0x10;
        }
        bg_tilemap_large.pen_to_flags[2][0] = 0x20;
        for (i = 1; i < 16; i++) {
            bg_tilemap_large.pen_to_flags[2][i] = 0x10;
        }
        bg_tilemap_large.scrollrows = 1;
        bg_tilemap_large.scrollcols = 1;
        bg_tilemap_large.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap_large.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap_large.tilemap_draw_instance3 = bg_tilemap_large::tilemap_draw_instanceM72;
        bg_tilemap.tile_update3 = bg_tilemap::tile_updateM72_bg_m72;
        fg_tilemap.tile_update3 = fg_tilemap::tile_updateM72_fg_m72;
        bg_tilemap_large.tile_update3 = bg_tilemap::tile_updateM72_bg_m72;
    }

//#endregion
}
