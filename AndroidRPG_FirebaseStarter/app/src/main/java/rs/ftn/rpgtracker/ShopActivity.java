package rs.ftn.rpgtracker;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ShopActivity extends AppCompatActivity {
    AppDbHelper helper;
    String uid;
    TextView tvCoins;
    ListView listCatalog, listInventory;
    FirebaseFirestore db;
    private ArrayList<InvItem> invItems = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        helper = new AppDbHelper(this);
        uid = Prefs.getUid(this);
        db = FirebaseFirestore.getInstance();

        tvCoins = findViewById(R.id.tvCoins);
        listCatalog = findViewById(R.id.listCatalog);
        listInventory = findViewById(R.id.listInventory);

        refresh();
        listCatalog.setOnItemClickListener((parent, view, position, id) ->
                confirmBuy((Item) parent.getItemAtPosition(position)));
    }


    private void refresh() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            long coins = doc.getLong("coins") == null ? 0 : doc.getLong("coins");
            tvCoins.setText("Coins: " + coins);
        });

        SQLiteDatabase sql = helper.getReadableDatabase();

        ArrayList<Item> catalog = new ArrayList<>();
        Cursor c = sql.rawQuery(
                "SELECT id,type,name,bonus_type,bonus_value,price,duration_battles,permanent,upgradeable FROM equipment_catalog",
                null
        );
        while (c.moveToNext()) {
            catalog.add(new Item(
                    c.getInt(0), c.getString(1), c.getString(2), c.getString(3),
                    c.getDouble(4), c.getInt(5), c.getInt(6),
                    c.getInt(7) == 1, c.getInt(8) == 1
            ));
        }
        c.close();
        listCatalog.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, catalog));

        ArrayList<String> inv = new ArrayList<>();
        Cursor i = sql.rawQuery(
                "SELECT inventory.id, equipment_catalog.name, active " +
                        "FROM inventory JOIN equipment_catalog ON equipment_catalog.id = inventory.equipment_id " +
                        "WHERE user_uid=?",
                new String[]{uid}
        );
        while (i.moveToNext()) inv.add((i.getInt(2) == 1 ? "[ACTIVE] " : "") + i.getString(1));
        i.close();
        listInventory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, inv));
    }

    private void confirmBuy(Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Buy " + item.name + "?")
                .setMessage("Price: " + item.price + " coins")
                .setPositiveButton("Buy", (d, w) -> buy(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void buy(Item item) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(doc -> {
            long coins = doc.getLong("coins") == null ? 0 : doc.getLong("coins");
            if (coins < item.price) {
                Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show();
                return;
            }
            userRef.update("coins", coins - item.price).addOnSuccessListener(a -> {
                SQLiteDatabase w = helper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("user_uid", uid);
                cv.put("equipment_id", item.id);
                cv.put("active", 0);
                cv.put("expires_after_battles", item.durationBattles);
                cv.put("permanent", item.permanent ? 1 : 0);
                cv.put("level", 0);
                w.insert("inventory", null, cv);
                Toast.makeText(this, "Bought " + item.name, Toast.LENGTH_SHORT).show();
                refresh();
            });
        });
    }

    private int priceFor(String type, String name, String bonusType, double bonusValue, int currentLevel){
        int prevLevel = Math.max(1, currentLevel - 1);
        int base = LevelCalculator.coinsForBoss(prevLevel);

        if ("potion".equals(type)) {
            if ("pp_percent".equals(bonusType) && (int)bonusValue == 20) return (int)Math.round(base * 0.50); // 50%
            if ("pp_percent".equals(bonusType) && (int)bonusValue == 40) return (int)Math.round(base * 0.70); // 70%
            if ("pp_perm_percent".equals(bonusType) && (int)bonusValue == 5)  return (int)Math.round(base * 2.00);   // 200%
            if ("pp_perm_percent".equals(bonusType) && (int)bonusValue == 10) return (int)Math.round(base * 10.00);  // 1000%
        } else if ("armor".equals(type)) {
            // Gloves +10% PP = 60%, Shield +10% HIT = 60%, Boots +40% extra attack = 80%
            if ("pp_percent".equals(bonusType) && (int)bonusValue == 10) return (int)Math.round(base * 0.60);
            if ("hit_percent".equals(bonusType) && (int)bonusValue == 10) return (int)Math.round(base * 0.60);
            if ("extra_attack_chance".equals(bonusType) && (int)bonusValue == 40) return (int)Math.round(base * 0.80);
        }
        // Weapon se NE kupuje u shopu po specifikaciji — vrati veliko da onemogući kupovinu
        return Integer.MAX_VALUE;
    }



    static class InvItem {
        int invId;           // inventory.id
        String type;         // ec.type  -> "potion" | "armor" | "weapon"
        String name;         // ec.name
        String bonusType;    // ec.bonus_type
        double bonusValue;   // ec.bonus_value
        int active;          // inventory.active (0/1)
        int permanent;       // inventory.permanent (0/1)
        Integer expires;     // inventory.expires_after_battles (može null)
        int level;           // inventory.level
        int durationBattles; // ec.duration_battles
        int upgradeable;     // ec.upgradeable (0/1)
    }
    static class Item {
        int id;
        String type, name, bonusType;
        double bonusValue;
        int price, durationBattles;
        boolean permanent, upgradeable;

        Item(int id, String type, String name, String bonusType, double bonusValue,
             int price, int durationBattles, boolean permanent, boolean upgradeable) {
            this.id = id; this.type = type; this.name = name; this.bonusType = bonusType;
            this.bonusValue = bonusValue; this.price = price; this.durationBattles = durationBattles;
            this.permanent = permanent; this.upgradeable = upgradeable;
        }
        @Override public String toString() { return name + " (" + type + ") - " + price + "c"; }
    }

    // primer helper metoda u ShopActivity
    private void insertIntoInventoryFromCatalog(SQLiteDatabase db, int catalogId){
        db.execSQL("INSERT INTO inventory(equipment_id, user_uid, permanent, level, active, expires_after_battles)\n" +
                        "SELECT id, ?, permanent, 0, 0, NULL FROM equipment_catalog WHERE id=?\n",
                new Object[]{ catalogId });
    }

}
