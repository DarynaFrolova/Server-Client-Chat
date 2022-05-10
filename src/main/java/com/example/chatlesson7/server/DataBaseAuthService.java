package com.example.chatlesson7.server;

import java.sql.*;

public class DataBaseAuthService implements AuthService {

    public static Connection connection;

    public DataBaseAuthService() throws SQLException, ClassNotFoundException {
        run();
        select(connection); // (для проверки)
    }

    @Override
    public String getNickByLoginAndPassword(String login, String password) {
        try (final PreparedStatement statement =
                     connection.prepareStatement("SELECT nick FROM users WHERE login = ? AND pass = ?")) {
            statement.setString(1, login);
            statement.setString(2, password);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getString("nick");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:javadb.db");
            System.out.println("AuthService run");
        } catch (SQLException e) {
            throw new RuntimeException("Error. Cannot connect to Database", e);
        }
    }

    @Override
    public void close() {
        try {
            select(connection); // (для проверки изменения ников)
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            if (connection != null) {
                connection.close();
                System.out.println("AuthService closed");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error. Cannot close connection with Database", e);
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
}

