/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Inptport;
import m1.emu.Inptport.FrameArray;
import m1.emu.Machine;
import m1.emu.Timer;
import m1.emu.Timer.EmuTimer;
import m1.emu.TrackInfo;
import m1.mame.capcom.Capcom;
import m1.mame.cps.CPS;
import m1.mame.dataeast.Dataeast;
import m1.mame.konami68000.Konami68000;
import m1.mame.m72.M72;
import m1.mame.m92.M92;
import m1.mame.namcos1.Namcos1;
import m1.mame.neogeo.Neogeo;
import m1.mame.taito.Taito;
import m1.mame.taitob.Taitob;
import m1.mame.tehkan.Tehkan;
import m1.sound.AY8910.ay8910_interface;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


public class Sound {

    private static final Logger logger = getLogger(Sound.class.getName());

    public static int nZero;
    public static EmuTimer sound_update_timer;
    private static int[] leftmix, rightmix;
    private static byte[] finalmixb;
    private static int sound_muted;
    public static short[] latched_value, utempdata;
    public static Runnable sound_update;
    public static SourceDataLine buf2;
    private static int stream_buffer_in;
    public static int iRecord;

    // track_delegate
    public static Consumer<TrackInfo> playtrack, stoptrack;
    public static Consumer<TrackInfo> stopandplaytrack;

    // sound_delegate
    public static Consumer<Short> PlayDelegate, StopDelegate;

    // sound_delegate2
    public static BiConsumer<Short, Short> StopAndPlayDelegate;

    static void start() {
        try {
            AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
            buf2 = (SourceDataLine) AudioSystem.getLine(info);
            buf2.addLineListener(event -> logger.log(Level.DEBUG, event.getType()));
            buf2.start();
        } catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void sound_init() throws IOException {
        iRecord = 0;
        leftmix = new int[0x3c0];
        rightmix = new int[0x3c0];
        finalmixb = new byte[0xf00];
        sound_muted = 0;
        start();
        Machine.FORM.LoadTrack();
        last_update_second = 0;
        //WavWrite.CreateSoundFile("tmp/2.wav");
        Atime update_frequency = new Atime(0, Attotime.ATTOSECONDS_PER_SECOND / 50);
logger.log(Level.DEBUG, "Machine.sBoard: " + Machine.sBoard);
        switch (Machine.sBoard) {
            case "CPS-1":
                latched_value = new short[2];
                utempdata = new short[2];
                sound_update = Sound::sound_updateC;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                YM2151.ym2151_init(3579545);
                OKI6295.okim6295_start();
                ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                okistream = new Streams(1000000 / 132, 0, 1, OKI6295::okim6295_update);
                mixerstream = new Streams(48000, 3, 0, null);
                playtrack = CPS::play_cps_default;
                stoptrack = CPS::stop_cps_default;
                stopandplaytrack = CPS::stopandplay_cps_default;
                break;
            case "CPS-1(QSound)":
                sound_update = Sound::sound_updateQ;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                QSound.qsound_start();
                qsoundstream = new Streams(4000000 / 166, 0, 2, QSound::qsound_update);
                mixerstream = new Streams(48000, 2, 0, null);
                playtrack = CPS::play_qsound_default;
                stoptrack = CPS::stop_qsound_default;
                stopandplaytrack = CPS::stopandplay_qsound_default;
                Inptport.lsFA.add(new FrameArray(0L, 0xffd, (byte) 0x88));
                Inptport.lsFA.add(new FrameArray(0L, 0xfff, (byte) 0xff));
                break;
            case "CPS2":
                sound_update = Sound::sound_updateQ;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                QSound.qsound_start();
                qsoundstream = new Streams(4000000 / 166, 0, 2, QSound::qsound_update);
                mixerstream = new Streams(48000, 2, 0, null);
                playtrack = CPS::play_cps2_default;
                stoptrack = CPS::stop_cps2_default;
                stopandplaytrack = CPS::stopandplay_cps2_default;
                Inptport.lsFA.add(new FrameArray(0L, 0xffd, (byte) 0x88));
                Inptport.lsFA.add(new FrameArray(0L, 0xfff, (byte) 0xff));
                break;
            case "Data East":
                latched_value = new short[1];
                utempdata = new short[1];
                sound_update = Sound::sound_updateDataeast_pcktgal;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                YM2203.ym2203_start(0, 1500000);
                YM3812.ym3812_start(3000000);
                MSM5205.msm5205_start(0, 384000, Dataeast::pcktgal_adpcm_int, 5);
                ym3812stream = new Streams(41666, 0, 1, FMOpl::ym3812_update_one);
                mixerstream = new Streams(48000, 6, 0, null);
                playtrack = Dataeast::play_dataeast_default;
                stoptrack = Dataeast::stop_dataeast_default;
                stopandplaytrack = Dataeast::stopandplay_dataeast_default;
                break;
            case "Tehkan":
                latched_value = new short[1];
                utempdata = new short[1];
                sound_update = Sound::sound_updateTehkan_pbaction;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                ay8910_interface generic_ay8910 = new ay8910_interface();
                generic_ay8910.flags = 1;
                generic_ay8910.res_load = new int[] {
                        1000, 1000, 1000
                };
                generic_ay8910.portAread = null;
                generic_ay8910.portBread = null;
                generic_ay8910.portAwrite = null;
                generic_ay8910.portBwrite = null;
                AY8910.ay8910_start_ym(6, 0, 1500000, generic_ay8910);
                AY8910.ay8910_start_ym(6, 1, 1500000, generic_ay8910);
                AY8910.ay8910_start_ym(6, 2, 1500000, generic_ay8910);
                mixerstream = new Streams(48000, 9, 0, null);
                playtrack = Tehkan::play_tehkan_default;
                stoptrack = Tehkan::stop_tehkan_default;
                stopandplaytrack = Tehkan::stopandplay_tehkan_default;
                break;
            case "Neo Geo":
                latched_value = new short[2];
                utempdata = new short[2];
                sound_update = Sound::sound_updateN;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                YM2610.ym2610_start(8000000);
                ym2610stream = new Streams(111111, 0, 2, YM2610.F2610::ym2610_update_one);
                mixerstream = new Streams(48000, 3, 0, null);
                playtrack = Neogeo::play_neogeo_default;
                stoptrack = Neogeo::stop_neogeo_default;
                stopandplaytrack = Neogeo::stopandplay_neogeo_default;
                break;
            case "Namco System 1":
                sound_update = Sound::sound_updateNa;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                YM2151.ym2151_init(3579580);
                Namco.namco_start();
                DAC.dac_start();
                ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                namcostream = new Streams(192000, 0, 2, Namco::namco_update_stereo);
                dacstream = new Streams(192000, 0, 1, DAC::DAC_update);
                mixerstream = new Streams(48000, 5, 0, null);
                playtrack = Namcos1::play_namcos1_default;
                stoptrack = Namcos1::stop_namcos1_default;
                stopandplaytrack = Namcos1::stopandplay_namcos1_default;
                Inptport.lsFA.add(new FrameArray(0L, 0, (byte) 0xa6));
                break;
            case "M72":
                latched_value = new short[1];
                utempdata = new short[1];
                sound_update = Sound::sound_updateM72;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                YM2151.ym2151_init(3579545);
                DAC.dac_start();
                ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                dacstream = new Streams(192000, 0, 1, DAC::DAC_update);
                mixerstream = new Streams(48000, 3, 0, null);
                playtrack = M72::play_m72_default;
                stoptrack = M72::stop_m72_default;
                stopandplaytrack = M72::stopandplay_m72_default;
                Inptport.lsFA.add(new FrameArray(0L, 0, (byte) 0));
                break;
            case "M92":
                latched_value = new short[1];
                utempdata = new short[1];
                sound_update = Sound::sound_updateM92;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                YM2151.ym2151_init(3579545);
                Iremga20.iremga20_start();
                ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                iremga20stream = new Streams(894886, 0, 2, Iremga20::iremga20_update);
                mixerstream = new Streams(48000, 4, 0, null);
                playtrack = M92::play_m92_default;
                stoptrack = M92::stop_m92_default;
                stopandplaytrack = M92::stopandplay_m92_default;
                Inptport.lsFA.add(new FrameArray(0L, 0, (byte) 0));
                break;
            case "Taito":
                switch (Machine.sName) {
                    case "tokio":
                        latched_value = new short[2];
                        utempdata = new short[2];
                        sound_update = Sound::sound_updateTaito_tokio;
                        sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                        YM2203.ym2203_start(0, 3000000);
                        mixerstream = new Streams(48000, 4, 0, null);
                        playtrack = Taito::play_taito_default;
                        stoptrack = Taito::stop_taito_default;
                        stopandplaytrack = Taito::stopandplay_taito_default;
                        Inptport.lsFA.add(new FrameArray(0L, 0, (byte) 0xef));
                        break;
                    case "bublbobl":
                        latched_value = new short[2];
                        utempdata = new short[2];
                        sound_update = Sound::sound_updateTaito_bublbobl;
                        sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                        YM2203.ym2203_start(0, 3000000);
                        YM3812.ym3526_start(3000000);
                        ym3526stream = new Streams(41666, 0, 1, FMOpl::ym3526_update_one);
                        mixerstream = new Streams(48000, 5, 0, null);
                        playtrack = Taito::play_taito_default;
                        stoptrack = Taito::stop_taito_default;
                        stopandplaytrack = Taito::stopandplay_taito_default;
                        Inptport.lsFA.add(new FrameArray(0L, 0, (byte) 0xef));
                        break;
                }
                break;
            case "Taito B":
                latched_value = new short[2];
                utempdata = new short[2];
                YM2610.ym2610_start(8000000);
                switch (Machine.sName) {
                    case "pbobble":
                        ym2610stream = new Streams(111111, 0, 2, YM2610.F2610::ym2610b_update_one);
                        break;
                    case "silentd":
                    case "silentdj":
                    case "silentdu":
                        ym2610stream = new Streams(111111, 0, 2, YM2610.F2610::ym2610_update_one);
                        break;
                }
                AY8910.AA8910[0].stream.gain = 0x100;
                ym2610stream.gain = 0x100;
                sound_update = Sound::sound_updateTaitoB;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                mixerstream = new Streams(48000, 3, 0, null);
                playtrack = Taitob::play_taitob_default;
                stoptrack = Taitob::stop_taitob_default;
                stopandplaytrack = Taitob::stopandplay_taitob_default;
                break;
            case "Konami 68000":
                switch (Machine.sName) {
                    case "cuebrick":
                        YM2151.ym2151_init(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        sound_update = Sound::sound_updateKonami68000_cuebrick;
                        mixerstream = new Streams(48000, 2, 0, null);
                        break;
                    case "mia":
                    case "mia2":
                        latched_value = new short[1];
                        utempdata = new short[1];
                        YM2151.ym2151_init(3579545);
                        K007232.k007232_start(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        k007232stream = new Streams(27965, 0, 2, K007232::KDAC_A_update);
                        sound_update = Sound::sound_updateKonami68000_mia;
                        mixerstream = new Streams(48000, 4, 0, null);
                        break;
                    case "tmnt":
                    case "tmntu":
                    case "tmntua":
                    case "tmntub":
                    case "tmht":
                    case "tmhta":
                    case "tmhtb":
                    case "tmntj":
                    case "tmnta":
                    case "tmht2p":
                    case "tmht2pa":
                    case "tmnt2pj":
                    case "tmnt2po":
                        latched_value = new short[1];
                        utempdata = new short[1];
                        YM2151.ym2151_init(3579545);
                        K007232.k007232_start(3579545);
                        Upd7759.upd7759_start(640000);
                        Sample.samples_start();
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        k007232stream = new Streams(27965, 0, 2, K007232::KDAC_A_update);
                        upd7759stream = new Streams(160000, 0, 1, Upd7759::upd7759_update);
                        samplestream = new Streams(48000, 0, 1, Sample::sample_update_sound);
                        sound_update = Sound::sound_updateKonami68000_tmnt;
                        mixerstream = new Streams(48000, 6, 0, null);
                        break;
                    case "punkshot":
                    case "punkshot2":
                    case "punkshotj":
                    case "lgtnfght":
                    case "lgtnfghta":
                    case "lgtnfghtu":
                    case "trigon":
                    case "ssriders":
                    case "ssriderseaa":
                    case "ssridersebd":
                    case "ssridersebc":
                    case "ssridersuda":
                    case "ssridersuac":
                    case "ssridersuab":
                    case "ssridersubc":
                    case "ssridersadd":
                    case "ssridersabd":
                    case "ssridersjad":
                    case "ssridersjac":
                    case "ssridersjbd":
                        YM2151.ym2151_init(3579545);
                        K053260.k053260_start(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        k053260stream = new Streams(111860, 0, 2, K053260::k053260_update);
                        sound_update = Sound::sound_updateKonami68000_ssriders;
                        mixerstream = new Streams(48000, 4, 0, null);
                        break;
                    case "blswhstl":
                    case "blswhstla":
                    case "detatwin":
                        YM2151.ym2151_init(3579545);
                        K053260.k053260_start(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        k053260stream = new Streams(111860, 0, 2, K053260::k053260_update);
                        sound_update = Sound::sound_updateKonami68000_blswhstl;
                        mixerstream = new Streams(48000, 4, 0, null);
                        break;
                    case "glfgreat":
                    case "glfgreatj":
                        K053260.k053260_start(3579545);
                        k053260stream = new Streams(111860, 0, 2, K053260::k053260_update);
                        sound_update = Sound::sound_updateKonami68000_glfgreat;
                        mixerstream = new Streams(48000, 2, 0, null);
                        break;
                    case "tmnt2":
                    case "tmnt2a":
                    case "tmht22pe":
                    case "tmht24pe":
                    case "tmnt22pu":
                    case "qgakumon":
                        YM2151.ym2151_init(3579545);
                        K053260.k053260_start(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        k053260stream = new Streams(111860, 0, 2, K053260::k053260_update);
                        sound_update = Sound::sound_updateKonami68000_tmnt2;
                        mixerstream = new Streams(48000, 4, 0, null);
                        break;
                    case "thndrx2":
                    case "thndrx2a":
                    case "thndrx2j":
                        YM2151.ym2151_init(3579545);
                        K053260.k053260_start(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        k053260stream = new Streams(111860, 0, 2, K053260::k053260_update);
                        sound_update = Sound::sound_updateKonami68000_thndrx2;
                        mixerstream = new Streams(48000, 4, 0, null);
                        break;
                    case "prmrsocr":
                    case "prmrsocrj":
                        latched_value = new short[3];
                        utempdata = new short[3];
                        K054539.k054539_start(48000);
                        k054539stream = new Streams(48000, 0, 2, K054539::k054539_update);
                        sound_update = Sound::sound_updateKonami68000_prmrsocr;
                        mixerstream = new Streams(48000, 2, 0, null);
                        break;
                }
                playtrack = Konami68000::play_konami68000_default;
                stoptrack = Konami68000::stop_konami68000_default;
                stopandplaytrack = Konami68000::stopandplay_konami68000_default;
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                break;
            case "Capcom":
                latched_value = new short[1];
                utempdata = new short[1];
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
                        latched_value = new short[2];
                        utempdata = new short[2];
                        YM2203.ym2203_start(0, 1500000);
                        YM2203.ym2203_start(1, 1500000);
                        sound_update = Sound::sound_updateCapcom_gng;
                        mixerstream = new Streams(48000, 8, 0, null);
                        playtrack = Capcom::play_capcom_default;
                        stoptrack = Capcom::stop_capcom_default;
                        stopandplaytrack = Capcom::stopandplay_capcom_default;
                        break;
                    case "sf":
                    case "sfua":
                    case "sfj":
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        YM2151.ym2151_init(3579545);
                        ym2151stream = new Streams(55930, 0, 2, YM2151::ym2151_update_one);
                        MSM5205.msm5205_start(0, 384000, MSM5205::null_vclk, 7);
                        MSM5205.msm5205_start(1, 384000, MSM5205::null_vclk, 7);
                        sound_update = Sound::sound_updateCapcom_sf;
                        mixerstream = new Streams(48000, 4, 0, null);
                        playtrack = Capcom::play_capcom_default;
                        stoptrack = Capcom::stop_capcom_default;
                        stopandplaytrack = Capcom::stopandplay_capcom_default;
                        break;
                }
                sound_update_timer = Timer.allocCommon(sound_update, "sound_update", false);
                break;
        }
        switch (Machine.sName) {
            case "forgottn":
            case "lostwrld":
            case "strider":
            case "striderua":
            case "dynwar":
            case "dynwarj":
            case "willow":
            case "wofhfh":
                stoptrack = CPS::stop_cps_strider;
                stopandplaytrack = CPS::stopandplay_cps_strider;
                break;
            case "dinohunt":
                playtrack = CPS::play_cps_dinohunt;
                stopandplaytrack = CPS::stopandplay_cps_dinohunt;
                break;
            case "megaman":
            case "sfzch":
                playtrack = CPS::play_cps_megaman;
                stoptrack = CPS::stop_cps_megaman;
                stopandplaytrack = CPS::stopandplay_cps_megaman;
                break;
            case "mslug2":
            case "mslug":
                playtrack = Neogeo::play_neogeo_mslug2;
                stoptrack = Neogeo::stop_neogeo_mslug2;
                stopandplaytrack = Neogeo::stopandplay_neogeo_mslug2;
                break;
            case "mslugx":
                playtrack = Neogeo::play_neogeo_mslugx;
                stoptrack = Neogeo::stop_neogeo_mslugx;
                stopandplaytrack = Neogeo::stopandplay_neogeo_mslugx;
                break;
            case "mslug3":
                playtrack = Neogeo::play_neogeo_mslug3;
                stoptrack = Neogeo::stop_neogeo_mslug3;
                stopandplaytrack = Neogeo::stopandplay_neogeo_mslug3;
                break;
            case "maglord":
            case "bjourney":
            case "trally":
            case "wh1":
                //case "wh2j":
            case "zintrckb":
            case "gururin":
            case "turfmast":
            case "doubledr":
            case "sdodgeb":
            case "tws96":
            case "froman2b":
                playtrack = Neogeo::play_neogeo_maglord;
                stoptrack = Neogeo::stop_neogeo_maglord;
                stopandplaytrack = Neogeo::stopandplay_neogeo_maglord;
                break;
            case "wh2j":
                playtrack = Neogeo::play_neogeo_wh2j;
                stoptrack = Neogeo::stop_neogeo_wh2j;
                stopandplaytrack = Neogeo::stopandplay_neogeo_wh2j;
                break;
            case "wh2":
            case "aodk":
            case "whp":
            case "moshougi":
            case "overtop":
            case "ninjamas":
            case "twinspri":
                playtrack = Neogeo::play_neogeo_aodk;
                stoptrack = Neogeo::stop_neogeo_aodk;
                stopandplaytrack = Neogeo::stopandplay_neogeo_aodk;
                break;
            case "wjammers":
            case "karnovr":
            case "strhoop":
            case "ghostlop":
            case "magdrop2":
                playtrack = Neogeo::play_neogeo_wjammers;
                stoptrack = Neogeo::stop_neogeo_wjammers;
                stopandplaytrack = Neogeo::stopandplay_neogeo_wjammers;
                break;
            case "pbobbl2n":
                playtrack = Neogeo::play_neogeo_pbobbl2n;
                stoptrack = Neogeo::stop_neogeo_pbobbl2n;
                stopandplaytrack = Neogeo::stopandplay_neogeo_pbobbl2n;
                break;
//            case "lasthope":
//                playtrack = Neogeo.play_neogeo_lasthope;
//                stoptrack = Neogeo.stop_neogeo_lasthope;
//                stopandplaytrack = Neogeo.stopandplay_neogeo_lasthope;
//                break;
        }
        stoptrack.accept(TrackInfo.StopTrack);
        Timer.adjustPeriodic(sound_update_timer, update_frequency, update_frequency);
    }

    public static void sound_reset() {
        switch (Machine.sBoard) {
            case "CPS-1":
                YM2151.ym2151_reset_chip();
                OKI6295.okim6295_reset();
                break;
            case "CPS-1(QSound)":
            case "CPS2":
                break;
            case "Data East":
                YM2203.FF2203[0].ym2203_reset_chip();
                FMOpl.ym3812_reset_chip();
                break;
            case "Tehkan":
                AY8910.AA8910[0].ay8910_reset_ym();
                AY8910.AA8910[1].ay8910_reset_ym();
                AY8910.AA8910[2].ay8910_reset_ym();
                break;
            case "Neo Geo":
                YM2610.F2610.ym2610_reset_chip();
                break;
            case "SunA8":
                FMOpl.ym3812_reset_chip();
                AY8910.AA8910[0].ay8910_reset_ym();
                break;
            case "Namco System 1":
                YM2151.ym2151_reset_chip();
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
                        OKI6295.okim6295_reset();
                        FMOpl.ym3812_reset_chip();
                        break;
                    case "lhb":
                    case "lhbv33c":
                    case "dbc":
                    case "ryukobou":
                    case "xymg":
                    case "wlcc":
                        OKI6295.okim6295_reset();
                        break;
                    case "lhb2":
                    case "nkishusp":
                        OKI6295.okim6295_reset();
                        YM2413.ym2413_reset_chip();
                        break;
                    case "vbowl":
                    case "vbowlj":
                        ICS2115.ics2115_reset();
                        break;
                }
                break;
            case "PGM":
                ICS2115.ics2115_reset();
                break;
            case "M72":
                YM2151.ym2151_reset_chip();
                break;
            case "M92":
                YM2151.ym2151_reset_chip();
                break;
            case "Taito":
                switch (Machine.sName) {
                    case "tokio":
                    case "tokioo":
                    case "tokiou":
                    case "tokiob":
                        YM2203.FF2203[0].ym2203_reset_chip();
                        break;
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
                        YM2203.FF2203[0].ym2203_reset_chip();
                        FMOpl.ym3526_reset_chip();
                        break;
                    case "opwolf":
                    case "opwolfa":
                    case "opwolfj":
                    case "opwolfu":
                    case "opwolfb":
                    case "opwolfp":
                        YM2151.ym2151_reset_chip();
                        break;
                }
                break;
            case "Taito B":
                YM2610.F2610.ym2610_reset_chip();
                break;
            case "Konami 68000":
                switch (Machine.sName) {
                    case "cuebrick":
                    case "mia":
                    case "mia2":
                        YM2151.ym2151_reset_chip();
                        break;
                    case "tmnt":
                    case "tmntu":
                    case "tmntua":
                    case "tmntub":
                    case "tmht":
                    case "tmhta":
                    case "tmhtb":
                    case "tmntj":
                    case "tmnta":
                    case "tmht2p":
                    case "tmht2pa":
                    case "tmnt2pj":
                    case "tmnt2po":
                        YM2151.ym2151_reset_chip();
                        Upd7759.upd7759_reset();
                        break;
                    case "punkshot":
                    case "punkshot2":
                    case "punkshotj":
                    case "lgtnfght":
                    case "lgtnfghta":
                    case "lgtnfghtu":
                    case "trigon":
                    case "blswhstl":
                    case "blswhstla":
                    case "detatwin":
                    case "tmnt2":
                    case "tmnt2a":
                    case "tmht22pe":
                    case "tmht24pe":
                    case "tmnt22pu":
                    case "qgakumon":
                    case "ssriders":
                    case "ssriderseaa":
                    case "ssridersebd":
                    case "ssridersebc":
                    case "ssridersuda":
                    case "ssridersuac":
                    case "ssridersuab":
                    case "ssridersubc":
                    case "ssridersadd":
                    case "ssridersabd":
                    case "ssridersjad":
                    case "ssridersjac":
                    case "ssridersjbd":
                    case "thndrx2":
                    case "thndrx2a":
                    case "thndrx2j":
                        YM2151.ym2151_reset_chip();
                        K053260.k053260_reset();
                        break;
                    case "glfgreat":
                    case "glfgreatj":
                        K053260.k053260_reset();
                        break;
                    case "prmrsocr":
                    case "prmrsocrj":
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
                        YM2203.FF2203[0].ym2203_reset_chip();
                        YM2203.FF2203[1].ym2203_reset_chip();
                        break;
                    case "sf":
                    case "sfua":
                    case "sfj":
                    case "sfjan":
                    case "sfan":
                    case "sfp":
                        YM2151.ym2151_reset_chip();
                        break;
                }
                break;
        }
    }

    public static void sound_pause(boolean pause) {
try {
        if (pause) {
            sound_muted |= 0x02;
            if (Sound.buf2 != null && Sound.buf2.isOpen())
                volume(Sound.buf2, 0);
        } else {
            sound_muted &= ~0x02;
            if (Sound.buf2 != null && Sound.buf2.isOpen())
                volume(Sound.buf2, 1);
        }
        //osd_set_mastervolume(sound_muted ? -32 : 0);
} catch (Exception e) {
 logger.log(Level.INFO, e.getMessage(), e);
}
    }

    public static void sound_updateC() {
        boolean bSound = false;
        int sampindex;
        byte[] ym2151b = new byte[0xf00];
        ym2151stream.stream_update();
        okistream.stream_update();
        generate_resampled_dataY5(0x59);
        generate_resampled_dataO(0x4c, 2);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
            if (bSound == false && samp != 0) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateC();
    }

    public static void sound_updateQ() {
        boolean bSound = false;
        int sampindex;
        qsoundstream.stream_update();
        generate_resampled_dataQ();
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateQ();
    }

    public static void sound_updateDataeast_pcktgal() {
        boolean bSound = false;
        int sampindex, sampmin = 32767, sampmax = -32768;
        AY8910.AA8910[0].stream.stream_update();
        YM2203.FF2203[0].stream.stream_update();
        ym3812stream.stream_update();
        MSM5205.mm1[0].voice.stream.stream_update();
        generate_resampled_dataA3(0, 0x99, 0);
        generate_resampled_dataYM2203(0, 0x99, 3);
        generate_resampled_dataYM3812(0x100, 4);
        generate_resampled_dataMSM5205_0(0xb3, 5);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex] + mixerstream.streaminput[5][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            if (samp < sampmin) {
                sampmin = samp;
            }
            if (samp > sampmax) {
                sampmax = samp;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
            if (bSound == false && sampmax - sampmin > 0x17) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateDataeast_pcktgal();
    }

    public static void sound_updateTehkan_pbaction() {
        boolean bSound = false;
        int sampindex, sampmin = 32767, sampmax = -32768;
        AY8910.AA8910[0].stream.stream_update();
        AY8910.AA8910[1].stream.stream_update();
        AY8910.AA8910[2].stream.stream_update();
        generate_resampled_dataA3(0, 0x40, 0);
        generate_resampled_dataA3(1, 0x40, 3);
        generate_resampled_dataA3(2, 0x40, 6);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex] + mixerstream.streaminput[5][sampindex] + mixerstream.streaminput[6][sampindex] + mixerstream.streaminput[7][sampindex] + mixerstream.streaminput[8][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            if (samp < sampmin) {
                sampmin = samp;
            }
            if (samp > sampmax) {
                sampmax = samp;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
            if (bSound == false && sampmax - sampmin > 0) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateTehkan();
    }

    public static void sound_updateN() {
        boolean bSound = false;
        int sampindex;
        AY8910.AA8910[0].stream.stream_update();
        ym2610stream.stream_update();
        generate_resampled_dataA_neogeo();
        generate_resampled_dataY6();
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && ((sampL != 0 && sampL != 0x80) || (sampR != 0) && sampR != 0x80)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateN();
    }

    public static void sound_updateNa() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        namcostream.stream_update();
        dacstream.stream_update();
        generate_resampled_dataY5(0x80);
        generate_resampled_dataNa();
        generate_resampled_dataDac(0x100, 4);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[4][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateNa();
    }

    public static void sound_updateM72() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        dacstream.stream_update();
        generate_resampled_dataY5(0x100);
        generate_resampled_dataDac(0x66, 2);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && ((sampL != 0 && sampL != 0x33) || (sampR != 0 && sampR != 0x33))) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateM72();
    }

    public static void sound_updateM92() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        iremga20stream.stream_update();
        generate_resampled_dataY5(0x66);
        generate_resampled_dataIremga20(0x100);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && ((sampL != 0) || (sampR != 0))) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        streams_updateM92();
    }

    public static void sound_updateTaito_tokio() {
        boolean bSound = false;
        int sampindex, sampmin = 32767, sampmax = -32768;
        AY8910.AA8910[0].stream.stream_update();
        YM2203.FF2203[0].stream.stream_update();
        generate_resampled_dataA3(0, 0x14, 0);
        generate_resampled_dataYM2203(0, 0x100, 3);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            if (samp < sampmin) {
                sampmin = samp;
            }
            if (samp > sampmax) {
                sampmax = samp;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
        }
        if (bSound == false && sampmax - sampmin > 20) {
            bSound = true;
            nZero = 0;
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateTaito_tokio();
    }

    public static void sound_updateTaito_bublbobl() {
        boolean bSound = false;
        int sampindex, sampmin = 32767, sampmax = -32768;
        AY8910.AA8910[0].stream.stream_update();
        YM2203.FF2203[0].stream.stream_update();
        ym3526stream.stream_update();
        generate_resampled_dataA3(0, 0x40, 0);
        generate_resampled_dataYM2203(0, 0x40, 3);
        generate_resampled_dataYM3526(0x80, 4);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            if (samp < sampmin) {
                sampmin = samp;
            }
            if (samp > sampmax) {
                sampmax = samp;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
        }
        if (!bSound && sampmax - sampmin > 30) {
            bSound = true;
            nZero = 0;
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateTaito_bublbobl();
    }

    public static void sound_updateTaitoB() {
        boolean bSound = false;
        int sampindex, sampmin = 32767, sampmax = -32768;
        AY8910.AA8910[0].stream.stream_update();
        ym2610stream.stream_update();
        generate_resampled_dataA_taitob();
        generate_resampled_dataY6();
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            if (samp < sampmin) {
                sampmin = samp;
            }
            if (samp > sampmax) {
                sampmax = samp;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
        }
        if (bSound == false && sampmax - sampmin > 30) {
            bSound = true;
            nZero = 0;
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateN();
    }

    public static void sound_updateKonami68000_cuebrick() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        generate_resampled_dataY5(0x100);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_cuebrick();
    }

    public static void sound_updateKonami68000_mia() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        k007232stream.stream_update();
        generate_resampled_dataY5(0x100);
        generate_resampled_dataK007232(0x33);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_mia();
    }

    public static void sound_updateKonami68000_tmnt() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        k007232stream.stream_update();
        upd7759stream.stream_update();
        samplestream.stream_update();
        generate_resampled_dataY5(0x100);
        generate_resampled_dataK007232(0x33);
        generate_resampled_dataUpd7759(0x99);
        generate_resampled_dataSample(0x100, 5);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex] + mixerstream.streaminput[5][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex] + mixerstream.streaminput[5][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_tmnt();
    }

    public static void sound_updateKonami68000_blswhstl() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        k053260stream.stream_update();
        generate_resampled_dataY5(0xb3);
        generate_resampled_dataK053260(0x80, 2, 3);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (!bSound && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_ssriders();
    }

    public static void sound_updateKonami68000_glfgreat() {
        boolean bSound = false;
        int sampindex;
        k053260stream.stream_update();
        generate_resampled_dataK053260(0x100, 0, 1);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (bSound == false && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_glfgreat();
    }

    public static void sound_updateKonami68000_tmnt2() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        k053260stream.stream_update();
        generate_resampled_dataY5(0x100);
        generate_resampled_dataK053260(0xc0, 2, 3);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (!bSound && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_ssriders();
    }

    public static void sound_updateKonami68000_ssriders() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        k053260stream.stream_update();
        generate_resampled_dataY5(0x100);
        generate_resampled_dataK053260(0xb3, 2, 3);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (!bSound && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_ssriders();
    }

    public static void sound_updateKonami68000_thndrx2() {
        boolean bSound = false;
        int sampindex;
        ym2151stream.stream_update();
        k053260stream.stream_update();
        generate_resampled_dataY5(0x100);
        generate_resampled_dataK053260(0xc0, 2, 3);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (!bSound && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_ssriders();
    }

    public static void sound_updateKonami68000_prmrsocr() {
        boolean bSound = false;
        int sampindex;
        k054539stream.stream_update();
        generate_resampled_dataK054539(0x100);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            sampR = mixerstream.streaminput[1][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
            if (!bSound && (sampL != 0 || sampR != 0)) {
                bSound = true;
                nZero = 0;
            }
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateKonami68000_prmrsocr();
    }

    public static void sound_updateCapcom_gng() {
        boolean bSound = false;
        int sampindex, sampmin = 32767, sampmax = -32768;
        AY8910.AA8910[0].stream.stream_update();
        AY8910.AA8910[1].stream.stream_update();
        YM2203.FF2203[0].stream.stream_update();
        YM2203.FF2203[1].stream.stream_update();
        generate_resampled_dataA3(0, 0x66, 0);
        generate_resampled_dataYM2203(0, 0x33, 3);
        generate_resampled_dataA3(1, 0x66, 4);
        generate_resampled_dataYM2203(1, 0x33, 7);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int samp;
            samp = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex] + mixerstream.streaminput[4][sampindex] + mixerstream.streaminput[5][sampindex] + mixerstream.streaminput[6][sampindex] + mixerstream.streaminput[7][sampindex];
            if (samp < -32768) {
                samp = -32768;
            } else if (samp > 32767) {
                samp = 32767;
            }
            if (samp < sampmin) {
                sampmin = samp;
            }
            if (samp > sampmax) {
                sampmax = samp;
            }
            finalmixb[sampindex * 4] = (byte) samp;
            finalmixb[sampindex * 4 + 1] = (byte) ((samp & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) samp;
            finalmixb[sampindex * 4 + 3] = (byte) ((samp & 0xff00) >> 8);
            if (!bSound && sampmax - sampmin > 0) {
                bSound = true;
                nZero = 0;
            }
        }
        if (!bSound) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateCapcom_gng();
    }

    public static void sound_updateCapcom_sf() {
        boolean bSound = false;
        int sampindex, sampLmin = 32767, sampLmax = -32768, sampRmin = 32767, sampRmax = -32768;
        ym2151stream.stream_update();
        MSM5205.mm1[0].voice.stream.stream_update();
        MSM5205.mm1[1].voice.stream.stream_update();
        generate_resampled_dataY5(0x99);
        generate_resampled_dataMSM5205_0(0x100, 2);
        generate_resampled_dataMSM5205_1(0x100, 3);
        mixerstream.output_sampindex += 0x3c0;
        for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
            int sampL, sampR;
            sampL = mixerstream.streaminput[0][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampL < -32768) {
                sampL = -32768;
            } else if (sampL > 32767) {
                sampL = 32767;
            }
            if (sampL < sampLmin) {
                sampLmin = sampL;
            }
            if (sampL > sampLmax) {
                sampLmax = sampL;
            }
            sampR = mixerstream.streaminput[1][sampindex] + mixerstream.streaminput[2][sampindex] + mixerstream.streaminput[3][sampindex];
            if (sampR < -32768) {
                sampR = -32768;
            } else if (sampR > 32767) {
                sampR = 32767;
            }
            if (sampR < sampRmin) {
                sampRmin = sampR;
            }
            if (sampR > sampRmax) {
                sampRmax = sampR;
            }
            finalmixb[sampindex * 4] = (byte) sampL;
            finalmixb[sampindex * 4 + 1] = (byte) ((sampL & 0xff00) >> 8);
            finalmixb[sampindex * 4 + 2] = (byte) sampR;
            finalmixb[sampindex * 4 + 3] = (byte) ((sampR & 0xff00) >> 8);
        }
        if (bSound == false && ((sampLmax - sampLmin > 0) || (sampRmax - sampRmin > 0))) {
            bSound = true;
            nZero = 0;
        }
        if (bSound == false) {
            nZero++;
        }
        osd_update_audio_stream(finalmixb, 0x3c0);
        if (WavWrite.waveFile != null) {
            WavWrite.addData16(finalmixb, 0xf00);
        }
        streams_updateCapcom_sf();
    }

    public static void latch_callback() {
        latched_value[0] = utempdata[0];
    }

    public static void latch_callback2() {
        latched_value[1] = utempdata[1];
    }

    public static void latch_callback3() {
        latched_value[2] = utempdata[2];
    }

    public static void latch_callback4() {
        latched_value[3] = utempdata[3];
    }

    public static short latch_r(int which) {
        return latched_value[which];
    }

    public static void soundlatch_w(short data) {
        utempdata[0] = data;
        Timer.setInternal(Sound::latch_callback, "latch_callback");
    }

    public static void soundlatch2_w(short data) {
        utempdata[1] = data;
        Timer.setInternal(Sound::latch_callback2, "latch_callback2");
    }

    public static void soundlatch3_w(short data) {
        utempdata[2] = data;
        Timer.setInternal(Sound::latch_callback3, "latch_callback3");
    }

    public static void soundlatch4_w(short data) {
        utempdata[3] = data;
        Timer.setInternal(Sound::latch_callback4, "latch_callback4");
    }

    public static short soundlatch_r() {
        return latched_value[0];
    }

    public static short soundlatch2_r() {
        return latched_value[1];
    }

    public static short soundlatch3_r() {
        return latched_value[2];
    }

    public static short soundlatch4_r() {
        return latched_value[3];
    }

    private static void osd_update_audio_stream(byte[] buffer, int samples_this_frame) {
logger.log(Level.INFO, "sound: " + samples_this_frame + "\n" + StringUtil.getDump(buffer, 64));
        if (!buf2.isOpen()) return;
        int l = 0;
        while (l < samples_this_frame) {
            l += buf2.write(buffer, l, samples_this_frame - l);
            stream_buffer_in = 0;
        }
    }

    private static void osd_update_audio_stream_OFF(byte[] buffer, int samples_this_frame) {
        int play_position = 0, write_position = 0;
        int stream_in;
        byte[] buffer1, buffer2;
        int length1, length2;
//        buf2.GetCurrentPosition(/* out */ play_position, /* out */ write_position);
        if (write_position < play_position) {
            write_position += 0x9400;
        }
        stream_in = stream_buffer_in;
        if (stream_in < write_position) {
            stream_in += 0x9400;
        }
        while (stream_in < write_position) {
            //buffer_underflows++;
            stream_in += 0xf00;
        }
        if (stream_in + 0xf00 > play_position + 0x9400) {
            //buffer_overflows++;
            return;
        }
logger.log(Level.INFO, "sound\n" + StringUtil.getDump(buffer, 64));
        stream_buffer_in = stream_in % 0x9400;
        if (stream_buffer_in + 0xf00 < 0x9400) {
            length1 = 0xf00;
            length2 = 0;
            buffer1 = new byte[length1];
            System.arraycopy(buffer, 0, buffer1, 0, length1);
            buf2.write(buffer1, stream_buffer_in, buffer1.length - stream_buffer_in);
            stream_buffer_in = stream_buffer_in + 0xf00;
        } else if (stream_buffer_in + 0xf00 == 0x9400) {
            length1 = 0xf00;
            length2 = 0;
            buffer1 = new byte[length1];
            System.arraycopy(buffer, 0, buffer1, 0, length1);
            buf2.write(buffer1, stream_buffer_in, buffer1.length - stream_buffer_in);
            stream_buffer_in = 0;
        } else if (stream_buffer_in + 0xf00 > 0x9400) {
            length1 = 0x9400 - stream_buffer_in;
            length2 = 0xf00 - length1;
            buffer1 = new byte[length1];
            buffer2 = new byte[length2];
            System.arraycopy(buffer, 0, buffer1, 0, length1);
            System.arraycopy(buffer, length1, buffer2, 0, length2);
            buf2.write(buffer1, stream_buffer_in, buffer1.length - stream_buffer_in);
            buf2.write(buffer2, 0, buffer2.length);
            stream_buffer_in = length2;
        }
    }

//#region sound_stream

    public static int last_update_second;
    public static Streams ym2151stream, okistream, mixerstream;
    public static Streams qsoundstream;
    public static Streams ym2610stream;
    public static Streams namcostream, dacstream;
    public static Streams ics2115stream;
    public static Streams ym3812stream, ym3526stream, ym2413stream;
    public static Streams iremga20stream;
    public static Streams k053260stream;
    public static Streams upd7759stream;
    public static Streams k007232stream;
    public static Streams samplestream;
    public static Streams k054539stream;
    public static final long update_attoseconds = Attotime.ATTOSECONDS_PER_SECOND / 50;

    private static void generate_resampled_dataY5(int gain) {
        int offset;
        int sample0, sample1;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / ym2151stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / ym2151stream.attoseconds_per_sample) - 1);
        offset = basesample - ym2151stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * ym2151stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) ym2151stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample0 = ym2151stream.streamoutput[0][offset + tpos] * scale;
                sample1 = ym2151stream.streamoutput[1][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += ym2151stream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += ym2151stream.streamoutput[1][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += ym2151stream.streamoutput[0][offset + tpos] * remainder;
                sample1 += ym2151stream.streamoutput[1][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                mixerstream.streaminput[0][sampindex] = (sample0 * gain) >> 8;
                mixerstream.streaminput[1][sampindex] = (sample1 * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataO(int gain, int minput) {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - okistream.attoseconds_per_sample * 2;
        if (basetime >= 0)
            basesample = (int) (basetime / okistream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / okistream.attoseconds_per_sample) - 1);
        offset = basesample - okistream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * okistream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) okistream.sample_rate << 22) / 48000);
        if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                sample = (okistream.streamoutput[0][offset] * (0x1000 - interp_frac) + okistream.streamoutput[0][offset + 1] * interp_frac) >> 12;
                mixerstream.streaminput[minput][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataQ() {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - qsoundstream.attoseconds_per_sample * 2;
        if (basetime >= 0)
            basesample = (int) (basetime / qsoundstream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / qsoundstream.attoseconds_per_sample) - 1);
        offset = basesample - qsoundstream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * qsoundstream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) qsoundstream.sample_rate << 22) / 48000);
        if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                mixerstream.streaminput[0][sampindex] = (qsoundstream.streamoutput[0][offset] * (0x1000 - interp_frac) + qsoundstream.streamoutput[0][offset + 1] * interp_frac) >> 12;
                mixerstream.streaminput[1][sampindex] = (qsoundstream.streamoutput[1][offset] * (0x1000 - interp_frac) + qsoundstream.streamoutput[1][offset + 1] * interp_frac) >> 12;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataA_neogeo() {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / AY8910.AA8910[0].stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / AY8910.AA8910[0].stream.attoseconds_per_sample) - 1);
        offset = basesample - AY8910.AA8910[0].stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * AY8910.AA8910[0].stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) AY8910.AA8910[0].stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample = AY8910.AA8910[0].stream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += AY8910.AA8910[0].stream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += AY8910.AA8910[0].stream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[0][sampindex] = (sample * 0x99) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataA3(int chip, int gain, int start) {
        int offset;
        int sample0, sample1, sample2;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / AY8910.AA8910[chip].stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / AY8910.AA8910[chip].stream.attoseconds_per_sample) - 1);
        offset = basesample - AY8910.AA8910[chip].stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * AY8910.AA8910[chip].stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) AY8910.AA8910[chip].stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample0 = AY8910.AA8910[chip].stream.streamoutput[0][offset + tpos] * scale;
                sample1 = AY8910.AA8910[chip].stream.streamoutput[1][offset + tpos] * scale;
                sample2 = AY8910.AA8910[chip].stream.streamoutput[2][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += AY8910.AA8910[chip].stream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += AY8910.AA8910[chip].stream.streamoutput[1][offset + tpos] * 0x100;
                    sample2 += AY8910.AA8910[chip].stream.streamoutput[2][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += AY8910.AA8910[chip].stream.streamoutput[0][offset + tpos] * remainder;
                sample1 += AY8910.AA8910[chip].stream.streamoutput[1][offset + tpos] * remainder;
                sample2 += AY8910.AA8910[chip].stream.streamoutput[2][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                sample2 /= smallstep;
                mixerstream.streaminput[start][sampindex] = (sample0 * gain) >> 8;
                mixerstream.streaminput[start + 1][sampindex] = (sample1 * gain) >> 8;
                mixerstream.streaminput[start + 2][sampindex] = (sample2 * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataA_taitob() {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int gain;
        int sampindex;
        gain = (0x40 * AY8910.AA8910[0].stream.gain) >> 8;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / AY8910.AA8910[0].stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / AY8910.AA8910[0].stream.attoseconds_per_sample) - 1);
        offset = basesample - AY8910.AA8910[0].stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * AY8910.AA8910[0].stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) AY8910.AA8910[0].stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample = AY8910.AA8910[0].stream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += AY8910.AA8910[0].stream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += AY8910.AA8910[0].stream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[0][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataYM2203(int c, int gain, int minput) {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - YM2203.FF2203[c].stream.attoseconds_per_sample * 2;
        if (basetime >= 0)
            basesample = (int) (basetime / YM2203.FF2203[c].stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / YM2203.FF2203[c].stream.attoseconds_per_sample) - 1);
        offset = basesample - YM2203.FF2203[c].stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * YM2203.FF2203[c].stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) YM2203.FF2203[c].stream.sample_rate << 22) / 48000);
        if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                int i2 = YM2203.FF2203[c].stream.streamoutput[0][offset];
                int i3 = YM2203.FF2203[c].stream.streamoutput[0][offset + 1];
                int i4 = (((YM2203.FF2203[c].stream.streamoutput[0][offset] * (0x1000 - interp_frac) + YM2203.FF2203[c].stream.streamoutput[0][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                mixerstream.streaminput[minput][sampindex] = (((YM2203.FF2203[c].stream.streamoutput[0][offset] * (0x1000 - interp_frac) + YM2203.FF2203[c].stream.streamoutput[0][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataYM3526(int gain, int minput) {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / ym3526stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / ym3526stream.attoseconds_per_sample) - 1);
        offset = basesample - ym3526stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * ym3526stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) ym3526stream.sample_rate << 22) / 48000);
        if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                mixerstream.streaminput[minput][sampindex] = (((ym3526stream.streamoutput[0][offset] * (0x1000 - interp_frac) + ym3526stream.streamoutput[0][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataY6() {
        int offset;
        int sample0, sample1;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / ym2610stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / ym2610stream.attoseconds_per_sample) - 1);
        offset = basesample - ym2610stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * ym2610stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) ym2610stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample0 = ym2610stream.streamoutput[0][offset + tpos] * scale;
                sample1 = ym2610stream.streamoutput[1][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += ym2610stream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += ym2610stream.streamoutput[1][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += ym2610stream.streamoutput[0][offset + tpos] * remainder;
                sample1 += ym2610stream.streamoutput[1][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                mixerstream.streaminput[1][sampindex] = sample0;
                mixerstream.streaminput[2][sampindex] = sample1;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataNa() {
        int offset;
        int sample0, sample1;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int gain;
        int sampindex;
        gain = 0x80;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / namcostream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / namcostream.attoseconds_per_sample) - 1);
        offset = basesample - namcostream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * namcostream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) namcostream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> (14);
                sample0 = namcostream.streamoutput[0][offset + tpos] * scale;
                sample1 = namcostream.streamoutput[1][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += namcostream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += namcostream.streamoutput[1][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += namcostream.streamoutput[0][offset + tpos] * remainder;
                sample1 += namcostream.streamoutput[1][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                mixerstream.streaminput[2][sampindex] = (sample0 * gain) >> 8;
                mixerstream.streaminput[3][sampindex] = (sample1 * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataDac(int gain, int minput) {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / dacstream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / dacstream.attoseconds_per_sample) - 1);
        offset = basesample - dacstream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * dacstream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) dacstream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> (14);
                sample = dacstream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += dacstream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += dacstream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[minput][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataYM2413(int gain, int minput) {
        int offset;
        int sample0, sample1;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / ym2413stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / ym2413stream.attoseconds_per_sample) - 1);
        offset = basesample - ym2413stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * ym2413stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) ym2413stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample0 = ym2413stream.streamoutput[0][offset + tpos] * scale;
                sample1 = ym2413stream.streamoutput[1][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += ym2413stream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += ym2413stream.streamoutput[1][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += ym2413stream.streamoutput[0][offset + tpos] * remainder;
                sample1 += ym2413stream.streamoutput[1][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                mixerstream.streaminput[minput][sampindex] = (sample0 * gain) >> 8;
                mixerstream.streaminput[minput + 1][sampindex] = (sample1 * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataYM3812(int gain, int minput) {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / ym3812stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / ym3812stream.attoseconds_per_sample) - 1);
        offset = basesample - ym3812stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * ym3812stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) ym3812stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample = ym3812stream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += ym3812stream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += ym3812stream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[minput][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        } else if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                mixerstream.streaminput[minput][sampindex] = (((ym3812stream.streamoutput[0][offset] * (0x1000 - interp_frac) + ym3812stream.streamoutput[0][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    private static void generate_resampled_dataIcs2115(int gain) {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - ics2115stream.attoseconds_per_sample * 2;
        if (basetime >= 0)
            basesample = (int) (basetime / ics2115stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / ics2115stream.attoseconds_per_sample) - 1);
        offset = basesample - ics2115stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * ics2115stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) ics2115stream.sample_rate << 22) / 48000);
        if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                mixerstream.streaminput[0][sampindex] = (((ics2115stream.streamoutput[0][offset] * (0x1000 - interp_frac) + ics2115stream.streamoutput[0][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                mixerstream.streaminput[1][sampindex] = (((ics2115stream.streamoutput[1][offset] * (0x1000 - interp_frac) + ics2115stream.streamoutput[1][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void generate_resampled_dataIremga20(int gain) {
        int offset;
        int sample0, sample1;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / iremga20stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / iremga20stream.attoseconds_per_sample) - 1);
        offset = basesample - iremga20stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * iremga20stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) iremga20stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample0 = iremga20stream.streamoutput[0][offset + tpos] * scale;
                sample1 = iremga20stream.streamoutput[1][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += iremga20stream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += iremga20stream.streamoutput[1][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += iremga20stream.streamoutput[0][offset + tpos] * remainder;
                sample1 += iremga20stream.streamoutput[1][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                mixerstream.streaminput[2][sampindex] = (sample0 * gain) >> 8;
                mixerstream.streaminput[3][sampindex] = (sample1 * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void generate_resampled_dataK053260(int gain, int minput1, int minput2) {
        int offset;
        int sample0, sample1;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / k053260stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / k053260stream.attoseconds_per_sample) - 1);
        offset = basesample - k053260stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * k053260stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) k053260stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample0 = k053260stream.streamoutput[0][offset + tpos] * scale;
                sample1 = k053260stream.streamoutput[1][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample0 += k053260stream.streamoutput[0][offset + tpos] * 0x100;
                    sample1 += k053260stream.streamoutput[1][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample0 += k053260stream.streamoutput[0][offset + tpos] * remainder;
                sample1 += k053260stream.streamoutput[1][offset + tpos] * remainder;
                sample0 /= smallstep;
                sample1 /= smallstep;
                mixerstream.streaminput[minput1][sampindex] = (sample0 * gain) >> 8;
                mixerstream.streaminput[minput2][sampindex] = (sample1 * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void generate_resampled_dataK007232(int gain) {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - k007232stream.attoseconds_per_sample * 2;
        if (basetime >= 0)
            basesample = (int) (basetime / k007232stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / k007232stream.attoseconds_per_sample) - 1);
        offset = basesample - k007232stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * k007232stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) k007232stream.sample_rate << 22) / 48000);
        if (step < 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int interp_frac = basefrac >> 10;
                mixerstream.streaminput[2][sampindex] = (((k007232stream.streamoutput[0][offset] * (0x1000 - interp_frac) + k007232stream.streamoutput[0][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                mixerstream.streaminput[3][sampindex] = (((k007232stream.streamoutput[1][offset] * (0x1000 - interp_frac) + k007232stream.streamoutput[1][offset + 1] * interp_frac) >> 12) * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void generate_resampled_dataUpd7759(int gain) {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / upd7759stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / upd7759stream.attoseconds_per_sample) - 1);
        offset = basesample - upd7759stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * upd7759stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) upd7759stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample = upd7759stream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += upd7759stream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += upd7759stream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[4][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void generate_resampled_dataSample(int gain, int minput) {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / samplestream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / samplestream.attoseconds_per_sample) - 1);
        offset = basesample - samplestream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * samplestream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) samplestream.sample_rate << 22) / 48000);
        if (step == 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                mixerstream.streaminput[minput][sampindex] = (samplestream.streamoutput[0][offset + sampindex] * gain) >> 8;
            }
        }
    }

    public static void generate_resampled_dataK054539(int gain) {
        int offset;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / k054539stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / k054539stream.attoseconds_per_sample) - 1);
        offset = basesample - k054539stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * k054539stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) k054539stream.sample_rate << 22) / 48000);
        if (step == 0x40_0000) {
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                mixerstream.streaminput[0][sampindex] = (k054539stream.streamoutput[0][offset + sampindex] * gain) >> 8;
                mixerstream.streaminput[1][sampindex] = (k054539stream.streamoutput[1][offset + sampindex] * gain) >> 8;
            }
        }
    }

    public static void generate_resampled_dataMSM5205_0(int gain, int minput) {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / MSM5205.mm1[0].voice.stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / MSM5205.mm1[0].voice.stream.attoseconds_per_sample) - 1);
        offset = basesample - MSM5205.mm1[0].voice.stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * MSM5205.mm1[0].voice.stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) MSM5205.mm1[0].voice.stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample = MSM5205.mm1[0].voice.stream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += MSM5205.mm1[0].voice.stream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += MSM5205.mm1[0].voice.stream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[minput][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void generate_resampled_dataMSM5205_1(int gain, int minput) {
        int offset;
        int sample;
        long basetime;
        int basesample;
        int basefrac;
        int step;
        int sampindex;
        basetime = mixerstream.output_sampindex * mixerstream.attoseconds_per_sample - mixerstream.attoseconds_per_sample;
        if (basetime >= 0)
            basesample = (int) (basetime / MSM5205.mm1[1].voice.stream.attoseconds_per_sample);
        else
            basesample = (int) (-(-basetime / MSM5205.mm1[1].voice.stream.attoseconds_per_sample) - 1);
        offset = basesample - MSM5205.mm1[1].voice.stream.output_base_sampindex;
        basefrac = (int) ((basetime - basesample * MSM5205.mm1[1].voice.stream.attoseconds_per_sample) / (Attotime.ATTOSECONDS_PER_SECOND >> 22));
        step = (int) (((long) MSM5205.mm1[1].voice.stream.sample_rate << 22) / 48000);
        if (step > 0x40_0000) {
            int smallstep = step >> 14;
            for (sampindex = 0; sampindex < 0x3c0; sampindex++) {
                int remainder = smallstep;
                int tpos = 0;
                int scale;
                scale = (0x40_0000 - basefrac) >> 14;
                sample = MSM5205.mm1[1].voice.stream.streamoutput[0][offset + tpos] * scale;
                tpos++;
                remainder -= scale;
                while (remainder > 0x100) {
                    sample += MSM5205.mm1[1].voice.stream.streamoutput[0][offset + tpos] * 0x100;
                    tpos++;
                    remainder -= 0x100;
                }
                sample += MSM5205.mm1[1].voice.stream.streamoutput[0][offset + tpos] * remainder;
                sample /= smallstep;
                mixerstream.streaminput[minput][sampindex] = (sample * gain) >> 8;
                basefrac += step;
                offset += basefrac >> 22;
                basefrac &= 0x3f_ffff;
            }
        }
    }

    public static void streams_updateC() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        okistream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateQ() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        qsoundstream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateDataeast_pcktgal() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        YM2203.FF2203[0].stream.adjuststream(second_tick);
        ym3812stream.adjuststream(second_tick);
        MSM5205.mm1[0].voice.stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
        AY8910.AA8910[0].stream.updatesamplerate();
    }

    private static void streams_updateTehkan() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        AY8910.AA8910[1].stream.adjuststream(second_tick);
        AY8910.AA8910[2].stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
        AY8910.AA8910[0].stream.updatesamplerate();
        AY8910.AA8910[1].stream.updatesamplerate();
        AY8910.AA8910[2].stream.updatesamplerate();
    }

    private static void streams_updateN() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        ym2610stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
        AY8910.AA8910[0].stream.updatesamplerate();
    }

    private static void streams_updateSunA8() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym3812stream.adjuststream(second_tick);
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        samplestream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
        //AY8910.AA8910[0].stream.updatesamplerate();
    }

    private static void streams_updateNa() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        namcostream.adjuststream(second_tick);
        dacstream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateIGS011_drgnwrld() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        okistream.adjuststream(second_tick);
        ym3812stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateIGS011_lhb() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        okistream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateIGS011_lhb2() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        okistream.adjuststream(second_tick);
        ym2413stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateIGS011_vbowl() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ics2115stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updatePGM() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ics2115stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateM72() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        dacstream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateM92() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        iremga20stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateTaito_tokio() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        YM2203.FF2203[0].stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateTaito_bublbobl() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        YM2203.FF2203[0].stream.adjuststream(second_tick);
        ym3526stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
        AY8910.AA8910[0].stream.updatesamplerate();
    }

    private static void streams_updateKonami68000_cuebrick() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateKonami68000_mia() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        k007232stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateKonami68000_tmnt() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        k007232stream.adjuststream(second_tick);
        upd7759stream.adjuststream(second_tick);
        samplestream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateKonami68000_glfgreat() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        k053260stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateKonami68000_ssriders() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        k053260stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateKonami68000_prmrsocr() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        k054539stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

    private static void streams_updateCapcom_gng() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        AY8910.AA8910[0].stream.adjuststream(second_tick);
        AY8910.AA8910[1].stream.adjuststream(second_tick);
        YM2203.FF2203[0].stream.adjuststream(second_tick);
        YM2203.FF2203[1].stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
        AY8910.AA8910[0].stream.updatesamplerate();
        AY8910.AA8910[1].stream.updatesamplerate();
    }

    private static void streams_updateCapcom_sf() {
        Atime curtime = Timer.global_basetime;
        boolean second_tick = false;
        if (curtime.seconds != last_update_second) {
            second_tick = true;
        }
        ym2151stream.adjuststream(second_tick);
        MSM5205.mm1[0].voice.stream.adjuststream(second_tick);
        MSM5205.mm1[1].voice.stream.adjuststream(second_tick);
        mixerstream.adjuststream(second_tick);
        last_update_second = curtime.seconds;
    }

//#endregion
}
