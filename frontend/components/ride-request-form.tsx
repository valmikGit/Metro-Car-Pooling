'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import locationsData from '@/data/nodes.json'
import metroStations from '@/data/mapping.json'

interface RideRequestFormProps {
  onSubmit: (data: any) => void
  riderId: number
}

export function RideRequestForm({ onSubmit, riderId }: RideRequestFormProps) {
  const [pickUpStation, setPickUpStation] = useState('')
  const [destinationPlace, setDestinationPlace] = useState('')
  const [arrivalTime, setArrivalTime] = useState('')

  // Extract nodes from locations.json
  const nodes = Array.isArray(locationsData) 
    ? locationsData 
    : (locationsData as any).nodes || Object.keys(locationsData)

  // Extract metro stations from mapping.json - only those with non-empty values
  const getMetroStations = (): string[] => {
    const stations = new Set<string>()
    
    Object.entries(metroStations).forEach(([key, value]) => {
      // Only add if value is not empty string
      if (value && value !== '') {
        stations.add(key)
      }
    })
    
    // Convert Set to Array and sort alphabetically
    return Array.from(stations).sort()
  }

  const metroStationList = getMetroStations()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!pickUpStation || !destinationPlace || !arrivalTime) {
      alert('Please fill all fields')
      return
    }

    // Combine today's date with the selected time
    const today = new Date()
    const [hours, minutes] = arrivalTime.split(':')
    const dateTime = new Date(
      today.getFullYear(),
      today.getMonth(),
      today.getDate(),
      parseInt(hours),
      parseInt(minutes)
    )
    
    // Convert to protobuf Timestamp format (seconds and nanos)
    const seconds = Math.floor(dateTime.getTime() / 1000)
    const nanos = (dateTime.getTime() % 1000) * 1000000
    
    const requestData = {
      riderId: riderId,
      pickUpStation: pickUpStation,
      destinationPlace: destinationPlace,
      arrivalTime: {
        seconds: seconds,
        nanos: nanos
      }
    }

    onSubmit(requestData)
    
    // Reset form
    setPickUpStation('')
    setDestinationPlace('')
    setArrivalTime('')
  }

  // Get formatted date-time string for display
  const getFormattedDateTime = () => {
    if (!arrivalTime) return ''
    
    const today = new Date()
    const [hours, minutes] = arrivalTime.split(':')
    const dateTime = new Date(
      today.getFullYear(),
      today.getMonth(),
      today.getDate(),
      parseInt(hours),
      parseInt(minutes)
    )
    
    return dateTime.toLocaleString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Pickup Station Selection */}
      <div className="space-y-2">
        <Label htmlFor="pickup" className="flex items-center gap-2">
          <span className="text-xl">📍</span>
          Pickup Metro Station
        </Label>
        <select
          id="pickup"
          value={pickUpStation}
          onChange={(e) => setPickUpStation(e.target.value)}
          className="w-full px-4 py-3 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-accent text-lg"
          required
        >
          <option value="">Select your pickup station...</option>
          {metroStationList && metroStationList.length > 0 ? (
            metroStationList.map((station: string, index: number) => (
              <option key={index} value={station}>
                {station}
              </option>
            ))
          ) : (
            <option value="" disabled>No stations available</option>
          )}
        </select>
      </div>

      {/* Visual representation of selected pickup */}
      {pickUpStation && (
        <div className="bg-gradient-to-br from-accent/5 via-background to-accent/10 rounded-xl p-4 border-2 border-accent/20">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-green-500 border-4 border-green-300 shadow-lg shadow-green-500/50">
              <span className="text-white text-xl">🚇</span>
            </div>
            <div>
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Pickup Location
              </p>
              <p className="text-lg font-bold text-foreground">
                {pickUpStation}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Destination Place Selection */}
      <div className="space-y-2">
        <Label htmlFor="destination" className="flex items-center gap-2">
          <span className="text-xl">🏁</span>
          Final Destination
        </Label>
        <select
          id="destination"
          value={destinationPlace}
          onChange={(e) => setDestinationPlace(e.target.value)}
          className="w-full px-4 py-3 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-accent text-lg"
          required
        >
          <option value="">Select your destination...</option>
          {metroStationList && metroStationList.length > 0 ? (
            metroStationList.map((station: string, index: number) => (
              <option key={index} value={station}>
                {station}
              </option>
            ))
          ) : (
            <option value="" disabled>No stations available</option>
          )}
        </select>
        <p className="text-xs text-muted-foreground">
          Select the station closest to your final destination
        </p>
      </div>

      {/* Visual representation of destination */}
      {destinationPlace && (
        <div className="bg-gradient-to-br from-red-500/5 via-background to-red-500/10 rounded-xl p-4 border-2 border-red-500/20">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-red-500 border-4 border-red-300 shadow-lg shadow-red-500/50">
              <span className="text-white text-xl">🏁</span>
            </div>
            <div>
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Destination
              </p>
              <p className="text-lg font-bold text-foreground">
                {destinationPlace}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Arrival Time */}
      <div className="space-y-2">
        <Label htmlFor="time" className="flex items-center gap-2">
          <span className="text-xl">⏰</span>
          Expected Arrival Time at Pickup (Today)
        </Label>
        <Input
          id="time"
          type="time"
          value={arrivalTime}
          onChange={(e) => setArrivalTime(e.target.value)}
          required
          className="text-lg py-6"
        />
        <p className="text-xs text-muted-foreground">
          Select the time you will reach the pickup station today
        </p>
      </div>

      {/* Time visualization */}
      {arrivalTime && (
        <div className="bg-gradient-to-br from-blue-500/5 via-background to-blue-500/10 rounded-xl p-4 border-2 border-blue-500/20">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-blue-500 border-4 border-blue-300 shadow-lg shadow-blue-500/50">
              <span className="text-white text-xl">⏰</span>
            </div>
            <div>
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Scheduled Time
              </p>
              <p className="text-lg font-bold text-foreground">
                {getFormattedDateTime()}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Route Summary */}
      {pickUpStation && destinationPlace && arrivalTime && (
        <div className="bg-gradient-to-br from-accent/10 via-background to-accent/5 rounded-xl p-6 border-2 border-accent/30 shadow-lg">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <span className="text-2xl">🗺️</span>
            Trip Summary
          </h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-green-500 flex items-center justify-center flex-shrink-0">
                <span className="text-white text-sm">🚇</span>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">From</p>
                <p className="font-semibold">{pickUpStation}</p>
              </div>
            </div>
            
            <div className="ml-4 border-l-2 border-dashed border-accent/30 h-6"></div>
            
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-red-500 flex items-center justify-center flex-shrink-0">
                <span className="text-white text-sm">🏁</span>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">To</p>
                <p className="font-semibold">{destinationPlace}</p>
              </div>
            </div>
            
            <div className="ml-4 pt-3 border-t border-border">
              <div className="flex items-center gap-2 text-sm">
                <span>⏰</span>
                <span className="text-muted-foreground">
                  {getFormattedDateTime()}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Submit Button */}
      <Button 
        type="submit" 
        className="w-full text-lg py-6 font-semibold shadow-lg hover:shadow-xl transition-all bg-accent hover:bg-accent/90" 
        disabled={!pickUpStation || !destinationPlace || !arrivalTime}
      >
        {!pickUpStation || !destinationPlace || !arrivalTime
          ? '📝 Fill all fields to continue' 
          : '🚀 Request Ride'
        }
      </Button>
    </form>
  )
}
