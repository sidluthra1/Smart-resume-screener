// src/App.js
import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";

function PrivateRoute({ children }) {
    return localStorage.getItem("jwt")
        ? children
        : <Navigate to="/" replace />;
}

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* public login page */}
                <Route path="/" element={<Login />} />

                {/* this is your “private” dashboard route */}
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
