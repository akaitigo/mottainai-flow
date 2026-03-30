.PHONY: build test lint format check quality clean

# === Multi-language project ===
# backend: Kotlin + Quarkus
# routing: Go + OR-Tools
# frontend: TypeScript + React
# proto: buf

build:
	@echo "=== Build ==="
	cd backend && ./gradlew build
	cd routing && go build $(GOFLAGS) -ldflags "-s -w" ./...
	cd frontend && npx tsc

test:
	@echo "=== Test ==="
	cd backend && ./gradlew test
	cd routing && go test -v -race -count=1 ./...
	cd frontend && npx vitest run

lint:
	@echo "=== Lint ==="
	cd backend && ./gradlew detekt
	cd routing && golangci-lint run ./...
	cd frontend && npx oxlint . && npx biome check .
	buf lint

format:
	@echo "=== Format ==="
	cd backend && ./gradlew formatKotlin
	cd routing && gofumpt -w . && goimports -w .
	cd frontend && npx biome format --write .
	buf format -w

check: format lint test build
	@echo "All checks passed."

quality:
	@echo "=== Quality Gate ==="
	@test -f LICENSE || { echo "ERROR: LICENSE missing. Fix: add MIT LICENSE file"; exit 1; }
	@! grep -rn "TODO\|FIXME\|HACK\|console\.log\|println\|print(" backend/src/ routing/ frontend/src/ 2>/dev/null | grep -v "node_modules" || { echo "ERROR: debug output or TODO found. Fix: remove before ship"; exit 1; }
	@! grep -rn "password=\|secret=\|api_key=\|sk-\|ghp_" backend/src/ routing/ frontend/src/ 2>/dev/null | grep -v '\$${' | grep -v "node_modules" || { echo "ERROR: hardcoded secrets. Fix: use env vars with no default"; exit 1; }
	@test ! -f PRD.md || ! grep -q "\[ \]" PRD.md || { echo "ERROR: unchecked acceptance criteria in PRD.md"; exit 1; }
	@test ! -f CLAUDE.md || [ $$(wc -l < CLAUDE.md) -le 50 ] || { echo "ERROR: CLAUDE.md is $$(wc -l < CLAUDE.md) lines (max 50). Fix: remove build details, use pointers only"; exit 1; }
	@echo "OK: automated quality checks passed"
	@echo "Manual checks required: README quickstart, demo GIF, input validation, ADR >=1"

clean:
	cd backend && ./gradlew clean
	cd routing && go clean -cache -testcache
	cd frontend && rm -rf dist/ coverage/ node_modules/.cache/
