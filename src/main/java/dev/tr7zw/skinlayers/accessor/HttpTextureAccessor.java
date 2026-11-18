package dev.tr7zw.skinlayers.accessor;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.FileNotFoundException;

/**
 * ЗАГЛУШКА только для компиляции. Настоящий интерфейс находится в моде Skin Layers 3D.
 * Этот файл исключается из итогового jar (см. build.gradle).
 */
public interface HttpTextureAccessor {
    NativeImage getImage() throws FileNotFoundException;
}
