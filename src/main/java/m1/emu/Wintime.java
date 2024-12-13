/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;


public class Wintime {

    public static long ticks_per_second;

    public static void wintime_init() {
        ticks_per_second = 1000_000_000L;
    }

    public static long osd_ticks() {
        return System.nanoTime();
    }

    public static void osd_sleep(long duration) {
        int msec;
        msec = (int) (duration * 1000 / ticks_per_second);
        if (msec >= 2) {
            msec -= 2;
            try {
                Thread.sleep(msec);
            } catch (InterruptedException ignore) {
            }
        }
    }
}
