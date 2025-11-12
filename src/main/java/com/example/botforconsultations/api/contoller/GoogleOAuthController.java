package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.api.bot.BotMessenger;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.service.GoogleCalendarService;
import com.example.botforconsultations.core.service.GoogleOAuthService;
import com.example.botforconsultations.api.bot.service.TodoTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер для OAuth авторизации с Google Calendar
 */
@Slf4j
@Controller
@RequestMapping("/api/oauth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService oAuthService;
    private final GoogleCalendarService calendarService;
    private final TodoTaskService todoTaskService;
    private final TelegramUserRepository userRepository;
    private final BotMessenger botMessenger;

    /**
     * Callback endpoint для OAuth авторизации
     */
    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) {
        
        try {
            // Проверяем, есть ли ошибка от Google
            if (error != null) {
                log.error("OAuth error from Google: {}", error);
                return new RedirectView("/oauth-error.html?message=Google+OAuth+error:+" + error);
            }
            
            // Проверяем наличие обязательных параметров
            if (code == null || state == null) {
                log.error("Missing required parameters: code={}, state={}", code, state);
                return new RedirectView("/oauth-error.html?message=Missing+required+parameters");
            }
            
            // state содержит userId
            Long userId;
            try {
                userId = Long.parseLong(state);
            } catch (NumberFormatException e) {
                log.error("Invalid state parameter: {}", state);
                return new RedirectView("/oauth-error.html?message=Invalid+state+parameter");
            }
            
            Optional<TelegramUser> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.error("User not found for OAuth callback: userId={}", userId);
                return new RedirectView("/oauth-error.html?message=User+not+found");
            }

            TelegramUser user = userOpt.get();

            // Сохраняем токены
            oAuthService.handleCallback(code, user);

            // После успешного подключения добавляем все активные задачи в календарь
            syncExistingTasksToCalendar(user);

            // Уведомляем пользователя в боте
            notifyUserAboutConnection(user);

            log.info("Successfully connected Google Calendar for user #{}", userId);
            return new RedirectView("/oauth-success.html");

        } catch (Exception e) {
            log.error("Error handling OAuth callback: {}", e.getMessage(), e);
            return new RedirectView("/oauth-error.html?message=" + e.getMessage());
        }
    }

    /**
     * Синхронизация существующих задач в календарь при подключении OAuth
     */
    private void syncExistingTasksToCalendar(TelegramUser user) {
        try {
            // Получаем все активные задачи преподавателя, где дедлайн еще не прошел
            List<TodoTask> activeTasks = todoTaskService.getTasksByTeacherId(user.getId())
                    .stream()
                    .filter(task -> !task.getIsCompleted())
                    .filter(task -> task.getDeadline() != null)
                    .filter(task -> task.getDeadline().isAfter(LocalDateTime.now()))
                    .filter(task -> task.getGoogleCalendarEventId() == null) // Еще не добавлены в календарь
                    .toList();

            int syncedCount = 0;
            for (TodoTask task : activeTasks) {
                log.info("Syncing task: {}", task);
                Optional<String> eventIdOpt = calendarService.createTaskEvent(user, task);
                if (eventIdOpt.isPresent()) {
                    // Сохраняем ID события в задаче
                    task.setGoogleCalendarEventId(eventIdOpt.get());
                    todoTaskService.saveTask(task);
                    syncedCount++;
                }
            }

            log.info("Synced {} existing tasks to Google Calendar for user #{}", 
                    syncedCount, user.getId());

        } catch (Exception e) {
            log.error("Error syncing existing tasks to calendar for user #{}: {}", 
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Уведомление пользователя об успешном подключении
     */
    private void notifyUserAboutConnection(TelegramUser user) {
        try {
            String message = """
                    ✅ Google Calendar успешно подключен!
                    
                    📅 Все ваши активные задачи добавлены в календарь.
                    📬 Новые задачи будут автоматически добавляться в календарь.
                    ⏰ Напоминания будут приходить согласно вашим настройкам.
                    
                    Вы можете отключить интеграцию в любой момент через профиль.
                    """;
            
            botMessenger.sendText(message, user.getTelegramId());
        } catch (Exception e) {
            log.error("Error notifying user #{} about calendar connection: {}", 
                    user.getId(), e.getMessage());
        }
    }
}
