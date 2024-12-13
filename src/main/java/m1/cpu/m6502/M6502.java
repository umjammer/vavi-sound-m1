/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m6502;


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


public class M6502 extends cpuexec_data {

    public static M6502[] mm1;
    public final Runnable[] insn;
    public final Runnable[] insn6502;
    public byte subtype;
    public Register ppc, pc, sp, zp, ea;
    public byte p, a, x, y, pending_irq, after_cli, nmi_state, irq_state, so_state;

    public interface irq_delegate extends Function<Integer, Integer> {

    }

    public irq_delegate irq_callback;

    public interface read8handler extends Function<Short, Byte> {

    }

    public interface write8handler extends BiConsumer<Short, Byte> {

    }

    public read8handler rdmem_id;
    public write8handler wrmem_id;
    private final short M6502_NMI_VEC = (short) 0xfffa;
    private final short M6502_RST_VEC = (short) 0xfffc;
    private final short M6502_IRQ_VEC = (short) 0xfffe;
    private static final int M6502_SET_OVERFLOW = 1;
    private int m6502_IntOccured;
    protected long totalExecutedCycles;
    protected int pendingCycles;

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

    public byte default_rdmem_id(short offset) {
        return ReadMemory.apply(offset);
    }

    private void default_wdmem_id(short offset, byte data) {
        WriteMemory.accept(offset, data);
    }

    public M6502() {
        insn6502 = new Runnable[] {
                this::m6502_00, this::m6502_01, this::m6502_02, this::m6502_03, this::m6502_04, this::m6502_05, this::m6502_06, this::m6502_07,
                this::m6502_08, this::m6502_09, this::m6502_0a, this::m6502_0b, this::m6502_0c, this::m6502_0d, this::m6502_0e, this::m6502_0f,
                this::m6502_10, this::m6502_11, this::m6502_12, this::m6502_13, this::m6502_14, this::m6502_15, this::m6502_16, this::m6502_17,
                this::m6502_18, this::m6502_19, this::m6502_1a, this::m6502_1b, this::m6502_1c, this::m6502_1d, this::m6502_1e, this::m6502_1f,
                this::m6502_20, this::m6502_21, this::m6502_22, this::m6502_23, this::m6502_24, this::m6502_25, this::m6502_26, this::m6502_27,
                this::m6502_28, this::m6502_29, this::m6502_2a, this::m6502_2b, this::m6502_2c, this::m6502_2d, this::m6502_2e, this::m6502_2f,
                this::m6502_30, this::m6502_31, this::m6502_32, this::m6502_33, this::m6502_34, this::m6502_35, this::m6502_36, this::m6502_37,
                this::m6502_38, this::m6502_39, this::m6502_3a, this::m6502_3b, this::m6502_3c, this::m6502_3d, this::m6502_3e, this::m6502_3f,
                this::m6502_40, this::m6502_41, this::m6502_42, this::m6502_43, this::m6502_44, this::m6502_45, this::m6502_46, this::m6502_47,
                this::m6502_48, this::m6502_49, this::m6502_4a, this::m6502_4b, this::m6502_4c, this::m6502_4d, this::m6502_4e, this::m6502_4f,
                this::m6502_50, this::m6502_51, this::m6502_52, this::m6502_53, this::m6502_54, this::m6502_55, this::m6502_56, this::m6502_57,
                this::m6502_58, this::m6502_59, this::m6502_5a, this::m6502_5b, this::m6502_5c, this::m6502_5d, this::m6502_5e, this::m6502_5f,
                this::m6502_60, this::m6502_61, this::m6502_62, this::m6502_63, this::m6502_64, this::m6502_65, this::m6502_66, this::m6502_67,
                this::m6502_68, this::m6502_69, this::m6502_6a, this::m6502_6b, this::m6502_6c, this::m6502_6d, this::m6502_6e, this::m6502_6f,
                this::m6502_70, this::m6502_71, this::m6502_72, this::m6502_73, this::m6502_74, this::m6502_75, this::m6502_76, this::m6502_77,
                this::m6502_78, this::m6502_79, this::m6502_7a, this::m6502_7b, this::m6502_7c, this::m6502_7d, this::m6502_7e, this::m6502_7f,
                this::m6502_80, this::m6502_81, this::m6502_82, this::m6502_83, this::m6502_84, this::m6502_85, this::m6502_86, this::m6502_87,
                this::m6502_88, this::m6502_89, this::m6502_8a, this::m6502_8b, this::m6502_8c, this::m6502_8d, this::m6502_8e, this::m6502_8f,
                this::m6502_90, this::m6502_91, this::m6502_92, this::m6502_93, this::m6502_94, this::m6502_95, this::m6502_96, this::m6502_97,
                this::m6502_98, this::m6502_99, this::m6502_9a, this::m6502_9b, this::m6502_9c, this::m6502_9d, this::m6502_9e, this::m6502_9f,
                this::m6502_a0, this::m6502_a1, this::m6502_a2, this::m6502_a3, this::m6502_a4, this::m6502_a5, this::m6502_a6, this::m6502_a7,
                this::m6502_a8, this::m6502_a9, this::m6502_aa, this::m6502_ab, this::m6502_ac, this::m6502_ad, this::m6502_ae, this::m6502_af,
                this::m6502_b0, this::m6502_b1, this::m6502_b2, this::m6502_b3, this::m6502_b4, this::m6502_b5, this::m6502_b6, this::m6502_b7,
                this::m6502_b8, this::m6502_b9, this::m6502_ba, this::m6502_bb, this::m6502_bc, this::m6502_bd, this::m6502_be, this::m6502_bf,
                this::m6502_c0, this::m6502_c1, this::m6502_c2, this::m6502_c3, this::m6502_c4, this::m6502_c5, this::m6502_c6, this::m6502_c7,
                this::m6502_c8, this::m6502_c9, this::m6502_ca, this::m6502_cb, this::m6502_cc, this::m6502_cd, this::m6502_ce, this::m6502_cf,
                this::m6502_d0, this::m6502_d1, this::m6502_d2, this::m6502_d3, this::m6502_d4, this::m6502_d5, this::m6502_d6, this::m6502_d7,
                this::m6502_d8, this::m6502_d9, this::m6502_da, this::m6502_db, this::m6502_dc, this::m6502_dd, this::m6502_de, this::m6502_df,
                this::m6502_e0, this::m6502_e1, this::m6502_e2, this::m6502_e3, this::m6502_e4, this::m6502_e5, this::m6502_e6, this::m6502_e7,
                this::m6502_e8, this::m6502_e9, this::m6502_ea, this::m6502_eb, this::m6502_ec, this::m6502_ed, this::m6502_ee, this::m6502_ef,
                this::m6502_f0, this::m6502_f1, this::m6502_f2, this::m6502_f3, this::m6502_f4, this::m6502_f5, this::m6502_f6, this::m6502_f7,
                this::m6502_f8, this::m6502_f9, this::m6502_fa, this::m6502_fb, this::m6502_fc, this::m6502_fd, this::m6502_fe, this::m6502_ff
        };
        insn = insn6502;
    }

    public void m6502_common_init(irq_delegate irqcallback) {
        irq_callback = irqcallback;
        subtype = 0;
        rdmem_id = this::default_rdmem_id;
        wrmem_id = this::default_wdmem_id;
    }

    @Override
    public void Reset() {
        m6502_reset();
    }

    public void m6502_reset() {
        pc.lowByte = RDMEM(M6502_RST_VEC);
        pc.highByte = RDMEM((short) (M6502_RST_VEC + 1));
        sp.d = 0x01ff;
        p = (byte) (F_T | F_I | F_Z | F_B | (p & F_D));
        pending_irq = 0;
        after_cli = 0;
        irq_state = 0;
        nmi_state = 0;
    }

    public void m6502_take_irq() {
        if ((p & F_I) == 0) {
            ea.d = M6502_IRQ_VEC;
            pendingCycles -= 2;
            PUSH(pc.highByte);
            PUSH(pc.lowByte);
            PUSH((byte) (p & ~F_B));
            p |= F_I;
            pc.lowByte = RDMEM((short) ea.d);
            pc.highByte = RDMEM((short) (ea.d + 1));
            if (irq_callback != null) {
                irq_callback.apply(0);
            }
        }
        pending_irq = 0;
    }

    @Override
    public int ExecuteCycles(int cycles) {
        return m6502_execute(cycles);
    }

    public int m6502_execute(int cycles) {
        pendingCycles = cycles;
        do {
            byte op;
            ppc.d = pc.d;
            //debugger_instruction_hook(Machine, PCD);
            if (pending_irq != 0) {
                m6502_take_irq();
            }
            op = ReadOp.apply(pc.lowWord);
            pc.lowWord++;
            pendingCycles -= 1;
            insn[op].run();
            if (after_cli != 0) {
                after_cli = 0;
                if (irq_state != (byte) LineState.CLEAR_LINE.ordinal()) {
                    pending_irq = 1;
                }
            } else {
                if (pending_irq == 2) {
                    if (m6502_IntOccured - pendingCycles > 1) {
                        pending_irq = 1;
                    }
                }
                if (pending_irq == 1) {
                    m6502_take_irq();
                }
                if (pending_irq == 2) {
                    pending_irq = 1;
                }
            }
        }
        while (pendingCycles > 0);
        return cycles - pendingCycles;
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        m6502_set_irq_line(irqline, state);
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
    }

    private void m6502_set_irq_line(int irqline, LineState state) {
        if (irqline == (byte) LineState.INPUT_LINE_NMI.ordinal()) {
            if (nmi_state == (byte) state.ordinal()) {
                return;
            }
            nmi_state = (byte) state.ordinal();
            if (state != LineState.CLEAR_LINE) {
                ea.d = M6502_NMI_VEC;
                pendingCycles -= 2;
                PUSH(pc.highByte);
                PUSH(pc.lowByte);
                PUSH((byte) (p & ~F_B));
                p |= F_I;
                pc.lowByte = RDMEM((short) ea.d);
                pc.highByte = RDMEM((short) (ea.d + 1));
            }
        } else {
            if (irqline == M6502_SET_OVERFLOW) {
                if (so_state != 0 && state.ordinal() == 0) {
                    p |= F_V;
                }
                so_state = (byte) state.ordinal();
                return;
            }
            irq_state = (byte) state.ordinal();
            if (state != LineState.CLEAR_LINE) {
                pending_irq = 1;
                m6502_IntOccured = pendingCycles;
            }
        }
    }

    public void SaveStateBinary(BinaryWriter writer) {
        writer.write(subtype);
        writer.write(ppc.lowWord);
        writer.write(pc.lowWord);
        writer.write(sp.lowWord);
        writer.write(p);
        writer.write(a);
        writer.write(x);
        writer.write(y);
        writer.write(pending_irq);
        writer.write(after_cli);
        writer.write(nmi_state);
        writer.write(irq_state);
        writer.write(so_state);
        writer.write(totalExecutedCycles);
        writer.write(pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        subtype = reader.readByte();
        ppc.lowWord = reader.readUInt16();
        pc.lowWord = reader.readUInt16();
        sp.lowWord = reader.readUInt16();
        p = reader.readByte();
        a = reader.readByte();
        x = reader.readByte();
        y = reader.readByte();
        pending_irq = reader.readByte();
        after_cli = reader.readByte();
        nmi_state = reader.readByte();
        irq_state = reader.readByte();
        so_state = reader.readByte();
        totalExecutedCycles = reader.readUInt64();
        pendingCycles = reader.readInt32();
    }

//#region M6502op

    protected void m6502_00() {
        BRK();
    }

    protected void m6502_20() {
        JSR();
    }

    protected void m6502_40() {
        RTI();
    }

    protected void m6502_60() {
        RTS();
    }

    protected void m6502_80() {
        int tmp;
        tmp = RDOPARG();
    }

    protected void m6502_a0() {
        int tmp;
        tmp = RDOPARG();
        LDY(tmp);
    }

    protected void m6502_c0() {
        int tmp;
        tmp = RDOPARG();
        CPY(tmp);
    }

    protected void m6502_e0() {
        int tmp;
        tmp = RDOPARG();
        CPX(tmp);
    }

    protected void m6502_10() {
        BPL();
    }

    protected void m6502_30() {
        BMI();
    }

    protected void m6502_50() {
        BVC();
    }

    protected void m6502_70() {
        BVS();
    }

    protected void m6502_90() {
        BCC();
    }

    protected void m6502_b0() {
        BCS();
    }

    protected void m6502_d0() {
        BNE();
    }

    protected void m6502_f0() {
        BEQ();
    }

    protected void m6502_01() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        ORA(tmp);
    }

    protected void m6502_21() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        AND(tmp);
    }

    protected void m6502_41() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        EOR(tmp);
    }

    protected void m6502_61() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        ADC(tmp);
    }

    protected void m6502_81() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp);
        EA_IDX();
        wrmem_id.accept((short) ea.d, (byte) tmp[0]);
        pendingCycles -= 1;
    }

    protected void m6502_a1() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        LDA(tmp);
    }

    protected void m6502_c1() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        CMP(tmp);
    }

    protected void m6502_e1() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        SBC(tmp);
    }

    protected void m6502_11() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        ORA(tmp);
    }

    protected void m6502_31() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        AND(tmp);
    }

    protected void m6502_51() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        EOR(tmp);
    }

    protected void m6502_71() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        ADC(tmp);
    }

    protected void m6502_91() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp); EA_IDY_NP();
        wrmem_id.accept((short) ea.d, (byte) tmp[0]);
        pendingCycles -= 1;
    }

    protected void m6502_b1() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        LDA(tmp);
    }

    protected void m6502_d1() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        CMP(tmp);
    }

    protected void m6502_f1() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        SBC(tmp);
    }

    protected void m6502_02() {
        KIL();
    }

    protected void m6502_22() {
        KIL();
    }

    protected void m6502_42() {
        KIL();
    }

    protected void m6502_62() {
        KIL();
    }

    protected void m6502_82() {
        int tmp;
        tmp = RDOPARG();
    }

    protected void m6502_a2() {
        int tmp;
        tmp = RDOPARG();
        LDX(tmp);
    }

    protected void m6502_c2() {
        int tmp;
        tmp = RDOPARG();
    }

    protected void m6502_e2() {
        int tmp;
        tmp = RDOPARG();
    }

    protected void m6502_12() {
        KIL();
    }

    protected void m6502_32() {
        KIL();
    }

    protected void m6502_52() {
        KIL();
    }

    protected void m6502_72() {
        KIL();
    }

    protected void m6502_92() {
        KIL();
    }

    protected void m6502_b2() {
        KIL();
    }

    protected void m6502_d2() {
        KIL();
    }

    protected void m6502_f2() {
        KIL();
    }

    protected void m6502_03() {
        int[] tmp = new int[1];
        EA_IDX();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_23() {
        int[] tmp = new int[1];
        EA_IDX();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_43() {
        int[] tmp = new int[1];
        EA_IDX();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_63() {
        int[] tmp = new int[1];
        EA_IDX();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_83() {
        int[] tmp = new int[1];
        SAX(/* ref */ tmp);
        EA_IDX();
        wrmem_id.accept((short) ea.d, (byte) tmp[0]);
        pendingCycles -= 1;
    }

    protected void m6502_a3() {
        int tmp;
        EA_IDX();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        LAX(tmp);
    }

    protected void m6502_c3() {
        int[] tmp = new int[1];
        EA_IDX();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_e3() {
        int[] tmp = new int[1];
        EA_IDX();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_13() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_33() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_53() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_73() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_93() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        SAH(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_b3() {
        int tmp;
        EA_IDY_P();
        tmp = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        LAX(tmp);
    }

    protected void m6502_d3() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_f3() {
        int[] tmp = new int[1];
        EA_IDY_NP();
        tmp[0] = rdmem_id.apply((short) ea.d);
        pendingCycles -= 1;
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp); WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_04() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_24() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        BIT(tmp);
    }

    protected void m6502_44() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_64() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_84() {
        int[] tmp = new int[1];
        STY(/* ref */ tmp);
        EA_ZPG();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_a4() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        LDY(tmp);
    }

    protected void m6502_c4() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        CPY(tmp);
    }

    protected void m6502_e4() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        CPX(tmp);
    }

    protected void m6502_14() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_34() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_54() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_74() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_94() {
        int[] tmp = new int[1];
        STY(/* ref */ tmp);
        EA_ZPX();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_b4() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        LDY(tmp);
    }

    protected void m6502_d4() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_f4() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_05() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        ORA(tmp);
    }

    protected void m6502_25() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        AND(tmp);
    }

    protected void m6502_45() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        EOR(tmp);
    }

    protected void m6502_65() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        ADC(tmp);
    }

    protected void m6502_85() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp);
        EA_ZPG();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_a5() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        LDA(tmp);
    }

    protected void m6502_c5() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        CMP(tmp);
    }

    protected void m6502_e5() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        SBC(tmp);
    }

    protected void m6502_15() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        ORA(tmp);
    }

    protected void m6502_35() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        AND(tmp);
    }

    protected void m6502_55() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        EOR(tmp);
    }

    protected void m6502_75() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        ADC(tmp);
    }

    protected void m6502_95() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp);
        EA_ZPX();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_b5() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        LDA(tmp);
    }

    protected void m6502_d5() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        CMP(tmp);
    }

    protected void m6502_f5() {
        int tmp;
        EA_ZPX();
        tmp = RDMEM((short) ea.d);
        SBC(tmp);
    }

    protected void m6502_06() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ASL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_26() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_46() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        LSR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_66() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_86() {
        int[] tmp = new int[1];
        STX(/* ref */ tmp);
        EA_ZPG();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_a6() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        LDX(tmp);
    }

    protected void m6502_c6() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DEC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_e6() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        INC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_16() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ASL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_36() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_56() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        LSR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_76() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_96() {
        int[] tmp = new int[1];
        STX(/* ref */ tmp);
        EA_ZPY();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_b6() {
        int tmp;
        EA_ZPY();
        tmp = RDMEM((short) ea.d);
        LDX(tmp);
    }

    protected void m6502_d6() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DEC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_f6() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        INC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_07() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_27() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_47() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_67() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_87() {
        int[] tmp = new int[1];
        SAX(/* ref */ tmp);
        EA_ZPG();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_a7() {
        int tmp;
        EA_ZPG();
        tmp = RDMEM((short) ea.d);
        LAX(tmp);
    }

    protected void m6502_c7() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_e7() {
        int[] tmp = new int[1];
        EA_ZPG();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_17() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_37() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_57() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_77() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_97() {
        int[] tmp = new int[1];
        SAX(/* ref */ tmp);
        EA_ZPY();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_b7() {
        int tmp;
        EA_ZPY();
        tmp = RDMEM((short) ea.d);
        LAX(tmp);
    }

    protected void m6502_d7() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_f7() {
        int[] tmp = new int[1];
        EA_ZPX();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_08() {
        RDMEM(pc.lowWord);
        PHP();
    }

    protected void m6502_28() {
        RDMEM(pc.lowWord);
        PLP();
    }

    protected void m6502_48() {
        RDMEM(pc.lowWord);
        PHA();
    }

    protected void m6502_68() {
        RDMEM(pc.lowWord);
        PLA();
    }

    protected void m6502_88() {
        RDMEM(pc.lowWord);
        DEY();
    }

    protected void m6502_a8() {
        RDMEM(pc.lowWord);
        TAY();
    }

    protected void m6502_c8() {
        RDMEM(pc.lowWord);
        INY();
    }

    protected void m6502_e8() {
        RDMEM(pc.lowWord);
        INX();
    }

    protected void m6502_18() {
        RDMEM(pc.lowWord);
        CLC();
    }

    protected void m6502_38() {
        RDMEM(pc.lowWord);
        SEC();
    }

    protected void m6502_58() {
        RDMEM(pc.lowWord);
        CLI();
    }

    protected void m6502_78() {
        RDMEM(pc.lowWord);
        SEI();
    }

    protected void m6502_98() {
        RDMEM(pc.lowWord);
        TYA();
    }

    protected void m6502_b8() {
        RDMEM(pc.lowWord);
        CLV();
    }

    protected void m6502_d8() {
        RDMEM(pc.lowWord);
        CLD();
    }

    protected void m6502_f8() {
        RDMEM(pc.lowWord);
        SED();
    }

    protected void m6502_09() {
        int tmp;
        tmp = RDOPARG();
        ORA(tmp);
    }

    protected void m6502_29() {
        int tmp;
        tmp = RDOPARG();
        AND(tmp);
    }

    protected void m6502_49() {
        int tmp;
        tmp = RDOPARG();
        EOR(tmp);
    }

    protected void m6502_69() {
        int tmp;
        tmp = RDOPARG();
        ADC(tmp);
    }

    protected void m6502_89() {
        int tmp;
        tmp = RDOPARG();
    }

    protected void m6502_a9() {
        int tmp;
        tmp = RDOPARG();
        LDA(tmp);
    }

    protected void m6502_c9() {
        int tmp;
        tmp = RDOPARG();
        CMP(tmp);
    }

    protected void m6502_e9() {
        int tmp;
        tmp = RDOPARG();
        SBC(tmp);
    }

    protected void m6502_19() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        ORA(tmp);
    }

    protected void m6502_39() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        AND(tmp);
    }

    protected void m6502_59() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        EOR(tmp);
    }

    protected void m6502_79() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        ADC(tmp);
    }

    protected void m6502_99() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp);
        EA_ABY_NP();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_b9() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        LDA(tmp);
    }

    protected void m6502_d9() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        CMP(tmp);
    }

    protected void m6502_f9() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        SBC(tmp);
    }

    protected void m6502_0a() {
        int[] tmp = new int[1];
        RDMEM(pc.lowWord);
        tmp[0] = a;
        ASL(/* ref */ tmp); a = (byte) tmp[0];
    }

    protected void m6502_2a() {
        int[] tmp = new int[1];
        RDMEM(pc.lowWord);
        tmp[0] = a;
        ROL(/* ref */ tmp); a = (byte) tmp[0];
    }

    protected void m6502_4a() {
        int[] tmp = new int[1];
        RDMEM(pc.lowWord);
        tmp[0] = a;
        LSR(/* ref */ tmp); a = (byte) tmp[0];
    }

    protected void m6502_6a() {
        int[] tmp = new int[1];
        RDMEM(pc.lowWord);
        tmp[0] = a;
        ROR(/* ref */ tmp); a = (byte) tmp[0];
    }

    protected void m6502_8a() {
        RDMEM(pc.lowWord);
        TXA();
    }

    protected void m6502_aa() {
        RDMEM(pc.lowWord);
        TAX();
    }

    protected void m6502_ca() {
        RDMEM(pc.lowWord);
        DEX();
    }

    protected void m6502_ea() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_1a() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_3a() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_5a() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_7a() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_9a() {
        RDMEM(pc.lowWord);
        TXS();
    }

    protected void m6502_ba() {
        RDMEM(pc.lowWord);
        TSX();
    }

    protected void m6502_da() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_fa() {
        RDMEM(pc.lowWord);
    }

    protected void m6502_0b() {
        int tmp;
        tmp = RDOPARG();
        ANC(tmp);
    }

    protected void m6502_2b() {
        int tmp;
        tmp = RDOPARG();
        ANC(tmp);
    }

    protected void m6502_4b() {
        int[] tmp = new int[1];
        tmp[0] = RDOPARG();
        ASR(/* ref */ tmp);
        a = (byte) tmp[0];
    }

    protected void m6502_6b() {
        int[] tmp = new int[1];
        tmp[0] = RDOPARG();
        ARR(/* ref */ tmp);
        a = (byte) tmp[0];
    }

    protected void m6502_8b() {
        int tmp;
        tmp = RDOPARG();
        AXA(tmp);
    }

    protected void m6502_ab() {
        int tmp;
        tmp = RDOPARG();
        OAL(tmp);
    }

    protected void m6502_cb() {
        int tmp;
        tmp = RDOPARG();
        ASX(tmp);
    }

    protected void m6502_eb() {
        int tmp;
        tmp = RDOPARG();
        SBC(tmp);
    }

    protected void m6502_1b() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_3b() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_5b() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_7b() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_9b() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        SSH(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_bb() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        AST(tmp);
    }

    protected void m6502_db() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_fb() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_0c() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_2c() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        BIT(tmp);
    }

    protected void m6502_4c() {
        EA_ABS();
        JMP();
    }

    protected void m6502_6c() {
        int tmp;
        EA_IND();
        JMP();
    }

    protected void m6502_8c() {
        int[] tmp = new int[1];
        STY(/* ref */ tmp); EA_ABS();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_ac() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        LDY(tmp);
    }

    protected void m6502_cc() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        CPY(tmp);
    }

    protected void m6502_ec() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        CPX(tmp);
    }

    protected void m6502_1c() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_3c() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_5c() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_7c() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_9c() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        SYH(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_bc() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        LDY(tmp);
    }

    protected void m6502_dc() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_fc() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
    }

    protected void m6502_0d() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        ORA(tmp);
    }

    protected void m6502_2d() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        AND(tmp);
    }

    protected void m6502_4d() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        EOR(tmp);
    }

    protected void m6502_6d() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        ADC(tmp);
    }

    protected void m6502_8d() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp);
        EA_ABS();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_ad() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        LDA(tmp);
    }

    protected void m6502_cd() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        CMP(tmp);
    }

    protected void m6502_ed() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        SBC(tmp);
    }

    protected void m6502_1d() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        ORA(tmp);
    }

    protected void m6502_3d() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        AND(tmp);
    }

    protected void m6502_5d() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        EOR(tmp);
    }

    protected void m6502_7d() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        ADC(tmp);
    }

    protected void m6502_9d() {
        int[] tmp = new int[1];
        STA(/* ref */ tmp);
        EA_ABX_NP();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_bd() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        LDA(tmp);
    }

    protected void m6502_dd() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        CMP(tmp);
    }

    protected void m6502_fd() {
        int tmp;
        EA_ABX_P();
        tmp = RDMEM((short) ea.d);
        SBC(tmp);
    }

    protected void m6502_0e() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ASL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_2e() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_4e() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        LSR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_6e() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_8e() {
        int[] tmp = new int[1];
        STX(/* ref */ tmp);
        EA_ABS();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_ae() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        LDX(tmp);
    }

    protected void m6502_ce() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DEC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_ee() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        INC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_1e() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ASL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_3e() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROL(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_5e() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        LSR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_7e() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ROR(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_9e() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        SXH(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_be() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        LDX(tmp);
    }

    protected void m6502_de() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DEC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_fe() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        INC(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_0f() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_2f() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_4f() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_6f() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_8f() {
        int[] tmp = new int[1];
        SAX(/* ref */ tmp);
        EA_ABS();
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_af() {
        int tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        LAX(tmp);
    }

    protected void m6502_cf() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_ef() {
        int[] tmp = new int[1];
        EA_ABS();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_1f() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SLO(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_3f() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RLA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_5f() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        SRE(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_7f() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        RRA(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_9f() {
        int[] tmp = new int[1];
        EA_ABY_NP();
        SAH(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_bf() {
        int tmp;
        EA_ABY_P();
        tmp = RDMEM((short) ea.d);
        LAX(tmp);
    }

    protected void m6502_df() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        DCP(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

    protected void m6502_ff() {
        int[] tmp = new int[1];
        EA_ABX_NP();
        tmp[0] = RDMEM((short) ea.d);
        WRMEM((short) ea.d, (byte) tmp[0]);
        ISB(/* ref */ tmp);
        WRMEM((short) ea.d, (byte) tmp[0]);
    }

//#endregion

//#region Ops02

    public static final byte F_C = 0x01;
    public static final byte F_Z = 0x02;
    public static final byte F_I = 0x04;
    public static final byte F_D = 0x08;
    public static final byte F_B = 0x10;
    public static final byte F_T = 0x20;
    public static final byte F_V = 0x40;
    public static final byte F_N = (byte) 0x80;

    private void SET_NZ(byte n) {
        if (n == 0) {
            p = (byte) ((p & ~F_N) | F_Z);
        } else {
            p = (byte) ((p & ~(F_N | F_Z)) | (n & F_N));
        }
    }

    private void SET_Z(byte n) {
        if (n == 0) {
            p |= F_Z;
        } else {
            p &= (byte) ~F_Z;
        }
    }

    private byte RDOPARG() {
        byte b1;
        b1 = ReadOpArg.apply(pc.lowWord++);
        pendingCycles -= 1;
        return b1;
    }

    private byte RDMEM(short addr) {
        byte b1;
        b1 = ReadMemory.apply(addr);
        pendingCycles -= 1;
        return b1;
    }

    private void WRMEM(short addr, byte data) {
        WriteMemory.accept(addr, data);
        pendingCycles -= 1;
    }

    private void BRA(boolean cond) {
        byte tmp2 = RDOPARG();
        if (cond) {
            RDMEM(pc.lowWord);
            ea.lowWord = (short) (pc.lowWord + tmp2);
            if (ea.highByte != pc.highByte) {
                RDMEM((short) ((pc.highByte << 8) | ea.lowByte));
            }
            pc.d = ea.d;
        }
    }

    private void EA_ZPG() {
        zp.lowByte = RDOPARG();
        ea.d = zp.d;
    }

    private void EA_ZPX() {
        zp.lowByte = RDOPARG();
        RDMEM((short) zp.d);
        zp.lowByte = (byte) (x + zp.lowByte);
        ea.d = zp.d;
    }

    private void EA_ZPY() {
        zp.lowByte = RDOPARG();
        RDMEM((short) zp.d);
        zp.lowByte = (byte) (y + zp.lowByte);
        ea.d = zp.d;
    }

    private void EA_ABS() {
        ea.lowByte = RDOPARG();
        ea.highByte = RDOPARG();
    }

    private void EA_ABX_P() {
        EA_ABS();
        if (ea.lowByte + x > 0xff) {
            RDMEM((short) ((ea.highByte << 8) | ((ea.lowByte + x) & 0xff)));
        }
        ea.lowWord += x;
    }

    private void EA_ABX_NP() {
        EA_ABS();
        RDMEM((short) ((ea.highByte << 8) | ((ea.lowByte + x) & 0xff)));
        ea.lowWord += x;
    }

    private void EA_ABY_P() {
        EA_ABS();
        if (ea.lowByte + y > 0xff) {
            RDMEM((short) ((ea.highByte << 8) | ((ea.lowByte + y) & 0xff)));
        }
        ea.lowWord += y;
    }

    private void EA_ABY_NP() {
        EA_ABS();
        RDMEM((short) ((ea.highByte << 8) | ((ea.lowByte + y) & 0xff)));
        ea.lowWord += y;
    }

    private void EA_IDX() {
        zp.lowByte = RDOPARG();
        RDMEM((short) zp.d);
        zp.lowByte = (byte) (zp.lowByte + x);
        ea.lowByte = RDMEM((short) zp.d);
        zp.lowByte++;
        ea.highByte = RDMEM((short) zp.d);
    }

    private void EA_IDY_P() {
        zp.lowByte = RDOPARG();
        ea.lowByte = RDMEM((short) zp.d);
        zp.lowByte++;
        ea.highByte = RDMEM((short) zp.d);
        if (ea.lowByte + y > 0xff) {
            RDMEM((short) ((ea.highByte << 8) | ((ea.lowByte + y) & 0xff)));
        }
        ea.lowWord += y;
    }

    private void EA_IDY_NP() {
        zp.lowByte = RDOPARG();
        ea.lowByte = RDMEM((short) zp.d);
        zp.lowByte++;
        ea.highByte = RDMEM((short) zp.d);
        RDMEM((short) ((ea.highByte << 8) | ((ea.lowByte + y) & 0xff)));
        ea.lowWord += y;
    }

    private void EA_ZPI() {
        zp.lowByte = RDOPARG();
        ea.lowByte = RDMEM((short) zp.d);
        zp.lowByte++;
        ea.highByte = RDMEM((short) zp.d);
    }

    private void EA_IND() {
        byte tmp;
        EA_ABS();
        tmp = RDMEM((short) ea.d);
        ea.lowByte++;
        ea.highByte = RDMEM((short) ea.d);
        ea.lowByte = tmp;
    }

    private void PUSH(byte Rg) {
        WRMEM((short) sp.d, Rg);
        sp.lowByte--;
    }

    private void PULL(/* ref */ byte[] Rg) {
        sp.lowByte++;
        Rg[0] = RDMEM((short) sp.d);
    }

    private void ADC(int tmp) {
        if ((p & F_D) != 0) {
            int c = (p & F_C);
            int lo = (a & 0x0f) + (tmp & 0x0f) + c;
            int hi = (a & 0xf0) + (tmp & 0xf0);
            p &= (byte) (~(F_V | F_C | F_N | F_Z));
            if (((lo + hi) & 0xff) == 0) {
                p |= F_Z;
            }
            if (lo > 0x09) {
                hi += 0x10;
                lo += 0x06;
            }
            if ((hi & 0x80) != 0) {
                p |= F_N;
            }
            if ((~(a ^ tmp) & (a ^ hi) & F_N) != 0) {
                p |= F_V;
            }
            if (hi > 0x90) {
                hi += 0x60;
            }
            if ((hi & 0xff00) != 0) {
                p |= F_C;
            }
            a = (byte) ((lo & 0x0f) + (hi & 0xf0));
        } else {
            int c = (p & F_C);
            int sum = a + tmp + c;
            p &= (byte) (~(F_V | F_C));
            if ((~(a ^ tmp) & (a ^ sum) & F_N) != 0) {
                p |= F_V;
            }
            if ((sum & 0xff00) != 0) {
                p |= F_C;
            }
            a = (byte) sum;
            SET_NZ(a);
        }
    }

    private void AND(int tmp) {
        a = (byte) (a & tmp);
        SET_NZ(a);
    }

    private void ASL(/* ref */ int[] tmp) {
        p = (byte) ((p & ~F_C) | ((tmp[0] >> 7) & F_C));
        tmp[0] = (byte) (tmp[0] << 1);
        SET_NZ((byte) tmp[0]);
    }

    private void BCC() {
        BRA((p & F_C) == 0);
    }

    private void BCS() {
        BRA((p & F_C) != 0);
    }

    private void BEQ() {
        BRA((p & F_Z) != 0);
    }

    private void BIT(int tmp) {
        p &= (byte) (~(F_N | F_V | F_Z));
        p |= (byte) (tmp & (F_N | F_V));
        if ((tmp & a) == 0) {
            p |= F_Z;
        }
    }

    private void BMI() {
        BRA((p & F_N) != 0);
    }

    private void BNE() {
        BRA((p & F_Z) == 0);
    }

    private void BPL() {
        BRA((p & F_N) == 0);
    }

    private void BRK() {
        RDOPARG();
        PUSH(pc.highByte);
        PUSH(pc.lowByte);
        PUSH((byte) (p | F_B));
        p = ((byte) (p | F_I));
        pc.lowByte = RDMEM(M6502_IRQ_VEC);
        pc.highByte = RDMEM((short) (M6502_IRQ_VEC + 1));
    }

    private void BVC() {
        BRA((p & F_V) == 0);
    }

    private void BVS() {
        BRA((p & F_V) != 0);
    }

    private void CLC() {
        p &= (byte) ~F_C;
    }

    private void CLD() {
        p &= (byte) ~F_D;
    }

    private void CLI() {
        if ((irq_state != (byte) LineState.CLEAR_LINE.ordinal()) && ((p & F_I) != 0)) {
            after_cli = 1;
        }
        p &= (byte) ~F_I;
    }

    private void CLV() {
        p &= (byte) ~F_V;
    }

    private void CMP(int tmp) {
        p &= (byte) ~F_C;
        if (a >= tmp) {
            p |= F_C;
        }
        SET_NZ((byte) (a - tmp));
    }

    private void CPX(int tmp) {
        p &= (byte) ~F_C;
        if (x >= tmp) {
            p |= F_C;
        }
        SET_NZ((byte) (x - tmp));
    }

    private void CPY(int tmp) {
        p &= (byte) ~F_C;
        if (y >= tmp) {
            p |= F_C;
        }
        SET_NZ((byte) (y - tmp));
    }

    private void DEC(/* ref */ int[] tmp) {
        tmp[0] = (byte) (tmp[0] - 1);
        SET_NZ((byte) tmp[0]);
    }

    private void DEX() {
        x = (byte) (x - 1);
        SET_NZ(x);
    }

    private void DEY() {
        y = (byte) (y - 1);
        SET_NZ(y);
    }

    private void EOR(int tmp) {
        a = (byte) (a ^ tmp);
        SET_NZ(a);
    }

    private void INC(/* ref */ int[] tmp) {
        tmp[0] = (byte) (tmp[0] + 1);
        SET_NZ((byte) tmp[0]);
    }

    private void INX() {
        x = (byte) (x + 1);
        SET_NZ(x);
    }

    private void INY() {
        y = (byte) (y + 1);
        SET_NZ(y);
    }

    private void JMP() {
        if (ea.d == ppc.d && pending_irq == 0 && after_cli == 0) {
            if (pendingCycles > 0) {
                pendingCycles = 0;
            }
        }
        pc.d = ea.d;
    }

    private void JSR() {
        ea.lowByte = RDOPARG();
        RDMEM((short) sp.d);
        PUSH(pc.highByte);
        PUSH(pc.lowByte);
        ea.highByte = RDOPARG();
        pc.d = ea.d;
    }

    private void LDA(int tmp) {
        a = (byte) tmp;
        SET_NZ(a);
    }

    private void LDX(int tmp) {
        x = (byte) tmp;
        SET_NZ(x);
    }

    private void LDY(int tmp) {
        y = (byte) tmp;
        SET_NZ(y);
    }

    private void LSR(/* ref */ int[] tmp) {
        p = (byte) ((p & ~F_C) | (tmp[0] & F_C));
        tmp[0] = (byte) tmp[0] >> 1;
        SET_NZ((byte) tmp[0]);
    }

    private void ORA(int tmp) {
        a = (byte) (a | tmp);
        SET_NZ(a);
    }

    private void PHA() {
        PUSH(a);
    }

    private void PHP() {
        PUSH(p);
    }

    private void PLA() {
        RDMEM((short) sp.d);
        byte[] tmp = new byte[1];
        PULL(/* ref */ tmp);
        a = tmp[0];
        SET_NZ(a);
    }

    private void PLP() {
        RDMEM((short) sp.d);
        byte[] tmp = new byte[1];
        if ((p & F_I) != 0) {
            PULL(/* ref */ tmp);
            p = tmp[0];
            if ((irq_state != (byte) LineState.CLEAR_LINE.ordinal()) && ((p & F_I) == 0)) {
                after_cli = 1;
            }
        } else {
            PULL(/* ref */ tmp);
            p = tmp[0];
        }
        p |= (byte) (F_T | F_B);
    }

    private void ROL(/* ref */ int[] tmp) {
        tmp[0] = (tmp[0] << 1) | (p & F_C);
        p = (byte) ((p & ~F_C) | ((tmp[0] >> 8) & F_C));
        tmp[0] = (byte) tmp[0];
        SET_NZ((byte) tmp[0]);
    }

    private void ROR(/* ref */ int[] tmp) {
        tmp[0] |= (p & F_C) << 8;
        p = (byte) ((p & ~F_C) | (tmp[0] & F_C));
        tmp[0] = (byte) (tmp[0] >> 1);
        SET_NZ((byte) tmp[0]);
    }

    private void RTI() {
        RDOPARG();
        RDMEM((short) sp.d);
        byte[] tmp = new byte[1];
        PULL(/* ref */ tmp);
        p = tmp[0];
        PULL(/* ref */ tmp);
        pc.lowByte = tmp[0];
        PULL(/* ref */ tmp);
        pc.highByte = tmp[0];
        p |= (byte) (F_T | F_B);
        if ((irq_state != (byte) LineState.CLEAR_LINE.ordinal()) && ((p & F_I) == 0)) {
            after_cli = 1;
        }
    }

    private void RTS() {
        RDOPARG();
        RDMEM((short) sp.d);
        byte[] tmp = new byte[1];
        PULL(/* ref */ tmp);
        pc.lowByte = tmp[0];
        PULL(/* ref */ tmp);
        pc.highByte = tmp[0];
        RDMEM(pc.lowWord);
        pc.lowWord++;
    }

    private void SBC(int tmp) {
        if ((p & F_D) != 0) {
            int c = (p & F_C) ^ F_C;
            int sum = a - tmp - c;
            int lo = (a & 0x0f) - (tmp & 0x0f) - c;
            int hi = (a & 0xf0) - (tmp & 0xf0);
            if ((lo & 0x10) != 0) {
                lo -= 6;
                hi--;
            }
            p &= (byte) ~(F_V | F_C | F_Z | F_N);
            if (((a ^ tmp) & (a ^ sum) & F_N) != 0) {
                p |= F_V;
            }
            if ((hi & 0x0100) != 0) {
                hi -= 0x60;
            }
            if ((sum & 0xff00) == 0) {
                p |= F_C;
            }
            if (((a - tmp - c) & 0xff) == 0) {
                p |= F_Z;
            }
            if (((a - tmp - c) & 0x80) != 0) {
                p |= F_N;
            }
            a = (byte) ((lo & 0x0f) | (hi & 0xf0));
        } else {
            int c = (p & F_C) ^ F_C;
            int sum = a - tmp - c;
            p &= (byte) ~(F_V | F_C);
            if (((a ^ tmp) & (a ^ sum) & F_N) != 0) {
                p |= F_V;
            }
            if ((sum & 0xff00) == 0) {
                p |= F_C;
            }
            a = (byte) sum;
            SET_NZ(a);
        }
    }

    private void SEC() {
        p |= F_C;
    }

    private void SED() {
        p |= F_D;
    }

    private void SEI() {
        p |= F_I;
    }

    private void STA(/* ref */ int[] tmp) {
        tmp[0] = a;
    }

    private void STX(/* ref */ int[] tmp) {
        tmp[0] = x;
    }

    private void STY(/* ref */ int[] tmp) {
        tmp[0] = y;
    }

    private void TAX() {
        x = a;
        SET_NZ(x);
    }

    private void TAY() {
        y = a;
        SET_NZ(y);
    }

    private void TSX() {
        x = sp.lowByte;
        SET_NZ(x);
    }

    private void TXA() {
        a = x;
        SET_NZ(a);
    }

    private void TXS() {
        sp.lowByte = x;
    }

    private void TYA() {
        a = y;
        SET_NZ(a);
    }

//#endregion

//#region III02

    private void ANC(int tmp) {
        p &= (byte) ~F_C;
        a = (byte) (a & tmp);
        if ((a & 0x80) != 0) {
            p |= F_C;
        }
        SET_NZ(a);
    }

    private void ASR(/* ref */ int[] tmp) {
        tmp[0] &= a;
        LSR(/* ref */ tmp);
    }

    private void AST(int tmp) {
        sp.lowByte &= (byte) tmp;
        a = x = sp.lowByte;
        SET_NZ(a);
    }

    private void ARR(/* ref */ int[] tmp) {
        if ((p & F_D) != 0) {
            int lo, hi, t;
            tmp[0] &= a;
            t = tmp[0];
            hi = tmp[0] & 0xf0;
            lo = tmp[0] & 0x0f;
            if ((p & F_C) != 0) {
                tmp[0] = (tmp[0] >> 1) | 0x80;
                p |= F_N;
            } else {
                tmp[0] >>= 1;
                p &= (byte) ~F_N;
            }
            if (tmp[0] != 0) {
                p &= (byte) ~F_Z;
            } else {
                p |= F_Z;
            }
            if (((t ^ tmp[0]) & 0x40) != 0) {
                p |= F_V;
            } else {
                p &= (byte) ~F_V;
            }
            if (lo + (lo & 0x01) > 0x05) {
                tmp[0] = (tmp[0] & 0xf0) | ((tmp[0] + 6) & 0xf);
            }
            if (hi + (hi & 0x10) > 0x50) {
                p |= F_C;
                tmp[0] = (tmp[0] + 0x60) & 0xff;
            } else {
                p &= (byte) ~F_C;
            }
        } else {
            tmp[0] &= a;
            ROR(/* ref */ tmp);
            p &= (byte) ~(F_V | F_C);
            if ((tmp[0] & 0x40) != 0) {
                p |= F_C;
            }
            if ((tmp[0] & 0x60) == 0x20 || (tmp[0] & 0x60) == 0x40) {
                p |= F_V;
            }
        }
    }

    private void ASX(int tmp) {
        p &= (byte) ~F_C;
        x &= a;
        if (x >= tmp) {
            p |= F_C;
        }
        x = (byte) (x - tmp);
        SET_NZ(x);
    }

    private void AXA(int tmp) {
        a = (byte) ((a | 0xee) & x & tmp);
        SET_NZ(a);
    }

    private void DCP(/* ref */ int[] tmp) {
        tmp[0] = (byte) (tmp[0] - 1);
        p &= (byte) ~F_C;
        if (a >= tmp[0]) {
            p |= F_C;
        }
        SET_NZ((byte) (a - tmp[0]));
    }

    private void ISB(/* ref */ int[] tmp) {
        tmp[0] = (byte) (tmp[0] + 1);
        SBC(tmp[0]);
    }

    private void LAX(int tmp) {
        a = x = (byte) tmp;
        SET_NZ(a);
    }

    private void OAL(int tmp) {
        a = x = (byte) ((a | 0xee) & tmp);
        SET_NZ(a);
    }

    private void RLA(/* ref */ int[] tmp) {
        tmp[0] = (tmp[0] << 1) | (p & F_C);
        p = (byte) ((p & ~F_C) | ((tmp[0] >> 8) & F_C));
        tmp[0] = (byte) tmp[0];
        a &= (byte) tmp[0];
        SET_NZ(a);
    }

    private void RRA(/* ref */ int[] tmp) {
        tmp[0] |= (p & F_C) << 8;
        p = (byte) ((p & ~F_C) | (tmp[0] & F_C));
        tmp[0] = (byte) (tmp[0] >> 1);
        ADC(tmp[0]);
    }

    private void SAX(/* ref */ int[] tmp) {
        tmp[0] = a & x;
    }

    private void SLO(/* ref */ int[] tmp) {
        p = (byte) ((p & ~F_C) | ((tmp[0] >> 7) & F_C));
        tmp[0] = (byte) (tmp[0] << 1);
        a |= (byte) tmp[0];
        SET_NZ(a);
    }

    private void SRE(/* ref */ int[] tmp) {
        p = (byte) ((p & ~F_C) | (tmp[0] & F_C));
        tmp[0] = (byte) tmp[0] >> 1;
        a ^= (byte) tmp[0];
        SET_NZ(a);
    }

    private void SAH(/* ref */ int[] tmp) {
        tmp[0] = a & x & (ea.highByte + 1);
    }

    private void SSH(/* ref */ int[] tmp) {
        sp.lowByte = (byte) (a & x);
        tmp[0] = sp.lowByte & (ea.highByte + 1);
    }

    private void SXH(/* ref */ int[] tmp) {
        tmp[0] = x & (ea.highByte + 1);
    }

    private void SYH(/* ref */ int[] tmp) {
        tmp[0] = y & (ea.highByte + 1);
    }

    private void KIL() {
        pc.lowWord--;
    }

//#endregion
}
