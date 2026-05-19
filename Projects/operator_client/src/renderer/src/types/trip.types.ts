import { PersonDto } from "./person.types";

export interface CarDto {
    carId: number;
    vinNumber: string;
    stateNumber: string | null | undefined;
    brandName: string;
    modelName: string;
    bodyName: string;
    releaseYear: number;
    gearboxName: string;
    driveName: string;
    vehicleWeightKg: number;
    enginePowerHp: number;
    enginePowerKw: number;
    engineCapacityL: number;
    tankCapacityL: number;
    fuelTypeName: string;
    person: PersonDto;
    photoId: number;
}

export interface TripDto {
    tripId: number;
    startDatetime: string;
    endDatetime: string | null | undefined;
    deviceId: number;
    car: CarDto
}

export interface Trip {
    tripId: number;
    startDatetime: Date;
    endDatetime: Date | null | undefined;
    deviceId: number;
    car: CarDto
}

export interface AllTripsDto {
    current: TripDto[];
    ended: TripDto[];
}

export interface AllTrips {
    current: Trip[];
    ended: Trip[];
}

export interface GPSDataMqtt {
    recDatetime: string;
    tripId: number;
    latitudeDeg: number;
    longitudeDeg:number;
    accuracyM: number | null | undefined;
    speedKmh: number | null | undefined;
    bearingDeg: number | null | undefined;
}

export interface GPSDataDto {
    recId: number;
    recDatetime: string;
    tripId: number;
    latitudeDeg: number;
    longitudeDeg: number;
    accuracyM: number | null | undefined;
    speedKmh: number | null | undefined;
    bearingDeg: number | null | undefined;
}

export interface GPSData {
    recId: number;
    recDatetime: Date;
    tripId: number;
    latitudeDeg: number;
    longitudeDeg: number;
    accuracyM: number | null | undefined;
    speedKmh: number | null | undefined;
    bearingDeg: number | null | undefined;
}