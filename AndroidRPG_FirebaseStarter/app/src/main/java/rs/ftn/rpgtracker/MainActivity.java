
package rs.ftn.rpgtracker;
import android.content.Intent; import android.os.Bundle; import android.widget.Button; import android.widget.Toast;
import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {
  private final ActivityResultLauncher<String> notifPerm =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { /* opcionalno: Toast */ });

  Button btnProfile,btnShop,btnFriends,btnChat,btnLogout;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Notifications.createChannels(this);
    if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
      registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        LiveNotifications.start(getApplicationContext());
      }).launch(Manifest.permission.POST_NOTIFICATIONS);
    } else {
      LiveNotifications.start(getApplicationContext());
    }

    if(Prefs.getUid(this)==null || FirebaseAuth.getInstance().getCurrentUser()==null){
        startActivity(new Intent(this, LoginActivity.class));
        finish();
        return;
    }
    btnProfile=findViewById(R.id.btnProfile);
    btnShop=findViewById(R.id.btnShop);
    btnFriends=findViewById(R.id.btnFriends);
    btnChat=findViewById(R.id.btnChat);
    btnLogout=findViewById(R.id.btnLogout);
    btnProfile.setOnClickListener(v->startActivity(new Intent(this, ProfileActivity.class)));
    btnShop.setOnClickListener(v->startActivity(new Intent(this, ShopActivity.class)));
    btnFriends.setOnClickListener(v->startActivity(new Intent(this, FriendsActivity.class)));
    btnChat.setOnClickListener(v->startActivity(new Intent(this, ChatActivity.class)));
    btnLogout.setOnClickListener(v->{ Prefs.clear(this); FirebaseAuth.getInstance().signOut(); Toast.makeText(this,"Logged out",Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); finish(); });
    //LiveNotifications.stop();
  }
}
