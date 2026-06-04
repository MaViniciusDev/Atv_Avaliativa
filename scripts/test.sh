#!/usr/bin/env bash
# Roda os testes de integracao (sobe o contexto Spring Boot real e valida os
# requisitos contra o MongoDB). Requer o MongoDB ativo (./scripts/start-mongo.sh).
set -e

cd "$(dirname "$0")/.."

find_jdk() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then return 0; fi
    if command -v javac >/dev/null 2>&1; then
        JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"; return 0
    fi
    for c in /usr/lib/jvm/*/bin/javac \
             "$HOME"/.local/share/JetBrains/Toolbox/apps/*/jbr/bin/javac \
             "$HOME"/.sdkman/candidates/java/*/bin/javac; do
        if [ -x "$c" ]; then JAVA_HOME="$(dirname "$(dirname "$c")")"; return 0; fi
    done
    return 1
}

if ! find_jdk; then
    echo "ERRO: nenhum JDK com 'javac' encontrado." >&2
    exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

echo ">> Rodando testes de integracao..."
./mvnw -B test
