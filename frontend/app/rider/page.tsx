'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { RiderNav } from '@/components/rider-nav'
import { RideRequestForm } from '@/components/ride-request-form'
import { MatchingModal } from '@/components/matching-modal'
import { RiderTripView } from '@/components/rider-trip-view'
import { apiRequest } from '@/lib/api-config'

type TabType = 'post-request' | 'matching' | 'trip'
type RideState = 'idle' | 'waiting' | 'matched' | 'active'

interface MatchData {
  riderId: number
  driverId: number
  driverArrivalTime?: string
}

interface DriverLocation {
  nextStation?: string
  timeToNextStation?: number
}

export default function RiderPage() {
  const router = useRouter()
  const [authenticated, setAuthenticated] = useState(false)
  const [activeTab, setActiveTab] = useState<TabType>('post-request')
  const [rideState, setRideState] = useState<RideState>('idle')
  const [loading, setLoading] = useState(false)
  const [riderId, setRiderId] = useState<number | null>(null)

  // Match state
  const [currentMatch, setCurrentMatch] = useState<MatchData | null>(null)
  const [driverLocation, setDriverLocation] = useState<DriverLocation | undefined>(undefined)
  const [showMatchModal, setShowMatchModal] = useState(false)
  const [sseConnected, setSseConnected] = useState(false)

  // Authentication check
  useEffect(() => {
    const token = localStorage.getItem('authToken')
    const role = localStorage.getItem('role')
    const storedRiderId = localStorage.getItem('userId')

    if (!token || role !== 'rider') {
      router.push('/auth?role=rider')
    } else {
      setAuthenticated(true)
      if (storedRiderId) {
        setRiderId(parseInt(storedRiderId))
      }
    }
  }, [router])

  // SSE for match notifications
  useEffect(() => {
    if (!authenticated || !riderId || rideState === 'active') return

    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081'
    const eventSource = new EventSource(
      `${API_BASE_URL}/api/notification/matches?status=true`,
      { withCredentials: true }
    )

    eventSource.onopen = () => {
      console.log('SSE connection opened for rider matches')
      setSseConnected(true)
    }

    eventSource.onmessage = (event) => {
      try {
        const match: MatchData = JSON.parse(event.data)
        console.log('Received match:', match)

        // Only process if this match is for current rider
        if (match.riderId === riderId) {
          setCurrentMatch(match)
          setShowMatchModal(true)
          setRideState('matched')
        }
      } catch (error) {
        console.error('Error processing match notification:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('SSE error:', error)
      setSseConnected(false)
      eventSource.close()
    }

    return () => {
      console.log('Closing match SSE connection')
      eventSource.close()
    }
  }, [authenticated, riderId, rideState])

  // SSE for driver location updates
  useEffect(() => {
    if (!authenticated || !riderId || rideState !== 'active') return

    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081'
    const eventSource = new EventSource(
      `${API_BASE_URL}/api/notification/driver-location-for-rider?status=true`,
      { withCredentials: true }
    )

    eventSource.onopen = () => {
      console.log('SSE connection opened for driver location')
    }

    eventSource.onmessage = (event) => {
      try {
        const locationData = JSON.parse(event.data)
        console.log('Driver location update:', locationData)

        // Only process if this update is for current rider
        if (locationData.riderId === riderId) {
          setDriverLocation({
            nextStation: locationData.nextStation,
            timeToNextStation: locationData.timeToNextStation
          })
        }
      } catch (error) {
        console.error('Error processing driver location:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('Driver location SSE error:', error)
      eventSource.close()
    }

    return () => {
      console.log('Closing driver location SSE connection')
      eventSource.close()
    }
  }, [authenticated, riderId, rideState])

  // SSE for ride completion
  useEffect(() => {
    if (!authenticated || !riderId || rideState !== 'active') return

    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081'
    const eventSource = new EventSource(
      `${API_BASE_URL}/api/notification/rider-ride-completion?status=true`,
      { withCredentials: true }
    )

    eventSource.onopen = () => {
      console.log('SSE connection opened for rider ride completion')
    }

    eventSource.onmessage = (event) => {
      try {
        const completion = JSON.parse(event.data)
        console.log('Ride completion received:', completion)

        if (completion.riderId === riderId) {
          handleRideCompletion(completion.completionMessage)
        }
      } catch (error) {
        console.error('Error processing completion notification:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('Completion SSE error:', error)
      eventSource.close()
    }

    return () => {
      console.log('Closing completion SSE connection')
      eventSource.close()
    }
  }, [authenticated, riderId, rideState])

  const handleLogout = () => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    router.push('/')
  }

  const handleSubmitRequest = async (requestData: any) => {
    setLoading(true)
    try {
      console.log('Submitting rider request:', requestData)

      const response = await apiRequest('/api/rider/rider-info', {
        method: 'POST',
        body: JSON.stringify(requestData),
      })

      console.log('Response:', response)

      if (response && response.status === 200) {
        console.log('Ride request submitted successfully')
        setRideState('waiting')
        setActiveTab('matching')
      } else {
        alert('❌ Failed to submit ride request. Please try again.')
      }
    } catch (error: any) {
      console.error('Error submitting ride request:', error)
      alert(`❌ Error: ${error.message || 'Failed to submit ride request'}`)
    } finally {
      setLoading(false)
    }
  }

  const handleAcceptMatch = () => {
    setShowMatchModal(false)
    setRideState('active')
    setActiveTab('trip')
  }

  const handleRejectMatch = () => {
    setShowMatchModal(false)
    setCurrentMatch(null)
    setDriverLocation(undefined)
    setRideState('waiting')
  }

  const handleRideCompletion = (message: string) => {
    console.log('Ride completion:', message)
    alert(`✅ ${message}`)

    // Reset ride state but DO NOT logout
    setRideState('idle')
    setCurrentMatch(null)
    setDriverLocation(undefined)
    setActiveTab('post-request')
  }

  if (!authenticated) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin text-4xl mb-4">⚙️</div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <RiderNav onLogout={handleLogout} />

      <main className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold text-accent">Rider Dashboard</h1>
          {rideState === 'waiting' && (
            <div className="flex items-center gap-2 px-4 py-2 bg-purple-50 border border-purple-200 rounded-lg">
              <div className={`w-2 h-2 rounded-full ${sseConnected ? 'bg-green-500' : 'bg-yellow-500'}`}></div>
              <span className="text-sm font-medium text-purple-700">
                {sseConnected ? 'Listening for matches...' : 'Connecting...'}
              </span>
            </div>
          )}
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 mb-6 border-b border-border">
          <button
            onClick={() => rideState === 'idle' && setActiveTab('post-request')}
            disabled={rideState !== 'idle'}
            className={`px-6 py-3 font-medium transition-colors ${activeTab === 'post-request'
                ? 'text-accent border-b-2 border-accent'
                : 'text-muted-foreground hover:text-foreground'
              } ${rideState !== 'idle' ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Post Ride Request
          </button>
          <button
            onClick={() => setActiveTab('matching')}
            disabled={rideState === 'idle'}
            className={`px-6 py-3 font-medium transition-colors ${activeTab === 'matching'
                ? 'text-accent border-b-2 border-accent'
                : 'text-muted-foreground hover:text-foreground'
              } ${rideState === 'idle' ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {rideState === 'waiting' ? 'Waiting for Match...' : 'Matching'}
          </button>
          <button
            onClick={() => setActiveTab('trip')}
            disabled={rideState !== 'active'}
            className={`px-6 py-3 font-medium transition-colors ${activeTab === 'trip'
                ? 'text-accent border-b-2 border-accent'
                : 'text-muted-foreground hover:text-foreground'
              } ${rideState !== 'active' ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Active Trip
          </button>
        </div>

        {/* Tab Content */}
        <div className="max-w-3xl mx-auto">
          {activeTab === 'post-request' && rideState === 'idle' && (
            <Card className="p-6">
              <h2 className="text-xl font-semibold mb-6">Post Your Ride Request</h2>
              {loading ? (
                <div className="text-center py-12">
                  <div className="animate-spin text-4xl mb-4">🚗</div>
                  <p className="text-muted-foreground">Submitting your ride request...</p>
                </div>
              ) : (
                <RideRequestForm onSubmit={handleSubmitRequest} riderId={riderId || 0} />
              )}
            </Card>
          )}

          {activeTab === 'matching' && rideState === 'waiting' && (
            <Card className="p-12 text-center">
              <div className="text-6xl mb-4 animate-bounce">🔍</div>
              <h3 className="text-2xl font-bold mb-2">Looking for Drivers...</h3>
              <p className="text-muted-foreground mb-6">
                We'll notify you when a driver matches your route
              </p>
              <div className="flex items-center justify-center gap-2">
                <div className="w-2 h-2 bg-purple-500 rounded-full animate-pulse"></div>
                <div className="w-2 h-2 bg-purple-500 rounded-full animate-pulse delay-75"></div>
                <div className="w-2 h-2 bg-purple-500 rounded-full animate-pulse delay-150"></div>
              </div>
            </Card>
          )}

          {activeTab === 'trip' && rideState === 'active' && currentMatch && (
            <RiderTripView match={currentMatch} driverLocation={driverLocation} />
          )}
        </div>
      </main>

      {/* Matching Modal */}
      <MatchingModal
        isOpen={showMatchModal}
        match={currentMatch}
        role="rider"
        onAccept={handleAcceptMatch}
        onReject={handleRejectMatch}
      />
    </div>
  )
}
