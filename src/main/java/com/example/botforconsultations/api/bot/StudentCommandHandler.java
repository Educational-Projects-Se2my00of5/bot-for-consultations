package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.ConsultationRequestService;
import com.example.botforconsultations.api.bot.service.ConsultationService;
import com.example.botforconsultations.api.bot.service.NotificationService;
import com.example.botforconsultations.api.bot.service.StudentServiceBot;
import com.example.botforconsultations.api.bot.service.TeacherConsultationService;
import com.example.botforconsultations.api.bot.service.TeacherSearchService;
import com.example.botforconsultations.api.bot.state.StudentStateManager;
import com.example.botforconsultations.api.bot.state.StudentStateManager.UserState;
import com.example.botforconsultations.api.bot.utils.ConsultationMessageFormatter;
import com.example.botforconsultations.api.bot.utils.StudentKeyboardBuilder;
import com.example.botforconsultations.api.bot.utils.TeacherNameFormatter;
import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.Subscription;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

/**
 * Рефакторенный обработчик команд студента
 * Использует паттерн Service Layer для разделения бизнес-логики
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCommandHandler {

    // Репозитории
    private final TelegramUserRepository telegramUserRepository;

    // Сервисы
    private final TeacherSearchService teacherSearchService;
    private final ConsultationService consultationService;
    private final ConsultationRequestService consultationRequestService;
    private final StudentServiceBot studentServiceBot;
    private final TeacherConsultationService teacherConsultationService;
    private final NotificationService notificationService;
    private final ProfileCommandHandler profileCommandHandler;
    private final BotMessenger botMessenger;

    // Утилиты
    private final StudentStateManager stateManager;
    private final StudentKeyboardBuilder keyboardBuilder;
    private final ConsultationMessageFormatter messageFormatter;

    /**
     * Главный обработчик команд студента
     */
    public void handleStudentCommand(String text, Long chatId) {

        if (text.equals("◀️ Назад")) {
            sendMainMenu(chatId);
            return;
        }

        // Обработка выбора преподавателя (кнопка начинается с эмодзи)
        if (text.startsWith("👨‍🏫")) {
            handleTeacherSelection(text, chatId);
            return;
        }

        UserState currentState = stateManager.getState(chatId);

        // Обработка кнопки отмены для всех состояний ввода
        if (handleCancelButton(text, chatId, currentState)) {
            return;
        }

        

        if (currentState == UserState.WAITING_FOR_CONSULTATION_MESSAGE) {
            processConsultationRegistration(text, chatId);
            return;
        }

        if (currentState == UserState.WAITING_FOR_REQUEST_TITLE) {
            processRequestCreation(text, chatId);
            return;
        }

        if (currentState == UserState.WAITING_FOR_REQUEST_MESSAGE) {
            processRequestRegistration(text, chatId);
            return;
        }

        // Обработка редактирования профиля
        if (currentState == UserState.EDITING_PROFILE_FIRST_NAME) {
            profileCommandHandler.processFirstNameUpdate(text, chatId, getCurrentStudent(chatId));
            stateManager.resetState(chatId);
            return;
        }

        if (currentState == UserState.EDITING_PROFILE_LAST_NAME) {
            profileCommandHandler.processLastNameUpdate(text, chatId, getCurrentStudent(chatId));
            stateManager.resetState(chatId);
            return;
        }

        // Обработка выбора консультации/запроса по номеру
        if (text.startsWith("№")) {
            handleNumberSelection(text, chatId);
            return;
        }

        // Основные команды
        // case "👤 Профиль", "✏️ Изменить имя", "✏️ Изменить фамилию" 
        if (profileCommandHandler.handleProfileCommand(text, chatId)) {
            return;
        }
        switch (text) {
            case "Помощь" -> sendHelp(chatId);
            case "🔍 Преподаватели" -> sendTeachersMenu(chatId);
            case "📝 Мои записи" -> showMyRegistrations(chatId);
            case "🔔 Подписки на обновления" -> showMySubscriptions(chatId);

            // Запросы консультаций
            case "❓ Запросить консультацию" -> startRequestCreation(chatId);
            case "📋 Просмотреть запросы" -> showMyRequests(chatId);

            // Меню преподавателей
            case "👥 Все преподаватели" -> showAllTeachers(chatId);
            case "🔍 Поиск преподавателя" -> startTeacherSearch(chatId);

            // Фильтры консультаций
            case "📅 Все" -> applyConsultationFilter(chatId, "all");
            case "⏭️ Будущие" -> applyConsultationFilter(chatId, "future");
            case "⏮️ Прошедшие" -> applyConsultationFilter(chatId, "past");

            // Подписки
            case "🔔 Подписаться" -> handleSubscribe(chatId);
            case "🔕 Отписаться" -> handleUnsubscribe(chatId);

            // Действия с консультацией
            case "✅ Записаться" -> startConsultationRegistration(chatId);
            case "❌ Отменить запись" -> handleCancelRegistration(chatId);

            // Действия с запросами консультаций
            case "✅ Записаться на запрос" -> startRequestRegistration(chatId);
            case "❌ Отписаться от запроса" -> handleRequestUnregistration(chatId);

            // Навигация
            case "🔙 К преподавателям" -> sendTeachersMenu(chatId);
            case "◀️ Назад к списку" -> backToConsultationsList(chatId);

            default ->{
                // Обработка состояний ввода
                if (currentState == UserState.WAITING_FOR_TEACHER_NAME) {
                    processTeacherSearch(text, chatId);
                    return;
                }
                botMessenger.sendText(
                    "Извините, я не понимаю эту команду. Отправьте 'Помощь' для получения списка доступных команд.",
                    chatId
                );
            } 
        }
    }

    // ========== Главное меню и справка ==========

    public void sendMainMenu(Long chatId) {
        // Полная очистка всех данных при возврате в главное меню
        stateManager.clearUserData(chatId);
        botMessenger.execute(SendMessage.builder()
                .text("Добро пожаловать, студент! Выберите действие:")
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildMainMenu())
                .build());
    }

    public void sendHelp(Long chatId) {
        String helpText = """
                Доступные команды для студента:
                
                🔍 Преподаватели - поиск преподавателей и консультаций
                📝 Мои записи - просмотр ваших записей на консультации
                🔔 Подписки на обновления - управление подписками на преподавателей
                ❓ Запросить консультацию - создание запроса на консультацию
                📋 Просмотреть запросы - просмотр и поддержка запросов
                👤 Профиль - редактирование имени и фамилии
                
                В разделе "🔍 Преподаватели":
                • 👥 Все преподаватели - список всех преподавателей (первые 5 в кнопках, можно искать по имени/фамилии)
                • 🔍 Поиск преподавателя - поиск по имени или фамилии
                • После выбора преподавателя: просмотр консультаций, подписка на обновления
                • Фильтры консультаций: 📅 Все / ⏭️ Будущие / ⏮️ Прошедшие
                • Введите №... для выбора конкретной консультации
                • После выбора конкретной консультации
                 * Запись на консультацию с указанием темы
                 * Отмена записи
                
                В разделе "📋 Просмотреть запросы":
                • Просмотр всех запросов студентов (введите №... для выбора)
                • Поддержка запроса - укажите тему и запишитесь
                • После выбора конкретного запроса, можно записать/отписаться, так же как и на консультацию
                • После принятия преподавателем все заинтересованные автоматически переносятся в консультацию
                
                💡 Подписки дают уведомления о:
                • Новых консультациях преподавателя
                • Появлении свободных мест
                • Изменениях и отменах консультаций
                """;
        botMessenger.sendText(helpText, chatId);
    }

    // ========== Работа с преподавателями ==========

    private void sendTeachersMenu(Long chatId) {
        // Очищаем выбранного преподавателя при возврате к меню преподавателей
        stateManager.clearCurrentTeacher(chatId);
        stateManager.clearCurrentConsultation(chatId);
        stateManager.resetState(chatId);

        botMessenger.execute(SendMessage.builder()
                .text("Выберите действие для работы с преподавателями:")
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeachersMenu())
                .build());
    }

    private void showAllTeachers(Long chatId) {
        List<TelegramUser> teachers = teacherSearchService.getAllTeachers();

        if (teachers.isEmpty()) {
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("В данный момент нет доступных преподавателей")
                    .replyMarkup(keyboardBuilder.buildMainMenu())
                    .build());
            return;
        }

        StringBuilder message = new StringBuilder("Список преподавателей:\n\n");

        // Показываем первых 5 преподавателей в кнопках
        int count = 0;
        for (TelegramUser teacher : teachers) {
            if (count >= 5) break;
            message.append(TeacherNameFormatter.formatFullName(teacher)).append("\n");
            count++;
        }

        // Если преподавателей больше 5, сообщаем об этом
        if (teachers.size() > 5) {
            message.append("\n... и ещё ").append(teachers.size() - 5).append(" преподавателей\n");
            message.append("\nИспользуйте поиск или выберите из первых 5:");
        } else {
            message.append("\nВыберите преподавателя из списка или используйте поиск:");
        }

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(message.toString())
                .replyMarkup(keyboardBuilder.buildTeacherSearchResults(teachers))
                .build());
    }

    private void startTeacherSearch(Long chatId) {
        stateManager.setState(chatId, UserState.WAITING_FOR_TEACHER_NAME);
        botMessenger.sendText(
                "Введите часть имени или фамилии преподавателя (или полностью имя и фамилию) для поиска:",
                chatId
        );
    }

    private void processTeacherSearch(String searchQuery, Long chatId) {
        stateManager.resetState(chatId);

        List<TelegramUser> teachers = teacherSearchService.searchTeachers(searchQuery);

        if (teachers.isEmpty()) {
            botMessenger.sendText(
                    "Преподаватели не найдены. Попробуйте другой запрос или выберите из общего списка.",
                    chatId
            );
            return;
        }

        StringBuilder message = new StringBuilder("Найденные преподаватели:\n\n");
        for (TelegramUser teacher : teachers) {
            message.append(TeacherNameFormatter.formatFullName(teacher)).append("\n");
        }
        message.append("\nВыберите преподавателя, чтобы увидеть его консультации.");

        botMessenger.execute(SendMessage.builder()
                .text(message.toString())
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeacherSearchResults(teachers))
                .build());
    }

    private void handleTeacherSelection(String teacherButton, Long chatId) {
        TelegramUser teacher = teacherSearchService.findByIdFromButton(teacherButton);

        if (teacher == null) {
            botMessenger.sendText("Преподаватель не найден", chatId);
            return;
        }

        stateManager.setCurrentTeacher(chatId, teacher.getId());
        stateManager.setFilter(chatId, "future");
        showTeacherConsultations(chatId, teacher);
    }

    private void showTeacherConsultations(Long chatId, TelegramUser teacher) {
        String filter = stateManager.getFilter(chatId);
        List<Consultation> consultations = consultationService.getTeacherConsultations(teacher, filter);
        boolean isSubscribed = checkSubscription(chatId, teacher);

        String messageText = messageFormatter.formatConsultationsList(teacher, consultations, filter);

        // Просмотр списка консультаций: устанавливаем состояние, очищаем ID конкретной консультации
        stateManager.clearCurrentConsultation(chatId);
        stateManager.setState(chatId, UserState.VIEWING_CONSULTATION_DETAILS);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeacherConsultations(consultations, isSubscribed))
                .build());
    }

    private void applyConsultationFilter(Long chatId, String filter) {
        TelegramUser teacher = getCurrentTeacherWithValidation(chatId);
        if (teacher == null) return;

        stateManager.setFilter(chatId, filter);
        showTeacherConsultations(chatId, teacher);
    }

    // ========== Управление подписками ==========

    private void handleSubscribe(Long chatId) {
        handleSubscriptionAction(chatId, true);
    }

    private void handleUnsubscribe(Long chatId) {
        handleSubscriptionAction(chatId, false);
    }

    /**
     * Универсальный обработчик подписки/отписки
     *
     * @param isSubscribe true - подписка, false - отписка
     */
    private void handleSubscriptionAction(Long chatId, boolean isSubscribe) {
        TelegramUser teacher = getCurrentTeacherWithValidation(chatId);
        if (teacher == null) return;

        TelegramUser student = getCurrentStudent(chatId);

        StudentServiceBot.SubscriptionResult result = isSubscribe
                ? studentServiceBot.subscribe(student, teacher)
                : studentServiceBot.unsubscribe(student, teacher);

        if (!result.success()) {
            botMessenger.sendText(result.message(), chatId);
        } else {
            String actionText = isSubscribe ? "подписались на" : "отписались от";
            botMessenger.sendText(
                    String.format("Вы успешно %s обновления преподавателя %s",
                            actionText,
                            TeacherNameFormatter.formatFullName(teacher)),
                    chatId
            );
        }

        // Решил что не надо отображать повторно список консультаций
        //showTeacherConsultations(chatId, teacher);
    }

    private void showMySubscriptions(Long chatId) {
        TelegramUser student = getCurrentStudent(chatId);
        List<Subscription> subscriptions = studentServiceBot.getStudentSubscriptions(student);
        String message = messageFormatter.formatSubscriptions(subscriptions);
        botMessenger.sendText(message, chatId);
    }

    // ========== Работа с консультациями ==========

    /**
     * Универсальный обработчик выбора по номеру (консультации или запроса)
     */
    private void handleNumberSelection(String text, Long chatId) {
        UserState currentState = stateManager.getState(chatId);

        try {
            Long id = extractId(text);

            // Определяем тип по состоянию
            if (currentState == UserState.VIEWING_REQUEST_DETAILS) {
                showRequestDetails(chatId, id);
            } else if (currentState == UserState.VIEWING_CONSULTATION_DETAILS) {
                showConsultationDetails(chatId, id);
            } else {
                // Неизвестное состояние - сообщаем об ошибке
                botMessenger.sendText(
                        "Ошибка: неверный контекст для выбора по номеру.\n" +
                                "Пожалуйста, перейдите в раздел консультаций или запросов.",
                        chatId
                );
            }
        } catch (Exception e) {
            log.error("Error parsing ID from '{}': {}", text, e.getMessage());
            botMessenger.sendText(
                    "Неверный формат номера.\nИспользуйте формат: №123",
                    chatId
            );
        }
    }

    private Long extractId(String text) {
        // Формат: "№123" или "№123 - 15.10 14:00" или "№123 - Название"
        String idStr = text.contains(" ")
                ? text.substring(1, text.indexOf(" "))
                : text.substring(1);
        return Long.parseLong(idStr);
    }

    /**
     * Показать консультацию из уведомления (по клику на inline-кнопку)
     * Устанавливает необходимые состояния и показывает детали
     */
    public void showConsultationFromNotification(Long consultationId, Long chatId) {
        Consultation consultation = consultationService.findById(consultationId);
        if (consultation == null) {
            botMessenger.sendText("Консультация не найдена или удалена", chatId);
            return;
        }

        // Устанавливаем преподавателя и фильтр
        stateManager.clearUserData(chatId);
        stateManager.setCurrentTeacher(chatId, consultation.getTeacher().getId());
        stateManager.setFilter(chatId, "future");

        // Показываем детали консультации
        showConsultationDetails(chatId, consultationId);
    }

    private void showConsultationDetails(Long chatId, Long consultationId) {
        Consultation consultation = consultationService.findById(consultationId);
        if (consultation == null) {
            botMessenger.sendText("Консультация не найдена", chatId);
            return;
        }

        // Просмотр конкретной консультации: устанавливаем ID и состояние
        stateManager.setCurrentConsultation(chatId, consultationId);
        stateManager.setState(chatId, UserState.VIEWING_CONSULTATION_DETAILS);

        TelegramUser student = getCurrentStudent(chatId);
        long registeredCount = studentServiceBot.getRegisteredCount(consultation);
        boolean isRegistered = studentServiceBot.isRegistered(student, consultation);

        // Получаем регистрацию студента, если он записан
        StudentConsultation studentRegistration = studentServiceBot.getStudentRegistration(student, consultation).orElse(null);

        String messageText = messageFormatter.formatConsultationDetails(consultation, registeredCount, studentRegistration);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildConsultationDetails(consultation, isRegistered))
                .build());
    }

    private void backToConsultationsList(Long chatId) {
        // Проверяем, откуда мы пришли - из консультаций преподавателя или из запросов
        Long teacherId = stateManager.getCurrentTeacher(chatId);
        Long requestId = stateManager.getCurrentRequest(chatId);

        // Приоритет: если есть teacherId - возврат к консультациям преподавателя
        if (teacherId != null) {
            // Очищаем ID консультации, но сохраняем преподавателя
            stateManager.clearCurrentConsultation(chatId);

            TelegramUser teacher = teacherSearchService.findById(teacherId);
            if (teacher != null) {
                showTeacherConsultations(chatId, teacher);
                return;
            }
        }

        // Если нет teacherId, но есть requestId - возврат к списку запросов
        if (requestId != null) {
            // Очищаем ID запроса перед показом списка
            stateManager.clearCurrentRequest(chatId);
            showMyRequests(chatId);
            return;
        }

        // В остальных случаях - главное меню
        stateManager.clearUserData(chatId);
        sendMainMenu(chatId);
    }

    // ========== Регистрация на консультацию ==========

    private void startConsultationRegistration(Long chatId) {
        Consultation consultation = getCurrentConsultationWithValidation(chatId);
        if (consultation == null) return;

        TelegramUser student = getCurrentStudent(chatId);
        long registeredCount = studentServiceBot.getRegisteredCount(consultation);

        // Проверка: уже записан?
        if (studentServiceBot.isRegistered(student, consultation)) {
            botMessenger.sendText("Вы уже записаны на эту консультацию", chatId);
            return;
        }

        // Валидация консультации (статус, вместимость)
        ConsultationService.ValidationResult validation =
                consultationService.validateForRegistration(consultation, registeredCount);

        if (!validation.isValid()) {
            botMessenger.sendText(validation.errorMessage(), chatId);
            return;
        }

        // Запрашиваем тему/вопрос от студента
        stateManager.setState(chatId, UserState.WAITING_FOR_CONSULTATION_MESSAGE);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Пожалуйста, укажите тему или вопрос, который хотите обсудить на консультации:
                        
                        Например: "Разбор темы 'Рекурсия'" или "Помощь с курсовой работой""")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    private void processConsultationRegistration(String message, Long chatId) {
        Consultation consultation = getCurrentConsultationWithValidation(chatId);
        if (consultation == null) {
            stateManager.resetState(chatId);
            return;
        }

        TelegramUser student = getCurrentStudent(chatId);
        StudentServiceBot.RegistrationResult result = studentServiceBot.register(student, consultation, message);

        stateManager.resetState(chatId);

        if (!result.success()) {
            botMessenger.sendText(result.message(), chatId);
        } else {
            String confirmMessage = messageFormatter.formatRegistrationConfirmation();
            botMessenger.sendText(confirmMessage, chatId);

            // Проверяем автозакрытие (передаём ID для перезагрузки свежих данных)
            teacherConsultationService.checkAndAutoClose(consultation.getId());
        }

        showConsultationDetails(chatId, consultation.getId());
    }

    private void handleCancelRegistration(Long chatId) {
        Consultation consultation = getCurrentConsultationWithValidation(chatId);
        if (consultation == null) return;

        TelegramUser student = getCurrentStudent(chatId);

        // Считаем до отмены
        long countBefore = studentServiceBot.getRegisteredCount(consultation);

        StudentServiceBot.RegistrationResult result = studentServiceBot.cancelRegistration(student, consultation);

        if (!result.success()) {
            botMessenger.sendText(result.message(), chatId);
        } else {
            String confirmMessage = messageFormatter.formatCancellationConfirmation();
            botMessenger.sendText(confirmMessage, chatId);

            // Перезагружаем консультацию для актуальных данных
            consultation = consultationService.findById(consultation.getId());

            // Считаем после отмены
            long countAfter = studentServiceBot.getRegisteredCount(consultation);


            teacherConsultationService.checkAndAutoOpen(consultation.getId(), countBefore);
            // Если освободилось место, уведомляем подписчиков
            if (countAfter < countBefore) {
                // Отправляем уведомления (исключая текущего студента)
                notificationService.notifySubscribersAvailableSpots(consultation.getId(), student.getId());
            }
        }

        showConsultationDetails(chatId, consultation.getId());
    }

    private void showMyRegistrations(Long chatId) {
        TelegramUser student = getCurrentStudent(chatId);
        List<StudentConsultation> registrations = studentServiceBot.getStudentRegistrations(student);
        String message = messageFormatter.formatStudentRegistrations(registrations);

        if (registrations.isEmpty()) {
            // Нет записей - просто показываем сообщение
            botMessenger.sendText(message, chatId);
        } else {
            // Есть записи - устанавливаем состояние для возможности выбора по номеру
            stateManager.setState(chatId, UserState.VIEWING_CONSULTATION_DETAILS);
            stateManager.clearCurrentConsultation(chatId);
            
            // Извлекаем консультации из регистраций
            List<Consultation> consultations = registrations.stream()
                    .map(StudentConsultation::getConsultation)
                    .toList();
            
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildMyRegistrationsList(consultations))
                    .build());
        }
    }

    // ========== Вспомогательные методы ==========

    /**
     * Обработка кнопки отмены для всех состояний ввода
     */
    private boolean handleCancelButton(String text, Long chatId, UserState currentState) {
        if (!text.equals("❌ Отмена")) {
            return false;
        }

        stateManager.resetState(chatId);

        switch (currentState) {
            case WAITING_FOR_CONSULTATION_MESSAGE -> {
                Long consultationId = stateManager.getCurrentConsultation(chatId);
                if (consultationId != null) {
                    showConsultationDetails(chatId, consultationId);
                } else {
                    sendMainMenu(chatId);
                }
            }
            case WAITING_FOR_REQUEST_TITLE -> sendMainMenu(chatId);
            case WAITING_FOR_REQUEST_MESSAGE -> {
                Long requestId = stateManager.getCurrentRequest(chatId);
                if (requestId != null) {
                    showRequestDetails(chatId, requestId);
                } else {
                    showMyRequests(chatId);
                }
            }
            default -> sendMainMenu(chatId);
        }

        return true;
    }

    /**
     * Получить текущего студента по chatId
     * Гарантированно вернет пользователя, т.к. UpdateConsumer проверяет регистрацию
     */
    private TelegramUser getCurrentStudent(Long chatId) {
        return telegramUserRepository.findByTelegramId(chatId).orElseThrow();
    }

    /**
     * Получить текущего преподавателя из состояния с валидацией
     *
     * @return преподаватель или null если не найден
     */
    private TelegramUser getCurrentTeacherWithValidation(Long chatId) {
        Long teacherId = stateManager.getCurrentTeacher(chatId);
        if (teacherId == null) {
            botMessenger.sendText("Сначала выберите преподавателя", chatId);
            return null;
        }

        TelegramUser teacher = teacherSearchService.findById(teacherId);
        if (teacher == null) {
            botMessenger.sendText("Преподаватель не найден", chatId);
            return null;
        }

        return teacher;
    }

    /**
     * Получить текущую консультацию из состояния с валидацией
     *
     * @return консультация или null если не найдена
     */
    private Consultation getCurrentConsultationWithValidation(Long chatId) {
        Long consultationId = stateManager.getCurrentConsultation(chatId);
        if (consultationId == null) {
            botMessenger.sendText("Ошибка: консультация не выбрана", chatId);
            return null;
        }

        Consultation consultation = consultationService.findById(consultationId);
        if (consultation == null) {
            botMessenger.sendText("Консультация не найдена", chatId);
            return null;
        }

        return consultation;
    }

    /**
     * Проверить подписку студента на преподавателя
     */
    private boolean checkSubscription(Long chatId, TelegramUser teacher) {
        TelegramUser student = getCurrentStudent(chatId);
        return studentServiceBot.isSubscribed(student, teacher);
    }

    // ========== Работа с запросами консультаций ==========

    /**
     * Начать создание запроса консультации
     */
    private void startRequestCreation(Long chatId) {
        stateManager.setState(chatId, UserState.WAITING_FOR_REQUEST_TITLE);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("""
                        ❓ Создание запроса консультации
                        
                        Введите тему консультации, которая вам нужна.
                        Например: "Помощь с курсовой работой по Java" или "Разбор темы Многопоточность"
                        
                        Ваш запрос увидят все преподаватели, и кто-то из них сможет его принять.""")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    /**
     * Обработать создание запроса (после ввода темы)
     */
    private void processRequestCreation(String title, Long chatId) {
        if (title == null || title.trim().isEmpty()) {
            botMessenger.sendText("Тема не может быть пустой. Попробуйте еще раз:", chatId);
            return;
        }

        if (title.length() > 200) {
            botMessenger.sendText(
                    "Тема слишком длинная (максимум 200 символов). Попробуйте сократить:",
                    chatId
            );
            return;
        }

        TelegramUser student = getCurrentStudent(chatId);
        Consultation request = consultationRequestService.createRequest(student, title.trim());

        stateManager.resetState(chatId);

        String message = messageFormatter.formatRequestCreationConfirmation(request);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .replyMarkup(keyboardBuilder.buildMainMenu())
                .build());
    }

    /**
     * Показать все запросы консультаций (от всех студентов)
     */
    private void showMyRequests(Long chatId) {
        List<Consultation> requests = consultationRequestService.getAllRequests();

        String message = messageFormatter.formatRequestsList(requests);

        if (requests.isEmpty()) {
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildMainMenu())
                    .build());
        } else {
            // Просмотр списка запросов: устанавливаем состояние, очищаем ID конкретного запроса
            stateManager.clearCurrentRequest(chatId);
            stateManager.setState(chatId, UserState.VIEWING_REQUEST_DETAILS);
            botMessenger.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .replyMarkup(keyboardBuilder.buildRequestsList(requests))
                    .build());
        }
    }

    /**
     * Показать детали конкретного запроса
     */
    private void showRequestDetails(Long chatId, Long requestId) {
        consultationRequestService.findRequestById(requestId).ifPresentOrElse(
                request -> {
                    // Просмотр конкретного запроса: устанавливаем ID и состояние
                    stateManager.setCurrentRequest(chatId, requestId);
                    stateManager.setState(chatId, UserState.VIEWING_REQUEST_DETAILS);

                    TelegramUser student = getCurrentStudent(chatId);
                    boolean isRegistered = consultationRequestService.isRegisteredOnRequest(student, request);

                    String message = messageFormatter.formatRequestDetails(request);
                    botMessenger.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(message)
                            .replyMarkup(keyboardBuilder.buildRequestDetails(isRegistered))
                            .build());
                },
                () -> botMessenger.sendText("Запрос не найден.", chatId)
        );
    }

    /**
     * Начать регистрацию на запрос (запрашиваем сообщение)
     */
    private void startRequestRegistration(Long chatId) {
        Long requestId = stateManager.getCurrentRequest(chatId);
        if (requestId == null) {
            botMessenger.sendText("Ошибка: запрос не выбран", chatId);
            return;
        }

        stateManager.setState(chatId, UserState.WAITING_FOR_REQUEST_MESSAGE);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Пожалуйста, укажите тему или вопрос, который хотите обсудить:
                        
                        Например: "Интересует эта тема" или "Тоже нужна помощь""")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    /**
     * Обработать регистрацию на запрос (после ввода сообщения)
     */
    private void processRequestRegistration(String message, Long chatId) {
        Long requestId = stateManager.getCurrentRequest(chatId);
        if (requestId == null) {
            botMessenger.sendText("Ошибка: запрос не выбран", chatId);
            stateManager.resetState(chatId);
            return;
        }

        consultationRequestService.findRequestById(requestId).ifPresentOrElse(
                request -> {
                    TelegramUser student = getCurrentStudent(chatId);
                    ConsultationRequestService.RequestRegistrationResult result =
                            consultationRequestService.registerOnRequest(student, request, message);

                    stateManager.resetState(chatId);

                    if (!result.success()) {
                        botMessenger.sendText(result.message(), chatId);
                    } else {
                        botMessenger.sendText(
                                """
                                        ✅ Вы успешно записались на запрос!
                                        
                                        Когда преподаватель примет этот запрос и создаст консультацию, \
                                        вы автоматически будете записаны на неё.""",
                                chatId
                        );
                    }

                    showRequestDetails(chatId, requestId);
                },
                () -> {
                    botMessenger.sendText("Запрос не найден.", chatId);
                    stateManager.resetState(chatId);
                }
        );
    }

    /**
     * Отписаться от запроса
     */
    private void handleRequestUnregistration(Long chatId) {
        Long requestId = stateManager.getCurrentRequest(chatId);
        if (requestId == null) {
            botMessenger.sendText("Ошибка: запрос не выбран", chatId);
            return;
        }

        consultationRequestService.findRequestById(requestId).ifPresentOrElse(
                request -> {
                    TelegramUser student = getCurrentStudent(chatId);
                    ConsultationRequestService.RequestUnregistrationResult result =
                            consultationRequestService.unregisterFromRequest(student, request);

                    if (!result.success()) {
                        botMessenger.sendText(result.message(), chatId);
                        showRequestDetails(chatId, requestId);
                    } else {
                        botMessenger.sendText(result.message(), chatId);

                        if (result.requestDeleted()) {
                            // Запрос удалён - возврат к списку
                            showMyRequests(chatId);
                        } else {
                            // Запрос остался - обновляем детали
                            showRequestDetails(chatId, requestId);
                        }
                    }
                },
                () -> botMessenger.sendText("Запрос не найден.", chatId)
        );
    }
}


