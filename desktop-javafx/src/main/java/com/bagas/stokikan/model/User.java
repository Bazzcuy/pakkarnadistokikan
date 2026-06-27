package com.bagas.stokikan.model;

public class User {
    private final int id;
    private final String nama;
    private final String username;
    private final String role;

    public User(int id, String nama, String username, String role) {
        this.id = id;
        this.nama = nama;
        this.username = username;
        this.role = role;
    }

    public int getId() { return id; }
    public String getNama() { return nama; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return nama + " (" + role + ")";
    }
}
