/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Machine;
import m1.mame.konami68000.Konami68000;
import m1.mame.suna8.SunA8;


public class Sample {

    public static class sample_channel {

        public short[] source;
        public int source_length;
        public int source_num;
        public int pos;
        public int frac;
        public int step;
        public int basefreq;
        public byte loop;
        public byte paused;
    }

    public static class samples_info {

        public int numchannels;
        public sample_channel[] channel;
        public loaded_samples[] samples;
        // starthandler
        public Runnable starthandler;
    }

    public static class loaded_sample {

        public int length;
        public int frequency;
    }

    public static class loaded_samples {

        public int total;
        public loaded_sample[] sample;
    }

    public static final samples_info info = new samples_info();

    public static void sample_start_raw_n(int num, int channel, short[] sampledata, int samples, int frequency, int loop) {
        Sound.samplestream.stream_update();
        info.channel[channel].source_length = samples;
        info.channel[channel].source = new short[samples];
        System.arraycopy(sampledata, 0, info.channel[channel].source, 0, samples);
        info.channel[channel].source_num = -1;
        info.channel[channel].pos = 0;
        info.channel[channel].frac = 0;
        info.channel[channel].basefreq = frequency;
        info.channel[channel].step = (int) (((long) info.channel[channel].basefreq << 24) / 48000);
        info.channel[channel].loop = (byte) loop;
    }

    public static void sample_start_raw(int channel, short[] sampledata, int samples, int frequency, int loop) {
        sample_start_raw_n(0, channel, sampledata, samples, frequency, loop);
    }

    public static void sample_stop_n(int num, int channel) {
        Sound.samplestream.stream_update();
        info.channel[channel].source = null;
        info.channel[channel].source_num = -1;
    }

    public static void sample_stop(int channel) {
        sample_stop_n(0, channel);
    }

    public static int sample_playing_n(int num, int channel) {
        Sound.samplestream.stream_update();
        return (info.channel[channel].source != null) ? 1 : 0;
    }

    public static int sample_playing(int channel) {
        return sample_playing_n(0, channel);
    }

    public static void sample_update_sound(int offset, int length) {
        int i, j;
        if (info.channel[0].source != null && info.channel[0].paused == 0) {
            int pos = info.channel[0].pos;
            int frac = info.channel[0].frac;
            int step = info.channel[0].step;
            int sample_length = info.channel[0].source_length;
            for (i = 0; i < length; i++) {
                int sample1 = info.channel[0].source[pos];
                int sample2 = info.channel[0].source[(pos + 1) % sample_length];
                int fracmult = frac >> (24 - 14);
                Sound.samplestream.streamoutput[0][offset + i] = ((0x4000 - fracmult) * sample1 + fracmult * sample2) >> 14;
                frac += step;
                pos += frac >> 24;
                frac = frac & ((1 << 24) - 1);
                if (pos >= sample_length) {
                    if (info.channel[0].loop != 0) {
                        pos %= sample_length;
                    } else {
                        info.channel[0].source = null;
                        info.channel[0].source_num = -1;
                        if (i + 1 < length) {
                            for (j = i + 1; j < length; j++) {
                                Sound.samplestream.streamoutput[0][offset + j] = 0;
                            }
                        }
                        break;
                    }
                }
            }
            info.channel[0].pos = pos;
            info.channel[0].frac = frac;
        } else {
            for (i = 0; i < length; i++) {
                Sound.samplestream.streamoutput[0][offset + i] = 0;
            }
        }
    }

    public static void samples_start() {
        int i;
        info.numchannels = 1;
        info.channel = new sample_channel[info.numchannels];
        for (i = 0; i < info.numchannels; i++) {
            info.channel[i].source = null;
            info.channel[i].source_num = -1;
            info.channel[i].step = 0;
            info.channel[i].loop = 0;
            info.channel[i].paused = 0;
        }
        switch (Machine.sName) {
            case "starfigh":
                info.starthandler = SunA8::suna8_sh_start;
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
                info.starthandler = Konami68000::tmnt_decode_sample;
                break;
            default:
                info.starthandler = null;
                break;
        }
        if (info.starthandler != null) {
            info.starthandler.run();
        }
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i;
        for (i = 0; i < info.numchannels; i++) {
            writer.write(info.channel[i].source_length);
            writer.write(info.channel[i].source_num);
            writer.write(info.channel[i].pos);
            writer.write(info.channel[i].frac);
            writer.write(info.channel[i].step);
            writer.write(info.channel[i].basefreq);
            writer.write(info.channel[i].loop);
            writer.write(info.channel[i].paused);
        }
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i;
        for (i = 0; i < info.numchannels; i++) {
            info.channel[i].source_length = reader.readInt32();
            info.channel[i].source_num = reader.readInt32();
            info.channel[i].pos = reader.readUInt32();
            info.channel[i].frac = reader.readUInt32();
            info.channel[i].step = reader.readUInt32();
            info.channel[i].basefreq = reader.readUInt32();
            info.channel[i].loop = reader.readByte();
            info.channel[i].paused = reader.readByte();
        }
    }
}
