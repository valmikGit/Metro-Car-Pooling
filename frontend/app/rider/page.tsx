'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { RiderNav } from '@/components/rider-nav'
import { MatchList } from '@/components/match-list'
import { RideRequestForm } from '@/components/ride-request-form'
import { apiRequest } from '@/lib/api-config'

type TabType = 'post-request' | 'matches' | 'history'

export default function RiderPage() {
  const router = useRouter()
  const [authenticated, setAuthenticated] = useState(false)
  const [activeTab, setActiveTab] = useState<TabType>('post-request')
  const [matches, setMatches] = useState<any[]>([])
  const [riderId, setRiderId] = useState<number | null>(null)
  const [disableRequestTab, setDisableRequestTab] = useState(false)
  const [loading, setLoading] = useState(false)

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
    if (!authenticated || !riderId) return

    const eventSource = new EventSource(
      `http://localhost:8080/api/notification/matches?status=true`,
      { withCredentials: true }
    )

    eventSource.onmessage = (event) => {
      try {
        const match = JSON.parse(event.data)
        if (match.riderId === riderId) {
          setMatches(prev => {
            const exists = prev.some(m => m.matchId === match.matchId)
            if (!exists) {
              return [match, ...prev]
            }
            return prev
          })
        }
      } catch (error) {
        console.error('Error processing match notification:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('SSE error:', error)
      eventSource.close()
    }

    return () => eventSource.close()
  }, [authenticated, riderId])

  // SSE for driver location
  useEffect(() => {
    if (!authenticated || !riderId) return

    const eventSource = new EventSource(
      `http://localhost:8080/api/notification/driver-location-for-rider?status=true`,
      { withCredentials: true }
    )

    eventSource.onmessage = (event) => {
      try {
        const locationData = JSON.parse(event.data)
        if (locationData.riderId === riderId) {
          console.log('Driver location update:', locationData)
          setMatches(prev => prev.map(match => 
            match.driverId === locationData.driverId
              ? { ...match, driverLocation: locationData }
              : match
          ))
        }
      } catch (error) {
        console.error('Error processing driver location:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('Driver location SSE error:', error)
      eventSource.close()
    }

    return () => eventSource.close()
  }, [authenticated, riderId])

  // SSE for ride completion
  useEffect(() => {
    if (!authenticated || !riderId) return

    const eventSource = new EventSource(
      `http://localhost:8080/api/notification/rider-ride-completion?status=true`,
      { withCredentials: true }
    )

    eventSource.onmessage = (event) => {
      try {
        const completion = JSON.parse(event.data)
        if (completion.riderId === riderId) {
          console.log('Ride completed:', completion)
          setMatches(prev => prev.filter(m => m.matchId !== completion.matchId))
        }
      } catch (error) {
        console.error('Error processing completion notification:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('Completion SSE error:', error)
      eventSource.close()
    }

    return () => eventSource.close()
  }, [authenticated, riderId])

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
        alert('🎉 Ride request submitted successfully!')
        setActiveTab('matches')
        setDisableRequestTab(true)
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
        <h1 className="text-3xl font-bold text-accent mb-8">Rider Dashboard</h1>

        {/* Tab Navigation */}
        <div className="flex gap-2 mb-6 border-b border-border">
          <button
            onClick={() => !disableRequestTab && setActiveTab('post-request')}
            disabled={disableRequestTab}
            className={`px-6 py-3 font-medium transition-colors ${
              activeTab === 'post-request'
                ? 'text-accent border-b-2 border-accent'
                : 'text-muted-foreground hover:text-foreground'
            } ${disableRequestTab ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Post Rider Request
          </button>
          <button
            onClick={() => setActiveTab('matches')}
            className={`px-6 py-3 font-medium transition-colors ${
              activeTab === 'matches'
                ? 'text-accent border-b-2 border-accent'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            Matches
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`px-6 py-3 font-medium transition-colors ${
              activeTab === 'history'
                ? 'text-accent border-b-2 border-accent'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            History
          </button>
        </div>

        {/* Tab Content */}
        <div className="max-w-3xl mx-auto">
          {activeTab === 'post-request' && (
            <Card className="p-6">
              <h2 className="text-xl font-semibold mb-6">Post Rider Request</h2>
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

          {activeTab === 'matches' && (
            <div>
              <MatchList matches={matches} role="rider" />
            </div>
          )}

          {activeTab === 'history' && (
            <Card className="p-12 text-center">
              <div className="text-4xl mb-4">⏱️</div>
              <p className="text-muted-foreground">No completed rides yet</p>
            </Card>
          )}
        </div>
      </main>
    </div>
  )
}
