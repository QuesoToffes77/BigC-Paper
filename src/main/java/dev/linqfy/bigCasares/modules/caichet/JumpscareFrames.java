package dev.linqfy.bigCasares.modules.caichet;

/**
 * Las imagenes de los sustos de las entidades de los backrooms: una por entidad (en el orden de
 * {@link BackroomsKind}), a pantalla completa. Mismo esquema que {@link MemoryFrames}: cada una va partida en mosaicos,
 * que son letras seguidas de la fuente {@code caichet:sustos}.
 */
final class JumpscareFrames {

    /** Una imagen por entidad. */
    static final int COUNT = BackroomsKind.values().length;
    static final int COLUMNS = MemoryFrames.COLUMNS;
    static final int ROWS = MemoryFrames.ROWS;
    /** La primera letra de la primera imagen; las demas siguen en orden. */
    static final int FIRST_GLYPH = 0xE100;
    /** Los mismos espacios de ancho negativo que usa la fuente de las memorias. */
    static final char NO_GAP = MemoryFrames.NO_GAP;
    static final char ROW_START = MemoryFrames.ROW_START;

    private JumpscareFrames() {
    }

    /** La letra del mosaico (fila, columna) de una imagen. */
    static char glyph(int frame, int row, int column) {
        return (char) (FIRST_GLYPH + frame * COLUMNS * ROWS + row * COLUMNS + column);
    }

    /** El texto que dibuja la imagen numero frame: sus mosaicos, fila por fila, sin huecos entre ellos. */
    static String glyphs(int frame) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            if (row > 0) {
                text.append(ROW_START);
            }
            for (int column = 0; column < COLUMNS; column++) {
                text.append(glyph(frame, row, column)).append(NO_GAP);
            }
        }
        return text.toString();
    }

    /** El texto de la imagen del susto de una entidad. */
    static String glyphs(BackroomsKind kind) {
        return glyphs(kind.frame());
    }
}
