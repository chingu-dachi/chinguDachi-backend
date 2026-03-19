package com.chat.chingudachi.account.repository

import com.chat.chingudachi.account.domain.Account
import com.chat.chingudachi.account.domain.Nation
import com.chat.chingudachi.account.domain.Nickname
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccountRepository : JpaRepository<Account, Long> {
    fun existsByNickname(nickname: Nickname): Boolean

    @Query(
        """
        SELECT a FROM Account a
        WHERE a.accountStatus = 'ACTIVE'
        AND a.deletedAt IS NULL
        AND a.id != :excludeAccountId
        AND (:nationality IS NULL OR a.nation = :nationality)
        AND (:cursor IS NULL OR a.id < :cursor)
        ORDER BY a.id DESC
        """,
    )
    fun findDiscoverableUsers(
        @Param("excludeAccountId") excludeAccountId: Long,
        @Param("nationality") nationality: Nation?,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Account>

    @Query(
        """
        SELECT DISTINCT a FROM Account a
        JOIN UserInterest ui ON ui.account = a
        JOIN ui.interestTag it
        WHERE a.accountStatus = 'ACTIVE'
        AND a.deletedAt IS NULL
        AND a.id != :excludeAccountId
        AND (:nationality IS NULL OR a.nation = :nationality)
        AND it.tagKey IN :interests
        AND (:cursor IS NULL OR a.id < :cursor)
        ORDER BY a.id DESC
        """,
    )
    fun findDiscoverableUsersByInterests(
        @Param("excludeAccountId") excludeAccountId: Long,
        @Param("nationality") nationality: Nation?,
        @Param("interests") interests: List<String>,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Account>
}
