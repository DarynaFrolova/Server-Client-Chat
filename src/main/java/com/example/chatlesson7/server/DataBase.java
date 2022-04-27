package com.example.chatlesson7.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataBase implements AuthService {

    public static Connection connection;
    private final List<UserData> users;

    public DataBase() throws SQLException, ClassNotFoundException {
        connection = DriverManager.getConnection("jdbc:sqlite:javadb.db");
        createTable(connection);

        users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(new UserData("login" + i, "pass" + i, "nick" + i));
        }
        bulkInsert(connection);
        select(connection);
    }

    private void bulkInsert(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users (login, pass, nick) VALUES (?, ?, ?)")) {
            for (UserData user : users) {
                statement.setString(1, user.login);
                statement.setString(2, user.password);
                statement.setString(3, user.nick);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void select(Connection connection) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM users")) {
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                String login = rs.getString(2);
                String pass = rs.getString(3);
                String nick = rs.getString(4);
                System.out.printf("%d - %s - %s - %s\n", id, login, pass, nick);
            }
        }
    }

    private void createTable(Connection connection) {
        try (final PreparedStatement statement = connection.prepareStatement("" +
                " CREATE TABLE IF NOT EXISTS users (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    login TEXT, " +
                "    pass TEXT, " +
                "    nick TEXT " +
                ")")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNickByLoginAndPassword(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.nick;
            }
        }
        return null;
    }

    @Override
    public void run() {
        System.out.println("AuthService run");
    }

    @Override
    public void close() {
        try {
            select(connection); // (для проверки изменения ников)
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("AuthService closed");
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class UserData {
        private final String login;
        private final String password;
        private final String nick;

        public UserData(String login, String password, String nick) {
            this.login = login;
            this.password = password;
            this.nick = nick;
        }
    }
}

