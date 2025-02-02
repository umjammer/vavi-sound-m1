/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import m1.mame.capcom.Capcom;
import m1.mame.cps.CPS;
import m1.mame.dataeast.Dataeast;
import m1.mame.igs011.IGS011;
import m1.mame.konami68000.Konami68000;
import m1.mame.m72.M72;
import m1.mame.m92.M92;
import m1.mame.namcos1.Namcos1;
import m1.mame.neogeo.Neogeo;
import m1.mame.pgm.PGM;
import m1.mame.taito.Taito;
import m1.mame.taitob.Taitob;
import m1.mame.tehkan.Tehkan;


public class Memory {

    public static byte[] mainrom, audiorom, mainram, audioram;

    public static void memory_reset() {
        switch (Machine.sBoard) {
            case "CPS-1":
            case "CPS-1(QSound)":
                CPS.sbyte0 = -1;
                CPS.short1 = -1;
                CPS.short2 = -1;
                CPS.sbyte3 = -1;
                break;
            case "CPS2":
                CPS.short0 = -1;
                CPS.short1 = -1;
                CPS.short2 = -1;
                break;
            case "Data East":
                Dataeast.byte1 = (byte) 0xff;
                Dataeast.byte2 = (byte) 0xff;
                break;
            case "Tehkan":
                Tehkan.byte0 = (byte) 0xff;
                Tehkan.byte1 = (byte) 0xff;
                Tehkan.byte2 = (byte) 0xff;
                break;
            case "Neo Geo":
                Neogeo.short0 = (short) 0xff00;
                Neogeo.short1 = -1;
                Neogeo.short2 = -1;
                Neogeo.short3 = 0x3f;
                Neogeo.short4 = (short) 0xffc0;
                Neogeo.short5 = (short) 0xffff;
                Neogeo.short6 = 0x03;
                break;
            case "Namco System 1":
                Namcos1.byte0 = (byte) 0xff;
                Namcos1.byte1 = (byte) 0xff;
                Namcos1.byte2 = (byte) 0xff;
                switch (Machine.sName) {
                    case "faceoff":
                        Namcos1.byte00 = (byte) 0xff;
                        Namcos1.byte01 = (byte) 0xff;
                        Namcos1.byte02 = (byte) 0xff;
                        Namcos1.byte03 = (byte) 0xff;
                        break;
                }
                break;
            case "IGS011":
                switch (Machine.sName) {
                    case "drgnwrld":
                    case "drgnwrldv30":
                    case "drgnwrldv21":
                    case "drgnwrldv21j":
                    case "drgnwrldv20j":
                    case "drgnwrldv10c":
                    case "drgnwrldv11h":
                    case "drgnwrldv40k":
                        IGS011.sbyte0 = -1;
                        IGS011.sbyte1 = -1;
                        IGS011.sbyte2 = -1;
                        IGS011.sbytec = -1;
                        break;
                    case "lhb":
                    case "lhbv33c":
                    case "dbc":
                    case "ryukobou":
                        IGS011.bkey0 = (byte) 0xff;
                        IGS011.bkey1 = (byte) 0xff;
                        IGS011.bkey2 = (byte) 0xff;
                        IGS011.bkey3 = (byte) 0xff;
                        IGS011.bkey4 = (byte) 0xff;
                        IGS011.sbytec = -1;
                        break;
                }
                break;
            case "PGM":
                PGM.short0 = -1;
                PGM.short1 = -1;
                PGM.short2 = -1;
                PGM.short3 = 0xff;
                PGM.short4 = 0;
                break;
            case "M72":
                M72.short0 = (short) 0xffff;
                M72.short1 = (short) 0xffff;
                break;
            case "M92":
                M92.short0 = (short) 0xffff;
                M92.short1 = (short) 0xff7f;
                M92.short2 = (short) 0xffff;
                break;
            case "Taito":
                Taito.sbyte0 = (byte) 0xf3;
                Taito.sbyte1 = -1;
                Taito.sbyte2 = -1;
                switch (Machine.sName) {
                    case "tokio":
                    case "tokioo":
                    case "tokiou":
                    case "tokiob":
                        Taito.sbyte0 = (byte) 0xd3;
                        break;
                    case "sboblbobl":
                        Taito.sbyte0 = (byte) 0x73;
                        break;
                    case "opwolf":
                        Taito.sbyte0 = (byte) 0xfc;
                        break;
                    case "opwolfp":
                        Taito.sbyte2 = 0x3e;
                        Taito.sbyte3 = 0;
                        break;
                }
                break;
            case "Taito B":
                Taitob.sbyte0 = -1;
                Taitob.sbyte1 = -1;
                Taitob.sbyte2 = -1;
                Taitob.sbyte3 = -1;
                Taitob.sbyte4 = -1;
                Taitob.sbyte5 = -1;
                break;
            case "Konami 68000":
                Konami68000.sbyte0 = -1;
                Konami68000.sbyte1 = -1;
                Konami68000.sbyte2 = -1;
                Konami68000.sbyte3 = -1;
                Konami68000.sbyte4 = -1;
                switch (Machine.sName) {
                    case "lgtnfght":
                    case "lgtnfghta":
                    case "lgtnfghtu":
                    case "trigon":
                        Konami68000.sbyte0 = (byte) 0xfb;
                        break;
                    case "prmrsocr":
                    case "prmrsocrj":
                        Konami68000.sbyte0 = (byte) 0xef;
                        Konami68000.bytee = (byte) 0xfe;
                        break;
                }
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
                        Capcom.bytes = (byte) 0xff;
                        Capcom.byte1 = (byte) 0xff;
                        Capcom.byte2 = (byte) 0xff;
                        break;
                    case "sf":
                        Capcom.short0 = -1;
                        Capcom.short1 = -1;
                        break;
                    case "sfua":
                    case "sfj":
                        Capcom.short1 = -1;
                        Capcom.short2 = -1;
                        Capcom.shortc = -1;
                        break;
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        Capcom.short0 = -1;
                        Capcom.shortc = -1;
                        Capcom.sbyte1 = 0;
                        Capcom.sbyte2 = 0;
                        Capcom.sbyte3 = 0;
                        Capcom.sbyte4 = 0;
                        break;
                }
                break;
        }
    }

    public static void memory_reset2() {
        switch (Machine.sBoard) {
            case "CPS-1":
            case "CPS-1(QSound)":
                CPS.sbyte0_old = 0;
                CPS.short1_old = 0;
                CPS.short2_old = 0;
                CPS.sbyte3_old = 0;
                break;
            case "CPS2":
                CPS.short0_old = 0;
                CPS.short1_old = 0;
                CPS.short2_old = 0;
                break;
            case "Data East":
                Dataeast.byte1_old = 0;
                Dataeast.byte2_old = 0;
                break;
            case "Tehkan":
                Tehkan.byte0_old = 0;
                Tehkan.byte1_old = 0;
                Tehkan.byte2_old = 0;
                break;
            case "Neo Geo":
                Neogeo.short0_old = 0;
                Neogeo.short1_old = 0;
                Neogeo.short2_old = 0;
                Neogeo.short3_old = 0;
                Neogeo.short4_old = 0;
                break;
            case "Namco System 1":
                Namcos1.byte0_old = 0;
                Namcos1.byte1_old = 0;
                Namcos1.byte2_old = 0;
                break;
            case "IGS011":
                IGS011.sbyte0_old = 0;
                IGS011.sbyte1_old = 0;
                IGS011.sbyte2_old = 0;
                IGS011.sbytec_old = 0;
                break;
            case "PGM":
                PGM.short0_old = 0;
                PGM.short1_old = 0;
                PGM.short2_old = 0;
                PGM.short3_old = 0;
                PGM.short4_old = 0;
                break;
            case "M72":
                M72.short0_old = 0;
                M72.short1_old = 0;
                break;
            case "M92":
                M92.short0_old = 0;
                M92.short1_old = 0;
                M92.short2_old = 0;
                break;
            case "Taito":
                Taito.sbyte0_old = 0;
                Taito.sbyte1_old = 0;
                Taito.sbyte2_old = 0;
                Taito.sbyte3_old = 0;
                break;
            case "Taito B":
                Taitob.dswb_old = 0;
                Taitob.sbyte0_old = 0;
                Taitob.sbyte1_old = 0;
                Taitob.sbyte2_old = 0;
                Taitob.sbyte3_old = 0;
                Taitob.sbyte4_old = 0;
                Taitob.sbyte5_old = 0;
                break;
            case "Konami 68000":
                Konami68000.sbyte0_old = 0;
                Konami68000.sbyte1_old = 0;
                Konami68000.sbyte2_old = 0;
                Konami68000.sbyte3_old = 0;
                Konami68000.sbyte4_old = 0;
                switch (Machine.sName) {
                    case "prmrsocr":
                    case "prmrsocrj":
                        Konami68000.bytee = 0;
                        break;
                }
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
                        Capcom.bytes_old = 0;
                        Capcom.byte1_old = 0;
                        Capcom.byte2_old = 0;
                        break;
                    case "sf":
                        Capcom.short0_old = 0;
                        Capcom.short1_old = 0;
                        Capcom.shorts_old = 0;
                        break;
                    case "sfua":
                    case "sfj":
                        Capcom.short1_old = 0;
                        Capcom.short2_old = 0;
                        Capcom.shortc_old = 0;
                        break;
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        Capcom.short0_old = 0;
                        Capcom.shortc_old = 0;
                        Capcom.sbyte1_old = 0;
                        Capcom.sbyte2_old = 0;
                        Capcom.sbyte3_old = 0;
                        Capcom.sbyte4_old = 0;
                        break;
                }
                break;
        }
    }
}
