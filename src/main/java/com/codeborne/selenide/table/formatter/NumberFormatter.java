package com.codeborne.selenide.table.formatter;

public final class NumberFormatter {
  private NumberFormatter() {
  }

  public static String formatNumberIfNull(Number number) {
    return number == null ? null : String.valueOf(number);
  }
}
