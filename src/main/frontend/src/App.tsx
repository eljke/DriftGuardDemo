import { useMemo, useState } from "react";
import { useDemoQueries } from "./app/useDemoQueries";
import type { Page } from "./app/navigation";
import { AppShell } from "./components/layout";
import { ApiStatusBanner } from "./components/ui";
import { KafkaPage } from "./features/kafka/KafkaPage";
import { ConfigurationPage } from "./pages/ConfigurationPage";
import { OverviewPage } from "./pages/OverviewPage";
import { ServicePage } from "./pages/ServicePage";
import { SyntheticPage } from "./pages/SyntheticPage";
import { ToolsPage } from "./pages/ToolsPage";
import { useI18n } from "./i18n";
import type { DriftEvent } from "./types";

export default function App() {
  const [showLab] = useState(() => new URLSearchParams(window.location.search).get("lab") === "1" || localStorage.getItem("driftguard.showLab") === "true");
  const [page, setPage] = useState<Page>("service");
  const { t } = useI18n();
  const queries = useDemoQueries(showLab);
  const { capabilities, configuration, help, kafka, kafkaOperations, overview, scenarios, service, serviceOperations, storedEvents, tools, webhookDeliveries } = queries;
  const notificationEvents = useMemo(() => {
    const byId = new Map<string, DriftEvent>();
    for (const event of [
      ...(service.data?.recentAlerts ?? []),
      ...(kafka.data?.consumedEvents ?? []),
      ...(overview.data?.events ?? []),
      ...(storedEvents.data ?? []).map((stored) => stored.event)
    ]) {
      byId.set(event.id, event);
    }
    return [...byId.values()];
  }, [kafka.data?.consumedEvents, overview.data?.events, service.data?.recentAlerts, storedEvents.data]);

  return (
    <AppShell page={page} onPageChange={setPage} notificationEvents={notificationEvents} overview={overview.data} kafka={kafka.data} showLab={showLab}>
      <ApiStatusBanner
        items={[
          { label: t("nav.service"), error: service.error, retry: () => service.refetch() },
          { label: t("nav.kafka"), error: kafka.error, retry: () => kafka.refetch() },
          { label: t("kafka.operations"), error: kafkaOperations.error, retry: () => kafkaOperations.refetch() },
          ...(showLab ? [
            { label: t("nav.overview"), error: overview.error, retry: () => overview.refetch() },
            { label: t("synthetic.title"), error: scenarios.error, retry: () => scenarios.refetch() }
          ] : []),
          { label: t("nav.configuration"), error: configuration.error, retry: () => configuration.refetch() },
          { label: t("overview.recentStored"), error: storedEvents.error, retry: () => storedEvents.refetch() },
          { label: t("alerts.webhookDeliveries"), error: webhookDeliveries.error, retry: () => webhookDeliveries.refetch() },
          ...(showLab ? [{ label: t("capabilities.title"), error: capabilities.error, retry: () => capabilities.refetch() }] : []),
          { label: "Help", error: help.error, retry: () => help.refetch() }
        ]}
      />
      {page === "service" && <ServicePage service={service.data} operations={serviceOperations.data ?? []} webhookDeliveries={webhookDeliveries.data ?? []} />}
      {page === "overview" && <OverviewPage result={overview.data} kafka={kafka.data} storedEvents={storedEvents.data ?? []} capabilities={capabilities.data ?? []} />}
      {page === "synthetic" && <SyntheticPage result={overview.data} scenarios={scenarios.data ?? []} />}
      {page === "kafka" && <KafkaPage status={kafka.data} operations={kafkaOperations.data} scenarios={scenarios.data ?? []} configuration={configuration.data} />}
      {page === "configuration" && <ConfigurationPage configuration={configuration.data} />}
      {page === "tools" && <ToolsPage endpoints={help.data ?? {}} tools={tools.data ?? []} />}
    </AppShell>
  );
}
