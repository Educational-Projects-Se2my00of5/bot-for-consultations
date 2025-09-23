package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.api.dto.ConsultationDto;
import com.example.botforconsultations.api.mapper.ConsultationMapper;
import com.example.botforconsultations.core.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ConsultationMapper consultationMapper;

    // 1. Посмотреть консультации у преподавателя
    @GetMapping("/consultations/{teacherId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ConsultationDto.ConsultationInfo> getConsultationsByTeacher(@PathVariable Long teacherId) {
        return consultationMapper.toConsultationDto(studentService.getConsultationsByTeacher(teacherId));
    }

//    // 2. Записаться на консультацию
//    @PostMapping("/consultations/{consultationId}/signup")
//    @ResponseStatus(HttpStatus.OK)
//    public void signupForConsultation(@PathVariable Long consultationId,
//                                      @RequestParam String requestText) {
//        studentService.signupForConsultation(consultationId, requestText);
//
//    }
//
//    // 3. Отменить запись на консультацию
//    @DeleteMapping("/consultations/{consultationId}/cancel")
//    @ResponseStatus(HttpStatus.OK)
//    public void cancelConsultationSignup(@PathVariable Long consultationId) {
//        studentService.cancelConsultationSignup(consultationId);
//
//    }
//
//    // 4. Подписаться на обновления преподавателя
//    @PostMapping("/teachers/{teacherId}/subscribe")
//    @ResponseStatus(HttpStatus.OK)
//    public void subscribeToTeacher(@PathVariable Long teacherId) {
//        studentService.subscribeToTeacher(teacherId);
//    }
//
//    // 5. Создать запрос на консультацию
//    @PostMapping("/requests")
//    @ResponseStatus(HttpStatus.OK)
//    public RequestDTO createRequest(@RequestParam String topic) {
//        return studentService.createRequest(topic);
//    }
//
//    // 6. Получить список запросов на консультацию
//    @GetMapping("/requests")
//    @ResponseStatus(HttpStatus.OK)
//    public List<RequestDTO> getRequests() {
//        return studentService.getRequests();
//    }
//
//    // 7. Подписаться под студентским запросом
//    @PostMapping("/requests/{requestId}/subscribe")
//    @ResponseStatus(HttpStatus.OK)
//    public String subscribeToRequest(@PathVariable Long requestId) {
//        studentService.subscribeToRequest(requestId);
//        return "Вы подписаны на запрос!";
//    }
}
