package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.TodoTask;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeaneryKeyboardBuilder {

    private static final DateTimeFormatter BUTTON_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter BUTTON_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Главное меню деканата
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🔍 Найти преподавателя"));
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🏠 Главное меню"));
        
        keyboard.add(row1);
        keyboard.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Список найденных преподавателей
     * (аналогично студенту - первые 5 в кнопках)
     */
    public ReplyKeyboardMarkup buildTeacherListKeyboard(List<TelegramUser> teachers) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем первых 5 преподавателей как кнопки
        int count = 0;
        for (TelegramUser teacher : teachers) {
            if (count >= 5) break;

            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(TeacherNameFormatter.formatFullName(teacher)));
            keyboard.add(row);
            count++;
        }

        // Кнопка поиска
        KeyboardRow searchRow = new KeyboardRow();
        searchRow.add(new KeyboardButton("🔍 Поиск преподавателя"));
        keyboard.add(searchRow);

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
     * Клавиатура со списком консультаций преподавателя
     * (аналогично студенту, но с кнопкой создания задачи вместо подписки)
     */
    public ReplyKeyboardMarkup buildTeacherConsultations(List<Consultation> consultations) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем консультации как кнопки (максимум 5 последних)
        int count = 0;
        for (Consultation consultation : consultations) {
            if (count >= 5) break;
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(String.format("№%d - %s %s",
                    consultation.getId(),
                    consultation.getDate().format(BUTTON_DATE_FORMATTER),
                    consultation.getStartTime().format(BUTTON_TIME_FORMATTER))));
            keyboard.add(row);
            count++;
        }

        // Фильтры
        KeyboardRow filterRow = new KeyboardRow();
        filterRow.add(new KeyboardButton("⏭️ Будущие"));
        filterRow.add(new KeyboardButton("📅 Все"));
        filterRow.add(new KeyboardButton("⏮️ Прошедшие"));
        keyboard.add(filterRow);

        // Действия деканата
        KeyboardRow actionRow1 = new KeyboardRow();
        actionRow1.add(new KeyboardButton("📝 Создать задачу"));
        keyboard.add(actionRow1);
        
        KeyboardRow actionRow2 = new KeyboardRow();
        actionRow2.add(new KeyboardButton("📋 Задачи преподавателя"));
        keyboard.add(actionRow2);

        // Навигация
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(new KeyboardButton("🔙 К поиску"));
        navRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(navRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для детального просмотра консультации
     * (только просмотр студентов, без записи)
     */
    public ReplyKeyboardMarkup buildConsultationDetails() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Просмотр записанных студентов
        KeyboardRow actionRow = new KeyboardRow();
        actionRow.add(new KeyboardButton("👥 Список студентов"));
        keyboard.add(actionRow);

        // Навигация
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(new KeyboardButton("◀️ Назад к списку"));
        keyboard.add(navRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Список задач преподавателя
     */
    public InlineKeyboardMarkup buildTodoListKeyboard(List<TodoTask> todos, Long teacherId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (TodoTask todo : todos) {
            String prefix = todo.getIsCompleted() ? "✅ " : "⏳ ";
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(prefix + todo.getTitle())
                    .callbackData("deanery_todo_" + todo.getId())
                    .build();
            rows.add(new InlineKeyboardRow(button));
        }

        // Кнопка назад
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("◀️ Назад")
                        .callbackData("deanery_teacher_" + teacherId)
                        .build()
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    /**
     * Действия с задачей
     */
    public InlineKeyboardMarkup buildTodoActionsKeyboard(TodoTask todo, Long teacherId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        // Отметить выполнено/не выполнено
        if (!todo.getIsCompleted()) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("✅ Отметить выполненной")
                            .callbackData("deanery_complete_todo_" + todo.getId())
                            .build()
            ));
        } else {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("🔄 Вернуть в активные")
                            .callbackData("deanery_incomplete_todo_" + todo.getId())
                            .build()
            ));
        }

        // Удалить
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("🗑️ Удалить задачу")
                        .callbackData("deanery_delete_todo_" + todo.getId())
                        .build()
        ));

        // Назад к списку задач
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("◀️ Назад к задачам")
                        .callbackData("deanery_view_todos_" + teacherId)
                        .build()
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    /**
     * Подтверждение удаления задачи
     */
    public InlineKeyboardMarkup buildDeleteConfirmationKeyboard(Long todoId, Long teacherId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        // Да, удалить
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("✅ Да, удалить")
                        .callbackData("deanery_confirm_delete_" + todoId)
                        .build()
        ));

        // Отмена
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("❌ Отмена")
                        .callbackData("deanery_todo_" + todoId)
                        .build()
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    /**
     * Кнопка отмены при создании задачи
     */
    public InlineKeyboardMarkup buildCancelTodoCreationKeyboard() {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("❌ Отменить создание")
                        .callbackData("deanery_cancel_todo")
                        .build()
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}
