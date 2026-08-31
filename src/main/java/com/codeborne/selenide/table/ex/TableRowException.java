package com.codeborne.selenide.table.ex;

import java.time.Duration;
import java.util.Map;

import com.codeborne.selenide.ex.UIAssertionError;

public class TableRowException extends UIAssertionError {
  public TableRowException(Enum<?> columnName, String value) {
    super("No row with '%s' value in '%s' column".formatted(value, columnName));
  }

  public <T extends Enum<?>> TableRowException(Map<T, String> values) {
    super("No row with expected values: %s".formatted(values));
  }

  public TableRowException(Enum<?> columnName, String value, Duration timeout, Throwable cause) {
    super("No row with '%s' value in '%s' column".formatted(value, columnName), timeout.toMillis(), cause);
  }

  public <T extends Enum<?>> TableRowException(Map<T, String> values, Duration timeout, Throwable cause) {
    super("No row with expected values: %s".formatted(values), timeout.toMillis(), cause);
  }

  public TableRowException(Enum<?> columnName, Duration timeout, Throwable cause) {
    super("No row found for '%s'".formatted(columnName), timeout.toMillis(), cause);
  }
}
