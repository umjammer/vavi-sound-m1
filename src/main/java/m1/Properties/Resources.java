/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.Properties;

import java.awt.image.BufferedImage;
import java.util.ResourceBundle;


public class Resources {

    private static ResourceBundle resourceMan;

    public static final byte[] _000_lo = (byte[]) resourceMan.getObject("_000_lo");

    public static BufferedImage _1 = (BufferedImage) resourceMan.getObject("_1");

    public static String m1 = resourceMan.getString("m1");

    public static final byte[] mainbios = (byte[]) resourceMan.getObject("mainbios");

    public static final byte[] mcu = (byte[]) resourceMan.getObject("mcu");

    static byte[] pgmaudiobios = (byte[]) resourceMan.getObject("pgmaudiobios");

    static byte[] pgmmainbios = (byte[]) resourceMan.getObject("pgmmainbios");

    static byte[] pgmvideobios = (byte[]) resourceMan.getObject("pgmvideobios");

    public static final String readme = resourceMan.getString("readme");

    public static final byte[] sfix = (byte[]) resourceMan.getObject("sfix");

    public static final byte[] sm1 = (byte[]) resourceMan.getObject("sm1");
}
