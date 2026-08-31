package com.codeborne.selenide;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelenideTableTest {
  @Test
  void resolvesCellThroughScopedLazyElements() {
    SelenideElement table = mock();
    ElementsCollection rows = mock();
    SelenideElement row = mock();
    ElementsCollection cells = mock();
    SelenideElement cell = mock();
    when(table.findAll(By.cssSelector("tbody > tr"))).thenReturn(rows);
    when(rows.get(1)).thenReturn(row);
    when(row.findAll(By.cssSelector(":scope > td, :scope > th"))).thenReturn(cells);
    when(cells.get(2)).thenReturn(cell);

    assertThat(SelenideTable.of(table).cell(1, 2)).isSameAs(cell);
  }

  @Test
  void rejectsNegativeIndexesBeforeCreatingElementReferences() {
    SelenideElement table = mock();
    SelenideTable navigator = SelenideTable.of(table);

    assertThatThrownBy(() -> navigator.cell(-1, 0))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("rowIndex");
    assertThatThrownBy(() -> navigator.cell(0, -1))
      .isInstanceOf(IndexOutOfBoundsException.class)
      .hasMessageContaining("cellIndex");
  }
}
