package org.example.wordgame.models;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private int id;
    private String roomName;
    private List<String> players; // Danh sách người chơi trong phòng

    public Room(int id, String roomName) {
        this.id = id;
        this.roomName = roomName;
        this.players = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getRoomName() {
        return roomName;
    }

    // Thêm người chơi vào phòng
    public void addUser (String username) {
        if (!players.contains(username)) {
            players.add(username);
        }
    }

    // Xóa người chơi khỏi phòng
    public void removeUser (String username) {
        players.remove(username);
    }

    // Kiểm tra xem người dùng có trong phòng không
    public boolean containsUser (String username) {
        return players.contains(username);
    }

    // Lấy danh sách người chơi trong phòng
    public List<String> getPlayers() {
        return players;
    }
}