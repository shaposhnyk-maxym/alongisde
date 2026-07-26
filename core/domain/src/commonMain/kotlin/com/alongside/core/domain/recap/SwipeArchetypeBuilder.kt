package com.alongside.core.domain.recap

import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.recap.CategorySwipeTally
import com.alongside.core.model.recap.RecapSlide

/**
 * Like/dislike distribution by [PlaceCandidate.category], separately per side - only candidates
 * with a non-null category count. Absent if either side has zero categorized swipes.
 */
public fun buildSwipeArchetype(
    placeCandidates: List<PlaceCandidate>,
    ownSwipes: List<PlaceSwipe>,
    partnerSwipes: List<PlaceSwipe>,
): RecapSlide.SwipeArchetype? {
    val categoryByCandidateId = categoryByCandidateId(placeCandidates)
    val ownTally = tallyByCategory(categoryByCandidateId, ownSwipes)
    val partnerTally = tallyByCategory(categoryByCandidateId, partnerSwipes)
    if (ownTally.isEmpty() || partnerTally.isEmpty()) return null
    return RecapSlide.SwipeArchetype(ownTally, partnerTally)
}

private fun categoryByCandidateId(placeCandidates: List<PlaceCandidate>): Map<String, String> =
    placeCandidates
        .mapNotNull { candidate -> candidate.category?.let { category -> candidate.id to category } }
        .toMap()

private fun tallyByCategory(
    categoryByCandidateId: Map<String, String>,
    swipes: List<PlaceSwipe>,
): List<CategorySwipeTally> =
    swipes
        .mapNotNull { swipe ->
            categoryByCandidateId[swipe.candidateId]?.let { category -> category to swipe.direction }
        }.groupBy({ it.first }, { it.second })
        .map { (category, directions) ->
            CategorySwipeTally(
                category = category,
                likeCount = directions.count { it == SwipeDirection.LIKE },
                dislikeCount = directions.count { it == SwipeDirection.DISLIKE },
            )
        }
