package com.chat.chingudachi.chat.controller

import com.chat.chingudachi.account.domain.AccountStatus
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import com.chat.chingudachi.account.domain.TranslateLanguage
import com.chat.chingudachi.account.repository.AccountRepository
import com.chat.chingudachi.auth.domain.OAuthProvider
import com.chat.chingudachi.auth.domain.OAuthUserInfo
import com.chat.chingudachi.auth.infra.OAuthClient
import com.chat.chingudachi.auth.repository.AccountCredentialRepository
import com.chat.chingudachi.auth.repository.AuthTokenRepository
import com.chat.chingudachi.chat.repository.ChatRoomRepository
import com.chat.chingudachi.chat.repository.RoomMemberRepository
import com.chat.chingudachi.fixture.AccountFixture
import com.chat.chingudachi.support.MockOAuthConfig
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@Import(MockOAuthConfig::class)
class ChatRoomControllerTest(
    private val mockMvc: MockMvc,
    private val oAuthClient: OAuthClient,
    private val authTokenRepository: AuthTokenRepository,
    private val accountCredentialRepository: AccountCredentialRepository,
    private val accountRepository: AccountRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val roomMemberRepository: RoomMemberRepository,
) : DescribeSpec() {
    init {
        afterEach {
            clearMocks(oAuthClient)
            roomMemberRepository.deleteAll()
            chatRoomRepository.deleteAll()
            authTokenRepository.deleteAll()
            accountCredentialRepository.deleteAll()
            accountRepository.deleteAll()
        }

        describe("POST /api/chat-rooms") {
            context("유효한 타겟 유저로 채팅방 생성") {
                it("201과 채팅방 정보를 반환한다") {
                    val myToken = loginAndGetToken("my-code", "google-1", "me@test.com")
                    val myAccount = accountRepository.findAll().first()
                    activateAccount(myAccount.id, Nickname("나유저"), Nation.KR)

                    val targetAccount = accountRepository.save(
                        AccountFixture.create(
                            id = 0,
                            accountStatus = AccountStatus.ACTIVE,
                            nickname = Nickname("타겟유저"),
                            nation = Nation.JP,
                            translateLanguage = TranslateLanguage.KO,
                        ),
                    )

                    mockMvc.post("/api/chat-rooms") {
                        header("Authorization", "Bearer $myToken")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"memberIds":[${targetAccount.id}]}"""
                    }.andExpect {
                        status { isCreated() }
                        jsonPath("$.data.roomId") { isNotEmpty() }
                        jsonPath("$.data.participants.length()") { value(2) }
                    }
                }
            }

            context("자기 자신에게 채팅방 생성") {
                it("400 에러를 반환한다") {
                    val myToken = loginAndGetToken("self-code", "google-self", "self@test.com")
                    val myAccount = accountRepository.findAll().first()
                    activateAccount(myAccount.id, Nickname("셀프유저"), Nation.KR)

                    mockMvc.post("/api/chat-rooms") {
                        header("Authorization", "Bearer $myToken")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"memberIds":[${myAccount.id}]}"""
                    }.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.code") { value("CHAT_ROOM_SELF_CHAT") }
                    }
                }
            }

            context("이미 채팅방이 존재할 때") {
                it("기존 채팅방을 반환한다") {
                    val myToken = loginAndGetToken("dup-code", "google-dup", "dup@test.com")
                    val myAccount = accountRepository.findAll().first()
                    activateAccount(myAccount.id, Nickname("중복유저"), Nation.KR)

                    val targetAccount = accountRepository.save(
                        AccountFixture.create(
                            id = 0,
                            accountStatus = AccountStatus.ACTIVE,
                            nickname = Nickname("대상유저"),
                            nation = Nation.JP,
                            translateLanguage = TranslateLanguage.KO,
                        ),
                    )

                    // 첫 번째 생성
                    mockMvc.post("/api/chat-rooms") {
                        header("Authorization", "Bearer $myToken")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"memberIds":[${targetAccount.id}]}"""
                    }.andExpect { status { isCreated() } }

                    // 두 번째 시도 — 기존 방 반환
                    mockMvc.post("/api/chat-rooms") {
                        header("Authorization", "Bearer $myToken")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"memberIds":[${targetAccount.id}]}"""
                    }.andExpect {
                        status { isCreated() }
                        jsonPath("$.data.participants.length()") { value(2) }
                    }

                    // 방은 하나만 존재
                    chatRoomRepository.count() == 1L
                }
            }

            context("인증 없이 요청") {
                it("401을 반환한다") {
                    mockMvc.post("/api/chat-rooms") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"memberIds":[999]}"""
                    }.andExpect {
                        status { isUnauthorized() }
                    }
                }
            }
        }

        describe("GET /api/chat-rooms") {
            context("채팅방 목록 조회") {
                it("빈 목록을 반환한다") {
                    val token = loginAndGetToken("list-code", "google-list", "list@test.com")
                    val account = accountRepository.findAll().first()
                    activateAccount(account.id, Nickname("목록유저"), Nation.KR)

                    mockMvc.get("/api/chat-rooms") {
                        header("Authorization", "Bearer $token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.items") { isArray() }
                        jsonPath("$.data.items.length()") { value(0) }
                        jsonPath("$.data.hasMore") { value(false) }
                    }
                }
            }

            context("채팅방이 있을 때") {
                it("채팅방 목록을 반환한다") {
                    val token = loginAndGetToken("rooms-code", "google-rooms", "rooms@test.com")
                    val myAccount = accountRepository.findAll().first()
                    activateAccount(myAccount.id, Nickname("방유저"), Nation.KR)

                    val target = accountRepository.save(
                        AccountFixture.create(
                            id = 0,
                            accountStatus = AccountStatus.ACTIVE,
                            nickname = Nickname("방상대"),
                            nation = Nation.JP,
                            translateLanguage = TranslateLanguage.KO,
                        ),
                    )

                    mockMvc.post("/api/chat-rooms") {
                        header("Authorization", "Bearer $token")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"memberIds":[${target.id}]}"""
                    }.andExpect { status { isCreated() } }

                    mockMvc.get("/api/chat-rooms") {
                        header("Authorization", "Bearer $token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.data.items.length()") { value(1) }
                        jsonPath("$.data.items[0].participants.length()") { value(2) }
                    }
                }
            }
        }
    }

    private fun loginAndGetToken(code: String, googleId: String, email: String): String {
        every { oAuthClient.authenticate(code, any()) } returns
            OAuthUserInfo(OAuthProvider.GOOGLE, googleId, email)

        val result = mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"$code"}"""
        }.andReturn()

        return com.jayway.jsonpath.JsonPath.read(
            result.response.contentAsString,
            "$.data.accessToken",
        )
    }

    private fun activateAccount(accountId: Long, nickname: Nickname, nation: Nation) {
        val account = accountRepository.findById(accountId).get()
        account.agreeToTerms()
        account.completeOnboarding(
            nickname = nickname,
            birthYear = 1995,
            nation = nation,
            bio = null,
            city = null,
            profileImageUrl = null,
        )
        accountRepository.save(account)
    }
}
