package rs.ftn.rpgtracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    AppDbHelper helper; long userId; TextView tvAlliance; ListView listMessages; EditText etMessage; Button btnSend; Long allianceId = null;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        helper = new AppDbHelper(this); userId = Prefs.getLoggedUserId(this);
        tvAlliance = findViewById(R.id.tvAlliance); listMessages = findViewById(R.id.listMessages);
        etMessage = findViewById(R.id.etMessage); btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> send());
        loadAlliance(); refresh();
    }

    private void loadAlliance() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor a = db.rawQuery("SELECT a.id, a.name FROM alliance_members am JOIN alliances a ON a.id = am.alliance_id WHERE am.user_id=?",
                new String[]{String.valueOf(userId)});
        if (a.moveToFirst()) { allianceId = a.getLong(0); tvAlliance.setText("Alliance: " + a.getString(1)); }
        else tvAlliance.setText("No alliance");
        a.close();
    }

    private void refresh() {
        if (allianceId == null) return;
        SQLiteDatabase db = helper.getReadableDatabase();
        List<String> msgs = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT m.content, m.sender_id, u.username, m.ts FROM messages m JOIN users u ON u.id=m.sender_id WHERE alliance_id=? ORDER BY ts ASC",
                new String[]{String.valueOf(allianceId)});
        while (c.moveToNext()) {
            String content = c.getString(0); long sender = c.getLong(1); String user = c.getString(2);
            msgs.add((sender == userId ? "Me: " : user + ": ") + content);
        }
        c.close();
        listMessages.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, msgs));
    }

    private void send() {
        if (allianceId == null) { Toast.makeText(this, "Join an alliance first", Toast.LENGTH_SHORT).show(); return; }
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO messages (alliance_id,sender_id,content,ts) VALUES (?,?,?,?)",
                new Object[]{allianceId, userId, content, System.currentTimeMillis()});
        etMessage.setText(""); refresh();
    }
}