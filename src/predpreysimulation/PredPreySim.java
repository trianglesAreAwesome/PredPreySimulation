/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package predpreysimulation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author Lachlan Harris
 */
public class PredPreySim extends Application{
    public static final double WIDTH = 1500;
    public static final double HEIGHT = 750;
    public static final double SCALE = 0.25;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        // set up
        VBox root = new VBox();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        BorderPane sim1Display = new BorderPane();
        Simulation sim1 = new Simulation(sim1Display, WIDTH, HEIGHT, SCALE);
        
        root.getChildren().add(sim1Display);
        stage.setTitle("Pred Prey Simulation");
        stage.setScene(scene);
        stage.show();
    }
}
