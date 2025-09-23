package com.example.botforconsultations.api.mapper;

import com.example.botforconsultations.api.dto.UserDto;
import com.example.botforconsultations.core.model.TelegramUser;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {


    UserDto.TelegramUserInfo toTelegramUserInfo(TelegramUser telegramUser);

    List<UserDto.TelegramUserInfo> toTelegramUserInfo(List<TelegramUser> telegramUsers);
}
