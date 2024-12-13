/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m6805;


import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Cpuexec.cpuexec_data;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Cpuint.Register;
import m1.emu.Timer;


public class M6805 extends cpuexec_data {

    public static M6805 m1;
    public Register ea, pc, s;
    public int subtype;
    public short sp_mask;
    public short sp_low;
    public byte a, x, cc;
    public short pending_interrupts;
    public interface irq_delegate extends Function<Integer, Integer> {}
    public irq_delegate irq_callback;
    public final int[] irq_state;
    public int nmi_state;
    public final byte CFLAG = 0x01;
    public final byte ZFLAG = 0x02;
    public final byte NFLAG = 0x04;
    public final byte IFLAG = 0x08;
    public final byte HFLAG = 0x10;
    public final int SUBTYPE_M6805 = 0;
    public final int SUBTYPE_M68705 = 1;
    public final int SUBTYPE_HD63705 = 2;
    public final byte[] flags8i = new byte[] { // increment
            0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04
    };
    public final byte[] flags8d = new byte[] { /* decrement */
            0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04
    };
    public final byte[] cycles1 = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */
            /*0*/ 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            /*1*/  7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            /*2*/  4, 0, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            /*3*/  6, 0, 0, 6, 6, 0, 6, 6, 6, 6, 6, 6, 0, 6, 6, 0,
            /*4*/  4, 0, 0, 4, 4, 0, 4, 4, 4, 4, 4, 0, 4, 4, 0, 4,
            /*5*/  4, 0, 0, 4, 4, 0, 4, 4, 4, 4, 4, 0, 4, 4, 0, 4,
            /*6*/  7, 0, 0, 7, 7, 0, 7, 7, 7, 7, 7, 0, 7, 7, 0, 7,
            /*7*/  6, 0, 0, 6, 6, 0, 6, 6, 6, 6, 6, 0, 6, 6, 0, 6,
            /*8*/  9, 6, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            /*9*/  0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 0, 2,
            /*A*/  2, 2, 2, 2, 2, 2, 2, 0, 2, 2, 2, 2, 0, 8, 2, 0,
            /*B*/  4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 3, 7, 4, 5,
            /*C*/  5, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 4, 8, 5, 6,
            /*D*/  6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 5, 9, 6, 7,
            /*E*/  5, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 4, 8, 5, 6,
            /*F*/  4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 3, 7, 4, 5
    };
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

    public Function<Short, Byte> ReadOp, ReadOpArg;
    public Function<Short, Byte> ReadMemory;
    public BiConsumer<Short, Byte> WriteMemory;

    public M6805() {
        irq_state = new int[9];
        m6805_init(Cpuint::cpu_3_irq_callback);
    }

    @Override
    public void Reset() {
        m6805_reset();
    }

    private void SP_INC() {
        if (++s.lowWord > sp_mask) {
            s.lowWord = sp_low;
        }
    }

    private void SP_DEC() {
        if (--s.lowWord < sp_low) {
            s.lowWord = sp_mask;
        }
    }

    private short SP_ADJUST(short a) {
        return (short) (((a) & sp_mask) | sp_low);
    }

    private void IMMBYTE(/* ref */ byte[] b) {
        b[0] = ReadOpArg.apply(pc.lowWord++);
    }

    private void IMMWORD(/* ref */ Register w) {
        w.d = 0;
        w.highByte = ReadOpArg.apply(pc.lowWord);
        w.lowByte = ReadOpArg.apply((short) (pc.lowWord + 1));
        pc.lowWord += 2;
    }

    private void PUSHBYTE(/* ref */ byte[] b) {
        wr_s_handler_b(/* ref */ b);
    }

    private void PUSHWORD(/* ref */ Register w) {
        wr_s_handler_w(/* ref */ w);
    }

    private void PULLBYTE(/* ref */ byte[] b) {
        rd_s_handler_b(/* ref */ b);
    }

    private void PULLWORD(/* ref */ Register w) {
        rd_s_handler_w(/* ref */ w);
    }

    private void CLR_NZ() {
        cc &= (byte) ~(NFLAG | ZFLAG);
    }

    private void CLR_HNZC() {
        cc &= (byte) ~(HFLAG | NFLAG | ZFLAG | CFLAG);
    }

    private void CLR_Z() {
        cc &= (byte) ~(ZFLAG);
    }

    private void CLR_NZC() {
        cc &= (byte) ~(NFLAG | ZFLAG | CFLAG);
    }

    private void CLR_ZC() {
        cc &= (byte) ~(ZFLAG | CFLAG);
    }

    private void SET_Z(byte b) {
        if (b == 0) {
            SEZ();
        }
    }

    private void SET_Z8(byte b) {
        SET_Z(b);
    }

    private void SET_N8(byte b) {
        cc |= (byte) ((b & 0x80) >> 5);
    }

    private void SET_H(byte a, byte b, byte r) {
        cc |= (byte) ((a ^ b ^ r) & 0x10);
    }

    private void SET_C8(short b) {
        cc |= (byte) ((b & 0x100) >> 8);
    }

    private void SET_FLAGS8I(byte b) {
        cc |= flags8i[b & 0xff];
    }

    private void SET_FLAGS8D(byte b) {
        cc |= flags8d[b & 0xff];
    }

    private void SET_NZ8(byte b) {
        SET_N8(b);
        SET_Z(b);
    }

    private void SET_FLAGS8(byte a, byte b, short r) {
        SET_N8((byte) r);
        SET_Z8((byte) r);
        SET_C8(r);
    }

    private short SIGNED(byte b) {
        return (short) ((b & 0x80) != 0 ? b | 0xff00 : b);
    }

    private void DIRECT() {
        ea.d = 0;
        byte[] tmp = new byte[1];
        IMMBYTE(/* ref */ tmp);
        ea.lowByte = tmp[0];
    }

    private void IMM8() {
        ea.lowWord = pc.lowWord++;
    }

    private void EXTENDED() {
        IMMWORD(/* ref */ ea);
    }

    private void INDEXED() {
        ea.lowWord = x;
    }

    private void INDEXED1() {
        ea.d = 0;
        byte[] tmp = new byte[1];
        IMMBYTE(/* ref */ tmp);
        ea.lowByte = tmp[0];
        ea.lowWord += x;
    }

    private void INDEXED2() {
        IMMWORD(/* ref */ ea);
        ea.lowWord += x;
    }

    private void SEC() {
        cc |= CFLAG;
    }

    private void CLC() {
        cc &= (byte) ~CFLAG;
    }

    private void SEZ() {
        cc |= ZFLAG;
    }

    private void CLZ() {
        cc &= (byte) ~ZFLAG;
    }

    private void SEN() {
        cc |= NFLAG;
    }

    private void CLN() {
        cc &= (byte) ~NFLAG;
    }

    private void SEH() {
        cc |= HFLAG;
    }

    private void CLH() {
        cc &= (byte) ~HFLAG;
    }

    private void SEI() {
        cc |= IFLAG;
    }

    private void CLI() {
        cc &= (byte) ~IFLAG;
    }

    private void DIRBYTE(/* ref */ byte[] b) {
        DIRECT();
        b[0] = ReadMemory.apply((short) ea.d);
    }

    private void EXTBYTE(/* ref */ byte[] b) {
        EXTENDED();
        b[0] = ReadMemory.apply((short) ea.d);
    }

    private void IDXBYTE(/* ref */ byte[] b) {
        INDEXED();
        b[0] = ReadMemory.apply((short) ea.d);
    }

    private void IDX1BYTE(/* ref */ byte[] b) {
        INDEXED1();
        b[0] = ReadMemory.apply((short) ea.d);
    }

    private void IDX2BYTE(/* ref */ byte[] b) {
        INDEXED2();
        b[0] = ReadMemory.apply((short) ea.d);
    }

    private void BRANCH(boolean f) {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        if (f) {
            pc.lowWord += SIGNED(t[0]);
            //change_pc(PC);
            if (t[0] == (byte) 0xfe) {
                if (pendingCycles > 0) {
                    pendingCycles = 0;
                }
            }
        }
    }

    private void CLEAR_PAIR(/* ref */ Register p) {
        p.d = 0;
    }

    private void rd_s_handler_b(/* ref */ byte[] b) {
        SP_INC();
        b[0] = ReadMemory.apply(s.lowWord);
    }

    private void rd_s_handler_w(/* ref */ Register p) {
        CLEAR_PAIR(/* ref */ p);
        SP_INC();
        p.highByte = ReadMemory.apply(s.lowWord);
        SP_INC();
        p.lowByte = ReadMemory.apply(s.lowWord);
    }

    private void wr_s_handler_b(/* ref */ byte[] b) {
        WriteMemory.accept(s.lowWord, b[0]);
        SP_DEC();
    }

    private void wr_s_handler_w(/* ref */ Register p) {
        WriteMemory.accept(s.lowWord, p.lowByte);
        SP_DEC();
        WriteMemory.accept(s.lowWord, p.highByte);
        SP_DEC();
    }

    protected void RM16(int Addr, /* ref */ Register p) {
        CLEAR_PAIR(/* ref */ p);
        p.highByte = ReadMemory.apply((short) Addr);
        ++Addr;
        p.lowByte = ReadMemory.apply((short) Addr);
    }

    private void m68705_Interrupt() {
        if ((pending_interrupts & ((1 << 0) | 0x03)) != 0) {
            if ((cc & IFLAG) == 0) {
                PUSHWORD(/* ref */ pc);
                byte[] tmp = new byte[1];
                PUSHBYTE(/* ref */ tmp);
                x = tmp[0];
                PUSHBYTE(/* ref */ tmp);
                a = tmp[0];
                PUSHBYTE(/* ref */ tmp);
                cc = tmp[0];
                SEI();
                if (irq_callback != null) {
                    irq_callback.apply(0);
                }
                if ((pending_interrupts & (1 << 0)) != 0) {
                    pending_interrupts &= (short) (~(1 << 0));
                    RM16(0xfffa, /* ref */ pc);
                    //change_pc(PC);
                } else if ((pending_interrupts & (1 << 0x01)) != 0) {
                    pending_interrupts &= (short) (~(1 << 0x01));
                    RM16(0xfff8, /* ref */ pc);
                    //change_pc(PC);
                }
            }
            pendingCycles -= 11;
        }
    }

    private void Interrupt() {
        byte[] tmp = new byte[1];
        if ((pending_interrupts & (1 << 0x08)) != 0) {
            PUSHWORD(/* ref */ pc);
            PUSHBYTE(/* ref */ tmp);
            x = tmp[0];
            PUSHBYTE(/* ref */ tmp);
            a = tmp[0];
            PUSHBYTE(/* ref */ tmp);
            cc = tmp[0];
            SEI();
            if (irq_callback != null) {
                irq_callback.apply(0);
            }
            RM16(0x1ffc, /* ref */ pc);
            //change_pc(PC);
            pending_interrupts &= (short) (~(1 << 0x08));
            pendingCycles -= 11;
        } else if ((pending_interrupts & ((1 << 0) | 0x1ff)) != 0) {
            if ((cc & IFLAG) == 0) {
                {
                    PUSHWORD(/* ref */ pc);
                    PUSHBYTE(/* ref */ tmp);
                    x = tmp[0];
                    PUSHBYTE(/* ref */ tmp);
                    a = tmp[0];
                    PUSHBYTE(/* ref */ tmp);
                    cc = tmp[0];
                    SEI();
                    if (irq_callback != null) {
                        irq_callback.apply(0);
                    }
                    if (subtype == SUBTYPE_HD63705) {
                        if ((pending_interrupts & (1 << 0x00)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x00);
                            RM16(0x1ff8, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x01)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x01);
                            RM16(0x1fec, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x07)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x07);
                            RM16(0x1fea, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x02)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x02);
                            RM16(0x1ff6, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x03)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x03);
                            RM16(0x1ff4, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x04)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x04);
                            RM16(0x1ff2, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x05)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x05);
                            RM16(0x1ff0, /* ref */ pc);
                            //change_pc(PC);
                        } else if ((pending_interrupts & (1 << 0x06)) != 0) {
                            pending_interrupts &= (short) ~(1 << 0x06);
                            RM16(0x1fee, /* ref */ pc);
                            //change_pc(PC);
                        }
                    } else {
                        RM16(0xffff - 5, /* ref */ pc);
                        //change_pc(PC);
                    }

                }  // CC & IFLAG
                pending_interrupts &= (short) ~(1 << 0);
            }
            pendingCycles -= 11;
        }
    }

    private void m6805_init(irq_delegate irqcallback) {
        irq_callback = irqcallback;
    }

    protected void m6805_reset() {
//        int (*save_irqcallback)(int) = m6805.irq_callback;
//        memset(&m6805, 0, sizeof(m6805));
//        m6805.irq_callback = save_irqcallback;
        int i;
        ea.d = 0;
        pc.d = 0;
        s.d = 0;
        a = 0;
        x = 0;
        cc = 0;
        pending_interrupts = 0;
        for (i = 0; i < 9; i++) {
            irq_state[i] = 0;
        }
        nmi_state = 0;
        subtype = SUBTYPE_M6805;
        sp_mask = 0x07f;
        sp_low = 0x060;
        s.lowWord = sp_mask;
        SEI();
        RM16(0xfffe, /* ref */ pc);
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        if (irq_state[0] == state.ordinal()) {
            return;
        }
        irq_state[0] = state.ordinal();
        if (state != LineState.CLEAR_LINE) {
            pending_interrupts |= 1 << 0;
        }
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
    }

    @Override
    public int ExecuteCycles(int cycles) {
        return m6805_execute(cycles);
    }

    public int m6805_execute(int cycles) {
        byte ireg;
        pendingCycles = cycles;
        do {
            int prevCycles = pendingCycles;
            if (pending_interrupts != 0) {
                if (subtype == SUBTYPE_M68705) {
                    m68705_Interrupt();
                } else {
                    Interrupt();
                }
            }
            //debugger_instruction_hook(Machine, PC);
            ireg = ReadOp.apply(pc.lowWord++);
            switch (ireg & 0xff) {
                case 0x00:
                    brset((byte) 0x01);
                    break;
                case 0x01:
                    brclr((byte) 0x01);
                    break;
                case 0x02:
                    brset((byte) 0x02);
                    break;
                case 0x03:
                    brclr((byte) 0x02);
                    break;
                case 0x04:
                    brset((byte) 0x04);
                    break;
                case 0x05:
                    brclr((byte) 0x04);
                    break;
                case 0x06:
                    brset((byte) 0x08);
                    break;
                case 0x07:
                    brclr((byte) 0x08);
                    break;
                case 0x08:
                    brset((byte) 0x10);
                    break;
                case 0x09:
                    brclr((byte) 0x10);
                    break;
                case 0x0A:
                    brset((byte) 0x20);
                    break;
                case 0x0B:
                    brclr((byte) 0x20);
                    break;
                case 0x0C:
                    brset((byte) 0x40);
                    break;
                case 0x0D:
                    brclr((byte) 0x40);
                    break;
                case 0x0E:
                    brset((byte) 0x80);
                    break;
                case 0x0F:
                    brclr((byte) 0x80);
                    break;
                case 0x10:
                    bset((byte) 0x01);
                    break;
                case 0x11:
                    bclr((byte) 0x01);
                    break;
                case 0x12:
                    bset((byte) 0x02);
                    break;
                case 0x13:
                    bclr((byte) 0x02);
                    break;
                case 0x14:
                    bset((byte) 0x04);
                    break;
                case 0x15:
                    bclr((byte) 0x04);
                    break;
                case 0x16:
                    bset((byte) 0x08);
                    break;
                case 0x17:
                    bclr((byte) 0x08);
                    break;
                case 0x18:
                    bset((byte) 0x10);
                    break;
                case 0x19:
                    bclr((byte) 0x10);
                    break;
                case 0x1a:
                    bset((byte) 0x20);
                    break;
                case 0x1b:
                    bclr((byte) 0x20);
                    break;
                case 0x1c:
                    bset((byte) 0x40);
                    break;
                case 0x1d:
                    bclr((byte) 0x40);
                    break;
                case 0x1e:
                    bset((byte) 0x80);
                    break;
                case 0x1f:
                    bclr((byte) 0x80);
                    break;
                case 0x20:
                    bra();
                    break;
                case 0x21:
                    brn();
                    break;
                case 0x22:
                    bhi();
                    break;
                case 0x23:
                    bls();
                    break;
                case 0x24:
                    bcc();
                    break;
                case 0x25:
                    bcs();
                    break;
                case 0x26:
                    bne();
                    break;
                case 0x27:
                    beq();
                    break;
                case 0x28:
                    bhcc();
                    break;
                case 0x29:
                    bhcs();
                    break;
                case 0x2a:
                    bpl();
                    break;
                case 0x2b:
                    bmi();
                    break;
                case 0x2c:
                    bmc();
                    break;
                case 0x2d:
                    bms();
                    break;
                case 0x2e:
                    bil();
                    break;
                case 0x2f:
                    bih();
                    break;
                case 0x30:
                    neg_di();
                    break;
                case 0x31:
                    illegal();
                    break;
                case 0x32:
                    illegal();
                    break;
                case 0x33:
                    com_di();
                    break;
                case 0x34:
                    lsr_di();
                    break;
                case 0x35:
                    illegal();
                    break;
                case 0x36:
                    ror_di();
                    break;
                case 0x37:
                    asr_di();
                    break;
                case 0x38:
                    lsl_di();
                    break;
                case 0x39:
                    rol_di();
                    break;
                case 0x3a:
                    dec_di();
                    break;
                case 0x3b:
                    illegal();
                    break;
                case 0x3c:
                    inc_di();
                    break;
                case 0x3d:
                    tst_di();
                    break;
                case 0x3e:
                    illegal();
                    break;
                case 0x3f:
                    clr_di();
                    break;
                case 0x40:
                    nega();
                    break;
                case 0x41:
                    illegal();
                    break;
                case 0x42:
                    illegal();
                    break;
                case 0x43:
                    coma();
                    break;
                case 0x44:
                    lsra();
                    break;
                case 0x45:
                    illegal();
                    break;
                case 0x46:
                    rora();
                    break;
                case 0x47:
                    asra();
                    break;
                case 0x48:
                    lsla();
                    break;
                case 0x49:
                    rola();
                    break;
                case 0x4a:
                    deca();
                    break;
                case 0x4b:
                    illegal();
                    break;
                case 0x4c:
                    inca();
                    break;
                case 0x4d:
                    tsta();
                    break;
                case 0x4e:
                    illegal();
                    break;
                case 0x4f:
                    clra();
                    break;
                case 0x50:
                    negx();
                    break;
                case 0x51:
                    illegal();
                    break;
                case 0x52:
                    illegal();
                    break;
                case 0x53:
                    comx();
                    break;
                case 0x54:
                    lsrx();
                    break;
                case 0x55:
                    illegal();
                    break;
                case 0x56:
                    rorx();
                    break;
                case 0x57:
                    asrx();
                    break;
                case 0x58:
                    aslx();
                    break;
                case 0x59:
                    rolx();
                    break;
                case 0x5a:
                    decx();
                    break;
                case 0x5b:
                    illegal();
                    break;
                case 0x5c:
                    incx();
                    break;
                case 0x5d:
                    tstx();
                    break;
                case 0x5e:
                    illegal();
                    break;
                case 0x5f:
                    clrx();
                    break;
                case 0x60:
                    neg_ix1();
                    break;
                case 0x61:
                    illegal();
                    break;
                case 0x62:
                    illegal();
                    break;
                case 0x63:
                    com_ix1();
                    break;
                case 0x64:
                    lsr_ix1();
                    break;
                case 0x65:
                    illegal();
                    break;
                case 0x66:
                    ror_ix1();
                    break;
                case 0x67:
                    asr_ix1();
                    break;
                case 0x68:
                    lsl_ix1();
                    break;
                case 0x69:
                    rol_ix1();
                    break;
                case 0x6a:
                    dec_ix1();
                    break;
                case 0x6b:
                    illegal();
                    break;
                case 0x6c:
                    inc_ix1();
                    break;
                case 0x6d:
                    tst_ix1();
                    break;
                case 0x6e:
                    illegal();
                    break;
                case 0x6f:
                    clr_ix1();
                    break;
                case 0x70:
                    neg_ix();
                    break;
                case 0x71:
                    illegal();
                    break;
                case 0x72:
                    illegal();
                    break;
                case 0x73:
                    com_ix();
                    break;
                case 0x74:
                    lsr_ix();
                    break;
                case 0x75:
                    illegal();
                    break;
                case 0x76:
                    ror_ix();
                    break;
                case 0x77:
                    asr_ix();
                    break;
                case 0x78:
                    lsl_ix();
                    break;
                case 0x79:
                    rol_ix();
                    break;
                case 0x7a:
                    dec_ix();
                    break;
                case 0x7b:
                    illegal();
                    break;
                case 0x7c:
                    inc_ix();
                    break;
                case 0x7d:
                    tst_ix();
                    break;
                case 0x7e:
                    illegal();
                    break;
                case 0x7f:
                    clr_ix();
                    break;
                case 0x80:
                    rti();
                    break;
                case 0x81:
                    rts();
                    break;
                case 0x82:
                    illegal();
                    break;
                case 0x83:
                    swi();
                    break;
                case 0x84:
                    illegal();
                    break;
                case 0x85:
                    illegal();
                    break;
                case 0x86:
                    illegal();
                    break;
                case 0x87:
                    illegal();
                    break;
                case 0x88:
                    illegal();
                    break;
                case 0x89:
                    illegal();
                    break;
                case 0x8a:
                    illegal();
                    break;
                case 0x8b:
                    illegal();
                    break;
                case 0x8c:
                    illegal();
                    break;
                case 0x8d:
                    illegal();
                    break;
                case 0x8e:
                    illegal();
                    break;
                case 0x8f:
                    illegal();
                    break;
                case 0x90:
                    illegal();
                    break;
                case 0x91:
                    illegal();
                    break;
                case 0x92:
                    illegal();
                    break;
                case 0x93:
                    illegal();
                    break;
                case 0x94:
                    illegal();
                    break;
                case 0x95:
                    illegal();
                    break;
                case 0x96:
                    illegal();
                    break;
                case 0x97:
                    tax();
                    break;
                case 0x98:
                    CLC();
                    break;
                case 0x99:
                    SEC();
                    break;
                case 0x9a:
                    CLI();
                    break;
                case 0x9b:
                    SEI();
                    break;
                case 0x9c:
                    rsp();
                    break;
                case 0x9d:
                    nop();
                    break;
                case 0x9e:
                    illegal();
                    break;
                case 0x9f:
                    txa();
                    break;
                case 0xa0:
                    suba_im();
                    break;
                case 0xa1:
                    cmpa_im();
                    break;
                case 0xa2:
                    sbca_im();
                    break;
                case 0xa3:
                    cpx_im();
                    break;
                case 0xa4:
                    anda_im();
                    break;
                case 0xa5:
                    bita_im();
                    break;
                case 0xa6:
                    lda_im();
                    break;
                case 0xa7:
                    illegal();
                    break;
                case 0xa8:
                    eora_im();
                    break;
                case 0xa9:
                    adca_im();
                    break;
                case 0xaa:
                    ora_im();
                    break;
                case 0xab:
                    adda_im();
                    break;
                case 0xac:
                    illegal();
                    break;
                case 0xad:
                    bsr();
                    break;
                case 0xae:
                    ldx_im();
                    break;
                case 0xaf:
                    illegal();
                    break;
                case 0xb0:
                    suba_di();
                    break;
                case 0xb1:
                    cmpa_di();
                    break;
                case 0xb2:
                    sbca_di();
                    break;
                case 0xb3:
                    cpx_di();
                    break;
                case 0xb4:
                    anda_di();
                    break;
                case 0xb5:
                    bita_di();
                    break;
                case 0xb6:
                    lda_di();
                    break;
                case 0xb7:
                    sta_di();
                    break;
                case 0xb8:
                    eora_di();
                    break;
                case 0xb9:
                    adca_di();
                    break;
                case 0xba:
                    ora_di();
                    break;
                case 0xbb:
                    adda_di();
                    break;
                case 0xbc:
                    jmp_di();
                    break;
                case 0xbd:
                    jsr_di();
                    break;
                case 0xbe:
                    ldx_di();
                    break;
                case 0xbf:
                    stx_di();
                    break;
                case 0xc0:
                    suba_ex();
                    break;
                case 0xc1:
                    cmpa_ex();
                    break;
                case 0xc2:
                    sbca_ex();
                    break;
                case 0xc3:
                    cpx_ex();
                    break;
                case 0xc4:
                    anda_ex();
                    break;
                case 0xc5:
                    bita_ex();
                    break;
                case 0xc6:
                    lda_ex();
                    break;
                case 0xc7:
                    sta_ex();
                    break;
                case 0xc8:
                    eora_ex();
                    break;
                case 0xc9:
                    adca_ex();
                    break;
                case 0xca:
                    ora_ex();
                    break;
                case 0xcb:
                    adda_ex();
                    break;
                case 0xcc:
                    jmp_ex();
                    break;
                case 0xcd:
                    jsr_ex();
                    break;
                case 0xce:
                    ldx_ex();
                    break;
                case 0xcf:
                    stx_ex();
                    break;
                case 0xd0:
                    suba_ix2();
                    break;
                case 0xd1:
                    cmpa_ix2();
                    break;
                case 0xd2:
                    sbca_ix2();
                    break;
                case 0xd3:
                    cpx_ix2();
                    break;
                case 0xd4:
                    anda_ix2();
                    break;
                case 0xd5:
                    bita_ix2();
                    break;
                case 0xd6:
                    lda_ix2();
                    break;
                case 0xd7:
                    sta_ix2();
                    break;
                case 0xd8:
                    eora_ix2();
                    break;
                case 0xd9:
                    adca_ix2();
                    break;
                case 0xda:
                    ora_ix2();
                    break;
                case 0xdb:
                    adda_ix2();
                    break;
                case 0xdc:
                    jmp_ix2();
                    break;
                case 0xdd:
                    jsr_ix2();
                    break;
                case 0xde:
                    ldx_ix2();
                    break;
                case 0xdf:
                    stx_ix2();
                    break;
                case 0xe0:
                    suba_ix1();
                    break;
                case 0xe1:
                    cmpa_ix1();
                    break;
                case 0xe2:
                    sbca_ix1();
                    break;
                case 0xe3:
                    cpx_ix1();
                    break;
                case 0xe4:
                    anda_ix1();
                    break;
                case 0xe5:
                    bita_ix1();
                    break;
                case 0xe6:
                    lda_ix1();
                    break;
                case 0xe7:
                    sta_ix1();
                    break;
                case 0xe8:
                    eora_ix1();
                    break;
                case 0xe9:
                    adca_ix1();
                    break;
                case 0xea:
                    ora_ix1();
                    break;
                case 0xeb:
                    adda_ix1();
                    break;
                case 0xec:
                    jmp_ix1();
                    break;
                case 0xed:
                    jsr_ix1();
                    break;
                case 0xee:
                    ldx_ix1();
                    break;
                case 0xef:
                    stx_ix1();
                    break;
                case 0xf0:
                    suba_ix();
                    break;
                case 0xf1:
                    cmpa_ix();
                    break;
                case 0xf2:
                    sbca_ix();
                    break;
                case 0xf3:
                    cpx_ix();
                    break;
                case 0xf4:
                    anda_ix();
                    break;
                case 0xf5:
                    bita_ix();
                    break;
                case 0xf6:
                    lda_ix();
                    break;
                case 0xf7:
                    sta_ix();
                    break;
                case 0xf8:
                    eora_ix();
                    break;
                case 0xf9:
                    adca_ix();
                    break;
                case 0xfa:
                    ora_ix();
                    break;
                case 0xfb:
                    adda_ix();
                    break;
                case 0xfc:
                    jmp_ix();
                    break;
                case 0xfd:
                    jsr_ix();
                    break;
                case 0xfe:
                    ldx_ix();
                    break;
                case 0xff:
                    stx_ix();
                    break;
            }
            pendingCycles -= cycles1[ireg];
            int delta = prevCycles - pendingCycles;
            totalExecutedCycles += delta;
        }
        while (pendingCycles > 0);
        return cycles - pendingCycles;
    }

    public void SaveStateBinary(BinaryWriter writer) {
        int i;
        writer.write(ea.lowWord);
        writer.write(pc.lowWord);
        writer.write(s.lowWord);
        writer.write(subtype);
        writer.write(a);
        writer.write(x);
        writer.write(cc);
        writer.write(pending_interrupts);
        for (i = 0; i < 9; i++) {
            writer.write(irq_state[i]);
        }
        writer.write(nmi_state);
        writer.write(totalExecutedCycles);
        writer.write(pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        int i;
        ea.lowWord = reader.readUInt16();
        pc.lowWord = reader.readUInt16();
        s.lowWord = reader.readUInt16();
        subtype = reader.readInt32();
        a = reader.readByte();
        x = reader.readByte();
        cc = reader.readByte();
        pending_interrupts = reader.readUInt16();
        for (i = 0; i < 9; i++) {
            irq_state[i] = reader.readInt32();
        }
        nmi_state = reader.readInt32();
        totalExecutedCycles = reader.readUInt64();
        pendingCycles = reader.readInt32();
    }

//#region M6805op

    protected void illegal() {
    }

    /* $00/$02/$04/$06/$08/$0A/$0C/$0E BRSET direct,relative ---- */
    protected void brset(byte bit) {
        byte[] t= new byte[1], r= new byte[1];
        DIRBYTE(/* ref */ r);
        IMMBYTE(/* ref */ t);
        CLC();
        if ((r[0] & bit) != 0) {
            SEC();
            pc.lowWord += SIGNED(t[0]);
            if (t[0] == (byte) 0xfd) {
                /* speed up busy loops */
                if (pendingCycles > 0)
                    pendingCycles = 0;
            }
        }
    }

    /* $01/$03/$05/$07/$09/$0B/$0D/$0F BRCLR direct,relative ---- */
    protected void brclr(byte bit) {
        byte[] t = new byte[1], r = new byte[1];
        DIRBYTE(/* ref */ r);
        IMMBYTE(/* ref */ t);
        SEC();
        if ((r[0] & bit) == 0) {
            CLC();
            pc.lowWord += SIGNED(t[0]);
            if (t[0] == (byte) 0xfd) {
                /* speed up busy loops */
                if (pendingCycles > 0)
                    pendingCycles = 0;
            }
        }
    }

    /* $10/$12/$14/$16/$18/$1A/$1C/$1E BSET direct ---- */
    protected void bset(byte bit) {
        byte[] t = new byte[1];
        byte r;
        DIRBYTE(/* ref */ t);
        r = (byte) (t[0] | bit);
        WriteMemory.accept((short) ea.d, r);
    }

    /* $11/$13/$15/$17/$19/$1B/$1D/$1F BCLR direct ---- */
    protected void bclr(byte bit) {
        byte[] t = new byte[1];
        byte r;
        DIRBYTE(/* ref */ t);
        r = (byte) (t[0] & (~bit));
        WriteMemory.accept((short) ea.d, r);
    }

    /* $20 BRA relative ---- */
    protected void bra() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        pc.lowWord += SIGNED(t[0]);
        if (t[0] == (byte) 0xfe) {
            /* speed up busy loops */
            if (pendingCycles > 0)
                pendingCycles = 0;
        }
    }

    /* $21 BRN relative ---- */
    protected void brn() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
    }

    /* $22 BHI relative ---- */
    protected void bhi() {
        BRANCH((cc & (CFLAG | ZFLAG)) == 0);
    }

    /* $23 BLS relative ---- */
    protected void bls() {
        BRANCH((cc & (CFLAG | ZFLAG)) != 0);
    }

    /* $24 BCC relative ---- */
    protected void bcc() {
        BRANCH((cc & CFLAG) == 0);
    }

    /* $25 BCS relative ---- */
    protected void bcs() {
        BRANCH((cc & CFLAG) != 0);
    }

    /* $26 BNE relative ---- */
    protected void bne() {
        BRANCH((cc & ZFLAG) == 0);
    }

    /* $27 BEQ relative ---- */
    protected void beq() {
        BRANCH((cc & ZFLAG) != 0);
    }

    /* $28 BHCC relative ---- */
    protected void bhcc() {
        BRANCH((cc & HFLAG) == 0);
    }

    /* $29 BHCS relative ---- */
    protected void bhcs() {
        BRANCH((cc & HFLAG) != 0);
    }

    /* $2a BPL relative ---- */
    protected void bpl() {
        BRANCH((cc & NFLAG) == 0);
    }

    /* $2b BMI relative ---- */
    protected void bmi() {
        BRANCH((cc & NFLAG) != 0);
    }

    /* $2c BMC relative ---- */
    protected void bmc() {
        BRANCH((cc & IFLAG) == 0);
    }

    /* $2d BMS relative ---- */
    protected void bms() {
        BRANCH((cc & IFLAG) != 0);
    }

    /* $2e BIL relative ---- */
    protected void bil() {
        if (subtype == SUBTYPE_HD63705) {
            BRANCH(nmi_state != LineState.CLEAR_LINE.ordinal());
        } else {
            BRANCH(irq_state[0] != LineState.CLEAR_LINE.ordinal());
        }
    }

    /* $2f BIH relative ---- */
    protected void bih() {
        if (subtype == SUBTYPE_HD63705) {
            BRANCH(nmi_state == LineState.CLEAR_LINE.ordinal());
        } else {
            BRANCH(irq_state[0] == LineState.CLEAR_LINE.ordinal());
        }
    }

    /* $30 NEG direct -*** */
    protected void neg_di() {
        byte[] t = new byte[1];
        short r;
        DIRBYTE(/* ref */ t); r = (short) -t[0];
        CLR_NZC();
        SET_FLAGS8((byte) 0, t[0], r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $31 ILLEGAL */

    /* $32 ILLEGAL */

    /* $33 COM direct -**1 */
    protected void com_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t); t[0] = (byte) ~t[0];
        CLR_NZ();
        SET_NZ8(t[0]);
        SEC();
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $34 LSR direct -0** */
    protected void lsr_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        t[0] >>= 1;
        SET_Z8(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $35 ILLEGAL */

    /* $36 ROR direct -*** */
    protected void ror_di() {
        byte[] t = new byte[1];
        byte r;
        DIRBYTE(/* ref */ t);
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        r |= (byte) (t[0] >> 1);
        SET_NZ8(r);
        WriteMemory.accept((short) ea.d, r);
    }

    /* $37 ASR direct ?*** */
    protected void asr_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        t[0] >>= 1;
        t[0] |= (byte) ((t[0] & 0x40) << 1);
        SET_NZ8(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $38 LSL direct ?*** */
    protected void lsl_di() {
        byte[] t = new byte[1];
        short r;
        DIRBYTE(/* ref */ t);
        r = (short) (t[0] << 1);
        CLR_NZC();
        SET_FLAGS8(t[0], t[0], r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $39 ROL direct -*** */
    protected void rol_di() {
        byte[] b = new byte[1];
        short t = 0, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZC();
        SET_FLAGS8((byte) t, (byte) t, r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $3a DEC direct -**- */
    protected void dec_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        --t[0];
        CLR_NZ();
        SET_FLAGS8D(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $3b ILLEGAL */

    /* $3c INC direct -**- */
    protected void inc_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        ++t[0];
        CLR_NZ();
        SET_FLAGS8I(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $3d TST direct -**- */
    protected void tst_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        CLR_NZ();
        SET_NZ8(t[0]);
    }

    /* $3e ILLEGAL */

    /* $3f CLR direct -0100 */
    protected void clr_di() {
        DIRECT();
        CLR_NZC();
        SEZ();
        WriteMemory.accept((short) ea.d, (byte) 0);
    }

    /* $40 NEGA inherent ?*** */
    protected void nega() {
        short r;
        r = (short) -a;
        CLR_NZC();
        SET_FLAGS8((byte) 0, a, r);
        a = (byte) r;
    }

    /* $41 ILLEGAL */

    /* $42 ILLEGAL */

    /* $43 COMA inherent -**1 */
    protected void coma() {
        a = (byte) ~a;
        CLR_NZ();
        SET_NZ8(a);
        SEC();
    }

    /* $44 LSRA inherent -0** */
    protected void lsra() {
        CLR_NZC();
        cc |= (byte) (a & 0x01);
        a >>= 1;
        SET_Z8(a);
    }

    /* $45 ILLEGAL */

    /* $46 RORA inherent -*** */
    protected void rora() {
        byte r;
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (a & 0x01);
        r |= (byte) (a >> 1);
        SET_NZ8(r);
        a = r;
    }

    /* $47 ASRA inherent ?*** */
    protected void asra() {
        CLR_NZC();
        cc |= (byte) (a & 0x01);
        a = (byte) ((a & 0x80) | (a >> 1));
        SET_NZ8(a);
    }

    /* $48 LSLA inherent ?*** */
    protected void lsla() {
        short r;
        r = (short) (a << 1);
        CLR_NZC();
        SET_FLAGS8(a, a, r);
        a = (byte) r;
    }

    /* $49 ROLA inherent -*** */
    protected void rola() {
        short t, r;
        t = a;
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZC();
        SET_FLAGS8((byte) t, (byte) t, r);
        a = (byte) r;
    }

    /* $4a DECA inherent -**- */
    protected void deca() {
        --a;
        CLR_NZ();
        SET_FLAGS8D(a);
    }

    /* $4b ILLEGAL */

    /* $4c INCA inherent -**- */
    protected void inca() {
        ++a;
        CLR_NZ();
        SET_FLAGS8I(a);
    }

    /* $4d TSTA inherent -**- */
    protected void tsta() {
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $4e ILLEGAL */

    /* $4f CLRA inherent -010 */
    protected void clra() {
        a = 0;
        CLR_NZ();
        SEZ();
    }

    /* $50 NEGX inherent ?*** */
    protected void negx() {
        short r;
        r = (short) -x;
        CLR_NZC();
        SET_FLAGS8((byte) 0, x, r);
        x = (byte) r;
    }

    /* $51 ILLEGAL */

    /* $52 ILLEGAL */

    /* $53 COMX inherent -**1 */
    protected void comx() {
        x = (byte) ~x;
        CLR_NZ();
        SET_NZ8(x);
        SEC();
    }

    /* $54 LSRX inherent -0** */
    protected void lsrx() {
        CLR_NZC();
        cc |= (byte) (x & 0x01);
        x >>= 1;
        SET_Z8(x);
    }

    /* $55 ILLEGAL */

    /* $56 RORX inherent -*** */
    protected void rorx() {
        byte r;
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (x & 0x01);
        r |= (byte) (x >> 1);
        SET_NZ8(r);
        x = r;
    }

    /* $57 ASRX inherent ?*** */
    protected void asrx() {
        CLR_NZC();
        cc |= (byte) (x & 0x01);
        x = (byte) ((x & 0x80) | (x >> 1));
        SET_NZ8(x);
    }

    /* $58 ASLX inherent ?*** */
    protected void aslx() {
        short r;
        r = (short) (x << 1);
        CLR_NZC();
        SET_FLAGS8(x, x, r);
        x = (byte) r;
    }

    /* $59 ROLX inherent -*** */
    protected void rolx() {
        short t, r;
        t = x;
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZC();
        SET_FLAGS8((byte) t, (byte) t, r);
        x = (byte) r;
    }

    /* $5a DECX inherent -**- */
    protected void decx() {
        --x;
        CLR_NZ();
        SET_FLAGS8D(x);
    }

    /* $5b ILLEGAL */

    /* $5c INCX inherent -**- */
    protected void incx() {
        ++x;
        CLR_NZ();
        SET_FLAGS8I(x);
    }

    /* $5d TSTX inherent -**- */
    protected void tstx() {
        CLR_NZ();
        SET_NZ8(x);
    }

    /* $5e ILLEGAL */

    /* $5f CLRX inherent -010 */
    protected void clrx() {
        x = 0;
        CLR_NZC();
        SEZ();
    }

    /* $60 NEG indexed, 1 byte offset -*** */
    protected void neg_ix1() {
        byte[] t = new byte[1];
        short r;
        IDX1BYTE(/* ref */ t); r = (short) (-t[0]);
        CLR_NZC();
        SET_FLAGS8((byte) 0, t[0], r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $61 ILLEGAL */

    /* $62 ILLEGAL */

    /* $63 COM indexed, 1 byte offset -**1 */
    protected void com_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t); t[0] = (byte) ~t[0];
        CLR_NZ();
        SET_NZ8(t[0]);
        SEC();
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $64 LSR indexed, 1 byte offset -0** */
    protected void lsr_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        t[0] >>= 1;
        SET_Z8(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $65 ILLEGAL */

    /* $66 ROR indexed, 1 byte offset -*** */
    protected void ror_ix1() {
        byte[] t = new byte[1];
        byte r;
        IDX1BYTE(/* ref */ t);
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        r |= (byte) (t[0] >> 1);
        SET_NZ8(r);
        WriteMemory.accept((short) ea.d, r);
    }

    /* $67 ASR indexed, 1 byte offset ?*** */
    protected void asr_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        t[0] >>= 1;
        t[0] |= (byte) ((t[0] & 0x40) << 1);
        SET_NZ8(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $68 LSL indexed, 1 byte offset ?*** */
    protected void lsl_ix1() {
        byte[] t = new byte[1];
        short r;
        IDX1BYTE(/* ref */ t);
        r = (short) (t[0] << 1);
        CLR_NZC();
        SET_FLAGS8(t[0], t[0], r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $69 ROL indexed, 1 byte offset -*** */
    protected void rol_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZC();
        SET_FLAGS8((byte) t, (byte) t, r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $6a DEC indexed, 1 byte offset -**- */
    protected void dec_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        --t[0];
        CLR_NZ();
        SET_FLAGS8D(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $6b ILLEGAL */

    /* $6c INC indexed, 1 byte offset -**- */
    protected void inc_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        ++t[0];
        CLR_NZ();
        SET_FLAGS8I(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $6d TST indexed, 1 byte offset -**- */
    protected void tst_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        CLR_NZ();
        SET_NZ8(t[0]);
    }

    /* $6e ILLEGAL */

    /* $6f CLR indexed, 1 byte offset -0100 */
    protected void clr_ix1() {
        INDEXED1();
        CLR_NZC();
        SEZ();
        WriteMemory.accept((short) ea.d, (byte) 0);
    }

    /* $70 NEG indexed -*** */
    protected void neg_ix() {
        byte[] t = new byte[1];
        short r;
        IDXBYTE(/* ref */ t); r = (short) -t[0];
        CLR_NZC();
        SET_FLAGS8((byte) 0, t[0], r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $71 ILLEGAL */

    /* $72 ILLEGAL */

    /* $73 COM indexed -**1 */
    protected void com_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t); t[0] = (byte) ~t[0];
        CLR_NZ();
        SET_NZ8(t[0]);
        SEC();
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $74 LSR indexed -0** */
    protected void lsr_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        t[0] >>= 1;
        SET_Z8(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $75 ILLEGAL */

    /* $76 ROR indexed -*** */
    protected void ror_ix() {
        byte[] t = new byte[1];
        byte r;
        IDXBYTE(/* ref */ t);
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        r |= (byte) (t[0] >> 1);
        SET_NZ8(r);
        WriteMemory.accept((short) ea.d, r);
    }

    /* $77 ASR indexed ?*** */
    protected void asr_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        CLR_NZC();
        cc |= (byte) (t[0] & 0x01);
        t[0] = (byte) ((t[0] & 0x80) | (t[0] >> 1));
        SET_NZ8(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $78 LSL indexed ?*** */
    protected void lsl_ix() {
        byte[] t = new byte[1];
        short r;
        IDXBYTE(/* ref */ t); r = (short) (t[0] << 1);
        CLR_NZC();
        SET_FLAGS8(t[0], t[0], r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $79 ROL indexed -*** */
    protected void rol_ix() {
        byte[] b = new byte[1];
        short t = 0, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZC();
        SET_FLAGS8((byte) t, (byte) t, r);
        WriteMemory.accept((short) ea.d, (byte) r);
    }

    /* $7a DEC indexed -**- */
    protected void dec_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        --t[0];
        CLR_NZ();
        SET_FLAGS8D(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $7b ILLEGAL */

    /* $7c INC indexed -**- */
    protected void inc_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        ++t[0];
        CLR_NZ();
        SET_FLAGS8I(t[0]);
        WriteMemory.accept((short) ea.d, t[0]);
    }

    /* $7d TST indexed -**- */
    protected void tst_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        CLR_NZ();
        SET_NZ8(t[0]);
    }

    /* $7e ILLEGAL */

    /* $7f CLR indexed -0100 */
    protected void clr_ix() {
        INDEXED();
        CLR_NZC();
        SEZ();
        WriteMemory.accept((short) ea.d, (byte) 0);
    }

    /* $80 RTI inherent #### */
    protected void rti() {
        byte[] t = new byte[1];
        PULLBYTE(/* ref */ t);
        cc = t[0];
        PULLBYTE(/* ref */ t);
        a = t[0];
        PULLBYTE(/* ref */ t);
        x = t[0];
        PULLWORD(/* ref */ pc);
        //change_pc(PC);
    }

    /* $81 RTS inherent ---- */
    protected void rts() {
        PULLWORD(/* ref */ pc);
        //change_pc(PC);
    }

    /* $82 ILLEGAL */

    /* $83 SWI absolute indirect ---- */
    protected void swi() {
        byte[] t = new byte[1];
        PUSHWORD(/* ref */ pc);
        PUSHBYTE(/* ref */ t);
        x = t[0];
        PUSHBYTE(/* ref */ t);
        a = t[0];
        PUSHBYTE(/* ref */ t);
        cc = t[0];
        SEI();
        if (subtype == SUBTYPE_HD63705) {
            RM16(0x1ffa, /* ref */ pc);
        } else {
            RM16(0xfffc, /* ref */ pc);
        }
        //change_pc(PC);
    }

    /* $84 ILLEGAL */

    /* $85 ILLEGAL */

    /* $86 ILLEGAL */

    /* $87 ILLEGAL */

    /* $88 ILLEGAL */

    /* $89 ILLEGAL */

    /* $8A ILLEGAL */

    /* $8B ILLEGAL */

    /* $8C ILLEGAL */

    /* $8D ILLEGAL */

    /* $8E ILLEGAL */

    /* $8F ILLEGAL */

    /* $90 ILLEGAL */

    /* $91 ILLEGAL */

    /* $92 ILLEGAL */

    /* $93 ILLEGAL */

    /* $94 ILLEGAL */

    /* $95 ILLEGAL */

    /* $96 ILLEGAL */

    /* $97 TAX inherent ---- */
    protected void tax() {
        x = a;
    }

    /* $98 CLC */

    /* $99 SEC */

    /* $9A CLI */

    /* $9B SEI */

    /* $9C RSP inherent ---- */
    protected void rsp() {
        s.lowWord = (byte) sp_mask;
    }

    /* $9D NOP inherent ---- */
    protected void nop() {
    }

    /* $9E ILLEGAL */

    /* $9F TXA inherent ---- */
    protected void txa() {
        a = x;
    }

    /* $a0 SUBA immediate ?*** */
    protected void suba_im() {
        byte[] b = new byte[1];
        short t, r;
        IMMBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $a1 CMPA immediate ?*** */
    protected void cmpa_im() {
        byte[] b = new byte[1];
        short t, r;
        IMMBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
    }

    /* $a2 SBCA immediate ?*** */
    protected void sbca_im() {
        byte[] b = new byte[1];
        short t, r;
        IMMBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t - (cc & 0x01));
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $a3 CPX immediate -*** */
    protected void cpx_im() {
        byte[] b = new byte[1];
        short t, r;
        IMMBYTE(/* ref */ b);
        t = b[0];
        r = (short) (x - t);
        CLR_NZC();
        SET_FLAGS8(x, (byte) t, r);
    }

    /* $a4 ANDA immediate -**- */
    protected void anda_im() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        a &= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $a5 BITA immediate -**- */
    protected void bita_im() {
        byte[] t = new byte[1];
        byte r;
        IMMBYTE(/* ref */ t);
        r = (byte) (a & t[0]);
        CLR_NZ();
        SET_NZ8(r);
    }

    /* $a6 LDA immediate -**- */
    protected void lda_im() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        a = t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $a7 ILLEGAL */

    /* $a8 EORA immediate -**- */
    protected void eora_im() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        a ^= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $a9 ADCA immediate **** */
    protected void adca_im() {
        byte[] b = new byte[1];
        short t, r;
        IMMBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t + (cc & 0x01));
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $aa ORA immediate -**- */
    protected void ora_im() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        a |= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $ab ADDA immediate **** */
    protected void adda_im() {
        byte[] b = new byte[1];
        short t, r;
        IMMBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t);
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $ac ILLEGAL */

    /* $ad BSR ---- */
    protected void bsr() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        PUSHWORD(/* ref */ pc);
        pc.lowWord += SIGNED(t[0]);
    }

    /* $ae LDX immediate -**- */
    protected void ldx_im() {
        byte[] t = new byte[1];
        IMMBYTE(/* ref */ t);
        x = t[0];
        CLR_NZ();
        SET_NZ8(x);
    }

    /* $af ILLEGAL */

    /* $b0 SUBA direct ?*** */
    protected void suba_di() {
        byte[] b = new byte[1];
        short t, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $b1 CMPA direct ?*** */
    protected void cmpa_di() {
        byte[] b = new byte[1];
        short t, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
    }

    /* $b2 SBCA direct ?*** */
    protected void sbca_di() {
        byte[] b = new byte[1];
        short t, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t - (cc & 0x01));
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $b3 CPX direct -*** */
    protected void cpx_di() {
        byte[] b = new byte[1];
        short t, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (x - t);
        CLR_NZC();
        SET_FLAGS8(x, (byte) t, r);
    }

    /* $b4 ANDA direct -**- */
    protected void anda_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        a &= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $b5 BITA direct -**- */
    protected void bita_di() {
        byte[] t = new byte[1];
        byte r;
        DIRBYTE(/* ref */ t);
        r = (byte) (a & t[0]);
        CLR_NZ();
        SET_NZ8(r);
    }

    /* $b6 LDA direct -**- */
    protected void lda_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        a = t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $b7 STA direct -**- */
    protected void sta_di() {
        CLR_NZ();
        SET_NZ8(a);
        DIRECT();
        WriteMemory.accept((short) ea.d, a);
    }

    /* $b8 EORA direct -**- */
    protected void eora_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        a ^= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $b9 ADCA direct **** */
    protected void adca_di() {
        byte[] b = new byte[1];
        short t, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t + (cc & 0x01));
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $ba ORA direct -**- */
    protected void ora_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        a |= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $bb ADDA direct **** */
    protected void adda_di() {
        byte[] b = new byte[1];
        short t, r;
        DIRBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t);
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $bc JMP direct -*** */
    protected void jmp_di() {
        DIRECT();
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $bd JSR direct ---- */
    protected void jsr_di() {
        DIRECT();
        PUSHWORD(/* ref */ pc);
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $be LDX direct -**- */
    protected void ldx_di() {
        byte[] t = new byte[1];
        DIRBYTE(/* ref */ t);
        x = t[0];
        CLR_NZ();
        SET_NZ8(x);
    }

    /* $bf STX direct -**- */
    protected void stx_di() {
        CLR_NZ();
        SET_NZ8(x);
        DIRECT();
        WriteMemory.accept((short) ea.d, x);
    }

    /* $c0 SUBA extended ?*** */
    protected void suba_ex() {
        byte[] b = new byte[1];
        short t, r;
        EXTBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $c1 CMPA extended ?*** */
    protected void cmpa_ex() {
        byte[] b = new byte[1];
        short t, r;
        EXTBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
    }

    /* $c2 SBCA extended ?*** */
    protected void sbca_ex() {
        byte[] b = new byte[1];
        short t, r;
        EXTBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t - (cc & 0x01));
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $c3 CPX extended -*** */
    protected void cpx_ex() {
        byte[] b = new byte[1];
        short t, r;
        EXTBYTE(/* ref */ b);
        t = b[0];
        r = (short) (x - t);
        CLR_NZC();
        SET_FLAGS8(x, (byte) t, r);
    }

    /* $c4 ANDA extended -**- */
    protected void anda_ex() {
        byte[] t = new byte[1];
        EXTBYTE(/* ref */ t);
        a &= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $c5 BITA extended -**- */
    protected void bita_ex() {
        byte[] t = new byte[1];
        byte r;
        EXTBYTE(/* ref */ t);
        r = (byte) (a & t[0]);
        CLR_NZ();
        SET_NZ8(r);
    }

    /* $c6 LDA extended -**- */
    protected void lda_ex() {
        byte[] t = new byte[1];
        EXTBYTE(/* ref */ t);
        a = t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $c7 STA extended -**- */
    protected void sta_ex() {
        CLR_NZ();
        SET_NZ8(a);
        EXTENDED();
        WriteMemory.accept((short) ea.d, a);
    }

    /* $c8 EORA extended -**- */
    protected void eora_ex() {
        byte[] t = new byte[1];
        EXTBYTE(/* ref */ t);
        a ^= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $c9 ADCA extended **** */
    protected void adca_ex() {
        byte[] b = new byte[1];
        short t, r;
        EXTBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t + (cc & 0x01));
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $ca ORA extended -**- */
    protected void ora_ex() {
        byte[] t = new byte[1];
        EXTBYTE(/* ref */ t);
        a |= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $cb ADDA extended **** */
    protected void adda_ex() {
        byte[] b = new byte[1];
        short t, r;
        EXTBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t);
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $cc JMP extended -*** */
    protected void jmp_ex() {
        EXTENDED();
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $cd JSR extended ---- */
    protected void jsr_ex() {
        EXTENDED();
        PUSHWORD(/* ref */ pc);
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $ce LDX extended -**- */
    protected void ldx_ex() {
        byte[] t = new byte[1];
        EXTBYTE(/* ref */ t);
        x = t[0];
        CLR_NZ();
        SET_NZ8(x);
    }

    /* $cf STX extended -**- */
    protected void stx_ex() {
        CLR_NZ();
        SET_NZ8(x);
        EXTENDED();
        WriteMemory.accept((short) ea.d, x);
    }

    /* $d0 SUBA indexed, 2 byte offset ?*** */
    protected void suba_ix2() {
        byte[] b = new byte[1];
        short t, r;
        IDX2BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $d1 CMPA indexed, 2 byte offset ?*** */
    protected void cmpa_ix2() {
        byte[] b = new byte[1];
        short t, r;
        IDX2BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
    }

    /* $d2 SBCA indexed, 2 byte offset ?*** */
    protected void sbca_ix2() {
        byte[] b = new byte[1];
        short t, r;
        IDX2BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t - (cc & 0x01));
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $d3 CPX indexed, 2 byte offset -*** */
    protected void cpx_ix2() {
        byte[] b = new byte[1];
        short t, r;
        IDX2BYTE(/* ref */ b);
        t = b[0];
        r = (short) (x - t);
        CLR_NZC();
        SET_FLAGS8(x, (byte) t, r);
    }

    /* $d4 ANDA indexed, 2 byte offset -**- */
    protected void anda_ix2() {
        byte[] t = new byte[1];
        IDX2BYTE(/* ref */ t);
        a &= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $d5 BITA indexed, 2 byte offset -**- */
    protected void bita_ix2() {
        byte[] t = new byte[1];
        byte r;
        IDX2BYTE(/* ref */ t);
        r = (byte) (a & t[0]);
        CLR_NZ();
        SET_NZ8(r);
    }

    /* $d6 LDA indexed, 2 byte offset -**- */
    protected void lda_ix2() {
        byte[] t = new byte[1];
        IDX2BYTE(/* ref */ t);
        a = t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $d7 STA indexed, 2 byte offset -**- */
    protected void sta_ix2() {
        CLR_NZ();
        SET_NZ8(a);
        INDEXED2();
        WriteMemory.accept((short) ea.d, a);
    }

    /* $d8 EORA indexed, 2 byte offset -**- */
    protected void eora_ix2() {
        byte[] t = new byte[1];
        IDX2BYTE(/* ref */ t);
        a ^= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $d9 ADCA indexed, 2 byte offset **** */
    protected void adca_ix2() {
        byte[] b = new byte[1];
        short t, r;
        IDX2BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t + (cc & 0x01));
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $da ORA indexed, 2 byte offset -**- */
    protected void ora_ix2() {
        byte[] t = new byte[1];
        IDX2BYTE(/* ref */ t);
        a |= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $db ADDA indexed, 2 byte offset **** */
    protected void adda_ix2() {
        byte[] b = new byte[1];
        short t, r;
        IDX2BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t);
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $dc JMP indexed, 2 byte offset -*** */
    protected void jmp_ix2() {
        INDEXED2();
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $dd JSR indexed, 2 byte offset ---- */
    protected void jsr_ix2() {
        INDEXED2();
        PUSHWORD(/* ref */ pc);
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $de LDX indexed, 2 byte offset -**- */
    protected void ldx_ix2() {
        byte[] t = new byte[1];
        IDX2BYTE(/* ref */ t);
        x = t[0];
        CLR_NZ();
        SET_NZ8(x);
    }

    /* $df STX indexed, 2 byte offset -**- */
    protected void stx_ix2() {
        CLR_NZ();
        SET_NZ8(x);
        INDEXED2();
        WriteMemory.accept((short) ea.d, x);
    }

    /* $e0 SUBA indexed, 1 byte offset ?*** */
    protected void suba_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $e1 CMPA indexed, 1 byte offset ?*** */
    protected void cmpa_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
    }

    /* $e2 SBCA indexed, 1 byte offset ?*** */
    protected void sbca_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t - (cc & 0x01));
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /* $e3 CPX indexed, 1 byte offset -*** */
    protected void cpx_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (x - t);
        CLR_NZC();
        SET_FLAGS8(x, (byte) t, r);
    }

    /* $e4 ANDA indexed, 1 byte offset -**- */
    protected void anda_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        a &= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $e5 BITA indexed, 1 byte offset -**- */
    protected void bita_ix1() {
        byte[] t = new byte[1];
        byte r;
        IDX1BYTE(/* ref */ t);
        r = (byte) (a & t[0]);
        CLR_NZ();
        SET_NZ8(r);
    }

    /* $e6 LDA indexed, 1 byte offset -**- */
    protected void lda_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        a = t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $e7 STA indexed, 1 byte offset -**- */
    protected void sta_ix1() {
        CLR_NZ();
        SET_NZ8(a);
        INDEXED1();
        WriteMemory.accept((short) ea.d, a);
    }

    /* $e8 EORA indexed, 1 byte offset -**- */
    protected void eora_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        a ^= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /** $e9 ADCA indexed, 1 byte offset **** */
    protected void adca_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t + (cc & 0x01));
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /** $ea ORA indexed, 1 byte offset -**- */
    protected void ora_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        a |= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /** $eb ADDA indexed, 1 byte offset **** */
    protected void adda_ix1() {
        byte[] b = new byte[1];
        short t, r;
        IDX1BYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t);
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /** $ec JMP indexed, 1 byte offset -*** */
    protected void jmp_ix1() {
        INDEXED1();
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /** $ed JSR indexed, 1 byte offset ---- */
    protected void jsr_ix1() {
        INDEXED1();
        PUSHWORD(/* ref */ pc);
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /** $ee LDX indexed, 1 byte offset -**- */
    protected void ldx_ix1() {
        byte[] t = new byte[1];
        IDX1BYTE(/* ref */ t);
        x = t[0];
        CLR_NZ();
        SET_NZ8(x);
    }

    /** $ef STX indexed, 1 byte offset -**- */
    protected void stx_ix1() {
        CLR_NZ();
        SET_NZ8(x);
        INDEXED1();
        WriteMemory.accept((short) ea.d, x);
    }

    /** $f0 SUBA indexed ?*** */
    protected void suba_ix() {
        byte[] b = new byte[1];
        short t, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /** $f1 CMPA indexed ?*** */
    protected void cmpa_ix() {
        byte[] b = new byte[1];
        short t, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t);
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
    }

    /** $f2 SBCA indexed ?*** */
    protected void sbca_ix() {
        byte[] b = new byte[1];
        short t, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a - t - (cc & 0x01));
        CLR_NZC();
        SET_FLAGS8(a, (byte) t, r);
        a = (byte) r;
    }

    /** $f3 CPX indexed -*** */
    protected void cpx_ix() {
        byte[] b = new byte[1];
        short t, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (x - t);
        CLR_NZC();
        SET_FLAGS8(x, (byte) t, r);
    }

    /* $f4 ANDA indexed -**- */
    protected void anda_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        a &= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $f5 BITA indexed -**- */
    protected void bita_ix() {
        byte[] t = new byte[1];
        byte r;
        IDXBYTE(/* ref */ t);
        r = (byte) (a & t[0]);
        CLR_NZ();
        SET_NZ8(r);
    }

    /* $f6 LDA indexed -**- */
    protected void lda_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        a = t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $f7 STA indexed -**- */
    protected void sta_ix() {
        CLR_NZ();
        SET_NZ8(a);
        INDEXED();
        WriteMemory.accept((short) ea.d, a);
    }

    /* $f8 EORA indexed -**- */
    protected void eora_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        a ^= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $f9 ADCA indexed **** */
    protected void adca_ix() {
        byte[] b = new byte[1];
        short t, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t + (cc & 0x01));
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $fa ORA indexed -**- */
    protected void ora_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        a |= t[0];
        CLR_NZ();
        SET_NZ8(a);
    }

    /* $fb ADDA indexed **** */
    protected void adda_ix() {
        byte[] b = new byte[1];
        short t, r;
        IDXBYTE(/* ref */ b);
        t = b[0];
        r = (short) (a + t);
        CLR_HNZC();
        SET_FLAGS8(a, (byte) t, r);
        SET_H(a, (byte) t, (byte) r);
        a = (byte) r;
    }

    /* $fc JMP indexed -*** */
    protected void jmp_ix() {
        INDEXED();
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $fd JSR indexed ---- */
    protected void jsr_ix() {
        INDEXED();
        PUSHWORD(/* ref */ pc);
        pc.lowWord = ea.lowWord;
        //change_pc(PC);
    }

    /* $fe LDX indexed -**- */
    protected void ldx_ix() {
        byte[] t = new byte[1];
        IDXBYTE(/* ref */ t);
        x = t[0];
        CLR_NZ();
        SET_NZ8(x);
    }

    /* $ff STX indexed -**- */
    protected void stx_ix() {
        CLR_NZ();
        SET_NZ8(x);
        INDEXED();
        WriteMemory.accept((short) ea.d, x);
    }

//#endregion
}

