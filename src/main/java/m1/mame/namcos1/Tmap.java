/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.mame.namcos1;

import m1.emu.Tilemap;
import m1.emu.Tilemap.RECT;
import m1.emu.Tilemap.trans_t;
import m1.emu.Video;


public class Tmap extends Tilemap.Tmap {

    public void tile_updateNa(int col, int row) {
        int x0 = tilewidth * col;
        int y0 = tileheight * row;
        int code, tile_index, col2, row2;
        byte flags;
        if ((attributes & Tilemap.TILEMAP_FLIPX) != 0) {
            col2 = (cols - 1) - col;
        } else {
            col2 = col;
        }
        if ((attributes & Tilemap.TILEMAP_FLIPY) != 0) {
            row2 = (rows - 1) - row;
        } else {
            row2 = row;
        }
        tile_index = (row2 * cols + col2) << 1;
        code = Namcos1.namcos1_videoram[videoram_offset + tile_index + 1] + ((Namcos1.namcos1_videoram[videoram_offset + tile_index] & 0x3f) << 8);
        flags = (byte) (attributes & 0x03);
        tileflags[row][col] = tile_drawNa(code * 0x40, x0, y0, 0x800, flags);
        tileflags[row][col] = tile_apply_bitmaskNa(code << 3, x0, y0, flags);
    }

    public byte tile_drawNa(int pendata_offset, int x0, int y0, int palette_base, byte flags) {
        int height = tileheight;
        int width = tilewidth;
        int dx0 = 1, dy0 = 1;
        int tx, ty;
        int offset1 = pendata_offset;
        int offsety1;
        if ((flags & Tilemap.TILE_FLIPY) != 0) {
            y0 += height - 1;
            dy0 = -1;
        }
        if ((flags & Tilemap.TILE_FLIPX) != 0) {
            x0 += width - 1;
            dx0 = -1;
        }
        for (ty = 0; ty < height; ty++) {
            int xoffs = 0;
            offsety1 = y0;
            y0 += dy0;
            for (tx = 0; tx < width; tx++) {
                byte pen;
                pen = Namcos1.gfx2rom[offset1];
                offset1++;
                pixmap[offsety1 * 0x200 + x0 + xoffs] = (short) (0x800 + pen);
                flagsmap[offsety1][x0 + xoffs] = Tilemap.TILEMAP_PIXEL_LAYER0;
                xoffs += dx0;
            }
        }
        return 0;
    }

    public byte tile_apply_bitmaskNa(int maskdata_offset, int x0, int y0, byte flags) {
        int height = tileheight;
        int width = tilewidth;
        byte andmask = (byte) 0xff, ormask = 0;
        int dx0 = 1, dy0 = 1;
        int bitoffs = 0;
        int tx, ty;
        int offsety1;
        if ((flags & Tilemap.TILE_FLIPY) != 0) {
            y0 += height - 1;
            dy0 = -1;
        }
        if ((flags & Tilemap.TILE_FLIPX) != 0) {
            x0 += width - 1;
            dx0 = -1;
        }
        for (ty = 0; ty < height; ty++) {
            int xoffs = 0;
            offsety1 = y0;
            y0 += dy0;
            for (tx = 0; tx < width; tx++) {
                byte map = flagsmap[offsety1][x0 + xoffs];
                if ((Namcos1.gfx1rom[maskdata_offset + bitoffs / 8] & (0x80 >> (bitoffs & 7))) == 0) {
                    map = flagsmap[offsety1][x0 + xoffs] = Tilemap.TILEMAP_PIXEL_TRANSPARENT;
                }
                andmask &= map;
                ormask |= map;
                xoffs += dx0;
                bitoffs++;
            }
        }
        return (byte) (andmask ^ ormask);
    }

    public void tilemap_draw_instanceNa(RECT cliprect, int xpos, int ypos) {
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
                if (column == maxcol)
                    cur_trans = trans_t.WHOLLY_TRANSPARENT;
                else {
                    if (tileflags[row][column] == Tilemap.TILE_FLAG_DIRTY) {
                        tile_updateNa(column, row);
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
                            for (i = xpos + x_start; i < xpos + x_end; i++) {
                                Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x200 + i] = (short) (pixmap[offsety2 * 0x200 + i - xpos] + palette_offset);
                                Tilemap.priority_bitmap[offsety2 + ypos][i] = priority;
                            }
                            offsety2++;
                        }
                    } else if (prev_trans == trans_t.MASKED) {
                        for (cury = y; cury < nexty; cury++) {
                            for (i = xpos + x_start; i < xpos + x_end; i++) {
                                if ((flagsmap[offsety2][i - xpos] & mask) == value) {
                                    Video.bitmapbase[Video.curbitmap][(offsety2 + ypos) * 0x200 + i] = (short) (pixmap[offsety2 * 0x200 + i - xpos] + palette_offset);
                                    Tilemap.priority_bitmap[offsety2 + ypos][i] = priority;
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
}
