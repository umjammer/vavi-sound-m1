/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m6800;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Cpuexec.cpuexec_data;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Cpuint.Register;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;


public class M6800 extends cpuexec_data {

    //@StructLayout(LayoutKind.Explicit)
    public static class RegisterPair implements Serializable {

        //@FieldOffset(0)
        public final short Word;

        //@FieldOffset(0)
        public final byte Low;

        //@FieldOffset(1)
        public final byte High;

        public RegisterPair(short value) {
            Word = value;
            Low = (byte) (Word);
            High = (byte) (Word >> 8);
        }

        public static short operator_short(RegisterPair rp) {
            return rp.Word;
        }

        public static RegisterPair operator_RegisterPair(short value) {
            return new RegisterPair(value);
        }
    }

    public static M6800 m1;
    public static Runnable action_rx, action_tx;
    public final Runnable[] insn;
    public final Runnable[] m6800_insn;
    public final Runnable[] hd63701_insn;
    public Runnable[] m6803_insn;
    public Register PPC, PC;
    public Register S, X, D, EA;
    public final byte[] cycles;
    public byte cc, wai_state, ic_eddge;
    public final byte[] irq_state = new byte[2];
    public byte nmi_state;
    public int extra_cycles;

    public interface irq_delegate extends Function<Integer, Integer> {}

    public irq_delegate irq_callback;
    public byte port1_ddr, port2_ddr, port3_ddr, port4_ddr, port1_data, port2_data, port3_data, port4_data;
    public byte tcsr, pending_tcsr, irq2, ram_ctrl;
    public Register counter, output_compare, timer_over;
    public short input_capture;
    public int clock;
    public byte trcsr, rmcr, rdr, tdr, rsr, tsr;
    public int rxbits, txbits, trcsr_read, tx;
    public M6800_TX_STATE txstate;
    public EmuTimer m6800_rx_timer, m6800_tx_timer;
    private final byte TCSR_OLVL = 0x01;
    private final byte TCSR_IEDG = 0x02;
    private final byte TCSR_ETOI = 0x04;
    private final byte TCSR_EOCI = 0x08;
    private final byte TCSR_EICI = 0x10;
    private final byte TCSR_TOF = 0x20;
    private final byte TCSR_OCF = 0x40;
    private final byte TCSR_ICF = (byte) 0x80;
    protected final byte M6800_WAI = 8;
    protected final byte M6800_SLP = 0x10;
    private static final byte M6800_IRQ_LINE = 0, M6800_TIN_LINE = 1;
    private final short M6803_DDR1 = 0x00;
    private final short M6803_DDR2 = 0x01;
    private final short M6803_DDR3 = 0x04;
    private final short M6803_DDR4 = 0x05;
    private final short M6803_PORT1 = 0x100;
    private final short M6803_PORT2 = 0x101;
    private static final short M6803_PORT3 = 0x102;
    private static final short M6803_PORT4 = 0x103;
    private final byte M6800_RMCR_SS_MASK = 0x03;
    private final byte M6800_RMCR_SS_4096 = 0x03;
    private final byte M6800_RMCR_SS_1024 = 0x02;
    private final byte M6800_RMCR_SS_128 = 0x01;
    private final byte M6800_RMCR_SS_16 = 0x00;
    private final byte M6800_RMCR_CC_MASK = 0x0c;
    private final byte M6800_TRCSR_RDRF = (byte) 0x80;
    private final byte M6800_TRCSR_ORFE = 0x40;
    private final byte M6800_TRCSR_TDRE = 0x20;
    private final byte M6800_TRCSR_RIE = 0x10;
    private final byte M6800_TRCSR_RE = 0x08;
    private final byte M6800_TRCSR_TIE = 0x04;
    private final byte M6800_TRCSR_TE = 0x02;
    private final byte M6800_TRCSR_WU = 0x01;
    private final byte M6800_PORT2_IO4 = 0x10;
    private final byte M6800_PORT2_IO3 = 0x08;
    private final int[] M6800_RMCR_SS = new int[] {16, 128, 1024, 4096};

    public enum M6800_TX_STATE {
        INIT,
        READY
    }

    private final byte CLEAR_LINE = 0;
    private final byte INPUT_LINE_NMI = 32;
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

    public int timer_next;
    public final byte[] flags8i = new byte[] { // increment
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
    private final byte[] flags8d = new byte[] { // decrement
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
    private final byte[] cycles_6800 = new byte[] {
            /* 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */
            /*0*/ 99, 2, 99, 99, 99, 99, 2, 2, 4, 4, 2, 2, 2, 2, 2, 2,
            /*1*/  2, 2, 99, 99, 99, 99, 2, 2, 99, 2, 99, 2, 99, 99, 99, 99,
            /*2*/  4, 99, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            /*3*/  4, 4, 4, 4, 4, 4, 4, 4, 99, 5, 99, 10, 99, 99, 9, 12,
            /*4*/  2, 99, 99, 2, 2, 99, 2, 2, 2, 2, 2, 99, 2, 2, 99, 2,
            /*5*/  2, 99, 99, 2, 2, 99, 2, 2, 2, 2, 2, 99, 2, 2, 99, 2,
            /*6*/  7, 99, 99, 7, 7, 99, 7, 7, 7, 7, 7, 99, 7, 7, 4, 7,
            /*7*/  6, 99, 99, 6, 6, 99, 6, 6, 6, 6, 6, 99, 6, 6, 3, 6,
            /*8*/  2, 2, 2, 99, 2, 2, 2, 99, 2, 2, 2, 2, 3, 8, 3, 99,
            /*9*/  3, 3, 3, 99, 3, 3, 3, 4, 3, 3, 3, 3, 4, 99, 4, 5,
            /*A*/  5, 5, 5, 99, 5, 5, 5, 6, 5, 5, 5, 5, 6, 8, 6, 7,
            /*B*/  4, 4, 4, 99, 4, 4, 4, 5, 4, 4, 4, 4, 5, 9, 5, 6,
            /*C*/  2, 2, 2, 99, 2, 2, 2, 99, 2, 2, 2, 2, 99, 99, 3, 99,
            /*D*/  3, 3, 3, 99, 3, 3, 3, 4, 3, 3, 3, 3, 99, 99, 4, 5,
            /*E*/  5, 5, 5, 99, 5, 5, 5, 6, 5, 5, 5, 5, 99, 99, 6, 7,
            /*F*/  4, 4, 4, 99, 4, 4, 4, 5, 4, 4, 4, 4, 99, 99, 5, 6
    };
    protected final byte[] cycles_6803 = {
            /* 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */
            /*0*/ 99, 2, 99, 99, 3, 3, 2, 2, 3, 3, 2, 2, 2, 2, 2, 2,
            /*1*/  2, 2, 99, 99, 99, 99, 2, 2, 99, 2, 99, 2, 99, 99, 99, 99,
            /*2*/  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            /*3*/  3, 3, 4, 4, 3, 3, 3, 3, 5, 5, 3, 10, 4, 10, 9, 12,
            /*4*/  2, 99, 99, 2, 2, 99, 2, 2, 2, 2, 2, 99, 2, 2, 99, 2,
            /*5*/  2, 99, 99, 2, 2, 99, 2, 2, 2, 2, 2, 99, 2, 2, 99, 2,
            /*6*/  6, 99, 99, 6, 6, 99, 6, 6, 6, 6, 6, 99, 6, 6, 3, 6,
            /*7*/  6, 99, 99, 6, 6, 99, 6, 6, 6, 6, 6, 99, 6, 6, 3, 6,
            /*8*/  2, 2, 2, 4, 2, 2, 2, 99, 2, 2, 2, 2, 4, 6, 3, 99,
            /*9*/  3, 3, 3, 5, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 4, 4,
            /*A*/  4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 5, 5,
            /*B*/  4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 5, 5,
            /*C*/  2, 2, 2, 4, 2, 2, 2, 99, 2, 2, 2, 2, 3, 99, 3, 99,
            /*D*/  3, 3, 3, 5, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4,
            /*E*/  4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5,
            /*F*/  4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5
    };
    private final byte[] cycles_63701 = {
            /* 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */
            /*0*/ 99, 1, 99, 99, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            /*1*/  1, 1, 99, 99, 99, 99, 1, 1, 2, 2, 4, 1, 99, 99, 99, 99,
            /*2*/  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            /*3*/  1, 1, 3, 3, 1, 1, 4, 4, 4, 5, 1, 10, 5, 7, 9, 12,
            /*4*/  1, 99, 99, 1, 1, 99, 1, 1, 1, 1, 1, 99, 1, 1, 99, 1,
            /*5*/  1, 99, 99, 1, 1, 99, 1, 1, 1, 1, 1, 99, 1, 1, 99, 1,
            /*6*/  6, 7, 7, 6, 6, 7, 6, 6, 6, 6, 6, 5, 6, 4, 3, 5,
            /*7*/  6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 3, 5,
            /*8*/  2, 2, 2, 3, 2, 2, 2, 99, 2, 2, 2, 2, 3, 5, 3, 99,
            /*9*/  3, 3, 3, 4, 3, 3, 3, 3, 3, 3, 3, 3, 4, 5, 4, 4,
            /*A*/  4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5,
            /*B*/  4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5, 6, 5, 5,
            /*C*/  2, 2, 2, 3, 2, 2, 2, 99, 2, 2, 2, 2, 3, 99, 3, 99,
            /*D*/  3, 3, 3, 4, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4,
            /*E*/  4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5,
            /*F*/  4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5
    };

    public Function<Short, Byte> ReadOp, ReadOpArg;
    public Function<Short, Byte> ReadMemory;
    public BiConsumer<Short, Byte> WriteMemory;
    public Function<Short, Byte> ReadIO;
    public BiConsumer<Short, Byte> WriteIO;

    public M6800() {
        m6800_insn = new Runnable[] {
                this::illegal, this::nop, this::illegal, this::illegal, this::illegal, this::illegal, this::tap, this::tpa,
                this::inx, this::dex, this::clv, this::sev, this::clc, this::sec, this::cli, this::sei,
                this::sba, this::cba, this::illegal, this::illegal, this::illegal, this::illegal, this::tab, this::tba,
                this::illegal, this::daa, this::illegal, this::aba, this::illegal, this::illegal, this::illegal, this::illegal,
                this::bra, this::brn, this::bhi, this::bls, this::bcc, this::bcs, this::bne, this::beq,
                this::bvc, this::bvs, this::bpl, this::bmi, this::bge, this::blt, this::bgt, this::ble,
                this::tsx, this::ins, this::pula, this::pulb, this::des, this::txs, this::psha, this::pshb,
                this::illegal, this::rts, this::illegal, this::rti, this::illegal, this::illegal, this::wai, this::swi,
                this::nega, this::illegal, this::illegal, this::coma, this::lsra, this::illegal, this::rora, this::asra,
                this::asla, this::rola, this::deca, this::illegal, this::inca, this::tsta, this::illegal, this::clra,
                this::negb, this::illegal, this::illegal, this::comb, this::lsrb, this::illegal, this::rorb, this::asrb,
                this::aslb, this::rolb, this::decb, this::illegal, this::incb, this::tstb, this::illegal, this::clrb,
                this::neg_ix, this::illegal, this::illegal, this::com_ix, this::lsr_ix, this::illegal, this::ror_ix, this::asr_ix,
                this::asl_ix, this::rol_ix, this::dec_ix, this::illegal, this::inc_ix, this::tst_ix, this::jmp_ix, this::clr_ix,
                this::neg_ex, this::illegal, this::illegal, this::com_ex, this::lsr_ex, this::illegal, this::ror_ex, this::asr_ex,
                this::asl_ex, this::rol_ex, this::dec_ex, this::illegal, this::inc_ex, this::tst_ex, this::jmp_ex, this::clr_ex,
                this::suba_im, this::cmpa_im, this::sbca_im, this::illegal, this::anda_im, this::bita_im, this::lda_im, this::sta_im,
                this::eora_im, this::adca_im, this::ora_im, this::adda_im, this::cmpx_im, this::bsr, this::lds_im, this::sts_im,
                this::suba_di, this::cmpa_di, this::sbca_di, this::illegal, this::anda_di, this::bita_di, this::lda_di, this::sta_di,
                this::eora_di, this::adca_di, this::ora_di, this::adda_di, this::cmpx_di, this::jsr_di, this::lds_di, this::sts_di,
                this::suba_ix, this::cmpa_ix, this::sbca_ix, this::illegal, this::anda_ix, this::bita_ix, this::lda_ix, this::sta_ix,
                this::eora_ix, this::adca_ix, this::ora_ix, this::adda_ix, this::cmpx_ix, this::jsr_ix, this::lds_ix, this::sts_ix,
                this::suba_ex, this::cmpa_ex, this::sbca_ex, this::illegal, this::anda_ex, this::bita_ex, this::lda_ex, this::sta_ex,
                this::eora_ex, this::adca_ex, this::ora_ex, this::adda_ex, this::cmpx_ex, this::jsr_ex, this::lds_ex, this::sts_ex,
                this::subb_im, this::cmpb_im, this::sbcb_im, this::illegal, this::andb_im, this::bitb_im, this::ldb_im, this::stb_im,
                this::eorb_im, this::adcb_im, this::orb_im, this::addb_im, this::illegal, this::illegal, this::ldx_im, this::stx_im,
                this::subb_di, this::cmpb_di, this::sbcb_di, this::illegal, this::andb_di, this::bitb_di, this::ldb_di, this::stb_di,
                this::eorb_di, this::adcb_di, this::orb_di, this::addb_di, this::illegal, this::illegal, this::ldx_di, this::stx_di,
                this::subb_ix, this::cmpb_ix, this::sbcb_ix, this::illegal, this::andb_ix, this::bitb_ix, this::ldb_ix, this::stb_ix,
                this::eorb_ix, this::adcb_ix, this::orb_ix, this::addb_ix, this::illegal, this::illegal, this::ldx_ix, this::stx_ix,
                this::subb_ex, this::cmpb_ex, this::sbcb_ex, this::illegal, this::andb_ex, this::bitb_ex, this::ldb_ex, this::stb_ex,
                this::eorb_ex, this::adcb_ex, this::orb_ex, this::addb_ex, this::illegal, this::illegal, this::ldx_ex, this::stx_ex
        };
        hd63701_insn = new Runnable[] {
                this::trap, this::nop, this::trap, this::trap, this::lsrd, this::asld, this::tap, this::tpa,
                this::inx, this::dex, this::clv, this::sev, this::clc, this::sec, this::cli, this::sei,
                this::sba, this::cba, this::undoc1, this::undoc2, this::trap, this::trap, this::tab, this::tba,
                this::xgdx, this::daa, this::slp, this::aba, this::trap, this::trap, this::trap, this::trap,
                this::bra, this::brn, this::bhi, this::bls, this::bcc, this::bcs, this::bne, this::beq,
                this::bvc, this::bvs, this::bpl, this::bmi, this::bge, this::blt, this::bgt, this::ble,
                this::tsx, this::ins, this::pula, this::pulb, this::des, this::txs, this::psha, this::pshb,
                this::pulx, this::rts, this::abx, this::rti, this::pshx, this::mul, this::wai, this::swi,
                this::nega, this::trap, this::trap, this::coma, this::lsra, this::trap, this::rora, this::asra,
                this::asla, this::rola, this::deca, this::trap, this::inca, this::tsta, this::trap, this::clra,
                this::negb, this::trap, this::trap, this::comb, this::lsrb, this::trap, this::rorb, this::asrb,
                this::aslb, this::rolb, this::decb, this::trap, this::incb, this::tstb, this::trap, this::clrb,
                this::neg_ix, this::aim_ix, this::oim_ix, this::com_ix, this::lsr_ix, this::eim_ix, this::ror_ix, this::asr_ix,
                this::asl_ix, this::rol_ix, this::dec_ix, this::tim_ix, this::inc_ix, this::tst_ix, this::jmp_ix, this::clr_ix,
                this::neg_ex, this::aim_di, this::oim_di, this::com_ex, this::lsr_ex, this::eim_di, this::ror_ex, this::asr_ex,
                this::asl_ex, this::rol_ex, this::dec_ex, this::tim_di, this::inc_ex, this::tst_ex, this::jmp_ex, this::clr_ex,
                this::suba_im, this::cmpa_im, this::sbca_im, this::subd_im, this::anda_im, this::bita_im, this::lda_im, this::sta_im,
                this::eora_im, this::adca_im, this::ora_im, this::adda_im, this::cpx_im, this::bsr, this::lds_im, this::sts_im,
                this::suba_di, this::cmpa_di, this::sbca_di, this::subd_di, this::anda_di, this::bita_di, this::lda_di, this::sta_di,
                this::eora_di, this::adca_di, this::ora_di, this::adda_di, this::cpx_di, this::jsr_di, this::lds_di, this::sts_di,
                this::suba_ix, this::cmpa_ix, this::sbca_ix, this::subd_ix, this::anda_ix, this::bita_ix, this::lda_ix, this::sta_ix,
                this::eora_ix, this::adca_ix, this::ora_ix, this::adda_ix, this::cpx_ix, this::jsr_ix, this::lds_ix, this::sts_ix,
                this::suba_ex, this::cmpa_ex, this::sbca_ex, this::subd_ex, this::anda_ex, this::bita_ex, this::lda_ex, this::sta_ex,
                this::eora_ex, this::adca_ex, this::ora_ex, this::adda_ex, this::cpx_ex, this::jsr_ex, this::lds_ex, this::sts_ex,
                this::subb_im, this::cmpb_im, this::sbcb_im, this::addd_im, this::andb_im, this::bitb_im, this::ldb_im, this::stb_im,
                this::eorb_im, this::adcb_im, this::orb_im, this::addb_im, this::ldd_im, this::std_im, this::ldx_im, this::stx_im,
                this::subb_di, this::cmpb_di, this::sbcb_di, this::addd_di, this::andb_di, this::bitb_di, this::ldb_di, this::stb_di,
                this::eorb_di, this::adcb_di, this::orb_di, this::addb_di, this::ldd_di, this::std_di, this::ldx_di, this::stx_di,
                this::subb_ix, this::cmpb_ix, this::sbcb_ix, this::addd_ix, this::andb_ix, this::bitb_ix, this::ldb_ix, this::stb_ix,
                this::eorb_ix, this::adcb_ix, this::orb_ix, this::addb_ix, this::ldd_ix, this::std_ix, this::ldx_ix, this::stx_ix,
                this::subb_ex, this::cmpb_ex, this::sbcb_ex, this::addd_ex, this::andb_ex, this::bitb_ex, this::ldb_ex, this::stb_ex,
                this::eorb_ex, this::adcb_ex, this::orb_ex, this::addb_ex, this::ldd_ex, this::std_ex, this::ldx_ex, this::stx_ex
        };
        insn = hd63701_insn;
        cycles = cycles_63701;
        clock = 1536000;
        irq_callback = null;
        m6800_rx_timer = Timer.allocCommon(this::m6800_rx_tick, "m6800_rx_tick", false);
        m6800_tx_timer = Timer.allocCommon(this::m6800_tx_tick, "m6800_tx_tick", false);
    }

    @Override
    public void Reset() {
        m6800_reset();
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
        WriteMemory.accept(S.lowWord, b);
        --S.lowWord;
    }

    private void PUSHWORD(Register w) {
        WriteMemory.accept(S.lowWord, w.lowByte);
        --S.lowWord;
        WriteMemory.accept(S.lowWord, w.highByte);
        --S.lowWord;
    }

    private byte PULLBYTE() {
        S.lowWord++;
        return ReadMemory.apply(S.lowWord);
    }

    private Register PULLWORD() {
        Register w = new Register();
        S.lowWord++;
        w.d = ReadMemory.apply(S.lowWord) << 8;
        S.lowWord++;
        w.d |= ReadMemory.apply(S.lowWord);
        return w;
    }

    private void MODIFIED_tcsr() {
        irq2 = (byte) ((tcsr & (tcsr << 3)) & (TCSR_ICF | TCSR_OCF | TCSR_TOF));
    }

    private void SET_TIMER_EVENT() {
        timer_next = (output_compare.d - counter.d < timer_over.d - counter.d) ? output_compare.d : timer_over.d;
    }

    protected void CLEANUP_conters() {
        output_compare.highWord -= counter.highWord;
        timer_over.lowWord -= counter.highWord;
        counter.highWord = 0;
        SET_TIMER_EVENT();
    }

    private void MODIFIED_counters() {
        output_compare.highWord = (output_compare.lowWord >= counter.lowWord) ? counter.highWord : (short) (counter.highWord + 1);
        SET_TIMER_EVENT();
    }

    private void TAKE_ICI() {
        ENTER_INTERRUPT((short) 0xfff6);
    }

    private void TAKE_OCI() {
        ENTER_INTERRUPT((short) 0xfff4);
    }

    private void TAKE_TOI() {
        ENTER_INTERRUPT((short) 0xfff2);
    }

    private void TAKE_SCI() {
        ENTER_INTERRUPT((short) 0xfff0);
    }

    private void TAKE_TRAP() {
        ENTER_INTERRUPT((short) 0xffee);
    }

    private void ONE_MORE_INSN() {
        byte ireg;
        PPC = PC;
        //debugger_instruction_hook(Machine, PCD);
        ireg = ReadOp.apply(PC.lowWord);
        PC.lowWord++;
        insn[ireg].run();
        INCREMENT_COUNTER(cycles[ireg]);
    }

    private void CHECK_IRQ_LINES() {
        if ((cc & 0x10) == 0) {
            if (irq_state[0] != (byte) LineState.CLEAR_LINE.ordinal()) {
                ENTER_INTERRUPT((short) 0xfff8);
                if (irq_callback != null) {
                    irq_callback.apply(0);
                }
            } else {
                m6800_check_irq2();
            }
        }
    }

    private void CLR_HNZVC() {
        cc &= (byte) 0xd0;
    }

    private void CLR_NZV() {
        cc &= (byte) 0xf1;
    }

    private void CLR_HNZC() {
        cc &= (byte) 0xd2;
    }

    private void CLR_NZVC() {
        cc &= (byte) 0xf0;
    }

    private void CLR_Z() {
        cc &= (byte) 0xfb;
    }

    private void CLR_NZC() {
        cc &= (byte) 0xf2;
    }

    private void CLR_ZC() {
        cc &= (byte) 0xfa;
    }

    private void CLR_C() {
        cc &= (byte) 0xfe;
    }

    private void SET_Z(byte a) {
        if (a == 0) {
            SEZ();
        }
    }

    private void SET_Z(short a) {
        if (a == 0) {
            SEZ();
        }
    }

    private void SET_Z8(byte a) {
        SET_Z(a);
    }

    private void SET_Z16(short a) {
        SET_Z(a);
    }

    private void SET_N8(byte a) {
        cc |= (byte) (((a) & 0x80) >> 4);
    }

    private void SET_N16(short a) {
        cc |= (byte) (((a) & 0x8000) >> 12);
    }

    private void SET_H(short a, short b, short r) {
        cc |= (byte) ((((a) ^ (b) ^ (r)) & 0x10) << 1);
    }

    private void SET_C8(short a) {
        cc |= (byte) (((a) & 0x100) >> 8);
    }

    private void SET_C16(int a) {
        cc |= (byte) (((a) & 0x1_0000) >> 16);
    }

    private void SET_V8(short a, short b, short r) {
        cc |= (byte) ((((a) ^ (b) ^ (r) ^ ((r) >> 1)) & 0x80) >> 6);
    }

    private void SET_V16(int a, int b, int r) {
        cc |= (byte) ((((a) ^ (b) ^ (r) ^ ((r) >> 1)) & 0x8000) >> 14);
    }

    private void SET_FLAGS8I(byte a) {
        cc |= flags8i[(a) & 0xff];
    }

    private void SET_FLAGS8D(byte a) {
        cc |= flags8d[(a) & 0xff];
    }

    private void SET_NZ8(byte a) {
        SET_N8(a);
        SET_Z8(a);
    }

    private void SET_NZ16(short a) {
        SET_N16(a);
        SET_Z16(a);
    }

    private void SET_FLAGS8(short a, short b, short r) {
        SET_N8((byte) r);
        SET_Z8((byte) r);
        SET_V8(a, b, r);
        SET_C8(r);
    }

    private void SET_FLAGS16(int a, int b, int r) {
        SET_N16((short) r);
        SET_Z16((short) r);
        SET_V16(a, b, r);
        SET_C16(r);
    }

    private short SIGNED(byte b) {
        return (short) ((b & 0x80) != 0 ? b | 0xff00 : b);
    }

    private void DIRECT() {
        EA.d = IMMBYTE();
    }

    private void IMM8() {
        EA.lowWord = PC.lowWord++;
    }

    private void IMM16() {
        EA.lowWord = PC.lowWord;
        PC.lowWord += 2;
    }

    private void EXTENDED() {
        EA = IMMWORD();
    }

    private void INDEXED() {
        EA.lowWord = (short) (X.lowWord + ReadOpArg.apply(PC.lowWord));
        PC.lowWord++;
    }

    protected void SEC() {
        cc |= 0x01;
    }

    protected void CLC() {
        cc &= (byte) 0xfe;
    }

    protected void SEZ() {
        cc |= 0x04;
    }

    protected void CLZ() {
        cc &= (byte) 0xfb;
    }

    protected void SEN() {
        cc |= 0x08;
    }

    protected void CLN() {
        cc &= (byte) 0xf7;
    }

    protected void SEV() {
        cc |= 0x02;
    }

    protected void CLV() {
        cc &= (byte) 0xfd;
    }

    protected void SEH() {
        cc |= 0x20;
    }

    protected void CLH() {
        cc &= (byte) 0xdf;
    }

    protected void SEI() {
        cc |= 0x10;
    }

    protected void CLI() {
        cc &= (byte) ~0x10;
    }

    protected void INCREMENT_COUNTER(int amount) {
        pendingCycles -= amount;
        counter.d += amount;
        if (counter.d >= timer_next)
            check_timer_event();
    }

    protected void EAT_CYCLES() {
        int cycles_to_eat;
        cycles_to_eat = timer_next - counter.d;
        if (cycles_to_eat > pendingCycles)
            cycles_to_eat = pendingCycles;
        if (cycles_to_eat > 0) {
            INCREMENT_COUNTER(cycles_to_eat);
        }
    }

    private byte DIRBYTE() {
        DIRECT();
        return ReadMemory.apply(EA.lowWord);
    }

    private Register DIRWORD() {
        Register w = new Register();
        DIRECT();
        w.lowWord = RM16(EA.lowWord);
        return w;
    }

    private byte EXTBYTE() {
        EXTENDED();
        return ReadMemory.apply(EA.lowWord);
    }

    private Register EXTWORD() {
        Register w = new Register();
        EXTENDED();
        w.lowWord = RM16(EA.lowWord);
        return w;
    }

    private byte IDXBYTE() {
        INDEXED();
        return ReadMemory.apply(EA.lowWord);
    }

    private Register IDXWORD() {
        Register w = new Register();
        INDEXED();
        w.lowWord = RM16(EA.lowWord);
        return w;
    }

    private void BRANCH(boolean f) {
        byte t = IMMBYTE();
        if (f) {
            PC.lowWord += SIGNED(t);
        }
    }

    private byte NXORV() {
        return (byte) ((cc & 0x08) ^ ((cc & 0x02) << 2));
    }

    private short RM16(short Addr) {
        short result = (short) (ReadMemory.apply(Addr) << 8);
        return (short) (result | ReadMemory.apply((short) (Addr + 1)));
    }

    private void WM16(short Addr, Register p) {
        WriteMemory.accept(Addr, p.highByte);
        WriteMemory.accept((short) (Addr + 1), p.lowByte);
    }

    private void ENTER_INTERRUPT(short irq_vector) {
        if ((wai_state & (M6800_WAI | M6800_SLP)) != 0) {
            if ((wai_state & M6800_WAI) != 0)
                extra_cycles += 4;
            wai_state &= (byte) (~(M6800_WAI | M6800_SLP));
        } else {
            PUSHWORD(PC);
            PUSHWORD(X);
            PUSHBYTE(D.highByte);
            PUSHBYTE(D.lowByte);
            PUSHBYTE(cc);
            extra_cycles += 12;
        }
        SEI();
        PC.d = RM16(irq_vector);
    }

    private void m6800_check_irq2() {
        if ((tcsr & (TCSR_EICI | TCSR_ICF)) == (TCSR_EICI | TCSR_ICF)) {
            TAKE_ICI();
            //if( m6800.irq_callback )
            //	(void)(*m6800.irq_callback)(M6800_TIN_LINE);
        } else if ((tcsr & (TCSR_EOCI | TCSR_OCF)) == (TCSR_EOCI | TCSR_OCF)) {
            TAKE_OCI();
        } else if ((tcsr & (TCSR_ETOI | TCSR_TOF)) == (TCSR_ETOI | TCSR_TOF)) {
            TAKE_TOI();
        } else if (((trcsr & (M6800_TRCSR_RIE | M6800_TRCSR_RDRF)) == (M6800_TRCSR_RIE | M6800_TRCSR_RDRF)) ||
                ((trcsr & (M6800_TRCSR_RIE | M6800_TRCSR_ORFE)) == (M6800_TRCSR_RIE | M6800_TRCSR_ORFE)) ||
                ((trcsr & (M6800_TRCSR_TIE | M6800_TRCSR_TDRE)) == (M6800_TRCSR_TIE | M6800_TRCSR_TDRE))) {
            TAKE_SCI();
        }
    }

    private void check_timer_event() {
        if (counter.d >= output_compare.d) {
            output_compare.highWord++;
            tcsr |= TCSR_OCF;
            pending_tcsr |= TCSR_OCF;
            MODIFIED_tcsr();
            if (((cc & 0x10) == 0) && ((tcsr & TCSR_EOCI) != 0))
                TAKE_OCI();
        }
        if (counter.d >= timer_over.d) {
            timer_over.lowWord++;
            tcsr |= TCSR_TOF;
            pending_tcsr |= TCSR_TOF;
            MODIFIED_tcsr();
            if (((cc & 0x10) == 0) && (tcsr & TCSR_ETOI) != 0)
                TAKE_TOI();
        }
        SET_TIMER_EVENT();
    }

    private void m6800_tx(int value) {
        port2_data = (byte) ((port2_data & 0xef) | (value << 4));
        if (port2_ddr == (byte) 0xff)
            WriteIO.accept(M6803_PORT2, port2_data);
        else
            WriteIO.accept(M6803_PORT2, (byte) ((port2_data & port2_ddr) | (ReadIO.apply(M6803_PORT2) & (port2_ddr ^ 0xff))));
    }

    private int m6800_rx() {
        return (ReadIO.apply(M6803_PORT2) & M6800_PORT2_IO3) >> 3;
    }

    public void m6800_tx_tick() {
        if ((trcsr & M6800_TRCSR_TE) != 0) {
            port2_ddr |= M6800_PORT2_IO4;
            switch (txstate) {
                case INIT:
                    tx = 1;
                    txbits++;

                    if (txbits == 10) {
                        txstate = M6800_TX_STATE.READY;
                        txbits = 0;
                    }
                    break;

                case READY:
                    switch (txbits) {
                        case 0:
                            if ((trcsr & M6800_TRCSR_TDRE) != 0) {
                                tx = 1;
                            } else {
                                tsr = tdr;
                                trcsr |= M6800_TRCSR_TDRE;
                                tx = 0;
                                txbits++;
                            }
                            break;

                        case 9:
                            // send stop bit '1'
                            tx = 1;
                            CHECK_IRQ_LINES();
                            txbits = 0;
                            break;

                        default:
                            tx = tsr & 0x01;
                            tsr >>= 1;
                            txbits++;
                            break;
                    }
                    break;
            }
        }
        m6800_tx(tx);
    }

    public void m6800_rx_tick() {
        if ((trcsr & M6800_TRCSR_RE) != 0) {
            if ((trcsr & M6800_TRCSR_WU) != 0) {
                if (m6800_rx() == 1) {
                    rxbits++;
                    if (rxbits == 10) {
                        trcsr &= (byte) (~M6800_TRCSR_WU);
                        rxbits = 0;
                    }
                } else {
                    rxbits = 0;
                }
            } else {
                switch (rxbits) {
                    case 0:
                        if (m6800_rx() == 0) {
                            rxbits++;
                        }
                        break;
                    case 9:
                        if (m6800_rx() == 1) {
                            if ((trcsr & M6800_TRCSR_RDRF) != 0) {
                                trcsr |= M6800_TRCSR_ORFE;
                                CHECK_IRQ_LINES();
                            } else {
                                if ((trcsr & M6800_TRCSR_ORFE) == 0) {
                                    rdr = rsr;
                                    trcsr |= M6800_TRCSR_RDRF;
                                    CHECK_IRQ_LINES();
                                }
                            }
                        } else {
                            if ((trcsr & M6800_TRCSR_ORFE) == 0) {
                                // transfer unframed data into receive register
                                rdr = rsr;
                            }
                            trcsr |= M6800_TRCSR_ORFE;
                            trcsr &= (byte) (~M6800_TRCSR_RDRF);
                            CHECK_IRQ_LINES();
                        }
                        rxbits = 0;
                        break;
                    default:
                        rsr >>= 1;
                        rsr |= (byte) (m6800_rx() << 7);
                        rxbits++;
                        break;
                }
            }
        }
    }

    private void m6800_reset() {
        SEI();                /* IRQ disabled */
        PC.lowWord = RM16((short) 0xfffe);
        wai_state = 0;
        nmi_state = 0;
        irq_state[0] = 0;
        irq_state[1] = 0;
        ic_eddge = 0;
        port1_ddr = 0x00;
        port2_ddr = 0x00;
        tcsr = 0x00;
        pending_tcsr = 0x00;
        irq2 = 0;
        counter.d = 0x0000;
        output_compare.d = 0xffff;
        timer_over.d = 0xffff;
        ram_ctrl |= 0x40;
        trcsr = M6800_TRCSR_TDRE;
        rmcr = 0;
        Timer.enable(m6800_rx_timer, false);
        Timer.enable(m6800_tx_timer, false);
        txstate = M6800_TX_STATE.INIT;
        txbits = rxbits = 0;
        trcsr_read = 0;
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        if (irqline == INPUT_LINE_NMI) {
            if (nmi_state == (byte) state.ordinal()) {
                return;
            }
            nmi_state = (byte) state.ordinal();
            if (state == LineState.CLEAR_LINE) {
                return;
            }
            ENTER_INTERRUPT((short) 0xfffc);
        } else {
            int eddge;
            if (irq_state[irqline] == (byte) state.ordinal()) {
                return;
            }
            irq_state[irqline] = (byte) state.ordinal();
            switch (irqline) {
                case M6800_IRQ_LINE:
                    if (state == LineState.CLEAR_LINE) {
                        return;
                    }
                    break;
                case M6800_TIN_LINE:
                    eddge = (state == LineState.CLEAR_LINE) ? 2 : 0;
                    if (((tcsr & TCSR_IEDG) ^ (state == LineState.CLEAR_LINE ? TCSR_IEDG : 0)) == 0) {
                        return;
                    }
                    /* active edge in */
                    tcsr |= TCSR_ICF;
                    pending_tcsr |= TCSR_ICF;
                    input_capture = counter.lowWord;
                    MODIFIED_tcsr();
                    if ((cc & 0x10) == 0) {
                        m6800_check_irq2();
                    }
                    break;
                default:
                    return;
            }
            CHECK_IRQ_LINES(); /* HJB 990417 */
        }
    }

    @Override
    public void cpunum_set_input_line_and_vector(int cpunum, int line, LineState state, int vector) {
        Timer.setInternal(Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue");
    }

    @Override
    public int ExecuteCycles(int cycles) {
        byte ireg;
        pendingCycles = cycles;
        CLEANUP_conters();
        INCREMENT_COUNTER(extra_cycles);
        extra_cycles = 0;
        do {
            int prevCycles = pendingCycles;
            if ((wai_state & (M6800_WAI | M6800_SLP)) != 0) {
                EAT_CYCLES();
            } else {
                PPC = PC;
                //debugger_instruction_hook(Machine, PCD);
                ireg = ReadOp.apply(PC.lowWord);
                PC.lowWord++;
                insn[ireg].run();
                INCREMENT_COUNTER(this.cycles[ireg]);
                int delta = prevCycles - pendingCycles;
                totalExecutedCycles += delta;
            }
        } while (pendingCycles > 0);
        INCREMENT_COUNTER(extra_cycles);
        extra_cycles = 0;
        return cycles - pendingCycles;
    }

    public byte hd63701_internal_registers_r(int offset) {
        return m6803_internal_registers_r(offset);
    }

    public void hd63701_internal_registers_w(int offset, byte data) {
        m6803_internal_registers_w(offset, data);
    }

    private byte m6803_internal_registers_r(int offset) {
        switch (offset) {
            case 0x00:
                return port1_ddr;
            case 0x01:
                return port2_ddr;
            case 0x02:
                return (byte) ((ReadIO.apply((short) 0x100) & (port1_ddr ^ 0xff)) | (port1_data & port1_ddr));
            case 0x03:
                return (byte) ((ReadIO.apply((short) 0x101) & (port2_ddr ^ 0xff)) | (port2_data & port2_ddr));
            case 0x04:
                return port3_ddr;
            case 0x05:
                return port4_ddr;
            case 0x06:
                return (byte) ((ReadIO.apply((short) 0x102) & (port3_ddr ^ 0xff)) | (port3_data & port3_ddr));
            case 0x07:
                return (byte) ((ReadIO.apply((short) 0x103) & (port4_ddr ^ 0xff)) | (port4_data & port4_ddr));
            case 0x08:
                pending_tcsr = 0;
                return tcsr;
            case 0x09:
                if ((pending_tcsr & TCSR_TOF) == 0) {
                    tcsr &= (byte) (~TCSR_TOF);
                    MODIFIED_tcsr();
                }
                return counter.highByte;
            case 0x0a:
                return counter.lowByte;
            case 0x0b:
                if ((pending_tcsr & TCSR_OCF) == 0) {
                    tcsr &= (byte) (~TCSR_OCF);
                    MODIFIED_tcsr();
                }
                return output_compare.highByte;
            case 0x0c:
                if ((pending_tcsr & TCSR_OCF) == 0) {
                    tcsr &= (byte) (~TCSR_OCF);
                    MODIFIED_tcsr();
                }
                return output_compare.lowByte;
            case 0x0d:
                if ((pending_tcsr & TCSR_ICF) == 0) {
                    tcsr &= (byte) (~TCSR_ICF);
                    MODIFIED_tcsr();
                }
                return (byte) ((input_capture >> 0) & 0xff);
            case 0x0e:
                return (byte) ((input_capture >> 8) & 0xff);
            case 0x0f:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: warning - read from unsupported register %02x\n",cpu_getactivecpu(),activecpu_get_pc(),offset);
                return 0;
            case 0x10:
                return rmcr;
            case 0x11:
                trcsr_read = 1;
                return trcsr;
            case 0x12:
                if (trcsr_read != 0) {
                    trcsr_read = 0;
                    trcsr = (byte) (trcsr & 0x3f);
                }
                return rdr;
            case 0x13:
                return tdr;
            case 0x14:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: read RAM control register\n",cpu_getactivecpu(),activecpu_get_pc());
                return ram_ctrl;
            case 0x15:
            case 0x16:
            case 0x17:
            case 0x18:
            case 0x19:
            case 0x1a:
            case 0x1b:
            case 0x1c:
            case 0x1d:
            case 0x1e:
            case 0x1f:
            default:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: warning - read from reserved internal register %02x\n",cpu_getactivecpu(),activecpu_get_pc(),offset);
                return 0;
        }
    }

    private void m6803_internal_registers_w(int offset, byte data) {
        int latch09 = 0;
        switch (offset) {
            case 0x00:
                if (port1_ddr != data) {
                    port1_ddr = data;
                    if (port1_ddr == (byte) 0xff)
                        WriteIO.accept((short) 0x100, port1_data);
                    else
                        WriteIO.accept((short) 0x100, (byte) ((port1_data & port1_ddr) | (ReadIO.apply((short) 0x100) & (port1_ddr ^ 0xff))));
                }
                break;
            case 0x01:
                if (port2_ddr != data) {
                    port2_ddr = data;
                    if (port2_ddr == (byte) 0xff)
                        WriteIO.accept((short) 0x101, port2_data);
                    else
                        WriteIO.accept((short) 0x101, (byte) ((port2_data & port2_ddr) | (ReadIO.apply((short) 0x101) & (port2_ddr ^ 0xff))));

                    if ((port2_ddr & 2) != 0) {
                        //logger.log(Level.TRACE, "CPU #%d PC %04x: warning - port 2 bit 1 set as output (OLVL) - not supported\n", cpu_getactivecpu(), activecpu_get_pc());
                    }
                }
                break;
            case 0x02:
                port1_data = data;
                if (port1_ddr == (byte) 0xff)
                    WriteIO.accept((short) 0x100, port1_data);
                else
                    WriteIO.accept((short) 0x100, (byte) ((port1_data & port1_ddr) | (ReadIO.apply((short) 0x100) & (port1_ddr ^ 0xff))));
                break;
            case 0x03:
                if ((trcsr & M6800_TRCSR_TE) != 0) {
                    port2_data = (byte) ((data & 0xef) | (tx << 4));
                } else {
                    port2_data = data;
                }
                if (port2_ddr == (byte) 0xff)
                    WriteIO.accept((short) 0x101, port2_data);
                else
                    WriteIO.accept((short) 0x101, (byte) ((port2_data & port2_ddr) | (ReadIO.apply((short) 0x101) & (port2_ddr ^ 0xff))));
                break;
            case 0x04:
                if (port3_ddr != data) {
                    port3_ddr = data;
                    if (port3_ddr == (byte) 0xff)
                        WriteIO.accept((short) 0x102, port3_data);
                    else
                        WriteIO.accept((short) 0x102, (byte) ((port3_data & port3_ddr) | (ReadIO.apply((short) 0x102) & (port3_ddr ^ 0xff))));
                }
                break;
            case 0x05:
                if (port4_ddr != data) {
                    port4_ddr = data;
                    if (port4_ddr == (byte) 0xff)
                        WriteIO.accept((short) 0x103, port4_data);
                    else
                        WriteIO.accept((short) 0x103, (byte) ((port4_data & port4_ddr) | (ReadIO.apply((short) 0x103) & (port4_ddr ^ 0xff))));
                }
                break;
            case 0x06:
                port3_data = data;
                if (port3_ddr == (byte) 0xff)
                    WriteIO.accept((short) 0x102, port3_data);
                else
                    WriteIO.accept((short) 0x102, (byte) ((port3_data & port3_ddr) | (ReadIO.apply((short) 0x102) & (port3_ddr ^ 0xff))));
                break;
            case 0x07:
                port4_data = data;
                if (port4_ddr == (byte) 0xff)
                    WriteIO.accept((short) 0x103, port4_data);
                else
                    WriteIO.accept((short) 0x103, (byte) ((port4_data & port4_ddr) | (ReadIO.apply((short) 0x103) & (port4_ddr ^ 0xff))));
                break;
            case 0x08:
                tcsr = data;
                pending_tcsr &= tcsr;
                MODIFIED_tcsr();
                if ((cc & 0x10) == 0)
                    m6800_check_irq2();
                break;
            case 0x09:
                latch09 = data & 0xff;    // 6301 only
                counter.lowWord = (short) 0xfff8;
                timer_over.lowWord = counter.highWord;
                MODIFIED_counters();
                break;
            case 0x0a:    // 6301 only
                counter.lowWord = (short) ((latch09 << 8) | (data & 0xff));
                timer_over.lowWord = counter.highWord;
                MODIFIED_counters();
                break;
            case 0x0b:
                if (output_compare.highByte != data) {
                    output_compare.highByte = data;
                    MODIFIED_counters();
                }
                break;
            case 0x0c:
                if (output_compare.lowByte != data) {
                    output_compare.lowByte = data;
                    MODIFIED_counters();
                }
                break;
            case 0x0d:
            case 0x0e:
            case 0x12:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: warning - write %02x to read only internal register %02x\n",cpu_getactivecpu(),activecpu_get_pc(),data,offset);
                break;
            case 0x0f:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: warning - write %02x to unsupported internal register %02x\n",cpu_getactivecpu(),activecpu_get_pc(),data,offset);
                break;
            case 0x10:
                rmcr = (byte) (data & 0x0f);
                switch ((rmcr & M6800_RMCR_CC_MASK) >> 2) {
                    case 0:
                    case 3: // not implemented
                        Timer.enable(m6800_rx_timer, false);
                        Timer.enable(m6800_tx_timer, false);
                        break;

                    case 1:
                    case 2: {
                        int divisor = M6800_RMCR_SS[rmcr & M6800_RMCR_SS_MASK];
                        Timer.adjustPeriodic(m6800_rx_timer, Attotime.ATTOTIME_ZERO, new Atime(0, (long) (1e18 / ((double) clock / divisor))));
                        Timer.adjustPeriodic(m6800_tx_timer, Attotime.ATTOTIME_ZERO, new Atime(0, (long) (1e18 / ((double) clock / divisor))));
                    }
                    break;
                }
                break;
            case 0x11:
                if ((data & M6800_TRCSR_TE) != 0 && (trcsr & M6800_TRCSR_TE) == 0) {
                    txstate = M6800_TX_STATE.values()[0];
                }
                trcsr = (byte) ((trcsr & 0xe0) | (data & 0x1f));
                break;
            case 0x13:
                if (trcsr_read != 0) {
                    trcsr_read = M6800_TX_STATE.INIT.ordinal();
                    trcsr &= (byte) (~M6800_TRCSR_TDRE);
                }
                tdr = data;
                break;
            case 0x14:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: write %02x to RAM control register\n",cpu_getactivecpu(),activecpu_get_pc(),data);
                ram_ctrl = data;
                break;
            case 0x15:
            case 0x16:
            case 0x17:
            case 0x18:
            case 0x19:
            case 0x1a:
            case 0x1b:
            case 0x1c:
            case 0x1d:
            case 0x1e:
            case 0x1f:
            default:
                //logger.log(Level.TRACE, "CPU #%d PC %04x: warning - write %02x to reserved internal register %02x\n",cpu_getactivecpu(),activecpu_get_pc(),data,offset);
                break;
        }
    }

    public void SaveStateBinary(BinaryWriter writer) {
        writer.write(PPC.lowWord);
        writer.write(PC.lowWord);
        writer.write(S.lowWord);
        writer.write(X.lowWord);
        writer.write(D.lowWord);
        writer.write(cc);
        writer.write(wai_state);
        writer.write(nmi_state);
        writer.write(irq_state[0]);
        writer.write(irq_state[1]);
        writer.write(ic_eddge);
        writer.write(port1_ddr);
        writer.write(port2_ddr);
        writer.write(port3_ddr);
        writer.write(port4_ddr);
        writer.write(port1_data);
        writer.write(port2_data);
        writer.write(port3_data);
        writer.write(port4_data);
        writer.write(tcsr);
        writer.write(pending_tcsr);
        writer.write(irq2);
        writer.write(ram_ctrl);
        writer.write(counter.d);
        writer.write(output_compare.d);
        writer.write(input_capture);
        writer.write(timer_over.d);
        writer.write(clock);
        writer.write(trcsr);
        writer.write(rmcr);
        writer.write(rdr);
        writer.write(tdr);
        writer.write(rsr);
        writer.write(tsr);
        writer.write(rxbits);
        writer.write(txbits);
        writer.write(txstate.ordinal());
        writer.write(trcsr_read);
        writer.write(tx);
        writer.write(totalExecutedCycles);
        writer.write(pendingCycles);
    }

    public void LoadStateBinary(BinaryReader reader) throws IOException {
        PPC.lowWord = reader.readUInt16();
        PC.lowWord = reader.readUInt16();
        S.lowWord = reader.readUInt16();
        X.lowWord = reader.readUInt16();
        D.lowWord = reader.readUInt16();
        cc = reader.readByte();
        wai_state = reader.readByte();
        nmi_state = reader.readByte();
        irq_state[0] = reader.readByte();
        irq_state[1] = reader.readByte();
        ic_eddge = reader.readByte();
        port1_ddr = reader.readByte();
        port2_ddr = reader.readByte();
        port3_ddr = reader.readByte();
        port4_ddr = reader.readByte();
        port1_data = reader.readByte();
        port2_data = reader.readByte();
        port3_data = reader.readByte();
        port4_data = reader.readByte();
        tcsr = reader.readByte();
        pending_tcsr = reader.readByte();
        irq2 = reader.readByte();
        ram_ctrl = reader.readByte();
        counter.d = reader.readUInt32();
        output_compare.d = reader.readUInt32();
        input_capture = reader.readUInt16();
        timer_over.d = reader.readUInt32();
        clock = reader.readInt32();
        trcsr = reader.readByte();
        rmcr = reader.readByte();
        rdr = reader.readByte();
        tdr = reader.readByte();
        rsr = reader.readByte();
        tsr = reader.readByte();
        rxbits = reader.readInt32();
        txbits = reader.readInt32();
        txstate = M6800.M6800_TX_STATE.values()[reader.readInt32()];
        trcsr_read = reader.readInt32();
        tx = reader.readInt32();
        totalExecutedCycles = reader.readUInt64();
        pendingCycles = reader.readInt32();
    }

//#region M6800op

    protected void illegal() {
    }

    protected void trap() {
//logger.log(Level.ERROR, "M6808: illegal opcode: address %04X, op %02X".formatted(PC, (int) M_RDOP_ARG(PC) & 0xFF);
        TAKE_TRAP();
    }

    protected void nop() {
    }

    protected void lsrd() {
        short t;
        CLR_NZC();
        t = D.lowWord;
        cc |= (byte) (t & 0x0001);
        t >>= 1;
        SET_Z16(t);
        D.lowWord = t;
    }

    protected void asld() {
        int r;
        short t;
        t = D.lowWord;
        r = t << 1;
        CLR_NZVC();
        SET_FLAGS16(t, t, r);
        D.lowWord = (short) r;
    }

    protected void tap() {
        cc = D.highByte;
        ONE_MORE_INSN();
        CHECK_IRQ_LINES();
    }

    protected void tpa() {
        D.highByte = cc;
    }

    protected void inx() {
        ++X.lowWord;
        CLR_Z();
        SET_Z16(X.lowWord);
    }

    protected void dex() {
        --X.lowWord;
        CLR_Z();
        SET_Z16(X.lowWord);
    }

    protected void clv() {
        CLV();
    }

    protected void sev() {
        SEV();
    }

    protected void clc() {
        CLC();
    }

    protected void sec() {
        SEC();
    }

    protected void cli() {
        CLI();
        ONE_MORE_INSN();
        CHECK_IRQ_LINES();
    }

    protected void sei() {
        SEI();
        ONE_MORE_INSN();
        CHECK_IRQ_LINES();
    }

    protected void sba() {
        short t;
        t = (short) (D.highByte - D.lowByte);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, D.lowByte, t);
        D.highByte = (byte) t;
    }

    protected void cba() {
        short t;
        t = (short) (D.highByte - D.lowByte);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, D.lowByte, t);
    }

    protected void undoc1() {
        X.lowWord += ReadMemory.apply((short) (S.lowWord + 1));
    }

    protected void undoc2() {
        X.lowWord += ReadMemory.apply((short) (S.lowWord + 1));
    }

    protected void tab() {
        D.lowByte = D.highByte;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void tba() {
        D.highByte = D.lowByte;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void xgdx() {
        short t = X.lowWord;
        X.lowWord = D.lowWord;
        D.lowWord = t;
    }

    protected void daa() {
        int msn, lsn;
        short t, cf = 0;
        msn = D.highByte & 0xf0;
        lsn = D.highByte & 0x0f;
        if (lsn > 0x09 || (cc & 0x20) != 0)
            cf |= 0x06;
        if (msn > 0x80 && lsn > 0x09)
            cf |= 0x60;
        if (msn > 0x90 || (cc & 0x01) != 0)
            cf |= 0x60;
        t = (short) (cf + D.highByte);
        CLR_NZV();
        SET_NZ8((byte) t);
        SET_C8(t);
        D.highByte = (byte) t;
    }

    protected void slp() {
        wai_state |= M6800_SLP;
        EAT_CYCLES();
    }

    protected void aba() {
        short t;
        t = (short) (D.highByte + D.lowByte);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, D.lowByte, t);
        SET_H(D.highByte, D.lowByte, t);
        D.highByte = (byte) t;
    }

    protected void bra() {
        byte t;
        t = IMMBYTE();
        PC.lowWord += SIGNED(t);
        if (t == (byte) 0xfe)
            EAT_CYCLES();
    }

    protected void brn() {
        byte t;
        t = IMMBYTE();
    }

    protected void bhi() {
        BRANCH((cc & 0x05) == 0);
    }

    protected void bls() {
        BRANCH((cc & 0x05) != 0);
    }

    protected void bcc() {
        BRANCH((cc & 0x01) == 0);
    }

    protected void bcs() {
        BRANCH((cc & 0x01) != 0);
    }

    protected void bne() {
        BRANCH((cc & 0x04) == 0);
    }

    protected void beq() {
        BRANCH((cc & 0x04) != 0);
    }

    protected void bvc() {
        BRANCH((cc & 0x02) == 0);
    }

    protected void bvs() {
        BRANCH((cc & 0x02) != 0);
    }

    protected void bpl() {
        BRANCH((cc & 0x08) == 0);
    }

    protected void bmi() {
        BRANCH((cc & 0x08) != 0);
    }

    protected void bge() {
        BRANCH(NXORV() == 0);
    }

    protected void blt() {
        BRANCH(NXORV() != 0);
    }

    protected void bgt() {
        BRANCH(!(NXORV() != 0 || (cc & 0x04) != 0));
    }

    protected void ble() {
        BRANCH(NXORV() != 0 || (cc & 0x04) != 0);
    }

    protected void tsx() {
        X.lowWord = (short) (S.lowWord + 1);
    }

    protected void ins() {
        ++S.lowWord;
    }

    protected void pula() {
        D.highByte = PULLBYTE();
    }

    protected void pulb() {
        D.lowByte = PULLBYTE();
    }

    protected void des() {
        --S.lowWord;
    }

    protected void txs() {
        S.lowWord = (short) (X.lowWord - 1);
    }

    protected void psha() {
        PUSHBYTE(D.highByte);
    }

    protected void pshb() {
        PUSHBYTE(D.lowByte);
    }

    protected void pulx() {
        X = PULLWORD();
    }

    protected void rts() {
        PC = PULLWORD();
        //CHANGE_PC();
    }

    protected void abx() {
        X.lowWord += D.lowByte;
    }

    protected void rti() {
        cc = PULLBYTE();
        D.lowByte = PULLBYTE();
        D.highByte = PULLBYTE();
        X = PULLWORD();
        PC = PULLWORD();
        //CHANGE_PC();
        CHECK_IRQ_LINES();
    }

    protected void pshx() {
        PUSHWORD(X);
    }

    protected void mul() {
        short t;
        t = (short) (D.highByte * D.lowByte);
        CLR_C();
        if ((t & 0x80) != 0)
            SEC();
        D.lowWord = t;
    }

    protected void wai() {
        wai_state |= M6800_WAI;
        PUSHWORD(PC);
        PUSHWORD(X);
        PUSHBYTE(D.highByte);
        PUSHBYTE(D.lowByte);
        PUSHBYTE(cc);
        CHECK_IRQ_LINES();
        if ((wai_state & M6800_WAI) != 0)
            EAT_CYCLES();
    }

    protected void swi() {
        PUSHWORD(PC);
        PUSHWORD(X);
        PUSHBYTE(D.highByte);
        PUSHBYTE(D.lowByte);
        PUSHBYTE(cc);
        SEI();
        PC.d = RM16((short) 0xfffa);
        //CHANGE_PC();
    }

    protected void nega() {
        short r;
        r = (short) (-D.highByte);
        CLR_NZVC();
        SET_FLAGS8((short) 0, D.highByte, r);
        D.highByte = (byte) r;
    }

    protected void coma() {
        D.highByte = (byte) (~D.highByte);
        CLR_NZV();
        SET_NZ8(D.highByte);
        SEC();
    }

    protected void lsra() {
        CLR_NZC();
        cc |= (byte) (D.highByte & 0x01);
        D.highByte >>= 1;
        SET_Z8(D.highByte);
    }

    protected void rora() {
        byte r;
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (D.highByte & 0x01);
        r |= (byte) (D.highByte >> 1);
        SET_NZ8(r);
        D.highByte = r;
    }

    protected void asra() {
        CLR_NZC();
        cc |= (byte) (D.highByte & 0x01);
        D.highByte >>= 1;
        D.highByte |= (byte) ((D.highByte & 0x40) << 1);
        SET_NZ8(D.highByte);
    }

    protected void asla() {
        short r;
        r = (short) (D.highByte << 1);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, D.highByte, r);
        D.highByte = (byte) r;
    }

    protected void rola() {
        short t, r;
        t = D.highByte;
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8(t, t, r);
        D.highByte = (byte) r;
    }

    protected void deca() {
        --D.highByte;
        CLR_NZV();
        SET_FLAGS8D(D.highByte);
    }

    protected void inca() {
        ++D.highByte;
        CLR_NZV();
        SET_FLAGS8I(D.highByte);
    }

    protected void tsta() {
        CLR_NZVC();
        SET_NZ8(D.highByte);
    }

    protected void clra() {
        D.highByte = 0;
        CLR_NZVC();
        SEZ();
    }

    protected void negb() {
        short r;
        r = (short) (-D.lowByte);
        CLR_NZVC();
        SET_FLAGS8((short) 0, D.lowByte, r);
        D.lowByte = (byte) r;
    }

    protected void comb() {
        D.lowByte = (byte) (~D.lowByte);
        CLR_NZV();
        SET_NZ8(D.lowByte);
        SEC();
    }

    protected void lsrb() {
        CLR_NZC();
        cc |= (byte) (D.lowByte & 0x01);
        D.lowByte >>= 1;
        SET_Z8(D.lowByte);
    }

    protected void rorb() {
        byte r;
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (D.lowByte & 0x01);
        r |= (byte) (D.lowByte >> 1);
        SET_NZ8(r);
        D.lowByte = r;
    }

    protected void asrb() {
        CLR_NZC();
        cc |= (byte) (D.lowByte & 0x01);
        D.lowByte >>= 1;
        D.lowByte |= (byte) ((D.lowByte & 0x40) << 1);
        SET_NZ8(D.lowByte);
    }

    protected void aslb() {
        short r;
        r = (short) (D.lowByte << 1);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, D.lowByte, r);
        D.lowByte = (byte) r;
    }

    protected void rolb() {
        short t, r;
        t = D.lowByte;
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8(t, t, r);
        D.lowByte = (byte) r;
    }

    protected void decb() {
        --D.lowByte;
        CLR_NZV();
        SET_FLAGS8D(D.lowByte);
    }

    protected void incb() {
        ++D.lowByte;
        CLR_NZV();
        SET_FLAGS8I(D.lowByte);
    }

    protected void tstb() {
        CLR_NZVC();
        SET_NZ8(D.lowByte);
    }

    protected void clrb() {
        D.lowByte = 0;
        CLR_NZVC();
        SEZ();
    }

    protected void neg_ix() {
        short r, t;
        t = IDXBYTE();
        r = (short) (-t);
        CLR_NZVC();
        SET_FLAGS8((short) 0, t, r);
        WriteMemory.accept(EA.lowWord, (byte) r);
    }

    protected void aim_ix() {
        byte t, r;
        t = IMMBYTE();
        r = IDXBYTE();
        r &= t;
        CLR_NZV();
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void oim_ix() {
        byte t, r;
        t = IMMBYTE();
        r = IDXBYTE();
        r |= t;
        CLR_NZV();
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void com_ix() {
        byte t;
        t = IDXBYTE();
        t = (byte) (~t);
        CLR_NZV();
        SET_NZ8(t);
        SEC();
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void lsr_ix() {
        byte t;
        t = IDXBYTE();
        CLR_NZC();
        cc |= (byte) (t & 0x01);
        t >>= 1;
        SET_Z8(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void eim_ix() {
        byte t, r;
        t = IMMBYTE();
        r = IDXBYTE();
        r ^= t;
        CLR_NZV();
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void ror_ix() {
        byte t, r;
        t = IDXBYTE();
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (t & 0x01);
        r |= (byte) (t >> 1);
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void asr_ix() {
        byte t;
        t = IDXBYTE();
        CLR_NZC();
        cc |= (byte) (t & 0x01);
        t >>= 1;
        t |= (byte) ((t & 0x40) << 1);
        SET_NZ8(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void asl_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8(t, t, r);
        WriteMemory.accept(EA.lowWord, (byte) r);
    }

    protected void rol_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8(t, t, r);
        WriteMemory.accept(EA.lowWord, (byte) r);
    }

    protected void dec_ix() {
        byte t;
        t = IDXBYTE();
        --t;
        CLR_NZV();
        SET_FLAGS8D(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void tim_ix() {
        byte t, r;
        t = IMMBYTE();
        r = IDXBYTE();
        r &= t;
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void inc_ix() {
        byte t;
        t = IDXBYTE();
        ++t;
        CLR_NZV();
        SET_FLAGS8I(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void tst_ix() {
        byte t;
        t = IDXBYTE();
        CLR_NZVC();
        SET_NZ8(t);
    }

    protected void jmp_ix() {
        INDEXED();
        PC.lowWord = EA.lowWord;
        //CHANGE_PC();
    }

    protected void clr_ix() {
        INDEXED();
        WriteMemory.accept(EA.lowWord, (byte) 0);
        CLR_NZVC();
        SEZ();
    }

    protected void neg_ex() {
        short r, t;
        t = EXTBYTE();
        r = (short) (-t);
        CLR_NZVC();
        SET_FLAGS8((short) 0, t, r);
        WriteMemory.accept(EA.lowWord, (byte) r);
    }

    protected void aim_di() {
        byte t, r;
        t = IMMBYTE();
        r = DIRBYTE();
        r &= t;
        CLR_NZV();
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void oim_di() {
        byte t, r;
        t = IMMBYTE();
        r = DIRBYTE();
        r |= t;
        CLR_NZV();
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void com_ex() {
        byte t;
        t = EXTBYTE();
        t = (byte) (~t);
        CLR_NZV();
        SET_NZ8(t);
        SEC();
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void lsr_ex() {
        byte t;
        t = EXTBYTE();
        CLR_NZC();
        cc |= (byte) (t & 0x01);
        t >>= 1;
        SET_Z8(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void eim_di() {
        byte t, r;
        t = IMMBYTE();
        r = DIRBYTE();
        r ^= t;
        CLR_NZV();
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void ror_ex() {
        byte t, r;
        t = EXTBYTE();
        r = (byte) ((cc & 0x01) << 7);
        CLR_NZC();
        cc |= (byte) (t & 0x01);
        r |= (byte) (t >> 1);
        SET_NZ8(r);
        WriteMemory.accept(EA.lowWord, r);
    }

    protected void asr_ex() {
        byte t;
        t = EXTBYTE();
        CLR_NZC();
        cc |= (byte) (t & 0x01);
        t >>= 1;
        t |= (byte) ((t & 0x40) << 1);
        SET_NZ8(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void asl_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8(t, t, r);
        WriteMemory.accept(EA.lowWord, (byte) r);
    }

    protected void rol_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (cc & 0x01);
        r |= (short) (t << 1);
        CLR_NZVC();
        SET_FLAGS8(t, t, r);
        WriteMemory.accept(EA.lowWord, (byte) r);
    }

    protected void dec_ex() {
        byte t;
        t = EXTBYTE();
        --t;
        CLR_NZV();
        SET_FLAGS8D(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void tim_di() {
        byte t, r;
        t = IMMBYTE();
        r = DIRBYTE();
        r &= t;
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void inc_ex() {
        byte t;
        t = EXTBYTE();
        ++t;
        CLR_NZV();
        SET_FLAGS8I(t);
        WriteMemory.accept(EA.lowWord, t);
    }

    protected void tst_ex() {
        byte t;
        t = EXTBYTE();
        CLR_NZVC();
        SET_NZ8(t);
    }

    protected void jmp_ex() {
        EXTENDED();
        PC.lowWord = EA.lowWord;
        //CHANGE_PC();
    }

    protected void clr_ex() {
        EXTENDED();
        WriteMemory.accept(EA.lowWord, (byte) 0);
        CLR_NZVC();
        SEZ();
    }

    protected void suba_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpa_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    protected void sbca_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void subd_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void anda_im() {
        byte t;
        t = IMMBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void bita_im() {
        byte t, r;
        t = IMMBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void lda_im() {
        D.highByte = IMMBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void sta_im() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        IMM8();
        WriteMemory.accept(EA.lowWord, D.highByte);
    }

    protected void eora_im() {
        byte t;
        t = IMMBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adca_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void ora_im() {
        byte t;
        t = IMMBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adda_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpx_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZV();
        SET_NZ16((short) r);
        SET_V16(d, b.d, r);
    }

    protected void cpx_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
    }

    protected void bsr() {
        byte t;
        t = IMMBYTE();
        PUSHWORD(PC);
        PC.lowWord += SIGNED(t);
        //CHANGE_PC();
    }

    protected void lds_im() {
        S = IMMWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
    }

    protected void sts_im() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        IMM16();
        WM16(EA.lowWord, S);
    }

    protected void suba_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpa_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    protected void sbca_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void subd_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void anda_di() {
        byte t;
        t = DIRBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void bita_di() {
        byte t, r;
        t = DIRBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void lda_di() {
        D.highByte = DIRBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void sta_di() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        DIRECT();
        WriteMemory.accept(EA.lowWord, D.highByte);
    }

    protected void eora_di() {
        byte t;
        t = DIRBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adca_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void ora_di() {
        byte t;
        t = DIRBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adda_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpx_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZV();
        SET_NZ16((short) r);
        SET_V16(d, b.d, r);
    }

    protected void cpx_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
    }

    protected void jsr_di() {
        DIRECT();
        PUSHWORD(PC);
        PC.lowWord = EA.lowWord;
        //CHANGE_PC();
    }

    protected void lds_di() {
        S = DIRWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
    }

    protected void sts_di() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        DIRECT();
        WM16(EA.lowWord, S);
    }

    protected void suba_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpa_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    protected void sbca_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.highByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void subd_ix() {
        int r, d;
        Register b;
        b = IDXWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void anda_ix() {
        byte t;
        t = IDXBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void bita_ix() {
        byte t, r;
        t = IDXBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void lda_ix() {
        D.highByte = IDXBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void sta_ix() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        INDEXED();
        WriteMemory.accept(EA.lowWord, D.highByte);
    }

    protected void eora_ix() {
        byte t;
        t = IDXBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adca_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.highByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void ora_ix() {
        byte t;
        t = IDXBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adda_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpx_ix() {
        int r, d;
        Register b;
        b = IDXWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZV();
        SET_NZ16((short) r);
        SET_V16(d, b.d, r);
    }

    protected void cpx_ix() {
        int r, d;
        Register b;
        b = IDXWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
    }

    protected void jsr_ix() {
        INDEXED();
        PUSHWORD(PC);
        PC.lowWord = EA.lowWord;
        //CHANGE_PC();
    }

    protected void lds_ix() {
        S = IDXWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
    }

    protected void sts_ix() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        INDEXED();
        WM16(EA.lowWord, S);
    }

    protected void suba_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpa_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
    }

    protected void sbca_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void subd_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = D.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void anda_ex() {
        byte t;
        t = EXTBYTE();
        D.highByte &= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void bita_ex() {
        byte t, r;
        t = EXTBYTE();
        r = (byte) (D.highByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void lda_ex() {
        D.highByte = EXTBYTE();
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void sta_ex() {
        CLR_NZV();
        SET_NZ8(D.highByte);
        EXTENDED();
        WriteMemory.accept(EA.lowWord, D.highByte);
    }

    protected void eora_ex() {
        byte t;
        t = EXTBYTE();
        D.highByte ^= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adca_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void ora_ex() {
        byte t;
        t = EXTBYTE();
        D.highByte |= t;
        CLR_NZV();
        SET_NZ8(D.highByte);
    }

    protected void adda_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.highByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.highByte, t, r);
        SET_H(D.highByte, t, r);
        D.highByte = (byte) r;
    }

    protected void cmpx_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZV();
        SET_NZ16((short) r);
        SET_V16(d, b.d, r);
    }

    protected void cpx_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = X.lowWord;
        r = d - b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
    }

    protected void jsr_ex() {
        EXTENDED();
        PUSHWORD(PC);
        PC.lowWord = EA.lowWord;
        //CHANGE_PC();
    }

    protected void lds_ex() {
        S = EXTWORD();
        CLR_NZV();
        SET_NZ16(S.lowWord);
    }

    protected void sts_ex() {
        CLR_NZV();
        SET_NZ16(S.lowWord);
        EXTENDED();
        WM16(EA.lowWord, S);
    }

    protected void subb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void cmpb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    protected void sbcb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void addd_im() {
        int r, d;
        Register b;
        b = IMMWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void andb_im() {
        byte t;
        t = IMMBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void bitb_im() {
        byte t, r;
        t = IMMBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void ldb_im() {
        D.lowByte = IMMBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void stb_im() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        IMM8();
        WriteMemory.accept(EA.lowWord, D.lowByte);
    }

    protected void eorb_im() {
        byte t;
        t = IMMBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void adcb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void orb_im() {
        byte t;
        t = IMMBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void addb_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void ldd_im() {
        D = IMMWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    protected void std_im() {
        IMM16();
        CLR_NZV();
        SET_NZ16(D.lowWord);
        WM16(EA.lowWord, D);
    }

    protected void ldx_im() {
        X = IMMWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    protected void stx_im() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        IMM16();
        WM16(EA.lowWord, X);
    }

    protected void subb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void cmpb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    protected void sbcb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void addd_di() {
        int r, d;
        Register b;
        b = DIRWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void andb_di() {
        byte t;
        t = DIRBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void bitb_di() {
        byte t, r;
        t = DIRBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void ldb_di() {
        D.lowByte = DIRBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void stb_di() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        DIRECT();
        WriteMemory.accept(EA.lowWord, D.lowByte);
    }

    protected void eorb_di() {
        byte t;
        t = DIRBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void adcb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void orb_di() {
        byte t;
        t = DIRBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void addb_di() {
        short t, r;
        t = DIRBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void ldd_di() {
        D = DIRWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    protected void std_di() {
        DIRECT();
        CLR_NZV();
        SET_NZ16(D.lowWord);
        WM16(EA.lowWord, D);
    }

    protected void ldx_di() {
        X = DIRWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    protected void stx_di() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        DIRECT();
        WM16(EA.lowWord, X);
    }

    protected void subb_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void cmpb_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    protected void sbcb_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.lowByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void addd_ix() {
        int r, d;
        Register b;
        b = IDXWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void andb_ix() {
        byte t;
        t = IDXBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void bitb_ix() {
        byte t, r;
        t = IDXBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void ldb_ix() {
        D.lowByte = IDXBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void stb_ix() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        INDEXED();
        WriteMemory.accept(EA.lowWord, D.lowByte);
    }

    protected void eorb_ix() {
        byte t;
        t = IDXBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void adcb_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.lowByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void orb_ix() {
        byte t;
        t = IDXBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void addb_ix() {
        short t, r;
        t = IDXBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void ldd_ix() {
        D = IDXWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    protected void adcx_im() {
        short t, r;
        t = IMMBYTE();
        r = (short) (X.lowWord + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(X.lowWord, t, r);
        SET_H(X.lowWord, t, r);
        X.lowWord = r;
    }

    protected void std_ix() {
        INDEXED();
        CLR_NZV();
        SET_NZ16(D.lowWord);
        WM16(EA.lowWord, D);
    }

    protected void ldx_ix() {
        X = IDXWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    protected void stx_ix() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        INDEXED();
        WM16(EA.lowWord, X);
    }

    protected void subb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void cmpb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte - t);
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
    }

    protected void sbcb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte - t - (cc & 0x01));
        CLR_NZVC();
        SET_FLAGS8(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void addd_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = D.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        D.lowWord = (short) r;
    }

    protected void andb_ex() {
        byte t;
        t = EXTBYTE();
        D.lowByte &= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void bitb_ex() {
        byte t, r;
        t = EXTBYTE();
        r = (byte) (D.lowByte & t);
        CLR_NZV();
        SET_NZ8(r);
    }

    protected void ldb_ex() {
        D.lowByte = EXTBYTE();
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void stb_ex() {
        CLR_NZV();
        SET_NZ8(D.lowByte);
        EXTENDED();
        WriteMemory.accept(EA.lowWord, D.lowByte);
    }

    protected void eorb_ex() {
        byte t;
        t = EXTBYTE();
        D.lowByte ^= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void adcb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte + t + (cc & 0x01));
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void orb_ex() {
        byte t;
        t = EXTBYTE();
        D.lowByte |= t;
        CLR_NZV();
        SET_NZ8(D.lowByte);
    }

    protected void addb_ex() {
        short t, r;
        t = EXTBYTE();
        r = (short) (D.lowByte + t);
        CLR_HNZVC();
        SET_FLAGS8(D.lowByte, t, r);
        SET_H(D.lowByte, t, r);
        D.lowByte = (byte) r;
    }

    protected void ldd_ex() {
        D = EXTWORD();
        CLR_NZV();
        SET_NZ16(D.lowWord);
    }

    protected void addx_ex() {
        int r, d;
        Register b;
        b = EXTWORD();
        d = X.lowWord;
        r = d + b.d;
        CLR_NZVC();
        SET_FLAGS16(d, b.d, r);
        X.lowWord = (short) r;
    }

    protected void std_ex() {
        EXTENDED();
        CLR_NZV();
        SET_NZ16(D.lowWord);
        WM16(EA.lowWord, D);
    }

    protected void ldx_ex() {
        X = EXTWORD();
        CLR_NZV();
        SET_NZ16(X.lowWord);
    }

    protected void stx_ex() {
        CLR_NZV();
        SET_NZ16(X.lowWord);
        EXTENDED();
        WM16(EA.lowWord, X);
    }

//    protected void CLV() {
//        cc &= 0xfd;
//    }
//
//    protected void SEV() {
//    }

//#endregion
}

