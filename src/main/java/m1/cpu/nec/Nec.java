/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.nec;


import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Cpuexec.cpuexec_data;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Cpuint.irq;
import m1.emu.Timer;


public class Nec extends cpuexec_data {

    protected long totalExecutedCycles;
    public int pendingCycles;

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

    @Override
    public int ExecuteCycles(int cycles) {
        return 0;
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        if (irqline == LineState.INPUT_LINE_NMI.ordinal()) {
            if (I.nmi_state == state.ordinal()) {
                return;
            }
            I.nmi_state = state.ordinal();
            if (state != LineState.CLEAR_LINE) {
                I.pending_irq |= 0x02;
            }
        } else {
            I.irq_state = state.ordinal();
            if (state == LineState.CLEAR_LINE) {
                I.pending_irq &= 0xffff_fffe;
            } else {
                I.pending_irq |= 0x01;
            }
        }
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        if (line >= 0 && line < 35) {
            Cpuint.lirq.add(new irq(cpunum, line, state, vector, Timer.getCurrentTime()));
            int event_index = Cpuint.input_event_index[cpunum][line]++;
            if (event_index >= 35) {
                Cpuint.input_event_index[cpunum][line]--;
                //Cpuint.cpunum_empty_event_queue(machine, NULL, cpunum | (line << 8));
                event_index = Cpuint.input_event_index[cpunum][line]++;
            }
            if (event_index < 35) {
                //Cpuint.input_event_queue[cpunum][line][event_index] = input_event;
                //if (event_index == 0)
                {
                    Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
                }
            }
        }
    }

    public static class necbasicregs {

        //public ushort[] w;//[8];
        public byte[] b; // [16];
    }

    public static class nec_Regs {

        public necbasicregs regs;
        public short[] sregs; // [4];
        public short ip;
        public int SignVal;
        public int AuxVal, OverVal, ZeroVal, CarryVal, ParityVal;
        public boolean TF, IF, DF, MF;
        public int int_vector;
        public int pending_irq;
        public int nmi_state;
        public int irq_state;
        public boolean poll_state;
        public byte no_interrupt;
        //int (*irq_callback)(int irqline);
        //memory_interface	mem;
        //const nec_config *config;
    }

    public static class Mod_RM {

        public int[] regw;
        public int[] regb;
        public int[] RMw;
        public int[] RMb;
    }

    public int iNOP;
    public static Nec[] nn1;
    public Mod_RM mod_RM;
    public byte[] v25v35_decryptiontable;
    public nec_Regs I;
    public int chip_type;
    int prefix_base;
    int seg_prefix;
    public static final int INT_IRQ = 0x01;
    public static final int NMI_IRQ = 0x02;
    public final boolean[] parity_table = new boolean[0x100];
    public Function<Integer, Byte> ReadOp, ReadOpArg;
    public Function<Integer, Byte> ReadByte;
    public BiConsumer<Integer, Byte> WriteByte;
    public Function<Integer, Short> ReadWord;
    public BiConsumer<Integer, Short> WriteWord;
    public Function<Integer, Byte> ReadIOByte;
    public BiConsumer<Integer, Byte> WriteIOByte;
    public Function<Integer, Short> ReadIOWord;
    public BiConsumer<Integer, Short> WriteIOWord;

    public interface nec_delegate extends Runnable {

    }

    public nec_delegate[] nec_instruction;

    public interface getea_delegate extends Supplier<Integer> {

    }

    public getea_delegate[] GetEA;

    public Nec() {
        nec_init();
    }

    private int DefaultBase(int Seg, nec_Regs I) {
        return (((seg_prefix != 0) && (Seg == 3 || Seg == 2)) ? prefix_base : (I.sregs[Seg] << 4));
    }

    private byte GetMemB(int Seg, int Off) {
        return ReadByte.apply(DefaultBase(Seg, I) + Off);
    }

    private short GetMemW(int Seg, int Off) {
        return ReadWord.apply(DefaultBase(Seg, I) + Off);
    }

    private void PutMemB(int Seg, int Off, byte x) {
        WriteByte.accept(DefaultBase(Seg, I) + Off, x);
    }

    private void PutMemW(int Seg, int Off, short x) {
        WriteWord.accept(DefaultBase(Seg, I) + Off, x);
    }

    private byte FETCH() {
        return ReadOpArg.apply(((I.sregs[1] << 4) + I.ip++) ^ 0);
    }

    public byte fetchop() {
        byte ret = ReadOp.apply(((I.sregs[1] << 4) + I.ip++) ^ 0);
        if (I.MF) {
            if (v25v35_decryptiontable != null) {
                ret = v25v35_decryptiontable[ret];
            }
        }
        return ret;
    }

    public short FETCHWORD() {
        short var = (short) ((ReadOpArg.apply((((I.sregs[1] & 0xffff) << 4) + (I.ip & 0xffff)) & 0xff) ^ 0) + ((ReadOpArg.apply((((I.sregs[1] & 0xffff) << 4) + (I.ip & 0xffff) + 1) ^ 0) & 0xff) << 8));
        I.ip += 2;
        return var;
    }

    public int GetModRM() {
        int ModRM = ReadOpArg.apply(((I.sregs[1] << 4) + I.ip++) ^ 0);
        return ModRM;
    }

    public void PUSH(short val) {
        //I.regs.w[4] -= 2;
        short w4 = (short) (I.regs.b[8] + I.regs.b[9] * 0x100 - 2);
        I.regs.b[8] = (byte) (w4 % 0x100);
        I.regs.b[9] = (byte) (w4 / 0x100);
        WriteWord.accept((I.sregs[2] << 4) + I.regs.b[8] + I.regs.b[9] * 0x100, val);
    }

    public void POP(/* ref */ short[] var, int varP) {
        var[varP] = ReadWord.apply((I.sregs[2] << 4) + I.regs.b[8] + I.regs.b[9] * 0x100);
        //I.regs.w[4] += 2;
        short w4 = (short) (I.regs.b[8] + I.regs.b[9] * 0x100 + 2);
        I.regs.b[8] = (byte) (w4 % 0x100);
        I.regs.b[9] = (byte) (w4 / 0x100);
    }

    public void POPW(int i) {
        short var = ReadWord.apply((I.sregs[2] << 4) + I.regs.b[8] + I.regs.b[9] * 0x100);
        I.regs.b[i * 2] = (byte) (var % 0x100);
        I.regs.b[i * 2 + 1] = (byte) (var / 0x100);
        short w4 = (short) (I.regs.b[8] + I.regs.b[9] * 0x100 + 2);
        I.regs.b[8] = (byte) (w4 % 0x100);
        I.regs.b[9] = (byte) (w4 / 0x100);
    }

    public byte PEEK(int addr) {
        return ReadOpArg.apply(addr ^ 0);
    }

    public byte PEEKOP(int addr) {
        return ReadOp.apply(addr ^ 0);
    }

    public void SetCFB(int x) {
        I.CarryVal = x & 0x100;
    }

    public void SetCFW(int x) {
        I.CarryVal = x & 0x1_0000;
    }

    public void SetAF(int x, int y, int z) {
        I.AuxVal = ((x) ^ ((y) ^ (z))) & 0x10;
    }

    public void SetSZPF_Byte(int x) {
        I.ZeroVal = I.ParityVal = (byte) x;
        I.SignVal = I.ZeroVal;
    }

    public void SetSZPF_Word(int x) {
        I.ZeroVal = I.ParityVal = (short) x;
        I.SignVal = I.ZeroVal;
    }

    public void SetOFW_Add(int x, int y, int z) {
        I.OverVal = ((x) ^ (y)) & ((x) ^ (z)) & 0x8000;
    }

    public void SetOFB_Add(int x, int y, int z) {
        I.OverVal = ((x) ^ (y)) & ((x) ^ (z)) & 0x80;
    }

    public void SetOFW_Sub(int x, int y, int z) {
        I.OverVal = ((z) ^ (y)) & ((z) ^ (x)) & 0x8000;
    }

    public void SetOFB_Sub(int x, int y, int z) {
        I.OverVal = ((z) ^ (y)) & ((z) ^ (x)) & 0x80;
    }

    public void ADDB(/* ref */ byte[] src, /* ref */ byte[] dst) {
        int res = dst[0] + src[0];
        SetCFB(res);
        SetOFB_Add(res, src[0], dst[0]);
        SetAF(res, src[0], dst[0]);
        SetSZPF_Byte(res);
        dst[0] = (byte) res;
    }

    public void ADDW(/* ref */ short[] src, /* ref */ short[] dst) {
        int res = dst[0] + src[0];
        SetCFW(res);
        SetOFW_Add(res, src[0], dst[0]);
        SetAF(res, src[0], dst[0]);
        SetSZPF_Word(res);
        dst[0] = (short) res;
    }

    public void SUBB(/* ref */ byte[] src, /* ref */ byte[] dst) {
        int res = dst[0] - src[0];
        SetCFB(res);
        SetOFB_Sub(res, src[0], dst[0]);
        SetAF(res, src[0], dst[0]);
        SetSZPF_Byte(res);
        dst[0] = (byte) res;
    }

    public void SUBW(/* ref */ short[] src, /* ref */ short[] dst) {
        int res = dst[0] - src[0];
        SetCFW(res);
        SetOFW_Sub(res, src[0], dst[0]);
        SetAF(res, src[0], dst[0]);
        SetSZPF_Word(res);
        dst[0] = (short) res;
    }

    public void ORB(/* ref */ byte[] src, /* ref */ byte[] dst) {
        dst[0] |= src[0];
        I.CarryVal = I.OverVal = I.AuxVal = 0;
        SetSZPF_Byte(dst[0]);
    }

    public void ORW(/* ref */ short[] src, /* ref */ short[] dst) {
        dst[0] |= src[0];
        I.CarryVal = I.OverVal = I.AuxVal = 0;
        SetSZPF_Word(dst[0]);
    }

    public void ANDB(/* ref */ byte[] src, /* ref */ byte[] dst) {
        dst[0] &= src[0];
        I.CarryVal = I.OverVal = I.AuxVal = 0;
        SetSZPF_Byte(dst[0]);
    }

    public void ANDW(/* ref */ short[] src, /* ref */ short[] dst) {
        dst[0] &= src[0];
        I.CarryVal = I.OverVal = I.AuxVal = 0;
        SetSZPF_Word(dst[0]);
    }

    public void XORB(/* ref */ byte[] src, /* ref */ byte[] dst) {
        dst[0] ^= src[0];
        I.CarryVal = I.OverVal = I.AuxVal = 0;
        SetSZPF_Byte(dst[0]);
    }

    public void XORW(/* ref */ short[] src, /* ref */ short[] dst) {
        dst[0] ^= src[0];
        I.CarryVal = I.OverVal = I.AuxVal = 0;
        SetSZPF_Word(dst[0]);
    }

    public boolean CF() {
        return (I.CarryVal != 0);
    }

    public boolean SF() {
        return (I.SignVal < 0);
    }

    public boolean ZF() {
        return (I.ZeroVal == 0);
    }

    public boolean PF() {
        return parity_table[(byte) I.ParityVal];
    }

    public boolean AF() {
        return (I.AuxVal != 0);
    }

    public boolean OF() {
        return (I.OverVal != 0);
    }

    public boolean MD() {
        return I.MF;
    }

    public void CLK(int all) {
        pendingCycles -= all;
    }

    public void CLKS(int v20, int v30, int v33) {
        int ccount = (v20 << 16) | (v30 << 8) | v33;
        pendingCycles -= (ccount >> chip_type) & 0x7f;
    }

    public void CLKW(int v20o, int v30o, int v33o, int v20e, int v30e, int v33e, int addr) {
        int ocount = (v20o << 16) | (v30o << 8) | v33o, ecount = (v20e << 16) | (v30e << 8) | v33e;
        pendingCycles -= ((addr & 1) != 0) ? ((ocount >> chip_type) & 0x7f) : ((ecount >> chip_type) & 0x7f);
    }

    public void CLKM(int ModRM, int v20, int v30, int v33, int v20m, int v30m, int v33m) {
        int ccount = (v20 << 16) | (v30 << 8) | v33, mcount = (v20m << 16) | (v30m << 8) | v33m;
        pendingCycles -= (ModRM >= 0xc0) ? ((ccount >> chip_type) & 0x7f) : ((mcount >> chip_type) & 0x7f);
    }

    public void CLKR(int ModRM, int v20o, int v30o, int v33o, int v20e, int v30e, int v33e, int vall, int addr) {
        int ocount = (v20o << 16) | (v30o << 8) | v33o, ecount = (v20e << 16) | (v30e << 8) | v33e;
        if (ModRM >= 0xc0) {
            pendingCycles -= vall;
        } else {
            pendingCycles -= ((addr & 1) != 0) ? ((ocount >> chip_type) & 0x7f) : ((ecount >> chip_type) & 0x7f);
        }
    }

    public short CompressFlags() {
        return (short) ((CF() ? 1 : 0) | ((PF() ? 1 : 0) << 2) | ((AF() ? 1 : 0) << 4) | ((ZF() ? 1 : 0) << 6) | ((SF() ? 1 : 0) << 7) | ((I.TF ? 1 : 0) << 8) | ((I.IF ? 1 : 0) << 9) | ((I.DF ? 1 : 0) << 10) | ((OF() ? 1 : 0) << 11) | ((MD() ? 1 : 0) << 15));
    }

    public void ExpandFlags(short f) {
        I.CarryVal = f & 1;
        I.ParityVal = (f & 4) == 0 ? 1 : 0;
        I.AuxVal = f & 16;
        I.ZeroVal = (f & 64) == 0 ? 1 : 0;
        I.SignVal = ((f & 128) != 0) ? -1 : 0;
        I.TF = (f & 256) == 256;
        I.IF = (f & 512) == 512;
        I.DF = (f & 1024) == 1024;
        I.OverVal = f & 2048;
        I.MF = (f & 0x8000) == 0x8000;
    }

    public void IncWordReg(int Reg) {
        int tmp = I.regs.b[Reg * 2] + I.regs.b[Reg * 2 + 1] * 0x100;
        int tmp1 = tmp + 1;
        I.OverVal = (tmp == 0x7fff) ? 1 : 0;
        SetAF(tmp1, tmp, 1);
        SetSZPF_Word(tmp1);
        //I.regs.w[Reg] = (ushort)tmp1;
        I.regs.b[Reg * 2] = (byte) ((short) tmp1 % 0x100);
        I.regs.b[Reg * 2 + 1] = (byte) ((short) tmp1 / 0x100);
    }

    public void DecWordReg(int Reg) {
        int tmp = I.regs.b[Reg * 2] + I.regs.b[Reg * 2 + 1] * 0x100;
        int tmp1 = tmp - 1;
        I.OverVal = (tmp == 0x8000) ? 1 : 0;
        SetAF(tmp1, tmp, 1);
        SetSZPF_Word(tmp1);
        //I.regs.w[Reg] = (ushort)tmp1;
        I.regs.b[Reg * 2] = (byte) ((short) tmp1 % 0x100);
        I.regs.b[Reg * 2 + 1] = (byte) ((short) tmp1 / 0x100);
    }

    public void JMP(boolean flag) {
        int tmp = FETCH();
        if (flag) {
            byte[] table = new byte[] {3, 10, 10};
            I.ip = (short) (I.ip + tmp);
            pendingCycles -= table[chip_type / 8];
            //PC = (I.sregs[1] << 4) + I.ip;
        }
    }

    public void ADJ4(int param1, int param2) {
        if (AF() || ((I.regs.b[0] & 0xf) > 9)) {
            short tmp;
            tmp = (short) (I.regs.b[0] + param1);
            I.regs.b[0] = (byte) tmp;
            I.AuxVal = 1;
            I.CarryVal |= tmp & 0x100;
        }
        if (CF() || ((I.regs.b[0] & 0xff) > 0x9f)) {
            I.regs.b[0] += (byte) param2;
            I.CarryVal = 1;
        }
        SetSZPF_Byte(I.regs.b[0]);
    }

    public void ADJB(int param1, int param2) {
        if (AF() || ((I.regs.b[0] & 0xf) > 9)) {
            I.regs.b[0] += (byte) param1;
            I.regs.b[1] += (byte) param2;
            I.AuxVal = 1;
            I.CarryVal = 1;
        } else {
            I.AuxVal = 0;
            I.CarryVal = 0;
        }
        I.regs.b[0] &= 0x0F;
    }

    public void BITOP_BYTE(/* ref */ int[] ModRM, /* ref */ int[] tmp) {
        ModRM[0] = FETCH();
        if (ModRM[0] >= 0xc0) {
            tmp[0] = I.regs.b[mod_RM.RMb[ModRM[0]]];
        } else {
            EA = GetEA[ModRM[0]].get();
            tmp[0] = ReadByte.apply(EA);
        }
    }

    public void BITOP_WORD(/* ref */ int[] ModRM, /* ref */ int[] tmp) {
        ModRM[0] = FETCH();
        if (ModRM[0] >= 0xc0) {
            tmp[0] = I.regs.b[mod_RM.RMw[ModRM[0]] * 2] + I.regs.b[mod_RM.RMw[ModRM[0]] * 2 + 1] * 0x100;
        } else {
            EA = GetEA[ModRM[0]].get();
            tmp[0] = ReadWord.apply(EA);
        }
    }

    public void BIT_NOT(/* ref */ int[] tmp, /* ref */ int[] tmp2) {
        if ((tmp[0] & (1 << tmp2[0])) != 0) {
            tmp[0] &= (~(1 << tmp2[0]));
        } else {
            tmp[0] |= (1 << tmp2[0]);
        }
    }

    public void XchgAWReg(int Reg) {
        short tmp;
        tmp = (short) (I.regs.b[Reg * 2] + I.regs.b[Reg * 2 + 1] * 0x100);
        //I.regs.w[Reg] = I.regs.w[0];
        //I.regs.w[0] = tmp;
        I.regs.b[Reg * 2] = I.regs.b[0];
        I.regs.b[Reg * 2 + 1] = I.regs.b[1];
        I.regs.b[0] = (byte) (tmp % 0x100);
        I.regs.b[1] = (byte) (tmp / 0x100);
    }

    public void ROL_BYTE(/* ref */ int[] dst) {
        I.CarryVal = dst[0] & 0x80;
        dst[0] = (dst[0] << 1) + (CF() ? 1 : 0);
    }

    public void ROL_WORD(/* ref */ int[] dst) {
        I.CarryVal = dst[0] & 0x8000;
        dst[0] = (dst[0] << 1) + (CF() ? 1 : 0);
    }

    public void ROR_BYTE(/* ref */ int[] dst) {
        I.CarryVal = dst[0] & 0x1;
        dst[0] = (dst[0] >> 1) + ((CF() ? 1 : 0) << 7);
    }

    public void ROR_WORD(/* ref */ int[] dst) {
        I.CarryVal = dst[0] & 0x1;
        dst[0] = (dst[0] >> 1) + ((CF() ? 1 : 0) << 15);
    }

    public void ROLC_BYTE(/* ref */ int[] dst) {
        dst[0] = (dst[0] << 1) + (CF() ? 1 : 0);
        SetCFB(dst[0]);
    }

    public void ROLC_WORD(/* ref */ int[] dst) {
        dst[0] = (dst[0] << 1) + (CF() ? 1 : 0);
        SetCFW(dst[0]);
    }

    public void RORC_BYTE(/* ref */ int[] dst) {
        dst[0] = ((CF() ? 1 : 0) << 8) + dst[0];
        I.CarryVal = dst[0] & 0x01;
        dst[0] >>= 1;
    }

    public void RORC_WORD(/* ref */ int[] dst) {
        dst[0] = ((CF() ? 1 : 0) << 16) + dst[0];
        I.CarryVal = dst[0] & 0x01;
        dst[0] >>= 1;
    }

    public void SHL_BYTE(int c, /* ref */ int[] dst, int ModRM) {
        pendingCycles -= c;
        dst[0] <<= c;
        SetCFB(dst[0]);
        SetSZPF_Byte(dst[0]);
        PutbackRMByte(ModRM, (byte) dst[0]);
    }

    public void SHL_WORD(int c, /* ref */ int[] dst, int ModRM) {
        pendingCycles -= c;
        dst[0] <<= c;
        SetCFW(dst[0]);
        SetSZPF_Word(dst[0]);
        PutbackRMWord(ModRM, (short) dst[0]);
    }

    public void SHR_BYTE(int c, /* ref */ int[] dst, int ModRM) {
        pendingCycles -= c;
        dst[0] >>= c - 1;
        I.CarryVal = dst[0] & 0x1;
        dst[0] >>= 1;
        SetSZPF_Byte(dst[0]);
        PutbackRMByte(ModRM, (byte) dst[0]);
    }

    public void SHR_WORD(int c, /* ref */ int[] dst, int ModRM) {
        pendingCycles -= c;
        dst[0] >>= c - 1;
        I.CarryVal = dst[0] & 0x1;
        dst[0] >>= 1;
        SetSZPF_Word(dst[0]);
        PutbackRMWord(ModRM, (short) dst[0]);
    }

    public void SHRA_BYTE(int c, /* ref */ int[] dst, int ModRM) {
        pendingCycles -= c;
        dst[0] = ((byte) dst[0]) >> (c - 1);
        I.CarryVal = dst[0] & 0x1;
        dst[0] = (byte) dst[0] >> 1;
        SetSZPF_Byte(dst[0]);
        PutbackRMByte(ModRM, (byte) dst[0]);
    }

    public void SHRA_WORD(int c, /* ref */ int[] dst, int ModRM) {
        pendingCycles -= c;
        dst[0] = ((short) dst[0]) >> (c - 1);
        I.CarryVal = dst[0] & 0x1;
        dst[0] = (short) dst[0] >> 1;
        SetSZPF_Word(dst[0]);
        PutbackRMWord(ModRM, (short) dst[0]);
    }

    public void DIVUB(int tmp, /* out */ boolean[] b1) {
        int uresult, uresult2;
        b1[0] = false;
        uresult = I.regs.b[0] + I.regs.b[1] * 0x100;
        uresult2 = uresult % tmp;
        if ((uresult /= tmp) > 0xff) {
            nec_interrupt(0, false);
            b1[0] = true;
        } else {
            I.regs.b[0] = (byte) uresult;
            I.regs.b[1] = (byte) uresult2;
        }
    }

    public void DIVB(int tmp, /* out */ boolean[] b1) {
        int result, result2;
        b1[0] = false;
        result = (short) (I.regs.b[0] + I.regs.b[1] * 0x100);
        result2 = result % (short) ((byte) tmp);
        if ((result /= (short) ((byte) tmp)) > 0xff) {
            nec_interrupt(0, false);
            b1[0] = true;
        } else {
            I.regs.b[0] = (byte) result;
            I.regs.b[1] = (byte) result2;
        }
    }

    public void DIVUW(int tmp, /* out */ boolean[] b1) {
        int uresult, uresult2;
        b1[0] = false;
        uresult = ((I.regs.b[4] + I.regs.b[5] * 0x100) << 16) | (I.regs.b[0] + I.regs.b[1] * 0x100);
        uresult2 = uresult % tmp;
        if ((uresult /= tmp) > 0xffff) {
            nec_interrupt(0, false);
            b1[0] = true;
        } else {
            //I.regs.w[0] = (ushort)uresult;
            //I.regs.w[2] = (ushort)uresult2;
            I.regs.b[0] = (byte) (uresult % 0x100);
            I.regs.b[1] = (byte) (uresult / 0x100);
            I.regs.b[4] = (byte) (uresult2 % 0x100);
            I.regs.b[5] = (byte) (uresult2 / 0x100);
        }
    }

    public void DIVW(int tmp, /* out */ boolean[] b1) {
        int result, result2;
        b1[0] = false;
        result = ((I.regs.b[4] + I.regs.b[5] * 0x100) << 16) + (I.regs.b[0] + I.regs.b[1] * 0x100);
        result2 = result % (int) ((short) tmp);
        if ((result /= (short) tmp) > 0xffff) {
            nec_interrupt(0, false);
            b1[0] = true;
        } else {
            //I.regs.w[0] = (ushort)result;
            //I.regs.w[2] = (ushort)result2;
            I.regs.b[0] = (byte) ((short) result % 0x100);
            I.regs.b[1] = (byte) ((short) result / 0x100);
            I.regs.b[4] = (byte) ((short) result2 % 0x100);
            I.regs.b[5] = (byte) ((short) result2 / 0x100);
        }
    }

    public void ADD4S(/* ref */ int[] tmp, /* ref */ int[] tmp2) {
        int i, v1, v2, result;
        int count = (I.regs.b[2] + 1) / 2;
        short di = (short) (I.regs.b[14] + I.regs.b[15] * 0x100);
        short si = (short) (I.regs.b[12] + I.regs.b[13] * 0x100);
        byte[] table = new byte[] {18, 19, 19};
        I.ZeroVal = I.CarryVal = 0;
        for (i = 0; i < count; i++) {
            pendingCycles -= table[chip_type / 8];
            tmp[0] = GetMemB(3, si);
            tmp2[0] = GetMemB(0, di);
            v1 = (tmp[0] >> 4) * 10 + (tmp[0] & 0xf);
            v2 = (tmp2[0] >> 4) * 10 + (tmp2[0] & 0xf);
            result = v1 + v2 + I.CarryVal;
            I.CarryVal = result > 99 ? 1 : 0;
            result = result % 100;
            v1 = ((result / 10) << 4) | (result % 10);
            PutMemB(0, di, (byte) v1);
            if (v1 != 0) {
                I.ZeroVal = 1;
            }
            si++;
            di++;
        }
    }

    public void SUB4S(/* ref */ int[] tmp, /* ref */ int[] tmp2) {
        int count = (I.regs.b[2] + 1) / 2;
        int i, v1, v2, result;
        short di = (short) (I.regs.b[14] + I.regs.b[15] * 0x100);
        short si = (short) (I.regs.b[12] + I.regs.b[13] * 0x100);
        byte[] table = new byte[] {
                18, 19, 19
        };
        I.ZeroVal = I.CarryVal = 0;
        for (i = 0; i < count; i++) {
            pendingCycles -= table[chip_type / 8];
            tmp[0] = GetMemB(0, di);
            tmp2[0] = GetMemB(3, si);
            v1 = (tmp[0] >> 4) * 10 + (tmp[0] & 0xf);
            v2 = (tmp2[0] >> 4) * 10 + (tmp2[0] & 0xf);
            if (v1 < (v2 + I.CarryVal)) {
                v1 += 100;
                result = v1 - (v2 + I.CarryVal);
                I.CarryVal = 1;
            } else {
                result = v1 - (v2 + I.CarryVal);
                I.CarryVal = 0;
            }
            v1 = ((result / 10) << 4) | (result % 10);
            PutMemB(0, di, (byte) v1);
            if (v1 != 0) {
                I.ZeroVal = 1;
            }
            si++;
            di++;
        }
    }

    private void CMP4S(/* ref */ int[] tmp, /* ref */ int[] tmp2) {
        int count = (I.regs.b[2] + 1) / 2;
        int i, v1, v2, result;
        short di = (short) (I.regs.b[14] + I.regs.b[15] * 0x100);
        short si = (short) (I.regs.b[12] + I.regs.b[13] * 0x100);
        byte[] table = new byte[] {
                14, 19, 19
        };
        I.ZeroVal = I.CarryVal = 0;
        for (i = 0; i < count; i++) {
            pendingCycles -= table[chip_type / 8];
            tmp[0] = GetMemB(0, di);
            tmp2[0] = GetMemB(3, si);
            v1 = (tmp[0] >> 4) * 10 + (tmp[0] & 0xf);
            v2 = (tmp2[0] >> 4) * 10 + (tmp2[0] & 0xf);
            if (v1 < (v2 + I.CarryVal)) {
                v1 += 100;
                result = v1 - (v2 + I.CarryVal);
                I.CarryVal = 1;
            } else {
                result = v1 - (v2 + I.CarryVal);
                I.CarryVal = 0;
            }
            v1 = ((result / 10) << 4) | (result % 10);
            if (v1 != 0) {
                I.ZeroVal = 1;
            }
            si++;
            di++;
        }
    }

    public void nec_init() {
        mod_RM = new Mod_RM();
        mod_RM.regw = new int[256];
        mod_RM.regb = new int[256];
        mod_RM.RMw = new int[256];
        mod_RM.RMb = new int[256];
        nec_instruction = new nec_delegate[] {
                this::i_add_br8,
                this::i_add_wr16,
                this::i_add_r8b,
                this::i_add_r16w,
                this::i_add_ald8,
                this::i_add_axd16,
                this::i_push_es,
                this::i_pop_es,
                this::i_or_br8,
                this::i_or_wr16,
                this::i_or_r8b,
                this::i_or_r16w,
                this::i_or_ald8,
                this::i_or_axd16,
                this::i_push_cs,
                this::i_pre_nec,
                this::i_adc_br8,
                this::i_adc_wr16,
                this::i_adc_r8b,
                this::i_adc_r16w,
                this::i_adc_ald8,
                this::i_adc_axd16,
                this::i_push_ss,
                this::i_pop_ss,
                this::i_sbb_br8,
                this::i_sbb_wr16,
                this::i_sbb_r8b,
                this::i_sbb_r16w,
                this::i_sbb_ald8,
                this::i_sbb_axd16,
                this::i_push_ds,
                this::i_pop_ds,
                this::i_and_br8,
                this::i_and_wr16,
                this::i_and_r8b,
                this::i_and_r16w,
                this::i_and_ald8,
                this::i_and_axd16,
                this::i_es,
                this::i_daa,
                this::i_sub_br8,
                this::i_sub_wr16,
                this::i_sub_r8b,
                this::i_sub_r16w,
                this::i_sub_ald8,
                this::i_sub_axd16,
                this::i_cs,
                this::i_das,
                this::i_xor_br8,
                this::i_xor_wr16,
                this::i_xor_r8b,
                this::i_xor_r16w,
                this::i_xor_ald8,
                this::i_xor_axd16,
                this::i_ss,
                this::i_aaa,
                this::i_cmp_br8,
                this::i_cmp_wr16,
                this::i_cmp_r8b,
                this::i_cmp_r16w,
                this::i_cmp_ald8,
                this::i_cmp_axd16,
                this::i_ds,
                this::i_aas,
                this::i_inc_ax,
                this::i_inc_cx,
                this::i_inc_dx,
                this::i_inc_bx,
                this::i_inc_sp,
                this::i_inc_bp,
                this::i_inc_si,
                this::i_inc_di,
                this::i_dec_ax,
                this::i_dec_cx,
                this::i_dec_dx,
                this::i_dec_bx,
                this::i_dec_sp,
                this::i_dec_bp,
                this::i_dec_si,
                this::i_dec_di,
                this::i_push_ax,
                this::i_push_cx,
                this::i_push_dx,
                this::i_push_bx,
                this::i_push_sp,
                this::i_push_bp,
                this::i_push_si,
                this::i_push_di,
                this::i_pop_ax,
                this::i_pop_cx,
                this::i_pop_dx,
                this::i_pop_bx,
                this::i_pop_sp,
                this::i_pop_bp,
                this::i_pop_si,
                this::i_pop_di,
                this::i_pusha,
                this::i_popa,
                this::i_chkind,
                this::i_brkn,
                this::i_repnc,
                this::i_repc,
                this::i_invalid,
                this::i_invalid,
                this::i_push_d16,
                this::i_imul_d16,
                this::i_push_d8,
                this::i_imul_d8,
                this::i_insb,
                this::i_insw,
                this::i_outsb,
                this::i_outsw,
                this::i_jo,
                this::i_jno,
                this::i_jc,
                this::i_jnc,
                this::i_jz,
                this::i_jnz,
                this::i_jce,
                this::i_jnce,
                this::i_js,
                this::i_jns,
                this::i_jp,
                this::i_jnp,
                this::i_jl,
                this::i_jnl,
                this::i_jle,
                this::i_jnle,
                this::i_80pre,
                this::i_81pre,
                this::i_82pre,
                this::i_83pre,
                this::i_test_br8,
                this::i_test_wr16,
                this::i_xchg_br8,
                this::i_xchg_wr16,
                this::i_mov_br8,
                this::i_mov_wr16,
                this::i_mov_r8b,
                this::i_mov_r16w,
                this::i_mov_wsreg,
                this::i_lea,
                this::i_mov_sregw,
                this::i_popw,
                this::i_nop,
                this::i_xchg_axcx,
                this::i_xchg_axdx,
                this::i_xchg_axbx,
                this::i_xchg_axsp,
                this::i_xchg_axbp,
                this::i_xchg_axsi,
                this::i_xchg_axdi,
                this::i_cbw,
                this::i_cwd,
                this::i_call_far,
                this::i_wait,
                this::i_pushf,
                this::i_popf,
                this::i_sahf,
                this::i_lahf,
                this::i_mov_aldisp,
                this::i_mov_axdisp,
                this::i_mov_dispal,
                this::i_mov_dispax,
                this::i_movsb,
                this::i_movsw,
                this::i_cmpsb,
                this::i_cmpsw,
                this::i_test_ald8,
                this::i_test_axd16,
                this::i_stosb,
                this::i_stosw,
                this::i_lodsb,
                this::i_lodsw,
                this::i_scasb,
                this::i_scasw,
                this::i_mov_ald8,
                this::i_mov_cld8,
                this::i_mov_dld8,
                this::i_mov_bld8,
                this::i_mov_ahd8,
                this::i_mov_chd8,
                this::i_mov_dhd8,
                this::i_mov_bhd8,
                this::i_mov_axd16,
                this::i_mov_cxd16,
                this::i_mov_dxd16,
                this::i_mov_bxd16,
                this::i_mov_spd16,
                this::i_mov_bpd16,
                this::i_mov_sid16,
                this::i_mov_did16,
                this::i_rotshft_bd8,
                this::i_rotshft_wd8,
                this::i_ret_d16,
                this::i_ret,
                this::i_les_dw,
                this::i_lds_dw,
                this::i_mov_bd8,
                this::i_mov_wd16,
                this::i_enter,
                this::i_leave,
                this::i_retf_d16,
                this::i_retf,
                this::i_int3,
                this::i_int,
                this::i_into,
                this::i_iret,
                this::i_rotshft_b,
                this::i_rotshft_w,
                this::i_rotshft_bcl,
                this::i_rotshft_wcl,
                this::i_aam,
                this::i_aad,
                this::i_setalc,
                this::i_trans,
                this::i_fpo,
                this::i_fpo,
                this::i_fpo,
                this::i_fpo,
                this::i_fpo,
                this::i_fpo,
                this::i_fpo,
                this::i_fpo,
                this::i_loopne,
                this::i_loope,
                this::i_loop,
                this::i_jcxz,
                this::i_inal,
                this::i_inax,
                this::i_outal,
                this::i_outax,
                this::i_call_d16,
                this::i_jmp_d16,
                this::i_jmp_far,
                this::i_jmp_d8,
                this::i_inaldx,
                this::i_inaxdx,
                this::i_outdxal,
                this::i_outdxax,
                this::i_lock,
                this::i_invalid,
                this::i_repne,
                this::i_repe,
                this::i_hlt,
                this::i_cmc,
                this::i_f6pre,
                this::i_f7pre,
                this::i_clc,
                this::i_stc,
                this::i_di,
                this::i_ei,
                this::i_cld,
                this::i_std,
                this::i_fepre,
                this::i_ffpre
        };
        GetEA = new getea_delegate[] {
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,
                this::EA_000, this::EA_001, this::EA_002, this::EA_003, this::EA_004, this::EA_005, this::EA_006, this::EA_007,

                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,
                this::EA_100, this::EA_101, this::EA_102, this::EA_103, this::EA_104, this::EA_105, this::EA_106, this::EA_107,

                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207,
                this::EA_200, this::EA_201, this::EA_202, this::EA_203, this::EA_204, this::EA_205, this::EA_206, this::EA_207
        };
    }

    @Override
    public void Reset() {
        nec_reset();
    }

    public void nec_reset() {
        //const nec_config *config;
        int i, j, c;
        //BREGS[] reg_name = new BREGS[8] { BREGS.AL, BREGS.CL, BREGS.DL, BREGS.BL, BREGS.AH, BREGS.CH, BREGS.DH, BREGS.BH };
        int[] reg_name = new int[] {
                0, 2, 4, 6, 1, 3, 5, 7
        };
        //int (*save_irqcallback)(int);
        //memory_interface save_mem;
        //save_irqcallback = I.irq_callback;
        //save_mem = I.mem;
        //config = I.config;
        I.sregs = new short[4];
        I.regs.b = new byte[16];
        for (i = 0; i < 4; i++) {
            I.sregs[i] = 0;
        }
        I.ip = 0;
        I.SignVal = 0;
        I.AuxVal = 0;
        I.OverVal = 0;
        I.ZeroVal = 0;
        I.CarryVal = 0;
        I.ParityVal = 0;
        I.TF = false;
        I.IF = false;
        I.DF = false;
        I.MF = false;
        I.int_vector = 0;
        I.pending_irq = 0;
        I.irq_state = 0;
        I.poll_state = false;
        I.no_interrupt = 0;
        //I.irq_callback = save_irqcallback;
        //I.mem = save_mem;
        //I.config = config;
        I.sregs[1] = (short) 0xffff;
        //PC = (I.sregs[1] << 4) + I.ip;
        for (i = 0; i < 256; i++) {
            for (j = i, c = 0; j > 0; j >>= 1) {
                if ((j & 1) != 0) {
                    c++;
                }
            }
            parity_table[i] = ((c & 1) == 0);
        }
        I.ZeroVal = I.ParityVal = 1;
        I.MF = true;
        for (i = 0; i < 256; i++) {
            mod_RM.regb[i] = reg_name[(i & 0x38) >> 3];
            mod_RM.regw[i] = (i & 0x38) >> 3;
        }
        for (i = 0xc0; i < 0x100; i++) {
            mod_RM.RMw[i] = i & 7;
            mod_RM.RMb[i] = reg_name[i & 7];
        }
        I.poll_state = true;
    }

    public void nec_interrupt(int int_num, boolean md_flag) {
        int dest_seg, dest_off;
        i_pushf();
        I.TF = I.IF = false;
        if (md_flag) {
            I.MF = false;
        }
        if (int_num == -1) {
            int_num = Cpuint.cpu_irq_callback(cpunum, 0);
            I.irq_state = 0;
            I.pending_irq &= 0xffff_fffe;
        }
        dest_off = ReadWord.apply(int_num * 4);
        dest_seg = ReadWord.apply(int_num * 4 + 2);
        PUSH(I.sregs[1]);
        PUSH(I.ip);
        I.ip = (short) dest_off;
        I.sregs[1] = (short) dest_seg;
        //CHANGE_PC;
    }

    public void nec_trap() {
        nec_instruction[fetchop()].run();
        nec_interrupt(1, false);
    }

    public void external_int() {
        if ((I.pending_irq & 0x02) != 0) {
            nec_interrupt(2, false);
            I.pending_irq &= ~2;
        } else if (I.pending_irq != 0) {
            nec_interrupt(-1, false);
        }
    }

    public void SaveStateBinary(BinaryWriter writer) {
        int i;
        writer.write(I.regs.b, 0, 16);
        for (i = 0; i < 4; i++) {
            writer.write(I.sregs[i]);
        }
        writer.write(I.ip);
        writer.write(I.TF);
        writer.write(I.IF);
        writer.write(I.DF);
        writer.write(I.MF);
        writer.write(I.SignVal);
        writer.write(I.int_vector);
        writer.write(I.pending_irq);
        writer.write(I.nmi_state);
        writer.write(I.irq_state);
        writer.write(I.poll_state);
        writer.write(I.AuxVal);
        writer.write(I.OverVal);
        writer.write(I.ZeroVal);
        writer.write(I.CarryVal);
        writer.write(I.ParityVal);
        writer.write(I.no_interrupt);
        writer.write(prefix_base);
        writer.write(seg_prefix);
        writer.write(totalExecutedCycles);
        writer.write(pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        int i;
        I.regs.b = reader.readBytes(16);
        for (i = 0; i < 4; i++) {
            I.sregs[i] = reader.readUInt16();
        }
        I.ip = reader.readUInt16();
        I.TF = reader.readBoolean();
        I.IF = reader.readBoolean();
        I.DF = reader.readBoolean();
        I.MF = reader.readBoolean();
        I.SignVal = reader.readInt32();
        I.int_vector = reader.readUInt32();
        I.pending_irq = reader.readUInt32();
        I.nmi_state = reader.readUInt32();
        I.irq_state = reader.readUInt32();
        I.poll_state = reader.readBoolean();
        I.AuxVal = reader.readUInt32();
        I.OverVal = reader.readUInt32();
        I.ZeroVal = reader.readUInt32();
        I.CarryVal = reader.readUInt32();
        I.ParityVal = reader.readUInt32();
        I.no_interrupt = reader.readByte();
        prefix_base = reader.readInt32();
        seg_prefix = reader.readInt32();
        totalExecutedCycles = reader.readUInt64();
        pendingCycles = reader.readInt32();
    }

//#region NecEa

    static int EA;
    static short EO;
    static short E16;

    int EA_000() {
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[12] + I.regs.b[13] * 0x100);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_001() {
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[14] + I.regs.b[15] * 0x100);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_002() {
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + I.regs.b[12] + I.regs.b[13] * 0x100);
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_003() {
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + I.regs.b[14] + I.regs.b[15] * 0x100);
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_004() {
        EO = (short) (I.regs.b[12] + I.regs.b[13] * 0x100);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_005() {
        EO = (short) (I.regs.b[14] + I.regs.b[15] * 0x100);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_006() {
        EO = FETCH();
        EO += (short) (FETCH() << 8);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_007() {
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_100() {
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[12] + I.regs.b[13] * 0x100 + FETCH());
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_101() {
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[14] + I.regs.b[15] * 0x100 + FETCH());
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_102() {
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + I.regs.b[12] + I.regs.b[13] * 0x100 + FETCH());
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_103() {
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + I.regs.b[14] + I.regs.b[15] * 0x100 + FETCH());
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_104() {
        EO = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + FETCH());
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_105() {
        EO = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + FETCH());
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_106() {
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + FETCH());
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_107() {
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + FETCH());
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_200() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[12] + I.regs.b[13] * 0x100 + E16);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_201() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[14] + I.regs.b[15] * 0x100 + E16);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_202() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + I.regs.b[12] + I.regs.b[13] * 0x100 + E16);
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_203() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + I.regs.b[14] + I.regs.b[15] * 0x100 + E16);
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_204() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + E16);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_205() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + E16);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

    int EA_206() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[10] + I.regs.b[11] * 0x100 + E16);
        EA = DefaultBase(2, I) + EO;
        return EA;
    }

    int EA_207() {
        E16 = FETCH();
        E16 += (short) (FETCH() << 8);
        EO = (short) (I.regs.b[6] + I.regs.b[7] * 0x100 + E16);
        EA = DefaultBase(3, I) + EO;
        return EA;
    }

//#endregion

//#region NecInstr

    void i_add_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        ADDB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_add_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        ADDW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_add_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        ADDB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_add_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        ADDW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_add_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        ADDB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_add_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        ADDW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_push_es() {
        PUSH(I.sregs[0]);
        CLKS(12, 8, 3);
    }

    void i_pop_es() {
        POP(/* ref */ I.sregs, 0);
        CLKS(12, 8, 5);
    }

    void i_or_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        ORB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_or_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        ORW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_or_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        ORB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_or_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        ORW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_or_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        ORB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_or_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        ORW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_push_cs() {
        PUSH(I.sregs[1]);
        CLKS(12, 8, 3);
    }

    void i_pre_nec() {
        int[] ModRM = new int[1], tmp = new int[1], tmp2 = new int[1];
        switch (FETCH() & 0xff) {
            case 0x10:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(3, 3, 4);
                tmp2[0] = I.regs.b[2] & 0x7;
                I.ZeroVal = ((tmp[0] & (1 << tmp2[0])) != 0) ? 1 : 0;
                I.CarryVal = I.OverVal = 0;
                break; /* Test */
            case 0x11:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(3, 3, 4);
                tmp2[0] = I.regs.b[2] & 0xf;
                I.ZeroVal = ((tmp[0] & (1 << tmp2[0])) != 0) ? 1 : 0;
                I.CarryVal = I.OverVal = 0;
                break; /* Test */
            case 0x12:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(5, 5, 4);
                tmp2[0] = I.regs.b[2] & 0x7;
                tmp[0] &= ~(1 << tmp2[0]);
                PutbackRMByte(ModRM[0], (byte) tmp[0]);
                break; /* Clr */
            case 0x13:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(5, 5, 4);
                tmp2[0] = I.regs.b[2] & 0xf;
                tmp[0] &= ~(1 << tmp2[0]);
                PutbackRMWord(ModRM[0], (short) tmp[0]);
                break; /* Clr */
            case 0x14:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(4, 4, 4);
                tmp2[0] = I.regs.b[2] & 0x7;
                tmp[0] |= (1 << tmp2[0]);
                PutbackRMByte(ModRM[0], (byte) tmp[0]);
                break; /* Set */
            case 0x15:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(4, 4, 4);
                tmp2[0] = I.regs.b[2] & 0xf;
                tmp[0] |= (1 << tmp2[0]);
                PutbackRMWord(ModRM[0], (short) tmp[0]);
                break; /* Set */
            case 0x16:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(4, 4, 4);
                tmp2[0] = I.regs.b[2] & 0x7;
                BIT_NOT(/* ref */ tmp, /* ref */ tmp2); PutbackRMByte(ModRM[0], (byte) tmp[0]);
                break; /* Not */
            case 0x17:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(4, 4, 4);
                tmp2[0] = I.regs.b[2] & 0xf;
                BIT_NOT(/* ref */ tmp, /* ref */ tmp2); PutbackRMWord(ModRM[0], (short) tmp[0]);
                break; /* Not */

            case 0x18:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(4, 4, 4);
                tmp2[0] = (FETCH()) & 0x7;
                I.ZeroVal = ((tmp[0] & (1 << tmp2[0])) != 0) ? 1 : 0;
                I.CarryVal = I.OverVal = 0;
                break; /* Test */
            case 0x19:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(4, 4, 4);
                tmp2[0] = (FETCH()) & 0xf;
                I.ZeroVal = ((tmp[0] & (1 << tmp2[0])) != 0) ? 1 : 0;
                I.CarryVal = I.OverVal = 0;
                break; /* Test */
            case 0x1a:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(6, 6, 4);
                tmp2[0] = (FETCH()) & 0x7;
                tmp[0] &= ~(1 << tmp2[0]);
                PutbackRMByte(ModRM[0], (byte) tmp[0]);
                break; /* Clr */
            case 0x1b:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(6, 6, 4);
                tmp2[0] = (FETCH()) & 0xf;
                tmp[0] &= ~(1 << tmp2[0]);
                PutbackRMWord(ModRM[0], (short) tmp[0]);
                break; /* Clr */
            case 0x1c:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(5, 5, 4);
                tmp2[0] = (FETCH()) & 0x7;
                tmp[0] |= (1 << tmp2[0]);
                PutbackRMByte(ModRM[0], (byte) tmp[0]);
                break; /* Set */
            case 0x1d:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(5, 5, 4);
                tmp2[0] = (FETCH()) & 0xf;
                tmp[0] |= (1 << tmp2[0]);
                PutbackRMWord(ModRM[0], (short) tmp[0]);
                break; /* Set */
            case 0x1e:
                BITOP_BYTE(/* ref */ ModRM, /* ref */ tmp); CLKS(5, 5, 4);
                tmp2[0] = (FETCH()) & 0x7;
                BIT_NOT(/* ref */ tmp, /* ref */ tmp2); PutbackRMByte(ModRM[0], (byte) tmp[0]);
                break; /* Not */
            case 0x1f:
                BITOP_WORD(/* ref */ ModRM, /* ref */ tmp); CLKS(5, 5, 4);
                tmp2[0] = (FETCH()) & 0xf;
                BIT_NOT(/* ref */ tmp, /* ref */ tmp2); PutbackRMWord(ModRM[0], (short) tmp[0]);
                break; /* Not */

            case 0x20:
                ADD4S(/* ref */ tmp, /* ref */ tmp2); CLKS(7, 7, 2);
                break;
            case 0x22:
                SUB4S(/* ref */ tmp, /* ref */ tmp2); CLKS(7, 7, 2);
                break;
            case 0x26:
                CMP4S(/* ref */ tmp, /* ref */ tmp2); CLKS(7, 7, 2);
                break;
            case 0x28:
                ModRM[0] = FETCH();
                tmp[0] = GetRMByte(ModRM[0]);
                tmp[0] <<= 4;
                tmp[0] |= I.regs.b[0] & 0xf;
                I.regs.b[0] = (byte) ((I.regs.b[0] & 0xf0) | ((tmp[0] >> 8) & 0xf));
                tmp[0] &= 0xff;
                PutbackRMByte(ModRM[0], (byte) tmp[0]);
                CLKM(ModRM[0], 13, 13, 9, 28, 28, 15);
                break;
            case 0x2a:
                ModRM[0] = FETCH();
                tmp[0] = GetRMByte(ModRM[0]);
                tmp2[0] = (I.regs.b[0] & 0xf) << 4;
                I.regs.b[0] = (byte) ((I.regs.b[0] & 0xf0) | (tmp[0] & 0xf));
                tmp[0] = tmp2[0] | (tmp[0] >> 4);
                PutbackRMByte(ModRM[0], (byte) tmp[0]);
                CLKM(ModRM[0], 17, 17, 13, 32, 32, 19);
                break;
            case 0x31:
                ModRM[0] = FETCH();
                ModRM[0] = 0;
                break;
            case 0x33:
                ModRM[0] = FETCH();
                ModRM[0] = 0;
                break;
            case 0x92:
                CLK(2);
                break; /* V25/35 FINT */
            case 0xe0:
                ModRM[0] = FETCH();
                ModRM[0] = 0;
                break;
            case 0xf0:
                ModRM[0] = FETCH();
                ModRM[0] = 0;
                break;
            case 0xff:
                ModRM[0] = FETCH();
                ModRM[0] = 0;
                break;
            default:
                break;
        }
    }

    void i_adc_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (byte) (CF() ? 1 : 0);
        ADDB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_adc_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (short) (CF() ? 1 : 0);
        ADDW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_adc_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (byte) (CF() ? 1 : 0);
        ADDB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_adc_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (short) (CF() ? 1 : 0);
        ADDW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_adc_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        src[0] += (byte) (CF() ? 1 : 0);
        ADDB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_adc_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        src[0] += (short) (CF() ? 1 : 0);
        ADDW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_push_ss() {
        PUSH(I.sregs[2]);
        CLKS(12, 8, 3);
    }

    void i_pop_ss() {
        POP(/* ref */ I.sregs, 2);
        CLKS(12, 8, 5);
        I.no_interrupt = 1;
    }

    void i_sbb_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (byte) (CF() ? 1 : 0);
        SUBB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_sbb_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (short) (CF() ? 1 : 0);
        SUBW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_sbb_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (byte) (CF() ? 1 : 0);
        SUBB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_sbb_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        src[0] += (short) (CF() ? 1 : 0);
        SUBW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_sbb_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        src[0] += (byte) (CF() ? 1 : 0);
        SUBB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_sbb_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        src[0] += (short) (CF() ? 1 : 0);
        SUBW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_push_ds() {
        PUSH(I.sregs[3]);
        CLKS(12, 8, 3);
    }

    void i_pop_ds() {
        POP(/* ref */ I.sregs, 3);
        CLKS(12, 8, 5);
    }

    void i_and_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        ANDB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_and_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        ANDW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_and_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        ANDB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_and_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        ANDW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_and_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        ANDB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_and_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        ANDW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_es() {
        seg_prefix = 1;
        prefix_base = I.sregs[0] << 4;
        CLK(2);
        nec_instruction[fetchop()].run();
        seg_prefix = 0;
    }

    void i_daa() {
        ADJ4(6, 0x60);
        CLKS(3, 3, 2);
    }

    void i_sub_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_sub_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_sub_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_sub_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_sub_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        SUBB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_sub_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        SUBW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_cs() {
        seg_prefix = 1;
        prefix_base = I.sregs[1] << 4;
        CLK(2);
        nec_instruction[fetchop()].run();
        seg_prefix = 0;
    }

    void i_das() {
        ADJ4(-6, -0x60);
        CLKS(3, 3, 2);
    }

    void i_xor_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        XORB(/* ref */ src, /* ref */ dst);
        PutbackRMByte(ModRM[0], dst[0]);
        CLKM(ModRM[0], 2, 2, 2, 16, 16, 7);
    }

    void i_xor_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        XORW(/* ref */ src, /* ref */ dst);
        PutbackRMWord(ModRM[0], dst[0]);
        CLKR(ModRM[0], 24, 24, 11, 24, 16, 7, 2, EA);
    }

    void i_xor_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        XORB(/* ref */ src, /* ref */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_xor_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        XORW(/* ref */ src, /* ref */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_xor_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        XORB(/* ref */ src, /* ref */ dst);
        I.regs.b[0] = dst[0];
        CLKS(4, 4, 2);
    }

    void i_xor_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        XORW(/* ref */ src, /* ref */ dst);
        //I.regs.w[0] = dst;
        I.regs.b[0] = (byte) (dst[0] % 0x100);
        I.regs.b[1] = (byte) (dst[0] / 0x100);
        CLKS(4, 4, 2);
    }

    void i_ss() {
        seg_prefix = 1;
        prefix_base = I.sregs[2] << 4;
        CLK(2);
        nec_instruction[fetchop()].run();
        seg_prefix = 0;
    }

    void i_aaa() {
        ADJB(6, ((I.regs.b[0] & 0xff) > 0xf9) ? 2 : 1);
        CLKS(7, 7, 4);
    }

    void i_cmp_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBB(/* ref */ src, /* ref */ dst);
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_cmp_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBW(/* ref */ src, /* ref */ dst);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_cmp_r8b() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_r8b(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBB(/* ref */ src, /* ref */ dst);
        CLKM(ModRM[0], 2, 2, 2, 11, 11, 6);
    }

    void i_cmp_r16w() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        SUBW(/* ref */ src, /* ref */ dst);
        CLKR(ModRM[0], 15, 15, 8, 15, 11, 6, 2, EA);
    }

    void i_cmp_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        SUBB(/* ref */ src, /* ref */ dst);
        CLKS(4, 4, 2);
    }

    void i_cmp_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        SUBW(/* ref */ src, /* ref */ dst);
        CLKS(4, 4, 2);
    }

    void i_ds() {
        seg_prefix = 1;
        prefix_base = I.sregs[3] << 4;
        CLK(2);
        nec_instruction[fetchop()].run();
        seg_prefix = 0;
    }

    void i_aas() {
        ADJB(-6, (I.regs.b[0] < 6) ? -2 : -1);
        CLKS(7, 7, 4);
    }

    void i_inc_ax() {
        IncWordReg(0);
        CLK(2);
    }

    void i_inc_cx() {
        IncWordReg(1);
        CLK(2);
    }

    void i_inc_dx() {
        IncWordReg(2);
        CLK(2);
    }

    void i_inc_bx() {
        IncWordReg(3);
        CLK(2);
    }

    void i_inc_sp() {
        IncWordReg(4);
        CLK(2);
    }

    void i_inc_bp() {
        IncWordReg(5);
        CLK(2);
    }

    void i_inc_si() {
        IncWordReg(6);
        CLK(2);
    }

    void i_inc_di() {
        IncWordReg(7);
        CLK(2);
    }

    void i_dec_ax() {
        DecWordReg(0);
        CLK(2);
    }

    void i_dec_cx() {
        DecWordReg(1);
        CLK(2);
    }

    void i_dec_dx() {
        DecWordReg(2);
        CLK(2);
    }

    void i_dec_bx() {
        DecWordReg(3);
        CLK(2);
    }

    void i_dec_sp() {
        DecWordReg(4);
        CLK(2);
    }

    void i_dec_bp() {
        DecWordReg(5);
        CLK(2);
    }

    void i_dec_si() {
        DecWordReg(6);
        CLK(2);
    }

    void i_dec_di() {
        DecWordReg(7);
        CLK(2);
    }

    void i_push_ax() {
        //PUSH(I.regs.w[0]);
        PUSH((short) (I.regs.b[0] + I.regs.b[1] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_cx() {
        //PUSH(I.regs.w[1]);
        PUSH((short) (I.regs.b[2] + I.regs.b[3] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_dx() {
        //PUSH(I.regs.w[2]);
        PUSH((short) (I.regs.b[4] + I.regs.b[5] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_bx() {
        //PUSH(I.regs.w[3]);
        PUSH((short) (I.regs.b[6] + I.regs.b[7] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_sp() {
        //PUSH(I.regs.w[4]);
        PUSH((short) (I.regs.b[8] + I.regs.b[9] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_bp() {
        //PUSH(I.regs.w[5]);
        PUSH((short) (I.regs.b[10] + I.regs.b[11] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_si() {
        //PUSH(I.regs.w[6]);
        PUSH((short) (I.regs.b[12] + I.regs.b[13] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_push_di() {
        //PUSH(I.regs.w[7]);
        PUSH((short) (I.regs.b[14] + I.regs.b[15] * 0x100));
        CLKS(12, 8, 3);
    }

    void i_pop_ax() {
        //POP(ref I.regs.w[0]);
        POPW(0);
        CLKS(12, 8, 5);
    }

    void i_pop_cx() {
        //POP(ref I.regs.w[1]);
        POPW(1);
        CLKS(12, 8, 5);
    }

    void i_pop_dx() {
        //POP(ref I.regs.w[2]);
        POPW(2);
        CLKS(12, 8, 5);
    }

    void i_pop_bx() {
        //POP(ref I.regs.w[3]);
        POPW(3);
        CLKS(12, 8, 5);
    }

    void i_pop_sp() {
        //POP(ref I.regs.w[4]);
        POPW(4);
        CLKS(12, 8, 5);
    }

    void i_pop_bp() {
        //POP(ref I.regs.w[5]);
        POPW(5);
        CLKS(12, 8, 5);
    }

    void i_pop_si() {
        //POP(ref I.regs.w[6]);
        POPW(6);
        CLKS(12, 8, 5);
    }

    void i_pop_di() {
        //POP(ref I.regs.w[7]);
        POPW(7);
        CLKS(12, 8, 5);
    }

    void i_pusha() {
        short tmp = (short) (I.regs.b[8] + I.regs.b[9] * 0x100);// I.regs.w[4];
            /*PUSH(I.regs.w[0]);
            PUSH(I.regs.w[1]);
            PUSH(I.regs.w[2]);
            PUSH(I.regs.w[3]);*/
        PUSH((short) (I.regs.b[0] + I.regs.b[1] * 0x100));
        PUSH((short) (I.regs.b[2] + I.regs.b[3] * 0x100));
        PUSH((short) (I.regs.b[4] + I.regs.b[5] * 0x100));
        PUSH((short) (I.regs.b[6] + I.regs.b[7] * 0x100));
        PUSH(tmp);
            /*PUSH(I.regs.w[5]);
            PUSH(I.regs.w[6]);
            PUSH(I.regs.w[7]);*/
        PUSH((short) (I.regs.b[10] + I.regs.b[11] * 0x100));
        PUSH((short) (I.regs.b[12] + I.regs.b[13] * 0x100));
        PUSH((short) (I.regs.b[14] + I.regs.b[15] * 0x100));
        CLKS(67, 35, 20);
    }

    void i_popa() {
        short[] tmp = new short[1];
            /*POP(ref I.regs.w[7]);
            POP(ref I.regs.w[6]);
            POP(ref I.regs.w[5]);*/
        POPW(7);
        POPW(6);
        POPW(5);
        POP(/* ref */ tmp, 0);
            /*POP(ref I.regs.w[3]);
            POP(ref I.regs.w[2]);
            POP(ref I.regs.w[1]);
            POP(ref I.regs.w[0]);*/
        POPW(3);
        POPW(2);
        POPW(1);
        POPW(0);
        CLKS(75, 43, 22);
    }

    void i_chkind() {
        int low, high, tmp;
        int ModRM;
        ModRM = GetModRM();
        low = GetRMWord(ModRM);
        high = GetnextRMWord();
        tmp = RegWord(ModRM);
        if (tmp < low || tmp > high) {
            nec_interrupt(5, false);
        }
        pendingCycles -= 20;
    }

    void i_brkn() {
        nec_interrupt(FETCH(), true);
        CLKS(50, 50, 24);
    }

    void i_repnc() {
        int next = fetchop();
        short c = (short) (I.regs.b[2] + I.regs.b[3] * 0x100);// I.regs.w[1];
        switch (next) { /* Segments */
            case 0x26:
                seg_prefix = 1;
                prefix_base = (I.sregs[0] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x2e:
                seg_prefix = 1;
                prefix_base = (I.sregs[1] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x36:
                seg_prefix = 1;
                prefix_base = (I.sregs[2] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x3e:
                seg_prefix = 1;
                prefix_base = (I.sregs[3] << 4);
                next = fetchop();
                CLK(2);
                break;
        }
        switch (next & 0xff) {
            case 0x6c:
                CLK(2);
                if (c != 0) do {
                    i_insb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100); /*I.regs.w[1] = c;*/
                break;
            case 0x6d:
                CLK(2);
                if (c != 0) do {
                    i_insw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6e:
                CLK(2);
                if (c != 0) do {
                    i_outsb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6f:
                CLK(2);
                if (c != 0) do {
                    i_outsw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa4:
                CLK(2);
                if (c != 0) do {
                    i_movsb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa5:
                CLK(2);
                if (c != 0) do {
                    i_movsw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa6:
                CLK(2);
                if (c != 0) do {
                    i_cmpsb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa7:
                CLK(2);
                if (c != 0) do {
                    i_cmpsw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaa:
                CLK(2);
                if (c != 0) do {
                    i_stosb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xab:
                CLK(2);
                if (c != 0) do {
                    i_stosw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xac:
                CLK(2);
                if (c != 0) do {
                    i_lodsb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xad:
                CLK(2);
                if (c != 0) do {
                    i_lodsw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xae:
                CLK(2);
                if (c != 0) do {
                    i_scasb();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaf:
                CLK(2);
                if (c != 0) do {
                    i_scasw();
                    c--;
                } while (c > 0 && !CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            default:
                nec_instruction[next].run(); break;
        }
        seg_prefix = 0;
    }

    void i_repc() {
        int next = fetchop();
        short c = (short) (I.regs.b[2] + I.regs.b[3] * 0x100);// I.regs.w[1];
        switch (next) { /* Segments */
            case 0x26:
                seg_prefix = 1;
                prefix_base = (I.sregs[0] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x2e:
                seg_prefix = 1;
                prefix_base = (I.sregs[1] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x36:
                seg_prefix = 1;
                prefix_base = (I.sregs[2] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x3e:
                seg_prefix = 1;
                prefix_base = (I.sregs[3] << 4);
                next = fetchop();
                CLK(2);
                break;
        }
        switch (next & 0xff) {
            case 0x6c:
                CLK(2);
                if (c != 0) do {
                    i_insb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);/*I.regs.w[1] = c;*/
                break;
            case 0x6d:
                CLK(2);
                if (c != 0) do {
                    i_insw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6e:
                CLK(2);
                if (c != 0) do {
                    i_outsb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6f:
                CLK(2);
                if (c != 0) do {
                    i_outsw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa4:
                CLK(2);
                if (c != 0) do {
                    i_movsb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa5:
                CLK(2);
                if (c != 0) do {
                    i_movsw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa6:
                CLK(2);
                if (c != 0) do {
                    i_cmpsb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa7:
                CLK(2);
                if (c != 0) do {
                    i_cmpsw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaa:
                CLK(2);
                if (c != 0) do {
                    i_stosb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xab:
                CLK(2);
                if (c != 0) do {
                    i_stosw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xac:
                CLK(2);
                if (c != 0) do {
                    i_lodsb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xad:
                CLK(2);
                if (c != 0) do {
                    i_lodsw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xae:
                CLK(2);
                if (c != 0) do {
                    i_scasb();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaf:
                CLK(2);
                if (c != 0) do {
                    i_scasw();
                    c--;
                } while (c > 0 && CF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            default:
                nec_instruction[next].run(); break;
        }
        seg_prefix = 0;
    }

    void i_push_d16() {
        int tmp;
        tmp = FETCHWORD();
        PUSH((short) tmp);
        //CLKW(12, 12, 5, 12, 8, 5, I.regs.w[4]);
        CLKW(12, 12, 5, 12, 8, 5, I.regs.b[8] + I.regs.b[9] * 0x100);
    }

    void i_imul_d16() {
        int tmp;
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        tmp = FETCHWORD();
        dst[0] = (short) ((int) src[0] * (int) ((short) tmp));
        I.CarryVal = I.OverVal = ((((int) dst[0]) >> 15 != 0) && (((int) dst[0]) >> 15 != -1)) ? 1 : 0;
        //I.regs.w[mod_RM.regw[ModRM]] = (ushort)dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        pendingCycles -= (ModRM[0] >= 0xc0) ? 38 : 47;
    }

    void i_push_d8() {
        int tmp = (short) FETCH();
        PUSH((short) tmp);
        //CLKW(11, 11, 5, 11, 7, 3, I.regs.w[4]);
        CLKW(11, 11, 5, 11, 7, 3, I.regs.b[8] + I.regs.b[9] * 0x100);
    }

    void i_imul_d8() {
        int src2;
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_r16w(/* out */ ModRM, /* out */ src, /* out */ dst);
        src2 = (short) FETCH();
        dst[0] = (short) ((int) src[0] * (int) ((short) src2));
        I.CarryVal = I.OverVal = ((((int) dst[0]) >> 15 != 0) && (((int) dst[0]) >> 15 != -1)) ? 1 : 0;
        //I.regs.w[mod_RM.regw[ModRM]] = (ushort)dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        pendingCycles -= (ModRM[0] >= 0xc0) ? 31 : 39;
    }

    void i_insb() {
        //PutMemB(0, I.regs.w[7], ReadIOByte(I.regs.w[2]));
        PutMemB(0, I.regs.b[14] + I.regs.b[15] * 0x100, ReadIOByte.apply(I.regs.b[4] + I.regs.b[5] * 0x100));
        //I.regs.w[7] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100);
        w7 += (short) (-2 * (I.DF ? 1 : 0) + 1);
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        CLK(8);
    }

    void i_insw() {
        //PutMemW(0, I.regs.w[7], ReadIOWord(I.regs.w[2]));
        PutMemW(0, I.regs.b[14] + I.regs.b[15] * 0x100, ReadIOWord.apply(I.regs.b[4] + I.regs.b[5] * 0x100));
        //I.regs.w[7] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100);
        w7 += (short) (-4 * (I.DF ? 1 : 0) + 2);
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        CLKS(18, 10, 8);
    }

    void i_outsb() {
        //WriteIOByte(I.regs.w[2], GetMemB(3, I.regs.w[6]));
        WriteIOByte.accept(I.regs.b[4] + I.regs.b[5] * 0x100, GetMemB(3, I.regs.b[12] + I.regs.b[13] * 0x100));
        //I.regs.w[6] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100);
        w6 += (short) (-2 * (I.DF ? 1 : 0) + 1);
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLK(8);
    }

    void i_outsw() {
        //WriteIOWord(I.regs.w[2], GetMemW(3, I.regs.w[6]));
        WriteIOWord.accept(I.regs.b[4] + I.regs.b[5] * 0x100, GetMemW(3, I.regs.b[12] + I.regs.b[13] * 0x100));
        //I.regs.w[6] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100);
        w6 += (short) (-4 * (I.DF ? 1 : 0) + 2);
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKS(18, 10, 8);
    }

    void i_jo() {
        boolean b1 = OF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jno() {
        boolean b1 = !OF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jc() {
        boolean b1 = CF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jnc() {
        boolean b1 = !CF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jz() {
        boolean b1 = ZF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jnz() {
        boolean b1 = !ZF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jce() {
        boolean b1 = CF() || ZF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jnce() {
        boolean b1 = !(CF() || ZF());
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_js() {
        boolean b1 = SF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jns() {
        boolean b1 = !SF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jp() {
        boolean b1 = PF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jnp() {
        boolean b1 = !PF();
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jl() {
        boolean b1 = (SF() != OF()) && (!ZF());
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jnl() {
        boolean b1 = (ZF()) || (SF() == OF());
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jle() {
        boolean b1 = (ZF()) || (SF() != OF());
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_jnle() {
        boolean b1 = (SF() == OF()) && (!ZF());
        JMP(b1);
        if (!b1) {
            CLKS(4, 4, 3);
        }
    }

    void i_80pre() {
        int ModRM;
        byte[] src = new byte[1], dst = new byte[1];
        ModRM = GetModRM();
        dst[0] = GetRMByte(ModRM);
        src[0] = FETCH();
        if (ModRM >= 0xc0) {
            CLKS(4, 4, 2);
        } else if ((ModRM & 0x38) == 0x38) {
            CLKS(13, 13, 6);
        } else {
            CLKS(18, 18, 7);
        }
        switch (ModRM & 0x38) {
            case 0x00:
                ADDB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x08:
                ORB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x10:
                src[0] += (byte) (CF() ? 1 : 0);
                ADDB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x18:
                src[0] += (byte) (CF() ? 1 : 0);
                SUBB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x20:
                ANDB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x28:
                SUBB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x30:
                XORB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x38:
                SUBB(/* ref */ src, /* ref */ dst); break;
        }
    }

    void i_81pre() {
        int ModRM;
        short[] src = new short[1], dst = new short[1];
        ModRM = GetModRM();
        dst[0] = GetRMWord(ModRM);
        src[0] = FETCH();
        src[0] += (short) (FETCH() << 8);
        if (ModRM >= 0xc0) {
            CLKS(4, 4, 2);
        } else if ((ModRM & 0x38) == 0x38) {
            CLKW(17, 17, 8, 17, 13, 6, EA);
        } else {
            CLKW(26, 26, 11, 26, 18, 7, EA);
        }
        switch (ModRM & 0x38) {
            case 0x00:
                ADDW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x08:
                ORW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x10:
                src[0] += (short) (CF() ? 1 : 0);
                ADDW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x18:
                src[0] += (short) (CF() ? 1 : 0);
                SUBW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x20:
                ANDW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x28:
                SUBW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x30:
                XORW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x38:
                SUBW(/* ref */ src, /* ref */ dst); break;
        }
    }

    void i_82pre() {
        int ModRM;
        byte[] src = new byte[1], dst = new byte[1];
        ModRM = GetModRM();
        dst[0] = GetRMByte(ModRM);
        src[0] = FETCH();
        if (ModRM >= 0xc0) {
            CLKS(4, 4, 2);
        } else if ((ModRM & 0x38) == 0x38) {
            CLKS(13, 13, 6);
        } else {
            CLKS(18, 18, 7);
        }
        switch (ModRM & 0x38) {
            case 0x00:
                ADDB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x08:
                ORB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x10:
                src[0] += (byte) (CF() ? 1 : 0);
                ADDB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x18:
                src[0] += (byte) (CF() ? 1 : 0);
                SUBB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x20:
                ANDB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x28:
                SUBB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x30:
                XORB(/* ref */ src, /* ref */ dst); PutbackRMByte(ModRM, dst[0]);
                break;
            case 0x38:
                SUBB(/* ref */ src, /* ref */ dst); break;
        }
    }

    void i_83pre() {
        int ModRM;
        short[] src = new short[1], dst = new short[1];
        ModRM = GetModRM();
        dst[0] = GetRMWord(ModRM);
        src[0] = FETCH();
        if (ModRM >= 0xc0) {
            CLKS(4, 4, 2);
        } else if ((ModRM & 0x38) == 0x38) {
            CLKW(17, 17, 8, 17, 13, 6, EA);
        } else {
            CLKW(26, 26, 11, 26, 18, 7, EA);
        }
        switch (ModRM & 0x38) {
            case 0x00:
                ADDW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x08:
                ORW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x10:
                src[0] += (short) (CF() ? 1 : 0);
                ADDW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x18:
                src[0] += (short) (CF() ? 1 : 0);
                SUBW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x20:
                ANDW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x28:
                SUBW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x30:
                XORW(/* ref */ src, /* ref */ dst); PutbackRMWord(ModRM, dst[0]);
                break;
            case 0x38:
                SUBW(/* ref */ src, /* ref */ dst); break;
        }
    }

    void i_test_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        ANDB(/* ref */ src, /* ref */ dst);
        CLKM(ModRM[0], 2, 2, 2, 10, 10, 6);
    }

    void i_test_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        ANDW(/* ref */ src, /* ref */ dst);
        CLKR(ModRM[0], 14, 14, 8, 14, 10, 6, 2, EA);
    }

    void i_xchg_br8() {
        int[] ModRM = new int[1];
        byte[] src = new byte[1], dst = new byte[1];
        DEF_br8(/* out */ ModRM, /* out */ src, /* out */ dst);
        I.regs.b[mod_RM.regb[ModRM[0]]] = dst[0];
        PutbackRMByte(ModRM[0], src[0]);
        CLKM(ModRM[0], 3, 3, 3, 16, 18, 8);
    }

    void i_xchg_wr16() {
        int[] ModRM = new int[1];
        short[] src = new short[1], dst = new short[1];
        DEF_wr16(/* out */ ModRM, /* out */ src, /* out */ dst);
        //I.regs.w[mod_RM.regw[ModRM]] = dst;
        I.regs.b[mod_RM.regw[ModRM[0]] * 2] = (byte) (dst[0] % 0x100);
        I.regs.b[mod_RM.regw[ModRM[0]] * 2 + 1] = (byte) (dst[0] / 0x100);
        PutbackRMWord(ModRM[0], src[0]);
        CLKR(ModRM[0], 24, 24, 12, 24, 16, 8, 3, EA);
    }

    void i_mov_br8() {
        int ModRM;
        byte src;
        ModRM = GetModRM();
        src = I.regs.b[mod_RM.regb[ModRM]];
        PutRMByte(ModRM, src);
        CLKM(ModRM, 2, 2, 2, 9, 9, 3);
    }

    void i_mov_wr16() {
        int ModRM;
        short src;
        ModRM = GetModRM();
        //src = I.regs.w[mod_RM.regw[ModRM]];
        src = (short) (I.regs.b[mod_RM.regw[ModRM] * 2] + I.regs.b[mod_RM.regw[ModRM] * 2 + 1] * 0x100);
        PutRMWord(ModRM, src);
        CLKR(ModRM, 13, 13, 5, 13, 9, 3, 2, EA);
    }

    void i_mov_r8b() {
        int ModRM;
        byte src;
        ModRM = GetModRM();
        src = GetRMByte(ModRM);
        I.regs.b[mod_RM.regb[ModRM]] = src;
        CLKM(ModRM, 2, 2, 2, 11, 11, 5);
    }

    void i_mov_r16w() {
        int ModRM;
        short src;
        ModRM = GetModRM();
        src = GetRMWord(ModRM);
        //I.regs.w[mod_RM.regw[ModRM]] = src;
        I.regs.b[mod_RM.regw[ModRM] * 2] = (byte) (src % 0x100);
        I.regs.b[mod_RM.regw[ModRM] * 2 + 1] = (byte) (src / 0x100);
        CLKR(ModRM, 15, 15, 7, 15, 11, 5, 2, EA);
    }

    void i_mov_wsreg() {
        int ModRM;
        ModRM = GetModRM();
        PutRMWord(ModRM, I.sregs[(ModRM & 0x38) >> 3]);
        CLKR(ModRM, 14, 14, 5, 14, 10, 3, 2, EA);
    }

    void i_lea() {
        int ModRM = FETCH();
        GetEA[ModRM].get();
        //I.regs.w[mod_RM.regw[ModRM]] = EO;
        I.regs.b[mod_RM.regw[ModRM] * 2] = (byte) (EO % 0x100);
        I.regs.b[mod_RM.regw[ModRM] * 2 + 1] = (byte) (EO / 0x100);
        CLKS(4, 4, 2);
    }

    void i_mov_sregw() {
        int ModRM;
        short src;
        ModRM = GetModRM();
        src = GetRMWord(ModRM);
        CLKR(ModRM, 15, 15, 7, 15, 11, 5, 2, EA);
        switch (ModRM & 0x38) {
            case 0x00:
                I.sregs[0] = src;
                break; /* mov es,ew */
            case 0x08:
                I.sregs[1] = src;
                break; /* mov cs,ew */
            case 0x10:
                I.sregs[2] = src;
                break; /* mov ss,ew */
            case 0x18:
                I.sregs[3] = src;
                break; /* mov ds,ew */
            default:
                break;
        }
        I.no_interrupt = 1;
    }

    void i_popw() {
        int ModRM;
        short[] tmp = new short[1];
        ModRM = GetModRM();
        POP(/* ref */ tmp, 0);
        PutRMWord(ModRM, tmp[0]);
        pendingCycles -= 21;
    }

    void i_nop() {
        CLK(3);
        if (I.no_interrupt == 0 && pendingCycles > 0 && (I.pending_irq == 0) && (PEEKOP((I.sregs[1] << 4) + I.ip)) == (byte) 0xeb && (PEEK((I.sregs[1] << 4) + I.ip + 1)) == (byte) 0xfd)
            pendingCycles %= 15;
    }

    void i_xchg_axcx() {
        XchgAWReg(1);
        CLK(3);
    }

    void i_xchg_axdx() {
        XchgAWReg(2);
        CLK(3);
    }

    void i_xchg_axbx() {
        XchgAWReg(3);
        CLK(3);
    }

    void i_xchg_axsp() {
        XchgAWReg(4);
        CLK(3);
    }

    void i_xchg_axbp() {
        XchgAWReg(5);
        CLK(3);
    }

    void i_xchg_axsi() {
        XchgAWReg(6);
        CLK(3);
    }

    void i_xchg_axdi() {
        XchgAWReg(7);
        CLK(3);
    }

    void i_cbw() {
        I.regs.b[1] = (byte) (((I.regs.b[0] & 0x80) != 0) ? 0xff : 0);
        CLK(2);
    }

    void i_cwd() {
        //I.regs.w[2] = (ushort)(((I.regs.b[1] & 0x80) != 0) ? 0xffff : 0);
        short w2 = (short) (((I.regs.b[1] & 0x80) != 0) ? 0xffff : 0);
        I.regs.b[4] = (byte) (w2 % 0x100);
        I.regs.b[5] = (byte) (w2 / 0x100);
        CLK(4);
    }

    void i_call_far() {
        short tmp, tmp2;
        tmp = FETCHWORD();
        tmp2 = FETCHWORD();
        PUSH(I.sregs[1]);
        PUSH(I.ip);
        I.ip = tmp;
        I.sregs[1] = tmp2;
        //CHANGE_PC;
        CLKW(29, 29, 13, 29, 21, 9, I.regs.b[8] + I.regs.b[9] * 0x100);
    }

    void i_wait() {
        if (!I.poll_state) {
            I.ip--;
        }
        CLK(5);
    }

    void i_pushf() {
        short tmp = CompressFlags();
        PUSH(tmp);
        CLKS(12, 8, 3);
    }

    void i_popf() {
        short[] tmp = new short[1];
        POP(/* ref */ tmp, 0);
        ExpandFlags(tmp[0]);
        CLKS(12, 8, 5);
        if (I.TF) {
            nec_trap();
        }
    }

    void i_sahf() {
        short tmp = (short) ((CompressFlags() & 0xff00) | (I.regs.b[1] & 0xd5));
        ExpandFlags(tmp);
        CLKS(3, 3, 2);
    }

    void i_lahf() {
        I.regs.b[1] = (byte) (CompressFlags() & 0xff);
        CLKS(3, 3, 2);
    }

    void i_mov_aldisp() {
        short addr;
        addr = FETCHWORD();
        I.regs.b[0] = GetMemB(3, addr);
        CLKS(10, 10, 5);
    }

    void i_mov_axdisp() {
        short addr;
        addr = FETCHWORD();
        //I.regs.w[0] = GetMemW(3, addr);
        short w0 = GetMemW(3, addr);
        I.regs.b[0] = (byte) (w0 % 0x100);
        I.regs.b[1] = (byte) (w0 / 0x100);
        CLKW(14, 14, 7, 14, 10, 5, addr);
    }

    void i_mov_dispal() {
        short addr;
        addr = FETCHWORD();
        PutMemB(3, addr, I.regs.b[0]);
        CLKS(9, 9, 3);
    }

    void i_mov_dispax() {
        short addr;
        addr = FETCHWORD();
        PutMemW(3, addr, (short) (I.regs.b[0] + I.regs.b[1] * 0x100));
        CLKW(13, 13, 5, 13, 9, 3, addr);
    }

    void i_movsb() {
        byte tmp = GetMemB(3, I.regs.b[12] + I.regs.b[13] * 0x100);
        PutMemB(0, I.regs.b[14] + I.regs.b[15] * 0x100, tmp);
        //I.regs.w[7] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        //I.regs.w[6] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKS(8, 8, 6);
    }

    void i_movsw() {
        short tmp = GetMemW(3, I.regs.b[12] + I.regs.b[13] * 0x100);
        PutMemW(0, I.regs.b[14] + I.regs.b[15] * 0x100, tmp);
        //I.regs.w[7] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        //I.regs.w[6] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKS(16, 16, 10);
    }

    void i_cmpsb() {
        byte[] src = new byte[GetMemB(0, I.regs.b[14] + I.regs.b[15] * 0x100)];
        byte[] dst = new byte[GetMemB(3, I.regs.b[12] + I.regs.b[13] * 0x100)];
        SUBB(/* ref */ src, /* ref */ dst);
        //I.regs.w[7] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        //I.regs.w[6] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKS(14, 14, 14);
    }

    void i_cmpsw() {
        short[] src = new short[GetMemW(0, I.regs.b[14] + I.regs.b[15] * 0x100)];
        short[] dst = new short[GetMemW(3, I.regs.b[12] + I.regs.b[13] * 0x100)];
        SUBW(/* ref */ src, /* ref */ dst);
        //I.regs.w[7] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        //I.regs.w[6] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKS(14, 14, 14);
    }

    void i_test_ald8() {
        byte[] src = new byte[1], dst = new byte[1];
        DEF_ald8(/* out */ src, /* out */ dst);
        ANDB(/* ref */ src, /* ref */ dst);
        CLKS(4, 4, 2);
    }

    void i_test_axd16() {
        short[] src = new short[1], dst = new short[1];
        DEF_axd16(/* out */ src, /* out */ dst);
        ANDW(/* ref */ src, /* ref */ dst);
        CLKS(4, 4, 2);
    }

    void i_stosb() {
        PutMemB(0, I.regs.b[14] + I.regs.b[15] * 0x100, I.regs.b[0]);
        //I.regs.w[7] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        CLKS(4, 4, 3);
    }

    void i_stosw() {
        PutMemW(0, I.regs.b[14] + I.regs.b[15] * 0x100, (short) (I.regs.b[0] + I.regs.b[1] * 0x100));
        //I.regs.w[7] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        CLKW(8, 8, 5, 8, 4, 3, I.regs.b[14] + I.regs.b[15] * 0x100);
    }

    void i_lodsb() {
        I.regs.b[0] = GetMemB(3, I.regs.b[12] + I.regs.b[13] * 0x100);
        //I.regs.w[6] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKS(4, 4, 3);
    }

    void i_lodsw() {
        short w0 = GetMemW(3, I.regs.b[12] + I.regs.b[13] * 0x100);
        I.regs.b[0] = (byte) (w0 % 0x100);
        I.regs.b[1] = (byte) (w0 / 0x100);
        //I.regs.w[0] = GetMemW(3, I.regs.b[12] + I.regs.b[13] * 0x100);
        //I.regs.w[6] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w6 = (short) (I.regs.b[12] + I.regs.b[13] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[12] = (byte) (w6 % 0x100);
        I.regs.b[13] = (byte) (w6 / 0x100);
        CLKW(8, 8, 5, 8, 4, 3, I.regs.b[12] + I.regs.b[13] * 0x100);
    }

    void i_scasb() {
        byte[] src = new byte[GetMemB(0, I.regs.b[14] + I.regs.b[15] * 0x100)];
        byte[] dst = new byte[I.regs.b[0]];
        SUBB(/* ref */ src, /* ref */ dst);
        //I.regs.w[7] += (ushort)(-2 * (I.DF ? 1 : 0) + 1);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-2 * (I.DF ? 1 : 0) + 1));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        CLKS(4, 4, 3);
    }

    void i_scasw() {
        short[] src = new short[GetMemW(0, I.regs.b[14] + I.regs.b[15] * 0x100)];
        short[] dst = new short[(short) (I.regs.b[0] + I.regs.b[1] * 0x100)];
        SUBW(/* ref */ src, /* ref */ dst);
        //I.regs.w[7] += (ushort)(-4 * (I.DF ? 1 : 0) + 2);
        short w7 = (short) (I.regs.b[14] + I.regs.b[15] * 0x100 + (-4 * (I.DF ? 1 : 0) + 2));
        I.regs.b[14] = (byte) (w7 % 0x100);
        I.regs.b[15] = (byte) (w7 / 0x100);
        CLKW(8, 8, 5, 8, 4, 3, I.regs.b[14] + I.regs.b[15] * 0x100);
    }

    void i_mov_ald8() {
        I.regs.b[0] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_cld8() {
        I.regs.b[2] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_dld8() {
        I.regs.b[4] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_bld8() {
        I.regs.b[6] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_ahd8() {
        I.regs.b[1] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_chd8() {
        I.regs.b[3] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_dhd8() {
        I.regs.b[5] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_bhd8() {
        I.regs.b[7] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_axd16() {
        I.regs.b[0] = FETCH();
        I.regs.b[1] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_cxd16() {
        I.regs.b[2] = FETCH();
        I.regs.b[3] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_dxd16() {
        I.regs.b[4] = FETCH();
        I.regs.b[5] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_bxd16() {
        I.regs.b[6] = FETCH();
        I.regs.b[7] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_spd16() {
        I.regs.b[8] = FETCH();
        I.regs.b[9] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_bpd16() {
        I.regs.b[10] = FETCH();
        I.regs.b[11] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_sid16() {
        I.regs.b[12] = FETCH();
        I.regs.b[13] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_mov_did16() {
        I.regs.b[14] = FETCH();
        I.regs.b[15] = FETCH();
        CLKS(4, 4, 2);
    }

    void i_rotshft_bd8() {
        int ModRM;
        int src;
        int[] dst = new int[1];
        byte c;
        ModRM = GetModRM();
        src = GetRMByte(ModRM);
        dst[0] = src;
        c = FETCH();
        CLKM(ModRM, 7, 7, 2, 19, 19, 6);
        if (c != 0) {
            switch (ModRM & 0x38) {
                case 0x00:
                    do {
                        ROL_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x08:
                    do {
                        ROR_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x10:
                    do {
                        ROLC_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x18:
                    do {
                        RORC_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x20:
                    SHL_BYTE(c, /* ref */ dst, ModRM); break;
                case 0x28:
                    SHR_BYTE(c, /* ref */ dst, ModRM); break;
                case 0x30:
                    break;
                case 0x38:
                    SHRA_BYTE(c, /* ref */ dst, ModRM); break;
            }
        }
    }

    void i_rotshft_wd8() {
        int ModRM;
        int src;
        int[] dst = new int[1];
        byte c;
        ModRM = GetModRM();
        src = GetRMWord(ModRM);
        dst[0] = src;
        c = FETCH();
        CLKM(ModRM, 7, 7, 2, 27, 19, 6);
        if (c != 0) {
            switch (ModRM & 0x38) {
                case 0x00:
                    do {
                        ROL_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x08:
                    do {
                        ROR_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x10:
                    do {
                        ROLC_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x18:
                    do {
                        RORC_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x20:
                    SHL_WORD(c, /* ref */ dst, ModRM); break;
                case 0x28:
                    SHR_WORD(c, /* ref */ dst, ModRM); break;
                case 0x30:
                    break;
                case 0x38:
                    SHRA_WORD(c, /* ref */ dst, ModRM); break;
            }
        }
    }

    void i_ret_d16() {
        short count = (short) (FETCH() & 0xff);
        count += (short) ((FETCH() & 0xff) << 8);
        short[] tmp = new short[1];
        POP(/* ref */ tmp, 0);
        I.ip = tmp[0];
        //I.regs.w[4] += count;
        short w4 = (short) (I.regs.b[8] + I.regs.b[9] * 0x100 + count);
        I.regs.b[8] = (byte) (w4 % 0x100);
        I.regs.b[9] = (byte) (w4 / 0x100);
        //CHANGE_PC;
        CLKS(24, 24, 10);
    }

    void i_ret() {
        short[] tmp = new short[1];
        POP(/* ref */ tmp, 0);
        I.ip = tmp[0];
        //CHANGE_PC;
        CLKS(19, 19, 10);
    }

    void i_les_dw() {
        int ModRM;
        ModRM = GetModRM();
        short tmp = GetRMWord(ModRM);
        //I.regs.w[mod_RM.regw[ModRM]] = tmp;
        I.regs.b[mod_RM.regw[ModRM] * 2] = (byte) (tmp % 0x100);
        I.regs.b[mod_RM.regw[ModRM] * 2 + 1] = (byte) (tmp / 0x100);
        I.sregs[0] = GetnextRMWord();
        CLKW(26, 26, 14, 26, 18, 10, EA);
    }

    void i_lds_dw() {
        int ModRM;
        ModRM = GetModRM();
        short tmp = GetRMWord(ModRM);
        //I.regs.w[mod_RM.regw[ModRM]] = tmp;
        I.regs.b[mod_RM.regw[ModRM] * 2] = (byte) (tmp % 0x100);
        I.regs.b[mod_RM.regw[ModRM] * 2 + 1] = (byte) (tmp / 0x100);
        I.sregs[3] = GetnextRMWord();
        CLKW(26, 26, 14, 26, 18, 10, EA);
    }

    void i_mov_bd8() {
        int ModRM;
        ModRM = GetModRM();
        PutImmRMByte(ModRM);
        pendingCycles -= (ModRM >= 0xc0) ? 4 : 11;
    }

    void i_mov_wd16() {
        int ModRM;
        ModRM = GetModRM();
        PutImmRMWord(ModRM);
        pendingCycles -= (ModRM >= 0xc0) ? 4 : 15;
    }

    void i_enter() {
        short nb = (short) (FETCH() & 0xff);
        int i, level;
        pendingCycles -= 23;
        nb += (short) ((FETCH() & 0xff) << 8);
        level = FETCH();
        PUSH((short) (I.regs.b[10] + I.regs.b[11] * 0x100));
        //I.regs.w[5] = I.regs.w[4];
        I.regs.b[10] = I.regs.b[8];
        I.regs.b[11] = I.regs.b[9];
        //I.regs.w[4] -= nb;
        short w4 = (short) (I.regs.b[8] + I.regs.b[9] * 0x100 - nb);
        I.regs.b[8] = (byte) (w4 % 0x100);
        I.regs.b[9] = (byte) (w4 / 0x100);
        for (i = 1; i < level; i++) {
            PUSH(GetMemW(2, I.regs.b[10] + I.regs.b[11] * 0x100 - i * 2));
            pendingCycles -= 16;
        }
        if (level != 0) {
            PUSH((short) (I.regs.b[10] + I.regs.b[11] * 0x100));
        }
    }

    void i_leave() {
        //I.regs.w[4] = I.regs.w[5];
        I.regs.b[8] = I.regs.b[10];
        I.regs.b[9] = I.regs.b[11];
        //POP(ref I.regs.w[5]);
        POPW(5);
        pendingCycles -= 8;
    }

    void i_retf_d16() {
        short count = (short) (FETCH() & 0xff);
        count += (short) ((FETCH() & 0xff) << 8);
        short[] tmp = new short[1];
        POP(/* ref */ tmp, 0);
        I.ip = tmp[0];
        POP(/* ref */ I.sregs, 1);
        //I.regs.w[4] += count;
        short w4 = (short) (I.regs.b[8] + I.regs.b[9] * 0x100 + count);
        I.regs.b[8] = (byte) (w4 % 0x100);
        I.regs.b[9] = (byte) (w4 / 0x100);
        //CHANGE_PC;
        CLKS(32, 32, 16);
    }

    void i_retf() {
        short[] tmp = new short[1];
        POP(/* ref */ tmp, 0);
        I.ip = tmp[0];
        POP(/* ref */ I.sregs, 1);
        //CHANGE_PC;
        CLKS(29, 29, 16);
    }

    void i_int3() {
        nec_interrupt(3, false);
        CLKS(50, 50, 24);
    }

    void i_int() {
        nec_interrupt(FETCH(), false);
        CLKS(50, 50, 24);
    }

    void i_into() {
        if (OF()) {
            nec_interrupt(4, false);
            CLKS(52, 52, 26);
        } else {
            CLK(3);
        }
    }

    void i_iret() {
        short[] tmp = new short[1];
        POP(/* ref */ tmp, 0);
        I.ip = tmp[0];
        POP(/* ref */ I.sregs, 1);
        i_popf();
        I.MF = true;
        //CHANGE_PC;
        CLKS(39, 39, 19);
    }

    void i_rotshft_b() {
        int ModRM;
        int src;
        int[] dst = new int[1];
        ModRM = GetModRM();
        src = GetRMByte(ModRM);
        dst[0] = src;
        CLKM(ModRM, 6, 6, 2, 16, 16, 7);
        switch (ModRM & 0x38) {
            case 0x00:
                ROL_BYTE(/* ref */ dst); PutbackRMByte(ModRM, (byte) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x80;
                break;
            case 0x08:
                ROR_BYTE(/* ref */ dst); PutbackRMByte(ModRM, (byte) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x80;
                break;
            case 0x10:
                ROLC_BYTE(/* ref */ dst); PutbackRMByte(ModRM, (byte) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x80;
                break;
            case 0x18:
                RORC_BYTE(/* ref */ dst); PutbackRMByte(ModRM, (byte) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x80;
                break;
            case 0x20:
                SHL_BYTE(1, /* ref */ dst, ModRM); I.OverVal = (src ^ dst[0]) & 0x80;
                break;
            case 0x28:
                SHR_BYTE(1, /* ref */ dst, ModRM); I.OverVal = (src ^ dst[0]) & 0x80;
                break;
            case 0x30:
                break;
            case 0x38:
                SHRA_BYTE(1, /* ref */ dst, ModRM); I.OverVal = 0;
                break;
        }
    }

    void i_rotshft_w() {
        int ModRM;
        int src;
        int[] dst = new int[1];
        ModRM = GetModRM();
        src = GetRMWord(ModRM);
        dst[0] = src;
        CLKM(ModRM, 6, 6, 2, 24, 16, 7);
        switch (ModRM & 0x38) {
            case 0x00:
                ROL_WORD(/* ref */ dst); PutbackRMWord(ModRM, (short) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x8000;
                break;
            case 0x08:
                ROR_WORD(/* ref */ dst); PutbackRMWord(ModRM, (short) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x8000;
                break;
            case 0x10:
                ROLC_WORD(/* ref */ dst); PutbackRMWord(ModRM, (short) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x8000;
                break;
            case 0x18:
                RORC_WORD(/* ref */ dst); PutbackRMWord(ModRM, (short) dst[0]);
                I.OverVal = (src ^ dst[0]) & 0x8000;
                break;
            case 0x20:
                SHL_WORD(1, /* ref */ dst, ModRM); I.OverVal = (src ^ dst[0]) & 0x8000;
                break;
            case 0x28:
                SHR_WORD(1, /* ref */ dst, ModRM); I.OverVal = (src ^ dst[0]) & 0x8000;
                break;
            case 0x30:
                break;
            case 0x38:
                SHRA_WORD(1, /* ref */ dst, ModRM); I.OverVal = 0;
                break;
        }
    }

    void i_rotshft_bcl() {
        int ModRM;
        int src;
        int[] dst = new int[1];
        byte c;
        ModRM = GetModRM();
        src = GetRMByte(ModRM);
        dst[0] = src;
        c = I.regs.b[2];
        CLKM(ModRM, 7, 7, 2, 19, 19, 6);
        if (c != 0) {
            switch (ModRM & 0x38) {
                case 0x00:
                    do {
                        ROL_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x08:
                    do {
                        ROR_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x10:
                    do {
                        ROLC_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x18:
                    do {
                        RORC_BYTE(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMByte(ModRM, (byte) dst[0]);
                    break;
                case 0x20:
                    SHL_BYTE(c, /* ref */ dst, ModRM); break;
                case 0x28:
                    SHR_BYTE(c, /* ref */ dst, ModRM); break;
                case 0x30:
                    break;
                case 0x38:
                    SHRA_BYTE(c, /* ref */ dst, ModRM); break;
            }
        }
    }

    void i_rotshft_wcl() {
        int ModRM;
        int src;
        int[] dst = new int[1];
        byte c;
        ModRM = GetModRM();
        src = GetRMWord(ModRM);
        dst[0] = src;
        c = I.regs.b[2];
        CLKM(ModRM, 7, 7, 2, 27, 19, 6);
        if (c != 0) {
            switch (ModRM & 0x38) {
                case 0x00:
                    do {
                        ROL_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x08:
                    do {
                        ROR_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x10:
                    do {
                        ROLC_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x18:
                    do {
                        RORC_WORD(/* ref */ dst); c--;
                        CLK(1);
                    } while (c > 0); PutbackRMWord(ModRM, (short) dst[0]);
                    break;
                case 0x20:
                    SHL_WORD(c, /* ref */ dst, ModRM); break;
                case 0x28:
                    SHR_WORD(c, /* ref */ dst, ModRM); break;
                case 0x30:
                    break;
                case 0x38:
                    SHRA_WORD(c, /* ref */ dst, ModRM); break;
            }
        }
    }

    void i_aam() {
        byte mult = FETCH();
        mult = 0;
        I.regs.b[1] = (byte) (I.regs.b[0] / 10);
        I.regs.b[0] %= 10;
        SetSZPF_Word(I.regs.b[0] + I.regs.b[1] * 0x100);
        CLKS(15, 15, 12);
    }

    void i_aad() {
        byte mult = FETCH();
        mult = 0;
        I.regs.b[0] = (byte) (I.regs.b[1] * 10 + I.regs.b[0]);
        I.regs.b[1] = 0;
        SetSZPF_Byte(I.regs.b[0]);
        CLKS(7, 7, 8);
    }

    void i_setalc() {
        I.regs.b[0] = (byte) (CF() ? 0xff : 0x00);
        pendingCycles -= 3;
    }

    void i_trans() {
        int dest = (I.regs.b[6] + I.regs.b[7] * 0x100 + I.regs.b[0]) & 0xffff;
        I.regs.b[0] = GetMemB(3, dest);
        CLKS(9, 9, 5);
    }

    void i_fpo() {
        int ModRM;
        ModRM = GetModRM();
        pendingCycles -= 2;
    }

    void i_loopne() {
        byte disp = FETCH();
        //I.regs.w[1]--;
        short w1 = (short) (I.regs.b[2] + I.regs.b[3] * 0x100 - 1);
        I.regs.b[2] = (byte) (w1 % 0x100);
        I.regs.b[3] = (byte) (w1 / 0x100);
        if (!ZF() && (I.regs.b[2] + I.regs.b[3] * 0x100 != 0)) {
            I.ip = (short) (I.ip + disp);
            CLKS(14, 14, 6);
        } else {
            CLKS(5, 5, 3);
        }
    }

    void i_loope() {
        byte disp = FETCH();
        //I.regs.w[1]--;
        short w1 = (short) (I.regs.b[2] + I.regs.b[3] * 0x100 - 1);
        I.regs.b[2] = (byte) (w1 % 0x100);
        I.regs.b[3] = (byte) (w1 / 0x100);
        if (ZF() && (I.regs.b[2] + I.regs.b[3] * 0x100 != 0)) {
            I.ip = (short) (I.ip + disp);
            CLKS(14, 14, 6);
        } else {
            CLKS(5, 5, 3);
        }
    }

    void i_loop() {
        byte disp = FETCH();
        //I.regs.w[1]--;
        short w1 = (short) (I.regs.b[2] + I.regs.b[3] * 0x100 - 1);
        I.regs.b[2] = (byte) (w1 % 0x100);
        I.regs.b[3] = (byte) (w1 / 0x100);
        if (I.regs.b[2] + I.regs.b[3] * 0x100 != 0) {
            I.ip = (short) (I.ip + disp);
            CLKS(13, 13, 6);
        } else {
            CLKS(5, 5, 3);
        }
    }

    void i_jcxz() {
        byte disp = FETCH();
        if (I.regs.b[2] + I.regs.b[3] * 0x100 == 0) {
            I.ip = (short) (I.ip + disp);
            CLKS(13, 13, 6);
        } else {
            CLKS(5, 5, 3);
        }
    }

    void i_inal() {
        byte port = FETCH();
        I.regs.b[0] = ReadIOByte.apply((int) port);
        CLKS(9, 9, 5);
    }

    void i_inax() {
        byte port = FETCH();
        //I.regs.w[0] = ReadIOWord(port);
        short w0 = ReadIOWord.apply((int) port);
        I.regs.b[0] = (byte) (w0 % 0x100);
        I.regs.b[1] = (byte) (w0 / 0x100);
        CLKW(13, 13, 7, 13, 9, 5, port);
    }

    void i_outal() {
        byte port = FETCH();
        WriteIOByte.accept((int) port, I.regs.b[0]);
        CLKS(8, 8, 3);
    }

    void i_outax() {
        byte port = FETCH();
        //WriteIOWord(port, I.regs.w[0]);
        WriteIOWord.accept((int) port, (short) (I.regs.b[0] + I.regs.b[1] * 0x100));
        CLKW(12, 12, 5, 12, 8, 3, port);
    }

    void i_call_d16() {
        short tmp;
        tmp = FETCHWORD();
        PUSH(I.ip);
        I.ip = (short) (I.ip + tmp);
        //CHANGE_PC;
        pendingCycles -= 24;
    }

    void i_jmp_d16() {
        short tmp;
        tmp = FETCHWORD();
        I.ip = (short) (I.ip + tmp);
        //CHANGE_PC;
        pendingCycles -= 15;
    }

    void i_jmp_far() {
        short tmp, tmp1;
        tmp = FETCHWORD();
        tmp1 = FETCHWORD();
        I.sregs[1] = tmp1;
        I.ip = tmp;
        //CHANGE_PC;
        pendingCycles -= 27;
    }

    void i_jmp_d8() {
        int tmp = FETCH();
        pendingCycles -= 12;
        if (tmp == -2 && I.no_interrupt == 0 && (I.pending_irq == 0) && pendingCycles > 0) {
            pendingCycles %= 12;
        }
        I.ip = (short) (I.ip + tmp);
    }

    void i_inaldx() {
        //I.regs.b[0] = ReadIOByte(I.regs.w[2]);
        I.regs.b[0] = ReadIOByte.apply(I.regs.b[4] + I.regs.b[5] * 0x100);
        CLKS(8, 8, 5);
    }

    void i_inaxdx() {
        //I.regs.w[0] = ReadIOWord(I.regs.w[2]);
        short w0 = ReadIOWord.apply(I.regs.b[4] + I.regs.b[5] * 0x100);
        I.regs.b[0] = (byte) (w0 % 0x100);
        I.regs.b[1] = (byte) (w0 / 0x100);
        CLKW(12, 12, 7, 12, 8, 5, I.regs.b[4] + I.regs.b[5] * 0x100);
    }

    void i_outdxal() {
        //WriteIOByte(I.regs.w[2], I.regs.b[0]);
        WriteIOByte.accept(I.regs.b[4] + I.regs.b[5] * 0x100, I.regs.b[0]);
        CLKS(8, 8, 3);
    }

    void i_outdxax() {
        //WriteIOWord(I.regs.w[2], I.regs.w[0]);
        //CLKW(12, 12, 5, 12, 8, 3, I.regs.w[2]);
        WriteIOWord.accept(I.regs.b[4] + I.regs.b[5] * 0x100, (short) (I.regs.b[0] + I.regs.b[1] * 0x100));
        CLKW(12, 12, 5, 12, 8, 3, I.regs.b[4] + I.regs.b[5] * 0x100);
    }

    void i_lock() {
        I.no_interrupt = 1;
        CLK(2);
    }

    void i_repne() {
        byte next = fetchop();
        short c = (short) (I.regs.b[2] + I.regs.b[3] * 0x100); // I.regs.w[1];
        switch (next) {
            case 0x26:
                seg_prefix = 1;
                prefix_base = (I.sregs[0] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x2e:
                seg_prefix = 1;
                prefix_base = (I.sregs[1] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x36:
                seg_prefix = 1;
                prefix_base = (I.sregs[2] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x3e:
                seg_prefix = 1;
                prefix_base = (I.sregs[3] << 4);
                next = fetchop();
                CLK(2);
                break;
        }
        switch (next & 0xff) {
            case 0x6c:
                CLK(2);
                if (c != 0) do {
                    i_insb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);/*I.regs.w[1] = c;*/
                break;
            case 0x6d:
                CLK(2);
                if (c != 0) do {
                    i_insw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6e:
                CLK(2);
                if (c != 0) do {
                    i_outsb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6f:
                CLK(2);
                if (c != 0) do {
                    i_outsw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa4:
                CLK(2);
                if (c != 0) do {
                    i_movsb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa5:
                CLK(2);
                if (c != 0) do {
                    i_movsw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa6:
                CLK(2);
                if (c != 0) do {
                    i_cmpsb();
                    c--;
                } while (c > 0 && ZF() == false);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa7:
                CLK(2);
                if (c != 0) do {
                    i_cmpsw();
                    c--;
                } while (c > 0 && ZF() == false);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaa:
                CLK(2);
                if (c != 0) do {
                    i_stosb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xab:
                CLK(2);
                if (c != 0) do {
                    i_stosw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xac:
                CLK(2);
                if (c != 0) do {
                    i_lodsb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xad:
                CLK(2);
                if (c != 0) do {
                    i_lodsw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xae:
                CLK(2);
                if (c != 0) do {
                    i_scasb();
                    c--;
                } while (c > 0 && !ZF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaf:
                CLK(2);
                if (c != 0) do {
                    i_scasw();
                    c--;
                } while (c > 0 && !ZF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            default:
                nec_instruction[next].run(); break;
        }
        seg_prefix = 0;
    }

    void i_repe() {
        byte next = fetchop();
        short c = (short) (I.regs.b[2] + I.regs.b[3] * 0x100);// I.regs.w[1];
        switch (next) {
            case 0x26:
                seg_prefix = 1;
                prefix_base = (I.sregs[0] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x2e:
                seg_prefix = 1;
                prefix_base = (I.sregs[1] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x36:
                seg_prefix = 1;
                prefix_base = (I.sregs[2] << 4);
                next = fetchop();
                CLK(2);
                break;
            case 0x3e:
                seg_prefix = 1;
                prefix_base = (I.sregs[3] << 4);
                next = fetchop();
                CLK(2);
                break;
        }
        switch (next & 0xff) {
            case 0x6c:
                CLK(2);
                if (c != 0) do {
                    i_insb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);/*I.regs.w[1] = c;*/
                break;
            case 0x6d:
                CLK(2);
                if (c != 0) do {
                    i_insw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6e:
                CLK(2);
                if (c != 0) do {
                    i_outsb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0x6f:
                CLK(2);
                if (c != 0) do {
                    i_outsw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa4:
                CLK(2);
                if (c != 0) do {
                    i_movsb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa5:
                CLK(2);
                if (c != 0) do {
                    i_movsw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa6:
                CLK(2);
                if (c != 0) do {
                    i_cmpsb();
                    c--;
                } while (c > 0 && ZF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xa7:
                CLK(2);
                if (c != 0) do {
                    i_cmpsw();
                    c--;
                } while (c > 0 && ZF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaa:
                CLK(2);
                if (c != 0) do {
                    i_stosb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xab:
                CLK(2);
                if (c != 0) do {
                    i_stosw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xac:
                CLK(2);
                if (c != 0) do {
                    i_lodsb();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xad:
                CLK(2);
                if (c != 0) do {
                    i_lodsw();
                    c--;
                } while (c > 0);
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xae:
                CLK(2);
                if (c != 0) do {
                    i_scasb();
                    c--;
                } while (c > 0 && ZF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            case 0xaf:
                CLK(2);
                if (c != 0) do {
                    i_scasw();
                    c--;
                } while (c > 0 && ZF());
                I.regs.b[2] = (byte) (c % 0x100);
                I.regs.b[3] = (byte) (c / 0x100);
                break;
            default:
                nec_instruction[next].run(); break;
        }
        seg_prefix = 0;
    }

    void i_hlt() {
        pendingCycles = 0;
    }

    void i_cmc() {
        I.CarryVal = CF() ? 0 : 1;
        CLK(2);
    }

    void i_f6pre() {
        int ModRM;
        int tmp;
        int uresult, uresult2;
        int result, result2;
        ModRM = GetModRM();
        tmp = GetRMByte(ModRM);
        switch (ModRM & 0x38) {
            case 0x00:
                tmp &= FETCH();
                I.CarryVal = I.OverVal = 0;
                SetSZPF_Byte(tmp);
                pendingCycles -= (ModRM >= 0xc0) ? 4 : 11;
                break;
            case 0x08:
                break;
            case 0x10:
                PutbackRMByte(ModRM, (byte) (~tmp));
                pendingCycles -= (ModRM >= 0xc0) ? 2 : 16;
                break;
            case 0x18:
                I.CarryVal = (tmp != 0) ? 1 : 0;
                tmp = (~tmp) + 1;
                SetSZPF_Byte(tmp);
                PutbackRMByte(ModRM, (byte) (tmp & 0xff));
                pendingCycles -= (ModRM >= 0xc0) ? 2 : 16;
                break;
            case 0x20:
                uresult = I.regs.b[0] * tmp;
                //I.regs.w[0] = (ushort)uresult;
                I.regs.b[0] = (byte) ((short) uresult % 0x100);
                I.regs.b[1] = (byte) ((short) uresult / 0x100);
                I.CarryVal = I.OverVal = (I.regs.b[1] != 0) ? 1 : 0;
                pendingCycles -= (ModRM >= 0xc0) ? 30 : 36;
                break;
            case 0x28:
                result = (short) I.regs.b[0] * (short) ((byte) tmp);
                //I.regs.w[0] = (ushort)result;
                I.regs.b[0] = (byte) ((short) result % 0x100);
                I.regs.b[1] = (byte) ((short) result / 0x100);
                I.CarryVal = I.OverVal = (I.regs.b[1] != 0) ? 1 : 0;
                pendingCycles -= (ModRM >= 0xc0) ? 30 : 36;
                break;
            case 0x30:
                if (tmp != 0) {
                    boolean[] b1 = new boolean[1];
                    DIVUB(tmp, /* out */ b1);
                    if (b1[0]) {
                        break;
                    }
                } else {
                    nec_interrupt(0, false);
                }
                pendingCycles -= (ModRM >= 0xc0) ? 43 : 53;
                break;
            case 0x38:
                if (tmp != 0) {
                    boolean[] b1 = new boolean[1];
                    DIVB(tmp, /* out */ b1);
                    if (b1[0]) {
                        break;
                    }
                } else {
                    nec_interrupt(0, false);
                }
                pendingCycles -= (ModRM >= 0xc0) ? 43 : 53;
                break;
        }
    }

    void i_f7pre() {
        int ModRM;
        int tmp, tmp2;
        int uresult, uresult2;
        int result, result2;
        ModRM = GetModRM();
        tmp = GetRMWord(ModRM);
        switch (ModRM & 0x38) {
            case 0x00:
                tmp2 = FETCHWORD();
                tmp &= tmp2;
                I.CarryVal = I.OverVal = 0;
                SetSZPF_Word(tmp);
                pendingCycles -= (ModRM >= 0xc0) ? 4 : 11;
                break;
            case 0x08:
                break;
            case 0x10:
                PutbackRMWord(ModRM, (short) (~tmp));
                pendingCycles -= (ModRM >= 0xc0) ? 2 : 16;
                break;
            case 0x18:
                I.CarryVal = (tmp != 0) ? 1 : 0;
                tmp = (~tmp) + 1;
                SetSZPF_Word(tmp);
                PutbackRMWord(ModRM, (short) (tmp & 0xffff));
                pendingCycles -= (ModRM >= 0xc0) ? 2 : 16;
                break;
            case 0x20:
                uresult = (I.regs.b[0] + I.regs.b[1] * 0x100) * tmp;
                //I.regs.w[0] = (ushort)(uresult & 0xffff);
                //I.regs.w[2] = (ushort)(uresult >> 16);
                I.regs.b[0] = (byte) ((short) (uresult & 0xffff) % 0x100);
                I.regs.b[1] = (byte) ((short) (uresult & 0xffff) / 0x100);
                I.regs.b[4] = (byte) ((short) (uresult >> 16) % 0x100);
                I.regs.b[5] = (byte) ((short) (uresult >> 16) / 0x100);
                I.CarryVal = I.OverVal = ((I.regs.b[4] + I.regs.b[5] * 0x100) != 0) ? 1 : 0;
                pendingCycles -= (ModRM >= 0xc0) ? 30 : 36;
                break;
            case 0x28:
                result = (int) ((short) (I.regs.b[0] + I.regs.b[1] * 0x100)) * (int) ((short) tmp);
                //I.regs.w[0] = (ushort)(result & 0xffff);
                //I.regs.w[2] = (ushort)(result >> 16);
                I.regs.b[0] = (byte) ((short) (result & 0xffff) % 0x100);
                I.regs.b[1] = (byte) ((short) (result & 0xffff) / 0x100);
                I.regs.b[4] = (byte) ((short) (result >> 16) % 0x100);
                I.regs.b[5] = (byte) ((short) (result >> 16) / 0x100);
                I.CarryVal = I.OverVal = ((I.regs.b[4] + I.regs.b[5] * 0x100) != 0) ? 1 : 0;
                pendingCycles -= (ModRM >= 0xc0) ? 30 : 36;
                break;
            case 0x30:
                if (tmp != 0) {
                    boolean[] b1 = new boolean[1];
                    DIVUW(tmp, /* out */ b1);
                    if (b1[0]) {
                        break;
                    }
                } else {
                    nec_interrupt(0, false);
                }
                pendingCycles -= (ModRM >= 0xc0) ? 43 : 53;
                break;
            case 0x38:
                if (tmp != 0) {
                    boolean[] b1 = new boolean[1];
                    DIVW(tmp, /* out */ b1);
                    if (b1[0]) {
                        break;
                    }
                } else {
                    nec_interrupt(0, false);
                }
                pendingCycles -= (ModRM >= 0xc0) ? 43 : 53;
                break;
        }
    }

    void i_clc() {
        I.CarryVal = 0;
        CLK(2);
    }

    void i_stc() {
        I.CarryVal = 1;
        CLK(2);
    }

    void i_di() {
        I.IF = false;
        CLK(2);
    }

    void i_ei() {
        I.IF = true;
        CLK(2);
    }

    void i_cld() {
        I.DF = false;
        CLK(2);
    }

    void i_std() {
        I.DF = true;
        CLK(2);
    }

    void i_fepre() {
        int ModRM;
        byte tmp, tmp1;
        ModRM = GetModRM();
        tmp = GetRMByte(ModRM);
        switch (ModRM & 0x38) {
            case 0x00:
                tmp1 = (byte) (tmp + 1);
                I.OverVal = (tmp == 0x7f) ? 1 : 0;
                SetAF(tmp1, tmp, 1);
                SetSZPF_Byte(tmp1);
                PutbackRMByte(ModRM, tmp1);
                CLKM(ModRM, 2, 2, 2, 16, 16, 7);
                break;
            case 0x08:
                tmp1 = (byte) (tmp - 1);
                I.OverVal = (tmp == (byte) 0x80) ? 1 : 0;
                SetAF(tmp1, tmp, 1);
                SetSZPF_Byte(tmp1);
                PutbackRMByte(ModRM, tmp1);
                CLKM(ModRM, 2, 2, 2, 16, 16, 7);
                break;
            default:
                break;
        }
    }

    void i_ffpre() {
        int ModRM;
        short tmp, tmp1;
        ModRM = GetModRM();
        tmp = GetRMWord(ModRM);
        switch (ModRM & 0x38) {
            case 0x00:
                tmp1 = (short) (tmp + 1);
                I.OverVal = (tmp == 0x7fff) ? 1 : 0;
                SetAF(tmp1, tmp, 1);
                SetSZPF_Word(tmp1);
                PutbackRMWord(ModRM, tmp1);
                CLKM(ModRM, 2, 2, 2, 24, 16, 7);
                break;
            case 0x08:
                tmp1 = (short) (tmp - 1);
                I.OverVal = (tmp == (short) 0x8000) ? 1 : 0;
                SetAF(tmp1, tmp, 1);
                SetSZPF_Word(tmp1);
                PutbackRMWord(ModRM, tmp1);
                CLKM(ModRM, 2, 2, 2, 24, 16, 7);
                break;
            case 0x10:
                PUSH(I.ip);
                I.ip = tmp;
                //CHANGE_PC;
                pendingCycles -= (ModRM >= 0xc0) ? 16 : 20;
                break;
            case 0x18:
                tmp1 = I.sregs[1];
                I.sregs[1] = GetnextRMWord();
                PUSH(tmp1);
                PUSH(I.ip);
                I.ip = tmp;
                //CHANGE_PC;
                pendingCycles -= (ModRM >= 0xc0) ? 16 : 26;
                break;
            case 0x20:
                I.ip = tmp;
                //CHANGE_PC;
                pendingCycles -= 13;
                break;
            case 0x28:
                I.ip = tmp;
                I.sregs[1] = GetnextRMWord();
                //CHANGE_PC;
                pendingCycles -= 15;
                break;
            case 0x30:
                PUSH(tmp);
                pendingCycles -= 4;
                break;
            default:
                break;
        }
    }

    void i_invalid() {
        pendingCycles -= 10;
    }

//#endregion

//#region NecModrm

    short RegWord(int ModRM) {
        return (short) (I.regs.b[mod_RM.regw[ModRM] * 2] + I.regs.b[mod_RM.regw[ModRM] * 2 + 1] * 0x100);// I.regs.w[mod_RM.regw[ModRM]];
    }

    byte RegByte(int ModRM) {
        return I.regs.b[mod_RM.regb[ModRM]];
    }

    short GetRMWord(int ModRM) {
        return (short) (ModRM >= 0xc0 ? I.regs.b[mod_RM.RMw[ModRM] * 2] + I.regs.b[mod_RM.RMw[ModRM] * 2 + 1] * 0x100 : ReadWord.apply(GetEA[ModRM].get()));
    }

    void PutbackRMWord(int ModRM, short val) {
        if (ModRM >= 0xc0) {
            //I.regs.w[mod_RM.RMw[ModRM]] = val;
            I.regs.b[mod_RM.RMw[ModRM] * 2] = (byte) (val % 0x100);
            I.regs.b[mod_RM.RMw[ModRM] * 2 + 1] = (byte) (val / 0x100);
        } else {
            WriteWord.accept(EA, val);
        }
    }

    short GetnextRMWord() {
        return ReadWord.apply((EA & 0xf_0000) | ((EA + 2) & 0xffff));
    }

    void PutRMWord(int ModRM, short val) {
        if (ModRM >= 0xc0) {
            //I.regs.w[mod_RM.RMw[ModRM]] = val;
            I.regs.b[mod_RM.RMw[ModRM] * 2] = (byte) (val % 0x100);
            I.regs.b[mod_RM.RMw[ModRM] * 2 + 1] = (byte) (val / 0x100);
        } else {
            WriteWord.accept(GetEA[ModRM].get(), val);
        }
    }

    void PutImmRMWord(int ModRM) {
        short val;
        if (ModRM >= 0xc0) {
            //I.regs.w[mod_RM.RMw[ModRM]] = FETCHWORD();
            short w = FETCHWORD();
            I.regs.b[mod_RM.RMw[ModRM] * 2] = (byte) (w % 0x100);
            I.regs.b[mod_RM.RMw[ModRM] * 2 + 1] = (byte) (w / 0x100);
        } else {
            EA = GetEA[ModRM].get();
            val = FETCHWORD();
            WriteWord.accept(EA, val);
        }
    }

    byte GetRMByte(int ModRM) {
        return ((ModRM) >= 0xc0 ? I.regs.b[mod_RM.RMb[ModRM]] : ReadByte.apply(GetEA[ModRM].get()));
    }

    void PutRMByte(int ModRM, byte val) {
        if (ModRM >= 0xc0) {
            I.regs.b[mod_RM.RMb[ModRM]] = val;
        } else {
            WriteByte.accept(GetEA[ModRM].get(), val);
        }
    }

    void PutImmRMByte(int ModRM) {
        if (ModRM >= 0xc0) {
            I.regs.b[mod_RM.RMb[ModRM]] = FETCH();
        } else {
            EA = GetEA[ModRM].get();
            WriteByte.accept(EA, FETCH());
        }
    }

    void PutbackRMByte(int ModRM, byte val) {
        if (ModRM >= 0xc0) {
            I.regs.b[mod_RM.RMb[ModRM]] = val;
        } else {
            WriteByte.accept(EA, val);
        }
    }

    void DEF_br8(/* out */ int[] ModRM, /* out */ byte[] src, /* out */ byte[] dst) {
        ModRM[0] = FETCH();
        src[0] = RegByte(ModRM[0]);
        dst[0] = GetRMByte(ModRM[0]);
    }

    void DEF_wr16(/* out */ int[] ModRM, /* out */ short[] src, /* out */ short[] dst) {
        ModRM[0] = FETCH();
        src[0] = RegWord(ModRM[0]);
        dst[0] = GetRMWord(ModRM[0]);
    }

    void DEF_r8b(/* out */ int[] ModRM, /* out */ byte[] src, /* out */ byte[] dst) {
        ModRM[0] = FETCH();
        dst[0] = RegByte(ModRM[0]);
        src[0] = GetRMByte(ModRM[0]);
    }

    void DEF_r16w(/* out */ int[] ModRM, /* out */ short[] src, /* out */ short[] dst) {
        ModRM [0]= FETCH();
        dst[0] = RegWord(ModRM[0]);
        src[0] = GetRMWord(ModRM[0]);
    }

    void DEF_ald8(/* out */ byte[] src, /* out */ byte[] dst) {
        src[0] = FETCH();
        dst[0] = I.regs.b[0];
    }

    void DEF_axd16(/* out */ short[] src, /* out */ short[] dst) {
        src[0] = FETCH();
        dst[0] = (short) (I.regs.b[0] + I.regs.b[1] * 0x100);// I.regs.w[0];
        src[0] += (short) (FETCH() << 8);
    }

//#endregion
}
