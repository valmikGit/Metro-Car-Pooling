'use client'

import { Card } from '@/components/ui/card'

interface RiderTripViewProps {
    match: {
        riderId?: number
        driverId: number
        driverArrivalTime?: string
    }
    driverLocation?: {
        nextStation?: string
        timeToNextStation?: number
    }
}

export function RiderTripView({ match, driverLocation }: RiderTripViewProps) {
    return (
        <div className="space-y-6">
            <Card className="p-6 bg-gradient-to-br from-purple-50 to-pink-50 border-purple-200">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-2xl font-bold text-purple-900">Your Ride</h2>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
                        <span className="text-sm font-medium text-green-700">Active</span>
                    </div>
                </div>

                <div className="space-y-4">
                    <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-sm">
                        <div className="text-4xl">🚗</div>
                        <div className="flex-1">
                            <p className="text-sm text-muted-foreground">Your Driver</p>
                            <p className="text-xl font-semibold">Driver #{match.driverId}</p>
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
                </div>
            </Card>

            {driverLocation && (driverLocation.nextStation || driverLocation.timeToNextStation !== undefined) && (
                <Card className="p-6 bg-gradient-to-br from-green-50 to-teal-50 border-green-200">
                    <div className="flex items-center gap-2 mb-4">
                        <div className="text-2xl">📍</div>
                        <h3 className="text-lg font-semibold text-green-900">Live Driver Location</h3>
                    </div>

                    <div className="space-y-3">
                        {driverLocation.nextStation && (
                            <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-sm">
                                <div className="text-3xl">🚉</div>
                                <div className="flex-1">
                                    <p className="text-sm text-muted-foreground">Next Station</p>
                                    <p className="text-lg font-semibold text-green-700">
                                        {driverLocation.nextStation}
                                    </p>
                                </div>
                            </div>
                        )}

                        {driverLocation.timeToNextStation !== undefined && (
                            <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-sm">
                                <div className="text-3xl">⏱️</div>
                                <div className="flex-1">
                                    <p className="text-sm text-muted-foreground">Arriving In</p>
                                    <p className="text-lg font-semibold text-green-700">
                                        {driverLocation.timeToNextStation} seconds
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>
                </Card>
            )}

            <Card className="p-4 bg-purple-50 border-purple-200">
                <div className="flex items-start gap-3">
                    <span className="text-xl">💡</span>
                    <div className="text-sm">
                        <p className="font-medium text-purple-900 mb-1">Ride Tips</p>
                        <ul className="text-purple-700 space-y-1">
                            <li>• Be ready at your pickup location</li>
                            <li>• Track your driver's live location above</li>
                            <li>• Contact driver if you need assistance</li>
                        </ul>
                    </div>
                </div>
            </Card>
        </div>
    )
}
