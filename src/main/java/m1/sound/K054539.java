/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;
import java.util.function.BiConsumer;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime.Atime;
import m1.emu.Machine;
import m1.emu.Timer;
import m1.mame.konami68000.Konami68000;


public class K054539 {

    public static class k054539_channel {

        public int pos;
        public int pfrac;
        public int val;
        public int pval;
    }

    public static class k054539_info {

        public double[] voltab;
        public double[] pantab;

        public double[] k054539_gain;
        public byte[][] k054539_posreg_latch;
        public int k054539_flags;

        public byte[] regs;
        public short[] ram;
        public int reverb_pos;

        public int cur_ptr;
        public int cur_limit;
        public byte[] cur_zone;
        public byte[] rom;
        public int rom_size;
        public int rom_mask;

        public k054539_channel[] channels;
    }

    public static byte[] k054539rom;
    public static k054539_info info;
    public static int zoneflag, zonedata;
    // apanhandler
    public static BiConsumer<Double, Double> apan;
    // irqhandler
    public static Runnable irq;

    public static boolean k054539_regupdate() {
        return (info.regs[0x22f] & 0x80) == 0;
    }

    public static void k054539_keyon(int channel) {
        if (k054539_regupdate()) {
            info.regs[0x22c] |= (byte) (1 << channel);
        }
    }

    public static void k054539_keyoff(int channel) {
        if (k054539_regupdate()) {
            info.regs[0x22c] &= (byte) (~(1 << channel));
        }
    }

    public static void k054539_update(int offset, int length) {
        short[] dpcm = {
                0 << 8, 1 << 8, 4 << 8, 9 << 8, 16 << 8, 25 << 8, 36 << 8, 49 << 8,
                -64 << 8, -49 << 8, -36 << 8, -25 << 8, -16 << 8, -9 << 8, -4 << 8, -1 << 8
        };
        int j, ch, reverb_pos;
        byte[] samples;
        int rom_mask;
        int offset1;
        int base1_offset, base2_offset;
        k054539_channel chan;
        int cur_pos, cur_pfrac, cur_val, cur_pval;
        int delta, rdelta, fdelta, pdelta;
        int vol, bval, pan, i;
        double gain, lvol, rvol, rbvol;
        reverb_pos = info.reverb_pos;
        for (i = 0; i < 2; i++) {
            for (j = 0; j < length; j++) {
                Sound.k054539stream.streamoutput[i][offset + j] = 0;
            }
        }
        samples = k054539rom; // info.rom;
        rom_mask = info.rom_mask;
        if ((info.regs[0x22f] & 1) == 0) {
            return;
        }
        info.reverb_pos = (reverb_pos + length) & 0x3fff;
        for (ch = 0; ch < 8; ch++) {
            if ((info.regs[0x22c] & (1 << ch)) != 0) {
                base1_offset = 0x20 * ch;
                base2_offset = 0x200 + 0x2 * ch;
                chan = info.channels[ch];
                delta = info.regs[base1_offset + 0x00] | (info.regs[base1_offset + 0x01] << 8) | (info.regs[base1_offset + 0x02] << 16);
                vol = info.regs[base1_offset + 0x03] & 0xff;
                bval = vol + info.regs[base1_offset + 0x04] & 0xff;
                if (bval > 255) {
                    bval = 255;
                }
                pan = info.regs[base1_offset + 0x05] & 0xff;
                if (pan >= 0x81 && pan <= 0x8f) {
                    pan -= 0x81;
                } else if (pan >= 0x11 && pan <= 0x1f) {
                    pan -= 0x11;
                } else {
                    pan = 0x18 - 0x11;
                }
                gain = info.k054539_gain[ch];
                lvol = info.voltab[vol] * info.pantab[pan] * gain;
                if (lvol > 1.80) {
                    lvol = 1.80;
                }
                rvol = info.voltab[vol] * info.pantab[0xe - pan] * gain;
                if (rvol > 1.80) {
                    rvol = 1.80;
                }
                rbvol = info.voltab[bval] * gain / 2;
                if (rbvol > 1.80) {
                    rbvol = 1.80;
                }
                rdelta = (info.regs[base1_offset + 6] | (info.regs[base1_offset + 7] << 8)) >> 3;
                rdelta = (rdelta + reverb_pos) & 0x3fff;
                cur_pos = (info.regs[base1_offset + 0x0c] | (info.regs[base1_offset + 0x0d] << 8) | (info.regs[base1_offset + 0x0e] << 16)) & rom_mask;
                offset1 = offset;
                if ((info.regs[base2_offset + 0] & 0x20) != 0) {
                    delta = -delta;
                    fdelta = +0x1_0000;
                    pdelta = -1;
                } else {
                    fdelta = -0x1_0000;
                    pdelta = +1;
                }
                if (cur_pos != chan.pos) {
                    chan.pos = cur_pos;
                    cur_pfrac = 0;
                    cur_val = 0;
                    cur_pval = 0;
                } else {
                    cur_pfrac = chan.pfrac;
                    cur_val = chan.val;
                    cur_pval = chan.pval;
                }
                switch (info.regs[base2_offset + 0] & 0xc) {
                    case 0x0: {
end_channel_0:
                        for (i = 0; i < length; i++) {
                            cur_pfrac += delta;
                            while ((cur_pfrac & ~0xffff) != 0) {
                                cur_pfrac += fdelta;
                                cur_pos += pdelta;
                                cur_pval = cur_val;
                                cur_val = (short) (samples[cur_pos] << 8);
                                if (cur_val == (short) 0x8000) {
                                    if ((info.regs[base2_offset + 1] & 1) != 0) {
                                        cur_pos = (info.regs[base1_offset + 0x08] | (info.regs[base1_offset + 0x09] << 8) | (info.regs[base1_offset + 0x0a] << 16)) & rom_mask;
                                        cur_val = (short) (samples[cur_pos] << 8);
                                        if (cur_val != (int) ((short) 0x8000)) {
                                            continue;
                                        }
                                    }
                                    k054539_keyoff(ch);
                                    break end_channel_0;
                                }
                            }
                            Sound.k054539stream.streamoutput[0][offset1] += (short) (cur_val * lvol);
                            Sound.k054539stream.streamoutput[1][offset1] += (short) (cur_val * rvol);
                            offset1++;
                            info.ram[rdelta] += (short) (cur_val * rbvol);
                            rdelta++;
                            rdelta &= 0x3fff;
                        }
                        break;
                    }
                    case 0x4: {
                        pdelta <<= 1;

end_channel_4:
                        for (i = 0; i < length; i++) {
                            cur_pfrac += delta;
                            while ((cur_pfrac & ~0xffff) != 0) {
                                cur_pfrac += fdelta;
                                cur_pos += pdelta;

                                cur_pval = cur_val;
                                cur_val = (short) (samples[cur_pos] | samples[cur_pos + 1] << 8);
                                if (cur_val == (short) 0x8000) {
                                    if ((info.regs[base2_offset + 1] & 1) != 0) {
                                        cur_pos = (info.regs[base1_offset + 0x08] | (info.regs[base1_offset + 0x09] << 8) | (info.regs[base1_offset + 0x0a] << 16)) & rom_mask;
                                        cur_val = (short) (samples[cur_pos] | samples[cur_pos + 1] << 8);
                                        if (cur_val != (short) 0x8000)
                                            continue;
                                    }
                                    k054539_keyoff(ch);
                                    break end_channel_4;
                                }
                            }
                            Sound.k054539stream.streamoutput[0][offset1] += (short) (cur_val * lvol);
                            Sound.k054539stream.streamoutput[1][offset1] += (short) (cur_val * rvol);
                            offset1++;
                            info.ram[rdelta] += (short) (cur_val * rbvol);
                            rdelta++;
                            rdelta &= 0x3fff;
                        }
                        break;
                    }
                    case 0x8: {
                        cur_pos <<= 1;
                        cur_pfrac <<= 1;
                        if ((cur_pfrac & 0x1_0000) != 0) {
                            cur_pfrac &= 0xffff;
                            cur_pos |= 1;
                        }
end_channel_8:
                        for (i = 0; i < length; i++) {
                            cur_pfrac += delta;
                            while ((cur_pfrac & ~0xffff) != 0) {
                                cur_pfrac += fdelta;
                                cur_pos += pdelta;
                                cur_pval = cur_val;
                                cur_val = samples[cur_pos >>> 1] & 0xff;
next_iter:
                                if (cur_val == 0x88) {
                                    if ((info.regs[base2_offset + 1] & 1) != 0) {
                                        cur_pos = (((info.regs[base1_offset + 0x08] & 0xff) | ((info.regs[base1_offset + 0x09] & 0xff) << 8) | ((info.regs[base1_offset + 0x0a] & 0xff) << 16)) & rom_mask) << 1;
                                        cur_val = samples[cur_pos >> 1] & 0xff;
                                        if (cur_val != 0x88)
                                            break next_iter;
                                    }
                                    k054539_keyoff(ch);
                                    break end_channel_8;
                                }
                                if ((cur_pos & 1) != 0) {
                                    cur_val >>= 4;
                                } else {
                                    cur_val &= 15;
                                }
                                cur_val = cur_pval + dpcm[cur_val];
                                if (cur_val < -32768) {
                                    cur_val = -32768;
                                } else if (cur_val > 32767) {
                                    cur_val = 32767;
                                }
                            }
                            Sound.k054539stream.streamoutput[0][offset1] += (short) (cur_val * lvol);
                            Sound.k054539stream.streamoutput[1][offset1] += (short) (cur_val * rvol);
                            offset1++;
                            info.ram[rdelta] += (short) (cur_val * rbvol);
                            rdelta++;
                            rdelta &= 0x3fff;
                        }
                        cur_pfrac >>= 1;
                        if ((cur_pos & 1) != 0) {
                            cur_pfrac |= 0x8000;
                        }
                        cur_pos >>= 1;
                        break;
                    }
                    default:
                        break;
                }
                chan.pos = cur_pos;
                chan.pfrac = cur_pfrac;
                chan.pval = cur_pval;
                chan.val = cur_val;
                if (k054539_regupdate()) {
                    info.regs[base1_offset + 0x0c] = (byte) (cur_pos & 0xff);
                    info.regs[base1_offset + 0x0d] = (byte) (cur_pos >> 8 & 0xff);
                    info.regs[base1_offset + 0x0e] = (byte) (cur_pos >> 16 & 0xff);
                }
            }
        }
        if ((info.k054539_flags & 2) == 0) {
            for (i = 0; i < length; i++) {
                short val = info.ram[(i + reverb_pos) & 0x3fff];
                Sound.k054539stream.streamoutput[0][offset + i] += val;
                Sound.k054539stream.streamoutput[1][offset + i] += val;
            }
        }
        if (reverb_pos + length > 0x4000) {
            i = 0x4000 - reverb_pos;
            for (j = 0; j < i; j++) {
                info.ram[reverb_pos + j] = 0;
            }
            for (j = 0; j < length - i; j++) {
                info.ram[j] = 0;
            }
        } else {
            for (j = 0; j < length; j++) {
                info.ram[reverb_pos + j] = 0;
            }
        }
    }

    public static void k054539_irq() {
        if ((info.regs[0x22f] & 0x20) != 0) {
            irq.run();
        }
    }

    public static void k054539_init_chip(int clock) {
        int i;
        info.k054539_flags |= 4;
        info.ram = new short[0x4000 + clock / 50];
        info.reverb_pos = 0;
        info.cur_ptr = 0;
        info.rom_size = k054539rom.length;
        info.rom_mask = 0xffff_ffff;
        for (i = 0; i < 32; i++) {
            if ((1 << i) >=info.rom_size) {
                info.rom_mask = (1 << i) -1;
                break;
            }
        }
        if (irq != null) {
            Timer.pulseInternal(new Atime(0, (long) (1e18 / 480)), K054539::k054539_irq, "k054539_irq");
        }
    }

    static void k054539_w(int chip, int offset, byte data) {
        int latch, offs, ch, pan;
        int regptr_offset;
        latch = ((info.k054539_flags & 4) != 0) && ((info.regs[0x22f] & 1) != 0) ? 1 : 0;
        if (latch != 0 && offset < 0x100) {
            offs = (offset & 0x1f) - 0xc;
            ch = offset >> 5;
            if (offs >= 0 && offs <= 2) {
                info.k054539_posreg_latch[ch][offs] = data;
                return;
            }
        } else {
            switch (offset) {
                case 0x13f:
                    pan = data >= 0x11 && data <= 0x1f ? data - 0x11 : 0x18 - 0x11;
                    if (apan != null) {
                        apan.accept(info.pantab[pan], info.pantab[0xe - pan]);
                    }
                    break;
                case 0x214:
                    if (latch != 0) {
                        for (ch = 0; ch < 8; ch++) {
                            if ((data & (1 << ch)) != 0) {
                                regptr_offset = (ch << 5) + 0xc;
                                info.regs[regptr_offset] = info.k054539_posreg_latch[ch][0];
                                info.regs[regptr_offset + 1] = info.k054539_posreg_latch[ch][1];
                                info.regs[regptr_offset + 2] = info.k054539_posreg_latch[ch][2];
                                k054539_keyon(ch);
                            }
                        }
                    } else {
                        for (ch = 0; ch < 8; ch++) {
                            if ((data & (1 << ch)) != 0) {
                                k054539_keyon(ch);
                            }
                        }
                    }
                    break;
                case 0x215:
                    for (ch = 0; ch < 8; ch++) {
                        if ((data & (1 << ch)) != 0) {
                            k054539_keyoff(ch);
                        }
                    }
                    break;
                case 0x22d:
                    if (info.regs[0x22e] == (byte) 0x80) {
                        if (zoneflag == 1) {
                            if (info.cur_ptr % 2 == 0) {
                                info.ram[info.cur_ptr / 2] = (short) (((data & 0xff) << 8) | (info.ram[info.cur_ptr / 2] & 0xffff));
                            } else if (info.cur_ptr % 2 == 1) {
                                info.ram[info.cur_ptr / 2] = (short) ((info.ram[info.cur_ptr / 2] & 0xff00) | (data & 0xff));
                            }
                        } else if (zoneflag == 2) {
                            k054539rom[zonedata + info.cur_ptr] = data;
                        }
                    }
                    info.cur_ptr++;
                    if (info.cur_ptr == info.cur_limit) {
                        info.cur_ptr = 0;
                    }
                    break;
                case 0x22e:
                    if (data == (byte) 0x80) {
                        zoneflag = 1;
                    } else {
                        zoneflag = 2;
                        zonedata = 0x2_0000 * data;
                    }
                    info.cur_limit = data == (byte) 0x80 ? 0x4000 : 0x2_0000;
                    info.cur_ptr = 0;
                    break;
                default:
                    break;
            }
        }
        info.regs[offset] = data;
    }

    public static byte k054539_r(int chip, int offset) {
        switch (offset) {
            case 0x22d:
                if ((info.regs[0x22f] & 0x10) != 0) {
                    byte res = 0;
                    if (zoneflag == 1) {
                        if (info.cur_ptr % 2 == 0) {
                            res = (byte) (info.ram[info.cur_ptr / 2] >> 8);
                        } else if (info.cur_ptr % 2 == 1) {
                            res = (byte) info.ram[info.cur_ptr / 2];
                        }
                    } else if (zoneflag == 2) {
                        res = k054539rom[zonedata + info.cur_ptr];
                    }
                    info.cur_ptr++;
                    if (info.cur_ptr == info.cur_limit) {
                        info.cur_ptr = 0;
                    }
                    return res;
                } else {
                    return 0;
                }
            case 0x22c:
                break;
            default:
                break;
        }
        return info.regs[offset];
    }

    public static void k054539_start(int clock) {
        int i;
        info = new k054539_info();
        info.voltab = new double[256];
        info.pantab = new double[0xf];
        info.k054539_gain = new double[8];
        info.k054539_posreg_latch = new byte[8][];
        info.regs = new byte[0x230];

        for (i = 0; i < 8; i++) {
            info.k054539_gain[i] = 1.0;
            info.k054539_posreg_latch[i] = new byte[3];
        }
        info.k054539_flags = 0;
        info.channels = new k054539_channel[8];
        irq = null;
        switch (Machine.sBoard) {
            case "Konami 68000":
                switch (Machine.sName) {
                    case "prmrsocr":
                        irq = Konami68000::sound_nmi;
                        break;
                }
                break;
        }
        for (i = 0; i < 256; i++) {
            info.voltab[i] = Math.pow(10.0, (-36.0 * (double) i / (double) 0x40) / 20.0) / 4.0;
        }
        for (i = 0; i < 0xf; i++) {
            info.pantab[i] = Math.sqrt(i) / Math.sqrt(0xe);
        }
        k054539_init_chip(clock);
    }

    public static void k054539_0_w(int offset, byte data) {
        k054539_w(0, offset, data);
    }

    public static byte k054539_0_r(int offset) {
        return k054539_r(0, offset);
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 3; j++) {
                writer.write(info.k054539_posreg_latch[i][j]);
            }
        }
        writer.write(info.k054539_flags);
        writer.write(info.regs, 0, 0x230);
        for (i = 0; i < info.ram.length; i++) {
            writer.write(info.ram[i]);
        }
        writer.write(info.reverb_pos);
        writer.write(info.cur_ptr);
        writer.write(info.cur_limit);
        for (i = 0; i < 8; i++) {
            writer.write(info.channels[i].pos);
            writer.write(info.channels[i].pfrac);
            writer.write(info.channels[i].val);
            writer.write(info.channels[i].pval);
        }
        writer.write(zoneflag);
        writer.write(zonedata);
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i, j;
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 3; j++) {
                info.k054539_posreg_latch[i][j] = reader.readByte();
            }
        }
        info.k054539_flags = reader.readInt32();
        info.regs = reader.readBytes(0x230);
        for (i = 0; i < info.ram.length; i++) {
            info.ram[i] = reader.readInt16();
        }
        info.reverb_pos = reader.readInt32();
        info.cur_ptr = reader.readInt32();
        info.cur_limit = reader.readInt32();
        for (i = 0; i < 8; i++) {
            info.channels[i].pos = reader.readInt32();
            info.channels[i].pfrac = reader.readInt32();
            info.channels[i].val = reader.readInt32();
            info.channels[i].pval = reader.readInt32();
        }
        zoneflag = reader.readInt32();
        zonedata = reader.readInt32();
    }
}
