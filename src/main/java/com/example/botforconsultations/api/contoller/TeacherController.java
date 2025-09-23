package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.core.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

//    // 1. Опубликовать время консультации
//    @PostMapping("/consultations/{consultationId}/publish")
//    @ResponseStatus(HttpStatus.OK)
//    public void publishConsultation(@PathVariable Long consultationId) {
//        teacherService.publishConsultation(consultationId);
//    }
//
//    // 2. Просмотреть список студентов на консультации
//    @GetMapping("/consultations/{consultationId}/students")
//    @ResponseStatus(HttpStatus.OK)
//    public List<StudentDTO> getStudents(@PathVariable Long consultationId) {
//        return teacherService.getStudentsOnConsultation(consultationId);
//    }
//
//    // 3. Закрыть запись (можно с лимитом)
//    @PostMapping("/consultations/{consultationId}/close")
//    @ResponseStatus(HttpStatus.OK)
//    public void closeConsultation(@PathVariable Long consultationId,
//                                    @RequestParam(required = false) Integer limit) {
//        teacherService.closeConsultation(consultationId, limit);
//
//    }
//
//    // 4. Отменить консультацию
//    @DeleteMapping("/consultations/{consultationId}")
//    @ResponseStatus(HttpStatus.OK)
//    public void cancelConsultation(@PathVariable Long consultationId) {
//        teacherService.cancelConsultation(consultationId);
//    }
//
//    // 5. Создать консультацию на основе запроса студентов
//    @PostMapping("/consultations/from-request/{requestId}")
//    @ResponseStatus(HttpStatus.OK)
//    public ConsultationDTO createFromRequest(@PathVariable Long requestId) {
//        return teacherService.createFromRequest(requestId);
//    }
}
