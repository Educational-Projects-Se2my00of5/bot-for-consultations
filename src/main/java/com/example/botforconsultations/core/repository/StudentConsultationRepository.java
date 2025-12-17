package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentConsultationRepository extends JpaRepository<StudentConsultation, Long> {

    /**
     * Проверяет, записан ли студент на консультацию
     */
    boolean existsByStudentAndConsultation(TelegramUser student, Consultation consultation);

    /**
     * Находит запись студента на консультацию
     */
    Optional<StudentConsultation> findByStudentAndConsultation(TelegramUser student, Consultation consultation);

    /**
     * Находит запись по консультации и студенту (обратный порядок)
     */
    Optional<StudentConsultation> findByConsultationAndStudent(Consultation consultation, TelegramUser student);

    /**
     * Находит все записи студента на консультации
     */
    List<StudentConsultation> findByStudent(TelegramUser student);

    /**
     * Находит все записи студента только на консультации (исключая запросы)
     */
    @Query("SELECT sc FROM StudentConsultation sc WHERE sc.student = :student AND sc.consultation.status != 'REQUEST'")
    List<StudentConsultation> findByStudentExcludingRequests(@Param("student") TelegramUser student);

    /**
     * Находит всех студентов, записанных на консультацию
     */
    List<StudentConsultation> findByConsultation(Consultation consultation);

    /**
     * Подсчитывает количество студентов на консультации
     */
    long countByConsultation(Consultation consultation);

    /**
     * Удаляет все записи студентов для консультации
     */
    void deleteByConsultation(Consultation consultation);

    /**
     * Удаляет все записи студентов для консультаций старше заданной даты (bulk delete)
     */
    @Modifying
    @Query("DELETE FROM StudentConsultation sc WHERE sc.consultation.date < :date")
    int deleteByConsultationDateBefore(@Param("date") LocalDate date);
}
