package org.example.wordgame.models;
import org.example.wordgame.constant.GameConstants;
public class User {
    private int id;
    private String username;
    private String password;
    private int score;

    public User(String username) {
        this.username = username;
        this.score = GameConstants.INIT_PLAYER_POINTS;;
    }

    public User(int id, String username, String password, int points) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.score = points;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public User() {

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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    public void addScore(int points) {
        this.score += points;
    }

    public void subtractScore(int points) {
        this.score -= points;
    }
}
