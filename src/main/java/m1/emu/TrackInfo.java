/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;


import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TrackInfo {

    public static List<TrackInfo> lTI;
    public static TrackInfo CurTrack, StopTrack;
    public final short TrackID;
    public final String Description;
    public final String Duration;
    public final double Second;
    public final String Detail;
    public final String Message;
    public final int IndexList;

    public TrackInfo(short _trackid, String _description, String _duration, String _detail, int _indexlist) {
        TrackID = _trackid;
        Description = _description;
        Duration = _duration;
        Second = CalSecond(Duration);
        Detail = _detail;
        IndexList = _indexlist;
        if (Duration.isEmpty()) {
            Message = Detail;
        } else {
            Message = Detail + " <" + Duration + ">";
        }
    }

    private double CalSecond(String s1) {
        Matcher m1;
        double d1;
        Pattern p = Pattern.compile("(.+):(.+)");
        m1 = p.matcher(s1);
        if (s1.isEmpty()) {
            d1 = 1000;
        } else if (m1.matches()) {
            if (m1.groupCount() == 2) {
                d1 = Double.parseDouble(m1.group(1)) * 60 + Double.parseDouble(m1.group(2));
            } else {
                throw new IllegalArgumentException("unsupported: " + s1);
            }
        } else {
            d1 = Double.parseDouble(s1);
        }
        return d1;
    }

    public static void GetNext() {
        int i1, n1;
        n1 = lTI.size();
        for (i1 = 0; i1 < n1; i1++) {
            if (lTI.get(i1).TrackID == CurTrack.TrackID && lTI.get(i1).IndexList == CurTrack.IndexList) {
                if (i1 < n1 - 1) {
                    CurTrack = lTI.get(i1 + 1);
                    return;
                }
            }
        }
        CurTrack = null;
    }

    public static TrackInfo GetTrackByIndexList(int i1) {
        for (TrackInfo ti : lTI) {
            if (ti.IndexList == i1) {
                return ti;
            }
        }
        return null;
    }

    @Override public String toString() {
        return new StringJoiner(", ", TrackInfo.class.getSimpleName() + "[", "]")
                .add("TrackID=" + TrackID)
                .add("Description='" + Description + "'")
                .add("Duration='" + Duration + "'")
                .add("Second=" + Second)
                .add("Detail='" + Detail + "'")
                .add("Message='" + Message + "'")
                .toString();
    }
}
