#!/usr/bin/env bash
# Compila e sobe a aplicacao Spring Boot (http://localhost:8080).
# Detecta um JDK com 'javac' automaticamente (o 'java' do PATH pode ser um JRE).
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
    echo "ERRO: nenhum JDK com 'javac' encontrado. Instale um JDK 17+ ou abra o projeto no IntelliJ IDEA." >&2
    exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo ">> Usando JDK em: $JAVA_HOME ($("$JAVA_HOME/bin/javac" -version 2>&1))"

echo ">> Subindo a aplicacao Spring Boot em http://localhost:8090 ..."
echo ">> (Ctrl+C para encerrar)"
./mvnw -q -B spring-boot:run
