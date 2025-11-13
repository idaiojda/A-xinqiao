package com.example.xinqiaobackend.consult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class ConsultController {

    @GetMapping("/api/consult/pro/list")
    public Map<String, Object> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "mode", required = false) String mode,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "city", required = false) String city
    ) {
        // 基础静态数据
        List<Map<String, Object>> all = baseConsultants();

        // 过滤城市（演示）：规范化后比较，避免“上海/上海市”等差异
        if (city != null && city.trim().length() > 0 && !"全部".equals(city)) {
            String normCity = normalizeCity(city);
            List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> it : all) {
                Object oc = it.get("city");
                if (oc != null) {
                    String itemCity = normalizeCity(oc.toString());
                    if (normCity.equals(itemCity)) {
                        filtered.add(it);
                    }
                }
            }
            all = filtered;
        }

        // 过滤困扰类型（演示）：按技能/标题关键词匹配；“全部”不筛选
        if (field != null && field.trim().length() > 0 && !"全部".equals(field)) {
            List<String> keys = keywordsForField(field);
            List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> it : all) {
                String title = String.valueOf(it.getOrDefault("title", ""));
                Object skillsObj = it.get("skills");
                boolean match = false;
                if (skillsObj instanceof List) {
                    List<?> skills = (List<?>) skillsObj;
                    outer:
                    for (String k : keys) {
                        for (Object s : skills) {
                            if (s != null && s.toString().contains(k)) { match = true; break outer; }
                        }
                    }
                }
                if (!match) {
                    for (String k : keys) { if (title.contains(k)) { match = true; break; } }
                }
                if (match) filtered.add(it);
            }
            all = filtered;
        }

        // 简单分页（演示）：截取 size 条
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(all.size(), from + size);
        List<Map<String, Object>> pageList = from >= all.size() ? new ArrayList<Map<String, Object>>() : all.subList(from, to);

        Map<String, Object> res = new HashMap<String, Object>();
        res.put("data", pageList);
        res.put("page", page);
        res.put("size", size);
        res.put("total", all.size());
        return res;
    }

    // 去后缀规范化城市名，提升匹配鲁棒性
    private String normalizeCity(String name) {
        String s = name.trim();
        if (s.endsWith("自治区")) s = s.substring(0, s.length() - 3);
        if (s.endsWith("特别行政区")) s = s.substring(0, s.length() - 5);
        if (s.endsWith("省")) s = s.substring(0, s.length() - 1);
        if (s.endsWith("市")) s = s.substring(0, s.length() - 1);
        return s;
    }

    // 困扰标签到关键词映射（与前端一致）
    private List<String> keywordsForField(String field) {
        switch (field) {
            case "焦虑缓解": return Arrays.asList("焦虑缓解", "焦虑");
            case "抑郁纾解": return Arrays.asList("抑郁");
            case "职场压力": return Arrays.asList("职场压力");
            case "亲子关系": return Arrays.asList("亲子教育");
            case "子女教育": return Arrays.asList("亲子教育");
            default: return Arrays.asList(field);
        }
    }

    @GetMapping("/api/consult/pro/cities")
    public List<String> cities() {
        List<Map<String, Object>> all = baseConsultants();
        List<String> out = new ArrayList<String>();
        for (Map<String, Object> it : all) {
            Object oc = it.get("city");
            if (oc != null) {
                String c = oc.toString();
                if (c.trim().length() > 0 && !out.contains(c)) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /**
     * 城市字典：分为“国内(含港澳台) / 海外 / 热门城市”，并提供省份分组与城市列表。
     * 结构示例：{"tabs": [{"label":"国内(含港澳台)", "groups":[{"label":"广东","cities":["广州","深圳", ...]}]}, {"label":"海外", "groups":[...]}, {"label":"热门城市", "cities":[...]}]}
     */
    @GetMapping("/api/consult/pro/cityDict")
    public Map<String, Object> cityDict() {
        Map<String, Object> dict = new HashMap<String, Object>();
        List<Map<String, Object>> tabs = new ArrayList<Map<String, Object>>();

        // 国内(含港澳台)
        Map<String, Object> tabDomestic = new HashMap<String, Object>();
        tabDomestic.put("label", "国内(含港澳台)");
        tabDomestic.put("groups", buildDomesticGroupsFull());

        // 海外
        Map<String, Object> tabOverseas = new HashMap<String, Object>();
        tabOverseas.put("label", "海外");
        List<Map<String, Object>> overseasGroups = new ArrayList<Map<String, Object>>();
        Map<String, Object> asia = new HashMap<String, Object>();
        asia.put("label", "亚洲");
        asia.put("cities", Arrays.asList("日本", "阿联酋", "新加坡", "韩国", "泰国"));
        overseasGroups.add(asia);
        Map<String, Object> europe = new HashMap<String, Object>();
        europe.put("label", "欧洲");
        europe.put("cities", Arrays.asList("英国", "德国", "法国", "意大利"));
        overseasGroups.add(europe);
        Map<String, Object> america = new HashMap<String, Object>();
        america.put("label", "美洲");
        america.put("cities", Arrays.asList("美国", "加拿大", "墨西哥"));
        overseasGroups.add(america);
        tabOverseas.put("groups", overseasGroups);

        // 热门城市
        Map<String, Object> tabHot = new HashMap<String, Object>();
        tabHot.put("label", "热门城市");
        tabHot.put("cities", Arrays.asList(
                "北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "南京", "苏州", "西安"
        ));

        tabs.add(tabDomestic);
        tabs.add(tabOverseas);
        tabs.add(tabHot);
        dict.put("tabs", tabs);
        return dict;
    }

    /**
     * 简单的经纬度 -> 城市名 反向地理解析接口（开发用途）。
     * 说明：
     * - 由于模拟器可能“无网络”，App 端 Geocoder 失败时可调用该接口；
     * - 模拟器访问主机后端使用 10.0.2.2:8080，可不依赖模拟器的外网；
     * - 该实现基于 OpenStreetMap Nominatim，仅用于演示，请勿在生产中高频调用。
     */
    @GetMapping("/api/geo/reverse")
    public Map<String, Object> reverse(@RequestParam("lat") double lat,
                                       @RequestParam("lon") double lon) {
        Map<String, Object> res = new HashMap<String, Object>();
        try {
            String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=10&addressdetails=1", lat, lon);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "xinqiao-demo/1.0 (reverse geocode)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code != 200) {
                res.put("city", null);
                res.put("ok", false);
                return res;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(sb.toString());
            JsonNode addr = root.path("address");
            String city = null;
            if (addr.isObject()) {
                // 常见字段优先级：city > town > county > state
                if (addr.hasNonNull("city")) city = addr.get("city").asText();
                else if (addr.hasNonNull("town")) city = addr.get("town").asText();
                else if (addr.hasNonNull("county")) city = addr.get("county").asText();
                else if (addr.hasNonNull("state")) city = addr.get("state").asText();
            }
            res.put("ok", true);
            res.put("city", city);
            return res;
        } catch (Exception e) {
            res.put("ok", false);
            res.put("city", null);
            return res;
        }
    }

    private List<Map<String, Object>> buildDomesticGroupsFull() {
        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();

        // 直辖市
        groups.add(group("北京", Arrays.asList("全部", "北京")));
        groups.add(group("上海", Arrays.asList("全部", "上海")));
        groups.add(group("天津", Arrays.asList("全部", "天津")));
        groups.add(group("重庆", Arrays.asList("全部", "重庆")));

        // 各省份（含港澳台）
        groups.add(group("广东", Arrays.asList(
                "全部", "广州", "深圳", "珠海", "汕头", "佛山", "江门", "湛江", "茂名", "肇庆",
                "惠州", "梅州", "汕尾", "河源", "阳江", "清远", "东莞", "中山", "潮州", "揭阳", "云浮"
        )));
        groups.add(group("江苏", Arrays.asList(
                "全部", "南京", "无锡", "徐州", "常州", "苏州", "南通", "连云港", "淮安", "盐城",
                "扬州", "镇江", "泰州", "宿迁"
        )));
        groups.add(group("浙江", Arrays.asList(
                "全部", "杭州", "宁波", "温州", "嘉兴", "湖州", "绍兴", "金华", "衢州", "舟山", "台州", "丽水"
        )));
        groups.add(group("山东", Arrays.asList(
                "全部", "济南", "青岛", "淄博", "枣庄", "东营", "烟台", "潍坊", "济宁", "泰安",
                "威海", "日照", "临沂", "德州", "聊城", "滨州", "菏泽"
        )));
        groups.add(group("河北", Arrays.asList(
                "全部", "石家庄", "唐山", "秦皇岛", "邯郸", "邢台", "保定", "张家口", "承德", "沧州", "廊坊", "衡水"
        )));
        groups.add(group("河南", Arrays.asList(
                "全部", "郑州", "开封", "洛阳", "平顶山", "安阳", "鹤壁", "新乡", "焦作", "濮阳",
                "许昌", "漯河", "三门峡", "南阳", "商丘", "信阳", "周口", "驻马店", "济源"
        )));
        groups.add(group("福建", Arrays.asList(
                "全部", "福州", "厦门", "莆田", "三明", "泉州", "漳州", "南平", "龙岩", "宁德"
        )));
        groups.add(group("湖南", Arrays.asList(
                "全部", "长沙", "株洲", "湘潭", "衡阳", "邵阳", "岳阳", "常德", "张家界", "益阳",
                "郴州", "永州", "怀化", "娄底", "湘西土家族苗族自治州"
        )));
        groups.add(group("湖北", Arrays.asList(
                "全部", "武汉", "黄石", "十堰", "宜昌", "襄阳", "鄂州", "荆门", "孝感", "荆州",
                "黄冈", "咸宁", "随州", "恩施土家族苗族自治州", "仙桃", "潜江", "天门", "神农架"
        )));
        groups.add(group("安徽", Arrays.asList(
                "全部", "合肥", "芜湖", "蚌埠", "淮南", "马鞍山", "淮北", "铜陵", "安庆", "黄山",
                "滁州", "阜阳", "宿州", "六安", "亳州", "池州", "宣城"
        )));
        groups.add(group("江西", Arrays.asList(
                "全部", "南昌", "景德镇", "萍乡", "九江", "新余", "鹰潭", "赣州", "吉安", "宜春", "抚州", "上饶"
        )));
        groups.add(group("辽宁", Arrays.asList(
                "全部", "沈阳", "大连", "鞍山", "抚顺", "本溪", "丹东", "锦州", "营口", "阜新",
                "辽阳", "盘锦", "铁岭", "朝阳", "葫芦岛"
        )));
        groups.add(group("吉林", Arrays.asList(
                "全部", "长春", "吉林", "四平", "辽源", "通化", "白山", "松原", "白城", "延边朝鲜族自治州"
        )));
        groups.add(group("黑龙江", Arrays.asList(
                "全部", "哈尔滨", "齐齐哈尔", "鸡西", "鹤岗", "双鸭山", "大庆", "伊春", "佳木斯",
                "七台河", "牡丹江", "黑河", "绥化", "大兴安岭地区"
        )));
        groups.add(group("山西", Arrays.asList(
                "全部", "太原", "大同", "阳泉", "长治", "晋城", "朔州", "晋中", "运城", "忻州", "临汾", "吕梁"
        )));
        groups.add(group("陕西", Arrays.asList(
                "全部", "西安", "铜川", "宝鸡", "咸阳", "渭南", "延安", "汉中", "榆林", "安康", "商洛"
        )));
        groups.add(group("甘肃", Arrays.asList(
                "全部", "兰州", "嘉峪关", "金昌", "白银", "天水", "武威", "张掖", "平凉",
                "酒泉", "庆阳", "定西", "陇南", "临夏回族自治州", "甘南藏族自治州"
        )));
        groups.add(group("青海", Arrays.asList(
                "全部", "西宁", "海东", "海北藏族自治州", "黄南藏族自治州", "海南藏族自治州",
                "果洛藏族自治州", "玉树藏族自治州", "海西蒙古族藏族自治州"
        )));
        groups.add(group("新疆", Arrays.asList(
                "全部", "乌鲁木齐", "克拉玛依", "吐鲁番", "哈密", "昌吉回族自治州", "博尔塔拉蒙古自治州",
                "巴音郭楞蒙古自治州", "阿克苏", "克孜勒苏柯尔克孜自治州", "喀什", "和田",
                "伊犁哈萨克自治州", "塔城", "阿勒泰", "石河子", "阿拉尔", "图木舒克", "五家渠"
        )));
        groups.add(group("西藏", Arrays.asList(
                "全部", "拉萨", "日喀则", "昌都", "林芝", "山南", "那曲", "阿里地区"
        )));
        groups.add(group("内蒙古", Arrays.asList(
                "全部", "呼和浩特", "包头", "乌海", "赤峰", "通辽", "鄂尔多斯", "呼伦贝尔",
                "巴彦淖尔", "乌兰察布", "锡林郭勒盟", "阿拉善盟"
        )));
        groups.add(group("广西", Arrays.asList(
                "全部", "南宁", "柳州", "桂林", "梧州", "北海", "防城港", "钦州", "贵港",
                "玉林", "百色", "贺州", "河池", "来宾", "崇左"
        )));
        groups.add(group("云南", Arrays.asList(
                "全部", "昆明", "曲靖", "玉溪", "保山", "昭通", "丽江", "普洱", "临沧",
                "楚雄彝族自治州", "红河哈尼族彝族自治州", "文山壮族苗族自治州", "西双版纳傣族自治州",
                "大理白族自治州", "德宏傣族景颇族自治州", "怒江傈僳族自治州", "迪庆藏族自治州"
        )));
        groups.add(group("四川", Arrays.asList(
                "全部", "成都", "自贡", "攀枝花", "泸州", "德阳", "绵阳", "广元", "遂宁", "内江",
                "乐山", "南充", "眉山", "宜宾", "广安", "达州", "雅安", "巴中", "资阳",
                "阿坝藏族羌族自治州", "甘孜藏族自治州", "凉山彝族自治州"
        )));
        groups.add(group("海南", Arrays.asList(
                "全部", "海口", "三亚", "三沙", "儋州"
        )));
        groups.add(group("贵州", Arrays.asList(
                "全部", "贵阳", "六盘水", "遵义", "安顺", "毕节", "铜仁",
                "黔西南布依族苗族自治州", "黔东南苗族侗族自治州", "黔南布依族苗族自治州"
        )));

        // 港澳台
        groups.add(group("香港", Arrays.asList("全部", "香港")));
        groups.add(group("澳门", Arrays.asList("全部", "澳门")));
        groups.add(group("台湾", Arrays.asList(
                "全部", "台北", "新北", "桃园", "台中", "台南", "高雄", "基隆", "新竹", "嘉义"
        )));

        return groups;
    }

    private Map<String, Object> group(String label, List<String> cities) {
        Map<String, Object> g = new HashMap<String, Object>();
        g.put("label", label);
        g.put("cities", cities);
        return g;
    }

    private List<Map<String, Object>> baseConsultants() {
        List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();

        Map<String, Object> c1 = new HashMap<String, Object>();
        c1.put("id", "c1");
        c1.put("name", "李老师");
        c1.put("title", "国家二级心理咨询师");
        // 使用稳定的公开占位头像，避免外部服务403
        c1.put("avatar", "https://picsum.photos/seed/consult_c1/150");
        c1.put("certified", true);
        c1.put("skills", Arrays.asList("焦虑缓解", "职场压力"));
        c1.put("rating", 4.9);
        c1.put("consultCount", 320);
        c1.put("price", 299);
        c1.put("duration", 60);
        c1.put("defaultMode", "文字咨询");
        c1.put("city", "北京");
        all.add(c1);

        Map<String, Object> c2 = new HashMap<String, Object>();
        c2.put("id", "c2");
        c2.put("name", "王老师");
        c2.put("title", "婚恋情感顾问");
        c2.put("avatar", "https://picsum.photos/seed/consult_c2/150");
        c2.put("certified", true);
        c2.put("skills", Arrays.asList("情感关系", "亲子教育", "抑郁干预"));
        c2.put("rating", 4.8);
        c2.put("consultCount", 210);
        c2.put("price", 359);
        c2.put("duration", 50);
        c2.put("defaultMode", "语音咨询");
        c2.put("city", "上海");
        all.add(c2);

        Map<String, Object> c3 = new HashMap<String, Object>();
        c3.put("id", "c3");
        c3.put("name", "赵顾问");
        c3.put("title", "资深心理顾问");
        c3.put("avatar", "https://picsum.photos/seed/consult_c3/150");
        c3.put("certified", true);
        c3.put("skills", Arrays.asList("抑郁", "情绪管理"));
        c3.put("rating", 4.7);
        c3.put("consultCount", 180);
        c3.put("price", 199);
        c3.put("duration", 45);
        c3.put("defaultMode", "文字咨询");
        c3.put("city", "广州");
        all.add(c3);

        return all;
    }
}
