using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace mame
{
    public class TrackInfo
    {
        public static List<TrackInfo> lTI;
        public static TrackInfo CurTrack, StopTrack;
        public ushort TrackID;
        public string Description;
        public string Duration;
        public double Second;
        public string Detail;
        public string Message;
        public int IndexList;
        public TrackInfo(ushort _trackid, string _description, string _duration, string _detail, int _indexlist)
        {
            TrackID = _trackid;
            Description = _description;
            Duration = _duration;
            Second = CalSecond(Duration);
            Detail = _detail;
            IndexList = _indexlist;
            if (Duration == "")
            {
                Message = Detail;
            }
            else
            {
                Message = Detail + " <" + Duration + ">";
            }
        }
        private double CalSecond(string s1)
        {
            Match m1;
            double d1;
            m1 = Regex.Match(s1, "(.+):(.+)");
            if (s1 == "")
            {
                d1 = 1000;
            }
            else if (m1.Groups.Count == 3)
            {
                d1 = double.Parse(m1.Groups[1].ToString()) * 60 + double.Parse(m1.Groups[2].ToString());
            }
            else
            {
                d1 = double.Parse(s1);
            }
            return d1;
        }
        public static void GetNext()
        {
            int i1, n1;
            n1 = lTI.Count;
            for (i1 = 0; i1 < n1; i1++)
            {
                if (lTI[i1].TrackID == CurTrack.TrackID && lTI[i1].IndexList == CurTrack.IndexList)
                {
                    if (i1 < n1 - 1)
                    {
                        CurTrack = lTI[i1 + 1];
                        return;
                    }
                }
            }
            CurTrack = null;
        }
        public static TrackInfo GetTrackByIndexList(int i1)
        {
            foreach (TrackInfo ti in lTI)
            {
                if (ti.IndexList == i1)
                {
                    return ti;
                }
            }
            return null;
        }
    }
}