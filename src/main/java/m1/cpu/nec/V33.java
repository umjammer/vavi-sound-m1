/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.nec;

public class V33 extends Nec {

    public V33() {
        nec_init();
        chip_type = 0;
    }

    @Override
    public int ExecuteCycles(int cycles) {
        return v33_execute(cycles);
    }

    public int v33_execute(int cycles) {
        pendingCycles = cycles;
        while (pendingCycles > 0) {
            int prevCycles = pendingCycles;
            if (I.pending_irq != 0 && I.no_interrupt == 0) {
                if ((I.pending_irq & NMI_IRQ) != 0) {
                    external_int();
                } else if (I.IF) {
                    external_int();
                }
            }
            if (I.no_interrupt != 0) {
                I.no_interrupt--;
            }
            iNOP = fetchop();
            nec_instruction[iNOP].run();
            int delta = prevCycles - pendingCycles;
            totalExecutedCycles += delta;
        }
        return cycles - pendingCycles;
    }
}
