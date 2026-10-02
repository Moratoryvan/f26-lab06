# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes.

**Why.** Overload resolution happens at compile time, and the first thing the
compiler does is discard candidates whose arity does not match the call. Both
call sites in `consumer/` pass four arguments:

- `FrontDesk.bookWalkIn` → `api.createBooking(roomId, startMinute, endMinute, null)`
  (`FrontDesk.java:27`)
- `FrontDesk.joinWaitlist` → `api.createBooking(roomId, startMinute, endMinute, guestName)`
  (`FrontDesk.java:33`)

The new overload `createBooking(String, long, long, String waitlistKey, String notes)`
takes five, so it is never even a candidate for those calls. Each four-argument
call still has exactly one applicable method, the original one, so there is no
ambiguity to report. That matters especially for `bookWalkIn`, which passes the
bare literal `null`: `null` is only ambiguous when two *same-arity* overloads
differ in a reference-typed parameter, and nothing of that shape is being
added.

`FrontDeskTest` goes through `FrontDesk`, never through `createBooking`
directly, and it constructs `new InMemoryBookingService()` rather than
implementing `BookingApi` itself, so adding a method to the interface cannot
leave an abstract method unimplemented in `consumer/`. I will still add the
overload as a `default` method on `BookingApi` (delegating to the four-argument
form and attaching the notes) so that any *other* implementor of the interface
is also unaffected; the `api/` module overrides it in `InMemoryBookingService`.

`Booking.getNotes()` is a new public getter on a final class with a
package-private constructor. No one outside `api/` can construct a `Booking`
or subclass it, so a new accessor cannot collide with anything the consumer
wrote. Existing bookings created through the four-argument path will report
`null` notes, and the consumer never asks for notes, so behaviour of every
sentence in the existing javadoc is unchanged.

Expected build: `api` compiles and its tests pass, `consumer` compiles with
no changes and `FrontDeskTest` passes, with zero warnings about `createBooking`.

### What happened

**The result.** `mvn -B clean test` from the repo root, consumer untouched
(`git status --porcelain consumer/` prints nothing). Both modules green, no
warnings of any kind in the log:

```
[INFO] --- compiler:3.13.0:compile (default-compile) @ lab06-api ---
[INFO] Compiling 4 source files with javac [debug deprecation release 21] to target/classes
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in edu.cmu.cs214.booking.InMemoryBookingServiceTest
[INFO] --- compiler:3.13.0:compile (default-compile) @ lab06-consumer ---
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/classes
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in edu.cmu.cs214.frontdesk.FrontDeskTest
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

(After adding one api-side test for the new overload, the api line reads
`Tests run: 6`; the consumer numbers are unchanged.)

**If your prediction was wrong,** nothing to report. The prediction was that
arity would filter the new five-argument method out of every consumer call
before the compiler even considered types, so the two call sites at
`FrontDesk.java:27` and `FrontDesk.java:33` would resolve exactly as before.
The consumer compiled without a diagnostic and all seven `FrontDeskTest`
cases passed, which is what that reasoning predicts. Note what the green
consumer build does *not* prove: `FrontDeskTest` never calls the new overload
or `getNotes()`, so it is evidence that the old contract survived, not that the
new method works. That is why the new test lives in `api/`.

**Is an additive change always safe in Java?** No. Three cases, all of which
leave the producer's own module compiling:

1. *Same-arity overload with a different reference type.* Had I added
   `createBooking(String, long, long, Integer partySize)` instead, the call
   `api.createBooking(roomId, startMinute, endMinute, null)` at
   `FrontDesk.java:27` would have two applicable candidates, neither more
   specific than the other, and `javac` reports
   `error: reference to createBooking is ambiguous`. The consumer goes red at
   compile time purely because of a literal `null` that used to be fine.
2. *A new abstract method on an interface.* If `createBooking(..., notes)`
   were declared abstract rather than `default`, every class outside `api/`
   that `implements BookingApi` (a test fake, a logging decorator) would fail
   with `is not abstract and does not override abstract method`. The
   consumer here happens not to implement the interface, so this one would
   not have bitten, but that is luck, not safety.
3. *A new enum constant.* Adding `BookingStatus.NO_SHOW` compiles everywhere,
   but a caller's `switch` over the status that was exhaustive yesterday now
   has an unhandled case. With a switch *expression* that is a compile error
   in the consumer; with a switch *statement* it is a silent fall-through at
   run time. Additive at the source, breaking at the call site.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** No. `consumer` goes
red at **compile time**, in the `compiler:compile` phase of `lab06-consumer`,
before `FrontDeskTest` is ever compiled or run. Removing the positional
`createBooking` overloads deletes a method the consumer's bytecode-to-be
names directly; there is nothing for the compiler to resolve a four-argument
call to, so `javac` reports something like
`error: method createBooking in interface BookingApi cannot be applied to given types`
(or `no suitable method found for createBooking(String,long,long,<null>)`),
with `required: BookingRequest` and `found: String,long,long,<null>`.

This is the mirror image of Milestone 1. Adding a method left every existing
call with exactly one applicable candidate. Removing one leaves the existing
calls with zero. The javadoc on `BookingApi` says callers are entitled to
everything stated there; the four-argument `createBooking` is stated there, so
by the contract's own definition this is a breaking change, and the build is
about to say so on the consumer's behalf.

`api` itself will stay green (once its tests are updated, see below), because
`api` only compiles against itself. Maven builds `api` first, so the reactor
will show `lab06-api SUCCESS`, then `lab06-consumer FAILURE`, and the build
stops there.

**Where.** Two call sites in `FrontDesk.java`, both in `consumer/src/main`:

- `FrontDesk.java:27`, `bookWalkIn`: `api.createBooking(roomId, startMinute, endMinute, null)`
- `FrontDesk.java:33`, `joinWaitlist`: `api.createBooking(roomId, startMinute, endMinute, guestName)`

`FrontDeskTest.java` does not call `createBooking` directly, it goes through
`FrontDesk`, so I do not expect any error to be reported in the test file.
It will not be compiled at all, since `compile` fails before `testCompile`.
`listBookings` and `cancelBooking` are untouched, so `displaySchedule`,
`cancelAndOfferToWaitlist` and `cancelQuietly` should produce no diagnostics.

**What about the tests in `api/`, after you update them?** They will pass.
`InMemoryBookingServiceTest` calls `createBooking` at roughly nine places
(lines 17, 25, 27, 33, 35, 43, 45, 52, 53, plus my Milestone 1 test). Each
one is a mechanical rewrite to build a `BookingRequest` and pass it; the
behaviour under test (conflict, waitlist, promotion, ids) does not change, so
every assertion still holds. If I forget one call site, `api` fails at
`testCompile` with the same "cannot be applied" error and the consumer is
never reached, which would tell me nothing new.

**Is that evidence about the consumer?** No. A green `api` module proves the
producer can use its own new surface, and that the implementation still obeys
the contract for callers who have *already migrated*. It says nothing about
callers who have not. The producer's suite was edited in the same commit as
the break, so it cannot detect the break by construction. The only thing in
this build that can is the consumer's module, because it is the only code
that still names the old signature. That is exactly why `FrontDeskTest` is
called the gate in the README, and why the gate fires at compile time rather
than at test time here: a removed method is caught by the type checker, not
by an assertion.

### Step 1: after the fold

**What the build printed.** `mvn -B clean test`, consumer untouched. The new
surface is `BookingRequest` (a record) and `Booking createBooking(BookingRequest)`;
both positional overloads are gone. All six api tests rewritten to the new call.

`lab06-api`:

```
[INFO] Compiling 5 source files with javac [debug deprecation release 21] to target/classes
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/test-classes
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in edu.cmu.cs214.booking.InMemoryBookingServiceTest
[INFO] lab06-api .......................................... SUCCESS [  0.701 s]
```

`lab06-consumer`:

```
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/classes
[ERROR] COMPILATION ERROR :
[ERROR] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,<nulltype>
[ERROR]   reason: actual and formal argument lists differ in length
[ERROR] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,java.lang.String
[ERROR]   reason: actual and formal argument lists differ in length
[INFO] lab06-consumer ..................................... FAILURE [  0.035 s]
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project lab06-consumer
```

Exactly the two call sites named in the prediction, both in the main compile
phase, with the error text predicted. Nothing reported in `FrontDeskTest.java`.

**Which module's tests ran, and which did not.** `InMemoryBookingServiceTest`
ran and passed, six of six. `FrontDeskTest` did not run and was not even
compiled: the failure is in `compile (default-compile)`, which precedes
`testCompile` and `surefire:test`, so Maven never got there.

What that says about detection: the producer's own suite is green while the
producer has just broken its contract. That is not a weak test suite, it is a
structural fact. I rewrote the tests in the same change that removed the
method, so they test the new contract and are blind to the old one by
construction. The only code in the build that can notice the break is code
that still depends on the old promise, and the only such code is in a module
I do not own. A contract break is detected by the *other* side of the
contract. The consumer's module is the gate, and here the gate is the type
checker, not an assertion, because a removed method fails earlier and more
loudly than a changed behaviour would.

### Step 2: the deprecation path

**What you added.** Both positional signatures came back on `BookingApi` as
`@Deprecated default` methods. Neither holds any logic; each builds a
`BookingRequest` and calls the one real method:

```java
@Deprecated
default Booking createBooking(String roomId, long startMinute, long endMinute,
                              String waitlistKey) {
    return createBooking(new BookingRequest(roomId, startMinute, endMinute, waitlistKey, null));
}

@Deprecated
default Booking createBooking(String roomId, long startMinute, long endMinute,
                              String waitlistKey, String notes) {
    return createBooking(new BookingRequest(roomId, startMinute, endMinute, waitlistKey, notes));
}
```

`InMemoryBookingService` implements only `createBooking(BookingRequest)`; the
deprecated forms reach it through the default methods. The `@deprecated`
javadoc tag on each names the replacement and the exact equivalent call.

**The warnings.** From `mvn -B clean test`, the only change in the output
between step 1 and step 2 is that the two `[ERROR]` lines on `FrontDesk.java`
became two `[WARNING]` lines at the same coordinates, and the consumer's
`testCompile` and `surefire:test` phases, which never ran in step 1, now run
and pass:

```
[INFO] --- compiler:3.13.0:compile (default-compile) @ lab06-consumer ---
[INFO] Compiling 1 source file with javac [debug deprecation release 21] to target/classes
[WARNING] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
[WARNING] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in edu.cmu.cs214.frontdesk.FrontDeskTest
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

`api` printed no warnings, because nothing in `api/` (including its tests)
calls the deprecated forms any more. A rerun of `mvn -B test` without `clean`
printed neither warning: javac saw nothing to recompile, so the deprecation
check never ran. The warning is emitted at compile time, on the caller's
compile, and only when the caller actually compiles.

**What the deprecation path resolves.** The front desk team can build again
without changing a line, which they could not during step 1. Both parties
are now on separate schedules: I shipped the new surface today, and they
migrate on their own timetable, under a visible, line-numbered reminder on
every clean build. The actual removal of the old overloads is a second,
later breaking change that waits until the warning count in their module is
zero. Step 1 was a one-sided change that forced the consumer to move at my
pace or stop building; step 2 lets each side move at its own pace with the
compiler keeping score.

**What the warnings accomplish that a README note would not.** A README note
is pull: it is read only by someone who opens the API's repo, on the day
they happen to open it, and the consumer team has not read my messages, let
alone my README. The warning is push: it shows up in the front desk team's
own build log, in their own module, at `FrontDesk.java:27` and `:33`, with
the exact method they are calling, every time they compile, without anyone
on their side having to look for it. The same text appears as a strikethrough
and hover in their IDE the moment they open the file. It also carries the
replacement, because the `@deprecated` javadoc tag is attached to the symbol
they are already hovering over, and it can be turned into a hard gate on
their side (`-Werror`, or a lint rule) when they decide they are ready. None
of that is possible for prose in a file they never open.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

**When the price is worth paying.** A condition under which it is.
