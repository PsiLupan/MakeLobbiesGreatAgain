package mlga.ui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

/**
 * A panel for rendering markdown, somewhat faithfully.
 * @author ShadowMoose
 */
public class MarkdownPanel extends JFrame{
	private static final long serialVersionUID = -8603001629015114181L;
	private String html = "";
	
	/**
	 * Creates the new Panel and parses the supplied HTML.  <br>
	 * <b> Supported Github Markdown: </b><i> Lists (unordered), Links, Images, Bold ('**' and '__'), Italics.  </i>
	 * @param newVersion The version of the new update.
	 * @param title The title to display at the top of the document.
	 * @param markup The markup to parse.
	 */
	public MarkdownPanel(double newVersion, String title, String markup){
		html = "<a style='color: #0366d6;text-decoration: none;' href='https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases/latest'><h1>"+title+"</h1></a>";
		boolean list = false;
		for(String s : markup.split("\n")){
			if(s.startsWith("  *")){
				s = "\t"+s.replaceFirst("\\*", "&#9676;")+"<br>";
			}else if(s.startsWith("* ")){
				if(!list){
					s="<ul>"+s;
					list=true;
				}
				s = s.replaceFirst("\\* ", "<li>")+"</li>";
			}else if(list){
				list = false;
				s+="</ul>";
			}
			
			s = parseTag(s, "\\*\\*", "b");
			s = parseTag(s, "__", "b");
			s = parseTag(s, "\\*", "i");
			html+=s.trim()+(!list?"<br>":"");
		}
		html = hyperlinks(html, "\\!\\[(.+?)\\]\\s?+\\((.+?)\\)", "<img src='[2]' alt='[1]'></img>");// Images
		html = hyperlinks(html, "\\[(.+?)\\]\\s?+\\((.+?)\\)", "<a href='[2]'>[1]</a>");// Embedded Links
		
		html += "<br><center><a style='color: #0366d6;' href='https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases/download/"+newVersion+"/MLGA.jar'>Direct Download</a></center>";
		System.out.println(html);
		JEditorPane ed = new JEditorPane("text/html", html);
		ed.setEditable(false);
		ed.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		ed.setFont(new Font("Helvetica", 0, 14));
		
		ed.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent he) {
				// Listen to link clicks and open them in the browser.
				if(he.getEventType() == EventType.ACTIVATED && Desktop.isDesktopSupported()){
					try {
						Desktop.getDesktop().browse(he.getURL().toURI());
						System.exit(0);
					} catch (IOException | URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		final JScrollPane scrollPane = new JScrollPane(ed);
        scrollPane.setPreferredSize(new Dimension(1000, 300));
		
		setTitle("MLGA Update "+newVersion);
		add(scrollPane);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Displays this panel to the user, and hangs until closed.  <br>
	 * Panel will terminate the JVM if the user clicks a link within it.
	 */
	public void prompt(){
		setVisible(true);

		try{
			while(this.isDisplayable())Thread.sleep(200);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/** Replaced the given regex tag with the surrpounding HTML element tags. */
	private String parseTag(String body, String tag, String replace){
		boolean t = false;
		// Uses split just for simple regex support.
		while(body.split(tag).length>1){
			body = body.replaceFirst(tag, "<"+(t?"/":"")+replace+">");
			t=!t;
		}
		return body;
	}
	
	/**
	 * Matches (using regex groups) for pattern in body, then replaces any full match strings with template.  <br>
	 * Template can contain references to group numbers, matched by the regex statement, to be inserted back into the template.  <br>
	 * The groups can be referenced in template via "[group_number]"
	 * @param body The Text to parse.
	 * @param pattern The pattern, and all groupings, to look for using Regex.
	 * @param template The template to swap the regex full match for. Can also contain references to group numbers.
	 * @return The full body, with replacements made. 
	 */
	private String hyperlinks(String body, String pattern, String template){
		Pattern r = Pattern.compile(pattern);

		// Now create matcher object.
		Matcher m = r.matcher(body);

		while(m.find()) {
			String tmp = template;
			for(int i=0; i<=m.groupCount();i++){
				tmp = tmp.replace("["+i+"]", m.group(i));
			}
			body = body.replace(m.group(0), tmp);
		}
		return body;
	}
}
