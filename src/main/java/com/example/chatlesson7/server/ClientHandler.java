package com.example.chatlesson7.server;

import com.example.chatlesson7.Command;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientHandler {
    private final Socket socket;
    private final ChatServer server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final AuthService authService;
    public String login = "";
    private String nick;
    private boolean isAuthenticatedWithTimeOut = true;

    public ClientHandler(Socket socket, ChatServer server, AuthService authService) {
        try {
            this.nick = "";
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.authService = authService;

            // Отключение неавторизованных пользователей по тайм-ауту 120 сек.
            Thread thread = new Thread(() -> {

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(() -> authenticate());
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
                        isAuthenticatedWithTimeOut = false;
                        closeConnection();
                    }
                } catch (InterruptedException | SQLException | IOException e) {
                    e.printStackTrace();
                }

                if (isAuthenticatedWithTimeOut) {
                    try {
                        readMessages();
                    } finally {
                        try {
                            closeConnection();
                        } catch (SQLException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            );
            thread.setDaemon(true);
            thread.start();

        } catch (IOException e) {
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


    public void closeConnection() throws SQLException, IOException {
        sendMessage("/end");
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                server.unsubscribe(this);
                if (isAuthenticatedWithTimeOut) {
                    server.broadcast("User " + nick + " has left the chat");
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        authService.close();
    }

    private void authenticate() {
        while (true) {
            try {
                if (!isAuthenticatedWithTimeOut) {
                    break;
                }
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
                                sendMessage(Command.ERROR, "User is already authorized");
                                continue;
                            }
                            sendMessage(Command.AUTHOK, nick, login);
                            this.nick = nick;
                            server.broadcast("User " + nick + " has entered the chat");
                            server.subscribe(this);
                            break;
                        } else {
                            sendMessage(Command.ERROR, "Wrong login and password");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }

    public void sendMessage(String message) {
        try {
            System.out.println("SERVER: Send message to " + nick);
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessages() {
        try {
            while (true) {
                final String msg = in.readUTF();
                System.out.println("Receive message: " + msg);
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
                        System.out.println("Nick successfully changed");
                        sendMessage(msg);
                        updateEx(DataBase.connection, this.login, this.nick);
                        server.broadcastClientList();
                        server.changeNick(temp, this);
                        continue;
                    }
                }
                server.broadcast(nick + ": " + msg);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}