import { AllTrips, AllTripsDto, Trip, TripDto } from "@renderer/types/trip.types";

export const mapTripDto = (trip: TripDto): Trip => {
    return {
        ...trip,
        endDatetime: trip.endDatetime ? new Date(trip.endDatetime) : null,
        startDatetime: new Date(trip.startDatetime)
    }
}

export const mapAllTripsDto = (trips: AllTripsDto): AllTrips => {
    return {
        current: trips.current.map(mapTripDto),
        ended: trips.ended.map(mapTripDto)
    }
}