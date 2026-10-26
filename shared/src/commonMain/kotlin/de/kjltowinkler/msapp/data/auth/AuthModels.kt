package de.kjltowinkler.msapp.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String = "",
    val className: String = "9aR",
    val schoolType: String = "Realschule",
    val classTeacher: String = "Hr. Thomas",
    val avatarType: String = "initials",
    val avatarColorHex: Long = 0xFF395A3CL
)

@Serializable
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val className: String,
    val schoolType: String,
    val classTeacher: String,
    val avatarType: String,
    val avatarColorHex: Long,
    val role: String = "USER",
    val createdAt: Long = 0L
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserProfileDto
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val className: String? = null,
    val schoolType: String? = null,
    val classTeacher: String? = null,
    val avatarType: String? = null,
    val avatarColorHex: Long? = null
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class SimpleApiResponse(
    val success: Boolean,
    val message: String
)
