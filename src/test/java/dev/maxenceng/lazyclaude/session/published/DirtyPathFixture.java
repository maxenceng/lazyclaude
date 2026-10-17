package dev.maxenceng.lazyclaude.session.published;

public final class DirtyPathFixture {

    private DirtyPathFixture() {}

    public static DirtyPath dirtyPath() {
        return new DirtyPath("src/Foo.java");
    }

    public static DirtyPath secondDirtyPath() {
        return new DirtyPath("notes.txt");
    }
}
