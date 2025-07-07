import axios from "axios";

// Create an Axios instance with a base URL pointing to your Spring Boot backend
const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080', // your Spring Boot URL
});

// Add a request interceptor to automatically add the JWT Authorization header
api.interceptors.request.use(
    (config) => {
        // Retrieve the token from local storage (or wherever you store it)
        const token = localStorage.getItem("jwt");
        // If the token exists, add it to the request headers
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        // Return the modified configuration object
        return config;
    },
    (error) => {
        // Handle any errors that occur during request configuration
        return Promise.reject(error);
    }
);

// Export the configured Axios instance for use throughout your frontend application
export default api;