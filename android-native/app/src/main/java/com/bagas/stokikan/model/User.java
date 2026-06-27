package com.bagas.stokikan.model;

public class User {
    public final int id;
    public final String nama;
    public final String username;
    public final String role;

    public User(int id, String nama, String username, String role) {
        this.id = id;
        this.nama = nama;
        this.username = username;
        this.role = role;
    }
}
