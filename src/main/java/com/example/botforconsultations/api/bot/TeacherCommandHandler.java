package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.ConsultationRequestService;
import com.example.botforconsultations.api.bot.service.NotificationService;
import com.example.botforconsultations.api.bot.service.TeacherConsultationService;
import com.example.botforconsultations.api.bot.state.TeacherStateManager;
import com.example.botforconsultations.api.bot.state.TeacherStateManager.TeacherState;
import com.example.botforconsultations.api.bot.utils.TeacherKeyboardBuilder;
import com.example.botforconsultations.api.bot.utils.TeacherMessageFormatter;
import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Обработчик команд преподавателя
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherCommandHandler {

    // Репозитории
    private final TelegramUserRepository telegramUserRepository;
    private final ConsultationRepository consultationRepository;

    // Сервисы
    private final TeacherConsultationService consultationService;
    private final ConsultationRequestService requestService;
    private final NotificationService notificationService;
    private final BotMessenger botMessenger;

    // Утилиты
    private final TeacherStateManager stateManager;
    private final TeacherKeyboardBuilder keyboardBuilder;
    private final TeacherMessageFormatter messageFormatter;

    // Форматтеры для парсинга
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yy"),
            DateTimeFormatter.ofPattern("dd.MM")
    };
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Главный обработчик команд преподавателя
     */
    public void handleTeacherCommand(String text, Long chatId) {
        TeacherState currentState = stateManager.getState(chatId);

        // Проверка кнопки "Отмена" - обрабатывается в первую очередь
        if (text.equals("❌ Отмена")) {
            handleCancel(chatId);
            return;
        }

        // Обработка состояний ввода
        if (currentState != TeacherState.DEFAULT
                && currentState != TeacherState.VIEWING_CONSULTATION_DETAILS
                && currentState != TeacherState.VIEWING_REQUEST_DETAILS) {
            switch (currentState) {
                case WAITING_FOR_CONSULTATION_TITLE -> processConsultationTitle(text, chatId);
                case WAITING_FOR_CONSULTATION_DATETIME -> processConsultationDateTime(text, chatId);
                case WAITING_FOR_CONSULTATION_CAPACITY -> processConsultationCapacity(text, chatId);
                case WAITING_FOR_CONSULTATION_AUTOCLOSE -> processConsultationAutoClose(text, chatId);
                case ACCEPTING_REQUEST_DATETIME -> processAcceptRequestDateTime(text, chatId);
                case ACCEPTING_REQUEST_CAPACITY -> processAcceptRequestCapacity(text, chatId);
                case ACCEPTING_REQUEST_AUTOCLOSE -> processAcceptRequestAutoClose(text, chatId);
                case EDITING_TITLE -> processEditTitle(chatId, text);
                case EDITING_DATETIME -> processEditDateTime(chatId, text);
                case EDITING_CAPACITY -> processEditCapacity(chatId, text);
                case EDITING_AUTOCLOSE -> processEditAutoClose(chatId, text);
                default -> {
                } // Никогда не должно произойти из-за условия if
            }
            return;
        }

        // Обработка выбора консультации/запроса по номеру в режиме просмотра
        if ((currentState == TeacherState.VIEWING_REQUEST_DETAILS ||
                currentState == TeacherState.VIEWING_CONSULTATION_DETAILS) &&
                text.startsWith("№")
        ) {
            handleNumberSelection(text, chatId);
            return;
        }

        // Основные команды
        switch (text) {
            case "Помощь" -> sendHelp(chatId);
            case "📅 Мои консультации" -> showMyConsultations(chatId);
            case "➕ Создать консультацию" -> startConsultationCreation(chatId);
            case "📋 Просмотреть запросы" -> showStudentRequests(chatId);

            // Управление консультацией
            case "🔒 Закрыть запись" -> handleCloseConsultation(chatId);
            case "🔓 Открыть запись" -> handleOpenConsultation(chatId);
            case "✏️ Редактировать" -> showEditMenu(chatId);
            case "❌ Отменить консультацию" -> handleCancelConsultation(chatId);
            case "👥 Просмотреть студентов" -> showRegisteredStudents(chatId);

            // Редактирование параметров
            case "📋 Название" -> startEditTitle(chatId);
            case "📅 Дата и время" -> startEditDateTime(chatId);
            case "👥 Вместимость" -> startEditCapacity(chatId);
            case "🔒 Автозакрытие" -> startEditAutoClose(chatId);

            // Работа с запросами
            case "✅ Принять запрос" -> startAcceptRequest(chatId);

            // Навигация
            case "◀️ Назад к списку" -> backToList(chatId);
            case "◀️ Назад" -> handleBackButton(chatId);

            default -> botMessenger.sendText(
                    "Извините, я не понимаю эту команду. Отправьте 'Помощь' для получения списка доступных команд.",
                    chatId
            );
        }
    }

    // ========== Главное меню и справка ==========

    public void sendMainMenu(Long chatId) {
        stateManager.resetState(chatId);
        stateManager.clearCurrentConsultation(chatId);
        stateManager.clearCurrentRequest(chatId);
        botMessenger.execute(SendMessage.builder()
                .text("Добро пожаловать, преподаватель! Выберите действие:")
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildMainMenu())
                .build());
    }

    public void sendHelp(Long chatId) {
        String helpText = """
                Доступные команды для преподавателя:
                
                📅 Мои консультации - управление вашими консультациями
                ➕ Создать консультацию - публикация нового времени для консультаций
                📋 Просмотреть запросы - просмотр запросов студентов на консультации
                
                В разделе "📅 Мои консультации" вы можете:
                - Просматривать список записавшихся студентов
                - Закрывать запись (можно установить лимит)
                - Отменять консультации
                
                В разделе "📋 Просмотреть запросы" можно создавать консультации на основе запросов
                """;
        botMessenger.sendText(helpText, chatId);
    }

    // ========== Создание консультации ==========

    private void startConsultationCreation(Long chatId) {
        stateManager.setState(chatId, TeacherState.WAITING_FOR_CONSULTATION_TITLE);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("➕ Создание новой консультации\n\n" +
                        "Шаг 1/4: Введите название консультации\n" +
                        "Например: \"Разбор курсовых работ\" или \"Подготовка к экзамену\"")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    private void processConsultationTitle(String title, Long chatId) {
        if (title == null || title.trim().isEmpty()) {
            botMessenger.sendText("Название не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        if (title.length() > 200) {
            botMessenger.sendText(
                    "Название слишком длинное (максимум 200 символов). Попробуйте сократить:",
                    chatId
            );
            return;
        }

        stateManager.setTempTitle(chatId, title.trim());
        stateManager.setState(chatId, TeacherState.WAITING_FOR_CONSULTATION_DATETIME);

        botMessenger.sendText(
                "✅ Название сохранено: \"" + title.trim() + "\"\n\n" +
                        "Шаг 2/4: Введите дату и время одной строкой\n\n" +
                        "Формат: ДД.ММ.ГГГГ ЧЧ:ММ-ЧЧ:ММ\n" +
                        "Примеры:\n" +
                        "• 15.10.2025 14:00-16:00\n" +
                        "• 20.10 10:00-12:00",
                chatId
        );
    }

    private void processConsultationDateTime(String input, Long chatId) {
        ParsedDateTime parsed = parseDateTimeInput(input);

        // Валидация с автоматической отправкой сообщений об ошибках
        if (!validateParsedDateTime(parsed, chatId)) {
            return;
        }

        // Сохраняем данные
        stateManager.setTempDate(chatId, parsed.date.toString());
        stateManager.setTempStartTime(chatId, parsed.startTime.toString());
        stateManager.setTempEndTime(chatId, parsed.endTime.toString());
        stateManager.setState(chatId, TeacherState.WAITING_FOR_CONSULTATION_CAPACITY);

        botMessenger.sendText(
                String.format("✅ Дата и время сохранены: %s %s-%s\n\n",
                        parsed.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        parsed.startTime.format(TIME_FORMATTER),
                        parsed.endTime.format(TIME_FORMATTER)) +
                        "Шаг 3/4: Введите вместимость (максимальное количество студентов)\n\n" +
                        "• Введите число (например: 5)\n" +
                        "• Или введите 0 для без ограничений",
                chatId
        );
    }

    private void processConsultationCapacity(String input, Long chatId) {
        Integer capacity = parseCapacity(input);

        stateManager.setTempCapacity(chatId, capacity);
        stateManager.setState(chatId, TeacherState.WAITING_FOR_CONSULTATION_AUTOCLOSE);

        String capacityText = capacity == null ? "без ограничений" : String.valueOf(capacity);

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(String.format("✅ Вместимость: %s\n\n", capacityText) +
                        "Шаг 4/4: Автоматически закрывать запись при достижении лимита?\n\n" +
                        "Если включено, запись автоматически закроется когда количество записавшихся достигнет вместимости.")
                .replyMarkup(keyboardBuilder.buildYesNoKeyboard())
                .build());
    }

    private void processConsultationAutoClose(String answer, Long chatId) {
        boolean autoClose = answer.equalsIgnoreCase("Да");

        // Получаем сохранённые данные
        String title = stateManager.getTempTitle(chatId);
        LocalDate date = LocalDate.parse(stateManager.getTempDate(chatId));
        LocalTime startTime = LocalTime.parse(stateManager.getTempStartTime(chatId));
        LocalTime endTime = LocalTime.parse(stateManager.getTempEndTime(chatId));
        Integer capacity = stateManager.getTempCapacity(chatId);

        // Создаём консультацию
        TelegramUser teacher = getCurrentTeacher(chatId);
        Consultation consultation = consultationService.createConsultation(
                teacher, title, date, startTime, endTime, capacity, autoClose
        );

        // Очищаем временные данные
        stateManager.clearTempConsultationData(chatId);
        stateManager.resetState(chatId);

        // Отправляем уведомления подписчикам
        notificationService.notifySubscribersNewConsultation(consultation.getId());

        // Подтверждение
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(String.format(
                        "✅ Консультация успешно создана!\n\n" +
                                "📋 Консультация №%d\n" +
                                "📝 %s\n" +
                                "📅 %s\n" +
                                "🕐 %s - %s\n" +
                                "👥 Вместимость: %s\n" +
                                "🔒 Автозакрытие: %s\n\n" +
                                "Уведомления отправлены всем подписанным студентам.",
                        consultation.getId(),
                        title,
                        date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                        startTime.format(TIME_FORMATTER),
                        endTime.format(TIME_FORMATTER),
                        capacity == null ? "без ограничений" : capacity,
                        autoClose ? "включено" : "выключено"
                ))
                .replyMarkup(keyboardBuilder.buildMainMenu())
                .build());
    }

    // ========== Просмотр консультаций ==========

    private void showMyConsultations(Long chatId) {
        TelegramUser teacher = getCurrentTeacher(chatId);
        List<Consultation> consultations = consultationService.getTeacherConsultations(teacher);

        String message = messageFormatter.formatConsultationsList(consultations);

        if (consultations.isEmpty()) {
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildMainMenu())
                    .build());
        } else {
            stateManager.clearCurrentConsultation(chatId);  // Очищаем при показе списка
            stateManager.setState(chatId, TeacherState.VIEWING_CONSULTATION_DETAILS);
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildConsultationsList(consultations))
                    .build());
        }
    }

    private void handleNumberSelection(String text, Long chatId) {
        try {
            Long id = extractId(text);
            TeacherState currentState = stateManager.getState(chatId);

            // Определяем: запрос или консультация по состоянию
            if (currentState == TeacherState.VIEWING_REQUEST_DETAILS) {
                // Проверяем, что это действительно запрос
                requestService.findRequestById(id).ifPresentOrElse(
                        request -> showRequestDetails(chatId, id),
                        () -> botMessenger.sendText("Запрос №" + id + " не найден", chatId)
                );
            } else if (currentState == TeacherState.VIEWING_CONSULTATION_DETAILS) {
                // Проверяем, что это консультация (не запрос)
                consultationRepository.findById(id).ifPresentOrElse(
                        consultation -> {
                            // Проверяем, что это либо запрос, либо консультация
                            if (consultation.getStatus().equals(ConsultationStatus.REQUEST)) {
                                // Это запрос, а не консультация
                                botMessenger.sendText("№" + id + " является запросом студента, а не консультацией.\n" +
                                        "Перейдите в раздел '📋 Просмотреть запросы' для просмотра.", chatId);
                            } else {
                                // Это консультация - показываем
                                showConsultationDetails(chatId, id);
                            }
                        },
                        () -> botMessenger.sendText("Консультация №" + id + " не найдена", chatId)
                );
            } else {
                botMessenger.sendText("Ошибка: неверное состояние для просмотра", chatId);
            }
        } catch (Exception e) {
            log.error("Error parsing ID from '{}': {}", text, e.getMessage());
            botMessenger.sendText("Неверный формат номера. Используйте формат: №123", chatId);
        }
    }

    private Long extractId(String text) {
        String idStr = text.contains(" ")
                ? text.substring(1, text.indexOf(" "))
                : text.substring(1);
        return Long.parseLong(idStr);
    }

    private void showConsultationDetails(Long chatId, Long consultationId) {
        TelegramUser currentTeacher = getCurrentTeacher(chatId);

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    stateManager.setCurrentConsultation(chatId, consultationId);
                    stateManager.setState(chatId, TeacherState.VIEWING_CONSULTATION_DETAILS);

                    long registeredCount = consultation.getRegUsers() != null
                            ? consultation.getRegUsers().size()
                            : 0;
                    String message = messageFormatter.formatConsultationDetails(consultation, registeredCount);

                    // Проверяем владельца консультации
                    boolean isOwner = consultation.getTeacher() != null &&
                            consultation.getTeacher().getId().equals(currentTeacher.getId());

                    if (isOwner) {
                        // Своя консультация - полный функционал
                        botMessenger.execute(SendMessage.builder()
                                .chatId(chatId)
                                .text(message)
                                .replyMarkup(keyboardBuilder.buildConsultationDetails(consultation, registeredCount))
                                .build());
                    } else {
                        // Чужая консультация - только просмотр
                        botMessenger.execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("⚠️ Это консультация другого преподавателя (только просмотр)\n\n" + message)
                                .replyMarkup(keyboardBuilder.buildConsultationDetailsReadOnly(registeredCount))
                                .build());
                    }
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void showRegisteredStudents(Long chatId) {
        TeacherState currentState = stateManager.getState(chatId);

        if (currentState == TeacherState.VIEWING_CONSULTATION_DETAILS) {
            Long consultationId = stateManager.getCurrentConsultation(chatId);
            if (consultationId == null) {
                botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
                return;
            }

            consultationRepository.findById(consultationId).ifPresentOrElse(
                    consultation -> {
                        List<StudentConsultation> registrations = consultation.getRegUsers() != null
                                ? List.copyOf(consultation.getRegUsers())
                                : List.of();
                        String message = messageFormatter.formatRegisteredStudents(registrations);

                        botMessenger.execute(SendMessage.builder()
                                .chatId(chatId)
                                .text(message)
                                .replyMarkup(keyboardBuilder.buildBackKeyboard())
                                .build());
                    },
                    () -> botMessenger.sendText("Консультация не найдена", chatId)
            );
        } else if (currentState == TeacherState.VIEWING_REQUEST_DETAILS) {
            Long requestId = stateManager.getCurrentRequest(chatId);
            if (requestId == null) {
                botMessenger.sendText("Ошибка: запрос не выбран", chatId);
                return;
            }

            showRequestStudents(chatId, requestId);
        } else {
            botMessenger.sendText("Ошибка: консультация или запрос не выбран", chatId);
        }
    }

    // ========== Управление консультацией ==========

    private void handleCloseConsultation(Long chatId) {
        Long consultationId = stateManager.getCurrentConsultation(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете управлять консультацией другого преподавателя", chatId);
                        return;
                    }

                    consultationService.closeConsultation(consultation);
                    botMessenger.sendText("🔒 Запись на консультацию закрыта", chatId);
                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void handleOpenConsultation(Long chatId) {
        Long consultationId = stateManager.getCurrentConsultation(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете управлять консультацией другого преподавателя", chatId);
                        return;
                    }

                    TeacherConsultationService.OpenResult result = consultationService.openConsultation(consultation);

                    if (!result.isSuccess()) {
                        // Не удалось открыть
                        botMessenger.sendText(result.message(), chatId);
                    } else {
                        botMessenger.sendText("🔓 Запись на консультацию открыта", chatId);

                        // Уведомляем подписчиков о появлении мест
                        notificationService.notifySubscribersAvailableSpots(consultation.getId(), null);
                    }

                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void handleCancelConsultation(Long chatId) {
        Long consultationId = stateManager.getCurrentConsultation(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете управлять консультацией другого преподавателя", chatId);
                        return;
                    }

                    consultationService.cancelConsultation(consultation, "Отменено преподавателем");

                    // Уведомляем всех записанных студентов
                    notificationService.notifyRegisteredStudentsCancellation(consultation.getId());

                    botMessenger.sendText(
                            "❌ Консультация отменена.\n" +
                                    "Все записанные студенты получили уведомление.",
                            chatId
                    );
                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    // ========== Редактирование консультации ==========

    private void showEditMenu(Long chatId) {
        Long consultationId = stateManager.getCurrentConsultationId(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете редактировать консультацию другого преподавателя", chatId);
                        return;
                    }

                    String dateTime = consultation.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                            " " + consultation.getStartTime().format(TIME_FORMATTER) +
                            "-" + consultation.getEndTime().format(TIME_FORMATTER);

                    String capacityText = (consultation.getCapacity() != null && consultation.getCapacity() > 0)
                            ? String.valueOf(consultation.getCapacity())
                            : "без ограничений";

                    String message = "Что хотите изменить?\n\n" +
                            "📋 Название: " + consultation.getTitle() + "\n" +
                            "📅 Дата и время: " + dateTime + "\n" +
                            "👥 Вместимость: " + capacityText + "\n" +
                            "🔒 Автозакрытие: " + (consultation.isAutoCloseOnCapacity() ? "Включено" : "Выключено");

                    SendMessage sendMessage = SendMessage.builder()
                            .chatId(chatId)
                            .text(message)
                            .replyMarkup(keyboardBuilder.buildEditMenu())
                            .build();

                    botMessenger.execute(sendMessage);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void startEditTitle(Long chatId) {
        stateManager.setState(chatId, TeacherState.EDITING_TITLE);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Введите новое название консультации:")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build();

        botMessenger.execute(sendMessage);
    }

    private void processEditTitle(Long chatId, String title) {
        Long consultationId = stateManager.getCurrentConsultationId(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете редактировать консультацию другого преподавателя", chatId);
                        stateManager.setState(chatId, TeacherState.DEFAULT);
                        return;
                    }

                    consultation.setTitle(title);
                    consultationRepository.save(consultation);

                    // Уведомляем записанных студентов
                    notificationService.notifyRegisteredStudentsUpdate(consultation.getId(), "Изменено название консультации");

                    stateManager.setState(chatId, TeacherState.DEFAULT);
                    botMessenger.sendText("✅ Название изменено", chatId);
                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void startEditDateTime(Long chatId) {
        stateManager.setState(chatId, TeacherState.EDITING_DATETIME);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Введите новую дату и время в формате:\n" +
                        "ДД.ММ.ГГГГ ЧЧ:ММ-ЧЧ:ММ\n\n" +
                        "Примеры:\n" +
                        "25.12.2024 15:30-17:00\n" +
                        "25.12.24 15:30-17:00\n" +
                        "25.12 15:30-17:00")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build();

        botMessenger.execute(sendMessage);
    }

    private void processEditDateTime(Long chatId, String dateTimeInput) {
        Long consultationId = stateManager.getCurrentConsultationId(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        ParsedDateTime parsed = parseDateTimeInput(dateTimeInput);

        // Валидация с автоматической отправкой сообщений об ошибках
        if (!validateParsedDateTime(parsed, chatId)) {
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете редактировать консультацию другого преподавателя", chatId);
                        stateManager.setState(chatId, TeacherState.DEFAULT);
                        return;
                    }

                    consultation.setDate(parsed.date());
                    consultation.setStartTime(parsed.startTime());
                    consultation.setEndTime(parsed.endTime());
                    consultationRepository.save(consultation);

                    // Уведомляем записанных студентов
                    notificationService.notifyRegisteredStudentsUpdate(consultation.getId(), "Изменены дата и время консультации");

                    stateManager.setState(chatId, TeacherState.DEFAULT);
                    botMessenger.sendText("✅ Дата и время изменены", chatId);
                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void startEditCapacity(Long chatId) {
        stateManager.setState(chatId, TeacherState.EDITING_CAPACITY);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Введите новую вместимость:\n\n" +
                        "• Введите число (например: 5)\n" +
                        "• Или введите 0 для без ограничений")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build();

        botMessenger.execute(sendMessage);
    }

    private void processEditCapacity(Long chatId, String capacityInput) {
        Long consultationId = stateManager.getCurrentConsultationId(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        Integer capacity = parseCapacity(capacityInput);

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете редактировать консультацию другого преподавателя", chatId);
                        stateManager.setState(chatId, TeacherState.DEFAULT);
                        return;
                    }

                    int registeredCount = consultation.getRegUsers() != null
                            ? consultation.getRegUsers().size()
                            : 0;

                    // Проверяем только если задана конкретная вместимость
                    if (capacity != null && capacity < registeredCount) {
                        botMessenger.sendText(
                                "❌ Новая вместимость (" + capacity + ") не может быть меньше " +
                                        "количества уже записанных студентов (" + registeredCount + ")",
                                chatId
                        );
                        return;
                    }

                    Integer oldCapacity = consultation.getCapacity();
                    consultation.setCapacity(capacity);
                    consultationRepository.save(consultation);

                    // Уведомляем записанных студентов
                    // notificationService.notifyRegisteredStudentsUpdate(consultation, "Изменена вместимость консультации");

                    // Если появились свободные места, уведомляем подписчиков
                    if (capacity != null && (oldCapacity == null || capacity > oldCapacity) && registeredCount < capacity) {
                        notificationService.notifySubscribersAvailableSpots(consultation.getId(), null);
                    }

                    stateManager.setState(chatId, TeacherState.DEFAULT);

                    String capacityText = capacity == null ? "без ограничений" : String.valueOf(capacity);
                    botMessenger.sendText("✅ Вместимость изменена на: " + capacityText, chatId);
                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    private void startEditAutoClose(Long chatId) {
        stateManager.setState(chatId, TeacherState.EDITING_AUTOCLOSE);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Включить автозакрытие при достижении вместимости?")
                .replyMarkup(keyboardBuilder.buildYesNoKeyboard())
                .build();

        botMessenger.execute(sendMessage);
    }

    private void processEditAutoClose(Long chatId, String answer) {
        Long consultationId = stateManager.getCurrentConsultationId(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return;
        }

        consultationRepository.findById(consultationId).ifPresentOrElse(
                consultation -> {
                    // Проверка владельца
                    if (!isConsultationOwner(consultation, chatId)) {
                        botMessenger.sendText("❌ Вы не можете редактировать консультацию другого преподавателя", chatId);
                        stateManager.setState(chatId, TeacherState.DEFAULT);
                        return;
                    }

                    boolean autoClose = answer.equals("Да");
                    consultation.setAutoCloseOnCapacity(autoClose);
                    consultationRepository.save(consultation);

                    // Уведомляем записанных студентов
//                    notificationService.notifyRegisteredStudentsUpdate(
//                            consultation,
//                            "Изменено автозакрытие: " + (autoClose ? "включено" : "выключено")
//                    );

                    stateManager.setState(chatId, TeacherState.DEFAULT);
                    botMessenger.sendText(
                            "✅ Автозакрытие " + (autoClose ? "включено" : "выключено"),
                            chatId
                    );
                    showConsultationDetails(chatId, consultationId);
                },
                () -> botMessenger.sendText("Консультация не найдена", chatId)
        );
    }

    // ========== Работа с запросами студентов ==========

    private void showStudentRequests(Long chatId) {
        List<Consultation> requests = requestService.getAllRequests();

        String message = messageFormatter.formatRequestsList(requests);

        if (requests.isEmpty()) {
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildMainMenu())
                    .build());
        } else {
            stateManager.clearCurrentRequest(chatId);  // Очищаем при показе списка
            stateManager.setState(chatId, TeacherState.VIEWING_REQUEST_DETAILS);
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildRequestsList(requests))
                    .build());
        }
    }

    private void showRequestDetails(Long chatId, Long requestId) {
        requestService.findRequestById(requestId).ifPresentOrElse(
                request -> {
                    stateManager.setCurrentRequest(chatId, requestId);

                    int interestedCount = request.getRegUsers() != null
                            ? request.getRegUsers().size()
                            : 0;
                    String message = messageFormatter.formatRequestDetails(request, interestedCount);

                    botMessenger.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(message)
                            .replyMarkup(keyboardBuilder.buildRequestDetails(interestedCount))
                            .build());
                },
                () -> botMessenger.sendText("Запрос не найден", chatId)
        );
    }

    private void showRequestStudents(Long chatId, Long requestId) {
        requestService.findRequestById(requestId).ifPresentOrElse(
                request -> {
                    List<StudentConsultation> registrations = request.getRegUsers() != null
                            ? List.copyOf(request.getRegUsers())
                            : List.of();
                    String message = messageFormatter.formatRegisteredStudents(registrations);

                    botMessenger.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(message)
                            .replyMarkup(keyboardBuilder.buildBackKeyboard())
                            .build());
                },
                () -> botMessenger.sendText("Запрос не найден", chatId)
        );
    }

    private void startAcceptRequest(Long chatId) {
        Long requestId = stateManager.getCurrentRequest(chatId);
        if (requestId == null) {
            botMessenger.sendText("Ошибка: запрос не выбран", chatId);
            return;
        }

        stateManager.setState(chatId, TeacherState.ACCEPTING_REQUEST_DATETIME);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("✅ Принятие запроса студента\n\n" +
                        "Название уже указано студентом.\n\n" +
                        "Шаг 1/3: Введите дату и время одной строкой\n\n" +
                        "Формат: ДД.ММ.ГГГГ ЧЧ:ММ-ЧЧ:ММ\n" +
                        "Примеры:\n" +
                        "• 15.10.2025 14:00-16:00\n" +
                        "• 20.10 10:00-12:00")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    private void processAcceptRequestDateTime(String input, Long chatId) {
        ParsedDateTime parsed = parseDateTimeInput(input);

        // Валидация с автоматической отправкой сообщений об ошибках
        if (!validateParsedDateTime(parsed, chatId)) {
            return;
        }

        stateManager.setTempDate(chatId, parsed.date.toString());
        stateManager.setTempStartTime(chatId, parsed.startTime.toString());
        stateManager.setTempEndTime(chatId, parsed.endTime.toString());
        stateManager.setState(chatId, TeacherState.ACCEPTING_REQUEST_CAPACITY);

        botMessenger.sendText(
                "✅ Дата и время сохранены\n\n" +
                        "Шаг 2/3: Введите вместимость\n" +
                        "• Введите число (например: 5)\n" +
                        "• Или 0 для без ограничений",
                chatId
        );
    }

    private void processAcceptRequestCapacity(String input, Long chatId) {
        Integer capacity = parseCapacity(input);

        stateManager.setTempCapacity(chatId, capacity);
        stateManager.setState(chatId, TeacherState.ACCEPTING_REQUEST_AUTOCLOSE);

        String capacityText = capacity == null ? "без ограничений" : String.valueOf(capacity);

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("✅ Вместимость: " + capacityText + "\n\n" +
                        "Шаг 3/3: Автоматически закрывать запись при достижении лимита?")
                .replyMarkup(keyboardBuilder.buildYesNoKeyboard())
                .build());
    }

    private void processAcceptRequestAutoClose(String answer, Long chatId) {
        boolean autoClose = answer.equalsIgnoreCase("Да");

        Long requestId = stateManager.getCurrentRequest(chatId);
        requestService.findRequestById(requestId).ifPresentOrElse(
                request -> {
                    // Получаем данные
                    LocalDate date = LocalDate.parse(stateManager.getTempDate(chatId));
                    LocalTime startTime = LocalTime.parse(stateManager.getTempStartTime(chatId));
                    LocalTime endTime = LocalTime.parse(stateManager.getTempEndTime(chatId));
                    Integer capacity = stateManager.getTempCapacity(chatId);

                    TelegramUser teacher = getCurrentTeacher(chatId);

                    // Принимаем запрос (превращаем в консультацию)
                    Consultation consultation = consultationService.acceptRequest(
                            request, teacher, date, startTime, endTime, capacity, autoClose
                    );

                    // Очищаем данные
                    stateManager.clearTempConsultationData(chatId);
                    stateManager.resetState(chatId);

                    // Уведомляем заинтересованных студентов
                    notificationService.notifyInterestedStudentsRequestAccepted(consultation.getId());

                    botMessenger.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(String.format(
                                    "✅ Запрос принят и превращён в консультацию!\n\n" +
                                            "📋 Консультация №%d\n" +
                                            "📝 %s\n" +
                                            "📅 %s %s-%s\n\n" +
                                            "Все заинтересованные студенты автоматически записаны и получили уведомление.",
                                    consultation.getId(),
                                    consultation.getTitle(),
                                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                    startTime.format(TIME_FORMATTER),
                                    endTime.format(TIME_FORMATTER)
                            ))
                            .replyMarkup(keyboardBuilder.buildMainMenu())
                            .build());
                },
                () -> {
                    botMessenger.sendText("Запрос не найден", chatId);
                    stateManager.resetState(chatId);
                }
        );
    }

    // ========== Навигация ==========

    private void backToList(Long chatId) {
        TeacherState currentState = stateManager.getState(chatId);

        if (currentState == TeacherState.VIEWING_REQUEST_DETAILS) {
            showStudentRequests(chatId);
        } else {
            showMyConsultations(chatId);
        }
    }

    /**
     * Обработка кнопки "◀️ Назад" - возврат в зависимости от контекста
     */
    private void handleBackButton(Long chatId) {
        TeacherState currentState = stateManager.getState(chatId);

        // Если мы в просмотре деталей консультации в меню редактирования
        if (currentState == TeacherState.VIEWING_CONSULTATION_DETAILS) {
            Long consultationId = stateManager.getCurrentConsultationId(chatId);
            if (consultationId != null) {
                // Возврат к деталям консультации
                showConsultationDetails(chatId, consultationId);
                return;
            }
        }

        if (currentState == TeacherState.VIEWING_REQUEST_DETAILS) {
            Long requestId = stateManager.getCurrentRequest(chatId);
            if (requestId != null) {
                // Возврат к деталям запроса
                showRequestDetails(chatId, requestId);
                return;
            }
        }

        // В остальных случаях - возврат в главное меню
        sendMainMenu(chatId);
    }

    /**
     * Обработка кнопки "Отмена" - прерывает процесс создания/редактирования
     */
    private void handleCancel(Long chatId) {
        TeacherState currentState = stateManager.getState(chatId);

        // Определяем, что именно отменяем и куда возвращаемся
        if (currentState == TeacherState.WAITING_FOR_CONSULTATION_TITLE
                || currentState == TeacherState.WAITING_FOR_CONSULTATION_DATETIME
                || currentState == TeacherState.WAITING_FOR_CONSULTATION_CAPACITY
                || currentState == TeacherState.WAITING_FOR_CONSULTATION_AUTOCLOSE) {
            // 1) Создание консультации - возврат в главное меню
            stateManager.clearTempConsultationData(chatId);
            stateManager.resetState(chatId);

            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Создание консультации отменено")
                    .replyMarkup(keyboardBuilder.buildMainMenu())
                    .build());

        } else if (currentState == TeacherState.EDITING_TITLE
                || currentState == TeacherState.EDITING_DATETIME
                || currentState == TeacherState.EDITING_CAPACITY
                || currentState == TeacherState.EDITING_AUTOCLOSE) {
            // 2) Редактирование консультации - возврат к просмотру консультации
            Long consultationId = stateManager.getCurrentConsultationId(chatId);
            stateManager.clearTempConsultationData(chatId);
            stateManager.resetState(chatId);

            botMessenger.sendText("❌ Редактирование отменено", chatId);

            if (consultationId != null) {
                showConsultationDetails(chatId, consultationId);
            } else {
                sendMainMenu(chatId);
            }

        } else if (currentState == TeacherState.ACCEPTING_REQUEST_DATETIME
                || currentState == TeacherState.ACCEPTING_REQUEST_CAPACITY
                || currentState == TeacherState.ACCEPTING_REQUEST_AUTOCLOSE) {
            // 3) Принятие запроса - возврат к просмотру запроса
            Long requestId = stateManager.getCurrentRequest(chatId);
            stateManager.clearTempConsultationData(chatId);
            stateManager.resetState(chatId);

            botMessenger.sendText("❌ Принятие запроса отменено", chatId);

            if (requestId != null) {
                showRequestDetails(chatId, requestId);
            } else {
                sendMainMenu(chatId);
            }

        } else {
            // Неожиданное состояние - возврат в главное меню
            stateManager.clearTempConsultationData(chatId);
            stateManager.resetState(chatId);

            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Операция отменена")
                    .replyMarkup(keyboardBuilder.buildMainMenu())
                    .build());
        }
    }

    // ========== Вспомогательные методы ==========

    private TelegramUser getCurrentTeacher(Long chatId) {
        return telegramUserRepository.findByTelegramId(chatId).orElseThrow();
    }

    /**
     * Проверка, является ли текущий преподаватель владельцем консультации
     *
     * @return true если консультация принадлежит преподавателю
     */
    private boolean isConsultationOwner(Consultation consultation, Long chatId) {
        TelegramUser currentTeacher = getCurrentTeacher(chatId);
        return consultation.getTeacher() != null &&
                consultation.getTeacher().getId().equals(currentTeacher.getId());
    }

    /**
     * Парсинг даты и времени из строки
     * Поддерживаемые форматы:
     * - 15.10.2025 14:00-16:00
     * - 15.10 14:00-16:00
     */
    private ParsedDateTime parseDateTimeInput(String input) {
        try {
            String[] parts = input.trim().split("\\s+");
            if (parts.length != 2) {
                return ParsedDateTime.invalid();
            }

            // Парсим дату
            LocalDate date = parseDate(parts[0]);
            if (date == null) {
                return ParsedDateTime.invalid();
            }

            // Парсим время
            String[] timeParts = parts[1].split("-");
            if (timeParts.length != 2) {
                return ParsedDateTime.invalid();
            }

            LocalTime startTime = LocalTime.parse(timeParts[0].trim(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(timeParts[1].trim(), TIME_FORMATTER);

            return new ParsedDateTime(date, startTime, endTime, true);
        } catch (DateTimeParseException e) {
            return ParsedDateTime.invalid();
        }
    }

    private LocalDate parseDate(String dateStr) {
        // Пробуем все форматы с парсером
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        // Если ни один формат не подошёл, пробуем вручную парсить dd.MM (без года)
        String[] parts = dateStr.split("\\.");
        if (parts.length == 2) {
            try {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = LocalDate.now().getYear();

                // Создаём дату с текущим годом
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Парсинг вместимости (0 или отрицательное = null)
     */
    private Integer parseCapacity(String input) {
        try {
            int value = Integer.parseInt(input.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Валидация ParsedDateTime с отправкой сообщений об ошибках
     *
     * @return true если валидация прошла успешно
     */
    private boolean validateParsedDateTime(ParsedDateTime parsed, Long chatId) {
        if (!parsed.isValid()) {
            botMessenger.sendText(
                    "❌ Неверный формат!\n\n" +
                            "Используйте формат: ДД.ММ.ГГГГ ЧЧ:ММ-ЧЧ:ММ\n" +
                            "Примеры:\n" +
                            "• 15.10.2025 14:00-16:00\n" +
                            "• 20.10 10:00-12:00\n\n" +
                            "Попробуйте ещё раз:",
                    chatId
            );
            return false;
        }

        // Валидация: время окончания должно быть после начала
        if (!parsed.endTime.isAfter(parsed.startTime)) {
            botMessenger.sendText(
                    "❌ Время окончания должно быть позже времени начала!\n" +
                            "Попробуйте ещё раз:",
                    chatId
            );
            return false;
        }

        // Валидация: дата и время начала не должны быть в прошлом
        if (parsed.date.atTime(parsed.startTime).isBefore(java.time.LocalDateTime.now())) {
            botMessenger.sendText(
                    "❌ Дата и время консультации не могут быть в прошлом!\n" +
                            "Попробуйте ещё раз:",
                    chatId
            );
            return false;
        }

        return true;
    }

    // ========== Record для парсинга ==========

    private record ParsedDateTime(LocalDate date, LocalTime startTime, LocalTime endTime, boolean valid) {
        public boolean isValid() {
            return valid;
        }

        public static ParsedDateTime invalid() {
            return new ParsedDateTime(null, null, null, false);
        }
    }
}
