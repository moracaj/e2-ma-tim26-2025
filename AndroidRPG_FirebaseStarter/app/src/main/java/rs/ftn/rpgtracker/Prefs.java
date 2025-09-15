
package rs.ftn.rpgtracker;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
public class Prefs {
  private static final String P = "prefs"; private static final String KEY_UID = "uid";
  public static void setUid(Context ctx, String uid){ ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString(KEY_UID, uid).apply(); }
  public static String getUid(Context ctx){
    if (FirebaseAuth.getInstance().getCurrentUser() != null){
      return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    // fallback – ako već čuvaš uid u SharedPreferences, zameni po potrebi
    return ctx.getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("uid", "");
  }
  public static void clear(Context ctx){ ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply(); }
}
