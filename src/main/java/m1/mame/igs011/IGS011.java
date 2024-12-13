/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.igs011;

import java.awt.event.KeyEvent;
import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m68000.MC68000;
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
import m1.emu.Timer;
import m1.emu.Video;
import m1.sound.ICS2115;
import m1.sound.OKI6295;
import m1.sound.Sound;
import m1.sound.YM2413;
import m1.sound.YM3812;


public class IGS011 {

    public static short[] priority_ram, paletteram16;
    public static byte prot1, prot2, prot1_swap;
    public static int prot1_addr;
    public static short[] igs003_reg, vbowl_trackball;
    public static short priority, igs_dips_sel, igs_input_sel, lhb_irq_enable;
    public static byte igs012_prot, igs012_prot_swap;
    private static boolean igs012_prot_mode;
    public static byte[] gfx1rom, gfx2rom;
    public static byte dsw1, dsw2, dsw3, dsw4, dsw5;

    public static void IGS011Init() {
        Machine.bRom = true;
        Generic.generic_nvram = new byte[0x4000];
        priority_ram = new short[0x800];
        paletteram16 = new short[0x1000];
        igs003_reg = new short[2];
        vbowl_trackball = new short[2];
        switch (Machine.sName) {
            case "drgnwrld":
            case "drgnwrldv30":
            case "drgnwrldv21":
            case "drgnwrldv21j":
            case "drgnwrldv20j":
            case "drgnwrldv10c":
            case "drgnwrldv11h":
            case "drgnwrldv40k":
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                gfx1rom = Machine.GetRom("gfx1.rom");
                OKI6295.okirom = Machine.GetRom("oki.rom");
                dsw1 = (byte) 0xff;
                dsw2 = (byte) 0xff;
                dsw3 = (byte) 0xff;
                if (Memory.mainrom == null || gfx1rom == null || OKI6295.okirom == null) {
                    Machine.bRom = false;
                }
                break;
            case "lhb":
            case "lhbv33c":
            case "dbc":
            case "ryukobou":
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                gfx1rom = Machine.GetRom("gfx1.rom");
                OKI6295.okirom = Machine.GetRom("oki.rom");
                dsw1 = (byte) 0xf7;
                dsw2 = (byte) 0xff;
                dsw3 = (byte) 0xff;
                dsw4 = (byte) 0xf0;
                dsw5 = (byte) 0xff;
                if (Memory.mainrom == null || gfx1rom == null || OKI6295.okirom == null) {
                    Machine.bRom = false;
                }
                break;
            case "lhb2":
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                gfx1rom = Machine.GetRom("gfx1.rom");
                gfx2rom = Machine.GetRom("gfx2.rom");

                break;
        }
    }

    public static void machine_reset_igs011() {
    }

    private static void igs_dips_w(int offset, byte data) {
        if (offset % 2 == 0) {
            igs_dips_sel = (short) ((data << 8) | (igs_dips_sel & 0xff));
        } else if (offset % 2 == 1) {
            igs_dips_sel = (short) ((igs_dips_sel & 0xff00) | data);
        }
    }

    private static void igs_dips_w(short data) {
        igs_dips_sel = data;
    }

    private static byte igs_dips_r(int num) {
        int i;
        byte ret = 0;
        byte[] dip = new byte[] {dsw1, dsw2, dsw3, dsw4, dsw5};
        for (i = 0; i < num; i++) {
            if (((~igs_dips_sel) & (1 << i)) != 0) {
                ret = dip[i];
            }
        }
        return ret;
    }

    private static byte igs_3_dips_r() {
        return igs_dips_r(3);
    }

    private static byte igs_4_dips_r() {
        return igs_dips_r(4);
    }

    private static byte igs_5_dips_r() {
        return igs_dips_r(5);
    }

    public static void igs011_prot1_w1(int offset, byte data) {
        switch (offset) {
            case 0: // COPY ACCESSING_BITS_8_15
                if ((data & 0xff) == 0x33) {
                    prot1 = prot1_swap;
                    return;
                }
                break;
            case 2: // INC
                if ((data & 0xff) == 0xff) {
                    prot1++;
                    return;
                }
                break;
            case 4: // DEC
                if ((data & 0xff) == 0xaa) {
                    prot1--;
                    return;
                }
                break;
            case 6: // SWAP
                if ((data & 0xff) == 0x55) {
                    byte x = prot1;
                    prot1_swap = (byte) ((BIT(x, 1) << 3) | ((BIT(x, 2) | BIT(x, 3)) << 2) | (BIT(x, 2) << 1) | (BIT(x, 0) & BIT(x, 3)));
                    return;
                }
                break;
        }
    }

    public static void igs011_prot1_w(int offset, short data) {
        offset *= 2;
        switch (offset) {
            case 0: // COPY ACCESSING_BITS_8_15
                if ((data & 0xff00) == 0x3300) {
                    prot1 = prot1_swap;
                    return;
                }
                break;
            case 2: // INC
                if ((data & 0xff00) == 0xff00) {
                    prot1++;
                    return;
                }
                break;
            case 4: // DEC
                if ((data & 0xff00) == 0xaa00) {
                    prot1--;
                    return;
                }
                break;
            case 6: // SWAP
                if ((data & 0xff00) == 0x5500) {
                    byte x = prot1;
                    prot1_swap = (byte) ((BIT(x, 1) << 3) | ((BIT(x, 2) | BIT(x, 3)) << 2) | (BIT(x, 2) << 1) | (BIT(x, 0) & BIT(x, 3)));
                    return;
                }
                break;
        }
    }

    public static byte igs011_prot1_r() {
        byte x = prot1;
        return (byte) ((((BIT(x, 1) & BIT(x, 2)) ^ 1) << 5) | ((BIT(x, 0) ^ BIT(x, 3)) << 2));
    }

    public static void igs011_prot_addr_w(short data) {
        prot1 = 0x00;
        prot1_swap = 0x00;
        prot1_addr = (data << 4) ^ 0x8340;
    }

    public static void igs011_prot2_reset_w() {
        prot2 = 0x00;
    }

    public static int igs011_prot2_reset_r() {
        prot2 = 0x00;
        return 0;
    }

    public static void igs011_prot2_inc_w() {
        prot2++;
    }

    public static void igs011_prot2_dec_w() {
        prot2--;
    }

    public static void chmplst2_interrupt() {
        switch (Cpuexec.iloops) {
            case 0:
                Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
                break;
            case 1:
            default:
                Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
                break;
        }
    }

    public static void drgnwrld_igs011_prot2_swap_w() {
        byte x = prot2;
        prot2 = (byte) (((BIT(x, 3) & BIT(x, 0)) << 4) | (BIT(x, 2) << 3) | ((BIT(x, 0) | BIT(x, 1)) << 2) | ((BIT(x, 2) ^ BIT(x, 4) ^ 1) << 1) | (BIT(x, 1) ^ 1 ^ BIT(x, 3)));
    }

    public static void lhb_igs011_prot2_swap_w(int offset) {
        offset *= 2;
        {
            byte x = prot2;
            prot2 = (byte) ((((BIT(x, 0) ^ 1) | BIT(x, 1)) << 2) | (BIT(x, 2) << 1) | (BIT(x, 0) & BIT(x, 1)));
        }
    }

    public static void wlcc_igs011_prot2_swap_w(int offset) {
        offset *= 2;
        {
            byte x = prot2;
            prot2 = (byte) (((BIT(x, 3) ^ BIT(x, 2)) << 4) | ((BIT(x, 2) ^ BIT(x, 1)) << 3) | ((BIT(x, 1) ^ BIT(x, 0)) << 2) | ((BIT(x, 4) ^ BIT(x, 0) ^ 1) << 1) | (BIT(x, 4) ^ BIT(x, 3) ^ 1));
        }
    }

    private static void vbowl_igs011_prot2_swap_w(int offset) {
        offset *= 2;
        {
            byte x = prot2;
            prot2 = (byte) (((BIT(x, 3) ^ BIT(x, 2)) << 4) | ((BIT(x, 2) ^ BIT(x, 1)) << 3) | ((BIT(x, 1) ^ BIT(x, 0)) << 2) | ((BIT(x, 4) ^ BIT(x, 0)) << 1) | (BIT(x, 4) ^ BIT(x, 3)));
        }
    }

    private static short drgnwrldv21_igs011_prot2_r() {
        byte x = prot2;
        byte b9 = (byte) ((BIT(x, 4) ^ 1) | ((BIT(x, 0) ^ 1) & BIT(x, 2)) | ((BIT(x, 3) ^ BIT(x, 1) ^ 1) & ((((BIT(x, 4) ^ 1) & BIT(x, 0)) | BIT(x, 2)) ^ 1)));
        return (short) (b9 << 9);
    }

    private static short drgnwrldv20j_igs011_prot2_r() {
        byte x = prot2;
        byte b9 = (byte) (((BIT(x, 4) ^ 1) | (BIT(x, 0) ^ 1)) | ((BIT(x, 3) | BIT(x, 1)) ^ 1) | ((BIT(x, 2) & BIT(x, 0)) ^ 1));
        return (short) (b9 << 9);
    }

    private static short lhb_igs011_prot2_r() {
        byte x = prot2;
        byte b9 = (byte) ((BIT(x, 2) ^ 1) | (BIT(x, 1) & BIT(x, 0)));
        return (short) (b9 << 9);
    }

    private static short dbc_igs011_prot2_r() {
        byte x = prot2;
        byte b9 = (byte) ((BIT(x, 1) ^ 1) | ((BIT(x, 0) ^ 1) & BIT(x, 2)));
        return (short) (b9 << 9);
    }

    private static short ryukobou_igs011_prot2_r() {
        byte x = prot2;
        byte b9 = (byte) (((BIT(x, 1) ^ 1) | BIT(x, 2)) & BIT(x, 0));
        return (short) (b9 << 9);
    }

    private static short lhb2_igs011_prot2_r() {
        byte x = prot2;
        byte b3 = (byte) ((BIT(x, 2) ^ 1) | (BIT(x, 1) ^ 1) | BIT(x, 0));
        return (short) (b3 << 3);
    }

    private static short vbowl_igs011_prot2_r() {
        byte x = prot2;
        byte b9 = (byte) (((BIT(x, 4) ^ 1) & (BIT(x, 3) ^ 1)) | ((BIT(x, 2) & BIT(x, 1)) ^ 1) | ((BIT(x, 4) | BIT(x, 0)) ^ 1));
        return (short) (b9 << 9);
    }

    private static void igs012_prot_reset_w() {
        igs012_prot = 0x00;
        igs012_prot_swap = 0x00;
        igs012_prot_mode = false;
    }

    private static boolean MODE_AND_DATA(boolean _MODE, byte _DATA, byte data) {
        boolean b1;
        b1 = ((igs012_prot_mode == _MODE) && (data == _DATA));
        return b1;
    }

    private static boolean MODE_AND_DATA(boolean _MODE, byte _DATA, short data) {
        boolean b1;
        b1 = (igs012_prot_mode == _MODE) && (((data & 0xff00) == (_DATA << 8)) || ((data & 0xff) == _DATA));
        return b1;
    }

    private static void igs012_prot_mode_w(short data) {
        if (MODE_AND_DATA(false, (byte) 0xcc, data) || MODE_AND_DATA(true, (byte) 0xdd, data)) {
            igs012_prot_mode = igs012_prot_mode ^ true;
        }
    }

    private static void igs012_prot_inc_w(short data) {
        if (MODE_AND_DATA(false, (byte) 0xff, data)) {
            igs012_prot = (byte) ((igs012_prot + 1) & 0x1f);
        }
    }

    private static void igs012_prot_dec_inc_w(byte data) {
        if (MODE_AND_DATA(false, (byte) 0xaa, data)) {
            igs012_prot = (byte) ((igs012_prot - 1) & 0x1f);
        } else if (MODE_AND_DATA(true, (byte) 0xfa, data)) {
            igs012_prot = (byte) ((igs012_prot + 1) & 0x1f);
        }
    }

    private static void igs012_prot_dec_inc_w(short data) {
        if (MODE_AND_DATA(false, (byte) 0xaa, data)) {
            igs012_prot = (byte) ((igs012_prot - 1) & 0x1f);
        } else if (MODE_AND_DATA(true, (byte) 0xfa, data)) {
            igs012_prot = (byte) ((igs012_prot + 1) & 0x1f);
        }
    }

    private static void igs012_prot_dec_copy_w(short data) {
        if (MODE_AND_DATA(false, (byte) 0x33, data)) {
            igs012_prot = igs012_prot_swap;
        } else if (MODE_AND_DATA(true, (byte) 0x5a, data)) {
            igs012_prot = (byte) ((igs012_prot - 1) & 0x1f);
        }
    }

    private static void igs012_prot_copy_w(short data) {
        if (MODE_AND_DATA(true, (byte) 0x22, data)) {
            igs012_prot = igs012_prot_swap;
        }
    }

    private static void igs012_prot_swap_w(short data) {
        if (MODE_AND_DATA(false, (byte) 0x55, data) || MODE_AND_DATA(true, (byte) 0xa5, data)) {
            byte x = igs012_prot;
            igs012_prot_swap = (byte) ((((BIT(x, 3) | BIT(x, 1)) ^ 1) << 3) | ((BIT(x, 2) & BIT(x, 1)) << 2) | ((BIT(x, 3) ^ BIT(x, 0)) << 1) | (BIT(x, 2) ^ 1));
        }
    }

    private static byte igs012_prot_r() {
        byte x = igs012_prot;
        byte b1 = (byte) ((BIT(x, 3) | BIT(x, 1)) ^ 1);
        byte b0 = (byte) (BIT(x, 3) ^ BIT(x, 0));
        return (byte) ((b1 << 1) | (b0 << 0));
    }

    public static void drgnwrld_igs003_w(int offset, byte data) {
        if ((offset & 1) == 0) {
            igs003_reg[offset / 2] = (short) ((data << 8) | (igs003_reg[offset / 2] & 0xff));
        } else if ((offset & 1) == 1) {
            igs003_reg[offset / 2] = (short) ((igs003_reg[offset / 2] & 0xff00) | data);
        }
        if ((offset / 2) == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x00:
                if ((offset & 1) == 1) {
                    Generic.coin_counter_w(0, data & 2);
                }
                break;
        }
    }

    public static void drgnwrld_igs003_w(int offset, short data) {
        igs003_reg[offset] = data;
        if (offset == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x00:
                Generic.coin_counter_w(0, data & 2);
                break;
            default:
                break;
        }
    }

    public static byte drgnwrld_igs003_r() {
        switch (igs003_reg[0]) {
            case 0x00:
                    /*if (Video.screenstate.frame_number >= 70 && Video.screenstate.frame_number <= 71)
                    {
                        return 0xfe;
                    }
                    else if (Video.screenstate.frame_number >= 80 && Video.screenstate.frame_number <= 81)
                    {
                        return 0xfb;
                    }
                    else*/
            {
                return sbyte0;
            }
            case 0x01:
                return sbyte1;
            case 0x02:
                    /*if (Video.screenstate.frame_number >= 90 && Video.screenstate.frame_number <= 91)
                    {
                        return 0xfb;
                    }
                    else*/
            {
                return sbyte2;
            }
            case 0x20:
                return 0x49;
            case 0x21:
                return 0x47;
            case 0x22:
                return 0x53;
            case 0x24:
                return 0x41;
            case 0x25:
                return 0x41;
            case 0x26:
                return 0x7f;
            case 0x27:
                return 0x41;
            case 0x28:
                return 0x41;
            case 0x2a:
                return 0x3e;
            case 0x2b:
                return 0x41;
            case 0x2c:
                return 0x49;
            case 0x2d:
                return (byte) 0xf9;
            case 0x2e:
                return 0x0a;
            case 0x30:
                return 0x26;
            case 0x31:
                return 0x49;
            case 0x32:
                return 0x49;
            case 0x33:
                return 0x49;
            case 0x34:
                return 0x32;

            default:
                break;
        }
        return 0;
    }

    private static void lhb_inputs_w(int offset, byte data) {
        if (offset == 0) {
            igs_input_sel = (short) ((data << 8) | (igs_input_sel & 0xff));
        } else if (offset == 1) {
            igs_input_sel = (short) ((igs_input_sel & 0xff00) | data);
            Generic.coin_counter_w(0, data & 0x20);
        }
    }

    private static void lhb_inputs_w(short data) {
        igs_input_sel = data;
        Generic.coin_counter_w(0, data & 0x20);
    }

    private static short lhb_inputs_r(int offset) {
        switch (offset) {
            case 0:
                return igs_input_sel;
            case 1:
                if ((~igs_input_sel & 0x01) != 0) {
                    return bkey0;
                }
                if ((~igs_input_sel & 0x02) != 0) {
                    return bkey1;
                }
                if ((~igs_input_sel & 0x04) != 0) {
                    return bkey2;
                }
                if ((~igs_input_sel & 0x08) != 0) {
                    return bkey3;
                }
                if ((~igs_input_sel & 0x10) != 0) {
                    return bkey4;
                }
                break;
        }
        return 0;
    }

    private static void lhb2_igs003_w1(int offset, byte data) {
        igs003_reg[offset] = (short) ((data << 8) | (igs003_reg[offset] & 0xff));
        if (offset == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x00:
                igs_input_sel = (short) ((data << 8) | (igs_input_sel & 0xff));
                break;
        }
    }

    private static void lhb2_igs003_w2(int offset, byte data) {
        igs003_reg[offset] = (short) ((igs003_reg[offset] & 0xff00) | data);
        if (offset == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x00:
                igs_input_sel = (short) ((igs_input_sel & 0xff00) | data);
                //if (ACCESSING_BITS_0_7)
            {
                Generic.coin_counter_w(0, data & 0x20);
            }
            break;
            case 0x02:
                //if (ACCESSING_BITS_0_7)
            {
                lhb2_pen_hi = (byte) (data & 0x07);
                OKI6295.okim6295_set_bank_base((data & 0x08) != 0 ? 0x4_0000 : 0);
            }
            break;
        }
    }

    private static void lhb2_igs003_w(int offset, short data) {
        igs003_reg[offset] = data;
        if (offset == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x00:
                igs_input_sel = data;
                //if (ACCESSING_BITS_0_7)
            {
                Generic.coin_counter_w(0, data & 0x20);
            }
            break;
            case 0x02:
                //if (ACCESSING_BITS_0_7)
            {
                lhb2_pen_hi = (byte) (data & 0x07);
                OKI6295.okim6295_set_bank_base((data & 0x08) != 0 ? 0x4_0000 : 0);
            }
            break;
        }
    }

    private static short lhb2_igs003_r() {
        switch (igs003_reg[0]) {
            case 0x01:
                if ((~igs_input_sel & 0x01) != 0) {
                    //return input_port_read(machine, "KEY0");
                }
                if ((~igs_input_sel & 0x02) != 0) {
                    //return input_port_read(machine, "KEY1");
                }
                if ((~igs_input_sel & 0x04) != 0) {
                    //return input_port_read(machine, "KEY2");
                }
                if ((~igs_input_sel & 0x08) != 0) {
                    //return input_port_read(machine, "KEY3");
                }
                if ((~igs_input_sel & 0x10) != 0) {
                    //return input_port_read(machine, "KEY4");
                }
                break;
            case 0x03:
                return 0xff;

            case 0x20:
                return 0x49;
            case 0x21:
                return 0x47;
            case 0x22:
                return 0x53;

            case 0x24:
                return 0x41;
            case 0x25:
                return 0x41;
            case 0x26:
                return 0x7f;
            case 0x27:
                return 0x41;
            case 0x28:
                return 0x41;

            case 0x2a:
                return 0x3e;
            case 0x2b:
                return 0x41;
            case 0x2c:
                return 0x49;
            case 0x2d:
                return 0xf9;
            case 0x2e:
                return 0x0a;

            case 0x30:
                return 0x26;
            case 0x31:
                return 0x49;
            case 0x32:
                return 0x49;
            case 0x33:
                return 0x49;
            case 0x34:
                return 0x32;
        }
        return 0;
    }

    private static void wlcc_igs003_w1(int offset, byte data) {
        igs003_reg[offset] = (short) ((data << 8) | (igs003_reg[offset] & 0xff));
        if (offset == 0) {
        }
    }

    private static void wlcc_igs003_w2(int offset, byte data) {
        igs003_reg[offset] = (short) ((igs003_reg[offset] & 0xff00) | data);
        if (offset == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x02:
                //if (ACCESSING_BITS_0_7)
            {
                Generic.coin_counter_w(0, data & 0x01);
                OKI6295.okim6295_set_bank_base((data & 0x10) != 0 ? 0x4_0000 : 0);
            }
            break;
        }
    }

    private static void wlcc_igs003_w(int offset, short data) {
        igs003_reg[offset] = data;
        if (offset == 0) {
            return;
        }
        switch (igs003_reg[0]) {
            case 0x02:
                //if (ACCESSING_BITS_0_7)
            {
                Generic.coin_counter_w(0, data & 0x01);
                OKI6295.okim6295_set_bank_base((data & 0x10) != 0 ? 0x4_0000 : 0);
            }
            break;
        }
    }

    private static byte wlcc_igs003_r() {
        switch (igs003_reg[0]) {
            case 0x00:
                return sbyte0;

            case 0x20:
                return 0x49;
            case 0x21:
                return 0x47;
            case 0x22:
                return 0x53;

            case 0x24:
                return 0x41;
            case 0x25:
                return 0x41;
            case 0x26:
                return 0x7f;
            case 0x27:
                return 0x41;
            case 0x28:
                return 0x41;

            case 0x2a:
                return 0x3e;
            case 0x2b:
                return 0x41;
            case 0x2c:
                return 0x49;
            case 0x2d:
                return (byte) 0xf9;
            case 0x2e:
                return 0x0a;

            case 0x30:
                return 0x26;
            case 0x31:
                return 0x49;
            case 0x32:
                return 0x49;
            case 0x33:
                return 0x49;
            case 0x34:
                return 0x32;
        }
        return 0;
    }

    private static void xymg_igs003_w(int offset, short data) {
        igs003_reg[offset] = data;
        if (offset == 0)
            return;
        switch (igs003_reg[0]) {
            case 0x01:
                igs_input_sel = data;
                //if (ACCESSING_BITS_0_7)
            {
                Generic.coin_counter_w(0, data & 0x20);
            }
            break;
        }
    }

    private static byte xymg_igs003_r() {
        switch (igs003_reg[0]) {
            case 0x00:
                return sbytec;
            case 0x02:
                if ((~igs_input_sel & 0x01) != 0) {
                    //return input_port_read(machine, "KEY0");
                }
                if ((~igs_input_sel & 0x02) != 0) {
                    //return input_port_read(machine, "KEY1");
                }
                if ((~igs_input_sel & 0x04) != 0) {
                    //return input_port_read(machine, "KEY2");
                }
                if ((~igs_input_sel & 0x08) != 0) {
                    //return input_port_read(machine, "KEY3");
                }
                if ((~igs_input_sel & 0x10) != 0) {
                    //return input_port_read(machine, "KEY4");
                }
                break;
            case 0x20:
                return 0x49;
            case 0x21:
                return 0x47;
            case 0x22:
                return 0x53;

            case 0x24:
                return 0x41;
            case 0x25:
                return 0x41;
            case 0x26:
                return 0x7f;
            case 0x27:
                return 0x41;
            case 0x28:
                return 0x41;

            case 0x2a:
                return 0x3e;
            case 0x2b:
                return 0x41;
            case 0x2c:
                return 0x49;
            case 0x2d:
                return (byte) 0xf9;
            case 0x2e:
                return 0x0a;

            case 0x30:
                return 0x26;
            case 0x31:
                return 0x49;
            case 0x32:
                return 0x49;
            case 0x33:
                return 0x49;
            case 0x34:
                return 0x32;
        }
        return 0;
    }

    private static void vbowl_igs003_w(int offset, short data) {
        igs003_reg[offset] = data;
        if (offset == 0)
            return;
        switch (igs003_reg[0]) {
            case 0x02:
                //if (ACCESSING_BITS_0_7)
            {
                Generic.coin_counter_w(0, data & 1);
                Generic.coin_counter_w(1, data & 2);
            }
            break;
        }
    }

    private static byte vbowl_igs003_r() {
        switch (igs003_reg[0]) {
            case 0x00:
                return sbyte0;
            case 0x01:
                return sbyte1;
            case 0x20:
                return 0x49;
            case 0x21:
                return 0x47;
            case 0x22:
                return 0x53;

            case 0x24:
                return 0x41;
            case 0x25:
                return 0x41;
            case 0x26:
                return 0x7f;
            case 0x27:
                return 0x41;
            case 0x28:
                return 0x41;

            case 0x2a:
                return 0x3e;
            case 0x2b:
                return 0x41;
            case 0x2c:
                return 0x49;
            case 0x2d:
                return (byte) 0xf9;
            case 0x2e:
                return 0x0a;

            case 0x30:
                return 0x26;
            case 0x31:
                return 0x49;
            case 0x32:
                return 0x49;
            case 0x33:
                return 0x49;
            case 0x34:
                return 0x32;
        }
        return 0;
    }

    private static void igs_YM3812_control_port_0_w(byte data) {
        //if (ACCESSING_BITS_0_7)
        YM3812.ym3812_control_port_0_w(data);
    }

    private static void igs_YM3812_write_port_0_w(byte data) {
        //if (ACCESSING_BITS_0_7)
        YM3812.ym3812_write_port_0_w(data);
    }

    private static void lhb_irq_enable_w(int offset, byte data) {
        if ((offset & 1) == 0) {
            lhb_irq_enable = (short) ((data << 8) | (lhb_irq_enable & 0xff));
        } else if ((offset & 1) == 1) {
            lhb_irq_enable = (short) ((lhb_irq_enable & 0xff00) | data);
        }
    }

    private static void lhb_irq_enable_w(short data) {
        lhb_irq_enable = data;
    }

    private static void lhb_okibank_w(byte data) {
        //ACCESSING_BITS_8_15
        OKI6295.okim6295_set_bank_base((data & 0x2) != 0 ? 0x4_0000 : 0);
    }

    private static void lhb_okibank_w(short data) {
        OKI6295.okim6295_set_bank_base((data & 0x200) != 0 ? 0x4_0000 : 0);
    }

    private static byte ics2115_0_word_r1(int offset) {
        switch (offset) {
            case 0:
                return 0;
            case 1:
                return 0;
            case 2:
                return ICS2115.ics2115_r(3);
        }
        return 0;
    }

    private static byte ics2115_0_word_r2(int offset) {
        switch (offset) {
            case 0:
                return ICS2115.ics2115_r(0);
            case 1:
                return ICS2115.ics2115_r(1);
            case 2:
                return ICS2115.ics2115_r(2);
        }
        return (byte) 0xff;
    }

    private static short ics2115_0_word_r(int offset) {
        switch (offset) {
            case 0:
                return ICS2115.ics2115_r(0);
            case 1:
                return ICS2115.ics2115_r(1);
            case 2:
                return (short) ((ICS2115.ics2115_r(3) << 8) | ICS2115.ics2115_r(2));
        }
        return 0xff;
    }

    private static void ics2115_0_word_w1(int offset, byte data) {
        switch (offset) {
            case 1:
                break;
            case 2:
                ICS2115.ics2115_w(3, data);
                break;
        }
    }

    private static void ics2115_0_word_w2(int offset, byte data) {
        switch (offset) {
            case 1:
                ICS2115.ics2115_w(1, data);
                break;
            case 2:
                ICS2115.ics2115_w(2, data);
                break;
        }
    }

    private static void ics2115_0_word_w(int offset, short data) {
        switch (offset) {
            case 1:
                ICS2115.ics2115_w(1, (byte) data);
                break;
            case 2:
                ICS2115.ics2115_w(2, (byte) data);
                ICS2115.ics2115_w(3, (byte) (data >> 8));
                break;
        }
    }

    private static byte vbowl_unk_r1() {
        return (byte) 0xff;
    }

    private static short vbowl_unk_r() {
        return (short) 0xffff;
    }

    public static void video_eof_vbowl() {
        vbowl_trackball[0] = vbowl_trackball[1];
        //vbowl_trackball[1] = (input_port_read(machine, "AN1") << 8) | input_port_read(machine, "AN0");
    }

    private static void vbowl_pen_hi_w(byte data) {
        //if (ACCESSING_BITS_0_7)
        {
            lhb2_pen_hi = (byte) (data & 0x07);
        }
    }

    private static void vbowl_link_0_w() {
    }

    private static void vbowl_link_1_w() {
    }

    private static void vbowl_link_2_w() {
    }

    private static void vbowl_link_3_w() {
    }

//#region Video

    private static byte lhb2_pen_hi;
    private static byte[][] layer;

    public static class blitter_t {

        public short x, y, w, h, gfx_lo, gfx_hi, depth, pen, flags;
    }

    private static blitter_t blitter;

    private static void igs011_priority_w(int offset, byte data) {
        if (offset % 2 == 0) {
            priority = (short) ((data << 8) | (priority & 0xff));
        } else if (offset % 2 == 1) {
            priority = (short) ((priority & 0xff00) | data);
        }
    }

    private static void igs011_priority_w(short data) {
        priority = data;
    }

    public static void video_start_igs011() {
        int i;
        layer = new byte[8][];
        for (i = 0; i < 8; i++) {
            layer[i] = new byte[0x2_0000];
        }
        lhb2_pen_hi = 0;
    }

    public static void video_update_igs011() {
        int x, y, l, scr_addr, pri_addr;
        int pri_ram_offset;
        pri_ram_offset = (priority & 7) * 0x100;
        for (y = 0; y <= 0xff; y++) // ef
        {
            for (x = 0; x <= 0x1ff; x++) {
                scr_addr = x + y * 0x200;
                pri_addr = 0xff;
                for (l = 0; l < 8; l++) {
                    if (layer[l][scr_addr] != (byte) 0xff) {
                        pri_addr &= ~(1 << l);
                    }
                }
                l = priority_ram[pri_ram_offset + pri_addr] & 7;
                Video.bitmapbase[Video.curbitmap][y * 0x200 + x] = (short) ((layer[l][scr_addr] & 0xff) | (l << 8));
            }
        }
    }

    public static void video_eof_igs011() {

    }

    private static byte igs011_layers_r1(int offset) {
        int layer0 = ((offset & (0x8_0000 / 2)) != 0 ? 4 : 0) + ((offset & 1) != 0 ? 0 : 2);
        offset >>= 1;
        offset &= 0x1_ffff;
        return (byte) (layer[layer0][offset] << 8);
    }

    private static byte igs011_layers_r2(int offset) {
        int layer0 = ((offset & (0x8_0000 / 2)) != 0 ? 4 : 0) + ((offset & 1) != 0 ? 0 : 2);
        offset >>= 1;
        offset &= 0x1_ffff;
        return layer[layer0 + 1][offset];
    }

    private static short igs011_layers_r(int offset) {
        int layer0 = ((offset & (0x8_0000 / 2)) != 0 ? 4 : 0) + ((offset & 1) != 0 ? 0 : 2);
        offset >>= 1;
        offset &= 0x1_ffff;
        return (short) ((layer[layer0][offset] << 8) | layer[layer0 + 1][offset]);
    }

    private static void igs011_layers_w(int offset, byte data) {
        int layer0 = ((offset & 0x8_0000) != 0 ? 4 : 0) + ((offset & 2) != 0 ? 0 : 2);
        offset >>= 2;
        offset &= 0x1_ffff;
        if (offset % 2 == 0) {
            layer[layer0][offset] = data;
        } else if (offset % 2 == 1) {
            layer[layer0 + 1][offset] = data;
        }
    }

    private static void igs011_layers_w(int offset, short data) {
        int layer0 = (((offset & (0x8_0000 / 2)) != 0) ? 4 : 0) + ((offset & 1) != 0 ? 0 : 2);
        offset >>= 1;
        offset &= 0x1_ffff;
        layer[layer0][offset] = (byte) (data >> 8);
        layer[layer0 + 1][offset] = (byte) data;
    }

    private static void igs011_palette(int offset, byte data) {
        int rgb;
        if (offset % 2 == 0) {
            paletteram16[offset / 2] = (short) ((data << 8) | (paletteram16[offset / 2] & 0xff));
        } else if (offset % 2 == 1) {
            paletteram16[offset / 2] = (short) ((paletteram16[offset / 2] & 0xff00) | data);
        }
        rgb = (paletteram16[(offset / 2) & 0x7ff] & 0xff) | ((paletteram16[(offset / 2) | 0x800] & 0xff) << 8);
        Palette.palette_entry_set_color1((offset / 2) & 0x7ff, (Palette.pal5bit((byte) (rgb >> 0)) << 16) | (Palette.pal5bit((byte) (rgb >> 5)) << 8) | Palette.pal5bit((byte) (rgb >> 10)));
    }

    private static void igs011_palette(int offset, short data) {
        int rgb;
        paletteram16[offset] = data;
        rgb = (paletteram16[offset & 0x7ff] & 0xff) | ((paletteram16[offset | 0x800] & 0xff) << 8);
        Palette.palette_entry_set_color1(offset & 0x7ff, (Palette.pal5bit((byte) (rgb >> 0)) << 16) | (Palette.pal5bit((byte) (rgb >> 5)) << 8) | Palette.pal5bit((byte) (rgb >> 10)));
    }

    private static void igs011_blit_x_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.x = (short) ((data << 8) | (blitter.x & 0xff));
        } else if (offset % 2 == 1) {
            blitter.x = (short) ((blitter.x & 0xff00) | data);
        }
    }

    private static void igs011_blit_x_w(short data) {
        blitter.x = data;
    }

    private static void igs011_blit_y_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.y = (short) ((data << 8) | (blitter.y & 0xff));
        } else if (offset % 2 == 1) {
            blitter.y = (short) ((blitter.y & 0xff00) | data);
        }
    }

    private static void igs011_blit_y_w(short data) {
        blitter.y = data;
    }

    private static void igs011_blit_gfx_lo_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.gfx_lo = (short) ((data << 8) | (blitter.gfx_lo & 0xff));
        } else if (offset % 2 == 1) {
            blitter.gfx_lo = (short) ((blitter.gfx_lo & 0xff00) | data);
        }
    }

    private static void igs011_blit_gfx_lo_w(short data) {
        blitter.gfx_lo = data;
    }

    private static void igs011_blit_gfx_hi_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.gfx_hi = (short) ((data << 8) | (blitter.gfx_hi & 0xff));
        } else if (offset % 2 == 1) {
            blitter.gfx_hi = (short) ((blitter.gfx_hi & 0xff00) | data);
        }
    }

    private static void igs011_blit_gfx_hi_w(short data) {
        blitter.gfx_hi = data;
    }

    private static void igs011_blit_w_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.w = (short) ((data << 8) | (blitter.w & 0xff));
        } else if (offset % 2 == 1) {
            blitter.w = (short) ((blitter.w & 0xff00) | data);
        }
    }

    private static void igs011_blit_w_w(short data) {
        blitter.w = data;
    }

    private static void igs011_blit_h_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.h = (short) ((data << 8) | (blitter.h & 0xff));
        } else if (offset % 2 == 1) {
            blitter.h = (short) ((blitter.h & 0xff00) | data);
        }
    }

    private static void igs011_blit_h_w(short data) {
        blitter.h = data;
    }

    private static void igs011_blit_depth_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.depth = (short) ((data << 8) | (blitter.depth & 0xff));
        } else if (offset % 2 == 1) {
            blitter.depth = (short) ((blitter.depth & 0xff00) | data);
        }
    }

    private static void igs011_blit_depth_w(short data) {
        blitter.depth = data;
    }

    private static void igs011_blit_pen_w(int offset, byte data) {
        if (offset % 2 == 0) {
            blitter.pen = (short) ((data << 8) | (blitter.pen & 0xff));
        } else if (offset % 2 == 1) {
            blitter.pen = (short) ((blitter.pen & 0xff00) | data);
        }
    }

    private static void igs011_blit_pen_w(short data) {
        blitter.pen = data;
    }

    private static void igs011_blit_flags_w(short data) {
        int x, xstart, xend, xinc, flipx;
        int y, ystart, yend, yinc, flipy;
        boolean depth4;
        int clear, opaque, z;
        byte trans_pen, clear_pen, pen_hi, pen = 0;
        int gfx_size = gfx1rom.length;
        int gfx2_size = 0;
        if (gfx2rom != null) {
            gfx2_size = gfx2rom.length;
        }
        blitter.flags = data;
        opaque = (blitter.flags & 0x0008) == 0 ? 1 : 0;
        clear = blitter.flags & 0x0010;
        flipx = blitter.flags & 0x0020;
        flipy = blitter.flags & 0x0040;
        if ((blitter.flags & 0x0400) == 0) {
            return;
        }
        pen_hi = (byte) ((lhb2_pen_hi & 0x07) << 5);
        z = blitter.gfx_lo + (blitter.gfx_hi << 16);
        depth4 = !((blitter.flags & 0x7) < (4 - (blitter.depth & 0x7))) || ((z & 0x80_0000) != 0);
        z &= 0x7f_ffff;
        if (depth4) {
            z *= 2;
            if (gfx2rom != null && (blitter.gfx_hi & 0x80) != 0) {
                trans_pen = 0x1f;
            } else {
                trans_pen = 0x0f;
            }
            clear_pen = (byte) (blitter.pen | 0xf0);
        } else {
            if (gfx2rom != null) {
                trans_pen = 0x1f;
            } else {
                trans_pen = (byte) 0xff;
            }
            clear_pen = (byte) blitter.pen;
        }
        xstart = (blitter.x & 0x1ff) - (blitter.x & 0x200);
        ystart = (blitter.y & 0x0ff) - (blitter.y & 0x100);
        if (flipx != 0) {
            xend = xstart - (blitter.w & 0x1ff) - 1;
            xinc = -1;
        } else {
            xend = xstart + (blitter.w & 0x1ff) + 1;
            xinc = 1;
        }
        if (flipy != 0) {
            yend = ystart - (blitter.h & 0x0ff) - 1;
            yinc = -1;
        } else {
            yend = ystart + (blitter.h & 0x0ff) + 1;
            yinc = 1;
        }
        for (y = ystart; y != yend; y += yinc) {
            for (x = xstart; x != xend; x += xinc) {
                if (clear == 0) {
                    if (depth4) {
                        pen = (byte) ((gfx1rom[(z / 2) % gfx_size] >> (((z & 1) != 0) ? 4 : 0)) & 0x0f);
                    } else {
                        pen = gfx1rom[z % gfx_size];
                    }
                    if (gfx2rom != null) {
                        pen &= 0x0f;
                        if ((gfx2rom[(z / 8) % gfx2_size] & (1 << (z & 7))) != 0) {
                            pen |= 0x10;
                        }
                    }
                }
                if (x >= 0 && x <= 0x1ff && y >= 0 && y <= 0xef) {
                    if (clear != 0) {
                        layer[blitter.flags & 0x0007][x + y * 512] = clear_pen;
                    } else if (pen != trans_pen) {
                        if ((blitter.flags & 0x0007) == 0 && x == 0xa4 && y == 0x41) {
                            int i1 = 1;
                        }
                        layer[blitter.flags & 0x0007][x + y * 512] = (byte) (pen | pen_hi);
                    } else if (opaque != 0) {
                        layer[blitter.flags & 0x0007][x + y * 512] = (byte) 0xff;
                    }
                }
                z++;
            }
        }
    }

//#endregion

//#region Memory

    public static byte bkey0, bkey1, bkey2, bkey3, bkey4;
    public static byte bkey0_old, bkey1_old, bkey2_old, bkey3_old, bkey4_old;
    public static byte sbyte0, sbyte1, sbyte2, sbytec;
    public static byte sbyte0_old, sbyte1_old, sbyte2_old, sbytec_old;

    public static byte MReadOpByte_drgnwrld(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0 && address <= 0x7_ffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte MReadByte_drgnwrld(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0 && address <= 0x7_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) paletteram16[offset];
            }
        } else if (address >= 0x50_0000 && address <= 0x50_0001) {
            if (address == 0x50_0001) {
                result = sbytec;
            }
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            //if (address == 0x60_0001)
            {
                result = (byte) OKI6295.okim6295_status_0_lsb_r();
            }
        } else if (address >= 0x80_0002 && address <= 0x80_0003) {
                /*if (address == 0x80_0002)
                {
                    int i1 = 1;
                }
                else*/
            if (address == 0x80_0003) {
                result = drgnwrld_igs003_r();
            }
        } else if (address >= 0xa8_8000 && address <= 0xa8_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static short MReadOpWord_drgnwrld(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0 && address + 1 <= 0x7_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        }
        return result;
    }

    public static short MReadWord_drgnwrld(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0 && address + 1 <= 0x7_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset];
        } else if (address >= 0x50_0000 && address + 1 <= 0x50_0001) {
                /*if (Video.screenstate.frame_number >= 60&&Video.screenstate.frame_number<=61)
                {
                    result = (short)(0xfe);
                }
                else*/
            {
                result = sbytec;
            }
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x80_0002 && address + 1 <= 0x80_0003) {
            result = drgnwrld_igs003_r();
        } else if (address >= 0xa8_8000 && address + 1 <= 0xa8_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static int MReadOpLong_drgnwrld(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0 && address + 3 <= 0x7_ffff) {
            result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
        }
        return result;
    }

    public static int MReadLong_drgnwrld(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0 && address + 3 <= 0x7_ffff) {
            result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset] * 0x1_0000 + paletteram16[offset + 1];
        } else {
            int i1 = 1;
        }
        return result;
    }

    public static void MWriteByte_drgnwrld(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if ((address & 1) == 0) {
                priority_ram[offset] = (short) ((value << 8) | (priority_ram[offset] & 0xff));
            } else if ((address & 1) == 1) {
                priority_ram[offset] = (short) ((priority_ram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = address - 0x40_0000;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            if (address == 0x60_0001) {
                OKI6295.okim6295_data_0_lsb_w(value);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0001) {
            if (address == 0x70_0001) {
                igs_YM3812_control_port_0_w(value);
            }
        } else if (address >= 0x70_0002 && address <= 0x70_0003) {
            if (address == 0x70_0003) {
                igs_YM3812_write_port_0_w(value);
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0003) {
            int offset = address - 0x80_0000;
            drgnwrld_igs003_w(offset, value);
        } else if (address >= 0xa2_0000 && address <= 0xa2_0001) {
            int offset = address - 0xa2_0000;
            igs011_priority_w(offset, value);
        } else if (address >= 0xa4_0000 && address <= 0xa4_0001) {
            //igs_dips_w((ushort)value);
        } else if (address >= 0xa5_0000 && address <= 0xa5_0001) {
            int i1 = 1;
        } else if (address >= 0xa5_8000 && address <= 0xa5_8001) {
            int offset = address - 0xa5_8000;
            igs011_blit_x_w(offset, value);
        } else if (address >= 0xa5_8800 && address <= 0xa5_8801) {
            int offset = address - 0xa5_8800;
            igs011_blit_y_w(offset, value);
        } else if (address >= 0xa5_9000 && address <= 0xa5_9001) {
            int offset = address - 0xa5_9000;
            igs011_blit_w_w(offset, value);
        } else if (address >= 0xa5_9800 && address <= 0xa5_9801) {
            int offset = address - 0xa5_9800;
            igs011_blit_h_w(offset, value);
        } else if (address >= 0xa5_a000 && address <= 0xa5_a001) {
            int offset = address - 0xa5_a000;
            igs011_blit_gfx_lo_w(offset, value);
        } else if (address >= 0xa5_a800 && address <= 0xa5_a801) {
            int offset = address - 0xa5_a800;
            igs011_blit_gfx_hi_w(offset, value);
        } else if (address >= 0xa5_b000 && address <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address <= 0xa5_b801) {
            int offset = address - 0xa5_b800;
            igs011_blit_pen_w(offset, value);
        } else if (address >= 0xa5_c000 && address <= 0xa5_c001) {
            int offset = address - 0xa5_c000;
            igs011_blit_depth_w(offset, value);
        }
    }

    public static void MWriteWord_drgnwrld(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = value;
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            OKI6295.okim6295_data_0_lsb_w((byte) value);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            igs_YM3812_control_port_0_w((byte) value);
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0003) {
            igs_YM3812_write_port_0_w((byte) value);
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0003) {
            int offset = (address - 0x80_0000) / 2;
            drgnwrld_igs003_w(offset, value);
        } else if (address >= 0xa2_0000 && address + 1 <= 0xa2_0001) {
            igs011_priority_w(value);
        } else if (address >= 0xa4_0000 && address + 1 <= 0xa4_0001) {
            igs_dips_w(value);
        } else if (address >= 0xa5_0000 && address + 1 <= 0xa5_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address + 1 <= 0xa5_8001) {
            igs011_blit_x_w(value);
        } else if (address >= 0xa5_8800 && address + 1 <= 0xa5_8801) {
            igs011_blit_y_w(value);
        } else if (address >= 0xa5_9000 && address + 1 <= 0xa5_9001) {
            igs011_blit_w_w(value);
        } else if (address >= 0xa5_9800 && address + 1 <= 0xa5_9801) {
            igs011_blit_h_w(value);
        } else if (address >= 0xa5_a000 && address + 1 <= 0xa5_a001) {
            igs011_blit_gfx_lo_w(value);
        } else if (address >= 0xa5_a800 && address + 1 <= 0xa5_a801) {
            igs011_blit_gfx_hi_w(value);
        } else if (address >= 0xa5_b000 && address + 1 <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address + 1 <= 0xa5_b801) {
            igs011_blit_pen_w(value);
        } else if (address >= 0xa5_c000 && address + 1 <= 0xa5_c001) {
            igs011_blit_depth_w(value);
        }
    }

    public static void MWriteLong_drgnwrld(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 24);
            Generic.generic_nvram[offset + 1] = (byte) (value >> 16);
            Generic.generic_nvram[offset + 2] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = (short) (value >> 16);
            priority_ram[offset + 1] = (short) value;
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, (short) (value >> 16));
            igs011_palette(offset + 1, (short) value);
        } else if (address >= 0x80_0000 && address + 3 <= 0x80_0003) {
            drgnwrld_igs003_w(0, (short) (value >> 16));
            drgnwrld_igs003_w(1, (short) value);
        }
    }

//#endregion

//#region Memory2

    public static byte MReadByte_drgnwrld_igs012(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1610 && address2 <= 0x00_161f) {
                if (address2 % 2 == 1) {
                    result = igs012_prot_r();
                }
            } else if (address2 >= 0x00_1660 && address2 <= 0x00_166f) {
                if (address2 % 2 == 1) {
                    result = igs012_prot_r();
                }
            } else if (address >= 0x00_d4c0 && address <= 0x00_d4ff) {
                if (address2 % 2 == 0) {
                    result = (byte) (drgnwrldv20j_igs011_prot2_r() >> 8);
                }
            } else {
                result = MReadByte_drgnwrld(address);
            }
        } else {
            result = MReadByte_drgnwrld(address);
        }
        return result;
    }

    public static short MReadWord_drgnwrld_igs012(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0 && address + 1 <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1610 && address2 + 1 <= 0x00_161f) {
                result = igs012_prot_r();
            } else if (address2 >= 0x00_1660 && address2 + 1 <= 0x00_166f) {
                result = igs012_prot_r();
            } else if (address >= 0x00_d4c0 && address + 1 <= 0x00_d4ff) {
                result = drgnwrldv20j_igs011_prot2_r();
            } else {
                result = MReadWord_drgnwrld(address);
            }
        } else {
            result = MReadWord_drgnwrld(address);
        }
        return result;
    }

    public static int MReadLong_drgnwrld_igs012(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0 && address + 3 <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1610 && address2 + 3 <= 0x00_161f) {
                int i1 = 1;
            } else if (address2 >= 0x00_1660 && address2 + 3 <= 0x00_166f) {
                int i1 = 1;
            } else if (address >= 0x00_d4c0 && address + 3 <= 0x00_d4ff) {
                int i1 = 1;
            } else {
                result = MReadLong_drgnwrld(address);
            }
        } else {
            result = MReadLong_drgnwrld(address);
        }
        return result;
    }

    public static void MWriteByte_drgnwrld_igs012(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1600 && address2 <= 0x00_160f) {
                igs012_prot_swap_w(value);
            } else if (address2 >= 0x00_1620 && address2 <= 0x00_162f) {
                igs012_prot_dec_inc_w(value);
            } else if (address2 >= 0x00_1630 && address2 <= 0x00_163f) {
                igs012_prot_inc_w(value);
            } else if (address2 >= 0x00_1640 && address2 <= 0x00_164f) {
                igs012_prot_copy_w(value);
            } else if (address2 >= 0x00_1650 && address2 <= 0x00_165f) {
                igs012_prot_dec_copy_w(value);
            } else if (address2 >= 0x00_1670 && address2 <= 0x00_167f) {
                igs012_prot_mode_w(value);
            } else if (address >= 0x00_d400 && address <= 0x00_d43f) {
                igs011_prot2_dec_w();
            } else if (address >= 0x00_d440 && address <= 0x00_d47f) {
                drgnwrld_igs011_prot2_swap_w();
            } else if (address >= 0x00_d480 && address <= 0x00_d4bf) {
                igs011_prot2_reset_w();
            } else {
                MWriteByte_drgnwrld(address, value);
            }
        } else if (address >= 0x90_2000 && address <= 0x90_2fff) {
            igs012_prot_reset_w();
        } else {
            MWriteByte_drgnwrld(address, value);
        }
    }

    public static void MWriteWord_drgnwrld_igs012(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1600 && address2 + 1 <= 0x00_160f) {
                igs012_prot_swap_w(value);
            } else if (address2 >= 0x00_1620 && address2 + 1 <= 0x00_162f) {
                igs012_prot_dec_inc_w(value);
            } else if (address2 >= 0x00_1630 && address2 + 1 <= 0x00_163f) {
                igs012_prot_inc_w(value);
            } else if (address2 >= 0x00_1640 && address2 + 1 <= 0x00_164f) {
                igs012_prot_copy_w(value);
            } else if (address2 >= 0x00_1650 && address2 + 1 <= 0x00_165f) {
                igs012_prot_dec_copy_w(value);
            } else if (address2 >= 0x00_1670 && address2 + 1 <= 0x00_167f) {
                igs012_prot_mode_w(value);
            } else if (address >= 0x00_d400 && address + 1 <= 0x00_d43f) {
                igs011_prot2_dec_w();
            } else if (address >= 0x00_d440 && address + 1 <= 0x00_d47f) {
                drgnwrld_igs011_prot2_swap_w();
            } else if (address >= 0x00_d480 && address + 1 <= 0x00_d4bf) {
                igs011_prot2_reset_w();
            } else {
                MWriteWord_drgnwrld(address, value);
            }
        } else if (address >= 0x90_2000 && address + 1 <= 0x90_2fff) {
            igs012_prot_reset_w();
        } else {
            MWriteWord_drgnwrld(address, value);
        }
    }

    public static void MWriteLong_drgnwrld_igs012(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1600 && address2 + 3 <= 0x00_160f) {
                int i1 = 1;
            } else if (address2 >= 0x00_1620 && address2 + 3 <= 0x00_162f) {
                int i1 = 1;
            } else if (address2 >= 0x00_1630 && address2 + 3 <= 0x00_163f) {
                int i1 = 1;
            } else if (address2 >= 0x00_1640 && address2 + 3 <= 0x00_164f) {
                int i1 = 1;
            } else if (address2 >= 0x00_1650 && address2 + 3 <= 0x00_165f) {
                int i1 = 1;
            } else if (address2 >= 0x00_1670 && address2 + 3 <= 0x00_167f) {
                int i1 = 1;
            } else if (address >= 0x00_d400 && address + 3 <= 0x00_d43f) {
                int i1 = 1;
            } else if (address >= 0x00_d440 && address + 3 <= 0x00_d47f) {
                int i1 = 1;
            } else if (address >= 0x00_d480 && address + 3 <= 0x00_d4bf) {
                int i1 = 1;
            } else {
                MWriteLong_drgnwrld(address, value);
            }
        } else if (address >= 0x90_2000 && address + 3 <= 0x90_2fff) {
            int i1 = 1;
        } else {
            MWriteLong_drgnwrld(address, value);
        }
    }

    public static short MReadWord_drgnwrld_igs012_drgnwrldv21(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0 && address + 1 <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1610 && address2 + 1 <= 0x00_161f) {
                result = igs012_prot_r();
            } else if (address2 >= 0x00_1660 && address2 + 1 <= 0x00_166f) {
                result = igs012_prot_r();
            } else if (address >= 0x00_d4c0 && address + 1 <= 0x00_d4ff) {
                result = drgnwrldv21_igs011_prot2_r();
            } else {
                result = MReadWord_drgnwrld(address);
            }
        } else {
            result = MReadWord_drgnwrld(address);
        }
        return result;
    }

    public static byte MReadByte_lhb(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x01_0600 && address <= 0x01_07ff) {
            if (address % 2 == 0) {
                result = (byte) (lhb_igs011_prot2_r() >> 8);
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = address - 0x30_0000;
            if (address % 2 == 0) {
                result = igs011_layers_r1(offset / 2);
            } else if (address % 2 == 1) {
                result = igs011_layers_r2(offset / 2);
            }
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            //if (address == 0x60_0001)
            {
                result = (byte) OKI6295.okim6295_status_0_lsb_r();
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0001) {
            if (address == 0x70_0001) {
                result = sbytec;
            }
        } else if (address >= 0x70_0002 && address <= 0x70_0005) {
            int offset = (address - 0x70_0002) / 2;
            if (address % 2 == 0) {
                int i1 = 1;
            } else if (address % 2 == 1) {
                result = (byte) lhb_inputs_r(offset);
            }
        } else if (address >= 0x88_8000 && address <= 0x88_8001) {
            if (address % 2 == 0) {
                result = 0;
            } else if (address % 2 == 1) {
                result = igs_5_dips_r();
            }
        }
        return result;
    }

    public static short MReadOpWord_lhb(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x01_0600 && address + 1 <= 0x01_07ff) {
            result = lhb_igs011_prot2_r();
        } else if (address >= 0 && address + 1 <= 0x7_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        }
        return result;
    }

    public static short MReadWord_lhb(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x01_0600 && address + 1 <= 0x01_07ff) {
            result = lhb_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            result = sbytec;
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0005) {
            int offset = (address - 0x70_0002) / 2;
            result = lhb_inputs_r(offset);
        } else if (address >= 0x88_8000 && address + 1 <= 0x88_8001) {
            result = igs_5_dips_r();
        }
        return result;
    }

    public static int MReadLong_lhb(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset) * 0x1_0000 + igs011_layers_r(offset + 1);
        } else if (address >= 0x70_0002 && address + 3 <= 0x70_0005) {
            int offset = (address - 0x70_0002) / 2;
            result = lhb_inputs_r(offset) * 0x1_0000 + lhb_inputs_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_lhb(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0x01_0000 && address <= 0x01_0001) {
            if (address % 2 == 0) {
                lhb_okibank_w(value);
            }
        } else if (address >= 0x01_0200 && address <= 0x01_03ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x01_0400 && address <= 0x01_05ff) {
            int offset = (address - 0x01_0400) / 2;
            lhb_igs011_prot2_swap_w(offset);
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                priority_ram[offset] = (short) ((value << 8) | (priority_ram[offset] & 0xff));
            } else if (address % 2 == 1) {
                priority_ram[offset] = (short) ((priority_ram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = address - 0x30_0000;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = address - 0x40_0000;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            if (address == 0x60_0001) {
                OKI6295.okim6295_data_0_lsb_w(value);
            }
        } else if (address >= 0x70_0002 && address <= 0x70_0003) {
            int offset = address - 0x70_0002;
            lhb_inputs_w(offset, value);
        } else if (address >= 0x82_0000 && address <= 0x82_0001) {
            int offset = address - 0x82_0000;
            igs011_priority_w(offset, value);
        } else if (address >= 0x83_8000 && address <= 0x83_8001) {
            int offset = address - 0x83_8000;
            lhb_irq_enable_w(offset, value);
        } else if (address >= 0x84_0000 && address <= 0x84_0001) {
            int offset = address - 0x84_0000;
            igs_dips_w(offset, value);
        } else if (address >= 0x85_0000 && address <= 0x85_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0x85_8000 && address <= 0x85_8001) {
            int offset = address - 0x85_8000;
            igs011_blit_x_w(offset, value);
        } else if (address >= 0x85_8800 && address <= 0x85_8801) {
            int offset = address - 0x85_8800;
            igs011_blit_y_w(offset, value);
        } else if (address >= 0x85_9000 && address <= 0x85_9001) {
            int offset = address - 0x85_9000;
            igs011_blit_w_w(offset, value);
        } else if (address >= 0x85_9800 && address <= 0x85_9801) {
            int offset = address - 0x85_9800;
            igs011_blit_h_w(offset, value);
        } else if (address >= 0x85_a000 && address <= 0x85_a001) {
            int offset = address - 0x85_a000;
            igs011_blit_gfx_lo_w(offset, value);
        } else if (address >= 0x85_a800 && address <= 0x85_a801) {
            int offset = address - 0x85_a800;
            igs011_blit_gfx_hi_w(offset, value);
        } else if (address >= 0x85_b000 && address <= 0x85_b001) {
            int i1 = 1;
            //igs011_blit_flags_w((byte)value);
        } else if (address >= 0x85_b800 && address <= 0x85_b801) {
            int offset = address - 0x85_b800;
            igs011_blit_pen_w(offset, value);
        } else if (address >= 0x85_c000 && address <= 0x85_c001) {
            int offset = address - 0x85_c000;
            igs011_blit_depth_w(offset, value);
        }
    }

    public static void MWriteWord_lhb(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0x01_0000 && address + 1 <= 0x01_0001) {
            lhb_okibank_w(value);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = value;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            OKI6295.okim6295_data_0_lsb_w((byte) value);
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0003) {
            lhb_inputs_w(value);
        } else if (address >= 0x82_0000 && address + 1 <= 0x82_0001) {
            igs011_priority_w(value);
        } else if (address >= 0x83_8000 && address + 1 <= 0x83_8001) {
            lhb_irq_enable_w(value);
        } else if (address >= 0x84_0000 && address + 1 <= 0x84_0001) {
            igs_dips_w(value);
        } else if (address >= 0x85_0000 && address <= 0x85_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0x85_8000 && address + 1 <= 0x85_8001) {
            igs011_blit_x_w(value);
        } else if (address >= 0x85_8800 && address + 1 <= 0x85_8801) {
            igs011_blit_y_w(value);
        } else if (address >= 0x85_9000 && address + 1 <= 0x85_9001) {
            igs011_blit_w_w(value);
        } else if (address >= 0x85_9800 && address + 1 <= 0x85_9801) {
            igs011_blit_h_w(value);
        } else if (address >= 0x85_a000 && address + 1 <= 0x85_a001) {
            igs011_blit_gfx_lo_w(value);
        } else if (address >= 0x85_a800 && address + 1 <= 0x85_a801) {
            igs011_blit_gfx_hi_w(value);
        } else if (address >= 0x85_b000 && address + 1 <= 0x85_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0x85_b800 && address + 1 <= 0x85_b801) {
            igs011_blit_pen_w(value);
        } else if (address >= 0x85_c000 && address + 1 <= 0x85_c001) {
            igs011_blit_depth_w(value);
        }
    }

    public static void MWriteLong_lhb(int address, int value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 3 <= prot1_addr + 7) {
            int i1 = 1;
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 24);
            Generic.generic_nvram[offset + 1] = (byte) (value >> 16);
            Generic.generic_nvram[offset + 2] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = (short) (value >> 16);
            priority_ram[offset + 1] = (short) value;
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, (short) (value >> 16));
            igs011_layers_w(offset + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, (short) (value >> 16));
            igs011_palette(offset + 1, (short) value);
        }
    }

    public static short MReadWord_dbc(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x01_0600 && address + 1 <= 0x01_07ff) {
            result = dbc_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            result = sbytec;
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0005) {
            int offset = (address - 0x70_0002) / 2;
            result = lhb_inputs_r(offset);
        } else if (address >= 0x88_8000 && address + 1 <= 0x88_8001) {
            result = igs_5_dips_r();
        }
        return result;
    }

    public static short MReadWord_ryukobou(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x01_0600 && address + 1 <= 0x01_07ff) {
            result = ryukobou_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0001) {
            result = sbytec;
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0005) {
            int offset = (address - 0x70_0002) / 2;
            result = lhb_inputs_r(offset);
        } else if (address >= 0x88_8000 && address + 1 <= 0x88_8001) {
            result = igs_5_dips_r();
        }
        return result;
    }

    public static byte MReadOpByte_lhb2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x02_0400 && address <= 0x02_05ff) {
            if (address % 2 == 1) {
                result = (byte) lhb2_igs011_prot2_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_lhb2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x02_0400 && address <= 0x02_05ff) {
            if (address % 2 == 1) {
                result = (byte) lhb2_igs011_prot2_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0001) {
            result = (byte) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x20_8002 && address <= 0x20_8003) {
            if (address % 2 == 1) {
                result = (byte) lhb2_igs003_r();
            }
        } else if (address >= 0x20_c000 && address <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x21_0000 && address <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) paletteram16[offset];
            }
        } else if (address >= 0x21_4000 && address <= 0x21_4001) {
            if (address % 2 == 1) {
                result = sbytec;
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = igs011_layers_r1(offset / 2);
            } else if (address % 2 == 1) {
                result = igs011_layers_r2(offset / 2);
            }
        } else if (address >= 0xa8_8000 && address <= 0xa8_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static short MReadOpWord_lhb2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x02_0400 && address + 1 <= 0x02_05ff) {
            result = lhb2_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_lhb2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x02_0400 && address + 1 <= 0x02_05ff) {
            result = lhb2_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x20_8002 && address + 1 <= 0x20_8003) {
            result = lhb2_igs003_r();
        } else if (address >= 0x20_c000 && address + 1 <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x21_0000 && address + 1 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            result = paletteram16[offset];
        } else if (address >= 0x21_4000 && address + 1 <= 0x21_4001) {
            result = sbytec;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0xa8_8000 && address + 1 <= 0xa8_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static int MReadOpLong_lhb2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_lhb2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_c000 && address + 3 <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x21_0000 && address + 3 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            result = paletteram16[offset] * 0x1_0000 + paletteram16[offset + 1];
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset) * 0x1_0000 + igs011_layers_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_lhb2(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0x02_0000 && address <= 0x02_01ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x02_0200 && address <= 0x02_03ff) {
            int offset = (address - 0x02_0200) / 2;
            lhb_igs011_prot2_swap_w(offset);
        } else if (address >= 0x02_0600 && address <= 0x02_07ff) {
            igs011_prot2_reset_w();
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0001) {
            if (address % 2 == 1) {
                OKI6295.okim6295_data_0_lsb_w(value);
            }
        } else if (address >= 0x20_4000 && address <= 0x20_4003) {
            int offset = (address - 0x20_4000) / 2;
            if (address % 2 == 1) {
                YM2413.ym2413_write(offset, value);
            }
        } else if (address >= 0x20_8000 && address <= 0x20_8003) {
            int offset = (address - 0x20_8000) / 2;
            if (address % 2 == 0) {
                lhb2_igs003_w1(offset, value);
            } else if (address % 2 == 1) {
                lhb2_igs003_w2(offset, value);
            }
        } else if (address >= 0x20_c000 && address <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            if (address % 2 == 0) {
                priority_ram[offset] = (short) ((value << 8) | (priority_ram[offset] & 0xff));
            } else if (address % 2 == 1) {
                priority_ram[offset] = (short) ((priority_ram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x21_0000 && address <= 0x21_1fff) {
            int offset = address - 0x21_0000;
            igs011_palette(offset, value);
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = address - 0x30_0000;
            igs011_layers_w(offset, value);
        } else if (address >= 0xa2_0000 && address <= 0xa2_0001) {
            int offset = address - 0xa2_0000;
            igs011_priority_w(offset, value);
        } else if (address >= 0xa4_0000 && address <= 0xa4_0001) {
            int offset = address - 0xa4_0000;
            igs_dips_w(offset, value);
        } else if (address >= 0xa5_0000 && address <= 0xa5_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address <= 0xa5_8001) {
            int offset = address - 0xa5_8000;
            igs011_blit_x_w(offset, value);
        } else if (address >= 0xa5_8800 && address <= 0xa5_8801) {
            int offset = address - 0xa5_8800;
            igs011_blit_y_w(offset, value);
        } else if (address >= 0xa5_9000 && address <= 0xa5_9001) {
            int offset = address - 0xa5_9000;
            igs011_blit_w_w(offset, value);
        } else if (address >= 0xa5_9800 && address <= 0xa5_9801) {
            int offset = address - 0xa5_9800;
            igs011_blit_h_w(offset, value);
        } else if (address >= 0xa5_a000 && address <= 0xa5_a001) {
            int offset = address - 0xa5_a000;
            igs011_blit_gfx_lo_w(offset, value);
        } else if (address >= 0xa5_a800 && address <= 0xa5_a801) {
            int offset = address - 0xa5_a800;
            igs011_blit_gfx_hi_w(offset, value);
        } else if (address >= 0xa5_b000 && address <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address <= 0xa5_b801) {
            int offset = address - 0xa5_b800;
            igs011_blit_pen_w(offset, value);
        } else if (address >= 0xa5_c000 && address <= 0xa5_c001) {
            int offset = address - 0xa5_c000;
            igs011_blit_depth_w(offset, value);
        }
    }

    public static void MWriteWord_lhb2(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0x02_0000 && address + 1 <= 0x02_01ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x02_0200 && address + 1 <= 0x02_03ff) {
            int offset = (address - 0x02_0200) / 2;
            lhb_igs011_prot2_swap_w(offset);
        } else if (address >= 0x02_0600 && address + 1 <= 0x02_07ff) {
            igs011_prot2_reset_w();
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0001) {
            OKI6295.okim6295_data_0_lsb_w((byte) value);
        } else if (address >= 0x20_4000 && address + 1 <= 0x20_4003) {
            int offset = (address - 0x20_4000) / 2;
            YM2413.ym2413_write(offset, value);
        } else if (address >= 0x20_8000 && address + 1 <= 0x20_8003) {
            int offset = (address - 0x20_8000) / 2;
            lhb2_igs003_w(offset, value);
        } else if (address >= 0x20_c000 && address + 1 <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            priority_ram[offset] = value;
        } else if (address >= 0x21_0000 && address + 1 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            igs011_palette(offset, value);
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, value);
        } else if (address >= 0xa2_0000 && address + 1 <= 0xa2_0001) {
            igs011_priority_w(value);
        } else if (address >= 0xa4_0000 && address + 1 <= 0xa4_0001) {
            igs_dips_w(value);
        } else if (address >= 0xa5_0000 && address + 1 <= 0xa5_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address + 1 <= 0xa5_8001) {
            igs011_blit_x_w(value);
        } else if (address >= 0xa5_8800 && address + 1 <= 0xa5_8801) {
            igs011_blit_y_w(value);
        } else if (address >= 0xa5_9000 && address + 1 <= 0xa5_9001) {
            igs011_blit_w_w(value);
        } else if (address >= 0xa5_9800 && address + 1 <= 0xa5_9801) {
            igs011_blit_h_w(value);
        } else if (address >= 0xa5_a000 && address + 1 <= 0xa5_a001) {
            igs011_blit_gfx_lo_w(value);
        } else if (address >= 0xa5_a800 && address + 1 <= 0xa5_a801) {
            igs011_blit_gfx_hi_w(value);
        } else if (address >= 0xa5_b000 && address + 1 <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address + 1 <= 0xa5_b801) {
            igs011_blit_pen_w(value);
        } else if (address >= 0xa5_c000 && address + 1 <= 0xa5_c001) {
            igs011_blit_depth_w(value);
        }
    }

    public static void MWriteLong_lhb2(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + +3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 24);
            Generic.generic_nvram[offset + 1] = (byte) (value >> 16);
            Generic.generic_nvram[offset + 2] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 3] = (byte) value;
        } else if (address >= 0x20_c000 && address + 3 <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            priority_ram[offset] = (short) (value >> 16);
            priority_ram[offset + 1] = (short) value;
        } else if (address >= 0x21_0000 && address + 3 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            igs011_palette(offset, (short) (value >> 16));
            igs011_palette(offset + 1, (short) value);
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, (short) (value >> 16));
            igs011_layers_w(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_xymg(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_xymg(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x01_0600 && address <= 0x01_07ff) {
            if (address % 2 == 0) {
                result = (byte) (lhb2_igs011_prot2_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) lhb2_igs011_prot2_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset];
        } else if (address >= 0x1f_0000 && address <= 0x1f_3fff) {
            int offset = address - 0x1f_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = igs011_layers_r1(offset / 2);
            } else if (address % 2 == 1) {
                result = igs011_layers_r2(offset / 2);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) paletteram16[offset];
            }
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            result = (byte) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x70_0002 && address <= 0x70_0003) {
            result = xymg_igs003_r();
        } else if (address >= 0x88_8000 && address <= 0x88_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static short MReadOpWord_xymg(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_xymg(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x01_0600 && address + 1 <= 0x01_07ff) {
            result = igs011_prot1_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Memory.mainram[offset] * 0x100 + Memory.mainram[offset + 1]);
        } else if (address >= 0x1f_0000 && address + 1 <= 0x1f_3fff) {
            int offset = address - 0x1f_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset];
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x70_0002 && address + 1 <= 0x70_0003) {
            result = xymg_igs003_r();
        } else if (address >= 0x88_8000 && address + 1 <= 0x88_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static int MReadOpLong_xymg(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address <= 0x01_0001) {
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

    public static int MReadLong_xymg(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x01_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Memory.mainram[offset] * 0x100_0000 + Memory.mainram[offset + 1] * 0x1_0000 + Memory.mainram[offset + 2] * 0x100 + Memory.mainram[offset + 3];
        } else if (address >= 0x1f_0000 && address + 3 <= 0x1f_3fff) {
            int offset = address - 0x1f_0000;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset) * 0x1_0000 + igs011_layers_r(offset + 1);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset] * 0x1_0000 + paletteram16[offset + 1];
        }
        return result;
    }

    public static void MWriteByte_xymg(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0x01_0000 && address <= 0x01_0001) {
            if (address % 2 == 0) {
                lhb_okibank_w(value);
            }
        } else if (address >= 0x01_0200 && address <= 0x01_03ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x01_0400 && address <= 0x01_05ff) {
            int offset = (address - 0x01_0400) / 2;
            lhb_igs011_prot2_swap_w(offset);
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = value;
        } else if (address >= 0x1f_0000 && address <= 0x1f_3fff) {
            int offset = address - 0x1f_0000;
            Generic.generic_nvram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                priority_ram[offset] = (short) ((value << 8) | (priority_ram[offset] & 0xff));
            } else if (address % 2 == 1) {
                priority_ram[offset] = (short) ((priority_ram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = address - 0x30_0000;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = address - 0x40_0000;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            if (address % 2 == 1) {
                OKI6295.okim6295_data_0_lsb_w(value);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0003) {
            int offset = (address - 0x70_0000) / 2;
            xymg_igs003_w(offset, value);
        } else if (address >= 0x82_0000 && address <= 0x82_0001) {
            int offset = address - 0x82_0000;
            igs011_priority_w(offset, value);
        } else if (address >= 0x84_0000 && address <= 0x84_0001) {
            igs_dips_w(value);
        } else if (address >= 0x85_0000 && address <= 0x85_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0x85_8000 && address <= 0x85_8001) {
            int offset = address - 0x85_8000;
            igs011_blit_x_w(offset, value);
        } else if (address >= 0x85_8800 && address <= 0x85_8801) {
            int offset = address - 0x85_8800;
            igs011_blit_y_w(offset, value);
        } else if (address >= 0x85_9000 && address <= 0x85_9001) {
            int offset = address - 0x85_9000;
            igs011_blit_w_w(offset, value);
        } else if (address >= 0x85_9800 && address <= 0x85_9801) {
            int offset = address - 0x85_9800;
            igs011_blit_h_w(offset, value);
        } else if (address >= 0x85_a000 && address <= 0x85_a001) {
            int offset = address - 0x85_a000;
            igs011_blit_gfx_lo_w(offset, value);
        } else if (address >= 0x85_a800 && address <= 0x85_a801) {
            int offset = address - 0x85_a800;
            igs011_blit_gfx_hi_w(offset, value);
        } else if (address >= 0x85_b000 && address <= 0x85_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0x85_b800 && address <= 0x85_b801) {
            int offset = address - 0x85_b800;
            igs011_blit_pen_w(offset, value);
        } else if (address >= 0x85_c000 && address <= 0x85_c001) {
            int offset = address - 0x85_c000;
            igs011_blit_depth_w(offset, value);
        }
    }

    public static void MWriteWord_xymg(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0x01_0000 && address + 1 <= 0x01_0001) {
            lhb_okibank_w(value);
        } else if (address >= 0x01_0200 && address + 1 <= 0x01_03ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x01_0400 && address + 1 <= 0x01_05ff) {
            int offset = (address - 0x01_0400) / 2;
            lhb_igs011_prot2_swap_w(offset);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 8);
            Memory.mainram[offset + 1] = (byte) value;
        } else if (address >= 0x1f_0000 && address + 1 <= 0x1f_3fff) {
            int offset = address - 0x1f_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = value;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            OKI6295.okim6295_data_0_lsb_w((byte) value);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0003) {
            int offset = (address - 0x70_0000) / 2;
            xymg_igs003_w(offset, value);
        } else if (address >= 0x82_0000 && address + 1 <= 0x82_0001) {
            igs011_priority_w(value);
        } else if (address >= 0x84_0000 && address + 1 <= 0x84_0001) {
            igs_dips_w(value);
        } else if (address >= 0x85_0000 && address + 1 <= 0x85_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0x85_8000 && address + 1 <= 0x85_8001) {
            igs011_blit_x_w(value);
        } else if (address >= 0x85_8800 && address + 1 <= 0x85_8801) {
            igs011_blit_y_w(value);
        } else if (address >= 0x85_9000 && address + 1 <= 0x85_9001) {
            igs011_blit_w_w(value);
        } else if (address >= 0x85_9800 && address + 1 <= 0x85_9801) {
            igs011_blit_h_w(value);
        } else if (address >= 0x85_a000 && address + 1 <= 0x85_a001) {
            igs011_blit_gfx_lo_w(value);
        } else if (address >= 0x85_a800 && address + 1 <= 0x85_a801) {
            igs011_blit_gfx_hi_w(value);
        } else if (address >= 0x85_b000 && address + 1 <= 0x85_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0x85_b800 && address + 1 <= 0x85_b801) {
            igs011_blit_pen_w(value);
        } else if (address >= 0x85_c000 && address + 1 <= 0x85_c001) {
            igs011_blit_depth_w(value);
        }
    }

    public static void MWriteLong_xymg(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Memory.mainram[offset] = (byte) (value >> 24);
            Memory.mainram[offset + 1] = (byte) (value >> 16);
            Memory.mainram[offset + 2] = (byte) (value >> 8);
            Memory.mainram[offset + 3] = (byte) value;
        } else if (address >= 0x1f_0000 && address + 3 <= 0x1f_3fff) {
            int offset = address - 0x1f_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 24);
            Generic.generic_nvram[offset + 1] = (byte) (value >> 16);
            Generic.generic_nvram[offset + 2] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = (short) (value >> 16);
            priority_ram[offset + 1] = (short) value;
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, (short) (value >> 16));
            igs011_layers_w(offset + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, (short) (value >> 16));
            igs011_palette(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_wlcc(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_wlcc(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x51_8800 && address <= 0x51_89ff) {
            result = (byte) igs011_prot2_reset_r();
        } else if (address >= 0x51_9000 && address <= 0x51_95ff) {
            if (address % 2 == 0) {
                result = (byte) (lhb2_igs011_prot2_r() >> 8);
            } else if (address % 2 == 1) {
                result = (byte) lhb2_igs011_prot2_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = igs011_layers_r1(offset / 2);
            } else if (address % 2 == 1) {
                result = igs011_layers_r2(offset / 2);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) paletteram16[offset];
            }
        } else if (address >= 0x52_0000 && address <= 0x52_0001) {
            if (address % 2 == 1) {
                result = sbytec;
            }
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            result = (byte) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x80_0002 && address <= 0x80_0003) {
            if (address % 2 == 1) {
                result = wlcc_igs003_r();
            }
        } else if (address >= 0xa8_8000 && address <= 0xa8_8001) {
            result = igs_4_dips_r();
        }
        return result;
    }

    public static short MReadOpWord_wlcc(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_wlcc(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x51_8800 && address + 1 <= 0x51_89ff) {
            result = (short) igs011_prot2_reset_r();
        } else if (address >= 0x51_9000 && address + 1 <= 0x51_95ff) {
            result = lhb2_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset];
        } else if (address >= 0x52_0000 && address + 1 <= 0x52_0001) {
            int offset = (address - 0x52_0000) / 2;
            result = sbytec;
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            int offset = (address - 0x60_0000) / 2;
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x80_0002 && address + 1 <= 0x80_0003) {
            int offset = (address - 0x80_0002) / 2;
            result = wlcc_igs003_r();
        } else if (address >= 0xa8_8000 && address + 1 <= 0xa8_8001) {
            int offset = (address - 0xa8_8000) / 2;
            result = igs_4_dips_r();
        }
        return result;
    }

    public static int MReadOpLong_wlcc(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_wlcc(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = (address - 0x10_0000) / 2;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset) * 0x1_0000 + igs011_layers_r(offset + 1);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset] * 0x1_0000 + paletteram16[offset + 1];
        }
        return result;
    }

    public static void MWriteByte_wlcc(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0x51_8000 && address <= 0x51_81ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x51_8200 && address <= 0x51_83ff) {
            int offset = (address - 0x51_8200) / 2;
            wlcc_igs011_prot2_swap_w(offset);
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                priority_ram[offset] = (short) ((value << 8) | (priority_ram[offset] & 0xff));
            } else if (address % 2 == 1) {
                priority_ram[offset] = (short) ((priority_ram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = address - 0x30_0000;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = address - 0x40_0000;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address <= 0x60_0001) {
            if (address % 2 == 1) {
                OKI6295.okim6295_data_0_lsb_w(value);
            }
        } else if (address >= 0x80_0000 && address <= 0x80_0003) {
            int offset = (address - 0x80_0000) / 2;
            if (address % 2 == 0) {
                wlcc_igs003_w1(offset, value);
            } else if (address % 2 == 1) {
                wlcc_igs003_w2(offset, value);
            }
        } else if (address >= 0xa2_0000 && address <= 0xa2_0001) {
            int offset = address - 0xa2_0000;
            igs011_priority_w(offset, value);
        } else if (address >= 0xa4_0000 && address <= 0xa4_0001) {
            igs_dips_w(value);
        } else if (address >= 0xa5_0000 && address <= 0xa5_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address <= 0xa5_8001) {
            int offset = address - 0xa5_8000;
            igs011_blit_x_w(offset, value);
        } else if (address >= 0xa5_8800 && address <= 0xa5_8801) {
            int offset = address - 0xa5_8800;
            igs011_blit_y_w(offset, value);
        } else if (address >= 0xa5_9000 && address <= 0xa5_9001) {
            int offset = address - 0xa5_9000;
            igs011_blit_w_w(offset, value);
        } else if (address >= 0xa5_9800 && address <= 0xa5_9801) {
            int offset = address - 0xa5_9800;
            igs011_blit_h_w(offset, value);
        } else if (address >= 0xa5_a000 && address <= 0xa5_a001) {
            int offset = address - 0xa5_a000;
            igs011_blit_gfx_lo_w(offset, value);
        } else if (address >= 0xa5_a800 && address <= 0xa5_a801) {
            int offset = address - 0xa5_a800;
            igs011_blit_gfx_hi_w(offset, value);
        } else if (address >= 0xa5_b000 && address <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address <= 0xa5_b801) {
            int offset = address - 0xa5_b800;
            igs011_blit_pen_w(offset, value);
        } else if (address >= 0xa5_c000 && address <= 0xa5_c001) {
            int offset = address - 0xa5_c000;
            igs011_blit_depth_w(offset, value);
        }
    }

    public static void MWriteWord_wlcc(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0x51_8000 && address + 1 <= 0x51_81ff) {
            igs011_prot2_inc_w();
        } else if (address >= 0x51_8200 && address + 1 <= 0x51_83ff) {
            int offset = (address - 0x51_8200) / 2;
            wlcc_igs011_prot2_swap_w(offset);
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = value;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0001) {
            OKI6295.okim6295_data_0_lsb_w((byte) value);
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0003) {
            int offset = (address - 0x80_0000) / 2;
            wlcc_igs003_w(offset, value);
        } else if (address >= 0xa2_0000 && address + 1 <= 0xa2_0001) {
            igs011_priority_w(value);
        } else if (address >= 0xa4_0000 && address + 1 <= 0xa4_0001) {
            int offset = (address - 0xa4_0000) / 2;
            igs_dips_w(value);
        } else if (address >= 0xa5_0000 && address + 1 <= 0xa5_0001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address + 1 <= 0xa5_8001) {
            igs011_blit_x_w(value);
        } else if (address >= 0xa5_8800 && address + 1 <= 0xa5_8801) {
            igs011_blit_y_w(value);
        } else if (address >= 0xa5_9000 && address + 1 <= 0xa5_9001) {
            igs011_blit_w_w(value);
        } else if (address >= 0xa5_9800 && address + 1 <= 0xa5_9801) {
            igs011_blit_h_w(value);
        } else if (address >= 0xa5_a000 && address + 1 <= 0xa5_a001) {
            igs011_blit_gfx_lo_w(value);
        } else if (address >= 0xa5_a800 && address + 1 <= 0xa5_a801) {
            igs011_blit_gfx_hi_w(value);
        } else if (address >= 0xa5_b000 && address + 1 <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address + 1 <= 0xa5_b801) {
            igs011_blit_pen_w(value);
        } else if (address >= 0xa5_c000 && address + 1 <= 0xa5_c001) {
            igs011_blit_depth_w(value);
        }
    }

    public static void MWriteLong_wlcc(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 24);
            Generic.generic_nvram[offset + 1] = (byte) (value >> 16);
            Generic.generic_nvram[offset + 2] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = (short) (value >> 16);
            priority_ram[offset + 1] = (short) value;
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, (short) (value >> 16));
            igs011_layers_w(offset + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, (short) (value >> 16));
            igs011_palette(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_vbowl(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_vbowl(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1610 && address2 <= 0x00_161f) {
                if (address2 % 2 == 1) {
                    result = igs012_prot_r();
                }
            } else if (address2 >= 0x00_1660 && address2 <= 0x00_166f) {
                if (address2 % 2 == 1) {
                    result = igs012_prot_r();
                }
            } else if (address >= 0x00_d4c0 && address <= 0x00_d4ff) {
                if (address2 % 2 == 0) {
                    result = (byte) (drgnwrldv20j_igs011_prot2_r() >> 8);
                }
            }
        } else if (address >= 0x50_f600 && address <= 0x50_f7ff) {
            int offset = (address - 0x50_f600) / 2;
            if (address % 2 == 0) {
                result = (byte) (vbowl_igs011_prot2_r() >> 8);
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = igs011_layers_r1(offset / 2);
            } else if (address % 2 == 1) {
                result = igs011_layers_r2(offset / 2);
            }
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) paletteram16[offset];
            }
        } else if (address >= 0x52_0000 && address <= 0x52_0001) {
            if (address % 2 == 1) {
                result = sbytec;
            }
        } else if (address >= 0x60_0000 && address <= 0x60_0007) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                result = ics2115_0_word_r1(offset);
            } else if (address % 2 == 1) {
                result = ics2115_0_word_r2(offset);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0003) {
            int offset = (address - 0x70_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (vbowl_trackball[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) vbowl_trackball[offset];
            }
        } else if (address >= 0x80_0002 && address <= 0x80_0003) {
            if (address % 2 == 1) {
                result = vbowl_igs003_r();
            }
        } else if (address >= 0xa8_0000 && address <= 0xa8_0001) {
            int offset = (address - 0xa8_0000) / 2;
            result = (byte) vbowl_unk_r();
        } else if (address >= 0xa8_8000 && address <= 0xa8_8001) {
            int offset = (address - 0xa8_8000) / 2;
            result = igs_4_dips_r();
        } else if (address >= 0xa9_0000 && address <= 0xa9_0001) {
            int offset = (address - 0xa9_0000) / 2;
            result = (byte) vbowl_unk_r();
        } else if (address >= 0xa9_8000 && address <= 0xa9_8001) {
            int offset = (address - 0xa9_8000) / 2;
            result = (byte) vbowl_unk_r();
        }
        return result;
    }

    public static short MReadOpWord_vbowl(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_vbowl(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1610 && address2 + 1 <= 0x00_161f) {
                result = igs012_prot_r();
            } else if (address2 >= 0x00_1660 && address2 + 1 <= 0x00_166f) {
                result = igs012_prot_r();
            } else if (address >= 0x00_d4c0 && address + 1 <= 0x00_d4ff) {
                result = drgnwrldv20j_igs011_prot2_r();
            }
        } else if (address >= 0x50_f600 && address + 1 <= 0x50_f7ff) {
            result = vbowl_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset];
        } else if (address >= 0x52_0000 && address + 1 <= 0x52_0001) {
            result = sbytec;
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0007) {
            int offset = (address - 0x60_0000) / 2;
            result = ics2115_0_word_r(offset);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0003) {
            int offset = (address - 0x70_0000) / 2;
            result = vbowl_trackball[offset];
        } else if (address >= 0x80_0002 && address + 1 <= 0x80_0003) {
            int offset = (address - 0x80_0002) / 2;
            result = vbowl_igs003_r();
        } else if (address >= 0xa8_0000 && address + 1 <= 0xa8_0001) {
            int offset = (address - 0xa8_0000) / 2;
            result = vbowl_unk_r();
        } else if (address >= 0xa8_8000 && address + 1 <= 0xa8_8001) {
            int offset = (address - 0xa8_8000) / 2;
            result = igs_4_dips_r();
        } else if (address >= 0xa9_0000 && address + 1 <= 0xa9_0001) {
            int offset = (address - 0xa9_0000) / 2;
            result = vbowl_unk_r();
        } else if (address >= 0xa9_8000 && address + 1 <= 0xa9_8001) {
            int offset = (address - 0xa9_8000) / 2;
            result = vbowl_unk_r();
        }
        return result;
    }

    public static int MReadOpLong_vbowl(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_vbowl(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x00_0000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = (address - 0x10_0000) / 2;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset) * 0x1_0000 + igs011_layers_r(offset + 1);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            result = paletteram16[offset] * 0x1_0000 + paletteram16[offset + 1];
        }
        return result;
    }

    public static void MWriteByte_vbowl(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w1(offset, value);
        } else if (address >= 0 && address <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1600 && address2 <= 0x00_160f) {
                igs012_prot_swap_w(value);
            } else if (address2 >= 0x00_1620 && address2 <= 0x00_162f) {
                igs012_prot_dec_inc_w(value);
            } else if (address2 >= 0x00_1630 && address2 <= 0x00_163f) {
                igs012_prot_inc_w(value);
            } else if (address2 >= 0x00_1640 && address2 <= 0x00_164f) {
                igs012_prot_copy_w(value);
            } else if (address2 >= 0x00_1650 && address2 <= 0x00_165f) {
                igs012_prot_dec_copy_w(value);
            } else if (address2 >= 0x00_1670 && address2 <= 0x00_167f) {
                igs012_prot_mode_w(value);
            } else if (address >= 0x00_d400 && address <= 0x00_d43f) {
                igs011_prot2_dec_w();
            } else if (address >= 0x00_d440 && address <= 0x00_d47f) {
                drgnwrld_igs011_prot2_swap_w();
            } else if (address >= 0x00_d480 && address <= 0x00_d4bf) {
                igs011_prot2_reset_w();
            }
        } else if (address >= 0x50_f000 && address <= 0x50_f1ff) {
            igs011_prot2_dec_w();
        } else if (address >= 0x50_f200 && address <= 0x50_f3ff) {
            int offset = (address - 0x50_f200) / 2;
            vbowl_igs011_prot2_swap_w(offset);
        } else if (address >= 0x50_f400 && address <= 0x50_f5ff) {
            igs011_prot2_reset_w();
        } else if (address >= 0x90_2000 && address <= 0x90_2fff) {
            igs012_prot_reset_w();
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = value;
        } else if (address >= 0x20_0000 && address <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            if (address % 2 == 0) {
                priority_ram[offset] = (short) ((value << 8) | (priority_ram[offset] & 0xff));
            } else if (address % 2 == 1) {
                priority_ram[offset] = (short) ((priority_ram[offset] & 0xff00) | value);
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = address - 0x30_0000;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address <= 0x40_1fff) {
            int offset = address - 0x40_0000;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address <= 0x60_0007) {
            int offset = (address - 0x60_0000) / 2;
            if (address % 2 == 0) {
                ics2115_0_word_w1(offset, value);
            } else if (address % 2 == 1) {
                ics2115_0_word_w2(offset, value);
            }
        } else if (address >= 0x70_0000 && address <= 0x70_0003) {
            int offset = (address - 0x70_0000) / 2;
            if (address % 2 == 0) {
                vbowl_trackball[offset] = (short) ((value << 8) | vbowl_trackball[offset] & 0xff);
            } else if (address % 2 == 1) {
                vbowl_trackball[offset] = (short) ((vbowl_trackball[offset] & 0xff00) | value);
            }
        } else if (address >= 0x70_0004 && address <= 0x70_0005) {
            vbowl_pen_hi_w(value);
        } else if (address >= 0x80_0000 && address <= 0x80_0003) {
            int offset = (address - 0x80_0000) / 2;
            vbowl_igs003_w(offset, value);
        } else if (address >= 0xa0_0000 && address <= 0xa0_0001) {
            vbowl_link_0_w();
        } else if (address >= 0xa0_8000 && address <= 0xa0_8001) {
            vbowl_link_1_w();
        } else if (address >= 0xa1_0000 && address <= 0xa1_0001) {
            vbowl_link_2_w();
        } else if (address >= 0xa1_8000 && address <= 0xa1_8001) {
            vbowl_link_3_w();
        } else if (address >= 0xa2_0000 && address <= 0xa2_0001) {
            int offset = address - 0xa2_0000;
            igs011_priority_w(offset, value);
        } else if (address >= 0xa4_0000 && address <= 0xa4_0001) {
            int offset = address - 0xa4_0000;
            igs_dips_w(offset, value);
        } else if (address >= 0xa4_8000 && address <= 0xa4_8001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address <= 0xa5_8001) {
            int offset = address - 0xa5_8000;
            igs011_blit_x_w(offset, value);
        } else if (address >= 0xa5_8800 && address <= 0xa5_8801) {
            int offset = address - 0xa5_8800;
            igs011_blit_y_w(offset, value);
        } else if (address >= 0xa5_9000 && address <= 0xa5_9001) {
            int offset = address - 0xa5_9000;
            igs011_blit_w_w(offset, value);
        } else if (address >= 0xa5_9800 && address <= 0xa5_9801) {
            int offset = address - 0xa5_9800;
            igs011_blit_h_w(offset, value);
        } else if (address >= 0xa5_a000 && address <= 0xa5_a001) {
            int offset = address - 0xa5_a000;
            igs011_blit_gfx_lo_w(offset, value);
        } else if (address >= 0xa5_a800 && address <= 0xa5_a801) {
            int offset = address - 0xa5_a800;
            igs011_blit_gfx_hi_w(offset, value);
        } else if (address >= 0xa5_b000 && address <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address <= 0xa5_b801) {
            int offset = address - 0xa5_b800;
            igs011_blit_pen_w(offset, value);
        } else if (address >= 0xa5_c000 && address <= 0xa5_c001) {
            int offset = address - 0xa5_c000;
            igs011_blit_depth_w(offset, value);
        }
    }

    public static void MWriteWord_vbowl(int address, short value) {
        address &= 0xff_ffff;
        if (address >= prot1_addr && address + 1 <= prot1_addr + 7) {
            int offset = address - prot1_addr;
            igs011_prot1_w(offset, value);
        } else if (address >= 0 && address + 1 <= 0x1_ffff) {
            int address2 = address & ~0x1_c000;
            if (address2 >= 0x00_1600 && address2 + 1 <= 0x00_160f) {
                igs012_prot_swap_w(value);
            } else if (address2 >= 0x00_1620 && address2 + 1 <= 0x00_162f) {
                igs012_prot_dec_inc_w(value);
            } else if (address2 >= 0x00_1630 && address2 + 1 <= 0x00_163f) {
                igs012_prot_inc_w(value);
            } else if (address2 >= 0x00_1640 && address2 + 1 <= 0x00_164f) {
                igs012_prot_copy_w(value);
            } else if (address2 >= 0x00_1650 && address2 + 1 <= 0x00_165f) {
                igs012_prot_dec_copy_w(value);
            } else if (address2 >= 0x00_1670 && address2 + 1 <= 0x00_167f) {
                igs012_prot_mode_w(value);
            } else if (address >= 0x00_d400 && address + 1 <= 0x00_d43f) {
                igs011_prot2_dec_w();
            } else if (address >= 0x00_d440 && address + 1 <= 0x00_d47f) {
                drgnwrld_igs011_prot2_swap_w();
            } else if (address >= 0x00_d480 && address + 1 <= 0x00_d4bf) {
                igs011_prot2_reset_w();
            }
        } else if (address >= 0x50_f000 && address + 1 <= 0x50_f1ff) {
            igs011_prot2_dec_w();
        } else if (address >= 0x50_f200 && address + 1 <= 0x50_f3ff) {
            int offset = (address - 0x50_f200) / 2;
            vbowl_igs011_prot2_swap_w(offset);
        } else if (address >= 0x50_f400 && address + 1 <= 0x50_f5ff) {
            igs011_prot2_reset_w();
        } else if (address >= 0x90_2000 && address + 1 <= 0x90_2fff) {
            igs012_prot_reset_w();
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 1] = (byte) value;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = value;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, value);
        } else if (address >= 0x40_0000 && address + 1 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, value);
        } else if (address >= 0x60_0000 && address + 1 <= 0x60_0007) {
            int offset = (address - 0x60_0000) / 2;
            ics2115_0_word_w(offset, value);
        } else if (address >= 0x70_0000 && address + 1 <= 0x70_0003) {
            int offset = (address - 0x70_0000) / 2;
            vbowl_trackball[offset] = value;
        } else if (address >= 0x70_0004 && address + 1 <= 0x70_0005) {
            vbowl_pen_hi_w((byte) value);
        } else if (address >= 0x80_0000 && address + 1 <= 0x80_0003) {
            int offset = (address - 0x80_0000) / 2;
            vbowl_igs003_w(offset, value);
        } else if (address >= 0xa0_0000 && address + 1 <= 0xa0_0001) {
            vbowl_link_0_w();
        } else if (address >= 0xa0_8000 && address + 1 <= 0xa0_8001) {
            vbowl_link_1_w();
        } else if (address >= 0xa1_0000 && address + 1 <= 0xa1_0001) {
            vbowl_link_2_w();
        } else if (address >= 0xa1_8000 && address + 1 <= 0xa1_8001) {
            vbowl_link_3_w();
        } else if (address >= 0xa2_0000 && address + 1 <= 0xa2_0001) {
            igs011_priority_w(value);
        } else if (address >= 0xa4_0000 && address + 1 <= 0xa4_0001) {
            igs_dips_w(value);
        } else if (address >= 0xa4_8000 && address + 1 <= 0xa4_8001) {
            igs011_prot_addr_w(value);
        } else if (address >= 0xa5_8000 && address + 1 <= 0xa5_8001) {
            igs011_blit_x_w(value);
        } else if (address >= 0xa5_8800 && address + 1 <= 0xa5_8801) {
            igs011_blit_y_w(value);
        } else if (address >= 0xa5_9000 && address + 1 <= 0xa5_9001) {
            igs011_blit_w_w(value);
        } else if (address >= 0xa5_9800 && address + 1 <= 0xa5_9801) {
            igs011_blit_h_w(value);
        } else if (address >= 0xa5_a000 && address + 1 <= 0xa5_a001) {
            igs011_blit_gfx_lo_w(value);
        } else if (address >= 0xa5_a800 && address + 1 <= 0xa5_a801) {
            igs011_blit_gfx_hi_w(value);
        } else if (address >= 0xa5_b000 && address + 1 <= 0xa5_b001) {
            igs011_blit_flags_w(value);
        } else if (address >= 0xa5_b800 && address + 1 <= 0xa5_b801) {
            igs011_blit_pen_w(value);
        } else if (address >= 0xa5_c000 && address + 1 <= 0xa5_c001) {
            igs011_blit_depth_w(value);
        }
    }

    public static void MWriteLong_vbowl(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            Generic.generic_nvram[offset] = (byte) (value >> 24);
            Generic.generic_nvram[offset + 1] = (byte) (value >> 16);
            Generic.generic_nvram[offset + 2] = (byte) (value >> 8);
            Generic.generic_nvram[offset + 3] = (byte) value;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0fff) {
            int offset = (address - 0x20_0000) / 2;
            priority_ram[offset] = (short) (value >> 16);
            priority_ram[offset + 1] = (short) value;
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            igs011_layers_w(offset, (short) (value >> 16));
            igs011_layers_w(offset + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x40_1fff) {
            int offset = (address - 0x40_0000) / 2;
            igs011_palette(offset, (short) (value >> 16));
            igs011_palette(offset + 1, (short) value);
        }
    }

    public static byte MReadOpByte_nkishusp(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static byte MReadByte_nkishusp(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= prot1_addr + 8 && address <= prot1_addr + 9) {
            if (address % 2 == 0) {
                result = (byte) (igs011_prot1_r() >> 8);
            } else if (address % 2 == 1) {
                result = igs011_prot1_r();
            }
        } else if (address >= 0x02_3400 && address <= 0x02_35ff) {
            result = (byte) lhb2_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address <= 0x07_ffff) {
            if (address < Memory.mainrom.length) {
                result = Memory.mainrom[address];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = Generic.generic_nvram[offset];
        } else if (address >= 0x20_0000 && address <= 0x20_0001) {
            result = (byte) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x20_8002 && address <= 0x20_8003) {
            if (address % 2 == 1) {
                result = (byte) lhb2_igs003_r();
            }
        } else if (address >= 0x20_c000 && address <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            if (address % 2 == 0) {
                result = (byte) (priority_ram[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) priority_ram[offset];
            }
        } else if (address >= 0x21_0000 && address <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            if (address % 2 == 0) {
                result = (byte) (paletteram16[offset] >> 8);
            } else if (address % 2 == 1) {
                result = (byte) paletteram16[offset];
            }
        } else if (address >= 0x21_4000 && address <= 0x21_4001) {
            if (address % 2 == 1) {
                result = sbytec;
            }
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            if (address % 2 == 0) {
                result = igs011_layers_r1(offset / 2);
            } else if (address % 2 == 1) {
                result = igs011_layers_r2(offset / 2);
            }
        } else if (address >= 0xa8_8000 && address <= 0xa8_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static short MReadOpWord_nkishusp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static short MReadWord_nkishusp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= prot1_addr + 8 && address + 1 <= prot1_addr + 9) {
            result = igs011_prot1_r();
        } else if (address >= 0x02_3400 && address + 1 <= 0x02_35ff) {
            result = lhb2_igs011_prot2_r();
        } else if (address >= 0x00_0000 && address + 1 <= 0x07_ffff) {
            if (address + 1 < Memory.mainrom.length) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 1 <= 0x10_3fff) {
            int offset = address - 0x10_0000;
            result = (short) (Generic.generic_nvram[offset] * 0x100 + Generic.generic_nvram[offset + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0001) {
            result = (short) OKI6295.okim6295_status_0_lsb_r();
        } else if (address >= 0x20_8002 && address + 1 <= 0x20_8003) {
            result = lhb2_igs003_r();
        } else if (address >= 0x20_c000 && address + 1 <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            result = priority_ram[offset];
        } else if (address >= 0x21_0000 && address + 1 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            result = paletteram16[offset];
        } else if (address >= 0x21_4000 && address + 1 <= 0x21_4001) {
            result = sbytec;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset);
        } else if (address >= 0xa8_8000 && address + 1 <= 0xa8_8001) {
            result = igs_3_dips_r();
        }
        return result;
    }

    public static int MReadOpLong_nkishusp(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        }
        return result;
    }

    public static int MReadLong_nkishusp(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x07_ffff) {
            if (address + 3 < Memory.mainrom.length) {
                int offset = (address - 0x02_3000) / 2;
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            } else {
                result = 0;
            }
        } else if (address >= 0x10_0000 && address + 3 <= 0x10_3fff) {
            int offset = (address - 0x10_0000) / 2;
            result = Generic.generic_nvram[offset] * 0x100_0000 + Generic.generic_nvram[offset + 1] * 0x1_0000 + Generic.generic_nvram[offset + 2] * 0x100 + Generic.generic_nvram[offset + 3];
        } else if (address >= 0x20_c000 && address + 3 <= 0x20_cfff) {
            int offset = (address - 0x20_c000) / 2;
            result = priority_ram[offset] * 0x1_0000 + priority_ram[offset + 1];
        } else if (address >= 0x21_0000 && address + 3 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
            result = paletteram16[offset] * 0x1_0000 + paletteram16[offset + 1];
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
            result = igs011_layers_r(offset) * 0x1_0000 + igs011_layers_r(offset + 1);
        }
        return result;
    }

    public static void MWriteByte_nkishusp(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x02_3000 && address <= 0x02_31ff) {
            int offset = (address - 0x02_3000) / 2;
        } else if (address >= 0x02_3200 && address <= 0x02_33ff) {
            int offset = (address - 0x02_3200) / 2;
        } else if (address >= 0x02_3600 && address <= 0x02_37ff) {
            int offset = (address - 0x02_3600) / 2;
        } else if (address >= 0x20_0000 && address <= 0x20_0001) {
            int offset = (address - 0x20_0000) / 2;
        } else if (address >= 0x20_4000 && address <= 0x20_4001) {
            int offset = (address - 0x20_4000) / 2;
        } else if (address >= 0x20_4002 && address <= 0x20_4003) {
            int offset = (address - 0x20_4002) / 2;
        } else if (address >= 0x20_8000 && address <= 0x20_8003) {
            int offset = (address - 0x20_8000) / 2;
        } else if (address >= 0x21_0000 && address <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
        } else if (address >= 0x30_0000 && address <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
        } else if (address >= 0xa2_0000 && address <= 0xa2_0001) {
            int offset = (address - 0xa2_0000) / 2;
        } else if (address >= 0xa3_8000 && address <= 0xa3_8001) {
            int offset = (address - 0xa3_8000) / 2;
        } else if (address >= 0xa4_0000 && address <= 0xa4_0001) {
            int offset = (address - 0xa4_0000) / 2;
        } else if (address >= 0xa5_0000 && address <= 0xa5_0001) {
            int offset = (address - 0xa5_0000) / 2;
        } else if (address >= 0xa5_8000 && address <= 0xa5_8001) {
            int offset = (address - 0xa5_8000) / 2;
        } else if (address >= 0xa5_8800 && address <= 0xa5_8801) {
            int offset = (address - 0xa5_8800) / 2;
        } else if (address >= 0xa5_9000 && address <= 0xa5_9001) {
            int offset = (address - 0xa5_9000) / 2;
        } else if (address >= 0xa5_9800 && address <= 0xa5_9801) {
            int offset = (address - 0xa5_9800) / 2;
        } else if (address >= 0xa5_a000 && address <= 0xa5_a001) {
            int offset = (address - 0xa5_a000) / 2;
        } else if (address >= 0xa5_a800 && address <= 0xa5_a801) {
            int offset = (address - 0xa5_a800) / 2;
        } else if (address >= 0xa5_b000 && address <= 0xa5_b001) {
            int offset = (address - 0xa5_b000) / 2;
        } else if (address >= 0xa5_b800 && address <= 0xa5_b801) {
            int offset = (address - 0xa5_b800) / 2;
        } else if (address >= 0xa5_c000 && address <= 0xa5_c001) {
            int offset = (address - 0xa5_c000) / 2;
        }
    }

    public static void MWriteWord_nkishusp(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x02_3000 && address + 1 <= 0x02_31ff) {
            int offset = (address - 0x02_3000) / 2;
        } else if (address >= 0x02_3200 && address + 1 <= 0x02_33ff) {
            int offset = (address - 0x02_3200) / 2;
        } else if (address >= 0x02_3600 && address + 1 <= 0x02_37ff) {
            int offset = (address - 0x02_3600) / 2;
        } else if (address >= 0x20_0000 && address + 1 <= 0x20_0001) {
            int offset = (address - 0x20_0000) / 2;
        } else if (address >= 0x20_4000 && address + 1 <= 0x20_4001) {
            int offset = (address - 0x20_4000) / 2;
        } else if (address >= 0x20_4002 && address + 1 <= 0x20_4003) {
            int offset = (address - 0x20_4002) / 2;
        } else if (address >= 0x20_8000 && address + 1 <= 0x20_8003) {
            int offset = (address - 0x20_8000) / 2;
        } else if (address >= 0x21_0000 && address + 1 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
        } else if (address >= 0x30_0000 && address + 1 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
        } else if (address >= 0xa2_0000 && address + 1 <= 0xa2_0001) {
            int offset = (address - 0xa2_0000) / 2;
        } else if (address >= 0xa3_8000 && address + 1 <= 0xa3_8001) {
            int offset = (address - 0xa3_8000) / 2;
        } else if (address >= 0xa4_0000 && address + 1 <= 0xa4_0001) {
            int offset = (address - 0xa4_0000) / 2;
        } else if (address >= 0xa5_0000 && address + 1 <= 0xa5_0001) {
            int offset = (address - 0xa5_0000) / 2;
        } else if (address >= 0xa5_8000 && address + 1 <= 0xa5_8001) {
            int offset = (address - 0xa5_8000) / 2;
        } else if (address >= 0xa5_8800 && address + 1 <= 0xa5_8801) {
            int offset = (address - 0xa5_8800) / 2;
        } else if (address >= 0xa5_9000 && address + 1 <= 0xa5_9001) {
            int offset = (address - 0xa5_9000) / 2;
        } else if (address >= 0xa5_9800 && address + 1 <= 0xa5_9801) {
            int offset = (address - 0xa5_9800) / 2;
        } else if (address >= 0xa5_a000 && address + 1 <= 0xa5_a001) {
            int offset = (address - 0xa5_a000) / 2;
        } else if (address >= 0xa5_a800 && address + 1 <= 0xa5_a801) {
            int offset = (address - 0xa5_a800) / 2;
        } else if (address >= 0xa5_b000 && address + 1 <= 0xa5_b001) {
            int offset = (address - 0xa5_b000) / 2;
        } else if (address >= 0xa5_b800 && address + 1 <= 0xa5_b801) {
            int offset = (address - 0xa5_b800) / 2;
        } else if (address >= 0xa5_c000 && address + 1 <= 0xa5_c001) {
            int offset = (address - 0xa5_c000) / 2;
        }
    }

    public static void MWriteLong_nkishusp(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x02_3000 && address + 3 <= 0x02_31ff) {
            int offset = (address - 0x02_3000) / 2;
        } else if (address >= 0x02_3200 && address + 3 <= 0x02_33ff) {
            int offset = (address - 0x02_3200) / 2;
        } else if (address >= 0x02_3600 && address + 3 <= 0x02_37ff) {
            int offset = (address - 0x02_3600) / 2;
        } else if (address >= 0x20_0000 && address + 3 <= 0x20_0001) {
            int offset = (address - 0x20_0000) / 2;
        } else if (address >= 0x20_4000 && address + 3 <= 0x20_4001) {
            int offset = (address - 0x20_4000) / 2;
        } else if (address >= 0x20_4002 && address + 3 <= 0x20_4003) {
            int offset = (address - 0x20_4002) / 2;
        } else if (address >= 0x20_8000 && address + 3 <= 0x20_8003) {
            int offset = (address - 0x20_8000) / 2;
        } else if (address >= 0x21_0000 && address + 3 <= 0x21_1fff) {
            int offset = (address - 0x21_0000) / 2;
        } else if (address >= 0x30_0000 && address + 3 <= 0x3f_ffff) {
            int offset = (address - 0x30_0000) / 2;
        } else if (address >= 0xa2_0000 && address + 3 <= 0xa2_0001) {
            int offset = (address - 0xa2_0000) / 2;
        } else if (address >= 0xa3_8000 && address + 3 <= 0xa3_8001) {
            int offset = (address - 0xa3_8000) / 2;
        } else if (address >= 0xa4_0000 && address + 3 <= 0xa4_0001) {
            int offset = (address - 0xa4_0000) / 2;
        } else if (address >= 0xa5_0000 && address + 3 <= 0xa5_0001) {
            int offset = (address - 0xa5_0000) / 2;
        } else if (address >= 0xa5_8000 && address + 3 <= 0xa5_8001) {
            int offset = (address - 0xa5_8000) / 2;
        } else if (address >= 0xa5_8800 && address + 3 <= 0xa5_8801) {
            int offset = (address - 0xa5_8800) / 2;
        } else if (address >= 0xa5_9000 && address + 3 <= 0xa5_9001) {
            int offset = (address - 0xa5_9000) / 2;
        } else if (address >= 0xa5_9800 && address + 3 <= 0xa5_9801) {
            int offset = (address - 0xa5_9800) / 2;
        } else if (address >= 0xa5_a000 && address + 3 <= 0xa5_a001) {
            int offset = (address - 0xa5_a000) / 2;
        } else if (address >= 0xa5_a800 && address + 3 <= 0xa5_a801) {
            int offset = (address - 0xa5_a800) / 2;
        } else if (address >= 0xa5_b000 && address + 3 <= 0xa5_b001) {
            int offset = (address - 0xa5_b000) / 2;
        } else if (address >= 0xa5_b800 && address + 3 <= 0xa5_b801) {
            int offset = (address - 0xa5_b800) / 2;
        } else if (address >= 0xa5_c000 && address + 3 <= 0xa5_c001) {
            int offset = (address - 0xa5_c000) / 2;
        }
    }

//#endregion

//#region Input


    public static void loop_inputports_igs011_drgnwrld() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbytec &= ~0x01;
        } else {
            sbytec |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            sbytec &= ~0x02;
        } else {
            sbytec |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            sbyte0 &= ~0x01;
        } else {
            sbyte0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            sbyte2 &= ~0x10;
        } else {
            sbyte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D))// || Mouse.deltaX > 0)
        {
            sbyte0 &= ~0x10;
        } else {
            sbyte0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A))// || Mouse.deltaX < 0)
        {
            sbyte2 &= ~0x02;
        } else {
            sbyte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S))// || Mouse.deltaY > 0)
        {
            sbyte0 &= ~0x04;
        } else {
            sbyte0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W))// || Mouse.deltaY < 0)
        {
            sbyte2 &= ~0x01;
        } else {
            sbyte2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J))// || Mouse.buttons[0] != 0)
        {
            sbyte2 &= ~0x04;
        } else {
            sbyte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K))// || Mouse.buttons[1] != 0)
        {
            sbyte0 &= ~0x40;
        } else {
            sbyte0 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            sbyte2 &= ~0x08;
        } else {
            sbyte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            sbyte2 &= ~0x40;
        } else {
            sbyte2 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            sbyte1 &= ~0x08;
        } else {
            sbyte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            sbyte2 &= ~0x20;
        } else {
            sbyte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            sbyte1 &= ~0x02;
        } else {
            sbyte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            sbyte1 &= ~0x20;
        } else {
            sbyte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            sbyte2 &= (byte) ~0x80;
        } else {
            sbyte2 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            sbyte1 &= (byte) ~0x80;
        } else {
            sbyte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            sbytec &= ~0x08;
        } else {
            sbytec |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            sbytec &= ~0x10;
        } else {
            sbytec |= 0x10;
        }
    }

    public static void loop_inputports_igs011_lhb() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbytec &= ~0x10;
        } else {
            sbytec |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            bkey0 &= (byte) ~0x20;
        } else {
            bkey0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            bkey1 &= (byte) ~0x20;
        } else {
            bkey1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_3)) {
            bkey4 &= (byte) ~0x10;
        } else {
            bkey4 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_4)) {
            bkey4 &= (byte) ~0x20;
        } else {
            bkey4 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            bkey0 &= (byte) ~0x01;
        } else {
            bkey0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_B)) {
            bkey1 &= (byte) ~0x01;
        } else {
            bkey1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_C)) {
            bkey2 &= (byte) ~0x01;
        } else {
            bkey2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            bkey3 &= (byte) ~0x01;
        } else {
            bkey3 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_E)) {
            bkey0 &= (byte) ~0x02;
        } else {
            bkey0 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_F)) {
            bkey1 &= (byte) ~0x02;
        } else {
            bkey1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_G)) {
            bkey2 &= (byte) ~0x02;
        } else {
            bkey2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_H)) {
            bkey3 &= (byte) ~0x02;
        } else {
            bkey3 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            bkey0 &= (byte) ~0x04;
        } else {
            bkey0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            bkey1 &= (byte) ~0x04;
        } else {
            bkey1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            bkey2 &= (byte) ~0x04;
        } else {
            bkey2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            bkey3 &= (byte) ~0x04;
        } else {
            bkey3 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_M)) {
            bkey0 &= (byte) ~0x08;
        } else {
            bkey0 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_N)) {
            bkey1 &= (byte) ~0x08;
        } else {
            bkey1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {
            bkey4 &= (byte) ~0x04;
        } else {
            bkey4 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_Q)) {
            bkey0 &= (byte) ~0x10;
        } else {
            bkey0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            bkey1 &= (byte) ~0x10;
        } else {
            bkey1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            bkey2 &= (byte) ~0x08;
        } else {
            bkey2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            bkey4 &= (byte) ~0x02;
        } else {
            bkey4 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            bkey3 &= (byte) ~0x08;
        } else {
            bkey3 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_Y)) {
            bkey4 &= (byte) ~0x01;
        } else {
            bkey4 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_Z)) {
            bkey2 &= (byte) ~0x10;
        } else {
            bkey2 |= 0x10;
        }
    }

    public static void loop_inputports_igs011_lhb2() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            sbytec &= ~0x10;
        } else {
            sbytec |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            bkey0 &= (byte) ~0x20;
        } else {
            bkey0 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            bkey1 &= (byte) ~0x20;
        } else {
            bkey1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            bkey0 &= (byte) ~0x01;
        } else {
            bkey0 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_B)) {
            bkey1 &= (byte) ~0x01;
        } else {
            bkey1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_C)) {
            bkey2 &= (byte) ~0x01;
        } else {
            bkey2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            bkey3 &= (byte) ~0x01;
        } else {
            bkey3 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_E)) {
            bkey0 &= (byte) ~0x02;
        } else {
            bkey0 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_F)) {
            bkey1 &= (byte) ~0x02;
        } else {
            bkey1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_G)) {
            bkey2 &= (byte) ~0x02;
        } else {
            bkey2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_H)) {
            bkey3 &= (byte) ~0x02;
        } else {
            bkey3 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            bkey0 &= (byte) ~0x04;
        } else {
            bkey0 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            bkey1 &= (byte) ~0x04;
        } else {
            bkey1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            bkey2 &= (byte) ~0x04;
        } else {
            bkey2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            bkey3 &= (byte) ~0x04;
        } else {
            bkey3 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_M)) {
            bkey0 &= (byte) ~0x08;
        } else {
            bkey0 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_N)) {
            bkey1 &= (byte) ~0x08;
        } else {
            bkey1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_Q)) {
            bkey0 &= (byte) ~0x10;
        } else {
            bkey0 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            bkey1 &= (byte) ~0x10;
        } else {
            bkey1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            bkey2 &= (byte) ~0x08;
        } else {
            bkey2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            bkey3 &= (byte) ~0x08;
        } else {
            bkey3 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_Z)) {
            bkey2 &= (byte) ~0x10;
        } else {
            bkey2 |= 0x10;
        }
    }

    public static void record_port_drgnwrld() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbytec != sbytec_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbytec_old = sbytec;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbytec);
        }
    }

    public static void replay_port_drgnwrld() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
                sbytec_old = Mame.brRecord.readSByte();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbytec = sbytec_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

    public static void record_port_lhb() {
        if (sbyte0 != sbyte0_old || sbyte1 != sbyte1_old || sbyte2 != sbyte2_old || sbytec != sbytec_old) {
            sbyte0_old = sbyte0;
            sbyte1_old = sbyte1;
            sbyte2_old = sbyte2;
            sbytec_old = sbytec;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(sbyte0);
            Mame.bwRecord.write(sbyte1);
            Mame.bwRecord.write(sbyte2);
            Mame.bwRecord.write(sbytec);
        }
    }

    public static void replay_port_lhb() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                sbyte0_old = Mame.brRecord.readSByte();
                sbyte1_old = Mame.brRecord.readSByte();
                sbyte2_old = Mame.brRecord.readSByte();
                sbytec_old = Mame.brRecord.readSByte();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            sbyte0 = sbyte0_old;
            sbyte1 = sbyte1_old;
            sbyte2 = sbyte2_old;
            sbytec = sbytec_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region State

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        for (i = 0; i < 0x800; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Generic.generic_nvram, 0, 0x4000);
        for (i = 0; i < 0x800; i++) {
            writer.write(priority_ram[i]);
        }
        for (i = 0; i < 0x1000; i++) {
            writer.write(paletteram16[i]);
        }
        writer.write(prot1);
        writer.write(prot2);
        writer.write(prot1_swap);
        writer.write(prot1_addr);
        for (i = 0; i < 2; i++) {
            writer.write(igs003_reg[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(vbowl_trackball[i]);
        }
        writer.write(priority);
        writer.write(igs_dips_sel);
        writer.write(igs_input_sel);
        writer.write(lhb_irq_enable);
        writer.write(igs012_prot);
        writer.write(igs012_prot_swap);
        writer.write(igs012_prot_mode);
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 0x2_0000; j++) {
                writer.write(layer[i][j]);
            }
        }
        writer.write(lhb2_pen_hi);
        writer.write(blitter.x);
        writer.write(blitter.y);
        writer.write(blitter.w);
        writer.write(blitter.h);
        writer.write(blitter.gfx_lo);
        writer.write(blitter.gfx_hi);
        writer.write(blitter.depth);
        writer.write(blitter.pen);
        writer.write(blitter.flags);
        MC68000.m1.SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        OKI6295.SaveStateBinary(writer);
        YM3812.SaveStateBinary(writer);
        writer.write(Sound.okistream.output_sampindex);
        writer.write(Sound.okistream.output_base_sampindex);
        writer.write(Sound.ym3812stream.output_sampindex);
        writer.write(Sound.ym3812stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i, j;
            for (i = 0; i < 0x800; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Generic.generic_nvram = reader.readBytes(0x4000);
            for (i = 0; i < 0x800; i++) {
                priority_ram[i] = reader.readUInt16();
            }
            for (i = 0; i < 0x1000; i++) {
                paletteram16[i] = reader.readUInt16();
            }
            prot1 = reader.readByte();
            prot2 = reader.readByte();
            prot1_swap = reader.readByte();
            prot1_addr = reader.readUInt32();
            for (i = 0; i < 2; i++) {
                igs003_reg[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                vbowl_trackball[i] = reader.readUInt16();
            }
            priority = reader.readUInt16();
            igs_dips_sel = reader.readUInt16();
            igs_input_sel = reader.readUInt16();
            lhb_irq_enable = reader.readUInt16();
            igs012_prot = reader.readByte();
            igs012_prot_swap = reader.readByte();
            igs012_prot_mode = reader.readBoolean();
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 0x2_0000; j++) {
                    layer[i][j] = reader.readByte();
                }
            }
            lhb2_pen_hi = reader.readByte();
            blitter.x = reader.readUInt16();
            blitter.y = reader.readUInt16();
            blitter.w = reader.readUInt16();
            blitter.h = reader.readUInt16();
            blitter.gfx_lo = reader.readUInt16();
            blitter.gfx_hi = reader.readUInt16();
            blitter.depth = reader.readUInt16();
            blitter.pen = reader.readUInt16();
            blitter.flags = reader.readUInt16();
            MC68000.m1.LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            OKI6295.LoadStateBinary(reader);
            YM3812.LoadStateBinary(reader);
            Sound.okistream.output_sampindex = reader.readInt32();
            Sound.okistream.output_base_sampindex = reader.readInt32();
            Sound.ym3812stream.output_sampindex = reader.readInt32();
            Sound.ym3812stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Machine

    public static void lhb2_interrupt() {
        if (Cpuexec.iloops == 0) {
            Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
        } else {
            Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
        }
    }

    public static void wlcc_interrupt() {
        if (Cpuexec.iloops == 0) {
            Cpuint.cpunum_set_input_line(0, 3, LineState.HOLD_LINE);
        } else {
            Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
        }
    }

    public static void lhb_interrupt() {
        if (lhb_irq_enable == 0) {
            return;
        }
        if (Cpuexec.iloops == 0) {
            Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
        } else {
            Cpuint.cpunum_set_input_line(0, 5, LineState.HOLD_LINE);
        }
    }

    public static void vbowl_interrupt() {
        if (Cpuexec.iloops == 0) {
            Cpuint.cpunum_set_input_line(0, 6, LineState.HOLD_LINE);
        } else {
            Cpuint.cpunum_set_input_line(0, 3, LineState.HOLD_LINE);
        }
    }

    public static int BIT(int x, int n) {
        return (x >> n) & 1;
    }

//#endregion
}
