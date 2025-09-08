package rs.ftn.rpgtracker;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    Button btnLogin, btnGoRegister;
    AppDbHelper helper;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        helper = new AppDbHelper(this);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        if (Prefs.getLoggedUserId(this) > 0) {
            startActivity(new Intent(this, MainActivity.class));
            finish(); return;
        }
        btnLogin.setOnClickListener(v -> doLogin());
        btnGoRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();
        if (TextUtils.isEmpty(e) || TextUtils.isEmpty(p)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show(); return;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,password,email_verified FROM users WHERE email=?", new String[]{e});
        if (c.moveToFirst()) {
            String realPass = c.getString(1);
            int verified = c.getInt(2);
            if (!p.equals(realPass)) {
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
            } else if (verified == 0) {
                Toast.makeText(this, "Email not verified (demo). Re-register to simulate verify.", Toast.LENGTH_LONG).show();
            } else {
                Prefs.setLoggedUserId(this, c.getLong(0));
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } else {
            Toast.makeText(this, "No user with that email", Toast.LENGTH_SHORT).show();
        }
        c.close();
    }
}