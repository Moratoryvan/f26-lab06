package edu.cmu.cs214.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The producer's own suite. It never touches the consumer module. */
class InMemoryBookingServiceTest {

    private final BookingApi api = new InMemoryBookingService();

    @Test
    void freeRoomGivesConfirmedBooking() {
        Booking booking = api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(540, booking.getStartMinute());
    }

    @Test
    void conflictWithoutKeyReturnsNull() {
        api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        assertNull(api.createBooking(new BookingRequest("R1", 570, 630, null, null)));
        assertEquals(1, api.listBookings("R1").size());
    }

    @Test
    void conflictWithKeyGoesOnWaitlist() {
        api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        Booking queued = api.createBooking(new BookingRequest("R1", 570, 630, "party-of-four", null));

        assertEquals(BookingStatus.WAITLISTED, queued.getStatus());
        assertEquals("party-of-four", queued.getWaitlistKey());
    }

    @Test
    void touchingRangesDoNotConflict() {
        api.createBooking(new BookingRequest("R1", 540, 600, null, null));

        Booking next = api.createBooking(new BookingRequest("R1", 600, 660, null, null));

        assertEquals(BookingStatus.CONFIRMED, next.getStatus());
    }

    @Test
    void cancelWithNotifyPromotesTheWaitlistedBooking() {
        Booking held = api.createBooking(new BookingRequest("R1", 540, 600, null, null));
        Booking queued = api.createBooking(new BookingRequest("R1", 570, 630, "party-of-four", null));

        assertTrue(api.cancelBooking(held.getId(), true));

        assertEquals(BookingStatus.CONFIRMED, queued.getStatus());
        List<Booking> schedule = api.listBookings("R1");
        assertEquals(1, schedule.size());
        assertEquals(queued.getId(), schedule.get(0).getId());
    }

    @Test
    void notesOverloadStoresNotesAndKeepsFourArgumentBehaviour() {
        Booking withNotes = api.createBooking(new BookingRequest("R1", 540, 600, null, "projector please"));
        Booking without = api.createBooking(new BookingRequest("R1", 600, 660, null, null));

        assertEquals("projector please", withNotes.getNotes());
        assertEquals(BookingStatus.CONFIRMED, withNotes.getStatus());
        assertNull(without.getNotes());
        assertNull(api.createBooking(new BookingRequest("R1", 570, 630, null, "still conflicts")));
    }
}
