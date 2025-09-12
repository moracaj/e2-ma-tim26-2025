
package rs.ftn.rpgtracker;
import android.graphics.Bitmap; import android.os.Bundle; import android.widget.*;
import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference; import com.google.firebase.firestore.DocumentSnapshot; import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap; import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;


public class ProfileActivity extends AppCompatActivity {
  FirebaseFirestore db;
  String uid;
  ImageView imgAvatar,imgQr;
  TextView tvUsername,tvTitle,tvLevel,tvXP,tvPP,tvCoins;
  Button btnGainXP,btnChangePassword;
  FirebaseAuth auth;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile);
    db=FirebaseFirestore.getInstance();
    auth=FirebaseAuth.getInstance();
    uid=Prefs.getUid(this);
    imgAvatar=findViewById(R.id.imgAvatar);
    imgQr=findViewById(R.id.imgQr);
    tvUsername=findViewById(R.id.tvUsername);
    tvTitle=findViewById(R.id.tvTitle); tvLevel=findViewById(R.id.tvLevel); tvXP=findViewById(R.id.tvXP); tvPP=findViewById(R.id.tvPP); tvCoins=findViewById(R.id.tvCoins);
    btnGainXP=findViewById(R.id.btnGainXP);
    btnChangePassword=findViewById(R.id.btnChangePassword);
    btnGainXP.setOnClickListener(v->gainXP(50));
    btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    load();
  }
  private void load(){ db.collection("users").document(uid).get().addOnSuccessListener(this::show); }
  private void show(DocumentSnapshot doc){
    if(doc==null||!doc.exists()) return;
    String username=doc.getString("username");
    String title=doc.getString("title");
    int level=doc.getLong("level")==null?1:doc.getLong("level").intValue();
    int xp=doc.getLong("xp")==null?0:doc.getLong("xp").intValue();
    int pp=doc.getLong("pp")==null?0:doc.getLong("pp").intValue();
    int coins=doc.getLong("coins")==null?0:doc.getLong("coins").intValue();
    tvUsername.setText(username);
    tvTitle.setText("Title: "+title);
    tvLevel.setText("Level: "+level+" (need "+LevelCalculator.xpForLevel(level)+" XP)");
    tvXP.setText("XP: "+xp);
    tvPP.setText("PP: "+pp);
    tvCoins.setText("Coins: "+coins);

    String avatarKey = doc.getString("avatar");
    if (avatarKey == null || avatarKey.trim().isEmpty()) {
      avatarKey = "avatar_blue"; // default ako nije upisano u bazi
    }
    int resId = getResources().getIdentifier(avatarKey, "drawable", getPackageName());
    if (resId == 0) {
      resId = R.drawable.avatar_blue; // fallback ako nema odgovarajućeg drawabla
    }
    imgAvatar.setImageResource(resId);

    try{ Bitmap bmp=QRCodeUtil.generate(username,800); imgQr.setImageBitmap(bmp);}catch(Exception e){ e.printStackTrace(); }
  }
  private void gainXP(int amount){
    DocumentReference ref=db.collection("users").document(uid);
    ref.get().addOnSuccessListener(doc->{
      int level=doc.getLong("level")==null?1:doc.getLong("level").intValue();
      int xp=doc.getLong("xp")==null?0:doc.getLong("xp").intValue();
      int pp=doc.getLong("pp")==null?0:doc.getLong("pp").intValue();
      xp+=amount; int needed=LevelCalculator.xpForLevel(level);
      while(xp>=needed){ level+=1; pp+=LevelCalculator.ppForLevelReward(level-1); needed=LevelCalculator.xpForLevel(level); }
      Map<String,Object> upd=new HashMap<>(); upd.put("xp",xp); upd.put("pp",pp); upd.put("level",level);
      ref.update(upd).addOnSuccessListener(a->load());
    });
  }


  private void showChangePasswordDialog() {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null || user.getEmail() == null) {
      Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
      return;
    }

    final android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(this)
            .setTitle("Change password")
            .setView(getLayoutInflater().inflate(R.layout.dialog_change_password, null))
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", (d,w) -> d.dismiss())
            .create();

    dlg.setOnShowListener(li -> {
      Button b = dlg.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
      EditText etOld = dlg.findViewById(R.id.etOld);
      EditText etNew1 = dlg.findViewById(R.id.etNew1);
      EditText etNew2 = dlg.findViewById(R.id.etNew2);
      TextView tvErr = dlg.findViewById(R.id.tvErr);

      b.setOnClickListener(v -> {
        String oldPw = etOld.getText().toString().trim();
        String n1 = etNew1.getText().toString().trim();
        String n2 = etNew2.getText().toString().trim();

        // Validacija po specifikaciji: stara + 2x nova
        if (oldPw.isEmpty() || n1.isEmpty() || n2.isEmpty()) {
          tvErr.setText("All fields are required.");
          return;
        }
        if (!n1.equals(n2)) { tvErr.setText("New passwords do not match."); return; }
        if (n1.length() < 6) { tvErr.setText("New password must have at least 6 characters."); return; }
        if (n1.equals(oldPw)) { tvErr.setText("New password must be different from current."); return; }

        b.setEnabled(false); tvErr.setText("Updating...");

        // Re-authenticate sa starom lozinkom
        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), oldPw);
        user.reauthenticate(cred).addOnSuccessListener(a -> {
          // updatePassword posle uspešne reautentikacije
          user.updatePassword(n1).addOnSuccessListener(a2 -> {
            Toast.makeText(this, "Password changed.", Toast.LENGTH_SHORT).show();
            dlg.dismiss();
          }).addOnFailureListener(e -> {
            b.setEnabled(true);
            if (e instanceof FirebaseAuthWeakPasswordException) {
              tvErr.setText("Weak password. Try a stronger one.");
            } else {
              tvErr.setText("Update failed: " + e.getMessage());
            }
          });
        }).addOnFailureListener(e -> {
          b.setEnabled(true);
          if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            tvErr.setText("Please log in again and retry.");
          } else {
            tvErr.setText("Current password incorrect.");
          }
        });
      });
    });

    dlg.show();
  }



}
