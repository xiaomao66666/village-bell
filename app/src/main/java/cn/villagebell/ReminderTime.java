package cn.villagebell;
import java.util.*;

/** Local wall-clock quiet hours. Delayed reminders are delivered, never discarded. */
public final class ReminderTime {
    public static long deliveryTime(long event,long now,boolean quiet,int start,int end,TimeZone zone){
        long time=Math.max(event,now);
        if(!quiet||start==end)return time;
        Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(time);
        int minute=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
        boolean inside=start<end ? minute>=start&&minute<end : minute>=start||minute<end;
        if(!inside)return time;
        if(start>end&&minute>=start)c.add(Calendar.DATE,1);
        c.set(Calendar.HOUR_OF_DAY,end/60);c.set(Calendar.MINUTE,end%60);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    public static long earlyTime(Village.Upgrade u,int minutes,long now,boolean quiet,int start,int end,TimeZone zone){
        if(minutes<=0||now>=u.endMillis)return Long.MAX_VALUE;
        long candidate=deliveryTime(u.endMillis-minutes*60000L,now,quiet,start,end,zone);
        return candidate<u.endMillis?candidate:Long.MAX_VALUE;
    }
}
