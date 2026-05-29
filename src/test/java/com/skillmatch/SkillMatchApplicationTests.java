package com.skillmatch;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;

@SpringBootTest
public class SkillMatchApplicationTests {

    private static final int BITS = 26;
    private static final double[] LAT_RANGE = {-90.0, 90.0};
    private static final double[] LNG_RANGE = {-180.0, 180.0};

    private static final int PRECISION = 12; // GeoHash字符串长度

    public static long encode(double longitude, double latitude) {
        long lat = encode(latitude, LAT_RANGE[0], LAT_RANGE[1]);
        long lng = encode(longitude, LNG_RANGE[0], LNG_RANGE[1]);

        // 交织位：从最低位开始构建
        long result = 0;
        for (int i = 0; i < BITS; i++) {
            int lngBit = (int)((lng >> i) & 1);
            int latBit = (int)((lat >> i) & 1);
            result |= ((long)lngBit) << (2 * i + 1);
            result |= ((long)latBit) << (2 * i);
        }
        return result;
    }

    private static long encode(double value, double min, double max) {
        long result = 0;
        for (int i = 0; i < BITS; i++) {
            double mid = (min + max) / 2;
            if (value >= mid) {
                result |= (1L << (BITS - 1 - i));
                min = mid;
            } else {
                max = mid;
            }
        }
        return result;
    }
    @Test
    public void contextLoads(){
        long score = encode(116.4074, 39.9042);
        System.out.println(score);
    }

}
