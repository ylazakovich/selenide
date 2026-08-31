package com.codeborne.selenide;

import java.util.Objects;

import org.openqa.selenium.By;

/**
 * Minimal, lazy navigation helper for ordinary HTML tables.
 *
 * <p>The helper intentionally covers only the common {@code tbody}/{@code tr}/{@code td} shape.
 * It keeps the table root and indexes, so Selenide resolves the current DOM when a cell is used
 * and existing Selenide waits, commands, and diagnostics remain in control.</p>
 */
public final class SelenideTable {
  private static final By BODY_ROWS = By.xpath("./tbody/tr");
  private static final By ROW_CELLS = By.xpath("./td | ./th");

  private final SelenideElement table;

  private SelenideTable(SelenideElement table) {
    this.table = Objects.requireNonNull(table, "table");
  }

  /**
   * Creates a table navigator rooted at the supplied lazy Selenide element.
   *
   * @param table element containing the table body
   * @return a navigator that resolves rows and cells lazily
   */
  public static SelenideTable of(SelenideElement table) {
    return new SelenideTable(table);
  }

  /**
   * Returns a cell by zero-based body-row and cell indexes.
   *
   * <p>The returned element is lazy: the row and cell are resolved when a Selenide operation is
   * invoked on it. This makes the reference safe across a table remount.</p>
   *
   * @param rowIndex zero-based index among {@code tbody > tr} elements
   * @param cellIndex zero-based index among direct {@code td}/{@code th} children
   * @return the requested lazy cell
   * @throws IndexOutOfBoundsException if either index is negative
   */
  public SelenideElement cell(int rowIndex, int cellIndex) {
    validateIndex(rowIndex, "rowIndex");
    validateIndex(cellIndex, "cellIndex");
    return table.findAll(BODY_ROWS).get(rowIndex).findAll(ROW_CELLS).get(cellIndex);
  }

  private static void validateIndex(int index, String name) {
    if (index < 0) {
      throw new IndexOutOfBoundsException(name + " must not be negative: " + index);
    }
  }
}
