package com.example.chatlesson7;

import com.example.chatlesson7.server.ChatHistory;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatClient {

    private static final Logger LOGGER = LogManager.getLogger(ChatClient.class);
    private final Controller controller;
    private final ChatHistory history;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String login;

    public ChatClient(Controller controller, ChatHistory history) {
        this.controller = controller;
        this.history = history;
    }

    public String getLogin() {
        return login;
    }

    public void openConnection() throws Exception {
        socket = new Socket("localhost", 8189);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        final Thread readThread = new Thread(() -> {
            try {
                waitAuthenticate();
                readMessage();
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e);
            } finally {
                try {
                    closeConnection();
                } catch (SQLException e) {
                    LOGGER.error(e);
                }
            }
        });

        readThread.setDaemon(true);
        readThread.start();

    }

    private void readMessage() throws IOException {
        while (true) {
            final String message = in.readUTF();
            LOGGER.trace("Receive message: {}", message);
            ;
            if (Command.isCommand(message)) {
                final Command command = Command.getCommand(message);
                final String[] params = command.parse(message);
                if (command == Command.END) {
                    controller.setAuth(false);
                    break;
                }
                if (command == Command.ERROR) {
                    Platform.runLater(() -> controller.showError(params));
                    continue;
                }
                if (command == Command.CLIENTS) {
                    controller.updateClientList(params);
                    continue;
                }
                if (command == Command.NICK) {
                    controller.addMessage("Nick successfully changed to " + params[0]);
                    continue;
                }
            }
            controller.addMessage(message);
            history.saveClientHistory(controller, this.login);
            history.saveChatHistory(controller);
        }
    }

    private void waitAuthenticate() throws IOException, InterruptedException {
        while (true) {
            final String msgAuth = in.readUTF();
            if (Command.isCommand(msgAuth)) {
                final Command command = Command.getCommand(msgAuth);
                final String[] params = command.parse(msgAuth);
                if (command == Command.AUTHOK) {
                    final String nick = params[0];
                    login = params[1];
                    File historyFile = new File("history.txt");
                    if (historyFile.exists()) {
                        history.loadHistory(controller);
                    }
                    controller.addMessage("Successful authorization under nick " + nick);
                    controller.setAuth(true);
                    break;
                }
                if (Command.ERROR.equals(command)) {
                    Platform.runLater(() -> controller.showError(params));
                }
            }
        }
    }

    private void closeConnection() throws SQLException {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
        System.exit(0);
    }

    public void sendMessage(String message) {
        try {
            LOGGER.trace("Send message: {} ", message);
            out.writeUTF(message);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }
}