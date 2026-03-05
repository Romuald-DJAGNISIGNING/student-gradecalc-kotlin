# student-gradecalc-kotlin

Native Android (Jetpack Compose) Student Grade Calculator with local CSV/XLSX processing and styled XLSX export.

## Features

- Offline-only workflow using Android Storage Access Framework.
- Import `.csv` and `.xlsx` files.
- Auto score mode:
  - `CA<=30` and `Exam<=70` -> `CA + Exam`
  - Else if both `<=100` -> weighted `0.30*CA + 0.70*Exam`
  - Else fallback to valid `Total`.
- Grade scale:
  - `A>=85`, `B+>=80`, `B>=75`, `C+>=70`, `C>=65`, `D+>=60`, `D>=55`, else `F`
  - `X` for unknown/invalid rows.
- Duplicate handling: keep latest, log issue.
- Styled workbook export with sheets:
  - `Grades`
  - `Summary`
  - `Issues`
  - `ChartData`
- In-app grade distribution chart using MPAndroidChart.

## Kotlin Lesson Principles Applied

- `val` by default and immutable data models.
- Null-safety-focused processing (`?`, explicit checks, safe defaults).
- Expression-style conditional grading logic.
- Data classes as the main data contract.

## Build & Test

```bash
./gradlew test
./gradlew assembleDebug
```

## Run Console Mode

```bash
./gradlew :console:run --args="--input samples/parity/input_students.csv --output build/exports/grades.xlsx"
```

## Sample Data

See [`samples/parity`](samples/parity) and [`samples/edge_cases.csv`](samples/edge_cases.csv).

## Core Contract

- `StudentInputRow`
- `NormalizedStudent`
- `GradeResult`
- `ValidationIssue`
- `ProcessingReport`
- Services/use-cases: `FileImportService`, `GradingEngine`, `WorkbookExportService`, `ChartDataBuilder`
