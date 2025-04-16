import React, { useEffect, useState } from "react";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";

export default function Dashboard() {
    const [file, setFile] = useState(null);
    const [candidateName, setCandidateName] = useState("");
    const [resumes, setResumes] = useState([]);
    const [msg, setMsg] = useState("");

    const fetchResumes = async () => {
        const res = await api.get("/resume/all");
        setResumes(res.data);
    };

    useEffect(() => { fetchResumes(); }, []);

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) return;
        const form = new FormData();
        form.append("file", file);
        form.append("candidateName", candidateName);
        try {
            await api.post("/resume/upload", form, {
                headers: { "Content-Type": "multipart/form-data" },
            });
            setMsg("Upload successful!");
            setFile(null);
            setCandidateName("");
            fetchResumes();
        } catch (ex) {
            setMsg("Upload failed: " + ex.response?.data?.message);
        }
    };

    return (
        <div className="p-8 max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold mb-6">Resume Dashboard</h1>

            {/* Upload card */}
            <form
                onSubmit={handleUpload}
                className="border p-6 rounded-lg mb-10 bg-white shadow"
            >
                <h2 className="text-xl font-semibold mb-4">Upload Resume</h2>
                <input
                    className="border p-2 w-full mb-3 rounded"
                    type="text"
                    placeholder="Candidate Name"
                    value={candidateName}
                    onChange={(e) => setCandidateName(e.target.value)}
                    required
                />
                <input
                    className="mb-4"
                    type="file"
                    accept=".pdf,.docx"
                    onChange={(e) => setFile(e.target.files[0])}
                    required
                />
                <button
                    className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
                    type="submit"
                >
                    Upload
                </button>
                {msg && <p className="mt-2 text-blue-600">{msg}</p>}
            </form>

            {/* List resumes */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {resumes.map((r) => (
                    <div
                        key={r.id}
                        className="border p-4 rounded bg-gray-50 shadow-sm flex flex-col"
                    >
                        <span className="font-medium">{r.candidateName}</span>
                        <span className="text-sm text-gray-500">{r.fileName}</span>
                        <span className="text-xs text-gray-400 mt-auto">
              {new Date(r.uploadDate).toLocaleString()}
            </span>
                    </div>
                ))}
            </div>
        </div>
    );
}
