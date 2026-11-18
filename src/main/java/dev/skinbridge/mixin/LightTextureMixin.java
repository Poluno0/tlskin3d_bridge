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

    @Unique
    private NativeImage skinbridge$image;

    @Inject(method = "load(Ljava/lang/Object;)V", at = @At("HEAD"), remap = false)
    private void skinbridge$capture(Object image, CallbackInfo ci) {
        skinbridge$release();
        SkinCopier.logTextureCreated(image);
        skinbridge$image = SkinCopier.copy(image);
    }

    @Inject(method = "close()V", at = @At("HEAD"), remap = false)
    private void skinbridge$onClose(CallbackInfo ci) {
        skinbridge$release();
    }

    @Unique
    private void skinbridge$release() {
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
        return skinbridge$image;
    }
}
