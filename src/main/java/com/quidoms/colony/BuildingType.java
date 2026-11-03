package com.quidoms.colony;

public enum BuildingType {
    TOWN_HALL(0, "town_hall"),
    HOUSE(2, "house"),
    BUILDER_HUT(0, "builder_hut"),
    WAREHOUSE(0, "warehouse");

    private final int bedSlots;
    private final String id;

    BuildingType(int bedSlots, String id) {
        this.bedSlots = bedSlots;
        this.id = id;
    }

    public int bedSlots() {
        return bedSlots;
    }

    public String id() {
        return id;
    }

    public static BuildingType byId(String id) {
        for (BuildingType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return HOUSE;
    }
}
