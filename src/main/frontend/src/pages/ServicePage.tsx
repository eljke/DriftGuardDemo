import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, Play, RotateCcw, Send, Square, Zap } from "lucide-react";
import type { ReactNode } from "react";
import { api } from "../api/client";
import { AlertDeliveryPanel } from "../features/alerts/AlertDeliveryPanel";
import { CriticalAlertToast } from "../features/alerts/CriticalAlertToast";
import { EventsTable } from "../features/events/EventsTable";
import { IncidentsPanel } from "../features/events/IncidentsPanel";
import { ServiceMetricsPanel } from "../features/service/ServiceMetricsPanel";
import { useI18n } from "../i18n";
import { readableError } from "../lib/format";
import { MetricCard, Notice, Panel } from "../components/ui";
import type { CheckoutMode, CheckoutServiceSnapshot, DemoWebhookDelivery } from "../types";

export function ServicePage({ service, operations, webhookDeliveries }: { service?: CheckoutServiceSnapshot; operations: string[]; webhookDeliveries: DemoWebhookDelivery[] }) {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const refresh = (data: CheckoutServiceSnapshot) => queryClient.setQueryData(["service"], data);
  const execute = useMutation({
    mutationFn: (operation: string) => api.executeServiceOperation({ operation, customerId: "ui-user" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["service"] })
  });
  const start = useMutation({ mutationFn: api.startServiceTraffic, onSuccess: refresh });
  const stop = useMutation({ mutationFn: api.stopServiceTraffic, onSuccess: refresh });
  const reset = useMutation({ mutationFn: api.resetServiceHistory, onSuccess: refresh });
  const mode = useMutation({ mutationFn: api.setServiceMode, onSuccess: refresh });
  const error = execute.error ?? start.error ?? stop.error ?? reset.error ?? mode.error;

  return (
    <section className="stack service-page">
      <CriticalAlertToast events={service?.recentAlerts ?? []} />
      <Panel className="service-hero" title={t("service.title")}>
        {error && <Notice tone="error" text={readableError(error)} />}
        <div className="service-control-row">
          <div>
            <p className="eyebrow">{t("service.eyebrow")}</p>
            <h2>{t("service.heading")}</h2>
            <p className="help-text">{t("service.description")}</p>
          </div>
          <div className="actions">
            <button className="primary-button" disabled={start.isPending || service?.running} onClick={() => start.mutate()} type="button">
              <Play size={16} />
              {t("service.start")}
            </button>
            <button className="secondary-button" disabled={stop.isPending || !service?.running} onClick={() => stop.mutate()} type="button">
              <Square size={16} />
              {t("service.stop")}
            </button>
            <button className="secondary-button" disabled={reset.isPending || (service?.operations ?? 0) === 0} onClick={() => reset.mutate()} type="button">
              <RotateCcw size={16} />
              {t("service.reset")}
            </button>
          </div>
        </div>
        <div className="mode-switch">
          {(["NORMAL", "DEGRADED", "OUTAGE"] as CheckoutMode[]).map((value) => (
            <button className={service?.mode === value ? "active" : ""} disabled={mode.isPending} key={value} onClick={() => mode.mutate(value)} type="button">
              {t(`mode.${value}`)}
            </button>
          ))}
        </div>
      </Panel>

      <section className="page-grid">
        <MetricCard title={t("service.operations")} value={service?.operations ?? 0} helper={service?.running ? t("service.running") : t("service.idle")} />
        <MetricCard title={t("service.latency")} value={`${Math.round(service?.meanLatencyMillis ?? 0)} ms`} helper={t("service.recentWindow")} />
        <MetricCard title={t("service.errorRate")} value={`${Math.round((service?.errorRate ?? 0) * 100)}%`} helper={`${service?.failures ?? 0} failed`} tone={(service?.errorRate ?? 0) > 0.1 ? "danger" : undefined} />
        <MetricCard title={t("service.alerts")} value={service?.recentAlerts.length ?? 0} helper={t("service.alertsHelper")} tone={(service?.recentAlerts.length ?? 0) > 0 ? "danger" : undefined} />
      </section>

      <Panel title={t("service.operationsPanel")}>
        <div className="service-operation-grid">
          {operations.map((operation) => (
            <button className="operation-card" disabled={execute.isPending} key={operation} onClick={() => execute.mutate(operation)} type="button">
              <Send size={17} />
              <strong>{operation}</strong>
              <span>{t("service.operationHelp")}</span>
            </button>
          ))}
        </div>
      </Panel>

      <Panel title={t("service.pipeline")}>
        <div className="service-pipeline">
          <PipelineTile icon={<Send size={18} />} title={t("service.business")} text={t("service.businessText")} active />
          <PipelineTile icon={<Zap size={18} />} title="MetricPoint" text={t("service.metricText")} active={(service?.recentMetrics.length ?? 0) > 0} />
          <PipelineTile icon={<AlertTriangle size={18} />} title="DriftGuard" text={t("service.guardText")} active={(service?.recentAlerts.length ?? 0) > 0} />
          <PipelineTile icon={<CheckCircle2 size={18} />} title={t("service.alerting")} text={t("service.alertingText")} active={(service?.recentAlerts.length ?? 0) > 0} />
        </div>
      </Panel>

      <AlertDeliveryPanel events={service?.recentAlerts ?? []} webhookDeliveries={webhookDeliveries} />

      <Panel title={t("service.metrics")}>
        <ServiceMetricsPanel
          points={service?.recentMetrics ?? []}
          events={service?.recentAlerts ?? []}
          operations={service?.recentOperations ?? []}
          running={Boolean(service?.running)}
        />
      </Panel>

      <Panel title={t("service.recentOperations")}>
        {(service?.recentOperations.length ?? 0) === 0 ? (
          <div className="empty-state compact">{t("service.noOperations")}</div>
        ) : (
          <div className="table-wrap operations-log" role="region" aria-label={t("service.recentOperations")} tabIndex={0}>
            <table>
              <thead>
                <tr>
                  <th>{t("events.time")}</th>
                  <th>{t("service.operation")}</th>
                  <th>{t("service.customer")}</th>
                  <th>{t("service.result")}</th>
                  <th>{t("service.latency")}</th>
                  <th>{t("service.queue")}</th>
                </tr>
              </thead>
              <tbody>
                {(service?.recentOperations ?? []).slice().reverse().map((operation) => (
                  <tr key={operation.id}>
                    <td>{new Date(operation.occurredAt).toLocaleTimeString()}</td>
                    <td>{operation.operation}</td>
                    <td>{operation.customerId}</td>
                    <td><span className={operation.success ? "phase recovered" : "phase started"}>{operation.success ? "OK" : "FAILED"}</span></td>
                    <td>{Math.round(operation.latencyMillis)} ms</td>
                    <td>{Math.round(operation.queueSize)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      <IncidentsPanel events={service?.recentAlerts ?? []} />

      <EventsTable events={service?.recentAlerts ?? []} />
    </section>
  );
}

function PipelineTile({ icon, title, text, active }: { icon: ReactNode; title: string; text: string; active: boolean }) {
  return (
    <article className={active ? "pipeline-step active" : "pipeline-step"}>
      <span>{icon}</span>
      <strong>{title}</strong>
      <small>{text}</small>
    </article>
  );
}
