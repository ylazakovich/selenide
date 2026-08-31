package com.codeborne.selenide.table.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class LocalDateUtils {
  private LocalDateUtils() {
  }

  public static Optional<LocalDate> dateParse(CharSequence text, DateTimeFormatter formatter) {
    try {
      return Optional.of(LocalDate.parse(text, formatter));
    }
    catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  public static Optional<LocalDateTime> dateTimeParse(CharSequence text, DateTimeFormatter formatter) {
    try {
      return Optional.of(LocalDateTime.parse(text, formatter));
    }
    catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }
}
