#!/usr/bin/env bash
# PreToolUse(Bash) hook: force a manual approval prompt before any `git commit`
# or `git push`, so code changes are reviewed first.
#
# Emits a PreToolUse "ask" decision when the Bash command runs commit/push;
# stays silent (no-op) for everything else.

set -euo pipefail

payload="$(cat)"
cmd="$(printf '%s' "$payload" | jq -r '.tool_input.command // ""')"

# Trigger whenever a `git` invocation also mentions the `commit` or `push`
# subcommand: covers `git commit`, `git push`, `git -c k=v commit --amend`,
# chained forms like `npm test && git push`, etc. Errs toward asking.
if printf '%s' "$cmd" | grep -Eq '(^|[^[:alnum:]_/.-])git\b.*\b(commit|push)\b'; then
  cat <<'JSON'
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "ask",
    "permissionDecisionReason": "Review gate: confirm you have reviewed the code changes before this commit/push runs."
  }
}
JSON
fi

exit 0
