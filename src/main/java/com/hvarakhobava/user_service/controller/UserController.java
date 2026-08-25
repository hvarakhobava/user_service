package com.hvarakhobava.user_service.controller;

import com.hvarakhobava.user_service.dto.UserDTO;
import com.hvarakhobava.user_service.model.User;
import com.hvarakhobava.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Hanna Varakhobava
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping("")
    public Collection<UserDTO> listUsers(ListUsersRequest pageRequest) {
        PageRequest pageable = PageRequest.of(pageRequest.pageNumber(), pageRequest.size(),
                Sort.by(pageRequest.direction(), pageRequest.sortBy()));

        return userService.list(pageable)
                .stream().map(UserController::toUserDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable UUID id) {
        return userService.findUserById(id)
                .map(UserController::toUserDTO)
                .orElseThrow(() -> new RuntimeException("User with id %s not found".formatted(id)));
    }

    @PutMapping("/{id}")
    public void updateUser(@PathVariable UUID id,
                           @RequestBody UserDTO userDTO) {
        userService.updateUser(toUser(userDTO));
    }

    private static UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private static User toUser(UserDTO userDTO) {
        return User.builder()
                .id(userDTO.id())
                .role(userDTO.role())
                .status(userDTO.status())
                .build();
    }
}
