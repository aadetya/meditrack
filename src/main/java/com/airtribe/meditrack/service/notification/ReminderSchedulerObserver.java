package com.airtribe.meditrack.service.notification;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.util.DateUtil;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/** Observer that schedules one reminder task per confirmed appointment. */
public class ReminderSchedulerObserver implements AppointmentObserver, AutoCloseable {
  private final Timer timer;
  private final Map<String, TimerTask> tasks = new HashMap<>();
  private final int minutesBefore;

  /** Creates a reminder scheduler using the default reminder offset. */
  public ReminderSchedulerObserver() {
    this(Constants.REMINDER_MINUTES_BEFORE);
  }

  /**
   * Creates a reminder scheduler with a custom lead time.
   *
   * @param minutesBefore reminder lead time in minutes
   */
  public ReminderSchedulerObserver(int minutesBefore) {
    this.minutesBefore = minutesBefore < 0 ? 0 : minutesBefore;
    this.timer = new Timer("meditrack-reminders", true);
  }

  @Override
  public void onEvent(AppointmentEvent event) {
    AppointmentSnapshot appointment = event.getAppointment();
    switch (event.getType()) {
      case CREATED, CONFIRMED, RESCHEDULED -> scheduleIfNeeded(appointment);
      case CANCELLED, COMPLETED -> cancel(appointment.getAppointmentId());
    }
  }

  /**
   * Returns whether a reminder task is currently scheduled for an appointment.
   *
   * @param appointmentId appointment identifier
   * @return true when scheduled
   */
  public boolean isScheduled(String appointmentId) {
    synchronized (tasks) {
      return tasks.containsKey(appointmentId);
    }
  }

  /**
   * Returns the number of currently scheduled reminder tasks.
   *
   * @return scheduled reminder count
   */
  public int getScheduledCount() {
    synchronized (tasks) {
      return tasks.size();
    }
  }

  private void scheduleIfNeeded(AppointmentSnapshot appointment) {
    cancel(appointment.getAppointmentId());
    if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      return;
    }

    Date trigger =
        Date.from(
            appointment.getStartTime()
                .minusMinutes(minutesBefore)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    long delay = Math.max(0, trigger.getTime() - System.currentTimeMillis());

    TimerTask task =
        new TimerTask() {
          @Override
          public void run() {
            try {
              System.out.println(
                  "[REMINDER] Appointment "
                      + appointment.getAppointmentId()
                      + " starts at "
                      + DateUtil.formatDateTime(appointment.getStartTime())
                      + " (Doctor="
                      + appointment.getDoctorId()
                      + ", Patient="
                      + appointment.getPatientId()
                      + ")");
            } finally {
              synchronized (tasks) {
                tasks.remove(appointment.getAppointmentId(), this);
              }
            }
          }
        };

    synchronized (tasks) {
      tasks.put(appointment.getAppointmentId(), task);
    }
    timer.schedule(task, Duration.ofMillis(delay).toMillis());
  }

  private void cancel(String appointmentId) {
    TimerTask existing;
    synchronized (tasks) {
      existing = tasks.remove(appointmentId);
    }
    if (existing != null) {
      existing.cancel();
    }
  }

  @Override
  public void close() {
    synchronized (tasks) {
      for (TimerTask task : tasks.values()) {
        task.cancel();
      }
      tasks.clear();
    }
    timer.cancel();
  }
}
