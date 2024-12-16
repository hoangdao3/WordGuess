package org.example.wordgame.models;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private int id;
    private String roomName;
    private int currentPlayers;
    private List<User> participants;

    public Room(String roomName) {
        this.roomName = roomName;
        this.participants = new ArrayList<>();
        this.currentPlayers = 0;
    }

    public Room(int id, String roomName) { // constructor for initializing a new room game
        this.id = id;
        this.roomName = roomName;
        this.currentPlayers = 0;
        this.participants = new ArrayList<>();
    }

    public Room() {
        this.participants = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    // Method to add a user to the room
    public void addUser (User user) {
        participants.add(user);
        currentPlayers++;
    }

    // Method to remove a user from the room
    public void removeUser (User user) {
        if (participants.remove(user)) {
            currentPlayers--;
        }
    }

    // Method to get the name of the room
    public String getName() {
        return roomName;
    }
}