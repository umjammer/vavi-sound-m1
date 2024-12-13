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


public class Upd7759 {

    public static class upd7759_chip {

        public int pos;
        public int step;
        public Atime clock_period;
        public EmuTimer timer;

        public byte fifo_in;
        public byte reset;
        public byte start;
        public byte drq;
        // drqcallback
        public Consumer<Integer> drqcallback;

        public byte state;
        public int clocks_left;
        public short nibbles_left;
        public byte repeat_count;
        public byte post_drq_state;
        public int post_drq_clocks;
        public byte req_sample;
        public byte last_sample;
        public byte block_header;
        public byte sample_rate;
        public byte first_valid_header;
        public int offset;
        public int repeat_offset;

        public byte adpcm_state;
        public byte adpcm_data;
        public short sample;

        public int rombase;
        public int romoffset;
    }

    public static final int[][] upd7759_step = {
            {0, 0, 1, 2, 3, 5, 7, 10, 0, 0, -1, -2, -3, -5, -7, -10},
            {0, 1, 2, 3, 4, 6, 8, 13, 0, -1, -2, -3, -4, -6, -8, -13},
            {0, 1, 2, 4, 5, 7, 10, 15, 0, -1, -2, -4, -5, -7, -10, -15},
            {0, 1, 3, 4, 6, 9, 13, 19, 0, -1, -3, -4, -6, -9, -13, -19},
            {0, 2, 3, 5, 8, 11, 15, 23, 0, -2, -3, -5, -8, -11, -15, -23},
            {0, 2, 4, 7, 10, 14, 19, 29, 0, -2, -4, -7, -10, -14, -19, -29},
            {0, 3, 5, 8, 12, 16, 22, 33, 0, -3, -5, -8, -12, -16, -22, -33},
            {1, 4, 7, 10, 15, 20, 29, 43, -1, -4, -7, -10, -15, -20, -29, -43},
            {1, 4, 8, 13, 18, 25, 35, 53, -1, -4, -8, -13, -18, -25, -35, -53},
            {1, 6, 10, 16, 22, 31, 43, 64, -1, -6, -10, -16, -22, -31, -43, -64},
            {2, 7, 12, 19, 27, 37, 51, 76, -2, -7, -12, -19, -27, -37, -51, -76},
            {2, 9, 16, 24, 34, 46, 64, 96, -2, -9, -16, -24, -34, -46, -64, -96},
            {3, 11, 19, 29, 41, 57, 79, 117, -3, -11, -19, -29, -41, -57, -79, -117},
            {4, 13, 24, 36, 50, 69, 96, 143, -4, -13, -24, -36, -50, -69, -96, -143},
            {4, 16, 29, 44, 62, 85, 118, 175, -4, -16, -29, -44, -62, -85, -118, -175},
            {6, 20, 36, 54, 76, 104, 144, 214, -6, -20, -36, -54, -76, -104, -144, -214},
    };
    public static final int[] upd7759_state = {
        -1, -1, 0, 0, 1, 2, 2, 3, -1, -1, 0, 0, 1, 2, 2, 3
    };

    public static upd7759_chip chip;
    public static byte[] updrom;

    public static void update_adpcm(int data) {
        chip.sample += (short) upd7759_step[chip.adpcm_state][data];
        chip.adpcm_state += (byte) upd7759_state[data];
        if (chip.adpcm_state < 0) {
            chip.adpcm_state = 0;
        } else if (chip.adpcm_state > 15) {
            chip.adpcm_state = 15;
        }
    }

    public static void advance_state() {
        switch (chip.state) {
            case 0:
                chip.clocks_left = 4;
                break;
            case 1:
                chip.drq = 0;
                chip.clocks_left = chip.post_drq_clocks;
                chip.state = chip.post_drq_state;
                break;
            case 2:
                chip.req_sample = (byte) (updrom != null ? chip.fifo_in : 0x10);
                chip.clocks_left = 70;
                chip.state = 3;
                break;
            case 3:
                chip.drq = 1;
                chip.clocks_left = 44;
                chip.state = 4;
                break;
            case 4:
                chip.last_sample = updrom != null ? updrom[0] : chip.fifo_in;
                chip.drq = 1;
                chip.clocks_left = 28;
                chip.state = (byte) ((chip.req_sample > chip.last_sample) ? 0 : 5);
                break;
            case 5:
                chip.drq = 1;
                chip.clocks_left = 32;
                chip.state = 6;
                break;
            case 6:
                chip.offset = (updrom != null ? updrom[chip.req_sample * 2 + 5] : chip.fifo_in) << 9;
                chip.drq = 1;
                chip.clocks_left = 44;
                chip.state = 7;
                break;
            case 7:
                chip.offset |= (updrom != null ? updrom[chip.req_sample * 2 + 6] : chip.fifo_in) << 1;
                chip.drq = 1;
                chip.clocks_left = 36;
                chip.state = 8;
                break;
            case 8:
                chip.offset++;
                chip.first_valid_header = 0;
                chip.drq = 1;
                chip.clocks_left = 36;
                chip.state = 9;
                break;
            case 9:
                if (chip.repeat_count != 0) {
                    chip.repeat_count--;
                    chip.offset = chip.repeat_offset;
                }
                chip.block_header = updrom != null ? updrom[chip.offset++ & 0x1_ffff] : chip.fifo_in;
                chip.drq = 1;
                switch (chip.block_header & 0xc0) {
                    case 0x00:
                        chip.clocks_left = 1024 * ((chip.block_header & 0x3f) + 1);
                        chip.state = (byte) ((chip.block_header == 0 && chip.first_valid_header != 0) ? 0 : 9);
                        chip.sample = 0;
                        chip.adpcm_state = 0;
                        break;
                    case 0x40:
                        chip.sample_rate = (byte) ((chip.block_header & 0x3f) + 1);
                        chip.nibbles_left = 256;
                        chip.clocks_left = 36;
                        chip.state = 11;
                        break;
                    case 0x80:
                        chip.sample_rate = (byte) ((chip.block_header & 0x3f) + 1);
                        chip.clocks_left = 36;
                        chip.state = 10;
                        break;
                    case 0xc0:
                        chip.repeat_count = (byte) ((chip.block_header & 7) + 1);
                        chip.repeat_offset = chip.offset;
                        chip.clocks_left = 36;
                        chip.state = 9;
                        break;
                }
                if (chip.block_header != 0) {
                    chip.first_valid_header = 1;
                }
                break;
            case 10:
                chip.nibbles_left = (short) ((updrom != null ? updrom[chip.offset++ & 0x1_ffff] : chip.fifo_in) + 1);
                chip.drq = 1;
                chip.clocks_left = 36;
                chip.state = 11;
                break;
            case 11:
                chip.adpcm_data = updrom != null ? updrom[chip.offset++ & 0x1_ffff] : chip.fifo_in;
                update_adpcm(chip.adpcm_data >> 4);
                chip.drq = 1;
                chip.clocks_left = chip.sample_rate * 4;
                if (--chip.nibbles_left == 0) {
                    chip.state = 9;
                } else {
                    chip.state = 12;
                }
                break;
            case 12:
                update_adpcm(chip.adpcm_data & 15);
                chip.clocks_left = chip.sample_rate * 4;
                if (--chip.nibbles_left == 0) {
                    chip.state = 9;
                } else {
                    chip.state = 11;
                }
                break;
        }
        if (chip.drq != 0) {
            chip.post_drq_state = chip.state;
            chip.post_drq_clocks = chip.clocks_left - 21;
            chip.state = 1;
            chip.clocks_left = 21;
        }
    }

    public static void upd7759_update(int offset, int length) {
        int clocks_left = chip.clocks_left;
        short sample = chip.sample;
        int step = chip.step;
        int pos = chip.pos;
        int i = 0, j;
        if (chip.state != 0) {
            for (i = 0; i < length; i++) {
                Sound.upd7759stream.streamoutput[0][offset + i] = sample << 7;
                pos += step;
                while (updrom != null && pos >= 0x10_0000) {
                    int clocks_this_time = pos >> 20;
                    if (clocks_this_time > clocks_left) {
                        clocks_this_time = clocks_left;
                    }
                    pos -= clocks_this_time * 0x10_0000;
                    clocks_left -= clocks_this_time;
                    if (clocks_left == 0) {
                        advance_state();
                        if (chip.state == 0) {
                            break;
                        }
                        clocks_left = chip.clocks_left;
                        sample = chip.sample;
                    }
                }
            }
        }
        if (i < length - 1) {
            for (j = i; j < length; j++) {
                Sound.upd7759stream.streamoutput[0][offset + j] = 0;
            }
        }
        chip.clocks_left = clocks_left;
        chip.pos = pos;
    }

    public static void upd7759_slave_update() {
        byte olddrq = chip.drq;
        Sound.upd7759stream.stream_update();
        //stream_update(chip.channel);
        advance_state();
        if (olddrq != chip.drq && chip.drqcallback != null) {
            chip.drqcallback.accept((int) chip.drq);
        }
        if (chip.state != 0) {
            Timer.adjustPeriodic(chip.timer, Attotime.attotime_mul(chip.clock_period, chip.clocks_left), Attotime.ATTOTIME_NEVER);
        }
    }

    public static void upd7759_reset() {
        chip.pos = 0;
        chip.fifo_in = 0;
        chip.drq = 0;
        chip.state = 0;
        chip.clocks_left = 0;
        chip.nibbles_left = 0;
        chip.repeat_count = 0;
        chip.post_drq_state = 0;
        chip.post_drq_clocks = 0;
        chip.req_sample = 0;
        chip.last_sample = 0;
        chip.block_header = 0;
        chip.sample_rate = 0;
        chip.first_valid_header = 0;
        chip.offset = 0;
        chip.repeat_offset = 0;
        chip.adpcm_state = 0;
        chip.adpcm_data = 0;
        chip.sample = 0;
        if (chip.timer != null) {
            Timer.adjustPeriodic(chip.timer, Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
        }
    }

    public static void upd7759_start(int clock) {
        chip = new upd7759_chip();
        chip.step = 4 * 0x10_0000;
        chip.clock_period = new Atime(0, (long) (1e18 / clock));
        chip.state = 0;
        chip.rombase = 0;
        if (updrom == null) {
            chip.timer = Timer.allocCommon(Upd7759::upd7759_slave_update, "upd7759_slave_update", false);
        }
        chip.reset = 1;
        chip.start = 1;
        upd7759_reset();
    }

    public static void upd7759_reset_w(int which, byte data) {
        byte oldreset = chip.reset;
        chip.reset = (byte) ((data != 0) ? 1 : 0);
        Sound.upd7759stream.stream_update();
        if (oldreset != 0 && chip.reset == 0) {
            upd7759_reset();
        }
    }

    public static void upd7759_start_w(int which, byte data) {
        byte oldstart = chip.start;
        chip.start = (byte) ((data != 0) ? 1 : 0);
        Sound.upd7759stream.stream_update();
        if (chip.state == 0 && oldstart == 0 && chip.start != 0 && chip.reset != 0) {
            chip.state = 2;
            if (chip.timer != null) {
                Timer.adjustPeriodic(chip.timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
            }
        }
    }

    public static void upd7759_port_w(int which, byte data) {
        chip.fifo_in = data;
    }

    public static int upd7759_busy_r(int which) {
        return (chip.state == 0) ? 1 : 0;
    }

    public static void upd7759_set_bank_base(int which, int base1) {
        //chip.rom = chip.rombase + base1;
        chip.romoffset = base1;
    }

    public static void upd7759_0_start_w(byte data) {
        upd7759_start_w(0, data);
    }

    public static void upd7759_0_reset_w(byte data) {
        upd7759_reset_w(0, data);
    }

    public static void upd7759_0_port_w(byte data) {
        upd7759_port_w(0, data);
    }

    public static byte upd7759_0_busy_r() {
        return (byte) upd7759_busy_r(0);
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        writer.write(chip.pos);
        writer.write(chip.step);
        writer.write(chip.fifo_in);
        writer.write(chip.reset);
        writer.write(chip.start);
        writer.write(chip.drq);
        writer.write(chip.state);
        writer.write(chip.clocks_left);
        writer.write(chip.nibbles_left);
        writer.write(chip.repeat_count);
        writer.write(chip.post_drq_state);
        writer.write(chip.post_drq_clocks);
        writer.write(chip.req_sample);
        writer.write(chip.last_sample);
        writer.write(chip.block_header);
        writer.write(chip.sample_rate);
        writer.write(chip.first_valid_header);
        writer.write(chip.offset);
        writer.write(chip.repeat_offset);
        writer.write(chip.adpcm_state);
        writer.write(chip.adpcm_data);
        writer.write(chip.sample);
        writer.write(chip.romoffset);
        writer.write(chip.rombase);
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        chip.pos = reader.readUInt32();
        chip.step = reader.readUInt32();
        chip.fifo_in = reader.readByte();
        chip.reset = reader.readByte();
        chip.start = reader.readByte();
        chip.drq = reader.readByte();
        chip.state = reader.readSByte();
        chip.clocks_left = reader.readInt32();
        chip.nibbles_left = reader.readUInt16();
        chip.repeat_count = reader.readByte();
        chip.post_drq_state = reader.readSByte();
        chip.post_drq_clocks = reader.readInt32();
        chip.req_sample = reader.readByte();
        chip.last_sample = reader.readByte();
        chip.block_header = reader.readByte();
        chip.sample_rate = reader.readByte();
        chip.first_valid_header = reader.readByte();
        chip.offset = reader.readUInt32();
        chip.repeat_offset = reader.readUInt32();
        chip.adpcm_state = reader.readSByte();
        chip.adpcm_data = reader.readByte();
        chip.sample = reader.readInt16();
        chip.romoffset = reader.readUInt32();
        chip.rombase = reader.readInt32();
    }
}
