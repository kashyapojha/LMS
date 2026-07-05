'use client';

import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import { useCatalog } from '@/hooks/useCatalog';

export default function AppLayout({ children }) {
  const { branding, hydrated } = useCatalog();
  const { pathname } = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    if (hydrated) {
      document.documentElement.style.setProperty('--brand-primary', branding.primaryColor || '#6C1D5F');
      document.documentElement.style.setProperty('--brand-secondary', branding.secondaryColor || '#84117C');
    }
  }, [branding, hydrated]);

  useEffect(() => {
    if (mobileOpen) {
      setMobileOpen(false);
    }
  }, [pathname]);

  return (
    <div className="min-h-screen bg-brand-surface text-brand-text-primary transition-colors duration-200">
      <Sidebar mobileOpen={mobileOpen} onMobileClose={() => setMobileOpen(false)} />

      <button
        type="button"
        aria-label="Open navigation menu"
        onClick={() => setMobileOpen(true)}
        className="fixed left-4 top-4 z-50 inline-flex h-11 w-11 items-center justify-center rounded-full border border-brand-border bg-white text-brand-text-primary shadow-lg transition hover:bg-brand-surface/90 lg:hidden"
      >
        <span className="sr-only">Open menu</span>
        <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
          <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </button>

      <div className="lg:pl-[220px] pl-0">
        <main className="min-h-screen pt-5 lg:pt-0">{children}</main>
      </div>
    </div>
  );
}
