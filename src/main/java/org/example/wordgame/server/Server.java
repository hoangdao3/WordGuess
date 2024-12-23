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
        private boolean inGameMenu = false;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sendMainMenu();
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (currentUser  == null) {
                        handleMainMenuCommands(message);
                    } else if (inGameMenu) {
                        handleGameMenuCommands(message);
                    } else {
                        handleRoomMenuCommands(message);
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

        private void sendMainMenu() {
            sendResponse("Menu chính:");
            sendResponse("Nhập 1 để đăng nhập");
            sendResponse("Nhập 2 để đăng ký");
            sendResponse("Nhập 3 để đổi mật khẩu");
            sendResponse("Nhập 4 để thoát");
        }

        private void sendRoomMenu() {
            sendResponse("Menu phòng:");
            sendResponse("Nhập 1 để xem danh sách phòng");
            sendResponse("Nhập 2 để tạo phòng");
            sendResponse("Nhập 3 để tham gia phòng");
            sendResponse("Nhập 4 để rời phòng");
            sendResponse("Nhập 5 để đăng xuất");
        }

        private void sendGameMenu() {
            sendResponse("Menu game:");
            sendResponse("Nhập 1 để gửi gợi ý");
            sendResponse("Nhập 2 để gửi từ đoán");
            sendResponse("Nhập 3 để gửi hình ảnh gợi ý");
            sendResponse("Nhập 4 để rời phòng");
            sendResponse("Nhập 5 để đăng xuất");
        }

        private void sendResponse(String response) {
            out.println(response);
            System.out.println("Server response: " + response);
        }

        private void handleMainMenuCommands(String message) throws IOException {
            switch (message) {
                case "1":
                    handleLoginProcess();
                    break;
                case "2":
                    handleRegisterProcess();
                    break;
                case "3":
                    handleChangePasswordProcess();
                    break;
                case "4":
                    socket.close();
                    break;
                default:
                    sendResponse("Lựa chọn không hợp lệ. Vui lòng nhập lại.");
                    sendMainMenu();
                    break;
            }
        }

        private void handleRoomMenuCommands(String message) throws IOException {
            switch (message) {
                case "1":
                    handleListRooms();
                    break;
                case "2":
                    handleCreateRoomProcess();
                    break;
                case "3":
                    handleJoinRoomProcess();
                    break;
                case "4":
                    handleLeaveRoom();
                    break;
                case "5":
                    handleLogout();
                    break;
                default:
                    sendResponse("Lựa chọn không hợp lệ. Vui lòng nhập lại.");
                    sendRoomMenu();
                    break;
            }
        }
        private void handleGameMenuCommands(String message) throws IOException {
            switch (message) {
                case "1":
                    handleSendHint();
                    break;
                case "2":
                    handleSendGuess();
                    break;
                case "3":
                    handleSendImageHint();
                    break;
                case "4":
                    handleLeaveRoom();
                    break;
                case "5":
                    handleLogout();
                    break;
                default:
                    sendResponse("Lựa chọn không hợp lệ. Vui lòng nhập lại.");
                    sendGameMenu();
                    break;
            }
        }

        private boolean checkGuess(String guess) {
            return "example".equalsIgnoreCase(guess);
        }

        private void handleSendImageHint() throws IOException {
            sendResponse("Nhập đường dẫn đến hình ảnh gợi ý:");
            String imagePath = in.readLine();
            sendResponse("Hình ảnh gợi ý đã được gửi: " + imagePath);
        }

        private void handleLoginProcess() throws IOException {
            sendResponse("Vui lòng nhập username:");
            String username = in.readLine();
            sendResponse("Vui lòng nhập mật khẩu:");
            String password = in.readLine();
            handleLogin(username, password);
        }

        private void handleRegisterProcess() throws IOException {
            sendResponse("Vui lòng nhập username:");
            String username = in.readLine();
            sendResponse("Vui lòng nhập mật khẩu:");
            String password = in.readLine();
            handleRegister(username, password);
        }

        private void handleChangePasswordProcess() throws IOException {
            sendResponse("Vui lòng nhập username:");
            String username = in.readLine();
            sendResponse("Vui lòng nhập mật khẩu cũ:");
            String oldPassword = in.readLine();
            sendResponse("Vui lòng nhập mật khẩu mới:");
            String newPassword = in.readLine();
            handleChangePassword(username, oldPassword, newPassword);
        }

        private void handleChangePassword(String username, String oldPassword, String newPassword) {
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
                    sendResponse("Đổi mật khẩu thành công.");
                } else {
                    sendResponse("Mật khẩu cũ không chính xác.");
                }
            } catch (SQLException e) {
                sendResponse("Lỗi cơ sở dữ liệu.");
            }
        }

        private void handleLogin(String username, String password) {
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentUser  = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), socket);
                    loggedInUsers.put(username, currentUser );
                    sendResponse("Đăng nhập thành công. Xin chào, " + username + "!");
                    sendRoomMenu();
                } else {
                    sendResponse("Tên người dùng hoặc mật khẩu không chính xác.");
                    sendMainMenu();
                }
            } catch (SQLException e) {
                sendResponse("Lỗi cơ sở dữ liệu.");
            }
        }

        private void handleRegister(String username, String password) {
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.executeUpdate();
                sendResponse("Đăng ký thành công.");
            } catch (SQLException e) {
                sendResponse("Tên người dùng đã tồn tại.");
            }
        }

        private void handleListRooms() {
            if (rooms.isEmpty()) {
                sendResponse("Không có phòng nào khả dụng.");
            } else {
                StringBuilder response = new StringBuilder("Danh sách phòng:");
                for (Room room : rooms) {
                    response.append(" ").append(room.getRoomName()).append(",");
                }
                sendResponse(response.toString());
            }
        }

        private void handleLeaveRoom() {
            if (currentUser  == null) {
                sendResponse("Bạn chưa đăng nhập.");
                return;
            }

            Room currentRoom = getCurrentRoomOfUser (currentUser .getUsername());

            if (currentRoom == null) {
                sendResponse("Bạn không ở trong phòng nào.");
                return;
            }

            currentRoom.removeUser (currentUser .getUsername());
            sendResponse("Bạn đã rời khỏi phòng " + currentRoom.getRoomName() + ".");

            sendRoomMenu();
        }

        private Room getCurrentRoomOfUser (String username) {
            for (Room room : rooms) {
                if (room.containsUser (username)) {
                    return room;
                }
            }
            return null;
        }

        private void handleCreateRoomProcess() throws IOException {
            sendResponse("Nhập tên phòng muốn tạo:");
            String roomName = in.readLine();
            handleCreateRoom(roomName);
        }

        private void handleCreateRoom(String roomName) {
            if (rooms.size() >= GameConstants.MAX_ROOMS) {
                sendResponse("Không thể tạo phòng mới. Đã đạt giới hạn số phòng tối đa.");
                return;
            }
            if (roomExists(roomName)) {
                sendResponse("Phòng với tên này đã tồn tại.");
                return;
            }
            Room newRoom = new Room(rooms.size() + 1, roomName);
            rooms.add(newRoom);
            sendResponse("Phòng đã được tạo thành công: " + roomName);
        }

        private void handleJoinRoomProcess() throws IOException {
            sendResponse("Nhập tên phòng muốn tham gia:");
            String roomName = in.readLine();
            handleJoinRoom(roomName);
        }

        private void handleJoinRoom(String roomName) throws IOException {
            if (currentUser  == null) {
                sendResponse("Bạn chưa đăng nhập.");
                return;
            }

            Room currentRoom = getCurrentRoomOfUser (currentUser .getUsername());
            if (currentRoom != null) {
                sendResponse("Bạn đã ở trong phòng " + currentRoom.getRoomName() + ". Vui lòng rời phòng này trước khi tham gia phòng khác.");
                return;
            }

            Room room = getRoomByName(roomName);
            if (room == null) {
                sendResponse("Không tìm thấy phòng.");
            } else {
                currentUser.setRoomName(roomName);
                room.addUser (currentUser .getUsername());
                sendResponse("Bạn đã tham gia phòng " + roomName + " thành công.");
                sendGameMenu();

                if (room.getPlayers().size() >= GameConstants.START_MEMBERS) {
                    notifyRoomStart(room);
                }
                inGameMenu = true;
            }
        }

        private void handleSendHint() throws IOException {
            if (currentUser  == null) {
                sendResponse("Bạn chưa đăng nhập.");
                return;
            }

            String roomName = currentUser .getRoomName();
            if (roomName == null || roomName.isEmpty()) {
                sendResponse("Bạn không ở trong phòng nào.");
                return;
            }

            Room currentRoom = getRoomByName(roomName);

            if (currentRoom == null) {
                sendResponse("Phòng không tồn tại.");
                return;
            }

            if (!currentUser .getUsername().equals(currentRoom.getGuesser())) {
                sendResponse("Chỉ người đố từ mới có thể gửi gợi ý.");
                return;
            }

            sendResponse("Nhập gợi ý của bạn:");
            String hint = in.readLine();

            sendResponseToRoom(currentRoom.getRoomName(), "Gợi ý: " + hint);
        }

        private void handleSendGuess() throws IOException {
            sendResponse("Nhập từ bạn đoán:");
            String guess = in.readLine();
            Room currentRoom = getCurrentRoomOfUser (currentUser .getUsername());
            if (currentUser.getUsername() .equals(currentRoom.getGuesser())) {

                sendResponse("Bạn là người đố từ, không thể đoán từ của chính mình.");
                return;
            }

            boolean isCorrect = checkGuess(guess, currentRoom.getWordToGuess());
            if (isCorrect) {
                sendResponseToRoom(currentRoom.getRoomName(), currentUser .getUsername() + " đã đoán đúng từ!");
            } else {
                sendResponseToRoom(currentRoom.getRoomName(), currentUser .getUsername() + " đã đoán sai. Hãy thử lại!");
            }
        }

        private boolean checkGuess(String guess, String wordToGuess) {
            return wordToGuess.equalsIgnoreCase(guess);
        }

        private void sendResponseToRoom(String roomName, String message) {
            Room room = getRoomByName(roomName);
            for (String player : room.getPlayers()) {
                sendResponseToUser (player, message);
            }
        }
        private void notifyRoomStart(Room room) {
            String message = "Phòng " + room.getRoomName() + " đã đủ số lượng người chơi. Bắt đầu chơi!";
            for (String player : room.getPlayers()) {
                sendResponseToUser (player, message);
                if (player.equals(room.getGuesser())) {
                    sendResponseToUser (player, "Bạn là người đố từ. Từ của bạn là: " + room.getWordToGuess());
                }
            }
        }

        private void sendResponseToUser (String username, String message) {
            User user = loggedInUsers.get(username);
            if (user != null) {
                try {
                    PrintWriter userOut = new PrintWriter(user.getSocket().getOutputStream(), true);
                    userOut.println(message);
                    System.out.println("Gửi thông điệp đến " + username + ": " + message);
                } catch (IOException e) {
                    System.out.println("Không thể gửi thông điệp đến " + username + ": " + e.getMessage());
                }
            }
        }

        private boolean roomExists(String roomName) {
            return getRoomByName(roomName) != null;
        }
        private void handleLogout() {
            sendResponse("Bạn đã đăng xuất.");
            currentUser = null;
            sendMainMenu();
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