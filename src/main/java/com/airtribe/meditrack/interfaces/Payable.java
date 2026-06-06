package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.entity.Bill;

/** Contract for entities that support payment operations. */
public interface Payable {
  /**
   * Pays the current object.
   *
   * @return paid bill instance
   */
  Bill pay();

  /**
   * Computes tax for a monetary amount.
   *
   * @param amount taxable amount
   * @param taxRate tax rate
   * @return computed tax
   */
  default double computeTax(double amount, double taxRate) {
    return amount * taxRate;
  }
}
