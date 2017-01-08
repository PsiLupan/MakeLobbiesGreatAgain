import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;

	private String locale = "N/A";
	private boolean country_name = false;
	private boolean proxy = false;
	private boolean frameMove = false;
	private boolean mode = false; //true for killer, false for surv
	private long[] rtt = new long[4];
	private final Font roboto = Font.createFont(Font.TRUETYPE_FONT, ClassLoader.getSystemClassLoader().getResourceAsStream("resources/Roboto-Medium.ttf")).deriveFont(15f);;

	Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FontFormatException, IOException{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		this.setOpaque(false);
		country_name = (Settings.getDouble("country_name", 0) == 1);
		final JWindow frame = new JWindow();
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setFocusableWindowState(false);

		frame.add(this);
		frame.setAlwaysOnTop(true);
		frame.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() >= 2){
					frameMove = !frameMove;
					Settings.set("frame_x", frame.getLocationOnScreen().x);
					Settings.set("frame_y", frame.getLocationOnScreen().y);
					frame.setFocusableWindowState(frameMove);
				}else if(SwingUtilities.isRightMouseButton(e) && !e.isShiftDown()){
					country_name = !country_name;
					Settings.set("country_name", country_name ? 1:0);
				}else if(SwingUtilities.isRightMouseButton(e) && e.isShiftDown()){
					mode = !mode;
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		frame.addMouseMotionListener(new MouseMotionListener(){

			@Override
			public void mouseDragged(MouseEvent e) {
				if(frameMove){
					frame.setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - (getPreferredSize().height / 2));
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
			}

		});

		frame.pack();
		frame.setLocation((int)Settings.getDouble("frame_x", 5), (int)Settings.getDouble("frame_y", 400));
		frame.setVisible(true);

		Thread t = new Thread("UIPainter"){
			public void run() {
				try{
					while(true){
						if(!frameMove){
							Thread.sleep(1000);
						}else{
							Thread.sleep(10);
						}
						Overlay.this.repaint();
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	public void setKillerLocale(String locale){
		this.locale = locale;
	}

	private boolean useCountryName(){
		return this.country_name;
	}

	private void setProxy(boolean proxy){
		this.proxy = proxy;
	}
	
	public boolean getMode(){
		return this.mode;
	}

	public void setKillerPing(long rtt){
		this.rtt[0] = rtt;
	}

	public void setSurvPing(long rtt, short slot){
		this.rtt[slot] = rtt;
	}

	public void geolocate(String ip) throws IOException, ParseException{
		String code = null;
		Long proxy = 0L;

		try (InputStream is = new URL("http://legacy.iphub.info/api.php?showtype=4&ip=" + ip.replace("/", "")).openStream();
				BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(buf);
			if(useCountryName()){
				code = (String)obj.get("countryName");
			}else{
				code = (String)obj.get("countryCode");
			}
			proxy = (Long)obj.get("proxy");
		}
		setKillerLocale(code);
		if(proxy != 0L){ //Could abuse anything non-zero being true, but probably shouldn't.
			setProxy(true);
		}else{
			setProxy(false);
		}

	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(118, 58);
	}

	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr.create();
		g.setColor(getBackground());
		g.setFont(roboto);
		//g.setComposite(AlphaComposite.SrcOver.derive(0f));
		g.setColor(new Color(0,0,0,0));
		g.fillRect(0, 0, getWidth(), getHeight());

		if(!frameMove){
			g.setColor(new Color(0f,0f,0f,.5f));
		}else{
			g.setColor(new Color(0f,0f,0f,1f));
		}

		if(!mode){
			if(country_name){
				g.fillRect(0, 0, getPreferredSize().width, getPreferredSize().height - 12);
				if(!proxy){
					g.setColor(Color.WHITE);
				}else{
					g.setColor(Color.RED);
				}
				g.drawString("Host Country:", 1, 13);
				g.drawString(""+ locale, 1, 27); //"" is used to avoid NPE, it's a hack

				if(rtt[0] <= 120){
					g.setColor(Color.GREEN);
				}else if(rtt[0] > 120 && rtt[0] <= 150){
					g.setColor(Color.YELLOW);
				}else{
					g.setColor(Color.RED);
				}
				g.drawString("Ping:" + rtt[0], 1, 42);
			}else{
				g.fillRect(0, 0, getPreferredSize().width, getPreferredSize().height - 26);
				if(!proxy){
					g.setColor(Color.WHITE);
				}else{
					g.setColor(Color.RED);
				}
				g.drawString("Host Locale: " + locale, 1, 13);

				if(rtt[0] <= 120){
					g.setColor(Color.GREEN);
				}else if(rtt[0] > 120 && rtt[0] <= 150){
					g.setColor(Color.YELLOW);
				}else{
					g.setColor(Color.RED);
				}
				g.drawString("Ping: "+ rtt[0], 1, 27);
			}
		}else{
			g.fillRect(55, 0, getPreferredSize().width, getPreferredSize().height);
			for(short i = 0; i < 4; i++){
				if(rtt[i] <= 120){
					g.setColor(Color.GREEN);
				}else if(rtt[i] > 120 && rtt[i] <= 150){
					g.setColor(Color.YELLOW);
				}else{
					g.setColor(Color.RED);
				}
				g.drawString("Ping: "+ rtt[i], 56, 13 * (i + 1));
			}
		}

		g.dispose();
	}
}
