import React, {useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle
} from "../components/ui/dialog";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { useForm } from "react-hook-form";
import { Loader2 } from "lucide-react";
import axios from "axios";

export default function AddCandidateModal({ open, onOpenChange, onSuccess }) {
    const { register, handleSubmit, reset } = useForm();
    const token = localStorage.getItem("jwt");
    const [isLoading, setIsLoading] = useState(false);

    const onSubmit = async (data) => {
        setIsLoading(true);
        const form = new FormData();
        form.append("candidateName", data.candidateName);
        form.append("file", data.file[0]);

        try {
            await axios.post("/resume/upload", form, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "multipart/form-data",
                },
            });
            reset();
            onOpenChange(false);
            onSuccess();
        } catch (err) {
            console.error("Upload failed", err);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Add Candidate</DialogTitle>
                </DialogHeader>

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium mb-1">Name</label>
                        <Input
                            {...register("candidateName", { required: true })}
                            placeholder="Candidate name"
                            disabled={isLoading}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium mb-1">
                            Resume (PDF/DOCX)
                        </label>
                        <Input
                            type="file"
                            accept=".pdf,.docx"
                            {...register("file", { required: true })}
                            disabled={isLoading}
                        />
                    </div>

                    <div className="flex justify-end">
                        <Button type="submit" disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2 className="animate-spin w-4 h-4 mr-2" />
                                    Uploading...
                                </>
                            ) : (
                                "Upload"
                            )}
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
