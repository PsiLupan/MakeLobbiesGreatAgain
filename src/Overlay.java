import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;
	
	private String locale = null;
	private boolean paint = true;
	
	Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		this.setOpaque(false);

		JFrame frame = new JFrame("DBD Overlay");
		frame.setUndecorated(true);
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setFocusableWindowState(false);
		
		frame.add(this);
		frame.setAlwaysOnTop(true);
		frame.pack();
		frame.setLocation(5, 400);
		frame.setVisible(true);
		
		Thread t = new Thread("UIPainter"){
			public void run() {
				try{
					while(true){
						Thread.sleep(50);
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
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(120, 100);
	}
	
	@Override
	protected void paintComponent(Graphics gr) {
		if(!paint){
			return;
		}
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr.create();
		g.setColor(getBackground());
		g.setFont(g.getFont().deriveFont(15f));
		//g.setComposite(AlphaComposite.SrcOver.derive(0f));
		g.setColor(new Color(0,0,0,0));
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(new Color(0f,0f,0f,.5f));
		g.fillRect(8, 15, 120, 24);
		
		g.setColor(Color.YELLOW);
		g.drawString("Killer Locale: " + locale, 10, 32);
		
		g.dispose();
	}
}
