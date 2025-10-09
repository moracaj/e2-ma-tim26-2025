package rs.ftn.rpgtracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BattleActivity extends AppCompatActivity implements SensorEventListener {

    private ProgressBar bossHpBar, userPpBar;
    private TextView tvBossHp, tvUserPp, tvInfo, tvResult;
    private ImageView imgBoss, imgChest, imgEquipment;
    private Button btnAttack;

    private FirebaseFirestore db;
    private String uid;

    // Boss parametri
    private int bossLevel = 1;
    private int bossHp;
    private int bossMaxHp;
    private int triesLeft = 5;

    // Korisnik parametri
    private int userPp = 100;
    private int successChance = 70; // šansa da napad uspe
    private boolean battleEnded = false;

    // Senzor za shake
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShake = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // inicijalizacija UI elemenata
        bossHpBar = findViewById(R.id.bossHpBar);
        userPpBar = findViewById(R.id.userPpBar);
        tvBossHp = findViewById(R.id.tvBossHp);
        tvUserPp = findViewById(R.id.tvUserPp);
        tvInfo = findViewById(R.id.tvInfo);
        tvResult = findViewById(R.id.tvResult);
        imgBoss = findViewById(R.id.imgBoss);
        imgChest = findViewById(R.id.imgChest);
        imgEquipment = findViewById(R.id.imgEquipment);
        btnAttack = findViewById(R.id.btnAttack);

        // senzori za "shake"
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // učitaj korisnika iz baze
        loadUserData();

        btnAttack.setOnClickListener(v -> performAttack());
    }

    private void loadUserData() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Long levelLong = doc.getLong("level");
            bossLevel = (levelLong != null) ? levelLong.intValue() : 1;

            Long ppLong = doc.getLong("pp");
            userPp = (ppLong != null) ? ppLong.intValue() : 100;

            // Boss HP formula: HP = 200 * 2.5^(level - 1)
            bossMaxHp = (int) (200 * Math.pow(2.5, bossLevel - 1));
            bossHp = bossMaxHp;

            bossHpBar.setMax(bossMaxHp);
            bossHpBar.setProgress(bossHp);

            userPpBar.setMax(500);
            userPpBar.setProgress(userPp);

            successChance = 70; // kasnije možeš računati iz taskova

            updateInfo();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void performAttack() {
        if (battleEnded || triesLeft <= 0) return;

        triesLeft--;
        updateInfo();

        Random r = new Random();
        int roll = r.nextInt(100);
        boolean hit = roll < successChance;

        if (hit) {
            bossHp -= userPp;
            if (bossHp < 0) bossHp = 0;
            bossHpBar.setProgress(bossHp);
            tvBossHp.setText("Boss HP: " + bossHp + "/" + bossMaxHp);

            imgBoss.setImageResource(R.drawable.boss_hit);
            imgBoss.postDelayed(() -> imgBoss.setImageResource(R.drawable.boss_idle), 400);

            // vibracija
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(150);

            Toast.makeText(this, "Hit!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Miss!", Toast.LENGTH_SHORT).show();
        }

        if (bossHp <= 0 || triesLeft == 0) {
            endBattle();
        }
    }

    private void endBattle() {
        battleEnded = true;

        int coins = calculateReward();
        int xpGained = bossHp <= 0 ? 100 : 50;

        tvResult.setText("Battle ended! You earned " + coins + " coins.");

        imgChest.setVisibility(ImageView.VISIBLE);
        imgChest.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        imgChest.setOnClickListener(v -> openChest(coins));

        // ažuriraj korisnika u Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("coins", FieldValue.increment(coins));
        updates.put("xp", FieldValue.increment(xpGained));
        updates.put("canFightBoss", false);

        if (bossHp <= 0) {
            updates.put("level", FieldValue.increment(1));
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(a -> Toast.makeText(this, "Data updated!", Toast.LENGTH_SHORT).show());
        EquipmentOps.settleAfterBoss(this);

        if (bossHp <= 0) {
            db.collection("users").document(uid)
                    .update("canFightBoss", false);
        }

    }

    private void openChest(int coins) {
        imgChest.setImageResource(R.drawable.chest_open);
        tvResult.setText("You found " + coins + " coins!");

        Random random = new Random();

        // šansa 20% za opremu
        if (random.nextInt(100) < 20) {
            boolean weapon = random.nextInt(100) < 5;
            String itemName = weapon ? "Sword of Destiny" : "Mystic Cloak";

            // update Firestore: dodaj item
            Map<String, Object> item = new HashMap<>();
            item.put("name", itemName);
            item.put("type", weapon ? "weapon" : "armor");
            item.put("power", weapon ? 30 : 15);

            db.collection("users").document(uid)
                    .collection("equipment")
                    .add(item);

            tvResult.append("\nYou also found: " + itemName);

            imgEquipment.setVisibility(ImageView.VISIBLE);
            imgEquipment.setImageResource(weapon ? R.drawable.sword_icon : R.drawable.cloak_icon);
        }
    }

    private int calculateReward() {
        double baseCoins = 200 * Math.pow(1.2, bossLevel - 1);

        if (bossHp <= 0) {
            // boss poražen → puna nagrada
            return (int) baseCoins;
        } else if (bossHp <= bossMaxHp / 2) {
            // pola HP-a skinuto → pola nagrade
            return (int) (baseCoins / 2);
        } else {
            // boss prejak → bez nagrade
            return 0;
        }
    }

    private void updateInfo() {
        tvBossHp.setText("Boss HP: " + bossHp + "/" + bossMaxHp);
        tvUserPp.setText("Your PP: " + userPp);
        tvInfo.setText("Chance: " + successChance + "%  |  Tries left: " + triesLeft + "/5");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double acceleration = Math.sqrt(x * x + y * y + z * z);
        if (acceleration > 15) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShake > 700) {
                lastShake = currentTime;
                performAttack();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
