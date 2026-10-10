package dev.maxenceng.lazyclaude.docs.published;

public final class DocumentPathFixture {

    private DocumentPathFixture() {}

    public static DocumentPath documentPath() {
        return new DocumentPath("context-map.md");
    }

    public static DocumentPath secondDocumentPath() {
        return new DocumentPath("glossary.md");
    }

    public static DocumentPath adrPath() {
        return new DocumentPath("adr/0001-record.md");
    }

    public static DocumentPath secondAdrPath() {
        return new DocumentPath("adr/0002-enforce.md");
    }

    public static DocumentPath deepPath() {
        return new DocumentPath("adr/superseded/0000-old.md");
    }

    public static DocumentPath researchPath() {
        return new DocumentPath("research/tamboui.md");
    }
}
