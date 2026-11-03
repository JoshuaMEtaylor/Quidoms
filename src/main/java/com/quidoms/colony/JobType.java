package com.quidoms.colony;

public enum JobType {
    UNEMPLOYED("unemployed"),
    BUILDER("builder");

    private final String id;

    JobType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static JobType byId(String id) {
        for (JobType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return UNEMPLOYED;
    }
}
