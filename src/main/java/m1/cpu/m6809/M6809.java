/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m6809;

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


public class M6809 extends cpuexec_data {

    public static M6809[] mm1;
    public final Runnable[] insn;
    public Register PC, PPC, D, DP, U, S, X, Y, EA;
    public byte CC, ireg;
    public final LineState[] irq_state = new LineState[2];
    public int extra_cycles; /* cycles used up by interrupts */
    public byte int_state;
    public LineState nmi_state;
    private final byte CC_C = 0x01;
    private final byte CC_V = 0x02;
    private final byte CC_Z = 0x04;
    private final byte CC_N = 0x08;
    private final byte CC_II = 0x10;
    private final byte CC_H = 0x20;
    private final byte CC_IF = 0x40;
    private final byte CC_E = (byte) 0x80;
    private final byte M6809_CWAI = 8;
    private final byte M6809_SYNC = 16;
    private final byte M6809_LDS = 32;
    private final byte M6809_IRQ_LINE = 0;
    private final byte M6809_FIRQ_LINE = 1;
    public Function<Short, Byte> ReadOp, ReadOpArg;
    public Function<Short, Byte> RM;
    public BiConsumer<Short, Byte> WM;
    public Function<Short, Byte> ReadIO;
    public BiConsumer<Short, Byte> WriteIO;

    public interface irq_delegate extends Function<Integer, Integer> {

    }

    public irq_delegate irq_callback;

    public interface debug_delegate extends Runnable {

    }

    public debug_delegate debugger_start_cpu_hook_callback, debugger_stop_cpu_hook_callback;
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

    public final byte[] flags8i = new byte[] {
            0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x0a, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08
    };
    public final byte[] flags8d = new byte[] {
            0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
            0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08
    };
    private final byte[] cycles_6809 = new byte[] {
            0x06, 0x06, 0x02, 0x06, 0x06, 0x02, 0x06, 0x06, 0x06, 0x06, 0x06, 0x02, 0x06, 0x06, 0x03, 0x06,
            0x00, 0x00, 0x02, 0x04, 0x02, 0x02, 0x05, 0x09, 0x02, 0x02, 0x03, 0x02, 0x03, 0x02, 0x08, 0x06,
            0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
            0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05, 0x02, 0x05, 0x03, 0x06, 0x14, 0x0b, 0x02, 0x13,
            0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
            0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
            0x06, 0x02, 0x02, 0x06, 0x06, 0x02, 0x06, 0x06, 0x06, 0x06, 0x06, 0x02, 0x06, 0x06, 0x03, 0x06,
            0x07, 0x02, 0x02, 0x07, 0x07, 0x02, 0x07, 0x07, 0x07, 0x07, 0x07, 0x02, 0x07, 0x07, 0x04, 0x07,
            0x02, 0x02, 0x02, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x04, 0x07, 0x03, 0x02,
            0x04, 0x04, 0x04, 0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x06, 0x07, 0x05, 0x05,
            0x04, 0x04, 0x04, 0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x06, 0x07, 0x05, 0x05,
            0x05, 0x05, 0x05, 0x07, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x07, 0x08, 0x06, 0x06,
            0x02, 0x02, 0x02, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x02, 0x03, 0x03,
            0x04, 0x04, 0x04, 0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05,
            0x04, 0x04, 0x04, 0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05,
            0x05, 0x05, 0x05, 0x07, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x06
    };

    public M6809() {
        insn = new Runnable[] {
                this::neg_di, this::neg_di, this::illegal, this::com_di, this::lsr_di, this::illegal, this::ror_di, this::asr_di,
                this::asl_di, this::rol_di, this::dec_di, this::illegal, this::inc_di, this::tst_di, this::jmp_di, this::clr_di,
                this::pref10, this::pref11, this::nop, this::sync, this::illegal, this::illegal, this::lbra, this::lbsr,
                this::illegal, this::daa, this::orcc, this::illegal, this::andcc, this::sex, this::exg, this::tfr,
                this::bra, this::brn, this::bhi, this::bls, this::bcc, this::bcs, this::bne, this::beq,
                this::bvc, this::bvs, this::bpl, this::bmi, this::bge, this::blt, this::bgt, this::ble,
                this::leax, this::leay, this::leas, this::leau, this::pshs, this::puls, this::pshu, this::pulu,
                this::illegal, this::rts, this::abx, this::rti, this::cwai, this::mul, this::illegal, this::swi,
                this::nega, this::illegal, this::illegal, this::coma, this::lsra, this::illegal, this::rora, this::asra,
                this::asla, this::rola, this::deca, this::illegal, this::inca, this::tsta, this::illegal, this::clra,
                this::negb, this::illegal, this::illegal, this::comb, this::lsrb, this::illegal, this::rorb, this::asrb,
                this::aslb, this::rolb, this::decb, this::illegal, this::incb, this::tstb, this::illegal, this::clrb,
                this::neg_ix, this::illegal, this::illegal, this::com_ix, this::lsr_ix, this::illegal, this::ror_ix, this::asr_ix,
                this::asl_ix, this::rol_ix, this::dec_ix, this::illegal, this::inc_ix, this::tst_ix, this::jmp_ix, this::clr_ix,
                this::neg_ex, this::illegal, this::illegal, this::com_ex, this::lsr_ex, this::illegal, this::ror_ex, this::asr_ex,
                this::asl_ex, this::rol_ex, this::dec_ex, this::illegal, this::inc_ex, this::tst_ex, this::jmp_ex, this::clr_ex,
                this::suba_im, this::cmpa_im, this::sbca_im, this::subd_im, this::anda_im, this::bita_im, this::lda_im, this::sta_im,
                this::eora_im, this::adca_im, this::ora_im, this::adda_im, this::cmpx_im, this::bsr, this::ldx_im, this::stx_im,
                this::suba_di, this::cmpa_di, this::sbca_di, this::subd_di, this::anda_di, this::bita_di, this::lda_di, this::sta_di,
                this::eora_di, this::adca_di, this::ora_di, this::adda_di, this::cmpx_di, this::jsr_di, this::ldx_di, this::stx_di,
                this::suba_ix, this::cmpa_ix, this::sbca_ix, this::subd_ix, this::anda_ix, this::bita_ix, this::lda_ix, this::sta_ix,
                this::eora_ix, this::adca_ix, this::ora_ix, this::adda_ix, this::cmpx_ix, this::jsr_ix, this::ldx_ix, this::stx_ix,
                this::suba_ex, this::cmpa_ex, this::sbca_ex, this::subd_ex, this::anda_ex, this::bita_ex, this::lda_ex, this::sta_ex,
                this::eora_ex, this::adca_ex, this::ora_ex, this::adda_ex, this::cmpx_ex, this::jsr_ex, this::ldx_ex, this::stx_ex,
                this::subb_im, this::cmpb_im, this::sbcb_im, this::addd_im, this::andb_im, this::bitb_im, this::ldb_im, this::stb_im,
                this::eorb_im, this::adcb_im, this::orb_im, this::addb_im, this::ldd_im, this::std_im, this::ldu_im, this::stu_im,
                this::subb_di, this::cmpb_di, this::sbcb_di, this::addd_di, this::andb_di, this::bitb_di, this::ldb_di, this::stb_di,
                this::eorb_di, this::adcb_di, this::orb_di, this::addb_di, this::ldd_di, this::std_di, this::ldu_di, this::stu_di,
                this::subb_ix, this::cmpb_ix, this::sbcb_ix, this::addd_ix, this::andb_ix, this::bitb_ix, this::ldb_ix, this::stb_ix,
                this::eorb_ix, this::adcb_ix, this::orb_ix, this::addb_ix, this::ldd_ix, this::std_ix, this::ldu_ix, this::stu_ix,
                this::subb_ex, this::cmpb_ex, this::sbcb_ex, this::addd_ex, this::andb_ex, this::bitb_ex, this::ldb_ex, this::stb_ex,
                this::eorb_ex, this::adcb_ex, this::orb_ex, this::addb_ex, this::ldd_ex, this::std_ex, this::ldu_ex, this::stu_ex
        };
    }

    @Override
    public void Reset() {
        m6809_reset();
    }

    private void CHECK_IRQ_LINES() {
        if (irq_state[M6809_IRQ_LINE] != LineState.CLEAR_LINE || irq_state[M6809_FIRQ_LINE] != LineState.CLEAR_LINE)
            int_state &= (byte) (~M6809_SYNC);
        if (irq_state[M6809_FIRQ_LINE] != LineState.CLEAR_LINE && (CC & CC_IF) == 0) {
            if ((int_state & M6809_CWAI) != 0) {
                int_state &= (byte) (~M6809_CWAI);
                extra_cycles += 7;
            } else {
                CC &= (byte) (~CC_E);
                PUSHWORD(PC);
                PUSHBYTE(CC);
                extra_cycles += 10;
            }
            CC |= (byte) (CC_IF | CC_II);
            PC.lowWord = RM16((short) 0xfff6);
            if (irq_callback != null) {
                irq_callback.apply((int) M6809_FIRQ_LINE);
            }
        } else if (irq_state[M6809_IRQ_LINE] != LineState.CLEAR_LINE && (CC & CC_II) == 0) {

            if ((int_state & M6809_CWAI) != 0) {
                int_state &= (byte) (~M6809_CWAI);
                extra_cycles += 7;
            } else {
                CC |= CC_E;
                PUSHWORD(PC);
                PUSHWORD(U);
                PUSHWORD(Y);
                PUSHWORD(X);
                PUSHBYTE(DP.highByte);
                PUSHBYTE(D.lowByte);
                PUSHBYTE(D.highByte);
                PUSHBYTE(CC);
                extra_cycles += 19;
            }
            CC |= CC_II;
            PC.lowWord = RM16((short) 0xfff8);
            if (irq_callback != null) {
                irq_callback.apply((int) M6809_IRQ_LINE);
            }
        }
    }

    private byte IMMBYTE() {
        byte b = ReadOpArg.apply(PC.lowWord);
        PC.lowWord++;
        return b;
    }

    private Register IMMWORD() {
        Register w = new Register();
        w.d = (ReadOpArg.apply(PC.lowWord) << 8) | ReadOpArg.apply((short) ((PC.lowWord + 1) & 0xffff));
        PC.lowWord += 2;
        return w;
    }

    private void PUSHBYTE(byte b) {
        --S.lowWord;
        WM.accept(S.lowWord, b);
    }

    private void PUSHWORD(Register w) {
        --S.lowWord;
        WM.accept(S.lowWord, w.lowByte);
        --S.lowWord;
        WM.accept(S.lowWord, w.highByte);
    }

    private byte PULLBYTE() {
        byte b;
        b = RM.apply(S.lowWord);
        S.lowWord++;
        return b;
    }

    private short PULLWORD() {
        short w;
        w = (short) (RM.apply(S.lowWord) << 8);
        S.lowWord++;
        w |= RM.apply(S.lowWord);
        S.lowWord++;
        return w;
    }

    private void PSHUBYTE(byte b) {
        --U.lowWord;
        WM.accept(U.lowWord, b);
    }

    private void PSHUWORD(Register w) {
        --U.lowWord;
        WM.accept(U.lowWord, w.lowByte);
        --U.lowWord;
        WM.accept(U.lowWord, w.highByte);
    }

    private byte PULUBYTE() {
        byte b;
        b = RM.apply(U.lowWord);
        U.lowWord++;
        return b;
    }

    private short PULUWORD() {
        short w;
        w = (short) (RM.apply(U.lowWord) << 8);
        U.lowWord++;
        w |= RM.apply(U.lowWord);
        U.lowWord++;
        return w;
    }

    private void CLR_HNZVC() {
        CC &= (byte) (~(CC_H | CC_N | CC_Z | CC_V | CC_C));
    }

    private void CLR_NZV() {
        CC &= (byte) (~(CC_N | CC_Z | CC_V));
    }

    private void CLR_NZ() {
        CC &= (byte) (~(CC_N | CC_Z));
    }

    private void CLR_HNZC() {
        CC &= (byte) (~(CC_H | CC_N | CC_Z | CC_C));
    }

    private void CLR_NZVC() {
        CC &= (byte) (~(CC_N | CC_Z | CC_V | CC_C));
    }

    private void CLR_Z() {
        CC &= (byte) (~(CC_Z));
    }

    private void CLR_NZC() {
        CC &= (byte) (~(CC_N | CC_Z | CC_C));
    }

    private void CLR_ZC() {
        CC &= (byte) (~(CC_Z | CC_C));
    }

    private void SET_Z(int a) {
        if (a == 0) {
            SEZ();
        }
    }

    private void SET_Z8(byte a) {
        if (a == 0) {
            SEZ();
        }
    }

    private void SET_Z16(short a) {
        if (a == 0) {
            SEZ();
        }
    }

    private void SET_N8(byte a) {
        CC |= (byte) ((a & 0x80) >> 4);
    }

    private void SET_N16(short a) {
        CC |= (byte) ((a & 0x8000) >> 12);
    }

    private void SET_H(byte a, byte b, byte r) {
        CC |= (byte) (((a ^ b ^ r) & 0x10) << 1);
    }

    private void SET_C8(short a) {
        CC |= (byte) ((a & 0x100) >> 8);
    }

    private void SET_C16(int a) {
        CC |= (byte) ((a & 0x1_0000) >> 16);
    }

    private void SET_V8(byte a, short b, short r) {
        CC |= (byte) (((a ^ b ^ r ^ (r >> 1)) & 0x80) >> 6);
    }

    private void SET_V16(short a, short b, int r) {
        CC |= (byte) (((a ^ b ^ r ^ (r >> 1)) & 0x8000) >> 14);
    }

    private void SET_FLAGS8I(byte a) {
        CC |= flags8i[(a) & 0xff];
    }

    private void SET_FLAGS8D(byte a) {
        CC |= flags8d[(a) & 0xff];
    }

    private void SET_NZ8(byte a) {
        SET_N8(a);
        SET_Z(a);
    }

    private void SET_NZ16(short a) {
        SET_N16(a);
        SET_Z(a);
    }

    private void SET_FLAGS8(byte a, short b, short r) {
        SET_N8((byte) r);
        SET_Z8((byte) r);
        SET_V8(a, b, r);
        SET_C8(r);
    }

    private void SET_FLAGS16(short a, short b, int r) {
        SET_N16((short) r);
        SET_Z16((short) r);
        SET_V16(a, b, r);
        SET_C16(r);
    }

    private short SIGNED(byte b) {
        return (short) ((b & 0x80) != 0 ? b | 0xff00 : b);
    }

    private void DIRECT() {
        EA.d = DP.d;
        EA.lowByte = IMMBYTE();
    }

    private void IMM8() {
        EA.d = PC.d;
        PC.lowWord++;
    }

    private void IMM16() {
        EA.d = PC.d;
        PC.lowWord += 2;
    }

    private void EXTENDED() {
        EA = IMMWORD();
    }

    private void SEC() {
        CC |= CC_C;
    }

    private void CLC() {
        CC &= (byte) (~CC_C);
    }

    private void SEZ() {
        CC |= CC_Z;
    }

    private void CLZ() {
        CC &= (byte) (~CC_Z);
    }

    private void SEN() {
        CC |= CC_N;
    }

    private void CLN() {
        CC &= (byte) (~CC_N);
    }

    private void SEV() {
        CC |= CC_V;
    }

    private void CLV() {
        CC &= (byte) (~CC_V);
    }

    private void SEH() {
        CC |= CC_H;
    }

    private void CLH() {
        CC &= (byte) (~CC_H);
    }

    private byte DIRBYTE() {
        DIRECT();
        return RM.apply(EA.lowWord);
    }

    private Register DIRWORD() {
        Register w = new Register();
        DIRECT();
        w.lowWord = RM16(EA.lowWord);
        return w;
    }

    private byte EXTBYTE() {
        EXTENDED();
        return RM.apply(EA.lowWord);
    }

    private Register EXTWORD() {
        Register w = new Register();
        EXTENDED();
        w.lowWord = RM16(EA.lowWord);
        return w;
    }

    private void BRANCH(boolean f) {
        byte t = IMMBYTE();
        if (f) {
            PC.lowWord += SIGNED(t);
        }
    }

    private void LBRANCH(boolean f) {
        Register t = IMMWORD();
        if (f) {
            pendingCycles -= 1;
            PC.lowWord += t.lowWord;
        }
    }

    private byte NXORV() {
        return (byte) ((CC & CC_N) ^ ((CC & CC_V) << 2));
    }

    private short RM16(short Addr) {
        short result = (short) (RM.apply(Addr) << 8);
        return (short) (result | RM.apply((short) ((Addr + 1) & 0xffff)));
    }

    private void WM16(short Addr, Register p) {
        WM.accept(Addr, p.highByte);
        WM.accept((short) ((Addr + 1) & 0xffff), p.lowByte);
    }

    private void m6809_reset() {
        int_state = 0;
        nmi_state = LineState.CLEAR_LINE;
        irq_state[0] = LineState.CLEAR_LINE;
        irq_state[1] = LineState.CLEAR_LINE;
        DP.d = 0;            /* Reset direct page register */
        CC |= CC_II;        /* IRQ disabled */
        CC |= CC_IF;        /* FIRQ disabled */
        PC.lowWord = RM16((short) 0xfffe);
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        if (irqline == LineState.INPUT_LINE_NMI.ordinal()) {
            if (nmi_state == state)
                return;
            nmi_state = state;
            if (state == LineState.CLEAR_LINE)
                return;
            if ((int_state & M6809_LDS) == 0)
                return;
            int_state &= (byte) (~M6809_SYNC);
            if ((int_state & M6809_CWAI) != 0) {
                int_state &= (byte) (~M6809_CWAI);
                extra_cycles += 7;
            } else {
                CC |= CC_E;
                PUSHWORD(PC);
                PUSHWORD(U);
                PUSHWORD(Y);
                PUSHWORD(X);
                PUSHBYTE(DP.highByte);
                PUSHBYTE(D.lowByte);
                PUSHBYTE(D.highByte);
                PUSHBYTE(CC);
                extra_cycles += 19;
            }
            CC |= (byte) (CC_IF | CC_II);
            PC.lowWord = RM16((short) 0xfffc);
        } else if (irqline < 2) {
            irq_state[irqline] = state;
            if (state == LineState.CLEAR_LINE)
                return;
            CHECK_IRQ_LINES();
        }
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
    }

    @Override
    public int ExecuteCycles(int cycles) {
        pendingCycles = cycles - extra_cycles;
        extra_cycles = 0;
        if ((int_state & (M6809_CWAI | M6809_SYNC)) != 0) {
            //debugger_instruction_hook(Machine, PCD);
            pendingCycles = 0;
        } else {
            do {
                int prevCycles = pendingCycles;
                PPC = PC;
                //debugger_instruction_hook(Machine, PCD);
                ireg = ReadOp.apply(PC.lowWord);
                PC.lowWord++;
                debugger_start_cpu_hook_callback.run();
                insn[ireg].run();
                pendingCycles -= cycles_6809[ireg];
                debugger_stop_cpu_hook_callback.run();
                int delta = prevCycles - pendingCycles;
                totalExecutedCycles += delta;
            } while (pendingCycles > 0);

            pendingCycles -= extra_cycles;
            extra_cycles = 0;
        }
        return cycles - pendingCycles;
    }

    private void fetch_effective_address() {
        byte postbyte = ReadOpArg.apply(PC.lowWord);
        PC.lowWord++;
        switch (postbyte & 0xff) {
            case 0x00:
                EA.lowWord = X.lowWord;
                pendingCycles -= 1;
                break;
            case 0x01:
                EA.lowWord = (short) (X.lowWord + 1);
                pendingCycles -= 1;
                break;
            case 0x02:
                EA.lowWord = (short) (X.lowWord + 2);
                pendingCycles -= 1;
                break;
            case 0x03:
                EA.lowWord = (short) (X.lowWord + 3);
                pendingCycles -= 1;
                break;
            case 0x04:
                EA.lowWord = (short) (X.lowWord + 4);
                pendingCycles -= 1;
                break;
            case 0x05:
                EA.lowWord = (short) (X.lowWord + 5);
                pendingCycles -= 1;
                break;
            case 0x06:
                EA.lowWord = (short) (X.lowWord + 6);
                pendingCycles -= 1;
                break;
            case 0x07:
                EA.lowWord = (short) (X.lowWord + 7);
                pendingCycles -= 1;
                break;
            case 0x08:
                EA.lowWord = (short) (X.lowWord + 8);
                pendingCycles -= 1;
                break;
            case 0x09:
                EA.lowWord = (short) (X.lowWord + 9);
                pendingCycles -= 1;
                break;
            case 0x0a:
                EA.lowWord = (short) (X.lowWord + 10);
                pendingCycles -= 1;
                break;
            case 0x0b:
                EA.lowWord = (short) (X.lowWord + 11);
                pendingCycles -= 1;
                break;
            case 0x0c:
                EA.lowWord = (short) (X.lowWord + 12);
                pendingCycles -= 1;
                break;
            case 0x0d:
                EA.lowWord = (short) (X.lowWord + 13);
                pendingCycles -= 1;
                break;
            case 0x0e:
                EA.lowWord = (short) (X.lowWord + 14);
                pendingCycles -= 1;
                break;
            case 0x0f:
                EA.lowWord = (short) (X.lowWord + 15);
                pendingCycles -= 1;
                break;

            case 0x10:
                EA.lowWord = (short) (X.lowWord - 16);
                pendingCycles -= 1;
                break;
            case 0x11:
                EA.lowWord = (short) (X.lowWord - 15);
                pendingCycles -= 1;
                break;
            case 0x12:
                EA.lowWord = (short) (X.lowWord - 14);
                pendingCycles -= 1;
                break;
            case 0x13:
                EA.lowWord = (short) (X.lowWord - 13);
                pendingCycles -= 1;
                break;
            case 0x14:
                EA.lowWord = (short) (X.lowWord - 12);
                pendingCycles -= 1;
                break;
            case 0x15:
                EA.lowWord = (short) (X.lowWord - 11);
                pendingCycles -= 1;
                break;
            case 0x16:
                EA.lowWord = (short) (X.lowWord - 10);
                pendingCycles -= 1;
                break;
            case 0x17:
                EA.lowWord = (short) (X.lowWord - 9);
                pendingCycles -= 1;
                break;
            case 0x18:
                EA.lowWord = (short) (X.lowWord - 8);
                pendingCycles -= 1;
                break;
            case 0x19:
                EA.lowWord = (short) (X.lowWord - 7);
                pendingCycles -= 1;
                break;
            case 0x1a:
                EA.lowWord = (short) (X.lowWord - 6);
                pendingCycles -= 1;
                break;
            case 0x1b:
                EA.lowWord = (short) (X.lowWord - 5);
                pendingCycles -= 1;
                break;
            case 0x1c:
                EA.lowWord = (short) (X.lowWord - 4);
                pendingCycles -= 1;
                break;
            case 0x1d:
                EA.lowWord = (short) (X.lowWord - 3);
                pendingCycles -= 1;
                break;
            case 0x1e:
                EA.lowWord = (short) (X.lowWord - 2);
                pendingCycles -= 1;
                break;
            case 0x1f:
                EA.lowWord = (short) (X.lowWord - 1);
                pendingCycles -= 1;
                break;

            case 0x20:
                EA.lowWord = Y.lowWord;
                pendingCycles -= 1;
                break;
            case 0x21:
                EA.lowWord = (short) (Y.lowWord + 1);
                pendingCycles -= 1;
                break;
            case 0x22:
                EA.lowWord = (short) (Y.lowWord + 2);
                pendingCycles -= 1;
                break;
            case 0x23:
                EA.lowWord = (short) (Y.lowWord + 3);
                pendingCycles -= 1;
                break;
            case 0x24:
                EA.lowWord = (short) (Y.lowWord + 4);
                pendingCycles -= 1;
                break;
            case 0x25:
                EA.lowWord = (short) (Y.lowWord + 5);
                pendingCycles -= 1;
                break;
            case 0x26:
                EA.lowWord = (short) (Y.lowWord + 6);
                pendingCycles -= 1;
                break;
            case 0x27:
                EA.lowWord = (short) (Y.lowWord + 7);
                pendingCycles -= 1;
                break;
            case 0x28:
                EA.lowWord = (short) (Y.lowWord + 8);
                pendingCycles -= 1;
                break;
            case 0x29:
                EA.lowWord = (short) (Y.lowWord + 9);
                pendingCycles -= 1;
                break;
            case 0x2a:
                EA.lowWord = (short) (Y.lowWord + 10);
                pendingCycles -= 1;
                break;
            case 0x2b:
                EA.lowWord = (short) (Y.lowWord + 11);
                pendingCycles -= 1;
                break;
            case 0x2c:
                EA.lowWord = (short) (Y.lowWord + 12);
                pendingCycles -= 1;
                break;
            case 0x2d:
                EA.lowWord = (short) (Y.lowWord + 13);
                pendingCycles -= 1;
                break;
            case 0x2e:
                EA.lowWord = (short) (Y.lowWord + 14);
                pendingCycles -= 1;
                break;
            case 0x2f:
                EA.lowWord = (short) (Y.lowWord + 15);
                pendingCycles -= 1;
                break;

            case 0x30:
                EA.lowWord = (short) (Y.lowWord - 16);
                pendingCycles -= 1;
                break;
            case 0x31:
                EA.lowWord = (short) (Y.lowWord - 15);
                pendingCycles -= 1;
                break;
            case 0x32:
                EA.lowWord = (short) (Y.lowWord - 14);
                pendingCycles -= 1;
                break;
            case 0x33:
                EA.lowWord = (short) (Y.lowWord - 13);
                pendingCycles -= 1;
                break;
            case 0x34:
                EA.lowWord = (short) (Y.lowWord - 12);
                pendingCycles -= 1;
                break;
            case 0x35:
                EA.lowWord = (short) (Y.lowWord - 11);
                pendingCycles -= 1;
                break;
            case 0x36:
                EA.lowWord = (short) (Y.lowWord - 10);
                pendingCycles -= 1;
                break;
            case 0x37:
                EA.lowWord = (short) (Y.lowWord - 9);
                pendingCycles -= 1;
                break;
            case 0x38:
                EA.lowWord = (short) (Y.lowWord - 8);
                pendingCycles -= 1;
                break;
            case 0x39:
                EA.lowWord = (short) (Y.lowWord - 7);
                pendingCycles -= 1;
                break;
            case 0x3a:
                EA.lowWord = (short) (Y.lowWord - 6);
                pendingCycles -= 1;
                break;
            case 0x3b:
                EA.lowWord = (short) (Y.lowWord - 5);
                pendingCycles -= 1;
                break;
            case 0x3c:
                EA.lowWord = (short) (Y.lowWord - 4);
                pendingCycles -= 1;
                break;
            case 0x3d:
                EA.lowWord = (short) (Y.lowWord - 3);
                pendingCycles -= 1;
                break;
            case 0x3e:
                EA.lowWord = (short) (Y.lowWord - 2);
                pendingCycles -= 1;
                break;
            case 0x3f:
                EA.lowWord = (short) (Y.lowWord - 1);
                pendingCycles -= 1;
                break;

            case 0x40:
                EA.lowWord = U.lowWord;
                pendingCycles -= 1;
                break;
            case 0x41:
                EA.lowWord = (short) (U.lowWord + 1);
                pendingCycles -= 1;
                break;
            case 0x42:
                EA.lowWord = (short) (U.lowWord + 2);
                pendingCycles -= 1;
                break;
            case 0x43:
                EA.lowWord = (short) (U.lowWord + 3);
                pendingCycles -= 1;
                break;
            case 0x44:
                EA.lowWord = (short) (U.lowWord + 4);
                pendingCycles -= 1;
                break;
            case 0x45:
                EA.lowWord = (short) (U.lowWord + 5);
                pendingCycles -= 1;
                break;
            case 0x46:
                EA.lowWord = (short) (U.lowWord + 6);
                pendingCycles -= 1;
                break;
            case 0x47:
                EA.lowWord = (short) (U.lowWord + 7);
                pendingCycles -= 1;
                break;
            case 0x48:
                EA.lowWord = (short) (U.lowWord + 8);
                pendingCycles -= 1;
                break;
            case 0x49:
                EA.lowWord = (short) (U.lowWord + 9);
                pendingCycles -= 1;
                break;
            case 0x4a:
                EA.lowWord = (short) (U.lowWord + 10);
                pendingCycles -= 1;
                break;
            case 0x4b:
                EA.lowWord = (short) (U.lowWord + 11);
                pendingCycles -= 1;
                break;
            case 0x4c:
                EA.lowWord = (short) (U.lowWord + 12);
                pendingCycles -= 1;
                break;
            case 0x4d:
                EA.lowWord = (short) (U.lowWord + 13);
                pendingCycles -= 1;
                break;
            case 0x4e:
                EA.lowWord = (short) (U.lowWord + 14);
                pendingCycles -= 1;
                break;
            case 0x4f:
                EA.lowWord = (short) (U.lowWord + 15);
                pendingCycles -= 1;
                break;

            case 0x50:
                EA.lowWord = (short) (U.lowWord - 16);
                pendingCycles -= 1;
                break;
            case 0x51:
                EA.lowWord = (short) (U.lowWord - 15);
                pendingCycles -= 1;
                break;
            case 0x52:
                EA.lowWord = (short) (U.lowWord - 14);
                pendingCycles -= 1;
                break;
            case 0x53:
                EA.lowWord = (short) (U.lowWord - 13);
                pendingCycles -= 1;
                break;
            case 0x54:
                EA.lowWord = (short) (U.lowWord - 12);
                pendingCycles -= 1;
                break;
            case 0x55:
                EA.lowWord = (short) (U.lowWord - 11);
                pendingCycles -= 1;
                break;
            case 0x56:
                EA.lowWord = (short) (U.lowWord - 10);
                pendingCycles -= 1;
                break;
            case 0x57:
                EA.lowWord = (short) (U.lowWord - 9);
                pendingCycles -= 1;
                break;
            case 0x58:
                EA.lowWord = (short) (U.lowWord - 8);
                pendingCycles -= 1;
                break;
            case 0x59:
                EA.lowWord = (short) (U.lowWord - 7);
                pendingCycles -= 1;
                break;
            case 0x5a:
                EA.lowWord = (short) (U.lowWord - 6);
                pendingCycles -= 1;
                break;
            case 0x5b:
                EA.lowWord = (short) (U.lowWord - 5);
                pendingCycles -= 1;
                break;
            case 0x5c:
                EA.lowWord = (short) (U.lowWord - 4);
                pendingCycles -= 1;
                break;
            case 0x5d:
                EA.lowWord = (short) (U.lowWord - 3);
                pendingCycles -= 1;
                break;
            case 0x5e:
                EA.lowWord = (short) (U.lowWord - 2);
                pendingCycles -= 1;
                break;
            case 0x5f:
                EA.lowWord = (short) (U.lowWord - 1);
                pendingCycles -= 1;
                break;

            case 0x60:
                EA.lowWord = S.lowWord;
                pendingCycles -= 1;
                break;
            case 0x61:
                EA.lowWord = (short) (S.lowWord + 1);
                pendingCycles -= 1;
                break;
            case 0x62:
                EA.lowWord = (short) (S.lowWord + 2);
                pendingCycles -= 1;
                break;
            case 0x63:
                EA.lowWord = (short) (S.lowWord + 3);
                pendingCycles -= 1;
                break;
            case 0x64:
                EA.lowWord = (short) (S.lowWord + 4);
                pendingCycles -= 1;
                break;
            case 0x65:
                EA.lowWord = (short) (S.lowWord + 5);
                pendingCycles -= 1;
                break;
            case 0x66:
                EA.lowWord = (short) (S.lowWord + 6);
                pendingCycles -= 1;
                break;
            case 0x67:
                EA.lowWord = (short) (S.lowWord + 7);
                pendingCycles -= 1;
                break;
            case 0x68:
                EA.lowWord = (short) (S.lowWord + 8);
                pendingCycles -= 1;
                break;
            case 0x69:
                EA.lowWord = (short) (S.lowWord + 9);
                pendingCycles -= 1;
                break;
            case 0x6a:
                EA.lowWord = (short) (S.lowWord + 10);
                pendingCycles -= 1;
                break;
            case 0x6b:
                EA.lowWord = (short) (S.lowWord + 11);
                pendingCycles -= 1;
                break;
            case 0x6c:
                EA.lowWord = (short) (S.lowWord + 12);
                pendingCycles -= 1;
                break;
            case 0x6d:
                EA.lowWord = (short) (S.lowWord + 13);
                pendingCycles -= 1;
                break;
            case 0x6e:
                EA.lowWord = (short) (S.lowWord + 14);
                pendingCycles -= 1;
                break;
            case 0x6f:
                EA.lowWord = (short) (S.lowWord + 15);
                pendingCycles -= 1;
                break;

            case 0x70:
                EA.lowWord = (short) (S.lowWord - 16);
                pendingCycles -= 1;
                break;
            case 0x71:
                EA.lowWord = (short) (S.lowWord - 15);
                pendingCycles -= 1;
                break;
            case 0x72:
                EA.lowWord = (short) (S.lowWord - 14);
                pendingCycles -= 1;
                break;
            case 0x73:
                EA.lowWord = (short) (S.lowWord - 13);
                pendingCycles -= 1;
                break;
            case 0x74:
                EA.lowWord = (short) (S.lowWord - 12);
                pendingCycles -= 1;
                break;
            case 0x75:
                EA.lowWord = (short) (S.lowWord - 11);
                pendingCycles -= 1;
                break;
            case 0x76:
                EA.lowWord = (short) (S.lowWord - 10);
                pendingCycles -= 1;
                break;
            case 0x77:
                EA.lowWord = (short) (S.lowWord - 9);
                pendingCycles -= 1;
                break;
            case 0x78:
                EA.lowWord = (short) (S.lowWord - 8);
                pendingCycles -= 1;
                break;
            case 0x79:
                EA.lowWord = (short) (S.lowWord - 7);
                pendingCycles -= 1;
                break;
            case 0x7a:
                EA.lowWord = (short) (S.lowWord - 6);
                pendingCycles -= 1;
                break;
            case 0x7b:
                EA.lowWord = (short) (S.lowWord - 5);
                pendingCycles -= 1;
                break;
            case 0x7c:
                EA.lowWord = (short) (S.lowWord - 4);
                pendingCycles -= 1;
                break;
            case 0x7d:
                EA.lowWord = (short) (S.lowWord - 3);
                pendingCycles -= 1;
                break;
            case 0x7e:
                EA.lowWord = (short) (S.lowWord - 2);
                pendingCycles -= 1;
                break;
            case 0x7f:
                EA.lowWord = (short) (S.lowWord - 1);
                pendingCycles -= 1;
                break;

            case 0x80:
                EA.lowWord = X.lowWord;
                X.lowWord++;
                pendingCycles -= 2;
                break;
            case 0x81:
                EA.lowWord = X.lowWord;
                X.lowWord += 2;
                pendingCycles -= 3;
                break;
            case 0x82:
                X.lowWord--;
                EA.lowWord = X.lowWord;
                pendingCycles -= 2;
                break;
            case 0x83:
                X.lowWord -= 2;
                EA.lowWord = X.lowWord;
                pendingCycles -= 3;
                break;
            case 0x84:
                EA.lowWord = X.lowWord;
                break;
            case 0x85:
                EA.lowWord = (short) (X.lowWord + SIGNED(D.lowByte));
                pendingCycles -= 1;
                break;
            case 0x86:
                EA.lowWord = (short) (X.lowWord + SIGNED(D.highByte));
                pendingCycles -= 1;
                break;
            case 0x87:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0x88:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (X.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break; /* this is a hack to make Vectrex work. It should be m6809_ICount-=1. Dunno where the cycle was lost :( */
            case 0x89:
                EA = IMMWORD();
                EA.lowWord += X.lowWord;
                pendingCycles -= 4;
                break;
            case 0x8a:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0x8b:
                EA.lowWord = (short) (X.lowWord + D.lowWord);
                pendingCycles -= 4;
                break;
            case 0x8c:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0x8d:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                pendingCycles -= 5;
                break;
            case 0x8e:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0x8f:
                EA = IMMWORD();
                pendingCycles -= 5;
                break;

            case 0x90:
                EA.lowWord = X.lowWord;
                X.lowWord++;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 5;
                break; /* Indirect ,R+ not in my specs */
            case 0x91:
                EA.lowWord = X.lowWord;
                X.lowWord += 2;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0x92:
                X.lowWord--;
                EA.lowWord = X.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0x93:
                X.lowWord -= 2;
                EA.lowWord = X.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0x94:
                EA.lowWord = X.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 3;
                break;
            case 0x95:
                EA.lowWord = (short) (X.lowWord + SIGNED(D.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0x96:
                EA.lowWord = (short) (X.lowWord + SIGNED(D.highByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0x97:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0x98:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (X.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0x99:
                EA = IMMWORD();
                EA.lowWord += X.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0x9a:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0x9b:
                EA.lowWord = (short) (X.lowWord + D.lowWord);
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0x9c:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0x9d:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;
            case 0x9e:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0x9f:
                EA = IMMWORD();
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;

            case 0xa0:
                EA.lowWord = Y.lowWord;
                Y.lowWord++;
                pendingCycles -= 2;
                break;
            case 0xa1:
                EA.lowWord = Y.lowWord;
                Y.lowWord += 2;
                pendingCycles -= 3;
                break;
            case 0xa2:
                Y.lowWord--;
                EA.lowWord = Y.lowWord;
                pendingCycles -= 2;
                break;
            case 0xa3:
                Y.lowWord -= 2;
                EA.lowWord = Y.lowWord;
                pendingCycles -= 3;
                break;
            case 0xa4:
                EA.lowWord = Y.lowWord;
                break;
            case 0xa5:
                EA.lowWord = (short) (Y.lowWord + SIGNED(D.lowByte));
                pendingCycles -= 1;
                break;
            case 0xa6:
                EA.lowWord = (short) (Y.lowWord + SIGNED(D.highByte));
                pendingCycles -= 1;
                break;
            case 0xa7:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0xa8:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (Y.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0xa9:
                EA = IMMWORD();
                EA.lowWord += Y.lowWord;
                pendingCycles -= 4;
                break;
            case 0xaa:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0xab:
                EA.lowWord = (short) (Y.lowWord + D.lowWord);
                pendingCycles -= 4;
                break;
            case 0xac:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0xad:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                pendingCycles -= 5;
                break;
            case 0xae:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0xaf:
                EA = IMMWORD();
                pendingCycles -= 5;
                break;

            case 0xb0:
                EA.lowWord = Y.lowWord;
                Y.lowWord++;
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0xb1:
                EA.lowWord = Y.lowWord;
                Y.lowWord += 2;
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0xb2:
                Y.lowWord--;
                EA.lowWord = Y.lowWord;
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0xb3:
                Y.lowWord -= 2;
                EA.lowWord = Y.lowWord;
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0xb4:
                EA.lowWord = Y.lowWord;
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 3;
                break;
            case 0xb5:
                EA.lowWord = (short) (Y.lowWord + SIGNED(D.lowByte));
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xb6:
                EA.lowWord = (short) (Y.lowWord + SIGNED(D.highByte));
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xb7:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0xb8:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (Y.lowWord + SIGNED(EA.lowByte));
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xb9:
                EA = IMMWORD();
                EA.lowWord += Y.lowWord;
                EA.lowWord = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0xba:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0xbb:
                EA.lowWord = (short) (Y.lowWord + D.lowWord);
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0xbc:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xbd:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;
            case 0xbe:
                EA.lowWord = 0;
                break; /*   ILLEGAL*/
            case 0xbf:
                EA = IMMWORD();
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;

            case 0xc0:
                EA.lowWord = U.lowWord;
                U.lowWord++;
                pendingCycles -= 2;
                break;
            case 0xc1:
                EA.lowWord = U.lowWord;
                U.lowWord += 2;
                pendingCycles -= 3;
                break;
            case 0xc2:
                U.lowWord--;
                EA.lowWord = U.lowWord;
                pendingCycles -= 2;
                break;
            case 0xc3:
                U.lowWord -= 2;
                EA.lowWord = U.lowWord;
                pendingCycles -= 3;
                break;
            case 0xc4:
                EA.lowWord = U.lowWord;
                break;
            case 0xc5:
                EA.lowWord = (short) (U.lowWord + SIGNED(D.lowByte));
                pendingCycles -= 1;
                break;
            case 0xc6:
                EA.lowWord = (short) (U.lowWord + SIGNED(D.highByte));
                pendingCycles -= 1;
                break;
            case 0xc7:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xc8:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (U.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0xc9:
                EA = IMMWORD();
                EA.lowWord += U.lowWord;
                pendingCycles -= 4;
                break;
            case 0xca:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xcb:
                EA.lowWord = (short) (U.lowWord + D.lowWord);
                pendingCycles -= 4;
                break;
            case 0xcc:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0xcd:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                pendingCycles -= 5;
                break;
            case 0xce:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xcf:
                EA = IMMWORD();
                pendingCycles -= 5;
                break;

            case 0xd0:
                EA.lowWord = U.lowWord;
                U.lowWord++;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0xd1:
                EA.lowWord = U.lowWord;
                U.lowWord += 2;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0xd2:
                U.lowWord--;
                EA.lowWord = U.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0xd3:
                U.lowWord -= 2;
                EA.lowWord = U.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0xd4:
                EA.lowWord = U.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 3;
                break;
            case 0xd5:
                EA.lowWord = (short) (U.lowWord + SIGNED(D.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xd6:
                EA.lowWord = (short) (U.lowWord + SIGNED(D.highByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xd7:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xd8:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (U.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xd9:
                EA = IMMWORD();
                EA.lowWord += U.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0xda:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xdb:
                EA.lowWord = (short) (U.lowWord + D.lowWord);
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0xdc:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xdd:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;
            case 0xde:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xdf:
                EA = IMMWORD();
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;

            case 0xe0:
                EA.lowWord = S.lowWord;
                S.lowWord++;
                pendingCycles -= 2;
                break;
            case 0xe1:
                EA.lowWord = S.lowWord;
                S.lowWord += 2;
                pendingCycles -= 3;
                break;
            case 0xe2:
                S.lowWord--;
                EA.lowWord = S.lowWord;
                pendingCycles -= 2;
                break;
            case 0xe3:
                S.lowWord -= 2;
                EA.lowWord = S.lowWord;
                pendingCycles -= 3;
                break;
            case 0xe4:
                EA.lowWord = S.lowWord;
                break;
            case 0xe5:
                EA.lowWord = (short) (S.lowWord + SIGNED(D.lowByte));
                pendingCycles -= 1;
                break;
            case 0xe6:
                EA.lowWord = (short) (S.lowWord + SIGNED(D.highByte));
                pendingCycles -= 1;
                break;
            case 0xe7:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xe8:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (S.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0xe9:
                EA = IMMWORD();
                EA.lowWord += S.lowWord;
                pendingCycles -= 4;
                break;
            case 0xea:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xeb:
                EA.lowWord = (short) (S.lowWord + D.lowWord);
                pendingCycles -= 4;
                break;
            case 0xec:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                pendingCycles -= 1;
                break;
            case 0xed:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                pendingCycles -= 5;
                break;
            case 0xee:
                EA.lowWord = 0;
                break;  /*ILLEGAL*/
            case 0xef:
                EA = IMMWORD();
                pendingCycles -= 5;
                break;

            case 0xf0:
                EA.lowWord = S.lowWord;
                S.lowWord++;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0xf1:
                EA.lowWord = S.lowWord;
                S.lowWord += 2;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0xf2:
                S.lowWord--;
                EA.lowWord = S.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 5;
                break;
            case 0xf3:
                S.lowWord -= 2;
                EA.lowWord = S.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 6;
                break;
            case 0xf4:
                EA.lowWord = S.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 3;
                break;
            case 0xf5:
                EA.lowWord = (short) (S.lowWord + SIGNED(D.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xf6:
                EA.lowWord = (short) (S.lowWord + SIGNED(D.highByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xf7:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xf8:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (S.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xf9:
                EA = IMMWORD();
                EA.lowWord += S.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0xfa:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xfb:
                EA.lowWord = (short) (S.lowWord + D.lowWord);
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 7;
                break;
            case 0xfc:
                EA.lowWord = IMMBYTE();
                EA.lowWord = (short) (PC.lowWord + SIGNED(EA.lowByte));
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 4;
                break;
            case 0xfd:
                EA = IMMWORD();
                EA.lowWord += PC.lowWord;
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;
            case 0xfe:
                EA.lowWord = 0;
                break; /*ILLEGAL*/
            case 0xff:
                EA = IMMWORD();
                EA.d = RM16(EA.lowWord);
                pendingCycles -= 8;
                break;
        }
    }

    public void SaveStateBinary(BinaryWriter writer) {
        writer.write(PC.lowWord);
        writer.write(PPC.lowWord);
        writer.write(D.lowWord);
        writer.write(DP.lowWord);
        writer.write(U.lowWord);
        writer.write(S.lowWord);
        writer.write(X.lowWord);
        writer.write(Y.lowWord);
        writer.write(CC);
        writer.write((byte) irq_state[0].ordinal());
        writer.write((byte) irq_state[1].ordinal());
        writer.write(int_state);
        writer.write((byte) nmi_state.ordinal());
        writer.write(totalExecutedCycles);
        writer.write(pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        PC.lowWord = reader.readUInt16();
        PPC.lowWord = reader.readUInt16();
        D.lowWord = reader.readUInt16();
        DP.lowWord = reader.readUInt16();
        U.lowWord = reader.readUInt16();
        S.lowWord = reader.readUInt16();
        X.lowWord = reader.readUInt16();
        Y.lowWord = reader.readUInt16();
        CC = reader.readByte();
        irq_state[0] = LineState.values()[reader.readByte()];
        irq_state[1] = LineState.values()[reader.readByte()];
        int_state = reader.readByte();
        nmi_state = LineState.values()[reader.readByte()];
        totalExecutedCycles = reader.readUInt64();
        pendingCycles = reader.readInt32();
    }

//#region M6809op

    void illegal() {
    }

    void neg_di() {
        short r, t;
        t = DIRBYTE();
        r = (short) (-t);
        CLR_NZVC();
        SET_FLAGS8((byte) 0, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void com_di() {
        byte t;
        t = DIRBYTE();
        t = (byte) (~t);
        CLR_NZV();
        SET_NZ8(t);
        SEC();
        WM.accept(EA.lowWord, t);
    }

    void lsr_di() {
        byte t;
        t = DIRBYTE();
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        t >>= 1;
        SET_Z8(t);
        WM.accept(EA.lowWord, t);
    }

    void ror_di() {
        byte t, r;
        t = DIRBYTE();
        r = (byte) ((CC & CC_C) << 7);
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        r |= (byte) (t >> 1);
        SET_NZ8(r);
        WM.accept(EA.lowWord, r);
    }

    void asr_di() {
        byte t;
        t = DIRBYTE();
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        t = (byte) ((t & 0x80) | (t >> 1));
        SET_NZ8(t);
        WM.accept(EA.lowWord, t);
    }

    void asl_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void rol_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) ((CC & CC_C) | (t << 1));
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void dec_di() {
        byte t;
        t = DIRBYTE();
        --t;
        CLR_NZV();
        SET_FLAGS8D(t);
        WM.accept(EA.lowWord, t);
    }

    void inc_di() {
        byte t;
        t = DIRBYTE();
        ++t;
        CLR_NZV();
        SET_FLAGS8I(t);
        WM.accept(EA.lowWord, t);
    }

    void tst_di() {
        byte t;
        t = DIRBYTE();
        CLR_NZV();
        SET_NZ8(t);
    }

    void jmp_di() {
        DIRECT();
        PC.d = EA.d;
        //CHANGE_PC;
    }

    void clr_di() {
        DIRECT();
        RM.apply(EA.lowWord);
        WM.accept(EA.lowWord, (byte) 0);
        CLR_NZVC();
        SEZ();
    }

    void nop() {

    }

    void sync() {
        int_state |= M6809_SYNC;
        CHECK_IRQ_LINES();
        if ((int_state & M6809_SYNC) != 0)
            if (pendingCycles > 0) pendingCycles = 0;
    }

    void lbra() {
        EA = IMMWORD();
        PC.lowWord += EA.lowWord;
        //CHANGE_PC;
        if (EA.lowWord == (short) 0xfffd)
            if (pendingCycles > 0)
                pendingCycles = 0;
    }

    void lbsr() {
        EA = IMMWORD();
        PUSHWORD(PC);
        PC.lowWord += EA.lowWord;
        //CHANGE_PC;
    }

    void daa() {
        byte msn, lsn;
        short t, cf = 0;
        msn = (byte) (D.highByte & 0xf0);
        lsn = (byte) (D.highByte & 0x0f);
        if (lsn > 0x09 || (CC & CC_H) != 0)
            cf |= 0x06;
        if (((msn & 0xff) > 0x80) && ((lsn & 0xff) > 0x09))
            cf |= 0x60;
        if (((msn & 0xff) > 0x90) || (CC & CC_C) != 0)
            cf |= 0x60;
        t = (short) (cf + D.highByte);
        CLR_NZV();
        SET_NZ8((byte) t);
        SET_C8(t);
        D.highByte = (byte) t;
    }

    void orcc() {
        byte t;
        t = IMMBYTE();
        CC |= t;
        CHECK_IRQ_LINES();
    }

    void andcc() {
        byte t;
        t = IMMBYTE();
        CC &= t;
        CHECK_IRQ_LINES();
    }

    void sex() {
        short t;
        t = SIGNED(D.lowByte);
        D.lowWord = t;
        CLR_NZ();
        SET_NZ16(t);
    }

    void exg() {
        short t1, t2;
        byte tb;
        tb = IMMBYTE();
        if (((tb ^ (tb >> 4)) & 0x08) != 0) {
            t1 = t2 = 0xff;
        } else {
            switch (tb >> 4) {
                case 0:
                    t1 = D.lowWord;
                    break;
                case 1:
                    t1 = X.lowWord;
                    break;
                case 2:
                    t1 = Y.lowWord;
                    break;
                case 3:
                    t1 = U.lowWord;
                    break;
                case 4:
                    t1 = S.lowWord;
                    break;
                case 5:
                    t1 = PC.lowWord;
                    break;
                case 8:
                    t1 = D.highByte;
                    break;
                case 9:
                    t1 = D.lowByte;
                    break;
                case 10:
                    t1 = CC;
                    break;
                case 11:
                    t1 = DP.highByte;
                    break;
                default:
                    t1 = 0xff;
                    break;
            }
            switch (tb & 15) {
                case 0:
                    t2 = D.lowWord;
                    break;
                case 1:
                    t2 = X.lowWord;
                    break;
                case 2:
                    t2 = Y.lowWord;
                    break;
                case 3:
                    t2 = U.lowWord;
                    break;
                case 4:
                    t2 = S.lowWord;
                    break;
                case 5:
                    t2 = PC.lowWord;
                    break;
                case 8:
                    t2 = D.highByte;
                    break;
                case 9:
                    t2 = D.lowByte;
                    break;
                case 10:
                    t2 = CC;
                    break;
                case 11:
                    t2 = DP.highByte;
                    break;
                default:
                    t2 = 0xff;
                    break;
            }
        }
        switch (tb >> 4) {
            case 0:
                D.lowWord = t2;
                break;
            case 1:
                X.lowWord = t2;
                break;
            case 2:
                Y.lowWord = t2;
                break;
            case 3:
                U.lowWord = t2;
                break;
            case 4:
                S.lowWord = t2;
                break;
            case 5:
                PC.lowWord = t2;
                break;
            case 8:
                D.highByte = (byte) t2;
                break;
            case 9:
                D.lowByte = (byte) t2;
                break;
            case 10:
                CC = (byte) t2;
                break;
            case 11:
                DP.highByte = (byte) t2;
                break;
        }
        switch (tb & 15) {
            case 0:
                D.lowWord = t1;
                break;
            case 1:
                X.lowWord = t1;
                break;
            case 2:
                Y.lowWord = t1;
                break;
            case 3:
                U.lowWord = t1;
                break;
            case 4:
                S.lowWord = t1;
                break;
            case 5:
                PC.lowWord = t1;
                break;
            case 8:
                D.highByte = (byte) t1;
                break;
            case 9:
                D.lowByte = (byte) t1;
                break;
            case 10:
                CC = (byte) t1;
                break;
            case 11:
                DP.highByte = (byte) t1;
                break;
        }
    }

    void tfr() {
        byte tb;
        short t;
        tb = IMMBYTE();
        if (((tb ^ (tb >> 4)) & 0x08) != 0) {
            t = 0xff;
        } else {
            switch (tb >> 4) {
                case 0:
                    t = D.lowWord;
                    break;
                case 1:
                    t = X.lowWord;
                    break;
                case 2:
                    t = Y.lowWord;
                    break;
                case 3:
                    t = U.lowWord;
                    break;
                case 4:
                    t = S.lowWord;
                    break;
                case 5:
                    t = PC.lowWord;
                    break;
                case 8:
                    t = D.highByte;
                    break;
                case 9:
                    t = D.lowByte;
                    break;
                case 10:
                    t = CC;
                    break;
                case 11:
                    t = DP.highByte;
                    break;
                default:
                    t = 0xff;
                    break;
            }
        }
        switch (tb & 15) {
            case 0:
                D.lowWord = t;
                break;
            case 1:
                X.lowWord = t;
                break;
            case 2:
                Y.lowWord = t;
                break;
            case 3:
                U.lowWord = t;
                break;
            case 4:
                S.lowWord = t;
                break;
            case 5:
                PC.lowWord = t;
                break;
            case 8:
                D.highByte = (byte) t;
                break;
            case 9:
                D.lowByte = (byte) t;
                break;
            case 10:
                CC = (byte) t;
                break;
            case 11:
                DP.highByte = (byte) t;
                break;
        }
    }

    void bra() {
        byte t;
        t = IMMBYTE();
        PC.lowWord += SIGNED(t);
        if (t == (byte) 0xfe)
            if (pendingCycles > 0)
                pendingCycles = 0;
    }

    void brn() {
        byte t;
        t = IMMBYTE();
    }

    void lbrn() {
        EA = IMMWORD();
    }

    void bhi() {
        BRANCH((CC & (CC_Z | CC_C)) == 0);
    }

    void lbhi() {
        LBRANCH((CC & (CC_Z | CC_C)) == 0);
    }

    void bls() {
        BRANCH((CC & (CC_Z | CC_C)) != 0);
    }

    void lbls() {
        LBRANCH((CC & (CC_Z | CC_C)) != 0);
    }

    void bcc() {
        BRANCH((CC & CC_C) == 0);
    }

    void lbcc() {
        LBRANCH((CC & CC_C) == 0);
    }

    void bcs() {
        BRANCH((CC & CC_C) != 0);
    }

    void lbcs() {
        LBRANCH((CC & CC_C) != 0);
    }

    void bne() {
        BRANCH((CC & CC_Z) == 0);
    }

    void lbne() {
        LBRANCH((CC & CC_Z) == 0);
    }

    void beq() {
        BRANCH((CC & CC_Z) != 0);
    }

    void lbeq() {
        LBRANCH((CC & CC_Z) != 0);
    }

    void bvc() {
        BRANCH((CC & CC_V) == 0);
    }

    void lbvc() {
        LBRANCH((CC & CC_V) == 0);
    }

    void bvs() {
        BRANCH((CC & CC_V) != 0);
    }

    void lbvs() {
        LBRANCH((CC & CC_V) != 0);
    }

    void bpl() {
        BRANCH((CC & CC_N) == 0);
    }

    void lbpl() {
        LBRANCH((CC & CC_N) == 0);
    }

    void bmi() {
        BRANCH((CC & CC_N) != 0);
    }

    void lbmi() {
        LBRANCH((CC & CC_N) != 0);
    }

    void bge() {
        BRANCH(NXORV() == 0);
    }

    void lbge() {
        LBRANCH(NXORV() == 0);
    }

    void blt() {
        BRANCH(NXORV() != 0);
    }

    void lblt() {
        LBRANCH(NXORV() != 0);
    }

    void bgt() {
        BRANCH(!(NXORV() != 0 || (CC & CC_Z) != 0));
    }

    void lbgt() {
        LBRANCH(!(NXORV() != 0 || (CC & CC_Z) != 0));
    }

    void ble() {
        BRANCH(NXORV() != 0 || (CC & CC_Z) != 0);
    }

    void lble() {
        LBRANCH(NXORV() != 0 || (CC & CC_Z) != 0);
    }

    void leax() {
        fetch_effective_address();
        X.lowWord = EA.lowWord;
        CLR_Z();
        SET_Z(X.lowWord);
    }

    void leay() {
        fetch_effective_address();
        Y.lowWord = EA.lowWord;
        CLR_Z();
        SET_Z(Y.lowWord);
    }

    void leas() {
        fetch_effective_address();
        S.lowWord = EA.lowWord;
        int_state |= M6809_LDS;
    }

    void leau() {
        fetch_effective_address();
        U.lowWord = EA.lowWord;
    }

    void pshs() {
        byte t;
        t = IMMBYTE();
        if ((t & 0x80) != 0) {
            PUSHWORD(PC);
            pendingCycles -= 2;
        }
        if ((t & 0x40) != 0) {
            PUSHWORD(U);
            pendingCycles -= 2;
        }
        if ((t & 0x20) != 0) {
            PUSHWORD(Y);
            pendingCycles -= 2;
        }
        if ((t & 0x10) != 0) {
            PUSHWORD(X);
            pendingCycles -= 2;
        }
        if ((t & 0x08) != 0) {
            PUSHBYTE(DP.highByte);
            pendingCycles -= 1;
        }
        if ((t & 0x04) != 0) {
            PUSHBYTE(D.lowByte);
            pendingCycles -= 1;
        }
        if ((t & 0x02) != 0) {
            PUSHBYTE(D.highByte);
            pendingCycles -= 1;
        }
        if ((t & 0x01) != 0) {
            PUSHBYTE(CC);
            pendingCycles -= 1;
        }
    }

    void puls() {
        byte t;
        t = IMMBYTE();
        if ((t & 0x01) != 0) {
            CC = PULLBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x02) != 0) {
            D.highByte = PULLBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x04) != 0) {
            D.lowByte = PULLBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x08) != 0) {
            DP.highByte = PULLBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x10) != 0) {
            X.d = PULLWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x20) != 0) {
            Y.d = PULLWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x40) != 0) {
            U.d = PULLWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x80) != 0) {
            PC.d = PULLWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x01) != 0) {
            CHECK_IRQ_LINES();
        }
    }

    void pshu() {
        byte t;
        t = IMMBYTE();
        if ((t & 0x80) != 0) {
            PSHUWORD(PC);
            pendingCycles -= 2;
        }
        if ((t & 0x40) != 0) {
            PSHUWORD(S);
            pendingCycles -= 2;
        }
        if ((t & 0x20) != 0) {
            PSHUWORD(Y);
            pendingCycles -= 2;
        }
        if ((t & 0x10) != 0) {
            PSHUWORD(X);
            pendingCycles -= 2;
        }
        if ((t & 0x08) != 0) {
            PSHUBYTE(DP.highByte);
            pendingCycles -= 1;
        }
        if ((t & 0x04) != 0) {
            PSHUBYTE(D.lowByte);
            pendingCycles -= 1;
        }
        if ((t & 0x02) != 0) {
            PSHUBYTE(D.highByte);
            pendingCycles -= 1;
        }
        if ((t & 0x01) != 0) {
            PSHUBYTE(CC);
            pendingCycles -= 1;
        }
    }

    void pulu() {
        byte t;
        t = IMMBYTE();
        if ((t & 0x01) != 0) {
            CC = PULUBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x02) != 0) {
            D.highByte = PULUBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x04) != 0) {
            D.lowByte = PULUBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x08) != 0) {
            DP.highByte = PULUBYTE();
            pendingCycles -= 1;
        }
        if ((t & 0x10) != 0) {
            X.d = PULUWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x20) != 0) {
            Y.d = PULUWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x40) != 0) {
            S.d = PULUWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x80) != 0) {
            PC.d = PULUWORD();
            pendingCycles -= 2;
        }
        if ((t & 0x01) != 0) {
            CHECK_IRQ_LINES();
        }
    }

    void rts() {
        PC.d = PULLWORD();
        //CHANGE_PC;
    }

    void abx() {
        X.lowWord += D.lowByte;
    }

    void rti() {
        byte t;
        CC = PULLBYTE();
        t = (byte) (CC & CC_E);
        if (t != 0) {
            pendingCycles -= 9;
            D.highByte = PULLBYTE();
            D.lowByte = PULLBYTE();
            DP.highByte = PULLBYTE();
            X.d = PULLWORD();
            Y.d = PULLWORD();
            U.d = PULLWORD();
        }
        PC.d = PULLWORD();
        //CHANGE_PC;
        CHECK_IRQ_LINES();
    }

    void cwai() {
        byte t;
        t = IMMBYTE();
        CC &= t;
        CC |= CC_E;
        PUSHWORD(PC);
        PUSHWORD(U);
        PUSHWORD(Y);
        PUSHWORD(X);
        PUSHBYTE(DP.highByte);
        PUSHBYTE(D.lowByte);
        PUSHBYTE(D.highByte);
        PUSHBYTE(CC);
        int_state |= M6809_CWAI;
        CHECK_IRQ_LINES();
        if ((int_state & M6809_CWAI) != 0)
            if (pendingCycles > 0)
                pendingCycles = 0;
    }

    void mul() {
        short t;
        t = (short) (D.highByte * D.lowByte);
        CLR_ZC();
        SET_Z16(t);
        if ((t & 0x80) != 0)
            SEC();
        D.lowWord = t;
    }

    void swi() {
        CC |= CC_E;
        PUSHWORD(PC);
        PUSHWORD(U);
        PUSHWORD(Y);
        PUSHWORD(X);
        PUSHBYTE(DP.highByte);
        PUSHBYTE(D.lowByte);
        PUSHBYTE(D.highByte);
        PUSHBYTE(CC);
        CC |= (byte) (CC_IF | CC_II);
        PC.d = RM16((short) 0xfffa);
        //CHANGE_PC;
    }

    void swi2() {
        CC |= CC_E;
        PUSHWORD(PC);
        PUSHWORD(U);
        PUSHWORD(Y);
        PUSHWORD(X);
        PUSHBYTE(DP.highByte);
        PUSHBYTE(D.lowByte);
        PUSHBYTE(D.highByte);
        PUSHBYTE(CC);
        PC.d = RM16((short) 0xfff4);
        //CHANGE_PC;
    }

    void swi3() {
        CC |= CC_E;
        PUSHWORD(PC);
        PUSHWORD(U);
        PUSHWORD(Y);
        PUSHWORD(X);
        PUSHBYTE(DP.highByte);
        PUSHBYTE(D.lowByte);
        PUSHBYTE(D.highByte);
        PUSHBYTE(CC);
        PC.d = RM16((short) 0xfff2);
        //CHANGE_PC;
    }

    void nega() {
        short r;
        r = (short) (-D.highByte);
        CLR_NZVC();
        SET_FLAGS8((byte) 0, D.highByte, r);
        D.highByte = (byte) r;
    }

    void coma() {
        D.highByte = (byte) (~D.highByte);
        CLR_NZV();
        SET_NZ8(D.highByte);
        SEC();
    }

    void lsra() {
        CLR_NZC();
        CC |= (byte) (D.highByte & CC_C);
        D.highByte >>= 1;
        SET_Z8(D.highByte);
    }

    void rora() {
        byte r;
        r = (byte) ((CC & CC_C) << 7);
        CLR_NZC();
        CC |= (byte) (D.highByte & CC_C);
        r |= (byte) (D.highByte >> 1);
        SET_NZ8(r);
        D.highByte = r;
    }

    void asra() {
        CLR_NZC();
        CC |= (byte) (D.highByte & CC_C);
        D.highByte = (byte) ((D.highByte & 0x80) | (D.highByte >> 1));
        SET_NZ8(D.highByte);
    }

    void asla() {
        short r;
        r = (short) (D.highByte << 1);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, D.highByte, r);
        D.highByte = (byte) r;
    }

    void rola() {
        short t, r;
        t = D.highByte;
        r = (short) ((CC & CC_C) | (t << 1));
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        D.highByte = (byte) r;
    }

    void deca() {
        --D.highByte;
        CLR_NZV();
        SET_FLAGS8D(D.highByte);
    }

    void inca() {
        ++D.highByte;
        CLR_NZV();
        SET_FLAGS8I(D.highByte);
    }

    void tsta() {
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void clra() {
        D.highByte = 0;
        CLR_NZVC();
        SEZ();
    }

    void negb() {
        short r;
        r = (short) (-D.lowByte);
        CLR_NZVC();
        SET_FLAGS8((byte) 0, D.lowByte, r);
        D.lowByte = (byte) r;
    }

    void comb() {
        D.lowByte = (byte) (~D.lowByte);
        CLR_NZV();
        SET_NZ8(D.lowByte);
        SEC();
    }

    void lsrb() {
        CLR_NZC();
        CC |= (byte) (D.lowByte & CC_C);
        D.lowByte >>= 1;
        SET_Z8(D.lowByte);
    }

    void rorb() {
        byte r;
        r = (byte) ((CC & CC_C) << 7);
        CLR_NZC();
        CC |= (byte) (D.lowByte & CC_C);
        r |= (byte) (D.lowByte >> 1);
        SET_NZ8(r);
        D.lowByte = r;
    }

    void asrb() {
        CLR_NZC();
        CC |= (byte) (D.lowByte & CC_C);
        D.lowByte = (byte) ((D.lowByte & 0x80) | (D.lowByte >> 1));
        SET_NZ8(D.lowByte);
    }

    void aslb() {
        short r;
        r = (short) (D.lowByte << 1);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, D.lowByte, r);
        D.lowByte = (byte) r;
    }

    void rolb() {
        short t, r;
        t = D.lowByte;
        r = (short) (CC & CC_C);
        r |= (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        D.lowByte = (byte) r;
    }

    void decb() {
        --D.lowByte;
        CLR_NZV();
        SET_FLAGS8D(D.lowByte);
    }

    void incb() {
        ++D.lowByte;
        CLR_NZV();
        SET_FLAGS8I(D.lowByte);
    }

    void tstb() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void clrb() {
        D.lowByte = 0;
        CLR_NZVC();
        SEZ();
    }

    void neg_ix() {
        short r, t;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (-t);
        CLR_NZVC();
        SET_FLAGS8((byte) 0, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void com_ix() {
        byte t;
        fetch_effective_address();
        t = (byte) (~RM.apply(EA.lowWord));
        CLR_NZV();
        SET_NZ8(t);
        SEC();
        WM.accept(EA.lowWord, t);
    }

    void lsr_ix() {
        byte t;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        t >>= 1;
        SET_Z8(t);
        WM.accept(EA.lowWord, t);
    }

    void ror_ix() {
        byte t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (byte) ((CC & CC_C) << 7);
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        r |= (byte) (t >> 1);
        SET_NZ8(r);
        WM.accept(EA.lowWord, r);
    }

    void asr_ix() {
        byte t;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        t = (byte) ((t & 0x80) | (t >> 1));
        SET_NZ8(t);
        WM.accept(EA.lowWord, t);
    }

    void asl_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void rol_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (CC & CC_C);
        r |= (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void dec_ix() {
        byte t;
        fetch_effective_address();
        t = (byte) (RM.apply(EA.lowWord) - 1);
        CLR_NZV();
        SET_FLAGS8D(t);
        WM.accept(EA.lowWord, t);
    }

    void inc_ix() {
        byte t;
        fetch_effective_address();
        t = (byte) (RM.apply(EA.lowWord) + 1);
        CLR_NZV();
        SET_FLAGS8I(t);
        WM.accept(EA.lowWord, t);
    }

    void tst_ix() {
        byte t;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(t);
    }

    void jmp_ix() {
        fetch_effective_address();
        PC.d = EA.d;
        //CHANGE_PC;
    }

    void clr_ix() {
        fetch_effective_address();
        RM.apply(EA.lowWord);
        WM.accept(EA.lowWord, (byte) 0);
        CLR_NZVC();
        SEZ();
    }

    void neg_ex() {
        short r, t;
        t = EXTBYTE();
        r = (short) (-t);
        CLR_NZVC();
        SET_FLAGS8((byte) 0, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void com_ex() {
        byte t;
        t = EXTBYTE();
        t = (byte) (~t);
        CLR_NZV();
        SET_NZ8(t);
        SEC();
        WM.accept(EA.lowWord, t);
    }

    void lsr_ex() {
        byte t;
        t = EXTBYTE();
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        t >>= 1;
        SET_Z8(t);
        WM.accept(EA.lowWord, t);
    }

    void ror_ex() {
        byte t, r;
        t = EXTBYTE();
        r = (byte) ((CC & CC_C) << 7);
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        r |= (byte) (t >> 1);
        SET_NZ8(r);
        WM.accept(EA.lowWord, r);
    }

    void asr_ex() {
        byte t;
        t = EXTBYTE();
        CLR_NZC();
        CC |= (byte) (t & CC_C);
        t = (byte) ((t & 0x80) | (t >> 1));
        SET_NZ8(t);
        WM.accept(EA.lowWord, t);
    }

    void asl_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void rol_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) ((CC & CC_C) | (t << 1));
        CLR_NZVC();
        SET_FLAGS8((byte) t, t, r);
        WM.accept(EA.lowWord, (byte) r);
    }

    void dec_ex() {
        byte t;
        t = EXTBYTE();
        --t;
        CLR_NZV();
        SET_FLAGS8D(t);
        WM.accept(EA.lowWord, t);
    }

    void inc_ex() {
        byte t;
        t = EXTBYTE();
        ++t;
        CLR_NZV();
        SET_FLAGS8I(t);
        WM.accept(EA.lowWord, t);
    }

    void tst_ex() {
        byte t;
        t = EXTBYTE();
        CLR_NZV();
        SET_NZ8(t);
    }

    void jmp_ex() {
        EXTENDED();
        PC.d = EA.d;
        //CHANGE_PC;
    }

    void clr_ex() {
        EXTENDED();
        RM.apply(EA.lowWord);
        WM.accept(EA.lowWord, (byte) 0);
        CLR_NZVC();
        SEZ();
    }

    void suba_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void cmpa_im() {
        short t, r;
        int i1, i2, i3;
        t = IMMBYTE();
        r = (short) (D.highByte - t);
        i1 = CC;
        CLR_NZVC();
        i2 = CC;
        SET_FLAGS8(D.highByte, t, r);
        i3 = CC;
    }

    void sbca_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void subd_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void cmpd_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpu_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = U.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void anda_im() {
        byte t;
        t = IMMBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void bita_im() {
        byte t, r;
        t = IMMBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    void lda_im() {
        D.highByte = IMMBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void sta_im() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        IMM8();
        WM.accept(EA.lowWord, D.highByte);
    }

    void eora_im() {
        byte t;
        t = IMMBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adca_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void ora_im() {
        byte t;
        t = IMMBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adda_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void cmpx_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpy_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = Y.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmps_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = S.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void bsr() {
        byte t;
        t = IMMBYTE();
        PUSHWORD(PC);
        PC.lowWord += SIGNED(t);
        //CHANGE_PC;
    }

    void ldx_im() {
        X = IMMWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    void ldy_im() {
        Y = IMMWORD();
        CLR_NZV();
        SET_NZ16(Y.lowWord);
    }

    void stx_im() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        IMM16();
        WM16(EA.lowWord, X);
    }

    void sty_im() {
        CLR_NZV();
        SET_NZ16(Y.lowWord);
        IMM16();
        WM16(EA.lowWord, Y);
    }

    void suba_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void cmpa_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    void sbca_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void subd_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void cmpd_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpu_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = U.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(U.lowWord, (short) b.d, r);
    }

    void anda_di() {
        byte t;
        t = DIRBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void bita_di() {
        byte t, r;
        t = DIRBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    void lda_di() {
        D.highByte = DIRBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void sta_di() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        DIRECT();
        WM.accept(EA.lowWord, D.highByte);
    }

    void eora_di() {
        byte t;
        t = DIRBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adca_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void ora_di() {
        byte t;
        t = DIRBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adda_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void cmpx_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpy_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = Y.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmps_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = S.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void jsr_di() {
        DIRECT();
        PUSHWORD(PC);
        PC.d = EA.d;
        //CHANGE_PC;
    }

    void ldx_di() {
        X = DIRWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    void ldy_di() {
        Y = DIRWORD();
        CLR_NZV();
        SET_NZ16(Y.lowWord);
    }

    void stx_di() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        DIRECT();
        WM16(EA.lowWord, X);
    }

    void sty_di() {
        CLR_NZV();
        SET_NZ16(Y.lowWord);
        DIRECT();
        WM16(EA.lowWord, Y);
    }

    void suba_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void cmpa_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    void sbca_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.highByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void subd_ix() {
        int r, d;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void cmpd_ix() {
        int r, d;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpu_ix() {
        int r;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        r = U.lowWord - b.d;
        CLR_NZVC();
        SET_FLAGS16(U.lowWord, b.lowWord, r);
    }

    void anda_ix() {
        fetch_effective_address();
        D.highByte &= RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void bita_ix() {
        byte r;
        fetch_effective_address();
        r = (byte) (D.highByte & RM.apply(EA.lowWord));
        CLR_NZV();
        SET_NZ8(r);
    }

    void lda_ix() {
        fetch_effective_address();
        D.highByte = RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void sta_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ8(D.highByte);
        WM.accept(EA.lowWord, D.highByte);
    }

    void eora_ix() {
        fetch_effective_address();
        D.highByte ^= RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adca_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.highByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void ora_ix() {
        fetch_effective_address();
        D.highByte |= RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adda_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void cmpx_ix() {
        int r, d;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpy_ix() {
        int r, d;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        d = Y.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmps_ix() {
        int r, d;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        d = S.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void jsr_ix() {
        fetch_effective_address();
        PUSHWORD(PC);
        PC.d = EA.d;
        //CHANGE_PC;
    }

    void ldx_ix() {
        fetch_effective_address();
        X.lowWord = RM16(EA.lowWord);
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    void ldy_ix() {
        fetch_effective_address();
        Y.lowWord = RM16(EA.lowWord);
        CLR_NZV();
        SET_NZ16(Y.lowWord);
    }

    void stx_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ16(X.lowWord);
        WM16(EA.lowWord, X);
    }

    void sty_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ16(Y.lowWord);
        WM16(EA.lowWord, Y);
    }

    void suba_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void cmpa_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    void sbca_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    void subd_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void cmpd_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpu_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = U.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void anda_ex() {
        byte t;
        t = EXTBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void bita_ex() {
        byte t, r;
        t = EXTBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    void lda_ex() {
        D.highByte = EXTBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void sta_ex() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        EXTENDED();
        WM.accept(EA.lowWord, D.highByte);
    }

    void eora_ex() {
        byte t;
        t = EXTBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adca_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void ora_ex() {
        byte t;
        t = EXTBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    void adda_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, (byte) t, (byte) r);
        D.highByte = (byte) r;
    }

    void cmpx_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmpy_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = Y.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void cmps_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = S.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
    }

    void jsr_ex() {
        EXTENDED();
        PUSHWORD(PC);
        PC.d = EA.d;
        //CHANGE_PC;
    }

    void ldx_ex() {
        X = EXTWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    void ldy_ex() {
        Y = EXTWORD();
        CLR_NZV();
        SET_NZ16(Y.lowWord);
    }

    void stx_ex() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        EXTENDED();
        WM16(EA.lowWord, X);
    }

    void sty_ex() {
        CLR_NZV();
        SET_NZ16(Y.lowWord);
        EXTENDED();
        WM16(EA.lowWord, Y);
    }

    void subb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void cmpb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    void sbcb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void addd_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void andb_im() {
        byte t;
        t = IMMBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void bitb_im() {
        byte t, r;
        t = IMMBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    void ldb_im() {
        D.lowByte = IMMBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void stb_im() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        IMM8();
        WM.accept(EA.lowWord, D.lowByte);
    }

    void eorb_im() {
        byte t;
        t = IMMBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void adcb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void orb_im() {
        byte t;
        t = IMMBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void addb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void ldd_im() {
        D = IMMWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    void std_im() {
        CLR_NZV();
        SET_NZ16(D.lowWord);
        IMM16();
        WM16(EA.lowWord, D);
    }

    void ldu_im() {
        U = IMMWORD();
        CLR_NZV();
        SET_NZ16(U.lowWord);
    }

    void lds_im() {
        S = IMMWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
        int_state |= M6809_LDS;
    }

    void stu_im() {
        CLR_NZV();
        SET_NZ16(U.lowWord);
        IMM16();
        WM16(EA.lowWord, U);
    }

    void sts_im() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        IMM16();
        WM16(EA.lowWord, S);
    }

    void subb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void cmpb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    void sbcb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void addd_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void andb_di() {
        byte t;
        t = DIRBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void bitb_di() {
        byte t, r;
        t = DIRBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    void ldb_di() {
        D.lowByte = DIRBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void stb_di() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        DIRECT();
        WM.accept(EA.lowWord, D.lowByte);
    }

    void eorb_di() {
        byte t;
        t = DIRBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void adcb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void orb_di() {
        byte t;
        t = DIRBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void addb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void ldd_di() {
        D = DIRWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    void std_di() {
        CLR_NZV();
        SET_NZ16(D.lowWord);
        DIRECT();
        WM16(EA.lowWord, D);
    }

    void ldu_di() {
        U = DIRWORD();
        CLR_NZV();
        SET_NZ16(U.lowWord);
    }

    void lds_di() {
        S = DIRWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
        int_state |= M6809_LDS;
    }

    void stu_di() {
        CLR_NZV();
        SET_NZ16(U.lowWord);
        DIRECT();
        WM16(EA.lowWord, U);
    }

    void sts_di() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        DIRECT();
        WM16(EA.lowWord, S);
    }

    void subb_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void cmpb_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    void sbcb_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.lowByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void addd_ix() {
        int r, d;
        Register b = new Register();
        fetch_effective_address();
        b.d = RM16(EA.lowWord);
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void andb_ix() {
        fetch_effective_address();
        D.lowByte &= RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void bitb_ix() {
        byte r;
        fetch_effective_address();
        r = (byte) (D.lowByte & RM.apply(EA.lowWord));
        CLR_NZV();
        SET_NZ8(r);
    }

    void ldb_ix() {
        fetch_effective_address();
        D.lowByte = RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void stb_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ8(D.lowByte);
        WM.accept(EA.lowWord, D.lowByte);
    }

    void eorb_ix() {
        fetch_effective_address();
        D.lowByte ^= RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void adcb_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.lowByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void orb_ix() {
        fetch_effective_address();
        D.lowByte |= RM.apply(EA.lowWord);
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void addb_ix() {
        short t, r;
        fetch_effective_address();
        t = RM.apply(EA.lowWord);
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void ldd_ix() {
        fetch_effective_address();
        D.lowWord = RM16(EA.lowWord);
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    void std_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ16(D.lowWord);
        WM16(EA.lowWord, D);
    }

    void ldu_ix() {
        fetch_effective_address();
        U.lowWord = RM16(EA.lowWord);
        CLR_NZV();
        SET_NZ16(U.lowWord);
    }

    void lds_ix() {
        fetch_effective_address();
        S.lowWord = RM16(EA.lowWord);
        CLR_NZV();
        SET_NZ16(S.lowWord);
        int_state |= M6809_LDS;
    }

    void stu_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ16(U.lowWord);
        WM16(EA.lowWord, U);
    }

    void sts_ix() {
        fetch_effective_address();
        CLR_NZV();
        SET_NZ16(S.lowWord);
        WM16(EA.lowWord, S);
    }

    void subb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void cmpb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    void sbcb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte - t - (CC & CC_C));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    void addd_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16((short) d, (short) b.d, r);
        D.lowWord = (short) r;
    }

    void andb_ex() {
        byte t;
        t = EXTBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void bitb_ex() {
        byte t, r;
        t = EXTBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    void ldb_ex() {
        D.lowByte = EXTBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void stb_ex() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        EXTENDED();
        WM.accept(EA.lowWord, D.lowByte);
    }

    void eorb_ex() {
        byte t;
        t = EXTBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void adcb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte + t + (CC & CC_C));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void orb_ex() {
        byte t;
        t = EXTBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    void addb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, (byte) t, (byte) r);
        D.lowByte = (byte) r;
    }

    void ldd_ex() {
        D = EXTWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    void std_ex() {
        CLR_NZV();
        SET_NZ16(D.lowWord);
        EXTENDED();
        WM16(EA.lowWord, D);
    }

    void ldu_ex() {
        U = EXTWORD();
        CLR_NZV();
        SET_NZ16(U.lowWord);
    }

    void lds_ex() {
        S = EXTWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
        int_state |= M6809_LDS;
    }

    void stu_ex() {
        CLR_NZV();
        SET_NZ16(U.lowWord);
        EXTENDED();
        WM16(EA.lowWord, U);
    }

    void sts_ex() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        EXTENDED();
        WM16(EA.lowWord, S);
    }

    void pref10() {
        byte ireg2 = ReadOp.apply(PC.lowWord);
        PC.lowWord++;
        switch (ireg2 & 0xff) {
            case 0x21:
                lbrn();
                pendingCycles -= 5;
                break;
            case 0x22:
                lbhi();
                pendingCycles -= 5;
                break;
            case 0x23:
                lbls();
                pendingCycles -= 5;
                break;
            case 0x24:
                lbcc();
                pendingCycles -= 5;
                break;
            case 0x25:
                lbcs();
                pendingCycles -= 5;
                break;
            case 0x26:
                lbne();
                pendingCycles -= 5;
                break;
            case 0x27:
                lbeq();
                pendingCycles -= 5;
                break;
            case 0x28:
                lbvc();
                pendingCycles -= 5;
                break;
            case 0x29:
                lbvs();
                pendingCycles -= 5;
                break;
            case 0x2a:
                lbpl();
                pendingCycles -= 5;
                break;
            case 0x2b:
                lbmi();
                pendingCycles -= 5;
                break;
            case 0x2c:
                lbge();
                pendingCycles -= 5;
                break;
            case 0x2d:
                lblt();
                pendingCycles -= 5;
                break;
            case 0x2e:
                lbgt();
                pendingCycles -= 5;
                break;
            case 0x2f:
                lble();
                pendingCycles -= 5;
                break;

            case 0x3f:
                swi2();
                pendingCycles -= 20;
                break;

            case 0x83:
                cmpd_im();
                pendingCycles -= 5;
                break;
            case 0x8c:
                cmpy_im();
                pendingCycles -= 5;
                break;
            case 0x8e:
                ldy_im();
                pendingCycles -= 4;
                break;
            case 0x8f:
                sty_im();
                pendingCycles -= 4;
                break;

            case 0x93:
                cmpd_di();
                pendingCycles -= 7;
                break;
            case 0x9c:
                cmpy_di();
                pendingCycles -= 7;
                break;
            case 0x9e:
                ldy_di();
                pendingCycles -= 6;
                break;
            case 0x9f:
                sty_di();
                pendingCycles -= 6;
                break;

            case 0xa3:
                cmpd_ix();
                pendingCycles -= 7;
                break;
            case 0xac:
                cmpy_ix();
                pendingCycles -= 7;
                break;
            case 0xae:
                ldy_ix();
                pendingCycles -= 6;
                break;
            case 0xaf:
                sty_ix();
                pendingCycles -= 6;
                break;

            case 0xb3:
                cmpd_ex();
                pendingCycles -= 8;
                break;
            case 0xbc:
                cmpy_ex();
                pendingCycles -= 8;
                break;
            case 0xbe:
                ldy_ex();
                pendingCycles -= 7;
                break;
            case 0xbf:
                sty_ex();
                pendingCycles -= 7;
                break;

            case 0xce:
                lds_im();
                pendingCycles -= 4;
                break;
            case 0xcf:
                sts_im();
                pendingCycles -= 4;
                break;

            case 0xde:
                lds_di();
                pendingCycles -= 6;
                break;
            case 0xdf:
                sts_di();
                pendingCycles -= 6;
                break;

            case 0xee:
                lds_ix();
                pendingCycles -= 6;
                break;
            case 0xef:
                sts_ix();
                pendingCycles -= 6;
                break;

            case 0xfe:
                lds_ex();
                pendingCycles -= 7;
                break;
            case 0xff:
                sts_ex();
                pendingCycles -= 7;
                break;

            default:
                illegal();
                break;
        }
    }

    void pref11() {
        byte ireg2 = ReadOp.apply(PC.lowWord);
        PC.lowWord++;
        switch (ireg2 & 0xff) {
            case 0x3f:
                swi3();
                pendingCycles -= 20;
                break;

            case 0x83:
                cmpu_im();
                pendingCycles -= 5;
                break;
            case 0x8c:
                cmps_im();
                pendingCycles -= 5;
                break;

            case 0x93:
                cmpu_di();
                pendingCycles -= 7;
                break;
            case 0x9c:
                cmps_di();
                pendingCycles -= 7;
                break;

            case 0xa3:
                cmpu_ix();
                pendingCycles -= 7;
                break;
            case 0xac:
                cmps_ix();
                pendingCycles -= 7;
                break;

            case 0xb3:
                cmpu_ex();
                pendingCycles -= 8;
                break;
            case 0xbc:
                cmps_ex();
                pendingCycles -= 8;
                break;

            default:
                illegal();
                break;
        }
    }

//#endregion

//#region Disassenbler

    public static class opcodeinfo {

        public final byte opcode;
        public final int length;
        public final String name;
        public final m6809_addressing_modes mode;
        public int flags;

        public opcodeinfo(byte _opcode, int _length, String _name, m6809_addressing_modes _mode) {
            opcode = _opcode;
            length = _length;
            name = _name;
            mode = _mode;
        }

        public opcodeinfo(byte _opcode, int _length, String _name, m6809_addressing_modes _mode, int _flags) {
            opcode = _opcode;
            length = _length;
            name = _name;
            mode = _mode;
            flags = _flags;
        }
    }

    public static final int DASMFLAG_STEP_OVER = 0x2000_0000;
    public static int DASMFLAG_SUPPORTED = 0x8000_0000;

    public enum m6809_addressing_modes {
        INH,  // Inherent
        DIR,  // Direct
        IND,  // Indexed
        REL,  // Relative (8 bit)
        LREL,  // Long relative (16 bit)
        EXT,  // Extended
        IMM,  // Immediate
        IMM_RR,  // Register-to-register
        PG1,  // Switch to page 1 opcodes
        PG2  // Switch to page 2 opcodes
    }

    public final opcodeinfo[] m6809_pg0opcodes = new opcodeinfo[] {
            new opcodeinfo((byte) 0x00, 2, "NEG", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x03, 2, "COM", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x04, 2, "LSR", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x06, 2, "ROR", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x07, 2, "ASR", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x08, 2, "ASL", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x09, 2, "ROL", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x0A, 2, "DEC", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x0C, 2, "INC", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x0D, 2, "TST", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x0E, 2, "JMP", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x0F, 2, "CLR", m6809_addressing_modes.DIR),

            new opcodeinfo((byte) 0x10, 1, "page1", m6809_addressing_modes.PG1),
            new opcodeinfo((byte) 0x11, 1, "page2", m6809_addressing_modes.PG2),
            new opcodeinfo((byte) 0x12, 1, "NOP", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x13, 1, "SYNC", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x16, 3, "LBRA", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x17, 3, "LBSR", m6809_addressing_modes.LREL, DASMFLAG_STEP_OVER),
            new opcodeinfo((byte) 0x19, 1, "DAA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x1A, 2, "ORCC", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x1C, 2, "ANDCC", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x1D, 1, "SEX", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x1E, 2, "EXG", m6809_addressing_modes.IMM_RR),
            new opcodeinfo((byte) 0x1F, 2, "TFR", m6809_addressing_modes.IMM_RR),

            new opcodeinfo((byte) 0x20, 2, "BRA", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x21, 2, "BRN", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x22, 2, "BHI", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x23, 2, "BLS", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x24, 2, "BCC", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x25, 2, "BCS", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x26, 2, "BNE", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x27, 2, "BEQ", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x28, 2, "BVC", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x29, 2, "BVS", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x2A, 2, "BPL", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x2B, 2, "BMI", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x2C, 2, "BGE", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x2D, 2, "BLT", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x2E, 2, "BGT", m6809_addressing_modes.REL),
            new opcodeinfo((byte) 0x2F, 2, "BLE", m6809_addressing_modes.REL),

            new opcodeinfo((byte) 0x30, 2, "LEAX", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x31, 2, "LEAY", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x32, 2, "LEAS", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x33, 2, "LEAU", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x34, 2, "PSHS", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x35, 2, "PULS", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x36, 2, "PSHU", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x37, 2, "PULU", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x39, 1, "RTS", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x3A, 1, "ABX", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x3B, 1, "RTI", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x3C, 2, "CWAI", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x3D, 1, "MUL", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x3F, 1, "SWI", m6809_addressing_modes.INH),

            new opcodeinfo((byte) 0x40, 1, "NEGA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x43, 1, "COMA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x44, 1, "LSRA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x46, 1, "RORA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x47, 1, "ASRA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x48, 1, "ASLA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x49, 1, "ROLA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x4A, 1, "DECA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x4C, 1, "INCA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x4D, 1, "TSTA", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x4F, 1, "CLRA", m6809_addressing_modes.INH),

            new opcodeinfo((byte) 0x50, 1, "NEGB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x53, 1, "COMB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x54, 1, "LSRB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x56, 1, "RORB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x57, 1, "ASRB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x58, 1, "ASLB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x59, 1, "ROLB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x5A, 1, "DECB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x5C, 1, "INCB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x5D, 1, "TSTB", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x5F, 1, "CLRB", m6809_addressing_modes.INH),

            new opcodeinfo((byte) 0x60, 2, "NEG", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x63, 2, "COM", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x64, 2, "LSR", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x66, 2, "ROR", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x67, 2, "ASR", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x68, 2, "ASL", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x69, 2, "ROL", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x6A, 2, "DEC", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x6C, 2, "INC", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x6D, 2, "TST", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x6E, 2, "JMP", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0x6F, 2, "CLR", m6809_addressing_modes.IND),

            new opcodeinfo((byte) 0x70, 3, "NEG", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x73, 3, "COM", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x74, 3, "LSR", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x76, 3, "ROR", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x77, 3, "ASR", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x78, 3, "ASL", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x79, 3, "ROL", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x7A, 3, "DEC", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x7C, 3, "INC", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x7D, 3, "TST", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x7E, 3, "JMP", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0x7F, 3, "CLR", m6809_addressing_modes.EXT),

            new opcodeinfo((byte) 0x80, 2, "SUBA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x81, 2, "CMPA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x82, 2, "SBCA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x83, 3, "SUBD", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x84, 2, "ANDA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x85, 2, "BITA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x86, 2, "LDA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x88, 2, "EORA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x89, 2, "ADCA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8A, 2, "ORA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8B, 2, "ADDA", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8C, 3, "CMPX", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8D, 2, "BSR", m6809_addressing_modes.REL, DASMFLAG_STEP_OVER),
            new opcodeinfo((byte) 0x8E, 3, "LDX", m6809_addressing_modes.IMM),

            new opcodeinfo((byte) 0x90, 2, "SUBA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x91, 2, "CMPA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x92, 2, "SBCA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x93, 2, "SUBD", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x94, 2, "ANDA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x95, 2, "BITA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x96, 2, "LDA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x97, 2, "STA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x98, 2, "EORA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x99, 2, "ADCA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9A, 2, "ORA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9B, 2, "ADDA", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9C, 2, "CMPX", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9D, 2, "JSR", m6809_addressing_modes.DIR, DASMFLAG_STEP_OVER),
            new opcodeinfo((byte) 0x9E, 2, "LDX", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9F, 2, "STX", m6809_addressing_modes.DIR),

            new opcodeinfo((byte) 0xA0, 2, "SUBA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA1, 2, "CMPA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA2, 2, "SBCA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA3, 2, "SUBD", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA4, 2, "ANDA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA5, 2, "BITA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA6, 2, "LDA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA7, 2, "STA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA8, 2, "EORA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xA9, 2, "ADCA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAA, 2, "ORA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAB, 2, "ADDA", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAC, 2, "CMPX", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAD, 2, "JSR", m6809_addressing_modes.IND, DASMFLAG_STEP_OVER),
            new opcodeinfo((byte) 0xAE, 2, "LDX", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAF, 2, "STX", m6809_addressing_modes.IND),

            new opcodeinfo((byte) 0xB0, 3, "SUBA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB1, 3, "CMPA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB2, 3, "SBCA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB3, 3, "SUBD", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB4, 3, "ANDA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB5, 3, "BITA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB6, 3, "LDA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB7, 3, "STA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB8, 3, "EORA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xB9, 3, "ADCA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBA, 3, "ORA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBB, 3, "ADDA", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBC, 3, "CMPX", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBD, 3, "JSR", m6809_addressing_modes.EXT, DASMFLAG_STEP_OVER),
            new opcodeinfo((byte) 0xBE, 3, "LDX", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBF, 3, "STX", m6809_addressing_modes.EXT),

            new opcodeinfo((byte) 0xC0, 2, "SUBB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC1, 2, "CMPB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC2, 2, "SBCB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC3, 3, "ADDD", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC4, 2, "ANDB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC5, 2, "BITB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC6, 2, "LDB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC8, 2, "EORB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xC9, 2, "ADCB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xCA, 2, "ORB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xCB, 2, "ADDB", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xCC, 3, "LDD", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xCE, 3, "LDU", m6809_addressing_modes.IMM),

            new opcodeinfo((byte) 0xD0, 2, "SUBB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD1, 2, "CMPB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD2, 2, "SBCB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD3, 2, "ADDD", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD4, 2, "ANDB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD5, 2, "BITB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD6, 2, "LDB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD7, 2, "STB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD8, 2, "EORB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xD9, 2, "ADCB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDA, 2, "ORB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDB, 2, "ADDB", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDC, 2, "LDD", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDD, 2, "STD", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDE, 2, "LDU", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDF, 2, "STU", m6809_addressing_modes.DIR),

            new opcodeinfo((byte) 0xE0, 2, "SUBB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE1, 2, "CMPB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE2, 2, "SBCB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE3, 2, "ADDD", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE4, 2, "ANDB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE5, 2, "BITB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE6, 2, "LDB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE7, 2, "STB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE8, 2, "EORB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xE9, 2, "ADCB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xEA, 2, "ORB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xEB, 2, "ADDB", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xEC, 2, "LDD", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xED, 2, "STD", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xEE, 2, "LDU", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xEF, 2, "STU", m6809_addressing_modes.IND),

            new opcodeinfo((byte) 0xF0, 3, "SUBB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF1, 3, "CMPB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF2, 3, "SBCB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF3, 3, "ADDD", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF4, 3, "ANDB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF5, 3, "BITB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF6, 3, "LDB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF7, 3, "STB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF8, 3, "EORB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xF9, 3, "ADCB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFA, 3, "ORB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFB, 3, "ADDB", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFC, 3, "LDD", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFD, 3, "STD", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFE, 3, "LDU", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFF, 3, "STU", m6809_addressing_modes.EXT),
    };
    public final opcodeinfo[] m6809_pg1opcodes = new opcodeinfo[] {
            new opcodeinfo((byte) 0x21, 4, "LBRN", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x22, 4, "LBHI", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x23, 4, "LBLS", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x24, 4, "LBCC", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x25, 4, "LBCS", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x26, 4, "LBNE", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x27, 4, "LBEQ", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x28, 4, "LBVC", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x29, 4, "LBVS", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x2A, 4, "LBPL", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x2B, 4, "LBMI", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x2C, 4, "LBGE", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x2D, 4, "LBLT", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x2E, 4, "LBGT", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x2F, 4, "LBLE", m6809_addressing_modes.LREL),
            new opcodeinfo((byte) 0x3F, 2, "SWI2", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x83, 4, "CMPD", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8C, 4, "CMPY", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8E, 4, "LDY", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x93, 3, "CMPD", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9C, 3, "CMPY", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9E, 3, "LDY", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9F, 3, "STY", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xA3, 3, "CMPD", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAC, 3, "CMPY", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAE, 3, "LDY", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAF, 3, "STY", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xB3, 4, "CMPD", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBC, 4, "CMPY", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBE, 4, "LDY", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBF, 4, "STY", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xCE, 4, "LDS", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0xDE, 3, "LDS", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xDF, 3, "STS", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xEE, 3, "LDS", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xEF, 3, "STS", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xFE, 4, "LDS", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xFF, 4, "STS", m6809_addressing_modes.EXT)
    };
    public final opcodeinfo[] m6809_pg2opcodes = new opcodeinfo[] {
            new opcodeinfo((byte) 0x3F, 2, "SWI3", m6809_addressing_modes.INH),
            new opcodeinfo((byte) 0x83, 4, "CMPU", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x8C, 4, "CMPS", m6809_addressing_modes.IMM),
            new opcodeinfo((byte) 0x93, 3, "CMPU", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0x9C, 3, "CMPS", m6809_addressing_modes.DIR),
            new opcodeinfo((byte) 0xA3, 3, "CMPU", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xAC, 3, "CMPS", m6809_addressing_modes.IND),
            new opcodeinfo((byte) 0xB3, 4, "CMPU", m6809_addressing_modes.EXT),
            new opcodeinfo((byte) 0xBC, 4, "CMPS", m6809_addressing_modes.EXT)
    };
    public opcodeinfo[][] m6809_pgpointers;
    public int[] m6809_numops;
    public static final String[] m6809_regs = new String[] {"X", "Y", "U", "S", "PC"};
    public static final String[] m6809_regs_te = new String[] {
            "D", "X", "Y", "U", "S", "PC", "inv", "inv",
            "A", "B", "CC", "DP", "inv", "inv", "inv", "inv"
    };
    public byte op;

    public void DisassemblerInit() {
        m6809_pgpointers = new opcodeinfo[][] {
                m6809_pg0opcodes, m6809_pg1opcodes, m6809_pg2opcodes
        };
        m6809_numops = new int[] {
                m6809_pg0opcodes.length, m6809_pg1opcodes.length, m6809_pg2opcodes.length
        };
    }

    public String m6809_dasm(int ppc) {
        String buffer = "";

        byte opcode, pb, pbm, reg;
        m6809_addressing_modes mode;
        byte[] operandarray;
        int ea;
        int flags;
        int numoperands, offset;
        int i, j, i1, page = 0;
        short p = (short) ppc;
        if (ppc == 0xc010) {
            i1 = 1;
        }
        buffer = "%2x".formatted(ReadOp.apply(p));
        boolean indirect, opcode_found = false;
        do {
            opcode = ReadOp.apply(p);
            p++;
            for (i = 0; i < m6809_numops[page]; i++)
                if (m6809_pgpointers[page][i].opcode == opcode)
                    break;
            if (i < m6809_numops[page])
                opcode_found = true;
            else {
                buffer += " Illegal Opcode";
                return buffer;
            }
            if (m6809_pgpointers[page][i].mode.ordinal() >= m6809_addressing_modes.PG1.ordinal()) {
                page = m6809_pgpointers[page][i].mode.ordinal() - m6809_addressing_modes.PG1.ordinal() + 1;
                opcode_found = false;
            }
        } while (!opcode_found);
        if (page == 0)
            numoperands = m6809_pgpointers[page][i].length - 1;
        else
            numoperands = m6809_pgpointers[page][i].length - 2;
        operandarray = new byte[numoperands];
        for (j = 0; j < numoperands; j++) {
            operandarray[j] = ReadOpArg.apply((short) (p + j));
        }
        p += (short) numoperands;
        ppc += numoperands;
        mode = m6809_pgpointers[page][i].mode;
        flags = m6809_pgpointers[page][i].flags;

        buffer += "%-6s".formatted(m6809_pgpointers[page][i].name);
        switch (mode) {
            case INH:
                switch (opcode) {
                    case 0x34:  // PSHS
                    case 0x36:  // PSHU
                        pb = operandarray[0];
                        if ((pb & 0x80) != 0)
                            buffer += "PC";
                        if ((pb & 0x40) != 0)
                            buffer += (((pb & 0x80) != 0) ? "," : "") + ((opcode == 0x34) ? "U" : "S");
                        if ((pb & 0x20) != 0)
                            buffer += (((pb & 0xc0) != 0) ? "," : "") + "Y";
                        if ((pb & 0x10) != 0)
                            buffer += (((pb & 0xe0) != 0) ? "," : "") + "X";
                        if ((pb & 0x08) != 0)
                            buffer += (((pb & 0xf0) != 0) ? "," : "") + "DP";
                        if ((pb & 0x04) != 0)
                            buffer += (((pb & 0xf8) != 0) ? "," : "") + "B";
                        if ((pb & 0x02) != 0)
                            buffer += (((pb & 0xfc) != 0) ? "," : "") + "A";
                        if ((pb & 0x01) != 0)
                            buffer += (((pb & 0xfe) != 0) ? "," : "") + "CC";
                        break;
                    case 0x35:  // PULS
                    case 0x37:  // PULU
                        pb = operandarray[0];
                        if ((pb & 0x01) != 0)
                            buffer += "CC";
                        if ((pb & 0x02) != 0)
                            buffer += (((pb & 0x01) != 0) ? "," : "") + "A";
                        if ((pb & 0x04) != 0)
                            buffer += (((pb & 0x03) != 0) ? "," : "") + "B";
                        if ((pb & 0x08) != 0)
                            buffer += (((pb & 0x07) != 0) ? "," : "") + "DP";
                        if ((pb & 0x10) != 0)
                            buffer += (((pb & 0x0f) != 0) ? "," : "") + "X";
                        if ((pb & 0x20) != 0)
                            buffer += (((pb & 0x1f) != 0) ? "," : "") + "Y";
                        if ((pb & 0x40) != 0)
                            buffer += (((pb & 0x3f) != 0) ? "," : "") + ((opcode == 0x35) ? "U" : "S");
                        if ((pb & 0x80) != 0)
                            buffer += (((pb & 0x7f) != 0) ? "," : "") + "PC ; (PUL? PC=RTS)";
                        break;
                    default:
                        // No operands
                        break;
                }
                break;

            case DIR:
                ea = operandarray[0];
                buffer += "%2x".formatted(ea);
                break;

            case REL:
                offset = operandarray[0];
                buffer += "%4x".formatted((ppc + offset) & 0xffff);
                break;

            case LREL:
                offset = (short) ((operandarray[0] << 8) + operandarray[1]);
                buffer += "%4x".formatted((ppc + offset) & 0xffff);
                break;

            case EXT:
                ea = ((operandarray[0] << 8) + operandarray[1]);
                buffer += "%2x".formatted(ea);
                break;

            case IND:
                pb = operandarray[0];
                reg = (byte) ((pb >> 5) & 3);
                pbm = (byte) (pb & 0x8f);
                indirect = (pb & 0x90) == 0x90;
                // open brackets if indirect
                if (indirect && pbm != (byte) 0x80 && pbm != (byte) 0x82)
                    buffer += "[";
                switch (pbm & 0xff) {
                    case 0x80:  // ,R+
                        if (indirect)
                            buffer = "Illegal Postbyte";
                        else
                            buffer += "," + m6809_regs[reg] + "+";
                        break;

                    case 0x81:  // ,R++
                        buffer += "," + m6809_regs[reg] + "++";
                        break;

                    case 0x82:  // ,-R
                        if (indirect)
                            buffer = "Illegal Postbyte";
                        else
                            buffer += ",-" + m6809_regs[reg];
                        break;

                    case 0x83:  // ,--R
                        buffer += ",--" + m6809_regs[reg];
                        break;

                    case 0x84:  // ,R
                        buffer += "," + m6809_regs[reg];
                        break;

                    case 0x85:  // (+/- B),R
                        buffer += "B," + m6809_regs[reg];
                        break;

                    case 0x86:  // (+/- A),R
                        buffer += "A," + m6809_regs[reg];
                        break;

                    case 0x87:
                        buffer = "Illegal Postbyte";
                        break;

                    case 0x88:  // (+/- 7 bit offset),R
                        offset = ReadOpArg.apply(p);
                        p++;
                        buffer += ((offset < 0) ? "-" : "");
                        buffer += "%2x".formatted((offset < 0) ? -offset : offset);
                        buffer += m6809_regs[reg];
                        break;

                    case 0x89:  // (+/- 15 bit offset),R
                        offset = (short) ((ReadOpArg.apply(p) << 8) + ReadOpArg.apply((short) (p + 1)));
                        p += 2;
                        buffer += ((offset < 0) ? "-" : "");
                        buffer += "%2x".formatted((offset < 0) ? -offset : offset);
                        buffer += m6809_regs[reg];
                        break;

                    case 0x8a:
                        buffer = "Illegal Postbyte";
                        break;

                    case 0x8b:  // (+/- D),R
                        buffer += "D," + m6809_regs[reg];
                        break;

                    case 0x8c:  // (+/- 7 bit offset),PC
                        offset = ReadOpArg.apply(p);
                        p++;
                        buffer += ((offset < 0) ? "-" : "");
                        buffer += "$" + "%2x".formatted((offset < 0) ? -offset : offset) + ",PC";
                        break;

                    case 0x8d:  // (+/- 15 bit offset),PC
                        offset = (short) ((ReadOpArg.apply(p) << 8) + ReadOpArg.apply((short) (p + 1)));
                        p += 2;
                        buffer += ((offset < 0) ? "-" : "");
                        buffer += "$" + "%4x".formatted((offset < 0) ? -offset : offset) + ",PC";
                        break;

                    case 0x8e:
                        buffer = "Illegal Postbyte";
                        break;

                    case 0x8f:  // address
                        ea = (short) ((ReadOpArg.apply(p) << 8) + ReadOpArg.apply((short) (p + 1)));
                        p += 2;
                        buffer += "$" + "%4x".formatted(ea);
                        break;

                    default:  // (+/- 4 bit offset),R
                        offset = pb & 0x1f;
                        if (offset > 15)
                            offset = offset - 32;
                        buffer += ((offset < 0) ? "-" : "");
                        buffer += "$" + "%x".formatted((offset < 0) ? -offset : offset) + ",";
                        buffer += m6809_regs[reg];
                        break;
                }

                // close brackets if indirect
                if (indirect && pbm != 0x80 && pbm != 0x82)
                    buffer += "]";
                break;

            case IMM:
                if (numoperands == 2) {
                    ea = (operandarray[0] << 8) + operandarray[1];
                    buffer += "#$" + "%4x".formatted(ea);
                } else if (numoperands == 1) {
                    ea = operandarray[0];
                    buffer += "#$" + "%2x".formatted(ea);
                }
                break;

            case IMM_RR:
                pb = operandarray[0];
                buffer += m6809_regs_te[(pb >> 4) & 0xf] + "," + m6809_regs_te[pb & 0xf];
                break;
        }
        return buffer;
    }

//#endregion
}
