/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.awt.Color;
import java.util.function.BiConsumer;


public class Palette {

    public static int[] entry_color;
    public static float[] entry_contrast;
    private static int trans_int;
    private static int numcolors, numgroups;
    public static Color trans_color;
    public interface palette_delegate extends BiConsumer<Integer, Integer> {}
    public static palette_delegate palette_set_callback;

    public static void palette_init() {
        int index;
        switch (Machine.sBoard) {
            case "CPS-1":
            case "CPS-1(QSound)":
            case "CPS2":
                trans_color = Color.magenta;
                trans_int = trans_color.getRGB(); // TODO ARGB
                numcolors = 0xc00;
                palette_set_callback = Palette::palette_entry_set_color1;
                break;
            case "Data East":
                trans_color = Color.magenta;
                trans_int = trans_color.getRGB();
                numcolors = 0x200;
                palette_set_callback = Palette::palette_entry_set_color2;
                break;
            case "Tehkan":
                trans_color = Color.magenta;
                trans_int = trans_color.getRGB();
                numcolors = 0x100;
                palette_set_callback = Palette::palette_entry_set_color1;
                break;
            case "SunA8":
                trans_color = Color.black;
                trans_int = trans_color.getRGB();
                numcolors = 0x100;
                palette_set_callback = Palette::palette_entry_set_color2;
                break;
            case "Namco System 1":
                trans_color = Color.black;
                trans_int = trans_color.getRGB();
                numcolors = 0x2001;
                palette_set_callback = Palette::palette_entry_set_color1;
                break;
            case "IGS011":
                trans_color = Color.black;
                trans_int = trans_color.getRGB();
                numcolors = 0x800;
                palette_set_callback = Palette::palette_entry_set_color1;
                break;
            case "PGM":
                trans_color = Color.magenta;
                trans_int = trans_color.getRGB();
                numcolors = 0x901;
                palette_set_callback = Palette::palette_entry_set_color2;
                break;
            case "M72":
                trans_color = Color.black;
                trans_int = trans_color.getRGB();
                numcolors = 0x201;
                palette_set_callback = Palette::palette_entry_set_color1;
                break;
            case "M92":
                trans_color = Color.black;
                trans_int = trans_color.getRGB();
                numcolors = 0x801;
                palette_set_callback = Palette::palette_entry_set_color2;
                break;
            case "Taito":
                switch (Machine.sName) {
                    case "tokio":
                    case "tokioo":
                    case "tokiou":
                    case "tokiob":
                    case "bublbobl":
                    case "bublbobl1":
                    case "bublboblr":
                    case "bublboblr1":
                    case "boblbobl":
                    case "sboblbobl":
                    case "sboblbobla":
                    case "sboblboblb":
                    case "sboblbobld":
                    case "sboblboblc":
                    case "bub68705":
                    case "dland":
                    case "bbredux":
                    case "bublboblb":
                    case "bublcave":
                    case "boblcave":
                    case "bublcave11":
                    case "bublcave10":
                        trans_color = Color.magenta;
                        numcolors = 0x100;
                        break;
                    case "opwolf":
                    case "opwolfa":
                    case "opwolfj":
                    case "opwolfu":
                    case "opwolfb":
                    case "opwolfp":
                        trans_color = Color.black;
                        numcolors = 0x2000;
                        break;
                }
                trans_int = trans_color.getRGB();
                palette_set_callback = Palette::palette_entry_set_color2;
                break;
            case "Taito B":
                trans_color = Color.magenta;
                trans_int = trans_color.getRGB();
                numcolors = 0x1000;
                palette_set_callback = Palette::palette_entry_set_color3;
                break;
            case "Konami 68000":
                trans_color = Color.black;
                trans_int = trans_color.getRGB();
                numcolors = 0x800;
                palette_set_callback = Palette::palette_entry_set_color3;
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
                        trans_color = Color.black;
                        trans_int = trans_color.getRGB();
                        numcolors = 0x100;
                        palette_set_callback = Palette::palette_entry_set_color2;
                        break;
                    case "sf":
                    case "sfua":
                    case "sfj":
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        trans_color = Color.black;
                        trans_int = trans_color.getRGB();
                        numcolors = 0x400;
                        palette_set_callback = Palette::palette_entry_set_color3;
                        break;
                }
                break;
        }
        entry_color = new int[numcolors];
        entry_contrast = new float[numcolors];
        for (index = 0; index < numcolors; index++) {
            palette_set_callback.accept(index, make_argb(0xff, pal1bit((byte) (index >> 0)), pal1bit((byte) (index >> 1)), pal1bit((byte) (index >> 2))));
        }
        switch (Machine.sBoard) {
            case "SunA8":
                entry_color[0xff] = trans_int;
                break;
            case "Namco System 1":
                entry_color[0x2000] = trans_int;
                break;
            case "PGM":
                entry_color[0x900] = trans_int;
                break;
            case "M72":
                entry_color[0x200] = trans_int;
                break;
            case "M92":
                entry_color[0x800] = trans_int;
                break;
            case "Taito":
                switch (Machine.sName) {
                    case "tokio":
                    case "tokioo":
                    case "tokiou":
                    case "tokiob":
                    case "bublbobl":
                    case "bublbobl1":
                    case "bublboblr":
                    case "bublboblr1":
                    case "boblbobl":
                    case "sboblbobl":
                    case "sboblbobla":
                    case "sboblboblb":
                    case "sboblbobld":
                    case "sboblboblc":
                    case "bub68705":
                    case "dland":
                    case "bbredux":
                    case "bublboblb":
                    case "bublcave":
                    case "boblcave":
                    case "bublcave11":
                    case "bublcave10":
                        entry_color[0xff] = trans_int;
                        break;
                    case "opwolf":
                    case "opwolfa":
                    case "opwolfj":
                    case "opwolfu":
                    case "opwolfb":
                    case "opwolfp":
                        break;
                }
                break;
            case "Taito B":
                entry_color[0] = trans_int;
                break;
            case "Konami 68000":
                entry_color[0] = trans_int;
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
                        //entry_color[0] = trans_int;
                        break;
                    case "sf":
                    case "sfua":
                    case "sfj":
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        entry_color[0] = trans_int;
                        break;
                }
                break;
        }
    }

    public static void palette_entry_set_color1(int index, int rgb) {
        if (index >= numcolors || entry_color[index] == rgb) {
            return;
        }
        if (index % 0x10 == 0x0f && rgb == 0) {
            entry_color[index] = trans_int;
        } else {
            entry_color[index] = 0xff00_0000 | rgb;
        }
    }

    public static void palette_entry_set_color2(int index, int rgb) {
        if (index >= numcolors || entry_color[index] == rgb) {
            return;
        }
        entry_color[index] = 0xff00_0000 | rgb;
    }

    public static void palette_entry_set_color3(int index, int rgb) {
        if (index >= numcolors || entry_color[index] == rgb || index == 0) {
            return;
        }
        entry_color[index] = 0xff00_0000 | rgb;
    }

    public static void palette_entry_set_contrast(int index, float contrast) {
        int groupnum;
        if (index >= numcolors || entry_contrast[index] == contrast) {
            return;
        }
        entry_contrast[index] = contrast;
        for (groupnum = 0; groupnum < numgroups; groupnum++) {
            //update_adjusted_color(palette, groupnum, index);
        }
    }

    public static int make_rgb(int r, int g, int b) {
        return (((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
    }

    public static int make_argb(int a, int r, int g, int b) {
        return (((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
    }

    public static byte pal1bit(byte bits) {
        return (byte) (((bits & 1) != 0) ? 0xff : 0x00);
    }

    public static byte pal4bit(byte bits) {
        bits &= 0xf;
        return (byte) ((bits << 4) | bits);
    }

    public static byte pal5bit(byte bits) {
        bits &= 0x1f;
        return (byte) ((bits << 3) | (bits >> 2));
    }
}
