/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.namcos1;


import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import m1.Properties.Resources;
import m1.cpu.m6800.M6800;
import m1.cpu.m6809.M6809;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Generic;
import m1.emu.Inptport;
import m1.emu.Inptport.FrameArray;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.Palette;
import m1.emu.RomInfo;
import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Timer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.DAC;
import m1.sound.Namco;
import m1.sound.Sound;
import m1.sound.YM2151;


public class Namcos1 {

    public static int dac0_value, dac1_value, dac0_gain, dac1_gain;
    public static byte[] gfx1rom, gfx2rom, gfx3rom, user1rom, mcurom;
    public static byte[] audiorom, voicerom, bank_ram20, bank_ram30;
    public static int namcos1_pri;
    public static byte dipsw;

    public static byte[] ByteTo2byte(byte[] bb1) {
        byte[] bb2 = null;
        int i1, n1;
        if (bb1 != null) {
            n1 = bb1.length;
            bb2 = new byte[n1 * 2];
            for (i1 = 0; i1 < n1; i1++) {
                bb2[i1 * 2] = (byte) (bb1[i1] >> 4);
                bb2[i1 * 2 + 1] = (byte) (bb1[i1] & 0x0f);
            }
        }
        return bb2;
    }

    public static void Namcos1Init() {
        Machine.bRom = true;
        user1rom_offset = new int[2][8];
        audiorom = Machine.GetRom("audiocpu.rom");
        gfx1rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
        gfx2rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
        gfx3rom = new byte[0x1_0000]; // ByteTo2byte(Machine.GetRom("gfx3.rom"));
        user1rom = new byte[0x40_0000]; // Machine.GetRom("user1.rom");
        mcurom = Resources.mcu;
        voicerom = new byte[0xc_0000];
        byte[] bb1 = Machine.GetRom("voice.rom");
        System.arraycopy(bb1, 0, voicerom, 0, bb1.length);
        bank_ram20 = new byte[0x2000];
        bank_ram30 = new byte[0x80];
        Namco.namco_wavedata = new byte[0x400];
        Generic.generic_nvram = new byte[0x800];
        cus117_offset = new int[2][8];
        key = new byte[8];
        if (audiorom == null || gfx1rom == null || gfx2rom == null || gfx3rom == null || user1rom == null || voicerom == null) {
            Machine.bRom = false;
        }
        if (Machine.bRom) {
            switch (Machine.sName) {
                case "quester":
                case "questers":
                    dipsw = (byte) 0xfb;
                    break;
                default:
                    dipsw = (byte) 0xff;
                    break;
            }
        }
    }

    public static void namcos1_sub_firq_w() {
        Cpuint.cpunum_set_input_line(1, 1, LineState.ASSERT_LINE);
    }

    public static void irq_ack_w(int cpunum) {
        Cpuint.cpunum_set_input_line(cpunum, 0, LineState.CLEAR_LINE);
    }

    public static void firq_ack_w(int cpunum) {
        Cpuint.cpunum_set_input_line(cpunum, 1, LineState.CLEAR_LINE);
    }

    public static byte dsw_r(int offset) {
        int ret = dipsw; // 0xff; // input_port_read(machine, "DIPSW");
        if ((offset & 2) == 0) {
            ret >>= 4;
        }
        return (byte) (0xf0 | ret);
    }

    public static void namcos1_coin_w(byte data) {
        Generic.coin_lockout_global_w(~data & 1);
        Generic.coin_counter_w(0, data & 2);
        Generic.coin_counter_w(1, data & 4);
    }

    public static void namcos1_update_DACs() {
        DAC.dac_signed_data_16_w(0, (short) (0x8000 + (dac0_value * dac0_gain) + (dac1_value * dac1_gain)));
    }

    public static void namcos1_init_DACs() {
        dac0_value = 0;
        dac1_value = 0;
        dac0_gain = 0x80;
        dac1_gain = 0x80;
    }

    public static void namcos1_dac_gain_w(byte data) {
        int value;
        value = (data & 1) | ((data >> 1) & 2);
        dac0_gain = 0x20 * (value + 1);
        value = (data >> 3) & 3;
        dac1_gain = 0x20 * (value + 1);
        namcos1_update_DACs();
    }

    public static void namcos1_dac0_w(byte data) {
        dac0_value = data - 0x80;
        namcos1_update_DACs();
    }

    public static void namcos1_dac1_w(byte data) {
        dac1_value = data - 0x80;
        namcos1_update_DACs();
    }

    public static void nvram_handler_load_namcos1() {
        if (Files.exists(Path.of("nvram",Machine.sName + ".nv"))) {
            FileStream fs1 = new FileStream(Path.of("nvram", Machine.sName + ".nv").toString(), FileMode.Open);
            int n = (int) fs1.getLength();
            fs1.read(Generic.generic_nvram, 0, n);
            fs1.close();
        } else {
            Arrays.fill(Generic.generic_nvram, 0, 0x800, (byte) 0);
        }
    }

    public static void nvram_handler_save_namcos1() {
        FileStream fs1 = new FileStream(Path.of("nvram", Machine.sName + ".nv").toString(), FileMode.Create);
        fs1.write(Generic.generic_nvram, 0, 0x800);
        fs1.close();
    }

    public static void play_namcos1_default(TrackInfo ti) {
        if (ti.TrackID >= 0x100) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x140 + ti.TrackID, (byte) 0x01));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x1100, (byte) ti.TrackID));
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x1101, (byte) 0x40));
        }
    }

    public static void stop_namcos1_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x1100, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x1101, (byte) 0x40));
    }

    public static void stopandplay_namcos1_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x1100, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x1101, (byte) 0x40));
        if (ti.TrackID >= 0x100) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0x140 + ti.TrackID, (byte) 0x01));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x1100, (byte) ti.TrackID));
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0x1101, (byte) 0x40));
        }
    }

//#region Machine

    public static byte[] namcos1_paletteram;
    public static byte[] s1ram, namcos1_triram;
    public static int audiocpurom_offset, mcurom_offset;
    public static int key_id, key_reg, key_rng, key_swap4_arg, key_swap4, key_bottom4, key_top4;
    public static int key_quotient, key_reminder, key_numerator_high_word;
    public static byte[] key;
    public static int mcu_patch_data;
    public static int namcos1_reset = 0;
    public static int wdog;
    public static int[][] cus117_offset;
    public static int[][] user1rom_offset;
    public static Function<Integer, Byte> key_r;
    public static BiConsumer<Integer, Byte> key_w;

    public static class namcos1_specific {

        public final int key_id;
        public final int key_reg1;
        public final int key_reg2;
        public final int key_reg3;
        public final int key_reg4;
        public final int key_reg5;
        public final int key_reg6;

        public namcos1_specific(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            key_id = i1;
            key_reg1 = i2;
            key_reg2 = i3;
            key_reg3 = i4;
            key_reg4 = i5;
            key_reg5 = i6;
            key_reg6 = i7;
        }
    }

    public static void namcos1_3dcs_w(int offset) {
        if ((offset & 1) != 0) {
            //popmessage("LEFT");
        } else {
            //popmessage("RIGHT");
        }
    }

    public static byte no_key_r(int offset) {
        return 0;
    }

    public static void no_key_w(int offset, byte data) {

    }

    public static byte key_type1_r(int offset) {
        if (offset < 3) {
            int d = key[0];
            int n = (key[1] << 8) | key[2];
            int q, r;
            if (d != 0) {
                q = n / d;
                r = n % d;
            } else {
                q = 0xffff;
                r = 0x00;
            }
            if (offset == 0)
                return (byte) r;
            if (offset == 1)
                return (byte) (q >> 8);
            if (offset == 2)
                return (byte) (q & 0xff);
        } else if (offset == 3)
            return (byte) key_id;
        return 0;
    }

    public static void key_type1_w(int offset, byte data) {
        if (offset < 4)
            key[offset] = data;
    }

    public static byte key_type2_r(int offset) {
        key_numerator_high_word = 0;
        if (offset < 4) {
            if (offset == 0)
                return (byte) (key_reminder >> 8);
            if (offset == 1)
                return (byte) (key_reminder & 0xff);
            if (offset == 2)
                return (byte) (key_quotient >> 8);
            if (offset == 3)
                return (byte) (key_quotient & 0xff);
        } else if (offset == 4)
            return (byte) key_id;
        return 0;
    }

    public static void key_type2_w(int offset, byte data) {
        if (offset < 5) {
            key[offset] = data;
            if (offset == 3) {
                int d = (key[0] << 8) | key[1];
                int n = (key_numerator_high_word << 16) | (key[2] << 8) | key[3];
                if (d != 0) {
                    key_quotient = n / d;
                    key_reminder = n % d;
                } else {
                    key_quotient = 0xffff;
                    key_reminder = 0x0000;
                }
                key_numerator_high_word = (key[2] << 8) | key[3];
            }
        }
    }

    public static byte key_type3_r(int offset) {
        int op;
        op = (offset & 0x70) >> 4;
        if (op == key_reg)
            return (byte) key_id;
        if (op == key_rng)
            return 0;// (byte)mame_rand(machine);
        if (op == key_swap4)
            return (byte) ((key[key_swap4_arg] << 4) | (key[key_swap4_arg] >> 4));
        if (op == key_bottom4)
            return (byte) ((offset << 4) | (key[key_swap4_arg] & 0x0f));
        if (op == key_top4)
            return (byte) ((offset << 4) | (key[key_swap4_arg] >> 4));
        return 0;
    }

    public static void key_type3_w(int offset, byte data) {
        key[(offset & 0x70) >> 4] = data;
    }

    public static void namcos1_sound_bankswitch_w(byte data) {
        int bank = (data & 0x70) >> 4;
        audiocpurom_offset = 0x4000 * bank;
    }

    public static void namcos1_cpu_control_w(byte data) {
        if (((data & 1) ^ namcos1_reset) != 0) {
            mcu_patch_data = 0;
            namcos1_reset = data & 1;
        }
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), ((data & 1) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Cpuint.cpunum_set_input_line(2, LineState.INPUT_LINE_RESET.ordinal(), ((data & 1) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
        Cpuint.cpunum_set_input_line(3, LineState.INPUT_LINE_RESET.ordinal(), ((data & 1) != 0) ? LineState.CLEAR_LINE : LineState.ASSERT_LINE);
    }

    public static void namcos1_watchdog_w() {
        wdog |= 1 << Cpuexec.activecpu;
        if (wdog == 7 || (namcos1_reset == 0)) {
            wdog = 0;
            Generic.watchdog_reset_w();
        }
    }

    public static byte soundram_r(int offset) {
        if (offset < 0x1000) {
            offset &= 0x3ff;
            return Namco.namcos1_cus30_r(offset);
        } else {
            offset &= 0x7ff;
            return namcos1_triram[offset];
        }
    }

    public static void soundram_w(int offset, byte data) {
        if (offset < 0x1000) {
            offset &= 0x3ff;
            Namco.namcos1_cus30_w(offset, data);
        } else {
            offset &= 0x7ff;
            namcos1_triram[offset] = data;
        }
    }

    public static void namcos1_bankswitch(int cpu, int offset, byte data) {
        int reg = (offset >> 9) & 0x7;
        if ((offset & 1) != 0) {
            cus117_offset[cpu][reg] = (cus117_offset[cpu][reg] & 0x60_0000) | (data * 0x2000);
        } else {
            cus117_offset[cpu][reg] = (cus117_offset[cpu][reg] & 0x1f_e000) | ((data & 0x03) * 0x20_0000);
        }
        if (cus117_offset[cpu][reg] >= 0x40_0000 && cus117_offset[cpu][reg] <= 0x7f_ffff) {
            user1rom_offset[cpu][reg] = cus117_offset[cpu][reg] - 0x40_0000;
        }
    }

    public static void namcos1_bankswitch_w(int offset, byte data) {
        namcos1_bankswitch(Cpuexec.activecpu, offset, data);
    }

    public static void namcos1_subcpu_bank_w(byte data) {
        cus117_offset[1][7] = 0x60_0000 | (data * 0x2000);
        user1rom_offset[1][7] = cus117_offset[1][7] - 0x40_0000;
    }

    public static void machine_reset_namcos1() {
        cus117_offset[0][0] = 0x0180 * 0x2000;
        cus117_offset[0][1] = 0x0180 * 0x2000;
        cus117_offset[0][7] = 0x03ff * 0x2000;
        cus117_offset[1][0] = 0x0180 * 0x2000;
        cus117_offset[1][7] = 0x03ff * 0x2000;
        user1rom_offset[0][7] = cus117_offset[0][7] - 0x40_0000;
        user1rom_offset[1][7] = cus117_offset[1][7] - 0x40_0000;
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_RESET.ordinal(), LineState.ASSERT_LINE);
        Cpuint.cpunum_set_input_line(2, LineState.INPUT_LINE_RESET.ordinal(), LineState.ASSERT_LINE);
        Cpuint.cpunum_set_input_line(3, LineState.INPUT_LINE_RESET.ordinal(), LineState.ASSERT_LINE);
        mcu_patch_data = 0;
        namcos1_reset = 0;
        namcos1_init_DACs();
        int i, j;
        for (i = 0; i < 8; i++) {
            key[i] = 0;
        }
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 8; j++) {
                cus117_offset[i][j] =0;
            }
        }
        wdog = 0;
    }

    public static void namcos1_mcu_bankswitch_w(byte data) {
        int addr;
        switch (data & 0xfc) {
            case 0xf8:
                addr = 0x0_0000;
                data ^= 2;
                break;
            case 0xf4:
                addr = 0x2_0000;
                break;
            case 0xec:
                addr = 0x4_0000;
                break;
            case 0xdc:
                addr = 0x6_0000;
                break;
            case 0xbc:
                addr = 0x8_0000;
                break;
            case 0x7c:
                addr = 0xa_0000;
                break;
            default:
                addr = 0x0_0000;
                break;
        }
        addr += (data & 3) * 0x8000;
        mcurom_offset = addr;
    }

    public static void namcos1_mcu_patch_w(byte data) {
        if (mcu_patch_data == 0xa6)
            return;
        mcu_patch_data = data;
        namcos1_triram[0] = data;
    }

    public static void namcos1_driver_init(namcos1_specific specific) {
        key_id = specific.key_id;
        key_reg = specific.key_reg1;
        key_rng = specific.key_reg2;
        key_swap4_arg = specific.key_reg3;
        key_swap4 = specific.key_reg4;
        key_bottom4 = specific.key_reg5;
        key_top4 = specific.key_reg6;
        s1ram = new byte[0x8000];
        namcos1_triram = new byte[0x800];
        namcos1_paletteram = new byte[0x8000];
    }

    public static void driver_init() {
        switch (Machine.sName) {
            case "shadowld":
            case "youkaidk2":
            case "youkaidk1":
                key_r = Namcos1::no_key_r;
                key_w = Namcos1::no_key_w;
                namcos1_driver_init(new namcos1_specific(0, 0, 0, 0, 0, 0, 0));
                break;
            case "dspirit":
            case "dspirit2":
            case "dspirit1":
                key_r = Namcos1::key_type1_r;
                key_w = Namcos1::key_type1_w;
                namcos1_driver_init(new namcos1_specific(0x36, 0, 0, 0, 0, 0, 0));
                break;
            case "blazer":
                key_r = Namcos1::key_type1_r;
                key_w = Namcos1::key_type1_w;
                namcos1_driver_init(new namcos1_specific(0x13, 0, 0, 0, 0, 0, 0));
                break;
            case "quester":
            case "questers":
                key_r = Namcos1::no_key_r;
                key_w = Namcos1::no_key_w;
                namcos1_driver_init(new namcos1_specific(0, 0, 0, 0, 0, 0, 0));
                //quester_paddle_r
                break;
            case "pacmania":
            case "pacmaniao":
            case "pacmaniaj":
                key_r = Namcos1::key_type2_r;
                key_w = Namcos1::key_type2_w;
                namcos1_driver_init(new namcos1_specific(0x12, 0, 0, 0, 0, 0, 0));
                break;
            case "galaga88":
            case "galaga88a":
            case "galaga88j":
                key_r = Namcos1::key_type2_r;
                key_w = Namcos1::key_type2_w;
                namcos1_driver_init(new namcos1_specific(0x31, 0, 0, 0, 0, 0, 0));
                break;
            case "ws":
                key_r = Namcos1::key_type2_r;
                key_w = Namcos1::key_type2_w;
                namcos1_driver_init(new namcos1_specific(0x07, 0, 0, 0, 0, 0, 0));
                break;
            case "berabohm":
            case "berabohmb":
                key_r = Namcos1::no_key_r;
                key_w = Namcos1::no_key_w;
                namcos1_driver_init(new namcos1_specific(0, 0, 0, 0, 0, 0, 0));
                //berabohm_buttons_r
                break;
            case "mmaze":
                key_r = Namcos1::key_type2_r;
                key_w = Namcos1::key_type2_w;
                namcos1_driver_init(new namcos1_specific(0x25, 0, 0, 0, 0, 0, 0));
                break;
            case "bakutotu":
                key_r = Namcos1::key_type2_r;
                key_w = Namcos1::key_type2_w;
                namcos1_driver_init(new namcos1_specific(0x22, 0, 0, 0, 0, 0, 0));
                break;
            case "wldcourt":
                key_r = Namcos1::key_type1_r;
                key_w = Namcos1::key_type1_w;
                namcos1_driver_init(new namcos1_specific(0x35, 0, 0, 0, 0, 0, 0));
                break;
            case "splatter":
            case "splatter2":
            case "splatterj":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(181, 3, 4, -1, -1, -1, -1));
                break;
            case "faceoff":
                key_r = Namcos1::no_key_r;
                key_w = Namcos1::no_key_w;
                namcos1_driver_init(new namcos1_specific(0, 0, 0, 0, 0, 0, 0));
                //faceoff_inputs_r
                break;
            case "rompers":
            case "romperso":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(182, 7, -1, -1, -1, -1, -1));
                break;
            case "blastoff":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(183, 0, 7, 3, 5, -1, -1));
                break;
            case "ws89":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(184, 2, -1, -1, -1, -1, -1));
                break;
            case "dangseed":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(308, 6, -1, 5, -1, 0, 4));
                break;
            case "ws90":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(310, 4, -1, 7, -1, 3, -1));
                break;
            case "pistoldm":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(309, 1, 2, 0, -1, 4, -1));
                break;
            case "boxyboy":
            case "soukobdx":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(311, 2, 3, 0, -1, 4, -1));
                break;
            case "puzlclub":
                key_r = Namcos1::key_type1_r;
                key_w = Namcos1::key_type1_w;
                namcos1_driver_init(new namcos1_specific(0x35, 0, 0, 0, 0, 0, 0));
                break;
            case "tankfrce":
            case "tankfrcej":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(185, 5, -1, 1, -1, 2, -1));
                break;
            case "tankfrce4":
                key_r = Namcos1::key_type3_r;
                key_w = Namcos1::key_type3_w;
                namcos1_driver_init(new namcos1_specific(185, 5, -1, 1, -1, 2, -1));
                //tankfrc4_input_r
                break;
        }
    }

//#endregion

//#region Memory

    public static byte byte0, byte1, byte2, byte00, byte01, byte02, byte03;
    public static byte byte0_old, byte1_old, byte2_old;
    public static byte strobe;
    public static int input_count, strobe_count;
    public static int stored_input0, stored_input1;

    public static byte N0ReadOpByte(short address) {
        int reg, offset;
        byte result;
        if (address >= 0x0000 && address <= 0xffff) {
            reg = address / 0x2000;
            offset = address & 0x1fff;
            result = user1rom[user1rom_offset[0][reg] + offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N1ReadOpByte(short address) {
        int reg, offset;
        byte result;
        if (address >= 0x0000 && address <= 0xffff) {
            reg = address / 0x2000;
            offset = address & 0x1fff;
            result = user1rom[user1rom_offset[1][reg] + offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N2ReadOpByte(short address) {
        int offset;
        byte result = 0;
        if (address >= 0xc000 && address <= 0xffff) {
            offset = address & 0x3fff;
            result = audiorom[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N3ReadOpByte(short address) {
        int offset;
        byte result;
        if (address >= 0x4000 && address <= 0xbfff) {
            offset = address - 0x4000;
            result = voicerom[mcurom_offset + offset];
        } else if (address >= 0xf000 && address <= 0xffff) {
            offset = address & 0xfff;
            result = mcurom[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N0ReadMemory(short address) {
        byte result;
        int offset;
        int reg;
        reg = address / 0x2000;
        offset = address & 0x1fff;
        if (cus117_offset[0][reg] == 0) {
            result = user1rom[user1rom_offset[0][reg] + offset];
        } else if (cus117_offset[0][reg] >= 0x2e_0000 && cus117_offset[0][reg] <= 0x2e_7fff) {
            result = namcos1_paletteram[cus117_offset[0][reg] - 0x2e_0000 + offset];
        } else if (cus117_offset[0][reg] >= 0x2f_0000 && cus117_offset[0][reg] <= 0x2f_7fff) {
            result = namcos1_videoram_r(cus117_offset[0][reg] - 0x2f_0000 + offset);
        } else if (cus117_offset[0][reg] == 0x2f_8000) {
            result = key_r.apply(offset);
        } else if (cus117_offset[0][reg] == 0x2f_c000) {
            result = namcos1_spriteram_r(offset);
        } else if (cus117_offset[0][reg] == 0x2f_e000) {
            result = soundram_r(offset);
        } else if (cus117_offset[0][reg] >= 0x30_0000 && cus117_offset[0][reg] <= 0x30_7fff) {
            result = s1ram[cus117_offset[0][reg] - 0x30_0000 + offset];
        } else if (cus117_offset[0][reg] >= 0x40_0000 && cus117_offset[0][reg] <= 0x7f_ffff) {
            result = user1rom[cus117_offset[0][reg] - 0x40_0000 + offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N1ReadMemory(short address) {
        byte result;
        int offset;
        int reg;
        reg = address / 0x2000;
        offset = address & 0x1fff;
        if (cus117_offset[1][reg] >= 0x2e_0000 && cus117_offset[1][reg] <= 0x2e_7fff) {
            result = namcos1_paletteram[cus117_offset[1][reg] - 0x2e_0000 + offset];
        } else if (cus117_offset[1][reg] >= 0x2f_0000 && cus117_offset[1][reg] <= 0x2f_7fff) {
            result = namcos1_videoram_r(cus117_offset[1][reg] - 0x2f_0000 + offset);
        } else if (cus117_offset[1][reg] == 0x2f_8000) {
            result = key_r.apply(offset);
        } else if (cus117_offset[1][reg] == 0x2f_c000) {
            result = namcos1_spriteram_r(offset);
        } else if (cus117_offset[1][reg] == 0x2f_e000) {
            result = soundram_r(offset);
        } else if (cus117_offset[1][reg] >= 0x30_0000 && cus117_offset[1][reg] <= 0x30_7fff) {
            result = s1ram[cus117_offset[1][reg] - 0x30_0000 + offset];
        } else if (cus117_offset[1][reg] >= 0x40_0000 && cus117_offset[1][reg] <= 0x7f_ffff) {
            result = user1rom[cus117_offset[1][reg] - 0x40_0000 + offset];
        } else if (cus117_offset[0][reg] == 0) {
            result = user1rom[user1rom_offset[1][reg] + offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N2ReadMemory(short address) {
        byte result;
        int offset;
        if (address >= 0x0000 && address <= 0x3fff) {
            offset = address & 0x3fff;
            result = audiorom[audiocpurom_offset + offset];
        } else if (address >= 0x4000 && address <= 0x4001) {
            result = YM2151.ym2151_status_port_0_r();
        } else if (address >= 0x5000 && address <= 0x53ff) {
            offset = address & 0x3ff;
            result = Namco.namcos1_cus30_r(offset);
        } else if (address >= 0x7000 && address <= 0x77ff) {
            offset = address & 0x7ff;
            result = namcos1_triram[offset];
        } else if (address >= 0x8000 && address <= 0x9fff) {
            offset = address & 0x1fff;
            result = bank_ram20[offset];
        } else if (address >= 0xc000 && address <= 0xffff) {
            offset = address & 0x3fff;
            result = audiorom[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N3ReadMemory(short address) {
        byte result;
        int offset;
        if (address >= 0x0000 && address <= 0x001f) {
            offset = address & 0x1f;
            result = M6800.m1.hd63701_internal_registers_r(offset);
        } else if (address >= 0x0080 && address <= 0x00ff) {
            offset = address & 0x7f;
            result = bank_ram30[offset];
        } else if (address >= 0x1000 && address <= 0x1003) {
            offset = address & 0x03;
            result = dsw_r(offset);
        } else if (address == 0x1400) {
            result = byte0;
        } else if (address == 0x1401) {
            result = byte1;
        } else if (address >= 0x4000 && address <= 0xbfff) {
            offset = address - 0x4000;
            result = voicerom[mcurom_offset + offset];
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            offset = address & 0x7ff;
            result = namcos1_triram[offset];
        } else if (address >= 0xc800 && address <= 0xcfff) {
            offset = address & 0x7ff;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0xf000 && address <= 0xffff) {
            offset = address & 0xfff;
            result = mcurom[offset];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte N3ReadIO(short address) {
        byte result;
        if (address == 0x100) {
            result = byte2;
        } else if (address == 0x101) {
            result = 0;
        } else {
            result = 0;
        }
        return result;
    }

    public static void N0WriteMemory(short address, byte data) {
        int offset;
        int reg;
        reg = address / 0x2000;
        offset = address & 0x1fff;
        if (cus117_offset[0][reg] == 0) {
            if (address >= 0x0000 && address <= 0xdfff) {
                user1rom[user1rom_offset[0][reg] + offset] = data;
            }
            if (address >= 0xe000 && address <= 0xefff) {
                namcos1_bankswitch_w(offset, data);
            } else if (address == 0xf000) {
                namcos1_cpu_control_w(data);
            } else if (address == 0xf200) {
                namcos1_watchdog_w();
            } else if (address == 0xf600) {
                irq_ack_w(0);
            } else if (address == 0xf800) {
                firq_ack_w(0);
            } else if (address == 0xfa00) {
                namcos1_sub_firq_w();
            } else if (address >= 0xfc00 && address <= 0xfc01) {
                namcos1_subcpu_bank_w(data);
            } else {
                int i1 = 1;
            }
        } else if (cus117_offset[0][reg] == 0x2c_0000) {
            namcos1_3dcs_w(offset);
        } else if (cus117_offset[0][reg] >= 0x2e_0000 && cus117_offset[0][reg] <= 0x2e_7fff) {
            namcos1_paletteram_w(cus117_offset[0][reg] - 0x2e_0000 + offset, data);
        } else if (cus117_offset[0][reg] >= 0x2f_0000 && cus117_offset[0][reg] <= 0x2f_7fff) {
            namcos1_videoram_w(cus117_offset[0][reg] - 0x2f_0000 + offset, data);
        } else if (cus117_offset[0][reg] == 0x2f_8000) {
            key_w.accept(offset, data);
        } else if (cus117_offset[0][reg] == 0x2f_c000) {
            namcos1_spriteram_w(offset, data);
        } else if (cus117_offset[0][reg] == 0x2f_e000) {
            soundram_w(offset, data);
        } else if (cus117_offset[0][reg] >= 0x30_0000 && cus117_offset[0][reg] <= 0x30_7fff) {
            s1ram[cus117_offset[0][reg] - 0x30_0000 + offset] = data;
        } else {
            int i1 = 1;
        }
    }

    public static void N1WriteMemory(short address, byte data) {
        int offset;
        int reg;
        reg = address / 0x2000;
        offset = address & 0x1fff;
        if (cus117_offset[1][reg] == 0x2c_0000) {
            namcos1_3dcs_w(offset);
        } else if (cus117_offset[1][reg] >= 0x2e_0000 && cus117_offset[1][reg] <= 0x2e_7fff) {
            namcos1_paletteram_w(cus117_offset[1][reg]-0x2e_0000 + offset, data);
        } else if (cus117_offset[1][reg] >= 0x2f_0000 && cus117_offset[1][reg] <= 0x2f_7fff) {
            namcos1_videoram_w(cus117_offset[1][reg] - 0x2f_0000 + offset, data);
        } else if (cus117_offset[1][reg] == 0x2f_8000) {
            key_w.accept(offset, data);
        } else if (cus117_offset[1][reg] == 0x2f_c000) {
            namcos1_spriteram_w(offset, data);
        } else if (cus117_offset[1][reg] == 0x2f_e000) {
            soundram_w(offset, data);
        } else if (cus117_offset[1][reg] >= 0x30_0000 && cus117_offset[1][reg] <= 0x30_7fff) {
            s1ram[cus117_offset[1][reg] - 0x30_0000 + offset] = data;
        } else if (cus117_offset[0][reg] == 0) {
            if (address >= 0x0000 && address <= 0xdfff) {
                user1rom[user1rom_offset[1][reg] + offset] = data;
            } else if (address >= 0xe000 && address <= 0xefff) {
                namcos1_bankswitch_w(offset, data);
            } else if (address == 0xf000) {
                int i1 = 1;
            } else if (address == 0xf200) {
                namcos1_watchdog_w();
            } else if (address == 0xf600) {
                irq_ack_w(1);
            } else if (address == 0xf800) {
                firq_ack_w(1);
            } else if (address == 0xfa00) {
                int i1 = 1;
            } else if (address >= 0xfc00 && address <= 0xfc01) {
                int i1 = 1;
            } else {
                int i1 = 1;
            }
        } else {
            int i1 = 1;
        }
    }

    public static void N2WriteMemory(short address, byte data) {
        int offset;
        if (address == 0x4000) {
            YM2151.ym2151_register_port_0_w(data);
        } else if (address == 0x4001) {
            YM2151.ym2151_data_port_0_w(data);
        } else if (address >= 0x5000 && address <= 0x53ff) {
            offset = address & 0x3ff;
            Namco.namcos1_cus30_w(offset, data);
        } else if (address >= 0x7000 && address <= 0x77ff) {
            offset = address & 0x7ff;
            namcos1_triram[offset] = data;
        } else if (address >= 0x8000 && address <= 0x9fff) {
            offset = address & 0x1fff;
            bank_ram20[offset] = data;
        } else if (address >= 0xc000 && address <= 0xc001) {
            namcos1_sound_bankswitch_w(data);
        } else if (address == 0xd001) {
            namcos1_watchdog_w();
        } else if (address == 0xe000) {
            irq_ack_w(2);
        } else {
            int i1 = 1;
        }
    }

    public static void N3WriteMemory(short address, byte data) {
        int offset;
        if (address >= 0x0000 && address <= 0x001f) {
            offset = address & 0x1f;
            M6800.m1.hd63701_internal_registers_w(offset, data);
        } else if (address >= 0x0080 && address <= 0x00ff) {
            offset = address & 0x7f;
            bank_ram30[offset] = data;
        } else if (address == 0xc000) {
            namcos1_mcu_patch_w(data);
        } else if (address >= 0xc000 && address <= 0xc7ff) {
            offset = address & 0x7ff;
            namcos1_triram[offset] = data;
        } else if (address >= 0xc800 && address <= 0xcfff) {
            offset = address & 0x7ff;
            Generic.generic_nvram[offset] = data;
        } else if (address == 0xd000) {
            namcos1_dac0_w(data);
        } else if (address == 0xd400) {
            namcos1_dac1_w(data);
        } else if (address == 0xd800) {
            namcos1_mcu_bankswitch_w(data);
        } else if (address == 0xf000) {
            irq_ack_w(3);
        } else {
            int i1 = 1;
        }
    }

    public static void N3WriteIO(short address, byte data) {
        if (address == 0x100) {
            namcos1_coin_w(data);
        } else if (address == 0x101) {
            namcos1_dac_gain_w(data);
        }
    }

//#endregion

//#region Memory2


    public static byte N3ReadMemory_quester(short address) {
        byte result;
        if (address == 0x1400) {
            if ((strobe & 0x20) == 0)
                result = (byte) ((byte0 & 0x90) | (strobe & 0x40) | (Inptport.input_port_read_direct(Inptport.analog_p0) & 0x0f));
            else
                result = (byte) ((byte0 & 0x90) | (strobe & 0x40) | (Inptport.input_port_read_direct(Inptport.analog_p1) & 0x0f));
            strobe ^= 0x40;
        } else if (address == 0x1401) {
            if ((strobe & 0x20) == 0)
                result = (byte) ((byte1 & 0x90) | 0x00 | (Inptport.input_port_read_direct(Inptport.analog_p0) >> 4));
            else
                result = (byte) ((byte1 & 0x90) | 0x20 | (Inptport.input_port_read_direct(Inptport.analog_p1) >> 4));
            if ((strobe & 0x40) == 0)
                strobe ^= 0x20;
        } else {
            result = N3ReadMemory(address);
        }
        return result;
    }

    public static byte N3ReadMemory_berabohm(short address) {
        byte result;
        if (address == 0x1400) {
            int inp = input_count;
            if (inp == 4) {
                result = byte0;
            } else {
                if (inp == 0) {
                    result = byte00;
                } else if (inp == 1) {
                    result = byte01;
                } else if (inp == 2) {
                    result = byte02;
                } else if (inp == 3) {
                    result = byte03;
                } else {
                    result = 0;
                }
                if ((result & 1) != 0) {
                    result = 0x7f;
                } else if ((result & 2) != 0) {
                    result = 0x48;
                } else if ((result & 4) != 0) {
                    result = 0x40;
                }
            }
        } else if (address == 0x1401) {
            result = (byte) (byte1 & 0x8f);
            if (++strobe_count > 4) {
                strobe_count = 0;
                strobe ^= 0x40;
                if (strobe == 0) {
                    input_count = (input_count + 1) % 5;
                    if (input_count == 3) {
                        result |= 0x10;
                    }
                }
            }
            result |= strobe;
        } else {
            result = N3ReadMemory(address);
        }
        return result;
    }

    public static byte N3ReadMemory_faceoff(short address) {
        byte result;
        if (address == 0x1400) {
            result = (byte) ((byte0 & 0x80) | stored_input0);
        } else if (address == 0x1401) {
            result = (byte) (byte1 & 0x80);
            if (++strobe_count > 8) {
                strobe_count = 0;
                result |= (byte) input_count;
                switch (input_count) {
                    case 0:
                        stored_input0 = byte00 & 0x1f;
                        stored_input1 = (byte03 & 0x07) << 3;
                        break;
                    case 3:
                        stored_input0 = byte02 & 0x1f;
                        break;
                    case 4:
                        stored_input0 = byte01 & 0x1f;
                        stored_input1 = byte03 & 0x18;
                        break;
                    default:
                        stored_input0 = 0x1f;
                        stored_input1 = 0x1f;
                        break;
                }
                input_count = (input_count + 1) & 7;
            } else {
                result |= (byte) (0x40 | stored_input1);
            }
        } else {
            result = N3ReadMemory(address);
        }
        return result;
    }

//#endregion

//#region State

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        writer.write(dipsw);
        for (i = 0; i < 0x2000; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(bank_ram20, 0, 0x2000);
        writer.write(bank_ram30, 0, 0x80);
        writer.write(namcos1_videoram, 0, 0x8000);
        writer.write(namcos1_cus116, 0, 0x10);
        writer.write(namcos1_spriteram, 0, 0x1000);
        writer.write(namcos1_playfield_control, 0, 0x20);
        writer.write(copy_sprites);
        writer.write(s1ram, 0, 0x8000);
        writer.write(namcos1_triram, 0, 0x800);
        writer.write(namcos1_paletteram, 0, 0x8000);
        writer.write(key, 0, 8);
        writer.write(audiocpurom_offset);
        writer.write(mcu_patch_data);
        writer.write(mcurom_offset);
        writer.write(namcos1_reset);
        writer.write(wdog);
        writer.write(dac0_value);
        writer.write(dac1_value);
        writer.write(dac0_gain);
        writer.write(dac1_gain);
        writer.write(Generic.generic_nvram, 0, 0x800);
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 8; j++) {
                writer.write(cus117_offset[i][j]);
            }
        }
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 8; j++) {
                writer.write(user1rom_offset[i][j]);
            }
        }
        for (i = 0; i < 3; i++) {
            M6809.mm1[i].SaveStateBinary(writer);
        }
        M6800.m1.SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        YM2151.SaveStateBinary(writer);
        Namco.SaveStateBinary(writer);
        writer.write(DAC.dac1.output);
        writer.write(Sound.ym2151stream.output_sampindex);
        writer.write(Sound.ym2151stream.output_base_sampindex);
        writer.write(Sound.namcostream.output_sampindex);
        writer.write(Sound.namcostream.output_base_sampindex);
        writer.write(Sound.dacstream.output_sampindex);
        writer.write(Sound.dacstream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i, j;
            dipsw = reader.readByte();
            for (i = 0; i < 0x2000; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            bank_ram20 = reader.readBytes(0x2000);
            bank_ram30 = reader.readBytes(0x80);
            namcos1_videoram = reader.readBytes(0x8000);
            namcos1_cus116 = reader.readBytes(0x10);
            namcos1_spriteram = reader.readBytes(0x1000);
            namcos1_playfield_control = reader.readBytes(0x20);
            copy_sprites = reader.readInt32();
            s1ram = reader.readBytes(0x8000);
            namcos1_triram = reader.readBytes(0x800);
            namcos1_paletteram = reader.readBytes(0x8000);
            key = reader.readBytes(8);
            audiocpurom_offset = reader.readInt32();
            mcu_patch_data = reader.readInt32();
            mcurom_offset = reader.readInt32();
            namcos1_reset = reader.readInt32();
            wdog = reader.readInt32();
            dac0_value = reader.readInt32();
            dac1_value = reader.readInt32();
            dac0_gain = reader.readInt32();
            dac1_gain = reader.readInt32();
            Generic.generic_nvram = reader.readBytes(0x800);
            for (i = 0; i < 2; i++) {
                for (j = 0; j < 8; j++) {
                    cus117_offset[i][j] = reader.readInt32();
                }
            }
            for (i = 0; i < 2; i++) {
                for (j = 0; j < 8; j++) {
                    user1rom_offset[i][j] = reader.readInt32();
                }
            }
            for (i = 0; i < 3; i++) {
                M6809.mm1[i].LoadStateBinary(reader);
            }
            M6800.m1.LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            YM2151.LoadStateBinary(reader);
            Namco.LoadStateBinary(reader);
            DAC.dac1.output = reader.readInt16();
            Sound.ym2151stream.output_sampindex = reader.readInt32();
            Sound.ym2151stream.output_base_sampindex = reader.readInt32();
            Sound.namcostream.output_sampindex = reader.readInt32();
            Sound.namcostream.output_base_sampindex = reader.readInt32();
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

    public static void loop_inputports_ns1_3b() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte0 &= (byte) ~0x80;
        } else {
            byte0 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte1 &= (byte) ~0x80;
        } else {
            byte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte0 &= (byte) ~0x01;
        } else {
            byte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte0 &= (byte) ~0x02;
        } else {
            byte0 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte0 &= (byte) ~0x04;
        } else {
            byte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte0 &= (byte) ~0x08;
        } else {
            byte0 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte0 &= (byte) ~0x10;
        } else {
            byte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte0 &= (byte) ~0x20;
        } else {
            byte0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            byte0 &= (byte) ~0x40;
        } else {
            byte0 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            byte1 &= (byte) ~0x01;
        } else {
            byte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte1 &= (byte) ~0x02;
        } else {
            byte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte1 &= (byte) ~0x04;
        } else {
            byte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte1 &= (byte) ~0x08;
        } else {
            byte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte1 &= (byte) ~0x10;
        } else {
            byte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            byte1 &= (byte) ~0x20;
        } else {
            byte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            byte1 &= (byte) ~0x40;
        } else {
            byte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            byte2 &= (byte) ~0x20;
        } else {
            byte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            byte2 &= (byte) ~0x40;
        } else {
            byte2 |= 0x40;
        }
    }

    public static void loop_inputports_ns1_quester() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte0 &= (byte) ~0x80;
        } else {
            byte0 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte1 &= (byte) ~0x80;
        } else {
            byte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte0 &= (byte) ~0x10;
        } else {
            byte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte0 &= (byte) ~0x20;
        } else {
            byte0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            byte0 &= (byte) ~0x40;
        } else {
            byte0 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte1 &= (byte) ~0x10;
        } else {
            byte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            byte1 &= (byte) ~0x20;
        } else {
            byte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            byte1 &= (byte) ~0x40;
        } else {
            byte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            byte2 &= (byte) ~0x20;
        } else {
            byte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            byte2 &= (byte) ~0x40;
        } else {
            byte2 |= 0x40;
        }
        Inptport.frame_update_analog_field_quester_p0(Inptport.analog_p0);
        Inptport.frame_update_analog_field_quester_p1(Inptport.analog_p1);
    }

    public static void loop_inputports_ns1_berabohm() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte0 &= (byte) ~0x80;
        } else {
            byte0 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte1 &= (byte) ~0x80;
        } else {
            byte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte0 &= (byte) ~0x01;
        } else {
            byte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte0 &= (byte) ~0x02;
        } else {
            byte0 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte0 &= (byte) ~0x04;
        } else {
            byte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte0 &= (byte) ~0x08;
        } else {
            byte0 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte01 |= 0x01;
        } else {
            byte01 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte01 |= 0x02;
        } else {
            byte01 &= (byte) ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            byte01 |= 0x04;
        } else {
            byte01 &= (byte) ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            byte00 |= 0x01;
        } else {
            byte00 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            byte00 |= 0x02;
        } else {
            byte00 &= (byte) ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            byte00 |= 0x04;
        } else {
            byte00 &= (byte) ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            byte1 &= (byte) ~0x01;
        } else {
            byte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte1 &= (byte) ~0x02;
        } else {
            byte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte1 &= (byte) ~0x04;
        } else {
            byte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte1 &= (byte) ~0x08;
        } else {
            byte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte03 |= 0x01;
        } else {
            byte03 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            byte03 |= 0x02;
        } else {
            byte03 &= (byte) ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            byte03 |= 0x04;
        } else {
            byte03 &= (byte) ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            byte02 |= 0x01;
        } else {
            byte02 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            byte02 |= 0x02;
        } else {
            byte02 &= (byte) ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {
            byte02 |= 0x04;
        } else {
            byte02 &= (byte) ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            byte2 &= (byte) ~0x20;
        } else {
            byte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            byte2 &= (byte) ~0x40;
        } else {
            byte2 |= 0x40;
        }
    }

    public static void loop_inputports_ns1_faceoff() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte0 &= (byte) ~0x80;
        } else {
            byte0 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte1 &= (byte) ~0x80;
        } else {
            byte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte00 &= (byte) ~0x01;
        } else {
            byte00 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte00 &= (byte) ~0x02;
        } else {
            byte00 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte00 &= (byte) ~0x04;
        } else {
            byte00 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte00 &= (byte) ~0x08;
        } else {
            byte00 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte00 &= (byte) ~0x10;
        } else {
            byte00 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte01 &= (byte) ~0x10;
        } else {
            byte01 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            byte01 &= (byte) ~0x01;
        } else {
            byte01 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte01 &= (byte) ~0x02;
        } else {
            byte01 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte01 &= (byte) ~0x04;
        } else {
            byte01 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte01 &= (byte) ~0x08;
        } else {
            byte01 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            byte2 &= (byte) ~0x20;
        } else {
            byte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            byte2 &= (byte) ~0x40;
        } else {
            byte2 |= 0x40;
        }
    }

    public static void loop_inputports_ns1_tankfrce4() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte00 &= (byte) ~0x01;
        } else {
            byte00 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte00 &= (byte) ~0x02;
        } else {
            byte00 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte00 &= (byte) ~0x04;
        } else {
            byte00 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte00 &= (byte) ~0x08;
        } else {
            byte00 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte00 &= (byte) ~0x10;
        } else {
            byte00 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            byte02 &= (byte) ~0x01;
        } else {
            byte02 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte02 &= (byte) ~0x02;
        } else {
            byte02 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte02 &= (byte) ~0x04;
        } else {
            byte02 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte02 &= (byte) ~0x08;
        } else {
            byte02 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte02 &= (byte) ~0x10;
        } else {
            byte02 |= 0x10;
        }
    }

    public static void record_port() {
        if (byte0 != byte0_old || byte1 != byte1_old || byte2 != byte2_old) {
            byte0_old = byte0;
            byte1_old = byte1;
            byte2_old = byte2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(byte0);
            Mame.bwRecord.write(byte1);
            Mame.bwRecord.write(byte2);
        }
    }

    public static void replay_port() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                byte0_old = Mame.brRecord.readByte();
                byte1_old = Mame.brRecord.readByte();
                byte2_old = Mame.brRecord.readByte();
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            byte0 = byte0_old;
            byte1 = byte1_old;
            byte2 = byte2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Video


    public static byte[] namcos1_videoram;
    public static byte[] namcos1_cus116;
    public static byte[] namcos1_spriteram;
    public static byte[] namcos1_playfield_control;
    private static short[] uu2000;
    public static int flip_screen_x, flip_screen_y;
    public static int copy_sprites;

    public static void video_start_namcos1() {
        int i;
        namcos1_videoram = new byte[0x8000];
        namcos1_cus116 = new byte[0x10];
        namcos1_spriteram = new byte[0x1000];
        namcos1_playfield_control = new byte[0x20];
        Arrays.fill(namcos1_videoram, 0, 0x8000, (byte) 0);
        Arrays.fill(namcos1_paletteram, 0, 0x8000, (byte) 0);
        Arrays.fill(namcos1_cus116, 0, 0x10, (byte) 0);
        Arrays.fill(namcos1_playfield_control, 0, 0x20, (byte) 0);
        uu2000 = new short[0x200 * 0x200];
        for (i = 0; i < 0x4_0000; i++) {
            uu2000[i] = 0x2000;
        }
        ttmap[4].tilemap_set_scrolldx(73, 512 - 73);
        ttmap[5].tilemap_set_scrolldx(73, 512 - 73);
        ttmap[4].tilemap_set_scrolldy(0x10, 0x110);
        ttmap[5].tilemap_set_scrolldy(0x10, 0x110);
        for (i = 0; i < 0x2000; i++) {
            Palette.palette_entry_set_color1(i, Palette.make_rgb(0, 0, 0));
        }
        copy_sprites = 0;
    }

    public static byte namcos1_videoram_r(int offset) {
        return namcos1_videoram[offset];
    }

    public static void namcos1_videoram_w(int offset, byte data) {
        namcos1_videoram[offset] = data;
        if (offset < 0x7000) {
            int layer = offset >> 13;
            int num = (offset & 0x1fff) >> 1;
            int[] row = new int[1], col = new int[1];
            //row = num / Tilemap.ttmap[layer].cols;
            //col = num % Tilemap.ttmap[layer].cols;
            //Tilemap.tilemap_mark_tile_dirty(Tilemap.ttmap[layer], row, col);
            //row = num / Tmap.ttmap[layer].cols;
            //col = num % Tmap.ttmap[layer].cols;
            ttmap[layer].get_row_col(num, /* out */ row, /* out */  col);
            ttmap[layer].tilemap_mark_tile_dirty(row[0], col[0]);
        } else {
            int layer = (offset >> 11 & 1) + 4;
            int num = ((offset & 0x7ff) - 0x10) >> 1;
            int[] row = new int[1], col = new int[1];
            if (num >= 0 && num < 0x3f0) {
                //row = num / Tilemap.ttmap[layer].cols;
                //col = num % Tilemap.ttmap[layer].cols;
                //Tilemap.tilemap_mark_tile_dirty(Tilemap.ttmap[layer], row, col);
                //row = num / Tmap.ttmap[layer].cols;
                //col = num % Tmap.ttmap[layer].cols;
                ttmap[layer].get_row_col(num, /* out */  row, /* out */  col);
                ttmap[layer].tilemap_mark_tile_dirty(row[0], col[0]);
            }
        }
    }

    public static void namcos1_paletteram_w(int offset, byte data) {
        if (namcos1_paletteram[offset] == data)
            return;
        if ((offset & 0x1800) != 0x1800) {
            int r, g, b;
            int color = ((offset & 0x6000) >> 2) | (offset & 0x7ff);
            namcos1_paletteram[offset] = data;
            offset &= ~0x1800;
            r = namcos1_paletteram[offset];
            g = namcos1_paletteram[offset + 0x0800];
            b = namcos1_paletteram[offset + 0x1000];
            Palette.palette_entry_set_color1(color, Palette.make_rgb(r, g, b));
        } else {
            int i, j;
            namcos1_cus116[offset & 0x0f] = data;
            for (i = 0x1800; i < 0x8000; i += 0x2000) {
                offset = (offset & 0x0f) | i;
                for (j = 0; j < 0x80; j++, offset += 0x10) {
                    namcos1_paletteram[offset] = data;
                }
            }
        }
    }

    public static byte namcos1_spriteram_r(int offset) {
        if (offset < 0x1000)
            return namcos1_spriteram[offset];
        else
            return namcos1_playfield_control[offset & 0x1f];
    }

    public static void namcos1_spriteram_w(int offset, byte data) {
        if (offset < 0x1000) {
            namcos1_spriteram[offset] = data;
            if (offset == 0x0ff2) {
                copy_sprites = 1;
            }
        } else {
            namcos1_playfield_control[offset & 0x1f] = data;
        }
    }

    public static void draw_sprites(int iBitmap, RECT cliprect) {
        int source_offset;
        int sprite_xoffs = namcos1_spriteram[0x800 + 0x07f5] + ((namcos1_spriteram[0x800 + 0x07f4] & 1) << 8);
        int sprite_yoffs = namcos1_spriteram[0x800 + 0x07f7];
        for (source_offset = 0xfe0; source_offset >= 0x800; source_offset -= 0x10) {
            int[] sprite_size = new int[] {16, 8, 32, 4};
            int attr1 = namcos1_spriteram[source_offset + 10];
            int attr2 = namcos1_spriteram[source_offset + 14];
            int color = namcos1_spriteram[source_offset + 12];
            int flipx = (attr1 & 0x20) >> 5;
            int flipy = (attr2 & 0x01);
            int sizex = sprite_size[(attr1 & 0xc0) >> 6];
            int sizey = sprite_size[(attr2 & 0x06) >> 1];
            int tx = (attr1 & 0x18) & (~(sizex - 1));
            int ty = (attr2 & 0x18) & (~(sizey - 1));
            int sx = namcos1_spriteram[source_offset + 13] + ((color & 0x01) << 8);
            int sy = -namcos1_spriteram[source_offset + 15] - sizey;
            int sprite = namcos1_spriteram[source_offset + 11];
            int sprite_bank = attr1 & 7;
            int priority = (namcos1_spriteram[source_offset + 14] & 0xe0) >> 5;
            namcos1_pri = priority;
            sprite += sprite_bank * 256;
            color = color >> 1;
            sx += sprite_xoffs;
            sy -= sprite_yoffs;
            if (Video.flip_screen_get()) {
                sx = -sx - sizex;
                sy = -sy - sizey;
                flipx ^= 1;
                flipy ^= 1;
            }
            sy++;
            Drawgfx.common_drawgfx_na(sizex, sizey, tx, ty, sprite, color, flipx, flipy, sx & 0x1ff, ((sy + 16) & 0xff) - 16, cliprect);
        }
    }

    public static void video_update_namcos1() {
        int i, j, scrollx, scrolly;
        byte priority;
        RECT new_clip = new RECT();
        new_clip.min_x = 0x49;
        new_clip.max_x = 0x168;
        new_clip.min_y = 0x10;
        new_clip.max_y = 0xef;
        Video.flip_screen_set_no_update((namcos1_spriteram[0x800 + 0x07f6] & 1) != 0);
        tilemap_set_flip(Video.flip_screen_get() ? (byte) (Tilemap.TILEMAP_FLIPY | Tilemap.TILEMAP_FLIPX) : (byte) 0);
        System.arraycopy(uu2000, 0, Video.bitmapbase[Video.curbitmap], 0, 0x4_0000);
        i = ((namcos1_cus116[0] << 8) | namcos1_cus116[1]) - 1;
        if (new_clip.min_x < i)
            new_clip.min_x = i;
        i = ((namcos1_cus116[2] << 8) | namcos1_cus116[3]) - 1 - 1;
        if (new_clip.max_x > i)
            new_clip.max_x = i;
        i = ((namcos1_cus116[4] << 8) | namcos1_cus116[5]) - 0x11;
        if (new_clip.min_y < i)
            new_clip.min_y = i;
        i = ((namcos1_cus116[6] << 8) | namcos1_cus116[7]) - 0x11 - 1;
        if (new_clip.max_y > i)
            new_clip.max_y = i;
        if (new_clip.max_x < new_clip.min_x || new_clip.max_y < new_clip.min_y)
            return;
        for (i = 0; i < 6; i++) {
            ttmap[i].tilemap_set_palette_offset((namcos1_playfield_control[i + 24] & 7) * 256);
        }
        for (i = 0; i < 4; i++) {
            int[] disp_x = new int[] {25, 27, 28, 29};
            j = i << 2;
            scrollx = (namcos1_playfield_control[j + 1] + (namcos1_playfield_control[j + 0] << 8)) - disp_x[i];
            scrolly = (namcos1_playfield_control[j + 3] + (namcos1_playfield_control[j + 2] << 8)) + 8;
            if (Video.flip_screen_get()) {
                scrollx = -scrollx;
                scrolly = -scrolly;
            }
            ttmap[i].tilemap_set_scrollx(0, scrollx);
            ttmap[i].tilemap_set_scrolly(0, scrolly);
        }
//        Arrays.fill(Tilemap.priority_bitmap, 0, 0x4_0000, null);
        for (i = 0; i < 0x200; i++) {
            for (j = 0; j < 0x200; j++) {
                Tilemap.priority_bitmap[i][j] = 0;
            }
        }
        for (priority = 0; priority < 8; priority++) {
            for (i = 0; i < 6; i++) {
                if (namcos1_playfield_control[16 + i] == priority) {
                    ttmap[i].tilemap_draw_primask(new_clip, 0x10, priority);
                }
            }
        }
        draw_sprites(Video.curbitmap, new_clip);
    }

    public static void video_eof_namcos1() {
        if (copy_sprites != 0) {
            int i, j;
            for (i = 0; i < 0x800; i += 16) {
                for (j = 10; j < 16; j++) {
                    namcos1_spriteram[0x800 + i + j] = namcos1_spriteram[0x800 + i + j - 6];
                }
            }
            copy_sprites = 0;
        }
    }

//#endregion

//#region Tilemap

    public static Tmap[] ttmap;

    public static void tilemap_init() {
        int i;
        ttmap = new Tmap[6];
        ttmap[0] = new Tmap();
        ttmap[0].rows = 64;
        ttmap[0].cols = 64;
        ttmap[0].videoram_offset = 0x0000;
        ttmap[1] = new Tmap();
        ttmap[1].rows = 64;
        ttmap[1].cols = 64;
        ttmap[1].videoram_offset = 0x2000;
        ttmap[2] = new Tmap();
        ttmap[2].rows = 64;
        ttmap[2].cols = 64;
        ttmap[2].videoram_offset = 0x4000;
        ttmap[3] = new Tmap();
        ttmap[3].rows = 32;
        ttmap[3].cols = 64;
        ttmap[3].videoram_offset = 0x6000;
        ttmap[4] = new Tmap();
        ttmap[4].rows = 28;
        ttmap[4].cols = 36;
        ttmap[4].videoram_offset = 0x7010;
        ttmap[5] = new Tmap();
        ttmap[5].rows = 28;
        ttmap[5].cols = 36;
        ttmap[5].videoram_offset = 0x7810;
        for (i = 0; i < 6; i++) {
            ttmap[i].tilewidth = 8;
            ttmap[i].tileheight = 8;
            ttmap[i].width = ttmap[i].cols * ttmap[i].tilewidth;
            ttmap[i].height = ttmap[i].rows * ttmap[i].tileheight;
            ttmap[i].enable = true;
            ttmap[i].all_tiles_dirty = true;
            ttmap[i].scrollrows = 1;
            ttmap[i].scrollcols = 1;
            ttmap[i].rowscroll = new int[ttmap[i].scrollrows];
            ttmap[i].colscroll = new int[ttmap[i].scrollcols];
            ttmap[i].pixmap = new short[0x200 * 0x200];
            ttmap[i].flagsmap = new byte[0x200][0x200];
            ttmap[i].tileflags = new byte[0x40][0x40];
            ttmap[i].tile_update3 = ttmap[i]::tile_updateNa;
            ttmap[i].tilemap_draw_instance3 = ttmap[i]::tilemap_draw_instanceNa;
        }
    }

    public static void tilemap_set_flip(byte attributes) {
        for (Tmap t1 : ttmap) {
            if (t1.attributes != attributes) {
                t1.attributes = attributes;
            }
        }
    }

//#endregion
}
