package rs.ftn.rpgtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.google.firebase.auth.FirebaseAuth;

public final class EquipmentOps {
    private EquipmentOps(){}

    // Pozovi ovo NA KRAJU borbe sa bosom
    public static void settleAfterBoss(Context ctx) {
        SQLiteDatabase db = new AppDbHelper(ctx).getWritableDatabase();

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // 1) Potroši jednokratne napitke aktivirane za narednu borbu
        if (uid != null) {
            db.execSQL(
                    "DELETE FROM inventory " +
                            "WHERE user_uid=? AND active=1 AND permanent=0 " +
                            "AND equipment_id IN (SELECT id FROM equipment_catalog WHERE type='potion')",
                    new Object[]{ uid }
            );
        } else {
            db.execSQL(
                    "DELETE FROM inventory " +
                            "WHERE active=1 AND permanent=0 " +
                            "AND equipment_id IN (SELECT id FROM equipment_catalog WHERE type='potion')"
            );
        }

        // 2) Odeća: smanji trajanje i deaktiviraj kad istekne
        if (uid != null) {
            db.execSQL(
                    "UPDATE inventory SET expires_after_battles = expires_after_battles - 1 " +
                            "WHERE user_uid=? AND active=1 AND expires_after_battles IS NOT NULL " +
                            "AND equipment_id IN (SELECT id FROM equipment_catalog WHERE type='armor')",
                    new Object[]{ uid }
            );
            db.execSQL(
                    "UPDATE inventory SET active=0 " +
                            "WHERE user_uid=? AND expires_after_battles<=0 AND expires_after_battles IS NOT NULL " +
                            "AND equipment_id IN (SELECT id FROM equipment_catalog WHERE type='armor')",
                    new Object[]{ uid }
            );
        } else {
            db.execSQL(
                    "UPDATE inventory SET expires_after_battles = expires_after_battles - 1 " +
                            "WHERE active=1 AND expires_after_battles IS NOT NULL " +
                            "AND equipment_id IN (SELECT id FROM equipment_catalog WHERE type='armor')"
            );
            db.execSQL(
                    "UPDATE inventory SET active=0 " +
                            "WHERE expires_after_battles<=0 AND expires_after_battles IS NOT NULL " +
                            "AND equipment_id IN (SELECT id FROM equipment_catalog WHERE type='armor')"
            );
        }
    }
}
