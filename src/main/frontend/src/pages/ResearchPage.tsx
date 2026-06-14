import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, FlaskConical, Loader2, Square } from "lucide-react";
import { useMemo, useState } from "react";
import { api } from "../api/client";
import { MetricCard, Notice, Panel, Progress } from "../components/ui";
import { ResearchF1Chart } from "../features/research/ResearchF1Chart";
import { useI18n } from "../i18n";
import { readableError } from "../lib/format";
import type { DemoScenarioDescriptor, ResearchExperimentRequest, ResearchStrategy } from "../types";

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
  const overallComparison = useMemo(
    () => report?.comparisons.find((comparison) => comparison.scope === "ALL"),
    [report]
  );
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
            <MetricCard title={t("research.holdoutTrials")} value={report.totalTrials} helper={`${report.calibration.calibrationTrials} ${t("research.calibrationTrials")}`} />
            <MetricCard
              title={t("research.utilityGain")}
              value={overallComparison ? signedPercent(relativeImprovement(overallComparison)) : "—"}
              helper={overallComparison
                ? `${overallComparison.meanBaselineUtility.toFixed(4)} → ${overallComparison.meanAdaptiveUtility.toFixed(4)}; Δ ${signed(overallComparison.meanDelta)} (${signedPercentagePoints(overallComparison.meanDelta)})`
                : "—"}
            />
            <MetricCard title={t("research.significance")} value={overallComparison ? formatP(overallComparison.wilcoxonPValue) : "—"} helper={overallComparison && overallComparison.wilcoxonPValue < 0.05 ? t("research.significant") : t("research.notSignificant")} />
            <MetricCard title={t("research.baseline")} value={report.calibration.bestGlobalProfile} helper={`${report.calibration.calibrationRepetitions}/${report.calibration.holdoutRepetitions} ${t("research.split")}`} />
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
                    <th>{t("research.specificity")}</th>
                    <th>{t("research.alarmFree")}</th>
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
                      <td>{percentInterval(result.f1ConfidenceLow, result.f1ConfidenceHigh)}</td>
                      <td>{percent(result.meanPrecision)}</td>
                      <td>{percent(result.meanRecall)}</td>
                      <td>{result.meanFalsePositiveEventsPerThousand.toFixed(2)}</td>
                      <td>{percent(result.meanSpecificity)}</td>
                      <td>{percent(result.falseAlarmFreeRate)}</td>
                      <td>{decimal(result.meanDetectionDelaySamples)}</td>
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

          <Panel title={t("research.comparison")}>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{t("research.scope")}</th>
                    <th>{t("research.baseline")}</th>
                    <th>{t("research.pairs")}</th>
                    <th>Δ utility</th>
                    <th>{t("research.relativeGain")}</th>
                    <th>Bootstrap 95% CI</th>
                    <th>Wilcoxon p</th>
                    <th>{t("research.wins")}</th>
                    <th>{t("research.losses")}</th>
                    <th>{t("research.ties")}</th>
                  </tr>
                </thead>
                <tbody>
                  {report.comparisons.map((comparison) => (
                    <tr key={comparison.scope}>
                      <td>{comparison.scope}</td>
                      <td>{comparison.baselineProfile}</td>
                      <td>{comparison.pairs}</td>
                      <td>{signed(comparison.meanDelta)}</td>
                      <td>{signedPercent(relativeImprovement(comparison))}</td>
                      <td>{interval(comparison.confidenceLow, comparison.confidenceHigh)}</td>
                      <td>{formatP(comparison.wilcoxonPValue)}</td>
                      <td>{comparison.adaptiveWins}</td>
                      <td>{comparison.adaptiveLosses}</td>
                      <td>{comparison.ties}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
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

function percent(value: number | null) {
  return value == null ? "N/A" : `${(value * 100).toFixed(1)}%`;
}

function decimal(value: number | null) {
  return value == null ? "N/A" : value.toFixed(1);
}

function interval(low: number | null, high: number | null) {
  return low == null || high == null ? "N/A" : `${signed(low)}…${signed(high)}`;
}

function percentInterval(low: number | null, high: number | null) {
  return low == null || high == null ? "N/A" : `${percent(low)}…${percent(high)}`;
}

function signed(value: number) {
  return `${value >= 0 ? "+" : ""}${value.toFixed(4)}`;
}

function signedPercent(value: number) {
  return `${value >= 0 ? "+" : ""}${value.toFixed(2)}%`;
}

function signedPercentagePoints(value: number) {
  const percentagePoints = value * 100;
  return `${percentagePoints >= 0 ? "+" : ""}${percentagePoints.toFixed(2)} p.p.`;
}

function relativeImprovement(comparison: {
  meanAdaptiveUtility: number;
  meanBaselineUtility: number;
  relativeImprovementPercent?: number;
}) {
  return comparison.relativeImprovementPercent
    ?? (comparison.meanAdaptiveUtility - comparison.meanBaselineUtility)
      / Math.abs(comparison.meanBaselineUtility)
      * 100;
}

function formatP(value: number) {
  return value < 0.001 ? "< 0.001" : value.toFixed(3);
}
