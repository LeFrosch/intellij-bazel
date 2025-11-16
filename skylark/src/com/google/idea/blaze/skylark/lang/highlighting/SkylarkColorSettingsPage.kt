package com.google.idea.blaze.skylark.lang.highlighting

import com.google.idea.blaze.skylark.lang.SkylarkLanguage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class SkylarkColorSettingsPage : ColorSettingsPage {

  override fun getDisplayName(): String = SkylarkLanguage.displayName

  override fun getIcon(): Icon? = null

  override fun getHighlighter(): SyntaxHighlighter = SkylarkSyntaxHighlighter()

  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
    return SkylarkTextAttributes.entries.map { it.descriptor }.toTypedArray()
  }

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
    return SkylarkTextAttributes.entries.associate { it.attribute.externalName to it.attribute }
  }

  override fun getDemoText(): String = """
    load("@rules_cc//cc:defs.bzl", "cc_common")
    
    # defensive list of features that can appear in the C++ toolchain
    <SKYLARK_CONSTANT>UNSUPPORTED_FEATURES</SKYLARK_CONSTANT> = [
        "thin_lto",
        "fdo_optimize",
    ]
    
    def <SKYLARK_FUNCTION_DECLARATION>get_cc_features</SKYLARK_FUNCTION_DECLARATION>(target, ctx):
        if not cc_common.<SKYLARK_PROVIDER>CcToolchainInfo</SKYLARK_PROVIDER> in target:
            <SKYLARK_FUNCTION_CALL>fail</SKYLARK_FUNCTION_CALL>("no C++ toolchain is not configured for the target: %s" % target.label) 
        
        return cc_common.<SKYLARK_FUNCTION_CALL>configure_features</SKYLARK_FUNCTION_CALL>(
            <SKYLARK_NAMED_ARGUMENT>ctx</SKYLARK_NAMED_ARGUMENT> = ctx,
            <SKYLARK_NAMED_ARGUMENT>cc_toolchain</SKYLARK_NAMED_ARGUMENT> = target[cc_common.<SKYLARK_PROVIDER>CcToolchainInfo</SKYLARK_PROVIDER>],
            <SKYLARK_NAMED_ARGUMENT>requested_features</SKYLARK_NAMED_ARGUMENT> = ctx.features,
            <SKYLARK_NAMED_ARGUMENT>unsupported_features</SKYLARK_NAMED_ARGUMENT> = ctx.disabled_features + <SKYLARK_CONSTANT>UNSUPPORTED_FEATURES</SKYLARK_CONSTANT>,
        )
  """.trimIndent()
}
