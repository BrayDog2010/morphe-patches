package app.braydog2010.patches.spotify.layout.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.stringOption
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.getReference
import app.braydog2010.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/braydog2010/extension/spotify/layout/theme/CustomThemePatch;"

private val customThemeBytecodePatch = bytecodePatch(
    name = "Custom theme (bytecode)",
    description = "Hooks color parsing for custom theming.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        ColorSpaceUtilsClassFingerprint.method.apply {
            addInstructions(
                0,
                """
                    long-to-int p0, p0
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->replaceColor(I)I
                    move-result p0
                    int-to-long p0, p0
                """,
            )
        }

        ParseLottieJsonFingerprint.method.apply {
            val invokeParseColorIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.definingClass == "Landroid/graphics/Color;" &&
                        reference.name == "parseColor"
            }
            val parsedColorRegister = getInstruction<OneRegisterInstruction>(invokeParseColorIndex + 1).registerA

            val replaceColorDescriptor = "$EXTENSION_CLASS_DESCRIPTOR->replaceColor(I)I"

            addInstructions(
                invokeParseColorIndex + 2,
                """
                    # Use invoke-static/range because the register number is too large.
                    invoke-static/range { v$parsedColorRegister .. v$parsedColorRegister }, $replaceColorDescriptor
                    move-result v$parsedColorRegister
                """,
            )
        }

        ParseAnimatedColorFingerprint.method.apply {
            val invokeArgbIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.definingClass == "Landroid/graphics/Color;" &&
                        reference.name == "argb"
            }
            val argbColorRegister = getInstruction<OneRegisterInstruction>(invokeArgbIndex + 1).registerA

            addInstructions(
                invokeArgbIndex + 2,
                """
                    invoke-static { v$argbColorRegister }, $EXTENSION_CLASS_DESCRIPTOR->replaceColor(I)I
                    move-result v$argbColorRegister
                """,
            )
        }
    }
}

@Suppress("unused")
val customThemePatch = resourcePatch(
    name = "Custom theme",
    description = "Applies a custom theme (defaults to amoled black)",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    dependsOn(customThemeBytecodePatch)

    val backgroundColor by stringOption(
        key = "spotify_theme_background",
        default = "@android:color/black",
        title = "Primary background color",
        description = "The background color. Can be a hex color or a resource reference.",
        required = true,
    )

    val overridePlayerGradientColor by booleanOption(
        key = "spotify_theme_override_gradient",
        default = false,
        title = "Override player gradient color",
        description = "Apply primary background color to the player gradient color.",
        required = false,
    )

    val backgroundColorSecondary by stringOption(
        key = "spotify_theme_background_secondary",
        default = "#FF121212",
        title = "Secondary background color",
        description = "The secondary background color.",
        required = true,
    )

    val accentColor by stringOption(
        key = "spotify_theme_accent",
        default = "#FF1ED760",
        title = "Accent color",
        description = "The accent color ('Spotify green' by default).",
        required = true,
    )

    val accentColorPressed by stringOption(
        key = "spotify_theme_accent_pressed",
        default = "#FF1ABC54",
        title = "Pressed accent color",
        description = "The color when accented buttons are pressed.",
        required = true,
    )

    finalize {
        document("res/values/colors.xml").use { doc ->
            val resourcesNode = doc.getElementsByTagName("resources").item(0) as Element

            val childNodes = resourcesNode.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i) as? Element ?: continue
                val name = node.getAttribute("name")

                if (name == "bg_gradient_start_color" && !overridePlayerGradientColor!!) {
                    continue
                }

                node.textContent = when (name) {
                    "gray_7",
                    "gray_10",
                    "dark_base_background_base",
                    "dark_base_background_elevated_base",
                    "bg_gradient_start_color", "bg_gradient_end_color",
                    "sthlm_blk", "sthlm_blk_grad_start",
                    "image_placeholder_color",
                        -> backgroundColor

                    "gray_15",
                    "track_credits_card_bg", "benefit_list_default_color", "merch_card_background",
                    "opacity_white_10",
                    "dark_base_background_tinted_highlight",
                        -> backgroundColorSecondary

                    "dark_brightaccent_background_base",
                    "dark_base_text_brightaccent",
                    "green_light",
                    "spotify_green_157",
                        -> accentColor

                    "dark_brightaccent_background_press",
                        -> accentColorPressed

                    else -> continue
                }
            }
        }

        try {
            document("res/drawable/start_screen_gradient.xml").use { doc ->
                val gradientNode = doc.getElementsByTagName("gradient").item(0) as Element
                gradientNode.setAttribute("android:startColor", "@color/gray_7")
                gradientNode.setAttribute("android:endColor", "@color/gray_7")
            }
        } catch (_: Exception) {
            // Fails for 9.0.66+
        }
    }
}
