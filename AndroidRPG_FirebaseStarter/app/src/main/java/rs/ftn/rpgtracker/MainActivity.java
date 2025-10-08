
package rs.ftn.rpgtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;


public class MainActivity extends AppCompatActivity {
  private final ActivityResultLauncher<String> notifPerm =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { /* opcionalno: Toast */ });

  Button btnProfile,btnShop,btnFriends,btnChat,btnLogout, btnTask, btnCategory;

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
    btnCategory = findViewById(R.id.btnCategory);
    btnTask = findViewById(R.id.btnTask);
    Button btnEquipment = findViewById(R.id.btnEquipment);
    btnEquipment.setOnClickListener(v -> startActivity(new Intent(this, EquipmentActivity.class)));
    Button btnStats = findViewById(R.id.btnStatistics);
    btnStats.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));

    btnProfile.setOnClickListener(v->startActivity(new Intent(this, ProfileActivity.class)));
    btnShop.setOnClickListener(v->startActivity(new Intent(this, ShopActivity.class)));
    btnFriends.setOnClickListener(v->startActivity(new Intent(this, FriendsActivity.class)));
    btnChat.setOnClickListener(v->startActivity(new Intent(this, ChatActivity.class)));
    btnLogout.setOnClickListener(v->{ Prefs.clear(this); FirebaseAuth.getInstance().signOut(); Toast.makeText(this,"Logged out",Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); finish(); });
    btnTask.setOnClickListener(v->startActivity(new Intent(this, TaskActivity.class)));
    btnCategory.setOnClickListener(v->startActivity(new Intent(this, CategoryActivity.class)));

    //LiveNotifications.stop();
  }
}
