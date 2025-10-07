package rs.ftn.rpgtracker;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class EquipmentActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private TabLayout tabs;
    private SummaryView summaryView;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment);

        summaryView = findViewById(R.id.summaryView);
        pager = findViewById(R.id.viewPager);
        tabs = findViewById(R.id.tabs);

        pager.setAdapter(new PagerAdapter(this));
        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            tab.setText(pos==0 ? "Napici" : pos==1 ? "Odeća" : "Oružje");
        }).attach();

        // osveži rezime bonusa pri otvaranju
        refreshSummary();
    }

    void refreshSummary() {
        AppDbHelper helper = new AppDbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        double ppActive = 0;
        double hitActive = 0;
        int    extraAttackChance = 0;
        double ppPerm = 0;
        double coinPerm = 0;

        // ako je ulogovan – filtriramo po user_uid
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        String sql = "SELECT ec.type, ec.bonus_type, ec.bonus_value, " +
                "       inv.active AS equipped, inv.permanent, " +
                "       ec.duration_battles, inv.expires_after_battles AS remaining_battles, " +
                "       inv.level, ec.name " +
                "FROM inventory inv " +
                "JOIN equipment_catalog ec ON ec.id = inv.equipment_id " +
                (uid != null ? "WHERE inv.user_uid=?" : "");

        Cursor c = db.rawQuery(sql, uid != null ? new String[]{ uid } : null);
        try {
            while (c.moveToNext()) {
                String type  = c.getString(0);
                String btype = c.getString(1);
                double bval  = c.getDouble(2);
                int equipped = c.getInt(3);
                int permanent= c.getInt(4);
                Integer rem  = c.isNull(6) ? null : c.getInt(6);

                if ("weapon".equals(type)) {
                    if ("pp_perm_percent".equals(btype))   ppPerm   += bval; // Sword
                    if ("coin_perm_percent".equals(btype)) coinPerm += bval; // Bow
                } else if ("potion".equals(type)) {
                    if (permanent == 1 && "pp_perm_percent".equals(btype)) {
                        // čeka Consume → ne sabiramo dok ne bude consumed
                    } else {
                        if (equipped == 1 && "pp_percent".equals(btype)) ppActive += bval;
                    }
                } else if ("armor".equals(type)) {
                    if (equipped == 1 && rem != null && rem > 0) {
                        switch (btype) {
                            case "pp_percent":          ppActive += bval; break;
                            case "hit_percent":         hitActive += bval; break;
                            case "extra_attack_chance": extraAttackChance += (int)bval; break;
                        }
                    }
                }
            }
        } finally { c.close(); }

        final double ppActiveF = ppActive;
        final double hitActiveF = hitActive;
        final int    extraAttackChanceF = extraAttackChance;
        final double ppPermF = ppPerm;
        final double coinPermF = coinPerm;

        if (uid == null) { // nije ulogovan – prikaži lokalno izračunato
            summaryView.bind(ppActiveF, hitActiveF, extraAttackChanceF, ppPermF, coinPermF);
            return;
        }

        // dodaj trajne procente koji žive u Firestore-u (perma potion/lucko)
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        DocumentReference uref = fs.collection("users").document(uid);
        uref.get().addOnSuccessListener(snap -> {
            double ppPermFs   = snap.contains("ppPermPercent")   ? snap.getDouble("ppPermPercent")   : 0.0;
            double coinPermFs = snap.contains("coinPermPercent") ? snap.getDouble("coinPermPercent") : 0.0;
            summaryView.bind(ppActiveF, hitActiveF, extraAttackChanceF,
                    ppPermF + ppPermFs, coinPermF + coinPermFs);
        }).addOnFailureListener(e ->
                summaryView.bind(ppActiveF, hitActiveF, extraAttackChanceF, ppPermF, coinPermF)
        );
    }



    // Adapter za 3 taba (potion / armor / weapon)
    static class PagerAdapter extends FragmentStateAdapter {
        PagerAdapter(FragmentActivity fa){ super(fa); }
        @Override public int getItemCount(){ return 3; }
        @Override public Fragment createFragment(int position){
            String type = position==0 ? "potion" : position==1 ? "armor" : "weapon";
            return InventoryListFragment.newInstance(type);
        }
    }

    // ===== Fragment za listu inventara određenog tipa =====
    public static class InventoryListFragment extends Fragment implements InventoryAdapter.ActionListener {
        private static final String ARG_TYPE = "type";
        private String type;
        private InventoryAdapter adapter;

        public static InventoryListFragment newInstance(String type){
            InventoryListFragment f = new InventoryListFragment();
            Bundle b = new Bundle(); b.putString(ARG_TYPE, type);
            f.setArguments(b); return f;
        }

        @Override public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            type = getArguments().getString(ARG_TYPE);
        }

        @Nullable @Override public View onCreateView(android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_inventory_list, container, false);
            RecyclerView rv = v.findViewById(R.id.recycler);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new InventoryAdapter(getContext(), this);
            rv.setAdapter(adapter);
            load();
            return v;
        }

        void load(){
            AppDbHelper helper = new AppDbHelper(getContext());
            SQLiteDatabase db = helper.getReadableDatabase();

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            String sql = "SELECT inv.id, ec.name, ec.bonus_type, ec.bonus_value, " +
                    "       ec.duration_battles, inv.permanent, inv.level, " +
                    "       inv.active AS equipped, inv.expires_after_battles AS remaining_battles, " +
                    "       ec.type " +
                    "FROM inventory inv " +
                    "JOIN equipment_catalog ec ON ec.id = inv.equipment_id " +
                    "WHERE " + (uid != null ? "inv.user_uid=? AND " : "") + "ec.type=? " +
                    "ORDER BY ec.name";

            Cursor c = db.rawQuery(sql,
                    uid != null ? new String[]{ uid, type } : new String[]{ type });

            List<InventoryAdapter.Row> rows = new ArrayList<>();
            try {
                while (c.moveToNext()){
                    InventoryAdapter.Row r = new InventoryAdapter.Row();
                    r.id = c.getInt(0);
                    r.name = c.getString(1);
                    r.bonusType = c.getString(2);
                    r.bonusValue = c.getDouble(3);
                    r.durationBattles = c.getInt(4);
                    r.permanent = c.getInt(5)==1;
                    r.level = c.getInt(6);
                    r.equipped = c.getInt(7)==1;
                    r.remainingBattles = c.isNull(8) ? null : c.getInt(8);
                    r.type = c.getString(9);
                    rows.add(r);
                }
            } finally { c.close(); }
            adapter.submit(rows);
        }


        // ==== Akcije ====
        @Override public void onActivatePotion(InventoryAdapter.Row r){
            // jednokratni potion: markiramo za SLEDEĆU borbu
            AppDbHelper h = new AppDbHelper(getContext());
            SQLiteDatabase db = h.getWritableDatabase();
           // ContentValues cv = new ContentValues(); cv.put("equipped", 1);
           // db.update("inventory", cv, "id=?", new String[]{ String.valueOf(r.id) });
            ContentValues cv = new ContentValues(); cv.put("active", 1);
            db.update("inventory", cv, "id=?", new String[]{ String.valueOf(r.id) });

            Toast.makeText(getContext(), "Potion aktiviran za narednu borbu.", Toast.LENGTH_SHORT).show();
            load();
            ((EquipmentActivity)getActivity()).refreshSummary();
        }

        @Override public void onConsumePermanentPotion(InventoryAdapter.Row r){
            // trajni potion: uvećaj ppPermPercent kod korisnika i ukloni ga iz inventara
            FirebaseFirestore fs = FirebaseFirestore.getInstance();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference uref = fs.collection("users").document(uid);
            fs.runTransaction(tr -> {
                Double old = (Double) tr.get(uref).get("ppPermPercent");
                double now = (old==null?0.0:old) + r.bonusValue;
                tr.update(uref, "ppPermPercent", now);
                return null;
            }).addOnSuccessListener(v -> {
                AppDbHelper h = new AppDbHelper(getContext());
                SQLiteDatabase db = h.getWritableDatabase();
                db.delete("inventory","id=?", new String[]{ String.valueOf(r.id) });
                Toast.makeText(getContext(), "Trajni napitak iskorišćen (+ " + r.bonusValue + "% PP).", Toast.LENGTH_SHORT).show();
                load();
                ((EquipmentActivity)getActivity()).refreshSummary();
            }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        }

        @Override public void onToggleArmor(InventoryAdapter.Row r, boolean activate){
            AppDbHelper h = new AppDbHelper(getContext());
            SQLiteDatabase db = h.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("active", activate ? 1 : 0);
            if (activate && (r.remainingBattles == null || r.remainingBattles <= 0)) {
                cv.put("expires_after_battles", r.durationBattles);
            }
            db.update("inventory", cv, "id=?", new String[]{ String.valueOf(r.id) });

            //  ContentValues cv = new ContentValues();
          //  cv.put("equipped", activate ? 1 : 0);
         //   if (activate) {
                // ako prvi put aktiviraš, podesi remaining_battles
           //    if (r.remainingBattles == null || r.remainingBattles <= 0) {
           //         cv.put("remaining_battles", r.durationBattles);
           //     }
           // }
           // db.update("inventory", cv, "id=?", new String[]{ String.valueOf(r.id) });
            Toast.makeText(getContext(), activate? "Odeća aktivirana." : "Odeća deaktivirana.", Toast.LENGTH_SHORT).show();
            load();
            ((EquipmentActivity)getActivity()).refreshSummary();
        }

        @Override public void onUpgradeWeapon(InventoryAdapter.Row r){
            FirebaseFirestore fs = FirebaseFirestore.getInstance();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference uref = fs.collection("users").document(uid);
            uref.get().addOnSuccessListener(snap -> {
                int level = snap.contains("currentLevel") ? snap.getLong("currentLevel").intValue() : 1;
                int coins = snap.contains("coins") ? snap.getLong("coins").intValue() : 0;
                int prevLevel = Math.max(1, level - 1);
                int base = LevelCalculator.coinsForBoss(prevLevel); // nagrada prethodnog nivoa
                int cost = (int) Math.round(base * 0.6);

                if (coins < cost) {
                    Toast.makeText(getContext(), "Nemaš dovoljno novčića ("+coins+"/"+cost+").", Toast.LENGTH_SHORT).show();
                    return;
                }

                fs.runTransaction(tr -> {
                    // skini novčiće
                    int c = ((Long) tr.get(uref).get("coins")).intValue();
                    tr.update(uref, "coins", c - cost);
                    return null;
                }).addOnSuccessListener(vv -> {
                    // podigni level oružja u inventory
                    AppDbHelper h = new AppDbHelper(getContext());
                    SQLiteDatabase db = h.getWritableDatabase();
                    db.execSQL("UPDATE inventory SET level=level+1 WHERE id=?", new Object[]{ r.id });
                    Toast.makeText(getContext(), "Oružje unapređeno (lvl "+(r.level+1)+").", Toast.LENGTH_SHORT).show();
                    load();
                    ((EquipmentActivity)getActivity()).refreshSummary();
                }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());

            }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }
}
