import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  BookOpen, LayoutDashboard, PlayCircle, ClipboardList, FileCheck2, BellRing,
  User, Settings
} from 'lucide-react';
import StudentLogo from '@/components/ui/StudentLogo';
import { cn } from '@/utils';

const STUDENT_NAV_ITEMS = [
  { href: '/student/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/student/courses', label: 'My Courses', icon: BookOpen },
  { href: '/student/learning-content', label: 'Learning Content', icon: PlayCircle },
  { href: '/student/assignments', label: 'Assignments', icon: ClipboardList },
  { href: '/student/assessments', label: 'Assessments', icon: FileCheck2 },
  { href: '/student/notifications', label: 'Notifications', icon: BellRing },
  { href: '/student/profile', label: 'Profile', icon: User },
  { href: '/student/settings', label: 'Settings', icon: Settings },
];

export default function StudentSidebar() {
  const { pathname } = useLocation();

  return (
    <aside
      className="fixed left-0 top-0 z-40 flex h-screen flex-col bg-[#4a1e47] dark:bg-[#111827] transition-colors duration-200"
      style={{ width: 270 }}
    >
      {/* Logo */}
      <div
        className="flex items-center gap-4 px-6 py-6"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.10)' }}
      >
        <StudentLogo size="lg" />
      </div>

      {/* Nav label */}
      <p
        className="px-6 pb-2 pt-5 text-[10px] font-bold uppercase tracking-widest"
        style={{ color: 'rgba(255,255,255,0.35)' }}
      >
        Student Menu
      </p>

      {/* Navigation */}
      <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-4 pb-4 scrollbar-thin">
        {STUDENT_NAV_ITEMS.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link
              key={href}
              to={href}
              className={cn(
                'flex items-center gap-4 rounded-xl px-5 py-3 text-sm font-semibold transition-all cursor-pointer mx-1 my-0.5',
                active
                  ? 'bg-white/10 text-white shadow-sm font-bold'
                  : 'text-white/80 hover:bg-white/10 hover:text-white'
              )}
              style={active ? { backgroundColor: 'rgba(1, 172, 159, 0.15)', borderLeft: '3px solid #01ac9f' } : {}}
            >
              <Icon className={cn("h-5 w-5 shrink-0", active ? "text-[#01ac9f]" : "text-white/80")} strokeWidth={active ? 2.5 : 2} />
              <span>{label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
