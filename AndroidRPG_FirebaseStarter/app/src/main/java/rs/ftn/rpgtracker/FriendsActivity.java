
package rs.ftn.rpgtracker;
import android.os.Bundle; import android.widget.*;
import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore; import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList; import java.util.HashMap; import java.util.List; import java.util.Map;
public class FriendsActivity extends AppCompatActivity {
  FirebaseFirestore db; String uid; EditText etFriend,etAllianceName; Button btnAddFriend,btnCreateAlliance; ListView listFriends; TextView tvAllianceInfo;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState); setContentView(R.layout.activity_friends);
    db=FirebaseFirestore.getInstance(); uid=Prefs.getUid(this);
    etFriend=findViewById(R.id.etFriend); etAllianceName=findViewById(R.id.etAllianceName);
    btnAddFriend=findViewById(R.id.btnAddFriend); btnCreateAlliance=findViewById(R.id.btnCreateAlliance);
    listFriends=findViewById(R.id.listFriends); tvAllianceInfo=findViewById(R.id.tvAllianceInfo);
    btnAddFriend.setOnClickListener(v->addFriend()); btnCreateAlliance.setOnClickListener(v->createOrJoinAlliance()); refresh();
  }
  private void addFriend(){
    String username=etFriend.getText().toString().trim();
    if(username.isEmpty()){ Toast.makeText(this,"Enter username",Toast.LENGTH_SHORT).show(); return; }
    db.collection("users").whereEqualTo("username",username).limit(1).get().addOnSuccessListener(q->{
      if(q.isEmpty()){ Toast.makeText(this,"User not found",Toast.LENGTH_SHORT).show(); return; }
      String friendUid=q.getDocuments().get(0).getId();
      Map<String,Object> f=new HashMap<>(); f.put("friendUid",friendUid);
      db.collection("friends").document(uid).collection("list").document(friendUid).set(f).addOnSuccessListener(a->{ Toast.makeText(this,"Friend added",Toast.LENGTH_SHORT).show(); refresh(); });
    });
  }
  private void refresh(){
    db.collection("friends").document(uid).collection("list").get().addOnSuccessListener(snap->{
      List<String> friends=new ArrayList<>(); List<String> ids=new ArrayList<>();
      for(QueryDocumentSnapshot d:snap) ids.add(d.getId());
      if(ids.isEmpty()){ listFriends.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friends)); }
      for(String id:ids){
        db.collection("users").document(id).get().addOnSuccessListener(doc->{ String name=doc.getString("username"); friends.add(name); listFriends.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friends)); });
      }
    });
    db.collection("users").document(uid).get().addOnSuccessListener(doc->{
      String allianceId=doc.getString("allianceId");
      if(allianceId==null||allianceId.isEmpty()) tvAllianceInfo.setText("No alliance");
      else db.collection("alliances").document(allianceId).get().addOnSuccessListener(aDoc-> tvAllianceInfo.setText("Alliance: "+aDoc.getString("name")+" ("+aDoc.getId()+")"));
    });
  }
  private void createOrJoinAlliance(){
    String name=etAllianceName.getText().toString().trim();
    if(name.isEmpty()){ Toast.makeText(this,"Enter alliance name",Toast.LENGTH_SHORT).show(); return; }
    db.collection("alliances").whereEqualTo("name",name).limit(1).get().addOnSuccessListener(q->{
      if(q.isEmpty()){
        Map<String,Object> a=new HashMap<>(); a.put("name",name); a.put("leaderUid",uid);
        db.collection("alliances").add(a).addOnSuccessListener(ref->{ db.collection("alliances").document(ref.getId()).collection("members").document(uid).set(a); db.collection("users").document(uid).update("allianceId",ref.getId()); Toast.makeText(this,"Created and joined alliance "+name,Toast.LENGTH_SHORT).show(); refresh(); });
      } else {
        String aid=q.getDocuments().get(0).getId();
        db.collection("alliances").document(aid).collection("members").document(uid).set(new HashMap<>()).addOnSuccessListener(a->{ db.collection("users").document(uid).update("allianceId",aid); Toast.makeText(this,"Joined alliance "+name,Toast.LENGTH_SHORT).show(); refresh(); });
      }
    });
  }
}
