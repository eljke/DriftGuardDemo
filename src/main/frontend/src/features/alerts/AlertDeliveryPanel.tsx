import { Bell, Database, FileText, MonitorDot, Webhook } from "lucide-react";
import type { ReactNode } from "react";
import { Panel } from "../../components/ui";
import { useI18n } from "../../i18n";
import { formatMoscow } from "../../lib/format";
import type { DemoWebhookDelivery, DriftEvent } from "../../types";

export function AlertDeliveryPanel({ events, webhookDeliveries }: { events: DriftEvent[]; webhookDeliveries: DemoWebhookDelivery[] }) {
  const { t } = useI18n();
  const critical = events.filter((event) => event.severity === "CRITICAL").length;
  const warnings = events.filter((event) => event.severity === "WARNING").length;
  const latest = events.slice().sort((left, right) => right.detectedAt.localeCompare(left.detectedAt))[0];
  const latestWebhook = webhookDeliveries[0];

  return (
    <Panel title={t("alerts.deliveryTitle")}>
      <div className="alert-delivery-grid">
        <DeliveryStep icon={<Bell size={18} />} title="DriftAlertListener" text={t("alerts.listenerStep")} active={events.length > 0} />
        <DeliveryStep icon={<FileText size={18} />} title="Slf4jDriftAlertSink" text={t("alerts.slf4jStep")} active={events.length > 0} />
        <DeliveryStep icon={<Database size={18} />} title="RepositoryDriftAlertSink" text={t("alerts.repositoryStep")} active={events.length > 0} />
        <DeliveryStep icon={<Webhook size={18} />} title="WebhookDriftAlertSink" text={t("alerts.webhookStep")} active={webhookDeliveries.length > 0} />
        <DeliveryStep icon={<MonitorDot size={18} />} title={t("alerts.uiStepTitle")} text={t("alerts.uiStep")} active={critical > 0} />
      </div>
      <div className="alert-delivery-summary">
        <span>{t("alerts.total", { count: events.length })}</span>
        <span>{t("alerts.warningCount", { count: warnings })}</span>
        <span>{t("alerts.criticalCount", { count: critical })}</span>
        <span>{t("alerts.webhookCount", { count: webhookDeliveries.length })}</span>
        <span>{latest ? t("alerts.latest", { metric: latest.key.metric, detector: latest.detector }) : t("alerts.noLatest")}</span>
      </div>
      <div className="webhook-delivery-box">
        <div>
          <strong>{t("alerts.webhookDeliveries")}</strong>
          <small>{latestWebhook ? t("alerts.webhookLatest", { time: formatMoscow(latestWebhook.acceptedAt), channel: latestWebhook.channel ?? "default" }) : t("alerts.webhookEmpty")}</small>
        </div>
        <div className="webhook-delivery-list">
          {webhookDeliveries.slice(0, 5).map((delivery) => (
            <article key={`${delivery.acceptedAt}-${delivery.payload.id}`} className="webhook-delivery-item">
              <span className={`severity ${delivery.payload.severity.toLowerCase()}`}>{delivery.payload.severity}</span>
              <div>
                <strong>{delivery.payload.service} · {delivery.payload.metric}</strong>
                <small>{delivery.payload.operation ?? t("common.any")} · {formatMoscow(delivery.acceptedAt)}</small>
              </div>
            </article>
          ))}
        </div>
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
