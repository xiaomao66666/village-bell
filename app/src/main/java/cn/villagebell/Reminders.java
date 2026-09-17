package cn.villagebell;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;

final class Reminders {
    static final String CHANNEL="upgrades", TEST_CHANNEL="test";
    static final int ALARM_ID=301, TEST_ID=302, NOTIFICATION_ID=401;
    static void channels(Context c) {
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"升级完成提醒",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("按最近一次村庄导出数据安排的升级完成提醒");
        nm.createNotificationChannel(channel);
        nm.createNotificationChannel(new NotificationChannel(TEST_CHANNEL,"提醒测试",NotificationManager.IMPORTANCE_HIGH));
    }
    static boolean allowed(Context c) {
        channels(c);
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        return (Build.VERSION.SDK_INT<33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
            && nm.areNotificationsEnabled() && nm.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }
    static boolean exact(Context c) {
        return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    private static PendingIntent intent(Context c,String revision,boolean test) {
        Intent i=new Intent(c,ReminderReceiver.class).setAction(test?"test":"upgrade").putExtra("revision",revision);
        return PendingIntent.getBroadcast(c,test?TEST_ID:ALARM_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c) {
        c.getSystemService(AlarmManager.class).cancel(intent(c,"",false));
    }
    static String schedule(Context c) {
        cancel(c);
        State state=new State(c);
        VillageWidget.updateAll(c);
        if(state.village==null||!state.enabled||state.village.timingUncertain||!allowed(c)) return "";
        long next=Long.MAX_VALUE;
        long now=System.currentTimeMillis();
        for(Village.Upgrade u:state.village.upgrades) if(state.eligible(u)) {
            next=Math.min(next,ReminderTime.deliveryTime(u.endMillis,now,state.quiet,state.quietStart,state.quietEnd,TimeZone.getDefault()));
            if(!state.earlyDelivered.contains(u.key))next=Math.min(next,ReminderTime.earlyTime(u,state.advanceMinutes,now,state.quiet,state.quietStart,state.quietEnd,TimeZone.getDefault()));
        }
        if(next==Long.MAX_VALUE) return "";
        try { set(c,Math.max(System.currentTimeMillis()+1000,next),intent(c,state.village.revision(),false)); return ""; }
        catch(RuntimeException e) { return "系统未能安排提醒，请检查闹钟权限后重试。"; }
    }
    private static void set(Context c,long time,PendingIntent intent) {
        AlarmManager am=c.getSystemService(AlarmManager.class);
        if(exact(c)) {
            try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,intent); return; }
            catch(SecurityException ignored) { /* permission changed between checking and scheduling */ }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,intent);
    }
    static void test(Context c) { set(c,System.currentTimeMillis()+10_000,intent(c,"",true)); }
    static void receive(Context c,Intent intent) {
        channels(c);
        if("test".equals(intent.getAction())) {
            if(allowed(c)) show(c,402,TEST_CHANNEL,"提醒测试成功","到点提醒已送达，可以返回游戏了。",Collections.emptyList());
            return;
        }
        State s=new State(c);
        if(s.village==null || !s.village.revision().equals(intent.getStringExtra("revision")) || !s.enabled || s.village.timingUncertain) return;
        if(!allowed(c)) return;
        List<String> lines=new ArrayList<>();
        long now=System.currentTimeMillis();
        if(ReminderTime.deliveryTime(now,now,s.quiet,s.quietStart,s.quietEnd,TimeZone.getDefault())>now){schedule(c);return;}
        for(Village.Upgrade u:s.village.upgrades) {
            if(!s.eligible(u))continue;
            if(u.endMillis<=now) { lines.add("预计已完成 · "+u.name); s.delivered.add(u.key); }
            else if(!s.earlyDelivered.contains(u.key)&&s.advanceMinutes>0&&u.endMillis-s.advanceMinutes*60000L<=now){
                lines.add("约 "+Math.max(1,(u.endMillis-now+59999)/60000)+" 分钟后 · "+u.name);s.earlyDelivered.add(u.key);
            }
        }
        if(!lines.isEmpty()) {
            try {
                show(c,NOTIFICATION_ID,CHANNEL,lines.size()==1?lines.get(0):lines.size()+" 项村庄升级提醒",
                    "按上次导出计时，请进入游戏确认并安排下一项升级。",lines);
                s.save();
            } catch(SecurityException ignored) { return; }
        }
        schedule(c);
    }
    private static void show(Context c,int id,String channel,String title,String text,List<String> lines) {
        Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title).setContentText(text).setContentIntent(pi).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_REMINDER);
        if(lines.size()>1) { Notification.InboxStyle style=new Notification.InboxStyle().setSummaryText(text); for(String line:lines) style.addLine(line); b.setStyle(style); }
        else b.setStyle(new Notification.BigTextStyle().bigText(text));
        c.getSystemService(NotificationManager.class).notify(id,b.build());
    }
}
