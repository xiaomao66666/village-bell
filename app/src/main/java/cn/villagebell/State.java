package cn.villagebell;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;
import org.json.JSONException;

final class State {
    final SharedPreferences prefs;
    Village village;
    String error;
    final Set<String> muted, delivered;
    boolean enabled;
    State(Context context) {
        prefs=context.getSharedPreferences("village",Context.MODE_PRIVATE);
        enabled=prefs.getBoolean("enabled",true);
        muted=new HashSet<>(prefs.getStringSet("muted",Collections.emptySet()));
        delivered=new HashSet<>(prefs.getStringSet("delivered",Collections.emptySet()));
        String raw=prefs.getString("raw",null);
        if(raw!=null) try { village=Village.parse(raw,System.currentTimeMillis()); }
        catch(JSONException e) { error="已保存的数据暂时无法读取，请检查手机时间或重新导入。"; }
    }
    boolean save() {
        SharedPreferences.Editor editor=prefs.edit().putBoolean("enabled",enabled)
            .putStringSet("muted",muted).putStringSet("delivered",delivered);
        if(village!=null) editor.putString("raw",village.raw); else editor.remove("raw");
        return editor.commit();
    }
    void replace(Village incoming) {
        village=incoming; muted.clear(); delivered.clear();
        long now=System.currentTimeMillis();
        // Importing historical snapshots must not produce a burst of stale notifications.
        for(Village.Upgrade u:incoming.upgrades) if(u.endMillis<=now) delivered.add(u.key);
    }
    boolean eligible(Village.Upgrade u) {
        return enabled && village!=null && !village.timingUncertain && !muted.contains(u.key) && !delivered.contains(u.key);
    }
}
