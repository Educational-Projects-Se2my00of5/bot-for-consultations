package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.StudentConsultation;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

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

        // Вместимость и автозакрытие
        message.append(String.format("👥 Записано студентов: %d", registeredCount));
        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
            message.append(String.format("/%d", consultation.getCapacity()));
            if (consultation.isAutoCloseOnCapacity()) {
                message.append(" (автозакрытие включено)");
            }
        } else {
            message.append(" (без ограничений)");
        }

        message.append("\n\n💡 Выберите действие:");
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
            message.append(formatRequestShort(request));
        }

        message.append("\n💡 Нажмите на запрос для просмотра деталей");
        return message.toString();
    }

    /**
     * Краткий формат запроса для списка
     */
    private String formatRequestShort(Consultation request) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("🆔 №%d\n", request.getId()));
        message.append(String.format("📝 %s\n", request.getTitle()));
        message.append(String.format("👤 Автор: %s\n",
                TeacherNameFormatter.formatFullName(request.getTeacher()))); // teacher = студент для запросов
        message.append("⏳ Ожидает принятия\n");
        message.append("───────────────\n");
        return message.toString();
    }

    /**
     * Детали конкретного запроса студента
     */
    public String formatRequestDetails(Consultation request, int interestedCount) {
        StringBuilder message = new StringBuilder();
        message.append("📋 Детали запроса студента\n\n");
        message.append(String.format("🆔 Номер: %d\n", request.getId()));
        message.append(String.format("📝 Тема: %s\n\n", request.getTitle()));
        message.append(String.format("👤 Автор запроса: %s\n",
                TeacherNameFormatter.formatFullName(request.getTeacher())));
        
        message.append(String.format("\n👥 Заинтересовано %s: %d\n\n",
                getStudentWordGenitive(interestedCount),
                interestedCount));

        message.append("💡 Вы можете принять этот запрос и создать консультацию.\n");
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

        message.append("\n✅ Вы можете записаться через:\n");
        message.append("🔍 Преподаватели → выбрать преподавателя → выбрать консультацию");

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

        message.append("✅ Запись теперь открыта! Вы можете записаться.");

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

        if (consultation.getClosedReason() != null && !consultation.getClosedReason().isEmpty()) {
            message.append(String.format("Причина: %s\n\n", consultation.getClosedReason()));
        }

        message.append("Приносим извинения за неудобства.");

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

    /**
     * Склонение "студентов" в родительном падеже
     */
    private String getStudentWordGenitive(int count) {
        if (count % 10 == 1 && count % 100 != 11) {
            return "студент";
        } else {
            return "студентов";
        }
    }
}
