package com.example.botforconsultations.core.service;

import com.example.botforconsultations.config.GoogleCalendarConfig;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.TodoTask;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

/**
 * Сервис для работы с Google Calendar API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final GoogleOAuthService oAuthService;
    private final GoogleCalendarConfig config;

    /**
     * Создать событие в календаре для задачи
     */
    public Optional<String> createTaskEvent(TelegramUser user, TodoTask task) {
        try {
            Optional<Credential> credentialOpt = oAuthService.getCredential(user);
            if (credentialOpt.isEmpty()) {
                log.warn("No Google Calendar credential for user #{}", user.getId());
                return Optional.empty();
            }

            Calendar service = getCalendarService(credentialOpt.get());

            Event event = new Event()
                    .setSummary("📋 " + task.getTitle())
                    .setDescription(buildEventDescription(task))
                    .setColorId("11"); // Красный цвет для дедлайнов

            // Устанавливаем время дедлайна
            DateTime deadline = new DateTime(
                    Date.from(task.getDeadline().atZone(ZoneId.systemDefault()).toInstant())
            );
            
            EventDateTime start = new EventDateTime()
                    .setDateTime(deadline)
                    .setTimeZone("Europe/Moscow");
            event.setStart(start);

            // Событие на 1 час (можно настроить)
            DateTime endTime = new DateTime(
                    Date.from(task.getDeadline().plusHours(1)
                            .atZone(ZoneId.systemDefault()).toInstant())
            );
            EventDateTime end = new EventDateTime()
                    .setDateTime(endTime)
                    .setTimeZone("Europe/Moscow");
            event.setEnd(end);

            // Напоминания на основе настроек пользователя
            if (user.getReminderTime() != null) {
                EventReminder reminder = new EventReminder()
                        .setMethod("popup")
                        .setMinutes(user.getReminderTime().getMinutesBeforeDeadline());

                Event.Reminders reminders = new Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(Arrays.asList(reminder));
                event.setReminders(reminders);
            }

            // Создаем событие
            event = service.events().insert("primary", event).execute();
            
            log.info("Created Google Calendar event {} for task #{}", event.getId(), task.getId());
            return Optional.of(event.getId());

        } catch (Exception e) {
            log.error("Error creating calendar event for task #{}: {}", task.getId(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Обновить событие в календаре
     */
    public boolean updateTaskEvent(TelegramUser user, TodoTask task, String eventId) {
        try {
            Optional<Credential> credentialOpt = oAuthService.getCredential(user);
            if (credentialOpt.isEmpty()) {
                return false;
            }

            Calendar service = getCalendarService(credentialOpt.get());

            // Получаем существующее событие
            Event event = service.events().get("primary", eventId).execute();

            // Обновляем данные
            event.setSummary("📋 " + task.getTitle());
            event.setDescription(buildEventDescription(task));

            // Обновляем время дедлайна
            DateTime deadline = new DateTime(
                    Date.from(task.getDeadline().atZone(ZoneId.systemDefault()).toInstant())
            );
            
            EventDateTime start = new EventDateTime()
                    .setDateTime(deadline)
                    .setTimeZone("Europe/Moscow");
            event.setStart(start);

            DateTime endTime = new DateTime(
                    Date.from(task.getDeadline().plusHours(1)
                            .atZone(ZoneId.systemDefault()).toInstant())
            );
            EventDateTime end = new EventDateTime()
                    .setDateTime(endTime)
                    .setTimeZone("Europe/Moscow");
            event.setEnd(end);

            // Обновляем событие
            service.events().update("primary", eventId, event).execute();
            
            log.info("Updated Google Calendar event {} for task #{}", eventId, task.getId());
            return true;

        } catch (Exception e) {
            log.error("Error updating calendar event {} for task #{}: {}", 
                    eventId, task.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Удалить событие из календаря
     */
    public boolean deleteTaskEvent(TelegramUser user, String eventId) {
        try {
            Optional<Credential> credentialOpt = oAuthService.getCredential(user);
            if (credentialOpt.isEmpty()) {
                return false;
            }

            Calendar service = getCalendarService(credentialOpt.get());
            service.events().delete("primary", eventId).execute();
            
            log.info("Deleted Google Calendar event {}", eventId);
            return true;

        } catch (Exception e) {
            log.error("Error deleting calendar event {}: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Отметить событие как выполненное (изменить цвет)
     */
    public boolean markEventAsCompleted(TelegramUser user, String eventId) {
        try {
            Optional<Credential> credentialOpt = oAuthService.getCredential(user);
            if (credentialOpt.isEmpty()) {
                return false;
            }

            Calendar service = getCalendarService(credentialOpt.get());
            
            Event event = service.events().get("primary", eventId).execute();
            event.setSummary("✅ " + event.getSummary().replace("📋 ", ""));
            event.setColorId("10"); // Зеленый цвет для выполненных
            
            service.events().update("primary", eventId, event).execute();
            
            log.info("Marked Google Calendar event {} as completed", eventId);
            return true;

        } catch (Exception e) {
            log.error("Error marking event {} as completed: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Построить описание события
     */
    private String buildEventDescription(TodoTask task) {
        StringBuilder description = new StringBuilder();
        
        description.append("Задача от деканата\n\n");
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            description.append("Описание: ").append(task.getDescription()).append("\n\n");
        }
        
        description.append("Статус: ")
                .append(task.getIsCompleted() ? "✅ Выполнено" : "⏳ В процессе");
        
        return description.toString();
    }

    /**
     * Создать Calendar service
     */
    private Calendar getCalendarService(Credential credential) {
        return new Calendar.Builder(
                credential.getTransport(),
                credential.getJsonFactory(),
                credential)
                .setApplicationName(config.getApplicationName())
                .build();
    }
}
