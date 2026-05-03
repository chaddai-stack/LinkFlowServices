package com.linkflow.api.auth.repository;

import com.linkflow.api.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户 仓库
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 邮箱唯一性检查
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * 用户名唯一性检查
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * 登录按邮箱查询
     */
    Optional<User> findByEmailIgnoreCase(String email);
}
