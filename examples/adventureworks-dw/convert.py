#!/usr/bin/env python3
"""
Convert mysql-adventure-works-dw dataset to current timeframe (latest year = 2026).
- Shifts dates by +22 years (2001-2004 -> 2023-2026).
- Accurately recalculates DimTime calendar attributes.
- Normalizes table names to match DDL case (avoids Linux MySQL case issues).
- Strips hardcoded database creation ('use aw;', etc.) to allow arbitrary target db.
- Produces both individual cleaned scripts and a single consolidated aw_dw_2026.sql (.gz).
"""

import os
import sys
import re
import calendar
import gzip
import tempfile
import subprocess
import argparse
from datetime import date, timedelta

UPSTREAM_REPO = "https://github.com/patgruber/mysql-adventure-works-dw.git"
OUT_DIR = os.path.dirname(os.path.abspath(__file__))

MONTHS = {
    'JAN': 1, 'FEB': 2, 'MAR': 3, 'APR': 4, 'MAY': 5, 'JUN': 6,
    'JUL': 7, 'AUG': 8, 'SEP': 9, 'OCT': 10, 'NOV': 11, 'DEC': 12
}

SPANISH_DAYS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado']
FRENCH_DAYS = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi']
SPANISH_MONTHS = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']
FRENCH_MONTHS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']

def is_leap_year(year: int) -> bool:
    return (year % 4 == 0 and year % 100 != 0) or (year % 400 == 0)

def safe_date(year: int, month: int, day: int) -> date:
    if month == 2 and day == 29 and not is_leap_year(year):
        day = 28
    return date(year, month, day)

def process_ddl():
    print("Processing DDL...")
    with open(os.path.join(SRC_DIR, "creates/create-database-tables.sql")) as f:
        content = f.read()
    
    # Remove drop/create database aw
    content = re.sub(r'drop database if exists aw;\s*', '', content, flags=re.IGNORECASE)
    content = re.sub(r'create database aw;\s*', '', content, flags=re.IGNORECASE)
    content = re.sub(r'use aw;\s*', '', content, flags=re.IGNORECASE)
    
    header = "-- AdventureWorks Data Warehouse (2026 Time Shifted Edition)\n"
    header += "-- Compatible with MySQL 5.7+ and 8.0+\n\n"
    return header + content.strip() + "\n\n"

def process_dim_time():
    print("Regenerating DimTime (2023-07-01 to 2026-08-31, 1158 days)...")
    start_d = date(2023, 7, 1)
    end_d = date(2026, 8, 31)
    
    lines = [
        "-- DimTime (Time-shifted to 2023-2026)",
        "TRUNCATE TABLE DimTime;",
        "BEGIN;"
    ]
    
    curr = start_d
    time_key = 1
    while curr <= end_d:
        day_of_week_num = (curr.weekday() + 1) % 7 + 1  # 1=Sunday, 2=Monday, ..., 7=Saturday
        day_idx = (curr.weekday() + 1) % 7
        
        eng_day = curr.strftime('%A')
        esp_day = SPANISH_DAYS[day_idx]
        fra_day = FRENCH_DAYS[day_idx]
        
        day_of_month = curr.day
        day_of_year = curr.timetuple().tm_yday
        week_of_year = (day_of_year - 1) // 7 + 1
        
        eng_month = calendar.month_name[curr.month]
        esp_month = SPANISH_MONTHS[curr.month - 1]
        fra_month = FRENCH_MONTHS[curr.month - 1]
        month_of_year = curr.month
        
        cal_quarter = (curr.month - 1) // 3 + 1
        cal_year = str(curr.year)
        cal_semester = 1 if curr.month <= 6 else 2
        
        # AdventureWorks fiscal year begins July 1:
        # July-Sept = Q1, Oct-Dec = Q2, Jan-Mar = Q3, Apr-Jun = Q4
        fiscal_quarter = ((curr.month + 5) % 12) // 3 + 1
        fiscal_year = str(curr.year + 1) if curr.month >= 7 else str(curr.year)
        fiscal_semester = 1 if curr.month in [7, 8, 9, 10, 11, 12] else 2
        
        date_str = curr.strftime('%Y-%m-%d 12:00:00')
        
        sql = (
            f"INSERT INTO DimTime (TimeKey, FullDateAlternateKey, DayNumberOfWeek, EnglishDayNameOfWeek, "
            f"SpanishDayNameOfWeek, FrenchDayNameOfWeek, DayNumberOfMonth, DayNumberOfYear, WeekNumberOfYear, "
            f"EnglishMonthName, SpanishMonthName, FrenchMonthName, MonthNumberOfYear, CalendarQuarter, "
            f"CalendarYear, CalendarSemester, FiscalQuarter, FiscalYear, FiscalSemester) VALUES "
            f"({time_key}, '{date_str}', {day_of_week_num}, '{eng_day}', '{esp_day}', '{fra_day}', "
            f"{day_of_month}, {day_of_year}, {week_of_year}, '{eng_month}', '{esp_month}', '{fra_month}', "
            f"{month_of_year}, {cal_quarter}, '{cal_year}', {cal_semester}, {fiscal_quarter}, '{fiscal_year}', {fiscal_semester});"
        )
        lines.append(sql)
        
        curr += timedelta(days=1)
        time_key += 1
        
    lines.append("COMMIT;\n")
    return "\n".join(lines) + "\n"

def process_dim_customer():
    print("Processing DimCustomer (shifting BirthDate and DateFirstPurchase +22 years)...")
    birth_pattern = re.compile(r"timestamp\(date_add\(str_to_date\('(\d{2})-([A-Z]{3})-(\d{2}) 12:00:00','%d-%b-%y %k:%i:%s'\), INTERVAL -100 YEAR\)\)")
    purchase_pattern = re.compile(r"timestamp\(str_to_date\('(\d{2})-([A-Z]{3})-(\d{2}) 12:00:00','%d-%b-%y %k:%i:%s'\)\)")
    
    def shift_birth(m):
        day, mon, yr = int(m.group(1)), MONTHS[m.group(2)], 1900 + int(m.group(3)) + 22
        d = safe_date(yr, mon, day)
        return f"'{d.year:04d}-{d.month:02d}-{d.day:02d} 12:00:00'"

    def shift_purchase(m):
        day, mon, yr = int(m.group(1)), MONTHS[m.group(2)], 2000 + int(m.group(3)) + 22
        d = safe_date(yr, mon, day)
        return f"'{d.year:04d}-{d.month:02d}-{d.day:02d} 12:00:00'"

    out_lines = []
    with open(os.path.join(SRC_DIR, "inserts/insert-DimCustomer.sql")) as f:
        for line in f:
            if line.strip().lower() == 'use aw;':
                continue
            if 'truncate table' in line.lower():
                out_lines.append("TRUNCATE TABLE DimCustomer;\n")
                continue
            if line.startswith('Insert into DIMCUSTOMER'):
                line = 'INSERT INTO DimCustomer' + line[len('Insert into DIMCUSTOMER'):]
                line = birth_pattern.sub(shift_birth, line)
                line = purchase_pattern.sub(shift_purchase, line)
            out_lines.append(line)
            
    return "".join(out_lines) + "\n"

def process_dim_product():
    print("Processing DimProduct (shifting StartDate/EndDate +22 years)...")
    date_pattern = re.compile(r"timestamp\('(\d{4})-(\d{2})-(\d{2}) 12:00:00'\)")
    
    def shift_prod_date(m):
        yr = int(m.group(1)) + 22
        mon = int(m.group(2))
        day = int(m.group(3))
        d = safe_date(yr, mon, day)
        return f"'{d.year:04d}-{d.month:02d}-{d.day:02d} 12:00:00'"

    out_lines = []
    with open(os.path.join(SRC_DIR, "inserts/insert-DimProduct.sql")) as f:
        for line in f:
            if line.strip().lower() == 'use aw;':
                continue
            if 'truncate table' in line.lower():
                out_lines.append("TRUNCATE TABLE DimProduct;\n")
                continue
            if line.startswith('Insert into DIMPRODUCT'):
                line = 'INSERT INTO DimProduct' + line[len('Insert into DIMPRODUCT'):]
                line = date_pattern.sub(shift_prod_date, line)
            out_lines.append(line)
            
    return "".join(out_lines) + "\n"

def process_dim_promotion():
    print("Processing DimPromotion (shifting dates +22 years)...")
    date_str_pattern = re.compile(r"'(200[1-4])(\d{4})'")
    
    def shift_promo_date(m):
        yr = int(m.group(1)) + 22
        return f"'{yr}{m.group(2)}'"

    out_lines = []
    with open(os.path.join(SRC_DIR, "inserts/insert-DimPromotion.sql")) as f:
        for line in f:
            if line.strip().lower() == 'use aw;':
                continue
            if 'truncate table' in line.lower():
                out_lines.append("TRUNCATE TABLE DimPromotion;\n")
                continue
            line = date_str_pattern.sub(shift_promo_date, line)
            line = line.replace('Discount-2002', 'Discount-2024')
            line = line.replace('Discount-2003', 'Discount-2025')
            out_lines.append(line)
            
    return "".join(out_lines) + "\n"

def process_dim_reseller():
    print("Processing DimReseller (shifting FirstOrderYear/LastOrderYear +22 years)...")
    out_lines = []
    with open(os.path.join(SRC_DIR, "inserts/insert-DimReseller.sql")) as f:
        for line in f:
            if line.strip().lower() == 'use aw;':
                continue
            if 'truncate table' in line.lower():
                out_lines.append("TRUNCATE TABLE DimReseller;\n")
                continue
            line = re.sub(r',\s*2001\s*,', ', 2023,', line)
            line = re.sub(r',\s*2002\s*,', ', 2024,', line)
            line = re.sub(r',\s*2003\s*,', ', 2025,', line)
            line = re.sub(r',\s*2004\s*,', ', 2026,', line)
            out_lines.append(line)
            
    return "".join(out_lines) + "\n"

def process_dim_employee():
    print("Processing DimEmployee (shifting dates +22 years)...")
    date_pattern = re.compile(r"(?<!N)'((?:19\d{2}|200\d))(\d{2})(\d{2})'")
    
    def shift_emp_date(m):
        yr = int(m.group(1)) + 22
        mon = int(m.group(2))
        day = int(m.group(3))
        if not (1 <= mon <= 12 and 1 <= day <= 31):
            return m.group(0)
        d = safe_date(yr, mon, day)
        return f"'{d.year:04d}{d.month:02d}{d.day:02d}'"

    out_lines = []
    with open(os.path.join(SRC_DIR, "inserts/insert-DimEmployee.sql")) as f:
        for line in f:
            if line.strip().lower() == 'use aw;':
                continue
            if 'truncate table' in line.lower():
                out_lines.append("TRUNCATE TABLE DimEmployee;\n")
                continue
            line = date_pattern.sub(shift_emp_date, line)
            out_lines.append(line)
            
    return "".join(out_lines) + "\n"

def process_simple_file(filename: str, table_name: str):
    print(f"Processing {table_name}...")
    out_lines = []
    with open(os.path.join(SRC_DIR, f"inserts/{filename}")) as f:
        for line in f:
            if line.strip().lower() == 'use aw;':
                continue
            if 'truncate' in line.lower():
                out_lines.append(f"TRUNCATE TABLE {table_name};\n")
                continue
            if line.startswith(f"Insert into {table_name.upper()}"):
                line = f"INSERT INTO {table_name}" + line[len(f"Insert into {table_name.upper()}"):]
            out_lines.append(line)
    return "".join(out_lines) + "\n"

def process_constraints():
    print("Processing constraints...")
    with open(os.path.join(SRC_DIR, "creates/add-constraints.sql")) as f:
        content = f.read()
    content = re.sub(r'use aw;\s*', '', content, flags=re.IGNORECASE)
    return "\n-- Foreign Key Constraints\n" + content.strip() + "\n"

def main():
    global SRC_DIR
    tmp_dir_obj = None
    if len(sys.argv) > 1 and os.path.isdir(sys.argv[1]):
        SRC_DIR = sys.argv[1]
        print(f"Using local source directory: {SRC_DIR}")
    else:
        tmp_dir_obj = tempfile.TemporaryDirectory()
        SRC_DIR = tmp_dir_obj.name
        print(f"Cloning {UPSTREAM_REPO} to temporary workspace...")
        subprocess.run(["git", "clone", "--depth", "1", UPSTREAM_REPO, SRC_DIR], check=True)

    try:
        _run_conversion()
    finally:
        if tmp_dir_obj:
            tmp_dir_obj.cleanup()

def _run_conversion():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(os.path.join(OUT_DIR, "sql"), exist_ok=True)
    
    parts = []
    
    # 0. Safety setup
    parts.append("SET FOREIGN_KEY_CHECKS = 0;\nSET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';\n\n")
    
    # 1. DDL
    ddl_sql = process_ddl()
    parts.append(ddl_sql)
    with open(os.path.join(OUT_DIR, "sql/00_create_tables.sql"), "w") as f:
        f.write(ddl_sql)
        
    # 2. Dimensions
    simple_dims = [
        ("insert-DimAccount.sql", "DimAccount"),
        ("insert-DimCurrency.sql", "DimCurrency"),
        ("insert-DimDepartmentGroup.sql", "DimDepartmentGroup"),
        ("insert-DimGeography.sql", "DimGeography"),
        ("insert-DimOrganization.sql", "DimOrganization"),
        ("insert-DimProductCategory.sql", "DimProductCategory"),
        ("insert-DimProductSubcategory.sql", "DimProductSubcategory"),
        ("insert-DimSalesReason.sql", "DimSalesReason"),
        ("insert-DimSalesTerritory.sql", "DimSalesTerritory"),
        ("insert-DimScenario.sql", "DimScenario"),
    ]
    
    for fn, tname in simple_dims:
        content = process_simple_file(fn, tname)
        parts.append(content)
        with open(os.path.join(OUT_DIR, f"sql/10_{tname}.sql"), "w") as f:
            f.write(content)
            
    # Specialized dims with date logic
    dim_time_sql = process_dim_time()
    parts.append(dim_time_sql)
    with open(os.path.join(OUT_DIR, "sql/11_DimTime.sql"), "w") as f:
        f.write(dim_time_sql)
        
    dim_cust_sql = process_dim_customer()
    parts.append(dim_cust_sql)
    with open(os.path.join(OUT_DIR, "sql/12_DimCustomer.sql"), "w") as f:
        f.write(dim_cust_sql)
        
    dim_prod_sql = process_dim_product()
    parts.append(dim_prod_sql)
    with open(os.path.join(OUT_DIR, "sql/13_DimProduct.sql"), "w") as f:
        f.write(dim_prod_sql)
        
    dim_promo_sql = process_dim_promotion()
    parts.append(dim_promo_sql)
    with open(os.path.join(OUT_DIR, "sql/14_DimPromotion.sql"), "w") as f:
        f.write(dim_promo_sql)
        
    dim_reseller_sql = process_dim_reseller()
    parts.append(dim_reseller_sql)
    with open(os.path.join(OUT_DIR, "sql/15_DimReseller.sql"), "w") as f:
        f.write(dim_reseller_sql)
        
    dim_emp_sql = process_dim_employee()
    parts.append(dim_emp_sql)
    with open(os.path.join(OUT_DIR, "sql/16_DimEmployee.sql"), "w") as f:
        f.write(dim_emp_sql)
        
    # Facts
    fact_finance_sql = process_simple_file("insert-FactFinance.sql", "FactFinance")
    parts.append(fact_finance_sql)
    with open(os.path.join(OUT_DIR, "sql/20_FactFinance.sql"), "w") as f:
        f.write(fact_finance_sql)
        
    fact_sales_sql = process_simple_file("insert-FactInternetSales.sql", "FactInternetSales")
    parts.append(fact_sales_sql)
    with open(os.path.join(OUT_DIR, "sql/21_FactInternetSales.sql"), "w") as f:
        f.write(fact_sales_sql)
        
    # Constraints
    constraints_sql = process_constraints()
    parts.append(constraints_sql)
    parts.append("\nSET FOREIGN_KEY_CHECKS = 1;\n")
    with open(os.path.join(OUT_DIR, "sql/30_add_constraints.sql"), "w") as f:
        f.write(constraints_sql + "\nSET FOREIGN_KEY_CHECKS = 1;\n")
        
    # Combine everything into single aw_dw_2026.sql
    combined_path = os.path.join(OUT_DIR, "aw_dw_2026.sql")
    print(f"Writing single consolidated bundle to {combined_path} ...")
    with open(combined_path, "w", encoding="utf-8") as f:
        for part in parts:
            f.write(part)
            
    # Gzip version
    gz_path = os.path.join(OUT_DIR, "aw_dw_2026.sql.gz")
    print(f"Writing compressed bundle to {gz_path} ...")
    with open(combined_path, "rb") as f_in:
        with gzip.open(gz_path, "wb") as f_out:
            f_out.writelines(f_in)
            
    sql_size_mb = os.path.getsize(combined_path) / (1024 * 1024)
    gz_size_mb = os.path.getsize(gz_path) / (1024 * 1024)
    print(f"Done! aw_dw_2026.sql: {sql_size_mb:.2f} MB, aw_dw_2026.sql.gz: {gz_size_mb:.2f} MB")
    
    return combined_path

def import_to_db(sql_path, args):
    print(f"Directly importing generated data into MySQL ({args.host}:{args.port}/{args.database})...")
    try:
        import pymysql
        conn = pymysql.connect(
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password,
            database=args.database,
            charset='utf8mb4',
            client_flag=pymysql.constants.CLIENT.MULTI_STATEMENTS
        )
        with open(sql_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        with conn.cursor() as cur:
            cur.execute(sql_content)
        conn.commit()
        conn.close()
        print("Import completed successfully via pymysql!")
    except ImportError:
        cmd = ["mysql", "-h", args.host, "-P", str(args.port), "-u", args.user]
        if args.password:
            cmd.append(f"-p{args.password}")
        cmd.append(args.database)
        with open(sql_path, "rb") as f:
            subprocess.run(cmd, stdin=f, check=True)
        print("Import completed successfully via mysql CLI!")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert AdventureWorks DW to 2026 and optionally import to MySQL")
    parser.add_argument("--host", help="MySQL host to import to (optional)")
    parser.add_argument("--port", type=int, default=3306, help="MySQL port (default: 3306)")
    parser.add_argument("--user", help="MySQL username")
    parser.add_argument("--password", help="MySQL password")
    parser.add_argument("--database", help="MySQL database name")
    args = parser.parse_args()

    combined_file = main()
    if args.host:
        if not args.user or not args.database:
            print("Error: --user and --database are required when --host is provided.")
            sys.exit(1)
        import_to_db(combined_file, args)
