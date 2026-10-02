package edu.cmu.cs214.booking;

/**
 * Everything needed to create one booking. Passed to
 * {@link BookingApi#createBooking(BookingRequest)}.
 *
 * <p>This is a plain value: it performs no validation of its own. The rules
 * on {@code roomId} and the minute range are checked, and reported as
 * {@link IllegalArgumentException}, by {@code createBooking}.
 *
 * @param roomId      the room to book, non-null
 * @param startMinute first minute of the booking, inclusive
 * @param endMinute   first minute after the booking, exclusive; must be
 *                    greater than {@code startMinute}
 * @param waitlistKey caller's waitlist key, or null to decline waitlisting
 * @param notes       free-text notes to attach, or null for none
 */
public record BookingRequest(String roomId, long startMinute, long endMinute,
                             String waitlistKey, String notes) {

    /** A request with no waitlist key and no notes. */
    public static BookingRequest of(String roomId, long startMinute, long endMinute) {
        return new BookingRequest(roomId, startMinute, endMinute, null, null);
    }
}
