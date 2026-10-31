package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import dev.maxenceng.lazyclaude.ApplicationModule;
import dev.maxenceng.lazyclaude.ModuleType;

public final class ProtocolCoverageFixture {

    private static final String ROOT = "dev.maxenceng.lazyclaude";
    private static final String PRIMARY = ".infrastructure.primary";
    private static final String PACKAGE_INFO = ".package-info";
    private static final String OPERATION = "%s/%s";
    private static final String QUERY = "Query";
    private static final String COMMAND = "Command";

    private ProtocolCoverageFixture() {}

    public static List<String> operations() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT)
                .stream()
                .filter(type -> type.getPackageName().endsWith(PRIMARY))
                .filter(type -> type.getSimpleName().endsWith(QUERY) || type.getSimpleName().endsWith(COMMAND))
                .flatMap(ProtocolCoverageFixture::operationsOf)
                .sorted()
                .toList();
    }

    private static Stream<String> operationsOf(JavaClass type) {
        String prefix = prefixOf(type);
        return type.getMethods().stream()
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> !method.getModifiers().contains(JavaModifier.STATIC))
                .map(JavaMethod::getName)
                .map(name -> OPERATION.formatted(prefix, name));
    }

    private static String prefixOf(JavaClass type) {
        String module = type.getPackageName().substring(ROOT.length() + 1).split("\\.")[0];
        if (kindOf(module) == ModuleType.CONTEXT) {
            return module;
        }
        String role = type.getSimpleName().replaceFirst(QUERY + "$|" + COMMAND + "$", "");
        return role.substring(0, 1).toLowerCase(Locale.ROOT) + role.substring(1);
    }

    private static ModuleType kindOf(String module) {
        try {
            return Class.forName(ROOT + "." + module + PACKAGE_INFO).getAnnotation(ApplicationModule.class).type();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(module + " declares no kind", e);
        }
    }

    public static Set<String> protocolMethods() {
        return Sidecar.protocolMethods().stream().map(ProtocolMethod::name).collect(Collectors.toSet());
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
"warning/onNoneHeld",
"warning/subscribe",
"warning/threadStopped");
    }
}
