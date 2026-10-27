package io.github.kestalf.futurebackport.block;

import net.minecraft.util.StringRepresentable;

/** {@code creaking_heart_state}: no matching logs, logs present during the day, logs present at night. */
public enum CreakingHeartState implements StringRepresentable {
    UPROOTED("uprooted"),
    DORMANT("dormant"),
    AWAKE("awake");

    private final String name;

    CreakingHeartState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
