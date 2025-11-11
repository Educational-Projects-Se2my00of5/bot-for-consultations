package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.TeacherSearchService;
import com.example.botforconsultations.api.bot.service.ConsultationService;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager.DeaneryState;
import com.example.botforconsultations.api.bot.utils.DeaneryKeyboardBuilder;
import com.example.botforconsultations.api.bot.utils.KeyboardConstants;
import com.example.botforconsultations.api.bot.utils.TeacherNameFormatter;
import com.example.botforconsultations.api.bot.utils.ConsultationMessageFormatter;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

/**
 * Обработчик команд деканата
 * Управляет поиском преподавателей, просмотром консультаций и управлением задачами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeaneryCommandHandler {

    // Репозитории
    private final TelegramUserRepository telegramUserRepository;

    // Сервисы
    private final TeacherSearchService teacherSearchService;
    private final ConsultationService consultationService;
    private final ProfileCommandHandler profileCommandHandler;
    private final BotMessenger botMessenger;

    // Утилиты
    private final DeaneryStateManager stateManager;
    private final DeaneryKeyboardBuilder keyboardBuilder;
    private final ConsultationMessageFormatter messageFormatter;

    /**
     * Главный обработчик команд деканата
     */
    public void handleDeaneryCommand(String text, Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);

        // Проверка кнопки "Отмена" - обрабатывается в первую очередь
        if (text.equals(KeyboardConstants.CANCEL)) {
            handleCancel(chatId);
            return;
        }

        // Обработка состояний ввода
        if (currentState != DeaneryState.DEFAULT 
                && currentState != DeaneryState.VIEWING_TEACHER_CONSULTATIONS
                && currentState != DeaneryState.VIEWING_CONSULTATION_DETAILS) {
            switch (currentState) {
                case WAITING_FOR_TEACHER_NAME -> processTeacherSearch(text, chatId);
                case CREATING_TODO_TITLE -> processTaskTitle(text, chatId);
                case CREATING_TODO_DESCRIPTION -> processTaskDescription(text, chatId);
                case CREATING_TODO_DEADLINE -> processTaskDeadline(text, chatId);
                case EDITING_PROFILE_FIRST_NAME -> {
                    profileCommandHandler.processFirstNameUpdate(text, chatId, getCurrentDeanery(chatId));
                    stateManager.resetState(chatId);
                }
                case EDITING_PROFILE_LAST_NAME -> {
                    profileCommandHandler.processLastNameUpdate(text, chatId, getCurrentDeanery(chatId));
                    stateManager.resetState(chatId);
                }
                // TODO: добавить обработку редактирования задач
                default -> {
                } // Никогда не должно произойти из-за условия if
            }
            return;
        }

        // Обработка выбора преподавателя (кнопка начинается с эмодзи)
        if (text.startsWith(KeyboardConstants.TEACHER_PREFIX)) {
            handleTeacherSelection(text, chatId);
            return;
        }

        // Обработка выбора консультации/задачи по номеру в режиме просмотра
        if ((currentState == DeaneryState.VIEWING_TEACHER_CONSULTATIONS ||
                currentState == DeaneryState.VIEWING_CONSULTATION_DETAILS) &&
                text.startsWith(KeyboardConstants.NUMBER_PREFIX)
        ) {
            handleNumberSelection(text, chatId);
            return;
        }

        // Основные команды профиля
        if (profileCommandHandler.handleProfileCommand(text, chatId)) {
            return;
        }

        // Основные команды
        switch (text) {
            case KeyboardConstants.HELP -> sendHelp(chatId);
            
            // Меню преподавателей
            case KeyboardConstants.TEACHERS_MENU -> sendTeachersMenu(chatId);
            case KeyboardConstants.ALL_TEACHERS -> showAllTeachers(chatId);
            case KeyboardConstants.SEARCH_TEACHER -> startTeacherSearch(chatId);
            
            // TODO: Управление задачами
            case KeyboardConstants.ALL_TASKS -> showAllTasks(chatId);
            case KeyboardConstants.CREATE_TASK -> startTaskCreation(chatId);
            case KeyboardConstants.TEACHER_TASKS -> showTeacherTasks(chatId);
            
            // Просмотр консультации
            case KeyboardConstants.STUDENT_LIST -> showStudentList(chatId);
            
            // TODO: Управление задачей
            case KeyboardConstants.MARK_COMPLETED -> markTaskCompleted(chatId);
            case KeyboardConstants.MARK_PENDING -> markTaskPending(chatId);
            case KeyboardConstants.EDIT_TASK -> startEditTask(chatId);
            case KeyboardConstants.DELETE_TASK -> startDeleteTask(chatId);
            case KeyboardConstants.CONFIRM_DELETE -> confirmDeleteTask(chatId);

            // Навигация
            case KeyboardConstants.BACK_TO_TEACHERS -> sendTeachersMenu(chatId);
            case KeyboardConstants.BACK_TO_LIST -> backToList(chatId);
            case KeyboardConstants.BACK -> handleBackButton(chatId);

            // Фильтры консультаций
            case KeyboardConstants.FILTER_PAST -> applyConsultationFilter(chatId, "past");
            case KeyboardConstants.FILTER_ALL -> applyConsultationFilter(chatId, "all");
            case KeyboardConstants.FILTER_FUTURE -> applyConsultationFilter(chatId, "future");

            default -> botMessenger.sendText(
                    "Извините, я не понимаю эту команду. Отправьте 'Помощь' для получения списка доступных команд.",
                    chatId
            );
        }
    }

    /**
     * Обработчик команд для неподтвержденных сотрудников деканата
     * Они могут редактировать только свой профиль
     */
    public void handleUnconfirmedDeaneryCommand(String text, Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);

        // Обработка состояний ввода для неподтвержденных сотрудников деканата
        if (currentState == DeaneryState.WAITING_APPROVAL_EDITING_FIRST_NAME) {
            profileCommandHandler.processFirstNameUpdate(text, chatId, getCurrentDeanery(chatId));
            stateManager.resetState(chatId);
            return;
        }

        if (currentState == DeaneryState.WAITING_APPROVAL_EDITING_LAST_NAME) {
            profileCommandHandler.processLastNameUpdate(text, chatId, getCurrentDeanery(chatId));
            stateManager.resetState(chatId);
            return;
        }

        // Основные команды профиля
        if (profileCommandHandler.handleProfileCommand(text, chatId)) {
            return;
        }
        
        switch (text) {
            case KeyboardConstants.BACK -> sendWaitingApprovalMenu(chatId);
            default -> botMessenger.sendText(
                    "Извините, я не понимаю эту команду.",
                    chatId
            );
        }
    }

    /**
     * Отправить меню ожидания подтверждения
     */
    public void sendWaitingApprovalMenu(Long chatId) {
        stateManager.resetState(chatId);
        botMessenger.execute(SendMessage.builder()
                .text("⏳ Ваш аккаунт ожидает подтверждения администратором.\n\n" +
                        "После подтверждения вы сможете управлять консультациями и задачами.\n\n" +
                        "Пока вы можете редактировать свой профиль:")
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildWaitingForApprovalMenu())
                .build());
    }

    // ========== Главное меню и справка ==========

    public void sendMainMenu(Long chatId) {
        // Полная очистка всех данных при возврате в главное меню
        stateManager.clearUserData(chatId);
        botMessenger.execute(SendMessage.builder()
                .text("Добро пожаловать в панель деканата! Выберите действие:")
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildMainMenu())
                .build());
    }

    public void sendHelp(Long chatId) {
        String helpText = """
                Доступные команды для деканата:
                
                🔍 Преподаватели - поиск преподавателей, просмотр консультаций и управление задачами
                📋 Все задачи - просмотр всех созданных задач с фильтрацией
                👤 Профиль - редактирование имени и фамилии
                
                В разделе "🔍 Преподаватели":
                • 👥 Все преподаватели - список всех преподавателей
                • 🔍 Поиск преподавателя - поиск по имени или фамилии
                • После выбора преподавателя: просмотр консультаций и задач
                • Создание задач для преподавателей
                • Фильтрация консультаций: ⏮️ Прошедшие / 📅 Все / ⏭️ Будущие
                
                В разделе "📋 Все задачи":
                • Просмотр всех задач с фильтрацией
                • Фильтры по дедлайну: ⏮️ Прошедшие / 📅 Все / ⏭️ Будущие
                • Фильтры по статусу: ❌ Невыполненные / 📋 Все / ✅ Выполненные
                • Поиск задачи по номеру (введите №...)
                • Редактирование и удаление задач
                """;
        botMessenger.sendText(helpText, chatId);
    }

    // ========== Работа с преподавателями ==========

    /**
     * Отправить меню для работы с преподавателями
     */
    private void sendTeachersMenu(Long chatId) {
        stateManager.resetState(chatId);
        botMessenger.execute(SendMessage.builder()
                .text("Выберите действие:")
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeachersMenu())
                .build());
    }

    /**
     * Показать всех преподавателей (первые 5 в кнопках)
     */
    private void showAllTeachers(Long chatId) {
        List<TelegramUser> teachers = teacherSearchService.getAllTeachers();

        if (teachers.isEmpty()) {
            botMessenger.sendText("Преподаватели не найдены.", chatId);
            return;
        }

        stateManager.setState(chatId, DeaneryState.DEFAULT);

        String message = formatTeachersList(teachers);
        botMessenger.execute(SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeacherListKeyboard(teachers))
                .build());
    }

    /**
     * Начать поиск преподавателя
     */
    private void startTeacherSearch(Long chatId) {
        stateManager.setState(chatId, DeaneryState.WAITING_FOR_TEACHER_NAME);
        botMessenger.sendText(
                "Введите имя или фамилию преподавателя для поиска:",
                chatId
        );
    }

    /**
     * Обработать поиск преподавателя
     */
    private void processTeacherSearch(String searchQuery, Long chatId) {
        List<TelegramUser> teachers = teacherSearchService.searchTeachers(searchQuery);

        if (teachers.isEmpty()) {
            botMessenger.sendText(
                    "Преподаватели не найдены. Попробуйте другой запрос или вернитесь назад.",
                    chatId
            );
            return;
        }

        stateManager.resetState(chatId);

        String message = "Найдено преподавателей: " + teachers.size() + "\n\n" + formatTeachersList(teachers);
        botMessenger.execute(SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeacherListKeyboard(teachers))
                .build());
    }

    /**
     * Обработать выбор преподавателя
     */
    private void handleTeacherSelection(String buttonText, Long chatId) {
        TelegramUser teacher = teacherSearchService.findByIdFromButton(buttonText);
        if (teacher == null) {
            botMessenger.sendText("Преподаватель не найден.", chatId);
            return;
        }

        // Сохраняем выбранного преподавателя
        stateManager.setCurrentTeacher(chatId, teacher.getId());
        stateManager.setState(chatId, DeaneryState.VIEWING_TEACHER_CONSULTATIONS);
        stateManager.setFilter(chatId, "future");

        showTeacherConsultations(chatId, teacher);
    }

    /**
     * Показать консультации выбранного преподавателя
     */
    private void showTeacherConsultations(Long chatId, TelegramUser teacher) {
        String filter = stateManager.getFilter(chatId);
        List<Consultation> consultations = consultationService.getTeacherConsultations(teacher, filter);

        String messageText = messageFormatter.formatConsultationsList(teacher, consultations, filter);

        // Просмотр списка консультаций: очищаем ID конкретной консультации
        stateManager.clearCurrentConsultation(chatId);
        stateManager.setState(chatId, DeaneryState.VIEWING_TEACHER_CONSULTATIONS);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeacherConsultations(consultations))
                .build());
    }

    /**
     * Применить фильтр к консультациям
     */
    private void applyConsultationFilter(Long chatId, String filter) {
        Long teacherId = stateManager.getCurrentTeacher(chatId);
        if (teacherId == null) {
            botMessenger.sendText("❌ Преподаватель не выбран. Вернитесь к списку преподавателей.", chatId);
            sendTeachersMenu(chatId);
            return;
        }

        TelegramUser teacher = teacherSearchService.findById(teacherId);
        
        if (teacher == null) {
            botMessenger.sendText("❌ Преподаватель не найден.", chatId);
            sendTeachersMenu(chatId);
            return;
        }

        stateManager.setFilter(chatId, filter);
        showTeacherConsultations(chatId, teacher);
    }

    /**
     * Обработать выбор по номеру (консультация или задача)
     */
    private void handleNumberSelection(String text, Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);

        try {
            Long id = extractId(text);

            // Определяем тип по состоянию
            if (currentState == DeaneryState.VIEWING_TEACHER_CONSULTATIONS || 
                currentState == DeaneryState.VIEWING_CONSULTATION_DETAILS) {
                showConsultationDetails(chatId, id);
            } else {
                // TODO: Обработка задач будет добавлена позже
                botMessenger.sendText(
                        "Ошибка: неверный контекст для выбора по номеру.\n" +
                        "Пожалуйста, перейдите в раздел консультаций или задач.",
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

    /**
     * Извлечь ID из текста кнопки
     */
    private Long extractId(String text) {
        // Формат: "№123" или "№123 - 15.10 14:00" или "№123 - Название"
        String idStr = text.contains(" ")
                ? text.substring(1, text.indexOf(" "))
                : text.substring(1);
        return Long.parseLong(idStr);
    }

    /**
     * Показать детали консультации
     */
    private void showConsultationDetails(Long chatId, Long consultationId) {
        Consultation consultation = consultationService.findById(consultationId);
        if (consultation == null) {
            botMessenger.sendText("❌ Консультация не найдена", chatId);
            return;
        }

        // Просмотр конкретной консультации: устанавливаем ID и состояние
        stateManager.setCurrentConsultation(chatId, consultationId);
        stateManager.setState(chatId, DeaneryState.VIEWING_CONSULTATION_DETAILS);

        long registeredCount = getRegisteredCount(consultation);
        String messageText = messageFormatter.formatConsultationDetails(consultation, registeredCount, null);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildConsultationDetailsForDeanery(consultation))
                .build());
    }

    /**
     * Получить количество зарегистрированных студентов на консультацию
     */
    private long getRegisteredCount(Consultation consultation) {
        return consultation.getRegUsers() != null
                ? consultation.getRegUsers().size()
                : 0;
    }

    /**
     * Показать список студентов, записанных на консультацию
     */
    private void showStudentList(Long chatId) {
        Long consultationId = stateManager.getCurrentConsultation(chatId);
        if (consultationId == null) {
            botMessenger.sendText("❌ Консультация не выбрана.", chatId);
            return;
        }

        Consultation consultation = consultationService.findById(consultationId);
        if (consultation == null) {
            botMessenger.sendText("❌ Консультация не найдена.", chatId);
            return;
        }

        String studentListText = formatStudentListForDeanery(consultation);
        botMessenger.execute(SendMessage.builder()
                .text(studentListText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildStudentListKeyboard())
                .build());
    }

    /**
     * Форматировать список студентов для деканата
     */
    private String formatStudentListForDeanery(Consultation consultation) {
        StringBuilder message = new StringBuilder();
        message.append("👥 Список студентов\n\n");
        message.append(String.format("📋 Консультация №%d\n", consultation.getId()));
        message.append(String.format("📅 %s в %s\n\n",
                consultation.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                consultation.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))));

        var regUsers = consultation.getRegUsers();
        if (regUsers == null || regUsers.isEmpty()) {
            message.append("❌ Нет записавшихся студентов");
        } else {
            Integer capacity = consultation.getCapacity();
            if (capacity != null) {
                message.append(String.format("Записано: %d/%d\n\n", regUsers.size(), capacity));
            } else {
                message.append(String.format("Записано: %d\n\n", regUsers.size()));
            }
            
            int counter = 1;
            for (var registration : regUsers) {
                TelegramUser student = registration.getStudent();
                message.append(String.format("%d. %s %s",
                        counter++,
                        student.getFirstName(),
                        student.getLastName() != null ? student.getLastName() : ""));
                
                if (registration.getMessage() != null && !registration.getMessage().isEmpty()) {
                    message.append(String.format("\n   💬 %s", registration.getMessage()));
                }
                message.append("\n\n");
            }
        }

        return message.toString();
    }

    /**
     * Вернуться к списку консультаций
     */
    private void backToConsultationsList(Long chatId) {
        Long teacherId = stateManager.getCurrentTeacher(chatId);
        if (teacherId == null) {
            botMessenger.sendText("❌ Преподаватель не выбран.", chatId);
            sendTeachersMenu(chatId);
            return;
        }

        TelegramUser teacher = teacherSearchService.findById(teacherId);
        if (teacher == null) {
            botMessenger.sendText("❌ Преподаватель не найден.", chatId);
            sendTeachersMenu(chatId);
            return;
        }

        // Очищаем ID консультации, но сохраняем преподавателя
        stateManager.clearCurrentConsultation(chatId);
        showTeacherConsultations(chatId, teacher);
    }

    // ========== Вспомогательные методы ==========

    /**
     * Обработка кнопки "Отмена"
     */
    private void handleCancel(Long chatId) {
        stateManager.resetState(chatId);
        stateManager.clearTempData(chatId);
        botMessenger.sendText("❌ Действие отменено.", chatId);
        sendMainMenu(chatId);
    }

    /**
     * Обработка кнопки "Назад"
     */
    private void handleBackButton(Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);
        
        switch (currentState) {
            case VIEWING_TEACHER_CONSULTATIONS -> sendTeachersMenu(chatId);
            case VIEWING_CONSULTATION_DETAILS -> {
                // TODO: Вернуться к списку консультаций преподавателя
                sendTeachersMenu(chatId);
            }
            default -> sendMainMenu(chatId);
        }
    }

    /**
     * Обработка кнопки "Назад к списку"
     */
    private void backToList(Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);
        
        if (currentState == DeaneryState.VIEWING_CONSULTATION_DETAILS) {
            // Вернуться к списку консультаций преподавателя
            backToConsultationsList(chatId);
        } else {
            sendMainMenu(chatId);
        }
    }

    // ========== Управление задачами (TODO: реализовать) ==========

    private void showAllTasks(Long chatId) {
        botMessenger.sendText("📋 Функционал просмотра всех задач будет реализован далее.", chatId);
    }

    private void startTaskCreation(Long chatId) {
        stateManager.setState(chatId, DeaneryState.CREATING_TODO_TITLE);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("➕ Создание новой задачи\n\n" +
                        "Шаг 1/3: Введите название задачи")
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    private void processTaskTitle(String title, Long chatId) {
        if (title == null || title.trim().isEmpty()) {
            botMessenger.sendText("Название не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        stateManager.setTempTitle(chatId, title.trim());
        stateManager.setState(chatId, DeaneryState.CREATING_TODO_DESCRIPTION);

        botMessenger.sendText(
                "✅ Название сохранено: \"" + title.trim() + "\"\n\n" +
                        "Шаг 2/3: Введите описание задачи",
                chatId
        );
    }

    private void processTaskDescription(String description, Long chatId) {
        if (description == null || description.trim().isEmpty()) {
            botMessenger.sendText("Описание не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        stateManager.setTempDescription(chatId, description.trim());
        stateManager.setState(chatId, DeaneryState.CREATING_TODO_DEADLINE);

        botMessenger.sendText(
                "✅ Описание сохранено.\n\n" +
                        "Шаг 3/3: Введите дедлайн\n" +
                        "Формат: ДД.ММ.ГГГГ\n" +
                        "Например: 15.12.2025",
                chatId
        );
    }

    private void processTaskDeadline(String deadlineText, Long chatId) {
        // TODO: Реализовать парсинг даты и создание задачи
        botMessenger.sendText("✅ Задача создана! (функционал в разработке)", chatId);
        stateManager.resetState(chatId);
        stateManager.clearTempData(chatId);
        sendMainMenu(chatId);
    }

    private void showTeacherTasks(Long chatId) {
        botMessenger.sendText("📝 Функционал просмотра задач преподавателя будет реализован далее.", chatId);
    }

    private void markTaskCompleted(Long chatId) {
        botMessenger.sendText("✅ Функционал отметки задачи как выполненной будет реализован далее.", chatId);
    }

    private void markTaskPending(Long chatId) {
        botMessenger.sendText("⏳ Функционал отметки задачи как невыполненной будет реализован далее.", chatId);
    }

    private void startEditTask(Long chatId) {
        botMessenger.sendText("✏️ Функционал редактирования задачи будет реализован далее.", chatId);
    }

    private void startDeleteTask(Long chatId) {
        botMessenger.sendText("❌ Функционал удаления задачи будет реализован далее.", chatId);
    }

    private void confirmDeleteTask(Long chatId) {
        botMessenger.sendText("✔️ Функционал подтверждения удаления задачи будет реализован далее.", chatId);
    }

    // ========== Форматирование ==========

    /**
     * Форматировать список преподавателей для отображения
     */
    private String formatTeachersList(List<TelegramUser> teachers) {
        StringBuilder message = new StringBuilder();
        int count = 1;
        for (TelegramUser teacher : teachers) {
            if (count > 5) {
                message.append("\n... и ещё ").append(teachers.size() - 5).append(" преподавателей");
                message.append("\nВведите имя или фамилию для поиска конкретного преподавателя.");
                break;
            }
            message.append(count++).append(". ")
                    .append(TeacherNameFormatter.formatFullName(teacher))
                    .append("\n");
        }
        return message.toString();
    }

    /**
     * Получить текущего пользователя деканата
     */
    private TelegramUser getCurrentDeanery(Long chatId) {
        return telegramUserRepository.findByTelegramId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь деканата не найден"));
    }
}
