// src/pages/CandidateDetailPage.jsx
import React, { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { format, parseISO } from "date-fns";
import { Edit2 as EditIcon, Check as CheckIcon, X as XIcon } from "lucide-react";
import {
    Card,
    CardHeader,
    CardContent,
    CardTitle
} from "../components/ui/card";
import {
    MailIcon,
    PhoneIcon,
    CalendarIcon,
    File,
    ArrowLeftIcon,
    Trash2Icon
} from "lucide-react";

export default function CandidateDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [cand, setCand] = useState(null);
    const [error, setError] = useState("");
    const [editing, setEditing]     = useState(false);
    const [newStatus, setNewStatus] = useState("");

    useEffect(() => {
        api.get(`/resume/${id}`)
            .then((res) => {
                setCand(res.data);
                setNewStatus(res.data.status);  // seed edit-box
            })
            .catch(() => setError("Failed to load candidate"));
    }, [id]);

    const saveStatus = async () => {
        try {
            const res = await api.patch(
                `/resume/${id}/status`,
                null,
                { params: { status: newStatus } }
            );
            setCand(res.data);
            setEditing(false);
        } catch (err) {
            console.error(err);
            setError("Could not update status");
        }
    };

    const handleDelete = async () => {
        if (!window.confirm("Delete this candidate?")) return;
        try {
            await api.delete(`/resume/${id}`);
            navigate("/candidates", { replace: true });
        } catch {
            setError("Delete failed");
        }
    };

    const handleDownload = async (e) => {
        e.preventDefault();
        try {
            const res = await api.get(`/resume/download/${id}`, {
                responseType: "blob",
            });
            const blobUrl = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement("a");
            link.href = blobUrl;
            link.setAttribute("download", cand.fileName);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(blobUrl);
        } catch {
            setError("Download failed");
        }
    };

    if (error) return <p className="p-6 text-red-600">{error}</p>;
    if (!cand) return <p className="p-6">Loading…</p>;

    return (
        <div className="space-y-6">
            {/* back + title + delete */}
            <div className="flex items-center justify-between bg-blue-600 text-white px-6 py-4 rounded-t">
                <Link to="/candidates" className="flex items-center space-x-2">
                    <ArrowLeftIcon className="w-5 h-5" />
                    <span>Back</span>
                </Link>
                <h1 className="text-xl font-semibold">{cand.candidateName}</h1>
                <Button
                    variant="destructive"
                    size="icon"
                    onClick={handleDelete}
                    title="Delete Candidate"
                >
                    <Trash2Icon className="w-5 h-5" />
                </Button>
            </div>

            {/* contact info + download */}
            <Card>
                <CardContent className="space-y-2">
                    {cand.email && (
                        <div className="flex items-center space-x-2">
                            <MailIcon className="w-5 h-5 text-gray-600" />
                            <span>{cand.email}</span>
                        </div>
                    )}
                    {cand.phone && (
                        <div className="flex items-center space-x-2">
                            <PhoneIcon className="w-5 h-5 text-gray-600" />
                            <span>{cand.phone}</span>
                        </div>
                    )}
                    <div className="flex items-center space-x-2">
                        <CalendarIcon className="w-5 h-5 text-gray-600" />
                        <span>
              Uploaded {format(parseISO(cand.uploadDate), "MMMM d, yyyy")}
            </span>
                    </div>
                    <div className="flex items-center space-x-2">
                        <File className="h-5 w-5 text-gray-500" />
                        <span>{cand.fileName}</span>
                    </div>
                    <a
                        onClick={handleDownload}
                        className="inline-flex items-center space-x-2 border border-gray-300 rounded px-3 py-1 hover:bg-gray-50 cursor-pointer"
                    >
                        <File className="h-5 w-5 text-gray-500" />
                        <span>Download Resume</span>
                    </a>
                </CardContent>
            </Card>

            {/* status badges */}
            <Card>
                <CardHeader className="flex justify-between items-center">
                    <CardTitle>Candidate Status</CardTitle>
                    {!editing && (
                        <Button
                            variant="outline"
                            size="icon"
                            onClick={() => setEditing(true)}
                            title="Edit status"
                        >
                            <EditIcon className="w-4 h-4" />
                        </Button>
                    )}
                </CardHeader>
                <CardContent className="flex items-center space-x-2">
                    {editing ? (
                        <>
                            <select
                                value={newStatus}
                                onChange={(e) => setNewStatus(e.target.value)}
                                className="border px-2 py-1 rounded"
                            >
                                {["New", "Reviewed", "Contacted", "Hired", "Rejected"].map((s) => (
                                    <option key={s} value={s}>{s}</option>
                                ))}
                            </select>
                            <Button size="icon" onClick={saveStatus} title="Save">
                                <CheckIcon className="w-4 h-4 text-green-600" />
                            </Button>
                            <Button
                                size="icon"
                                variant="ghost"
                                onClick={() => {
                                    setEditing(false);
                                    setNewStatus(cand.status);
                                }}
                                title="Cancel"
                            >
                                <XIcon className="w-4 h-4 text-red-600" />
                            </Button>
                        </>
                    ) : (
                        ["New", "Reviewed", "Contacted", "Hired", "Rejected"].map((s) => {
                            const colorMap = {
                                New:       "bg-blue-100 text-blue-700",
                                Reviewed:  "bg-purple-100 text-purple-700",
                                Contacted: "bg-yellow-100 text-yellow-700",
                                Hired:     "bg-green-100 text-green-700",
                                Rejected:  "bg-red-100 text-red-700",
                            };
                            const classes = cand.status === s
                                ? colorMap[s]
                                : "bg-gray-100 text-gray-500";

                            return (
                                <Badge
                                    key={s}
                                    className={`rounded-full px-3 py-0.5 text-sm ${classes}`}
                                >
                                    {s}
                                </Badge>
                            );
                        })
                    )}
                </CardContent>
            </Card>

            {/* summary */}
            {cand.summary && (
                <Card>
                    <CardHeader>
                        <CardTitle>Summary</CardTitle>
                    </CardHeader>
                    <CardContent>{cand.summary}</CardContent>
                </Card>
            )}

            {/* skills */}
            {cand.skills?.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>Skills</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-wrap gap-2">
                        {cand.skills.map((s) => (
                            <Badge key={s.name} variant="secondary">
                                {s.name}
                            </Badge>
                        ))}
                    </CardContent>
                </Card>
            )}

            {/* experience with real bullets */}
            {cand.experiences?.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>Experience</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <ul className="list-disc list-inside space-y-1">
                            {cand.experiences.flatMap((exp) =>
                                exp.description
                                    .split(";")
                                    .map((line) => line.trim())
                                    .filter(Boolean)
                                    .map((line, idx) => <li key={`${exp.id}-${idx}`}>{line}</li>)
                            )}
                        </ul>
                    </CardContent>
                </Card>
            )}

            {/* education */}
            {cand.education && (
                <Card>
                    <CardHeader>
                        <CardTitle>Education</CardTitle>
                    </CardHeader>
                    <CardContent>{cand.education}</CardContent>
                </Card>
            )}
        </div>
    );
}
