import { tripApi } from "@renderer/api/trip.api";
import { mqttService } from "@renderer/services/mqtt.service";
import { GPSData, GPSDataMqtt, Trip } from "@renderer/types/trip.types";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Fab,
  IconButton,
  Paper,
  Tooltip,
  Typography,
} from "@mui/material";
import {
  DirectionsCar,
  GpsFixed,
  GpsNotFixed,
  MyLocation,
  Speed,
  Timeline,
} from "@mui/icons-material";
import L, { Map as LeafletMap, Marker, Polyline } from "leaflet";
import "leaflet/dist/leaflet.css";


const createCarIcon = (bearingDeg: number) =>
  L.divIcon({
    className: "",
    html: `
      <div style="
        width: 36px; height: 36px;
        display: flex; align-items: center; justify-content: center;
        transform: rotate(${bearingDeg}deg);
        transition: transform 0.4s ease;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.45));
      ">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="36" height="36">
          <circle cx="12" cy="12" r="12" fill="#1565C0" opacity="0.15"/>
          <path d="M12 3 L16 9 H14 V21 H10 V9 H8 Z" fill="#1976D2" stroke="#fff" stroke-width="0.8"/>
        </svg>
      </div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18],
  });

const accuracyCircleOptions: L.CircleOptions = {
  color: "#1976D2",
  fillColor: "#1976D2",
  fillOpacity: 0.08,
  weight: 1,
};

export const TripDetails = (): React.JSX.Element => {
  const { id } = useParams();
  const tripId = Number(id);

  const [trip, setTrip] = useState<Trip | null>(null);
  const [loading, setLoading] = useState(true);
  const [followMarker, setFollowMarker] = useState(true);
  const [currentSpeed, setCurrentSpeed] = useState<number | null>(null);
  const [pointCount, setPointCount] = useState(0);

  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const markerRef = useRef<Marker | null>(null);
  const accuracyCircleRef = useRef<L.Circle | null>(null);
  const polylineRef = useRef<Polyline | null>(null);
  const latlngsRef = useRef<L.LatLngTuple[]>([]);
  const followRef = useRef(true);

  useEffect(() => {
    followRef.current = followMarker;
  }, [followMarker]);

  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: [55.751244, 37.618423],
      zoom: 15,
      zoomControl: false,
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "© OpenStreetMap contributors",
      maxZoom: 19,
    }).addTo(map);

    L.control.zoom({ position: "bottomright" }).addTo(map);

    map.on("mousedown", () => {
      setFollowMarker(false);
      followRef.current = false;
    });

    // Route polyline
    const polyline = L.polyline([], {
      color: "#1976D2",
      weight: 4,
      opacity: 0.85,
      lineJoin: "round",
    }).addTo(map);
    polylineRef.current = polyline;

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  const addPointToMap = useCallback(
    (
      lat: number,
      lng: number,
      bearing: number | null | undefined,
      accuracy: number | null | undefined,
      speed: number | null | undefined,
      panCamera = false
    ) => {
      const map = mapRef.current;
      if (!map) return;

      const latlng: L.LatLngTuple = [lat, lng];
      latlngsRef.current.push(latlng);
      polylineRef.current?.setLatLngs(latlngsRef.current);

      const icon = createCarIcon(bearing ?? 0);
      if (!markerRef.current) {
        markerRef.current = L.marker(latlng, { icon, zIndexOffset: 1000 }).addTo(map);
      } else {
        markerRef.current.setLatLng(latlng).setIcon(icon);
      }

      if (accuracy != null) {
        if (!accuracyCircleRef.current) {
          accuracyCircleRef.current = L.circle(latlng, {
            ...accuracyCircleOptions,
            radius: accuracy,
          }).addTo(map);
        } else {
          accuracyCircleRef.current.setLatLng(latlng).setRadius(accuracy);
        }
      }

      setCurrentSpeed(speed ?? null);
      setPointCount((c) => c + 1);

      if (panCamera && followRef.current) {
        map.panTo(latlng, { animate: true, duration: 0.5 });
      }
    },
    []
  );

  useEffect(() => {
    const fetch = async () => {
      setLoading(true);
      const t = await tripApi.getTrip(tripId);
      setTrip(t);
      setLoading(false);
    };
    fetch();
  }, [id]);

  useEffect(() => {
    if (!trip) return;

    const loadOld = async () => {
      const gpsData: GPSData[] = await tripApi.getAllGpsData(trip.tripId);
      if (!gpsData.length) return;

      // Batch-add historical points (no camera follow spam)
      gpsData.forEach((pt, i) => {
        addPointToMap(
          pt.latitudeDeg,
          pt.longitudeDeg,
          pt.bearingDeg,
          pt.accuracyM,
          pt.speedKmh,
          false
        );
      });

      if (mapRef.current && latlngsRef.current.length > 1) {
        mapRef.current.fitBounds(L.latLngBounds(latlngsRef.current), {
          padding: [40, 40],
          maxZoom: 17,
        });
      } else if (mapRef.current && latlngsRef.current.length === 1) {
        mapRef.current.setView(latlngsRef.current[0], 16);
      }
    };

    loadOld();

    if (!trip.endDatetime) {
      // Live trip — subscribe MQTT
      mqttService.connect();
      mqttService.gpsDataCallback = (gpsData: GPSDataMqtt) => {
        addPointToMap(
          gpsData.latitudeDeg,
          gpsData.longitudeDeg,
          gpsData.bearingDeg,
          gpsData.accuracyM,
          gpsData.speedKmh,
          true // pan camera if follow is on
        );
      };
      mqttService.subscribe(`gps/new-data/${trip.tripId}`);

      return () => {
        mqttService.disconnect();
      };
    }
  }, [trip, addPointToMap]);

 
  const handleFollowClick = () => {
    setFollowMarker(true);
    followRef.current = true;
    const last = latlngsRef.current.at(-1);
    if (last && mapRef.current) {
      mapRef.current.panTo(last, { animate: true, duration: 0.6 });
    }
  };

  const isLive = trip ? !trip.endDatetime : false;

  return (
    <Box
      sx={{
        position: "relative",
        width: "100%",
        height: "100vh",
        bgcolor: "#0d1117",
        overflow: "hidden",
        fontFamily: "'IBM Plex Mono', monospace",
      }}
    >
      <Box
        ref={mapContainerRef}
        sx={{ position: "absolute", inset: 0, zIndex: 0 }}
      />

      {loading && (
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            zIndex: 10,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            bgcolor: "rgba(13,17,23,0.75)",
            backdropFilter: "blur(4px)",
          }}
        >
          <CircularProgress size={48} sx={{ color: "#1976D2" }} />
        </Box>
      )}

      {trip && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: 16,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 5,
            display: "flex",
            alignItems: "center",
            gap: 1.5,
            px: 2.5,
            py: 1,
            bgcolor: "rgba(13,17,23,0.82)",
            backdropFilter: "blur(10px)",
            borderRadius: 3,
            border: "1px solid rgba(255,255,255,0.08)",
            color: "#e6edf3",
            whiteSpace: "nowrap",
          }}
        >
          <DirectionsCar sx={{ color: "#1976D2", fontSize: 20 }} />
          <Typography variant="body2" sx={{ fontWeight: 600, letterSpacing: 0.5 }}>
            {trip.car.brandName} {trip.car.modelName}
          </Typography>
          <Typography
            variant="caption"
            sx={{ color: "rgba(230,237,243,0.5)", mx: 0.5 }}
          >
            ·
          </Typography>
          <Typography variant="caption" sx={{ color: "rgba(230,237,243,0.6)" }}>
            {trip.car.stateNumber ?? "—"}
          </Typography>

          {isLive && (
            <Chip
              label="LIVE"
              size="small"
              icon={
                <Box
                  sx={{
                    width: 6,
                    height: 6,
                    borderRadius: "50%",
                    bgcolor: "#f44336",
                    animation: "pulse 1.4s infinite",
                    "@keyframes pulse": {
                      "0%,100%": { opacity: 1 },
                      "50%": { opacity: 0.3 },
                    },
                    ml: "6px !important",
                  }}
                />
              }
              sx={{
                bgcolor: "rgba(244,67,54,0.15)",
                color: "#f44336",
                border: "1px solid rgba(244,67,54,0.35)",
                height: 22,
                fontSize: "0.65rem",
                fontWeight: 700,
                letterSpacing: 1,
                "& .MuiChip-icon": { color: "inherit" },
              }}
            />
          )}
        </Paper>
      )}

      <Card
        elevation={0}
        sx={{
          position: "absolute",
          bottom: 24,
          left: 16,
          zIndex: 5,
          minWidth: 160,
          bgcolor: "rgba(13,17,23,0.82)",
          backdropFilter: "blur(10px)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 3,
          color: "#e6edf3",
        }}
      >
        <CardContent sx={{ p: "12px 16px !important" }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
            <Speed sx={{ color: "#1976D2", fontSize: 18 }} />
            <Typography variant="caption" sx={{ color: "rgba(230,237,243,0.55)", letterSpacing: 0.5 }}>
              СКОРОСТЬ
            </Typography>
          </Box>
          <Typography
            variant="h5"
            sx={{ fontWeight: 700, lineHeight: 1, color: "#fff", mb: 0.5 }}
          >
            {currentSpeed != null ? Math.round(currentSpeed) : "—"}
            <Typography
              component="span"
              variant="caption"
              sx={{ color: "rgba(230,237,243,0.5)", ml: 0.5 }}
            >
              км/ч
            </Typography>
          </Typography>

          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mt: 1.5 }}>
            <Timeline sx={{ color: "#1976D2", fontSize: 18 }} />
            <Typography variant="caption" sx={{ color: "rgba(230,237,243,0.55)", letterSpacing: 0.5 }}>
              ТОЧЕК
            </Typography>
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 600, color: "#90caf9" }}>
            {pointCount}
          </Typography>
        </CardContent>
      </Card>

      <Tooltip
        title={followMarker ? "Слежение включено" : "Вернуться к маркеру"}
        placement="left"
      >
        <Fab
          size="medium"
          onClick={handleFollowClick}
          sx={{
            position: "absolute",
            bottom: 100,
            right: 16,
            zIndex: 5,
            bgcolor: followMarker ? "#1976D2" : "rgba(13,17,23,0.85)",
            color: followMarker ? "#fff" : "#90caf9",
            border: followMarker
              ? "2px solid #42a5f5"
              : "1px solid rgba(144,202,249,0.35)",
            backdropFilter: "blur(8px)",
            boxShadow: followMarker
              ? "0 0 16px rgba(25,118,210,0.55)"
              : "0 2px 12px rgba(0,0,0,0.4)",
            transition: "all 0.3s ease",
            "&:hover": {
              bgcolor: followMarker ? "#1565C0" : "rgba(25,118,210,0.25)",
            },
          }}
        >
          {followMarker ? <GpsFixed /> : <GpsNotFixed />}
        </Fab>
      </Tooltip>

      <style>{`
        .leaflet-control-zoom a {
          background: rgba(13,17,23,0.85) !important;
          color: #90caf9 !important;
          border-color: rgba(144,202,249,0.2) !important;
          backdrop-filter: blur(8px);
        }
        .leaflet-control-zoom a:hover {
          background: rgba(25,118,210,0.35) !important;
        }
        .leaflet-control-attribution {
          background: rgba(13,17,23,0.6) !important;
          color: rgba(230,237,243,0.35) !important;
          font-size: 10px;
        }
        .leaflet-control-attribution a { color: rgba(144,202,249,0.5) !important; }
      `}</style>
    </Box>
  );
};