import React, { useState } from "react";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";

export default function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [err, setErr] = useState(null);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErr(null); // Clear previous errors
        try {
            // send JSON body, not `params`
            const res = await api.post("/auth/login", { email, password });
            // backend now returns { token: "…" }
            const token = res.data.token;
            localStorage.setItem("jwt", token); // Store token in local storage
            navigate("/dashboard"); // Navigate to dashboard on success
        } catch (ex) {
            console.error("Login error:", ex); // Log the error for debugging
            // Check if the error response exists and has a message, otherwise show generic message
            const errorMessage = ex.response?.data?.message || ex.response?.data || "Invalid credentials or server error.";
            setErr(errorMessage);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
            <form
                onSubmit={handleSubmit}
                className="bg-white p-8 rounded shadow-md w-full max-w-xs" // Added max-w-xs for better sizing
            >
                <h2 className="text-2xl mb-6 text-center font-semibold text-gray-700">Login</h2>
                <div className="mb-4"> {/* Added margin-bottom */}
                    <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="email">
                        Email
                    </label>
                    <input
                        id="email"
                        className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>
                <div className="mb-6"> {/* Increased margin-bottom */}
                    <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="password">
                        Password
                    </label>
                    <input
                        id="password"
                        className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 mb-3 leading-tight focus:outline-none focus:shadow-outline" // Added mb-3
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                {/* Display error message */}
                {err && <p className="text-red-500 text-xs italic mb-4">{err}</p>}
                <div className="flex items-center justify-between"> {/* To center button if needed */}
                    <button
                        type="submit"
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition duration-150 ease-in-out"
                    >
                        Login
                    </button>
                </div>
            </form>
        </div>
    );
}