package dev.maxenceng.lazyclaude.session.published;

public final class ResidenceNameFixture {

    private ResidenceNameFixture() {}

    public static ResidenceName kittyName() {
        return new ResidenceName("kitty");
    }

    public static ResidenceName tmuxName() {
        return new ResidenceName("tmux");
    }
}
