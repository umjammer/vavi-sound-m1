/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;
import java.util.function.Consumer;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;


public class MSM5205 {

    public static class MSM5205Voice {

        // vclk_delegate
        public Consumer<Integer> vclk_callback;
        public int select;
        /** number of stream system */
        public Streams stream;
        public int index;
        /** clock rate */
        public int clock;
        /** next adpcm data */
        public int data;
        /** vclk signal (external mode) */
        public int vclk;
        /** reset pin signal */
        public int reset;
        /** prescaler selector S1 and S2 */
        public int prescaler;
        /** bit width selector -3B/4B */
        public int bitwidth;
        /** current ADPCM signal */
        public int signal;
        /** current ADPCM step */
        public int step;
    }

    public static int[] diff_lookup;
    public static final int[] index_shift = {
            -1, -1, -1, -1, 2, 4, 6, 8
    };
    public MSM5205Voice voice;
    public static final EmuTimer[] timer = new EmuTimer[2];
    public static final MSM5205[] mm1 = new MSM5205[2];

    public static void ComputeTables() {
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
        diff_lookup = new int[49 * 16];
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
    }

    public void MSM5205_update(int offset, int length) {
        int i;
        if (voice.signal != 0) {
            short val = (short) (voice.signal * 16);
            for (i = 0; i < length; i++) {
                voice.stream.streamoutput[0][offset + i] = val;
            }
        } else {
            for (i = 0; i < length; i++) {
                voice.stream.streamoutput[0][offset + i] = 0;
            }
        }
    }

    public static void MSM5205_vclk_callback0() {
        int val;
        int new_signal;
        if (mm1[0].voice.vclk_callback != null) {
            mm1[0].voice.vclk_callback.accept(mm1[0].voice.index);
        }
        if (mm1[0].voice.reset != 0) {
            new_signal = 0;
            mm1[0].voice.step = 0;
        } else {
            val = mm1[0].voice.data;
            new_signal = mm1[0].voice.signal + diff_lookup[mm1[0].voice.step * 16 + (val & 15)];
            if (new_signal > 2047) {
                new_signal = 2047;
            } else if (new_signal < -2048) {
                new_signal = -2048;
            }
            mm1[0].voice.step += index_shift[val & 7];
            if (mm1[0].voice.step > 48) {
                mm1[0].voice.step = 48;
            } else if (mm1[0].voice.step < 0) {
                mm1[0].voice.step = 0;
            }
        }
        if (mm1[0].voice.signal != new_signal) {
            mm1[0].voice.stream.stream_update();
            mm1[0].voice.signal = new_signal;
        }
    }

    public static void MSM5205_vclk_callback1() {
        int val;
        int new_signal;
        if (mm1[1].voice.vclk_callback != null) {
            mm1[1].voice.vclk_callback.accept(mm1[1].voice.index);
        }
        if (mm1[1].voice.reset != 0) {
            new_signal = 0;
            mm1[1].voice.step = 0;
        } else {
            val = mm1[1].voice.data;
            new_signal = mm1[1].voice.signal + diff_lookup[mm1[1].voice.step * 16 + (val & 15)];
            if (new_signal > 2047) {
                new_signal = 2047;
            } else if (new_signal < -2048) {
                new_signal = -2048;
            }
            mm1[1].voice.step += index_shift[val & 7];
            if (mm1[1].voice.step > 48) {
                mm1[1].voice.step = 48;
            } else if (mm1[1].voice.step < 0) {
                mm1[1].voice.step = 0;
            }
        }
        if (mm1[1].voice.signal != new_signal) {
            mm1[1].voice.stream.stream_update();
            mm1[1].voice.signal = new_signal;
        }
    }

    public void msm5205_reset() {
        voice.data = 0;
        voice.vclk = 0;
        voice.reset = 0;
        voice.signal = 0;
        voice.step = 0;
        msm5205_playmode_w(voice.index, voice.select);
    }

    // vclk_delegate
    public static void msm5205_start(int sndindex, int clock, Consumer<Integer> vclk, int select) {
        //struct MSM5205Voice *voice;
//        voice = auto_malloc(sizeof( * voice));
//        memset(voice, 0, sizeof( * voice));
//        sndintrf_register_token(voice);
        //voice.intf = config;
        mm1[sndindex] = new MSM5205();
        mm1[sndindex].voice.vclk_callback = vclk;
        mm1[sndindex].voice.select = select;
        mm1[sndindex].voice.index = sndindex;
        mm1[sndindex].voice.clock = clock;
        ComputeTables();
        // stream system initialize
        mm1[sndindex].voice.stream = new Streams(clock, 0, 1, mm1[sndindex]::MSM5205_update);
        if (sndindex == 0) {
            timer[0] = Timer.allocCommon(MSM5205::MSM5205_vclk_callback0, "msm5205_vclk_callback0", false);
        } else if (sndindex == 1) {
            timer[1] = Timer.allocCommon(MSM5205::MSM5205_vclk_callback1, "msm5205_vclk_callback1", false);
        }
        mm1[sndindex].msm5205_reset();
    }

    public static void null_vclk(int i) {
    }

    public static void msm5205_vclk_w(int num, int vclk) {
        if (mm1[num].voice.prescaler != 0) {
            //logger.log(Level.TRACE, "error: msm5205_vclk_w() called with chip = %d, but VCLK selected master mode\n", num);
        } else {
            if (mm1[num].voice.vclk != vclk) {
                mm1[num].voice.vclk = vclk;
                if (vclk == 0) {
                    if (num == 0) {
                        MSM5205_vclk_callback0();
                    } else if (num == 1) {
                        MSM5205_vclk_callback1();
                    }
                }
            }
        }
    }

    public static void msm5205_reset_w(int num, int reset) {
        mm1[num].voice.reset = reset;
    }

    public static void msm5205_data_w(int num, int data) {
        if (mm1[num].voice.bitwidth == 4) {
            mm1[num].voice.data = data & 0x0f;
        } else {
            mm1[num].voice.data = (data & 0x07) << 1; /* unknown */
        }
    }

    public static void msm5205_playmode_w(int num, int select) {
        int[] prescaler_table = {
                96, 48, 64, 0
        };
        int prescaler = prescaler_table[select & 3];
        int bitwidth = ((select & 4) != 0) ? 4 : 3;
        if (mm1[num].voice.prescaler != prescaler) {
            mm1[num].voice.stream.stream_update();
            mm1[num].voice.prescaler = prescaler;
            if (prescaler != 0) {
                Atime period = Attotime.attotime_mul(Attotime.ATTOTIME_IN_HZ(mm1[num].voice.clock), prescaler);
                Timer.adjustPeriodic(timer[num], period, period);
            } else {
                Timer.adjustPeriodic(timer[num], Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
            }
        }
        if (mm1[num].voice.bitwidth != bitwidth) {
            mm1[num].voice.stream.stream_update();
            mm1[num].voice.bitwidth = bitwidth;
        }
    }

    public void SaveStateBinary(BinaryWriter writer) {
        writer.write(voice.select);
        writer.write(voice.index);
        writer.write(voice.clock);
        writer.write(voice.data);
        writer.write(voice.vclk);
        writer.write(voice.reset);
        writer.write(voice.prescaler);
        writer.write(voice.bitwidth);
        writer.write(voice.signal);
        writer.write(voice.step);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        voice.select = reader.readInt32();
        voice.index = reader.readInt32();
        voice.clock = reader.readInt32();
        voice.data = reader.readInt32();
        voice.vclk = reader.readInt32();
        voice.reset = reader.readInt32();
        voice.prescaler = reader.readInt32();
        voice.bitwidth = reader.readInt32();
        voice.signal = reader.readInt32();
        voice.step = reader.readInt32();
    }
}
