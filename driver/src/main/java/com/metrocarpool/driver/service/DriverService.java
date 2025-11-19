package com.metrocarpool.driver.service;

import com.metrocarpool.contracts.proto.DriverLocationEvent;
import com.metrocarpool.contracts.proto.DriverRideCompletionEvent;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.metrocarpool.driver.cache.DriverCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Jackson for safe JSON parsing of plain Redis values (Option B)
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class DriverService {
    // ✅ Inject KafkaTemplate to publish events (assuming Spring Boot Kafka configured)
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.driver-location-topic}")
    private String DRIVER_TOPIC;

    @Value("${kafka.topics.ride-completion-topic}")
    private String RIDE_COMPLETION_TOPIC;

    // Redis Cache top level key
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String DRIVER_CACHE_KEY = "drivers";
    @SuppressWarnings("unused")
    private final RedisTemplate<String, Object> redisTemplateNearby;
    private static final String NEARBY_STATIONS_CACHE_KEY = "nearby-stations";
    @SuppressWarnings("unused")
    private final RedisTemplate<String, Object> redisTemplateLocationMap;
    private static final String LOCATION_LOCATION_MAP_CACHE_KEY = "location-location-map";

    // String template + mapper for tolerant reads of plain JSON (no @class)
    private final RedisTemplate<String, String> redisStringTemplate;
    private final ObjectMapper objectMapper;

    // Simulation constants
    private static final double DISTANCE_PER_TICK = 10.0;     // units per cron tick (2 minutes)
    private static final long SECONDS_PER_TICK = 120L;       // 2 minutes = 120 seconds

    public boolean processDriverInfo(Long driverId, List<String> routePlaces, String finalDestination,
                                     Integer availableSeats) {
        try {
            // Validate basic inputs early
            if (driverId == null || routePlaces == null || routePlaces.size() < 2 ||
                finalDestination == null || finalDestination.isEmpty() ||
                availableSeats == null || availableSeats <= 0) {
                log.warn("Invalid driver info: id={}, routeSize={}, dest={}, seats={}. Returning false.",
                        driverId, routePlaces == null ? null : routePlaces.size(), finalDestination, availableSeats);
                return false;
            }

            log.info("Reached DriverService.processDriverInfo with driverId = {}, routePlaces = {}, finalDestination = {}, availableSeats = {}",
                    driverId, routePlaces, finalDestination, availableSeats);

            // Safe reads (tolerant to plain JSON; avoids @class requirement)
            // Nearby map is not required for initial driver registration; read later during cron
            Object raw = redisTemplate.opsForValue().get(DRIVER_CACHE_KEY);
            Map<Long, DriverCache> allDriverCacheData = normalizeDriverCache(raw);
            if (allDriverCacheData.isEmpty()) {
                log.warn("Driver cache not found or empty; initializing new cache.");
                allDriverCacheData = new HashMap<>();
            }
            Map<String, Map<String, Double>> locationLocationMap = safeReadLocationMap();
            if (locationLocationMap.isEmpty()) {
                log.warn("Location-Location map missing/empty; cannot compute distances. Returning false.");
                return false;
            }
            // Initialize DriverCache
            DriverCache driverCache = DriverCache.builder()
                    .availableSeats(availableSeats)
                    .routePlaces(routePlaces)
                    .nextPlace(routePlaces.get(1))
                    .timeToNextPlace(computeDurationFromDistance(
                            getDistanceBetween(routePlaces.get(0), routePlaces.get(1), locationLocationMap)))
                    .distanceToNextPlace(getDistanceBetween(routePlaces.get(0), routePlaces.get(1), locationLocationMap))
                    .finalDestination(finalDestination)
                    .lastSeenMetroStation("") // initially empty
                    .build();


            allDriverCacheData.put(driverId, driverCache);
            redisTemplate.opsForValue().set(DRIVER_CACHE_KEY, allDriverCacheData);
            return true;
        } catch (Exception e) {
            log.error("Failed to process driver info for ID {}: {}. Returning false.", driverId, e.getMessage());
            return false;
        }
    }

    @KafkaListener(topics = "rider-driver-match", groupId = "matching-service")
    public void matchFoundUpdateCache(byte[] message,
                                      Acknowledgment acknowledgment) {

        try{
            DriverRiderMatchEvent  event = DriverRiderMatchEvent.parseFrom(message);
            Long driverId = event.getDriverId();
            Long riderId = event.getRiderId();
            String pickUpStation = event.getPickUpStation();
            // Acknowledge that you have got the message
            acknowledgment.acknowledge();

            log.info("Reached DriverService.matchFoundUpdateCache.");
            // Decrement the availableSeats by 1 for this driverId
            Object raw = redisTemplate.opsForValue().get(DRIVER_CACHE_KEY);
            Map<Long, DriverCache> allDriverCacheData = normalizeDriverCache(raw);
            DriverCache driverCache = allDriverCacheData.get(driverId);
            if (driverCache == null) {
                log.warn("Driver with ID = {} not found in cache during match update. Returning void.", driverId);
                return;
            }
            int currentAvailableSeats = Optional.ofNullable(driverCache.getAvailableSeats()).orElse(0);
            currentAvailableSeats = Math.max(0, currentAvailableSeats - 1);
            driverCache.setAvailableSeats(currentAvailableSeats);

            // If availableSeats == 0 => evict from cache, else write back
            if (currentAvailableSeats == 0) {
                log.info("Driver = {}, has 0 available seats.", driverCache);
//                allDriverCacheData.remove(driverId);
                allDriverCacheData.put(driverId, driverCache);
            } else {
                allDriverCacheData.put(driverId, driverCache);
            }
            redisTemplate.opsForValue().set(DRIVER_CACHE_KEY, allDriverCacheData);
        } catch (InvalidProtocolBufferException e){
            log.error("Failed to parse RiderDriverMatchEvent protobuf message", e);
        }
        
    }

   /**
     * Cron job every 2 minutes -> moves drivers along their route, updates cache & emits events.
     */
    @Scheduled(cron = "0 */2 * * * *")
    public void cronJobDriverLocationSimulation() {
        log.debug("cron tick - driver simulation starting");
        log.info("CRON job ka tick-tick chal raha hai...");
        // Read caches from Redis and normalize key/value types
        Object rawDrivers = redisTemplate.opsForValue().get(DRIVER_CACHE_KEY);
        Map<Long, DriverCache> allDriverCacheData = normalizeDriverCache(rawDrivers);
        if (allDriverCacheData.isEmpty()) {
            log.warn("Driver cache empty or unreadable. Key: {}", DRIVER_CACHE_KEY);
            return;
        }

        // Tolerant reads (no @class requirement)
        Map<String, Map<String, Double>> locationLocationMap = safeReadLocationMap();
        if (locationLocationMap.isEmpty()) {
            log.warn("Location map missing or empty. Key: {}", LOCATION_LOCATION_MAP_CACHE_KEY);
            return;
        }
        Map<String, String> nearbyStationMap = safeReadNearby();

        // Iterate drivers and update
        List<Long> driversToEvict = new ArrayList<>();
        for (Map.Entry<Long, DriverCache> e : allDriverCacheData.entrySet()) {
            Long driverId = e.getKey();
            DriverCache cache = e.getValue();
            try {
                boolean evict = processSingleDriverTick(driverId, cache, locationLocationMap, nearbyStationMap);
                if (evict) {
                    driversToEvict.add(driverId);
                } else {
                    // update the map value (already mutated)
                    allDriverCacheData.put(driverId, cache);
                }
            } catch (Exception ex) {
                log.error("Error processing driver {}: {}", driverId, ex.getMessage(), ex);
            }
        }

        // Evict drivers that reached final destination
        for (Long id : driversToEvict) {
            allDriverCacheData.remove(id);
            log.info("Driver {} evicted from cache - reached final destination", id);
        }

        // Persist updated driver cache back to Redis
        redisTemplate.opsForValue().set(DRIVER_CACHE_KEY, allDriverCacheData);

        log.debug("cron tick - driver simulation finished. updated drivers: {}, evicted: {}",
                allDriverCacheData.size(), driversToEvict.size());
    }

    /**
     * Process one driver's tick: decrement distance, advance route nodes if needed, update times,
     * compute next metro station and emit Kafka event.
//     * @param driverId
     * @return true if driver should be evicted (reached final destination)
     */
    private boolean processSingleDriverTick(
            Long driverId,
            DriverCache cache,
            Map<String, Map<String, Double>> locationLocationMap,
            Map<String, String> nearbyStationMap) {

        if (cache == null) return true;

        log.info("Reached DriverService.processSingleDriverTick for driver ID = {}", driverId);

        // Defensive checks
        List<String> route = cache.getRoutePlaces();
        if (route == null || route.isEmpty()) {
            log.warn("Driver {} has empty route; evicting", driverId);
            return true;
        }
        String currentNextPlace = cache.getNextPlace();
        if (currentNextPlace == null) {
            // If nextPlace missing, set to first route node after assumed current
            currentNextPlace = route.get(0);
            cache.setNextPlace(currentNextPlace);
        }

        double distanceToNext = Optional.ofNullable(cache.getDistanceToNextPlace()).orElse(0.0);
        double newDistanceToNext = distanceToNext - DISTANCE_PER_TICK;

        // If not crossing a node, update distance/time and emit event
        String oldMetroStation = Optional.ofNullable(cache.getLastSeenMetroStation()).orElse("");
        String newNextMetroStation = "";

        // Remaining distance to process when crossing nodes
        double remainder = 0.0;

        // find index of currentNextPlace in route (we assume route list is ordered in traversal direction)
        int currentIndex = indexOf(route, currentNextPlace);
        if (currentIndex == -1) {
            // defensive fallback: set to 0
            currentIndex = 0;
            cache.setNextPlace(route.get(0));
        }

        if (newDistanceToNext > 0) {
            // simple case, didn't yet reach the next place
            cache.setDistanceToNextPlace(newDistanceToNext);
            cache.setTimeToNextPlace(computeDurationFromDistance(newDistanceToNext));
        } else {
            // crossed into next node or multiple nodes; compute remainder and advance route
            remainder = Math.abs(newDistanceToNext);

            // advance until remainder consumed or route ends
            boolean reachedFinalDest = false;
            int idx = currentIndex;
            String prevPlace = currentNextPlace; // place we just reached (old nextPlace becomes current)
            while (true) {
                // if prevPlace equals finalDestination -> evict
                if (prevPlace != null && prevPlace.equals(cache.getFinalDestination())) {
                    // We reached final destination during this tick
                    log.info("Driver reached final destination: driverId={}, finalDest={}", driverId, prevPlace);
                    DriverRideCompletionEvent event = DriverRideCompletionEvent.newBuilder()
                            .setDriverId(driverId)
                            .build();

                    CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(RIDE_COMPLETION_TOPIC,
                            String.valueOf(driverId), event.toByteArray());
                    future.thenAccept(result -> {
                        log.debug("Event = {} delivered to {}", event, result.getRecordMetadata().topic());
                    }).exceptionally(ex -> {
                        log.error("Event failed. Error message = {}", ex.getMessage());
                        // Optional: retry, put into Redis dead-letter queue
                        return null;
                    });
                    return true; // evict driver
                }

                // determine next index
                int nextIdx = idx + 1;
                if (nextIdx >= route.size()) {
                    // no next node -> driver finished
                    log.info("Driver {} has no further nodes in route -> evicting", driverId);
                    return true;
                }

                String nextPlace = route.get(nextIdx);
                double segmentDistance = getDistanceBetween(prevPlace, nextPlace, locationLocationMap);

                if (Double.isInfinite(segmentDistance)) {
                    // if distance unknown, assume large; stop to avoid infinite loop
                    log.warn("Distance between {} and {} unknown. Stopping advancement for driver {}", prevPlace, nextPlace, driverId);
                    // set next place and use remainder as is (no progress)
                    cache.setNextPlace(nextPlace);
                    cache.setDistanceToNextPlace(segmentDistance);
                    cache.setTimeToNextPlace(computeDurationFromDistance(segmentDistance));
                    break;
                }

                if (remainder < segmentDistance) {
                    // we land somewhere between prevPlace and nextPlace
                    double newDistToNext = segmentDistance - remainder;
                    cache.setNextPlace(nextPlace);
                    cache.setDistanceToNextPlace(newDistToNext);
                    cache.setTimeToNextPlace(computeDurationFromDistance(newDistToNext));
                    // set prevPlace as current physical place for next iteration's station search
                    prevPlace = prevPlace; // current location is between prevPlace and nextPlace
                    break;
                } else {
                    // we fully cross this segment; subtract and continue
                    remainder = remainder - segmentDistance;
                    // shift prevPlace / idx forward
                    prevPlace = nextPlace;
                    idx = nextIdx;
                    // if we've exactly landed on nextPlace and it's final destination -> evict
                    if (prevPlace.equals(cache.getFinalDestination())) {
                        log.info("Driver {} reached final destination during multi-hop progression", driverId);
                        return true;
                    }
                    // if remainder == 0 -> we are exactly at prevPlace; set nextPlace to the following node
                    if (remainder == 0) {
                        int followingIdx = idx + 1;
                        if (followingIdx >= route.size()) {
                            // finished route
                            return true;
                        }
                        String followingPlace = route.get(followingIdx);
                        double nextSegDistance = getDistanceBetween(prevPlace, followingPlace, locationLocationMap);
                        cache.setNextPlace(followingPlace);
                        cache.setDistanceToNextPlace(nextSegDistance);
                        cache.setTimeToNextPlace(computeDurationFromDistance(nextSegDistance));
                        break;
                    }
                    // else continue loop to consume remainder across next segment
                }
            } // end while
        } // end crossing logic

        // Update last seen / next metro station using nearbyStationMap scanning remaining route from current position
        String lastSeen = Optional.ofNullable(cache.getLastSeenMetroStation()).orElse("");
        newNextMetroStation = findNextMetroStationInRoute(cache.getNextPlace(), cache.getRoutePlaces(), nearbyStationMap);

        // Update lastSeen logic: if the driver passed a metro station in this tick, update lastSeen
        // We'll detect if any station id equals nearbyStationMap mapping for nodes we traversed.
        // Simple heuristic: if oldMetroStation is empty and the nearest station for current nextPlace equals something, set lastSeen accordingly.
        // For simulation robustness we'll update lastSeen only if we detect that we passed a node with a station.
        String passedStation = detectPassedMetroStationDuringTick(cache, locationLocationMap, nearbyStationMap, DISTANCE_PER_TICK);
        if (!passedStation.isEmpty()) {
            cache.setLastSeenMetroStation(passedStation);
            oldMetroStation = passedStation;
        }

        // Prepare kafka event with oldStation, nextStation
        String oldStationForEvent = Optional.ofNullable(cache.getLastSeenMetroStation()).orElse("");
        String nextStationForEvent = newNextMetroStation == null ? "" : newNextMetroStation;

        // compute timeToNextStation using distance from current location to next station
        int timeToNextStationSec = computeTimeToNextStationSec(cache, newNextMetroStation, locationLocationMap, nearbyStationMap);

        // available seats & finalDestination
        int availableSeats = Optional.ofNullable(cache.getAvailableSeats()).orElse(0);
        String finalDestination = Optional.ofNullable(cache.getFinalDestination()).orElse("");

        if (availableSeats > 0) {
            // emit Kafka event
            DriverLocationEvent event = DriverLocationEvent.newBuilder()
                    .setDriverId(driverId)
                    .setOldStation(oldStationForEvent)
                    .setNextStation(nextStationForEvent)
                    .setTimeToNextStation(timeToNextStationSec)
                    .setAvailableSeats(availableSeats)
                    .setFinalDestination(finalDestination)
                    .build();

            // send with key driverId
            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(DRIVER_TOPIC,
                    String.valueOf(driverId), event.toByteArray());
            future.thenAccept(result -> {
                log.debug("Event = {} delivered to {}", event, result.getRecordMetadata().topic());
            }).exceptionally(ex -> {
                log.error("Event failed. Error message = {}", ex.getMessage());
                // Optional: retry, put into Redis dead-letter queue
                return null;
            });
            log.debug("Published driver location event for driver {}: oldStation={}, nextStation={}, tts={}s",
                    driverId, oldStationForEvent, nextStationForEvent, timeToNextStationSec);

        }
        return false; // not evicted
    }

    // ---------- Helper functions ----------

    // Safe readers that accept plain JSON strings and serializer JSON alike
    private Map<String, String> safeReadNearby() {
        try {
            log.info("Reached DriverService.safeReadNearby.");
            String json = redisStringTemplate.opsForValue().get(NEARBY_STATIONS_CACHE_KEY);
            if (json == null || json.isEmpty()) return new HashMap<>();
            // First parse as generic map to tolerate typed JSON (with "@class")
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                if ("@class".equals(e.getKey())) continue;
                Object v = e.getValue();
                if (v != null) result.put(e.getKey(), String.valueOf(v));
            }
            return result;
        } catch (Exception e) {
            log.error("nearby-stations parse failed: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Map<String, Double>> safeReadLocationMap() {
        try {
            log.info("Reached DriverService.safeReadLocationMap.");
            String json = redisStringTemplate.opsForValue().get(LOCATION_LOCATION_MAP_CACHE_KEY);
            if (json == null || json.isEmpty()) return new HashMap<>();
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, Map<String, Double>> result = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                if ("@class".equals(e.getKey())) continue;
                Object inner = e.getValue();
                if (inner instanceof Map) {
                    Map<?, ?> innerMap = (Map<?, ?>) inner;
                    Map<String, Double> typedInner = new HashMap<>();
                    for (Map.Entry<?, ?> ie : innerMap.entrySet()) {
                        String k = String.valueOf(ie.getKey());
                        Object val = ie.getValue();
                        if (val instanceof Number) {
                            typedInner.put(k, ((Number) val).doubleValue());
                        } else {
                            try {
                                typedInner.put(k, Double.parseDouble(String.valueOf(val)));
                            } catch (NumberFormatException nfe) {
                                // skip non-numeric
                            }
                        }
                    }
                    result.put(e.getKey(), typedInner);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("location-location-map parse failed: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private int indexOf(List<String> route, String place) {
        log.info("Reached DriverService.indexOf");
        if (route == null) return -1;
        for (int i = 0; i < route.size(); i++) {
            if (Objects.equals(route.get(i), place)) return i;
        }
        return -1;
    }

    /**
     * Get distance between two places using the locationLocationMap structure.
     * Tries symmetric lookup: map[a].get(b) or map[b].get(a). Returns Double.POSITIVE_INFINITY if unknown.
     */
    private double getDistanceBetween(String a, String b, Map<String, Map<String, Double>> locationLocationMap) {
        log.info("Reached DriverService.getDistanceBetween.");
        if (a == null || b == null) return Double.POSITIVE_INFINITY;
        if (Objects.equals(a, b)) return 0.0;
        Map<String, Double> inner = locationLocationMap.get(a);
        if (inner != null && inner.containsKey(b)) {
            return inner.get(b);
        }
        Map<String, Double> inner2 = locationLocationMap.get(b);
        if (inner2 != null && inner2.containsKey(a)) {
            return inner2.get(a);
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Convert distance units to java.time.Duration (approx based on DISTANCE_PER_TICK).
     * Returns Duration in seconds computed as ceil(distance / DISTANCE_PER_TICK * SECONDS_PER_TICK).
     */
    private Duration computeDurationFromDistance(double distance) {
        log.info("Reached DriverService.computeDurationFromDistance.");
        if (Double.isInfinite(distance) || distance <= 0) {
            return Duration.ZERO;
        }
        double ticks = distance / DISTANCE_PER_TICK;
        long secs = (long) Math.ceil(ticks * SECONDS_PER_TICK);
        return Duration.ofSeconds(secs);
    }

    /**
     * Find the first metro station mapped by nearbyStationMap in route starting from currentNextPlace index inclusive.
     * nearbyStationMap maps placeId -> stationId (if place has a nearby metro station).
     * Returns stationId or empty string if not found.
     */
    private String findNextMetroStationInRoute(String currentNextPlace, List<String> routePlaces, Map<String, String> nearbyStationMap) {
        log.info("Reached DriverService.findNextMetroStationInRoute.");
        if (routePlaces == null || routePlaces.isEmpty()) return "";
        int startIdx = indexOf(routePlaces, currentNextPlace);
        if (startIdx == -1) startIdx = 0;
        for (int i = startIdx; i < routePlaces.size(); i++) {
            String place = routePlaces.get(i);
            String station = nearbyStationMap.get(place);
            if (station != null && !station.isEmpty()) {
                return station;
            }
        }
        return "";
    }

    /**
     * Detect if the driver passed a metro station during this tick.
     * Simple heuristic: check nodes in route that are within DISTANCE_PER_TICK from current progress.
     * For simulation we return the first matched station id, else empty.
     */
    private String detectPassedMetroStationDuringTick(DriverCache cache,
                                                     Map<String, Map<String, Double>> locationLocationMap,
                                                     Map<String, String> nearbyStationMap,
                                                     double distanceCovered) {
        log.info("Reached DriverService.detectPassedMetroStationDuringTick.");
        if (cache == null) return "";
        List<String> route = cache.getRoutePlaces();
        if (route == null || route.isEmpty()) return "";

        // find position: we have current nextPlace and distanceToNextPlace; estimate previous pass node
        String nextPlace = cache.getNextPlace();
        int idx = indexOf(route, nextPlace);
        if (idx <= 0) {
            // we are near beginning; nothing passed yet
            return "";
        }

        // We might have passed nodes between idx-1 and idx depending on distance covered.
        // Loop backward from previous nodes to see if any had a nearby station.
        for (int i = idx - 1; i >= 0; i--) {
            String place = route.get(i);
            String station = nearbyStationMap.get(place);
            if (station != null && !station.isEmpty()) {
                // rough heuristic - assume we passed it
                return station;
            }
        }
        return "";
    }

    /**
     * Compute time to next metro station in seconds from current driver state using map distances.
     * If nextStationId is empty string -> return 0 (or -1 optionally). We return 0 per instructions.
     */
    private int computeTimeToNextStationSec(DriverCache cache,
                                            String nextStationId,
                                            Map<String, Map<String, Double>> locationLocationMap,
                                            Map<String, String> nearbyStationMap) {
        log.info("Reached DriverService.computeTimeToNextStationSec.");
        if (cache == null || nextStationId == null || nextStationId.isEmpty()) return 0;

        // We must find the first place on route that maps to nextStationId, then compute distance from
        // current logical position to that place by summing segment distances.
        List<String> route = cache.getRoutePlaces();
        if (route == null || route.isEmpty()) return 0;

        // find index of route place whose nearbyStationMap maps to nextStationId
        int targetIdx = -1;
        for (int i = 0; i < route.size(); i++) {
            String p = route.get(i);
            String station = nearbyStationMap.get(p);
            if (nextStationId.equals(station)) {
                targetIdx = i;
                break;
            }
        }
        if (targetIdx == -1) return 0;

        // Estimate current position: between prevNode and nextPlace
        String currentNextPlace = cache.getNextPlace();
        int currentNextIdx = indexOf(route, currentNextPlace);
        if (currentNextIdx == -1) currentNextIdx = 0;

        // Sum distances from current position to the target place
        double totalDistance = 0.0;
        // distance from "current position" to route[currentNextIdx] is the current distanceToNextPlace
        double distToNextPlace = Optional.ofNullable(cache.getDistanceToNextPlace()).orElse(0.0);
        totalDistance += distToNextPlace;

        // then add segment distances from currentNextIdx -> targetIdx-1 segments
        for (int idx = currentNextIdx; idx < targetIdx; idx++) {
            String a = route.get(idx);
            String b = route.get(idx + 1);
            double seg = getDistanceBetween(a, b, locationLocationMap);
            if (Double.isInfinite(seg)) {
                // abort and return 0
                return 0;
            }
            totalDistance += seg;
        }

        // convert to seconds: time = ceil(totalDistance / DISTANCE_PER_TICK * SECONDS_PER_TICK)
        long secs = (long) Math.ceil((totalDistance / DISTANCE_PER_TICK) * SECONDS_PER_TICK);
        return (int) secs;
    }

    // ---------- Normalization helpers ----------

    /**
     * Normalize the Redis 'drivers' value to a Map<Long, DriverCache> regardless of stored key/value shapes.
     * Accepts keys as Long/Integer/String and values as DriverCache or Map (convertible to DriverCache).
     */
    private Map<Long, DriverCache> normalizeDriverCache(Object raw) {
        log.info("Reached DriverService.normalizeDriverCache.");
        Map<Long, DriverCache> out = new HashMap<>();
        if (!(raw instanceof Map<?, ?> rawMap)) return out;
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Long id = parseLongKey(entry.getKey());
            if (id == null) continue;
            Object val = entry.getValue();
            DriverCache cache = null;
            if (val instanceof DriverCache) {
                cache = (DriverCache) val;
            } else if (val instanceof Map) {
                try {
                    cache = objectMapper.convertValue(val, DriverCache.class);
                } catch (IllegalArgumentException ex) {
                    log.warn("Failed to convert driver cache for id {}: {}", id, ex.getMessage());
                }
            }
            if (cache != null) out.put(id, cache);
        }
        return out;
    }

    private Long parseLongKey(Object key) {
        log.info("Reached DriverService.parseLongKey.");
        if (key instanceof Long l) return l;
        if (key instanceof Integer i) return i.longValue();
        if (key instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}