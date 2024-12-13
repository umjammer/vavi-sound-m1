/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.m92;

import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Tilemap.trans_t;
import m1.emu.Video;


public class Tmap extends Tilemap.Tmap {

    public void tilemap_draw_instanceM92(RECT cliprect, int xpos, int ypos) {
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
                if (cur_trans == prev_trans)
                    continue;
                x_end = column * tilewidth;
                x_end = Math.max(x_end, x1);
                x_end = Math.min(x_end, x2);
                if (prev_trans != trans_t.WHOLLY_TRANSPARENT) {
                    int cury;
                    offsety2 = offsety1;
                    if (prev_trans == trans_t.WHOLLY_OPAQUE) {
                        for (cury = y; cury < nexty; cury++) {
                            System.arraycopy(pixmap, offsety2 * width + x_start, Video.bitmapbase[Video.curbitmap], (offsety2 + ypos) * 0x200 + xpos + x_start, x_end - x_start);
                            if (priority != 0) {
                                for (i = xpos + x_start; i < xpos + x_end; i++) {
                                    Tilemap.priority_bitmap[offsety2 + ypos][i] =
                                            (byte) (Tilemap.priority_bitmap[offsety2 + ypos][i] | priority);
                                }
                            }
                            offsety2++;
                        }
                    } else if (prev_trans == trans_t.MASKED) {
                        for (cury = y; cury < nexty; cury++) {
                            for (i = xpos + x_start; i < xpos + x_end; i++) {
                                if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                    Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x200 + i] = pixmap[offsety2 * width + i - xpos];
                                    Tilemap.priority_bitmap[offsety2 + ypos][i] =
                                            (byte) (Tilemap.priority_bitmap[offsety2 + ypos][i] | priority);
                                }
                            }
                            offsety2++;
                        }
                    }
                }
                x_start = x_end;
                prev_trans = cur_trans;
            }
            if (nexty == y2)
                break;
            offsety1 += (nexty - y);
            y = nexty;
            nexty += tileheight;
            nexty = Math.min(nexty, y2);
        }
    }

    public void tile_updateM92(int col, int row) {
        int x0 = tilewidth * col;
        int y0 = tileheight * row;
        int flags;
        int tile_index;
        int tile, attrib, code;
        int pen_data_offset, palette_base, group;
        tile_index = 2 * (row * cols + col) + M92.pf_layer[laynum].vram_base;
        attrib = M92.m92_vram_data[tile_index + 1];
        tile = M92.m92_vram_data[tile_index] + ((attrib & 0x8000) << 1);
        code = tile % total_elements;
        pen_data_offset = code * 0x40;
        palette_base = 0x10 * (attrib & 0x7f);
        if ((attrib & 0x100) != 0) {
            group = 2;
        } else if ((attrib & 0x80) != 0) {
            group = 1;
        } else {
            group = 0;
        }
        flags = ((attrib >> 9) & 3) ^ (attributes & 0x03);
        tileflags[row][col] = tile_drawM92(M92.gfx11rom, pen_data_offset, x0, y0, palette_base, group, flags);
    }

    public byte tile_drawM92(byte[] bb1, int pen_data_offset, int x0, int y0, int palette_base, int group, int flags) {
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
                pixmap[(offsety1 % 0x200) * width + x0 + xoffs] = (short) (palette_base + pen);
                flagsmap[offsety1 % 0x200][x0 + xoffs] = map;
                andmask &= map;
                ormask |= map;
                xoffs += dx0;
            }
        }
        return (byte) (andmask ^ ormask);
    }
}
