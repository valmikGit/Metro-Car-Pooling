'use client'

import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useState } from 'react'

interface DriverTripViewProps {
    match: {
        riderId: number
        driverId: number
        driverArrivalTime?: string
    }
    onCompleteRide: () => void
}

export function DriverTripView({ match, onCompleteRide }: DriverTripViewProps) {
    const [completing, setCompleting] = useState(false)

    const handleComplete = async () => {
        setCompleting(true)
        try {
            await onCompleteRide()
        } finally {
            setCompleting(false)
        }
    }

    return (
        <div className="space-y-6">
            <Card className="p-6 bg-gradient-to-br from-blue-50 to-purple-50 border-blue-200">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-2xl font-bold text-blue-900">Active Trip</h2>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
                        <span className="text-sm font-medium text-green-700">In Progress</span>
                    </div>
                </div>

                <div className="space-y-4">
                    <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-sm">
                        <div className="text-4xl">👤</div>
                        <div className="flex-1">
                            <p className="text-sm text-muted-foreground">Passenger</p>
                            <p className="text-xl font-semibold">Rider #{match.riderId}</p>
                        </div>
                    </div>

                    {match.driverArrivalTime && (
                        <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-sm">
                            <div className="text-4xl">⏰</div>
                            <div className="flex-1">
                                <p className="text-sm text-muted-foreground">Estimated Arrival</p>
                                <p className="font-semibold">
                                    {new Date(match.driverArrivalTime).toLocaleString()}
                                </p>
                            </div>
                        </div>
                    )}

                    <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-sm">
                        <div className="text-4xl">🚗</div>
                        <div className="flex-1">
                            <p className="text-sm text-muted-foreground">Trip Status</p>
                            <p className="font-semibold text-blue-600">Driving to destination</p>
                        </div>
                    </div>
                </div>
            </Card>

            <Card className="p-6">
                <h3 className="text-lg font-semibold mb-4">Trip Actions</h3>
                <div className="space-y-3">
                    <Button
                        onClick={handleComplete}
                        disabled={completing}
                        className="w-full bg-green-600 hover:bg-green-700 text-white"
                        size="lg"
                    >
                        {completing ? (
                            <>
                                <div className="animate-spin mr-2">⚙️</div>
                                Completing Trip...
                            </>
                        ) : (
                            <>
                                ✅ Complete Trip
                            </>
                        )}
                    </Button>
                    <p className="text-xs text-center text-muted-foreground">
                        Click when you have safely dropped off the passenger
                    </p>
                </div>
            </Card>

            <Card className="p-4 bg-blue-50 border-blue-200">
                <div className="flex items-start gap-3">
                    <span className="text-xl">💡</span>
                    <div className="text-sm">
                        <p className="font-medium text-blue-900 mb-1">Trip Tips</p>
                        <ul className="text-blue-700 space-y-1">
                            <li>• Ensure passenger safety at all times</li>
                            <li>• Follow the designated route</li>
                            <li>• Complete trip only after drop-off</li>
                        </ul>
                    </div>
                </div>
            </Card>
        </div>
    )
}
