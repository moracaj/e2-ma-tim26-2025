package rs.ftn.rpgtracker;

import android.app.Application;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // kanali uvek postoje
        Notifications.createChannels(this);
        // globalni listener za chat poruke (radi u celom app-u)
        LiveNotifications.start(this);
    }
}
