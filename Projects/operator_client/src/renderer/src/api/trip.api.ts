import { AllTrips, AllTripsDto, GPSData, GPSDataDto, Trip, TripDto } from "@renderer/types/trip.types";
import api from "./axios";
import { mapAllTripsDto, mapGpsDataList, mapTripDto } from "@renderer/mappers/trip.mappers";

export const tripApi = {

  async getCarPhoto(photoId: number) {
    const response = await api.get(
      `/car-photos/${photoId}`,
      {
        responseType: "blob",
      }
    );

    return URL.createObjectURL(response.data);
  },

  async getTrip(tripId: number): Promise<Trip> {
  
      const response =
        await api.get<TripDto>(
          `/trips/${tripId}`
        );
  
      return mapTripDto(
        response.data
      );
    },
  
  async getAllTrips(): Promise<AllTrips> {
  
      const response =
        await api.get<AllTripsDto>(
          '/trips/all'
        );
        
      return mapAllTripsDto(response.data)
    },
  
  async getAllGpsData(tripId: number): Promise<GPSData[]> {
  
      const response =
        await api.get<GPSDataDto[]>(
          `/gps-data/trip/${tripId}`
        );
        
      return mapGpsDataList(response.data)
    },
};