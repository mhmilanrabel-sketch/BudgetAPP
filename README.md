# MoneyManager LK 🇱🇰

An offline, secure, confidential personal finance and salary reconciliation application built for Sri Lanka in Kotlin and Jetpack Compose. 

MoneyManager LK automatically parses official Sri Lankan 2-column salary slip PDFs and multi-section monthly budget sheets (CSV/XLSX), mathematically verifies all payroll figures, and reconciles them into an accurate monthly financial overview.

---

## 🛡️ Security Architecture & Privacy Mandates

1. **100% Offline by Design**: `android.permission.INTERNET` is completely omitted from `AndroidManifest.xml`. The app contains zero network SDKs, zero telemetry, and zero cloud sync. All data stays strictly on your physical device.
2. **Hardware-Backed Encryption (SQLCipher + Android KeyStore)**: The Room database is encrypted with SQLCipher using a 256-bit AES passphrase protected by the Android KeyStore hardware (TEE / StrongBox).
3. **FLAG_SECURE**: All activities display sensitive financial information with `WindowManager.LayoutParams.FLAG_SECURE`, preventing screenshots, screen recording, and exposure in recent task lists.
4. **Zero-Disk Ingestion**: Uploaded PDFs, CSVs, and Excel spreadsheets are parsed purely in-memory from `InputStream` and never written to temporary or persistent disk storage.
5. **Strict Mathematical Validation**: Every imported salary slip enforces `grossSalary - totalDeductions == netSalary` within ±1 LKR tolerance. Any corrupted or tampered file is rejected with a clear mathematical discrepancy error.
6. **Biometric App Lock**: Integrated with `androidx.biometric:biometric` for fingerprint and facial authentication.

---

## 📊 File Format Specifications

### 1. Sri Lankan Salary Slip PDF
The parser utilizes PDFBox Android (`com.tom-roush:pdfbox-android`) with `sortByPosition = true` to accurately read 2-column Sri Lankan pay slips:
* **Gross Salary & Allowances**: Basic Salary, Vehicle Allowance, Exceptional Incentive, Shift Compensation.
* **Statutory Deductions**: APIT (Advance Personal Income Tax), EPF Employee Contribution (8%), Funeral Fund, Excess Mobile, Meals Deduction.
* **Take-Home**: Net Salary, Salary To Bank.
* **Employer Statutory Contributions**: EPF Employer Cont (12%), ETF Employer Cont (3%), Stamp Duty.

### 2. Two-Section Budget Sheet (CSV / XLSX)
Parses spreadsheets containing two independent data tables on the same rows:
* **Row 2**: Detects `Current Bank Rs` opening balance (e.g. `Rs. 240,760.00`).
* **Left Section (Columns A–D)**:
  * **Col A**: Expense item description.
  * **Col B**: Budgeted amount.
  * **Col C**: Not paid (unpaid bills carried forward to next month).
  * **Col D**: Total real cash pay.
* **Right Section (Columns K–N)**:
  * **Col K**: Category labels (`Basic Salary`, `Gross Salary`, `APIT`, `EPF`, `Sal`, `Hand Save`, `Saving`).
  * **Col N**: Category values.

### 3. Expense Classification Engine
Keywords automatically classified as **Mandatory (Fixed)**:
`rent`, `mortgage`, `electric`, `water`, `gas`, `cell phone`, `peo tv`, `van`, `fees`, `school`, `poli`, `washing`, `grocery`, `sanga`, `nipuna`, `mint pay`, `amma`.
All other items are classified as **Optional (Discretionary)**. Users can customize this keyword list in Settings or toggle any individual expense row on the ledger.

---

## 🛠️ Tech Stack

* **Language**: Kotlin 2.2+
* **UI**: Jetpack Compose, Material 3
* **Architecture**: Clean Architecture (Data, Domain, Presentation) + MVVM
* **Local DB**: Room + SQLCipher 256-bit encryption
* **Key Storage**: Android KeyStore (MasterKey AES-GCM)
* **PDF Parsing**: `com.tom-roush:pdfbox-android:2.0.27.0`
* **CSV Parsing**: `com.opencsv:opencsv:5.9`
* **XLSX Parsing**: Streaming Zip XML Parser (`XmlPullParser`)
* **OCR Fallback**: ML Kit Text Recognition on-device
* **Concurrency**: Kotlin Coroutines + Flow
* **Formatting**: `NumberFormat` with `Locale("en", "LK")` ("Rs. X,XXX.XX")

---

## 🚀 Building & Testing

### Compile the Application
```bash
gradle assembleDebug
```

### Run Local Unit Tests
```bash
gradle :app:testDebugUnitTest
```
Unit test suite verifies:
* `PaySlipParserTest`: Exact parsing of sample Sri Lankan salary slip and rejection of mathematically mismatched slips.
* `BudgetSheetParserTest`: Two-section CSV parsing, opening bank balance detection, real pay calculations, and keyword classification.
* `ReconciliationUseCaseTest`: Full reconciliation formula and warning generation.
