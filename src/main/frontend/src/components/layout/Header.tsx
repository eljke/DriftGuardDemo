import { StatusPill } from "../ui";
import type { DemoRunResult, DriftEvent, KafkaDemoStatus } from "../../types";
import { useI18n } from "../../i18n";
import { NotificationCenter } from "../../features/alerts/NotificationCenter";

export function Header({ events, overview, kafka }: { events: DriftEvent[]; overview?: DemoRunResult; kafka?: KafkaDemoStatus }) {
  const { locale, setLocale, t } = useI18n();

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">{t("app.eyebrow")}</p>
        <h1>{t("app.title")}</h1>
      </div>
      <div className="status-strip">
        <div className="language-switcher" aria-label={t("language.label")}>
          <button className={locale === "ru" ? "active" : ""} onClick={() => setLocale("ru")} type="button">RU</button>
          <button className={locale === "en" ? "active" : ""} onClick={() => setLocale("en")} type="button">EN</button>
        </div>
        <NotificationCenter events={events} />
        <StatusPill label={t("status.synthetic")} active={Boolean(overview?.running)} />
        <StatusPill label={t("status.kafka")} active={Boolean(kafka?.running)} />
      </div>
    </header>
  );
}
