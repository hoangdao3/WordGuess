package org.example.wordgame.models;
import java.time.LocalDateTime;

public class Log {
    private int id;
    private User user;
    private Room room;
    private String action;
    private String message;
    private LocalDateTime timestamp;

    public Log(int id, User user, Room room, String action, String message) {
        this.id = id;
        this.user = user;
        this.room = room;
        this.action = action;
        this.message = message;
        this.timestamp = LocalDateTime.now();
   }
   public Log(){

   }
}
