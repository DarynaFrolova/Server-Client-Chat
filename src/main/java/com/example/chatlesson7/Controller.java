package com.example.chatlesson7;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class Controller {

    private final ChatClient client;
    @FXML
    private ListView<String> clientList;
    @FXML
    private HBox messageBox;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField loginField;
    @FXML
    private HBox loginBox;
    @FXML
    private TextField textField;
    @FXML
    private TextArea textArea;

    public Controller() {
        client = new ChatClient(this);
        while (true) {
            try {
                client.openConnection();
                break;
            } catch (Exception e) {
                showNotification();
            }
        }
    }

    public void btnSendClick(ActionEvent event) {
        final String message = textField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        client.sendMessage(message);
        textField.clear();
        textField.requestFocus();
    }

    public void addMessage(String message) {
        textArea.appendText(message + "\n");
    }

    public void btnAuthClick(ActionEvent actionEvent) {
        client.sendMessage(Command.AUTH, loginField.getText(), passwordField.getText());
    }

    public void passwordEnter(ActionEvent actionEvent) {
        client.sendMessage("/auth " + loginField.getText() + " " + passwordField.getText());
    }

    public void setAuth(boolean success) {
        loginBox.setVisible(!success);
        messageBox.setVisible(success);
        textArea.setVisible(success);
    }

    private void showNotification() {
        final Alert alert = new Alert(Alert.AlertType.ERROR,
                "Server Not Found \n" +
                        "Make sure that server has been started",
                new ButtonType("Try once more", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE));
        alert.getDialogPane().setStyle("-fx-font-family: 'serif'");
        alert.setTitle("Connection error");
        final Optional<ButtonType> buttonType = alert.showAndWait();
        final Boolean isExit = buttonType.map(btn -> btn.getButtonData().isCancelButton()).orElse(false);
        if (isExit) {
            System.exit(0);
        }
    }

    public void showError(String[] error) {
        final Alert alert = new Alert(Alert.AlertType.ERROR, error[0]);
        alert.setTitle("Error!");
        alert.getDialogPane().setStyle("-fx-font-family: 'serif'");
        alert.showAndWait();
    }

    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) { // /w nick1 private message
            final String message = textField.getText();
            final String nick = clientList.getSelectionModel().getSelectedItem();
            textField.setText(Command.PRIVATE_MESSAGE.collectMessage(nick, message));
            textField.requestFocus();
            textField.selectEnd();
        }
    }

    public void updateClientList(String[] params) {
        Platform.runLater(() -> clientList.getItems().clear());
        Platform.runLater(() -> clientList.getItems().addAll(params));
    }

    // Запись локальной истории (с момента подключения конкретного клиента) в текстовый файл на клиенте
    public void saveClientHistory() {
        try {
            String fileName = "history_" + client.getLogin() + ".txt";
            File userHistory = new File(fileName);

            PrintWriter fileWriter1 = new PrintWriter(new FileWriter(userHistory, false));

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter1);
            bufferedWriter.write(textArea.getText());
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Запись истории чата (с момента подключения первого клиента)
    public void saveChatHistory() {
        try {
            File chatHistory = new File("history.txt");
            PrintWriter fileWriter2 = new PrintWriter(new FileWriter(chatHistory, false));
            BufferedWriter bufferedWriter2 = new BufferedWriter(fileWriter2);
            bufferedWriter2.write(textArea.getText());
            bufferedWriter2.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Демонстрация последних 100 строк истории чата
    public void loadHistory() throws IOException {
        int maxLines = 100;
        List<String> historyList = new ArrayList<>();
        FileInputStream in = new FileInputStream("history.txt");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        String temp;
        while ((temp = bufferedReader.readLine()) != null) {
            if (!temp.startsWith("to ") && !temp.startsWith("from ")) {
                if (historyList.size() == maxLines) {
                    historyList.remove(0);
                }
                historyList.add(temp);
            }
        }

        for (String s : historyList) {
            textArea.appendText(s + "\n");
        }
    }
}