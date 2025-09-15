package rs.ftn.rpgtracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.*;
import android.app.PendingIntent;
import java.util.Objects;

public class LiveNotifications {
    private static ListenerRegistration userReg;
    private static ListenerRegistration msgReg;
    private static boolean msgInit = false;
    private static String currentAid = null;
    private static String myUid;

    public static void start(Context ctx){
        stop(); // očisti stare
        myUid = Prefs.getUid(ctx);
        if (myUid == null || myUid.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // slušaj promene mog profila -> allianceId
       // userReg = db.collection("users").document(myUid)
        //       .addSnapshotListener((doc, e) -> {
          //          if (e != null || doc == null || !doc.exists()) return;
           //         String aid = doc.getString("allianceId");
            //        if (!Objects.equals(aid, currentAid)){
             //           attachMessages(ctx.getApplicationContext(), db, aid);
              //      }
             //  });
        String uid = Prefs.getUid(ctx);
        if (uid == null || uid.isEmpty()) return;

        listenPendingInvites(ctx, uid);
        listenLeaderInbox(ctx, uid);   // ko je prihvatio moje pozive
        listenAllianceMessages(ctx, uid); // ako nemaš već — vidi metodu ispod
    }

    private static ListenerRegistration invitesReg;

    private static void listenPendingInvites(Context appCtx, String uid){
        detach(invitesReg);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        invitesReg = db.collection("users").document(uid)
                .collection("allianceInvites")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;

                    for (DocumentChange dc : qs.getDocumentChanges()){
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        DocumentSnapshot d = dc.getDocument();
                        String allianceId   = d.getString("allianceId");
                        String allianceName = d.getString("allianceName");
                        String fromUid      = d.getString("fromUid");
                        String fromUsername = d.getString("fromUsername");
                        if (allianceId == null) continue;

                        // Tap otvara FriendsActivity > Invites
                        Intent tap = new Intent(appCtx, FriendsActivity.class);

                        // Accept/Decline akcije
                        Intent accept = new Intent(appCtx, InviteActionReceiver.class)
                                .setAction(InviteActionReceiver.ACTION_ACCEPT)
                                .putExtra("inviteDocPath", d.getReference().getPath())
                                .putExtra("allianceId", allianceId)
                                .putExtra("allianceName", allianceName)
                                .putExtra("fromUid", fromUid)
                                .putExtra("fromUsername", fromUsername)
                                .putExtra("myUid", uid);

                        Intent decline = new Intent(appCtx, InviteActionReceiver.class)
                                .setAction(InviteActionReceiver.ACTION_DECLINE)
                                .putExtra("inviteDocPath", d.getReference().getPath())
                                .putExtra("allianceId", allianceId)
                                .putExtra("myUid", uid);

                        PendingIntent piTap     = PendingIntent.getActivity(appCtx, Math.abs(allianceId.hashCode()),
                                tap, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        PendingIntent piAccept  = PendingIntent.getBroadcast(appCtx, Math.abs((allianceId + ":A").hashCode()),
                                accept, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        PendingIntent piDecline = PendingIntent.getBroadcast(appCtx, Math.abs((allianceId + ":D").hashCode()),
                                decline, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                        String title = (fromUsername == null ? "Alliance invite" : fromUsername + " invited you");
                        String text  = (allianceName == null ? "New alliance" : "Alliance: " + allianceName);

                        Notifications.showInviteWithActions(
                                appCtx,
                                Math.abs(allianceId.hashCode()),
                                title,
                                text,
                                piTap,
                                piAccept,
                                piDecline
                        );
                    }
                });
    }

    private static ListenerRegistration inboxReg;

    private static void listenLeaderInbox(Context appCtx, String uid){
        detach(inboxReg);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        inboxReg = db.collection("users").document(uid)
                .collection("inbox")
                .whereEqualTo("type","inviteAccepted")
                .orderBy("ts", Query.Direction.DESCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;

                    for (DocumentChange dc : qs.getDocumentChanges()){
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        DocumentSnapshot d = dc.getDocument();
                        String byUser = d.getString("byUsername");
                        String aName  = d.getString("allianceName");
                        String aId    = d.getString("allianceId");
                        Intent tap = new Intent(appCtx, FriendsActivity.class);

                        Notifications.show(appCtx, Notifications.CH_INVITES,
                                Math.abs((aId + ":ACC").hashCode()),
                                (byUser == null ? "Invite accepted" : byUser + " joined"),
                                (aName == null ? "" : "Alliance: " + aName),
                                tap, false);
                    }
                });
    }


    //private static ListenerRegistration userReg, msgReg;

    private static void listenAllianceMessages(Context appCtx, String uid){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        detach(userReg);
        userReg = db.collection("users").document(uid)
                .addSnapshotListener((me, e) -> {
                    if (e != null || me == null || !me.exists()) return;

                    String allianceId = me.getString("allianceId");
                    detach(msgReg);
                    if (allianceId == null || allianceId.isEmpty()) return;

                    msgReg = db.collection("alliances").document(allianceId)
                            .collection("messages")
                            .orderBy("ts", Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener((qs, e2) -> {
                                if (e2 != null || qs == null || qs.isEmpty()) return;
                                DocumentSnapshot d = qs.getDocuments().get(0);

                                String fromUid = d.getString("senderUid");
                                String fromN   = d.getString("senderUsername");
                                String text    = d.getString("text");

                                if (uid.equals(fromUid)) return; // ne obaveštavamo za sopstvenu poruku

                                Intent tap = new Intent(appCtx, ChatActivity.class);
                                Notifications.show(appCtx, Notifications.CH_CHAT,
                                        Math.abs(d.getId().hashCode()),
                                        (fromN == null ? "New message" : fromN),
                                        (text  == null ? "" : text),
                                        tap, false);
                            });
                });
    }

    private static void detach(ListenerRegistration r){
        if (r != null) { r.remove(); }
    }



    public static void stop(){
        if (userReg != null) { userReg.remove(); userReg = null; }
        if (msgReg  != null) { msgReg.remove();  msgReg  = null; }
        msgInit = false; currentAid = null; myUid = null;
    }

}
