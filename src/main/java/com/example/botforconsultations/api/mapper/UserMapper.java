package com.example.botforconsultations.api.mapper;

import com.example.botforconsultations.api.dto.UserDto;
import com.example.botforconsultations.core.model.TelegramUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(telegramUser.getRole().name())")
    @Mapping(target = "isActive", source = "hasConfirmed")
    @Mapping(target = "telegramId", source = "telegramId")
    UserDto.TelegramUserInfo toTelegramUserInfo(TelegramUser telegramUser);

    List<UserDto.TelegramUserInfo> toTelegramUserInfo(List<TelegramUser> telegramUsers);
}
