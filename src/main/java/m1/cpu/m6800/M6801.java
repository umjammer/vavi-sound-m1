/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m6800;

import m1.emu.Cpuint;
import m1.emu.Timer;


public class M6801 extends M6800 {

    public M6801() {
        m6803_insn = new Runnable[] {
                this::illegal, this::nop, this::illegal, this::illegal, this::lsrd, this::asld, this::tap, this::tpa,
                this::inx, this::dex, this::CLV, this::SEV, this::CLC, this::SEC, this::cli, this::sei,
                this::sba, this::cba, this::illegal, this::illegal, this::illegal, this::illegal, this::tab, this::tba,
                this::illegal, this::daa, this::illegal, this::aba, this::illegal, this::illegal, this::illegal, this::illegal,
                this::bra, this::brn, this::bhi, this::bls, this::bcc, this::bcs, this::bne, this::beq,
                this::bvc, this::bvs, this::bpl, this::bmi, this::bge, this::blt, this::bgt, this::ble,
                this::tsx, this::ins, this::pula, this::pulb, this::des, this::txs, this::psha, this::pshb,
                this::pulx, this::rts, this::abx, this::rti, this::pshx, this::mul, this::wai, this::swi,
                this::nega, this::illegal, this::illegal, this::coma, this::lsra, this::illegal, this::rora, this::asra,
                this::asla, this::rola, this::deca, this::illegal, this::inca, this::tsta, this::illegal, this::clra,
                this::negb, this::illegal, this::illegal, this::comb, this::lsrb, this::illegal, this::rorb, this::asrb,
                this::aslb, this::rolb, this::decb, this::illegal, this::incb, this::tstb, this::illegal, this::clrb,
                this::neg_ix, this::illegal, this::illegal, this::com_ix, this::lsr_ix, this::illegal, this::ror_ix, this::asr_ix,
                this::asl_ix, this::rol_ix, this::dec_ix, this::illegal, this::inc_ix, this::tst_ix, this::jmp_ix, this::clr_ix,
                this::neg_ex, this::illegal, this::illegal, this::com_ex, this::lsr_ex, this::illegal, this::ror_ex, this::asr_ex,
                this::asl_ex, this::rol_ex, this::dec_ex, this::illegal, this::inc_ex, this::tst_ex, this::jmp_ex, this::clr_ex,
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
        clock = 1000000;
        irq_callback = Cpuint::cpu_3_irq_callback;
        m6800_rx_timer = Timer.allocCommon(this::m6800_rx_tick, "m6800_rx_tick", false);
        m6800_tx_timer = Timer.allocCommon(this::m6800_tx_tick, "m6800_tx_tick", false);
    }

    @Override
    public int ExecuteCycles(int cycles) {
        return m6801_execute(cycles);
    }

    public int m6801_execute(int cycles) {
        byte ireg;
        pendingCycles = cycles;
        CLEANUP_conters();
        INCREMENT_COUNTER(extra_cycles);
        extra_cycles = 0;
        do {
            int prevCycles = pendingCycles;
            if ((wai_state & M6800_WAI) != 0) {
                EAT_CYCLES();
            } else {
                PPC = PC;
                ireg = ReadOp.apply(PC.lowWord);
                PC.lowWord++;
                m6803_insn[ireg].run();
                INCREMENT_COUNTER(cycles_6803[ireg]);
                int delta = prevCycles - pendingCycles;
                totalExecutedCycles += delta;
            }
        } while (pendingCycles > 0);
        INCREMENT_COUNTER(extra_cycles);
        extra_cycles = 0;
        return cycles - pendingCycles;
    }
}