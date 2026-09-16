import cn.villagebell.*;
import java.util.*;

public final class DashboardTest {
    static int count;
    static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        long exported=1700000000L;
        String raw="{\"tag\":\"#TEST123\",\"timestamp\":"+exported+",\"buildings\":[{\"data\":1000008,\"lvl\":1,\"timer\":3600},{\"data\":1000000,\"lvl\":1,\"timer\":900000}],\"units\":[],\"heroes\":[],\"spells\":[],\"pets\":[],\"buildings2\":[{\"data\":1000041,\"lvl\":1,\"timer\":7200}]}";
        long start=exported*1000L;
        Village v=Village.parse(raw,start);
        Village.Upgrade u=v.upgrades.get(0);
        check(Dashboard.waitingProgress(u,start)==0,"starts at export, not upgrade start");
        check(Dashboard.waitingProgress(u,start+1800000)==.5f,"half of snapshot wait elapsed");
        check(Dashboard.waitingProgress(u,start-1000)==0,"clock rollback clamps progress");
        check(Dashboard.waitingProgress(u,start+9999999)==1,"completion clamps progress");
        check(Dashboard.active(v,"全部",start)==3,"all active count");
        check(Dashboard.active(v,"夜世界",start)==1,"builder base count");
        check(Dashboard.active(v,"建筑与英雄",start)==2,"home building count");
        check(Dashboard.completed(v,start+3600000)==1,"exact boundary counts completed");
        int[] utc=Dashboard.nextSevenDays(v,start,TimeZone.getTimeZone("UTC"));
        check(utc[0]==1 && utc[1]==1,"midnight separates calendar days");
        check(utc[7]==1,"later than seven days retained");
        int[] china=Dashboard.nextSevenDays(v,start,TimeZone.getTimeZone("Asia/Shanghai"));
        check(china[0]==2,"local timezone used for buckets");
        int sum=0;for(int n:Dashboard.nextSevenDays(v,start+3600000,TimeZone.getTimeZone("UTC")))sum+=n;
        check(sum==2,"completed entries excluded from forecast");
        System.out.println("PASS: "+count+" dashboard checks");
    }
}
