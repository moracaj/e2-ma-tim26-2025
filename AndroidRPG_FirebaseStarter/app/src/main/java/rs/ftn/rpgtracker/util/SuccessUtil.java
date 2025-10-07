// rs/ftn/rpgtracker/util/SuccessUtil.java
package rs.ftn.rpgtracker.util;

import com.google.firebase.firestore.*;
import java.util.*;
import rs.ftn.rpgtracker.model.Task;

public class SuccessUtil {

    public interface SuccessCallback {
        void onDone(int percent);
        void onError(Exception e);
    }

    public static void computeSuccessForStage(FirebaseFirestore db, String uid, Date stageStart, Date stageEnd, SuccessCallback cb) {
        // 1) povuci taskove korisnika u periodu
        db.collection("tasks")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    int total = 0;
                    int success = 0;

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Task t = d.toObject(Task.class);
                        if (t == null) continue;

                        // ignorisi pauzirane i otkazane u racunanju "total"
                        if (t.getStatus() == Task.Status.PAUSED || t.getStatus() == Task.Status.CANCELLED) continue;

                        // u etapi (približno: po startDate)
                        Date sd = t.getStartDate();
                        if (sd == null) continue;
                        if (stageStart != null && sd.before(stageStart)) continue;
                        if (stageEnd != null && sd.after(stageEnd)) continue;

                        // preskoci "over-quota" za success — pretpostavka: taskExecutions beleze svako validno izvrsenje
                        // total++
                        total++;

                        if (t.getStatus() == Task.Status.COMPLETED) {
                            success++;
                        }
                    }

                    int percent = (total == 0) ? 0 : (int)Math.round(success * 100.0 / total);
                    cb.onDone(percent);
                })
                .addOnFailureListener(cb::onError);
    }
}
