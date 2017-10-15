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
import java.io.InputStream;
import java.net.Inet4Address;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import mlga.Boot;
import mlga.io.FileUtil;
import mlga.io.Settings;
import mlga.io.peer.PeerTracker;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;

	private boolean frameMove = false;

	private CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<Peer>();
	private Font roboto;
	/** idx & fh are updated by listener and rendering events. <br>They track hovered index and font height. */
	private int idx = -1, fh = 0;

	private final PeerTracker peerTracker;

	private final JWindow frame;

	public Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FontFormatException, IOException {
		peerTracker = new PeerTracker();
		peerTracker.start();

		InputStream is = FileUtil.localResource("Roboto-Medium.ttf");
		roboto = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(15f);
		is.close();

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		this.setOpaque(false);
		frame = new JWindow();
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setFocusableWindowState(false);

		frame.add(this);
		frame.setAlwaysOnTop(true);
		frame.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) {
					if (e.isShiftDown()) {
						if (idx < 0 || idx >= peers.size() || peers.isEmpty() || e.getX() < 0 || e.getY() < 0)
							return;

						Peer p = peers.get(idx);
						if (!p.saved()) {
							p.rate(true);
						} else if (p.blocked()) {
							p.rate(false);
						} else {
							p.unsave();
						}
					} else if (e.getClickCount() >= 2) {
						frameMove = !frameMove;
						Settings.set("frame_x", frame.getLocationOnScreen().x);
						Settings.set("frame_y", frame.getLocationOnScreen().y);
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
		frame.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (frameMove)
					frame.setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - 6);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				idx = Math.min(peers.size() - 1, (int) Math.floor(e.getY() / (fh)));
			}

		});

		frame.pack();
		frame.setLocation((int) Settings.getDouble("frame_x", 5), (int) Settings.getDouble("frame_y", 400));
		frame.setVisible(true);

		Timer cleanTime = new Timer();
		cleanTime.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				peers.stream().filter(p -> p.age() >= 5000).forEach(p -> {
					Boot.active.remove(p.getID().hashCode());
					peers.remove(p);
				});
			}
		}, 0, 2500);

		Thread t = new Thread("UIPainter") {
			public void run() {
				try {
					while (true) {
						frame.toFront(); //Fix for window sometime hiding behind others
						if (!frameMove) {
							Thread.sleep(400);
						} else {
							Thread.sleep(10);
						}
						Overlay.this.repaint();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private void addPeer(Inet4Address addr, long rtt) {
		peers.add(new Peer(addr, rtt, peerTracker.getPeer(addr)));
	}

	/** Sets a peer's ping, or creates their object. */
	public void setPing(Inet4Address id, long ping) {
		Peer p = this.getPeer(id);
		if (p != null) {
			p.setPing(ping);
		} else {
			this.addPeer(id, ping);
		}
	}

	/** Finds a Peer connection by its ID. */
	private Peer getPeer(Inet4Address id) {
		return peers.stream().filter(p -> p.getID().equals(id)).findFirst().orElse(null);
	}

	/** Dispose this Overlay's Window. */
	public void close() {
		this.frame.dispose();
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
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, getWidth(), getHeight());

		if (!frameMove) {
			g.setColor(new Color(0f, 0f, 0f, .5f));
		} else {
			g.setColor(new Color(0f, 0f, 0f, 1f));
		}

		fh = g.getFontMetrics().getAscent();//line height. Can use getHeight() for more padding between.

		g.fillRect(0, 0, getPreferredSize().width, fh * Math.max(1, peers.size()) + 2);

		if (!peers.isEmpty()) {
			short i = 0;
			for (Peer p : peers) {
				if (idx == i) {
					g.setColor(new Color(0f, 0f, 0f));
					g.fillRect(1, fh * i + 1, getPreferredSize().width, fh + 1);//Pronounce hovered Peer.
				}
				long rtt = p.getPing();
				if (rtt <= 140) {
					g.setColor(Color.GREEN);
				} else if (rtt > 140 && rtt <= 190) {
					g.setColor(Color.YELLOW);
				} else {
					g.setColor(Color.RED);
				}

				String render = "Ping: " + rtt;
				if (p.saved())
					render = (p.blocked() ? "BLOCKED: " : "LOVED: ") + rtt;

				g.drawString(render, 1, fh * (i + 1));
				++i;
			}
		} else {
			g.setColor(Color.RED);
			g.drawString("No Players", 1, fh);
		}

		g.dispose();
	}
}
