import { Bell, Database, FileText, MonitorDot } from "lucide-react";
import type { ReactNode } from "react";
import { Panel } from "../../components/ui";
import { useI18n } from "../../i18n";
import type { DriftEvent } from "../../types";

export function AlertDeliveryPanel({ events }: { events: DriftEvent[] }) {
  const { t } = useI18n();
  const critical = events.filter((event) => event.severity === "CRITICAL").length;
  const warnings = events.filter((event) => event.severity === "WARNING").length;
  const latest = events.slice().sort((left, right) => right.detectedAt.localeCompare(left.detectedAt))[0];

  return (
    <Panel title={t("alerts.deliveryTitle")}>
      <div className="alert-delivery-grid">
        <DeliveryStep icon={<Bell size={18} />} title="DriftAlertListener" text={t("alerts.listenerStep")} active={events.length > 0} />
        <DeliveryStep icon={<FileText size={18} />} title="Slf4jDriftAlertSink" text={t("alerts.slf4jStep")} active={events.length > 0} />
        <DeliveryStep icon={<Database size={18} />} title="RepositoryDriftAlertSink" text={t("alerts.repositoryStep")} active={events.length > 0} />
        <DeliveryStep icon={<MonitorDot size={18} />} title={t("alerts.uiStepTitle")} text={t("alerts.uiStep")} active={critical > 0} />
      </div>
      <div className="alert-delivery-summary">
        <span>{t("alerts.total", { count: events.length })}</span>
        <span>{t("alerts.warningCount", { count: warnings })}</span>
        <span>{t("alerts.criticalCount", { count: critical })}</span>
        <span>{latest ? t("alerts.latest", { metric: latest.key.metric, detector: latest.detector }) : t("alerts.noLatest")}</span>
      </div>
    </Panel>
  );
}

function DeliveryStep({ icon, title, text, active }: { icon: ReactNode; title: string; text: string; active: boolean }) {
  return (
    <article className={active ? "alert-delivery-step active" : "alert-delivery-step"}>
      <span>{icon}</span>
      <strong>{title}</strong>
      <small>{text}</small>
    </article>
  );
}
