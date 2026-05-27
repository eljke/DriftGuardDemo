import type { ReactNode } from "react";
import type { Page } from "../../app/navigation";
import type { DemoRunResult, DriftEvent, KafkaDemoStatus } from "../../types";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";

export function AppShell({
  children,
  kafka,
  notificationEvents,
  overview,
  page,
  onPageChange,
  showLab
}: {
  children: ReactNode;
  kafka?: KafkaDemoStatus;
  notificationEvents: DriftEvent[];
  overview?: DemoRunResult;
  page: Page;
  onPageChange: (page: Page) => void;
  showLab: boolean;
}) {
  return (
    <div className="app-shell">
      <Sidebar page={page} onPageChange={onPageChange} showLab={showLab} />
      <main className="main">
        <Header events={notificationEvents} overview={overview} kafka={kafka} />
        {children}
      </main>
    </div>
  );
}
