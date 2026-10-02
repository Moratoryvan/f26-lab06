package edu.cmu.cs214.booking;

/**
 * One room booking.
 *
 * <p>Times are minutes from midnight on the booking day, as raw longs. The
 * interval is half-open: {@code [startMinute, endMinute)}.
 *
 * <p>Every field is fixed at creation except the status, which the service
 * updates as the booking is cancelled or promoted off the waitlist.
 */
public final class Booking {

    private final long id;
    private final String roomId;
    private final long startMinute;
    private final long endMinute;
    private final String waitlistKey;
    private final String notes;
    private BookingStatus status;

    Booking(long id, String roomId, long startMinute, long endMinute,
            BookingStatus status, String waitlistKey) {
        this(id, roomId, startMinute, endMinute, status, waitlistKey, null);
    }

    Booking(long id, String roomId, long startMinute, long endMinute,
            BookingStatus status, String waitlistKey, String notes) {
        this.id = id;
        this.roomId = roomId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.status = status;
        this.waitlistKey = waitlistKey;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public long getStartMinute() {
        return startMinute;
    }

    public long getEndMinute() {
        return endMinute;
    }

    public BookingStatus getStatus() {
        return status;
    }

    /** The waitlist key this booking was created with, or null if none was given. */
    public String getWaitlistKey() {
        return waitlistKey;
    }

    /**
     * Free-text notes attached when the booking was created, or null if the
     * booking was created without notes.
     */
    public String getNotes() {
        return notes;
    }

    void setStatus(BookingStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Booking[" + id + " " + roomId + " " + startMinute + "-" + endMinute
                + " " + status + "]";
    }
}
