package com.example.botforconsultations.api.mapper;

import com.example.botforconsultations.api.dto.ConsultationDto;
import com.example.botforconsultations.core.model.Consultation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConsultationMapper {

    @Mapping(target = "userFullName", 
            expression = "java(consultation.getUser().getFirstName() + \" \" + (consultation.getUser().getLastName() != null ? consultation.getUser().getLastName() : \"\"))")
    ConsultationDto.ConsultationInfo toConsultationDto(Consultation consultation);

    List<ConsultationDto.ConsultationInfo> toConsultationDto(List<Consultation> consultations);
}
