package dev.maxenceng.lazyclaude.board.published;

public final class FileNameFixture {

    private FileNameFixture() {}

    public static FileName fileName() {
        return new FileName("PROJ-1.md");
    }

    public static FileName secondFileName() {
        return new FileName("PROJ-2.md");
    }

    public static FileName slugFileName() {
        return new FileName("PROJ-1-first-slice.md");
    }

    public static FileName dottedFileName() {
        return new FileName("PROJ-1-v1.2-notes.md");
    }

    public static FileName doubleExtensionFileName() {
        return new FileName("PROJ-1.md.md");
    }
}
