package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

public final class UnifiedDiffFixture {

    private UnifiedDiffFixture() {}

    public static String modifiedFileDiff() {
        return """
                diff --git a/src/Board.java b/src/Board.java
                index 111..222 100644
                --- a/src/Board.java
                +++ b/src/Board.java
                @@ -2,4 +2,5 @@ class Board {
                 keep
                -old
                +new
                +extra
                 end
                """;
    }

    public static String addedFileDiff() {
        return """
                diff --git a/src/Docs.java b/src/Docs.java
                new file mode 100644
                index 0000000..222
                --- /dev/null
                +++ b/src/Docs.java
                @@ -0,0 +1,2 @@
                +first
                +second
                """;
    }

    public static String deletedFileDiff() {
        return """
                diff --git a/src/Docs.java b/src/Docs.java
                deleted file mode 100644
                index 111..0000000
                --- a/src/Docs.java
                +++ /dev/null
                @@ -1,2 +0,0 @@
                -first
                -second
                """;
    }

    public static String renamedFileDiff() {
        return """
                diff --git a/src/Old.java b/src/New.java
                similarity index 60%
                rename from src/Old.java
                rename to src/New.java
                index 111..222 100644
                --- a/src/Old.java
                +++ b/src/New.java
                @@ -1,2 +1,2 @@
                 keep
                -old
                +new
                """;
    }

    public static String renamedFileWithoutChangesDiff() {
        return """
                diff --git a/src/Old.java b/src/New.java
                similarity index 100%
                rename from src/Old.java
                rename to src/New.java
                """;
    }

    public static String binaryFileDiff() {
        return """
                diff --git a/src/Board.java b/src/Board.java
                index 111..222 100644
                Binary files a/src/Board.java and b/src/Board.java differ
                """;
    }

    public static String firstLineDiff() {
        return """
                diff --git a/src/Board.java b/src/Board.java
                index 111..222 100644
                --- a/src/Board.java
                +++ b/src/Board.java
                @@ -1 +1 @@
                -old
                \\ No newline at end of file
                +new
                \\ No newline at end of file
                """;
    }

    public static String twoHunksDiff() {
        return """
                diff --git a/src/Board.java b/src/Board.java
                index 111..222 100644
                --- a/src/Board.java
                +++ b/src/Board.java
                @@ -2,2 +2,2 @@
                 one
                +two
                @@ -40,2 +41,2 @@
                -three
                 four
                """;
    }

    public static String blankContextDiff() {
        return """
                diff --git a/src/Board.java b/src/Board.java
                index 111..222 100644
                --- a/src/Board.java
                +++ b/src/Board.java
                @@ -1,3 +1,3 @@
                 a

                --- not a header
                +++ nor this
                """;
    }

    public static String twoFilesDiff() {
        return modifiedFileDiff() + addedFileDiff();
    }

    public static String hunkHeaderWithoutNumbers() {
        return """
                diff --git a/src/Board.java b/src/Board.java
                --- a/src/Board.java
                +++ b/src/Board.java
                @@ nonsense @@
                +x
                """;
    }
}
