package com.metrocarpool.driver.util;

import com.metrocarpool.contracts.proto.DriverLocationEvent;
import com.metrocarpool.contracts.proto.DriverRideCompletionEvent;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.metrocarpool.driver.cache.DriverCache;
import com.metrocarpool.driver.proto.PostDriver;

import java.time.Duration;
import java.util.*;

/**
 * Utility class for building test data objects
 */
public class TestDataBuilder {

    public static DriverCache buildDriverCache(Long driverId, Integer availableSeats) {
        return DriverCache.builder()
                .availableSeats(availableSeats)
                .routePlaces(Arrays.asList("Location1", "Location2", "Location3", "Location4"))
                .nextPlace("Location2")
                .timeToNextPlace(Duration.ofMinutes(10))
                .distanceToNextPlace(50.0)
                .finalDestination("Location4")
                .lastSeenMetroStation("Station1")
                .build();
    }

    public static DriverCache buildDriverCacheWithRoute(List<String> route, String finalDestination) {
        return DriverCache.builder()
                .availableSeats(3)
                .routePlaces(route)
                .nextPlace(route.size() > 1 ? route.get(1) : route.get(0))
                .timeToNextPlace(Duration.ofMinutes(10))
                .distanceToNextPlace(50.0)
                .finalDestination(finalDestination)
                .lastSeenMetroStation("")
                .build();
    }

    public static Map<Long, DriverCache> buildDriverCacheMap() {
        Map<Long, DriverCache> cacheMap = new HashMap<>();
        cacheMap.put(1L, buildDriverCache(1L, 3));
        cacheMap.put(2L, buildDriverCache(2L, 2));
        return cacheMap;
    }

    public static Map<String, Map<String, Double>> buildLocationLocationMap() {
        Map<String, Map<String, Double>> locationMap = new HashMap<>();
        
        Map<String, Double> location1Distances = new HashMap<>();
        location1Distances.put("Location2", 50.0);
        location1Distances.put("Location3", 100.0);
        location1Distances.put("Location4", 150.0);
        locationMap.put("Location1", location1Distances);
        
        Map<String, Double> location2Distances = new HashMap<>();
        location2Distances.put("Location3", 50.0);
        location2Distances.put("Location4", 100.0);
        locationMap.put("Location2", location2Distances);
        
        Map<String, Double> location3Distances = new HashMap<>();
        location3Distances.put("Location4", 50.0);
        locationMap.put("Location3", location3Distances);
        
        return locationMap;
    }

    public static Map<String, String> buildNearbyStationsMap() {
        Map<String, String> nearbyMap = new HashMap<>();
        nearbyMap.put("Location1", "Station1");
        nearbyMap.put("Location2", "Station2");
        nearbyMap.put("Location3", "Station3");
        nearbyMap.put("Location4", "Station4");
        return nearbyMap;
    }

    public static PostDriver buildPostDriverRequest(Long driverId, List<String> route, String destination, Integer seats) {
        return PostDriver.newBuilder()
                .setDriverId(driverId)
                .addAllRouteStations(route)
                .setFinalDestination(destination)
                .setAvailableSeats(seats)
                .build();
    }

    public static DriverRiderMatchEvent buildDriverRiderMatchEvent(Long driverId, Long riderId, String pickupStation) {
        return DriverRiderMatchEvent.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setDriverId(driverId)
                .setRiderId(riderId)
                .setPickUpStation(pickupStation)
                .build();
    }

    public static DriverLocationEvent buildDriverLocationEvent(Long driverId, String oldStation, String nextStation, 
                                                                int timeToNext, int availableSeats, String finalDestination) {
        return DriverLocationEvent.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setDriverId(driverId)
                .setOldStation(oldStation)
                .setNextStation(nextStation)
                .setTimeToNextStation(timeToNext)
                .setAvailableSeats(availableSeats)
                .setFinalDestination(finalDestination)
                .build();
    }

    public static DriverRideCompletionEvent buildDriverRideCompletionEvent(Long driverId) {
        return DriverRideCompletionEvent.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setDriverId(driverId)
                .build();
    }
}
