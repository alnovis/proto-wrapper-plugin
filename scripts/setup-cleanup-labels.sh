#!/bin/bash
# Setup labels for branch cleanup workflow
#
# Usage:
#   ./scripts/setup-cleanup-labels.sh [REPO]
#
# Examples:
#   ./scripts/setup-cleanup-labels.sh                    # Uses current repo
#   ./scripts/setup-cleanup-labels.sh alnovis/proto-wrapper-plugin

set -e

# Determine repository
if [ -n "$1" ]; then
    REPO="$1"
else
    # Try to get repo from git remote
    REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || echo "")
    if [ -z "$REPO" ]; then
        echo "Error: Could not determine repository."
        echo "Usage: $0 <owner/repo>"
        exit 1
    fi
fi

echo "Setting up cleanup labels for repository: $REPO"
echo ""

# Create cleanup-scheduled label
echo "Creating label: cleanup-scheduled"
gh label create "cleanup-scheduled" \
    --description "Branch scheduled for automatic deletion" \
    --color "fbca04" \
    --repo "$REPO" 2>/dev/null \
    && echo "  Created successfully" \
    || echo "  Label already exists"

# Create cleanup-done label
echo "Creating label: cleanup-done"
gh label create "cleanup-done" \
    --description "Branch was automatically deleted" \
    --color "0e8a16" \
    --repo "$REPO" 2>/dev/null \
    && echo "  Created successfully" \
    || echo "  Label already exists"

# Create keep-branch label
echo "Creating label: keep-branch"
gh label create "keep-branch" \
    --description "Prevent automatic branch deletion" \
    --color "d73a4a" \
    --repo "$REPO" 2>/dev/null \
    && echo "  Created successfully" \
    || echo "  Label already exists"

echo ""
echo "Labels setup complete!"
echo ""
echo "Labels created:"
echo "  - cleanup-scheduled (yellow): Added when PR is merged, branch will be deleted"
echo "  - cleanup-done (green): Added after branch is deleted"
echo "  - keep-branch (red): Add to prevent automatic deletion"
