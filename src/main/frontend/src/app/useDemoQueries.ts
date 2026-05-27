import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";

export function useDemoQueries(showLab = false) {
  const service = useQuery({ queryKey: ["service"], queryFn: api.serviceStatus, refetchInterval: 750 });
  const serviceOperations = useQuery({ queryKey: ["service-operations"], queryFn: api.serviceOperations });
  const overview = useQuery({ queryKey: ["overview"], queryFn: api.overview, refetchInterval: 750, enabled: showLab });
  const scenarios = useQuery({ queryKey: ["scenarios"], queryFn: api.scenarios });
  const kafka = useQuery({ queryKey: ["kafka"], queryFn: api.kafkaStatus, refetchInterval: 750 });
  const kafkaOperations = useQuery({ queryKey: ["kafka-operations"], queryFn: api.kafkaOperations, refetchInterval: 1500 });
  const tools = useQuery({ queryKey: ["tools"], queryFn: api.tools });
  const help = useQuery({ queryKey: ["help"], queryFn: api.help });
  const capabilities = useQuery({ queryKey: ["capabilities"], queryFn: api.capabilities, enabled: showLab });
  const configuration = useQuery({ queryKey: ["configuration"], queryFn: api.configuration });
  const storedEvents = useQuery({ queryKey: ["stored-events"], queryFn: api.storedEvents, refetchInterval: 750 });
  const webhookDeliveries = useQuery({ queryKey: ["webhook-deliveries"], queryFn: api.webhookDeliveries, refetchInterval: 750 });

  return {
    service,
    serviceOperations,
    overview,
    scenarios,
    kafka,
    kafkaOperations,
    tools,
    help,
    capabilities,
    configuration,
    storedEvents,
    webhookDeliveries
  };
}
