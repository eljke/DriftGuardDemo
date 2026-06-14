export type Severity = "INFO" | "WARNING" | "CRITICAL";

export interface MetricKey {
  service: string;
  metric: string;
  instance?: string;
  operation?: string;
}

export interface MetricPoint {
  key: MetricKey;
  timestamp: string;
  value: number;
  kind: string;
  tags: Record<string, string>;
  attributes: Record<string, unknown>;
}

export interface DriftEvent {
    id: string;
    key: MetricKey;
    detectedAt: string;
    phase: string;
    direction: string;
    detector: string;
    algorithm: string;
    severity: Severity;
    score: number;
    currentValue: number;
    baselineValue: number;
    baselineSummary?: string;
    currentSummary?: string;
    reason: string;
    metadata: Record<string, string>;
    tags: Record<string, string>;
    details: Record<string, unknown>;
}

export interface DemoStoredDriftEvent {
  source: string;
  runId: string;
  receivedAt: string;
  event: DriftEvent;
}

export interface DemoWebhookAlertPayload {
  id: string;
  severity: Severity;
  title: string;
  message: string;
  service: string;
  metric: string;
  operation?: string;
  labels: Record<string, string>;
}

export interface DemoWebhookDelivery {
  acceptedAt: string;
  channel?: string;
  payload: DemoWebhookAlertPayload;
}

export interface DetectionMetrics {
  detected: boolean;
  events: number;

  truePositive?: boolean;
  falsePositive?: boolean;
  missed?: boolean;
  detectionDelaySamples?: number;
  detectionDelay?: string;

  truePositiveEvents?: number;
  falsePositiveEvents?: number;
  expectedDriftIntervals?: number;
  detectedDriftIntervals?: number;
  missedDriftIntervals?: number;
  firstDetectionDelay?: string;
  precision?: number;
  recall?: number;
}

export interface DetectionBenchmarkReport {
  label: string;
  results: DetectionBenchmarkResult[];
  summary: DetectionBenchmarkSummary;
}

export interface DetectionBenchmarkResult {
  scenario: string;
  metrics: DetectionMetrics;
}

export interface DetectionBenchmarkSummary {
  scenarios: number;
  detectedScenarios: number;
  events: number;
  truePositiveEvents: number;
  falsePositiveEvents: number;
  expectedDriftIntervals: number;
  detectedDriftIntervals: number;
  missedDriftIntervals: number;
  precision: number;
  recall: number;
  meanFirstDetectionDelay: string;
}

export interface DriftInterval {
  start: string;
  end: string;
  description: string;
}

export interface DemoRunResult {
  scenario: string;
  title: string;
  mode: string;
  running: boolean;
  processedPoints: number;
  metricPoints: number;
  samplePoints: MetricPoint[];
  expectedDrifts: DriftInterval[];
  events: DriftEvent[];
  quality: DetectionMetrics;
}

export interface DemoScenarioDescriptor {
  id: string;
  title: string;
  metric: string;
  description: string;
}

export interface DemoScenarioRequest {
  samples?: number;
  baselineValue?: number;
  driftValue?: number;
  noiseStdDev?: number;
  driftStartPercent?: number;
  spikeLengthPercent?: number;
}

export interface KafkaProducerStatus {
  id: string;
  service: string;
  metric: string;
  operation?: string;
  producedPoints: number;
  totalPoints: number;
  running: boolean;
}

export interface KafkaDemoStatus {
  enabled: boolean;
  running: boolean;
  replay: boolean;
  scenario: string;
  inputTopic: string;
  outputTopic: string;
  speed: number;
  bootstrapServers: string;
  producedPoints: number;
  totalPoints: number;
  producers: KafkaProducerStatus[];
  consumedEvents: DriftEvent[];
  samplePoints: MetricPoint[];
  error?: string;
}


export interface KafkaOperationsMetrics {
  processedPoints: number;
  emittedEvents: number;
  failedPoints: number;
  routedErrors: number;
  durationMeasurements: number;
  totalDurationMillis: number;
  maxDurationMillis: number;
  meanDurationMillis: number;
}

export interface KafkaOperationsSnapshot {
  enabled: boolean;
  running: boolean;
  replay: boolean;
  scenario: string;
  inputTopic: string;
  outputTopic: string;
  bootstrapServers: string;
  producedPoints: number;
  totalPoints: number;
  consumedEvents: number;
  progressPercent: number;
  streamsApplicationId: string;
  streamsInputTopics: string[];
  streamsOutputTopic: string;
  runtimeStateStoreName: string;
  detectionErrorMode: string;
  telemetryEnabled: boolean;
  metrics: KafkaOperationsMetrics;
  error?: string;
}

export interface KafkaReplayRequest extends DemoScenarioRequest {
  scenario: string;
  speed: number;
  resetState: boolean;
  profile?: string;
}


export type DemoCapabilityStatus = "READY" | "PARTIAL" | "PLANNED";

export interface DemoCapability {
  id: string;
  title: string;
  description: string;
  category: string;
  status: DemoCapabilityStatus;
  apiEndpoints: string[];
  uiSurfaces: string[];
}

export interface DemoCapabilityGroup {
  id: string;
  title: string;
  description: string;
  capabilities: DemoCapability[];
}

export interface ToolLink {
  id: string;
  title: string;
  url: string;
  description: string;
}

export type DemoHelp = Record<string, string>;

export interface DemoConfigurationView {
  aggressiveness: {
    level: string;
    description: string;
  };
  availableProfiles: string[];
  registeredAlgorithms: string[];
  kafka: {
    demoEnabled: boolean;
    bootstrapServers: string;
    inputTopic: string;
    outputTopic: string;
    applicationId: string;
    playbackInterval: string;
  };
  detectors: DetectorConfigurationView[];
}

export type CheckoutMode = "NORMAL" | "DEGRADED" | "OUTAGE";

export interface CheckoutOperationRequest {
  operation?: string;
  customerId?: string;
}

export interface CheckoutOperationResult {
  id: number;
  operation: string;
  customerId: string;
  success: boolean;
  latencyMillis: number;
  queueSize: number;
  mode: CheckoutMode;
  occurredAt: string;
  alerts: DriftEvent[];
}

export interface CheckoutServiceSnapshot {
  running: boolean;
  mode: CheckoutMode;
  operations: number;
  successes: number;
  failures: number;
  errorRate: number;
  meanLatencyMillis: number;
  queueSize: number;
  throughputPerMinute: number;
  recentOperations: CheckoutOperationResult[];
  recentMetrics: MetricPoint[];
  recentAlerts: DriftEvent[];
}

export interface DetectorConfigurationView {
  name: string;
  algorithm: string;
  services: string[];
  metrics: string[];
  warningThreshold: number;
  criticalThreshold: number;
  warningPValue: number;
  criticalPValue: number;
  warmupSamples: number;
  emissionPolicy: {
    minConsecutiveSignals: number;
    cooldown: string;
    recoveryConsecutiveNormal: number;
  };
  sensitivity: string;
}

export type ResearchStrategy = "AGGRESSIVE" | "BALANCED" | "CONSERVATIVE" | "ADAPTIVE";
export type ResearchJobStatus = "IDLE" | "RUNNING" | "COMPLETED" | "CANCELLED" | "FAILED";

export interface ResearchExperimentRequest {
  repetitions: number;
  samples: number;
  baseSeed: number;
  scenarios: string[];
  noiseMultipliers: number[];
  effectMultipliers: number[];
}

export interface ResearchAggregate {
  scenario: string;
  strategy: ResearchStrategy;
  trials: number;
  meanPrecision: number | null;
  meanRecall: number | null;
  meanF1: number | null;
  f1ConfidenceLow: number | null;
  f1ConfidenceHigh: number | null;
  meanFalsePositiveEventsPerThousand: number;
  meanDetectionDelaySamples: number | null;
  detectionRate: number | null;
  meanSpecificity: number | null;
  falseAlarmFreeRate: number;
  meanTimeToFirstFalseAlarmSamples: number | null;
  selectedProfiles: Record<string, number>;
}

export interface ResearchTrial {
  scenario: string;
  strategy: ResearchStrategy;
  selectedProfile: string;
  seed: number;
  noiseMultiplier: number;
  effectMultiplier: number;
  driftExpected: boolean;
  precision: number | null;
  recall: number | null;
  f1: number | null;
  falsePositiveEvents: number;
  falsePositiveEventsPerThousand: number;
  detectionDelaySamples: number | null;
  specificity: number | null;
  falseAlarmFree: boolean;
  timeToFirstFalseAlarmSamples: number | null;
  detected: boolean;
}

export interface ResearchCalibrationSummary {
  calibrationRepetitions: number;
  holdoutRepetitions: number;
  calibrationTrials: number;
  evaluatedTrials: number;
  trainingExamples: number;
  bestGlobalProfile: string;
  bestProfileLabels: Record<string, number>;
}

export interface ResearchComparison {
  scope: string;
  baselineProfile: string;
  pairs: number;
  meanAdaptiveUtility: number;
  meanBaselineUtility: number;
  meanDelta: number;
  relativeImprovementPercent: number;
  confidenceLow: number;
  confidenceHigh: number;
  wilcoxonPValue: number;
  adaptiveWins: number;
  adaptiveLosses: number;
  ties: number;
}

export interface ResearchExperimentReport {
  hypothesis: string;
  method: string;
  completedAt: string;
  request: ResearchExperimentRequest;
  totalTrials: number;
  calibration: ResearchCalibrationSummary;
  aggregates: ResearchAggregate[];
  comparisons: ResearchComparison[];
  trials: ResearchTrial[];
}

export interface ResearchJobSnapshot {
  jobId?: string;
  status: ResearchJobStatus;
  completedTrials: number;
  totalTrials: number;
  progressPercent: number;
  startedAt?: string;
  finishedAt?: string;
  error?: string;
  report?: ResearchExperimentReport;
}
