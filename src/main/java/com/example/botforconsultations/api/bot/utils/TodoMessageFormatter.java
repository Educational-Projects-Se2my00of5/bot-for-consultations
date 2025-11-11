package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Форматтер сообщений для задач (Todo)
 */
@Component
public class TodoMessageFormatter {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Форматировать список задач преподавателя
     */
    public String formatTeacherTasksList(TelegramUser teacher, List<TodoTask> tasks, String statusFilter, String deadlineFilter) {
        StringBuilder message = new StringBuilder();
        message.append("📋 Задачи преподавателя\n\n");
        message.append(String.format("👨‍🏫 %s %s\n\n",
                teacher.getFirstName(),
                teacher.getLastName() != null ? teacher.getLastName() : ""));

        if (tasks.isEmpty()) {
            message.append("❌ Задач не найдено");
            return message.toString();
        }

        // Фильтр текст
        String filterText = getFilterText(statusFilter, deadlineFilter);
        if (!filterText.isEmpty()) {
            message.append(filterText).append("\n\n");
        }

        LocalDateTime now = LocalDateTime.now();
        
        for (TodoTask task : tasks) {
            message.append(formatTaskShort(task, now));
        }

        message.append(String.format("\nВсего задач: %d", tasks.size()));
        message.append("\n\n💡 Введите №... для просмотра деталей задачи");

        return message.toString();
    }

    /**
     * Форматировать список всех задач (для деканата)
     */
    public String formatAllTasksList(List<TodoTask> tasks, String statusFilter, String deadlineFilter) {
        StringBuilder message = new StringBuilder();
        message.append("📋 Все задачи в системе\n\n");

        if (tasks.isEmpty()) {
            message.append("❌ Задач не найдено");
            return message.toString();
        }

        // Фильтр текст
        String filterText = getFilterText(statusFilter, deadlineFilter);
        if (!filterText.isEmpty()) {
            message.append(filterText).append("\n\n");
        }

        LocalDateTime now = LocalDateTime.now();
        
        for (TodoTask task : tasks) {
            message.append(formatTaskShort(task, now));
        }

        message.append(String.format("\nВсего задач: %d", tasks.size()));
        message.append("\n\n💡 Введите №... для просмотра деталей задачи");

        return message.toString();
    }

    /**
     * Форматировать краткую информацию о задаче для списка
     */
    private String formatTaskShort(TodoTask task, LocalDateTime now) {
        StringBuilder message = new StringBuilder();
        
        // Номер и статус
        String statusEmoji = task.getIsCompleted() ? "✅" : "❌";
        message.append(String.format("%s №%d - ", statusEmoji, task.getId()));
        
        // Заголовок (обрезаем если длинный)
        String title = task.getTitle();
        if (title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }
        message.append(title).append("\n");
        
        // Преподаватель
        TelegramUser teacher = task.getTeacher();
        message.append(String.format("   👨‍🏫 %s %s\n",
                teacher.getFirstName(),
                teacher.getLastName() != null ? teacher.getLastName() : ""));
        
        // Дедлайн
        LocalDateTime deadline = task.getDeadline();
        message.append(String.format("   ⏰ %s",
                deadline.format(DATETIME_FORMATTER)));
        
        // Индикатор просрочки
        if (!task.getIsCompleted() && deadline.isBefore(now)) {
            message.append(" ⚠️ ПРОСРОЧЕНО");
        }
        
        message.append("\n\n");
        
        return message.toString();
    }

    /**
     * Форматировать детали задачи
     */
    public String formatTaskDetails(TodoTask task) {
        StringBuilder message = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        
        message.append("📋 Детали задачи\n\n");
        
        // Номер и статус
        String statusEmoji = task.getIsCompleted() ? "✅" : "❌";
        String statusText = task.getIsCompleted() ? "Выполнена" : "Не выполнена";
        message.append(String.format("№%d %s %s\n\n", task.getId(), statusEmoji, statusText));
        
        // Заголовок
        message.append(String.format("📌 Заголовок:\n%s\n\n", task.getTitle()));
        
        // Описание
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            message.append(String.format("📝 Описание:\n%s\n\n", task.getDescription()));
        }
        
        // Преподаватель
        TelegramUser teacher = task.getTeacher();
        message.append(String.format("👨‍🏫 Преподаватель: %s %s\n",
                teacher.getFirstName(),
                teacher.getLastName() != null ? teacher.getLastName() : ""));
        
        // Создатель
        TelegramUser createdBy = task.getCreatedBy();
        if (createdBy != null) {
            message.append(String.format("👤 Создал: %s %s\n",
                    createdBy.getFirstName(),
                    createdBy.getLastName() != null ? createdBy.getLastName() : ""));
        }
        
        // Дедлайн
        LocalDateTime deadline = task.getDeadline();
        message.append(String.format("⏰ Дедлайн: %s\n",
                deadline.format(DATETIME_FORMATTER)));
        
        // Статус просрочки
        if (!task.getIsCompleted() && deadline.isBefore(now)) {
            long daysOverdue = java.time.Duration.between(deadline, now).toDays();
            message.append(String.format("⚠️ Просрочено на %d %s\n",
                    daysOverdue, getDaysWord(daysOverdue)));
        } else if (!task.getIsCompleted()) {
            long daysLeft = java.time.Duration.between(now, deadline).toDays();
            if (daysLeft == 0) {
                message.append("⏳ Дедлайн сегодня!\n");
            } else {
                message.append(String.format("⏳ Осталось %d %s\n",
                        daysLeft, getDaysWord(daysLeft)));
            }
        }
        
        // Дата выполнения
        if (task.getIsCompleted() && task.getCompletedAt() != null) {
            message.append(String.format("✓ Выполнено: %s\n",
                    task.getCompletedAt().format(DATETIME_FORMATTER)));
        }
        
        return message.toString();
    }

    /**
     * Получить текст фильтров
     */
    private String getFilterText(String statusFilter, String deadlineFilter) {
        StringBuilder text = new StringBuilder("🔍 Фильтры: ");
        boolean hasFilters = false;
        
        if (statusFilter != null && !statusFilter.equals("all")) {
            text.append(getStatusFilterText(statusFilter));
            hasFilters = true;
        }
        
        if (deadlineFilter != null && !deadlineFilter.equals("all")) {
            if (hasFilters) text.append(", ");
            text.append(getDeadlineFilterText(deadlineFilter));
            hasFilters = true;
        }
        
        return hasFilters ? text.toString() : "";
    }

    /**
     * Получить текст фильтра статуса
     */
    private String getStatusFilterText(String filter) {
        return switch (filter) {
            case "completed" -> "Выполненные";
            case "incomplete" -> "Невыполненные";
            default -> "Все";
        };
    }

    /**
     * Получить текст фильтра дедлайна
     */
    private String getDeadlineFilterText(String filter) {
        return switch (filter) {
            case "overdue" -> "Просроченные";
            case "today" -> "Сегодня";
            case "week" -> "На неделю";
            case "future" -> "Будущие";
            default -> "Все";
        };
    }

    /**
     * Склонение слова "день"
     */
    private String getDaysWord(long days) {
        if (days % 10 == 1 && days % 100 != 11) {
            return "день";
        } else if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) {
            return "дня";
        } else {
            return "дней";
        }
    }
}
