# One verb surface for humans, hooks, CI and coding agents, so nothing has to
# reconstruct a build invocation from memory.

SHELL := /bin/bash
MVN   := ./mvnw -B
JAR   := target/lazyclaude.jar

# Keep an already-correct JAVA_HOME (CI sets one), else find a local JDK 25. A JRE is
# skipped because it looks fine until compilation starts.
JAVAC_OK := $(shell [ -x "$(JAVA_HOME)/bin/javac" ] && "$(JAVA_HOME)/bin/javac" -version 2>&1 | grep -q '^javac 25' && echo yes)
ifneq ($(JAVAC_OK),yes)
JDK25 := $(shell for d in "$$HOME"/.jdks/jdk-25* /usr/lib/jvm/*25*; do [ -x "$$d/bin/javac" ] && echo "$$d" && break; done)
ifneq ($(strip $(JDK25)),)
export JAVA_HOME := $(JDK25)
endif
endif
JAVA := $(if $(JAVA_HOME),$(JAVA_HOME)/bin/java,java)

.DEFAULT_GOAL := help

.PHONY: help doctor build test contract test-one arch fmt lint dup config-check adr-check
.PHONY: ticket-check guard-fixtures guard-harness no-skipped-tests test-comment-check var-check
.PHONY: package run ci verify verify-report pr-checks clean

help: ## Show available targets
	@grep -hE '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

doctor: ## Check the toolchain before blaming the build
	@bash scripts/doctor.sh

build: ## Compile main sources
	$(MVN) compile

test: ## Run every test, architecture rules included
	$(MVN) test

contract: ## Check a live hosted `claude`'s registry record, its keybindings and `gh`; run by hand after a Claude Code or gh upgrade
	$(MVN) test -Dgroups=contract -DexcludedGroups= -Dsurefire.failIfNoSpecifiedTests=false

test-one: ## Run one test class or method, e.g. make test-one T=SomeTest
	@[ -n "$(T)" ] || { echo "T is unset: make test-one T=SomeTest"; exit 1; }
	$(MVN) test -Dtest='$(T)'

arch: ## Run the architecture rules only
	$(MVN) test -Dtest='ArchitectureTest' -DfailIfNoSpecifiedTests=false

fmt: ## Apply formatting
	$(MVN) spotless:apply

lint: ## Verify formatting without touching files
	$(MVN) spotless:check

dup: ## Fail on copy-pasted code
	$(MVN) pmd:cpd-check

rules: ## Fail on a source rule ArchUnit cannot see: a for loop, a magic number
	$(MVN) pmd:check checkstyle:check

config-check: ## Fail on a config comment that explains the tool rather than the choice
	@python3 scripts/check-config-comments.py

adr-check: ## Fail on a duplicate or missing ADR number
	@python3 scripts/check-adr-numbers.py

test-comment-check: ## Fail on a comment under src/test, where the test name is the explanation
	@python3 scripts/check-test-comments.py

var-check: ## Fail on `var` where a type should be written out
	@python3 scripts/check-no-var.py

ticket-check: ## Fail on a reference to a ticket that has already landed
	@python3 scripts/check-ticket-references.py

guard-fixtures: ## Fail if a scripts/check-* guard has no fixture proven to trip it
	@python3 scripts/guard-fixtures.py

guard-harness: ## Fail if guard-fixtures.py stops detecting a fixture that proves nothing
	@python3 scripts/prove-guard-harness.py

no-skipped-tests: ## Fail if the last test run skipped anything
	@python3 scripts/check-no-skipped-tests.py

package: ## Build the runnable jar into target/lazyclaude.jar
	$(MVN) -q package -DskipTests

run: package ## Start the TUI in this terminal
	@$(JAVA) --enable-native-access=ALL-UNNAMED -jar $(JAR)

ci: lint config-check adr-check ticket-check test-comment-check var-check guard-fixtures guard-harness test dup rules ## CI's first step, cheapest check first

# CI runs these targets in this order. Adding a step to one without the other is what
# makes "it passed locally" stop meaning anything.
verify: ## Everything CI runs, in the order CI runs it
	$(MAKE) ci
	$(MAKE) no-skipped-tests
	$(MAKE) package

verify-report: ## Run verify into a log and print the verdict, exit code included
	@bash scripts/verify-report.sh

pr-checks: ## Wait for CI on this branch's PR and exit on its verdict
	@bash scripts/pr-checks.sh

clean: ## Remove build output
	$(MVN) clean
