import { useEffect, useState } from "react";
import {
  Drawer, List, ListItem, ListItemButton, ListItemText,
  Box, Avatar, Typography, Divider, Button,
} from "@mui/material";
import LogoutIcon from "@mui/icons-material/Logout";
import { personApi } from "@renderer/api/person.api";
import { Person } from "@renderer/types/person.types";
import { useNavigate } from "react-router-dom";
import { tokenService } from "@renderer/services/token.service";
import { authApi } from "@renderer/api/auth.api";

export default function Sidebar() {
  const [person, setPerson] = useState<Person | null>(null);
  const [avatarUrl, setAvatarUrl] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPerson = async () => {
      try {
        const data = await personApi.getPersonData();
        setPerson(data);
        const avatar = await personApi.getAvatar(data.avatarId);
        setAvatarUrl(avatar);
      } catch (error) {
        console.error(error);
      }
    };
    fetchPerson();
  }, []);

  const drawerWidth = 250;

  const handleLogout = async () => {
    await authApi.logout()

    tokenService.clear()
    navigate("/login");
  };

  return (
    <Drawer
      variant="permanent"
      anchor="left"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        "& .MuiDrawer-paper": {
          width: drawerWidth,
          boxSizing: "border-box",
        },
      }}
    >
      {/* Растягиваем Box на всю высоту и делаем flex-колонку */}
      <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>

        {/* Верхняя часть: профиль + навигация */}
        <Box sx={{ flexGrow: 1, overflow: "auto" }}>
          {person && (
            <Box
              sx={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 2,
                p: 3,
                borderBottom: "1px solid #ccc",
              }}
            >
              <Avatar src={avatarUrl} sx={{ width: 200, height: 200 }} />
              <Box>
                <Typography variant="subtitle1">
                  {person.lastName} {person.firstName}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {person.email}
                </Typography>
              </Box>
            </Box>
          )}

          <List>
            <ListItem disablePadding>
              <ListItemButton onClick={() => navigate("/monitoring")}>
                <ListItemText primary="Мониторинг" />
              </ListItemButton>
            </ListItem>
          </List>
        </Box>

        <Box>
          <Divider />
          <Box sx={{ p: 2 }}>
            <Button
              fullWidth
              variant="outlined"
              color="error"
              startIcon={<LogoutIcon />}
              onClick={handleLogout}
            >
              Выйти
            </Button>
          </Box>
        </Box>

      </Box>
    </Drawer>
  );
}