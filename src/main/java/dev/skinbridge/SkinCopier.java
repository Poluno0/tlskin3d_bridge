package dev.skinbridge;

import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Копирует пиксели из обёртки изображения TL (ImageWrap) в обычный NativeImage 64x64.
 * Классы TL не используются при компиляции: всё делается через рефлексию, чтобы не копировать чужой мод в проект.
 */
public final class SkinCopier {
    private static final Map<Class<?>, Accessors> ACCESSORS = new ConcurrentHashMap<>();
    private static int logged;
    private static boolean warned;

    private SkinCopier() {
    }

    private static final class Accessors {
        final Method width;
        final Method height;
        final Method rgb;
        /** NativeImageWrap отдаёт «родной» формат NativeImage (ABGR), BufferedImageWrap отдаёт ARGB. */
        final boolean nativeFormat;

        Accessors(Method width, Method height, Method rgb, boolean nativeFormat) {
            this.width = width;
            this.height = height;
            this.rgb = rgb;
            this.nativeFormat = nativeFormat;
        }
    }

    /** Возвращает копию скина 64x64 или null, если это не скин (или формат не подходит). */
    public static NativeImage copy(Object wrap) {
        if (wrap == null) {
            return null;
        }
        try {
            Accessors a = resolve(wrap.getClass());
            int w = (Integer) a.width.invoke(wrap);
            int h = (Integer) a.height.invoke(wrap);
            // Skin Layers 3D умеет работать только с 64x64 (старые 64x32 мы достраиваем сами).
            if (w != 64 || (h != 64 && h != 32)) {
                return null;
            }
            NativeImage out = new NativeImage(64, 64, true);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = (Integer) a.rgb.invoke(wrap, x, y);
                    out.setPixelRGBA(x, y, a.nativeFormat ? c : argbToAbgr(c));
                }
            }
            if (h == 32) {
                convertLegacy(out);
            }
            if (logged < 3) {
                logged++;
                SkinBridgeMod.LOGGER.info("Captured a {}x{} skin image from {}", w, h, wrap.getClass().getSimpleName());
            }
            return out;
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                SkinBridgeMod.LOGGER.warn("Could not copy the TL skin image; 3D layers will be unavailable for it.", t);
            }
            return null;
        }
    }

    private static Accessors resolve(Class<?> type) throws NoSuchMethodException {
        Accessors cached = ACCESSORS.get(type);
        if (cached != null) {
            return cached;
        }
        Method width = type.getMethod("getWidth");
        Method height = type.getMethod("getHeight");
        Method rgb = type.getMethod("getRGB", int.class, int.class);
        Accessors created = new Accessors(width, height, rgb, type.getSimpleName().equals("NativeImageWrap"));
        ACCESSORS.put(type, created);
        return created;
    }

    private static int argbToAbgr(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** То же, что делает игра для старых скинов 64x32: достраивает левые руку и ногу зеркально. */
    private static void convertLegacy(NativeImage image) {
        image.copyRect(4, 16, 16, 32, 4, 4, true, false);
        image.copyRect(8, 16, 16, 32, 4, 4, true, false);
        image.copyRect(0, 20, 24, 32, 4, 12, true, false);
        image.copyRect(4, 20, 16, 32, 4, 12, true, false);
        image.copyRect(8, 20, 8, 32, 4, 12, true, false);
        image.copyRect(12, 20, 16, 32, 4, 12, true, false);
        image.copyRect(44, 16, -8, 32, 4, 4, true, false);
        image.copyRect(48, 16, -8, 32, 4, 4, true, false);
        image.copyRect(40, 20, 0, 32, 4, 12, true, false);
        image.copyRect(44, 20, -8, 32, 4, 12, true, false);
        image.copyRect(48, 20, -16, 32, 4, 12, true, false);
        image.copyRect(52, 20, -8, 32, 4, 12, true, false);
    }
}
