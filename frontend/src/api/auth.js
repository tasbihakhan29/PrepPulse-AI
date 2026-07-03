import axios from 'axios';

const API = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  // headers: {
  //   'Content-Type': 'application/json',
  // },
});

// Add interceptor to append authorization token
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const authApi = {
  signup: async (name, email, password) => {
    const response = await API.post('/api/auth/signup', { name, email, password });
    return response.data;
  },

  login: async (email, password) => {
    const response = await API.post('/api/auth/login', { email, password });
    return response.data;
  },

  googleLogin: async (idToken) => {
    const response = await API.post('/api/auth/google', { idToken });
    return response.data;
  },

  forgotPassword: async (email) => {
    const response = await API.post('/api/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (email, otpCode, password) => {
    const response = await API.post('/api/auth/reset-password', { email, otpCode, password });
    return response.data;
  },

  getMe: async () => {
    const response = await API.get('/api/auth/me');
    return response.data;
  },
};

export default API;
