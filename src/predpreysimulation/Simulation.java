/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import predpreysimulation.PredManager.Pred;
import predpreysimulation.PreyManager.Prey;

/**
 * Manages encounters and displays the GUI, Readouts, and simulation area
 * @author Lachlan Harris
 */
public final class Simulation {

    private final StackPane display;
    private final Canvas canvas;
    public final GraphicsContext gc;
    public final PreyManager preyManager;
    public final PreyFood preyFood;
    public final PredManager predManager;
    private final double width;
    private final double height;
    public final HashMap<String, Timeline> timelines = new HashMap<>();
    public static final Duration DISPLAY_UPDATE_INTERVAL = Duration.millis(25);
    public static final Duration ENCOUNTER_CHECK_INTERVAL = Duration.millis(200);
    private static final double SECTOR_SIZE = 50;
    private final HashSet<Animal>[][] sectors;

    public Simulation(BorderPane main, double width, double height, double scale) {
        this.width = width / scale;
        this.height = height / scale;

        FlowPane readout = new FlowPane();
        main.setTop(readout);

        display = new StackPane();
        Scale scaleObj = new Scale();
        scaleObj.setPivotX(0);
        scaleObj.setPivotY(0);
        scaleObj.setX(scale);
        scaleObj.setY(scale);
        display.getTransforms().add(scaleObj);
        main.setCenter(display);

        Button pauseBtn = new Button("Pause");
        pauseBtn.setOnAction(event -> {
            if (pauseBtn.getText().equals("Pause")) {
                pauseTimelines();
                pauseBtn.setText("Play");
            } else {
                playTimelines();
                pauseBtn.setText("Pause");
            }
        });
        readout.getChildren().add(pauseBtn);

        canvas = new Canvas(this.width, this.height);
        gc = canvas.getGraphicsContext2D();

        preyFood = new PreyFood(this.width, this.height, timelines, main);
        this.display.getChildren().add(preyFood);
        this.display.getChildren().add(canvas);

        preyManager = new PreyManager(this.width, this.height, timelines, preyFood, main);
        predManager = new PredManager(this.width, this.height, timelines, main);

        sectors = new HashSet[(int) (this.height / SECTOR_SIZE)][(int) (this.width / SECTOR_SIZE)];
        for (HashSet<Animal>[] sector : sectors) {
            for (int j = 0; j < sectors[0].length; j++) {
                sector[j] = new HashSet<>();
            }
        }

        Timeline timeline
                = new Timeline(new KeyFrame(DISPLAY_UPDATE_INTERVAL, event -> updateDisplay()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("simDisplayUpdate", timeline);

        timeline = new Timeline(new KeyFrame(ENCOUNTER_CHECK_INTERVAL, event -> checkEncounters()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("encounterCheck", timeline);
    }

    public void pauseTimelines() {
        for (Timeline t : timelines.values()) {
            t.pause();
        }
    }

    public void playTimelines() {
        for (Timeline t : timelines.values()) {
            t.play();
        }
    }

    public void updateDisplay() {
        gc.beginPath();
        gc.clearRect(0, 0, width, height);
        preyManager.displayPrey(gc);
        predManager.displayPreds(gc);
    }

    public void checkEncounters() {
        Date startTime = Date.from(Instant.now());
        updateSectors();
        predManager.updateAvgFitness();
        for (int row = 0; row < height / SECTOR_SIZE; row++) {
            for (int col = 0; col < width / SECTOR_SIZE; col++) {
                checkSector(row, col);
            }
        }
        System.out.println("Encounter check duration: " + startTime.toInstant().until(Instant.now()));
    }

    private void checkSector(int row, int col) {
        Set<Animal> sourceSet = sectors[row][col];
        Set<Animal> targetSet = new HashSet<>(sectors[row][col]);
        targetSet.addAll(getSector(row - 1, col - 1));
        targetSet.addAll(getSector(row - 1, col));
        targetSet.addAll(getSector(row - 1, col + 1));
        targetSet.addAll(getSector(row, col - 1));
        targetSet.addAll(getSector(row, col + 1));
        targetSet.addAll(getSector(row + 1, col - 1));
        targetSet.addAll(getSector(row + 1, col));
        targetSet.addAll(getSector(row + 1, col + 1));

        int checkCount = 0;
        boolean hungry;
        boolean mature;
        for (Animal a : sourceSet) {
            if (a instanceof Pred pred && pred.getAlive()) {
                hungry = pred.isHungry();
                mature = pred.isMature();
                for (Animal target : targetSet) {
                    if (target.getAlive()) {
                        checkCount++;
                        if (target instanceof Prey prey) {
                            if (hungry && Coord.inRange(pred.getPos(), prey.getPos(), pred.traits.encounterDistance())) {
                                predManager.encounter(pred, prey);
                                hungry = pred.isHungry();
                            }
                        } else if (target instanceof Pred other
                                && mature
                                && pred.id != other.id
                                && ((pred.traits.encounterDistance() == other.traits.encounterDistance() && pred.id > other.id)
                                || pred.traits.encounterDistance() > other.traits.encounterDistance())
                                && Coord.inRange(pred.getPos(), other.getPos(), pred.traits.encounterDistance())) {
                            predManager.encounter(pred, other);
                        }
                    }
                }
            }
        }
    }

    private void updateSectors() {
        for (HashSet<Animal>[] sectorRow : sectors) {
            for (HashSet<Animal> sector : sectorRow) {
                sector.clear();
            }
        }
        for (Animal prey : preyManager) {
            getSector(prey.getPos()).add(prey);
        }
        for (Animal pred : predManager) {
            getSector(pred.getPos()).add(pred);
        }
    }

    private HashSet<Animal> getSector(int row, int col) {
        if (row < 0 || col < 0 || row >= sectors.length || col >= sectors[0].length) {
            return new HashSet<>();
        }
        return sectors[row][col];
    }

    private HashSet<Animal> getSector(Coord pos) {
        return sectors[(int) (pos.getY() / SECTOR_SIZE)][(int) (pos.getX() / SECTOR_SIZE)];
    }

}
