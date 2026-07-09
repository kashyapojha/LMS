import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading, user } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-brand-background">
        <div className="flex flex-col items-center gap-3">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-brand-primary border-t-transparent" />
          <p className="text-sm font-medium text-brand-text-secondary">Authenticating...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to the login page and save the current location they tried to go to
    return <Navigate to="/admin/login" state={{ from: location }} replace />;
  }

  // Guard: Admin routes are accessible only by ADMIN
  if (user && user.role?.toUpperCase() !== 'ADMIN') {
    return <Navigate to="/student/dashboard" replace />;
  }

  return children;
}
