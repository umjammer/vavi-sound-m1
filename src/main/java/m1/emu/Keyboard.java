/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.emu;


import m1.ui.MainForm;


public class Keyboard {

    public static boolean bF10;

    public static void InitializeInput(MainForm form1) {
    }

    static class KeyState {

        public boolean isPressed;
        public boolean isTriggered;
        public boolean wasPressed;
    }

    private static final KeyState[] keyStates = new KeyState[256];

    static {
        for (int i = 0; i < keyStates.length; i++)
            keyStates[i] = new KeyState();
    }

    public static boolean IsPressed(int key) {
        return keyStates[key].isPressed;
    }

    public static boolean IsTriggered(int key) {
        return keyStates[key].isTriggered;
    }

    public static void Update() {
        for (int i = 0; i < 256; i++) {
            keyStates[i].isPressed = false;
        }
        for (int key : new int[] {}) { // TODO
            keyStates[key].isPressed = true;
        }
        for (int i = 0; i < 256; i++) {
            if (keyStates[i].isPressed) {
                if (keyStates[i].wasPressed) {
                    keyStates[i].isTriggered = false;
                } else {
                    keyStates[i].wasPressed = true;
                    keyStates[i].isTriggered = true;
                }
            } else {
                keyStates[i].wasPressed = false;
                keyStates[i].isTriggered = false;
            }
        }
    }
}
