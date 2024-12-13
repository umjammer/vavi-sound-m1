/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.suna8;


import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
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
import m1.emu.Tilemap.RECT;
import m1.emu.Video;
import m1.sound.AY8910;
import m1.sound.FMOpl;
import m1.sound.Sample;
import m1.sound.Sound;


public class SunA8 {

    public static byte m_rombank, m_spritebank, m_palettebank, spritebank_latch;
    public static byte suna8_unknown;
    public static byte m_gfxbank;
    public static int m_has_text;
    public static GFXBANK_TYPE m_gfxbank_type;
    public static byte dsw1, dsw2, dswcheat;
    public static byte m_rombank_latch, m_nmi_enable;
    public static byte[] mainromop, gfx1rom, gfx12rom, samplesrom;
    public static int basebankmain;
    public static short[] samplebuf, samplebuf2;
    public static int sample;
    public static int sample_offset;

    public static void SunA8Init() {
        int i, n;
        Machine.bRom = true;
        switch (Machine.sName) {
            case "starfigh":
                Generic.spriteram = new byte[0x4000];
                mainromop = Machine.GetRom("maincpuop.rom");
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                samplesrom = Machine.GetRom("samples.rom");
                gfx12rom = Machine.GetRom("gfx1.rom");
                n = gfx12rom.length;
                gfx1rom = new byte[n * 2];
                for (i = 0; i < n; i++) {
                    gfx1rom[i * 2] = (byte) (gfx12rom[i] >> 4);
                    gfx1rom[i * 2 + 1] = (byte) (gfx12rom[i] & 0x0f);
                }
                Memory.mainram = new byte[0x1800];
                Memory.audioram = new byte[0x800];
                Generic.paletteram = new byte[0x200];
                if (mainromop == null || Memory.mainrom == null || Memory.audiorom == null || samplesrom == null || gfx12rom == null) {
                    Machine.bRom = false;
                }
                break;
        }
        if (Machine.bRom) {
            switch (Machine.sName) {
                case "starfigh":
                    dsw1 = 0x5f;
                    dsw2 = (byte) 0xff;
                    dswcheat = (byte) 0xbf;
                    Sample.info.starthandler = SunA8::suna8_sh_start;
                    break;
            }
        }
    }

    public static void hardhea2_flipscreen_w(byte data) {
        Generic.flip_screen_set(data & 0x01);
    }

    public static void starfigh_leds_w(byte data) {
        int bank;
        //set_led_status(0, data & 0x01);
        //set_led_status(1, data & 0x02);
        Generic.coin_counter_w(0, data & 0x04);
        m_gfxbank = (byte) ((data & 0x08) != 0 ? 4 : 0);
        bank = m_rombank_latch & 0x0f;
        basebankmain = 0x1_0000 + bank * 0x4000;
        //memory_set_bank(1,bank);
        m_rombank = m_rombank_latch;
    }

    public static void starfigh_rombank_latch_w(byte data) {
        m_rombank_latch = data;
    }

    public static void starfigh_sound_latch_w(byte data) {
        if ((m_rombank_latch & 0x20) == 0) {
            Sound.soundlatch_w(data);
        }
    }

    public static byte starfigh_cheats_r() {
        byte b1 = dswcheat;
        if (Video.video_screen_get_vblank()) {
            b1 = (byte) (dswcheat | 0x40);
        }
        return b1;
    }

    public static void hardhea2_interrupt() {
        switch (Cpuexec.iloops) {
            case 240:
                Cpuint.cpunum_set_input_line(0, 0, LineState.HOLD_LINE);
                break;
            case 112:
                if (m_nmi_enable != 0) {
                    Cpuint.cpunum_set_input_line(0, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
                }
                break;
        }
    }

    public static void starfigh_spritebank_latch_w(byte data) {
        spritebank_latch = (byte) ((data >> 2) & 1);
        m_nmi_enable = (byte) ((data >> 5) & 1);
    }

    public static void starfigh_spritebank_w() {
        m_spritebank = spritebank_latch;
    }

    public static void suna8_play_samples_w(int offset, byte data) {
        if (data != 0) {
            if ((~data & 0x10) != 0) {
                sample_offset = 0x800 * sample;
                if (sample_offset == 0x3000) {
                    int i1 = 1;
                }
                System.arraycopy(samplebuf, 0x800 * sample, samplebuf2, 0, 0x800);
                Sample.sample_start_raw(0, samplebuf2, 0x0800, 4000, 0);
            } else if ((~data & 0x08) != 0) {
                sample &= 3;
                sample_offset = 0x800 * (sample + 7);
                System.arraycopy(samplebuf, 0x800 * (sample + 7), samplebuf2, 0, 0x800);
                Sample.sample_start_raw(0, samplebuf2, 0x0800, 4000, 0);
            }
        }
    }

    public static void suna8_samples_number_w(int offset, byte data) {
        sample = data & 0xf;
    }

    public static void suna8_sh_start() {
        int i, len = samplesrom.length;
        samplebuf = new short[len];
        samplebuf2 = new short[0x800];
        for (i = 0; i < len; i++) {
            samplebuf[i] = (short) ((byte) (samplesrom[i] ^ 0x80) * 256);
        }
//        BinaryWriter bw1 = new BinaryWriter(new FileStream("/VS2008/compare1/compare1/bin/Debug/sample.dat", FileMode.Append));
//        for (i = 0; i < len; i++) {
//            bw1.write((byte) (samplebuf[i] >> 8));
//        }
//        bw1.close();
    }

    public static void machine_reset_suna8() {
    }

//#region Input

    public static void loop_inputports_suna8_starfigh() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte1 &= (byte) ~0x80;
        } else {
            byte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x80;
        } else {
            byte2 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte1 &= (byte) ~0x40;
        } else {
            byte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte2 &= (byte) ~0x40;
        } else {
            byte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte1 &= (byte) ~0x08;
        } else {
            byte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte1 &= (byte) ~0x04;
        } else {
            byte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte1 &= (byte) ~0x02;
        } else {
            byte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte1 &= (byte) ~0x01;
        } else {
            byte1 |= 0x01;
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
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte2 &= (byte) ~0x04;
        } else {
            byte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte2 &= (byte) ~0x02;
        } else {
            byte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte2 &= (byte) ~0x01;
        } else {
            byte2 |= 0x01;
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
    }

    public static void record_port_starfigh() {
        if (byte1 != byte1_old || byte2 != byte2_old) {
            byte1_old = byte1;
            byte2_old = byte2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(byte1);
            Mame.bwRecord.write(byte2);
        }
    }

    public static void replay_port_starfigh() throws IOException {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                byte1_old = Mame.brRecord.readByte();
                byte2_old = Mame.brRecord.readByte();
            } catch (Exception e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            byte1 = byte1_old;
            byte2 = byte2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Memory

    public static byte byte1, byte2;
    public static byte byte1_old, byte2_old;

    public static byte Z0ReadOp_starfigh(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = mainromop[address];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            int offset = address - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte Z0ReadMemory_starfigh(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            int offset = address - 0x8000;
            result = Memory.mainrom[basebankmain + offset];
        } else if (address == 0xc000) {
            result = byte1;
        } else if (address == 0xc001) {
            result = byte2;
        } else if (address == 0xc002) {
            result = dsw1;
        } else if (address == 0xc003) {
            result = dsw2;
        } else if (address == 0xc080) {
            result = starfigh_cheats_r();
        } else if (address >= 0xc600 && address <= 0xc7ff) {
            int offset = address - 0xc600;
            result = suna8_banked_paletteram_r(offset);
        } else if (address >= 0xc800 && address <= 0xdfff) {
            int offset = address - 0xc800;
            result = Memory.mainram[offset];
        } else if (address >= 0xe000 && address <= 0xffff) {
            int offset = address - 0xe000;
            result = suna8_banked_spriteram_r(offset);
        }
        return result;
    }

    public static void Z0WriteMemory_starfigh(short address, byte value) {
        if (address <= 0x7fff) {
            Memory.mainrom[address] = value;
        } else if (address >= 0x8000 && address <= 0xbfff) {
            int offset = address - 0x8000;
            Memory.mainrom[basebankmain + offset] = value;
        } else if (address == 0xc200) {
            starfigh_spritebank_w();
        } else if (address >= 0xc280 && address <= 0xc2ff) {
            starfigh_rombank_latch_w(value);
        } else if (address == 0xc300) {
            hardhea2_flipscreen_w(value);
        } else if (address >= 0xc380 && address <= 0xc3ff) {
            starfigh_spritebank_latch_w(value);
        } else if (address == 0xc400) {
            starfigh_leds_w(value);
        } else if (address == 0xc500) {
            starfigh_sound_latch_w(value);
        } else if (address >= 0xc600 && address <= 0xc7ff) {
            int offset = address - 0xc600;
            Generic.paletteram_RRRRGGGGBBBBxxxx_be_w(offset, value);
        } else if (address >= 0xc800 && address <= 0xdfff) {
            int offset = address - 0xc800;
            Memory.mainram[offset] = value;
        } else if (address >= 0xe000 && address <= 0xffff) {
            int offset = address - 0xe000;
            suna8_banked_spriteram_w(offset, value);
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

    public static byte Z1ReadOp_hardhead(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static byte Z1ReadMemory_hardhead(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            int offset = address - 0xc000;
            result = Memory.audioram[offset];
        } else if (address == 0xc800) {
            result = FMOpl.ym3812_read(0);
        } else if (address == 0xd800) {
            result = (byte) Sound.soundlatch_r();
        }
        return result;
    }

    public static void Z1Write_Memory_hardhead(short address, byte value) {
        if (address <= 0x7fff) {
            Memory.audiorom[address] = value;
        } else if (address >= 0xa000 && address <= 0xa001) {
            int offset = address - 0xa000;
            FMOpl.ym3812_write(offset, value);
        } else if (address >= 0xa002 && address <= 0xa003) {
            int offset = address - 0xa002;
            AY8910.AA8910[0].ay8910_write_ym(offset, value);
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            int offset = address - 0xc000;
            Memory.audioram[offset] = value;
        } else if (address == 0xd000) {
            Sound.soundlatch2_w(value);
        }
    }

    public static byte Z1ReadHardware(short address) {
        return 0;
    }

    public static void Z1WriteHardware(short address, byte value) {

    }

    public static int Z1IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[1].cpunum, 0);
    }

    //#endregion

//#region State

    public static void SaveStateBinary_starfigh(BinaryWriter writer) {

    }

    public static void LoadStateBinary_starfigh(BinaryReader reader) {

    }

//#endregion

//#region Video
public static RECT cliprect;
    public static short[] uuFF;

    public enum GFXBANK_TYPE {
        GFXBANK_TYPE_SPARKMAN,
        GFXBANK_TYPE_BRICKZN,
        GFXBANK_TYPE_STARFIGH
    }

    public static byte suna8_banked_paletteram_r(int offset) {
        offset += m_palettebank * 0x200;
        return Generic.paletteram[offset];
    }

    public static byte suna8_banked_spriteram_r(int offset) {
        offset += m_spritebank * 0x2000;
        return Generic.spriteram[offset];
    }

    public static void suna8_spriteram_w(int offset, byte data) {
        Generic.spriteram[offset] = data;
    }

    public static void suna8_banked_spriteram_w(int offset, byte data) {
        offset += m_spritebank * 0x2000;
        Generic.spriteram[offset] = data;
    }

    public static void brickzn_banked_paletteram_w(int offset, byte data) {
        byte r, g, b;
        short rgb;
        offset += m_palettebank * 0x200;
        Generic.paletteram[offset] = data;
        rgb = (short) ((Generic.paletteram[offset & ~1] << 8) + Generic.paletteram[offset | 1]);
        r = (byte) ((((rgb & (1 << 0xc)) != 0 ? 1 : 0) << 0) |
                (((rgb & (1 << 0xb)) != 0 ? 1 : 0) << 1) |
                (((rgb & (1 << 0xe)) != 0 ? 1 : 0) << 2) |
                (((rgb & (1 << 0xf)) != 0 ? 1 : 0) << 3));
        g = (byte) ((((rgb & (1 << 0x8)) != 0 ? 1 : 0) << 0) |
                (((rgb & (1 << 0x9)) != 0 ? 1 : 0) << 1) |
                (((rgb & (1 << 0xa)) != 0 ? 1 : 0) << 2) |
                (((rgb & (1 << 0xd)) != 0 ? 1 : 0) << 3));
        b = (byte) ((((rgb & (1 << 0x4)) != 0 ? 1 : 0) << 0) |
                (((rgb & (1 << 0x3)) != 0 ? 1 : 0) << 1) |
                (((rgb & (1 << 0x6)) != 0 ? 1 : 0) << 2) |
                (((rgb & (1 << 0x7)) != 0 ? 1 : 0) << 3));
        Palette.palette_set_callback.accept(offset / 2, Palette.make_rgb(Palette.pal4bit(r), Palette.pal4bit(g), Palette.pal4bit(b)));
    }

    public static void suna8_vh_start_common(int has_text, GFXBANK_TYPE gfxbank_type) {
        m_has_text = has_text;
        m_spritebank = 0;
        m_gfxbank = 0;
        m_gfxbank_type = gfxbank_type;
        m_palettebank = 0;
        if (m_has_text == 0) {
            Generic.paletteram = new byte[0x200 * 2];
            Generic.spriteram = new byte[0x2000 * 2 * 2];
            Arrays.fill(Generic.spriteram, 0, 0x2000 * 2 * 2, (byte) 0);
        }
    }

    public static void video_start_suna8_text() {
        suna8_vh_start_common(1, GFXBANK_TYPE.GFXBANK_TYPE_SPARKMAN);
    }

    public static void video_start_suna8_sparkman() {
        suna8_vh_start_common(0, GFXBANK_TYPE.GFXBANK_TYPE_SPARKMAN);
    }

    public static void video_start_suna8_brickzn() {
        suna8_vh_start_common(0, GFXBANK_TYPE.GFXBANK_TYPE_BRICKZN);
    }

    public static void video_start_suna8_starfigh() {
        int i;
        uuFF = new short[0x100 * 0x100];
        for (i = 0; i < 0x1_0000; i++) {
            uuFF[i] = 0xff;
        }
        cliprect = new RECT();
        cliprect.min_x = 0;
        cliprect.max_x = 0xff;
        cliprect.min_y = 0x10;
        cliprect.max_y = 0xef;
        suna8_vh_start_common(0, GFXBANK_TYPE.GFXBANK_TYPE_STARFIGH);
    }

    public static void draw_sprites(int start, int end, int which) {
        int i, x, y, bank, read_mask;
        int gfxbank, code, code2, color, addr, tile, attr, tile_flipx, tile_flipy, sx, sy;
        int spriteram_offset = which * 0x2000 * 2;
        int mx = 0;
        int max_x = 0xf8;
        int max_y = 0xf8;
        if (m_has_text != 0) {
            //fillbitmap(priority_bitmap,0,cliprect);
        }
        for (i = start; i < end; i += 4) {
            int srcpg, srcx, srcy, dimx, dimy, tx, ty;
            int colorbank = 0, flipx, flipy, multisprite;
            y = Generic.spriteram[spriteram_offset + i + 0];
            code = Generic.spriteram[spriteram_offset + i + 1];
            x = Generic.spriteram[spriteram_offset + i + 2];
            bank = Generic.spriteram[spriteram_offset + i + 3];
            read_mask = 0;
            if (m_has_text != 0) {
                read_mask = 1;
                if ((bank & 0xc0) == 0xc0) {
                    int text_list = (i - start) & 0x20;
                    int text_start = text_list != 0 ? 0x19c0 : 0x1980;
                    int write_mask = (text_list == 0) ? 1 : 0;
                    //draw_text_sprites(machine, bitmap, cliprect, text_start, text_start + 0x80, y, write_mask);
                    continue;
                }
                flipx = 0;
                flipy = 0;
                gfxbank = bank & 0x3f;
                switch (code & 0x80) {
                    case 0x80:
                        dimx = 2;
                        dimy = 32;
                        srcx = (code & 0xf) * 2;
                        srcy = 0;
                        srcpg = (code >> 4) & 3;
                        break;
                    case 0x00:
                    default:
                        dimx = 2;
                        dimy = 2;
                        srcx = (code & 0xf) * 2;
                        srcy = ((code >> 5) & 0x3) * 8 + 6;
                        srcpg = (code >> 4) & 1;
                        break;
                }
                multisprite = ((code & 0x80) != 0 && (code & 0x40) != 0) ? 1 : 0;
            } else {
                switch (code & 0xc0) {
                    case 0xc0:
                        dimx = 4;
                        dimy = 32;
                        srcx = (code & 0xe) * 2;
                        srcy = 0;
                        flipx = (code & 0x1);
                        flipy = 0;
                        gfxbank = bank & 0x1f;
                        srcpg = (code >> 4) & 3;
                        break;
                    case 0x80:
                        dimx = 2;
                        dimy = 32;
                        srcx = (code & 0xf) * 2;
                        srcy = 0;
                        flipx = 0;
                        flipy = 0;
                        gfxbank = bank & 0x1f;
                        srcpg = (code >> 4) & 3;
                        break;
                    case 0x40:
                        dimx = 4;
                        dimy = 4;
                        srcx = (code & 0xe) * 2;
                        flipx = code & 0x01;
                        flipy = bank & 0x10;
                        srcy = (((bank & 0x80) >> 4) + (bank & 0x04) + ((~bank >> 4) & 2)) * 2;
                        srcpg = ((code >> 4) & 3) + 4;
                        gfxbank = (bank & 0x3);
                        switch (m_gfxbank_type) {
                            case GFXBANK_TYPE_SPARKMAN:
                                break;
                            case GFXBANK_TYPE_BRICKZN:
                                gfxbank += 4;
                                break;
                            case GFXBANK_TYPE_STARFIGH:
                                if (gfxbank == 3)
                                    gfxbank += m_gfxbank;
                                break;
                        }
                        colorbank = (bank & 8) >> 3;
                        break;
                    case 0x00:
                    default:
                        dimx = 2;
                        dimy = 2;
                        srcx = (code & 0xf) * 2;
                        flipx = 0;
                        flipy = 0;
                        srcy = (((bank & 0x80) >> 4) + (bank & 0x04) + ((~bank >> 4) & 3)) * 2;
                        srcpg = (code >> 4) & 3;
                        gfxbank = bank & 0x03;
                        switch (m_gfxbank_type) {
                            case GFXBANK_TYPE_STARFIGH:
                                if (gfxbank == 3) {
                                    gfxbank += m_gfxbank;
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                }
                multisprite = ((code & 0x80) != 0 && (bank & 0x80) != 0) ? 1 : 0;
            }
            x = x - ((bank & 0x40) != 0 ? 0x100 : 0);
            y = (0x100 - y - dimy * 8) & 0xff;
            if (multisprite != 0) {
                mx += dimx * 8;
                x = mx;
            } else {
                mx = x;
            }
            gfxbank *= 0x400;
            for (ty = 0; ty < dimy; ty++) {
                for (tx = 0; tx < dimx; tx++) {
                    addr = (srcpg * 0x20 * 0x20) + ((srcx + (flipx != 0 ? dimx - tx - 1 : tx)) & 0x1f) * 0x20 + ((srcy + (flipy != 0 ? dimy - ty - 1 : ty)) & 0x1f);
                    tile = Generic.spriteram[addr * 2 + 0];
                    attr = Generic.spriteram[addr * 2 + 1];
                    tile_flipx = attr & 0x40;
                    tile_flipy = attr & 0x80;
                    sx = x + tx * 8;
                    sy = (y + ty * 8) & 0xff;
                    if (flipx != 0) {
                        tile_flipx = (tile_flipx == 0 ? 1 : 0);
                    }
                    if (flipy != 0) {
                        tile_flipy = (tile_flipy == 0 ? 1 : 0);
                    }
                    if (Generic.flip_screen_get() != 0) {
                        sx = max_x - sx;
                        tile_flipx = (tile_flipx == 0 ? 1 : 0);
                        sy = max_y - sy;
                        tile_flipy = (tile_flipy == 0 ? 1 : 0);
                    }
                    code2 = tile + (attr & 0x3) * 0x100 + gfxbank;
                    color = (((attr >> 2) & 0xf) ^ colorbank) + 0x10 * m_palettebank;
                    if (read_mask != 0) {
                        //((mygfx_element*)(m_gfxdecode->gfx(which)))->prio_mask_transpen(bitmap, cliprect,	code, color, tile_flipx, tile_flipy, sx, sy, screen.priority(), 0xf);
                    } else {
                        Drawgfx.common_drawgfx_starfigh(gfx1rom, code2, color, tile_flipx, tile_flipy, sx, sy, cliprect);
                        //m_gfxdecode->gfx(which)->transpen(bitmap, cliprect,	code, color, tile_flipx, tile_flipy, sx, sy, 0xf);
                    }
                }
            }
        }
    }

    public static void video_update_suna8() {
        System.arraycopy(uuFF, 0, Video.bitmapbase[Video.curbitmap], 0, 0x1_0000);
        draw_sprites(0x1d00, 0x2000, 0);
    }

    public static void video_eof_suna8() {

    }

//#endregion
}
