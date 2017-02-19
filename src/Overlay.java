import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;

	private boolean frameMove = false;
	private boolean mode = false; //true for killer, false for surv
	private long killerPing = 0;
	private HashMap<String, Long> survivors = new HashMap<String, Long>();
	private final Font roboto = Font.createFont(Font.TRUETYPE_FONT, ClassLoader.getSystemClassLoader().getResourceAsStream("resources/Roboto-Medium.ttf")).deriveFont(15f);;

	Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FontFormatException, IOException{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		this.setOpaque(false);
		final JWindow frame = new JWindow();
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setFocusableWindowState(false);

		frame.add(this);
		frame.setAlwaysOnTop(true);
		frame.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() >= 2 && !SwingUtilities.isRightMouseButton(e)){
					frameMove = !frameMove;
					Settings.set("frame_x", frame.getLocationOnScreen().x);
					Settings.set("frame_y", frame.getLocationOnScreen().y);
					frame.setFocusableWindowState(frameMove);
				}else if(SwingUtilities.isRightMouseButton(e)){
					killerPing = 0;
					if(!survivors.isEmpty())
						survivors.clear();
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

	public boolean getMode(){
		return mode;
	}

	public void setKillerPing(long rtt){
		killerPing = rtt;
	}

	public void setSurvPing(String key, long rtt){
		survivors.put(key, rtt);
	}

	public void removeSurv(String key){
		survivors.remove(key);
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
			g.fillRect(8, 0, getPreferredSize().width, getPreferredSize().height - 42);
			if(killerPing <= 120){
				g.setColor(Color.GREEN);
			}else if(killerPing > 120 && killerPing <= 150){
				g.setColor(Color.YELLOW);
			}else{
				g.setColor(Color.RED);
			}
			g.drawString("Killer Ping: "+ killerPing, 9, 13);
		}else{
			g.fillRect(35, 0, getPreferredSize().width, getPreferredSize().height);
			if(!survivors.isEmpty()){
				Iterator<Long> iter = survivors.values().iterator();
				short i = 0;
				while(iter.hasNext()){
					long rtt = iter.next();
					if(rtt <= 120){
						g.setColor(Color.GREEN);
					}else if(rtt > 120 && rtt <= 150){
						g.setColor(Color.YELLOW);
					}else{
						g.setColor(Color.RED);
					}
					g.drawString("Ping: "+ rtt, 36, 13 * (i + 1));
					++i;
				}
			}else{
				g.setColor(Color.RED);
				g.drawString("NO", 36, 13);
				g.drawString("SURVIVORS", 36, 26);
				g.drawString("FOUND IN", 36, 39);
				g.drawString("LOBBY", 36, 52);
			}
		}

		g.dispose();
	}
}
