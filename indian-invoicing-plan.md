# Indian Invoicing Features — Implementation Plan

**Project:** Invoice Builder Mobile (Android, Kotlin, Jetpack Compose, multi-module Gradle)
**Repository:** `https://github.com/vipulverulkar/Invoice_Builder.git`
**App version:** 3.2.1 · **Package:** `com.invoicebuilder.mobile`
**Document status:** Approved for execution · **Last updated:** 2026-11-11

---

## Table of Contents

1. [Purpose & Scope](#1-purpose--scope)
2. [Scope Decisions & Non-Goals](#2-scope-decisions--non-goals)
3. [Current-State Gap Analysis](#3-current-state-gap-analysis)
4. [Indian Compliance Reference](#4-indian-compliance-reference)
5. [Target Architecture](#5-target-architecture)
6. [Data Model Changes](#6-data-model-changes)
7. [GST Tax Engine Specification](#7-gst-tax-engine-specification)
8. [Milestone Roadmap](#8-milestone-roadmap)
9. [Testing & Verification Strategy](#9-testing--verification-strategy)
10. [Risk Register](#10-risk-register)
11. [Effort Summary & Sequencing](#11-effort-summary--sequencing)
12. [Appendix A — File Change Index](#appendix-a--file-change-index)
13. [Appendix B — GST Reference Data](#appendix-b--gst-reference-data)

---

## 1. Purpose & Scope

Transform Invoice Builder Mobile from a European-VAT-oriented invoicing app into an **India-first GST invoicing application** while preserving its offline-first, multi-module architecture and Compose UI stack.

### 1.1 In Scope

| Area | Capability |
|---|---|
| GST master data | GSTIN, PAN, HSN/SAC, GST slab rates, unit of measure, place of supply (state code), bank + UPI details |
| GST tax engine | CGST/SGST (intra-state) vs IGST (inter-state), per-line rates, inclusive/exclusive tax, cess, discount allocation, round-off, TDS/TCS, amount in words |
| GST invoicing | Tax Invoice / Retail / Export invoice kinds, FY-aware invoice numbering, reverse charge, GST-compliant PDF, GST thermal receipt |
| E-invoicing | IRN capture (manual + optional GSTN API), IRN QR embedding, e-invoice JSON generation for portal upload |
| E-way bill | Local e-way bill JSON generation + share, optional API integration |
| Returns | GSTR-1 and GSTR-3B offline file preparation, HSN summary, reconciliation against imported portal data |
| Reporting | GSTR-1 style dashboards: B2B/B2C/credit notes, HSN-wise summary, tax collected by rate, state-wise breakup |
| Master data UI | New Business Profile and Bank Accounts screens; extended Client and Item editors |

### 1.2 Out of Scope (explicit)

- Direct GSTN portal login / CAPTCHA automation.
- Payment gateway integration (UPI intent links are generated as text, but no payment processing).
- Inventory, accounting, payroll, and purchase-order modules.
- Multi-country VAT (PEPPOL / XRechnung) — removed per scope decision.

---

## 2. Scope Decisions & Non-Goals

| Decision | Choice | Consequence |
|---|---|---|
| Compliance depth | Offline GST + live GSTN API + GSTR file preparation | API work is additive; offline path remains the primary UX |
| Existing EU stack | **Removed** (India-only) | `core:einvoice`, `FacturXAttacher`, PEPPOL/XRechnung strings deleted in M0 |
| First milestone | Foundation + tax engine + end-to-end GST invoice + reports | M1 is broad; M2–M4 deepen output surfaces and integrations |
| Tax model | Single GST model replacing the single-VAT-rate model | One tax model in the codebase at all times — no parallel EU/IN engines |

**Non-goals for this programme:** rewriting the Compose UI layer, replacing Room, migrating to KMP, or changing the app's offline-first guarantee.

---

## 3. Current-State Gap Analysis

Verified against the working tree (branch `master`, commit `2c6b1db`).

### 3.1 Persistence & Schema

| Gap | Location | Impact |
|---|---|---|
| DB is `version = 1` with `fallbackToDestructiveMigration()` | `core/database/.../AppDatabase.kt`, `app/.../di/DatabaseModule.kt:27` | Any new column wipes all user data on upgrade — **blocks every schema change** |
| No `Migration` classes exist | `core/database/src/main/java/.../database/` | Upgrade path must be authored from scratch |
| `Money` is `Double` cents throughout | `core/model/.../Models.kt` (`CalculatedInvoice`, `VatGroup`), `core/calculator/.../InvoiceCalculator.kt` | Float drift in totals; `Double` for currency is a correctness risk at ₹ crore scale |

### 3.2 Tax & Compliance

| Gap | Location | Impact |
|---|---|---|
| One global tax rate per invoice | `InvoiceEntity.taxRate` / `taxType` / `taxName` | Cannot represent a mixed-rate GST invoice (5% + 18% lines) |
| No CGST/SGST/IGST split | `core/calculator/.../InvoiceCalculator.kt` → `VatGroup(rate, taxable, tax)` | No place-of-supply logic, no intra/inter-state tax split |
| No HSN/SAC | `ItemEntity` has no `hsnCode` | HSN-wise reporting and e-invoice impossible |
| No GSTIN on business/client | `BusinessEntity.vatCode`, `ClientEntity.vatCode` (generic VAT field) | GSTIN validation & B2B detection impossible |
| No place of supply | absent | Cannot decide IGST vs CGST+SGST |
| No TDS/TCS | absent | Section 194C/194Q deductions unsupported |
| No round-off | `CalculatedInvoice` has no round-off field | Indian invoices require round-off to nearest rupee |

### 3.3 Documents & Output

| Gap | Location | Impact |
|---|---|---|
| PDF is a fixed single-page canvas | `core/pdf/.../PdfGenerator.kt` (237 lines, hardcoded coordinates) | No HSN table, GST breakup, amount in words, UPI QR, signature, IRN, multi-page support |
| PDF still says "VAT:" | `PdfGenerator.kt:94,120` | Wrong labels for India |
| Thermal receipt lacks GST fields | `core/common/.../EscPosBuilder.kt` | No GSTIN, no HSN summary, no tax split |
| QR capability absent | `zxing-core` defined in `gradle/libs.versions.toml` but not a dependency of any module | UPI QR and IRN QR cannot be produced |
| EU e-invoice only | `core/einvoice/.../UblXmlGenerator.kt` (PEPPOL BIS 3.0 / XRechnung) | Replace with GST e-invoice JSON / IRN flow |
| Factur-X hybrid PDF | `core/pdf/.../FacturXAttacher.kt` | EU-only, remove |

### 3.4 Application Features

| Gap | Location | Impact |
|---|---|---|
| Reports use fabricated amounts | `app/.../ui/viewmodel/ReportsViewModel.kt` — adds `10000.0` per invoice | Revenue/collection figures are meaningless |
| Invoice preview unreachable | `InvoicePreviewScreen` never invoked; `onInvoiceClick` unused | No way to share the generated PDF/XML from the list |
| No Business/Bank screens | `app/src/main/java/.../ui/screens/` has only clients, invoices, items, reports, settings | GSTIN, state, IFSC, UPI cannot be captured |
| Line items lack price snapshots | `InvoiceItemEntity` = `itemId`, `quantity`, `taxRate`, `taxType` | Editing/deleting a catalogue item silently rewrites historical invoices |
| `SignaturePad` unused | `app/.../ui/components/SignaturePad.kt` | No signature on documents |
| `LayoutSchema` unused | `core/model/.../LayoutSchema.kt` (region/section AST) | The renderer that should host GST sections is already modelled but unbuilt |
| `core:common` not depended on by `:app` | `app/build.gradle.kts` | Shared formatters/validators unavailable to UI |

---

## 4. Indian Compliance Reference

### 4.1 GST Structure

- **Intra-state supply** (supplier state == place of supply): `CGST = SGST = taxable × rate ÷ 2`.
- **Inter-state supply**: `IGST = taxable × rate` (full rate).
- **Place of supply** is derived from the supplier's state code vs the buyer's state code; defaults to the supplier's state when the buyer has none (B2C/over-the-counter).
- **Zero-rated / exempt** lines carry rate `0` and are reported separately, not as taxed lines.
- **Cess** (e.g. 1% on luxury goods) is applied *on top of* GST and reported in its own column.
- **Reverse charge (RCM)**: buyer pays GST; seller reports it in the GSTR-1 `RCM` liability table.

### 4.2 Reference Data

| Item | Value |
|---|---|
| GST slabs (goods, typical) | 0%, 0.25% (rough diamonds), 3%, 5%, 12%, 18%, 28% |
| Service slabs (typical) | 5%, 12%, 18% |
| Special rates | 6%, 7.5% (gold/jewellery), 1.5% ( cess on top of 12%/7.5% for gems) |
| GSTIN format | `15` chars: 2 state code · 10 PAN · 1 entity code (`1`=person, `2`=firm…) · 1 `Z` · 1 checksum (mod-36) |
| PAN format | `[A-Z]{5}[0-9]{4}[A-Z]` |
| HSN/SAC format | 4, 6 or 8 digits (SAC for services) |
| IFSC format | 4 letters (`ZERO` for RBI) + `0` + 6 alphanumeric |
| UPI ID format | `handle@bank`, handle `[a-zA-Z0-9._-]{2,256}` |
| State codes | 01–36 (see Appendix B) |

### 4.3 Invoice Numbering

A GST-compliant series typically embeds the financial year, e.g. `INV/25-26/0001`, and is unique per (business, FY, invoice type). `InvoiceSequenceEntity` already keys on `(businessId, clientId, invoiceType)` and must be extended with a financial-year component.

### 4.4 Document Requirements (Tax Invoice)

Supplier GSTIN and legal name · recipient GSTIN (if B2B) · invoice number and date · place of supply · HSN/SAC per line · taxable value per rate · CGST/SGST/IGST breakup · total tax · total invoice value in words · amount in figures · bank details with UPI/QR · supplier signature · IRN/QR for e-invoiced documents · supplier declaration.

---

## 5. Target Architecture

### 5.1 Module Layout

```
invoice_builder_mobile/
├── app/                    # UI, navigation, Hilt graph, seeding
├── core/
│   ├── model/              # Domain models, enums, Money, CurrencyFormat, validators contracts
│   ├── common/             # Validators, amount-in-words, UPI intent, date formatting  (now used by :app)
│   ├── calculator/         # GST tax engine (pure Kotlin, heavily unit tested)
│   ├── database/           # Room v2 + explicit Migrations, repositories
│   ├── pdf/                # Section-based GST Tax Invoice renderer
│   ├── gstn/               # NEW: GSTN e-invoice + e-way bill client (OAuth, IRN, EWB)
│   └── receipts/           # NEW (or extend core:common): thermal receipt composer
```

### 5.2 Key Design Decisions

1. **One tax model.** `VatGroup` is replaced by `GstTaxGroup`. The EU XML generator is deleted rather than left dormant, so the calculator has a single contract.
2. **`Money` value class.** `Money(amountPaise: Long)` with operator arithmetic and `format()`; `CalculatedInvoice` holds `Money`. `Double` cents are removed from the money path (`Double` remains only as a transient parse format for legacy string columns during migration).
3. **Snapshot integrity.** Every invoice line stores `name`, `unitPrice`, `hsnCode`, `gstRate`, `taxType`, `discount` at save time. Catalogue edits can never rewrite issued documents.
4. **Section-based rendering.** The PDF renderer is rebuilt on the existing `LayoutSchema` AST (`LayoutSectionType` already models `itemsTable`, `financialTotals`, `paymentInfo`, `notes`, `signature`), so GST sections become layout nodes rather than hardcoded coordinates.
5. **Offline-first with an optional network edge.** GSTN calls live behind an interface (`EInvoiceGateway`) with two implementations: `ManualIrnGateway` (default, no network) and `GstnApiGateway` (opt-in, sandbox/production credentials). The app never blocks invoice creation on the network.
6. **Deterministic tax.** All tax computation is pure and in `core:calculator`, covered by exhaustive unit tests; no Android dependencies.

---

## 6. Data Model Changes

### 6.1 New & Modified Entities (Room v2)

**`BusinessEntity`** (+ columns)
```kotlin
val gstin: String? = null                 // 15-char GSTIN
val pan: String? = null
val stateCode: String? = null              // GST state code, e.g. "27"
val registrationType: String? = null       // regular | composition | sez | ut | oidc
val declaration: String? = null            // supplier declaration text
```

**`ClientEntity`** (+ columns)
```kotlin
val gstin: String? = null
val tradeName: String? = null              // legal/trade name as printed on the invoice
val stateCode: String? = null              // place of supply default
val tdsSection: String? = null             // e.g. "194C"
val tdsRate: Double = 0.0
val tcsApplicable: Boolean = false
```

**`ItemEntity`** (+ columns)
```kotlin
val hsnCode: String? = null                // HSN/SAC
val gstRate: Double = 0.0
val unitCode: String? = null               // NOS | PCS | KGS | MTR | HRS | LTR | …
val itemType: String? = null               // goods | service
```

**`BankEntity`** (+ columns)
```kotlin
val ifscCode: String? = null
val upiId: String? = null
```

**`InvoiceEntity`** (+ columns)
```kotlin
val invoiceKind: String? = null            // tax_invoice | retail | export | proforma
val placeOfSupply: String? = null          // state code
val reverseCharge: Boolean = false
val roundOffCents: Long = 0
val amountInWords: String? = null
val tdsSection: String? = null
val tdsAmountCents: Long = 0
val tcsSection: String? = null
val tcsAmountCents: Long = 0
val irn: String? = null
val ackNumber: String? = null
val ackDate: String? = null
val ewbNumber: String? = null
val ewbDate: String? = null
```

**`InvoiceItemEntity`** (+ snapshot columns — fixes historical drift)
```kotlin
val itemName: String? = null
val unitPriceCents: String = "0"
val hsnCode: String? = null
val gstRate: Double = 0.0
val discountType: String? = null
val discountAmountCents: String = "0"
val unitCode: String? = null
val lineTotalCents: String = "0"
```

**`InvoiceItemSnapshotEntity`** — mirror the same snapshot fields so PDF/QRCs render history, not live catalogue data.

**`SettingsEntity`** (+ columns)
```kotlin
val financialYear: String? = null          // "2025-26"
val defaultStateCode: String? = null
val gstRegistrationOn: Boolean = false
val roundOffEnabled: Boolean = true
val defaultInvoiceKind: String? = null
val gstnClientId: String? = null           // optional API credentials (encrypted at rest)
val gstnClientSecret: String? = null
val gstnGstin: String? = null
val gstnEnvironment: String? = null        // sandbox | production
```

### 6.2 New Enums (core/model)

```kotlin
enum class InvoiceKind { TaxInvoice, Retail, Export, Proforma }
enum class GstSupplyType { IntraState, InterState, ZeroRated, Exempt, DeemedExport }
enum class GstRegistrationType { Regular, Composition,sez, Ut, Oidc }
enum class UnitOfMeasure { NOS, PCS, KGS, MTR, HRS, LTR, SQF, CMT }
enum class ItemType { Goods, Service }
```

### 6.3 Migration

`AppDatabase` moves to `version = 2` with an explicit `Migration(1, 2)` that:
1. `ALTER TABLE` each entity to add the new columns (SQLite `ADD COLUMN` with defaults).
2. Backfills `currencies` with INR if empty and `businesses` with a placeholder business if empty (the seeding already added in `InvoiceBuilderApp.kt` becomes a migration-level guarantee so upgrades never land on an empty database).
3. Backfills `settings.amountFormat` to `en-IN`.

`fallbackToDestructiveMigration()` is removed from `DatabaseModule.kt` — a failed migration must crash loudly in development, not delete a user's ledger.

### 6.4 Domain Model Additions (core/model)

```kotlin
@JvmInline
value class Money(val paise: Long) {
    operator fun plus(other: Money) = Money(paise + other.paise)
    operator fun minus(other: Money) = Money(paise - other.paise)
    fun percent(rate: Double): Money = Money((paise * rate / 100.0).roundToLong())
    fun format(): String = CurrencyFormat.formatAmount(paise / 100.0)
}

data class GstTaxGroup(
    val rate: Double,
    val supplyType: GstSupplyType,
    val taxable: Money,
    val cgst: Money,
    val sgst: Money,
    val igst: Money,
    val cess: Money
)

data class GstInvoiceTotals(
    val lines: List<CalculatedLine>,
    val subtotalNet: Money,
    val discountTotal: Money,
    val shippingTotal: Money,
    val surchargeTotal: Money,
    val taxTotal: Money,
    val cessTotal: Money,
    val tdsTotal: Money,
    val tcsTotal: Money,
    val roundOff: Money,
    val payableTotal: Money,
    val gstGroups: List<GstTaxGroup>,
    val amountInWords: String
)
```

---

## 7. GST Tax Engine Specification

### 7.1 Inputs

```kotlin
data class GstLineInput(
    val itemId: Long,
    val name: String,
    val hsnCode: String?,
    val quantity: Double,
    val unitPricePaise: Long,
    val discountType: DiscountType?,
    val discountAmountPaise: Long,
    val gstRate: Double,
    val taxType: TaxType?,              // exclusive | inclusive | deducted
    val cessRate: Double = 0.0,
    val supplyType: GstSupplyType,
    val isReverseCharge: Boolean = false
)

data class GstInvoiceInput(
    val lines: List<GstLineInput>,
    val supplierStateCode: String,
    val placeOfSupply: String,
    val discountType: DiscountType?,
    val discountAmountPaise: Long,
    val discountPercent: Double,
    val shippingPaise: Long,
    val shippingTaxable: Boolean = false,
    val surchargeType: DiscountType?,
    val surchargeAmountPaise: Long,
    val surchargePercent: Double,
    val tdsSection: String?,
    val tdsRate: Double = 0.0,
    val tcsSection: String?,
    val tcsRate: Double = 0.0,
    val roundOffEnabled: Boolean = true
)
```

### 7.2 Algorithm

1. **Line gross** = `unitPricePaise × quantity`.
2. **Line discount** — invoice-level discount is allocated pro-rata by gross value; line-level discounts apply first. Both are *pre-tax* (value reduction) unless the business opts for post-tax discount.
3. **Taxable value**:
   - `exclusive` → `postDiscount`
   - `inclusive` → `postDiscount ÷ (1 + rate/100)`
   - `deducted` → `postDiscount`, tax computed negatively
4. **Supply type resolution** — `placeOfSupply == supplierStateCode` ⇒ `IntraState` ⇒ `CGST = SGST = taxable × rate ÷ 2`; otherwise `InterState` ⇒ `IGST = taxable × rate`.
5. **Cess** — `cess = (taxable + gst) × cessRate ÷ 100` (levied on the tax-inclusive value, per Section 8 of the CGST Act).
6. **Shipping** — added to the taxable base when `shippingTaxable`, taxed at the invoice's default slab; otherwise shown as a non-taxed line.
7. **Grouping** — lines grouped by `(rate, supplyType)` into `GstTaxGroup`, preserving taxable/CGST/SGST/IGST/cess per group for the PDF breakup table and HSN summary.
8. **TDS/TCS** — computed on the post-tax invoice value (`194C` = 1%/2%; `194Q` = 0.1% on collections above ₹50 lakh), deducted from the payable amount, never added to tax.
9. **Round-off** — `payableTotal` rounded to the nearest rupee; the delta stored as `roundOff` and printed as "Round Off: ₹0.00" on the invoice.
10. **Amount in words** — Indian numbering (`₹1,23,456.78` → "Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six and Seventy Eight Paise Only"), produced by `core/common/AmountInWords.kt`.

### 7.3 API Surface

```kotlin
object GstTaxEngine {
    fun calculate(input: GstInvoiceInput): GstInvoiceTotals
    fun resolveSupplyType(supplierStateCode: String, placeOfSupply: String): GstSupplyType
    fun validateHsn(hsn: String?): Boolean
}
```

`InvoiceCalculator` is retained as a thin compatibility wrapper for the EU-free path and its existing tests are rewritten against `GstTaxEngine` in `core/calculator/src/test/.../GstTaxEngineTest.kt`.

---

## 8. Milestone Roadmap

### M0 — Foundations (3 days) — ✅ **DONE** (`9139031`)

**Goal:** make schema changes safe and remove the EU tax model.

| # | Task | Files | Status |
|---|------|-------|--------|
| 0.1 | Author `Migration(1, 2)` adding all new columns + backfills | `core/database/.../AppDatabase.kt` | ✅ `@AutoMigration(from = 1, to = 2)` + committed `schemas/*.json` (63 generated `ALTER TABLE` statements) |
| 0.2 | Remove `fallbackToDestructiveMigration()` | `app/.../di/DatabaseModule.kt` | ✅ |
| 0.3 | Introduce `Money` value class; migrate `CalculatedInvoice`/`VatGroup` → `GstInvoiceTotals`/`GstTaxGroup` | `core/model/.../Money.kt`, `GstModels.kt`, `core/calculator/.../InvoiceCalculator.kt` | ✅ CGST/SGST vs IGST split, round-off, amount in words |
| 0.4 | Delete `core:einvoice` module, `FacturXAttacher`, `EInvoice` enum usage, PEPPOL/XRechnung strings | module removed, 55 EU strings pruned per locale | ✅ `InvoicePreviewScreen` rewritten for GST PDF + e-invoice JSON |
| 0.5 | Add `core:gstn` module skeleton + wire `core:common` into `:app` | `core/gstn/**` | ✅ gateway interface, `GstEInvoiceJsonBuilder`, manual gateway |
| 0.6 | Extend `gradle/libs.versions.toml` with `zxing`, `security-crypto`, `okhttp` | `gradle/libs.versions.toml` | ✅ |

**Verified on device (SM-M536B):** v1 → v2 migration ran with no data loss (seeded INR currency + business preserved), invoice creation works end-to-end, ₹ formatting and amount-in-words correct, 0 crashes.

**Bugs found and fixed during M0 verification:**
| Bug | Fix |
|---|---|
| Second invoice for the same client crashed the app (`SQLiteConstraintException` — invoice number never incremented because the sequence was only reserved on manual client/business selection) | Reserve the number when the catalogues load (`reserveNextNumber()`), plus crash-safe `saveInvoice` that surfaces errors instead of throwing |
| Invoice lines stored no price/name snapshot, so catalogue edits rewrote issued invoices | Line-item snapshots (`itemName`, `unitPriceCents`, `hsnCode`, `gstRate`, `lineTotalCents`) written to `invoice_items` and `invoice_item_snapshots` |
| Zero-rated/exempt supplies were still taxed; per-line rates were ignored | Supply-type aware tax resolution, nullable per-line rate (`null` = inherit invoice rate, `0.0` = zero-rated) |

**Deviations from the original plan:**
1. The `ublON` / `xrechnungON` columns are **retained** in the `settings` table (unused) instead of being dropped — Room's Kotlin auto-migration spec annotations (`@DeleteColumn`) were not detected by the KSP processor, and keeping the columns avoids a table rebuild on upgrade.
2. `GstTaxEngine` is deferred to M1.2 as planned; M0 delivers the `Money`/`GstTaxGroup`/`GstInvoiceTotals` domain model and the CGST/SGST/IGST split inside `InvoiceCalculator`, which M1.2 will promote into `GstTaxEngine` with per-line discounts, cess, TDS/TCS and place-of-supply resolution.

---

### M1 — Master Data, Validators, Tax Engine, End-to-End GST Invoice, Reports (12–14 days)

**Goal:** a user can register a GSTIN business, add an HSN-coded item, create a mixed-rate intra-state tax invoice, and see correct GST reports.

#### M1.1 Validators (`core/common`, new `validation/` package)
`GstinValidator` (length + regex + mod-36 checksum), `PanValidator`, `HsnValidator` (4/6/8 digits), `IfscValidator`, `UpiValidator`, `StateCodeValidator` (01–36), `InvoiceNumberValidator`. Each returns a sealed `ValidationResult` so the UI can show inline errors.

#### M1.2 Tax engine (`core/calculator`)
Implement `GstTaxEngine` per §7, with exhaustive unit tests:
- exclusive / inclusive / deducted tax
- intra-state CGST+SGST vs inter-state IGST, including mixed supply types on one invoice
- pro-rata discount allocation (including the largest-remainder method so the parts sum exactly to the total)
- cess on top of GST, zero-rated and exempt lines
- shipping taxable vs non-taxable
- TDS and TCS deduction
- round-off to the nearest rupee, both directions
- amount-in-words generation

#### M1.3 Master data UI
- New `BusinessProfileScreen` — legal name, GSTIN (validated, live), PAN, state code picker, registration type, logo, declaration text.
- New `BankAccountScreen` — account holder, number, IFSC, UPI ID, QR image (existing `qrCode` blob).
- Extend `ModernClientFormDialog` — GSTIN, trade name, state code, TDS section/rate.
- Extend `ModernItemFormDialog` — HSN/SAC, GST rate dropdown (slab table), unit code, goods/service toggle.
- New `app/.../ui/components/` helpers: `GstinField`, `HsnField`, `StateCodePicker`, `GstRateDropdown`, `AmountInWordsText`.

#### M1.4 Invoice form rebuild (`InvoiceFormScreen.kt`, `InvoiceFormViewModel.kt`)
- **General** — invoice kind, place of supply picker (pre-filled from client state), supplier GSTIN display, reverse-charge switch, bank selection, FY-aware invoice number (read-only, from `InvoiceSequenceEntity`).
- **Items** — per line: quantity, unit price, GST rate, HSN (auto-filled, editable), discount; live line total incl. tax; "Add Item" pulls from the catalogue.
- **Totals** — GST breakup table (rate → taxable, CGST, SGST, IGST, cess), round-off, TDS/TCS, grand total, amount in words.
- **Notes** — supplier declaration, terms, thanks note, bank details preview, signature pad (wire `SignaturePad.kt`).
- Save path writes the new invoice columns and per-line snapshots; `InvoiceRepository.createInvoice` extended accordingly.

#### M1.5 Reports
- Replace the fabricated `10000.0` totals in `ReportsViewModel.kt` with real values computed from stored invoice snapshots (requires storing the computed payable total on the invoice row — add `totalCents` to `InvoiceEntity` as part of the M0 migration).
- GSTR-1 style summary: B2B vs B2C counts/values, credit/debit notes, HSN-wise summary, tax collected by rate, state-wise breakup.
- Dashboard cards and Vico charts (`vico` is already a dependency but unused).

**Acceptance criteria**
- A GST invoice with 5% and 18% lines, intra-state, prints correct CGST/SGST; switching the buyer's state switches it to IGST.
- Discount + round-off reconcile to the rupee exactly (asserted in tests).
- Reports match the invoices that produced them (property test over a seeded dataset).

---

### M2 — GST Tax Invoice PDF + Thermal Receipt (4–5 days)

**Goal:** a shareable, GST-compliant Tax Invoice PDF.

- Rebuild `core/pdf/.../PdfGenerator.kt` as a section-based renderer driven by `LayoutSchema` (`LayoutSectionType`), with a dedicated `GstInvoiceRenderer`.
- Document sections: header (title "Tax Invoice", GSTIN, invoice no., date, place of supply), supplier block, recipient block, item table (**with HSN/SAC and per-line GST rate columns**), GST breakup table, totals block (taxable, CGST, SGST, IGST, cess, round-off, TDS/TCS, grand total), **amount in words**, bank block with **UPI QR (zxing)** and IFSC, IRN + IRN QR block, declaration, signature, footer with page x of y.
- Date format `dd/MM/yyyy`; amounts via `CurrencyFormat` (₹ with Indian grouping).
- `core/common/.../EscPosBuilder.kt`: add GSTIN header line, HSN summary, CGST/SGST/IGST split, UPI handle, IRN.
- Wire `InvoicePreviewScreen` into the invoice list (`onInvoiceClick` is currently unused) with in-app PDF preview, Share PDF / Share XML actions, and the IRN capture dialog.

**Acceptance criteria**
- PDF renders every GST-mandated field; golden-file test asserts the extracted text contains GSTIN, HSN, each tax head, and the amount in words.
- 58 mm and 80 mm thermal output stays within 32/48 columns.

---

### M3 — GSTN API: E-Invoice (IRN) & E-Way Bill (6–8 days, requires sandbox credentials)

**Goal:** optional live IRN and e-way bill generation, with the manual path always available.

- New `core/gstn` module: OAuth 2.0 client-credentials token manager (with refresh), `generateIrn`, `cancelIrn`, `getIrnStatus`, `generateEwb`, `extendEwb`, `cancelEwb`.
- Gateway interface in `core/model`; `ManualIrnGateway` (default: manual IRN/Ack capture) and `GstnApiGateway` (opt-in).
- Credentials stored with `androidx.security:security-crypto` (Keystore-backed); environment switch sandbox/production.
- IRN QR payload `IRN<32-hex>|SHA256|<hash>` rendered with zxing and embedded in the PDF (M2 renderer).
- Offline behaviour: queue failed submissions, surface "Pending sync" on the invoice card, retry when connectivity returns (`ConnectivityManager` observer).
- E-way bill: local JSON generation for manual portal upload + optional API generation; vehicle/transporter details captured on the invoice.

**Acceptance criteria**
- With no credentials and no network, the app behaves exactly as before (manual IRN entry only).
- With sandbox credentials, IRN is fetched, stored on the invoice, and rendered as a QR in the PDF.

---

### M4 — GSTR-1 / GSTR-3B Return File Preparation (4 days)

**Goal:** produce portal-ready return files offline.

- GSTR-1 JSON builder (sections `b2b`, `b2cl`, `b2cs`, `hsn`, `credit/debit notes`, `docs`, `ecmrcredit`) matching the GSTN offline-utility schema.
- GSTR-3B summary table view (outward supplies, input tax credit placeholders, net payable).
- Data-quality checks surfaced in the UI: missing GSTIN on B2B invoices, missing HSN, invalid place of supply, unconfirmed IRN.
- Export JSON/CSV via the Android share sheet; import the portal's JSON to reconcile differences.

**Acceptance criteria**
- Exported GSTR-1 JSON validates against the official schema (sample fixtures) and matches invoice data.

---

### M5 — Hardening, Localisation, QA (3 days)

- India-specific strings; drop remaining EU strings; ensure `hi-IN`, `ta-IN`, `te-IN`, `kn-IN`, `ml-IN`, `mr-IN`, `bn-IN`, `gu-IN`, `pa-IN` resources cover the new surfaces.
- Backup/restore (`Backup JSON` / `Restore` in `SettingsScreen.kt` are currently no-ops) must include all GST columns and IRN/EWB fields.
- Settings: financial year, default state, GST registration toggle, round-off toggle, default invoice kind, GSTN credentials, UPI ID.
- Room migration tests, PDF golden tests, calculator property tests, Compose UI smoke tests.
- Compliance disclaimer in Settings ("This tool does not provide tax advice; verify GST returns before filing").

---

## 9. Testing & Verification Strategy

| Layer | Tooling | Target |
|---|---|---|
| Tax engine | JUnit 5 + Truth (existing convention in `core/calculator/src/test/.../InvoiceCalculatorTest.kt`) | 100% branch coverage of `GstTaxEngine`; property tests for discount allocation and rounding |
| Validators | JUnit 5 | Known-good/bad GSTIN, PAN, HSN, IFSC, UPI corpora |
| Room migrations | `MigrationTestHelper` (androidTest) | v1 → v2 preserves rows and backfills correctly |
| PDF rendering | Unit test on extracted text (pdfbox-android already available) | Required GST fields present; tax totals match the engine output |
| Repository | MockK (already in the version catalogue) | Snapshot integrity: editing an item does not change issued invoices |
| UI | Compose UI tests on the connected device + AVD `Medium_Phone_API_37.0` | Create → save → preview → share happy path per milestone |
| Build gates | `./gradlew :app:lintDebug :app:assembleDebug :core:calculator:test` | Must pass before every milestone merge |

---

## 10. Risk Register

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| R1 | Room v1 → v2 migration loses user data | Medium | Critical | Explicit `Migration` + `MigrationTestHelper` test + in-app auto-backup before first upgrade |
| R2 | Float rounding drift in tax totals | High | High | `Money` value class (Long paise) introduced in M0 before any tax work |
| R3 | GST slab/rate table becomes stale | Medium | Medium | Rates live in seed/reference data with user override; never hardcoded in logic |
| R4 | GSTN sandbox credentials unavailable | Medium | Medium | Manual IRN path is the primary UX; API work is strictly additive and feature-flagged |
| R5 | Scope creep — EU and Indian models coexisting | Medium | Medium | EU stack removed wholesale in M0; one tax model only |
| R6 | PDF layout regression on multi-page invoices | Medium | Medium | Section-based renderer + golden-file tests + manual review on device |
| R7 | Historical invoices break after schema changes | Medium | High | Snapshot columns added and backfilled; catalogue edits never mutate invoice rows |
| R8 | Heat-map: reports imply tax-filing capability | Low | Medium | Explicit disclaimers; "preparation", not "filing", in all copy |

---

## 11. Effort Summary & Sequencing

| Milestone | Scope | Effort | Depends on |
|---|---|---|---|
| M0 | Foundations: migration, `Money`, EU removal, module setup | 3 d | — |
| M1 | Master data + validators + tax engine + GST invoice flow + reports | 12–14 d | M0 |
| M2 | GST Tax Invoice PDF + thermal receipt + preview/share flow | 4–5 d | M1 |
| M3 | GSTN API: IRN + e-way bill | 6–8 d | M2 (IRN QR), credentials |
| M4 | GSTR-1 / GSTR-3B file preparation | 4 d | M1 |
| M5 | Hardening, localisation, backup/restore, QA | 3 d | M2–M4 |
| | **Total** | **32–37 d** | |

**Sequencing note:** M4 depends only on M1 and can run in parallel with M2/M3. M3 is credential-gated and can slip without blocking the offline product.

---

## Appendix A — File Change Index

### New files
```
core/model/src/main/java/com/invoicebuilder/core/model/Money.kt
core/model/src/main/java/com/invoicebuilder/core/model/GstModels.kt          # enums + GstTaxGroup + inputs
core/common/src/main/java/com/invoicebuilder/core/common/validation/GstinValidator.kt
core/common/src/main/java/com/invoicebuilder/core/common/validation/PanValidator.kt
core/common/src/main/java/com/invoicebuilder/core/common/validation/HsnValidator.kt
core/common/src/main/java/com/invoicebuilder/core/common/validation/IfscValidator.kt
core/common/src/main/java/com/invoicebuilder/core/common/validation/UpiValidator.kt
core/common/src/main/java/com/invoicebuilder/core/common/AmountInWords.kt
core/common/src/main/java/com/invoicebuilder/core/common/GstReferenceData.kt    # states, slabs, units
core/calculator/src/main/java/com/invoicebuilder/core/calculator/GstTaxEngine.kt
core/calculator/src/test/java/com/invoicebuilder/core/calculator/GstTaxEngineTest.kt
core/database/src/main/java/com/invoicebuilder/core/database/Migrations.kt
core/gstn/...  (module: token client, IRN gateway, EWB gateway, DTOs)
core/pdf/src/main/java/com/invoicebuilder/core/pdf/GstInvoiceRenderer.kt
app/src/main/java/com/invoicebuilder/mobile/ui/screens/business/BusinessProfileScreen.kt
app/src/main/java/com/invoicebuilder/mobile/ui/screens/banks/BankAccountScreen.kt
app/src/main/java/com/invoicebuilder/mobile/ui/screens/reports/GstrSummaryScreen.kt
app/src/main/java/com/invoicebuilder/mobile/ui/components/GstFields.kt
app/src/main/java/com/invoicebuilder/mobile/ui/viewmodel/BusinessViewModel.kt
app/src/main/java/com/invoicebuilder/mobile/ui/viewmodel/BankViewModel.kt
app/src/main/java/com/invoicebuilder/mobile/ui/viewmodel/GstrReportsViewModel.kt
```

### Modified files
```
core/model/.../Models.kt          # Money, GstTaxGroup, GstInvoiceTotals, entity mirrors
core/model/.../Enums.kt           # Gst enums; remove EInvoice; AmountFormat.EnIN retained
core/database/.../AppDatabase.kt  # version = 2 + addMigration
core/database/.../dao/Daos.kt     # queries for GST reports, e-way bill, sequences with FY
core/database/.../entity/Entities.kt, entity/Snapshots.kt   # new columns
core/database/.../repository/InvoiceRepository.kt, Repositories.kt
core/calculator/.../InvoiceCalculator.kt   # delegate to GstTaxEngine
core/pdf/.../PdfGenerator.kt      # section-based renderer
core/common/.../EscPosBuilder.kt  # GST receipt
app/.../MainActivity.kt           # new tabs (Business, Banks), navigation wiring
app/.../InvoiceBuilderApp.kt      # seeding of reference data
app/.../di/DatabaseModule.kt      # remove destructive fallback
app/.../ui/screens/invoices/InvoiceFormScreen.kt      # GST steps
app/.../ui/screens/invoices/InvoicePreviewScreen.kt   # IRN capture, share flow
app/.../ui/screens/invoices/InvoiceListScreen.kt      # pending-sync badge, PDF open on tap
app/.../ui/screens/clients/ClientListScreen.kt        # GSTIN fields
app/.../ui/screens/items/ItemListScreen.kt            # HSN + rate columns
app/.../ui/screens/reports/ReportsScreen.kt           # real totals, GSTR cards
app/.../ui/screens/settings/SettingsScreen.kt         # GST settings, backup/restore
app/.../ui/viewmodel/*.kt                            # new state for GST fields
app/src/main/res/values*/strings.xml                  # India strings, remove EU strings
app/build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml
```

---

## Appendix B — GST Reference Data

### B.1 State Codes
`01` Jammu & Kashmir · `02` Himachal Pradesh · `03` Punjab · `04` Chandigarh · `05` Uttarakhand · `06` Haryana · `07` Delhi · `08` Rajasthan · `09` Uttar Pradesh · `10` Bihar · `11` Sikkim · `12` Arunachal Pradesh · `13` Nagaland · `14` Manipur · `15` Mizoram · `16` Tripura · `17` Meghalaya · `18` Assam · `19` West Bengal · `20` Jharkhand · `21` Odisha · `22` Chhattisgarh · `23` Madhya Pradesh · `24` Gujarat · `25` Daman & Diu (old) / Dadra & Nagar Haveli and Daman & Diu · `26` Karnataka · `27` Maharashtra · `29` Andhra Pradesh (old) · `30` Goa · `31` Lakshadweep · `32` Kerala · `33` Tamil Nadu · `34` Puducherry · `35` Andaman & Nicobar Islands · `36` Telangana · `37` Ladakh · `38` Andhra Pradesh (new) · `97` Other Territory.

> State-code lists are periodically revised by the GST Council; the app ships them as **seed data** (not hardcoded logic) so they can be updated without touching the tax engine.

### B.2 GST Slabs
| Rate | Typical use |
|---|---|
| 0% | Essential goods, exempt supplies |
| 0.25% | Rough diamonds |
| 3% | Gold, silver, jewellery (pre-2022 rates) |
| 5% | Essentials, economy goods/services |
| 12% | Processed food, mobile phones, services |
| 18% | Most goods and services (default) |
| 28% | Luxury, demerit goods, sin goods |
| 6% / 7.5% | Special categories (gold, jewellery, MRO) |
| 1.5% cess | Applicable on top of 12%/7.5% in specific categories |

### B.3 GSTIN Checksum (mod-36)
Alphabet used for the checksum (36 characters):
`0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ` → factor table:
`1 2 3 4 5 6 7 8 9 1 2 3 4 5 6 7 8 9 1 2 3 4 5 6 7 8 9 1 2 3 4 5 7 8 9 1 2 3 4 5 6 7 8 9 1 2 3 4 5 6 7 8 9`
Algorithm: for each of the first 14 characters, take the alphanumeric value, multiply by the factor for its position, add to a running sum; the check digit is the value at index `sum mod 36`.

### B.4 UPI Payment Intent
```
upi://pay?pa=<upiId>&pn=<payeeName>&am=<amount>&cu=INR&tn=<note>&tr=<txnRef>
```
Rendered as a QR (zxing) on the invoice; tapping the QR in a UPI app opens the payment sheet.

### B.5 IRN QR Payload
```
IRN<32-hex-char IRN>|SHA256|<base64 SHA-256 hash of the IRN>
```

---

*End of document. Compliance notes in this plan describe technical implementation only and are not tax advice; users remain responsible for the accuracy of filed returns.*
