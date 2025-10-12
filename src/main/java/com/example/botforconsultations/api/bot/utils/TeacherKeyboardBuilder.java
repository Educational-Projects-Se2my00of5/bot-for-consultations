package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для построения клавиатур преподавателя
 */
@Component
public class TeacherKeyboardBuilder {

    private static final DateTimeFormatter BUTTON_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter BUTTON_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Главное меню преподавателя
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📅 Мои консультации"));
        row1.add(new KeyboardButton("➕ Создать консультацию"));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📋 Просмотреть запросы"));
        keyboard.add(row2);

        KeyboardRow helpRow = new KeyboardRow();
        helpRow.add(new KeyboardButton("Помощь"));
        keyboard.add(helpRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура со списком консультаций преподавателя
     */
    public ReplyKeyboardMarkup buildConsultationsList(List<Consultation> consultations) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем консультации как кнопки (максимум 5)
        int count = 0;
        for (Consultation consultation : consultations) {
            if (count >= 5) break;
            
            KeyboardRow row = new KeyboardRow();
            // Консультации всегда имеют дату и время
            String buttonText = String.format("№%d - %s %s", 
                    consultation.getId(),
                    consultation.getDate().format(BUTTON_DATE_FORMATTER),
                    consultation.getStartTime().format(BUTTON_TIME_FORMATTER));
            
            row.add(new KeyboardButton(buttonText));
            keyboard.add(row);
            count++;
        }

        // Кнопка "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для детального просмотра консультации
     * @param consultation консультация
     * @param registeredCount количество записанных студентов
     */
    public ReplyKeyboardMarkup buildConsultationDetails(Consultation consultation, long registeredCount) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        ConsultationStatus status = consultation.getStatus();

        // Кнопки управления в зависимости от статуса
        if (status == ConsultationStatus.OPEN) {
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton("🔒 Закрыть запись"));
            keyboard.add(row1);
            
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton("✏️ Редактировать"));
            row2.add(new KeyboardButton("❌ Отменить консультацию"));
            keyboard.add(row2);
            
        } else if (status == ConsultationStatus.CLOSED) {
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton("🔓 Открыть запись"));
            keyboard.add(row1);
            
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton("✏️ Редактировать"));
            row2.add(new KeyboardButton("❌ Отменить консультацию"));
            keyboard.add(row2);
        }
        // Для CANCELLED статуса не добавляем кнопки управления

        // Кнопка "Просмотреть студентов" (если есть записанные)
        if (registeredCount > 0) {
            KeyboardRow studentsRow = new KeyboardRow();
            studentsRow.add(new KeyboardButton("👥 Просмотреть студентов"));
            keyboard.add(studentsRow);
        }

        // Кнопка "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад к списку"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для просмотра консультации другого преподавателя (только чтение)
     */
    public ReplyKeyboardMarkup buildConsultationDetailsReadOnly(long registeredCount) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Только кнопка "Просмотреть студентов" (если есть записанные)
        if (registeredCount > 0) {
            KeyboardRow studentsRow = new KeyboardRow();
            studentsRow.add(new KeyboardButton("👥 Просмотреть студентов"));
            keyboard.add(studentsRow);
        }

        // Кнопка "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад к списку"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для просмотра списка запросов студентов
     */
    public ReplyKeyboardMarkup buildRequestsList(List<Consultation> requests) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем запросы как кнопки (максимум 5)
        int count = 0;
        for (Consultation request : requests) {
            if (count >= 5) break;
            
            KeyboardRow row = new KeyboardRow();
            String buttonText = String.format("№%d - %s", 
                    request.getId(), 
                    request.getTitle().length() > 30 
                        ? request.getTitle().substring(0, 30) + "..." 
                        : request.getTitle());
            row.add(new KeyboardButton(buttonText));
            keyboard.add(row);
            count++;
        }

        // Кнопка "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для детального просмотра запроса студента
     * @param interestedCount количество заинтересованных студентов
     */
    public ReplyKeyboardMarkup buildRequestDetails(int interestedCount) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Кнопка "Принять запрос" (создать консультацию)
        KeyboardRow acceptRow = new KeyboardRow();
        acceptRow.add(new KeyboardButton("✅ Принять запрос"));
        keyboard.add(acceptRow);

        // Если есть заинтересованные студенты, показываем кнопку просмотра
        if (interestedCount > 0) {
            KeyboardRow studentsRow = new KeyboardRow();
            studentsRow.add(new KeyboardButton("👥 Просмотреть студентов"));
            keyboard.add(studentsRow);
        }

        // Кнопка "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад к списку"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для подтверждения автозакрытия (Да/Нет)
     */
    public ReplyKeyboardMarkup buildYesNoKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Да"));
        row.add(new KeyboardButton("Нет"));
        keyboard.add(row);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для выбора параметра редактирования
     */
    public ReplyKeyboardMarkup buildEditMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📋 Название"));
        row1.add(new KeyboardButton("📅 Дата и время"));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("👥 Вместимость"));
        row2.add(new KeyboardButton("🔒 Автозакрытие"));
        keyboard.add(row2);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Простая клавиатура с кнопкой "Назад" для просмотра списка студентов
     */
    public ReplyKeyboardMarkup buildBackKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура с кнопкой "Отмена" для прерывания процесса создания/редактирования
     */
    public ReplyKeyboardMarkup buildCancelKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow cancelRow = new KeyboardRow();
        cancelRow.add(new KeyboardButton("❌ Отмена"));
        keyboard.add(cancelRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }
}
