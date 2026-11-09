package com.weathermate.security;

import com.weathermate.config.AppProperties;
import com.weathermate.entity.AppUser;
import com.weathermate.service.UserService;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Turns the "Authorization: tma &lt;initData&gt;" header into an AppUser.
 * In dev mode the header "X-Dev-User-Id: 123" can be used instead, to test the API from a normal browser.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PREFIX = "tma ";

    private final InitDataValidator validator;
    private final UserService userService;
    private final AppProperties properties;

    public CurrentUserArgumentResolver(InitDataValidator validator, UserService userService,
                                       AppProperties properties) {
        this.validator = validator;
        this.userService = userService;
        this.properties = properties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AppUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith(PREFIX)) {
            TelegramUser tg = validator.validate(auth.substring(PREFIX.length()))
                    .orElseThrow(() -> new UnauthorizedException("Invalid Telegram initData"));
            return userService.getOrCreate(tg);
        }

        String devUserId = request.getHeader("X-Dev-User-Id");
        if (properties.devMode() && devUserId != null) {
            try {
                return userService.getOrCreate(new TelegramUser(Long.parseLong(devUserId), "Dev", "dev", "ru"));
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("Bad X-Dev-User-Id");
            }
        }
        throw new UnauthorizedException("Open the app from Telegram");
    }
}
