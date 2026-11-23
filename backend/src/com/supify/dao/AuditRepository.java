package com.supify.dao;

import com.supify.model.AuditEvent;
import java.util.List;

public interface AuditRepository {
    AuditEvent recordEvent(AuditEvent event);
    List<AuditEvent> findRecentEvents(int limit);
    long count();
}
