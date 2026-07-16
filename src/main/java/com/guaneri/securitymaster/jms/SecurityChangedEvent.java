package com.guaneri.securitymaster.jms;

import java.time.Instant;
import java.util.UUID;

public record SecurityChangedEvent(
    UUID internalId, String paceSecId, String sedol, String cusip, String ticker, String changeType, Instant timestamp) {}
