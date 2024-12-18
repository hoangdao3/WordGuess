package org.example.wordgame.server;

import org.example.wordgame.models.Room;
import org.example.wordgame.models.User;
import org.example.wordgame.utils.DatabaseConnectionPool;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 12345;
    private static final List<Room> rooms = new ArrayList<>();
    private static final Map<String, User> loggedInUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private User currentUser ;
        private Room currentRoom;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    processCommand(message);
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        private void processCommand(String message) {
            String[] command = message.split(" ", 3);
            if (command.length < 1) {
                sendResponse("Invalid command format.");
                return;
            }

            String commandType = command[0].toUpperCase();
            String username = currentUser  != null ? currentUser .getUsername() : "";

            try {
                switch (commandType) {
                    case "REGISTER":
                        handleRegister(command);
                        break;
                    case "LOGIN":
                        handleLogin(command);
                        break;
                    case "LOGOUT":
                        handleLogout(username);
                        break;
                    case "LIST_ROOM":
                        handleListRooms(username);
                        break;
                    case "CREATE_ROOM":
                        handleCreateRoom(command);
                        break;
                    case "JOIN_ROOM":
                        handleJoinRoom(command);
                        break;
                    case "LEAVE_ROOM":
                        handleLeaveRoom(username);
                        break;
                    default:
                        sendResponse("Unknown command: " + commandType);
                }
            } catch (Exception e) {
                sendResponse("Error processing command: " + e.getMessage());
            }
        }

        private void handleRegister(String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid REGISTER command. Use: REGISTER <username> <password>");
                return;
            }

            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO users (username, password) VALUES (?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.executeUpdate();
                sendResponse("Registration successful for " + username);
            } catch (SQLException e) {
                sendResponse("Registration failed: " + e.getMessage());
            }
        }

        private void handleLogin(String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid LOGIN command. Use: LOGIN <username> <password>");
                return;
            }

            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT * FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        currentUser  = new User(username);
                        loggedInUsers.put(username, currentUser );
                        sendResponse("Login successful for " + username);
                    } else {
                        sendResponse(" Invalid username or password");
                    }
                }
            } catch (SQLException e) {
                sendResponse("Login failed: " + e.getMessage());
            }
        }

        private void handleLogout(String username) {
            if (loggedInUsers.remove(username) != null) {
                currentUser   = null;
                currentRoom = null;
                sendResponse("Logout successful for " + username);
            } else {
                sendResponse("User  not logged in");
            }
        }

        private void handleListRooms(String username) {
            if (username.isEmpty()) {
                sendResponse("Please login first");
                return;
            }

            if (rooms.isEmpty()) {
                sendResponse("No rooms available");
            } else {
                StringBuilder roomList = new StringBuilder("Available Rooms: ");
                for (Room room : rooms) {
                    roomList.append(room.getRoomName()).append(" ");
                }
                sendResponse(roomList.toString());
            }
        }

        private void handleCreateRoom(String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid CREATE_ROOM command. Use: CREATE_ROOM <username> <room_name>");
                return;
            }

            String roomName = command[2];

            for (Room room : rooms) {
                if (room.getRoomName().equalsIgnoreCase(roomName)) {
                    sendResponse("Room '" + roomName + "' already exists. Please choose a different name.");
                    return;
                }
            }

            Room newRoom = new Room(roomName);
            rooms.add(newRoom);
            sendResponse("Room '" + roomName + "' created successfully.");
        }

        private void handleJoinRoom(String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid JOIN_ROOM command. Use: JOIN_ROOM <username> <room_name>");
                return;
            }

            String username = command[1];
            String roomName = command[2];

            for (Room room : rooms) {
                if (room.getRoomName().equals(roomName)) {
                    currentRoom = room;
                    sendResponse("Joined room: " + roomName);
                    return;
                }
            }
            sendResponse("Room '" + roomName + "' not found");
        }

        private void handleLeaveRoom(String username) {
            if (currentRoom != null) {
                currentRoom = null;
                sendResponse("Left the current room");
            } else {
                sendResponse("You are not in any room");
            }
        }

        private void sendResponse(String response) {
            out.println(response);
            System.out.println("Sent: " + response);
        }
    }
}