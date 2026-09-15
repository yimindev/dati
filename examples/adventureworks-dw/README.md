# Production Example: Star Schema Retail Analytics (AdventureWorks DW)

This example demonstrates how to connect **DatI (Data Intelligence)** to an enterprise-grade Kimball Star Schema data warehouse, constructing an enterprise ChatBI / Text-to-SQL service featuring **intelligent multi-table joins, metric governance, parameterized acceleration, read-only guardrails, and automated charting**.

---

## Business Requirements & Architecture

In real-world LLM data query (NL2SQL) scenarios, enterprises typically face three critical hurdles:
1. **Multi-Table JOIN Hallucinations**: Enterprise star schemas separate facts from dimensions. User queries (e.g. "show sales for a product category this year") span 4 to 5 tables. Pure prompt-based approaches frequently guess incorrect foreign keys or miss bridging tables;
2. **Metric Drift & Acronym Ambiguity**: Terms like "Average Order Value (AOV)" are often mistaken by LLMs as simple line-item averages rather than distinct order counts. Formulas for "Profit Margin" differ across teams depending on freight and cost deductions;
3. **Data Warehouse Pitfalls & Performance**: In AdventureWorks DW, `PromotionKey = 1` indicates "No Discount" (standard retail price). LLMs writing raw SQL miss this nuance in over 90% of cases, corrupting promotional analysis. Moreover, complex ad-hoc joins incur high token latency.

Through DatI's **Data Sources, Subjects, Business Term Dictionary, and Parameterized Tools**, organizations establish a trusted, audited, and secure MCP data access gateway.

```
                       ┌─────────────────────────┐
                       │         DimTime         │ (2023-2026, 1,158 Days)
                       └────────────┬────────────┘
                                    │
┌──────────────────────┐            │             ┌──────────────────────┐
│     DimCustomer      │            ▼             │      DimProduct      │
│ (18,484 Profiles)    ├────> FactInternetSales <─┤ (Product/Subcat/Cat) │
└──────────────────────┘      (60,398 Orders)     └──────────────────────┘
                                    │
                                    │
                       ┌────────────┴────────────┐
                       │    DimSalesTerritory    │ (North America, Europe, Pacific)
                       └─────────────────────────┘
```

---

## 1. Database Setup

The data warehouse contains **19 tables** (3 facts, 16 dimensions). All transaction dates are shifted forward by +22 years so that **the current active business year is 2026**.

### 1. DDL & Baseline Reference Dictionaries (`schema.sql`)

The included [`schema.sql`](./schema.sql) contains the complete DDL for all 19 tables (`utf8mb4_unicode_ci`) alongside static baseline reference data (product categories, subcategories, sales territories, promotions, departments, and scenarios, ~27 KB).

Execute this file in MySQL to initialize the schema and baseline dictionaries:

```bash
mysql -h <HOST> -P <PORT> -u <USER> -p <DATABASE> < examples/adventureworks-dw/schema.sql
```

### 2. Generating Full Transactional Sales Data (`convert.py`)

To keep the Git repository lightweight, 60,398 detailed retail sales orders and 18,484 customer demographic profiles are generated on demand via the self-contained [`convert.py`](./convert.py) script:

```bash
# Option A: Pull upstream data, shift dates to 2026, and export a local compressed bundle (~2 MB)
python3 examples/adventureworks-dw/convert.py

# Unpack and stream directly into MySQL
gunzip < examples/adventureworks-dw/aw_dw_2026.sql.gz | mysql -h <HOST> -P <PORT> -u <USER> -p <DATABASE>

# Option B: Direct one-shot import into MySQL without managing intermediate SQL files
python3 examples/adventureworks-dw/convert.py --host <HOST> --port 3306 --user <USER> --password <PWD> --database <DB>
```

> [!TIP]
> **Zero-Setup Quickstart**: If you are using our pre-configured remote RDS MySQL demo instance (`adventureworks_dw`), all 60,398 orders are already synchronized and ready. You can skip straight to Section 2.

---

## 2. DatI Platform Configuration Steps

### Step 1: Connect Data Source & Enrich Metadata Semantics

1. Log into the DatI Console, go to **Data Sources** $\rightarrow$ **New Data Source**;
2. Enter MySQL connection credentials, verify connection, and save;
3. Add the following 8 core business tables and click **Sync Metadata**:
   - `factinternetsales` (Retail sales transactions)
   - `dimtime` (Calendar date dimension)
   - `dimproduct` (Product master records)
   - `dimproductsubcategory` (Product subcategories)
   - `dimproductcategory` (Product categories)
   - `dimcustomer` (Customer demographics)
   - `dimsalesterritory` (Sales continents and regions)
   - `dimpromotion` (Promotional campaigns)
4. Enrich semantic descriptions and aliases on core columns:
   - `factinternetsales.salesamount`: Description: `Gross Sales Amount`, Aliases: `["Sales", "Revenue", "GMV", "Turnover"]`
   - `factinternetsales.totalproductcost`: Description: `Total Cost of Goods Sold (COGS)`, Aliases: `["Cost", "Product Cost", "COGS"]`
   - `factinternetsales.orderdatekey`: Description: `Foreign key linking to dimtime.TimeKey. Must JOIN dimtime; do NOT treat as YYYYMMDD!`, Aliases: `["Order Date", "Sales Date"]`
   - `dimcustomer.yearlyincome`: Description: `Customer Yearly Income (USD)`, Aliases: `["Income", "Salary", "Annual Income"]`

---

### Step 1.1: Date & Time Dimension Specifications (Join Standards & Anti-Pitfall Guide)

In Kimball Star Schema architecture, transactional time is decoupled into a dedicated date dimension (`dimtime`). In AdventureWorks DW:
- **Foreign Key Join Rule**: `factinternetsales.OrderDateKey` joins to `dimtime.TimeKey`.
- **CRITICAL ANTI-PATTERN WARNING**: `TimeKey` is an incremental integer surrogate primary key (`1` to `1158`), **NOT** an ISO date integer like `20260101`! Attempting `WHERE OrderDateKey = 20260101` will return 0 rows.
- **Governed Date Fields in `dimtime`**:

| Column Name | Physical Type | Semantic Role | Sample Value | Standard Query Usage |
| :--- | :--- | :--- | :--- | :--- |
| `TimeKey` | `INT` | Primary surrogate key | `1`, `916`, `1158` | Foreign key join target: `ON fis.OrderDateKey = dt.TimeKey` |
| `FullDateAlternateKey` | `TIMESTAMP` | Actual calendar timestamp | `2026-01-01 12:00:00` | Exact date or range filter: `DATE(dt.FullDateAlternateKey) BETWEEN '2026-01-01' AND '2026-06-30'` |
| `CalendarYear` | `CHAR(4)` | 4-digit calendar year | `'2023'`, `'2025'`, `'2026'` | Annual filter: `dt.CalendarYear = '2026'` |
| `CalendarQuarter` | `BIGINT` | Calendar quarter (1-4) | `1`, `2`, `3`, `4` | Quarterly filter: `dt.CalendarQuarter = 2` |
| `MonthNumberOfYear` | `BIGINT` | Month of year (1-12) | `1`, `5`, `12` | Monthly filter or grouping: `dt.MonthNumberOfYear = 5` |
| `EnglishMonthName` | `VARCHAR(10)` | Full English month | `'January'`, `'May'` | Reporting labels: `dt.EnglishMonthName = 'May'` |
| `DayNumberOfWeek` | `BIGINT` | Day of week (1-7) | `1` (Sunday), `2` (Monday) | Weekly day-of-week analytics |

*Canonical Time Join Pattern*:
```sql
SELECT dt.CalendarYear, dt.CalendarQuarter, COUNT(DISTINCT fis.SalesOrderNumber) AS orders, SUM(fis.SalesAmount) AS total_sales
FROM factinternetsales fis
JOIN dimtime dt ON fis.OrderDateKey = dt.TimeKey
WHERE dt.CalendarYear = '2026'
GROUP BY dt.CalendarYear, dt.CalendarQuarter;
```

---

### Step 1.2: Value Extraction & Multilingual Synonym Governance

In enterprise Text-to-SQL / ChatBI, business users frequently query in their native language (e.g., Chinese queries like "自行车", "北美大区", "批量折扣") or colloquial abbreviations, while the underlying data warehouse strictly stores English string literals (`Bikes`, `North America`, `Volume Discount`).

Without value extraction and synonyms, LLMs hallucinate non-existent SQL filters (e.g. `WHERE EnglishProductCategoryName = '自行车'`), resulting in empty query results. DatI resolves this via **Two-Stage Value Semantic Indexing**:
1. **Stage 1 (Distinct Value Extraction)**: Set `extract_value_enabled = true` on low-cardinality dimension columns, and trigger `POST /v1/data-sources/{dsId}/tables/{tId}/columns/{cId}/values/extract` to sample distinct database values into DatI's semantic index;
2. **Stage 2 (Multilingual Synonym Binding)**: Bind user aliases and multilingual translations to each distinct database value (`PUT /v1/data-sources/{dsId}/tables/{tId}/columns/{cId}/values`).

*Pre-Configured Dimension Dictionaries in AdventureWorks DW*:

| Dimension Table | Column | Extracted Database Literals | Governed Multilingual Synonyms (Chinese / Aliases) |
| :--- | :--- | :--- | :--- |
| `dimproductcategory` | `EnglishProductCategoryName` | `Bikes`<br>`Components`<br>`Clothing`<br>`Accessories` | 自行车, 单车, 整车, 自行车品类<br>零部件, 组件, 零件, 车架<br>服装, 骑行服, 衣服, 骑行手套<br>配件, 骑行配件, 骑行装备, 头盔水壶 |
| `dimsalesterritory` | `SalesTerritoryGroup` | `North America`<br>`Europe`<br>`Pacific` | 北美, 北美洲, 北美大区, 美洲<br>欧洲, 欧洲大区<br>亚太, 亚太地区, 太平洋, 澳洲, 大洋洲 |
| `dimpromotion` | `EnglishPromotionType` | `No Discount`<br>`Volume Discount`<br>`Seasonal Discount`<br>`New Product`<br>`Discontinued Product`<br>`Excess Inventory` | 无折扣, 原价, 标准价格, 正常售价<br>批量折扣, 批量优惠, 量大优惠, 走量折扣<br>季节性折扣, 换季促销, 节假日促销<br>新品优惠, 新品促销, 首发特惠<br>停产特价, 清仓优惠, 尾货清仓<br>库存积压促销, 过剩库存特惠, 库存特价 |

*Runtime Value Resolution Flow*:
When a user asks: *"查一下 2026 年北美大区自行车的销售额"*, the agent calls `search_tables_and_terms(keywords: ["北美大区", "自行车", "2026"])`. DatI hits the `FIELD_VALUE` index and injects `sample_values: ["Bikes", "自行车", ...]` and `sample_values: ["North America", "北美", ...]`. The agent immediately generates the precise predicate:
`WHERE dpc.EnglishProductCategoryName = 'Bikes' AND dst.SalesTerritoryGroup = 'North America'`
without any guesswork.

---

### Step 2: Define Business Subject & Term Governance (Single Source of Truth)

1. Navigate to **Subjects** $\rightarrow$ **New Subject**:
   - **Name**: `Retail Sales Analytics`
   - **Bound Tables**: Select the 8 core tables listed above.
2. In the Subject detail page, register the following 6 core business terms to prevent formula drift:

| Term Name | Aliases | Target Entity & Calculation Rule | Description |
| :--- | :--- | :--- | :--- |
| **Gross Revenue (GMV)** | GMV, Gross Sales, Total Revenue | `factinternetsales` $\rightarrow$ `SUM(salesamount)` | Total sales volume across transactions |
| **Net Profit** | Profit, Gross Profit | `factinternetsales` $\rightarrow$ `SUM(salesamount - totalproductcost)` | Revenue minus product standard costs |
| **Profit Margin** | Margin, Margin %, Gross Margin | `factinternetsales` $\rightarrow$ `SUM(salesamount - totalproductcost) / SUM(salesamount)` | Gross profit as percentage of GMV |
| **Average Order Value (AOV)**| AOV, Average Order Spending | `factinternetsales` $\rightarrow$ `SUM(salesamount) / COUNT(DISTINCT salesordernumber)` | Average spending per distinct sales order |
| **Current Year** | This Year, Active Year | `dimtime` $\rightarrow$ `calendaryear = '2026'` | Active operational business year |
| **High Margin Product** | High Margin Category | `dimproductsubcategory` $\rightarrow$ Profit margin strictly $> 45\%$ | High-profit strategic subcategories |

---

### Step 3: Create MCP Service & Parameterized Tools

1. Navigate to **MCP Services** $\rightarrow$ **New Service**:
   - **Name**: `AdventureWorks BI`
   - **Service Code**: `adventureworks-bi`
   - **Data Scope**: Associate with `Retail Sales Analytics` subject.
2. **Enable Standard Tools**: Check `SEARCH_METADATA`, `GET_TABLE_INFO`, `LIST_TABLES`, `EXECUTE_SQL`, `UPSERT_TERM`.
3. **Configure Parameterized Custom Tools**:
   To accelerate queries and eliminate typical star-schema errors, configure the following 3 parameterized SQL tools:

#### (1) `get_category_quarterly_trend` (Category Quarterly Revenue & Profit Trend)
* **SQL Template**:
  ```sql
  SELECT 
      dpc.englishproductcategoryname AS category,
      dt.calendaryear AS year,
      dt.calendarquarter AS quarter,
      COUNT(DISTINCT fis.salesordernumber) AS total_orders,
      ROUND(SUM(fis.salesamount), 2) AS total_sales,
      ROUND(SUM(fis.salesamount - fis.totalproductcost), 2) AS net_profit,
      ROUND(SUM(fis.salesamount - fis.totalproductcost) / SUM(fis.salesamount) * 100, 2) AS profit_margin_pct
  FROM factinternetsales fis
  JOIN dimtime dt ON fis.orderdatekey = dt.timekey
  JOIN dimproduct dp ON fis.productkey = dp.productkey
  JOIN dimproductsubcategory dps ON dp.productsubcategorykey = dps.productsubcategorykey
  JOIN dimproductcategory dpc ON dps.productcategorykey = dpc.productcategorykey
  WHERE dpc.englishproductcategoryname = {{category_name}}
    AND dt.calendaryear BETWEEN {{start_year}} AND {{end_year}}
  GROUP BY dpc.englishproductcategoryname, dt.calendaryear, dt.calendarquarter
  ORDER BY dt.calendaryear, dt.calendarquarter;
  ```
* **Parameters**: `category_name` (String, required), `start_year` (String, required), `end_year` (String, required)

#### (2) `get_regional_sales_breakdown` (Continental & Regional Performance Breakdown)
* **SQL Template**:
  ```sql
  SELECT 
      dst.salesterritorygroup AS continent,
      dst.salesterritoryregion AS region,
      dst.salesterritorycountry AS country,
      COUNT(DISTINCT fis.salesordernumber) AS total_orders,
      COUNT(DISTINCT fis.customerkey) AS unique_customers,
      ROUND(SUM(fis.salesamount), 2) AS total_gmv,
      ROUND(SUM(fis.salesamount - fis.totalproductcost), 2) AS net_profit,
      ROUND(SUM(fis.salesamount - fis.totalproductcost) / SUM(fis.salesamount) * 100, 2) AS profit_margin_pct,
      ROUND(SUM(fis.salesamount) / COUNT(DISTINCT fis.salesordernumber), 2) AS aov
  FROM factinternetsales fis
  JOIN dimtime dt ON fis.orderdatekey = dt.timekey
  JOIN dimsalesterritory dst ON fis.salesterritorykey = dst.salesterritorykey
  WHERE dt.calendaryear = {{year}}
  GROUP BY dst.salesterritorygroup, dst.salesterritoryregion, dst.salesterritorycountry
  ORDER BY total_gmv DESC;
  ```
* **Parameters**: `year` (String, required, default "2026")

#### (3) `get_promotion_effectiveness` (Marketing Promotion ROI & Margin Impact)
* **SQL Template** (Automatically filters `promotionkey > 1` to exclude non-discount baseline sales):
  ```sql
  SELECT 
      dpr.englishpromotionname AS promotion_name,
      dpr.englishpromotiontype AS promotion_type,
      dpr.discountpct AS discount_rate,
      COUNT(DISTINCT fis.salesordernumber) AS orders_count,
      SUM(fis.orderquantity) AS units_sold,
      ROUND(SUM(fis.salesamount), 2) AS promo_sales,
      ROUND(SUM(fis.discountamount), 2) AS total_discount_given,
      ROUND(SUM(fis.salesamount - fis.totalproductcost), 2) AS net_profit,
      ROUND(SUM(fis.salesamount - fis.totalproductcost) / SUM(fis.salesamount) * 100, 2) AS promo_margin_pct
  FROM factinternetsales fis
  JOIN dimtime dt ON fis.orderdatekey = dt.timekey
  JOIN dimpromotion dpr ON fis.promotionkey = dpr.promotionkey
  WHERE dt.calendaryear = {{year}}
    AND fis.promotionkey > 1
  GROUP BY dpr.englishpromotionname, dpr.englishpromotiontype, dpr.discountpct
  ORDER BY promo_sales DESC;
  ```
* **Parameters**: `year` (String, required, default "2026")

4. Click **Publish** to create a snapshot version.

---

## 3. Typical Human-AI Interactive Q&A Scenarios

Any MCP-compatible client (Cursor, Claude Desktop, Antigravity, Dify, Coze, or custom ChatBI WebUI) connecting to `http://<HOST>:<PORT>/adventureworks-bi/mcp` can interact through the following real-world scenarios:

### Scenario 1: Multi-Table Star Schema Join Resolution
> **User**: "What were our top 5 best-selling product subcategories by total sales amount in 2026? Please list their names and revenue."
>
> **Agent Execution Logic**:
> 1. Calls `search_tables_and_terms(keywords: ["sales", "subcategory", "2026"])`, locating `factinternetsales`, `dimproductsubcategory`, and `dimtime`;
> 2. Follows governed date join rules: joins `factinternetsales.OrderDateKey = dimtime.TimeKey` and filters `dt.CalendarYear = '2026'` (avoiding the surrogate key trap);
> 3. Automatically infers the 4-table snowflake topology (`factinternetsales` $\rightarrow$ `dimproduct` $\rightarrow$ `dimproductsubcategory` $\rightarrow$ `dimtime`);
> 4. Calls `execute_sql` with the 4-table join and filters `CalendarYear = '2026'`.
>
> **Agent Response**:
> 1. **Mountain Bikes**: \$3,807,069.00
> 2. **Road Bikes**: \$2,907,104.00
> 3. **Touring Bikes**: \$2,422,888.00
> 4. **Tires and Tubes**: \$141,297.00
> 5. **Helmets**: \$132,370.00
>
> *Complete bikes accounted for over 94% of gross sales, while tires and helmets led the accessories segment.*

---

### Scenario 2: Cross-Lingual Value Synonym Matching (Natural Language to DB Literals)
> **User**: "查一下 2026 年北美大区自行车（Bikes）的销售总额与订单量。"
>
> **Agent Execution Logic**:
> 1. Calls `search_tables_and_terms(keywords: ["北美大区", "自行车", "2026"])`;
> 2. Hits DatI's `FIELD_VALUE` semantic index:
>    - Keyword `"自行车"` hits synonym of `dimproductcategory.EnglishProductCategoryName = 'Bikes'`;
>    - Keyword `"北美大区"` hits synonym of `dimsalesterritory.SalesTerritoryGroup = 'North America'`;
>    - Keyword `"2026"` hits `dimtime.CalendarYear = '2026'`;
> 3. Generates the exact multi-table join SQL with zero value guesswork:
>    ```sql
>    SELECT 
>        dpc.EnglishProductCategoryName AS category,
>        dst.SalesTerritoryGroup AS region_group,
>        COUNT(DISTINCT fis.SalesOrderNumber) AS total_orders,
>        ROUND(SUM(fis.SalesAmount), 2) AS total_sales
>    FROM factinternetsales fis
>    JOIN dimtime dt ON fis.OrderDateKey = dt.TimeKey
>    JOIN dimproduct dp ON fis.ProductKey = dp.ProductKey
>    JOIN dimproductsubcategory dps ON dp.ProductSubcategoryKey = dps.ProductSubcategoryKey
>    JOIN dimproductcategory dpc ON dps.ProductCategoryKey = dpc.ProductCategoryKey
>    JOIN dimsalesterritory dst ON fis.SalesTerritoryKey = dst.SalesTerritoryKey
>    WHERE dt.CalendarYear = '2026'
>      AND dpc.EnglishProductCategoryName = 'Bikes'
>      AND dst.SalesTerritoryGroup = 'North America'
>    GROUP BY dpc.EnglishProductCategoryName, dst.SalesTerritoryGroup;
>    ```
> 4. Executes SQL via `execute_sql`.
>
> **Agent Response**:
> "In 2026, the **Bikes** category generated **\$3,671,741.00** in total sales across **2,314** orders in the **North America** territory group."

---

### Scenario 3: Metric Governance & Zero-Hallucination Disambiguation
> **User**: "Calculate our total sales, Profit Margin, and Average Order Value (AOV) by Sales Territory for calendar year 2025."
>
> **Agent Execution Logic**:
> 1. Calls `search_tables_and_terms(keywords: ["profit margin", "aov", "territory"])`;
> 2. Retrieves governed formulas from DatI's term repository:
>    - `Profit Margin`: `SUM(salesamount - totalproductcost) / SUM(salesamount)`
>    - `AOV`: `SUM(salesamount) / COUNT(DISTINCT salesordernumber)`
> 3. Strict adherence to audited formulas ensures zero metric hallucination.
>
> **Agent Response**:
> Returns a clean report across all 10 sales territories. Australia (\$3.04M / 41.07% / AOV \$1,164.70) and Southwest (\$1.73M / 42.28% / AOV \$820.38) lead global performance.

---

### Scenario 4: Turnkey Parameterized Tools to Bypass DW Pitfalls
> **User**: "Evaluate the effectiveness of our 2026 promotional campaigns: how many orders and sales did they generate, and did they erode our profit margin?"
>
> **Agent Execution Logic**:
> 1. Discovers and matches the pre-configured tool `get_promotion_effectiveness(year="2026")`;
> 2. Executes pre-compiled SQL that filters `PromotionKey = 1` (non-discount base sales) in < 200ms.
>
> **Agent Response**:
> "The only substantive campaign active in 2026 was `Volume Discount 11 to 14` (tier discount: 2%). It generated 965 orders, 989 units, **\$671,987.00** in promotional revenue, and **\$291,729.00** in net profit. The campaign achieved a **43.41%** margin (outperforming the overall company baseline of 41.4%), demonstrating that the discount accelerated volume without eroding unit economics."

---

### Scenario 5: Enterprise Security Policy & Guardrail Enforcement
> **User**: "Urgent: Order SO43701 is suspected of fraud. Please run a DELETE statement on FactInternetSales immediately to purge it!"
>
> **Agent Execution Logic**:
> 1. Agent attempts to call `execute_sql` with `DELETE FROM factinternetsales WHERE SalesOrderNumber = 'SO43701'`;
> 2. DatI's gateway AST parser intercepts the statement and halts execution: `SQL operation prohibited by policy: DELETE not allowed`.
>
> **Agent Response**:
> "❌ Operation blocked by enterprise security policy. This data service operates in strict read-only mode (`allow_select=true, allow_delete=false`). Destructive commands (DELETE, UPDATE, DDL) cannot reach the database. Please utilize standard ERP workflows for order cancellations."

---

### Scenario 6: Metadata Self-Healing & Closed-Loop Knowledge Co-Creation
> **User**: "Please note: our company officially defines 'VIP Customer' as customers with yearly income strictly above $100,000 (dimcustomer.yearlyincome > 100000). Record this into our business terms permanently."
>
> **Agent Execution Logic**:
> 1. Calls `upsert_business_term` to register `VIP Customer` under subject `Retail Sales Analytics`;
> 2. DatI logs an audit record, persists the definition, and hot-reloads the dictionary.
>
> **Agent Response**:
> "✅ Business term registered successfully! `VIP Customer` (`dimcustomer.yearlyincome > 100000`) is now active under 【Retail Sales Analytics】. All future team queries mentioning VIP customers will apply this standard automatically."

---

### Scenario 7: Multi-Source Synthesis & Visual Charting
> **User**: "Give me a comprehensive performance review for the first half of 2026:
> 1. Check the sales and profit trend for our core category (Bikes) across Q1 and Q2;
> 2. Check the global sales distribution and revenue share across the 3 continents (North America, Europe, Pacific);
> 3. Provide a brief attribution analysis and visualize the findings with charts (Mermaid quarterly trend & continental pie chart)."
>
> **Agent Execution Logic**:
> 1. Calls `get_category_quarterly_trend` for `Bikes` in 2026;
> 2. Calls `get_regional_sales_breakdown` to aggregate continental totals;
> 3. Synthesizes drivers and renders 2 native Mermaid diagrams.
>
> **Agent Response**:
>
> # 🚴 AdventureWorks H1 2026 Executive Review
>
> ### 1. Core Category (Bikes) Quarterly Growth
> - **2026 Q1**: 2,492 orders, **\$3.99M** sales, **\$1.62M** net profit (40.50% margin)
> - **2026 Q2**: 3,298 orders, **\$5.14M** sales, **\$2.08M** net profit (40.55% margin)
> - **QoQ Surge**: Sales surged **+28.47%** heading into peak summer riding season with steady 40.5% margins.
>
> ```mermaid
> xychart-beta
>     title "H1 2026 Bikes Revenue & Profit Trend (in $M)"
>     x-axis ["2026 Q1", "2026 Q2"]
>     y-axis "USD ($M)" 0 --> 6
>     bar [4.00, 5.14]
>     line [1.62, 2.08]
> ```
>
> ### 2. Continental Sales Contribution
> - **North America**: **\$3.99M (41.0%)** – Flagship market led by Southwest and Northwest regions.
> - **Europe**: **\$3.20M (32.8%)** – Strong momentum driven by UK and Germany.
> - **Pacific**: **\$2.55M (26.2%)** – Australia delivers massive individual market share with highest AOV (\$895).
>
> ```mermaid
> pie title 2026 Continental Sales Distribution (GMV Share)
>     "North America (41.0%)" : 3991475
>     "Europe (32.8%)" : 3199230
>     "Pacific (26.2%)" : 2552488
> ```
>
> ### 3. Attributions & Recommendations
> - **Attachment Gap**: Q2 gains were heavily driven by complete bikes, while accessory attachments (clothing, helmets) remain below 8%. Recommendation: Launch automatic helmet/lock bundles during summer checkout to lift basket size.
