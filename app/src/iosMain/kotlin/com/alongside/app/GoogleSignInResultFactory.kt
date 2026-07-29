package com.alongside.app

import com.alongside.feature.auth.GoogleSignInResult

/**
 * `GoogleSignInResult`'s concrete subtypes live in `feature:auth`, not this module - Kotlin/Native
 * only exports types from a dependency module when they're reachable from an exported
 * declaration's signature, so without these, Swift's `GoogleSignInAuthProvider` would have no way
 * to construct a result to pass into `onResult`. These three are `app`'s own declarations (always
 * fully exported), returning the sealed interface - Swift never needs the concrete class itself.
 */
public fun googleSignInSuccess(idToken: String): GoogleSignInResult = GoogleSignInResult.Success(idToken)

public fun googleSignInFailure(message: String?): GoogleSignInResult = GoogleSignInResult.Failure(message)

public fun googleSignInCancelled(): GoogleSignInResult = GoogleSignInResult.Cancelled
