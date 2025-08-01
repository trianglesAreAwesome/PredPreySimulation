/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.util.ArrayList;
import java.util.Collection;
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
 *
 * @author Lachlan Harris
 */
public class PredManager extends LinkedList<PredManager.Pred> {

    private final double width;
    private final double height;
    private final HashMap<String, Timeline> timelines;
    private final Random rand = new Random();
    private final Duration DEATH_CHECK_INTERVAL = Duration.millis(500);
    private final Duration BIRTH_CHECK_INTERVAL = Duration.millis(100);
    private final Duration RANGE_UPDATE_INTERVAL = Duration.millis(200);
    private static final Color DISPLAY_COLOR = Color.DARKSLATEGREY;
    private static final Color DEAD_COLOR = Color.RED;
    private static final Color PARENT_COLOR = Color.DARKBLUE;
    private static final Color CHILD_COLOR = Color.WHITE;
    private static final double ADULT_SIZE = 17;
    private static final double ADULT_DIAMOND_SIZE = Math.sqrt(2 * ADULT_SIZE * ADULT_SIZE);
    private static final double CHILD_SIZE = 13;
    private static final double CHILD_DIAMOND_SIZE = Math.sqrt(2 * CHILD_SIZE * CHILD_SIZE);
    private static final double SPEED = 10;
    private static final double RANGE_BASE = 200;
    private static final double RANGE_SCALE = 700;
    private final HashSet<Pred> deadPreds = new HashSet<>();
    private final HashSet<Pred> childPreds = new HashSet<>();
    private final HashSet<Pred> parentPreds = new HashSet<>();
    private final int initialPop = 300;
    private final double preyCatchChance = 0.25;
    private final double maxEncounterDeathChance = 0.35;
    private final double maturityAge = 0.3;
    private final double hungerThreshold = 0.2;
    public double avgFitness;
    public int predCount;

    private final PredVars startVars = new PredVars(
            500, // range
            20, // moveInterval
            5000, // fedInterval
            100000, // lifespan
            350, // childDistMean
            100, // childDistDev
            50, // catchRange
            0.5, // maxMatingChance
            50 // encounterDistance
    );

    public record PredVars(double range, double moveInterval, double fedInterval, double lifespan,
            double childDistMean, double childDistDev, double catchRange, double maxMatingChance, double encounterDistance) {

    }

    public PredManager(double width, double height, HashMap<String, Timeline> timelines, BorderPane main) {
        this.width = width;
        this.height = height;
        this.timelines = timelines;
        Label popLabel = new Label("  Pred Pop: " + 0);
        Label genLabel = new Label("  Pred Avg Gen: " + 0);
        ((FlowPane)main.getTop()).getChildren().add(popLabel);
        ((FlowPane)main.getTop()).getChildren().add(genLabel);
        predCount = 0;

        Timeline timeline = new Timeline(new KeyFrame(BIRTH_CHECK_INTERVAL, event -> {
            checkPredBirths();
            popLabel.setText("Pred Pop: " + size());
            int totalGen = 0;
            for (Pred pred : this) 
                totalGen += pred.gen;
            genLabel.setText("Pred Avg Gen: " + (double)totalGen / size());
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

        for (int i = initialPop; i > 0; i--) {
            add(new Pred());
        }

        avgFitness = 1;
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
        childPreds.clear();
        parentPreds.clear();
        for (Pred pred : this) {
            if (pred.unbornChild) {
                childPreds.add(pred.procreate());
                parentPreds.add(pred);
            }
        }
        for (Pred child : childPreds) {
            add(child);
        }
    }

    public void displayPreds(GraphicsContext gc) {
        displayGroup(gc, this, DISPLAY_COLOR);
        displayGroup(gc, deadPreds, DEAD_COLOR);
    }

    private void displayGroup(GraphicsContext gc, Collection<Pred> predList, Color color) {
        gc.setFill(color);
        gc.beginPath();
        for (Pred pred : predList) {
            pred.display(gc);
        }
    }

    public void encounter(Pred pred, Prey prey) {
        if (rand.nextDouble() < preyCatchChance) {
            pred.eat();
            prey.kill();
        }
    }

    public void encounter(Pred pred1, Pred pred2) {
        if (pred1.isMature() && pred2.isMature()) {
            if (pred1.gender != pred2.gender) {
                Pred male = pred1.gender == 'm' ? pred1 : pred2;
                if (rand.nextDouble() < male.vars.maxMatingChance * male.getFitness() / avgFitness) {
                    (pred1.gender == 'f' ? pred1 : pred2).addChild(male.id);
                }
            } else {
                double randNum = rand.nextDouble();
                double firstDeathChance = pred1.getFitness() * maxEncounterDeathChance;
                System.out.println(firstDeathChance);
                if (randNum < firstDeathChance) {
                    pred1.kill("encounter");
                } else if (randNum < firstDeathChance + pred2.getFitness() * maxEncounterDeathChance) {
                    pred2.kill("encounter");
                } else {
                    Pred weakling = pred1.getFitness() < pred2.getFitness() ? pred1 : pred2;
                    weakling.pos.move(new Vector(rand.nextGaussian(weakling.vars.childDistMean, weakling.vars.childDistDev))).constrain(width, height);
                }
            }
        }
    }

    public class Pred implements Animal {

        private final Coord pos;
        private final MovePattern pattern;
        private final int gen;
        private boolean alive;
        private boolean unbornChild;
        public final PredVars vars;
        public final int id;
        public final char gender;

        public Pred() {
            this(new Coord(rand.nextDouble() * (width - startVars.range) + startVars.range / 2, rand.nextDouble() * (height - startVars.range) + startVars.range / 2));
        }

        public Pred(Coord pos) {
            this(pos, 0, startVars);
        }

        public Pred(Coord pos, int gen, PredVars vars) {
            this.pos = pos;
            pos.constrain(width, height);
            this.vars = vars;
            this.pattern = new MovePattern(vars.range, SPEED);
            this.gen = gen;
            id = predCount++;
            alive = true;
            unbornChild = false;
            gender = (rand.nextDouble() < 0.25) ? 'm' : 'f';

            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(vars.moveInterval), event -> move()));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
            timelines.put("predMove" + id, timeline);

            timeline = new Timeline(new KeyFrame(Duration.millis(vars.lifespan), event -> kill("lifespan")));
            timeline.setCycleCount(1);
            timeline.play();
            timelines.put("predLifespan" + id, timeline);

            timeline = new Timeline(new KeyFrame(Duration.millis(vars.fedInterval), event -> kill("starved")));
            timeline.setCycleCount(1);
            timeline.play();
            timelines.put("predStarvation" + id, timeline);
        }

        /**
         * Returns age as calculated by % time alive out of lifespan
         *
         * @return double - age
         */
        public double getAge() {
            try {
                return timelines.get("predLifespan" + id).currentTimeProperty().get().divide(vars.lifespan).toMillis();
            } catch (NullPointerException e) {
                return 1;
            }
        }
        
        public boolean isHungry() {
            return getHunger() > hungerThreshold;
        }

        public double getHunger() {
            try {
                return timelines.get("predStarvation" + id).currentTimeProperty().get().divide(vars.fedInterval).toMillis();
            } catch (NullPointerException e) {
                return 1;
            }
        }

        public double getFitness() {
            return (1 - 2 * Math.pow(getAge() - 0.5, 2)) * Math.sqrt(1 - Math.pow(getHunger(), 2));
        }

        public boolean isMature() {
            return getAge() > maturityAge;
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
//            System.out.println(reason);
            alive = false;
        }

        private void terminate() {
            timelines.get("predMove" + id).stop();
            timelines.get("predLifespan" + id).stop();
            timelines.get("predStarvation" + id).stop();
            timelines.remove("predMove" + id);
            timelines.remove("predLifespan" + id);
            timelines.remove("predStarvation" + id);
        }
        
        public void updateRange() {
            pattern.setRange(RANGE_BASE + RANGE_SCALE * (1 - getHunger()));
        }

        private void move() {
            pos.move(pattern.nextPos()).constrain(width, height);
        }

        public void addChild(int parentId) {
            unbornChild = true;
//            System.out.println("Mother: " + id + ", Father: " + parentId);
        }

        private Pred procreate() {
            if (unbornChild) {
//                System.out.println("unborn: " + unbornChild);
                unbornChild = false;
                return new Pred(
                        new Coord(pos).move(new Vector(rand.nextGaussian(vars.childDistMean, vars.childDistDev))),
                        gen + 1,
                        vars
                );
            }
            return null;
        }

        public void eat() {
            if (alive) {
                timelines.get("predStarvation" + id).playFromStart();
            }
        }
        
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
            double size;
            switch (gender) {
                case 'm':
                    size = isMature() ? ADULT_SIZE : CHILD_SIZE;
                    gc.fillRect(pos.getX(), pos.getY(), size, size);
                    break;
                case 'f':
                    size = isMature() ? ADULT_DIAMOND_SIZE : CHILD_DIAMOND_SIZE;
                    double x = pos.getX();
                    double y = pos.getY();
                    double halfSize = size / 2;
                    gc.moveTo(x, y);
                    gc.lineTo(x + halfSize, y - halfSize);
                    gc.lineTo(x + size, y);
                    gc.lineTo(x + halfSize, y + halfSize);
                    gc.closePath();
                    gc.fill();
                    break;
                default:
                    System.out.println("ERROR gender of pred " + id + " is " + gender);
            }
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
    }

}
