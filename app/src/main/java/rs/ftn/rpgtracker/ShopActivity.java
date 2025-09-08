package rs.ftn.rpgtracker;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {
    AppDbHelper helper;
    long userId;
    TextView tvCoins;
    ListView listCatalog, listInventory;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        helper = new AppDbHelper(this);
        userId = Prefs.getLoggedUserId(this);
        tvCoins = findViewById(R.id.tvCoins);
        listCatalog = findViewById(R.id.listCatalog);
        listInventory = findViewById(R.id.listInventory);
        refresh();
        listCatalog.setOnItemClickListener((parent, view, position, id) -> confirmBuy((Item) parent.getItemAtPosition(position)));
    }

    private void refresh() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cu = db.rawQuery("SELECT coins FROM users WHERE id=?", new String[]{String.valueOf(userId)});
        if (cu.moveToFirst()) tvCoins.setText("Coins: " + cu.getInt(0));
        cu.close();

        List<Item> catalog = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT id,type,name,bonus_type,bonus_value,price,duration_battles,permanent,upgradeable FROM equipment_catalog", null);
        while (c.moveToNext()) {
            catalog.add(new Item(c.getInt(0), c.getString(1), c.getString(2), c.getString(3),
                    c.getDouble(4), c.getInt(5), c.getInt(6), c.getInt(7) == 1, c.getInt(8) == 1));
        }
        c.close();
        listCatalog.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, catalog));

        List<String> inv = new ArrayList<>();
        Cursor i = db.rawQuery("SELECT inventory.id, equipment_catalog.name, active FROM inventory JOIN equipment_catalog ON equipment_catalog.id = inventory.equipment_id WHERE user_id=?",
                new String[]{String.valueOf(userId)});
        while (i.moveToNext()) inv.add((i.getInt(2)==1?"[ACTIVE] ":"") + i.getString(1));
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
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT coins FROM users WHERE id=?", new String[]{String.valueOf(userId)});
        int coins = 0; if (c.moveToFirst()) coins = c.getInt(0); c.close();
        if (coins < item.price) { Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show(); return; }
        db.execSQL("UPDATE users SET coins = coins - ? WHERE id=?", new Object[]{item.price, userId});
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId); cv.put("equipment_id", item.id); cv.put("active", 0);
        cv.put("expires_after_battles", item.durationBattles); cv.put("permanent", item.permanent ? 1 : 0); cv.put("level", 0);
        db.insert("inventory", null, cv);
        Toast.makeText(this, "Bought " + item.name, Toast.LENGTH_SHORT).show();
        refresh();
    }

    static class Item {
        int id; String type, name, bonusType; double bonusValue; int price, durationBattles; boolean permanent, upgradeable;
        Item(int id, String type, String name, String bonusType, double bonusValue, int price, int durationBattles, boolean permanent, boolean upgradeable) {
            this.id=id; this.type=type; this.name=name; this.bonusType=bonusType; this.bonusValue=bonusValue; this.price=price; this.durationBattles=durationBattles; this.permanent=permanent; this.upgradeable=upgradeable;
        }
        @Override public String toString() { return name + " (" + type + ") - " + price + "c"; }
    }
}