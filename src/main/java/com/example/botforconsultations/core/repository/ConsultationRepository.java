package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByTeacherOrderByStartTimeAsc(TelegramUser teacher);

}
