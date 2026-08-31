package integration;

import java.time.Duration;
import java.util.Map;

import com.codeborne.selenide.table.classic.FlexTable;
import com.codeborne.selenide.table.classic.SelenideDataTable;
import com.codeborne.selenide.table.classic.Table;
import com.codeborne.selenide.table.model.RowConditions;
import com.codeborne.selenide.table.model.SelenideTableQuery;
import com.codeborne.selenide.table.model.TableColumnAmbiguousException;
import com.codeborne.selenide.table.model.TableColumnNotFoundException;
import com.codeborne.selenide.table.model.TableDomAdapters;
import com.codeborne.selenide.table.model.TableRowAmbiguousException;
import com.codeborne.selenide.table.model.TableRowNotFoundException;
import com.codeborne.selenide.table.model.TypedTableCellRef;
import com.codeborne.selenide.table.model.TypedTableColumnRef;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableQueryContractTest extends TableContractTestSupport {

  private enum Header {
    COUNTRY("Country"),
    COMPANY("Company"),
    EMPLOYEES("Employees"),
    MISSING("Missing");

    private final String displayed;

    Header(String displayed) {
      this.displayed = displayed;
    }
  }

  private FixturePage page;

  @BeforeEach
  public void openFixture() {
    openQueriesFixture();
    page = driver().page(FixturePage.class);
  }

  @Test
  public void addressesClassicTableByIndexAndTypedKey() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);
    TypedTableCellRef<Header> company = query.row(0).requiredCell(Header.COMPANY);

    Assertions.assertThat(query.firstRow().cell(0).text()).isEqualTo("Austria");
    Assertions.assertThat(query.lastRow().requiredCell(Header.COMPANY).text()).isEqualTo("Alpine");
    Assertions.assertThat(query.cell(1, 1).text()).isEqualTo("Berglunds");
    Assertions.assertThat(company.rowIndex()).isZero();
    Assertions.assertThat(company.columnIndex()).isEqualTo(1);
    Assertions.assertThat(query.column(1).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
    Assertions.assertThat(query.column(Header.COMPANY).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");

    driver().executeJavaScript("window.remountQueryClassic()");
    Assertions.assertThat(company.text()).isEqualTo("Alfreds");
  }

  @Test
  public void reResolvesTypedColumnAfterHeaderReorder() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);
    TypedTableColumnRef<Header> company = query.column(Header.COMPANY);

    Assertions.assertThat(company.index()).isEqualTo(1);
    driver().executeJavaScript("window.remountQueryClassicWithReorderedHeaders()");

    Assertions.assertThat(company.index()).isZero();
    Assertions.assertThat(company.cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
  }

  @Test
  public void distinguishesMountedFromVisibleRows() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.mountedRows()).hasSize(4);
    Assertions.assertThat(query.visibleRows()).hasSize(3);
    Assertions.assertThat(query.mountedRows().get(2).isVisible()).isFalse();
  }

  @Test
  public void composesConditionsAndPreservesOrder() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.findRows(RowConditions.exact(Header.COUNTRY, "Austria")))
        .extracting(row -> row.requiredCell(Header.COMPANY).text())
        .containsExactly("Alfreds", "Alpine");
    Assertions.assertThat(query.findRow(RowConditions.contains(Header.COMPANY, "glund")))
        .hasValueSatisfying(row -> Assertions.assertThat(row.index()).isEqualTo(1));
    Assertions.assertThat(query.findRows(RowConditions.regex(Header.COMPANY, "Al.*")))
        .hasSize(2);
    Assertions.assertThat(query.findRows(RowConditions.all(
        RowConditions.exact(Header.COUNTRY, "Germany"),
        RowConditions.greaterThan(Header.EMPLOYEES, 15))))
        .hasSize(1);
  }

  @Test
  public void waitsAndEnforcesUniqueRows() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);
    driver().executeJavaScript("window.prepareDelayedQueryRow()");

    Assertions.assertThat(query.findRow(RowConditions.exact(Header.COMPANY, "Berglunds"))).isEmpty();
    driver().executeJavaScript("window.restoreDelayedQueryRow()");
    driver().$("#query-classic").shouldHave(Condition.text("Berglunds"), Duration.ofSeconds(2));
    Assertions.assertThat(query.requiredRow(RowConditions.exact(Header.COMPANY, "Berglunds"),
        Duration.ofSeconds(2)).requiredCell(Header.COUNTRY).text()).isEqualTo("Germany");
    Assertions.assertThat(query.uniqueRow(RowConditions.exact(Header.COMPANY, "Berglunds"),
        Duration.ofSeconds(2)).requiredCell(Header.COMPANY).text()).isEqualTo("Berglunds");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COUNTRY, "Austria")))
        .isInstanceOf(TableRowAmbiguousException.class)
        .hasMessageContaining("2");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COMPANY, "Absent")))
        .isInstanceOf(TableRowNotFoundException.class);
  }

  @Test
  public void timedUniqueRowsReportObservedCardinality() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.uniqueRow(RowConditions.exact(Header.COMPANY, "Alfreds"),
        Duration.ofMillis(100)).requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COMPANY, "Absent"), Duration.ofMillis(100)))
        .isInstanceOf(TableRowNotFoundException.class);
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COUNTRY, "Austria"), Duration.ofMillis(100)))
        .isInstanceOf(TableRowAmbiguousException.class)
        .hasMessageContaining("2");
  }

  @Test
  public void horizontalTypedLookupWaitsAndRejectsDuplicates() {
    SelenideTableQuery<Header> query = SelenideTableQuery.of(
        page.horizontal, TableDomAdapters.horizontal(), header -> header.displayed);
    driver().executeJavaScript("window.prepareDelayedQueryHorizontalRow()");
    Assertions.assertThat(query.requiredRow(row -> row.cell(Header.COMPANY).isPresent(),
        Duration.ofSeconds(2)).requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");

    driver().executeJavaScript("window.duplicateQueryHorizontalHeader()");
    Assertions.assertThatThrownBy(() -> query.requiredRow(
            row -> row.cell(Header.COMPANY).isPresent(), Duration.ofMillis(200)))
        .isInstanceOf(com.codeborne.selenide.table.model.TableColumnAmbiguousException.class)
        .hasMessageContaining("Company");
  }

  @Test
  public void preservesRowIndexInsideNativeWait() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.requiredRow(row -> row.index() == 1, Duration.ofMillis(200)).index())
        .isEqualTo(1);
  }

  @Test
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.requiredRow(row -> {
      if (row.index() == 1) {
        driver().executeJavaScript(
            "const body = document.querySelector('#query-classic tbody');"
                + "body.prepend(body.lastElementChild);");
      }
      return row.requiredCell(Header.COMPANY).text().equals("Berglunds");
    }, Duration.ofSeconds(2)).index()).isEqualTo(1);
  }

  @Test
  public void appliesLayoutSpecificColumnSemantics() {
    SelenideTableQuery<Header> flex = page.flex.query(header -> header.displayed);
    SelenideTableQuery<Header> horizontal = SelenideTableQuery.of(
        page.horizontal, TableDomAdapters.horizontal(), header -> header.displayed);

    Assertions.assertThat(flex.column(Header.COMPANY).cells()).extracting(cell -> cell.text())
        .containsExactly("Quokkify");
    Assertions.assertThat(horizontal.column(Header.COMPANY).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds");
    Assertions.assertThat(horizontal.column(1).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds");
  }

  @Test
  public void reportsMissingAndOutOfRangeReferences() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThatThrownBy(() -> query.row(4)).isInstanceOf(IndexOutOfBoundsException.class);
    Assertions.assertThatThrownBy(() -> query.cell(0, 3).text())
        .isInstanceOf(IndexOutOfBoundsException.class);
    Assertions.assertThatThrownBy(() -> query.column(3)).isInstanceOf(IndexOutOfBoundsException.class);
    Assertions.assertThatThrownBy(() -> query.column(Header.MISSING))
        .isInstanceOf(TableColumnNotFoundException.class);
  }

  @Test
  public void supportsStringKeysOnlyWithExplicitResolver() {
    SelenideTableQuery<String> query = SelenideTableQuery.of(
        driver().$("#query-classic"), TableDomAdapters.classic(), value -> value);

    Assertions.assertThat(query.column("Company").cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
  }

  @Test
  public void supportsFindByStringFirstComponent() {
    SelenideDataTable customers = page.customers;

    Assertions.assertThat(SelenideTableQuery.byHeaderText(
        customers.getSelf(), TableDomAdapters.classic()).column("Company").cells())
        .extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
    Assertions.assertThat(customers.query().column("Company").cells())
        .extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
    Assertions.assertThat(customers.query().row(0).requiredCell("Company").text())
        .isEqualTo("Alfreds");
  }

  @Test
  public void supportsMapShortcutsAndRowConditions() {
    SelenideDataTable customers = page.customers;

    Assertions.assertThat(customers.requiredRow(Map.of("Company", "Berglunds", "Country", "Germany"))
        .requiredCell("Employees").text()).isEqualTo("20");
    customers.shouldHave(Map.of("Company", "Alfreds", "Country", "Austria"));
    customers.query().requiredRow(RowConditions.exact("Company", "Alfreds"))
        .shouldHave(SelenideDataTable.rowConditions(Map.of(
            "Company", Condition.exactTextCaseSensitive("Alfreds"),
            "Country", Condition.exactTextCaseSensitive("Austria"))));
  }

  @Test
  public void waitsForDelayedRowsAndResolvesRemountedRoot() {
    SelenideDataTable customers = page.customers;
    driver().executeJavaScript("window.prepareDelayedQueryRow()");
    driver().executeJavaScript("window.restoreDelayedQueryRow()");

    var company = customers.requiredRow(Map.of("Company", "Berglunds"))
        .requiredCell("Company");
    driver().executeJavaScript("window.remountQueryClassic()");

    Assertions.assertThat(company.text()).isEqualTo("Berglunds");
  }

  @Test
  public void reportsMissingDuplicateAndNullHeaders() {
    SelenideDataTable customers = page.customers;

    Assertions.assertThatThrownBy(() -> customers.query().column("Missing"))
        .isInstanceOf(TableColumnNotFoundException.class)
        .hasMessageContaining("Missing")
        .hasMessageContaining("[Country, Company, Employees]");
    driver().executeJavaScript("document.querySelector('#query-classic thead th').textContent = 'Company'");
    Assertions.assertThatThrownBy(() -> customers.query().column("Company"))
        .isInstanceOf(TableColumnAmbiguousException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("[Company, Company, Employees]");
    Assertions.assertThatThrownBy(() -> customers.query().column((String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("column");
  }

  private static final class FixturePage {
    @FindBy(how = How.ID, using = "query-classic")
    private Table<Header> classic;
    @FindBy(how = How.ID, using = "query-flex")
    private FlexTable<Header> flex;
    @FindBy(how = How.ID, using = "query-horizontal")
    private SelenideElement horizontal;
    @FindBy(how = How.ID, using = "query-classic")
    private SelenideDataTable customers;
  }
}
