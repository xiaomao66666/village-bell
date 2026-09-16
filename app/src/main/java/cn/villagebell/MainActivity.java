package cn.villagebell;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
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
    private static final int BG=0xfff5f1e7, PANEL=0xfffffcf5, GOLD=0xffa76b25, TEXT=0xff303b30, MUTED=0xff6f7564, GREEN=0xff53764e;
    private LinearLayout root, content;
    private State state;
    private int page=0;
    private String filter="全部";
    private int listMode=0;
    private Village demo;
    private ScrollView scroll;
    private View taskAnchor;
    private long nextMilestone=Long.MAX_VALUE;
    private final java.util.List<ProgressTrack> progressViews=new ArrayList<>();
    private boolean foreground, busy;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Map<TextView,Village.Upgrade> countdowns=new HashMap<>();
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            if(!foreground)return;
            if(System.currentTimeMillis()>=nextMilestone){int y=scroll==null?0:scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));}
            for(Map.Entry<TextView,Village.Upgrade> e:countdowns.entrySet())e.getKey().setText(remaining(e.getValue().endMillis));
            for(ProgressTrack track:progressViews)track.invalidate();
            handler.postDelayed(this,1000);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); Reminders.channels(this);
        if(saved!=null) { page=saved.getInt("page"); filter=saved.getString("filter","全部");listMode=saved.getInt("listMode");if(saved.getBoolean("demo"))demo=createDemo(); }
        render(); handleIntent(getIntent());
    }
    @Override protected void onResume() { super.onResume(); foreground=true; String problem=Reminders.schedule(this); render(); handler.removeCallbacks(ticker); handler.post(ticker); if(!problem.isEmpty()) toast(problem); }
    @Override protected void onPause() { foreground=false; handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
    @Override public void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putInt("page",page); out.putString("filter",filter);out.putInt("listMode",listMode);out.putBoolean("demo",demo!=null); }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleIntent(i); }
    private void handleIntent(Intent i) {
        if(!Intent.ACTION_SEND.equals(i.getAction()))return;
        String text=i.getStringExtra(Intent.EXTRA_TEXT);
        Uri uri=i.getParcelableExtra(Intent.EXTRA_STREAM);
        i.setAction(null);
        if(text!=null) parse(text); else if(uri!=null) readUri(uri);
    }
    private void render() {
        state=new State(this); countdowns.clear();progressViews.clear();nextMilestone=Long.MAX_VALUE;
        Village shown=demo!=null?demo:state.village;if(shown!=null)for(Village.Upgrade u:shown.upgrades)if(u.endMillis>System.currentTimeMillis())nextMilestone=Math.min(nextMilestone,u.endMillis);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        setContentView(root); root.requestApplyInsets();
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(20),dp(10),dp(20),dp(12));
        GameArt emblem=new GameArt(this,GameArt.BELL);header.addView(emblem,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout titles=column();titles.setPadding(dp(10),0,0,0);titles.addView(text("村庄铃铛",23,TEXT,true));titles.addView(text("VILLAGE BELL",10,MUTED,false));header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=text("离线助手",11,GREEN,true);badge.setPadding(dp(9),dp(5),dp(9),dp(5));badge.setBackground(shape(0xffe5eddc,8));header.addView(badge);root.addView(header);
        scroll=new ScrollView(this); scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=column(); content.setPadding(dp(18),dp(2),dp(18),dp(24)); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page==0) dashboard(); else if(page==1) settingsPage(); else helpPage();
        LinearLayout nav=new LinearLayout(this); nav.setPadding(dp(12),dp(6),dp(12),dp(6)); nav.setBackgroundColor(PANEL);nav.setElevation(dp(8));
        String[] tabs={"村庄","提醒","指南"};int[] icons={GameArt.BUILDING,GameArt.BELL,GameArt.BOOK};
        for(int n=0;n<tabs.length;n++) { final int target=n;LinearLayout b=column();b.setGravity(Gravity.CENTER);b.setPadding(0,dp(4),0,dp(4));b.setBackground(shape(n==page?0xffe7eddf:PANEL,14));
            b.addView(new GameArt(this,icons[n]),new LinearLayout.LayoutParams(dp(30),dp(30)));TextView navLabel=text(tabs[n],11,n==page?GREEN:MUTED,n==page);navLabel.setGravity(Gravity.CENTER);b.addView(navLabel);b.setContentDescription(tabs[n]);b.setFocusable(true);b.setOnClickListener(view->{page=target;render();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(60),1)); }
        root.addView(nav);
    }
    private void dashboard() {
        if(state.error!=null) notice(state.error);
        Village v=demo!=null?demo:state.village;
        if(v==null) {
            LinearLayout welcome=card();welcome.setBackground(gradient());
            welcome.addView(text("欢迎回到村庄",12,0xffd6e4c5,false));gap(welcome,10);
            welcome.addView(text("每一场等待\n都有归来的时刻。",28,0xfffff6dc,true));gap(welcome,12);
            welcome.addView(text("建筑、英雄与研究进度，一眼尽收。",14,0xffd6e4c5,false));content.addView(welcome);
            GameArt art=new GameArt(this,GameArt.VILLAGE);art.setBackground(shape(0xffe2ebd1,20));art.setClipToOutline(true);content.addView(art,new LinearLayout.LayoutParams(-1,dp(205)));gap(content,18);
            content.addView(button("＋  导入我的村庄",true,this::importDialog));gap(content,10);
            content.addView(button("先逛逛演示村庄",false,()->{demo=createDemo();filter="全部";render();}));gap(content,18);
            step("01","游戏里复制","设置 → 更多设置 → 导出村庄数据");
            step("02","铃铛里粘贴","升级任务会自动整理成村庄面板与完成时间轴。");
            return;
        }
        long now=System.currentTimeMillis();int active=Dashboard.active(v,"全部",now);Village.Upgrade next=null;
        for(Village.Upgrade u:v.upgrades)if(u.endMillis>now){next=u;break;}
        if(demo!=null){LinearLayout demoRow=new LinearLayout(this);demoRow.setGravity(Gravity.CENTER_VERTICAL);TextView label=text("演示村庄 · 不参与升级提醒",12,GOLD,true);demoRow.addView(label,new LinearLayout.LayoutParams(0,-2,1));demoRow.addView(button("退出",false,()->{demo=null;render();}));content.addView(demoRow);gap(content,8);}
        LinearLayout villageHeader=new LinearLayout(this);villageHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout who=column();who.addView(text("我的村庄",26,TEXT,true));who.addView(text(v.tag+"  ·  "+format(v.timestamp*1000L)+" 同步",11,MUTED,false));villageHeader.addView(who,new LinearLayout.LayoutParams(0,-2,1));
        villageHeader.addView(button("↻ 同步",false,this::importDialog));content.addView(villageHeader);gap(content,14);
        LinearLayout scene=column();scene.setBackground(shape(0xffe5edd7,20));scene.setClipToOutline(true);
        scene.addView(new GameArt(this,GameArt.VILLAGE),new LinearLayout.LayoutParams(-1,dp(180)));
        LinearLayout stats=new LinearLayout(this);stats.setPadding(dp(12),0,dp(12),dp(12));
        metric(stats,String.valueOf(active),"升级进行中");metric(stats,String.valueOf(Dashboard.completed(v,now)),"预计已完成");metric(stats,v.timingUncertain?"待校正":"本机计时","同步快照状态");scene.addView(stats);content.addView(scene);gap(content,14);
        LinearLayout hero=card();hero.setBackground(gradient());
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);LinearLayout copy=column();copy.addView(text("下一项归来  /  NEXT UP",10,0xffd9e6cc,true));gap(copy,7);
        if(next!=null) {
            copy.addView(text(next.name,22,0xfffff8e7,true));TextView time=text(remaining(next.endMillis),20,0xfff1d795,true);countdowns.put(time,next);copy.addView(time);gap(copy,7);
            copy.addView(text(format(next.endMillis)+" 预计完成",12,0xffd9e6cc,false));
        }else{copy.addView(text("这一轮等待结束了",23,0xfffff8e7,true));gap(copy,8);copy.addView(text("回游戏安排下一项，再同步回来。",13,0xffd9e6cc,false));}
        top.addView(copy,new LinearLayout.LayoutParams(0,-2,1));top.addView(new GameArt(this,next==null?GameArt.CROWN:artKind(next)),new LinearLayout.LayoutParams(dp(70),dp(70)));hero.addView(top);content.addView(hero);gap(content,20);
        section("升级分区","点击分区筛选任务");
        String[] groups={"建筑与英雄","研究","战宠","夜世界"};int[] kinds={GameArt.BUILDING,GameArt.RESEARCH,GameArt.PET,GameArt.NIGHT};
        for(int i=0;i<2;i++){LinearLayout row=new LinearLayout(this);for(int j=0;j<2;j++){int k=i*2+j;String g=groups[k];LinearLayout tile=card();tile.setPadding(dp(12),dp(10),dp(12),dp(10));tile.setBackground(outline(g.equals(filter)?0xffe8efdf:PANEL,g.equals(filter)?GREEN:0xffe4dece,16));
            LinearLayout inside=new LinearLayout(this);inside.setGravity(Gravity.CENTER_VERTICAL);inside.addView(new GameArt(this,kinds[k]),new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout info=column();info.setPadding(dp(7),0,0,0);info.addView(text(g,12,MUTED,false));info.addView(text(Dashboard.active(v,g,now)+" 项",20,groupColor(g),true));inside.addView(info);tile.addView(inside);tile.setContentDescription(g+"，"+Dashboard.active(v,g,now)+"项进行中，点击筛选");tile.setFocusable(true);tile.setOnClickListener(view->{filter=g;render();scroll.post(()->scroll.smoothScrollTo(0,taskAnchor.getTop()));});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(j==0)lp.rightMargin=dp(10);row.addView(tile,lp);}content.addView(row);gap(content,10);}
        gap(content,8);weeklyChart(v,now);gap(content,18);
        if(demo==null){
            if(!Reminders.allowed(this))notice("提醒未开启 · 前往「提醒」页允许通知。");
            else if(!state.enabled)notice("提醒已暂停 · 可在「提醒」页恢复。");
            else if(!Reminders.exact(this))notice("普通提醒可能延迟 · 可在「提醒」页开启准时提醒。");
            for(String warning:v.warnings)notice(warning);
        }
        taskAnchor=new View(this);content.addView(taskAnchor,new LinearLayout.LayoutParams(1,1));
        section("升级日志",v.timingUncertain?"基础预计时间 · 未校正加速":"按预计完成时间排序");
        LinearLayout modes=new LinearLayout(this);modes.setBackground(shape(0xffe9e4d7,12));modes.setPadding(dp(4),dp(4),dp(4),dp(4));
        for(int i=0;i<2;i++){final int mode=i;Button b=button(i==0?"任务卡片":"完成时间轴",false,()->{int y=scroll.getScrollY();listMode=mode;render();scroll.post(()->scroll.scrollTo(0,y));});b.setTextColor(listMode==i?GREEN:MUTED);b.setBackground(shape(listMode==i?PANEL:0xffe9e4d7,10));modes.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));}content.addView(modes);gap(content,10);
        HorizontalScrollView filters=new HorizontalScrollView(this); filters.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this);
        for(String f:new String[]{"全部","建筑与英雄","研究","战宠","夜世界"}) {
            Button b=button(f,f.equals(filter),()->{int y=scroll.getScrollY();filter=f;render();scroll.post(()->scroll.scrollTo(0,y));});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(44));lp.rightMargin=dp(6);chips.addView(b,lp); }
        filters.addView(chips); content.addView(filters); gap(content,10);
        int count=0;String previousDay="";
        java.util.List<Village.Upgrade> ordered=new ArrayList<>();for(Village.Upgrade u:v.upgrades)if(u.endMillis>now)ordered.add(u);for(Village.Upgrade u:v.upgrades)if(u.endMillis<=now)ordered.add(u);
        for(Village.Upgrade u:ordered)if(filter.equals("全部")||filter.equals(u.group())){
            if(listMode==1){String day=u.endMillis<=now?"预计已完成":new SimpleDateFormat("MM月dd日 EEEE",Locale.CHINA).format(new Date(u.endMillis));if(!day.equals(previousDay)){gap(content,10);content.addView(text("●  "+day,15,GREEN,true));gap(content,8);previousDay=day;}timelineCard(u,now);}else upgradeCard(u,now);count++;
        }
        if(count==0) notice("这份数据中没有该类别的升级任务。");
        gap(content,8);content.addView(text("插画为示意村庄，不代表实际阵型。进度条以同步时的剩余等待为起点；实际完成状态请在游戏中确认。",11,MUTED,false));
    }
    private void upgradeCard(Village.Upgrade u,long now) {
        LinearLayout box=card();
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        GameArt icon=new GameArt(this,artKind(u));icon.setBackground(shape(tint(u.group()),13));LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams(dp(55),dp(55));iconLp.rightMargin=dp(12);row.addView(icon,iconLp);
        LinearLayout label=column(); label.addView(text(u.name,17,TEXT,true)); label.addView(text(u.group()+"  ·  Lv."+u.level,11,MUTED,false));
        row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Switch sw=new Switch(this); sw.setContentDescription(u.name+"的完成提醒");
        Village displayed=demo!=null?demo:state.village;
        sw.setChecked(demo==null&&!state.muted.contains(u.key));sw.setThumbTintList(ColorStateList.valueOf(PANEL));sw.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{GREEN,0xffccc7b9}));
        sw.setEnabled(demo==null&&u.endMillis>now&&!displayed.timingUncertain);
        sw.setOnCheckedChangeListener((b,checked)->{State fresh=new State(this); if(checked)fresh.muted.remove(u.key);else fresh.muted.add(u.key); if(!fresh.save())toast("保存失败，请重试。"); String result=Reminders.schedule(this); if(!result.isEmpty())toast(result);});
        row.addView(sw);box.addView(row);gap(box,14);
        TextView remaining=text(remaining(u.endMillis),21,u.endMillis>now?groupColor(u.group()):GREEN,true);countdowns.put(remaining,u);box.addView(remaining);gap(box,10);
        ProgressTrack track=new ProgressTrack(this,u,groupColor(u.group()));progressViews.add(track);box.addView(track,new LinearLayout.LayoutParams(-1,dp(7)));gap(box,8);
        box.addView(text("同步后等待进度  ·  "+format(u.endMillis)+" 预计完成",10,MUTED,false));content.addView(box);gap(content,10);
    }
    private void settingsPage() {
        illustratedHeading("让铃铛准时响起","准备好提醒，再安心离开村庄。",GameArt.BELL);
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
        illustratedHeading("村庄冒险手册","每次同步，都为下一次上线做好准备。",GameArt.BOOK);
        step("01","升级后同步一次","游戏 → 设置 → 更多设置 → 导出村庄数据，回到铃铛粘贴导入。无需逐项输入时间。");
        step("02","一份数据，一个村庄","第一版管理一个村庄。重新导入会替换旧任务与提醒；更换村庄前会显示预览。");
        step("03","使用加速后再同步","本应用不会连接游戏服务器。导出之后发生的变化，只有再次导入才能知道。");
        step("04","遇到特殊加速","包含活动加速或自动助手的快照暂不安排到点提醒；加速结束后重新导出。冷却时间不会被误认为加速。");
        LinearLayout privacy=card();privacy.addView(text("你的村庄，留在你的手机",18,TEXT,true));gap(privacy,10);
        privacy.addView(text("不需要游戏密码，不上传数据，没有网络权限。不使用后台剪贴板监听。仅在你点击粘贴、选择文件或分享时读取数据。",14,MUTED,false));
        gap(privacy,16);privacy.addView(text("村庄铃铛 0.2.0 · 非官方工具\n时间为预测值，请以游戏内实际状态为准。",12,MUTED,false));content.addView(privacy);gap(content,12);
        if(state.village!=null) content.addView(button("清除本机村庄数据",false,()->new AlertDialog.Builder(this).setTitle("清除村庄数据？")
            .setMessage("删除本机保存的导出数据，并取消全部升级提醒。")
            .setNegativeButton("保留",null).setPositiveButton("清除",(d,w)->{
                Reminders.cancel(this);State s=new State(this);s.village=null;s.muted.clear();s.delivered.clear();s.save();
                getSystemService(NotificationManager.class).cancel(Reminders.NOTIFICATION_ID);demo=null;page=0;render();
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
            String result=Reminders.schedule(this);demo=null;page=0;render();toast(result.isEmpty()?"村庄已同步":result);
        }).show();
    }
    private void illustratedHeading(String title,String subtitle,int kind){
        LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(outline(0xffe9eddc,0xffdbe2ce,20));
        LinearLayout copy=column();copy.addView(text(title,24,TEXT,true));gap(copy,7);copy.addView(text(subtitle,13,MUTED,false));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));row.addView(new GameArt(this,kind),new LinearLayout.LayoutParams(dp(64),dp(74)));content.addView(row);gap(content,18);
    }
    private void metric(LinearLayout row,String value,String title){LinearLayout c=column();c.setGravity(Gravity.CENTER);TextView number=text(value,21,TEXT,true);number.setGravity(Gravity.CENTER);c.addView(number);TextView caption=text(title,10,MUTED,false);caption.setGravity(Gravity.CENTER);c.addView(caption);row.addView(c,new LinearLayout.LayoutParams(0,-2,1));}
    private void section(String title,String sub){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(text(title,18,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));row.addView(text(sub,10,MUTED,false));content.addView(row);gap(content,10);}
    private void weeklyChart(Village v,long now){
        LinearLayout chart=card();chart.addView(text("未来七日 · 完成节奏",17,TEXT,true));gap(chart,4);chart.addView(text("每日预计完成的升级数量",11,MUTED,false));gap(chart,14);
        int[] buckets=Dashboard.nextSevenDays(v,now,TimeZone.getDefault());int max=1;for(int i=0;i<7;i++)max=Math.max(max,buckets[i]);
        LinearLayout bars=new LinearLayout(this);bars.setGravity(Gravity.BOTTOM);Calendar date=Calendar.getInstance();date.setTimeInMillis(now);
        for(int i=0;i<7;i++){LinearLayout col=column();col.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);col.setContentDescription((i==0?"今天":(date.get(Calendar.MONTH)+1)+"月"+date.get(Calendar.DAY_OF_MONTH)+"日")+"预计完成"+buckets[i]+"项");
            TextView count=text(String.valueOf(buckets[i]),12,buckets[i]>0?GREEN:MUTED,true);count.setGravity(Gravity.CENTER);col.addView(count);gap(col,5);
            FrameLayout track=new FrameLayout(this);track.setBackground(shape(0xffefebdf,6));LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(dp(19),dp(56));col.addView(track,tlp);
            View bar=new View(this);bar.setBackground(shape(i==0?0xffcaa45d:0xff86a374,6));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(buckets[i]==0?0:Math.max(5,56*buckets[i]/max)),Gravity.BOTTOM);track.addView(bar,bp);gap(col,7);
            TextView dayLabel=text(i==0?"今天":(date.get(Calendar.MONTH)+1)+"/"+date.get(Calendar.DAY_OF_MONTH),10,MUTED,false);dayLabel.setGravity(Gravity.CENTER);col.addView(dayLabel);bars.addView(col,new LinearLayout.LayoutParams(0,-2,1));date.add(Calendar.DATE,1);}
        chart.addView(bars);if(buckets[7]>0){gap(chart,10);chart.addView(text("另有 "+buckets[7]+" 项将在七日后完成",11,MUTED,false));}content.addView(chart);
    }
    private void timelineCard(Village.Upgrade u,long now){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView time=text(new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(u.endMillis)),13,MUTED,true);row.addView(time,new LinearLayout.LayoutParams(dp(52),-2));
        LinearLayout card=card();card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));
        card.addView(new GameArt(this,artKind(u)),new LinearLayout.LayoutParams(dp(42),dp(42)));LinearLayout label=column();label.setPadding(dp(8),0,0,0);label.addView(text(u.name,15,TEXT,true));TextView countdown=text(remaining(u.endMillis),12,groupColor(u.group()),false);countdowns.put(countdown,u);label.addView(countdown);card.addView(label,new LinearLayout.LayoutParams(0,-2,1));row.addView(card,new LinearLayout.LayoutParams(0,-2,1));
        card.setOnClickListener(view->new AlertDialog.Builder(this).setTitle(u.name).setMessage(u.group()+" · 导出等级 "+u.level+"\n"+format(u.endMillis)+" 预计完成\n\n切换到任务卡片，可以设置单项提醒。").setPositiveButton("知道了",null).show());content.addView(row);gap(content,8);
    }
    private int artKind(Village.Upgrade u){if(u.dataId==1000008||u.dataId==1000041||u.dataId==1000057)return GameArt.CANNON;if(u.category.startsWith("units")||u.category.equals("siege_machines"))return GameArt.TROOP;if(u.dataId==1000007||u.dataId==1000046)return GameArt.RESEARCH;if(u.dataId==1000068)return GameArt.PET;if(u.category.startsWith("heroes")||u.category.equals("guardians"))return GameArt.CROWN;switch(u.group()){case "研究":return GameArt.RESEARCH;case "战宠":return GameArt.PET;case "夜世界":return GameArt.NIGHT;default:return GameArt.BUILDING;}}
    private int groupColor(String g){switch(g){case "研究":return 0xff806b9d;case "战宠":return 0xff64835c;case "夜世界":return 0xff587798;default:return GOLD;}}
    private int tint(String g){switch(g){case "研究":return 0xffefe6f4;case "战宠":return 0xffe8efde;case "夜世界":return 0xffe6edf3;default:return 0xfff4ead4;}}
    private Village createDemo(){
        try{long stamp=System.currentTimeMillis()/1000-7200;
            String raw="{\"tag\":\"#DEMO123\",\"timestamp\":"+stamp+",\"buildings\":[{\"data\":1000008,\"lvl\":20,\"timer\":11700},{\"data\":1000011,\"lvl\":13,\"timer\":93600},{\"data\":1000000,\"lvl\":11,\"timer\":350000}],\"units\":[{\"data\":4000011,\"lvl\":5,\"timer\":37800}],\"heroes\":[{\"data\":28000000,\"lvl\":75,\"timer\":180000}],\"spells\":[],\"pets\":[{\"data\":73000009,\"lvl\":3,\"timer\":9900}],\"buildings2\":[{\"data\":1000041,\"lvl\":6,\"timer\":25200},{\"data\":1000057,\"lvl\":9,\"timer\":266400}],\"units2\":[],\"heroes2\":[],\"boosts\":{}}";
            return Village.parse(raw,System.currentTimeMillis());
        }catch(Exception e){throw new IllegalStateException("Invalid bundled demo",e);}
    }
    private void open(Intent i){try{startActivity(i);}catch(ActivityNotFoundException e){toast("请在系统设置中手动打开对应权限。");}}
    private void error(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message==null?"请检查数据是否完整。":message).setPositiveButton("知道了",null).show();}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary?0xfffff8e7:GREEN);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setBackground(outline(primary?GREEN:0xffeaeede,primary?0xff466643:0xffdce3cf,12));b.setStateListAnimator(null);b.setOnClickListener(v->action.run());return b;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable outline(int color,int stroke,int radius){GradientDrawable d=shape(color,radius);d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff4b6847,0xff304f3d});d.setCornerRadius(dp(20));return d;}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(17),dp(17),dp(17),dp(17));c.setBackground(outline(PANEL,0xffe5dfd0,18));return c;}
    private void gap(LinearLayout parent,int height){View gap=new View(this);parent.addView(gap,new LinearLayout.LayoutParams(1,dp(height)));}
    private void notice(String value){LinearLayout n=card();n.addView(text(value,13,MUTED,false));content.addView(n);gap(content,10);}
    private void step(String number,String title,String detail){LinearLayout c=card();c.addView(text(number+"  /  "+title,17,GOLD,true));gap(c,8);c.addView(text(detail,14,MUTED,false));content.addView(c);gap(content,12);}
    private static String format(long millis){return new SimpleDateFormat("MM月dd日 HH:mm",Locale.CHINA).format(new Date(millis));}
    private static String remaining(long end){long seconds=Math.max(0,(end-System.currentTimeMillis()+999)/1000);if(seconds==0)return "预计已完成";long days=seconds/86400,h=seconds/3600%24,m=seconds/60%60,s=seconds%60;if(days>0)return days+"天 "+h+"小时 "+m+"分";if(h>0)return h+"小时 "+m+"分 "+s+"秒";return m+"分 "+s+"秒";}
}
