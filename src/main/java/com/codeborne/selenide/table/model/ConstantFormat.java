package com.codeborne.selenide.table.model;

import java.util.Locale;

public interface ConstantFormat {
  String formatValue();

  default String value() {
    return formatValue().toLowerCase(Locale.ROOT);
  }

  default String capitalize() {
    return capitalizeFirstWord();
  }

  default String capitalizeFirstWord() {
    String value = lowerCaseWithSpace();
    return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  default String lowerCaseWithSpace() {
    return value().replace('_', ' ');
  }

  default String lowerCaseWithDash() {
    return value().replace('_', '-');
  }

  default String lowerCase() {
    return value();
  }

  default String upperCaseWithSpace() {
    return upperCase().replace('_', ' ');
  }

  default String upperCase() {
    return formatValue().toUpperCase(Locale.ROOT);
  }

  default String upperCamelCase() {
    return camelCase(true);
  }

  default String camelCase() {
    return camelCase(false);
  }

  private String camelCase(boolean upper) {
    String[] parts = lowerCase().split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (!part.isEmpty()) {
        result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
      }
    }
    if (!upper && result.length() > 0) {
      result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
    }
    return result.toString();
  }

  default String upperCaseWithDash() {
    return upperCase().replace('_', '-');
  }
}
