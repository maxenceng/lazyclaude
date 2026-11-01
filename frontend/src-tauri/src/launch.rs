//! What the relay runs and where: the jar `make` builds unless the environment names another, in the project directory.

use std::path::PathBuf;

const BUILT_JAR: &str = concat!(env!("CARGO_MANIFEST_DIR"), "/../../target/lazyclaude.jar");

pub fn jar(named: Option<String>) -> PathBuf {
    named
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from(BUILT_JAR))
}

/// The first argument after the program name, else the directory the app was launched from.
pub fn project(first_argument: Option<String>, launch_dir: PathBuf) -> PathBuf {
    first_argument.map(PathBuf::from).unwrap_or(launch_dir)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_environment_names_the_jar_over_the_built_one() {
        assert_eq!(
            jar(Some("/tmp/other.jar".into())),
            PathBuf::from("/tmp/other.jar")
        );
        assert!(jar(None).ends_with("target/lazyclaude.jar"));
    }

    #[test]
    fn the_first_argument_is_the_project_else_the_launch_directory() {
        assert_eq!(
            project(Some("/work/p".into()), "/here".into()),
            PathBuf::from("/work/p")
        );
        assert_eq!(project(None, "/here".into()), PathBuf::from("/here"));
    }
}
