package cn.villagebell;
import android.content.*;
public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) { Reminders.receive(context,intent); }
}
