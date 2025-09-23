package com.example.botforconsultations.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class ConsultationDto {

    public record ConsultationInfo(
            Long id, String title, LocalTime startTime,
            LocalTime endTime, LocalDate date, String userFullName
    ) {
    }

}
