package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.ConsultationService;
import com.example.botforconsultations.api.bot.service.TeacherSearchService;
import com.example.botforconsultations.api.bot.service.TodoTaskService;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager.DeaneryState;
import com.example.botforconsultations.api.bot.utils.ConsultationMessageFormatter;
import com.example.botforconsultations.api.bot.utils.DeaneryKeyboardBuilder;
import com.example.botforconsultations.api.bot.utils.KeyboardConstants;
import com.example.botforconsultations.api.bot.utils.TeacherNameFormatter;
import com.example.botforconsultations.api.bot.utils.TodoMessageFormatter;
import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final TodoTaskService todoTaskService;
    private final ProfileCommandHandler profileCommandHandler;
    private final BotMessenger botMessenger;

    // Утилиты
    private final DeaneryStateManager stateManager;
    private final DeaneryKeyboardBuilder keyboardBuilder;
    private final ConsultationMessageFormatter messageFormatter;
    private final TodoMessageFormatter todoMessageFormatter;

    @Autowired
    @Lazy
    private AuthCommandHandler authCommandHandler;

    /**
     * Главный обработчик команд деканата
     */
    public void handleDeaneryCommand(String text, Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);

        if (text.equals(KeyboardConstants.CANCEL)) {
            handleCancelButton(chatId);
            return;
        }


        // Обработка состояний ввода
        if (currentState != DeaneryState.DEFAULT
                && currentState != DeaneryState.VIEWING_TEACHER_CONSULTATIONS
                && currentState != DeaneryState.VIEWING_CONSULTATION_DETAILS
                && currentState != DeaneryState.VIEWING_TEACHER_TASKS
                && currentState != DeaneryState.VIEWING_TASK_DETAILS
                && currentState != DeaneryState.VIEWING_ALL_TASKS
                && currentState != DeaneryState.CONFIRMING_DELETE_TASK) {
            switch (currentState) {
                case WAITING_FOR_TEACHER_NAME -> processTeacherSearch(text, chatId);
                case CREATING_TODO_TITLE -> processTaskTitle(text, chatId);
                case CREATING_TODO_DESCRIPTION -> processTaskDescription(text, chatId);
                case CREATING_TODO_DEADLINE -> processTaskDeadline(text, chatId);
                case EDITING_TODO_TITLE -> processEditTaskTitle(text, chatId);
                case EDITING_TODO_DESCRIPTION -> processEditTaskDescription(text, chatId);
                case EDITING_TODO_DEADLINE -> processEditTaskDeadline(text, chatId);
                case EDITING_PROFILE_FIRST_NAME -> {
                    profileCommandHandler.processFirstNameUpdate(text, chatId, getCurrentDeanery(chatId));
                    stateManager.resetState(chatId);
                }
                case EDITING_PROFILE_LAST_NAME -> {
                    profileCommandHandler.processLastNameUpdate(text, chatId, getCurrentDeanery(chatId));
                    stateManager.resetState(chatId);
                }
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
                currentState == DeaneryState.VIEWING_CONSULTATION_DETAILS ||
                currentState == DeaneryState.VIEWING_TEACHER_TASKS ||
                currentState == DeaneryState.VIEWING_TASK_DETAILS ||
                currentState == DeaneryState.VIEWING_ALL_TASKS) &&
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
            case KeyboardConstants.EDIT_TASK_TITLE -> startEditTaskTitle(chatId);
            case KeyboardConstants.EDIT_TASK_DESCRIPTION -> startEditTaskDescription(chatId);
            case KeyboardConstants.EDIT_TASK_DEADLINE -> startEditTaskDeadline(chatId);
            case KeyboardConstants.DELETE_TASK -> startDeleteTask(chatId);
            case KeyboardConstants.CONFIRM_DELETE -> confirmDeleteTask(chatId);


            // Навигация
            case KeyboardConstants.MAIN_MENU -> sendMainMenu(chatId);
            case KeyboardConstants.BACK_TO_TEACHERS -> sendTeachersMenu(chatId);
            case KeyboardConstants.BACK_TO_LIST -> backToList(chatId);
            case KeyboardConstants.BACK -> handleBackButton(chatId);

            // Фильтры по времени (используются и для консультаций, и для задач)
            case KeyboardConstants.FILTER_PAST -> applyTimeFilter(chatId, "past");
            case KeyboardConstants.FILTER_ALL -> applyTimeFilter(chatId, "all");
            case KeyboardConstants.FILTER_FUTURE -> applyTimeFilter(chatId, "future");

            // Фильтры задач по статусу
            case KeyboardConstants.FILTER_TASK_INCOMPLETE -> applyTaskStatusFilter(chatId, "incomplete");
            case KeyboardConstants.FILTER_TASK_ALL -> applyTaskStatusFilter(chatId, "all");
            case KeyboardConstants.FILTER_TASK_COMPLETED -> applyTaskStatusFilter(chatId, "completed");

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

        // Если ожидаем выбора роли
        if (currentState == DeaneryState.WAITING_APPROVAL_ROLE_SELECTION) {
            Role role = switch (text) {
                case "Я студент" -> Role.STUDENT;
                case "Я преподаватель" -> Role.TEACHER;
                case "Я сотрудник деканата" -> Role.DEANERY;
                default -> null;
            };

            if (role != null) {
                authCommandHandler.handleRoleChange(chatId, role);
                stateManager.resetState(chatId);
            } else {
                sendWaitingApprovalMenu(chatId);
            }
            return;
        }

        switch (text) {
            case KeyboardConstants.BACK -> sendWaitingApprovalMenu(chatId);
            case KeyboardConstants.EDIT_ROLE -> {
                stateManager.setState(chatId, DeaneryState.WAITING_APPROVAL_ROLE_SELECTION);
                authCommandHandler.sendRoleSelectionMenu(chatId, true);
            }
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
                .text("""
                        ⏳ Ваш аккаунт ожидает подтверждения администратором.
                        
                        После подтверждения вы сможете управлять консультациями и задачами.
                        
                        Пока вы можете редактировать свой профиль:""")
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
     * Применить фильтр по времени (консультации или задачи в зависимости от контекста)
     */
    private void applyTimeFilter(Long chatId, String filter) {
        DeaneryState currentState = stateManager.getState(chatId);

        // Определяем контекст - задачи или консультации
        if (currentState == DeaneryState.VIEWING_ALL_TASKS) {
            applyTaskDeadlineFilter(chatId, filter);
        } else if (currentState == DeaneryState.VIEWING_TEACHER_TASKS) {
            applyTaskDeadlineFilter(chatId, filter);
        } else if (currentState == DeaneryState.VIEWING_TEACHER_CONSULTATIONS) {
            applyConsultationFilter(chatId, filter);
        } else {
            botMessenger.sendText("❌ Фильтр не применим в текущем контексте.", chatId);
        }
    }

    /**
     * Применить фильтр по дедлайну задач
     */
    private void applyTaskDeadlineFilter(Long chatId, String filter) {
        DeaneryState currentState = stateManager.getState(chatId);
        stateManager.setTaskDeadlineFilter(chatId, filter);

        // Обновляем отображение в зависимости от контекста
        if (currentState == DeaneryState.VIEWING_ALL_TASKS) {
            showAllTasks(chatId);
        } else if (currentState == DeaneryState.VIEWING_TEACHER_TASKS) {
            showTeacherTasks(chatId);
        }
    }

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
            } else if (currentState == DeaneryState.VIEWING_TEACHER_TASKS ||
                    currentState == DeaneryState.VIEWING_TASK_DETAILS ||
                    currentState == DeaneryState.VIEWING_ALL_TASKS) {
                showTaskDetails(chatId, id);
            } else {
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
                .replyMarkup(keyboardBuilder.buildConsultationDetails())
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
                consultation.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                consultation.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

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
     * Обработка кнопки "Назад"
     */
    private void handleBackButton(Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);

        switch (currentState) {
            case VIEWING_TEACHER_CONSULTATIONS -> sendTeachersMenu(chatId);
            case VIEWING_CONSULTATION_DETAILS, VIEWING_TEACHER_TASKS -> {
                // Вернуться к списку консультаций преподавателя
                backToConsultationsList(chatId);
            }
            case VIEWING_TASK_DETAILS -> {
                // Вернуться к списку задач - используем тот же механизм, что и для "Назад к списку"
                DeaneryState previousState = stateManager.getPreviousState(chatId);

                if (previousState == DeaneryState.VIEWING_ALL_TASKS) {
                    showAllTasks(chatId);
                } else if (previousState == DeaneryState.VIEWING_TEACHER_TASKS) {
                    showTeacherTasks(chatId);
                } else {
                    sendMainMenu(chatId);
                }

                stateManager.clearPreviousState(chatId);
            }
            case VIEWING_ALL_TASKS -> sendMainMenu(chatId);
            default -> sendMainMenu(chatId);
        }
    }

    /**
     * Обработка кнопки "Отмена"
     */
    private void handleCancelButton(Long chatId) {
        DeaneryState currentState = stateManager.getState(chatId);

        switch (currentState) {
            case CONFIRMING_DELETE_TASK -> {
                // Отмена удаления - возвращаемся к деталям задачи
                Long taskId = stateManager.getCurrentTask(chatId);
                if (taskId != null) {
                    showTaskDetails(chatId, taskId);
                } else {
                    sendMainMenu(chatId);
                }
            }
            case CREATING_TODO_TITLE, CREATING_TODO_DESCRIPTION, CREATING_TODO_DEADLINE -> {
                // Отмена создания задачи
                stateManager.clearTempData(chatId);
                botMessenger.sendText("❌ Создание задачи отменено", chatId);

                Long teacherId = stateManager.getCurrentTeacher(chatId);
                if (teacherId != null) {
                    TelegramUser teacher = teacherSearchService.findById(teacherId);
                    if (teacher != null) {
                        showTeacherConsultations(chatId, teacher);
                        return;
                    }
                }
                sendMainMenu(chatId);
            }
            case EDITING_TODO_TITLE, EDITING_TODO_DESCRIPTION, EDITING_TODO_DEADLINE -> {
                // Отмена редактирования задачи - возвращаемся к деталям
                stateManager.resetState(chatId);
                botMessenger.sendText("❌ Редактирование отменено", chatId);

                Long taskId = stateManager.getCurrentTask(chatId);
                if (taskId != null) {
                    showTaskDetails(chatId, taskId);
                } else {
                    sendMainMenu(chatId);
                }
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
        } else if (currentState == DeaneryState.VIEWING_TASK_DETAILS) {
            // Вернуться к списку задач - проверяем откуда пришли
            DeaneryState previousState = stateManager.getPreviousState(chatId);

            if (previousState == DeaneryState.VIEWING_ALL_TASKS) {
                showAllTasks(chatId);
            } else if (previousState == DeaneryState.VIEWING_TEACHER_TASKS) {
                showTeacherTasks(chatId);
            } else {
                // На всякий случай - вернуться к задачам преподавателя
                showTeacherTasks(chatId);
            }

            // Очистить сохранённое предыдущее состояние
            stateManager.clearPreviousState(chatId);
        } else {
            sendMainMenu(chatId);
        }
    }

    // ========== Управление задачами ==========

    /**
     * Показать все задачи в системе
     */
    private void showAllTasks(Long chatId) {
        // Получить фильтры из состояния
        String statusFilter = stateManager.getTaskStatusFilter(chatId);
        String deadlineFilter = stateManager.getTaskDeadlineFilter(chatId);

        // Получить все активные задачи
        List<TodoTask> allTasks = todoTaskService.getAllActiveTasks();

        // Применить фильтры
        List<TodoTask> filteredTasks = applyTaskFilters(allTasks, statusFilter, deadlineFilter);

        String messageText = todoMessageFormatter.formatAllTasksList(filteredTasks, statusFilter, deadlineFilter);

        // Очищаем текущую задачу при просмотре списка
        stateManager.clearCurrentTask(chatId);
        stateManager.setState(chatId, DeaneryState.VIEWING_ALL_TASKS);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildAllTasksList(filteredTasks))
                .build());
    }

    /**
     * Применить фильтр по статусу задач
     */
    private void applyTaskStatusFilter(Long chatId, String filter) {
        DeaneryState currentState = stateManager.getState(chatId);
        stateManager.setTaskStatusFilter(chatId, filter);

        // Обновляем отображение в зависимости от контекста
        if (currentState == DeaneryState.VIEWING_ALL_TASKS) {
            showAllTasks(chatId);
        } else if (currentState == DeaneryState.VIEWING_TEACHER_TASKS) {
            showTeacherTasks(chatId);
        }
    }

    /**
     * Применить фильтры к списку задач
     */
    private List<TodoTask> applyTaskFilters(List<TodoTask> tasks, String statusFilter, String deadlineFilter) {
        LocalDateTime now = LocalDateTime.now();

        return tasks.stream()
                .filter(task -> {
                    // Фильтр по статусу
                    boolean statusMatch = switch (statusFilter) {
                        case "incomplete" -> !task.getIsCompleted();
                        case "completed" -> task.getIsCompleted();
                        default -> true; // "all"
                    };

                    // Фильтр по дедлайну
                    boolean deadlineMatch = switch (deadlineFilter) {
                        case "past" -> task.getDeadline().isBefore(now);
                        case "future" -> task.getDeadline().isAfter(now);
                        default -> true; // "all"
                    };

                    return statusMatch && deadlineMatch;
                })
                .toList();
    }

    private void startTaskCreation(Long chatId) {
        stateManager.setState(chatId, DeaneryState.CREATING_TODO_TITLE);
        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("""
                        ➕ Создание новой задачи
                        
                        Шаг 1/3: Введите название задачи""")
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
                """
                        ✅ Описание сохранено.
                        
                        Шаг 3/3: Введите дедлайн
                        Формат: ДД.ММ.ГГГГ ЧЧ:ММ
                        Например: 15.12.2025 18:00""",
                chatId
        );
    }

    private void processTaskDeadline(String deadlineText, Long chatId) {
        if (deadlineText == null || deadlineText.trim().isEmpty()) {
            botMessenger.sendText("Дедлайн не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        try {
            // Парсинг даты и времени в формате ДД.ММ.ГГГГ ЧЧ:ММ
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            java.time.LocalDateTime deadline = java.time.LocalDateTime.parse(deadlineText.trim(), formatter);

            // Проверка что дата не в прошлом
            if (deadline.isBefore(java.time.LocalDateTime.now())) {
                botMessenger.sendText("❌ Дедлайн не может быть в прошлом. Введите другую дату:", chatId);
                return;
            }

            // Получаем данные для создания задачи
            Long teacherId = stateManager.getCurrentTeacher(chatId);
            String title = stateManager.getTempTitle(chatId);
            String description = stateManager.getTempDescription(chatId);

            if (teacherId == null || title == null || description == null) {
                botMessenger.sendText("❌ Ошибка: данные задачи потеряны. Начните заново.", chatId);
                stateManager.resetState(chatId);
                stateManager.clearTempData(chatId);
                sendMainMenu(chatId);
                return;
            }

            TelegramUser teacher = teacherSearchService.findById(teacherId);
            if (teacher == null) {
                botMessenger.sendText("❌ Преподаватель не найден.", chatId);
                stateManager.resetState(chatId);
                stateManager.clearTempData(chatId);
                sendMainMenu(chatId);
                return;
            }

            TelegramUser createdBy = getCurrentDeanery(chatId);

            // Создаём задачу
            TodoTask createdTask = todoTaskService.createTodoForTeacher(
                    teacher, createdBy, title, description, deadline
            );

            botMessenger.sendText(
                    String.format("""
                                    ✅ Задача успешно создана!
                                    
                                    📋 Задача №%d
                                    👨‍🏫 Преподаватель: %s %s
                                    📌 Название: %s
                                    ⏰ Дедлайн: %s""",
                            createdTask.getId(),
                            teacher.getFirstName(),
                            teacher.getLastName() != null ? teacher.getLastName() : "",
                            title,
                            deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))),
                    chatId
            );

            stateManager.resetState(chatId);
            stateManager.clearTempData(chatId);

            // Показываем обновлённый список задач преподавателя
            showTeacherTasks(chatId);

        } catch (java.time.format.DateTimeParseException e) {
            botMessenger.sendText(
                    """
                            ❌ Неверный формат даты и времени.
                            
                            Используйте формат: ДД.ММ.ГГГГ ЧЧ:ММ
                            Например: 15.12.2025 18:00
                            
                            Попробуйте ещё раз:""",
                    chatId
            );
        }
    }

    /**
     * Показать задачи выбранного преподавателя
     */
    private void showTeacherTasks(Long chatId) {
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

        // Получить фильтры из состояния
        String statusFilter = stateManager.getTaskStatusFilter(chatId);
        String deadlineFilter = stateManager.getTaskDeadlineFilter(chatId);

        // Получить все задачи преподавателя
        List<TodoTask> allTasks = todoTaskService.getTeacherTasks(teacher);

        // Применить фильтры
        List<TodoTask> filteredTasks = applyTaskFilters(allTasks, statusFilter, deadlineFilter);

        String messageText = todoMessageFormatter.formatTeacherTasksList(teacher, filteredTasks, statusFilter, deadlineFilter);

        // Очищаем текущую задачу при просмотре списка
        stateManager.clearCurrentTask(chatId);
        stateManager.setState(chatId, DeaneryState.VIEWING_TEACHER_TASKS);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTeacherTasksList(filteredTasks))
                .build());
    }

    /**
     * Показать детали задачи
     */
    private void showTaskDetails(Long chatId, Long taskId) {
        TodoTask task = todoTaskService.getTodoById(taskId).orElse(null);
        if (task == null) {
            botMessenger.sendText("❌ Задача не найдена", chatId);
            return;
        }

        // Сохраняем предыдущее состояние ТОЛЬКО если мы переходим из состояния списка
        // (чтобы не затереть при обновлении/отмене/редактировании)
        DeaneryState currentState = stateManager.getState(chatId);
        if (currentState == DeaneryState.VIEWING_ALL_TASKS ||
                currentState == DeaneryState.VIEWING_TEACHER_TASKS) {
            stateManager.savePreviousState(chatId);
        }

        // Сохраняем ID текущей задачи
        stateManager.setCurrentTask(chatId, taskId);
        stateManager.setState(chatId, DeaneryState.VIEWING_TASK_DETAILS);

        String messageText = todoMessageFormatter.formatTaskDetails(task);

        botMessenger.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildTaskDetails(task))
                .build());
    }

    private void markTaskCompleted(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана.", chatId);
            return;
        }

        todoTaskService.markAsCompleted(taskId);
        botMessenger.sendText("✅ Задача отмечена как выполненная!", chatId);

        // Обновляем детали задачи
        showTaskDetails(chatId, taskId);
    }

    private void markTaskPending(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана.", chatId);
            return;
        }

        todoTaskService.markAsIncomplete(taskId);
        botMessenger.sendText("⏳ Задача отмечена как невыполненная.", chatId);

        // Обновляем детали задачи
        showTaskDetails(chatId, taskId);
    }

    private void startEditTask(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            return;
        }

        TodoTask task = todoTaskService.getTodoById(taskId).orElse(null);
        if (task == null) {
            botMessenger.sendText("❌ Задача не найдена", chatId);
            return;
        }

        String message = String.format(
                """
                        ✏️ Редактирование задачи №%d
                        
                        Выберите, что хотите изменить:""",
                taskId
        );

        botMessenger.execute(SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildEditTaskMenu())
                .build());
    }

    /**
     * Начать редактирование названия задачи
     */
    private void startEditTaskTitle(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            return;
        }

        TodoTask task = todoTaskService.getTodoById(taskId).orElse(null);
        if (task == null) {
            botMessenger.sendText("❌ Задача не найдена", chatId);
            return;
        }

        stateManager.setState(chatId, DeaneryState.EDITING_TODO_TITLE);
        botMessenger.execute(SendMessage.builder()
                .text(String.format(
                        """
                                ✏️ Редактирование названия
                                
                                Текущее название:
                                %s
                                
                                Введите новое название:""",
                        task.getTitle()))
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    /**
     * Обработать новое название задачи
     */
    private void processEditTaskTitle(String newTitle, Long chatId) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            botMessenger.sendText("❌ Название не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            stateManager.resetState(chatId);
            sendMainMenu(chatId);
            return;
        }

        try {
            todoTaskService.updateTitle(taskId, newTitle.trim());
            botMessenger.sendText("✅ Название задачи обновлено!", chatId);
            stateManager.resetState(chatId);
            showTaskDetails(chatId, taskId);
        } catch (Exception e) {
            log.error("Ошибка при обновлении названия задачи: {}", e.getMessage());
            botMessenger.sendText("❌ Ошибка при обновлении названия. Попробуйте позже.", chatId);
            showTaskDetails(chatId, taskId);
        }
    }

    /**
     * Начать редактирование описания задачи
     */
    private void startEditTaskDescription(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            return;
        }

        TodoTask task = todoTaskService.getTodoById(taskId).orElse(null);
        if (task == null) {
            botMessenger.sendText("❌ Задача не найдена", chatId);
            return;
        }

        stateManager.setState(chatId, DeaneryState.EDITING_TODO_DESCRIPTION);
        botMessenger.execute(SendMessage.builder()
                .text(String.format(
                        """
                                ✏️ Редактирование описания
                                
                                Текущее описание:
                                %s
                                
                                Введите новое описание:""",
                        task.getDescription()))
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    /**
     * Обработать новое описание задачи
     */
    private void processEditTaskDescription(String newDescription, Long chatId) {
        if (newDescription == null || newDescription.trim().isEmpty()) {
            botMessenger.sendText("❌ Описание не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            stateManager.resetState(chatId);
            sendMainMenu(chatId);
            return;
        }

        try {
            todoTaskService.updateDescription(taskId, newDescription.trim());
            botMessenger.sendText("✅ Описание задачи обновлено!", chatId);
            stateManager.resetState(chatId);
            showTaskDetails(chatId, taskId);
        } catch (Exception e) {
            log.error("Ошибка при обновлении описания задачи: {}", e.getMessage());
            botMessenger.sendText("❌ Ошибка при обновлении описания. Попробуйте позже.", chatId);
            showTaskDetails(chatId, taskId);
        }
    }

    /**
     * Начать редактирование дедлайна задачи
     */
    private void startEditTaskDeadline(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            return;
        }

        TodoTask task = todoTaskService.getTodoById(taskId).orElse(null);
        if (task == null) {
            botMessenger.sendText("❌ Задача не найдена", chatId);
            return;
        }

        stateManager.setState(chatId, DeaneryState.EDITING_TODO_DEADLINE);
        botMessenger.execute(SendMessage.builder()
                .text(String.format(
                        """
                                ✏️ Редактирование дедлайна
                                
                                Текущий дедлайн:
                                %s
                                
                                Введите новый дедлайн
                                Формат: ДД.ММ.ГГГГ ЧЧ:ММ
                                Например: 15.12.2025 18:00""",
                        task.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))))
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildCancelKeyboard())
                .build());
    }

    /**
     * Обработать новый дедлайн задачи
     */
    private void processEditTaskDeadline(String deadlineText, Long chatId) {
        if (deadlineText == null || deadlineText.trim().isEmpty()) {
            botMessenger.sendText("❌ Дедлайн не может быть пустым. Попробуйте ещё раз:", chatId);
            return;
        }

        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            stateManager.resetState(chatId);
            sendMainMenu(chatId);
            return;
        }

        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            java.time.LocalDateTime newDeadline = java.time.LocalDateTime.parse(deadlineText.trim(), formatter);

            // Проверка что дата не в прошлом
            if (newDeadline.isBefore(java.time.LocalDateTime.now())) {
                botMessenger.sendText("❌ Дедлайн не может быть в прошлом. Введите другую дату:", chatId);
                return;
            }

            todoTaskService.updateDeadline(taskId, newDeadline);
            botMessenger.sendText("✅ Дедлайн задачи обновлён!", chatId);
            stateManager.resetState(chatId);
            showTaskDetails(chatId, taskId);
        } catch (java.time.format.DateTimeParseException e) {
            botMessenger.sendText(
                    """
                            ❌ Неверный формат даты и времени.
                            
                            Используйте формат: ДД.ММ.ГГГГ ЧЧ:ММ
                            Например: 15.12.2025 18:00
                            
                            Попробуйте ещё раз:""",
                    chatId
            );
        } catch (Exception e) {
            log.error("Ошибка при обновлении дедлайна задачи: {}", e.getMessage());
            botMessenger.sendText("❌ Ошибка при обновлении дедлайна. Попробуйте позже.", chatId);
            showTaskDetails(chatId, taskId);
        }
    }


    /**
     * Начать процесс удаления задачи
     */
    private void startDeleteTask(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            return;
        }

        TodoTask task = todoTaskService.getTodoById(taskId).orElse(null);
        if (task == null) {
            botMessenger.sendText("❌ Задача не найдена", chatId);
            return;
        }

        // Переходим в состояние подтверждения удаления
        stateManager.setState(chatId, DeaneryState.CONFIRMING_DELETE_TASK);

        String message = String.format(
                """
                        ⚠️ Подтверждение удаления
                        
                        Вы уверены, что хотите удалить задачу?
                        
                        📝 %s
                        👨‍🏫 %s %s
                        
                        ❗ Это действие нельзя отменить!""",
                task.getTitle(),
                task.getTeacher().getFirstName(),
                task.getTeacher().getLastName() != null ? task.getTeacher().getLastName() : ""
        );

        botMessenger.execute(SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(keyboardBuilder.buildConfirmDeleteKeyboard())
                .build());
    }

    /**
     * Подтвердить удаление задачи
     */
    private void confirmDeleteTask(Long chatId) {
        Long taskId = stateManager.getCurrentTask(chatId);
        if (taskId == null) {
            botMessenger.sendText("❌ Задача не выбрана", chatId);
            stateManager.setState(chatId, DeaneryState.DEFAULT);
            sendMainMenu(chatId);
            return;
        }

        try {
            todoTaskService.deleteTodo(taskId);
            botMessenger.sendText("✅ Задача успешно удалена", chatId);

            // Очищаем текущую задачу и возвращаемся к списку задач
            stateManager.clearCurrentTask(chatId);

            // Проверяем предыдущее состояние, чтобы вернуться к правильному списку
            DeaneryState previousState = stateManager.getPreviousState(chatId);

            if (previousState == DeaneryState.VIEWING_ALL_TASKS) {
                showAllTasks(chatId);
            } else if (previousState == DeaneryState.VIEWING_TEACHER_TASKS) {
                showTeacherTasks(chatId);
            } else {
                // По умолчанию возвращаемся к главному меню
                sendMainMenu(chatId);
            }

            stateManager.clearPreviousState(chatId);

        } catch (Exception e) {
            log.error("Ошибка при удалении задачи: {}", e.getMessage());
            botMessenger.sendText("❌ Ошибка при удалении задачи. Попробуйте позже.", chatId);
            showTaskDetails(chatId, taskId);
        }
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
