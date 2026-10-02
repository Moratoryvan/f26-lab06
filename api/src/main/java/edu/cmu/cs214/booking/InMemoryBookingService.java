package edu.cmu.cs214.booking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** In-memory {@link BookingApi}. Bookings live in one list, in creation order. */
public class InMemoryBookingService implements BookingApi {

    private final List<Booking> bookings = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Booking createBooking(String roomId, long startMinute, long endMinute,
                                 String waitlistKey) {
        return createBooking(roomId, startMinute, endMinute, waitlistKey, null);
    }

    @Override
    public Booking createBooking(String roomId, long startMinute, long endMinute,
                                 String waitlistKey, String notes) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        if (endMinute <= startMinute) {
            throw new IllegalArgumentException(
                    "endMinute must be greater than startMinute");
        }

        boolean conflict = hasConfirmedConflict(roomId, startMinute, endMinute);
        if (conflict && waitlistKey == null) {
            return null;
        }

        BookingStatus status = conflict ? BookingStatus.WAITLISTED : BookingStatus.CONFIRMED;
        Booking booking = new Booking(nextId++, roomId, startMinute, endMinute,
                status, waitlistKey, notes);
        bookings.add(booking);
        return booking;
    }

    @Override
    public List<Booking> listBookings(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        List<Booking> result = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getRoomId().equals(roomId)
                    && booking.getStatus() != BookingStatus.CANCELLED) {
                result.add(booking);
            }
        }
        result.sort(Comparator.comparingLong(Booking::getStartMinute)
                .thenComparingLong(Booking::getId));
        return result;
    }

    @Override
    public boolean cancelBooking(long bookingId, boolean notifyWaitlist) {
        Booking cancelled = findById(bookingId);
        if (cancelled == null || cancelled.getStatus() == BookingStatus.CANCELLED) {
            return false;
        }
        cancelled.setStatus(BookingStatus.CANCELLED);

        if (notifyWaitlist) {
            promoteFirstEligible(cancelled);
        }
        return true;
    }

    private void promoteFirstEligible(Booking cancelled) {
        for (Booking candidate : bookings) {
            if (candidate.getStatus() != BookingStatus.WAITLISTED
                    || !candidate.getRoomId().equals(cancelled.getRoomId())) {
                continue;
            }
            boolean overlapsCancelled = overlaps(candidate.getStartMinute(),
                    candidate.getEndMinute(), cancelled.getStartMinute(),
                    cancelled.getEndMinute());
            if (!overlapsCancelled) {
                continue;
            }
            if (!hasConfirmedConflict(candidate.getRoomId(), candidate.getStartMinute(),
                    candidate.getEndMinute())) {
                candidate.setStatus(BookingStatus.CONFIRMED);
                return;
            }
        }
    }

    private Booking findById(long bookingId) {
        for (Booking booking : bookings) {
            if (booking.getId() == bookingId) {
                return booking;
            }
        }
        return null;
    }

    private boolean hasConfirmedConflict(String roomId, long startMinute, long endMinute) {
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED
                    && booking.getRoomId().equals(roomId)
                    && overlaps(startMinute, endMinute, booking.getStartMinute(),
                            booking.getEndMinute())) {
                return true;
            }
        }
        return false;
    }

    /** Half-open overlap: [aStart, aEnd) against [bStart, bEnd). */
    private static boolean overlaps(long aStart, long aEnd, long bStart, long bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }
}
