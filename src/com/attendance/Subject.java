package com.attendance;

public class Subject {
    private String name;
    private int classesConducted;
    private int classesAttended;
    private int classesPerWeek; // Used for future predictions

    public Subject(String name, int classesPerWeek) {
        this.name = name;
        this.classesPerWeek = classesPerWeek;
        this.classesConducted = 0;
        this.classesAttended = 0;
    }

    public String getName() {
        return name;
    }

    public int getClassesConducted() {
        return classesConducted;
    }

    public int getClassesAttended() {
        return classesAttended;
    }

    public int getClassesPerWeek() {
        return classesPerWeek;
    }

    public void addClass(boolean attended) {
        this.classesConducted++;
        if (attended) {
            this.classesAttended++;
        }
    }
    
    public void setAttendance(int conducted, int attended) {
        if (attended > conducted) {
            throw new IllegalArgumentException("Attended classes cannot be more than conducted classes.");
        }
        this.classesConducted = conducted;
        this.classesAttended = attended;
    }

    public double getAttendancePercentage() {
        if (classesConducted == 0) return 100.0; // Default to 100% if no classes
        return (double) classesAttended / classesConducted * 100.0;
    }

    @Override
    public String toString() {
        return String.format("%s: %d/%d (%.2f%%)", name, classesAttended, classesConducted, getAttendancePercentage());
    }
}
