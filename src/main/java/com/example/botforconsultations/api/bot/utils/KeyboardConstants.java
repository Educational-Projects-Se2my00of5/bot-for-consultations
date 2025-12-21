package com.example.botforconsultations.api.bot.utils;

/**
 * Константы для текстов кнопок клавиатуры.
 * Централизованное хранение всех текстов кнопок для единообразия.
 */
public final class KeyboardConstants {

    private KeyboardConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========== Навигация ==========
    public static final String BACK = "◀️ Назад";
    public static final String BACK_TO_LIST = "◀️ Назад к списку";
    public static final String BACK_TO_TEACHERS = "🔙 К преподавателям";
    public static final String MAIN_MENU = "🏠 Главное меню";

    // ========== Фильтры консультаций ==========
    public static final String FILTER_FUTURE = "⏭️ Будущие";
    public static final String FILTER_ALL = "📅 Все";
    public static final String FILTER_PAST = "⏮️ Прошедшие";

    // ========== Поиск и преподаватели ==========
    public static final String SEARCH_TEACHER = "🔍 Поиск преподавателя";
    public static final String ALL_TEACHERS = "👥 Все преподаватели";
    public static final String TEACHERS_MENU = "🔍 Преподаватели";

    // ========== Действия студента ==========
    public static final String SUBSCRIBE = "🔔 Подписаться";
    public static final String UNSUBSCRIBE = "🔕 Отписаться";
    public static final String SUBSCRIPTIONS = "🔔 Подписки на обновления";
    public static final String MY_REGISTRATIONS = "📝 Мои записи";
    public static final String REGISTER = "✅ Записаться";
    public static final String CANCEL_REGISTRATION = "❌ Отменить запись";

    // ========== Запросы консультаций ==========
    public static final String REQUEST_CONSULTATION = "❓ Запросить консультацию";
    public static final String VIEW_REQUESTS = "📋 Просмотреть запросы";
    public static final String REGISTER_FOR_REQUEST = "✅ Записаться на запрос";
    public static final String UNREGISTER_FROM_REQUEST = "❌ Отписаться от запроса";

    // ========== Действия преподавателя ==========
    public static final String CREATE_CONSULTATION = "➕ Создать консультацию";
    public static final String MY_CONSULTATIONS = "📅 Мои консультации";
    public static final String MY_TASKS = "📋 Мои задачи";
    public static final String ACCEPT_REQUEST = "✅ Принять запрос";
    public static final String VIEW_STUDENTS = "👥 Просмотреть студентов";
    public static final String CLOSE_REGISTRATION = "🔒 Закрыть запись";
    public static final String OPEN_REGISTRATION = "🔓 Открыть запись";
    public static final String EDIT_CONSULTATION = "✏️ Редактировать";
    public static final String CANCEL_CONSULTATION = "❌ Отменить консультацию";
    public static final String EDIT_TITLE = "📋 Название";
    public static final String EDIT_DATE_TIME = "📅 Дата и время";
    public static final String EDIT_CAPACITY = "👥 Вместимость";
    public static final String EDIT_AUTO_CLOSE = "🔒 Автозакрытие";
    public static final String YES = "Да";
    public static final String NO = "Нет";

    // ========== Действия деканата ==========
    public static final String CREATE_TASK = "📝 Создать задачу";
    public static final String TEACHER_TASKS = "📋 Задачи преподавателя";
    public static final String ALL_TASKS = "📋 Все задачи";
    public static final String STUDENT_LIST = "👥 Список студентов";
    public static final String EDIT_TASK = "✏️ Редактировать";
    public static final String DELETE_TASK = "🗑️ Удалить";
    public static final String MARK_COMPLETED = "✅ Отметить выполненной";
    public static final String MARK_PENDING = "⏳ Отметить невыполненной";
    public static final String EDIT_TASK_TITLE = "📋 Изменить название";
    public static final String EDIT_TASK_DESCRIPTION = "📝 Изменить описание";
    public static final String EDIT_TASK_DEADLINE = "⏰ Изменить дедлайн";

    // ========== Действия преподавателя с задачами ==========
    public static final String MARK_TASK_COMPLETED = "✅ Отметить выполненной";
    public static final String MARK_TASK_PENDING = "⏳ Отметить невыполненной";

    // ========== Фильтры задач ==========
    public static final String FILTER_TASK_INCOMPLETE = "❌ Невыполненные";
    public static final String FILTER_TASK_ALL = "📋 Все";
    public static final String FILTER_TASK_COMPLETED = "✅ Выполненные";

    // ========== Профиль ==========
    public static final String PROFILE = "👤 Профиль";
    public static final String HELP = "Помощь";
    public static final String EDIT_FIRST_NAME = "✏️ Изменить имя";
    public static final String EDIT_LAST_NAME = "✏️ Изменить фамилию";
    public static final String EDIT_REMINDER_TIME = "⏰ Время напоминаний";
    public static final String CONNECT_GOOGLE_CALENDAR = "🔗 Подключить Google Calendar";
    public static final String DISCONNECT_GOOGLE_CALENDAR = "🔓 Отключить Google Calendar";

    // ========== Общие действия ==========
    public static final String CONFIRM_DELETE = "✅ Да, удалить";
    public static final String CANCEL = "❌ Отмена";

    // ========== Префиксы ==========
    public static final String TEACHER_PREFIX = "👨‍🏫 ";
    public static final String COMPLETED_PREFIX = "✅ ";
    public static final String PENDING_PREFIX = "⏳ ";
    public static final String NUMBER_PREFIX = "№";


    public static final String EDIT_ROLE = "✏️ Изменить роль";
}
