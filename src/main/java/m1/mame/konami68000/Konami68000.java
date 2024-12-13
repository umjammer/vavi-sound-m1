/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.konami68000;

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
import m1.emu.Drawgfx;
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
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.K007232;
import m1.sound.K053260;
import m1.sound.K054539;
import m1.sound.Sample;
import m1.sound.Sound;
import m1.sound.Upd7759;
import m1.sound.YM2151;


public class Konami68000 {

    public static byte[] gfx1rom, gfx2rom, gfx12rom, gfx22rom, titlerom, user1rom, zoomrom;
    public static byte dsw1, dsw2, dsw3, bytee;
    public static byte[] mainram2;
    public static short[] sampledata;
    public static short[] cuebrick_nvram, tmnt2_1c0800;
    private static int init_eeprom_count;
    private static int toggle, sprite_totel_element;
    private static int tmnt_soundlatch, cuebrick_snd_irqlatch, cuebrick_nvram_bank;
    public static int basebanksnd;

    public static void Konami68000Init() {
        int i, n1, n2;
        Generic.paletteram16 = new short[0x800];
        Generic.spriteram16 = new short[0x2000];
        init_eeprom_count = 10;
        toggle = 0;
        Memory.mainram = new byte[0x4000];
        Memory.audioram = new byte[0x2000]; // 0x800 prmrsocr_0x2000
        mainram2 = new byte[0x4000]; // 0x4000 tmnt2_ssriders_0x80
        layer_colorbase = new int[3];
        cuebrick_nvram = new short[0x400 * 0x20];
        tmnt2_1c0800 = new short[0x10];
        K053245_memory_region = new byte[2][];
        K053244_rombank = new int[2];
        K053245_ramsize = new int[2];
        K053245_dx = new int[2];
        K053245_dy = new int[2];
        K053245_ram = new byte[2][];
        K053245_buffer = new short[2][];
        K053244_regs = new byte[2][];
        K052109_charrombank = new byte[4];
        K052109_charrombank_2 = new byte[4];
        K053251_ram = new byte[0x10];
        K053251_palette_index = new int[5];
        K052109_dx = new int[3];
        K052109_dy = new int[3];
        for (i = 0; i < 2; i++) {
            K053245_ram[i] = new byte[0];
            K053245_buffer[i] = new short[0];
            K053244_regs[i] = new byte[0x10];
        }
        K053251_tilemaps = new Tmap[5];
        K053936_offset = new int[2][];
        for (i = 0; i < 2; i++) {
            K053936_offset[i] = new int[2];
        }
        K053936_wraparound = new int[2];
        K053936_0_ctrl = new short[0x10];
        K053936_0_linectrl = new short[0x800];
        K054000_ram = new byte[0x20];
        layerpri = new int[3];
        sorted_layer = new int[3];
        Machine.bRom = true;
        Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
        Memory.audiorom = Machine.GetRom("audiocpu.rom");
        gfx1rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
        n1 = gfx1rom.length;
        gfx12rom = new byte[n1 * 2];
        for (i = 0; i < n1; i++) {
            gfx12rom[i * 2] = (byte) (gfx1rom[i] >> 4);
            gfx12rom[i * 2 + 1] = (byte) (gfx1rom[i] & 0x0f);
        }
        gfx2rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
        n2 = gfx2rom.length;
        gfx22rom = new byte[n2 * 2];
        for (i = 0; i < n2; i++) {
            gfx22rom[i * 2] = (byte) (gfx2rom[i] >> 4);
            gfx22rom[i * 2 + 1] = (byte) (gfx2rom[i] & 0x0f);
        }
        sprite_totel_element = gfx22rom.length / 0x100;
        switch (Machine.sName) {
            case "cuebrick":
                K052109_memory_region = new byte[0x1_0000];// Machine.GetRom("k052109.rom");
                K051960_memory_region = new byte[0x1_0000]; // Machine.GetRom("k051960.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K051960_memory_region == null) {
                    Machine.bRom = false;
                }
                break;
            case "mia":
            case "mia2":
                K052109_memory_region = new byte[0x1_0000]; // Machine.GetRom("k052109.rom");
                K051960_memory_region = new byte[0x1_0000]; // Machine.GetRom("k051960.rom");
                K007232.k007232rom = Machine.GetRom("k007232.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K051960_memory_region == null || Memory.audiorom == null || K007232.k007232rom == null) {
                    Machine.bRom = false;
                }
                break;
            case "tmnt":
            case "tmntu":
            case "tmntua":
            case "tmntub":
            case "tmht":
            case "tmhta":
            case "tmhtb":
            case "tmntj":
            case "tmnta":
            case "tmht2p":
            case "tmht2pa":
            case "tmnt2pj":
            case "tmnt2po":
                K052109_memory_region = new byte[0x1_0000]; // Machine.GetRom("k052109.rom");
                K051960_memory_region = new byte[0x1_0000]; // Machine.GetRom("k051960.rom");
                K007232.k007232rom = Machine.GetRom("k007232.rom");
                Upd7759.updrom = Machine.GetRom("upd.rom");
                titlerom = Machine.GetRom("title.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K051960_memory_region == null || Memory.audiorom == null || K007232.k007232rom == null || Upd7759.updrom == null || titlerom == null) {
                    Machine.bRom = false;
                }
                break;
            case "punkshot":
            case "punkshot2":
            case "punkshotj":
            case "thndrx2":
            case "thndrx2a":
            case "thndrx2j":
                K052109_memory_region = new byte[0x1_0000]; // Machine.GetRom("k052109.rom");
                K051960_memory_region = new byte[0x1_0000]; // Machine.GetRom("k051960.rom");
                K053260.k053260rom = Machine.GetRom("k053260.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K051960_memory_region == null || Memory.audiorom == null || K053260.k053260rom == null) {
                    Machine.bRom = false;
                }
                break;
            case "lgtnfght":
            case "lgtnfghta":
            case "lgtnfghtu":
            case "trigon":
            case "blswhstl":
            case "blswhstla":
            case "detatwin":
            case "tmnt2":
            case "tmnt2a":
            case "tmht22pe":
            case "tmht24pe":
            case "tmnt22pu":
            case "qgakumon":
            case "ssriders":
            case "ssriderseaa":
            case "ssridersebd":
            case "ssridersebc":
            case "ssridersuda":
            case "ssridersuac":
            case "ssridersuab":
            case "ssridersubc":
            case "ssridersadd":
            case "ssridersabd":
            case "ssridersjad":
            case "ssridersjac":
            case "ssridersjbd":
                K052109_memory_region = new byte[0x1_0000]; // Machine.GetRom("k052109.rom");
                K053245_memory_region[0] = new byte[0x1_0000]; // Machine.GetRom("k053245.rom");
                K053260.k053260rom = Machine.GetRom("k053260.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K053245_memory_region[0] == null || Memory.audiorom == null || K053260.k053260rom == null) {
                    Machine.bRom = false;
                }
                break;
            case "glfgreat":
            case "glfgreatj":
                K052109_memory_region = new byte[0x1_0000]; // Machine.GetRom("k052109.rom");
                K053245_memory_region[0] = new byte[0x1_0000]; // Machine.GetRom("k053245.rom");
                zoomrom = new byte[0x1_0000]; // Machine.GetRom("zoom.rom");
                user1rom = new byte[0x1_0000]; // Machine.GetRom("user1.rom");
                K053260.k053260rom = Machine.GetRom("k053260.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K053245_memory_region[0] == null || zoomrom == null || user1rom == null || Memory.audiorom == null || K053260.k053260rom == null) {
                    Machine.bRom = false;
                }
                break;
            case "prmrsocr":
            case "prmrsocrj":
                K052109_memory_region = new byte[0x1_0000];// Machine.GetRom("k052109.rom");
                K053245_memory_region[0] = new byte[0x1_0000]; // Machine.GetRom("k053245.rom");
                zoomrom = new byte[0x1_0000]; // Machine.GetRom("zoom.rom");
                user1rom = new byte[0x1_0000]; // Machine.GetRom("user1.rom");
                K054539.k054539rom = Machine.GetRom("k054539.rom");
                if (Memory.mainrom == null || gfx1rom == null || gfx2rom == null || K052109_memory_region == null || K053245_memory_region[0] == null || zoomrom == null || user1rom == null || Memory.audiorom == null || K054539.k054539rom == null) {
                    Machine.bRom = false;
                }
                break;
        }
        if (Machine.bRom) {
            switch (Machine.sName) {
                case "cuebrick":
                    dsw1 = 0x56;
                    dsw2 = (byte) 0xff;
                    dsw3 = 0x0f;
                    K052109_callback = Konami68000::cuebrick_tile_callback;
                    K051960_callback = Konami68000::mia_sprite_callback;
                    break;
                case "mia":
                case "mia2":
                    dsw1 = (byte) 0xff;
                    dsw2 = 0x56;
                    dsw3 = 0x0f;
                    K052109_callback = Konami68000::mia_tile_callback;
                    K051960_callback = Konami68000::mia_sprite_callback;
                    break;
                case "tmnt":
                case "tmntu":
                case "tmntua":
                case "tmntub":
                case "tmht":
                case "tmhta":
                case "tmhtb":
                case "tmntj":
                case "tmnta":
                    dsw1 = 0x0f;
                    dsw2 = 0x5f;
                    dsw3 = (byte) 0xff;
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K051960_callback = Konami68000::tmnt_sprite_callback;
                    break;
                case "tmht2p":
                case "tmht2pa":
                case "tmnt2pj":
                case "tmnt2po":
                    dsw1 = (byte) 0xff;
                    dsw2 = 0x5f;
                    dsw3 = (byte) 0xff;
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K051960_callback = Konami68000::tmnt_sprite_callback;
                    break;
                case "punkshot":
                case "punkshot2":
                case "punkshotj":
                    dsw1 = (byte) 0xff;
                    dsw2 = 0x7f;
                    dsw3 = (byte) 0xff;
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K051960_callback = Konami68000::punkshot_sprite_callback;
                    break;
                case "lgtnfght":
                case "lgtnfghta":
                case "lgtnfghtu":
                case "trigon":
                    dsw1 = 0x5e;
                    dsw2 = (byte) 0xff;
                    dsw3 = (byte) 0xfd;
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K053245_callback = Konami68000::lgtnfght_sprite_callback;
                    break;
                case "blswhstl":
                case "blswhstla":
                case "detatwin":
                    bytee = (byte) 0xfe;
                    K052109_callback = Konami68000::blswhstl_tile_callback;
                    K053245_callback = Konami68000::blswhstl_sprite_callback;
                    break;
                case "glfgreat":
                case "glfgreatj":
                    dsw1 = (byte) 0xff;
                    dsw2 = 0x59;
                    dsw3 = (byte) 0xf7;
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K053245_callback = Konami68000::lgtnfght_sprite_callback;
                    break;
                case "tmnt2":
                case "tmnt2a":
                case "tmht22pe":
                case "tmht24pe":
                case "tmnt22pu":
                case "qgakumon":
                case "ssriders":
                case "ssriderseaa":
                case "ssridersebd":
                case "ssridersebc":
                case "ssridersuda":
                case "ssridersuac":
                case "ssridersuab":
                case "ssridersubc":
                case "ssridersadd":
                case "ssridersabd":
                case "ssridersjad":
                case "ssridersjac":
                case "ssridersjbd":
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K053245_callback = Konami68000::lgtnfght_sprite_callback;
                    break;
                case "thndrx2":
                case "thndrx2a":
                case "thndrx2j":
                    bytee = (byte) 0xfe;
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K051960_callback = Konami68000::thndrx2_sprite_callback;
                    break;
                case "prmrsocr":
                case "prmrsocrj":
                    K052109_callback = Konami68000::tmnt_tile_callback;
                    K053245_callback = Konami68000::prmrsocr_sprite_callback;
                    break;
            }
        }
    }

    public static void cuebrick_irq_handler(int irq) {
        cuebrick_snd_irqlatch = irq;
    }

    public static void konami68000_ym2151_irq_handler(int irq) {

    }

    public static short K052109_word_noA12_r(int offset) {
        int offset1 = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        return K052109_word_r(offset1);
    }

    public static void K052109_word_noA12_w(int offset, short data) {
        int offset1;
        offset1 = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        K052109_word_w(offset1, data);
    }

    public static void K052109_word_noA12_w1(int offset, byte data) {
        int offset1;
        offset1 = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        K052109_w(offset1, data);
    }

    public static void K052109_word_noA12_w2(int offset, byte data) {
        int offset1;
        offset1 = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        K052109_w(offset1 + 0x2000, data);
    }

    public static void punkshot_K052109_word_w(int offset, short data) {
        //if (ACCESSING_BITS_8_15)
        K052109_w(offset, (byte) ((data >> 8) & 0xff));
        //else if (ACCESSING_BITS_0_7)
        K052109_w(offset + 0x2000, (byte) (data & 0xff));
    }

    public static void punkshot_K052109_word_w1(int offset, byte data) {
        K052109_w(offset, data);
    }

    public static void punkshot_K052109_word_w2(int offset, byte data) {
        K052109_w(offset + 0x2000, data);
    }

    public static void punkshot_K052109_word_noA12_w(int offset, short data) {
        offset = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        punkshot_K052109_word_w(offset, data);
    }

    public static void punkshot_K052109_word_noA12_w1(int offset, byte data) {
        offset = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        punkshot_K052109_word_w1(offset, data);
    }

    public static void punkshot_K052109_word_noA12_w2(int offset, byte data) {
        offset = ((offset & 0x3000) >> 1) | (offset & 0x07ff);
        punkshot_K052109_word_w2(offset, data);
    }

    public static short K053245_scattered_word_r(int offset) {
        short result;
        if ((offset & 0x0031) != 0) {
            result = Generic.spriteram16[offset];
        } else {
            offset = ((offset & 0x000e) >> 1) | ((offset & 0x1fc0) >> 3);
            result = K053245_word_r(offset);
        }
        return result;
    }


    public static void K053245_scattered_word_w(int offset, short data) {
        Generic.spriteram16[offset] = data;
        if ((offset & 0x0031) == 0) {
            offset = ((offset & 0x000e) >> 1) | ((offset & 0x1fc0) >> 3);
            K053245_word_w(offset, data);
        }
    }

    public static void K053245_scattered_word_w1(int offset, byte data) {
        Generic.spriteram16[offset] = (short) ((data << 8) | (Generic.spriteram16[offset] & 0xff));
        if ((offset & 0x0031) == 0) {
            offset = ((offset & 0x000e) >> 1) | ((offset & 0x1fc0) >> 3);
            //K053245_word_w(offset, data);
            K053245_ram[0][offset * 2] = data;
        }
    }

    public static void K053245_scattered_word_w2(int offset, byte data) {
        Generic.spriteram16[offset] = (short) ((Generic.spriteram16[offset] & 0xff00) | data);
        if ((offset & 0x0031) == 0) {
            offset = ((offset & 0x000e) >> 1) | ((offset & 0x1fc0) >> 3);
            //K053245_word_w(offset, data);
            K053245_ram[0][offset * 2 + 1] = data;
        }
    }

    public static short K053244_word_noA1_r(int offset) {
        offset &= ~1;
        return (short) (K053244_r(offset + 1) | (K053244_r(offset) << 8));
    }

    public static void K053244_word_noA1_w(int offset, short data) {
        offset &= ~1;
        //if (ACCESSING_BITS_8_15)
        K053244_w(offset, (byte) ((data >> 8) & 0xff));
        //if (ACCESSING_BITS_0_7)
        K053244_w(offset + 1, (byte) (data & 0xff));
    }

    public static void K053244_word_noA1_w1(int offset, byte data) {
        offset &= ~1;
        K053244_w(offset, (byte) (data & 0xff));
    }

    public static void K053244_word_noA1_w2(int offset, byte data) {
        offset &= ~1;
        K053244_w(offset + 1, (byte) (data & 0xff));
    }

    public static void cuebrick_interrupt() {
        switch (Cpuexec.iloops) {
            case 0:
                Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
                break;
            default:
                if (cuebrick_snd_irqlatch != 0) {
                    Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
                }
                break;
        }
    }

    public static void punkshot_interrupt() {
        if (K052109_is_IRQ_enabled() != 0) {
            Generic.irq4_line_hold(0);
        }
    }

    public static void lgtnfght_interrupt() {
        if (K052109_is_IRQ_enabled() != 0) {
            Generic.irq5_line_hold(0);
        }
    }

    public static void tmnt_sound_command_w(short data) {
        //if (ACCESSING_BITS_0_7)
        Sound.soundlatch_w((short) (data & 0xff));
    }

    public static void tmnt_sound_command_w2(byte data) {
        //if (ACCESSING_BITS_0_7)
        Sound.soundlatch_w((short) (data & 0xff));
    }

    public static short punkshot_sound_r(int offset) {
        return K053260.k053260_0_r(2 + offset);
    }

    public static short blswhstl_sound_r(int offset) {
        return K053260.k053260_0_r(2 + offset);
    }

    public static short glfgreat_sound_r(int offset) {
        return (short) (K053260.k053260_0_r(2 + offset) << 8);
    }

    public static byte glfgreat_sound_r1(int offset) {
        return K053260.k053260_0_r(2 + offset);
    }

    public static void glfgreat_sound_w(int offset, short data) {
        //if (ACCESSING_BITS_8_15)
        K053260.k053260_0_w(offset, (byte) ((data >> 8) & 0xff));
        if (offset != 0) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
        }
    }

    public static void glfgreat_sound_w1(int offset, byte data) {
        K053260.k053260_0_w(offset, data);
        if (offset != 0) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
        }
    }

    public static void glfgreat_sound_w2(int offset, byte data) {
        if (offset != 0) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
        }
    }

    public static short prmrsocr_sound_r() {
        return Sound.soundlatch3_r();
    }

    public static void prmrsocr_sound_cmd_w(int offset, short data) {
        //if (ACCESSING_BITS_0_7)
        {
            data &= 0xff;
            if (offset == 0) {
                Sound.soundlatch_w(data);
            } else {
                Sound.soundlatch2_w(data);
            }
        }
    }

    public static void prmrsocr_sound_cmd_w2(int offset, byte data) {
        data &= (byte) 0xff;
        if (offset == 0) {
            Sound.soundlatch_w(data);
        } else {
            Sound.soundlatch2_w(data);
        }
    }

    public static void prmrsocr_sound_irq_w() {
        Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
    }

    public static void prmrsocr_audio_bankswitch_w(byte data) {
        basebanksnd = 0x1_0000 + (data & 7) * 0x4000;
    }

    public static short tmnt2_sound_r(int offset) {
        return K053260.k053260_0_r(2 + offset);
    }

    public static byte tmnt_sres_r() {
        return (byte) tmnt_soundlatch;
    }

    public static void tmnt_sres_w(byte data) {
        Upd7759.upd7759_reset_w(0, (byte) (data & 2));
        if ((data & 0x04) != 0) {
            if (Sample.sample_playing(0) == 0) {
                Sample.sample_start_raw(0, sampledata, 0x4_0000, 20000, 0);
            }
        } else {
            Sample.sample_stop(0);
        }
        tmnt_soundlatch = data;
    }

    public static void tmnt_decode_sample() {
        int i;
        sampledata = new short[0x4_0000];
        for (i = 0; i < 0x4_0000; i++) {
            int val = titlerom[2 * i] + titlerom[2 * i + 1] * 256;
            int expo = val >> 13;
            val = (val >> 3) & (0x3ff);
            val -= 0x200;
            val <<= (expo - 3);
            sampledata[i] = (short) val;
        }
    }

    public static void nmi_callback() {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.ASSERT_LINE);
    }

    public static void sound_arm_nmi_w() {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.CLEAR_LINE);
        EmuTimer timer = Timer.allocCommon(Konami68000::nmi_callback, "nmi_callback", true);
        Timer.adjustPeriodic(timer, new Atime(0, (long) 50e12), Attotime.ATTOTIME_NEVER);
    }

    public static short punkshot_kludge_r() {
        return 0;
    }

    public static byte punkshot_kludge_r1() {
        return 0;
    }

    public static short ssriders_protection_r() {
        int data = MC68000.m1.ReadWord.apply(0x10_5a0a);
        int cmd = MC68000.m1.ReadWord.apply(0x10_58fc);
        short result;
        switch (cmd) {
            case 0x100b:
                result = 0x0064;
                break;
            case 0x6003:
                result = (short) (data & 0x000f);
                break;
            case 0x6004:
                result = (short) (data & 0x001f);
                break;
            case 0x6000:
                result = (short) (data & 0x0001);
                break;
            case 0x0000:
                result = (short) (data & 0x00ff);
                break;
            case 0x6007:
                result = (short) (data & 0x00ff);
                break;
            case 0x8abc:
                data = -(short) MC68000.m1.ReadWord.apply(0x10_5818);
                data = ((data / 8 - 4) & 0x1f) * 0x40;
                data += ((MC68000.m1.ReadWord.apply(0x10_5cb0) + 256 * K052109_r(0x1a01) + K052109_r(0x1a00) - 6) / 8 + 12) & 0x3f;
                result = (short) data;
                break;
            default:
                result = (short) 0xffff;
                break;
        }
        return result;
    }

    public static void ssriders_protection_w(int offset) {
        if (offset == 1) {
            int logical_pri, hardware_pri;
            hardware_pri = 1;
            for (logical_pri = 1; logical_pri < 0x100; logical_pri <<= 1) {
                int i;

                for (i = 0; i < 128; i++) {
                    if ((MC68000.m1.ReadWord.apply(0x18_0006 + 128 * i) >> 8) == logical_pri) {
                        K053245_word_w2(8 * i, (short) hardware_pri);
                        hardware_pri++;
                    }
                }
            }
        }
    }

    public static byte getbytee() {
        byte result;
        if (Video.video_screen_get_vblank()) {
            result = (byte) 0xfe;
        } else {
            result = (byte) 0xf6;
        }
        return result;
    }

    public static short blswhstl_coin_r() {
        int res;
        res = sbyte0;
        if (init_eeprom_count != 0) {
            init_eeprom_count--;
            res &= 0xf7;
        }
        toggle ^= 0x40;
        return (short) (res ^ toggle);
    }

    public static short blswhstl_eeprom_r() {
        int res;
        res = Eeprom.eeprom_read_bit() | bytee;
        return (short) res;
    }

    public static short ssriders_eeprom_r() {
        int res;
        res = (Eeprom.eeprom_read_bit() | getbytee());
        if (init_eeprom_count != 0) {
            init_eeprom_count--;
            res &= 0x7f;
        }
        toggle ^= 0x04;
        return (short) (res ^ toggle);
    }

    public static void blswhstl_eeprom_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            Eeprom.eeprom_write_bit(data & 0x01);
            Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
            Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void blswhstl_eeprom_w2(byte data) {
        Eeprom.eeprom_write_bit(data & 0x01);
        Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void ssriders_eeprom_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            Eeprom.eeprom_write_bit(data & 0x01);
            Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
            Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            dim_c = data & 0x18;
            K053244_bankselect(0, ((data & 0x20) >> 5) << 2);
        }
    }

    public static void ssriders_eeprom_w2(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            Eeprom.eeprom_write_bit(data & 0x01);
            Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
            Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            dim_c = data & 0x18;
            K053244_bankselect(0, ((data & 0x20) >> 5) << 2);
        }
    }

    public static short thndrx2_in0_r() {
        short res;
        res = (short) ((sbyte0 << 8) | sbyte1);
        if (init_eeprom_count != 0) {
            init_eeprom_count--;
            res &= (short) 0xf7ff;
        }
        return res;
    }

    public static short thndrx2_eeprom_r() {
        int res;
        res = (Eeprom.eeprom_read_bit() << 8) | (short) ((bytee << 8) | sbyte2);
        toggle ^= 0x0800;
        return (short) (res ^ toggle);
    }

    public static void thndrx2_eeprom_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            Eeprom.eeprom_write_bit(data & 0x01);
            Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
            Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            if (last == 0 && (data & 0x20) != 0) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
                //cpunum_set_input_line_and_vector(machine, 1, 0, LineState.HOLD_LINE, 0xff);
            }
            last = data & 0x20;
            K052109_set_RMRD_line((data & 0x40) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void thndrx2_eeprom_w2(byte data) {
        Eeprom.eeprom_write_bit(data & 0x01);
        Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        if (last == 0 && (data & 0x20) != 0) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
        }
        last = data & 0x20;
        K052109_set_RMRD_line((data & 0x40) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static short prmrsocr_IN0_r() {
        short res;
        res = (short) ((sbyte0 << 8) | sbyte1);
        if (init_eeprom_count != 0) {
            init_eeprom_count--;
            res &= (short) 0xfdff;
        }
        return res;
    }

    public static short prmrsocr_eeprom_r() {
        return (short) ((Eeprom.eeprom_read_bit() << 8) | ((short) (bytee << 8) | sbyte2));
    }

    public static void prmrsocr_eeprom_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            prmrsocr_122000_w(data);
        }
        //if (ACCESSING_BITS_8_15)
        {
            Eeprom.eeprom_write_bit(data & 0x0100);
            Eeprom.eeprom_set_cs_line((data & 0x0200) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
            Eeprom.eeprom_set_clock_line((data & 0x0400) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void prmrsocr_eeprom_w1(byte data) {
        Eeprom.eeprom_write_bit((data & 0x01) << 8);
        Eeprom.eeprom_set_cs_line((data & 0x02) != 0 ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Eeprom.eeprom_set_clock_line((data & 0x04) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void prmrsocr_eeprom_w2(byte data) {
        prmrsocr_122000_w2(data);
    }

    public static short cuebrick_snd_r() {
        return (short) (YM2151.ym2151_status_port_0_r() << 8);
    }

    public static byte cuebrick_snd_r1() {
        return YM2151.ym2151_status_port_0_r();
    }

    public static void cuebrick_snd_w(int offset, short data) {
        if (offset != 0) {
            YM2151.ym2151_data_port_0_w((byte) (data >> 8));
        } else {
            YM2151.ym2151_register_port_0_w((byte) (data >> 8));
        }
    }

    public static void cuebrick_snd_w1(int offset, byte data) {
        if (offset != 0) {
            YM2151.ym2151_data_port_0_w(data);
        } else {
            YM2151.ym2151_register_port_0_w(data);
        }
    }

    public static void cuebrick_snd_w2(int offset, byte data) {
        if (offset != 0) {
            YM2151.ym2151_data_port_0_w((byte) 0);
        } else {
            YM2151.ym2151_register_port_0_w((byte) 0);
        }
    }

    public static short cuebrick_nv_r(int offset) {
        return cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)];
    }

    public static byte cuebrick_nv_r1(int offset) {
        return (byte) (cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)] >> 8);
    }

    public static byte cuebrick_nv_r2(int offset) {
        return (byte) cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)];
    }

    public static void cuebrick_nv_w(int offset, short data) {
        cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)] = data;
    }

    public static void cuebrick_nv_w1(int offset, byte data) {
        cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)] = (short) ((data << 8) | (cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)] & 0xff));
    }

    public static void cuebrick_nv_w2(int offset, byte data) {
        cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)] = (short) ((cuebrick_nvram[offset + (cuebrick_nvram_bank * 0x400 / 2)] & 0xff00) | data);
    }

    public static void cuebrick_nvbank_w(short data) {
        cuebrick_nvram_bank = (data >> 8);
    }

    public static void cuebrick_nvbank_w1(byte data) {
        cuebrick_nvram_bank = data;
    }

    public static void cuebrick_nvbank_w2(byte data) {
        cuebrick_nvram_bank = 0;
    }

    public static void ssriders_soundkludge_w() {
        Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
    }

    public static byte tmnt2_get_byte(int addr) {
        byte result = 0;
        if (addr <= 0x07_ffff) {
            result = Memory.mainrom[addr];
        } else if (addr >= 0x10_4000 && addr <= 0x10_7fff) {
            int offset = addr - 0x10_4000;
            result = Memory.mainram[offset];
        } else if (addr >= 0x18_0000 && addr <= 0x18_3fff) {
            int offset = (addr - 0x18_0000) / 2;
            if (addr % 2 == 0) {
                result = (byte) (Generic.spriteram16[offset] >> 8);
            } else {
                result = (byte) Generic.spriteram16[offset];
            }
        }
        return result;
    }

    public static short tmnt2_get_word(int addr) {
        short result = 0;
        addr *= 2;
        if (addr <= 0x07_ffff) {
            result = (short) (Memory.mainrom[addr] * 0x100 + Memory.mainrom[addr + 1]);
        } else if (addr >= 0x10_4000 && addr <= 0x10_7fff) {
            int offset = addr - 0x10_4000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (addr >= 0x18_0000 && addr <= 0x18_3fff) {
            int offset = (addr - 0x18_0000) / 2;
            result = Generic.spriteram16[offset];
        }
        return result;
    }

    public static void tmnt2_put_word(int addr, short data) {
        addr *= 2;
        if (addr >= 0x18_0000 && addr <= 0x18_3fff) {
            int offset = (addr - 0x18_0000) / 2;
            Generic.spriteram16[offset] = data;
            if ((offset & 0x0031) == 0) {
                int offset2;
                offset2 = ((offset & 0x000e) >> 1) | ((offset & 0x1fc0) >> 3);
                K053245_word_w(offset2, data);
                if (K053245_ram[0][2] == 0x11 && K053245_ram[0][3] == (byte) 0x80) {
                    int i1 = 1;
                }
            }
        } else if (addr >= 0x10_4000 && addr <= 0x10_7fff) {
            int offset = (addr - 0x10_4000) / 2;
            Memory.mainram[offset] = (byte) (data >> 8);
            Memory.mainram[offset + 1] = (byte) data;
        }
    }

    public static void tmnt2_1c0800_w(int offset, short data) {
        int src_addr, dst_addr, mod_addr, attr1, code, attr2, cbase, cmod, color;
        int xoffs, yoffs, xmod, ymod, zmod, xzoom, yzoom, i;
        short[] src, mod;
        src = new short[4];
        mod = new short[24];
        byte keepaspect, xlock, ylock, zlock;
        tmnt2_1c0800[offset] = data;
        if (offset != 0x18 / 2)// || !ACCESSING_BITS_8_15)
        {
            return;
        }
        if ((tmnt2_1c0800[8] & 0xff00) != 0x8200) {
            return;
        }
        src_addr = (tmnt2_1c0800[0] | (tmnt2_1c0800[1] & 0xff) << 16) >> 1;
        dst_addr = (tmnt2_1c0800[2] | (tmnt2_1c0800[3] & 0xff) << 16) >> 1;
        mod_addr = (tmnt2_1c0800[4] | (tmnt2_1c0800[5] & 0xff) << 16) >> 1;
        zlock = (byte) (((tmnt2_1c0800[8] & 0xff) == 0x0001) ? 1 : 0);
        for (i = 0; i < 4; i++) {
            src[i] = tmnt2_get_word(src_addr + i);
        }
        for (i = 0; i < 24; i++) {
            mod[i] = tmnt2_get_word(mod_addr + i);
        }
        code = src[0];
        i = src[1];
        attr1 = i >> 2 & 0x3f00;
        attr2 = i & 0x380;
        cbase = i & 0x01f;
        cmod = mod[0x2a / 2] >> 8;
        color = (cbase != 0x0f && cmod <= 0x1f && zlock == 0) ? cmod : cbase;
        xoffs = src[2];
        yoffs = src[3];
        i = mod[0];
        attr2 |= i & 0x0060;
        keepaspect = (byte) (((i & 0x0014) == 0x0014) ? 1 : 0);
        if ((i & 0x8000) != 0) {
            attr1 |= 0x8000;
        }
        if (keepaspect != 0) {
            attr1 |= 0x4000;
        }
        if ((i & 0x4000) != 0) {
            attr1 ^= 0x1000;
            xoffs = -xoffs;
        }
        xmod = mod[6];
        ymod = mod[7];
        zmod = mod[8];
        xzoom = mod[0x1c / 2];
        yzoom = (keepaspect != 0) ? xzoom : mod[0x1e / 2];
        ylock = xlock = (byte) (((i & 0x0020) != 0 && (xzoom == 0 || xzoom == 0x100)) ? 1 : 0);
        if (xlock == 0) {
            i = xzoom - 0x4f00;
            if (i > 0) {
                i >>= 8;
                xoffs += (int) (Math.pow(i, 1.891292) * xoffs / 599.250121);
            } else if (i < 0) {
                i = (i >> 3) + (i >> 4) + (i >> 5) + (i >> 6) + xzoom;
                xoffs = (i > 0) ? (xoffs * i / 0x4f00) : 0;
            }
        }
        if (ylock == 0) {
            i = yzoom - 0x4f00;
            if (i > 0) {
                i >>= 8;
                yoffs += (int) (Math.pow(i, /*1.898461*/1.891292) * yoffs / 599.250121);
            } else if (i < 0) {
                i = (i >> 3) + (i >> 4) + (i >> 5) + (i >> 6) + yzoom;
                yoffs = (i > 0) ? (yoffs * i / 0x4f00) : 0;
            }
        }
        if (zlock == 0) {
            yoffs += zmod;
        }
        xoffs += xmod;
        yoffs += ymod;
        tmnt2_put_word(dst_addr + 0, (short) attr1);
        tmnt2_put_word(dst_addr + 2, (short) code);
        tmnt2_put_word(dst_addr + 4, (short) yoffs);
        tmnt2_put_word(dst_addr + 6, (short) xoffs);
        tmnt2_put_word(dst_addr + 12, (short) (attr2 | color));
    }

    public static void tmnt2_1c0800_w1(int offset, byte data) {
        int src_addr, dst_addr, mod_addr, attr1, code, attr2, cbase, cmod, color;
        int xoffs, yoffs, xmod, ymod, zmod, xzoom, yzoom, i;
        short[] src, mod;
        src = new short[4];
        mod = new short[24];
        byte keepaspect, xlock, ylock, zlock;
        tmnt2_1c0800[offset] = (short) ((data << 8) | (tmnt2_1c0800[offset] & 0xff));
        if (offset != 0x18 / 2) {
            return;
        }
        if ((tmnt2_1c0800[8] & 0xff00) != 0x8200) {
            return;
        }
        src_addr = (tmnt2_1c0800[0] | (tmnt2_1c0800[1] & 0xff) << 16) >> 1;
        dst_addr = (tmnt2_1c0800[2] | (tmnt2_1c0800[3] & 0xff) << 16) >> 1;
        mod_addr = (tmnt2_1c0800[4] | (tmnt2_1c0800[5] & 0xff) << 16) >> 1;
        zlock = (byte) (((tmnt2_1c0800[8] & 0xff) == 0x0001) ? 1 : 0);
        for (i = 0; i < 4; i++) {
            src[i] = tmnt2_get_word(src_addr + i);
        }
        for (i = 0; i < 24; i++) {
            mod[i] = tmnt2_get_word(mod_addr + i);
        }
        code = src[0];
        i = src[1];
        attr1 = i >> 2 & 0x3f00;
        attr2 = i & 0x380;
        cbase = i & 0x01f;
        cmod = mod[0x2a / 2] >> 8;
        color = (cbase != 0x0f && cmod <= 0x1f && zlock == 0) ? cmod : cbase;
        xoffs = src[2];
        yoffs = src[3];
        i = mod[0];
        attr2 |= i & 0x0060;
        keepaspect = (byte) (((i & 0x0014) == 0x0014) ? 1 : 0);
        if ((i & 0x8000) != 0) {
            attr1 |= 0x8000;
        }
        if (keepaspect != 0) {
            attr1 |= 0x4000;
        }
        if ((i & 0x4000) != 0) {
            attr1 ^= 0x1000;
            xoffs = -xoffs;
        }
        xmod = mod[6];
        ymod = mod[7];
        zmod = mod[8];
        xzoom = mod[0x1c / 2];
        yzoom = (keepaspect != 0) ? xzoom : mod[0x1e / 2];
        ylock = xlock = (byte) (((i & 0x0020) != 0 && (xzoom == 0 || xzoom == 0x100)) ? 1 : 0);
        if (xlock == 0) {
            i = xzoom - 0x4f00;
            if (i > 0) {
                i >>= 8;
                xoffs += (int) (Math.pow(i, 1.891292) * xoffs / 599.250121);
            } else if (i < 0) {
                i = (i >> 3) + (i >> 4) + (i >> 5) + (i >> 6) + xzoom;
                xoffs = (i > 0) ? (xoffs * i / 0x4f00) : 0;
            }
        }
        if (ylock == 0) {
            i = yzoom - 0x4f00;
            if (i > 0) {
                i >>= 8;
                yoffs += (int) (Math.pow(i, /*1.898461*/1.891292) * yoffs / 599.250121);
            } else if (i < 0) {
                i = (i >> 3) + (i >> 4) + (i >> 5) + (i >> 6) + yzoom;
                yoffs = (i > 0) ? (yoffs * i / 0x4f00) : 0;
            }
        }
        if (zlock == 0) {
            yoffs += zmod;
        }
        xoffs += xmod;
        yoffs += ymod;
        tmnt2_put_word(dst_addr + 0, (short) attr1);
        tmnt2_put_word(dst_addr + 2, (short) code);
        tmnt2_put_word(dst_addr + 4, (short) yoffs);
        tmnt2_put_word(dst_addr + 6, (short) xoffs);
        tmnt2_put_word(dst_addr + 12, (short) (attr2 | color));
    }

    public static void tmnt2_1c0800_w2(int offset, byte data) {
        tmnt2_1c0800[offset] = (short) ((tmnt2_1c0800[offset] & 0xff00) | data);
    }

    public static byte k054539_0_ctrl_r(int offset) {
        return K054539.k054539_0_r(0x200 + offset);
    }

    public static void k054539_0_ctrl_w(int offset, byte data) {
        K054539.k054539_0_w(0x200 + offset, data);
    }

    public static void volume_callback(int v) {
        K007232.k007232_set_volume(0, 0, (v >> 4) * 0x11, 0);
        K007232.k007232_set_volume(0, 1, 0, (v & 0x0f) * 0x11);
    }

    public static void sound_nmi() {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
    }

    public static void machine_reset_konami68000() {
        switch (Machine.sName) {
            case "tmnt":
            case "tmntu":
            case "tmntua":
            case "tmntub":
            case "tmht":
            case "tmhta":
            case "tmhtb":
            case "tmntj":
            case "tmnta":
            case "tmht2p":
            case "tmht2pa":
            case "tmnt2pj":
            case "tmnt2po":
                Upd7759.upd7759_0_start_w((byte) 0);
                Upd7759.upd7759_0_reset_w((byte) 1);
                break;
            case "prmrsocr":
            case "prmrsocrj":
                basebanksnd = 0;
                break;
        }
    }

    public static void play_konami68000_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_konami68000_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_konami68000_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//#region Memory

    public static byte sbyte0, sbyte1, sbyte2, sbyte3, sbyte4;
    public static byte sbyte0_old, sbyte1_old, sbyte2_old, sbyte3_old, sbyte4_old;
    public static byte bytee_old;

    public static byte MReadOpByte_cuebrick(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x01_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset];
        }
        return result;
    }

    public static byte MReadByte_cuebrick(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x01_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset];
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x0a_0002 && address <= 0x0a_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x0a_0004 && address <= 0x0a_0005) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw1;
            }
        } else if (address >= 0x0a_0012 && address <= 0x0a_0013) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw2;
            }
        } else if (address >= 0x0a_0018 && address <= 0x0a_0019) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw3;
            }
        } else if (address >= 0x0b_0000 && address <= 0x0b_03ff) {
            int offset = (address - 0x0b_0000) / 2;
            if (address % 2 == 0) {
                result = cuebrick_nv_r1(offset);
            } else {
                result = cuebrick_nv_r2(offset);
            }
        } else if (address >= 0x0c_0000 && address <= 0x0c_0003) {
            if (address % 2 == 0) {
                result = cuebrick_snd_r1();
            } else {
                result = (byte) 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x14_0000 && address <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = K051937_r(offset);
        } else if (address >= 0x14_0400 && address <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = K051960_r(offset);
        }
        return result;
    }

    public static short MReadOpWord_cuebrick(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x01_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 1 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = (short) (mainram2[offset] * 0x100 + mainram2[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_cuebrick(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x01_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 1 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = (short) (mainram2[offset] * 0x100 + mainram2[offset + 1]);
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            result = sbyte0;
        } else if (address >= 0x0a_0002 && address + 1 <= 0x0a_0003) {
            result = sbyte1;
        } else if (address >= 0x0a_0004 && address + 1 <= 0x0a_0005) {
            result = sbyte2;
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            result = dsw1;
        } else if (address >= 0x0a_0012 && address + 1 <= 0x0a_0013) {
            result = dsw2;
        } else if (address >= 0x0a_0018 && address + 1 <= 0x0a_0019) {
            result = dsw3;
        } else if (address >= 0x0b_0000 && address + 1 <= 0x0b_03ff) {
            int offset = (address - 0x0b_0000) / 2;
            result = cuebrick_nv_r(offset);
        } else if (address >= 0x0c_0000 && address + 1 <= 0x0c_0003) {
            result = cuebrick_snd_r();
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = (short) (K051937_r(offset) * 0x100 + K051937_r(offset + 1));
        } else if (address >= 0x14_0400 && address + 1 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = (short) (K051960_r(offset) * 0x100 + K051960_r(offset + 1));
        }
        return result;
    }

    public static int MReadOpLong_cuebrick(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x01_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 3 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset] * 0x100_0000 + mainram2[offset + 1] * 0x1_0000 + mainram2[offset + 2] * 0x100 + mainram2[offset + 3];
        }
        return result;
    }

    public static int MReadLong_cuebrick(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x01_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 3 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset] * 0x100_0000 + mainram2[offset + 1] * 0x1_0000 + mainram2[offset + 2] * 0x100 + mainram2[offset + 3];
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x0b_0000 && address + 3 <= 0x0b_03ff) {
            int offset = (address - 0x0b_0000) / 2;
            result = cuebrick_nv_r(offset) * 0x1_0000 + cuebrick_nv_r(offset + 1);
        } else if (address >= 0x0c_0000 && address + 3 <= 0x0c_0003) {
            result = 0; // cuebrick_snd_r();
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = (short) (K051937_r(offset) * 0x100_0000 + K051937_r(offset + 1) * 0x1_0000 + K051937_r(offset + 2) * 0x100 + K051937_r(offset + 3));
        } else if (address >= 0x14_0400 && address + 3 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = (short) (K051960_r(offset) * 0x100_0000 + K051960_r(offset + 1) * 0x1_0000 + K051960_r(offset + 2) * 0x100 + K051960_r(offset + 3));
        }
        return result;
    }

    public static void MWriteByte_cuebrick(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x04_0000 && address <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            mainram2[offset] = value;
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                tmnt_paletteram_word_w1(offset, value);
            } else if (address % 2 == 1) {
                tmnt_paletteram_word_w2(offset, value);
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                tmnt_0a0000_w2(value);
            }
        } else if (address >= 0x0a_0008 && address <= 0x0a_0009) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                tmnt_sound_command_w2(value);
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x0b_0000 && address <= 0x0b_03ff) {
            int offset = (address - 0x0b_0000) / 2;
            if (address % 2 == 0) {
                cuebrick_nv_w1(offset, value);
            } else if (address % 2 == 1) {
                cuebrick_nv_w2(offset, value);
            }
        } else if (address >= 0x0b_0400 && address <= 0x0b_0401) {
            int offset = (address - 0x0b_0400) / 2;
            if (address % 2 == 0) {
                cuebrick_nvbank_w1(value);
            } else if (address % 2 == 1) {
                cuebrick_nvbank_w2(value);
            }
        } else if (address >= 0x0c_0000 && address <= 0x0c_0003) {
            int offset = (address - 0x0c_0000) / 2;
            if (address % 2 == 0) {
                cuebrick_snd_w1(offset, value);
            } else if (address % 2 == 1) {
                cuebrick_snd_w2(offset, value);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        } else if (address >= 0x14_0000 && address <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, value);
        } else if (address >= 0x14_0400 && address <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, value);
        }
    }

    public static void MWriteWord_cuebrick(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x04_0000 && address + 1 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            tmnt_paletteram_word_w(offset, value);
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            tmnt_0a0000_w(value);
        } else if (address >= 0x0a_0008 && address + 1 <= 0x0a_0009) {
            tmnt_sound_command_w(value);
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x0b_0000 && address + 1 <= 0x0b_03ff) {
            int offset = (address - 0x0b_0000) / 2;
            cuebrick_nv_w(offset, value);
        } else if (address >= 0x0b_0400 && address + 1 <= 0x0b_0401) {
            int offset = (address - 0x0b_0400) / 2;
            cuebrick_nvbank_w(value);
        } else if (address >= 0x0c_0000 && address + 1 <= 0x0c_0003) {
            int offset = (address - 0x0c_0000) / 2;
            cuebrick_snd_w(offset, value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, value);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, (byte) (value >> 8));
            K051937_w(offset + 1, (byte) value);
        } else if (address >= 0x14_0400 && address + 1 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, (byte) (value >> 8));
            K051960_w(offset + 1, (byte) value);
        }
    }

    public static void MWriteLong_cuebrick(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x04_0000 && address + 3 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            tmnt_paletteram_word_w(offset, (short) (value >> 16));
            tmnt_paletteram_word_w(offset + 1, (short) value);
        } else if (address >= 0x0b_0000 && address + 3 <= 0x0b_03ff) {
            int offset = (address - 0x0b_0000) / 2;
            cuebrick_nv_w(offset, (short) (value >> 16));
            cuebrick_nv_w(offset + 1, (short) value);
        } else if (address >= 0x0c_0000 && address + 3 <= 0x0c_0003) {
            int offset = (address - 0x0c_0000) / 2;
            cuebrick_snd_w(offset, (short) (value >> 16));
            cuebrick_snd_w(offset + 1, (short) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, (byte) (value >> 24));
            K051937_w(offset + 1, (byte) (value >> 16));
            K051937_w(offset + 2, (byte) (value >> 8));
            K051937_w(offset + 3, (byte) value);
        } else if (address >= 0x14_0400 && address + 3 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, (byte) (value >> 24));
            K051960_w(offset + 1, (byte) (value >> 16));
            K051960_w(offset + 2, (byte) (value >> 8));
            K051960_w(offset + 3, (byte) value);
        }
    }

    public static byte MReadOpByte_mia(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset];
        }
        return result;
    }

    public static byte MReadByte_mia(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset];
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x0a_0002 && address <= 0x0a_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x0a_0004 && address <= 0x0a_0005) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw1;
            }
        } else if (address >= 0x0a_0012 && address <= 0x0a_0013) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw2;
            }
        } else if (address >= 0x0a_0018 && address <= 0x0a_0019) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw3;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x14_0000 && address <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = K051937_r(offset);
        } else if (address >= 0x14_0400 && address <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = K051960_r(offset);
        }
        return result;
    }

    public static short MReadOpWord_mia(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 1 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = (short) (mainram2[offset] * 0x100 + mainram2[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_mia(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 1 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = (short) (mainram2[offset] * 0x100 + mainram2[offset + 1]);
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            result = sbyte0;
        } else if (address >= 0x0a_0002 && address + 1 <= 0x0a_0003) {
            result = sbyte1;
        } else if (address >= 0x0a_0004 && address + 1 <= 0x0a_0005) {
            result = sbyte2;
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            result = dsw1;
        } else if (address >= 0x0a_0012 && address + 1 <= 0x0a_0013) {
            result = dsw2;
        } else if (address >= 0x0a_0018 && address + 1 <= 0x0a_0019) {
            result = dsw3;
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = (short) (K051937_r(offset) * 0x100 + K051937_r(offset + 1));
        } else if (address >= 0x14_0400 && address + 1 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = (short) (K051960_r(offset) * 0x100 + K051960_r(offset + 1));
        }
        return result;
    }

    public static int MReadOpLong_mia(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 3 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset] * 0x100_0000 + mainram2[offset + 1] * 0x1_0000 + mainram2[offset + 2] * 0x100 + mainram2[offset + 3];
        }
        return result;
    }

    public static int MReadLong_mia(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x04_0000 && address + 3 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = mainram2[offset] * 0x100_0000 + mainram2[offset + 1] * 0x1_0000 + mainram2[offset + 2] * 0x100 + mainram2[offset + 3];
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = (short) (K051937_r(offset) * 0x100_0000 + K051937_r(offset + 1) * 0x1_0000 + K051937_r(offset + 2) * 0x100 + K051937_r(offset + 3));
        } else if (address >= 0x14_0400 && address + 3 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = (short) (K051960_r(offset) * 0x100_0000 + K051960_r(offset + 1) * 0x1_0000 + K051960_r(offset + 2) * 0x100 + K051960_r(offset + 3));
        }
        return result;
    }

    public static void MWriteByte_mia(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x04_0000 && address <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            mainram2[offset] = value;
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                tmnt_paletteram_word_w1(offset, value);
            } else if (address % 2 == 1) {
                tmnt_paletteram_word_w2(offset, value);
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                tmnt_0a0000_w2(value);
            }
        } else if (address >= 0x0a_0008 && address <= 0x0a_0009) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                tmnt_sound_command_w2(value);
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        } else if (address >= 0x14_0000 && address <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, value);
        } else if (address >= 0x14_0400 && address <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, value);
        }
    }

    public static void MWriteWord_mia(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x04_0000 && address + 1 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            tmnt_paletteram_word_w(offset, value);
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            tmnt_0a0000_w(value);
        } else if (address >= 0x0a_0008 && address + 1 <= 0x0a_0009) {
            tmnt_sound_command_w(value);
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, value);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, (byte) (value >> 8));
            K051937_w(offset + 1, (byte) value);
        } else if (address >= 0x14_0400 && address + 1 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, (byte) (value >> 8));
            K051960_w(offset + 1, (byte) value);
        }
    }

    public static void MWriteLong_mia(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x04_0000 && address + 3 <= 0x04_3fff) {
            int offset = address - 0x04_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            tmnt_paletteram_word_w(offset, (short) (value >> 16));
            tmnt_paletteram_word_w(offset + 1, (short) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, (byte) (value >> 24));
            K051937_w(offset + 1, (byte) (value >> 16));
            K051937_w(offset + 2, (byte) (value >> 8));
            K051937_w(offset + 3, (byte) value);
        } else if (address >= 0x14_0400 && address + 3 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, (byte) (value >> 24));
            K051960_w(offset + 1, (byte) (value >> 16));
            K051960_w(offset + 2, (byte) (value >> 8));
            K051960_w(offset + 3, (byte) value);
        }
    }

    public static byte MReadOpByte_tmnt(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x05_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_tmnt(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x05_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x0a_0002 && address <= 0x0a_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x0a_0004 && address <= 0x0a_0005) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x0a_0006 && address <= 0x0a_0007) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw1;
            }
        } else if (address >= 0x0a_0012 && address <= 0x0a_0013) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw2;
            }
        } else if (address >= 0x0a_0014 && address <= 0x0a_0015) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte4;
            }
        } else if (address >= 0x0a_0018 && address <= 0x0a_0019) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw3;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x14_0000 && address <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = K051937_r(offset);
        } else if (address >= 0x14_0400 && address <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = K051960_r(offset);
        }
        return result;
    }

    public static short MReadOpWord_tmnt(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x05_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_tmnt(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x05_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            result = sbyte0;
        } else if (address >= 0x0a_0002 && address + 1 <= 0x0a_0003) {
            result = sbyte1;
        } else if (address >= 0x0a_0004 && address + 1 <= 0x0a_0005) {
            result = sbyte2;
        } else if (address >= 0x0a_0006 && address + 1 <= 0x0a_0007) {
            result = sbyte3;
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            result = dsw1;
        } else if (address >= 0x0a_0012 && address + 1 <= 0x0a_0013) {
            result = dsw2;
        } else if (address >= 0x0a_0014 && address + 1 <= 0x0a_0015) {
            result = sbyte4;
        } else if (address >= 0x0a_0018 && address + 1 <= 0x0a_0019) {
            result = dsw3;
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = (short) (K051937_r(offset) * 0x100 + K051937_r(offset + 1));
        } else if (address >= 0x14_0400 && address + 1 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = (short) (K051960_r(offset) * 0x100 + K051960_r(offset + 1));
        }
        return result;
    }

    public static int MReadOpLong_tmnt(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x05_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_tmnt(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x05_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            result = K051937_r(offset) * 0x100_0000 + K051937_r(offset + 1) * 0x1_0000 + K051937_r(offset + 2) * 0x100 + K051937_r(offset + 3);
        } else if (address >= 0x14_0400 && address + 3 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            result = K051960_r(offset) * 0x100_0000 + K051960_r(offset + 1) * 0x1_0000 + K051960_r(offset + 2) * 0x100 + K051960_r(offset + 3);
        }
        return result;
    }

    public static void MWriteByte_tmnt(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x06_0000 && address <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                tmnt_paletteram_word_w1(offset, value);
            } else if (address % 2 == 1) {
                tmnt_paletteram_word_w2(offset, value);
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                tmnt_0a0000_w2(value);
            }
        } else if (address >= 0x0a_0008 && address <= 0x0a_0009) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                tmnt_sound_command_w2(value);
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x0c_0000 && address <= 0x0c_0001) {
            tmnt_priority_w2(value);
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        } else if (address >= 0x14_0000 && address <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, value);
        } else if (address >= 0x14_0400 && address <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, value);
        }
    }

    public static void MWriteWord_tmnt(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x06_0000 && address + 1 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            tmnt_paletteram_word_w(offset, value);
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            tmnt_0a0000_w(value);
        } else if (address >= 0x0a_0008 && address + 1 <= 0x0a_0009) {
            tmnt_sound_command_w(value);
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x0c_0000 && address + 1 <= 0x0c_0001) {
            tmnt_priority_w(value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, value);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, (byte) (value >> 8));
            K051937_w(offset + 1, (byte) value);
        } else if (address >= 0x14_0400 && address + 1 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, (byte) (value >> 8));
            K051960_w(offset + 1, (byte) value);
        }
    }

    public static void MWriteLong_tmnt(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x06_0000 && address + 3 <= 0x06_3fff) {
            int offset = address - 0x06_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            tmnt_paletteram_word_w(offset, (short) (value >> 16));
            tmnt_paletteram_word_w(offset + 1, (short) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0007) {
            int offset = address - 0x14_0000;
            K051937_w(offset, (byte) (value >> 24));
            K051937_w(offset + 1, (byte) (value >> 16));
            K051937_w(offset + 2, (byte) (value >> 8));
            K051937_w(offset + 3, (byte) value);
        } else if (address >= 0x14_0400 && address + 3 <= 0x14_07ff) {
            int offset = address - 0x14_0400;
            K051960_w(offset, (byte) (value >> 24));
            K051960_w(offset + 1, (byte) (value >> 16));
            K051960_w(offset + 2, (byte) (value >> 8));
            K051960_w(offset + 3, (byte) value);
        }
    }

    public static byte MReadOpByte_punkshot(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_punkshot(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x09_0000 && address <= 0x09_0fff) {
            int offset = (address - 0x09_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {
                result = dsw2;
            } else if (address % 2 == 1) {
                result = dsw1;
            }
        } else if (address >= 0x0a_0002 && address <= 0x0a_0003) {
            if (address % 2 == 0) {
                result = dsw3;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x0a_0004 && address <= 0x0a_0005) {
            if (address % 2 == 0) {
                result = sbyte4;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x0a_0006 && address <= 0x0a_0007) {
            if (address % 2 == 0) {
                result = sbyte2;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x0a_0040 && address <= 0x0a_0043) {
            int offset = (address - 0x0a_0040) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) punkshot_sound_r(offset);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x11_0000 && address <= 0x11_0007) {
            int offset = address - 0x11_0000;
            result = K051937_r(offset);
        } else if (address >= 0x11_0400 && address <= 0x11_07ff) {
            int offset = address - 0x11_0400;
            result = K051960_r(offset);
        } else if (address >= 0xff_fffc && address <= 0xff_ffff) {
            result = punkshot_kludge_r1();
        }
        return result;
    }

    public static short MReadOpWord_punkshot(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_punkshot(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x09_0000 && address + 1 <= 0x09_0fff) {
            int offset = (address - 0x09_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            result = (short) ((dsw2 << 8) | dsw1);
        } else if (address >= 0x0a_0002 && address + 1 <= 0x0a_0003) {
            result = (short) ((dsw3 << 8) | sbyte0);
        } else if (address >= 0x0a_0004 && address + 1 <= 0x0a_0005) {
            result = (short) ((sbyte4 << 8) | sbyte3);
        } else if (address >= 0x0a_0006 && address + 1 <= 0x0a_0007) {
            result = (short) ((sbyte2 << 8) | sbyte1);
        } else if (address >= 0x0a_0040 && address + 1 <= 0x0a_0043) {
            int offset = (address - 0x0a_0040) / 2;
            result = punkshot_sound_r(offset);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x11_0000 && address + 1 <= 0x11_0007) {
            int offset = address - 0x11_0000;
            result = (short) (K051937_r(offset) * 0x100 + K051937_r(offset + 1));
        } else if (address >= 0x11_0400 && address + 1 <= 0x11_07ff) {
            int offset = address - 0x11_0400;
            result = (short) (K051960_r(offset) * 0x100 + K051960_r(offset + 1));
        } else if (address >= 0xff_fffc && address + 1 <= 0xff_ffff) {
            result = punkshot_kludge_r();
        }
        return result;
    }

    public static int MReadOpLong_punkshot(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_punkshot(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x09_0000 && address + 3 <= 0x09_0fff) {
            int offset = (address - 0x09_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x0a_0040 && address + 3 <= 0x0a_0043) {
            int offset = (address - 0x0a_0040) / 2;
            result = punkshot_sound_r(offset) * 0x1_0000 + punkshot_sound_r(offset + 1);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x11_0000 && address + 3 <= 0x11_0007) {
            int offset = address - 0x11_0000;
            result = K051937_r(offset) * 0x100_0000 + K051937_r(offset + 1) * 0x1_0000 + K051937_r(offset + 2) * 0x100 + K051937_r(offset + 3);
        } else if (address >= 0x11_0400 && address + 3 <= 0x11_07ff) {
            int offset = address - 0x11_0400;
            result = K051960_r(offset) * 0x100_0000 + K051960_r(offset + 1) * 0x1_0000 + K051960_r(offset + 2) * 0x100 + K051960_r(offset + 3);
        } else if (address >= 0xff_fffc && address + 3 <= 0xff_ffff) {
            result = punkshot_kludge_r();
        }
        return result;
    }

    public static void MWriteByte_punkshot(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x08_0000 && address <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x09_0000 && address <= 0x09_0fff) {
            int offset = (address - 0x09_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x0a_0020 && address <= 0x0a_0021) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                punkshot_0a0020_w2(value);
            }
        } else if (address >= 0x0a_0040 && address <= 0x0a_0041) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053260.k053260_0_lsb_w2(0, value);
            }
        } else if (address >= 0x0a_0060 && address <= 0x0a_007f) {
            int offset = (address - 0x0a_0060) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053251_lsb_w2(offset, value);
            }
        } else if (address >= 0x0a_0080 && address <= 0x0a_0081) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                punkshot_K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                punkshot_K052109_word_noA12_w2(offset, value);
            }
        } else if (address >= 0x11_0000 && address <= 0x11_0007) {
            int offset = address - 0x11_0000;
            K051937_w(offset, value);
        } else if (address >= 0x11_0400 && address <= 0x11_07ff) {
            int offset = address - 0x11_0400;
            K051960_w(offset, value);
        }
    }

    public static void MWriteWord_punkshot(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x08_0000 && address + 1 <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x09_0000 && address + 1 <= 0x09_0fff) {
            int offset = (address - 0x09_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x0a_0020 && address + 1 <= 0x0a_0021) {
            punkshot_0a0020_w(value);
        } else if (address >= 0x0a_0040 && address + 1 <= 0x0a_0041) {
            K053260.k053260_0_lsb_w(0, value);
        } else if (address >= 0x0a_0060 && address + 1 <= 0x0a_007f) {
            int offset = (address - 0x0a_0060) / 2;
            K053251_lsb_w(offset, value);
        } else if (address >= 0x0a_0080 && address + 1 <= 0x0a_0081) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            punkshot_K052109_word_noA12_w(offset, value);
        } else if (address >= 0x11_0000 && address + 1 <= 0x11_0007) {
            int offset = address - 0x11_0000;
            K051937_w(offset, (byte) (value >> 8));
            K051937_w(offset + 1, (byte) value);
        } else if (address >= 0x11_0400 && address + 1 <= 0x11_07ff) {
            int offset = address - 0x11_0400;
            K051960_w(offset, (byte) (value >> 8));
            K051960_w(offset + 1, (byte) value);
        }
    }

    public static void MWriteLong_punkshot(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x08_0000 && address + 3 <= 0x08_3fff) {
            int offset = address - 0x08_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x09_0000 && address + 3 <= 0x09_0fff) {
            int offset = (address - 0x09_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x0a_0060 && address + 3 <= 0x0a_007f) {
            int offset = (address - 0x0a_0060) / 2;
            K053251_lsb_w(offset, (short) (value >> 16));
            K053251_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            punkshot_K052109_word_noA12_w(offset, (short) (value >> 16));
            punkshot_K052109_word_noA12_w(offset + 1, (short) value);
        } else if (address >= 0x11_0000 && address + 3 <= 0x11_0007) {
            int offset = address - 0x11_0000;
            K051937_w(offset, (byte) (value >> 24));
            K051937_w(offset + 1, (byte) (value >> 16));
            K051937_w(offset + 2, (byte) (value >> 8));
            K051937_w(offset + 3, (byte) value);
        } else if (address >= 0x11_0400 && address + 3 <= 0x11_07ff) {
            int offset = address - 0x11_0400;
            K051960_w(offset, (byte) (value >> 24));
            K051960_w(offset + 1, (byte) (value >> 16));
            K051960_w(offset + 2, (byte) (value >> 8));
            K051960_w(offset + 3, (byte) value);
        }
    }

    public static byte MReadOpByte_lgtnfght(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x09_0000 && address <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_lgtnfght(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x09_0000 && address <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x0a_0000 && address <= 0x0a_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x0a_0002 && address <= 0x0a_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x0a_0004 && address <= 0x0a_0005) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x0a_0006 && address <= 0x0a_0007) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw1;
            }
        } else if (address >= 0x0a_0008 && address <= 0x0a_0009) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw2;
            }
        } else if (address >= 0x0a_0010 && address <= 0x0a_0011) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = dsw3;
            }
        } else if (address >= 0x0a_0020 && address <= 0x0a_0023) {
            int offset = (address - 0x0a_0020) / 2;
            if (address % 2 == 0) {
                result = (byte) (punkshot_sound_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) punkshot_sound_r(offset);
            }
        } else if (address >= 0x0b_0000 && address <= 0x0b_3fff) {
            int offset = (address - 0x0b_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053245_scattered_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053245_scattered_word_r(offset);
            }
        } else if (address >= 0x0c_0000 && address <= 0x0c_001f) {
            int offset = (address - 0x0c_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053244_word_noA1_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053244_word_noA1_r(offset);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_lgtnfght(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x09_0000 && address + 1 <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_lgtnfght(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x09_0000 && address + 1 <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x0a_0000 && address + 1 <= 0x0a_0001) {
            int offset = (address - 0x0a_0000) / 2;
            result = sbyte0;
        } else if (address >= 0x0a_0002 && address + 1 <= 0x0a_0003) {
            int offset = (address - 0x0a_0002) / 2;
            result = sbyte1;
        } else if (address >= 0x0a_0004 && address + 1 <= 0x0a_0005) {
            int offset = (address - 0x0a_0004) / 2;
            result = sbyte2;
        } else if (address >= 0x0a_0006 && address + 1 <= 0x0a_0007) {
            int offset = (address - 0x0a_0006) / 2;
            result = dsw1;
        } else if (address >= 0x0a_0008 && address + 1 <= 0x0a_0009) {
            int offset = (address - 0x0a_0008) / 2;
            result = dsw2;
        } else if (address >= 0x0a_0010 && address + 1 <= 0x0a_0011) {
            int offset = (address - 0x0a_0010) / 2;
            result = dsw3;
        } else if (address >= 0x0a_0020 && address + 1 <= 0x0a_0023) {
            int offset = (address - 0x0a_0020) / 2;
            result = punkshot_sound_r(offset);
        } else if (address >= 0x0b_0000 && address + 1 <= 0x0b_3fff) {
            int offset = (address - 0x0b_0000) / 2;
            result = K053245_scattered_word_r(offset);
        } else if (address >= 0x0c_0000 && address + 1 <= 0x0c_001f) {
            int offset = (address - 0x0c_0000) / 2;
            result = K053244_word_noA1_r(offset);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_lgtnfght(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x09_0000 && address + 3 <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_lgtnfght(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x09_0000 && address + 3 <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x0a_0020 && address + 3 <= 0x0a_0023) {
            int offset = (address - 0x0a_0020) / 2;
            result = punkshot_sound_r(offset) * 0x1_0000 + punkshot_sound_r(offset + 1);
        } else if (address >= 0x0b_0000 && address + 3 <= 0x0b_3fff) {
            int offset = (address - 0x0b_0000) / 2;
            result = K053245_scattered_word_r(offset) * 0x1_0000 + K053245_scattered_word_r(offset + 1);
        } else if (address >= 0x0c_0000 && address + 3 <= 0x0c_001f) {
            int offset = (address - 0x0c_0000) / 2;
            result = K053244_word_noA1_r(offset) * 0x1_0000 + K053244_word_noA1_r(offset + 1);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_lgtnfght(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x08_0000 && address <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x09_0000 && address <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x0a_0018 && address <= 0x0a_0019) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                lgtnfght_0a0018_w2(value);
            }
        } else if (address >= 0x0a_0020 && address <= 0x0a_0021) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053260.k053260_0_lsb_w2(0, value);
            }
        } else if (address >= 0x0a_0028 && address <= 0x0a_0029) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x0b_0000 && address <= 0x0b_3fff) {
            int offset = (address - 0x0b_0000) / 2;
            if (address % 2 == 0) {
                K053245_scattered_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K053245_scattered_word_w2(offset, value);
            }
        } else if (address >= 0x0c_0000 && address <= 0x0c_001f) {
            int offset = (address - 0x0c_0000) / 2;
            if (address % 2 == 0) {
                K053244_word_noA1_w1(offset, value);
            } else if (address % 2 == 1) {
                K053244_word_noA1_w2(offset, value);
            }
        } else if (address >= 0x0e_0000 && address <= 0x0e_001f) {
            int offset = (address - 0x0e_0000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053251_lsb_w2(offset, value);
            }
        } else if (address >= 0x10_0000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_lgtnfght(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x08_0000 && address + 1 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x09_0000 && address + 1 <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x0a_0018 && address + 1 <= 0x0a_0019) {
            lgtnfght_0a0018_w(value);
        } else if (address >= 0x0a_0020 && address + 1 <= 0x0a_0021) {
            K053260.k053260_0_lsb_w(0, value);
        } else if (address >= 0x0a_0028 && address + 1 <= 0x0a_0029) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x0b_0000 && address + 1 <= 0x0b_3fff) {
            int offset = (address - 0x0b_0000) / 2;
            K053245_scattered_word_w(offset, value);
        } else if (address >= 0x0c_0000 && address + 1 <= 0x0c_001f) {
            int offset = (address - 0x0c_0000) / 2;
            K053244_word_noA1_w(offset, value);
        } else if (address >= 0x0e_0000 && address + 1 <= 0x0e_001f) {
            int offset = (address - 0x0e_0000) / 2;
            K053251_lsb_w(offset, value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, value);
        }
    }

    public static void MWriteLong_lgtnfght(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x08_0000 && address + 3 <= 0x08_0fff) {
            int offset = (address - 0x08_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x09_0000 && address + 3 <= 0x09_3fff) {
            int offset = address - 0x09_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x0b_0000 && address + 3 <= 0x0b_3fff) {
            int offset = (address - 0x0b_0000) / 2;
            K053245_scattered_word_w(offset, (short) (value >> 16));
            K053245_scattered_word_w(offset + 1, (short) value);
        } else if (address >= 0x0c_0000 && address + 3 <= 0x0c_001f) {
            int offset = (address - 0x0c_0000) / 2;
            K053244_word_noA1_w(offset, (short) (value >> 16));
            K053244_word_noA1_w(offset + 1, (short) value);
        } else if (address >= 0x0e_0000 && address + 3 <= 0x0e_001f) {
            int offset = (address - 0x0e_0000) / 2;
            K053251_lsb_w(offset, (short) (value >> 16));
            K053251_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_ssriders(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x0b_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            result = Memory.mainram[address - 0x10_4000];
        }
        return result;
    }

    public static byte MReadByte_ssriders(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x0b_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            result = Memory.mainram[address - 0x10_4000];
        } else if (address >= 0x14_0000 && address <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x18_0000 && address <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053245_scattered_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053245_scattered_word_r(offset);
            }
        } else if (address >= 0x1c_0000 && address <= 0x1c_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x1c_0002 && address <= 0x1c_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x1c_0004 && address <= 0x1c_0005) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x1c_0006 && address <= 0x1c_0007) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte4;
            }
        } else if (address >= 0x1c_0100 && address <= 0x1c_0101) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x1c_0102 && address <= 0x1c_0103) {
            if (address % 2 == 0) {
                result = (byte) (ssriders_eeprom_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) ssriders_eeprom_r();
            }
        } else if (address >= 0x1c_0400 && address <= 0x1c_0401) {
            result = (byte) Generic.watchdog_reset16_r();
        } else if (address >= 0x1c_0500 && address <= 0x1c_057f) {
            result = mainram2[address - 0x1c_0500];
        } else if (address >= 0x1c_0800 && address <= 0x1c_0801) {
            if (address % 2 == 0) {
                result = (byte) (ssriders_protection_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) ssriders_protection_r();
            }
        } else if (address >= 0x5a_0000 && address <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053244_word_noA1_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053244_word_noA1_r(offset);
            }
        } else if (address >= 0x5c_0600 && address <= 0x5c_0603) {
            int offset = (address - 0x5c_0600) / 2;
            if (address % 2 == 0) {
                result = (byte) (punkshot_sound_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) punkshot_sound_r(offset);
            }
        } else if (address >= 0x60_0000 && address <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_ssriders(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x0b_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            result = (short) (Memory.mainram[address - 0x10_4000] * 0x100 + Memory.mainram[address - 0x10_4000 + 1]);
        }
        return result;
    }

    public static short MReadWord_ssriders(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x0b_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            result = (short) (Memory.mainram[address - 0x10_4000] * 0x100 + Memory.mainram[address - 0x10_4000 + 1]);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x18_0000 && address + 1 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            result = K053245_scattered_word_r(offset);
        } else if (address >= 0x1c_0000 && address + 1 <= 0x1c_0001) {
            result = sbyte1;
        } else if (address >= 0x1c_0002 && address + 1 <= 0x1c_0003) {
            result = sbyte2;
        } else if (address >= 0x1c_0004 && address + 1 <= 0x1c_0005) {
            result = sbyte3;
        } else if (address >= 0x1c_0006 && address + 1 <= 0x1c_0007) {
            result = sbyte4;
        } else if (address >= 0x1c_0100 && address + 1 <= 0x1c_0101) {
            result = sbyte0;
        } else if (address >= 0x1c_0102 && address + 1 <= 0x1c_0103) {
            result = ssriders_eeprom_r();
        } else if (address >= 0x1c_0400 && address + 1 <= 0x1c_0401) {
            result = Generic.watchdog_reset16_r();
        } else if (address >= 0x1c_0500 && address + 1 <= 0x1c_057f) {
            result = (short) (mainram2[address - 0x1c_0500] * 0x100 + mainram2[address - 0x1c_0500 + 1]);
        } else if (address >= 0x1c_0800 && address + 1 <= 0x1c_0801) {
            result = ssriders_protection_r();
        } else if (address >= 0x5a_0000 && address + 1 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            result = K053244_word_noA1_r(offset);
        } else if (address >= 0x5c_0600 && address + 1 <= 0x5c_0603) {
            int offset = (address - 0x5c_0600) / 2;
            result = punkshot_sound_r(offset);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            result = K052109_word_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_ssriders(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x0b_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            result = Memory.mainram[address - 0x10_4000] * 0x100_0000 + Memory.mainram[address - 0x10_4000 + 1] * 0x1_0000 + Memory.mainram[address - 0x10_4000 + 2] * 0x100 + Memory.mainram[address - 0x10_4000 + 3];
        }
        return result;
    }

    public static int MReadLong_ssriders(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x0b_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            result = Memory.mainram[address - 0x10_4000] * 0x100_0000 + Memory.mainram[address - 0x10_4000 + 1] * 0x1_0000 + Memory.mainram[address - 0x10_4000 + 2] * 0x100 + Memory.mainram[address - 0x10_4000 + 3];
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x18_0000 && address + 3 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            result = K053245_scattered_word_r(offset) * 0x1_0000 + K053245_scattered_word_r(offset + 1);
        } else if (address >= 0x1c_0500 && address + 3 <= 0x1c_057f) {
            result = mainram2[address - 0x1c_0500] * 0x100_0000 + mainram2[address - 0x1c_0500 + 1] * 0x1_0000 + mainram2[address - 0x1c_0500 + 2] * 0x100 + mainram2[address - 0x1c_0500 + 3];
        } else if (address >= 0x5a_0000 && address + 3 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            result = K053244_word_noA1_r(offset) * 0x1_0000 + K053244_word_noA1_r(offset);
        } else if (address >= 0x5c_0600 && address + 3 <= 0x5c_0603) {
            int offset = (address - 0x5c_0600) / 2;
            result = punkshot_sound_r(offset) * 0x1_0000 + punkshot_sound_r(offset + 1);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            result = K052109_word_r(offset) * 0x1_0000 + K052109_word_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_ssriders(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x14_0000 && address <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x18_0000 && address <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            if (address % 2 == 0) {
                K053245_scattered_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K053245_scattered_word_w2(offset, value);
            }
        } else if (address >= 0x1c_0200 && address <= 0x1c_0201) {
            int offset = (address - 0x1c_0200) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                ssriders_eeprom_w2(value);
            }
        } else if (address >= 0x1c_0300 && address <= 0x1c_0301) {
            int offset = (address - 0x1c_0300) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                ssriders_1c0300_w2(offset, value);
            }
        } else if (address >= 0x1c_0400 && address <= 0x1c_0401) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x1c_0500 && address <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            mainram2[offset] = value;
        } else if (address >= 0x1c_0800 && address <= 0x1c_0803) {
            int offset = (address - 0x1c_0800) / 2;
            ssriders_protection_w(offset);
        } else if (address >= 0x5a_0000 && address <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            if (address % 2 == 0) {
                K053244_word_noA1_w1(offset, value);
            } else if (address % 2 == 1) {
                K053244_word_noA1_w2(offset, value);
            }
        } else if (address >= 0x5c_0600 && address <= 0x5c_0601) {
            int offset = (address - 0x5c_0600) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053260.k053260_0_lsb_w(offset, value);
            }
        } else if (address >= 0x5c_0604 && address <= 0x5c_0605) {
            ssriders_soundkludge_w();
        } else if (address >= 0x5c_0700 && address <= 0x5c_071f) {
            int offset = (address - 0x5c_0700) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053251_lsb_w2(offset, value);
            }
        } else if (address >= 0x60_0000 && address <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_ssriders(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x18_0000 && address + 1 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            K053245_scattered_word_w(offset, value);
        } else if (address >= 0x1c_0200 && address + 1 <= 0x1c_0201) {
            int offset = (address - 0x1c_0200) / 2;
            ssriders_eeprom_w(value);
        } else if (address >= 0x1c_0300 && address + 1 <= 0x1c_0301) {
            int offset = (address - 0x1c_0300) / 2;
            ssriders_1c0300_w(offset, value);
        } else if (address >= 0x1c_0400 && address + 1 <= 0x1c_0401) {
            int offset = (address - 0x1c_0400) / 2;
            Generic.watchdog_reset16_w();
        } else if (address >= 0x1c_0500 && address + 1 <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0x1c_0800 && address + 1 <= 0x1c_0803) {
            int offset = (address - 0x1c_0800) / 2;
            ssriders_protection_w(offset);
        } else if (address >= 0x5a_0000 && address + 1 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            K053244_word_noA1_w(offset, value);
        } else if (address >= 0x5c_0600 && address + 1 <= 0x5c_0601) {
            int offset = (address - 0x5c_0600) / 2;
            K053260.k053260_0_lsb_w(offset, value);
        } else if (address >= 0x5c_0604 && address + 1 <= 0x5c_0605) {
            ssriders_soundkludge_w();
        } else if (address >= 0x5c_0700 && address + 1 <= 0x5c_071f) {
            int offset = (address - 0x5c_0700) / 2;
            K053251_lsb_w(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            K052109_word_w(offset, value);
        }
    }

    public static void MWriteLong_ssriders(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x18_0000 && address + 3 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            K053245_scattered_word_w(offset, (short) (value >> 16));
            K053245_scattered_word_w(offset + 1, (short) value);
        } else if (address >= 0x1c_0500 && address + 3 <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0x1c_0800 && address + 3 <= 0x1c_0803) {
            int offset = (address - 0x1c_0800) / 2;
            //ssriders_protection_w(offset);
            ssriders_protection_w(offset + 1);
        } else if (address >= 0x5a_0000 && address + 3 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            K053260.k053260_0_lsb_w(offset, (short) (value >> 16));
            K053260.k053260_0_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x5c_0700 && address + 3 <= 0x5c_071f) {
            int offset = (address - 0x5c_0700) / 2;
            K053251_lsb_w(offset, (short) (value >> 16));
            K053251_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            K052109_word_w(offset, (short) (value >> 16));
            K052109_word_w(offset + 1, (short) value);
        }
    }

    public static byte ZReadOp_mia(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_mia(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            result = Memory.audioram[offset];
        } else if (address == 0xa000) {
            result = (byte) Sound.soundlatch_r();
        } else if (address >= 0xb000 && address <= 0xb00d) {
            int offset = address - 0xb000;
            result = K007232.k007232_read_port_0_r(offset);
        } else if (address == 0xc001) {
            result = YM2151.ym2151_status_port_0_r();
        }
        return result;
    }

    public static void ZWriteMemory_mia(short address, byte value) {
        if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            Memory.audioram[offset] = value;
        } else if (address >= 0xb000 && address <= 0xb00d) {
            int offset = address - 0xb000;
            K007232.k007232_write_port_0_w(offset, value);
        } else if (address == 0xc000) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xc001) {
            YM2151.ym2151_data_port_0_w(value);
        }
    }

    public static byte ZReadOp_tmnt(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_tmnt(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            result = Memory.audioram[offset];
        } else if (address == 0x9000) {
            result = tmnt_sres_r();
        } else if (address == 0xa000) {
            result = (byte) Sound.soundlatch_r();
        } else if (address >= 0xb000 && address <= 0xb00d) {
            int offset = address - 0xb000;
            result = K007232.k007232_read_port_0_r(offset);
        } else if (address == 0xc001) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address == 0xf000) {
            result = Upd7759.upd7759_0_busy_r();
        }
        return result;
    }

    public static void ZWriteMemory_tmnt(short address, byte value) {
        if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            Memory.audioram[offset] = value;
        } else if (address == 0x9000) {
            tmnt_sres_w(value);
        } else if (address >= 0xb000 && address <= 0xb00d) {
            int offset = address - 0xb000;
            K007232.k007232_write_port_0_w(offset, value);
        } else if (address == 0xc000) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xc001) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address == 0xd000) {
            Upd7759.upd7759_0_port_w(value);
        } else if (address == 0xe000) {
            Upd7759.upd7759_0_start_w(value);
        }
    }

    public static byte ZReadOp_punkshot(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_punkshot(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            result = Memory.audioram[offset];
        } else if (address == 0xf801) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address >= 0xfc00 && address <= 0xfc2f) {
            int offset = address - 0xfc00;
            result = K053260.k053260_0_r(offset);
        }
        return result;
    }

    public static void ZWriteMemory_punkshot(short address, byte value) {
        if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            Memory.audioram[offset] = value;
        } else if (address == 0xf800) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xf801) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address == 0xfa00) {
            sound_arm_nmi_w();
        } else if (address >= 0xfc00 && address <= 0xfc2f) {
            int offset = address - 0xfc00;
            K053260.k053260_0_w(offset, value);
        }
    }

    public static byte ZReadOp_lgtnfght(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_lgtnfght(short address) {
        byte result = 0;
        if (address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            result = Memory.audioram[offset];
        } else if (address == 0xa001) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address >= 0xc000 && address <= 0xc02f) {
            int offset = address - 0xc000;
            result = K053260.k053260_0_r(offset);
        }
        return result;
    }

    public static void ZWriteMemory_lgtnfght(short address, byte value) {
        if (address >= 0x8000 && address <= 0x87ff) {
            int offset = address - 0x8000;
            Memory.audioram[offset] = value;
        } else if (address == 0xa000) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xa001) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address >= 0xc000 && address <= 0xc02f) {
            int offset = address - 0xc000;
            K053260.k053260_0_w(offset, value);
        }
    }

    public static byte ZReadOp_ssriders(short address) {
        byte result = 0;
        if (address <= 0xefff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_ssriders(short address) {
        byte result = 0;
        if (address <= 0xefff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            result = Memory.audioram[offset];
        } else if (address == 0xf801) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address >= 0xfa00 && address <= 0xfa2f) {
            int offset = address - 0xfa00;
            result = K053260.k053260_0_r(offset);
        }
        return result;
    }

    public static void ZWriteMemory_ssriders(short address, byte value) {
        if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            Memory.audioram[offset] = value;
        } else if (address == 0xf800) {
            YM2151.ym2151_register_port_0_w(value);
        } else if (address == 0xf801) {
            YM2151.ym2151_data_port_0_w(value);
        } else if (address >= 0xfa00 && address <= 0xfa2f) {
            int offset = address - 0xfa00;
            K053260.k053260_0_w(offset, value);
        } else if (address == 0xfc00) {
            sound_arm_nmi_w();
        }
    }

    public static byte ZReadHardware(short address) {
        return 0;
    }

    public static void ZWriteHardware(short address, byte value) {

    }

    public static int ZIRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[0].cpunum, 0);
    }

//#region Memeory2

    public static byte MReadOpByte_blswhstl(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x20_4000 && address <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_blswhstl(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x18_0000 && address <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_r(offset);
            }
        } else if (address >= 0x20_4000 && address <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            result = Memory.mainram[offset];
        } else if (address >= 0x30_0000 && address <= 0x30_3fff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053245_scattered_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053245_scattered_word_r(offset);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_0fff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x50_0000 && address <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) K054000_lsb_r(offset);
            }
        } else if (address >= 0x68_0000 && address <= 0x68_001f) {
            int offset = (address - 0x68_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053244_word_noA1_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053244_word_noA1_r(offset);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x70_0002 && address <= 0x70_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x70_0004 && address <= 0x70_0005) {
            int offset = (address - 0x70_0004) / 2;
            if (address % 2 == 0) {
                result = (byte) (blswhstl_coin_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) blswhstl_coin_r();
            }
        } else if (address >= 0x70_0006 && address <= 0x70_0007) {
            int offset = (address - 0x70_0006) / 2;
            if (address % 2 == 0) {
                result = (byte) (blswhstl_eeprom_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) blswhstl_eeprom_r();
            }
        } else if (address >= 0x78_0600 && address <= 0x78_0603) {
            int offset = (address - 0x78_0600) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) blswhstl_sound_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_blswhstl(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x20_4000 && address + 1 <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_blswhstl(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x18_0000 && address + 1 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            result = K052109_word_r(offset);
        } else if (address >= 0x20_4000 && address + 1 <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x30_0000 && address + 1 <= 0x30_3fff) {
            int offset = (address - 0x30_0000) / 2;
            result = K053245_scattered_word_r(offset);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_0fff) {
            int offset = (address - 0x40_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            result = K054000_lsb_r(offset);
        } else if (address >= 0x68_0000 && address + 1 <= 0x68_001f) {
            int offset = (address - 0x68_0000) / 2;
            result = K053244_word_noA1_r(offset);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            int offset = (address - 0x70_0000) / 2;
            result = sbyte1;
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0003) {
            int offset = (address - 0x70_0002) / 2;
            result = sbyte2;
        } else if (address >= 0x70_0004 && address + 1 <= 0x70_0005) {
            int offset = (address - 0x70_0004) / 2;
            result = blswhstl_coin_r();
        } else if (address >= 0x70_0006 && address + 1 <= 0x70_0007) {
            int offset = (address - 0x70_0006) / 2;
            result = blswhstl_eeprom_r();
        } else if (address >= 0x78_0600 && address + 1 <= 0x78_0603) {
            int offset = (address - 0x78_0600) / 2;
            result = blswhstl_sound_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_blswhstl(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x20_4000 && address + 3 <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_blswhstl(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x18_0000 && address + 3 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            result = K052109_word_r(offset) * 0x1_0000 + K052109_word_r(offset + 1);
        } else if (address >= 0x20_4000 && address + 3 <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x30_0000 && address + 3 <= 0x30_3fff) {
            int offset = (address - 0x30_0000) / 2;
            result = K053245_scattered_word_r(offset) * 0x1_0000 + K053245_scattered_word_r(offset + 1);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_0fff) {
            int offset = (address - 0x40_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x50_0000 && address + 3 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            result = K054000_lsb_r(offset) * 0x1_0000 + K054000_lsb_r(offset + 1);
        } else if (address >= 0x68_0000 && address + 3 <= 0x68_001f) {
            int offset = (address - 0x68_0000) / 2;
            result = K053244_word_noA1_r(offset) * 0x1_0000 + K053244_word_noA1_r(offset + 1);
        } else if (address >= 0x78_0600 && address + 3 <= 0x78_0603) {
            int offset = (address - 0x78_0600) / 2;
            result = 0;
        } else if (address >= 0x78_0600 && address + 3 <= 0x78_0601) {
            int offset = (address - 0x78_0600) / 2;
            result = blswhstl_sound_r(offset) * 0x1_0000 + blswhstl_sound_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_blswhstl(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x18_0000 && address <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_w2(offset, value);
            }
        } else if (address >= 0x20_4000 && address <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x30_0000 && address <= 0x30_3fff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                K053245_scattered_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K053245_scattered_word_w2(offset, value);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_0fff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x50_0000 && address <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K054000_lsb_w2(offset, value);
            }
        } else if (address >= 0x68_0000 && address <= 0x68_001f) {
            int offset = (address - 0x68_0000) / 2;
            if (address % 2 == 0) {
                K053244_word_noA1_w1(offset, value);
            } else if (address % 2 == 1) {
                K053244_word_noA1_w2(offset, value);
            }
        } else if (address >= 0x70_0200 && address <= 0x70_0201) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                blswhstl_eeprom_w2(value);
            }
        } else if (address >= 0x70_0300 && address <= 0x70_0301) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                blswhstl_700300_w2(value);
            }
        } else if (address >= 0x70_0400 && address <= 0x70_0401) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x78_0600 && address <= 0x78_0601) {
            int offset = (address - 0x78_0600) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053260.k053260_0_lsb_w2(offset, value);
            }
        } else if (address >= 0x78_0604 && address <= 0x78_0605) {
            ssriders_soundkludge_w();
        } else if (address >= 0x78_0700 && address <= 0x78_071f) {
            int offset = (address - 0x78_0700) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053251_lsb_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_blswhstl(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x18_0000 && address + 1 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            K052109_word_w(offset, value);
        } else if (address >= 0x20_4000 && address + 1 <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x30_0000 && address + 1 <= 0x30_3fff) {
            int offset = (address - 0x30_0000) / 2;
            K053245_scattered_word_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_0fff) {
            int offset = (address - 0x40_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            K054000_lsb_w(offset, value);
        } else if (address >= 0x68_0000 && address + 1 <= 0x68_001f) {
            int offset = (address - 0x68_0000) / 2;
            K053244_word_noA1_w(offset, value);
        } else if (address >= 0x70_0200 && address + 1 <= 0x70_0201) {
            blswhstl_eeprom_w(value);
        } else if (address >= 0x70_0300 && address + 1 <= 0x70_0301) {
            blswhstl_700300_w(value);
        } else if (address >= 0x70_0400 && address + 1 <= 0x70_0401) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x78_0600 && address + 1 <= 0x78_0601) {
            int offset = (address - 0x78_0600) / 2;
            K053260.k053260_0_lsb_w(offset, value);
        } else if (address >= 0x78_0604 && address + 1 <= 0x78_0605) {
            ssriders_soundkludge_w();
        } else if (address >= 0x78_0700 && address + 1 <= 0x78_071f) {
            int offset = (address - 0x78_0700) / 2;
            K053251_lsb_w(offset, value);
        }
    }

    public static void MWriteLong_blswhstl(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x18_0000 && address + 3 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            K052109_word_w(offset, (short) (value >> 16));
            K052109_word_w(offset + 1, (short) value);
        } else if (address >= 0x20_4000 && address + 3 <= 0x20_7fff) {
            int offset = address - 0x20_4000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x30_0000 && address + 3 <= 0x30_3fff) {
            int offset = (address - 0x30_0000) / 2;
            K053245_scattered_word_w(offset, (short) (value >> 16));
            K053245_scattered_word_w(offset + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_0fff) {
            int offset = (address - 0x40_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x50_0000 && address + 3 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            K054000_lsb_w(offset, (short) (value >> 16));
            K054000_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x68_0000 && address + 3 <= 0x68_001f) {
            int offset = (address - 0x68_0000) / 2;
            K053244_word_noA1_w(offset, (short) (value >> 16));
            K053244_word_noA1_w(offset + 1, (short) value);
        } else if (address >= 0x78_0700 && address + 3 <= 0x78_071f) {
            int offset = (address - 0x78_0700) / 2;
            K053251_lsb_w(offset, (short) (value >> 16));
            K053251_lsb_w(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_glfgreat(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_glfgreat(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053245_scattered_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053245_scattered_word_r(offset);
            }
        } else if (address >= 0x10_8000 && address <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x10_c000 && address <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053936_0_linectrl[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053936_0_linectrl[offset];
            }
        } else if (address >= 0x11_4000 && address <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) K053244_lsb_r(offset);
            }
        } else if (address >= 0x12_0000 && address <= 0x12_0001) {
            if (address % 2 == 0) {
                result = sbyte2;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x12_0002 && address <= 0x12_0003) {
            if (address % 2 == 0) {
                result = sbyte4;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x12_0004 && address <= 0x12_0005) {
            if (address % 2 == 0) {
                result = dsw3;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x12_0006 && address <= 0x12_0007) {
            if (address % 2 == 0) {
                result = dsw2;
            } else if (address % 2 == 1) {
                result = dsw1;
            }
        } else if (address >= 0x12_1000 && address <= 0x12_1001) {
            int offset = (address - 0x12_1000) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) glfgreat_ball_r();
            }
        } else if (address >= 0x12_5000 && address <= 0x12_5003) {
            int offset = (address - 0x12_5000) / 2;
            if (address % 2 == 0) {
                result = glfgreat_sound_r1(offset);
            } else {
                result = (byte) 0;
            }
        } else if (address >= 0x20_0000 && address <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (glfgreat_rom_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) glfgreat_rom_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_glfgreat(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_glfgreat(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            result = K053245_scattered_word_r(offset);
        } else if (address >= 0x10_8000 && address + 1 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x10_c000 && address + 1 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            result = K053936_0_linectrl[offset];
        } else if (address >= 0x11_4000 && address + 1 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            result = K053244_lsb_r(offset);
        } else if (address >= 0x12_0000 && address + 1 <= 0x12_0001) {
            int offset = (address - 0x12_0000) / 2;
            result = (short) ((sbyte2 << 8) | sbyte1);
        } else if (address >= 0x12_0002 && address + 1 <= 0x12_0003) {
            int offset = (address - 0x12_0002) / 2;
            result = (short) ((sbyte4 << 8) | sbyte3);
        } else if (address >= 0x12_0004 && address + 1 <= 0x12_0005) {
            int offset = (address - 0x12_0004) / 2;
            result = (short) ((dsw3 << 8) | sbyte0);
        } else if (address >= 0x12_0006 && address + 1 <= 0x12_0007) {
            int offset = (address - 0x12_0006) / 2;
            result = (short) ((dsw2 << 8) | dsw1);
        } else if (address >= 0x12_1000 && address + 1 <= 0x12_1001) {
            int offset = (address - 0x12_1000) / 2;
            result = glfgreat_ball_r();
        } else if (address >= 0x12_5000 && address + 1 <= 0x12_5003) {
            int offset = (address - 0x12_5000) / 2;
            result = glfgreat_sound_r(offset);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = glfgreat_rom_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_glfgreat(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_glfgreat(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            result = K053245_scattered_word_r(offset) * 0x1_0000 + K053245_scattered_word_r(offset + 1);
        } else if (address >= 0x10_8000 && address + 3 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x10_c000 && address + 3 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            result = K053936_0_linectrl[offset] * 0x1_0000 + K053936_0_linectrl[offset + 1];
        } else if (address >= 0x11_4000 && address + 3 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            result = K053244_lsb_r(offset) * 0x1_0000 + K053244_lsb_r(offset + 1);
        } else if (address >= 0x12_5000 && address + 3 <= 0x12_5003) {
            int offset = (address - 0x12_5000) / 2;
            result = glfgreat_sound_r(offset) * 0x1_0000 + glfgreat_sound_r(offset + 1);
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = glfgreat_rom_r(offset) * 0x1_0000 + glfgreat_rom_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_glfgreat(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            if (address % 2 == 0) {
                K053245_scattered_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K053245_scattered_word_w2(offset, value);
            }
        } else if (address >= 0x10_8000 && address <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x10_c000 && address <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            if (address % 2 == 0) {
                K053936_0_linectrl[offset] = (short) ((value << 8) | (K053936_0_linectrl[offset] & 0xff));
            } else if (address % 2 == 1) {
                K053936_0_linectrl[offset] = (short) ((K053936_0_linectrl[offset] & 0xff00) | value);
            }
        } else if (address >= 0x11_0000 && address <= 0x11_001f) {
            int offset = (address - 0x11_0000) / 2;
            if (address % 2 == 0) {
                K053244_word_noA1_w1(offset, value);
            } else if (address % 2 == 1) {
                K053244_word_noA1_w2(offset, value);
            }
        } else if (address >= 0x11_4000 && address <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053244_lsb_w2(offset, value);
            }
        } else if (address >= 0x11_8000 && address <= 0x11_801f) {
            int offset = (address - 0x11_8000) / 2;
            if (address % 2 == 0) {
                K053936_0_ctrl[offset] = (short) ((value << 8) | (K053936_0_ctrl[offset] & 0xff));
            } else if (address % 2 == 1) {
                K053936_0_ctrl[offset] = (short) ((K053936_0_ctrl[offset] & 0xff00) | value);
            }
        } else if (address >= 0x11_c000 && address <= 0x11_c01f) {
            int offset = (address - 0x11_c000) / 2;
            if (address % 2 == 0) {
                K053251_msb_w1(offset, value);
            } else if (address % 2 == 1) {

            }
        } else if (address >= 0x12_2000 && address <= 0x12_2001) {
            int offset = (address - 0x12_2000) / 2;
            if (address % 2 == 0) {
                glfgreat_122000_w1(value);
            } else if (address % 2 == 1) {
                glfgreat_122000_w2(value);
            }
        } else if (address >= 0x12_4000 && address <= 0x12_4001) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x12_5000 && address <= 0x12_5003) {
            int offset = (address - 0x12_5000) / 2;
            if (address % 2 == 0) {
                glfgreat_sound_w1(offset, value);
            } else if (address % 2 == 1) {
                glfgreat_sound_w2(offset, value);
            }
        } else if (address >= 0x20_0000 && address <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_glfgreat(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            K053245_scattered_word_w(offset, value);
        } else if (address >= 0x10_8000 && address + 1 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x10_c000 && address + 1 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            K053936_0_linectrl[offset] = value;
        } else if (address >= 0x11_0000 && address + 1 <= 0x11_001f) {
            int offset = (address - 0x11_0000) / 2;
            K053244_word_noA1_w(offset, value);
        } else if (address >= 0x11_4000 && address + 1 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            K053244_lsb_w(offset, value);
        } else if (address >= 0x11_8000 && address + 1 <= 0x11_801f) {
            int offset = (address - 0x11_8000) / 2;
            K053936_0_ctrl[offset] = value;
        } else if (address >= 0x11_c000 && address + 1 <= 0x11_c01f) {
            int offset = (address - 0x11_c000) / 2;
            K053251_msb_w(offset, value);
        } else if (address >= 0x12_2000 && address + 1 <= 0x12_2001) {
            int offset = (address - 0x12_2000) / 2;
            glfgreat_122000_w(value);
        } else if (address >= 0x12_4000 && address + 1 <= 0x12_4001) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x12_5000 && address + 1 <= 0x12_5003) {
            int offset = (address - 0x12_5000) / 2;
            glfgreat_sound_w(offset, value);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            K052109_word_noA12_w(offset, value);
        }
    }

    public static void MWriteLong_glfgreat(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            K053245_scattered_word_w(offset, (short) (value >> 16));
            K053245_scattered_word_w(offset + 1, (short) value);
        } else if (address >= 0x10_8000 && address + 3 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x10_c000 && address + 3 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            K053936_0_linectrl[offset] = (short) (value >> 16);
            K053936_0_linectrl[offset + 1] = (short) value;
        } else if (address >= 0x11_0000 && address + 3 <= 0x11_001f) {
            int offset = (address - 0x11_0000) / 2;
            K053244_word_noA1_w(offset, (short) (value >> 16));
            K053244_word_noA1_w(offset + 1, (short) value);
        } else if (address >= 0x11_4000 && address + 3 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            K053244_lsb_w(offset, (short) (value >> 16));
            K053244_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x11_8000 && address + 3 <= 0x11_801f) {
            int offset = (address - 0x11_8000) / 2;
            K053936_0_ctrl[offset] = (short) (value >> 16);
            K053936_0_ctrl[offset + 1] = (short) value;
        } else if (address >= 0x11_c000 && address + 3 <= 0x11_c01f) {
            int offset = (address - 0x11_c000) / 2;
            K053251_msb_w(offset, (short) (value >> 16));
            K053251_msb_w(offset + 1, (short) value);
        } else if (address >= 0x12_5000 && address + 3 <= 0x12_5003) {
            int offset = (address - 0x12_5000) / 2;
            glfgreat_sound_w(offset, (short) (value >> 16));
            glfgreat_sound_w(offset + 1, (short) value);
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_tmnt2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x0f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_tmnt2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x0f_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            result = Memory.mainram[offset];
        } else if (address >= 0x14_0000 && address <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x18_0000 && address <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.spriteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.spriteram16[offset];
            }
        } else if (address >= 0x1c_0000 && address <= 0x1c_0001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte1;
            }
        } else if (address >= 0x1c_0002 && address <= 0x1c_0003) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte2;
            }
        } else if (address >= 0x1c_0004 && address <= 0x1c_0005) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte3;
            }
        } else if (address >= 0x1c_0006 && address <= 0x1c_0007) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte4;
            }
        } else if (address >= 0x1c_0100 && address <= 0x1c_0101) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = sbyte0;
            }
        } else if (address >= 0x1c_0102 && address <= 0x1c_0103) {
            if (address % 2 == 0) {
                result = (byte) (ssriders_eeprom_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) ssriders_eeprom_r();
            }
        } else if (address >= 0x1c_0400 && address <= 0x1c_0401) {
            Generic.watchdog_reset16_r();
        } else if (address >= 0x1c_0500 && address <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            result = mainram2[offset];
        } else if (address >= 0x5a_0000 && address <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053244_word_noA1_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053244_word_noA1_r(offset);
            }
        } else if (address >= 0x5c_0600 && address <= 0x5c_0603) {
            int offset = (address - 0x5c_0600) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else {
                result = (byte) tmnt2_sound_r(offset);
            }
        } else if (address >= 0x60_0000 && address <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_r(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_tmnt2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x0f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_tmnt2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x0f_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x18_0000 && address + 1 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            result = Generic.spriteram16[offset];
        } else if (address >= 0x1c_0000 && address + 1 <= 0x1c_0001) {
            int offset = (address - 0x1c_0000) / 2;
            result = sbyte1;
        } else if (address >= 0x1c_0002 && address + 1 <= 0x1c_0003) {
            int offset = (address - 0x1c_0002) / 2;
            result = sbyte2;
        } else if (address >= 0x1c_0004 && address + 1 <= 0x1c_0005) {
            int offset = (address - 0x1c_0004) / 2;
            result = sbyte3;
        } else if (address >= 0x1c_0006 && address + 1 <= 0x1c_0007) {
            int offset = (address - 0x1c_0006) / 2;
            result = sbyte4;
        } else if (address >= 0x1c_0100 && address + 1 <= 0x1c_0101) {
            int offset = (address - 0x1c_0100) / 2;
            result = sbyte0;
        } else if (address >= 0x1c_0102 && address + 1 <= 0x1c_0103) {
            int offset = (address - 0x1c_0102) / 2;
            result = ssriders_eeprom_r();
        } else if (address >= 0x1c_0400 && address + 1 <= 0x1c_0401) {
            Generic.watchdog_reset16_r();
        } else if (address >= 0x1c_0500 && address + 1 <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            result = (short) (mainram2[offset] * 0x100 + mainram2[offset + 1]);
        } else if (address >= 0x5a_0000 && address + 1 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            result = K053244_word_noA1_r(offset);
        } else if (address >= 0x5c_0600 && address + 1 <= 0x5c_0603) {
            int offset = (address - 0x5c_0600) / 2;
            result = tmnt2_sound_r(offset);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            result = K052109_word_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_tmnt2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x0f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_tmnt2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x0f_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x18_0000 && address + 3 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            result = Generic.spriteram16[offset] * 0x1_0000 + Generic.spriteram16[offset + 1];
        } else if (address >= 0x1c_0500 && address + 3 <= 0x1c_057f) {
            int offset = (address - 0x1c_0500) / 2;
            result = mainram2[address - 0x1c_0500] * 0x100_0000 + mainram2[address - 0x1c_0500 + 1] * 0x1_0000 + mainram2[address - 0x1c_0500 + 2] * 0x100 + mainram2[address - 0x1c_0500 + 3];
        } else if (address >= 0x5a_0000 && address + 3 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            result = K053244_word_noA1_r(offset) * 0x1_0000 + K053244_word_noA1_r(offset + 1);
        } else if (address >= 0x5c_0600 && address + 3 <= 0x5c_0603) {
            int offset = (address - 0x5c_0600) / 2;
            result = tmnt2_sound_r(offset) * 0x1_0000 + tmnt2_sound_r(offset + 1);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            result = K052109_word_r(offset) * 0x1_0000 + K052109_word_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_tmnt2(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x14_0000 && address <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x18_0000 && address <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            if (address % 2 == 0) {
                K053245_scattered_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K053245_scattered_word_w2(offset, value);
            }
        } else if (address >= 0x1c_0200 && address <= 0x1c_0201) {
            int offset = (address - 0x1c_0200) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                ssriders_eeprom_w2(value);
            }
        } else if (address >= 0x1c_0300 && address <= 0x1c_0301) {
            int offset = (address - 0x1c_0300) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                ssriders_1c0300_w2(offset, value);
            }
        } else if (address >= 0x1c_0400 && address <= 0x1c_0401) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x1c_0500 && address <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            mainram2[offset] = value;
        } else if (address >= 0x1c_0800 && address <= 0x1c_081f) {
            int offset = (address - 0x1c_0800) / 2;
            if (address % 2 == 0) {
                tmnt2_1c0800_w1(offset, value);
            } else if (address % 2 == 1) {
                tmnt2_1c0800_w2(offset, value);
            }
        } else if (address >= 0x5a_0000 && address <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            if (address % 2 == 0) {
                K053244_word_noA1_w1(offset, value);
            } else if (address % 2 == 1) {
                K053244_word_noA1_w2(offset, value);
            }
        } else if (address >= 0x5c_0600 && address <= 0x5c_0601) {
            int offset = (address - 0x5c_0600) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053260.k053260_0_lsb_w(offset, value);
            }
        } else if (address >= 0x5c_0604 && address <= 0x5c_0605) {
            ssriders_soundkludge_w();
        } else if (address >= 0x5c_0700 && address <= 0x5c_071f) {
            int offset = (address - 0x5c_0700) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053251_lsb_w2(offset, value);
            }
        } else if (address >= 0x60_0000 && address <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_w2(offset, value);
            }
        }
    }

    public static void MWriteWord_tmnt2(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x14_0000 && address + 1 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x18_0000 && address + 1 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            K053245_scattered_word_w(offset, value);
        } else if (address >= 0x1c_0200 && address + 1 <= 0x1c_0201) {
            int offset = (address - 0x1c_0200) / 2;
            ssriders_eeprom_w(value);
        } else if (address >= 0x1c_0300 && address + 1 <= 0x1c_0301) {
            int offset = (address - 0x1c_0300) / 2;
            ssriders_1c0300_w(offset, value);
        } else if (address >= 0x1c_0400 && address + 1 <= 0x1c_0401) {
            Generic.watchdog_reset16_w();
        } else if (address >= 0x1c_0500 && address + 1 <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            mainram2[offset] = (byte) (value >> 8);
            mainram2[offset + 1] = (byte) value;
        } else if (address >= 0x1c_0800 && address + 1 <= 0x1c_081f) {
            int offset = (address - 0x1c_0800) / 2;
            tmnt2_1c0800_w(offset, value);
        } else if (address >= 0x5a_0000 && address + 1 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            K053244_word_noA1_w(offset, value);
        } else if (address >= 0x5c_0600 && address + 1 <= 0x5c_0601) {
            int offset = (address - 0x5c_0600) / 2;
            K053260.k053260_0_lsb_w(offset, value);
        } else if (address >= 0x5c_0604 && address + 1 <= 0x5c_0605) {
            ssriders_soundkludge_w();
        } else if (address >= 0x5c_0700 && address + 1 <= 0x5c_071f) {
            int offset = (address - 0x5c_0700) / 2;
            K053251_lsb_w(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            K052109_word_w(offset, value);
        }
    }

    public static void MWriteLong_tmnt2(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = address - 0x10_4000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x14_0000 && address + 3 <= 0x14_0fff) {
            int offset = (address - 0x14_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x18_0000 && address + 3 <= 0x18_3fff) {
            int offset = (address - 0x18_0000) / 2;
            K053245_scattered_word_w(offset, (short) (value >> 16));
            K053245_scattered_word_w(offset + 1, (short) value);
        } else if (address >= 0x1c_0500 && address + 3 <= 0x1c_057f) {
            int offset = address - 0x1c_0500;
            mainram2[offset] = (byte) (value >> 24);
            mainram2[offset + 1] = (byte) (value >> 16);
            mainram2[offset + 2] = (byte) (value >> 8);
            mainram2[offset + 3] = (byte) value;
        } else if (address >= 0x1c_0800 && address + 3 <= 0x1c_081f) {
            int offset = (address - 0x1c_0800) / 2;
            tmnt2_1c0800_w(offset, (short) (value >> 16));
            tmnt2_1c0800_w(offset + 1, (short) value);
        } else if (address >= 0x5a_0000 && address + 3 <= 0x5a_001f) {
            int offset = (address - 0x5a_0000) / 2;
            K053244_word_noA1_w(offset, (short) (value >> 16));
            K053244_word_noA1_w(offset + 1, (short) value);
        } else if (address >= 0x5c_0700 && address + 3 <= 0x5c_071f) {
            int offset = (address - 0x5c_0700) / 2;
            K053251_lsb_w(offset, (short) (value >> 16));
            K053251_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_3fff) {
            int offset = (address - 0x60_0000) / 2;
            K052109_word_w(offset, (short) (value >> 16));
            K052109_word_w(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_thndrx2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_thndrx2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x03_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x40_0000 && address <= 0x40_0003) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (punkshot_sound_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) punkshot_sound_r(offset);
            }
        } else if (address >= 0x50_0000 && address <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) K054000_lsb_r(offset);
            }
        } else if (address >= 0x50_0200 && address <= 0x50_0201) {
            if (address % 2 == 0) {
                result = (byte) (thndrx2_in0_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) thndrx2_in0_r();
            }
        } else if (address >= 0x50_0202 && address <= 0x50_0203) {
            if (address % 2 == 0) {
                result = (byte) (thndrx2_eeprom_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) thndrx2_eeprom_r();
            }
        } else if (address >= 0x60_0000 && address <= 0x60_7fff) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0007) {
            int offset = address - 0x70_0000;
            result = K051937_r(offset);
        } else if (address >= 0x70_0400 && address <= 0x70_07ff) {
            int offset = address - 0x70_0400;
            result = K051960_r(offset);
        }
        return result;
    }

    public static short MReadOpWord_thndrx2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_thndrx2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x03_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_0003) {
            int offset = (address - 0x40_0000) / 2;
            result = punkshot_sound_r(offset);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            result = K054000_lsb_r(offset);
        } else if (address >= 0x50_0200 && address + 1 <= 0x50_0201) {
            result = thndrx2_in0_r();
        } else if (address >= 0x50_0202 && address + 1 <= 0x50_0203) {
            result = thndrx2_eeprom_r();
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_7fff) {
            int offset = (address - 0x60_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0007) {
            int offset = address - 0x70_0000;
            result = (short) (K051937_r(offset) * 0x100 + K051937_r(offset + 1));
        } else if (address >= 0x70_0400 && address + 1 <= 0x70_07ff) {
            int offset = address - 0x70_0400;
            result = (short) (K051960_r(offset) * 0x100 + K051960_r(offset + 1));
        }
        return result;
    }

    public static int MReadOpLong_thndrx2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_thndrx2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x03_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_0003) {
            int offset = (address - 0x40_0000) / 2;
            result = punkshot_sound_r(offset) * 0x1_0000 + punkshot_sound_r(offset + 1);
        } else if (address >= 0x50_0000 && address + 3 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            result = K054000_lsb_r(offset) * 0x1_0000 + K054000_lsb_r(offset + 1);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_7fff) {
            int offset = (address - 0x60_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x70_0000 && address + 3 <= 0x70_0007) {
            int offset = address - 0x70_0000;
            result = (short) (K051937_r(offset) * 0x100_0000 + K051937_r(offset + 1) * 0x1_0000 + K051937_r(offset + 2) * 0x100 + K051937_r(offset + 3));
        } else if (address >= 0x70_0400 && address + 3 <= 0x70_07ff) {
            int offset = address - 0x70_0400;
            result = (short) (K051960_r(offset) * 0x100_0000 + K051960_r(offset + 1) * 0x1_0000 + K051960_r(offset + 2) * 0x100 + K051960_r(offset + 3));
        }
        return result;
    }

    public static void MWriteByte_thndrx2(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x30_0000 && address <= 0x30_001f) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053251_lsb_w2(offset, value);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_0001) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053260.k053260_0_lsb_w2(0, value);
            }
        } else if (address >= 0x50_0000 && address <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K054000_lsb_w2(offset, value);
            }
        } else if (address >= 0x50_0100 && address <= 0x50_0101) {
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                thndrx2_eeprom_w2(value);
            }
        } else if (address >= 0x50_0300 && address <= 0x50_0301) {
            int offset = (address - 0x50_0300) / 2;
            //NOP
        } else if (address >= 0x60_0000 && address <= 0x60_7fff) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0007) {
            int offset = address - 0x70_0000;
            K051937_w(offset, value);
        } else if (address >= 0x70_0400 && address <= 0x70_07ff) {
            int offset = address - 0x70_0400;
            K051960_w(offset, value);
        }
    }

    public static void MWriteWord_thndrx2(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x30_0000 && address + 1 <= 0x30_001f) {
            int offset = (address - 0x30_0000) / 2;
            K053251_lsb_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_0001) {
            K053260.k053260_0_lsb_w(0, value);
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            K054000_lsb_w(offset, value);
        } else if (address >= 0x50_0100 && address + 1 <= 0x50_0101) {
            int offset = (address - 0x50_0100) / 2;
            thndrx2_eeprom_w(value);
        } else if (address >= 0x50_0300 && address + 1 <= 0x50_0301) {
            int offset = (address - 0x50_0300) / 2;
            //NOP
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_7fff) {
            int offset = (address - 0x60_0000) / 2;
            K052109_word_noA12_w(offset, value);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0007) {
            int offset = address - 0x70_0000;
            K051937_w(offset, (byte) (value >> 8));
            K051937_w(offset + 1, (byte) value);
        } else if (address >= 0x70_0400 && address + 1 <= 0x70_07ff) {
            int offset = address - 0x70_0400;
            K051960_w(offset, (byte) (value >> 8));
            K051960_w(offset + 1, (byte) value);
        }
    }

    public static void MWriteLong_thndrx2(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x30_0000 && address + 3 <= 0x30_001f) {
            int offset = (address - 0x30_0000) / 2;
            K053251_lsb_w(offset, (short) (value >> 16));
            K053251_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x50_0000 && address + 3 <= 0x50_003f) {
            int offset = (address - 0x50_0000) / 2;
            K054000_lsb_w(offset, (short) (value >> 16));
            K054000_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x60_0000 && address + 3 <= 0x60_7fff) {
            int offset = (address - 0x60_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        } else if (address >= 0x70_0000 && address + 3 <= 0x70_0007) {
            int offset = address - 0x70_0000;
            K051937_w(offset, (byte) (value >> 24));
            K051937_w(offset + 1, (byte) (value >> 16));
            K051937_w(offset + 2, (byte) (value >> 8));
            K051937_w(offset + 3, (byte) value);
        } else if (address >= 0x70_0400 && address + 3 <= 0x70_07ff) {
            int offset = address - 0x70_0400;
            K051960_w(offset, (byte) (value >> 24));
            K051960_w(offset + 1, (byte) (value >> 16));
            K051960_w(offset + 2, (byte) (value >> 8));
            K051960_w(offset + 3, (byte) value);
        }
    }

    public static byte MReadOpByte_prmrsocr(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        }
        return result;
    }

    public static byte MReadByte_prmrsocr(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053245_scattered_word_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053245_scattered_word_r(offset);
            }
        } else if (address >= 0x10_8000 && address <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            if (address % 2 == 0) {
                result = (byte) (Generic.paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) Generic.paletteram16[offset];
            }
        } else if (address >= 0x10_c000 && address <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K053936_0_linectrl[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K053936_0_linectrl[offset];
            }
        } else if (address >= 0x11_4000 && address <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            if (address % 2 == 0) {
                result = (byte) 0;
            } else if (address % 2 == 1) {
                result = (byte) K053244_lsb_r(offset);
            }
        } else if (address >= 0x12_0000 && address <= 0x12_0001) {
            if (address % 2 == 0) {
                result = (byte) (prmrsocr_IN0_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) prmrsocr_IN0_r();
            }
        } else if (address >= 0x12_0002 && address <= 0x12_0003) {
            if (address % 2 == 0) {
                result = (byte) (prmrsocr_eeprom_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) prmrsocr_eeprom_r();
            }
        } else if (address >= 0x12_1014 && address <= 0x12_1015) {
            if (address % 2 == 0) {
                result = (byte) (prmrsocr_sound_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) prmrsocr_sound_r();
            }
        } else if (address >= 0x20_0000 && address <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (K052109_word_noA12_r(offset) >> 8);
            } else if (address % 2 == 1) {
                result = (byte) K052109_word_noA12_r(offset);
            }
        } else if (address >= 0x30_0000 && address <= 0x33_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = prmrsocr_rom_r1(offset);
            } else if (address % 2 == 1) {
                result = prmrsocr_rom_r2(offset);
            }
        }
        return result;
    }

    public static short MReadOpWord_prmrsocr(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        }
        return result;
    }

    public static short MReadWord_prmrsocr(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            result = K053245_scattered_word_r(offset);
        } else if (address >= 0x10_8000 && address + 1 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            result = Generic.paletteram16[offset];
        } else if (address >= 0x10_c000 && address + 1 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            result = K053936_0_linectrl[offset];
        } else if (address >= 0x11_4000 && address + 1 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            result = K053244_lsb_r(offset);
        } else if (address >= 0x12_0000 && address + 1 <= 0x12_0001) {
            int offset = (address - 0x12_0000) / 2;
            result = prmrsocr_IN0_r();
        } else if (address >= 0x12_0002 && address + 1 <= 0x12_0003) {
            int offset = (address - 0x12_0002) / 2;
            result = prmrsocr_eeprom_r();
        } else if (address >= 0x12_1014 && address + 1 <= 0x12_1015) {
            int offset = (address - 0x12_1014) / 2;
            result = prmrsocr_sound_r();
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            result = K052109_word_noA12_r(offset);
        } else if (address >= 0x30_0000 && address + 1 <= 0x33_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = prmrsocr_rom_r(offset);
        }
        return result;
    }

    public static int MReadOpLong_prmrsocr(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        }
        return result;
    }

    public static int MReadLong_prmrsocr(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            result = K053245_scattered_word_r(offset) * 0x1_0000 + K053245_scattered_word_r(offset + 1);
        } else if (address >= 0x10_8000 && address + 3 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            result = Generic.paletteram16[offset] * 0x1_0000 + Generic.paletteram16[offset + 1];
        } else if (address >= 0x10_c000 && address + 3 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            result = K053936_0_linectrl[offset] * 0x1_0000 + K053936_0_linectrl[offset + 1];
        } else if (address >= 0x11_4000 && address + 3 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            result = K053244_lsb_r(offset) * 0x1_0000 + K053244_lsb_r(offset + 1);
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            result = K052109_word_noA12_r(offset) * 0x1_0000 + K052109_word_noA12_r(offset + 1);
        } else if (address >= 0x30_0000 && address + 3 <= 0x33_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = prmrsocr_rom_r(offset) * 0x1_0000 + prmrsocr_rom_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_prmrsocr(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x10_4000 && address <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            if (address % 2 == 0) {
                K053245_scattered_word_w1(offset, value);
            } else if (address % 2 == 1) {
                K053245_scattered_word_w2(offset, value);
            }
        } else if (address >= 0x10_8000 && address <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            if (address % 2 == 0) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w1(offset, value);
            } else if (address % 2 == 1) {
                Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w2(offset, value);
            }
        } else if (address >= 0x10_c000 && address <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            if (address % 2 == 0) {
                K053936_0_linectrl[offset] = (short) ((value << 8) | (K053936_0_linectrl[offset] & 0xff));
            } else if (address % 2 == 1) {
                K053936_0_linectrl[offset] = (short) ((K053936_0_linectrl[offset] & 0xff00) | value);
            }
        } else if (address >= 0x11_0000 && address <= 0x11_001f) {
            int offset = (address - 0x11_0000) / 2;
            if (address % 2 == 0) {
                K053244_word_noA1_w1(offset, value);
            } else if (address % 2 == 1) {
                K053244_word_noA1_w2(offset, value);
            }
        } else if (address >= 0x11_4000 && address <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                K053244_lsb_w2(offset, value);
            }
        } else if (address >= 0x11_8000 && address <= 0x11_801f) {
            int offset = (address - 0x11_8000) / 2;
            if (address % 2 == 0) {
                K053936_0_ctrl[offset] = (short) ((value << 8) | (K053936_0_ctrl[offset] & 0xff));
            } else if (address % 2 == 1) {
                K053936_0_ctrl[offset] = (short) ((K053936_0_ctrl[offset] & 0xff00) | value);
            }
        } else if (address >= 0x11_c000 && address <= 0x11_c01f) {
            int offset = (address - 0x11_c000) / 2;
            if (address % 2 == 0) {
                K053251_msb_w1(offset, value);
            } else if (address % 2 == 1) {

            }
        } else if (address >= 0x12_100c && address <= 0x12_100f) {
            int offset = (address - 0x12_100c) / 2;
            if (address % 2 == 0) {

            } else if (address % 2 == 1) {
                prmrsocr_sound_cmd_w2(offset, value);
            }
        } else if (address >= 0x12_2000 && address <= 0x12_2001) {
            if (address % 2 == 0) {
                prmrsocr_eeprom_w1(value);
            } else if (address % 2 == 1) {
                prmrsocr_eeprom_w2(value);
            }
        } else if (address >= 0x12_3000 && address <= 0x12_3001) {
            prmrsocr_sound_irq_w();
        } else if (address >= 0x20_0000 && address <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                K052109_word_noA12_w1(offset, value);
            } else if (address % 2 == 1) {
                K052109_word_noA12_w2(offset, value);
            }
        } else if (address >= 0x28_0000 && address <= 0x28_0001) {
            Generic.watchdog_reset16_w();
        }
    }

    public static void MWriteWord_prmrsocr(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x10_4000 && address + 1 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            K053245_scattered_word_w(offset, value);
        } else if (address >= 0x10_8000 && address + 1 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, value);
        } else if (address >= 0x10_c000 && address + 1 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            K053936_0_linectrl[offset] = value;
        } else if (address >= 0x11_0000 && address + 1 <= 0x11_001f) {
            int offset = (address - 0x11_0000) / 2;
            K053244_word_noA1_w(offset, value);
        } else if (address >= 0x11_4000 && address + 1 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            K053244_lsb_w(offset, value);
        } else if (address >= 0x11_8000 && address + 1 <= 0x11_801f) {
            int offset = (address - 0x11_8000) / 2;
            K053936_0_ctrl[offset] = value;
        } else if (address >= 0x11_c000 && address + 1 <= 0x11_c01f) {
            int offset = (address - 0x11_c000) / 2;
            K053251_msb_w(offset, value);
        } else if (address >= 0x12_100c && address + 1 <= 0x12_100f) {
            int offset = (address - 0x12_100c) / 2;
            prmrsocr_sound_cmd_w(offset, value);
        } else if (address >= 0x12_2000 && address + 1 <= 0x12_2001) {
            prmrsocr_eeprom_w(value);
        } else if (address >= 0x12_3000 && address + 1 <= 0x12_3001) {
            prmrsocr_sound_irq_w();
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            K052109_word_noA12_w(offset, value);
        } else if (address >= 0x28_0000 && address + 1 <= 0x28_0001) {
            Generic.watchdog_reset16_w();
        }
    }

    public static void MWriteLong_prmrsocr(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x10_4000 && address + 3 <= 0x10_7fff) {
            int offset = (address - 0x10_4000) / 2;
            K053245_scattered_word_w(offset, (short) (value >> 16));
            K053245_scattered_word_w(offset + 1, (short) value);
        } else if (address >= 0x10_8000 && address + 3 <= 0x10_8fff) {
            int offset = (address - 0x10_8000) / 2;
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset, (short) (value >> 16));
            Generic.paletteram16_xBBBBBGGGGGRRRRR_word_w(offset + 1, (short) value);
        } else if (address >= 0x10_c000 && address + 3 <= 0x10_cfff) {
            int offset = (address - 0x10_c000) / 2;
            K053936_0_linectrl[offset] = (short) (value >> 16);
            K053936_0_linectrl[offset + 1] = (short) value;
        } else if (address >= 0x11_0000 && address + 3 <= 0x11_001f) {
            int offset = (address - 0x11_0000) / 2;
            K053244_word_noA1_w(offset, (short) (value >> 16));
            K053244_word_noA1_w(offset + 1, (short) value);
        } else if (address >= 0x11_4000 && address + 3 <= 0x11_401f) {
            int offset = (address - 0x11_4000) / 2;
            K053244_lsb_w(offset, (short) (value >> 16));
            K053244_lsb_w(offset + 1, (short) value);
        } else if (address >= 0x11_8000 && address + 3 <= 0x11_801f) {
            int offset = (address - 0x11_8000) / 2;
            K053936_0_ctrl[offset] = (short) (value >> 16);
            K053936_0_ctrl[offset + 1] = (short) value;
        } else if (address >= 0x11_c000 && address + 3 <= 0x11_c01f) {
            int offset = (address - 0x11_c000) / 2;
            K053251_msb_w(offset, (short) (value >> 16));
            K053251_msb_w(offset + 1, (short) value);
        } else if (address >= 0x12_100c && address + 3 <= 0x12_100f) {
            int offset = (address - 0x12_100c) / 2;
            prmrsocr_sound_cmd_w(offset, (short) (value >> 16));
            prmrsocr_sound_cmd_w(offset + 1, (short) value);
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_7fff) {
            int offset = (address - 0x20_0000) / 2;
            K052109_word_noA12_w(offset, (short) (value >> 16));
            K052109_word_noA12_w(offset + 1, (short) value);
        }
    }

    public static byte ZReadOp_glfgreat(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0xefff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xf000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_glfgreat(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0xefff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xf000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xf82f) {
            int offset = (address & 0xffff) - 0xf800;
            result = K053260.k053260_0_r(offset);
        }
        return result;
    }

    public static void ZWriteMemory_glfgreat(short address, byte value) {
        if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            int offset = address - 0xf000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xf82f) {
            int offset = address - 0xf800;
            K053260.k053260_0_w(offset, value);
        } else if ((address & 0xffff) == 0xfa00) {
            sound_arm_nmi_w();
        }
    }

    public static byte ZReadOp_thndrx2(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0xefff) {
            result = Memory.audiorom[address];
        } else if (address >= 0xf000 && address <= 0xf7ff) {
            int offset = address - 0xf000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_thndrx2(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0xefff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xf000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) == 0xf801 || (address & 0xffff) == 0xf811) {
            result = YM2151.ym2151_status_port_0_r();
        } else if ((address & 0xffff) >= 0xfc00 && (address & 0xffff) <= 0xfc2f) {
            int offset = address - 0xfc00;
            result = K053260.k053260_0_r(offset);
        }
        return result;
    }

    public static void ZWriteMemory_thndrx2(short address, byte value) {
        if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            int offset = (address & 0xffff) - 0xf000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) == 0xf800 || (address & 0xffff) == 0xf810) {
            YM2151.ym2151_register_port_0_w(value);
        } else if ((address & 0xffff) == 0xf801 || (address & 0xffff) == 0xf811) {
            YM2151.ym2151_data_port_0_w(value);
        } else if ((address & 0xffff) == 0xfa00) {
            sound_arm_nmi_w();
        } else if ((address & 0xffff) >= 0xfc00 && (address & 0xffff) <= 0xfc2f) {
            int offset = address - 0xfc00;
            K053260.k053260_0_w(offset, value);
        }
    }

    public static byte ZReadOp_prmrsocr(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.audiorom[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xc000;
            result = Memory.audioram[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte ZReadMemory_prmrsocr(short address) {
        byte result = 0;
        if ((address & 0xffff) <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = (address & 0xffff) - 0x8000;
            result = Memory.audiorom[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xc000;
            result = Memory.audioram[offset];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xe0ff) {
            int offset = (address & 0xffff) - 0xe000;
            result = K054539.k054539_0_r(offset);
        } else if ((address & 0xffff) >= 0xe100 && (address & 0xffff) <= 0xe12f) {
            int offset = (address & 0xffff) - 0xe100;
            result = k054539_0_ctrl_r(offset);
        } else if ((address & 0xffff) == 0xf002) {
            result = (byte) Sound.soundlatch_r();
        } else if ((address & 0xffff) == 0xf003) {
            result = (byte) Sound.soundlatch2_r();
        }
        return result;
    }

    public static void ZWriteMemory_prmrsocr(short address, byte value) {
        if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            int offset = address - 0x8000;
            Memory.audiorom[basebanksnd + offset] = value;
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdfff) {
            int offset = (address & 0xffff) - 0xc000;
            Memory.audioram[offset] = value;
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xe0ff) {
            int offset = (address & 0xffff) - 0xe000;
            K054539.k054539_0_w(offset, value);
        } else if ((address & 0xffff) >= 0xe100 && (address & 0xffff) <= 0xe12f) {
            int offset = (address & 0xffff) - 0xe100;
            k054539_0_ctrl_w(offset, value);
        } else if ((address & 0xffff) == 0xf000) {
            Sound.soundlatch3_w(value);
        } else if ((address & 0xffff) == 0xf800) {
            prmrsocr_audio_bankswitch_w(value);
        }
    }

//#endregin

//#endregion

//#region State

    public static void SaveStateBinary_cuebrick(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(dsw3);
        writer.write(cuebrick_snd_irqlatch);
        writer.write(cuebrick_nvram_bank);
        for (i = 0; i < 0x8000; i++) {
            writer.write(cuebrick_nvram[i]);
        }
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K051960(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        writer.write(mainram2, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_cuebrick(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            dsw3 = reader.readByte();
            cuebrick_snd_irqlatch = reader.readInt32();
            cuebrick_nvram_bank = reader.readInt32();
            for (i = 0; i < 0x8000; i++) {
                cuebrick_nvram[i] = reader.readUInt16();
            }
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K051960(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            mainram2 = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_mia(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(dsw3);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K051960(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        writer.write(mainram2, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K007232.SaveStateBinary(writer);
        for (i = 0; i < 1; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 1; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k007232stream.output_sampindex);
        writer.write(Sound.k007232stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_mia(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            dsw3 = reader.readByte();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K051960(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            mainram2 = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K007232.LoadStateBinary(reader);
            for (i = 0; i < 1; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 1; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k007232stream.output_sampindex = reader.readInt32();
            Sound.k007232stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_tmnt(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(dsw3);
        writer.write(tmnt_soundlatch);
        for (i = 0; i < 0x4_0000; i++) {
            writer.write(sampledata[i]);
        }
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K051960(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K007232.SaveStateBinary(writer);
        Upd7759.SaveStateBinary(writer);
        Sample.SaveStateBinary(writer);
        for (i = 0; i < 1; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 1; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k007232stream.output_sampindex);
        writer.write(Sound.k007232stream.output_base_sampindex);
        writer.write(Sound.upd7759stream.output_sampindex);
        writer.write(Sound.upd7759stream.output_base_sampindex);
        writer.write(Sound.samplestream.output_sampindex);
        writer.write(Sound.samplestream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_tmnt(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            dsw3 = reader.readByte();
            tmnt_soundlatch = reader.readInt32();
            for (i = 0; i < 0x4_0000; i++) {
                sampledata[i] = reader.readInt16();
            }
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K051960(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K007232.LoadStateBinary(reader);
            Upd7759.LoadStateBinary(reader);
            Sample.LoadStateBinary(reader);
            for (i = 0; i < 1; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 1; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k007232stream.output_sampindex = reader.readInt32();
            Sound.k007232stream.output_base_sampindex = reader.readInt32();
            Sound.upd7759stream.output_sampindex = reader.readInt32();
            Sound.upd7759stream.output_base_sampindex = reader.readInt32();
            Sound.samplestream.output_sampindex = reader.readInt32();
            Sound.samplestream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_punkshot(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(dsw3);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K051960(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_punkshot(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            dsw3 = reader.readByte();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K051960(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_lgtnfght(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(dsw3);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K053245(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_lgtnfght(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            dsw3 = reader.readByte();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K053245(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_blswhstl(BinaryWriter writer) {
        int i;
        writer.write(bytee);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K053245(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void LoadStateBinary_blswhstl(BinaryReader reader) {
        try {
            int i;
            bytee = reader.readByte();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K053245(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_glfgreat(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(dsw3);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K053245(writer);
        SaveStateBinary_K053936(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_glfgreat(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            dsw3 = reader.readByte();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K053245(reader);
            LoadStateBinary_K053936(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_tmnt2(BinaryWriter writer) {
        int i;
        for (i = 0; i < 0x10; i++) {
            writer.write(tmnt2_1c0800[i]);
        }
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K053245(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        writer.write(mainram2, 0, 0x80);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void LoadStateBinary_tmnt2(BinaryReader reader) {
        try {
            int i;
            for (i = 0; i < 0x10; i++) {
                tmnt2_1c0800[i] = reader.readUInt16();
            }
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K053245(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            mainram2 = reader.readBytes(0x80);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_ssriders(BinaryWriter writer) {
        int i;
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K053245(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        writer.write(mainram2, 0, 0x80);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void LoadStateBinary_ssriders(BinaryReader reader) {
        try {
            int i;
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K053245(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            mainram2 = reader.readBytes(0x80);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_thndrx2(BinaryWriter writer) {
        int i;
        writer.write(bytee);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K051960(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        K053260.SaveStateBinary(writer);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.k053260stream.output_sampindex);
        writer.write(Sound.k053260stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void LoadStateBinary_thndrx2(BinaryReader reader) {
        try {
            int i;
            bytee = reader.readByte();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K051960(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            K053260.LoadStateBinary(reader);
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.k053260stream.output_sampindex = reader.readInt32();
            Sound.k053260stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static void SaveStateBinary_prmrsocr(BinaryWriter writer) {
        int i;
        writer.write(basebanksnd);
        writer.write(init_eeprom_count);
        writer.write(toggle);
        writer.write(dim_c);
        writer.write(dim_v);
        writer.write(lastdim);
        writer.write(lasten);
        writer.write(sprite_colorbase);
        writer.write(bg_colorbase);
        for (i = 0; i < 3; i++) {
            writer.write(layer_colorbase[i]);
        }
        SaveStateBinary_K053251(writer);
        SaveStateBinary_K052109(writer);
        SaveStateBinary_K053245(writer);
        SaveStateBinary_K053936(writer);
        for (i = 0; i < 0x800; i++) {
            writer.write(Generic.paletteram16[i]);
        }
        for (i = 0; i < 0x2000; i++) {
            writer.write(Generic.spriteram16[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x4000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x2000);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        K054539.SaveStateBinary(writer);
        for (i = 0; i < 3; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 3; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(Sound.k054539stream.output_sampindex);
        writer.write(Sound.k054539stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Eeprom.SaveStateBinary(writer);
    }

    public static void LoadStateBinary_prmrsocr(BinaryReader reader) {
        try {
            int i;
            basebanksnd = reader.readInt32();
            init_eeprom_count = reader.readInt32();
            toggle = reader.readInt32();
            dim_c = reader.readInt32();
            dim_v = reader.readInt32();
            lastdim = reader.readInt32();
            lasten = reader.readInt32();
            sprite_colorbase = reader.readInt32();
            bg_colorbase = reader.readInt32();
            for (i = 0; i < 3; i++) {
                layer_colorbase[i] = reader.readInt32();
            }
            LoadStateBinary_K053251(reader);
            LoadStateBinary_K052109(reader);
            LoadStateBinary_K053245(reader);
            LoadStateBinary_K053936(reader);
            for (i = 0; i < 0x800; i++) {
                Generic.paletteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x2000; i++) {
                Generic.spriteram16[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x4000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x2000);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            K054539.LoadStateBinary(reader);
            for (i = 0; i < 3; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 3; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            Sound.k054539stream.output_sampindex = reader.readInt32();
            Sound.k054539stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Eeprom.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Input

    public static void loop_inputports_konami68000_common() {
    }

    public static void loop_inputports_konami68000_cuebrick() {
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
            sbyte0 &= ~0x08;
        } else {
            sbyte0 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            sbyte1 &= ~0x40;
        } else {
            sbyte1 |= 0x40;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            //sbyte0 &= ~0x20;
        } else {
            //sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_tmnt() {
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            //sbyte1 &= ~0x04;
        } else {
            //sbyte1 |= 0x04;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            //sbyte1 &= ~0x40;
        } else {
            //sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x20;
        } else {
            sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_blswhstl() {
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            //sbyte1 &= ~0x04;
        } else {
            //sbyte1 |= 0x04;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            //sbyte1 &= ~0x40;
        } else {
            //sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            //sbyte0 &= ~0x20;
        } else {
            //sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_glfgreat() {
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
            dsw3 &= (byte) ~0x01;
        } else {
            dsw3 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            dsw3 &= (byte) ~0x02;
        } else {
            dsw3 |= 0x02;
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            sbyte1 &= ~0x40;
        } else {
            sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            sbyte1 &= (byte) ~0x80;
        } else {
            sbyte1 |= (byte) 0x80;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            //sbyte0 &= ~0x20;
        } else {
            //sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_qgakumon() {
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
            sbyte1 &= (byte) ~0x80;
        } else {
            sbyte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_K)) {
//            sbyte1 &= ~0x20;
//        } else {
//            sbyte1 |= 0x20;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_L)) {
//            sbyte1 &= ~0x40;
//        } else {
//            sbyte1 |= 0x40;
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD2)) {
//            sbyte2 &= ~0x20;
//        } else {
//            sbyte2 |= 0x20;
//        }
//        if (Keyboard.isPressed(KeyEvent.VK_NUMPAD3)) {
//            sbyte2 &= ~0x40;
//        } else {
//            sbyte2 |= 0x40;
//        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
//            sbyte0 &= ~0x40;
        } else {
//            sbyte0 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
//            sbyte0 &= ~0x20;
        } else {
//            sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_ssriders() {
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
            sbyte1 &= (byte) ~0x80;
        } else {
            sbyte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            //sbyte1 &= ~0x04;
        } else {
            //sbyte1 |= 0x04;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            //sbyte1 &= ~0x40;
        } else {
            //sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbyte0 &= ~0x20;
        } else {
            sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_thndrx2() {
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
            sbyte1 &= (byte) ~0x80;
        } else {
            sbyte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if ((sbyte1 & 0x03) == 0) {
            sbyte1 |= 0x03;
        }
        if ((sbyte1 & 0x0c) == 0) {
            sbyte1 |= 0x0c;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            //sbyte1 &= ~0x04;
        } else {
            //sbyte1 |= 0x04;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if ((sbyte2 & 0x03) == 0) {
            sbyte2 |= 0x03;
        }
        if ((sbyte2 & 0x0c) == 0) {
            sbyte2 |= 0x0c;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            //sbyte1 &= ~0x40;
        } else {
            //sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            //sbyte0 &= ~0x20;
        } else {
            //sbyte0 |= 0x20;
        }
    }

    public static void loop_inputports_konami68000_prmrsocr() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            bytee &= (byte) ~0x04;
        } else {
            bytee |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte1 &= (byte) ~0x80;
        } else {
            sbyte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
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
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            sbyte1 &= ~0x04;
        } else {
            sbyte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            sbyte1 &= ~0x10;
        } else {
            sbyte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            //sbyte1 &= ~0x04;
        } else {
            //sbyte1 |= 0x04;
        }
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
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            //sbyte1 &= ~0x40;
        } else {
            //sbyte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            //sbyte0 &= ~0x20;
        } else {
            //sbyte0 |= 0x20;
        }
    }

    public static void record_port() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbyte3 != sbyte3_old || sbyte4 != sbyte4_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbyte3_old = sbyte3;
            sbyte4_old = sbyte4;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbyte3);
            Mame.bwRecord.write(sbyte4);
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
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbyte3 = sbyte3_old;
            sbyte4 = sbyte4_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void record_port_prmrsocr() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbyte3 != sbyte3_old || sbyte4 != sbyte4_old || bytee != bytee_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbyte3_old = sbyte3;
            sbyte4_old = sbyte4;
            bytee_old = bytee;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbyte3);
            Mame.bwRecord.write(sbyte4);
            Mame.bwRecord.write(bytee);
        }
    }

    public static void replay_port_prmrsocr() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
                sbyte3_old = Mame.brRecord.readSByte();
                sbyte4_old = Mame.brRecord.readSByte();
                bytee_old = Mame.brRecord.readByte();
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbyte3 = sbyte3_old;
            sbyte4 = sbyte4_old;
            bytee = bytee_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Konamiic

    @FunctionalInterface
    public interface PentaConsumer<T, U, V, W, X> {
        void accept(T var1, U var2, V var3, W var4, X var5);
    }

    @FunctionalInterface
    public interface HeptaConsumer<T, U, V, W, X, Y, Z> {
        void accept(T var1, U var2, V var3, W var4, X var5, Y var6, Z var7);
    }

    @FunctionalInterface
    public interface NonaConsumer<T, U, V, W, X, Y, Z, A, B> {
        void accept(T var1, U var2, V var3, W var4, X var5, Y var6, Z var7, A var8, B var9);
    }

    private static byte[] K052109_memory_region;
    public static int K052109_videoram_F_offset, K052109_videoram2_F_offset, K052109_colorram_F_offset, K052109_videoram_A_offset, K052109_videoram2_A_offset, K052109_colorram_A_offset, K052109_videoram_B_offset, K052109_videoram2_B_offset, K052109_colorram_B_offset;
    public static byte[] K052109_ram, K052109_charrombank, K052109_charrombank_2;
    public static LineState K052109_RMRD_line;
    public static byte K052109_romsubbank, K052109_scrollctrl, K052109_irq_enabled, has_extra_video_ram;
    public static int[] K052109_dx, K052109_dy;
    public static int K052109_tileflip_enable;

    public static byte[] K051960_memory_region;
    public static int K051960_romoffset, K051960_spriteflip, K051960_readroms;
    public static byte[] K051960_spriterombank, K051960_ram;
    public static int K051960_dx, K051960_dy;
    public static int K051960_irq_enabled, K051960_nmi_enabled;

    private static byte[][] K053245_memory_region;
    private static int K05324x_z_rejection;
    private static int[] K053244_rombank, K053245_ramsize, K053245_dx, K053245_dy;
    public static short[][] K053245_buffer;
    public static byte[][] K053245_ram, K053244_regs;
    public static byte[] K054000_ram;

    public static short[] K053936_0_ctrl, K053936_0_linectrl;
    public static short[] K053936_1_ctrl, K053936_1_linectrl;
    public static int[][] K053936_offset;
    public static int[] K053936_wraparound;

    public static byte[] K053251_ram;
    public static int[] K053251_palette_index;
    public static int K053251_tilemaps_set;

    public static int counter;
    public interface K052109_delegate extends NonaConsumer<Integer, Integer, Integer, Integer, Integer, Integer, /* out */ int[], /* out */ int[], /* out */ int[]> {}

    public static K052109_delegate K052109_callback;
    public interface K051960_delegate extends HeptaConsumer<Integer, Integer, Integer, Integer, /* out */ int[], /* out */ int[], /* out */ int[]> {}

    public static K051960_delegate K051960_callback;
    public interface K053245_delegate extends PentaConsumer<Integer, Integer, /* out */ int[], /* out */ int[], /* out */ int[]> {}

    public static K053245_delegate K053245_callback;

    public static void K052109_vh_start() {
        int i, j;
        K052109_RMRD_line = LineState.CLEAR_LINE;
        K052109_irq_enabled = 0;
        has_extra_video_ram = 0;
        K052109_tilemap = new Tmap[3];
        for (i = 0; i < 3; i++) {
            K052109_tilemap[i] = new Tmap();
            K052109_tilemap[i].tilewidth = 8;
            K052109_tilemap[i].tileheight = 8;
            K052109_tilemap[i].cols = 0x40;
            K052109_tilemap[i].rows = 0x20;
            K052109_tilemap[i].width = K052109_tilemap[i].cols * K052109_tilemap[i].tilewidth;
            K052109_tilemap[i].height = K052109_tilemap[i].rows * K052109_tilemap[i].tileheight;
            K052109_tilemap[i].enable = true;
            K052109_tilemap[i].all_tiles_dirty = true;
        }
        K052109_ram = new byte[0x6000];
        K052109_colorram_F_offset = 0x0000;
        K052109_colorram_A_offset = 0x0800;
        K052109_colorram_B_offset = 0x1000;
        K052109_videoram_F_offset = 0x2000;
        K052109_videoram_A_offset = 0x2800;
        K052109_videoram_B_offset = 0x3000;
        K052109_videoram2_F_offset = 0x4000;
        K052109_videoram2_A_offset = 0x4800;
        K052109_videoram2_B_offset = 0x5000;
//        tilemap_set_transparent_pen(K052109_tilemap[0],0);
//        tilemap_set_transparent_pen(K052109_tilemap[1],0);
//        tilemap_set_transparent_pen(K052109_tilemap[2],0);
        K052109_tilemap[0].scrollrows = 1;
        K052109_tilemap[0].scrollcols = 1;
        K052109_tilemap[1].scrollrows = 256;
        K052109_tilemap[2].scrollrows = 256;
        K052109_tilemap[1].scrollcols = 512;
        K052109_tilemap[2].scrollcols = 512;
        for (i = 0; i < 3; i++) {
            K052109_tilemap[i].rowscroll = new int[K052109_tilemap[i].scrollrows];
            K052109_tilemap[i].colscroll = new int[K052109_tilemap[1].scrollcols];
            K052109_tilemap[i].tilemap_draw_instance3 = K052109_tilemap[i]::tilemap_draw_instanceKonami68000;
            K052109_tilemap[i].pixmap = new short[0x100 * 0x200];
            K052109_tilemap[i].flagsmap = new byte[0x100][0x200];
            K052109_tilemap[i].tileflags = new byte[0x20][0x40];
            K052109_tilemap[i].pen_data = new byte[0x40];
            K052109_tilemap[i].pen_to_flags = new byte[1][16];
            K052109_tilemap[i].pen_to_flags[0][0] =0;
            for (j = 1; j < 16; j++) {
                K052109_tilemap[i].pen_to_flags[0][j] =0x10;
            }
            K052109_tilemap[i].total_elements = gfx12rom.length / 0x40;
        }
        K052109_tilemap[0].tile_update3 = K052109_tilemap[0]::tile_updateKonami68000_0;
        K052109_tilemap[1].tile_update3 = K052109_tilemap[1]::tile_updateKonami68000_1;
        K052109_tilemap[2].tile_update3 = K052109_tilemap[2]::tile_updateKonami68000_2;
        for (i = 0; i < 3; i++) {
            K052109_dx[i] = K052109_dy[i] = 0;
        }
    }

    public static byte K052109_r(int offset) {
        if (K052109_RMRD_line == LineState.CLEAR_LINE) {
            return K052109_ram[offset];
        } else {
            int code = (offset & 0x1fff) >> 5;
            int color = K052109_romsubbank;
            int[] code2 = new int[1], color2 = new int[1], flags2 = new int[1];
            int flags = 0;
            int priority = 0;
            int bank = K052109_charrombank[(color & 0x0c) >> 2] >> 2;
            int addr;
            bank |= (K052109_charrombank_2[(color & 0x0c) >> 2] >> 2);
            if (has_extra_video_ram != 0) {
                code |= color << 8;
            } else {
                K052109_callback.accept(0, bank, code, color, flags, priority, /* out */ code2, /* out */  color2, /* out */  flags2);
                code = code2[0];
                color = color2[0];
            }
            addr = (code << 5) + (offset & 0x1f);
            addr &= K052109_memory_region.length - 1;
            return K052109_memory_region[addr];
        }
    }

    public static void K052109_w(int offset, byte data) {
        int row, col;
        if (offset == 0x90d) {
            int i1 = 1;
        }
        if (offset == 0x290d) {
            int i1 = 1;
        }
        if ((offset & 0x1fff) < 0x1800) {
            if (offset >= 0x4000) {
                has_extra_video_ram = 1;
            }
            K052109_ram[offset] = data;
            row = (offset & 0x7ff) / 0x40;
            col = (offset & 0x7ff) % 0x40;
            K052109_tilemap[(offset & 0x1800) >> 11].tilemap_mark_tile_dirty(row, col);
            //tilemap_mark_tile_dirty(K052109_tilemap[(offset & 0x1800) >> 11], offset & 0x7ff);
        } else {
            K052109_ram[offset] = data;
            if (offset >= 0x180c && offset < 0x1834) {

            } else if (offset >= 0x1a00 && offset < 0x1c00) {

            } else if (offset == 0x1c80) {
                if (K052109_scrollctrl != data) {
                    K052109_scrollctrl = data;
                }
            } else if (offset == 0x1d00) {
                K052109_irq_enabled = (byte) (data & 0x04);
            } else if (offset == 0x1d80) {
                int dirty = 0;
                if (K052109_charrombank[0] != (data & 0x0f)) dirty |= 1;
                if (K052109_charrombank[1] != ((data >> 4) & 0x0f)) dirty |= 2;
                if (dirty != 0) {
                    int i;
                    K052109_charrombank[0] = (byte) (data & 0x0f);
                    K052109_charrombank[1] = (byte) ((data >> 4) & 0x0f);
                    for (i = 0; i < 0x1800; i++) {
                        int bank = (K052109_ram[i] & 0x0c) >> 2;
                        if ((bank == 0 && ((dirty & 1) != 0) || (bank == 1 && ((dirty & 2) != 0)))) {
                            row = (i & 0x7ff) / 0x40;
                            col = (i & 0x7ff) % 0x40;
                            K052109_tilemap[(i & 0x1800) >> 11].tilemap_mark_tile_dirty(row, col);
//                            tilemap_mark_tile_dirty(K052109_tilemap[(i & 0x1800) >> 11], i & 0x7ff);
                        }
                    }
                }
            } else if (offset == 0x1e00 || offset == 0x3e00) {
                K052109_romsubbank = data;
            } else if (offset == 0x1e80) {
//                tilemap_set_flip(K052109_tilemap[0], (data & 1) ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
//                tilemap_set_flip(K052109_tilemap[1], (data & 1) ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
//                tilemap_set_flip(K052109_tilemap[2], (data & 1) ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
                if (K052109_tileflip_enable != ((data & 0x06) >> 1)) {
                    K052109_tileflip_enable = ((data & 0x06) >> 1);
                    K052109_tilemap[0].all_tiles_dirty = true;
                    K052109_tilemap[1].all_tiles_dirty = true;
                    K052109_tilemap[2].all_tiles_dirty = true;
                }
            } else if (offset == 0x1f00) {
                int dirty = 0;
                if (K052109_charrombank[2] != (data & 0x0f)) dirty |= 1;
                if (K052109_charrombank[3] != ((data >> 4) & 0x0f)) dirty |= 2;
                if (dirty != 0) {
                    int i;
                    K052109_charrombank[2] = (byte) (data & 0x0f);
                    K052109_charrombank[3] = (byte) ((data >> 4) & 0x0f);
                    for (i = 0; i < 0x1800; i++) {
                        int bank = (K052109_ram[i] & 0x0c) >> 2;
                        if ((bank == 2 && ((dirty & 1) != 0)) || (bank == 3 && ((dirty & 2) != 0))) {
                            row = (i & 0x7ff) / 0x40;
                            col = (i & 0x7ff) % 0x40;
                            K052109_tilemap[(i & 0x1800) >> 11].tilemap_mark_tile_dirty(row, col);
//                            tilemap_mark_tile_dirty(K052109_tilemap[(i & 0x1800) >> 11], i & 0x7ff);
                        }
                    }
                }
            } else if (offset >= 0x380c && offset < 0x3834) {

            } else if (offset >= 0x3a00 && offset < 0x3c00) {

            } else if (offset == 0x3d80) {
                K052109_charrombank_2[0] = (byte) (data & 0x0f);
                K052109_charrombank_2[1] = (byte) ((data >> 4) & 0x0f);
            } else if (offset == 0x3f00) {
                K052109_charrombank_2[2] = (byte) (data & 0x0f);
                K052109_charrombank_2[3] = (byte) ((data >> 4) & 0x0f);
            }
        }
    }

    public static short K052109_word_r(int offset) {
        return (short) (K052109_r(offset + 0x2000) | (K052109_r(offset) << 8));
    }

    public static void K052109_word_w(int offset, short data) {
        K052109_w(offset, (byte) ((data >> 8) & 0xff));
        K052109_w(offset + 0x2000, (byte) (data & 0xff));
    }

    public static void K052109_word_w1(int offset, byte data) {
        K052109_w(offset, data);
    }

    public static void K052109_word_w2(int offset, byte data) {
        K052109_w(offset + 0x2000, data);
    }

    public static void K052109_set_RMRD_line(LineState state) {
        K052109_RMRD_line = state;
    }

    public static int K052109_get_RMRD_line() {
        return K052109_RMRD_line.ordinal();
    }

    public static void K052109_tilemap_update() {
        if ((K052109_scrollctrl & 0x03) == 0x02) {
            int xscroll, yscroll, offs;
            int scrollram_offset = 0x1a00;
            K052109_tilemap[1].tilemap_set_scroll_rows(256);
            K052109_tilemap[1].tilemap_set_scroll_cols(1);
            yscroll = K052109_ram[0x180c];
            K052109_tilemap[1].tilemap_set_scrolly(0, yscroll + K052109_dy[1]);
            for (offs = 0; offs < 256; offs++) {
                xscroll = K052109_ram[scrollram_offset + 2 * (offs & 0xfff8) + 0] + 256 * K052109_ram[scrollram_offset + 2 * (offs & 0xfff8) + 1];
                xscroll -= 6;
                K052109_tilemap[1].tilemap_set_scrollx((offs + yscroll) & 0xff, xscroll + K052109_dx[1]);
            }
        } else if ((K052109_scrollctrl & 0x03) == 0x03) {
            int xscroll, yscroll, offs;
            int scrollram_offset = 0x1a00;
            K052109_tilemap[1].tilemap_set_scroll_rows(256);
            K052109_tilemap[1].tilemap_set_scroll_cols(1);
            yscroll = K052109_ram[0x180c];
            K052109_tilemap[1].tilemap_set_scrolly(0, yscroll + K052109_dy[1]);
            for (offs = 0; offs < 256; offs++) {
                xscroll = K052109_ram[scrollram_offset + 2 * offs + 0] + 256 * K052109_ram[scrollram_offset + 2 * offs + 1];
                xscroll -= 6;
                K052109_tilemap[1].tilemap_set_scrollx((offs + yscroll) & 0xff, xscroll + K052109_dx[1]);
            }
        } else if ((K052109_scrollctrl & 0x04) == 0x04) {
            int xscroll, yscroll, offs;
            int scrollram_offset = 0x1800;
            K052109_tilemap[1].tilemap_set_scroll_rows(1);
            K052109_tilemap[1].tilemap_set_scroll_cols(512);
            xscroll = K052109_ram[0x1a00] + 256 * K052109_ram[0x1a01];
            xscroll -= 6;
            K052109_tilemap[1].tilemap_set_scrollx(0, xscroll + K052109_dx[1]);
            for (offs = 0; offs < 512; offs++) {
                yscroll = K052109_ram[scrollram_offset + offs / 8];
                K052109_tilemap[1].tilemap_set_scrolly((offs + xscroll) & 0x1ff, yscroll + K052109_dy[1]);
            }
        } else {
            int xscroll, yscroll;
            int scrollram_offset = 0x1a00;
            K052109_tilemap[1].tilemap_set_scroll_rows(1);
            K052109_tilemap[1].tilemap_set_scroll_cols(1);
            xscroll = K052109_ram[scrollram_offset + 0] + 256 * K052109_ram[scrollram_offset + 1];
            xscroll -= 6;
            yscroll = K052109_ram[0x180c];
            K052109_tilemap[1].tilemap_set_scrollx(0, xscroll + K052109_dx[1]);
            K052109_tilemap[1].tilemap_set_scrolly(0, yscroll + K052109_dy[1]);
        }
        if ((K052109_scrollctrl & 0x18) == 0x10) {
            int xscroll, yscroll, offs;
            int scrollram_offset = 0x3a00;
            K052109_tilemap[2].tilemap_set_scroll_rows(256);
            K052109_tilemap[2].tilemap_set_scroll_cols(1);
            yscroll = K052109_ram[0x380c];
            K052109_tilemap[2].tilemap_set_scrolly(0, yscroll + K052109_dy[2]);
            for (offs = 0; offs < 256; offs++) {
                xscroll = K052109_ram[scrollram_offset + 2 * (offs & 0xfff8) + 0] + 256 * K052109_ram[scrollram_offset + 2 * (offs & 0xfff8) + 1];
                xscroll -= 6;
                K052109_tilemap[2].tilemap_set_scrollx((offs + yscroll) & 0xff, xscroll + K052109_dx[2]);
            }
        } else if ((K052109_scrollctrl & 0x18) == 0x18) {
            int xscroll, yscroll, offs;
            int scrollram_offset = 0x3a00;
            K052109_tilemap[2].tilemap_set_scroll_rows(256);
            K052109_tilemap[2].tilemap_set_scroll_cols(1);
            yscroll = K052109_ram[0x380c];
            K052109_tilemap[2].tilemap_set_scrolly(0, yscroll + K052109_dy[2]);
            for (offs = 0; offs < 256; offs++) {
                xscroll = K052109_ram[scrollram_offset + 2 * offs + 0] + 256 * K052109_ram[scrollram_offset + 2 * offs + 1];
                xscroll -= 6;
                K052109_tilemap[2].tilemap_set_scrollx((offs + yscroll) & 0xff, xscroll + K052109_dx[2]);
            }
        } else if ((K052109_scrollctrl & 0x20) == 0x20) {
            int xscroll, yscroll, offs;
            int scrollram_offset = 0x3800;
            K052109_tilemap[2].tilemap_set_scroll_rows(1);
            K052109_tilemap[2].tilemap_set_scroll_cols(512);
            xscroll = K052109_ram[0x3a00] + 256 * K052109_ram[0x3a01];
            xscroll -= 6;
            K052109_tilemap[2].tilemap_set_scrollx(0, xscroll + K052109_dx[2]);
            for (offs = 0; offs < 512; offs++) {
                yscroll = K052109_ram[scrollram_offset + offs / 8];
                K052109_tilemap[2].tilemap_set_scrolly((offs + xscroll) & 0x1ff, yscroll + K052109_dy[2]);
            }
        } else {
            int xscroll, yscroll;
            int scrollram_offset = 0x3a00;
            K052109_tilemap[2].tilemap_set_scroll_rows(1);
            K052109_tilemap[2].tilemap_set_scroll_cols(1);
            xscroll = K052109_ram[scrollram_offset + 0] + 256 * K052109_ram[scrollram_offset + 1];
            xscroll -= 6;
            yscroll = K052109_ram[0x380c];
            K052109_tilemap[2].tilemap_set_scrollx(0, xscroll + K052109_dx[2]);
            K052109_tilemap[2].tilemap_set_scrolly(0, yscroll + K052109_dy[2]);
        }
    }

    public static int K052109_is_IRQ_enabled() {
        return K052109_irq_enabled;
    }

    public static void SaveStateBinary_K052109(BinaryWriter writer) {
        int i;
        writer.write(K052109_ram, 0, 0x6000);
        writer.write(K052109_RMRD_line.ordinal());
        writer.write(K052109_romsubbank);
        writer.write(K052109_scrollctrl);
        writer.write(K052109_irq_enabled);
        writer.write(K052109_charrombank, 0, 4);
        writer.write(K052109_charrombank_2, 0, 4);
        for (i = 0; i < 3; i++) {
            writer.write(K052109_dx[i]);
        }
        for (i = 0; i < 3; i++) {
            writer.write(K052109_dy[i]);
        }
        writer.write(has_extra_video_ram);
        writer.write(K052109_tileflip_enable);
    }

    public static void LoadStateBinary_K052109(BinaryReader reader) throws IOException {
        int i;
        K052109_ram = reader.readBytes(0x6000);
        K052109_RMRD_line = LineState.values()[reader.readInt32()];
        K052109_romsubbank = reader.readByte();
        K052109_scrollctrl = reader.readByte();
        K052109_irq_enabled = reader.readByte();
        K052109_charrombank = reader.readBytes(4);
        K052109_charrombank_2 = reader.readBytes(4);
        for (i = 0; i < 3; i++) {
            K052109_dx[i] = reader.readInt32();
        }
        for (i = 0; i < 3; i++) {
            K052109_dy[i] = reader.readInt32();
        }
        has_extra_video_ram = reader.readByte();
        K052109_tileflip_enable = reader.readInt32();
    }

    public static void LoadStateBinary_K052109_2(BinaryReader reader) throws IOException {
        int i;
        reader.readBytes(0x6000);
        reader.readInt32();
        reader.readByte();
        reader.readByte();
        reader.readByte();
        reader.readBytes(4);
        reader.readBytes(4);
        for (i = 0; i < 3; i++) {
            reader.readInt32();
        }
        for (i = 0; i < 3; i++) {
            reader.readInt32();
        }
        reader.readByte();
        reader.readInt32();
    }

    public static void K051960_vh_start() {
        K051960_dx = K051960_dy = 0;
        K051960_ram = new byte[0x400];
        K051960_spriterombank = new byte[3];
    }

    public static byte K051960_fetchromdata(int byte1) {
        int code, color, pri, shadow, off1, addr;
        int[] code2 = new int[1], color2 = new int[1], pri2 = new int[1];
        addr = K051960_romoffset + (K051960_spriterombank[0] << 8) +
                ((K051960_spriterombank[1] & 0x03) << 16);
        code = (addr & 0x3_ffe0) >> 5;
        off1 = addr & 0x1f;
        color = ((K051960_spriterombank[1] & 0xfc) >> 2) + ((K051960_spriterombank[2] & 0x03) << 6);
        pri = 0;
        shadow = color & 0x80;
        K051960_callback.accept(code, color, pri, shadow, /* out */  code2, /* out */  color2, /* out */  pri2);
        addr = (code2[0] << 7) | (off1 << 2) | byte1;
        addr &= K051960_memory_region.length - 1;
        return K051960_memory_region[addr];
    }

    public static byte K051960_r(int offset) {
        if (K051960_readroms != 0) {
            K051960_romoffset = (offset & 0x3fc) >> 2;
            return K051960_fetchromdata(offset & 3);
        } else {
            return K051960_ram[offset];
        }
    }

    public static void K051960_w(int offset, byte data) {
        K051960_ram[offset] = data;
    }

    public static byte K051937_r(int offset) {
        if (K051960_readroms != 0 && offset >= 4 && offset < 8) {
            return K051960_fetchromdata(offset & 3);
        } else {
            if (offset == 0) {
                return (byte) ((counter++) & 1);
            }
            return 0;
        }
    }

    public static void K051937_w(int offset, byte data) {
        if (offset == 0) {
            K051960_irq_enabled = (data & 0x01);
            K051960_nmi_enabled = (data & 0x04);
            K051960_spriteflip = data & 0x08;
            K051960_readroms = data & 0x20;
        } else if (offset == 1) {
            //
        } else if (offset >= 2 && offset < 5) {
            K051960_spriterombank[offset - 2] = data;
        } else {
            //
        }
    }

    public static void K051960_sprites_draw(RECT cliprect, int min_priority, int max_priority) {
        int ox, oy, code, color, pri, shadow, size, w, h, x, y, flipx, flipy, zoomx, zoomy;
        int[] code2 = new int[1], color2 = new int[1], pri2 = new int[1];
        int offs, pri_code;
        int[] sortedlist = new int[128];
        int[] xoffset = new int[] {0, 1, 4, 5, 16, 17, 20, 21};
        int[] yoffset = new int[] {0, 2, 8, 10, 32, 34, 40, 42};
        int[] width = new int[] {1, 2, 1, 2, 4, 2, 4, 8};
        int[] height = new int[] {1, 1, 2, 2, 2, 4, 4, 8};
        for (offs = 0; offs < 128; offs++) {
            sortedlist[offs] = -1;
        }
        for (offs = 0; offs < 0x400; offs += 8) {
            if ((K051960_ram[offs] & 0x80) != 0) {
                if (max_priority == -1) {
                    sortedlist[(K051960_ram[offs] & 0x7f) ^ 0x7f] = offs;
                } else {
                    sortedlist[K051960_ram[offs] & 0x7f] = offs;
                }
            }
        }
        for (pri_code = 0; pri_code < 128; pri_code++) {
            offs = sortedlist[pri_code];
            if (offs == -1) {
                continue;
            }
            code = K051960_ram[offs + 2] + ((K051960_ram[offs + 1] & 0x1f) << 8);
            color = K051960_ram[offs + 3] & 0xff;
            pri = 0;
            shadow = color & 0x80;
            K051960_callback.accept(code, color, pri, shadow, /* out */  code2, /* out */  color2, /* out */  pri2);
            code = code2[0];
            color = color2[0];
            pri = pri2[0];
            if (max_priority != -1) {
                if (pri < min_priority || pri > max_priority) {
                    continue;
                }
            }
            size = (K051960_ram[offs + 1] & 0xe0) >> 5;
            w = width[size];
            h = height[size];
            if (w >= 2) code &= ~0x01;
            if (h >= 2) code &= ~0x02;
            if (w >= 4) code &= ~0x04;
            if (h >= 4) code &= ~0x08;
            if (w >= 8) code &= ~0x10;
            if (h >= 8) code &= ~0x20;
            ox = (256 * K051960_ram[offs + 6] + K051960_ram[offs + 7]) & 0x01ff;
            oy = 256 - ((256 * K051960_ram[offs + 4] + K051960_ram[offs + 5]) & 0x01ff);
            ox += K051960_dx;
            oy += K051960_dy;
            flipx = K051960_ram[offs + 6] & 0x02;
            flipy = K051960_ram[offs + 4] & 0x02;
            zoomx = (K051960_ram[offs + 6] & 0xfc) >> 2;
            zoomy = (K051960_ram[offs + 4] & 0xfc) >> 2;
            zoomx = 0x1_0000 / 128 * (128 - zoomx);
            zoomy = 0x1_0000 / 128 * (128 - zoomy);
            if (K051960_spriteflip != 0) {
                ox = 512 - (zoomx * w >> 12) - ox;
                oy = 256 - (zoomy * h >> 12) - oy;
                flipx = (flipx == 0) ? 1 : 0;
                flipy = (flipy == 0) ? 1 : 0;
            }
            if (zoomx == 0x1_0000 && zoomy == 0x1_0000) {
                int sx, sy;
                for (y = 0; y < h; y++) {
                    sy = oy + 16 * y;
                    for (x = 0; x < w; x++) {
                        int c = code;
                        sx = ox + 16 * x;
                        if (flipx != 0) {
                            c += xoffset[(w - 1 - x)];
                        } else {
                            c += xoffset[x];
                        }
                        if (flipy != 0) {
                            c += yoffset[(h - 1 - y)];
                        } else {
                            c += yoffset[y];
                        }
                        if (max_priority == -1) {
                            common_drawgfx_konami68000(gfx22rom, c, color, flipx, flipy, sx & 0x1ff, sy, cliprect, pri | (1 << 31));
                                /*pdrawgfx(bitmap, K051960_gfx,
                                        c,
                                        color,
                                        flipx, flipy,
                                        sx & 0x1ff, sy,
                                        cliprect, shadow ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN, 0, pri);*/
                        } else {
                            common_drawgfx_konami68000(gfx22rom, c, color, flipx, flipy, sx & 0x1ff, sy, cliprect, 0);
                                /*drawgfx(bitmap, K051960_gfx,
                                        c,
                                        color,
                                        flipx, flipy,
                                        sx & 0x1ff, sy,
                                        cliprect, shadow ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN, 0);*/
                        }
                    }
                }
            } else {
                int sx, sy, zw, zh;
                for (y = 0; y < h; y++) {
                    sy = oy + ((zoomy * y + (1 << 11)) >> 12);
                    zh = (oy + ((zoomy * (y + 1) + (1 << 11)) >> 12)) - sy;
                    for (x = 0; x < w; x++) {
                        int c = code;
                        sx = ox + ((zoomx * x + (1 << 11)) >> 12);
                        zw = (ox + ((zoomx * (x + 1) + (1 << 11)) >> 12)) - sx;
                        if (flipx != 0) {
                            c += xoffset[(w - 1 - x)];
                        } else c += xoffset[x];
                        if (flipy != 0) {
                            c += yoffset[(h - 1 - y)];
                        } else {
                            c += yoffset[y];
                        }
                        if (max_priority == -1) {
                            common_drawgfxzoom_konami68000(gfx22rom, c, color, flipx, flipy, sx & 0x1ff, sy, cliprect, 0, (zw << 16) / 16, (zh << 16) / 16, pri | (1 << 31));
                                /*pdrawgfxzoom(bitmap, K051960_gfx,
                                        c,
                                        color,
                                        flipx, flipy,
                                        sx & 0x1ff, sy,
                                        cliprect, shadow ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN, 0,
                                        (zw << 16) / 16, (zh << 16) / 16, pri);*/
                        } else {
                            common_drawgfxzoom_konami68000(gfx22rom, c, color, flipx, flipy, sx & 0x1ff, sy, cliprect, 0, (zw << 16) / 16, (zh << 16) / 16);
                                /*drawgfxzoom(bitmap, K051960_gfx,
                                        c,
                                        color,
                                        flipx, flipy,
                                        sx & 0x1ff, sy,
                                        cliprect, shadow ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN, 0,
                                        (zw << 16) / 16, (zh << 16) / 16);*/
                        }
                    }
                }
            }
        }
    }

    public static void SaveStateBinary_K051960(BinaryWriter writer) {
        writer.write(K051960_romoffset);
        writer.write(K051960_spriteflip);
        writer.write(K051960_readroms);
        writer.write(K051960_spriterombank, 0, 3);
        writer.write(K051960_ram, 0, 0x400);
        writer.write(K051960_dx);
        writer.write(K051960_dy);
        writer.write(K051960_irq_enabled);
        writer.write(K051960_nmi_enabled);
    }

    public static void LoadStateBinary_K051960(BinaryReader reader) throws IOException {
        K051960_romoffset = reader.readInt32();
        K051960_spriteflip = reader.readInt32();
        K051960_readroms = reader.readInt32();
        K051960_spriterombank = reader.readBytes(3);
        K051960_ram = reader.readBytes(0x400);
        K051960_dx = reader.readInt32();
        K051960_dy = reader.readInt32();
        K051960_irq_enabled = reader.readInt32();
        K051960_nmi_enabled = reader.readInt32();
    }

    public static void LoadStateBinary_K051960_2(BinaryReader reader) throws IOException {
        reader.readInt32();
        reader.readInt32();
        reader.readInt32();
        reader.readBytes(3);
        reader.readBytes(0x400);
        reader.readInt32();
        reader.readInt32();
        reader.readInt32();
        reader.readInt32();
    }

    public static void K05324x_set_z_rejection(int zcode) {
        K05324x_z_rejection = zcode;
    }

    public static void K053245_vh_start() {
        int i;
        Drawgfx.gfx_drawmode_table[0] = 0;
        for (i = 1; i < 15; i++) {
            Drawgfx.gfx_drawmode_table[i] = 1;
        }
        Drawgfx.gfx_drawmode_table[15] = 2;
        K05324x_z_rejection = -1;
        K053244_rombank[0] = 0;
        K053245_ramsize[0] = 0x800;
        K053245_ram[0] = new byte[K053245_ramsize[0]];
        K053245_dx[0] = K053245_dy[0] = 0;
        K053245_buffer[0] = new short[K053245_ramsize[0] / 2];
        for (i = 0; i < K053245_ramsize[0]; i++) {
            K053245_ram[0][i] = 0;
        }
        for (i = 0; i < K053245_ramsize[0] / 2; i++) {
            K053245_buffer[0][i] = 0;
        }
    }

    public static short K053245_word_r(int offset) {
        return (short) (K053245_ram[0][offset * 2] * 0x100 + K053245_ram[0][offset * 2 + 1]);
    }

    public static void K053245_word_w(int offset, short data) {
        K053245_ram[0][offset * 2] = (byte) (data >> 8);
        K053245_ram[0][offset * 2 + 1] = (byte) data;
    }

    public static void K053245_word_w2(int offset, short data) {
        K053245_ram[0][offset * 2 + 1] = (byte) data;
    }

    public static void K053245_clear_buffer(int chip) {
        int i, e;
        for (e = K053245_ramsize[chip] / 2, i = 0; i < e; i += 8) {
            K053245_buffer[chip][i] = 0;
        }
    }

    public static void K053245_update_buffer(int chip) {
        int i;
        for (i = 0; i < K053245_ramsize[chip] / 2; i++) {
            K053245_buffer[chip][i] = (short) (K053245_ram[chip][i * 2] * 0x100 + K053245_ram[chip][i * 2 + 1]);
        }
    }

    public static byte K053244_chip_r(int chip, int offset) {
        if ((K053244_regs[chip][5] & 0x10) != 0 && offset >= 0x0c && offset < 0x10) {
            int addr;
            addr = (K053244_rombank[chip] << 19) | ((K053244_regs[chip][11] & 0x7) << 18) | (K053244_regs[chip][8] << 10) | (K053244_regs[chip][9] << 2) | ((offset & 3) ^ 1);
            addr &= K053245_memory_region[chip].length - 1;
            return K053245_memory_region[chip][addr];
        } else if (offset == 0x06) {
            K053245_update_buffer(chip);
            return 0;
        } else {
            return 0;
        }
    }

    public static byte K053244_r(int offset) {
        return K053244_chip_r(0, offset);
    }

    public static void K053244_chip_w(int chip, int offset, byte data) {
        K053244_regs[chip][offset] = data;
        switch (offset) {
            case 0x05: {
                break;
            }
            case 0x06:
                K053245_update_buffer(chip);
                break;
        }
    }

    public static void K053244_w(int offset, byte data) {
        K053244_chip_w(0, offset, data);
    }

    public static short K053244_lsb_r(int offset) {
        return K053244_r(offset);
    }

    public static void K053244_lsb_w(int offset, short data) {
        //if (ACCESSING_BITS_0_7)
        K053244_w(offset, (byte) (data & 0xff));
    }

    public static void K053244_lsb_w2(int offset, byte data) {
        K053244_w(offset, data);
    }

    public static void K053244_bankselect(int chip, int bank) {
        K053244_rombank[chip] = bank;
    }

    public static void K053245_sprites_draw(RECT cliprect) {
        int offs, pri_code, i;
        int[] sortedlist = new int[128];
        int flipscreenX, flipscreenY, spriteoffsX, spriteoffsY;
        flipscreenX = K053244_regs[0][5] & 0x01;
        flipscreenY = K053244_regs[0][5] & 0x02;
        spriteoffsX = (K053244_regs[0][0] << 8) | K053244_regs[0][1];
        spriteoffsY = (K053244_regs[0][2] << 8) | K053244_regs[0][3];
        for (offs = 0; offs < 128; offs++) {
            sortedlist[offs] = -1;
        }
        i = K053245_ramsize[0] / 2;
        for (offs = 0; offs < i; offs += 8) {
            pri_code = K053245_buffer[0][offs];
            if ((pri_code & 0x8000) != 0) {
                pri_code &= 0x007f;
                if (offs != 0 && pri_code == K05324x_z_rejection) {
                    continue;
                }
                if (sortedlist[pri_code] == -1) {
                    sortedlist[pri_code] = offs;
                }
            }
        }
        for (pri_code = 127; pri_code >= 0; pri_code--) {
            int ox, oy, color, code, size, w, h, x, y, flipx, flipy, mirrorx, mirrory, shadow, zoomx, zoomy, pri;
            int[] color2 = new int[1], code2 = new int[1], pri2 = new int[1];
            offs = sortedlist[pri_code];
            if (offs == -1) {
                continue;
            }
            code = K053245_buffer[0][offs + 1];
            code = ((code & 0xffe1) + ((code & 0x0010) >> 2) + ((code & 0x0008) << 1) + ((code & 0x0004) >> 1) + ((code & 0x0002) << 2));
            color = K053245_buffer[0][offs + 6] & 0x00ff;
            pri = 0;
            K053245_callback.accept(code, color, /* out */  code2, /* out */  color2, /* out */  pri2);
            size = (K053245_buffer[0][offs] & 0x0f00) >> 8;
            w = 1 << (size & 0x03);
            h = 1 << ((size >> 2) & 0x03);
            zoomy = K053245_buffer[0][offs + 4];
            if (zoomy > 0x2000) {
                continue;
            }
            if (zoomy != 0) {
                zoomy = (0x40_0000 + zoomy / 2) / zoomy;
            } else {
                zoomy = 2 * 0x40_0000;
            }
            if ((K053245_buffer[0][offs] & 0x4000) == 0) {
                zoomx = K053245_buffer[0][offs + 5];
                if (zoomx > 0x2000) {
                    continue;
                }
                if (zoomx != 0) {
                    zoomx = (0x40_0000 + zoomx / 2) / zoomx;
                } else {
                    zoomx = 2 * 0x40_0000;
                }
            } else {
                zoomx = zoomy;
            }
            ox = K053245_buffer[0][offs + 3] + spriteoffsX;
            oy = K053245_buffer[0][offs + 2];
            ox += K053245_dx[0];
            oy += K053245_dy[0];
            flipx = K053245_buffer[0][offs] & 0x1000;
            flipy = K053245_buffer[0][offs] & 0x2000;
            mirrorx = K053245_buffer[0][offs + 6] & 0x0100;
            if (mirrorx != 0) {
                flipx = 0;
            }
            mirrory = K053245_buffer[0][offs + 6] & 0x0200;
            shadow = K053245_buffer[0][offs + 6] & 0x0080;
            if (flipscreenX != 0) {
                ox = 512 - ox;
                if (mirrorx == 0) {
                    flipx = (flipx == 0) ? 1 : 0;
                }
            }
            if (flipscreenY != 0) {
                oy = -oy;
                if (mirrory == 0) {
                    flipy = (flipy == 0) ? 1 : 0;
                }
            }
            ox = (ox + 0x5d) & 0x3ff;
            if (ox >= 768) {
                ox -= 1024;
            }
            oy = (-(oy + spriteoffsY + 0x07)) & 0x3ff;
            if (oy >= 640) {
                oy -= 1024;
            }
            ox -= (zoomx * w) >> 13;
            oy -= (zoomy * h) >> 13;
            for (y = 0; y < h; y++) {
                int sx, sy, zw, zh;
                sy = oy + ((zoomy * y + (1 << 11)) >> 12);
                zh = (oy + ((zoomy * (y + 1) + (1 << 11)) >> 12)) - sy;
                for (x = 0; x < w; x++) {
                    int c, fx, fy;
                    sx = ox + ((zoomx * x + (1 << 11)) >> 12);
                    zw = (ox + ((zoomx * (x + 1) + (1 << 11)) >> 12)) - sx;
                    c = code2[0];
                    if (mirrorx != 0) {
                        if ((flipx == 0) ^ (2 * x < w)) {
                            c += (w - x - 1);
                            fx = 1;
                        } else {
                            c += x;
                            fx = 0;
                        }
                    } else {
                        if (flipx != 0) {
                            c += w - 1 - x;
                        } else {
                            c += x;
                        }
                        fx = flipx;
                    }
                    if (mirrory != 0) {
                        if ((flipy == 0) ^ (2 * y >= h)) {
                            c += 8 * (h - y - 1);
                            fy = 1;
                        } else {
                            c += 8 * y;
                            fy = 0;
                        }
                    } else {
                        if (flipy != 0) {
                            c += 8 * (h - 1 - y);
                        } else {
                            c += 8 * y;
                        }
                        fy = flipy;
                    }
                    c = (c & 0x3f) | (code2[0] & ~0x3f);
                    if (zoomx == 0x1_0000 && zoomy == 0x1_0000) {
                        common_drawgfx_konami68000(gfx22rom, c, color2[0], fx, fy, sx, sy, cliprect, pri2[0] | (1 << 31));
                            /*pdrawgfx(bitmap, K053245_gfx[chip],
                                    c,
                                    color2,
                                    fx, fy,
                                    sx, sy,
                                    cliprect, shadow ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN, 0, pri);*/
                    } else {
                        common_drawgfxzoom_konami68000(gfx22rom, c, color2[0], fx, fy, sx, sy, cliprect, 0, (zw << 16) / 16, (zh << 16) / 16, pri2[0] | 1 << 31);
                            /*pdrawgfxzoom(bitmap, K053245_gfx[chip],
                                    c,
                                    color2,
                                    fx, fy,
                                    sx, sy,
                                    cliprect, shadow ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN, 0,
                                    (zw << 16) / 16, (zh << 16) / 16, pri);*/
                    }
                }
            }
        }
    }

    public static void SaveStateBinary_K053245(BinaryWriter writer) {
        int i;
        writer.write(K05324x_z_rejection);
        writer.write(K053245_ram[0], 0, 0x800);
        for (i = 0; i < 0x400; i++) {
            writer.write(K053245_buffer[0][i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(K053244_rombank[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(K053245_dx[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(K053245_dy[i]);
        }
        writer.write(K053244_regs[0], 0, 0x10);
        writer.write(K054000_ram, 0, 0x20);
    }

    public static void LoadStateBinary_K053245(BinaryReader reader) throws IOException {
        int i;
        K05324x_z_rejection = reader.readInt32();
        K053245_ram[0] = reader.readBytes(0x800);
        for (i = 0; i < 0x400; i++) {
            K053245_buffer[0][i] = reader.readUInt16();
        }
        for (i = 0; i < 2; i++) {
            K053244_rombank[i] = reader.readInt32();
        }
        for (i = 0; i < 2; i++) {
            K053245_dx[i] = reader.readInt32();
        }
        for (i = 0; i < 2; i++) {
            K053245_dy[i] = reader.readInt32();
        }
        K053244_regs[0] = reader.readBytes(0x10);
        K054000_ram = reader.readBytes(0x20);
    }

    public static void K053936_zoom_draw(int chip, short[] ctrl, short[] linectrl, RECT cliprect, Tmap tmap, int flags, int priority) {
        if ((ctrl[0x07] & 0x0040) != 0) {
            int startx, starty;
            int incxx, incxy;
            RECT my_clip = new RECT();
            int y, maxy;
            if (((ctrl[0x07] & 0x0002) != 0) && (ctrl[0x09] != 0)) {
                my_clip.min_x = ctrl[0x08] + K053936_offset[chip][0] + 2;
                my_clip.max_x = ctrl[0x09] + K053936_offset[chip][0] + 2 - 1;
                if (my_clip.min_x < cliprect.min_x) {
                    my_clip.min_x = cliprect.min_x;
                }
                if (my_clip.max_x > cliprect.max_x) {
                    my_clip.max_x = cliprect.max_x;
                }
                y = ctrl[0x0a] + K053936_offset[chip][1] - 2;
                if (y < cliprect.min_y) {
                    y = cliprect.min_y;
                }
                maxy = ctrl[0x0b] + K053936_offset[chip][1] - 2 - 1;
                if (maxy > cliprect.max_y) {
                    maxy = cliprect.max_y;
                }
            } else {
                my_clip.min_x = cliprect.min_x;
                my_clip.max_x = cliprect.max_x;
                y = cliprect.min_y;
                maxy = cliprect.max_y;
            }
            while (y <= maxy) {
                //UINT16 *lineaddr = linectrl + 4*((y - K053936_offset[chip][1]) & 0x1ff);
                int lineaddr_offset = 4 * ((y - K053936_offset[chip][1]) & 0x1ff);
                my_clip.min_y = my_clip.max_y = y;
                startx = 256 * (short) (linectrl[lineaddr_offset] + ctrl[0x00]);
                starty = 256 * (short) (linectrl[lineaddr_offset + 1] + ctrl[0x01]);
                incxx = linectrl[lineaddr_offset + 2];
                incxy = linectrl[lineaddr_offset + 3];

                if ((ctrl[0x06] & 0x8000) != 0) {
                    incxx *= 256;
                }
                if ((ctrl[0x06] & 0x0080) != 0) {
                    incxy *= 256;
                }
                startx -= K053936_offset[chip][0] * incxx;
                starty -= K053936_offset[chip][0] * incxy;
                //tilemap_draw_roz(bitmap,&my_clip,tmap,startx << 5,starty << 5,incxx << 5,incxy << 5,0,0,K053936_wraparound[chip],flags,priority);
                y++;
            }
        } else {
            int startx, starty;
            int incxx, incxy, incyx, incyy;
            startx = 256 * ctrl[0x00];
            starty = 256 * ctrl[0x01];
            incyx = ctrl[0x02];
            incyy = ctrl[0x03];
            incxx = ctrl[0x04];
            incxy = ctrl[0x05];
            if ((ctrl[0x06] & 0x4000) != 0) {
                incyx *= 256;
                incyy *= 256;
            }
            if ((ctrl[0x06] & 0x0040) != 0) {
                incxx *= 256;
                incxy *= 256;
            }
            startx -= K053936_offset[chip][1] * incyx;
            starty -= K053936_offset[chip][1] * incyy;
            startx -= K053936_offset[chip][0] * incxx;
            starty -= K053936_offset[chip][0] * incxy;
            //tilemap_draw_roz(bitmap,cliprect,tmap,startx << 5,starty << 5,incxx << 5,incxy << 5,incyx << 5,incyy << 5,K053936_wraparound[chip],flags,priority);
        }
    }

    public static void K053936_0_zoom_draw(RECT cliprect, Tmap tmap, int flags, int priority) {
        K053936_zoom_draw(0, K053936_0_ctrl, K053936_0_linectrl, cliprect, tmap, flags, priority);
    }

    public static void K053936_wraparound_enable(int chip, int status) {
        K053936_wraparound[chip] = status;
    }

    public static void K053936_set_offset(int chip, int xoffs, int yoffs) {
        K053936_offset[chip][0] = xoffs;
        K053936_offset[chip][1] = yoffs;
    }

    public static void SaveStateBinary_K053936(BinaryWriter writer) {
        int i, j;
        for (i = 0; i < 0x10; i++) {
            writer.write(K053936_0_ctrl[i]);
        }
        for (i = 0; i < 0x800; i++) {
            writer.write(K053936_0_linectrl[i]);
        }
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 2; j++) {
                writer.write(K053936_offset[i][j]);
            }
        }
        for (i = 0; i < 2; i++) {
            writer.write(K053936_wraparound[i]);
        }
    }

    public static void LoadStateBinary_K053936(BinaryReader reader) throws IOException {
        int i, j;
        for (i = 0; i < 0x10; i++) {
            K053936_0_ctrl[i] = reader.readUInt16();
        }
        for (i = 0; i < 0x800; i++) {
            K053936_0_linectrl[i] = reader.readUInt16();
        }
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 2; j++) {
                K053936_offset[i][j] = reader.readInt32();
            }
        }
        for (i = 0; i < 2; i++) {
            K053936_wraparound[i] = reader.readInt32();
        }
    }

    public static void K053251_vh_start() {
        K053251_set_tilemaps(null, null, null, null, null);
    }

    public static void K053251_set_tilemaps(Tmap ci0, Tmap ci1, Tmap ci2, Tmap ci3, Tmap ci4) {
        K053251_tilemaps[0] = ci0;
        K053251_tilemaps[1] = ci1;
        K053251_tilemaps[2] = ci2;
        K053251_tilemaps[3] = ci3;
        K053251_tilemaps[4] = ci4;
        if (ci0 == null && ci1 == null && ci2 == null && ci3 == null && ci4 == null) {
            K053251_tilemaps_set = 0;
        } else {
            K053251_tilemaps_set = 1;
        }
    }

    public static void K053251_w(int offset, byte data) {
        int i, newind;
        data &= 0x3f;
        if (K053251_ram[offset] != data) {
            K053251_ram[offset] = data;
            if (offset == 9) {
                for (i = 0; i < 3; i++) {
                    newind = 32 * ((data >> 2 * i) & 0x03);
                    if (K053251_palette_index[i] != newind) {
                        K053251_palette_index[i] = newind;
                        if (K053251_tilemaps[i] != null) {
                            K053251_tilemaps[i].all_tiles_dirty = true;
                        }
                    }
                }
                if (K053251_tilemaps_set == 0) {
                    for (i = 0; i < 3; i++) {
                        K052109_tilemap[i].all_tiles_dirty = true;
                    }
                }
            } else if (offset == 10) {
                for (i = 0; i < 2; i++) {
                    newind = 16 * ((data >> 3 * i) & 0x07);
                    if (K053251_palette_index[3 + i] != newind) {
                        K053251_palette_index[3 + i] = newind;
                        if (K053251_tilemaps[3 + i] != null) {
                            K053251_tilemaps[3 + i].all_tiles_dirty = true;
                        }
                    }
                }
                if (K053251_tilemaps_set == 0) {
                    for (i = 0; i < 3; i++) {
                        K052109_tilemap[i].all_tiles_dirty = true;
                    }
                }
            }
        }
    }

    public static void K053251_lsb_w(int offset, short data) {
        //if (ACCESSING_BITS_0_7)
        K053251_w(offset, (byte) (data & 0xff));
    }

    public static void K053251_lsb_w2(int offset, byte data) {
        K053251_w(offset, data);
    }

    public static void K053251_msb_w(int offset, short data) {
        //if (ACCESSING_BITS_8_15)
        K053251_w(offset, (byte) ((data >> 8) & 0xff));
    }

    public static void K053251_msb_w1(int offset, byte data) {
        K053251_w(offset, data);
    }

    public static int K053251_get_priority(int ci) {
        return K053251_ram[ci];
    }

    public static int K053251_get_palette_index(int ci) {
        return K053251_palette_index[ci];
    }

    public static void SaveStateBinary_K053251(BinaryWriter writer) {
        int i;
        writer.write(K053251_ram);
        for (i = 0; i < 5; i++) {
            writer.write(K053251_palette_index[i]);
        }
        writer.write(K053251_tilemaps_set);
    }

    public static void LoadStateBinary_K053251(BinaryReader reader) throws IOException {
        int i;
        K053251_ram = reader.readBytes(0x10);
        for (i = 0; i < 5; i++) {
            K053251_palette_index[i] = reader.readInt32();
        }
        K053251_tilemaps_set = reader.readInt32();
    }

    public static void LoadStateBinary_K053251_2(BinaryReader reader) throws IOException {
        int i;
        reader.readBytes(0x10);
        for (i = 0; i < 5; i++) {
            reader.readInt32();
        }
        reader.readInt32();
    }

    public static void K054000_w(int offset, byte data) {
        K054000_ram[offset] = data;
    }

    public static byte K054000_r(int offset) {
        int Acx, Acy, Aax, Aay;
        int Bcx, Bcy, Bax, Bay;
        if (offset != 0x18) {
            return 0;
        }
        Acx = (K054000_ram[0x01] << 16) | (K054000_ram[0x02] << 8) | K054000_ram[0x03];
        Acy = (K054000_ram[0x09] << 16) | (K054000_ram[0x0a] << 8) | K054000_ram[0x0b];
        if (K054000_ram[0x04] == (byte) 0xff) {
            Acx += 3;
        }
        if (K054000_ram[0x0c] == (byte) 0xff) {
            Acy += 3;
        }
        Aax = K054000_ram[0x06] + 1;
        Aay = K054000_ram[0x07] + 1;
        Bcx = (K054000_ram[0x15] << 16) | (K054000_ram[0x16] << 8) | K054000_ram[0x17];
        Bcy = (K054000_ram[0x11] << 16) | (K054000_ram[0x12] << 8) | K054000_ram[0x13];
        Bax = K054000_ram[0x0e] + 1;
        Bay = K054000_ram[0x0f] + 1;
        if (Acx + Aax < Bcx - Bax) {
            return 1;
        }
        if (Bcx + Bax < Acx - Aax) {
            return 1;
        }
        if (Acy + Aay < Bcy - Bay) {
            return 1;
        }
        if (Bcy + Bay < Acy - Aay) {
            return 1;
        }
        return 0;
    }

    public static short K054000_lsb_r(int offset) {
        return K054000_r(offset);
    }

    public static void K054000_lsb_w(int offset, short data) {
        //if (ACCESSING_BITS_0_7)
        K054000_w(offset, (byte) (data & 0xff));
    }

    public static void K054000_lsb_w2(int offset, byte data) {
        K054000_w(offset, data);
    }

//#endregion

//#region Video

    private static int[] layer_colorbase;
    private static int sprite_colorbase, bg_colorbase;
    private static int priorityflag;
    private static int dim_c, dim_v;
    private static int lastdim, lasten, last;
    private static int[] layerpri, sorted_layer;
    private static int blswhstl_rombank, glfgreat_pixel;
    private static int glfgreat_roz_rom_bank, glfgreat_roz_char_bank, glfgreat_roz_rom_mode, prmrsocr_sprite_bank;
    private static Tmap roz_tilemap;

    public static void mia_tile_callback(int layer, int bank, int code, int color, int flags, int priority, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] flags2) {
        flags2[0] = (color & 0x04) != 0 ? 1 : 0;
        if (layer == 0) {
            code2[0] = code | ((color & 0x01) << 8);
            color2[0] = layer_colorbase[layer] + ((color & 0x80) >> 5) + ((color & 0x10) >> 1);
        } else {
            code2[0] = code | ((color & 0x01) << 8) | ((color & 0x18) << 6) | (bank << 11);
            color2[0] = layer_colorbase[layer] + ((color & 0xe0) >> 5);
        }
    }

    public static void cuebrick_tile_callback(int layer, int bank, int code, int color, int flags, int priority, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] flags2) {
        flags2[0] = flags;
        if ((K052109_get_RMRD_line() == 0) && (layer == 0)) {
            code2[0] = code | ((color & 0x01) << 8);
            color2[0] = layer_colorbase[layer] + ((color & 0x80) >> 5) + ((color & 0x10) >> 1);
        } else {
            code2[0] = code | ((color & 0xf) << 8);
            color2[0] = layer_colorbase[layer] + ((color & 0xe0) >> 5);
        }
    }

    public static void tmnt_tile_callback(int layer, int bank, int code, int color, int flags, int priority, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] flags2) {
        flags2[0] = flags;
        code2[0] = code | ((color & 0x03) << 8) | ((color & 0x10) << 6) | ((color & 0x0c) << 9) | (bank << 13);
        color2[0] = layer_colorbase[layer] + ((color & 0xe0) >> 5);
    }

    public static void blswhstl_tile_callback(int layer, int bank, int code, int color, int flags, int priority, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] flags2) {
        flags2[0] = flags;
        code2[0] = code | ((color & 0x01) << 8) | ((color & 0x10) << 5) | ((color & 0x0c) << 8) | (bank << 12) | blswhstl_rombank << 14;
        color2[0] = layer_colorbase[layer] + ((color & 0xe0) >> 5);
    }

    public static void mia_sprite_callback(int code, int color, int priority, int shadow, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority2) {
        code2[0] = code;
        color2[0] = sprite_colorbase + (color & 0x0f);
        priority2[0] = priority;
    }

    public static void tmnt_sprite_callback(int code, int color, int priority, int shadow, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority2) {
        code2[0] = code | ((color & 0x10) << 9);
        color2[0] = sprite_colorbase + (color & 0x0f);
        priority2[0] = priority;
    }

    public static void punkshot_sprite_callback(int code, int color, int priority, int shadow, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority_mask) {
        int pri = 0x20 | ((color & 0x60) >> 2);
        if (pri <= layerpri[2]) {
            priority_mask[0] = 0;
        } else if (pri > layerpri[2] && pri <= layerpri[1]) {
            priority_mask[0] = 0xf0;
        } else if (pri > layerpri[1] && pri <= layerpri[0]) {
            priority_mask[0] = 0xf0 | 0xcc;
        } else {
            priority_mask[0] = 0xf0 | 0xcc | 0xaa;
        }
        code2[0] = code | ((color & 0x10) << 9);
        color2[0] = sprite_colorbase + (color & 0x0f);
    }

    public static void thndrx2_sprite_callback(int code, int color, int proority, int shadow, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority_mask) {
        int pri = 0x20 | ((color & 0x60) >> 2);
        if (pri <= layerpri[2]) {
            priority_mask[0] = 0;
        } else if (pri > layerpri[2] && pri <= layerpri[1]) {
            priority_mask[0] = 0xf0;
        } else if (pri > layerpri[1] && pri <= layerpri[0]) {
            priority_mask[0] = 0xf0 | 0xcc;
        } else {
            priority_mask[0] = 0xf0 | 0xcc | 0xaa;
        }
        code2[0] = code;
        color2[0] = sprite_colorbase + (color & 0x0f);
    }

    public static void lgtnfght_sprite_callback(int code, int color, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority_mask) {
        int pri = 0x20 | ((color & 0x60) >> 2);
        if (pri <= layerpri[2]) {
            priority_mask[0] = 0;
        } else if (pri > layerpri[2] && pri <= layerpri[1]) {
            priority_mask[0] = 0xf0;
        } else if (pri > layerpri[1] && pri <= layerpri[0]) {
            priority_mask[0] = 0xf0 | 0xcc;
        } else {
            priority_mask[0] = 0xf0 | 0xcc | 0xaa;
        }
        code2[0] = code;
        color2[0] = sprite_colorbase + (color & 0x1f);
    }

    public static void blswhstl_sprite_callback(int code, int color, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority_mask) {
        int pri = 0x20 | ((color & 0x60) >> 2);
        if (pri <= layerpri[2]) {
            priority_mask[0] = 0;
        } else if (pri > layerpri[2] && pri <= layerpri[1]) {
            priority_mask[0] = 0xf0;
        } else if (pri > layerpri[1] && pri <= layerpri[0]) {
            priority_mask[0] = 0xf0 | 0xcc;
        } else {
            priority_mask[0] = 0xf0 | 0xcc | 0xaa;
        }
        code2[0] = code;
        color2[0] = sprite_colorbase + (color & 0x1f);
    }

    public static void prmrsocr_sprite_callback(int code, int color, /* out */ int[] code2, /* out */ int[] color2, /* out */ int[] priority_mask) {
        int pri = 0x20 | ((color & 0x60) >> 2);
        if (pri <= layerpri[2]) {
            priority_mask[0] = 0;
        } else if (pri > layerpri[2] && pri <= layerpri[1]) {
            priority_mask[0] = 0xf0;
        } else if (pri > layerpri[1] && pri <= layerpri[0]) {
            priority_mask[0] = 0xf0 | 0xcc;
        } else {
            priority_mask[0] = 0xf0 | 0xcc | 0xaa;
        }
        code2[0] = code | (prmrsocr_sprite_bank << 14);
        color2[0] = sprite_colorbase + (color & 0x1f);
    }

    public static void video_start_tmnt() {
        layer_colorbase[0] = 0;
        layer_colorbase[1] = 32;
        layer_colorbase[2] = 40;
        sprite_colorbase = 16;
        K052109_vh_start();
        K051960_vh_start();
    }

    public static void video_start_punkshot() {
        K053251_vh_start();
        K052109_vh_start();
        K051960_vh_start();
    }

    public static void video_start_lgtnfght() {
        K053251_vh_start();
        K052109_vh_start();
        K053245_vh_start();
        K05324x_set_z_rejection(0);
        dim_c = dim_v = lastdim = lasten = 0;
    }

    public static void video_start_blswhstl() {
        K053251_vh_start();
        K052109_vh_start();
        K053245_vh_start();
    }

    public static void video_start_glfgreat() {
        K053251_vh_start();
        K052109_vh_start();
        K053245_vh_start();
        roz_tilemap = new Tmap();
        roz_tilemap.rows = 512;
        roz_tilemap.cols = 512;
        roz_tilemap.tilewidth = 16;
        roz_tilemap.tileheight = 16;
        roz_tilemap.width = roz_tilemap.cols * roz_tilemap.tilewidth;
        roz_tilemap.height = roz_tilemap.rows * roz_tilemap.tileheight;
        roz_tilemap.enable = true;
        roz_tilemap.all_tiles_dirty = true;
        roz_tilemap.scrollrows = 1;
        roz_tilemap.scrollcols = 1;
        //roz_tilemap = tilemap_create(glfgreat_get_roz_tile_info,tilemap_scan_rows,16,16,512,512);
        //tilemap_set_transparent_pen(roz_tilemap,0);
        K053936_wraparound_enable(0, 1);
        K053936_set_offset(0, 85, 0);
    }

    public static void video_start_thndrx2() {
        K053251_vh_start();
        K052109_vh_start();// "k052109", NORMAL_PLANE_ORDER, tmnt_tile_callback);
        K051960_vh_start(); // , "k051960", NORMAL_PLANE_ORDER, thndrx2_sprite_callback);
    }

    public static void video_start_prmrsocr() {
        K053251_vh_start();
        K052109_vh_start(); // , "k052109", NORMAL_PLANE_ORDER, tmnt_tile_callback);
        K053245_vh_start(); // , 0, "k053245", NORMAL_PLANE_ORDER, prmrsocr_sprite_callback);
        //roz_tilemap = tilemap_create(prmrsocr_get_roz_tile_info, tilemap_scan_rows, 16, 16, 512, 256);
        //tilemap_set_transparent_pen(roz_tilemap, 0);
        K053936_wraparound_enable(0, 0);
        K053936_set_offset(0, 85, 1);
    }

    public static void tmnt_paletteram_word_w(int offset, short data) {
        short data1;
        Generic.paletteram16[offset] = data;
        //COMBINE_DATA(paletteram16 + offset);
        offset &= ~1;
        data1 = (short) ((Generic.paletteram16[offset] << 8) | Generic.paletteram16[offset + 1]);
        Palette.palette_set_callback.accept(offset / 2, Palette.make_rgb(Palette.pal5bit((byte) (data1 >> 0)), Palette.pal5bit((byte) (data1 >> 5)), Palette.pal5bit((byte) (data1 >> 10))));
        //palette_set_color_rgb(machine,offset / 2,pal5bit(data >> 0),pal5bit(data >> 5),pal5bit(data >> 10));
    }

    public static void tmnt_paletteram_word_w1(int offset, byte data) {
        short data1;
        Generic.paletteram16[offset] = (short) ((data << 8) | (Generic.paletteram16[offset] & 0xff));
        offset &= ~1;
        data1 = (short) ((Generic.paletteram16[offset] << 8) | Generic.paletteram16[offset + 1]);
        Palette.palette_set_callback.accept(offset / 2, Palette.make_rgb(Palette.pal5bit((byte) (data1 >> 0)), Palette.pal5bit((byte) (data1 >> 5)), Palette.pal5bit((byte) (data1 >> 10))));
    }

    public static void tmnt_paletteram_word_w2(int offset, byte data) {
        short data1;
        Generic.paletteram16[offset] = (short) ((Generic.paletteram16[offset] & 0xff00) | data);
        offset &= ~1;
        data1 = (short) ((Generic.paletteram16[offset] << 8) | Generic.paletteram16[offset + 1]);
        Palette.palette_set_callback.accept(offset / 2, Palette.make_rgb(Palette.pal5bit((byte) (data1 >> 0)), Palette.pal5bit((byte) (data1 >> 5)), Palette.pal5bit((byte) (data1 >> 10))));
    }

    public static void tmnt_0a0000_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            if (last == 0x08 && (data & 0x08) == 0) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
                //cpunum_set_input_line_and_vector(machine, 1,0,HOLD_LINE,0xff);
            }
            last = data & 0x08;
            Generic.interrupt_enable_w((byte) (data & 0x20));
            K052109_set_RMRD_line((data & 0x80) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void tmnt_0a0000_w2(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            if (last == 0x08 && (data & 0x08) == 0) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
            }
            last = data & 0x08;
            Generic.interrupt_enable_w((byte) (data & 0x20));
            K052109_set_RMRD_line((data & 0x80) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void punkshot_0a0020_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            if (last == 0x04 && (data & 0x04) == 0) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
            }
            last = data & 0x04;
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void punkshot_0a0020_w2(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            if (last == 0x04 && (data & 0x04) == 0) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
            }
            last = data & 0x04;
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void lgtnfght_0a0018_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            if (last == 0x00 && (data & 0x04) == 0x04) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
            }
            last = data & 0x04;
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void lgtnfght_0a0018_w2(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            if (last == 0x00 && (data & 0x04) == 0x04) {
                Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
            }
            last = data & 0x04;
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        }
    }

    public static void blswhstl_700300_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            if (blswhstl_rombank != ((data & 0x80) >> 7)) {
                blswhstl_rombank = (data & 0x80) >> 7;
                //tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
            }
        }
    }

    public static void blswhstl_700300_w2(byte data) {
        //coin_counter_w(0,data & 0x01);
        //coin_counter_w(1,data & 0x02);
        K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        if (blswhstl_rombank != ((data & 0x80) >> 7)) {
            blswhstl_rombank = (data & 0x80) >> 7;
            //tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
        }
    }

    public static short glfgreat_rom_r(int offset) {
        if (glfgreat_roz_rom_mode != 0) {
            return zoomrom[glfgreat_roz_char_bank * 0x8_0000 + offset];
        } else if (offset < 0x4_0000) {
            return (short) (user1rom[offset + 0x8_0000 + glfgreat_roz_rom_bank * 0x4_0000] + 256 * user1rom[offset + glfgreat_roz_rom_bank * 0x4_0000]);
        } else {
            return user1rom[((offset & 0x3_ffff) >> 2) + 0x10_0000 + glfgreat_roz_rom_bank * 0x1_0000];
        }
    }

    public static void glfgreat_122000_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            K052109_set_RMRD_line((data & 0x10) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            if (glfgreat_roz_rom_bank != (data & 0x20) >> 5) {
                glfgreat_roz_rom_bank = (data & 0x20) >> 5;
                roz_tilemap.all_tiles_dirty = true;
            }
            glfgreat_roz_char_bank = (data & 0xc0) >> 6;
        }
        //if (ACCESSING_BITS_8_15)
        {
            glfgreat_roz_rom_mode = data & 0x100;
        }
    }

    public static void glfgreat_122000_w1(byte data) {
        glfgreat_roz_rom_mode = (data << 8) & 0x100;
    }

    public static void glfgreat_122000_w2(byte data) {
        //coin_counter_w(0,data & 0x01);
        //coin_counter_w(1,data & 0x02);
        K052109_set_RMRD_line((data & 0x10) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        if (glfgreat_roz_rom_bank != (data & 0x20) >> 5) {
            glfgreat_roz_rom_bank = (data & 0x20) >> 5;
            roz_tilemap.all_tiles_dirty = true;
        }
        glfgreat_roz_char_bank = (data & 0xc0) >> 6;
    }

    public static void ssriders_1c0300_w(int offset, short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            dim_v = (data & 0x70) >> 4;
        }
    }

    public static void ssriders_1c0300_w2(int offset, byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            K052109_set_RMRD_line((data & 0x08) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            dim_v = (data & 0x70) >> 4;
        }
    }

    public static void prmrsocr_122000_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            //coin_counter_w(0,data & 0x01);
            //coin_counter_w(1,data & 0x02);
            K052109_set_RMRD_line((data & 0x10) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
            prmrsocr_sprite_bank = (data & 0x40) >> 6;
            K053244_bankselect(0, prmrsocr_sprite_bank << 2);
            glfgreat_roz_char_bank = (data & 0x80) >> 7;
        }
    }

    public static void prmrsocr_122000_w2(byte data) {
        K052109_set_RMRD_line((data & 0x10) != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
        prmrsocr_sprite_bank = (data & 0x40) >> 6;
        K053244_bankselect(0, prmrsocr_sprite_bank << 2);
        glfgreat_roz_char_bank = (data & 0x80) >> 7;
    }

    public static short prmrsocr_rom_r(int offset) {
        short result;
        if (glfgreat_roz_char_bank != 0) {
            result = (short) (zoomrom[offset] * 0x100 + zoomrom[offset + 1]);// memory_region(machine, "zoom")[offset];
        } else {
            result = (short) (user1rom[offset] * 0x100 + user1rom[offset + 0x2_0000]);
        }
        return result;
    }

    public static byte prmrsocr_rom_r1(int offset) {
        byte result;
        if (glfgreat_roz_char_bank != 0) {
            result = zoomrom[offset];
        } else {
            result = user1rom[offset];
        }
        return result;
    }

    public static byte prmrsocr_rom_r2(int offset) {
        byte result;
        if (glfgreat_roz_char_bank != 0) {
            result = zoomrom[offset + 1];
        } else {
            result = user1rom[offset + 0x2_0000];
        }
        return result;
    }

    public static void tmnt_priority_w(short data) {
        //if (ACCESSING_BITS_0_7)
        {
            priorityflag = (data & 0x0c) >> 2;
        }
    }

    public static void tmnt_priority_w2(byte data) {
        priorityflag = (data & 0x0c) >> 2;
    }

    public static void swap(int[] layer, int[] pri, int a, int b) {
        if (pri[a] < pri[b]) {
            int t;
            t = pri[a];
            pri[a] = pri[b];
            pri[b] = t;
            t = layer[a];
            layer[a] = layer[b];
            layer[b] = t;
        }
    }

    public static void sortlayers(int[] layer, int[] pri) {
        swap(layer, pri, 0, 1);
        swap(layer, pri, 0, 2);
        swap(layer, pri, 1, 2);
    }

    public static void video_update_mia() {
        K052109_tilemap_update();
        K052109_tilemap[2].tilemap_draw_primask(Video.screenstate.visarea, 0, (byte) 0);
        if ((priorityflag & 1) == 1) {
            K051960_sprites_draw(Video.screenstate.visarea, 0, 0);
        }
        K052109_tilemap[1].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
        if ((priorityflag & 1) == 0) {
            K051960_sprites_draw(Video.screenstate.visarea, 0, 0);
        }
        K052109_tilemap[0].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
    }

    public static void video_update_punkshot() {
        int i;
        bg_colorbase = K053251_get_palette_index(0);
        sprite_colorbase = K053251_get_palette_index(1);
        layer_colorbase[0] = K053251_get_palette_index(2);
        layer_colorbase[1] = K053251_get_palette_index(4);
        layer_colorbase[2] = K053251_get_palette_index(3);
        K052109_tilemap_update();
        sorted_layer[0] = 0;
        layerpri[0] = K053251_get_priority(2);
        sorted_layer[1] = 1;
        layerpri[1] = K053251_get_priority(4);
        sorted_layer[2] = 2;
        layerpri[2] = K053251_get_priority(3);
        sortlayers(sorted_layer, layerpri);
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x4_0000, null);
        K052109_tilemap[sorted_layer[0]].tilemap_draw_primask(Video.screenstate.visarea, 0, (byte) 1);
        K052109_tilemap[sorted_layer[1]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 2);
        K052109_tilemap[sorted_layer[2]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 4);
        K051960_sprites_draw(Video.screenstate.visarea, -1, -1);
    }

    public static void video_update_lgtnfght() {
        int i;
        bg_colorbase = K053251_get_palette_index(0);
        sprite_colorbase = K053251_get_palette_index(1);
        layer_colorbase[0] = K053251_get_palette_index(2);
        layer_colorbase[1] = K053251_get_palette_index(4);
        layer_colorbase[2] = K053251_get_palette_index(3);
        K052109_tilemap_update();
        sorted_layer[0] = 0;
        layerpri[0] = K053251_get_priority(2);
        sorted_layer[1] = 1;
        layerpri[1] = K053251_get_priority(4);
        sorted_layer[2] = 2;
        layerpri[2] = K053251_get_priority(3);
        sortlayers(sorted_layer, layerpri);
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x4_0000, null);
        for (i = 0; i < 0x2_0000; i++) {
            Video.bitmapbase[Video.curbitmap][i] = (short) (16 * bg_colorbase);
        }
        K052109_tilemap[sorted_layer[0]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 1);
        K052109_tilemap[sorted_layer[1]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 2);
        K052109_tilemap[sorted_layer[2]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 4);
        K053245_sprites_draw(Video.screenstate.visarea);
    }

    public static short glfgreat_ball_r() {
        if (glfgreat_pixel < 0x400 || glfgreat_pixel >= 0x500) {
            return 0;
        } else {
            return (short) (glfgreat_pixel & 0xff);
        }
    }

    public static void video_update_glfgreat() {
        int i;
        K053251_set_tilemaps(null, null, K052109_tilemap[0], K052109_tilemap[1], K052109_tilemap[2]);
        bg_colorbase = K053251_get_palette_index(0);
        sprite_colorbase = K053251_get_palette_index(1);
        layer_colorbase[0] = K053251_get_palette_index(2);
        layer_colorbase[1] = K053251_get_palette_index(3) + 8;
        layer_colorbase[2] = K053251_get_palette_index(4);
        K052109_tilemap_update();
        sorted_layer[0] = 0;
        layerpri[0] = K053251_get_priority(2);
        sorted_layer[1] = 1;
        layerpri[1] = K053251_get_priority(3);
        sorted_layer[2] = 2;
        layerpri[2] = K053251_get_priority(4);
        sortlayers(sorted_layer, layerpri);
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x4_0000, null);
        for (i = 0; i < 0x2_0000; i++) {
            Video.bitmapbase[Video.curbitmap][i] = (short) (16 * bg_colorbase);
        }
        K052109_tilemap[sorted_layer[0]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 1);
        if (layerpri[0] >= 0x30 && layerpri[1] < 0x30) {
//            K053936_0_zoom_draw(Video.screenstate.visarea, roz_tilemap, 0, 1);
//            glfgreat_pixel = BITMAP_ADDR16(bitmap, 0x80, 0x105);
        }
        K052109_tilemap[sorted_layer[1]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 2);
        if (layerpri[1] >= 0x30 && layerpri[2] < 0x30) {
//            K053936_0_zoom_draw(Video.screenstate.visarea, roz_tilemap, 0, 1);
//            glfgreat_pixel = BITMAP_ADDR16(bitmap, 0x80, 0x105);
        }
        K052109_tilemap[sorted_layer[2]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 4);
        if (layerpri[2] >= 0x30) {
//            K053936_0_zoom_draw(Video.screenstate.visarea, roz_tilemap, 0, 1);
//            glfgreat_pixel = BITMAP_ADDR16(bitmap, 0x80, 0x105);
        }
        K053245_sprites_draw(Video.screenstate.visarea);
    }

    public static void video_update_tmnt2() {
        double brt;
        int i, newdim, newen, cb, ce;
        newdim = dim_v | ((~dim_c & 0x10) >> 1);
        newen = (K053251_get_priority(5) != 0 && K053251_get_priority(5) != 0x3e) ? 1 : 0;
        if (newdim != lastdim || newen != lasten) {
            brt = 1.0;
            if (newen != 0) {
                brt -= (1.0 - 0.6) * newdim / 8;
            }
            lastdim = newdim;
            lasten = newen;
            cb = layer_colorbase[sorted_layer[2]] << 4;
            ce = cb + 128;
//            for (i = 0; i < cb; i++) {
//                palette_set_brightness(screen -> machine, i, brt);
//            }
//            for (i = cb; i < ce; i++) {
//                palette_set_brightness(screen -> machine, i, 1.0);
//            }
//            for (i = ce; i < 2048; i++) {
//                palette_set_brightness(screen -> machine, i, brt);
//            }
//            if ((~dim_c & 0x10) != 0) {
//                palette_set_shadow_mode(screen -> machine, 1);
//            } else {
//                palette_set_shadow_mode(screen -> machine, 0);
//            }
        }
        video_update_lgtnfght();
    }

    public static void video_update_thndrx2() {
        int i;
        bg_colorbase = K053251_get_palette_index(0);
        sprite_colorbase = K053251_get_palette_index(1);
        layer_colorbase[0] = K053251_get_palette_index(2);
        layer_colorbase[1] = K053251_get_palette_index(4);
        layer_colorbase[2] = K053251_get_palette_index(3);
        K052109_tilemap_update();
        sorted_layer[0] = 0;
        layerpri[0] = K053251_get_priority(2);
        sorted_layer[1] = 1;
        layerpri[1] = K053251_get_priority(4);
        sorted_layer[2] = 2;
        layerpri[2] = K053251_get_priority(3);
        sortlayers(sorted_layer, layerpri);
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x4_0000, null);
        for (i = 0; i < 0x2_0000; i++) {
            Video.bitmapbase[Video.curbitmap][i] = (short) (16 * bg_colorbase);
        }
        K052109_tilemap[sorted_layer[0]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 1);
        K052109_tilemap[sorted_layer[1]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 2);
        K052109_tilemap[sorted_layer[2]].tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 4);
        K051960_sprites_draw(Video.screenstate.visarea, -1, -1);
    }

    public static void video_eof() {
    }

    public static void video_eof_blswhstl() {
        K053245_clear_buffer(0);
    }

//#endregion

//#region Drawgfx

    public static void common_drawgfxzoom_konami68000(byte[] bb1, int code, int color, int flipx, int flipy, int sx, int sy, RECT clip, int transparent_color, int scalex, int scaley) {
        if ((scalex == 0) || (scaley == 0)) {
            return;
        }
        if (scalex == 0x1_0000 && scaley == 0x1_0000) {
            common_drawgfx_konami68000(bb1, code, color, flipx, flipy, sx, sy, clip, 0);
            return;
        }
        RECT myclip = new RECT();
        myclip.min_x = clip.min_x;
        myclip.max_x = clip.max_x;
        myclip.min_y = clip.min_y;
        myclip.max_y = clip.max_y;
        if (myclip.min_x < 0)
            myclip.min_x = 0;
        if (myclip.max_x >= 0x200)
            myclip.max_x = 0x200 - 1;
        if (myclip.min_y < 0)
            myclip.min_y = 0;
        if (myclip.max_y >= 0x100)
            myclip.max_y = 0x100 - 1;
        int colorbase = 0x10 * (color % 0x80);
        int source_baseoffset = (code % sprite_totel_element) * 0x100;
        int sprite_screen_height = (scaley * 0x10 + 0x8000) >> 16;
        int sprite_screen_width = (scalex * 0x10 + 0x8000) >> 16;
        int countx, county, i, j, srcoffset, dstoffset;
        if (sprite_screen_width != 0 && sprite_screen_height != 0) {
            int dx = (0x10 << 16) / sprite_screen_width;
            int dy = (0x10 << 16) / sprite_screen_height;
            int ex = sx + sprite_screen_width;
            int ey = sy + sprite_screen_height;
            int x_index_base;
            int y_index;
            if (flipx != 0) {
                x_index_base = (sprite_screen_width - 1) * dx;
                dx = -dx;
            } else {
                x_index_base = 0;
            }
            if (flipy != 0) {
                y_index = (sprite_screen_height - 1) * dy;
                dy = -dy;
            } else {
                y_index = 0;
            }
            if (sx < myclip.min_x) {
                int pixels = myclip.min_x - sx;
                sx += pixels;
                x_index_base += pixels * dx;
            }
            if (sy < myclip.min_y) {
                int pixels = myclip.min_y - sy;
                sy += pixels;
                y_index += pixels * dy;
            }
            if (ex > myclip.max_x + 1) {
                int pixels = ex - myclip.max_x - 1;
                ex -= pixels;
            }
            if (ey > myclip.max_y + 1) {
                int pixels = ey - myclip.max_y - 1;
                ey -= pixels;
            }
            if (ex > sx) {
                countx = ex - sx;
                county = ey - sy;
                for (i = 0; i < county; i++) {
                    for (j = 0; j < countx; j++) {
                        int c;
                        srcoffset = ((y_index + dy * i) >> 16) * 0x10 + ((x_index_base + dx * j) >> 16);
                        dstoffset = (sy + i) * 0x200 + sx + j;
                        c = bb1[source_baseoffset + srcoffset];
                        if (c != transparent_color) {
                            Video.bitmapbase[Video.curbitmap][(sy + i) * 0x200 + sx + j] = (short) (colorbase + c);
                        }
                    }
                }
            }
        }
    }

    public static void common_drawgfxzoom_konami68000(byte[] bb1, int code, int color, int flipx, int flipy, int sx, int sy, RECT clip, int transparent_color, int scalex, int scaley, int pri_mask) {
        if ((scalex == 0) || (scaley == 0)) {
            return;
        }
        if (scalex == 0x1_0000 && scaley == 0x1_0000) {
            common_drawgfx_konami68000(bb1, code, color, flipx, flipy, sx, sy, clip, pri_mask);
            return;
        }
        RECT myclip = new RECT();
        myclip.min_x = clip.min_x;
        myclip.max_x = clip.max_x;
        myclip.min_y = clip.min_y;
        myclip.max_y = clip.max_y;
        if (myclip.min_x < 0)
            myclip.min_x = 0;
        if (myclip.max_x >= 0x200)
            myclip.max_x = 0x200 - 1;
        if (myclip.min_y < 0)
            myclip.min_y = 0;
        if (myclip.max_y >= 0x100)
            myclip.max_y = 0x100 - 1;
        int colorbase = 0x10 * (color % 0x80);
        int source_baseoffset = (code % sprite_totel_element) * 0x100;
        int sprite_screen_height = (scaley * 0x10 + 0x8000) >> 16;
        int sprite_screen_width = (scalex * 0x10 + 0x8000) >> 16;
        int countx, county, i, j, srcoffset, dstoffset;
        if (sprite_screen_width != 0 && sprite_screen_height != 0) {
            int dx = (0x10 << 16) / sprite_screen_width;
            int dy = (0x10 << 16) / sprite_screen_height;
            int ex = sx + sprite_screen_width;
            int ey = sy + sprite_screen_height;
            int x_index_base;
            int y_index;
            if (flipx != 0) {
                x_index_base = (sprite_screen_width - 1) * dx;
                dx = -dx;
            } else {
                x_index_base = 0;
            }
            if (flipy != 0) {
                y_index = (sprite_screen_height - 1) * dy;
                dy = -dy;
            } else {
                y_index = 0;
            }
            if (sx < myclip.min_x) {
                int pixels = myclip.min_x - sx;
                sx += pixels;
                x_index_base += pixels * dx;
            }
            if (sy < myclip.min_y) {
                int pixels = myclip.min_y - sy;
                sy += pixels;
                y_index += pixels * dy;
            }
            if (ex > myclip.max_x + 1) {
                int pixels = ex - myclip.max_x - 1;
                ex -= pixels;
            }
            if (ey > myclip.max_y + 1) {
                int pixels = ey - myclip.max_y - 1;
                ey -= pixels;
            }
            if (ex > sx) {
                countx = ex - sx;
                county = ey - sy;
                for (i = 0; i < county; i++) {
                    for (j = 0; j < countx; j++) {
                        int c;
                        srcoffset = ((y_index + dy * i) >> 16) * 0x10 + ((x_index_base + dx * j) >> 16);
                        dstoffset = (sy + i) * 0x200 + sx + j;
                        c = bb1[source_baseoffset + srcoffset];
                        if (c != transparent_color) {
                            if (((1 << Tilemap.priority_bitmap[sy + i][sx + j]) &pri_mask) ==0)
                            {
                                Video.bitmapbase[Video.curbitmap][(sy + i) * 0x200 + sx + j] = (short) (colorbase + c);
                            }
                            Tilemap.priority_bitmap[sy + i][sx + j] =0x1f;
                        }
                    }
                }
            }
        }
    }

    public static void common_drawgfx_konami68000(byte[] bb1, int code, int color, int flipx, int flipy, int sx, int sy, RECT clip, int pri_mask) {
        int ox;
        int oy;
        int ex;
        int ey;
        ox = sx;
        oy = sy;
        ex = sx + 0x10 - 1;
        if (code > 0x4000 || color > 0x80) {
            int i1 = 1;
        }
        if (sx < 0) {
            sx = 0;
        }
        if (sx < clip.min_x) {
            sx = clip.min_x;
        }
        if (ex >= 0x200) {
            ex = 0x200 - 1;
        }
        if (ex > clip.max_x) {
            ex = clip.max_x;
        }
        if (sx > ex) {
            return;
        }
        ey = sy + 0x10 - 1;
        if (sy < 0) {
            sy = 0;
        }
        if (sy < clip.min_y) {
            sy = clip.min_y;
        }
        if (ey >= 0x100) {
            ey = 0x100 - 1;
        }
        if (ey > clip.max_y) {
            ey = clip.max_y;
        }
        if (sy > ey) {
            return;
        }
        int sw = 0x10;
        int sh = 0x10;
        int ls = sx - ox;
        int ts = sy - oy;
        int dw = ex - sx + 1;
        int dh = ey - sy + 1;
        int colorbase = color * 0x10;
        blockmove_8toN_transpen_pri16_konami68000(bb1, code, sw, sh, 0x10, ls, ts, flipx, flipy, dw, dh, colorbase, pri_mask, sx, sy);
    }

    public static void blockmove_8toN_transpen_pri16_konami68000(byte[] bb1, int code, int srcwidth, int srcheight, int srcmodulo,
                                                                 int leftskip, int topskip, int flipx, int flipy,
                                                                 int dstwidth, int dstheight, int colorbase, int pmask, int sx, int sy) {
        int ydir, xdir, col, i, j, offsetx, offsety;
        int srcdata_offset = code * 0x100;
        offsetx = sx;
        offsety = sy;
        if (flipy != 0) {
            offsety += (dstheight - 1);
            srcdata_offset += (srcheight - dstheight - topskip) * srcmodulo;
            ydir = -1;
        } else {
            srcdata_offset += topskip * srcmodulo;
            ydir = 1;
        }
        if (flipx != 0) {
            offsetx += (dstwidth - 1);
            srcdata_offset += (srcwidth - dstwidth - leftskip);
            xdir = -1;
        } else {
            srcdata_offset += leftskip;
            xdir = 1;
        }
        for (i = 0; i < dstheight; i++) {
            for (j = 0; j < dstwidth; j++) {
                col = bb1[srcdata_offset + srcmodulo * i + j];
                if (col != 0) {
                    if (((1 << (Tilemap.priority_bitmap[offsety + ydir * i][offsetx + xdir * j] &0x1f)) &pmask) ==0)
                    {
                        if ((Tilemap.priority_bitmap[offsety + ydir * i][offsetx + xdir * j] &0x80)!=0)
                        {
                            Video.bitmapbase[Video.curbitmap][(offsety + ydir * i) * 0x200 + offsetx + xdir * j] = (short) (colorbase + col); // palette_shadow_table[paldata[col]];
                        }
                        else
                        {
                            Video.bitmapbase[Video.curbitmap][(offsety + ydir * i) * 0x200 + offsetx + xdir * j] = (short) (colorbase + col);
                        }
                    }
                    Tilemap.priority_bitmap[offsety + ydir * i][offsetx + xdir * j] =
                            (byte) ((Tilemap.priority_bitmap[offsety + ydir * i][offsetx + xdir * j] &0x7f) |0x1f);
                }
            }
        }
    }

//#endregion

//#region Tilemap

    public static Tmap[] K052109_tilemap, K053251_tilemaps;

    public static void tilemap_init() {

    }

//#endregion
}
