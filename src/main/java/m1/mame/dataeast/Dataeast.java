/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.dataeast;

import java.awt.event.KeyEvent;
import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.m6502.M6502;
import m1.emu.Cpuexec;
import m1.emu.Cpuint;
import m1.emu.Cpuint.LineState;
import m1.emu.Generic;
import m1.emu.Inptport;
import m1.emu.Inptport.FrameArray;
import m1.emu.Keyboard;
import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.Memory;
import m1.emu.Palette;
import m1.emu.RomInfo;
import m1.emu.Tilemap.RECT;
import m1.emu.Timer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.AY8910;
import m1.sound.MSM5205;
import m1.sound.Sound;
import m1.sound.YM2203;
import m1.sound.YM3812;


public class Dataeast {

//#region Memory

    public static byte byte1, byte2;
    public static byte byte1_old, byte2_old;

    public static byte D0ReadOp(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.mainram[address];
        } else if (address >= 0x4000 && address <= 0x5fff) {
            int offset = address - 0x4000;
            result = Memory.mainrom[basebankmain1 + offset];
        } else if (address >= 0x6000 && address <= 0x7fff) {
            int offset = address - 0x6000;
            result = Memory.mainrom[basebankmain2 + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte D0ReadOpArg(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.mainram[address];
        } else if (address >= 0x4000 && address <= 0x5fff) {
            int offset = address - 0x4000;
            result = Memory.mainrom[basebankmain1 + offset];
        } else if (address >= 0x6000 && address <= 0x7fff) {
            int offset = address - 0x6000;
            result = Memory.mainrom[basebankmain2 + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte D0ReadMemory(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.mainram[address];
        } else if (address == 0x1800) {
            result = byte1;
        } else if (address == 0x1a00) {
            result = byte2;
        } else if (address == 0x1c00) {
            result = dsw;
        } else if (address >= 0x4000 && address <= 0x5fff) {
            int offset = address - 0x4000;
            result = Memory.mainrom[basebankmain1 + offset];
        } else if (address >= 0x6000 && address <= 0x7fff) {
            int offset = address - 0x6000;
            result = Memory.mainrom[basebankmain2 + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static void D0WriteMemory(short address, byte data) {
        if (address <= 0x7ff) {
            Memory.mainram[address] = data;
        } else if (address >= 0x800 && address <= 0xfff) {
            int offset = address - 0x800;
            pcktgal_videoram_w(offset, data);
        } else if (address >= 0x1000 && address <= 0x11ff) {
            int offset = address - 0x1000;
            Generic.spriteram[offset] = data;
        } else if (address == 0x1801) {
            pcktgal_flipscreen_w(data);
        } else if (address == 0x1a00) {
            pcktgal_sound_w(data);
        } else if (address == 0x1c00) {
            pcktgal_bank_w(data);
        } else if (address >= 0x4000 && (address & 0xffff) <= 0xffff) {
            Memory.mainrom[address] = data;
        }
    }

    public static byte D1ReadOp(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.audioram[address];
        } else if (address >= 0x4000 && address <= 0x7fff) {
            int offset = address - 0x4000;
            result = audioromop[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            int offset = address - 0x8000;
            result = audioromop[offset];
        }
        return result;
    }

    public static byte D1ReadOp_2(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.audioram[address];
        } else if (address >= 0x4000 && address <= 0x7fff) {
            int offset = address - 0x4000;
            result = Memory.audiorom[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static byte D1ReadOpArg(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.audioram[address];
        } else if (address >= 0x4000 && address <= 0x7fff) {
            int offset = address - 0x4000;
            result = Memory.audiorom[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static byte D1ReadMemory(short address) {
        byte result = 0;
        if (address <= 0x7ff) {
            result = Memory.audioram[address];
        } else if (address == 0x3000) {
            result = (byte) Sound.soundlatch_r();
        } else if (address == 0x3400) {
            result = pcktgal_adpcm_reset_r();
        } else if (address >= 0x4000 && address <= 0x7fff) {
            int offset = address - 0x4000;
            result = Memory.audiorom[basebanksnd + offset];
        } else if ((address & 0xffff) >= 0x8000 && (address & 0xffff) <= 0xffff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static void D1WriteMemory(short address, byte data) {
        if (address <= 0x7ff) {
            Memory.audioram[address] = data;
        } else if (address == 0x0800) {
            YM2203.ym2203_control_port_0_w(data);
        } else if (address == 0x0801) {
            YM2203.ym2203_write_port_0_w(data);
        } else if (address == 0x1000) {
            YM3812.ym3812_control_port_0_w(data);
        } else if (address == 0x1001) {
            YM3812.ym3812_write_port_0_w(data);
        } else if (address == 0x1800) {
            pcktgal_adpcm_data_w(data);
        } else if (address == 0x2000) {
            pcktgal_sound_bank_w(data);
        } else if (address >= 0x4000 && (address & 0xffff) <= 0xffff) {
            Memory.audiorom[address] = data;
        }
    }

//#endregion

//#region Input

    public static void loop_inputports_dataeast_pcktgal() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 &= (byte) ~0x10;
        } else {
            byte2 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 &= (byte) ~0x20;
        } else {
            byte2 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte1 &= (byte) ~0x10;
        } else {
            byte1 |= 0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte1 &= (byte) ~0x20;
        } else {
            byte1 |= 0x20;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_D)) {
            byte1 &= (byte) ~0x01;
        } else {
            byte1 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_A)) {
            byte1 &= (byte) ~0x02;
        } else {
            byte1 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_S)) {
            byte1 &= (byte) ~0x04;
        } else {
            byte1 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_W)) {
            byte1 &= (byte) ~0x08;
        } else {
            byte1 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte1 &= (byte) ~0x80;
        } else {
            byte1 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte1 &= (byte) ~0x40;
        } else {
            byte1 |= 0x40;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_RIGHT)) {
            byte2 &= (byte) ~0x01;
        } else {
            byte2 |= 0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_LEFT)) {
            byte2 &= (byte) ~0x02;
        } else {
            byte2 |= 0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_DOWN)) {
            byte2 &= (byte) ~0x04;
        } else {
            byte2 |= 0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_UP)) {
            byte2 &= (byte) ~0x08;
        } else {
            byte2 |= 0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte2 &= (byte) ~0x80;
        } else {
            byte2 |= (byte) 0x80;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            byte2 &= (byte) ~0x40;
        } else {
            byte2 |= 0x40;
        }
    }

    public static void record_port_pcktgal() {
        if (byte1 != byte1_old || byte2 != byte2_old) {
            byte1_old = byte1;
            byte2_old = byte2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(byte1);
            Mame.bwRecord.write(byte2);
        }
    }

    public static void replay_port_pcktgal() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                byte1_old = Mame.brRecord.readByte();
                byte2_old = Mame.brRecord.readByte();
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            byte1 = byte1_old;
            byte2 = byte2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region State

    public static void SaveStateBinary_pcktgal(BinaryWriter writer) {
        int i;
        writer.write(dsw);
        writer.write(basebankmain1);
        writer.write(basebankmain2);
        writer.write(basebanksnd);
        writer.write(msm5205next);
        writer.write(toggle);
        for (i = 0; i < 0x200; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x800);
        writer.write(Generic.videoram, 0, 0x800);
        writer.write(Generic.spriteram, 0, 0x200);
        writer.write(Memory.audioram, 0, 0x800);
        M6502.mm1[0].SaveStateBinary(writer);
        M6502.mm1[1].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        AY8910.AA8910[0].SaveStateBinary(writer);
        YM2203.FF2203[0].SaveStateBinary(writer);
        YM3812.SaveStateBinary(writer);
        MSM5205.mm1[0].SaveStateBinary(writer);
        writer.write(Sound.latched_value[0]);
        writer.write(Sound.utempdata[0]);
        writer.write(AY8910.AA8910[0].stream.output_sampindex);
        writer.write(AY8910.AA8910[0].stream.output_base_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_sampindex);
        writer.write(YM2203.FF2203[0].stream.output_base_sampindex);
        writer.write(Sound.ym3812stream.output_sampindex);
        writer.write(Sound.ym3812stream.output_base_sampindex);
        writer.write(MSM5205.mm1[0].voice.stream.output_sampindex);
        writer.write(MSM5205.mm1[0].voice.stream.output_base_sampindex);
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_pcktgal(BinaryReader reader) {
        try {
            int i;
            dsw = reader.readByte();
            basebankmain1 = reader.readInt32();
            basebankmain2 = reader.readInt32();
            basebanksnd = reader.readInt32();
            msm5205next = reader.readInt32();
            toggle = reader.readInt32();
            for (i = 0; i < 0x200; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x800);
            Generic.videoram = reader.readBytes(0x800);
            Generic.spriteram = reader.readBytes(0x200);
            Memory.audioram = reader.readBytes(0x800);
            M6502.mm1[0].LoadStateBinary(reader);
            M6502.mm1[1].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            AY8910.AA8910[0].LoadStateBinary(reader);
            YM2203.FF2203[0].LoadStateBinary(reader);
            YM3812.LoadStateBinary(reader);
            MSM5205.mm1[0].LoadStateBinary(reader);
            Sound.latched_value[0] = reader.readUInt16();
            Sound.utempdata[0] = reader.readUInt16();
            AY8910.AA8910[0].stream.output_sampindex = reader.readInt32();
            AY8910.AA8910[0].stream.output_base_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_sampindex = reader.readInt32();
            YM2203.FF2203[0].stream.output_base_sampindex = reader.readInt32();
            Sound.ym3812stream.output_sampindex = reader.readInt32();
            Sound.ym3812stream.output_base_sampindex = reader.readInt32();
            MSM5205.mm1[0].voice.stream.output_sampindex = reader.readInt32();
            MSM5205.mm1[0].voice.stream.output_base_sampindex = reader.readInt32();
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Pcktgal

    public static byte[] audioromop, gfx1rom, gfx2rom, gfx12rom, gfx22rom, prom;
    public static byte dsw;
    public static int basebankmain1, basebankmain2, basebanksnd, msm5205next, toggle;

    public static void DataeastInit() {
        Machine.bRom = true;
        Memory.mainram = new byte[0x800];
        Memory.audioram = new byte[0x800];
        Generic.spriteram = new byte[0x200];
        Generic.videoram = new byte[0x800];
        switch (Machine.sName) {
            case "pcktgal":
            case "pcktgalb":
                Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                audioromop = Machine.GetRom("audiocpuop.rom");
                gfx1rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
                gfx2rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
                prom = new byte[0x1_0000]; // Machine.GetRom("proms.rom");
                if (Memory.mainrom == null || Memory.audiorom == null || audioromop == null) {
                    Machine.bRom = false;
                }
                break;
            case "pcktgal2":
            case "pcktgal2j":
            case "spool3":
            case "spool3i":
                Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx1rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
                gfx2rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
                prom = new byte[0x1_0000]; // Machine.GetRom("proms.rom");
                if (Memory.mainrom == null || Memory.audiorom == null) {
                    Machine.bRom = false;
                }
                break;
        }
        if (Machine.bRom) {
            dsw = (byte) 0xbf;
        }
    }

    public static void irqhandler(int irq) {
    }

    public static void pcktgal_bank_w(byte data) {
        if ((data & 1) != 0) {
            basebankmain1 = 0x4000;
        } else {
            basebankmain1 = 0x1_0000;
        }
        if ((data & 2) != 0) {
            basebankmain2 = 0x6000;
        } else {
            basebankmain2 = 0x1_2000;
        }
    }

    public static void pcktgal_sound_bank_w(byte data) {
        basebanksnd = 0x1_0000 + 0x4000 * ((data >> 2) & 1);
    }

    public static void pcktgal_sound_w(byte data) {
        Sound.soundlatch_w(data);
        Cpuint.cpunum_set_input_line(1, LineState.INPUT_LINE_NMI.ordinal(), LineState.PULSE_LINE);
    }

    public static void pcktgal_adpcm_int(int data) {
        MSM5205.msm5205_data_w(0, msm5205next >> 4);
        msm5205next <<= 4;
        toggle = 1 - toggle;
        if (toggle != 0) {
            Cpuint.cpunum_set_input_line(1, 0, LineState.HOLD_LINE);
        }
    }

    public static void pcktgal_adpcm_data_w(byte data) {
        msm5205next = data;
    }

    public static byte pcktgal_adpcm_reset_r() {
        MSM5205.msm5205_reset_w(0, 0);
        return 0;
    }

    public static void machine_reset_dataeast() {
        basebankmain1 = 0;
        basebankmain2 = 0;
        basebanksnd = 0;
        msm5205next = 0;
        toggle = 0;
    }

    public static void play_dataeast_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_dataeast_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_dataeast_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//#endregion

//#region Video

    public static void palette_init_pcktgal(byte[] color_prom) {
        int i;
        for (i = 0; i < 0x200; i++) {
            int bit0, bit1, bit2, bit3, r, g, b;
            bit0 = (color_prom[i] >> 0) & 0x01;
            bit1 = (color_prom[i] >> 1) & 0x01;
            bit2 = (color_prom[i] >> 2) & 0x01;
            bit3 = (color_prom[i] >> 3) & 0x01;
            r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
            bit0 = (color_prom[i] >> 4) & 0x01;
            bit1 = (color_prom[i] >> 5) & 0x01;
            bit2 = (color_prom[i] >> 6) & 0x01;
            bit3 = (color_prom[i] >> 7) & 0x01;
            g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
            bit0 = (color_prom[i + 0x200] >> 0) & 0x01;
            bit1 = (color_prom[i + 0x200] >> 1) & 0x01;
            bit2 = (color_prom[i + 0x200] >> 2) & 0x01;
            bit3 = (color_prom[i + 0x200] >> 3) & 0x01;
            b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
            Palette.palette_set_callback.accept(i, Palette.make_rgb(r, g, b));
        }
    }

    public static void pcktgal_videoram_w(int offset, byte data) {
        int row, col;
        Generic.videoram[offset] = data;
        row = (offset / 2) / 0x20;
        col = (offset / 2) % 0x20;
        bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void pcktgal_flipscreen_w(byte data) {
        if (Generic.flip_screen_get() != (data & 0x80)) {
            Generic.flip_screen_set(data & 0x80);
            bg_tilemap.all_tiles_dirty = true;
        }
    }

    public static void draw_sprites(RECT cliprect) {
        int offs;
        for (offs = 0; offs < 0x200; offs += 4) {
            if (Generic.spriteram[offs] != (byte) 0xf8) {
                int sx, sy, flipx, flipy;
                sx = 240 - Generic.spriteram[offs + 2];
                sy = 240 - Generic.spriteram[offs];
                flipx = Generic.spriteram[offs + 1] & 0x04;
                flipy = Generic.spriteram[offs + 1] & 0x02;
                if (Generic.flip_screen_get() != 0) {
                    sx = 240 - sx;
                    sy = 240 - sy;
                    if (flipx != 0) {
                        flipx = 0;
                    } else {
                        flipx = 1;
                    }
                    if (flipy != 0) {
                        flipy = 0;
                    } else {
                        flipy = 1;
                    }
                }
                Drawgfx.common_drawgfx_pcktgal(gfx2rom, 16, 16, 16, 0x400, Generic.spriteram[offs + 3] + ((Generic.spriteram[offs + 1] & 1) << 8), (Generic.spriteram[offs + 1] & 0x70) >> 4, flipx, flipy, sx, sy, cliprect);
            }
        }
    }

    public static void video_update_pcktgal() {
        bg_tilemap.tilemap_draw_primask(Video.screenstate.visarea, 0x10, (byte) 0);
        draw_sprites(Video.screenstate.visarea);
    }

    public static void video_eof_pcktgal() {
    }

//#endregion

//#region Tilemap

    public static Tmap bg_tilemap;

    public static void tilemap_init() {
        int i;
        bg_tilemap = new Tmap();
        bg_tilemap.cols = 32;
        bg_tilemap.rows = 32;
        bg_tilemap.tilewidth = 8;
        bg_tilemap.tileheight = 8;
        bg_tilemap.width = 0x100;
        bg_tilemap.height = 0x100;
        bg_tilemap.enable = true;
        bg_tilemap.all_tiles_dirty = true;
        bg_tilemap.total_elements = gfx1rom.length / 0x40;
        bg_tilemap.pixmap = new short[0x100 * 0x100];
        bg_tilemap.flagsmap = new byte[0x100][0x100];
        bg_tilemap.tileflags = new byte[32][32];
        bg_tilemap.pen_data = new byte[0x100];
        bg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap.pen_to_flags[0][i] =0x10;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceDataeast_pcktgal;
        bg_tilemap.tile_update3 = bg_tilemap::tile_updatePcktgalbg;
    }

//#endregion
}
