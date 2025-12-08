package com.metrocarpool.matching.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import com.metrocarpool.contracts.proto.DriverLocationEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.metrocarpool.contracts.proto.RiderRequestDriverEvent;
import com.metrocarpool.matching.cache.MatchingDriverCache;
import com.metrocarpool.matching.cache.RiderWaitingQueueCache;
import com.metrocarpool.matching.redislock.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;
import java.time.Duration;
import java.util.*;

import com.google.protobuf.util.Timestamps;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    @Value("${kafka.topics.rider-driver-match}")
    private String MATCHING_TOPIC;

    private final RedisTemplate<String, Object> redisDriverTemplate;
    private static final String MATCHING_DRIVER_CACHE_KEY = "driver-cache";
    private final RedisTemplate<String, Object> redisWaitingQueueTemplate;
    private static final String MATCHING_WAITING_QUEUE_KEY = "rider-waiting-queue";
    private final RedisTemplate<String, Object> redisDistancesHashMap;
    private static final String MATCHING_DISTANCE_KEY = "distance";

    // Redis Distributed Lock
    private final RedisDistributedLock redisDistributedLock;
    private static final String redisDriverLockKey = "lock:drivers";
    private static final String redisWaitingQueueLockKey = "lock:rider-waiting-queue";
    private static final String redisDistanceLockKey = "lock:distance";

    // Redis usage to ensure Kafka consumer idempotency
    private static final String DRIVER_UPDATE_KAFKA_DEDUP_KEY_PREFIX = "driver_update_processed_kafka_msg:";
    private static final String RIDER_REQUEST_KAFKA_DEDUP_KEY_PREFIX = "rider_request_processed_kafka_msg:";

    // String template + mapper for tolerant reads of plain JSON (no @class)
    private final RedisTemplate<String, String> redisStringTemplate;
    private final ObjectMapper objectMapper;

    // Thresholds (tune as required)
    private static final int DISTANCE_THRESHOLD_UNITS = 5;            // X units (distance)
    private static final long TIME_THRESHOLD_MS = 10 * 60 * 1000L;   // Y units (10 minutes)

    private String tryAcquireLockWithRetry(String lockKey) {
        for (int attempt = 1; attempt <= 10; attempt++) {
            String lockValue = redisDistributedLock.acquireLock(lockKey, 5000);
            if (lockValue != null) {
                return lockValue;  // success
            }

            log.warn("Lock not acquired for key {}. Attempt {}/{}. Retrying in {} ms...",
                    lockKey, attempt, 10, (long) 200);

            try {
                Thread.sleep((long) 200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null; // all retries failed
    }

    private boolean alreadyProcessed(String topicDedupKey, String messageId) {
        if (messageId == null) return false;

        String redisKey = topicDedupKey + messageId;

        return redisStringTemplate.hasKey(redisKey);
    }

    private void markProcessed(String topicDedupKey, String messageId) {
        if (messageId == null) return;

        String redisKey = topicDedupKey + messageId;

        // store marker with 24-hour TTL
        redisStringTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
    }

    // -----------------------
    // Helper methods to ensure nested caches exist
    // -----------------------

    /**
     * Ensure top-level allMatchingCache is non-null; if null return new empty map.
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, HashMap<String, List<MatchingDriverCache>>> ensureAllMatchingCache() {
        HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache =
                (HashMap<String, HashMap<String, List<MatchingDriverCache>>>) redisDriverTemplate.opsForValue().get(MATCHING_DRIVER_CACHE_KEY);
        if (allMatchingCache == null) {
            allMatchingCache = new HashMap<>();
        }
        return allMatchingCache;
    }

    /**
     * Get or create the station map for a station key.
     * Returns the station map (String -> List<MatchingDriverCache>) and ensures it is put in allMatchingCache.
     */
    private HashMap<String, List<MatchingDriverCache>> getOrCreateStationMap(
            HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache,
            String stationKey) {
        HashMap<String, List<MatchingDriverCache>> stationMap = allMatchingCache.get(stationKey);
        if (stationMap == null) {
            stationMap = new HashMap<>();
            allMatchingCache.put(stationKey, stationMap);
        }
        return stationMap;
    }

    /**
     * Get or create the driver list (for a destination under a station).
     */
    private List<MatchingDriverCache> getOrCreateDriverList(
            HashMap<String, List<MatchingDriverCache>> stationMap,
            String destinationKey) {
        List<MatchingDriverCache> list = stationMap.get(destinationKey);
        if (list == null) {
            list = new ArrayList<>();
            stationMap.put(destinationKey, list);
        }
        return list;
    }

    /**
     * Ensure distances map is non-null.
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, HashMap<String, Integer>> ensureDistancesMap() {
        HashMap<String, HashMap<String, Integer>> distances =
                (HashMap<String, HashMap<String, Integer>>) redisDistancesHashMap.opsForValue().get(MATCHING_DISTANCE_KEY);
        if (distances == null) {
            distances = new HashMap<>();
        }
        return distances;
    }

    /**
     * Ensure waiting queue is non-null.
     */
    private Queue<RiderWaitingQueueCache> ensureWaitingQueue() {
        Object rawQueue = redisWaitingQueueTemplate.opsForValue().get(MATCHING_WAITING_QUEUE_KEY);
        Queue<RiderWaitingQueueCache> riderWaitingQueueCache = new LinkedList<>();

        if (rawQueue instanceof List) {
            List<?> list = (List<?>) rawQueue;
            for (Object item : list) {
                try {
                    RiderWaitingQueueCache cacheItem;
                    if (item instanceof String) {
                        cacheItem = objectMapper.readValue((String) item, RiderWaitingQueueCache.class);
                    } else {
                        cacheItem = objectMapper.convertValue(item, RiderWaitingQueueCache.class);
                    }
                    riderWaitingQueueCache.add(cacheItem);
                } catch (Exception e) {
                    log.error("Failed to convert item to RiderWaitingQueueCache: {}", item, e);
                }
            }
        }
        return riderWaitingQueueCache;
    }

    // -----------------------
    // Kafka listeners and scheduled job
    // -----------------------

    @KafkaListener(topics = "${kafka.topics.driver-location-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void driverInfoUpdateCache(byte[] message, Acknowledgment ack) {
        // Try to acquire lock
        String lockValue = tryAcquireLockWithRetry(redisDriverLockKey);
        if (lockValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisDriverLockKey, 5000, 10, 200);
            return;
        } else {
            log.info("Acquired lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning false", redisDriverLockKey, 5000, 10, 200);
        }

        try{
            log.debug("Reached MatchingService.driverInfoUpdateCache.");

            DriverLocationEvent event = DriverLocationEvent.parseFrom(message);
            String messageId = event.getMessageId();
            if (alreadyProcessed(DRIVER_UPDATE_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("MatchingService.driverInfoUpdateCache: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                ack.acknowledge();
                return;
            }

            Long driverId = event.getDriverId();
            String oldStation = event.getOldStation();
            String nextStation = event.getNextStation();
            Duration timeToNextStation = Duration.ofSeconds(event.getTimeToNextStation());
            Integer availableSeats = event.getAvailableSeats();
            String finalDestination = event.getFinalDestination();

            // Acknowledge the message
            markProcessed(DRIVER_UPDATE_KAFKA_DEDUP_KEY_PREFIX, messageId);
            ack.acknowledge();

            // Update in Redis cache - ensure top-level map exists
            HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache = ensureAllMatchingCache();

            // Remove from old station
            if (oldStation != null && !oldStation.isEmpty()) {
                HashMap<String, List<MatchingDriverCache>> matchingCache = allMatchingCache.get(oldStation);
                if (matchingCache != null && finalDestination != null && !finalDestination.isEmpty()) {
                    List<MatchingDriverCache> matchingDriverCacheList = matchingCache.get(finalDestination);
                    if (matchingDriverCacheList != null) {
                        matchingDriverCacheList.removeIf(matchingDriverCache1 -> Objects.equals(matchingDriverCache1.getDriverId(), driverId));
                        // if list empty remove the key
                        if (matchingDriverCacheList.isEmpty()) {
                            matchingCache.remove(finalDestination);
                        } else {
                            matchingCache.put(finalDestination, matchingDriverCacheList);
                        }
                        allMatchingCache.put(oldStation, matchingCache);
                        redisDriverTemplate.opsForValue().set(MATCHING_DRIVER_CACHE_KEY, allMatchingCache);
                    }
                }
            }

            // Add in new station
            if (nextStation != null && !nextStation.isEmpty() && finalDestination != null && !finalDestination.isEmpty()) {
                // ensure station map and list exist
                HashMap<String, List<MatchingDriverCache>> matchingCache1 = allMatchingCache.get(nextStation);
                if (matchingCache1 == null) {
                    matchingCache1 = new HashMap<>();
                    allMatchingCache.put(nextStation, matchingCache1);
                }
                List<MatchingDriverCache> matchingDriverCacheList1 = matchingCache1.get(finalDestination);
                if (matchingDriverCacheList1 == null) {
                    matchingDriverCacheList1 = new ArrayList<>();
                    matchingCache1.put(finalDestination, matchingDriverCacheList1);
                }
                matchingDriverCacheList1.add(MatchingDriverCache.builder()
                        .driverId(driverId)
                        .timeToReachStation(timeToNextStation)
                        .availableSeats(availableSeats)
                        .build()
                );
                matchingCache1.put(finalDestination, matchingDriverCacheList1);
                allMatchingCache.put(nextStation, matchingCache1);
                redisDriverTemplate.opsForValue().set(MATCHING_DRIVER_CACHE_KEY, allMatchingCache);
            } else {
                log.info("Skipping cache update for driver {}: nextStation or finalDestination is empty. nextStation={}, finalDestination={}",
                        driverId, nextStation, finalDestination);
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse DriverLocationEvent message: {}", e.getMessage());
        } finally {
            redisDistributedLock.releaseLock(redisDriverLockKey, lockValue);
        }
    }

    @KafkaListener(topics = "${kafka.topics.rider-requests}", groupId = "${spring.kafka.consumer.group-id}")
    public void riderInfoDriverMatchingAlgorithm(byte[] message,
                                                 Acknowledgment acknowledgment) {
        // Try to acquire lock
        String lockDriverValue = tryAcquireLockWithRetry(redisDriverLockKey);
        if (lockDriverValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisDriverLockKey, 5000, 10, 200);
            return;
        }

        // Try to acquire lock
        String lockDistanceValue = tryAcquireLockWithRetry(redisDistanceLockKey);
        if (lockDistanceValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisDistanceLockKey, 5000, 10, 200);
            return;
        }

        try {
            log.debug("Reached MatchingService.riderInfoDriverMatchingAlgorithm.");
            RiderRequestDriverEvent tempEvent = RiderRequestDriverEvent.parseFrom(message);
            String messageId = tempEvent.getMessageId();

            if (alreadyProcessed(RIDER_REQUEST_KAFKA_DEDUP_KEY_PREFIX, messageId)) {
                log.info("MatchingService.riderInfoDriverMatchingALgorithm: Duplicate Kafka message detected. Skipping. messageId={}",
                        messageId);
                acknowledgment.acknowledge();
                return;
            }

            long riderId = tempEvent.getRiderId();
            String pickUpStation = tempEvent.getPickUpStation();
            com.google.protobuf.Timestamp arrivalTime = tempEvent.getArrivalTime();
            String destinationPlace = tempEvent.getDestinationPlace();

            // Acknowledge the message
            markProcessed(RIDER_REQUEST_KAFKA_DEDUP_KEY_PREFIX, messageId);
            acknowledgment.acknowledge();

            // Load caches from Redis (ensure initialization)
            HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache = ensureAllMatchingCache();

            HashMap<String, HashMap<String, Integer>> distances = ensureDistancesMap();

            // Candidate pool priority queue ordered by driver's timeToReachStation (smaller first)
            PriorityQueue<MatchingDriverCache> pq = new PriorityQueue<>(Comparator.comparingLong(d ->
                    d.getTimeToReachStation() == null ? Long.MAX_VALUE : d.getTimeToReachStation().toMillis()));

            long riderMillis = 0L;
            try {
                riderMillis = Timestamps.toMillis(arrivalTime);
            } catch (Exception ex) {
                riderMillis = System.currentTimeMillis();
            }

            boolean matched = false;
            MatchingDriverCache chosenDriver;
            String chosenDriverStation = null;
            String chosenDriverDestination = null;

            if (pickUpStation != null && !pickUpStation.isEmpty()) {
                HashMap<String, List<MatchingDriverCache>> stationMap = allMatchingCache.get(pickUpStation);
                if (stationMap != null && !stationMap.isEmpty()) {
                    // iterate only over driver destination keys in M
                    for (String driverDestination : stationMap.keySet()) {
                        // distance check: D[rider destination, driver destination] <= X
                        int distVal = Integer.MAX_VALUE;
                        if (destinationPlace != null && destinationPlace.equals(driverDestination)) {
                            distVal = 0;
                        } else if (distances != null && destinationPlace != null) {
                            HashMap<String, Integer> inner = distances.get(destinationPlace);
                            if (inner != null && inner.containsKey(driverDestination)) {
                                Integer dv = inner.get(driverDestination);
                                if (dv != null) distVal = dv;
                            }
                        }
                        if (distVal <= DISTANCE_THRESHOLD_UNITS) {
                            // add all drivers for this driverDestination to candidate pool
                            List<MatchingDriverCache> driversAtDest = stationMap.get(driverDestination);
                            if (driversAtDest != null) {
                                for (MatchingDriverCache driverCache : driversAtDest) {
                                    // Condition a (Time Filter): |Rider arrival time - Driver arrival time| <= Y
                                    long driverArrivalMillis = System.currentTimeMillis();
                                    try {
                                        if (driverCache.getTimeToReachStation() != null) {
                                            driverArrivalMillis = System.currentTimeMillis() + driverCache.getTimeToReachStation().toMillis();
                                        }
                                    } catch (Exception ex) {
                                        driverArrivalMillis = System.currentTimeMillis();
                                    }
                                    long diff = Math.abs(riderMillis - driverArrivalMillis);
                                    if (diff <= TIME_THRESHOLD_MS) {
                                        pq.add(driverCache);
                                    }
                                }
                            }
                        }
                    }

                    // pop top driver from PQ
                    if (!pq.isEmpty()) {
                        chosenDriver = pq.poll();
                        // Need to find chosen driver's destination key and station list to remove later
                        outer_loop:
                        for (String destKey : stationMap.keySet()) {
                            List<MatchingDriverCache> list = stationMap.get(destKey);
                            if (list != null) {
                                for (MatchingDriverCache mdc : list) {
                                    if (Objects.equals(mdc.getDriverId(), chosenDriver.getDriverId())) {
                                        chosenDriverDestination = destKey;
                                        chosenDriverStation = pickUpStation;
                                        break outer_loop;
                                    }
                                }
                            }
                        }
                        // If matched, build and send Kafka event and remove the driver from cache
                        if (chosenDriver != null) {
                            long driverArrivalMillis = System.currentTimeMillis();
                            if (chosenDriver.getTimeToReachStation() != null) {
                                driverArrivalMillis = System.currentTimeMillis() + chosenDriver.getTimeToReachStation().toMillis();
                            }
                            Timestamp driverArrivalTs = Timestamps.fromMillis(driverArrivalMillis);

                            DriverRiderMatchEvent event = DriverRiderMatchEvent.newBuilder()
                                    .setMessageId(UUID.randomUUID().toString())
                                    .setDriverId(chosenDriver.getDriverId())
                                    .setRiderId(riderId)
                                    .setPickUpStation(pickUpStation)
                                    .setDriverArrivalTime(driverArrivalTs)
                                    .build();

                            log.info("Matching: Rider = {} and driver = {} matched.", riderId, chosenDriver.getDriverId());

                            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(MATCHING_TOPIC,
                                    String.valueOf(riderId) , event.toByteArray());
                            future.thenAccept(result -> {
                                log.debug("Event = {} delivered to {}", event, result.getRecordMetadata().topic());
                            }).exceptionally(ex -> {
                                log.error("Event failed. Error message = {}", ex.getMessage());
                                // Optional: retry, put into Redis dead-letter queue
                                return null;
                            });
                            matched = true;

                            // remove matched driver from allMatchingCache
                            if (chosenDriverStation != null && chosenDriverDestination != null) {
                                HashMap<String, List<MatchingDriverCache>> stationMap2 = allMatchingCache.get(chosenDriverStation);
                                if (stationMap2 != null) {
                                    List<MatchingDriverCache> driverList = stationMap2.get(chosenDriverDestination);
                                    if (driverList != null) {
                                        driverList.removeIf(mdc -> Objects.equals(mdc.getDriverId(), chosenDriver.getDriverId()));
                                        if (driverList.isEmpty()) {
                                            stationMap2.remove(chosenDriverDestination);
                                        } else {
                                            stationMap2.put(chosenDriverDestination, driverList);
                                        }
                                        allMatchingCache.put(chosenDriverStation, stationMap2);
                                        redisDriverTemplate.opsForValue().set(MATCHING_DRIVER_CACHE_KEY, allMatchingCache);
                                    }
                                }
                            }
                        }
                    } else {
                        chosenDriver = null;
                    }
                } else {
                    chosenDriver = null;
                }
            } else {
                chosenDriver = null;
            }

            // If no match found, push the rider into the waiting queue (as earlier)
            if (!matched) {
                Queue<RiderWaitingQueueCache> riderWaitingQueueCache = ensureWaitingQueue();

                riderWaitingQueueCache.add(RiderWaitingQueueCache.builder()
                        .riderId(riderId)
                        .pickUpStation(pickUpStation)
                        .arrivalTime(Timestamps.toMillis(arrivalTime))
                        .destinationPlace(destinationPlace)
                        .build()
                );

                log.debug("Rider waiting queue: Rider added to waiting queue.");
                redisWaitingQueueTemplate.opsForValue().set(MATCHING_WAITING_QUEUE_KEY, riderWaitingQueueCache);
            }
        } catch (InvalidProtocolBufferException e){
            log.error("Failed to parse RiderRequestDriverEvent protobuf message", e);
        } finally {
            redisDistributedLock.releaseLock(redisDriverLockKey, lockDriverValue);
            redisDistributedLock.releaseLock(redisDistanceLockKey, lockDistanceValue);
        }
    }

    @Scheduled(cron = "* * * * * *") 
    public void cronJobMatchingAlgorithm() {
        // Try to acquire lock
        String lockDriverValue = tryAcquireLockWithRetry(redisDriverLockKey);
        if (lockDriverValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisDriverLockKey, 5000, 10, 200);
            return;
        }

        // Try to acquire lock
        String lockDistanceValue = tryAcquireLockWithRetry(redisDistanceLockKey);
        if (lockDistanceValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisDistanceLockKey, 5000, 10, 200);
            return;
        }

        // Try to acqiure lock
        String lockWaitingQueueValue = tryAcquireLockWithRetry(redisWaitingQueueLockKey);
        if (lockWaitingQueueValue == null) {
            log.error("Unable to acquire lock with retry policy: {} lock key {} timeout milliseconds {} maximum retries {} back off milliseconds. " +
                    "Returning void.", redisWaitingQueueLockKey, 5000, 10, 200);
        }

        try {

            // Run this CRON job every second to check whether there is a driver for the riders in the waiting queue => pop the first element from the queue
            // Load caches from Redis (ensure initialization)
            HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache = ensureAllMatchingCache();

            HashMap<String, HashMap<String, Integer>> distances = ensureDistancesMap();

            Queue<RiderWaitingQueueCache> riderWaitingQueueCache = ensureWaitingQueue();

            if (riderWaitingQueueCache == null || riderWaitingQueueCache.isEmpty()) {
                // nothing to do in this cron tick - no logging to avoid spam
                return;
            }

            // Log meaningful info about waiting queue
            log.debug("Cron job: Processing waiting queue with {} rider(s) pending", riderWaitingQueueCache.size());

            RiderWaitingQueueCache rider = riderWaitingQueueCache.poll(); // pop first element
            if (rider == null) {
                return;
            }

            log.info("Cron job: Attempting to match rider {} from {} to {}", 
                    rider.getRiderId(), rider.getPickUpStation(), rider.getDestinationPlace());


            boolean matched = false;
            MatchingDriverCache chosenDriver;
            String chosenDriverStation = null;
            String chosenDriverDestination = null;

            long riderMillis = 0L;
            try {
                if (rider.getArrivalTime() != null) {
                    riderMillis = rider.getArrivalTime();
                } else {
                    riderMillis = System.currentTimeMillis();
                }
            } catch (Exception ex) {
                riderMillis = System.currentTimeMillis();
            }

            String pickUpStation = rider.getPickUpStation();
            String destinationPlace = rider.getDestinationPlace();

            if (pickUpStation != null && !pickUpStation.isEmpty()) {
                HashMap<String, List<MatchingDriverCache>> stationMap = allMatchingCache.get(pickUpStation);
                if (stationMap != null && !stationMap.isEmpty()) {
                    PriorityQueue<MatchingDriverCache> pq = new PriorityQueue<>(Comparator.comparingLong(d ->
                            d.getTimeToReachStation() == null ? Long.MAX_VALUE : d.getTimeToReachStation().toMillis()));

                    // Build candidate pool same as in rider handler
                    for (String driverDestination : stationMap.keySet()) {
                        int distVal = Integer.MAX_VALUE;
                        if (destinationPlace != null && destinationPlace.equals(driverDestination)) {
                            distVal = 0;
                        } else if (distances != null && destinationPlace != null) {
                            HashMap<String, Integer> inner = distances.get(destinationPlace);
                            if (inner != null && inner.containsKey(driverDestination)) {
                                Integer dv = inner.get(driverDestination);
                                if (dv != null) distVal = dv;
                            }
                        }
                        
                        // log.info("Checking driverDest: {}, riderDest: {}, distance: {}", driverDestination, destinationPlace, distVal);

                        if (distVal <= DISTANCE_THRESHOLD_UNITS) {
                            List<MatchingDriverCache> driversAtDest = stationMap.get(driverDestination);
                            if (driversAtDest != null) {
                                for (MatchingDriverCache driverCache : driversAtDest) {
                                    long driverArrivalMillis = System.currentTimeMillis();
                                    try {
                                        if (driverCache.getTimeToReachStation() != null) {
                                            driverArrivalMillis = System.currentTimeMillis() + driverCache.getTimeToReachStation().toMillis();
                                        }
                                    } catch (Exception ex) {
                                        driverArrivalMillis = System.currentTimeMillis();
                                    }
                                    long diff = Math.abs(riderMillis - driverArrivalMillis);
                                    
                                    // log.info("Driver {}: arrival={}, rider={}, diff={}, threshold={}", 
                                    //        driverCache.getDriverId(), driverArrivalMillis, riderMillis, diff, TIME_THRESHOLD_MS);

                                    if (diff <= TIME_THRESHOLD_MS) {
                                        pq.add(driverCache);
                                    }
                                }
                            }
                        }
                    }

                    if (!pq.isEmpty()) {
                        chosenDriver = pq.poll();
                        outer_loop:
                        for (String destKey : stationMap.keySet()) {
                            List<MatchingDriverCache> list = stationMap.get(destKey);
                            if (list != null) {
                                for (MatchingDriverCache mdc : list) {
                                    if (Objects.equals(mdc.getDriverId(), chosenDriver.getDriverId())) {
                                        chosenDriverDestination = destKey;
                                        chosenDriverStation = pickUpStation;
                                        break outer_loop;
                                    }
                                }
                            }
                        }

                        if (chosenDriver != null) {
                            long driverArrivalMillis = System.currentTimeMillis();
                            if (chosenDriver.getTimeToReachStation() != null) {
                                driverArrivalMillis = System.currentTimeMillis() + chosenDriver.getTimeToReachStation().toMillis();
                            }
                            Timestamp driverArrivalTs = Timestamps.fromMillis(driverArrivalMillis);

                            log.info("Rider waiting queue: Rider popped from waiting queue.");

                            DriverRiderMatchEvent event = DriverRiderMatchEvent.newBuilder()
                                    .setMessageId(UUID.randomUUID().toString())
                                    .setDriverId(chosenDriver.getDriverId())
                                    .setRiderId(rider.getRiderId())
                                    .setPickUpStation(pickUpStation)
                                    .setDriverArrivalTime(driverArrivalTs)
                                    .build();

                            log.info("Matching: Rider = {} and driver = {} matched.", rider.getRiderId(), chosenDriver.getDriverId());

                            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(MATCHING_TOPIC,
                                    String.valueOf(event.getDriverId() + event.getRiderId()), event.toByteArray());
                            future.thenAccept(result -> {
                                log.debug("Event = {} delivered to {}", event, result.getRecordMetadata().topic());
                            }).exceptionally(ex -> {
                                log.error("Event failed. Error message = {}", ex.getMessage());
                                // Optional: retry, put into Redis dead-letter queue
                                return null;
                            });
                            matched = true;

                            // remove matched driver from allMatchingCache
                            if (chosenDriverStation != null && chosenDriverDestination != null) {
                                HashMap<String, List<MatchingDriverCache>> stationMap2 = allMatchingCache.get(chosenDriverStation);
                                if (stationMap2 != null) {
                                    List<MatchingDriverCache> driverList = stationMap2.get(chosenDriverDestination);
                                    if (driverList != null) {
                                        driverList.removeIf(mdc -> Objects.equals(mdc.getDriverId(), chosenDriver.getDriverId()));
                                        if (driverList.isEmpty()) {
                                            stationMap2.remove(chosenDriverDestination);
                                        } else {
                                            stationMap2.put(chosenDriverDestination, driverList);
                                        }
                                        allMatchingCache.put(chosenDriverStation, stationMap2);
                                        redisDriverTemplate.opsForValue().set(MATCHING_DRIVER_CACHE_KEY, allMatchingCache);
                                    }
                                }
                            }
                        }
                    } else {
                        chosenDriver = null;
                    }
                } else {
                    chosenDriver = null;
                }
            } else {
                chosenDriver = null;
            }

            // If not matched, push rider back to waiting queue (end of queue)
            if (!matched) {
                rider.setArrivalTime(System.currentTimeMillis());
                riderWaitingQueueCache.add(rider);
                log.info("Cron job: No driver match found for rider {}. Re-added to waiting queue (queue size: {})", 
                        rider.getRiderId(), riderWaitingQueueCache.size());
            }


            // update waiting queue in redis
            redisWaitingQueueTemplate.opsForValue().set(MATCHING_WAITING_QUEUE_KEY, riderWaitingQueueCache);
        } catch (Exception e) {
            log.error("Error = {}.", e.getMessage());
        } finally {
            redisDistributedLock.releaseLock(redisDriverLockKey, lockDriverValue);
            redisDistributedLock.releaseLock(redisDistanceLockKey, lockDistanceValue);
            redisDistributedLock.releaseLock(redisWaitingQueueLockKey, lockWaitingQueueValue);
        }
    }
}