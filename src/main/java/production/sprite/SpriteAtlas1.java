package production.sprite;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class SpriteAtlas1 {

    /*                    ~~~ creation parameter ~~~                          */
    private SpritePalette palette;
    private int spriteSize;                     /* pixels */

    /*                      ~~~ comes from file ~~~                           */
    private int size;

    /*                          ~~~ derived ~~~                               */
    private int spritesPerRow;                  /* unitless */
    private int spriteCount;                    /* unitless */

    private boolean init;
    private byte data[];

    /*                         ~~~ constants ~~~                              */
    /* atlas idx are stored as unsigned shorts */
    public static final int MAX_ATLAS_IDX = 0xFFFF;

    SpriteAtlas1(SpritePalette palette, int spriteSize, byte data[]) {
        int size, i, maxPaletteIdx;

        if (data == null || !(data.length > 0)) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_NO_DATA);
        }
        size = (int)Math.sqrt(data.length);
        /* doubles as fuzzy check that size is < ~46k */
        if (!(size * size == data.length)) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_DATA_NOT_SQUARE);
        }
        if (!((size & (size - 1)) == 0)) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_DATA_NOT_POW2);
        }
        if (!(size % spriteSize == 0)) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_MULTIPLE_SPRITESIZE);
        }
        this.spritesPerRow = size / spriteSize;
        this.spriteCount = this.spritesPerRow * this.spritesPerRow;
        if (this.spriteCount > MAX_ATLAS_IDX) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_TOO_MANY_SPRITES);
        }
        /* Palette guarantees properly initialized if !null */
        if (palette == null) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_PALETTE);
        }
        maxPaletteIdx = palette.getMaxIdx();
        for (i = 0; i < data.length; ++i) {
            if (!((data[i] & 0xFF) <= maxPaletteIdx)) {
                LogFatalAndExit(ErrStrIdxOOR(data[i] & 0xFF, maxPaletteIdx));
            }
        }
        if (!((spriteSize & (spriteSize -1)) == 0)) {
            LogFatalAndExit(ERR_STR_FAILED_CONSTRUCT_SPRITESIZE);
        }
        this.palette = palette;
        this.spriteSize = spriteSize;
        this.data = data;
        this.size = size;
        this.init = true;
    }

    public static final String CLASS = SpriteAtlas1.class.getSimpleName();
    private static final String ERR_STR_FAILED_CONSTRUCT_NO_DATA = CLASS +
            " failed construction because data was empty or null.\n";
    private static final String ERR_STR_FAILED_CONSTRUCT_PALETTE = CLASS +
            " failed construction because palette was null.\n";
    private static final String ERR_STR_FAILED_CONSTRUCT_SPRITESIZE = CLASS +
            " failed construction because spriteSize was not a power of two.\n";
    private static final String ERR_STR_FAILED_CONSTRUCT_DATA_NOT_POW2 = CLASS +
            " failed construction because data size was not a power of two.\n";
    private static final String ERR_STR_FAILED_CONSTRUCT_MULTIPLE_SPRITESIZE =
            CLASS + " failed construction because the data size was not a " +
                    "multiple of spriteSize.\n";
    private static final String ERR_STR_FAILED_CONSTRUCT_DATA_NOT_SQUARE =
            CLASS + " failed construction because the data was not square.\n";
    private static final String ERR_STR_FAILED_CONSTRUCT_TOO_MANY_SPRITES =
            CLASS + " failed construction because the maximum allowable " +
                    "sprite count [" + MAX_ATLAS_IDX + "] was exceeded.\n";
    private static String ErrStrIdxOOR(int idx, int maxIdx) {
        return String.format("%s failed construction because an index [%d] " +
                "was outside the valid range [0 - %d].\n", idx, maxIdx);
    }
}
