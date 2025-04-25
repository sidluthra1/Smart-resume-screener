// src/pages/Dashboard.js
import React, { useState, useEffect, useRef } from "react";
import api from "../api/axios";
import { Loader2 } from "lucide-react";
import { Link } from "react-router-dom";

export default function Dashboard() {
    const [jobs, setJobs] = useState([]);
    const [resumes, setResumes] = useState([]);
    const [activeTab, setActiveTab] = useState("upload"); // "upload" | "select"

    // upload tab state
    const [file, setFile] = useState(null);
    const [candidateName, setCandidateName] = useState("");

    // select tab state
    const [selectedResumeId, setSelectedResumeId] = useState("");

    // common job picker
    const [selectedJobId, setSelectedJobId] = useState("");

    // messaging / loading
    const [msg, setMsg] = useState("");
    const [isLoading, setIsLoading] = useState(false);

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
                setResumes(
                    resumesRes.data.sort((a, b) => (b.matchScore ?? -1) - (a.matchScore ?? -1))
                );
            } catch (err) {
                console.error(err);
                setMsg("Failed to load jobs or resumes");
            }
        })();
    }, []);

    // common job change
    const onJobChange = (e) => {
        setSelectedJobId(e.target.value);
        setMsg("");
    };

    // upload tab handlers
    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
        setMsg("");
    };

    const clearUploadForm = () => {
        setCandidateName("");
        setFile(null);
        setSelectedJobId("");
        if (fileInputRef.current) fileInputRef.current.value = "";
    };

    const refreshResumes = async () => {
        const { data } = await api.get("/resume/all");
        setResumes(data.sort((a, b) => (b.matchScore ?? -1) - (a.matchScore ?? -1)));
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        setMsg("");
        if (!selectedJobId || !file || !candidateName.trim()) {
            setMsg("Pick a job, choose a file & enter candidate name.");
            return;
        }
        const formData = new FormData();
        formData.append("file", file);
        formData.append("candidateName", candidateName.trim());
        formData.append("jobId", selectedJobId);

        setIsLoading(true);
        try {
            await api.post("/resume/upload", formData);
            setMsg("Uploaded & scored!");
            await refreshResumes();
            clearUploadForm();
        } catch (err) {
            console.error(err);
            setMsg(`Upload failed: ${err.response?.data || err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    // select tab handler
    const handleScore = async (e) => {
        e.preventDefault();
        setMsg("");
        if (!selectedResumeId || !selectedJobId) {
            setMsg("Pick a resume & pick a job.");
            return;
        }

        setIsLoading(true);
        try {
            await api.post(
                "/resume/score",
                null,
                { params: { resumeId: selectedResumeId, jobId: selectedJobId } }
            );
            setMsg("Scored successfully!");
            await refreshResumes();
        } catch (err) {
            console.error(err);
            setMsg(`Scoring failed: ${err.response?.data || err.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="p-8 max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold mb-6 text-gray-800">Resume Dashboard</h1>

            {/* Tab selectors */}
            <div className="flex space-x-2 mb-4">
                <button
                    onClick={() => { setActiveTab("upload"); setMsg(""); }}
                    className={`px-4 py-2 rounded ${
                        activeTab === "upload"
                            ? "bg-blue-600 text-white"
                            : "bg-gray-200 text-gray-700"
                    }`}
                >
                    Upload & Score
                </button>
                <button
                    onClick={() => { setActiveTab("select"); setMsg(""); }}
                    className={`px-4 py-2 rounded ${
                        activeTab === "select"
                            ? "bg-blue-600 text-white"
                            : "bg-gray-200 text-gray-700"
                    }`}
                >
                    Select & Score
                </button>
            </div>

            {/* Form */}
            <form
                onSubmit={activeTab === "upload" ? handleUpload : handleScore}
                className="mb-8 p-6 bg-white shadow rounded-lg border border-gray-200 space-y-4"
            >
                <h2 className="text-xl font-semibold text-gray-700 mb-4">
                    {activeTab === "upload"
                        ? "Upload New Resume"
                        : "Score Existing Resume"}
                </h2>

                {/* Select & Score tab */}
                {activeTab === "select" && (
                    <div>
                        <label className="block mb-1 font-medium text-gray-700">
                            Select Resume:
                        </label>
                        <select
                            value={selectedResumeId}
                            onChange={(e) => { setSelectedResumeId(e.target.value); setMsg(""); }}
                            className="border p-2 rounded w-full focus:ring-blue-500 focus:border-blue-500"
                            disabled={isLoading}
                            required
                        >
                            <option value="" disabled>-- pick a resume --</option>
                            {resumes.map((r) => (
                                <option key={r.id} value={r.id}>
                                    {r.candidateName} — {r.fileName}
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                {/* Upload & Score tab */}
                {activeTab === "upload" && (
                    <>
                        <div>
                            <label className="block mb-1 font-medium text-gray-700">
                                Candidate Name:
                            </label>
                            <input
                                type="text"
                                placeholder="Enter candidate name"
                                value={candidateName}
                                onChange={(e) => setCandidateName(e.target.value)}
                                className="border p-2 rounded w-full focus:ring-blue-500 focus:border-blue-500"
                                disabled={isLoading}
                                required
                            />
                        </div>
                        <div>
                            <label className="block mb-1 font-medium text-gray-700">
                                Resume File:
                            </label>
                            <input
                                ref={fileInputRef}
                                type="file"
                                onChange={handleFileChange}
                                className="block w-full text-sm text-gray-500
                                   file:mr-4 file:py-2 file:px-4 file:rounded-full
                                   file:border-0 file:text-sm file:font-semibold
                                   file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                                accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                disabled={isLoading}
                                required
                            />
                        </div>
                    </>
                )}

                {/* common job picker */}
                <div>
                    <label className="block mb-1 font-medium text-gray-700">
                        Select Job:
                    </label>
                    <select
                        value={selectedJobId}
                        onChange={onJobChange}
                        className="border p-2 rounded w-full focus:ring-blue-500 focus:border-blue-500"
                        disabled={isLoading}
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

                <button
                    type="submit"
                    disabled={isLoading}
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
                    ) : activeTab === "upload" ? (
                        "Upload & Score"
                    ) : (
                        "Score"
                    )}
                </button>

                {msg && (
                    <p
                        className={`mt-3 p-3 rounded text-sm ${
                            msg.toLowerCase().includes("failed")
                                ? "bg-red-100 text-red-700"
                                : "bg-green-100 text-green-700"
                        }`}
                    >
                        {msg}
                    </p>
                )}
            </form>

            {/* Uploaded resumes list */}
            <div>
                <h2 className="text-2xl font-semibold mb-4 text-gray-800">
                    Uploaded Resumes
                </h2>
                {resumes.length > 0 ? (
                    <ul className="space-y-3">
                        {resumes.map((r) => (
                            <li key={r.id}>
                                <Link to={`/resume/${r.id}/match`}
                                    className="block p-4 bg-white shadow rounded-lg border border-gray-200 hover:shadow-md transition-shadow"
                                >
                                    <div className="flex justify-between items-center">
                                        <div>
                                            <strong className="text-lg text-gray-700">
                                                {r.candidateName}
                                            </strong>
                                            <span className="text-sm text-gray-500 ml-2">- {r.fileName}</span>
                                        </div>

                                        {r.matchScore > 0 ? (
                                            <span
                                                className={`ml-4 px-3 py-1 rounded-full text-sm font-semibold ${
                                                    r.matchScore >= 75
                                                        ? "bg-green-100 text-green-800"
                                                        : r.matchScore >= 50
                                                            ? "bg-yellow-100 text-yellow-800"
                                                            : "bg-red-100 text-red-800"}`}
                                            >
                  Score: {r.matchScore.toFixed(1)}%
                </span>
                                        ) : (
                                            <span className="ml-4 px-3 py-1 rounded-full text-sm font-semibold bg-gray-100 text-gray-600">
                  Not Scored Yet
                </span>
                                        )}
                                    </div>
                                </Link>
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
