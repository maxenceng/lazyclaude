package dev.maxenceng.lazyclaude.docs.published;

public final class FolderPathFixture {

    private FolderPathFixture() {}

    public static FolderPath adrFolderPath() {
        return new FolderPath("adr");
    }

    public static FolderPath researchFolderPath() {
        return new FolderPath("research");
    }

    public static FolderPath supersededFolderPath() {
        return new FolderPath("adr/superseded");
    }
}
