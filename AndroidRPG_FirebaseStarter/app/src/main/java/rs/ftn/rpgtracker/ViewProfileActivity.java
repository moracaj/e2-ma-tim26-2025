package rs.ftn.rpgtracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class ViewProfileActivity extends AppCompatActivity {

    public static final String EXTRA_VIEW_UID = "view_uid";

    TextView tvUsername, tvTitle, tvLevel, tvXp, tvBadges;
    TextView tvEquipWeapon, tvEquipArmor, tvEquipAccessory;
    ImageView ivAvatar, ivQr;
    Button btnAddFriend;

    String myUid;
    String targetUid;
    String targetUsername = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvUsername = findViewById(R.id.tvUsername);
        tvTitle    = findViewById(R.id.tvTitle);
        tvLevel    = findViewById(R.id.tvLevel);
        tvXp       = findViewById(R.id.tvXp);
        tvBadges   = findViewById(R.id.tvBadges);
        tvEquipWeapon = findViewById(R.id.tvEquipWeapon);
        tvEquipArmor  = findViewById(R.id.tvEquipArmor);
        tvEquipAccessory = findViewById(R.id.tvEquipAccessory);
        ivAvatar   = findViewById(R.id.ivAvatar);
        ivQr       = findViewById(R.id.ivQr);
        btnAddFriend = findViewById(R.id.btnAddFriend);

        myUid = Prefs.getUid(this);
        Intent in = getIntent();
        targetUid = (in != null) ? in.getStringExtra(EXTRA_VIEW_UID) : null;
        if (TextUtils.isEmpty(targetUid)) { Toast.makeText(this, "No user id provided", Toast.LENGTH_SHORT).show(); finish(); return; }
        if (targetUid.equals(myUid)) btnAddFriend.setVisibility(Button.GONE);

        FirebaseFirestore.getInstance().collection("users").document(targetUid).get()
                .addOnSuccessListener(this::bindUserSafe)
                .addOnFailureListener(e -> Toast.makeText(this, "Load error: " + e.getMessage(), Toast.LENGTH_LONG).show());

        btnAddFriend.setOnClickListener(v -> addFriend());
    }

    private void bindUserSafe(DocumentSnapshot doc){
        if (!doc.exists()) { Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show(); finish(); return; }

        try {
            // osnovna polja
            targetUsername = safeStr(doc.getString("username"));
            String title   = safeStr(doc.getString("title"));
            Long levelL    = asLong(doc.get("level"));
            Long xpL       = asLong(doc.get("xp"));
            int level = levelL == null ? 1 : levelL.intValue();
            int xp    = xpL    == null ? 0 : xpL.intValue();

            tvUsername.setText(targetUsername);
            tvTitle.setText(TextUtils.isEmpty(title) ? "—" : title);
            tvLevel.setText("Level: " + level);
            tvXp.setText("XP: " + xp);

            // ✅ AVATAR po tvom modelu: doc["avatar"] je ime drawable-a (npr. "avatar_blue")
            String avatarKey = doc.getString("avatar");
            if (avatarKey == null || avatarKey.trim().isEmpty()) avatarKey = "avatar_blue";
            int resId = getResources().getIdentifier(avatarKey, "drawable", getPackageName());
            if (resId == 0) resId = R.drawable.avatar_blue;
            ivAvatar.setImageResource(resId);

            // ✅ QR: koristimo isto što koristiš u ProfileActivity — username
            try {
                Bitmap bmp = QRCodeUtil.generate(targetUsername, 800);
                ivQr.setImageBitmap(bmp);
            } catch (Exception ignore) {}

            // bedževi (listu ili string pretvori u CSV)
            Object badgesObj = doc.get("badges");
            List<String> badges = new ArrayList<>();
            if (badgesObj instanceof List) for (Object o : (List<?>) badgesObj) badges.add(String.valueOf(o));
            else if (badgesObj instanceof String) badges.add((String) badgesObj);
            tvBadges.setText(badges.isEmpty() ? "—" : TextUtils.join(", ", badges));

            // oprema (mapa weapon/armor/accessory)
            String weapon="", armor="", accessory="";
            Object eqObj = doc.get("equipment");
            if (eqObj instanceof Map) {
                Map<?,?> m = (Map<?,?>) eqObj;
                weapon = safeStr(String.valueOf(m.get("weapon")));
                armor  = safeStr(String.valueOf(m.get("armor")));
                accessory = safeStr(String.valueOf(m.get("accessory")));
            }
            tvEquipWeapon.setText("Weapon: " + (TextUtils.isEmpty(weapon) ? "—" : weapon));
            tvEquipArmor.setText("Armor: "   + (TextUtils.isEmpty(armor)  ? "—" : armor));
            tvEquipAccessory.setText("Accessory: " + (TextUtils.isEmpty(accessory) ? "—" : accessory));

        } catch (Exception ex) {
            Toast.makeText(this, "Profile parse warning: " + ex.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addFriend(){
        if (TextUtils.isEmpty(myUid)) { Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show(); return; }
        if (targetUid.equals(myUid)) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(myUid).get().addOnSuccessListener(meDoc -> {
            String myUsername = safeStr(meDoc.getString("username"));

            com.google.firebase.firestore.WriteBatch b = db.batch();
            b.set(db.collection("users").document(myUid)
                            .collection("friends").document(targetUid),
                    friendDoc(targetUid, targetUsername));
            b.set(db.collection("users").document(targetUid)
                            .collection("friends").document(myUid),
                    friendDoc(myUid, myUsername));

            b.commit()
                    .addOnSuccessListener(x -> Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }).addOnFailureListener(e -> Toast.makeText(this, "Load me failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private static Map<String,Object> friendDoc(String uid, String username){
        Map<String,Object> m = new HashMap<>();
        m.put("uid", uid);
        m.put("username", username);
        m.put("createdAt", Timestamp.now());
        return m;
    }

    private String safeStr(String s){ return s == null ? "" : s; }
    private Long asLong(Object o){ if (o instanceof Number) return ((Number)o).longValue(); try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
