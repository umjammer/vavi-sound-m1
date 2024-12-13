/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m6800.M6800;
import m1.emu.Attotime.Atime;
import m1.mame.konami68000.Konami68000;
import m1.mame.m72.M72;
import m1.mame.m92.M92;
import m1.mame.neogeo.Neogeo;
import m1.mame.taito.Taito;
import m1.sound.ICS2115;
import m1.sound.K054539;
import m1.sound.MSM5205;
import m1.sound.Sound;
import m1.sound.Upd7759;
import m1.sound.YM2151;
import m1.sound.YM2203;
import m1.sound.YM2610;
import m1.sound.YM3812;

import static java.lang.System.getLogger;


public class Timer {

    private static final Logger logger = getLogger(Timer.class.getName());

    public static List<EmuTimer> lt;
    private static List<emu_timer2> lt2;
    public static Atime global_basetime;
    public static Atime global_basetime_obj;
    private static boolean callback_timer_modified;
    private static EmuTimer callback_timer;
    private static Atime callback_timer_expire_time;

    public interface timer_fired_func extends Runnable {

    }

    public static Runnable setvector;

    public static class EmuTimer {

        public Runnable action;
        public String func;
        public boolean enabled;
        public boolean temporary;
        public Atime period;
        public Atime start;
        public Atime expire;

        public EmuTimer() {
        }
    }

    public static class emu_timer2 {

        public final int index;
        public final Runnable action;
        public final String func;

        public emu_timer2(int i1, Runnable ac1, String func1) {
            index = i1;
            action = ac1;
            func = func1;
        }
    }

    public static Runnable getActionByIndex(int index) {
        Runnable action = null;
        for (emu_timer2 timer : lt2) {
            if (timer.index == index) {
                action = timer.action;
                if (index == 4) {
                    action = Sound.sound_update;
                } else if (index == 32) {
                    action = M6800.action_rx;
                } else if (index == 33) {
                    action = M6800.action_tx;
                } else if (index == 39) {
                    action = setvector;
                } else if (index == 42) {
                    action = Cpuexec.vblank_interrupt2;
                }
            }
        }
        return action;
    }

    public static String getFuncByIndex(int index) {
        String func = "";
        for (emu_timer2 timer : lt2) {
            if (timer.index == index) {
                func = timer.func;
                break;
            }
        }
        return func;
    }

    public static int getIndexByFunc(String func) {
        int index = 0;
        for (emu_timer2 timer : lt2) {
            if (timer.func.equals(func)) {
                index = timer.index;
                break;
            }
        }
        return index;
    }

    public static void timer_init() {
        global_basetime = Attotime.ATTOTIME_ZERO;
        lt = new ArrayList<>();
        lt2 = new ArrayList<>();
        lt2.add(new emu_timer2(1, Video::vblank_begin_callback, "vblank_begin_callback"));
        lt2.add(new emu_timer2(2, Mame::soft_reset, "soft_reset"));
        lt2.add(new emu_timer2(3, Cpuint::cpunum_empty_event_queue, "cpunum_empty_event_queue"));
        lt2.add(new emu_timer2(4, Sound.sound_update, "sound_update"));
        lt2.add(new emu_timer2(5, Watchdog::watchdog_callback, "watchdog_callback"));
        lt2.add(new emu_timer2(6, Generic::irq_1_0_line_hold, "irq_1_0_line_hold"));
        lt2.add(new emu_timer2(7, Video::vblank_end_callback, "vblank_end_callback"));

        lt2.add(new emu_timer2(10, YM2151::irqAon_callback, "irqAon_callback"));
        lt2.add(new emu_timer2(11, YM2151::irqBon_callback, "irqBon_callback"));
        lt2.add(new emu_timer2(12, YM2151::irqAoff_callback, "irqAoff_callback"));
        lt2.add(new emu_timer2(13, YM2151::irqBoff_callback, "irqBoff_callback"));
        lt2.add(new emu_timer2(14, YM2151::timer_callback_a, "timer_callback_a"));
        lt2.add(new emu_timer2(15, YM2151::timer_callback_b, "timer_callback_b"));
        lt2.add(new emu_timer2(16, Cpuexec::trigger_partial_frame_interrupt, "trigger_partial_frame_interrupt"));
        lt2.add(new emu_timer2(17, Cpuexec::null_callback, "boost_callback"));
        lt2.add(new emu_timer2(18, Cpuexec::end_interleave_boost, "end_interleave_boost"));
        lt2.add(new emu_timer2(19, Video::scanline0_callback, "scanline0_callback"));
        lt2.add(new emu_timer2(20, Sound::latch_callback, "latch_callback"));
        lt2.add(new emu_timer2(21, Sound::latch_callback2, "latch_callback2"));
        lt2.add(new emu_timer2(22, Sound::latch_callback3, "latch_callback3"));
        lt2.add(new emu_timer2(23, Sound::latch_callback4, "latch_callback4"));
        lt2.add(new emu_timer2(24, Neogeo::display_position_interrupt_callback, "display_position_interrupt_callback"));
        lt2.add(new emu_timer2(25, Neogeo::display_position_vblank_callback, "display_position_vblank_callback"));
        lt2.add(new emu_timer2(26, Neogeo::vblank_interrupt_callback, "vblank_interrupt_callback"));
        lt2.add(new emu_timer2(27, Neogeo::auto_animation_timer_callback, "auto_animation_timer_callback"));
        lt2.add(new emu_timer2(29, YM2610.F2610::timer_callback_0, "timer_callback_0"));
        lt2.add(new emu_timer2(30, YM2610.F2610::timer_callback_1, "timer_callback_1"));
        lt2.add(new emu_timer2(31, Neogeo::sprite_line_timer_callback, "sprite_line_timer_callback"));
        lt2.add(new emu_timer2(32, M6800.action_rx, "m6800_rx_tick"));
        lt2.add(new emu_timer2(33, M6800.action_tx, "m6800_tx_tick"));
        lt2.add(new emu_timer2(34, YM3812::timer_callback_3812_0, "timer_callback_3812_0"));
        lt2.add(new emu_timer2(35, YM3812::timer_callback_3812_1, "timer_callback_3812_1"));
        lt2.add(new emu_timer2(36, ICS2115::timer_cb_0, "timer_cb_0"));
        lt2.add(new emu_timer2(37, ICS2115::timer_cb_1, "timer_cb_1"));
        lt2.add(new emu_timer2(38, M72::m72_scanline_interrupt, "m72_scanline_interrupt"));
        lt2.add(new emu_timer2(39, setvector, "setvector_callback"));
        lt2.add(new emu_timer2(40, M92::m92_scanline_interrupt, "m92_scanline_interrupt"));
        lt2.add(new emu_timer2(41, Cpuexec::cpu_timeslicecallback, "cpu_timeslicecallback"));
        lt2.add(new emu_timer2(42, Cpuexec.vblank_interrupt2, "vblank_interrupt2"));
        lt2.add(new emu_timer2(43, Konami68000::nmi_callback, "nmi_callback"));
        lt2.add(new emu_timer2(44, Upd7759::upd7759_slave_update, "upd7759_slave_update"));
        lt2.add(new emu_timer2(45, Generic::irq_2_0_line_hold, "irq_2_0_line_hold"));
        lt2.add(new emu_timer2(46, MSM5205::MSM5205_vclk_callback0, "msm5205_vclk_callback0"));
        lt2.add(new emu_timer2(47, MSM5205::MSM5205_vclk_callback1, "msm5205_vclk_callback1"));
        lt2.add(new emu_timer2(48, YM2203::timer_callback_2203_0_0, "timer_callback_2203_0_0"));
        lt2.add(new emu_timer2(49, YM2203::timer_callback_2203_0_1, "timer_callback_2203_0_1"));
        lt2.add(new emu_timer2(50, YM2203::timer_callback_2203_1_0, "timer_callback_2203_1_0"));
        lt2.add(new emu_timer2(51, YM2203::timer_callback_2203_1_1, "timer_callback_2203_1_1"));
        lt2.add(new emu_timer2(52, YM3812::timer_callback_3526_0, "timer_callback_3526_0"));
        lt2.add(new emu_timer2(53, YM3812::timer_callback_3526_1, "timer_callback_3526_1"));
        lt2.add(new emu_timer2(54, K054539::k054539_irq, "k054539_irq"));
        lt2.add(new emu_timer2(55, Taito::cchip_timer, "cchip_timer"));
    }

    public static Atime getCurrentTime() {
//logger.log(Level.INFO, "time.callback_timer: %s, Cpuexec.activecpu: %d, Cpuexec.ncpu: %d, global_basetime: %s".formatted(callback_timer, Cpuexec.activecpu, Cpuexec.ncpu, global_basetime));
        if (callback_timer != null) {
            return callback_timer_expire_time;
        }
        if (Cpuexec.activecpu >= 0 && Cpuexec.activecpu < Cpuexec.ncpu) {
            return Cpuexec.cpunum_get_localtime(Cpuexec.activecpu);
        }
        return global_basetime;
    }

    public static void remove(EmuTimer timer1) {
        if (timer1 == callback_timer) {
            callback_timer_modified = true;
        }
        removeList(timer1);
    }

    public static void adjustPeriodic(EmuTimer which, Atime start_delay, Atime period) {
        Atime time = getCurrentTime();
        if (which == callback_timer) {
            callback_timer_modified = true;
        }
        which.enabled = true;
        if (start_delay.seconds < 0) {
            start_delay = Attotime.ATTOTIME_ZERO;
        }
        which.start = time;
        which.expire = Attotime.attotime_add(time, start_delay);
        which.period = period;
        removeList(which);
        insertList(which);
        if (lt.indexOf(which) == 0) {
            if (Cpuexec.activecpu >= 0 && Cpuexec.activecpu < Cpuexec.ncpu) {
                Cpuexec.activecpu_abort_timeslice(Cpuexec.activecpu);
            }
        }
    }

    public static void pulseInternal(Atime period, Runnable action, String func) {
        EmuTimer timer = allocCommon(action, func, false);
        adjustPeriodic(timer, period, period);
    }

    public static void setInternal(Runnable action, String func) {
        EmuTimer timer = allocCommon(action, func, true);
        adjustPeriodic(timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
    }

    public static void insertList(EmuTimer timer1) {
        int i;
        int i1 = -1;
        if (timer1.func.equals("cpunum_empty_event_queue") || timer1.func.equals("setvector_callback")) {
            for (EmuTimer et : lt) {
                if (et.func.equals(timer1.func) && Attotime.attotime_compare(et.expire, global_basetime) <= 0) {
                    i1 = lt.indexOf(et);
                    break;
                }
            }
        }
        if (i1 == -1) {
            Atime expire = timer1.enabled ? timer1.expire : Attotime.ATTOTIME_NEVER;
            for (i = 0; i < lt.size(); i++) {
                if (Attotime.attotime_compare(lt.get(i).expire, expire) > 0) {
                    break;
                }
            }
            lt.add(i, timer1);
        }
    }

    public static void removeList(EmuTimer timer1) {
        if (timer1.func.equals("cpunum_empty_event_queue") || timer1.func.equals("setvector_callback")) {
            List<EmuTimer> lt1 = new ArrayList<>();
            for (EmuTimer et : lt) {
                if (et.func.equals(timer1.func) && Attotime.attotime_compare(et.expire, timer1.expire) == 0) {
                    lt1.add(et);
//                    lt.remove(et);
//                    break;
                } else if (et.func.equals(timer1.func) && Attotime.attotime_compare(et.expire, timer1.expire) < 0) {
                    int i1 = 1;
                } else if (et.func.equals(timer1.func) && Attotime.attotime_compare(et.expire, timer1.expire) > 0) {
                    int i1 = 1;
                }
            }
            for (EmuTimer et1 : lt1) {
                lt.remove(et1);
            }
        } else {
            for (EmuTimer et : lt) {
                if (et.func.equals(timer1.func)) {
                    lt.remove(et);
                    break;
                }
            }
        }
    }

//    public static void sort() {
//        int i1, i2, n1;
//        Atime expire1, expire2;
//        n1 = lt.Count;
//        for (i2 = 1; i2 < n1; i2++) {
//            for (i1 = 0; i1 < i2; i1++) {
//                if (lt[i1].enabled == true) {
//                    expire1 = lt[i1].expire;
//                } else {
//                    expire1 = Attotime.ATTOTIME_NEVER;
//                }
//                if (lt[i2].enabled == true) {
//                    expire2 = lt[i2].expire;
//                } else {
//                    expire2 = Attotime.ATTOTIME_NEVER;
//                }
//                if (Attotime.attotime_compare(expire1, expire2) > 0) {
//                    var temp = lt[i1];
//                    lt[i1] = lt[i2];
//                    lt[i2] = temp;
//                }
//            }
//        }
//    }

    public static void setGlobalTime(Atime newbase) {
        EmuTimer timer;
        global_basetime = newbase;
logger.log(Level.DEBUG, "new global_basetime: " + global_basetime);

        while (Attotime.attotime_compare(lt.get(0).expire, global_basetime) <= 0) {
            boolean was_enabled = lt.get(0).enabled;
            timer = lt.get(0);
            if (Attotime.attotime_compare(timer.period, Attotime.ATTOTIME_ZERO) == 0 || Attotime.attotime_compare(timer.period, Attotime.ATTOTIME_NEVER) == 0) {
                timer.enabled = false;
            }
            callback_timer_modified = false;
            callback_timer = timer;
            callback_timer_expire_time = timer.expire;
            Runnable a = Cpuexec::null_callback;
//logger.log(Level.TRACE, timer.action.hashCode() + ", " + a.hashCode());
            if (was_enabled && (timer.action != null && timer.action.hashCode() != a.hashCode())) { // TODO works???
                timer.action.run();
            }
            callback_timer = null;
            if (!callback_timer_modified) {
                if (timer.temporary) {
                    removeList(timer);
                } else {
                    timer.start = timer.expire;
                    timer.expire = Attotime.attotime_add(timer.expire, timer.period);
                    removeList(timer);
                    insertList(timer);
                }
            }
        }
    }

    public static EmuTimer allocCommon(Runnable action, String func, boolean temp) {
        Atime time = getCurrentTime();
        EmuTimer timer = new EmuTimer();
        timer.action = action;
        timer.enabled = false;
        timer.temporary = temp;
        timer.period = Attotime.ATTOTIME_ZERO;
        timer.func = func;
        timer.start = time;
        timer.expire = Attotime.ATTOTIME_NEVER;
        insertList(timer);
        return timer;
    }

    public static boolean enable(EmuTimer which, boolean enable) {
        boolean old;
        old = which.enabled;
        which.enabled = enable;
        removeList(which);
        insertList(which);
        return old;
    }

    public static boolean enabled(EmuTimer which) {
        return which.enabled;
    }

    public static Atime timeLeft(EmuTimer which) {
        return Attotime.attotime_sub(which.expire, getCurrentTime());
    }

    public static void saveStateBinary(BinaryWriter writer) {
        int i, i1, n;
        n = lt.size();
        writer.write(n);
        for (i = 0; i < n; i++) {
            i1 = getIndexByFunc(lt.get(i).func);
            writer.write(i1);
            writer.write(lt.get(i).enabled);
            writer.write(lt.get(i).temporary);
            writer.write(lt.get(i).period.seconds);
            writer.write(lt.get(i).period.attoseconds);
            writer.write(lt.get(i).start.seconds);
            writer.write(lt.get(i).start.attoseconds);
            writer.write(lt.get(i).expire.seconds);
            writer.write(lt.get(i).expire.attoseconds);
        }
        for (i = n; i < 32; i++) {
            writer.write(0);
            writer.write(false);
            writer.write(false);
            writer.write(0);
            writer.write((long) 0);
            writer.write(0);
            writer.write((long) 0);
            writer.write(0);
            writer.write((long) 0);
        }
    }

    public static void loadStateBinary(BinaryReader reader) throws IOException {
        int i, i1, n;
        n = reader.readInt32();
        lt = new ArrayList<>();
        for (i = 0; i < n; i++) {
            lt.add(new EmuTimer());
            i1 = reader.readInt32();
            lt.get(i).action = getActionByIndex(i1);
            lt.get(i).func = getFuncByIndex(i1);
            lt.get(i).enabled = reader.readBoolean();
            lt.get(i).temporary = reader.readBoolean();
            lt.get(i).period.seconds = reader.readInt32();
            lt.get(i).period.attoseconds = reader.readInt64();
            lt.get(i).start.seconds = reader.readInt32();
            lt.get(i).start.attoseconds = reader.readInt64();
            lt.get(i).expire.seconds = reader.readInt32();
            lt.get(i).expire.attoseconds = reader.readInt64();
            if (lt.get(i).func.equals("vblank_begin_callback")) {
                Video.vblank_begin_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Video.vblank_begin_timer);
            } else if (lt.get(i).func.equals("vblank_end_callback")) {
                Video.vblank_end_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Video.vblank_end_timer);
            } else if (lt.get(i).func.equals("soft_reset")) {
                Mame.soft_reset_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Mame.soft_reset_timer);
            } else if (lt.get(i).func.equals("watchdog_callback")) {
                Watchdog.watchdog_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Watchdog.watchdog_timer);
            } else if (lt.get(i).func.equals("irq_1_0_line_hold")) {
                Cpuexec.timedint_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Cpuexec.timedint_timer);
            } else if (lt.get(i).func.equals("timer_callback_a")) {
                YM2151.PSG.timer_A = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2151.PSG.timer_A);
            } else if (lt.get(i).func.equals("timer_callback_b")) {
                YM2151.PSG.timer_B = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2151.PSG.timer_B);
            } else if (lt.get(i).func.equals("trigger_partial_frame_interrupt")) {
                switch (Machine.sBoard) {
                    case "CPS2":
                    case "IGS011":
                    case "Konami68000":
                        Cpuexec.cpu[0].partial_frame_timer = lt.get(i);
                        lt.remove(lt.get(i));
                        lt.add(Cpuexec.cpu[0].partial_frame_timer);
                        break;
                    case "M72":
                        Cpuexec.cpu[1].partial_frame_timer = lt.get(i);
                        lt.remove(lt.get(i));
                        lt.add(Cpuexec.cpu[1].partial_frame_timer);
                        break;
                    case "Capcom":
                        switch (Machine.sName) {
                            case "gng":
                            case "gnga":
                            case "gngbl":
                            case "gngprot":
                            case "gngblita":
                            case "gngc":
                            case "gngt":
                            case "makaimur":
                            case "makaimurc":
                            case "makaimurg":
                            case "diamond":
                                Cpuexec.cpu[1].partial_frame_timer = lt.get(i);
                                lt.remove(lt.get(i));
                                lt.add(Cpuexec.cpu[1].partial_frame_timer);
                                break;
                        }
                        break;
                }
            } else if (lt.get(i).func.equals("boost_callback")) {
                Cpuexec.interleave_boost_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Cpuexec.interleave_boost_timer);
            } else if (lt.get(i).func.equals("end_interleave_boost")) {
                Cpuexec.interleave_boost_timer_end = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Cpuexec.interleave_boost_timer_end);
            } else if (lt.get(i).func.equals("scanline0_callback")) {
                Video.scanline0_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Video.scanline0_timer);
            } else if (lt.get(i).func.equals("display_position_interrupt_callback")) {
                Neogeo.display_position_interrupt_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Neogeo.display_position_interrupt_timer);
            } else if (lt.get(i).func.equals("display_position_vblank_callback")) {
                Neogeo.display_position_vblank_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Neogeo.display_position_vblank_timer);
            } else if (lt.get(i).func.equals("vblank_interrupt_callback")) {
                Neogeo.vblank_interrupt_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Neogeo.vblank_interrupt_timer);
            } else if (lt.get(i).func.equals("auto_animation_timer_callback")) {
                Neogeo.auto_animation_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Neogeo.auto_animation_timer);
            } else if (lt.get(i).func.equals("sprite_line_timer_callback")) {
                Neogeo.sprite_line_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Neogeo.sprite_line_timer);
            } else if (lt.get(i).func.equals("timer_callback_0")) {
                YM2610.timer[0] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2610.timer[0]);
            } else if (lt.get(i).func.equals("timer_callback_1")) {
                YM2610.timer[1] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2610.timer[1]);
            } else if (lt.get(i).func.equals("m6800_rx_tick")) {
                M6800.m1.m6800_rx_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(M6800.m1.m6800_rx_timer);
            } else if (lt.get(i).func.equals("m6800_tx_tick")) {
                M6800.m1.m6800_tx_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(M6800.m1.m6800_tx_timer);
            } else if (lt.get(i).func.equals("timer_callback_3812_0")) {
                YM3812.timer[0] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM3812.timer[0]);
            } else if (lt.get(i).func.equals("timer_callback_3812_1")) {
                YM3812.timer[1] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM3812.timer[1]);
            } else if (lt.get(i).func.equals("timer_cb_0")) {
                ICS2115.timer[0].timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(ICS2115.timer[0].timer);
            } else if (lt.get(i).func.equals("timer_cb_1")) {
                ICS2115.timer[1].timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(ICS2115.timer[1].timer);
            } else if (lt.get(i).func.equals("m72_scanline_interrupt")) {
                M72.scanline_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(M72.scanline_timer);
            } else if (lt.get(i).func.equals("m92_scanline_interrupt")) {
                M92.scanline_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(M92.scanline_timer);
            } else if (lt.get(i).func.equals("cpu_timeslicecallback")) {
                Cpuexec.timeslice_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Cpuexec.timeslice_timer);
            } else if (lt.get(i).func.equals("upd7759_slave_update")) {
                Upd7759.chip.timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Upd7759.chip.timer);
            } else if (lt.get(i).func.equals("irq_2_0_line_hold")) {
                Cpuexec.timedint_timer = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(Cpuexec.timedint_timer);
            } else if (lt.get(i).func.equals("msm5205_vclk_callback0")) {
                MSM5205.timer[0] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(MSM5205.timer[0]);
            } else if (lt.get(i).func.equals("msm5205_vclk_callback1")) {
                MSM5205.timer[1] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(MSM5205.timer[1]);
            } else if (lt.get(i).func.equals("timer_callback_2203_0_0")) {
                YM2203.FF2203[0].timer[0] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2203.FF2203[0].timer[0]);
            } else if (lt.get(i).func.equals("timer_callback_2203_0_1")) {
                YM2203.FF2203[0].timer[1] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2203.FF2203[0].timer[1]);
            } else if (lt.get(i).func.equals("timer_callback_2203_1_0")) {
                YM2203.FF2203[1].timer[0] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2203.FF2203[1].timer[0]);
            } else if (lt.get(i).func.equals("timer_callback_2203_1_1")) {
                YM2203.FF2203[1].timer[1] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM2203.FF2203[1].timer[1]);
            } else if (lt.get(i).func.equals("timer_callback_3526_0")) {
                YM3812.timer[0] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM3812.timer[0]);
            } else if (lt.get(i).func.equals("timer_callback_3526_1")) {
                YM3812.timer[1] = lt.get(i);
                lt.remove(lt.get(i));
                lt.add(YM3812.timer[1]);
            }
        }
        for (i = n; i < 32; i++) {
            reader.readInt32();
            reader.readBoolean();
            reader.readBoolean();
            reader.readInt32();
            reader.readInt64();
            reader.readInt32();
            reader.readInt64();
            reader.readInt32();
            reader.readInt64();
        }
    }
}
