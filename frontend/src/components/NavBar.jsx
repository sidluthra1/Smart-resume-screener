import { NavLink } from "react-router-dom";

export default function NavBar() {
    return (
        <nav className="p-4 bg-gray-100 flex space-x-6">
            <NavLink
                to="/dashboard"
                className={({ isActive }) =>
                    isActive ? "font-semibold underline" : ""
                }
            >
                Dashboard
            </NavLink>
            <NavLink
                to="/jobs"
                className={({ isActive }) =>
                    isActive ? "font-semibold underline" : ""
                }
            >
                Jobs
            </NavLink>
        </nav>
    );
}
