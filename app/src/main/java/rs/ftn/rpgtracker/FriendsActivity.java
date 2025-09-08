package rs.ftn.rpgtracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {
    AppDbHelper helper; long userId;
    EditText etFriend, etAllianceName; Button btnAddFriend, btnCreateAlliance;
    ListView listFriends; TextView tvAllianceInfo;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        helper = new AppDbHelper(this); userId = Prefs.getLoggedUserId(this);
        etFriend = findViewById(R.id.etFriend); etAllianceName = findViewById(R.id.etAllianceName);
        btnAddFriend = findViewById(R.id.btnAddFriend); btnCreateAlliance = findViewById(R.id.btnCreateAlliance);
        listFriends = findViewById(R.id.listFriends); tvAllianceInfo = findViewById(R.id.tvAllianceInfo);
        btnAddFriend.setOnClickListener(v -> addFriend());
        btnCreateAlliance.setOnClickListener(v -> createOrJoinAlliance());
        refresh();
    }

    private void addFriend() {
        String username = etFriend.getText().toString().trim();
        if (username.isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return; }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM users WHERE username=?", new String[]{username});
        if (!c.moveToFirst()) { Toast.makeText(this, "User not found (napravi 2. nalog za test)", Toast.LENGTH_SHORT).show(); c.close(); return; }
        long friendId = c.getLong(0); c.close();
        SQLiteDatabase w = helper.getWritableDatabase();
        ContentValues cv = new ContentValues(); cv.put("user_id", userId); cv.put("friend_user_id", friendId);
        w.insert("friends", null, cv);
        Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<String> friends = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT u.username FROM friends f JOIN users u ON u.id=f.friend_user_id WHERE f.user_id=?",
                new String[]{String.valueOf(userId)});
        while (c.moveToNext()) friends.add(c.getString(0)); c.close();
        listFriends.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friends));

        Cursor a = db.rawQuery("SELECT a.id, a.name FROM alliance_members am JOIN alliances a ON a.id = am.alliance_id WHERE am.user_id=?",
                new String[]{String.valueOf(userId)});
        if (a.moveToFirst()) tvAllianceInfo.setText("Alliance: " + a.getString(1) + " (id " + a.getLong(0) + ")"); else tvAllianceInfo.setText("No alliance");
        a.close();
    }

    private void createOrJoinAlliance() {
        String name = etAllianceName.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "Enter alliance name", Toast.LENGTH_SHORT).show(); return; }
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM alliances WHERE name=?", new String[]{name});
        long aid;
        if (c.moveToFirst()) { aid = c.getLong(0); }
        else {
            ContentValues cv = new ContentValues(); cv.put("name", name); cv.put("leader_id", userId);
            aid = db.insert("alliances", null, cv);
        }
        c.close();
        db.execSQL("DELETE FROM alliance_members WHERE user_id=?", new Object[]{userId});
        ContentValues m = new ContentValues(); m.put("alliance_id", aid); m.put("user_id", userId);
        db.insert("alliance_members", null, m);
        Toast.makeText(this, "Joined alliance " + name, Toast.LENGTH_SHORT).show();
        refresh();
    }
}