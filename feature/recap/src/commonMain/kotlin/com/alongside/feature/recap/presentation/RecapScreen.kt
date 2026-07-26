package com.alongside.feature.recap.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.StoriesChrome
import com.alongside.core.ui.recap.ClosestMomentSlideContent
import com.alongside.core.ui.recap.DayHighlightSlideContent
import com.alongside.core.ui.recap.FinalSlideContent
import com.alongside.core.ui.recap.IntroSlideContent
import com.alongside.core.ui.recap.MatchListSlideContent
import com.alongside.core.ui.recap.ParallelLivesSlideContent
import com.alongside.core.ui.recap.SwipeArchetypeSlideContent
import com.alongside.core.ui.recap.UnresolvedQuestionSlideContent
import org.orbitmvi.orbit.compose.collectAsState

/**
 * The trip's Stories deck (docs/roadmap.md M20.3) - [StoriesChrome] (M20.2) driven by whatever
 * [RecapContainer] built from the active trip's current local data, one Composable per
 * [RecapSlide] variant (M20.3.5). [onFinish] is a plain navigation callback, not an Orbit side
 * effect - "the user is done watching" is a UI concern, same as `entry<Settings>`'s `onClose`.
 */
@Composable
public fun RecapScreen(
    container: RecapContainer,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()

    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        if (!state.isLoading && state.slides.isNotEmpty()) {
            StoriesChrome(
                slideCount = state.slides.size,
                activeIndex = state.activeIndex,
                onActiveIndexChange = { container.onIntent(RecapIntent.ChangeActiveSlide(it)) },
                onFinish = onFinish,
                modifier = Modifier.fillMaxSize(),
            ) { index, _ ->
                RecapSlideContent(state.slides[index])
            }
        }
    }
}

/** Exhaustive `when`, no `else` - the compiler guarantees every [RecapSlide] variant is handled. */
@Composable
internal fun RecapSlideContent(
    slide: RecapSlide,
    modifier: Modifier = Modifier,
) {
    when (slide) {
        is RecapSlide.Intro -> IntroSlideContent(slide, modifier)
        is RecapSlide.ParallelLives -> ParallelLivesSlideContent(slide, modifier)
        is RecapSlide.DayHighlight -> DayHighlightSlideContent(slide, modifier)
        is RecapSlide.ClosestMoment -> ClosestMomentSlideContent(slide, modifier)
        is RecapSlide.SwipeArchetype -> SwipeArchetypeSlideContent(slide, modifier)
        is RecapSlide.UnresolvedQuestion -> UnresolvedQuestionSlideContent(slide, modifier)
        is RecapSlide.MatchList -> MatchListSlideContent(slide, modifier)
        is RecapSlide.Final -> FinalSlideContent(slide, modifier)
    }
}
