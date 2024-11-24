package org.example.wordgame.server;

import org.example.wordgame.constant.GameConstants;
import org.example.wordgame.models.Room;
import org.example.wordgame.models.User;
import org.example.wordgame.utils.DatabaseConnectionPool;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static final List<Room> rooms = new ArrayList<>();
    private static final Map<String, User> loggedInUsers = new HashMap<>();

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
        private User currentUser ;
        private Room currentRoom;

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

                    if (command.length < 1) {
                        sendResponse("Invalid command format.");
                        continue;
                    }

                    String username = command.length > 1 ? command[1] : "";

                    switch (command[0].toUpperCase()) {
                        case "REGISTER":
                            handleRegister(username, command);
                            break;
                        case "LOGIN":
                            handleLogin(username, command);
                            break;
                        case "LOGOUT":
                            handleLogout(username, command);
                            break;
                        case "CHANGE_PASSWORD":
                            handleChangePassword(username, command);
                            break;
                        case "LIST_ROOM":
                            handleListRooms(username);
                            break;
                        case "CREATE_ROOM":
                            handleCreateRoom(username, command);
                            break;
                        case "JOIN_ROOM":
                            handleJoinRoom(username, command);
                            break;
                        case "LEAVE_ROOM":
                            handleLeaveRoom(username);
                            break;
                        default:
                            sendResponse("Unknown command.");
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

        private void sendResponse(String response) {
            out.println(response);
            System.out.println("Server response: " + response); // Log the response to the server console
        }

        private void handleRegister(String username, String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid REGISTER command.");
                return;
            }
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.executeUpdate();
                sendResponse("Registration successful.");
            } catch (SQLException e) {
                sendResponse("Username already exists.");
            }
        }

        private void handleLogin(String username, String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid LOGIN command.");
                return;
            }
            String password = command[2 ];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    currentUser  = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
                    loggedInUsers.put(username, currentUser );
                    sendResponse("Login successful. Welcome, " + username + "!");
                } else {
                    sendResponse("Invalid username or password.");
                }
            } catch (SQLException e) {
                sendResponse("Database error.");
            }
        }

        private void handleLogout(String username, String[] command) {
            if (command.length != 2) {
                sendResponse("Invalid LOGOUT command.");
                return;
            }

            if (!loggedInUsers.containsKey(username)) {
                sendResponse("You must log in first.");
                return;
            }

            loggedInUsers.remove(username);
            sendResponse("User   " + username + " logged out.");
        }

        private void handleChangePassword(String username, String[] command) {
            if (command.length != 4) {
                sendResponse("Invalid CHANGE_PASSWORD command.");
                return;
            }
            String oldPassword = command[2];
            String newPassword = command[3];

            if (newPassword.equals(oldPassword)) {
                sendResponse("New password cannot be the same as the old password.");
                return;
            }

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
                    sendResponse("Password changed successfully.");
                } else {
                    sendResponse("Incorrect old password.");
                }
            } catch (SQLException e) {
                sendResponse("Database error.");
            }
        }

        private void handleListRooms(String username) {
            if (!loggedInUsers.containsKey(username)) {
                sendResponse("You must log in first.");
                return;
            }

            if (rooms.isEmpty()) {
                sendResponse("No rooms available.");
            } else {
                StringBuilder response = new StringBuilder("Available rooms:");
                for (Room room : rooms) {
                    response.append(" Room ").append(room.getRoomName());
                }
                sendResponse(response.toString());
            }
        }

        private void handleCreateRoom(String username, String[] command) {
            if (!loggedInUsers.containsKey(username)) {
                sendResponse("You must log in first.");
                return;
            }

            if (rooms.size() >= GameConstants.MAX_ROOMS) {
                sendResponse("Maximum number of rooms reached.");
                return;
            }

            String roomName = command[2];
            if (roomExists(roomName)) {
                sendResponse("Room with this name already exists.");
                return;
            }

            Room newRoom = new Room(rooms.size() + 1, roomName);
            rooms.add(newRoom);
            sendResponse("Room created successfully by " + username);
        }

        private void handleJoinRoom(String username, String[] command) {
            if (!loggedInUsers.containsKey(username)) {
                sendResponse("You must log in first.");
                return;
            }

            if (currentRoom != null) {
                sendResponse("You are already in a room. Please leave the current room before joining a new one.");
                return;
            }

            String roomName = command[2];
            Room room = getRoomByName(roomName);
            if (room == null) {
                sendResponse("Room not found.");
                return;
            }

            currentRoom = room;
            sendResponse("User   " + username + " successfully joined room " + roomName);
        }

        private void handleLeaveRoom(String username) {
            if (!loggedInUsers.containsKey(username)) {
                sendResponse("You must log in first.");
                return;
            }

            if (currentRoom == null) {
                sendResponse("You are not in any room.");
                return;
            }

            currentRoom = null;
            sendResponse("User   " + username + " left the room.");
        }

        private boolean roomExists(String roomName) {
            return getRoomByName(roomName) != null;
        }

        private Room getRoomByName(String roomName) {
            for (Room room : rooms) {
                if (room.getRoomName().equals(roomName)) {
                    return room;
                }
            }
            return null;
        }
    }
}