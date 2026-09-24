package com.lind.algorithm.geo;

/**
 * 经纬度。
 * @param latitude 纬度 [-90, 90]
 * @param longitude 经度 [-180, 180]
 */
public record LatLon(double latitude, double longitude) {
}
