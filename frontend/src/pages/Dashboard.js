// src/pages/Dashboard.js
import React, { useState, useEffect, useRef } from "react";
import api from "../api/axios";

export default function Dashboard() {
    const [resumes, setResumes] = useState([]);
    const [file, setFile] = useState(null);
    const [candidateName, setCandidateName] = useState("");
    const [msg, setMsg] = useState("");
    const fileInputRef = useRef(null);

    // load all resumes on mount
    useEffect(() => {
        async function loadResumes() {
            try {
                const res = await api.get("/resume/all");
                setResumes(res.data);
            } catch (err) {
                console.error("Failed to load resumes:", err);
                setMsg("Failed to load resumes");
            }
        }
        loadResumes();
    }, []);

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        setMsg("");
        if (!file || !candidateName.trim()) {
            setMsg("Please select a file and enter a candidate name.");
            return;
        }

        const formData = new FormData();
        formData.append("file", file);
        formData.append("candidateName", candidateName);

        try {
            await api.post("/resume/upload", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });
            setMsg("Uploaded successfully!");
            // refresh list
            const all = await api.get("/resume/all");
            setResumes(all.data);
            // reset inputs
            setCandidateName("");
            setFile(null);
            if (fileInputRef.current) fileInputRef.current.value = "";
        } catch (err) {
            console.error("Upload error:", err);
            const status = err.response?.status;
            if (status === 403) {
                setMsg("Upload forbidden: please log in again.");
            } else {
                setMsg(`Upload failed: ${err.response?.data || err.message}`);
            }
        }
    };

    return (
        <div className="p-8">
            <h1 className="text-2xl mb-4">Resume Dashboard</h1>

            <form onSubmit={handleUpload} className="mb-6 space-y-2">
                <input
                    ref={fileInputRef}
                    type="file"
                    onChange={handleFileChange}
                    className="block"
                />

                <input
                    type="text"
                    placeholder="Candidate Name"
                    value={candidateName}
                    onChange={(e) => setCandidateName(e.target.value)}
                    className="border p-2 rounded w-64"
                />

                <button
                    type="submit"
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                >
                    Upload Resume
                </button>

                {msg && <p className="mt-2 text-red-600">{msg}</p>}
            </form>

            <ul className="space-y-1">
                {resumes.map((r) => (
                    <li key={r.id}>
                        <strong>{r.candidateName}</strong> — {r.fileName}
                    </li>
                ))}
            </ul>
        </div>
    );
}
