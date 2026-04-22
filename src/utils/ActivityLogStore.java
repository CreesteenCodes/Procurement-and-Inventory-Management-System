package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ActivityLogStore {

    public enum Period {
        DAY,
        WEEK,
        MONTH
    }

    public static final class ActivityEntry {
        private final LocalDateTime timestamp;
        private final String description;

        public ActivityEntry(LocalDateTime timestamp, String description) {
            this.timestamp = timestamp;
            this.description = description;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final int MAX_FETCH_ENTRIES = 500;
    private static final int MAX_STORED_ENTRIES = 5000;
    private static final Path STORAGE_FILE = Paths.get(
        System.getProperty("user.home"),
        ".celeste-pim",
        "activity-logs.txt"
    );
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static final List<ActivityEntry> LOG_ENTRIES = new ArrayList<>();
    private static boolean entriesLoaded;

    private ActivityLogStore() {
    }

    public static synchronized void log(String description) {
        if(description == null || description.trim().isEmpty()) {
            return;
        }

        ensureEntriesLoaded();

        LOG_ENTRIES.add(new ActivityEntry(LocalDateTime.now(), description.trim()));
        trimToMaxStoredEntries();
        persistEntries();

        notifyListeners();
    }

    public static synchronized List<ActivityEntry> getEntries(Period period) {
        ensureEntriesLoaded();

        LocalDateTime periodStart = getPeriodStart(period);
        LocalDateTime now = LocalDateTime.now();
        List<ActivityEntry> entries = new ArrayList<>();

        for(int i = LOG_ENTRIES.size() - 1; i >= 0; i--) {
            ActivityEntry entry = LOG_ENTRIES.get(i);

            if(!entry.getTimestamp().isBefore(periodStart) && !entry.getTimestamp().isAfter(now)) {
                entries.add(entry);
            }

            if(entries.size() >= MAX_FETCH_ENTRIES) {
                break;
            }
        }

        return entries;
    }

    public static synchronized void addListener(Runnable listener) {
        if(listener == null) {
            return;
        }

        LISTENERS.add(listener);
    }

    public static synchronized void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    private static LocalDateTime getPeriodStart(Period period) {
        LocalDate today = LocalDate.now();

        if(period == null) {
            return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        }

        switch(period) {
            case DAY:
                return today.atStartOfDay();
            case WEEK:
                return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTH:
                return today.withDayOfMonth(1).atStartOfDay();
            default:
                return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        }
    }

    private static void ensureEntriesLoaded() {
        if(entriesLoaded) {
            return;
        }

        entriesLoaded = true;

        if(!Files.exists(STORAGE_FILE)) {
            return;
        }

        try(BufferedReader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
            String line;

            while((line = reader.readLine()) != null) {
                ActivityEntry entry = parseEntryLine(line);
                if(entry != null) {
                    LOG_ENTRIES.add(entry);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        int originalSize = LOG_ENTRIES.size();
        trimToMaxStoredEntries();

        if(LOG_ENTRIES.size() != originalSize) {
            persistEntries();
        }
    }

    private static void trimToMaxStoredEntries() {
        while(LOG_ENTRIES.size() > MAX_STORED_ENTRIES) {
            LOG_ENTRIES.remove(0);
        }
    }

    private static ActivityEntry parseEntryLine(String line) {
        if(line == null || line.trim().isEmpty()) {
            return null;
        }

        int separatorIndex = line.indexOf('|');

        if(separatorIndex <= 0 || separatorIndex >= line.length() - 1) {
            return null;
        }

        try {
            long epochMillis = Long.parseLong(line.substring(0, separatorIndex));
            String encodedDescription = line.substring(separatorIndex + 1);
            String description = new String(Base64.getDecoder().decode(encodedDescription), StandardCharsets.UTF_8);

            if(description.trim().isEmpty()) {
                return null;
            }

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()
            );
            return new ActivityEntry(timestamp, description);
        } catch(Exception ignored) {
            return null;
        }
    }

    private static void persistEntries() {
        try {
            Path parent = STORAGE_FILE.getParent();
            if(parent != null) {
                Files.createDirectories(parent);
            }

            try(BufferedWriter writer = Files.newBufferedWriter(STORAGE_FILE, StandardCharsets.UTF_8)) {
                for(ActivityEntry entry : LOG_ENTRIES) {
                    long epochMillis = entry.getTimestamp()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                    String encodedDescription = Base64.getEncoder().encodeToString(
                        entry.getDescription().getBytes(StandardCharsets.UTF_8)
                    );

                    writer.write(epochMillis + "|" + encodedDescription);
                    writer.newLine();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static void notifyListeners() {
        List<Runnable> listenersCopy = new ArrayList<>(LISTENERS);
        for(Runnable listener : listenersCopy) {
            try {
                listener.run();
            } catch(Exception ignored) {
                // Ignore listener failures so logging remains reliable.
            }
        }
    }
}
