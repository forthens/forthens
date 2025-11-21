import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 鸭科夫游戏 - Java实现
 * 使用基本的多边形和色块来表示游戏元素
 */
public class DuckGame extends JPanel implements KeyListener, Runnable {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    
    // 玩家位置和方向
    private double playerX = 50;
    private double playerY = 50;
    private double playerAngle = 0; // 玩家朝向角度
    private double playerSpeed = 0; // 前进/后退速度
    private double rotationSpeed = 0; // 旋转速度
    
    // 游戏对象
    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Wall> walls = new ArrayList<>();
    
    // 控制状态
    private boolean[] keys = new boolean[256];
    private boolean gameRunning = true;
    
    public DuckGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        // 初始化墙壁
        initializeWalls();
        
        // 初始化敌人
        initializeEnemies();
        
        // 启动游戏循环
        new Thread(this).start();
    }
    
    private void initializeWalls() {
        // 创建简单的墙壁布局
        walls.add(new Wall(0, 0, 400, 20)); // 上边界
        walls.add(new Wall(0, 0, 20, 400)); // 左边界
        walls.add(new Wall(380, 0, 20, 400)); // 右边界
        walls.add(new Wall(0, 380, 400, 20)); // 下边界
        
        // 内部墙壁
        walls.add(new Wall(100, 100, 200, 20));
        walls.add(new Wall(200, 200, 20, 100));
        walls.add(new Wall(50, 250, 100, 20));
    }
    
    private void initializeEnemies() {
        // 添加一些敌人
        enemies.add(new Enemy(200, 200, 30, 30, Color.RED));
        enemies.add(new Enemy(300, 150, 30, 30, Color.ORANGE));
        enemies.add(new Enemy(100, 300, 30, 30, Color.MAGENTA));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制墙壁
        g2d.setColor(Color.GRAY);
        for (Wall wall : walls) {
            g2d.fillRect(wall.x, wall.y, wall.width, wall.height);
        }
        
        // 绘制敌人
        for (Enemy enemy : enemies) {
            g2d.setColor(enemy.color);
            g2d.fillRect((int)enemy.x, (int)enemy.y, (int)enemy.width, (int)enemy.height);
            
            // 绘制敌人标签
            g2d.setColor(Color.WHITE);
            g2d.drawString("ENEMY", (int)enemy.x, (int)enemy.y - 5);
        }
        
        // 绘制子弹
        g2d.setColor(Color.YELLOW);
        for (Bullet bullet : bullets) {
            g2d.fillOval((int)bullet.x - 3, (int)bullet.y - 3, 6, 6);
        }
        
        // 绘制玩家（三角形表示方向）
        g2d.setColor(Color.CYAN);
        int[] xPoints = {
            (int)(playerX + Math.cos(playerAngle) * PLAYER_SIZE / 2),
            (int)(playerX + Math.cos(playerAngle + 2.5) * PLAYER_SIZE / 2),
            (int)(playerX + Math.cos(playerAngle - 2.5) * PLAYER_SIZE / 2)
        };
        int[] yPoints = {
            (int)(playerY + Math.sin(playerAngle) * PLAYER_SIZE / 2),
            (int)(playerY + Math.sin(playerAngle + 2.5) * PLAYER_SIZE / 2),
            (int)(playerY + Math.sin(playerAngle - 2.5) * PLAYER_SIZE / 2)
        };
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        // 绘制玩家标签
        g2d.setColor(Color.WHITE);
        g2d.drawString("PLAYER", (int)playerX - 15, (int)playerY - 15);
        
        // 绘制UI信息
        g2d.setColor(Color.WHITE);
        g2d.drawString("WASD: 移动, 空格: 射击, 鼠标: 转向", 10, WINDOW_HEIGHT - 30);
        g2d.drawString("敌人数量: " + enemies.size(), 10, WINDOW_HEIGHT - 10);
    }
    
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60; // 60 FPS
        double delta = 0;
        
        while (gameRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;
            
            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
            
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void update() {
        // 处理玩家移动
        handlePlayerMovement();
        
        // 更新子弹位置
        updateBullets();
        
        // 更新敌人
        updateEnemies();
        
        // 检查碰撞
        checkCollisions();
    }
    
    private void handlePlayerMovement() {
        // 旋转
        playerAngle += rotationSpeed * 0.05;
        
        // 前进/后退
        double newX = playerX + Math.cos(playerAngle) * playerSpeed;
        double newY = playerY + Math.sin(playerAngle) * playerSpeed;
        
        // 简单的碰撞检测（仅检测墙壁）
        boolean canMove = true;
        for (Wall wall : walls) {
            if (newX < wall.x + wall.width && newX + PLAYER_SIZE > wall.x &&
                newY < wall.y + wall.height && newY + PLAYER_SIZE > wall.y) {
                canMove = false;
                break;
            }
        }
        
        if (canMove) {
            playerX = newX;
            playerY = newY;
        }
        
        // 限制玩家在窗口内
        playerX = Math.max(0, Math.min(playerX, 400 - PLAYER_SIZE));
        playerY = Math.max(0, Math.min(playerY, 400 - PLAYER_SIZE));
    }
    
    private void updateBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.x += Math.cos(bullet.angle) * bullet.speed;
            bullet.y += Math.sin(bullet.angle) * bullet.speed;
            
            // 移除超出边界的子弹
            if (bullet.x < 0 || bullet.x > 400 || bullet.y < 0 || bullet.y > 400) {
                bullets.remove(i);
            }
        }
    }
    
    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            // 简单的AI：向玩家移动
            double dx = playerX - enemy.x;
            double dy = playerY - enemy.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0) {
                enemy.x += (dx / distance) * enemy.speed;
                enemy.y += (dy / distance) * enemy.speed;
            }
        }
    }
    
    private void checkCollisions() {
        // 子弹与敌人的碰撞
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                
                double dx = bullet.x - enemy.x;
                double dy = bullet.y - enemy.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < 20) { // 碰撞检测
                    bullets.remove(i);
                    enemies.remove(j);
                    break;
                }
            }
        }
        
        // 玩家与敌人的碰撞
        for (Enemy enemy : enemies) {
            double dx = playerX - enemy.x;
            double dy = playerY - enemy.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < PLAYER_SIZE) {
                // 简单的碰撞处理 - 暂时只是打印信息
                System.out.println("玩家被敌人击中！");
            }
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                playerSpeed = 3;
                break;
            case KeyEvent.VK_S:
                playerSpeed = -3;
                break;
            case KeyEvent.VK_A:
                rotationSpeed = -0.1;
                break;
            case KeyEvent.VK_D:
                rotationSpeed = 0.1;
                break;
            case KeyEvent.VK_SPACE:
                // 发射子弹
                bullets.add(new Bullet(
                    playerX + Math.cos(playerAngle) * PLAYER_SIZE / 2,
                    playerY + Math.sin(playerAngle) * PLAYER_SIZE / 2,
                    playerAngle,
                    7
                ));
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_S:
                playerSpeed = 0;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_D:
                rotationSpeed = 0;
                break;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("鸭科夫游戏");
        DuckGame game = new DuckGame();
        
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}