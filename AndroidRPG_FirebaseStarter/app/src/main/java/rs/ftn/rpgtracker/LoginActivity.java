
package rs.ftn.rpgtracker;
import android.content.Intent; import android.os.Bundle; import android.text.TextUtils; import android.widget.*;
import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth; import com.google.firebase.auth.FirebaseUser;
public class LoginActivity extends AppCompatActivity {
  EditText email,password; Button btnLogin,btnGoRegister; FirebaseAuth auth;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState); setContentView(R.layout.activity_login);
    auth=FirebaseAuth.getInstance();
    email=findViewById(R.id.email); password=findViewById(R.id.password);
    btnLogin=findViewById(R.id.btnLogin); btnGoRegister=findViewById(R.id.btnGoRegister);
    if(Prefs.getUid(this)!=null && auth.getCurrentUser()!=null){ startActivity(new Intent(this, MainActivity.class)); finish(); return; }
    btnLogin.setOnClickListener(v->doLogin());
    btnGoRegister.setOnClickListener(v->startActivity(new Intent(this, RegisterActivity.class)));
  }
  private void doLogin(){
    String e=email.getText().toString().trim(); String p=password.getText().toString();
    if(TextUtils.isEmpty(e)||TextUtils.isEmpty(p)){ Toast.makeText(this,"Enter email and password",Toast.LENGTH_SHORT).show(); return; }
    auth.signInWithEmailAndPassword(e,p).addOnSuccessListener(res->{
      FirebaseUser u=auth.getCurrentUser();
      if(u==null){ Toast.makeText(this,"Login error",Toast.LENGTH_SHORT).show(); return; }
      if(!u.isEmailVerified()){ Toast.makeText(this,"Verify your email first.",Toast.LENGTH_LONG).show(); auth.signOut(); return; }
      Prefs.setUid(this,u.getUid()); startActivity(new Intent(this, MainActivity.class)); finish();
    }).addOnFailureListener(e1->Toast.makeText(this,e1.getMessage(),Toast.LENGTH_LONG).show());
  }
}
