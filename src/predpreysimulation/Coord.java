/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

/**
 *
 * @author Lachlan Harris
 */
public class Coord {
    private double x;
    private double y;
    
    public Coord(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Coord(Coord prev) {
        this(prev.x, prev.y);
    }
    
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Coord move(Vector v) {
        x += v.getX();
        y += v.getY();
        return this;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public void constrain(double width, double height) {
        x = Math.max(Math.min(x, width - 1), 0);
        y = Math.max(Math.min(y, height - 1), 0);
    }
    
    public static Vector diff(Coord pos1, Coord pos2) {
        return new Vector(pos2.x - pos1.x, pos2.y - pos1.y);
    }
    
    public static double slope(Coord pos1, Coord pos2) {
        return (pos2.y - pos1.y) / (pos2.x - pos1.x);
    }
    
    public static double angle(Coord pos1, Coord pos2) {
        return angle(slope(pos1, pos2));
    }
    
    public static double angle(double slope) {
        return Math.tan(slope);
    }
    
    public static boolean inRange(Coord pos1, Coord pos2, double range) {
        double xDiff = Math.abs(pos1.x - pos2.x);
        if (xDiff > range) {
            return false;
        }
        double yDiff = Math.abs(pos1.y - pos2.y);
        return yDiff <= range && Math.hypot(xDiff, yDiff) <= range;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    
    
}
