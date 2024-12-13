/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;


public class DAC {

    public static class dac_info {

        public Streams channel;
        public short output;
        public short[] UnsignedVolTable;
        public short[] SignedVolTable;
    }

    public static dac_info dac1;

    public static void DAC_update(int offset, int length) {
        short out1 = dac1.output;
        int i;
        for (i = 0; i < length; i++) {
            Sound.dacstream.streamoutput[0][offset + i] = out1;
        }
    }

    public static void dac_signed_data_w(int num, byte data) {
        short out1 = dac1.SignedVolTable[data];
        if (dac1.output != out1) {
            Sound.dacstream.stream_update();
            dac1.output = out1;
        }
    }

    public static void dac_signed_data_16_w(int num, short data) {
        short out1 = (short) ((int) data - 0x0_8000);
        if (dac1.output != out1) {
            Sound.dacstream.stream_update();
            dac1.output = out1;
        }
    }

    public static void DAC_build_voltable() {
        int i;
        for (i = 0; i < 256; i++) {
            dac1.UnsignedVolTable[i] = (short) (i * 0x101 / 2);
            dac1.SignedVolTable[i] = (short) (i * 0x101 - 0x8000);
        }
    }

    public static void dac_start() {
        dac1 = new dac_info();
        dac1.UnsignedVolTable = new short[256];
        dac1.SignedVolTable = new short[256];
        DAC_build_voltable();
        dac1.output = 0;
    }
}
