package com.example.chatlesson7.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {

    private final Map<String, ClientHandler> clients;

    public ChatServer() {
        this.clients = new HashMap<>();
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8189);
             AuthService authService = new InMemoryAuthService()) {
            while (true) {
                System.out.println("Wait client connection...");
                final Socket socket = serverSocket.accept();
                new ClientHandler(socket, this, authService);
                System.out.println("Client connected");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isNickBusy(String nick) {
        return clients.containsKey(nick);
    }

    public void subscribe(ClientHandler client) {
        clients.put(client.getNick(), client);
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client.getNick());
    }

    public void broadcast(String msg) {
        clients.values().forEach(client -> client.sendMessage(msg));
    }

    // Private messages
    public void privateMsg(ClientHandler from, String to, String msg)
    {
        for (ClientHandler client: clients.values()) {
            if(client.getNick().equals(to)) {
                client.sendMessage("[Message from: " + from.getNick() + "] " + msg);
                break;
            }
        }
        from.sendMessage("[Message to: " + to + "] " + msg);
    }
}
