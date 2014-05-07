package co.juliansuarez.joggingtracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.parse.Parse;

/**
 * Created by julian on 3/05/14.
 */
public class JoggingTrackerApp extends Application {

    public static JoggingTrackerApp APP;

    @Override
    public void onCreate() {
        super.onCreate();
        APP = this;
        Parse.initialize(this, "9iPhRIzPeAavGN0A15n021n3Edowf9XtXTzz8VOS",
                         "RkGdAZhsAFdbPww4pd2NWpUa1MiabVWZR8DEUP1Z");
    }

    private SharedPreferences getSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(JoggingTrackerApp.class.getName(),
                                                       Context.MODE_PRIVATE);
        return prefs;
    }

    public void saveString(String key, String value) {
        SharedPreferences prefs = getSharedPreferences();
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, value);
        edit.commit();
    }

    public String getString(String key, String defValue) {
        SharedPreferences prefs = getSharedPreferences();
        return prefs.getString(key, defValue);
    }
}
