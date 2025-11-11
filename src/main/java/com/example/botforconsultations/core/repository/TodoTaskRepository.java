package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.TodoTask;
import com.example.botforconsultations.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TodoTaskRepository extends JpaRepository<TodoTask, Long> {

    // Все задачи конкретного преподавателя
    List<TodoTask> findByTeacherOrderByDeadlineAsc(User teacher);

    // Активные (не выполненные) задачи преподавателя
    List<TodoTask> findByTeacherAndIsCompletedFalseOrderByDeadlineAsc(User teacher);

    // Выполненные задачи преподавателя
    List<TodoTask> findByTeacherAndIsCompletedTrueOrderByCompletedAtDesc(User teacher);

    // Задачи с истекающим дедлайном (для напоминаний)
    @Query("SELECT t FROM TodoTask t WHERE t.isCompleted = false " +
           "AND t.deadline IS NOT NULL " +
           "AND t.reminderSent = false " +
           "AND t.deadline BETWEEN :now AND :reminderTime")
    List<TodoTask> findTasksNeedingReminder(@Param("now") LocalDateTime now,
                                             @Param("reminderTime") LocalDateTime reminderTime);

    // Просроченные задачи
    @Query("SELECT t FROM TodoTask t WHERE t.isCompleted = false " +
           "AND t.deadline IS NOT NULL " +
           "AND t.deadline < :now")
    List<TodoTask> findOverdueTasks(@Param("now") LocalDateTime now);

    // Все активные задачи (для деканата)
    List<TodoTask> findByIsCompletedFalseOrderByDeadlineAsc();
}
