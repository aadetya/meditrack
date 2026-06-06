package com.airtribe.meditrack.ui;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/** Supplementary Swing launcher for MediTrack. */
public final class GuiMain {
  private GuiMain() {}

  /**
   * Starts the Swing UI unless the environment is headless.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    boolean loadData = args != null && Arrays.asList(args).contains("--loadData");
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("Swing UI cannot open in a headless environment.");
      System.out.println("Use the console app or run GuiMain on a desktop JVM.");
      return;
    }

    SwingUtilities.invokeLater(
        () -> {
          MediTrackFrame frame = new MediTrackFrame(loadData);
          frame.setVisible(true);
        });
  }
}
