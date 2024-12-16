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
//            sendInitialInstructions();
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] command = message.split(" ");
                    System.out.println(message);
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
                            handleLogout(username);
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
                        case "SEND_HINT":
                            handleSendHint(command);
                            break;
                        case "RECEIVE_HINT_TEXT":
                            handleReceiveHintText(command);
                            break;
                        case "RECEIVE_HINT_IMAGE":
                            handleReceiveHintImage(command);
                            break;
                        case "SEND_WORD":
                            handleSendWord(command);
                            break;
                        case "MY_POINTS":
                            handleMyPoints(username);
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

        private void sendInitialInstructions() {
            sendResponse("Register: REGISTER <username> <password>");
            sendResponse("Login: LOGIN <username> <password>");
            sendResponse("Logout: LOGOUT <username>");
            sendResponse("Change Password: CHANGE_PASSWORD <username> <old_password> <new_password>");
        }

        private void sendRoomInstructions() {
            sendResponse("List Rooms: LIST_ROOM <username>");
            sendResponse("Create Room: CREATE_ROOM <username> <room_name>");
            sendResponse("Join Room: JOIN_ROOM <username> <room_name>");
            sendResponse("Leave Room: LEAVE_ROOM <username>");
        }

        private void sendGameplayInstructions() {
            sendResponse("Send Hint: SEND_HINT \"hint\"");
            sendResponse("Receive Hint Text: RECEIVE_HINT_TEXT \"hint\"");
            sendResponse("Receive Hint Image: RECEIVE_HINT_IMAGE <image_path>");
            sendResponse("Send Word: SEND_WORD \"word\"");
            sendResponse("Check Points: MY_POINTS <username>");
        }

        private void sendResponse(String response) {
            out.println(response);
            System.out.println(response);
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
                sendResponse ("Registration failed: " + e.getMessage());
            }
        }

        private void handleLogin(String username, String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid LOGIN command.");
                return;
            }
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    currentUser  = new User(username);
                    loggedInUsers.put(username, currentUser );
                    sendResponse("Login successful.");
//                    sendRoomInstructions(); // Send room instructions after login
                } else {
                    sendResponse("Invalid username or password.");
                }
            } catch (SQLException e) {
                sendResponse("Login failed: " + e.getMessage());
            }
        }

        private void handleLogout(String username) {
            if (loggedInUsers.remove(username) != null) {
                sendResponse("Logout successful.");
            } else {
                sendResponse("User  not logged in.");
            }
        }

        private void handleChangePassword(String username, String[] command) {
            if (command.length != 4) {
                sendResponse("Invalid CHANGE_PASSWORD command.");
                return;
            }
            String oldPassword = command[2];
            String newPassword = command[3];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ? AND password = ?")) {
                stmt.setString(1, newPassword);
                stmt.setString(2, username);
                stmt.setString(3, oldPassword);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    sendResponse("Password changed successfully.");
                } else {
                    sendResponse("Old password is incorrect.");
                }
            } catch (SQLException e) {
                sendResponse("Change password failed: " + e.getMessage());
            }
        }

        private void handleListRooms(String username) {
            // Implementation for listing rooms
            sendResponse("List of rooms: " + rooms.toString());
        }

        private void handleCreateRoom(String username, String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid CREATE_ROOM command.");
                return;
            }
            String roomName = command[2];
            Room newRoom = new Room(roomName);
            rooms.add(newRoom);
            sendResponse("Room '" + roomName + "' created successfully.");
        }

        private void handleJoinRoom(String username, String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid JOIN_ROOM command.");
                return;
            }
            String roomName = command[2];
            for (Room room : rooms) {
                if (room.getRoomName().equals(roomName)) {
                    currentRoom = room;
                    sendResponse("You have joined the room: " + roomName);
//                    sendGameplayInstructions(); // Send gameplay instructions after joining a room
                    return;
                }
            }
            sendResponse("Room '" + roomName + "' does not exist.");
        }

        private void handleLeaveRoom(String username) {
            if (currentRoom != null) {
                currentRoom.removeUser (currentUser );
                sendResponse("You have left the room: " + currentRoom.getName());
                currentRoom = null;
            } else {
                sendResponse("You are not in any room.");
            }
        }

        private void handleSendHint(String[] command) {
            // Implementation for sending hints
            sendResponse("Hint sent: " + String.join(" ", Arrays.copyOfRange(command, 1, command.length)));
        }

        private void handleReceiveHintText(String[] command) {
            // Implementation for receiving hint text
            sendResponse("Received hint text: " + String.join(" ", Arrays.copyOfRange(command, 1, command.length)));
        }

        private void handleReceiveHintImage(String[] command) {
            // Implementation for receiving hint image
            sendResponse("Received hint image: " + command[1]);
        }

        private void handleSendWord(String[] command) {
            // Implementation for sending words
            sendResponse("Word sent: " + String.join(" ", Arrays.copyOfRange(command, 1, command.length)));
        }

        private void handleMyPoints(String username) {
            // Implementation for checking points
            sendResponse("Your points: 100"); // Example response
        }
    }
}