package com.skillmatch.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 地理工具 — 逆地理编码 & 坐标兜底城市映射
 * <p>供 UserServiceImpl / AdminServiceImpl 共用</p>
 */
@Slf4j
@Component
public class GeoUtil {

    public static final Set<String> KNOWN_CITIES = Set.of(
            "北京", "上海", "广州", "深圳", "成都", "重庆", "杭州", "武汉", "南京", "天津",
            "石家庄", "太原", "呼和浩特", "沈阳", "长春", "哈尔滨", "合肥", "福州", "南昌", "济南",
            "郑州", "长沙", "南宁", "海口", "贵阳", "昆明", "拉萨", "西安", "兰州", "西宁", "银川", "乌鲁木齐",
            "苏州", "无锡", "宁波", "厦门", "青岛", "佛山", "东莞", "大连", "温州",
            "泉州", "烟台", "常州", "绍兴", "珠海", "潍坊", "惠州", "中山", "襄阳", "洛阳"
    );

    @Value("${gaoDe.key}")
    private String gaoDeKey;

    /**
     * 调用高德逆地理编码，将经纬度转为城市名
     * 失败时使用经纬度范围兜底映射到主要城市
     */
    public String resolveCity(double longitude, double latitude) {
        // 1) 优先高德 API
        try {
            String body = HttpUtil.get(StrUtil.format(
                    "https://restapi.amap.com/v3/geocode/regeo?location={},{}&key={}",
                    longitude, latitude, gaoDeKey), 3000);
            JSONObject c = JSONUtil.parseObj(body)
                    .getByPath("regeocode.addressComponent", JSONObject.class);
            if (c == null) {
                return fallbackCityByCoords(longitude, latitude);
            }
            // city 正常为字符串，直辖市/郊区为空数组 []
            Object raw = c.get("city");
            String city;
            if (raw != null && !(raw instanceof cn.hutool.json.JSONArray)) {
                city = raw.toString();
            } else {
                Object p = c.get("province");
                if (p != null && !(p instanceof cn.hutool.json.JSONArray)) {
                    city = p.toString();
                } else {
                    city = fallbackCityByCoords(longitude, latitude);
                }
            }
            //去掉市
            city = city.replace("市", "");
            // 校验返回结果是否为已知城市名（非省份），否则用坐标兜底
            String result = StrUtil.isBlank(city) ? fallbackCityByCoords(longitude, latitude) : city;
            return KNOWN_CITIES.contains(result) ? result : fallbackCityByCoords(longitude, latitude);
        } catch (Exception e) {
            log.warn("高德逆地理编码失败, lng={}, lat={}, 使用坐标兜底", longitude, latitude);
            return fallbackCityByCoords(longitude, latitude);
        }
    }

    /**
     * 根据经纬度范围兜底映射到中国主要城市
     * <p>覆盖 24 个主要城市的大致范围，确保管理端地图始终有标记点</p>
     */
    private String fallbackCityByCoords(double lng, double lat) {
        // 按距离/范围粗略匹配主要城市
        // 东北
        if (lng > 126 && lat > 45) return "哈尔滨";
        if (lng > 123 && lat > 41) return "沈阳";
        // 华北
        if (lng > 115 && lng < 118 && lat > 38 && lat < 41) return "北京";
        if (lng > 116 && lng < 119 && lat > 38 && lat < 40) return "天津";
        // 华东
        if (lng > 120 && lng < 122 && lat > 30 && lat < 32) return "上海";
        if (lng > 118 && lng < 122 && lat > 31 && lat < 33) return "南京";
        if (lng > 119 && lng < 121 && lat > 29 && lat < 31) return "杭州";
        if (lng > 116 && lng < 120 && lat > 30 && lat < 32) return "合肥";
        if (lng > 120 && lng < 121 && lat > 30 && lat < 32) return "苏州";
        if (lng > 118 && lng < 121 && lat > 35 && lat < 38) return "青岛";
        if (lng > 116 && lng < 118 && lat > 36 && lat < 37) return "济南";
        if (lng > 117 && lng < 120 && lat > 25 && lat < 27) return "福州";
        if (lng > 117 && lng < 119 && lat > 24 && lat < 25) return "厦门";
        // 华中
        if (lng > 113 && lng < 115 && lat > 29 && lat < 32) return "武汉";
        if (lng > 112 && lng < 114 && lat > 27 && lat < 29) return "长沙";
        if (lng > 112 && lng < 114 && lat > 34 && lat < 35) return "郑州";
        // 华南
        if (lng > 112 && lng < 114 && lat > 22 && lat < 24) return "广州";
        if (lng > 113 && lng < 115 && lat > 22 && lat < 23) return "深圳";
        if (lng > 107 && lng < 109 && lat > 22 && lat < 23) return "南宁";
        // 西南
        if (lng > 103 && lng < 105 && lat > 30 && lat < 31) return "成都";
        if (lng > 106 && lng < 107 && lat > 29 && lat < 30) return "重庆";
        if (lng > 102 && lng < 103 && lat > 24 && lat < 26) return "昆明";
        if (lng > 106 && lng < 107 && lat > 26 && lat < 27) return "贵阳";
        // 西北
        if (lng > 107 && lng < 110 && lat > 33 && lat < 35) return "西安";
        // 默认 — 返回河北省会或其他粗粒度
        return "北京";
    }
}
