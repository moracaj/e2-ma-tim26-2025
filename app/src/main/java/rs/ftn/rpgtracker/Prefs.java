package rs.ftn.rpgtracker;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static final String P = "prefs";
    private static final String KEY_LOGGED_ID = "logged_user_id";
    public static void setLoggedUserId(Context ctx, long id) {
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit().putLong(KEY_LOGGED_ID, id).apply();
    }
    public static long getLoggedUserId(Context ctx) {
        return ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getLong(KEY_LOGGED_ID, -1);
    }
    public static void clear(Context ctx) {
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply();
    }
}