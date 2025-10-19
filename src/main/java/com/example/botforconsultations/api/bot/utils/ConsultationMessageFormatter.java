package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.Subscription;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Утилита для форматирования сообщений о консультациях
 */
@Component
public class ConsultationMessageFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Формирует сообщение со списком консультаций преподавателя
     */
    public String formatConsultationsList(TelegramUser teacher, List<Consultation> consultations, String filter) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Консультации преподавателя %s %s:\n\n",
                teacher.getFirstName(),
                teacher.getLastName() != null ? teacher.getLastName() : ""));

        message.append(getFilterText(filter)).append("\n\n");

        if (consultations.isEmpty()) {
            message.append("Нет консультаций для отображения.\n");
        } else {
            for (Consultation consultation : consultations) {
                message.append(formatConsultationShort(consultation));
            }
            message.append("\n💡 Нажмите на кнопку с консультацией\n");
            message.append("или введите номер в формате: №123\n");
        }
        return message.toString();
    }

    /**
     * Краткий формат консультации для списка
     */
    public String formatConsultationShort(Consultation consultation) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("№%d\n", consultation.getId()));
        message.append(String.format("📅 %s %s - %s\n",
                consultation.getDate().format(SHORT_DATE_FORMATTER),
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        if (consultation.getTitle() != null && !consultation.getTitle().isEmpty()) {
            message.append(String.format("📝 %s\n", consultation.getTitle()));
        }

        // Добавляем статус консультации
        String statusEmoji = switch (consultation.getStatus()) {
            case OPEN -> "✅ Открыта";
            case CLOSED -> "🔒 Закрыта";
            case CANCELLED -> "❌ Отменена";
            case REQUEST -> "⏳ Запрос";
        };
        message.append(String.format("%s\n", statusEmoji));

        message.append("\n");
        return message.toString();
    }

    /**
     * Детальная информация о консультации
     */
    public String formatConsultationDetails(Consultation consultation, long registeredCount, StudentConsultation studentRegistration) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("📋 Консультация №%d\n\n", consultation.getId()));
        message.append(String.format("👨‍🏫 Преподаватель: %s %s\n",
                consultation.getTeacher().getFirstName(),
                consultation.getTeacher().getLastName() != null ? consultation.getTeacher().getLastName() : ""));
        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        message.append(String.format("🕐 Время: %s - %s\n",
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        if (consultation.getTitle() != null && !consultation.getTitle().isEmpty()) {
            message.append(String.format("\n📝 Тема: %s\n", consultation.getTitle()));
        }

        // Если студент зарегистрирован - показываем его сообщение
        if (studentRegistration != null && studentRegistration.getMessage() != null && !studentRegistration.getMessage().isEmpty()) {
            message.append(String.format("\n💬 Ваш вопрос: %s\n", studentRegistration.getMessage()));
        }

        // Отображаем количество записанных студентов с учётом вместимости
        message.append("\n👥 Записано студентов: ");
        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
            message.append(String.format("%d/%d", registeredCount, consultation.getCapacity()));
        } else {
            message.append(String.format("%d (без ограничений)", registeredCount));
        }

        // Добавляем статус консультации
        String statusText = switch (consultation.getStatus()) {
            case OPEN -> "✅ Открыта для записи";
            case CLOSED -> "🔒 Запись закрыта";
            case CANCELLED -> "❌ Консультация отменена";
            case REQUEST -> "⏳ Запрос на консультацию";
        };
        message.append(String.format("\n📊 Статус: %s", statusText));

        message.append("\n\n💡 Выберите действие:");
        return message.toString();
    }

    /**
     * Сообщение о подтверждении записи
     */
    public String formatRegistrationConfirmation() {//(Consultation consultation, String studentMessage, long registeredCount) {
        StringBuilder message = new StringBuilder();
        message.append("✅ Вы успешно записались на консультацию!\n");
//        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
//        message.append(String.format("👨‍🏫 Преподаватель: %s %s\n",
//                consultation.getTeacher().getFirstName(),
//                consultation.getTeacher().getLastName() != null ? consultation.getTeacher().getLastName() : ""));
//        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
//        message.append(String.format("🕐 Время: %s - %s\n",
//                consultation.getStartTime().format(TIME_FORMATTER),
//                consultation.getEndTime().format(TIME_FORMATTER)));
//        message.append(String.format("\n📝 Ваш вопрос: %s\n", studentMessage));
//
//        // Отображаем количество записанных студентов с учётом вместимости
//        message.append("\n👥 Записано студентов: ");
//        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
//            message.append(String.format("%d/%d", registeredCount, consultation.getCapacity()));
//        } else {
//            message.append(String.format("%d (без ограничений)", registeredCount));
//        }

        return message.toString();
    }

    /**
     * Сообщение об отмене записи
     */
    public String formatCancellationConfirmation() {//(Consultation consultation) {
        StringBuilder message = new StringBuilder();
        message.append("❌ Запись отменена\n");
//        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
//        message.append(String.format("👨‍🏫 Преподаватель: %s %s\n",
//                consultation.getTeacher().getFirstName(),
//                consultation.getTeacher().getLastName() != null ? consultation.getTeacher().getLastName() : ""));
//        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
//        message.append(String.format("🕐 Время: %s - %s\n",
//                consultation.getStartTime().format(TIME_FORMATTER),
//                consultation.getEndTime().format(TIME_FORMATTER)));
        return message.toString();
    }

    /**
     * Список записей студента
     */
    public String formatStudentRegistrations(List<StudentConsultation> registrations) {
        if (registrations.isEmpty()) {
            return "У вас пока нет записей на консультации.\n\n" +
                    "Используйте \"🔍 Преподаватели\" для поиска консультаций.";
        }

        LocalDate today = LocalDate.now();
        List<StudentConsultation> futureRegistrations = new java.util.ArrayList<>();
        List<StudentConsultation> pastRegistrations = new java.util.ArrayList<>();

        // Разделяем на будущие и прошедшие (сегодняшние считаются будущими)
        for (StudentConsultation sc : registrations) {
            Consultation consultation = sc.getConsultation();
            if (consultation.getDate().isAfter(today) || consultation.getDate().isEqual(today)) {
                futureRegistrations.add(sc);
            } else {
                pastRegistrations.add(sc);
            }
        }

        StringBuilder message = new StringBuilder();
        message.append("📝 Ваши записи на консультации\n\n");

        if (!futureRegistrations.isEmpty()) {
            message.append("📅 Предстоящие консультации:\n\n");
            futureRegistrations.forEach(sc -> message.append(formatStudentConsultation(sc, true)));
        }

        if (!pastRegistrations.isEmpty()) {
            message.append("📆 Прошедшие консультации:\n\n");
            pastRegistrations.forEach(sc -> message.append(formatStudentConsultation(sc, false)));
        }

        message.append(String.format("\nВсего записей: %d", registrations.size()));
        if (!futureRegistrations.isEmpty() || !pastRegistrations.isEmpty()) {
            message.append(String.format(" (предстоящих: %d, прошедших: %d)",
                    futureRegistrations.size(), pastRegistrations.size()));
        }

        return message.toString();
    }

    /**
     * Форматирование одной записи студента
     */
    private String formatStudentConsultation(StudentConsultation sc, boolean includeDetails) {
        Consultation consultation = sc.getConsultation();
        StringBuilder message = new StringBuilder();
        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
        message.append(String.format("👨‍🏫 Преподаватель: %s %s\n",
                consultation.getTeacher().getFirstName(),
                consultation.getTeacher().getLastName() != null ? consultation.getTeacher().getLastName() : ""));
        message.append(String.format("📅 Дата: %s\n", consultation.getDate().format(DATE_FORMATTER)));
        message.append(String.format("🕐 Время: %s - %s\n",
                consultation.getStartTime().format(TIME_FORMATTER),
                consultation.getEndTime().format(TIME_FORMATTER)));

        if (includeDetails) {
            if (sc.getMessage() != null && !sc.getMessage().isEmpty()) {
                message.append(String.format("📝 Ваш вопрос: %s\n", sc.getMessage()));
            }

            if (consultation.getStatus() != null) {
                String statusEmoji = switch (consultation.getStatus()) {
                    case OPEN -> "✅";
                    case CLOSED -> "🔒";
                    case CANCELLED -> "❌";
                    case REQUEST -> "⏳";
                };
                message.append(String.format("%s Статус: %s\n", statusEmoji, consultation.getStatus()));
            }
        }

        message.append("\n");
        return message.toString();
    }

    /**
     * Список подписок студента
     */
    public String formatSubscriptions(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return "У вас пока нет подписок на обновления преподавателей.\n\n" +
                    "Вы можете подписаться на преподавателя при просмотре его консультаций.\n" +
                    "Используйте \"🔍 Преподаватели\" → выберите преподавателя → \"🔔 Подписаться\"";
        }

        StringBuilder message = new StringBuilder();
        message.append("🔔 Ваши подписки на обновления:\n\n");
        message.append("Вы получите уведомление при изменении расписания следующих преподавателей:\n\n");

        for (Subscription subscription : subscriptions) {
            TelegramUser teacher = subscription.getTeacher();
            message.append(TeacherNameFormatter.formatFullName(teacher));
            message.append("\n");
        }

        message.append(String.format("\nВсего подписок: %d\n\n", subscriptions.size()));
        message.append("Для отмены подписки выберите преподавателя в разделе \"🔍 Преподаватели\" " +
                "и нажмите \"🔕 Отписаться\"");

        return message.toString();
    }

    /**
     * Получить текст фильтра
     */
    private String getFilterText(String filter) {
        return switch (filter) {
            case "future" -> "📅 Показаны будущие консультации";
            case "past" -> "📅 Показаны прошедшие консультации";
            default -> "📅 Показаны все консультации";
        };
    }

    /**
     * Список запросов консультаций студента
     */
    public String formatRequestsList(List<Consultation> requests) {
        if (requests.isEmpty()) {
            return "❓ Пока нет запросов консультаций от студентов.\n\n" +
                    "Любой студент может создать запрос через главное меню:\n" +
                    "\"❓ Запросить консультацию\"";
        }

        StringBuilder message = new StringBuilder();
        message.append("📋 Запросы консультаций от студентов:\n\n");

        for (Consultation request : requests) {
            String statusEmoji = switch (request.getStatus()) {
                case REQUEST -> "⏳";
                case OPEN -> "✅";
                case CLOSED -> "🔒";
                case CANCELLED -> "❌";
            };

            int interestedCount = request.getRegUsers() != null ? request.getRegUsers().size() : 0;

            message.append(String.format("%s №%d - %s\n",
                    statusEmoji,
                    request.getId(),
                    request.getTitle()));
            message.append(String.format("   👤 Автор: %s\n",
                    TeacherNameFormatter.formatFullName(request.getTeacher())));
            message.append(String.format("   👥 Заинтересовано: %d\n",
                    interestedCount));
            message.append("\n");
        }

        message.append("💡 Выберите запрос для просмотра деталей");
        return message.toString();
    }

    /**
     * Детали конкретного запроса
     */
    public String formatRequestDetails(Consultation request) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("📋 Запрос консультации №%d\n\n", request.getId()));

        message.append(String.format("📝 Тема: %s\n\n", request.getTitle()));

        message.append(String.format("👤 Автор запроса: %s\n",
                TeacherNameFormatter.formatFullName(request.getTeacher())));

        // Количество заинтересованных студентов
        int interestedCount = request.getRegUsers() != null ? request.getRegUsers().size() : 0;
        message.append(String.format("\n👥 Заинтересовано студентов: %d\n",
                interestedCount));

        // Статус запроса
        String statusText = switch (request.getStatus()) {
            case REQUEST -> "⏳ Ожидает обработки преподавателем";
            case OPEN -> "✅ Принят преподавателем";
            case CLOSED -> "🔒 Запрос закрыт";
            case CANCELLED -> "❌ Запрос отменён";
        };
        message.append(String.format("📊 Статус: %s\n", statusText));

        message.append("\n💡 Выберите действие:");

        return message.toString();
    }

    /**
     * Подтверждение создания запроса
     */
    public String formatRequestCreationConfirmation(Consultation request) {
        return String.format(
                "✅ Запрос консультации успешно создан!\n\n" +
                        "🆔 Номер запроса: %d\n" +
                        "📝 Тема: %s\n\n" +
                        "Ваш запрос отправлен преподавателям. " +
                        "Когда кто-то из них примет запрос, вы получите уведомление.\n\n" +
                        "Вы можете просмотреть все свои запросы в разделе \"📋 Просмотреть запросы\"",
                request.getId(),
                request.getTitle()
        );
    }
}

