package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.model.User;
import com.example.botforconsultations.core.repository.TodoTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoTaskService {

    private final TodoTaskRepository todoTaskRepository;

    /**
     * Создать новую задачу для преподавателя
     */
    @Transactional
    public TodoTask createTodoForTeacher(User teacher, User createdBy, String title,
                                          String description, LocalDateTime deadline) {
        TodoTask todo = TodoTask.builder()
                .teacher(teacher)
                .createdBy(createdBy)
                .title(title)
                .description(description)
                .deadline(deadline)
                .isCompleted(false)
                .reminderSent(false)
                .build();

        TodoTask saved = todoTaskRepository.save(todo);
        log.info("Created todo task {} for teacher {} by user {}",
                saved.getId(), teacher.getId(), createdBy.getId());
        
        return saved;
    }

    /**
     * Получить все задачи преподавателя
     */
    public List<TodoTask> getTeacherTasks(User teacher) {
        return todoTaskRepository.findByTeacherOrderByDeadlineAsc(teacher);
    }

    /**
     * Получить активные задачи преподавателя
     */
    public List<TodoTask> getActiveTeacherTasks(User teacher) {
        return todoTaskRepository.findByTeacherAndIsCompletedFalseOrderByDeadlineAsc(teacher);
    }

    /**
     * Получить выполненные задачи преподавателя
     */
    public List<TodoTask> getCompletedTeacherTasks(User teacher) {
        return todoTaskRepository.findByTeacherAndIsCompletedTrueOrderByCompletedAtDesc(teacher);
    }

    /**
     * Отметить задачу как выполненную
     */
    @Transactional
    public void markAsCompleted(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();
            todo.setIsCompleted(true);
            todo.setCompletedAt(LocalDateTime.now());
            todoTaskRepository.save(todo);
            log.info("Todo task {} marked as completed", todoId);
        }
    }

    /**
     * Отменить выполнение задачи
     */
    @Transactional
    public void markAsIncomplete(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();
            todo.setIsCompleted(false);
            todo.setCompletedAt(null);
            todoTaskRepository.save(todo);
            log.info("Todo task {} marked as incomplete", todoId);
        }
    }

    /**
     * Удалить задачу
     */
    @Transactional
    public void deleteTodo(Long todoId) {
        todoTaskRepository.deleteById(todoId);
        log.info("Todo task {} deleted", todoId);
    }

    /**
     * Получить задачу по ID
     */
    public Optional<TodoTask> getTodoById(Long todoId) {
        return todoTaskRepository.findById(todoId);
    }

    /**
     * Обновить задачу
     */
    @Transactional
    public TodoTask updateTodo(Long todoId, String title, String description, 
                               LocalDateTime deadline) {
        TodoTask todo = todoTaskRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        todo.setTitle(title);
        todo.setDescription(description);
        todo.setDeadline(deadline);
        
        return todoTaskRepository.save(todo);
    }

    /**
     * Получить задачи, которым нужно отправить напоминание
     */
    public List<TodoTask> getTasksNeedingReminder(LocalDateTime now, LocalDateTime reminderTime) {
        return todoTaskRepository.findTasksNeedingReminder(now, reminderTime);
    }

    /**
     * Отметить что напоминание отправлено
     */
    @Transactional
    public void markReminderSent(Long todoId) {
        Optional<TodoTask> todoOpt = todoTaskRepository.findById(todoId);
        if (todoOpt.isPresent()) {
            TodoTask todo = todoOpt.get();
            todo.setReminderSent(true);
            todoTaskRepository.save(todo);
            log.info("Reminder sent for todo task {}", todoId);
        }
    }

    /**
     * Получить просроченные задачи
     */
    public List<TodoTask> getOverdueTasks() {
        return todoTaskRepository.findOverdueTasks(LocalDateTime.now());
    }

    /**
     * Получить все активные задачи (для деканата)
     */
    public List<TodoTask> getAllActiveTasks() {
        return todoTaskRepository.findByIsCompletedFalseOrderByDeadlineAsc();
    }
}
