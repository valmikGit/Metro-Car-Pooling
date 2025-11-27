package com.metrocarpool.driver.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DriverCache Unit Tests")
class DriverCacheTest {

    @Test
    @DisplayName("Should create DriverCache using builder pattern")
    void testBuilderPattern() {
        // Given
        List<String> route = Arrays.asList("A", "B", "C");
        
        // When
        DriverCache cache = DriverCache.builder()
                .availableSeats(3)
                .routePlaces(route)
                .nextPlace("B")
                .timeToNextPlace(Duration.ofMinutes(10))
                .distanceToNextPlace(50.0)
                .finalDestination("C")
                .lastSeenMetroStation("Station1")
                .build();

        // Then
        assertThat(cache).isNotNull();
        assertThat(cache.getAvailableSeats()).isEqualTo(3);
        assertThat(cache.getRoutePlaces()).isEqualTo(route);
        assertThat(cache.getNextPlace()).isEqualTo("B");
        assertThat(cache.getTimeToNextPlace()).isEqualTo(Duration.ofMinutes(10));
        assertThat(cache.getDistanceToNextPlace()).isEqualTo(50.0);
        assertThat(cache.getFinalDestination()).isEqualTo("C");
        assertThat(cache.getLastSeenMetroStation()).isEqualTo("Station1");
    }

    @Test
    @DisplayName("Should create DriverCache with no-args constructor")
    void testNoArgsConstructor() {
        // When
        DriverCache cache = new DriverCache();

        // Then
        assertThat(cache).isNotNull();
        assertThat(cache.getAvailableSeats()).isNull();
        assertThat(cache.getRoutePlaces()).isNull();
    }

    @Test
    @DisplayName("Should create DriverCache with all-args constructor")
    void testAllArgsConstructor() {
        // Given
        List<String> route = Arrays.asList("A", "B", "C");
        Duration duration = Duration.ofMinutes(5);

        // When
        DriverCache cache = new DriverCache(
                2, route, "B", duration, 25.0, "C", "Station1"
        );

        // Then
        assertThat(cache.getAvailableSeats()).isEqualTo(2);
        assertThat(cache.getRoutePlaces()).isEqualTo(route);
        assertThat(cache.getNextPlace()).isEqualTo("B");
        assertThat(cache.getTimeToNextPlace()).isEqualTo(duration);
        assertThat(cache.getDistanceToNextPlace()).isEqualTo(25.0);
        assertThat(cache.getFinalDestination()).isEqualTo("C");
        assertThat(cache.getLastSeenMetroStation()).isEqualTo("Station1");
    }

    @Test
    @DisplayName("Should properly set and get all fields")
    void testSettersAndGetters() {
        // Given
        DriverCache cache = new DriverCache();
        List<String> route = Arrays.asList("X", "Y", "Z");
        Duration duration = Duration.ofMinutes(15);

        // When
        cache.setAvailableSeats(4);
        cache.setRoutePlaces(route);
        cache.setNextPlace("Y");
        cache.setTimeToNextPlace(duration);
        cache.setDistanceToNextPlace(75.0);
        cache.setFinalDestination("Z");
        cache.setLastSeenMetroStation("Station2");

        // Then
        assertThat(cache.getAvailableSeats()).isEqualTo(4);
        assertThat(cache.getRoutePlaces()).isEqualTo(route);
        assertThat(cache.getNextPlace()).isEqualTo("Y");
        assertThat(cache.getTimeToNextPlace()).isEqualTo(duration);
        assertThat(cache.getDistanceToNextPlace()).isEqualTo(75.0);
        assertThat(cache.getFinalDestination()).isEqualTo("Z");
        assertThat(cache.getLastSeenMetroStation()).isEqualTo("Station2");
    }

    @Test
    @DisplayName("Should handle equality correctly (Lombok @Data)")
    void testEquality() {
        // Given
        List<String> route = Arrays.asList("A", "B", "C");
        Duration duration = Duration.ofMinutes(10);

        DriverCache cache1 = DriverCache.builder()
                .availableSeats(3)
                .routePlaces(route)
                .nextPlace("B")
                .timeToNextPlace(duration)
                .distanceToNextPlace(50.0)
                .finalDestination("C")
                .lastSeenMetroStation("Station1")
                .build();

        DriverCache cache2 = DriverCache.builder()
                .availableSeats(3)
                .routePlaces(route)
                .nextPlace("B")
                .timeToNextPlace(duration)
                .distanceToNextPlace(50.0)
                .finalDestination("C")
                .lastSeenMetroStation("Station1")
                .build();

        // Then
        assertThat(cache1).isEqualTo(cache2);
        assertThat(cache1.hashCode()).isEqualTo(cache2.hashCode());
    }

    @Test
    @DisplayName("Should handle toString correctly (Lombok @Data)")
    void testToString() {
        // Given
        DriverCache cache = DriverCache.builder()
                .availableSeats(3)
                .routePlaces(Arrays.asList("A", "B"))
                .nextPlace("B")
                .finalDestination("B")
                .build();

        // When
        String toString = cache.toString();

        // Then
        assertThat(toString).contains("DriverCache");
        assertThat(toString).contains("availableSeats=3");
        assertThat(toString).contains("nextPlace=B");
    }
}
