
package rs.ftn.rpgtracker;
import android.os.Bundle; import android.text.TextUtils; import android.widget.*;
import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth; import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap; import java.util.Map;
public class RegisterActivity extends AppCompatActivity {
  EditText email,username,password,confirm; Button btnRegister; FirebaseAuth auth; FirebaseFirestore db;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState); setContentView(R.layout.activity_register);
    auth=FirebaseAuth.getInstance(); db=FirebaseFirestore.getInstance();
    email=findViewById(R.id.email); username=findViewById(R.id.username); password=findViewById(R.id.password); confirm=findViewById(R.id.confirm);
    btnRegister=findViewById(R.id.btnRegister); btnRegister.setOnClickListener(v->register());
  }
  private void register(){
    String e=email.getText().toString().trim(); String u=username.getText().toString().trim(); String p=password.getText().toString(); String c=confirm.getText().toString();
    if(TextUtils.isEmpty(e)||TextUtils.isEmpty(u)||TextUtils.isEmpty(p)||TextUtils.isEmpty(c)){ Toast.makeText(this,"All fields are required",Toast.LENGTH_SHORT).show(); return; }
    if(!p.equals(c)){ Toast.makeText(this,"Passwords do not match",Toast.LENGTH_SHORT).show(); return; }
    auth.createUserWithEmailAndPassword(e,p).addOnSuccessListener(res->{
      if(auth.getCurrentUser()!=null){
        auth.getCurrentUser().sendEmailVerification();
        String uid=auth.getCurrentUser().getUid();
        Map<String,Object> data=new HashMap<>();
        data.put("email",e); data.put("username",u); data.put("avatar","avatar_blue");
        data.put("xp",0); data.put("pp",0); data.put("coins",200); data.put("level",1); data.put("title","Beginner"); data.put("allianceId",null);
        db.collection("users").document(uid).set(data).addOnSuccessListener(aVoid->{ Toast.makeText(this,"Account created. Check your email to verify.",Toast.LENGTH_LONG).show(); auth.signOut(); finish(); })
           .addOnFailureListener(e2->Toast.makeText(this,e2.getMessage(),Toast.LENGTH_LONG).show());
      }
    }).addOnFailureListener(e1->Toast.makeText(this,e1.getMessage(),Toast.LENGTH_LONG).show());
  }
}
