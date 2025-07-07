import React, { useState, useEffect, useMemo } from "react";
import { parseISO, format } from "date-fns";
import { Plus } from "lucide-react";
import { Link } from "react-router-dom";

import api from "../api/axios";              // ← use your axios instance
import AddCandidateModal from "../components/AddCandidateModal";
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
} from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Badge } from "../components/ui/badge";

const STATUS_TABS = ["All", "New", "Reviewed", "Contacted", "Hired", "Rejected"];

export default function CandidatesPage() {
    const [candidates, setCandidates] = useState([]);
    const [search, setSearch] = useState("");
    const [activeTab, setActiveTab] = useState("All");
    const [openAdd, setOpenAdd] = useState(false);

    useEffect(() => {
        api
            .get("/resume/all")
            .then((res) => setCandidates(res.data))
            .catch(console.error);
    }, []);

    const filtered = useMemo(() => {
        return candidates
            .filter((c) => {
                if (activeTab !== "All") {
                    const status = c.status || "New";
                    if (status !== activeTab) return false;
                }
                return (
                    c.candidateName.toLowerCase().includes(search.toLowerCase()) ||
                    c.email?.toLowerCase().includes(search.toLowerCase())
                );
            })
            .sort((a, b) => a.candidateName.localeCompare(b.candidateName));
    }, [candidates, search, activeTab]);

    const refetch = () => {
        api
            .get("/resume/all")
            .then((res) => setCandidates(res.data))
            .catch(console.error);
    };

    return (
        <div className="space-y-6">
            {/* 🔍 Search + ➕ */}
            <div className="flex items-center space-x-4">
                <Input
                    placeholder="Search candidates…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="flex-1"
                />
                <button
                    onClick={() => setOpenAdd(true)}
                    className="inline-flex items-center justify-center w-8 h-8 bg-blue-600 hover:bg-blue-700 rounded-full text-white"
                >
                    <Plus className="w-4 h-4" />
                </button>
                <AddCandidateModal open={openAdd} onOpenChange={setOpenAdd} onSuccess={refetch} />
            </div>

            {/* 📑 Tabs */}
            <div className="flex space-x-2 overflow-x-auto">
                {STATUS_TABS.map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`px-3 py-1 rounded-full text-sm ${
                            activeTab === tab
                                ? "bg-blue-600 text-white"
                                : "bg-gray-200 text-gray-700"
                        }`}
                    >
                        {tab} (
                        {filtered.filter((c) =>
                            tab === "All" ? true : (c.status || "New") === tab
                        ).length}
                        )
                    </button>
                ))}
            </div>

            {/* 📇 Cards */}
            <div className="space-y-4">
                {filtered.length === 0 && (
                    <p className="text-center text-gray-500">No candidates found.</p>
                )}
                {filtered.map((c) => {
                    const first4 = (c.skills || []).slice(0, 4);
                    const more = (c.skills || []).length - first4.length;

                    return (
                        <Link to={`/candidates/${c.id}`} key={c.id}>
                            <Card className="hover:shadow-md transition-shadow cursor-pointer">
                                <CardHeader className="flex justify-between items-center">
                                    <div className="flex items-baseline space-x-2">
                                        <CardTitle>{c.candidateName}</CardTitle>
                                        <Badge variant={c.status?.toLowerCase() || "default"}>
                                            {c.status || "New"}
                                        </Badge>
                                    </div>
                                    <div className="text-sm text-gray-600">
                                        {format(parseISO(c.uploadDate), "MMM d, yyyy")}
                                    </div>
                                </CardHeader>

                                <CardContent className="space-y-2">
                                    {c.email && (
                                        <div className="text-sm text-gray-600">{c.email}</div>
                                    )}
                                    {c.phone && (
                                        <div className="text-sm text-gray-600">{c.phone}</div>
                                    )}
                                    <div className="text-sm text-gray-600">{c.fileName}</div>

                                    {/* skills chips */}
                                    <div className="flex flex-wrap gap-2">
                                        {first4.map((s) => (
                                            <Badge key={s.id} variant="secondary">
                                                {s.name}
                                            </Badge>
                                        ))}
                                        {more > 0 && (
                                            <Badge variant="secondary">+{more} more</Badge>
                                        )}
                                    </div>
                                </CardContent>
                            </Card>
                        </Link>
                    );
                })}
            </div>
        </div>
    );
}
