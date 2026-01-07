package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.TodoTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.example.botforconsultations.core.util.TimeUtils.now;

/**
 * Утилита для форматирования сообщений преподавателя
 */
@Component
public class TeacherMessageFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Формирует список консультаций преподавателя
     */
    public String formatConsultationsList(List<Consultation> consultations) {
        if (consultations.isEmpty()) {
            return "📅 У вас пока нет консультаций.\n\n" +
                    "Создайте новую через \"➕ Создать консультацию\"";
        }

        StringBuilder message = new StringBuilder();
        message.append("📅 Ваши консультации:\n\n");

        for (Consultation consultation : consultations) {
            message.append(formatConsultationShort(consultation));
        }

        message.append("\n💡 Нажмите на консультацию для просмотра деталей\n");
        message.append("или введите номер в формате: №123");

        return message.toString();
    }

    /**
     * Краткий формат консультации для списка
     */
    private String formatConsultationShort(Consultation consultation) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("№%d\n", consultation.getId()));

        if (consultation.getDate() != null && consultation.getStartTime() != null) {
            message.append(String.format("📅 %s %s - %s\n",
                    consultation.getDate().format(SHORT_DATE_FORMATTER),
                    consultation.getStartTime().format(TIME_FORMATTER),
                    consultation.getEndTime().format(TIME_FORMATTER)));
        }

        if (consultation.getTitle() != null && !consultation.getTitle().isEmpty()) {
            message.append(String.format("📝 %s\n", consultation.getTitle()));
        }

        String statusEmoji = getStatusEmoji(consultation.getStatus());
        message.append(String.format("%s %s\n", statusEmoji, getStatusText(consultation.getStatus())));
        message.append("\n");

        return message.toString();
    }

    /**
     * Детальная информация о консультации для преподавателя
     */
    public String formatConsultationDetails(Consultation consultation, long registeredCount) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("📋 Консультация №%d\n\n", consultation.getId()));

        if (consultation.getTitle() != null && !consultation.getTitle().isEmpty()) {
            message.append(String.format("📝 Тема: %s\n\n", consultation.getTitle()));
        }

        if (consultation.getDate() != null) {
            message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        }

        if (consultation.getStartTime() != null && consultation.getEndTime() != null) {
            message.append(String.format("🕐 Время: %s - %s\n\n",
                    consultation.getStartTime().format(TIME_FORMATTER),
                    consultation.getEndTime().format(TIME_FORMATTER)));
        }

        // Статус
        String statusEmoji = getStatusEmoji(consultation.getStatus());
        message.append(String.format("Статус: %s %s\n\n", statusEmoji, getStatusText(consultation.getStatus())));

        // Вместимость - отображаем записанных студентов с учётом лимита
        message.append("👥 Записано студентов: ");
        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
            message.append(String.format("%d/%d\n", registeredCount, consultation.getCapacity()));
        } else {
            message.append(String.format("%d (без ограничений)\n", registeredCount));
        }

        // Автозакрытие
        message.append(String.format("🔒 Автозакрытие: %s\n",
                consultation.isAutoCloseOnCapacity() ? "включено" : "выключено"));

        message.append("\n💡 Выберите действие:");
        return message.toString();
    }

    /**
     * Список записанных студентов с их вопросами
     */
    public String formatRegisteredStudents(List<StudentConsultation> registrations) {
        if (registrations.isEmpty()) {
            return "👥 На эту консультацию пока никто не записался";
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format("👥 Список студентов (%d %s):\n\n",
                registrations.size(),
                getStudentWord(registrations.size())));

        int count = 1;
        for (StudentConsultation sc : registrations) {
            message.append(String.format("%d. %s\n",
                    count++,
                    TeacherNameFormatter.formatFullName(sc.getStudent())));

            if (sc.getMessage() != null && !sc.getMessage().isEmpty()) {
                message.append(String.format("   📝 Вопрос: %s\n", sc.getMessage()));
            }
            message.append("\n");
        }

        return message.toString();
    }

    /**
     * Список запросов студентов
     */
    public String formatRequestsList(List<Consultation> requests) {
        if (requests.isEmpty()) {
            return "📋 Пока нет запросов от студентов.\n\n" +
                    "Студенты могут создавать запросы через бота,\n" +
                    "и вы сможете принимать их, создав консультацию.";
        }

        StringBuilder message = new StringBuilder();
        message.append("📋 Запросы студентов на консультации:\n\n");

        for (Consultation request : requests) {
            int interestedCount = request.getRegUsers() != null ? request.getRegUsers().size() : 0;

            message.append(String.format("⏳ №%d - %s\n",
                    request.getId(),
                    request.getTitle()));
            message.append(String.format("   👤 Автор: %s\n",
                    TeacherNameFormatter.formatFullName(request.getTeacher())));
            message.append(String.format("   👥 Заинтересовано: %d\n",
                    interestedCount));
            message.append("\n");
        }

        message.append("💡 Нажмите на запрос для просмотра деталей");
        return message.toString();
    }

    /**
     * Детали конкретного запроса студента
     */
    public String formatRequestDetails(Consultation request, int interestedCount) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("📋 Запрос консультации №%d\n\n", request.getId()));

        message.append(String.format("📝 Тема: %s\n\n", request.getTitle()));

        message.append(String.format("👤 Автор запроса: %s\n",
                TeacherNameFormatter.formatFullName(request.getTeacher())));

        message.append(String.format("\n👥 Заинтересовано студентов: %d\n",
                interestedCount));

        message.append("📊 Статус: ⏳ Ожидает принятия\n");

        message.append("\n💡 Вы можете принять этот запрос и создать консультацию.\n");
        message.append("Все заинтересованные студенты автоматически запишутся на неё.");

        return message.toString();
    }

    // ========== Уведомления ==========

    /**
     * Уведомление о новой консультации (для подписчиков)
     */
    public String formatNewConsultationNotification(Consultation consultation) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 Новая консультация!\n\n");
        message.append(String.format("👨‍🏫 Преподаватель: %s\n\n",
                TeacherNameFormatter.formatFullName(consultation.getTeacher())));

        if (consultation.getTitle() != null && !consultation.getTitle().isEmpty()) {
            message.append(String.format("📝 Тема: %s\n\n", consultation.getTitle()));
        }

        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        message.append(String.format("🕐 Время: %s - %s\n\n",
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
            message.append(String.format("👥 Мест: %d\n", consultation.getCapacity()));
        } else {
            message.append("👥 Мест: без ограничений\n");
        }

//        message.append("\n✅ Вы можете записаться через:\n");
//        message.append("🔍 Преподаватели → выбрать преподавателя → выбрать консультацию");

        return message.toString();
    }

    /**
     * Уведомление об изменении консультации (для записанных студентов)
     */
    public String formatConsultationUpdateNotification(Consultation consultation, String changeDescription) {
        StringBuilder message = new StringBuilder();
        message.append("⚠️ Изменение в консультации!\n\n");
        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
        message.append(String.format("👨‍🏫 Преподаватель: %s\n\n",
                TeacherNameFormatter.formatFullName(consultation.getTeacher())));

        message.append(String.format("Что изменилось: %s\n\n", changeDescription));

        if (consultation.getTitle() != null) {
            message.append(String.format("📝 Тема: %s\n", consultation.getTitle()));
        }
        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        message.append(String.format("🕐 Время: %s - %s\n",
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        return message.toString();
    }

    /**
     * Уведомление о появлении мест (для подписчиков, не записанных)
     */
    public String formatAvailableSpotsNotification(Consultation consultation, long currentCount) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 Освободилось место!\n\n");
        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
        message.append(String.format("👨‍🏫 Преподаватель: %s\n\n",
                TeacherNameFormatter.formatFullName(consultation.getTeacher())));

        if (consultation.getTitle() != null) {
            message.append(String.format("📝 Тема: %s\n\n", consultation.getTitle()));
        }

        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        message.append(String.format("🕐 Время: %s - %s\n\n",
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
            long availableSpots = consultation.getCapacity() - currentCount;
            message.append(String.format("👥 Свободных мест: %d\n\n", availableSpots));
        }

        message.append("✅ Запись теперь открыта!");

        return message.toString();
    }

    /**
     * Уведомление об отмене консультации (для записанных студентов)
     */
    public String formatCancellationNotification(Consultation consultation) {
        StringBuilder message = new StringBuilder();
        message.append("❌ Консультация отменена\n\n");
        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
        message.append(String.format("👨‍🏫 Преподаватель: %s\n\n",
                TeacherNameFormatter.formatFullName(consultation.getTeacher())));

        if (consultation.getTitle() != null) {
            message.append(String.format("📝 Тема: %s\n\n", consultation.getTitle()));
        }

        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        message.append(String.format("🕐 Время: %s - %s\n\n",
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        // if (consultation.getClosedReason() != null && !consultation.getClosedReason().isEmpty()) {
        //     message.append(String.format("Причина: %s\n\n", consultation.getClosedReason()));
        // }

        return message.toString();
    }

    // ========== Вспомогательные методы ==========

    /**
     * Получить эмодзи для статуса
     */
    private String getStatusEmoji(ConsultationStatus status) {
        return switch (status) {
            case OPEN -> "✅";
            case CLOSED -> "🔒";
            case CANCELLED -> "❌";
            case REQUEST -> "⏳";
        };
    }

    /**
     * Получить текстовое описание статуса
     */
    private String getStatusText(ConsultationStatus status) {
        return switch (status) {
            case OPEN -> "Открыта для записи";
            case CLOSED -> "Запись закрыта";
            case CANCELLED -> "Отменена";
            case REQUEST -> "Запрос от студента";
        };
    }

    /**
     * Правильное склонение слова "студент"
     */
    private String getStudentWord(int count) {
        if (count % 10 == 1 && count % 100 != 11) {
            return "студент";
        } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
            return "студента";
        } else {
            return "студентов";
        }
    }

    // ========== Форматирование задач ==========

    /**
     * Форматировать список задач преподавателя
     */
    public String formatMyTasksList(List<TodoTask> tasks, String statusFilter, String deadlineFilter) {
        StringBuilder message = new StringBuilder();
        message.append("📋 Мои задачи\n\n");

        if (tasks.isEmpty()) {
            message.append("❌ У вас пока нет задач");
            return message.toString();
        }

        // Фильтры
        String filterText = getTaskFilterText(statusFilter, deadlineFilter);
        if (!filterText.isEmpty()) {
            message.append(filterText).append("\n\n");
        }

        LocalDateTime currentTime = now();

        for (TodoTask task : tasks) {
            message.append(formatTaskShort(task, currentTime));
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
        String statusEmoji = task.getIsCompleted() ? "✅" : "⏳";
        message.append(String.format("%s №%d - ", statusEmoji, task.getId()));

        // Заголовок (обрезаем если длинный)
        String title = task.getTitle();
        if (title.length() > 40) {
            title = title.substring(0, 40) + "...";
        }
        message.append(title).append("\n");

        // Дедлайн
        LocalDateTime deadline = task.getDeadline();
        message.append(String.format("   ⏰ %s",
                deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

        // Индикатор просрочки
        if (!task.getIsCompleted() && deadline.isBefore(now)) {
            message.append(" ⚠️ ПРОСРОЧЕНО");
        } else if (!task.getIsCompleted() && deadline.isBefore(now.plusDays(1))) {
            message.append(" 🔥 Срочно");
        }

        message.append("\n\n");

        return message.toString();
    }

    /**
     * Детальная информация о задаче для преподавателя
     */
    public static String formatTaskDetails(TodoTask task) {
        StringBuilder message = new StringBuilder();
        LocalDateTime currentTime = now();

        message.append(String.format("📋 Задача №%d\n\n", task.getId()));

        // Название
        message.append(String.format("📝 *%s*\n\n", task.getTitle()));

        // Описание
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            message.append(String.format("📄 Описание:\n%s\n\n", task.getDescription()));
        }

        // Дедлайн
        if (task.getDeadline() != null) {
            message.append(String.format("⏰ Дедлайн: %s\n",
                    task.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

            // Статус просрочки/времени
            if (!task.getIsCompleted() && task.getDeadline().isBefore(currentTime)) {
                long daysOverdue = java.time.Duration.between(task.getDeadline(), currentTime).toDays();
                message.append(String.format("⚠️ Просрочено на %d %s\n",
                        daysOverdue, getDaysWord(daysOverdue)));
            } else if (!task.getIsCompleted()) {
                long daysLeft = java.time.Duration.between(currentTime, task.getDeadline()).toDays();
                if (daysLeft == 0) {
                    message.append("⏳ Дедлайн сегодня!\n");
                } else {
                    message.append(String.format("⏳ Осталось %d %s\n",
                            daysLeft, getDaysWord(daysLeft)));
                }
            }
            message.append("\n");
        }

        // Статус
        String statusIcon = task.getIsCompleted() ? "✅" : "⏳";
        String statusText = task.getIsCompleted() ? "Выполнена" : "В работе";
        message.append(String.format("Статус: %s %s\n", statusIcon, statusText));

        // Дата выполнения
        if (task.getIsCompleted() && task.getCompletedAt() != null) {
            message.append(String.format("Выполнена: %s\n",
                    task.getCompletedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        }

        // Кто создал
        if (task.getCreatedBy() != null) {
            message.append(String.format("\n👤 Создано: %s\n",
                    TeacherNameFormatter.formatFullName(task.getCreatedBy())));
        }

        message.append("\n💡 Выберите действие:");
        return message.toString();
    }

    /**
     * Получить текст фильтров для задач
     */
    private String getTaskFilterText(String statusFilter, String deadlineFilter) {
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
            case "past" -> "Просроченные";
            case "future" -> "Будущие";
            default -> "Все";
        };
    }

    /**
     * Склонение слова "день"
     */
    private static String getDaysWord(long days) {
        if (days % 10 == 1 && days % 100 != 11) {
            return "день";
        } else if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) {
            return "дня";
        } else {
            return "дней";
        }
    }
}

