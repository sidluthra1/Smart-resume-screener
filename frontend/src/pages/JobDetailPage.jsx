// src/pages/JobDetailPage.jsx
import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Edit, Trash2 } from 'lucide-react'
import { parseISO, format } from 'date-fns'
import { Button } from '../components/ui/button'
import { Badge } from '../components/ui/badge'
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card'
import api from '../api/axios'

export default function JobDetailPage() {
    const { id } = useParams()
    const navigate = useNavigate()
    const [job, setJob] = useState(null)

    useEffect(() => {
        api
            .get('/job/all')
            .then(res => {
                const found = res.data.find(j => String(j.id) === id)
                setJob(found)
            })
            .catch(console.error)
    }, [id])

    if (!job) return <div className="p-6">Loading…</div>

    // Determine “active” status (posted within last 30 days)
    const dateStr  = job.postedAt ?? job.uploadDate ?? new Date().toISOString()
    const posted   = parseISO(dateStr)
    const isActive = (Date.now() - posted.getTime()) / 86400000 < 30

    // Helper: normalize string or array into cleaned, capitalized list items
    const toList = val => {
        let arr = []
        if (Array.isArray(val)) {
            arr = val
        } else if (typeof val === 'string') {
            arr = val.split(/\s*,\s*/)
        }
        return arr
            .map(item => item.replace(/^\s*and\s+/i, '').trim())           // strip leading "and"
            .filter(item => item.length)                                   // drop empties
            .map(item => item.charAt(0).toUpperCase() + item.slice(1))     // capitalize
    }

    // Delete handler
    const handleDelete = async () => {
           if (!window.confirm('Delete this job?')) return
           try {
                 await api.delete(`/job/${id}`)
                 navigate('/jobs', { replace: true })
               } catch (err) {
                 console.error(err)
                 // optionally show an error message in your UI
               }
         }

    // Safely render a skill (could be string or object)
    const renderSkill = s => (typeof s === 'string' ? s : s.name)

    return (
        <div className="space-y-6">
            {/* Top bar */}
            <header className="bg-blue-600 text-white p-4 flex items-center justify-between">
                <Button variant="ghost" onClick={() => navigate(-1)}>
                    <ArrowLeft className="w-5 h-5" />
                </Button>
                <h1 className="text-xl font-bold">{job.title}</h1>
                <div className="flex space-x-2">
                    <Button variant="ghost" onClick={handleDelete}><Trash2 className="w-5 h-5" /></Button>
                </div>
            </header>

            <section className="p-4 bg-white rounded shadow space-y-4">
                {/* Meta info */}
                <div className="flex flex-wrap items-center gap-4 text-gray-700">
                    <Badge variant={isActive ? 'secondary' : 'outline'}>
                        {isActive ? 'Active' : 'Closed'}
                    </Badge>
                    <span>Category: {job.category}</span>
                    <span>Location: {job.location}</span>
                    <span>Posted {format(posted, 'MMMM d, yyyy')}</span>
                </div>

                {/* Parsed summary */}
                <Card>
                    <CardHeader>
                        <CardTitle>Job Description</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {job.summary || 'No summary available.'}
                    </CardContent>
                </Card>

                {/* Requirements */}
                <Card>
                    <CardHeader>
                        <CardTitle>Requirements</CardTitle>
                    </CardHeader>
                    <CardContent as="ul" className="list-disc list-inside space-y-1">
                        {toList(job.requirements).map((req, i) => (
                            <li key={i}>{req}</li>
                        ))}
                    </CardContent>
                </Card>

                {/* Responsibilities */}
                <Card>
                    <CardHeader>
                        <CardTitle>Responsibilities</CardTitle>
                    </CardHeader>
                    <CardContent as="ul" className="list-disc list-inside space-y-1">
                        {toList(job.responsibilities).map((resp, i) => (
                            <li key={i}>{resp}</li>
                        ))}
                    </CardContent>
                </Card>

                {/* Skills */}
                <Card>
                    <CardHeader>
                        <CardTitle>Required Skills</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-wrap gap-2">
                        {(job.skills || []).map((s, i) => (
                            <Badge key={i} variant="secondary" className="capitalize">
                                {renderSkill(s)}
                            </Badge>
                        ))}
                    </CardContent>
                </Card>
            </section>
        </div>
    )
}
