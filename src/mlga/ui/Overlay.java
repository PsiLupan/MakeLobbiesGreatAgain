package mlga.ui;

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
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


import mlga.io.Preferences;
import mlga.io.Settings;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;

	private boolean frameMove = false;
	/** False for killer, True for surv*/
	private boolean mode = false; 

	private CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<Peer>();
	private final Font roboto = Font.createFont(Font.TRUETYPE_FONT, ClassLoader.getSystemClassLoader().getResourceAsStream("resources/Roboto-Medium.ttf")).deriveFont(15f);
	/** idx & fh are updated by listener and rendering events. <br>They track hovered index and font height.*/
	private int idx = -1, fh = 0;

	public Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FontFormatException, IOException{
		Preferences.init();

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
				if(!SwingUtilities.isRightMouseButton(e)){
					if(e.isShiftDown()){
						if(idx < 0||idx >= peers.size() || peers.size() < 1 || e.getX() < 0 || e.getY() < 0){
							return;
						}
						Peer p = peers.get(idx);
						if(!p.saved()){
							p.save(true);
						}else if(p.blocked()){
							p.save(false);
						}else{
							p.unsave();
						}
					}
				}
				if(e.getClickCount() >= 2 && !SwingUtilities.isRightMouseButton(e) && !e.isShiftDown()){
					frameMove = !frameMove;
					Settings.set("frame_x", frame.getLocationOnScreen().x);
					Settings.set("frame_y", frame.getLocationOnScreen().y);
					frame.setFocusableWindowState(frameMove);
				}else if(SwingUtilities.isRightMouseButton(e)){
					if(!e.isShiftDown()){ //Change between Killer/Survivor Mode
						if(!peers.isEmpty())
							peers.clear();
						mode = !mode;
					}
					try {
						Thread.sleep(200);
						frame.repaint();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
				idx = -1;
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
					frame.setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - 6);
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				idx = Math.min(peers.size()-1, (int) Math.floor(e.getY()/(fh) ) );
			}

		});

		frame.pack();
		frame.setLocation((int)Settings.getDouble("frame_x", 5), (int)Settings.getDouble("frame_y", 400));
		frame.setVisible(true);

		Thread t = new Thread("UIPainter"){
			public void run() {
				try{
					while(true){
						frame.toFront(); //Fix for window sometime hiding behind others
						if(!frameMove){
							Thread.sleep(100);
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

	private void addPeer(int srcAddrHash, long rtt){
		peers.add(new Peer(srcAddrHash, rtt));
	}

	/** Sets a peer's ping, or creates their object. */
	public void setPing(int id, long ping){
		Peer p = this.getPeer(id);
		if(p != null){
			p.setPing(ping);
		}else{
			this.addPeer(id, ping);
		}
	}

	public int numPeers(){
		return peers.size();
	}

	public void clearPeers(){
		peers.clear();
	}

	public void removePeer(int i){
		Peer p = this.getPeer(i);
		if(p != null){
			peers.remove(p);
		}
	}

	/** Finds a Peer connection by its ID. */
	private Peer getPeer(int id){
		for(Peer p : peers){
			if(p.getID() == id){
				return p;
			}
		}
		return null;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(110, 100);
	}

	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr.create();
		g.setColor(getBackground());
		g.setFont(roboto);
		g.setColor(new Color(0,0,0,0));
		g.fillRect(0, 0, getWidth(), getHeight());

		if(!frameMove){
			g.setColor(new Color(0f,0f,0f,.5f));
		}else{
			g.setColor(new Color(0f,0f,0f,1f));
		}

		fh = g.getFontMetrics().getAscent();//line height. Can use getHeight() for more padding between.

		g.fillRect(0, 0, getPreferredSize().width, fh*Math.max(1, peers.size())+2 );

		if(!peers.isEmpty()){
			short i = 0;
			for(Peer p : peers){
				if(idx == i){
					g.setColor(new Color(0f,0f,0f));
					g.fillRect(1, fh*i+1, getPreferredSize().width, fh+1);//Pronounce hovered Peer.
				}
				long rtt = p.getPing();
				if(rtt <= 140){
					g.setColor(Color.GREEN);
				}else if(rtt > 140 && rtt <= 190){
					g.setColor(Color.YELLOW);
				}else{
					g.setColor(Color.RED);
				}

				String render = (mode ? "Survivor":"Killer") + ": "+ rtt;
				if(p.saved()){
					render = (p.blocked() ? "BLOCKED: ":"LOVED: ") + rtt;
				}
				g.drawString(render, 1, fh*(i+1));
				++i;
			}
		}else{
			g.setColor(Color.RED);
			g.drawString("No " + (mode ? "Survivors":"Killer"), 1, fh);
		}

		g.dispose();
	}
}
