import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class Game extends JComponent implements KeyListener, Runnable {
	private static Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	private static JFrame frame = new JFrame("Game");
	
	private Ball ball;
	private Enemy enemy;
	private ArrayList<Projectile> projectiles;
	private ArrayList<Projectile> bullets;
	
	private int lives;
	private Rectangle healthFrame;
	private int health;
	private Rectangle healthBar;
	private int shieldTimer;
	private boolean playerWon;
	
	private boolean[] keyState = new boolean[256];
	
	public Game() {
		ball = new Ball(200, screen.height / 2 - 50, 100, 100);
		enemy = new Enemy(screen.width - 50, screen.height / 2 - 100);
		projectiles = new ArrayList<Projectile>();
		bullets = new ArrayList<Projectile>();
		lives = 3;
		healthFrame = new Rectangle(screen.width - 1200, 30, 1000, 50);
		health = 100;
		shieldTimer = 0;
		
		this.setFocusable(true);
		this.addKeyListener(this);
	}
	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		if (shieldTimer > 0) {
			g2.setColor(Color.GRAY);
		} else {
			g2.setColor(Color.BLUE);
		}
		g2.fill(ball);
		g2.draw(ball);
		
		g2.setColor(Color.GREEN);
		g2.fill(enemy);
		g2.draw(enemy);
		
		g2.setColor(Color.RED);
		synchronized (projectiles) {
			for (Projectile p : projectiles) {
				g2.fill(p);
				g2.draw(p);
			}
		}
		
		g2.setColor(Color.BLUE);
		synchronized (bullets) {
			for (Projectile b : bullets) {
				g2.fill(b);
				g2.draw(b);
			}
		}
		
		Rectangle container = new Rectangle(0, 0, screen.width, 110);
		g2.setColor(Color.WHITE);
		g2.fill(container);
		g2.draw(container);
		
		if (lives == 3)
			g2.setColor(Color.GREEN);
		else if (lives == 2)
			g2.setColor(Color.YELLOW);
		else
			g2.setColor(Color.RED);
		
		g2.setFont(new Font("Calibri", Font.PLAIN, 50));
		g2.drawString("Lives: " + lives, 40, 70);
		
		healthBar = new Rectangle(screen.width - 1200, 30, health * 10, 50);
		g2.setColor(Color.RED);
		g2.fill(healthBar);
		g2.draw(healthBar);
		
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(5));
		g2.draw(healthFrame);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		// Do Nothing
	}

	@Override
	public void keyPressed(KeyEvent e) {
		keyState[e.getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		keyState[e.getKeyCode()] = false;
	}
	
	@Override
	public void run() {
		for (int i = 0;;i++) {
			if (keyState[KeyEvent.VK_RIGHT] || keyState[KeyEvent.VK_D]) {
				if (ball.x + 125 < screen.width)
					ball.x += 15;
			} if (keyState[KeyEvent.VK_LEFT] || keyState[KeyEvent.VK_A]) {
				if (ball.x > 15)
					ball.x -= 15;
			} if (keyState[KeyEvent.VK_UP] ||  keyState[KeyEvent.VK_W]) {
				if (ball.y > 15 + 115)
					ball.y -= 15;
			} if (keyState[KeyEvent.VK_DOWN] || keyState[KeyEvent.VK_S]) {
				if (ball.y + 175 < screen.height)
					ball.y += 15;
			}
			repaint();
						
			if (i > 100) {
				enemy.activated = true;
			}
			
			if (enemy.activated) {
				if(enemy.y <= 110 || enemy.y >= screen.height - 250) {
					enemy.direction *= -1;
				}
				enemy.y += enemy.direction * 10;
				
				synchronized (projectiles) {
					if (i % 5 == 0) {
						projectiles.add(new Projectile(enemy.x, enemy.y + 100, 60, 10));
						double rand = Math.random();
						int projDirection = 0;
						if (rand  > .50) {
							projDirection = 1;
						} else {
							projDirection = -1;
						}
						
						projectiles.get(projectiles.size()-1).launch(-10 -(int)(Math.random() * 20), projDirection*(int)(Math.random() * 20));
					}
					
					for (int j = 0; j < projectiles.size(); j++) {
						if (projectiles.get(j).x < 0 || projectiles.get(j).y < 0 || projectiles.get(j).y > screen.height) {
							projectiles.remove(projectiles.get(j));
							j--;
							continue;
						}
						projectiles.get(j).move();
					}
					repaint();
				}
			}
			
			
			synchronized(bullets) {
				if (keyState[KeyEvent.VK_SPACE]) {
					if (i % 7 == 0) { 
						bullets.add(new Projectile(ball.x + 200, ball.y + 50, 20, 20));
						bullets.get(bullets.size() - 1).launch(30, 0);
					}
				}
				
				for (int j = 0; j < bullets.size(); j++) {
					if (bullets.get(j).x > screen.width) {
						bullets.remove(bullets.get(j));
						j--;
						continue;
					}
					bullets.get(j).move();
				}
			}
			
			detectHit();
			
			if(shieldTimer > 0) {
				shieldTimer--;
			}
			
			if(shieldTimer <= 0 && detectCollision()) {
				shieldTimer = 100;
				lives--;
			}
			
			repaint();
			
			if (lives == 0) {
				break;
			}
			
			if (health == 0) {
				playerWon = true;
				break;
			}
			
			try {
				Thread.sleep(15);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			repaint();
		}
		
		int pause;
		if (playerWon) {
			pause = JOptionPane.showConfirmDialog(frame,"You win! Play Again?","YOU WIN",JOptionPane.YES_NO_OPTION);
		} else {
			pause = JOptionPane.showConfirmDialog(frame,"Game Over! Play Again?","GAME OVER",JOptionPane.YES_NO_OPTION);
		}
		
		if (pause == JOptionPane.YES_OPTION) {
			for (int i = 0 ; i < keyState.length ; i++) {
				keyState[i] = false;
			}
			
			reset();
			run();
		} else {
			System.exit(0);
		}
	}
	
	public void reset() {
		ball = new Ball(200, screen.height / 2 - 50, 100, 100);
		enemy = new Enemy(screen.width - 50, screen.height / 2 - 100);
		projectiles = new ArrayList<Projectile>();
		bullets = new ArrayList<Projectile>();
		lives = 3;
		healthFrame = new Rectangle(screen.width - 1200, 30, 1000, 50);
		health = 100;
		shieldTimer = 0;
	}
	
	public void detectHit() {
		for (Projectile b: bullets) {
			if (b.intersects(enemy)) {
					health--;
			}
		}
	}
	
	public synchronized boolean detectCollision() {
		for (int j = 0; j < projectiles.size(); j++) {
			if (projectiles.get(j).intersects(ball.getBounds2D())) {
				projectiles.remove(projectiles.get(j));
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		frame.setSize(screen.width, screen.height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Game game = new Game();
		
		frame.add(game, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.setResizable(false);
		
		Thread t = new Thread(game);
		t.start();
	}
}

@SuppressWarnings("serial")
class Ball extends Ellipse2D.Double {	
	public Ball(double x, double y, double w, double h) {
		super(x, y, w, h);
	}
}

@SuppressWarnings("serial")
class Projectile extends Ellipse2D.Double {
	public double vx;
	public double vy;
	
	public Projectile(double x, double y, double w, double h) {
		super(x, y, w, h);
	}
	
	public void launch(double vx, double vy) {
		this.vx = vx;
		this.vy = vy;
	}
	
	public void move() {
		this.x += vx;
		this.y += vy;
	}
}

@SuppressWarnings("serial")
class Enemy extends Rectangle {
	public int direction = -1;
	public boolean activated;
	
	public Enemy(int x, int y) {
		super(x, y, 50, 200);
	}
}