/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime.Atime;


public class Cpuint {

    //@StructLayout(LayoutKind.Explicit)
    public static class Register implements Serializable {

        //@FieldOffset(0)
        public int d;
        //@FieldOffset(0)
        public int sd;

        //@FieldOffset(0)
        public short lowWord;
        //@FieldOffset(2)
        public short highWord;

        //@FieldOffset(0)
        public byte lowByte;
        //@FieldOffset(1)
        public byte highByte;
        //@FieldOffset(2)
        public byte highByte2;
        //@FieldOffset(3)
        public byte highByte3;

        @Override
        public String toString() {
            return "%08x".formatted(d);
        }
    }

    public enum LineState {
        CLEAR_LINE(0),
        ASSERT_LINE(1),
        HOLD_LINE(2),
        PULSE_LINE(3),
        INTERNAL_CLEAR_LINE(100 + CLEAR_LINE.v),
        INTERNAL_ASSERT_LINE(100 + ASSERT_LINE.v),
        MAX_INPUT_LINES(32 + 3),
        INPUT_LINE_NMI(MAX_INPUT_LINES.v - 3),
        INPUT_LINE_RESET(MAX_INPUT_LINES.v - 2),
        INPUT_LINE_HALT(MAX_INPUT_LINES.v - 1);
        final int v;

        LineState(int v) {
            this.v = v;
        }
    }

    public static class irq {

        public int cpunum;
        public int line;
        public LineState state;
        public int vector;
        public Atime time;

        public irq() {
        }

        public irq(int cpunum, int line, LineState state, int vector, Atime time) {
            this.cpunum = cpunum;
            this.line = line;
            this.state = state;
            this.vector = vector;
            this.time = time;
        }
    }

    public static class vec {

        public int vector;
        public Atime time;

        public vec() {
        }

        public vec(int vector, Atime time) {
            this.vector = vector;
            this.time = time;
        }
    }

    public static int[][] interrupt_vector;
    public static byte[][] input_line_state;
    public static int[][] input_line_vector;
    public static int[][] input_event_index;
    //public static int[][][] input_state;
    public static List<irq> lirq;
    public static List<vec> lvec;

    public static void cpuint_init() {
        int i, j;
        lirq = new ArrayList<>();
        lvec = new ArrayList<>();
        interrupt_vector = new int[8][35];
        input_line_state = new byte[8][35];
        input_line_vector = new int[8][35];
        input_event_index = new int[8][35];
        //input_state = new int[8][35][32];
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                input_line_state[i][j] = 0;
                interrupt_vector[i][j] = input_line_vector[i][j] = 0xff;
                input_event_index[i][j] = 0;
            }
        }
    }

    public static void cpuint_reset() {
        lirq = new ArrayList<>();
        lvec = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 35; j++) {
                interrupt_vector[i][j] = 0xff;
                input_event_index[i][j] = 0;
            }
        }
    }

    public static void cps1_irq_handler_mus(int irq) {
        cpunum_set_input_line(1, 0, (irq != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void namcos1_sound_interrupt(int irq) {
        cpunum_set_input_line(2, 1, (irq != 0) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void cpunum_set_input_line(int cpunum, int line, LineState state) {
        int vector = (line >= 0 && line < 35) ? interrupt_vector[cpunum][line] : 0xff;
        lirq.add(new irq(cpunum, line, state, vector, Timer.getCurrentTime()));
        Cpuexec.cpu[cpunum].cpunum_set_input_line_and_vector(cpunum, line, state, vector);
    }

    public static void cpunum_set_input_line_vector(int cpunum, int line, int vector) {
        if (cpunum < Cpuexec.ncpu && line >= 0 && line < LineState.MAX_INPUT_LINES.v) {
            interrupt_vector[cpunum][line] = vector;
        }
    }

    public static void cpunum_set_input_line_and_vector2(int cpunum, int line, LineState state, int vector) {
        if (line >= 0 && line < 35) {
            lirq.add(new irq(cpunum, line, state, vector, Timer.getCurrentTime()));
            Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
        }
    }

    public static void cpunum_empty_event_queue() {
        List<irq> lsirq = new ArrayList<>();
        if (lirq.isEmpty()) {
            int i1 = 1;
        }
        for (irq irq1 : lirq) {
            if (Attotime.attotime_compare(irq1.time, Timer.global_basetime) <= 0) {
                input_line_state[irq1.cpunum][irq1.line] = (byte) irq1.state.v;
                input_line_vector[irq1.cpunum][irq1.line] = irq1.vector;
                if (irq1.line == LineState.INPUT_LINE_RESET.ordinal()) {
                    if (irq1.state == LineState.ASSERT_LINE) {
                        Cpuexec.cpunum_suspend(irq1.cpunum, Cpuexec.SUSPEND_REASON_RESET, (byte) 1);
                    } else {
                        if ((irq1.state == LineState.CLEAR_LINE && Cpuexec.cpunum_is_suspended(irq1.cpunum, Cpuexec.SUSPEND_REASON_RESET)) || irq1.state == LineState.PULSE_LINE) {
                            Cpuexec.cpu[irq1.cpunum].Reset();
                        }
                        Cpuexec.cpunum_resume(irq1.cpunum, Cpuexec.SUSPEND_REASON_RESET);
                    }
                } else if (irq1.line == LineState.INPUT_LINE_HALT.v) {
                    if (irq1.state == LineState.ASSERT_LINE) {
                        Cpuexec.cpunum_suspend(irq1.cpunum, Cpuexec.SUSPEND_REASON_HALT, (byte) 1);
                    } else if (irq1.state == LineState.CLEAR_LINE) {
                        Cpuexec.cpunum_resume(irq1.cpunum, Cpuexec.SUSPEND_REASON_HALT);
                    }
                } else {
                    switch (irq1.state) {
                        case PULSE_LINE:
                            Cpuexec.cpu[irq1.cpunum].set_irq_line(irq1.line, LineState.ASSERT_LINE);
                            Cpuexec.cpu[irq1.cpunum].set_irq_line(irq1.line, LineState.CLEAR_LINE);
                            break;
                        case HOLD_LINE:
                        case ASSERT_LINE:
                            Cpuexec.cpu[irq1.cpunum].set_irq_line(irq1.line, LineState.ASSERT_LINE);
                            break;
                        case CLEAR_LINE:
                            Cpuexec.cpu[irq1.cpunum].set_irq_line(irq1.line, LineState.CLEAR_LINE);
                            break;
                    }
                    if (irq1.state != LineState.CLEAR_LINE) {
                        Cpuexec.cpu_triggerint(irq1.cpunum);
                    }
                }
                lsirq.add(irq1);
            }
        }
        for (irq irq1 : lsirq) {
            input_event_index[irq1.cpunum][irq1.line] = 0;
            lirq.remove(irq1);
        }
        if (!lirq.isEmpty()) {
            int i1 = 1;
        }
    }

    public static int cpu_irq_callback(int cpunum, int line) {
        int vector = input_line_vector[cpunum][line];
        if (input_line_state[cpunum][line] == (byte) LineState.HOLD_LINE.v) {
            Cpuexec.cpu[cpunum].set_irq_line(line, LineState.CLEAR_LINE);
            input_line_state[cpunum][line] = (byte) LineState.CLEAR_LINE.v;
        }
        return vector;
    }

    public static int cpu_0_irq_callback(int line) {
        return cpu_irq_callback(0, line);
    }

    public static int cpu_1_irq_callback(int line) {
        return cpu_irq_callback(1, line);
    }

    public static int cpu_2_irq_callback(int line) {
        return cpu_irq_callback(2, line);
    }

    public static int cpu_3_irq_callback(int line) {
        return cpu_irq_callback(3, line);
    }

    public static void SaveStateBinary_v(BinaryWriter writer) {
        int i, n;
        n = lvec.size();
        writer.write(n);
        for (i = 0; i < n; i++) {
            writer.write(lvec.get(i).vector);
            writer.write(lvec.get(i).time.seconds);
            writer.write(lvec.get(i).time.attoseconds);
        }
        for (i = n; i < 16; i++) {
            writer.write(0);
            writer.write(0);
            writer.write((long) 0);
        }
    }

    public static void LoadStateBinary_v(BinaryReader reader) throws IOException {
        int i, n;
        n = reader.readInt32();
        lvec = new ArrayList<>();
        for (i = 0; i < n; i++) {
            lvec.add(new vec());
            lvec.get(i).vector = reader.readInt32();
            lvec.get(i).time.seconds = reader.readInt32();
            lvec.get(i).time.attoseconds = reader.readInt64();
        }
        for (i = n; i < 16; i++) {
            reader.readInt32();
            reader.readInt32();
            reader.readInt64();
        }
    }

    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j, n;
        n = lirq.size();
        writer.write(n);
        for (i = 0; i < n; i++) {
            writer.write(lirq.get(i).cpunum);
            writer.write(lirq.get(i).line);
            writer.write(lirq.get(i).state.v);
            writer.write(lirq.get(i).vector);
            writer.write(lirq.get(i).time.seconds);
            writer.write(lirq.get(i).time.attoseconds);
        }
        for (i = n; i < 16; i++) {
            writer.write(0);
            writer.write(0);
            writer.write(0);
            writer.write(0);
            writer.write(0);
            writer.write((long) 0);
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                writer.write(interrupt_vector[i][j]);
            }
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                writer.write(input_line_state[i][j]);
            }
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                writer.write(input_line_vector[i][j]);
            }
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                writer.write(input_event_index[i][j]);
            }
        }
    }

    public static void LoadStateBinary(BinaryReader reader) throws IOException {
        int i, j, n;
        n = reader.readInt32();
        lirq = new ArrayList<>();
        for (i = 0; i < n; i++) {
            lirq.add(new irq());
            lirq.get(i).cpunum = reader.readInt32();
            lirq.get(i).line = reader.readInt32();
            lirq.get(i).state = LineState.values()[reader.readInt32()];
            lirq.get(i).vector = reader.readInt32();
            lirq.get(i).time.seconds = reader.readInt32();
            lirq.get(i).time.attoseconds = reader.readInt64();
        }
        for (i = n; i < 16; i++) {
            reader.readInt32();
            reader.readInt32();
            reader.readInt32();
            reader.readInt32();
            reader.readInt32();
            reader.readInt64();
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                interrupt_vector[i][j] = reader.readInt32();
            }
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                input_line_state[i][j] = reader.readByte();
            }
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                input_line_vector[i][j] = reader.readInt32();
            }
        }
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 35; j++) {
                input_event_index[i][j] = reader.readInt32();
            }
        }
    }
}
