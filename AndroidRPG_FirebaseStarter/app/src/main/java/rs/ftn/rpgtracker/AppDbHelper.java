package rs.ftn.rpgtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDbHelper extends SQLiteOpenHelper {
  public static final String DB_NAME = "rpgtracker.db";
  // ↑ podigni verziju
  public static final int DB_VERSION = 2;

  public AppDbHelper(Context c){ super(c, DB_NAME, null, DB_VERSION); }

  @Override public void onCreate(SQLiteDatabase db){
    // Katalog opreme
    db.execSQL(
            "CREATE TABLE equipment_catalog (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " type TEXT NOT NULL," +                // 'potion' | 'armor' | 'weapon'
                    " name TEXT NOT NULL," +
                    " bonus_type TEXT NOT NULL," +          // 'pp_percent' | 'pp_perm_percent' | 'hit_percent' | 'extra_attack_chance' | 'coin_perm_percent'
                    " bonus_value REAL NOT NULL," +
                    " price INTEGER NOT NULL," +
                    " duration_battles INTEGER NOT NULL," + // 0=jednokratno; >0=traje toliko borbi kad se aktivira
                    " permanent INTEGER NOT NULL," +        // 1=trajno (kupljeno/iskorišćeno daje trajan bonus)
                    " upgradeable INTEGER NOT NULL" +       // 1=može upgrade
                    ")"
    );

    // Inventar igrača
    db.execSQL(
            "CREATE TABLE inventory (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " type TEXT NOT NULL," +
                    " name TEXT NOT NULL," +
                    " bonus_type TEXT NOT NULL," +
                    " bonus_value REAL NOT NULL," +
                    " duration_battles INTEGER NOT NULL," +  // za ARMOR: koliko traje kad se aktivira
                    " permanent INTEGER NOT NULL," +          // 1 = trajno (potion perm / weapon)
                    " level INTEGER DEFAULT 0," +             // za oružje upgrade nivo
                    " equipped INTEGER DEFAULT 0," +          // 1 = aktivirano (potion-next-battle / armor aktivan)
                    " remaining_battles INTEGER" +            // ARMOR: preostale borbe od trenutne aktivacije
                    ")"
    );

    // Inicijalni katalog (cene za demo su fiksne; možeš i dinamički da računaš u Shop-u)
    db.execSQL(
            "INSERT INTO equipment_catalog(type,name,bonus_type,bonus_value,price,duration_battles,permanent,upgradeable) VALUES " +
                    "('potion','PP +20%','pp_percent',20,100,0,0,0)," +
                    "('potion','PP +40%','pp_percent',40,140,0,0,0)," +
                    "('potion','Permanent PP +5%','pp_perm_percent',5,400,0,1,0)," +
                    "('armor','Gloves +10% PP','pp_percent',10,120,2,0,0)," +
                    "('armor','Shield +10% hit','hit_percent',10,120,2,0,0)," +
                    "('armor','Boots +1 attack chance 40%','extra_attack_chance',40,160,2,0,0)," +
                    "('weapon','Sword +5% PP','pp_perm_percent',5,0,0,1,1)," +
                    "('weapon','Bow +5% coins','coin_perm_percent',5,0,0,1,1)"
    );
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV){
    if (oldV < 2) {
      // Dodaj nove kolone u inventory ako nedostaju
      db.execSQL("ALTER TABLE inventory ADD COLUMN equipped INTEGER DEFAULT 0");
      db.execSQL("ALTER TABLE inventory ADD COLUMN remaining_battles INTEGER");
      // Ubaci Bow u katalog ako ga nema
      db.execSQL("INSERT INTO equipment_catalog(type,name,bonus_type,bonus_value,price,duration_battles,permanent,upgradeable) " +
              "SELECT 'weapon','Bow +5% coins','coin_perm_percent',5,0,0,1,1 " +
              "WHERE NOT EXISTS (SELECT 1 FROM equipment_catalog WHERE name='Bow +5% coins')");
    }
  }
}
