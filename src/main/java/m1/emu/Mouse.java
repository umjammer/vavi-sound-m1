/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;

import java.awt.Point;
import java.awt.PointerInfo;
import javax.swing.JFrame;


public class Mouse {

    public static PointerInfo mouseDevice;
    public static int deltaX, deltaY, oldX, oldY;
    public static byte[] buttons;

    public static void InitialMouse(JFrame form1) {
    }

    public static void Update() {
        Point mouseState = mouseDevice.getLocation();
        deltaX = mouseState.x - oldX;
        deltaY = mouseState.y - oldY;
        oldX = mouseState.x;
        oldY = mouseState.y;
//        buttons = mouseState.GetMouseButtons();
    }
}
