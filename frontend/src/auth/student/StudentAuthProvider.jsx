import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { studentAuthService } from './studentAuthService';
import { useToast } from '@/hooks/useToast';
import api from '@/services/api';

export const StudentAuthContext = createContext(null);

export function StudentAuthProvider({ children }) {
  const [studentUser, setStudentUser] = useState(null);
  const [studentToken, setStudentToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const { showToast } = useToast();

  // Load student profile on mount using HttpOnly Cookie
  useEffect(() => {
    const checkStudentAuth = async () => {
      const cachedUser = localStorage.getItem('xebia-student-user');
      if (cachedUser) {
        try {
          setStudentUser(JSON.parse(cachedUser));
          setStudentToken('cookie-session');
        } catch (e) {
          localStorage.removeItem('xebia-student-user');
        }
      }
      
      try {
        const response = await api.get('/auth/me');
        const userDetails = response.data.data;
        
        setStudentUser(userDetails);
        setStudentToken('cookie-session');
        localStorage.setItem('xebia-student-user', JSON.stringify(userDetails));
      } catch (err) {
        setStudentUser(null);
        setStudentToken(null);
        localStorage.removeItem('xebia-student-user');
      } finally {
        setLoading(false);
      }
    };
    
    checkStudentAuth();
  }, []);

  const login = useCallback(async (email, password) => {
    setLoading(true);
    try {
      const user = await studentAuthService.login(email, password);
      
      localStorage.setItem('xebia-student-user', JSON.stringify(user));
      
      setStudentToken('cookie-session');
      setStudentUser(user);
      
      showToast('Successfully logged in!', 'success');
      return user;
    } catch (error) {
      showToast(error.message || 'Invalid Email or Password.', 'error');
      throw error;
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  const register = useCallback(async (fullName, email, password) => {
    setLoading(true);
    try {
      const result = await studentAuthService.register(fullName, email, password);
      showToast('Account created successfully. Please sign in.', 'success');
      return result;
    } catch (error) {
      showToast(error.message || 'Registration failed.', 'error');
      throw error;
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch (err) {
      // Ignore network errors on logout to allow offline logout
    }
    localStorage.removeItem('xebia-student-user');
    
    setStudentToken(null);
    setStudentUser(null);
    
    showToast('Logged out successfully', 'info');
    window.location.href = '/student/login';
  }, [showToast]);

  const value = useMemo(() => ({
    user: studentUser,
    token: studentToken,
    loading,
    login,
    register,
    logout,
    isAuthenticated: !!studentToken,
  }), [studentUser, studentToken, loading, login, register, logout]);

  return (
    <StudentAuthContext.Provider value={value}>
      {children}
    </StudentAuthContext.Provider>
  );
}
