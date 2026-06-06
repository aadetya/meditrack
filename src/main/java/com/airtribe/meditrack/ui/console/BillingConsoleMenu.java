package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.entity.BillSummary;
import com.airtribe.meditrack.service.BillingService;

/** Console menu for bill generation and inspection. */
public class BillingConsoleMenu {
  private final InputReader inputReader;
  private final BillingService billingService;

  /**
   * Creates the billing console menu.
   */
  public BillingConsoleMenu(InputReader inputReader, BillingService billingService) {
    this.inputReader = inputReader;
    this.billingService = billingService;
  }

  /** Runs the billing menu loop. */
  public void run() {
    while (true) {
      System.out.println();
      System.out.println("Billing");
      System.out.println("1. Generate bill for appointment");
      System.out.println("2. List bills");
      System.out.println("0. Back");

      switch (inputReader.promptOptionalString("Choose: ")) {
        case "1" -> generateBill();
        case "2" -> billingService.listBills().forEach(System.out::println);
        case "0" -> {
          return;
        }
        default -> System.out.println("Invalid choice.");
      }
    }
  }

  private void generateBill() {
    BillSummary summary =
        billingService.generateBillSummary(inputReader.promptString("Appointment ID: "));
    System.out.println(summary);
  }
}
