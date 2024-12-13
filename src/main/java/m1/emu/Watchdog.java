/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import m1.emu.Attotime.Atime;
import m1.emu.Timer.EmuTimer;


public class Watchdog {

    public static boolean watchdog_enabled;
    public static EmuTimer watchdog_timer;
    public static Atime watchdog_time;

    public static void watchdog_init() {
        watchdog_timer = Timer.allocCommon(Watchdog::watchdog_callback, "watchdog_callback", false);
        switch (Machine.sBoard) {
            case "CPS-1":
            case "CPS-1(QSound)":
            case "CPS2":
            case "Data East":
            case "Tehkan":
            case "Namco System 1":
            case "IGS011":
            case "PGM":
            case "M72":
            case "M92":
            case "Taito":
            case "Taito B":
            case "Konami 68000":
            case "Capcom":
                watchdog_time = Attotime.ATTOTIME_ZERO;
                break;
            case "Neo Geo":
                watchdog_time = new Atime(0, (long) 128762e12);
                break;
        }
    }

    public static void watchdog_internal_reset() {
        watchdog_enabled = false;
        watchdog_reset();
        watchdog_enabled = true;
    }

    public static void watchdog_callback() {
        Mame.mame_schedule_soft_reset();
    }

    public static void watchdog_reset() {
        if (!watchdog_enabled) {
            Timer.adjustPeriodic(watchdog_timer, Attotime.ATTOTIME_NEVER, Attotime.ATTOTIME_NEVER);
        } else if (Attotime.attotime_compare(watchdog_time, Attotime.ATTOTIME_ZERO) != 0) {
            Timer.adjustPeriodic(watchdog_timer, watchdog_time, Attotime.ATTOTIME_NEVER);
        } else {
            Timer.adjustPeriodic(watchdog_timer, new Atime(3, 0), Attotime.ATTOTIME_NEVER);
        }
    }
}
