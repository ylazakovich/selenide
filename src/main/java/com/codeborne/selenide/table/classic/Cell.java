package com.codeborne.selenide.table.classic;

import com.codeborne.selenide.table.classic.base.BaseCell;

import com.codeborne.selenide.SelenideElement;

/**
 * Classic table cell UI element and methods of working with it.
 */
public class Cell<T extends Enum<T>> extends BaseCell {

  private final Row<T> row;

  public Cell(SelenideElement element, Row<T> row) {
    super(element);
    this.row = row;
  }

  @Override
  public Row<T> getRow() {
    return row;
  }
}
