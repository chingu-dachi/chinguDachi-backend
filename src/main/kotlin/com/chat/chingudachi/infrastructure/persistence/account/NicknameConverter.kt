package com.chat.chingudachi.infrastructure.persistence.account

import com.chat.chingudachi.domain.account.Nickname
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class NicknameConverter : AttributeConverter<Nickname, String> {
    override fun convertToDatabaseColumn(attribute: Nickname?): String? =
        attribute?.value

    override fun convertToEntityAttribute(dbData: String?): Nickname? =
        dbData?.let { Nickname(it) }
}
