package dev.maxenceng.lazyclaude.docs.published;

public final class DocumentTextFixture {

    private DocumentTextFixture() {}

    public static Content content() {
        return new Content("# Context map\n\nEach context is a package.\n");
    }

    public static Content secondContent() {
        return new Content("# Glossary\n\nThe words.\n");
    }
}
