package cn.villagebell;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff101a1d, PANEL=0xff1a292c, GOLD=0xffe5b95c, TEXT=0xfff2f4eb, MUTED=0xff9daeb0, GREEN=0xff9cd5ae;
    private LinearLayout root, content;
    private State state;
    private int page=0;
    private String filter="全部";
    private boolean foreground, busy;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Map<TextView,Village.Upgrade> countdowns=new HashMap<>();
    private final Runnable ticker=new Runnable() {
        @Override public void run() { if(!foreground)return; for(Map.Entry<TextView,Village.Upgrade> e:countdowns.entrySet()) e.getKey().setText(remaining(e.getValue().endMillis)); handler.postDelayed(this,1000); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); Reminders.channels(this);
        if(saved!=null) { page=saved.getInt("page"); filter=saved.getString("filter","全部"); }
        render(); handleIntent(getIntent());
    }
    @Override protected void onResume() { super.onResume(); foreground=true; String problem=Reminders.schedule(this); render(); handler.removeCallbacks(ticker); handler.post(ticker); if(!problem.isEmpty()) toast(problem); }
    @Override protected void onPause() { foreground=false; handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
    @Override public void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putInt("page",page); out.putString("filter",filter); }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleIntent(i); }
    private void handleIntent(Intent i) {
        if(!Intent.ACTION_SEND.equals(i.getAction()))return;
        String text=i.getStringExtra(Intent.EXTRA_TEXT);
        Uri uri=i.getParcelableExtra(Intent.EXTRA_STREAM);
        i.setAction(null);
        if(text!=null) parse(text); else if(uri!=null) readUri(uri);
    }
    private void render() {
        state=new State(this); countdowns.clear();
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        setContentView(root); root.requestApplyInsets();
        LinearLayout header=column(); header.setPadding(dp(22),dp(18),dp(22),dp(12));
        TextView brand=text("村庄铃铛",24,TEXT,true); header.addView(brand);
        header.addView(text("让每一次上线，都刚刚好。",13,MUTED,false)); root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=column(); content.setPadding(dp(20),dp(4),dp(20),dp(24)); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page==0) dashboard(); else if(page==1) settingsPage(); else helpPage();
        LinearLayout nav=new LinearLayout(this); nav.setPadding(dp(12),dp(8),dp(12),dp(8)); nav.setBackgroundColor(PANEL);
        String[] tabs={"升级","提醒","使用说明"};
        for(int n=0;n<tabs.length;n++) { final int target=n; Button b=button(tabs[n],n==page,()->{page=target;render();}); nav.addView(b,new LinearLayout.LayoutParams(0,dp(50),1)); }
        root.addView(nav);
    }
    private void dashboard() {
        if(state.error!=null) notice(state.error);
        Village v=state.village;
        if(v==null) {
            LinearLayout hero=card(); hero.setBackground(gradient());
            hero.addView(text("升级交给工人\n提醒交给铃铛",29,TEXT,true)); gap(hero,16);
            hero.addView(text("从游戏导入村庄数据，建筑、英雄和研究计时会一起出现。",15,0xffcfdbcf,false)); gap(hero,20);
            hero.addView(button("＋  导入村庄数据",true,this::importDialog)); content.addView(hero); gap(content,20);
            step("01","在游戏中导出","设置 → 更多设置 → 导出村庄数据");
            step("02","回到这里粘贴","也可以选择 .json / .txt 文件，或通过分享导入。");
            step("03","开启完成提醒","允许通知和准时提醒，再发送一次测试通知。");
            return;
        }
        long now=System.currentTimeMillis(); int active=0, home=0, night=0; Village.Upgrade next=null;
        for(Village.Upgrade u:v.upgrades) if(u.endMillis>now) {active++; if(u.builderBase())night++;else home++; if(next==null)next=u;}
        LinearLayout hero=card(); hero.setBackground(gradient());
        hero.addView(text("下一项预计完成",12,0xffc5d7c5,false)); gap(hero,10);
        if(next!=null) {
            hero.addView(text(next.name,27,TEXT,true));
            TextView time=text(remaining(next.endMillis),23,GOLD,true); countdowns.put(time,next); hero.addView(time); gap(hero,10);
            hero.addView(text(format(next.endMillis)+" · "+next.group(),13,0xffcad8cc,false));
        } else { hero.addView(text("这一轮等待结束了",26,TEXT,true)); gap(hero,8); hero.addView(text("回到游戏安排升级，再导出一次新的数据。",14,TEXT,false)); }
        gap(hero,20); hero.addView(text(active+" 项进行中     主世界 "+home+"  /  夜世界 "+night,14,TEXT,false));
        content.addView(hero); gap(content,14);
        LinearLayout info=column(); info.addView(text(v.tag+"  ·  本机保存",13,MUTED,false));
        info.addView(text("最近同步  "+format(v.timestamp*1000L),12,MUTED,false)); content.addView(info); gap(content,10);
        content.addView(button("同步新的村庄数据",true,this::importDialog)); gap(content,12);
        if(!Reminders.allowed(this)) notice("通知尚未开启。到「提醒」页授权，才能收到完成通知。");
        else if(!state.enabled) notice("完成提醒已暂停，可在「提醒」页恢复。");
        else if(!Reminders.exact(this)) notice("当前使用普通提醒，系统可能延迟。到「提醒」页开启准时提醒。");
        for(String warning:v.warnings) notice(warning);
        HorizontalScrollView filters=new HorizontalScrollView(this); filters.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this);
        for(String f:new String[]{"全部","建筑与英雄","研究","战宠","夜世界"}) {
            Button b=button(f,f.equals(filter),()->{filter=f;render();}); chips.addView(b); }
        filters.addView(chips); content.addView(filters); gap(content,10);
        int count=0;
        for(Village.Upgrade u:v.upgrades) if(filter.equals("全部")||filter.equals(u.group())) { upgradeCard(u,now);count++; }
        if(count==0) notice("这份数据中没有该类别的升级任务。");
        gap(content,8); content.addView(text("时间依据最近一次导出推算。新升级、药水、助手或立即完成后，请再次同步。",12,MUTED,false));
    }
    private void upgradeCard(Village.Upgrade u,long now) {
        LinearLayout box=card();
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout label=column(); label.addView(text(u.name,18,TEXT,true)); label.addView(text(u.group()+"  ·  导出等级 "+u.level,12,MUTED,false));
        row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Switch sw=new Switch(this); sw.setContentDescription(u.name+"的完成提醒");
        sw.setChecked(!state.muted.contains(u.key)); sw.setEnabled(u.endMillis>now&&!state.village.timingUncertain);
        sw.setOnCheckedChangeListener((b,checked)->{State fresh=new State(this); if(checked)fresh.muted.remove(u.key);else fresh.muted.add(u.key); if(!fresh.save())toast("保存失败，请重试。"); String result=Reminders.schedule(this); if(!result.isEmpty())toast(result);});
        row.addView(sw); box.addView(row); gap(box,14);
        TextView remaining=text(remaining(u.endMillis),20,u.endMillis>now?GOLD:GREEN,true); countdowns.put(remaining,u); box.addView(remaining);
        box.addView(text(format(u.endMillis)+" 预计完成",12,MUTED,false)); content.addView(box); gap(content,10);
    }
    private void settingsPage() {
        content.addView(text("把提醒准备好",25,TEXT,true)); gap(content,6);
        content.addView(text("开启一次，完成时间交给手机记住。",14,MUTED,false));gap(content,18);
        LinearLayout master=card(); Switch enabled=new Switch(this); enabled.setText("升级完成提醒"); enabled.setTextColor(TEXT);enabled.setTextSize(18);enabled.setChecked(state.enabled);
        enabled.setOnCheckedChangeListener((b,c)->{State fresh=new State(this);fresh.enabled=c;if(!fresh.save())toast("保存失败，请重试。");Reminders.schedule(this);render();});
        master.addView(enabled);gap(master,8);master.addView(text("可在升级列表中单独关闭某一项。",13,MUTED,false));content.addView(master);gap(content,12);
        permissionCard("1. 通知权限",Reminders.allowed(this)?"已开启":"尚未开启","允许铃铛在升级预计完成时通知你。",this::notificationPermission);
        permissionCard("2. 准时提醒",Reminders.exact(this)?"已开启":"普通提醒，可能延迟","开启「闹钟和提醒」权限，尽量按预计时间送达。",()->{
            if(Build.VERSION.SDK_INT>=31) open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
            else toast("当前系统无需额外授权。");
        });
        LinearLayout test=card();test.addView(text("离开 APP，也试一次",18,TEXT,true));gap(test,8);
        test.addView(text("点击后切到桌面，10 秒后检查通知。未开启准时提醒时，测试也可能延迟。",14,MUTED,false));gap(test,14);
        test.addView(button("发送 10 秒测试提醒",true,()->{
            if(!Reminders.allowed(this)){notificationPermission();return;}
            try{Reminders.test(this);toast("测试已安排，请切到桌面等待通知。");}catch(RuntimeException e){toast("测试安排失败，请检查系统提醒权限。");}
        }));content.addView(test);gap(content,12);
        notice("部分手机会限制后台提醒。如测试无法送达，请在系统应用设置中检查自启动、电池管理和通知设置。强行停止应用后，需要重新打开以恢复提醒。");
        content.addView(button("打开系统应用设置",false,()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))));
    }
    private void helpPage() {
        content.addView(text("少一点等待\n多一点刚刚好",27,TEXT,true));gap(content,20);
        step("01","升级后同步一次","游戏 → 设置 → 更多设置 → 导出村庄数据，回到铃铛粘贴导入。无需逐项输入时间。");
        step("02","一份数据，一个村庄","第一版管理一个村庄。重新导入会替换旧任务与提醒；更换村庄前会显示预览。");
        step("03","使用加速后再同步","本应用不会连接游戏服务器。导出之后发生的变化，只有再次导入才能知道。");
        step("04","遇到特殊加速","包含活动加速或自动助手的快照暂不安排到点提醒；加速结束后重新导出。冷却时间不会被误认为加速。");
        LinearLayout privacy=card();privacy.addView(text("你的村庄，留在你的手机",18,TEXT,true));gap(privacy,10);
        privacy.addView(text("不需要游戏密码，不上传数据，没有网络权限。不使用后台剪贴板监听。仅在你点击粘贴、选择文件或分享时读取数据。",14,MUTED,false));
        gap(privacy,16);privacy.addView(text("村庄铃铛 0.1.0 · 非官方工具\n时间为预测值，请以游戏内实际状态为准。",12,MUTED,false));content.addView(privacy);gap(content,12);
        if(state.village!=null) content.addView(button("清除本机村庄数据",false,()->new AlertDialog.Builder(this).setTitle("清除村庄数据？")
            .setMessage("删除本机保存的导出数据，并取消全部升级提醒。")
            .setNegativeButton("保留",null).setPositiveButton("清除",(d,w)->{
                Reminders.cancel(this);State s=new State(this);s.village=null;s.muted.clear();s.delivered.clear();s.save();
                getSystemService(NotificationManager.class).cancel(Reminders.NOTIFICATION_ID);page=0;render();
            }).show()));
    }
    private void permissionCard(String title,String status,String explanation,Runnable action) {
        LinearLayout box=card();box.addView(text(title,18,TEXT,true));box.addView(text(status,14,GOLD,true));gap(box,8);box.addView(text(explanation,14,MUTED,false));gap(box,12);
        box.addView(button("设置",false,action));content.addView(box);gap(content,12);
    }
    private void notificationPermission() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
            && !getPreferences(0).getBoolean("notificationAsked",false)) {
            getPreferences(0).edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},90);
        } else {
            Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());open(i);
        }
    }
    @Override public void onRequestPermissionsResult(int code,String[] p,int[] result){super.onRequestPermissionsResult(code,p,result);Reminders.schedule(this);render();}
    private void importDialog() {
        if(busy){toast("正在读取数据，请稍候。");return;}
        LinearLayout box=column();box.setPadding(dp(20),dp(10),dp(20),dp(4));
        box.addView(text("导出后粘贴，或选择 JSON / TXT 文件。",14,MUTED,false));
        EditText input=new EditText(this);input.setHint("在这里粘贴村庄数据…");input.setTextSize(13);input.setMinLines(4);input.setMaxLines(7);
        input.setGravity(Gravity.TOP);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Village.MAX_CHARS)});box.addView(input);
        box.addView(button("从剪贴板粘贴",false,()->{
            ClipboardManager cm=getSystemService(ClipboardManager.class);
            ClipData clip=cm.getPrimaryClip();
            if(clip!=null&&clip.getItemCount()>0){CharSequence text=clip.getItemAt(0).coerceToText(this);if(text!=null)input.setText(text);}else toast("剪贴板里没有文字。");
        }));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("同步村庄").setView(box).setNegativeButton("取消",null)
            .setNeutralButton("选择文件",(d,w)->chooseFile()).setPositiveButton("读取数据",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{String text=input.getText().toString();if(text.trim().isEmpty()){input.setError("请先粘贴导出数据");return;}dialog.dismiss();parse(text);}));dialog.show();
    }
    private void chooseFile() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try{startActivityForResult(i,71);}catch(ActivityNotFoundException e){toast("未找到文件选择器，请使用粘贴导入。");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==71&&result==RESULT_OK&&data!=null&&data.getData()!=null)readUri(data.getData());}
    private void readUri(Uri uri) {
        if(busy)return;busy=true;toast("正在读取文件…");
        io.execute(()->{
            try(InputStream stream=getContentResolver().openInputStream(uri)) {
                if(stream==null)throw new IOException("无法打开文件");
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
                while((n=stream.read(buffer))!=-1){if(out.size()+n>Village.MAX_CHARS)throw new IOException("文件超过 2 MB，请选择游戏导出的 JSON / TXT。");out.write(buffer,0,n);}
                String raw=out.toString("UTF-8");Village parsed=Village.parse(raw,System.currentTimeMillis());
                runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed())preview(parsed);});
            }catch(Exception e){runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed())error("无法导入文件",e.getMessage()+"\n请选择纯文本 JSON / TXT，不能直接导入 Word 文件。");});}
        });
    }
    private void parse(String raw) {
        if(busy)return;busy=true;
        io.execute(()->{try{Village parsed=Village.parse(raw,System.currentTimeMillis());runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed())preview(parsed);});}
        catch(Exception e){runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed())error("数据未能读取",e.getMessage());});}});
    }
    private void preview(Village v) {
        State existing=new State(this);
        if(existing.village!=null&&existing.village.tag.equals(v.tag)) {
            if(v.timestamp<existing.village.timestamp){error("这是较早的导出","已保留更新的数据。请在游戏中重新导出。旧数据不会覆盖当前提醒。");return;}
            if(v.timestamp==existing.village.timestamp){toast("这份数据已导入，无需重复同步。");return;}
        }
        int active=0;for(Village.Upgrade u:v.upgrades)if(u.endMillis>System.currentTimeMillis())active++;
        String message="村庄 "+v.tag+"\n导出于 "+format(v.timestamp*1000L)+"\n\n识别到 "+v.upgrades.size()+" 项升级，"+active+" 项尚未到预计完成时间。";
        if(existing.village!=null)message+="\n\n本次导入将替换当前村庄的任务和提醒，单项提醒开关会重置。";
        for(String warning:v.warnings)message+="\n\n"+warning;
        new AlertDialog.Builder(this).setTitle("确认同步内容").setMessage(message).setNegativeButton("取消",null).setPositiveButton("同步",(d,w)->{
            State latest=new State(this);latest.replace(v);
            if(!latest.save()){error("保存失败","本机存储写入失败，请检查空间后重试。");return;}
            getSystemService(NotificationManager.class).cancel(Reminders.NOTIFICATION_ID);
            String result=Reminders.schedule(this);page=0;render();toast(result.isEmpty()?"村庄已同步":result);
        }).show();
    }
    private void open(Intent i){try{startActivity(i);}catch(ActivityNotFoundException e){toast("请在系统设置中手动打开对应权限。");}}
    private void error(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message==null?"请检查数据是否完整。":message).setPositiveButton("知道了",null).show();}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?BG:GOLD);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(16),dp(10),dp(16),dp(10));b.setBackground(shape(primary?GOLD:PANEL,12));b.setOnClickListener(v->action.run());return b;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable gradient(){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff284738,0xff1c3432});d.setCornerRadius(dp(22));return d;}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(shape(PANEL,18));return c;}
    private void gap(LinearLayout parent,int height){View gap=new View(this);parent.addView(gap,new LinearLayout.LayoutParams(1,dp(height)));}
    private void notice(String value){LinearLayout n=card();n.addView(text(value,13,MUTED,false));content.addView(n);gap(content,10);}
    private void step(String number,String title,String detail){LinearLayout c=card();c.addView(text(number+"  /  "+title,17,GOLD,true));gap(c,8);c.addView(text(detail,14,MUTED,false));content.addView(c);gap(content,12);}
    private static String format(long millis){return new SimpleDateFormat("MM月dd日 HH:mm",Locale.CHINA).format(new Date(millis));}
    private static String remaining(long end){long seconds=Math.max(0,(end-System.currentTimeMillis()+999)/1000);if(seconds==0)return "预计已完成";long days=seconds/86400,h=seconds/3600%24,m=seconds/60%60,s=seconds%60;if(days>0)return days+"天 "+h+"小时 "+m+"分";if(h>0)return h+"小时 "+m+"分 "+s+"秒";return m+"分 "+s+"秒";}
}
