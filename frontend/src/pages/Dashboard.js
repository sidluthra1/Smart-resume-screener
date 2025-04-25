// src/pages/Dashboard.js
import React, { useState, useEffect, useRef } from "react";
import api from "../api/axios";
import { Loader2 } from "lucide-react";

export default function Dashboard() {
    const [jobs, setJobs] = useState([]);
    const [selectedJobId, setSelectedJobId] = useState("");
    const [resumes, setResumes] = useState([]);
    const [file, setFile] = useState(null);
    const [candidateName, setCandidateName] = useState("");
    const [msg, setMsg] = useState("");
    const [isLoading, setIsLoading] = useState(false);           // ← new
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (!localStorage.getItem("jwt")) return;
        (async () => {
            try {
                const [jobsRes, resumesRes] = await Promise.all([
                    api.get("/job/all"),
                    api.get("/resume/all"),
                ]);
                setJobs(jobsRes.data);
                setResumes(resumesRes.data.sort((a, b) => (b.matchScore ?? -1) - (a.matchScore ?? -1)));
            } catch (err) {
                console.error(err);
                setMsg("Failed to load jobs or resumes");
            }
        })();
    }, []);

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        setMsg("");

        if (!selectedJobId || !file || !candidateName.trim()) {
            setMsg("Please select a job, pick a file, and enter a candidate name.");
            return;
        }

        const formData = new FormData();
        formData.append("file", file);
        formData.append("candidateName", candidateName.trim());
        formData.append("jobId", selectedJobId);

        setIsLoading(true);  // ← start loading
        try {
            await api.post("/resume/upload", formData);
            setMsg("Uploaded & scored successfully!");

            const updatedResumes = await api.get("/resume/all");
            setResumes(updatedResumes.data.sort((a, b) => (b.matchScore ?? -1) - (a.matchScore ?? -1)));

            setSelectedJobId("");
            setCandidateName("");
            setFile(null);
            if (fileInputRef.current) fileInputRef.current.value = "";
        } catch (err) {
            console.error(err);
            if (err.response?.status === 401 || err.response?.status === 403) {
                setMsg("Upload forbidden: Your session may have expired. Please log in again.");
            } else {
                setMsg(`Upload failed: ${err.response?.data?.message || err.message}`);
            }
        } finally {
            setIsLoading(false);  // ← stop loading
        }
    };

    return (
        <div className="p-8 max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold mb-6 text-gray-800">Resume Dashboard</h1>

            <form onSubmit={handleUpload} className="mb-8 p-6 bg-white shadow rounded-lg border border-gray-200 space-y-4">
                <h2 className="text-xl font-semibold text-gray-700 mb-4">Upload New Resume</h2>

                <div>
                    <label htmlFor="job-select" className="block mb-1 font-medium text-gray-700">Select Job:</label>
                    <select
                        id="job-select"
                        value={selectedJobId}
                        onChange={(e) => setSelectedJobId(e.target.value)}
                        className="border p-2 rounded w-full focus:ring-blue-500 focus:border-blue-500"
                        disabled={isLoading}  // ← disable while loading
                        required
                    >
                        <option value="" disabled>-- pick a job --</option>
                        {jobs.map((j) => (
                            <option key={j.id} value={j.id}>
                                {j.title}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label htmlFor="candidate-name" className="block mb-1 font-medium text-gray-700">Candidate Name:</label>
                    <input
                        id="candidate-name"
                        type="text"
                        placeholder="Enter candidate name"
                        value={candidateName}
                        onChange={(e) => setCandidateName(e.target.value)}
                        className="border p-2 rounded w-full focus:ring-blue-500 focus:border-blue-500"
                        disabled={isLoading}  // ← disable while loading
                        required
                    />
                </div>

                <div>
                    <label htmlFor="resume-file" className="block mb-1 font-medium text-gray-700">Resume File:</label>
                    <input
                        id="resume-file"
                        ref={fileInputRef}
                        type="file"
                        onChange={handleFileChange}
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        disabled={isLoading}  // ← disable while loading
                        required
                    />
                </div>

                <button
                    type="submit"
                    disabled={isLoading}  // ← disable while loading
                    className={`
                        flex items-center justify-center 
                        bg-blue-600 text-white px-6 py-2 rounded 
                        hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 
                        transition duration-150 ease-in-out
                        ${isLoading ? "opacity-50 cursor-not-allowed" : ""}
                    `}
                >
                    {isLoading ? (
                        <Loader2 className="animate-spin h-5 w-5" />
                    ) : (
                        "Upload & Score"
                    )}
                </button>

                {msg && (
                    <p className={`
                        mt-3 p-3 rounded text-sm 
                        ${msg.startsWith("Upload failed") || msg.startsWith("Upload forbidden")
                        ? "bg-red-100 text-red-700"
                        : "bg-green-100 text-green-700"}
                    `}>
                        {msg}
                    </p>
                )}
            </form>

            <div>
                <h2 className="text-2xl font-semibold mb-4 text-gray-800">Uploaded Resumes</h2>
                {resumes.length > 0 ? (
                    <ul className="space-y-3">
                        {resumes.map((r) => (
                            <li key={r.id} className="p-4 bg-white shadow rounded-lg border border-gray-200 flex justify-between items-center">
                                <div>
                                    <strong className="text-lg text-gray-700">{r.candidateName}</strong>
                                    <span className="text-sm text-gray-500 ml-2">- {r.fileName}</span>
                                </div>
                                {r.matchScore != null ? (
                                    <span className={`
                                        ml-4 px-3 py-1 rounded-full text-sm font-semibold
                                        ${r.matchScore >= 75 ? 'bg-green-100 text-green-800'
                                        : r.matchScore >= 50 ? 'bg-yellow-100 text-yellow-800'
                                            : 'bg-red-100 text-red-800'}
                                    `}>
                                        Score: {r.matchScore.toFixed(1)}%
                                    </span>
                                ) : (
                                    <span className="ml-4 px-3 py-1 rounded-full text-sm font-semibold bg-gray-100 text-gray-600">
                                        Not Scored
                                    </span>
                                )}
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p className="text-gray-500">No resumes uploaded yet.</p>
                )}
            </div>
        </div>
    );
}
