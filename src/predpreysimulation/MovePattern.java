/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.util.Random;
import java.util.function.BiFunction;

/**
 * Object for keeping track of an Animal's movement, relative to there center.
 * @author Lachlan Harris
 */
public class MovePattern {

    private static final Random rand = new Random();
    public static final double ANGLE_INCREMENT = 0.025;
    public static final double SAMPLE_ANGLE_INCREMENT = 0.001;
    // Pattern used when moving within territory for predators and prey.
    public static final BiFunction<Double, Double, Double> TERRITORY_PATTERN = (theta, maxRadius) -> maxRadius * Math.cos(Math.PI * theta + theta / Math.PI);
    // Pattern used when a predator moves their territory in response to lack of procreation encounters or lack of prey encounters.
    public static final BiFunction<Double, Double, Double> MIGRATION_PATTERN = (theta, maxRadius) -> theta * maxRadius / 4;
    private double angle;
    private double maxRadius;
    private final BiFunction<Double, Double, Double> pattern;
    private double increment;
    private double angleIncrement;
    private final double sampleAngleIncrement;
    private final Coord lastPos;
    private final double angleOffset;

    public MovePattern(double diameter, double speed, double sampleAngleIncrement, BiFunction<Double, Double, Double> pattern, double angleOffset) {
        this.sampleAngleIncrement = sampleAngleIncrement;
        maxRadius = diameter / 2;
        angleIncrement = ANGLE_INCREMENT * 300 / maxRadius;
        this.pattern = pattern;
        angle = 0;
        increment = speed;
        this.angleOffset = angleOffset;
        lastPos = toCoord(angle, this.pattern.apply(angle, maxRadius));
    }

    public MovePattern(double diameter, double speed) {
        this(diameter, speed, TERRITORY_PATTERN);
    }

    public MovePattern(double diameter, MovePattern prev) {
        this(diameter, prev.increment, prev.pattern);
    }

    public MovePattern(double diameter, double speed, BiFunction<Double, Double, Double> pattern) {
        this(diameter, speed, SAMPLE_ANGLE_INCREMENT, pattern, rand.nextDouble() * Math.PI * 2);
    }

    public void setAngleIncrement(double angleIncrement) {
        this.angleIncrement = angleIncrement;
    }

    public final void setRange(double diameter) {
        lastPos.move(Coord.diff(lastPos, new Vector(lastPos).scale((diameter / 2) / maxRadius)));
        maxRadius = diameter / 2;
        angleIncrement = ANGLE_INCREMENT * 300 / maxRadius;
    }

    public void setSpeed(double speed) {
        increment = speed;
    }

    public Vector getOffset() {
        return new Vector(lastPos);
    }

    public Vector nextPos() {
        double sampleAngle = angle + sampleAngleIncrement;
        Vector last = Coord.diff(lastPos, toCoord(sampleAngle, pattern.apply(sampleAngle, maxRadius))).scaleTo(increment);
        lastPos.move(last);
        angle += angleIncrement + ANGLE_INCREMENT * (rand.nextDouble() - 0.5);
        return last;
    }

    private Coord toCoord(double theta, double r) {
        return new Coord(r * Math.cos(theta + angleOffset), r * Math.sin(theta + angleOffset));
    }
}
