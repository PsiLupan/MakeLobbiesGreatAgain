package mlga.ui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent.EventType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A panel for rendering Github updates, somewhat faithfully.
 *
 * @author ShadowMoose
 */
public class GithubPanel extends JFrame {
	private static final long serialVersionUID = -8603001629015114181L;
	/**
	 * The project owner, name, and release JAR name, for this Github Repository. Used for lookup.
	 */
	private static final String author = "PsiLupan", project = "MakeLobbiesGreatAgain", directJAR = "MLGA.jar";
	/**
	 * The String flag to be included in the Release Notes Body if the update is mandatory for all below the new version.
	 */
	private static final String mandatory = "Required";
	private String html = "";
	private double version = 0;
	private JEditorPane ed;

	private int updates = 0, required = 0;

	/**
	 * Creates the new Panel and parses the supplied HTML.  <br>
	 * <b> Supported Github Markdown: </b><i> Lists (unordered), Links, Images, Bold ('**' and '__'), Strikethrough, & Italics.  </i>
	 *
	 * @param currentVersion The version of the Jar currently running.
	 */
	public GithubPanel(double currentVersion) {
		this.version = currentVersion;

		setTitle("MLGA Update");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			parseReleases();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (updates <= 0) {
			return;
		}
		ed = new JEditorPane("text/html", html);
		ed.setEditable(false);
		ed.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		ed.setFont(new Font("Helvetica", 0, 12));

		ed.addHyperlinkListener(he -> {
			// Listen to link clicks and open them in the browser.
			if (he.getEventType() == EventType.ACTIVATED && Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(he.getURL().toURI());
					System.exit(0);
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
		});
		final JScrollPane scrollPane = new JScrollPane(ed);
		scrollPane.setPreferredSize(new Dimension(1100, 300));
		add(scrollPane);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Parse the given markup into HTML, and append it to the full html string.
	 *
	 * @param releaseVersion The Version Number, exactly as Github lists it.
	 * @param title          The title to add for this entry.
	 * @param markup         The markup to parse.
	 */
	private void parse(String releaseVersion, String title, String markup) {
		String formatted = "";
		formatted += "<a style='color: #0366d6;text-decoration: none;' href='https://github.com/" + author + "/" + project + "/releases/tag/" + releaseVersion + "'><h1>" + title + "</h1></a>";
		boolean list = false;
		for (String s : markup.split("\n")) {
			if (s.startsWith("  *")) {
				s = "\t" + s.replaceFirst("\\*", "&#9676;") + "<br>";
			} else if (s.startsWith("* ")) {
				if (!list) {
					s = "<ul>" + s;
					list = true;
				}
				s = s.replaceFirst("\\* ", "<li>") + "</li>";
			} else if (list) {
				list = false;
				s += "</ul>";
			}

			formatted += s.trim() + (!list ? "<br>" : "");
		}
		formatted = parseTag(formatted, "\\*\\*", "b");// Bold
		formatted = parseTag(formatted, "__", "b");// Also Bold
		formatted = parseTag(formatted, "\\*", "i");// Italics
		formatted = parseTag(formatted, "~~", "s");// Strikethrough (JEPanel uses HTML 3.2)
		formatted = hyperlinks(formatted, "\\!\\[(.+?)\\]\\s?+\\((.+?)\\)", "<img src='[2]' alt='[1]'></img>");// Images
		formatted = hyperlinks(formatted, "\\[(.+?)\\]\\s?+\\((.+?)\\)", "<a href='[2]'>[1]</a>");// Embedded Links

		formatted += "<br><center><a style='color: #0366d6;' href='https://github.com/" + author + "/" + project + "/releases/download/" + releaseVersion + "/" + directJAR + "'><b>[ Direct Download ]</b></a></center>";
		this.html += formatted;
	}

	/**
	 * If there are updates, displays this panel to the user and hangs until closed.  <br>
	 * Panel will terminate the JVM if the user clicks a link within it.
	 *
	 * @return True if an update is located that is mandatory.
	 */
	public boolean prompt() {
		if (updates <= 0) {
			return true;
		}
		setVisible(true);

		try {
			while (this.isDisplayable()) Thread.sleep(200);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (required > 0) {
			System.err.println("Mandatory updates: " + required);
			return false;
		}
		return true;
	}

	/**
	 * Replaced the given regex tag with the surrpounding HTML element tags.
	 */
	private String parseTag(String body, String tag, String replace) {
		boolean open = false;
		Pattern r = Pattern.compile(tag);
		Matcher m = r.matcher(body);

		while (m.find()) {
			body = body.replaceFirst(tag, "<" + (open ? "/" : "") + replace + ">");
			open = !open;
		}
		if (open) {
			// Uh oh, an unclosed tag.
			body += "</" + replace + ">";
		}
		return body;
	}

	/**
	 * Matches (using regex groups) for pattern in body, then replaces any full match strings with template.  <br>
	 * Template can contain references to group numbers, matched by the regex statement, to be inserted back into the template.  <br>
	 * The groups can be referenced in template via "[group_number]"
	 *
	 * @param body     The Text to parse.
	 * @param pattern  The pattern, and all groupings, to look for using Regex.
	 * @param template The template to swap the regex full match for. Can also contain references to group numbers.
	 *
	 * @return The full body, with replacements made.
	 */
	private String hyperlinks(String body, String pattern, String template) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(body);

		while (m.find()) {
			String tmp = template;
			for (int i = 0; i <= m.groupCount(); i++) {
				tmp = tmp.replace("[" + i + "]", m.group(i));
			}
			body = body.replace(m.group(0), tmp);
		}
		return body;
	}

	/**
	 * Connects to Github to check for project updates.
	 *
	 * @throws IOException
	 */
	private void parseReleases() throws IOException {
		InputStream is = new URL("https://api.github.com/repos/" + author + "/" + project + "/releases").openStream();
		JsonElement ele = new JsonParser().parse(new InputStreamReader(is));
		is.close();

		SimpleDateFormat gdate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		JsonArray arr = ele.getAsJsonArray();
		for (int i = 0; i < arr.size(); i++) {
			JsonObject obj = arr.get(i).getAsJsonObject();
			try {
				String vers = obj.get("tag_name").getAsString();
				if (vers.contains("-")) {
					vers = vers.substring(0, vers.indexOf("-")).trim();
				}
				double nv = Double.parseDouble(vers);
				if (nv <= this.version)
					continue;// Skip older updates.
				System.out.println("Version: " + nv);

				if (i > 0)
					html += "<hr />";

				String body = obj.get("body").getAsString().trim();
				String title = "<b style='color:black;'>" + sdf.format(gdate.parse(obj.get("published_at").getAsString())) + ":</b> " + obj.get("name").getAsString();

				if (body.contains(mandatory)) {
					required++;
					title += "<b style='color: red;'> - (Required Update)</b>";
				}

				parse(obj.get("tag_name").getAsString().trim(), title, body);
				updates++;
			} catch (ClassCastException cce) {
				System.out.println("Ignoring build: " + obj.get("tag_name").getAsString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		setTitle("MLGA Update - " + updates + " releases behind");
	}
}
