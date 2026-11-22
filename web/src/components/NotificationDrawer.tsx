import React from 'react';
import { Bell, CheckCheck, X } from 'lucide-react';
import { AppLanguage, StudentNotification } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';

interface NotificationDrawerProps {
  open: boolean;
  lang: AppLanguage;
  notifications: StudentNotification[];
  onMarkAllRead: () => void;
  onClose: () => void;
}

export const NotificationDrawer: React.FC<NotificationDrawerProps> = ({
  open,
  lang,
  notifications,
  onMarkAllRead,
  onClose,
}) => {
  if (!open) return null;
  const tr = TRANSLATIONS[lang];
  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-sm">
      <div className="flex h-full w-full max-w-md flex-col bg-svt-surface pt-safe pb-safe text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-4">
          <div className="flex items-center gap-2 font-display text-lg font-bold">
            <Bell className="h-5 w-5 text-svt-primary" />
            <span>{tr.notificationsTitle}</span>
            {unreadCount > 0 && (
              <span className="rounded-full bg-svt-error px-2 py-0.5 text-xs font-bold text-white">
                {unreadCount}
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={onMarkAllRead}
                className="flex min-h-[40px] items-center gap-1 rounded-xl bg-svt-primaryContainer px-3 text-xs font-bold text-svt-onPrimaryContainer"
              >
                <CheckCheck className="h-4 w-4" />
                <span>{tr.markAllRead}</span>
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 space-y-2.5 overflow-y-auto p-4">
          {notifications.length === 0 ? (
            <div className="rounded-2xl bg-svt-surfaceVariant/40 p-6 text-center text-sm text-svt-onSurfaceVariant">
              {tr.notificationsEmpty}
            </div>
          ) : (
            notifications.map((n) => (
              <div
                key={n.id}
                className={`rounded-2xl border p-3.5 transition ${
                  n.isRead
                    ? 'border-svt-outline/15 bg-svt-bg/50'
                    : 'border-svt-primary/35 bg-svt-primaryContainer/35'
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-display text-sm font-bold">{n.title}</span>
                  <span className="text-[11px] text-svt-onSurfaceVariant">
                    {new Date(n.createdAtMillis).toLocaleDateString()}
                  </span>
                </div>
                <p className="mt-1 text-xs leading-relaxed text-svt-onSurfaceVariant">{n.message}</p>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
