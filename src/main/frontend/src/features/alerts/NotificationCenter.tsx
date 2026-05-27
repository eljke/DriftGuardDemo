import { Bell, CheckCheck, TriangleAlert, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useI18n } from "../../i18n";
import { eventExplanation } from "../../lib/drift";
import { formatMoscow } from "../../lib/format";
import type { DriftEvent } from "../../types";

export function NotificationCenter({ events }: { events: DriftEvent[] }) {
  const { locale, t } = useI18n();
  const [open, setOpen] = useState(false);
  const [unreadIds, setUnreadIds] = useState<Set<string>>(() => new Set());
  const seenIds = useRef<Set<string>>(new Set());

  const notifications = useMemo(
    () => [...events]
      .filter((event) => event.phase !== "RECOVERED")
      .sort((left, right) => right.detectedAt.localeCompare(left.detectedAt))
      .slice(0, 30),
    [events]
  );

  useEffect(() => {
    const activeIds = new Set(notifications.map((event) => event.id));
    setUnreadIds((current) => {
      const next = new Set([...current].filter((id) => activeIds.has(id)));
      for (const event of notifications) {
        if (!seenIds.current.has(event.id)) {
          seenIds.current.add(event.id);
          next.add(event.id);
        }
      }
      return next;
    });
  }, [notifications]);

  const unread = notifications.filter((event) => unreadIds.has(event.id));
  const critical = notifications.filter((event) => event.severity === "CRITICAL");

  return (
    <div className="notification-center">
      <button
        type="button"
        className={`notification-trigger ${unread.length > 0 ? "has-unread" : ""}`}
        aria-label={t("notifications.open")}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <Bell size={18} />
        {unread.length > 0 && <span className="notification-badge">{unread.length}</span>}
      </button>
      {open && (
        <section className="notification-popover" aria-label={t("notifications.title")}>
          <div className="notification-header">
            <div>
              <strong>{t("notifications.title")}</strong>
              <span>{t("notifications.summary", { total: notifications.length, unread: unread.length, critical: critical.length })}</span>
            </div>
            <div className="notification-actions">
              <button type="button" onClick={() => setUnreadIds(new Set())} disabled={unread.length === 0}>
                <CheckCheck size={15} />
                {t("notifications.markRead")}
              </button>
              <button type="button" aria-label={t("notifications.close")} onClick={() => setOpen(false)}>
                <X size={15} />
              </button>
            </div>
          </div>
          <div className="notification-list">
            {notifications.length === 0 ? (
              <p className="notification-empty">{t("notifications.empty")}</p>
            ) : notifications.map((event) => (
              <article key={event.id} className={`notification-item ${unreadIds.has(event.id) ? "unread" : ""}`}>
                <div className={`notification-icon severity-${event.severity.toLowerCase()}`}>
                  <TriangleAlert size={16} />
                </div>
                <div>
                  <div className="notification-item-title">
                    <strong>{event.key.service} · {event.key.metric}</strong>
                    <span>{formatMoscow(event.detectedAt)}</span>
                  </div>
                  <p>{event.key.operation ?? t("common.any")} · {event.detector}</p>
                  <small>{eventExplanation(event, locale)}</small>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
