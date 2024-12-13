/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;


import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;

import static java.lang.System.getLogger;


public class OKI6295 {

    private static final Logger logger = getLogger(OKI6295.class.getName());

    public static class adpcm_state {

        public int signal;
        public int step;
    }

    public static class ADPCMVoice {

        public boolean playing;
        public int base_offset;
        public int sample;
        public int count;
        public int volume;
    }

    public static class okim6295Struct {

        public ADPCMVoice[] voice;
        public int command;
        public int bank_offset;
        public int master_clock;
    }

    public static byte[] okirom;
    public static okim6295Struct OKI = new okim6295Struct();
    public static adpcm_state[] adpcm;
    private static final int[] index_shift = {
            -1, -1, -1, -1, 2, 4, 6, 8
    };
    private static final int[] diff_lookup = new int[49 * 16];
    private static final int[] volume_table = new int[16];
    private static int tables_computed = 0;

    private static void compute_tables() {
        int[][] nbl2bit = {
                {1, 0, 0, 0},
                {1, 0, 0, 1},
                {1, 0, 1, 0},
                {1, 0, 1, 1},
                {1, 1, 0, 0},
                {1, 1, 0, 1},
                {1, 1, 1, 0},
                {1, 1, 1, 1},
                {-1, 0, 0, 0},
                {-1, 0, 0, 1},
                {-1, 0, 1, 0},
                {-1, 0, 1, 1},
                {-1, 1, 0, 0},
                {-1, 1, 0, 1},
                {-1, 1, 1, 0},
                {-1, 1, 1, 1}
        };
        int step, nib;
        for (step = 0; step <= 48; step++) {
            int stepval = (int) Math.floor(16.0 * Math.pow(11.0 / 10.0, step));
            for (nib = 0; nib < 16; nib++) {
                diff_lookup[step * 16 + nib] = nbl2bit[nib][0] *
                        (stepval * nbl2bit[nib][1] +
                                stepval / 2 * nbl2bit[nib][2] +
                                stepval / 4 * nbl2bit[nib][3] +
                                stepval / 8);
            }
        }
        for (step = 0; step < 16; step++) {
            double dout = 256.0;
            int vol = step;
            while (vol-- > 0)
                dout /= 1.412537545;
            volume_table[step] = (int) dout;
        }
        tables_computed = 1;
    }

    private static void reset_adpcm(int i) {
        if (tables_computed == 0) {
            compute_tables();
        }
        adpcm[i].signal = -2;
        adpcm[i].step = 0;
    }

    private static short clock_adpcm(int i, byte nibble) {
        adpcm[i].signal += diff_lookup[adpcm[i].step * 16 + (nibble & 15)];
        if (adpcm[i].signal > 2047)
            adpcm[i].signal = 2047;
        else if (adpcm[i].signal < -2048)
            adpcm[i].signal = -2048;
        adpcm[i].step += index_shift[nibble & 7];
        if (adpcm[i].step > 48)
            adpcm[i].step = 48;
        else if (adpcm[i].step < 0)
            adpcm[i].step = 0;
        return (short) (adpcm[i].signal << 4);
    }

    private static void generate_adpcm(int i, short[] buffer, int samples) {
        int i1 = 0;
        if (OKI.voice[i].playing) {
            int bbase = OKI.bank_offset + OKI.voice[i].base_offset;
            int sample = OKI.voice[i].sample;
            int count = OKI.voice[i].count;
            while (samples != 0) {
                int nibble = okirom[bbase + sample / 2] >> (((sample & 1) << 2) ^ 4);
                buffer[i1] = (short) (clock_adpcm(i, (byte) nibble) * OKI.voice[i].volume / 256);
                i1++;
                samples--;
                if (++sample >= count) {
                    OKI.voice[i].playing = false;
                    break;
                }
            }
            OKI.voice[i].sample = sample;
        }
        while ((samples--) != 0) {
            buffer[i1] = 0;
            i1++;
        }
    }

    public static void okim6295_update(int offset, int length) {
        int i;
logger.log(Level.INFO, "offset: " + offset + ", length: " + length + ", streamoutput[0]: " + Sound.okistream.streamoutput[0].length);
        for (i = 0; i < length; i++) {
            Sound.okistream.streamoutput[0][offset + i] = 0;
        }
        for (i = 0; i < 4; i++) {
            short[] sample_data = new short[10000];
            int remaining = length;
            while (remaining != 0) {
                int samples1 = Math.min(remaining, 10000);
                int samp;
                generate_adpcm(i, sample_data, samples1);
                for (samp = 0; samp < length; samp++) {
                    Sound.okistream.streamoutput[0][offset + samp] += sample_data[samp];
                }
                remaining -= samples1;
            }
        }
    }

    public static void okim6295_start() {
        int voice;
        compute_tables();
        OKI.command = -1;
        OKI.bank_offset = 0;
        OKI.master_clock = 1_000_000;
        OKI.voice = new ADPCMVoice[4];
        adpcm = new adpcm_state[4];
        for (voice = 0; voice < 4; voice++) {
            OKI.voice[voice] = new ADPCMVoice();
            OKI.voice[voice].volume = 255;
            adpcm[voice] = new adpcm_state();
            reset_adpcm(voice);
        }
    }

    public static void okim6295_reset() {
        Sound.okistream.stream_update();
        for (int i = 0; i < 4; i++) {
            OKI.voice[i].playing = false;
        }
    }

    public static void okim6295_set_bank_base(int base1) {
        Sound.okistream.stream_update();
        OKI.bank_offset = base1;
    }

    public static void okim6295_set_pin7(int pin7) {
        int divisor = pin7 != 0 ? 132 : 165;
        //stream_set_sample_rate(info->stream, info->master_clock / divisor);
    }

    public static int okim6295_status_r() {
        int result = 0xf0;
        Sound.okistream.stream_update();
        for (int i = 0; i < 4; i++) {
            ADPCMVoice voice = OKI.voice[i];
            if (voice.playing)
                result |= 1 << i;
        }
        return result;
    }

    private static void okim6295_data_w(int num, int data) {
        if (OKI.command != -1) {
            int temp = data >> 4, i, start, stop;
            int baseoffset;
            Sound.okistream.stream_update();
            for (i = 0; i < 4; i++, temp >>= 1) {
                if ((temp & 1) != 0) {
                    baseoffset = OKI.bank_offset + OKI.command * 8;
                    start = ((okirom[baseoffset + 0] << 16) + (okirom[baseoffset + 1] << 8) + okirom[baseoffset + 2]) & 0x3_ffff;
                    stop = ((okirom[baseoffset + 3] << 16) + (okirom[baseoffset + 4] << 8) + okirom[baseoffset + 5]) & 0x3_ffff;
                    if (start < stop) {
                        if (!OKI.voice[i].playing) {
                            OKI.voice[i].playing = true;
                            OKI.voice[i].base_offset = start;
                            OKI.voice[i].sample = 0;
                            OKI.voice[i].count = 2 * (stop - start + 1);
                            reset_adpcm(i);
                            OKI.voice[i].volume = volume_table[data & 0x0f];
                        } else {

                        }
                    } else {
                        OKI.voice[i].playing = false;
                    }
                }
            }
            OKI.command = -1;
        } else if ((data & 0x80) != 0) {
            OKI.command = data & 0x7f;
        } else {
            int temp = data >> 3, i;
            Sound.okistream.stream_update();
            for (i = 0; i < 4; i++, temp >>= 1) {
                if ((temp & 1) != 0) {
                    OKI.voice[i].playing = false;
                }
            }
        }
    }

    public static byte okim6295_status_0_r() {
        int i;
        byte result;
        result = (byte) 0xf0;
        Sound.okistream.stream_update();
        for (i = 0; i < 4; i++) {
            if (OKI.voice[i].playing) {
                result |= (byte) (1 << i);
            }
        }
        return result;
    }

    public static int okim6295_status_0_lsb_r() {
        return okim6295_status_r();
    }

    public static void okim6295_data_0_w(byte data) {
        okim6295_data_w(0, data);
    }

    public static void okim6295_data_0_lsb_w(byte data) {
        //if (ACCESSING_BITS_0_7)
        okim6295_data_w(0, data & 0xff);
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i;
        writer.write(OKI.command);
        writer.write(OKI.bank_offset);
        for (i = 0; i < 4; i++) {
            writer.write(OKI.voice[i].playing);
            writer.write(OKI.voice[i].sample);
            writer.write(OKI.voice[i].count);
            writer.write(OKI.voice[i].volume);
            writer.write(OKI.voice[i].base_offset);
            writer.write(adpcm[i].signal);
            writer.write(adpcm[i].step);
        }
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i;
        OKI.command = reader.readInt32();
        OKI.bank_offset = reader.readInt32();
        for (i = 0; i < 4; i++) {
            OKI.voice[i].playing = reader.readBoolean();
            OKI.voice[i].sample = reader.readUInt32();
            OKI.voice[i].count = reader.readUInt32();
            OKI.voice[i].volume = reader.readUInt32();
            OKI.voice[i].base_offset = reader.readUInt32();
            adpcm[i].signal = reader.readInt32();
            adpcm[i].step = reader.readInt32();
        }
    }
}
