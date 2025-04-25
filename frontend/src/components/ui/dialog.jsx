// src/components/ui/dialog.jsx
import React from 'react'

export function Dialog({ open, onOpenChange, children }) {
    if (!open) return null
    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
            onClick={() => onOpenChange(false)}
        >
            {children}
        </div>
    )
}

export function DialogContent({ children, className = '' }) {
    return (
        <div
            className={`bg-white rounded shadow-lg max-w-md w-full p-6 ${className}`}
            onClick={e => e.stopPropagation()}
        >
            {children}
        </div>
    )
}

export function DialogHeader({ children, className = '' }) {
    return <div className={`border-b pb-2 mb-4 ${className}`}>{children}</div>
}

export function DialogTitle({ children, className = '' }) {
    return <h2 className={`text-xl font-semibold ${className}`}>{children}</h2>
}
