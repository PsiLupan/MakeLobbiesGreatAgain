package mlga;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

public class Sanity {
    private static boolean headless = false;

    public static boolean check() {
        boolean[] checks = {checkGraphics(), checkJava()};

        for (boolean check : checks) {
            if (!check)
                return false;
        }
        return true;
    }

    /**
     * Check for a valid graphical environment.
     */
    private static boolean checkGraphics() {
        if (GraphicsEnvironment.isHeadless()) {
            headless = true;
            message("This program requires a graphical environment to run!\nIt's weird that you even got this far.");
            return false;
        }
        return true;
    }

    /**
     * Check the current Java Version.
     */
    private static boolean checkJava() {
        String v = System.getProperty("java.version");
        System.out.println("Java Version: " + v);
        if (!v.equals("9")) {
            double version = Double.parseDouble(v.substring(0, v.indexOf('.', 2)));
            if (version < 1.8) {
                message("Java version 1.8 or higher is required!\nYou are currently using " + version + "!\n");
                return false;
            }
        }
        return true;
    }

    private static void message(String out) {
        System.err.println(out);
        if (!headless)
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
