import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Response Interceptor: Handle errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const isStudentPath = window.location.pathname.startsWith('/student');
    const isLoginPage = window.location.pathname.endsWith('/login') || window.location.pathname.endsWith('/register');
    const isAuthEndpoint = error.config?.url?.includes('/auth/');
    
    if (error.response?.status === 401 && !isLoginPage && !isAuthEndpoint) {
      if (isStudentPath) {
        localStorage.removeItem('xebia-student-user');
        window.location.href = '/student/login';
      } else {
        localStorage.removeItem('xebia-lms-user');
        window.location.href = '/admin/login';
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
