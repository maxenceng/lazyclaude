package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.otherWarning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;

import java.util.List;

public final class WarningsFixture {

    private WarningsFixture() {}

    public static Warnings warnings() {
        return new Warnings(List.of(warning()));
    }

    public static Warnings twoWarnings() {
        return new Warnings(List.of(warning(), otherWarning()));
    }
}
