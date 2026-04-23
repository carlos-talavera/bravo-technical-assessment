package com.charlie2code.bravotechnicalassessment.infrastructure.sse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        var emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(String eventName, Object payload) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        });
    }
}
