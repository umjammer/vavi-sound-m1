/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;
import java.util.function.Consumer;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.mame.konami68000.Konami68000;


public class K007232 {

    public static class kdacApcm {

        public byte[][] vol;
        public int[] addr;
        public int[] start;
        public int[] step;
        public int[] bank;
        public int[] play;

        public byte[] wreg;
        public int[] pcmbuf_offset;

        public int clock;
        public int pcmlimit;
        // portwritehandler
        public Consumer<Integer> portwritehandler;
        public int[] fncode;
    }

    public static kdacApcm info;
    public static byte[] k007232rom;

    public static void KDAC_A_make_fncode() {
        int i;
        for (i = 0; i < 0x200; i++) {
            info.fncode[i] = (32 << 12) / (0x200 - i);
        }
    }

    public static void KDAC_A_update(int offset, int length) {
        int i, j;
        for (i = 0; i < 2; i++) {
            for (j = 0; j < length; j++) {
                Sound.k007232stream.streamoutput[i][offset + j] = 0;
            }
        }
        for (i = 0; i < 2; i++) {
            if (info.play[i] != 0) {
                int volA, volB, out1;
                int addr, old_addr;
                addr = info.start[i] + ((info.addr[i] >> 12) & 0x000f_ffff);
                volA = info.vol[i][0] * 2;
                volB = info.vol[i][1] * 2;
                for (j = 0; j < length; j++) {
                    old_addr = addr;
                    addr = info.start[i] + ((info.addr[i] >> 12) & 0x000f_ffff);
                    while (old_addr <= addr) {
                        if ((k007232rom[info.pcmbuf_offset[i] + old_addr] & 0x80) != 0 || old_addr >= info.pcmlimit) {
                            if ((info.wreg[0x0d] & (1 << i)) != 0) {
                                info.start[i] =
                                        ((((int) info.wreg[i * 0x06 + 0x04] << 16) & 0x0001_0000) |
                                                (((int) info.wreg[i * 0x06 + 0x03] << 8) & 0x0000_ff00) |
                                                (((int) info.wreg[i * 0x06 + 0x02]) & 0x0000_00ff) |
                                                info.bank[i]);
                                addr = info.start[i];
                                info.addr[i] = 0;
                                old_addr = addr;
                            } else {
                                info.play[i] = 0;
                            }
                            break;
                        }
                        old_addr++;
                    }
                    if (info.play[i] == 0) {
                        break;
                    }
                    info.addr[i] += info.step[i];
                    out1 = (k007232rom[info.pcmbuf_offset[i] + addr] & 0x7f) - 0x40;
                    Sound.k007232stream.streamoutput[0][offset + j] += out1 * volA;
                    Sound.k007232stream.streamoutput[1][offset + j] += out1 * volB;
                }
            }
        }
    }

    public static void k007232_start(int clock) {
        int i;
        info = new kdacApcm();
        info.portwritehandler = Konami68000::volume_callback;
        info.pcmbuf_offset = new int[2];
        info.addr = new int[2];
        info.start = new int[2];
        info.step = new int[2];
        info.play = new int[2];
        info.bank = new int[2];
        info.vol = new byte[2][];
        info.wreg = new byte[0x10];
        info.fncode = new int[0x200];
        info.pcmbuf_offset[0] = 0;
        info.pcmbuf_offset[1] = 0;
        info.pcmlimit = k007232rom.length;
        info.clock = clock;
        for (i = 0; i < 2; i++) {
            info.start[i] = 0;
            info.step[i] = 0;
            info.play[i] = 0;
            info.bank[i] = 0;
            info.vol[i] = new byte[2];
        }
        info.vol[0][0] = (byte) 255;
        info.vol[0][1] = 0;
        info.vol[1][0] = 0;
        info.vol[1][1] = (byte) 255;
        for (i = 0; i < 0x10; i++) {
            info.wreg[i] = 0;
        }
        KDAC_A_make_fncode();
    }

    public static void k007232_WriteReg(int r, int v, int chip) {
        int data;
        Sound.k007232stream.stream_update();
        info.wreg[r] = (byte) v;
        if (r == 0x0c) {
            if (info.portwritehandler != null) {
                info.portwritehandler.accept(v);
            }
        } else if (r == 0x0d) {
        } else {
            int reg_port;
            reg_port = 0;
            if (r >= 0x06) {
                reg_port = 1;
                r -= 0x06;
            }
            switch (r) {
                case 0x00:
                case 0x01:
                    data = ((((int) info.wreg[reg_port * 0x06 + 0x01]) << 8) & 0x0100) | (((int) info.wreg[reg_port * 0x06 + 0x00]) & 0x00ff);
                    info.step[reg_port] = info.fncode[data];
                    break;
                case 0x02:
                case 0x03:
                case 0x04:
                    break;
                case 0x05:
                    info.start[reg_port] =
                            ((((int) info.wreg[reg_port * 0x06 + 0x04] << 16) & 0x0001_0000) |
                                    (((int) info.wreg[reg_port * 0x06 + 0x03] << 8) & 0x0000_ff00) |
                                    (((int) info.wreg[reg_port * 0x06 + 0x02]) & 0x0000_00ff) |
                                    info.bank[reg_port]);
                    if (info.start[reg_port] < info.pcmlimit) {
                        info.play[reg_port] = 1;
                        info.addr[reg_port] = 0;
                    }
                    break;
            }
        }
    }

    public static int k007232_ReadReg(int r, int chip) {
        int ch = 0;
        if (r == 0x0005 || r == 0x000b) {
            ch = r / 0x0006;
            r = ch * 0x0006;
            info.start[ch] =
                    ((((int) info.wreg[r + 0x04] << 16) & 0x0001_0000) |
                            (((int) info.wreg[r + 0x03] << 8) & 0x0000_ff00) |
                            (((int) info.wreg[r + 0x02]) & 0x0000_00ff) |
                            info.bank[ch]);
            if (info.start[ch] < info.pcmlimit) {
                info.play[ch] = 1;
                info.addr[ch] = 0;
            }
        }
        return 0;
    }

    public static void k007232_write_port_0_w(int offset, byte data) {
        k007232_WriteReg(offset, data, 0);
    }

    public static byte k007232_read_port_0_r(int offset) {
        return (byte) k007232_ReadReg(offset, 0);
    }

    public static void k007232_write_port_1_w(int offset, byte data) {
        k007232_WriteReg(offset, data, 1);
    }

    public static byte k007232_read_port_1_r(int offset) {
        return (byte) k007232_ReadReg(offset, 1);
    }

    public static void k007232_write_port_2_w(int offset, byte data) {
        k007232_WriteReg(offset, data, 2);
    }

    public static byte k007232_read_port_2_r(int offset) {
        return (byte) k007232_ReadReg(offset, 2);
    }

    public static void k007232_set_volume(int chip, int channel, int volumeA, int volumeB) {
        info.vol[channel][0] = (byte) volumeA;
        info.vol[channel][1] = (byte) volumeB;
    }

    public static void k007232_set_bank(int chip, int chABank, int chBBank) {
        info.bank[0] = chABank << 17;
        info.bank[1] = chBBank << 17;
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 2; j++) {
                writer.write(info.vol[i][j]);
            }
        }
        for (i = 0; i < 2; i++) {
            writer.write(info.addr[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(info.start[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(info.step[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(info.bank[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(info.play[i]);
        }
        for (i = 0; i < 0x10; i++) {
            writer.write(info.wreg[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(info.pcmbuf_offset[i]);
        }
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i, j;
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 2; j++) {
                info.vol[i][j] = reader.readByte();
            }
        }
        for (i = 0; i < 2; i++) {
            info.addr[i] = reader.readUInt32();
        }
        for (i = 0; i < 2; i++) {
            info.start[i] = reader.readUInt32();
        }
        for (i = 0; i < 2; i++) {
            info.step[i] = reader.readUInt32();
        }
        for (i = 0; i < 2; i++) {
            info.bank[i] = reader.readUInt32();
        }
        for (i = 0; i < 2; i++) {
            info.play[i] = reader.readInt32();
        }
        for (i = 0; i < 0x10; i++) {
            info.wreg[i] = reader.readByte();
        }
        for (i = 0; i < 2; i++) {
            info.pcmbuf_offset[i] = reader.readInt32();
        }
    }
}
