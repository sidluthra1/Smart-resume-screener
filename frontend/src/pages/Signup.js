import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import logo from "../Resume Matcher Logo.png";

export default function Signup() {
    const [name,        setName]        = useState("");
    const [email,       setEmail]       = useState("");
    const [password,    setPassword]    = useState("");
    const [confirm,     setConfirm]     = useState("");
    const [err,         setErr]         = useState(null);
    const navigate                     = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErr(null);

        if (password !== confirm) {
            setErr("Passwords do not match");
            return;
        }

        try {
            await api.post("/auth/signup", { name, email, password });
            const { data } = await api.post("/auth/login", { email, password });
            localStorage.setItem("jwt", data.token);
            navigate("/dashboard");
        } catch (ex) {
            const msg =
                ex.response?.data?.message ||
                ex.response?.data ||
                "Signup failed. Try a different email.";
            setErr(msg);
        }
    };

    return (
        <div className="min-h-screen flex flex-col items-center justify-start bg-gray-100 pt-10 px-4">
            {/* logo ------------------------------------------------------------ */}
            <img
                src={logo}
                alt="ResumeMatch AI"
                className="w-40 h-40 md:w-48 md:h-48 object-contain mb-4"
            />

            {/* subtitle -------------------------------------------------------- */}
            <p className="text-center text-sm text-gray-500 mb-6">
                Create an account to get started
            </p>

            {/* form ------------------------------------------------------------ */}
            <form
                onSubmit={handleSubmit}
                className="w-full max-w-sm mx-auto flex flex-col items-center space-y-4"
            >
                <input
                    type="text"
                    placeholder="Full Name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    className="w-full rounded border px-3 py-2 shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />

                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className="w-full rounded border px-3 py-2 shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />

                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="w-full rounded border px-3 py-2 shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />

                <input
                    type="password"
                    placeholder="Confirm Password"
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                    required
                    className="w-full rounded border px-3 py-2 shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />

                {err && <p className="text-xs text-red-500 text-center">{err}</p>}

                {/* sign‑up button */}
                <button
                    type="submit"
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded transition"
                >
                    Sign Up
                </button>
            </form>

            {/* link back to login --------------------------------------------- */}
            <div className="mt-4 flex flex-col items-center">
                <Link
                    to="/"
                    className="text-xs text-blue-600 hover:text-blue-700 no-underline"
                >
                    Already have an account? Sign In
                </Link>
            </div>
        </div>
    );
}
