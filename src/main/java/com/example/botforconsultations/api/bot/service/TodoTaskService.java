package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.repository.TodoTaskRepository;
import com.example.botforconsultations.core.service.GoogleCalendarService;
import com.example.botforconsultations.core.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoTaskService {

    private final TodoTaskRepository todoTaskRepository;
    private final GoogleOAuthService googleOAuthService;
    private final GoogleCalendarService googleCalendarService;

    /**
     * Создать новую задачу для преподавателя
     */
    @Transactional
    public TodoTask createTodoForTeacher(TelegramUser teacher, TelegramUser createdBy, String title,
                                         String description, LocalDateTime deadline) {
        TodoTask todo = TodoTask.builder()
                .teacher(teacher)
                .createdBy(createdBy)
                .title(title)
                .description(description)
                .deadline(deadline)
                .isCompleted(false)
                .reminderSent(false)
                .build();

        TodoTask saved = todoTaskRepository.save(todo);
        log.info("Created todo task {} for teacher {} by user {}",
                saved.getId(), teacher.getId(), createdBy.getId());

        // Проверяем, подключен ли у преподавателя Google Calendar
        if (googleOAuthService.isConnected(teacher)) {
            try {
                Optional<String> eventIdOpt = googleCalendarService.createTaskEvent(teacher, saved);
                if (eventIdOpt.isPresent()) {
                    saved.setGoogleCalendarEventId(eventIdOpt.get());
                    saved = todoTaskRepository.save(saved);
                    log.info("Created Google Calendar event {} for task {}", eventIdOpt.get(), saved.getId());
                }
            } catch (Exception e) {
                log.error("Failed to create Google Calendar event for task {}: {}",
                        saved.getId(), e.getMessage());
            }
        }

        return saved;
    }

    /**
     * Получить все задачи преподавателя
     */
    public List<TodoTask> getTeacherTasks(TelegramUser teacher) {
        return todoTaskRepository.findByTeacherOrderByDeadlineAsc(teacher);
    }

    /**
     * Получить все задачи преподавателя по ID
     */
    public List<TodoTask> getTasksByTeacherId(Long teacherId) {
        return todoTaskRepository.findByTeacher_IdOrderByDeadlineAsc(teacherId);
    }

    /**
     * Получить задачу по ID
     */
    public Optional<TodoTask> getTaskById(Long taskId) {
        return todoTaskRepository.findById(taskId);
    }

    /**
     * Получить активные задачи преподавателя
     */
    public List<TodoTask> getActiveTeacherTasks(TelegramUser teacher) {
        return todoTaskRepository.findByTeacherAndIsCompletedFalseOrderByDeadlineAsc(teacher);
    }

    /**
     * Получить выполненные задачи преподавателя
     */
    public List<TodoTask> getCompletedTeacherTasks(TelegramUser teacher) {
        return todoTaskRepository.findByTeacherAndIsCompletedTrueOrderByCompletedAtDesc(teacher);
    }

    /**
     * Отметить задачу как выполненную
     */
    @Transactional
    public void markAsCompleted(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();
            todo.setIsCompleted(true);
            todo.setCompletedAt(LocalDateTime.now());
            todoTaskRepository.save(todo);
            log.info("Todo task {} marked as completed", todoId);

            // Обновляем событие в Google Calendar (меняем цвет на зеленый)
            if (todo.getGoogleCalendarEventId() != null) {
                try {
                    googleCalendarService.markEventAsCompleted(
                            todo.getTeacher(),
                            todo.getGoogleCalendarEventId());
                    log.info("Marked Google Calendar event {} as completed",
                            todo.getGoogleCalendarEventId());
                } catch (Exception e) {
                    log.error("Failed to mark Google Calendar event as completed: {}",
                            e.getMessage());
                }
            }
        }
    }

    /**
     * Отменить выполнение задачи
     */
    @Transactional
    public void markAsIncomplete(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();
            todo.setIsCompleted(false);
            todo.setCompletedAt(null);
            todoTaskRepository.save(todo);
            log.info("Todo task {} marked as incomplete", todoId);

            // Обновляем событие в Google Calendar (возвращаем красный цвет)
            if (todo.getGoogleCalendarEventId() != null) {
                try {
                    googleCalendarService.updateTaskEvent(
                            todo.getTeacher(),
                            todo,
                            todo.getGoogleCalendarEventId());
                    log.info("Updated Google Calendar event {} (unmarked as completed)",
                            todo.getGoogleCalendarEventId());
                } catch (Exception e) {
                    log.error("Failed to update Google Calendar event: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Удалить задачу
     */
    @Transactional
    public void deleteTodo(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();

            // Удаляем событие из Google Calendar
            if (todo.getGoogleCalendarEventId() != null) {
                try {
                    googleCalendarService.deleteTaskEvent(
                            todo.getTeacher(),
                            todo.getGoogleCalendarEventId());
                    log.info("Deleted Google Calendar event {}", todo.getGoogleCalendarEventId());
                } catch (Exception e) {
                    log.error("Failed to delete Google Calendar event: {}", e.getMessage());
                }
            }

            todoTaskRepository.deleteById(todoId);
            log.info("Todo task {} deleted", todoId);
        } else {
            todoTaskRepository.deleteById(todoId);
            log.info("Todo task {} deleted (no calendar event)", todoId);
        }
    }

    /**
     * Получить задачу по ID
     */
    public Optional<TodoTask> getTodoById(Long todoId) {
        return todoTaskRepository.findById(todoId);
    }

    /**
     * Обновить задачу
     */
    @Transactional
    public TodoTask updateTodo(Long todoId, String title, String description,
                               LocalDateTime deadline) {
        TodoTask todo = todoTaskRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));

        todo.setTitle(title);
        todo.setDescription(description);
        todo.setDeadline(deadline);

        return todoTaskRepository.save(todo);
    }

    /**
     * Получить задачи, которым нужно отправить напоминание
     */
    public List<TodoTask> getTasksNeedingReminder(LocalDateTime now, LocalDateTime reminderTime) {
        return todoTaskRepository.findTasksNeedingReminder(now, reminderTime);
    }

    /**
     * Отметить что напоминание отправлено
     */
    @Transactional
    public void markReminderSent(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();
            todo.setReminderSent(true);
            todoTaskRepository.save(todo);
            log.info("Reminder sent for todo task {}", todoId);
        }
    }

    /**
     * Получить просроченные задачи
     */
    public List<TodoTask> getOverdueTasks() {
        return todoTaskRepository.findOverdueTasks(LocalDateTime.now());
    }

    /**
     * Получить все активные задачи (для деканата)
     */
    public List<TodoTask> getAllActiveTasks() {
        return todoTaskRepository.findByIsCompletedFalseOrderByDeadlineAsc();
    }

    /**
     * Обновить название задачи
     */
    @Transactional
    public void updateTitle(Long todoId, String newTitle) {
        TodoTask todo = todoTaskRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo task not found: " + todoId));

        todo.setTitle(newTitle);
        todoTaskRepository.save(todo);
        log.info("Updated title for todo task {}", todoId);
    }

    /**
     * Обновить описание задачи
     */
    @Transactional
    public void updateDescription(Long todoId, String newDescription) {
        TodoTask todo = todoTaskRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo task not found: " + todoId));

        todo.setDescription(newDescription);
        todoTaskRepository.save(todo);
        log.info("Updated description for todo task {}", todoId);
    }

    /**
     * Обновить дедлайн задачи
     */
    @Transactional
    public void updateDeadline(Long todoId, LocalDateTime newDeadline) {
        TodoTask todo = todoTaskRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo task not found: " + todoId));

        todo.setDeadline(newDeadline);
        // Сбрасываем флаг напоминания, если дедлайн изменён
        todo.setReminderSent(false);
        todoTaskRepository.save(todo);
        log.info("Updated deadline for todo task {} to {}", todoId, newDeadline);

        // Обновляем событие в Google Calendar
        if (todo.getGoogleCalendarEventId() != null) {
            try {
                googleCalendarService.updateTaskEvent(
                        todo.getTeacher(),
                        todo,
                        todo.getGoogleCalendarEventId());
                log.info("Updated Google Calendar event {} with new deadline",
                        todo.getGoogleCalendarEventId());
            } catch (Exception e) {
                log.error("Failed to update Google Calendar event: {}", e.getMessage());
            }
        }
    }

    /**
     * Сохранить задачу (вспомогательный метод)
     */
    @Transactional
    public TodoTask saveTask(TodoTask task) {
        return todoTaskRepository.save(task);
    }
}
