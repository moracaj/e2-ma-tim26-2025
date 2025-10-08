//package rs.ftn.rpgtracker;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.widget.Toast;
//
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationManagerCompat;
//import android.content.Context;
//import android.content.SharedPreferences;
//
//import com.google.firebase.firestore.*;
//import android.app.PendingIntent;
//import java.util.Objects;
//
//public class LiveNotifications {
//    private static ListenerRegistration userReg;
//    private static ListenerRegistration msgReg;
//    private static boolean msgInit = false;
//    private static String currentAid = null;
//    private static String myUid;
//
//
//    // ====== DUPLICATE-NOTIF STATE (LOCALNO PAMĆENJE) ======
//    private static final String SP = "live_notif";
//
//    private static SharedPreferences sp(Context c) {
//        return c.getSharedPreferences(SP, Context.MODE_PRIVATE);
//    }
//
//    private static boolean wasNotified(Context c, String key) {
//        return sp(c).getBoolean("n_" + key, false);
//    }
//
//    private static void markNotified(Context c, String key) {
//        sp(c).edit().putBoolean("n_" + key, true).apply();
//    }
//
//    private static long lastChatTs(Context c, String allianceId) {
//        return sp(c).getLong("chat_ts_" + allianceId, 0L);
//    }
//
//    private static void setLastChatTs(Context c, String allianceId, long ts) {
//        long cur = lastChatTs(c, allianceId);
//        if (ts > cur) {
//            sp(c).edit().putLong("chat_ts_" + allianceId, ts).apply();
//        }
//    }
//
//    // flagovi za "skip initial snapshot"
//    private static boolean invitesInit = false;
//
//
//    public static void start(Context ctx){
//        stop(); // očisti stare
//        myUid = Prefs.getUid(ctx);
//        if (myUid == null || myUid.isEmpty()) return;
//
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        // slušaj promene mog profila -> allianceId
//       // userReg = db.collection("users").document(myUid)
//        //       .addSnapshotListener((doc, e) -> {
//          //          if (e != null || doc == null || !doc.exists()) return;
//           //         String aid = doc.getString("allianceId");
//            //        if (!Objects.equals(aid, currentAid)){
//             //           attachMessages(ctx.getApplicationContext(), db, aid);
//              //      }
//             //  });
//        String uid = Prefs.getUid(ctx);
//        if (uid == null || uid.isEmpty()) return;
//
//        listenPendingInvites(ctx, uid);
//        listenLeaderInbox(ctx, uid);   // ko je prihvatio moje pozive
//        listenAllianceMessages(ctx, uid); // ako nemaš već — vidi metodu ispod
//    }
//
//    private static ListenerRegistration invitesReg;
//
//    private static void listenPendingInvites(Context appCtx, String uid){
//        detach(invitesReg);
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        invitesReg = db.collection("users").document(uid)
//                .collection("allianceInvites")
//                .whereEqualTo("status", "pending")
//                .addSnapshotListener((qs, e) -> {
//                    if (e != null || qs == null) return;
//
//                    for (DocumentChange dc : qs.getDocumentChanges()){
//                        if (dc.getType() != DocumentChange.Type.ADDED) continue;
//
//                        DocumentSnapshot d = dc.getDocument();
//                        String allianceId   = d.getString("allianceId");
//                        String allianceName = d.getString("allianceName");
//                        String fromUid      = d.getString("fromUid");
//                        String fromUsername = d.getString("fromUsername");
//                        if (allianceId == null) continue;
//
//                        // Tap otvara FriendsActivity > Invites
//                        Intent tap = new Intent(appCtx, FriendsActivity.class);
//
//                        // Accept/Decline akcije
//                        Intent accept = new Intent(appCtx, InviteActionReceiver.class)
//                                .setAction(InviteActionReceiver.ACTION_ACCEPT)
//                                .putExtra("inviteDocPath", d.getReference().getPath())
//                                .putExtra("allianceId", allianceId)
//                                .putExtra("allianceName", allianceName)
//                                .putExtra("fromUid", fromUid)
//                                .putExtra("fromUsername", fromUsername)
//                                .putExtra("myUid", uid);
//
//                        Intent decline = new Intent(appCtx, InviteActionReceiver.class)
//                                .setAction(InviteActionReceiver.ACTION_DECLINE)
//                                .putExtra("inviteDocPath", d.getReference().getPath())
//                                .putExtra("allianceId", allianceId)
//                                .putExtra("myUid", uid);
//
//                        PendingIntent piTap     = PendingIntent.getActivity(appCtx, Math.abs(allianceId.hashCode()),
//                                tap, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//                        PendingIntent piAccept  = PendingIntent.getBroadcast(appCtx, Math.abs((allianceId + ":A").hashCode()),
//                                accept, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//                        PendingIntent piDecline = PendingIntent.getBroadcast(appCtx, Math.abs((allianceId + ":D").hashCode()),
//                                decline, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//                        String title = (fromUsername == null ? "Alliance invite" : fromUsername + " invited you");
//                        String text  = (allianceName == null ? "New alliance" : "Alliance: " + allianceName);
//
//                        Notifications.showInviteWithActions(
//                                appCtx,
//                                Math.abs(allianceId.hashCode()),
//                                title,
//                                text,
//                                piTap,
//                                piAccept,
//                                piDecline
//                        );
//                    }
//                });
//    }
//
//    private static ListenerRegistration inboxReg;
//
//    private static void listenLeaderInbox(Context appCtx, String uid){
//        detach(inboxReg);
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        inboxReg = db.collection("users").document(uid)
//                .collection("inbox")
//                .whereEqualTo("type","inviteAccepted")
//                .orderBy("ts", Query.Direction.DESCENDING)
//                .addSnapshotListener((qs, e) -> {
//                    if (e != null || qs == null) return;
//
//                    for (DocumentChange dc : qs.getDocumentChanges()){
//                        if (dc.getType() != DocumentChange.Type.ADDED) continue;
//
//                        DocumentSnapshot d = dc.getDocument();
//                        String byUser = d.getString("byUsername");
//                        String aName  = d.getString("allianceName");
//                        String aId    = d.getString("allianceId");
//                        Intent tap = new Intent(appCtx, FriendsActivity.class);
//
//                        Notifications.show(appCtx, Notifications.CH_INVITES,
//                                Math.abs((aId + ":ACC").hashCode()),
//                                (byUser == null ? "Invite accepted" : byUser + " joined"),
//                                (aName == null ? "" : "Alliance: " + aName),
//                                tap, false);
//                    }
//                });
//    }
//
//
//    //private static ListenerRegistration userReg, msgReg;
//
//    private static void listenAllianceMessages(Context appCtx, String uid){
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        detach(userReg);
//        userReg = db.collection("users").document(uid)
//                .addSnapshotListener((me, e) -> {
//                    if (e != null || me == null || !me.exists()) return;
//
//                    String allianceId = me.getString("allianceId");
//                    detach(msgReg);
//                    if (allianceId == null || allianceId.isEmpty()) return;
//
//                    msgReg = db.collection("alliances").document(allianceId)
//                            .collection("messages")
//                            .orderBy("ts", Query.Direction.DESCENDING)
//                            .limit(1)
//                            .addSnapshotListener((qs, e2) -> {
//                                if (e2 != null || qs == null || qs.isEmpty()) return;
//                                DocumentSnapshot d = qs.getDocuments().get(0);
//
//                                String fromUid = d.getString("senderUid");
//                                String fromN   = d.getString("senderUsername");
//                                String text    = d.getString("text");
//
//                                if (uid.equals(fromUid)) return; // ne obaveštavamo za sopstvenu poruku
//
//                                Intent tap = new Intent(appCtx, ChatActivity.class);
//                                Notifications.show(appCtx, Notifications.CH_CHAT,
//                                        Math.abs(d.getId().hashCode()),
//                                        (fromN == null ? "New message" : fromN),
//                                        (text  == null ? "" : text),
//                                        tap, false);
//                            });
//                });
//    }
//
//    private static void detach(ListenerRegistration r){
//        if (r != null) { r.remove(); }
//    }
//
//
//
//    public static void stop(){
//        if (userReg != null) { userReg.remove(); userReg = null; }
//        if (msgReg  != null) { msgReg.remove();  msgReg  = null; }
//        msgInit = false; currentAid = null; myUid = null;
//    }
//
//}



package rs.ftn.rpgtracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class LiveNotifications {

    // --- Firestore registracije ---
    private static ListenerRegistration userReg;
    private static ListenerRegistration invitesReg;
    private static ListenerRegistration inboxReg;
    private static ListenerRegistration msgReg;

    private static String currentAid = null;
    private static String myUid;

    // ====== LOCAL STATE (SharedPreferences) ======
    private static final String SP = "live_notif";
    private static SharedPreferences sp(Context c) { return c.getSharedPreferences(SP, Context.MODE_PRIVATE); }

    private static boolean wasNotified(Context c, String key) { return sp(c).getBoolean("n_"+key, false); }
    private static void markNotified(Context c, String key) { sp(c).edit().putBoolean("n_"+key, true).apply(); }

    private static long lastChatTs(Context c, String allianceId) { return sp(c).getLong("chat_ts_"+allianceId, 0L); }
    private static void setLastChatTs(Context c, String allianceId, long ts) {
        long cur = lastChatTs(c, allianceId);
        if (ts > cur) sp(c).edit().putLong("chat_ts_"+allianceId, ts).apply();
    }

    private static long lastInboxTs(Context c) { return sp(c).getLong("inbox_ts", 0L); }
    private static void setLastInboxTs(Context c, long ts) {
        long cur = lastInboxTs(c);
        if (ts > cur) sp(c).edit().putLong("inbox_ts", ts).apply();
    }

    // ====== PUBLIC ======
    public static void start(Context ctx) {
        stop(); // očisti stare listenere/flagove
        myUid = Prefs.getUid(ctx);
        if (myUid == null || myUid.isEmpty()) return;

        Context appCtx = ctx.getApplicationContext();
        listenPendingInvites(appCtx, myUid);   // pozivi u savez
        listenLeaderInbox(appCtx, myUid);      // inviteAccepted meni
        listenAllianceMessages(appCtx, myUid); // chat poruke
    }

    public static void stop() {
        detach(userReg);  userReg  = null;
        detach(invitesReg); invitesReg = null;
        detach(inboxReg);  inboxReg = null;
        detach(msgReg);    msgReg   = null;
        currentAid = null;
        myUid = null;
    }

    private static void detach(ListenerRegistration r) { if (r != null) r.remove(); }

    // ====== HELPERS ======
    private static long tsOf(DocumentSnapshot d) {
        Timestamp ts = d.getTimestamp("ts");
        return ts == null ? 0L : ts.toDate().getTime();
    }

    // ====== INVITES (pending) – markiraj postojeće kao viđene, pa slušaj samo nove ======
    private static void listenPendingInvites(Context appCtx, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        detach(invitesReg);

        // 1) PRIMING: sve trenutno pending invite-ove obeleži kao viđene (da se ne pojave kao nove)
        db.collection("users").document(uid).collection("allianceInvites")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        markNotified(appCtx, "invite_"+d.getId());
                    }
                    // 2) LISTENER: sada će stizati samo stvarno novi ADDED
                    invitesReg = db.collection("users").document(uid)
                            .collection("allianceInvites")
                            .whereEqualTo("status", "pending")
                            .addSnapshotListener((snap, e) -> {
                                if (e != null || snap == null) return;

                                for (DocumentChange ch : snap.getDocumentChanges()) {
                                    if (ch.getType() != DocumentChange.Type.ADDED) continue;

                                    DocumentSnapshot d = ch.getDocument();
                                    String inviteId = d.getId();
                                    if (wasNotified(appCtx, "invite_"+inviteId)) continue; // već viđeno

                                    String allianceId   = d.getString("allianceId");
                                    String allianceName = d.getString("allianceName");
                                    String fromUid      = d.getString("fromUid");
                                    String fromUsername = d.getString("fromUsername");
                                    if (allianceId == null) continue;

                                    Intent tap = new Intent(appCtx, FriendsActivity.class);

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

                                    markNotified(appCtx, "invite_"+inviteId);
                                }
                            });
                });
    }

    // ====== INBOX (inviteAccepted meni) – priming poslednjeg ts pa onda > ts ======
    private static void listenLeaderInbox(Context appCtx, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        detach(inboxReg);

        // 1) PRIMING: pročitaj najnoviji ts i zapamti ga
        db.collection("users").document(uid)
                .collection("inbox")
                .whereEqualTo("type","inviteAccepted")
                .orderBy("ts", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    long base = 0L;
                    if (!q.isEmpty()) base = tsOf(q.getDocuments().get(0));
                    setLastInboxTs(appCtx, base);

                    // 2) LISTENER: samo dokumenti sa ts > lastInboxTs
                    inboxReg = db.collection("users").document(uid)
                            .collection("inbox")
                            .whereEqualTo("type","inviteAccepted")
                            .whereGreaterThan("ts", new Timestamp(new Date(lastInboxTs(appCtx))))
                            .orderBy("ts", Query.Direction.ASCENDING)
                            .addSnapshotListener((qs, e) -> {
                                if (e != null || qs == null) return;

                                for (DocumentChange dc : qs.getDocumentChanges()) {
                                    if (dc.getType() != DocumentChange.Type.ADDED) continue;

                                    DocumentSnapshot d = dc.getDocument();
                                    long tsm = tsOf(d);
                                    String byUser = d.getString("byUsername");
                                    String aName  = d.getString("allianceName");
                                    String aId    = d.getString("allianceId");

                                    Intent tap = new Intent(appCtx, FriendsActivity.class);

                                    Notifications.show(appCtx, Notifications.CH_INVITES,
                                            Math.abs((aId + ":ACC").hashCode()),
                                            (byUser == null ? "Invite accepted" : byUser + " joined"),
                                            (aName == null ? "" : "Alliance: " + aName),
                                            tap, false);

                                    setLastInboxTs(appCtx, tsm);
                                }
                            });
                });
    }

    // ====== CHAT poruke – priming poslednjeg ts pa onda > ts ======
    private static void listenAllianceMessages(Context appCtx, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        detach(userReg);
        detach(msgReg);
        currentAid = null;

        userReg = db.collection("users").document(uid)
                .addSnapshotListener((me, e) -> {
                    if (e != null || me == null || !me.exists()) return;

                    String allianceId = me.getString("allianceId");
                    if (!Objects.equals(allianceId, currentAid)) {
                        currentAid = allianceId;
                        detach(msgReg);
                        if (allianceId == null || allianceId.isEmpty()) return;

                        // PRIMING: nađi poslednju poruku i setuj lastChatTs
                        db.collection("alliances").document(allianceId)
                                .collection("messages")
                                .orderBy("ts", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(q -> {
                                    long base = 0L;
                                    if (!q.isEmpty()) base = tsOf(q.getDocuments().get(0));
                                    setLastChatTs(appCtx, allianceId, base);

                                    // LISTENER: samo poruke sa ts > lastChatTs
                                    msgReg = db.collection("alliances").document(allianceId)
                                            .collection("messages")
                                            .whereGreaterThan("ts", new Timestamp(new Date(lastChatTs(appCtx, allianceId))))
                                            .orderBy("ts", Query.Direction.ASCENDING)
                                            .addSnapshotListener((qs, e2) -> handleChatSnapshot(appCtx, allianceId, uid, qs, e2));
                                });
                    }
                });
    }

    private static void handleChatSnapshot(Context appCtx, String allianceId, String myUid, QuerySnapshot qs, Exception e2) {
        if (e2 != null || qs == null) return;

        long lastTs = lastChatTs(appCtx, allianceId);

        for (DocumentChange ch : qs.getDocumentChanges()) {
            if (ch.getType() != DocumentChange.Type.ADDED) continue;

            DocumentSnapshot d = ch.getDocument();
            long tsm = tsOf(d);
            String senderUid = d.getString("senderUid");
            String senderN   = d.getString("senderUsername");
            String text      = d.getString("text");

            // moja poruka? samo update lastSeen
            if (myUid != null && myUid.equals(senderUid)) {
                setLastChatTs(appCtx, allianceId, tsm);
                continue;
            }

            // starija/ista od poslednje viđene? preskoči (defanzivno)
            if (tsm <= lastTs) continue;

            // NOVA poruka -> notifikuj
            Intent tap = new Intent(appCtx, ChatActivity.class);
            Notifications.show(appCtx, Notifications.CH_CHAT,
                    Math.abs(d.getId().hashCode()),
                    (senderN == null ? "New message" : senderN),
                    (text == null ? "" : text),
                    tap, false);

            setLastChatTs(appCtx, allianceId, tsm);
        }
    }
}

