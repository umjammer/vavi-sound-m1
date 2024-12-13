/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;


import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import dotnet4j.io.FileStream;
import m1.emu.Timer.EmuTimer;
import m1.mame.capcom.Capcom;
import m1.mame.cps.CPS;
import m1.mame.dataeast.Dataeast;
import m1.mame.konami68000.Konami68000;
import m1.mame.m72.M72;
import m1.mame.m92.M92;
import m1.mame.namcos1.Namcos1;
import m1.mame.neogeo.Neogeo;
import m1.mame.pgm.PGM;
import m1.mame.taito.Taito;
import m1.mame.taitob.Taitob;
import m1.mame.tehkan.Tehkan;
import m1.sound.Sound;
import m1.sound.YM2151;
import m1.sound.YM2203;
import m1.sound.YM2610;


public class Mame {

    public enum PlayState {
        PLAY_STOPPED,
        PLAY_STOPPING,
        PLAY_RUNNING,
        PLAY_SAVE,
        PLAY_LOAD,
        PLAY_RESET,
        PLAY_RECORDSTART,
        PLAY_RECORDRUNNING,
        PLAY_RECORDEND,
        PLAY_REPLAYSTART,
        PLAY_REPLAYRUNNING,
        PLAY_REPLAYEND,

        PLAY_START,
        PLAY_NEXT,
        PLAY_NEXT2,
        PLAY_CONTINUOUS,
        PLAY_CONTINUOUSSTART,
        PLAY_CONTINUOUSSTART2,
        RECORD_STOPPED,
        RECORD_START,
        RECORD_RECORDING
    }

    public static PlayState playState;
    public static boolean is_foreground;
    public static boolean paused, exit_pending;
    public static EmuTimer soft_reset_timer;
    public static BinaryReader brRecord = null;
    public static BinaryWriter bwRecord = null;
    private static final FileStream fsRecord = null;

    public static void mame_execute() {
        soft_reset();
        mame_pause(true);
        while (!exit_pending) {
            if (!paused) {
                Cpuexec.cpuexec_timeslice();
            } else {
                Video.video_frame_update();
            }
            handlestate();
        }
    }

    public static void mame_schedule_soft_reset() {
        Timer.adjustPeriodic(soft_reset_timer, Attotime.ATTOTIME_ZERO, Attotime.ATTOTIME_NEVER);
        mame_pause(false);
        if (Cpuexec.activecpu >= 0) {
            Cpuexec.cpu[Cpuexec.activecpu].setPendingCycles(-1);
        }
    }

    private static void handlestate() {
        if (playState == PlayState.PLAY_SAVE) {
            mame_pause(true);
            UI.ui_handler_callback = Mame::handle_save;
        } else if (playState == PlayState.PLAY_LOAD) {
            mame_pause(true);
            UI.ui_handler_callback = Mame::handle_load;
        } else if (playState == PlayState.PLAY_RESET) {
            soft_reset();
            playState = PlayState.PLAY_RUNNING;
        } else if (playState == PlayState.PLAY_RECORDSTART) {
            mame_pause(true);
            UI.ui_handler_callback = Mame::handle_record;
        } else if (playState == PlayState.PLAY_RECORDEND) {
            handle_record();
        } else if (playState == PlayState.PLAY_REPLAYSTART) {
            mame_pause(true);
            UI.ui_handler_callback = Mame::handle_replay;
        } else if (playState == PlayState.PLAY_REPLAYEND) {
            handle_replay();
        }
    }

    public static void init_machine() throws IOException {
        //fileio_init();
        //config_init();
        //Inptport.input_init();
        //output_init();

        Palette.palette_init();
        //render_init();
        //ui_init();

        Generic.generic_machine_init();

        Timer.timer_init();
        soft_reset_timer = Timer.allocCommon(Mame::soft_reset, "soft_reset", false);

        //osd_init();

        //time(&mame->base_time);

        Inptport.input_port_init();
        //if (newbase != 0)
        //    mame->base_time = newbase;

        // intialize UI input
        //ui_input_init();

        //rom_init();
        //memory_init();
        Cpuexec.cpuexec_init();
        Watchdog.watchdog_init();
        Cpuint.cpuint_init();

        //device_list_start();

        Video.video_init();
        Tilemap.tilemap_init();
        Crosshair.crosshair_init();
        Sound.sound_init();
        State.state_init();
        Machine.machine_start();
        Machine.FORM.ui_init();
    }

    public static void mame_pause(boolean pause) {
        if (paused == pause)
            return;
        paused = pause;
        Sound.sound_pause(paused);
    }

    public static boolean mame_is_paused() {
        //return (playState != PlayState.PLAY_RUNNING && playState != PlayState.PLAY_RECORDRUNNING&& playState !=PlayState.PLAY_REPLAYRUNNING) || paused;
        return paused;
    }

    public static void soft_reset() {
        Memory.memory_reset();
        Cpuint.cpuint_reset();
        Machine.machine_reset_callback.run();
        Generic.interrupt_reset();
        Cpuexec.cpuexec_reset();
        Watchdog.watchdog_internal_reset();
        Sound.sound_reset();
        Timer.setGlobalTime(Timer.getCurrentTime());
    }

    private static void handle_save() {

    }

    private static void handle_load() {

    }

    private static void handle_record() {

    }

    private static void handle_replay() {

    }

    public static void postload() {
        int i;
        switch (Machine.sBoard) {
            case "CPS-1":
                for (i = 0; i < 3; i++) {
                    CPS.ttmap[i].all_tiles_dirty = true;
                }
                YM2151.ym2151_postload();
                break;
            case "CPS-1(QSound)":
            case "CPS2":
                for (i = 0; i < 3; i++) {
                    CPS.ttmap[i].all_tiles_dirty = true;
                }
                break;
            case "Data East":
                Dataeast.bg_tilemap.all_tiles_dirty = true;
                YM2203.FF2203[0].ym2203_postload();
                break;
            case "Tehkan":
                Tehkan.bg_tilemap.all_tiles_dirty = true;
                Tehkan.fg_tilemap.all_tiles_dirty = true;
                break;
            case "Neo Geo":
                Neogeo.regenerate_pens();
                YM2610.F2610.ym2610_postload();
                break;
            case "Namco System 1":
                for (i = 0; i < 6; i++) {
                    Namcos1.ttmap[i].all_tiles_dirty = true;
                }
                YM2151.ym2151_postload();
                break;
            case "IGS011":
                break;
            case "PGM":
                PGM.pgm_tx_tilemap.all_tiles_dirty = true;
                PGM.pgm_bg_tilemap.all_tiles_dirty = true;
                break;
            case "M72":
                M72.bg_tilemap.all_tiles_dirty = true;
                M72.fg_tilemap.all_tiles_dirty = true;
                break;
            case "M92":
                for (i = 0; i < 3; i++) {
                    M92.pf_layer[i].tmap.all_tiles_dirty = true;
                    M92.pf_layer[i].wide_tmap.all_tiles_dirty = true;
                }
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
                        YM2203.FF2203[0].ym2203_postload();
                        break;
                    case "opwolf":
                    case "opwolfa":
                    case "opwolfj":
                    case "opwolfu":
                    case "opwolfb":
                    case "opwolfp":
                        Taito.PC080SN_tilemap[0][0].all_tiles_dirty = true;
                        Taito.PC080SN_tilemap[0][1].all_tiles_dirty = true;
                        break;
                }
                break;
            case "Taito B":
                Taitob.bg_tilemap.all_tiles_dirty = true;
                Taitob.fg_tilemap.all_tiles_dirty = true;
                Taitob.tx_tilemap.all_tiles_dirty = true;
                YM2610.F2610.ym2610_postload();
                break;
            case "Konami 68000":
                Konami68000.K052109_tilemap[0].all_tiles_dirty = true;
                Konami68000.K052109_tilemap[1].all_tiles_dirty = true;
                Konami68000.K052109_tilemap[2].all_tiles_dirty = true;
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
                        Capcom.bg_tilemap.all_tiles_dirty = true;
                        Capcom.fg_tilemap.all_tiles_dirty = true;
                        Capcom.bg_tilemap.tilemap_set_scrollx(0, Capcom.scrollx[0] + 256 * Capcom.scrollx[1]);
                        Capcom.fg_tilemap.tilemap_set_scrollx(0, Capcom.scrolly[0] + 256 * Capcom.scrolly[1]);
                        YM2203.FF2203[0].ym2203_postload();
                        YM2203.FF2203[1].ym2203_postload();
                        break;
                    case "sf":
                    case "sfua":
                    case "sfj":
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        Capcom.bg_tilemap.all_tiles_dirty = true;
                        Capcom.fg_tilemap.all_tiles_dirty = true;
                        Capcom.tx_tilemap.all_tiles_dirty = true;
                        Capcom.bg_tilemap.tilemap_set_scrollx(0, Capcom.bg_scrollx);
                        Capcom.fg_tilemap.tilemap_set_scrollx(0, Capcom.fg_scrollx);
                        break;
                }
                break;
        }
    }
}
