/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.m6805;

import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;


public class M68705 extends M6805 {

    public M68705() {
        m68705_init(Cpuint::cpu_3_irq_callback);
    }

    private void m68705_init(irq_delegate irqcallback) {
        irq_callback = irqcallback;
    }

    @Override
    public void Reset() {
        m68705_reset();
    }

    private void m68705_reset() {
        m6805_reset();
        subtype = SUBTYPE_M68705;
        RM16(0xfffe, /* ref */ pc);
    }

    @Override
    public void set_irq_line(int irqline, LineState state) {
        m68705_set_irq_line(irqline, state);
    }

    private void m68705_set_irq_line(int irqline, LineState state) {
        if (irq_state[irqline] == state.ordinal()) {
            return;
        }
        irq_state[irqline] = state.ordinal();
        if (state != LineState.CLEAR_LINE) {
            pending_interrupts |= (short) (1 << irqline);
        }
    }
}
