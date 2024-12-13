/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.neogeo;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import m1.Properties.Resources;
import m1.cpu.m68000.MC68000;
import m1.cpu.z80.Z80A;
import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Inptport;
import m1.emu.Inptport.FrameArray;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.Memory;
import m1.emu.Pd4900a;
import m1.emu.RomInfo;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.emu.Watchdog;
import m1.sound.AY8910;
import m1.sound.FM;
import m1.sound.Sound;
import m1.sound.YM2610;
import m1.sound.YMDeltat;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;


public class Neogeo {

    private static final int NEOGEO_HBEND = 0x01e; // 30	/* this should really be 29.5 */
    private static final int NEOGEO_HBSTART = 0x15e; // 350 /* this should really be 349.5 */
    private static final int NEOGEO_VTOTAL = 0x108; // 264
    private static final int NEOGEO_VBEND = 0x010;
    private static final int NEOGEO_VBSTART = 0x0f0; // 240
    private static final int NEOGEO_VBLANK_RELOAD_HPOS = 0x11f; // 287
    private static byte display_position_interrupt_control;
    private static int display_counter;
    private static int vblank_interrupt_pending;
    private static int display_position_interrupt_pending;
    private static int irq3_pending;
    public static byte dsw;
    public static EmuTimer display_position_interrupt_timer;
    public static EmuTimer display_position_vblank_timer;
    public static EmuTimer vblank_interrupt_timer;
    private static byte controller_select;
    public static int main_cpu_bank_address;
    public static byte main_cpu_vector_table_source;
    //public static byte audio_result;
    public static byte[] audio_cpu_banks;
    public static byte[] mainbiosrom, mainram2, audiobiosrom, fixedrom, fixedbiosrom, zoomyrom, spritesrom, pvc_cartridge_ram;
    public static final byte[] extra_ram = new byte[0x2000];
    public static int fatfury2_prot_data;
    public static short neogeo_rng;
    private static byte save_ram_unlocked;
    public static boolean audio_cpu_nmi_enabled, audio_cpu_nmi_pending;

    public static void NeogeoInit() {
        audio_cpu_banks = new byte[4];
        pvc_cartridge_ram = new byte[0x2000];
        Memory.mainram = new byte[0x1_0000];
        mainram2 = new byte[0x1_0000];
        Memory.audioram = new byte[0x800];
        Machine.bRom = true;
        dsw = (byte) 0xff;
        fixedbiosrom = Resources.sfix;
        zoomyrom = Resources._000_lo;
        audiobiosrom = Resources.sm1;
        mainbiosrom = Resources.mainbios;
        Memory.mainrom = new byte[0x1_0000];// Machine.GetRom("maincpu.rom");
        Memory.audiorom = Machine.GetRom("audiocpu.rom");
        fixedrom = new byte[0x1_0000];// Machine.GetRom("fixed.rom");
        FM.ymsndrom = Machine.GetRom("ymsnd.rom");
        YMDeltat.ymsnddeltatrom = Machine.GetRom("ymsnddeltat.rom");
        spritesrom = new byte[0x1_0000];// Machine.GetRom("sprites.rom");
        if (fixedbiosrom == null || zoomyrom == null || audiobiosrom == null || mainbiosrom == null || Memory.mainrom == null || Memory.audiorom == null || fixedrom == null || FM.ymsndrom == null || spritesrom == null) {
            Machine.bRom = false;
        }
        if (Machine.bRom) {
            switch (Machine.sName) {
                case "irrmaze":
                case "kizuna4p":
                    mainbiosrom = Machine.GetRom("mainbios.rom");
                    break;
                case "kof99":
                case "kof99h":
                case "kof99e":
                case "kof99k":
                case "garou":
                case "garouh":
                case "mslug3":
                case "mslug3h":
                case "mslug4":
                case "mslug4h":
                case "ms4plus":
                case "ganryu":
                case "s1945p":
                case "preisle2":
                case "bangbead":
                case "nitd":
                case "zupapa":
                case "sengoku3":
                case "rotd":
                case "rotdh":
                case "pnyaa":
                case "mslug5":
                case "mslug5h":
                case "ms5plus":
                case "samsho5":
                case "samsho5h":
                case "samsho5b":
                case "samsh5sp":
                case "samsh5sph":
                case "samsh5spho":
                case "jockeygp":
                case "jockeygpa":
                    neogeo_fixed_layer_bank_type = 1;
                    break;
                case "kof2000":
                case "kof2000n":
                case "matrim":
                case "matrimbl":
                case "svc":
                case "kof2003":
                case "kof2003h":
                    neogeo_fixed_layer_bank_type = 2;
                    break;
                default:
                    neogeo_fixed_layer_bank_type = 0;
                    break;
            }
        }
    }

    private static void adjust_display_position_interrupt_timer() {
        if ((display_counter + 1) != 0) {
            Atime period = Attotime.attotime_mul(new Atime(0, Attotime.ATTOSECONDS_PER_SECOND / 6_000_000), display_counter + 1);
            Timer.adjustPeriodic(display_position_interrupt_timer, period, Attotime.ATTOTIME_NEVER);
        }
    }

    private static void update_interrupts() {
        int level = 0;
        if (vblank_interrupt_pending != 0) {
            level = 1;
        }
        if (display_position_interrupt_pending != 0) {
            level = 2;
        }
        if (irq3_pending != 0) {
            level = 3;
        }
//        if (level == 1) {
//            Cpuint.cpunum_set_input_line(0, 1, LineState.ASSERT_LINE);
//        } else if (level == 2) {
//            Cpuint.cpunum_set_input_line(0, 2, LineState.ASSERT_LINE);
//        } else if (level == 3) {
//            Cpuint.cpunum_set_input_line(0, 3, LineState.ASSERT_LINE);
//        } else {
//            Cpuint.cpunum_set_input_line(0, 7, LineState.CLEAR_LINE);
//        }
    }

    public static void display_position_interrupt_callback() {
        if ((display_position_interrupt_control & 0x10) != 0) {
            display_position_interrupt_pending = 1;
            update_interrupts();
        }
        if ((display_position_interrupt_control & 0x80) != 0) {
            adjust_display_position_interrupt_timer();
        }
    }

    public static void display_position_vblank_callback() {
        if ((display_position_interrupt_control & 0x40) != 0) {
            adjust_display_position_interrupt_timer();
        }
        Timer.adjustPeriodic(display_position_vblank_timer, Video.video_screen_get_time_until_pos(NEOGEO_VBSTART, NEOGEO_VBLANK_RELOAD_HPOS), Attotime.ATTOTIME_NEVER);
    }

    public static void vblank_interrupt_callback() {
        calendar_clock();
        vblank_interrupt_pending = 1;
        update_interrupts();
        Timer.adjustPeriodic(vblank_interrupt_timer, Video.video_screen_get_time_until_pos(NEOGEO_VBSTART, 0), Attotime.ATTOTIME_NEVER);
    }

    public static void audio_cpu_irq(int assert_) {
        Cpuint.cpunum_set_input_line(1, 0, assert_ != 0 ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    private static void select_controller(byte data) {
        controller_select = data;
    }

    public static void io_control_w(int offset, byte data) {
        switch (offset) {
            case 0x00:
                select_controller(data);
                break;
//            case 0x18:
//                set_output_latch(data & 0x00ff);
//                break;
//            case 0x20:
//                set_output_data(data & 0x00ff);
//                break;
            case 0x28:
                Pd4900a.pd4990a_control_16_w(data);
                break;
//            case 0x30:
//                break; // coin counters
//            case 0x31:
//                break; // coin counters
//            case 0x32:
//                break; // coin lockout
//            case 0x33:
//                break; // coui lockout
            default:
                break;
        }
    }

    private static void calendar_init() {
        Instant time = Instant.parse("1970-1-1");
        Pd4900a.pd4990a.seconds = ((time.get(SECOND_OF_MINUTE) / 10) << 4) + (time.get(SECOND_OF_MINUTE) % 10);
        Pd4900a.pd4990a.minutes = ((time.get(MINUTE_OF_HOUR) / 10) << 4) + (time.get(MINUTE_OF_HOUR) % 10);
        Pd4900a.pd4990a.hours = ((time.get(HOUR_OF_DAY) / 10) << 4) + (time.get(HOUR_OF_DAY) % 10);
        Pd4900a.pd4990a.days = ((time.get(DAY_OF_MONTH) / 10) << 4) + (time.get(DAY_OF_MONTH) % 10);
        Pd4900a.pd4990a.month = time.get(MONTH_OF_YEAR);
        Pd4900a.pd4990a.year = ((((time.get(YEAR) - 1900) % 100) / 10) << 4) + ((time.get(YEAR) - 1900) % 10);
        Pd4900a.pd4990a.weekday = time.get(DAY_OF_WEEK);
    }

    public static void calendar_clock() {
        Pd4900a.pd4990a_addretrace();
    }

    public static int get_calendar_status() {
        int i1 = (Pd4900a.outputbit << 1) | Pd4900a.testbit;
        return i1;
    }

    public static void save_ram_w(int offset, byte data) {
        if (save_ram_unlocked != 0)
            mainram2[offset] = data;
    }

    public static void audio_cpu_check_nmi() {
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), (audio_cpu_nmi_enabled && audio_cpu_nmi_pending) ? LineState.ASSERT_LINE : LineState.CLEAR_LINE);
    }

    public static void audio_cpu_enable_nmi_w(int offset) {
        audio_cpu_nmi_enabled = (offset & 0x10) == 0;
        audio_cpu_check_nmi();
    }

    public static void audio_command_w(byte data) {
        Sound.soundlatch_w(data);
        audio_cpu_nmi_pending = true;
        audio_cpu_check_nmi();
        Cpuexec.cpu_boost_interleave(Attotime.ATTOTIME_ZERO, new Atime(0, (long) (50 * 1e12)));
    }

    public static byte audio_command_r() {
        byte ret = (byte) Sound.soundlatch_r();
        audio_cpu_nmi_pending = false;
        audio_cpu_check_nmi();
        return ret;
    }

    public static void audio_result_w(byte data) {
        Sound.soundlatch2_w(data);
    }

    public static byte get_audio_result() {
        return (byte) Sound.soundlatch2_r();
    }

    public static void main_cpu_bank_select_w(int data) {
        int bank_address;
        int len = Memory.mainrom.length;
        if ((len <= 0x10_0000) && ((data & 0x07) != 0)) {
            int i1 = 1;
        } else {
            bank_address = ((data & 0x07) + 1) * 0x10_0000;
            if (bank_address >= len) {
                bank_address = 0x10_0000;
            }
            main_cpu_bank_address = bank_address;
        }
    }

    public static void system_control_w(int offset) {
        //if (ACCESSING_BITS_0_7)
        {
            byte bit = (byte) ((offset >> 3) & 0x01);
            switch (offset & 0x07) {
                default:
                case 0x00:
                    neogeo_set_screen_dark(bit);
                    break;
                case 0x01:
                    main_cpu_vector_table_source = bit;
                    break;
                case 0x05:
                    fixed_layer_source = bit;
                    break;
                case 0x06:
                    save_ram_unlocked = bit;
                    break;
                case 0x07:
                    neogeo_set_palette_bank(bit);
                    break;
                case 0x02: /* unknown - HC32 middle pin 1 */
                case 0x03: /* unknown - uPD4990 pin ? */
                case 0x04: /* unknown - HC32 middle pin 10 */
                    break;
            }
        }
    }

    public static void watchdog_w() {
        Watchdog.watchdog_reset();
    }

    public static void machine_start_neogeo() {
        if (Memory.mainrom.length > 0x10_0000) {
            main_cpu_bank_address = 0x10_0000;
        } else {
            main_cpu_bank_address = 0x00_0000;
        }
        audio_cpu_banks[0] = 0x1e;
        audio_cpu_banks[1] = 0x0e;
        audio_cpu_banks[2] = 0x06;
        audio_cpu_banks[3] = 0x02;
        display_position_interrupt_timer = Timer.allocCommon(Neogeo::display_position_interrupt_callback, "display_position_interrupt_callback", false);
        display_position_vblank_timer = Timer.allocCommon(Neogeo::display_position_vblank_callback, "display_position_vblank_callback", false);
        vblank_interrupt_timer = Timer.allocCommon(Neogeo::vblank_interrupt_callback, "vblank_interrupt_callback", false);
        Pd4900a.pd4990a_init();
        calendar_init();
        irq3_pending = 1;
    }

    public static void nvram_handler_load_neogeo() {
        if (Files.exists(Path.of("nvram", Machine.sName + ".nv"))) {
            FileStream fs1 = new FileStream(Path.of("nvram", Machine.sName + ".nv").toString(), FileMode.Open);
            int n = (int) fs1.getLength();
            fs1.read(mainram2, 0, n);
            fs1.close();
        }
    }

    public static void nvram_handler_save_neogeo() {
        FileStream fs1 = new FileStream(Path.of("nvram", Machine.sName + ".nv").toString(), FileMode.Create);
        fs1.write(mainram2, 0, 0x2000);
        fs1.close();
    }

    public static void machine_reset_neogeo() {
        int offs;
        for (offs = 0; offs < 8; offs++)
            system_control_w(offs);
        audio_cpu_nmi_enabled = false;
        audio_cpu_nmi_pending = false;
        audio_cpu_check_nmi();
        Timer.adjustPeriodic(vblank_interrupt_timer, Video.video_screen_get_time_until_pos(NEOGEO_VBSTART, 0), Attotime.ATTOTIME_NEVER);
        Timer.adjustPeriodic(display_position_vblank_timer, Video.video_screen_get_time_until_pos(NEOGEO_VBSTART, NEOGEO_VBLANK_RELOAD_HPOS), Attotime.ATTOTIME_NEVER);
        update_interrupts();
        start_sprite_line_timer();
        start_auto_animation_timer();
    }

    public static void play_neogeo_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_default(TrackInfo ti) {
        //Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number, 0, 7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) 7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 3, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_maglord(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_maglord(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_maglord(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_wh2j(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) ti.TrackID));
        //Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte)ti.TrackID));
    }

    public static void stop_neogeo_wh2j(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) ti.TrackID));
        //Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte)ti.TrackID));
    }

    public static void stopandplay_neogeo_wh2j(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 1, (byte) RomInfo.iStop));
        //Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 1, 1, (byte)RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 1, (byte) ti.TrackID));
        //Inptport.lsFA.Add(new FrameArray(Video.screenstate.frame_number + 3, 1, (byte)ti.TrackID));
    }

    public static void play_neogeo_mslug2(TrackInfo ti) {
        if (ti.IndexList == 3) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 30));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 0));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_mslug2(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_mslug2(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 0, 0, (byte) RomInfo.iStop));
        if (ti.IndexList == 3) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 30));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_mslugx(TrackInfo ti) {
        if (ti.IndexList >= 4 && ti.IndexList <= 20) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 0));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 30));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_mslugx(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_mslugx(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 0, 0, (byte) RomInfo.iStop));
        if (ti.IndexList >= 4 && ti.IndexList <= 20) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 30));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_mslug3(TrackInfo ti) {
        if (ti.IndexList >= 3 && ti.IndexList <= 34) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 0));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 30));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_mslug3(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_mslug3(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 0, 0, (byte) RomInfo.iStop));
        if (ti.IndexList >= 3 && ti.IndexList <= 34) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 0));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 30));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_aodk(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 252));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_aodk(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 252));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_aodk(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 252));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) 252));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 3, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_wjammers(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 23));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_wjammers(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_wjammers(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) 23));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 3, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_pbobbl2n(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 25));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_pbobbl2n(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_pbobbl2n(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 7));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) 25));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 3, 0, (byte) ti.TrackID));
    }

    public static void play_neogeo_lasthope(TrackInfo ti) {
        if (ti.IndexList >= 3 && ti.IndexList <= 34) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 30));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) 30));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

    public static void stop_neogeo_lasthope(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_neogeo_lasthope(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 0, 0, (byte) RomInfo.iStop));
        if (ti.IndexList >= 3 && ti.IndexList <= 34) {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 30));
        } else {
            Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) 30));
        }
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 2, 0, (byte) ti.TrackID));
    }

//#region Memory


    public static short short0, short1, short2, short3, short4, short5, short6;
    public static short short0_old, short1_old, short2_old, short3_old, short4_old, short5_old, short6_old;

    public static byte MReadOpByte(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x00_0000 && address <= 0x00_007f) {
            if (main_cpu_vector_table_source == 0) {
                result = mainbiosrom[address];
            } else if (main_cpu_vector_table_source == 1) {
                result = Memory.mainrom[address];
            }
        } else if (address >= 0x00_0080 && address <= 0x0f_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x10_0000 && address <= 0x1f_ffff) {
            result = Memory.mainram[address & 0xffff];
        } else if (address >= 0x20_0000 && address <= 0x2f_ffff) {
            result = Memory.mainrom[main_cpu_bank_address + (address - 0x20_0000)];
        } else if (address >= 0xc0_0000 && address <= 0xcf_ffff) {
            result = mainbiosrom[address & 0x1_ffff];
        } else {
            result = 0;
        }
        return result;
    }

    public static byte readByteM(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x00_0000 && address <= 0x00_007f) {
            if (main_cpu_vector_table_source == 0) {
                result = mainbiosrom[address];
            } else if (main_cpu_vector_table_source == 1) {
                result = Memory.mainrom[address];
            }
        } else if (address >= 0x00_0080 && address <= 0x0f_ffff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x10_0000 && address <= 0x1f_ffff) {
            result = Memory.mainram[address & 0xffff];
        } else if (address >= 0x20_0000 && address <= 0x2f_ffff) {
            result = Memory.mainrom[main_cpu_bank_address + (address - 0x20_0000)];
//        } else if (address >= 0x30_0000 && address <= 0x30_0001) {
//            if (address == 0x30_0000) {
//                result = (sbyte) (short0 >> 8);
//            } else if (address == 0x30_0001) {
//                result = (sbyte) dsw;
//            }
//        } else if (address >= 0x30_0080 && address <= 0x30_0081) {
//            if (address == 0x30_0080) {
//                result = (sbyte) (short4 >> 8);
//            } else if (address == 0x30_0081) {
//                result = (sbyte) short4;
//            }
        } else if (address >= 0x30_0000 && address <= 0x31_ffff) {
            int add = address & 0x81;
            if (add == 0x00) {
                result = (byte) (short0 >> 8);
            } else if (add == 0x01) {
                result = dsw;
            } else if (add == 0x80) {
                result = (byte) (short4 >> 8);
            } else if (add == 0x81) {
                result = (byte) short4;
            }
        } else if (address >= 0x32_0000 && address <= 0x33_ffff) {
            if ((address & 0x01) == 0) {
                result = (byte) ((short3 >> 8) | get_audio_result());
            } else if ((address & 0x01) == 1) {
                result = (byte) (short3 | ((get_calendar_status() & 0x03) << 6));
            }
        } else if (address >= 0x34_0000 && address <= 0x35_ffff) {
            if ((address & 0x01) == 0) {
                result = (byte) (short1 >> 8);
            } else if ((address & 0x01) == 1) {
                result = (byte) short1;
            }
        } else if (address >= 0x38_0000 && address <= 0x39_ffff) {
            if ((address & 0x01) == 0) {
                result = (byte) (short2 >> 8);
            } else if ((address & 0x01) == 1) {
                result = (byte) short2;
            }
        } else if (address >= 0x3c_0000 && address <= 0x3d_ffff) {
            if ((address & 0x01) == 0) {
                result = (byte) (neogeo_video_register_r((address & 0x07) >> 1) >> 8);
            } else if ((address & 0x01) == 1) {
                int i1 = 1;
            }
        } else if (address >= 0x40_0000 && address <= 0x7f_ffff) {
            int i1 = 1;
            //result = palettes[palette_bank][(address &0x1fff) >> 1];
        } else if (address >= 0xc0_0000 && address <= 0xcf_ffff) {
            result = mainbiosrom[address & 0x1_ffff];
        } else if (address >= 0xd0_0000 && address <= 0xdf_ffff) {
            result = mainram2[address & 0xffff];
        } else {
            int i1 = 1;
        }
        return result;
    }

    public static short MReadOpWord(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x00_0000 && address + 1 <= 0x00_007f) {
            if (main_cpu_vector_table_source == 0) {
                result = (short) (mainbiosrom[address] * 0x100 + mainbiosrom[address + 1]);
            } else if (main_cpu_vector_table_source == 1) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            }
        } else if (address >= 0x00_0080 && address + 1 <= 0x0f_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x1f_ffff) {
            result = (short) (Memory.mainram[address & 0xffff] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        } else if (address >= 0x20_0000 && address + 1 <= 0x2f_ffff) {
            result = (short) (Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff)] * 0x100 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 1]);
        } else if (address >= 0xc0_0000 && address + 1 <= 0xcf_ffff) {
            result = (short) (mainbiosrom[address & 0x1_ffff] * 0x100 + mainbiosrom[(address & 0x1_ffff) + 1]);
        } else {
            result = 0;
        }
        return result;
    }

    public static short readWordM(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x00_0000 && address + 1 <= 0x00_007f) {
            if (main_cpu_vector_table_source == 0) {
                result = (short) (mainbiosrom[address] * 0x100 + mainbiosrom[address + 1]);
            } else if (main_cpu_vector_table_source == 1) {
                result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
            }
        } else if (address >= 0x00_0080 && address + 1 <= 0x0f_ffff) {
            result = (short) (Memory.mainrom[address] * 0x100 + Memory.mainrom[address + 1]);
        } else if (address >= 0x10_0000 && address + 1 <= 0x1f_ffff) {
            result = (short) (Memory.mainram[address & 0xffff] * 0x100 + Memory.mainram[(address & 0xffff) + 1]);
        } else if (address >= 0x20_0000 && address <= 0x2f_ffff) {
            result = (short) (Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff)] * 0x100 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 1]);
        }
            /*else if (address >= 0x30_0000 && address <= 0x30_0001)
            {
                result = (short)((ushort)short0 | dsw);
            }
            else if (address >= 0x30_0080 && address <= 0x30_0081)
            {
                result = short4;
            }*/
        else if (address >= 0x30_0000 && address <= 0x31_ffff) {
            int add = address & 0x81;
            if (add >= 0x00 && add + 1 <= 0x01) {
                result = (short) (short0 | dsw);
            } else if (add >= 0x80 && add + 1 <= 0x81) {
                result = short4;
            }
        } else if (address >= 0x32_0000 && address <= 0x33_ffff) {
            result = (short) (short3 | ((get_calendar_status() & 0x03) << 6) | (get_audio_result() << 8));
        } else if (address >= 0x34_0000 && address <= 0x35_ffff) {
            result = short1;
        } else if (address >= 0x38_0000 && address <= 0x39_ffff) {
            result = short2;
        } else if (address >= 0x3c_0000 && address + 1 <= 0x3d_ffff) {
            result = neogeo_video_register_r((address & 0x07) >> 1);
        } else if (address >= 0x40_0000 && address + 1 <= 0x7f_ffff) {
            result = palettes[palette_bank][(address & 0x1fff) >> 1];
        } else if (address >= 0xc0_0000 && address + 1 <= 0xcf_ffff) {
            result = (short) (mainbiosrom[address & 0x1_ffff] * 0x100 + mainbiosrom[(address & 0x1_ffff) + 1]);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xdf_ffff) {
            result = (short) (mainram2[address & 0xffff] * 0x100 + mainram2[(address & 0xffff) + 1]);
        } else {
            int i1 = 1;
        }
        return result;
    }

    public static int MReadOpLong(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 < 0x00_007f) {
            if (main_cpu_vector_table_source == 0) {
                result = mainbiosrom[address] * 0x100_0000 + mainbiosrom[address + 1] * 0x1_0000 + mainbiosrom[address + 2] * 0x100 + mainbiosrom[address + 3];
            } else if (main_cpu_vector_table_source == 1) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            }
        } else if (address >= 0x00_0080 && address + 3 <= 0x0f_ffff) {
            result = Memory.mainrom[address] * 0x10_00000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
        } else if (address >= 0x10_0000 && address + 3 <= 0x1f_ffff) {
            result = Memory.mainram[address & 0xffff] * 0x10_00000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x2f_ffff) {
            result = Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff)] * 0x10_00000 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 1] * 0x1_0000 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 2] * 0x100 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 3];
        } else if (address >= 0xc0_0000 && address + 3 <= 0xcf_ffff) {
            result = mainbiosrom[address & 0x1_ffff] * 0x10_00000 + mainbiosrom[(address & 0x1_ffff) + 1] * 0x1_0000 + mainbiosrom[(address & 0x1_ffff) + 2] * 0x100 + mainbiosrom[(address & 0x1_ffff) + 3];
        } else {
            result = 0;
        }
        return result;
    }

    public static int readLongM(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x00_0000 && address + 3 <= 0x00_007f) {
            if (main_cpu_vector_table_source == 0) {
                result = mainbiosrom[address] * 0x100_0000 + mainbiosrom[address + 1] * 0x1_0000 + mainbiosrom[address + 2] * 0x100 + mainbiosrom[address + 3];
            } else if (main_cpu_vector_table_source == 1) {
                result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
            }
        } else if (address >= 0x00_0080 && address + 3 <= 0x0f_ffff) {
            result = Memory.mainrom[address] * 0x100_0000 + Memory.mainrom[address + 1] * 0x1_0000 + Memory.mainrom[address + 2] * 0x100 + Memory.mainrom[address + 3];
        } else if (address >= 0x10_0000 && address + 3 <= 0x1f_ffff) {
            result = Memory.mainram[address & 0xffff] * 0x100_0000 + Memory.mainram[(address & 0xffff) + 1] * 0x1_0000 + Memory.mainram[(address & 0xffff) + 2] * 0x100 + Memory.mainram[(address & 0xffff) + 3];
        } else if (address >= 0x20_0000 && address + 3 <= 0x2f_ffff) {
            result = Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff)] * 0x100_0000 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 1] * 0x1_0000 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 2] * 0x100 + Memory.mainrom[main_cpu_bank_address + (address & 0xf_ffff) + 3];
        } else if (address >= 0x30_0000 && address <= 0x31_ffff) {
            result = 0;
        } else if (address >= 0x32_0000 && address <= 0x33_ffff) {
            result = 0;
        } else if (address >= 0x34_0000 && address <= 0x35_ffff) {
            result = 0;
        } else if (address >= 0x38_0000 && address <= 0x39_ffff) {
            result = 0;
        } else if (address >= 0x3c_0000 && address + 3 <= 0x3d_ffff) {
            int i1 = 1;
            //result =neogeo_video_register_r((address &0x07) >> 1, mem_mask);
        } else if (address >= 0x40_0000 && address + 3 <= 0x7f_ffff) {
            result = palettes[palette_bank][(address & 0x1fff) / 2] * 0x1_0000 + palettes[palette_bank][((address & 0x1fff) / 2) + 1];
        } else if (address >= 0xc0_0000 && address + 3 <= 0xcf_ffff) {
            result = mainbiosrom[address & 0x1_ffff] * 0x100_0000 + mainbiosrom[(address & 0x1_ffff) + 1] * 0x1_0000 + mainbiosrom[(address & 0x1_ffff) + 2] * 0x100 + mainbiosrom[(address & 0x1_ffff) + 3];
        } else if (address >= 0xd0_0000 && address + 3 <= 0xdf_ffff) {
            result = mainram2[address & 0xffff] * 0x100_0000 + mainram2[(address & 0xffff) + 1] * 0x1_0000 + mainram2[(address & 0xffff) + 2] * 0x100 + mainram2[(address & 0xffff) + 3];
        } else {
            int i1 = 1;
        }
        return result;
    }

    public static void MWriteByte(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address <= 0x1f_ffff) {
            Memory.mainram[address & 0xffff] = value;
        } else if (address >= 0x2f_fff0 && address <= 0x2f_ffff) {
            main_cpu_bank_select_w(value);
        } else if (address >= 0x30_0000 && address <= 0x31_ffff) {
            if ((address & 0x01) == 0) {
                int i1 = 1;
            } else if ((address & 0x01) == 1) {
                watchdog_w();
            }
        } else if (address >= 0x32_0000 && address <= 0x33_ffff) {
            if ((address & 0x01) == 0) {
                audio_command_w(value);
            } else if ((address & 0x01) == 1) {
                int i1 = 1;
            }
        } else if (address >= 0x38_0000 && address <= 0x39_ffff) {
            io_control_w((address & 0x7f) >> 1, value);
        } else if (address >= 0x3a_0000 && address <= 0x3b_ffff) {
            if ((address & 0x01) == 1) {
                system_control_w((address & 0x1f) >> 1);
            }
        } else if (address >= 0x3c_0000 && address <= 0x3d_ffff) {
            if ((address & 0x01) == 0) {
                neogeo_video_register_w((address & 0x0f) >> 1, (short) ((value << 8) | value));
            } else if ((address & 0x01) == 1) {
                int i1 = 1;
            }
        } else if (address >= 0x40_0000 && address <= 0x7f_ffff) {
            int i1 = 1;
//            neogeo_paletteram_w((address - 0x40_0000) >> 1, data, mem_mask);
        } else if (address >= 0xd0_0000 && address <= 0xdf_ffff) {
            save_ram_w(address & 0xffff, value);
        } else {
            int i1 = 1;
        }
    }

    public static void writeWordM(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 1 <= 0x1f_ffff) {
            Memory.mainram[address & 0xffff] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 1] = (byte) value;
        } else if (address >= 0x2f_fff0 && address <= 0x2f_ffff) {
            main_cpu_bank_select_w(value);
        } else if (address >= 0x30_0000 && address <= 0x31_ffff) {
            int i1 = 1;
//            watchdog_w();
        } else if (address >= 0x32_0000 && address <= 0x33_ffff) {
            audio_command_w((byte) (value >> 8));
        } else if (address >= 0x38_0000 && address <= 0x39_ffff) {
            io_control_w((address & 0x7f) >> 1, (byte) value);
        } else if (address >= 0x3a_0000 && address <= 0x3b_ffff) {
            system_control_w((address & 0x1f) >> 1);
        } else if (address >= 0x3c_0000 && address <= 0x3d_ffff) {
            neogeo_video_register_w((address & 0x0f) >> 1, value);
        } else if (address >= 0x40_0000 && address <= 0x7f_ffff) {
            neogeo_paletteram_w((address & 0x1fff) >> 1, value);
        } else if (address >= 0xd0_0000 && address + 1 <= 0xdf_ffff) {
            save_ram_w(address & 0xffff, (byte) (value >> 8));
            save_ram_w((address & 0xffff) + 1, (byte) value);
        } else {
            int i1 = 1;
        }
    }

    public static void writeLongM(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x10_0000 && address + 3 <= 0x1f_ffff) {
            Memory.mainram[address & 0xffff] = (byte) (value >> 24);
            Memory.mainram[(address & 0xffff) + 1] = (byte) (value >> 16);
            Memory.mainram[(address & 0xffff) + 2] = (byte) (value >> 8);
            Memory.mainram[(address & 0xffff) + 3] = (byte) value;
        } else if (address >= 0x2f_fff0 && address <= 0x2f_ffff) {
            main_cpu_bank_select_w(value);
        } else if (address >= 0x30_0000 && address <= 0x31_ffff) {
            int i1 = 1;
//            watchdog_w();
        } else if (address >= 0x32_0000 && address <= 0x33_ffff) {
            int i1 = 1;
//            audio_command_w
        } else if (address >= 0x38_0000 && address <= 0x39_ffff) {
            int i1 = 1;
//            io_control_w((address & 0x7f) >> 1, value);
        } else if (address >= 0x3a_0000 && address <= 0x3b_ffff) {
//            system_control_w((address &0x1f) >> 1, mem_mask);
            int i1 = 1;
        } else if (address >= 0x3c_0000 && address + 3 <= 0x3d_ffff) {
            neogeo_video_register_w((address & 0x0f) >> 1, (short) (value >> 16));
            neogeo_video_register_w(((address & 0x0f) >> 1) + 1, (short) value);
        } else if (address >= 0x40_0000 && address + 3 <= 0x7f_ffff) {
            neogeo_paletteram_w((address & 0x1fff) >> 1, (short) (value >> 16));
            neogeo_paletteram_w(((address & 0x1fff) >> 1) + 1, (short) value);
        } else if (address >= 0xd0_0000 && address + 3 <= 0xdf_ffff) {
            save_ram_w(address & 0xffff, (byte) (value >> 24));
            save_ram_w((address & 0xffff) + 1, (byte) (value >> 16));
            save_ram_w((address & 0xffff) + 2, (byte) (value >> 8));
            save_ram_w((address & 0xffff) + 3, (byte) value);
        } else {
            int i1 = 1;
        }
    }

    public static byte MReadByte_fatfury2(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x20_0000 && address <= 0x2f_ffff) {
            int offset = (address - 0x20_0000) / 2;
            result = (byte) fatfury2_protection_16_r(offset);
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_fatfury2(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x20_0000 && address + 1 <= 0x2f_f_fff) {
            int offset = (address - 0x20_0000) / 2;
            result = fatfury2_protection_16_r(offset);
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_fatfury2(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x20_0000 && address + 3 <= 0x2f_ffff) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_fatfury2(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address <= 0x2f_ffff) {
            int offset = (address - 0x20_0000) / 2;
            fatfury2_protection_16_w(offset);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_fatfury2(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address + 1 <= 0x2f_ffff) {
            int offset = (address - 0x20_0000) / 2;
            fatfury2_protection_16_w(offset);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_fatfury2(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address + 3 <= 0x2_fffff) {
            int i1 = 1;
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_irrmaze(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x30_0000 && address <= 0x30_0001) {
            if (address == 0x30_0000) {
                result = dsw;
            } else if (address == 0x30_0001) {
                if ((controller_select & 0x01) == 0) {
                    result = (byte) (Inptport.input_port_read_direct(Inptport.analog_p0) ^ 0xff);
                } else if ((controller_select & 0x01) == 1) {
                    result = (byte) (Inptport.input_port_read_direct(Inptport.analog_p1) ^ 0xff);
                }
            }
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static void MWriteWord_kof98(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x20_aaaa && address + 1 <= 0x20_aaab) {
            kof98_prot_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static byte MReadByte_kof99(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = (byte) prot_9a37_r();
        } else if (address >= 0x2f_fff8 && address <= 0x2f_fffb) {
            result = (byte) sma_random_r();
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_kof99(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e446 && address + 1 <= 0x2f_e447) {
            result = prot_9a37_r();
        } else if (address >= 0x2f_fff8 && address + 1 <= 0x2f_fffb) {
            result = sma_random_r();
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_kof99(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = 0;
        } else if (address >= 0x2f_fff8 && address <= 0x2f_fffb) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_kof99(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address <= 0x2f_fff1) {
            kof99_bankswitch_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_kof99(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address + 1 <= 0x2f_fff1) {
            kof99_bankswitch_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_kof99(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address <= 0x2f_fff1) {
            int i1 = 1;
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_garou(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = (byte) prot_9a37_r();
        } else if (address >= 0x2_fffcc && address <= 0x2f_ffcd) {
            result = (byte) sma_random_r();
        } else if (address >= 0x2f_fff0 && address <= 0x2f_fff1) {
            result = (byte) sma_random_r();
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_garou(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e446 && address + 1 <= 0x2f_e447) {
            result = prot_9a37_r();
        } else if (address >= 0x2f_ffcc && address + 1 <= 0x2f_ffcd) {
            result = sma_random_r();
        } else if (address >= 0x2f_fff0 && address + 1 <= 0x2f_fff1) {
            result = sma_random_r();
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_garou(int address) {
        address &= 0xff_ffff;
        int result;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = 0;
        } else if (address >= 0x2f_ffcc && address <= 0x2f_ffcd) {
            result = 0;
        } else if (address >= 0x2f_fff0 && address <= 0x2f_fff1) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_garou(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffc0 && address <= 0x2f_ffc1) {
            garou_bankswitch_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_garou(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffc0 && address + 1 <= 0x2f_ffc1) {
            garou_bankswitch_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_garou(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffc0 && address <= 0x2f_ffc1) {
            int i1 = 1;
        } else {
            writeLongM(address, value);
        }
    }

    public static void MWriteByte_garouh(int address, byte value) {
        if (address >= 0x2f_ffc0 && address <= 0x2f_ffc1) {
            garouh_bankswitch_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_garouh(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffc0 && address + 1 <= 0x2f_ffc1) {
            garouh_bankswitch_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_garouh(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffc0 && address <= 0x2f_ffc1) {
            int i1 = 1;
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_mslug3(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = (byte) prot_9a37_r();
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_mslug3(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e446 && address + 1 <= 0x2f_e447) {
            result = prot_9a37_r();
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_mslug3(int address) {
        address &= 0xff_ffff;
        int result;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_mslug3(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffe4 && address <= 0x2f_ffe5) {
            mslug3_bankswitch_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_mslug3(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffe4 && address + 1 <= 0x2f_ffe5) {
            mslug3_bankswitch_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_mslug3(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffe4 && address <= 0x2f_ffe5) {
            int i1 = 1;
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_kof2000(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            if (address == 0x2f_e446) {
                result = (byte) (prot_9a37_r() >> 8);
            } else if (address == 0x2f_e447) {
                result = (byte) prot_9a37_r();
            }
        } else if (address >= 0x2f_ffd8 && address <= 0x2f_ffd9) {
            result = (byte) sma_random_r();
        } else if (address >= 0x2_fffda && address <= 0x2f_ffdb) {
            result = (byte) sma_random_r();
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_kof2000(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e446 && address + 1 <= 0x2f_e447) {
            result = prot_9a37_r();
        } else if (address >= 0x2f_ffd8 && address + 1 <= 0x2f_ffd9) {
            result = sma_random_r();
        } else if (address >= 0x2f_ffda && address + 1 <= 0x2f_ffdb) {
            result = sma_random_r();
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_kof2000(int address) {
        address &= 0xff_ffff;
        int result;
        if (address >= 0x2f_e446 && address <= 0x2f_e447) {
            result = 0;
        } else if (address >= 0x2f_ffd8 && address <= 0x2f_ffd9) {
            result = 0;
        } else if (address >= 0x2f_ffda && address <= 0x2f_ffdb) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_kof2000(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffec && address <= 0x2f_ffed) {
            kof2000_bankswitch_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_kof2000(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffec && address + 1 <= 0x2f_ffed) {
            kof2000_bankswitch_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_kof2000(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_ffec && address <= 0x2f_ffed) {
            kof2000_bankswitch_w(value);
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_pvc(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e000 && address <= 0x2f_ffff) {
            result = pvc_cartridge_ram[address - 0x2f_e000];
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_pvc(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e000 && address + 1 <= 0x2f_ffff) {
            result = (short) (pvc_cartridge_ram[address - 0x2f_e000] * 0x100 + pvc_cartridge_ram[address - 0x2f_e000 + 1]);
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_pvc(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x2f_e000 && address + 3 <= 0x2f_ffff) {
            result = pvc_cartridge_ram[address - 0x2f_e000] * 0x10_00000 + pvc_cartridge_ram[address - 0x2f_e000 + 1] * 0x1_0000 + pvc_cartridge_ram[address - 0x2f_e000 + 2] * 0x100 + pvc_cartridge_ram[address - 0x2f_e000 + 3];
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_pvc(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address <= 0x2f_ffff) {
            pvc_cartridge_ram[address - 0x2f_e000] = value;
            int offset = (address - 0x2f_e000) / 2;
            if (offset == 0xff0)
                pvc_prot1();
            else if (offset >= 0xff4 && offset <= 0xff5)
                pvc_prot2();
            else if (offset >= 0xff8)
                pvc_write_bankswitch();
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_pvc(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address + 1 <= 0x2f_ffff) {
            pvc_cartridge_ram[address - 0x2f_e000] = (byte) (value >> 8);
            pvc_cartridge_ram[address - 0x2f_e000 + 1] = (byte) (value);
            int offset = (address - 0x2f_e000) / 2;
            if (offset == 0xff0)
                pvc_prot1();
            else if (offset >= 0xff4 && offset <= 0xff5)
                pvc_prot2();
            else if (offset >= 0xff8)
                pvc_write_bankswitch();
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_pvc(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address + 3 <= 0x2f_ffff) {
            pvc_cartridge_ram[address - 0x2f_e000] = (byte) (value >> 24);
            pvc_cartridge_ram[address - 0x2f_e000 + 1] = (byte) (value >> 16);
            pvc_cartridge_ram[address - 0x2f_e000 + 2] = (byte) (value >> 8);
            pvc_cartridge_ram[address - 0x2f_e000 + 3] = (byte) (value);
            int offset = (address - 0x2f_e000) / 2;
            if (offset == 0xff0)
                pvc_prot1();
            else if (offset >= 0xff4 && offset <= 0xff5)
                pvc_prot2();
            else if (offset >= 0xff8)
                pvc_write_bankswitch();

            if (offset + 1 == 0xff0)
                pvc_prot1();
            else if (offset + 1 >= 0xff4 && offset + 1 <= 0xff5)
                pvc_prot2();
            else if (offset + 1 >= 0xff8)
                pvc_write_bankswitch();
        } else {
            writeLongM(address, value);
        }
    }

    public static void MWriteByte_cthd2003(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address <= 0x2f_fff1) {
            cthd2003_bankswitch_w(value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_cthd2003(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address + 1 <= 0x2f_fff1) {
            cthd2003_bankswitch_w(value);
        } else {
            writeWordM(address, value);
        }
    }

    public static byte MReadByte_ms5plus(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_fff0 && address <= 0x2f_ffff) {
            result = (byte) mslug5_prot_r();
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_ms5plus(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_fff0 && address + 1 <= 0x2f_ffff) {
            result = mslug5_prot_r();
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_ms5plus(int address) {
        address &= 0xff_ffff;
        int result;
        if (address >= 0x2f_fff0 && address + 3 <= 0x2f_ffff) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_ms5plus(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address <= 0x2f_ffff) {
            int offset = (address - 0x2f_fff0) / 2;
            ms5plus_bankswitch_w(offset, value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_ms5plus(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address + 1 <= 0x2f_ffff) {
            int offset = (address - 0x2f_fff0) / 2;
            ms5plus_bankswitch_w(offset, value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_ms5plus(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_fff0 && address + 3 <= 0x2f_ffff) {
            int i1 = 1;
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_kog(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x0f_fffe && address <= 0x0f_ffff) {
            if (address == 0x0f_fffe) {
                result = (byte) 0xff;
            } else {
                result = 0x01;
            }
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_kog(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x0f_fffe && address + 1 <= 0x0f_ffff) {
            result = (short) 0xff01;
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_kog(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x0f_fffe && address <= 0x0f_ffff) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static byte MReadByte_kf2k3bl(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e000 && address <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            result = extra_ram[offset];
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_kf2k3bl(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e000 && address + 1 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            result = (short) (extra_ram[offset] * 0x100 + extra_ram[offset + 1]);
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_kf2k3bl(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x2f_e000 && address + 3 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            result = extra_ram[offset] * 0x10_00000 + extra_ram[offset + 1] * 0x1_0000 + extra_ram[offset + 2] * 0x100 + extra_ram[offset + 3];
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_kf2k3bl(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            extra_ram[offset] = value;
            kof2003_w(offset / 2);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_kf2k3bl(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address + 1 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            extra_ram[offset] = (byte) (value >> 8);
            extra_ram[offset + 1] = (byte) value;
            kof2003_w(offset / 2);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_kf2k3bl(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address + 3 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            extra_ram[offset] = (byte) (value >> 24);
            extra_ram[offset + 1] = (byte) (value >> 16);
            extra_ram[offset + 2] = (byte) (value >> 8);
            extra_ram[offset + 3] = (byte) value;
            kof2003_w(offset / 2);
            kof2003_w((offset + 2) / 2);
        } else {
            writeLongM(address, value);
        }
    }

    public static void MWriteByte_kf2k3pl(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            extra_ram[offset] = value;
            kof2003p_w(offset / 2);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_kf2k3pl(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address + 1 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            extra_ram[offset] = (byte) (value >> 8);
            extra_ram[offset + 1] = (byte) value;
            kof2003p_w(offset / 2);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_kf2k3pl(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x2f_e000 && address + 3 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            extra_ram[offset] = (byte) (value >> 24);
            extra_ram[offset + 1] = (byte) (value >> 16);
            extra_ram[offset + 2] = (byte) (value >> 8);
            extra_ram[offset + 3] = (byte) value;
            kof2003p_w(offset / 2);
            kof2003p_w((offset + 2) / 2);
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_sbp(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x0_0200 && address <= 0x00_1fff) {
            int offset = address - 0x200;
            result = sbp_protection_r(offset);
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_sbp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x0_0200 && address + 1 <= 0x00_1fff) {
            int offset = address - 0x200;
            result = (short) (sbp_protection_r(offset) * 0x100 + sbp_protection_r(offset + 1));
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_sbp(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x0_0200 && address + 3 <= 0x00_1fff) {
            int offset = address - 0x200;
            result = sbp_protection_r(offset) * 0x100_0000 + sbp_protection_r(offset + 1) * 0x1_0000 + sbp_protection_r(offset + 2) * 0x100 + sbp_protection_r(offset + 3);
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static byte MReadByte_kof10th(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x2f_e000 && address <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            result = extra_ram[offset];
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_kof10th(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x2f_e000 && address + 1 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            result = (short) (extra_ram[offset] * 0x100 + extra_ram[offset + 1]);
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_kof10th(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x2f_e000 && address + 3 <= 0x2f_ffff) {
            int offset = address - 0x2f_e000;
            result = extra_ram[offset] * 0x100_0000 + extra_ram[offset + 1] * 0x1_0000 + extra_ram[offset + 2] * 0x100 + extra_ram[offset + 3];
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_sbp(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x200 && address <= 0x1fff) {
            int offset = (address - 0x200) / 2;
            sbp_protection_w(offset, value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_sbp(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x200 && address + 1 <= 0x1fff) {
            int offset = (address - 0x200) / 2;
            sbp_protection_w(offset, value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_sbp(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x200 && address + 3 <= 0x1fff) {
            int offset = (address - 0x200) / 2;
            sbp_protection_w(offset, (short) (value >> 16));
            sbp_protection_w(offset + 1, (byte) value);
        } else {
            writeLongM(address, value);
        }
    }

    public static void MWriteByte_kof10th(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address <= 0x23_ffff) {
            int offset = address - 0x20_0000;
            kof10th_custom_w(offset, value);
        } else if (address >= 0x24_0000 && address <= 0x2f_ffff) {
            int offset = address - 0x24_0000;
            kof10th_bankswitch_w(offset, value);
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_kof10th(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address + 1 <= 0x23_ffff) {
            int offset = address - 0x20_0000;
            kof10th_custom_w(offset, value);
        } else if (address >= 0x24_0000 && address + 1 <= 0x2f_ffff) {
            int offset = address - 0x24_0000;
            kof10th_bankswitch_w(offset, value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_kof10th(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address + 3 <= 0x23_ffff) {
            int i1 = 1;
        } else if (address >= 0x24_0000 && address + 3 <= 0x2f_ffff) {
            int offset = address - 0x24_0000;
            kof10th_bankswitch_w(offset, (short) (value >> 16));
            kof10th_bankswitch_w(offset + 2, (short) value);
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_jockeygp(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x20_0000 && address <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            result = extra_ram[offset];
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_jockeygp(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x20_0000 && address + 1 <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            result = (short) (extra_ram[offset] * 0x100 + extra_ram[offset + 1]);
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_jockeygp(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x20_0000 && address + 3 <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            result = extra_ram[offset] * 0x100_0000 + extra_ram[offset + 1] * 0x1_0000 + extra_ram[offset + 2] * 0x100 + extra_ram[offset + 3];
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void MWriteByte_jockeygp(int address, byte value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            extra_ram[offset] = value;
        } else {
            MWriteByte(address, value);
        }
    }

    public static void MWriteWord_jockeygp(int address, short value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address + 1 <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            extra_ram[offset] = (byte) (value >> 8);
            extra_ram[offset + 1] = (byte) (value);
        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_jockeygp(int address, int value) {
        address &= 0xff_ffff;
        if (address >= 0x20_0000 && address + 3 <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            extra_ram[offset] = (byte) (value >> 24);
            extra_ram[offset + 1] = (byte) (value >> 16);
            extra_ram[offset + 2] = (byte) (value >> 8);
            extra_ram[offset + 3] = (byte) (value);
        } else {
            writeLongM(address, value);
        }
    }

    public static byte MReadByte_vliner(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0x20_0000 && address <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            result = extra_ram[offset];
        } else if (address >= 0x28_0000 && address <= 0x28_0001) {
            if (address == 0x28_0000) {
                result = (byte) (short5 >> 8);
            } else {
                result = (byte) (short5);
            }
        } else if (address >= 0x2c_0000 && address <= 0x2c_0001) {
            if (address == 0x2c_0000) {
                result = (byte) (short6 >> 8);
            } else {
                result = (byte) (short6);
            }
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short MReadWord_vliner(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0x20_0000 && address + 1 <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            result = (short) (extra_ram[offset] * 0x100 + extra_ram[offset + 1]);
        } else if (address >= 0x28_0000 && address + 1 <= 0x28_0001) {
            result = short5;
        } else if (address >= 0x2c_0000 && address + 1 <= 0x2c_0001) {
            result = short6;
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int MReadLong_vliner(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0x20_0000 && address + 3 <= 0x20_1fff) {
            int offset = address - 0x20_0000;
            result = extra_ram[offset] * 0x100_0000 + extra_ram[offset + 1] * 0x1_0000 + extra_ram[offset + 2] * 0x100 + extra_ram[offset + 3];
        } else if (address >= 0x28_0000 && address <= 0x28_0001) {
            result = 0;
        } else if (address >= 0x2c_0000 && address <= 0x2c_0001) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static byte readByteM_(int address) {
        address &= 0xff_ffff;
        byte result = 0;
        if (address >= 0 && address <= 0) {
            result = 0;
        } else {
            result = readByteM(address);
        }
        return result;
    }

    public static short readWordM_(int address) {
        address &= 0xff_ffff;
        short result = 0;
        if (address >= 0 && address <= 0) {
            result = 0;
        } else {
            result = readWordM(address);
        }
        return result;
    }

    public static int readLongM_(int address) {
        address &= 0xff_ffff;
        int result = 0;
        if (address >= 0 && address <= 0) {
            result = 0;
        } else {
            result = readLongM(address);
        }
        return result;
    }

    public static void writeByteM_(int address, byte value) {
        if (address >= 0 && address <= 0) {

        } else {
            MWriteByte(address, value);
        }
    }

    public static void writeWordM_(int address, short value) {
        if (address >= 0 && address <= 0) {

        } else {
            writeWordM(address, value);
        }
    }

    public static void MWriteLong_(int address, int value) {
        if (address >= 0 && address <= 0) {

        } else {
            writeLongM(address, value);
        }
    }

    public static byte ZReadOp(short address) {
        byte result = 0;
        if (address >= 0x0000 && address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            result = Memory.audiorom[audio_cpu_banks[3] * 0x4000 + address - 0x8000];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdfff) {
            result = Memory.audiorom[audio_cpu_banks[2] * 0x2000 + address - 0xc000];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xefff) {
            result = Memory.audiorom[audio_cpu_banks[1] * 0x1000 + address - 0xe000];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            result = Memory.audiorom[audio_cpu_banks[0] * 0x800 + address - 0xf000];
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xffff) {
            result = Memory.audioram[address - 0xf800];
        }
        return result;
    }

    public static byte ZReadMemory(short address) {
        byte result = 0;
        if (address >= 0x0000 && address <= 0x7fff) {
            result = Memory.audiorom[address];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xbfff) {
            result = Memory.audiorom[audio_cpu_banks[3] * 0x4000 + address - 0x8000];
        } else if ((address & 0xffff) >= 0xc000 && (address & 0xffff) <= 0xdfff) {
            result = Memory.audiorom[audio_cpu_banks[2] * 0x2000 + address - 0xc000];
        } else if ((address & 0xffff) >= 0xe000 && (address & 0xffff) <= 0xefff) {
            result = Memory.audiorom[audio_cpu_banks[1] * 0x1000 + address - 0xe000];
        } else if ((address & 0xffff) >= 0xf000 && (address & 0xffff) <= 0xf7ff) {
            result = Memory.audiorom[audio_cpu_banks[0] * 0x800 + address - 0xf000];
        } else if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xffff) {
            result = Memory.audioram[address - 0xf800];
        }
        return result;
    }

    public static void writeMemoryZ(short address, byte value) {
        if ((address & 0xffff) >= 0xf800 && (address & 0xffff) <= 0xffff) {
            Memory.audioram[(address & 0xffff) - 0xf800] = value;
        } else {
            int i1 = 1;
        }
    }

    public static byte readHardwareZ(short address) {
        byte result = 0;
        int add1, add2;
        address &= (short) 0xffff;
        add1 = address & 0xff;
        if (add1 == 0) {
            result = audio_command_r();
        } else if (add1 >= 0x04 && add1 <= 0x07) {
            result = YM2610.F2610.ym2610_read(add1 - 0x04);
        } else if (add1 >= 0x08 && add1 <= 0xfb) {
            add2 = add1 & 0x0f;
            if (add2 >= 0x08 && add2 <= 0x0b) {
                audio_cpu_banks[add2 - 0x08] = (byte) (address >> 8);
            } else {
                int i1 = 1;
            }
            result = 0;
        } else {
            int i1 = 1;
        }
        return result;
    }

    public static void writeHardwareZ(short address, byte value) {
        int add1;
        add1 = address & 0xff;
        if (add1 == 0x00) {
            Sound.soundlatch_w((short) 0);
        } else if (add1 >= 0x04 && add1 <= 0x07) {
            YM2610.F2610.ym2610_write(add1 - 0x04, value);
        } else if (add1 == 0x08) {
            audio_cpu_enable_nmi_w(0);
        } else if (add1 == 0x0c) {
            audio_result_w(value);
        } else if (add1 == 0x18) {
            audio_cpu_enable_nmi_w(0x10);
        } else {
            int i1 = 1;
        }
    }

    public static int callbackIRQZ() {
        return 0;
    }

//#endregion

//#region Neoprot

    public static short fatfury2_protection_16_r(int offset) {
        short res = (short) (fatfury2_prot_data >> 24);
        switch (offset) {
            case 0x5_5550 / 2:
            case 0xf_fff0 / 2:
            case 0x0_0000 / 2:
            case 0xf_f000 / 2:
            case 0x3_6000 / 2:
            case 0x3_6008 / 2:
                return res;
            case 0x3_6004 / 2:
            case 0x3_600c / 2:
                return (short) (((res & 0xf0) >> 4) | ((res & 0x0f) << 4));
            default:
                return 0;
        }
    }

    public static void fatfury2_protection_16_w(int offset) {
        switch (offset) {
            case 0x1_1112 / 2:
                fatfury2_prot_data = 0xff00_0000;
                break;
            case 0x3_3332 / 2:
                fatfury2_prot_data = 0x0000_ffff;
                break;
            case 0x4_4442 / 2:
                fatfury2_prot_data = 0x00ff_0000;
                break;
            case 0x5_5552 / 2:
                fatfury2_prot_data = 0xff00_ff00;
                break;
            case 0x5_6782 / 2:
                fatfury2_prot_data = 0xf05a_3601;
                break;
            case 0x4_2812 / 2:
                fatfury2_prot_data = 0x8142_2418;
                break;
            case 0x5_5550 / 2:
            case 0xf_fff0 / 2:
            case 0xf_f000 / 2:
            case 0x3_6000 / 2:
            case 0x3_6004 / 2:
            case 0x3_6008 / 2:
            case 0x3_600c / 2:
                fatfury2_prot_data <<= 8;
                break;
            default:
                break;
        }
    }

    public static void kof98_prot_w(int value) {
        switch (value) {
            case 0x0090:
                Memory.mainrom[0x100] = 0x00;
                Memory.mainrom[0x101] = (byte) 0xc2;
                Memory.mainrom[0x102] = 0x00;
                Memory.mainrom[0x103] = (byte) 0xfd;
                break;
            case 0x00f0:
                Memory.mainrom[0x100] = 0x4e;
                Memory.mainrom[0x101] = 0x45;
                Memory.mainrom[0x102] = 0x4f;
                Memory.mainrom[0x103] = 0x2d;
                break;
            default:
                break;
        }
    }

    public static short prot_9a37_r() {
        return (short) 0x9a37;
    }

    public static short sma_random_r() {
        short old = neogeo_rng;
        short newbit = (short) (((neogeo_rng >> 2) ^
                (neogeo_rng >> 3) ^
                (neogeo_rng >> 5) ^
                (neogeo_rng >> 6) ^
                (neogeo_rng >> 7) ^
                (neogeo_rng >> 11) ^
                (neogeo_rng >> 12) ^
                (neogeo_rng >> 15)) & 1);
        neogeo_rng = (short) ((neogeo_rng << 1) | newbit);
        return old;
    }

    public static void kof99_bankswitch_w(int data) {
        int bankaddress;
        int[] bankoffset = {
                0x00_0000, 0x10_0000, 0x20_0000, 0x30_0000,
                0x3c_c000, 0x4c_c000, 0x3f_2000, 0x4f_2000,
                0x40_7800, 0x50_7800, 0x40_d000, 0x50_d000,
                0x41_7800, 0x51_7800, 0x42_0800, 0x52_0800,
                0x42_4800, 0x52_4800, 0x42_9000, 0x52_9000,
                0x42_e800, 0x52_e800, 0x43_1800, 0x53_1800,
                0x54_d000, 0x55_1000, 0x56_7000, 0x59_2800,
                0x58_8800, 0x58_1800, 0x59_9800, 0x59_4800,
                0x59_8000
        };
        data = (((data >> 14) & 1) << 0) +
                (((data >> 6) & 1) << 1) +
                (((data >> 8) & 1) << 2) +
                (((data >> 10) & 1) << 3) +
                (((data >> 12) & 1) << 4) +
                (((data >> 5) & 1) << 5);
        bankaddress = 0x10_0000 + bankoffset[data];
        main_cpu_bank_address = bankaddress;
    }

    public static void garou_bankswitch_w(int data) {
        int bankaddress;
        int[] bankoffset = {
                0x00_0000, 0x10_0000, 0x20_0000, 0x30_0000, // 00
                0x28_0000, 0x38_0000, 0x2d_0000, 0x3d_0000, // 04
                0x2f_0000, 0x3f_0000, 0x40_0000, 0x50_0000, // 08
                0x42_0000, 0x52_0000, 0x44_0000, 0x54_0000, // 12
                0x49_8000, 0x59_8000, 0x4a_0000, 0x5a_0000, // 16
                0x4a_8000, 0x5a_8000, 0x4b_0000, 0x5b_0000, // 20
                0x4b_8000, 0x5b_8000, 0x4c_0000, 0x5c_0000, // 24
                0x4c_8000, 0x5c_8000, 0x4d_0000, 0x5d_0000, // 28
                0x45_8000, 0x55_8000, 0x46_0000, 0x56_0000, // 32
                0x46_8000, 0x56_8000, 0x47_0000, 0x57_0000, // 36
                0x47_8000, 0x57_8000, 0x48_0000, 0x58_0000, // 40
                0x48_8000, 0x58_8000, 0x49_0000, 0x59_0000, // 44
                0x5d_0000, 0x5d_8000, 0x5e_0000, 0x5e_8000, // 48
                0x5f_0000, 0x5f_8000, 0x60_0000
        };
        data = (((data >> 5) & 1) << 0) +
                (((data >> 9) & 1) << 1) +
                (((data >> 7) & 1) << 2) +
                (((data >> 6) & 1) << 3) +
                (((data >> 14) & 1) << 4) +
                (((data >> 12) & 1) << 5);
        bankaddress = 0x10_0000 + bankoffset[data];
        main_cpu_bank_address = bankaddress;
    }

    public static void garouh_bankswitch_w(int data) {
        int bankaddress;
        int[] bankoffset = {
                0x00_0000, 0x10_0000, 0x20_0000, 0x30_0000, // 00
                0x28_0000, 0x38_0000, 0x2d_0000, 0x3d_0000, // 04
                0x2c_8000, 0x3c_8000, 0x40_0000, 0x50_0000, // 08
                0x42_0000, 0x52_0000, 0x44_0000, 0x54_0000, // 12
                0x59_8000, 0x69_8000, 0x5a_0000, 0x6a_0000, // 16
                0x5a_8000, 0x6a_8000, 0x5b_0000, 0x6b_0000, // 20
                0x5b_8000, 0x6b_8000, 0x5c_0000, 0x6c_0000, // 24
                0x5c_8000, 0x6c_8000, 0x5d_0000, 0x6d_0000, // 28
                0x45_8000, 0x55_8000, 0x46_0000, 0x56_0000, // 32
                0x46_8000, 0x56_8000, 0x47_0000, 0x57_0000, // 36
                0x47_8000, 0x57_8000, 0x48_0000, 0x58_0000, // 40
                0x48_8000, 0x58_8000, 0x49_0000, 0x59_0000, // 44
                0x5d_8000, 0x6d_8000, 0x5e_0000, 0x6e_0000, // 48
                0x5e_8000, 0x6e_8000, 0x6e_8000, 0x00_0000, // 52
                0x00_0000, 0x00_0000, 0x00_0000, 0x00_0000, // 56
                0x00_0000, 0x00_0000, 0x00_0000, 0x00_0000  // 60
        };
        data = (((data >> 4) & 1) << 0) +
                (((data >> 8) & 1) << 1) +
                (((data >> 14) & 1) << 2) +
                (((data >> 2) & 1) << 3) +
                (((data >> 11) & 1) << 4) +
                (((data >> 13) & 1) << 5);
        bankaddress = 0x10_0000 + bankoffset[data];
        main_cpu_bank_address = bankaddress;
    }

    public static void mslug3_bankswitch_w(int data) {
        int bankaddress;
        int[] bankoffset = {
                0x00_0000, 0x02_0000, 0x04_0000, 0x06_0000, // 00
                0x07_0000, 0x09_0000, 0x0b_0000, 0x0d_0000, // 04
                0x0e_0000, 0x0f_0000, 0x12_0000, 0x13_0000, // 08
                0x14_0000, 0x15_0000, 0x18_0000, 0x19_0000, // 12
                0x1a_0000, 0x1b_0000, 0x1e_0000, 0x1f_0000, // 16
                0x20_0000, 0x21_0000, 0x24_0000, 0x25_0000, // 20
                0x26_0000, 0x27_0000, 0x2a_0000, 0x2b_0000, // 24
                0x2c_0000, 0x2d_0000, 0x30_0000, 0x31_0000, // 28
                0x32_0000, 0x33_0000, 0x36_0000, 0x37_0000, // 32
                0x38_0000, 0x39_0000, 0x3c_0000, 0x3d_0000, // 36
                0x40_0000, 0x41_0000, 0x44_0000, 0x45_0000, // 40
                0x46_0000, 0x47_0000, 0x4a_0000, 0x4b_0000, // 44
                0x4c_0000
        };
        data = (((data >> 14) & 1) << 0) +
                (((data >> 12) & 1) << 1) +
                (((data >> 15) & 1) << 2) +
                (((data >> 6) & 1) << 3) +
                (((data >> 3) & 1) << 4) +
                (((data >> 9) & 1) << 5);
        bankaddress = 0x10_0000 + bankoffset[data];
        main_cpu_bank_address = bankaddress;
    }

    public static void kof2000_bankswitch_w(int data) {
        int bankaddress;
        int[] bankoffset = {
                0x00_0000, 0x10_0000, 0x20_0000, 0x30_0000, // 00
                0x3f_7800, 0x4f_7800, 0x3f_f800, 0x4f_f800, // 04
                0x40_7800, 0x50_7800, 0x40_f800, 0x50_f800, // 08
                0x41_6800, 0x51_6800, 0x41_d800, 0x51_d800, // 12
                0x42_4000, 0x52_4000, 0x52_3800, 0x62_3800, // 16
                0x52_6000, 0x62_6000, 0x52_8000, 0x62_8000, // 20
                0x52_a000, 0x62_a000, 0x52_b800, 0x62_b800, // 24
                0x52_d000, 0x62_d000, 0x52_e800, 0x62_e800, // 28
                0x61_8000, 0x61_9000, 0x61_a000, 0x61_a800, // 32
        };
        data = (((data >> 15) & 1) << 0) +
                (((data >> 14) & 1) << 1) +
                (((data >> 7) & 1) << 2) +
                (((data >> 3) & 1) << 3) +
                (((data >> 10) & 1) << 4) +
                (((data >> 5) & 1) << 5);
        bankaddress = 0x10_0000 + bankoffset[data];
        main_cpu_bank_address = bankaddress;
    }

    public static void pvc_prot1() {
        byte b1, b2;
        b1 = pvc_cartridge_ram[0x1fe0];
        b2 = pvc_cartridge_ram[0x1fe1];
        pvc_cartridge_ram[0x1fe2] = (byte) ((((b2 >> 4) & 0xf) << 1) | ((b1 >> 5) & 1));
        pvc_cartridge_ram[0x1fe3] = (byte) ((((b2 >> 0) & 0xf) << 1) | ((b1 >> 4) & 1));
        pvc_cartridge_ram[0x1fe4] = (byte) (b1 >> 7);
        pvc_cartridge_ram[0x1fe5] = (byte) ((((b1 >> 0) & 0xf) << 1) | ((b1 >> 6) & 1));
    }

    public static void pvc_prot2() {
        byte b1, b2, b3, b4;
        b1 = pvc_cartridge_ram[0x1fe8];
        b2 = pvc_cartridge_ram[0x1fe9];
        b3 = pvc_cartridge_ram[0x1fea];
        b4 = pvc_cartridge_ram[0x1feb];
        pvc_cartridge_ram[0x1fec] = (byte) ((b4 >> 1) | ((b2 & 1) << 4) | ((b1 & 1) << 5) | ((b4 & 1) << 6) | ((b3 & 1) << 7));
        pvc_cartridge_ram[0x1fed] = (byte) ((b2 >> 1) | ((b1 >> 1) << 4));
    }

    public static void pvc_write_bankswitch() {
        int bankaddress;
        bankaddress = pvc_cartridge_ram[0xff8 * 2] + pvc_cartridge_ram[0xff9 * 2] * 0x1_0000 + pvc_cartridge_ram[0xff9 * 2 + 1] * 0x100;
        pvc_cartridge_ram[0x1ff0] &= (byte) 0xfe;
        pvc_cartridge_ram[0x1ff1] = (byte) 0xa0;
        pvc_cartridge_ram[0x1ff2] &= 0x7f;
        main_cpu_bank_address = bankaddress + 0x10_0000;
    }

    public static void cthd2003_bankswitch_w(int data) {
        int bankaddress;
        int[] cthd2003_banks = new int[] {
                1, 0, 1, 0, 1, 0, 3, 2,
        };
        bankaddress = 0x10_0000 + cthd2003_banks[data & 7] * 0x10_0000;
        main_cpu_bank_address = bankaddress;
    }

    public static short mslug5_prot_r() {
        return 0xa0;
    }

    public static void ms5plus_bankswitch_w(int offset, int data) {
        int bankaddress;
        if ((offset == 0) && (data == 0xa0)) {
            bankaddress = 0xa0;
            main_cpu_bank_address = bankaddress;
        } else if (offset == 2) {
            data = data >> 4;
            bankaddress = data * 0x10_0000;
            main_cpu_bank_address = bankaddress;
        }
    }

    public static void kof2003_w(int offset) {
        if (offset == 0x1ff0 / 2 || offset == 0x1ff2 / 2) {
            int address = (extra_ram[0x1ff2] << 16) | (extra_ram[0x1ff3] << 8) | extra_ram[0x1ff0];
            byte prt = extra_ram[0x1ff3];
            extra_ram[0x1ff0] &= (byte) 0xfe;
            extra_ram[0x1ff1] = (byte) 0xa0;
            extra_ram[0x1ff2] &= 0x7f;
            main_cpu_bank_address = address + 0x10_0000;
            Memory.mainrom[0x5_8197] = prt;
        }
    }

    public static void kof2003p_w(int offset) {
        if (offset == 0x1ff0 / 2 || offset == 0x1ff2 / 2) {
            int address = (extra_ram[0x1ff2] << 16) | (extra_ram[0x1ff3] << 8) | extra_ram[0x1ff1];
            byte prt = extra_ram[0x1ff3];
            extra_ram[0x1ff1] &= (byte) 0xfe;
            extra_ram[0x1ff2] &= 0x7f;
            main_cpu_bank_address = address + 0x10_0000;
            Memory.mainrom[0x5_8197] = prt;
        }
    }

    public static byte sbp_protection_r(int offset) {
        byte origdata = Memory.mainrom[offset + 0x200];
        byte data = (byte) BITSWAP8(origdata, 3, 2, 1, 0, 7, 6, 5, 4);
        int realoffset = 0x200 + offset;
        if (realoffset == 0xd5e || realoffset == 0xd5f)
            return origdata;
        return data;
    }

    public static void sbp_protection_w(int offset, int data) {
        int realoffset = 0x200 + (offset * 2);
        if (realoffset == 0x1080) {
            if (data == 0x4e75) {
            } else if (data == 0xffff) {
            }
        }
    }

    public static void kof10th_custom_w(int offset, byte data) {
        if (extra_ram[0x1ffc] == 0 && extra_ram[0x1ffd] == 0) {
            Memory.mainrom[0xe_0000 + offset] = data;
        } else {
            Neogeo.fixedbiosrom[offset / 2] = (byte) BITSWAP8(data, 7, 6, 0, 4, 3, 2, 1, 5);
        }
    }

    public static void kof10th_custom_w(int offset, short data) {
        if (extra_ram[0x1ffc] == 0 && extra_ram[0x1ffd] == 0) {
            Memory.mainrom[0xe_0000 + offset] = (byte) (data >> 8);
            Memory.mainrom[0xe_0000 + offset + 1] = (byte) data;
        } else {
            Neogeo.fixedbiosrom[offset / 2] = (byte) BITSWAP8(data, 7, 6, 0, 4, 3, 2, 1, 5);
        }
    }

    public static void kof10th_bankswitch_w(int offset, byte data) {
        if (offset >= 0xb_e000) {
            if (offset == 0xb_fff0) {
                int bank = 0x10_0000 + ((data & 7) << 20);
                if (bank >= 0x70_0000)
                    bank = 0x10_0000;
                main_cpu_bank_address = bank;
            } else if (offset == 0xb_fff8 && (extra_ram[0x1ff8] * 0x100 + extra_ram[0x1ff9] != data)) {
                System.arraycopy(Memory.mainrom, ((data & 1) != 0) ? 0x81_0000 : 0x71_0000, Memory.mainrom, 0x1_0000, 0xc_ffff);
            }
            extra_ram[(offset - 0xb_e000) & 0x1fff] = data;
        }
    }

    public static void kof10th_bankswitch_w(int offset, short data) {
        if (offset >= 0xb_e000) {
            if (offset == 0xb_fff0) {
                int bank = 0x10_0000 + ((data & 7) << 20);
                if (bank >= 0x70_0000)
                    bank = 0x10_0000;
                main_cpu_bank_address = bank;
            } else if (offset == 0xb_fff8 && (extra_ram[0x1ff8] * 0x100 + extra_ram[0x1ff9] != data)) {
                System.arraycopy(Memory.mainrom, ((data & 1) != 0) ? 0x81_0000 : 0x71_0000, Memory.mainrom, 0x1_0000, 0xc_ffff);
            }
            extra_ram[(offset - 0xb_e000) & 0x1fff] = (byte) (data >> 8);
            extra_ram[((offset - 0xb_e000) & 0x1fff) + 1] = (byte) data;
        }
    }

    public static int BIT(int x, int n) {
        return (x >> n) & 1;
    }

    public static int BITSWAP8(int val, int B7, int B6, int B5, int B4, int B3, int B2, int B1, int B0) {
        return ((BIT(val, B7) << 7) |
                (BIT(val, B6) << 6) |
                (BIT(val, B5) << 5) |
                (BIT(val, B4) << 4) |
                (BIT(val, B3) << 3) |
                (BIT(val, B2) << 2) |
                (BIT(val, B1) << 1) |
                (BIT(val, B0) << 0));
    }

    public static int BITSWAP16(int val, int B15, int B14, int B13, int B12, int B11, int B10, int B9, int B8, int B7, int B6, int B5, int B4, int B3, int B2, int B1, int B0) {
        return ((BIT(val, B15) << 15) |
                (BIT(val, B14) << 14) |
                (BIT(val, B13) << 13) |
                (BIT(val, B12) << 12) |
                (BIT(val, B11) << 11) |
                (BIT(val, B10) << 10) |
                (BIT(val, B9) << 9) |
                (BIT(val, B8) << 8) |
                (BIT(val, B7) << 7) |
                (BIT(val, B6) << 6) |
                (BIT(val, B5) << 5) |
                (BIT(val, B4) << 4) |
                (BIT(val, B3) << 3) |
                (BIT(val, B2) << 2) |
                (BIT(val, B1) << 1) |
                (BIT(val, B0) << 0));
    }

//#endregion

//#region State


    public static void SaveStateBinary(BinaryWriter writer) {
        int i, j;
        writer.write(dsw);
        writer.write(display_position_interrupt_control);
        writer.write(display_counter);
        writer.write(vblank_interrupt_pending);
        writer.write(display_position_interrupt_pending);
        writer.write(irq3_pending);
        writer.write(controller_select);
        writer.write(main_cpu_bank_address);
        writer.write(main_cpu_vector_table_source);
        writer.write(audio_cpu_banks, 0, 4);
        writer.write(save_ram_unlocked);
        writer.write(audio_cpu_nmi_enabled);
        writer.write(audio_cpu_nmi_pending);
        writer.write(mainram2, 0, 0x1_0000);
        writer.write(pvc_cartridge_ram, 0, 0x2000);
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 0x1000; j++) {
                writer.write(palettes[i][j]);
            }
        }
        for (i = 0; i < 0x1_0000; i++) {
            writer.write(neogeo_videoram[i]);
        }
        writer.write(videoram_read_buffer);
        writer.write(videoram_modulo);
        writer.write(videoram_offset);
        writer.write(fixed_layer_source);
        writer.write(screen_dark);
        writer.write(palette_bank);
        writer.write(neogeo_scanline_param);
        writer.write(auto_animation_speed);
        writer.write(auto_animation_disabled);
        writer.write(auto_animation_counter);
        writer.write(auto_animation_frame_counter);
        writer.write(Memory.mainram, 0, 0x1_0000);
        MC68000.m1.SaveStateBinary(writer);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        writer.write(Video.screenstate.vblank_start_time.seconds);
        writer.write(Video.screenstate.vblank_start_time.attoseconds);
        writer.write(Video.screenstate.frame_number);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        AY8910.AA8910[0].SaveStateBinary(writer);
        YM2610.F2610.SaveStateBinary(writer);
        for (i = 0; i < 2; i++) {
            writer.write(Sound.latched_value[i]);
        }
        for (i = 0; i < 2; i++) {
            writer.write(Sound.utempdata[i]);
        }
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(Sound.ym2610stream.output_sampindex);
        writer.write(Sound.ym2610stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
        Pd4900a.SaveStateBinary(writer);
    }

    public static void LoadStateBinary(BinaryReader reader) {
        try {
            int i, j;
            dsw = reader.readByte();
            display_position_interrupt_control = reader.readByte();
            display_counter = reader.readUInt32();
            vblank_interrupt_pending = reader.readInt32();
            display_position_interrupt_pending = reader.readInt32();
            irq3_pending = reader.readInt32();
            controller_select = reader.readByte();
            main_cpu_bank_address = reader.readInt32();
            main_cpu_vector_table_source = reader.readByte();
            audio_cpu_banks = reader.readBytes(4);
            save_ram_unlocked = reader.readByte();
            audio_cpu_nmi_enabled = reader.readBoolean();
            audio_cpu_nmi_pending = reader.readBoolean();
            mainram2 = reader.readBytes(0x1_0000);
            pvc_cartridge_ram = reader.readBytes(0x2000);
            for (i = 0; i < 2; i++) {
                for (j = 0; j < 0x1000; j++) {
                    palettes[i][j] = reader.readUInt16();
                }
            }
            for (i = 0; i < 0x1_0000; i++) {
                neogeo_videoram[i] = reader.readUInt16();
            }
            videoram_read_buffer = reader.readUInt16();
            videoram_modulo = reader.readUInt16();
            videoram_offset = reader.readUInt16();
            fixed_layer_source = reader.readByte();
            screen_dark = reader.readByte();
            palette_bank = reader.readByte();
            neogeo_scanline_param = reader.readInt32();
            auto_animation_speed = reader.readByte();
            auto_animation_disabled = reader.readByte();
            auto_animation_counter = reader.readInt32();
            auto_animation_frame_counter = reader.readInt32();
            Memory.mainram = reader.readBytes(0x1_0000);
            MC68000.m1.LoadStateBinary(reader);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.screenstate.vblank_start_time.seconds = reader.readInt32();
            Video.screenstate.vblank_start_time.attoseconds = reader.readInt64();
            Video.screenstate.frame_number = reader.readInt64();
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            AY8910.AA8910[0].LoadStateBinary(reader);
            YM2610.F2610.LoadStateBinary(reader);
            for (i = 0; i < 2; i++) {
                Sound.latched_value[i] = reader.readUInt16();
            }
            for (i = 0; i < 2; i++) {
                Sound.utempdata[i] = reader.readUInt16();
            }
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            Sound.ym2610stream.output_sampindex = reader.readInt32();
            Sound.ym2610stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
            Pd4900a.LoadStateBinary(reader);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Input

    public static void loop_inputports_neogeo_standard() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short3 &= ~0x0001;
        } else {
            short3 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short3 &= ~0x0002;
        } else {
            short3 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short2 &= ~0x0400;
        } else {
            short2 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            short0 &= ~0x0800;
        } else {
            short0 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            short0 &= ~0x0400;
        } else {
            short0 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            short0 &= ~0x0200;
        } else {
            short0 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            short0 &= ~0x0100;
        } else {
            short0 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short0 &= ~0x1000;
        } else {
            short0 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short0 &= ~0x2000;
        } else {
            short0 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            short0 &= ~0x4000;
        } else {
            short0 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_I)) {
            short0 &= (short) ~0x8000;
        } else {
            short0 |= (short) 0x8000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_O)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            short1 &= ~0x0800;
        } else {
            short1 |= 0x0800;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            short1 &= ~0x0400;
        } else {
            short1 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            short1 &= ~0x0200;
        } else {
            short1 |= 0x0200;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            short1 &= ~0x0100;
        } else {
            short1 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short1 &= ~0x1000;
        } else {
            short1 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short1 &= ~0x2000;
        } else {
            short1 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD5)) {
            short1 &= (short) ~0x8000;
        } else {
            short1 |= (short) 0x8000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD6)) {

        } else {

        }
        if (Keyboard.IsPressed(KeyEvent.VK_R)) {
            short3 &= ~0x0004;
        } else {
            short3 |= 0x0004;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_T)) {
            short4 &= ~0x0080;
        } else {
            short4 |= 0x0080;
        }
    }

    public static void loop_inputports_neogeo_irrmaze() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            short3 &= ~0x0001;
        } else {
            short3 |= 0x0001;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            short3 &= ~0x0002;
        } else {
            short3 |= 0x0002;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            short2 &= ~0x0100;
        } else {
            short2 |= 0x0100;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            short2 &= ~0x0400;
        } else {
            short2 |= 0x0400;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            short1 &= ~0x1000;
        } else {
            short1 |= 0x1000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            short1 &= ~0x2000;
        } else {
            short1 |= 0x2000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            short1 &= ~0x4000;
        } else {
            short1 |= 0x4000;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            short1 &= (short) ~0x8000;
        } else {
            short1 |= (short) 0x8000;
        }
        Inptport.frame_update_analog_field_irrmaze_p0(Inptport.analog_p0);
        Inptport.frame_update_analog_field_irrmaze_p1(Inptport.analog_p1);
    }

    public static void record_port() {
        if (short0 != short0_old || short1 != short1_old || short2 != short2_old || short3 != short3_old || short4 != short4_old) {
            short0_old = short0;
            short1_old = short1;
            short2_old = short2;
            short3_old = short3;
            short4_old = short4;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(short0);
            Mame.bwRecord.write(short1);
            Mame.bwRecord.write(short2);
            Mame.bwRecord.write(short3);
            Mame.bwRecord.write(short4);
        }
    }

    public static void replay_port() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                short0_old = Mame.brRecord.readInt16();
                short1_old = Mame.brRecord.readInt16();
                short2_old = Mame.brRecord.readInt16();
                short3_old = Mame.brRecord.readInt16();
                short4_old = Mame.brRecord.readInt16();
            } catch (IOException e) {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            short0 = short0_old;
            short1 = short1_old;
            short2 = short2_old;
            short3 = short3_old;
            short4 = short4_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Video

    public static byte[] sprite_gfx;
    public static int sprite_gfx_address_mask;
    public static short[] neogeo_videoram;
    public static short[][] palettes;
    public static int[] pens;
    private static int neogeo_scanline_param;
    private static byte palette_bank;
    private static byte screen_dark;
    private static short videoram_read_buffer;
    private static short videoram_modulo;
    private static short videoram_offset;
    public static byte fixed_layer_source;
    public static int neogeo_fixed_layer_bank_type;
    private static byte auto_animation_speed;
    public static byte auto_animation_disabled;
    public static int auto_animation_counter;
    private static int auto_animation_frame_counter;
    public static EmuTimer auto_animation_timer, sprite_line_timer;
    private static double[] rgb_weights_normal;
    private static double[] rgb_weights_normal_bit15;
    private static double[] rgb_weights_dark;
    private static double[] rgb_weights_dark_bit15;
    public static int[][] zoom_x_tables;
    private static int[] transarray, bgarray;
    private static int trans_color;

    private static byte combine_5_weights(double[] tab, int w0, int w1, int w2, int w3, int w4) {
        return (byte) (tab[0] * w0 + tab[1] * w1 + tab[2] * w2 + tab[3] * w3 + tab[4] * w4 + 0.5);
    }

    private static int get_pen(short data) {
        double[] weights;
        byte r, g, b;
        if (screen_dark != 0) {
            if ((data & 0x8000) != 0)
                weights = rgb_weights_dark_bit15;
            else
                weights = rgb_weights_dark;
        } else {
            if ((data & 0x8000) != 0)
                weights = rgb_weights_normal_bit15;
            else
                weights = rgb_weights_normal;
        }
        r = combine_5_weights(weights,
                (data >> 11) & 0x01,
                (data >> 10) & 0x01,
                (data >> 9) & 0x01,
                (data >> 8) & 0x01,
                (data >> 14) & 0x01);
        g = combine_5_weights(weights,
                (data >> 7) & 0x01,
                (data >> 6) & 0x01,
                (data >> 5) & 0x01,
                (data >> 4) & 0x01,
                (data >> 13) & 0x01);
        b = combine_5_weights(weights,
                (data >> 3) & 0x01,
                (data >> 2) & 0x01,
                (data >> 1) & 0x01,
                (data >> 0) & 0x01,
                (data >> 12) & 0x01);
        return (r << 16) | (g << 8) | b;
    }

    public static void regenerate_pens() {
        int i;
        for (i = 0; i < 0x1000; i++) {
            pens[i] = get_pen(palettes[palette_bank][i]);
        }
        for (i = 0; i < 384 * 264; i++) {
            bgarray[i] = pens[0xfff];
        }
    }

    private static void neogeo_set_palette_bank(byte data) {
        if (data != palette_bank) {
            palette_bank = data;
            regenerate_pens();
        }
    }

    private static void neogeo_set_screen_dark(byte data) {
        if (data != screen_dark) {
            screen_dark = data;
            regenerate_pens();
        }
    }

    private static void neogeo_paletteram_w(int offset, short data) {
        int i;
        palettes[palette_bank][offset] = data;
        pens[offset] = get_pen(data);
        if (offset == 0xfff) {
            for (i = 0; i < 384 * 264; i++) {
                bgarray[i] = pens[0xfff];
            }
        }
    }

    public static void auto_animation_timer_callback() {
        if (auto_animation_frame_counter == 0) {
            auto_animation_frame_counter = auto_animation_speed;
            auto_animation_counter = auto_animation_counter + 1;
        } else {
            auto_animation_frame_counter = auto_animation_frame_counter - 1;
        }
        Timer.adjustPeriodic(auto_animation_timer, Video.video_screen_get_time_until_pos(0, 0), Attotime.ATTOTIME_NEVER);
    }

    private static void create_auto_animation_timer() {
        auto_animation_timer = Timer.allocCommon(Neogeo::auto_animation_timer_callback, "auto_animation_timer_callback", false);
    }

    private static void start_auto_animation_timer() {
        Timer.adjustPeriodic(auto_animation_timer, Video.video_screen_get_time_until_pos(0, 0), Attotime.ATTOTIME_NEVER);
    }

    private static int rows_to_height(int rows) {
        if ((rows == 0) || (rows > 0x20))
            rows = 0x20;
        return rows * 0x10;
    }

    public static boolean sprite_on_scanline(int scanline, int y, int rows) {
        int max_y = (y + rows_to_height(rows) - 1) & 0x1ff;
        return (((max_y >= y) && (scanline >= y) && (scanline <= max_y)) ||
                ((max_y < y) && ((scanline >= y) || (scanline <= max_y))));
    }

    private static void draw_sprites(int iBitmap, int scanline) {
        int x_2, code_2;
        int x, y, rows, zoom_x, zoom_y, sprite_list_offset, sprite_index, max_sprite_index, sprite_number, sprite_y, tile, attr_and_code_offs, code, zoom_x_table_offset, gfx_offset, line_pens_offset, x_inc, sprite_line, zoom_line;
        short y_control, zoom_control, attr;
        byte sprite_y_and_tile;
        boolean invert;
        y = 0;
        x = 0;
        rows = 0;
        zoom_y = 0;
        zoom_x = 0;
        if ((scanline & 0x01) != 0) {
            sprite_list_offset = 0x8680;
        } else {
            sprite_list_offset = 0x8600;
        }
        for (max_sprite_index = 95; max_sprite_index >= 0; max_sprite_index--) {
            if (neogeo_videoram[sprite_list_offset + max_sprite_index] != 0) {
                break;
            }
        }
        if (max_sprite_index != 95) {
            max_sprite_index = max_sprite_index + 1;
        }
        for (sprite_index = 0; sprite_index < max_sprite_index; sprite_index++) {
            sprite_number = neogeo_videoram[sprite_list_offset + sprite_index] & 0x1ff;
            y_control = neogeo_videoram[0x8200 | sprite_number];
            zoom_control = neogeo_videoram[0x8000 | sprite_number];
            x_2 = neogeo_videoram[0x8400 | sprite_number];
            code_2 = neogeo_videoram[sprite_number << 6];
            if ((y_control & 0x40) != 0) {
                x = (x + zoom_x + 1) & 0x01ff;
                zoom_x = (zoom_control >> 8) & 0x0f;
            } else {
                y = 0x200 - (y_control >> 7);
                x = neogeo_videoram[0x8400 | sprite_number] >> 7;
                zoom_y = zoom_control & 0xff;
                zoom_x = (zoom_control >> 8) & 0x0f;
                rows = y_control & 0x3f;
            }
            if ((x >= 0x140) && (x <= 0x1f0)) {
                continue;
            }
            if (sprite_on_scanline(scanline, y, rows)) {
                sprite_line = (scanline - y) & 0x1ff;
                zoom_line = sprite_line & 0xff;
                invert = ((sprite_line & 0x100) != 0) ? true : false;
                if (invert) {
                    zoom_line ^= 0xff;
                }
                if (rows > 0x20) {
                    zoom_line = zoom_line % ((zoom_y + 1) << 1);
                    if (zoom_line > zoom_y) {
                        zoom_line = ((zoom_y + 1) << 1) - 1 - zoom_line;
                        invert = !invert;
                    }
                }
                sprite_y_and_tile = zoomyrom[(zoom_y << 8) | zoom_line];
                sprite_y = sprite_y_and_tile & 0x0f;
                tile = sprite_y_and_tile >> 4;
                if (invert) {
                    sprite_y ^= 0x0f;
                    tile ^= 0x1f;
                }
                attr_and_code_offs = (sprite_number << 6) | (tile << 1);
                attr = neogeo_videoram[attr_and_code_offs + 1];
                code = ((attr << 12) & 0x7_0000) | neogeo_videoram[attr_and_code_offs];
                if (auto_animation_disabled == 0) {
                    if ((attr & 0x0008) != 0) {
                        code = (code & ~0x07) | (auto_animation_counter & 0x07);
                    } else if ((attr & 0x0004) != 0) {
                        code = (code & ~0x03) | (auto_animation_counter & 0x03);
                    }
                }
                if ((attr & 0x0002) != 0) {
                    sprite_y ^= 0x0f;
                }
                zoom_x_table_offset = 0;
                gfx_offset = ((code << 8) | (sprite_y << 4)) & sprite_gfx_address_mask;
                line_pens_offset = attr >> 8 << 4;
                if ((attr & 0x0001) != 0) {
                    gfx_offset = gfx_offset + 0x0f;
                    x_inc = -1;
                } else {
                    x_inc = 1;
                }
                int pixel_addr_offsetx, pixel_addr_offsety;
                if (x <= 0x01f0) {
                    int i;
                    pixel_addr_offsetx = x + NEOGEO_HBEND;
                    pixel_addr_offsety = scanline;
                    for (i = 0; i < 0x10; i++) {
                        if (zoom_x_tables[zoom_x][zoom_x_table_offset] != 0) {
                            if (sprite_gfx[gfx_offset] != 0) {
                                Video.bitmapbaseN[iBitmap][pixel_addr_offsety * 384 + pixel_addr_offsetx] = pens[line_pens_offset + sprite_gfx[gfx_offset]];
                            }
                            pixel_addr_offsetx++;
                        }
                        zoom_x_table_offset++;
                        gfx_offset += x_inc;
                    }
                } else {
                    int i;
                    int x_save = x;
                    pixel_addr_offsetx = NEOGEO_HBEND;
                    pixel_addr_offsety = scanline;
                    for (i = 0; i < 0x10; i++) {
                        if (zoom_x_tables[zoom_x][zoom_x_table_offset] != 0) {
                            if (x >= 0x200) {
                                if (sprite_gfx[gfx_offset] != 0) {
                                    Video.bitmapbaseN[iBitmap][pixel_addr_offsety * 384 + pixel_addr_offsetx] = pens[line_pens_offset + sprite_gfx[gfx_offset]];
                                }
                                pixel_addr_offsetx++;
                            }
                            x++;
                        }
                        zoom_x_table_offset++;
                        gfx_offset += x_inc;
                    }
                    x = x_save;
                }
            }
        }
    }

    private static void parse_sprites(int scanline) {
        short sprite_number, y_control;
        int y = 0;
        int rows = 0;
        int sprite_list_offset;
        int active_sprite_count = 0;
        if ((scanline & 0x01) != 0) {
            sprite_list_offset = 0x8680;
        } else {
            sprite_list_offset = 0x8600;
        }
        for (sprite_number = 0; sprite_number < 381; sprite_number++) {
            y_control = neogeo_videoram[0x8200 | sprite_number];
            if ((~y_control & 0x40) != 0) {
                y = 0x200 - (y_control >> 7);
                rows = y_control & 0x3f;
            }
            if (rows == 0) {
                continue;
            }
            if (!sprite_on_scanline(scanline, y, rows)) {
                continue;
            }
            neogeo_videoram[sprite_list_offset] = sprite_number;
            sprite_list_offset++;
            active_sprite_count++;
            if (active_sprite_count == 96) {
                break;
            }
        }
        for (; active_sprite_count <= 96; active_sprite_count++) {
            neogeo_videoram[sprite_list_offset] = 0;
            sprite_list_offset++;
        }
    }

    public static void sprite_line_timer_callback() {
        int scanline = neogeo_scanline_param;
        if (scanline != 0) {
            Video.video_screen_update_partial(scanline - 1);
        }
        parse_sprites(scanline);
        scanline = (scanline + 1) % 264;
        neogeo_scanline_param = scanline;
        Timer.adjustPeriodic(sprite_line_timer, Video.video_screen_get_time_until_pos(scanline, 0), Attotime.ATTOTIME_NEVER);
    }

    private static void create_sprite_line_timer() {
        sprite_line_timer = Timer.allocCommon(Neogeo::sprite_line_timer_callback, "sprite_line_timer_callback", false);
    }

    private static void start_sprite_line_timer() {
        neogeo_scanline_param = 0;
        Timer.adjustPeriodic(sprite_line_timer, Video.video_screen_get_time_until_pos(0, 0), Attotime.ATTOTIME_NEVER);
    }

    private static void draw_fixed_layer(int iBitmap, int scanline) {
        int i, j, x, y;
        int[] garouoffsets = new int[32], pix_offsets = new int[] {0x10, 0x18, 0x00, 0x08};
        byte[] gfx_base;
        int addr_mask;
        int gfx_offset, char_pens_offset;
        byte data;
        boolean banked;
        int garoubank, k, code;
        short code_and_palette;
        if (fixed_layer_source != 0) {
            gfx_base = fixedrom;
            addr_mask = fixedrom.length - 1;
        } else {
            gfx_base = fixedbiosrom;
            addr_mask = fixedbiosrom.length - 1;
        }
        int video_data_offset = 0x7000 | (scanline >> 3);
        banked = (fixed_layer_source != 0) && (addr_mask > 0x1_ffff);
        if (banked && neogeo_fixed_layer_bank_type == 1) {
            garoubank = 0;
            k = 0;
            y = 0;
            while (y < 32) {
                if (neogeo_videoram[0x7500 + k] == 0x0200 && (neogeo_videoram[0x7580 + k] & 0xff00) == 0xff00) {
                    garoubank = neogeo_videoram[0x7580 + k] & 3;
                    garouoffsets[y++] = garoubank;
                }
                garouoffsets[y++] = garoubank;
                k += 2;
            }
        }
        for (x = 0; x < 40; x++) {
            code_and_palette = neogeo_videoram[video_data_offset];
            code = code_and_palette & 0x0fff;
            if (banked) {
                y = scanline >> 3;
                switch (neogeo_fixed_layer_bank_type) {
                    case 1:
                        code += 0x1000 * (garouoffsets[(y - 2) & 31] ^ 3);
                        break;
                    case 2:
                        code += 0x1000 * (((neogeo_videoram[0x7500 + ((y - 1) & 31) + 32 * (x / 6)] >> (5 - (x % 6)) * 2) & 3) ^ 3);
                        break;
                }
            }
            data = 0;
            gfx_offset = ((code << 5) | (scanline & 0x07)) & addr_mask;
            char_pens_offset = code_and_palette >> 12 << 4;
            for (i = 0; i < 8; i++) {
                if ((i & 0x01) != 0) {
                    data = (byte) (data >> 4);
                } else {
                    data = gfx_base[gfx_offset + pix_offsets[i >> 1]];
                }
                if ((data & 0x0f) != 0) {
                    Video.bitmapbaseN[iBitmap][384 * scanline + 30 + x * 8 + i] = pens[char_pens_offset + (data & 0x0f)];
                }
            }
            video_data_offset += 0x20;
        }
    }

    private static void optimize_sprite_data() {
        sprite_gfx_address_mask = spritesrom.length * 2 - 1;
        for (int i = 0; i < spritesrom.length; i++) {
            sprite_gfx[i * 2] = (byte) ((spritesrom[i] & 0xf0) >> 4);
            sprite_gfx[i * 2 + 1] = (byte) (spritesrom[i] & 0x0f);
        }
    }

    private static short get_video_control() {
        int ret;
        int v_counter;
        v_counter = Video.video_screen_get_vpos() + 0x100;
        if (v_counter >= 0x200)
            v_counter = v_counter - NEOGEO_VTOTAL;
        ret = (v_counter << 7) | (auto_animation_counter & 0x07);
        return (short) ret;
    }

    private static short neogeo_video_register_r(int offset) {
        short ret;
        switch (offset) {
            default:
            case 0x00:
            case 0x01:
                ret = videoram_read_buffer;
                break;
            case 0x02:
                ret = videoram_modulo;
                break;
            case 0x03:
                ret = get_video_control();
                break;
        }
        return ret;
    }

    private static void neogeo_video_register_w(int offset, short data) {
        switch (offset) {
            case 0x00:
                videoram_offset = data;
                videoram_read_buffer = neogeo_videoram[videoram_offset];
                break;
            case 0x01:
                if (videoram_offset == 0x842d && data == 0x0) {
                    int i1 = 1;
                }
                if (videoram_offset == 0x8263 && data == 0xb102) {
                    int i1 = 1;
                }
                if (videoram_offset == 0x18c0 && data == 0xcb06) {
                    int i1 = 1;
                }
                neogeo_videoram[videoram_offset] = data;
                videoram_offset = (short) ((videoram_offset & 0x8000) | ((videoram_offset + videoram_modulo) & 0x7fff));
                videoram_read_buffer = neogeo_videoram[videoram_offset];
                break;
            case 0x02:
                videoram_modulo = data;
                break;
            case 0x03:
                auto_animation_speed = (byte) (data >> 8);
                auto_animation_disabled = (byte) (data & 0x08);
                display_position_interrupt_control = (byte) (data & 0xf0);
                break;
            case 0x04:
                display_counter = (display_counter & 0x0000_ffff) | ((int) data << 16);
                break;
            case 0x05:
                display_counter = (display_counter & 0xffff_0000) | data;
                if ((display_position_interrupt_control & 0x20) != 0) {
                    adjust_display_position_interrupt_timer();
                }
                break;
            case 0x06:
                if ((data & 0x01) != 0)
                    irq3_pending = 0;
                if ((data & 0x02) != 0)
                    display_position_interrupt_pending = 0;
                if ((data & 0x04) != 0)
                    vblank_interrupt_pending = 0;
                update_interrupts();
                break;
            case 0x07:
                break;
        }
    }

    public static void video_start_neogeo() {
        sprite_gfx = new byte[spritesrom.length * 2];
        neogeo_videoram = new short[0x1_0000];
        palettes = new short[2][0x1000];
        pens = new int[0x1000];
        transarray = new int[384 * 264];
        bgarray = new int[384 * 264];
        trans_color = 0xffff_00ff;
        rgb_weights_normal = new double[] {
                138.24919544273797, 64.712389372353314, 30.414823021125919, 13.824919571647138, 7.7986725921356159
        };
        rgb_weights_normal_bit15 = new double[] {
                136.26711031260342, 63.784604843122239, 29.978764292156193, 13.626711058241233, 7.6868626613063098
        };
        rgb_weights_dark = new double[] {
                77.012238506947057, 36.048281863327709, 16.942692484743652, 7.7012238659431276, 4.3442801368916566
        };
        rgb_weights_dark_bit15 = new double[] {
                76.322306339305158, 35.725334891159271, 16.790907407744047, 7.6322306490423326, 4.3053608862660706
        };
        zoom_x_tables = new int[][] {
                {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0},
                {0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0},
                {0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0},
                {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0},
                {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0},
                {1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0},
                {1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1},
                {1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1},
                {1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        Arrays.fill(palettes[0], 0, 0x1000, (byte) 0);
        Arrays.fill(palettes[1], 0, 0x1000, (byte) 0);
        Arrays.fill(pens, 0, 0x1000, 0);
        Arrays.fill(neogeo_videoram, 0, 0x1_0000, (short) 0);
        for (int i = 0; i < 384 * 264; i++) {
            transarray[i] = trans_color;
        }
        create_sprite_line_timer();
        create_auto_animation_timer();
        optimize_sprite_data();
        videoram_read_buffer = 0;
        videoram_offset = 0;
        videoram_modulo = 0;
        auto_animation_speed = 0;
        auto_animation_disabled = 0;
        auto_animation_counter = 0;
        auto_animation_frame_counter = 0;
    }

    public static void video_update_neogeo() {
        System.arraycopy(bgarray, 0, Video.bitmapbaseN[Video.curbitmap], 384 * Video.new_clip.min_y, 384 * (Video.new_clip.max_y - Video.new_clip.min_y + 1));
        draw_sprites(Video.curbitmap, Video.new_clip.min_y);
        draw_fixed_layer(Video.curbitmap, Video.new_clip.min_y);
    }

    public static void video_eof_neogeo() {
    }

//#endregion
}
