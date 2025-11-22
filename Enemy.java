import java.awt.*;

/**
 * 敌人类
 */
public class Enemy {
    public double x, y;
    public double width, height;
    public double speed = 1.0;
    public Color color;
    
    public Enemy(double x, double y, double width, double height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }
}