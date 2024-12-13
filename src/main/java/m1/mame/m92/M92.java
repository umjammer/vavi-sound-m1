/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.m92;


import java.awt.event.KeyEvent;
import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.nec.Nec;
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
import m1.emu.Palette;
import m1.emu.RomInfo;
import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.Iremga20;
import m1.sound.Sound;
import m1.sound.YM2151;


public class M92 {

    public static byte irqvector;
    public static short sound_status;
    public static int bankaddress;
    public static EmuTimer scanline_timer;
    public static byte m92_irq_vectorbase;
    public static int m92_raster_irq_position;
    public static int m92_scanline_param;
    public static int setvector_param;
    public static byte m92_sprite_buffer_busy;
    public static byte[] gfx1rom, gfx11rom, gfx2rom, gfx21rom, eeprom;
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
    public static final byte[] bomberman_decryption_table = new byte[] {
            (byte) 0x90, (byte) 0x90, 0x79, (byte) 0x90, (byte) 0x9d, 0x48, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2e, (byte) 0x90, (byte) 0x90, (byte) 0xa5, 0x72, (byte) 0x90, /* 00 */
            0x46, 0x5b, (byte) 0xb1, 0x3a, (byte) 0xc3, (byte) 0x90, 0x35, (byte) 0x90, (byte) 0x90, 0x23, (byte) 0x90, (byte) 0x99, (byte) 0x90, 0x05, (byte) 0x90, 0x3c, /* 10 */
            0x3b, 0x76, 0x11, (byte) 0x90, (byte) 0x90, 0x4b, (byte) 0x90, (byte) 0x92, (byte) 0x90, 0x32, 0x5d, (byte) 0x90, (byte) 0xf7, 0x5a, (byte) 0x9c, (byte) 0x90, /* 20 */
            0x26, 0x40, (byte) 0x89, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x57, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xba, 0x53, (byte) 0xbb, /* 30 */
            0x42, 0x59, 0x2f, (byte) 0x90, 0x77, (byte) 0x90, (byte) 0x90, 0x4f, (byte) 0xbf, 0x4a, (byte) 0xcb, (byte) 0x86, 0x62, 0x7d, (byte) 0x90, (byte) 0xb8, /* 40 */
            (byte) 0x90, 0x34, (byte) 0x90, 0x5f, (byte) 0x90, 0x7f, (byte) 0xf8, (byte) 0x80, (byte) 0xa0, (byte) 0x84, 0x12, 0x52, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x47, /* 50 */
            (byte) 0x90, 0x2b, (byte) 0x88, (byte) 0xf9, (byte) 0x90, (byte) 0xa3, (byte) 0x83, (byte) 0x90, 0x75, (byte) 0x87, (byte) 0x90, (byte) 0xab, (byte) 0xeb, (byte) 0x90, (byte) 0xfe, (byte) 0x90, /* 60 */
            (byte) 0x90, (byte) 0xaf, (byte) 0xd0, 0x2c, (byte) 0xd1, (byte) 0xe6, (byte) 0x90, 0x43, (byte) 0xa2, (byte) 0xe7, (byte) 0x85, (byte) 0xe2, 0x49, 0x22, 0x29, (byte) 0x90, /* 70 */
            0x7c, (byte) 0x90, (byte) 0x90, (byte) 0x9a, (byte) 0x90, (byte) 0x90, (byte) 0xb9, (byte) 0x90, 0x14, (byte) 0xcf, 0x33, 0x02, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x73, /* 80 */
            (byte) 0x90, (byte) 0xc5, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xf3, (byte) 0xf6, 0x24, (byte) 0x90, 0x56, (byte) 0xd3, (byte) 0x90, 0x09, 0x01, (byte) 0x90, (byte) 0x90, /* 90 */
            0x03, 0x2d, 0x1b, (byte) 0x90, (byte) 0xf5, (byte) 0xbe, (byte) 0x90, (byte) 0x90, (byte) 0xfb, (byte) 0x8e, 0x21, (byte) 0x8d, 0x0b, (byte) 0x90, (byte) 0x90, (byte) 0xb2, /* A0 */
            (byte) 0xfc, (byte) 0xfa, (byte) 0xc6, (byte) 0x90, (byte) 0xe8, (byte) 0xd2, (byte) 0x90, 0x08, 0x0a, (byte) 0xa8, 0x78, (byte) 0xff, (byte) 0x90, (byte) 0xb5, (byte) 0x90, (byte) 0x90, /* B0 */
            (byte) 0xc7, 0x06, 0x18, (byte) 0x90, (byte) 0x90, 0x1e, 0x7e, (byte) 0xb0, 0x0e, 0x0f, (byte) 0x90, (byte) 0x90, 0x0c, (byte) 0xaa, 0x55, (byte) 0x90, /* C0 */
            (byte) 0x90, 0x74, 0x3d, (byte) 0x90, (byte) 0x90, 0x38, 0x27, 0x50, (byte) 0x90, (byte) 0xb6, 0x5e, (byte) 0x8b, 0x07, (byte) 0xe5, 0x39, (byte) 0xea, /* D0 */
            (byte) 0xbd, (byte) 0x90, (byte) 0x81, (byte) 0xb7, (byte) 0x90, (byte) 0x8a, 0x0d, (byte) 0x90, 0x58, (byte) 0xa1, (byte) 0xa9, 0x36, (byte) 0x90, (byte) 0xc4, (byte) 0x90, (byte) 0x8f, /* E0 */
            (byte) 0x8c, 0x1f, 0x51, 0x04, (byte) 0xf2, (byte) 0x90, (byte) 0xb3, (byte) 0xb4, (byte) 0xe9, 0x2a, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x25, (byte) 0x90, (byte) 0xbc, /* F0 */
    };
    public static final byte[] lethalth_decryption_table = new byte[] {
            0x7f, 0x26, 0x5d, (byte) 0x90, (byte) 0xba, (byte) 0x90, 0x1e, 0x5e, (byte) 0xb8, (byte) 0x90, (byte) 0xbc, (byte) 0xe8, 0x01, (byte) 0x90, 0x4a, 0x25, /* 00 */
            (byte) 0x90, (byte) 0xbd, (byte) 0x90, 0x22, 0x10, (byte) 0x90, 0x02, 0x57, 0x70, (byte) 0x90, 0x7e, (byte) 0x90, (byte) 0xe7, 0x52, (byte) 0x90, (byte) 0xa9, /* 10 */
            (byte) 0x90, (byte) 0x90, (byte) 0xc6, 0x06, (byte) 0xa0, (byte) 0xfe, (byte) 0xcf, (byte) 0x8e, 0x43, (byte) 0x8f, 0x2d, (byte) 0x90, (byte) 0xd4, (byte) 0x85, 0x75, (byte) 0xa2, /* 20 */
            0x3d, (byte) 0x90, (byte) 0x90, 0x38, 0x7c, (byte) 0x89, (byte) 0xd1, (byte) 0x80, 0x3b, 0x72, 0x07, (byte) 0x90, 0x42, 0x37, 0x0a, 0x18, /* 30 */
            (byte) 0x88, (byte) 0xb4, (byte) 0x98, (byte) 0x8b, (byte) 0xb9, (byte) 0x9c, (byte) 0xad, 0x0e, 0x2b, (byte) 0x90, (byte) 0xbf, (byte) 0x90, 0x55, (byte) 0x90, 0x56, (byte) 0xb0, /* 40 */
            (byte) 0x93, (byte) 0x91, (byte) 0x90, (byte) 0xeb, (byte) 0x90, 0x50, 0x41, 0x29, 0x47, (byte) 0x90, (byte) 0x90, 0x60, (byte) 0x90, (byte) 0xab, (byte) 0x90, (byte) 0x90, /* 50 */
            (byte) 0xc3, (byte) 0xe2, (byte) 0xd0, (byte) 0xb2, 0x11, 0x79, (byte) 0x90, 0x08, (byte) 0x90, (byte) 0xfb, (byte) 0x90, 0x2c, 0x23, (byte) 0x90, 0x28, 0x0d, /* 60 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x83, 0x3c, (byte) 0x90, 0x1b, 0x34, 0x5b, (byte) 0x90, 0x40, (byte) 0x90, (byte) 0x90, 0x04, (byte) 0xfc, (byte) 0xcd, /* 70 */
            (byte) 0xb1, (byte) 0xf3, (byte) 0x8a, (byte) 0x90, (byte) 0x90, (byte) 0x87, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbe, (byte) 0x84, 0x1f, (byte) 0xe6, /* 80 */
            (byte) 0xff, (byte) 0x90, 0x12, (byte) 0x90, (byte) 0xb5, 0x36, (byte) 0x90, (byte) 0xb3, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xd2, 0x4e, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 90 */
            (byte) 0xa5, (byte) 0x90, (byte) 0x90, (byte) 0xc7, (byte) 0x90, 0x27, 0x0b, (byte) 0x90, 0x20, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x61, 0x7d, /* A0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x86, 0x0f, (byte) 0x90, (byte) 0xb7, (byte) 0x90, 0x4f, (byte) 0x90, (byte) 0x90, (byte) 0xc0, (byte) 0xfd, (byte) 0x90, 0x39, (byte) 0x90, 0x77, /* B0 */
            0x05, 0x3a, (byte) 0x90, 0x48, (byte) 0x92, 0x76, 0x3e, 0x03, (byte) 0x90, (byte) 0xf8, (byte) 0x90, 0x59, (byte) 0xa8, 0x5f, (byte) 0xf9, (byte) 0xbb, /* C0 */
            (byte) 0x81, (byte) 0xfa, (byte) 0x9d, (byte) 0xe9, 0x2e, (byte) 0xa1, (byte) 0xc1, 0x33, (byte) 0x90, 0x78, (byte) 0x90, 0x0c, (byte) 0x90, 0x24, (byte) 0xaa, (byte) 0xac, /* D0 */
            (byte) 0x90, (byte) 0xb6, (byte) 0x90, (byte) 0xea, (byte) 0x90, 0x73, (byte) 0xe5, 0x58, 0x00, (byte) 0xf7, (byte) 0x90, 0x74, (byte) 0x90, 0x76, (byte) 0x90, (byte) 0xa3, /* E0 */
            (byte) 0x90, 0x5a, (byte) 0xf6, 0x32, 0x46, 0x2a, (byte) 0x90, (byte) 0x90, 0x53, 0x4b, (byte) 0x90, 0x0d, 0x51, 0x68, (byte) 0x99, 0x13, /* F0 */
    };
    public static final byte[] dynablaster_decryption_table = new byte[] {
            0x1f, 0x51, (byte) 0x84, (byte) 0x90, 0x3d, 0x09, 0x0d, (byte) 0x90, (byte) 0x90, 0x57, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x32, 0x11, (byte) 0x90, /* 00 */
            (byte) 0x90, (byte) 0x9c, (byte) 0x90, (byte) 0x90, 0x4b, (byte) 0x90, (byte) 0x90, 0x03, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x89, (byte) 0xb0, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 10 */
            (byte) 0x90, (byte) 0xbb, 0x18, (byte) 0xbe, 0x53, 0x21, 0x55, 0x7c, (byte) 0x90, (byte) 0x90, 0x47, 0x58, (byte) 0xf6, (byte) 0x90, (byte) 0x90, (byte) 0xb2, /* 20 */
            0x06, (byte) 0x90, 0x2b, (byte) 0x90, 0x2f, 0x0b, (byte) 0xfc, (byte) 0x91, (byte) 0x90, (byte) 0x90, (byte) 0xfa, (byte) 0x81, (byte) 0x83, 0x40, 0x38, (byte) 0x90, /* 30 */
            (byte) 0x90, (byte) 0x90, 0x49, (byte) 0x85, (byte) 0xd1, (byte) 0xf5, 0x07, (byte) 0xe2, 0x5e, 0x1e, (byte) 0x90, 0x04, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xb1, /* 40 */
            (byte) 0xc7, (byte) 0x90, (byte) 0x96, (byte) 0xf2 /*(byte) 0xaf*/, (byte) 0xb6, (byte) 0xd2, (byte) 0xc3, (byte) 0x90, (byte) 0x87, (byte) 0xba, (byte) 0xcb, (byte) 0x88, (byte) 0x90, (byte) 0xb9, (byte) 0xd0, (byte) 0xb5, /* 50 */
            (byte) 0x9a, (byte) 0x80, (byte) 0xa2, 0x72, (byte) 0x90, (byte) 0xb4, (byte) 0x90, (byte) 0xaa, 0x26, 0x7d, 0x52, 0x33, 0x2e, (byte) 0xbc, 0x08, 0x79, /* 60 */
            0x48, (byte) 0x90, 0x76, 0x36, 0x02, (byte) 0x90, 0x5b, 0x12, (byte) 0x8b, (byte) 0xe7, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xab, (byte) 0x90, 0x4f, /* 70 */
            (byte) 0x90, (byte) 0x90, (byte) 0xa8, (byte) 0xe5, 0x39, 0x0e, (byte) 0xa9, (byte) 0x90, (byte) 0x90, 0x14, (byte) 0x90, (byte) 0xff, 0x7f/*0x75*/, (byte) 0x90, (byte) 0x90, 0x27, /* 80 */
            (byte) 0x90, 0x01, (byte) 0x90, (byte) 0x90, (byte) 0xe6, (byte) 0x8a, (byte) 0xd3, (byte) 0x90, (byte) 0x90, (byte) 0x8e, 0x56, (byte) 0xa5, (byte) 0x92, (byte) 0x90, (byte) 0x90, (byte) 0xf9, /* 90 */
            0x22, (byte) 0x90, 0x5f, (byte) 0x90, (byte) 0x90, (byte) 0xa1, (byte) 0x90, 0x74, (byte) 0xb8, (byte) 0x90, 0x46, 0x05, (byte) 0xeb, (byte) 0xcf, (byte) 0xbf, 0x5d, /* a0 */
            0x24, (byte) 0x90, (byte) 0x9d, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x59, (byte) 0x8d, 0x3c, (byte) 0xf8, (byte) 0xc5, (byte) 0x90, (byte) 0xf3, 0x4e, /* b0 */
            (byte) 0x90, (byte) 0x90, 0x50, (byte) 0xc6, (byte) 0xe9, (byte) 0xfe, 0x0a, (byte) 0x90, (byte) 0x99, (byte) 0x86, (byte) 0x90, (byte) 0x90, (byte) 0xaf, (byte) 0x8c/*(byte) 0x8e*/, 0x42, (byte) 0xf7, /* c0 */
            (byte) 0x90, 0x41, (byte) 0x90, (byte) 0xa3, (byte) 0x90, 0x3a, 0x2a, 0x43, (byte) 0x90, (byte) 0xb3, (byte) 0xe8, (byte) 0x90, (byte) 0xc4, 0x35, 0x78, 0x25, /* d0 */
            0x75, (byte) 0x90, (byte) 0xb7, (byte) 0x90, 0x23, (byte) 0x90, (byte) 0x90/*(byte) 0xe2*/, (byte) 0x8f, (byte) 0x90, (byte) 0x90, 0x2c, (byte) 0x90, 0x77, 0x7e, (byte) 0x90, 0x0f, /* e0 */
            0x0c, (byte) 0xa0, (byte) 0xbd, (byte) 0x90, (byte) 0x90, 0x2d, 0x29, (byte) 0xea, (byte) 0x90, 0x3b, 0x73, (byte) 0x90, (byte) 0xfb, 0x20, (byte) 0x90, 0x5a /* f0 */
    };
    public static final byte[] mysticri_decryption_table = new byte[] {
            (byte) 0x90, 0x57, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbf, 0x43, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xfc, (byte) 0x90, /* 00 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x52, (byte) 0xa3, 0x26, (byte) 0x90, (byte) 0xc7, (byte) 0x90, 0x0f, (byte) 0x90, 0x0c, (byte) 0x90, (byte) 0x90, /* 10 */
            (byte) 0x90, (byte) 0x90, (byte) 0xff, (byte) 0x90, (byte) 0x90, 0x02, (byte) 0x90, (byte) 0x90, 0x2e, (byte) 0x90, 0x5f, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x73, 0x50, /* 20 */
            (byte) 0xb2, 0x3a, (byte) 0x90, (byte) 0x90, (byte) 0xbb, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 30 */
            (byte) 0x90, (byte) 0x90, (byte) 0x8e, 0x3c, 0x42, (byte) 0x90, (byte) 0x90, (byte) 0xb9, (byte) 0x90, (byte) 0x90, 0x2a, (byte) 0x90, 0x47, (byte) 0xa0, 0x2b, 0x03, /* 40 */
            (byte) 0xb5, 0x1f, (byte) 0x90, (byte) 0xaa, (byte) 0x90, (byte) 0xfb, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x38, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 50 */
            0x2c, (byte) 0x90, (byte) 0x90, (byte) 0xc6, (byte) 0x90, (byte) 0x90, (byte) 0xb1, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xa2, (byte) 0x90, /* 60 */
            (byte) 0xe9, (byte) 0xe8, (byte) 0x90, (byte) 0x90, (byte) 0x86, (byte) 0x90, (byte) 0x8b, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5b, 0x72, (byte) 0x90, (byte) 0x90, /* 70 */
            (byte) 0x90, (byte) 0x90, 0x5d, 0x0a, (byte) 0x90, (byte) 0x90, (byte) 0x89, (byte) 0x90, (byte) 0xb0, (byte) 0x88, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x87, 0x75, (byte) 0xbd, /* 80 */
            (byte) 0x90, 0x51, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5a, 0x58, (byte) 0x90, (byte) 0x90, 0x56, /* 90 */
            (byte) 0x90, (byte) 0x8a, (byte) 0x90, 0x55, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xb4, 0x08, (byte) 0x90, (byte) 0xf6, (byte) 0x90, (byte) 0x90, (byte) 0x9d, (byte) 0x90, (byte) 0xbc, /* A0 */
            0x0b, (byte) 0x90, (byte) 0x90, 0x5e, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x22, 0x36, (byte) 0x90, 0x1e, (byte) 0x90, (byte) 0xb6, (byte) 0xba, 0x23, (byte) 0x90, /* B0 */
            0x20, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x59, 0x53, (byte) 0x90, 0x04, (byte) 0x81, (byte) 0x90, (byte) 0x90, (byte) 0xf3, (byte) 0x90, (byte) 0x90, 0x3b, 0x06, /* C0 */
            (byte) 0x90, 0x79, (byte) 0x83, (byte) 0x9c, (byte) 0x90, 0x18, (byte) 0x80, (byte) 0x90, (byte) 0xc3, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x32, (byte) 0x90, (byte) 0xcf, (byte) 0x90, /* D0 */
            (byte) 0xeb, (byte) 0x90, (byte) 0x90, 0x33, (byte) 0x90, (byte) 0xfa, (byte) 0x90, (byte) 0x90, (byte) 0xd2, (byte) 0x90, 0x24, (byte) 0x90, 0x74, 0x41, (byte) 0xb8, (byte) 0x90, /* E0 */
            (byte) 0x90, (byte) 0x90, (byte) 0xd0, 0x07, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x46, (byte) 0x90, (byte) 0xea, (byte) 0xfe, 0x78, (byte) 0x90, (byte) 0x90, /* F0 */
    };
    public static final byte[] majtitl2_decryption_table = new byte[] {
            (byte) 0x87, (byte) 0x90, 0x78, (byte) 0xaa, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2c, 0x32, 0x0a, 0x0f, (byte) 0x90, 0x5e, (byte) 0x90, (byte) 0xc6, (byte) 0x8a, /* 00 */
            0x33, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xea, (byte) 0x90, 0x72, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x24, 0x55, /* 10 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x89, (byte) 0xfb, (byte) 0x90, 0x59, 0x02, (byte) 0x90, (byte) 0x90, 0x5d, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x36, (byte) 0x90, /* 20 */
            (byte) 0x90, 0x06, 0x79, (byte) 0x90, (byte) 0x90, 0x1e, 0x07, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x83, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 30 */
            (byte) 0x9d, (byte) 0x90, (byte) 0x90, 0x74, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x0c, 0x58, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 40 */
            0x3c, (byte) 0x90, 0x03, (byte) 0x90, (byte) 0x90, (byte) 0xfa, 0x43, (byte) 0x90, (byte) 0xbf, (byte) 0x90, (byte) 0x90, 0x75, (byte) 0x90, (byte) 0x88, (byte) 0x90, (byte) 0x80, /* 50 */
            (byte) 0x90, (byte) 0xa3, (byte) 0x90, (byte) 0xfe, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x3a, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 60 */
            0x2b, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xe9, 0x5f, (byte) 0x90, 0x46, (byte) 0x90, 0x41, (byte) 0x90, 0x18, (byte) 0xb8, (byte) 0x90, (byte) 0x90, /* 70 */
            (byte) 0xb4, 0x5a, (byte) 0xb1, (byte) 0x90, (byte) 0x90, 0x50, (byte) 0xe8, 0x20, (byte) 0x90, (byte) 0xb2, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x51, /* 80 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x56, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xcf, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xc3, (byte) 0x90, (byte) 0x90, /* 90 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x0b, (byte) 0x90, (byte) 0x90, (byte) 0xb5, 0x57, (byte) 0x90, (byte) 0x90, (byte) 0xc7, 0x3b, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* A0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xb6, (byte) 0x90, (byte) 0xeb, (byte) 0x90, 0x38, (byte) 0x90, (byte) 0xa0, 0x08, (byte) 0x90, (byte) 0x86, (byte) 0xb0, (byte) 0x90, /* B0 */
            0x42, 0x1f, 0x73, (byte) 0x90, (byte) 0xf6, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x53, (byte) 0x90, 0x52, (byte) 0x90, 0x04, (byte) 0xbd, (byte) 0x90, (byte) 0x90, /* C0 */
            0x26, (byte) 0xff, 0x2e, (byte) 0x90, (byte) 0x81, (byte) 0x90, 0x47, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xd0, 0x22, (byte) 0x90, (byte) 0x90, (byte) 0xb9, /* D0 */
            0x23, (byte) 0x90, (byte) 0xf3, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xd2, (byte) 0x8b, (byte) 0xba, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5b, /* E0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x9c, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xfc, (byte) 0xbc, (byte) 0xa2, 0x2a, (byte) 0x90, (byte) 0x90, (byte) 0x8e, (byte) 0xbb, (byte) 0x90, /* F0 */
    };
    public static final byte[] hook_decryption_table = new byte[] {
            (byte) 0xb6, 0x20, 0x22, (byte) 0x90, 0x0f, 0x57, 0x59, (byte) 0xc6, (byte) 0xeb, (byte) 0x90, (byte) 0xb0, (byte) 0xbb, 0x3b, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 00 */
            0x36, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xfe, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xa0, /* 10 */
            0x2e, (byte) 0x90, 0x0b, (byte) 0x90, (byte) 0x90, 0x58, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x80, (byte) 0x90, (byte) 0x90, /* 20 */
            0x33, (byte) 0x90, (byte) 0x90, (byte) 0xbf, 0x55, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x53, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 30 */
            0x47, 0x74, (byte) 0x90, (byte) 0xb1, (byte) 0xb4, (byte) 0x90, (byte) 0x90, (byte) 0x88, (byte) 0x90, (byte) 0x90, 0x38, (byte) 0xcf, (byte) 0x90, (byte) 0x8e, (byte) 0x90, (byte) 0x90, /* 40 */
            (byte) 0x90, (byte) 0xc7, (byte) 0x90, 0x32, (byte) 0x90, 0x52, 0x3c, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x83, 0x72, /* 50 */
            (byte) 0x90, 0x73, (byte) 0x90, 0x5a, (byte) 0x90, 0x43, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x41, (byte) 0xe9, (byte) 0xbd, (byte) 0x90, (byte) 0xb2, (byte) 0xd2, /* 60 */
            (byte) 0x90, (byte) 0xaa, (byte) 0xa2, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x26, (byte) 0x90, (byte) 0x90, (byte) 0x8a, (byte) 0x90, /* 70 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x18, (byte) 0x90, (byte) 0x9d, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5d, (byte) 0x90, 0x46, /* 80 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xf6, (byte) 0xc3, (byte) 0xa3, 0x1e, 0x07, 0x5f, (byte) 0x81, (byte) 0x90, 0x0c, (byte) 0x90, (byte) 0xb8, (byte) 0x90, 0x75, /* 90 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x79, /* A0 */
            (byte) 0x90, 0x5e, (byte) 0x90, (byte) 0x90, 0x06, (byte) 0x90, (byte) 0xff, (byte) 0x90, 0x5b, 0x24, (byte) 0x90, 0x2b, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x02, /* B0 */
            (byte) 0x86, (byte) 0x90, (byte) 0x90, (byte) 0xfb, (byte) 0x90, (byte) 0x90, 0x50, (byte) 0xfc, 0x08, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x03, (byte) 0x90, (byte) 0xb9, (byte) 0x90, /* C0 */
            (byte) 0x90, (byte) 0xbc, (byte) 0xe8, 0x1f, (byte) 0xfa, 0x42, (byte) 0x90, (byte) 0x90, (byte) 0x89, (byte) 0x90, 0x23, (byte) 0x87, (byte) 0x90, 0x2a, (byte) 0x90, (byte) 0x90, /* D0 */
            (byte) 0x8b, (byte) 0x90, (byte) 0xf3, (byte) 0xea, 0x04, 0x2c, (byte) 0xb5, (byte) 0x90, 0x0a, (byte) 0x90, 0x51, (byte) 0x90, (byte) 0x90, 0x3a, (byte) 0x90, (byte) 0x9c, /* E0 */
            (byte) 0x90, (byte) 0x90, 0x78, (byte) 0x90, (byte) 0xba, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xd0, 0x56, (byte) 0x90, (byte) 0x90, /* F0 */
    };
    public static final byte[] rtypeleo_decryption_table = new byte[] {
            0x5d, (byte) 0x90, (byte) 0xc6, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2a, 0x3a, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x86, (byte) 0x90, 0x22, (byte) 0x90, (byte) 0xf3, /* 00 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x38, 0x01, 0x42, 0x04, (byte) 0x90, (byte) 0x90, 0x1f, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x58, /* 10 */
            0x57, 0x2e, (byte) 0x90, (byte) 0x90, 0x53, (byte) 0x90, (byte) 0xb9, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x20, 0x55, (byte) 0x90, 0x3d, /* 20 */
            (byte) 0xa0, (byte) 0x90, (byte) 0x90, 0x0c, 0x03, (byte) 0x90, (byte) 0x83, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x8a, (byte) 0x90, (byte) 0x90, (byte) 0xaa, (byte) 0x90, (byte) 0x90, /* 30 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x41, 0x0a, 0x26, (byte) 0x8b, 0x56, 0x5e, (byte) 0x90, /* 40 */
            (byte) 0x90, 0x74, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x06, (byte) 0x90, (byte) 0x90, (byte) 0x89, 0x5b, (byte) 0xc7, 0x43, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 50 */
            (byte) 0x90, (byte) 0xb6, (byte) 0x90, 0x3b, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x36, (byte) 0xea, (byte) 0x80, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5f, /* 60 */
            (byte) 0x90, 0x0f, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x46, (byte) 0x90, (byte) 0x90, 0x3c, (byte) 0x8e, (byte) 0x90, (byte) 0xa3, (byte) 0x87, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 70 */
            0x2b, (byte) 0xfb, 0x47, 0x0b, (byte) 0x90, (byte) 0xfc, 0x02, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x72, 0x2c, /* 80 */
            0x33, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x9d, (byte) 0xbd, (byte) 0x90, (byte) 0xb2, (byte) 0x90, 0x78, 0x75, (byte) 0xb8, (byte) 0x90, (byte) 0x90, /* 90 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xcf, 0x5a, (byte) 0x88, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xc3, (byte) 0x90, (byte) 0xeb, (byte) 0xfa, (byte) 0x90, 0x32, /* A0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x52, (byte) 0xb4, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbc, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xb1, 0x59, 0x50, /* B0 */
            (byte) 0x90, (byte) 0x90, (byte) 0xb5, (byte) 0x90, 0x08, (byte) 0xa2, (byte) 0xbf, (byte) 0xbb, 0x1e, (byte) 0x9c, (byte) 0x90, 0x73, (byte) 0x90, (byte) 0xd0, (byte) 0x90, (byte) 0x90, /* C0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x81, (byte) 0x90, 0x79, (byte) 0x90, (byte) 0x90, 0x24, 0x23, (byte) 0x90, (byte) 0x90, (byte) 0xb0, 0x07, (byte) 0xff, /* D0 */
            (byte) 0x90, (byte) 0xba, (byte) 0xf6, 0x51, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xfe, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xe9, (byte) 0x90, /* E0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xe8, (byte) 0xd2, (byte) 0x90, 0x18, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xd1, (byte) 0x90, (byte) 0x90, /* F0 */
    };
    public static final byte[] inthunt_decryption_table = new byte[] {
            0x1f, (byte) 0x90, (byte) 0xbb, 0x50, (byte) 0x90, 0x58, 0x42, 0x57, (byte) 0x90, (byte) 0x90, (byte) 0xe9, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x0b, /* 00 */
            (byte) 0x90, (byte) 0x90, (byte) 0x9d, (byte) 0x9c, (byte) 0x90, (byte) 0x90, 0x1e, (byte) 0x90, (byte) 0x90, (byte) 0xb4, 0x5b, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 10 */
            (byte) 0x90, (byte) 0x90, 0x78, (byte) 0xc7, (byte) 0x90, (byte) 0x90, (byte) 0x83, (byte) 0x90, (byte) 0x90, 0x0c, (byte) 0xb0, 0x04, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 20 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x3b, (byte) 0xc3, (byte) 0xb5, 0x47, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x59, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 30 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x38, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5f, (byte) 0xa3, (byte) 0xfa, (byte) 0x90, (byte) 0xe8, 0x36, 0x75, (byte) 0x90, /* 40 */
            (byte) 0x88, 0x33, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x43, (byte) 0x90, (byte) 0x90, (byte) 0x87, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 50 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x8e, (byte) 0xf3, 0x56, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x26, (byte) 0xff, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 60 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2a, (byte) 0x90, (byte) 0x8a, (byte) 0x90, 0x18, (byte) 0x90, (byte) 0x90, 0x03, (byte) 0x89, 0x24, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 70 */
            0x0a, (byte) 0x90, (byte) 0xeb, (byte) 0x90, (byte) 0x86, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x79, 0x3a, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xa0, (byte) 0x90, /* 80 */
            (byte) 0xea, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2c, (byte) 0x90, (byte) 0xc6, (byte) 0x90, (byte) 0x90, 0x46, (byte) 0x90, (byte) 0xaa, (byte) 0xb6, 0x5e, /* 90 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x8b, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xba, (byte) 0x90, (byte) 0xb9, 0x53, (byte) 0xa2, (byte) 0x90, /* A0 */
            (byte) 0x90, 0x07, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x3c, 0x32, (byte) 0x90, 0x2b, (byte) 0x90, (byte) 0xb8, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* B0 */
            (byte) 0xbd, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x81, (byte) 0x90, (byte) 0xd0, 0x08, (byte) 0x90, 0x55, 0x06, (byte) 0xcf, (byte) 0x90, (byte) 0x90, (byte) 0xfc, /* C0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xb1, (byte) 0xbf, (byte) 0x90, (byte) 0x90, 0x51, 0x52, (byte) 0x90, 0x5d, (byte) 0x90, 0x5a, (byte) 0x90, (byte) 0xb2, (byte) 0x90, /* D0 */
            (byte) 0xfe, (byte) 0x90, (byte) 0x90, 0x22, 0x20, 0x72, (byte) 0xf6, (byte) 0x80, 0x02, 0x2e, (byte) 0x90, 0x74, 0x0f, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* E0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbc, 0x41, (byte) 0x90, (byte) 0xfb, 0x73, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x23, (byte) 0xd2, (byte) 0x90, (byte) 0x90, /* F0 */
    };
    public static final byte[] leagueman_decryption_table = new byte[] {
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x55, (byte) 0xbb, (byte) 0x90, 0x23, 0x79, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x38, (byte) 0x90, /* 00 */
            0x01, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x3d, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xba, (byte) 0x90, 0x1e, (byte) 0x90, /* 10 */
            0x2c, 0x46, (byte) 0x90, (byte) 0xb5, (byte) 0x90, 0x4b, (byte) 0x90, (byte) 0xfe, (byte) 0x90, (byte) 0x90, (byte) 0xfb, 0x2e, (byte) 0x90, (byte) 0x90, 0x36, 0x04, /* 20 */
            (byte) 0xcf, (byte) 0x90, (byte) 0xf3, 0x5a, (byte) 0x8a, 0x0c, (byte) 0x9c, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xb2, 0x50, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x5f, /* 30 */
            (byte) 0x90, (byte) 0x90, 0x24, (byte) 0x90, (byte) 0x90, 0x41, 0x2b, (byte) 0x90, (byte) 0xe9, (byte) 0x90, 0x08, 0x3b, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 40 */
            (byte) 0x90, (byte) 0xd2, 0x51, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x22, (byte) 0x90, (byte) 0xeb, 0x3a, 0x5b, (byte) 0xa2, (byte) 0xb1, (byte) 0x80, (byte) 0x90, (byte) 0x90, /* 50 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x59, (byte) 0xb4, (byte) 0x88, (byte) 0x90, (byte) 0x90, (byte) 0xbf, (byte) 0xd1, (byte) 0x90, (byte) 0xb9, 0x57, (byte) 0x90, (byte) 0x90, /* 60 */
            0x72, (byte) 0x90, 0x73, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x0f, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x56, (byte) 0x90, (byte) 0x90, (byte) 0xc6, /* 70 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2a, (byte) 0x8e, (byte) 0x90, (byte) 0x81, (byte) 0xa3, 0x58, (byte) 0x90, (byte) 0xaa, 0x78, (byte) 0x89, (byte) 0x90, /* 80 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbd, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xff, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x07, 0x53, /* 90 */
            (byte) 0xa0, (byte) 0x90, (byte) 0x90, 0x5e, (byte) 0xb0, (byte) 0x90, (byte) 0x83, (byte) 0xf6, (byte) 0x90, 0x26, 0x32, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x74, 0x0a, /* A0 */
            0x18, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x75, 0x03, (byte) 0x90, (byte) 0x90, (byte) 0xb6, 0x02, (byte) 0x90, (byte) 0x90, 0x43, (byte) 0x90, (byte) 0xb8, (byte) 0x90, /* B0 */
            (byte) 0xe8, (byte) 0x90, (byte) 0xfc, (byte) 0x90, 0x20, (byte) 0xc3, (byte) 0x90, 0x06, (byte) 0x90, 0x1f, (byte) 0x86, 0x00, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xd0, /* C0 */
            0x47, (byte) 0x90, (byte) 0x87, (byte) 0x90, (byte) 0x90, (byte) 0x9d, 0x3c, (byte) 0xc7, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* D0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x8b, (byte) 0x90, (byte) 0x90, 0x33, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xfa, 0x42, (byte) 0x90, (byte) 0x90, /* E0 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xea, (byte) 0x90, 0x52, (byte) 0x90, 0x5d, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbc, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* F0 */
    };
    public static final byte[] psoldier_decryption_table = new byte[] {
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x8a, (byte) 0x90, (byte) 0xaa, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x20, 0x23, 0x55, (byte) 0x90, (byte) 0xb5, 0x0a, (byte) 0x90, /* 00 */
            (byte) 0x90, 0x46, (byte) 0x90, (byte) 0xb6, (byte) 0x90, 0x74, (byte) 0x8b, (byte) 0x90, (byte) 0x90, (byte) 0xba, 0x01, (byte) 0x90, (byte) 0x90, 0x5a, (byte) 0x86, (byte) 0xfb, /* 10 */
            (byte) 0xb2, (byte) 0x90, (byte) 0xb0, (byte) 0x90, 0x42, 0x06, 0x1e, 0x08, 0x22, (byte) 0x9d, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x73, /* 20 */
            (byte) 0x90, (byte) 0x90, 0x5f, (byte) 0x90, (byte) 0x90, (byte) 0xd0, (byte) 0x90, (byte) 0xff, (byte) 0x90, (byte) 0x90, (byte) 0xbd, (byte) 0x90, 0x03, (byte) 0x90, (byte) 0xb9, (byte) 0x90, /* 30 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x51, 0x5e, 0x24, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x58, 0x59, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 40 */
            0x52, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xa0, (byte) 0x90, (byte) 0x90, 0x02, (byte) 0xd2, (byte) 0x90, 0x79, 0x26, 0x3a, 0x0f, (byte) 0xcf, (byte) 0xb4, /* 50 */
            (byte) 0xf3, (byte) 0x90, (byte) 0x90, 0x50, (byte) 0x90, 0x75, (byte) 0xb1, (byte) 0x90, (byte) 0xd1, 0x47, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* 60 */
            (byte) 0xc6, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xbc, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x53, 0x41, (byte) 0x90, (byte) 0x90, /* 70 */
            (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x04, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2c, (byte) 0x90, (byte) 0xbf, (byte) 0x90, (byte) 0x90, /* 80 */
            (byte) 0x90, (byte) 0x90, (byte) 0xe8, (byte) 0x90, (byte) 0x90, 0x78, (byte) 0x90, (byte) 0xbb, (byte) 0x90, (byte) 0x90, 0x1f, 0x2b, (byte) 0x87, (byte) 0x90, 0x4b, 0x56, /* 90 */
            0x36, 0x33, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x9c, (byte) 0xc3, (byte) 0x90, (byte) 0x90, (byte) 0x81, (byte) 0x90, (byte) 0xe9, (byte) 0x90, (byte) 0xfa, (byte) 0x90, (byte) 0x90, /* A0 */
            (byte) 0x90, 0x72, (byte) 0x90, (byte) 0xa2, (byte) 0x90, (byte) 0x90, (byte) 0xc7, (byte) 0x90, (byte) 0x90, (byte) 0x92, (byte) 0x90, (byte) 0x90, (byte) 0x88, (byte) 0x90, (byte) 0x90, (byte) 0x90, /* B0 */
            0x3b, (byte) 0x90, 0x0c, (byte) 0x90, (byte) 0x80, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2e, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x57, (byte) 0x90, (byte) 0x8e, /* C0 */
            0x07, (byte) 0x90, (byte) 0xa3, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x3d, (byte) 0x90, (byte) 0xfe, (byte) 0x90, (byte) 0x90, (byte) 0xfc, (byte) 0xea, (byte) 0x90, 0x38, (byte) 0x90, /* D0 */
            0x3c, (byte) 0xf6, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x18, (byte) 0x90, (byte) 0x90, (byte) 0xb8, (byte) 0x90, (byte) 0x90, (byte) 0x90, 0x2a, 0x5d, 0x5b, (byte) 0x90, /* E0 */
            (byte) 0x90, 0x43, 0x32, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xeb, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0x83, (byte) 0x89, (byte) 0x90, (byte) 0x90, /* F0 */
    };

    public static void M92Init() {
        int i1, i2, n1, n2;
        byte[] bb1;
        Machine.bRom = true;
        Timer.setvector = M92::setvector_callback;
        pf_master_control = new short[4];
        M92.pf_layer = new M92.pf_layer_info[3];
        for (i1 = 0; i1 < 3; i1++) {
            pf_layer[i1].control = new short[4];
        }
        Generic.paletteram16 = new short[0x800];
        Generic.spriteram16 = new short[0x400];
        Generic.buffered_spriteram16 = new short[0x400];
        m92_vram_data = new short[0x8000];
        m92_spritecontrol = new short[8];
        bb1 = new byte[0x1_0000]; //Machine.GetRom("maincpu.rom");
        Memory.mainrom = new byte[0x19_0000];
        System.arraycopy(bb1, 0, Memory.mainrom, 0, bb1.length);
        Memory.audiorom = Machine.GetRom("soundcpu.rom");
        Iremga20.iremrom = Machine.GetRom("irem.rom");
        Memory.mainram = new byte[0x1_0000];
        Memory.audioram = new byte[0x4000];
        gfx1rom = new byte[0x10_0000];// Machine.GetRom("gfx1.rom");
        n1 = gfx1rom.length;
        gfx11rom = new byte[n1 * 2];
        for (i1 = 0; i1 < n1; i1++) {
            gfx11rom[i1 * 2] = (byte) (gfx1rom[i1] >> 4);
            gfx11rom[i1 * 2 + 1] = (byte) (gfx1rom[i1] & 0x0f);
        }
        gfx2rom = new byte[0x10_0000];// Machine.GetRom("gfx2.rom");
        n2 = gfx2rom.length;
        gfx21rom = new byte[n2 * 2];
        for (i2 = 0; i2 < n2; i2++) {
            gfx21rom[i2 * 2] = (byte) (gfx2rom[i2] >> 4);
            gfx21rom[i2 * 2 + 1] = (byte) (gfx2rom[i2] & 0x0f);
        }
        eeprom = new byte[0x1_0000];// Machine.GetRom("eeprom.rom");
        m92_game_kludge = 0;
        m92_irq_vectorbase = (byte) 0x80;
        m92_sprite_buffer_busy = 1;
        bankaddress = 0xa_0000;
        sound_status = 0;
        dsw = (short) 0xffbf;
        switch (Machine.sName) {
            case "lethalth":
            case "thndblst":
                bankaddress = 0;
                m92_irq_vectorbase = 0x20;
                break;
            case "majtitl2":
            case "majtitl2a":
            case "majtitl2b":
            case "majtitl2j":
            case "skingame":
            case "skingame2":
                m92_game_kludge = 2;
                dsw = (short) 0xfd9f;
                break;
            case "rtypeleo":
            case "rtypeleoj":
                m92_irq_vectorbase = 0x20;
                break;
            case "nbbatman":
            case "lnbbatmanu":
            case "leaguemn":
                dsw = (short) 0xff9f;
                break;
            case "ssoldier":
            case "psoldier":
                dsw = (short) 0xff9f;
                sound_status = 0x80;
                m92_irq_vectorbase = 0x20;
                break;
        }
        if (Memory.mainrom == null || Memory.audiorom == null || gfx11rom == null || gfx21rom == null) {
            Machine.bRom = false;
        }
    }

    public static void machine_start_m92() {
        setvector_param = 0;
        setvector_callback();
        scanline_timer = Timer.allocCommon(M92::m92_scanline_interrupt, "m92_scanline_interrupt", false);
    }

    public static void machine_reset_m92() {
        Timer.adjustPeriodic(scanline_timer, Video.video_screen_get_time_until_pos(0, 0), Attotime.ATTOTIME_NEVER);
    }

    public static void m92_scanline_interrupt() {
        int scanline = m92_scanline_param;
        if (scanline == m92_raster_irq_position) {
            Cpuexec.cpu[0].cpunum_set_input_line_and_vector(0, 0, LineState.HOLD_LINE, (m92_irq_vectorbase + 8) / 4);
        } else if (scanline == 0xf8) {
            Cpuexec.cpu[0].cpunum_set_input_line_and_vector(0, 0, LineState.HOLD_LINE, (m92_irq_vectorbase + 0) / 4);
        }
        if (++scanline >= Video.screenstate.height) {
            scanline = 0;
        }
        m92_scanline_param = scanline;
        Timer.adjustPeriodic(scanline_timer, Video.video_screen_get_time_until_pos(scanline, 0), Attotime.ATTOTIME_NEVER);
    }

    public static byte m92_eeprom_r(int offset) {
        return eeprom[offset];
    }

    public static short m92_eeprom_r2(int offset) {
        return (short) (0xff00 | eeprom[offset]);
    }

    public static void m92_eeprom_w(int offset, byte data) {
        eeprom[offset] = data;
    }

    public static void m92_coincounter_w(byte data) {

    }

    public static void m92_bankswitch_w(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            bankaddress = 0x10_0000 + ((data & 0x7) * 0x1_0000);
            //set_m92_bank(machine);
        }
    }

    public static byte m92_sprite_busy_r() {
        return m92_sprite_buffer_busy;
    }

    public static void setvector_callback() {
        switch (setvector_param) {
            case 0:
                irqvector = 0;
                break;
            case 1:
                irqvector |= 0x2;
                break;
            case 2:
                irqvector &= (byte) 0xfd;
                break;
            case 3:
                irqvector |= 0x1;
                break;
            case 4:
                irqvector &= (byte) 0xfe;
                break;
        }
        if ((irqvector & 0x2) != 0) {
            Cpuint.interrupt_vector[1][0] = 0x18;
        } else if ((irqvector & 0x1) != 0) {
            Cpuint.interrupt_vector[1][0] = 0x19;
        }
        if (irqvector == 0) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.CLEAR_LINE);
        } else {
            Cpuint.cpunum_set_input_line(1, 0, LineState.ASSERT_LINE);
        }
    }

    public static void m92_soundlatch_w(short data) {
        setvector_param = 3;
        EmuTimer timer = Timer.allocCommon(M92::setvector_callback, "setvector_callback", true);
        Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        Sound.soundlatch_w((short) (data & 0xff));
    }

    public static short m92_sound_status_r() {
        return sound_status;
    }

    public static short m92_soundlatch_r() {
        return (short) (Sound.soundlatch_r() | 0xff00);
    }

    public static void m92_sound_irq_ack_w() {
        setvector_param = 4;
        EmuTimer timer = Timer.allocCommon(M92::setvector_callback, "setvector_callback", true);
        Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
    }

    public static void m92_sound_status_w(short data) {
        sound_status = data;
        Cpuexec.cpu[0].cpunum_set_input_line_and_vector(0, 0, LineState.HOLD_LINE, (m92_irq_vectorbase + 12) / 4);
    }

    public static void sound_irq(int state) {
        if (state != 0) {
            setvector_param = 1;
            EmuTimer timer = Timer.allocCommon(M92::setvector_callback, "setvector_callback", true);
            Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        } else {
            setvector_param = 2;
            EmuTimer timer = Timer.allocCommon(M92::setvector_callback, "setvector_callback", true);
            Timer.adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        }
    }

    public static void m92_sprite_interrupt() {
        Cpuexec.cpu[0].cpunum_set_input_line_and_vector(0, 0, LineState.HOLD_LINE, (m92_irq_vectorbase + 4) / 4);
    }

    public static void play_m92_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_m92_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_m92_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//    public static void play_m92_gunforce(TrackInfo ti) {
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number, 0, 36));
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
//    }
//
//    public static void stop_m92_gunforce(TrackInfo ti) {
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
//    }
//
//    public static void stopandplay_m92_gunforce(TrackInfo ti) {
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) RomInfo.iStop));
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 2, 0, 36));
//        Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 3, 0, (byte) ti.TrackID));
//    }


//#region Memory

    public static short short0, short1, short2, dsw;
    public static short short0_old, short1_old, short2_old;

    public static byte N0ReadOpByte(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0xf_ffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte N0ReadByte_m92(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x9_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0xa_0000 && address <= 0xb_ffff) {
            int offset = address - 0xa_0000;
            result = Memory.mainrom[bankaddress + offset];
        } else if (address >= 0xc_0000 && address <= 0xc_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0xd_0000 && address <= 0xd_ffff) {
            int offset = (address - 0xd_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (m92_vram_data[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) m92_vram_data[offset];
            }
        } else if (address >= 0xe_0000 && address <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0xf_8000 && address <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            result = (byte) Generic.spriteram16[offset];
        } else if (address >= 0xf_8800 && address <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            result = (byte) m92_paletteram_r(offset);
        } else if (address >= 0xf_fff0 && address <= 0xf_ffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static short N0ReadWord_m92(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x9_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        } else if (address >= 0xa_0000 && address + 1 <= 0xb_ffff) {
            int offset = address - 0xa_0000;
            result = (short) (Memory.mainrom[bankaddress + offset] + Memory.mainrom[bankaddress + offset + 1] * 0x100);
        } else if (address >= 0xc_0000 && address + 1 <= 0xc_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        } else if (address >= 0xd_0000 && address + 1 <= 0xd_ffff) {
            int offset = (address - 0xd_0000) / 2;
            result = m92_vram_data[offset];
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            result = (short) (Memory.mainram[offset] + Memory.mainram[offset + 1] * 0x100);
        } else if (address >= 0xf_8000 && address + 1 <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            result = Generic.spriteram16[offset];
        } else if (address >= 0xf_8800 && address + 1 <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            result = m92_paletteram_r(offset);
        } else if (address >= 0xf_fff0 && address + 1 <= 0xf_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        }
        return result;
    }

    public static void N0WriteByte_m92(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0xd_0000 && address <= 0xd_ffff) {
            int offset = (address - 0xd_0000) / 2;
            if (address % 2 == 0) {
                m92_vram_data[offset] = (short) ((value << 8) | (m92_vram_data[offset] & 0xff));
            } else if (address % 2 == 1) {
                m92_vram_data[offset] = (short) ((m92_vram_data[offset] & 0xff00) | value);
            }
            m92_vram_w(offset);
        } else if (address >= 0xe_0000 && address <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0xf_8000 && address <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xf_8800 && address <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            m92_paletteram_w(offset, value);
        } else if (address >= 0xf_9000 && address <= 0xf_900f) {
            int offset = (address - 0xf_9000) / 2;
            if (address % 2 == 0) {
                m92_spritecontrol_w1(offset, value);
            } else if (address % 2 == 1) {
                m92_spritecontrol_w2(offset, value);
            }
        } else if (address >= 0xf_9800 && address <= 0xf_9801) {
            if (address % 2 == 1) {
                m92_videocontrol_w(value);
            }
        }
    }

    public static void N0WriteWord_m92(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0xd_0000 && address + 1 <= 0xd_ffff) {
            int offset = (address - 0xd_0000) / 2;
            m92_vram_data[offset] = value;
            m92_vram_w(offset);
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            Memory.mainram[offset] = (byte) value;
            Memory.mainram[offset + 1] = (byte) (value >> 8);
        } else if (address >= 0xf_8000 && address + 1 <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xf_8800 && address + 1 <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            m92_paletteram_w(offset, value);
        } else if (address >= 0xf_9000 && address + 1 <= 0xf_900f) {
            int offset = (address - 0xf_9000) / 2;
            m92_spritecontrol_w(offset, value);
        } else if (address >= 0xf_9800 && address + 1 <= 0xf_9801) {
            m92_videocontrol_w((byte) value);
        }
    }

    public static byte N0ReadIOByte_m92(int address) {
        byte result = 0;
        if (address >= 0x00 && address <= 0x01) {
            if (address == 0x00) {
                result = (byte) short0;
            } else if (address == 0x01) {
                result = (byte) (short0 >> 8);
            }
        } else if (address >= 0x02 && address <= 0x03) {
            result = (byte) short1;
        } else if (address >= 0x04 && address <= 0x05) {
            result = (byte) dsw;
        } else if (address >= 0x06 && address <= 0x07) {
            result = (byte) short2;
        } else if (address >= 0x08 && address <= 0x09) {
            result = (byte) m92_sound_status_r();
        }
        return result;
    }

    public static short N0ReadIOWord_m92(int address) {
        short result = 0;
        if (address >= 0x00 && address + 1 <= 0x01) {
            result = short0;
        } else if (address >= 0x02 && address + 1 <= 0x03) {
            result = short1;
            result = (short) (result | (m92_sprite_busy_r() * 0x80));
        } else if (address >= 0x04 && address + 1 <= 0x05) {
            result = dsw;
        } else if (address >= 0x06 && address + 1 <= 0x07) {
            result = short2;
        } else if (address >= 0x08 && address + 1 <= 0x09) {
            result = m92_sound_status_r();
        }
        return result;
    }

    public static void N0WriteIOByte_m92(int address, byte value) {
        if (address >= 0x00 && address <= 0x01) {
            m92_soundlatch_w(value);
        } else if (address >= 0x02 && address <= 0x03) {
            m92_coincounter_w(value);
        } else if (address >= 0x20 && address <= 0x21) {
            m92_bankswitch_w(value);
        } else if (address >= 0x40 && address <= 0x43) {

        } else if (address >= 0x80 && address <= 0x87) {
            int offset = (address - 0x80) / 2;
            if (address % 2 == 0) {
                m92_pf1_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_pf1_control_w1(offset, value);
            }
        } else if (address >= 0x88 && address <= 0x8f) {
            int offset = (address - 0x88) / 2;
            if (address % 2 == 0) {
                m92_pf2_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_pf2_control_w1(offset, value);
            }
        } else if (address >= 0x90 && address <= 0x97) {
            int offset = (address - 0x90) / 2;
            if (address % 2 == 0) {
                m92_pf3_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_pf3_control_w1(offset, value);
            }
        } else if (address >= 0x98 && address <= 0x9f) {
            int offset = (address - 0x98) / 2;
            if (address % 2 == 0) {
                m92_master_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_master_control_w1(offset, value);
            }
        }
    }

    public static void N0WriteIOWord_m92(int address, short value) {
        if (address >= 0x00 && address + 1 <= 0x01) {
            m92_soundlatch_w(value);
        } else if (address >= 0x02 && address + 1 <= 0x03) {
            m92_coincounter_w((byte) value);
        } else if (address >= 0x20 && address + 1 <= 0x21) {
            m92_bankswitch_w((byte) value);
        } else if (address >= 0x40 && address + 1 <= 0x43) {

        } else if (address >= 0x80 && address + 1 <= 0x87) {
            int offset = (address - 0x80) / 2;
            m92_pf1_control_w(offset, value);
        } else if (address >= 0x88 && address + 1 <= 0x8f) {
            int offset = (address - 0x88) / 2;
            m92_pf2_control_w(offset, value);
        } else if (address >= 0x90 && address + 1 <= 0x97) {
            int offset = (address - 0x90) / 2;
            m92_pf3_control_w(offset, value);
        } else if (address >= 0x98 && address + 1 <= 0x9f) {
            int offset = (address - 0x98) / 2;
            m92_master_control_w(offset, value);
        }
    }

    public static byte N1ReadOpByte(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x1_ffff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf_fff0 && address <= 0xf_ffff) {
            int offset = address - 0xe_0000;
            result = Memory.audiorom[offset];
        }
        return result;
    }

    public static byte N1ReadByte(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x1_ffff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xa_0000 && address <= 0xa_3fff) {
            int offset = address - 0xa_0000;
            result = Memory.audioram[offset];
        } else if (address >= 0xa_8000 && address <= 0xa_803f) {
            int offset = (address - 0xa_8000) / 2;
            result = (byte) Iremga20.irem_ga20_r(offset);
        } else if (address >= 0xa_8042 && address <= 0xa_8043) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address >= 0xa_8044 && address <= 0xa_8045) {
            result = (byte) m92_soundlatch_r();
        } else if (address >= 0xf_fff0 && address <= 0xf_ffff) {
            int offset = address - 0xe_0000;
            result = Memory.mainrom[offset];
        }
        return result;
    }

    public static short N1ReadWord(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x1_ffff) {
            result = (short) (Memory.audiorom[address] + Memory.audiorom[address + 1] * 0x100);
        } else if (address >= 0xa_0000 && address + 1 <= 0xa_3fff) {
            int offset = address - 0xa_0000;
            result = (short) (Memory.audioram[offset] + Memory.audioram[offset + 1] * 0x100);
        } else if (address >= 0xa_8000 && address + 1 <= 0xa_803f) {
            int offset = (address - 0xa_8000) / 2;
            result = Iremga20.irem_ga20_r(offset);
        } else if (address >= 0xa_8042 && address + 1 <= 0xa_8043) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address >= 0xa_8044 && address + 1 <= 0xa_8045) {
            result = m92_soundlatch_r();
        } else if (address >= 0xf_fff0 && address + 1 <= 0xf_ffff) {
            int offset = address - 0xe_0000;
            result = (short) (Memory.mainrom[offset] + Memory.mainrom[offset + 1] * 0x100);
        }
        return result;
    }

    public static void N1WriteByte(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0x9_ff00 && address <= 0x9_ffff) {

        } else if (address >= 0xa_0000 && address <= 0xa_3fff) {
            int offset = address - 0xa_0000;
            Memory.audioram[offset] = value;
        } else if (address >= 0xa_8000 && address <= 0xa_803f) {
            int offset = (address - 0xa_8000) / 2;
            Iremga20.irem_ga20_w(offset, value);
        } else if (address >= 0xa_8040 && address <= 0xa_8041) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address >= 0xa_8042 && address <= 0xa_8043) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address >= 0xa_8044 && address <= 0xa_8045) {
            m92_sound_irq_ack_w();
        } else if (address >= 0xa_8046 && address <= 0xa_8047) {
            m92_sound_status_w(value);
        }
    }

    public static void N1WriteWord(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0x9_ff00 && address + 1 <= 0x9_ffff) {

        } else if (address >= 0xa_0000 && address + 1 <= 0xa_3fff) {
            int offset = address - 0xa_0000;
            Memory.audioram[offset] = (byte) value;
            Memory.audioram[offset + 1] = (byte) (value >> 8);
        } else if (address >= 0xa_8000 && address + 1 <= 0xa_803f) {
            int offset = (address - 0xa_8000) / 2;
            Iremga20.irem_ga20_w(offset, value);
        } else if (address >= 0xa_8040 && address + 1 <= 0xa_8041) {
            YM2151.ym2151_register_port_0_w((byte) value);
        } else if (address >= 0xa_8042 && address + 1 <= 0xa_8043) {
            YM2151.ym2151_data_port_0_w((byte) value);
        } else if (address >= 0xa_8044 && address + 1 <= 0xa_8045) {
            m92_sound_irq_ack_w();
        } else if (address >= 0xa_8046 && address + 1 <= 0xa_8047) {
            m92_sound_status_w(value);
        }
    }

//#endregion

//#region Memory2

    public static byte N0ReadByte_lethalth(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0x7_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x8_0000 && address <= 0x8_ffff) {
            int offset = (address - 0x8_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (m92_vram_data[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) m92_vram_data[offset];
            }
        } else if (address >= 0xe_0000 && address <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0xf_8000 && address <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            result = (byte) Generic.spriteram16[offset];
        } else if (address >= 0xf_8800 && address <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            result = (byte) m92_paletteram_r(offset);
        } else if (address >= 0xf_fff0 && address <= 0xf_ffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static short N0ReadWord_lethalth(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0 && address + 1 <= 0x7_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        } else if (address >= 0x8_0000 && address + 1 <= 0x8_ffff) {
            int offset = (address - 0x8_0000) / 2;
            result = m92_vram_data[offset];
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            result = (short) (Memory.mainram[offset] + Memory.mainram[offset + 1] * 0x100);
        } else if (address >= 0xf_8000 && address + 1 <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            result = Generic.spriteram16[offset];
        } else if (address >= 0xf_8800 && address + 1 <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            result = m92_paletteram_r(offset);
        } else if (address >= 0xf_fff0 && address + 1 <= 0xf_ffff) {
            result = (short) (Memory.mainrom[address] + Memory.mainrom[address + 1] * 0x100);
        }
        return result;
    }

    public static void N0WriteByte_lethalth(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0x8_0000 && address <= 0x8_ffff) {
            int offset = (address - 0x8_0000) / 2;
            if (address % 2 == 0) {
                m92_vram_data[offset] = (short) ((value << 8) | (m92_vram_data[offset] & 0xff));
            } else if (address % 2 == 1) {
                m92_vram_data[offset] = (short) ((m92_vram_data[offset] & 0xff00) | value);
            }
            m92_vram_w(offset);
        } else if (address >= 0xe_0000 && address <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0xf_8000 && address <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xf_8800 && address <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            m92_paletteram_w(offset, value);
        } else if (address >= 0xf_9000 && address <= 0xf_900f) {
            int offset = (address - 0xf_9000) / 2;
            if (address % 2 == 0) {
                m92_spritecontrol_w1(offset, value);
            } else if (address % 2 == 1) {
                m92_spritecontrol_w2(offset, value);
            }
        } else if (address >= 0xf_9800 && address <= 0xf_9801) {
            if (address % 2 == 1) {
                m92_videocontrol_w(value);
            }
        }
    }

    public static void N0WriteWord_lethalth(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0x8_0000 && address + 1 <= 0x8_ffff) {
            int offset = (address - 0x8_0000) / 2;
            m92_vram_data[offset] = value;
            m92_vram_w(offset);
        } else if (address >= 0xe_0000 && address + 1 <= 0xe_ffff) {
            int offset = address - 0xe_0000;
            Memory.mainram[offset] = (byte) value;
            Memory.mainram[offset + 1] = (byte) (value >> 8);
        } else if (address >= 0xf_8000 && address + 1 <= 0xf_87ff) {
            int offset = (address - 0xf_8000) / 2;
            Generic.spriteram16[offset] = value;
        } else if (address >= 0xf_8800 && address + 1 <= 0xf_8fff) {
            int offset = (address - 0xf_8800) / 2;
            m92_paletteram_w(offset, value);
        } else if (address >= 0xf_9000 && address + 1 <= 0xf_900f) {
            int offset = (address - 0xf_9000) / 2;
            m92_spritecontrol_w(offset, value);
        } else if (address >= 0xf_9800 && address + 1 <= 0xf_9801) {
            m92_videocontrol_w((byte) value);
        }
    }

    public static void N0WriteIOByte_lethalth(int address, byte value) {
        if (address >= 0x00 && address <= 0x01) {
            m92_soundlatch_w(value);
        } else if (address >= 0x02 && address <= 0x03) {
            m92_coincounter_w(value);
        } else if (address >= 0x40 && address <= 0x43) {

        } else if (address >= 0x80 && address <= 0x87) {
            int offset = (address - 0x80) / 2;
            if (address % 2 == 0) {
                m92_pf1_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_pf1_control_w1(offset, value);
            }
        } else if (address >= 0x88 && address <= 0x8f) {
            int offset = (address - 0x88) / 2;
            if (address % 2 == 0) {
                m92_pf2_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_pf2_control_w1(offset, value);
            }
        } else if (address >= 0x90 && address <= 0x97) {
            int offset = (address - 0x90) / 2;
            if (address % 2 == 0) {
                m92_pf3_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_pf3_control_w1(offset, value);
            }
        } else if (address >= 0x98 && address <= 0x9f) {
            int offset = (address - 0x98) / 2;
            if (address % 2 == 0) {
                m92_master_control_w2(offset, value);
            } else if (address % 2 == 1) {
                m92_master_control_w1(offset, value);
            }
        }
    }

    public static void N0WriteIOWord_lethalth(int address, short value) {
        if (address >= 0x00 && address + 1 <= 0x01) {
            m92_soundlatch_w(value);
        } else if (address >= 0x02 && address + 1 <= 0x03) {
            m92_coincounter_w((byte) value);
        } else if (address >= 0x40 && address + 1 <= 0x43) {

        } else if (address >= 0x80 && address + 1 <= 0x87) {
            int offset = (address - 0x80) / 2;
            m92_pf1_control_w(offset, value);
        } else if (address >= 0x88 && address + 1 <= 0x8f) {
            int offset = (address - 0x88) / 2;
            m92_pf2_control_w(offset, value);
        } else if (address >= 0x90 && address + 1 <= 0x97) {
            int offset = (address - 0x90) / 2;
            m92_pf3_control_w(offset, value);
        } else if (address >= 0x98 && address + 1 <= 0x9f) {
            int offset = (address - 0x98) / 2;
            m92_master_control_w(offset, value);
        }
    }

    public static byte N0ReadByte_majtitl2(int address) {
        address &= 0xf_ffff;
        byte result = 0;
        if (address >= 0xf_0000 && address <= 0xf_3fff) {
            int offset = (address - 0xf_0000) / 2;
            result = m92_eeprom_r(offset);
        } else {
            result = N0ReadByte_m92(address);
        }
        return result;
    }

    public static short N0ReadWord_majtitl2(int address) {
        address &= 0xf_ffff;
        short result = 0;
        if (address >= 0xf_0000 && address + 1 <= 0xf_3fff) {
            int offset = (address - 0xf_0000) / 2;
            result = m92_eeprom_r2(offset);
        } else {
            result = N0ReadWord_m92(address);
        }
        return result;
    }

    public static void N0WriteByte_majtitl2(int address, byte value) {
        address &= 0xf_ffff;
        if (address >= 0xf_0000 && address <= 0xf_3fff) {
            int offset = (address - 0xf_0000) / 2;
            m92_eeprom_w(offset, value);
        } else {
            N0WriteByte_m92(address, value);
        }
    }

    public static void N0WriteWord_majtitl2(int address, short value) {
        address &= 0xf_ffff;
        if (address >= 0xf_0000 && address + 1 <= 0xf_3fff) {
            int offset = (address - 0xf_0000) / 2;
            m92_eeprom_w(offset, (byte) value);
        } else {
            N0WriteWord_m92(address, value);
        }
    }

//#endregion

//#region State

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        writer.write(dsw);
        writer.write(irqvector);
        writer.write(sound_status);
        writer.write(bankaddress);
        writer.write(m92_irq_vectorbase);
        writer.write(m92_raster_irq_position);
        writer.write(m92_scanline_param);
        writer.write(setvector_param);
        writer.write(m92_sprite_buffer_busy);
        for (i = 0; i < 4; i++) {
            writer.write(pf_master_control[i]);
        }
        writer.write(m92_sprite_list);
        for (i = 0; i < 0x8000; i++) {
            writer.write(m92_vram_data[i]);
        }
        for (i = 0; i < 8; i++) {
            writer.write(m92_spritecontrol[i]);
        }
        writer.write(m92_game_kludge);
        writer.write(m92_palette_bank);
        for (i = 0; i < 3; i++) {
            writer.write(pf_layer[i].vram_base);
        }
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 4; j++) {
                writer.write(pf_layer[i].control[j]);
            }
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x400; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x400; i++) {
            writer.write(Generic.buffered_spriteram16[i]);
        }
        for (i = 0; i < 0x801; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1_0000);
        Nec.nn1[0].SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x4000);
        Nec.nn1[1].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        Cpuint.SaveStateBinary_v(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        Iremga20.SaveStateBinary(writer);
        writer.write(Sound.latched_value[0]);
        writer.write(Sound.utempdata[0]);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.iremga20stream.output_sampindex);
        writer.write(Sound.iremga20stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i, j;
            dsw = reader.readUInt16();
            irqvector = reader.readByte();
            sound_status = reader.readUInt16();
            bankaddress = reader.readInt32();
            m92_irq_vectorbase = reader.readByte();
            m92_raster_irq_position = reader.readInt32();
            m92_scanline_param = reader.readInt32();
            setvector_param = reader.readInt32();
            m92_sprite_buffer_busy = reader.readByte();
            for (i = 0; i < 4; i++) {
                pf_master_control[i] = reader.readUInt16();
            }
            m92_sprite_list = reader.readInt32();
            for (i = 0; i < 0x8000; i++) {
                m92_vram_data[i] = reader.readUInt16();
            }
            for (i = 0; i < 8; i++) {
                m92_spritecontrol[i] = reader.readUInt16();
            }
            m92_game_kludge = reader.readInt32();
            m92_palette_bank = reader.readInt32();
            for (i = 0; i < 3; i++) {
                pf_layer[i].vram_base = reader.readUInt16();
            }
            for (i = 0; i < 3; i++) {
                for (j = 0; j < 4; j++) {
                    pf_layer[i].control[j] = reader.readUInt16();
                }
            }
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x400; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x400; i++) {
                Generic.buffered_spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x801; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1_0000);
            Nec.nn1[0].LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x4000);
            Nec.nn1[1].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Cpuint.LoadStateBinary_v(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            Iremga20.LoadStateBinary(reader);
            Sound.latched_value[0] = reader.readUInt16();
            Sound.utempdata[0] = reader.readUInt16();
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.iremga20stream.output_sampindex = reader.readInt32();
            Sound.iremga20stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Input

    public static void loop_inputports_m92_common() {
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

    public static void replay_port() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                short0_old = Mame.brRecord.readUInt16();
                short1_old = Mame.brRecord.readUInt16();
                short2_old = Mame.brRecord.readUInt16();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
                //Mame.mame_pause(true);
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            short0 = short0_old;
            short1 = short1_old;
            short2 = short2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Tilemap

    public static void tilemap_init() {
        int i, j, k;
        for (i = 0; i < 3; i++) {
            M92.pf_layer[i].tmap = new Tmap();
            M92.pf_layer[i].tmap.laynum = i;
            M92.pf_layer[i].tmap.rows = 64;
            M92.pf_layer[i].tmap.cols = 64;
            M92.pf_layer[i].tmap.tilewidth = 8;
            M92.pf_layer[i].tmap.tileheight = 8;
            M92.pf_layer[i].tmap.width = 0x200;
            M92.pf_layer[i].tmap.height = 0x200;
            M92.pf_layer[i].tmap.enable = true;
            M92.pf_layer[i].tmap.all_tiles_dirty = true;
            M92.pf_layer[i].tmap.pixmap = new short[0x200 * 0x200];
            M92.pf_layer[i].tmap.flagsmap = new byte[0x200][0x200];
            M92.pf_layer[i].tmap.tileflags = new byte[0x40][0x40];
            M92.pf_layer[i].tmap.total_elements = M92.gfx11rom.length / 0x40;
            M92.pf_layer[i].tmap.pen_data = new byte[0x40];
            M92.pf_layer[i].tmap.pen_to_flags = new byte[3][0x10];
            M92.pf_layer[i].tmap.scrollrows = 512;
            M92.pf_layer[i].tmap.scrollcols = 1;
            M92.pf_layer[i].tmap.rowscroll = new int[M92.pf_layer[i].tmap.scrollrows];
            M92.pf_layer[i].tmap.colscroll = new int[M92.pf_layer[i].tmap.scrollcols];
            M92.pf_layer[i].tmap.tilemap_draw_instance3 = M92.pf_layer[i].tmap::tilemap_draw_instanceM92;
            M92.pf_layer[i].tmap.tile_update3 = M92.pf_layer[i].tmap::tile_updateM92;

            M92.pf_layer[i].wide_tmap = new Tmap();
            M92.pf_layer[i].wide_tmap.laynum = i;
            M92.pf_layer[i].wide_tmap.rows = 64;
            M92.pf_layer[i].wide_tmap.cols = 128;
            M92.pf_layer[i].wide_tmap.tilewidth = 8;
            M92.pf_layer[i].wide_tmap.tileheight = 8;
            M92.pf_layer[i].wide_tmap.width = 0x400;
            M92.pf_layer[i].wide_tmap.height = 0x200;
            M92.pf_layer[i].wide_tmap.enable = true;
            M92.pf_layer[i].wide_tmap.all_tiles_dirty = true;
            M92.pf_layer[i].wide_tmap.pixmap = new short[0x200 * 0x400];
            M92.pf_layer[i].wide_tmap.flagsmap = new byte[0x200][0x400];
            M92.pf_layer[i].wide_tmap.tileflags = new byte[0x40][0x80];
            M92.pf_layer[i].wide_tmap.total_elements = M92.gfx11rom.length / 0x40;
            M92.pf_layer[i].wide_tmap.pen_data = new byte[0x40];
            M92.pf_layer[i].wide_tmap.pen_to_flags = new byte[3][0x10];
            M92.pf_layer[i].wide_tmap.scrollrows = 512;
            M92.pf_layer[i].wide_tmap.scrollcols = 1;
            M92.pf_layer[i].wide_tmap.rowscroll = new int[M92.pf_layer[i].tmap.scrollrows];
            M92.pf_layer[i].wide_tmap.colscroll = new int[M92.pf_layer[i].tmap.scrollcols];
            M92.pf_layer[i].wide_tmap.tilemap_draw_instance3 = M92.pf_layer[i].wide_tmap::tilemap_draw_instanceM92;
            M92.pf_layer[i].wide_tmap.tile_update3 = M92.pf_layer[i].wide_tmap::tile_updateM92;
        }
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 3; j++) {
                pf_layer[i].tmap.pen_to_flags[j][0] = 0;
                pf_layer[i].wide_tmap.pen_to_flags[j][0] = 0;
            }
            for (k = 1; k < 0x10; k++) {
                pf_layer[i].tmap.pen_to_flags[0][k] = 0x20;
                pf_layer[i].wide_tmap.pen_to_flags[0][k] = 0x20;
            }
            for (k = 1; k < 8; k++) {
                pf_layer[i].tmap.pen_to_flags[1][k] = 0x20;
                pf_layer[i].wide_tmap.pen_to_flags[1][k] = 0x20;
            }
            for (k = 8; k < 0x10; k++) {
                pf_layer[i].tmap.pen_to_flags[1][k] = 0x10;
                pf_layer[i].wide_tmap.pen_to_flags[1][k] = 0x10;
            }
            for (k = 1; k < 0x10; k++) {
                pf_layer[i].tmap.pen_to_flags[2][k] = 0x10;
                pf_layer[i].wide_tmap.pen_to_flags[2][k] = 0x10;
            }
        }
        for (k = 0; k < 0x10; k++) {
            pf_layer[2].tmap.pen_to_flags[0][k] = 0x20;
            pf_layer[2].wide_tmap.pen_to_flags[0][k] = 0x20;
        }
        for (k = 0; k < 8; k++) {
            pf_layer[2].tmap.pen_to_flags[1][k] = 0x20;
            pf_layer[2].wide_tmap.pen_to_flags[1][k] = 0x20;
        }
        for (k = 8; k < 0x10; k++) {
            pf_layer[2].tmap.pen_to_flags[1][k] = 0x10;
            pf_layer[2].wide_tmap.pen_to_flags[1][k] = 0x10;
        }
        pf_layer[2].tmap.pen_to_flags[2][0] = 0x20;
        pf_layer[2].wide_tmap.pen_to_flags[2][0] = 0x20;
        for (k = 1; k < 0x10; k++) {
            pf_layer[2].tmap.pen_to_flags[2][k] = 0x10;
            pf_layer[2].wide_tmap.pen_to_flags[2][k] = 0x10;
        }
    }

//#endregion

//#region Video

    public static short[] pf_master_control;
    public static int m92_sprite_list;
    public static short[] m92_vram_data;
    public static short[] m92_spritecontrol;
    public static int m92_game_kludge;
    private static short[] uuB800;
    public static int m92_palette_bank;

    public static class pf_layer_info {

        public Tmap tmap;
        public Tmap wide_tmap;
        public short vram_base;
        public short[] control;
    }

    public static pf_layer_info[] pf_layer;

    public static void spritebuffer_callback() {
        m92_sprite_buffer_busy = 1;
        if (m92_game_kludge != 2) {
            m92_sprite_interrupt();
        }
    }

    public static void m92_spritecontrol_w1(int offset, byte data) {
        m92_spritecontrol[offset] = (short) ((data << 8) | (m92_spritecontrol[offset] & 0xff));
//        if (offset == 2) {
//            if ((data & 0xff) == 8) {
//                m92_sprite_list = (((0x100 - m92_spritecontrol[0]) & 0xff) * 4);
//            } else {
//                m92_sprite_list = 0x400;
//            }
//        }
        if (offset == 4) {
            Generic.buffer_spriteram16_w();
            m92_sprite_buffer_busy = 0;
            EmuTimer timer = Timer.allocCommon(M92::spritebuffer_callback, "spritebuffer_callback", true);
            Timer.adjustPeriodic(timer, Attotime.attotime_mul(new Atime(0, (long) (1e18 / 26666000)), 0x400), Attotime.ATTOTIME_NEVER);
        }
    }

    public static void m92_spritecontrol_w2(int offset, byte data) {
        m92_spritecontrol[offset] = (short) ((m92_spritecontrol[offset] & 0xff00) | data);
        if (offset == 2) {
            if ((data & 0xff) == 8) {
                m92_sprite_list = (((0x100 - m92_spritecontrol[0]) & 0xff) * 4);
            } else {
                m92_sprite_list = 0x400;
            }
        }
        if (offset == 4) {
            Generic.buffer_spriteram16_w();
            m92_sprite_buffer_busy = 0;
            EmuTimer timer = Timer.allocCommon(M92::spritebuffer_callback, "spritebuffer_callback", true);
            Timer.adjustPeriodic(timer, Attotime.attotime_mul(new Atime(0, (long) (1e18 / 26666000)), 0x400), Attotime.ATTOTIME_NEVER);
        }
    }

    public static void m92_spritecontrol_w(int offset, short data) {
        m92_spritecontrol[offset] = data;
        if (offset == 2) {
            if ((data & 0xff) == 8) {
                m92_sprite_list = (((0x100 - m92_spritecontrol[0]) & 0xff) * 4);
            } else {
                m92_sprite_list = 0x400;
            }
        }
        if (offset == 4) {
            Generic.buffer_spriteram16_w();
            m92_sprite_buffer_busy = 0;
            EmuTimer timer = Timer.allocCommon(M92::spritebuffer_callback, "spritebuffer_callback", true);
            Timer.adjustPeriodic(timer, Attotime.attotime_mul(new Atime(0, (long) (1e18 / 26666000)), 0x400), Attotime.ATTOTIME_NEVER);
        }
    }

    public static void m92_videocontrol_w(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            m92_palette_bank = (data >> 1) & 1;
        }
    }

    public static short m92_paletteram_r(int offset) {
        return Generic.paletteram16[offset + 0x400 * m92_palette_bank];
    }

    public static void m92_paletteram_w(int offset, short data) {
        Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 0x400 * m92_palette_bank, data);
    }

    public static void m92_vram_w(int offset) {
        int laynum;
        //COMBINE_DATA(&m92_vram_data[offset]);
        for (laynum = 0; laynum < 3; laynum++) {
            if ((offset & 0x6000) == pf_layer[laynum].vram_base) {
                pf_layer[laynum].tmap.tilemap_mark_tile_dirty(((offset & 0x1fff) / 2) / 0x40, ((offset & 0x1fff) / 2) % 0x40); // tilemap_mark_tile_dirty((offset & 0x1fff) / 2);
                pf_layer[laynum].wide_tmap.tilemap_mark_tile_dirty(((offset & 0x3fff) / 2) / 0x80, ((offset & 0x3fff) / 2) % 0x80);
            }
            if ((offset & 0x6000) == pf_layer[laynum].vram_base + 0x2000) {
                pf_layer[laynum].wide_tmap.tilemap_mark_tile_dirty(((offset & 0x3fff) / 2) / 0x80, ((offset & 0x3fff) / 2) % 0x80);
            }
        }
    }

    public static void m92_pf1_control_w1(int offset, byte data) {
        pf_layer[0].control[offset] = (short) ((data << 8) | (pf_layer[0].control[offset] & 0xff));
    }

    public static void m92_pf1_control_w2(int offset, byte data) {
        pf_layer[0].control[offset] = (short) ((pf_layer[0].control[offset] & 0xff00) | data);
    }

    public static void m92_pf1_control_w(int offset, short data) {
        pf_layer[0].control[offset] = data;
    }

    public static void m92_pf2_control_w1(int offset, byte data) {
        pf_layer[1].control[offset] = (short) ((data << 8) | (pf_layer[1].control[offset] & 0xff));
    }

    public static void m92_pf2_control_w2(int offset, byte data) {
        pf_layer[1].control[offset] = (short) ((pf_layer[1].control[offset] & 0xff00) | data);
    }

    public static void m92_pf2_control_w(int offset, short data) {
        pf_layer[1].control[offset] = data;
    }

    public static void m92_pf3_control_w1(int offset, byte data) {
        pf_layer[2].control[offset] = (short) ((data << 8) | (pf_layer[2].control[offset] & 0xff));
    }

    public static void m92_pf3_control_w2(int offset, byte data) {
        pf_layer[2].control[offset] = (short) ((pf_layer[2].control[offset] & 0xff00) | data);
    }

    public static void m92_pf3_control_w(int offset, short data) {
        pf_layer[2].control[offset] = data;
    }

    public static void m92_master_control_w1(int offset, byte data) {
        short old = pf_master_control[offset];
        pf_master_control[offset] = (short) ((data << 8) | (pf_master_control[offset] & 0xff));
        switch (offset) {
            case 0:
            case 1:
            case 2:
                pf_layer[offset].vram_base = (short) ((pf_master_control[offset] & 3) * 0x2000);
                if ((pf_master_control[offset] & 0x04) != 0) {
                    pf_layer[offset].tmap.enable = false;
                    pf_layer[offset].wide_tmap.enable = ((~pf_master_control[offset] >> 4) & 1) != 0 ? true : false;
                } else {
                    pf_layer[offset].tmap.enable = ((~pf_master_control[offset] >> 4) & 1) != 0 ? true : false;
                    pf_layer[offset].wide_tmap.enable = false;
                }
                if (((old ^ pf_master_control[offset]) & 0x07) != 0) {
                    pf_layer[offset].tmap.all_tiles_dirty = true;
                    pf_layer[offset].wide_tmap.all_tiles_dirty = true;
                }
                break;
            case 3:
                m92_raster_irq_position = pf_master_control[3] - 128;
                break;
        }
    }

    public static void m92_master_control_w2(int offset, byte data) {
        short old = pf_master_control[offset];
        pf_master_control[offset] = (short) ((pf_master_control[offset] & 0xff00) | data);
        switch (offset) {
            case 0:
            case 1:
            case 2:
                pf_layer[offset].vram_base = (short) ((pf_master_control[offset] & 3) * 0x2000);
                if ((pf_master_control[offset] & 0x04) != 0) {
                    pf_layer[offset].tmap.enable = false;
                    pf_layer[offset].wide_tmap.enable = ((~pf_master_control[offset] >> 4) & 1) != 0 ? true : false;
                } else {
                    pf_layer[offset].tmap.enable = ((~pf_master_control[offset] >> 4) & 1) != 0 ? true : false;
                    pf_layer[offset].wide_tmap.enable = false;
                }
                if (((old ^ pf_master_control[offset]) & 0x07) != 0) {
                    pf_layer[offset].tmap.all_tiles_dirty = true;
                    pf_layer[offset].wide_tmap.all_tiles_dirty = true;
                }
                break;
            case 3:
                m92_raster_irq_position = pf_master_control[3] - 128;
                break;
        }
    }

    public static void m92_master_control_w(int offset, short data) {
        short old = pf_master_control[offset];
        //COMBINE_DATA(&pf_master_control[offset]);
        pf_master_control[offset] = data;
        switch (offset) {
            case 0:
            case 1:
            case 2:
                pf_layer[offset].vram_base = (short) ((pf_master_control[offset] & 3) * 0x2000);
                if ((pf_master_control[offset] & 0x04) != 0) {
                    pf_layer[offset].tmap.enable = false;
                    pf_layer[offset].wide_tmap.enable = ((~pf_master_control[offset] >> 4) & 1) != 0;
                } else {
                    pf_layer[offset].tmap.enable = ((~pf_master_control[offset] >> 4) & 1) != 0;
                    pf_layer[offset].wide_tmap.enable = false;
                }
                if (((old ^ pf_master_control[offset]) & 0x07) != 0) {
                    pf_layer[offset].tmap.all_tiles_dirty = true;
                    pf_layer[offset].wide_tmap.all_tiles_dirty = true;
                }
                break;
            case 3:
                m92_raster_irq_position = pf_master_control[3] - 128;
                break;
        }
    }

    public static void video_start_m92() {
        int i;
        int laynum;
        uuB800 = new short[0x200 * 0x200];
        for (i = 0; i < 0x4_0000; i++) {
            uuB800[i] = 0x800;
        }
        for (laynum = 0; laynum < 3; laynum++) {
            pf_layer[laynum].tmap.tilemap_set_scrolldx(2 * laynum, -2 * laynum + 8);
            pf_layer[laynum].tmap.tilemap_set_scrolldy(-128, -128);
            pf_layer[laynum].wide_tmap.tilemap_set_scrolldx(2 * laynum - 256, -2 * laynum + 8 - 256);
            pf_layer[laynum].wide_tmap.tilemap_set_scrolldy(-128, -128);
        }
    }

    public static void draw_sprites(RECT cliprect) {
        int offs, k;
        for (k = 0; k < 8; k++) {
            for (offs = 0; offs < m92_sprite_list; ) {
                int x, y, sprite, colour, fx, fy, x_multi, y_multi, i, j, s_ptr, pri_back, pri_sprite;
                y = Generic.buffered_spriteram16[offs + 0] & 0x1ff;
                x = Generic.buffered_spriteram16[offs + 3] & 0x1ff;
                if ((Generic.buffered_spriteram16[offs + 2] & 0x0080) != 0) {
                    pri_back = 0;
                } else {
                    pri_back = 2;
                }
                sprite = Generic.buffered_spriteram16[offs + 1];
                colour = Generic.buffered_spriteram16[offs + 2] & 0x007f;
                pri_sprite = (Generic.buffered_spriteram16[offs + 0] & 0xe000) >> 13;
                fx = (Generic.buffered_spriteram16[offs + 2] >> 8) & 1;
                fy = (Generic.buffered_spriteram16[offs + 2] >> 9) & 1;
                y_multi = (Generic.buffered_spriteram16[offs + 0] >> 9) & 3;
                x_multi = (Generic.buffered_spriteram16[offs + 0] >> 11) & 3;
                y_multi = 1 << y_multi;
                x_multi = 1 << x_multi;
                offs += 4 * x_multi;
                if (pri_sprite != k) {
                    continue;
                }
                x = x - 16;
                y = 384 - 16 - y;
                if (fx != 0) {
                    x += 16 * (x_multi - 1);
                }
                for (j = 0; j < x_multi; j++) {
                    s_ptr = 8 * j;
                    if (fy == 0) {
                        s_ptr += y_multi - 1;
                    }
                    x &= 0x1ff;
                    for (i = 0; i < y_multi; i++) {
                        if (Generic.flip_screen_get() != 0) {
                            int i1 = 1;
//                            pdrawgfx(bitmap, machine -> gfx[1],
//                                    sprite + s_ptr,
//                                    colour,
//                                    !fx, !fy,
//                                    464 - x, 240 - (y - i * 16),
//                                    cliprect, TRANSPARENCY_PEN, 0, pri_back);
//
//                            pdrawgfx(bitmap, machine -> gfx[1],
//                                    sprite + s_ptr,
//                                    colour,
//                                    !fx, !fy,
//                                    464 - x + 512, 240 - (y - i * 16),
//                                    cliprect, TRANSPARENCY_PEN, 0, pri_back);

                        } else {
//                            pdrawgfx(bitmap, machine -> gfx[1],
//                                    sprite + s_ptr,
//                                    colour,
//                                    fx, fy,
//                                    x, y - i * 16,
//                                    cliprect, TRANSPARENCY_PEN, 0, pri_back);
//
//                            pdrawgfx(bitmap, machine -> gfx[1],
//                                    sprite + s_ptr,
//                                    colour,
//                                    fx, fy,
//                                    x - 512, y - i * 16,
//                                    cliprect, TRANSPARENCY_PEN, 0, pri_back);
                            Drawgfx.common_drawgfx_m92(gfx21rom, sprite + s_ptr, colour, fx, fy, x, y - i * 16, cliprect, pri_back | (1 << 31));
                            Drawgfx.common_drawgfx_m92(gfx21rom, sprite + s_ptr, colour, fx, fy, x - 512, y - i * 16, cliprect, pri_back | (1 << 31));
                        }
                        if (fy != 0) {
                            s_ptr++;
                        } else {
                            s_ptr--;
                        }
                    }
                    if (fx != 0) {
                        x -= 16;
                    } else {
                        x += 16;
                    }
                }
            }
        }
    }

    public static void m92_update_scroll_positions() {
        int laynum;
        int i;
        for (laynum = 0; laynum < 3; laynum++) {
            if ((pf_master_control[laynum] & 0x40) != 0) {
                int scrolldata_offset = (0xf400 + 0x400 * laynum) / 2;
                pf_layer[laynum].tmap.tilemap_set_scroll_rows(512);
                pf_layer[laynum].wide_tmap.tilemap_set_scroll_rows(512);
                for (i = 0; i < 512; i++) {
                    pf_layer[laynum].tmap.tilemap_set_scrollx(i, m92_vram_data[scrolldata_offset + i]);
                    pf_layer[laynum].wide_tmap.tilemap_set_scrollx(i, m92_vram_data[scrolldata_offset + i]);
                }
            } else {
                pf_layer[laynum].tmap.tilemap_set_scroll_rows(1);
                pf_layer[laynum].wide_tmap.tilemap_set_scroll_rows(1);
                pf_layer[laynum].tmap.tilemap_set_scrollx(0, pf_layer[laynum].control[2]);
                pf_layer[laynum].wide_tmap.tilemap_set_scrollx(0, pf_layer[laynum].control[2]);
            }
            pf_layer[laynum].tmap.tilemap_set_scrolly(0, pf_layer[laynum].control[0]);
            pf_layer[laynum].wide_tmap.tilemap_set_scrolly(0, pf_layer[laynum].control[0]);
        }
    }

    public static void m92_screenrefresh(RECT cliprect) {
        System.arraycopy(Tilemap.bb00, 0, Tilemap.priority_bitmap, 0x200 * cliprect.min_y, 0x200 * (cliprect.max_y - cliprect.min_y + 1));
        if (((~pf_master_control[2] >> 4) & 1) != 0) {
            pf_layer[2].wide_tmap.tilemap_draw_primask(cliprect, 0x20, (byte) 0);
            pf_layer[2].tmap.tilemap_draw_primask(cliprect, 0x20, (byte) 0);
            pf_layer[2].wide_tmap.tilemap_draw_primask(cliprect, 0x10, (byte) 1);
            pf_layer[2].tmap.tilemap_draw_primask(cliprect, 0x10, (byte) 1);
        } else {
            System.arraycopy(uuB800, 0, Video.bitmapbase[Video.curbitmap], 0x200 * cliprect.min_y, 0x200 * (cliprect.max_y - cliprect.min_y + 1));
        }
        pf_layer[1].wide_tmap.tilemap_draw_primask(cliprect, 0x20, (byte) 0);
        pf_layer[1].tmap.tilemap_draw_primask(cliprect, 0x20, (byte) 0);
        pf_layer[1].wide_tmap.tilemap_draw_primask(cliprect, 0x10, (byte) 1);
        pf_layer[1].tmap.tilemap_draw_primask(cliprect, 0x10, (byte) 1);
        pf_layer[0].wide_tmap.tilemap_draw_primask(cliprect, 0x20, (byte) 0);
        pf_layer[0].tmap.tilemap_draw_primask(cliprect, 0x20, (byte) 0);
        pf_layer[0].wide_tmap.tilemap_draw_primask(cliprect, 0x10, (byte) 1);
        pf_layer[0].tmap.tilemap_draw_primask(cliprect, 0x10, (byte) 1);
        draw_sprites(cliprect);
    }

    public static void video_update_m92() {
        m92_update_scroll_positions();
        m92_screenrefresh(Video.new_clip);
        if ((dsw & 0x100) != 0) {
            Generic.flip_screen_set(0);
        } else {
            Generic.flip_screen_set(1);
        }
    }

    public static void video_eof_m92() {
    }

//#endregion

//#region
//#endregion

//#region
//#endregion

//#region
//#endregion

//#region
//#endregion
}

