package com.attendance;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeeklySchedule {
    private Map<DayOfWeek, List<Subject>> timetable;

    public WeeklySchedule() {
        this.timetable = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            timetable.put(day, new ArrayList<>());
        }
    }

    public void addClass(DayOfWeek day, Subject subject) {
        timetable.get(day).add(subject);
    }

    public List<Subject> getSubjectsOn(DayOfWeek day) {
        return timetable.get(day);
    }

    public int getClassesCountFor(Subject subject) {
        int count = 0;
        for (List<Subject> daySubjects : timetable.values()) {
            for (Subject s : daySubjects) {
                if (s.equals(subject)) {
                    count++;
                }
            }
        }
        return count;
    }
}
