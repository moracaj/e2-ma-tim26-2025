package rs.ftn.rpgtracker;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    EditText email, username, password, confirm;
    RadioGroup avatarGroup;
    Button btnRegister;
    AppDbHelper helper;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        helper = new AppDbHelper(this);
        email = findViewById(R.id.email);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        confirm = findViewById(R.id.confirm);
        avatarGroup = findViewById(R.id.avatarGroup);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> register());
    }

    private String selectedAvatar() {
        int id = avatarGroup.getCheckedRadioButtonId();
        if (id == R.id.avRed) return "avatar_red";
        if (id == R.id.avBlue) return "avatar_blue";
        if (id == R.id.avGreen) return "avatar_green";
        if (id == R.id.avPurple) return "avatar_purple";
        if (id == R.id.avOrange) return "avatar_orange";
        return "avatar_blue";
    }

    private void register() {
        String e = email.getText().toString().trim();
        String u = username.getText().toString().trim();
        String p = password.getText().toString();
        String c = confirm.getText().toString();
        String av = selectedAvatar();

        if (TextUtils.isEmpty(e) || TextUtils.isEmpty(u) || TextUtils.isEmpty(p) || TextUtils.isEmpty(c)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show(); return;
        }
        if (!p.equals(c)) { Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show(); return; }

        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("email", e); cv.put("password", p); cv.put("username", u); cv.put("avatar", av);
        cv.put("xp", 0); cv.put("pp", 0); cv.put("coins", 200); cv.put("level", 1); cv.put("title","Beginner"); cv.put("email_verified", 0);
        try {
            long id = db.insertOrThrow("users", null, cv);
            db.execSQL("UPDATE users SET email_verified = 1 WHERE id = ?", new Object[]{id}); // demo verify
            Toast.makeText(this, "Account created (demo verified).", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}