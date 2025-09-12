
package rs.ftn.rpgtracker;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class AppDbHelper extends SQLiteOpenHelper {
  public static final String DB_NAME="rpgtracker.db"; public static final int DB_VERSION=1;
  public AppDbHelper(Context c){ super(c, DB_NAME, null, DB_VERSION); }
  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE equipment_catalog (id INTEGER PRIMARY KEY, type TEXT, name TEXT, bonus_type TEXT, bonus_value REAL, price INTEGER, duration_battles INTEGER, permanent INTEGER, upgradeable INTEGER)");
    db.execSQL("CREATE TABLE inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, equipment_id INTEGER, active INTEGER DEFAULT 0, expires_after_battles INTEGER, permanent INTEGER, level INTEGER DEFAULT 0)");
    db.execSQL("INSERT INTO equipment_catalog (id,type,name,bonus_type,bonus_value,price,duration_battles,permanent,upgradeable) VALUES " +
               "(1,'potion','PP +20%','pp_percent',20,100,0,0,0)," +
               "(2,'potion','PP +40%','pp_percent',40,140,0,0,0)," +
               "(3,'potion','Permanent PP +5%','pp_perm_percent',5,400,0,1,0)," +
               "(4,'armor','Gloves +10% PP','pp_percent',10,120,2,0,0)," +
               "(5,'armor','Shield +10% hit','hit_percent',10,120,2,0,0)," +
               "(6,'armor','Boots +1 attack chance 40%','extra_attack_chance',40,160,2,0,0)," +
               "(7,'weapon','Sword +5% PP','pp_perm_percent',5,0,0,1,1)");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int o,int n){}
}
