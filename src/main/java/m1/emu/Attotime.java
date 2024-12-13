/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;


public class Attotime {

    public static class Atime {

        public int seconds;
        public long attoseconds;

        public Atime(int i, long l) {
            seconds = i;
            attoseconds = l;
        }

        @Override public String toString() {
            String buffer = "";
            if (this.seconds >= Attotime.ATTOTIME_MAX_SECONDS)
                return "never";
            if (this.seconds < 0) {
                buffer += "-";
                this.seconds = -this.seconds;
            }
            buffer += String.format("%d.%-9d", this.seconds, this.attoseconds);
            return buffer;
        }
    }

    public static final int ATTOTIME_MAX_SECONDS = 1_000_000_000;
    public static final int ATTOSECONDS_PER_SECOND_SQRT = 1_000_000_000;
    public static final long ATTOSECONDS_PER_SECOND = (long) 1e18;
    public static final Atime ATTOTIME_ZERO = new Atime(0, 0);
    public static final Atime ATTOTIME_NEVER = new Atime(1_000_000_000, 0);
    public static final long ATTOSECONDS_PER_NANOSECOND = (long) 1e9;

    public static Atime ATTOTIME_IN_NSEC(long ns) {
        return new Atime((int) (ns / 1_000_000_000), (ns % 1_000_000_000) * ATTOSECONDS_PER_NANOSECOND);
    }

    public static Atime ATTOTIME_IN_HZ(int hz) {
        return new Atime(0, ATTOSECONDS_PER_SECOND / hz);
    }

    public static long attotime_to_attoseconds(Atime _time) {
        if (_time.seconds == 0) {
            return _time.attoseconds;
        } else if (_time.seconds == -1) {
            return _time.attoseconds - Attotime.ATTOSECONDS_PER_SECOND;
        } else if (_time.seconds > 0) {
            return Attotime.ATTOSECONDS_PER_SECOND;
        } else {
            return -Attotime.ATTOSECONDS_PER_SECOND;
        }
    }

    public static Atime attotime_add(Atime _time1, Atime _time2) {
        Atime result = ATTOTIME_ZERO;

        // if one of the items is attotime_never, return attotime_never
        if (_time1.seconds >= ATTOTIME_MAX_SECONDS || _time2.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;

        // add the seconds and attoseconds
        result.attoseconds = _time1.attoseconds + _time2.attoseconds;
        result.seconds = _time1.seconds + _time2.seconds;

        // normalize and return
        if (result.attoseconds >= ATTOSECONDS_PER_SECOND) {
            result.attoseconds -= ATTOSECONDS_PER_SECOND;
            result.seconds++;
        }

        // overflow
        if (result.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;
        return result;
    }

    public static Atime attotime_add_attoseconds(Atime _time1, long _attoseconds) {
        Atime result = ATTOTIME_ZERO;

        // if one of the items is attotime_never, return attotime_never
        if (_time1.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;

        // add the seconds and attoseconds
        result.attoseconds = _time1.attoseconds + _attoseconds;
        result.seconds = _time1.seconds;

        // normalize and return
        if (result.attoseconds >= ATTOSECONDS_PER_SECOND) {
            result.attoseconds -= ATTOSECONDS_PER_SECOND;
            result.seconds++;
        }

        // overflow
        if (result.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;
        return result;
    }

    public static Atime attotime_sub(Atime _time1, Atime _time2) {
        Atime result = ATTOTIME_ZERO;

        // if time1 is attotime_never, return attotime_never
        if (_time1.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;

        // add the seconds and attoseconds
        result.attoseconds = _time1.attoseconds - _time2.attoseconds;
        result.seconds = _time1.seconds - _time2.seconds;

        // normalize and return
        if (result.attoseconds < 0) {
            result.attoseconds += ATTOSECONDS_PER_SECOND;
            result.seconds--;
        }
        return result;
    }

    public static Atime attotime_sub_attoseconds(Atime _time1, long _attoseconds) {
        Atime result = ATTOTIME_ZERO;

        // if time1 is attotime_never, return attotime_never
        if (_time1.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;

        // add the seconds and attoseconds
        result.attoseconds = _time1.attoseconds - _attoseconds;
        result.seconds = _time1.seconds;

        // normalize and return
        if (result.attoseconds < 0) {
            result.attoseconds += ATTOSECONDS_PER_SECOND;
            result.seconds--;
        }
        return result;
    }

    public static Atime attotime_mul(Atime _time1, int factor) {
        int attohi;
        int[] attolo = new int[1], reslo = new int[1], reshi = new int[1];
        long temp;

        // if one of the items is attotime_never, return attotime_never
        if (_time1.seconds >= ATTOTIME_MAX_SECONDS)
            return ATTOTIME_NEVER;

        // 0 times anything is zero
        if (factor == 0)
            return ATTOTIME_ZERO;

        // split attoseconds into upper and lower halves which fit into 32 bits
        attohi = divu_64x32_rem(_time1.attoseconds, 1_000_000_000, /* out */ attolo);

        // scale the lower half, then split into high/low parts
        temp = mulu_32x32(attolo[0], factor);
        temp = divu_64x32_rem(temp, 1_000_000_000, /* out */ reslo);

        // scale the upper half, then split into high/low parts
        temp += mulu_32x32(attohi, factor);
        temp = divu_64x32_rem(temp, 1_000_000_000, /* out */ reshi);

        // scale the seconds
        temp += mulu_32x32(_time1.seconds, factor);
        if (temp >= 1_000_000_000)
            return ATTOTIME_NEVER;

        // build the result
        return new Atime((int) temp, (long) reslo[0] + mul_32x32(reshi[0], 1_000_000_000));
    }

    private static int divu_64x32_rem(long a, int b, /* out */ int[] remainder) {
        remainder[0] = (int) (a % (b & 0xffff_ffffL));
        return (int) (a / (b & 0xffff_ffffL));
    }

    private static long mulu_32x32(int a, int b) {
        return (a & 0xffff_ffffL) * (b & 0xffff_ffffL);
    }

    private static long mul_32x32(int a, int b) {
        return (long) a * (long) b;
    }

    public static Atime attotime_div(Atime _time1, int factor) {
        int attohi, reshi, reslo;
        int[] attolo = new int[1], remainder = new int[1];
        Atime result = new Atime(0, 0);
        long temp;

        // if one of the items is attotime_never, return attotime_never
        if (_time1.seconds >= ATTOTIME_MAX_SECONDS)
            return new Atime(ATTOTIME_MAX_SECONDS, 0);

        // ignore divide by zero
        if (factor == 0)
            return _time1;

        // split attoseconds into upper and lower halves which fit into 32 bits
        attohi = divu_64x32_rem(_time1.attoseconds, 1_000_000_000, /* out */ attolo);

        // divide the seconds and get the remainder
        result.seconds = divu_64x32_rem(_time1.seconds, factor, /* out */ remainder);

        // combine the upper half of attoseconds with the remainder and divide that
        temp = (long) attohi + mulu_32x32(remainder[0], 1_000_000_000);
        reshi = divu_64x32_rem(temp, factor, /* out */ remainder);

        // combine the lower half of attoseconds with the remainder and divide that
        temp = attolo[0] + mulu_32x32(remainder[0], 1_000_000_000);
        reslo = divu_64x32_rem(temp, factor, /* out */ remainder);

        // round based on the remainder
        result.attoseconds = (long) reslo + mulu_32x32(reshi, 1_000_000_000);
        if (remainder[0] >= factor / 2)
            if (++result.attoseconds >= ATTOSECONDS_PER_SECOND) {
                result.attoseconds = 0;
                result.seconds++;
            }
        return result;
    }

    public static int attotime_compare(Atime _time1, Atime _time2) {
        if (_time1.seconds > _time2.seconds)
            return 1;
        if (_time1.seconds < _time2.seconds)
            return -1;
        return Long.compare(_time1.attoseconds, _time2.attoseconds);
    }
}
