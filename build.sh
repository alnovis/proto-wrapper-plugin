#!/bin/bash
#
# Proto-Wrapper Plugin Build Script
#
# DAG-based build system with parallel execution support.
#
# Usage:
#   ./build.sh              Full build + all tests
#   ./build.sh --quick      Build without tests
#   ./build.sh --parallel   Enable parallel execution
#   ./build.sh -j N         Limit parallel jobs (default: unlimited)
#   ./build.sh --target X   Build specific target (core, maven, gradle, tests, examples, all)
#   ./build.sh --graph      Display module dependency graph
#   ./build.sh --list       List all modules
#   ./build.sh --dry-run    Show what would be executed
#   ./build.sh --check      Check version consistency
#   ./build.sh --bump X.Y.Z Update version in all files
#   ./build.sh --version    Show current version
#   ./build.sh --ci         CI-friendly output (no colors/spinners)
#   ./build.sh --help       Show this help
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ============================================================================
# Configuration
# ============================================================================

CI_MODE=false
PARALLEL_MODE=false
DRY_RUN=false
SKIP_TESTS=false
TARGET="all"
MAX_JOBS=0  # 0 = unlimited

# ============================================================================
# Module Registry (Associative Arrays - requires bash 4+)
# ============================================================================

declare -A MODULE_DEPS      # module -> dependencies (comma-separated)
declare -A MODULE_SYSTEM    # module -> maven|gradle|gradle-standalone
declare -A MODULE_CMD       # module -> build command
declare -A MODULE_CMD_TEST  # module -> test command (optional)
declare -A MODULE_DESC      # module -> description
declare -A MODULE_DIR       # module -> working directory
declare -A MODULE_STATUS    # module -> pending|running|success|failed|skipped
declare -A MODULE_TAGS      # module -> tags for filtering (build,test,example)
declare -A MODULE_TIME      # module -> build time in seconds
declare -A MODULE_START     # module -> start timestamp

MODULES_ORDER=()  # Topologically sorted list
SPINNER_IDX=0     # Global spinner index for animation

# ============================================================================
# Module Registration
# ============================================================================

register_module() {
    local id="$1"
    local deps="$2"
    local system="$3"
    local cmd="$4"
    local desc="$5"
    local dir="${6:-.}"
    local tags="${7:-build}"
    local cmd_test="${8:-}"

    MODULE_DEPS[$id]="$deps"
    MODULE_SYSTEM[$id]="$system"
    MODULE_CMD[$id]="$cmd"
    MODULE_CMD_TEST[$id]="$cmd_test"
    MODULE_DESC[$id]="$desc"
    MODULE_DIR[$id]="$dir"
    MODULE_STATUS[$id]="pending"
    MODULE_TAGS[$id]="$tags"
}

init_modules() {
    # === CORE ===
    register_module "core" "" "maven" \
        "mvn clean install -pl proto-wrapper-core -DskipTests -q" \
        "Core library (analysis, merging, models)" \
        "." \
        "build,maven" \
        "mvn test -pl proto-wrapper-core -q"

    # === PLUGINS ===
    register_module "maven-plugin" "core" "maven" \
        "mvn clean install -pl proto-wrapper-maven-plugin -DskipTests -q" \
        "Maven plugin" \
        "." \
        "build,maven" \
        "mvn test -pl proto-wrapper-maven-plugin -q"

    register_module "spring-starter" "core" "maven" \
        "mvn clean install -pl proto-wrapper-spring-boot-starter -DskipTests -q" \
        "Spring Boot starter" \
        "." \
        "build,maven" \
        "mvn test -pl proto-wrapper-spring-boot-starter -q"

    register_module "gradle-plugin" "core" "gradle" \
        "./gradlew :proto-wrapper-gradle-plugin:build -x test -q" \
        "Gradle plugin" \
        "." \
        "build,gradle" \
        "./gradlew :proto-wrapper-gradle-plugin:test -q"

    # === TESTS ===
    register_module "golden-tests" "maven-plugin" "maven" \
        "mvn compile -pl proto-wrapper-golden-tests -q" \
        "Golden tests (all field variations)" \
        "." \
        "test,maven" \
        "mvn verify -pl proto-wrapper-golden-tests -q"

    register_module "maven-int-tests" "maven-plugin" "maven" \
        "mvn compile -pl proto-wrapper-maven-integration-tests -q" \
        "Maven integration tests" \
        "." \
        "test,maven" \
        "mvn verify -pl proto-wrapper-maven-integration-tests -q"

    register_module "gradle-int-tests" "gradle-plugin" "gradle-standalone" \
        "gradle classes -q" \
        "Gradle integration tests" \
        "proto-wrapper-gradle-integration-tests" \
        "test,gradle" \
        "gradle test -q"

    # === EXAMPLES ===
    register_module "maven-example" "maven-plugin" "maven" \
        "mvn compile -f examples/maven-example/pom.xml -q" \
        "Maven example project" \
        "." \
        "example,maven" \
        "mvn verify -f examples/maven-example/pom.xml -q"

    register_module "gradle-example" "gradle-plugin" "gradle-standalone" \
        "gradle classes -q" \
        "Gradle example project" \
        "examples/gradle-example" \
        "example,gradle" \
        "gradle build -q"

    register_module "spring-example" "spring-starter,maven-plugin" "maven" \
        "mvn compile -f examples/spring-boot-example/pom.xml -q" \
        "Spring Boot example" \
        "." \
        "example,maven" \
        "mvn verify -f examples/spring-boot-example/pom.xml -q"
}

# ============================================================================
# Color and Output
# ============================================================================

setup_colors() {
    if [[ "$CI_MODE" == "true" ]] || [[ ! -t 1 ]]; then
        RED="" GREEN="" YELLOW="" BLUE="" CYAN="" BOLD="" RESET="" DIM=""
        CHECK="[OK]" CROSS="[FAIL]" WAITING="[ ]" RUNNING="[*]"
    else
        RED="\033[0;31m" GREEN="\033[0;32m" YELLOW="\033[0;33m"
        BLUE="\033[0;34m" CYAN="\033[0;36m" BOLD="\033[1m"
        RESET="\033[0m" DIM="\033[2m"
        CHECK="*" CROSS="!" WAITING="○"
    fi
}

print_header() {
    local title="$1"
    echo ""
    echo -e "${BOLD}$title${RESET}"
    local len=${#title}
    printf '%*s\n' "$len" '' | tr ' ' '='
}

print_section() {
    echo ""
    echo -e "${BLUE}${BOLD}=== $1 ===${RESET}"
}

print_success() { echo -e "  ${GREEN}${CHECK}${RESET} $1"; }
print_error() { echo -e "  ${RED}${CROSS}${RESET} $1"; }
print_warning() { echo -e "  ${YELLOW}!${RESET} $1"; }
print_info() { echo -e "$1"; }
print_dim() { echo -e "  ${DIM}$1${RESET}"; }

# ============================================================================
# Status Board Rendering
# ============================================================================

SPINNER_CHARS='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
STATUS_BOARD_LINES=0

# Get waiting dependency for a module
get_waiting_dep() {
    local module="$1"
    IFS=',' read -ra deps <<< "${MODULE_DEPS[$module]}"
    for dep in "${deps[@]}"; do
        [[ -z "$dep" ]] && continue
        local status="${MODULE_STATUS[$dep]}"
        if [[ "$status" != "success" && "$status" != "skipped" ]]; then
            echo "$dep"
            return 0
        fi
    done
    return 0
}

# Render single module status line
render_module_line() {
    local module="$1"
    local status="${MODULE_STATUS[$module]}"

    # Only show modules that are in target cache
    [[ -z "${TARGET_MODULES_CACHE[$module]}" ]] && return 0

    case "$status" in
        success)
            local time="${MODULE_TIME[$module]:-0}"
            echo -e "  ${GREEN}${CHECK}${RESET} ${module} ${DIM}(${time}s)${RESET}"
            ;;
        failed)
            echo -e "  ${RED}${CROSS}${RESET} ${module}"
            ;;
        running)
            local spinner_char="${SPINNER_CHARS:SPINNER_IDX%${#SPINNER_CHARS}:1}"
            echo -e "  ${CYAN}${spinner_char}${RESET} ${module}"
            ;;
        pending)
            local waiting_for=$(get_waiting_dep "$module")
            if [[ -n "$waiting_for" ]]; then
                echo -e "  ${DIM}${WAITING} ${module} (waiting: ${waiting_for})${RESET}"
            else
                echo -e "  ${DIM}${WAITING} ${module}${RESET}"
            fi
            ;;
    esac
    return 0
}

# Render full status board (updates in place)
render_status_board() {
    local m  # Local loop variable to avoid clobbering outer scope

    # Move cursor up to overwrite previous board
    if [[ $STATUS_BOARD_LINES -gt 0 ]]; then
        printf "\033[%dA" "$STATUS_BOARD_LINES" || true
    fi

    # Count lines we'll print
    local line_count=0

    # Print status for each module in order
    for m in "${MODULES_ORDER[@]}"; do
        [[ -z "${TARGET_MODULES_CACHE[$m]}" ]] && continue
        printf "\033[K" || true  # Clear line
        render_module_line "$m" || true
        ((line_count++)) || true
    done

    STATUS_BOARD_LINES=$line_count
    ((SPINNER_IDX++)) || true
    return 0
}

# ============================================================================
# DAG Operations
# ============================================================================

# Global cache for target modules (includes target + all dependencies)
declare -A TARGET_MODULES_CACHE

# Get all transitive dependencies of a module (recursive)
get_transitive_deps() {
    local module="$1"
    local -A visited
    local -a result=()

    _collect_deps() {
        local m="$1"
        [[ -n "${visited[$m]}" ]] && return
        visited[$m]=1

        IFS=',' read -ra deps <<< "${MODULE_DEPS[$m]}"
        for dep in "${deps[@]}"; do
            [[ -z "$dep" ]] && continue
            _collect_deps "$dep"
        done
        result+=("$m")
    }

    _collect_deps "$module"
    echo "${result[@]}"
}

# Compute modules needed for target (including dependencies)
compute_target_modules() {
    local target="$1"
    TARGET_MODULES_CACHE=()

    case "$target" in
        all|build|test)
            # Include all modules
            for m in "${!MODULE_DEPS[@]}"; do
                TARGET_MODULES_CACHE[$m]=1
            done
            ;;
        tests|examples|maven|gradle)
            # Include matching modules + their dependencies
            for m in "${!MODULE_DEPS[@]}"; do
                if module_matches_category "$m" "$target"; then
                    local deps_str=$(get_transitive_deps "$m")
                    read -ra deps <<< "$deps_str"
                    for d in "${deps[@]}"; do
                        TARGET_MODULES_CACHE[$d]=1
                    done
                fi
            done
            ;;
        *)
            # Specific module - include it + all dependencies
            # Check if key exists (not if value is non-empty)
            if [[ -v MODULE_DEPS[$target] ]]; then
                local deps_str=$(get_transitive_deps "$target")
                read -ra deps <<< "$deps_str"
                for d in "${deps[@]}"; do
                    TARGET_MODULES_CACHE[$d]=1
                done
            fi
            ;;
    esac
}

# Check if module matches a category (without dependency expansion)
module_matches_category() {
    local module="$1"
    local category="$2"
    local tags="${MODULE_TAGS[$module]}"
    local system="${MODULE_SYSTEM[$module]}"

    case "$category" in
        tests) [[ "$tags" == *"test"* ]] && return 0 ;;
        examples) [[ "$tags" == *"example"* ]] && return 0 ;;
        maven) [[ "$system" == "maven" ]] && return 0 ;;
        gradle) [[ "$system" == "gradle"* ]] && return 0 ;;
    esac
    return 1
}

# Topological sort using Kahn's algorithm
topo_sort() {
    local -A in_degree
    local -a queue=()
    local -a result=()

    # Initialize in-degrees
    for module in "${!MODULE_DEPS[@]}"; do
        in_degree[$module]=0
    done

    # Calculate in-degrees
    for module in "${!MODULE_DEPS[@]}"; do
        IFS=',' read -ra deps <<< "${MODULE_DEPS[$module]}"
        for dep in "${deps[@]}"; do
            [[ -n "$dep" ]] && ((in_degree[$module]++)) || true
        done
    done

    # Find root modules (in_degree = 0)
    for module in "${!in_degree[@]}"; do
        [[ ${in_degree[$module]} -eq 0 ]] && queue+=("$module")
    done

    # Process queue
    while [[ ${#queue[@]} -gt 0 ]]; do
        local current="${queue[0]}"
        queue=("${queue[@]:1}")
        result+=("$current")

        # Decrease in-degree of dependents
        for module in "${!MODULE_DEPS[@]}"; do
            IFS=',' read -ra deps <<< "${MODULE_DEPS[$module]}"
            for dep in "${deps[@]}"; do
                if [[ "$dep" == "$current" ]]; then
                    ((in_degree[$module]--)) || true
                    [[ ${in_degree[$module]} -eq 0 ]] && queue+=("$module")
                    break
                fi
            done
        done
    done

    MODULES_ORDER=("${result[@]}")
}

# Get modules that can run now (all deps satisfied)
get_ready_modules() {
    local target="$1"
    local -a ready=()

    for module in "${!MODULE_STATUS[@]}"; do
        [[ "${MODULE_STATUS[$module]}" != "pending" ]] && continue

        # Check if module matches target filter
        if ! module_matches_target "$module" "$target"; then
            MODULE_STATUS[$module]="skipped"
            continue
        fi

        # Check if all dependencies are satisfied
        local deps_satisfied=true
        IFS=',' read -ra deps <<< "${MODULE_DEPS[$module]}"
        for dep in "${deps[@]}"; do
            [[ -z "$dep" ]] && continue
            local dep_status="${MODULE_STATUS[$dep]}"
            if [[ "$dep_status" == "failed" ]]; then
                MODULE_STATUS[$module]="skipped"
                deps_satisfied=false
                break
            elif [[ "$dep_status" != "success" && "$dep_status" != "skipped" ]]; then
                deps_satisfied=false
                break
            fi
        done

        [[ "$deps_satisfied" == "true" ]] && ready+=("$module")
    done

    echo "${ready[@]}"
}

module_matches_target() {
    local module="$1"
    local target="$2"

    # Use precomputed cache
    [[ -n "${TARGET_MODULES_CACHE[$module]}" ]] && return 0
    return 1
}

# ============================================================================
# Execution Engine
# ============================================================================

# Log directory (set by scheduler, used by executor)
LOG_DIR=""

# Run a single module command (low-level)
run_module_command() {
    local module="$1"
    local log_file="$2"
    local run_tests="$3"
    local dir="${MODULE_DIR[$module]}"
    local cmd="${MODULE_CMD[$module]}"

    # If running tests and test command exists, use it
    if [[ "$run_tests" == "true" && -n "${MODULE_CMD_TEST[$module]}" ]]; then
        cmd="${MODULE_CMD_TEST[$module]}"
    fi

    # Change to module directory if needed
    if [[ "$dir" != "." ]]; then
        pushd "$dir" > /dev/null 2>&1 || return 1
    fi

    # Execute command
    eval "$cmd" > "$log_file" 2>&1
    local exit_code=$?

    if [[ "$dir" != "." ]]; then
        popd > /dev/null 2>&1 || true
    fi

    return $exit_code
}

# Execution state
EXECUTOR_FAILED_MODULE=""
BUILD_HAS_FAILURE=false

# Start a single module in background (non-blocking)
start_module() {
    local module="$1"
    local log="$LOG_DIR/$module"

    MODULE_STATUS[$module]="running"
    MODULE_START[$module]=$(date +%s)

    # Subprocess: run, write exit code, end time, done marker
    (
        run_module_command "$module" "${log}.log" "$RUN_TESTS"
        echo $? > "${log}.exit"
        date +%s > "${log}.end"
        touch "${log}.done"
    ) &
}

# Check all running modules for completion, update statuses
check_completions() {
    for module in "${!MODULE_STATUS[@]}"; do
        [[ "${MODULE_STATUS[$module]}" != "running" ]] && continue

        local log="$LOG_DIR/$module"
        [[ ! -f "${log}.done" ]] && continue

        # Module finished
        local exit_code=$(cat "${log}.exit" 2>/dev/null || echo "1")
        local end_time=$(cat "${log}.end" 2>/dev/null || date +%s)
        MODULE_TIME[$module]=$((end_time - MODULE_START[$module]))

        if [[ "$exit_code" -eq 0 ]]; then
            MODULE_STATUS[$module]="success"
        else
            MODULE_STATUS[$module]="failed"
            BUILD_HAS_FAILURE=true
            [[ -z "$EXECUTOR_FAILED_MODULE" ]] && EXECUTOR_FAILED_MODULE="$module"
        fi
    done
}

# Count currently running modules
count_running() {
    local count=0
    for module in "${!MODULE_STATUS[@]}"; do
        [[ "${MODULE_STATUS[$module]}" == "running" ]] && ((count++)) || true
    done
    echo $count
}

# Show failure details
show_failure() {
    local module="$1"
    local log_file="$LOG_DIR/$module.log"

    echo ""
    echo -e "${RED}Build failed: ${module}${RESET}"
    echo -e "${DIM}--- Last 30 lines of log ---${RESET}"
    tail -30 "$log_file" 2>/dev/null || true
    echo -e "${DIM}----------------------------${RESET}"
    echo -e "${DIM}Full log: ${log_file}${RESET}"
}

# ============================================================================
# DAG Scheduler
# ============================================================================

# Scheduler: orchestrates the build using DAG
# - Computes what to build
# - Gets ready modules
# - Delegates to executor
# - Handles results
dag_scheduler() {
    local target="$1"
    RUN_TESTS="$2"  # Global for executor

    # Reset failure tracking
    BUILD_HAS_FAILURE=false
    EXECUTOR_FAILED_MODULE=""

    # Compute which modules are needed (target + dependencies)
    compute_target_modules "$target"
    topo_sort

    # Count modules to build
    local total_modules=0
    for module in "${MODULES_ORDER[@]}"; do
        module_matches_target "$module" "$target" && ((total_modules++)) || true
    done

    [[ $total_modules -eq 0 ]] && { print_warning "No modules match target: $target"; return 0; }

    # Setup build logs directory
    LOG_DIR="$SCRIPT_DIR/target/build-logs"
    rm -rf "$LOG_DIR"
    mkdir -p "$LOG_DIR"

    echo ""
    echo "Building modules..."
    echo ""

    # Initial render
    [[ "$DRY_RUN" != "true" && "$CI_MODE" != "true" ]] && render_status_board

    # Main scheduling loop - dynamic slot management
    while true; do
        # Check for completed modules
        check_completions

        # Get ready modules
        local ready_str=$(get_ready_modules "$target")
        local running=$(count_running)

        # Exit condition: nothing running and nothing ready
        [[ $running -eq 0 && -z "$ready_str" ]] && break

        # Dry-run mode
        if [[ "$DRY_RUN" == "true" && -n "$ready_str" ]]; then
            read -ra ready <<< "$ready_str"
            for module in "${ready[@]}"; do
                echo "  [DRY-RUN] Would execute: $module (${MODULE_CMD[$module]})"
                MODULE_STATUS[$module]="success"
            done
            continue
        fi

        # Start new modules if we have capacity
        if [[ -n "$ready_str" ]]; then
            read -ra ready <<< "$ready_str"

            if [[ "$PARALLEL_MODE" == "true" ]]; then
                # Calculate available slots
                local max_slots=$MAX_JOBS
                [[ $max_slots -eq 0 ]] && max_slots=999  # unlimited
                local available=$((max_slots - running))

                # Start modules up to available slots
                local started=0
                for module in "${ready[@]}"; do
                    [[ $started -ge $available ]] && break
                    start_module "$module"
                    ((started++)) || true
                done
            else
                # Sequential mode: start one, wait for completion
                start_module "${ready[0]}"
                # Wait for it to complete
                while [[ $(count_running) -gt 0 ]]; do
                    check_completions
                    [[ "$CI_MODE" != "true" ]] && render_status_board
                    sleep 0.1
                done
                # Stop on failure in sequential mode
                [[ "$BUILD_HAS_FAILURE" == "true" ]] && break
            fi
        fi

        # Render and wait
        [[ "$CI_MODE" != "true" ]] && render_status_board
        sleep 0.1
    done

    # Wait for any remaining background processes
    wait 2>/dev/null || true

    # Final render
    if [[ "$DRY_RUN" != "true" && "$CI_MODE" != "true" ]]; then
        render_status_board
        echo ""
    fi

    # Handle failure
    if [[ "$BUILD_HAS_FAILURE" == "true" ]]; then
        show_failure "$EXECUTOR_FAILED_MODULE"
        return 1
    fi

    return 0
}

# ============================================================================
# Display Functions
# ============================================================================

show_graph() {
    print_header "Proto-Wrapper Module DAG"
    echo ""

    topo_sort

    # Find root modules and print tree
    local -a roots=()
    for m in "${MODULES_ORDER[@]}"; do
        if [[ -z "${MODULE_DEPS[$m]}" ]]; then
            roots+=("$m")
        fi
    done

    for m in "${roots[@]}"; do
        print_tree_root "$m"
    done
}

print_tree_root() {
    local module="$1"
    local system="${MODULE_SYSTEM[$module]}"

    echo -e "${module} (${system})"

    # Find children (modules that depend on this one)
    local -a children=()
    for child in "${MODULES_ORDER[@]}"; do
        IFS=',' read -ra deps <<< "${MODULE_DEPS[$child]}"
        for dep in "${deps[@]}"; do
            if [[ "$dep" == "$module" ]]; then
                children+=("$child")
                break
            fi
        done
    done

    # Print children
    local child_count=${#children[@]}
    local child_idx=0
    for child in "${children[@]}"; do
        ((child_idx++)) || true
        local child_is_last=$([[ $child_idx -eq $child_count ]] && echo "true" || echo "false")
        print_tree_node "$child" "" "$child_is_last"
    done
}

print_tree_node() {
    local module="$1"
    local prefix="$2"
    local is_last="$3"
    local system="${MODULE_SYSTEM[$module]}"

    # Choose connector based on position
    local connector="├──"
    local child_prefix="${prefix}│   "
    if [[ "$is_last" == "true" ]]; then
        connector="└──"
        child_prefix="${prefix}    "
    fi

    echo -e "${prefix}${connector} ${module} (${system})"

    # Find children (modules that depend on this one)
    local -a children=()
    for child in "${MODULES_ORDER[@]}"; do
        IFS=',' read -ra deps <<< "${MODULE_DEPS[$child]}"
        for dep in "${deps[@]}"; do
            if [[ "$dep" == "$module" ]]; then
                children+=("$child")
                break
            fi
        done
    done

    # Print children
    local child_count=${#children[@]}
    local child_idx=0
    for child in "${children[@]}"; do
        ((child_idx++)) || true
        local child_is_last=$([[ $child_idx -eq $child_count ]] && echo "true" || echo "false")
        print_tree_node "$child" "$child_prefix" "$child_is_last"
    done
}

show_list() {
    print_header "Proto-Wrapper Modules"
    echo ""
    printf "  ${BOLD}%-18s %-18s %s${RESET}\n" "MODULE" "SYSTEM" "DESCRIPTION"
    printf "  %-18s %-18s %s\n" "------" "------" "-----------"

    topo_sort

    for module in "${MODULES_ORDER[@]}"; do
        printf "  %-18s %-18s %s\n" \
            "$module" \
            "${MODULE_SYSTEM[$module]}" \
            "${MODULE_DESC[$module]}"
    done
    echo ""
}

# ============================================================================
# Version Management
# ============================================================================

get_version() {
    cat VERSION 2>/dev/null || echo "unknown"
}

check_versions() {
    local expected="$1"
    local has_errors=false

    # VERSION file
    local v=$(cat VERSION 2>/dev/null)
    if [[ "$v" == "$expected" ]]; then
        print_success "VERSION: $v"
    else
        print_error "VERSION: $v (expected $expected)"
        has_errors=true
    fi

    # Root pom.xml
    v=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
    if [[ "$v" == "$expected" ]]; then
        print_success "pom.xml: $v"
    else
        print_error "pom.xml: $v (expected $expected)"
        has_errors=true
    fi

    # Root build.gradle.kts (reads from VERSION, so check if it matches)
    v=$(./gradlew properties -q 2>/dev/null | grep "^version:" | awk '{print $2}')
    if [[ "$v" == "$expected" ]]; then
        print_success "build.gradle.kts: $v"
    else
        print_error "build.gradle.kts: $v (expected $expected)"
        has_errors=true
    fi

    # Submodule parent versions
    local submodule_poms=(
        "proto-wrapper-core/pom.xml"
        "proto-wrapper-maven-plugin/pom.xml"
        "proto-wrapper-spring-boot-starter/pom.xml"
        "proto-wrapper-golden-tests/pom.xml"
        "proto-wrapper-maven-integration-tests/pom.xml"
        "examples/maven-example/pom.xml"
    )

    for file in "${submodule_poms[@]}"; do
        if [[ -f "$file" ]]; then
            v=$(grep -A5 '<parent>' "$file" | grep '<version>' | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
            if [[ "$v" == "$expected" ]]; then
                print_success "$file (parent): $v"
            else
                print_error "$file (parent): $v (expected $expected)"
                has_errors=true
            fi
        fi
    done

    # Standalone Gradle project
    v=$(grep -m1 'version = "' "proto-wrapper-gradle-integration-tests/build.gradle.kts" | sed 's/.*version = "\(.*\)".*/\1/')
    if [[ "$v" == "$expected" ]]; then
        print_success "proto-wrapper-gradle-integration-tests/build.gradle.kts: $v"
    else
        print_error "proto-wrapper-gradle-integration-tests/build.gradle.kts: $v (expected $expected)"
        has_errors=true
    fi

    [[ "$has_errors" == "true" ]] && return 1
    return 0
}

bump_version() {
    local new="$1"
    local old=$(get_version)

    print_section "Bumping version: $old → $new"

    # VERSION file
    echo "$new" > VERSION
    print_success "VERSION"

    # Root pom.xml
    sed -i "0,/<version>$old<\/version>/s//<version>$new<\/version>/" pom.xml
    print_success "pom.xml"

    # Submodule parent versions
    local submodule_poms=(
        "proto-wrapper-core/pom.xml"
        "proto-wrapper-maven-plugin/pom.xml"
        "proto-wrapper-spring-boot-starter/pom.xml"
        "proto-wrapper-golden-tests/pom.xml"
        "proto-wrapper-maven-integration-tests/pom.xml"
        "examples/maven-example/pom.xml"
    )

    for file in "${submodule_poms[@]}"; do
        if [[ -f "$file" ]]; then
            # Update parent version
            awk -v old="$old" -v new="$new" '
                /<parent>/ { in_parent=1 }
                in_parent && /<version>/ && !done { gsub("<version>"old"</version>", "<version>"new"</version>"); done=1 }
                /<\/parent>/ { in_parent=0 }
                { print }
            ' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
            print_success "$file (parent)"
        fi
    done

    # examples/maven-example project version
    awk -v old="$old" -v new="$new" '
        /<\/parent>/ { after_parent=1 }
        after_parent && /<version>/ && !done { gsub("<version>"old"</version>", "<version>"new"</version>"); done=1 }
        { print }
    ' "examples/maven-example/pom.xml" > "examples/maven-example/pom.xml.tmp" && \
    mv "examples/maven-example/pom.xml.tmp" "examples/maven-example/pom.xml"
    print_success "examples/maven-example/pom.xml (project)"

    # examples/spring-boot-example property
    sed -i "s/<proto-wrapper.version>$old<\/proto-wrapper.version>/<proto-wrapper.version>$new<\/proto-wrapper.version>/" \
        "examples/spring-boot-example/pom.xml"
    print_success "examples/spring-boot-example/pom.xml"

    # Standalone Gradle project
    sed -i "s/version = \"$old\"/version = \"$new\"/" "proto-wrapper-gradle-integration-tests/build.gradle.kts"
    print_success "proto-wrapper-gradle-integration-tests/build.gradle.kts"

    # Documentation - only update version in code examples, not in prose
    for file in README.md docs/CONFIGURATION.md; do
        if [[ -f "$file" ]]; then
            # Maven: <version>X.Y.Z</version>
            sed -i "s/<version>$old<\/version>/<version>$new<\/version>/g" "$file"
            # Gradle Kotlin: version = "X.Y.Z" or version("X.Y.Z")
            sed -i "s/version = \"$old\"/version = \"$new\"/g" "$file"
            sed -i "s/version(\"$old\")/version(\"$new\")/g" "$file"
            # Gradle Groovy: version "X.Y.Z"
            sed -i "s/version \"$old\"/version \"$new\"/g" "$file"
            # Maven property: <proto-wrapper.version>X.Y.Z</proto-wrapper.version>
            sed -i "s/<proto-wrapper.version>$old<\/proto-wrapper.version>/<proto-wrapper.version>$new<\/proto-wrapper.version>/g" "$file"
            print_success "$file"
        fi
    done

    echo ""
    print_info "Version bumped to $new"
    print_warning "Don't forget to update CHANGELOG.md manually!"
}

# ============================================================================
# Help
# ============================================================================

show_help() {
    cat << 'EOF'
Proto-Wrapper Plugin Build Script (DAG-based)

Usage: ./build.sh [OPTIONS]

Build Options:
  (no options)     Full build with all tests
  --quick          Build without running tests
  --parallel       Enable parallel execution for independent modules
  -j, --jobs <N>   Limit concurrent jobs (implies --parallel)
  --target <X>     Build specific target:
                     all      - Everything (default)
                     build    - Only build modules (no tests/examples)
                     test     - Build + run tests
                     tests    - Only test modules
                     examples - Only example projects
                     maven    - Only Maven modules
                     gradle   - Only Gradle modules
                     <module> - Specific module (e.g., core, maven-plugin)

Display Options:
  --graph          Display module dependency graph as tree
  --list           List all modules with descriptions
  --dry-run        Show what would be executed without running

Version Options:
  --check          Check version consistency across all files
  --bump <X.Y.Z>   Update version in all files to X.Y.Z
  --version        Show current version

Other Options:
  --ci             CI-friendly output (no colors, no spinners)
  --help           Show this help message

Examples:
  ./build.sh                          # Full build + tests
  ./build.sh --quick                  # Fast build, skip tests
  ./build.sh --parallel --quick       # Parallel fast build
  ./build.sh -j 4 --quick             # Parallel with max 4 concurrent jobs
  ./build.sh --target core            # Build only core module
  ./build.sh --target maven           # Build all Maven modules
  ./build.sh --target gradle          # Build all Gradle modules
  ./build.sh --target tests           # Build and run tests
  ./build.sh --graph                  # Show dependency tree
  ./build.sh --bump 2.5.0             # Update version to 2.5.0

EOF
}

# ============================================================================
# Main Build
# ============================================================================

full_build() {
    local target="$1"
    local run_tests="$2"
    local version=$(get_version)
    local start_time=$(date +%s)

    print_header "Proto-Wrapper Build"
    echo "Version: $version"
    echo "Target: $target"
    if [[ "$PARALLEL_MODE" == "true" ]]; then
        if [[ $MAX_JOBS -gt 0 ]]; then
            local jobs_word=$([[ $MAX_JOBS -eq 1 ]] && echo "job" || echo "jobs")
            echo "Parallel: true (max $MAX_JOBS $jobs_word)"
        else
            echo "Parallel: true (unlimited)"
        fi
    else
        echo "Parallel: false"
    fi
    echo "Tests: $([[ "$run_tests" == "true" ]] && echo "enabled" || echo "skipped")"

    # Check versions
    echo ""
    echo "Checking versions..."
    if ! check_versions "$version"; then
        print_error "Version mismatch detected! Run --bump to fix."
        return 1
    fi
    echo "All versions OK"

    # Execute DAG
    if ! dag_scheduler "$target" "$run_tests"; then
        echo ""
        print_error "Build failed!"
        return 1
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # Summary
    echo ""
    print_header "Build Summary"
    print_success "Version: $version"
    print_success "Duration: ${duration}s"

    local success_count=0
    local skip_count=0
    for module in "${!MODULE_STATUS[@]}"; do
        [[ "${MODULE_STATUS[$module]}" == "success" ]] && ((success_count++)) || true
        [[ "${MODULE_STATUS[$module]}" == "skipped" ]] && ((skip_count++)) || true
    done

    print_success "Modules built: $success_count"
    [[ $skip_count -gt 0 ]] && echo "  Modules skipped: $skip_count"

    echo ""
    echo -e "${GREEN}${BOLD}BUILD SUCCESSFUL${RESET}"
    echo ""

    return 0
}

# ============================================================================
# Main
# ============================================================================

main() {
    local action="build"
    local bump_version_arg=""

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --check)
                action="check"
                shift
                ;;
            --bump)
                action="bump"
                bump_version_arg="$2"
                [[ -z "$bump_version_arg" ]] && { echo "Error: --bump requires version"; exit 1; }
                shift 2
                ;;
            --quick)
                SKIP_TESTS=true
                shift
                ;;
            --parallel)
                PARALLEL_MODE=true
                shift
                ;;
            --jobs|-j)
                MAX_JOBS="$2"
                PARALLEL_MODE=true  # -j implies --parallel
                [[ -z "$MAX_JOBS" || ! "$MAX_JOBS" =~ ^[0-9]+$ ]] && { echo "Error: --jobs requires a number"; exit 1; }
                shift 2
                ;;
            --target)
                TARGET="$2"
                [[ -z "$TARGET" ]] && { echo "Error: --target requires argument"; exit 1; }
                shift 2
                ;;
            --graph)
                action="graph"
                shift
                ;;
            --list)
                action="list"
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --ci)
                CI_MODE=true
                shift
                ;;
            --version)
                action="version"
                shift
                ;;
            --help|-h)
                action="help"
                shift
                ;;
            *)
                echo "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done

    # Setup colors after parsing --ci flag
    setup_colors

    # Initialize modules
    init_modules

    # Execute action
    case "$action" in
        check)
            local version=$(get_version)
            print_header "Version Check"
            if check_versions "$version"; then
                echo ""
                echo -e "${GREEN}All versions are consistent${RESET}"
                exit 0
            else
                echo ""
                echo -e "${RED}Version mismatch detected!${RESET}"
                exit 1
            fi
            ;;
        bump)
            bump_version "$bump_version_arg"
            echo ""
            check_versions "$bump_version_arg"
            ;;
        graph)
            show_graph
            ;;
        list)
            show_list
            ;;
        version)
            get_version
            ;;
        help)
            show_help
            ;;
        build)
            local run_tests="true"
            [[ "$SKIP_TESTS" == "true" ]] && run_tests="false"
            full_build "$TARGET" "$run_tests"
            ;;
    esac
}

main "$@"
