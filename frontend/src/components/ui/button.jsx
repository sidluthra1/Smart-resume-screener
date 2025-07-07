import React from 'react'
export function Button(props) {
    return <button {...props} className={`px-4 py-2 rounded ${props.size === 'icon' ? 'p-2' : ''}`}>{props.children}</button>
}
