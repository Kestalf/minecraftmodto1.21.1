package io.github.kestalf.futurebackport.block;

import net.minecraft.util.StringRepresentable;

/** {@code copper_golem_pose} of a copper golem statue; the comparator reads {@code ordinal() + 1}. */
public enum CopperGolemPose implements StringRepresentable {
    STANDING("standing"),
    SITTING("sitting"),
    RUNNING("running"),
    STAR("star");

    private final String name;

    CopperGolemPose(String name) {
        this.name = name;
    }

    public CopperGolemPose next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
