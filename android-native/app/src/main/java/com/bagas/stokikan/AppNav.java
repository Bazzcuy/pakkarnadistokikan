package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;

import com.bagas.stokikan.model.User;

public class AppNav {
    public static void putUser(Intent intent, User user) {
        intent.putExtra("user_id", user.id);
        intent.putExtra("nama", user.nama);
        intent.putExtra("username", user.username);
        intent.putExtra("role", user.role);
    }

    public static User readUser(Activity activity) {
        Intent i = activity.getIntent();
        int id = i.getIntExtra("user_id", 0);
        String nama = i.getStringExtra("nama");
        String username = i.getStringExtra("username");
        String role = i.getStringExtra("role");
        if (nama == null) nama = "Pengguna";
        if (username == null) username = "user";
        if (role == null) role = "PENGGUNA";
        return new User(id, nama, username, role);
    }

    public static void open(Activity from, Class<?> target, User user) {
        Intent intent = new Intent(from, target);
        putUser(intent, user);
        from.startActivity(intent);
    }

    public static boolean allow(User user, String... roles) {
        if (user == null || user.role == null) return false;
        if ("PENGGUNA".equalsIgnoreCase(user.role)) return true;
        for (String role : roles) if (role.equalsIgnoreCase(user.role)) return true;
        return false;
    }
}
