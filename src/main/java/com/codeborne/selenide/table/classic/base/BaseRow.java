package com.codeborne.selenide.table.classic.base;



import com.codeborne.selenide.table.base.Component;

import com.codeborne.selenide.SelenideElement;

/**
 * Abstract class to work with table row.
 */
public abstract class BaseRow<T extends Enum<T>> extends Component {

  protected static final int HTML_START_INDEX = 1;
  protected SelenideElement element;

  public BaseRow(SelenideElement element) {
    this.element = element;
  }

  @Override
  public SelenideElement getSelf() {
    return element;
  }
}
