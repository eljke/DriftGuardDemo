# Методика исследования DriftGuard

## Цель

Оценить, может ли выбор профиля чувствительности по характеристикам baseline повысить качество обнаружения drift в ранее не наблюдавшихся потоках по сравнению с фиксированным профилем.

Исследуется не новый drift detector, а мета-алгоритм выбора конфигурации существующего Page-Hinkley detector. Это потенциальный элемент научной новизны ВКР, но утверждение о новизне требует обзора и сравнения с опубликованными adaptive drift detection и algorithm selection методами.

## Гипотезы

**H0:** математическое ожидание парной разницы utility между adaptive selector и лучшим глобальным фиксированным профилем равно нулю.

**H1:** математическое ожидание парной разницы utility положительно на независимой hold-out выборке.

Отдельно анализируются сценарии с drift и no-drift. Отрицательный или статистически незначимый результат является допустимым результатом исследования.

## Calibration и hold-out

Seeds детерминированно делятся без пересечения:

- первая треть repetitions используется для calibration;
- оставшиеся repetitions используются только для hold-out оценки;
- при минимальных двух repetitions разделение равно `1/1`.

На каждом calibration потоке запускаются `AGGRESSIVE`, `BALANCED` и `CONSERVATIVE`. Для каждой группы `scenario × noise × effect` профиль с максимальной средней utility по calibration seeds становится меткой всех baseline examples группы. Такое агрегирование уменьшает влияние случайной удачи отдельного seed. Ground truth используется для построения меток, но не передаётся selector во время hold-out выбора.

Из начальной четверти потока, расположенной до заданного drift, извлекаются признаки:

- coefficient of variation;
- signed logarithm of baseline mean magnitude;
- logarithm of baseline standard deviation;
- lag-one autocorrelation;
- median absolute deviation, нормированное на median;
- нормированный линейный trend;
- robust outlier rate.

Признаки стандартизуются по calibration выборке. Adaptive strategy выбирает профиль взвешенным методом пяти ближайших соседей. Selector передаётся в публичный `AdaptivePageHinkleyConfig` библиотеки DriftGuard; hold-out поток обрабатывается библиотечным `adaptive-page-hinkley`, а выбранный профиль хранится в detector state. Scenario id, параметры генератора, ожидаемый интервал drift и результаты detector не являются входами selector.

Лучший глобальный фиксированный baseline также выбирается только по средней calibration utility. Именно с ним adaptive сравнивается на hold-out.

## Экспериментальный дизайн

Матрица включает:

- step drift;
- pulse spike;
- throughput drop;
- gradual drift;
- seasonal no-drift;
- множители шума;
- множители величины эффекта;
- random seeds.

На hold-out каждый фиксированный профиль и adaptive получают один и тот же поток. Такое парное сравнение устраняет различия генерации между стратегиями.

## Метрики

Для сценариев с ожидаемым drift:

- `precision = TP / (TP + FP)`;
- `recall = detected intervals / expected intervals`;
- `F1 = 2 * precision * recall / (precision + recall)`;
- false-positive events на 1 000 наблюдений;
- первая задержка обнаружения;
- detection rate.

Для no-drift сценариев `precision`, `recall`, `F1`, detection delay и detection rate не определены и экспортируются как `N/A`. Вместо них используются:

- specificity как доля наблюдений без ложного события;
- false-alarm-free rate как доля потоков без единой ложной тревоги;
- mean time to first false alarm.

Calibration и paired comparison используют:

```text
drift utility =
    F1
    - 0.25 * falsePositiveEvents / samples
    - 0.15 * min(detectionDelay / samples, 1)

no-drift utility =
    specificity
    - 0.25 * falsePositiveEvents / samples
```

Пропущенный drift получает нормированную delay penalty, равную `1`. Коэффициенты utility заданы до hold-out оценки и должны быть обоснованы требованиями предметной области либо проверены sensitivity analysis.

## Статистическая оценка

Для каждой hold-out комбинации вычисляется парная разница:

```text
delta = utility(adaptive) - utility(calibration-selected fixed baseline)
```

Отчёт содержит:

- среднюю парную delta;
- percentile bootstrap 95% confidence interval по 5 000 resamples;
- двусторонний Wilcoxon signed-rank p-value;
- число wins, losses и ties;
- общий результат и результаты по каждому сценарию.

Практическое улучшение нельзя утверждать только по положительной средней delta. Минимальный критерий свидетельства: confidence interval не пересекает ноль и `p < 0.05`. Для одновременных выводов по нескольким сценариям необходима коррекция множественных сравнений, например Holm.

## Воспроизводимость

JSON, CSV и Markdown сохраняют параметры матрицы, calibration/hold-out split, выбранный baseline, trial-level результаты, агрегаты и paired statistics. Одинаковые seed и параметры дают одинаковые результаты, кроме timestamp завершения.

## Угрозы валидности

1. Сценарии синтетические и не полностью моделируют production telemetry.
2. Сравниваются профили одной конфигурации Page-Hinkley, а не независимые семейства алгоритмов.
3. k-NN selector и набор признаков могут переобучиться на ограниченное семейство генераторов.
4. Utility зависит от заранее заданных весов ошибок и задержки.
5. Normal-approximation CI для агрегированного F1 сохранён как описательная метрика; основной вывод должен опираться на paired bootstrap и Wilcoxon.
6. При малом числе hold-out pairs статистическая мощность недостаточна.
7. Для внешней валидности нужны публичные или реальные telemetry datasets и сравнение с опубликованными baselines.

## Рекомендуемый протокол ВКР

Использовать не менее 30 hold-out seeds, зафиксировать матрицу и веса utility до запуска, выполнить sensitivity analysis весов, применить Holm correction для выводов по сценариям и повторить эксперимент на внешнем dataset. В тексте ВКР следует разделять инженерный вклад, экспериментальный результат и подтверждённую научную новизну.
