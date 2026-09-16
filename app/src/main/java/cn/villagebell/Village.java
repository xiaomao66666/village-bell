package cn.villagebell;

import org.json.*;
import java.util.*;

/** Export parsing is independent of Android so it can be tested on the JVM. */
public final class Village {
    public static final int MAX_CHARS = 2_000_000;
    private static final String[] CATEGORIES = {"buildings", "traps", "heroes", "guardians", "units", "spells", "siege_machines", "pets", "buildings2", "traps2", "heroes2", "units2"};
    public final String tag, raw;
    public final long timestamp;
    public final List<Upgrade> upgrades;
    public final List<String> warnings;
    public final boolean timingUncertain;

    private Village(String tag, String raw, long timestamp, List<Upgrade> items, List<String> warnings, boolean uncertain) {
        this.tag=tag; this.raw=raw; this.timestamp=timestamp;
        this.upgrades=Collections.unmodifiableList(items);
        this.warnings=Collections.unmodifiableList(warnings); this.timingUncertain=uncertain;
    }
    public String revision() { return tag+":"+timestamp; }
    public static final class Upgrade {
        public final String key, name, category;
        public final long dataId, endMillis, seconds;
        public final int level;
        Upgrade(String key, long id, int level, long seconds, long exported, String category) {
            this.key=key; this.dataId=id; this.level=level; this.seconds=seconds;
            this.endMillis=(exported+seconds)*1000L; this.category=category;
            this.name=NAMES.containsKey(id)?NAMES.get(id):"未知项目 · "+id;
        }
        public boolean builderBase() { return category.endsWith("2"); }
        public String group() {
            if(builderBase()) return "夜世界";
            if(category.equals("pets")) return "战宠";
            if(category.equals("units")||category.equals("spells")||category.equals("siege_machines")) return "研究";
            return "建筑与英雄";
        }
    }

    public static Village parse(String input, long nowMillis) throws JSONException {
        if(input==null || input.length()>MAX_CHARS) throw new JSONException("文件过大或没有内容，请导入游戏导出的 JSON。");
        String raw=input.trim(); if(raw.startsWith("\uFEFF")) raw=raw.substring(1).trim();
        if(!raw.startsWith("{")||!raw.endsWith("}")) throw new JSONException("数据不完整：请重新导出，或选择完整的 .json / .txt 文件。");
        JSONTokener tokenizer=new JSONTokener(raw);
        JSONObject root=new JSONObject(tokenizer);
        if(tokenizer.nextClean()!=0) throw new JSONException("JSON 末尾有额外内容。");
        Object tagValue=root.opt("tag");
        if(!(tagValue instanceof String)||!((String)tagValue).matches("#[A-Za-z0-9]{3,30}")) throw new JSONException("缺少有效的村庄标签 tag。");
        long timestamp=integer(root,"timestamp",1_500_000_000L,4_102_444_800L);
        if(timestamp*1000L>nowMillis+300_000L) throw new JSONException("导出时间在未来，请检查手机时间是否准确。");
        // A village export contains these even when empty. Reject partial snippets.
        for(String required:new String[]{"buildings","units","heroes","spells","pets"})
            if(!(root.opt(required) instanceof JSONArray)) throw new JSONException("缺少 "+required+" 列表，请使用完整村庄导出。");
        List<Upgrade> items=new ArrayList<>();
        List<String> warnings=new ArrayList<>();
        boolean[] uncertain={false};
        for(String category:CATEGORIES) {
            if(!root.has(category)) continue;
            JSONArray arr=root.getJSONArray(category);
            for(int i=0;i<arr.length();i++) visit(arr.getJSONObject(i),category+"/"+i,category,timestamp,items,uncertain,0);
        }
        JSONObject boosts=root.optJSONObject("boosts");
        if(boosts!=null) {
            Iterator<String> keys=boosts.keys();
            while(keys.hasNext()) {
                String key=keys.next();
                // Cooldowns do not accelerate upgrades. All other positive effects need calibration.
                if(!key.endsWith("_cooldown") && boosts.optDouble(key,0)>0) uncertain[0]=true;
            }
        }
        JSONArray helpers=root.optJSONArray("helpers");
        if(helpers!=null && helpers.length()>0)
            warnings.add("国服助手映射尚在验证；如之后使用助手或药水，请重新导出同步。");
        if(uncertain[0]) warnings.add("检测到加速或自动助手：当前显示未校正的基础预计时间，本次数据暂停到点提醒。请在加速结束后重新导出。");
        if(nowMillis-timestamp*1000L>86_400_000L) warnings.add("这是超过 24 小时的快照，请重新导出以更新升级状态。");
        boolean unknown=false; for(Upgrade u:items) if(!NAMES.containsKey(u.dataId)) unknown=true;
        if(unknown) warnings.add("部分国服项目名称尚未收录，已保留 ID 和计时，不会漏掉这些升级。");
        items.sort(Comparator.comparingLong(u->u.endMillis));
        return new Village((String)tagValue,raw,timestamp,items,warnings,uncertain[0]);
    }
    private static void visit(JSONObject obj,String path,String category,long timestamp,List<Upgrade> items,boolean[] uncertain,int depth) throws JSONException {
        if(depth>12) throw new JSONException("数据嵌套过深。");
        if(obj.optDouble("helper_timer",0)>0 || obj.optBoolean("helper_recurrent",false)) uncertain[0]=true;
        if(obj.has("timer")) {
            long seconds=integer(obj,"timer",0,315_360_000L);
            long id=integer(obj,"data",1,Integer.MAX_VALUE);
            int lvl=(int)integer(obj,"lvl",0,10000);
            if(seconds>0) items.add(new Upgrade(path,id,lvl,seconds,timestamp,category));
            if(items.size()>1000) throw new JSONException("升级项目过多，请检查数据。");
        }
        for(String key:new String[]{"types","modules"}) {
            if(!obj.has(key)) continue;
            JSONArray arr=obj.getJSONArray(key);
            for(int i=0;i<arr.length();i++) visit(arr.getJSONObject(i),path+"/"+key+"/"+i,category,timestamp,items,uncertain,depth+1);
        }
    }
    private static long integer(JSONObject obj,String key,long min,long max) throws JSONException {
        Object value=obj.opt(key);
        if(!(value instanceof Number)) throw new JSONException("字段 "+key+" 必须是整数。");
        double number=((Number)value).doubleValue();
        if(!Double.isFinite(number)||number!=Math.rint(number)||number<min||number>max) throw new JSONException("字段 "+key+" 超出有效范围。");
        return ((Number)value).longValue();
    }
    // Hand-maintained display labels; unknown IDs always retain their timers.
    private static final Map<Long,String> NAMES=new HashMap<>();
    static {
        name(1000000,"兵营"); name(1000001,"大本营"); name(1000002,"圣水收集器"); name(1000003,"圣水瓶");
        name(1000004,"金矿"); name(1000005,"储金罐"); name(1000006,"训练营"); name(1000007,"实验室");
        name(1000008,"加农炮"); name(1000009,"箭塔"); name(1000010,"城墙"); name(1000011,"法师塔");
        name(1000012,"防空火箭"); name(1000013,"迫击炮"); name(1000014,"部落城堡"); name(1000015,"建筑工人小屋");
        name(1000019,"特斯拉电磁塔"); name(1000020,"法术工厂"); name(1000021,"X连弩"); name(1000023,"暗黑重油钻井");
        name(1000024,"暗黑重油罐"); name(1000026,"暗黑训练营"); name(1000027,"地狱之塔"); name(1000028,"空气炮");
        name(1000029,"暗黑法术工厂"); name(1000031,"天鹰火炮"); name(1000032,"炸弹塔");
        name(1000034,"建筑大师大本营"); name(1000035,"圣水收集器"); name(1000036,"圣水瓶"); name(1000037,"金矿");
        name(1000038,"储金罐"); name(1000039,"时光钟楼"); name(1000040,"训练营"); name(1000041,"双管加农炮");
        name(1000042,"兵营"); name(1000043,"特斯拉电磁塔"); name(1000044,"加农炮"); name(1000045,"多管迫击炮");
        name(1000046,"星空实验室"); name(1000048,"箭塔"); name(1000050,"防空火炮"); name(1000051,"守卫岗哨");
        name(1000052,"超级特斯拉电磁塔"); name(1000054,"防空炸弹发射器"); name(1000055,"巨石强森");
        name(1000056,"熔岩火炮"); name(1000057,"巨型加农炮"); name(1000058,"宝石矿井"); name(1000059,"攻城机器工坊");
        name(1000063,"熔岩发射器"); name(1000064,"B.O.B小屋"); name(1000065,"B.O.B控制室"); name(1000067,"投石炮");
        name(1000068,"战宠小屋"); name(1000070,"铁匠铺"); name(1000071,"英雄殿堂"); name(1000072,"法术塔");
        name(1000077,"巨石碑"); name(1000078,"奥仔哨站"); name(1000082,"治疗小屋"); name(1000093,"助手小屋");
        name(28000000,"野蛮人之王"); name(28000001,"弓箭女皇"); name(28000002,"大守护者");
        name(28000003,"战争机器"); name(28000004,"飞盾战神"); name(28000005,"战斗直升机"); name(28000006,"亡灵王子");
        name(4000000,"野蛮人"); name(4000001,"弓箭手"); name(4000002,"哥布林"); name(4000003,"巨人");
        name(4000004,"炸弹人"); name(4000005,"气球兵"); name(4000006,"法师"); name(4000007,"天使");
        name(4000008,"飞龙"); name(4000009,"皮卡超人"); name(4000010,"亡灵"); name(4000011,"野猪骑士");
        name(4000012,"瓦基丽武神"); name(4000013,"戈仑石人"); name(4000015,"女巫"); name(4000017,"熔岩猎犬");
        name(4000022,"巨石投手"); name(4000023,"飞龙宝宝"); name(4000024,"掘地矿工");
        name(4000031,"狂暴野蛮人"); name(4000032,"隐秘弓箭手"); name(4000033,"异变亡灵"); name(4000034,"巨人拳击手");
        name(4000035,"炸弹兵"); name(4000036,"超级皮卡"); name(4000037,"加农炮战车"); name(4000038,"骷髅气球");
        name(4000041,"飞龙宝宝"); name(4000042,"暗夜女巫"); name(4000070,"野猪飞骑");
        name(4000051,"攻城战车"); name(4000052,"攻城飞艇"); name(4000053,"大雪怪"); name(4000058,"戈仑冰人");
        name(4000059,"雷电飞龙"); name(4000062,"攻城气球"); name(4000065,"龙骑士"); name(4000075,"攻城训练营");
        name(4000082,"英雄猎手"); name(4000087,"攻城滚木车"); name(4000091,"烈焰战车"); name(4000095,"雷霆泰坦");
        name(4000097,"大守护者学徒"); name(4000123,"德鲁伊");
        name(26000000,"雷电法术"); name(26000001,"治疗法术"); name(26000002,"狂暴法术"); name(26000003,"弹跳法术");
        name(26000005,"冰冻法术"); name(26000009,"毒药法术"); name(26000010,"地震法术"); name(26000011,"急速法术");
        name(26000016,"镜像法术"); name(26000017,"骷髅法术"); name(26000028,"蝙蝠法术"); name(26000035,"隐形法术");
        name(26000053,"召回法术"); name(26000070,"蔓生法术");
        name(73000000,"莱希"); name(73000001,"大牦"); name(73000002,"闪枭"); name(73000003,"独角");
        name(73000004,"凤凰"); name(73000007,"猛蜥"); name(73000008,"挖挖"); name(73000009,"寒冰犬"); name(73000010,"灵狐");
        name(12000000,"炸弹"); name(12000001,"隐形弹簧"); name(12000002,"巨型炸弹"); name(12000005,"空中炸弹");
        name(12000006,"搜空地雷"); name(12000008,"骷髅陷阱"); name(12000016,"飓风陷阱");
    }
    private static void name(long id,String label) { NAMES.put(id,label); }
}
