package org.example.wordgame.server;

import org.example.wordgame.utils.DatabaseConnectionPool;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is listening on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected");
            new ClientHandler(clientSocket).start();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] command = message.split(" ");
                    switch (command[0].toUpperCase()) {
                        case "REGISTER":
                            handleRegister(command);
                            break;
                        case "LOGIN":
                            handleLogin(command);
                            break;
                        case "LOGOUT":
                            handleLogout(command);
                            break;
                        case "CHANGE_PASSWORD":
                            handleChangePassword(command);
                            break;
                        default:
                            out.println("Unknown command.");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRegister(String[] command) {
            if (command.length != 3) {
                out.println("Invalid REGISTER command.");
                return;
            }
            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.executeUpdate();
                out.println("Registration successful.");
            } catch (SQLException e) {
                out.println("Username already exists.");
            }
        }

        private void handleLogin(String[] command) {
            if (command.length != 3) {
                out.println("Invalid LOGIN command.");
                return;
            }
            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    out.println("Login successful.");
                } else {
                    out.println("Invalid username or password.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("Database error.");
            }
        }

        private void handleLogout(String[] command) {
            if (command.length != 2) {
                out.println("Invalid LOGOUT command.");
                return;
            }
            String userId = command[1];
            out.println("User " + userId + " logged out.");
        }

        private void handleChangePassword(String[] command) {
            if (command.length != 4) {
                out.println("Invalid CHANGE_PASSWORD command.");
                return;
            }
            String username = command[1];
            String oldPassword = command[2];
            String newPassword = command[3];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
                 PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?")) {

                checkStmt.setString(1, username);
                checkStmt.setString(2, oldPassword);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    updateStmt.setString(1, newPassword);
                    updateStmt.setString(2, username);
                    updateStmt.executeUpdate();
                    out.println("Password changed successfully.");
                } else {
                    out.println("Incorrect old password.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("Database error.");
            }
        }
    }
}
