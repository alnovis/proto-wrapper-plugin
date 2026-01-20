#!/bin/bash
#
# Proto-Wrapper Plugin Build Script
#
# Usage:
#   ./build.sh              Full build + all tests
#   ./build.sh --check      Check version consistency only
#   ./build.sh --bump 1.7.0 Update version in all files
#   ./build.sh --quick      Build without tests
#   ./build.sh --parallel   Run Maven and Gradle tests in parallel
#   ./build.sh --ci         CI-friendly output (no colors/emojis)
#   ./build.sh --version    Show current version
#   ./build.sh --help       Show this help
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ============================================================================
# Configuration
# ============================================================================

# Files that need version updates
VERSION_FILES=(
    "pom.xml:9"
    "build.gradle.kts:8"
    "examples/maven-example/pom.xml:proto-wrapper.version"
    "examples/spring-boot-example/pom.xml:proto-wrapper.version"
    "proto-wrapper-gradle-integration-tests/build.gradle.kts:6"
)

# Documentation files with version references
DOC_FILES=(
    "README.md"
    "docs/CONFIGURATION.md"
    "../CLAUDE.md"
)

# ============================================================================
# Color and Output
# ============================================================================

CI_MODE=false
PARALLEL_MODE=false

setup_colors() {
    if [[ "$CI_MODE" == "true" ]] || [[ ! -t 1 ]]; then
        RED=""
        GREEN=""
        YELLOW=""
        BLUE=""
        CYAN=""
        BOLD=""
        RESET=""
        CHECK="[OK]"
        CROSS="[FAIL]"
        ARROW="->"
    else
        RED="\033[0;31m"
        GREEN="\033[0;32m"
        YELLOW="\033[0;33m"
        BLUE="\033[0;34m"
        CYAN="\033[0;36m"
        BOLD="\033[1m"
        RESET="\033[0m"
        CHECK="✓"
        CROSS="✗"
        ARROW="→"
    fi
}

print_header() {
    local title="$1"
    local width=50
    echo ""
    if [[ "$CI_MODE" == "true" ]]; then
        echo "========================================"
        echo "  $title"
        echo "========================================"
    else
        echo -e "${CYAN}╔$(printf '═%.0s' $(seq 1 $width))╗${RESET}"
        echo -e "${CYAN}║${RESET}  ${BOLD}$title${RESET}$(printf ' %.0s' $(seq 1 $((width - ${#title} - 2))))${CYAN}║${RESET}"
        echo -e "${CYAN}╚$(printf '═%.0s' $(seq 1 $width))╝${RESET}"
    fi
}

print_section() {
    local title="$1"
    echo ""
    echo -e "${BLUE}${BOLD}=== $title ===${RESET}"
}

print_success() {
    echo -e "  ${GREEN}${CHECK}${RESET} $1"
}

print_error() {
    echo -e "  ${RED}${CROSS}${RESET} $1"
}

print_warning() {
    echo -e "  ${YELLOW}!${RESET} $1"
}

print_info() {
    echo -e "  ${ARROW} $1"
}

# ============================================================================
# Version Management
# ============================================================================

get_current_version() {
    grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/'
}

check_versions() {
    local expected_version="$1"
    local has_errors=false

    print_section "Checking version consistency (expected: $expected_version)"

    # Check pom.xml
    local pom_version=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
    if [[ "$pom_version" == "$expected_version" ]]; then
        print_success "pom.xml: $pom_version"
    else
        print_error "pom.xml: $pom_version (expected $expected_version)"
        has_errors=true
    fi

    # Check build.gradle.kts
    local gradle_version=$(grep -m1 'version = "' build.gradle.kts | sed 's/.*version = "\(.*\)".*/\1/')
    if [[ "$gradle_version" == "$expected_version" ]]; then
        print_success "build.gradle.kts: $gradle_version"
    else
        print_error "build.gradle.kts: $gradle_version (expected $expected_version)"
        has_errors=true
    fi

    # Check examples/maven-example/pom.xml
    # Note: maven-example uses ${project.parent.version} which is valid (inherits from parent)
    local maven_example_version=$(grep '<proto-wrapper.version>' examples/maven-example/pom.xml | sed 's/.*<proto-wrapper.version>\(.*\)<\/proto-wrapper.version>.*/\1/')
    if [[ "$maven_example_version" == "$expected_version" ]] || [[ "$maven_example_version" == '${project.parent.version}' ]]; then
        print_success "examples/maven-example/pom.xml: $maven_example_version"
    else
        print_error "examples/maven-example/pom.xml: $maven_example_version (expected $expected_version or \${project.parent.version})"
        has_errors=true
    fi

    # Check examples/spring-boot-example/pom.xml
    local spring_example_version=$(grep '<proto-wrapper.version>' examples/spring-boot-example/pom.xml | sed 's/.*<proto-wrapper.version>\(.*\)<\/proto-wrapper.version>.*/\1/')
    if [[ "$spring_example_version" == "$expected_version" ]]; then
        print_success "examples/spring-boot-example/pom.xml: $spring_example_version"
    else
        print_error "examples/spring-boot-example/pom.xml: $spring_example_version (expected $expected_version)"
        has_errors=true
    fi

    # Check proto-wrapper-gradle-integration-tests/build.gradle.kts
    local gradle_it_version=$(grep -m1 'version = "' proto-wrapper-gradle-integration-tests/build.gradle.kts | sed 's/.*version = "\(.*\)".*/\1/')
    if [[ "$gradle_it_version" == "$expected_version" ]]; then
        print_success "proto-wrapper-gradle-integration-tests/build.gradle.kts: $gradle_it_version"
    else
        print_error "proto-wrapper-gradle-integration-tests/build.gradle.kts: $gradle_it_version (expected $expected_version)"
        has_errors=true
    fi

    # Check proto-wrapper-maven-integration-tests/pom.xml (parent version)
    local maven_it_version=$(grep -A5 '<parent>' proto-wrapper-maven-integration-tests/pom.xml | grep '<version>' | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
    if [[ "$maven_it_version" == "$expected_version" ]]; then
        print_success "proto-wrapper-maven-integration-tests/pom.xml (parent): $maven_it_version"
    else
        print_error "proto-wrapper-maven-integration-tests/pom.xml (parent): $maven_it_version (expected $expected_version)"
        has_errors=true
    fi

    # Check CLAUDE.md (workspace)
    if [[ -f "../CLAUDE.md" ]]; then
        local claude_version_count=$(grep -c "Version: $expected_version\|version \"$expected_version\"\|<version>$expected_version</version>" ../CLAUDE.md 2>/dev/null || echo "0")
        if [[ "$claude_version_count" -ge 2 ]]; then
            print_success "CLAUDE.md: found $claude_version_count version references"
        else
            print_warning "CLAUDE.md: found only $claude_version_count version references (check manually)"
        fi
    fi

    if [[ "$has_errors" == "true" ]]; then
        return 1
    fi
    return 0
}

bump_version() {
    local new_version="$1"
    local old_version=$(get_current_version)

    print_section "Bumping version: $old_version -> $new_version"

    # Update pom.xml (root) - first <version> tag
    sed -i "0,/<version>$old_version<\/version>/s//<version>$new_version<\/version>/" pom.xml
    print_success "pom.xml"

    # Update build.gradle.kts (root)
    sed -i "s/version = \"$old_version\"/version = \"$new_version\"/" build.gradle.kts
    print_success "build.gradle.kts"

    # Update examples/maven-example/pom.xml (only if not using parent version)
    local maven_example_version=$(grep '<proto-wrapper.version>' examples/maven-example/pom.xml | sed 's/.*<proto-wrapper.version>\(.*\)<\/proto-wrapper.version>.*/\1/')
    if [[ "$maven_example_version" != '${project.parent.version}' ]]; then
        sed -i "s/<proto-wrapper.version>$old_version<\/proto-wrapper.version>/<proto-wrapper.version>$new_version<\/proto-wrapper.version>/" examples/maven-example/pom.xml
        print_success "examples/maven-example/pom.xml"
    else
        print_info "examples/maven-example/pom.xml: uses \${project.parent.version} (no change needed)"
    fi

    # Update examples/spring-boot-example/pom.xml
    sed -i "s/<proto-wrapper.version>$old_version<\/proto-wrapper.version>/<proto-wrapper.version>$new_version<\/proto-wrapper.version>/" examples/spring-boot-example/pom.xml
    print_success "examples/spring-boot-example/pom.xml"

    # Update proto-wrapper-gradle-integration-tests/build.gradle.kts
    sed -i "s/version = \"$old_version\"/version = \"$new_version\"/" proto-wrapper-gradle-integration-tests/build.gradle.kts
    print_success "proto-wrapper-gradle-integration-tests/build.gradle.kts"

    # Update proto-wrapper-maven-integration-tests/pom.xml (parent version)
    sed -i "s/<version>$old_version<\/version>/<version>$new_version<\/version>/" proto-wrapper-maven-integration-tests/pom.xml
    print_success "proto-wrapper-maven-integration-tests/pom.xml (parent)"

    # Update submodule parent versions
    for submodule in proto-wrapper-core proto-wrapper-maven-plugin proto-wrapper-spring-boot-starter proto-wrapper-golden-tests; do
        if [[ -f "$submodule/pom.xml" ]]; then
            sed -i "s/<version>$old_version<\/version>/<version>$new_version<\/version>/" "$submodule/pom.xml"
            print_success "$submodule/pom.xml (parent)"
        fi
    done

    # Update examples/maven-example/pom.xml parent version
    sed -i "0,/<version>$old_version<\/version>/s//<version>$new_version<\/version>/" examples/maven-example/pom.xml
    print_success "examples/maven-example/pom.xml (parent)"

    # Update CLAUDE.md
    if [[ -f "../CLAUDE.md" ]]; then
        sed -i "s/$old_version/$new_version/g" ../CLAUDE.md
        print_success "CLAUDE.md"
    fi

    # Update README.md
    sed -i "s/$old_version/$new_version/g" README.md
    print_success "README.md"

    # Update docs/CONFIGURATION.md
    if [[ -f "docs/CONFIGURATION.md" ]]; then
        sed -i "s/$old_version/$new_version/g" docs/CONFIGURATION.md
        print_success "docs/CONFIGURATION.md"
    fi

    print_info "Version bumped to $new_version"
    print_info "Don't forget to update CHANGELOG.md manually!"
}

# ============================================================================
# Build Functions
# ============================================================================

maven_build() {
    local skip_tests="$1"

    print_section "Maven Build"

    local output
    local exit_code

    if [[ "$skip_tests" == "true" ]]; then
        print_info "Running: mvn clean install -DskipTests"
        output=$(mvn clean install -DskipTests -q 2>&1)
        exit_code=$?
        if [[ $exit_code -eq 0 ]]; then
            print_success "Maven build completed"
            return 0
        else
            print_error "Maven build failed"
            echo "$output" | tail -30
            return 1
        fi
    else
        print_info "Running: mvn clean install"
        output=$(mvn clean install -q 2>&1)
        exit_code=$?
        if [[ $exit_code -eq 0 ]]; then
            print_success "Maven build and tests completed"
            return 0
        else
            print_error "Maven build failed"
            echo "$output" | tail -50
            return 1
        fi
    fi
}

maven_tests() {
    print_section "Maven Tests"

    print_info "Running: mvn test -pl proto-wrapper-core,proto-wrapper-golden-tests,proto-wrapper-maven-integration-tests"

    local output
    output=$(mvn test -pl proto-wrapper-core,proto-wrapper-golden-tests,proto-wrapper-maven-integration-tests -q 2>&1)
    local exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        print_success "Core tests passed"
        print_success "Golden tests passed"
        print_success "Maven integration tests passed"
        return 0
    else
        print_error "Maven tests failed"
        echo "$output" | tail -30
        return 1
    fi
}

gradle_build() {
    print_section "Gradle Build"

    print_info "Running: ./gradlew check publishToMavenLocal"

    local output
    output=$(./gradlew check publishToMavenLocal -q 2>&1)
    local exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        print_success "Gradle build and tests completed"
        print_success "Published to mavenLocal"
        return 0
    else
        print_error "Gradle build failed"
        echo "$output" | tail -30
        return 1
    fi
}

gradle_integration_tests() {
    print_section "Gradle Integration Tests (standalone)"

    print_info "Running: gradle test (in proto-wrapper-gradle-integration-tests)"

    cd proto-wrapper-gradle-integration-tests
    local output
    output=$(gradle test -q 2>&1)
    local exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        print_success "Gradle integration tests passed"
        cd ..
        return 0
    else
        print_error "Gradle integration tests failed"
        echo "$output" | tail -30
        cd ..
        return 1
    fi
}

# ============================================================================
# Main Build Orchestration
# ============================================================================

full_build() {
    local skip_tests="$1"
    local version=$(get_current_version)
    local start_time=$(date +%s)
    local maven_result=0
    local gradle_result=0
    local gradle_it_result=0

    print_header "Proto-Wrapper Plugin Build"
    print_info "Version: $version"
    print_info "Parallel: $PARALLEL_MODE"

    # Check versions first
    if ! check_versions "$version"; then
        print_error "Version mismatch detected! Run with --bump to fix."
        return 1
    fi

    if [[ "$skip_tests" == "true" ]]; then
        # Quick build without tests
        maven_build true
        gradle_build
    else
        if [[ "$PARALLEL_MODE" == "true" ]]; then
            # Parallel execution
            print_section "Running builds in parallel"

            # First, do Maven build (needed for Gradle)
            maven_build true || return 1

            # Then run tests in parallel
            local maven_log=$(mktemp)
            local gradle_log=$(mktemp)

            print_info "Starting Maven tests..."
            (maven_tests > "$maven_log" 2>&1; echo $? > "${maven_log}.exit") &
            local maven_pid=$!

            print_info "Starting Gradle build..."
            (gradle_build > "$gradle_log" 2>&1; echo $? > "${gradle_log}.exit") &
            local gradle_pid=$!

            # Wait for both
            wait $maven_pid
            wait $gradle_pid

            maven_result=$(cat "${maven_log}.exit")
            gradle_result=$(cat "${gradle_log}.exit")

            # Show results
            echo ""
            echo -e "${BOLD}Maven Tests Output:${RESET}"
            cat "$maven_log"

            echo ""
            echo -e "${BOLD}Gradle Build Output:${RESET}"
            cat "$gradle_log"

            rm -f "$maven_log" "${maven_log}.exit" "$gradle_log" "${gradle_log}.exit"

            if [[ $maven_result -ne 0 ]] || [[ $gradle_result -ne 0 ]]; then
                print_error "Parallel build failed"
                return 1
            fi

            # Gradle integration tests (must run after Gradle build)
            gradle_integration_tests || gradle_it_result=1
        else
            # Sequential execution
            maven_build false || return 1
            gradle_build || return 1
            gradle_integration_tests || return 1
        fi
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # Final summary
    print_header "Build Summary"
    print_success "Version: $version"
    print_success "Duration: ${duration}s"

    if [[ "$skip_tests" != "true" ]]; then
        print_success "All tests passed"
    fi

    echo ""
    echo -e "${GREEN}${BOLD}BUILD SUCCESSFUL${RESET}"
    echo ""

    return 0
}

# ============================================================================
# Help
# ============================================================================

show_help() {
    cat << EOF
Proto-Wrapper Plugin Build Script

Usage: ./build.sh [OPTIONS]

Options:
  (no options)     Full build with all tests
  --check          Check version consistency only
  --bump VERSION   Update version in all files to VERSION
  --quick          Build without running tests
  --parallel       Run Maven and Gradle tests in parallel
  --ci             CI-friendly output (no colors/emojis)
  --version        Show current version
  --help           Show this help message

Examples:
  ./build.sh                    # Full build + tests
  ./build.sh --check            # Just check versions
  ./build.sh --bump 1.7.0       # Update all versions to 1.7.0
  ./build.sh --quick            # Fast build without tests
  ./build.sh --parallel --ci    # Parallel tests with CI output

Files checked/updated:
  - pom.xml (root)
  - build.gradle.kts (root)
  - examples/maven-example/pom.xml
  - examples/spring-boot-example/pom.xml
  - proto-wrapper-gradle-integration-tests/build.gradle.kts
  - README.md
  - docs/CONFIGURATION.md
  - CLAUDE.md (workspace)

EOF
}

# ============================================================================
# Main
# ============================================================================

main() {
    local action="build"
    local skip_tests=false
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
                if [[ -z "$bump_version_arg" ]]; then
                    echo "Error: --bump requires a version argument"
                    exit 1
                fi
                shift 2
                ;;
            --quick)
                skip_tests=true
                shift
                ;;
            --parallel)
                PARALLEL_MODE=true
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

    # Execute action
    case "$action" in
        check)
            local version=$(get_current_version)
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
            # Verify after bump
            echo ""
            check_versions "$bump_version_arg"
            ;;
        version)
            echo "$(get_current_version)"
            ;;
        help)
            show_help
            ;;
        build)
            full_build "$skip_tests"
            ;;
    esac
}

main "$@"
