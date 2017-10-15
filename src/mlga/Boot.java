package mlga;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import mlga.io.Backup;
import mlga.io.FileUtil;
import mlga.io.Settings;

public class Boot {
    private static boolean running = true;

    public static void main(String[] args) throws AWTException {
        System.setProperty("jna.nosys", "true");
        if (!Sanity.check()) {
            System.exit(1);
        }
        Settings.init();
        setupTray();

        if (Backup.enabled()) {
            Backup.startBackupDaemon();
        }

        while (running) {
        }
    }

    public static void setupTray() throws AWTException {
        final SystemTray tray = SystemTray.getSystemTray();
        final PopupMenu popup = new PopupMenu();
        final MenuItem exit = new MenuItem();
        final TrayIcon trayIcon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "MLGA", popup);
        try {
            InputStream is = FileUtil.localResource("icon.png");
            trayIcon.setImage(ImageIO.read(is));
            is.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        exit.addActionListener(e -> {
            running = false;
            tray.remove(trayIcon);
            System.exit(0);
        });
        exit.setLabel("Exit");
        popup.add(exit);
        tray.add(trayIcon);
    }
}
