package com.metrocarpool.rider.grpc;

import com.google.protobuf.Timestamp;
import com.metrocarpool.rider.proto.PostRider;
import com.metrocarpool.rider.proto.RiderStatusResponse;
import com.metrocarpool.rider.service.RiderService;
import com.metrocarpool.rider.util.TestDataBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiderGrpcServer Unit Tests")
class RiderGrpcServerUnitTest {

    @Mock
    private RiderService riderService;

    @Mock
    private StreamObserver<RiderStatusResponse> responseObserver;

    private RiderGrpcServer riderGrpcServer;

    @BeforeEach
    void setUp() {
        riderGrpcServer = new RiderGrpcServer(riderService);
    }

    @Test
    @DisplayName("postRiderInfo - Should successfully process rider request")
    void postRiderInfo_Success() {
        // Given
        Long riderId = 1L;
        String pickUpStation = "StationA";
        String destinationPlace = "LocationC";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();
        
        PostRider request = TestDataBuilder.buildPostRiderRequest(riderId, pickUpStation, destinationPlace, arrivalTime);
        
        when(riderService.processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime))
                .thenReturn(true);

        // When
        riderGrpcServer.postRiderInfo(request, responseObserver);

        // Then
        verify(riderService).processRiderInfo(riderId, pickUpStation, destinationPlace, arrivalTime);
        verify(responseObserver).onNext(any(RiderStatusResponse.class));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    @DisplayName("postRiderInfo - Should return success status when service succeeds")
    void postRiderInfo_ReturnsSuccessStatus() {
        // Given
        PostRider request = TestDataBuilder.buildPostRiderRequest(1L, "StationA", "LocationC", TestDataBuilder.buildTimestamp());
        when(riderService.processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(true);

        ArgumentCaptor<RiderStatusResponse> responseCaptor = ArgumentCaptor.forClass(RiderStatusResponse.class);

        // When
        riderGrpcServer.postRiderInfo(request, responseObserver);

        // Then
        verify(responseObserver).onNext(responseCaptor.capture());
        RiderStatusResponse response = responseCaptor.getValue();
        assertThat(response.getStatus()).isTrue();
    }

    @Test
    @DisplayName("postRiderInfo - Should return failure status when service fails")
    void postRiderInfo_ReturnsFailureStatus() {
        // Given
        PostRider request = TestDataBuilder.buildPostRiderRequest(1L, "StationA", "LocationC", TestDataBuilder.buildTimestamp());
        when(riderService.processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(false);

        ArgumentCaptor<RiderStatusResponse> responseCaptor = ArgumentCaptor.forClass(RiderStatusResponse.class);

        // When
        riderGrpcServer.postRiderInfo(request, responseObserver);

        // Then
        verify(responseObserver).onNext(responseCaptor.capture());
        RiderStatusResponse response = responseCaptor.getValue();
        assertThat(response.getStatus()).isFalse();
    }

    @Test
    @DisplayName("postRiderInfo - Should handle service exceptions gracefully")
    void postRiderInfo_HandlesServiceException() {
        // Given
        PostRider request = TestDataBuilder.buildPostRiderRequest(1L, "StationA", "LocationC", TestDataBuilder.buildTimestamp());
        RuntimeException exception = new RuntimeException("Service error");
        when(riderService.processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class)))
                .thenThrow(exception);

        // When
        riderGrpcServer.postRiderInfo(request, responseObserver);

        // Then
        verify(responseObserver).onError(exception);
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    @DisplayName("postRiderInfo - Should call service with correct parameters")
    void postRiderInfo_CallsServiceWithCorrectParameters() {
        // Given
        Long riderId = 123L;
        String pickUpStation = "StationX";
        String destinationPlace = "LocationY";
        Timestamp arrivalTime = TestDataBuilder.buildTimestamp();
        
        PostRider request = TestDataBuilder.buildPostRiderRequest(riderId, pickUpStation, destinationPlace, arrivalTime);
        when(riderService.processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(true);

        // When
        riderGrpcServer.postRiderInfo(request, responseObserver);

        // Then
        verify(riderService).processRiderInfo(
                eq(riderId),
                eq(pickUpStation),
                eq(destinationPlace),
                eq(arrivalTime)
        );
    }

    @Test
    @DisplayName("postRiderInfo - Should complete response observer after sending response")
    void postRiderInfo_CompletesResponseObserver() {
        // Given
        PostRider request = TestDataBuilder.buildPostRiderRequest(1L, "StationA", "LocationC", TestDataBuilder.buildTimestamp());
        when(riderService.processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(true);

        // When
        riderGrpcServer.postRiderInfo(request, responseObserver);

        // Then
        verify(responseObserver).onCompleted();
    }

    @Test
    @DisplayName("postRiderInfo - Should handle multiple sequential requests")
    void postRiderInfo_HandlesMultipleRequests() {
        // Given
        PostRider request1 = TestDataBuilder.buildPostRiderRequest(1L, "StationA", "LocationC", TestDataBuilder.buildTimestamp());
        PostRider request2 = TestDataBuilder.buildPostRiderRequest(2L, "StationB", "LocationD", TestDataBuilder.buildTimestamp());
        
        when(riderService.processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(true);

        // When
        riderGrpcServer.postRiderInfo(request1, responseObserver);
        riderGrpcServer.postRiderInfo(request2, responseObserver);

        // Then
        verify(riderService, times(2)).processRiderInfo(anyLong(), anyString(), anyString(), any(Timestamp.class));
        verify(responseObserver, times(2)).onNext(any(RiderStatusResponse.class));
        verify(responseObserver, times(2)).onCompleted();
    }
}
