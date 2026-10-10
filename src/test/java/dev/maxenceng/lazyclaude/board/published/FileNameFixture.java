package dev.maxenceng.lazyclaude.board.published;

public final class FileNameFixture {

    private FileNameFixture() {}

    public static FileName fileName() {
        return new FileName("PROJ-1.md");
    }

    public static FileName secondFileName() {
        return new FileName("PROJ-2.md");
    }
}
