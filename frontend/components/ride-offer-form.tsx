'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import locationsData from '@/data/nodes.json'

interface RideOfferFormProps {
  onSubmit: (data: any) => void
}

export function RideOfferForm({ onSubmit }: RideOfferFormProps) {
  const [routeStations, setRouteStations] = useState<string[]>([])
  const [selectedStation, setSelectedStation] = useState('')
  const [availableSeats, setAvailableSeats] = useState('')

  // Extract nodes safely
  const nodes = Array.isArray(locationsData) 
    ? locationsData 
    : (locationsData as any).nodes || Object.keys(locationsData)

  const handleAddStation = () => {
    if (selectedStation && !routeStations.includes(selectedStation)) {
      setRouteStations([...routeStations, selectedStation])
      setSelectedStation('')
    }
  }

  const handleRemoveStation = (index: number) => {
    setRouteStations(routeStations.filter((_, i) => i !== index))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    if (routeStations.length < 2) {
      alert('Please add at least 2 stations (including final destination)')
      return
    }

    const finalDestination = routeStations[routeStations.length - 1]
    
    const offerData = {
      routeStations,
      finalDestination,
      availableSeats: parseInt(availableSeats)
    }

    onSubmit(offerData)
    
    // Reset form
    setRouteStations([])
    setAvailableSeats('')
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Route Stations Selection */}
      <div className="space-y-3">
        <Label htmlFor="station">Select Route Stations</Label>
        <div className="flex gap-2">
          <select
            id="station"
            value={selectedStation}
            onChange={(e) => setSelectedStation(e.target.value)}
            className="flex-1 px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="">Select a station...</option>
            {nodes && nodes.length > 0 ? (
              nodes.map((node: string, index: number) => (
                <option key={index} value={node}>
                  {node}
                </option>
              ))
            ) : (
              <option value="" disabled>No stations available</option>
            )}
          </select>
          <Button
            type="button"
            onClick={handleAddStation}
            variant="outline"
            disabled={!selectedStation}
            className="whitespace-nowrap"
          >
            ➕ Add
          </Button>
        </div>
        
        {/* Map-like Route Visualization */}
        {routeStations.length > 0 && (
          <div className="mt-6">
            <p className="text-sm font-medium mb-4 flex items-center gap-2">
              <span className="text-2xl">🗺️</span>
              Your Route Map
            </p>
            <div className="bg-linear-to-br from-primary/5 via-background to-primary/10 rounded-xl p-6 border-2 border-primary/20 shadow-lg">
              <div className="space-y-1">
                {routeStations.map((station, index) => (
                  <div key={index} className="relative">
                    {/* Station Card */}
                    <div className="flex items-center gap-4">
                      {/* Route Line and Pin */}
                      <div className="flex flex-col items-center">
                        {/* Pin/Marker */}
                        <div className={`relative z-10 flex items-center justify-center w-10 h-10 rounded-full border-4 ${
                          index === 0 
                            ? 'bg-green-500 border-green-300 shadow-lg shadow-green-500/50' 
                            : index === routeStations.length - 1
                            ? 'bg-red-500 border-red-300 shadow-lg shadow-red-500/50'
                            : 'bg-primary border-primary/30 shadow-lg shadow-primary/50'
                        }`}>
                          <span className="text-white font-bold text-sm">
                            {index === 0 ? '🚗' : index === routeStations.length - 1 ? '🏁' : index + 1}
                          </span>
                        </div>
                        
                        {/* Connecting Line */}
                        {index < routeStations.length - 1 && (
                          <div className="w-1 h-12 bg-linear-to-b from-primary via-primary/70 to-primary/50 relative">
                            <div className="absolute inset-0 bg-primary/30 animate-pulse"></div>
                          </div>
                        )}
                      </div>

                      {/* Station Info Card */}
                      <div className={`flex-1 p-4 rounded-lg border-2 transition-all ${
                        index === 0
                          ? 'bg-green-50 dark:bg-green-950/20 border-green-500/50'
                          : index === routeStations.length - 1
                          ? 'bg-red-50 dark:bg-red-950/20 border-red-500/50'
                          : 'bg-card border-border hover:border-primary/50'
                      }`}>
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                                {index === 0 
                                  ? 'Starting Point' 
                                  : index === routeStations.length - 1
                                  ? 'Final Destination'
                                  : `Stop ${index}`
                                }
                              </span>
                            </div>
                            <p className="text-lg font-bold text-foreground flex items-center gap-2">
                              📍 {station}
                            </p>
                          </div>
                          
                          {/* Remove Button */}
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => handleRemoveStation(index)}
                            className="text-destructive hover:text-destructive hover:bg-destructive/10 ml-2"
                          >
                            <span className="text-lg">🗑️</span>
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Available Seats */}
      <div className="space-y-2">
        <Label htmlFor="seats" className="flex items-center gap-2">
          <span className="text-xl">💺</span>
          Available Seats
        </Label>
        <Input
          id="seats"
          type="number"
          min="1"
          max="8"
          value={availableSeats}
          onChange={(e) => setAvailableSeats(e.target.value)}
          placeholder="Enter number of seats"
          required
          className="text-lg"
        />
      </div>

      {/* Submit Button */}
      <Button 
        type="submit" 
        className="w-full text-lg py-6 font-semibold shadow-lg hover:shadow-xl transition-all" 
        disabled={routeStations.length < 2 || !availableSeats}
      >
        {routeStations.length < 2 
          ? '➕ Add at least 2 stations to continue' 
          : '🚀 Post Ride Offer'
        }
      </Button>
    </form>
  )
}
