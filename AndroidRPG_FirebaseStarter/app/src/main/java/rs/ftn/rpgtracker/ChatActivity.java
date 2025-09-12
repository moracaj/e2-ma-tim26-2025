
package rs.ftn.rpgtracker;
import android.os.Bundle; import android.widget.*;
import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp; import com.google.firebase.firestore.*;
import java.util.ArrayList; import java.util.HashMap; import java.util.List; import java.util.Map;
public class ChatActivity extends AppCompatActivity {
  FirebaseFirestore db; String uid; String allianceId=null; String username="";
  TextView tvAlliance; ListView listMessages; EditText etMessage; Button btnSend; ListenerRegistration reg;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState); setContentView(R.layout.activity_chat);
    db=FirebaseFirestore.getInstance(); uid=Prefs.getUid(this);
    tvAlliance=findViewById(R.id.tvAlliance); listMessages=findViewById(R.id.listMessages); etMessage=findViewById(R.id.etMessage); btnSend=findViewById(R.id.btnSend);
    btnSend.setOnClickListener(v->send()); loadMeAndAlliance();
  }
  @Override protected void onDestroy(){ super.onDestroy(); if(reg!=null) reg.remove(); }
  private void loadMeAndAlliance(){
    db.collection("users").document(uid).get().addOnSuccessListener(doc->{ username=doc.getString("username"); allianceId=doc.getString("allianceId");
      if(allianceId==null||allianceId.isEmpty()){ tvAlliance.setText("No alliance"); }
      else { db.collection("alliances").document(allianceId).get().addOnSuccessListener(aDoc-> tvAlliance.setText("Alliance: "+aDoc.getString("name"))); listenMessages(); }
    });
  }
  private void listenMessages(){
    if(allianceId==null) return;
    reg=db.collection("alliances").document(allianceId).collection("messages").orderBy("ts", Query.Direction.ASCENDING).addSnapshotListener((snap,e)->{
      if(snap==null) return; List<String> msgs=new ArrayList<>();
      for(DocumentSnapshot d:snap.getDocuments()){ String content=d.getString("content"); String senderName=d.getString("senderName"); String senderUid=d.getString("senderUid"); msgs.add((uid.equals(senderUid)?"Me: ":senderName+": ")+content); }
      listMessages.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, msgs));
    });
  }
  private void send(){
    if(allianceId==null){ Toast.makeText(this,"Join an alliance first",Toast.LENGTH_SHORT).show(); return; }
    String content=etMessage.getText().toString().trim(); if(content.isEmpty()) return;
    Map<String,Object> msg=new HashMap<>(); msg.put("senderUid",uid); msg.put("senderName",username); msg.put("content",content); msg.put("ts", Timestamp.now());
    db.collection("alliances").document(allianceId).collection("messages").add(msg).addOnSuccessListener(r-> etMessage.setText(""));
  }
}
