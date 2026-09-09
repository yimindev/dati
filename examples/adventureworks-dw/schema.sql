-- AdventureWorks Data Warehouse (Kimball Star Schema)
-- MySQL 8.0+ Compatible DDL & Base Reference Data
-- Includes all 19 DW tables and baseline dictionary records

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================================
-- Table Structures (DDL)
-- ========================================================

CREATE TABLE IF NOT EXISTS `dimaccount` (
  `AccountKey` int NOT NULL,
  `ParentAccountKey` int DEFAULT NULL,
  `AccountCodeAlternateKey` int DEFAULT NULL,
  `ParentAccountCodeAlternateKey` int DEFAULT NULL,
  `AccountDescription` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AccountType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Operator` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomMembers` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ValueType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomMemberOptions` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`AccountKey`),
  KEY `FK_DimAccount_DimAccount` (`ParentAccountKey`),
  CONSTRAINT `FK_DimAccount_DimAccount` FOREIGN KEY (`ParentAccountKey`) REFERENCES `dimaccount` (`AccountKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimcurrency` (
  `CurrencyKey` int NOT NULL,
  `CurrencyAlternateKey` char(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CurrencyName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`CurrencyKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimcustomer` (
  `CustomerKey` int NOT NULL,
  `GeographyKey` int DEFAULT NULL,
  `CustomerAlternateKey` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Title` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FirstName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MiddleName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LastName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NameStyle` bigint DEFAULT NULL,
  `BirthDate` datetime DEFAULT NULL,
  `MaritalStatus` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Suffix` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Gender` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EmailAddress` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `YearlyIncome` decimal(22,2) DEFAULT NULL,
  `TotalChildren` bigint DEFAULT NULL,
  `NumberChildrenAtHome` bigint DEFAULT NULL,
  `EnglishEducation` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EnglishOccupation` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HouseOwnerFlag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NumberCarsOwned` bigint DEFAULT NULL,
  `AddressLine1` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AddressLine2` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DateFirstPurchase` timestamp NULL DEFAULT NULL,
  `CommuteDistance` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`CustomerKey`),
  KEY `FK_DimCustomer_DimGeography` (`GeographyKey`),
  CONSTRAINT `FK_DimCustomer_DimGeography` FOREIGN KEY (`GeographyKey`) REFERENCES `dimgeography` (`GeographyKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimdepartmentgroup` (
  `DepartmentGroupKey` int NOT NULL,
  `ParentDepartmentGroupKey` int DEFAULT NULL,
  `DepartmentGroupName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`DepartmentGroupKey`),
  KEY `FK_DimDeptGroup_DimDeptGroup` (`ParentDepartmentGroupKey`),
  CONSTRAINT `FK_DimDeptGroup_DimDeptGroup` FOREIGN KEY (`ParentDepartmentGroupKey`) REFERENCES `dimdepartmentgroup` (`DepartmentGroupKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimemployee` (
  `EmployeeKey` int NOT NULL,
  `ParentEmployeeKey` int DEFAULT NULL,
  `EmployeeNationalIDAlternateKey` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ParentEmployeeNationalIDAltKey` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SalesTerritoryKey` int DEFAULT NULL,
  `FirstName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `LastName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MiddleName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NameStyle` bigint NOT NULL,
  `Title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HireDate` bigint DEFAULT NULL,
  `BirthDate` bigint DEFAULT NULL,
  `LoginID` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EmailAddress` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Phone` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MaritalStatus` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EmergencyContactName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EmergencyContactPhone` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SalariedFlag` bigint DEFAULT NULL,
  `Gender` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PayFrequency` bigint DEFAULT NULL,
  `BaseRate` decimal(22,5) DEFAULT NULL,
  `VacationHours` bigint DEFAULT NULL,
  `SickLeaveHours` bigint DEFAULT NULL,
  `CurrentFlag` bigint NOT NULL,
  `SalesPersonFlag` bigint NOT NULL,
  `DepartmentName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `StartDate` bigint DEFAULT NULL,
  `EndDate` bigint DEFAULT NULL,
  `Status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`EmployeeKey`),
  KEY `FK_DimEmployee_DimEmployee` (`ParentEmployeeKey`),
  KEY `FK_DimEmployee_DimSalesTerr` (`SalesTerritoryKey`),
  CONSTRAINT `FK_DimEmployee_DimEmployee` FOREIGN KEY (`ParentEmployeeKey`) REFERENCES `dimemployee` (`EmployeeKey`),
  CONSTRAINT `FK_DimEmployee_DimSalesTerr` FOREIGN KEY (`SalesTerritoryKey`) REFERENCES `dimsalesterritory` (`SalesTerritoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimgeography` (
  `GeographyKey` int NOT NULL,
  `City` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `StateProvinceCode` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `StateProvinceName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CountryRegionCode` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EnglishCountryRegionName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SpanishCountryRegionName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FrenchCountryRegionName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PostalCode` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SalesTerritoryKey` int DEFAULT NULL,
  PRIMARY KEY (`GeographyKey`),
  KEY `FK_DimGeography_DimSalesTerr` (`SalesTerritoryKey`),
  CONSTRAINT `FK_DimGeography_DimSalesTerr` FOREIGN KEY (`SalesTerritoryKey`) REFERENCES `dimsalesterritory` (`SalesTerritoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimorganization` (
  `OrganizationKey` int NOT NULL,
  `ParentOrganizationKey` int DEFAULT NULL,
  `PercentageOfOwnership` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `OrganizationName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CurrencyKey` int DEFAULT NULL,
  PRIMARY KEY (`OrganizationKey`),
  KEY `FK_DimOrg_DimCurrency` (`CurrencyKey`),
  KEY `FK_DimOrg_DimOrg` (`ParentOrganizationKey`),
  CONSTRAINT `FK_DimOrg_DimCurrency` FOREIGN KEY (`CurrencyKey`) REFERENCES `dimcurrency` (`CurrencyKey`),
  CONSTRAINT `FK_DimOrg_DimOrg` FOREIGN KEY (`ParentOrganizationKey`) REFERENCES `dimorganization` (`OrganizationKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimproduct` (
  `ProductKey` int NOT NULL,
  `ProductAlternateKey` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ProductSubcategoryKey` int DEFAULT NULL,
  `WeightUnitMeasureCode` char(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SizeUnitMeasureCode` char(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EnglishProductName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `StandardCost` decimal(22,2) DEFAULT NULL,
  `FinishedGoodsFlag` bigint NOT NULL,
  `Color` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SafetyStockLevel` bigint DEFAULT NULL,
  `ReorderPoint` bigint DEFAULT NULL,
  `ListPrice` decimal(13,2) DEFAULT NULL,
  `SizeActual` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SizeRange` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Weight` float DEFAULT NULL,
  `DaysToManufacture` int DEFAULT NULL,
  `ProductLine` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DealerPrice` decimal(22,2) DEFAULT NULL,
  `Class` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Style` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ModelName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EnglishDescription` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `StartDate` timestamp NULL DEFAULT NULL,
  `EndDate` timestamp NULL DEFAULT NULL,
  `Status` varchar(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ProductKey`),
  KEY `FK_DimProduct_DimProductSubcat` (`ProductSubcategoryKey`),
  CONSTRAINT `FK_DimProduct_DimProductSubcat` FOREIGN KEY (`ProductSubcategoryKey`) REFERENCES `dimproductsubcategory` (`ProductSubcategoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimproductcategory` (
  `ProductCategoryKey` int NOT NULL,
  `ProductCategoryAlternateKey` int DEFAULT NULL,
  `EnglishProductCategoryName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SpanishProductCategoryName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FrenchProductCategoryName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`ProductCategoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimproductsubcategory` (
  `ProductSubcategoryKey` int NOT NULL,
  `ProductSubcategoryAlternateKey` int DEFAULT NULL,
  `EnglishProductSubcategoryName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SpanishProductSubcategoryName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FrenchProductSubcategoryName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ProductCategoryKey` int DEFAULT NULL,
  PRIMARY KEY (`ProductSubcategoryKey`),
  KEY `FK_DimProdSubcat_DimProdCat` (`ProductCategoryKey`),
  CONSTRAINT `FK_DimProdSubcat_DimProdCat` FOREIGN KEY (`ProductCategoryKey`) REFERENCES `dimproductcategory` (`ProductCategoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimpromotion` (
  `PromotionKey` int NOT NULL,
  `PromotionAlternateKey` int DEFAULT NULL,
  `EnglishPromotionName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SpanishPromotionName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FrenchPromotionName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DiscountPct` float DEFAULT NULL,
  `EnglishPromotionType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SpanishPromotionType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FrenchPromotionType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EnglishPromotionCategory` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SpanishPromotionCategory` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FrenchPromotionCategory` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `StartDate` bigint NOT NULL,
  `EndDate` bigint DEFAULT NULL,
  `MinQty` int DEFAULT NULL,
  `MaxQty` int DEFAULT NULL,
  PRIMARY KEY (`PromotionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimreseller` (
  `ResellerKey` int NOT NULL,
  `GeographyKey` int DEFAULT NULL,
  `ResellerAlternateKey` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Phone` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BusinessType` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ResellerName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NumberEmployees` int DEFAULT NULL,
  `OrderFrequency` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `OrderMonth` bigint DEFAULT NULL,
  `FirstOrderYear` int DEFAULT NULL,
  `LastOrderYear` int DEFAULT NULL,
  `ProductLine` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AddressLine1` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AddressLine2` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AnnualSales` decimal(22,2) DEFAULT NULL,
  `BankName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MinPaymentType` bigint DEFAULT NULL,
  `MinPaymentAmount` decimal(22,2) DEFAULT NULL,
  `AnnualRevenue` decimal(22,2) DEFAULT NULL,
  `YearOpened` int DEFAULT NULL,
  PRIMARY KEY (`ResellerKey`),
  KEY `FK_DimReseller_DimGeo` (`GeographyKey`),
  CONSTRAINT `FK_DimReseller_DimGeo` FOREIGN KEY (`GeographyKey`) REFERENCES `dimgeography` (`GeographyKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimsalesreason` (
  `SalesReasonKey` int NOT NULL,
  `SalesReasonAlternateKey` int NOT NULL,
  `SalesReasonName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SalesReasonReasonType` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SalesReasonKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimsalesterritory` (
  `SalesTerritoryKey` int NOT NULL,
  `SalesTerritoryAlternateKey` int DEFAULT NULL,
  `SalesTerritoryRegion` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SalesTerritoryCountry` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SalesTerritoryGroup` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`SalesTerritoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimscenario` (
  `ScenarioKey` int NOT NULL,
  `ScenarioName` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ScenarioKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dimtime` (
  `TimeKey` int NOT NULL,
  `FullDateAlternateKey` timestamp NULL DEFAULT NULL,
  `DayNumberOfWeek` bigint DEFAULT NULL,
  `EnglishDayNameOfWeek` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SpanishDayNameOfWeek` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FrenchDayNameOfWeek` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DayNumberOfMonth` bigint DEFAULT NULL,
  `DayNumberOfYear` bigint DEFAULT NULL,
  `WeekNumberOfYear` bigint DEFAULT NULL,
  `EnglishMonthName` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SpanishMonthName` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FrenchMonthName` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MonthNumberOfYear` bigint DEFAULT NULL,
  `CalendarQuarter` bigint DEFAULT NULL,
  `CalendarYear` char(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CalendarSemester` bigint DEFAULT NULL,
  `FiscalQuarter` bigint DEFAULT NULL,
  `FiscalYear` char(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FiscalSemester` bigint DEFAULT NULL,
  PRIMARY KEY (`TimeKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `factcurrencyrate` (
  `CurrencyKey` int NOT NULL,
  `TimeKey` int NOT NULL,
  `AverageRate` float NOT NULL,
  `EndOfDayRate` float NOT NULL,
  KEY `FK_FactCurrRate_DimCurr` (`CurrencyKey`),
  KEY `FK_FactCurrRate_DimTime` (`TimeKey`),
  CONSTRAINT `FK_FactCurrRate_DimCurr` FOREIGN KEY (`CurrencyKey`) REFERENCES `dimcurrency` (`CurrencyKey`),
  CONSTRAINT `FK_FactCurrRate_DimTime` FOREIGN KEY (`TimeKey`) REFERENCES `dimtime` (`TimeKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `factfinance` (
  `TimeKey` int DEFAULT NULL,
  `OrganizationKey` int DEFAULT NULL,
  `DepartmentGroupKey` int DEFAULT NULL,
  `ScenarioKey` int DEFAULT NULL,
  `AccountKey` int DEFAULT NULL,
  `Amount` float DEFAULT NULL,
  KEY `FK_FactFinance_DimAccount` (`AccountKey`),
  KEY `FK_FactFinance_DimDeptGroup` (`DepartmentGroupKey`),
  KEY `FK_FactFinance_DimOrg` (`OrganizationKey`),
  KEY `FK_FactFinance_DimScenario` (`ScenarioKey`),
  KEY `FK_FactFinance_DimTime` (`TimeKey`),
  CONSTRAINT `FK_FactFinance_DimAccount` FOREIGN KEY (`AccountKey`) REFERENCES `dimaccount` (`AccountKey`),
  CONSTRAINT `FK_FactFinance_DimDeptGroup` FOREIGN KEY (`DepartmentGroupKey`) REFERENCES `dimdepartmentgroup` (`DepartmentGroupKey`),
  CONSTRAINT `FK_FactFinance_DimOrg` FOREIGN KEY (`OrganizationKey`) REFERENCES `dimorganization` (`OrganizationKey`),
  CONSTRAINT `FK_FactFinance_DimScenario` FOREIGN KEY (`ScenarioKey`) REFERENCES `dimscenario` (`ScenarioKey`),
  CONSTRAINT `FK_FactFinance_DimTime` FOREIGN KEY (`TimeKey`) REFERENCES `dimtime` (`TimeKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `factinternetsales` (
  `ProductKey` int NOT NULL,
  `OrderDateKey` int NOT NULL,
  `DueDateKey` int NOT NULL,
  `ShipDateKey` int NOT NULL,
  `CustomerKey` int NOT NULL,
  `PromotionKey` int NOT NULL,
  `CurrencyKey` int NOT NULL,
  `SalesTerritoryKey` int NOT NULL,
  `SalesOrderNumber` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SalesOrderLineNumber` bigint NOT NULL,
  `RevisionNumber` bigint DEFAULT NULL,
  `OrderQuantity` bigint DEFAULT NULL,
  `UnitPrice` decimal(22,2) DEFAULT NULL,
  `ExtendedAmount` decimal(22,2) DEFAULT NULL,
  `UnitPriceDiscountPct` float DEFAULT NULL,
  `DiscountAmount` float DEFAULT NULL,
  `ProductStandardCost` decimal(22,2) DEFAULT NULL,
  `TotalProductCost` decimal(22,2) DEFAULT NULL,
  `SalesAmount` decimal(22,2) DEFAULT NULL,
  `TaxAmt` decimal(22,2) DEFAULT NULL,
  `Freight` decimal(22,2) DEFAULT NULL,
  `CarrierTrackingNumber` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CustomerPONumber` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  KEY `FK_FactInternetSales_DimCurr` (`CurrencyKey`),
  KEY `FK_FactInternetSales_DimCust` (`CustomerKey`),
  KEY `FK_FactInternetSales_DimProd` (`ProductKey`),
  KEY `FK_FactInternetSales_DimPromo` (`PromotionKey`),
  KEY `FK_FactNetSales_DimSalesTerr` (`SalesTerritoryKey`),
  KEY `FK_FactInternetSales_DimTime` (`OrderDateKey`),
  CONSTRAINT `FK_FactInternetSales_DimCurr` FOREIGN KEY (`CurrencyKey`) REFERENCES `dimcurrency` (`CurrencyKey`),
  CONSTRAINT `FK_FactInternetSales_DimCust` FOREIGN KEY (`CustomerKey`) REFERENCES `dimcustomer` (`CustomerKey`),
  CONSTRAINT `FK_FactInternetSales_DimProd` FOREIGN KEY (`ProductKey`) REFERENCES `dimproduct` (`ProductKey`),
  CONSTRAINT `FK_FactInternetSales_DimPromo` FOREIGN KEY (`PromotionKey`) REFERENCES `dimpromotion` (`PromotionKey`),
  CONSTRAINT `FK_FactInternetSales_DimTime` FOREIGN KEY (`OrderDateKey`) REFERENCES `dimtime` (`TimeKey`),
  CONSTRAINT `FK_FactNetSales_DimSalesTerr` FOREIGN KEY (`SalesTerritoryKey`) REFERENCES `dimsalesterritory` (`SalesTerritoryKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- Base Reference & Dictionary Data
-- ========================================================

-- Base Reference Data: dimproductcategory (4 rows)
INSERT INTO `dimproductcategory` (`ProductCategoryKey`, `ProductCategoryAlternateKey`, `EnglishProductCategoryName`, `SpanishProductCategoryName`, `FrenchProductCategoryName`) VALUES
(1, 1, 'Bikes', 'Bicicleta', 'Vélo'),
(2, 2, 'Components', 'Componente', 'Composant'),
(3, 3, 'Clothing', 'Prenda', 'Vêtements'),
(4, 4, 'Accessories', 'Accesorio', 'Accessoire');

-- Base Reference Data: dimproductsubcategory (37 rows)
INSERT INTO `dimproductsubcategory` (`ProductSubcategoryKey`, `ProductSubcategoryAlternateKey`, `EnglishProductSubcategoryName`, `SpanishProductSubcategoryName`, `FrenchProductSubcategoryName`, `ProductCategoryKey`) VALUES
(1, 1, 'Mountain Bikes', 'Bicicleta de montaña', 'VTT', 1),
(2, 2, 'Road Bikes', 'Bicicleta de carretera', 'Vélo de route', 1),
(3, 3, 'Touring Bikes', 'Bicicleta de paseo', 'Vélo de randonnée', 1),
(4, 4, 'Handlebars', 'Barra', 'Barre d\'appui', 2),
(5, 5, 'Bottom Brackets', 'Eje de pedalier', 'Axe de pédalier', 2),
(6, 6, 'Brakes', 'Frenos', 'Freins', 2),
(7, 7, 'Chains', 'Cadena', 'Chaîne', 2),
(8, 8, 'Cranksets', 'Bielas', 'Pédalier', 2),
(9, 9, 'Derailleurs', 'Desviador', 'Dérailleur', 2),
(10, 10, 'Forks', 'Horquilla', 'Fourche', 2),
(11, 11, 'Headsets', 'Dirección', 'Jeu de direction', 2),
(12, 12, 'Mountain Frames', 'Cuadro de montaña', 'Cadre de VTT', 2),
(13, 13, 'Pedals', 'Pedal', 'Pédale', 2),
(14, 14, 'Road Frames', 'Cuadro de carretera', 'Cadre de vélo de route', 2),
(15, 15, 'Saddles', 'Sillín', 'Selle', 2),
(16, 16, 'Touring Frames', 'Cuadro de paseo', 'Cadre de vélo de randonnée', 2),
(17, 17, 'Wheels', 'Rueda', 'Roue', 2),
(18, 18, 'Bib-Shorts', 'Culote corto', 'Cuissards avec bretelles', 3),
(19, 19, 'Caps', 'Gorra', 'Casquette', 3),
(20, 20, 'Gloves', 'Guantes', 'Gants', 3),
(21, 21, 'Jerseys', 'Jersey', 'Maillot', 3),
(22, 22, 'Shorts', 'Pantalones cortos', 'Cuissards', 3),
(23, 23, 'Socks', 'Calcetines', 'Chaussettes', 3),
(24, 24, 'Tights', 'Mallas', 'Collants', 3),
(25, 25, 'Vests', 'Camiseta', 'Veste', 3),
(26, 26, 'Bike Racks', 'Portabicicletas', 'Porte-vélo', 4),
(27, 27, 'Bike Stands', 'Soporte para bicicletas', 'Range-vélo', 4),
(28, 28, 'Bottles and Cages', 'Portabotellas y botella', 'Bidon et porte-bidon', 4),
(29, 29, 'Cleaners', 'Limpiador', 'Nettoyant', 4),
(30, 30, 'Fenders', 'Guardabarros', 'Garde-boue', 4),
(31, 31, 'Helmets', 'Casco', 'Casque', 4),
(32, 32, 'Hydration Packs', 'Sistema de hidratación', 'Sac d\'hydratation', 4),
(33, 33, 'Lights', 'Luz', 'Éclairage', 4),
(34, 34, 'Locks', 'Candado', 'Antivol', 4),
(35, 35, 'Panniers', 'Cesta', 'Sacoche', 4),
(36, 36, 'Pumps', 'Bomba', 'Pompe', 4),
(37, 37, 'Tires and Tubes', 'Cubierta y cámara', 'Pneu et chambre à air', 4);

-- Base Reference Data: dimsalesterritory (11 rows)
INSERT INTO `dimsalesterritory` (`SalesTerritoryKey`, `SalesTerritoryAlternateKey`, `SalesTerritoryRegion`, `SalesTerritoryCountry`, `SalesTerritoryGroup`) VALUES
(1, 1, 'Northwest', 'United States', 'North America'),
(2, 2, 'Northeast', 'United States', 'North America'),
(3, 3, 'Central', 'United States', 'North America'),
(4, 4, 'Southwest', 'United States', 'North America'),
(5, 5, 'Southeast', 'United States', 'North America'),
(6, 6, 'Canada', 'Canada', 'North America'),
(7, 7, 'France', 'France', 'Europe'),
(8, 8, 'Germany', 'Germany', 'Europe'),
(9, 9, 'Australia', 'Australia', 'Pacific'),
(10, 10, 'United Kingdom', 'United Kingdom', 'Europe'),
(11, 0, 'NA', 'NA', 'NA');

-- Base Reference Data: dimpromotion (16 rows)
INSERT INTO `dimpromotion` (`PromotionKey`, `PromotionAlternateKey`, `EnglishPromotionName`, `SpanishPromotionName`, `FrenchPromotionName`, `DiscountPct`, `EnglishPromotionType`, `SpanishPromotionType`, `FrenchPromotionType`, `EnglishPromotionCategory`, `SpanishPromotionCategory`, `FrenchPromotionCategory`, `StartDate`, `EndDate`, `MinQty`, `MaxQty`) VALUES
(1, 1, 'No Discount', 'Sin descuento', 'Aucune remise', 0.0, 'No Discount', 'Sin descuento', 'Aucune remise', 'No Discount', 'Sin descuento', 'Aucune remise', 20230601, 20261231, 0, NULL),
(2, 2, 'Volume Discount 11 to 14', 'Descuento por volumen (entre 11 y 14)', 'Remise sur quantité (de 11 à 14)', 0.02, 'Volume Discount', 'Descuento por volumen', 'Remise sur quantité', 'Reseller', 'Distribuidor', 'Revendeur', 20230701, 20260630, 11, 14),
(3, 3, 'Volume Discount 15 to 24', 'Descuento por volumen (entre 15 y 24)', 'Remise sur quantité (de 15 à 24)', 0.05, 'Volume Discount', 'Descuento por volumen', 'Remise sur quantité', 'Reseller', 'Distribuidor', 'Revendeur', 20230701, 20260630, 15, 24),
(4, 4, 'Volume Discount 25 to 40', 'Descuento por volumen (entre 25 y 40)', 'Remise sur quantité (de 25 à 40)', 0.1, 'Volume Discount', 'Descuento por volumen', 'Remise sur quantité', 'Reseller', 'Distribuidor', 'Revendeur', 20230701, 20260630, 25, 40),
(5, 5, 'Volume Discount 41 to 60', 'Descuento por volumen (entre 41 y 60)', 'Remise sur quantité (de 41 à 60)', 0.15, 'Volume Discount', 'Descuento por volumen', 'Remise sur quantité', 'Reseller', 'Distribuidor', 'Revendeur', 20230701, 20260630, 41, 60),
(6, 6, 'Volume Discount over 60', 'Descuento por volumen (más de 60)', 'Remise sur quantité (au-delà de 60)', 0.2, 'Volume Discount', 'Descuento por volumen', 'Remise sur quantité', 'Reseller', 'Distribuidor', 'Revendeur', 20230701, 20260630, 61, NULL),
(7, 7, 'Mountain-100 Clearance Sale', 'Liquidación de bicicleta de montaña, 100', 'Liquidation VTT 100', 0.35, 'Discontinued Product', 'Descatalogado', 'Ce produit n\'est plus commercialisé', 'Reseller', 'Distribuidor', 'Revendeur', 20240515, 20240630, 0, NULL),
(8, 8, 'Sport Helmet Discount-2024', 'Casco deportivo, descuento: 2002', 'Remise sur les casques sport - 2002', 0.1, 'Seasonal Discount', 'Descuento de temporada', 'Remise saisonnière', 'Reseller', 'Distribuidor', 'Revendeur', 20240701, 20240731, 0, NULL),
(9, 9, 'Road-650 Overstock', 'Bicicleta de carretera: 650, oferta especial', 'Déstockage Vélo de route 650', 0.3, 'Excess Inventory', 'Inventario excedente', 'Déstockage', 'Reseller', 'Distribuidor', 'Revendeur', 20240701, 20240831, 0, NULL),
(10, 10, 'Mountain Tire Sale', 'Oferta de cubierta de montaña', 'Vente de pneus de VTT', 0.5, 'Excess Inventory', 'Inventario excedente', 'Déstockage', 'Customer', 'Cliente', 'Client', 20250615, 20250830, 0, NULL),
(11, 11, 'Sport Helmet Discount-2025', 'Casco deportivo, descuento: 2003', 'Remise sur les casques sport - 2003', 0.15, 'Seasonal Discount', 'Descuento de temporada', 'Remise saisonnière', 'Reseller', 'Distribuidor', 'Revendeur', 20250701, 20250731, 0, NULL),
(12, 12, 'LL Road Frame Sale', 'Oferta de cuadro de carretera GB', 'Vente de cadres de vélo de route LL', 0.35, 'Excess Inventory', 'Inventario excedente', 'Déstockage', 'Reseller', 'Distribuidor', 'Revendeur', 20250701, 20250815, 0, NULL),
(13, 13, 'Touring-3000 Promotion', NULL, NULL, 0.15, 'New Product', NULL, NULL, 'Reseller', NULL, NULL, 20250701, 20250930, 0, NULL),
(14, 14, 'Touring-1000 Promotion', NULL, NULL, 0.2, 'New Product', NULL, NULL, 'Reseller', NULL, NULL, 20250701, 20250930, 0, NULL),
(15, 15, 'Half-Price Pedal Sale', NULL, NULL, 0.5, 'Seasonal Discount', NULL, NULL, 'Customer', NULL, NULL, 20250815, 20250915, 0, NULL),
(16, 16, 'Mountain-500 Silver Clearance Sale', 'Liquidación de bicicleta de montaña, 500, plateada', 'Liquidation VTT 500 argent', 0.4, 'Discontinued Product', 'Descatalogado', 'Ce produit n\'est plus commercialisé', 'Reseller', 'Distribuidor', 'Revendeur', 20260501, 20260630, 0, NULL);

-- Base Reference Data: dimscenario (3 rows)
INSERT INTO `dimscenario` (`ScenarioKey`, `ScenarioName`) VALUES
(1, 'Actual'),
(2, 'Budget'),
(3, 'Forecast');

-- Base Reference Data: dimdepartmentgroup (7 rows)
INSERT INTO `dimdepartmentgroup` (`DepartmentGroupKey`, `ParentDepartmentGroupKey`, `DepartmentGroupName`) VALUES
(1, NULL, 'Corporate'),
(2, 1, 'Executive General and Administration'),
(3, 1, 'Inventory Management'),
(4, 1, 'Manufacturing'),
(5, 1, 'Quality Assurance'),
(6, 1, 'Research and Development'),
(7, 1, 'Sales and Marketing');

SET FOREIGN_KEY_CHECKS = 1;
