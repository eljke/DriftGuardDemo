import { AlertTriangle, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useI18n } from "../../i18n";
import { eventExplanation } from "../../lib/drift";
import type { DriftEvent } from "../../types";

export function CriticalAlertToast({ events }: { events: DriftEvent[] }) {
  const { locale, t } = useI18n();
  const latestCritical = useMemo(
    () => [...events]
      .filter((event) => event.severity === "CRITICAL" && event.phase !== "RECOVERED")
      .sort((left, right) => right.detectedAt.localeCompare(left.detectedAt))[0],
    [events]
  );
  const [dismissedId, setDismissedId] = useState<string | undefined>();

  useEffect(() => {
    if (latestCritical && latestCritical.id !== dismissedId) {
      const timeout = window.setTimeout(() => setDismissedId(latestCritical.id), 12_000);
      return () => window.clearTimeout(timeout);
    }
    return undefined;
  }, [dismissedId, latestCritical]);

  if (!latestCritical || latestCritical.id === dismissedId) {
    return null;
  }

  return (
    <aside className="critical-toast" role="alert" aria-live="assertive">
      <AlertTriangle size={20} />
      <div>
        <strong>{t("alerts.criticalTitle")}</strong>
        <p>{latestCritical.key.service} · {latestCritical.key.metric} · {latestCritical.key.operation ?? t("common.any")}</p>
        <small>{eventExplanation(latestCritical, locale)}</small>
      </div>
      <button type="button" aria-label={t("alerts.dismiss")} onClick={() => setDismissedId(latestCritical.id)}>
        <X size={16} />
      </button>
    </aside>
  );
}
