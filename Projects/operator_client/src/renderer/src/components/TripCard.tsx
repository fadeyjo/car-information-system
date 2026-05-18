import { Trip } from "@renderer/types/trip.types"
import {
    Card,
    CardContent,
    CardMedia,
    Typography,
    Button,
    Box,
    Stack
} from "@mui/material";
import { useEffect, useState } from "react";
import { tripApi } from "@renderer/api/trip.api";

function TripCard({trip}: {trip: Trip | null | undefined}) {
    if (!trip)
        return <></>
    
    const [photo, setPhoto] = useState('')

    useEffect(() => {
        const fetchPhoto = async () => {
            setPhoto(await tripApi.getCarPhoto(trip.car.photoId))
        }

        fetchPhoto()
    }, [trip.car.photoId])

    const formatDate = (date: Date | null | undefined) => {
        if (!date) return "-";

        return new Intl.DateTimeFormat("ru-RU", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(date);
    };

    return (
        <Card
            sx={{
                display: "flex",
                alignItems: "stretch",
                borderRadius: 3,
                boxShadow: 3,
                overflow: "hidden"
            }}
        >
            <CardMedia
                component="img"
                image={photo}
                alt={`${trip.car.brandName} ${trip.car.modelName}`}
                sx={{
                    width: 220,
                    objectFit: "cover"
                }}
            />

            <Box
                sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "space-between",
                    flex: 1
                }}
            >
                <CardContent>
                    <Stack spacing={1}>
                        <Typography variant="h5" fontWeight={700}>
                            {trip.car.stateNumber ?? "Без номера"}
                        </Typography>

                        <Typography variant="body1" color="text.secondary">
                            {trip.car.person.lastName}{" "}
                            {trip.car.person.firstName}
                        </Typography>

                        <Typography variant="body2" color="text.secondary">
                            Начало: {formatDate(trip.startDatetime)}
                        </Typography>

                        <Typography variant="body2" color="text.secondary">
                            Окончание: {formatDate(trip.endDatetime)}
                        </Typography>
                    </Stack>
                </CardContent>

                <Box sx={{ p: 2, pt: 0 }}>
                    <Button variant="contained">
                        Открыть
                    </Button>
                </Box>
            </Box>
        </Card>
    );
}

export default TripCard