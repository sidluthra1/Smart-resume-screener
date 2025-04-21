// src/App.js
import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Signup from "./pages/Signup";

// Your PrivateRoute component remains the same

function PrivateRoute({ children }) {
    // Check if JWT exists
    const token = localStorage.getItem("jwt");
    // You might want to add token validation/expiration check here in a real app
    return token ? children : <Navigate to="/" replace />;
}


export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Public Routes */}
                <Route path="/" element={<Login />} />
                <Route path="/signup" element={<Signup />} />


                {/* Private Routes */}
                <Route
                    path="/dashboard"
                    element={
                        <PrivateRoute>
                            <Dashboard />
                        </PrivateRoute>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}