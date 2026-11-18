package dev.skinbridge.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.skinbridge.SkinCopier;
import dev.tr7zw.skinlayers.accessor.HttpTextureAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TL Skin Cape хранит скин в своём классе LightTexture: картинка уходит в видеопамять и сразу закрывается,
 * а Skin Layers 3D умеет читать пиксели только у HttpTexture и DynamicTexture.
 *
 * Здесь мы:
 * 1) в начале LightTexture.load(Object) копируем пиксели скина в свой NativeImage (до закрытия оригинала);
 * 2) объявляем LightTexture реализацией интерфейса HttpTextureAccessor из Skin Layers 3D,
 *    чтобы Skin Layers мог получить эту копию через getImage();
 * 3) освобождаем копию, когда TL закрывает текстуру.
 */
@Mixin(targets = "org.tlauncher.renderer.texture.LightTexture", remap = false)
public abstract class LightTextureMixin implements HttpTextureAccessor {

    /** Наш личный оригинал: его никому не отдаём, поэтому его не может закрыть Skin Layers. */
    @Unique
    private NativeImage skinbridge$image;

    /** Последняя копия, отданная Skin Layers (он вправе её закрыть, например при выходе из мира). */
    @Unique
    private NativeImage skinbridge$handedOut;

    @Inject(method = "load(Ljava/lang/Object;)V", at = @At("HEAD"), remap = false)
    private void skinbridge$capture(Object image, CallbackInfo ci) {
        skinbridge$release();
        SkinCopier.logTextureCreated(image);
        skinbridge$image = SkinCopier.copy(image);
        if (skinbridge$image != null) {
            SkinCopier.logEvent("Captured into texture #" + System.identityHashCode(this) + ": " + SkinCopier.describe(skinbridge$image));
        }
    }

    @Inject(method = "close()V", at = @At("HEAD"), remap = false)
    private void skinbridge$onClose(CallbackInfo ci) {
        // Копию намеренно НЕ освобождаем: TL может закрыть текстуру, а скин в игре продолжит использоваться.
        SkinCopier.logEvent("TL closed texture #" + System.identityHashCode(this) + " (our copy: " + SkinCopier.describe(skinbridge$image) + ")");
    }

    @Unique
    private void skinbridge$release() {
        SkinCopier.closeQuietly(skinbridge$handedOut);
        skinbridge$handedOut = null;
        NativeImage old = skinbridge$image;
        skinbridge$image = null;
        if (old != null) {
            try {
                old.close();
            } catch (RuntimeException ignored) {
                // уже закрыта
            }
        }
    }

    @Override
    public NativeImage getImage() {
        if (skinbridge$image == null) {
            SkinCopier.logEvent("Skin Layers asked for the skin image (texture #" + System.identityHashCode(this) + "): NONE");
            return null;
        }
        if (!SkinCopier.isUsable(skinbridge$handedOut)) {
            // Прежнюю копию Skin Layers уже закрыл (или её не было): делаем новую из оригинала.
            skinbridge$handedOut = SkinCopier.duplicate(skinbridge$image);
            SkinCopier.logEvent("Skin Layers asked for the skin image (texture #" + System.identityHashCode(this)
                    + "): new copy, " + SkinCopier.describe(skinbridge$handedOut));
        }
        return skinbridge$handedOut;
    }
}
