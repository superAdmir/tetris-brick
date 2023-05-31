package com.tb.tetrisbrick.game.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.ui.score.ScoreActivity;

import static com.tb.tetrisbrick.game.Values.NOTIFICATION_ID;
import static com.tb.tetrisbrick.game.Values.SCORE_CHANNEL;

public class NotificationUtil {

    private final Context context;
    private PendingIntent pendingIntent;
    private final int score;

    public NotificationUtil(Context context, int newScore) {
        this.context = context;
        this.score = newScore;
        initIntent(context);
    }

    private void initIntent(Context context) {
        Intent intent = new Intent(context, ScoreActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);
    }

    private Notification getNotificationBuilder() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SCORE_CHANNEL).setSmallIcon(R.drawable.ic_star).setContentTitle(context.getString(R.string.new_score_notification_title)).setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(pendingIntent).setContentText(context.getString(R.string.congrats_sub_title) + " - " + score + "!").setColorized(true).setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setColor(context.getColor(R.color.colorPrimary));
        }
        return builder.build();
    }

    public void createNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, getNotificationBuilder());
    }

    public static void createChannel(NotificationManager nm, String id, String channelName, int importance) {
        NotificationChannel channel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = nm.getNotificationChannel(id);
            if (channel == null) {
                channel = new NotificationChannel(id, channelName, importance);
                channel.setSound(null, null);
                nm.createNotificationChannel(channel);
            }
        }
    }
}
