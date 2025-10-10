package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCommandHandler {

    private final TelegramUserRepository telegramUserRepository;
    private final ConsultationRepository consultationRepository;
    private final BotMessenger botMessenger;

    private final Map<Long, UserState> userStates = new HashMap<>();

    private enum UserState {
        WAITING_FOR_TEACHER_NAME,
        DEFAULT
    }

    public void handleStudentCommand(String text, Long chatId) {
        // Проверяем, находится ли пользователь в состоянии ожидания ввода
        UserState currentState = userStates.getOrDefault(chatId, UserState.DEFAULT);
        if (currentState == UserState.WAITING_FOR_TEACHER_NAME) {
            processTeacherSearch(text, chatId);
            userStates.put(chatId, UserState.DEFAULT);
            return;
        }

        // Проверяем, является ли текст выбором преподавателя
        if (text.startsWith("👨‍🏫")) {
            showTeacherConsultations(text, chatId);
            return;
        }

        switch (text) {
            case "Помощь" -> sendHelp(chatId);

            // Основные команды студента
            case "🔍 Преподаватели" -> handleTeachers(chatId);
//            case "🔔 Подписки на обновления" -> handleSubscriptions(chatId);
//            case "📝 Мои записи" -> handleMyRegistrations(chatId);
//            case "❓ Запросить консультацию" -> handleRequestConsultation(chatId);
//            case "📋 Просмотреть запросы" -> handleViewRequests(chatId);

            // Команды меню '🔍 Преподаватели'
            case "👥 Все преподаватели" -> handleAllTeachers(chatId);
            case "🔍 Поиск преподавателя" -> handleTeacherSearch(chatId);


            case "◀️ Назад" -> sendMainMenu(chatId);

//            case "🔔 Подписаться" -> handleSubscribeToTeacher(chatId);

            default -> botMessenger.sendText(
                    "Извините, я не понимаю эту команду. Отправьте 'Помощь' для получения списка доступных команд.",
                    chatId
            );
        }
    }

    public void sendHelp(Long chatId) {
        StringBuilder helpText = new StringBuilder();
        helpText.append("Доступные команды для студента:\n\n")
                .append("🔍 Преподаватели - просмотр времени консультаций у преподавателей\n")
                .append("🔔 Подписки на обновления - просмотр ваших подписок на уведомление при изменении в расписании преподавателя\n")
                .append("📝 Мои записи - просмотр ваших записей на консультации и запросы на консультации\n")
                .append("❓ Запросить консультацию - создание запроса на консультацию\n")
                .append("📋 Просмотреть запросы - просмотр запросов на консультации\n\n")
                .append("В разделе \"🔍 Преподаватели\" можно:\n")
                .append("- 👥 Все преподаватели - получение всех преподавателей")
                .append("- 🔍 Поиск преподавателя - поиск по фамилии или имени.")
                .append("- После '🔍 Поиск преподавателя' можно выбрать конкретного и получить его консультации")
                .append("В разделе \"📋 Просмотреть запросы\" можно подписаться под запросом на консультацию(так же как и при записи на консультацию).\n");

        botMessenger.sendText(helpText.toString(), chatId);
    }

    public void sendMainMenu(Long chatId) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow helpRow = new KeyboardRow();

        helpRow.add(new KeyboardButton("Помощь"));
        row1.add(new KeyboardButton("🔍 Преподаватели"));
        row2.add(new KeyboardButton("🔔 Подписки на обновления"));
        row2.add(new KeyboardButton("📝 Мои записи"));
        row3.add(new KeyboardButton("❓ Запросить консультацию"));
        row3.add(new KeyboardButton("📋 Просмотреть запросы"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(helpRow);

        botMessenger.execute(
                SendMessage.builder()
                        .text("Добро пожаловать, студент! Выберите действие:")
                        .chatId(chatId)
                        .replyMarkup(
                                ReplyKeyboardMarkup.builder()
                                        .keyboard(keyboard)
                                        .resizeKeyboard(true)
                                        .build()
                        )
                        .build()
        );
    }


    // Меню для работы с преподавателями
    private void handleTeachers(Long chatId) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("👥 Все преподаватели"));
        row1.add(new KeyboardButton("🔍 Поиск преподавателя"));
        keyboard.add(row1);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();

        SendMessage message = SendMessage.builder()
                .text("Выберите действие для работы с преподавателями:")
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        botMessenger.execute(message);
    }

    private void handleTeacherSearch(Long chatId) {
        userStates.put(chatId, UserState.WAITING_FOR_TEACHER_NAME);
        botMessenger.sendText(
                "Введите часть имени или фамилии преподавателя (или полностью имя и фамилию) для поиска:",
                chatId
        );
    }

    private void handleAllTeachers(Long chatId) {
        List<TelegramUser> teachers = telegramUserRepository.findByRoleAndHasConfirmed(Role.TEACHER, true);

        if (teachers.isEmpty()) {
            botMessenger.sendText("В данный момент нет доступных преподавателей", chatId);
            return;
        }

        StringBuilder message = new StringBuilder("Список преподавателей:\n\n");
        for (TelegramUser teacher : teachers) {
            message.append(String.format("👨‍🏫 %s %s\n",
                    teacher.getFirstName(),
                    teacher.getLastName() != null ? teacher.getLastName() : ""));
        }

        userStates.put(chatId, UserState.WAITING_FOR_TEACHER_NAME);
        message.append("\nВведите часть имени или фамилии преподавателя для поиска:");

        botMessenger.sendText(message.toString(), chatId);
    }

    private void processTeacherSearch(String searchQuery, Long chatId) {
        // Поиск преподавателей по имени или фамилии (игнорируя регистр)
        List<TelegramUser> foundTeachers = telegramUserRepository
                .findByRoleAndHasConfirmedTrueAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        Role.TEACHER, searchQuery, searchQuery);

        if (foundTeachers.isEmpty()) {
            botMessenger.sendText(
                    "Преподаватели не найдены. Попробуйте другой запрос или выберите из общего списка.",
                    chatId
            );
            return;
        }

        StringBuilder message = new StringBuilder("Найденные преподаватели:\n\n");
        for (TelegramUser teacher : foundTeachers) {
            message.append(String.format("👨‍🏫 %s %s\n",
                    teacher.getFirstName(),
                    teacher.getLastName() != null ? teacher.getLastName() : ""));
        }
        message.append("\nВыберите преподавателя, чтобы увидеть его консультации.");

        // Создаем клавиатуру с найденными преподавателями
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (TelegramUser teacher : foundTeachers) {
            KeyboardRow row = new KeyboardRow();
            String teacherName = String.format("👨‍🏫 %s %s",
                    teacher.getFirstName(),
                    teacher.getLastName() != null ? teacher.getLastName() : "");
            row.add(new KeyboardButton(teacherName));
            keyboard.add(row);
        }


        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();

        SendMessage searchResults = SendMessage.builder()
                .text(message.toString())
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        botMessenger.execute(searchResults);
    }



    private void showTeacherConsultations(String teacherButton, Long chatId) {
        // Извлекаем имя преподавателя
        String teacherName = teacherButton.substring(teacherButton.indexOf(" ") + 1);
        String[] nameParts = teacherName.split(" ");

        // Ищем преподавателя в базе
        TelegramUser teacher;
        if (nameParts.length > 1) {
            teacher = telegramUserRepository.findByFirstNameAndLastNameAndRole(
                    nameParts[0], nameParts[1], Role.TEACHER).orElse(null);
        } else {
            teacher = telegramUserRepository.findByFirstNameAndRole(
                    nameParts[0], Role.TEACHER).orElse(null);
        }

        if (teacher == null) {
            botMessenger.sendText("Преподаватель не найден", chatId);
            return;
        }

        // Получаем список консультаций преподавателя
        List<Consultation> consultations = consultationRepository.findByTeacherOrderByStartTimeAsc(teacher);

        StringBuilder message = new StringBuilder();
        message.append(String.format("Консультации преподавателя %s:\n\n", teacherName));

        if (consultations.isEmpty()) {
            message.append("В данный момент нет запланированных консультаций.\n");
        } else {
            for (Consultation consultation : consultations) {
                message.append(String.format("№%d\n", consultation.getId()));
                message.append(String.format("📅 %s - %s\n",
                        consultation.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        consultation.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

                if (consultation.getTitle() != null && !consultation.getTitle().isEmpty()) {
                    message.append(String.format("📝 %s\n", consultation.getTitle()));
                }
                message.append("\n");
            }
        }

        // Клавиатура действий
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow actionRow = new KeyboardRow();
        actionRow.add(new KeyboardButton("🔔 Подписаться"));
        actionRow.add(new KeyboardButton("❓ Создать запрос"));
        keyboard.add(actionRow);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();

        SendMessage consultationsMessage = SendMessage.builder()
                .text(message.toString())
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        botMessenger.execute(consultationsMessage);
    }
}


