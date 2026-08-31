package integration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.codeborne.selenide.table.model.ColumnAssertions;
import com.codeborne.selenide.table.model.RowAssertions;
import com.codeborne.selenide.table.model.SelenideTableQuery;
import com.codeborne.selenide.table.model.TableAssertions;
import com.codeborne.selenide.table.model.TableCellRef;
import com.codeborne.selenide.table.model.TableColumnRef;
import com.codeborne.selenide.table.model.TableDomAdapters;
import com.codeborne.selenide.table.model.TableQueryRow;
import com.codeborne.selenide.table.model.TypedTableCellRef;
import com.codeborne.selenide.table.model.UnsupportedTableEditException;

import com.codeborne.selenide.Condition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableAssertionsActionsContractTest extends TableContractTestSupport {

  private static final Column NAME = new Column("Name");
  private static final Column STATUS = new Column("Status");
  private static final Column ACTION = new Column("Action");
  private static final Column INPUT = new Column("Input");
  private static final Column CHECK = new Column("Check");
  private static final Column RADIO = new Column("Radio");
  private static final Column SELECT = new Column("Select");
  private static final Column READ_ONLY = new Column("Read only");
  private static final Column LINK = new Column("Link");

  private SelenideTableQuery<Column> table;

  @BeforeEach
  public void openFixture() {
    openAssertionsFixture();
    table = SelenideTableQuery.of(driver().$("#assertion-actions"),
        TableDomAdapters.classic(), Column::header);
  }

  @Test
  public void waitsForTableStateWithOneRootCondition() {
    driver().executeJavaScript("window.prepareDelayedAssertionHeader()");
    table.shouldHave(TableAssertions.headers(
        "Name", "Status", "Action", "Input", "Check", "Radio", "Select", "Read only", "Link"),
        Duration.ofSeconds(2));

    driver().executeJavaScript("window.prepareDelayedAssertionRow()");
    table.shouldHave(TableAssertions.rowCount(2), Duration.ofSeconds(2))
        .shouldHave(TableAssertions.columnExists(STATUS))
        .shouldHave(TableAssertions.matchingRow(RowAssertions.cell(NAME, Condition.text("Beta"))));
  }

  @Test
  public void waitsForRowAndCellState() {
    driver().executeJavaScript("window.prepareDelayedAssertionCell()");

    table.row(0).shouldHave(RowAssertions.cell(STATUS, Condition.exactText("Ready")),
        Duration.ofSeconds(2));
    table.row(1).shouldHave(RowAssertions.values(
        "Beta", "Ready", "", "", "", "", "", "fixed too", ""));
    table.row(0).requiredCell(STATUS).shouldHave(Condition.exactText("Ready"));
  }

  @Test
  public void reResolvesEveryHandleAfterRemount() {
    TableQueryRow<Column> row = table.row(0);
    TableCellRef<Column> cell = row.requiredCell(STATUS);
    TableColumnRef<Column> column = table.column(STATUS);

    driver().executeJavaScript("window.remountAssertionTable()");

    row.shouldHave(RowAssertions.cell(NAME, Condition.exactText("Alpha")));
    cell.shouldBe(Condition.visible).shouldHave(Condition.exactText("Ready"));
    column.shouldHave(ColumnAssertions.values("Ready", "Ready"));
  }

  @Test
  public void actsOnButtonsAndLinks() {
    table.row(0).requiredCell(ACTION).button().shouldBe(Condition.enabled).click();
    driver().$("#table-action-result").shouldHave(Condition.exactText("button"));

    table.row(0).requiredCell(LINK).link().shouldHave(Condition.text("Details")).click();
    driver().$("#table-action-result").shouldHave(Condition.exactText("link"));
  }

  @Test
  public void editsExplicitEmbeddedControls() {
    table.row(0).requiredCell(INPUT).input().setValue("changed");
    Assertions.assertThat(table.row(0).requiredCell(INPUT).input().value()).isEqualTo("changed");

    table.row(0).requiredCell(CHECK).checkbox().setSelected(true);
    Assertions.assertThat(table.row(0).requiredCell(CHECK).checkbox().isSelected()).isTrue();

    table.row(0).requiredCell(RADIO).radio().select();
    Assertions.assertThat(table.row(0).requiredCell(RADIO).radio().isSelected()).isTrue();

    table.row(0).requiredCell(SELECT).select().selectOption("Two");
    Assertions.assertThat(table.row(0).requiredCell(SELECT).select().selectedText()).isEqualTo("Two");
  }

  @Test
  public void editsContenteditableCells() {
    SelenideTableQuery<String> contenteditable = SelenideTableQuery.of(
        driver().$("#contenteditable-table"), TableDomAdapters.classic(), header -> header);

    Assertions.assertThat(contenteditable.row(0).requiredCell("Direct").editable().value())
        .isEqualTo("Direct");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Inherited").editable().value())
        .isEqualTo("Inherited");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Empty").editable().value())
        .isEqualTo("Empty");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Plaintext").editable().value())
        .isEqualTo("Plaintext");
    contenteditable.row(0).requiredCell("Direct").editable().setValue("Changed");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Direct").editable().value())
        .isEqualTo("Changed");
    Assertions.assertThatThrownBy(() -> contenteditable.row(0).requiredCell("False").editable())
        .isInstanceOf(UnsupportedTableEditException.class);
  }

  @Test
  public void rejectsReadOnlyEditingExplicitly() {
    Assertions.assertThatThrownBy(() -> table.row(0).requiredCell(READ_ONLY).editable())
        .isInstanceOf(UnsupportedTableEditException.class)
        .hasMessageContaining("row index 0")
        .hasMessageContaining("Read only");
  }

  @Test
  public void reportsDiagnosticAddressesAndValues() {
    Assertions.assertThatThrownBy(() -> table.row(0).shouldHave(
            RowAssertions.values("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("#assertion-actions")
        .hasMessageContaining("row index 0")
        .hasMessageContaining("headers=[Name, Status")
        .hasMessageContaining("Alpha, Ready");

    Assertions.assertThatThrownBy(() -> table.column(STATUS).shouldHave(
            ColumnAssertions.values("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("column key=" + STATUS)
        .hasMessageContaining("header=Status")
        .hasMessageContaining("Alpha, Ready");
  }

  @Test
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    openQueriesFixture();
    AtomicBoolean redirected = new AtomicBoolean();
    AtomicReference<String> indexedText = new AtomicReference<>();
    AtomicReference<String> typedText = new AtomicReference<>();
    SelenideTableQuery<Column> query = SelenideTableQuery.of(driver().$("#query-classic"),
        TableDomAdapters.classic(), Column::header);

    query.requiredRow(row -> {
      TableCellRef<Column> indexed = row.cell(0);
      TypedTableCellRef<Column> typed = row.requiredCell(new Column("Country"));
      if (redirected.compareAndSet(false, true)) {
        driver().executeJavaScript("window.redirectQueryClassicRoot()");
        indexedText.set(indexed.text());
        typedText.set(typed.text());
      }
      return true;
    }, Duration.ofSeconds(2));

    Assertions.assertThat(indexedText).hasValue("Austria");
    Assertions.assertThat(typedText).hasValue("Austria");
    Assertions.assertThat(query.row(0).cell(0).text()).isEqualTo("Changed");
  }

  private record Column(String header) {
  }
}
