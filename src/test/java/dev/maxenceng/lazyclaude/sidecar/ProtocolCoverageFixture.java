package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

public final class ProtocolCoverageFixture {

    private static final String ROOT = "dev.maxenceng.lazyclaude";
    private static final String PRIMARY = ".infrastructure.primary";
    private static final String OPERATION = "%s/%s";

    private ProtocolCoverageFixture() {}

    public static Set<String> operations() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT)
                .stream()
                .filter(type -> type.getPackageName().endsWith(PRIMARY))
                .filter(type -> type.getSimpleName().endsWith("Query") || type.getSimpleName().endsWith("Command"))
                .flatMap(ProtocolCoverageFixture::operationsOf)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private static Stream<String> operationsOf(JavaClass type) {
        String module = type.getPackageName().substring(ROOT.length() + 1).split("\\.")[0];
        return type.getMethods().stream()
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> !method.getModifiers().contains(JavaModifier.STATIC))
                .map(JavaMethod::getName)
                .map(name -> OPERATION.formatted(module, name));
    }

    public static Set<String> protocolMethods() {
        return BoardMethods.signatures().stream().map(ProtocolMethod::name).collect(Collectors.toSet());
    }

    public static List<String> servedByAWatch() {
        return List.of("board/subscribe", "board/boardAfterChange");
    }

    public static List<String> notYetOnTheProtocol() {
        return List.of(
"board/capture",
"docs/docsFor",
"docs/subscribe",
"review/anchorOf",
"review/checking",
"review/comment",
"review/delete",
"review/diffOf",
"review/edit",
"review/loadingDiff",
"review/loadingPending",
"review/loadingThreads",
"review/pendingInDiff",
"review/pendingOf",
"review/refresh",
"review/reply",
"review/submit",
"review/subscribe",
"review/threadsInDiff",
"review/threadsOf",
"session/configuration",
"session/endAll",
"session/followsPointer",
"session/knowTickets",
"session/onScreenChange",
"session/onTicketDoneEnded",
"session/refresh",
"session/remove",
"session/resize",
"session/resume",
"session/resumeInTicketWorktree",
"session/screenOf",
"session/send",
"session/sessionsFor",
"session/startTicket",
"session/startUnlinked",
"session/subscribe",
"session/verbChoiceFor",
"session/watching",
"session/worktreeOf",
"shared/onNoneHeld",
"shared/subscribe",
"shared/threadStopped");
    }
}
