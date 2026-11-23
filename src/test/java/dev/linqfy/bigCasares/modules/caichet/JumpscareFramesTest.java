package dev.linqfy.bigCasares.modules.caichet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Las imagenes de los sustos: el texto que las dibuja y, si el pack ya las trae, que la fuente este completa. */
class JumpscareFramesTest {

    private static final Path ASSETS = Path.of("resourcepack/java/assets/caichet");
    private static final int MAX_GLYPH_PIXELS = 256;

    @Test
    void thereIsOneImagePerEntity() {
        assertEquals(BackroomsKind.values().length, JumpscareFrames.COUNT);
        assertEquals(5, JumpscareFrames.COUNT);
        assertEquals(0xE100, JumpscareFrames.FIRST_GLYPH);
        assertEquals(4, JumpscareFrames.COLUMNS);
        assertEquals(2, JumpscareFrames.ROWS);
    }

    @Test
    void eachImageDrawsItsTilesRowByRowWithoutGaps() {
        String text = JumpscareFrames.glyphs(0);
        int tiles = JumpscareFrames.COLUMNS * JumpscareFrames.ROWS;
        assertEquals(tiles * 2 + JumpscareFrames.ROWS - 1, text.length());
        assertEquals(JumpscareFrames.FIRST_GLYPH, text.charAt(0));
        assertEquals(JumpscareFrames.NO_GAP, text.charAt(1));
        assertEquals(JumpscareFrames.ROW_START, text.charAt(JumpscareFrames.COLUMNS * 2));
        assertEquals(JumpscareFrames.glyph(0, 1, 0), text.charAt(JumpscareFrames.COLUMNS * 2 + 1));
        assertEquals(0xF8FF, JumpscareFrames.NO_GAP);
        assertEquals(0xF8FE, JumpscareFrames.ROW_START);
    }

    @Test
    void imagesNeverShareAGlyphWithEachOtherNorWithTheMemories() {
        assertEquals(JumpscareFrames.glyph(0, JumpscareFrames.ROWS - 1, JumpscareFrames.COLUMNS - 1) + 1,
            JumpscareFrames.glyph(1, 0, 0));
        char last = JumpscareFrames.glyph(JumpscareFrames.COUNT - 1, JumpscareFrames.ROWS - 1,
            JumpscareFrames.COLUMNS - 1);
        assertTrue(last < JumpscareFrames.ROW_START, "no pisan los espacios de la fuente");
        char lastMemory = MemoryFrames.glyph(MemoryFrames.COUNT - 1, MemoryFrames.ROWS - 1, MemoryFrames.COLUMNS - 1);
        assertTrue(lastMemory < JumpscareFrames.FIRST_GLYPH, "ni las letras de las memorias");
    }

    @Test
    void anEntityDrawsItsOwnImage() {
        for (BackroomsKind kind : BackroomsKind.values()) {
            assertEquals(JumpscareFrames.glyphs(kind.ordinal()), JumpscareFrames.glyphs(kind));
            assertEquals(JumpscareFrames.glyph(kind.ordinal(), 0, 0), JumpscareFrames.glyphs(kind).charAt(0));
        }
    }

    @Test
    void theFontShipsEveryTileWithinTheGlyphSizeLimitOnceThePackHasIt() throws IOException {
        Path fontFile = ASSETS.resolve("font/sustos.json");
        assumeTrue(Files.isRegularFile(fontFile), "el pack todavia no trae la fuente de sustos");
        JsonObject font;
        try (Reader reader = Files.newBufferedReader(fontFile, StandardCharsets.UTF_8)) {
            font = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Map<Character, JsonObject> bitmaps = new HashMap<>();
        JsonObject spaces = null;
        for (JsonElement element : font.getAsJsonArray("providers")) {
            JsonObject provider = element.getAsJsonObject();
            if (provider.get("type").getAsString().equals("space")) {
                spaces = provider.getAsJsonObject("advances");
            } else {
                JsonArray chars = provider.getAsJsonArray("chars");
                bitmaps.put(chars.get(0).getAsString().charAt(0), provider);
            }
        }
        assertNotNull(spaces, "la fuente trae los espacios de ancho negativo");
        assertEquals(-1, spaces.get(String.valueOf(JumpscareFrames.NO_GAP)).getAsInt());
        int tileWidth = 0;
        for (int frame = 0; frame < JumpscareFrames.COUNT; frame++) {
            for (int row = 0; row < JumpscareFrames.ROWS; row++) {
                for (int column = 0; column < JumpscareFrames.COLUMNS; column++) {
                    JsonObject provider = bitmaps.get(JumpscareFrames.glyph(frame, row, column));
                    assertNotNull(provider, "falta el mosaico " + frame + "/" + row + "/" + column);
                    String file = provider.get("file").getAsString();
                    assertTrue(file.startsWith("caichet:"), file);
                    Path png = ASSETS.resolve("textures").resolve(file.substring("caichet:".length()));
                    BufferedImage image = ImageIO.read(png.toFile());
                    assertNotNull(image, png + " se puede leer");
                    assertTrue(image.getWidth() <= MAX_GLYPH_PIXELS && image.getHeight() <= MAX_GLYPH_PIXELS,
                        png + " entra en el limite de " + MAX_GLYPH_PIXELS + " pixeles");
                    int height = provider.get("height").getAsInt();
                    assertTrue(provider.get("ascent").getAsInt() <= height, "ascent <= height en " + file);
                    assertEquals(0, image.getWidth() * height % image.getHeight(), file + " calza sin fraccion");
                    tileWidth = image.getWidth() * height / image.getHeight();
                }
            }
        }
        assertEquals(-tileWidth * JumpscareFrames.COLUMNS,
            spaces.get(String.valueOf(JumpscareFrames.ROW_START)).getAsInt());
    }
}
