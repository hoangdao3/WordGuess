package org.example.wordgame.models;

import java.net.Socket;

public class User {
    private int id;
    private String username;
    private String password;
    private int points;
    private Socket socket;
    private String roomName; // Add this field to store the room name

    public User(int id, String username, String password, int points) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.points = points;
    }

    public User(int id, String username, String password, Socket socket) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.socket = socket; // Initialize socket
    }

    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter and setter for socket
    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    // Getter and setter for roomName
    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}