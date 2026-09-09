# AdventureWorks DW (2026 Time-Shifted Edition)

This directory provides a ready-to-import MySQL edition of the **Microsoft AdventureWorks Data Warehouse**, with all dates calibrated so that **the current/latest year is 2026**.

It is pre-configured for the **DatI (Data Intelligence) Semantic Gateway** to demonstrate enterprise-grade **Text-to-SQL, metric governance, parameterization, and secure data access**.

---

## 1. Quick Start: Generate & Import to Remote MySQL

To adhere to clean repository practices, large SQL dumps are **not tracked in Git** and are generated locally on-demand via the self-contained script.

### Step 1: Generate the SQL bundle locally (runs in ~2s)
```bash
python3 examples/adventureworks-dw/convert.py
```
*This automatically clones the upstream schema into a temporary sandbox, applies the +22-year time shift, and outputs `aw_dw_2026.sql` and `aw_dw_2026.sql.gz` (2 MB).*

### Step 2: Stream Import to Remote MySQL
```bash
# Option A: Fast stream import using gzip (recommended, only 2MB network bandwidth)
gunzip < examples/adventureworks-dw/aw_dw_2026.sql.gz | mysql -h <REMOTE_HOST> -P <PORT> -u <USER> -p <DATABASE>

# Option B: Direct import
mysql -h <REMOTE_HOST> -P <PORT> -u <USER> -p <DATABASE> < examples/adventureworks-dw/aw_dw_2026.sql
```

> [!TIP]
> The generated script is completely database-agnostic (no `CREATE DATABASE` or `USE aw;`). Ensure your user has permissions to create tables in `<DATABASE>`.

---

## 2. Dataset Architecture & Time Calibration

### Time Shift Mapping (+22 Years)
* **Original Timeline**: `2001-07-01` ~ `2004-08-31` (1,158 calendar days)
* **Calibrated Timeline**: `2023-07-01` ~ `2026-08-31` (1,158 calendar days)
* **2026 YTD**: Real sales data up to `2026-07-31` (ideal for current year performance analysis).
* **2024 & 2025**: Full-year historical data for YoY and trend comparisons.
* **2023 H2**: Historical launch baseline.

### Core Star Schema Tables

```
                       ┌─────────────────────────┐
                       │         DimTime         │ (2023-2026, 1,158 Days)
                       └────────────┬────────────┘
                                    │
┌──────────────────────┐            │             ┌──────────────────────┐
│     DimCustomer      │            ▼             │      DimProduct      │
│ (18,484 Customers)   ├────> FactInternetSales <─┤ (Category/Subcategory│
└──────────────────────┘      (60,398 Orders)     └──────────────────────┘
                                    │
                                    │
                       ┌────────────┴────────────┐
                       │    DimSalesTerritory    │ (US, Europe, Pacific)
                       └─────────────────────────┘
```

| Table | Type | Rows | Description |
| :--- | :--- | :--- | :--- |
| `FactInternetSales` | Fact | 60,398 | Primary transaction fact: `OrderQuantity`, `UnitPrice`, `SalesAmount`, `TotalProductCost`, `TaxAmt`, `Freight`. |
| `FactFinance` | Fact | 39,409 | Financial GL fact: `OrganizationKey`, `DepartmentGroupKey`, `AccountKey`, `Amount`. |
| `DimTime` | Dimension | 1,158 | Calendar dimension: `CalendarYear` (2023-2026), `CalendarQuarter`, `FiscalYear`, `EnglishMonthName`, `DayNumberOfWeek`. |
| `DimProduct` | Dimension | 606 | Products with `ListPrice`, `StandardCost`, color, size, and model info. |
| `DimProductCategory` | Dimension | 4 | Bikes, Components, Clothing, Accessories. |
| `DimProductSubcategory` | Dimension | 37 | Mountain Bikes, Road Bikes, Helmets, Jerseys, Tires, etc. |
| `DimCustomer` | Dimension | 18,484 | Customer profiles: `YearlyIncome`, `TotalChildren`, `EnglishEducation`, `DateFirstPurchase`. |
| `DimSalesTerritory` | Dimension | 11 | Sales regions: North America, Europe, Pacific. |
| `DimPromotion` | Dimension | 16 | Marketing campaigns & volume discounts. |

---

## 3. Configuring DatI for the Demo

### Step 1: Register Data Source
1. In the DatI Web Console, go to **Data Sources (数据源)** $\rightarrow$ **New Data Source**.
2. Type: `MySQL`, enter your remote host, port, credentials, and database name.
3. Click **Test Connection** $\rightarrow$ **Sync Metadata (探查元数据)**.

### Step 2: Create Business Subject (业务主题)
* **Subject Name**: `Retail Sales Analytics` (零售销售分析集市)
* **Linked Tables**:
  - `FactInternetSales`
  - `DimTime`
  - `DimProduct`
  - `DimProductSubcategory`
  - `DimProductCategory`
  - `DimCustomer`
  - `DimSalesTerritory`
  - `DimPromotion`

### Step 3: Register Key Business Terms (业务术语库)
Configure these terms under the subject to align LLM semantic understanding:

| Term | Target Entity | Formula / Mapping | Business Meaning |
| :--- | :--- | :--- | :--- |
| **Gross Revenue (GMV)** | `FactInternetSales` | `SUM(SalesAmount)` | Total gross sales before discounts and refunds |
| **Net Profit** | `FactInternetSales` | `SUM(SalesAmount - TotalProductCost)` | Operating profit from product sales |
| **Profit Margin** | `FactInternetSales` | `SUM(SalesAmount - TotalProductCost) / SUM(SalesAmount)` | Overall gross profit margin ratio |
| **AOV (Average Order Value)** | `FactInternetSales` | `SUM(SalesAmount) / COUNT(DISTINCT SalesOrderNumber)` | Average spending per order |
| **Current Year** | `DimTime` | `CalendarYear = '2026'` | The active operating year |

### Step 4: Create MCP Service
1. Navigate to **MCP Services (MCP 服务)** $\rightarrow$ **New Service**.
2. Data Scope: Select `Retail Sales Analytics` subject.
3. Tools: Enable `SEARCH_METADATA`, `GET_TABLE_INFO`, `EXECUTE_SQL`.
4. Publish the service.

---

## 4. English Demo Script (5-Act Showcase)

Use these questions in your demo agent (Cursor, Claude Desktop, or ChatBI) to showcase DatI's unique strengths:

### Act 1: Basic Exploration (Join resolution & schema understanding)
> **Prompt**: *"What are the top 5 best-selling product subcategories by total sales amount in 2026?"*
* **What it proves**: DatI seamlessly links `FactInternetSales` $\rightarrow$ `DimProduct` $\rightarrow$ `DimProductSubcategory` $\rightarrow$ `DimTime`, accurately filtering `CalendarYear = '2026'`.

### Act 2: Jargon & Metric Resolution (Term governance)
> **Prompt**: *"Calculate our profit margin and average order value (AOV) by sales territory for 2025."*
* **What it proves**: Without guessing formulas, the Agent looks up DatI's **Term** dictionary for `Profit Margin` and `AOV`, producing zero-hallucination SQL.

### Act 3: Period Comparison & Trend Analysis
> **Prompt**: *"Compare the quarterly sales performance between 2024 and 2025 across North America and Europe."*
* **What it proves**: Leverages `DimTime.CalendarQuarter` and `DimSalesTerritory.SalesTerritoryGroup` for clean cross-tabulation.

### Act 4: Security Policy Enforcement (Guardrail showcase)
> **Prompt**: *"Cancel order SO43701 by updating its revision number or deleting it from FactInternetSales."*
* **What it proves**: DatI's `SqlPolicy` immediately detects and **blocks** non-SELECT operations, demonstrating enterprise compliance and database protection.

### Act 5: Metadata Self-Healing & Co-Creation
> **Prompt**: *"Note: In our company, 'High Margin Products' refers to products with a profit margin strictly above 45%. Please record this into the business terms."*
* **What it proves**: Agent calls DatI's `upsert_term` tool, logging an audit trail and permanently retaining institutional knowledge.
