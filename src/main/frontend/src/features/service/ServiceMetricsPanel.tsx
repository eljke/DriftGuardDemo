import { ChevronDown, ChevronRight } from "lucide-react";
import { useMemo, useState } from "react";
import { TimeSeriesChart } from "../../components/TimeSeriesChart";
import { useI18n } from "../../i18n";
import { groupStreams, streamId } from "../../lib/drift";
import { formatNumber, formatPercent } from "../../lib/format";
import type { CheckoutOperationResult, DriftEvent, MetricPoint } from "../../types";

interface ServiceMetricsPanelProps {
  points: MetricPoint[];
  events: DriftEvent[];
  operations: CheckoutOperationResult[];
  running: boolean;
}

export function ServiceMetricsPanel({ points, events, operations, running }: ServiceMetricsPanelProps) {
  const { t } = useI18n();
  const metricGroups = useMemo(() => groupByMetric(points), [points]);
  const operationGroups = useMemo(() => groupByOperation(operations), [operations]);
  const [collapsed, setCollapsed] = useState<Set<string>>(() => new Set());

  if (points.length === 0) {
    return <div className="empty-state">{t("stream.empty")}</div>;
  }

  return (
    <div className="service-observability">
      <section className="metric-sections" aria-label={t("service.byMetric")}>
        {metricGroups.map((group) => {
          const isCollapsed = collapsed.has(group.metric);
          return (
            <article className="metric-section" key={group.metric}>
              <button
                className="section-head collapsible-head"
                type="button"
                aria-expanded={!isCollapsed}
                onClick={() => setCollapsed((current) => toggle(current, group.metric))}
              >
                <span className="section-title">
                  {isCollapsed ? <ChevronRight size={18} /> : <ChevronDown size={18} />}
                  <span>
                    <strong>{metricLabel(group.metric, t)}</strong>
                    <small>{t("service.metricPointCount", { points: group.points })}</small>
                  </span>
                </span>
                <span className="badge">{t("service.streamCount", { count: group.streams.length })}</span>
              </button>
              {!isCollapsed && (
                <div className="metric-streams">
                  {group.streams.map((stream) => {
                    const streamEvents = events.filter((event) => streamId(event.key) === stream.id);
                    const displayPoints = displayMetricPoints(group.metric, stream.points);
                    const latest = displayPoints.at(-1);
                    return (
                      <article className="stream-card compact-stream" key={stream.id}>
                        <div className="stream-head">
                          <div>
                            <strong>{stream.operation || "-"}</strong>
                            <span>{t("service.latestValue")}: {latest ? formatMetricValue(group.metric, latest.value) : "-"}</span>
                          </div>
                          <span className="badge">{t("stream.badge", { points: stream.points.length, events: streamEvents.length })}</span>
                        </div>
                        {streamEvents.length > 0 && <EventSummary events={streamEvents} />}
                        {running && streamEvents.length === 0 && (
                          <div className="inline-hint">{t("stream.waiting")}</div>
                        )}
                        <TimeSeriesChart
                          points={displayPoints}
                          events={streamEvents}
                          height={210}
                          valueLabel={t("chart.value")}
                          valueFormatter={(value) => formatMetricValue(group.metric, value)}
                        />
                      </article>
                    );
                  })}
                </div>
              )}
            </article>
          );
        })}
      </section>

      <section className="operation-breakdown" aria-label={t("service.byOperation")}>
        <div className="section-head">
          <div>
            <strong>{t("service.operationBreakdown")}</strong>
            <span>{t("service.operationBreakdownHelp")}</span>
          </div>
        </div>
        <div className="operation-breakdown-grid">
          {operationGroups.map((operation) => (
            <article className="operation-summary-card" key={operation.name}>
              <strong>{operation.name}</strong>
              <dl>
                <div>
                  <dt>{t("service.operations")}</dt>
                  <dd>{operation.total}</dd>
                </div>
                <div>
                  <dt>{t("service.errorRate")}</dt>
                  <dd>{Math.round(operation.errorRate * 100)}%</dd>
                </div>
                <div>
                  <dt>{t("service.latency")}</dt>
                  <dd>{Math.round(operation.meanLatency)} ms</dd>
                </div>
                <div>
                  <dt>{t("service.queue")}</dt>
                  <dd>{Math.round(operation.meanQueue)}</dd>
                </div>
              </dl>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

function groupByMetric(points: MetricPoint[]) {
  const streams = groupStreams(points);
  const byMetric = new Map<string, { metric: string; points: number; streams: typeof streams }>();
  for (const stream of streams) {
    const current = byMetric.get(stream.metric) ?? { metric: stream.metric, points: 0, streams: [] };
    current.points += stream.points.length;
    current.streams.push(stream);
    byMetric.set(stream.metric, current);
  }
  return [...byMetric.values()].sort((left, right) => metricRank(left.metric) - metricRank(right.metric));
}

function groupByOperation(operations: CheckoutOperationResult[]) {
  const grouped = new Map<string, { name: string; total: number; failures: number; latency: number; queue: number }>();
  for (const operation of operations) {
    const current = grouped.get(operation.operation) ?? {
      name: operation.operation,
      total: 0,
      failures: 0,
      latency: 0,
      queue: 0
    };
    current.total += 1;
    current.failures += operation.success ? 0 : 1;
    current.latency += operation.latencyMillis;
    current.queue += operation.queueSize;
    grouped.set(operation.operation, current);
  }
  return [...grouped.values()]
    .map((operation) => ({
      name: operation.name,
      total: operation.total,
      errorRate: operation.total === 0 ? 0 : operation.failures / operation.total,
      meanLatency: operation.total === 0 ? 0 : operation.latency / operation.total,
      meanQueue: operation.total === 0 ? 0 : operation.queue / operation.total
    }))
    .sort((left, right) => left.name.localeCompare(right.name));
}

function metricRank(metric: string) {
  return ["latency", "error-rate", "throughput", "queue-size"].indexOf(metric);
}

function metricLabel(metric: string, t: (key: string) => string) {
  const key = `metric.${metric}`;
  const translated = t(key);
  if (translated !== key) {
    return translated;
  }
  return metric.split("-").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function toggle(current: Set<string>, metric: string) {
  const next = new Set(current);
  if (next.has(metric)) {
    next.delete(metric);
  } else {
    next.add(metric);
  }
  return next;
}

function displayMetricPoints(metric: string, points: MetricPoint[]) {
  if (metric !== "error-rate") {
    return points;
  }

  const sorted = [...points].sort((left, right) => Date.parse(left.timestamp) - Date.parse(right.timestamp));
  return sorted.map((point, index) => {
    const window = sorted.slice(Math.max(0, index - 19), index + 1);
    const rollingErrorRate = window.reduce((sum, current) => sum + current.value, 0) / window.length;
    return { ...point, value: rollingErrorRate };
  });
}

function formatMetricValue(metric: string, value: number) {
  if (metric === "error-rate") {
    return formatPercent(value);
  }
  return formatNumber(value);
}

function EventSummary({ events }: { events: DriftEvent[] }) {
  const critical = events.filter((event) => event.severity === "CRITICAL").length;
  const warning = events.filter((event) => event.severity === "WARNING").length;
  const recovered = events.filter((event) => event.phase === "RECOVERED").length;
  return (
    <div className="event-summary-strip" aria-label="Drift event summary">
      {critical > 0 && <span className="event-summary critical">CRIT {critical}</span>}
      {warning > 0 && <span className="event-summary warning">WARN {warning}</span>}
      {recovered > 0 && <span className="event-summary recovered">REC {recovered}</span>}
    </div>
  );
}
