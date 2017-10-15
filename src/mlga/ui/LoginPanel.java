package mlga.ui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * For verification, the Kindred System requires an account registered from one of several supported sites.
 * <br>
 * The LoginPanel loads a list from the server, containing the available authentication modules -
 * which are all tied into the Kindred System.
 *
 * @author ShadowMoose
 */
public class LoginPanel extends JFrame {
	private static final long serialVersionUID = 1L;
	private String code = null;
	private final String server = "https://mlga.rofl.wtf/";

	/**
	 * Prepare the panel. Once open, it will prompt the user to choose a method for authentication.
	 */
	public LoginPanel() {
		setTitle("Log in For Database Access");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		JLabel lbl = new JLabel("<html>Select a service to sign in:</html>", SwingConstants.CENTER);
		// ^ Why does this max-width with html tags?
		lbl.setPreferredSize(new Dimension(300, 10));
		lbl.setFont(new Font("Helvetica", 0, 20));

		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		jp.add(lbl);

		try {
			InputStream is = new URL(server + "list.json").openStream();
			JsonElement ele = new JsonParser().parse(new InputStreamReader(is));
			is.close();
			JsonArray arr = ele.getAsJsonArray();
			arr.forEach(e -> {
				JsonObject obj = e.getAsJsonObject();
				String name = obj.get("name").getAsString();
				JButton b = build(name, server + name.toLowerCase() + "/", obj.get("description").getAsString());
				jp.add(b);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		JScrollPane sc = new JScrollPane(jp);
		sc.setPreferredSize(new Dimension(325, 370));
		add(sc);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Display prompt, and hang until this panel is closed. <br>
	 * Use {@link #getCode()} to access the user-entered value.
	 */
	public void prompt() {
		JOptionPane.showMessageDialog(null,
				"Using community-submitted data, MLGA can now track all Players reliably, even through Dynamic IPs and Proxies.\n"
						+ "User's Blocked/Loved settings are never transferred. This system simply assists with identifying the people you personally rate.\n"
						+ "In order to validate submitted data, you may (optionally) access this feature with a one-time account authentication.\n"
						+ "\n"
						+ "It is highly recommended that you use this feature, as it greatly enhances MLGA's ability to track your Loved/Blocked players.\n"
						+ "If you do not wish to use this system, simply close the next window.",
				"Kindred - Optional", JOptionPane.INFORMATION_MESSAGE);
		setVisible(true);
		try {
			while (this.isDisplayable())
				Thread.sleep(200);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Build an image-based button with the given details.
	 *
	 * @param name  Name of this authentication method.
	 * @param base  Base URL of the server for authentication.
	 * @param hover Text to display when hovered over by the mouse.
	 *
	 * @return The completed button, with all event listeners added.
	 */
	private JButton build(String name, final String base, String hover) {
		URL url = null;
		Image im = null;
		try {
			url = new URL(base + "logo.png");
			im = ImageIO.read(url);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		JButton ll = new JButton("");
		double width = 300;
		double ratio = (double) im.getWidth(null) / width;

		int nw = (int) ((double) im.getWidth(null) / ratio);
		int nh = (int) ((double) im.getHeight(null) / ratio);
		ll.setIcon(new ImageIcon(im.getScaledInstance(nw, nh, Image.SCALE_SMOOTH)));
		ll.setPreferredSize(new Dimension(nw, nh));
		ll.setFocusable(false);
		ll.setToolTipText(hover);
		ll.addActionListener(ae -> {
			try {
				Desktop.getDesktop().browse(new URL(base + "request").toURI());
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
			String c = JOptionPane.showInputDialog(LoginPanel.this, "Enter the code you've been given: ");
			if (c != null) {
				LoginPanel.this.code = c;
				LoginPanel.this.dispose();
			}
		});
		return ll;
	}

	/**
	 * Get the auth code from this panel.
	 */
	public String getCode() {
		if (this.code == null || this.code.trim().equals(""))
			return null;
		return this.code;
	}
}
