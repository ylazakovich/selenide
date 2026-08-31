package com.codeborne.selenide.table.model;

/** Explicit capability for a checkbox control. */
public interface CheckableTableControl extends TableControl {

  CheckableTableControl setSelected(boolean selected);

  boolean isSelected();
}
