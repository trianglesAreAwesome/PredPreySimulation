/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import predpreysimulation.PreyManager.Prey;

/**
 * Manages the predators
 *
 * @author Lachlan Harris
 */
public final class PredManager extends LinkedList<PredManager.Pred> {

    private final double width;
    private final double height;
    private final HashMap<String, Timeline> timelines;
    private final Random rand = new Random();
    private final Duration DEATH_CHECK_INTERVAL = Duration.millis(500); // Time between death checks for pred in millis.
    private final Duration BIRTH_CHECK_INTERVAL = Duration.millis(100); // Time between birth checks for pred in millis.
    private final Duration RANGE_UPDATE_INTERVAL = Duration.millis(200); // How often the range of a predator is updated based on time since last meal in millis.
    private static final Color DISPLAY_COLOR = Color.DARKSLATEGREY;
    private static final Color DEAD_COLOR = Color.RED;
    private static final Color PARENT_COLOR = Color.DARKBLUE;
    private static final Color CHILD_COLOR = Color.WHITE;
    private static final double ADULT_SIZE = 17;
    private static final double ADULT_DIAMOND_SIZE = Math.sqrt(2 * ADULT_SIZE * ADULT_SIZE);
    private static final double CHILD_SIZE = 13;
    private static final double CHILD_DIAMOND_SIZE = Math.sqrt(2 * CHILD_SIZE * CHILD_SIZE);
    private static final double SPEED = 10;
    private static final double RANGE_BASE = 200; // Minimum range of patrol pattern.
    private static final double RANGE_SCALE = 700; // RANGE_SCALE increases in proportion to time since last meal. RANGE_SCALE + RANGE_BASE = range of patrol pattern. 
    private final LinkedList<Male> males;
    private final LinkedList<Female> females;
    private final HashSet<Pred> deadPreds = new HashSet<>();
    private static final int INITIAL_POP = 300; // Initial population size of predators.
    private static final double PERCENT_MALES = 0.25; // Percent chance of a new predator being a male.
    private static final double PREY_CATCH_CHANCE = 0.25; // Percent chance of a predator catching prey when they encounter.
    private static final double MAX_ENCOUNTER_DEATH_CHANCE = 0.35; // The chance of death from a territorial encounter is this variable multiplied by inverse of fitness.
    private static final double MATURITY_AGE = 0.3; // Percentage of lifespan at which rules apply for predator territorial encounters and predator procreation encounters.
    private static final double HUNGER_THRESHOLD = 0.2; // The minimum percentage of fedInterval for which the predator can eat prey when encountered.
    public double avgFitness;
    public int predCount;

    private final PredTraits startTraits = new PredTraits(
            500, // range
            20, // moveInterval
            5000, // fedInterval
            100000, // lifespan
            350, // jumpDistMean
            100, // jumpDistDev
            50, // catchRange
            0.5, // maxMatingChance
            50 // encounterDist
    );

    public record PredTraits(
            double range, // Maximum distance from center a pred may reach while following their TERRITORY_PATTERN.
            double moveInterval, // How often the pred moves, in millis.
            double fedInterval, // How long the pred can go without eating a prey before starving.
            double lifespan, // The max time a pred can live, in millis.
            double jumpDistMean, // The mean distance a pred moves when jumping.
            double jumpDistDev, // The standard deviation of the distance a pred moves when jumping.
            double catchRange, // The pred must be this close to a prey before it can try to catch it.
            double maxMatingChance, // Multiplied by male fitness out of avg fitness to get mating chance.
            double encounterDist // Max dist between two preds for an encounter to trigger.
            ) {

    }

    public PredManager(double width, double height, HashMap<String, Timeline> timelines, BorderPane main) {
        this.width = width;
        this.height = height;
        this.timelines = timelines;
        Label popLabel = new Label("  Pred Pop: " + 0);
        Label genLabel = new Label("  Pred Avg Gen: " + 0);
        ((FlowPane) main.getTop()).getChildren().add(popLabel);
        ((FlowPane) main.getTop()).getChildren().add(genLabel);
        predCount = 0;
        Timeline timeline = new Timeline(new KeyFrame(BIRTH_CHECK_INTERVAL, event -> {
            checkPredBirths();
            popLabel.setText("Pred Pop: " + size());
            int totalGen = 0;
            for (Pred pred : this) {
                totalGen += pred.gen;
            }
            genLabel.setText("Pred Avg Gen: " + (double) totalGen / size());
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("predBirthCheck", timeline);

        timeline = new Timeline(new KeyFrame(DEATH_CHECK_INTERVAL, event -> checkDeadPreds()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("predDeathCheck", timeline);

        timeline = new Timeline(new KeyFrame(RANGE_UPDATE_INTERVAL, event -> updateRanges()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("predRangeUpdate", timeline);

        males = new LinkedList<>();
        females = new LinkedList<>();

        for (int i = INITIAL_POP; i > 0; i--) {
            add(spawn());
        }
        avgFitness = 1;
    }

    @Override
    public boolean add(Pred p) {
        switch (p) {
            case Female female ->
                females.add(female);
            case Male male ->
                males.add(male);
            default ->
                System.out.println("ERROR: Pred " + p.id + " is neither male nor female.");
        }
        return super.add(p);
    }

    @Override
    public boolean remove(Object p) {
        if (p instanceof Pred pred) {
            pred.terminate();
            deadPreds.add(pred);
            ((pred.gender == 'f') ? females : males).remove(pred);
            return super.remove(p);
        } else {
            System.out.println("attempted to remove " + p + " from PredManager");
            return false;
        }
    }

    public void updateAvgFitness() {
        double totalFitness = 0;
        for (Pred pred : this) {
            totalFitness += pred.getFitness();
        }
        avgFitness = totalFitness / this.size();
    }

    private void updateRanges() {
        for (Pred pred : this) {
            pred.updateRange();
        }
    }

    private void checkDeadPreds() {
        deadPreds.clear();
        Iterator<Pred> iter = this.iterator();
        Pred next;
        while (iter.hasNext()) {
            next = iter.next();
            if (!next.getAlive()) {
                deadPreds.add(next);
                next.terminate();
                iter.remove();
            }
        }
    }

    private void checkPredBirths() {
        LinkedList<Pred> childPreds = new LinkedList<>();
        for (Female pred : females) {
            if (pred.unbornChild) {
                childPreds.add(pred.procreate());
            }
        }
        for (Pred child : childPreds) {
            add(child);
        }
    }

    public void displayPreds(GraphicsContext gc) {
        for (Pred pred : this) {
            pred.display(gc);
        }
        for (Pred pred : deadPreds) {
            pred.display(gc);
        }
    }

    public void encounter(Pred pred, Prey prey) {
        if (rand.nextDouble() < PREY_CATCH_CHANCE) {
            pred.eat();
            prey.kill(PreyManager.Prey.DeathCause.EATEN);
        }
    }

    public void encounter(Pred pred1, Pred pred2) {
        if (pred1.isMature() && pred2.isMature()) {
            if (pred1.gender != pred2.gender) {
                Pred male = pred1.gender == 'm' ? pred1 : pred2;
                if (rand.nextDouble() < male.traits.maxMatingChance * male.getFitness() / avgFitness) {
                    ((Female) (pred1.gender == 'f' ? pred1 : pred2)).addChild(male.id);
                }
            } else {
                double randNum = rand.nextDouble();
                double firstDeathChance = (1 - pred1.getFitness()) * MAX_ENCOUNTER_DEATH_CHANCE;
                if (randNum < firstDeathChance) {
                    pred1.kill("encounter");
                } else if (randNum < firstDeathChance + (1 - pred2.getFitness()) * MAX_ENCOUNTER_DEATH_CHANCE) {
                    pred2.kill("encounter");
                } else {
                    Pred weakling = pred1.getFitness() < pred2.getFitness() ? pred1 : pred2;
                    weakling.jump(rand.nextGaussian(weakling.traits.jumpDistMean, weakling.traits.jumpDistDev));
                }
            }
        }
    }

    private Pred spawn() {
        return (rand.nextDouble() < PERCENT_MALES ? new Male() : new Female());
    }

    public abstract class Pred implements Animal {

        final Coord center;
        final MovePattern pattern;
        final int gen;
        boolean alive;
        final PredTraits traits;
        public final int id;
        final char gender;
        Timeline lifespanTimeline;
        Timeline starvationTimeline;
        Timeline moveTimeline;

        public Pred() {
            this(new Coord(rand.nextDouble() * (width - startTraits.range) + startTraits.range / 2, rand.nextDouble() * (height - startTraits.range) + startTraits.range / 2));
        }

        public Pred(Coord pos) {
            this(pos, 0, startTraits);
        }

        public Pred(Coord center, int gen, PredTraits traits) {
            this.center = center;
            this.traits = traits;
            this.pattern = new MovePattern(traits.range, SPEED);
            this.gen = gen;
            id = predCount++;
            alive = true;
            gender = (this instanceof Male ? 'm' : 'f');

            moveTimeline = new Timeline(new KeyFrame(Duration.millis(traits.moveInterval), event -> move()));
            moveTimeline.setCycleCount(Animation.INDEFINITE);
            moveTimeline.play();
            timelines.put("predMove" + id, moveTimeline);

            lifespanTimeline = new Timeline(new KeyFrame(Duration.millis(traits.lifespan), event -> kill("lifespan")));
            lifespanTimeline.setCycleCount(1);
            lifespanTimeline.play();
            timelines.put("predLifespan" + id, lifespanTimeline);

            starvationTimeline = new Timeline(new KeyFrame(Duration.millis(traits.fedInterval), event -> kill("starved")));
            starvationTimeline.setCycleCount(1);
            starvationTimeline.play();
            timelines.put("predStarvation" + id, starvationTimeline);
        }

        /**
         * Returns age as calculated by % time alive out of lifespan
         *
         * @return double - age
         */
        public double getAge() {
            try {
                return lifespanTimeline.currentTimeProperty().get().divide(traits.lifespan).toMillis();
            } catch (NullPointerException e) {
                return 1;
            }
        }

        public boolean isHungry() {
            return getHunger() > HUNGER_THRESHOLD;
        }

        public double getHunger() {
            try {
                return starvationTimeline.currentTimeProperty().get().divide(traits.fedInterval).toMillis();
            } catch (NullPointerException e) {
                return 1;
            }
        }

        public double getFitness() {
            return (1 - 2 * Math.pow(getAge() - 0.5, 2)) * Math.sqrt(1 - Math.pow(getHunger(), 2));
        }

        public boolean isMature() {
            return getAge() > MATURITY_AGE;
        }

        @Override
        public boolean getAlive() {
            return alive;
        }

        @Override
        public void kill() {
            kill("unspecified");
        }

        public void kill(String reason) {
            alive = false;
        }

        private void terminate() {
            moveTimeline.stop();
            lifespanTimeline.stop();
            starvationTimeline.stop();
            timelines.remove("predMove" + id);
            timelines.remove("predLifespan" + id);
            timelines.remove("predStarvation" + id);
            moveTimeline = null;
            lifespanTimeline = null;
            starvationTimeline = null;
        }

        public void updateRange() {
            pattern.setRange(RANGE_BASE + RANGE_SCALE * (1 - getHunger()));
        }

        public abstract void move();

        public void eat() {
            try {
                if (alive) {
                    starvationTimeline.playFromStart();
                }
            } catch (NullPointerException e) {
                System.out.println("ERROR: pred with no starvationTimeline tried to eat");
            }
        }

        public abstract void display(GraphicsContext gc);

        @Override
        public double getX() {
            return pattern.getOffset().getX();
        }

        @Override
        public double getY() {
            return pattern.getOffset().getY();
        }

        @Override
        public Coord getPos() {
            Coord pos = center.clone().move(pattern.getOffset());
            pos.constrain(width, height);
            return pos;
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
            Coord testCenter;
            double avgRange = (RANGE_BASE + RANGE_SCALE / 2) / 2;
            int counter = 0;
            do {
                testCenter = center.clone().move(new Vector(dist));
                if (counter++ > 50) {
                    testCenter.constrain(avgRange, avgRange, width - avgRange, height - avgRange);
                }
            } while (testCenter.getX() < avgRange
                    || testCenter.getX() > width - avgRange
                    || testCenter.getY() < avgRange
                    || testCenter.getY() > height - avgRange);
            center.set(testCenter);
        }

    }

    public class Female extends Pred {

        boolean unbornChild = false;

        public Female() {
            super();
        }

        public Female(Coord center) {
            super(center);
        }

        public Female(Coord center, int gen, PredTraits traits) {
            super(center, gen, traits);
        }

        public void addChild(int fatherId) {
            unbornChild = true;
        }

        @Override
        public void display(GraphicsContext gc) {
            gc.beginPath();
            if (!alive) {
                gc.setFill(DEAD_COLOR);
            } else if (getAge() < 0.05) {
                gc.setFill(CHILD_COLOR);
            } else if (unbornChild) {
                gc.setFill(PARENT_COLOR);
            } else {
                gc.setFill(DISPLAY_COLOR);
            }
            double size = isMature() ? ADULT_DIAMOND_SIZE : CHILD_DIAMOND_SIZE;
            double x = center.getX() + pattern.getOffset().getX();
            double y = center.getY() + pattern.getOffset().getY();
            double halfSize = size / 2;
            gc.moveTo(x, y);
            gc.lineTo(x + halfSize, y - halfSize);
            gc.lineTo(x + size, y);
            gc.lineTo(x + halfSize, y + halfSize);
            gc.closePath();
            gc.fill();
        }

        public Pred procreate() {
            Pred child = null;
            if (unbornChild) {
                unbornChild = false;
                PredTraits traits = super.traits; // this is where evolution will be implemented
                int gen = super.gen + 1;
                Coord center = getPos();
                if (rand.nextDouble() < PERCENT_MALES) {
                    child = new Male(center, gen, traits);
                } else {
                    child = new Female(center, gen, traits);
                }
                child.jump(rand.nextGaussian(traits.jumpDistMean, traits.jumpDistDev));

            }
            return child;
        }

        @Override
        public void move() {
            pattern.nextPos();
        }
    }

    public class Male extends Pred {

        public Male() {
            super();
        }

        public Male(Coord center) {
            super(center);
        }

        public Male(Coord center, int gen, PredTraits traits) {
            super(center, gen, traits);
        }

        @Override
        public void move() {
            pattern.getOffset().move(pattern.nextPos()).constrain(width, height);
        }

        @Override
        public void display(GraphicsContext gc) {
            gc.beginPath();
            if (!alive) {
                gc.setFill(DEAD_COLOR);
            } else if (getAge() < 0.05) {
                gc.setFill(CHILD_COLOR);
            } else {
                gc.setFill(DISPLAY_COLOR);
            }
            double size = isMature() ? ADULT_SIZE : CHILD_SIZE;
            gc.fillRect(center.getX() + pattern.getOffset().getX(), center.getY() + pattern.getOffset().getY(), size, size);
        }
    }
}
