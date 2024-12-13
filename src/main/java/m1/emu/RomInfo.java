/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import vavi.swing.binding.table.Column;
import vavi.swing.binding.table.Row;
import vavi.swing.binding.table.Table;
import vavix.util.screenscrape.annotation.SaxonXPathParser;
import vavix.util.screenscrape.annotation.Target;
import vavix.util.screenscrape.annotation.WebScraper;


@WebScraper(url = "classpath:m1.xml",
        parser = SaxonXPathParser.class,
        isDebug = true,
        value = "/m1/game")
@Table(row = RomInfo.class, iterable = "entries")
@Row(setter = "setVO")
public class RomInfo {

    public static List<RomInfo> romList = new ArrayList<>();
    public static RomInfo rom;

    @Target(value = "/game/@name")
    public String name;
    @Target(value = "/game/@board")
    public String board;
    @Target(value = "/game/parent/text()")
    public String parent;
    //@Target(value = "/game/direction/text()")
    public String direction;
    @Target(value = "/game/description/text()")
    public String description;
    @Target(value = "/game/year/text()")
    public String year;
    @Target(value = "/game/manufacturer/text()")
    public String manufacturer;

    @Target(value = "/game/m1data/@default")
    public String m1Default;
    @Target(value = "/game/m1data/@stop")
    public String m1Stop;
    @Target(value = "/game/m1data/@min")
    public String m1Min;
    @Target(value = "/game/m1data/@max")
    public String m1Max;
    @Target(value = "/game/m1data/@subtype")
    public String m1Subtype;

    public static short iStop;

    @Column(sequence = 2, name = "ROM", width = 80)
    public String getName() {
        return name;
    }

    @Column(sequence = 1, width = 60, align = Column.Align.right)
    public String getYear() {
        return year;
    }

    @Column(sequence = 3, width = 60)
    public String getParent() {
        return parent;
    }

    @Column(sequence = 5, width = 120)
    public String getBoard() {
        return board;
    }

    @Column(sequence = 4, width = 120)
    public String getManufacturer() {
        return manufacturer;
    }

    @Column(sequence = 0, name = "Title", width = 350)
    public String getDescription() {
        return description;
    }

    //@Column(sequence = 4, width = 70)
    public String getDirection() {
        return direction;
    }

    public RomInfo() {
    }

    public static RomInfo getRomByName(String s1) {
        for (RomInfo ri : romList) {
            if (s1.equals(ri.name)) {
                return ri;
            }
        }
        return null;
    }

    public static String getParent(String s1) {
        String sParent = "";
        for (RomInfo ri : romList) {
            if (s1.equals(ri.name)) {
                sParent = ri.parent;
                break;
            }
        }
        return sParent;
    }

    public static List<String> getParents(String s1) {
        String sChild, sParent;
        List<String> ls1 = new ArrayList<>();
        sChild = s1;
        while (!sChild.isEmpty()) {
            ls1.add(sChild);
            sParent = getParent(sChild);
            sChild = sParent;
        }
        return ls1;
    }

    public Iterable<RomInfo> entries() {
        return romList;
    }

    public void setVO(RomInfo romInfo) {
        this.name = romInfo.name;
        this.year = romInfo.year;
        this.parent = romInfo.parent;
        this.board = romInfo.board;
        this.manufacturer = romInfo.manufacturer;
        this.description = romInfo.description;
        this.direction = romInfo.direction;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RomInfo.class.getSimpleName() + "[", "]")
                .add("manufacturer='" + manufacturer + "'")
                .add("year='" + year + "'")
                .add("description='" + description + "'")
                .add("parent='" + parent + "'")
                .add("board='" + board + "'")
                .add("name='" + name + "'")
                .toString();
    }
}
