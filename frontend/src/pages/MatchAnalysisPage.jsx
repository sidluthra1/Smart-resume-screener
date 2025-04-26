// src/pages/MatchAnalysisPage.jsx  – fixed to call /analysis endpoint
import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useSearchParams } from "react-router-dom";
import api from "../api/axios";
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
} from "../components/ui/card";
import { Button } from "../components/ui/button";

export default function MatchAnalysisPage() {
    const { resumeId } = useParams();
    const [searchParams] = useSearchParams();
    const jobId = searchParams.get("jobId");
    const navigate = useNavigate();

    const [resume, setResume] = useState(null);
    const [job, setJob]       = useState(null);   // optional
    const [error, setError]   = useState("");


    // ── fetch analysis JSON + (optional) job DTO ──────────────────────────
    useEffect(() => {
        if (!resumeId) { setError("Missing resumeId"); return; }

        // 1️⃣ fetch analysis → gives us jobId (if scored)
        api.get(`/resume/${resumeId}/analysis`)
            .then(res => {
                setResume(res.data);
                if (res.data.jobId) {
                    api.get(`/job/${res.data.jobId}`)
                        .then(j => setJob(j.data))
                        .catch(() => setError("Failed to load job description"));
                }
            })
            .catch(() => setError("Failed to load resume analysis"));
    }, [resumeId]);


    if (error)      return <p className="p-6 text-red-600">{error}</p>;
    if (!resume)    return <p className="p-6">Loading…</p>;

    // helper to pick bar color
    const barColor = (v) =>
        v >= 75 ? "bg-green-500" :
            v >= 50 ? "bg-yellow-500" :
                "bg-red-500";

    // text color to match dashboard badge style
    const textColor = (v) =>
        v >= 75 ? "text-green-600" :
            v >= 50 ? "text-yellow-600" :
                "text-red-600";


    const breakdown = [
        { label: "Skills Match",     value: resume.skillsScore     },
        { label: "Experience Match", value: resume.experienceScore },
        { label: "Education Match",  value: resume.educationScore  },
    ];

    return (
        <div className="p-8 max-w-3xl mx-auto space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">Match Analysis</h1>
                <div className="space-x-2">
                    {/* back */}
                    <Button size="sm" onClick={() => navigate(-1)}>
                        Back
                    </Button>

                    {/* view candidate */}
                    <Button size="sm" onClick={() => navigate(`/candidates/${resumeId}`)}>
                        View Candidate
                    </Button>

                    {/* view job description */}
                    <Button
                        size="sm"
                        onClick={() => job && navigate(`/jobs/${job.id}`)}
                        disabled={!job}
                    >
                        View Job Description
                    </Button>
                </div>
            </div>

            {/* overall score */}
            <Card>
                <CardHeader><CardTitle>Overall Score</CardTitle></CardHeader>
                <CardContent className="flex justify-center py-8">
                    <span className={`text-6xl font-extrabold ${textColor(resume.matchScore)}`}
                    >
                        {resume.matchScore.toFixed(0)}%
                    </span>
                </CardContent>
            </Card>

            {/* breakdown bars */}
            <Card>
                <CardHeader><CardTitle>Breakdown</CardTitle></CardHeader>
                <CardContent className="space-y-4">
                    {breakdown.map(({ label, value }) => (
                        <div key={label}>
                            <div className="flex justify-between mb-1">
                                <span>{label}</span>
                                <span>{value.toFixed(0)}%</span>
                            </div>
                            <div className="w-full bg-gray-200 rounded-full h-2">
                                <div className={`${barColor(value)} h-2 rounded-full`} style={{ width: `${value}%` }} />
                            </div>
                        </div>
                    ))}
                </CardContent>
            </Card>
        </div>
    );
}
