import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authService } from '@/services/authService';
import { useToast } from '@/hooks/useToast';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const { showToast } = useToast();

  // Load user profile on mount using HttpOnly Cookie
  useEffect(() => {
    const checkAuth = async () => {
      const cachedUser = localStorage.getItem('xebia-lms-user');
      if (cachedUser) {
        try {
          setUser(JSON.parse(cachedUser));
          setToken('cookie-session');
        } catch (e) {
          localStorage.removeItem('xebia-lms-user');
        }
      }
      
      try {
        const response = await authService.getProfile();
        const userDetails = response.data;
        
        setUser(userDetails);
        setToken('cookie-session');
        localStorage.setItem('xebia-lms-user', JSON.stringify(userDetails));
      } catch (err) {
        setUser(null);
        setToken(null);
        localStorage.removeItem('xebia-lms-user');
      } finally {
        setLoading(false);
      }
    };
    
    checkAuth();
  }, []);

  const login = useCallback(async (email, password) => {
    setLoading(true);
    try {
      const response = await authService.login(email, password);
      const loggedUser = response.data;
      
      localStorage.setItem('xebia-lms-user', JSON.stringify(loggedUser));
      
      setToken('cookie-session');
      setUser(loggedUser);
      
      showToast('Successfully logged in', 'success');
      return loggedUser;
    } catch (error) {
      let errorMsg;
      if (error.response) {
        errorMsg = error.response.data?.message
          || (error.response.status === 401 || error.response.status === 403
            ? 'Invalid email or password.'
            : `Server error (${error.response.status}).`);
      } else if (error.request) {
        errorMsg = 'Could not reach the server. Check that the backend is running and reachable.';
      } else {
        errorMsg = 'Login failed. Please check your credentials.';
      }
      showToast(errorMsg, 'error');
      throw error;
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch (err) {
      // Ignore network errors on logout to allow offline logout
    }
    localStorage.removeItem('xebia-lms-user');
    
    setToken(null);
    setUser(null);
    
    showToast('Logged out successfully', 'info');
    window.location.href = '/admin/login';
  }, [showToast]);

  const value = useMemo(() => ({
    user,
    token,
    loading,
    login,
    logout,
    isAuthenticated: !!token,
  }), [user, token, loading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
