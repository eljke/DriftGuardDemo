import type { EChartsOption } from "echarts";
import * as echarts from "echarts/core";
import { DataZoomComponent, GridComponent, MarkLineComponent, TooltipComponent } from "echarts/components";
import { LineChart } from "echarts/charts";
import { CanvasRenderer } from "echarts/renderers";
import { useEffect, useMemo, useRef } from "react";
import type { DriftEvent, MetricPoint } from "../types";

echarts.use([CanvasRenderer, DataZoomComponent, GridComponent, LineChart, MarkLineComponent, TooltipComponent]);

interface TimeSeriesChartProps {
  points: MetricPoint[];
  events: DriftEvent[];
  height?: number;
}

const severityColor: Record<string, string> = {
  INFO: "#2563eb",
  WARNING: "#d97706",
  CRITICAL: "#dc2626"
};

const phaseLabel: Record<string, string> = {
  STARTED: "START",
  ONGOING: "ONGOING",
  RECOVERED: "RECOVERED"
};

export function TimeSeriesChart({ points, events, height = 260 }: TimeSeriesChartProps) {
  const elementRef = useRef<HTMLDivElement | null>(null);
  const option = useMemo(() => buildOption(points, events), [points, events]);

  useEffect(() => {
    if (!elementRef.current) {
      return;
    }
    const chart = echarts.init(elementRef.current, undefined, { renderer: "canvas" });
    chart.setOption(option);
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [option]);

  return <div className="chart" ref={elementRef} style={{ height }} />;
}

function buildOption(points: MetricPoint[], events: DriftEvent[]): EChartsOption {
  const sortedPoints = [...points].sort((left, right) => Date.parse(left.timestamp) - Date.parse(right.timestamp));
  const values = sortedPoints.map((point) => [point.timestamp, point.value]);
  const markerLabels = labeledMarkers(events);
  const markLines = events.map((event) => ({
    xAxis: event.detectedAt,
    lineStyle: {
      color: severityColor[event.severity] ?? "#dc2626",
      width: event.severity === "CRITICAL" ? 2 : 1,
      type: event.severity === "CRITICAL" ? "solid" as const : "dashed" as const
    },
    label: {
      formatter: markerLabels.get(event.id) ?? "",
      color: severityColor[event.severity] ?? "#dc2626",
      fontSize: 11,
      fontWeight: 700,
      distance: 4
    }
  }));

  return {
    animation: false,
    color: ["#2563eb"],
    grid: {
      top: 34,
      right: 18,
      bottom: 58,
      left: 54
    },
    tooltip: {
      trigger: "axis",
      valueFormatter: (value) => Number(value).toFixed(3)
    },
    dataZoom: [
      { type: "inside", throttle: 40 },
      { type: "slider", height: 22, bottom: 18 }
    ],
    xAxis: {
      type: "time",
      axisLabel: {
        hideOverlap: true,
        formatter: (value: number) => {
          return new Intl.DateTimeFormat("ru-RU", {
            timeZone: "Europe/Moscow",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
          }).format(value);
        }
      }
    },
    yAxis: {
      type: "value",
      scale: true,
      splitLine: {
        lineStyle: {
          color: "#e5e7eb"
        }
      }
    },
    series: [
      {
        type: "line",
        name: "value",
        data: values,
        showSymbol: false,
        smooth: true,
        lineStyle: {
          width: 2
        },
        areaStyle: {
          color: "rgba(37, 99, 235, 0.08)"
        },
        markLine: {
          silent: true,
          symbol: "none",
          data: markLines
        }
      }
    ]
  };
}

function labeledMarkers(events: DriftEvent[]) {
  const sortedEvents = [...events].sort((left, right) => Date.parse(left.detectedAt) - Date.parse(right.detectedAt));
  const labeled = new Map<string, string>();
  const minGapMillis = markerGapMillis(sortedEvents);
  let cluster: DriftEvent[] = [];
  let clusterStart = 0;

  for (const event of sortedEvents) {
    const timestamp = Date.parse(event.detectedAt);
    if (!Number.isFinite(timestamp)) {
      continue;
    }

    if (cluster.length === 0) {
      cluster = [event];
      clusterStart = timestamp;
      continue;
    }

    if (timestamp - clusterStart < minGapMillis) {
      cluster.push(event);
    } else {
      addClusterLabel(labeled, cluster);
      cluster = [event];
      clusterStart = timestamp;
    }
  }

  if (cluster.length > 0) {
    addClusterLabel(labeled, cluster);
  }

  return labeled;
}

function markerGapMillis(events: DriftEvent[]) {
  if (events.length < 2) {
    return 0;
  }
  const times = events.map((event) => Date.parse(event.detectedAt)).filter(Number.isFinite);
  const span = Math.max(...times) - Math.min(...times);
  return Math.max(18_000, span * 0.07);
}

function addClusterLabel(labels: Map<string, string>, events: DriftEvent[]) {
  const representative = [...events].sort(eventPriority)[0];
  labels.set(representative.id, markerLabel(representative, events.length));
}

function eventPriority(left: DriftEvent, right: DriftEvent) {
  return priority(right) - priority(left);
}

function priority(event: DriftEvent) {
  const severity = event.severity === "CRITICAL" ? 30 : event.severity === "WARNING" ? 20 : 10;
  const phase = event.phase === "STARTED" ? 3 : event.phase === "RECOVERED" ? 2 : 1;
  return severity + phase;
}

function markerLabel(event: DriftEvent, clusterSize = 1) {
  const suffix = clusterSize > 1 ? ` +${clusterSize - 1}` : "";
  if (event.phase === "RECOVERED") {
    return `${phaseLabel[event.phase]}${suffix}`;
  }
  return `${phaseLabel[event.phase] ?? event.phase} ${severityShortLabel(event.severity)}${suffix}`;
}

function severityShortLabel(severity: string) {
  return severity === "CRITICAL" ? "CRIT" : severity === "WARNING" ? "WARN" : severity;
}
