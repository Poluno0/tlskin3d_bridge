package dev.skinbridge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Мост между TL Skin Cape и Skin Layers 3D.
 * Вся работа выполняется миксином {@link dev.skinbridge.mixin.LightTextureMixin}.
 */
@Mod(SkinBridgeMod.MOD_ID)
public class SkinBridgeMod {
    public static final String MOD_ID = "tlskin3dbridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkinBridgeMod() {
        LOGGER.info("TL Skin Cape <-> Skin Layers 3D bridge loaded.");
    }
}
