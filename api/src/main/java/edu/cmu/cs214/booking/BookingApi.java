package edu.cmu.cs214.booking;

import java.util.List;

/**
 * The room booking API, version 0.
 *
 * <p>This javadoc is the written contract. Callers outside this module (the
 * front desk app in {@code consumer/}, for one) are entitled to everything
 * stated here and to nothing else. If a change would make any sentence below
 * false for an existing caller, it is a breaking change no matter how the code
 * is shaped.
 *
 * <p>Vocabulary used throughout: a room's schedule is a set of bookings, each
 * covering the half-open minute range {@code [startMinute, endMinute)}. Two
 * ranges conflict when they overlap, so a booking ending at minute 600 and one
 * starting at minute 600 do not conflict. Minutes are counted from midnight on
 * the booking day.
 */
public interface BookingApi {

    /**
     * Books the room named in {@code request} for the half-open range
     * {@code [request.startMinute(), request.endMinute())}.
     *
     * <p>If no CONFIRMED booking on that room overlaps the range, the returned
     * booking is CONFIRMED and holds the room.
     *
     * <p>If some CONFIRMED booking does overlap, what happens next is decided
     * by {@code request.waitlistKey()}:
     * <ul>
     *   <li>null means do not waitlist on conflict. No booking is created and
     *       the method returns null. Nothing about the room's schedule
     *       changes.</li>
     *   <li>non-null means waitlist on conflict. A booking is created with
     *       status WAITLISTED, carrying the key, and returned. It does not
     *       hold the room. It becomes CONFIRMED only if it is later promoted,
     *       which happens when a conflicting booking is cancelled with
     *       notification (see {@link #cancelBooking(long, boolean)}).</li>
     * </ul>
     *
     * <p>The key itself is an opaque caller-supplied string. This API stores it
     * and hands it back on {@link Booking#getWaitlistKey()}; it never
     * interprets it. The key has no effect when there is no conflict: the
     * booking is CONFIRMED and the key is simply retained. The same holds for
     * {@code request.notes()}, handed back on {@link Booking#getNotes()}.
     *
     * <p>Ids are assigned by the implementation, are unique, and increase in
     * creation order.
     *
     * @param request what to book, non-null
     * @return the CONFIRMED booking, the WAITLISTED booking, or null when the
     *         range conflicts and no waitlist key was given
     * @throws IllegalArgumentException if {@code request} or its room id is
     *         null, or its end minute is not greater than its start minute
     */
    Booking createBooking(BookingRequest request);

    /**
     * Books a room with no notes.
     *
     * @deprecated since 1.0.0, replaced by {@link #createBooking(BookingRequest)}.
     *     Equivalent to
     *     {@code createBooking(new BookingRequest(roomId, startMinute, endMinute, waitlistKey, null))}
     *     and will be removed in a future version.
     * @param roomId      the room to book, non-null
     * @param startMinute first minute of the booking, inclusive
     * @param endMinute   first minute after the booking, exclusive
     * @param waitlistKey caller's waitlist key, or null to decline waitlisting
     * @return as {@link #createBooking(BookingRequest)}
     * @throws IllegalArgumentException as {@link #createBooking(BookingRequest)}
     */
    @Deprecated
    default Booking createBooking(String roomId, long startMinute, long endMinute,
                                  String waitlistKey) {
        return createBooking(new BookingRequest(roomId, startMinute, endMinute,
                waitlistKey, null));
    }

    /**
     * Books a room and attaches notes.
     *
     * @deprecated since 1.0.0, replaced by {@link #createBooking(BookingRequest)}.
     *     Equivalent to
     *     {@code createBooking(new BookingRequest(roomId, startMinute, endMinute, waitlistKey, notes))}
     *     and will be removed in a future version.
     * @param roomId      the room to book, non-null
     * @param startMinute first minute of the booking, inclusive
     * @param endMinute   first minute after the booking, exclusive
     * @param waitlistKey caller's waitlist key, or null to decline waitlisting
     * @param notes       free-text notes to attach, or null for none
     * @return as {@link #createBooking(BookingRequest)}
     * @throws IllegalArgumentException as {@link #createBooking(BookingRequest)}
     */
    @Deprecated
    default Booking createBooking(String roomId, long startMinute, long endMinute,
                                  String waitlistKey, String notes) {
        return createBooking(new BookingRequest(roomId, startMinute, endMinute,
                waitlistKey, notes));
    }

    /**
     * Returns every non-cancelled booking for one room, ordered by start minute.
     *
     * <p>Both CONFIRMED and WAITLISTED bookings are included; CANCELLED
     * bookings are excluded. Bookings that share a start minute come back in
     * creation order. An unknown room id is not an error: it yields an empty
     * list, same as a known room with nothing on it.
     *
     * <p>The returned list is a snapshot. Callers may hold it, and later
     * bookings on the room do not appear in it.
     *
     * @param roomId the room to list, non-null
     * @return a possibly empty list of non-cancelled bookings, ordered by start
     *         minute
     * @throws IllegalArgumentException if {@code roomId} is null
     */
    List<Booking> listBookings(String roomId);

    /**
     * Cancels one booking, optionally promoting someone off the waitlist.
     *
     * <p>The booking's status becomes CANCELLED. It stops holding the room and
     * stops appearing in {@link #listBookings(String)}.
     *
     * <p>{@code notifyWaitlist} decides what happens to the people queued
     * behind it:
     * <ul>
     *   <li>true promotes at most one booking. Among the WAITLISTED bookings on
     *       the same room that overlap the cancelled booking's range, the
     *       implementation takes the first in creation order that no longer
     *       conflicts with any remaining CONFIRMED booking, and sets it to
     *       CONFIRMED. If every candidate still conflicts, or there are no
     *       candidates, nothing is promoted and the cancellation still
     *       stands.</li>
     *   <li>false cancels quietly. No booking is ever promoted, and the
     *       waitlisted bookings stay WAITLISTED.</li>
     * </ul>
     *
     * <p>At most one promotion happens per call, whichever way the flag is set.
     *
     * @param bookingId      the booking to cancel
     * @param notifyWaitlist true to promote the first eligible overlapping
     *                       waitlisted booking, false to cancel without
     *                       promoting anyone
     * @return true if a booking was cancelled by this call; false if the id is
     *         unknown or the booking was already CANCELLED. An unknown id is
     *         not an error and changes nothing.
     */
    boolean cancelBooking(long bookingId, boolean notifyWaitlist);
}
