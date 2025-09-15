package rs.ftn.rpgtracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

  FirebaseFirestore db;
  String uid;
  String username;
  String allianceId;

  TextView tvAlliance;
  ListView listMessages;
  EditText etMessage;
  Button btnSend;

  MessageAdapter adapter;
  ListenerRegistration regMsgs;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_chat);

    db = FirebaseFirestore.getInstance();
    uid = Prefs.getUid(this);

    tvAlliance    = findViewById(R.id.tvAlliance);
    listMessages  = findViewById(R.id.listMessages);
    etMessage     = findViewById(R.id.etMessage);
    btnSend       = findViewById(R.id.btnSend);

    adapter = new MessageAdapter(this, uid);
    listMessages.setAdapter(adapter);

    loadMeAndAlliance();
    btnSend.setOnClickListener(v -> send());
  }

  @Override protected void onDestroy(){
    super.onDestroy();
    if (regMsgs != null) regMsgs.remove();
  }

  private void loadMeAndAlliance(){
    db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
      username   = doc.getString("username");
      allianceId = doc.getString("allianceId");

      if (TextUtils.isEmpty(allianceId)){
        tvAlliance.setText("No alliance");
        btnSend.setEnabled(false);
        return;
      }

      db.collection("alliances").document(allianceId).get().addOnSuccessListener(aDoc -> {
        tvAlliance.setText("Alliance chat: " + aDoc.getString("name"));
      });

      listenMessages();
    });
  }

  private void listenMessages(){
    regMsgs = db.collection("alliances").document(allianceId)
            .collection("messages")
            .orderBy("ts", Query.Direction.ASCENDING)
            .addSnapshotListener((qs, e) -> {
              if (e != null || qs == null) return;
              List<Message> list = new ArrayList<>();
              for (DocumentSnapshot d : qs.getDocuments()){
                Message m = d.toObject(Message.class);
                if (m != null) { m.id = d.getId(); list.add(m); }
              }
              adapter.setAll(list);
              listMessages.post(() -> listMessages.setSelection(adapter.getCount()-1));
            });
  }

  private void send(){
    String text = etMessage.getText().toString().trim();
    if (text.isEmpty()) return;

    Map<String,Object> msg = new HashMap<>();
    msg.put("text", text);
    msg.put("senderUid", uid);
    msg.put("senderUsername", username);
    msg.put("ts", Timestamp.now());

    db.collection("alliances").document(allianceId).collection("messages")
            .add(msg).addOnSuccessListener(ref -> {
              etMessage.setText("");

            });
  }
}
