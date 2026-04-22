package auth;

import database.DBConnection;
import models.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class AuthService {

    public static User login(String username, String password) {

        if(username == null || password == null){
            return null;
        }

        String cleanUsername = username.trim();

        if(cleanUsername.isEmpty() || password.isEmpty()){
            return null;
        }

        ensureUsersTableExists();

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement(
                    "SELECT username,password FROM users WHERE username=? LIMIT 1"
                )
        ){
            pst.setString(1, cleanUsername);

            try(ResultSet rs = pst.executeQuery()){
                if(rs.next()){
                    String storedPassword = rs.getString("password");
                    String hashedInput = hashPasswordForStorage(password);

                    if(hashedInput.equalsIgnoreCase(storedPassword)){
                        return new User(rs.getString("username"));
                    }

                    // Backward compatibility: accept legacy plaintext then migrate to hash.
                    if(password.equals(storedPassword)){
                        migrateLegacyPasswordToHash(cleanUsername, hashedInput);
                        return new User(rs.getString("username"));
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private static void ensureUsersTableExists(){

        try(
                Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement()
        ){
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(100) NOT NULL UNIQUE," +
                "password VARCHAR(255) NOT NULL" +
                ")"
            );
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void migrateLegacyPasswordToHash(String username, String hashedPassword){

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement("UPDATE users SET password=? WHERE username=?")
        ){
            pst.setString(1, hashedPassword);
            pst.setString(2, username);
            pst.executeUpdate();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static String hashPasswordForStorage(String plainPassword){

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for(byte b : hashBytes){
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        }catch(Exception e){
            throw new RuntimeException("Failed to hash password", e);
        }
    }

}