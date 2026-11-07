//! What the relay runs and where: the jar `make` builds unless the environment names another, in the project directory.

use std::path::{Path, PathBuf};

use crate::relay::Sidecar;

const BUILT_JAR: &str = concat!(env!("CARGO_MANIFEST_DIR"), "/../../target/lazyclaude.jar");

/// A relative name is read from the launch directory, since the sidecar runs in the project directory.
pub fn jar(named: Option<String>, launch_dir: &Path) -> PathBuf {
    named
        .map(|name| launch_dir.join(name))
        .unwrap_or_else(|| PathBuf::from(BUILT_JAR))
}

/// The sidecar to run: the environment's jar on the system java in any build, else the built jar on it in a debug build, else the bundled runtime.
/// A debug build keeps the system java because `tauri dev` and `cargo test` have no bundled resources, and listing them in `tauri.conf.json` would copy the runtime into every build.
pub fn sidecar(
    named: Option<String>,
    launch_dir: &Path,
    debug: bool,
    resource_dir: Result<PathBuf, String>,
    project: PathBuf,
) -> Sidecar {
    if named.is_some() || debug {
        return Sidecar::java(jar(named, launch_dir), project);
    }
    match resource_dir {
        Ok(resources) => Sidecar::bundled(&resources, project),
        Err(reason) => Sidecar::unavailable(
            &format!("resource directory unavailable: {reason}"),
            project,
        ),
    }
}

/// The directory the app was launched from: the AppImage runtime's `OWD`, since its `AppRun` moves into the AppImage before the relay starts, else the current one.
pub fn launch_dir(original: Option<String>, current: PathBuf) -> PathBuf {
    original
        .filter(|directory| !directory.is_empty())
        .map(PathBuf::from)
        .unwrap_or(current)
}

/// The first argument after the program name, read from the launch directory when relative, else the launch directory.
pub fn project(first_argument: Option<String>, launch_dir: PathBuf) -> PathBuf {
    first_argument
        .map(|argument| launch_dir.join(argument))
        .unwrap_or(launch_dir)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_environment_names_the_jar_over_the_built_one() {
        assert_eq!(
            jar(Some("/tmp/other.jar".into()), Path::new("/here")),
            PathBuf::from("/tmp/other.jar")
        );
        assert!(jar(None, Path::new("/here")).ends_with("target/lazyclaude.jar"));
    }

    #[test]
    fn a_relative_jar_is_read_from_the_launch_directory() {
        assert_eq!(
            jar(Some("target/other.jar".into()), Path::new("/here")),
            PathBuf::from("/here/target/other.jar")
        );
    }

    #[test]
    fn the_first_argument_is_the_project_else_the_launch_directory() {
        assert_eq!(
            project(Some("/work/p".into()), "/here".into()),
            PathBuf::from("/work/p")
        );
        assert_eq!(project(None, "/here".into()), PathBuf::from("/here"));
    }

    #[test]
    fn a_relative_project_argument_is_read_from_the_launch_directory() {
        assert_eq!(
            project(Some("p".into()), "/here".into()),
            PathBuf::from("/here/p")
        );
    }

    #[test]
    fn an_appimage_is_launched_from_its_original_directory_not_the_one_apprun_moved_to() {
        assert_eq!(
            launch_dir(Some("/work/p".into()), "/tmp/.mount_x/usr".into()),
            PathBuf::from("/work/p")
        );
        assert_eq!(
            launch_dir(None, "/work/p".into()),
            PathBuf::from("/work/p")
        );
        assert_eq!(
            launch_dir(Some(String::new()), "/work/p".into()),
            PathBuf::from("/work/p")
        );
    }

    const RES: &str = "/usr/lib/lazyclaude";

    fn chosen(env: Option<&str>, debug: bool) -> String {
        sidecar(
            env.map(String::from),
            Path::new("/here"),
            debug,
            Ok(PathBuf::from(RES)),
            PathBuf::from("/work/p"),
        )
        .describe()
    }

    #[test]
    fn a_release_build_starts_the_bundled_runtime_with_its_natives() {
        assert_eq!(
            chosen(None, false),
            "/usr/lib/lazyclaude/runtime/bin/java \
             -Dpty4j.preferred.native.folder=/usr/lib/lazyclaude/natives \
             -Djna.boot.library.path=/usr/lib/lazyclaude/natives \
             -Djna.nounpack=true \
             --enable-native-access=ALL-UNNAMED -jar /usr/lib/lazyclaude/lazyclaude.jar --sidecar"
        );
    }

    #[test]
    fn the_environment_wins_over_the_bundle_in_a_release_build() {
        assert_eq!(
            chosen(Some("/tmp/other.jar"), false),
            "java --enable-native-access=ALL-UNNAMED -jar /tmp/other.jar --sidecar"
        );
    }

    #[test]
    fn a_debug_build_keeps_the_system_java_and_the_built_jar() {
        let described = chosen(None, true);
        assert!(described.starts_with("java --enable-native-access=ALL-UNNAMED -jar "));
        assert!(described.ends_with("target/lazyclaude.jar --sidecar"));
    }

    #[test]
    fn an_unresolved_resource_directory_is_a_sidecar_that_exits_with_why() {
        let sidecar = sidecar(
            None,
            Path::new("/here"),
            false,
            Err("no resources".into()),
            PathBuf::from("/work/p"),
        );
        assert_eq!(
            sidecar.describe(),
            "resource directory unavailable: no resources"
        );
    }
}
