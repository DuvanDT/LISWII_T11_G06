package com.example.gymclass.domain;

import com.example.gymclass.domain.model.ClassSession;
import com.example.gymclass.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de objetos de valor e invariantes del agregado ClassSession. */
class DomainRulesTest {

    private ClassSession session(LocalDateTime start) {
        return new ClassSession(SessionId.newId(), "Pilates", ClassType.PILATES, new ClassSchedule(start, start.plusHours(1)), 10);
    }

    @Test
    void scheduleEndMustBeAfterStart() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class, () -> new ClassSchedule(now, now.minusHours(1)));
    }

    @Test
    void emailMustBeValidAndNormalized() {
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress("correo-invalido"));
        assertEquals("ana@mail.com", new EmailAddress("  ANA@Mail.com ").value());
    }

    @Test
    void plansRestrictClassTypes() {
        assertTrue(MembershipPlan.BASIC.allows(ClassType.YOGA));
        assertFalse(MembershipPlan.BASIC.allows(ClassType.PILATES));
        assertTrue(MembershipPlan.STANDARD.allows(ClassType.PILATES));
        assertFalse(MembershipPlan.STANDARD.allows(ClassType.SPINNING));
        assertTrue(MembershipPlan.PREMIUM.allows(ClassType.SPINNING));
    }

    @Test
    void memberCannotBookSameSessionTwice() {
        ClassSession session = session(LocalDateTime.now().plusDays(1));
        MemberId member = MemberId.newId();
        session.book(member, LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> session.book(member, LocalDateTime.now()));
    }

    @Test
    void bookingsCloseWhenClassStarts() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        ClassSession session = session(start);
        assertThrows(IllegalStateException.class, () -> session.book(MemberId.newId(), start));
    }

    @Test
    void capacityMustBePositive() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> new ClassSession(SessionId.newId(), "Yoga", ClassType.YOGA, new ClassSchedule(start, start.plusHours(1)), 0));
    }
}
