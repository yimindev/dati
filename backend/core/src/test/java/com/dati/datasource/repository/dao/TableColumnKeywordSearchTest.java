package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Table/Column keyword search tests")
class TableColumnKeywordSearchTest {

    private static final PageRequest PAGE = PageRequest.of(0, 10);

    @Autowired
    private TableInfoDAO tableInfoDAO;

    @Autowired
    private ColumnInfoDAO columnInfoDAO;

    private TableInfoPO table(String dataSourceId, String name, List<String> aliases) {
        TableInfoPO po = new TableInfoPO();
        po.setDataSourceId(dataSourceId);
        po.setSchema("public");
        po.setName(name);
        po.setAliases(aliases);
        po.setCreatedBy("u1");
        return po;
    }

    private ColumnInfoPO column(String tableId, String name, List<String> aliases) {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setTableId(tableId);
        po.setName(name);
        po.setColumnType("VARCHAR");
        po.setAliases(aliases);
        po.setCreatedBy("u1");
        return po;
    }

    @Test
    @DisplayName("table keyword matches name and aliases")
    void tableKeyword_matchesNameAndAliases() {
        String dsId = "ds-1";
        tableInfoDAO.save(table(dsId, "customer", List.of("客户", "buyer")));
        tableInfoDAO.save(table(dsId, "orders", List.of()));

        assertThat(searchTables(dsId, "cust")).containsExactly("customer");
        assertThat(searchTables(dsId, "客户")).containsExactly("customer");
        assertThat(searchTables(dsId, "buyer")).containsExactly("customer");
    }

    @Test
    @DisplayName("table keyword treats % and _ literally")
    void tableKeyword_treatsWildcardsLiterally() {
        String dsId = "ds-1";
        tableInfoDAO.save(table(dsId, "order_items", List.of()));
        tableInfoDAO.save(table(dsId, "orders", List.of()));

        assertThat(searchTables(dsId, "%")).isEmpty();
        assertThat(searchTables(dsId, "_")).containsExactly("order_items");
    }

    @Test
    @DisplayName("column keyword matches name and aliases")
    void columnKeyword_matchesNameAndAliases() {
        String tableId = "table-1";
        columnInfoDAO.save(column(tableId, "total_amount", List.of("订单金额", "amount")));
        columnInfoDAO.save(column(tableId, "status", List.of()));

        assertThat(searchColumns(tableId, "total")).containsExactly("total_amount");
        assertThat(searchColumns(tableId, "订单")).containsExactly("total_amount");
        assertThat(searchColumns(tableId, "amount")).containsExactly("total_amount");
    }

    @Test
    @DisplayName("column keyword treats % and _ literally")
    void columnKeyword_treatsWildcardsLiterally() {
        String tableId = "table-1";
        columnInfoDAO.save(column(tableId, "unit_price", List.of()));
        columnInfoDAO.save(column(tableId, "status", List.of()));

        assertThat(searchColumns(tableId, "%")).isEmpty();
        assertThat(searchColumns(tableId, "_")).containsExactly("unit_price");
    }

    private List<String> searchTables(String dataSourceId, String keyword) {
        Page<TableInfoPO> page = tableInfoDAO.findByDataSourceIdAndKeyword(dataSourceId, keyword, PAGE);
        return page.getContent().stream().map(TableInfoPO::getName).toList();
    }

    private List<String> searchColumns(String tableId, String keyword) {
        Page<ColumnInfoPO> page = columnInfoDAO.findByTableIdAndKeyword(tableId, keyword, PAGE);
        return page.getContent().stream().map(ColumnInfoPO::getName).toList();
    }
}
