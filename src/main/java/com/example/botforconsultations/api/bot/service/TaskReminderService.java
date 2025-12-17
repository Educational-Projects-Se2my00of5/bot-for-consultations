package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.api.bot.BotMessenger;
import com.example.botforconsultations.core.model.ReminderTime;
import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.repository.TodoTaskRepository;
import com.example.botforconsultations.core.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для отправки напоминаний преподавателям о приближающихся дедлайнах задач
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskReminderService {

    private final TodoTaskRepository todoTaskRepository;
    private final BotMessenger botMessenger;
    private final GoogleOAuthService googleOAuthService;

    /**
     * Проверка и отправка напоминаний каждые 5 минут
     */
    @Scheduled(fixedRate = 300000) // 5 минут в миллисекундах
    public void checkAndSendReminders() {
        log.debug("Checking for task reminders...");

        LocalDateTime now = LocalDateTime.now();

        // Получаем все невыполненные задачи с дедлайном в будущем
        List<TodoTask> tasks = todoTaskRepository.findByIsCompletedFalseAndDeadlineAfter(now);

        for (TodoTask task : tasks) {
            if (task.getTeacher() == null || task.getTeacher().getReminderTime() == null) {
                continue;
            }

            // Пропускаем преподавателей, у которых подключен Google Calendar
            // (напоминания для них приходят через Google Calendar)
            if (googleOAuthService.isConnected(task.getTeacher())) {
                log.debug("Skipping reminder for task #{} - teacher #{} has Google Calendar connected",
                        task.getId(), task.getTeacher().getId());
                continue;
            }

            ReminderTime reminderTime = task.getTeacher().getReminderTime();
            int minutesBeforeDeadline = reminderTime.getMinutesBeforeDeadline();

            // Вычисляем время, когда нужно отправить напоминание
            LocalDateTime reminderDateTime = task.getDeadline().minusMinutes(minutesBeforeDeadline);

            // Проверяем, находится ли текущее время в интервале для отправки напоминания
            // Интервал: от reminderDateTime до reminderDateTime + 5 минут (частота проверки)
            if (now.isAfter(reminderDateTime) && now.isBefore(reminderDateTime.plusMinutes(5))) {
                sendReminder(task);
            }
        }
    }

    /**
     * Отправить напоминание о задаче в Telegram
     * Используется только для преподавателей без подключенного Google Calendar
     */
    private void sendReminder(TodoTask task) {
        try {
            Long chatId = task.getTeacher().getTelegramId();

            String message = String.format("""
                            ⏰ Напоминание о дедлайне задачи!
                            
                            📋 Задача: %s
                            📝 Описание: %s
                            ⏱️ Дедлайн: %s
                            
                            ⚠️ До дедлайна осталось: %s
                            
                            💡 Используйте "📋 Мои задачи" для просмотра деталей.
                            """,
                    task.getTitle(),
                    task.getDescription() != null ? task.getDescription() : "Не указано",
                    formatDeadline(task.getDeadline()),
                    task.getTeacher().getReminderTime().getDisplayName()
            );

            botMessenger.sendText(message, chatId);
            log.info("Sent reminder for task #{} to teacher #{}", task.getId(), task.getTeacher().getId());

        } catch (Exception e) {
            log.error("Failed to send reminder for task #{}: {}", task.getId(), e.getMessage());
        }
    }

    /**
     * Форматирование дедлайна для отображения
     */
    private String formatDeadline(LocalDateTime deadline) {
        return deadline.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
