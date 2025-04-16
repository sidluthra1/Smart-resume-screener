// src/App.js
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import './App.css'; // you can swap this for './index.css' once Tailwind is wired up

// Protects routes by checking for a JWT in localStorage
function PrivateRoute({ children }) {
  const token = localStorage.getItem('jwt');
  return token ? children : <Navigate to="/" replace />;
}

export default function App() {
  return (
      <BrowserRouter>
        <Routes>
          {/* Public login page */}
          <Route path="/" element={<Login />} />

          {/* Dashboard is protected */}
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
