///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
// */
//package testpkg;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.Random;
//import javafx.animation.Animation;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.application.Application;
//import static javafx.application.Application.launch;
//import javafx.scene.Group;
//import javafx.scene.Node;
//import javafx.scene.Scene;
//import javafx.scene.canvas.Canvas;
//import javafx.scene.canvas.GraphicsContext;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.control.Spinner;
//import javafx.scene.layout.BorderPane;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.StackPane;
//import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.ArcType;
//import javafx.scene.shape.Circle;
//import javafx.scene.shape.Rectangle;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//import predpreysimulation.Coord;
//import predpreysimulation.MovePattern;
//import static predpreysimulation.PreyFood.SPACING;
//import predpreysimulation.PreyManager;
//import predpreysimulation.PreyManager.Prey;
//import predpreysimulation.PreyManager.PreyTraits;
//import predpreysimulation.Simulation;
//import static predpreysimulation.Simulation.DISPLAY_UPDATE_INTERVAL;
//
///**
// *
// * @author Lachlan Harris
// */
//public class TestPreyPath extends Application{
//    public static final double WIDTH = 1500;
//    public static final double HEIGHT = 750;
//    public Spinner<Double> range;
//    public Spinner<Double> moveInterval;
//    public Spinner<Double> sampleAngleIncrement;
//    public Spinner<Double> angleIncrement;
//    public Spinner<Double> speed;
//    public Simulation sim;
//    public Timeline displayTimeline;
//    
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//        launch(args);
//    }
//    
//    @Override
//    public void start(Stage stage) throws Exception {
//        // set up
//        VBox root = new VBox();
//        Scene scene = new Scene(root);
//        
//        BorderPane sim1Display = new BorderPane();
//        sim = new Simulation(sim1Display, WIDTH, HEIGHT, 1);
//        
//        Button pauseBtn = new Button("Pause");
//        pauseBtn.setOnAction(event -> {
//            if (pauseBtn.getText().equals("Pause")) {
//                sim.pauseTimelines();
//                pauseBtn.setText("Play");
//            } else {
//                sim.playTimelines();
//                pauseBtn.setText("Pause");
//            }
//        });
//        
//        range = new Spinner<>(0, (WIDTH < HEIGHT) ? WIDTH : HEIGHT, sim.preyManager.startRange);
//        moveInterval = new Spinner<>(1, 1000, sim.preyManager.startMoveInterval);
//        sampleAngleIncrement = new Spinner<>(1, 1000, MovePattern.SAMPLE_ANGLE_INCREMENT * 1000);
//        angleIncrement = new Spinner<>(1, 1000, MovePattern.ANGLE_INCREMENT * 1000);
//        speed = new Spinner<>(1, 10000, PreyManager.SPEED * 1000);
//        range.setEditable(true);
//        moveInterval.setEditable(true);
//        sampleAngleIncrement.setEditable(true);
//        angleIncrement.setEditable(true);
//        speed.setEditable(true);
//        Button update = new Button("Update");
//        update.setOnAction(event -> newPrey());
//        
//        HBox spinners = new HBox(update, 
//                new Label("range"), range, 
//                new Label("moveInterval"), moveInterval, 
//                new Label("sampleAngle"), sampleAngleIncrement, 
//                new Label("angle"), angleIncrement, 
//                new Label("speed"), speed
//        );
//        
//        root.getChildren().add(spinners);
//        root.getChildren().add(pauseBtn);
//        root.getChildren().add(sim1Display);
//        stage.setTitle("Pred Prey Simulation");
//        stage.setScene(scene);
//        stage.show();
//        pauseBtn.fire();
//        sim.timelines.get("preyDeathCheck").play();
//        sim.timelines.get("predDeathCheck").play();
//        displayTimeline = new Timeline(
//                new KeyFrame(Duration.millis(20), event -> {
//                    sim.gc.beginPath();
//                    sim.preyManager.displayPrey(sim.gc);
//                })
//        );
//        displayTimeline.setCycleCount(Animation.INDEFINITE);
//        displayTimeline.play();
//        newPrey();
//    }
//    
//    private void newPrey() {
//        for (int i = 0; i < sim.preyManager.size(); i++) {
//            sim.preyManager.get(i).kill();
//        }
//        sim.gc.beginPath();
//        sim.gc.clearRect(0, 0, WIDTH, HEIGHT);
////        Coord pos, int generation, double childDistMean, double childDistDev, Duration lifespan, Duration moveInterval, 
////                double range, MovePattern pattern, double birthFoodThreshold, double maxBirthChance, double litterSize
//        MovePattern pattern = new MovePattern(range.getValue(), speed.getValue() / 1000.0, sampleAngleIncrement.getValue() / 1000.0, MovePattern.START_PATTERN, 0);
//        pattern.setAngleIncrement(angleIncrement.getValue() / 1000.0);
//        sim.preyManager.add(sim.preyManager.new Prey(
//                new Coord(WIDTH / 2, HEIGHT / 2),
//                0,
//                new PreyTraits(
//                0,
//                0,
//                Double.MAX_VALUE,
//                this.moveInterval.getValue(),
//                (double)range.getValue(),
//                100,
//                0,
//                0)
//        ));
//        sim.preyManager.getLast().setPattern(pattern);
//        sim.gc.strokeArc(WIDTH / 2 - range.getValue() / 2, HEIGHT / 2 - range.getValue() / 2, range.getValue(), range.getValue(), 0, 360, ArcType.OPEN);
//    }
//}
