import { tripApi } from "@renderer/api/trip.api";
import TripCard from "@renderer/components/TripCard";
import { Trip } from "@renderer/types/trip.types";

import { useEffect, useState } from "react";

import {
  Box,
  Grid,
  Typography,
  Paper,
  Stack,
  Chip,
  Divider,
  CircularProgress,
} from "@mui/material";
import { mqttService } from "@renderer/services/mqtt.service";

function MonitoringPage(): React.JSX.Element {
  const [endedTrips, setEndedTrips] = useState<Trip[]>([]);
  const [currentTrips, setCurrentTrips] = useState<Trip[]>([]);

  const [loading, setLoading] = useState(true);

  const newTripCallback = (newTripId: number) => {
    const fetchTrip = async (): Promise<void> => {
      const trip = await tripApi.getTrip(newTripId)
      setCurrentTrips(prev => [trip, ...prev])
    }

    fetchTrip()
  }

  const endTripCallback = (endTripId: number) => {
    const fetchTrip = async (): Promise<void> => {
      const trip = await tripApi.getTrip(endTripId)
      setEndedTrips(prev => [...prev, trip])
    }

    setCurrentTrips((prev) => prev.filter(item => item.tripId !== endTripId));
    fetchTrip()
  }

  useEffect(() => {
    const fetchTrips = async () => {
      try {
        setLoading(true);

        const allTrips = await tripApi.getAllTrips();

        setEndedTrips(allTrips.ended);
        setCurrentTrips(allTrips.current);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    };

    mqttService.connect()

    mqttService.newTripCallback = newTripCallback
    mqttService.subscribe("new-trip/+")

    mqttService.endTripCallback = endTripCallback
    mqttService.subscribe("end-trip/+")

    fetchTrips();
    
    return () => {
      mqttService.disconnect()
    }
  }, []);



  if (loading) {
    return (
      <Box
        sx={{
          height: "100vh",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          flexDirection: "column",
          gap: 2,
        }}
      >
        <CircularProgress size={60} />

        <Typography variant="h6" color="text.secondary">
          Загрузка поездок...
        </Typography>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        p: 4,
        width: "100%",
        backgroundColor: "#f5f5f5",
        height: "98vh",
        overflow: "hidden",
        boxSizing: "border-box",
      }}
    >
      <Paper
        elevation={3}
        sx={{
          mb: 4,
          p: 3,
          borderRadius: 4,
          
        }}
      >
        <Stack
          direction="row"
          gap={2}
        >
          <Box>
            <Typography variant="h4" fontWeight={700}>
              Мониторинг поездок
            </Typography>

            <Typography
              variant="body1"
              color="text.secondary"
            >
              Информация о текущих и завершённых поездках
            </Typography>
          </Box>

          <Stack direction="row" spacing={2}>
            <Chip
              color="success"
              label={`Текущих: ${currentTrips.length}`}
            />

            <Chip
              color="default"
              label={`Завершённых: ${endedTrips.length}`}
            />
          </Stack>
        </Stack>
      </Paper>

      <Grid container spacing={4}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper
            elevation={2}
            sx={{
              p: 3,
              borderRadius: 4
            }}
          >
            <Typography
              variant="h5"
              fontWeight={700}
              gutterBottom
            >
              Текущие поездки
            </Typography>

            <Divider sx={{ mb: 3 }} />

            <Stack spacing={3} sx={{overflowY: "auto", height: "70vh"}}>
              {currentTrips.length > 0 ? (
                currentTrips.map((trip) => (
                  <TripCard
                    key={trip.tripId}
                    trip={trip}
                  />
                ))
              ) : (
                <Typography color="text.secondary">
                  Нет активных поездок
                </Typography>
              )}
            </Stack>
          </Paper>
        </Grid>

        {/* Завершённые поездки */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper
            elevation={2}
            sx={{
              p: 3,
              borderRadius: 4
            }}
          >
            <Typography
              variant="h5"
              fontWeight={700}
              gutterBottom
            >
              Завершённые поездки
            </Typography>

            <Divider sx={{ mb: 3 }} />

            <Stack spacing={3} sx={{overflowY: "auto", height: "70vh"}}>
              {endedTrips.length > 0 ? (
                endedTrips.map((trip) => (
                  <TripCard
                    key={trip.tripId}
                    trip={trip}
                  />
                ))
              ) : (
                <Typography color="text.secondary">
                  Нет завершённых поездок
                </Typography>
              )}
            </Stack>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

export default MonitoringPage;