package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByTeacherOrderByStartTimeAsc(TelegramUser teacher);
    
    // Для запросов консультаций: teacher = студент, status = REQUEST
    List<Consultation> findByTeacherAndStatusOrderByIdDesc(TelegramUser student, ConsultationStatus status);
    
    // Для scheduled tasks
    List<Consultation> findByStatus(ConsultationStatus status);
    
    /**
     * Найти все открытые консультации, которые уже прошли (дата в прошлом)
     */
    @Query("SELECT c FROM Consultation c WHERE c.status = :status AND c.date < :date")
    List<Consultation> findExpiredConsultations(@Param("status") ConsultationStatus status, 
                                                 @Param("date") LocalDate date);
    
    /**
     * Удалить все консультации старше заданной даты (bulk delete)
     * ВАЖНО: Этот запрос обходит JPA каскады, поэтому нужно сначала удалить связанные записи!
     */
    @Modifying
    @Query("DELETE FROM Consultation c WHERE c.date < :date")
    int deleteByDateBefore(@Param("date") LocalDate date);
}
