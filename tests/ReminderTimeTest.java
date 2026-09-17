import cn.villagebell.*;
import java.util.*;
import java.text.*;

public final class ReminderTimeTest {
    static int count;static final TimeZone Z=TimeZone.getTimeZone("Asia/Shanghai");
    static void check(boolean value,String label){count++;if(!value)throw new AssertionError(label);}
    static long at(String value)throws Exception{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm");f.setTimeZone(Z);return f.parse(value).getTime();}
    public static void main(String[] args)throws Exception{
        long nine=at("2026-09-17 09:00"),night=at("2026-09-17 23:00"),morning=at("2026-09-18 08:00");
        check(ReminderTime.deliveryTime(night,nine,true,1320,480,Z)==morning,"cross midnight defers");
        check(ReminderTime.deliveryTime(at("2026-09-18 07:00"),nine,true,1320,480,Z)==morning,"morning quiet defers");
        check(ReminderTime.deliveryTime(morning,nine,true,1320,480,Z)==morning,"end is exclusive");
        check(ReminderTime.deliveryTime(at("2026-09-17 22:00"),nine,true,1320,480,Z)==morning,"start is inclusive");
        check(ReminderTime.deliveryTime(nine,night,true,1320,480,Z)==morning,"overdue reminders respect current quiet hours");
        check(ReminderTime.deliveryTime(night,nine,false,1320,480,Z)==night,"disabled quiet hours");
        check(ReminderTime.deliveryTime(night,nine,true,480,480,Z)==night,"equal boundaries do not mute all day");
        check(ReminderTime.deliveryTime(nine,nine,true,480,600,Z)==at("2026-09-17 10:00"),"daytime quiet period");
        String raw="{\"tag\":\"#TEST123\",\"timestamp\":"+(nine/1000)+",\"buildings\":[{\"data\":1000008,\"lvl\":1,\"timer\":3600}],\"units\":[],\"heroes\":[],\"spells\":[],\"pets\":[]}";
        Village.Upgrade u=Village.parse(raw,nine).upgrades.get(0);
        check(ReminderTime.earlyTime(u,15,nine,false,0,0,Z)==nine+45*60000,"early event time");
        check(ReminderTime.earlyTime(u,0,nine,false,0,0,Z)==Long.MAX_VALUE,"early off");
        check(ReminderTime.earlyTime(u,15,u.endMillis,false,0,0,Z)==Long.MAX_VALUE,"never send stale early notice");
        check(ReminderTime.earlyTime(u,15,nine,true,540,660,Z)==Long.MAX_VALUE,"quiet early superseded by completion");
        check(Dashboard.matches(u,"全部","加农",false,false,nine,Z),"search partial Chinese name");
        check(Dashboard.matches(u,"全部","1000008",false,false,nine,Z),"search ID");
        check(!Dashboard.matches(u,"研究","",false,false,nine,Z),"category conjunction");
        check(!Dashboard.matches(u,"全部","",true,false,u.endMillis,Z),"hide exact completion boundary");
        check(Dashboard.matches(u,"全部","",false,true,nine,Z),"today matches local date");
        check(!Dashboard.matches(u,"全部","",false,true,morning,Z),"yesterday excluded");
        System.out.println("PASS: "+count+" reminder and filter checks");
    }
}
