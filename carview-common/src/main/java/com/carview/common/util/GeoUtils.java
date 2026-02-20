package com.carview.common.util;

/**
 * 地理位置工具类
 */
public final class GeoUtils {

    private GeoUtils() {}

    /** 地球半径(米) */
    private static final double EARTH_RADIUS = 6371000.0;

    /**
     * 判断点是否在矩形围栏内
     */
    public static boolean isInRectangle(double lng, double lat,
                                        double minLng, double minLat,
                                        double maxLng, double maxLat) {
        return lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat;
    }

    /**
     * 判断点是否在圆形围栏内
     */
    public static boolean isInCircle(double lng, double lat,
                                     double centerLng, double centerLat,
                                     double radiusMeters) {
        double distance = calculateDistance(lat, lng, centerLat, centerLng);
        return distance <= radiusMeters;
    }

    /**
     * 计算两点之间的距离（米），使用 Haversine 公式
     */
    public static double calculateDistance(double lat1, double lng1,
                                           double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}
