# Invoice Builder: Native Android Implementation Plan (Kotlin & Jetpack)

This document establishes the official, production-ready implementation plan for engineering a **100% native Android application** for **Invoice Builder**, migrating from the current TypeScript/Electron/React monorepo to **Kotlin** and **Android Jetpack**.

---

## Table of Contents

1. [Project Overview & Architectural Standards](#1-project-overview--architectural-standards)
2. [Multi-Module Gradle Architecture](#2-multi-module-gradle-architecture)
3. [Complete Database Schema Mapping (SQLite to Room)](#3-complete-database-schema-mapping-sqlite-to-room)
4. [Sprint-by-Sprint Work Breakdown Structure (WBS)](#4-sprint-by-sprint-work-breakdown-structure-wbs)
   - [Epic 1: Project Scaffolding & Core Domain Models](#epic-1-project-scaffolding--core-domain-models)
   - [Epic 2: Room Persistence Layer & Snapshot Architecture](#epic-2-room-persistence-layer--snapshot-architecture)
   - [Epic 3: Precision Financial Calculation Engine](#epic-3-precision-financial-calculation-engine)
   - [Epic 4: Master Data Catalogs (CRUD Repositories & UI)](#epic-4-master-data-catalogs-crud-repositories--ui)
   - [Epic 5: Invoice & Quotation Core Lifecycle](#epic-5-invoice--quotation-core-lifecycle)
   - [Epic 6: Hybrid PDF Engine & E-Invoicing (Factur-X / UBL)](#epic-6-hybrid-pdf-engine--e-invoicing-factur-x--ubl)
   - [Epic 7: Dynamic Layout Engine (Compose AST Renderer)](#epic-7-dynamic-layout-engine-compose-ast-renderer)
   - [Epic 8: Thermal Receipt Printing & Hardware Media](#epic-8-thermal-receipt-printing--hardware-media)
   - [Epic 9: Analytics, Reporting & Vico Visualizations](#epic-9-analytics-reporting--vico-visualizations)
   - [Epic 10: Import/Export, Settings, Localization & Hardening](#epic-10-importexport-settings-localization--hardening)
5. [Screen Inventory & Navigation Flow](#5-screen-inventory--navigation-flow)
6. [Testing, QA, & Verification Strategy](#6-testing-qa--verification-strategy)
7. [CI/CD & Release Pipeline](#7-cicd--release-pipeline)
8. [Risk Register & Contingency Actions](#8-risk-register--contingency-actions)

---

## 1. Project Overview & Architectural Standards

### 1.1 Goals
- Deliver a native Android mobile experience with **100% feature parity** to the Invoice Builder desktop app (v3.2.1).
- Preserve the **offline-first local database architecture** using Android Room.
- Provide full support for European and international e-invoicing standards (**PEPPOL BIS Billing 3.0**, **XRechnung UBL 2.1**, and **Factur-X / ZUGFeRD** hybrid PDF embedding).
- Leverage modern Android capabilities: direct Bluetooth thermal receipt printing, camera receipt scanning, touch signature canvas, biometric protection, and Android Share Sheet.

### 1.2 Target Platform Specifications
- **Language**: Kotlin 2.1+
- **Min SDK**: 26 (Android 8.0 Oreo - covers 97.4%+ active devices globally)
- **Target SDK / Compile SDK**: 35 (Android 15)
- **UI Framework**: Jetpack Compose with Material 3 (Material You adaptive theming)
- **Architecture**: Clean Architecture + MVI/MVVM with Unidirectional Data Flow (UDF)
- **Dependency Injection**: Dagger Hilt
- **Asynchronous Execution**: Kotlin Coroutines & Reactive `Flow`
- **Serialization**: `kotlinx.serialization`

---

## 2. Multi-Module Gradle Architecture

To ensure high build speeds, strict boundary enforcement, and independent testability, the project will follow the modern Android multi-module pattern:

```
invoice-builder-android/
├── app/                           # App entry point, Hilt Application, Root Navigation Graph
├── core/
│   ├── model/                     # Pure Kotlin domain entities, DTOs, Enums, Interfaces
│   ├── common/                    # Coroutine dispatchers, Result monad, Formatters
│   ├── database/                  # Room Database, 17 Entities, DAOs, TypeConverters
│   ├── calculator/                # Financial calculation engine (Cents math, Tax, Surcharge)
│   ├── einvoice/                  # UBL 2.1 & XRechnung XML generator
│   └── pdf/                       # PDFBox-Android rendering & Factur-X file attachment
├── feature/
│   ├── invoices/                  # Invoices list, creation wizard, payments, quotation conversion
│   ├── clients/                   # Client catalog CRUD, search, history
│   ├── businesses/                # Business profiles, logo management, tax IDs
│   ├── items/                     # Items & services catalog, units, categories
│   ├── layouts/                   # Dynamic layout engine (AST parser & visual editor)
│   ├── reports/                   # Financial cards, Vico trend charts, sales breakdown
│   └── settings/                  # Backup/restore, language switcher, numbering formats
└── build-logic/                   # Convention plugins for Compose, Hilt, Room, Library
```

---

## 3. Complete Database Schema Mapping (SQLite to Room)

The existing SQLite schema in `packages/core/src/db/setup.ts` maps directly to Room entities as defined below:

| # | Current SQLite Table | Android Room Entity | Key Constraints & Relations |
|---|---|---|---|
| 1 | `settings` | `SettingsEntity` | Single row (`id = 1`), language, formats, feature toggles. |
| 2 | `businesses` | `BusinessEntity` | Primary key `id`, `logo` (`ByteArray?`), Peppol endpoint, VAT code. |
| 3 | `clients` | `ClientEntity` | Primary key `id`, buyer reference, Peppol endpoint, address. |
| 4 | `units` | `UnitEntity` | Unique `name`, archive flag. |
| 5 | `categories` | `CategoryEntity` | Unique `name`, archive flag. |
| 6 | `currencies` | `CurrencyEntity` | Unique `code`, `symbol`, `subunit` (e.g. 100), `format`. |
| 7 | `items` | `ItemEntity` | FK to `units` and `categories`, `amount` in cents string. |
| 8 | `layouts` | `LayoutEntity` | Schema stored as JSON string (`LayoutSchemaV2`). |
| 9 | `style_profiles` | `StyleProfileEntity` | Colors, fonts, logo size, watermarks (`ByteArray?`). |
| 10 | `banks` | `BankEntity` | Bank details, SWIFT, IBAN, UPI code, QR code (`ByteArray?`). |
| 11 | `presets` | `PresetEntity` | FKs to `business`, `client`, `currency`, `bank`, `styleProfile`. |
| 12 | `invoices` | `InvoiceEntity` | Check constraints, status (`unpaid` to `paid`), sequential number. |
| 13 | `invoice_items` | `InvoiceItemEntity` | FK to `invoices` (`CASCADE`), FK to `items`, quantity, tax rate. |
| 14 | `invoice_payments` | `InvoicePaymentEntity` | FK to `invoices` (`CASCADE`), `amountCents`, `paymentMethod`. |
| 15 | `invoice_sequences` | `InvoiceSequenceEntity` | Unique composite `(businessId, clientId, invoiceType)`. |
| 16 | **Snapshot Tables (8)** | `Invoice*SnapshotEntity` | Immutable historic snapshots created on invoice issuance. |
| 17 | `workspaces` / `sessions` | *Omitted for Local Mobile* | Replaced by direct Room database isolation. |

---

## 4. Sprint-by-Sprint Work Breakdown Structure (WBS)

### Epic 1: Project Scaffolding & Core Domain Models
**Duration**: Sprint 1 (Days 1–5)  
**Objective**: Initialize Gradle build configuration, directory structure, core domain types, and automated translation tooling.

- [ ] **Task 1.1**: Initialize Android project with Gradle Version Catalog (`gradle/libs.versions.toml`).
- [ ] **Task 1.2**: Configure build-logic convention plugins (`android-application`, `android-library`, `compose`, `hilt`).
- [ ] **Task 1.3**: Port all enums from `packages/contracts/src/enums/`:
  - `InvoiceType`, `InvoiceStatus`, `DiscountType`, `TaxType`, `InvoiceTaxOptionType`, `AmountFormat`, `DateFormat`, `Language`, `PageFormat`.
- [ ] **Task 1.4**: Port domain models from `packages/contracts/src/types/`:
  - `Invoice`, `Client`, `Business`, `Item`, `Currency`, `Bank`, `Preset`, `StyleProfile`, `Settings`.
- [ ] **Task 1.5**: Execute `scripts/export-android-strings.js` to populate `res/values*/strings.xml` for `en`, `de`, `fr`, `cs`, `lt`, and `pt`.
- [ ] **Task 1.6**: Implement Material 3 Dark/Light color schemes matching desktop palettes (`#006400` primary green theme).

**Definition of Done (DoD)**: Project compiles cleanly with `:core:model` and `:core:common`; all translations resolve in Android Studio preview.

---

### Epic 2: Room Persistence Layer & Snapshot Architecture
**Duration**: Sprint 2 (Days 6–10)  
**Objective**: Build Room database schema, DAOs, type converters, and verify migration safety.

- [ ] **Task 2.1**: Implement `AppDatabase : RoomDatabase` with 17 entities in `:core:database`.
- [ ] **Task 2.2**: Implement `JsonTypeConverters` using `kotlinx.serialization` for:
  - `LayoutSchemaV2`, `PdfTexts`, `FieldSortOrders`.
- [ ] **Task 2.3**: Build Room DAOs with reactive `Flow` queries:
  - `InvoiceDao`, `ClientDao`, `BusinessDao`, `ItemDao`, `CurrencyDao`, `BankDao`, `PresetDao`, `LayoutDao`, `SettingsDao`.
- [ ] **Task 2.4**: Implement Transactional Snapshot saving in `InvoiceDao`:
  - Atomic insertion of `invoice_business_snapshots`, `invoice_client_snapshots`, `invoice_currency_snapshots`, `invoice_bank_snapshots`, `invoice_item_snapshots`.
- [ ] **Task 2.5**: Write in-memory Room Unit Tests (`Room.inMemoryDatabaseBuilder`) verifying foreign key cascade deletes and unique constraints.

**Definition of Done (DoD)**: 100% DAO test pass rate; database operations execute asynchronously on `Dispatchers.IO`.

---

### Epic 3: Precision Financial Calculation Engine
**Duration**: Sprint 3 (Days 11–15)  
**Objective**: Port financial totals calculation, discount distribution, VAT grouping, and sequence generation.

- [ ] **Task 3.1**: Implement `InvoiceCalculator` in `:core:calculator` matching `packages/core/src/utils/einvoice/invoiceFunctions.ts`.
- [ ] **Task 3.2**: Implement tax calculations:
  - `exclusive`: `tax = (net * rate) / 100`
  - `inclusive`: `net = gross / (1 + rate / 100); tax = gross - net`
  - `deducted`: `tax = -(net * rate) / 100`
- [ ] **Task 3.3**: Implement proportional discount distribution across line items (`grossPortion = (gross / totalGross) * totalDiscount`).
- [ ] **Task 3.4**: Implement VAT Group aggregation for multi-rate tax compliance.
- [ ] **Task 3.5**: Implement `SequenceGeneratorUseCase` matching `packages/core/src/services/invoices.ts` nextSequence algorithm.
- [ ] **Task 3.6**: Port all 40+ unit tests from `packages/core/src/services/__tests__/invoices.spec.ts` to JUnit 5 tests.

**Definition of Done (DoD)**: Calculation engine output matches desktop test fixtures down to the exact cent without rounding discrepancies.

---

### Epic 4: Master Data Catalogs (CRUD Repositories & UI)
**Duration**: Sprint 4 (Days 16–20)  
**Objective**: Implement repositories, ViewModels, and Compose screens for master entities.

- [ ] **Task 4.1**: Build `ClientRepository` & `ClientViewModel` with search, filter by archived, and CRUD operations.
- [ ] **Task 4.2**: Build Compose Screens:
  - `ClientListScreen`: Swipe-to-archive, search bar, avatar badge.
  - `ClientFormScreen`: Input fields, VAT number validation, Peppol Endpoint picker.
- [ ] **Task 4.3**: Build `BusinessListScreen` & `BusinessFormScreen` with logo upload and cropping integration.
- [ ] **Task 4.4**: Build `ItemListScreen` & `ItemFormScreen` with unit and category dropdown selectors.
- [ ] **Task 4.5**: Build `BankListScreen` & `BankFormScreen` with UPI/IBAN fields and QR code image preview.
- [ ] **Task 4.6**: Build `CurrencyListScreen` with subunit configuration and live formatting preview.

**Definition of Done (DoD)**: Users can create, view, edit, search, and archive clients, businesses, items, and banks with real-time Room reactivity.

---

### Epic 5: Invoice & Quotation Core Lifecycle
**Duration**: Sprint 5 & 6 (Days 21–30)  
**Objective**: Build the invoice creation wizard, sequential numbering, line item management, and payment tracking.

- [ ] **Task 5.1**: Build `InvoiceListScreen`:
  - Segmented control for `Invoices` vs `Quotations`.
  - Filter chips (`All`, `Unpaid`, `Open`, `Partially Paid`, `Paid`, `Closed`).
  - Invoice summary card with formatted full invoice number, client name, due date, and payment status badge.
- [ ] **Task 5.2**: Build `InvoiceFormScreen` (Multi-Step Wizard / Tabbed interface):
  - **Step 1: General Info**: Business selector, Client selector, Currency selector, Issue date, Due date, Sequential invoice number.
  - **Step 2: Line Items**: Reorderable items list (`sh.calvin.reorderable`), Quick-add from catalog, custom item entry, quantity and unit price controls.
  - **Step 3: Adjustments**: Discount (Fixed / Percentage), Surcharge, Shipping fee, Global vs Line Tax.
  - **Step 4: Terms & Notes**: Customer notes, thank you notes, bank account selector.
- [ ] **Task 5.3**: Build `PaymentRecordDialog`:
  - Record partial or full payments with date and method (`Cash`, `Bank Transfer`, `Card`, `UPI`).
  - Automatic status progression (`unpaid` ➔ `partially` ➔ `paid`).
- [ ] **Task 5.4**: Implement Quotation to Invoice Conversion:
  - Duplicate quotation data, generate new invoice sequence, preserve `convertedFromQuotationId` link.

**Definition of Done (DoD)**: End-to-end invoice creation, saving, editing, and payment recording fully operational.

---

### Epic 6: Hybrid PDF Engine & E-Invoicing (Factur-X / UBL)
**Duration**: Sprint 7 (Days 31–35)  
**Objective**: Build standards-compliant XML generation, PDF rendering, Factur-X file embedding, and sharing.

- [ ] **Task 6.1**: Implement `UblXmlGenerator` in `:core:einvoice`:
  - Full conformance with **PEPPOL BIS Billing 3.0** and **XRechnung UBL 2.1** XML schemas.
  - Proper XML escaping and Leitweg-ID validation in `BuyerReference`.
- [ ] **Task 6.2**: Implement PDF Document Renderer in `:core:pdf`:
  - Printable template layout using native Android printing (`PrintAttributes`, Canvas drawing or WebView-based print adapter).
- [ ] **Task 6.3**: Implement `HybridPdfAttacher` using `pdfbox-android`:
  - Attach the UBL XML into the PDF document stream (`PDEmbeddedFilesNameTreeNode`).
- [ ] **Task 6.4**: Build `InvoicePreviewScreen`:
  - Display rendered PDF using `PdfRenderer` in a zoomable, scrollable Compose view.
  - Action buttons: `Download PDF`, `Export XML`, `Share Invoice`, `Print Receipt`.
- [ ] **Task 6.5**: Integrate Android Storage Access Framework (`CreateDocument`) and Share Sheet (`Intent.ACTION_SEND`).

**Definition of Done (DoD)**: Generated hybrid PDFs successfully validate against the official European Peppol / KoSIT validation engines.

---

### Epic 7: Dynamic Layout Engine (Compose AST Renderer)
**Duration**: Sprint 8 (Days 36–40)  
**Objective**: Interpret and render custom V1 and V2 layout JSON schemas dynamically in Jetpack Compose.

- [ ] **Task 7.1**: Implement recursive layout AST parser for `LayoutSchemaV2` (`Region`, `ContainerNode`, `BlockNode`, `SectionNode`).
- [ ] **Task 7.2**: Implement Compose Composable `@Composable fun RenderLayoutRegion`:
  - Dynamic column widths (`20%` – `100%`), directions (`column`, `row`, `grid`), gaps (`5dp`, `10dp`).
- [ ] **Task 7.3**: Implement section handlers:
  - `header`, `itemsTable`, `financialTotals`, `paymentInfo`, `notes`, `signature`, `watermark`, `pageCounter`.
- [ ] **Task 7.4**: Build Mobile Visual Layout Builder (`LayoutBuilderScreen`):
  - Visual reordering list allowing users to drag and drop sections, toggle visibility, and adjust color schemes.
  - Save valid `LayoutSchemaV2` JSON directly into Room.

**Definition of Done (DoD)**: Default and custom layouts render consistently between desktop and mobile previews.

---

### Epic 8: Thermal Receipt Printing & Hardware Media
**Duration**: Sprint 9 (Days 41–45)  
**Objective**: Implement hardware-level POS features: Bluetooth thermal receipt printing, touch signature canvas, image cropping, and QR codes.

- [ ] **Task 8.1**: Build ESC/POS Command Generator in `:core:common`:
  - Standard 58mm / 80mm ESC/POS byte sequence generation for invoice header, items, totals, and barcodes.
- [ ] **Task 8.2**: Build `BluetoothPrinterManager`:
  - Bluetooth device discovery, SPP RFCOMM socket connection, and direct raw byte printing.
- [ ] **Task 8.3**: Build `SignaturePadComposable`:
  - Compose `Canvas` capturing gesture paths, with clear, undo, and export to PNG `ByteArray`.
- [ ] **Task 8.4**: Integrate `CanHub/Android-Image-Cropper` for business logo uploads.
- [ ] **Task 8.5**: Integrate ZXing QR Code generator for bank UPI / Swiss QR bills.

**Definition of Done (DoD)**: Instant Bluetooth receipt printing tested and verified on thermal test printer; touch signatures persist in Room.

---

### Epic 9: Analytics, Reporting & Vico Visualizations
**Duration**: Sprint 10 (Days 46–50)  
**Objective**: Build executive financial reports, charts, and business metrics.

- [ ] **Task 9.1**: Implement `ReportAggregationDao`:
  - SQLite aggregate queries for Total Revenue, Total Receivables, Paid Invoices, Open Invoices, and Average Invoice Value.
- [ ] **Task 9.2**: Integrate **Vico** charting library in `:feature:reports`:
  - `RevenueTrendChart`: Monthly / Quarterly revenue line chart with interactive scrubbing.
  - `ClientRevenueBarChart`: Horizontal bar chart of top clients by revenue.
  - `CategorySalesPieChart`: Breakdown of item sales by category.
- [ ] **Task 9.3**: Implement date range filters (`This Month`, `Last Quarter`, `This Year`, `Custom Date Range`).

**Definition of Done (DoD)**: Reports screen provides real-time business financial metrics with fluid Compose animations.

---

### Epic 10: Import/Export, Settings, Localization & Hardening
**Duration**: Sprint 11 & 12 (Days 51–60)  
**Objective**: Data portability, JSON database backup/restore, Excel import/export, biometrics, security, and release preparation.

- [ ] **Task 10.1**: Implement JSON Database Backup & Restore:
  - Export all 17 tables to a single `.json` file matching desktop backup format.
  - Transactional restore with schema validation.
- [ ] **Task 10.2**: Implement Excel Import & Export via Apache POI Android:
  - Export clients, items, and invoices to `.xlsx` spreadsheets.
- [ ] **Task 10.3**: Build `SettingsScreen`:
  - Language picker, Amount & Date format selectors, Dark/Light mode toggle, Invoice number prefix/suffix customization.
- [ ] **Task 10.4**: Implement Biometric Authentication (`androidx.biometric:biometric`):
  - Optional app lock requiring Fingerprint or Face Unlock upon launching.
- [ ] **Task 10.5**: Configure ProGuard / R8 optimization rules for Room, PDFBox, and Kotlinx Serialization.
- [ ] **Task 10.6**: Build Release APK and Signed Android App Bundle (AAB).

**Definition of Done (DoD)**: App passes all lint checks, unit tests, and security scans; release AAB ready for Google Play distribution.

---

## 5. Screen Inventory & Navigation Flow

```
[ Splash / Biometric Auth ]
             │
             ▼
[ Main Navigation Shell (Bottom Navigation Bar / Drawer) ]
  ├── 1. Invoices & Quotes Tab
  │      ├── Invoice List (Tabs: Invoices | Quotations; Status Filter Chips)
  │      ├── Invoice Creation / Edit Wizard (Step 1 -> Step 2 -> Step 3)
  │      ├── Invoice Detail & Payment Recording Dialog
  │      └── Full-screen PDF Preview & Share Sheet
  ├── 2. Catalogs Tab
  │      ├── Clients List & Form Screen
  │      ├── Items & Services List & Form Screen
  │      ├── Businesses List & Form Screen
  │      └── Banks & Currencies Screen
  ├── 3. Reports Tab
  │      ├── Financial Overview Summary Cards
  │      ├── Vico Revenue Trend & Client Performance Charts
  │      └── Date Range Filter BottomSheet
  └── 4. Settings Tab
         ├── General (Language, Date/Amount formats, Dark mode)
         ├── Invoice Numbering (Prefix, Suffix, Sequence reset)
         ├── Layout Builder & Style Profiles
         └── Data Management (JSON Backup/Restore, Excel Export)
```

---

## 6. Testing, QA, & Verification Strategy

| Layer | Testing Framework | Coverage Target | Key Verification Focus |
|---|---|---|---|
| **Domain & Calculator** | JUnit 5 + Truth | **95%+** | Precision cents math, multi-tier tax, discount splitting, VAT groups. |
| **Persistence (Room)** | AndroidX Test + In-Memory Room | **90%+** | Cascade delete safety, foreign keys, unique sequence generation. |
| **E-Invoicing XML** | JUnit 5 + XMLUnit | **100%** | Byte-for-byte schema validation against PEPPOL BIS 3.0 rules. |
| **ViewModels & Flow** | Kotlinx Coroutines Test + Turbine | **85%+** | StateFlow emissions, UI state transitions, error handling. |
| **Compose UI** | AndroidX Compose Test Rule | **75%+** | Form input validation, wizard step progression, dialog actions. |
| **End-to-End (E2E)** | Maestro Mobile E2E | Core Flows | Create client ➔ Create item ➔ Generate invoice ➔ Share PDF. |

---

## 7. CI/CD & Release Pipeline

A GitHub Actions workflow (`.github/workflows/android.yml`) will automate build verification:

```yaml
name: Android CI/CD

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Run Spotless & Lint
        run: ./gradlew spotlessCheck lintDebug
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/reports/tests/'
```

---

## 8. Risk Register & Contingency Actions

| Risk | Severity | Probability | Contingency Action |
|---|---|---|---|
| **1. PDFBox-Android Memory Footprint** | Medium | Medium | Recycle `PDDocument` instances immediately in `try-finally` blocks; process PDFs off the main thread. |
| **2. Complex Layout Rendering Differences** | Medium | Low | Maintain the exact default layout parameters as fallback; provide live side-by-side Compose preview. |
| **3. Large Local Database Backups** | Low | Low | Stream JSON backup directly to disk via `JsonWriter` instead of holding large strings in memory. |
| **4. Bluetooth Printer Incompatibility** | Low | Medium | Provide standard Android Print Spooler fallback alongside direct ESC/POS byte printing. |
| **5. Migration from Existing Desktop DB** | Low | Low | Built-in SQLite importer allowing users to transfer `.db` files from desktop to phone via SAF. |

---

## 9. Conclusion

This implementation plan provides a clear, sprint-by-sprint roadmap to build a top-tier native Android version of **Invoice Builder**. By combining the proven offline-first data model of the current application with Android's native strengths (Jetpack Compose, Room, Bluetooth printing, and hardware media integration), the resulting app will be fast, reliable, and perfectly tailored for mobile commerce.
