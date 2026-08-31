package integration;

import java.time.Duration;

import com.codeborne.selenide.table.model.SelenideDomTableModel;
import com.codeborne.selenide.table.model.TableCell;
import com.codeborne.selenide.table.model.TableDomAdapters;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SelenideRowConditionSnapshotTest extends TableContractTestSupport {

  private enum Header {
    COUNTRY("Country"),
    COMPANY("Company"),
    EMPLOYEES("Employees");

    private final String displayed;

    Header(String displayed) {
      this.displayed = displayed;
    }
  }

  @Test
  public void keepsRowConditionCellReadsInsideOneSnapshot() {
    openQueriesFixture();

    SelenideDomTableModel<Header> model = SelenideDomTableModel.of(
        driver().$("#query-classic"), TableDomAdapters.classic(), header -> header.displayed);

    model.requiredRow(candidate -> candidate.cell(Header.COMPANY).map(TableCell::text)
          .filter("Berglunds"::equals)
          .isPresent(), "snapshot condition", Duration.ofSeconds(2));
    Assertions.assertThat(model.rows()).hasSize(4);
  }
}
