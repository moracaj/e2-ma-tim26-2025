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
}
