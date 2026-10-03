package com.sgedts.demopaseto.service;

import com.sgedts.demopaseto.model.DemoEvent;
import com.sgedts.demopaseto.model.DemoMode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DemoStateService {

    private final AtomicReference<DemoMode> mode = new AtomicReference<>(DemoMode.JWT);
    private final CopyOnWriteArrayList<DemoEvent> events = new CopyOnWriteArrayList<>();

    public DemoMode getMode() {
        return mode.get();
    }

    public void setMode(DemoMode newMode) {
        mode.set(newMode);
    }

    public List<DemoEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public DemoEvent addEvent(String type, Map<String, Object> payload) {
        DemoEvent event = new DemoEvent(type, payload);
        events.add(0, event);
        // Keep max 40 events
        while (events.size() > 40) {
            events.remove(events.size() - 1);
        }
        return event;
    }

    public void clearEvents() {
        events.clear();
    }
}
