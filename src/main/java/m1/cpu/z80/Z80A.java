/*
 * This Z80 emulator is a modified version of Ben Ryves 'Brazil' emulator.
 * It is MIT licensed.
 */

package m1.cpu.z80;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Cpuexec;
import m1.emu.Cpuexec.cpuexec_data;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Cpuint.irq;
import m1.emu.Timer;

import static java.lang.System.getLogger;
import static m1.cpu.z80.Tables.InitialiseTables;
import static m1.cpu.z80.Tables.IsS;
import static m1.cpu.z80.Tables.IsX;
import static m1.cpu.z80.Tables.IsY;
import static m1.cpu.z80.Tables.TableALU;
import static m1.cpu.z80.Tables.TableDaa;
import static m1.cpu.z80.Tables.TableDec;
import static m1.cpu.z80.Tables.TableHalfBorrow;
import static m1.cpu.z80.Tables.TableInc;
import static m1.cpu.z80.Tables.TableNeg;
import static m1.cpu.z80.Tables.TableParity;
import static m1.cpu.z80.Tables.TableRotShift;
import static m1.cpu.z80.Tables.cc_ex;
import static m1.cpu.z80.Tables.cc_op;


/**
 * ZiLOG Z80A CPU Emulator
 */
public class Z80A extends cpuexec_data {

    private static final Logger logger = getLogger(Z80A.class.getName());

    public static Z80A[] zz1;
    public static int nZ80;
    private boolean Interruptable;
    private long totalExecutedCycles;
    private int pendingCycles;

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

    /**
     * Creates an instance of the {@link Z80A} emulator class.
     */
    public Z80A() {
        InitialiseTables();
        // Clear main registers
        PPC = 0;
        RegAF = new RegisterPair((short) 0x0040);
        RegBC = new RegisterPair((short) 0);
        RegDE = new RegisterPair((short) 0);
        RegHL = new RegisterPair((short) 0);
        // Clear alternate registers
        RegAltAF = new RegisterPair((short) 0);
        RegAltBC = new RegisterPair((short) 0);
        RegAltDE = new RegisterPair((short) 0);
        RegAltHL = new RegisterPair((short) 0);
        // Clear special purpose registers
        RegI = 0;
        RegR = 0;
        RegR2 = 0;
        RegIX.Word = (short) 0xffff;
        RegIY.Word = (short) 0xffff;
        RegSP.Word = 0;
        RegPC.Word = 0;
        RegWZ.Word = 0;
        iff1 = iff2 = false;
        halted = false;
        interruptMode = 0;
    }

    /**
     * Reset the Z80 to its initial state
     */
    @Override
    public void Reset() {
        ResetRegisters();
        ResetInterrupts();
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        if (irqline == LineState.INPUT_LINE_NMI.ordinal()) {
            if (!nonMaskableInterrupt && state != LineState.CLEAR_LINE)
                nonMaskableInterruptPending = true;
            nonMaskableInterrupt = (state != LineState.CLEAR_LINE);
        } else {
            interrupt = (state != LineState.CLEAR_LINE);
        }
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        Atime time1;
        time1 = Timer.getCurrentTime();
        boolean b1 = false;
        for (irq irq1 : Cpuint.lirq) {
            if (irq1.cpunum == cpunum && irq1.line == line) {
                if (Attotime.attotime_compare(irq1.time, time1) > 0) {
                    b1 = true;
                    break;
                } else {
                    int i1 = 1;
                }
            }
        }
        if (b1) {
            int i1 = 1;
        } else {
            Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
        }
    }

    // Memory Access

    public Function<Short, Byte> ReadOp, ReadOpArg;
    public Function<Short, Byte> ReadMemory;
    public BiConsumer<Short, Byte> WriteMemory;

    public interface debug_delegate extends Runnable {

    }

    public debug_delegate debugger_start_cpu_hook_callback, debugger_stop_cpu_hook_callback;

    public void UnregisterMemoryMapper() {
        ReadMemory = null;
        WriteMemory = null;
    }

    // Hardware I/O Port Access

    public Function<Short, Byte> ReadHardware;
    public BiConsumer<Short, Byte> WriteHardware;

    // State Save/Load

    public void SaveStateBinary(BinaryWriter writer) {
        writer.write(PPC);
        writer.write(getRegisterAF());
        writer.write(getRegisterBC());
        writer.write(getRegisterDE());
        writer.write(getRegisterHL());
        writer.write(getRegisterShadowAF());
        writer.write(getRegisterShadowBC());
        writer.write(getRegisterShadowDE());
        writer.write(getRegisterShadowHL());
        writer.write(getRegisterIX());
        writer.write(getRegisterIY());
        writer.write(getRegisterSP());
        writer.write(getRegisterPC());
        writer.write(getRegisterWZ());
        writer.write(getRegisterI());
        writer.write(getRegisterR());
        writer.write(getRegisterR2());
        writer.write(interrupt);
        writer.write(nonMaskableInterrupt);
        writer.write(nonMaskableInterruptPending);
        writer.write(interruptMode);
        writer.write(iff1);
        writer.write(iff2);
        writer.write(halted);
        writer.write(totalExecutedCycles);
        writer.write(pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        PPC = reader.readUInt16();
        setRegisterAF(reader.readUInt16());
        setRegisterBC(reader.readUInt16());
        setRegisterDE(reader.readUInt16());
        setRegisterHL(reader.readUInt16());
        setRegisterShadowAF(reader.readUInt16());
        setRegisterShadowBC(reader.readUInt16());
        setRegisterShadowDE(reader.readUInt16());
        setRegisterShadowHL(reader.readUInt16());
        setRegisterIX(reader.readUInt16());
        setRegisterIY(reader.readUInt16());
        setRegisterSP(reader.readUInt16());
        setRegisterPC(reader.readUInt16());
        setRegisterWZ(reader.readUInt16());
        setRegisterI(reader.readByte());
        setRegisterR(reader.readByte());
        setRegisterR2(reader.readByte());
        interrupt = reader.readBoolean();
        nonMaskableInterrupt = reader.readBoolean();
        nonMaskableInterruptPending = reader.readBoolean();
        interruptMode = reader.readInt32();
        iff1 = reader.readBoolean();
        iff2 = reader.readBoolean();
        halted = reader.readBoolean();
        totalExecutedCycles = reader.readUInt64();
        pendingCycles = reader.readInt32();
    }

    public void SaveStateText(PrintWriter writer) {
        writer.printf("[Z80]");
        writer.printf("AF %4X", RegAF.Word);
        writer.printf("BC %4X", RegBC.Word);
        writer.printf("DE %4X", RegDE.Word);
        writer.printf("HL %4X", RegHL.Word);
        writer.printf("ShadowAF %4X", RegAltAF.Word);
        writer.printf("ShadowBC %4X", RegAltBC.Word);
        writer.printf("ShadowDE %4X", RegAltDE.Word);
        writer.printf("ShadowHL %4X", RegAltHL.Word);
        writer.printf("I %2X", RegI);
        writer.printf("R %2X", RegR);
        writer.printf("IX %4X", RegIX.Word);
        writer.printf("IY %4X", RegIY.Word);
        writer.printf("SP %4X", RegSP.Word);
        writer.printf("PC %4X", RegPC.Word);
        writer.printf("IRQ %s", interrupt);
        writer.printf("NMI %s", nonMaskableInterrupt);
        writer.printf("NMIPending %s", nonMaskableInterruptPending);
        writer.printf("IM %d", interruptMode);
        writer.printf("IFF1 %s", iff1);
        writer.printf("IFF2 %s", iff2);
        writer.printf("Halted %s", halted);
        writer.printf("ExecutedCycles %d", totalExecutedCycles);
        writer.printf("PendingCycles %d", pendingCycles);
        writer.printf("[/Z80]");
        writer.println();
    }

    public void LoadStateText(Scanner reader) {
        while (reader.hasNextLine()) {
            String[] args = reader.nextLine().split(" ");
            if (args[0].trim().isEmpty()) continue;
            if (args[0].equals("[/Z80]")) break;
            if (args[0].equals("AF"))
                RegAF.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("BC"))
                RegBC.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("DE"))
                RegDE.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("HL"))
                RegHL.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("ShadowAF"))
                RegAltAF.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("ShadowBC"))
                RegAltBC.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("ShadowDE"))
                RegAltDE.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("ShadowHL"))
                RegAltHL.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("I"))
                RegI = Byte.parseByte(args[1], 16);
            else if (args[0].equals("R"))
                RegR = Byte.parseByte(args[1], 16);
            else if (args[0].equals("IX"))
                RegIX.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("IY"))
                RegIY.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("SP"))
                RegSP.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("PC"))
                RegPC.Word = Short.parseShort(args[1], 16);
            else if (args[0].equals("IRQ"))
                interrupt = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("NMI"))
                nonMaskableInterrupt = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("NMIPending"))
                nonMaskableInterruptPending = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("IM"))
                interruptMode = Integer.parseInt(args[1]);
            else if (args[0].equals("IFF1"))
                iff1 = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("IFF2"))
                iff2 = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("Halted"))
                halted = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("ExecutedCycles"))
                totalExecutedCycles = Long.parseLong(args[1]);
            else if (args[0].equals("PendingCycles"))
                pendingCycles = Integer.parseInt(args[1]);

            else
                logger.log(Level.DEBUG, "Skipping unrecognized identifier " + args[0]);
        }
    }

//#region Registers

    //@StructLayout(LayoutKind.Explicit)
    public static class RegisterPair implements Serializable {

        //@FieldOffset(0)
        public short Word;

        //@FieldOffset(0)
        public byte Low;

        //@FieldOffset(1)
        public byte High;

        public RegisterPair(short value) {
            Word = value;
            Low = (byte) (Word);
            High = (byte) (Word >> 8);
        }
    }

    public short getPC() {
        return RegPC.Word;
    }

    public boolean getRegFlagC() {
        return (RegAF.Low & 0x01) != 0;
    }

    public void setRegFlagC(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x01) | (value ? 0x01 : 0x00));
    }

    public boolean getRegFlagN() {
        return (RegAF.Low & 0x02) != 0;
    }

    public void setRegFlagN(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x02) | (value ? 0x02 : 0x00));
    }

    public boolean getRegFlagP() {
        return (RegAF.Low & 0x04) != 0;
    }

    public void setRegFlagP(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x04) | (value ? 0x04 : 0x00));
    }

    public boolean getRegFlag3() {
        return (RegAF.Low & 0x08) != 0;
    }

    public void setRegFlag3(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x08) | (value ? 0x08 : 0x00));
    }

    public boolean getRegFlagH() {
        return (RegAF.Low & 0x10) != 0;
    }

    public void setRegFlagH(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x10) | (value ? 0x10 : 0x00));
    }

    public boolean getRegFlag5() {
        return (RegAF.Low & 0x20) != 0;
    }

    public void setRegFlag5(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x20) | (value ? 0x20 : 0x00));
    }

    public boolean getRegFlagZ() {
        return (RegAF.Low & 0x40) != 0;
    }

    public void setRegFlagZ(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x40) | (value ? 0x40 : 0x00));
    }

    public boolean getRegFlagS() {
        return (RegAF.Low & 0x80) != 0;
    }

    public void setRegFlagS(boolean value) {
        RegAF.Low = (byte) ((RegAF.Low & ~0x80) | (value ? 0x80 : 0x00));
    }

    private final RegisterPair RegAF;
    private final RegisterPair RegBC;
    private final RegisterPair RegDE;
    private final RegisterPair RegHL;

    private final RegisterPair RegAltAF; // Shadow for A and F
    private final RegisterPair RegAltBC; // Shadow for B and C
    private final RegisterPair RegAltDE; // Shadow for D and E
    private final RegisterPair RegAltHL; // Shadow for H and L

    private byte RegI; // I (interrupt vector)
    private byte RegR; // R (memory refresh)

    private byte RegR2;

    private RegisterPair RegIX = new RegisterPair((short) 0); // IX (index register x)
    private RegisterPair RegIY = new RegisterPair((short) 0); // IY (index register y)
    private RegisterPair RegWZ = new RegisterPair((short) 0); // WZ

    private RegisterPair RegSP = new RegisterPair((short) 0); // SP (stack pointer)
    private RegisterPair RegPC = new RegisterPair((short) 0); // PC (program counter)

    private void ResetRegisters() {
        RegI = 0;
        RegR = 0;
        RegR2 = 0;
        RegPC.Word = 0;
        RegWZ.Word = 0;
    }

    public byte getRegisterA() {
        return RegAF.High;
    }

    public void setRegisterA(byte value) {
        RegAF.High = value;
    }

    public byte getRegisterF() {
        return RegAF.Low;
    }

    public void setRegisterF(byte value) {
        RegAF.Low = value;
    }

    public short getRegisterAF() {
        return RegAF.Word;
    }

    public void setRegisterAF(short value) {
        RegAF.Word = value;
    }

    public byte getRegisterB() {
        return RegBC.High;
    }

    public void setRegisterB(byte value) {
        RegBC.High = value;
    }

    public byte getRegisterC() {
        return RegBC.Low;
    }

    public void setRegisterC(byte value) {
        RegBC.Low = value;
    }

    public short getRegisterBC() {
        return RegBC.Word;
    }

    public void setRegisterBC(short value) {
        RegBC.Word = value;
    }

    public byte getRegisterD() {
        return RegDE.High;
    }

    public void setRegisterD(byte value) {
        RegDE.High = value;
    }

    public byte getRegisterE() {
        return RegDE.Low;
    }

    public void setRegisterE(byte value) {
        RegDE.Low = value;
    }

    public short getRegisterDE() {
        return RegDE.Word;
    }

    public void setRegisterDE(short value) {
        RegDE.Word = value;
    }

    public byte getRegisterH() {
        return RegHL.High;
    }

    public void setRegisterH(byte value) {
        RegHL.High = value;
    }

    public byte getRegisterL() {
        return RegHL.Low;
    }

    public void setRegisterL(byte value) {
        RegHL.Low = value;
    }

    public short getRegisterHL() {
        return RegHL.Word;
    }

    public void setRegisterHL(short value) {
        RegHL.Word = value;
    }

    public short getRegisterPC() {
        return RegPC.Word;
    }

    public void setRegisterPC(short value) {
        RegPC.Word = value;
    }

    public short getRegisterSP() {
        return RegSP.Word;
    }

    public void setRegisterSP(short value) {
        RegSP.Word = value;
    }

    public short getRegisterIX() {
        return RegIX.Word;
    }

    public void setRegisterIX(short value) {
        RegIX.Word = value;
    }

    public short getRegisterIY() {
        return RegIY.Word;
    }

    public void setRegisterIY(short value) {
        RegIY.Word = value;
    }

    public short getRegisterWZ() {
        return RegWZ.Word;
    }

    public void setRegisterWZ(short value) {
        RegWZ.Word = value;
    }

    public byte getRegisterI() {
        return RegI;
    }

    public void setRegisterI(byte value) {
        RegI = value;
    }

    public byte getRegisterR() {
        return RegR;
    }

    public void setRegisterR(byte value) {
        RegR = value;
    }

    public byte getRegisterR2() {
        return RegR2;
    }

    public void setRegisterR2(byte value) {
        RegR2 = value;
    }

    public short getRegisterShadowAF() {
        return RegAltAF.Word;
    }

    public void setRegisterShadowAF(short value) {
        RegAltAF.Word = value;
    }

    public short getRegisterShadowBC() {
        return RegAltBC.Word;
    }

    public void setRegisterShadowBC(short value) {
        RegAltBC.Word = value;
    }

    public short getRegisterShadowDE() {
        return RegAltDE.Word;
    }

    public void setRegisterShadowDE(short value) {
        RegAltDE.Word = value;
    }

    public short getRegisterShadowHL() {
        return RegAltHL.Word;
    }

    public void setRegisterShadowHL(short value) {
        RegAltHL.Word = value;
    }

//#endregion

//#region Execute

    public short PPC;
    public short OP;

    private static class Context {
        byte Displacement;

        boolean TBOOL;
        byte TB;
        byte TBH;
        byte TBL;
        byte TB1;
        byte TB2;
        byte TSB;
        short TUS;
        int TI1;
        int TI2;
        int TIR;
    }

    /**
     * Runs the CPU for a particular number of clock cycles.
     *
     * @param cycles The number of cycles to run the CPU emulator for. Specify -1 to run for a single instruction.
     */
    @Override
    public int ExecuteCycles(int cycles) {
        pendingCycles = cycles;

        Context c = new Context();

        // Process interrupt requests.
        if (nonMaskableInterruptPending) {
            if (halted) {
                halted = false;
                RegPC.Word++;
            }
            totalExecutedCycles += 11;
            pendingCycles -= 11;
            nonMaskableInterruptPending = false;
//            iff2 = iff1;
            iff1 = false;
            WriteMemory.accept(--RegSP.Word, RegPC.High);
            WriteMemory.accept(--RegSP.Word, RegPC.Low);
            RegPC.Word = 0x66;
            RegWZ.Word = RegPC.Word;
            NMICallback.run();
        }
        do {
            if (interrupt && iff1 && Interruptable) { //Z80.irq_state iff1 !Z80.after_ei
                if (halted) {
                    halted = false;
                    RegPC.Word++;
                }
                iff1 = iff2 = false;
                int irq_vector = IRQCallback.get();
                switch (interruptMode) {
                    case 0:
                        switch (irq_vector & 0xff_0000) {
                            case 0xcd_0000:
                                WriteMemory.accept(--RegSP.Word, RegPC.High);
                                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                                RegPC.Word = (short) (irq_vector & 0xffff);
                                totalExecutedCycles += 19;
                                pendingCycles -= 19;
                                break;
                            case 0xc3_0000:
                                RegPC.Word = (short) (irq_vector & 0xffff);
                                totalExecutedCycles += 12;
                                pendingCycles -= 12;
                                break;
                            default:
                                WriteMemory.accept(--RegSP.Word, RegPC.High);
                                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                                RegPC.Word = (short) (irq_vector & 0x0038);
                                totalExecutedCycles += cc_op[RegPC.Low] + cc_ex[RegPC.Low];
                                pendingCycles -= cc_op[RegPC.Low] + cc_ex[RegPC.Low];
                                break;
                        }
                        break;
                    case 1:
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x38;
                        totalExecutedCycles += 13;
                        pendingCycles -= 13;
                        break;
                    case 2:
                        c.TUS = (short) (RegI * 256 + irq_vector);
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Low = ReadMemory.apply(c.TUS++);
                        RegPC.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 17;
                        pendingCycles -= 17;
                        break;
                }
                RegWZ.Word = RegPC.Word;
            }

            {
                Interruptable = true;

                ++RegR;

                PPC = RegPC.Word;
                OP = ReadOp.apply(PPC);
                RegPC.Word++;
                if (cpunum == 1 && Cpuexec.bLog1 == 2 && pendingCycles == 0xf91) {
                    int i1 = 1;
                }

                debugger_start_cpu_hook_callback.run();
                switch (OP) { //ReadMemory(RegPC.Word++))
                    case 0x00: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x01: // LD BC, nn
                        RegBC.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x02: // LD (BC), A
                        WriteMemory.accept(RegBC.Word, RegAF.High);
                        RegWZ.Low = (byte) ((RegBC.Word + 1) & 0xFF);
                        RegWZ.High = RegAF.High;
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x03: // INC BC
                        ++RegBC.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x04: // INC B
                        RegAF.Low = (byte) (TableInc[++RegBC.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x05: // DEC B
                        RegAF.Low = (byte) (TableDec[--RegBC.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x06: // LD B, n
                        RegBC.High = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x07: // RLCA
                        RegAF.Word = TableRotShift[0][0][RegAF.Word];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x08: // EX AF, AF'
                        c.TUS = RegAF.Word;
                        RegAF.Word = RegAltAF.Word;
                        RegAltAF.Word = c.TUS;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x09: // ADD HL, BC
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegBC.Word;
                        c.TIR = c.TI1 + c.TI2;
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x0A: // LD A, (BC)
                        RegAF.High = ReadMemory.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x0B: // DEC BC
                        --RegBC.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x0C: // INC C
                        RegAF.Low = (byte) (TableInc[++RegBC.Low] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0D: // DEC C
                        RegAF.Low = (byte) (TableDec[--RegBC.Low] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0E: // LD C, n
                        RegBC.Low = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x0F: // RRCA
                        RegAF.Word = TableRotShift[0][1][RegAF.Word];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x10: // DJNZ d
                        c.TSB = ReadOpArg.apply(RegPC.Word++);
                        if (--RegBC.High != 0) {
                            RegPC.Word = (short) (RegPC.Word + c.TSB);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 13;
                            pendingCycles -= 13;
                        } else {
                            totalExecutedCycles += 8;
                            pendingCycles -= 8;
                        }
                        break;
                    case 0x11: // LD DE, nn
                        RegDE.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x12: // LD (DE), A
                        WriteMemory.accept(RegDE.Word, RegAF.High);
                        RegWZ.Low = (byte) ((RegDE.Word + 1) & 0xFF);
                        RegWZ.High = RegAF.High;
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x13: // INC DE
                        ++RegDE.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x14: // INC D
                        RegAF.Low = (byte) (TableInc[++RegDE.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x15: // DEC D
                        RegAF.Low = (byte) (TableDec[--RegDE.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x16: // LD D, n
                        RegDE.High = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x17: // RLA
                        RegAF.Word = TableRotShift[0][2][RegAF.Word];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x18: // JR d
                        c.TSB = ReadOpArg.apply(RegPC.Word++);
                        RegPC.Word = (short) (RegPC.Word + c.TSB);
                        RegWZ.Word = RegPC.Word;
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        break;
                    case 0x19: // ADD HL, DE
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegDE.Word;
                        c.TIR = c.TI1 + c.TI2;
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x1A: // LD A, (DE)
                        RegAF.High = ReadMemory.apply(RegDE.Word);
                        RegWZ.Word = (short) (RegDE.Word + 1);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x1B: // DEC DE
                        --RegDE.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x1C: // INC E
                        RegAF.Low = (byte) (TableInc[++RegDE.Low] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1D: // DEC E
                        RegAF.Low = (byte) (TableDec[--RegDE.Low] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1E: // LD E, n
                        RegDE.Low = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x1F: // RRA
                        RegAF.Word = TableRotShift[0][3][RegAF.Word];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x20: // JR NZ, d
                        c.TSB = ReadOpArg.apply(RegPC.Word++);
                        if (!getRegFlagZ()) {
                            RegPC.Word = (short) (RegPC.Word + c.TSB);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x21: // LD HL, nn
                        RegHL.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x22: // LD (nn), HL
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegHL.Low);
                        WriteMemory.accept(c.TUS, RegHL.High);
                        RegWZ.Word = c.TUS;
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x23: // INC HL
                        ++RegHL.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x24: // INC H
                        RegAF.Low = (byte) (TableInc[++RegHL.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x25: // DEC H
                        RegAF.Low = (byte) (TableDec[--RegHL.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x26: // LD H, n
                        RegHL.High = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x27: // DAA
                        RegAF.Word = TableDaa[RegAF.Word];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x28: // JR Z, d
                        c.TSB = ReadOpArg.apply(RegPC.Word++);
                        if (getRegFlagZ()) {
                            RegPC.Word = (short) (RegPC.Word + c.TSB);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x29: // ADD HL, HL
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegHL.Word;
                        c.TIR = c.TI1 + c.TI2;
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x2A: // LD HL, (nn)
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        RegHL.Low = ReadMemory.apply(c.TUS++);
                        RegHL.High = ReadMemory.apply(c.TUS);
                        RegWZ.Word = c.TUS;
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x2B: // DEC HL
                        --RegHL.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x2C: // INC L
                        RegAF.Low = (byte) (TableInc[++RegHL.Low] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2D: // DEC L
                        RegAF.Low = (byte) (TableDec[--RegHL.Low] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2E: // LD L, n
                        RegHL.Low = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x2F: // CPL
                        RegAF.High ^= (byte) 0xFF;
                        setRegFlagH(true);
                        setRegFlagN(true);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x30: // JR NC, d
                        c.TSB = ReadOpArg.apply(RegPC.Word++);
                        if (!getRegFlagC()) {
                            RegPC.Word = (short) (RegPC.Word + c.TSB);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x31: // LD SP, nn
                        RegSP.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x32: // LD (nn), A
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS, RegAF.High);
                        RegWZ.Low = (byte) ((c.TUS + 1) & 0xFF);
                        RegWZ.High = RegAF.High;
                        totalExecutedCycles += 13;
                        pendingCycles -= 13;
                        break;
                    case 0x33: // INC SP
                        ++RegSP.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x34: // INC (HL)
                        c.TB = ReadMemory.apply(RegHL.Word);
                        RegAF.Low = (byte) (TableInc[++c.TB] | (RegAF.Low & 1));
                        WriteMemory.accept(RegHL.Word, c.TB);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x35: // DEC (HL)
                        c.TB = ReadMemory.apply(RegHL.Word);
                        RegAF.Low = (byte) (TableDec[--c.TB] | (RegAF.Low & 1));
                        WriteMemory.accept(RegHL.Word, c.TB);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x36: // LD (HL), n
                        WriteMemory.accept(RegHL.Word, ReadOpArg.apply(RegPC.Word++));
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x37: // SCF
                        setRegFlagH(false);
                        setRegFlagN(false);
                        setRegFlagC(true);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x38: // JR C, d
                        c.TSB = ReadOpArg.apply(RegPC.Word++);
                        if (getRegFlagC()) {
                            RegPC.Word = (short) (RegPC.Word + c.TSB);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x39: // ADD HL, SP
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegSP.Word;
                        c.TIR = c.TI1 + c.TI2;
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x3A: // LD A, (nn)
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        RegAF.High = ReadMemory.apply(c.TUS);
                        RegWZ.Word = (short) (c.TUS + 1);
                        totalExecutedCycles += 13;
                        pendingCycles -= 13;//
                        break;
                    case 0x3B: // DEC SP
                        --RegSP.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x3C: // INC A
                        RegAF.Low = (byte) (TableInc[++RegAF.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3D: // DEC A
                        RegAF.Low = (byte) (TableDec[--RegAF.High] | (RegAF.Low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3E: // LD A, n
                        RegAF.High = ReadOpArg.apply(RegPC.Word++);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x3F: // CCF
                        setRegFlagH(getRegFlagC());
                        setRegFlagN(false);
                        setRegFlagC(getRegFlagC() ^ true);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x40: // LD B, B
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x41: // LD B, C
                        RegBC.High = RegBC.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x42: // LD B, D
                        RegBC.High = RegDE.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x43: // LD B, E
                        RegBC.High = RegDE.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x44: // LD B, H
                        RegBC.High = RegHL.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x45: // LD B, L
                        RegBC.High = RegHL.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x46: // LD B, (HL)
                        RegBC.High = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x47: // LD B, A
                        RegBC.High = RegAF.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x48: // LD C, B
                        RegBC.Low = RegBC.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x49: // LD C, C
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4A: // LD C, D
                        RegBC.Low = RegDE.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4B: // LD C, E
                        RegBC.Low = RegDE.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4C: // LD C, H
                        RegBC.Low = RegHL.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4D: // LD C, L
                        RegBC.Low = RegHL.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4E: // LD C, (HL)
                        RegBC.Low = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x4F: // LD C, A
                        RegBC.Low = RegAF.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x50: // LD D, B
                        RegDE.High = RegBC.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x51: // LD D, C
                        RegDE.High = RegBC.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x52: // LD D, D
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x53: // LD D, E
                        RegDE.High = RegDE.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x54: // LD D, H
                        RegDE.High = RegHL.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x55: // LD D, L
                        RegDE.High = RegHL.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x56: // LD D, (HL)
                        RegDE.High = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x57: // LD D, A
                        RegDE.High = RegAF.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x58: // LD E, B
                        RegDE.Low = RegBC.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x59: // LD E, C
                        RegDE.Low = RegBC.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5A: // LD E, D
                        RegDE.Low = RegDE.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5B: // LD E, E
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5C: // LD E, H
                        RegDE.Low = RegHL.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5D: // LD E, L
                        RegDE.Low = RegHL.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5E: // LD E, (HL)
                        RegDE.Low = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x5F: // LD E, A
                        RegDE.Low = RegAF.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x60: // LD H, B
                        RegHL.High = RegBC.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x61: // LD H, C
                        RegHL.High = RegBC.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x62: // LD H, D
                        RegHL.High = RegDE.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x63: // LD H, E
                        RegHL.High = RegDE.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x64: // LD H, H
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x65: // LD H, L
                        RegHL.High = RegHL.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x66: // LD H, (HL)
                        RegHL.High = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x67: // LD H, A
                        RegHL.High = RegAF.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x68: // LD L, B
                        RegHL.Low = RegBC.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x69: // LD L, C
                        RegHL.Low = RegBC.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6A: // LD L, D
                        RegHL.Low = RegDE.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6B: // LD L, E
                        RegHL.Low = RegDE.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6C: // LD L, H
                        RegHL.Low = RegHL.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6D: // LD L, L
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6E: // LD L, (HL)
                        RegHL.Low = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x6F: // LD L, A
                        RegHL.Low = RegAF.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x70: // LD (HL), B
                        WriteMemory.accept(RegHL.Word, RegBC.High);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x71: // LD (HL), C
                        WriteMemory.accept(RegHL.Word, RegBC.Low);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x72: // LD (HL), D
                        WriteMemory.accept(RegHL.Word, RegDE.High);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x73: // LD (HL), E
                        WriteMemory.accept(RegHL.Word, RegDE.Low);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x74: // LD (HL), H
                        WriteMemory.accept(RegHL.Word, RegHL.High);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x75: // LD (HL), L
                        WriteMemory.accept(RegHL.Word, RegHL.Low);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x76: // HALT
                        Halt();
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x77: // LD (HL), A
                        WriteMemory.accept(RegHL.Word, RegAF.High);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x78: // LD A, B
                        RegAF.High = RegBC.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x79: // LD A, C
                        RegAF.High = RegBC.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7A: // LD A, D
                        RegAF.High = RegDE.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7B: // LD A, E
                        RegAF.High = RegDE.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7C: // LD A, H
                        RegAF.High = RegHL.High;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7D: // LD A, L
                        RegAF.High = RegHL.Low;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7E: // LD A, (HL)
                        RegAF.High = ReadMemory.apply(RegHL.Word);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x7F: // LD A, A
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x80: // ADD A, B
                        RegAF.Word = TableALU[0][RegAF.High][RegBC.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x81: // ADD A, C
                        RegAF.Word = TableALU[0][RegAF.High][RegBC.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x82: // ADD A, D
                        RegAF.Word = TableALU[0][RegAF.High][RegDE.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x83: // ADD A, E
                        RegAF.Word = TableALU[0][RegAF.High][RegDE.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x84: // ADD A, H
                        RegAF.Word = TableALU[0][RegAF.High][RegHL.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x85: // ADD A, L
                        RegAF.Word = TableALU[0][RegAF.High][RegHL.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x86: // ADD A, (HL)
                        RegAF.Word = TableALU[0][RegAF.High][ReadMemory.apply(RegHL.Word)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x87: // ADD A, A
                        RegAF.Word = TableALU[0][RegAF.High][RegAF.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x88: // ADC A, B
                        RegAF.Word = TableALU[1][RegAF.High][RegBC.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x89: // ADC A, C
                        RegAF.Word = TableALU[1][RegAF.High][RegBC.Low][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8A: // ADC A, D
                        RegAF.Word = TableALU[1][RegAF.High][RegDE.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8B: // ADC A, E
                        RegAF.Word = TableALU[1][RegAF.High][RegDE.Low][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8C: // ADC A, H
                        RegAF.Word = TableALU[1][RegAF.High][RegHL.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8D: // ADC A, L
                        RegAF.Word = TableALU[1][RegAF.High][RegHL.Low][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8E: // ADC A, (HL)
                        RegAF.Word = TableALU[1][RegAF.High][ReadMemory.apply(RegHL.Word)][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x8F: // ADC A, A
                        RegAF.Word = TableALU[1][RegAF.High][RegAF.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x90: // SUB B
                        RegAF.Word = TableALU[2][RegAF.High][RegBC.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x91: // SUB C
                        RegAF.Word = TableALU[2][RegAF.High][RegBC.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x92: // SUB D
                        RegAF.Word = TableALU[2][RegAF.High][RegDE.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x93: // SUB E
                        RegAF.Word = TableALU[2][RegAF.High][RegDE.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x94: // SUB H
                        RegAF.Word = TableALU[2][RegAF.High][RegHL.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x95: // SUB L
                        RegAF.Word = TableALU[2][RegAF.High][RegHL.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x96: // SUB (HL)
                        RegAF.Word = TableALU[2][RegAF.High][ReadMemory.apply(RegHL.Word)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x97: // SUB A, A
                        RegAF.Word = TableALU[2][RegAF.High][RegAF.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x98: // SBC A, B
                        RegAF.Word = TableALU[3][RegAF.High][RegBC.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x99: // SBC A, C
                        RegAF.Word = TableALU[3][RegAF.High][RegBC.Low][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9A: // SBC A, D
                        RegAF.Word = TableALU[3][RegAF.High][RegDE.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9B: // SBC A, E
                        RegAF.Word = TableALU[3][RegAF.High][RegDE.Low][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9C: // SBC A, H
                        RegAF.Word = TableALU[3][RegAF.High][RegHL.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9D: // SBC A, L
                        RegAF.Word = TableALU[3][RegAF.High][RegHL.Low][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9E: // SBC A, (HL)
                        RegAF.Word = TableALU[3][RegAF.High][ReadMemory.apply(RegHL.Word)][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x9F: // SBC A, A
                        RegAF.Word = TableALU[3][RegAF.High][RegAF.High][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA0: // AND B
                        RegAF.Word = TableALU[4][RegAF.High][RegBC.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA1: // AND C
                        RegAF.Word = TableALU[4][RegAF.High][RegBC.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA2: // AND D
                        RegAF.Word = TableALU[4][RegAF.High][RegDE.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA3: // AND E
                        RegAF.Word = TableALU[4][RegAF.High][RegDE.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA4: // AND H
                        RegAF.Word = TableALU[4][RegAF.High][RegHL.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA5: // AND L
                        RegAF.Word = TableALU[4][RegAF.High][RegHL.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA6: // AND (HL)
                        RegAF.Word = TableALU[4][RegAF.High][ReadMemory.apply(RegHL.Word)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xA7: // AND A
                        RegAF.Word = TableALU[4][RegAF.High][RegAF.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA8: // XOR B
                        RegAF.Word = TableALU[5][RegAF.High][RegBC.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA9: // XOR C
                        RegAF.Word = TableALU[5][RegAF.High][RegBC.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAA: // XOR D
                        RegAF.Word = TableALU[5][RegAF.High][RegDE.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAB: // XOR E
                        RegAF.Word = TableALU[5][RegAF.High][RegDE.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAC: // XOR H
                        RegAF.Word = TableALU[5][RegAF.High][RegHL.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAD: // XOR L
                        RegAF.Word = TableALU[5][RegAF.High][RegHL.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAE: // XOR (HL)
                        RegAF.Word = TableALU[5][RegAF.High][ReadMemory.apply(RegHL.Word)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xAF: // XOR A
                        RegAF.Word = TableALU[5][RegAF.High][RegAF.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB0: // OR B
                        RegAF.Word = TableALU[6][RegAF.High][RegBC.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB1: // OR C
                        RegAF.Word = TableALU[6][RegAF.High][RegBC.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB2: // OR D
                        RegAF.Word = TableALU[6][RegAF.High][RegDE.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB3: // OR E
                        RegAF.Word = TableALU[6][RegAF.High][RegDE.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB4: // OR H
                        RegAF.Word = TableALU[6][RegAF.High][RegHL.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB5: // OR L
                        RegAF.Word = TableALU[6][RegAF.High][RegHL.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB6: // OR (HL)
                        RegAF.Word = TableALU[6][RegAF.High][ReadMemory.apply(RegHL.Word)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xB7: // OR A
                        RegAF.Word = TableALU[6][RegAF.High][RegAF.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB8: // CP B
                        RegAF.Word = TableALU[7][RegAF.High][RegBC.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB9: // CP C
                        RegAF.Word = TableALU[7][RegAF.High][RegBC.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBA: // CP D
                        RegAF.Word = TableALU[7][RegAF.High][RegDE.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBB: // CP E
                        RegAF.Word = TableALU[7][RegAF.High][RegDE.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBC: // CP H
                        RegAF.Word = TableALU[7][RegAF.High][RegHL.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBD: // CP L
                        RegAF.Word = TableALU[7][RegAF.High][RegHL.Low][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBE: // CP (HL)
                        RegAF.Word = TableALU[7][RegAF.High][ReadMemory.apply(RegHL.Word)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xBF: // CP A
                        RegAF.Word = TableALU[7][RegAF.High][RegAF.High][0];
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC0: // RET NZ
                        if (!getRegFlagZ()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xC1: // POP BC
                        RegBC.Low = ReadMemory.apply(RegSP.Word++);
                        RegBC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xC2: // JP NZ, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagZ()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xC3: // JP nn
                        RegPC.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        RegWZ.Word = RegPC.Word;
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xC4: // CALL NZ, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagZ()) {
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xC5: // PUSH BC
                        WriteMemory.accept(--RegSP.Word, RegBC.High);
                        WriteMemory.accept(--RegSP.Word, RegBC.Low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xC6: // ADD A, n
                        RegAF.Word = TableALU[0][RegAF.High][ReadOpArg.apply(RegPC.Word++)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xC7: // RST $00
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x00;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xC8: // RET Z
                        if (getRegFlagZ()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xC9: // RET
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        RegWZ.Word = RegPC.Word;
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xCA: // JP Z, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagZ()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xCB: // (Prefix)
                        ++RegR;
                        processCB(c);
                        break;
                    case 0xCC: // CALL Z, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagZ()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xCD: // CALL nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = RegWZ.Word;
                        totalExecutedCycles += 17;
                        pendingCycles -= 17;
                        break;
                    case 0xCE: // ADC A, n
                        RegAF.Word = TableALU[1][RegAF.High][ReadOpArg.apply(RegPC.Word++)][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xCF: // RST $08
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x08;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD0: // RET NC
                        if (!getRegFlagC()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xD1: // POP DE
                        RegDE.Low = ReadMemory.apply(RegSP.Word++);
                        RegDE.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xD2: // JP NC, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagC()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xD3: // OUT n, A
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) | (RegAF.High << 8));
                        WriteHardware.accept(c.TUS, RegAF.High);
                        RegWZ.Low = (byte) (((c.TUS & 0xFF) + 1) & 0xFF);
                        RegWZ.High = RegAF.High;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD4: // CALL NC, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagC()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xD5: // PUSH DE
                        WriteMemory.accept(--RegSP.Word, RegDE.High);
                        WriteMemory.accept(--RegSP.Word, RegDE.Low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD6: // SUB n
                        RegAF.Word = TableALU[2][RegAF.High][ReadOpArg.apply(RegPC.Word++)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xD7: // RST $10
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x10;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD8: // RET C
                        if (getRegFlagC()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xD9: // EXX
                        c.TUS = RegBC.Word;
                        RegBC.Word = RegAltBC.Word;
                        RegAltBC.Word = c.TUS;
                        c.TUS = RegDE.Word;
                        RegDE.Word = RegAltDE.Word;
                        RegAltDE.Word = c.TUS;
                        c.TUS = RegHL.Word;
                        RegHL.Word = RegAltHL.Word;
                        RegAltHL.Word = c.TUS;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDA: // JP C, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagC()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xDB: // IN A, n
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) | (RegAF.High << 8));
                        RegAF.High = ReadHardware.apply(c.TUS);
                        RegWZ.Word = (short) (c.TUS + 1);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xDC: // CALL C, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagC()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xDD: // (Prefix)
                        ++RegR;
                        processDD(c);
                        break;
                    case 0xDE: // SBC A, n
                        RegAF.Word = TableALU[3][RegAF.High][ReadOpArg.apply(RegPC.Word++)][getRegFlagC() ? 1 : 0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xDF: // RST $18
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x18;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xE0: // RET PO
                        if (!getRegFlagP()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xE1: // POP HL
                        RegHL.Low = ReadMemory.apply(RegSP.Word++);
                        RegHL.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xE2: // JP PO, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagP()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xE3: // EX (SP), HL
                        c.TUS = RegSP.Word;
                        c.TBL = ReadMemory.apply(c.TUS++);
                        c.TBH = ReadMemory.apply(c.TUS--);
                        WriteMemory.accept(c.TUS++, RegHL.Low);
                        WriteMemory.accept(c.TUS, RegHL.High);
                        RegHL.Low = c.TBL;
                        RegHL.High = c.TBH;
                        RegWZ.Word = RegHL.Word;
                        totalExecutedCycles += 19;
                        pendingCycles -= 19;
                        break;
                    case 0xE4: // CALL C, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagC()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xE5: // PUSH HL
                        WriteMemory.accept(--RegSP.Word, RegHL.High);
                        WriteMemory.accept(--RegSP.Word, RegHL.Low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xE6: // AND n
                        RegAF.Word = TableALU[4][RegAF.High][ReadOpArg.apply(RegPC.Word++)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xE7: // RST $20
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x20;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xE8: // RET PE
                        if (getRegFlagP()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xE9: // JP HL
                        RegPC.Word = RegHL.Word;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEA: // JP PE, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagP()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xEB: // EX DE, HL
                        c.TUS = RegDE.Word;
                        RegDE.Word = RegHL.Word;
                        RegHL.Word = c.TUS;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEC: // CALL PE, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagP()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xED: // (Prefix)
                        ++RegR;
                        processED(c);
                        break;
                    case 0xEE: // XOR n
                        RegAF.Word = TableALU[5][RegAF.High][ReadOpArg.apply(RegPC.Word++)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xEF: // RST $28
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x28;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xF0: // RET P
                        if (!getRegFlagS()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xF1: // POP AF
                        RegAF.Low = ReadMemory.apply(RegSP.Word++);
                        RegAF.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xF2: // JP P, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagS()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xF3: // DI
                        iff1 = iff2 = false;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF4: // CALL P, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (!getRegFlagS()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xF5: // PUSH AF
                        WriteMemory.accept(--RegSP.Word, RegAF.High);
                        WriteMemory.accept(--RegSP.Word, RegAF.Low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xF6: // OR n
                        RegAF.Word = TableALU[6][RegAF.High][ReadOpArg.apply(RegPC.Word++)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xF7: // RST $30
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x30;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xF8: // RET M
                        if (getRegFlagS()) {
                            RegPC.Low = ReadMemory.apply(RegSP.Word++);
                            RegPC.High = ReadMemory.apply(RegSP.Word++);
                            RegWZ.Word = RegPC.Word;
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xF9: // LD SP, HL
                        RegSP.Word = RegHL.Word;
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0xFA: // JP M, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagS()) {
                            RegPC.Word = RegWZ.Word;
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xFB: // EI
                        iff1 = iff2 = true;
                        Interruptable = false;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFC: // CALL M, nn
                        RegWZ.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        if (getRegFlagS()) {
                            WriteMemory.accept(--RegSP.Word, RegPC.High);
                            WriteMemory.accept(--RegSP.Word, RegPC.Low);
                            RegPC.Word = RegWZ.Word;
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xFD: // (Prefix)
                        ++RegR;
                        processFD(c);
                        break;
                    case 0xFE: // CP n
                        RegAF.Word = TableALU[7][RegAF.High][ReadOpArg.apply(RegPC.Word++)][0];
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xFF: // RST $38
                        WriteMemory.accept(--RegSP.Word, RegPC.High);
                        WriteMemory.accept(--RegSP.Word, RegPC.Low);
                        RegPC.Word = 0x38;
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                }
                debugger_stop_cpu_hook_callback.run();
            }
        }
        while (pendingCycles > 0);
        return cycles - pendingCycles;
    }

    private void processCB(Context c) {
        switch (ReadOp.apply(RegPC.Word++) & 0xff) {
            case 0x00: // RLC B
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x01: // RLC C
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x02: // RLC D
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x03: // RLC E
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x04: // RLC H
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x05: // RLC L
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x06: // RLC (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x07: // RLC A
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x08: // RRC B
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x09: // RRC C
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0A: // RRC D
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0B: // RRC E
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0C: // RRC H
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0D: // RRC L
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0E: // RRC (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x0F: // RRC A
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x10: // RL B
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x11: // RL C
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x12: // RL D
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x13: // RL E
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x14: // RL H
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x15: // RL L
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x16: // RL (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x17: // RL A
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x18: // RR B
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x19: // RR C
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1A: // RR D
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1B: // RR E
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1C: // RR H
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1D: // RR L
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1E: // RR (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1F: // RR A
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x20: // SLA B
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x21: // SLA C
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x22: // SLA D
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x23: // SLA E
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x24: // SLA H
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x25: // SLA L
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x26: // SLA (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x27: // SLA A
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x28: // SRA B
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x29: // SRA C
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2A: // SRA D
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2B: // SRA E
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2C: // SRA H
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2D: // SRA L
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2E: // SRA (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2F: // SRA A
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x30: // SL1 B
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x31: // SL1 C
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x32: // SL1 D
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x33: // SL1 E
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x34: // SL1 H
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x35: // SL1 L
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x36: // SL1 (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x37: // SL1 A
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x38: // SRL B
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegBC.High];
                RegBC.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x39: // SRL C
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegBC.Low];
                RegBC.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3A: // SRL D
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegDE.High];
                RegDE.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3B: // SRL E
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegDE.Low];
                RegDE.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3C: // SRL H
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegHL.High];
                RegHL.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3D: // SRL L
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegHL.Low];
                RegHL.Low = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3E: // SRL (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegHL.Word)];
                WriteMemory.accept(RegHL.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3F: // SRL A
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * RegAF.High];
                RegAF.High = (byte) (c.TUS >> 8);
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x40: // BIT 0, B
                setRegFlagZ((RegBC.High & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x41: // BIT 0, C
                setRegFlagZ((RegBC.Low & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x42: // BIT 0, D
                setRegFlagZ((RegDE.High & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x43: // BIT 0, E
                setRegFlagZ((RegDE.Low & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x44: // BIT 0, H
                setRegFlagZ((RegHL.High & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x45: // BIT 0, L
                setRegFlagZ((RegHL.Low & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x46: // BIT 0, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x47: // BIT 0, A
                setRegFlagZ((RegAF.High & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x48: // BIT 1, B
                setRegFlagZ((RegBC.High & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x49: // BIT 1, C
                setRegFlagZ((RegBC.Low & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4A: // BIT 1, D
                setRegFlagZ((RegDE.High & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4B: // BIT 1, E
                setRegFlagZ((RegDE.Low & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4C: // BIT 1, H
                setRegFlagZ((RegHL.High & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4D: // BIT 1, L
                setRegFlagZ((RegHL.Low & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4E: // BIT 1, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x4F: // BIT 1, A
                setRegFlagZ((RegAF.High & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x50: // BIT 2, B
                setRegFlagZ((RegBC.High & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x51: // BIT 2, C
                setRegFlagZ((RegBC.Low & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x52: // BIT 2, D
                setRegFlagZ((RegDE.High & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x53: // BIT 2, E
                setRegFlagZ((RegDE.Low & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x54: // BIT 2, H
                setRegFlagZ((RegHL.High & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x55: // BIT 2, L
                setRegFlagZ((RegHL.Low & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x56: // BIT 2, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x57: // BIT 2, A
                setRegFlagZ((RegAF.High & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x58: // BIT 3, B
                setRegFlagZ((RegBC.High & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x59: // BIT 3, C
                setRegFlagZ((RegBC.Low & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5A: // BIT 3, D
                setRegFlagZ((RegDE.High & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5B: // BIT 3, E
                setRegFlagZ((RegDE.Low & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5C: // BIT 3, H
                setRegFlagZ((RegHL.High & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5D: // BIT 3, L
                setRegFlagZ((RegHL.Low & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5E: // BIT 3, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x5F: // BIT 3, A
                setRegFlagZ((RegAF.High & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x60: // BIT 4, B
                setRegFlagZ((RegBC.High & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x61: // BIT 4, C
                setRegFlagZ((RegBC.Low & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x62: // BIT 4, D
                setRegFlagZ((RegDE.High & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x63: // BIT 4, E
                setRegFlagZ((RegDE.Low & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x64: // BIT 4, H
                setRegFlagZ((RegHL.High & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x65: // BIT 4, L
                setRegFlagZ((RegHL.Low & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x66: // BIT 4, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x67: // BIT 4, A
                setRegFlagZ((RegAF.High & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x68: // BIT 5, B
                setRegFlagZ((RegBC.High & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x69: // BIT 5, C
                setRegFlagZ((RegBC.Low & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6A: // BIT 5, D
                setRegFlagZ((RegDE.High & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6B: // BIT 5, E
                setRegFlagZ((RegDE.Low & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6C: // BIT 5, H
                setRegFlagZ((RegHL.High & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6D: // BIT 5, L
                setRegFlagZ((RegHL.Low & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6E: // BIT 5, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x6F: // BIT 5, A
                setRegFlagZ((RegAF.High & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x70: // BIT 6, B
                setRegFlagZ((RegBC.High & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x71: // BIT 6, C
                setRegFlagZ((RegBC.Low & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x72: // BIT 6, D
                setRegFlagZ((RegDE.High & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x73: // BIT 6, E
                setRegFlagZ((RegDE.Low & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x74: // BIT 6, H
                setRegFlagZ((RegHL.High & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x75: // BIT 6, L
                setRegFlagZ((RegHL.Low & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x76: // BIT 6, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x77: // BIT 6, A
                setRegFlagZ((RegAF.High & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x78: // BIT 7, B
                setRegFlagZ((RegBC.High & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x79: // BIT 7, C
                setRegFlagZ((RegBC.Low & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7A: // BIT 7, D
                setRegFlagZ((RegDE.High & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7B: // BIT 7, E
                setRegFlagZ((RegDE.Low & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7C: // BIT 7, H
                setRegFlagZ((RegHL.High & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7D: // BIT 7, L
                setRegFlagZ((RegHL.Low & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7E: // BIT 7, (HL)
                setRegFlagZ((ReadMemory.apply(RegHL.Word) & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3((RegWZ.High & 0x08) != 0);
                setRegFlag5((RegWZ.High & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x7F: // BIT 7, A
                setRegFlagZ((RegAF.High & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x80: // RES 0, B
                RegBC.High &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x81: // RES 0, C
                RegBC.Low &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x82: // RES 0, D
                RegDE.High &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x83: // RES 0, E
                RegDE.Low &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x84: // RES 0, H
                RegHL.High &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x85: // RES 0, L
                RegHL.Low &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x86: // RES 0, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x01));
                break;
            case 0x87: // RES 0, A
                RegAF.High &= (byte) ~0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x88: // RES 1, B
                RegBC.High &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x89: // RES 1, C
                RegBC.Low &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8A: // RES 1, D
                RegDE.High &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8B: // RES 1, E
                RegDE.Low &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8C: // RES 1, H
                RegHL.High &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8D: // RES 1, L
                RegHL.Low &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8E: // RES 1, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x02));
                break;
            case 0x8F: // RES 1, A
                RegAF.High &= (byte) ~0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x90: // RES 2, B
                RegBC.High &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x91: // RES 2, C
                RegBC.Low &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x92: // RES 2, D
                RegDE.High &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x93: // RES 2, E
                RegDE.Low &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x94: // RES 2, H
                RegHL.High &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x95: // RES 2, L
                RegHL.Low &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x96: // RES 2, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x04));
                break;
            case 0x97: // RES 2, A
                RegAF.High &= (byte) ~0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x98: // RES 3, B
                RegBC.High &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x99: // RES 3, C
                RegBC.Low &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9A: // RES 3, D
                RegDE.High &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9B: // RES 3, E
                RegDE.Low &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9C: // RES 3, H
                RegHL.High &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9D: // RES 3, L
                RegHL.Low &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9E: // RES 3, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x08));
                break;
            case 0x9F: // RES 3, A
                RegAF.High &= (byte) ~0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA0: // RES 4, B
                RegBC.High &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA1: // RES 4, C
                RegBC.Low &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA2: // RES 4, D
                RegDE.High &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA3: // RES 4, E
                RegDE.Low &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA4: // RES 4, H
                RegHL.High &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA5: // RES 4, L
                RegHL.Low &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA6: // RES 4, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x10));
                break;
            case 0xA7: // RES 4, A
                RegAF.High &= (byte) ~0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA8: // RES 5, B
                RegBC.High &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA9: // RES 5, C
                RegBC.Low &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAA: // RES 5, D
                RegDE.High &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAB: // RES 5, E
                RegDE.Low &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAC: // RES 5, H
                RegHL.High &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAD: // RES 5, L
                RegHL.Low &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAE: // RES 5, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x20));
                break;
            case 0xAF: // RES 5, A
                RegAF.High &= (byte) ~0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB0: // RES 6, B
                RegBC.High &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB1: // RES 6, C
                RegBC.Low &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB2: // RES 6, D
                RegDE.High &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB3: // RES 6, E
                RegDE.Low &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB4: // RES 6, H
                RegHL.High &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB5: // RES 6, L
                RegHL.Low &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB6: // RES 6, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x40));
                break;
            case 0xB7: // RES 6, A
                RegAF.High &= (byte) ~0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB8: // RES 7, B
                RegBC.High &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB9: // RES 7, C
                RegBC.Low &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBA: // RES 7, D
                RegDE.High &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBB: // RES 7, E
                RegDE.Low &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBC: // RES 7, H
                RegHL.High &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBD: // RES 7, L
                RegHL.Low &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBE: // RES 7, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) & (byte) ~0x80));
                break;
            case 0xBF: // RES 7, A
                RegAF.High &= (byte) ~0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC0: // SET 0, B
                RegBC.High |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC1: // SET 0, C
                RegBC.Low |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC2: // SET 0, D
                RegDE.High |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC3: // SET 0, E
                RegDE.Low |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC4: // SET 0, H
                RegHL.High |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC5: // SET 0, L
                RegHL.Low |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC6: // SET 0, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x01));
                break;
            case 0xC7: // SET 0, A
                RegAF.High |= (byte) 0x01;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC8: // SET 1, B
                RegBC.High |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC9: // SET 1, C
                RegBC.Low |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCA: // SET 1, D
                RegDE.High |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCB: // SET 1, E
                RegDE.Low |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCC: // SET 1, H
                RegHL.High |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCD: // SET 1, L
                RegHL.Low |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCE: // SET 1, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x02));
                break;
            case 0xCF: // SET 1, A
                RegAF.High |= (byte) 0x02;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD0: // SET 2, B
                RegBC.High |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD1: // SET 2, C
                RegBC.Low |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD2: // SET 2, D
                RegDE.High |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD3: // SET 2, E
                RegDE.Low |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD4: // SET 2, H
                RegHL.High |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD5: // SET 2, L
                RegHL.Low |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD6: // SET 2, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x04));
                break;
            case 0xD7: // SET 2, A
                RegAF.High |= (byte) 0x04;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD8: // SET 3, B
                RegBC.High |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD9: // SET 3, C
                RegBC.Low |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDA: // SET 3, D
                RegDE.High |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDB: // SET 3, E
                RegDE.Low |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDC: // SET 3, H
                RegHL.High |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDD: // SET 3, L
                RegHL.Low |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDE: // SET 3, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x08));
                break;
            case 0xDF: // SET 3, A
                RegAF.High |= (byte) 0x08;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE0: // SET 4, B
                RegBC.High |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE1: // SET 4, C
                RegBC.Low |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE2: // SET 4, D
                RegDE.High |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE3: // SET 4, E
                RegDE.Low |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE4: // SET 4, H
                RegHL.High |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE5: // SET 4, L
                RegHL.Low |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE6: // SET 4, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x10));
                break;
            case 0xE7: // SET 4, A
                RegAF.High |= (byte) 0x10;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE8: // SET 5, B
                RegBC.High |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE9: // SET 5, C
                RegBC.Low |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEA: // SET 5, D
                RegDE.High |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEB: // SET 5, E
                RegDE.Low |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEC: // SET 5, H
                RegHL.High |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xED: // SET 5, L
                RegHL.Low |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEE: // SET 5, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x20));
                break;
            case 0xEF: // SET 5, A
                RegAF.High |= (byte) 0x20;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF0: // SET 6, B
                RegBC.High |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF1: // SET 6, C
                RegBC.Low |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF2: // SET 6, D
                RegDE.High |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF3: // SET 6, E
                RegDE.Low |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF4: // SET 6, H
                RegHL.High |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF5: // SET 6, L
                RegHL.Low |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF6: // SET 6, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x40));
                break;
            case 0xF7: // SET 6, A
                RegAF.High |= (byte) 0x40;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF8: // SET 7, B
                RegBC.High |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF9: // SET 7, C
                RegBC.Low |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFA: // SET 7, D
                RegDE.High |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFB: // SET 7, E
                RegDE.Low |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFC: // SET 7, H
                RegHL.High |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFD: // SET 7, L
                RegHL.Low |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFE: // SET 7, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(RegHL.Word, (byte) (ReadMemory.apply(RegHL.Word) | (byte) 0x80));
                break;
            case 0xFF: // SET 7, A
                RegAF.High |= (byte) 0x80;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
        }
    }

    private void processDD(Context c) {
        switch (ReadOp.apply(RegPC.Word++) & 0xff) {
            case 0x00: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x01: // LD BC, nn
                RegBC.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x02: // LD (BC), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                WriteMemory.accept(RegBC.Word, RegAF.High);
                break;
            case 0x03: // INC BC
                ++RegBC.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x04: // INC B
                RegAF.Low = (byte) (TableInc[++RegBC.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x05: // DEC B
                RegAF.Low = (byte) (TableDec[--RegBC.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x06: // LD B, n
                RegBC.High = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x07: // RLCA
                RegAF.Word = TableRotShift[0][0][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x08: // EX AF, AF'
                c.TUS = RegAF.Word;
                RegAF.Word = RegAltAF.Word;
                RegAltAF.Word = c.TUS;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x09: // ADD IX, BC
                c.TI1 = RegIX.Word;
                c.TI2 = RegBC.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIX.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIX.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x0A: // LD A, (BC)
                RegAF.High = ReadMemory.apply(RegBC.Word);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x0B: // DEC BC
                --RegBC.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x0C: // INC C
                RegAF.Low = (byte) (TableInc[++RegBC.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0D: // DEC C
                RegAF.Low = (byte) (TableDec[--RegBC.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0E: // LD C, n
                RegBC.Low = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x0F: // RRCA
                RegAF.Word = TableRotShift[0][1][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x10: // DJNZ d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (--RegBC.High != 0) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 13;
                    pendingCycles -= 13;
                } else {
                    totalExecutedCycles += 8;
                    pendingCycles -= 8;
                }
                break;
            case 0x11: // LD DE, nn
                RegDE.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x12: // LD (DE), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                WriteMemory.accept(RegDE.Word, RegAF.High);
                break;
            case 0x13: // INC DE
                ++RegDE.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x14: // INC D
                RegAF.Low = (byte) (TableInc[++RegDE.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x15: // DEC D
                RegAF.Low = (byte) (TableDec[--RegDE.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x16: // LD D, n
                RegDE.High = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x17: // RLA
                RegAF.Word = TableRotShift[0][2][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x18: // JR d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                RegPC.Word = (short) (RegPC.Word + c.TSB);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x19: // ADD IX, DE
                c.TI1 = RegIX.Word;
                c.TI2 = RegDE.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIX.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIX.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x1A: // LD A, (DE)
                RegAF.High = ReadMemory.apply(RegDE.Word);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1B: // DEC DE
                --RegDE.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x1C: // INC E
                RegAF.Low = (byte) (TableInc[++RegDE.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1D: // DEC E
                RegAF.Low = (byte) (TableDec[--RegDE.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1E: // LD E, n
                RegDE.Low = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1F: // RRA
                RegAF.Word = TableRotShift[0][3][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x20: // JR NZ, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (!getRegFlagZ()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x21: // LD IX, nn
                RegIX.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0x22: // LD (nn), IX
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                WriteMemory.accept(c.TUS++, RegIX.Low);
                WriteMemory.accept(c.TUS, RegIX.High);
                RegWZ.Word = c.TUS;
                break;
            case 0x23: // INC IX
                ++RegIX.Word;
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x24: // INC IXH
                RegAF.Low = (byte) (TableInc[++RegIX.High] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x25: // DEC IXH
                RegAF.Low = (byte) (TableDec[--RegIX.High] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x26: // LD IXH, n
                RegIX.High = ReadOpArg.apply(RegPC.Word++);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x27: // DAA
                RegAF.Word = TableDaa[RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x28: // JR Z, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (getRegFlagZ()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x29: // ADD IX, IX
                c.TI1 = RegIX.Word;
                c.TI2 = RegIX.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIX.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIX.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x2A: // LD IX, (nn)
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                RegIX.Low = ReadMemory.apply(c.TUS++);
                RegIX.High = ReadMemory.apply(c.TUS);
                RegWZ.Word = c.TUS;
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x2B: // DEC IX
                --RegIX.Word;
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x2C: // INC IXL
                RegAF.Low = (byte) (TableInc[++RegIX.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2D: // DEC IXL
                RegAF.Low = (byte) (TableDec[--RegIX.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2E: // LD IXL, n
                RegIX.Low = ReadOpArg.apply(RegPC.Word++);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2F: // CPL
                RegAF.High ^= (byte) 0xFF;
                setRegFlagH(true);
                setRegFlagN(true);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x30: // JR NC, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (!getRegFlagC()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x31: // LD SP, nn
                RegSP.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x32: // LD (nn), A
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                WriteMemory.accept((short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256), RegAF.High);
                break;
            case 0x33: // INC SP
                ++RegSP.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x34: // INC (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                c.TB = ReadMemory.apply(RegWZ.Word);
                RegAF.Low = (byte) (TableInc[++c.TB] | (RegAF.Low & 1));
                WriteMemory.accept(RegWZ.Word, c.TB);
                break;
            case 0x35: // DEC (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                c.TB = ReadMemory.apply(RegWZ.Word);
                RegAF.Low = (byte) (TableDec[--c.TB] | (RegAF.Low & 1));
                WriteMemory.accept(RegWZ.Word, c.TB);
                break;
            case 0x36: // LD (IX+d), n
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, ReadOpArg.apply(RegPC.Word++));
                break;
            case 0x37: // SCF
                setRegFlagH(false);
                setRegFlagN(false);
                setRegFlagC(true);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x38: // JR C, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (getRegFlagC()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x39: // ADD IX, SP
                c.TI1 = RegIX.Word;
                c.TI2 = RegSP.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIX.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIX.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x3A: // LD A, (nn)
                RegAF.High = ReadMemory.apply((short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256));
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                break;
            case 0x3B: // DEC SP
                --RegSP.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x3C: // INC A
                RegAF.Low = (byte) (TableInc[++RegAF.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3D: // DEC A
                RegAF.Low = (byte) (TableDec[--RegAF.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3E: // LD A, n
                RegAF.High = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x3F: // CCF
                setRegFlagH(getRegFlagC());
                setRegFlagN(false);
                setRegFlagC(getRegFlagC() ^ true);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x40: // LD B, B
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x41: // LD B, C
                RegBC.High = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x42: // LD B, D
                RegBC.High = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x43: // LD B, E
                RegBC.High = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x44: // LD B, IXH
                RegBC.High = RegIX.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x45: // LD B, IXL
                RegBC.High = RegIX.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x46: // LD B, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegBC.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x47: // LD B, A
                RegBC.High = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x48: // LD C, B
                RegBC.Low = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x49: // LD C, C
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4A: // LD C, D
                RegBC.Low = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4B: // LD C, E
                RegBC.Low = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4C: // LD C, IXH
                RegBC.Low = RegIX.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4D: // LD C, IXL
                RegBC.Low = RegIX.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4E: // LD C, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegBC.Low = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x4F: // LD C, A
                RegBC.Low = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x50: // LD D, B
                RegDE.High = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x51: // LD D, C
                RegDE.High = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x52: // LD D, D
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x53: // LD D, E
                RegDE.High = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x54: // LD D, IXH
                RegDE.High = RegIX.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x55: // LD D, IXL
                RegDE.High = RegIX.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x56: // LD D, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegDE.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x57: // LD D, A
                RegDE.High = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x58: // LD E, B
                RegDE.Low = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x59: // LD E, C
                RegDE.Low = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5A: // LD E, D
                RegDE.Low = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5B: // LD E, E
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5C: // LD E, IXH
                RegDE.Low = RegIX.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5D: // LD E, IXL
                RegDE.Low = RegIX.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5E: // LD E, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegDE.Low = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x5F: // LD E, A
                RegDE.Low = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x60: // LD IXH, B
                RegIX.High = RegBC.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x61: // LD IXH, C
                RegIX.High = RegBC.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x62: // LD IXH, D
                RegIX.High = RegDE.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x63: // LD IXH, E
                RegIX.High = RegDE.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x64: // LD IXH, IXH
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x65: // LD IXH, IXL
                RegIX.High = RegIX.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x66: // LD H, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegHL.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x67: // LD IXH, A
                RegIX.High = RegAF.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x68: // LD IXL, B
                RegIX.Low = RegBC.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x69: // LD IXL, C
                RegIX.Low = RegBC.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6A: // LD IXL, D
                RegIX.Low = RegDE.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6B: // LD IXL, E
                RegIX.Low = RegDE.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6C: // LD IXL, IXH
                RegIX.Low = RegIX.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6D: // LD IXL, IXL
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6E: // LD L, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegHL.Low = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x6F: // LD IXL, A
                RegIX.Low = RegAF.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x70: // LD (IX+d), B
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegBC.High);
                break;
            case 0x71: // LD (IX+d), C
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegBC.Low);
                break;
            case 0x72: // LD (IX+d), D
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegDE.High);
                break;
            case 0x73: // LD (IX+d), E
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegDE.Low);
                break;
            case 0x74: // LD (IX+d), H
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegHL.High);
                break;
            case 0x75: // LD (IX+d), L
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegHL.Low);
                break;
            case 0x76: // HALT
                Halt();
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x77: // LD (IX+d), A
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegAF.High);
                break;
            case 0x78: // LD A, B
                RegAF.High = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x79: // LD A, C
                RegAF.High = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7A: // LD A, D
                RegAF.High = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7B: // LD A, E
                RegAF.High = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7C: // LD A, IXH
                RegAF.High = RegIX.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7D: // LD A, IXL
                RegAF.High = RegIX.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7E: // LD A, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x7F: // LD A, A
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x80: // ADD A, B
                RegAF.Word = TableALU[0][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x81: // ADD A, C
                RegAF.Word = TableALU[0][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x82: // ADD A, D
                RegAF.Word = TableALU[0][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x83: // ADD A, E
                RegAF.Word = TableALU[0][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x84: // ADD A, IXH
                RegAF.Word = TableALU[0][RegAF.High][RegIX.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x85: // ADD A, IXL
                RegAF.Word = TableALU[0][RegAF.High][RegIX.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x86: // ADD A, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[0][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19; // modify 16 to 19
                break;
            case 0x87: // ADD A, A
                RegAF.Word = TableALU[0][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x88: // ADC A, B
                RegAF.Word = TableALU[1][RegAF.High][RegBC.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x89: // ADC A, C
                RegAF.Word = TableALU[1][RegAF.High][RegBC.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8A: // ADC A, D
                RegAF.Word = TableALU[1][RegAF.High][RegDE.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8B: // ADC A, E
                RegAF.Word = TableALU[1][RegAF.High][RegDE.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8C: // ADC A, IXH
                RegAF.Word = TableALU[1][RegAF.High][RegIX.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x8D: // ADC A, IXL
                RegAF.Word = TableALU[1][RegAF.High][RegIX.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x8E: // ADC A, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[1][RegAF.High][ReadMemory.apply(RegWZ.Word)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x8F: // ADC A, A
                RegAF.Word = TableALU[1][RegAF.High][RegAF.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x90: // SUB B
                RegAF.Word = TableALU[2][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x91: // SUB C
                RegAF.Word = TableALU[2][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x92: // SUB D
                RegAF.Word = TableALU[2][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x93: // SUB E
                RegAF.Word = TableALU[2][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x94: // SUB IXH
                RegAF.Word = TableALU[2][RegAF.High][RegIX.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x95: // SUB IXL
                RegAF.Word = TableALU[2][RegAF.High][RegIX.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x96: // SUB (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[2][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x97: // SUB A, A
                RegAF.Word = TableALU[2][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x98: // SBC A, B
                RegAF.Word = TableALU[3][RegAF.High][RegBC.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x99: // SBC A, C
                RegAF.Word = TableALU[3][RegAF.High][RegBC.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9A: // SBC A, D
                RegAF.Word = TableALU[3][RegAF.High][RegDE.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9B: // SBC A, E
                RegAF.Word = TableALU[3][RegAF.High][RegDE.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9C: // SBC A, IXH
                RegAF.Word = TableALU[3][RegAF.High][RegIX.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x9D: // SBC A, IXL
                RegAF.Word = TableALU[3][RegAF.High][RegIX.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x9E: // SBC A, (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[3][RegAF.High][ReadMemory.apply(RegWZ.Word)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x9F: // SBC A, A
                RegAF.Word = TableALU[3][RegAF.High][RegAF.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA0: // AND B
                RegAF.Word = TableALU[4][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA1: // AND C
                RegAF.Word = TableALU[4][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA2: // AND D
                RegAF.Word = TableALU[4][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA3: // AND E
                RegAF.Word = TableALU[4][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA4: // AND IXH
                RegAF.Word = TableALU[4][RegAF.High][RegIX.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xA5: // AND IXL
                RegAF.Word = TableALU[4][RegAF.High][RegIX.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xA6: // AND (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[4][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xA7: // AND A
                RegAF.Word = TableALU[4][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA8: // XOR B
                RegAF.Word = TableALU[5][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA9: // XOR C
                RegAF.Word = TableALU[5][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAA: // XOR D
                RegAF.Word = TableALU[5][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAB: // XOR E
                RegAF.Word = TableALU[5][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAC: // XOR IXH
                RegAF.Word = TableALU[5][RegAF.High][RegIX.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xAD: // XOR IXL
                RegAF.Word = TableALU[5][RegAF.High][RegIX.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xAE: // XOR (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[5][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xAF: // XOR A
                RegAF.Word = TableALU[5][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB0: // OR B
                RegAF.Word = TableALU[6][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB1: // OR C
                RegAF.Word = TableALU[6][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB2: // OR D
                RegAF.Word = TableALU[6][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB3: // OR E
                RegAF.Word = TableALU[6][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB4: // OR IXH
                RegAF.Word = TableALU[6][RegAF.High][RegIX.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xB5: // OR IXL
                RegAF.Word = TableALU[6][RegAF.High][RegIX.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xB6: // OR (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[6][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xB7: // OR A
                RegAF.Word = TableALU[6][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB8: // CP B
                RegAF.Word = TableALU[7][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB9: // CP C
                RegAF.Word = TableALU[7][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBA: // CP D
                RegAF.Word = TableALU[7][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBB: // CP E
                RegAF.Word = TableALU[7][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBC: // CP IXH
                RegAF.Word = TableALU[7][RegAF.High][RegIX.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xBD: // CP IXL
                RegAF.Word = TableALU[7][RegAF.High][RegIX.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xBE: // CP (IX+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                RegAF.Word = TableALU[7][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xBF: // CP A
                RegAF.Word = TableALU[7][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC0: // RET NZ
                if (!getRegFlagZ()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC1: // POP BC
                RegBC.Low = ReadMemory.apply(RegSP.Word++);
                RegBC.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC2: // JP NZ, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagZ()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC3: // JP nn
                RegPC.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC4: // CALL NZ, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xC5: // PUSH BC
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegBC.High);
                WriteMemory.accept(--RegSP.Word, RegBC.Low);
                break;
            case 0xC6: // ADD A, n
                RegAF.Word = TableALU[0][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xC7: // RST $00
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x00;
                break;
            case 0xC8: // RET Z
                if (getRegFlagZ()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC9: // RET
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCA: // JP Z, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagZ()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCB: // (Prefix)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                //++RegR;
                RegWZ.Word = (short) (RegIX.Word + c.Displacement);
                switch (ReadOpArg.apply(RegPC.Word++) & 0xff) {
                    case 0x00: // RLC (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x01: // RLC (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x02: // RLC (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x03: // RLC (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x04: // RLC (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x05: // RLC (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x06: // RLC (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x07: // RLC (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x08: // RRC (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x09: // RRC (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x0A: // RRC (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x0B: // RRC (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x0C: // RRC (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x0D: // RRC (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x0E: // RRC (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x0F: // RRC (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x10: // RL (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x11: // RL (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x12: // RL (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x13: // RL (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x14: // RL (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x15: // RL (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x16: // RL (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x17: // RL (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x18: // RR (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x19: // RR (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x1A: // RR (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x1B: // RR (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x1C: // RR (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x1D: // RR (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x1E: // RR (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x1F: // RR (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x20: // SLA (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x21: // SLA (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x22: // SLA (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x23: // SLA (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x24: // SLA (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x25: // SLA (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x26: // SLA (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x27: // SLA (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x28: // SRA (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x29: // SRA (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x2A: // SRA (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x2B: // SRA (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x2C: // SRA (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x2D: // SRA (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x2E: // SRA (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x2F: // SRA (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x30: // SL1 (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x31: // SL1 (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x32: // SL1 (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x33: // SL1 (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x34: // SL1 (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x35: // SL1 (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x36: // SL1 (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x37: // SL1 (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x38: // SRL (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.High = (byte) c.TUS;
                        break;
                    case 0x39: // SRL (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegBC.Low = (byte) c.TUS;
                        break;
                    case 0x3A: // SRL (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.High = (byte) c.TUS;
                        break;
                    case 0x3B: // SRL (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegDE.Low = (byte) c.TUS;
                        break;
                    case 0x3C: // SRL (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.High = (byte) c.TUS;
                        break;
                    case 0x3D: // SRL (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegHL.Low = (byte) c.TUS;
                        break;
                    case 0x3E: // SRL (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        break;
                    case 0x3F: // SRL (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                        WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                        RegAF.Low = (byte) c.TUS;
                        RegAF.High = (byte) c.TUS;
                        break;
                    case 0x40: // BIT 0, (IX+d)
                    case 0x41: // BIT 0, (IX+d)
                    case 0x42: // BIT 0, (IX+d)
                    case 0x43: // BIT 0, (IX+d)
                    case 0x44: // BIT 0, (IX+d)
                    case 0x45: // BIT 0, (IX+d)
                    case 0x46: // BIT 0, (IX+d)
                    case 0x47: // BIT 0, (IX+d)
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x01) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        break;
                    case 0x48: // BIT 1, (IX+d)
                    case 0x49: // BIT 1, (IX+d)
                    case 0x4A: // BIT 1, (IX+d)
                    case 0x4B: // BIT 1, (IX+d)
                    case 0x4C: // BIT 1, (IX+d)
                    case 0x4D: // BIT 1, (IX+d)
                    case 0x4E: // BIT 1, (IX+d)
                    case 0x4F: // BIT 1, (IX+d)
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x02) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        break;
                    case 0x50: // BIT 2, (IX+d)
                    case 0x51: // BIT 2, (IX+d)
                    case 0x52: // BIT 2, (IX+d)
                    case 0x53: // BIT 2, (IX+d)
                    case 0x54: // BIT 2, (IX+d)
                    case 0x55: // BIT 2, (IX+d)
                    case 0x56: // BIT 2, (IX+d)
                    case 0x57: // BIT 2, (IX+d)
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x04) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        break;
                    case 0x58: // BIT 3, (IX+d)
                    case 0x59: // BIT 3, (IX+d)
                    case 0x5A: // BIT 3, (IX+d)
                    case 0x5B: // BIT 3, (IX+d)
                    case 0x5C: // BIT 3, (IX+d)
                    case 0x5D: // BIT 3, (IX+d)
                    case 0x5E: // BIT 3, (IX+d)
                    case 0x5F: // BIT 3, (IX+d)
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x08) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        break;
                    case 0x60: // BIT 4, (IX+d)
                    case 0x61: // BIT 4, (IX+d)
                    case 0x62: // BIT 4, (IX+d)
                    case 0x63: // BIT 4, (IX+d)
                    case 0x64: // BIT 4, (IX+d)
                    case 0x65: // BIT 4, (IX+d)
                    case 0x66: // BIT 4, (IX+d)
                    case 0x67: // BIT 4, (IX+d)
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x10) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x68: // BIT 5, (IX+d)
                    case 0x69: // BIT 5, (IX+d)
                    case 0x6A: // BIT 5, (IX+d)
                    case 0x6B: // BIT 5, (IX+d)
                    case 0x6C: // BIT 5, (IX+d)
                    case 0x6D: // BIT 5, (IX+d)
                    case 0x6E: // BIT 5, (IX+d)
                    case 0x6F: // BIT 5, (IX+d)
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x20) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x70: // BIT 6, (IX+d)
                    case 0x71: // BIT 6, (IX+d)
                    case 0x72: // BIT 6, (IX+d)
                    case 0x73: // BIT 6, (IX+d)
                    case 0x74: // BIT 6, (IX+d)
                    case 0x75: // BIT 6, (IX+d)
                    case 0x76: // BIT 6, (IX+d)
                    case 0x77: // BIT 6, (IX+d)
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x40) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(false);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x78: // BIT 7, (IX+d)
                    case 0x79: // BIT 7, (IX+d)
                    case 0x7A: // BIT 7, (IX+d)
                    case 0x7B: // BIT 7, (IX+d)
                    case 0x7C: // BIT 7, (IX+d)
                    case 0x7D: // BIT 7, (IX+d)
                    case 0x7E: // BIT 7, (IX+d)
                    case 0x7F: // BIT 7, (IX+d)
                        c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x80) == 0;
                        setRegFlagN(false);
                        setRegFlagP(c.TBOOL);
                        setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                        setRegFlagH(true);
                        setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                        setRegFlagZ(c.TBOOL);
                        setRegFlagS(!c.TBOOL);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x80: // RES 0, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0x81: // RES 0, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0x82: // RES 0, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0x83: // RES 0, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0x84: // RES 0, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0x85: // RES 0, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0x86: // RES 0, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                        break;
                    case 0x87: // RES 0, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0x88: // RES 1, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0x89: // RES 1, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0x8A: // RES 1, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0x8B: // RES 1, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0x8C: // RES 1, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0x8D: // RES 1, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0x8E: // RES 1, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                        break;
                    case 0x8F: // RES 1, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0x90: // RES 2, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0x91: // RES 2, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0x92: // RES 2, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0x93: // RES 2, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0x94: // RES 2, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0x95: // RES 2, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0x96: // RES 2, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                        break;
                    case 0x97: // RES 2, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0x98: // RES 3, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0x99: // RES 3, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0x9A: // RES 3, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0x9B: // RES 3, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0x9C: // RES 3, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0x9D: // RES 3, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0x9E: // RES 3, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                        break;
                    case 0x9F: // RES 3, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xA0: // RES 4, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xA1: // RES 4, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xA2: // RES 4, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xA3: // RES 4, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xA4: // RES 4, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xA5: // RES 4, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xA6: // RES 4, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                        break;
                    case 0xA7: // RES 4, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xA8: // RES 5, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xA9: // RES 5, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xAA: // RES 5, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xAB: // RES 5, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xAC: // RES 5, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xAD: // RES 5, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xAE: // RES 5, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                        break;
                    case 0xAF: // RES 5, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xB0: // RES 6, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xB1: // RES 6, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xB2: // RES 6, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xB3: // RES 6, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xB4: // RES 6, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xB5: // RES 6, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xB6: // RES 6, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                        break;
                    case 0xB7: // RES 6, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xB8: // RES 7, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xB9: // RES 7, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xBA: // RES 7, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xBB: // RES 7, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xBC: // RES 7, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xBD: // RES 7, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xBE: // RES 7, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                        break;
                    case 0xBF: // RES 7, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xC0: // SET 0, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xC1: // SET 0, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xC2: // SET 0, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xC3: // SET 0, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xC4: // SET 0, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xC5: // SET 0, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xC6: // SET 0, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                        break;
                    case 0xC7: // SET 0, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xC8: // SET 1, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xC9: // SET 1, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xCA: // SET 1, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xCB: // SET 1, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xCC: // SET 1, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xCD: // SET 1, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xCE: // SET 1, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                        break;
                    case 0xCF: // SET 1, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xD0: // SET 2, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xD1: // SET 2, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xD2: // SET 2, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xD3: // SET 2, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xD4: // SET 2, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xD5: // SET 2, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xD6: // SET 2, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                        break;
                    case 0xD7: // SET 2, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xD8: // SET 3, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xD9: // SET 3, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xDA: // SET 3, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xDB: // SET 3, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xDC: // SET 3, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xDD: // SET 3, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xDE: // SET 3, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                        break;
                    case 0xDF: // SET 3, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xE0: // SET 4, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xE1: // SET 4, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xE2: // SET 4, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xE3: // SET 4, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xE4: // SET 4, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xE5: // SET 4, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xE6: // SET 4, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                        break;
                    case 0xE7: // SET 4, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xE8: // SET 5, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xE9: // SET 5, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xEA: // SET 5, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xEB: // SET 5, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xEC: // SET 5, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xED: // SET 5, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xEE: // SET 5, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                        break;
                    case 0xEF: // SET 5, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xF0: // SET 6, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xF1: // SET 6, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xF2: // SET 6, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xF3: // SET 6, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xF4: // SET 6, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xF5: // SET 6, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xF6: // SET 6, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                        break;
                    case 0xF7: // SET 6, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                    case 0xF8: // SET 7, (IX+d)→B
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegBC.High);
                        break;
                    case 0xF9: // SET 7, (IX+d)→C
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegBC.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegBC.Low);
                        break;
                    case 0xFA: // SET 7, (IX+d)→D
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegDE.High);
                        break;
                    case 0xFB: // SET 7, (IX+d)→E
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegDE.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegDE.Low);
                        break;
                    case 0xFC: // SET 7, (IX+d)→H
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegHL.High);
                        break;
                    case 0xFD: // SET 7, (IX+d)→L
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegHL.Low = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegHL.Low);
                        break;
                    case 0xFE: // SET 7, (IX+d)
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                        break;
                    case 0xFF: // SET 7, (IX+d)→A
                        totalExecutedCycles += 23;
                        pendingCycles -= 23;
                        RegAF.High = (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80);
                        WriteMemory.accept(RegWZ.Word, RegAF.High);
                        break;
                }
                break;
            case 0xCC: // CALL Z, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xCD: // CALL nn
                totalExecutedCycles += 17;
                pendingCycles -= 17;
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = c.TUS;
                break;
            case 0xCE: // ADC A, n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                RegAF.Word = TableALU[1][RegAF.High][ReadMemory.apply(RegPC.Word++)][getRegFlagC() ? 1 : 0];
                break;
            case 0xCF: // RST $08
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x08;
                break;
            case 0xD0: // RET NC
                if (!getRegFlagC()) {
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD1: // POP DE
                RegDE.Low = ReadMemory.apply(RegSP.Word++);
                RegDE.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD2: // JP NC, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagC()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD3: // OUT n, A
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteHardware.accept((short) (byte) ReadMemory.apply(RegPC.Word++), RegAF.High);
                break;
            case 0xD4: // CALL NC, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xD5: // PUSH DE
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegDE.High);
                WriteMemory.accept(--RegSP.Word, RegDE.Low);
                break;
            case 0xD6: // SUB n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                RegAF.Word = TableALU[2][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                break;
            case 0xD7: // RST $10
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x10;
                break;
            case 0xD8: // RET C
                if (getRegFlagC()) {
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD9: // EXX
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                c.TUS = RegBC.Word;
                RegBC.Word = RegAltBC.Word;
                RegAltBC.Word = c.TUS;
                c.TUS = RegDE.Word;
                RegDE.Word = RegAltDE.Word;
                RegAltDE.Word = c.TUS;
                c.TUS = RegHL.Word;
                RegHL.Word = RegAltHL.Word;
                RegAltHL.Word = c.TUS;
                break;
            case 0xDA: // JP C, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagC()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xDB: // IN A, n
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) | (RegAF.High << 8));
                RegAF.High = ReadHardware.apply(c.TUS);
                RegWZ.Word = (short) (c.TUS + 1);
                break;
            case 0xDC: // CALL C, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xDD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xDE: // SBC A, n
                RegAF.Word = TableALU[3][RegAF.High][ReadMemory.apply(RegPC.Word++)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xDF: // RST $18
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x18;
                break;
            case 0xE0: // RET PO
                if (!getRegFlagP()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE1: // POP IX
                RegIX.Low = ReadMemory.apply(RegSP.Word++);
                RegIX.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0xE2: // JP PO, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagP()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xE3: // EX (SP), IX
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = RegSP.Word;
                c.TBL = ReadMemory.apply(c.TUS++);
                c.TBH = ReadMemory.apply(c.TUS--);
                WriteMemory.accept(c.TUS++, RegIX.Low);
                WriteMemory.accept(c.TUS, RegIX.High);
                RegIX.Low = c.TBL;
                RegIX.High = c.TBH;
                RegWZ.Word = RegIX.Word;
                break;
            case 0xE4: // CALL C, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xE5: // PUSH IX
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(--RegSP.Word, RegIX.High);
                WriteMemory.accept(--RegSP.Word, RegIX.Low);
                break;
            case 0xE6: // AND n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                RegAF.Word = TableALU[4][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                break;
            case 0xE7: // RST $20
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x20;
                break;
            case 0xE8: // RET PE
                if (getRegFlagP()) {
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE9: // JP IX
                RegPC.Word = RegIX.Word;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEA: // JP PE, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagP()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xEB: // EX DE, HL
                c.TUS = RegDE.Word;
                RegDE.Word = RegHL.Word;
                RegHL.Word = c.TUS;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEC: // CALL PE, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagP()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xED: // (Prefix)
                ++RegR;
                switch (ReadOp.apply(RegPC.Word++) & 0xff) {
                    case 0x00: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x01: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x02: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x03: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x04: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x05: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x06: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x07: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x08: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x09: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x10: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x11: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x12: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x13: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x14: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x15: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x16: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x17: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x18: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x19: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x20: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x21: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x22: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x23: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x24: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x25: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x26: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x27: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x28: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x29: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x30: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x31: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x32: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x33: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x34: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x35: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x36: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x37: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x38: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x39: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x40: // IN B, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegBC.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegBC.High > 127);
                        setRegFlagZ(RegBC.High == 0);
                        setRegFlag5((RegBC.High & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegBC.High & 0x08) != 0);
                        setRegFlagP(TableParity[RegBC.High]);
                        setRegFlagN(false);
                        break;
                    case 0x41: // OUT C, B
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegBC.High);
                        break;
                    case 0x42: // SBC HL, BC
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegBC.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((RegHL.Word ^ RegBC.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegBC.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x43: // LD (nn), BC
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegBC.Low);
                        WriteMemory.accept(c.TUS, RegBC.High);
                        break;
                    case 0x44: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x45: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x46: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x47: // LD I, A
                        RegI = RegAF.High;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x48: // IN C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegBC.Low = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegBC.Low > 127);
                        setRegFlagZ(RegBC.Low == 0);
                        setRegFlag5((RegBC.Low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegBC.Low & 0x08) != 0);
                        setRegFlagP(TableParity[RegBC.Low]);
                        setRegFlagN(false);
                        break;
                    case 0x49: // OUT C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegBC.Low);
                        break;
                    case 0x4A: // ADC HL, BC
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegBC.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x4B: // LD BC, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegBC.Low = ReadMemory.apply(c.TUS++);
                        RegBC.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x4C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x4E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4F: // LD R, A
                        RegR = RegAF.High;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x50: // IN D, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegDE.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegDE.High > 127);
                        setRegFlagZ(RegDE.High == 0);
                        setRegFlag5((RegDE.High & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegDE.High & 0x08) != 0);
                        setRegFlagP(TableParity[RegDE.High]);
                        setRegFlagN(false);
                        break;
                    case 0x51: // OUT C, D
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegDE.High);
                        break;
                    case 0x52: // SBC HL, DE
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegDE.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((RegHL.Word ^ RegDE.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegDE.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x53: // LD (nn), DE
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegDE.Low);
                        WriteMemory.accept(c.TUS, RegDE.High);
                        break;
                    case 0x54: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x55: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x56: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x57: // LD A, I
                        RegAF.High = RegI;
                        setRegFlagS((RegI & 0xff) > 127);
                        setRegFlagZ(RegI == 0);
                        setRegFlag5(((RegI & 0x20) != 0));
                        setRegFlagH(false);
                        setRegFlag3(((RegI & 0x08) != 0));
                        setRegFlagN(false);
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x58: // IN E, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegDE.Low = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegDE.Low > 127);
                        setRegFlagZ(RegDE.Low == 0);
                        setRegFlag5((RegDE.Low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegDE.Low & 0x08) != 0);
                        setRegFlagP(TableParity[RegDE.Low]);
                        setRegFlagN(false);
                        break;
                    case 0x59: // OUT C, E
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegDE.Low);
                        break;
                    case 0x5A: // ADC HL, DE
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegDE.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x5B: // LD DE, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegDE.Low = ReadMemory.apply(c.TUS++);
                        RegDE.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x5C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x5E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5F: // LD A, R
                        RegAF.High = (byte) ((RegR & 0x7F) | RegR2);
                        setRegFlagS((RegR2 == (byte) 0x80));
                        setRegFlagZ((byte) ((RegR & 0x7F) | RegR2) == 0);
                        setRegFlagH(false);
                        setRegFlag5(((RegR & 0x20) != 0));
                        setRegFlagN(false);
                        setRegFlag3(((RegR & 0x08) != 0));
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x60: // IN H, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegHL.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegHL.High > 127);
                        setRegFlagZ(RegHL.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegHL.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegHL.High & 0x08) != 0);
                        setRegFlag5((RegHL.High & 0x20) != 0);
                        break;
                    case 0x61: // OUT C, H
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegHL.High);
                        break;
                    case 0x62: // SBC HL, HL
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegHL.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((RegHL.Word ^ RegHL.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegHL.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x63: // LD (nn), HL
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegHL.Low);
                        WriteMemory.accept(c.TUS, RegHL.High);
                        break;
                    case 0x64: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x65: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x66: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x67: // RRD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.TB1 = RegAF.High;
                        c.TB2 = ReadMemory.apply(RegHL.Word);
                        WriteMemory.accept(RegHL.Word, (byte) ((c.TB2 >> 4) + (c.TB1 << 4)));
                        RegAF.High = (byte) ((c.TB1 & 0xF0) + (c.TB2 & 0x0F));
                        setRegFlagS(RegAF.High > 127);
                        setRegFlagZ(RegAF.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegAF.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        break;
                    case 0x68: // IN L, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegHL.Low = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegHL.Low > 127);
                        setRegFlagZ(RegHL.Low == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegHL.Low]);
                        setRegFlagN(false);
                        setRegFlag3((RegHL.Low & 0x08) != 0);
                        setRegFlag5((RegHL.Low & 0x20) != 0);
                        break;
                    case 0x69: // OUT C, L
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegHL.Low);
                        break;
                    case 0x6A: // ADC HL, HL
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegHL.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x6B: // LD HL, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegHL.Low = ReadMemory.apply(c.TUS++);
                        RegHL.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x6C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x6E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6F: // RLD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.TB1 = RegAF.High;
                        c.TB2 = ReadMemory.apply(RegHL.Word);
                        WriteMemory.accept(RegHL.Word, (byte) ((c.TB1 & 0x0F) + (c.TB2 << 4)));
                        RegAF.High = (byte) ((c.TB1 & 0xF0) + (c.TB2 >> 4));
                        setRegFlagS(RegAF.High > 127);
                        setRegFlagZ(RegAF.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegAF.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        break;
                    case 0x70: // IN 0, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        setRegFlagS((c.TB & 0xff) > 127);
                        setRegFlagZ(c.TB == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[c.TB]);
                        setRegFlagN(false);
                        setRegFlag3((c.TB & 0x08) != 0);
                        setRegFlag5((c.TB & 0x20) != 0);
                        break;
                    case 0x71: // OUT C, 0
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, (byte) 0);
                        break;
                    case 0x72: // SBC HL, SP
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegSP.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((RegHL.Word ^ RegSP.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegSP.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x73: // LD (nn), SP
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegSP.Low);
                        WriteMemory.accept(c.TUS, RegSP.High);
                        break;
                    case 0x74: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x75: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x76: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x77: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x78: // IN A, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegAF.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegAF.High > 127);
                        setRegFlagZ(RegAF.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegAF.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        break;
                    case 0x79: // OUT C, A
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegAF.High);
                        break;
                    case 0x7A: // ADC HL, SP
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegSP.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x7B: // LD SP, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegSP.Low = ReadMemory.apply(c.TUS++);
                        RegSP.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x7C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x7E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x80: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x81: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x82: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x83: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x84: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x85: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x86: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x87: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x88: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x89: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x90: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x91: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x92: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x93: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x94: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x95: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x96: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x97: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x98: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x99: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA0: // LDI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        WriteMemory.accept(RegDE.Word++, c.TB1 = ReadMemory.apply(RegHL.Word++));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA1: // CPI
                        c.TB1 = ReadMemory.apply(RegHL.Word++);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS((c.TB2 & 0xff) > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xA2: // INI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word++, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low + 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xA3: // OUTI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word++);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(IsX(RegBC.High));
                        setRegFlagH(false);
                        setRegFlag5(IsY(RegBC.High));
                        setRegFlagZ(RegBC.High == 0);
                        setRegFlagS(IsS(RegBC.High));
                        c.TUS = (short) (RegHL.Low + c.TB);
                        if (IsS(c.TB)) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xA4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA8: // LDD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        WriteMemory.accept(RegDE.Word--, c.TB1 = ReadMemory.apply(RegHL.Word--));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA9: // CPD
                        c.TB1 = ReadMemory.apply(RegHL.Word--);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        RegWZ.Word--;
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS(c.TB2 > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xAA: // IND
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word--, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low - 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xAB: // OUTD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word--);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        c.TUS = (short) (RegHL.Low + c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xAC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB0: // LDIR
                        WriteMemory.accept(RegDE.Word++, c.TB1 = ReadMemory.apply(RegHL.Word++));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (RegBC.Word != 0) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB1: // CPIR
                        c.TB1 = ReadMemory.apply(RegHL.Word++);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS(c.TB2 > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        if (RegBC.Word != 0 && !getRegFlagZ()) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB2: // INIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word++, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low + 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB3: // OTIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word++);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(IsX(RegBC.High));
                        setRegFlagH(false);
                        setRegFlag5(IsY(RegBC.High));
                        setRegFlagZ(RegBC.High == 0);
                        setRegFlagS(IsS(RegBC.High));
                        c.TUS = (short) (RegHL.Low + c.TB);
                        if (IsS(c.TB)) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB8: // LDDR
                        WriteMemory.accept(RegDE.Word--, c.TB1 = ReadMemory.apply(RegHL.Word--));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (RegBC.Word != 0) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB9: // CPDR
                        c.TB1 = ReadMemory.apply(RegHL.Word--);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS(c.TB2 > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        if (RegBC.Word != 0 && !getRegFlagZ()) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xBA: // INDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word--, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low - 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBB: // OTDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word--);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        c.TUS = (short) (RegHL.Low + c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xED: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                }
                break;
            case 0xEE: // XOR n
                RegAF.Word = TableALU[5][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xEF: // RST $28
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x28;
                break;
            case 0xF0: // RET P
                if (!getRegFlagS()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF1: // POP AF
                RegAF.Low = ReadMemory.apply(RegSP.Word++);
                RegAF.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF2: // JP P, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagS()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF3: // DI
                iff1 = iff2 = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF4: // CALL P, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xF5: // PUSH AF
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegAF.High);
                WriteMemory.accept(--RegSP.Word, RegAF.Low);
                break;
            case 0xF6: // OR n
                RegAF.Word = TableALU[6][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xF7: // RST $30
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x30;
                break;
            case 0xF8: // RET M
                if (getRegFlagS()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF9: // LD SP, IX
                RegSP.Word = RegIX.Word;
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFA: // JP M, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagS()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFB: // EI
                iff1 = iff2 = true;
                Interruptable = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFC: // CALL M, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xFD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xFE: // CP n
                RegAF.Word = TableALU[7][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xFF: // RST $38
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x38;
                break;
        }
    }

    private void processED(Context c) {
        switch (ReadOp.apply(RegPC.Word++) & 0xff) {
            case 0x00: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x01: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x02: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x03: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x04: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x05: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x06: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x07: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x08: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x09: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x10: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x11: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x12: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x13: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x14: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x15: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x16: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x17: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x18: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x19: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x20: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x21: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x22: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x23: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x24: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x25: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x26: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x27: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x28: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x29: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x30: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x31: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x32: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x33: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x34: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x35: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x36: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x37: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x38: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x39: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x40: // IN B, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegBC.High = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegBC.High > 127);
                setRegFlagZ(RegBC.High == 0);
                setRegFlag5((RegBC.High & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((RegBC.High & 0x08) != 0);
                setRegFlagP(TableParity[RegBC.High]);
                setRegFlagN(false);
                break;
            case 0x41: // OUT C, B
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegBC.High);
                break;
            case 0x42: // SBC HL, BC
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegBC.Word;
                c.TIR = c.TI1 - c.TI2;
                if (getRegFlagC()) {
                    --c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((RegHL.Word ^ RegBC.Word ^ c.TUS) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) RegHL.Word - (int) RegBC.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x43: // LD (nn), BC
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                WriteMemory.accept(c.TUS++, RegBC.Low);
                WriteMemory.accept(c.TUS, RegBC.High);
                RegWZ.Word = c.TUS;
                break;
            case 0x44: // NEG
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                RegAF.Word = TableNeg[RegAF.Word];
                break;
            case 0x45: // RETN
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x46: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x47: // LD I, A
                RegI = RegAF.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x48: // IN C, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegBC.Low = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegBC.Low > 127);
                setRegFlagZ(RegBC.Low == 0);
                setRegFlag5((RegBC.Low & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((RegBC.Low & 0x08) != 0);
                setRegFlagP(TableParity[RegBC.Low]);
                setRegFlagN(false);
                break;
            case 0x49: // OUT C, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegBC.Low);
                break;
            case 0x4A: // ADC HL, BC
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegBC.Word;
                c.TIR = c.TI1 + c.TI2;
                if (getRegFlagC()) {
                    ++c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x4B: // LD BC, (nn)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                RegBC.Low = ReadMemory.apply(c.TUS++);
                RegBC.High = ReadMemory.apply(c.TUS);
                RegWZ.Word = c.TUS;
                break;
            case 0x4C: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x4E: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4F: // LD R, A
                RegR = RegAF.High;
                RegR2 = (byte) (RegAF.High & 0x80);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x50: // IN D, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegDE.High = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegDE.High > 127);
                setRegFlagZ(RegDE.High == 0);
                setRegFlag5((RegDE.High & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((RegDE.High & 0x08) != 0);
                setRegFlagP(TableParity[RegDE.High]);
                setRegFlagN(false);
                break;
            case 0x51: // OUT C, D
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegDE.High);
                break;
            case 0x52: // SBC HL, DE
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegDE.Word;
                c.TIR = c.TI1 - c.TI2;
                if (getRegFlagC()) {
                    --c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((RegHL.Word ^ RegDE.Word ^ c.TUS) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) RegHL.Word - (int) RegDE.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x53: // LD (nn), DE
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                WriteMemory.accept(c.TUS++, RegDE.Low);
                WriteMemory.accept(c.TUS, RegDE.High);
                RegWZ.Word = c.TUS;
                break;
            case 0x54: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x55: // RETN
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0x56: // IM $1
                interruptMode = 1;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x57: // LD A, I
                RegAF.High = RegI;
                setRegFlagS((RegI & 0xff) > 127);
                setRegFlagZ(RegI == 0);
                setRegFlag5(((RegI & 0x20) != 0));
                setRegFlagH(false);
                setRegFlag3(((RegI & 0x08) != 0));
                setRegFlagN(false);
                setRegFlagP(iff2);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x58: // IN E, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegDE.Low = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegDE.Low > 127);
                setRegFlagZ(RegDE.Low == 0);
                setRegFlag5((RegDE.Low & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((RegDE.Low & 0x08) != 0);
                setRegFlagP(TableParity[RegDE.Low]);
                setRegFlagN(false);
                break;
            case 0x59: // OUT C, E
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegDE.Low);
                break;
            case 0x5A: // ADC HL, DE
                c.TI1 = RegHL.Word;
                c.TI2 = RegDE.Word;
                c.TIR = c.TI1 + c.TI2;
                if (getRegFlagC()) {
                    ++c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x5B: // LD DE, (nn)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                RegDE.Low = ReadMemory.apply(c.TUS++);
                RegDE.High = ReadMemory.apply(c.TUS);
                RegWZ.Word = c.TUS;
                break;
            case 0x5C: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x5E: // IM $2
                interruptMode = 2;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5F: // LD A, R
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                RegAF.High = (byte) ((RegR & 0x7F) | RegR2);
                setRegFlagS((RegR2 == (byte) 0x80));
                setRegFlagZ((byte) ((RegR & 0x7F) | RegR2) == 0);
                setRegFlagH(false);
                setRegFlag5(((RegR & 0x20) != 0));
                setRegFlagN(false);
                setRegFlag3(((RegR & 0x08) != 0));
                setRegFlagP(iff2);
                break;
            case 0x60: // IN H, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegHL.High = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegHL.High > 127);
                setRegFlagZ(RegHL.High == 0);
                setRegFlagH(false);
                setRegFlagP(TableParity[RegHL.High]);
                setRegFlagN(false);
                setRegFlag3((RegHL.High & 0x08) != 0);
                setRegFlag5((RegHL.High & 0x20) != 0);
                break;
            case 0x61: // OUT C, H
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegHL.High);
                break;
            case 0x62: // SBC HL, HL
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegHL.Word;
                c.TIR = c.TI1 - c.TI2;
                if (getRegFlagC()) {
                    --c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((RegHL.Word ^ RegHL.Word ^ c.TUS) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) RegHL.Word - (int) RegHL.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x63: // LD (nn), HL
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                WriteMemory.accept(c.TUS++, RegHL.Low);
                WriteMemory.accept(c.TUS, RegHL.High);
                RegWZ.Word = c.TUS;
                break;
            case 0x64: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x65: // RETN
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x66: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x67: // RRD
                totalExecutedCycles += 18;
                pendingCycles -= 18;
                c.TB1 = RegAF.High;
                c.TB2 = ReadMemory.apply(RegHL.Word);
                RegWZ.Word = (short) (RegHL.Word + 1);
                WriteMemory.accept(RegHL.Word, (byte) ((c.TB2 >> 4) + (c.TB1 << 4)));
                RegAF.High = (byte) ((c.TB1 & 0xF0) + (c.TB2 & 0x0F));
                setRegFlagS(RegAF.High > 127);
                setRegFlagZ(RegAF.High == 0);
                setRegFlagH(false);
                setRegFlagP(TableParity[RegAF.High]);
                setRegFlagN(false);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                break;
            case 0x68: // IN L, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegHL.Low = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegHL.Low > 127);
                setRegFlagZ(RegHL.Low == 0);
                setRegFlagH(false);
                setRegFlagP(TableParity[RegHL.Low]);
                setRegFlagN(false);
                setRegFlag3((RegHL.Low & 0x08) != 0);
                setRegFlag5((RegHL.Low & 0x20) != 0);
                break;
            case 0x69: // OUT C, L
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegHL.Low);
                break;
            case 0x6A: // ADC HL, HL
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegHL.Word;
                c.TIR = c.TI1 + c.TI2;
                if (getRegFlagC()) {
                    ++c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x6B: // LD HL, (nn)
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                RegHL.Low = ReadMemory.apply(c.TUS++);
                RegHL.High = ReadMemory.apply(c.TUS);
                RegWZ.Word = c.TUS;
                break;
            case 0x6C: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x6E: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6F: // RLD
                totalExecutedCycles += 18;
                pendingCycles -= 18;
                c.TB1 = RegAF.High;
                c.TB2 = ReadMemory.apply(RegHL.Word);
                RegWZ.Word = (short) (RegHL.Word + 1);
                WriteMemory.accept(RegHL.Word, (byte) ((c.TB1 & 0x0F) + (c.TB2 << 4)));
                RegAF.High = (byte) ((c.TB1 & 0xF0) + (c.TB2 >> 4));
                setRegFlagS(RegAF.High > 127);
                setRegFlagZ(RegAF.High == 0);
                setRegFlagH(false);
                setRegFlagP(TableParity[RegAF.High]);
                setRegFlagN(false);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                break;
            case 0x70: // IN 0, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                c.TB = ReadHardware.apply(RegBC.Word);
                setRegFlagS((c.TB & 0xff) > 127);
                setRegFlagZ(c.TB == 0);
                setRegFlagH(false);
                setRegFlagP(TableParity[c.TB]);
                setRegFlagN(false);
                setRegFlag3((c.TB & 0x08) != 0);
                setRegFlag5((c.TB & 0x20) != 0);
                break;
            case 0x71: // OUT C, 0
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, (byte) 0);
                break;
            case 0x72: // SBC HL, SP
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegSP.Word;
                c.TIR = c.TI1 - c.TI2;
                if (getRegFlagC()) {
                    --c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((RegHL.Word ^ RegSP.Word ^ c.TUS) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) RegHL.Word - (int) RegSP.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x73: // LD (nn), SP
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                WriteMemory.accept(c.TUS++, RegSP.Low);
                WriteMemory.accept(c.TUS, RegSP.High);
                RegWZ.Word = c.TUS;
                break;
            case 0x74: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x75: // RETN
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x76: // IM $1
                interruptMode = 1;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x77: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x78: // IN A, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                RegAF.High = ReadHardware.apply(RegBC.Word);
                setRegFlagS(RegAF.High > 127);
                setRegFlagZ(RegAF.High == 0);
                setRegFlagH(false);
                setRegFlagP(TableParity[RegAF.High]);
                setRegFlagN(false);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                RegWZ.Word = (short) (RegBC.Word + 1);
                break;
            case 0x79: // OUT C, A
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                WriteHardware.accept(RegBC.Word, RegAF.High);
                RegWZ.Word = (short) (RegBC.Word + 1);
                break;
            case 0x7A: // ADC HL, SP
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.TI1 = RegHL.Word;
                c.TI2 = RegSP.Word;
                c.TIR = c.TI1 + c.TI2;
                if (getRegFlagC()) {
                    ++c.TIR;
                    ++c.TI2;
                }
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegHL.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                setRegFlagS(c.TUS > 32767);
                setRegFlagZ(c.TUS == 0);
                RegHL.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                break;
            case 0x7B: // LD SP, (nn)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                RegSP.Low = ReadMemory.apply(c.TUS++);
                RegSP.High = ReadMemory.apply(c.TUS);
                RegWZ.Word = c.TUS;
                break;
            case 0x7C: // NEG
                RegAF.Word = TableNeg[RegAF.Word];
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                RegWZ.Word = RegPC.Word;
                iff1 = iff2;
                break;
            case 0x7E: // IM $2
                interruptMode = 2;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x80: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x81: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x82: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x83: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x84: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x85: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x86: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x87: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x88: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x89: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x90: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x91: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x92: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x93: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x94: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x95: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x96: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x97: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x98: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x99: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA0: // LDI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                WriteMemory.accept(RegDE.Word++, c.TB1 = ReadMemory.apply(RegHL.Word++));
                c.TB1 += RegAF.High;
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                break;
            case 0xA1: // CPI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB1 = ReadMemory.apply(RegHL.Word++);
                c.TB2 = (byte) (RegAF.High - c.TB1);
                RegWZ.Word++;
                setRegFlagN(true);
                setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                setRegFlagZ(c.TB2 == 0);
                setRegFlagS((c.TB2 & 0xff) > 127);
                c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                break;
            case 0xA2: // INI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadHardware.apply(RegBC.Word);
                RegWZ.Word = (short) (RegBC.Word + 1);
                --RegBC.High;
                WriteMemory.accept(RegHL.Word++, c.TB);
                setRegFlagZ(RegBC.High == 0);
                c.TUS = (short) (((RegBC.Low + 1) & 0xff) + c.TB);
                if ((c.TB & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                break;
            case 0xA3: // OUTI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadMemory.apply(RegHL.Word++);
                --RegBC.High;
                RegWZ.Word = (short) (RegBC.Word + 1);
                WriteHardware.accept(RegBC.Word, c.TB);
                setRegFlagC(false);
                setRegFlagN(false);
                setRegFlag3(IsX(RegBC.High));
                setRegFlagH(false);
                setRegFlag5(IsY(RegBC.High));
                setRegFlagZ(RegBC.High == 0);
                setRegFlagS(IsS(RegBC.High));
                c.TUS = (short) (RegHL.Low + c.TB);
                if (IsS(c.TB)) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagC(true);
                    setRegFlagH(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                break;
            case 0xA4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA8: // LDD
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                WriteMemory.accept(RegDE.Word--, c.TB1 = ReadMemory.apply(RegHL.Word--));
                c.TB1 += RegAF.High;
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                break;
            case 0xA9: // CPD
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB1 = ReadMemory.apply(RegHL.Word--);
                c.TB2 = (byte) (RegAF.High - c.TB1);
                RegWZ.Word--;
                setRegFlagN(true);
                setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                setRegFlagZ(c.TB2 == 0);
                setRegFlagS(c.TB2 > 127);
                c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                break;
            case 0xAA: // IND
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadHardware.apply(RegBC.Word);
                RegWZ.Word = (short) (RegBC.Word - 1);
                --RegBC.High;
                WriteMemory.accept(RegHL.Word--, c.TB);
                setRegFlagZ(RegBC.High == 0);
                c.TUS = (short) (((RegBC.Low - 1) & 0xff) + c.TB);
                if ((c.TB & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                break;
            case 0xAB: // OUTD
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadMemory.apply(RegHL.Word--);
                WriteHardware.accept(RegBC.Word, c.TB);
                --RegBC.High;
                RegWZ.Word = (short) (RegBC.Word - 1);
                c.TUS = (short) (RegHL.Low + c.TB);
                setRegFlagZ(RegBC.High == 0);
                if ((c.TB & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                break;
            case 0xAC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB0: // LDIR
                WriteMemory.accept(RegDE.Word++, c.TB1 = ReadMemory.apply(RegHL.Word++));
                c.TB1 += RegAF.High;
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                if (RegBC.Word != 0) {
                    RegPC.Word -= 2;
                    RegWZ.Word = (short) (RegPC.Word + 1);
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xB1: // CPIR
                c.TB1 = ReadMemory.apply(RegHL.Word++);
                c.TB2 = (byte) (RegAF.High - c.TB1);
                RegWZ.Word++;
                setRegFlagN(true);
                setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                setRegFlagZ(c.TB2 == 0);
                setRegFlagS(c.TB2 > 127);
                c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                if (RegBC.Word != 0 && !getRegFlagZ()) {
                    RegPC.Word -= 2;
                    RegWZ.Word = (short) (RegPC.Word + 1);
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xB2: // INIR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadHardware.apply(RegBC.Word);
                RegWZ.Word = (short) (RegBC.Word + 1);
                --RegBC.High;
                WriteMemory.accept(RegHL.Word++, c.TB);
                setRegFlagZ(RegBC.High == 0);
                c.TUS = (short) (((RegBC.Low + 1) & 0xff) + c.TB);
                if ((c.TB & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                if (RegBC.High != 0) {
                    RegPC.Word -= 2;
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xB3: // OTIR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadMemory.apply(RegHL.Word++);
                --RegBC.High;
                RegWZ.Word = (short) (RegBC.Word + 1);
                WriteHardware.accept(RegBC.Word, c.TB);
                setRegFlagC(false);
                setRegFlagN(false);
                setRegFlag3(IsX(RegBC.High));
                setRegFlagH(false);
                setRegFlag5(IsY(RegBC.High));
                setRegFlagZ(RegBC.High == 0);
                setRegFlagS(IsS(RegBC.High));
                c.TUS = (short) (RegHL.Low + c.TB);
                if (IsS(c.TB)) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagC(true);
                    setRegFlagH(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                if (RegBC.High != 0) {
                    RegPC.Word -= 2;
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xB4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB8: // LDDR
                WriteMemory.accept(RegDE.Word--, c.TB1 = ReadMemory.apply(RegHL.Word--));
                c.TB1 += RegAF.High;
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                if (RegBC.Word != 0) {
                    RegPC.Word -= 2;
                    RegWZ.Word = (short) (RegPC.Word + 1);
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xB9: // CPDR
                c.TB1 = ReadMemory.apply(RegHL.Word--);
                c.TB2 = (byte) (RegAF.High - c.TB1);
                RegWZ.Word--;
                setRegFlagN(true);
                setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                setRegFlagZ(c.TB2 == 0);
                setRegFlagS(c.TB2 > 127);
                c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                setRegFlag5((c.TB1 & 0x02) != 0);
                setRegFlag3((c.TB1 & 0x08) != 0);
                --RegBC.Word;
                setRegFlagP(RegBC.Word != 0);
                if (RegBC.Word != 0 && !getRegFlagZ()) {
                    RegPC.Word -= 2;
                    RegWZ.Word = (short) (RegPC.Word + 1);
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xBA: // INDR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadHardware.apply(RegBC.Word);
                RegWZ.Word = (short) (RegBC.Word - 1);
                --RegBC.High;
                WriteMemory.accept(RegHL.Word--, c.TB);
                setRegFlagZ(RegBC.High == 0);
                c.TUS = (short) (((RegBC.Low - 1) & 0xff) + c.TB);
                if ((c.TB & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                if (RegBC.High != 0) {
                    RegPC.Word -= 2;
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xBB: // OTDR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.TB = ReadMemory.apply(RegHL.Word--);
                WriteHardware.accept(RegBC.Word, c.TB);
                --RegBC.High;
                RegWZ.Word = (short) (RegBC.Word - 1);
                c.TUS = (short) (RegHL.Low + c.TB);
                setRegFlagZ(RegBC.High == 0);
                if ((c.TB & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.TUS & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                if (RegBC.High != 0) {
                    RegPC.Word -= 2;
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xBC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xED: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
        }
    }

    private void processFDCB(Context c) {
        switch (ReadOpArg.apply(RegPC.Word++) & 0xff) {
            case 0x00: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x01: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x02: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x03: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x04: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x05: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x06: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x07: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][0][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x08: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x09: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x0A: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x0B: // RRC (IY+d)
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                break;
            case 0x0C: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x0D: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x0E: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x0F: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][1][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x10: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x11: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x12: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x13: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x14: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x15: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x16: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x17: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][2][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x18: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x19: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1A: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1B: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1C: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1D: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1E: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x1F: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][3][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x20: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x21: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x22: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x23: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x24: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x25: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x26: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x27: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][4][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x28: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x29: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2A: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2B: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2C: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2D: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2E: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x2F: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][5][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x30: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x31: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x32: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x33: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x34: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x35: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x36: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x37: // SL1 (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][6][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x38: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x39: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3A: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3B: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3C: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3D: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3E: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x3F: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = TableRotShift[1][7][RegAF.Low + 256 * ReadMemory.apply(RegWZ.Word)];
                WriteMemory.accept(RegWZ.Word, (byte) (c.TUS >> 8));
                RegAF.Low = (byte) c.TUS;
                break;
            case 0x40: // BIT 0, (IY+d)
            case 0x41: // BIT 0, (IY+d)
            case 0x42: // BIT 0, (IY+d)
            case 0x43: // BIT 0, (IY+d)
            case 0x44: // BIT 0, (IY+d)
            case 0x45: // BIT 0, (IY+d)
            case 0x46: // BIT 0, (IY+d)
            case 0x47: // BIT 0, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x01) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x48: // BIT 1, (IY+d)
            case 0x49: // BIT 1, (IY+d)
            case 0x4A: // BIT 1, (IY+d)
            case 0x4B: // BIT 1, (IY+d)
            case 0x4C: // BIT 1, (IY+d)
            case 0x4D: // BIT 1, (IY+d)
            case 0x4E: // BIT 1, (IY+d)
            case 0x4F: // BIT 1, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x02) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x50: // BIT 2, (IY+d)
            case 0x51: // BIT 2, (IY+d)
            case 0x52: // BIT 2, (IY+d)
            case 0x53: // BIT 2, (IY+d)
            case 0x54: // BIT 2, (IY+d)
            case 0x55: // BIT 2, (IY+d)
            case 0x56: // BIT 2, (IY+d)
            case 0x57: // BIT 2, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x04) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x58: // BIT 3, (IY+d)
            case 0x59: // BIT 3, (IY+d)
            case 0x5A: // BIT 3, (IY+d)
            case 0x5B: // BIT 3, (IY+d)
            case 0x5C: // BIT 3, (IY+d)
            case 0x5D: // BIT 3, (IY+d)
            case 0x5E: // BIT 3, (IY+d)
            case 0x5F: // BIT 3, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x08) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x60: // BIT 4, (IY+d)
            case 0x61: // BIT 4, (IY+d)
            case 0x62: // BIT 4, (IY+d)
            case 0x63: // BIT 4, (IY+d)
            case 0x64: // BIT 4, (IY+d)
            case 0x65: // BIT 4, (IY+d)
            case 0x66: // BIT 4, (IY+d)
            case 0x67: // BIT 4, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x10) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x68: // BIT 5, (IY+d)
            case 0x69: // BIT 5, (IY+d)
            case 0x6A: // BIT 5, (IY+d)
            case 0x6B: // BIT 5, (IY+d)
            case 0x6C: // BIT 5, (IY+d)
            case 0x6D: // BIT 5, (IY+d)
            case 0x6E: // BIT 5, (IY+d)
            case 0x6F: // BIT 5, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x20) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x70: // BIT 6, (IY+d)
            case 0x71: // BIT 6, (IY+d)
            case 0x72: // BIT 6, (IY+d)
            case 0x73: // BIT 6, (IY+d)
            case 0x74: // BIT 6, (IY+d)
            case 0x75: // BIT 6, (IY+d)
            case 0x76: // BIT 6, (IY+d)
            case 0x77: // BIT 6, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x40) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x78: // BIT 7, (IY+d)
            case 0x79: // BIT 7, (IY+d)
            case 0x7A: // BIT 7, (IY+d)
            case 0x7B: // BIT 7, (IY+d)
            case 0x7C: // BIT 7, (IY+d)
            case 0x7D: // BIT 7, (IY+d)
            case 0x7E: // BIT 7, (IY+d)
            case 0x7F: // BIT 7, (IY+d)
                c.TBOOL = (ReadMemory.apply(RegWZ.Word) & 0x80) == 0;
                setRegFlagN(false);
                setRegFlagP(c.TBOOL);
                setRegFlag3(((RegWZ.Word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((RegWZ.Word >> 8) & 0x20) != 0);
                setRegFlagZ(c.TBOOL);
                setRegFlagS(!c.TBOOL);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x80: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x81: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x82: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x83: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x84: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x85: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x86: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x87: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x01));
                break;
            case 0x88: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x89: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x8A: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x8B: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x8C: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x8D: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x8E: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x8F: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x02));
                break;
            case 0x90: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x91: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x92: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x93: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x94: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x95: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x96: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x97: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x04));
                break;
            case 0x98: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x99: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x9A: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x9B: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x9C: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x9D: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x9E: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0x9F: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x08));
                break;
            case 0xA0: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA1: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA2: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA3: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA4: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA5: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA6: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA7: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x10));
                break;
            case 0xA8: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xA9: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xAA: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xAB: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xAC: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xAD: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xAE: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xAF: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x20));
                break;
            case 0xB0: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB1: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB2: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB3: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB4: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB5: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB6: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB7: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x40));
                break;
            case 0xB8: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xB9: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xBA: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xBB: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xBC: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xBD: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xBE: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xBF: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) & (byte) ~0x80));
                break;
            case 0xC0: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC1: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC2: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC3: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC4: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC5: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC6: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC7: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x01));
                break;
            case 0xC8: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xC9: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xCA: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xCB: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xCC: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xCD: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xCE: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xCF: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x02));
                break;
            case 0xD0: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD1: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD2: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD3: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD4: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD5: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD6: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD7: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x04));
                break;
            case 0xD8: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xD9: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xDA: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xDB: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xDC: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xDD: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xDE: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xDF: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x08));
                break;
            case 0xE0: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE1: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE2: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE3: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE4: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE5: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE6: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE7: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x10));
                break;
            case 0xE8: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xE9: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xEA: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xEB: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xEC: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xED: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xEE: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xEF: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x20));
                break;
            case 0xF0: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF1: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF2: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF3: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF4: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF5: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF6: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF7: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x40));
                break;
            case 0xF8: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xF9: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xFA: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xFB: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xFC: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xFD: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xFE: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
            case 0xFF: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                WriteMemory.accept(RegWZ.Word, (byte) (ReadMemory.apply(RegWZ.Word) | (byte) 0x80));
                break;
        }
    }

    void processFD(Context c) {
        switch (ReadOp.apply(RegPC.Word++) & 0xff) {
            case 0x00: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x01: // LD BC, nn
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                RegBC.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                break;
            case 0x02: // LD (BC), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                WriteMemory.accept(RegBC.Word, RegAF.High);
                break;
            case 0x03: // INC BC
                ++RegBC.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x04: // INC B
                RegAF.Low = (byte) (TableInc[++RegBC.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x05: // DEC B
                RegAF.Low = (byte) (TableDec[--RegBC.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x06: // LD B, n
                RegBC.High = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x07: // RLCA
                RegAF.Word = TableRotShift[0][0][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x08: // EX AF, AF'
                c.TUS = RegAF.Word;
                RegAF.Word = RegAltAF.Word;
                RegAltAF.Word = c.TUS;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x09: // ADD IY, BC
                c.TI1 = RegIY.Word;
                c.TI2 = RegBC.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIY.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIY.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x0A: // LD A, (BC)
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                RegAF.High = ReadMemory.apply(RegBC.Word);
                break;
            case 0x0B: // DEC BC
                --RegBC.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x0C: // INC C
                RegAF.Low = (byte) (TableInc[++RegBC.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0D: // DEC C
                RegAF.Low = (byte) (TableDec[--RegBC.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0E: // LD C, n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                RegBC.Low = ReadMemory.apply(RegPC.Word++);
                break;
            case 0x0F: // RRCA
                RegAF.Word = TableRotShift[0][1][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x10: // DJNZ d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (--RegBC.High != 0) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 13;
                    pendingCycles -= 13;
                } else {
                    totalExecutedCycles += 8;
                    pendingCycles -= 8;
                }
                break;
            case 0x11: // LD DE, nn
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                RegDE.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                break;
            case 0x12: // LD (DE), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                WriteMemory.accept(RegDE.Word, RegAF.High);
                break;
            case 0x13: // INC DE
                ++RegDE.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x14: // INC D
                RegAF.Low = (byte) (TableInc[++RegDE.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x15: // DEC D
                RegAF.Low = (byte) (TableDec[--RegDE.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x16: // LD D, n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                RegDE.High = ReadMemory.apply(RegPC.Word++);
                break;
            case 0x17: // RLA
                RegAF.Word = TableRotShift[0][2][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x18: // JR d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                RegPC.Word = (short) (RegPC.Word + c.TSB);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x19: // ADD IY, DE
                c.TI1 = RegIY.Word;
                c.TI2 = RegDE.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIY.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIY.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x1A: // LD A, (DE)
                RegAF.High = ReadMemory.apply(RegDE.Word);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1B: // DEC DE
                --RegDE.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x1C: // INC E
                RegAF.Low = (byte) (TableInc[++RegDE.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1D: // DEC E
                RegAF.Low = (byte) (TableDec[--RegDE.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1E: // LD E, n
                RegDE.Low = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1F: // RRA
                RegAF.Word = TableRotShift[0][3][RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x20: // JR NZ, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (!getRegFlagZ()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x21: // LD IY, nn
                RegIY.Word = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0x22: // LD (nn), IY
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                WriteMemory.accept(c.TUS++, RegIY.Low);
                WriteMemory.accept(c.TUS, RegIY.High);
                RegWZ.Word = c.TUS;
                break;
            case 0x23: // INC IY
                ++RegIY.Word;
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x24: // INC IYH
                RegAF.Low = (byte) (TableInc[++RegIY.High] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x25: // DEC IYH
                RegAF.Low = (byte) (TableDec[--RegIY.High] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x26: // LD IYH, n
                RegIY.High = ReadOpArg.apply(RegPC.Word++);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x27: // DAA
                RegAF.Word = TableDaa[RegAF.Word];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x28: // JR Z, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (getRegFlagZ()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x29: // ADD IY, IY
                c.TI1 = RegIY.Word;
                c.TI2 = RegIY.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIY.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIY.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x2A: // LD IY, (nn)
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) + ReadOpArg.apply(RegPC.Word++) * 256);
                RegIY.Low = ReadMemory.apply(c.TUS++);
                RegIY.High = ReadMemory.apply(c.TUS);
                RegWZ.Word = c.TUS;
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x2B: // DEC IY
                --RegIY.Word;
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x2C: // INC IYL
                RegAF.Low = (byte) (TableInc[++RegIY.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2D: // DEC IYL
                RegAF.Low = (byte) (TableDec[--RegIY.Low] | (RegAF.Low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2E: // LD IYL, n
                RegIY.Low = ReadOpArg.apply(RegPC.Word++);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2F: // CPL
                RegAF.High ^= (byte) 0xFF;
                setRegFlagH(true);
                setRegFlagN(true);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x30: // JR NC, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (!getRegFlagC()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x31: // LD SP, nn
                RegSP.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x32: // LD (nn), A
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                WriteMemory.accept((short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256), RegAF.High);
                break;
            case 0x33: // INC SP
                ++RegSP.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x34: // INC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                c.TB = ReadMemory.apply(RegWZ.Word);
                RegAF.Low = (byte) (TableInc[++c.TB] | (RegAF.Low & 1));
                WriteMemory.accept(RegWZ.Word, c.TB);
                break;
            case 0x35: // DEC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                c.TB = ReadMemory.apply(RegWZ.Word);
                RegAF.Low = (byte) (TableDec[--c.TB] | (RegAF.Low & 1));
                WriteMemory.accept(RegWZ.Word, c.TB);
                break;
            case 0x36: // LD (IY+d), n
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, ReadOpArg.apply(RegPC.Word++));
                break;
            case 0x37: // SCF
                setRegFlagH(false);
                setRegFlagN(false);
                setRegFlagC(true);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x38: // JR C, d
                c.TSB = ReadMemory.apply(RegPC.Word++);
                if (getRegFlagC()) {
                    RegPC.Word = (short) (RegPC.Word + c.TSB);
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x39: // ADD IY, SP
                c.TI1 = RegIY.Word;
                c.TI2 = RegSP.Word;
                c.TIR = c.TI1 + c.TI2;
                c.TUS = (short) c.TIR;
                RegWZ.Word = (short) (RegIY.Word + 1);
                setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                RegIY.Word = c.TUS;
                setRegFlag3((c.TUS & 0x0800) != 0);
                setRegFlag5((c.TUS & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x3A: // LD A, (nn)
                RegAF.High = ReadMemory.apply((short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256));
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                break;
            case 0x3B: // DEC SP
                --RegSP.Word;
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x3C: // INC A
                RegAF.Low = (byte) (TableInc[++RegAF.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3D: // DEC A
                RegAF.Low = (byte) (TableDec[--RegAF.High] | (RegAF.Low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3E: // LD A, n
                RegAF.High = ReadMemory.apply(RegPC.Word++);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x3F: // CCF
                setRegFlagH(getRegFlagC());
                setRegFlagN(false);
                setRegFlagC(getRegFlagC() ^ true);
                setRegFlag3((RegAF.High & 0x08) != 0);
                setRegFlag5((RegAF.High & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x40: // LD B, B
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x41: // LD B, C
                RegBC.High = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x42: // LD B, D
                RegBC.High = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x43: // LD B, E
                RegBC.High = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x44: // LD B, IYH
                RegBC.High = RegIY.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x45: // LD B, IYL
                RegBC.High = RegIY.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x46: // LD B, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegBC.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x47: // LD B, A
                RegBC.High = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x48: // LD C, B
                RegBC.Low = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x49: // LD C, C
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4A: // LD C, D
                RegBC.Low = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4B: // LD C, E
                RegBC.Low = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4C: // LD C, IYH
                RegBC.Low = RegIY.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4D: // LD C, IYL
                RegBC.Low = RegIY.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4E: // LD C, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegBC.Low = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x4F: // LD C, A
                RegBC.Low = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x50: // LD D, B
                RegDE.High = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x51: // LD D, C
                RegDE.High = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x52: // LD D, D
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x53: // LD D, E
                RegDE.High = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x54: // LD D, IYH
                RegDE.High = RegIY.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x55: // LD D, IYL
                RegDE.High = RegIY.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x56: // LD D, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegDE.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x57: // LD D, A
                RegDE.High = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x58: // LD E, B
                RegDE.Low = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x59: // LD E, C
                RegDE.Low = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5A: // LD E, D
                RegDE.Low = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5B: // LD E, E
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5C: // LD E, IYH
                RegDE.Low = RegIY.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5D: // LD E, IYL
                RegDE.Low = RegIY.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5E: // LD E, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegDE.Low = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x5F: // LD E, A
                RegDE.Low = RegAF.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x60: // LD IYH, B
                RegIY.High = RegBC.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x61: // LD IYH, C
                RegIY.High = RegBC.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x62: // LD IYH, D
                RegIY.High = RegDE.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x63: // LD IYH, E
                RegIY.High = RegDE.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x64: // LD IYH, IYH
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x65: // LD IYH, IYL
                RegIY.High = RegIY.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x66: // LD H, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegHL.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x67: // LD IYH, A
                RegIY.High = RegAF.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x68: // LD IYL, B
                RegIY.Low = RegBC.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x69: // LD IYL, C
                RegIY.Low = RegBC.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6A: // LD IYL, D
                RegIY.Low = RegDE.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6B: // LD IYL, E
                RegIY.Low = RegDE.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6C: // LD IYL, IYH
                RegIY.Low = RegIY.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6D: // LD IYL, IYL
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6E: // LD L, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegHL.Low = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x6F: // LD IYL, A
                RegIY.Low = RegAF.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x70: // LD (IY+d), B
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegBC.High);
                break;
            case 0x71: // LD (IY+d), C
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegBC.Low);
                break;
            case 0x72: // LD (IY+d), D
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegDE.High);
                break;
            case 0x73: // LD (IY+d), E
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegDE.Low);
                break;
            case 0x74: // LD (IY+d), H
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegHL.High);
                break;
            case 0x75: // LD (IY+d), L
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegHL.Low);
                break;
            case 0x76: // HALT
                Halt();
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x77: // LD (IY+d), A
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                WriteMemory.accept(RegWZ.Word, RegAF.High);
                break;
            case 0x78: // LD A, B
                RegAF.High = RegBC.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x79: // LD A, C
                RegAF.High = RegBC.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7A: // LD A, D
                RegAF.High = RegDE.High;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7B: // LD A, E
                RegAF.High = RegDE.Low;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7C: // LD A, IYH
                RegAF.High = RegIY.High;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7D: // LD A, IYL
                RegAF.High = RegIY.Low;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7E: // LD A, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.High = ReadMemory.apply(RegWZ.Word);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x7F: // LD A, A
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x80: // ADD A, B
                RegAF.Word = TableALU[0][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x81: // ADD A, C
                RegAF.Word = TableALU[0][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x82: // ADD A, D
                RegAF.Word = TableALU[0][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x83: // ADD A, E
                RegAF.Word = TableALU[0][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x84: // ADD A, IYH
                RegAF.Word = TableALU[0][RegAF.High][RegIY.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x85: // ADD A, IYL
                RegAF.Word = TableALU[0][RegAF.High][RegIY.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x86: // ADD A, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[0][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19; // 16
                break;
            case 0x87: // ADD A, A
                RegAF.Word = TableALU[0][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x88: // ADC A, B
                RegAF.Word = TableALU[1][RegAF.High][RegBC.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x89: // ADC A, C
                RegAF.Word = TableALU[1][RegAF.High][RegBC.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8A: // ADC A, D
                RegAF.Word = TableALU[1][RegAF.High][RegDE.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8B: // ADC A, E
                RegAF.Word = TableALU[1][RegAF.High][RegDE.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8C: // ADC A, IYH
                RegAF.Word = TableALU[1][RegAF.High][RegIY.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x8D: // ADC A, IYL
                RegAF.Word = TableALU[1][RegAF.High][RegIY.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x8E: // ADC A, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[1][RegAF.High][ReadMemory.apply(RegWZ.Word)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x8F: // ADC A, A
                RegAF.Word = TableALU[1][RegAF.High][RegAF.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x90: // SUB B
                RegAF.Word = TableALU[2][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x91: // SUB C
                RegAF.Word = TableALU[2][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x92: // SUB D
                RegAF.Word = TableALU[2][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x93: // SUB E
                RegAF.Word = TableALU[2][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x94: // SUB IYH
                RegAF.Word = TableALU[2][RegAF.High][RegIY.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x95: // SUB IYL
                RegAF.Word = TableALU[2][RegAF.High][RegIY.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x96: // SUB (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[2][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x97: // SUB A, A
                RegAF.Word = TableALU[2][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x98: // SBC A, B
                RegAF.Word = TableALU[3][RegAF.High][RegBC.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x99: // SBC A, C
                RegAF.Word = TableALU[3][RegAF.High][RegBC.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9A: // SBC A, D
                RegAF.Word = TableALU[3][RegAF.High][RegDE.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9B: // SBC A, E
                RegAF.Word = TableALU[3][RegAF.High][RegDE.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9C: // SBC A, IYH
                RegAF.Word = TableALU[3][RegAF.High][RegIY.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x9D: // SBC A, IYL
                RegAF.Word = TableALU[3][RegAF.High][RegIY.Low][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x9E: // SBC A, (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[3][RegAF.High][ReadMemory.apply(RegWZ.Word)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x9F: // SBC A, A
                RegAF.Word = TableALU[3][RegAF.High][RegAF.High][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA0: // AND B
                RegAF.Word = TableALU[4][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA1: // AND C
                RegAF.Word = TableALU[4][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA2: // AND D
                RegAF.Word = TableALU[4][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA3: // AND E
                RegAF.Word = TableALU[4][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA4: // AND IYH
                RegAF.Word = TableALU[4][RegAF.High][RegIY.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xA5: // AND IYL
                RegAF.Word = TableALU[4][RegAF.High][RegIY.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xA6: // AND (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[4][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xA7: // AND A
                RegAF.Word = TableALU[4][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA8: // XOR B
                RegAF.Word = TableALU[5][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA9: // XOR C
                RegAF.Word = TableALU[5][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAA: // XOR D
                RegAF.Word = TableALU[5][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAB: // XOR E
                RegAF.Word = TableALU[5][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAC: // XOR IYH
                RegAF.Word = TableALU[5][RegAF.High][RegIY.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xAD: // XOR IYL
                RegAF.Word = TableALU[5][RegAF.High][RegIY.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xAE: // XOR (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[5][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xAF: // XOR A
                RegAF.Word = TableALU[5][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB0: // OR B
                RegAF.Word = TableALU[6][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB1: // OR C
                RegAF.Word = TableALU[6][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB2: // OR D
                RegAF.Word = TableALU[6][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB3: // OR E
                RegAF.Word = TableALU[6][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB4: // OR IYH
                RegAF.Word = TableALU[6][RegAF.High][RegIY.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xB5: // OR IYL
                RegAF.Word = TableALU[6][RegAF.High][RegIY.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xB6: // OR (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[6][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xB7: // OR A
                RegAF.Word = TableALU[6][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB8: // CP B
                RegAF.Word = TableALU[7][RegAF.High][RegBC.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB9: // CP C
                RegAF.Word = TableALU[7][RegAF.High][RegBC.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBA: // CP D
                RegAF.Word = TableALU[7][RegAF.High][RegDE.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBB: // CP E
                RegAF.Word = TableALU[7][RegAF.High][RegDE.Low][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBC: // CP IYH
                RegAF.Word = TableALU[7][RegAF.High][RegIY.High][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xBD: // CP IYL
                RegAF.Word = TableALU[7][RegAF.High][RegIY.Low][0];
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0xBE: // CP (IY+d)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                RegAF.Word = TableALU[7][RegAF.High][ReadMemory.apply(RegWZ.Word)][0];
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xBF: // CP A
                RegAF.Word = TableALU[7][RegAF.High][RegAF.High][0];
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC0: // RET NZ
                if (!getRegFlagZ()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC1: // POP BC
                RegBC.Low = ReadMemory.apply(RegSP.Word++);
                RegBC.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC2: // JP NZ, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagZ()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC3: // JP nn
                RegPC.Word = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC4: // CALL NZ, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xC5: // PUSH BC
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegBC.High);
                WriteMemory.accept(--RegSP.Word, RegBC.Low);
                break;
            case 0xC6: // ADD A, n
                RegAF.Word = TableALU[0][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xC7: // RST $00
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x00;
                break;
            case 0xC8: // RET Z
                if (getRegFlagZ()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC9: // RET
                RegPC.Low = ReadMemory.apply(RegSP.Word++);
                RegPC.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCA: // JP Z, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagZ()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCB: // (Prefix)
                c.Displacement = ReadOpArg.apply(RegPC.Word++);
                //++RegR;
                RegWZ.Word = (short) (RegIY.Word + c.Displacement);
                processFDCB(c);
                break;
            case 0xCC: // CALL Z, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xCD: // CALL nn
                totalExecutedCycles += 17;
                pendingCycles -= 17;
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = c.TUS;
                break;
            case 0xCE: // ADC A, n
                RegAF.Word = TableALU[1][RegAF.High][ReadMemory.apply(RegPC.Word++)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xCF: // RST $08
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x08;
                break;
            case 0xD0: // RET NC
                if (!getRegFlagC()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD1: // POP DE
                RegDE.Low = ReadMemory.apply(RegSP.Word++);
                RegDE.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD2: // JP NC, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagC()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD3: // OUT n, A
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteHardware.accept((short) (byte) ReadMemory.apply(RegPC.Word++), RegAF.High);
                break;
            case 0xD4: // CALL NC, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xD5: // PUSH DE
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegDE.High);
                WriteMemory.accept(--RegSP.Word, RegDE.Low);
                break;
            case 0xD6: // SUB n
                RegAF.Word = TableALU[2][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xD7: // RST $10
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x10;
                break;
            case 0xD8: // RET C
                if (getRegFlagC()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD9: // EXX
                c.TUS = RegBC.Word;
                RegBC.Word = RegAltBC.Word;
                RegAltBC.Word = c.TUS;
                c.TUS = RegDE.Word;
                RegDE.Word = RegAltDE.Word;
                RegAltDE.Word = c.TUS;
                c.TUS = RegHL.Word;
                RegHL.Word = RegAltHL.Word;
                RegAltHL.Word = c.TUS;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDA: // JP C, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagC()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xDB: // IN A, n
                c.TUS = (short) (ReadOpArg.apply(RegPC.Word++) | (RegAF.High << 8));
                RegAF.High = ReadHardware.apply(c.TUS);
                RegWZ.Word = (short) (c.TUS + 1);
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                break;
            case 0xDC: // CALL C, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xDD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xDE: // SBC A, n
                RegAF.Word = TableALU[3][RegAF.High][ReadMemory.apply(RegPC.Word++)][getRegFlagC() ? 1 : 0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xDF: // RST $18
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x18;
                break;
            case 0xE0: // RET PO
                if (!getRegFlagP()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE1: // POP IY
                RegIY.Low = ReadMemory.apply(RegSP.Word++);
                RegIY.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0xE2: // JP PO, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagP()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xE3: // EX (SP), IY
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.TUS = RegSP.Word;
                c.TBL = ReadMemory.apply(c.TUS++);
                c.TBH = ReadMemory.apply(c.TUS--);
                WriteMemory.accept(c.TUS++, RegIY.Low);
                WriteMemory.accept(c.TUS, RegIY.High);
                RegIY.Low = c.TBL;
                RegIY.High = c.TBH;
                RegWZ.Word = RegIY.Word;
                break;
            case 0xE4: // CALL C, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xE5: // PUSH IY
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                WriteMemory.accept(--RegSP.Word, RegIY.High);
                WriteMemory.accept(--RegSP.Word, RegIY.Low);
                break;
            case 0xE6: // AND n
                RegAF.Word = TableALU[4][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xE7: // RST $20
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x20;
                break;
            case 0xE8: // RET PE
                if (getRegFlagP()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE9: // JP IY
                RegPC.Word = RegIY.Word;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEA: // JP PE, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagP()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xEB: // EX DE, HL
                c.TUS = RegDE.Word;
                RegDE.Word = RegHL.Word;
                RegHL.Word = c.TUS;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEC: // CALL PE, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagP()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xED: // (Prefix)
                ++RegR;
                switch (ReadOp.apply(RegPC.Word++) & 0xff) {
                    case 0x00: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x01: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x02: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x03: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x04: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x05: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x06: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x07: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x08: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x09: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x10: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x11: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x12: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x13: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x14: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x15: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x16: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x17: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x18: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x19: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x20: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x21: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x22: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x23: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x24: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x25: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x26: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x27: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x28: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x29: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x30: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x31: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x32: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x33: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x34: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x35: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x36: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x37: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x38: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x39: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x40: // IN B, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegBC.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegBC.High > 127);
                        setRegFlagZ(RegBC.High == 0);
                        setRegFlag5((RegBC.High & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegBC.High & 0x08) != 0);
                        setRegFlagP(TableParity[RegBC.High]);
                        setRegFlagN(false);
                        break;
                    case 0x41: // OUT C, B
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegBC.High);
                        break;
                    case 0x42: // SBC HL, BC
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegBC.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((RegHL.Word ^ RegBC.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegBC.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x43: // LD (nn), BC
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegBC.Low);
                        WriteMemory.accept(c.TUS, RegBC.High);
                        RegWZ.Word = c.TUS;
                        break;
                    case 0x44: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x45: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        RegWZ.Word = RegPC.Word;
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x46: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x47: // LD I, A
                        RegI = RegAF.High;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x48: // IN C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegBC.Low = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegBC.Low > 127);
                        setRegFlagZ(RegBC.Low == 0);
                        setRegFlag5((RegBC.Low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegBC.Low & 0x08) != 0);
                        setRegFlagP(TableParity[RegBC.Low]);
                        setRegFlagN(false);
                        break;
                    case 0x49: // OUT C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegBC.Low);
                        break;
                    case 0x4A: // ADC HL, BC
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegBC.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x4B: // LD BC, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegBC.Low = ReadMemory.apply(c.TUS++);
                        RegBC.High = ReadMemory.apply(c.TUS);
                        RegWZ.Word = c.TUS;
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x4C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x4E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4F: // LD R, A
                        RegR = RegAF.High;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x50: // IN D, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegDE.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegDE.High > 127);
                        setRegFlagZ(RegDE.High == 0);
                        setRegFlag5((RegDE.High & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegDE.High & 0x08) != 0);
                        setRegFlagP(TableParity[RegDE.High]);
                        setRegFlagN(false);
                        break;
                    case 0x51: // OUT C, D
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegDE.High);
                        break;
                    case 0x52: // SBC HL, DE
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegDE.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((RegHL.Word ^ RegDE.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegDE.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x53: // LD (nn), DE
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegDE.Low);
                        WriteMemory.accept(c.TUS, RegDE.High);
                        RegWZ.Word = c.TUS;
                        break;
                    case 0x54: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x55: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x56: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x57: // LD A, I
                        RegAF.High = RegI;
                        setRegFlagS((RegI & 0xff) > 127);
                        setRegFlagZ(RegI == 0);
                        setRegFlag5(((RegI & 0x20) != 0));
                        setRegFlagH(false);
                        setRegFlag3(((RegI & 0x08) != 0));
                        setRegFlagN(false);
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x58: // IN E, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegDE.Low = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegDE.Low > 127);
                        setRegFlagZ(RegDE.Low == 0);
                        setRegFlag5((RegDE.Low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((RegDE.Low & 0x08) != 0);
                        setRegFlagP(TableParity[RegDE.Low]);
                        setRegFlagN(false);
                        break;
                    case 0x59: // OUT C, E
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegDE.Low);
                        break;
                    case 0x5A: // ADC HL, DE
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegDE.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x5B: // LD DE, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegDE.Low = ReadMemory.apply(c.TUS++);
                        RegDE.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x5C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x5E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5F: // LD A, R
                        RegAF.High = (byte) ((RegR & 0x7F) | RegR2);
                        setRegFlagS((RegR2 == (byte) 0x80));
                        setRegFlagZ((byte) ((RegR & 0x7F) | RegR2) == 0);
                        setRegFlagH(false);
                        setRegFlag5(((RegR & 0x20) != 0));
                        setRegFlagN(false);
                        setRegFlag3(((RegR & 0x08) != 0));
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x60: // IN H, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegHL.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegHL.High > 127);
                        setRegFlagZ(RegHL.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegHL.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegHL.High & 0x08) != 0);
                        setRegFlag5((RegHL.High & 0x20) != 0);
                        break;
                    case 0x61: // OUT C, H
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegHL.High);
                        break;
                    case 0x62: // SBC HL, HL
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegHL.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((RegHL.Word ^ RegHL.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegHL.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x63: // LD (nn), HL
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegHL.Low);
                        WriteMemory.accept(c.TUS, RegHL.High);
                        break;
                    case 0x64: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x65: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x66: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x67: // RRD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.TB1 = RegAF.High;
                        c.TB2 = ReadMemory.apply(RegHL.Word);
                        WriteMemory.accept(RegHL.Word, (byte) ((c.TB2 >> 4) + (c.TB1 << 4)));
                        RegAF.High = (byte) ((c.TB1 & 0xF0) + (c.TB2 & 0x0F));
                        setRegFlagS(RegAF.High > 127);
                        setRegFlagZ(RegAF.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegAF.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        break;
                    case 0x68: // IN L, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegHL.Low = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegHL.Low > 127);
                        setRegFlagZ(RegHL.Low == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegHL.Low]);
                        setRegFlagN(false);
                        setRegFlag3((RegHL.Low & 0x08) != 0);
                        setRegFlag5((RegHL.Low & 0x20) != 0);
                        break;
                    case 0x69: // OUT C, L
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegHL.Low);
                        break;
                    case 0x6A: // ADC HL, HL
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegHL.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x6B: // LD HL, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegHL.Low = ReadMemory.apply(c.TUS++);
                        RegHL.High = ReadMemory.apply(c.TUS);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x6C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x6E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6F: // RLD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.TB1 = RegAF.High;
                        c.TB2 = ReadMemory.apply(RegHL.Word);
                        WriteMemory.accept(RegHL.Word, (byte) ((c.TB1 & 0x0F) + (c.TB2 << 4)));
                        RegAF.High = (byte) ((c.TB1 & 0xF0) + (c.TB2 >> 4));
                        setRegFlagS(RegAF.High > 127);
                        setRegFlagZ(RegAF.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegAF.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        break;
                    case 0x70: // IN 0, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        setRegFlagS((c.TB & 0xff) > 127);
                        setRegFlagZ(c.TB == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[c.TB]);
                        setRegFlagN(false);
                        setRegFlag3((c.TB & 0x08) != 0);
                        setRegFlag5((c.TB & 0x20) != 0);
                        break;
                    case 0x71: // OUT C, 0
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, (byte) 0);
                        break;
                    case 0x72: // SBC HL, SP
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegSP.Word;
                        c.TIR = c.TI1 - c.TI2;
                        if (getRegFlagC()) {
                            --c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((RegHL.Word ^ RegSP.Word ^ c.TUS) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) RegHL.Word - (int) RegSP.Word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x73: // LD (nn), SP
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        WriteMemory.accept(c.TUS++, RegSP.Low);
                        WriteMemory.accept(c.TUS, RegSP.High);
                        RegWZ.Word = c.TUS;
                        break;
                    case 0x74: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x75: // RETN
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        RegWZ.Word = RegPC.Word;
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x76: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x77: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x78: // IN A, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        RegAF.High = ReadHardware.apply(RegBC.Word);
                        setRegFlagS(RegAF.High > 127);
                        setRegFlagZ(RegAF.High == 0);
                        setRegFlagH(false);
                        setRegFlagP(TableParity[RegAF.High]);
                        setRegFlagN(false);
                        setRegFlag3((RegAF.High & 0x08) != 0);
                        setRegFlag5((RegAF.High & 0x20) != 0);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        break;
                    case 0x79: // OUT C, A
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        WriteHardware.accept((short) RegBC.Low, RegAF.High);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        break;
                    case 0x7A: // ADC HL, SP
                        c.TI1 = RegHL.Word;
                        c.TI2 = RegSP.Word;
                        c.TIR = c.TI1 + c.TI2;
                        if (getRegFlagC()) {
                            ++c.TIR;
                            ++c.TI2;
                        }
                        c.TUS = (short) c.TIR;
                        RegWZ.Word = (short) (RegHL.Word + 1);
                        setRegFlagH(((c.TI1 & 0xFFF) + (c.TI2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC(((short) c.TI1 + (short) c.TI2) > 0xFFFF);
                        setRegFlagP(c.TIR > 32767 || c.TIR < -32768);
                        setRegFlagS(c.TUS > 32767);
                        setRegFlagZ(c.TUS == 0);
                        RegHL.Word = c.TUS;
                        setRegFlag3((c.TUS & 0x0800) != 0);
                        setRegFlag5((c.TUS & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x7B: // LD SP, (nn)
                        c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                        RegSP.Low = ReadMemory.apply(c.TUS++);
                        RegSP.High = ReadMemory.apply(c.TUS);
                        RegWZ.Word = c.TUS;
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x7C: // NEG
                        RegAF.Word = TableNeg[RegAF.Word];
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7D: // RETI
                        RegPC.Low = ReadMemory.apply(RegSP.Word++);
                        RegPC.High = ReadMemory.apply(RegSP.Word++);
                        RegWZ.Word = RegPC.Word;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x7E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x80: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x81: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x82: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x83: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x84: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x85: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x86: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x87: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x88: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x89: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x90: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x91: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x92: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x93: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x94: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x95: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x96: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x97: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x98: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x99: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA0: // LDI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        WriteMemory.accept(RegDE.Word++, c.TB1 = ReadMemory.apply(RegHL.Word++));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA1: // CPI
                        c.TB1 = ReadMemory.apply(RegHL.Word++);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS((c.TB2 & 0xff) > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xA2: // INI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word++, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low + 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xA3: // OUTI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word++);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(IsX(RegBC.High));
                        setRegFlagH(false);
                        setRegFlag5(IsY(RegBC.High));
                        setRegFlagZ(RegBC.High == 0);
                        setRegFlagS(IsS(RegBC.High));
                        c.TUS = (short) (RegHL.Low + c.TB);
                        if (IsS(c.TB)) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xA4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA8: // LDD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        WriteMemory.accept(RegDE.Word--, c.TB1 = ReadMemory.apply(RegHL.Word--));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA9: // CPD
                        c.TB1 = ReadMemory.apply(RegHL.Word--);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS(c.TB2 > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xAA: // IND
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word--, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low - 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xAB: // OUTD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word--);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        c.TUS = (short) (RegHL.Low + c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        break;
                    case 0xAC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB0: // LDIR
                        WriteMemory.accept(RegDE.Word++, c.TB1 = ReadMemory.apply(RegHL.Word++));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (RegBC.Word != 0) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB1: // CPIR
                        c.TB1 = ReadMemory.apply(RegHL.Word++);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        RegWZ.Word++;
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS(c.TB2 > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        if (RegBC.Word != 0 && !getRegFlagZ()) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB2: // INIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word++, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low + 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB3: // OTIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word++);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word + 1);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(IsX(RegBC.High));
                        setRegFlagH(false);
                        setRegFlag5(IsY(RegBC.High));
                        setRegFlagZ(RegBC.High == 0);
                        setRegFlagS(IsS(RegBC.High));
                        c.TUS = (short) (RegHL.Low + c.TB);
                        if (IsS(c.TB)) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB8: // LDDR
                        WriteMemory.accept(RegDE.Word--, c.TB1 = ReadMemory.apply(RegHL.Word--));
                        c.TB1 += RegAF.High;
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (RegBC.Word != 0) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB9: // CPDR
                        c.TB1 = ReadMemory.apply(RegHL.Word--);
                        c.TB2 = (byte) (RegAF.High - c.TB1);
                        RegWZ.Word--;
                        setRegFlagN(true);
                        setRegFlagH(TableHalfBorrow[RegAF.High][c.TB1]);
                        setRegFlagZ(c.TB2 == 0);
                        setRegFlagS(c.TB2 > 127);
                        c.TB1 = (byte) (RegAF.High - c.TB1 - (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.TB1 & 0x02) != 0);
                        setRegFlag3((c.TB1 & 0x08) != 0);
                        --RegBC.Word;
                        setRegFlagP(RegBC.Word != 0);
                        if (RegBC.Word != 0 && !getRegFlagZ()) {
                            RegPC.Word -= 2;
                            RegWZ.Word = (short) (RegPC.Word + 1);
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xBA: // INDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadHardware.apply(RegBC.Word);
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        --RegBC.High;
                        WriteMemory.accept(RegHL.Word--, c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        c.TUS = (short) (((RegBC.Low - 1) & 0xff) + c.TB);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBB: // OTDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.TB = ReadMemory.apply(RegHL.Word--);
                        WriteHardware.accept(RegBC.Word, c.TB);
                        --RegBC.High;
                        RegWZ.Word = (short) (RegBC.Word - 1);
                        c.TUS = (short) (RegHL.Low + c.TB);
                        setRegFlagZ(RegBC.High == 0);
                        if ((c.TB & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.TUS & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(TableParity[(c.TUS & 0x07) ^ RegBC.High]);
                        if (RegBC.High != 0) {
                            RegPC.Word -= 2;
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xED: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                }
                break;
            case 0xEE: // XOR n
                RegAF.Word = TableALU[5][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xEF: // RST $28
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x28;
                break;
            case 0xF0: // RET P
                if (!getRegFlagS()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF1: // POP AF
                RegAF.Low = ReadMemory.apply(RegSP.Word++);
                RegAF.High = ReadMemory.apply(RegSP.Word++);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF2: // JP P, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagS()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF3: // DI
                iff1 = iff2 = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF4: // CALL P, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (!getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xF5: // PUSH AF
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegAF.High);
                WriteMemory.accept(--RegSP.Word, RegAF.Low);
                break;
            case 0xF6: // OR n
                RegAF.Word = TableALU[6][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xF7: // RST $30
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x30;
                break;
            case 0xF8: // RET M
                if (getRegFlagS()) {
                    RegPC.Low = ReadMemory.apply(RegSP.Word++);
                    RegPC.High = ReadMemory.apply(RegSP.Word++);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF9: // LD SP, IY
                RegSP.Word = RegIY.Word;
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFA: // JP M, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagS()) {
                    RegPC.Word = c.TUS;
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFB: // EI
                iff1 = iff2 = true;
                Interruptable = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFC: // CALL M, nn
                c.TUS = (short) (ReadMemory.apply(RegPC.Word++) + ReadMemory.apply(RegPC.Word++) * 256);
                if (getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    WriteMemory.accept(--RegSP.Word, RegPC.High);
                    WriteMemory.accept(--RegSP.Word, RegPC.Low);
                    RegPC.Word = c.TUS;
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xFD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xFE: // CP n
                RegAF.Word = TableALU[7][RegAF.High][ReadMemory.apply(RegPC.Word++)][0];
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xFF: // RST $38
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                WriteMemory.accept(--RegSP.Word, RegPC.High);
                WriteMemory.accept(--RegSP.Word, RegPC.Low);
                RegPC.Word = 0x38;
                break;
        }
    }

//#endregion

//#region Interrupts

    private boolean iff1;

    public boolean getIFF1() {
        return iff1;
    }

    public void setIFF1(boolean value) {
        iff1 = value;
    }

    private boolean iff2;

    public boolean getIFF2() {
        return iff2;
    }

    public void setIFF2(boolean value) {
        iff2 = value;
    }

    private boolean interrupt;

    public boolean getInterrupt() {
        return interrupt;
    }

    public void setInterrupt(boolean value) {
        interrupt = value;
    }

    private boolean nonMaskableInterrupt;

    public boolean getNonMaskableInterrupt() {
        return nonMaskableInterrupt;
    }

    public void setNonMaskableInterrupt(boolean value) {
        if (value && !nonMaskableInterrupt) nonMaskableInterruptPending = true;
        nonMaskableInterrupt = value;
    }

    private boolean nonMaskableInterruptPending;

    public boolean getNonMaskableInterruptPending() {
        return nonMaskableInterruptPending;
    }

    public void setNonMaskableInterruptPending(boolean value) {
        nonMaskableInterruptPending = value;
    }

    private int interruptMode;

    public int getInterruptMode() {
        return interruptMode;
    }

    public void setInterruptMode(int value) {
        if (value < 0 || value > 2) throw new IndexOutOfBoundsException();
        interruptMode = value;
    }

    private boolean halted;

    public boolean getHalted() {
        return halted;
    }

    public void setHalted(boolean value) {
        halted = value;
    }

    public Supplier<Integer> IRQCallback = () -> {
        return 0;
    };

    public final Runnable NMICallback = () -> {
    };

    private void ResetInterrupts() {
        interrupt = false;
        nonMaskableInterrupt = false;
        nonMaskableInterruptPending = false;
        Interruptable = true;
        iff1 = iff2 = false;
    }

    private void Halt() {
        RegPC.Word--;
        halted = true;
    }

//#endregion
}
