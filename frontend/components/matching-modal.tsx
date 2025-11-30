'use client'

import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'

interface MatchData {
    riderId: number
    driverId: number
    driverArrivalTime?: string
}

interface MatchingModalProps {
    isOpen: boolean
    match: MatchData | null
    role: 'driver' | 'rider'
    onAccept: () => void
    onReject: () => void
}

export function MatchingModal({ isOpen, match, role, onAccept, onReject }: MatchingModalProps) {
    if (!match) return null

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onReject()}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle className="text-2xl flex items-center gap-2">
                        🎉 Match Found!
                    </DialogTitle>
                    <DialogDescription>
                        {role === 'driver'
                            ? 'A rider has been matched with your route'
                            : 'A driver has been matched with your request'}
                    </DialogDescription>
                </DialogHeader>

                <Card className="p-4 bg-gradient-to-br from-green-50 to-blue-50 border-green-200">
                    <div className="space-y-3">
                        <div className="flex items-center gap-3">
                            <span className="text-2xl">👤</span>
                            <div>
                                <p className="text-sm text-muted-foreground">
                                    {role === 'driver' ? 'Rider ID' : 'Driver ID'}
                                </p>
                                <p className="font-semibold text-lg">
                                    #{role === 'driver' ? match.riderId : match.driverId}
                                </p>
                            </div>
                        </div>

                        {match.driverArrivalTime && (
                            <div className="flex items-center gap-3">
                                <span className="text-2xl">⏰</span>
                                <div>
                                    <p className="text-sm text-muted-foreground">Estimated Arrival</p>
                                    <p className="font-semibold">
                                        {new Date(match.driverArrivalTime).toLocaleString()}
                                    </p>
                                </div>
                            </div>
                        )}

                        <div className="flex items-center gap-3">
                            <span className="text-2xl">🚗</span>
                            <div>
                                <p className="text-sm text-muted-foreground">Status</p>
                                <p className="font-semibold text-green-600">Ready to Connect</p>
                            </div>
                        </div>
                    </div>
                </Card>

                <div className="flex gap-3 mt-4">
                    <Button
                        onClick={onAccept}
                        className="flex-1 bg-green-600 hover:bg-green-700 text-white"
                        size="lg"
                    >
                        ✅ Accept Match
                    </Button>
                    <Button
                        onClick={onReject}
                        variant="outline"
                        className="flex-1 border-red-200 text-red-600 hover:bg-red-50"
                        size="lg"
                    >
                        ❌ Decline
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    )
}
