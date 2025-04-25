import React, { useState, useRef } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog"
import { Button } from "./ui/button"
import api from "../api/axios"

export default function AddJobModal({ open, onOpenChange, onSuccess }) {
    const [mode, setMode] = useState("manual")        // "manual" | "upload"
    const [busy, setBusy] = useState(false)
    const titleRef    = useRef()
    const descRef     = useRef()
    const fileRef     = useRef()

    const handleManual = async () => {
        const title = titleRef.current.value.trim()
        const text  = descRef.current.value.trim()
        if (!title || !text) return

        setBusy(true)
        try {
            // send JSON to new /job/createManual endpoint
            await api.post("/job/createManual", { title, descriptionText: text })
            onSuccess()
            onOpenChange(false)
        } catch {
            alert("Create failed")
        } finally {
            setBusy(false)
        }
    }

    const handleUpload = async () => {
        const file  = fileRef.current.files[0]
        const title = titleRef.current.value.trim()
        if (!title || !file) return

        const form = new FormData()
        form.append("file", file)
        form.append("title", title)

        setBusy(true)
        try {
            await api.post("/job/uploadFile", form, {
                headers: { "Content-Type": "multipart/form-data" }
            })
            onSuccess()
            onOpenChange(false)
        } catch {
            alert("Upload failed")
        } finally {
            setBusy(false)
        }
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="space-y-4">
                <DialogHeader>
                    <DialogTitle>Add Job</DialogTitle>
                </DialogHeader>

                {/* Toggle */}
                <div className="flex space-x-2">
                    <button
                        onClick={() => setMode("manual")}
                        className={`px-3 py-1 rounded ${mode==="manual" ? "bg-blue-600 text-white" : "bg-gray-200"}`}
                    >
                        Manual Entry
                    </button>
                    <button
                        onClick={() => setMode("upload")}
                        className={`px-3 py-1 rounded ${mode==="upload" ? "bg-blue-600 text-white" : "bg-gray-200"}`}
                    >
                        Upload File
                    </button>
                </div>

                {/* Title (common) */}
                <div>
                    <label className="block text-sm font-medium">Title</label>
                    <input
                        ref={titleRef}
                        type="text"
                        className="mt-1 block w-full rounded border px-2 py-1"
                        placeholder="Job title…"
                    />
                </div>

                {mode === "manual" ? (
                    // manual textarea
                    <div>
                        <label className="block text-sm font-medium">Description</label>
                        <textarea
                            ref={descRef}
                            rows={6}
                            className="mt-1 block w-full rounded border px-2 py-1"
                            placeholder="Paste the full job description…"
                        />
                    </div>
                ) : (
                    // file upload
                    <div>
                        <label className="block text-sm font-medium">File (TXT/PDF/DOCX)</label>
                        <input
                            ref={fileRef}
                            type="file"
                            accept=".txt,.pdf,.doc,.docx"
                            className="mt-1 block w-full"
                        />
                    </div>
                )}

                {/* Action Button */}
                <div className="flex justify-end">
                    <Button
                        onClick={mode === "manual" ? handleManual : handleUpload}
                        disabled={busy}
                    >
                        {busy
                            ? mode === "manual"
                                ? "Saving…"
                                : "Uploading…"
                            : mode === "manual"
                                ? "Create"
                                : "Upload"
                        }
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    )
}
