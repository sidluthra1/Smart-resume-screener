// src/api/axios.js
import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8080",  // your Spring Boot URL
});

// before each request, add the Authorization header if we have a token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("jwt");
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export default api;
