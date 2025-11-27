package com.metrocarpool.rider.service;

import com.google.protobuf.Timestamp;
import com.metrocarpool.contracts.proto.RiderRequestDriverEvent;
import com.metrocarpool.rider.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiderService Unit Tests")
class RiderServiceUnitTest {

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private RiderService riderService;

    private static final String RIDER_TOPIC = "rider-requests-test";

    @BeforeEach
    void setUp() {
        riderService = new RiderService(kafkaTemplate);
        ReflectionTestUtils.setField(riderService, "RIDER_TOPIC", RIDER_TOPIC);
    }

    @Test
    @DisplayName("processRiderInfo - Should successfully publish rider request to Kafka")
    void processRiderInfo_Success() {
        // Given
        Long riderId = 1L;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class))).thenReturn(future);

        // When
        boolean result = riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        assertThat(result).isTrue();
        verify(kafkaTemplate).send(eq(RIDER_TOPIC), eq(riderId.toString()), any(byte[].class));
    }

    @Test
    @DisplayName("processRiderInfo - Should construct event with correct fields")
    void processRiderInfo_ConstructsCorrectEvent() throws Exception {
        // Given
        Long riderId = 2L;
        String pickUpStation = "StationB";
        String destinationPlace = "LocationD";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
        CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), eventCaptor.capture())).thenReturn(future);

        // When
        riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        byte[] capturedEvent = eventCaptor.getValue();
        RiderRequestDriverEvent event = RiderRequestDriverEvent.parseFrom(capturedEvent);
        
        assertThat(event.getRiderId()).isEqualTo(riderId);
        assertThat(event.getPickUpStation()).isEqualTo(pickUpStation);
        assertThat(event.getDestinationPlace()).isEqualTo(destinationPlace);
        assertThat(event.getArrivalTime()).isEqualTo(arrivalTime);
        assertThat(event.getMessageId()).isNotEmpty();
    }

    @Test
    @DisplayName("processRiderInfo - Should handle null rider ID gracefully")
    void processRiderInfo_HandlesNullRiderId() {
        // Given
        Long riderId = null;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        // When
        boolean result = riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        assertThat(result).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("processRiderInfo - Should handle null pickup station gracefully")
    void processRiderInfo_HandlesNullPickupStation() {
        // Given
        Long riderId = 3L;
        String pickUpStation = null;
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        // When
        boolean result = riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        assertThat(result).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("processRiderInfo - Should handle null destination gracefully")
    void processRiderInfo_HandlesNullDestination() {
        // Given
        Long riderId = 4L;
        String pickUpStation = "StationA";
        String destinationPlace = null;
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        // When
        boolean result = riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        assertThat(result).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("processRiderInfo - Should handle null arrival time gracefully")
    void processRiderInfo_HandlesNullArrivalTime() {
        // Given
        Long riderId = 5L;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = null;

        // When
        boolean result = riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        assertThat(result).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("processRiderInfo - Should handle Kafka publishing failure")
    void processRiderInfo_HandlesKafkaFailure() {
        // Given
        Long riderId = 6L;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        CompletableFuture<SendResult<String, byte[]>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class))).thenReturn(future);

        // When
        boolean result = riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        // Should still return true as the method doesn't wait for future completion
        assertThat(result).isTrue();
        verify(kafkaTemplate).send(eq(RIDER_TOPIC), eq(riderId.toString()), any(byte[].class));
    }

    @Test
    @DisplayName("processRiderInfo - Should use riderId as Kafka message key")
    void processRiderInfo_UsesRiderIdAsKey() {
        // Given
        Long riderId = 7L;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), keyCaptor.capture(), any(byte[].class))).thenReturn(future);

        // When
        riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        String capturedKey = keyCaptor.getValue();
        assertThat(capturedKey).isEqualTo(riderId.toString());
    }

    @Test
    @DisplayName("processRiderInfo - Should publish to correct topic")
    void processRiderInfo_PublishesToCorrectTopic() {
        // Given
        Long riderId = 8L;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(topicCaptor.capture(), anyString(), any(byte[].class))).thenReturn(future);

        // When
        riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);

        // Then
        String capturedTopic = topicCaptor.getValue();
        assertThat(capturedTopic).isEqualTo(RIDER_TOPIC);
    }

    @Test
    @DisplayName("processRiderInfo - Should generate unique message IDs for different requests")
    void processRiderInfo_GeneratesUniqueMessageIds() throws Exception {
        // Given
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();
        ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
        CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), eventCaptor.capture())).thenReturn(future);

        // When
        riderService.processRiderInfo(1L, "StationA", "LocationC", arrivalTime);
        riderService.processRiderInfo(2L, "StationB", "LocationD", arrivalTime);

        // Then
        assertThat(eventCaptor.getAllValues()).hasSize(2);
        RiderRequestDriverEvent event1 = RiderRequestDriverEvent.parseFrom(eventCaptor.getAllValues().get(0));
        RiderRequestDriverEvent event2 = RiderRequestDriverEvent.parseFrom(eventCaptor.getAllValues().get(1));
        
        assertThat(event1.getMessageId()).isNotEqualTo(event2.getMessageId());
    }
}
