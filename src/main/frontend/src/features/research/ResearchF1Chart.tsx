import type { EChartsOption } from "echarts";
import * as echarts from "echarts/core";
import { BarChart } from "echarts/charts";
import { GridComponent, LegendComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { useEffect, useMemo, useRef } from "react";
import type { ResearchAggregate, ResearchStrategy } from "../../types";

echarts.use([BarChart, CanvasRenderer, GridComponent, LegendComponent, TooltipComponent]);

const strategies: ResearchStrategy[] = ["AGGRESSIVE", "BALANCED", "CONSERVATIVE", "ADAPTIVE"];
const colors: Record<ResearchStrategy, string> = {
  AGGRESSIVE: "#dc2626",
  BALANCED: "#2563eb",
  CONSERVATIVE: "#64748b",
  ADAPTIVE: "#059669"
};

export function ResearchF1Chart({ results }: { results: ResearchAggregate[] }) {
  const elementRef = useRef<HTMLDivElement | null>(null);
  const option = useMemo(() => buildOption(results), [results]);

  useEffect(() => {
    if (!elementRef.current) return;
    const chart = echarts.init(elementRef.current, undefined, { renderer: "canvas" });
    chart.setOption(option);
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [option]);

  return <div className="research-chart" ref={elementRef} />;
}

function buildOption(results: ResearchAggregate[]): EChartsOption {
  const scenarios = [...new Set(results.map((result) => result.scenario))];
  return {
    animation: false,
    color: strategies.map((strategy) => colors[strategy]),
    grid: { top: 48, right: 20, bottom: 68, left: 52 },
    legend: { top: 8 },
    tooltip: {
      trigger: "axis",
      formatter: (items) => {
        const rows = Array.isArray(items) ? items : [items];
        return rows.map((item) => {
          const result = results.find((candidate) =>
            candidate.scenario === item.name && candidate.strategy === item.seriesName
          );
          return result?.meanF1 != null
            ? `${item.marker} ${item.seriesName}: ${(result.meanF1 * 100).toFixed(1)}% `
              + `[${((result.f1ConfidenceLow ?? 0) * 100).toFixed(1)}-${((result.f1ConfidenceHigh ?? 0) * 100).toFixed(1)}]`
            : "";
        }).join("<br/>");
      }
    },
    xAxis: {
      type: "category",
      data: scenarios,
      axisLabel: { interval: 0, rotate: scenarios.length > 3 ? 24 : 0 }
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 1,
      axisLabel: { formatter: (value: number) => `${Math.round(value * 100)}%` }
    },
    series: strategies.map((strategy) => ({
      type: "bar",
      name: strategy,
      data: scenarios.map((scenario) =>
        results.find((result) => result.scenario === scenario && result.strategy === strategy)?.meanF1 ?? null
      ),
      itemStyle: { color: colors[strategy] }
    }))
  };
}
