/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 *
 * @author Lachlan Harris
 */
public class PreyManager extends LinkedList<PreyManager.Prey> {

    private final double width;
    private final double height;
    private final HashMap<String, Timeline> timelines;
    private final PreyFood preyFood;
    private final Random rand;
    private static final Duration EAT_INTERVAL = Duration.millis(20);
    private static final Duration BIRTH_CHECK_INTERVAL = Duration.millis(250);
    private static final Duration DEATH_CHECK_INTERVAL = Duration.millis(500);
    private static final int DISPLAY_SIZE = 8;
    private static final Color DISPLAY_COLOR = Color.GRAY;
    private static final Color DEAD_COLOR = Color.RED;
    private static final Color PARENT_COLOR = Color.BLACK;
    private static final Color CHILD_COLOR = Color.WHITE;
    public static final double SPEED = 10;
    private final int initialPop = 1500;
    public final double maxStarvationChance = 0.005;
    public final double startChildDistMean = 200;
    public final double startChildDistDev = 200;
    public final double startLifespan = 10000; // millis
    public final double startMoveInterval = 15; // millis
    public double startRange = 300;
    public final double startBirthFoodThreshold = 0.9;
    public final double startMaxBirthChance = 0.004;
    public final double startLitterSize = 5;
    private final HashSet<Prey> deadPrey = new HashSet<>();
    private final HashSet<Prey> parentPrey = new HashSet<>();
    private final HashSet<Prey> childPrey = new HashSet<>();
    public int preyCount = 0;

    public final PreyVars startVars = new PreyVars(startChildDistMean, startChildDistDev, startLifespan,
            startMoveInterval, startRange, startBirthFoodThreshold, startMaxBirthChance, startLitterSize);

    public PreyManager(double width, double height, HashMap<String, Timeline> timelines, PreyFood preyFood, BorderPane main) {
        this.timelines = timelines;
        this.width = width;
        this.height = height;
        this.preyFood = preyFood;
        rand = new Random();
        Label popLabel = new Label("  Prey Pop: " + 0);
        Label genLabel = new Label("  Prey Avg Gen: " + 0);
        ((FlowPane)main.getTop()).getChildren().add(popLabel);
        ((FlowPane)main.getTop()).getChildren().add(genLabel);

        // prey eat
        Timeline timeline = new Timeline(
                new KeyFrame(EAT_INTERVAL, event -> {
                    feedPrey();
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("preyEat", timeline);

        // prey give birth
        timeline = new Timeline(
                new KeyFrame(BIRTH_CHECK_INTERVAL, event -> {
            checkPreyBirths();
            popLabel.setText("Prey Pop: " + size());
            int totalGen = 0;
            for (Prey prey : this) 
                totalGen += prey.gen;
            genLabel.setText("Pred Avg Gen: " + (double)totalGen / size());
                }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("preyBirthCheck", timeline);

        // prey die
        timeline = new Timeline(
                new KeyFrame(DEATH_CHECK_INTERVAL, event -> {
                    checkDeadPrey();
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("preyDeathCheck", timeline);

        for (int i = initialPop; i > 0; i--) {
            add(new Prey());
        }
    }

    private void feedPrey() {
        for (Prey prey : this) {
            prey.eat();
        }
    }

    private void checkPreyBirths() {
        childPrey.clear();
        parentPrey.clear();
        for (Prey prey : this) {
            Prey child;
            while (true) {
                child = prey.procreate();
                if (child == null) {
                    break;
                }
                parentPrey.add(prey);
                childPrey.add(child);
            }
        }
        for (Prey child : childPrey) {
            add(child);
        }
    }

    private void checkDeadPrey() {
        deadPrey.clear();
        for (Prey prey : this) {
            if (!prey.getAlive()) {
                deadPrey.add(prey);
                prey.terminate();
            }
        }
        for (Prey prey : deadPrey) {
            remove(prey);
        }
    }

    public void displayPrey(GraphicsContext gc) {
        displayGroup(gc, this, DISPLAY_COLOR);
        displayGroup(gc, parentPrey, PARENT_COLOR);
        displayGroup(gc, childPrey, CHILD_COLOR);
        displayGroup(gc, deadPrey, DEAD_COLOR);
    }

    private void displayGroup(GraphicsContext gc, Collection<Prey> preyList, Color color) {
        gc.setFill(color);
        gc.beginPath();
        for (Prey prey : preyList) {
            gc.fillOval(prey.pos.getX(), prey.pos.getY(), DISPLAY_SIZE, DISPLAY_SIZE);
        }
    }

    public record PreyVars(double childDistMean, double childDistDev, double lifespan,
            double moveInterval, double range, double birthFoodThreshold, double maxBirthChance, double litterSize) {

    }

    public class Prey implements Animal {
        private final Coord pos;
        private MovePattern pattern;
        private final int gen;
        private final PreyVars vars;
        private boolean alive;
        private int unbornChildren;
        public final int id;

        public Prey() {
            this(new Coord(rand.nextDouble() * (width - startRange) + startRange / 2, rand.nextDouble() * (height - startRange) + startRange / 2));
//            System.out.println("(" + preyFood.getNode(pos).row + ", " + preyFood.getNode(pos).col + ")");
        }

        public Prey(Coord pos) {
            this(pos,
                    0,
                    startVars
            );
        }

        public Prey(Coord pos, int gen, PreyVars vars) {
            this.vars = vars;
            this.gen = gen;
            this.pos = pos;
            pattern = new MovePattern(vars.range, SPEED);
            this.pos.move(new Vector(pattern.getLastPos())).constrain(width, height);
            id = preyCount++;

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(vars.moveInterval), event -> {
                        move();
                    })
            );
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
            timelines.put("preyMove" + id, timeline);

            timeline = new Timeline(
                    new KeyFrame(Duration.millis(vars.lifespan), event -> {
                        kill();
                    })
            );
            timeline.setCycleCount(1);
            timeline.play();
            timelines.put("preyLifespan" + id, timeline);
            alive = true;
            unbornChildren = 0;
        }

        private Prey procreate() {
            if (unbornChildren > 0) {
                unbornChildren--;
                return new Prey(
                        new Coord(pos).move(new Vector(rand.nextGaussian(vars.childDistMean, vars.childDistDev))),
                        gen + 1,
                        vars
                );
            }
            return null;
        }
        
        public void setPattern(MovePattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean getAlive() {
            return alive;
        }

        @Override
        public void kill() {
            alive = false;
        }

        private void terminate() {
            timelines.get("preyMove" + id).stop();
            timelines.get("preyLifespan" + id).stop();
            timelines.remove("preyMove" + id);
            timelines.remove("preyLifespan" + id);
        }

        private void move() {
            pos.move(pattern.nextPos()).constrain(width, height);
        }

        @Override
        public double getX() {
            return pos.getX();
        }

        @Override
        public double getY() {
            return pos.getY();
        }
        
        @Override
        public Coord getPos() {
            return new Coord(pos);
        }

        public void eat() {
            double food = preyFood.eat(pos);
            if (rand.nextDouble() < food * maxStarvationChance) {
                kill();
            } else if (food > vars.birthFoodThreshold && rand.nextDouble() < vars.maxBirthChance * food) {
                addLitter();
            }
        }

        public void addLitter() {
            unbornChildren += vars.litterSize;
        }
    }
}
