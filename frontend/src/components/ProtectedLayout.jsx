import React from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";

export default function ProtectedLayout() {
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem("jwt");
        navigate("/", { replace: true });
    };

    return (
        <div className="min-h-screen flex flex-col">
            {/* NAVBAR */}
            <nav className="bg-blue-600 text-white px-6 py-4 flex justify-between items-center">
                <div className="flex items-center space-x-8">
                    <Link to="/dashboard" className="text-2xl font-bold">
                        Resume Screener
                    </Link>
                    <Link to="/dashboard" className="hover:underline">
                        Dashboard
                    </Link>
                    <Link to="/jobs" className="hover:underline">
                        Jobs
                    </Link>
                    <Link to="/candidates" className="hover:underline">
                        Candidates
                    </Link>
                </div>

                {/* LOGOUT BUTTON */}
                <button
                    onClick={handleLogout}
                    className="bg-red-600 text-white px-4 py-1 rounded hover:bg-gray-100"
                >
                    Logout
                </button>
            </nav>

            {/* PAGE CONTENT */}
            <main className="flex-1 bg-gray-50 p-6">
                <Outlet />
            </main>
        </div>
    );
}
