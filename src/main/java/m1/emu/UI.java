/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.awt.event.KeyEvent;
import javax.swing.JFrame;

import static m1.ui.MainForm.isWindowForeground;


public class UI {

    private static final int UI_FILLCOLOR = Palette.make_argb(0xe0, 0x10, 0x10, 0x30);

    public interface ui_delegate extends Runnable {}

    public static ui_delegate ui_handler_callback, ui_update_callback;
    public static boolean single_step;
    public static JFrame mainform;

    public static void ui_init(JFrame form1) {
        mainform = form1;
    }

    public static void ui_update_and_render() {
        ui_update_callback.run();
        ui_handler_callback.run();
    }

    public static void ui_updateC() {
        int i;
        int red, green, blue;
        if (single_step || Mame.paused) {
            byte bright = (byte) 0xa7;
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                red = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff_0000) >> 16) * bright / 0xff;
                green = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff00) >> 8) * bright / 0xff;
                blue = (Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff) * bright / 0xff;
                Video.bitmapcolor[i] = Palette.make_argb(0xff, red, green, blue);
            }
        } else {
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                Video.bitmapcolor[i] = Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]];
            }
        }
    }

    public static void ui_updateTehkan() {
        int i;
        int red, green, blue;
        if (single_step || Mame.paused) {
            byte bright = (byte) 0xa7;
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                if (Video.bitmapbase[Video.curbitmap][i] < 0x100) {
                    red = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff_0000) >> 16) * bright / 0xff;
                    green = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff00) >> 8) * bright / 0xff;
                    blue = (Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff) * bright / 0xff;
                    Video.bitmapcolor[i] = Palette.make_argb(0xff, red, green, blue);
                } else {
                    int i1 = 1;
                }
            }
        } else {
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                if (Video.bitmapbase[Video.curbitmap][i] < 0x100) {
                    Video.bitmapcolor[i] = Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]];
                } else {
                    Video.bitmapcolor[i] = Palette.entry_color[0];
                }
            }
        }
    }

    public static void ui_updateN() {
        int i;
        int red, green, blue;
        if (single_step || Mame.paused) {
            byte bright = (byte) 0xa7;
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                red = ((Video.bitmapbaseN[Video.curbitmap][i] & 0xff_0000) >> 16) * bright / 0xff;
                green = ((Video.bitmapbaseN[Video.curbitmap][i] & 0xff00) >> 8) * bright / 0xff;
                blue = (Video.bitmapbaseN[Video.curbitmap][i] & 0xff) * bright / 0xff;
                Video.bitmapcolor[i] = Palette.make_argb(0xff, red, green, blue);
            }
        } else {
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                Video.bitmapcolor[i] = 0xff00_0000 | Video.bitmapbaseN[Video.curbitmap][i];
            }
        }
    }

    public static void ui_updateNa() {
        int i;
        int red, green, blue;
        if (single_step || Mame.paused) {
            byte bright = (byte) 0xa7;
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                red = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff_0000) >> 16) * bright / 0xff;
                green = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff00) >> 8) * bright / 0xff;
                blue = (Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff) * bright / 0xff;
                Video.bitmapcolor[i] = Palette.make_argb(0xff, red, green, blue);
            }
        } else {
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                Video.bitmapcolor[i] = Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]];
            }
        }
    }

    public static void ui_updateIGS011() {
        int i;
        int red, green, blue;
        if (single_step || Mame.paused) {
            byte bright = (byte) 0xa7;
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                red = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff_0000) >> 16) * bright / 0xff;
                green = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff00) >> 8) * bright / 0xff;
                blue = (Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff) * bright / 0xff;
                Video.bitmapcolor[i] = Palette.make_argb(0xff, red, green, blue);
            }
        } else {
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                Video.bitmapcolor[i] = Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]];
            }
        }
    }

    public static void ui_updatePGM() {
        int i;
        int red, green, blue;
        if (single_step || Mame.paused) {
            byte bright = (byte) 0xa7;
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                red = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff_0000) >> 16) * bright / 0xff;
                green = ((Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff00) >> 8) * bright / 0xff;
                blue = (Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]] & 0xff) * bright / 0xff;
                Video.bitmapcolor[i] = Palette.make_argb(0xff, red, green, blue);
            }
        } else {
            for (i = 0; i < Video.fullwidth * Video.fullheight; i++) {
                Video.bitmapcolor[i] = Palette.entry_color[Video.bitmapbase[Video.curbitmap][i]];
            }
        }
    }

    public static void handler_ingame() {
        Mame.is_foreground = isWindowForeground();
        boolean is_paused = Mame.mame_is_paused();
        if (single_step) {
            Mame.mame_pause(true);
            single_step = false;
        }
        if (Mame.is_foreground) {
            if (Keyboard.IsPressed(KeyEvent.VK_F3)) {
                cpurun();
                Mame.playState = Mame.PlayState.PLAY_RESET;
            }
            if (Keyboard.IsTriggered(KeyEvent.VK_F7)) {
                cpurun();
                if (Keyboard.IsPressed(KeyEvent.VK_SHIFT)) {
                    Mame.playState = Mame.PlayState.PLAY_SAVE;
                } else {
                    Mame.playState = Mame.PlayState.PLAY_LOAD;
                }
                return;
            }
            if (Keyboard.IsTriggered(KeyEvent.VK_F8)) {
                cpurun();
                if (Keyboard.IsPressed(KeyEvent.VK_SHIFT)) {
                    if (Mame.playState == Mame.PlayState.PLAY_RECORDRUNNING) {
                        Mame.playState = Mame.PlayState.PLAY_RECORDEND;
                    } else {
                        Mame.playState = Mame.PlayState.PLAY_RECORDSTART;
                    }
                } else {
                    Mame.playState = Mame.PlayState.PLAY_REPLAYSTART;
                }
                return;
            }
            if (Keyboard.IsTriggered(KeyEvent.VK_P)) {
                if (is_paused && (Keyboard.IsPressed(KeyEvent.VK_SHIFT))) {
                    single_step = true;
                    Mame.mame_pause(false);
                } else {
                    Mame.mame_pause(!Mame.mame_is_paused());
                }
            }
            if (Keyboard.IsTriggered(KeyEvent.VK_F10)) {
                Keyboard.bF10 = true;
                boolean b1 = Video.global_throttle;
                Video.global_throttle = !b1;
            }
        }
    }

    public static void cpurun() {
    }

    private static double ui_get_line_height() {
        int raw_font_pixel_height = 0x0b;
        int target_pixel_height = 0xff;
        double one_to_one_line_height;
        double scale_factor;
        one_to_one_line_height = (double) raw_font_pixel_height / (double) target_pixel_height;
        scale_factor = 1.0;
        return scale_factor * one_to_one_line_height;
    }
}
