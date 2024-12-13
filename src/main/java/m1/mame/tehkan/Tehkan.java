/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.tehkan;

import java.awt.event.KeyEvent;
import java.io.IOException;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.BinaryWriter;
import m1.cpu.z80.Z80A;
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
import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Tilemap.trans_t;
import m1.emu.Timer;
import m1.emu.TrackInfo;
import m1.emu.Video;
import m1.sound.AY8910;
import m1.sound.Sound;


public class Tehkan {

//#region Memory

    public static byte byte0, byte1, byte2;
    public static byte byte0_old, byte1_old, byte2_old;

    public static byte Z0ReadOp(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte Z0ReadOp_pbaction3(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0x7fff) {
            result = mainromop[address];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            result = Memory.mainrom[address];
        }
        return result;
    }

    public static byte Z0ReadMemory(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0x7fff) {
            result = Memory.mainrom[address];
        } else if (address >= 0x8000 && address <= 0xbfff) {
            result = Memory.mainrom[address];
        } else if (address >= 0xc000 && address <= 0xcfff) {
            int offset = address - 0xc000;
            result = Memory.mainram[offset];
        } else if (address >= 0xd000 && address <= 0xd3ff) {
            int offset = address - 0xd000;
            result = pbaction_videoram2[offset];
        } else if (address >= 0xd400 && address <= 0xd7ff) {
            int offset = address - 0xd400;
            result = pbaction_colorram2[offset];
        } else if (address >= 0xd800 && address <= 0xdbff) {
            int offset = address - 0xd800;
            result = Generic.videoram[offset];
        } else if (address >= 0xdc00 && address <= 0xdfff) {
            int offset = address - 0xdc00;
            result = Generic.colorram[offset];
        } else if (address >= 0xe000 && address <= 0xe07f) {
            int offset = address - 0xe000;
            result = Generic.spriteram[offset];
        } else if (address >= 0xe400 && address <= 0xe5ff) {
            int offset = address - 0xe400;
            result = Generic.paletteram[offset];
        } else if (address == 0xe600) {
            result = byte0;
        } else if (address == 0xe601) {
            result = byte1;
        } else if (address == 0xe602) {
            result = byte2;
        } else if (address == 0xe604) {
            result = dsw1;
        } else if (address == 0xe605) {
            result = dsw2;
        } else if (address == 0xe606) {
            result = 0;
        }
        return result;
    }

    public static byte Z0ReadMemory_pbaction3(short address) {
        byte result = 0;
        if (address == 0xc000) {
            result = pbaction3_prot_kludge_r();
        } else {
            result = Z0ReadMemory(address);
        }
        return result;
    }

    public static void Z0WriteMemory(short address, byte value) {
        if (address >= 0x0000 && address <= 0x7fff) {
            Memory.mainrom[address] = value;
        } else if (address >= 0x8000 && address <= 0xbfff) {
            Memory.mainrom[address] = value;
        } else if (address >= 0xc000 && address <= 0xcfff) {
            int offset = address - 0xc000;
            Memory.mainram[offset] = value;
        } else if (address >= 0xd000 && address <= 0xd3ff) {
            int offset = address - 0xd000;
            pbaction_videoram2_w(offset, value);
        } else if (address >= 0xd400 && address <= 0xd7ff) {
            int offset = address - 0xd400;
            pbaction_colorram2_w(offset, value);
        } else if (address >= 0xd800 && address <= 0xdbff) {
            int offset = address - 0xd800;
            pbaction_videoram_w(offset, value);
        } else if (address >= 0xdc00 && address <= 0xdfff) {
            int offset = address - 0xdc00;
            pbaction_colorram_w(offset, value);
        } else if (address >= 0xe000 && address <= 0xe07f) {
            int offset = address - 0xe000;
            Generic.spriteram[offset] = value;
        } else if (address >= 0xe400 && address <= 0xe5ff) {
            int offset = address - 0xe400;
            Generic.paletteram_xxxxBBBBGGGGRRRR_le_w(offset, value);
        } else if (address == 0xe600) {
            Generic.interrupt_enable_w(value);
        } else if (address == 0xe604) {
            pbaction_flipscreen_w(value);
        } else if (address == 0xe606) {
            pbaction_scroll_w(value);
        } else if (address == 0xe800) {
            pbaction_sh_command_w(value);
        }
    }

    public static byte Z0ReadHardware(short address) {
        return 0;
    }

    public static void Z0WriteHardware(short address, byte value) {

    }

    public static int Z0IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[0].cpunum, 0);
    }

    public static byte Z1ReadOp(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0x1fff) {
            result = Memory.audiorom[address];
        }
        return result;
    }

    public static byte Z1ReadMemory(short address) {
        byte result = 0;
        if (address >= 0 && address <= 0x1fff) {
            result = Memory.audiorom[address];
        } else if (address >= 0x4000 && address <= 0x47ff) {
            int offset = address - 0x4000;
            result = Memory.audioram[offset];
        } else if (address == 0x8000) {
            result = (byte) Sound.soundlatch_r();
        }
        return result;
    }

    public static void Z1WriteMemory(short address, byte value) {
        if (address >= 0 && address <= 0x1fff) {
            Memory.audiorom[address] = value;
        } else if (address >= 0x4000 && address <= 0x47ff) {
            int offset = address - 0x4000;
            Memory.audioram[offset] = value;
        } else if (address == 0xffff) {

        }
    }

    public static byte Z1ReadHardware(short address) {
        byte result = 0;
        return result;
    }

    public static void Z1WriteHardware(short address, byte value) {
        address &= 0xff;
        if (address >= 0x10 && address <= 0x11) {
            int offset = address - 0x10;
            AY8910.AA8910[0].ay8910_write_ym(offset, value);
        } else if (address >= 0x20 && address <= 0x21) {
            int offset = address - 0x20;
            AY8910.AA8910[1].ay8910_write_ym(offset, value);
        } else if (address >= 0x30 && address <= 0x31) {
            int offset = address - 0x30;
            AY8910.AA8910[2].ay8910_write_ym(offset, value);
        }
    }

    public static int Z1IRQCallback() {
        return Cpuint.cpu_irq_callback(Z80A.zz1[1].cpunum, 0);
    }

//#endregion

//#region State

    public static void SaveStateBinary_pbaction(BinaryWriter writer) {
        int i;
        writer.write(dsw1);
        writer.write(dsw2);
        writer.write(scroll);
        for (i = 0; i < 0x100; i++) {
            writer.write(Palette.entry_color[i]);
        }
        writer.write(Memory.mainram, 0, 0x1000);
        writer.write(Generic.videoram, 0, 0x400);
        writer.write(pbaction_videoram2, 0, 0x400);
        writer.write(Generic.colorram, 0, 0x400);
        writer.write(pbaction_colorram2, 0, 0x400);
        writer.write(Generic.spriteram, 0, 0x80);
        writer.write(Generic.paletteram, 0, 0x200);
        writer.write(Memory.audioram, 0, 0x800);
        Z80A.zz1[0].SaveStateBinary(writer);
        Z80A.zz1[1].SaveStateBinary(writer);
        Cpuint.SaveStateBinary(writer);
        writer.write(Timer.global_basetime.seconds);
        writer.write(Timer.global_basetime.attoseconds);
        Video.SaveStateBinary(writer);
        writer.write(Sound.last_update_second);
        Cpuexec.SaveStateBinary(writer);
        Timer.saveStateBinary(writer);
        for (i = 0; i < 3; i++) {
            AY8910.AA8910[i].SaveStateBinary(writer);
        }
        writer.write(Sound.latched_value[0]);
        writer.write(Sound.utempdata[0]);
        for (i = 0; i < 3; i++) {
            writer.write(AY8910.AA8910[i].stream.output_sampindex);
            writer.write(AY8910.AA8910[i].stream.output_base_sampindex);
        }
        writer.write(Sound.mixerstream.output_sampindex);
        writer.write(Sound.mixerstream.output_base_sampindex);
    }

    public static void LoadStateBinary_pbaction(BinaryReader reader) {
        try {
            int i;
            dsw1 = reader.readByte();
            dsw2 = reader.readByte();
            scroll = reader.readInt32();
            for (i = 0; i < 0x100; i++) {
                Palette.entry_color[i] = reader.readUInt32();
            }
            Memory.mainram = reader.readBytes(0x1000);
            Generic.videoram = reader.readBytes(0x400);
            pbaction_videoram2 = reader.readBytes(0x400);
            Generic.colorram = reader.readBytes(0x400);
            pbaction_colorram2 = reader.readBytes(0x400);
            Generic.spriteram = reader.readBytes(0x80);
            Generic.paletteram = reader.readBytes(0x200);
            Memory.audioram = reader.readBytes(0x800);
            Z80A.zz1[0].LoadStateBinary(reader);
            Z80A.zz1[1].LoadStateBinary(reader);
            Cpuint.LoadStateBinary(reader);
            Timer.global_basetime.seconds = reader.readInt32();
            Timer.global_basetime.attoseconds = reader.readInt64();
            Video.LoadStateBinary(reader);
            Sound.last_update_second = reader.readInt32();
            Cpuexec.LoadStateBinary(reader);
            Timer.loadStateBinary(reader);
            for (i = 0; i < 3; i++) {
                AY8910.AA8910[i].LoadStateBinary(reader);
            }
            Sound.latched_value[0] = reader.readUInt16();
            Sound.utempdata[0] = reader.readUInt16();
            for (i = 0; i < 3; i++) {
                AY8910.AA8910[i].stream.output_sampindex = reader.readInt32();
                AY8910.AA8910[i].stream.output_base_sampindex = reader.readInt32();
            }
            Sound.mixerstream.output_sampindex = reader.readInt32();
            Sound.mixerstream.output_base_sampindex = reader.readInt32();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

//#endregion

//#region Pbaction

    public static byte dsw1, dsw2;
    public static byte[] mainromop, gfx1rom, gfx2rom, gfx3rom, gfx32rom;

    public static void PbactionInit() {
        int i, n;
        Machine.bRom = true;
        switch (Machine.sName) {
            case "pbaction":
            case "pbaction2":
                Memory.mainrom = new byte[0x1_0000]; // Machine.GetRom("maincpu.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx1rom = new byte[0x1_0000]; // Machine.GetRom("gfx1.rom");
                gfx2rom = new byte[0x1_0000]; // Machine.GetRom("gfx2.rom");
                gfx3rom = new byte[0x1_0000]; // Machine.GetRom("gfx3.rom");
                gfx32rom = new byte[0x1_0000]; // Machine.GetRom("gfx32.rom");
                Memory.mainram = new byte[0x1000];
                Memory.audioram = new byte[0x800];
                Generic.videoram = new byte[0x400];
                pbaction_videoram2 = new byte[0x400];
                Generic.colorram = new byte[0x400];
                pbaction_colorram2 = new byte[0x400];
                Generic.spriteram = new byte[0x80];
                Generic.paletteram = new byte[0x200];
                if (Memory.mainrom == null || Memory.audiorom == null || gfx1rom == null || gfx2rom == null || gfx3rom == null || gfx32rom == null) {
                    Machine.bRom = false;
                }
                break;
            case "pbaction3":
            case "pbaction4":
            case "pbaction5":
                Memory.mainrom = Machine.GetRom("maincpu.rom");
                mainromop = Machine.GetRom("maincpuop.rom");
                Memory.audiorom = Machine.GetRom("audiocpu.rom");
                gfx1rom = Machine.GetRom("gfx1.rom");
                gfx2rom = Machine.GetRom("gfx2.rom");
                gfx3rom = Machine.GetRom("gfx3.rom");
                gfx32rom = Machine.GetRom("gfx32.rom");
                Memory.mainram = new byte[0x1000];
                Memory.audioram = new byte[0x800];
                Generic.videoram = new byte[0x400];
                pbaction_videoram2 = new byte[0x400];
                Generic.colorram = new byte[0x400];
                pbaction_colorram2 = new byte[0x400];
                Generic.spriteram = new byte[0x80];
                Generic.paletteram = new byte[0x200];
                if (Memory.mainrom == null || mainromop == null || Memory.audiorom == null || gfx1rom == null || gfx2rom == null || gfx3rom == null || gfx32rom == null) {
                    Machine.bRom = false;
                }
                break;
        }
        if (Machine.bRom) {
            switch (Machine.sName) {
                case "pbaction":
                case "pbaction2":
                case "pbaction3":
                case "pbaction4":
                case "pbaction5":
                    dsw1 = 0x40;
                    dsw2 = 0x00;
                    break;
            }
        }
    }

    public static void pbaction_sh_command_w(byte data) {
        Sound.soundlatch_w(data);
        Cpuint.cpunum_set_input_line_and_vector2(1, 0, LineState.HOLD_LINE, 0);
    }

    public static void pbaction_interrupt() {
        Cpuint.cpunum_set_input_line_and_vector2(1, 0, LineState.HOLD_LINE, 0x02);
    }

    public static byte pbaction3_prot_kludge_r() {
        byte result;
        if (Z80A.zz1[0].getPC() == (short) 0xab80) {
            result = 0;
        } else {
            result = Memory.mainram[0];
        }
        return result;
    }

    public static void machine_reset_tehkan() {

    }

    public static void play_tehkan_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stop_tehkan_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) ti.TrackID));
    }

    public static void stopandplay_tehkan_default(TrackInfo ti) {
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number, 0, (byte) RomInfo.iStop));
        Inptport.lsFA.add(new FrameArray(Video.screenstate.frame_number + 1, 0, (byte) ti.TrackID));
    }

//#endregion

//#region Input

    public static void loop_inputports_tehkan_pbaction() {
        if (Keyboard.IsPressed(KeyEvent.VK_5)) {
            byte2 |= 0x01;
        } else {
            byte2 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_6)) {
            byte2 |= 0x02;
        } else {
            byte2 &= (byte) ~0x02;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_1)) {
            byte2 |= 0x04;
        } else {
            byte2 &= (byte) ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_2)) {
            byte2 |= 0x08;
        } else {
            byte2 &= (byte) ~0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_J)) {
            byte0 |= 0x08;
        } else {
            byte0 &= (byte) ~0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_K)) {
            byte0 |= 0x10;
        } else {
            byte0 &= (byte) ~0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_L)) {
            byte0 |= 0x01;
        } else {
            byte0 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_U)) {
            byte0 |= 0x04;
        } else {
            byte0 &= (byte) ~0x04;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD1)) {
            byte1 |= 0x08;
        } else {
            byte1 &= (byte) ~0x08;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD2)) {
            byte1 |= 0x10;
        } else {
            byte1 &= (byte) ~0x10;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD3)) {
            byte1 |= 0x01;
        } else {
            byte1 &= (byte) ~0x01;
        }
        if (Keyboard.IsPressed(KeyEvent.VK_NUMPAD4)) {
            byte1 |= 0x04;
        } else {
            byte1 &= (byte) ~0x04;
        }
    }

    public static void record_port_pbaction() {
        if (byte0 != byte0_old || byte1 != byte1_old || byte2 != byte2_old) {
            byte0_old = byte0;
            byte1_old = byte1;
            byte2_old = byte2;
            Mame.bwRecord.write(Video.screenstate.frame_number);
            Mame.bwRecord.write(byte0);
            Mame.bwRecord.write(byte1);
            Mame.bwRecord.write(byte2);
        }
    }

    public static void replay_port_pbaction() {
        if (Inptport.bReplayRead) {
            try {
                Video.frame_number_obj = Mame.brRecord.readInt64();
                byte0_old = Mame.brRecord.readByte();
                byte1_old = Mame.brRecord.readByte();
                byte2_old = Mame.brRecord.readByte();
            } catch (IOException e)
            {
                Mame.playState = Mame.PlayState.PLAY_REPLAYEND;
            }
            Inptport.bReplayRead = false;
        }
        if (Video.screenstate.frame_number == Video.frame_number_obj) {
            byte0 = byte0_old;
            byte1 = byte1_old;
            byte2 = byte2_old;
            Inptport.bReplayRead = true;
        } else {
            Inptport.bReplayRead = false;
        }
    }

//#endregion

//#region Video

    public static byte[] pbaction_videoram2, pbaction_colorram2;
    public static int scroll;
    public static RECT cliprect;

    public static void pbaction_videoram_w(int offset, byte data) {
        int row, col;
        Generic.videoram[offset] = data;
        row = offset / 0x20;
        col = offset % 0x20;
        bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void pbaction_colorram_w(int offset, byte data) {
        int row, col;
        Generic.colorram[offset] = data;
        row = offset / 0x20;
        col = offset % 0x20;
        bg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void pbaction_videoram2_w(int offset, byte data) {
        int row, col;
        pbaction_videoram2[offset] = data;
        row = offset / 0x20;
        col = offset % 0x20;
        fg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void pbaction_colorram2_w(int offset, byte data) {
        int row, col;
        pbaction_colorram2[offset] = data;
        row = offset / 0x20;
        col = offset % 0x20;
        fg_tilemap.tilemap_mark_tile_dirty(row, col);
    }

    public static void pbaction_scroll_w(byte data) {
        scroll = data - 3;
        if (Generic.flip_screen_get() != 0) {
            scroll = -scroll;
        }
        bg_tilemap.tilemap_set_scrollx(0, scroll);
        fg_tilemap.tilemap_set_scrollx(0, scroll);
    }

    public static void pbaction_flipscreen_w(byte data) {
        Generic.flip_screen_set(data & 0x01);
    }

    public static void video_start_pbaction() {
        cliprect = new RECT();
        cliprect.min_x = 0;
        cliprect.max_x = 0xff;
        cliprect.min_y = 0x10;
        cliprect.max_y = 0xef;
    }

    public static void draw_sprites(RECT cliprect) {
        int offs;
        for (offs = 0x80 - 4; offs >= 0; offs -= 4) {
            int sx, sy, flipx, flipy;
            if (offs > 0 && (Generic.spriteram[offs - 4] & 0x80) != 0) {
                continue;
            }
            sx = Generic.spriteram[offs + 3];
            if ((Generic.spriteram[offs] & 0x80) != 0) {
                sy = 225 - Generic.spriteram[offs + 2];
            } else {
                sy = 241 - Generic.spriteram[offs + 2];
            }
            flipx = Generic.spriteram[offs + 1] & 0x40;
            flipy = Generic.spriteram[offs + 1] & 0x80;
            if (Generic.flip_screen_get() != 0) {
                if ((Generic.spriteram[offs] & 0x80) != 0) {
                    sx = 224 - sx;
                    sy = 225 - sy;
                } else {
                    sx = 240 - sx;
                    sy = 241 - sy;
                }
                flipx = (flipx == 0 ? 1 : 0);
                flipy = (flipy == 0 ? 1 : 0);
            }
            if ((Generic.spriteram[offs] & 0x80) != 0) {
                Drawgfx.common_drawgfx_pbaction(gfx32rom, 32, 32, 32, 0x20, Generic.spriteram[offs], Generic.spriteram[offs + 1] & 0x0f, flipx, flipy, sx + (Generic.flip_screen_get() != 0 ? scroll : -scroll), sy, cliprect);
            } else {
                Drawgfx.common_drawgfx_pbaction(gfx3rom, 16, 16, 16, 0x80, Generic.spriteram[offs], Generic.spriteram[offs + 1] & 0x0f, flipx, flipy, sx + (Generic.flip_screen_get() != 0 ? scroll : -scroll), sy, cliprect);
            }
        }
    }

    public static void video_update_pbaction() {
        bg_tilemap.tilemap_draw_primask(cliprect, 0x10, (byte) 0);
        draw_sprites(cliprect);
        fg_tilemap.tilemap_draw_primask(cliprect, 0x10, (byte) 0);
    }

    public static void video_eof_pbaction() {

    }

//#endregion

//#region Tilemap

    public static Tmap bg_tilemap, fg_tilemap;

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
        bg_tilemap.total_elements = gfx2rom.length / 0x40;
        bg_tilemap.pixmap = new short[0x100 * 0x100];
        bg_tilemap.flagsmap = new byte[0x100][0x100];
        bg_tilemap.tileflags = new byte[32][32];
        bg_tilemap.pen_data = new byte[0x100];
        bg_tilemap.pen_to_flags = new byte[1][16];
        for (i = 0; i < 16; i++) {
            bg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        bg_tilemap.scrollrows = 1;
        bg_tilemap.scrollcols = 1;
        bg_tilemap.rowscroll = new int[bg_tilemap.scrollrows];
        bg_tilemap.colscroll = new int[bg_tilemap.scrollcols];
        bg_tilemap.tilemap_draw_instance3 = bg_tilemap::tilemap_draw_instanceTehkan_pbaction;
        bg_tilemap.tile_update3 = bg_tilemap::tile_updatePbactionbg;

        fg_tilemap = new Tmap();
        fg_tilemap.cols = 32;
        fg_tilemap.rows = 32;
        fg_tilemap.tilewidth = 8;
        fg_tilemap.tileheight = 8;
        fg_tilemap.width = 0x100;
        fg_tilemap.height = 0x100;
        fg_tilemap.enable = true;
        fg_tilemap.all_tiles_dirty = true;
        fg_tilemap.total_elements = gfx1rom.length / 0x40;
        fg_tilemap.pixmap = new short[0x100 * 0x100];
        fg_tilemap.flagsmap = new byte[0x100][0x100];
        fg_tilemap.tileflags = new byte[32][32];
        fg_tilemap.pen_data = new byte[0x100];
        fg_tilemap.pen_to_flags = new byte[1][16];
        fg_tilemap.pen_to_flags[0][0] = 0;
        for (i = 1; i < 16; i++) {
            fg_tilemap.pen_to_flags[0][i] = 0x10;
        }
        fg_tilemap.scrollrows = 1;
        fg_tilemap.scrollcols = 1;
        fg_tilemap.rowscroll = new int[fg_tilemap.scrollrows];
        fg_tilemap.colscroll = new int[fg_tilemap.scrollcols];
        fg_tilemap.tilemap_draw_instance3 = fg_tilemap::tilemap_draw_instanceTehkan_pbaction;
        fg_tilemap.tile_update3 = fg_tilemap::tile_updatePbactionfg;
    }

    public static class Tmap extends Tilemap.Tmap {

        public void tile_updatePbactionbg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, attr, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;

            attr = Generic.colorram[tile_index];
            code = Generic.videoram[tile_index] + 0x10 * (attr & 0x70);
            color = attr & 0x0f;
            flags = (attr & 0x80) != 0 ? Tilemap.TILE_FLIPY : 0;

            pen_data_offset = code * 0x40;
            palette_base = 0x80 + 0x10 * color;
            group = 0;

            tileflags[row][col] = tile_drawTehkanbg(Tehkan.gfx2rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public void tile_updatePbactionfg(int col, int row) {
            int x0 = tilewidth * col;
            int y0 = tileheight * row;
            int flags;
            int tile_index;
            int code, attr, color;
            int pen_data_offset, palette_base, group;
            tile_index = row * cols + col;

            attr = Tehkan.pbaction_colorram2[tile_index];
            code = Tehkan.pbaction_videoram2[tile_index] + 0x10 * (attr & 0x30);
            color = attr & 0x0f;
            flags = ((attr & 0x40) != 0 ? Tilemap.TILE_FLIPX : 0) | ((attr & 0x80) != 0 ? Tilemap.TILE_FLIPY : 0);

            pen_data_offset = code * 0x40;
            palette_base = 0x08 * color;
            group = 0;

            tileflags[row][col] = tile_drawTehkanfg(Tehkan.gfx1rom, pen_data_offset, x0, y0, palette_base, group, flags);
        }

        public byte tile_drawTehkanbg(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
            byte andmask = (byte) 0xff, ormask = 0;
            int dx0 = 1, dy0 = 1;
            int tx, ty;
            byte pen, map;
            int offset1 = 0;
            int offsety1;
            int xoffs;
            System.arraycopy(bb1, pen_data_offset, pen_data, 0, 0x40);
            if ((flags & Tilemap.TILE_FLIPY) != 0) {
                y0 += tileheight - 1;
                dy0 = -1;
            }
            if ((flags & Tilemap.TILE_FLIPX) != 0) {
                x0 += tilewidth - 1;
                dx0 = -1;
            }
            for (ty = 0; ty < tileheight; ty++) {
                xoffs = 0;
                offsety1 = y0;
                y0 += dy0;
                for (tx = 0; tx < tilewidth; tx++) {
                    pen = pen_data[offset1];
                    map = pen_to_flags[group][pen];
                    offset1++;
                    pixmap[(offsety1 % width) * width + x0 + xoffs] = (short) (palette_base + pen);
                    if (palette_base + pen == 0x0106) {
                        int i1 = 1;
                    }
                    flagsmap[offsety1 % width][x0 + xoffs] = map;
                    andmask &= map;
                    ormask |= map;
                    xoffs += dx0;
                }
            }
            return (byte) (andmask ^ ormask);
        }

        public byte tile_drawTehkanfg(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
            byte andmask = (byte) 0xff, ormask = 0;
            int dx0 = 1, dy0 = 1;
            int tx, ty;
            byte pen, map;
            int offset1 = 0;
            int offsety1;
            int xoffs;
            System.arraycopy(bb1, pen_data_offset, pen_data, 0, 0x40);
            if ((flags & Tilemap.TILE_FLIPY) != 0) {
                y0 += tileheight - 1;
                dy0 = -1;
            }
            if ((flags & Tilemap.TILE_FLIPX) != 0) {
                x0 += tilewidth - 1;
                dx0 = -1;
            }
            for (ty = 0; ty < tileheight; ty++) {
                xoffs = 0;
                offsety1 = y0;
                y0 += dy0;
                for (tx = 0; tx < tilewidth; tx++) {
                    pen = pen_data[offset1];
                    map = pen_to_flags[group][pen];
                    offset1++;
                    pixmap[(offsety1 % width) * width + x0 + xoffs] = (short) (palette_base + pen);
                    flagsmap[offsety1 % width][x0 + xoffs] = map;
                    andmask &= map;
                    ormask |= map;
                    xoffs += dx0;
                }
            }
            return (byte) (andmask ^ ormask);
        }

        public void tilemap_draw_instanceTehkan_pbaction(RECT cliprect, int xpos, int ypos) {
            int mincol, maxcol;
            int x1, y1, x2, y2;
            int y, nexty;
            int offsety1, offsety2;
            int i;
            x1 = Math.max(xpos, cliprect.min_x);
            x2 = Math.min(xpos + width, cliprect.max_x + 1);
            y1 = Math.max(ypos, cliprect.min_y);
            y2 = Math.min(ypos + height, cliprect.max_y + 1);
            if (x1 >= x2 || y1 >= y2)
                return;
            x1 -= xpos;
            y1 -= ypos;
            x2 -= xpos;
            y2 -= ypos;
            offsety1 = y1;
            mincol = x1 / tilewidth;
            maxcol = (x2 + tilewidth - 1) / tilewidth;
            y = y1;
            nexty = tileheight * (y1 / tileheight) + tileheight;
            nexty = Math.min(nexty, y2);
            for (; ; ) {
                int row = y / tileheight;
                trans_t prev_trans = trans_t.WHOLLY_TRANSPARENT;
                trans_t cur_trans;
                int x_start = x1;
                int column;
                for (column = mincol; column <= maxcol; column++) {
                    int x_end;
                    if (column == maxcol) {
                        cur_trans = trans_t.WHOLLY_TRANSPARENT;
                    } else {
                        if (tileflags[row][column] == Tilemap.TILE_FLAG_DIRTY) {
                            tile_update3.accept(column, row);
                        }
                        if ((tileflags[row][column] & mask) != 0) {
                            cur_trans = trans_t.MASKED;
                        } else {
                            cur_trans = ((flagsmap[offsety1][column * tilewidth] & mask) == value) ? trans_t.WHOLLY_OPAQUE :
                                    trans_t.WHOLLY_TRANSPARENT;
                        }
                    }
                    if (cur_trans == prev_trans) {
                        continue;
                    }
                    x_end = column * tilewidth;
                    x_end = Math.max(x_end, x1);
                    x_end = Math.min(x_end, x2);
                    if (prev_trans != trans_t.WHOLLY_TRANSPARENT) {
                        int cury;
                        offsety2 = offsety1;
                        if (prev_trans == trans_t.WHOLLY_OPAQUE) {
                            for (cury = y; cury < nexty; cury++) {
                                System.arraycopy(pixmap, offsety2 * width + x_start, Video.bitmapbase[Video.curbitmap], (offsety2 + ypos) * 0x100 + xpos + x_start, x_end - x_start);
                                offsety2++;
                            }
                        } else if (prev_trans == trans_t.MASKED) {
                            for (cury = y; cury < nexty; cury++) {
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                        Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x100 + i] = pixmap[offsety2 * width + i - xpos];
                                    }
                                }
                                offsety2++;
                            }
                        }
                    }
                    x_start = x_end;
                    prev_trans = cur_trans;
                }
                if (nexty == y2) {
                    break;
                }
                offsety1 += (nexty - y);
                y = nexty;
                nexty += tileheight;
                nexty = Math.min(nexty, y2);
            }
        }
    }

//#endregion
}
