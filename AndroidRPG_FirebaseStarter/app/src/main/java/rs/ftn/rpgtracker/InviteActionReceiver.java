// InviteActionReceiver.java
package rs.ftn.rpgtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class InviteActionReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPT  = "rs.ftn.rpgtracker.ACTION_ACCEPT_INVITE";
    public static final String ACTION_DECLINE = "rs.ftn.rpgtracker.ACTION_DECLINE_INVITE";

    @Override
    public void onReceive(Context ctx, Intent i) {
        String action = i.getAction();
        String inviteDocPath = i.getStringExtra("inviteDocPath");
        String myUid         = i.getStringExtra("myUid");
        String allianceId    = i.getStringExtra("allianceId");
        String allianceName  = i.getStringExtra("allianceName");
        String fromUid       = i.getStringExtra("fromUid");
        String fromUsername  = i.getStringExtra("fromUsername");

        int notifId = Math.abs((allianceId == null ? "x" : allianceId).hashCode());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (inviteDocPath == null || myUid == null) return;

        DocumentReference invRef = db.document(inviteDocPath);
        DocumentReference meRef  = db.collection("users").document(myUid);

        if (ACTION_DECLINE.equals(action)) {
            invRef.update("status","declined");
            NotificationManagerCompat.from(ctx).cancel(notifId);
            return;
        }

        if (ACTION_ACCEPT.equals(action)) {
            // transakcija: napusti prethodni savez (ako postoji) + uđi u novi + ažuriraj invite
            db.runTransaction(tx -> {
                DocumentSnapshot me = tx.get(meRef);
                String currentAid = me.getString("allianceId");

                if (currentAid != null && !currentAid.isEmpty() && !currentAid.equals(allianceId)) {
                    // izbaci me iz starog saveza
                    DocumentReference oldA = db.collection("alliances").document(currentAid);
                    tx.delete(oldA.collection("members").document(myUid));
                }

                // upiši novo članstvo + user.allianceId + invite status
                DocumentReference newA = db.collection("alliances").document(allianceId);
                Map<String,Object> member = new HashMap<>();
                member.put("uid", myUid);
                member.put("username", me.getString("username"));
                member.put("role", "member");
                tx.set(newA.collection("members").document(myUid), member);

                tx.update(meRef, "allianceId", allianceId);
                tx.update(invRef, "status", "accepted");

                // ubaci poruku kreatoru – inbox event (da dobije notifikaciju #2)
                if (fromUid != null && !fromUid.isEmpty()) {
                    Map<String,Object> event = new HashMap<>();
                    event.put("type", "inviteAccepted");
                    event.put("byUid", myUid);
                    event.put("byUsername", me.getString("username"));
                    event.put("allianceId", allianceId);
                    event.put("allianceName", allianceName);
                    event.put("ts", com.google.firebase.Timestamp.now());
                    tx.set(db.collection("users").document(fromUid)
                            .collection("inbox").document(), event);
                }
                return null;
            }).addOnSuccessListener(v -> {
                NotificationManagerCompat.from(ctx).cancel(notifId);
            });
        }
    }
}
