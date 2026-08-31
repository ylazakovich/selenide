package integration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.codeborne.selenide.table.classic.DynamicTable;
import com.codeborne.selenide.table.classic.FlexTable;
import com.codeborne.selenide.table.classic.Table;
import com.codeborne.selenide.table.horizontal.DynamicHorizontalTable;
import com.codeborne.selenide.table.horizontal.HorizontalTable;
import com.codeborne.selenide.table.model.DisplayedHeaderResolver;
import com.codeborne.selenide.table.model.NoTableHeaders;
import com.codeborne.selenide.table.model.RowConditions;
import com.codeborne.selenide.table.model.SelenideDomTableModel;
import com.codeborne.selenide.table.model.SelenideTableQuery;
import com.codeborne.selenide.table.model.TableCell;
import com.codeborne.selenide.table.model.TableCellNotFoundException;
import com.codeborne.selenide.table.model.TableColumnAmbiguousException;
import com.codeborne.selenide.table.model.TableColumnNotFoundException;
import com.codeborne.selenide.table.model.TableDomAdapter;
import com.codeborne.selenide.table.model.TableDomAdapters;
import com.codeborne.selenide.table.model.TableHeaderRowLocator;
import com.codeborne.selenide.table.model.TableModel;
import com.codeborne.selenide.table.model.TableQueryRow;
import com.codeborne.selenide.table.model.TableRow;
import com.codeborne.selenide.table.model.TypedTableCellRef;
import com.codeborne.selenide.table.model.ConstantFormat;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.logevents.SelenideLogger;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.junit.jupiter.api.Test;

public class TableModelContractTest extends TableContractTestSupport {

  private static final String REPETITIONS_PROPERTY = "tableModel.contract.repetitions";

  private enum Header {
    COMPANY("Company"),
    COUNTRY("Country");

    private final String displayed;

    Header(String displayed) {
      this.displayed = displayed;
    }
  }

  @Test
  public void resolvesDisplayedHeader() {
    TableModel<Header> model = model(List.of("Country", "Company"));

    Assertions.assertThat(model.columnIndex(Header.COMPANY, DisplayedHeaderResolver.requiringNonNull(h -> h.displayed)))
        .isEqualTo(1);
  }

  @Test
  public void reportsMissingDisplayedHeader() {
    TableModel<Header> model = model(List.of("Country"));

    Assertions.assertThatThrownBy(() -> model.columnIndex(Header.COMPANY,
            DisplayedHeaderResolver.requiringNonNull(h -> h.displayed)))
        .isInstanceOf(TableColumnNotFoundException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("Country");
  }

  @Test
  public void rejectsAmbiguousDisplayedHeader() {
    TableModel<Header> model = model(List.of("Country", "Company", "Company"));

    Assertions.assertThatThrownBy(() -> model.columnIndex(Header.COMPANY,
            DisplayedHeaderResolver.requiringNonNull(h -> h.displayed)))
        .isInstanceOf(TableColumnAmbiguousException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("[Country, Company, Company]");
  }

  @Test
  public void readsTypedCellLazily() {
    TableRow<Header> row = new TableRow<>() {
      @Override
      public Optional<? extends TableCell<Header>> cell(Header column) {
        return Optional.of(new TableCell<>() {
          @Override
          public Header column() {
            return column;
          }

          @Override
          public String text() {
            return "Austria";
          }
        });
      }
    };

    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
  public void supportsFullyCustomBackendContract() {
    TableModel<Header> backend = new TableModel<>() {
      private final List<BackendRow> dataRows = List.of(
          new BackendRow(Map.of(Header.COUNTRY, "Austria", Header.COMPANY, "Outer (nested: Leak)")));

      @Override
      public List<String> displayedHeaders() {
        return List.of("Country", "Company");
      }

      @Override
      public List<BackendRow> rows() {
        return dataRows;
      }
    };

    TableRow<Header> row = backend.requiredRow(candidate -> candidate
        .requiredCell(Header.COMPANY).text().startsWith("Outer"), "custom backend row");

    Assertions.assertThat(backend.rows()).hasSize(1);
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(row.requiredCell(Header.COMPANY).text())
        .isEqualTo("Outer (nested: Leak)");
    Assertions.assertThat(row.cell(Header.COMPANY)).isPresent();
  }

  @Test
  public void bridgesAllLegacyVariants() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);

    Assertions.assertThat(page.classic.asDomModel(h -> h.displayed).rows().get(0)
        .requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(page.dynamic.asDomModel(h -> h.formatValue()).displayedHeaders())
        .containsExactly("Company", "Country");
    Assertions.assertThat(page.flex.asDomModel(h -> h.displayed).displayedHeaders())
        .containsExactly("Country", "Company");
    Assertions.assertThat(page.flex.asDomModel(h -> h.displayed).rows().get(0)
        .requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");
    Assertions.assertThat(page.horizontal.asDomModel(h -> h.formatValue()).row(
        row -> row.cell(HorizontalHeader.COUNTRY).isPresent()).orElseThrow()
        .requiredCell(HorizontalHeader.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(page.dynamicHorizontal.asDomModel(h -> h.formatValue()).row(
        row -> row.cell(DynamicHorizontalHeader.COUNTRY).isPresent()).orElseThrow()
        .requiredCell(DynamicHorizontalHeader.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
    public void waitsForDelayedRow() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    driver().executeJavaScript("window.prepareDelayedRow()");
    $$("#classic tbody tr").shouldHave(CollectionCondition.empty);
    driver().executeJavaScript("window.restoreDelayedRow()");
    TableRow<Header> row = model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "company", Duration.ofSeconds(2));
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
    public void rowReferenceSurvivesRemount() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    TableRow<Header> row = model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "company", Duration.ofSeconds(2));
    driver().executeJavaScript("window.remount()");
    SelenideLogger.step("Verify remounted row country", () ->
        Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria"));
  }

  @Test
  public void requiredRowsSkipHeaders() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);

    TableRow<Header> classicRow = page.classic.asDomModel(h -> h.displayed)
        .requiredRow(candidate -> true, "first classic", Duration.ofMillis(100));
    TableRow<Header> flexRow = page.flex.asDomModel(h -> h.displayed)
        .requiredRow(candidate -> true, "first flex", Duration.ofMillis(100));

    Assertions.assertThat(classicRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(flexRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    driver().executeJavaScript("window.remount()");
    Assertions.assertThat(classicRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(flexRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
  public void reportsMissingRowsAndCellsConsistently() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    TableRow<Header> row = model.rows().get(0);

    Assertions.assertThat(row.cell(Header.COUNTRY)).isPresent();
    driver().executeJavaScript("window.prepareMissingCell()");
    Assertions.assertThat(row.cell(Header.COMPANY)).isEmpty();
    Assertions.assertThatThrownBy(() -> row.requiredCell(Header.COMPANY))
        .isInstanceOf(TableCellNotFoundException.class)
        .hasMessageContaining("COMPANY");
    Assertions.assertThatThrownBy(() -> model.requiredRow(candidate -> false, "missing", Duration.ofMillis(100)))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("missing");
  }

  @Test
  public void waitsForLateRootMount() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);

    driver().executeJavaScript("window.prepareLateMount()");
    Assertions.assertThat(model.row(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false))).isEmpty();
    Assertions.assertThat(model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "late company", Duration.ofSeconds(2))
        .requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
  public void timesOutAcrossLateRootAndRowDiscovery() {
    openClassicVariantsFixture();
    FixturePage page = driver().page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);

    driver().executeJavaScript("""
        const table = document.getElementById('classic');
        const row = table.querySelector('tbody tr');
        row.remove();
        table.remove();
        window.setTimeout(() => document.body.prepend(table), 250);
        window.setTimeout(() => table.querySelector('tbody').appendChild(row), 650);
        """);

    Assertions.assertThatThrownBy(() -> model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
            .map(cell -> cell.text().equals("Alfreds")).orElse(false),
        "late row", Duration.ofMillis(450)))
        .isInstanceOf(com.codeborne.selenide.table.model.TableRowNotFoundException.class)
        .hasMessageContaining("late row")
        .hasMessageContaining("PT0.45S");
  }

  @Test
  public void supportsCustomDivAdapter() {
    openCustomGridsFixture();
    TableDomAdapter adapter = customGridAdapter();
    TableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#custom-grid"), adapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableModel<Header> nested = SelenideDomTableModel.of(
        driver().$("#nested-custom-grid"), adapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.displayedHeaders()).containsExactly("Country", "Company");
    Assertions.assertThat(model.rows()).hasSize(3);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text()).contains("Outer");
    Assertions.assertThat(model.rows().get(1).cell(Header.COMPANY)).isPresent()
        .get().extracting(TableCell::text).isEqualTo("");
    Assertions.assertThat(model.rows().get(2).cell(Header.COMPANY)).isEmpty();
    Assertions.assertThat(nested.rows()).hasSize(1);
    Assertions.assertThat(nested.rows().get(0).requiredCell(Header.COMPANY).text()).isEqualTo("Leak");
  }

  @Test
    public void customAdapterWaitsAndSurvivesRemount() {
    openCustomGridsFixture();
    TableDomAdapter adapter = customGridAdapter();
    TableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#custom-grid"), adapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableRow<Header> row = model.rows().get(0);
    driver().executeJavaScript("window.remountCustomGrid()");
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");

    driver().executeJavaScript("window.prepareCustomDelayed()");
    TableRow<Header> delayed = model.requiredRow(candidate -> candidate
        .cell(Header.COUNTRY).map(cell -> cell.text().equals("Austria")).orElse(false),
        "custom late row", Duration.ofSeconds(2));
    Assertions.assertThat(delayed.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
  public void supportsBodyOnlyClassicTable() {
    openEdgeCasesFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#body-only-classic"), TableDomAdapters.classic(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.displayedHeaders()).containsExactly("Country", "Company");
    Assertions.assertThat(model.rows()).hasSize(1);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text())
        .isEqualTo("Alfreds");
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COUNTRY).text())
        .isEqualTo("Austria");
  }

  @Test
  public void excludesNestedTableRows() {
    openEdgeCasesFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#nested-classic"), TableDomAdapters.classic(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.rows()).hasSize(1);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text())
        .contains("Outer");
  }

  @Test
  public void supportsNestedClassicRoot() {
    openEdgeCasesFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#nested-classic table"), TableDomAdapters.classic(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.rows()).hasSize(1);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COUNTRY).text())
        .isEqualTo("Nested");
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text())
        .isEqualTo("Leak");
  }

  @Test
  public void supportsAriaGridAndRemount() {
    openCustomGridsFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#aria-grid"), TableDomAdapters.ariaGrid(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableRow<Header> row = model.rows().get(0);

    Assertions.assertThat(model.displayedHeaders()).containsExactly("Country", "Company");
    Assertions.assertThat(row.requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");
    driver().executeJavaScript("window.remountAriaGrid()");
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test
  public void handlesHeaderlessAndRepeatedHeaders() {
    openCustomGridsFixture();
    TableDomAdapter headerlessAdapter = TableDomAdapters.of(
        By.cssSelector(":scope > .data-row"), By.cssSelector(":scope > .cell"),
        NoTableHeaders.instance());
    TableModel<Header> headerless = SelenideDomTableModel.of(
        driver().$("#headerless-grid"), headerlessAdapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableModel<Header> repeated = SelenideDomTableModel.of(
        driver().$("#custom-repeated-grid"), customGridAdapter(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(headerless.displayedHeaders()).isEmpty();
    Assertions.assertThatThrownBy(() -> headerless.rows().get(0).cell(Header.COUNTRY))
        .isInstanceOf(TableColumnNotFoundException.class);
    Assertions.assertThatThrownBy(() -> repeated.rows().get(0).cell(Header.COMPANY))
        .isInstanceOf(TableColumnAmbiguousException.class);
    Assertions.assertThatThrownBy(() -> SelenideTableQuery.<Header>of(
            driver().$("#custom-repeated-grid"), customGridAdapter(), header -> header.displayed)
        .uniqueRow(candidate -> true))
        .isInstanceOf(com.codeborne.selenide.table.model.TableRowAmbiguousException.class)
        .hasMessageContaining("found 3");
  }

  @Test
  public void greaterThanUsesStrictNumericContract() {
    String[] accepted = {"11", "10.01", "+11", "1e2"};
    for (String value : accepted) {
      Assertions.assertThat(RowConditions.greaterThan(Header.COMPANY, 10)
          .test(numericRow(value)))
          .as(value).isTrue();
    }
    String[] rejected = {"$100", "10%", "1,000", "", "ten"};
    for (String value : rejected) {
      Assertions.assertThat(RowConditions.greaterThan(Header.COMPANY, 10)
          .test(numericRow(value)))
          .as(value).isFalse();
    }
  }

  @SuppressWarnings("unchecked")
  private static TableQueryRow<Header> numericRow(String value) {
    TableQueryRow<Header> row = Mockito.mock(TableQueryRow.class);
    TypedTableCellRef<Header> cell = Mockito.mock(TypedTableCellRef.class);
    Mockito.when(cell.text()).thenReturn(value);
    Mockito.doReturn(Optional.of(cell)).when(row).cell(Header.COMPANY);
    return row;
  }

  private static TableDomAdapter customGridAdapter() {
    return TableDomAdapters.of(
        By.cssSelector(":scope > .data-row"),
        By.cssSelector(":scope > .cell:not([hidden])"),
        new TableHeaderRowLocator(
            By.cssSelector(":scope > .header-row"),
            By.cssSelector(":scope > .cell:not([hidden])")));
  }

  private static final class FixturePage {
    @FindBy(how = How.ID, using = "classic")
    private Table<Header> classic;
    @FindBy(how = How.ID, using = "dynamic")
    private DynamicTable<DynamicHeader> dynamic;
    @FindBy(how = How.ID, using = "flex")
    private FlexTable<Header> flex;
    @FindBy(how = How.ID, using = "horizontal")
    private HorizontalTable<HorizontalHeader> horizontal;
    @FindBy(how = How.ID, using = "dynamic-horizontal")
    private DynamicHorizontalTable<DynamicHorizontalHeader> dynamicHorizontal;
  }

  private enum DynamicHeader implements ConstantFormat {
    COMPANY, COUNTRY;

    @Override
    public String formatValue() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private enum HorizontalHeader implements ConstantFormat {
    COUNTRY, COMPANY;

    @Override
    public String formatValue() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private enum DynamicHorizontalHeader implements ConstantFormat {
    COMPANY, COUNTRY;

    @Override
    public String formatValue() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private static TableModel<Header> model(List<String> headers) {
    return new TableModel<>() {
      @Override
      public List<String> displayedHeaders() {
        return headers;
      }

      @Override
      public List<? extends TableRow<Header>> rows() {
        return List.of();
      }
    };
  }

  private record BackendRow(Map<Header, String> values) implements TableRow<Header> {
    @Override
    public Optional<BackendCell> cell(Header column) {
      return Optional.ofNullable(values.get(column)).map(value -> new BackendCell(column, value));
    }
  }

  private record BackendCell(Header column, String text) implements TableCell<Header> {
  }
}
