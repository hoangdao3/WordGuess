package org.example.wordgame.models;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private int id;
    private String roomName;
    private int currentPlayers;
    private List<User> participants;

    public Room(int id, String roomName) { // constructor for initialize new room game
        this.id = id;
        this.roomName = roomName;
        this.currentPlayers = 0;
        this.participants = new ArrayList<>();
    }
    public Room(){

    }
}
