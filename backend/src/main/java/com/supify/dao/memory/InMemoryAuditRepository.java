package com.supify.dao.memory;

import com.supify.dao.AuditRepository;
import com.supify.model.AuditEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class InMemoryAuditRepository implements AuditRepository {
    private final ConcurrentLinkedDeque<AuditEvent> auditLog = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOG_SIZE = 1000;

    @Override
    public AuditEvent recordEvent(AuditEvent event) {
        auditLog.addFirst(event); // newest first
        if (auditLog.size() > MAX_LOG_SIZE) {
            auditLog.removeLast();
        }
        return event;
    }

    @Override
    public List<AuditEvent> findRecentEvents(int limit) {
        return auditLog.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return auditLog.size();
    }
}
