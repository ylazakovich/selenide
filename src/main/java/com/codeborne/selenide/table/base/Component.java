package com.codeborne.selenide.table.base;

import com.codeborne.selenide.Container;
import com.codeborne.selenide.SelenideElement;

public abstract class Component implements Container {
  @Self
  private SelenideElement self;

  public SelenideElement getSelf() {
    return self;
  }
}
