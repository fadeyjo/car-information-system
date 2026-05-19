import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import MonitoringPage from "./pages/MonitoringPage";
import { ProtectedRoute } from "./pages/ProtectedRoute";
import { useAuth } from "./hooks/useAuth";
import Sidebar from "./components/Sidebar";
import { TripDetails } from "./components/TripDetails";

function App(): React.JSX.Element {
  const isAuthenticated = useAuth()

  return (
    <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            
            <Route
              path="/monitoring"
              element={
                <div style={{ display: "flex" }}>
                  <Sidebar />
              
                  <div style={{ flex: 1 }}>
                    <MonitoringPage />
                  </div>
                </div>
              }
            />

            <Route
              path="/monitoring/:id"
              element={
                <div style={{ display: "flex" }}>
                  <Sidebar />
              
                  <div style={{ flex: 1 }}>
                    <TripDetails />
                  </div>
                </div>
              }
            />
          </Route>

          <Route
            path="*"
            element={
              isAuthenticated
                ? <Navigate to="/monitoring" replace />
                : <Navigate to="/login" replace />
            }
          />

        </Routes>
    </Router>
  )
}

export default App
