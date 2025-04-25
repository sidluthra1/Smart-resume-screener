import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog'
import { Button } from './ui/button'
import { useRef, useState } from "react";

export default function AddJobModal({ open, onOpenChange, onSuccess }) {
    const fileRef   = useRef(null);
    const titleRef  = useRef(null);
    const [busy, setBusy] = useState(false);

    async function handleUpload() {
        const file  = fileRef.current.files[0];
        const title = titleRef.current.value;
        if (!file || !title) return;

        const fd = new FormData();
        fd.append("file",  file);
        fd.append("title", title);

        setBusy(true);
        const res = await fetch(`${import.meta.env.VITE_API}/job/upload`, {
            method: "POST",
            headers: { Authorization: `Bearer ${localStorage.token}` },
            body: fd,
        });
        setBusy(false);

        if (res.ok) {
            onSuccess();       // trigger refetch in JobsPage
            onOpenChange(false);
        } else {
            alert("Upload failed");
        }
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Upload new Job Description</DialogTitle>
                </DialogHeader>

                <input
                    ref={titleRef}
                    type="text"
                    placeholder="Job title"
                    className="w-full border rounded p-2"
                />

                <input ref={fileRef} type="file" className="w-full" />

                <Button disabled={busy} onClick={handleUpload} className="w-full mt-2">
                    {busy ? "Uploading…" : "Upload"}
                </Button>
            </DialogContent>
        </Dialog>
    );
}
