/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m68000;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Cpuexec.cpuexec_data;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Timer;


public class MC68000 extends cpuexec_data {

    public static MC68000 m1;
    // Machine State
    public final Register[] D = new Register[8];
    public final Register[] A = new Register[8];
    public int PC, PPC;
    private long totalExecutedCycles;
    private int pendingCycles;

    {
        for (int i = 0; i < 8; i++) {
            A[i] = new Register();
            D[i] = new Register();
        }
    }

    @Override
    public long getTotalExecutedCycles() {
        return totalExecutedCycles;
    }

    @Override
    public void setTotalExecutedCycles(long value) {
        totalExecutedCycles = value;
    }

    @Override
    public int getPendingCycles() {
        return pendingCycles;
    }

    @Override
    public void setPendingCycles(int value) {
        pendingCycles = value;
    }

    // Status Registers
    public int int_cycles;
    public int InterruptMaskLevel;
    boolean s, m;
    public int usp, ssp;
    public boolean stopped;

    /** Machine/Interrupt mode */
    public boolean getM() {
        return m;
    }

    public void setM(boolean value) {
        m = value;
    } // TODO probably have some switch logic maybe

    public void SetS(boolean b1) {
        s = b1;
    }

    public void m68k_set_irq(int interrupt) {
        this.interrupt = interrupt;
        m68ki_check_interrupts();
    }

    /** Supervisor/User mode */
    public boolean getS() {
        return s;
    }

    public void setS(boolean value) {
        if (value == s)
            return;
        if (value) { // entering supervisor mode
            //logger.log(Level.TRACE, "&^&^&^&^& ENTER SUPERVISOR MODE");
            usp = A[7].s32;
            A[7].s32 = ssp;
            s = true;
        } else { // exiting supervisor mode
            //logger.log(Level.TRACE, "&^&^&^&^& LEAVE SUPERVISOR MODE");
            ssp = A[7].s32;
            A[7].s32 = usp;
            s = false;
        }
    }

    /** Extend Flag */
    public boolean X;
    /** Negative Flag */
    public boolean N;
    /** Zero Flag */
    public boolean Z;
    /** Overflow Flag */
    public boolean V;
    /** Carry Flag */
    public boolean C;

    /** Status Register */
    private short SR;

    public short getSR() {
        short value = 0;
        if (C) value |= 0x0001;
        if (V) value |= 0x0002;
        if (Z) value |= 0x0004;
        if (N) value |= 0x0008;
        if (X) value |= 0x0010;
        if (m) value |= 0x1000;
        if (s) value |= 0x2000;
        value |= (short) ((InterruptMaskLevel & 7) << 8);
        return value;
    }

    public void setSR(short value) {
        C = (value & 0x0001) != 0;
        V = (value & 0x0002) != 0;
        Z = (value & 0x0004) != 0;
        N = (value & 0x0008) != 0;
        X = (value & 0x0010) != 0;
        m = (value & 0x1000) != 0;
        s = (value & 0x2000) != 0;
        InterruptMaskLevel = (value >> 8) & 7;
    }

    private short CCR;

    public short getCCR() {
        short value = 0;
        if (C) value |= 0x0001;
        if (V) value |= 0x0002;
        if (Z) value |= 0x0004;
        if (N) value |= 0x0008;
        if (X) value |= 0x0010;
        return value;
    }

    public void setCCR(short value) {
        C = (value & 0x0001) != 0;
        V = (value & 0x0002) != 0;
        Z = (value & 0x0004) != 0;
        N = (value & 0x0008) != 0;
        X = (value & 0x0010) != 0;
    }

    private int interrupt;

    public int getInterrupt() {
        return interrupt;
    }

    public void setInterrupt(int value) {
        interrupt = value;
    }

    // Memory Access
    public Function<Integer, Byte> ReadOpByte, ReadByte;
    public Function<Integer, Short> ReadOpWord, ReadPcrelWord, ReadWord;
    public Function<Integer, Integer> ReadOpLong, ReadPcrelLong, ReadLong;

    public BiConsumer<Integer, Byte> WriteByte;
    public BiConsumer<Integer, Short> WriteWord;
    public BiConsumer<Integer, Integer> WriteLong;

    public interface debug_delegate extends Runnable {

    }

    public debug_delegate debugger_start_cpu_hook_callback, debugger_stop_cpu_hook_callback;

    // Initialization

    public MC68000() {
        BuildOpcodeTable();
    }

    @Override
    public void Reset() {
        Pulse_Reset();
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        if (irqline == LineState.INPUT_LINE_NMI.ordinal())
            irqline = 7;
        switch (state) {
            case CLEAR_LINE:
                m68k_set_irq(0);
                break;
            case ASSERT_LINE:
                m68k_set_irq(irqline);
                break;
            default:
                m68k_set_irq(irqline);
                break;
        }
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
    }

    public void Pulse_Reset() {
        stopped = false;
        pendingCycles = 0;
        s = true;
        m = false;
        InterruptMaskLevel = 7;
        interrupt = 0;
        A[7].s32 = ReadOpLong.apply(0);
        PC = ReadOpLong.apply(4);
    }

    public final Runnable[] Opcodes = new Runnable[0x1_0000];
    public short op;

    public void Step() {
        //logger.log(Level.TRACE, Disassemble(PC));

        op = ReadOpWord.apply(PC);
        PC += 2;
        Opcodes[op].run();
    }

    @Override
    public int ExecuteCycles(int cycles) {
        if (!stopped) {
            pendingCycles = cycles;
            int ran;
            pendingCycles -= int_cycles;
            int_cycles = 0;
            do {
                int prevCycles = pendingCycles;
                PPC = PC;
                debugger_start_cpu_hook_callback.run();
                op = ReadOpWord.apply(PC);
                PC += 2;
                //Log.Note("CPU", State());
                if (Opcodes[op] == null) {
                    //throw new Exception(string.Format("unhandled opcode at pc=%6X", PC));
                }
                //if (Opcodes[op] == ASLd0 || Opcodes[op] == ASRd0 || Opcodes[op] == LSLd0 || Opcodes[op] == LSRd0 || Opcodes[op] == ROXLd0 || Opcodes[op] == ROXRd0 || Opcodes[op] == ROLd0 || Opcodes[op] == RORd0 || Opcodes[op] == NBCD || Opcodes[op] == ILLEGAL || Opcodes[op] == STOP || Opcodes[op] == TRAPV || Opcodes[op] == CHK || Opcodes[op] == NEGX || Opcodes[op] == SBCD0 || Opcodes[op] == SBCD1 || Opcodes[op] == ABCD0 || Opcodes[op] == ABCD1 || Opcodes[op] == EXGdd || Opcodes[op] == EXGaa || Opcodes[op] == EXGda || Opcodes[op] == TAS || Opcodes[op] == MOVEP || Opcodes[op] == ADDX0 || Opcodes[op] == ADDX1 || Opcodes[op] == SUBX0 || Opcodes[op] == SUBX1)
                Opcodes[op].run();
                m68ki_check_interrupts();
                debugger_stop_cpu_hook_callback.run();
                int delta = prevCycles - pendingCycles;
                totalExecutedCycles += delta;
            }
            while (pendingCycles > 0);
            pendingCycles -= int_cycles;
//            totalExecutedCycles += (long) int_cycles;
            int_cycles = 0;
            ran = cycles - pendingCycles;
            return ran;
        }
        pendingCycles = 0;
        int_cycles = 0;
        return cycles;
    }

    public void m68ki_check_interrupts() {
        if (interrupt > 0 && (interrupt > InterruptMaskLevel || interrupt > 7)) {
            stopped = false;
            short sr = SR;  // capture current SR.
            s = true;  // switch to supervisor mode, if not already in it.
            A[7].s32 -= 4;  // Push PC on stack
            WriteLong.accept(A[7].s32, PC);
            A[7].s32 -= 2;  // Push SR on stack
            WriteWord.accept(A[7].s32, sr);
            PC = ReadLong.apply((24 + interrupt) * 4); // Jump to interrupt vector
            InterruptMaskLevel = interrupt;  // Set interrupt mask to level currently being entered
            interrupt = 0;  // "ack" interrupt. Note: this is wrong.
            int_cycles += 0x2c;
        }
    }

    public String State() {
        String a = "%-64s".formatted(Disassemble(PC));
//        String a = "%-62s".formatted("%6X: %4X".formatted(PC, ReadWord.apply(PC)));
        String b = "D0:%8X D1:%8X D2:%8X D3:%8X D4:%8X D5:%8X D6:%8X D7:%8X ".formatted(D[0].u32, D[1].u32, D[2].u32, D[3].u32, D[4].u32, D[5].u32, D[6].u32, D[7].u32);
        String c = "A0:%8X A1:%8X A2:%8X A3:%8X A4:%8X A5:%8X A6:%8X A7:%8X ".formatted(A[0].u32, A[1].u32, A[2].u32, A[3].u32, A[4].u32, A[5].u32, A[6].u32, A[7].u32);
        String d = "SR:%4X Pending %d".formatted(SR, pendingCycles);
        return a + b + c + d;
    }

    public void SaveStateBinary(BinaryWriter writer) {
        int i;
        for (i = 0; i < 0x08; i++) {
            writer.write(MC68000.m1.D[i].u32);
        }
        for (i = 0; i < 0x08; i++) {
            writer.write(MC68000.m1.A[i].u32);
        }
        writer.write(MC68000.m1.PPC);
        writer.write(MC68000.m1.PC);
        writer.write(MC68000.m1.s);
        writer.write(MC68000.m1.m);
        writer.write(MC68000.m1.X);
        writer.write(MC68000.m1.N);
        writer.write(MC68000.m1.Z);
        writer.write(MC68000.m1.V);
        writer.write(MC68000.m1.C);
        writer.write(MC68000.m1.InterruptMaskLevel);
        writer.write(MC68000.m1.interrupt);
        writer.write(MC68000.m1.int_cycles);
        writer.write(MC68000.m1.usp);
        writer.write(MC68000.m1.ssp);
        writer.write(MC68000.m1.stopped);
        writer.write(MC68000.m1.totalExecutedCycles);
        writer.write(MC68000.m1.pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        int i;
        for (i = 0; i < 0x08; i++) {
            MC68000.m1.D[i].u32 = reader.readUInt32();
        }
        for (i = 0; i < 0x08; i++) {
            MC68000.m1.A[i].u32 = reader.readUInt32();
        }
        MC68000.m1.PPC = reader.readInt32();
        MC68000.m1.PC = reader.readInt32();
        MC68000.m1.SetS(reader.readBoolean());
        MC68000.m1.m = reader.readBoolean();
        MC68000.m1.X = reader.readBoolean();
        MC68000.m1.N = reader.readBoolean();
        MC68000.m1.Z = reader.readBoolean();
        MC68000.m1.V = reader.readBoolean();
        MC68000.m1.C = reader.readBoolean();
        MC68000.m1.InterruptMaskLevel = reader.readInt32();
        MC68000.m1.interrupt = reader.readInt32();
        MC68000.m1.int_cycles = reader.readInt32();
        MC68000.m1.usp = reader.readInt32();
        MC68000.m1.ssp = reader.readInt32();
        MC68000.m1.stopped = reader.readBoolean();
        MC68000.m1.totalExecutedCycles = reader.readUInt64();
        MC68000.m1.pendingCycles = reader.readInt32();
    }

    public void SaveStateText(PrintWriter writer, String id) {
        writer.printf("[%s]%n", id);
        writer.printf("D0 %8X%n", D[0].s32);
        writer.printf("D1 %8X%n", D[1].s32);
        writer.printf("D2 %8X%n", D[2].s32);
        writer.printf("D3 %8X%n", D[3].s32);
        writer.printf("D4 %8X%n", D[4].s32);
        writer.printf("D5 %8X%n", D[5].s32);
        writer.printf("D6 %8X%n", D[6].s32);
        writer.printf("D7 %8X%n", D[7].s32);
        writer.println();

        writer.printf("A0 %8X%n", A[0].s32);
        writer.printf("A1 %8X%n", A[1].s32);
        writer.printf("A2 %8X%n", A[2].s32);
        writer.printf("A3 %8X%n", A[3].s32);
        writer.printf("A4 %8X%n", A[4].s32);
        writer.printf("A5 %8X%n", A[5].s32);
        writer.printf("A6 %8X%n", A[6].s32);
        writer.printf("A7 %8X%n", A[7].s32);
        writer.println();

        writer.printf("PC %6X%n", PC);
        writer.printf("InterruptMaskLevel %d%n", InterruptMaskLevel);
        writer.printf("USP %8X%n", usp);
        writer.printf("SSP %8X%n", ssp);
        writer.printf("S %s%n", s);
        writer.printf("M %s%n", m);
        writer.println();

        writer.printf("TotalExecutedCycles %d%n", totalExecutedCycles);
        writer.printf("PendingCycles %d%n", pendingCycles);

        writer.printf("[/%s]%n", id);
    }

    public void LoadStateText(Scanner reader, String id) {
        while (reader.hasNextLine()) {
            String[] args = reader.nextLine().split(" ");
            if (args[0].trim().isEmpty()) continue;
            if (args[0].equals("[/" + id + "]")) break;
            else if (args[0].equals("D0")) D[0].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D1")) D[1].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D2")) D[2].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D3")) D[3].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D4")) D[4].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D5")) D[5].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D6")) D[6].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("D7")) D[7].s32 = Integer.parseInt(args[1], 16);

            else if (args[0].equals("A0")) A[0].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A1")) A[1].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A2")) A[2].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A3")) A[3].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A4")) A[4].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A5")) A[5].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A6")) A[6].s32 = Integer.parseInt(args[1], 16);
            else if (args[0].equals("A7")) A[7].s32 = Integer.parseInt(args[1], 16);

            else if (args[0].equals("PC")) PC = Integer.parseInt(args[1], 16);
            else if (args[0].equals("InterruptMaskLevel")) InterruptMaskLevel = Integer.parseInt(args[1]);
            else if (args[0].equals("USP")) usp = Integer.parseInt(args[1], 16);
            else if (args[0].equals("SSP")) ssp = Integer.parseInt(args[1], 16);
            else if (args[0].equals("S")) s = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("M")) m = Boolean.parseBoolean(args[1]);

            else if (args[0].equals("TotalExecutedCycles")) totalExecutedCycles = Long.parseLong(args[1]);
            else if (args[0].equals("PendingCycles")) pendingCycles = Integer.parseInt(args[1]);

            else {
                //logger.log(Level.TRACE, "Skipping unrecognized identifier " + args[0]);
            }
        }
    }

    //@StructLayout(LayoutKind.Explicit)
    public static class Register {

        //@FieldOffset(0)
        public int u32;
        //@FieldOffset(0)
        public int s32;

        //@FieldOffset(0)
        public short u16;
        //@FieldOffset(0)
        public short s16;

        //@FieldOffset(0)
        public byte u8;
        //@FieldOffset(0)
        public byte s8;

        @Override
        public String toString() {
            return "%8x".formatted(u32);
        }
    }

//#region BitArithemetic

    void AND0() { // AND <ea>, Dn
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcMode = (op >> 3) & 0x07;
        int srcReg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: // Byte
                D[dstReg].s8 &= ReadValueB(srcMode, srcReg);
                pendingCycles -= (srcMode == 0) ? 4 : 4 + EACyclesBW[srcMode][srcReg];
                N = (D[dstReg].s8 & 0x80) != 0;
                Z = (D[dstReg].s8 == 0);
                break;
            case 1: // Word
                D[dstReg].s16 &= ReadValueW(srcMode, srcReg);
                pendingCycles -= (srcMode == 0) ? 4 : 4 + EACyclesBW[srcMode][srcReg];
                N = (D[dstReg].s16 & 0x8000) != 0;
                Z = (D[dstReg].s16 == 0);
                break;
            case 2: // Long
                D[dstReg].s32 &= ReadValueL(srcMode, srcReg);
                if (srcMode == 0 || (srcMode == 7 && srcReg == 4)) {
                    pendingCycles -= 8 + EACyclesL[srcMode][srcReg];
                } else {
                    pendingCycles -= 6 + EACyclesL[srcMode][srcReg];
                }
                N = (D[dstReg].s32 & 0x8000_0000) != 0;
                Z = (D[dstReg].s32 == 0);
                break;
        }
    }

    void AND0_Disasm(DisassemblyInfo info) {
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcMode = (op >> 3) & 0x07;
        int srcReg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "and.b";
                info.Args = "%s, D%d".formatted(DisassembleValue(srcMode, srcReg, 1, /* ref */ pc), dstReg);
                break;
            case 1: // Word
                info.Mnemonic = "and.w";
                info.Args = "%s, D%d".formatted(DisassembleValue(srcMode, srcReg, 2, /* ref */ pc), dstReg);
                break;
            case 2: // Long
                info.Mnemonic = "and.l";
                info.Args = "%s, D%d".formatted(DisassembleValue(srcMode, srcReg, 4, /* ref */ pc), dstReg);
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void AND1() { // AND Dn, <ea>
        int srcReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: // Byte
            {
                byte dest = PeekValueB(dstMode, dstReg);
                byte value = (byte) (dest & D[srcReg].s8);
                WriteValueB(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 4 : 8 + EACyclesBW[dstMode][dstReg];
                N = (value & 0x80) != 0;
                Z = (value == 0);
                break;
            }
            case 1: // Word
            {
                short dest = PeekValueW(dstMode, dstReg);
                short value = (short) (dest & D[srcReg].s16);
                WriteValueW(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 4 : 8 + EACyclesBW[dstMode][dstReg];
                N = (value & 0x8000) != 0;
                Z = (value == 0);
                break;
            }
            case 2: // Long
            {
                int dest = PeekValueL(dstMode, dstReg);
                int value = dest & D[srcReg].s32;
                WriteValueL(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 8 : 12 + EACyclesL[dstMode][dstReg];
                N = (value & 0x8000_0000) != 0;
                Z = (value == 0);
                break;
            }
        }
    }

    void AND1_Disasm(DisassemblyInfo info) {
        int srcReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "and.b";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 1, /* ref */ pc));
                break;
            case 1: // Word
                info.Mnemonic = "and.w";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 2, /* ref */ pc));
                break;
            case 2: // Long
                info.Mnemonic = "and.l";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 4, /* ref */ pc));
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void ANDI() { // ANDI #<data>, <ea>
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: { // Byte
                byte imm = (byte) (short) ReadOpWord.apply(PC);
                PC += 2;
                byte arg = PeekValueB(dstMode, dstReg);
                byte result = (byte) (imm & arg);
                WriteValueB(dstMode, dstReg, result);
                pendingCycles -= (dstMode == 0) ? 8 : 12 + EACyclesBW[dstMode][dstReg];
                N = (result & 0x80) != 0;
                Z = (result == 0);
                break;
            }
            case 1: { // Word
                short imm = ReadOpWord.apply(PC);
                PC += 2;
                short arg = PeekValueW(dstMode, dstReg);
                short result = (short) (imm & arg);
                WriteValueW(dstMode, dstReg, result);
                pendingCycles -= (dstMode == 0) ? 8 : 12 + EACyclesBW[dstMode][dstReg];
                N = (result & 0x8000) != 0;
                Z = (result == 0);
                break;
            }
            case 2: { // Long
                int imm = ReadOpLong.apply(PC);
                PC += 4;
                int arg = PeekValueL(dstMode, dstReg);
                int result = imm & arg;
                WriteValueL(dstMode, dstReg, result);
                pendingCycles -= (dstMode == 0) ? 14 : 20 + EACyclesL[dstMode][dstReg];
                N = (result & 0x8000_0000) != 0;
                Z = (result == 0);
                break;
            }
        }
    }

    void ANDI_Disasm(DisassemblyInfo info) {
        int size = ((op >> 6) & 0x03);
        int dstMode = ((op >> 3) & 0x07);
        int dstReg = (op & 0x07);

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: { // Byte
                info.Mnemonic = "andi.b";
                byte imm = (byte) (short) ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Args = "$%X, ".formatted(imm);
                info.Args += DisassembleValue(dstMode, dstReg, 1, /* ref */ pc);
                break;
            }
            case 1: { // Word
                info.Mnemonic = "andi.w";
                short imm = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Args = "$%X, ".formatted(imm);
                info.Args += DisassembleValue(dstMode, dstReg, 2, /* ref */ pc);
                break;
            }
            case 2: { // Long
                info.Mnemonic = "andi.l";
                int imm = ReadOpLong.apply(pc[0]);
                pc[0] += 4;
                info.Args = "$%X, ".formatted(imm);
                info.Args += DisassembleValue(dstMode, dstReg, 4, /* ref */ pc);
                break;
            }
        }

        info.Length = pc[0] - info.PC;
    }

    void ANDI_CCR() { // m68k_op_andi_16_toc         , 0xffff, 0x023c, { 20}
        short value;
        value = ReadOpWord.apply(PC);
        PC += 2;
        CCR = (short) (CCR & value);
        pendingCycles -= 20;
    }

    void ANDI_CCR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "andi";
        info.Args = DisassembleImmediate(1, /* ref */ pc) + ", CCR";
        info.Length = pc[0] - info.PC;
    }

    void EOR() { // EOR Dn, <ea>
        int srcReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: // Byte
            {
                byte dest = PeekValueB(dstMode, dstReg);
                byte value = (byte) (dest ^ D[srcReg].s8);
                WriteValueB(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 4 : 8 + EACyclesBW[dstMode][dstReg];
                N = (value & 0x80) != 0;
                Z = (value == 0);
                return;
            }
            case 1: // Word
            {
                short dest = PeekValueW(dstMode, dstReg);
                short value = (short) (dest ^ D[srcReg].s16);
                WriteValueW(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 4 : 8 + EACyclesBW[dstMode][dstReg];
                N = (value & 0x8000) != 0;
                Z = (value == 0);
                return;
            }
            case 2: // Long
            {
                int dest = PeekValueL(dstMode, dstReg);
                int value = dest ^ D[srcReg].s32;
                WriteValueL(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 8 : 12 + EACyclesL[dstMode][dstReg];
                N = (value & 0x8000_0000) != 0;
                Z = (value == 0);
            }
        }
    }

    void EOR_Disasm(DisassemblyInfo info) {
        int srcReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "eor.b";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 1, /* ref */ pc));
                break;
            case 1: // Word
                info.Mnemonic = "eor.w";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 2, /* ref */ pc));
                break;
            case 2: // Long
                info.Mnemonic = "eor.l";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 4, /* ref */ pc));
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void EORI() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
            {
                byte immed = (byte) (short) ReadOpWord.apply(PC);
                PC += 2;
                byte value = (byte) (PeekValueB(mode, reg) ^ immed);
                WriteValueB(mode, reg, value);
                N = (value & 0x80) != 0;
                Z = value == 0;
                pendingCycles -= mode == 0 ? 8 : 12 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short immed = ReadOpWord.apply(PC);
                PC += 2;
                short value = (short) (PeekValueW(mode, reg) ^ immed);
                WriteValueW(mode, reg, value);
                N = (value & 0x8000) != 0;
                Z = value == 0;
                pendingCycles -= mode == 0 ? 8 : 12 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int immed = ReadOpLong.apply(PC);
                PC += 4;
                int value = PeekValueL(mode, reg) ^ immed;
                WriteValueL(mode, reg, value);
                N = (value & 0x8000_0000) != 0;
                Z = value == 0;
                pendingCycles -= mode == 0 ? 16 : 20 + EACyclesL[mode][reg];
            }
        }
    }

    void EORI_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                info.Mnemonic = "eori.b";
                byte immed = (byte) (short) ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Args = "$%X, %s".formatted(immed, DisassembleValue(mode, reg, 1, /* ref */ pc));
                break;
            }
            case 1: // word
            {
                info.Mnemonic = "eori.w";
                short immed = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Args = "$%X, %s".formatted(immed, DisassembleValue(mode, reg, 2, /* ref */ pc));
                break;
            }
            case 2: // long
            {
                info.Mnemonic = "eori.l";
                int immed = ReadOpLong.apply(pc[0]);
                pc[0] += 4;
                info.Args = "$%X, %s".formatted(immed, DisassembleValue(mode, reg, 4, /* ref */ pc));
                break;
            }
        }

        info.Length = pc[0] - info.PC;
    }

    void EORI_CCR() { //m68k_op_eori_16_toc         , 0xffff, 0x0a3c, { 20}
//        m68ki_set_ccr(m68ki_get_ccr() ^ m68ki_read_imm_16());
        short value;
        value = ReadOpWord.apply(PC);
        PC += 2;
        CCR = (short) (CCR ^ value);
        pendingCycles -= 20;
    }

    void EORI_CCR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "eori";
        info.Args = DisassembleImmediate(1, /* ref */ pc) + ", CCR";
        info.Length = pc[0] - info.PC;
    }

    void OR0() { // OR <ea>, Dn
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcMode = (op >> 3) & 0x07;
        int srcReg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: // Byte
                D[dstReg].s8 |= ReadValueB(srcMode, srcReg);
                pendingCycles -= (srcMode == 0) ? 4 : 4 + EACyclesBW[srcMode][srcReg];
                N = (D[dstReg].s8 & 0x80) != 0;
                Z = (D[dstReg].s8 == 0);
                return;
            case 1: // Word
                D[dstReg].s16 |= ReadValueW(srcMode, srcReg);
                pendingCycles -= (srcMode == 0) ? 4 : 4 + EACyclesBW[srcMode][srcReg];
                N = (D[dstReg].s16 & 0x8000) != 0;
                Z = (D[dstReg].s16 == 0);
                return;
            case 2: // Long
                D[dstReg].s32 |= ReadValueL(srcMode, srcReg);
                if (srcMode == 0 || (srcMode == 7 && srcReg == 4)) {
                    pendingCycles -= 8 + EACyclesL[srcMode][srcReg];
                } else {
                    pendingCycles -= 6 + EACyclesL[srcMode][srcReg];
                }
                N = (D[dstReg].s32 & 0x8000_0000) != 0;
                Z = (D[dstReg].s32 == 0);
        }
    }

    void OR0_Disasm(DisassemblyInfo info) {
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcMode = (op >> 3) & 0x07;
        int srcReg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "or.b";
                info.Args = "%s, D%d".formatted(DisassembleValue(srcMode, srcReg, 1, /* ref */ pc), dstReg);
                break;
            case 1: // Word
                info.Mnemonic = "or.w";
                info.Args = "%s, D%d".formatted(DisassembleValue(srcMode, srcReg, 2, /* ref */ pc), dstReg);
                break;
            case 2: // Long
                info.Mnemonic = "or.l";
                info.Args = "%s, D%d".formatted(DisassembleValue(srcMode, srcReg, 4, /* ref */ pc), dstReg);
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void OR1() // OR Dn, <ea>
    {
        int srcReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: // Byte
            {
                byte dest = PeekValueB(dstMode, dstReg);
                byte value = (byte) (dest | D[srcReg].s8);
                WriteValueB(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 4 : 8 + EACyclesBW[dstMode][dstReg];
                N = (value & 0x80) != 0;
                Z = (value == 0);
                return;
            }
            case 1: // Word
            {
                short dest = PeekValueW(dstMode, dstReg);
                short value = (short) (dest | D[srcReg].s16);
                WriteValueW(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 4 : 8 + EACyclesBW[dstMode][dstReg];
                N = (value & 0x8000) != 0;
                Z = (value == 0);
                return;
            }
            case 2: // Long
            {
                int dest = PeekValueL(dstMode, dstReg);
                int value = dest | D[srcReg].s32;
                WriteValueL(dstMode, dstReg, value);
                pendingCycles -= (dstMode == 0) ? 8 : 12 + EACyclesL[dstMode][dstReg];
                N = (value & 0x8000_0000) != 0;
                Z = (value == 0);
            }
        }
    }

    void OR1_Disasm(DisassemblyInfo info) {
        int srcReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int dstMode = (op >> 3) & 0x07;
        int dstReg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "or.b";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 1, /* ref */ pc));
                break;
            case 1: // Word
                info.Mnemonic = "or.w";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 2, /* ref */ pc));
                break;
            case 2: // Long
                info.Mnemonic = "or.l";
                info.Args = "D%d, %s".formatted(srcReg, DisassembleValue(dstMode, dstReg, 4, /* ref */ pc));
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void ORI() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
            {
                byte immed = (byte) (short) ReadOpWord.apply(PC);
                PC += 2;
                byte value = (byte) (PeekValueB(mode, reg) | immed);
                WriteValueB(mode, reg, value);
                N = (value & 0x80) != 0;
                Z = value == 0;
                pendingCycles -= mode == 0 ? 8 : 12 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short immed = ReadOpWord.apply(PC);
                PC += 2;
                short value = (short) (PeekValueW(mode, reg) | immed);
                WriteValueW(mode, reg, value);
                N = (value & 0x8000) != 0;
                Z = value == 0;
                pendingCycles -= mode == 0 ? 8 : 12 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int immed = ReadOpLong.apply(PC);
                PC += 4;
                int value = PeekValueL(mode, reg) | immed;
                WriteValueL(mode, reg, value);
                N = (value & 0x8000_0000) != 0;
                Z = value == 0;
                pendingCycles -= mode == 0 ? 16 : 20 + EACyclesL[mode][reg];
            }
        }
    }

    void ORI_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                info.Mnemonic = "ori.b";
                byte immed = (byte) (short) ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Args = "$%X, %s".formatted(immed, DisassembleValue(mode, reg, 1, /* ref */ pc));
                break;
            }
            case 1: // word
            {
                info.Mnemonic = "ori.w";
                short immed = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Args = "$%X, %s".formatted(immed, DisassembleValue(mode, reg, 2, /* ref */ pc));
                break;
            }
            case 2: // long
            {
                info.Mnemonic = "ori.l";
                int immed = ReadOpLong.apply(pc[0]);
                pc[0] += 4;
                info.Args = "$%X, %s".formatted(immed, DisassembleValue(mode, reg, 4, /* ref */ pc));
                break;
            }
        }

        info.Length = pc[0] - info.PC;
    }

    void ORI_CCR() { //m68k_op_ori_16_toc          , 0xffff, 0x003c, { 20}
        short value;
        value = ReadOpWord.apply(PC);
        PC += 2;
        CCR = (short) (CCR | value);
        pendingCycles -= 20;
    }

    void ORI_CCR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "ori";
        info.Args = DisassembleImmediate(1, /* ref */ pc) + ", CCR";
        info.Length = pc[0] - info.PC;
    }

    void NOT() {
        int size = (op >> 6) & 0x03;
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;

        V = false;
        C = false;

        switch (size) {
            case 0: // Byte
            {
                byte value = PeekValueB(mode, reg);
                value = (byte) ~value;
                WriteValueB(mode, reg, value);
                pendingCycles -= (mode == 0) ? 4 : 8 + EACyclesBW[mode][reg];
                N = (value & 0x80) != 0;
                Z = (value == 0);
                return;
            }
            case 1: // Word
            {
                short value = PeekValueW(mode, reg);
                value = (short) ~value;
                WriteValueW(mode, reg, value);
                pendingCycles -= (mode == 0) ? 4 : 8 + EACyclesBW[mode][reg];
                N = (value & 0x8000) != 0;
                Z = (value == 0);
                return;
            }
            case 2: // Long
            {
                int value = PeekValueL(mode, reg);
                value = ~value;
                WriteValueL(mode, reg, value);
                pendingCycles -= (mode == 0) ? 6 : 12 + EACyclesL[mode][reg]; // 8:12
                N = (value & 0x8000_0000) != 0;
                Z = (value == 0);
            }
        }
    }

    void NOT_Disasm(DisassemblyInfo info) {
        int size = (op >> 6) & 0x03;
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "not.b";
                info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1: // Word
                info.Mnemonic = "not.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2: // Long
                info.Mnemonic = "not.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void LSLd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    C = X = (D[reg].u8 & 0x80) != 0;
                    D[reg].u8 <<= 1;
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].u8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    C = X = (D[reg].u16 & 0x8000) != 0;
                    D[reg].u16 <<= 1;
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].u16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    C = X = (D[reg].u32 & 0x8000_0000) != 0;
                    D[reg].u32 <<= 1;
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].u32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void LSLd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "lsl.b";
                break;
            case 1:
                info.Mnemonic = "lsl.w";
                break;
            case 2:
                info.Mnemonic = "lsl.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void LSLd0() {
        //m68k_op_lsl_16_ai           , 0xfff8, 0xe3d0, { 12}
        //m68k_op_lsl_16_pi           , 0xfff8, 0xe3d8, { 12}
        //m68k_op_lsl_16_pd           , 0xfff8, 0xe3e0, { 14}
        //m68k_op_lsl_16_di           , 0xfff8, 0xe3e8, { 16}
        //m68k_op_lsl_16_ix           , 0xfff8, 0xe3f0, { 18}
        //m68k_op_lsl_16_aw           , 0xffff, 0xe3f8, { 16}
        //m68k_op_lsl_16_al           , 0xffff, 0xe3f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        int src;
        short res;
        src = PeekValueW(mode, reg);
        res = (short) (src << 1);
        WriteValueW(mode, reg, res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        X = C = ((res & 0x8000) != 0);
        V = false;
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void LSLd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "lsl";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void LSRd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    C = X = (D[reg].u8 & 1) != 0;
                    D[reg].u8 >>= 1;
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].u8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    C = X = (D[reg].u16 & 1) != 0;
                    D[reg].u16 >>= 1;
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].u16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    C = X = (D[reg].u32 & 1) != 0;
                    D[reg].u32 >>= 1;
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].u32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void LSRd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "lsr.b";
                break;
            case 1:
                info.Mnemonic = "lsr.w";
                break;
            case 2:
                info.Mnemonic = "lsr.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void LSRd0() {
        //m68k_op_lsr_16_ai           , 0xfff8, 0xe2d0, { 12}
        //m68k_op_lsr_16_pi           , 0xfff8, 0xe2d8, { 12}
        //m68k_op_lsr_16_pd           , 0xfff8, 0xe2e0, { 14}
        //m68k_op_lsr_16_di           , 0xfff8, 0xe2e8, { 16}
        //m68k_op_lsr_16_ix           , 0xfff8, 0xe2f0, { 18}
        //m68k_op_lsr_16_aw           , 0xffff, 0xe2f8, { 16}
        //m68k_op_lsr_16_al           , 0xffff, 0xe2f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        int src;
        short res;
        src = PeekValueW(mode, reg);
        res = (short) (src >> 1);
        WriteValueW(mode, reg, res);
        N = false;
        Z = (res == 0);
        //C = X = ((src & 0x1_0000) != 0);
        C = X = ((src & 1) != 0);
        V = false;
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void LSRd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "lsr";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ASLd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    boolean msb = D[reg].s8 < 0;
                    C = X = (D[reg].u8 & 0x80) != 0;
                    D[reg].s8 <<= 1;
                    V |= (D[reg].s8 < 0) != msb;
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].u8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    boolean msb = D[reg].s16 < 0;
                    C = X = (D[reg].u16 & 0x8000) != 0;
                    D[reg].s16 <<= 1;
                    V |= (D[reg].s16 < 0) != msb;
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].u16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    boolean msb = D[reg].s32 < 0;
                    C = X = (D[reg].u32 & 0x8000_0000) != 0;
                    D[reg].s32 <<= 1;
                    V |= (D[reg].s32 < 0) != msb;
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].u32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void ASLd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "asl.b";
                break;
            case 1:
                info.Mnemonic = "asl.w";
                break;
            case 2:
                info.Mnemonic = "asl.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void ASLd0() {
        //m68k_op_asl_16_ai           , 0xfff8, 0xe1d0, { 12}
        //m68k_op_asl_16_pi           , 0xfff8, 0xe1d8, { 12}
        //m68k_op_asl_16_pd           , 0xfff8, 0xe1e0, { 14}
        //m68k_op_asl_16_di           , 0xfff8, 0xe1e8, { 16}
        //m68k_op_asl_16_ix           , 0xfff8, 0xe1f0, { 18}
        //m68k_op_asl_16_aw           , 0xffff, 0xe1f8, { 16}
        //m68k_op_asl_16_al           , 0xffff, 0xe1f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        int src;
        short res;
        src = PeekValueW(mode, reg);
        res = (short) (src << 1);
        WriteValueW(mode, reg, res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        X = C = ((src & 0x8000) != 0);
        src &= 0xc000;
        V = !(src == 0 || src == 0xc000);
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void ASLd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "asl";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ASRd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    boolean msb = D[reg].s8 < 0;
                    C = X = (D[reg].u8 & 1) != 0;
                    D[reg].s8 >>= 1;
                    V |= (D[reg].s8 < 0) != msb;
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].u8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    boolean msb = D[reg].s16 < 0;
                    C = X = (D[reg].u16 & 1) != 0;
                    D[reg].s16 >>= 1;
                    V |= (D[reg].s16 < 0) != msb;
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].u16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    boolean msb = D[reg].s32 < 0;
                    C = X = (D[reg].u32 & 1) != 0;
                    D[reg].s32 >>= 1;
                    V |= (D[reg].s32 < 0) != msb;
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].u32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void ASRd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "asr.b";
                break;
            case 1:
                info.Mnemonic = "asr.w";
                break;
            case 2:
                info.Mnemonic = "asr.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void ASRd0() {
        // m68k_op_asr_16_ai           , 0xfff8, 0xe0d0, { 12}
        // m68k_op_asr_16_pi           , 0xfff8, 0xe0d8, { 12}
        // m68k_op_asr_16_pd           , 0xfff8, 0xe0e0, { 14}
        // m68k_op_asr_16_di           , 0xfff8, 0xe0e8, { 16}
        // m68k_op_asr_16_ix           , 0xfff8, 0xe0f0, { 18}
        // m68k_op_asr_16_aw           , 0xffff, 0xe0f8, { 16}
        // m68k_op_asr_16_al           , 0xffff, 0xe0f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        int src;
        short res;
        src = PeekValueW(mode, reg);
        res = (short) (src >> 1);
        if ((src & 0x8000) != 0) {
            res |= (short) 0x8000;
        }
        WriteValueW(mode, reg, res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        V = false;
        C = X = ((src & 0x01) != 0);
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void ASRd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "asr";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ROLd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u8 & 0x80) != 0;
                    D[reg].u8 = (byte) ((D[reg].u8 << 1) | (D[reg].u8 >> 7));
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].u8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u16 & 0x8000) != 0;
                    D[reg].u16 = (short) ((D[reg].u16 << 1) | (D[reg].u16 >> 15));
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].u16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u32 & 0x8000_0000) != 0;
                    D[reg].u32 = ((D[reg].u32 << 1) | (D[reg].u32 >> 31));
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].u32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void ROLd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "rol.b";
                break;
            case 1:
                info.Mnemonic = "rol.w";
                break;
            case 2:
                info.Mnemonic = "rol.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void ROLd0() {
        //m68k_op_rol_16_ai           , 0xfff8, 0xe7d0, { 12}
        //m68k_op_rol_16_pi           , 0xfff8, 0xe7d8, { 12}
        //m68k_op_rol_16_pd           , 0xfff8, 0xe7e0, { 14}
        //m68k_op_rol_16_di           , 0xfff8, 0xe7e8, { 16}
        //m68k_op_rol_16_ix           , 0xfff8, 0xe7f0, { 18}
        //m68k_op_rol_16_aw           , 0xffff, 0xe7f8, { 16}
        //m68k_op_rol_16_al           , 0xffff, 0xe7f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        int src;
        int res;
        src = PeekValueW(mode, reg);
        res = ((src << 1) | (src >> 15)) & 0xffff;
        WriteValueW(mode, reg, (short) res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        C = ((res & 0x8000) != 0);
        V = false;
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void ROLd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "rol";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void RORd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        V = false;
        C = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u8 & 1) != 0;
                    D[reg].u8 = (byte) ((D[reg].u8 >> 1) | (D[reg].u8 << 7));
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].u8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u16 & 1) != 0;
                    D[reg].u16 = (short) ((D[reg].u16 >> 1) | (D[reg].u16 << 15));
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].u16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u32 & 1) != 0;
                    D[reg].u32 = ((D[reg].u32 >> 1) | (D[reg].u32 << 31));
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].u32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void RORd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "ror.b";
                break;
            case 1:
                info.Mnemonic = "ror.w";
                break;
            case 2:
                info.Mnemonic = "ror.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void RORd0() {
        //m68k_op_ror_16_ai           , 0xfff8, 0xe6d0, { 12}
        //m68k_op_ror_16_pi           , 0xfff8, 0xe6d8, { 12}
        //m68k_op_ror_16_pd           , 0xfff8, 0xe6e0, { 14}
        //m68k_op_ror_16_di           , 0xfff8, 0xe6e8, { 16}
        //m68k_op_ror_16_ix           , 0xfff8, 0xe6f0, { 18}
        //m68k_op_ror_16_aw           , 0xffff, 0xe6f8, { 16}
        //m68k_op_ror_16_al           , 0xffff, 0xe6f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        int src;
        int res;
        src = PeekValueW(mode, reg);
        res = ((src >> 1) | (src << 15)) & 0xffff;
        WriteValueW(mode, reg, (short) res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        C = ((res & 0x01) != 0);
        V = false;
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void RORd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "ror";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ROXLd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        C = X;
        V = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u8 & 0x80) != 0;
                    D[reg].u8 = (byte) ((D[reg].u8 << 1) | (X ? 1 : 0));
                    X = C;
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].s8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u16 & 0x8000) != 0;
                    D[reg].u16 = (short) ((D[reg].u16 << 1) | (X ? 1 : 0));
                    X = C;
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].s16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].s32 & 0x8000_0000) != 0;
                    D[reg].s32 = ((D[reg].s32 << 1) | (X ? 1 : 0));
                    X = C;
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].s32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void ROXLd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "roxl.b";
                break;
            case 1:
                info.Mnemonic = "roxl.w";
                break;
            case 2:
                info.Mnemonic = "roxl.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void ROXLd0() {
        //m68k_op_roxl_16_ai          , 0xfff8, 0xe5d0, { 12}
        //m68k_op_roxl_16_pi          , 0xfff8, 0xe5d8, { 12}
        //m68k_op_roxl_16_pd          , 0xfff8, 0xe5e0, { 14}
        //m68k_op_roxl_16_di          , 0xfff8, 0xe5e8, { 16}
        //m68k_op_roxl_16_ix          , 0xfff8, 0xe5f0, { 18}
        //m68k_op_roxl_16_aw          , 0xffff, 0xe5f8, { 16}
        //m68k_op_roxl_16_al          , 0xffff, 0xe5f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        short src;
        int src2;
        int res;
        src = PeekValueW(mode, reg);
        src2 = src | (X ? 0x1_0000 : 0);
        res = ((src2 << 1) | (src2 >> 16));
        C = X = (((res >> 8) & 0x100) != 0);
        WriteValueW(mode, reg, (short) res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        V = false;
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void ROXLd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "roxl";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ROXRd() {
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;
        else if (m == 1) rot = D[rot].s32 & 63;

        C = X;
        V = false;

        switch (size) {
            case 0: // byte
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u8 & 1) != 0;
                    D[reg].u8 = (byte) ((D[reg].u8 >> 1) | (X ? 0x80 : 0));
                    X = C;
                }
                N = (D[reg].s8 & 0x80) != 0;
                Z = D[reg].s8 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 1: // word
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].u16 & 1) != 0;
                    D[reg].u16 = (short) ((D[reg].u16 >> 1) | (X ? 0x8000 : 0));
                    X = C;
                }
                N = (D[reg].s16 & 0x8000) != 0;
                Z = D[reg].s16 == 0;
                pendingCycles -= 6 + (rot * 2);
                return;
            case 2: // long
                for (int i = 0; i < rot; i++) {
                    C = (D[reg].s32 & 1) != 0;
                    D[reg].u32 = ((D[reg].u32 >> 1) | (X ? 0x8000_0000 : 0));
                    X = C;
                }
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = D[reg].s32 == 0;
                pendingCycles -= 8 + (rot * 2);
        }
    }

    void ROXRd_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int rot = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int m = (op >> 5) & 1;
        int reg = op & 7;

        if (m == 0 && rot == 0) rot = 8;

        switch (size) {
            case 0:
                info.Mnemonic = "roxr.b";
                break;
            case 1:
                info.Mnemonic = "roxr.w";
                break;
            case 2:
                info.Mnemonic = "roxr.l";
                break;
        }
        if (m == 0) info.Args = rot + ", D" + reg;
        else info.Args = "D" + rot + ", D" + reg;

        info.Length = pc - info.PC;
    }

    void ROXRd0() {
        // m68k_op_roxr_16_ai          , 0xfff8, 0xe4d0, { 12}
        // m68k_op_roxr_16_pi          , 0xfff8, 0xe4d8, { 12}
        // m68k_op_roxr_16_pd          , 0xfff8, 0xe4e0, { 14}
        // m68k_op_roxr_16_di          , 0xfff8, 0xe4e8, { 16}
        // m68k_op_roxr_16_ix          , 0xfff8, 0xe4f0, { 18}
        // m68k_op_roxr_16_aw          , 0xffff, 0xe4f8, { 16}
        // m68k_op_roxr_16_al          , 0xffff, 0xe4f9, { 20}
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        short src;
        int src2;
        int res;
        src = PeekValueW(mode, reg);
        src2 = ((int) src | (X ? 0x1_0000 : 0));
        res = ((src2 >> 1) | (src2 << 16));
        C = X = ((res & 0x1_0000) != 0);
        res = res & 0xffff;
        WriteValueW(mode, reg, (short) res);
        N = ((res & 0x8000) != 0);
        Z = (res == 0);
        V = false;
        pendingCycles -= 8 + EACyclesBW[mode][reg];
    }

    void ROXRd0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "roxr";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void SWAP() {
        int reg = op & 7;
        D[reg].u32 = (D[reg].u32 << 16) | (D[reg].u32 >> 16);
        V = C = false;
        Z = D[reg].u32 == 0;
        N = (D[reg].s32 & 0x8000_0000) != 0;
        pendingCycles -= 4;
    }

    void SWAP_Disasm(DisassemblyInfo info) {
        int reg = op & 7;
        info.Mnemonic = "swap";
        info.Args = "D" + reg;
    }

//#endregion

//#region DataMovement

    void MOVE() {
        int size = ((op >> 12) & 0x03);
        int dstMode = ((op >> 6) & 0x07);
        int dstReg = ((op >> 9) & 0x07);
        int srcMode = ((op >> 3) & 0x07);
        int srcReg = (op & 0x07);

        int value = 0;
        switch (size) {
            case 1: // Byte
                value = ReadValueB(srcMode, srcReg);
                WriteValueB(dstMode, dstReg, (byte) value);
                pendingCycles -= MoveCyclesBW[srcMode + (srcMode == 7 ? srcReg : 0)][dstMode + (dstMode == 7 ? dstReg : 0)];
                N = (value & 0x80) != 0;
                break;
            case 3: // Word
                value = ReadValueW(srcMode, srcReg);
                WriteValueW(dstMode, dstReg, (short) value);
                pendingCycles -= MoveCyclesBW[srcMode + (srcMode == 7 ? srcReg : 0)][dstMode + (dstMode == 7 ? dstReg : 0)];
                N = (value & 0x8000) != 0;
                break;
            case 2: // Long
                value = ReadValueL(srcMode, srcReg);
                WriteValueL(dstMode, dstReg, value);
                pendingCycles -= MoveCyclesL[srcMode + (srcMode == 7 ? srcReg : 0)][dstMode + (dstMode == 7 ? dstReg : 0)];
                N = (value & 0x8000_0000) != 0;
                break;
        }

        V = false;
        C = false;
        Z = (value == 0);
    }

    void MOVE_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = ((op >> 12) & 0x03);
        int dstMode = ((op >> 6) & 0x07);
        int dstReg = ((op >> 9) & 0x07);
        int srcMode = ((op >> 3) & 0x07);
        int srcReg = (op & 0x07);

        switch (size) {
            case 1:
                info.Mnemonic = "move.b";
                info.Args = DisassembleValue(srcMode, srcReg, 1, /* ref */ pc) + ", ";
                info.Args += DisassembleValue(dstMode, dstReg, 1, /* ref */ pc);
                break;
            case 3:
                info.Mnemonic = "move.w";
                info.Args = DisassembleValue(srcMode, srcReg, 2, /* ref */ pc) + ", ";
                info.Args += DisassembleValue(dstMode, dstReg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "move.l";
                info.Args = DisassembleValue(srcMode, srcReg, 4, /* ref */ pc) + ", ";
                info.Args += DisassembleValue(dstMode, dstReg, 4, /* ref */ pc);
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void MOVEA() {
        int size = ((op >> 12) & 0x03);
        int dstReg = ((op >> 9) & 0x07);
        int srcMode = ((op >> 3) & 0x07);
        int srcReg = (op & 0x07);

        if (size == 3) // Word
        {
            A[dstReg].s32 = ReadValueW(srcMode, srcReg);
            switch (srcMode) {
                case 0:
                    pendingCycles -= 4;
                    break;
                case 1:
                    pendingCycles -= 4;
                    break;
                case 2:
                    pendingCycles -= 8;
                    break;
                case 3:
                    pendingCycles -= 8;
                    break;
                case 4:
                    pendingCycles -= 10;
                    break;
                case 5:
                    pendingCycles -= 12;
                    break;
                case 6:
                    pendingCycles -= 14;
                    break;
                case 7:
                    switch (srcReg) {
                        case 0:
                            pendingCycles -= 12;
                            break;
                        case 1:
                            pendingCycles -= 16;
                            break;
                        case 2:
                            pendingCycles -= 12;
                            break;
                        case 3:
                            pendingCycles -= 14;
                            break;
                        case 4:
                            pendingCycles -= 8;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    break;
            }
        } else { // Long
            A[dstReg].s32 = ReadValueL(srcMode, srcReg);
            switch (srcMode) {
                case 0:
                    pendingCycles -= 4;
                    break;
                case 1:
                    pendingCycles -= 4;
                    break;
                case 2:
                    pendingCycles -= 12;
                    break;
                case 3:
                    pendingCycles -= 12;
                    break;
                case 4:
                    pendingCycles -= 14;
                    break;
                case 5:
                    pendingCycles -= 16;
                    break;
                case 6:
                    pendingCycles -= 18;
                    break;
                case 7:
                    switch (srcReg) {
                        case 0:
                            pendingCycles -= 16;
                            break;
                        case 1:
                            pendingCycles -= 20;
                            break;
                        case 2:
                            pendingCycles -= 16;
                            break;
                        case 3:
                            pendingCycles -= 18;
                            break;
                        case 4:
                            pendingCycles -= 12;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    break;
            }
        }
    }

    void MOVEA_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = ((op >> 12) & 0x03);
        int dstReg = ((op >> 9) & 0x07);
        int srcMode = ((op >> 3) & 0x07);
        int srcReg = (op & 0x07);

        if (size == 3) {
            info.Mnemonic = "movea.w";
            info.Args = DisassembleValue(srcMode, srcReg, 2, /* ref */ pc) + ", A" + dstReg;
        } else {
            info.Mnemonic = "movea.l";
            info.Args = DisassembleValue(srcMode, srcReg, 4, /* ref */ pc) + ", A" + dstReg;
        }
        info.Length = pc[0] - info.PC;
    }

    void MOVEP() {
        int dReg = ((op >> 9) & 0x07);
        int dir = ((op >> 7) & 0x01);
        int size = ((op >> 6) & 0x01);
        int aReg = (op & 0x07);
        if (dir == 0 && size == 0) {
            int ea;
            ea = A[aReg].s32 + ReadOpWord.apply(PC);
            PC += 2;
            D[dReg].u32 = (D[dReg].u32 & 0xffff_0000) | ((ReadByte.apply(ea) << 8) + ReadByte.apply(ea + 2));
            pendingCycles -= 16;
        } else if (dir == 0 && size == 1) {
            int ea;
            ea = A[aReg].s32 + ReadOpWord.apply(PC);
            PC += 2;
            D[dReg].u32 = (ReadByte.apply(ea) << 24) + (ReadByte.apply(ea + 2) << 16) + (ReadByte.apply(ea + 4) << 8) + ReadByte.apply(ea + 6);
            pendingCycles -= 24;
        } else if (dir == 1 && size == 0) {
            int src;
            int ea;
            ea = A[aReg].s32 + ReadOpWord.apply(PC);
            PC += 2;
            src = D[dReg].u32;
            WriteByte.accept(ea, (byte) ((src >> 8) & 0xff));
            WriteByte.accept(ea + 2, (byte) (src & 0xff));
            pendingCycles -= 16;
        } else if (dir == 1 && size == 1) {
            int src;
            int ea;
            ea = A[aReg].s32 + ReadOpWord.apply(PC);
            PC += 2;
            src = D[dReg].u32;
            WriteByte.accept(ea, (byte) ((src >> 24) & 0xff));
            WriteByte.accept(ea + 2, (byte) ((src >> 16) & 0xff));
            WriteByte.accept(ea + 4, (byte) ((src >> 8) & 0xff));
            WriteByte.accept(ea + 6, (byte) (src & 0xff));
            pendingCycles -= 24;
        }
    }

    void MOVEP_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dReg = ((op >> 9) & 0x07);
        int dir = ((op >> 7) & 0x01);
        int size = ((op >> 6) & 0x01);
        int aReg = (op & 0x07);
        if (size == 0) {
            info.Mnemonic = "movep.w";
            if (dir == 0) {
                info.Args = DisassembleValue(5, aReg, 2, /* ref */ pc) + ", D" + dReg;
            } else if (dir == 1) {
                info.Args = "D" + dReg + ", " + DisassembleValue(5, aReg, 2, /* ref */ pc);
            }
        } else if (size == 1) {
            info.Mnemonic = "movep.l";
            if (dir == 0) {
                info.Args = DisassembleValue(5, aReg, 4, /* ref */ pc) + ", D" + dReg;
            } else if (dir == 1) {
                info.Args = "D" + dReg + ", " + DisassembleValue(5, aReg, 4, /* ref */ pc);
            }
        }
        info.Length = pc[0] - info.PC;
    }

    void MOVEQ() {
        int value = (byte) op; // 8-bit data payload is sign-extended to 32-bits.
        N = (value & 0x80) != 0;
        Z = (value == 0);
        V = false;
        C = false;
        D[(op >> 9) & 7].s32 = value;
        pendingCycles -= 4;
    }

    void MOVEQ_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "moveq";
        info.Args = "$%X, %d".formatted((int) ((byte) op), (op >> 9) & 7);
    }

    static String DisassembleRegisterList0(short registers) {
        var str = new StringBuilder();
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if ((registers & 0x8000) != 0) {
                if (count > 0) str.append(",");
                str.append("D").append(i);
                count++;
            }
            registers <<= 1;
        }
        for (int i = 0; i < 8; i++) {
            if ((registers & 0x8000) != 0) {
                if (count > 0) str.append(",");
                str.append("A").append(i);
                count++;
            }
            registers <<= 1;
        }
        return str.toString();
    }

    static String DisassembleRegisterList1(short registers) {
        var str = new StringBuilder();
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if ((registers & 1) != 0) {
                if (count > 0) str.append(",");
                str.append("D").append(i);
                count++;
            }
            registers >>= 1;
        }
        for (int i = 0; i < 8; i++) {
            if ((registers & 1) != 0) {
                if (count > 0) str.append(",");
                str.append("A").append(i);
                count++;
            }
            registers >>= 1;
        }
        return str.toString();
    }

    void MOVEM0() {
        // Move register to memory
        int size = (op >> 6) & 1;
        int dstMode = (op >> 3) & 7;
        int dstReg = (op >> 0) & 7;

        short registers = ReadOpWord.apply(PC);
        PC += 2;
        int address = ReadAddress(dstMode, dstReg);
        int regCount = 0;

        if (size == 0) {
            // word-assign
            if (dstMode == 4) // decrement address
            {
                for (int i = 7; i >= 0; i--) {
                    if ((registers & 1) == 1) {
                        address -= 2;
                        WriteWord.accept(address, A[i].s16);
                        regCount++;
                    }
                    registers >>= 1;
                }
                for (int i = 7; i >= 0; i--) {
                    if ((registers & 1) == 1) {
                        address -= 2;
                        WriteWord.accept(address, D[i].s16);
                        regCount++;
                    }
                    registers >>= 1;
                }
                A[dstReg].s32 = address;
            } else { // increment address
                for (int i = 0; i <= 7; i++) {
                    if ((registers & 1) == 1) {
                        WriteWord.accept(address, D[i].s16);
                        address += 2;
                        regCount++;
                    }
                    registers >>= 1;
                }
                for (int i = 0; i <= 7; i++) {
                    if ((registers & 1) == 1) {
                        WriteWord.accept(address, A[i].s16);
                        address += 2;
                        regCount++;
                    }
                    registers >>= 1;
                }
            }
            pendingCycles -= regCount * 4;
        } else {
            // long-assign
            if (dstMode == 4) // decrement address
            {
                for (int i = 7; i >= 0; i--) {
                    if ((registers & 1) == 1) {
                        address -= 4;
                        WriteLong.accept(address, A[i].s32);
                        regCount++;
                    }
                    registers >>= 1;
                }
                for (int i = 7; i >= 0; i--) {
                    if ((registers & 1) == 1) {
                        address -= 4;
                        WriteLong.accept(address, D[i].s32);
                        regCount++;
                    }
                    registers >>= 1;
                }
                A[dstReg].s32 = address;
            } else { // increment address
                for (int i = 0; i <= 7; i++) {
                    if ((registers & 1) == 1) {
                        WriteLong.accept(address, D[i].s32);
                        address += 4;
                        regCount++;
                    }
                    registers >>= 1;
                }
                for (int i = 0; i <= 7; i++) {
                    if ((registers & 1) == 1) {
                        WriteLong.accept(address, A[i].s32);
                        address += 4;
                        regCount++;
                    }
                    registers >>= 1;
                }
            }
            pendingCycles -= regCount * 8;
        }

        switch (dstMode) {
            case 2:
                pendingCycles -= 8;
                break;
            case 3:
                pendingCycles -= 8;
                break;
            case 4:
                pendingCycles -= 8;
                break;
            case 5:
                pendingCycles -= 12;
                break;
            case 6:
                pendingCycles -= 14;
                break;
            case 7:
                switch (dstReg) {
                    case 0:
                        pendingCycles -= 12;
                        break;
                    case 1:
                        pendingCycles -= 16;
                        break;
                }
                break;
        }
    }

    void MOVEM0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        short registers = ReadOpWord.apply(pc[0]);
        pc[0] += 2;
        String address = DisassembleAddress(mode, reg, /* ref */ pc);

        info.Mnemonic = size == 0 ? "movem.w" : "movem.l";
        info.Args = DisassembleRegisterList0(registers) + ", " + address;
        info.Length = pc[0] - info.PC;
    }

    void MOVEM1() {
        // Move memory to register
        int size = (op >> 6) & 1;
        int srcMode = (op >> 3) & 7;
        int srcReg = (op >> 0) & 7;

        short registers = ReadOpWord.apply(PC);
        PC += 2;
        int address = ReadAddress(srcMode, srcReg);
        int regCount = 0;

        if (size == 0) {
            // word-assign
            for (int i = 0; i < 8; i++) {
                if ((registers & 1) == 1) {
                    if (srcMode == 7 && (srcReg == 2 || srcReg == 3)) {
                        D[i].s32 = ReadPcrelWord.apply(address);
                    } else {
                        D[i].s32 = ReadWord.apply(address);
                    }
                    address += 2;
                    regCount++;
                }
                registers >>= 1;
            }
            for (int i = 0; i < 8; i++) {
                if ((registers & 1) == 1) {
                    if (srcMode == 7 && (srcReg == 2 || srcReg == 3)) {
                        A[i].s32 = ReadPcrelWord.apply(address);
                    } else {
                        A[i].s32 = ReadWord.apply(address);
                    }
                    address += 2;
                    regCount++;
                }
                registers >>= 1;
            }
            pendingCycles -= regCount * 4;
            if (srcMode == 3)
                A[srcReg].s32 = address;
        } else {
            // long-assign
            for (int i = 0; i < 8; i++) {
                if ((registers & 1) == 1) {
                    if (srcMode == 7 && (srcReg == 2 || srcReg == 3)) {
                        D[i].s32 = ReadPcrelLong.apply(address);
                    } else {
                        D[i].s32 = ReadLong.apply(address);
                    }
                    address += 4;
                    regCount++;
                }
                registers >>= 1;
            }
            for (int i = 0; i < 8; i++) {
                if ((registers & 1) == 1) {
                    if (srcMode == 7 && (srcReg == 2 || srcReg == 3)) {
                        A[i].s32 = ReadPcrelLong.apply(address);
                    } else {
                        A[i].s32 = ReadLong.apply(address);
                    }
                    address += 4;
                    regCount++;
                }
                registers >>= 1;
            }
            pendingCycles -= regCount * 8;
            if (srcMode == 3)
                A[srcReg].s32 = address;
        }

        switch (srcMode) {
            case 2:
                pendingCycles -= 12;
                break;
            case 3:
                pendingCycles -= 12;
                break;
            case 4:
                pendingCycles -= 12;
                break;
            case 5:
                pendingCycles -= 16;
                break;
            case 6:
                pendingCycles -= 18;
                break;
            case 7:
                switch (srcReg) {
                    case 0:
                        pendingCycles -= 16;
                        break;
                    case 1:
                        pendingCycles -= 20;
                        break;
                    case 2:
                        pendingCycles -= 16;
                        break;
                    case 3:
                        pendingCycles -= 18;
                        break;
                }
                break;
        }
    }

    void MOVEM1_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        short registers = ReadOpWord.apply(pc[0]);
        pc[0] += 2;
        String address = DisassembleAddress(mode, reg, /* ref */ pc);

        info.Mnemonic = size == 0 ? "movem.w" : "movem.l";
        info.Args = address + ", " + DisassembleRegisterList1(registers);
        info.Length = pc[0] - info.PC;
    }

    void LEA() {
        int mode = (op >> 3) & 7;
        int sReg = (op >> 0) & 7;
        int dReg = (op >> 9) & 7;

        A[dReg].u32 = ReadAddress(mode, sReg);
        switch (mode) {
            case 2:
                pendingCycles -= 4;
                break;
            case 5:
                pendingCycles -= 8;
                break;
            case 6:
                pendingCycles -= 12;
                break;
            case 7:
                switch (sReg) {
                    case 0:
                        pendingCycles -= 8;
                        break;
                    case 1:
                        pendingCycles -= 12;
                        break;
                    case 2:
                        pendingCycles -= 8;
                        break;
                    case 3:
                        pendingCycles -= 12;
                        break;
                }
                break;
        }
    }

    void LEA_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int sReg = (op >> 0) & 7;
        int dReg = (op >> 9) & 7;

        info.Mnemonic = "lea";
        info.Args = DisassembleAddress(mode, sReg, /* ref */ pc);
        info.Args += ", A" + dReg;

        info.Length = pc[0] - info.PC;
    }

    void CLR() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                WriteValueB(mode, reg, (byte) 0);
                pendingCycles -= mode == 0 ? 4 : 8 + EACyclesBW[mode][reg];
                break;
            case 1:
                WriteValueW(mode, reg, (short) 0);
                pendingCycles -= mode == 0 ? 4 : 8 + EACyclesBW[mode][reg];
                break;
            case 2:
                WriteValueL(mode, reg, 0);
                pendingCycles -= mode == 0 ? 6 : 12 + EACyclesL[mode][reg];
                break;
        }

        N = V = C = false;
        Z = true;
    }

    void CLR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "clr.b";
                info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "clr.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "clr.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void EXT() {
        int size = (op >> 6) & 1;
        int reg = op & 7;

        switch (size) {
            case 0: // ext.w
                D[reg].s16 = D[reg].s8;
                N = (D[reg].s16 & 0x8000) != 0;
                Z = (D[reg].s16 == 0);
                break;
            case 1: // ext.l
                D[reg].s32 = D[reg].s16;
                N = (D[reg].s32 & 0x8000_0000) != 0;
                Z = (D[reg].s32 == 0);
                break;
        }

        V = false;
        C = false;
        pendingCycles -= 4;
    }

    void EXT_Disasm(DisassemblyInfo info) {
        int size = (op >> 6) & 1;
        int reg = op & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "ext.w";
                info.Args = "D" + reg;
                break;
            case 1:
                info.Mnemonic = "ext.l";
                info.Args = "D" + reg;
                break;
        }
    }

    void PEA() {
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        int ea = ReadAddress(mode, reg);

        A[7].s32 -= 4;
        WriteLong.accept(A[7].s32, ea);

        switch (mode) {
            case 2:
                pendingCycles -= 12;
                break;
            case 5:
                pendingCycles -= 16;
                break;
            case 6:
                pendingCycles -= 20;
                break;
            case 7:
                switch (reg) {
                    case 0:
                        pendingCycles -= 16;
                        break;
                    case 1:
                        pendingCycles -= 20;
                        break;
                    case 2:
                        pendingCycles -= 16;
                        break;
                    case 3:
                        pendingCycles -= 20;
                        break;
                }
                break;
        }
    }

    void PEA_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        info.Mnemonic = "pea";
        info.Args = DisassembleAddress(mode, reg, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

//#endregion

//#region IntegerMath

    void ADD0() {
        int Dreg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte value = ReadValueB(mode, reg);
                int result = D[Dreg].s8 + value;
                int uresult = D[Dreg].u8 + value;
                X = C = (uresult & 0x100) != 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                N = (result & 0x80) != 0;
                Z = (result & 0xff) == 0;
                D[Dreg].s8 = (byte) result;
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short value = ReadValueW(mode, reg);
                int result = D[Dreg].s16 + value;
                int uresult = D[Dreg].u16 + value;
                X = C = (uresult & 0x1_0000) != 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                N = (result & 0x8000) != 0;
                Z = (result & 0xffff) == 0;
                D[Dreg].s16 = (short) result;
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int value = ReadValueL(mode, reg);
                long result = (long) D[Dreg].s32 + (long) value;
                long uresult = (long) D[Dreg].u32 + ((long) value);
                X = C = (uresult & 0x100000000L) != 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                N = (result & 0x8000_0000) != 0;
                Z = (int) result == 0;
                D[Dreg].s32 = (int) result;
                if (mode == 0 || mode == 1 || (mode == 7 && reg == 4)) {
                    pendingCycles -= 8 + EACyclesL[mode][reg];
                } else {
                    pendingCycles -= 6 + EACyclesL[mode][reg];
                }
            }
        }
    }

    void ADD1() {
        int Dreg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte value = PeekValueB(mode, reg);
                int result = value + D[Dreg].s8;
                int uresult = value + D[Dreg].u8;
                X = C = (uresult & 0x100) != 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                N = (result & 0x80) != 0;
                Z = (result & 0xff) == 0;
                WriteValueB(mode, reg, (byte) result);
                pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short value = PeekValueW(mode, reg);
                int result = value + D[Dreg].s16;
                int uresult = value + D[Dreg].u16;
                X = C = (uresult & 0x1_0000) != 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                N = (result & 0x8000) != 0;
                Z = (result & 0xffff) == 0;
                WriteValueW(mode, reg, (short) result);
                pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int value = PeekValueL(mode, reg);
                long result = (long) value + (long) D[Dreg].s32;
                long uresult = ((long) value) + (long) D[Dreg].u32;
                X = C = (uresult & 0x100000000L) != 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                N = (result & 0x8000_0000) != 0;
                Z = ((int) result == 0);
                WriteValueL(mode, reg, (int) result);
                pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void ADD_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int Dreg = (op >> 9) & 7;
        int dir = (op >> 8) & 1;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        String op1 = "D" + Dreg;
        String op2;

        switch (size) {
            case 0:
                info.Mnemonic = "add.b";
                op2 = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "add.w";
                op2 = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            default:
                info.Mnemonic = "add.l";
                op2 = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Args = dir == 0 ? (op2 + ", " + op1) : (op1 + ", " + op2);
        info.Length = pc[0] - info.PC;
    }

    void ADDI() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                int immed = (byte) (short) ReadOpWord.apply(PC);
                PC += 2;
                byte value = PeekValueB(mode, reg);
                int result = value + immed;
                int uresult = value + (byte) immed;
                X = C = (uresult & 0x100) != 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                N = (result & 0x80) != 0;
                Z = (result & 0xff) == 0;
                WriteValueB(mode, reg, (byte) result);
                if (mode == 0) pendingCycles -= 8;
                else pendingCycles -= 12 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                int immed = ReadOpWord.apply(PC);
                PC += 2;
                short value = PeekValueW(mode, reg);
                int result = value + immed;
                int uresult = value + (short) immed;
                X = C = (uresult & 0x1_0000) != 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                N = (result & 0x8000) != 0;
                Z = (result & 0xffff) == 0;
                WriteValueW(mode, reg, (short) result);
                if (mode == 0) pendingCycles -= 8;
                else pendingCycles -= 12 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int immed = ReadOpLong.apply(PC);
                PC += 4;
                int value = PeekValueL(mode, reg);
                long result = (long) value + (long) immed;
                long uresult = ((long) value) + ((long) immed);
                X = C = (uresult & 0x1_0000_0000L) != 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                N = (result & 0x8000_0000) != 0;
                Z = ((int) result == 0);
                WriteValueL(mode, reg, (int) result);
                if (mode == 0)
                    pendingCycles -= 16;
                else
                    pendingCycles -= 20 + EACyclesL[mode][reg];
            }
        }
    }

    void ADDI_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "addi.b";
                info.Args = DisassembleImmediate(1, /* ref */ pc) + ", " + DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "addi.w";
                info.Args = DisassembleImmediate(2, /* ref */ pc) + ", " + DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "addi.l";
                info.Args = DisassembleImmediate(4, /* ref */ pc) + ", " + DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void ADDQ() {
        int data = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        data = data == 0 ? 8 : data; // range is 1-8; 0 represents 8

        switch (size) {
            case 0: // byte
            {
                if (mode == 1) throw new IllegalStateException("ADDQ.B on address reg is invalid");
                byte value = PeekValueB(mode, reg);
                int result = value + data;
                int uresult = value + data;
                N = (result & 0x80) != 0;
                Z = result == 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = X = (uresult & 0x100) != 0;
                WriteValueB(mode, reg, (byte) result);
                if (mode == 0) pendingCycles -= 4;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                if (mode == 1) {
                    int value = PeekValueL(mode, reg);
                    WriteValueL(mode, reg, value + data);
                } else {
                    short value = PeekValueW(mode, reg);
                    int result = value + data;
                    int uresult = value + data;
                    N = (result & 0x8000) != 0;
                    Z = result == 0;
                    V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                    C = X = (uresult & 0x1_0000) != 0;
                    WriteValueW(mode, reg, (short) result);
                }
                if (mode <= 1)
                    pendingCycles -= 4;
                else
                    pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            default: // long
            {
                int value = PeekValueL(mode, reg);
                long result = (long) value + (long) data;
                long uresult = ((long) value) + ((long) data);
                if (mode != 1) {
                    N = (result & 0x8000_0000L) != 0;
                    Z = (result == 0);
                    V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                    C = X = (uresult & 0x1_0000_0000L) != 0;
                }
                WriteValueL(mode, reg, (int) result);
                if (mode <= 1)
                    pendingCycles -= 8;
                else
                    pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void ADDQ_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int data = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        data = data == 0 ? 8 : data; // range is 1-8; 0 represents 8

        switch (size) {
            case 0:
                info.Mnemonic = "addq.b";
                info.Args = data + ", " + DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "addq.w";
                info.Args = data + ", " + DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "addq.l";
                info.Args = data + ", " + DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void ADDA() {
        int aReg = (op >> 9) & 7;
        int size = (op >> 8) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        if (size == 0) { // word
            int value = ReadValueW(mode, reg);
            A[aReg].s32 += value;
            pendingCycles -= 8 + EACyclesBW[mode][reg];
        } else { // long
            int value = ReadValueL(mode, reg);
            A[aReg].s32 += value;
            if (mode == 0 || mode == 1 || (mode == 7 && reg == 4))
                pendingCycles -= 8 + EACyclesL[mode][reg];
            else
                pendingCycles -= 6 + EACyclesL[mode][reg];
        }
    }

    void ADDA_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int aReg = (op >> 9) & 7;
        int size = (op >> 8) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        info.Mnemonic = (size == 0) ? "adda.w" : "adda.l";
        info.Args = DisassembleValue(mode, reg, (size == 0) ? 2 : 4, /* ref */ pc) + ", A" + aReg;

        info.Length = pc[0] - info.PC;
    }

    void SUB0() {
        int dReg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte a = D[dReg].s8;
                byte b = ReadValueB(mode, reg);
                int result = a - b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                N = (result & 0x80) != 0;
                Z = result == 0;
                D[dReg].s8 = (byte) result;
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short a = D[dReg].s16;
                short b = ReadValueW(mode, reg);
                int result = a - b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                N = (result & 0x8000) != 0;
                Z = result == 0;
                D[dReg].s16 = (short) result;
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int a = D[dReg].s32;
                int b = ReadValueL(mode, reg);
                long result = (long) a - (long) b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                N = (result & 0x8000_0000) != 0;
                Z = result == 0;
                D[dReg].s32 = (int) result;
                if (mode == 0 || mode == 1 || (mode == 7 && reg == 4)) {
                    pendingCycles -= 8 + EACyclesL[mode][reg];
                } else {
                    pendingCycles -= 6 + EACyclesL[mode][reg];
                }
            }
        }
    }

    void SUB1() {
        int dReg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte a = PeekValueB(mode, reg);
                byte b = D[dReg].s8;
                int result = a - b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                N = (result & 0x80) != 0;
                Z = result == 0;
                WriteValueB(mode, reg, (byte) result);
                pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short a = PeekValueW(mode, reg);
                short b = D[dReg].s16;
                int result = a - b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                N = (result & 0x8000) != 0;
                Z = result == 0;
                WriteValueW(mode, reg, (short) result);
                pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int a = PeekValueL(mode, reg);
                int b = D[dReg].s32;
                long result = (long) a - (long) b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                N = (result & 0x8000_0000) != 0;
                Z = ((int) result == 0);
                WriteValueL(mode, reg, (int) result);
                pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void SUB_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dReg = (op >> 9) & 7;
        int dir = (op >> 8) & 1;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        String op1 = "D" + dReg;
        String op2;

        switch (size) {
            case 0:
                info.Mnemonic = "sub.b";
                op2 = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "sub.w";
                op2 = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            default:
                info.Mnemonic = "sub.l";
                op2 = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Args = dir == 0 ? (op2 + ", " + op1) : (op1 + ", " + op2);
        info.Length = pc[0] - info.PC;
    }

    void SUBI() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte b = (byte) (short) ReadOpWord.apply(PC);
                PC += 2;
                byte a = PeekValueB(mode, reg);
                int result = a - b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                N = (result & 0x80) != 0;
                Z = result == 0;
                WriteValueB(mode, reg, (byte) result);
                if (mode == 0) pendingCycles -= 8;
                else pendingCycles -= 12 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short b = ReadOpWord.apply(PC);
                PC += 2;
                short a = PeekValueW(mode, reg);
                int result = a - b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                N = (result & 0x8000) != 0;
                Z = result == 0;
                WriteValueW(mode, reg, (short) result);
                if (mode == 0) pendingCycles -= 8;
                else pendingCycles -= 12 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int b = ReadOpLong.apply(PC);
                PC += 4;
                int a = PeekValueL(mode, reg);
                long result = (long) a - (long) b;
                X = C = ((a < b) ^ ((a ^ b) >= 0) == false);
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                N = (result & 0x8000_0000) != 0;
                Z = ((int) result == 0);
                WriteValueL(mode, reg, (int) result);
                if (mode == 0)
                    pendingCycles -= 16;
                else
                    pendingCycles -= 20 + EACyclesL[mode][reg];
            }
        }
    }

    void SUBI_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "subi.b";
                info.Args = DisassembleImmediate(1, /* ref */ pc) + ", " + DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "subi.w";
                info.Args = DisassembleImmediate(2, /* ref */ pc) + ", " + DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "subi.l";
                info.Args = DisassembleImmediate(4, /* ref */ pc) + ", " + DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void SUBQ() {
        int data = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        data = data == 0 ? 8 : data; // range is 1-8; 0 represents 8

        switch (size) {
            case 0: // byte
            {
                if (mode == 1) throw new IllegalStateException("SUBQ.B on address reg is invalid");
                byte value = PeekValueB(mode, reg);
                int result = value - data;
                N = (result & 0x80) != 0;
                Z = result == 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = X = ((value < data) ^ ((value ^ data) >= 0) == false);
                WriteValueB(mode, reg, (byte) result);
                if (mode == 0) pendingCycles -= 4;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                if (mode == 1) {
                    int value = PeekValueL(mode, reg);
                    WriteValueL(mode, reg, value - data);
                } else {
                    short value = PeekValueW(mode, reg);
                    int result = value - data;
                    N = (result & 0x8000) != 0;
                    Z = result == 0;
                    V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                    C = X = ((value < data) ^ ((value ^ data) >= 0) == false);
                    WriteValueW(mode, reg, (short) result);
                }
                if (mode == 0)
                    pendingCycles -= 4;
                else if (mode == 1)
                    pendingCycles -= 8;
                else
                    pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            default: // long
            {
                int value = PeekValueL(mode, reg);
                long result = (long) value - (long) data;
                if (mode != 1) {
                    N = (result & 0x8000_0000) != 0;
                    Z = (result == 0);
                    V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                    C = X = ((value < data) ^ ((value ^ data) >= 0) == false);
                }
                WriteValueL(mode, reg, (int) result);
                if (mode <= 1) pendingCycles -= 8;
                else pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void SUBQ_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int data = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        data = data == 0 ? 8 : data; // range is 1-8; 0 represents 8

        switch (size) {
            case 0:
                info.Mnemonic = "subq.b";
                info.Args = data + ", " + DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "subq.w";
                info.Args = data + ", " + DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "subq.l";
                info.Args = data + ", " + DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void SUBA() {
        int aReg = (op >> 9) & 7;
        int size = (op >> 8) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        if (size == 0) // word
        {
            int value = ReadValueW(mode, reg);
            A[aReg].s32 -= value;
            pendingCycles -= 8 + EACyclesBW[mode][reg];
        } else { // long
            int value = ReadValueL(mode, reg);
            A[aReg].s32 -= value;
            if (mode == 0 || mode == 1 || (mode == 7 && reg == 4))
                pendingCycles -= 8 + EACyclesL[mode][reg];
            else
                pendingCycles -= 6 + EACyclesL[mode][reg];
        }
    }

    void SUBA_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};

        int aReg = (op >> 9) & 7;
        int size = (op >> 8) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        info.Mnemonic = (size == 0) ? "suba.w" : "suba.l";
        info.Args = DisassembleValue(mode, reg, (size == 0) ? 2 : 4, /* ref */ pc) + ", A" + aReg;

        info.Length = pc[0] - info.PC;
    }

    void NEG() {
        int size = (op >> 6) & 0x03;
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;

        if (mode == 1) throw new IllegalStateException("NEG on address reg is invalid");

        switch (size) {
            case 0: // Byte
            {
                byte value = PeekValueB(mode, reg);
                int result = 0 - value;
                N = (result & 0x80) != 0;
                Z = result == 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = X = ((0 < value) ^ ((0 ^ value) >= 0) == false);
                WriteValueB(mode, reg, (byte) result);
                if (mode == 0) pendingCycles -= 4;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // Word
            {
                short value = PeekValueW(mode, reg);
                int result = 0 - value;
                N = (result & 0x8000) != 0;
                Z = result == 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                C = X = ((0 < value) ^ ((0 ^ value) >= 0) == false);
                WriteValueW(mode, reg, (short) result);
                if (mode == 0) pendingCycles -= 4;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // Long
            {
                int value = PeekValueL(mode, reg);
                long result = 0 - value;
                N = (result & 0x8000_0000) != 0;
                Z = result == 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = X = ((0 < value) ^ ((0 ^ value) >= 0) == false);
                WriteValueL(mode, reg, (int) result);
                if (mode == 0)
                    pendingCycles -= 6;
                else
                    pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void NEG_Disasm(DisassemblyInfo info) {
        int size = (op >> 6) & 0x03;
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;

        int[] pc = new int[] {info.PC + 2};

        switch (size) {
            case 0: // Byte
                info.Mnemonic = "neg.b";
                info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1: // Word
                info.Mnemonic = "neg.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2: // Long
                info.Mnemonic = "neg.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }

        info.Length = pc[0] - info.PC;
    }

    void NBCD() {
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        byte result = PeekValueB(mode, reg);
        result = (byte) ((0x9a - result - (X ? 1 : 0)) & 0xff);
        if (result != (byte) 0x9a) {
            V = (((~result) & 0x80) != 0);
            if ((result & 0x0f) == 0x0a) {
                result = (byte) ((result & 0xf0) + 0x10);
            }
            V &= ((result & 0x80) != 0);
            WriteValueB(mode, reg, result);
            Z &= (result == 0);
            C = true;
            X = true;
        } else {
            V = false;
            C = false;
            X = false;
        }
        N = ((result & 0x80) != 0);
        pendingCycles -= (mode == 0) ? 6 : 8 + EACyclesBW[mode][reg];
    }

    void NBCD_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "nbcd.b";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ILLEGAL() {
        TrapVector2(4);
        pendingCycles -= 4;
    }

    void ILLEGAL_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "illegal";
        info.Args = "";
    }

    void ILL() {
        TrapVector2(4);
    }

    void ILL_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "ill";
        info.Args = "";
    }

    void STOP() {
        if (s) {
            short new_sr = ReadOpWord.apply(PC);
            PC += 2;
            stopped = true;
            SR = new_sr;
            pendingCycles = 0;
        } else {
            TrapVector2(8);
        }
        pendingCycles -= 4;
    }

    void STOP_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "stop";
        info.Args = DisassembleImmediate(2, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void TRAPV() {
        if (!V) {
            pendingCycles -= 4;
        } else {
            TrapVector(7);
            pendingCycles -= 4;
        }
    }

    void TRAPV_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "trapv";
        info.Args = "";
    }

    void CHK() {
        int dreg = (op >> 9) & 0x07;
        int boundMode = (op >> 3) & 0x07;
        int boundReg = op & 0x07;
        short src, bound;
        src = D[dreg].s16;
        bound = ReadValueW(boundMode, boundReg);
        Z = (src == 0);
        V = false;
        C = false;
        if (src >= 0 && src <= bound) {
            pendingCycles -= 10 + EACyclesBW[boundMode][boundReg];
        } else {
            N = (src < 0);
            TrapVector(6);
            pendingCycles -= 10 + EACyclesBW[boundMode][boundReg];
        }
    }

    void CHK_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        info.Mnemonic = "chk.w";
        info.Args = "%s, D%d".formatted(DisassembleValue(mode, reg, 2, /* ref */ pc), dreg);
        info.Length = pc[0] - info.PC;
    }

    void NEGX() {
        int size = (op >> 6) & 0x03;
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;

        if (mode == 1) {
            throw new IllegalStateException("NEG on address reg is invalid");
        }
        switch (size) {
            case 0: // Byte
            {
                byte value = PeekValueB(mode, reg);
                int result = 0 - value - (X ? 1 : 0);
                N = (result & 0x80) != 0;
                Z &= (result == 0);
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = X = ((0 < value) ^ ((0 ^ value) >= 0) == false);
                WriteValueB(mode, reg, (byte) result);
                if (mode == 0) pendingCycles -= 4;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // Word
            {
                short value = PeekValueW(mode, reg);
                int result = 0 - value - (X ? 1 : 0);
                N = (result & 0x8000) != 0;
                Z &= (result == 0);
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                C = X = ((0 < value) ^ ((0 ^ value) >= 0) == false);
                WriteValueW(mode, reg, (short) result);
                if (mode == 0) pendingCycles -= 4;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // Long
            {
                int value = PeekValueL(mode, reg);
                long result = 0 - value - (X ? 1 : 0);
                N = (result & 0x8000_0000) != 0;
                Z &= (result == 0);
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = X = ((0 < value) ^ ((0 ^ value) >= 0) == false);
                WriteValueL(mode, reg, (int) result);
                if (mode == 0) pendingCycles -= 6;
                else pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void NEGX_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        switch (size) {
            case 0:
                info.Mnemonic = "negx.b";
                info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "negx.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "negx.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void SBCD0() {
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        int dst = D[dstReg].u32;
        int src = D[srcReg].u32;
        int res;
        res = (dst & 0x0f) - (src & 0x0f) - (X ? 1 : 0);
        V = false;
        if (res > 9) {
            res -= 6;
        }
        res += (dst & 0xf0) - (src & 0xf0);
        if (res > 0x99) {
            res += 0xa0;
            X = C = true;
            N = true;
        } else {
            N = X = C = false;
        }
        res = res & 0xff;
        Z &= (res == 0);
        D[dstReg].u32 = (D[dstReg].u32 & 0xffff_ff00) | res;
        pendingCycles -= 6;
    }

    void SBCD0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        info.Mnemonic = "sbcd.b";
        info.Args = DisassembleValue(0, srcReg, 1, /* ref */ pc) + "," + DisassembleValue(0, dstReg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void SBCD1() {
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        int src, dst, res;
        if (srcReg == 7) {
            A[srcReg].u32 -= 2;
        } else {
            A[srcReg].u32--;
        }
        if (dstReg == 7) {
            A[dstReg].u32 -= 2;
        } else {
            A[dstReg].u32--;
        }
        src = (int) ReadByte.apply(A[srcReg].s32);
        dst = (int) ReadByte.apply(A[dstReg].s32);
        res = (dst & 0x0f) - (src & 0x0f) - (X ? 1 : 0);
        V = false;
        if (res > 9) {
            res -= 6;
        }
        res += (dst & 0xf0) - (src & 0xf0);
        if (res > 0x99) {
            res += 0xa0;
            X = C = true;
            N = true;
        } else {
            N = X = C = false;
        }
        res = res & 0xff;
        Z &= (res == 0);
        WriteByte.accept(A[dstReg].s32, (byte) res);
        pendingCycles -= 18;
    }

    void SBCD1_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        info.Mnemonic = "sbcd.b";
        info.Args = DisassembleValue(4, srcReg, 1, /* ref */ pc) + "," + DisassembleValue(4, dstReg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ABCD0() {
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        int src, dst, res;
        src = D[srcReg].u32;
        dst = D[dstReg].u32;
        res = (src & 0x0f) + (dst & 0x0f) + (X ? 1 : 0);
        V = (((~res) & 0x80) != 0);
        if (res > 9) {
            res += 6;
        }
        res += (src & 0xf0) + (dst & 0xf0);
        X = C = (res > 0x99);
        if (C) {
            res -= 0xa0;
        }
        V &= ((res & 0x80) != 0);
        N = ((res & 0x80) != 0);
        res = res & 0xff;
        Z &= (res == 0);
        D[dstReg].u32 = (((D[dstReg].u32) & 0xffff_ff00) | res);
        pendingCycles -= 6;
    }

    void ABCD0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        info.Mnemonic = "abcd.b";
        info.Args = DisassembleValue(0, srcReg, 1, /* ref */ pc) + "," + DisassembleValue(0, dstReg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ABCD1() {
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        int src, dst, res;
        if (srcReg == 7) {
            A[srcReg].u32 -= 2;
        } else {
            A[srcReg].u32--;
        }
        if (dstReg == 7) {
            A[dstReg].u32 -= 2;
        } else {
            A[dstReg].u32--;
        }
        src = (int) ReadByte.apply(A[srcReg].s32);
        dst = (int) ReadByte.apply(A[dstReg].s32);
        res = (src & 0x0f) + (dst & 0x0f) + (X ? 1 : 0);
        V = (((~res) & 0x80) != 0);
        if (res > 9) {
            res += 6;
        }
        res += (src & 0xf0) + (dst & 0xf0);
        X = C = (res > 0x99);
        if (C) {
            res -= 0xa0;
        }
        V &= ((res & 0x80) != 0);
        N = ((res & 0x80) != 0);
        res = res & 0xff;
        Z &= (res == 0);
        WriteByte.accept(A[dstReg].s32, (byte) res);
        pendingCycles -= 18;
    }

    void ABCD1_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int srcReg = op & 0x07;
        info.Mnemonic = "abcd.b";
        info.Args = DisassembleValue(4, srcReg, 1, /* ref */ pc) + "," + DisassembleValue(4, dstReg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void EXGdd() {
        int reg_a = (op >> 9) & 0x07;
        int reg_b = op & 0x07;
        int tmp;
        tmp = D[reg_a].u32;
        D[reg_a].u32 = D[reg_b].u32;
        D[reg_b].u32 = tmp;
        pendingCycles -= 6;
    }

    void EXGdd_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int reg_a = (op >> 9) & 0x07;
        int reg_b = op & 0x07;
        info.Mnemonic = "exg";
        info.Args = DisassembleValue(0, reg_a, 1, /* ref */ pc) + "," + DisassembleValue(0, reg_b, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void EXGaa() {
        int reg_a = (op >> 9) & 0x07;
        int reg_b = op & 0x07;
        int tmp;
        tmp = A[reg_a].u32;
        A[reg_a].u32 = A[reg_b].u32;
        A[reg_b].u32 = tmp;
        pendingCycles -= 6;
    }

    void EXGaa_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int reg_a = (op >> 9) & 0x07;
        int reg_b = op & 0x07;
        info.Mnemonic = "exg";
        info.Args = DisassembleValue(1, reg_a, 1, /* ref */ pc) + "," + DisassembleValue(1, reg_b, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void EXGda() {
        int reg_a = (op >> 9) & 0x07;
        int reg_b = op & 0x07;
        int tmp;
        tmp = D[reg_a].u32;
        D[reg_a].u32 = A[reg_b].u32;
        A[reg_b].u32 = tmp;
        pendingCycles -= 6;
    }

    void EXGda_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int reg_a = (op >> 9) & 0x07;
        int reg_b = op & 0x07;
        info.Mnemonic = "exg";
        info.Args = DisassembleValue(0, reg_a, 1, /* ref */ pc) + "," + DisassembleValue(1, reg_b, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void ADDX0() {
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;
        switch (size) {
            case 0: {
                int src = D[srcReg].u32 & 0xff;
                int dst = D[dstReg].u32 & 0xff;
                int res;
                res = dst + src + (X ? 1 : 0);
                N = ((res & 0x80) != 0);
                V = (((src ^ res & (dst ^ res)) & 0x80) != 0);
                X = C = ((res & 0x100) != 0);
                res = res & 0xff;
                Z &= (res == 0);
                D[dstReg].u32 = (D[dstReg].u32 & 0xffff_ff00) | res;
                pendingCycles -= 4;
                return;
            }
            case 1: {
                int src = D[srcReg].u32 & 0xffff;
                int dst = D[dstReg].u32 & 0xffff;
                int res;
                res = dst + src + (X ? 1 : 0);
                N = ((res & 0x8000) != 0);
                V = ((((src ^ res) & (dst ^ res)) & 0x8000) != 0);
                X = C = ((res & 0x1_0000) != 0);
                res = res & 0xffff;
                Z &= (res == 0);
                D[dstReg].u32 = (D[dstReg].u32 & 0xffff_0000) | res;
                pendingCycles -= 4;
                return;
            }
            case 2: {
                int src = D[srcReg].u32;
                int dst = D[dstReg].u32;
                int res;
                res = dst + src + (X ? 1 : 0);
                N = ((res & 0x8000_0000) != 0);
                V = ((((src ^ res) & (dst ^ res)) & 0x8000_0000) != 0);
                X = C = ((((src & dst) | (~res & (src | dst))) & 0x8000_0000) != 0);
                Z &= (res == 0);
                D[dstReg].u32 = res;
                pendingCycles -= 8;
            }
        }
    }

    void ADDX0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;
        switch (size) {
            case 0:
                info.Mnemonic = "addx.b";
                info.Args = DisassembleValue(0, srcReg, 1, /* ref */ pc) + ", D" + dstReg;
                break;
            case 1:
                info.Mnemonic = "addx.w";
                info.Args = DisassembleValue(0, srcReg, 2, /* ref */ pc) + ", D" + dstReg;
                break;
            case 2:
                info.Mnemonic = "addx.l";
                info.Args = DisassembleValue(0, srcReg, 4, /* ref */ pc) + ", D" + dstReg;
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void ADDX1() {
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;
        switch (size) {
            case 0: {
                if (srcReg == 7) {
                    A[srcReg].u32 -= 2;
                } else {
                    A[srcReg].u32--;
                }
                if (dstReg == 7) {
                    A[dstReg].u32 -= 2;
                } else {
                    A[dstReg].u32--;
                }
                int src = (int) ReadByte.apply(A[srcReg].s32);
                int dst = (int) ReadByte.apply(A[dstReg].s32);
                int res;
                res = dst + src + (X ? 1 : 0);
                N = ((res & 0x80) != 0);
                V = ((((src ^ res) & (dst ^ res)) & 0x80) != 0);
                X = C = ((res & 0x100) != 0);
                res = res & 0xff;
                Z &= (res == 0);
                WriteByte.accept(A[dstReg].s32, (byte) res);
                pendingCycles -= 18;
                return;
            }
            case 1: {
                A[srcReg].u32 -= 2;
                int src = (int) ReadWord.apply(A[srcReg].s32);
                A[dstReg].u32 -= 2;
                int dst = (int) ReadWord.apply(A[dstReg].s32);
                int res;
                res = dst + src + (X ? 1 : 0);
                N = ((res & 0x8000) != 0);
                V = ((((src ^ res) & (dst ^ res)) & 0x8000) != 0);
                X = C = ((res & 0x1_0000) != 0);
                res = res & 0xffff;
                Z &= (res == 0);
                WriteWord.accept(A[dstReg].s32, (short) res);
                pendingCycles -= 18;
                return;
            }
            case 2: {
                A[srcReg].u32 -= 4;
                int src = ReadLong.apply(A[srcReg].s32);
                A[dstReg].u32 -= 4;
                int dst = (int) ReadWord.apply(A[dstReg].s32);
                int res;
                res = dst + src + (X ? 1 : 0);
                N = ((res & 0x8000_0000) != 0);
                V = (((((src ^ res) & (dst ^ res)) >> 24) & 0x80) != 0);
                X = C = (((((src & dst) | (~res & (src | dst))) >> 23) & 0x100) != 0);
                Z &= (res == 0);
                WriteLong.accept(A[dstReg].s32, res);
                pendingCycles -= 30;
            }
        }
    }

    void ADDX1_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;

        switch (size) {
            case 0:
                info.Mnemonic = "addx.b";
                info.Args = DisassembleValue(4, srcReg, 1, /* ref */ pc) + ", " + DisassembleValue(4, dstReg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "addx.w";
                info.Args = DisassembleValue(4, srcReg, 2, /* ref */ pc) + ", " + DisassembleValue(4, dstReg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "addx.l";
                info.Args = DisassembleValue(4, srcReg, 4, /* ref */ pc) + ", " + DisassembleValue(4, dstReg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void SUBX0() {
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;
        switch (size) {
            case 0: {
                int src = D[srcReg].u32 & 0xff;
                int dst = D[dstReg].u32 & 0xff;
                int res;
                res = dst - src - (X ? 1 : 0);
                N = ((res & 0x80) != 0);
                X = C = ((res & 0x100) != 0);
                V = ((((src ^ dst) & (res ^ dst)) & 0x80) != 0);
                res = res & 0xff;
                Z &= (res == 0);
                D[dstReg].u32 = (D[dstReg].u32 & 0xffff_ff00) | res;
                pendingCycles -= 4;
                return;
            }
            case 1: {
                int src = D[srcReg].u32 & 0xffff;
                int dst = D[dstReg].u32 & 0xffff;
                int res;
                res = dst - src - (X ? 1 : 0);
                N = ((res & 0x8000) != 0);
                X = C = ((res & 0x1_0000) != 0);
                V = ((((src ^ dst) & (res ^ dst)) & 0x8000) != 0);
                res = res & 0xffff;
                Z &= (res == 0);
                D[dstReg].u32 = (D[dstReg].u32 & 0xffff_0000) | res;
                pendingCycles -= 4;
                return;
            }
            case 2: {
                int src = D[srcReg].u32;
                int dst = D[dstReg].u32;
                int res;
                res = dst - src - (X ? 1 : 0);
                N = ((res & 0x8000_0000) != 0);
                X = C = (((((src & res) | (~dst & (src | res))) >> 23) & 0x100) != 0);
                V = (((((src ^ dst) & (res ^ dst)) >> 24) & 0x80) != 0);
                Z &= (res == 0);
                D[dstReg].u32 = res;
                pendingCycles -= 8;
            }
        }
    }

    void SUBX0_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;
        switch (size) {
            case 0:
                info.Mnemonic = "subx.b";
                info.Args = DisassembleValue(0, srcReg, 1, /* ref */ pc) + ", D" + dstReg;
                break;
            case 1:
                info.Mnemonic = "subx.w";
                info.Args = DisassembleValue(0, srcReg, 2, /* ref */ pc) + ", D" + dstReg;
                break;
            case 2:
                info.Mnemonic = "subx.l";
                info.Args = DisassembleValue(0, srcReg, 4, /* ref */ pc) + ", D" + dstReg;
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void SUBX1() {
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;
        switch (size) {
            case 0: {
                if (srcReg == 7) {
                    A[srcReg].u32 -= 2;
                } else {
                    A[srcReg].u32--;
                }
                if (dstReg == 7) {
                    A[dstReg].u32 -= 2;
                } else {
                    A[dstReg].u32--;
                }
                int src = (int) ReadByte.apply(A[srcReg].s32);
                int dst = (int) ReadByte.apply(A[dstReg].s32);
                int res;
                res = dst - src - (X ? 1 : 0);
                N = ((res & 0x80) != 0);
                X = C = ((res & 0x100) != 0);
                V = ((((src ^ dst) & (res ^ dst)) & 0x80) != 0);
                res = res & 0xff;
                Z &= (res == 0);
                WriteByte.accept(A[dstReg].s32, (byte) res);
                pendingCycles -= 18;
                return;
            }
            case 1: {
                A[srcReg].u32 -= 2;
                int src = (int) ReadWord.apply(A[srcReg].s32);
                A[dstReg].u32 -= 2;
                int dst = (int) ReadWord.apply(A[dstReg].s32);
                int res;
                res = dst - src - (X ? 1 : 0);
                N = ((res & 0x8000) != 0);
                X = C = ((res & 0x1_0000) != 0);
                V = ((((src ^ dst) & (res ^ dst)) & 0x8000) != 0);
                res = res & 0xffff;
                Z &= (res == 0);
                WriteWord.accept(A[dstReg].s32, (short) res);
                pendingCycles -= 18;
                return;
            }
            case 2: {
                A[srcReg].u32 -= 4;
                int src = ReadLong.apply(A[srcReg].s32);
                A[dstReg].u32 -= 4;
                int dst = (int) ReadWord.apply(A[dstReg].s32);
                int res;
                res = dst - src - (X ? 1 : 0);
                N = ((res & 0x8000_0000) != 0);
                X = C = (((((src & res) | (~dst & (src | res))) >> 23) & 0x100) != 0);
                V = (((((src ^ dst) & (res ^ dst)) >> 24) & 0x80) != 0);
                Z &= (res == 0);
                WriteLong.accept(A[dstReg].s32, res);
                pendingCycles -= 30;
            }
        }
    }

    void SUBX1_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dstReg = (op >> 9) & 0x07;
        int size = (op >> 6) & 0x03;
        int srcReg = op & 0x07;

        switch (size) {
            case 0:
                info.Mnemonic = "subx.b";
                info.Args = DisassembleValue(4, srcReg, 1, /* ref */ pc) + ", " + DisassembleValue(4, dstReg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "subx.w";
                info.Args = DisassembleValue(4, srcReg, 2, /* ref */ pc) + ", " + DisassembleValue(4, dstReg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "subx.l";
                info.Args = DisassembleValue(4, srcReg, 4, /* ref */ pc) + ", " + DisassembleValue(4, dstReg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void CMP() {
        int dReg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte a = D[dReg].s8;
                byte b = ReadValueB(mode, reg);
                int result = a - b;
                N = (result & 0x80) != 0;
                Z = result == 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short a = D[dReg].s16;
                short b = ReadValueW(mode, reg);
                int result = a - b;
                N = (result & 0x8000) != 0;
                Z = result == 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int a = D[dReg].s32;
                int b = ReadValueL(mode, reg);
                long result = (long) a - (long) b;
                N = (result & 0x8000_0000) != 0;
                Z = (int) result == 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 6 + EACyclesL[mode][reg];
            }
        }
    }

    void CMP_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};

        int dReg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "cmp.b";
                info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc) + ", D" + dReg;
                break;
            case 1:
                info.Mnemonic = "cmp.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc) + ", D" + dReg;
                break;
            case 2:
                info.Mnemonic = "cmp.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc) + ", D" + dReg;
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void CMPA() {
        int aReg = (op >> 9) & 7;
        int size = (op >> 8) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // word
            {
                int a = A[aReg].s32;
                short b = ReadValueW(mode, reg);
                long result = a - b;
                N = (result & 0x8000_0000) != 0;
                Z = result == 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 6 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // long
            {
                int a = A[aReg].s32;
                int b = ReadValueL(mode, reg);
                long result = a - b;
                N = (result & 0x8000_0000) != 0;
                Z = result == 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 6 + EACyclesL[mode][reg];
            }
        }
    }

    void CMPA_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};

        int aReg = (op >> 9) & 7;
        int size = (op >> 8) & 1;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "cmpa.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc) + ", A" + aReg;
                break;
            case 1:
                info.Mnemonic = "cmpa.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc) + ", A" + aReg;
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void CMPM() {
        int axReg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int ayReg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte a = ReadByte.apply(A[axReg].s32);
                A[axReg].s32 += 1; // Does A7 stay word aligned???
                byte b = ReadByte.apply(A[ayReg].s32);
                A[ayReg].s32 += 1;
                int result = a - b;
                N = (result & 0x80) != 0;
                Z = (result & 0xff) == 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 12;
                return;
            }
            case 1: // word
            {
                short a = ReadWord.apply(A[axReg].s32);
                A[axReg].s32 += 2;
                short b = ReadWord.apply(A[ayReg].s32);
                A[ayReg].s32 += 2;
                int result = a - b;
                N = (result & 0x8000) != 0;
                Z = (result & 0xffff) == 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 12;
                return;
            }
            case 2: // long
            {
                int a = ReadLong.apply(A[axReg].s32);
                A[axReg].s32 += 4;
                int b = ReadLong.apply(A[ayReg].s32);
                A[ayReg].s32 += 4;
                long result = a - b;
                N = (result & 0x8000_0000) != 0;
                Z = (int) result == 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                pendingCycles -= 20;
            }
        }
    }

    void CMPM_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int axReg = (op >> 9) & 7;
        int size = (op >> 6) & 3;
        int ayReg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "cmpm.b";
                break;
            case 1:
                info.Mnemonic = "cmpm.w";
                break;
            case 2:
                info.Mnemonic = "cmpm.l";
                break;
        }
        info.Args = "(A%d)+, (A%d)+".formatted(ayReg, axReg);
        info.Length = pc - info.PC;
    }

    void CMPI() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0: // byte
            {
                byte b = (byte) (short) ReadOpWord.apply(PC);
                PC += 2;
                byte a = ReadValueB(mode, reg);
                int result = a - b;
                N = (result & 0x80) != 0;
                Z = result == 0;
                V = result > Byte.MAX_VALUE || result < Byte.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                if (mode == 0) pendingCycles -= 8;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 1: // word
            {
                short b = ReadOpWord.apply(PC);
                PC += 2;
                short a = ReadValueW(mode, reg);
                int result = a - b;
                N = (result & 0x8000) != 0;
                Z = result == 0;
                V = result > Short.MAX_VALUE || result < Short.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                if (mode == 0) pendingCycles -= 8;
                else pendingCycles -= 8 + EACyclesBW[mode][reg];
                return;
            }
            case 2: // long
            {
                int b = ReadOpLong.apply(PC);
                PC += 4;
                int a = ReadValueL(mode, reg);
                long result = a - b;
                N = (result & 0x8000_0000) != 0;
                Z = result == 0;
                V = result > Integer.MAX_VALUE || result < Integer.MIN_VALUE;
                C = ((a < b) ^ ((a ^ b) >= 0) == false);
                if (mode == 0) pendingCycles -= 14;
                else pendingCycles -= 12 + EACyclesL[mode][reg];
            }
        }
    }

    void CMPI_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        int immediate;

        switch (size) {
            case 0:
                immediate = (byte) (short) ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Mnemonic = "cmpi.b";
                info.Args = "$%X, %s".formatted((byte) immediate, DisassembleValue(mode, reg, 1, /* ref */ pc));
                break;
            case 1:
                immediate = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                info.Mnemonic = "cmpi.w";
                info.Args = "$%X, %s".formatted((short) immediate, DisassembleValue(mode, reg, 2, /* ref */ pc));
                break;
            case 2:
                immediate = ReadOpLong.apply(pc[0]);
                pc[0] += 4;
                info.Mnemonic = "cmpi.l";
                info.Args = "$%X, %s".formatted(immediate, DisassembleValue(mode, reg, 4, /* ref */ pc));
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void MULU() {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int result = D[dreg].u16 * ReadValueW(mode, reg);
        D[dreg].u32 = result;

        V = false;
        C = false;
        N = (result & 0x8000_0000) != 0;
        Z = result == 0;

        pendingCycles -= 54 + EACyclesBW[mode][reg];
    }

    void MULU_Disasm(DisassemblyInfo info) {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "mulu";
        info.Args = "%s, D%d".formatted(DisassembleValue(mode, reg, 2, /* ref */ pc), dreg);
        info.Length = pc[0] - info.PC;
    }

    void MULS() {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int result = D[dreg].s16 * ReadValueW(mode, reg);
        D[dreg].s32 = result;

        V = false;
        C = false;
        N = (result & 0x8000_0000) != 0;
        Z = result == 0;

        pendingCycles -= 54 + EACyclesBW[mode][reg];
    }

    void MULS_Disasm(DisassemblyInfo info) {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "muls";
        info.Args = "%s, D%d".formatted(DisassembleValue(mode, reg, 2, /* ref */ pc), dreg);
        info.Length = pc[0] - info.PC;
    }

    void DIVU() {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int source = ReadValueW(mode, reg);
        int dest = D[dreg].u32;

        if (source == 0) {
            TrapVector(5);
        } else {
            int quotient = dest / source;
            int remainder = dest % source;
            if (quotient < 0x1_0000) {
                Z = quotient == 0;
                N = (quotient & 0x8000) != 0;
                V = false;
                C = false;
                D[dreg].u32 = (quotient & 0xFFFF) | (remainder << 16);
            } else {
                V = true;
            }
        }
        pendingCycles -= 140 + EACyclesBW[mode][reg];
    }

    void DIVU_Disasm(DisassemblyInfo info) {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "divu";
        info.Args = "%s, D%d".formatted(DisassembleValue(mode, reg, 2, /* ref */ pc), dreg);
        info.Length = pc[0] - info.PC;
    }

    void DIVS() {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int source = ReadValueW(mode, reg);
        int dest = D[dreg].s32;

        if (source == 0) {
            TrapVector(5);
        } else {
            int quotient = dest / source;
            int remainder = dest % source;
            if (quotient == (short) quotient) {
                Z = quotient == 0;
                N = (quotient & 0x8000) != 0;
                V = false;
                C = false;
                D[dreg].s32 = (quotient & 0xFFFF) | (remainder << 16);
            } else {
                V = true;
            }
        }
        pendingCycles -= 158 + EACyclesBW[mode][reg];
    }

    void DIVS_Disasm(DisassemblyInfo info) {
        int dreg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "divs";
        info.Args = "%s, D%d".formatted(DisassembleValue(mode, reg, 2, /* ref */ pc), dreg);
        info.Length = pc[0] - info.PC;
    }

//#endregion

//#region ProgramFlow

    boolean TestCondition(int condition) {
        switch (condition) {
            case 0x00:
                return true;  // True
            case 0x01:
                return false;  // False
            case 0x02:
                return !C && !Z; // High (Unsigned)
            case 0x03:
                return C || Z;  // Less or Same (Unsigned)
            case 0x04:
                return !C;  // Carry Clear (High or Same)
            case 0x05:
                return C;  // Carry Set (Lower)
            case 0x06:
                return !Z;  // Not Equal
            case 0x07:
                return Z;  // Equal
            case 0x08:
                return !V;  // Overflow Clear
            case 0x09:
                return V;  // Overflow Set
            case 0x0A:
                return !N;  // Plus (Positive)
            case 0x0B:
                return N;  // Minus (Negative)
            case 0x0C:
                return N && V || !N && !V;  // Greater or Equal
            case 0x0D:
                return N && !V || !N && V;  // Less Than
            case 0x0E:
                return N && V && !Z || !N && !V && !Z; // Greater Than
            case 0x0F:
                return Z || N && !V || !N && V;  // Less or Equal
            default:
                throw new IllegalArgumentException("Invalid condition " + condition);
        }
    }

    String DisassembleCondition(int condition) {
        switch (condition) {
            case 0x00:
                return "t";  // True
            case 0x01:
                return "f";  // False
            case 0x02:
                return "hi"; // High (Unsigned)
            case 0x03:
                return "ls"; // Less or Same (Unsigned)
            case 0x04:
                return "cc"; // Carry Clear (High or Same)
            case 0x05:
                return "cs"; // Carry Set (Lower)
            case 0x06:
                return "ne"; // Not Equal
            case 0x07:
                return "eq"; // Equal
            case 0x08:
                return "vc"; // Overflow Clear
            case 0x09:
                return "vs"; // Overflow Set
            case 0x0A:
                return "pl"; // Plus (Positive)
            case 0x0B:
                return "mi"; // Minus (Negative)
            case 0x0C:
                return "ge"; // Greater or Equal
            case 0x0D:
                return "lt"; // Less Than
            case 0x0E:
                return "gt"; // Greater Than
            case 0x0F:
                return "le"; // Less or Equal
            default:
                return "??"; // Invalid condition
        }
    }

    void Bcc() // Branch on condition
    {
        byte displacement8 = (byte) op;
        int cond = (op >> 8) & 0x0F;

        if (TestCondition(cond) == true) {
            if (displacement8 != 0) {
                // use opcode-embedded displacement
                PC += displacement8;
                pendingCycles -= 10;
            } else {
                // use extension word displacement
                PC += ReadOpWord.apply(PC);
                pendingCycles -= 10;
            }
        } else { // false
            if (displacement8 != 0)
                pendingCycles -= 8;
            else {
                PC += 2;
                pendingCycles -= 12;
            }
        }
    }

    void Bcc_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        byte displacement8 = (byte) op;
        int cond = (op >> 8) & 0x0F;

        info.Mnemonic = "b" + DisassembleCondition(cond);
        if (displacement8 != 0) {
            info.Args = "$%X".formatted(pc + displacement8);
        } else {
            info.Args = "$%X".formatted(pc + ReadOpWord.apply(pc));
            pc += 2;
        }
        info.Length = pc - info.PC;
    }

    void BRA() {
        byte displacement8 = (byte) op;

        if (displacement8 != 0)
            PC += displacement8;
        else
            PC += ReadOpWord.apply(PC);
        if (PPC == PC) {
            pendingCycles = 0;
        }
        pendingCycles -= 10;
    }

    void BRA_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        info.Mnemonic = "bra";

        byte displacement8 = (byte) op;
        if (displacement8 != 0)
            info.Args = "$%X".formatted(pc + displacement8);
        else {
            info.Args = "$%X".formatted(pc + ReadOpWord.apply(pc));
            pc += 2;
        }
        info.Length = pc - info.PC;
    }

    void BSR() {
        byte displacement8 = (byte) op;

        A[7].s32 -= 4;
        if (displacement8 != 0) {
            // use embedded displacement
            WriteLong.accept(A[7].s32, PC);
            PC += displacement8;
        } else {
            // use extension word displacement
            WriteLong.accept(A[7].s32, PC + 2);
            PC += ReadOpWord.apply(PC);
        }
        pendingCycles -= 18;
    }

    void BSR_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        info.Mnemonic = "bsr";

        byte displacement8 = (byte) op;
        if (displacement8 != 0)
            info.Args = "$%X".formatted(pc + displacement8);
        else {
            info.Args = "$%X".formatted(pc + ReadOpWord.apply(pc));
            pc += 2;
        }
        info.Length = pc - info.PC;
    }

    void DBcc() {
        if (TestCondition((op >> 8) & 0x0F) == true) {
            PC += 2; // condition met, break out of loop
            pendingCycles -= 12;
        } else {
            int reg = op & 7;
            D[reg].u16--;

            if (D[reg].u16 == 0xFFFF) {
                PC += 2; // counter underflowed, break out of loop
                pendingCycles -= 14;
            } else {
                PC += ReadOpWord.apply(PC); // condition false and counter not exhausted, so branch.
                pendingCycles -= 10;
            }
        }
    }

    void DBcc_Disasm(DisassemblyInfo info) {
        int cond = (op >> 8) & 0x0F;
        info.Mnemonic = "db" + DisassembleCondition(cond);

        int pc = info.PC + 2;
        info.Args = "D%d, $%X".formatted(op & 7, pc + ReadWord.apply(pc));
        info.Length = 4;
    }

    void RTS() {
        PC = ReadLong.apply(A[7].s32);
        A[7].s32 += 4;
        pendingCycles -= 16;
    }

    void RTS_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "rts";
        info.Args = "";
    }

    void RTR() {
        short value = ReadWord.apply(A[7].s32);
        A[7].s32 += 2;
        CCR = value;
        PC = ReadLong.apply(A[7].s32);
        A[7].s32 += 4;
        pendingCycles -= 20;
    }

    void RTR_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "rtr";
        info.Args = "";
    }

    void RESET() {
        if (s) {
            pendingCycles -= 132;
        } else {
            TrapVector2(8);
        }
    }

    void RESET_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "reset";
        info.Args = "";
    }

    void RTE() {
        short newSR = ReadWord.apply(A[7].s32);
        A[7].s32 += 2;
        PC = ReadLong.apply(A[7].s32);
        A[7].s32 += 4;
        SR = newSR;
        pendingCycles -= 20;
    }

    void RTE_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "rte";
        info.Args = "";
    }

    void TAS() {
        int mode = (op >> 3) & 0x07;
        int reg = op & 0x07;
        byte result;
//        result = (byte) ReadValueB(mode, reg);
        result = PeekValueB(mode, reg);
        Z = (result == 0);
        N = ((result & 0x80) != 0);
        V = false;
        C = false;
//        if (mode == 0) {
//            //D[reg].u8 = (byte)(result | 0x80);
//        }
        WriteValueB(mode, reg, (byte) (result | 0x80));
        pendingCycles -= (mode == 0) ? 4 : 14 + EACyclesBW[mode][reg];
    }

    void TAS_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = op & 7;
        info.Mnemonic = "tas.b";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void TST() {
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        int value;
        switch (size) {
            case 0:
                value = ReadValueB(mode, reg);
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                N = (value & 0x80) != 0;
                break;
            case 1:
                value = ReadValueW(mode, reg);
                pendingCycles -= 4 + EACyclesBW[mode][reg];
                N = (value & 0x8000) != 0;
                break;
            default:
                value = ReadValueL(mode, reg);
                pendingCycles -= 4 + EACyclesL[mode][reg];
                N = (value & 0x8000_0000) != 0;
                break;
        }
        V = false;
        C = false;
        Z = (value == 0);
    }

    void TST_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int size = (op >> 6) & 3;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        switch (size) {
            case 0:
                info.Mnemonic = "tst.b";
                info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
                break;
            case 1:
                info.Mnemonic = "tst.w";
                info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc);
                break;
            case 2:
                info.Mnemonic = "tst.l";
                info.Args = DisassembleValue(mode, reg, 4, /* ref */ pc);
                break;
        }
        info.Length = pc[0] - info.PC;
    }

    void BTSTi() {
        int bit = ReadOpWord.apply(PC);
        PC += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            pendingCycles -= 10;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            Z = (ReadValueB(mode, reg) & mask) == 0;
            pendingCycles -= 8 + EACyclesBW[mode][reg];
        }
    }

    void BTSTi_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int bit = ReadOpWord.apply(pc[0]);
        pc[0] += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "btst";
        info.Args = "$%X, %s".formatted(bit, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BTSTr() {
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;
        int bit = D[dReg].s32;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            pendingCycles -= 6;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            Z = (ReadValueB(mode, reg) & mask) == 0;
            pendingCycles -= 4 + EACyclesBW[mode][reg];
        }
    }

    void BTSTr_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "btst";
        info.Args = "D%d, %s".formatted(dReg, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BCHGi() {
        int bit = ReadOpWord.apply(PC);
        PC += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            D[reg].s32 ^= mask;
            pendingCycles -= 12;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            byte value = PeekValueB(mode, reg);
            Z = (value & mask) == 0;
            value ^= (byte) mask;
            WriteValueB(mode, reg, value);
            pendingCycles -= 12 + EACyclesBW[mode][reg];
        }
    }

    void BCHGi_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int bit = ReadOpWord.apply(pc[0]);
        pc[0] += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "bchg";
        info.Args = "$%X, %s".formatted(bit, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BCHGr() {
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;
        int bit = D[dReg].s32;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            D[reg].s32 ^= mask;
            pendingCycles -= 8;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            byte value = PeekValueB(mode, reg);
            Z = (value & mask) == 0;
            value ^= (byte) mask;
            WriteValueB(mode, reg, value);
            pendingCycles -= 8 + EACyclesBW[mode][reg];
        }
    }

    void BCHGr_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "bchg";
        info.Args = "D%d, %s".formatted(dReg, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BCLRi() {
        int bit = ReadOpWord.apply(PC);
        PC += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            D[reg].s32 &= ~mask;
            pendingCycles -= 14;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            byte value = PeekValueB(mode, reg);
            Z = (value & mask) == 0;
            value &= (byte) ~mask;
            WriteValueB(mode, reg, value);
            pendingCycles -= 12 + EACyclesBW[mode][reg];
        }
    }

    void BCLRi_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int bit = ReadOpWord.apply(pc[0]);
        pc[0] += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "bclr";
        info.Args = "$%X, %s".formatted(bit, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BCLRr() {
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;
        int bit = D[dReg].s32;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            D[reg].s32 &= ~mask;
            pendingCycles -= 10;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            byte value = PeekValueB(mode, reg);
            Z = (value & mask) == 0;
            value &= (byte) ~mask;
            WriteValueB(mode, reg, value);
            pendingCycles -= 8 + EACyclesBW[mode][reg];
        }
    }

    void BCLRr_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "bclr";
        info.Args = "D%d, %s".formatted(dReg, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BSETi() {
        int bit = ReadOpWord.apply(PC);
        PC += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            D[reg].s32 |= mask;
            pendingCycles -= 12;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            byte value = PeekValueB(mode, reg);
            Z = (value & mask) == 0;
            value |= (byte) mask;
            WriteValueB(mode, reg, value);
            pendingCycles -= 12 + EACyclesBW[mode][reg];
        }
    }

    void BSETi_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int bit = ReadOpWord.apply(pc[0]);
        pc[0] += 2;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "bset";
        info.Args = "$%X, %s".formatted(bit, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void BSETr() {
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;
        int bit = D[dReg].s32;

        if (mode == 0) {
            bit &= 31;
            int mask = 1 << bit;
            Z = (D[reg].s32 & mask) == 0;
            D[reg].s32 |= mask;
            pendingCycles -= 8;
        } else {
            bit &= 7;
            int mask = 1 << bit;
            byte value = PeekValueB(mode, reg);
            Z = (value & mask) == 0;
            value |= (byte) mask;
            WriteValueB(mode, reg, value);
            pendingCycles -= 8 + EACyclesBW[mode][reg];
        }
    }

    void BSETr_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int dReg = (op >> 9) & 7;
        int mode = (op >> 3) & 7;
        int reg = op & 7;

        info.Mnemonic = "bset";
        info.Args = "D%d, %s".formatted(dReg, DisassembleValue(mode, reg, 1, /* ref */ pc));
        info.Length = pc[0] - info.PC;
    }

    void JMP() {
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        PC = ReadAddress(mode, reg);
        if (PPC == PC) {
            pendingCycles = 0;
        }
        switch (mode) {
            case 2:
                pendingCycles -= 8;
                break;
            case 5:
                pendingCycles -= 10;
                break;
            case 6:
                pendingCycles -= 14;
                break;
            case 7:
                switch (reg) {
                    case 0:
                        pendingCycles -= 10;
                        break;
                    case 1:
                        pendingCycles -= 12;
                        break;
                    case 2:
                        pendingCycles -= 10;
                        break;
                    case 3:
                        pendingCycles -= 14;
                        break;
                }
                break;
        }
    }

    void JMP_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        info.Mnemonic = "jmp";
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void JSR() {
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        int addr = ReadAddress(mode, reg);

        A[7].s32 -= 4;
        WriteLong.accept(A[7].s32, PC);
        PC = addr;

        switch (mode) {
            case 2:
                pendingCycles -= 16;
                break;
            case 5:
                pendingCycles -= 18;
                break;
            case 6:
                pendingCycles -= 22;
                break;
            case 7:
                switch (reg) {
                    case 0:
                        pendingCycles -= 18;
                        break;
                    case 1:
                        pendingCycles -= 20;
                        break;
                    case 2:
                        pendingCycles -= 18;
                        break;
                    case 3:
                        pendingCycles -= 22;
                        break;
                }
                break;
        }
    }

    void JSR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        info.Mnemonic = "jsr";
        info.Args = DisassembleAddress(mode, reg, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void LINK() {
        int reg = op & 7;
        A[7].s32 -= 4;
        short offset = ReadOpWord.apply(PC);
        PC += 2;
        WriteLong.accept(A[7].s32, A[reg].s32);
        A[reg].s32 = A[7].s32;
        A[7].s32 += offset;
        pendingCycles -= 16;
    }

    void LINK_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int reg = op & 7;
        info.Mnemonic = "link";
        info.Args = "A" + reg + ", " + DisassembleImmediate(2, /* ref */ pc); // TODO need a DisassembleSigned or something
        info.Length = pc[0] - info.PC;
    }

    void UNLK() {
        int reg = op & 7;
        A[7].s32 = A[reg].s32;
        A[reg].s32 = ReadLong.apply(A[7].s32);
        A[7].s32 += 4;
        pendingCycles -= 12;
    }

    void UNLK_Disasm(DisassemblyInfo info) {
        int reg = op & 7;
        info.Mnemonic = "unlk";
        info.Args = "A" + reg;
        info.Length = 2;
    }

    void NOP() {
        pendingCycles -= 4;
    }

    void NOP_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "nop";
    }

    void Scc() // Set on condition
    {
        int cond = (op >> 8) & 0x0F;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        if (TestCondition(cond) == true) {
            WriteValueB(mode, reg, (byte) -1);
            if (mode == 0) pendingCycles -= 6;
            else pendingCycles -= 8 + EACyclesBW[mode][reg];
        } else {
            WriteValueB(mode, reg, (byte) 0);
            if (mode == 0)
                pendingCycles -= 4;
            else
                pendingCycles -= 8 + EACyclesBW[mode][reg];
        }
    }

    void Scc_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int cond = (op >> 8) & 0x0F;
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

        info.Mnemonic = "s" + DisassembleCondition(cond);
        info.Args = DisassembleValue(mode, reg, 1, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

//#endregion

//#region Supervisor

    void MOVEtSR() {
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        if (s == false) {
            //throw new Exception("Write to SR when not in supervisor mode. supposed to trap or something...");
            TrapVector2(8);
        } else {
            SR = ReadValueW(mode, reg);
        }
        pendingCycles -= 12 + EACyclesBW[mode][reg];
    }

    void MOVEtSR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        info.Mnemonic = "move";
        info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc) + ", SR";
        info.Length = pc[0] - info.PC;
    }

    void MOVEfSR() {
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        WriteValueW(mode, reg, SR);
        pendingCycles -= (mode == 0) ? 6 : 8 + EACyclesBW[mode][reg];
    }

    void MOVEfSR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        info.Mnemonic = "move";
        info.Args = "SR, " + DisassembleValue(mode, reg, 2, /* ref */ pc);
        info.Length = pc[0] - info.PC;
    }

    void MOVEUSP() {
        int dir = (op >> 3) & 1;
        int reg = op & 7;
        if (s == false) {
            //throw new Exception("MOVE to USP when not supervisor. needs to trap");
            TrapVector2(8);
        } else {
            if (dir == 0) {
                usp = A[reg].s32;
            } else {
                A[reg].s32 = usp;
            }
        }
        pendingCycles -= 4;
    }

    void MOVEUSP_Disasm(DisassemblyInfo info) {
        int pc = info.PC + 2;
        int dir = (op >> 3) & 1;
        int reg = op & 7;
        info.Mnemonic = "move";
        info.Args = (dir == 0) ? ("A" + reg + ", USP") : ("USP, A" + reg);
        info.Length = pc - info.PC;
    }

    void ANDI_SR() {
        if (s == false)
            throw new IllegalStateException("trap!");
        SR &= ReadOpWord.apply(PC);
        PC += 2;
        pendingCycles -= 20;
    }

    void ANDI_SR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "andi";
        info.Args = DisassembleImmediate(2, /* ref */ pc) + ", SR";
        info.Length = pc[0] - info.PC;
    }

    void EORI_SR() {
        if (s == false)
            throw new IllegalStateException("trap!");
        SR ^= ReadOpWord.apply(PC);
        PC += 2;
        pendingCycles -= 20;
    }

    void EORI_SR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "eori";
        info.Args = DisassembleImmediate(2, /* ref */ pc) + ", SR";
        info.Length = pc[0] - info.PC;
    }

    void ORI_SR() {
        if (s == false)
            throw new IllegalStateException("trap!");
        SR |= ReadOpWord.apply(PC);
        PC += 2;
        pendingCycles -= 20;
    }

    void ORI_SR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        info.Mnemonic = "ori";
        info.Args = DisassembleImmediate(2, /* ref */ pc) + ", SR";
        info.Length = pc[0] - info.PC;
    }

    void MOVECCR() {
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;

//        short sr = (short) (SR & 0xFF00);
//        sr |= (byte) ReadValueB(mode, reg);
//        SR = (short) sr;
        short value = ReadValueW(mode, reg);
        CCR = value;
        pendingCycles -= 12 + EACyclesBW[mode][reg];
    }

    void MOVECCR_Disasm(DisassemblyInfo info) {
        int[] pc = new int[] {info.PC + 2};
        int mode = (op >> 3) & 7;
        int reg = (op >> 0) & 7;
        info.Mnemonic = "move";
        info.Args = DisassembleValue(mode, reg, 2, /* ref */ pc) + ", CCR";
        info.Length = pc[0] - info.PC;
    }

    void TRAP() {
        int vector = 32 + (op & 0x0F);
        TrapVector(vector);
        pendingCycles -= 4;
    }

    void TRAP_Disasm(DisassemblyInfo info) {
        info.Mnemonic = "trap";
        info.Args = "$%X".formatted(op & 0xF);
    }

    void TrapVector(int vector) {
        short sr = SR;  // capture current SR.
        s = true;  // switch to supervisor mode, if not already in it.
        A[7].s32 -= 4;  // Push PC on stack
        WriteLong.accept(A[7].s32, PC);
        A[7].s32 -= 2;  // Push SR on stack
        WriteWord.accept(A[7].s32, sr);
        PC = ReadLong.apply(vector * 4);  // Jump to vector
        pendingCycles -= CyclesException[vector];
    }

    void TrapVector2(int vector) {
        short sr = SR;  // capture current SR.
        s = true;  // switch to supervisor mode, if not already in it.
        A[7].s32 -= 4;  // Push PPC on stack
        WriteLong.accept(A[7].s32, PPC);
        A[7].s32 -= 2;  // Push SR on stack
        WriteWord.accept(A[7].s32, sr);
        PC = ReadLong.apply(vector * 4);  // Jump to vector
        pendingCycles -= CyclesException[vector];
    }

//#endregion

//#region Disassembler

    public static class DisassemblyInfo {

        public int PC;
        public String Mnemonic;
        public String Args;
        public String RawBytes;
        public int Length;

        @Override
        public String toString() {
            return "%1$6X: %4$-20s  %2$-8s %3$s".formatted(PC, Mnemonic, Args, RawBytes);
        }
    }

    static boolean isEqual(Runnable a, Runnable b) { // TODO works???
        return a.hashCode() == b.hashCode();
    }

    public DisassemblyInfo Disassemble(int pc) {
        var info = new DisassemblyInfo();
        info.Mnemonic = "UNKNOWN";
        info.PC = pc;
        info.Length = 2;
        op = ReadOpWord.apply(pc);

        if (isEqual(Opcodes[op], this::MOVE)) MOVE_Disasm(info); //
        else if (isEqual(Opcodes[op], this::MOVEA)) MOVEA_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVEQ)) MOVEQ_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVEM0)) MOVEM0_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVEM1)) MOVEM1_Disasm(info);
        else if (isEqual(Opcodes[op], this::LEA)) LEA_Disasm(info); //
        else if (isEqual(Opcodes[op], this::CLR)) CLR_Disasm(info);
        else if (isEqual(Opcodes[op], this::EXT)) EXT_Disasm(info);
        else if (isEqual(Opcodes[op], this::PEA)) PEA_Disasm(info);
        else if (isEqual(Opcodes[op], this::ANDI)) ANDI_Disasm(info);
        else if (isEqual(Opcodes[op], this::ANDI_CCR)) ANDI_CCR_Disasm(info);
        else if (isEqual(Opcodes[op], this::EORI)) EORI_Disasm(info);
        else if (isEqual(Opcodes[op], this::EORI_CCR)) EORI_CCR_Disasm(info);
        else if (isEqual(Opcodes[op], this::ORI)) ORI_Disasm(info);
        else if (isEqual(Opcodes[op], this::ORI_CCR)) ORI_CCR_Disasm(info);
        else if (isEqual(Opcodes[op], this::ASLd)) ASLd_Disasm(info);
        else if (isEqual(Opcodes[op], this::ASRd)) ASRd_Disasm(info);
        else if (isEqual(Opcodes[op], this::LSLd)) LSLd_Disasm(info);
        else if (isEqual(Opcodes[op], this::LSRd)) LSRd_Disasm(info);
        else if (isEqual(Opcodes[op], this::ROXLd)) ROXLd_Disasm(info);
        else if (isEqual(Opcodes[op], this::ROXRd)) ROXRd_Disasm(info);
        else if (isEqual(Opcodes[op], this::ROLd)) ROLd_Disasm(info);
        else if (isEqual(Opcodes[op], this::RORd)) RORd_Disasm(info);
        else if (isEqual(Opcodes[op], this::ASLd0)) ASLd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::ASRd0)) ASRd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::LSLd0)) LSLd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::LSRd0)) LSRd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::ROXLd0)) ROXLd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::ROXRd0)) ROXRd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::ROLd0)) ROLd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::RORd0)) RORd0_Disasm(info);
        else if (isEqual(Opcodes[op], this::SWAP)) SWAP_Disasm(info);
        else if (isEqual(Opcodes[op], this::AND0)) AND0_Disasm(info);
        else if (isEqual(Opcodes[op], this::AND1)) AND1_Disasm(info);
        else if (isEqual(Opcodes[op], this::EOR)) EOR_Disasm(info);
        else if (isEqual(Opcodes[op], this::OR0)) OR0_Disasm(info);
        else if (isEqual(Opcodes[op], this::OR1)) OR1_Disasm(info);
        else if (isEqual(Opcodes[op], this::NOT)) NOT_Disasm(info);
        else if (isEqual(Opcodes[op], this::NEG)) NEG_Disasm(info);
        else if (isEqual(Opcodes[op], this::JMP)) JMP_Disasm(info);
        else if (isEqual(Opcodes[op], this::JSR)) JSR_Disasm(info);
        else if (isEqual(Opcodes[op], this::Bcc)) Bcc_Disasm(info);
        else if (isEqual(Opcodes[op], this::BRA)) BRA_Disasm(info);
        else if (isEqual(Opcodes[op], this::BSR)) BSR_Disasm(info);
        else if (isEqual(Opcodes[op], this::DBcc)) DBcc_Disasm(info);
        else if (isEqual(Opcodes[op], this::Scc)) Scc_Disasm(info);
        else if (isEqual(Opcodes[op], this::RTE)) RTE_Disasm(info);
        else if (isEqual(Opcodes[op], this::RTS)) RTS_Disasm(info);
        else if (isEqual(Opcodes[op], this::RTR)) RTR_Disasm(info);
        else if (isEqual(Opcodes[op], this::TST)) TST_Disasm(info);
        else if (isEqual(Opcodes[op], this::BTSTi)) BTSTi_Disasm(info);
        else if (isEqual(Opcodes[op], this::BTSTr)) BTSTr_Disasm(info);
        else if (isEqual(Opcodes[op], this::BCHGi)) BCHGi_Disasm(info);
        else if (isEqual(Opcodes[op], this::BCHGr)) BCHGr_Disasm(info);
        else if (isEqual(Opcodes[op], this::BCLRi)) BCLRi_Disasm(info);
        else if (isEqual(Opcodes[op], this::BCLRr)) BCLRr_Disasm(info);
        else if (isEqual(Opcodes[op], this::BSETi)) BSETi_Disasm(info);
        else if (isEqual(Opcodes[op], this::BSETr)) BSETr_Disasm(info);
        else if (isEqual(Opcodes[op], this::LINK)) LINK_Disasm(info);
        else if (isEqual(Opcodes[op], this::UNLK)) UNLK_Disasm(info);
        else if (isEqual(Opcodes[op], this::RESET)) RESET_Disasm(info);
        else if (isEqual(Opcodes[op], this::NOP)) NOP_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADD0)) ADD_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADD1)) ADD_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADDA)) ADDA_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADDI)) ADDI_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADDQ)) ADDQ_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUB0)) SUB_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUB1)) SUB_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUBA)) SUBA_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUBI)) SUBI_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUBQ)) SUBQ_Disasm(info);
        else if (isEqual(Opcodes[op], this::CMP)) CMP_Disasm(info);
        else if (isEqual(Opcodes[op], this::CMPM)) CMPM_Disasm(info);
        else if (isEqual(Opcodes[op], this::CMPA)) CMPA_Disasm(info);
        else if (isEqual(Opcodes[op], this::CMPI)) CMPI_Disasm(info);
        else if (isEqual(Opcodes[op], this::MULU)) MULU_Disasm(info);
        else if (isEqual(Opcodes[op], this::MULS)) MULS_Disasm(info);
        else if (isEqual(Opcodes[op], this::DIVU)) DIVU_Disasm(info);
        else if (isEqual(Opcodes[op], this::DIVS)) DIVS_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVEtSR)) MOVEtSR_Disasm(info); //
        else if (isEqual(Opcodes[op], this::MOVEfSR)) MOVEfSR_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVEUSP)) MOVEUSP_Disasm(info);
        else if (isEqual(Opcodes[op], this::ANDI_SR)) ANDI_SR_Disasm(info);
        else if (isEqual(Opcodes[op], this::EORI_SR)) EORI_SR_Disasm(info);
        else if (isEqual(Opcodes[op], this::ORI_SR)) ORI_SR_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVECCR)) MOVECCR_Disasm(info);
        else if (isEqual(Opcodes[op], this::TRAP)) TRAP_Disasm(info);
        else if (isEqual(Opcodes[op], this::NBCD)) NBCD_Disasm(info);
        else if (isEqual(Opcodes[op], this::ILLEGAL)) ILLEGAL_Disasm(info);
        else if (isEqual(Opcodes[op], this::STOP)) STOP_Disasm(info);
        else if (isEqual(Opcodes[op], this::TRAPV)) TRAPV_Disasm(info);
        else if (isEqual(Opcodes[op], this::CHK)) CHK_Disasm(info);
        else if (isEqual(Opcodes[op], this::NEGX)) NEGX_Disasm(info);
        else if (isEqual(Opcodes[op], this::SBCD0)) SBCD0_Disasm(info);
        else if (isEqual(Opcodes[op], this::SBCD1)) SBCD1_Disasm(info);
        else if (isEqual(Opcodes[op], this::ABCD0)) ABCD0_Disasm(info);
        else if (isEqual(Opcodes[op], this::ABCD1)) ABCD1_Disasm(info);
        else if (isEqual(Opcodes[op], this::EXGdd)) EXGdd_Disasm(info);
        else if (isEqual(Opcodes[op], this::EXGaa)) EXGaa_Disasm(info);
        else if (isEqual(Opcodes[op], this::EXGda)) EXGda_Disasm(info);
        else if (isEqual(Opcodes[op], this::TAS)) TAS_Disasm(info);
        else if (isEqual(Opcodes[op], this::MOVEP)) MOVEP_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADDX0)) ADDX0_Disasm(info);
        else if (isEqual(Opcodes[op], this::ADDX1)) ADDX1_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUBX0)) SUBX0_Disasm(info);
        else if (isEqual(Opcodes[op], this::SUBX1)) SUBX1_Disasm(info);
        else if (isEqual(Opcodes[op], this::ILL)) ILL_Disasm(info);

        var sb = new StringBuilder();
        for (int p = info.PC; p < info.PC + info.Length; p += 2) {
            sb.append("%4X ".formatted(ReadOpWord.apply(p)));
        }
        info.RawBytes = sb.toString();
        return info;
    }

//#endregion

//#region Memory

    byte ReadValueB(int mode, int reg) {
        byte value;
        switch (mode) {
            case 0: // Dn
                return D[reg].s8;
            case 1: // An
                return A[reg].s8;
            case 2: // (An)
                return ReadByte.apply(A[reg].s32);
            case 3: // (An)+
                value = ReadByte.apply(A[reg].s32);
                A[reg].s32 += reg == 7 ? 2 : 1;
                return value;
            case 4: // -(An)
                A[reg].s32 -= reg == 7 ? 2 : 1;
                return ReadByte.apply(A[reg].s32);
            case 5: // (d16,An)
                value = ReadByte.apply((A[reg].s32 + ReadOpWord.apply(PC)));
                PC += 2;
                return value;
            case 6: // (d8,An,Xn)
                return ReadByte.apply(A[reg].s32 + GetIndex());
            case 7:
                switch (reg) {
                    case 0: // (imm).W
                        value = ReadByte.apply((int) (short) ReadOpWord.apply(PC));
                        PC += 2;
                        return value;
                    case 1: // (imm).L
                        value = ReadByte.apply(ReadOpLong.apply(PC));
                        PC += 4;
                        return value;
                    case 2: // (d16,PC)
                        value = ReadOpByte.apply(PC + ReadOpWord.apply(PC));
                        PC += 2;
                        return value;
                    case 3: // (d8,PC,Xn)
                        int pc = PC;
                        value = ReadOpByte.apply((pc + GetIndex()));
                        return value;
                    case 4: // immediate
                        value = (byte) (short) ReadOpWord.apply(PC);
                        PC += 2;
                        return value;
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    short ReadValueW(int mode, int reg) {
        short value;
        switch (mode) {
            case 0: // Dn
                return D[reg].s16;
            case 1: // An
                return A[reg].s16;
            case 2: // (An)
                return ReadWord.apply(A[reg].s32);
            case 3: // (An)+
                value = ReadWord.apply(A[reg].s32);
                A[reg].s32 += 2;
                return value;
            case 4: // -(An)
                A[reg].s32 -= 2;
                return ReadWord.apply(A[reg].s32);
            case 5: // (d16,An)
                value = ReadWord.apply((A[reg].s32 + ReadOpWord.apply(PC)));
                PC += 2;
                return value;
            case 6: // (d8,An,Xn)
                return ReadWord.apply(A[reg].s32 + GetIndex());
            case 7:
                switch (reg) {
                    case 0: // (imm).W
                        value = ReadWord.apply((int) (short) ReadOpWord.apply(PC));
                        PC += 2;
                        return value;
                    case 1: // (imm).L
                        value = ReadWord.apply(ReadOpLong.apply(PC));
                        PC += 4;
                        return value;
                    case 2: // (d16,PC)
                        value = ReadOpWord.apply(PC + ReadOpWord.apply(PC));
                        PC += 2;
                        return value;
                    case 3: // (d8,PC,Xn)
                        int pc = PC;
                        value = ReadOpWord.apply((pc + GetIndex()));
                        return value;
                    case 4: // immediate
                        value = ReadOpWord.apply(PC);
                        PC += 2;
                        return value;
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    int ReadValueL(int mode, int reg) {
        int value;
        switch (mode) {
            case 0: // Dn
                return D[reg].s32;
            case 1: // An
                return A[reg].s32;
            case 2: // (An)
                return ReadLong.apply(A[reg].s32);
            case 3: // (An)+
                value = ReadLong.apply(A[reg].s32);
                A[reg].s32 += 4;
                return value;
            case 4: // -(An)
                A[reg].s32 -= 4;
                return ReadLong.apply(A[reg].s32);
            case 5: // (d16,An)
                value = ReadLong.apply((A[reg].s32 + ReadOpWord.apply(PC)));
                PC += 2;
                return value;
            case 6: // (d8,An,Xn)
                return ReadLong.apply(A[reg].s32 + GetIndex());
            case 7:
                switch (reg) {
                    case 0: // (imm).W
                        value = ReadLong.apply((int) (short) ReadOpWord.apply(PC));
                        PC += 2;
                        return value;
                    case 1: // (imm).L
                        value = ReadLong.apply(ReadOpLong.apply(PC));
                        PC += 4;
                        return value;
                    case 2: // (d16,PC)
                        value = ReadOpLong.apply(PC + ReadOpWord.apply(PC));
                        PC += 2;
                        return value;
                    case 3: // (d8,PC,Xn)
                        int pc = PC;
                        value = ReadOpLong.apply((pc + GetIndex()));
                        return value;
                    case 4: // immediate
                        value = ReadOpLong.apply(PC);
                        PC += 4;
                        return value;
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    byte PeekValueB(int mode, int reg) {
        byte value;
        switch (mode) {
            case 0: // Dn
                return D[reg].s8;
            case 1: // An
                return A[reg].s8;
            case 2: // (An)
                return ReadByte.apply(A[reg].s32);
            case 3: // (An)+
                value = ReadByte.apply(A[reg].s32);
                return value;
            case 4: // -(An)
                value = ReadByte.apply(A[reg].s32 - (reg == 7 ? 2 : 1));
                return value;
            case 5: // (d16,An)
                value = ReadByte.apply((A[reg].s32 + ReadOpWord.apply(PC)));
                return value;
            case 6: // (d8,An,Xn)
                return ReadByte.apply(A[reg].s32 + PeekIndex());
            case 7:
                switch (reg) {
                    case 0: // (imm).W
                        value = ReadByte.apply((int) (short) ReadOpWord.apply(PC));
                        return value;
                    case 1: // (imm).L
                        value = ReadByte.apply(ReadOpLong.apply(PC));
                        return value;
                    case 2: // (d16,PC)
                        value = ReadByte.apply(PC + ReadOpWord.apply(PC));
                        return value;
                    case 3: // (d8,PC,Xn)
                        value = ReadByte.apply((PC + PeekIndex()));
                        return value;
                    case 4: // immediate
                        return (byte) (short) ReadOpWord.apply(PC);
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    short PeekValueW(int mode, int reg) {
        short value;
        switch (mode) {
            case 0: // Dn
                return D[reg].s16;
            case 1: // An
                return A[reg].s16;
            case 2: // (An)
                return ReadWord.apply(A[reg].s32);
            case 3: // (An)+
                value = ReadWord.apply(A[reg].s32);
                return value;
            case 4: // -(An)
                value = ReadWord.apply(A[reg].s32 - 2);
                return value;
            case 5: // (d16,An)
                value = ReadWord.apply((A[reg].s32 + ReadOpWord.apply(PC)));
                return value;
            case 6: // (d8,An,Xn)
                return ReadWord.apply(A[reg].s32 + PeekIndex());
            case 7:
                switch (reg) {
                    case 0: // (imm).W
                        value = ReadWord.apply((int) (short) ReadOpWord.apply(PC));
                        return value;
                    case 1: // (imm).L
                        value = ReadWord.apply(ReadOpLong.apply(PC));
                        return value;
                    case 2: // (d16,PC)
                        value = ReadWord.apply(PC + ReadOpWord.apply(PC));
                        return value;
                    case 3: // (d8,PC,Xn)
                        value = ReadWord.apply((PC + PeekIndex()));
                        return value;
                    case 4: // immediate
                        return ReadOpWord.apply(PC);
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    int PeekValueL(int mode, int reg) {
        int value;
        switch (mode) {
            case 0: // Dn
                return D[reg].s32;
            case 1: // An
                return A[reg].s32;
            case 2: // (An)
                return ReadLong.apply(A[reg].s32);
            case 3: // (An)+
                value = ReadLong.apply(A[reg].s32);
                return value;
            case 4: // -(An)
                value = ReadLong.apply(A[reg].s32 - 4);
                return value;
            case 5: // (d16,An)
                value = ReadLong.apply((A[reg].s32 + ReadOpWord.apply(PC)));
                return value;
            case 6: // (d8,An,Xn)
                return ReadLong.apply(A[reg].s32 + PeekIndex());
            case 7:
                switch (reg) {
                    case 0: // (imm).W
                        value = ReadLong.apply((int) (short) ReadOpWord.apply(PC));
                        return value;
                    case 1: // (imm).L
                        value = ReadLong.apply(ReadOpLong.apply(PC));
                        return value;
                    case 2: // (d16,PC)
                        value = ReadLong.apply(PC + ReadOpWord.apply(PC));
                        return value;
                    case 3: // (d8,PC,Xn)
                        value = ReadLong.apply((PC + PeekIndex()));
                        return value;
                    case 4: // immediate
                        return ReadOpLong.apply(PC);
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    int ReadAddress(int mode, int reg) {
        int addr;
        switch (mode) {
            case 0:
                throw new IllegalArgumentException("Invalid addressing mode!"); // Dn
            case 1:
                throw new IllegalArgumentException("Invalid addressing mode!"); // An
            case 2:
                return A[reg].s32; // (An)
            case 3:
                return A[reg].s32; // (An)+
            case 4:
                return A[reg].s32; // -(An)
            case 5:
                addr = A[reg].s32 + ReadOpWord.apply(PC);
                PC += 2;
                return addr; // (d16,An)
            case 6:
                return A[reg].s32 + GetIndex(); // (d8,An,Xn)
            case 7:
                switch (reg) {
                    case 0:
                        addr = ReadOpWord.apply(PC);
                        PC += 2;
                        return addr; // (imm).w
                    case 1:
                        addr = ReadOpLong.apply(PC);
                        PC += 4;
                        return addr; // (imm).l
                    case 2:
                        addr = PC;
                        addr += ReadOpWord.apply(PC);
                        PC += 2;
                        return addr; // (d16,PC)
                    case 3:
                        addr = PC;
                        addr += GetIndex();
                        return addr; // (d8,PC,Xn)
                    case 4:
                        throw new IllegalArgumentException("Invalid addressing mode!"); // immediate
                }
                break;
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    String DisassembleValue(int mode, int reg, int size, /* ref */ int[] pc) {
        String value;
        int addr;
        switch (mode) {
            case 0:
                return "D" + reg;  // Dn
            case 1:
                return "A" + reg;  // An
            case 2:
                return "(A" + reg + ")";  // (An)
            case 3:
                return "(A" + reg + ")+"; // (An)+
            case 4:
                return "-(A" + reg + ")"; // -(An)
            case 5:
                value = "($%X,A%d)".formatted(ReadOpWord.apply(pc[0]), reg);
                pc[0] += 2;
                return value; // (d16,An)
            case 6:
                addr = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                return DisassembleIndex("A" + reg, (short) addr); // (d8,An,Xn)
            case 7:
                switch (reg) {
                    case 0:
                        value = "$%X".formatted(ReadOpWord.apply(pc[0]));
                        pc[0] += 2;
                        return value; // (imm).W
                    case 1:
                        value = "$%X".formatted(ReadOpLong.apply(pc[0]));
                        pc[0] += 4;
                        return value; // (imm).L
                    case 2:
                        value = "$%X".formatted(pc[0] + ReadOpWord.apply(pc[0]));
                        pc[0] += 2;
                        return value; // (d16,PC)
                    case 3:
                        addr = ReadOpWord.apply(pc[0]);
                        pc[0] += 2;
                        return DisassembleIndex("PC", (short) addr); // (d8,PC,Xn)
                    case 4:
                        switch (size) {
                            case 1:
                                value = "$%X".formatted((byte) (short) ReadOpWord.apply(pc[0]));
                                pc[0] += 2;
                                return value;
                            case 2:
                                value = "$%X".formatted(ReadOpWord.apply(pc[0]));
                                pc[0] += 2;
                                return value;
                            case 4:
                                value = "$%X".formatted(ReadOpLong.apply(pc[0]));
                                pc[0] += 4;
                                return value;
                        }
                        break;
                }
                break;
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    String DisassembleImmediate(int size, /* ref */ int[] pc) {
        int immed;
        switch (size) {
            case 1:
                immed = (byte) (short) ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                return "$%X".formatted(immed);
            case 2:
                immed = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                return "$%X".formatted(immed);
            case 4:
                immed = ReadOpLong.apply(pc[0]);
                pc[0] += 4;
                return "$%X".formatted(immed);
        }
        throw new IllegalArgumentException("Invalid size");
    }

    String DisassembleAddress(int mode, int reg, /* ref */ int[] pc) {
        int addr;
        switch (mode) {
            case 0:
                return "INVALID"; // Dn
            case 1:
                return "INVALID"; // An
            case 2:
                return "(A" + reg + ")"; // (An)
            case 3:
                return "(A" + reg + ")+"; // (An)+
            case 4:
                return "-(A" + reg + ")"; // -(An)
            case 5:
                addr = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                return "($%X,A%d)".formatted((short) addr, reg); // (d16,An)
            case 6:
                addr = ReadOpWord.apply(pc[0]);
                pc[0] += 2;
                return DisassembleIndex("A" + reg, (short) addr); // (d8,An,Xn)
            case 7:
                switch (reg) {
                    case 0:
                        addr = ReadOpWord.apply(pc[0]);
                        pc[0] += 2;
                        return "$%X.w".formatted(addr); // (imm).w
                    case 1:
                        addr = ReadOpLong.apply(pc[0]);
                        pc[0] += 4;
                        return "$%X.l".formatted(addr); // (imm).l
                    case 2:
                        addr = ReadOpWord.apply(pc[0]);
                        pc[0] += 2;
                        return "($%X,PC)".formatted(addr); // (d16,PC)
                    case 3:
                        addr = ReadOpWord.apply(pc[0]);
                        pc[0] += 2;
                        return DisassembleIndex("PC", (short) addr); // (d8,PC,Xn)
                    case 4:
                        return "INVALID"; // immediate
                }
                break;
        }
        throw new IllegalArgumentException("Invalid addressing mode!");
    }

    void WriteValueB(int mode, int reg, byte value) {
        switch (mode) {
            case 0x00: // Dn
                D[reg].s8 = value;
                return;
            case 0x01: // An
                A[reg].s32 = value;
                return;
            case 0x02: // (An)
                WriteByte.accept(A[reg].s32, value);
                return;
            case 0x03: // (An)+
                WriteByte.accept(A[reg].s32, value);
                A[reg].s32 += reg == 7 ? 2 : 1;
                return;
            case 0x04: // -(An)
                A[reg].s32 -= reg == 7 ? 2 : 1;
                WriteByte.accept(A[reg].s32, value);
                return;
            case 0x05: // (d16,An)
                WriteByte.accept(A[reg].s32 + ReadOpWord.apply(PC), value);
                PC += 2;
                return;
            case 0x06: // (d8,An,Xn)
                WriteByte.accept(A[reg].s32 + GetIndex(), value);
                return;
            case 0x07:
                switch (reg) {
                    case 0x00: // (imm).W
                        WriteByte.accept((int) (short) ReadOpWord.apply(PC), value);
                        PC += 2;
                        return;
                    case 0x01: // (imm).L
                        WriteByte.accept(ReadOpLong.apply(PC), value);
                        PC += 4;
                        return;
                    case 0x02: // (d16,PC)
                        WriteByte.accept(PC + ReadOpWord.apply(PC), value);
                        PC += 2;
                        return;
                    case 0x03: // (d8,PC,Xn)
                        int pc = PC;
                        WriteByte.accept(pc + PeekIndex(), value);
                        PC += 2;
                        return;
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
    }

    void WriteValueW(int mode, int reg, short value) {
        switch (mode) {
            case 0x00: // Dn
                D[reg].s16 = value;
                return;
            case 0x01: // An
                A[reg].s32 = value;
                return;
            case 0x02: // (An)
                WriteWord.accept(A[reg].s32, value);
                return;
            case 0x03: // (An)+
                WriteWord.accept(A[reg].s32, value);
                A[reg].s32 += 2;
                return;
            case 0x04: // -(An)
                A[reg].s32 -= 2;
                WriteWord.accept(A[reg].s32, value);
                return;
            case 0x05: // (d16,An)
                WriteWord.accept(A[reg].s32 + ReadOpWord.apply(PC), value);
                PC += 2;
                return;
            case 0x06: // (d8,An,Xn)
                WriteWord.accept(A[reg].s32 + GetIndex(), value);
                return;
            case 0x07:
                switch (reg) {
                    case 0x00: // (imm).W
                        WriteWord.accept((int) (short) ReadOpWord.apply(PC), value);
                        PC += 2;
                        return;
                    case 0x01: // (imm).L
                        WriteWord.accept(ReadOpLong.apply(PC), value);
                        PC += 4;
                        return;
                    case 0x02: // (d16,PC)
                        WriteWord.accept(PC + ReadOpWord.apply(PC), value);
                        PC += 2;
                        return;
                    case 0x03: // (d8,PC,Xn)
                        int pc = PC;
                        WriteWord.accept(pc + PeekIndex(), value);
                        PC += 2;
                        return;
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
    }

    void WriteValueL(int mode, int reg, int value) {
        switch (mode) {
            case 0x00: // Dn
                D[reg].s32 = value;
                return;
            case 0x01: // An
                A[reg].s32 = value;
                return;
            case 0x02: // (An)
                WriteLong.accept(A[reg].s32, value);
                return;
            case 0x03: // (An)+
                WriteLong.accept(A[reg].s32, value);
                A[reg].s32 += 4;
                return;
            case 0x04: // -(An)
                A[reg].s32 -= 4;
                WriteLong.accept(A[reg].s32, value);
                return;
            case 0x05: // (d16,An)
                WriteLong.accept(A[reg].s32 + ReadOpWord.apply(PC), value);
                PC += 2;
                return;
            case 0x06: // (d8,An,Xn)
                WriteLong.accept(A[reg].s32 + GetIndex(), value);
                return;
            case 0x07:
                switch (reg) {
                    case 0x00: // (imm).W
                        WriteLong.accept((int) (short) ReadOpWord.apply(PC), value);
                        PC += 2;
                        return;
                    case 0x01: // (imm).L
                        WriteLong.accept(ReadOpLong.apply(PC), value);
                        PC += 4;
                        return;
                    case 0x02: // (d16,PC)
                        WriteLong.accept(PC + ReadOpWord.apply(PC), value);
                        PC += 2;
                        return;
                    case 0x03: // (d8,PC,Xn)
                        int pc = PC;
                        WriteLong.accept(pc + PeekIndex(), value);
                        PC += 2;
                        return;
                    default:
                        throw new IllegalArgumentException("Invalid addressing mode!");
                }
        }
    }

    int GetIndex() {
        //logger.log(Level.TRACE, "IN INDEX PORTION - NOT VERIFIED!!!");
        // TODO kid chameleon triggers this in startup sequence

        short extension = ReadOpWord.apply(PC);
        PC += 2;

        int da = (extension >> 15) & 0x1;
        int reg = (extension >> 12) & 0x7;
        int size = (extension >> 11) & 0x1;
        int scale = (extension >> 9) & 0x3;
        byte displacement = (byte) extension;

        int indexReg;
        switch (scale) {
            case 0:
                indexReg = 1;
                break;
            case 1:
                indexReg = 2;
                break;
            case 2:
                indexReg = 4;
                break;
            default:
                indexReg = 8;
                break;
        }
        if (da == 0)
            indexReg *= size == 0 ? D[reg].s16 : D[reg].s32;
        else
            indexReg *= size == 0 ? A[reg].s16 : A[reg].s32;

        return displacement + indexReg;
    }

    int PeekIndex() {
        //logger.log(Level.TRACE, "IN INDEX PORTION - NOT VERIFIED!!!");

        short extension = ReadOpWord.apply(PC);

        int da = (extension >> 15) & 0x1;
        int reg = (extension >> 12) & 0x7;
        int size = (extension >> 11) & 0x1;
        int scale = (extension >> 9) & 0x3;
        byte displacement = (byte) extension;

        int indexReg;
        switch (scale) {
            case 0:
                indexReg = 1;
                break;
            case 1:
                indexReg = 2;
                break;
            case 2:
                indexReg = 4;
                break;
            default:
                indexReg = 8;
                break;
        }
        if (da == 0)
            indexReg *= size == 0 ? D[reg].s16 : D[reg].s32;
        else
            indexReg *= size == 0 ? A[reg].s16 : A[reg].s32;

        return displacement + indexReg;
    }

    String DisassembleIndex(String baseRegister, short extension) {
        int d_a = (extension >> 15) & 0x1;
        int reg = (extension >> 12) & 0x7;
        int size = (extension >> 11) & 0x1;
        int scale = (extension >> 9) & 0x3;
        byte displacement = (byte) extension;

        String scaleFactor;
        switch (scale) {
            case 0:
                scaleFactor = "";
                break;
            case 1:
                scaleFactor = "2";
                break;
            case 2:
                scaleFactor = "4";
                break;
            default:
                scaleFactor = "8";
                break;
        }

        String offsetRegister = (d_a == 0) ? "D" : "A";
        String sizeStr = size == 0 ? ".w" : ".l";
        String displacementStr = displacement == 0 ? "" : ("," + (displacement >= 0 ? "$" + "%X".formatted(displacement) : "-$" + "%X".formatted(-displacement)));
        return "(%s,%s%s%d%s%s)".formatted(baseRegister, scaleFactor, offsetRegister, reg, sizeStr, displacementStr);
    }

//#endregion

//#region OpcodeTable

    void BuildOpcodeTable() {
        Assign("ill", this::ILL, "", "Data8", "Data8");//
        Assign("ori2ccr", this::ORI_CCR, "0000000000111100");//
        Assign("ori2sr", this::ORI_SR, "0000000001111100");
        Assign("ori", this::ORI, "00000000", "Size2_1", "OAmXn");
        Assign("andi2ccr", this::ANDI_CCR, "0000001000111100");//
        Assign("andi2sr", this::ANDI_SR, "0000001001111100");
        Assign("andi", this::ANDI, "00000010", "Size2_1", "OAmXn");
        Assign("subi", this::SUBI, "00000100", "Size2_1", "OAmXn");
        Assign("addi", this::ADDI, "00000110", "Size2_1", "OAmXn");
        Assign("eori2ccr", this::EORI_CCR, "0000101000111100");//
        Assign("eori2sr", this::EORI_SR, "0000101001111100");
        Assign("eori", this::EORI, "00001010", "Size2_1", "OAmXn");
        Assign("cmpi", this::CMPI, "00001100", "Size2_1", "OAmXn");
        Assign("btst", this::BTSTi, "0000100000", "BAmXn");
        Assign("bchg", this::BCHGi, "0000100001", "OAmXn");
        Assign("bclr", this::BCLRi, "0000100010", "OAmXn");
        Assign("bset", this::BSETi, "0000100011", "OAmXn");
        Assign("btst", this::BTSTr, "0000", "Xn", "100", "AmXn");
        Assign("bchg", this::BCHGr, "0000", "Xn", "101", "OAmXn");
        Assign("bclr", this::BCLRr, "0000", "Xn", "110", "OAmXn");
        Assign("bset", this::BSETr, "0000", "Xn", "111", "OAmXn");
        Assign("movep", this::MOVEP, "0000", "Xn", "1", "Data1", "Size1", "001", "Xn");//
        Assign("movea", this::MOVEA, "00", "Size2_2", "Xn", "001", "AmXn");
        Assign("move", this::MOVE, "00", "01", "OXnAm", "MAmXn");
        Assign("move", this::MOVE, "00", "Size2_2", "OXnAm", "AmXn");
        Assign("movefsr", this::MOVEfSR, "0100000011", "OAmXn");
        Assign("moveccr", this::MOVECCR, "0100010011", "MAmXn");
        Assign("move2sr", this::MOVEtSR, "0100011011", "MAmXn");
        Assign("negx", this::NEGX, "01000000", "Size2_1", "OAmXn");//
        Assign("clr", this::CLR, "01000010", "Size2_1", "OAmXn");
        Assign("neg", this::NEG, "01000100", "Size2_1", "OAmXn");
        Assign("not", this::NOT, "01000110", "Size2_1", "OAmXn");
        Assign("ext", this::EXT, "010010001", "Size1", "000", "Xn");
        Assign("nbcd", this::NBCD, "0100100000", "OAmXn");//
        Assign("swap", this::SWAP, "0100100001000", "Xn");
        Assign("pea", this::PEA, "0100100001", "LAmXn");
        Assign("illegal", this::ILLEGAL, "0100101011111100");//
        Assign("tas", this::TAS, "0100101011", "OAmXn");//
        Assign("tst", this::TST, "01001010", "Size2_1", "OAmXn");
        Assign("trap", this::TRAP, "010011100100", "Data4");
        Assign("link", this::LINK, "0100111001010", "Xn");
        Assign("unlk", this::UNLK, "0100111001011", "Xn");
        Assign("moveusp", this::MOVEUSP, "010011100110", "Data1", "Xn");
        Assign("reset", this::RESET, "0100111001110000");//
        Assign("nop", this::NOP, "0100111001110001");
        Assign("stop", this::STOP, "0100111001110010");//
        Assign("rte", this::RTE, "0100111001110011");
        Assign("rts", this::RTS, "0100111001110101");
        Assign("trapv", this::TRAPV, "0100111001110110");//
        Assign("rtr", this::RTR, "0100111001110111");
        Assign("jsr", this::JSR, "0100111010", "LAmXn");
        Assign("jmp", this::JMP, "0100111011", "LAmXn");
        Assign("movem", this::MOVEM0, "010010001", "Size1", "M2AmXn");
        Assign("movem", this::MOVEM1, "010011001", "Size1", "M3AmXn");
        Assign("lea", this::LEA, "0100", "Xn", "111", "LAmXn");
        Assign("chk", this::CHK, "0100", "Xn", "110", "MAmXn");//
        Assign("addq", this::ADDQ, "0101", "Data3", "0", "00", "OAmXn");
        Assign("addq", this::ADDQ, "0101", "Data3", "0", "Size2_3", "A2AmXn");
        Assign("subq", this::SUBQ, "0101", "Data3", "1", "00", "OAmXn");
        Assign("subq", this::SUBQ, "0101", "Data3", "1", "Size2_3", "A2AmXn");
        Assign("scc", this::Scc, "0101", "CondAll", "11", "OAmXn");
        Assign("dbcc", this::DBcc, "0101", "CondAll", "11001", "Xn");
        Assign("bra", this::BRA, "01100000", "Data8");
        Assign("bsr", this::BSR, "01100001", "Data8");
        Assign("bcc", this::Bcc, "0110", "CondMain", "Data8");
        Assign("moveq", this::MOVEQ, "0111", "Xn", "0", "Data8");
        Assign("divu", this::DIVU, "1000", "Xn", "011", "MAmXn");
        Assign("divs", this::DIVS, "1000", "Xn", "111", "MAmXn");
        Assign("sbcd", this::SBCD0, "1000", "Xn", "100000", "Xn");//
        Assign("sbcd", this::SBCD1, "1000", "Xn", "100001", "Xn");//
        Assign("or", this::OR0, "1000", "Xn", "0", "Size2_1", "MAmXn");
        Assign("or", this::OR1, "1000", "Xn", "1", "Size2_1", "O2AmXn");
        Assign("sub", this::SUB0, "1001", "Xn", "0", "00", "MAmXn");
        Assign("sub", this::SUB0, "1001", "Xn", "0", "Size2_3", "AmXn");
        Assign("sub", this::SUB1, "1001", "Xn", "1", "00", "O2AmXn");
        Assign("sub", this::SUB1, "1001", "Xn", "1", "Size2_3", "A2AmXn");
        Assign("subx", this::SUBX0, "1001", "Xn", "1", "Size2_1", "000", "Xn"); //
        Assign("subx", this::SUBX1, "1001", "Xn", "1", "Size2_1", "001", "Xn"); //
        Assign("suba", this::SUBA, "1001", "Xn", "Size1", "11", "AmXn");
        Assign("eor", this::EOR, "1011", "Xn", "1", "Size2_1", "OAmXn");
        Assign("cmpm", this::CMPM, "1011", "Xn", "1", "Size2_1", "001", "Xn");
        Assign("cmp", this::CMP, "1011", "Xn", "0", "00", "MAmXn");
        Assign("cmp", this::CMP, "1011", "Xn", "0", "Size2_3", "AmXn");
        Assign("cmpa", this::CMPA, "1011", "Xn", "Size1", "11", "AmXn");
        Assign("mulu", this::MULU, "1100", "Xn", "011", "MAmXn");
        Assign("muls", this::MULS, "1100", "Xn", "111", "MAmXn");
        Assign("abcd", this::ABCD0, "1100", "Xn", "100000", "Xn"); //
        Assign("abcd", this::ABCD1, "1100", "Xn", "100001", "Xn"); //
        Assign("exg", this::EXGdd, "1100", "Xn", "101000", "Xn"); //
        Assign("exg", this::EXGaa, "1100", "Xn", "101001", "Xn"); //
        Assign("exg", this::EXGda, "1100", "Xn", "110001", "Xn"); //
        Assign("and", this::AND0, "1100", "Xn", "0", "Size2_1", "MAmXn");
        Assign("and", this::AND1, "1100", "Xn", "1", "Size2_1", "O2AmXn");
        Assign("add", this::ADD0, "1101", "Xn", "0", "00", "MAmXn");
        Assign("add", this::ADD0, "1101", "Xn", "0", "Size2_3", "AmXn");
        Assign("add", this::ADD1, "1101", "Xn", "1", "Size2_1", "O2AmXn");
        Assign("addx", this::ADDX0, "1101", "Xn", "1", "Size2_1", "000", "Xn"); //
        Assign("addx", this::ADDX1, "1101", "Xn", "1", "Size2_1", "001", "Xn"); //
        Assign("adda", this::ADDA, "1101", "Xn", "Size1", "11", "AmXn");
        Assign("asl", this::ASLd0, "1110000111", "O2AmXn");//
        Assign("asr", this::ASRd0, "1110000011", "O2AmXn");//
        Assign("lsl", this::LSLd0, "1110001111", "O2AmXn");//
        Assign("lsr", this::LSRd0, "1110001011", "O2AmXn");//
        Assign("roxl", this::ROXLd0, "1110010111", "O2AmXn");//
        Assign("roxr", this::ROXRd0, "1110010011", "O2AmXn");//
        Assign("rol", this::ROLd0, "1110011111", "O2AmXn");//
        Assign("ror", this::RORd0, "1110011011", "O2AmXn");//
        Assign("asl", this::ASLd, "1110", "Data3", "1", "Size2_1", "Data1", "00", "Xn");
        Assign("asr", this::ASRd, "1110", "Data3", "0", "Size2_1", "Data1", "00", "Xn");
        Assign("lsl", this::LSLd, "1110", "Data3", "1", "Size2_1", "Data1", "01", "Xn");
        Assign("lsr", this::LSRd, "1110", "Data3", "0", "Size2_1", "Data1", "01", "Xn");
        Assign("roxl", this::ROXLd, "1110", "Data3", "1", "Size2_1", "Data1", "10", "Xn");
        Assign("roxr", this::ROXRd, "1110", "Data3", "0", "Size2_1", "Data1", "10", "Xn");
        Assign("rol", this::ROLd, "1110", "Data3", "1", "Size2_1", "Data1", "11", "Xn");
        Assign("ror", this::RORd, "1110", "Data3", "0", "Size2_1", "Data1", "11", "Xn");
    }

    void Assign(String instr, Runnable exec, String root, /* params */ String... bitfield) {
        List<String> opList = new ArrayList<>();
        opList.add(root);
        for (var component : bitfield) {
            if (IsBinary(component)) AppendConstant(opList, component);
            else if (component.equals("Size1")) opList = AppendPermutations(opList, Size1);
            else if (component.equals("Size2_0")) opList = AppendPermutations(opList, Size2_0);
            else if (component.equals("Size2_1")) opList = AppendPermutations(opList, Size2_1);
            else if (component.equals("Size2_2")) opList = AppendPermutations(opList, Size2_2);
            else if (component.equals("Size2_3")) opList = AppendPermutations(opList, Size2_3);
            else if (component.equals("OXnAm")) opList = AppendPermutations(opList, OXn3Am3);  // 0,2-6,7_0-7_1
            else if (component.equals("AmXn")) opList = AppendPermutations(opList, Am3Xn3);  // 0-6,7_0-7_4
            else if (component.equals("OAmXn")) opList = AppendPermutations(opList, OAm3Xn3);  // 0,2-6,7_0-7_1
            else if (component.equals("BAmXn")) opList = AppendPermutations(opList, BAm3Xn3);  // 0,2-6,7_0-7_3
            else if (component.equals("MAmXn")) opList = AppendPermutations(opList, MAm3Xn3);  // 0,2-6,7_0-7_4
            else if (component.equals("AAmXn")) opList = AppendPermutations(opList, AAm3Xn3);  // 1-6,7_0-7_4
            else if (component.equals("LAmXn")) opList = AppendPermutations(opList, LAm3Xn3);  // 2,5-6,7_0-7_3
            else if (component.equals("M2AmXn")) opList = AppendPermutations(opList, M2Am3Xn3); // 2,4-6,7_0-7_1
            else if (component.equals("M3AmXn")) opList = AppendPermutations(opList, M3Am3Xn3); // 2-3,5-6,7_0-7_3
            else if (component.equals("A2AmXn")) opList = AppendPermutations(opList, A2Am3Xn3); // 0-6,7_0-7_1
            else if (component.equals("O2AmXn")) opList = AppendPermutations(opList, O2Am3Xn3); // 2-6,7_0-7_1
            else if (component.equals("Xn")) opList = AppendPermutations(opList, Xn3);
            else if (component.equals("CondMain")) opList = AppendPermutations(opList, ConditionMain);
            else if (component.equals("CondAll")) opList = AppendPermutations(opList, ConditionAll);
            else if (component.equals("Data1")) opList = AppendData(opList, 1);
            else if (component.equals("Data3")) opList = AppendData(opList, 3);
            else if (component.equals("Data4")) opList = AppendData(opList, 4);
            else if (component.equals("Data8")) opList = AppendData(opList, 8);
        }
        for (var opcode : opList) {
            int opc = Integer.parseInt(opcode, 2);
            Opcodes[opc] = exec;
        }
    }

    void AppendConstant(List<String> ops, String constant) {
        ops.replaceAll(string -> string + constant);
    }

    List<String> AppendPermutations(List<String> ops, String[] permutations) {
        List<String> output = new ArrayList<>();

        for (var input : ops)
            for (var perm : permutations)
                output.add(input + perm);

        return output;
    }

    List<String> AppendData(List<String> ops, int bits) {
        List<String> output = new ArrayList<>();

        for (var input : ops)
            for (int i = 0; i < BinaryExp(bits); i++)
                output.add(input + ("%0" + bits + "d").formatted(Integer.parseInt(Integer.toBinaryString(i))));

        return output;
    }

    int BinaryExp(int bits) {
        int res = 1;
        for (int i = 0; i < bits; i++)
            res *= 2;
        return res;
    }

    boolean IsBinary(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '0' || c == '1')
                continue;
            return false;
        }
        return true;
    }

//#region Tables

    static final String[] Size2_0 = {"01", "11", "10"};
    static final String[] Size2_1 = {"00", "01", "10"};
    static final String[] Size2_2 = {"11", "10"};
    static final String[] Size2_3 = {"01", "10"};
    static final String[] Size1 = {"0", "1"};
    static final String[] Xn3 = {"000", "001", "010", "011", "100", "101", "110", "111"};

    static final String[] OXn3Am3 = {
            "000000", // Dn   Data register
            "001000",
            "010000",
            "011000",
            "100000",
            "101000",
            "110000",
            "111000",

            "000010", // (An) Address
            "001010",
            "010010",
            "011010",
            "100010",
            "101010",
            "110010",
            "111010",

            "000011", // (An)+ Address with Postincrement
            "001011",
            "010011",
            "011011",
            "100011",
            "101011",
            "110011",
            "111011",

            "000100", // -(An) Address with Predecrement
            "001100",
            "010100",
            "011100",
            "100100",
            "101100",
            "110100",
            "111100",

            "000101", // (d16, An) Address with Displacement
            "001101",
            "010101",
            "011101",
            "100101",
            "101101",
            "110101",
            "111101",

            "000110", // (d8, An, Xn) Address with Index
            "001110",
            "010110",
            "011110",
            "100110",
            "101110",
            "110110",
            "111110",

            "000111", // (xxx).W       Absolute Short
            "001111", // (xxx).L       Absolute Long
    };

    static final String[] Am3Xn3 = {
            "000000", // Dn   Data register
            "000001",
            "000010",
            "000011",
            "000100",
            "000101",
            "000110",
            "000111",

            "001000", // An    Address register
            "001001",
            "001010",
            "001011",
            "001100",
            "001101",
            "001110",
            "001111",

            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111010", // (d16, PC)     PC with Displacement
            "111011", // (d8, PC, Xn)  PC with Index
            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
            "111100", // #imm          Immediate
    };

    static final String[] OAm3Xn3 = {
            "000000", // Dn   Data register
            "000001",
            "000010",
            "000011",
            "000100",
            "000101",
            "000110",
            "000111",

            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] BAm3Xn3 = {
            "000000", // Dn   Data register
            "000001",
            "000010",
            "000011",
            "000100",
            "000101",
            "000110",
            "000111",

            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111010", // (d16, PC)     PC with Displacement
            "111011", // (d8, PC, Xn)  PC with Index
            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] MAm3Xn3 = {
            "000000", // Dn   Data register
            "000001",
            "000010",
            "000011",
            "000100",
            "000101",
            "000110",
            "000111",

            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111010", // (d16, PC)     PC with Displacement
            "111011", // (d8, PC, Xn)  PC with Index
            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
            "111100", // #imm          Immediate
    };

    static final String[] AAm3Xn3 = {
            "001000", // An    Address register
            "001001",
            "001010",
            "001011",
            "001100",
            "001101",
            "001110",
            "001111",

            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111010", // (d16, PC)     PC with Displacement
            "111011", // (d8, PC, Xn)  PC with Index
            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
            "111100", // #imm          Immediate
    };

    static final String[] LAm3Xn3 = {
            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111010", // (d16, PC)     PC with Displacement
            "111011", // (d8, PC, Xn)  PC with Index
            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] M2Am3Xn3 = {
            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] M3Am3Xn3 = {
            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111010", // (d16, PC)     PC with Displacement
            "111011", // (d8, PC, Xn)  PC with Index
            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] A2Am3Xn3 = {
            "000000", // Dn   Data register
            "000001",
            "000010",
            "000011",
            "000100",
            "000101",
            "000110",
            "000111",

            "001000", // An    Address register
            "001001",
            "001010",
            "001011",
            "001100",
            "001101",
            "001110",
            "001111",

            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] O2Am3Xn3 = {
            "010000", // (An) Address
            "010001",
            "010010",
            "010011",
            "010100",
            "010101",
            "010110",
            "010111",

            "011000", // (An)+ Address with Postincrement
            "011001",
            "011010",
            "011011",
            "011100",
            "011101",
            "011110",
            "011111",

            "100000", // -(An) Address with Predecrement
            "100001",
            "100010",
            "100011",
            "100100",
            "100101",
            "100110",
            "100111",

            "101000", // (d16, An) Address with Displacement
            "101001",
            "101010",
            "101011",
            "101100",
            "101101",
            "101110",
            "101111",

            "110000", // (d8, An, Xn) Address with Index
            "110001",
            "110010",
            "110011",
            "110100",
            "110101",
            "110110",
            "110111",

            "111000", // (xxx).W       Absolute Short
            "111001", // (xxx).L       Absolute Long
    };

    static final String[] ConditionMain = {
            "0010", // HI  Higher (unsigned)
            "0011", // LS  Lower or Same (unsigned)
            "0100", // CC  Carry Clear (aka Higher or Same, unsigned)
            "0101", // CS  Carry Set (aka Lower, unsigned)
            "0110", // NE  Not Equal
            "0111", // EQ  Equal
            "1000", // VC  Overflow Clear
            "1001", // VS  Overflow Set
            "1010", // PL  Plus
            "1011", // MI  Minus
            "1100", // GE  Greater or Equal (signed)
            "1101", // LT  Less Than (signed)
            "1110", // GT  Greater Than (signed)
            "1111"  // LE  Less or Equal (signed)
    };

    static final String[] ConditionAll = {
            "0000", // T   True
            "0001", // F   False
            "0010", // HI  Higher (unsigned)
            "0011", // LS  Lower or Same (unsigned)
            "0100", // CC  Carry Clear (aka Higher or Same, unsigned)
            "0101", // CS  Carry Set (aka Lower, unsigned)
            "0110", // NE  Not Equal
            "0111", // EQ  Equal
            "1000", // VC  Overflow Clear
            "1001", // VS  Overflow Set
            "1010", // PL  Plus
            "1011", // MI  Minus
            "1100", // GE  Greater or Equal (signed)
            "1101", // LT  Less Than (signed)
            "1110", // GT  Greater Than (signed)
            "1111"  // LE  Less or Equal (signed)
    };

//#endregion

//#endregion

//#region Tables

    static final int[][] MoveCyclesBW = {
            {4, 4, 8, 8, 8, 12, 14, 12, 16},
            {4, 4, 8, 8, 8, 12, 14, 12, 16},
            {8, 8, 12, 12, 12, 16, 18, 16, 20},
            {8, 8, 12, 12, 12, 16, 18, 16, 20},
            {10, 10, 14, 14, 14, 18, 20, 18, 22},
            {12, 12, 16, 16, 16, 20, 22, 20, 24},
            {14, 14, 18, 18, 18, 22, 24, 22, 26},
            {12, 12, 16, 16, 16, 20, 22, 20, 24},
            {16, 16, 20, 20, 20, 24, 26, 24, 28},
            {12, 12, 16, 16, 16, 20, 22, 20, 24},
            {14, 14, 18, 18, 18, 22, 24, 22, 26},
            {8, 8, 12, 12, 12, 16, 18, 16, 20}
    };

    static final int[][] MoveCyclesL = {
            {4, 4, 12, 12, 12, 16, 18, 16, 20},
            {4, 4, 12, 12, 12, 16, 18, 16, 20},
            {12, 12, 20, 20, 20, 24, 26, 24, 28},
            {12, 12, 20, 20, 20, 24, 26, 24, 28},
            {14, 14, 22, 22, 22, 26, 28, 26, 30},
            {16, 16, 24, 24, 24, 28, 30, 28, 32},
            {18, 18, 26, 26, 26, 30, 32, 30, 34},
            {16, 16, 24, 24, 24, 28, 30, 28, 32},
            {20, 20, 28, 28, 28, 32, 34, 32, 36},
            {16, 16, 24, 24, 24, 28, 30, 28, 32},
            {18, 18, 26, 26, 26, 30, 32, 30, 34},
            {12, 12, 20, 20, 20, 24, 26, 24, 28}
    };

    static final int[][] EACyclesBW = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {6, 6, 6, 6, 6, 6, 6, 6},
            {8, 8, 8, 8, 8, 8, 8, 8},
            {10, 10, 10, 10, 10, 10, 10, 10},
            {8, 12, 8, 10, 4, 99, 99, 99}
    };

    static final int[][] EACyclesL = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {8, 8, 8, 8, 8, 8, 8, 8},
            {8, 8, 8, 8, 8, 8, 8, 8},
            {10, 10, 10, 10, 10, 10, 10, 10},
            {12, 12, 12, 12, 12, 12, 12, 12},
            {14, 14, 14, 14, 14, 14, 14, 14},
            {12, 16, 12, 14, 8, 99, 99, 99}
    };

    static final int[] CyclesException = {
            0x04, 0x04, 0x32, 0x32, 0x22, 0x26, 0x28, 0x22,
            0x22, 0x22, 0x04, 0x04, 0x04, 0x04, 0x04, 0x2C,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x2C, 0x2C, 0x2C, 0x2C, 0x2C, 0x2C, 0x2C, 0x2C,
            0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
            0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22
    };

//#endregion
}
