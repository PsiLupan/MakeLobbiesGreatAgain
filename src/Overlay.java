import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;
	
	private String locale = null;
	private boolean country_name = false;
	private boolean proxy = false;
	private boolean frameMove = false;
	private long rtt = 0;
	
	Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		this.setOpaque(false);

		JWindow frame = new JWindow();
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setFocusableWindowState(false);
		
		frame.add(this);
		frame.setAlwaysOnTop(true);
		frame.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() >= 2){
					frameMove = !frameMove;
					frame.setFocusableWindowState(frameMove);
				}else if(SwingUtilities.isRightMouseButton(e)){
					country_name = !country_name;
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
		frame.setLocation(5, 400);
		frame.setVisible(true);
		
		Thread t = new Thread("UIPainter"){
			public void run() {
				try{
					while(true){
						if(!frameMove){
							Thread.sleep(1000);
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
	
	public boolean useCountryName(){
		return this.country_name;
	}
	
	public void setProxy(boolean proxy){
		this.proxy = proxy;
	}
	
	public void setPing(long rtt){
		this.rtt = rtt;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(112, 46);
	}
	
	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr.create();
		g.setColor(getBackground());
		g.setFont(g.getFont().deriveFont(15f));
		//g.setComposite(AlphaComposite.SrcOver.derive(0f));
		g.setColor(new Color(0,0,0,0));
		g.fillRect(0, 0, getWidth(), getHeight());
		
		if(!frameMove){
			g.setColor(new Color(0f,0f,0f,.5f));
		}else{
			g.setColor(new Color(0f,0f,0f,1f));
		}
		
		if(country_name){
			g.fillRect(0, 0, getPreferredSize().width, getPreferredSize().height);
			if(!proxy){
				g.setColor(Color.GREEN);
			}else{
				g.setColor(Color.RED);
			}
			g.drawString("Killer Country:", 2, 14);
			g.drawString(""+ locale, 2, 28); //"" is used to avoid NPE, it's a hack
			g.drawString("Ping:" + rtt, 2, 42);
		}else{
			g.fillRect(0, 0, getPreferredSize().width, getPreferredSize().height - 14);
			if(!proxy){
				g.setColor(Color.GREEN);
			}else{
				g.setColor(Color.RED);
			}
			g.drawString("Killer Locale: " + locale, 2, 14);
			g.drawString("Ping: "+ rtt, 2, 28);
		}
		
		g.dispose();
	}
}
