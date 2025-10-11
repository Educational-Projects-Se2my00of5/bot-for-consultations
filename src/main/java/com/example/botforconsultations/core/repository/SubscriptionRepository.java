package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Subscription;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    /**
     * Проверяет, подписан ли студент на преподавателя
     */
    boolean existsByStudentAndTeacher(TelegramUser student, TelegramUser teacher);
    
    /**
     * Находит подписку студента на преподавателя
     */
    Optional<Subscription> findByStudentAndTeacher(TelegramUser student, TelegramUser teacher);
    
    /**
     * Находит все подписки студента
     */
    List<Subscription> findByStudent(TelegramUser student);
    
    /**
     * Находит всех подписчиков преподавателя
     */
    List<Subscription> findByTeacher(TelegramUser teacher);
    
    /**
     * Удаляет подписку студента на преподавателя
     */
    @Transactional
    void deleteByStudentAndTeacher(TelegramUser student, TelegramUser teacher);
}
