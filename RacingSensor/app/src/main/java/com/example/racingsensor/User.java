package com.example.racingsensor;

public class User {
    private String email;
    private String password; // Lưu password đã mã hóa

    public User() {
        // Constructor mặc định cần cho Firebase
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getter và Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}