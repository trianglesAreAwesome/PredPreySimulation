/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package predpreysimulation;

import java.util.ArrayList;
import java.util.HashMap;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 *
 * @author Lachlan Harris
 */
public class PreyFood extends Group{
    public final ArrayList<FoodNode> foodNodes = new ArrayList<>();
    public static final double SPACING = 25; // Size of preyfood tiles.
    public static final Duration GROWTH_INTERVAL = Duration.millis(4000); // Time between growth of prey food tiles in millis.
    public static final Duration DISPLAY_INTERVAL = Duration.millis(200); // Time between updating color of prey food tiles in millis.
    public static final double PREY_CONSUMPTION = 0.01; // Amount of prey food that prey eat (in percentage decimal).
    public static final double[] GROWTH_AMOUNTS = {0.5, 0.2, 0.3, 0.1}; // Array containing the amount of prey food grown back every growth interval for each season.
    public static final Duration SEASON_DUR = Duration.seconds(100); // Time between switching of seasons in SECONDS.
    private double currentGrowthAmount;
    private final double height;
    private final double width;
    
    public PreyFood(double width, double height, HashMap<String, Timeline> timelines, BorderPane main) {
        this.width = width;
        this.height = height;
        for (int row = 0; row < height / SPACING; row++) {
            for (int col = 0; col < width / SPACING; col++) {
                foodNodes.add(new FoodNode(row, col));
                getChildren().add(getNode(row, col));
            }
        }
        currentGrowthAmount = GROWTH_AMOUNTS[0];
        
        // Changing Seasons
        Timeline timeline =  new Timeline(
                new KeyFrame(SEASON_DUR, event -> {
                    currentGrowthAmount = GROWTH_AMOUNTS[0];
                    main.setBackground(Background.fill(Color.SPRINGGREEN));
                    System.out.println("Spring");
                }),
                new KeyFrame(SEASON_DUR.multiply(2), event -> {
                    currentGrowthAmount = GROWTH_AMOUNTS[1];
                    main.setBackground(Background.fill(Color.YELLOW));
                    System.out.println("Summer");
                }),
                new KeyFrame(SEASON_DUR.multiply(3), event -> {
                    currentGrowthAmount = GROWTH_AMOUNTS[2];
                    main.setBackground(Background.fill(Color.BROWN));
                    System.out.println("Autumn");
                }),
                new KeyFrame(SEASON_DUR.multiply(4), event -> {
                    currentGrowthAmount = GROWTH_AMOUNTS[3];
                    main.setBackground(Background.fill(Color.WHITESMOKE));
                    System.out.println("Winter");
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
//        timeline.play();
        timelines.put("seasons", timeline);
        
        // Grow food
        timeline = new Timeline(
                new KeyFrame(GROWTH_INTERVAL, event -> {
                    for (FoodNode node : foodNodes) {
                        node.growFood();
                    }
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("preyFoodGrowth", timeline);
        
        // Update Display
        timeline = new Timeline(
                new KeyFrame(DISPLAY_INTERVAL, event -> {
                    for (FoodNode node : foodNodes) {
                        node.updateDisplay();
                    }
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        timelines.put("preyFoodDisplay", timeline);
        this.setOnMouseClicked(event -> {
            System.out.println(new Coord(event.getX(), event.getY()));
        });
    }
    
    private FoodNode getNode(int row, int col) {
        int index = Math.min(row, (int)(height / SPACING - 1)) * (int)(width / SPACING) + (int)Math.min(col, (int)(width / SPACING - 1));
        try {
            return (FoodNode)foodNodes.get(index);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("[ERROR] somehow tried to access FoodNode #" + index + " with (row,col) of (" + row + " , " + col + ")");
            return null;
        }
    }
    
    public FoodNode getNode(Coord pos) {
        return getNode((int)(pos.getY() / SPACING), (int)(pos.getX() / SPACING));
    }
    
    public double eat(Coord pos) {
        return getNode(pos).eatFood();
    }
    
    public class FoodNode extends Rectangle {
        private double foodLeft;
        public final int row;
        public final int col;
        
        public FoodNode(int row, int col) {
            this.row = row;
            this.col = col;
            foodLeft = 1;
            setX(col * SPACING + SPACING / 2);
            setY(row * SPACING + SPACING / 2);
            setWidth(SPACING);
            setHeight(SPACING);
            setFill(Color.color(0, foodLeft, 0));
        }
        
        public double eatFood() {
            return foodLeft = Math.max(foodLeft - PREY_CONSUMPTION, 0);
        }
        
        public void growFood() {
            foodLeft = Math.min(foodLeft + currentGrowthAmount, 1);
        }
        
        public void updateDisplay() {
            setFill(Color.color(0.5, 0.75 + foodLeft / 4, 0.5));
        }
    }
}
