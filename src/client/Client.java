package client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import user.User;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Client extends javafx.application.Application {
    private volatile ObservableList<User> users = FXCollections.observableArrayList();
    private volatile List<UserMessage> messages = new LinkedList<>();
    private volatile GridPane grid;
    private volatile ClientController clientController;
    private VBox usernameBox;
    private ListView<User> userListView;
    private TextArea messagesArea;
    private volatile Stage stage;

    public static void main(String args[]) {
        Client.launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        Socket socket = new Socket("127.0.0.1", 5000);
        clientController = new ClientController(socket, this);
        new Thread(clientController).start();

        grid = new GridPane();
        grid.setMinSize(800, 600);
        grid.setMaxSize(800, 600);
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setAlignment(Pos.CENTER);

        Button quit = new Button();
        quit.setText("Quit");
        quit.setOnAction(e -> Platform.exit());
        grid.add(quit, 1, 1);
        quit.setMaxWidth(Double.MAX_VALUE);

        Scene scene = new Scene(grid);
        stage.setScene(scene);
        stage.setTitle("Java Chat");
        stage.show();
    }

    public void stop() {
        clientController.close();
    }

    void showUsersList(List<User> users) {
        Platform.runLater(() -> {
            this.users.addAll(users);
            userListView = new ListView<>(this.users);
            userListView.setMouseTransparent(true);
            userListView.setFocusTraversable(false);
            grid.add(userListView, 1, 0);
        });
    }

    void addUser(User user) {
        Platform.runLater(() -> users.add(user));
    }

    void removeUser(User user) {
        Platform.runLater(() -> {
            userListView.requestFocus();

            if (user == currentlySelectedUser()) {
                userListView.getSelectionModel().select(0);
            }

            users.removeIf(u -> u.equals(user));
        });
    }

    void showChoosingUsernameBox() {
        Platform.runLater(() -> {
            usernameBox = new VBox();
            usernameBox.setMinSize(400, 500);
            usernameBox.setFillWidth(true);
            usernameBox.setSpacing(10);
            usernameBox.setAlignment(Pos.TOP_CENTER);

            TextField usernameField = new TextField();
            usernameField.setMaxWidth(Double.MAX_VALUE);

            Label usernameLabel = new Label();
            usernameLabel.setLabelFor(usernameField);
            usernameLabel.setText("Choose your username");

            Button usernameButton = new Button();
            usernameButton.setText("Submit");
            usernameButton.setOnAction(e -> {
                clientController.proposeUsername(usernameField.getText());
                usernameField.clear();
            });
            usernameButton.setDefaultButton(true);

            usernameBox.getChildren().addAll(usernameLabel, usernameField, usernameButton);
            grid.add(usernameBox, 0, 0, 1, 2);
            usernameField.requestFocus();
        });
    }

    void acceptUsername() {
        Platform.runLater(() -> {
            grid.getChildren().remove(usernameBox);
            String username = clientController.getUser().getUsername();
            stage.setTitle(username + " - Java Chat");

            messagesArea = new TextArea();
            messagesArea.setText("Welcome to the chat, " + username + "!");
            messagesArea.setEditable(false);
            messagesArea.setMouseTransparent(true);
            messagesArea.setFocusTraversable(false);

            TextField messageField = new TextField();

            messageField.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    String message = messageField.getText();
                    messageField.clear();
                    User fromUser = clientController.getUser();
                    User toUser = userListView.getSelectionModel().getSelectedItems().get(0);
                    UserMessage userMessage = new UserMessage(fromUser, toUser, message);
                    clientController.sendMessage(userMessage);
                    addMessage(userMessage);
                }
            });

            grid.add(messagesArea, 0, 0);
            grid.add(messageField, 0, 1);

            userListView.setOnMouseClicked(ev -> displayMessages(currentlySelectedUser()));
            userListView.setMouseTransparent(false);
            userListView.setFocusTraversable(true);
            userListView.requestFocus();
            userListView.getSelectionModel().select(0);

            messageField.requestFocus();
        });
    }

    void rejectUsername() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Info");
            alert.setHeaderText("That username is taken");
            alert.setContentText("Choose a different one");
            alert.showAndWait();
        });
    }

    void addMessage(UserMessage message) {
        Platform.runLater(() -> {
            messages.add(message);
            displayMessages(currentlySelectedUser());
        });
    }

    private User currentlySelectedUser() {
        return userListView.getSelectionModel().getSelectedItems().get(0);
    }

    private void displayMessages(User fromUser) {
        Platform.runLater(() -> {
            Stream<UserMessage> stream;

            if (fromUser.getId() == 0) {
                stream = messages.stream();
            } else {
                stream = messages.stream().filter(m -> m.getFromUser().equals(fromUser) || m.getFromUser().equals(clientController.getUser()) && (m.getToUser().equals(fromUser) || m.getToUser().equals(users.get(0))));
            }

            String message = stream.map(UserMessage::toString).reduce("", (a, b) -> a + "\n" + b);
            messagesArea.clear();
            messagesArea.setText(message);
        });
    }
}
