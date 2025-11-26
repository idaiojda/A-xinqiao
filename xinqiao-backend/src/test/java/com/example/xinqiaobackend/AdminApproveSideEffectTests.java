package com.example.xinqiaobackend;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.entity.ScheduleSlot;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import com.example.xinqiaobackend.repository.ScheduleSlotRepository;
import com.example.xinqiaobackend.security.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=this_is_a_very_long_test_secret_key_for_jwt_256_bits",
        "app.jwt.expiration-seconds=3600"
})
public class AdminApproveSideEffectTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AppointmentRepository apptRepo;
    @Autowired private ScheduleSlotRepository slotRepo;

    @Test
    void adminApprove_closesSlot() throws Exception {
        String adminToken = jwtUtil.generateToken("admin", Collections.singletonList("ADMIN"));
        String counselor = "c1";
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        ScheduleSlot slot = new ScheduleSlot();
        slot.setCounselorUsername(counselor);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setAvailable(true);
        slot = slotRepo.save(slot);

        Appointment a = new Appointment();
        a.setUserUsername("u1");
        a.setCounselorUsername(counselor);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus(AppointmentStatus.PENDING);
        a = apptRepo.save(a);

        mockMvc.perform(post("/api/admin/appointments/" + a.getId() + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        ScheduleSlot reloaded = slotRepo.findById(slot.getId()).orElseThrow();
        Assertions.assertFalse(reloaded.isAvailable(), "slot should be closed after approve");
        Appointment ap = apptRepo.findById(a.getId()).orElseThrow();
        Assertions.assertEquals(AppointmentStatus.APPROVED, ap.getStatus());
    }
}