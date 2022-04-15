package com.example.chatlesson7.server;

import com.example.chatlesson7.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientHandler {
    private final Socket socket;
    private final ChatServer server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final AuthService authService;

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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isAuthenticatedWithTimeOut) {
                    try {
                        readMessages();
                    } finally {
                        closeConnection();
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

    private void closeConnection() {
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
                        final String login = params[0];
                        final String password = params[1];
                        final String nick = authService.getNickByLoginAndPassword(login, password);
                        if (nick != null) {
                            if (server.isNickBusy(nick)) {
                                sendMessage(Command.ERROR, "User is already authorized");
                                continue;
                            }
                            sendMessage(Command.AUTHOK, nick);
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
                }
                server.broadcast(nick + ": " + msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
