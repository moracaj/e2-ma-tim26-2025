package rs.ftn.rpgtracker;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest; // <— NOVO
import androidx.core.app.ActivityCompat; // <— NOVO
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
public class Notifications {
    public static final String CH_INVITES = "invites";
    public static final String CH_CHAT    = "chat_high"; // HIGH važnost

    public static void createChannels(Context ctx){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);

            if (nm.getNotificationChannel(CH_INVITES) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_INVITES, "Alliance Invites", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
            if (nm.getNotificationChannel(CH_CHAT) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_CHAT, "Alliance Chat", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
        }
    }

    public static void show(Context ctx, String channelId, int id,
                            String title, String text, Intent tapIntent, boolean ongoing){
        PendingIntent pi = PendingIntent.getActivity(
                ctx, id, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);

        if (Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return;

        NotificationManagerCompat.from(ctx).notify(id, b.build());
    }
    public static void showInviteWithActions(
            Context ctx,
            int id,
            String title,
            String text,
            PendingIntent tap,
            PendingIntent acceptAction,
            PendingIntent declineAction
    ) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_INVITES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true) // dok korisnik ne prihvati/odbijе, ne može da “skine” notifikaciju
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(tap)
                .addAction(new NotificationCompat.Action(0, "Accept",  acceptAction))
                .addAction(new NotificationCompat.Action(0, "Decline", declineAction));

        if (Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return;

        NotificationManagerCompat.from(ctx).notify(id, b.build());
    }
}
