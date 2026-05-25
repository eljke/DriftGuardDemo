import { Activity, BarChart3, Cable, Settings, ShoppingCart, Wrench } from "lucide-react";

export type Page = "service" | "overview" | "synthetic" | "kafka" | "configuration" | "tools";

export const navigation: Array<{ page: Page; labelKey: string; icon: typeof Activity; lab?: boolean }> = [
  { page: "service", labelKey: "nav.service", icon: ShoppingCart },
  { page: "kafka", labelKey: "nav.kafka", icon: Cable },
  { page: "overview", labelKey: "nav.overview", icon: Activity, lab: true },
  { page: "synthetic", labelKey: "nav.synthetic", icon: BarChart3, lab: true },
  { page: "configuration", labelKey: "nav.configuration", icon: Settings },
  { page: "tools", labelKey: "nav.tools", icon: Wrench }
];
