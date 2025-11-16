package com.metrocarpool.matching.service;

import com.google.protobuf.Timestamp;
import com.metrocarpool.contracts.proto.DriverLocationEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.metrocarpool.contracts.proto.DriverRiderMatchEvent;
import com.metrocarpool.contracts.proto.RiderRequestDriverEvent;
import com.metrocarpool.matching.cache.MatchingDriverCache;
import com.metrocarpool.matching.cache.RiderWaitingQueueCache;
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
import java.util.List;
import java.util.HashMap;
import com.google.protobuf.util.Timestamps;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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

    // Thresholds (tune as required)
    private static final int DISTANCE_THRESHOLD_UNITS = 5;            // X units (distance)
    private static final long TIME_THRESHOLD_MS = 10 * 60 * 1000L;   // Y units (10 minutes)

    @KafkaListener(topics = "driver-updates", groupId = "matching-service")
    public void driverInfoUpdateCache(byte[] message, Acknowledgment ack) {
        try{
            log.info("Reached MatchingService.driverInfoUpdateCache.");

            DriverLocationEvent event = DriverLocationEvent.parseFrom(message);
            Long driverId = event.getDriverId();
            String oldStation = event.getOldStation();
            String nextStation = event.getNextStation();
            Duration timeToNextStation = Duration.ofSeconds(event.getTimeToNextStation());
            Integer availableSeats = event.getAvailableSeats();
            String finalDestination = event.getFinalDestination();
            // Acknowledge the message
            ack.acknowledge();
            // Update in Redis cache
            HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache =
                    (HashMap<String, HashMap<String, List<MatchingDriverCache>>>) redisDriverTemplate.opsForValue().get(MATCHING_DRIVER_CACHE_KEY);

            if (allMatchingCache == null) {
                allMatchingCache = new HashMap<>();
            }

            // Remove from old station
            if (!oldStation.isEmpty()) {
                HashMap<String, List<MatchingDriverCache>> matchingCache = allMatchingCache.get(oldStation);
                if (matchingCache != null && !finalDestination.isEmpty()) {
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
            if (!nextStation.isEmpty() && !finalDestination.isEmpty()) {
                HashMap<String, List<MatchingDriverCache>> matchingCache1 = allMatchingCache.get(nextStation);
                if (matchingCache1 == null) {
                    matchingCache1 = new HashMap<>();
                }
                List<MatchingDriverCache> matchingDriverCacheList1 = matchingCache1.get(finalDestination);
                if (matchingDriverCacheList1 == null) {
                    matchingDriverCacheList1 = new ArrayList<>();
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
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse DriverLocationEvent message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "rider-requests", groupId = "matching-service")
    public void riderInfoDriverMatchingAlgorithm(byte[] message,
                                                 Acknowledgment acknowledgment) {
        try{
            log.info("Reached MatchingService.riderInfoDriverMatchingAlgorithm.");

            RiderRequestDriverEvent tempEvent = RiderRequestDriverEvent.parseFrom(message);
            long riderId = tempEvent.getRiderId();
            String pickUpStation = tempEvent.getPickUpStation();
            com.google.protobuf.Timestamp arrivalTime = tempEvent.getArrivalTime();
            String destinationPlace = tempEvent.getDestinationPlace();
            acknowledgment.acknowledge();

            // Load caches from Redis
            HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache =
                    (HashMap<String, HashMap<String, List<MatchingDriverCache>>>) redisDriverTemplate.opsForValue().get(MATCHING_DRIVER_CACHE_KEY);

            if (allMatchingCache == null) {
                allMatchingCache = new HashMap<>();
            }

            HashMap<String, HashMap<String, Integer>> distances =
                    (HashMap<String, HashMap<String, Integer>>) redisDistancesHashMap.opsForValue().get(MATCHING_DISTANCE_KEY);

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

            if (!pickUpStation.isEmpty()) {
                HashMap<String, List<MatchingDriverCache>> stationMap = allMatchingCache.get(pickUpStation);
                if (stationMap != null && !stationMap.isEmpty()) {
                    // iterate only over driver destination keys in M
                    for (String driverDestination : stationMap.keySet()) {
                        // distance check: D[rider destination, driver destination] <= X
                        int distVal = Integer.MAX_VALUE;
                        if (distances != null && destinationPlace != null) {
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
                                    .setDriverId(chosenDriver.getDriverId())
                                    .setRiderId(riderId)
                                    .setPickUpStation(pickUpStation)
                                    .setDriverArrivalTime(driverArrivalTs)
                                    .build();

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
                Queue<RiderWaitingQueueCache> riderWaitingQueueCache =
                        (Queue<RiderWaitingQueueCache>) redisWaitingQueueTemplate.opsForValue().get(MATCHING_WAITING_QUEUE_KEY);
                if (riderWaitingQueueCache == null) {
                    riderWaitingQueueCache = new LinkedList<>();
                }
                riderWaitingQueueCache.add(RiderWaitingQueueCache.builder()
                        .riderId(riderId)
                        .arrivalTime(arrivalTime != null ? arrivalTime : Timestamps.fromMillis(System.currentTimeMillis()))
                        .destinationPlace(destinationPlace)
                        .pickUpStation(pickUpStation)
                        .build()
                );
                redisWaitingQueueTemplate.opsForValue().set(MATCHING_WAITING_QUEUE_KEY, riderWaitingQueueCache);
            }
            
        }catch (InvalidProtocolBufferException e){
            log.error("Failed to parse RiderRequestDriverEvent protobuf message", e);
        }
        
    }

    @Scheduled(cron = "* * * * * *")
    public void cronJobMatchingAlgorithm() {
        log.info("Reached MatchingService.cronJobMatchingAlgorithm.");

        // Run this CRON job every second to check whether there is a driver for the riders in the waiting queue => pop the first element from the queue
        // Load caches from Redis
        HashMap<String, HashMap<String, List<MatchingDriverCache>>> allMatchingCache =
                (HashMap<String, HashMap<String, List<MatchingDriverCache>>>) redisDriverTemplate.opsForValue().get(MATCHING_DRIVER_CACHE_KEY);

        if (allMatchingCache == null) {
            allMatchingCache = new HashMap<>();
        }

        HashMap<String, HashMap<String, Integer>> distances =
                (HashMap<String, HashMap<String, Integer>>) redisDistancesHashMap.opsForValue().get(MATCHING_DISTANCE_KEY);

        Queue<RiderWaitingQueueCache> riderWaitingQueueCache =
                (Queue<RiderWaitingQueueCache>) redisWaitingQueueTemplate.opsForValue().get(MATCHING_WAITING_QUEUE_KEY);

        if (riderWaitingQueueCache == null || riderWaitingQueueCache.isEmpty()) {
            // nothing to do in this cron tick
            return;
        }

        RiderWaitingQueueCache rider = riderWaitingQueueCache.poll(); // pop first element
        if (rider == null) {
            return;
        }

        boolean matched = false;
        MatchingDriverCache chosenDriver;
        String chosenDriverStation = null;
        String chosenDriverDestination = null;

        long riderMillis = 0L;
        try {
            if (rider.getArrivalTime() != null) {
                riderMillis = Timestamps.toMillis(rider.getArrivalTime());
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
                    if (distances != null && destinationPlace != null) {
                        HashMap<String, Integer> inner = distances.get(destinationPlace);
                        if (inner != null && inner.containsKey(driverDestination)) {
                            Integer dv = inner.get(driverDestination);
                            if (dv != null) distVal = dv;
                        }
                    }
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

                        DriverRiderMatchEvent event = DriverRiderMatchEvent.newBuilder()
                                .setDriverId(chosenDriver.getDriverId())
                                .setRiderId(rider.getRiderId())
                                .setPickUpStation(pickUpStation)
                                .setDriverArrivalTime(driverArrivalTs)
                                .build();
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
            riderWaitingQueueCache.add(rider);
        }

        // update waiting queue in redis
        redisWaitingQueueTemplate.opsForValue().set(MATCHING_WAITING_QUEUE_KEY, riderWaitingQueueCache);
    }
}