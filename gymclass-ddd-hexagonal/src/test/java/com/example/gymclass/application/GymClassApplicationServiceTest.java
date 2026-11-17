package com.example.gymclass.application;

import com.example.gymclass.application.dto.Requests.RegisterMemberRequest;
import com.example.gymclass.application.dto.Requests.ScheduleClassRequest;
import com.example.gymclass.application.dto.Responses.BookingResponse;
import com.example.gymclass.application.dto.Responses.MemberResponse;
import com.example.gymclass.application.dto.Responses.SessionResponse;
import com.example.gymclass.application.port.out.ClassSessionRepositoryPort;
import com.example.gymclass.application.port.out.MemberRepositoryPort;
import com.example.gymclass.application.service.GymClassApplicationService;
import com.example.gymclass.domain.model.ClassSession;
import com.example.gymclass.domain.model.Member;
import com.example.gymclass.domain.service.BookingPolicy;
import com.example.gymclass.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Prueba el núcleo de la aplicación con adaptadores de salida en memoria (sin Spring ni H2). */
class GymClassApplicationServiceTest {

    static class InMemoryMemberRepository implements MemberRepositoryPort {
        private final Map<MemberId, Member> data = new HashMap<>();
        public Member save(Member member) { data.put(member.id(), member); return member; }
        public Optional<Member> findById(MemberId id) { return Optional.ofNullable(data.get(id)); }
        public boolean existsByEmail(String email) { return data.values().stream().anyMatch(m -> m.email().value().equals(email)); }
    }

    static class InMemoryClassSessionRepository implements ClassSessionRepositoryPort {
        private final Map<SessionId, ClassSession> data = new HashMap<>();
        public ClassSession save(ClassSession session) { data.put(session.id(), session); return session; }
        public Optional<ClassSession> findById(SessionId id) { return Optional.ofNullable(data.get(id)); }
        public List<ClassSession> findAll() { return new ArrayList<>(data.values()); }
    }

    private GymClassApplicationService service;

    @BeforeEach
    void setUp() {
        service = new GymClassApplicationService(new InMemoryMemberRepository(), new InMemoryClassSessionRepository(), new BookingPolicy());
    }

    private MemberResponse register(String email, MembershipPlan plan) {
        return service.registerMember(new RegisterMemberRequest("Ana Gomez", email, plan));
    }

    private SessionResponse schedule(ClassType type, int capacity) {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        return service.scheduleClass(new ScheduleClassRequest("Clase", type, start, start.plusHours(1), capacity));
    }

    @Test
    void shouldBookClassAndReduceAvailableSpots() {
        MemberResponse member = register("ana@mail.com", MembershipPlan.PREMIUM);
        SessionResponse session = schedule(ClassType.SPINNING, 5);

        BookingResponse booking = service.bookClass(session.id().toString(), member.id().toString());

        assertEquals("CONFIRMED", booking.status());
        assertEquals(4, service.listSessions().get(0).availableSpots());
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        register("ana@mail.com", MembershipPlan.BASIC);
        assertThrows(IllegalStateException.class, () -> register("ANA@mail.com", MembershipPlan.BASIC));
    }

    @Test
    void basicPlanShouldNotBookSpinning() {
        MemberResponse member = register("ana@mail.com", MembershipPlan.BASIC);
        SessionResponse session = schedule(ClassType.SPINNING, 5);
        assertThrows(IllegalStateException.class, () -> service.bookClass(session.id().toString(), member.id().toString()));
    }

    @Test
    void suspendedMemberShouldNotBookAndCanBeReactivated() {
        MemberResponse member = register("ana@mail.com", MembershipPlan.PREMIUM);
        SessionResponse session = schedule(ClassType.YOGA, 5);

        assertEquals("SUSPENDED", service.suspendMember(member.id().toString()).status());
        assertThrows(IllegalStateException.class, () -> service.bookClass(session.id().toString(), member.id().toString()));

        assertEquals("ACTIVE", service.activateMember(member.id().toString()).status());
        assertEquals("CONFIRMED", service.bookClass(session.id().toString(), member.id().toString()).status());
    }

    @Test
    void cancelBookingShouldReleaseSpot() {
        MemberResponse member = register("ana@mail.com", MembershipPlan.PREMIUM);
        SessionResponse session = schedule(ClassType.PILATES, 1);
        service.bookClass(session.id().toString(), member.id().toString());

        SessionResponse result = service.cancelBooking(session.id().toString(), member.id().toString());

        assertEquals(1, result.availableSpots());
    }

    @Test
    void shouldFailWhenMemberDoesNotExist() {
        SessionResponse session = schedule(ClassType.YOGA, 5);
        assertThrows(NoSuchElementException.class, () -> service.bookClass(session.id().toString(), UUID.randomUUID().toString()));
    }
}
