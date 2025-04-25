// src/pages/JobsPage.jsx
import React, { useState, useEffect, useMemo } from 'react'
import { Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import clsx from 'clsx'

import api from '../api/axios'
import AddJobModal from '../components/AddJobModal'
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { Input } from '../components/ui/input'
import { parseISO, format } from 'date-fns'

export default function JobsPage() {
    const [openAdd, setOpenAdd] = useState(false)
    const [jobs, setJobs] = useState([])
    const [search, setSearch] = useState('')
    const [statusFilter, setStatusFilter] = useState('all')

    // fetch all jobs with JWT auth
    const fetchJobs = async () => {
        try {
            const res = await api.get('/job/all')
            setJobs(res.data)
        } catch (err) {
            console.error('Fetch /job/all failed:', err)
        }
    }

    useEffect(() => {
        fetchJobs()
    }, [])

    function getStatus(job) {
        const posted = parseISO(job.postedAt || job.uploadDate || new Date().toISOString())
        const days = (Date.now() - posted.getTime()) / 86_400_000
        return days < 30 ? 'active' : 'closed'
    }

    const visibleJobs = useMemo(() => {
        return jobs
            .filter(
                (j) =>
                    j.title.toLowerCase().includes(search.toLowerCase()) &&
                    (statusFilter === 'all' || getStatus(j) === statusFilter)
            )
            .sort((a, b) => a.title.localeCompare(b.title))
    }, [jobs, search, statusFilter])

    return (
        <div className="p-6 flex flex-col gap-6">
            {/* Search + Add */}
            <div className="flex items-center gap-4">
                <Input
                    placeholder="Search jobs…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="max-w-md bg-gray-50"
                />
                <button
                    onClick={() => setOpenAdd(true)}
                    className="inline-flex items-center justify-center w-8 h-8 bg-blue-600 hover:bg-blue-700 rounded-full text-white"
                >
                    <Plus className="w-4 h-4" />
                </button>
            </div>

            <AddJobModal open={openAdd} onOpenChange={setOpenAdd} onSuccess={fetchJobs} />

            {/* Status Filters */}
            <div className="flex flex-wrap gap-2 text-sm font-medium">
                {[
                    { id: 'all', label: `All (${jobs.length})` },
                    { id: 'active', label: `Active (${jobs.filter((j) => getStatus(j) === 'active').length})` },
                    { id: 'closed', label: `Closed (${jobs.filter((j) => getStatus(j) === 'closed').length})` },
                ].map((tab) => (
                    <Badge
                        key={tab.id}
                        variant={statusFilter === tab.id ? 'default' : 'secondary'}
                        onClick={() => setStatusFilter(tab.id)}
                        className="cursor-pointer"
                    >
                        {tab.label}
                    </Badge>
                ))}
            </div>

            {/* Job Cards */}
            <div className="flex flex-col gap-6">
                {visibleJobs.map((job) => (
                    <Link key={job.id} to={`/jobs/${job.id}`} className="block">
                        <Card className="hover:shadow-md transition-shadow">
                            <CardHeader className="pb-2 flex-row justify-between items-start">
                                <div>
                                    <CardTitle className="text-lg font-semibold">{job.title}</CardTitle>
                                    <div className="mt-1 text-sm text-gray-500 flex flex-col gap-0.5">
                                        <span>{job.category}</span>
                                        <span>{job.location}</span>
                                        {job.postedAt && (
                                            <span>Posted {format(parseISO(job.postedAt), 'MMM d, yyyy')}</span>
                                        )}
                                    </div>
                                </div>
                                <Badge
                                    variant="outline"
                                    className={clsx(
                                        'text-xs px-2 py-0.5 rounded-full capitalize',
                                        getStatus(job) === 'active'
                                            ? 'bg-green-100 text-green-700'
                                            : 'bg-gray-100 text-gray-600'
                                    )}
                                >
                                    {getStatus(job)}
                                </Badge>
                            </CardHeader>

                            <CardContent className="flex flex-wrap gap-2">
                                {job.skills.slice(0, 4).map((skill) => (
                                    <Badge key={skill} variant="secondary" className="capitalize">
                                        {skill}
                                    </Badge>
                                ))}
                                {job.skills.length > 4 && (
                                    <Badge variant="ghost" className="cursor-default text-gray-500">
                                        +{job.skills.length - 4} more
                                    </Badge>
                                )}
                            </CardContent>
                        </Card>
                    </Link>
                ))}

                {visibleJobs.length === 0 && (
                    <p className="text-sm text-gray-500 mx-auto mt-16">No jobs found.</p>
                )}
            </div>
        </div>
    )
}
