package com.airtribe.meditrack.service.notification;

import com.airtribe.meditrack.util.DateUtil;

/** Observer that prints lifecycle updates to the console. */
public class ConsoleNotificationObserver implements AppointmentObserver {
  @Override
  public void onEvent(AppointmentEvent event) {
    AppointmentSnapshot a = event.getAppointment();
    System.out.println(
        "[NOTIFY] "
            + event.getType()
            + " appointment "
            + a.getAppointmentId()
            + " (Doctor="
            + a.getDoctorId()
            + ", Patient="
            + a.getPatientId()
            + ", Start="
            + DateUtil.formatDateTime(a.getStartTime())
            + ")");
  }
}
