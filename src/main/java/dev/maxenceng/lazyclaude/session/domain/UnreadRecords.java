package dev.maxenceng.lazyclaude.session.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * The hosted sessions whose record could not be used at the last read, so each is warned of once however often its record is
 * read again, and again if it recovers and fails anew.
 */
final class UnreadRecords {

    private final Set<SessionId> unread = ConcurrentHashMap.newKeySet();

    /** Notes that {@code session}'s record could not be used, and says whether this is the first read to find so. */
    boolean firstUnreadOf(SessionId session) {
        return unread.add(session);
    }

    void readable(SessionId session) {
        unread.remove(session);
    }
}
