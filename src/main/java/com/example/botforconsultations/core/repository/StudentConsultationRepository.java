package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * Находит всех студентов, записанных на консультацию
     */
    List<StudentConsultation> findByConsultation(Consultation consultation);
    
    /**
     * Подсчитывает количество студентов на консультации
     */
    long countByConsultation(Consultation consultation);
}
