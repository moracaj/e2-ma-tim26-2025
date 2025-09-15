package rs.ftn.rpgtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InviteActionReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPT  = "rs.ftn.rpgtracker.ACTION_ACCEPT_INVITE";
    public static final String ACTION_DECLINE = "rs.ftn.rpgtracker.ACTION_DECLINE_INVITE";

    @Override public void onReceive(Context ctx, Intent i) {
        String action        = i.getAction();
        String inviteDocPath = i.getStringExtra("inviteDocPath");
        String myUid         = i.getStringExtra("myUid");
        String allianceId    = i.getStringExtra("allianceId");
        String allianceName  = i.getStringExtra("allianceName");
        String fromUid       = i.getStringExtra("fromUid");
        String fromUsername  = i.getStringExtra("fromUsername");

        if (inviteDocPath == null || myUid == null || allianceId == null) {
            Toast.makeText(ctx, "Invite action: missing data.", Toast.LENGTH_SHORT).show();
            return;
        }

        int notifId = Math.abs(allianceId.hashCode());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference invRef = db.document(inviteDocPath);
        DocumentReference meRef  = db.collection("users").document(myUid);
        DocumentReference aRef   = db.collection("alliances").document(allianceId);

        if (ACTION_DECLINE.equals(action)) {
            invRef.update("status","declined")
                    .addOnSuccessListener(v -> {
                        NotificationManagerCompat.from(ctx).cancel(notifId);
                        Toast.makeText(ctx, "Invite declined", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(ctx, "Decline failed: "+e.getMessage(), Toast.LENGTH_LONG).show());
            return;
        }

        if (ACTION_ACCEPT.equals(action)) {
            db.runTransaction(tx -> {
                // 1) učitaj mene
                DocumentSnapshot me = tx.get(meRef);
                String myName = me.getString("username");
                String oldAid = me.getString("allianceId");

                // 2) ako sam već u drugom savezu → izlazim iz starog
                if (oldAid != null && !oldAid.isEmpty() && !oldAid.equals(allianceId)) {
                    DocumentReference oldA = db.collection("alliances").document(oldAid);
                    DocumentSnapshot oldDoc = tx.get(oldA);
                    // zaštita: ako sam lider starog saveza, blokiraj
                    if (oldDoc.exists()) {
                        String leaderUid = oldDoc.getString("leaderUid");
                        if (myUid.equals(leaderUid)) {
                            throw new RuntimeException("You are leader of current alliance. Disband first.");
                        }
                    }
                    tx.delete(oldA.collection("members").document(myUid));
                }

                // 3) upis članstva u novi savez + ažuriranje usera + invite status
                tx.set(aRef.collection("members").document(myUid), member(myUid, myName, "member"));
                tx.update(meRef, "allianceId", allianceId);
                tx.update(invRef, "status", "accepted");

                // 4) inbox event kreatoru: “inviteAccepted”
                if (fromUid != null && !fromUid.isEmpty()) {
                    Map<String,Object> event = new HashMap<>();
                    event.put("type", "inviteAccepted");
                    event.put("byUid", myUid);
                    event.put("byUsername", myName);
                    event.put("allianceId", allianceId);
                    event.put("allianceName", allianceName);
                    event.put("ts", com.google.firebase.Timestamp.now());
                    tx.set(db.collection("users").document(fromUid).collection("inbox").document(), event);
                }
                return null;
            }).addOnSuccessListener(v -> {
                NotificationManagerCompat.from(ctx).cancel(notifId);
                Toast.makeText(ctx, "Joined "+(allianceName==null?"alliance":allianceName), Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(ctx, "Accept failed: "+e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private static Map<String,Object> member(String uid, String username, String role){
        Map<String,Object> m = new HashMap<>();
        m.put("uid", uid);
        m.put("username", username == null ? "" : username);
        m.put("role", role);
        m.put("joinedAt", com.google.firebase.Timestamp.now());
        return m;
    }
}
