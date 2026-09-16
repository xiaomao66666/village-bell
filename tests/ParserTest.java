import cn.villagebell.Village;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ParserTest {
    static int assertions=0;
    static final long NOW=1700000000000L;
    static void check(boolean result,String label) {assertions++;if(!result)throw new AssertionError(label);}
    static String base(String extra) {
        return "{\"tag\":\"#TEST123\",\"timestamp\":1700000000,\"buildings\":[],\"units\":[],\"heroes\":[],\"spells\":[],\"pets\":[]"+extra+"}";
    }
    static void rejects(String input,String label) throws Exception {try{Village.parse(input,NOW);throw new AssertionError(label);}catch(JSONException expected){assertions++;}}
    public static void main(String[] args) throws Exception {
        Village empty=Village.parse(base(""),NOW);check(empty.upgrades.isEmpty(),"empty village valid");
        JSONObject duplicate=new JSONObject(base(""));
        duplicate.put("buildings",new JSONArray("[{\"data\":1000008,\"lvl\":20,\"timer\":3600},{\"data\":1000008,\"lvl\":20,\"timer\":7200}]"));
        Village d=Village.parse(duplicate.toString(),NOW);
        check(d.upgrades.size()==2,"identical buildings retained");
        check(!d.upgrades.get(0).key.equals(d.upgrades.get(1).key),"distinct instance keys");
        check(d.upgrades.get(0).endMillis==NOW+3600000L,"export time used");
        Village late=Village.parse(duplicate.toString(),NOW+1800000L);
        check(late.upgrades.get(0).endMillis==d.upgrades.get(0).endMillis,"late import doesn't restart timers");
        JSONObject nested=new JSONObject(base(""));
        nested.put("buildings",new JSONArray("[{\"data\":1000097,\"types\":[{\"data\":152000011,\"modules\":[{\"data\":151000033,\"lvl\":1,\"timer\":55}]}]}]"));
        Village n=Village.parse(nested.toString(),NOW);
        check(n.upgrades.size()==1,"nested module found");check(n.upgrades.get(0).name.contains("151000033"),"unknown IDs retained");
        check(!Village.parse(base(",\"boosts\":{\"clocktower_cooldown\":37996}"),NOW).timingUncertain,"cooldown is not a boost");
        check(Village.parse(base(",\"boosts\":{\"lab_boost\":600}"),NOW).timingUncertain,"boost guards notification");
        duplicate.getJSONArray("buildings").getJSONObject(0).put("helper_recurrent",true);
        check(Village.parse(duplicate.toString(),NOW).timingUncertain,"recurring helper guards notification");
        rejects(base("").substring(0,40),"truncated JSON");
        rejects(base("")+"garbage","trailing garbage");
        rejects(base("")+"{}","second JSON");
        rejects("{\"tag\":\"#ABC\",\"timestamp\":1700000000}","partial export");
        rejects(base("").replace("1700000000","\"1700000000\""),"string timestamp");
        rejects(base("").replace("1700000000","1700001000"),"future timestamp");
        duplicate.getJSONArray("buildings").getJSONObject(0).put("timer",-1);rejects(duplicate.toString(),"negative timer");
        duplicate.getJSONArray("buildings").getJSONObject(0).put("timer",0.5);rejects(duplicate.toString(),"fractional timer");
        check(Village.parse("\uFEFF"+base(""),NOW).upgrades.isEmpty(),"UTF8 BOM accepted");
        check(!Village.parse(base(""),NOW+86400001L).warnings.isEmpty(),"old snapshot warning");
        if(args.length>0) {
            String raw=new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8);
            Village real=Village.parse(raw,NOW);
            check(real.upgrades.size()==12,"synthetic sample has 12 upgrades");
            check(!real.timingUncertain,"sample only contains cooldowns");
            check(real.upgrades.get(0).dataId==73000009,"pet finishes first");
            check(real.upgrades.get(0).endMillis==1700000060000L,"pet completion epoch");
            int night=0;for(Village.Upgrade u:real.upgrades)if(u.builderBase())night++;
            check(night==4,"four builder base upgrades");
            check(real.upgrades.get(11).dataId==1000000,"army camp finishes last");
        }
        if(args.length>1) { Village sample=Village.parse(new String(Files.readAllBytes(Paths.get(args[1])),StandardCharsets.UTF_8),System.currentTimeMillis()); check(sample.upgrades.size()>0,"optional local sample parsed"); }
        System.out.println("PASS: "+assertions+" parser checks");
    }
}
