/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.cpu.z80;


public class Tables {

//#region Table

    public static void InitialiseTables() {
        InitTableInc();
        InitTableDec();
        InitTableParity();
        InitTableALU();
        InitTableRotShift();
        InitTableHalfBorrow();
        InitTableHalfCarry();
        InitTableNeg();
        InitTableDaa();
        InitTableCc();
    }

    static byte[] TableInc;

    private static void InitTableInc() {
        TableInc = new byte[256];
        for (int i = 0; i < 256; ++i)
            TableInc[i] = FlagByte(false, false, i == 0x80, UndocumentedX(i), (i & 0xF) == 0x0, UndocumentedY(i), i == 0, i > 127);
    }

    static byte[] TableDec;

    private static void InitTableDec() {
        TableDec = new byte[256];
        for (int i = 0; i < 256; ++i)
            TableDec[i] = FlagByte(false, true, i == 0x7F, UndocumentedX(i), (i & 0xF) == 0xF, UndocumentedY(i), i == 0, i > 127);
    }

    static boolean[] TableParity;

    private static void InitTableParity() {
        TableParity = new boolean[256];
        for (int i = 0; i < 256; ++i) {
            int Bits = 0;
            for (int j = 0; j < 8; ++j) {
                Bits += (i >> j) & 1;
            }
            TableParity[i] = (Bits & 1) == 0;
        }
    }

    static short[][][][] TableALU;

    private static void InitTableALU() {
        TableALU = new short[8][256][256][2]; // Class, OP1, OP2, Carry

        for (int i = 0; i < 8; ++i) {
            for (int op1 = 0; op1 < 256; ++op1) {
                for (int op2 = 0; op2 < 256; ++op2) {
                    for (int c = 0; c < 2; ++c) {

                        int ac = (i == 1 || i == 3) ? c : 0;

                        boolean S = false;
                        boolean Z = false;
                        boolean C = false;
                        boolean H = false;
                        boolean N = false;
                        boolean P = false;

                        byte result_b = 0;
                        int result_si = 0;
                        int result_ui = 0;

                        // Fetch result
                        switch (i) {
                            case 0:
                            case 1:
                                result_si = (byte) op1 + (byte) op2 + ac;
                                result_ui = op1 + op2 + ac;
                                break;
                            case 2:
                            case 3:
                            case 7:
                                result_si = (byte) op1 - (byte) op2 - ac;
                                result_ui = op1 - op2 - ac;
                                break;
                            case 4:
                                result_si = op1 & op2;
                                break;
                            case 5:
                                result_si = op1 ^ op2;
                                break;
                            case 6:
                                result_si = op1 | op2;
                                break;
                        }

                        result_b = (byte) result_si;

                        // Parity/Carry

                        switch (i) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 7:
                                P = result_si < -128 || result_si > 127;
                                C = result_ui < 0 || result_ui > 255;
                                break;
                            case 4:
                            case 5:
                            case 6:
                                P = TableParity[result_b & 0xff];
                                C = false;
                                break;
                        }

                        // Subtraction
                        N = i == 2 || i == 3 || i == 7;

                        // Half carry
                        switch (i) {
                            case 0:
                            case 1:
                                H = ((op1 & 0xF) + (op2 & 0xF) + (ac & 0xF)) > 0xF;
                                break;
                            case 2:
                            case 3:
                            case 7:
                                H = ((op1 & 0xF) - (op2 & 0xF) - (ac & 0xF)) < 0x0;
                                break;
                            case 4:
                                H = true;
                                break;
                            case 5:
                            case 6:
                                H = false;
                                break;
                        }

                        // Undocumented
                        byte UndocumentedFlags = (byte) (result_b & 0x28);
                        if (i == 7) UndocumentedFlags = (byte) (op2 & 0x28);

                        S = (result_b & 0xff) > 127;
                        Z = result_b == 0;

                        if (i == 7) result_b = (byte) op1;

                        TableALU[i][op1][op2][c] = (short) (
                                result_b * 256 +
                                        ((C ? 0x01 : 0) + (N ? 0x02 : 0) + (P ? 0x04 : 0) + (H ? 0x10 : 0) + (Z ? 0x40 : 0) + (S ? 0x80 : 0)) +
                                        (UndocumentedFlags));

                    }
                }
            }
        }
    }

    static boolean[][] TableHalfBorrow;

    private static void InitTableHalfBorrow() {
        TableHalfBorrow = new boolean[256][256];
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                TableHalfBorrow[i][j] = ((i & 0xF) - (j & 0xF)) < 0;
            }
        }
    }

    static boolean[][] TableHalfCarry;

    private static void InitTableHalfCarry() {
        TableHalfCarry = new boolean[256][256];
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                TableHalfCarry[i][j] = ((i & 0xF) + (j & 0xF)) > 0xF;
            }
        }
    }

    static short[][][] TableRotShift;

    private static void InitTableRotShift() {
        TableRotShift = new short[2][8][65536]; // All, operation, AF
        for (int all = 0; all < 2; all++) {
            for (int y = 0; y < 8; ++y) {
                for (int af = 0; af < 65536; af++) {
                    byte Old = (byte) (af >> 8);
                    boolean OldCarry = (af & 0x01) != 0;

                    short newAf = (short) (af & ~(0x13)); // Clear HALF-CARRY, SUBTRACT and CARRY flags

                    byte New = Old;
                    if ((y & 1) == 0) {
                        if ((Old & 0x80) != 0) ++newAf;

                        New <<= 1;

                        if ((y & 0x04) == 0) {
                            if (((y & 0x02) == 0) ? ((newAf & 0x01) != 0) : OldCarry) New |= 0x01;
                        } else {
                            if ((y & 0x02) != 0) New |= 0x01;
                        }

                    } else {

                        if ((Old & 0x01) != 0) ++newAf;

                        New >>= 1;

                        if ((y & 0x04) == 0) {
                            if (((y & 0x02) == 0) ? ((newAf & 0x01) != 0) : OldCarry) New |= (byte) 0x80;
                        } else {
                            if ((y & 0x02) == 0) New |= (byte) (Old & 0x80);
                        }
                    }

                    newAf &= 0xFF;
                    newAf |= (short) (New * 256);

                    if (all == 1) {
                        newAf &= (short) ~0xC4; // Clear S, Z, P
                        if ((New & 0xff) > 127) newAf |= 0x80;
                        if (New == 0) newAf |= 0x40;
                        if (TableParity[New & 0xff]) newAf |= 0x04;
                    }

                    TableRotShift[all][y][af] = (short) ((newAf & ~0x28) | ((newAf >> 8) & 0x28));
                }
            }
        }
    }

    static short[] TableNeg;

    private static void InitTableNeg() {
        TableNeg = new short[65536];
        for (int af = 0; af < 65536; af++) {
            short raf = 0;
            byte b = (byte) (af >> 8);
            byte a = (byte) -b;
            raf |= (short) (a * 256);
            raf |= FlagByte(b != 0x00, true, b == (byte) 0x80, UndocumentedX(a), TableHalfCarry[a & 0xff][b & 0xff],
                    UndocumentedY(a), a == 0, (a & 0xff) > 127);
            TableNeg[af] = raf;
        }
    }

    static short[] TableDaa;

    private static void InitTableDaa() {
        TableDaa = new short[65536];
        for (int af = 0; af < 65536; ++af) {
            byte a = (byte) (af >> 8);
            byte tmp = a;

            if (IsN(af)) {
                if (IsH(af) || ((a & 0x0F) > 0x09)) tmp -= 0x06;
                if (IsC(af) || (a & 0xff) > 0x99) tmp -= 0x60;
            } else {
                if (IsH(af) || ((a & 0x0F) > 0x09)) tmp += 0x06;
                if (IsC(af) || (a & 0xff) > 0x99) tmp += 0x60;
            }

            TableDaa[af] = (short) ((tmp * 256) + FlagByte(IsC(af) || (a & 0xff) > 0x99, IsN(af), TableParity[tmp & 0xff], UndocumentedX(tmp), ((a ^ tmp) & 0x10) != 0, UndocumentedY(tmp), tmp == 0, (tmp & 0xff) > 127));
        }
    }

    static int[] cc_op, cc_cb, cc_ed, cc_xy, cc_xycb, cc_ex;

    private static void InitTableCc() {
        cc_op = new int[] {
                4, 10, 7, 6, 4, 4, 7, 4, 4, 11, 7, 6, 4, 4, 7, 4,
                8, 10, 7, 6, 4, 4, 7, 4, 12, 11, 7, 6, 4, 4, 7, 4,
                7, 10, 16, 6, 4, 4, 7, 4, 7, 11, 16, 6, 4, 4, 7, 4,
                7, 10, 13, 6, 11, 11, 10, 4, 7, 11, 13, 6, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                7, 7, 7, 7, 7, 7, 4, 7, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
                5, 10, 10, 10, 10, 11, 7, 11, 5, 10, 10, 0, 10, 17, 7, 11,
                5, 10, 10, 11, 10, 11, 7, 11, 5, 4, 10, 11, 10, 0, 7, 11,
                5, 10, 10, 19, 10, 11, 7, 11, 5, 4, 10, 4, 10, 0, 7, 11,
                5, 10, 10, 4, 10, 11, 7, 11, 5, 6, 10, 4, 10, 0, 7, 11
        };
        cc_cb = new int[] {
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
                8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8
        };
        cc_ed = new int[] {
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                12, 12, 15, 20, 8, 14, 8, 9, 12, 12, 15, 20, 8, 14, 8, 9,
                12, 12, 15, 20, 8, 14, 8, 9, 12, 12, 15, 20, 8, 14, 8, 9,
                12, 12, 15, 20, 8, 14, 8, 18, 12, 12, 15, 20, 8, 14, 8, 18,
                12, 12, 15, 20, 8, 14, 8, 8, 12, 12, 15, 20, 8, 14, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                16, 16, 16, 16, 8, 8, 8, 8, 16, 16, 16, 16, 8, 8, 8, 8,
                16, 16, 16, 16, 8, 8, 8, 8, 16, 16, 16, 16, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
        };
        cc_xy = new int[] {
                4, 4, 4, 4, 4, 4, 4, 4, 4, 15, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 15, 4, 4, 4, 4, 4, 4,
                4, 14, 20, 10, 9, 9, 9, 4, 4, 15, 20, 10, 9, 9, 9, 4,
                4, 4, 4, 4, 23, 23, 19, 4, 4, 15, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                9, 9, 9, 9, 9, 9, 19, 9, 9, 9, 9, 9, 9, 9, 19, 9,
                19, 19, 19, 19, 19, 19, 4, 19, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 14, 4, 23, 4, 15, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 10, 4, 4, 4, 4, 4, 4
        };
        cc_xycb = new int[] {
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
                23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23
        };
        cc_ex = new int[] {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                5, 5, 5, 5, 0, 0, 0, 0, 5, 5, 5, 5, 0, 0, 0, 0,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
                6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2
        };
    }

    private static byte FlagByte(boolean C, boolean N, boolean P, boolean X, boolean H, boolean Y, boolean Z, boolean S) {
        return (byte) (
                (C ? 0x01 : 0) +
                        (N ? 0x02 : 0) +
                        (P ? 0x04 : 0) +
                        (X ? 0x08 : 0) +
                        (H ? 0x10 : 0) +
                        (Y ? 0x20 : 0) +
                        (Z ? 0x40 : 0) +
                        (S ? 0x80 : 0)
        );
    }

    public static boolean UndocumentedX(int value) {
        return (value & 0x08) != 0;
    }

    public static boolean UndocumentedY(int value) {
        return (value & 0x20) != 0;
    }

    public static boolean IsC(int value) {
        return (value & 0x01) != 0;
    }

    public static boolean IsN(int value) {
        return (value & 0x02) != 0;
    }

    public static boolean IsP(int value) {
        return (value & 0x04) != 0;
    }

    public static boolean IsX(int value) {
        return (value & 0x08) != 0;
    }

    public static boolean IsH(int value) {
        return (value & 0x10) != 0;
    }

    public static boolean IsY(int value) {
        return (value & 0x20) != 0;
    }

    public static boolean IsZ(int value) {
        return (value & 0x40) != 0;
    }

    public static boolean IsS(int value) {
        return (value & 0x80) != 0;
    }

//#endregion
}
