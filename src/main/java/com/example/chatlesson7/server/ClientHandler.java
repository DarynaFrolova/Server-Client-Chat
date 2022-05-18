package com.example.chatlesson7.server;

import com.example.chatlesson7.Command;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler {
    private static final int AUTH_TIMEOUT = 120_000;
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
    private final Socket socket;
    private final ChatServer server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final AuthService authService;
    private final Future<Void> timeoutFuture;
    public String login = "";
    private String nick;
    private boolean authenticatedWithinTimeout;

    public ClientHandler(Socket socket,
                         ChatServer server,
                         AuthService authService,
                         ExecutorService executorService) {
        try {
            this.nick = "";
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.authService = authService;
            this.authenticatedWithinTimeout = true;

            executorService.submit(() -> {
                try {
                    authenticate();
                    readMessages();
                } finally {
                    closeConnection();
                }
            });

            timeoutFuture = executorService.submit(() -> {
                try {
                    Thread.sleep(AUTH_TIMEOUT);
                    this.authenticatedWithinTimeout = false;
                    LOGGER.warn("Time is over, stop client");
                    closeConnection();
                } catch (InterruptedException ignored) {
                }
                return null;
            });

        } catch (IOException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
    }

    private static void updateEx(Connection connection, String login, String newNick) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement("UPDATE users SET nick = ? WHERE login = ? ")) {
            statement.setString(1, newNick);
            statement.setString(2, login);
            statement.executeUpdate();
        }
    }

    private void closeConnection() {
        sendMessage("/end");
        if (authenticatedWithinTimeout) {
            LOGGER.info("User {} has left the chat", nick);
            server.broadcast("User " + nick + " has left the chat");
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
        try {
            if (socket != null) {
                server.unsubscribe(this);
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    private void authenticate() {
        while (true) {
            try {
                final String str = in.readUTF();
                if (Command.isCommand(str)) {
                    final Command command = Command.getCommand(str);
                    final String[] params = command.parse(str);
                    if (command == Command.AUTH) {
                        login = params[0];
                        final String password = params[1];
                        final String nick = authService.getNickByLoginAndPassword(login, password);
                        if (nick != null) {
                            if (server.isNickBusy(nick)) {
                                LOGGER.info("Authorization declined. User with these login and password is already authorized");
                                sendMessage(Command.ERROR, "User is already authorized");
                                continue;
                            }
                            this.timeoutFuture.cancel(true);
                            sendMessage(Command.AUTHOK, nick, login);
                            this.nick = nick;
                            LOGGER.info("Successful authorization. User {} has entered the chat", nick);
                            server.broadcast("User " + nick + " has entered the chat");
                            server.subscribe(this);
                            break;
                        } else {
                            LOGGER.info("Authorization declined. User has entered wrong login and password");
                            sendMessage(Command.ERROR, "Wrong login and password");
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e);
                break;
            }
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }

    public void sendMessage(String message) {
        try {
            LOGGER.trace("SERVER: Send message to {}", nick);
            out.writeUTF(message);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    private void readMessages() {
        try {
            while (true) {
                final String msg = in.readUTF();
                LOGGER.trace("Receive message: {}", msg);
                if (Command.isCommand(msg)) {
                    final Command command = Command.getCommand(msg);
                    final String[] params = command.parse(msg);
                    if (command == Command.END) {
                        break;
                    }
                    if (command == Command.PRIVATE_MESSAGE) {
                        server.sendMessageToClient(this, params[0], params[1]);
                        continue;
                    }
                    // Смена ника
                    if (command == Command.NICK) {
                        String temp = this.nick;
                        this.nick = params[0];
                        LOGGER.info("Nick of {} successfully changed to {}", temp, params[0]);
                        sendMessage(msg);
                        updateEx(DataBaseAuthService.connection, this.login, this.nick);
                        server.broadcastClientList();
                        server.changeNick(temp, this);
                        continue;
                    }
                }
                server.broadcast(nick + ": " + msg);
            }
        } catch (IOException | SQLException e) {
            LOGGER.error(e);
        }
    }

    public String getNick() {
        return nick;
    }
}