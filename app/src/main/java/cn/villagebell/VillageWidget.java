package cn.villagebell;
import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.*;

public final class VillageWidget extends AppWidgetProvider {
    static void updateAll(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        int[] ids=manager.getAppWidgetIds(new ComponentName(c,VillageWidget.class));
        if(ids.length>0)new VillageWidget().onUpdate(c,manager,ids);
    }
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){
        State state=new State(c);Village v=state.village;long now=System.currentTimeMillis();
        Village.Upgrade next=null;if(v!=null)for(Village.Upgrade u:v.upgrades)if(u.endMillis>now){next=u;break;}
        for(int id:ids){RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.village_widget);
            views.setTextViewText(R.id.widget_title,next!=null?next.name:v==null?"把村庄放在桌面":"这一轮预计已完成");
            views.setTextViewText(R.id.widget_time,next!=null?new SimpleDateFormat("MM月dd日 HH:mm",Locale.CHINA).format(new Date(next.endMillis))+" 预计完成":v==null?"导入数据后显示最近升级":"回游戏安排升级后再同步");
            views.setTextViewText(R.id.widget_status,v==null?"离线助手 · 点击开始":(v.timingUncertain?"加速待校正 · ":"")+"数据导出于 "+new SimpleDateFormat("MM/dd HH:mm",Locale.CHINA).format(new Date(v.timestamp*1000L)));
            if(next!=null&&GameIcons.bitmap(c,next)!=null)views.setImageViewBitmap(R.id.widget_icon,GameIcons.bitmap(c,next));
            else views.setImageViewResource(R.id.widget_icon,R.drawable.launcher_foreground);
            Intent open=new Intent(c,MainActivity.class).setAction("widget-open");
            views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,501,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            Intent sync=new Intent(c,MainActivity.class).setAction("widget-sync");
            views.setOnClickPendingIntent(R.id.widget_sync,PendingIntent.getActivity(c,502,sync,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            manager.updateAppWidget(id,views);
        }
    }
}
