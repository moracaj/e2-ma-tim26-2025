package rs.ftn.rpgtracker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    AppDbHelper helper;
    long userId;
    ImageView imgAvatar, imgQr;
    TextView tvUsername, tvTitle, tvLevel, tvXP, tvPP, tvCoins;
    Button btnGainXP, btnChangePassword;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        helper = new AppDbHelper(this);
        userId = Prefs.getLoggedUserId(this);
        imgAvatar = findViewById(R.id.imgAvatar);
        imgQr = findViewById(R.id.imgQr);
        tvUsername = findViewById(R.id.tvUsername);
        tvTitle = findViewById(R.id.tvTitle);
        tvLevel = findViewById(R.id.tvLevel);
        tvXP = findViewById(R.id.tvXP);
        tvPP = findViewById(R.id.tvPP);
        tvCoins = findViewById(R.id.tvCoins);
        btnGainXP = findViewById(R.id.btnGainXP);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnGainXP.setOnClickListener(v -> {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("UPDATE users SET xp = xp + 50 WHERE id = ?", new Object[]{userId});
            recalcLevel();
            load();
        });
        btnChangePassword.setOnClickListener(v -> {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("UPDATE users SET password = 'newpass' WHERE id = ?", new Object[]{userId});
            Toast.makeText(this, "Password changed to 'newpass' (demo)", Toast.LENGTH_SHORT).show();
        });
        load();
    }

    private void recalcLevel() {
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT level, xp, pp FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        if (c.moveToFirst()) {
            int level = c.getInt(0);
            int xp = c.getInt(1);
            int pp = c.getInt(2);
            int needed = LevelCalculator.xpForLevel(level);
            while (xp >= needed) {
                level += 1;
                int rewardPrev = LevelCalculator.ppForLevelReward(level - 1);
                pp += rewardPrev;
                needed = LevelCalculator.xpForLevel(level);
            }
            db.execSQL("UPDATE users SET level=?, pp=? WHERE id=?", new Object[]{level, pp, userId});
        }
        c.close();
    }

    private void load() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT username, title, level, xp, pp, coins, avatar FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        if (c.moveToFirst()) {
            String username = c.getString(0);
            String title = c.getString(1);
            int level = c.getInt(2);
            int xp = c.getInt(3);
            int pp = c.getInt(4);
            int coins = c.getInt(5);
            String avatar = c.getString(6);

            int resId = getResources().getIdentifier(avatar, "drawable", getPackageName());
            imgAvatar.setImageResource(resId);
            tvUsername.setText(username);
            tvTitle.setText("Title: " + title);
            tvLevel.setText("Level: " + level + " (need " + LevelCalculator.xpForLevel(level) + " XP)");
            tvXP.setText("XP: " + xp);
            tvPP.setText("PP: " + pp);
            tvCoins.setText("Coins: " + coins);

            try {
                Bitmap bmp = QRCodeUtil.generate(username, 800);
                imgQr.setImageBitmap(bmp);
            } catch (Exception e) { e.printStackTrace(); }
        }
        c.close();
    }
}