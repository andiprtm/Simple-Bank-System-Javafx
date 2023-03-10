package com.bank.app;

import com.bank.app.models.Model;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        ConnectionManager.getInstance();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Model.getInstance().getViewFactory().showLoginWindow();
    }

}
