package cn.villagebell;

import java.util.*;

/** Presentation calculations: never infer true upgrade-start time from an export. */
public final class Dashboard {
    public static boolean matches(Village.Upgrade u,String group,String query,boolean hideCompleted,boolean todayOnly,long now,TimeZone zone){
        if(!group.equals("全部")&&!u.group().equals(group))return false;
        if(hideCompleted&&u.endMillis<=now)return false;
        String q=query.trim().toLowerCase(Locale.ROOT);
        if(!q.isEmpty()&&!(u.name+" "+u.group()+" "+u.dataId).toLowerCase(Locale.ROOT).contains(q))return false;
        if(todayOnly){Calendar a=Calendar.getInstance(zone),b=Calendar.getInstance(zone);a.setTimeInMillis(now);b.setTimeInMillis(u.endMillis);if(a.get(Calendar.YEAR)!=b.get(Calendar.YEAR)||a.get(Calendar.DAY_OF_YEAR)!=b.get(Calendar.DAY_OF_YEAR))return false;}
        return true;
    }
    public static float waitingProgress(Village.Upgrade u,long now) {
        if(u.seconds<=0)return 1f;
        long snapshot=u.endMillis-u.seconds*1000L;
        return (float)Math.max(0,Math.min(1,(now-snapshot)/(u.seconds*1000d)));
    }
    public static int active(Village v,String group,long now){int n=0;for(Village.Upgrade u:v.upgrades)if(u.endMillis>now&&(group.equals("全部")||group.equals(u.group())))n++;return n;}
    public static int completed(Village v,long now){return v.upgrades.size()-active(v,"全部",now);}
    public static int[] nextSevenDays(Village v,long now,TimeZone zone) {
        int[] result=new int[8];
        Calendar day=Calendar.getInstance(zone);day.setTimeInMillis(now);day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
        long[] edges=new long[8];for(int i=0;i<8;i++){edges[i]=day.getTimeInMillis();day.add(Calendar.DATE,1);}
        for(Village.Upgrade u:v.upgrades){if(u.endMillis<=now)continue;int bucket=7;for(int i=0;i<7;i++)if(u.endMillis<edges[i+1]){bucket=i;break;}result[bucket]++;}
        return result;
    }
}
