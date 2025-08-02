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
 * Manages all the Prey
 * @author Lachlan Harris
 */
public final class PreyManager extends LinkedList<PreyManager.Prey> {

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
    public static final double SPEED = 10; // The distance moved each moveInterval, DO NOT CHANGE
    private final int initialPop = 1500; // Num of prey spawned at start of sim
    public final double maxStarvationChance = 0.005; // Multiplied by the percent food missing in the prey's PreyFood tile to get the chance of starvation.
    private final HashSet<Prey> deadPrey = new HashSet<>();
    private final HashSet<Prey> parentPrey = new HashSet<>();
    private final HashSet<Prey> childPrey = new HashSet<>();
    public int preyCount = 0;

    public final PreyTraits startTraits = new PreyTraits(
            200, // childDistMean
            200, // childDistDev
            10000, // lifespan
            15, // moveInterval
            300, // range
            0.9, // birthFoodThreshold
            0.004, // maxBirthChance
            5 // litterSize
    );

    public record PreyTraits(
            double childDistMean,
            double childDistDev,
            double lifespan,
            double moveInterval, // how often the prey moves
            double range, // the diameter of the prey's "teritory"
            double birthFoodThreshold, // minimum percentage of food in prey's PreyFood tile for a birth check to be made
            double maxBirthChance, // max chance that the prey will have a litter
            double litterSize // num offspring created when birth check passed
            ) {

    }

    public PreyManager(double width, double height, HashMap<String, Timeline> timelines, PreyFood preyFood, BorderPane main) {
        this.timelines = timelines;
        this.width = width;
        this.height = height;
        this.preyFood = preyFood;
        rand = new Random();
        Label popLabel = new Label("  Prey Pop: " + 0);
        Label genLabel = new Label("  Prey Avg Gen: " + 0);
        ((FlowPane) main.getTop()).getChildren().add(popLabel);
        ((FlowPane) main.getTop()).getChildren().add(genLabel);

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
                    for (Prey prey : this) {
                        totalGen += prey.gen;
                    }
                    genLabel.setText("Pred Avg Gen: " + (double) totalGen / size());
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
            gc.fillOval(prey.center.getX() + prey.pattern.getOffset().getX(), prey.center.getY() + prey.pattern.getOffset().getY(), DISPLAY_SIZE, DISPLAY_SIZE);
        }
    }

    public final class Prey implements Animal {

        final Coord center; // Prey's current position in the display area.
        MovePattern pattern; // Used to 
        final int gen; // generation of the prey.
        /**
         * Used to store traits, currently unnecessary, but will be needed once
         * evolution of prey traits is implemented.
         */
        final PreyTraits traits;
        /**
         * Flag for whether prey will be terminated when death check occurs.
         * Note: death has no effect until death check, and prey is still
         * displayed for 1 death check interval after being terminated.
         */
        boolean alive;
        int unbornChildren; // Counter tracking how many children this prey will spawn when the birth check occurs
        public final int id; // Each prey gets a unique identifier, used to access things specific to this prey.
        Timeline lifespanTimeline;
        Timeline moveTimeline;
        
        /**
         * Constructor used to spawn gen 0 prey in a random spot.
         */
        public Prey() {
            this(new Coord(rand.nextDouble() * (width - startTraits.range) + startTraits.range / 2, rand.nextDouble() * (height - startTraits.range) + startTraits.range / 2));
        }

        /**
         * Constructor used to create a gen 0 prey at a specified position.
         *
         * @param center Coord - center of the area the new prey will move in.
         */
        public Prey(Coord center) {
            this(center,
                    0,
                    startTraits
            );
        }

        /**
         * Main constructor used to create prey.
         *
         * @param center Coord - center of the area the new prey will move in.
         * @param gen int - the generation of the new prey.
         * @param traits PreyTraits - record of the prey's traits will have.
         */
        public Prey(Coord center, int gen, PreyTraits traits) {
            this.traits = traits;
            this.gen = gen;
            this.center = center;
            pattern = new MovePattern(traits.range, SPEED);
            id = preyCount++;

            // Moves prey, movement and display timelines are seperate, and a prey may move multiple times inbetween each display
            moveTimeline = new Timeline(
                    new KeyFrame(Duration.millis(traits.moveInterval), event -> {
                        move();
                    })
            );
            moveTimeline.setCycleCount(Animation.INDEFINITE);
            moveTimeline.play();
            timelines.put("preyMove" + id, moveTimeline);

            // When this timeline ends, the prey dies of old age.
            lifespanTimeline = new Timeline(
                    new KeyFrame(Duration.millis(traits.lifespan), event -> {
                        kill();
                    })
            );
            lifespanTimeline.setCycleCount(1);
            lifespanTimeline.play();
            timelines.put("preyLifespan" + id, lifespanTimeline);

            alive = true;
            unbornChildren = 0;
        }

        /**
         * Called to create offspring and return it. If there are no offspring
         * to create, returns null. Only creates 1 offspring at a time, so must
         * be called until null is returned.
         *
         * @return Prey - new offspring of this prey
         */
        private Prey procreate() {
            if (unbornChildren > 0) {
                unbornChildren--;
                Prey child = new Prey(
                        pattern.getOffset(),
                        gen + 1,
                        traits
                );
                child.jump(rand.nextGaussian(traits.childDistMean, traits.childDistDev));
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
            kill("unspecified");
        }

        public void kill(String cause) {
            alive = false;
        }

        /**
         * Called when the prey is removed from the PreyManager, stops all
         * updates. Note: after termination, prey is still displayed for 1
         * preyDeathCheck cycle
         */
        private void terminate() {
            moveTimeline.stop();
            lifespanTimeline.stop();
            timelines.remove("preyMove" + id);
            timelines.remove("preyLifespan" + id);
            moveTimeline = null;
            lifespanTimeline = null;
        }

        /**
         * Called to move the prey. Note: movement and display timelines are on
         * different cycles, and move may be called multiple times between each
         * display.
         */
        private void move() {
            pattern.nextPos();
        }

        @Override
        public double getX() {
            return center.getX() + pattern.getOffset().getX();
        }

        @Override
        public double getY() {
            return center.getY() + pattern.getOffset().getY();
        }

        @Override
        public Coord getPos() {
            return center.clone().move(pattern.getOffset());
        }

        /**
         * Called to remove from the PreyFood tile the prey is on, make a
         * starvation check based on how little food is left, then makes a birth
         * check based on how much food is left if food is above a certain
         * threshold.
         */
        public void eat() {
            double food = preyFood.eat(getPos());
            if (rand.nextDouble() < food * maxStarvationChance) {
                kill();
            } else if (food > traits.birthFoodThreshold && rand.nextDouble() < traits.maxBirthChance * food) {
                addLitter();
            }
        }

        public void addLitter() {
            unbornChildren += traits.litterSize;
        }

        /**
         * Called on spawning and when neither pred dies in a same-gender
         * encounter. Moves the pred's center the given distance, while not
         * putting it in range of going offscreen.
         *
         * @param dist double - the distance the pred will move.
         */
        @Override
        public void jump(double dist) {
            Coord testCenter = center.clone();
            int counter = 0;
            do {
                testCenter.move(new Vector(dist));
                if (counter++ > 100) {
                    System.out.println("Over 100 testCenters for jump of Pred " + id + " with center " + center);
                    return;
                }
            } while (testCenter.getX() < traits.range
                    || testCenter.getX() > width - traits.range
                    || testCenter.getY() < traits.range
                    || testCenter.getY() > height - traits.range);
            center.set(testCenter);
        }
    }
}
