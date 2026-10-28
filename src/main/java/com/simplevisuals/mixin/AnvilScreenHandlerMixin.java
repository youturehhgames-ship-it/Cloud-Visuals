package com.simplevisuals.mixin;

import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {

    /**
     * Wir leiten den Aufruf in der Methode "sanitize" um.
     * Da "sanitize" static ist, muss auch unsere Mixin-Methode static sein.
     */
    @Redirect(
            method = "sanitize",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StringHelper;stripInvalidChars(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private static String allowSectionSign(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            // Wir nutzen StringHelper.isValidChar (statt SharedConstants)
            // ODER wir schreiben die Logik selbst, um Import-Probleme zu vermeiden:
            // (c != 167 && c >= 32 && c != 127) ist der Vanilla-Check.
            // Wir wollen § (167) erlauben, also prüfen wir nur auf "Nicht-Steuerzeichen".

            if (c >= 32 && c != 127) { // Erlaubt alles außer Steuerzeichen (DEL etc.)
                sb.append(c);
            }
        }
        return sb.toString();
    }
}