package tiptoieditor;

import javafx.application.Application;
import javafx.stage.Stage;
import tiptoieditor.ui.MainWindow;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow();
        window.show(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}

// .\gradlew.bat run