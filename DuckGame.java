import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 鸭科夫游戏 - Java实现
 * 使用基本的多边形和色块来表示游戏元素
 */
public class DuckGame extends JPanel implements KeyListener, MouseListener, MouseMotionListener, Runnable {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    
    // 玩家位置和方向
    private double playerX = 50;
    private double playerY = 50;
    private double playerAngle = 0; // 玩家朝向角度
    private double playerSpeed = 0; // 前进/后退速度
    private double rotationSpeed = 0; // 旋转速度
    
    // 鼠标位置
    private int mouseX = 0;
    private int mouseY = 0;
    private boolean mousePressed = false;
    
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
        addMouseListener(this);
        addMouseMotionListener(this);
        
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
        
        // 绘制2.5D视角的墙壁和环境
        draw25DView(g2d);
        
        // 绘制2D小地图
        drawMinimap(g2d);
    }
    
    private void draw25DView(Graphics2D g2d) {
        // 实现射线投射算法来创建2.5D视角
        int screenWidth = WINDOW_WIDTH;
        int screenHeight = WINDOW_HEIGHT;
        
        // 从玩家位置向各个方向投射射线
        for (int x = 0; x < screenWidth; x++) {
            // 计算射线的角度（考虑视角）
            double rayAngle = playerAngle - Math.PI / 4 + (Math.PI / 2) * x / screenWidth;
            
            // 射线的方向向量
            double rayDirX = Math.cos(rayAngle);
            double rayDirY = Math.sin(rayAngle);
            
            // 玩家所在的格子
            int mapX = (int) playerX / 40; // 假设每个格子是40像素
            int mapY = (int) playerY / 40;
            
            // 射线的长度
            double sideDistX, sideDistY;
            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistY = Math.abs(1 / rayDirY);
            double perpWallDist;
            
            // 步长
            int stepX, stepY;
            boolean hit = false;
            int side = 0; // 0表示东西方向，1表示南北方向
            
            // 计算步长和初始边距
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (playerX - mapX * 40) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = ((mapX + 1) * 40 - playerX) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (playerY - mapY * 40) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = ((mapY + 1) * 40 - playerY) * deltaDistY;
            }
            
            // DDA算法
            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                
                // 检查是否碰到墙壁
                if (isWallAt(mapX * 40, mapY * 40)) {
                    hit = true;
                }
            }
            
            // 计算墙壁的高度
            if (side == 0) {
                perpWallDist = (mapX * 40 - playerX + (1 - stepX) / 2) / rayDirX;
            } else {
                perpWallDist = (mapY * 40 - playerY + (1 - stepY) / 2) / rayDirY;
            }
            
            // 计算墙壁在屏幕上的高度
            int lineHeight = (int) (screenHeight / perpWallDist);
            
            // 计算绘制的起始和结束位置
            int drawStart = -lineHeight / 2 + screenHeight / 2;
            if (drawStart < 0) drawStart = 0;
            int drawEnd = lineHeight / 2 + screenHeight / 2;
            if (drawEnd >= screenHeight) drawEnd = screenHeight - 1;
            
            // 根据距离调整颜色亮度
            float brightness = (float) (1.0 / (1.0 + 0.001 * perpWallDist + 0.0001 * perpWallDist * perpWallDist));
            brightness = Math.max(0.2f, brightness); // 确保最小亮度
            
            Color wallColor = side == 0 ? Color.RED : Color.BLUE; // 不同方向的墙壁使用不同颜色
            Color shadedColor = new Color(
                (int) (wallColor.getRed() * brightness),
                (int) (wallColor.getGreen() * brightness),
                (int) (wallColor.getBlue() * brightness)
            );
            
            // 绘制墙壁线
            g2d.setColor(shadedColor);
            g2d.drawLine(x, drawStart, x, drawEnd);
        }
        
        // 绘制敌人（以2.5D视角）
        for (Enemy enemy : enemies) {
            // 计算敌人相对于玩家的位置
            double relX = enemy.x - playerX;
            double relY = enemy.y - playerY;
            
            // 将世界坐标转换为玩家的局部坐标系
            double rotatedX = relX * Math.cos(-playerAngle) - relY * Math.sin(-playerAngle);
            double rotatedY = relX * Math.sin(-playerAngle) + relY * Math.cos(-playerAngle);
            
            // 如果敌人在前方
            if (rotatedY > 0) {
                // 计算敌人在屏幕上的位置
                int screenX = (int) (screenWidth / 2 * (1 + rotatedX / rotatedY * 0.5));
                int size = (int) (screenHeight / rotatedY * 30); // 远小近大
                
                if (screenX >= -size && screenX < screenWidth + size) {
                    // 绘制敌人
                    g2d.setColor(enemy.color);
                    g2d.fillRect(screenX - size/2, (int) (screenHeight/2 - size/2), size, size);
                    
                    // 绘制敌人标签
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("ENEMY", screenX - 20, (int) (screenHeight/2 - size/2 - 5));
                }
            }
        }
        
        // 绘制子弹（以2.5D视角）
        for (Bullet bullet : bullets) {
            // 计算子弹相对于玩家的位置
            double relX = bullet.x - playerX;
            double relY = bullet.y - playerY;
            
            // 将世界坐标转换为玩家的局部坐标系
            double rotatedX = relX * Math.cos(-playerAngle) - relY * Math.sin(-playerAngle);
            double rotatedY = relX * Math.sin(-playerAngle) + relY * Math.cos(-playerAngle);
            
            // 如果子弹在前方
            if (rotatedY > 0) {
                // 计算子弹在屏幕上的位置
                int screenX = (int) (screenWidth / 2 * (1 + rotatedX / rotatedY * 0.5));
                int size = (int) (screenHeight / rotatedY * 5); // 远小近大
                
                if (size > 0 && screenX >= -size && screenX < screenWidth + size) {
                    // 绘制子弹
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(screenX - size/2, (int) (screenHeight/2 - size/2), size, size);
                }
            }
        }
    }
    
    private boolean isWallAt(int x, int y) {
        for (Wall wall : walls) {
            if (x >= wall.x && x < wall.x + wall.width && 
                y >= wall.y && y < wall.y + wall.height) {
                return true;
            }
        }
        return false;
    }
    
    private void drawMinimap(Graphics2D g2d) {
        // 绘制一个小地图，显示玩家和环境的俯视图
        int mapSize = 150;
        int mapX = 10;
        int mapY = 10;
        double scale = 0.3; // 缩放比例
        
        // 绘制地图背景
        g2d.setColor(new Color(0, 0, 0, 128)); // 半透明背景
        g2d.fillRect(mapX, mapY, mapSize, mapSize);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(mapX, mapY, mapSize, mapSize);
        
        // 绘制墙壁
        for (Wall wall : walls) {
            g2d.setColor(Color.GRAY);
            g2d.fillRect(
                mapX + (int)(wall.x * scale), 
                mapY + (int)(wall.y * scale), 
                (int)(wall.width * scale), 
                (int)(wall.height * scale)
            );
        }
        
        // 绘制敌人
        for (Enemy enemy : enemies) {
            g2d.setColor(enemy.color);
            g2d.fillRect(
                mapX + (int)(enemy.x * scale) - 2, 
                mapY + (int)(enemy.y * scale) - 2, 
                4, 
                4
            );
        }
        
        // 绘制玩家
        g2d.setColor(Color.CYAN);
        int playerMapX = mapX + (int)(playerX * scale);
        int playerMapY = mapY + (int)(playerY * scale);
        g2d.fillOval(playerMapX - 3, playerMapY - 3, 6, 6);
        
        // 绘制玩家朝向
        int toX = playerMapX + (int)(Math.cos(playerAngle) * 10);
        int toY = playerMapY + (int)(Math.sin(playerAngle) * 10);
        g2d.setColor(Color.YELLOW);
        g2d.drawLine(playerMapX, playerMapY, toX, toY);
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
        // 根据鼠标位置更新玩家朝向
        double dx = mouseX - playerX;
        double dy = mouseY - playerY;
        playerAngle = Math.atan2(dy, dx); // 计算玩家到鼠标位置的角度
        
        // 前进/后退（仅当W或S键按下时）
        if (keys[KeyEvent.VK_W]) {
            double newX = playerX + Math.cos(playerAngle) * 3;
            double newY = playerY + Math.sin(playerAngle) * 3;
            
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
        } else if (keys[KeyEvent.VK_S]) {
            double newX = playerX - Math.cos(playerAngle) * 3;
            double newY = playerY - Math.sin(playerAngle) * 3;
            
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
                // 前进逻辑现在在handlePlayerMovement中处理
                break;
            case KeyEvent.VK_S:
                // 后退逻辑现在在handlePlayerMovement中处理
                break;
            case KeyEvent.VK_A:
                // A键不再用于旋转
                break;
            case KeyEvent.VK_D:
                // D键不再用于旋转
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_S:
                // 不需要特殊处理，移动逻辑在handlePlayerMovement中根据按键状态判断
                break;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) { // 左键
            mousePressed = true;
            // 发射子弹
            bullets.add(new Bullet(
                playerX + Math.cos(playerAngle) * PLAYER_SIZE / 2,
                playerY + Math.sin(playerAngle) * PLAYER_SIZE / 2,
                playerAngle,
                7
            ));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) { // 左键
            mousePressed = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }
    
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