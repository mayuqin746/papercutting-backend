package com.example.kpappercutting.controller

import com.example.kpappercutting.model.User
import com.example.kpappercutting.model.UserFollow
import com.example.kpappercutting.repository.PostRepository
import com.example.kpappercutting.repository.UserFollowRepository
import com.example.kpappercutting.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository,
    private val userFollowRepository: UserFollowRepository,
    private val postRepository: PostRepository
) {
    @GetMapping("/{userId}")
    fun getUserProfile(
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?
    ): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(user.toProfileResponse(viewerId))
    }

    @GetMapping("/{userId}/following")
    fun getFollowingUsers(
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?
    ): ResponseEntity<Any> {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build()
        }

        val followingIds = userFollowRepository.findByFollowerId(userId).map { it.followingId }
        val users = userRepository.findAllById(followingIds).associateBy { it.id }
        val response = followingIds.mapNotNull { id -> users[id]?.toProfileResponse(viewerId) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{userId}/followers")
    fun getFollowerUsers(
        @PathVariable userId: Long,
        @RequestParam(required = false) viewerId: Long?
    ): ResponseEntity<Any> {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build()
        }

        val followerIds = userFollowRepository.findByFollowingId(userId).map { it.followerId }
        val users = userRepository.findAllById(followerIds).associateBy { it.id }
        val response = followerIds.mapNotNull { id -> users[id]?.toProfileResponse(viewerId) }

        return ResponseEntity.ok(response)
    }

    @Transactional
    @PostMapping("/follow")
    fun follow(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val followerId = body["followerId"]
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少followerId"))
        val followingId = body["followingId"]
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少followingId"))

        if (followerId == followingId) {
            return ResponseEntity.badRequest().body(mapOf("message" to "不能关注自己"))
        }

        val follower = userRepository.findById(followerId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "关注者不存在"))
        val following = userRepository.findById(followingId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "被关注用户不存在"))

        if (userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId) == null) {
            userFollowRepository.save(UserFollow(followerId = followerId, followingId = followingId))
            userRepository.save(follower.copy(followingCount = follower.followingCount + 1))
            userRepository.save(following.copy(followerCount = following.followerCount + 1))
        }

        return ResponseEntity.ok(buildFollowResponse(followerId, followingId, true))
    }

    @Transactional
    @PostMapping("/follow/toggle")
    fun toggleFollow(@RequestBody body: Map<String, Long>): ResponseEntity<Any> {
        val followerId = body["followerId"]
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少followerId"))
        val followingId = body["followingId"]
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "缺少followingId"))

        if (followerId == followingId) {
            return ResponseEntity.badRequest().body(mapOf("message" to "不能关注自己"))
        }

        val follower = userRepository.findById(followerId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "关注者不存在"))
        val following = userRepository.findById(followingId).orElse(null)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "被关注用户不存在"))

        val existingFollow = userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId)
        val isFollowing = if (existingFollow != null) {
            userFollowRepository.delete(existingFollow)
            userRepository.save(follower.copy(followingCount = (follower.followingCount - 1).coerceAtLeast(0)))
            userRepository.save(following.copy(followerCount = (following.followerCount - 1).coerceAtLeast(0)))
            false
        } else {
            userFollowRepository.save(UserFollow(followerId = followerId, followingId = followingId))
            userRepository.save(follower.copy(followingCount = follower.followingCount + 1))
            userRepository.save(following.copy(followerCount = following.followerCount + 1))
            true
        }

        return ResponseEntity.ok(buildFollowResponse(followerId, followingId, isFollowing))
    }

    private fun User.toProfileResponse(viewerId: Long?): UserProfileResponse {
        val isFollowing = viewerId != null &&
                viewerId != id &&
                userFollowRepository.findByFollowerIdAndFollowingId(viewerId, id) != null

        return UserProfileResponse(
            id = id,
            username = username,
            nickname = nickname,
            region = region,
            bio = bio,
            followingCount = followingCount,
            followerCount = followerCount,
            likedCount = postRepository.sumLikeCountByAuthorId(id).toInt(),
            avatarUrl = avatarUrl,
            backgroundUrl = backgroundUrl,
            isFollowing = isFollowing
        )
    }

    private fun buildFollowResponse(
        followerId: Long,
        followingId: Long,
        isFollowing: Boolean
    ): FollowResponse {
        val follower = userRepository.findById(followerId).orElse(null)
        val following = userRepository.findById(followingId).orElse(null)

        return FollowResponse(
            isFollowing = isFollowing,
            followerCount = following?.followerCount ?: userFollowRepository.countByFollowingId(followingId).toInt(),
            followingCount = following?.followingCount ?: userFollowRepository.countByFollowerId(followingId).toInt(),
            currentUserFollowingCount = follower?.followingCount ?: userFollowRepository.countByFollowerId(followerId).toInt()
        )
    }
}

data class UserProfileResponse(
    val id: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val region: String = "",
    val bio: String = "",
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val likedCount: Int = 0,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null,
    val isFollowing: Boolean = false
)

data class FollowResponse(
    val isFollowing: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val currentUserFollowingCount: Int = 0
)
