'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { DriverNav } from '@/components/driver-nav'
import { RideOfferForm } from '@/components/ride-offer-form'
import { MatchingModal } from '@/components/matching-modal'
import { DriverTripView } from '@/components/driver-trip-view'
import { apiRequest } from '@/lib/api-config'

type TabType = 'post-offer' | 'matching' | 'trip'
type RideState = 'idle' | 'waiting' | 'matched' | 'active'

interface MatchData {
  riderId: number
  driverId: number
  driverArrivalTime?: string
}

export default function DriverPage() {
  const router = useRouter()
  const [authenticated, setAuthenticated] = useState(false)
  const [activeTab, setActiveTab] = useState<TabType>('post-offer')
  const [rideState, setRideState] = useState<RideState>('idle')
  const [loading, setLoading] = useState(false)
  const [driverId, setDriverId] = useState<number | null>(null)

  // Match state
  const [currentMatch, setCurrentMatch] = useState<MatchData | null>(null)
  const [showMatchModal, setShowMatchModal] = useState(false)
  const [sseConnected, setSseConnected] = useState(false)

  // Authentication check
  useEffect(() => {
    const token = localStorage.getItem('authToken')
    const role = localStorage.getItem('role')
    const storedDriverId = localStorage.getItem('userId')

    console.log('Auth check - token:', !!token, 'role:', role, 'storedDriverId:', storedDriverId)

    if (!token || role !== 'driver') {
      router.push('/auth?role=driver')
    } else {
      setAuthenticated(true)
      if (storedDriverId) {
        const parsedId = parseInt(storedDriverId, 10)
        console.log('Setting driverId to:', parsedId)
        setDriverId(parsedId)
      } else {
        console.warn('No userId found in localStorage! User may need to re-login.')
      }
    }
  }, [router])

  // SSE for match notifications
  useEffect(() => {
    // Only connect when waiting for or already matched with a driver
    if (!authenticated || !driverId || (rideState !== 'waiting' && rideState !== 'matched')) return

    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://127.0.0.1:8088'
    const eventSource = new EventSource(
      `${API_BASE_URL}/api/notification/matches?status=true`,
      { withCredentials: true }
    )

    eventSource.onopen = () => {
      console.log('SSE connection opened for driver matches')
      setSseConnected(true)
    }

    eventSource.onmessage = (event) => {
      try {
        const match: MatchData = JSON.parse(event.data)
        console.log('Received match:', match)

        // Only process if this match is for current driver
        if (Number(match.driverId) === driverId) {
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
  }, [authenticated, driverId, rideState])

  // SSE for ride completion
  useEffect(() => {
    console.log('Driver ride completion SSE effect triggered. State:', { authenticated, driverId, rideState })

    if (!authenticated || !driverId || rideState !== 'active') {
      console.log('Driver ride completion SSE conditions not met, skipping connection')
      return
    }

    console.log('Attempting to connect to driver ride completion SSE endpoint...')

    const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://127.0.0.1:8088'
    const eventSource = new EventSource(
      `${API_BASE_URL}/api/notification/driver-ride-completion?status=true`,
      { withCredentials: true }
    )

    eventSource.onopen = () => {
      console.log('✅ SSE connection opened for driver ride completion')
    }

    eventSource.onmessage = (event) => {
      try {
        const completion = JSON.parse(event.data)
        console.log('Ride completion received:', completion)

        if (Number(completion.driverId) === driverId) {
          console.log('Completion is for this driver, showing alert and resetting state')
          const message = completion.completionMessage || 'Ride completed successfully!'
          alert(`✅ ${message}`)

          // Reset ride state but DO NOT logout
          setRideState('idle')
          setCurrentMatch(null)
          setActiveTab('post-offer')
        }
      } catch (error) {
        console.error('Error processing completion notification:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('Driver completion SSE error:', error)
      eventSource.close()
    }

    return () => {
      console.log('Closing driver completion SSE connection')
      eventSource.close()
    }
  }, [authenticated, driverId, rideState])

  const handleLogout = () => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    router.push('/')
  }

  const handleSubmitOffer = async (offerData: any) => {
    setLoading(true)
    try {
      const payload = {
        driverId: driverId || 1,
        routeStations: offerData.routeStations,
        finalDestination: offerData.finalDestination,
        availableSeats: offerData.availableSeats
      }

      console.log('Submitting driver offer:', payload)

      const response = await apiRequest('/api/driver/driver-info', {
        method: 'POST',
        body: JSON.stringify(payload),
      })

      if (response && response.status === 200) {
        console.log('Ride offer posted successfully')
        setRideState('waiting')
        setActiveTab('matching')
      } else {
        alert('❌ Failed to post ride offer. Please try again.')
      }
    } catch (error: any) {
      console.error('Error submitting ride offer:', error)
      alert(`❌ Error: ${error.message || 'Failed to post ride offer'}`)
    } finally {
      setLoading(false)
    }
  }

  const handleMatchContinue = () => {
    setShowMatchModal(false)
    setRideState('active')
    setActiveTab('trip')
  }

  const handleCompleteRide = async () => {
    // This will be triggered by backend SSE, but we can also allow manual completion
    console.log('Completing ride manually')
    // In a real scenario, you'd call an API to mark the ride as complete
    // For now, we'll just reset the state
    handleRideCompletion('Ride completed successfully')
  }

  const handleRideCompletion = (message: string) => {
    console.log('Ride completion:', message)
    alert(`✅ ${message}`)

    // Reset ride state but DO NOT logout
    setRideState('idle')
    setCurrentMatch(null)
    setActiveTab('post-offer')
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
      <DriverNav onLogout={handleLogout} />

      <main className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold text-primary">Driver Dashboard</h1>
          {rideState === 'waiting' && (
            <div className="flex items-center gap-2 px-4 py-2 bg-blue-50 border border-blue-200 rounded-lg">
              <div className={`w-2 h-2 rounded-full ${sseConnected ? 'bg-green-500' : 'bg-yellow-500'}`}></div>
              <span className="text-sm font-medium text-blue-700">
                {sseConnected ? 'Listening for matches...' : 'Connecting...'}
              </span>
            </div>
          )}
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 mb-6 border-b border-border">
          <button
            onClick={() => rideState === 'idle' && setActiveTab('post-offer')}
            disabled={rideState !== 'idle'}
            className={`px-6 py-3 font-medium transition-colors ${activeTab === 'post-offer'
              ? 'text-primary border-b-2 border-primary'
              : 'text-muted-foreground hover:text-foreground'
              } ${rideState !== 'idle' ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Post Ride Offer
          </button>
          <button
            onClick={() => setActiveTab('matching')}
            disabled={rideState === 'idle'}
            className={`px-6 py-3 font-medium transition-colors ${activeTab === 'matching'
              ? 'text-primary border-b-2 border-primary'
              : 'text-muted-foreground hover:text-foreground'
              } ${rideState === 'idle' ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {rideState === 'waiting' ? 'Waiting for Match...' : 'Matching'}
          </button>
          <button
            onClick={() => setActiveTab('trip')}
            disabled={rideState !== 'active'}
            className={`px-6 py-3 font-medium transition-colors ${activeTab === 'trip'
              ? 'text-primary border-b-2 border-primary'
              : 'text-muted-foreground hover:text-foreground'
              } ${rideState !== 'active' ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Active Trip
          </button>
        </div>

        {/* Tab Content */}
        <div className="max-w-3xl mx-auto">
          {activeTab === 'post-offer' && rideState === 'idle' && (
            <Card className="p-6">
              <h2 className="text-xl font-semibold mb-6">Post Your Ride Offer</h2>
              {loading ? (
                <div className="text-center py-12">
                  <div className="animate-spin text-4xl mb-4">🚗</div>
                  <p className="text-muted-foreground">Posting your ride offer...</p>
                </div>
              ) : (
                <RideOfferForm onSubmit={handleSubmitOffer} />
              )}
            </Card>
          )}

          {activeTab === 'matching' && rideState === 'waiting' && (
            <Card className="p-12 text-center">
              <div className="text-6xl mb-4 animate-bounce">🔍</div>
              <h3 className="text-2xl font-bold mb-2">Looking for Riders...</h3>
              <p className="text-muted-foreground mb-6">
                We'll notify you when a rider matches your route
              </p>
              <div className="flex items-center justify-center gap-2">
                <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
                <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse delay-75"></div>
                <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse delay-150"></div>
              </div>
            </Card>
          )}

          {activeTab === 'trip' && rideState === 'active' && currentMatch && (
            <DriverTripView match={currentMatch} onCompleteRide={handleCompleteRide} />
          )}
        </div>
      </main>

      {/* Matching Modal */}
      <MatchingModal
        isOpen={showMatchModal}
        match={currentMatch}
        role="driver"
        onContinue={handleMatchContinue}
      />
    </div>
  )
}
