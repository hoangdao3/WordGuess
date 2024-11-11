package org.example.wordgame.models;

import java.time.LocalDateTime;

public class RoomParticipant {
    private int id;
    private Room room;
    private User user;
    private LocalDateTime joinedAt;

    public RoomParticipant(int id, Room room, User user){
        this.id = id;
        this.room = room;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
    }
}
