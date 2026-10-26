package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.Chord;

public final class ClaudeKeybindingsDocsFixture {

    private static final URI DOCUMENTATION = URI.create("https://code.claude.com/docs/en/keybindings.md");
    private static final Duration PATIENCE = Duration.ofSeconds(30);
    private static final Pattern ACTION_ROW = Pattern.compile("^\\| `[^`|]+` \\| ([^|]*) \\|.*$", Pattern.MULTILINE);
    private static final Pattern ALTERNATIVE_SEPARATOR = Pattern.compile(",| / |[()]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String FOOTNOTE = "\\*";
    private static final String ESCAPED = "\\";
    private static final int HTTP_OK = 200;

    private ClaudeKeybindingsDocsFixture() {}

    public static List<Chord> defaultBindingsInTheDocumentation() throws IOException, InterruptedException {
        return ACTION_ROW.matcher(documentation()).results()
                .map(row -> row.group(1))
                .flatMap(ALTERNATIVE_SEPARATOR::splitAsStream)
                .map(alternative -> alternative.replace(FOOTNOTE, "").replace(ESCAPED, "").strip())
                .filter(alternative -> !alternative.isEmpty())
                .map(ClaudeKeybindingsDocsFixture::firstKeystroke)
                .flatMap(Optional::stream)
                .flatMap(keystroke -> ChordSpelling.boundByClaude(keystroke).stream())
                .toList();
    }

    public static long rowsInTheDocumentation() throws IOException, InterruptedException {
        return ACTION_ROW.matcher(documentation()).results().count();
    }

    private static Optional<String> firstKeystroke(String alternative) {
        return Stream.of(WHITESPACE.split(alternative)).findFirst();
    }

    private static String documentation() throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(PATIENCE)
                .build()
                .send(HttpRequest.newBuilder(DOCUMENTATION).timeout(PATIENCE).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HTTP_OK) {
            throw new IOException("%s answered %d".formatted(DOCUMENTATION, response.statusCode()));
        }
        return response.body();
    }
}
