import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import JobsPage         from "./pages/JobsPage";
import CandidatesPage   from "./pages/CandidatesPage";
import CandidateDetailPage from "./pages/CandidateDetailPage";
import Login            from "./pages/Login";
import Signup           from "./pages/Signup";
import Dashboard        from "./pages/Dashboard";
import MatchAnalysisPage from "./pages/MatchAnalysisPage";
import ProtectedLayout  from "./components/ProtectedLayout";
import JobDetailPage from "./pages/JobDetailPage";

function PrivateRoute({ children }) {
    const token = localStorage.getItem("jwt");
    // if no token, send to "/" (our login)
    return token ? children : <Navigate to="/" replace />;
}

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* 1️⃣ Login is at "/" */}
                <Route path="/" element={<Login />} />
                <Route path="/signup" element={<Signup />} />

                {/* 2️⃣ Protected “shell” */}
                <Route
                    element={
                        <PrivateRoute>
                            <ProtectedLayout />
                        </PrivateRoute>
                    }
                >
                    <Route path="dashboard" element={<Dashboard />} />
                    <Route path="/resume/:resumeId/match"  element={<MatchAnalysisPage />}/>
                    <Route path="jobs"      element={<JobsPage />} />
                    <Route path="/jobs/:id"  element={<JobDetailPage />} />
                    <Route path="candidates" element={<CandidatesPage />} />
                    <Route path="/candidates/:id" element={<CandidateDetailPage />} />
                </Route>

                {/* 3️⃣ Anything else → back to login */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
