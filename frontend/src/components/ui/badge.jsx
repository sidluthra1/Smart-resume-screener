import React from 'react'
export function Badge({ children, className }) {
    return <span className={`text-xs font-medium px-2 py-1 rounded ${className}`}>{children}</span>
}
