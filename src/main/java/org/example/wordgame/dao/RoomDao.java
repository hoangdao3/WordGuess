package org.example.wordgame.dao;

import org.example.wordgame.models.Room;
import org.example.wordgame.utils.DatabaseConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDao {

    // Tạo phòng mới
    public static boolean createRoom(Room room) {
        String query = "INSERT INTO rooms (room_name) VALUES (?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, room.getRoomName());
            int rowsAffected = stmt.executeUpdate();

            return rowsAffected > 0; // Nếu tạo thành công, trả về true
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Nếu có lỗi, trả về false
        }
    }

    // Liệt kê tất cả các phòng hiện có
    public static List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String query = "SELECT * FROM rooms";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int roomId = rs.getInt("room_id");
                String roomName = rs.getString("room_name");
                rooms.add(new Room(roomId, roomName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rooms; // Trả về danh sách phòng
    }

    // Kiểm tra sự tồn tại của phòng theo tên
    public static boolean roomExists(String roomName) {
        String query = "SELECT COUNT(*) FROM rooms WHERE room_name = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, roomName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0; // Nếu có phòng, trả về true
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false; // Nếu không tìm thấy phòng, trả về false
    }

    // Lấy thông tin phòng theo tên
    public static Room getRoomByName(String roomName) {
        String query = "SELECT * FROM rooms WHERE room_name = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, roomName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int roomId = rs.getInt("room_id");
                String name = rs.getString("room_name");
                return new Room(roomId, name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Nếu không tìm thấy phòng, trả về null
    }
}
