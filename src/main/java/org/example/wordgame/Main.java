package org.example.wordgame;

import org.example.wordgame.utils.DatabaseConnection;
import org.example.wordgame.utils.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        String sql = "SELECT username FROM users WHERE id = 1";
        try (Connection con = DatabaseConnectionPool.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String name = rs.getString(1);
                System.out.println(name);
            } else {
                System.out.println("No data found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
