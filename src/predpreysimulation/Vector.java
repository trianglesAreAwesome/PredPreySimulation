/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.util.Random;

/**
 *
 * @author Lachlan Harris
 */
public class Vector extends Coord {
    
    public Vector(double x, double y) {
        super(x, y);
    }
    
    public Vector(Vector prev) {
        this(prev.getX(), prev.getY());
    }
    
    public Vector(Coord prev) {
        this(prev.getX(), prev.getY());
    }
    
    /**
     * Creates a Vector with a random direction but the given distance.
     * @param dist 
     */
    public Vector(double dist) {
        super(0, 0);
        Random r = new Random();
        double angle = r.nextDouble() * Math.TAU; // in radians
        move(new Vector(dist * Math.cos(angle), dist * Math.sin(angle)));
    }
    
    public Vector scale(double scale) {
        return new Vector(super.getX() * scale, super.getY() * scale);
    }
    
    public Vector scaleTo(double toRadius) {
        double radius = Math.sqrt(super.getX() * super.getX() + super.getY() * super.getY());
        return scale(toRadius/radius);
    }
    
    @Override
    public Vector clone() {
        return (Vector)super.clone();
    }
}
