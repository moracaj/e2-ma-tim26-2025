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
        userReg = db.collection("users").document(myUid)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    String aid = doc.getString("allianceId");
                    if (!Objects.equals(aid, currentAid)){
                        attachMessages(ctx.getApplicationContext(), db, aid);
                    }
                });
    }

    public static void stop(){
        if (userReg != null) { userReg.remove(); userReg = null; }
        if (msgReg  != null) { msgReg.remove();  msgReg  = null; }
        msgInit = false; currentAid = null; myUid = null;
    }

    private static void attachMessages(Context appCtx, FirebaseFirestore db, String aid){
        if (msgReg != null) { msgReg.remove(); msgReg = null; }
        msgInit = false; currentAid = aid;
        if (aid == null || aid.isEmpty()) return;

        msgReg = db.collection("alliances").document(aid)
                .collection("messages")
                .orderBy("ts", Query.Direction.ASCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;

                    if (!msgInit) { // ignoriši istoriju
                        msgInit = true; return;
                    }

                    for (DocumentChange ch : qs.getDocumentChanges()){
                        if (ch.getType() != DocumentChange.Type.ADDED) continue;

                        DocumentSnapshot d = ch.getDocument();
                        String fromU = d.getString("senderUid");
                        if (myUid != null && myUid.equals(fromU)) continue; // ne notifikuj sebe

                        String fromN = d.getString("senderUsername");
                        String text  = d.getString("text");

                        // Ako nema dozvole ili su notifikacije ugašene, pokaži kratko obaveštenje (jednom)
                        if (Build.VERSION.SDK_INT >= 33 &&
                                ActivityCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS)
                                        != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(appCtx, "Enable notifications to see chat alerts.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!NotificationManagerCompat.from(appCtx).areNotificationsEnabled()){
                            Toast.makeText(appCtx, "Notifications are disabled in system settings.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Intent tap = new Intent(appCtx, ChatActivity.class);
                        Notifications.show(appCtx, Notifications.CH_CHAT,
                                Math.abs(d.getId().hashCode()),
                                (fromN == null ? "New message" : fromN),
                                (text  == null ? "" : text),
                                tap, false);
                    }
                });
    }
}
