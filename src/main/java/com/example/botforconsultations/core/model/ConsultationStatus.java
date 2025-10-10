package com.example.botforconsultations.core.model;

public enum ConsultationStatus {
    OPEN,        // открыта для записи
    CLOSED,      // запись временно закрыта (можно вновь открыть)
    CANCELLED,   // консультация отменена (окончательно)
    REQUEST      // запрос от студентов, без даты/времени/преподавателя
}


