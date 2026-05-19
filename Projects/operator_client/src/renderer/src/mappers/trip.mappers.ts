import { AllTrips, AllTripsDto, GPSData, GPSDataDto, GPSDataMqtt, Trip, TripDto } from "@renderer/types/trip.types";

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

export const mapGpsDataFromMqtt = (data: string): GPSDataMqtt => {
    return JSON.parse(data.toString())
}

export const mapGpsData = (data: GPSDataDto): GPSData => {
    return {
        ...data,
        recDatetime: new Date(data.recDatetime)
    }
}

export const mapGpsDataList = (data: GPSDataDto[]): GPSData[] => {
    return data.map(d => mapGpsData(d))
}