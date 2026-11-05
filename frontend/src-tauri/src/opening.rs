//! Which paths the window may hand the system opener: those under the project directory, as it resolves on disk.

use std::path::Path;

use glob::Pattern;

/// The opener checks a path after resolving its symlinks and `..`, so the project is resolved the same way, and
/// escaped, since a directory name may hold a glob's `*`, `?` or `[`.
pub fn scope(project: &Path) -> String {
    let resolved = project
        .canonicalize()
        .unwrap_or_else(|_| project.to_path_buf());
    format!("{}/**", Pattern::escape(&resolved.to_string_lossy()))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    fn allows(scope: &str, path: &str) -> bool {
        Pattern::new(scope).unwrap().matches(path)
    }

    #[test]
    fn a_path_under_the_project_is_allowed_and_one_beside_it_is_not() {
        let scope = scope(Path::new("/work/no-such-project"));
        assert!(allows(&scope, "/work/no-such-project/src/main.rs"));
        assert!(!allows(&scope, "/work/other/src/main.rs"));
        assert!(!allows(&scope, "/etc/passwd"));
    }

    #[test]
    fn a_project_named_with_glob_characters_allows_only_itself() {
        let scope = scope(Path::new("/work/p[ab]*"));
        assert!(allows(&scope, "/work/p[ab]*/notes.txt"));
        assert!(!allows(&scope, "/work/pa/notes.txt"));
    }

    #[test]
    fn the_project_is_resolved_through_its_symlinks() {
        let base = std::env::temp_dir().join(format!("lazyclaude-opening-{}", std::process::id()));
        let real = base.join("real");
        let link = base.join("link");
        std::fs::create_dir_all(&real).unwrap();
        let _ = std::fs::remove_file(&link);
        std::os::unix::fs::symlink(&real, &link).unwrap();
        let resolved: PathBuf = real.canonicalize().unwrap();
        let scope = scope(&link);
        std::fs::remove_dir_all(&base).unwrap();
        assert!(allows(&scope, &resolved.join("a.txt").to_string_lossy()));
    }
}
