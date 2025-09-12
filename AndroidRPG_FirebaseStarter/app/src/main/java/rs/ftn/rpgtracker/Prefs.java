
package rs.ftn.rpgtracker;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
  private static final String P = "prefs"; private static final String KEY_UID = "uid";
  public static void setUid(Context ctx, String uid){ ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString(KEY_UID, uid).apply(); }
  public static String getUid(Context ctx){ return ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString(KEY_UID, null); }
  public static void clear(Context ctx){ ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply(); }
}
