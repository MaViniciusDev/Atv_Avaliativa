package br.com.steamcatalog.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitor de CSV no estilo RFC 4180, escrito a mao (sem bibliotecas externas).
 * Trata corretamente:
 *  - campos entre aspas com virgulas internas;
 *  - aspas escapadas dentro do campo ("");
 *  - quebras de linha dentro de campos entre aspas.
 *
 * Isso e necessario porque a coluna "Description" do dataset contem virgulas,
 * aspas e quebras de linha.
 */
public class CsvReader implements Closeable {

    private final BufferedReader reader;

    public CsvReader(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    /**
     * Le o proximo registro do CSV (um registro pode ocupar varias linhas
     * fisicas se houver quebras dentro de aspas).
     *
     * @return a lista de campos do registro, ou {@code null} no fim do arquivo.
     */
    public List<String> readRecord() throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;
        int c = reader.read();

        if (c == -1) {
            return null; // fim do arquivo
        }

        while (c != -1) {
            char ch = (char) c;

            if (inQuotes) {
                if (ch == '"') {
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"'); // aspas escapadas
                    } else {
                        inQuotes = false;
                        c = next;
                        continue; // ja lemos o proximo caractere
                    }
                } else {
                    field.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                    fieldStarted = true;
                } else if (ch == ',') {
                    fields.add(field.toString());
                    field.setLength(0);
                    fieldStarted = false;
                } else if (ch == '\r') {
                    // ignora; o \n trata o fim de linha
                } else if (ch == '\n') {
                    break; // fim do registro
                } else {
                    field.append(ch);
                    fieldStarted = true;
                }
            }
            c = reader.read();
        }

        // adiciona o ultimo campo (a menos que a linha esteja totalmente vazia)
        if (fieldStarted || field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
        }
        return fields;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
