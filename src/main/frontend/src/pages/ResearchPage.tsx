import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, FlaskConical, Loader2, Square } from "lucide-react";
import { useMemo, useState } from "react";
import { api } from "../api/client";
import { MetricCard, Notice, Panel, Progress } from "../components/ui";
import { ResearchF1Chart } from "../features/research/ResearchF1Chart";
import { useI18n } from "../i18n";
import { readableError } from "../lib/format";
import type {
  DemoScenarioDescriptor,
  ResearchAggregate,
  ResearchExperimentRequest,
  ResearchStrategy
} from "../types";

const defaultScenarios = [
  "latency-step",
  "error-rate-spike",
  "throughput-drop",
  "queue-growth",
  "seasonal-latency"
];

export function ResearchPage({ scenarios }: { scenarios: DemoScenarioDescriptor[] }) {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [request, setRequest] = useState<ResearchExperimentRequest>({
    repetitions: 10,
    samples: 200,
    baseSeed: 1000,
    scenarios: defaultScenarios,
    noiseMultipliers: [0.5, 1, 2],
    effectMultipliers: [0.75, 1, 1.25]
  });
  const job = useQuery({
    queryKey: ["research"],
    queryFn: api.research,
    refetchInterval: (query) => query.state.data?.status === "RUNNING" ? 500 : false
  });
  const start = useMutation({
    mutationFn: api.startResearch,
    onSuccess: (data) => queryClient.setQueryData(["research"], data)
  });
  const cancel = useMutation({
    mutationFn: api.cancelResearch,
    onSuccess: (data) => queryClient.setQueryData(["research"], data)
  });
  const snapshot = job.data;
  const report = snapshot?.report;
  const adaptive = useMemo(
    () => report?.aggregates.filter((result) => result.strategy === "ADAPTIVE") ?? [],
    [report]
  );
  const best = useMemo(() => {
    if (!report?.aggregates.length) return undefined;
    return [...report.aggregates].sort((left, right) => right.meanF1 - left.meanF1)[0];
  }, [report]);
  const error = start.error ?? cancel.error ?? job.error;

  return (
    <section className="stack">
      <Panel title={t("research.title")} className="research-intro">
        <div className="research-heading">
          <div>
            <p className="eyebrow">{t("research.eyebrow")}</p>
            <h3>{t("research.heading")}</h3>
            <p>{t("research.description")}</p>
          </div>
          <FlaskConical size={34} />
        </div>
        <div className="research-hypothesis">
          <strong>{t("research.hypothesis")}</strong>
          <span>{report?.hypothesis ?? t("research.hypothesisText")}</span>
        </div>
      </Panel>

      <Panel title={t("research.matrix")}>
        {error && <Notice tone="error" text={readableError(error)} />}
        <div className="research-controls">
          <NumberControl label={t("research.repetitions")} min={2} max={100} value={request.repetitions} onChange={(value) => setRequest({ ...request, repetitions: value })} />
          <NumberControl label={t("research.samples")} min={100} max={2000} step={50} value={request.samples} onChange={(value) => setRequest({ ...request, samples: value })} />
          <NumberControl label={t("research.seed")} min={1} max={1000000} value={request.baseSeed} onChange={(value) => setRequest({ ...request, baseSeed: value })} />
        </div>
        <div className="research-scenarios">
          {(scenarios.length ? scenarios : defaultScenarios.map((id) => ({ id, title: id, metric: "", description: "" })))
            .filter((scenario) => defaultScenarios.includes(scenario.id))
            .map((scenario) => (
              <label key={scenario.id}>
                <input
                  checked={request.scenarios.includes(scenario.id)}
                  disabled={snapshot?.status === "RUNNING"}
                  type="checkbox"
                  onChange={() => setRequest({
                    ...request,
                    scenarios: request.scenarios.includes(scenario.id)
                      ? request.scenarios.filter((id) => id !== scenario.id)
                      : [...request.scenarios, scenario.id]
                  })}
                />
                <span>{scenario.title}</span>
              </label>
            ))}
        </div>
        <p className="help-text">{t("research.matrixHelp")}</p>
        <div className="actions">
          <button
            className="primary-button"
            disabled={snapshot?.status === "RUNNING" || request.scenarios.length === 0}
            onClick={() => start.mutate(request)}
            type="button"
          >
            {snapshot?.status === "RUNNING" ? <Loader2 className="spin" size={16} /> : <FlaskConical size={16} />}
            {t("research.run")}
          </button>
          <button
            className="secondary-button"
            disabled={snapshot?.status !== "RUNNING"}
            onClick={() => cancel.mutate()}
            type="button"
          >
            <Square size={15} />
            {t("research.cancel")}
          </button>
        </div>
        {snapshot?.status === "RUNNING" && (
          <div className="research-progress">
            <Progress value={snapshot.completedTrials} max={snapshot.totalTrials} />
          </div>
        )}
      </Panel>

      {report && (
        <>
          <div className="summary-grid">
            <MetricCard title={t("research.trials")} value={report.totalTrials} helper={t("research.paired")} />
            <MetricCard title={t("research.bestF1")} value={best ? percent(best.meanF1) : "—"} helper={best ? `${best.strategy} · ${best.scenario}` : "—"} />
            <MetricCard title={t("research.adaptiveWins")} value={adaptiveWins(report.aggregates)} helper={t("research.scenarioCount")} />
            <MetricCard title={t("research.completed")} value={new Date(report.completedAt).toLocaleString()} helper={t("research.reproducible")} />
          </div>

          <Panel title={t("research.f1Chart")}>
            <ResearchF1Chart results={report.aggregates} />
            <p className="help-text">{t("research.ciHelp")}</p>
          </Panel>

          <Panel title={t("research.results")}>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{t("research.scenario")}</th>
                    <th>{t("research.strategy")}</th>
                    <th>F1</th>
                    <th>95% CI</th>
                    <th>Precision</th>
                    <th>Recall</th>
                    <th>FP/1000</th>
                    <th>{t("research.delay")}</th>
                    <th>{t("research.profileChoice")}</th>
                  </tr>
                </thead>
                <tbody>
                  {report.aggregates.map((result) => (
                    <tr key={`${result.scenario}-${result.strategy}`}>
                      <td>{result.scenario}</td>
                      <td><StrategyBadge strategy={result.strategy} /></td>
                      <td>{percent(result.meanF1)}</td>
                      <td>{percent(result.f1ConfidenceLow)}-{percent(result.f1ConfidenceHigh)}</td>
                      <td>{percent(result.meanPrecision)}</td>
                      <td>{percent(result.meanRecall)}</td>
                      <td>{result.meanFalsePositiveEventsPerThousand.toFixed(2)}</td>
                      <td>{result.meanDetectionDelaySamples.toFixed(1)}</td>
                      <td>{Object.entries(result.selectedProfiles).map(([profile, count]) => `${profile}: ${count}`).join(", ")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="actions research-exports">
              <a className="secondary-button" href="/api/research/export.csv">
                <Download size={16} /> CSV
              </a>
              <a className="secondary-button" href="/api/research/export.md">
                <Download size={16} /> Markdown
              </a>
            </div>
          </Panel>
        </>
      )}
    </section>
  );
}

function NumberControl({ label, min, max, step = 1, value, onChange }: {
  label: string;
  min: number;
  max: number;
  step?: number;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input max={max} min={min} step={step} type="number" value={value} onChange={(event) => onChange(Number(event.target.value))} />
    </label>
  );
}

function StrategyBadge({ strategy }: { strategy: ResearchStrategy }) {
  return <span className={`research-strategy ${strategy.toLowerCase()}`}>{strategy}</span>;
}

function adaptiveWins(results: ResearchAggregate[]) {
  const scenarios = [...new Set(results.map((result) => result.scenario))];
  return scenarios.filter((scenario) => {
    const rows = results.filter((result) => result.scenario === scenario);
    const adaptive = rows.find((result) => result.strategy === "ADAPTIVE");
    return adaptive && adaptive.meanF1 >= Math.max(...rows.map((result) => result.meanF1));
  }).length;
}

function percent(value: number) {
  return `${(value * 100).toFixed(1)}%`;
}
