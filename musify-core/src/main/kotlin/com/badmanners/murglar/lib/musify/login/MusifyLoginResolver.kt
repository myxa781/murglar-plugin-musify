package com.badmanners.murglar.lib.musify.login

import com.badmanners.murglar.lib.core.login.CredentialsLoginVariant
import com.badmanners.murglar.lib.core.login.CredentialLoginStep
import com.badmanners.murglar.lib.core.login.LoginResolver
import com.badmanners.murglar.lib.core.login.SuccessfulLogin
import com.badmanners.murglar.lib.core.login.WebLoginVariant
import com.badmanners.murglar.lib.core.webview.WebViewProvider
import com.badmanners.murglar.lib.musify.localization.MusifyMessages

class MusifyLoginResolver(
    private val messages: MusifyMessages
) : LoginResolver {

    override val isLogged: Boolean get() = true
    override val loginInfo: String get() = messages.youAreNotLoggedIn
    override val webLoginVariants: List<WebLoginVariant> get() = emptyList()
    override val credentialsLoginVariants: List<CredentialsLoginVariant> get() = emptyList()

    override suspend fun credentialsLogin(loginVariantId: String, args: Map<String, String>): CredentialLoginStep =
        SuccessfulLogin

    override suspend fun webLogin(loginVariantId: String, webViewProvider: WebViewProvider): Boolean = false

    override fun logout() {}
}
